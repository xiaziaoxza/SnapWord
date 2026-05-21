package com.snapword.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapword.data.local.WordEntity
import com.snapword.data.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val words: List<String> = emptyList(),
    val translations: Map<String, String?> = emptyMap(),
    val savedWordIds: Set<String> = emptySet()  // words already in vocab
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResultUiState())
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    fun setWords(words: List<String>) {
        val translations = repository.lookupWithFallback(words)
            .associate { it.first to it.second }

        viewModelScope.launch {
            val existing = repository.findExisting(words)
            _state.value = ResultUiState(
                words = words,
                translations = translations,
                savedWordIds = existing.map { it.word }.toSet()
            )
        }
    }

    fun saveWord(word: String) {
        viewModelScope.launch {
            val existing = repository.findExisting(listOf(word))
            if (existing.isEmpty()) {
                val entity = WordEntity(
                    word = word,
                    translation = _state.value.translations[word]
                )
                repository.addWord(entity)
                _state.value = _state.value.copy(
                    savedWordIds = _state.value.savedWordIds + word
                )
            }
        }
    }
}
