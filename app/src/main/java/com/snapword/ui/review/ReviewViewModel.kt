package com.snapword.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapword.data.local.WordEntity
import com.snapword.data.repository.WordRepository
import com.snapword.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val words: List<WordEntity> = emptyList(),
    val currentIndex: Int = 0,
    val userInput: String = "",
    val showAnswer: Boolean = false,
    val isCorrect: Boolean? = null,  // null = not yet judged
    val completed: Boolean = false,
    val sessionStats: SessionStats = SessionStats(),
    val showConfig: Boolean = false,
    // Config
    val totalCount: Int = 20,
    val bucketConfig: String = "1:5,3:5,7:5,14:3,30:2",
)

data class SessionStats(
    val correct: Int = 0,
    val incorrect: Int = 0,
    val total: Int = 0
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: WordRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Load saved config
            val savedTotal = settings.reviewTotalCount.first()
            val savedBuckets = settings.reviewBuckets.first()
            _state.value = _state.value.copy(
                totalCount = savedTotal,
                bucketConfig = savedBuckets,
                showConfig = true
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Config
    // ══════════════════════════════════════════════════════════════

    fun updateTotalCount(count: Int) {
        _state.value = _state.value.copy(totalCount = count.coerceIn(1, 100))
    }

    fun updateBucketConfig(config: String) {
        _state.value = _state.value.copy(bucketConfig = config)
    }

    fun showConfig() {
        _state.value = _state.value.copy(showConfig = true)
    }

    fun dismissConfig() {
        _state.value = _state.value.copy(showConfig = false)
    }

    fun startReview() {
        viewModelScope.launch {
            val s = _state.value
            // Save config
            settings.setReviewTotalCount(s.totalCount)
            settings.setReviewBuckets(s.bucketConfig)

            // Increment forgetting days for due words
            repository.incrementForgettingDays()

            // Parse buckets and select words
            val buckets = WordRepository.parseBuckets(s.bucketConfig)
            val words = repository.selectReviewWords(buckets)

            _state.value = s.copy(
                words = words,
                currentIndex = 0,
                userInput = "",
                showAnswer = false,
                isCorrect = null,
                completed = words.isEmpty(),
                showConfig = false,
                sessionStats = SessionStats(total = words.size)
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Input
    // ══════════════════════════════════════════════════════════════

    fun onInputChanged(input: String) {
        _state.value = _state.value.copy(userInput = input, isCorrect = null)
    }

    fun submitAnswer() {
        val s = _state.value
        if (s.showAnswer) return

        val word = currentWord ?: return
        val translation = word.translation ?: ""
        val input = s.userInput.trim()

        val correct = if (input.isNotEmpty() && translation.isNotEmpty()) {
            repository.checkTranslation(input, translation)
        } else false

        _state.value = s.copy(
            showAnswer = true,
            isCorrect = correct
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Skip
    // ══════════════════════════════════════════════════════════════

    fun skipWord() {
        val s = _state.value
        val word = currentWord ?: return

        viewModelScope.launch {
            repository.recordReviewSkipped(word)
            advance(s, correct = false)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Next (after showing answer)
    // ══════════════════════════════════════════════════════════════

    fun nextWord() {
        val s = _state.value
        val word = currentWord ?: return
        val wasCorrect = s.isCorrect == true

        viewModelScope.launch {
            if (wasCorrect) {
                repository.recordReviewCorrect(word)
            } else {
                repository.recordReviewSkipped(word)
            }
            advance(s, correct = wasCorrect)
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════

    val currentWord: WordEntity?
        get() = _state.value.words.getOrNull(_state.value.currentIndex)

    private fun advance(s: ReviewUiState, correct: Boolean) {
        val next = s.currentIndex + 1
        val stats = s.sessionStats.copy(
            correct = if (correct) s.sessionStats.correct + 1 else s.sessionStats.correct,
            incorrect = if (!correct) s.sessionStats.incorrect + 1 else s.sessionStats.incorrect
        )
        if (next >= s.words.size) {
            _state.value = s.copy(
                completed = true,
                sessionStats = stats
            )
        } else {
            _state.value = s.copy(
                currentIndex = next,
                userInput = "",
                showAnswer = false,
                isCorrect = null,
                sessionStats = stats
            )
        }
    }
}
