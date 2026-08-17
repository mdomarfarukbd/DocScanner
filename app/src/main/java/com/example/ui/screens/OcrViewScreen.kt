package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PrintAndShareHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodels.ScannerViewModel
import com.example.ui.viewmodels.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrViewScreen(
    viewModel: ScannerViewModel,
    documentId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docState by viewModel.repository.getDocumentById(documentId).collectAsState(initial = null)
    val pagesState by viewModel.repository.getPagesForDocument(documentId).collectAsState(initial = emptyList())

    var editableText by remember(docState) {
        mutableStateOf(docState?.ocrFullText ?: "")
    }

    var searchQuery by remember { mutableStateOf("") }

    val wordCount = remember(editableText) {
        editableText.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    val matchCount = remember(editableText, searchQuery) {
        if (searchQuery.isBlank()) 0
        else {
            Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE).findAll(editableText).count()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TextFields,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text("Extracted OCR Text", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.2).sp)
                            Text(
                                text = "${docState?.title ?: "Document"} • $wordCount words",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.DocumentDetail(documentId)) },
                        modifier = Modifier.testTag("ocr_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Copy to clipboard
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Extracted OCR Text", editableText)
                            clipboard.setPrimaryClip(clip)
                            viewModel.showSnackbar("Copied to clipboard")
                        },
                        modifier = Modifier.testTag("ocr_copy_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy text")
                    }

                    // Share text
                    IconButton(
                        onClick = {
                            viewModel.shareExtractedText(
                                editableText,
                                "${docState?.title ?: "Document"} - Extracted Text"
                            )
                        },
                        modifier = Modifier.testTag("ocr_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share text")
                    }

                    // Export TXT file
                    IconButton(
                        onClick = {
                            val file = PrintAndShareHelper.exportTextFile(
                                context,
                                editableText,
                                docState?.title ?: "Document"
                            )
                            PrintAndShareHelper.sharePdf(context, file, "Exported OCR TXT")
                            viewModel.showSnackbar("Exported as .txt file")
                        }
                    ) {
                        Icon(Icons.Default.Description, contentDescription = "Export TXT")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$wordCount words • ${editableText.length} chars",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "100% Offline On-Device OCR",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            // Update OCR for the document
                            docState?.let { doc ->
                                viewModel.updateDocumentDetails(
                                    doc.id,
                                    doc.title,
                                    doc.tag,
                                    doc.isVault
                                )
                                // Also if first page exists
                                if (pagesState.isNotEmpty()) {
                                    viewModel.updateOcrText(pagesState.first().id, editableText)
                                }
                                viewModel.showSnackbar("OCR edits saved")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("ocr_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Edits")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // In-text Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Find text in document...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "$matchCount matches",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            if (editableText.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No Text Detected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Try scanning with better lighting or using the Magic Color filter.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Editable OCR Text Area
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .testTag("ocr_text_content_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }
    }
}
