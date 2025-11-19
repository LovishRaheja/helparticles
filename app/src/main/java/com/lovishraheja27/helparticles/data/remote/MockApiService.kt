package com.lovishraheja27.helparticles.data.remote

import com.lovishraheja27.shared.models.Article
import com.lovishraheja27.shared.models.ArticleDetailResponse
import com.lovishraheja27.shared.models.ArticlesResponse
import kotlinx.datetime.Clock
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import kotlin.random.Random

interface ArticleApiService {
    @GET("articles")
    suspend fun getArticles(): ArticlesResponse

    @GET("articles/{id}")
    suspend fun getArticle(@Path("id") id: String): ArticleDetailResponse
}

class MockApiInterceptor : Interceptor {
    private var requestCount = 0

    override fun intercept(chain: Interceptor.Chain): Response {
        requestCount++
        val request = chain.request()
        val path = request.url.encodedPath

        // Simulate network delay
        Thread.sleep(Random.nextLong(200, 800))

        return when {
            requestCount % 15 == 0 -> {
                throw java.net.SocketTimeoutException("Connection timeout")
            }

            requestCount % 20 == 0 -> {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Internal Server Error")
                    .body("Server error".toResponseBody("text/plain".toMediaType()))
                    .build()
            }

            requestCount % 25 == 0 -> {
                val errorJson = """
                    {
                        "errorCode": "RATE_LIMIT_EXCEEDED",
                        "errorTitle": "Too Many Requests",
                        "errorMessage": "You have exceeded the rate limit. Please try again in 60 seconds."
                    }
                """.trimIndent()

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .body(errorJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }

            path.startsWith("/articles/") -> {
                val articleId = path.substringAfterLast("/")
                val json = createArticleDetailJson(articleId)

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }

            path == "/articles" -> {
                val json = createArticlesListJson()

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }

            else -> chain.proceed(request)
        }
    }

    private fun createArticlesListJson(): String {
        val articles = (1..10).map { id ->
            val article = createMockArticle(id.toString())
            """
            {
                "id": "${article.id}",
                "title": ${escapeJson(article.title)},
                "summary": ${escapeJson(article.summary)},
                "content": ${escapeJson(article.content)},
                "updatedAt": "${article.updatedAt}",
                "category": "${article.category}"
            }
            """.trimIndent()
        }

        return """{"articles": [${articles.joinToString(",")}]}"""
    }

    private fun createArticleDetailJson(id: String): String {
        val article = createMockArticle(id)
        return """
        {
            "article": {
                "id": "${article.id}",
                "title": ${escapeJson(article.title)},
                "summary": ${escapeJson(article.summary)},
                "content": ${escapeJson(article.content)},
                "updatedAt": "${article.updatedAt}",
                "category": "${article.category}"
            }
        }
        """.trimIndent()
    }

    private fun escapeJson(str: String): String {
        return "\"" + str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }

    private fun createMockArticle(id: String): Article {
        val categories = listOf("Getting Started", "Account", "Billing", "Technical", "Privacy")
        val titles = mapOf(
            "1" to "How to Reset Your Password",
            "2" to "Understanding Your Dashboard",
            "3" to "Setting Up Two-Factor Authentication",
            "4" to "Billing Cycle Explained",
            "5" to "API Authentication Guide",
            "6" to "Managing Team Members",
            "7" to "Data Export Options",
            "8" to "Privacy Settings Overview",
            "9" to "Troubleshooting Connection Issues",
            "10" to "Mobile App Installation"
        )

        val title = titles[id] ?: "Help Article #$id"

        return Article(
            id = id,
            title = title,
            summary = "This article covers important information about ${title.lowercase()}. Learn the key steps and best practices.",
            content = """
                # ${titles[id] ?: "Help Article"}
                
                ## Introduction
                Welcome to this comprehensive guide. This article will walk you through everything you need to know.
                
                ## Step-by-Step Instructions
                
                ### Step 1: Prerequisites
                Before you begin, make sure you have:
                - Valid account credentials
                - Admin access (if required)
                - Latest app version installed
                
                ### Step 2: Main Process
                1. Navigate to the settings page
                2. Click on the relevant section
                3. Follow the on-screen instructions
                4. Confirm your changes
                
                ### Step 3: Verification
                After completing the process:
                - Check your email for confirmation
                - Test the new configuration
                - Contact support if issues persist
                
                ## Common Issues
                
                ### Issue: Changes not saving
                **Solution**: Clear your browser cache and try again.
                
                ### Issue: Error message appears
                **Solution**: Ensure all required fields are filled correctly.
                
                ## Additional Resources
                - [Video Tutorial](#)
                - [Community Forum](#)
                - [Contact Support](#)
                
                Last updated: ${Clock.System.now()}
            """.trimIndent(),
            updatedAt = Clock.System.now().toString(),
            category = categories[id.toIntOrNull()?.rem(categories.size) ?: 0]
        )
    }
}

object ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(MockApiInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://mock-api.example.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ArticleApiService = retrofit.create(ArticleApiService::class.java)
}