package com.snapword.util

import android.content.Context
import android.content.Intent

object ShareUtil {
    fun shareWords(context: Context, words: List<Pair<String, String?>>) {
        val text = words.joinToString("\n\n") { (word, translation) ->
            "$word${translation?.let { " — $it" } ?: ""}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "我的生词本")
        }
        context.startActivity(Intent.createChooser(intent, "分享生词"))
    }
}
