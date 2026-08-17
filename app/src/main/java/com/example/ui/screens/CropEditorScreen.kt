package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DocumentFilter
import com.example.engine.ImageProcessor
import com.example.engine.QuadPoints
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ScanLaserColor
import com.example.ui.viewmodels.ScannerViewModel
import com.example.ui.viewmodels.Screen
import kotlin.math.hypot

enum class Corner {
    TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT, NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropEditorScreen(
    viewModel: ScannerViewModel,
    cropState: Screen.CropAdjust,
    modifier: Modifier = Modifier
) {
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var currentQuad by remember { mutableStateOf(cropState.initialQuad) }
    var activeCorner by remember { mutableStateOf(Corner.NONE) }
    var activeCornerOffset by remember { mutableStateOf(Offset.Zero) }

    val rotatedBitmap = remember(cropState.bitmap, rotationDegrees) {
        if (rotationDegrees % 360 == 0) cropState.bitmap
        else ImageProcessor.rotateBitmap(cropState.bitmap, rotationDegrees)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (cropState.pageId != null) "Adjust Page Crop" else "Crop & Perspective",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        val remaining = cropState.remainingBitmaps.size
                        if (remaining > 0) {
                            Text(
                                text = "$remaining more ${if (remaining == 1) "page" else "pages"} left",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (cropState.documentId != null) {
                                viewModel.navigateTo(Screen.DocumentDetail(cropState.documentId))
                            } else {
                                viewModel.navigateTo(Screen.Home)
                            }
                        },
                        modifier = Modifier.testTag("crop_cancel_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    // Auto detect corners
                    IconButton(onClick = {
                        currentQuad = ImageProcessor.detectDocumentCorners(rotatedBitmap)
                    }) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Auto Detect",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Rotate 90
                    IconButton(onClick = {
                        rotationDegrees = (rotationDegrees + 90) % 360
                    }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90")
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Preset buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        currentQuad = QuadPoints(
                            PointF(0f, 0f),
                            PointF(1f, 0f),
                            PointF(1f, 1f),
                            PointF(0f, 1f)
                        )
                    }) {
                        Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Full Image", fontSize = 12.sp)
                    }

                    TextButton(onClick = {
                        currentQuad = ImageProcessor.detectDocumentCorners(rotatedBitmap)
                    }) {
                        Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto Crop", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confirm button
                Button(
                    onClick = {
                        if (cropState.pageId != null && cropState.documentId != null) {
                            viewModel.updatePageCrop(
                                pageId = cropState.pageId,
                                documentId = cropState.documentId,
                                quad = currentQuad,
                                filter = DocumentFilter.MAGIC_COLOR,
                                rotation = rotationDegrees
                            )
                        } else {
                            viewModel.onCropConfirmed(
                                currentBitmap = rotatedBitmap,
                                quad = currentQuad,
                                rotation = rotationDegrees,
                                targetDocId = cropState.documentId,
                                remainingBitmaps = cropState.remainingBitmaps,
                                collectedBitmaps = cropState.capturedQuads.indices.map { rotatedBitmap }, // collected
                                collectedQuads = cropState.capturedQuads
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("crop_confirm_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (cropState.remainingBitmaps.isNotEmpty()) "Next Page" else "Confirm & Enhance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121212))
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val boxWidth = maxWidth.value
                val boxHeight = maxHeight.value

                val imgW = rotatedBitmap.width.toFloat()
                val imgH = rotatedBitmap.height.toFloat()

                val scale = minOf(boxWidth / imgW, boxHeight / imgH)
                val displayW = imgW * scale
                val displayH = imgH * scale

                Box(
                    modifier = Modifier
                        .size(displayW.dp, displayH.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    // Bitmap background
                    Image(
                        bitmap = rotatedBitmap.asImageBitmap(),
                        contentDescription = "Image to crop",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Interactive Corner Touch Layer
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(rotatedBitmap, currentQuad) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width
                                        val h = size.height
                                        val tl = Offset(currentQuad.topLeft.x * w, currentQuad.topLeft.y * h)
                                        val tr = Offset(currentQuad.topRight.x * w, currentQuad.topRight.y * h)
                                        val br = Offset(currentQuad.bottomRight.x * w, currentQuad.bottomRight.y * h)
                                        val bl = Offset(currentQuad.bottomLeft.x * w, currentQuad.bottomLeft.y * h)

                                        val threshold = 40.dp.toPx()
                                        val dTL = hypot((offset.x - tl.x).toDouble(), (offset.y - tl.y).toDouble())
                                        val dTR = hypot((offset.x - tr.x).toDouble(), (offset.y - tr.y).toDouble())
                                        val dBR = hypot((offset.x - br.x).toDouble(), (offset.y - br.y).toDouble())
                                        val dBL = hypot((offset.x - bl.x).toDouble(), (offset.y - bl.y).toDouble())

                                        val minD = minOf(dTL, dTR, dBR, dBL)
                                        if (minD < threshold) {
                                            activeCorner = when (minD) {
                                                dTL -> Corner.TOP_LEFT
                                                dTR -> Corner.TOP_RIGHT
                                                dBR -> Corner.BOTTOM_RIGHT
                                                else -> Corner.BOTTOM_LEFT
                                            }
                                            activeCornerOffset = offset
                                        } else {
                                            activeCorner = Corner.NONE
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val w = size.width
                                        val h = size.height
                                        val newNormX = ((change.position.x) / w).coerceIn(0f, 1f)
                                        val newNormY = ((change.position.y) / h).coerceIn(0f, 1f)
                                        activeCornerOffset = change.position

                                        currentQuad = when (activeCorner) {
                                            Corner.TOP_LEFT -> currentQuad.copy(topLeft = PointF(newNormX, newNormY))
                                            Corner.TOP_RIGHT -> currentQuad.copy(topRight = PointF(newNormX, newNormY))
                                            Corner.BOTTOM_RIGHT -> currentQuad.copy(bottomRight = PointF(newNormX, newNormY))
                                            Corner.BOTTOM_LEFT -> currentQuad.copy(bottomLeft = PointF(newNormX, newNormY))
                                            Corner.NONE -> currentQuad
                                        }
                                    },
                                    onDragEnd = {
                                        activeCorner = Corner.NONE
                                    },
                                    onDragCancel = {
                                        activeCorner = Corner.NONE
                                    }
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        val pTL = Offset(currentQuad.topLeft.x * w, currentQuad.topLeft.y * h)
                        val pTR = Offset(currentQuad.topRight.x * w, currentQuad.topRight.y * h)
                        val pBR = Offset(currentQuad.bottomRight.x * w, currentQuad.bottomRight.y * h)
                        val pBL = Offset(currentQuad.bottomLeft.x * w, currentQuad.bottomLeft.y * h)

                        // Draw shaded polygon mask
                        drawCropOverlay(w, h, pTL, pTR, pBR, pBL)

                        // Draw Quad boundary lines
                        val path = Path().apply {
                            moveTo(pTL.x, pTL.y)
                            lineTo(pTR.x, pTR.y)
                            lineTo(pBR.x, pBR.y)
                            lineTo(pBL.x, pBL.y)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = ScanLaserColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw Corner handles
                        drawCornerHandle(pTL, activeCorner == Corner.TOP_LEFT)
                        drawCornerHandle(pTR, activeCorner == Corner.TOP_RIGHT)
                        drawCornerHandle(pBR, activeCorner == Corner.BOTTOM_RIGHT)
                        drawCornerHandle(pBL, activeCorner == Corner.BOTTOM_LEFT)

                        // Draw edge midpoints
                        drawEdgeHandle((pTL + pTR) / 2f)
                        drawEdgeHandle((pTR + pBR) / 2f)
                        drawEdgeHandle((pBR + pBL) / 2f)
                        drawEdgeHandle((pBL + pTL) / 2f)
                    }
                }

                // Precision Magnifying Loupe
                if (activeCorner != Corner.NONE) {
                    val loupeAlignment = when (activeCorner) {
                        Corner.TOP_LEFT -> Alignment.BottomEnd
                        Corner.TOP_RIGHT -> Alignment.BottomStart
                        Corner.BOTTOM_RIGHT -> Alignment.TopStart
                        Corner.BOTTOM_LEFT -> Alignment.TopEnd
                        Corner.NONE -> Alignment.TopEnd
                    }

                    Box(
                        modifier = Modifier
                            .align(loupeAlignment)
                            .padding(12.dp)
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, ScanLaserColor, CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = rotatedBitmap.asImageBitmap(),
                            contentDescription = "Magnified loupe",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Crosshair
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val c = center
                            val len = 14.dp.toPx()
                            drawLine(Color.Red, Offset(c.x - len, c.y), Offset(c.x + len, c.y), 2.dp.toPx())
                            drawLine(Color.Red, Offset(c.x, c.y - len), Offset(c.x, c.y + len), 2.dp.toPx())
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCornerHandle(center: Offset, isActive: Boolean) {
    val outerRadius = if (isActive) 18.dp.toPx() else 14.dp.toPx()
    val innerRadius = if (isActive) 8.dp.toPx() else 6.dp.toPx()

    drawCircle(
        color = Color(0x99000000),
        radius = outerRadius + 4.dp.toPx(),
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = outerRadius,
        center = center
    )
    drawCircle(
        color = ScanLaserColor,
        radius = innerRadius,
        center = center
    )
}

private fun DrawScope.drawEdgeHandle(center: Offset) {
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = 5.dp.toPx(),
        center = center
    )
}

private fun DrawScope.drawCropOverlay(
    width: Float,
    height: Float,
    tl: Offset,
    tr: Offset,
    br: Offset,
    bl: Offset
) {
    val maskColor = Color(0x66000000)

    val topPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(width, 0f)
        lineTo(tr.x, tr.y)
        lineTo(tl.x, tl.y)
        close()
    }
    drawPath(topPath, maskColor)

    val rightPath = Path().apply {
        moveTo(width, 0f)
        lineTo(width, height)
        lineTo(br.x, br.y)
        lineTo(tr.x, tr.y)
        close()
    }
    drawPath(rightPath, maskColor)

    val bottomPath = Path().apply {
        moveTo(width, height)
        lineTo(0f, height)
        lineTo(bl.x, bl.y)
        lineTo(br.x, br.y)
        close()
    }
    drawPath(bottomPath, maskColor)

    val leftPath = Path().apply {
        moveTo(0f, height)
        lineTo(0f, 0f)
        lineTo(tl.x, tl.y)
        lineTo(bl.x, bl.y)
        close()
    }
    drawPath(leftPath, maskColor)
}

operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
operator fun Offset.div(scalar: Float): Offset = Offset(x / scalar, y / scalar)
