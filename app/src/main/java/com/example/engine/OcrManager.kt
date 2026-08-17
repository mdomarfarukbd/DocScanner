package com.example.engine

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class OcrBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<String>
)

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>,
    val wordCount: Int,
    val characterCount: Int
)

class OcrManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blockList = visionText.textBlocks.map { block ->
                        OcrBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            lines = block.lines.map { it.text }
                        )
                    }

                    val fullText = visionText.text
                    val words = fullText.split(Regex("\\s+")).filter { it.isNotBlank() }

                    continuation.resume(
                        OcrResult(
                            fullText = fullText,
                            blocks = blockList,
                            wordCount = words.size,
                            characterCount = fullText.length
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    // Fallback graceful return
                    continuation.resume(
                        OcrResult(
                            fullText = "",
                            blocks = emptyList(),
                            wordCount = 0,
                            characterCount = 0
                        )
                    )
                }
        } catch (e: Exception) {
            continuation.resume(
                OcrResult(
                    fullText = "",
                    blocks = emptyList(),
                    wordCount = 0,
                    characterCount = 0
                )
            )
        }
    }

    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            // Ignored
        }
    }
}
