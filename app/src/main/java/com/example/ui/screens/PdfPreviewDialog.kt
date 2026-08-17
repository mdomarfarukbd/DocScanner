package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PdfExportConfig
import com.example.engine.PdfMargin
import com.example.engine.PdfPageSize
import com.example.ui.theme.EmeraldPrimary

@Composable
fun PdfPreviewDialog(
    initialTitle: String,
    pageCount: Int,
    onDismiss: () -> Unit,
    onSharePdf: (PdfExportConfig) -> Unit,
    onPrintPdf: (PdfExportConfig) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var selectedPageSize by remember { mutableStateOf(PdfPageSize.A4) }
    var selectedMargin by remember { mutableStateOf(PdfMargin.NONE) }
    var includePageNumbers by remember { mutableStateOf(true) }
    var includeWatermark by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("Scanned with DocScanner") }

    val config = remember(title, selectedPageSize, selectedMargin, includePageNumbers, includeWatermark, watermarkText) {
        PdfExportConfig(
            title = title,
            pageSize = selectedPageSize,
            margin = selectedMargin,
            includePageNumbers = includePageNumbers,
            includeWatermark = includeWatermark,
            watermarkText = watermarkText
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text("Export PDF ($pageCount ${if (pageCount == 1) "page" else "pages"})", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // PDF File Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("PDF File Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pdf_title_input")
                )

                // Page Size Selector
                Text(
                    text = "Page Size",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PdfPageSize.values().forEach { size ->
                        FilterChip(
                            selected = selectedPageSize == size,
                            onClick = { selectedPageSize = size },
                            label = { Text(size.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Margins
                Text(
                    text = "Page Margins",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PdfMargin.values().forEach { margin ->
                        FilterChip(
                            selected = selectedMargin == margin,
                            onClick = { selectedMargin = margin },
                            label = { Text(margin.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Page Numbers toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Page Numbers (e.g. Page 1 of $pageCount)", fontSize = 13.sp)
                    Switch(
                        checked = includePageNumbers,
                        onCheckedChange = { includePageNumbers = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                // Watermark toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Security Watermark", fontSize = 13.sp)
                    Switch(
                        checked = includeWatermark,
                        onCheckedChange = { includeWatermark = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                if (includeWatermark) {
                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        label = { Text("Watermark text") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direct Print Button
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onPrintPdf(config)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("pdf_print_action_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print")
                }

                // Share PDF Button
                Button(
                    onClick = {
                        onDismiss()
                        onSharePdf(config)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("pdf_share_action_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
