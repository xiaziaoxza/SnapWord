package com.snapword.domain.model

import com.snapword.data.local.WordEntity

data class Word(
    val id: Long = 0,
    val text: String,
    val translation: String? = null,
    val phonetic: String? = null,
    val exampleSentence: String? = null,
    val mastered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val forgettingDays: Int = 0,
    val reviewCount: Int = 0
)

fun WordEntity.toDomain(): Word = Word(
    id = id,
    text = word,
    translation = translation,
    phonetic = phonetic,
    exampleSentence = exampleSentence,
    mastered = mastered,
    createdAt = createdAt,
    lastReviewedAt = lastReviewedAt,
    forgettingDays = forgettingDays,
    reviewCount = reviewCount
)

fun Word.toEntity(): WordEntity = WordEntity(
    id = id,
    word = text,
    translation = translation,
    phonetic = phonetic,
    exampleSentence = exampleSentence,
    createdAt = createdAt,
    mastered = mastered,
    lastReviewedAt = lastReviewedAt,
    forgettingDays = forgettingDays,
    reviewCount = reviewCount
)
