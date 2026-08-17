package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentPageEntity
import com.example.engine.DocumentFilter
import com.example.engine.PdfExportConfig
import com.example.engine.QuadPoints
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.VaultGold
import com.example.ui.viewmodels.ScannerViewModel
import com.example.ui.viewmodels.Screen
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    viewModel: ScannerViewModel,
    documentId: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val docState by viewModel.repository.getDocumentById(documentId).collectAsState(initial = null)
    val pagesState by viewModel.repository.getPagesForDocument(documentId).collectAsState(initial = emptyList())

    val pagerState = rememberPagerState(pageCount = { maxOf(1, pagesState.size) })
    val cachedBitmaps = remember { mutableStateMapOf<String, Bitmap?>() }

    var showPdfDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddPageMenu by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Edit info dialog fields
    var editTitle by remember { mutableStateOf("") }
    var editTag by remember { mutableStateOf("") }
    var editVault by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromGalleryUris(uris, targetDocId = documentId)
        }
    }

    // Load decrypted bitmaps reactively for pages
    LaunchedEffect(pagesState) {
        for (page in pagesState) {
            if (!cachedBitmaps.containsKey(page.id)) {
                val bitmap = viewModel.repository.getDecryptedBitmap(page.processedEncryptedPath)
                cachedBitmaps[page.id] = bitmap
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = docState?.title ?: "Document",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            if (docState?.isVault == true) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Encrypted Vault",
                                    tint = VaultGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "${pagesState.size} ${if (pagesState.size == 1) "page" else "pages"} • ${docState?.tag ?: "Document"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("doc_detail_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Quick OCR Extracted Text
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.OcrViewer(documentId)) },
                        modifier = Modifier.testTag("doc_ocr_button")
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = "Extracted OCR Text",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Direct Print
                    IconButton(
                        onClick = {
                            docState?.let { doc ->
                                viewModel.exportAndPrintPdf(
                                    doc.id,
                                    PdfExportConfig(title = doc.title)
                                )
                            }
                        },
                        modifier = Modifier.testTag("doc_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print Document")
                    }

                    // Share PDF
                    IconButton(
                        onClick = { showPdfDialog = true },
                        modifier = Modifier.testTag("doc_share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export & Share PDF")
                    }

                    // 3-dot Menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Title & Tag") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    docState?.let {
                                        editTitle = it.title
                                        editTag = it.tag
                                        editVault = it.isVault
                                        showEditInfoDialog = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Document", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Page Controls Bar (Reorder, Crop, Filter, Delete Page)
                if (pagesState.isNotEmpty()) {
                    val currentPageIndex = pagerState.currentPage.coerceIn(0, pagesState.size - 1)
                    val currentPage = pagesState.getOrNull(currentPageIndex)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reorder Page Left
                        IconButton(
                            enabled = currentPageIndex > 0,
                            onClick = {
                                val mutable = pagesState.toMutableList()
                                val temp = mutable[currentPageIndex]
                                mutable[currentPageIndex] = mutable[currentPageIndex - 1]
                                mutable[currentPageIndex - 1] = temp
                                viewModel.reorderPages(documentId, mutable)
                                coroutineScope.launch {
                                    pagerState.scrollToPage(currentPageIndex - 1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Move Page Left", modifier = Modifier.size(16.dp))
                        }

                        // Adjust Crop
                        TextButton(
                            onClick = {
                                if (currentPage != null) {
                                    coroutineScope.launch {
                                        val rawBitmap = viewModel.repository.getDecryptedBitmap(currentPage.rawEncryptedPath)
                                        if (rawBitmap != null) {
                                            val quad = QuadPoints.fromSerialized(currentPage.cropCorners)
                                            viewModel.navigateTo(
                                                Screen.CropAdjust(
                                                    bitmap = rawBitmap,
                                                    initialQuad = quad,
                                                    documentId = documentId,
                                                    pageId = currentPage.id
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("page_crop_adjust_button")
                        ) {
                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Crop", fontSize = 12.sp)
                        }

                        // Enhance Filters
                        TextButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.testTag("page_filters_button")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filters", fontSize = 12.sp)
                        }

                        // Reorder Page Right
                        IconButton(
                            enabled = currentPageIndex < pagesState.size - 1,
                            onClick = {
                                val mutable = pagesState.toMutableList()
                                val temp = mutable[currentPageIndex]
                                mutable[currentPageIndex] = mutable[currentPageIndex + 1]
                                mutable[currentPageIndex + 1] = temp
                                viewModel.reorderPages(documentId, mutable)
                                coroutineScope.launch {
                                    pagerState.scrollToPage(currentPageIndex + 1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = "Move Page Right", modifier = Modifier.size(16.dp))
                        }

                        // Delete Single Page
                        IconButton(
                            onClick = {
                                if (currentPage != null) {
                                    viewModel.deletePage(currentPage, documentId)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Horizontal Thumbnail Carousel
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(pagesState) { index, page ->
                        val isSelected = pagerState.currentPage == index
                        val bmp = cachedBitmaps[page.id]

                        Box(
                            modifier = Modifier
                                .size(width = 54.dp, height = 72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Add Page Button Tile
                    item {
                        Box(
                            modifier = Modifier
                                .size(width = 54.dp, height = 72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .clickable { showAddPageMenu = true }
                                .testTag("add_page_thumbnail_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Page",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Add",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            if (pagesState.isEmpty()) {
                Text("No pages in document", color = Color.White)
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val page = pagesState.getOrNull(pageIndex)
                    val bmp = page?.let { cachedBitmaps[it.id] }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bmp != null) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier.fillMaxWidth(0.92f)
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Scanned Page ${pageIndex + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                // Page Indicator Floating Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Page ${pagerState.currentPage + 1} of ${pagesState.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Add Page Bottom Sheet / Picker
    if (showAddPageMenu) {
        AlertDialog(
            onDismissRequest = { showAddPageMenu = false },
            title = { Text("Add Page to Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showAddPageMenu = false
                            viewModel.navigateTo(Screen.CameraScanner(targetDocId = documentId))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan with Camera")
                    }

                    OutlinedButton(
                        onClick = {
                            showAddPageMenu = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import from Gallery")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddPageMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PDF Export & Print Dialog
    if (showPdfDialog && docState != null) {
        PdfPreviewDialog(
            initialTitle = docState!!.title,
            pageCount = pagesState.size,
            onDismiss = { showPdfDialog = false },
            onSharePdf = { config ->
                viewModel.exportAndSharePdf(documentId, config)
            },
            onPrintPdf = { config ->
                viewModel.exportAndPrintPdf(documentId, config)
            }
        )
    }

    // Filter Enhancement Modal Bottom Sheet
    if (showFilterSheet && pagesState.isNotEmpty()) {
        val currentPageIndex = pagerState.currentPage.coerceIn(0, pagesState.size - 1)
        val currentPage = pagesState[currentPageIndex]
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enhancement Filter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                // Filter options grid
                val filters = DocumentFilter.values()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.take(3).forEach { filter ->
                        val isSelected = currentPage.filterType == filter.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.applyFilterToPage(currentPage, filter)
                                cachedBitmaps.remove(currentPage.id)
                            },
                            label = { Text(filter.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.drop(3).forEach { filter ->
                        val isSelected = currentPage.filterType == filter.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.applyFilterToPage(currentPage, filter)
                                cachedBitmaps.remove(currentPage.id)
                            },
                            label = { Text(filter.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Apply to all pages button
                Button(
                    onClick = {
                        val currentFilterEnum = try {
                            DocumentFilter.valueOf(currentPage.filterType)
                        } catch (e: Exception) {
                            DocumentFilter.MAGIC_COLOR
                        }
                        viewModel.applyFilterToAllPages(documentId, currentFilterEnum)
                        cachedBitmaps.clear()
                        showFilterSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Current Filter to All ${pagesState.size} Pages")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Edit Details Dialog
    if (showEditInfoDialog && docState != null) {
        AlertDialog(
            onDismissRequest = { showEditInfoDialog = false },
            title = { Text("Edit Document Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTag,
                        onValueChange = { editTag = it },
                        label = { Text("Category Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { editVault = !editVault }
                    ) {
                        Icon(
                            imageVector = if (editVault) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (editVault) VaultGold else MaterialTheme.colorScheme.outline
                        )
                        Text("Encrypted Secure Vault")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDocumentDetails(documentId, editTitle, editTag, editVault)
                        showEditInfoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditInfoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Document Confirm Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to permanently delete this document and all its encrypted pages?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteDocument(documentId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
