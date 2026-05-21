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
    @ApplicationContext private val context: Context
) {
    private val wordMap: MutableMap<String, String> = HashMap()

    init {
        loadDictionary()
    }

    private fun loadDictionary() {
        try {
            val inputStream = context.assets.open("dictionary.json")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val json = reader.readText()
            reader.close()
            inputStream.close()

            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val word = obj.getString("w")
                val translation = obj.getString("t")
                wordMap[word] = translation
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun lookup(word: String): String? {
        return wordMap[word.lowercase().trim()]
    }

    fun lookupAll(words: List<String>): List<Pair<String, String>> {
        return words.mapNotNull { word ->
            lookup(word)?.let { word to it }
        }
    }

    val size: Int get() = wordMap.size
}
