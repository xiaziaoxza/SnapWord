package com.snapword.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapword.data.ocr.OcrEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeUiState(
    val isProcessing: Boolean = false,
    val recognizedWords: List<String>? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ocrEngine: OcrEngine
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true, error = null)
            try {
                val words = withContext(Dispatchers.IO) { ocrEngine.recognize(bitmap) }
                if (words.isEmpty()) {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        error = "没有识别到英文单词，请确保图片中有清晰的英文文字"
                    )
                } else {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        recognizedWords = words
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = "识别失败: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearResult() {
        _state.value = HomeUiState()
    }
}
