package com.lovishraheja27.helparticles

import com.lovishraheja27.helparticles.data.remote.ArticleApiService
import com.lovishraheja27.helparticles.data.repository.ArticlesRepository
import com.lovishraheja27.shared.cache.ArticleCache
import com.lovishraheja27.shared.cache.CacheResult
import com.lovishraheja27.shared.models.Article
import com.lovishraheja27.shared.models.ArticlesResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.HttpException
import com.lovishraheja27.helparticles.data.repository.Result
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ArticlesRepositoryTest {

    private lateinit var apiService: ArticleApiService
    private lateinit var cache: ArticleCache
    private lateinit var repository: ArticlesRepository

    private val testArticle = Article(
        id = "1",
        title = "Test Article",
        summary = "Test summary",
        content = "Test content",
        updatedAt = Clock.System.now().toString(),
        category = "Test"
    )

    @Before
    fun setup() {
        apiService = mock()
        cache = mock()
        repository = ArticlesRepository(apiService, cache)
    }

    @Test
    fun `getArticles returns cached data when fresh`() = runTest {
        whenever(cache.getArticles()).thenReturn(
            CacheResult.Hit(
                data = listOf(testArticle),
                timestamp = Clock.System.now(),
                isStale = false
            )
        )

        val results = repository.getArticles().toList()
        val success = results.last()
        assertIs<Result.Success<List<Article>>>(success)
        assertEquals(listOf(testArticle), success.data)
        assertTrue(success.fromCache)
        verify(apiService, never()).getArticles()
    }

    @Test
    fun `getArticles fetches from network when stale`() = runTest {
        val freshArticle = testArticle.copy(title = "Fresh Article")
        whenever(cache.getArticles()).thenReturn(
            CacheResult.Hit(
                data = listOf(testArticle),
                timestamp = Clock.System.now(),
                isStale = true
            )
        )
        whenever(apiService.getArticles()).thenReturn(
            ArticlesResponse(listOf(freshArticle))
        )

        val results = repository.getArticles().toList()

        assertTrue(results.size >= 2)

        val cachedResult = results.first { it is Result.Success }
        assertIs<Result.Success<List<Article>>>(cachedResult)
        assertTrue(cachedResult.fromCache)

        val freshResult = results.last()
        assertIs<Result.Success<List<Article>>>(freshResult)
        assertEquals(freshArticle, freshResult.data.first())

        verify(cache).saveArticles(listOf(freshArticle))
    }

    @Test
    fun `getArticles handles 404 as non-retryable error`() = runTest {
        val mockResponse = mock<retrofit2.Response<ArticlesResponse>> {
            on { code() } doReturn 404
            on { errorBody() } doReturn null
        }
        val httpException = HttpException(mockResponse)

        whenever(cache.getArticles()).thenReturn(CacheResult.Miss)
        whenever(apiService.getArticles()).thenThrow(httpException)

        val result = repository.getArticles().first { it is Result.Error }

        assertIs<Result.Error>(result)
        assertEquals(false, result.isNetworkError)
        assertEquals(false, result.canRetry)
    }

    @Test
    fun `prefetchArticles returns success on successful fetch`() = runTest {
        whenever(apiService.getArticles()).thenReturn(
            ArticlesResponse(listOf(testArticle))
        )

        val success = repository.prefetchArticles()

        assertTrue(success)
        verify(cache).saveArticles(listOf(testArticle))
    }
}