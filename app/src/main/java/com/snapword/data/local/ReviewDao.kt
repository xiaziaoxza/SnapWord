package com.snapword.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Insert
    suspend fun insert(record: ReviewRecordEntity)

    @Query("""
        SELECT * FROM review_records
        WHERE wordId = :wordId
        ORDER BY reviewedAt DESC
        LIMIT 1
    """)
    suspend fun latestForWord(wordId: Long): ReviewRecordEntity?

    @Query("""
        SELECT * FROM review_records
        WHERE nextReviewAt IS NULL OR nextReviewAt <= :now
        ORDER BY reviewedAt ASC
    """)
    suspend fun getDueForReview(now: Long): List<ReviewRecordEntity>

    @Query("SELECT COUNT(*) FROM review_records WHERE wordId = :wordId")
    fun reviewCountForWord(wordId: Long): Flow<Int>
}
