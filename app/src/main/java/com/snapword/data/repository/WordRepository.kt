package com.snapword.data.repository

import com.snapword.data.local.AppDatabase
import com.snapword.data.local.Dictionary
import com.snapword.data.local.ReviewRecordEntity
import com.snapword.data.local.WordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Singleton
class WordRepository @Inject constructor(
    private val db: AppDatabase,
    private val dictionary: Dictionary,
    private val settings: SettingsRepository
) {
    private val wordDao = db.wordDao()
    private val reviewDao = db.reviewDao()

    val vocabFlow: Flow<List<WordEntity>> = wordDao.getAll()
    val activeFlow: Flow<List<WordEntity>> = wordDao.getActive()

    suspend fun findExisting(words: List<String>): List<WordEntity> =
        wordDao.findExisting(words)

    suspend fun addWord(word: WordEntity): Long =
        wordDao.insert(word)

    suspend fun updateWord(word: WordEntity) =
        wordDao.update(word)

    suspend fun deleteWord(id: Long) =
        wordDao.delete(id)

    suspend fun setMastered(id: Long, mastered: Boolean) =
        wordDao.setMastered(id, mastered)

    suspend fun addReview(record: ReviewRecordEntity) =
        reviewDao.insert(record)

    // ══════════════════════════════════════════════════════════════
    // 复习：天数递增
    // ══════════════════════════════════════════════════════════════

    suspend fun incrementForgettingDays() {
        val cutoff = System.currentTimeMillis() - 24 * 3600_000L
        wordDao.incrementForgettingDays(cutoff)
    }

    // ══════════════════════════════════════════════════════════════
    // 复习：Ebbinghaus 加权抽词
    // ══════════════════════════════════════════════════════════════

    /**
     * Select words for review using Ebbinghaus-based probability weighting.
     *
     * @param buckets list of (maxForgettingDays, desiredCount) pairs.
     *   e.g. [(1,5), (3,5), (7,5), (14,3), (30,2)]
     * @return selected words in review order (shuffled within buckets)
     */
    suspend fun selectReviewWords(
        buckets: List<Pair<Int, Int>>
    ): List<WordEntity> {
        val allCandidates = wordDao.getReviewCandidates()
        if (allCandidates.isEmpty()) return emptyList()

        // Group candidates by forgettingDays bucket
        val selected = mutableListOf<WordEntity>()
        val used = mutableSetOf<Long>()

        for ((maxDays, wantCount) in buckets) {
            if (wantCount <= 0) continue

            val minDays = buckets
                .takeWhile { it.first < maxDays }
                .lastOrNull()?.first?.plus(1) ?: 0

            val pool = allCandidates.filter {
                it.id !in used && it.forgettingDays in minDays..maxDays
            }

            if (pool.isEmpty()) {
                // Redistribute: take from next bucket if available
                val overflow = allCandidates.filter {
                    it.id !in used && it.forgettingDays > maxDays
                }.take(wantCount)

                for (w in overflow) {
                    if (selected.size >= wantCount) break
                    selected.add(w)
                    used.add(w.id)
                }
                continue
            }

            val n = min(wantCount, pool.size)
            val picked = weightedSample(pool, n)
            selected.addAll(picked)
            used.addAll(picked.map { it.id })
        }

        // Shuffle so the order isn't purely by forgetting days
        return selected.shuffled()
    }

    /**
     * Weighted random sampling: P(word) ∝ 1 - e^(-forgettingDays / EBBINGHAUS_S)
     * Higher forgettingDays → higher probability.
     */
    private fun weightedSample(
        candidates: List<WordEntity>,
        n: Int
    ): List<WordEntity> {
        if (candidates.size <= n) return candidates

        val weights = candidates.map { 1.0 - exp(-it.forgettingDays.toDouble() / EBBINGHAUS_S) }
        val totalWeight = weights.sum()

        val result = mutableListOf<WordEntity>()
        val remaining = candidates.toMutableList()
        val remainingWeights = weights.toMutableList()
        var currentTotal = totalWeight

        repeat(n) {
            if (remaining.isEmpty()) return@repeat
            var r = Random.nextDouble() * currentTotal
            var idx = 0
            while (idx < remaining.size - 1 && r > remainingWeights[idx]) {
                r -= remainingWeights[idx]
                idx++
            }
            result.add(remaining.removeAt(idx))
            currentTotal -= remainingWeights.removeAt(idx)
        }

        return result
    }

    // ══════════════════════════════════════════════════════════════
    // 翻译 + 匹配
    // ══════════════════════════════════════════════════════════════

    fun translate(word: String): String? = dictionary.lookup(word)

    fun lookupWithFallback(words: List<String>): List<Pair<String, String?>> {
        return words.map { it to dictionary.lookup(it) }
    }

    /**
     * Check if user input matches any keyword in the translation.
     * Translation "移除；去除；删除" → split by ； → ["移除","去除","删除"]
     * User input "移除" matches "移除" → correct.
     */
    fun checkTranslation(input: String, translation: String): Boolean {
        val userInput = input.trim().lowercase()
        if (userInput.isEmpty()) return false

        val keywords = translation
            .split("；", ";", "，", ",", "、")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        return keywords.any { kw -> userInput.contains(kw) || kw.contains(userInput) }
    }

    // ══════════════════════════════════════════════════════════════
    // 复习反馈
    // ══════════════════════════════════════════════════════════════

    suspend fun recordReviewCorrect(word: WordEntity) {
        val now = System.currentTimeMillis()
        wordDao.update(
            word.copy(
                lastReviewedAt = now,
                forgettingDays = 0,
                reviewCount = word.reviewCount + 1
            )
        )
        reviewDao.insert(
            ReviewRecordEntity(
                wordId = word.id,
                feedback = "correct",
                reviewedAt = now
            )
        )
    }

    suspend fun recordReviewSkipped(word: WordEntity) {
        val now = System.currentTimeMillis()
        wordDao.update(
            word.copy(
                lastReviewedAt = now,
                forgettingDays = word.forgettingDays + 1,
                reviewCount = word.reviewCount + 1
            )
        )
        reviewDao.insert(
            ReviewRecordEntity(
                wordId = word.id,
                feedback = "skipped",
                reviewedAt = now
            )
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Fallback for compatibility (old getDueWords removed, kept stub)
    // ══════════════════════════════════════════════════════════════

    suspend fun getDueWords(now: Long = System.currentTimeMillis()): List<WordEntity> {
        return wordDao.getReviewCandidates()
    }

    companion object {
        /** Ebbinghaus curve shape constant: half-life ≈ 7 days */
        const val EBBINGHAUS_S = 7.0

        /** Default bucket config: (maxForgettingDays, count) */
        val DEFAULT_BUCKETS = listOf(1 to 5, 3 to 5, 7 to 5, 14 to 3, 30 to 2)

        fun parseBuckets(raw: String): List<Pair<Int, Int>> {
            return raw.split(",")
                .mapNotNull { part ->
                    val kv = part.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
                    val days = kv[0].trim().toIntOrNull() ?: return@mapNotNull null
                    val count = kv[1].trim().toIntOrNull() ?: return@mapNotNull null
                    days to count
                }
                .ifEmpty { DEFAULT_BUCKETS }
        }
    }
}
