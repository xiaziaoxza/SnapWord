package com.snapword.domain.model

import com.snapword.data.local.WordEntity

data class Word(
    val id: Long = 0,
    val text: String,
    val translation: String? = null,
    val phonetic: String? = null,
    val exampleSentence: String? = null,
    val mastered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReviewFeedback(val label: String) {
    REMEMBERED("记住了"),
    FUZZY("模糊"),
    FORGOT("没记住")
}

fun WordEntity.toDomain(): Word = Word(
    id = id,
    text = word,
    translation = translation,
    phonetic = phonetic,
    exampleSentence = exampleSentence,
    mastered = mastered,
    createdAt = createdAt
)

fun Word.toEntity(): WordEntity = WordEntity(
    id = id,
    word = text,
    translation = translation,
    phonetic = phonetic,
    exampleSentence = exampleSentence,
    createdAt = createdAt,
    mastered = mastered
)
