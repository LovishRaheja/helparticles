package com.lovishraheja27.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val updatedAt: String,
    val category: String
)

@Serializable
data class ArticlesResponse(
    val articles: List<Article>
)

@Serializable
data class ArticleDetailResponse(
    val article: Article
)

@Serializable
data class ErrorResponse(
    val errorCode: String,
    val errorTitle: String,
    val errorMessage: String
)