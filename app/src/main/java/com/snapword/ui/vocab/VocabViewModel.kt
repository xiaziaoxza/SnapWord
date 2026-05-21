package com.snapword.ui.vocab

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

data class VocabUiState(
    val words: List<WordEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet()
)

@HiltViewModel
class VocabViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VocabUiState())
    val state: StateFlow<VocabUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.vocabFlow.collect { words ->
                _state.value = _state.value.copy(words = words)
            }
        }
    }

    fun deleteWord(id: Long) {
        viewModelScope.launch { repository.deleteWord(id) }
    }

    fun markMastered(id: Long) {
        viewModelScope.launch { repository.setMastered(id, true) }
    }

    fun toggleSelection(id: Long) {
        val current = _state.value.selectedIds
        _state.value = _state.value.copy(
            selectedIds = if (id in current) current - id else current + id
        )
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedIds = emptySet())
    }

    fun getSelectedWords(): List<Pair<String, String?>> {
        return _state.value.words
            .filter { it.id in _state.value.selectedIds }
            .map { it.word to it.translation }
    }
}
