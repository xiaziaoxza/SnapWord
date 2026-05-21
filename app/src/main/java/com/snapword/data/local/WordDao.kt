package com.snapword.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM word_entries ORDER BY createdAt DESC")
    fun getAll(): Flow<List<WordEntity>>

    @Query("SELECT * FROM word_entries WHERE mastered = 0 ORDER BY createdAt DESC")
    fun getActive(): Flow<List<WordEntity>>

    @Query("SELECT * FROM word_entries WHERE word = :word LIMIT 1")
    suspend fun findByWord(word: String): WordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Query("UPDATE word_entries SET mastered = :mastered WHERE id = :id")
    suspend fun setMastered(id: Long, mastered: Boolean)

    @Query("DELETE FROM word_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM word_entries WHERE word IN (:words)")
    suspend fun findExisting(words: List<String>): List<WordEntity>
}
