package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CropEditorScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OcrViewScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.ScannerViewModel
import com.example.ui.viewmodels.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: ScannerViewModel = viewModel()
                DocScannerApp(viewModel)
            }
        }
    }
}

@Composable
fun DocScannerApp(viewModel: ScannerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingMessage by viewModel.loadingMessage.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // System Back Navigation
    BackHandler(enabled = currentScreen !is Screen.Home) {
        when (val screen = currentScreen) {
            is Screen.Home -> Unit
            is Screen.CameraScanner -> viewModel.navigateTo(Screen.Home)
            is Screen.CropAdjust -> {
                if (screen.documentId != null) {
                    viewModel.navigateTo(Screen.DocumentDetail(screen.documentId))
                } else {
                    viewModel.navigateTo(Screen.Home)
                }
            }
            is Screen.FilterEnhance -> viewModel.navigateTo(Screen.DocumentDetail(screen.documentId))
            is Screen.DocumentDetail -> viewModel.navigateTo(Screen.Home)
            is Screen.OcrViewer -> viewModel.navigateTo(Screen.DocumentDetail(screen.documentId))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val screen = currentScreen) {
                    is Screen.Home -> HomeScreen(viewModel = viewModel)
                    is Screen.CameraScanner -> CameraScannerScreen(viewModel = viewModel, targetDocId = screen.targetDocId)
                    is Screen.CropAdjust -> CropEditorScreen(viewModel = viewModel, cropState = screen)
                    is Screen.FilterEnhance -> DocumentDetailScreen(viewModel = viewModel, documentId = screen.documentId)
                    is Screen.DocumentDetail -> DocumentDetailScreen(viewModel = viewModel, documentId = screen.documentId)
                    is Screen.OcrViewer -> OcrViewScreen(viewModel = viewModel, documentId = screen.documentId)
                }
            }
        }

        // Global Processing / OCR Loading Overlay
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldPrimary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = loadingMessage,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hardware accelerated & 100% offline",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
