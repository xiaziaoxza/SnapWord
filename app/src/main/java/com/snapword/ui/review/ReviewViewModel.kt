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

// Ebbinghaus forgetting curve intervals (in hours)
// Stage 0: 20min, Stage 1: 1h, Stage 2: 8h, Stage 3: 1d,
// Stage 4: 2d, Stage 5: 6d, Stage 6: 14d, Stage 7: 30d
private val EBBINGHAUS_INTERVALS = longArrayOf(
    1L / 3,  // 20 minutes
    1,       // 1 hour
    8,       // 8 hours
    24,      // 1 day
    48,      // 2 days
    144,     // 6 days
    336,     // 14 days
    720      // 30 days
)

data class ReviewUiState(
    val words: List<WordEntity> = emptyList(),
    val currentIndex: Int = 0,
    val currentStage: Int = 0,
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
            // Ebbinghaus: advance or regress stage based on feedback
            var newStage = current.currentStage
            when (feedback) {
                ReviewFeedback.REMEMBERED -> {
                    newStage = (current.currentStage + 1).coerceIn(0, EBBINGHAUS_INTERVALS.size - 1)
                }
                ReviewFeedback.FUZZY -> {
                    newStage = (current.currentStage).coerceIn(0, EBBINGHAUS_INTERVALS.size - 1)
                }
                ReviewFeedback.FORGOT -> {
                    newStage = 0 // reset to beginning
                }
            }

            val intervalHours = EBBINGHAUS_INTERVALS[newStage].toInt()

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
                    currentStage = 0,  // reset stage for new word
                    isFlipped = false,
                    reviewedCount = nextIndex
                )
            }
        }
    }
}
