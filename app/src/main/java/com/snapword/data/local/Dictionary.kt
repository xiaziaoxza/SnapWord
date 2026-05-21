package com.snapword.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Dictionary @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordListManager: WordListManager
) {
    private val builtinMap: MutableMap<String, String> = HashMap()

    init {
        loadBuiltin()
    }

    private fun loadBuiltin() {
        try {
            val stream = context.assets.open("dictionary.json")
            val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
            val json = reader.readText()
            reader.close()
            stream.close()

            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                builtinMap[obj.getString("w")] = obj.getString("t")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Lookup: checks downloaded lists first, then built-in */
    fun lookup(word: String): String? {
        val key = word.lowercase().trim()
        // Check downloaded lists
        val downloaded = wordListManager.loadDownloadedWords()
        downloaded[key]?.let { return it }
        // Fall back to built-in
        return builtinMap[key]
    }

    val builtinSize: Int get() = builtinMap.size
}
