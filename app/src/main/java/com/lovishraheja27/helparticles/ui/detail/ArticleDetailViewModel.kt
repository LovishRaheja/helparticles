package com.lovishraheja27.helparticles.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovishraheja27.helparticles.data.repository.ArticlesRepository
import com.lovishraheja27.helparticles.ui.list.ErrorState
import com.lovishraheja27.shared.models.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.lovishraheja27.helparticles.data.repository.Result

data class ArticleDetailUiState(
    val article: Article? = null,
    val isLoading: Boolean = false,
    val error: ErrorState? = null,
    val fromCache: Boolean = false
)

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ArticlesRepository
) : ViewModel() {

    private val articleId: String = checkNotNull(savedStateHandle["articleId"])

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        loadArticle()
    }

    fun loadArticle(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getArticle(articleId, forceRefresh)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }

                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    article = result.data,
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

    fun retry() {
        loadArticle(forceRefresh = true)
    }
}