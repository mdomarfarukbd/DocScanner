package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ScanLaserColor
import com.example.ui.viewmodels.ScannerViewModel
import com.example.ui.viewmodels.Screen
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun CameraScannerScreen(
    viewModel: ScannerViewModel,
    targetDocId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (perm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromGalleryUris(uris, targetDocId)
        }
    }

    // Camera state
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) } // 0: OFF, 1: ON, 2: AUTO
    var isBatchMode by remember { mutableStateOf(false) }
    val capturedBitmaps = remember { mutableStateListOf<Bitmap>() }
    var isCapturing by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "DocScanner needs camera access to scan physical documents offline.",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Grant Camera Access")
                }
                TextButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Text("Go Back", color = Color.White)
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Preview View
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(flashMode)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Document Guide Overlay & Scan Line
        DocumentGuideOverlay(showGrid = showGrid)

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (capturedBitmaps.isNotEmpty()) {
                        viewModel.onPhotosCaptured(capturedBitmaps.toList(), targetDocId)
                    } else {
                        viewModel.navigateTo(Screen.Home)
                    }
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("camera_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Grid Toggle
                IconButton(
                    onClick = { showGrid = !showGrid },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.GridOn,
                        contentDescription = "Toggle Grid",
                        tint = if (showGrid) ScanLaserColor else Color.White
                    )
                }

                // Flash Toggle
                IconButton(
                    onClick = {
                        val nextMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                        flashMode = nextMode
                        imageCapture?.flashMode = nextMode
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    val icon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    }
                    val tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) ScanLaserColor else Color.White
                    Icon(icon, contentDescription = "Flash Mode", tint = tint)
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(bottom = 32.dp, top = 16.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Switcher (Single vs Batch)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (!isBatchMode) EmeraldPrimary else Color.Transparent,
                    modifier = Modifier.clickable { isBatchMode = false }
                ) {
                    Text(
                        text = "Single Page",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (!isBatchMode) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isBatchMode) EmeraldPrimary else Color.Transparent,
                    modifier = Modifier.clickable { isBatchMode = true }
                ) {
                    Text(
                        text = "Batch Multi-Page",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isBatchMode) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Shutter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery import shortcut
                IconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("camera_gallery_button")
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick from Gallery",
                        tint = Color.White
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else EmeraldPrimary)
                        .clickable(enabled = !isCapturing && imageCapture != null) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true
                            val executor = Executors.newSingleThreadExecutor()

                            capture.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(image)
                                        image.close()
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            if (bitmap != null) {
                                                if (isBatchMode) {
                                                    capturedBitmaps.add(bitmap)
                                                } else {
                                                    viewModel.onPhotosCaptured(listOf(bitmap), targetDocId)
                                                }
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            viewModel.showSnackbar("Capture failed: ${exception.message}")
                                        }
                                    }
                                }
                            )
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Batch Finish or Empty Placeholder
                if (isBatchMode && capturedBitmaps.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ScanLaserColor)
                            .clickable {
                                viewModel.onPhotosCaptured(capturedBitmaps.toList(), targetDocId)
                            }
                            .testTag("batch_done_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Finish Batch",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${capturedBitmaps.size}",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (capturedBitmaps.isNotEmpty()) {
                    // Show thumbnail of last captured
                    Image(
                        bitmap = capturedBitmaps.last().asImageBitmap(),
                        contentDescription = "Last Scan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    )
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    val rotation = image.imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

@Composable
fun DocumentGuideOverlay(showGrid: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val docLeft = w * 0.08f
        val docTop = h * 0.16f
        val docRight = w * 0.92f
        val docBottom = h * 0.74f
        val docWidth = docRight - docLeft
        val docHeight = docBottom - docTop

        // Corner guides
        val cornerLen = 28.dp.toPx()
        val strokeW = 4.dp.toPx()
        val cornerColor = Color.White

        // Top-Left
        drawLine(cornerColor, Offset(docLeft, docTop), Offset(docLeft + cornerLen, docTop), strokeW)
        drawLine(cornerColor, Offset(docLeft, docTop), Offset(docLeft, docTop + cornerLen), strokeW)

        // Top-Right
        drawLine(cornerColor, Offset(docRight, docTop), Offset(docRight - cornerLen, docTop), strokeW)
        drawLine(cornerColor, Offset(docRight, docTop), Offset(docRight, docTop + cornerLen), strokeW)

        // Bottom-Left
        drawLine(cornerColor, Offset(docLeft, docBottom), Offset(docLeft + cornerLen, docBottom), strokeW)
        drawLine(cornerColor, Offset(docLeft, docBottom), Offset(docLeft, docBottom - cornerLen), strokeW)

        // Bottom-Right
        drawLine(cornerColor, Offset(docRight, docBottom), Offset(docRight - cornerLen, docBottom), strokeW)
        drawLine(cornerColor, Offset(docRight, docBottom), Offset(docRight, docBottom - cornerLen), strokeW)

        // Subtle 3x3 Grid
        if (showGrid) {
            val gridColor = Color.White.copy(alpha = 0.2f)
            val gridStroke = 1.dp.toPx()

            drawLine(gridColor, Offset(docLeft + docWidth / 3f, docTop), Offset(docLeft + docWidth / 3f, docBottom), gridStroke)
            drawLine(gridColor, Offset(docLeft + 2 * docWidth / 3f, docTop), Offset(docLeft + 2 * docWidth / 3f, docBottom), gridStroke)
            drawLine(gridColor, Offset(docLeft, docTop + docHeight / 3f), Offset(docRight, docTop + docHeight / 3f), gridStroke)
            drawLine(gridColor, Offset(docLeft, docTop + 2 * docHeight / 3f), Offset(docRight, docTop + 2 * docHeight / 3f), gridStroke)
        }

        // Animated Laser Beam
        val laserY = docTop + (docHeight * laserProgress)
        drawLine(
            color = Color(0xFF00E676).copy(alpha = 0.75f),
            start = Offset(docLeft, laserY),
            end = Offset(docRight, laserY),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}
