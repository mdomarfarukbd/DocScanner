package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfPageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (Standard)", 595, 842),
    US_LETTER("US Letter", 612, 792),
    LEGAL("Legal", 612, 1008),
    AUTO_FIT("Fit to Image", 0, 0)
}

enum class PdfMargin(val displayName: String, val marginPt: Float) {
    NONE("No Margin", 0f),
    SMALL("Small (16 pt)", 16f),
    STANDARD("Standard (32 pt)", 32f)
}

data class PdfExportConfig(
    val title: String,
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val margin: PdfMargin = PdfMargin.NONE,
    val includePageNumbers: Boolean = true,
    val includeWatermark: Boolean = false,
    val watermarkText: String = "DocScanner",
    val qualityPercent: Int = 85
)

object PdfExporter {

    fun generatePdf(
        context: Context,
        pageBitmaps: List<Bitmap>,
        config: PdfExportConfig
    ): File {
        val pdfDocument = PdfDocument()
        val totalPages = pageBitmaps.size

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
        }

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 100, 100, 100)
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        for (i in pageBitmaps.indices) {
            val bitmap = pageBitmaps[i]
            val pageNum = i + 1

            val (pageWidth, pageHeight) = if (config.pageSize == PdfPageSize.AUTO_FIT) {
                Pair(bitmap.width, bitmap.height)
            } else {
                Pair(config.pageSize.widthPt, config.pageSize.heightPt)
            }

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Fill clean white background
            canvas.drawColor(Color.WHITE)

            val margin = config.margin.marginPt
            val footerSpace = if (config.includePageNumbers && config.pageSize != PdfPageSize.AUTO_FIT) 24f else 0f

            val availWidth = pageWidth - (margin * 2)
            val availHeight = pageHeight - (margin * 2) - footerSpace

            if (availWidth > 0 && availHeight > 0) {
                // Calculate scaled dimensions to fit inside available area while preserving aspect ratio
                val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetRatio = availWidth / availHeight

                val drawWidth: Float
                val drawHeight: Float

                if (imgRatio > targetRatio) {
                    drawWidth = availWidth
                    drawHeight = availWidth / imgRatio
                } else {
                    drawHeight = availHeight
                    drawWidth = availHeight * imgRatio
                }

                val left = margin + (availWidth - drawWidth) / 2f
                val top = margin + (availHeight - drawHeight) / 2f

                val destRect = RectF(left, top, left + drawWidth, top + drawHeight)
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)

                canvas.drawBitmap(bitmap, srcRect, destRect, imagePaint)

                // Optional watermark
                if (config.includeWatermark && config.watermarkText.isNotBlank()) {
                    canvas.save()
                    canvas.rotate(-30f, pageWidth / 2f, pageHeight / 2f)
                    canvas.drawText(config.watermarkText, pageWidth / 2f, pageHeight / 2f, watermarkPaint)
                    canvas.restore()
                }

                // Page numbering footer
                if (config.includePageNumbers && config.pageSize != PdfPageSize.AUTO_FIT) {
                    val pageText = "Page $pageNum of $totalPages"
                    val textWidth = textPaint.measureText(pageText)
                    canvas.drawText(
                        pageText,
                        (pageWidth - textWidth) / 2f,
                        pageHeight - margin - 8f,
                        textPaint
                    )
                }
            }

            pdfDocument.finishPage(page)
        }

        val sanitizedTitle = config.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outputFile = File(outputDir, "${sanitizedTitle}_$timestamp.pdf")

        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outputFile
    }
}
