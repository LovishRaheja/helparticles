package com.lovishraheja27.helparticles.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lovishraheja27.helparticles.ui.components.ErrorContent
import com.lovishraheja27.helparticles.ui.components.MarkdownText
import com.lovishraheja27.shared.models.Article
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    viewModel: ArticleDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.article?.title ?: "Loading...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    if (uiState.fromCache) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Showing cached data",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Crossfade(
            targetState = when {
                uiState.error != null -> DetailContentState.ERROR
                uiState.isLoading && uiState.article == null -> DetailContentState.LOADING
                uiState.article != null -> DetailContentState.CONTENT
                else -> DetailContentState.LOADING
            },
            label = "detail_content_animation",
            modifier = Modifier.padding(paddingValues)
        ) { state ->
            when (state) {
                DetailContentState.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                DetailContentState.ERROR -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DetailContentState.CONTENT -> {
                    ArticleContent(
                        article = uiState.article!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private enum class DetailContentState {
    LOADING, ERROR, CONTENT
}

@Composable
private fun ArticleContent(
    article: Article,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AssistChip(
                onClick = { },
                label = { Text(article.category) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            Text(
                text = formatDetailDate(article.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = article.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))

        MarkdownText(
            markdown = article.content,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Was this article helpful?",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { }) {
                        Text("Yes")
                    }
                    OutlinedButton(onClick = { }) {
                        Text("No")
                    }
                }
            }
        }
    }
}

private fun formatDetailDate(dateString: String): String {
    val instant = Instant.parse(dateString)

    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val month = localDateTime.month.name
        .lowercase()
        .replaceFirstChar { it.uppercase() }

    return "Updated $month ${localDateTime.dayOfMonth}, ${localDateTime.year}"
}