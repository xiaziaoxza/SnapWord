package com.snapword.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class WordListMeta(
    val id: String,
    val name: String,
    val description: String,
    val wordCount: Int,
    val size: Long,
    val url: String?,
    val audioUrl: String? = null,
    val builtin: Boolean,
    var isDownloaded: Boolean = false,
    var downloadProgress: Float = 0f
)

data class WordListState(
    val available: List<WordListMeta> = emptyList(),
    val downloading: Set<String> = emptySet(),
    val downloadedIds: Set<String> = emptySet()
)

@Singleton
class WordListManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wordListsDir = File(context.filesDir, "wordlists")
    private val _state = MutableStateFlow(WordListState())
    val state: StateFlow<WordListState> = _state.asStateFlow()

    init {
        wordListsDir.mkdirs()
        refreshDownloadedState()
    }

    private fun refreshDownloadedState() {
        val downloaded = wordListsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.toSet() ?: emptySet()
        _state.value = _state.value.copy(downloadedIds = downloaded)
    }

    /** Fetch manifest from GitHub and populate available word lists */
    suspend fun loadManifest() {
        withContext(Dispatchers.IO) {
            try {
                val json = downloadText(MANIFEST_URL)
                val arr = JSONArray(json)
                val lists = mutableListOf<WordListMeta>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    lists.add(
                        WordListMeta(
                            id = id,
                            name = obj.getString("name"),
                            description = obj.getString("description"),
                            wordCount = obj.getInt("wordCount"),
                            size = obj.getLong("size"),
                            url = obj.optString("url", null)?.ifBlank { null },
                            audioUrl = obj.optString("audioUrl", null)?.ifBlank { null },
                            builtin = obj.optBoolean("builtin", false),
                            isDownloaded = id in _state.value.downloadedIds || obj.optBoolean("builtin", false)
                        )
                    )
                }
                _state.value = _state.value.copy(available = lists)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Download a word list JSON file + optional audio pack */
    suspend fun download(list: WordListMeta) {
        if (list.url == null || list.builtin) return

        withContext(Dispatchers.IO) {
            val downloading = _state.value.downloading.toMutableSet()
            downloading.add(list.id)
            _state.value = _state.value.copy(downloading = downloading)

            try {
                val json = downloadText(list.url)
                val targetFile = File(wordListsDir, "${list.id}.json")
                targetFile.writeText(json, Charsets.UTF_8)

                // Download audio pack if available
                if (list.audioUrl != null) {
                    try {
                        downloadAndExtractAudio(list.audioUrl)
                    } catch (_: Exception) {
                        // Audio download failed — TTS fallback still works
                    }
                }

                refreshDownloadedState()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                downloading.remove(list.id)
                _state.value = _state.value.copy(downloading = downloading)
            }
        }
    }

    private fun downloadAndExtractAudio(audioUrl: String) {
        val audioDir = File(context.filesDir, "audio")
        audioDir.mkdirs()
        val zipFile = File(audioDir, "_temp.zip")
        try {
            val url = URL(audioUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 120000
            conn.inputStream.use { input ->
                zipFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // Extract zip
            java.util.zip.ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && (entry.name.endsWith(".mp3") || entry.name.endsWith(".ogg"))) {
                        val outFile = File(audioDir, File(entry.name).name)
                        zip.getInputStream(entry).use { it.copyTo(outFile.outputStream()) }
                    }
                }
            }
        } finally {
            zipFile.delete()
        }
    }

    /** Delete a downloaded word list */
    fun delete(listId: String) {
        val file = File(wordListsDir, "${listId}.json")
        if (file.exists()) file.delete()
        refreshDownloadedState()
    }

    /** Get all downloaded word lists as merged word->translation map */
    fun loadDownloadedWords(): Map<String, String> {
        val words = mutableMapOf<String, String>()
        val files = wordListsDir.listFiles()?.filter { it.extension == "json" } ?: return words
        for (file in files) {
            try {
                val json = file.readText(Charsets.UTF_8)
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val word = obj.getString("w")
                    val trans = obj.getString("t")
                    if (word !in words || trans.length > (words[word]?.length ?: 0)) {
                        words[word] = trans
                    }
                }
            } catch (_: Exception) {}
        }
        return words
    }

    val downloadedWordCount: Int
        get() = loadDownloadedWords().size

    private fun downloadText(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        return try {
            val stream = conn.inputStream
            val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
            val text = reader.readText()
            reader.close()
            stream.close()
            text
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/hiokanbd/SnapWord/main/wordlists/wordlist_manifest.json"
    }
}
