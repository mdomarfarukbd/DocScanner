package com.example.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentPageEntity
import com.example.data.repository.DocumentRepository
import com.example.data.security.CryptoManager
import com.example.data.security.SecurityPreferences
import com.example.engine.DocumentFilter
import com.example.engine.ImageProcessor
import com.example.engine.OcrManager
import com.example.engine.PdfExportConfig
import com.example.engine.PrintAndShareHelper
import com.example.engine.QuadPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class Screen {
    object Home : Screen()
    data class CameraScanner(val targetDocId: String? = null) : Screen()
    data class CropAdjust(
        val bitmap: Bitmap,
        val initialQuad: QuadPoints = QuadPoints.default(),
        val documentId: String? = null,
        val pageId: String? = null,
        val remainingBitmaps: List<Bitmap> = emptyList(),
        val capturedQuads: List<QuadPoints> = emptyList()
    ) : Screen()
    data class FilterEnhance(
        val documentId: String,
        val pageId: String
    ) : Screen()
    data class DocumentDetail(val documentId: String) : Screen()
    data class OcrViewer(val documentId: String) : Screen()
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val cryptoManager = CryptoManager(context)
    val ocrManager = OcrManager()
    val securityPreferences = SecurityPreferences(context)

    val repository = DocumentRepository(
        context = context,
        database = database,
        cryptoManager = cryptoManager,
        ocrManager = ocrManager
    )

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Processing...")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Filtered documents for Home screen
    val documents: StateFlow<List<DocumentEntity>> = combine(
        repository.getAllDocuments(),
        _searchQuery,
        _selectedTag,
        _isVaultUnlocked
    ) { allDocs, query, tag, vaultUnlocked ->
        allDocs.filter { doc ->
            // Vault protection rule
            if (doc.isVault && !vaultUnlocked && tag != "Vault") {
                false
            } else if (tag == "Vault") {
                doc.isVault && (vaultUnlocked || true) // Vault tab handled in UI
            } else {
                val matchesTag = tag == "All" || doc.tag.equals(tag, ignoreCase = true)
                val matchesQuery = query.isBlank() ||
                        doc.title.contains(query, ignoreCase = true) ||
                        doc.tag.contains(query, ignoreCase = true) ||
                        doc.ocrFullText.contains(query, ignoreCase = true)
                matchesTag && matchesQuery
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTag(tag: String) {
        _selectedTag.value = tag
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Vault security
    fun unlockVault(pin: String): Boolean {
        if (!securityPreferences.isPinSet) {
            securityPreferences.vaultPin = pin
            _isVaultUnlocked.value = true
            showSnackbar("Vault PIN set successfully")
            return true
        } else if (securityPreferences.verifyPin(pin)) {
            _isVaultUnlocked.value = true
            return true
        }
        return false
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    // Process raw photos into Crop Adjust screen
    fun onPhotosCaptured(bitmaps: List<Bitmap>, targetDocId: String? = null) {
        if (bitmaps.isEmpty()) return
        val first = bitmaps.first()
        val detected = ImageProcessor.detectDocumentCorners(first)
        _currentScreen.value = Screen.CropAdjust(
            bitmap = first,
            initialQuad = detected,
            documentId = targetDocId,
            remainingBitmaps = bitmaps.drop(1),
            capturedQuads = emptyList()
        )
    }

    // Import from Gallery Uris
    fun importFromGalleryUris(uris: List<Uri>, targetDocId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Importing photos..."
            val bitmaps = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val options = BitmapFactory.Options().apply {
                                inSampleSize = 1
                            }
                            BitmapFactory.decodeStream(stream, null, options)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            _isLoading.value = false
            if (bitmaps.isNotEmpty()) {
                onPhotosCaptured(bitmaps, targetDocId)
            } else {
                showSnackbar("Could not load selected images")
            }
        }
    }

    // Crop confirmed -> either next photo or finalize document
    fun onCropConfirmed(
        currentBitmap: Bitmap,
        quad: QuadPoints,
        rotation: Int,
        targetDocId: String?,
        remainingBitmaps: List<Bitmap>,
        collectedBitmaps: List<Bitmap>,
        collectedQuads: List<QuadPoints>
    ) {
        val nextCollectedBitmaps = collectedBitmaps + currentBitmap
        val nextCollectedQuads = collectedQuads + quad

        if (remainingBitmaps.isNotEmpty()) {
            val nextBitmap = remainingBitmaps.first()
            val detected = ImageProcessor.detectDocumentCorners(nextBitmap)
            _currentScreen.value = Screen.CropAdjust(
                bitmap = nextBitmap,
                initialQuad = detected,
                documentId = targetDocId,
                remainingBitmaps = remainingBitmaps.drop(1),
                capturedQuads = nextCollectedQuads
            )
        } else {
            // All pages cropped -> Save document
            saveScannedDocument(nextCollectedBitmaps, nextCollectedQuads, targetDocId)
        }
    }

    private fun saveScannedDocument(
        bitmaps: List<Bitmap>,
        quads: List<QuadPoints>,
        targetDocId: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Enhancing & extracting OCR text..."
            try {
                if (targetDocId == null) {
                    val docId = repository.createDocument(
                        title = "Doc ${System.currentTimeMillis() % 100000}",
                        tag = if (_selectedTag.value != "All" && _selectedTag.value != "Vault") _selectedTag.value else "Document",
                        isVault = _selectedTag.value == "Vault",
                        rawBitmaps = bitmaps,
                        initialFilter = DocumentFilter.MAGIC_COLOR,
                        quads = quads
                    )
                    _isLoading.value = false
                    showSnackbar("Document saved & encrypted securely")
                    _currentScreen.value = Screen.DocumentDetail(docId)
                } else {
                    repository.addPagesToDocument(
                        documentId = targetDocId,
                        newBitmaps = bitmaps,
                        filter = DocumentFilter.MAGIC_COLOR
                    )
                    _isLoading.value = false
                    showSnackbar("Pages added successfully")
                    _currentScreen.value = Screen.DocumentDetail(targetDocId)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                showSnackbar("Error saving scan: ${e.localizedMessage}")
                _currentScreen.value = Screen.Home
            }
        }
    }

    // Re-crop or edit single page
    fun updatePageCrop(
        pageId: String,
        documentId: String,
        quad: QuadPoints,
        filter: DocumentFilter,
        rotation: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Updating page..."
            repository.updatePageCropAndFilter(pageId, quad, filter, rotation)
            _isLoading.value = false
            showSnackbar("Page updated")
            _currentScreen.value = Screen.DocumentDetail(documentId)
        }
    }

    // Apply Filter
    fun applyFilterToPage(page: DocumentPageEntity, filter: DocumentFilter) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Applying ${filter.displayName}..."
            val quad = QuadPoints.fromSerialized(page.cropCorners)
            repository.updatePageCropAndFilter(page.id, quad, filter, page.rotation)
            _isLoading.value = false
        }
    }

    fun applyFilterToAllPages(documentId: String, filter: DocumentFilter) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Applying to all pages..."
            repository.applyFilterToAllPages(documentId, filter)
            _isLoading.value = false
            showSnackbar("Filter applied to all pages")
        }
    }

    // Document modifications
    fun updateDocumentDetails(documentId: String, title: String, tag: String, isVault: Boolean) {
        viewModelScope.launch {
            repository.updateDocumentDetails(documentId, title, tag, isVault)
            showSnackbar("Document updated")
        }
    }

    fun deletePage(page: DocumentPageEntity, documentId: String) {
        viewModelScope.launch {
            repository.deletePage(page)
            showSnackbar("Page deleted")
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
            showSnackbar("Document deleted")
            _currentScreen.value = Screen.Home
        }
    }

    fun reorderPages(documentId: String, pages: List<DocumentPageEntity>) {
        viewModelScope.launch {
            repository.reorderPages(documentId, pages)
        }
    }

    // PDF Export, Print & Share
    fun exportAndSharePdf(documentId: String, config: PdfExportConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Generating PDF..."
            val file = repository.exportDocumentToPdf(documentId, config)
            _isLoading.value = false
            if (file != null) {
                PrintAndShareHelper.sharePdf(context, file, config.title)
            } else {
                showSnackbar("Failed to generate PDF")
            }
        }
    }

    fun exportAndPrintPdf(documentId: String, config: PdfExportConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "Preparing print job..."
            val file = repository.exportDocumentToPdf(documentId, config)
            _isLoading.value = false
            if (file != null) {
                PrintAndShareHelper.printPdf(context, file, config.title)
            } else {
                showSnackbar("Failed to prepare document for printing")
            }
        }
    }

    fun shareExtractedText(text: String, title: String) {
        PrintAndShareHelper.shareText(context, text, title)
    }

    fun updateOcrText(pageId: String, newText: String) {
        viewModelScope.launch {
            repository.updatePageOcrText(pageId, newText)
            showSnackbar("OCR Text updated")
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrManager.close()
    }
}
