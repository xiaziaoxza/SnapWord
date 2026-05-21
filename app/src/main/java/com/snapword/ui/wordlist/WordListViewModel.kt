package com.snapword.ui.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapword.data.local.WordListManager
import com.snapword.data.local.WordListMeta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordListManager: WordListManager
) : ViewModel() {

    val state = wordListManager.state

    init {
        viewModelScope.launch {
            wordListManager.loadManifest()
        }
    }

    fun download(list: WordListMeta) {
        viewModelScope.launch {
            wordListManager.download(list)
        }
    }

    fun delete(listId: String) {
        wordListManager.delete(listId)
    }
}
