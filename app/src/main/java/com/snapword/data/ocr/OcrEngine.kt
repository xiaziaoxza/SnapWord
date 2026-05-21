package com.snapword.data.ocr

import android.content.Context
import android.graphics.Bitmap
import com.equationl.ncnnandroidppocr.OCR
import com.equationl.ncnnandroidppocr.bean.Device
import com.equationl.ncnnandroidppocr.bean.ImageSize
import com.equationl.ncnnandroidppocr.bean.ModelType
import com.snapword.data.local.Dictionary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dictionary: Dictionary
) {

    private val ocr = OCR()
    private var initialized = false

    // Characters common in English phonetic transcription
    private val phoneticChars = setOf(
        '/', 'ə', 'æ', 'ɔ', 'ɪ', 'ʊ', 'ʌ', 'ɒ', 'ɜ', 'θ', 'ð',
        'ʃ', 'ʒ', 'ŋ', 'ɡ', 'ɑ', 'ɛ', 'ː', 'ˈ', 'ˌ', '(', ')'
    )

    private val checkboxMisreads = setOf('O', 'D', 'C', 'Q', '0')
    private val checkboxMisreadsLower = setOf('o', 'd', 'c', 'q', '0')

    private suspend fun ensureInitialized(): Boolean {
        if (initialized) return true
        return withContext(Dispatchers.Default) {
            ocr.initModelFromAssert(
                context.assets,
                ModelType.Mobile,
                ImageSize.Size640,
                Device.CPU
            ).also { initialized = it }
        }
    }

    suspend fun recognize(bitmap: Bitmap): List<String> {
        if (!ensureInitialized()) return emptyList()

        val result = withContext(Dispatchers.Default) {
            ocr.detectBitmap(bitmap)
        } ?: return emptyList()

        val words = mutableListOf<String>()

        for (line in result.textLines) {
            val raw = line.text.trim()
            val tokens = raw.split(Regex("[\\s,;:()\\[\\]{}]+"))

            for (token in tokens) {
                val corrected = correctCheckboxArtifact(token)
                if (isValidWord(corrected)) {
                    words.add(corrected)
                }
            }
        }

        // Remove duplicates, preserving order
        val seen = mutableSetOf<String>()
        val filtered = mutableListOf<String>()
        for (word in words) {
            if (seen.add(word.lowercase())) {
                filtered.add(word)
            }
        }
        return filtered.take(50)
    }

    /**
     * When OCR misreads □ checkbox as a letter (O/D/C/Q/0),
     * check the dictionary: if the word isn't found but stripping
     * the first character yields a known word, use the corrected version.
     */
    private fun correctCheckboxArtifact(word: String): String {
        if (word.length < 3) return word
        if (dictionary.lookup(word) != null) return word

        val first = word[0]
        if (first in checkboxMisreads || first in checkboxMisreadsLower) {
            val stripped = word.substring(1)
            if (stripped.length >= 2 && dictionary.lookup(stripped) != null) {
                return stripped
            }
        }

        return word
    }

    private fun isValidWord(text: String): Boolean {
        if (text.any { it in phoneticChars }) return false

        var cleaned = text.trim { c ->
            !c.isLetter() || c in "□■●•·◦▪▫○◯○⭕"
        }
        cleaned = cleaned.trimEnd { c -> !c.isLetter() }

        if (cleaned.length < 2 || cleaned.length > 25) return false
        if (!cleaned.first().isLetter() || !cleaned.last().isLetter()) return false

        val validPattern = Regex("^[a-zA-Z]+(?:['\\-][a-zA-Z]+)*$")
        if (!cleaned.matches(validPattern)) return false
        if (cleaned.length == 1) return false

        val vowels = cleaned.count { it.lowercaseChar() in "aeiou" }
        val letters = cleaned.count { it.isLetter() }
        if (letters > 0 && vowels.toFloat() / letters < 0.15f) return false

        if (cleaned.length > 3 && cleaned.all { it.isUpperCase() }) return false
        if (cleaned.length >= 3 && cleaned.all { it.equals(cleaned[0], ignoreCase = true) }) return false

        return true
    }
}
