package com.lovishraheja27.helparticles.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovishraheja27.helparticles.data.repository.ArticlesRepository
import com.lovishraheja27.shared.models.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.lovishraheja27.helparticles.data.repository.Result

data class ArticlesListUiState(
    val articles: List<Article> = emptyList(),
    val filteredArticles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val error: ErrorState? = null,
    val searchQuery: String = "",
    val fromCache: Boolean = false
)

data class ErrorState(
    val message: String,
    val isNetworkError: Boolean,
    val canRetry: Boolean
)

@HiltViewModel
class ArticlesListViewModel @Inject constructor(
    private val repository: ArticlesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticlesListUiState())
    val uiState: StateFlow<ArticlesListUiState> = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getArticles(forceRefresh)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }

                        is Result.Success -> {
                            _uiState.update {
                                val filtered = filterArticles(result.data, it.searchQuery)
                                it.copy(
                                    articles = result.data,
                                    filteredArticles = filtered,
                                    isLoading = false,
                                    error = null,
                                    fromCache = result.fromCache
                                )
                            }
                        }

                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = ErrorState(
                                        message = result.message,
                                        isNetworkError = result.isNetworkError,
                                        canRetry = result.canRetry
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            val filtered = filterArticles(it.articles, query)
            it.copy(
                searchQuery = query,
                filteredArticles = filtered
            )
        }
    }

    fun retry() {
        loadArticles(forceRefresh = true)
    }

    private fun filterArticles(articles: List<Article>, query: String): List<Article> {
        if (query.isBlank()) return articles

        val lowercaseQuery = query.lowercase()
        return articles.filter { article ->
            article.title.lowercase().contains(lowercaseQuery) ||
                    article.summary.lowercase().contains(lowercaseQuery) ||
                    article.category.lowercase().contains(lowercaseQuery)
        }
    }
}