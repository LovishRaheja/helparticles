package com.lovishraheja27.helparticles.ui.list

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.lovishraheja27.helparticles.ui.components.EmptyState
import com.lovishraheja27.helparticles.ui.components.ErrorContent
import com.lovishraheja27.shared.models.Article
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesListScreen(
    viewModel: ArticlesListViewModel,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isLoading)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help Articles") },
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
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.loadArticles(forceRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                AnimatedContent(
                    targetState = when {
                        uiState.error != null -> ContentState.ERROR
                        uiState.filteredArticles.isEmpty() && !uiState.isLoading -> ContentState.EMPTY
                        else -> ContentState.CONTENT
                    },
                    label = "content_animation"
                ) { state ->
                    when (state) {
                        ContentState.ERROR -> {
                            ErrorContent(
                                error = uiState.error!!,
                                onRetry = viewModel::retry,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        ContentState.EMPTY -> {
                            EmptyState(
                                title = if (uiState.searchQuery.isNotBlank()) {
                                    "No articles found"
                                } else {
                                    "No articles available"
                                },
                                message = if (uiState.searchQuery.isNotBlank()) {
                                    "Try a different search term"
                                } else {
                                    "Pull to refresh or check your connection"
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        ContentState.CONTENT -> {
                            ArticlesList(
                                articles = uiState.filteredArticles,
                                onArticleClick = onArticleClick,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class ContentState {
    ERROR, EMPTY, CONTENT
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search articles...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        modifier = modifier.semantics {
            contentDescription = "Search articles by title, summary, or category"
        },
        singleLine = true
    )
}

@Composable
private fun ArticlesList(
    articles: List<Article>,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = articles,
            key = { it.id }
        ) { article ->
            ArticleCard(
                article = article,
                onClick = { onArticleClick(article.id) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "Article: ${article.title}. ${article.summary}"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatDate(article.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDate(dateString: String): String {
    val instant = Instant.parse(dateString)   // parse string to Instant
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val month = localDateTime.month.name
        .lowercase()
        .replaceFirstChar { it.uppercase() }

    return "$month ${localDateTime.dayOfMonth}, ${localDateTime.year}"
}