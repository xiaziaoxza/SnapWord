package com.snapword.data.repository

import com.snapword.data.local.AppDatabase
import com.snapword.data.local.Dictionary
import com.snapword.data.local.ReviewRecordEntity
import com.snapword.data.local.WordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val db: AppDatabase,
    private val dictionary: Dictionary
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

    suspend fun getDueWords(now: Long = System.currentTimeMillis()): List<WordEntity> {
        val dueRecords = reviewDao.getDueForReview(now)
        val wordIds = dueRecords.map { it.wordId }.distinct()
        if (wordIds.isEmpty()) {
            return wordDao.getActive().let { flow ->
                var result = emptyList<WordEntity>()
                flow.collect { result = it; return@collect }
                result
            }
        }
        return wordDao.getAll().let { flow ->
            var all = emptyList<WordEntity>()
            flow.collect { all = it; return@collect }
            all.filter { it.id in wordIds }
        }
    }

    /** Look up translation from local dictionary. Returns null if not found. */
    fun translate(word: String): String? = dictionary.lookup(word)

    /** Batch lookup with existing DB words merged in */
    fun lookupWithFallback(words: List<String>): List<Pair<String, String?>> {
        return words.map { word ->
            word to dictionary.lookup(word)
        }
    }
}
