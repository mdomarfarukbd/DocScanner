package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentEntity
import com.example.engine.PdfExportConfig
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.VaultGold
import com.example.ui.viewmodels.ScannerViewModel
import com.example.ui.viewmodels.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var showRenameDialog by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameTitle by remember { mutableStateOf("") }
    var renameTag by remember { mutableStateOf("") }
    var renameVault by remember { mutableStateOf(false) }

    // Multi-photo gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromGalleryUris(uris)
        }
    }

    val tags = listOf("All", "Document", "Receipt", "ID Card", "Notes", "Vault")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "DocScanner Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "DocScanner",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.2).sp
                                )
                                Text(
                                    text = "Offline • Hardware Encrypted",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    actions = {
                        // Vault lock toggle
                        IconButton(
                            onClick = {
                                if (isVaultUnlocked) {
                                    viewModel.lockVault()
                                    viewModel.showSnackbar("Vault Locked")
                                } else {
                                    showPinDialog = true
                                }
                            },
                            modifier = Modifier.testTag("vault_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Vault Security",
                                tint = if (isVaultUnlocked) VaultGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search docs, tags, or OCR text...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("search_text_field")
                )

                // Category Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags) { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (tag == "Vault" && !isVaultUnlocked) {
                                    showPinDialog = true
                                } else {
                                    viewModel.setSelectedTag(tag)
                                }
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (tag == "Vault") {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else VaultGold
                                        )
                                    }
                                    Text(tag, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("filter_chip_$tag")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Import from Gallery FAB
                FloatingActionButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("gallery_import_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Import Gallery Photos",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Camera Scanner Primary FAB
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(Screen.CameraScanner()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("camera_scan_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan Document",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Scan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTag == "Vault" && !isVaultUnlocked) {
                // Vault locked banner view
                VaultLockedState(onUnlockClick = { showPinDialog = true })
            } else if (documents.isEmpty()) {
                EmptyDocumentsState(
                    hasQuery = searchQuery.isNotBlank(),
                    onScanClick = { viewModel.navigateTo(Screen.CameraScanner()) },
                    onImportClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentGridCard(
                            document = doc,
                            viewModel = viewModel,
                            onClick = { viewModel.navigateTo(Screen.DocumentDetail(doc.id)) },
                            onRenameClick = {
                                showRenameDialog = doc
                                renameTitle = doc.title
                                renameTag = doc.tag
                                renameVault = doc.isVault
                            },
                            onDeleteClick = { viewModel.deleteDocument(doc.id) },
                            onSharePdfClick = {
                                viewModel.exportAndSharePdf(
                                    doc.id,
                                    PdfExportConfig(title = doc.title)
                                )
                            },
                            onPrintClick = {
                                viewModel.exportAndPrintPdf(
                                    doc.id,
                                    PdfExportConfig(title = doc.title)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Vault PIN Dialog
    if (showPinDialog) {
        val isSettingNewPin = !viewModel.securityPreferences.isPinSet
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinInput = ""
                pinError = false
            },
            title = {
                Text(if (isSettingNewPin) "Set Vault PIN" else "Unlock Encrypted Vault")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isSettingNewPin)
                            "Create a 4-digit PIN to secure sensitive documents in the encrypted vault."
                        else
                            "Enter your 4-digit security PIN to access encrypted files.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        label = { Text("4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth().testTag("vault_pin_input")
                    )
                    if (pinError) {
                        Text(
                            "Incorrect PIN. Try again.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            val success = viewModel.unlockVault(pinInput)
                            if (success) {
                                showPinDialog = false
                                pinInput = ""
                                viewModel.setSelectedTag("Vault")
                            } else {
                                pinError = true
                            }
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isSettingNewPin) "Set PIN" else "Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename & Tag Dialog
    showRenameDialog?.let { doc ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Edit Document Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = renameTitle,
                        onValueChange = { renameTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = renameTag,
                        onValueChange = { renameTag = it },
                        label = { Text("Category Tag (e.g. Receipt, ID Card, Work)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable { renameVault = !renameVault }
                    ) {
                        Icon(
                            imageVector = if (renameVault) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (renameVault) VaultGold else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Move to Encrypted Secure Vault",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDocumentDetails(doc.id, renameTitle, renameTag, renameVault)
                        showRenameDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DocumentGridCard(
    document: DocumentEntity,
    viewModel: ScannerViewModel,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSharePdfClick: () -> Unit,
    onPrintClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(document.coverImagePath) {
        if (document.coverImagePath.isNotBlank()) {
            coverBitmap = viewModel.repository.getDecryptedBitmap(document.coverImagePath)
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("document_card_${document.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 3.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Thumbnail Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = document.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Page Count Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${document.pageCount} ${if (document.pageCount == 1) "page" else "pages"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Vault Badge
                if (document.isVault) {
                    Surface(
                        shape = CircleShape,
                        color = VaultGold,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Vault Document",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Document Info
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share PDF") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onSharePdfClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Direct Print") },
                                leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onPrintClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Title & Tag") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRenameClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = document.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(document.updatedAt)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (document.ocrFullText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "OCR Ready",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "OCR Text Indexed",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyDocumentsState(
    hasQuery: Boolean,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasQuery) Icons.Default.Search else Icons.Default.CameraAlt,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (hasQuery) "No matching documents" else "No Scanned Documents",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasQuery)
                "Try searching with another keyword, tag, or extracted text"
            else
                "Scan documents, receipts, book pages, or notes with high clarity offline cropping and OCR.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )

        if (!hasQuery) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Document")
                }
                Button(
                    onClick = onImportClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Photo")
                }
            }
        }
    }
}

@Composable
fun VaultLockedState(onUnlockClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(VaultGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = VaultGold,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Encrypted Vault is Locked",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Documents in the vault are protected with AES-256 hardware encryption. Enter your PIN to view or manage vault files.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onUnlockClick,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unlock Vault")
        }
    }
}
