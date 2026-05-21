package com.snapword.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrEngine @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): List<String> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image)
        return result.textBlocks
            .flatMap { block -> block.lines.map { it.text.trim() } }
            .filter { it.length in 2..20 }
            .filter { it.matches(Regex("^[a-zA-Z]+$")) }
            .distinct()
            .take(50)
    }
}
