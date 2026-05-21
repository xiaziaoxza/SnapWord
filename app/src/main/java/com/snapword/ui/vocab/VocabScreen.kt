package com.snapword.ui.vocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.snapword.data.local.WordEntity
import com.snapword.ui.components.WordCard
import com.snapword.util.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabScreen(
    viewModel: VocabViewModel = hiltViewModel(),
    onReviewClick: () -> Unit,
    onWordClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生词本 (${state.words.size})") },
                actions = {
                    if (state.selectedIds.isNotEmpty()) {
                        IconButton(onClick = {
                            val words = viewModel.getSelectedWords()
                            ShareUtil.shareWords(context, words)
                            viewModel.clearSelection()
                        }) {
                            Icon(Icons.Default.Share, "分享")
                        }
                    }
                    IconButton(onClick = onReviewClick) {
                        Text("复习")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (state.words.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "生词本还没有单词\n拍照添加一些吧",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.words, key = { it.id }) { word: WordEntity ->
                    WordCard(
                        word = word.word,
                        translation = word.translation,
                        mastered = word.mastered,
                        onClick = {
                            if (state.selectedIds.isNotEmpty()) {
                                viewModel.toggleSelection(word.id)
                            } else {
                                onWordClick(word.word)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
