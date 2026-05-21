package com.snapword.util

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var speechRate = 1.0f
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            ready = (status == TextToSpeech.SUCCESS)
        }
        tts?.language = Locale.US
    }

    fun speak(text: String) {
        val t = tts ?: return
        // Guard: TTS must be initialized before speaking
        if (!ready) {
            // Retry language setting (some engines need it after init)
            t.language = Locale.US
        }
        t.setSpeechRate(speechRate)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "snapword_tts_$text")
    }

    fun setRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
