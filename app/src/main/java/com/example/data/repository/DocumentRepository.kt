package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentPageEntity
import com.example.data.security.CryptoManager
import com.example.engine.DocumentFilter
import com.example.engine.ImageProcessor
import com.example.engine.OcrManager
import com.example.engine.PdfExportConfig
import com.example.engine.PdfExporter
import com.example.engine.QuadPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DocumentRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val cryptoManager: CryptoManager,
    private val ocrManager: OcrManager
) {
    private val documentDao = database.documentDao()
    private val pageDao = database.documentPageDao()

    fun getAllDocuments(): Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    fun getDocumentsByVault(isVault: Boolean): Flow<List<DocumentEntity>> =
        documentDao.getDocumentsByVault(isVault)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> =
        documentDao.searchDocuments(query)

    fun getDocumentById(id: String): Flow<DocumentEntity?> =
        documentDao.getDocumentById(id)

    fun getPagesForDocument(documentId: String): Flow<List<DocumentPageEntity>> =
        pageDao.getPagesForDocument(documentId)

    suspend fun createDocument(
        title: String,
        tag: String = "Document",
        isVault: Boolean = false,
        rawBitmaps: List<Bitmap>,
        initialFilter: DocumentFilter = DocumentFilter.MAGIC_COLOR,
        quads: List<QuadPoints> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val documentId = UUID.randomUUID().toString()
        val docFolder = File(context.filesDir, "encrypted_docs/$documentId").apply { mkdirs() }
        
        val pages = mutableListOf<DocumentPageEntity>()
        val fullOcrBuilder = StringBuilder()
        var coverImagePath = ""

        for (i in rawBitmaps.indices) {
            val raw = rawBitmaps[i]
            val quad = if (i < quads.size) quads[i] else ImageProcessor.detectDocumentCorners(raw)

            // Save encrypted raw
            val rawFile = File(docFolder, "page_${i + 1}_raw.enc")
            cryptoManager.saveEncryptedBitmap(raw, rawFile)

            // Process crop & filter
            val cropped = ImageProcessor.cropPerspective(raw, quad)
            val filtered = ImageProcessor.applyFilter(cropped, initialFilter)

            // Save encrypted processed
            val procFile = File(docFolder, "page_${i + 1}_proc.enc")
            cryptoManager.saveEncryptedBitmap(filtered, procFile)

            if (i == 0) {
                coverImagePath = procFile.absolutePath
            }

            // Extract OCR text offline
            val ocrResult = ocrManager.extractText(filtered)
            if (ocrResult.fullText.isNotBlank()) {
                if (fullOcrBuilder.isNotEmpty()) fullOcrBuilder.append("\n\n--- Page ${i + 1} ---\n")
                fullOcrBuilder.append(ocrResult.fullText)
            }

            val pageEntity = DocumentPageEntity(
                id = UUID.randomUUID().toString(),
                documentId = documentId,
                pageNumber = i + 1,
                rawEncryptedPath = rawFile.absolutePath,
                processedEncryptedPath = procFile.absolutePath,
                cropCorners = quad.toSerialized(),
                filterType = initialFilter.name,
                rotation = 0,
                ocrText = ocrResult.fullText
            )
            pages.add(pageEntity)
        }

        val docEntity = DocumentEntity(
            id = documentId,
            title = title.ifBlank { "Scan ${System.currentTimeMillis() % 10000}" },
            tag = tag,
            isVault = isVault,
            pageCount = pages.size,
            coverImagePath = coverImagePath,
            ocrFullText = fullOcrBuilder.toString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        documentDao.insertDocument(docEntity)
        pageDao.insertPages(pages)

        documentId
    }

    suspend fun addPagesToDocument(
        documentId: String,
        newBitmaps: List<Bitmap>,
        filter: DocumentFilter = DocumentFilter.MAGIC_COLOR
    ) = withContext(Dispatchers.IO) {
        val existingPages = pageDao.getPagesListForDocument(documentId)
        val docFolder = File(context.filesDir, "encrypted_docs/$documentId").apply { mkdirs() }
        var currentCount = existingPages.size

        val newPageEntities = mutableListOf<DocumentPageEntity>()
        val newOcrBuilder = StringBuilder()

        for (raw in newBitmaps) {
            currentCount++
            val quad = ImageProcessor.detectDocumentCorners(raw)

            val rawFile = File(docFolder, "page_${currentCount}_raw.enc")
            cryptoManager.saveEncryptedBitmap(raw, rawFile)

            val cropped = ImageProcessor.cropPerspective(raw, quad)
            val filtered = ImageProcessor.applyFilter(cropped, filter)

            val procFile = File(docFolder, "page_${currentCount}_proc.enc")
            cryptoManager.saveEncryptedBitmap(filtered, procFile)

            val ocrResult = ocrManager.extractText(filtered)
            if (ocrResult.fullText.isNotBlank()) {
                newOcrBuilder.append("\n\n--- Page $currentCount ---\n").append(ocrResult.fullText)
            }

            newPageEntities.add(
                DocumentPageEntity(
                    id = UUID.randomUUID().toString(),
                    documentId = documentId,
                    pageNumber = currentCount,
                    rawEncryptedPath = rawFile.absolutePath,
                    processedEncryptedPath = procFile.absolutePath,
                    cropCorners = quad.toSerialized(),
                    filterType = filter.name,
                    rotation = 0,
                    ocrText = ocrResult.fullText
                )
            )
        }

        pageDao.insertPages(newPageEntities)

        val existingDoc = documentDao.getDocumentDirect(documentId)
        if (existingDoc != null) {
            val updatedFullOcr = (existingDoc.ocrFullText + newOcrBuilder.toString()).trim()
            documentDao.updateDocument(
                existingDoc.copy(
                    pageCount = currentCount,
                    ocrFullText = updatedFullOcr,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun updatePageCropAndFilter(
        pageId: String,
        quad: QuadPoints,
        filter: DocumentFilter,
        rotation: Int
    ) = withContext(Dispatchers.IO) {
        val page = pageDao.getPageById(pageId) ?: return@withContext
        val rawFile = File(page.rawEncryptedPath)
        val rawBitmap = cryptoManager.loadDecryptedBitmap(rawFile) ?: return@withContext

        // Recompute cropped + filtered
        val cropped = ImageProcessor.cropPerspective(rawBitmap, quad, rotation)
        val filtered = ImageProcessor.applyFilter(cropped, filter)

        val procFile = File(page.processedEncryptedPath)
        cryptoManager.saveEncryptedBitmap(filtered, procFile)

        // Rerun OCR on the enhanced page
        val ocrResult = ocrManager.extractText(filtered)

        val updatedPage = page.copy(
            cropCorners = quad.toSerialized(),
            filterType = filter.name,
            rotation = rotation,
            ocrText = ocrResult.fullText
        )
        pageDao.updatePage(updatedPage)

        // Refresh document full text & updatedAt
        val allPages = pageDao.getPagesListForDocument(page.documentId)
        val fullText = allPages.joinToString("\n\n") { it.ocrText }.trim()
        val doc = documentDao.getDocumentDirect(page.documentId)
        if (doc != null) {
            documentDao.updateDocument(
                doc.copy(
                    ocrFullText = fullText,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun applyFilterToAllPages(documentId: String, filter: DocumentFilter) = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesListForDocument(documentId)
        for (page in pages) {
            val rawFile = File(page.rawEncryptedPath)
            val rawBitmap = cryptoManager.loadDecryptedBitmap(rawFile) ?: continue
            val quad = QuadPoints.fromSerialized(page.cropCorners)
            val cropped = ImageProcessor.cropPerspective(rawBitmap, quad, page.rotation)
            val filtered = ImageProcessor.applyFilter(cropped, filter)

            val procFile = File(page.processedEncryptedPath)
            cryptoManager.saveEncryptedBitmap(filtered, procFile)

            val ocr = ocrManager.extractText(filtered)
            pageDao.updatePage(page.copy(filterType = filter.name, ocrText = ocr.fullText))
        }

        val doc = documentDao.getDocumentDirect(documentId)
        if (doc != null) {
            val allPages = pageDao.getPagesListForDocument(documentId)
            val fullText = allPages.joinToString("\n\n") { it.ocrText }.trim()
            documentDao.updateDocument(doc.copy(ocrFullText = fullText, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun reorderPages(documentId: String, reorderedPages: List<DocumentPageEntity>) = withContext(Dispatchers.IO) {
        val updated = reorderedPages.mapIndexed { index, page ->
            page.copy(pageNumber = index + 1)
        }
        pageDao.insertPages(updated)

        val doc = documentDao.getDocumentDirect(documentId)
        if (doc != null && updated.isNotEmpty()) {
            val firstPage = updated.first()
            documentDao.updateDocument(
                doc.copy(
                    coverImagePath = firstPage.processedEncryptedPath,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deletePage(page: DocumentPageEntity) = withContext(Dispatchers.IO) {
        cryptoManager.deleteEncryptedFile(File(page.rawEncryptedPath))
        cryptoManager.deleteEncryptedFile(File(page.processedEncryptedPath))
        pageDao.deletePage(page)

        val remaining = pageDao.getPagesListForDocument(page.documentId)
        if (remaining.isEmpty()) {
            documentDao.deleteDocumentById(page.documentId)
        } else {
            // Re-index pages
            val reindexed = remaining.mapIndexed { index, p -> p.copy(pageNumber = index + 1) }
            pageDao.insertPages(reindexed)

            val doc = documentDao.getDocumentDirect(page.documentId)
            if (doc != null) {
                documentDao.updateDocument(
                    doc.copy(
                        pageCount = reindexed.size,
                        coverImagePath = reindexed.first().processedEncryptedPath,
                        ocrFullText = reindexed.joinToString("\n\n") { it.ocrText }.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun deleteDocument(documentId: String) = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesListForDocument(documentId)
        for (p in pages) {
            cryptoManager.deleteEncryptedFile(File(p.rawEncryptedPath))
            cryptoManager.deleteEncryptedFile(File(p.processedEncryptedPath))
        }
        val docFolder = File(context.filesDir, "encrypted_docs/$documentId")
        docFolder.deleteRecursively()

        pageDao.deletePagesForDocument(documentId)
        documentDao.deleteDocumentById(documentId)
    }

    suspend fun updateDocumentDetails(
        documentId: String,
        title: String,
        tag: String,
        isVault: Boolean
    ) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentDirect(documentId) ?: return@withContext
        documentDao.updateDocument(
            doc.copy(
                title = title.ifBlank { doc.title },
                tag = tag,
                isVault = isVault,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updatePageOcrText(pageId: String, newText: String) = withContext(Dispatchers.IO) {
        val page = pageDao.getPageById(pageId) ?: return@withContext
        pageDao.updatePage(page.copy(ocrText = newText))

        val allPages = pageDao.getPagesListForDocument(page.documentId)
        val fullText = allPages.joinToString("\n\n") { if (it.id == pageId) newText else it.ocrText }.trim()
        val doc = documentDao.getDocumentDirect(page.documentId)
        if (doc != null) {
            documentDao.updateDocument(doc.copy(ocrFullText = fullText, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun getDecryptedBitmap(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        cryptoManager.loadDecryptedBitmap(File(filePath))
    }

    suspend fun getAllDecryptedBitmapsForDocument(documentId: String): List<Bitmap> = withContext(Dispatchers.IO) {
        val pages = pageDao.getPagesListForDocument(documentId)
        pages.mapNotNull { cryptoManager.loadDecryptedBitmap(File(it.processedEncryptedPath)) }
    }

    suspend fun exportDocumentToPdf(
        documentId: String,
        config: PdfExportConfig
    ): File? = withContext(Dispatchers.IO) {
        val bitmaps = getAllDecryptedBitmapsForDocument(documentId)
        if (bitmaps.isEmpty()) return@withContext null
        PdfExporter.generatePdf(context, bitmaps, config)
    }
}
