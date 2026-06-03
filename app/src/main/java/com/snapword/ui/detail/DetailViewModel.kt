package com.snapword.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapword.data.local.WordEntity
import com.snapword.data.repository.WordRepository
import com.snapword.util.AudioManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val word: String = "",
    val translation: String? = null,
    val isSaved: Boolean = false,
    val showSavedHint: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: WordRepository,
    private val audioManager: AudioManager
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun loadWord(word: String) {
        _state.value = DetailUiState(
            word = word,
            translation = repository.translate(word)
        )
        viewModelScope.launch {
            val existing = repository.findExisting(listOf(word))
            _state.value = _state.value.copy(
                isSaved = existing.isNotEmpty()
            )
        }
    }

    fun speak() {
        audioManager.speak(_state.value.word)
    }

    fun saveToVocab() {
        viewModelScope.launch {
            val existing = repository.findExisting(listOf(_state.value.word))
            if (existing.isEmpty()) {
                repository.addWord(
                    WordEntity(
                        word = _state.value.word,
                        translation = _state.value.translation
                    )
                )
                _state.value = _state.value.copy(isSaved = true, showSavedHint = true)
            }
        }
    }

    fun dismissHint() {
        _state.value = _state.value.copy(showSavedHint = false)
    }
}
