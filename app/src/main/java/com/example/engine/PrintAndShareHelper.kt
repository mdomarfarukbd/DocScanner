package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PrintAndShareHelper {

    /**
     * Triggers the Android Native Print Spooler with the generated PDF file.
     */
    fun printPdf(context: Context, pdfFile: File, jobName: String = "DocScanner Print Job") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val pdi = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()

                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val input = FileInputStream(pdfFile)
                    val output = FileOutputStream(destination?.fileDescriptor)

                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                            input.close()
                            output.close()
                            return
                        }
                        output.write(buffer, 0, bytesRead)
                    }

                    input.close()
                    output.close()
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    /**
     * Shares a PDF file using the Android Intent chooser.
     */
    fun sharePdf(context: Context, pdfFile: File, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Document PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Shares plain text (e.g. extracted OCR text).
     */
    fun shareText(context: Context, text: String, title: String = "Extracted Document Text") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Share Extracted Text")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Exports text to a .txt file and shares it.
     */
    fun exportTextFile(context: Context, text: String, documentTitle: String): File {
        val outputDir = File(context.cacheDir, "text_exports").apply { mkdirs() }
        val file = File(outputDir, "${documentTitle.replace(" ", "_")}_OCR.txt")
        file.writeText(text)
        return file
    }
}
