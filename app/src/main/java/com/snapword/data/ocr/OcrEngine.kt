package com.snapword.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snapword.data.local.Dictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrEngine @Inject constructor(
    private val dictionary: Dictionary
) {

    // Latin text recognizer — bundled model, 100% offline, ~5MB APK increase
    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    suspend fun recognize(bitmap: Bitmap): List<String> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)

        val result: Text = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

        val words = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (block in result.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val word = element.text.trim()
                    val cleaned = cleanWord(word)
                    if (isValidWord(cleaned) && seen.add(cleaned.lowercase())) {
                        words.add(cleaned)
                    }
                }
            }
        }

        words.take(50)
    }

    // ── strip trailing punctuation/symbols from word edges ──
    private fun cleanWord(raw: String): String {
        return raw.trim { c -> !c.isLetter() }
    }

    // ── structural validation ──
    private fun isValidWord(text: String): Boolean {
        if (text.length < 2 || text.length > 25) return false
        if (!text.matches(WORD_PATTERN)) return false

        // Reject all-uppercase >= 3 chars (likely abbreviations / non-words)
        if (text.length >= 3 && text.all { it.isUpperCase() }) return false

        // Require at least 15% vowels (filters OCR noise like "brrrng")
        val vowels = text.count { it.lowercaseChar() in "aeiou" }
        val letters = text.count { it.isLetter() }
        if (letters > 0 && vowels.toFloat() / letters < 0.15f) return false

        return true
    }

    companion object {
        private val WORD_PATTERN = Regex("^[a-zA-Z]+(?:['\\-][a-zA-Z]+)*$")
    }
}
