package com.snapword.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapword.data.local.ReviewRecordEntity
import com.snapword.data.local.WordEntity
import com.snapword.data.repository.WordRepository
import com.snapword.domain.model.ReviewFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val words: List<WordEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val completed: Boolean = false,
    val reviewedCount: Int = 0,
    val totalCount: Int = 0
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: WordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val dueWords = repository.getDueWords()
            if (dueWords.isEmpty()) {
                _state.value = _state.value.copy(completed = true)
            } else {
                _state.value = _state.value.copy(
                    words = dueWords,
                    totalCount = dueWords.size
                )
            }
        }
    }

    fun flip() {
        _state.value = _state.value.copy(isFlipped = !_state.value.isFlipped)
    }

    fun recordFeedback(feedback: ReviewFeedback) {
        val current = _state.value
        val word = current.words.getOrNull(current.currentIndex) ?: return

        viewModelScope.launch {
            val intervalHours = when (feedback) {
                ReviewFeedback.REMEMBERED -> 24 * 3  // 3 days
                ReviewFeedback.FUZZY -> 12            // 12 hours
                ReviewFeedback.FORGOT -> 2             // 2 hours
            }

            repository.addReview(
                ReviewRecordEntity(
                    wordId = word.id,
                    feedback = feedback.name.lowercase(),
                    nextReviewAt = System.currentTimeMillis() + intervalHours * 3600_000L,
                    intervalHours = intervalHours
                )
            )

            val nextIndex = current.currentIndex + 1
            if (nextIndex >= current.words.size) {
                _state.value = current.copy(
                    completed = true,
                    reviewedCount = current.totalCount
                )
            } else {
                _state.value = current.copy(
                    currentIndex = nextIndex,
                    isFlipped = false,
                    reviewedCount = nextIndex
                )
            }
        }
    }
}
