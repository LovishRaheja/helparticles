package com.lovishraheja27.helparticles.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lovishraheja27.helparticles.data.repository.ArticlesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class ArticlePrefetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticlesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting article prefetch")

        return@withContext try {
            val success = repository.prefetchArticles()

            if (success) {
                Log.d(TAG, "Article prefetch successful")
                Result.success()
            } else {
                Log.w(TAG, "Article prefetch failed — retrying")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Article prefetch error: ${e.message}", e)

            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "ArticlePrefetchWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
        const val WORK_NAME = "article_prefetch_work"
    }
}

object ArticlePrefetchScheduler {

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val prefetchRequest =
            PeriodicWorkRequestBuilder<ArticlePrefetchWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 2,
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag("prefetch")
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ArticlePrefetchWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            prefetchRequest
        )

        Log.d("ArticlePrefetchScheduler", "Background prefetch scheduled")
    }
}
