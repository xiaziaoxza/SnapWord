package com.snapword.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val RECOGNITION_LANG = stringPreferencesKey("recognition_lang")
        val REVIEW_TOTAL_COUNT = stringPreferencesKey("review_total_count")
        val REVIEW_BUCKETS = stringPreferencesKey("review_buckets")
    }

    val speechRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[SPEECH_RATE] ?: 1.0f
    }

    val recognitionLang: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[RECOGNITION_LANG] ?: "en"
    }

    /** Total words per review session, default 20 */
    val reviewTotalCount: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[REVIEW_TOTAL_COUNT] ?: "20").toIntOrNull() ?: 20
    }

    /** Bucket config: "1:5,3:5,7:5,14:3,30:2" → pairs of (maxDays, count) */
    val reviewBuckets: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[REVIEW_BUCKETS] ?: "1:5,3:5,7:5,14:3,30:2"
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setRecognitionLang(lang: String) {
        context.dataStore.edit { it[RECOGNITION_LANG] = lang }
    }

    suspend fun setReviewTotalCount(count: Int) {
        context.dataStore.edit { it[REVIEW_TOTAL_COUNT] = count.toString() }
    }

    suspend fun setReviewBuckets(buckets: String) {
        context.dataStore.edit { it[REVIEW_BUCKETS] = buckets }
    }
}
