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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    // Regex to find all letter-sequences (handles apostrophe/hyphen within)
    // This replaces symbol-enumeration — □have → "have", .test → "test", 中文word → "word"
    private val wordPattern = Regex("[a-zA-Z]+(?:['\\-][a-zA-Z]+)*")

    // Characters OCR misreads □ checkbox as (still checked as fallback for merged tokens)
    private val checkboxMisreads = setOf('O', 'D', 'C', 'Q', '0')
    private val checkboxMisreadsLower = setOf('o', 'd', 'c', 'q', '0')

    // ── Grid scan thresholds ──
    private val gridMinWords = 5  // If full-image scan finds < 5 words, try grid

    private suspend fun ensureInitialized(): Boolean {
        if (initialized) return true
        return withContext(Dispatchers.Default) {
            ocr.initModelFromAssert(
                context.assets,
                ModelType.Mobile,
                ImageSize.Size1080,
                Device.CPU
            ).also { initialized = it }
        }
    }

    suspend fun recognize(bitmap: Bitmap): List<String> {
        if (!ensureInitialized()) return emptyList()

        // ── Step 1: Full-image scan with upscaling ──
        val scaled = scaleUpForOCR(bitmap)
        val fullWords = withContext(Dispatchers.Default) {
            extractWords(ocr.detectBitmap(scaled))
        }

        // ── Step 2: If few words found, try grid scan for dense/diverse layouts ──
        if (fullWords.size < gridMinWords) {
            val gridWords = recognizeGrid(scaled)
            return mergeResults(fullWords, gridWords)
        }

        return fullWords
    }

    // ============================================================
    // 图片预处理：放大低分辨率图片提升小字识别率
    // ============================================================

    private fun scaleUpForOCR(src: Bitmap): Bitmap {
        val minDim = 1200
        val w = src.width
        val h = src.height

        // If already large enough, return as-is (but ensure ARGB_8888 for ncnn)
        if (w >= minDim && h >= minDim) {
            return if (src.config == Bitmap.Config.ARGB_8888) src
            else src.copy(Bitmap.Config.ARGB_8888, false)
        }

        // Scale up so shorter side reaches minDim
        val scale = if (w < h) minDim.toFloat() / w else minDim.toFloat() / h
        val newW = (w * scale).toInt()
        val newH = (h * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        // Clean up if we created a new bitmap
        if (scaled !== src && src.config != Bitmap.Config.ARGB_8888) {
            // src will be garbage collected, no explicit recycle needed
        }
        return scaled
    }

    // ============================================================
    // 分块识别：将图片分割成网格，多线程并行识别
    // ============================================================

    private suspend fun recognizeGrid(bitmap: Bitmap): List<String> = coroutineScope {
        val w = bitmap.width
        val h = bitmap.height

        // Determine grid: 1 row for narrow images, 2 rows for tall ones
        val cols = if (w > 1200) 2 else 1
        val rows = if (h > 1600) 2 else 1

        if (cols == 1 && rows == 1) return@coroutineScope emptyList()

        val tileW = w / cols
        val tileH = h / rows
        val overlap = 0.15f  // 15% overlap to avoid cutting words

        val tiles = mutableListOf<Bitmap>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = (c * tileW - if (c > 0) tileW * overlap else 0).toInt().coerceAtLeast(0)
                val y = (r * tileH - if (r > 0) tileH * overlap else 0).toInt().coerceAtLeast(0)
                val rw = ((tileW * (1 + overlap * 2)).toInt()).coerceAtMost(w - x)
                val rh = ((tileH * (1 + overlap * 2)).toInt()).coerceAtMost(h - y)
                if (rw > 100 && rh > 100) {
                    tiles.add(Bitmap.createBitmap(bitmap, x, y, rw, rh))
                }
            }
        }

        // Process all tiles in parallel
        val results = tiles.map { tile ->
            async(Dispatchers.Default) {
                extractWords(ocr.detectBitmap(tile))
            }
        }.awaitAll()

        results.flatten()
    }

    // ============================================================
    // 核心：从 OcrResult 提取单词
    // ============================================================

    private fun extractWords(result: com.equationl.ncnnandroidppocr.bean.OcrResult?): List<String> {
        if (result == null) return emptyList()

        val words = mutableListOf<String>()

        for (line in result.textLines) {
            val raw = line.text

            // Find all letter-sequences using regex — this naturally strips
            // □, ., Chinese chars, numbers, symbols from around words
            val matches = wordPattern.findAll(raw)
            for (match in matches) {
                val token = match.value
                val corrected = correctCheckboxArtifact(token)
                if (isValidWord(corrected)) {
                    words.add(corrected)
                }
            }
        }

        return words
    }

    // ============================================================
    // 结果合并去重
    // ============================================================

    private fun mergeResults(vararg lists: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<String>()
        for (list in lists) {
            for (word in list) {
                if (seen.add(word.lowercase())) {
                    merged.add(word)
                }
            }
        }
        return merged.take(50)
    }

    // ============================================================
    // 复选框误读纠正（兜底）
    // ============================================================

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

    // ============================================================
    // 单词有效性检查
    // ============================================================

    private fun isValidWord(text: String): Boolean {
        if (text.any { it in phoneticChars }) return false
        if (text.none { it.isLetter() }) return false

        var cleaned = text.trim { c ->
            !c.isLetter() || c in "□■●•·◦▪▫○◯⭕☐☑☒✓✔✕✗✘◻◼◽◾▢▣▤▥▦▧▨▩⬜⬛❑❒"
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
