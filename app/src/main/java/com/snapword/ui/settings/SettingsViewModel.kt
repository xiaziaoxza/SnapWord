package com.snapword.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapword.data.repository.SettingsRepository
import com.snapword.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val speechRate: Float = 1.0f,
    val recognitionLang: String = "en"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val ttsManager: TtsManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.speechRate.collect { rate ->
                _state.value = _state.value.copy(speechRate = rate)
            }
        }
        viewModelScope.launch {
            settingsRepo.recognitionLang.collect { lang ->
                _state.value = _state.value.copy(recognitionLang = lang)
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsRepo.setSpeechRate(rate)
            ttsManager.setRate(rate)
        }
    }

    fun setRecognitionLang(lang: String) {
        viewModelScope.launch {
            settingsRepo.setRecognitionLang(lang)
        }
    }
}
