package com.lovishraheja27.shared.cache

import com.lovishraheja27.shared.models.Article
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

interface ArticleCache {
    suspend fun getArticles(): CacheResult<List<Article>>
    suspend fun saveArticles(articles: List<Article>)
    suspend fun getArticle(id: String): CacheResult<Article>
    suspend fun saveArticle(article: Article)
    suspend fun clear()

    companion object {
        val STALE_THRESHOLD: Duration = 24.hours
        val EXPIRY_THRESHOLD: Duration = 72.hours
    }
}

sealed class CacheResult<out T> {
    data class Hit<T>(
        val data: T,
        val timestamp: Instant,
        val isStale: Boolean
    ) : CacheResult<T>()

    data object Miss : CacheResult<Nothing>()
    data object Expired : CacheResult<Nothing>()
}

class InMemoryArticleCache : ArticleCache {
    private val articlesMap = mutableMapOf<String, Pair<Article, Instant>>()
    private var articlesList: Pair<List<Article>, Instant>? = null

    override suspend fun getArticles(): CacheResult<List<Article>> {
        val cached = articlesList ?: return CacheResult.Miss

        return when {
            isExpired(cached.second) -> {
                articlesList = null
                CacheResult.Expired
            }
            else -> CacheResult.Hit(
                data = cached.first,
                timestamp = cached.second,
                isStale = isStale(cached.second)
            )
        }
    }

    override suspend fun saveArticles(articles: List<Article>) {
        val now = Clock.System.now()
        articlesList = articles to now

        // Also cache individual articles for detail screen
        articles.forEach { article ->
            articlesMap[article.id] = article to now
        }
    }

    override suspend fun getArticle(id: String): CacheResult<Article> {
        val cached = articlesMap[id] ?: return CacheResult.Miss

        return when {
            isExpired(cached.second) -> {
                articlesMap.remove(id)
                CacheResult.Expired
            }
            else -> CacheResult.Hit(
                data = cached.first,
                timestamp = cached.second,
                isStale = isStale(cached.second)
            )
        }
    }

    override suspend fun saveArticle(article: Article) {
        articlesMap[article.id] = article to Clock.System.now()
    }

    override suspend fun clear() {
        articlesMap.clear()
        articlesList = null
    }

    private fun isStale(timestamp: Instant): Boolean {
        val age = Clock.System.now() - timestamp
        return age >= ArticleCache.STALE_THRESHOLD
    }

    private fun isExpired(timestamp: Instant): Boolean {
        val age = Clock.System.now() - timestamp
        return age >= ArticleCache.EXPIRY_THRESHOLD
    }
}