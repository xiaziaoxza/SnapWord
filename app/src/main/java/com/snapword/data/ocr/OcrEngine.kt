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
        val words = mutableListOf<String>()
        for (block in result.getTextBlocks()) {
            for (line in block.getLines()) {
                val text = line.getText().trim()
                if (text.length in 2..20 && text.matches(Regex("^[a-zA-Z]+$"))) {
                    words.add(text)
                }
            }
        }
        return words.distinct().take(50)
    }
}
