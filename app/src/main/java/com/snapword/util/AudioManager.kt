package com.snapword.util

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRate = 1.0f
    private var mediaPlayer: MediaPlayer? = null

    // Map inflected forms → base word for audio lookup
    private val inflectedMap: MutableMap<String, String> = HashMap()
    private val audioDir: File = File(context.filesDir, "audio")

    init {
        audioDir.mkdirs()
        loadIndex()

        tts = TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) tts?.language = Locale.US
        }
        tts?.language = Locale.US
    }

    // ── Load audio index (built-in + downloaded) ──────────────────

    private fun loadIndex() {
        try {
            val json = context.assets.open("audio/index.json")
                .bufferedReader().readText()
            val obj = JSONObject(json)
            val map = obj.optJSONObject("inflected_map") ?: JSONObject()
            val keys = map.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                inflectedMap[k] = map.getString(k)
            }
        } catch (_: Exception) {
            // No built-in audio index — fine, TTS fallback works
        }
    }

    // ── Resolve audio file: asset or downloaded ───────────────────

    private fun resolveAudioPath(word: String): String? {
        val lower = word.lowercase()
        val candidates = mutableListOf(lower)

        // If this is an inflected form, use the base word's audio
        inflectedMap[lower]?.let { base -> candidates.add(0, base) }

        for (candidate in candidates) {
            // Check downloaded audio first
            val file = File(audioDir, "$candidate.mp3")
            if (file.exists()) return file.absolutePath

            // Check built-in assets
            try {
                context.assets.openFd("audio/$candidate.mp3").close()
                return "asset:audio/$candidate.mp3"
            } catch (_: Exception) {}
        }
        return null
    }

    // ── Public API ─────────────────────────────────────────────────

    suspend fun speak(text: String) {
        val audioPath = resolveAudioPath(text)

        if (audioPath != null) {
            playAudioFile(audioPath)
        } else {
            speakWithTts(text)
        }
    }

    fun setRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            reset()
        }
        tts?.stop()
    }

    fun shutdown() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.shutdown()
        tts = null
    }

    // ── Save downloaded audio to internal storage ─────────────────

    fun saveAudio(word: String, oggData: ByteArray) {
        val file = File(audioDir, "${word.lowercase()}.mp3")
        file.writeBytes(oggData)
    }

    fun batchSaveAudio(files: Map<String, ByteArray>) {
        for ((word, data) in files) {
            saveAudio(word, data)
        }
    }

    // ── Private helpers ────────────────────────────────────────────

    private suspend fun playAudioFile(path: String) = withContext(Dispatchers.Default) {
        try {
            val mp = MediaPlayer()
            mediaPlayer = mp

            if (path.startsWith("asset:")) {
                val assetPath = path.removePrefix("asset:")
                val afd = context.assets.openFd(assetPath)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            } else {
                mp.setDataSource(path)
            }

            mp.prepare()
            mp.start()
            mp.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun speakWithTts(text: String) {
        val t = tts ?: return
        if (!ttsReady) {
            t.language = Locale.US
        }
        t.setSpeechRate(speechRate)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "snapword_$text")
    }
}
