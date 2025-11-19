package com.lovishraheja27.helparticles.di

import com.lovishraheja27.helparticles.data.remote.ApiClient
import com.lovishraheja27.helparticles.data.remote.ArticleApiService
import com.lovishraheja27.helparticles.data.repository.ArticlesRepository
import com.lovishraheja27.shared.cache.ArticleCache
import com.lovishraheja27.shared.cache.InMemoryArticleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideArticleApiService(): ArticleApiService {
        return ApiClient.apiService
    }

    @Provides
    @Singleton
    fun provideArticleCache(): ArticleCache {
        return InMemoryArticleCache()
    }

    @Provides
    @Singleton
    fun provideArticlesRepository(
        apiService: ArticleApiService,
        cache: ArticleCache
    ): ArticlesRepository {
        return ArticlesRepository(apiService, cache)
    }
}