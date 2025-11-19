package com.lovishraheja27.helparticles.data.repository

import com.lovishraheja27.helparticles.data.remote.ArticleApiService
import com.lovishraheja27.shared.cache.ArticleCache
import com.lovishraheja27.shared.cache.CacheResult
import com.lovishraheja27.shared.models.Article
import com.lovishraheja27.shared.models.ErrorResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class Result<out T> {
    data class Success<T>(val data: T, val fromCache: Boolean = false) : Result<T>()
    data class Error(
        val message: String,
        val isNetworkError: Boolean,
        val canRetry: Boolean = true,
        val errorCode: String? = null
    ) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

class ArticlesRepository(
    private val apiService: ArticleApiService,
    private val cache: ArticleCache
) {

    fun getArticles(forceRefresh: Boolean = false): Flow<Result<List<Article>>> = flow {
        emit(Result.Loading)

        if (!forceRefresh) {
            when (val cacheResult = cache.getArticles()) {
                is CacheResult.Hit -> {
                    emit(Result.Success(cacheResult.data, fromCache = true))
                    if (!cacheResult.isStale) {
                        return@flow
                    }
                }
                is CacheResult.Expired, CacheResult.Miss -> {
                }
            }
        }

        try {
            val response = apiService.getArticles()
            cache.saveArticles(response.articles)
            emit(Result.Success(response.articles, fromCache = false))
        } catch (e: Exception) {
            val error = handleException(e)

            if (error.isNetworkError) {
                val cacheResult = cache.getArticles()
                if (cacheResult is CacheResult.Hit) {
                    return@flow
                }
            }

            emit(error)
        }
    }

    fun getArticle(id: String, forceRefresh: Boolean = false): Flow<Result<Article>> = flow {
        emit(Result.Loading)

        if (!forceRefresh) {
            when (val cacheResult = cache.getArticle(id)) {
                is CacheResult.Hit -> {
                    emit(Result.Success(cacheResult.data, fromCache = true))
                    if (!cacheResult.isStale) {
                        return@flow
                    }
                }
                is CacheResult.Expired, CacheResult.Miss -> {}
            }
        }

        try {
            val response = apiService.getArticle(id)
            cache.saveArticle(response.article)
            emit(Result.Success(response.article, fromCache = false))
        } catch (e: Exception) {
            val error = handleException(e)

            if (error.isNetworkError) {
                val cacheResult = cache.getArticle(id)
                if (cacheResult is CacheResult.Hit) {
                    return@flow
                }
            }

            emit(error)
        }
    }

    suspend fun prefetchArticles(): Boolean {
        return try {
            val response = apiService.getArticles()
            cache.saveArticles(response.articles)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun handleException(exception: Exception): Result.Error {
        return when (exception) {
            is UnknownHostException, is ConnectException -> {
                Result.Error(
                    message = "No internet connection. Please check your network settings.",
                    isNetworkError = true,
                    canRetry = true
                )
            }

            is SocketTimeoutException -> {
                Result.Error(
                    message = "Connection timed out. Please try again.",
                    isNetworkError = true,
                    canRetry = true
                )
            }

            is retrofit2.HttpException -> {
                val code = exception.code()

                val errorBody = exception.response()?.errorBody()?.string()
                val backendError = errorBody?.let {
                    try {
                        Gson().fromJson(it, ErrorResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (backendError != null) {
                    Result.Error(
                        message = backendError.errorMessage,
                        isNetworkError = false,
                        canRetry = code != 404, // Don't retry on Not Found
                        errorCode = backendError.errorCode
                    )
                } else {
                    when (code) {
                        in 500..599 -> Result.Error(
                            message = "Server error. Please try again later.",
                            isNetworkError = true,
                            canRetry = true
                        )
                        404 -> Result.Error(
                            message = "Article not found.",
                            isNetworkError = false,
                            canRetry = false
                        )
                        else -> Result.Error(
                            message = "An error occurred (HTTP $code)",
                            isNetworkError = false,
                            canRetry = true
                        )
                    }
                }
            }

            else -> {
                Result.Error(
                    message = "An unexpected error occurred: ${exception.message}",
                    isNetworkError = false,
                    canRetry = true
                )
            }
        }
    }
}