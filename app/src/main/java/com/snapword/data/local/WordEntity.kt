package com.snapword.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_entries")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val translation: String? = null,
    val phonetic: String? = null,
    val exampleSentence: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val mastered: Boolean = false
)
