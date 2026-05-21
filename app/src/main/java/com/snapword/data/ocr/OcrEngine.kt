package com.snapword.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrEngine @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Characters common in English phonetic transcription
    private val phoneticChars = setOf(
        '/', 'ə', 'æ', 'ɔ', 'ɪ', 'ʊ', 'ʌ', 'ɒ', 'ɜ', 'θ', 'ð',
        'ʃ', 'ʒ', 'ŋ', 'ɡ', 'ɑ', 'ɛ', 'ː', 'ˈ', 'ˌ', '(', ')'
    )

    suspend fun recognize(bitmap: Bitmap): List<String> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()

        val words = mutableListOf<String>()

        for (block in result.textBlocks) {
            for (line in block.lines) {
                val raw = line.text.trim()

                // Split into tokens (handles multi-word lines, checkboxes, etc.)
                val tokens = raw.split(Regex("[\\s,;:()\\[\\]{}]+"))

                for (token in tokens) {
                    if (isValidWord(token)) {
                        words.add(token)
                    }
                }
            }
        }

        // Remove duplicates, preserving order
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (word in words) {
            if (seen.add(word.lowercase())) {
                result.add(word)
            }
        }
        return result.take(50)
    }

    private fun isValidWord(text: String): Boolean {
        // Skip if contains phonetic characters
        if (text.any { it in phoneticChars }) return false

        // Strip common leading/trailing OCR noise: □, *, bullet, numbers, dots, brackets
        var cleaned = text.trim { c ->
            !c.isLetter() || c in "□■●•·◦▪▫○◯○⭕"
        }
        // Also strip trailing non-letter garbage
        cleaned = cleaned.trimEnd { c -> !c.isLetter() }

        // Length check: 2-25 after cleaning
        if (cleaned.length < 2 || cleaned.length > 25) return false

        // Word must start and end with a letter
        if (!cleaned.first().isLetter() || !cleaned.last().isLetter()) return false

        // Allow letters, apostrophe, hyphen within words
        // But apostrophe/hyphen can't be first or last (already checked above)
        val validPattern = Regex("^[a-zA-Z]+(?:['\\-][a-zA-Z]+)*$")
        if (!cleaned.matches(validPattern)) return false

        // Reject single-letter garbage (common OCR artifact)
        if (cleaned.length == 1) return false

        // Reject words with >80% consonants (likely OCR noise)
        val vowels = cleaned.count { it.lowercaseChar() in "aeiou" }
        val letters = cleaned.count { it.isLetter() }
        if (letters > 0 && vowels.toFloat() / letters < 0.15f) return false

        // Reject all-caps words longer than 3 chars (likely acronyms/artifacts)
        if (cleaned.length > 3 && cleaned.all { it.isUpperCase() }) return false

        // Reject words that are just repeated letters (e.g., "aaa", "bbb")
        if (cleaned.length >= 3 && cleaned.all { it.equals(cleaned[0], ignoreCase = true) }) return false

        return true
    }
}
