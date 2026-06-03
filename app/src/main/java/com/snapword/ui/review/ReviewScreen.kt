package com.snapword.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // ═══════════════════════════════════════════
    // Config dialog
    // ═══════════════════════════════════════════
    if (state.showConfig) {
        ReviewConfigDialog(
            totalCount = state.totalCount,
            bucketConfig = state.bucketConfig,
            onTotalCountChange = { viewModel.updateTotalCount(it) },
            onBucketConfigChange = { viewModel.updateBucketConfig(it) },
            onDismiss = { viewModel.dismissConfig() },
            onStart = { viewModel.startReview() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("复习") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (!state.completed && state.words.isNotEmpty()) {
                        IconButton(onClick = { viewModel.showConfig() }) {
                            Icon(Icons.Filled.Settings, "复习设置")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            // ═══════════════════════════════════════════
            // Completed
            // ═══════════════════════════════════════════
            state.completed -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "复习完成!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val stats = state.sessionStats
                        Text("正确: ${stats.correct}  错误/跳过: ${stats.incorrect}")
                        if (stats.total > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val rate = stats.correct * 100 / stats.total
                            Text(
                                "正确率: $rate%",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (rate >= 80)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = { viewModel.showConfig() }) {
                            Text("再复习一组")
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════
            // Loading / empty
            // ═══════════════════════════════════════════
            state.words.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无复习单词，先去拍照添加吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ═══════════════════════════════════════════
            // Active review
            // ═══════════════════════════════════════════
            else -> {
                val word = state.words[state.currentIndex]

                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress
                    LinearProgressIndicator(
                        progress = { state.currentIndex.toFloat() / state.words.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${state.currentIndex + 1} / ${state.words.size}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Word card
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(0.4f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (word.forgettingDays > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "遗忘 ${word.forgettingDays} 天",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Answer feedback
                    if (state.showAnswer) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isCorrect == true)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (state.isCorrect == true) "✓ 正确!" else "✗ 跳过",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = word.translation ?: "暂无释义",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Input field
                    if (!state.showAnswer) {
                        OutlinedTextField(
                            value = state.userInput,
                            onValueChange = { viewModel.onInputChanged(it) },
                            label = { Text("输入中文翻译") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.submitAnswer() }
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Action buttons
                    if (!state.showAnswer) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.skipWord() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("跳过")
                            }
                            Button(
                                onClick = { viewModel.submitAnswer() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = state.userInput.isNotBlank()
                            ) {
                                Text("提交")
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.nextWord() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val isLast = state.currentIndex + 1 >= state.words.size
                            Text(if (isLast) "完成" else "下一词")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// Config Dialog
// ══════════════════════════════════════════════════════════════

@Composable
fun ReviewConfigDialog(
    totalCount: Int,
    bucketConfig: String,
    onTotalCountChange: (Int) -> Unit,
    onBucketConfigChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("复习设置", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "总复习词数",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = totalCount.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { onTotalCountChange(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "各天数段配额",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "格式: 最大天数:词数, 逗号分隔",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = bucketConfig,
                    onValueChange = { onBucketConfigChange(it) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("1:5,3:5,7:5,14:3,30:2") }
                )
            }
        },
        confirmButton = {
            Button(onClick = onStart) { Text("开始复习") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("返回") }
        }
    )
}
