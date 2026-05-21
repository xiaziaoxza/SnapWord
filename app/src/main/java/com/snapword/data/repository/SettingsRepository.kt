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
    }

    val speechRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[SPEECH_RATE] ?: 1.0f
    }

    val recognitionLang: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[RECOGNITION_LANG] ?: "en"
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setRecognitionLang(lang: String) {
        context.dataStore.edit { it[RECOGNITION_LANG] = lang }
    }
}
