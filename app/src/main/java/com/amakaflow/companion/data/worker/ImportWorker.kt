package com.amakaflow.companion.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.amakaflow.companion.data.api.IngestorApi
import com.amakaflow.companion.data.model.UrlImportRequest
import com.amakaflow.companion.util.NotificationHelper
import com.amakaflow.companion.util.PlatformDetector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * AMA-1258: WorkManager worker for background URL import.
 * Survives process death, handles retries, requires network connectivity.
 */
@HiltWorker
class ImportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ingestorApi: IngestorApi,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ImportWorker"
        const val KEY_URL = "url"
        const val KEY_PLATFORM = "platform"
        const val KEY_WORKOUT_NAME = "workout_name"
        const val KEY_WORKOUT_ID = "workout_id"
        const val KEY_ERROR = "error"
        private const val MAX_RETRIES = 3

        /**
         * Enqueue a single URL import.
         */
        fun enqueue(context: Context, url: String): String {
            val platform = PlatformDetector.detectPlatform(url)
            val workName = "import_url_${url.hashCode()}"

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString(KEY_URL, url)
                .putString(KEY_PLATFORM, platform.name)
                .build()

            val request = OneTimeWorkRequestBuilder<ImportWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS,
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)

            return workName
        }

        /**
         * Enqueue multiple URL imports (for ACTION_SEND_MULTIPLE).
         */
        fun enqueueBatch(context: Context, urls: List<String>): List<String> {
            return urls.map { enqueue(context, it) }
        }
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure(
            Data.Builder().putString(KEY_ERROR, "No URL provided").build()
        )

        Log.d(TAG, "Starting import for URL: $url")

        // Show progress notification
        NotificationHelper.showProgressNotification(applicationContext, url)

        return try {
            val platform = PlatformDetector.detectPlatform(url)
            val request = UrlImportRequest(url = url)

            val response = when (platform) {
                PlatformDetector.Platform.YOUTUBE -> ingestorApi.importYouTube(request)
                PlatformDetector.Platform.INSTAGRAM -> {
                    val igRequest = com.amakaflow.companion.data.model.InstagramReelRequest(url = url)
                    ingestorApi.importInstagramReel(igRequest)
                }
                PlatformDetector.Platform.TIKTOK -> ingestorApi.importTikTok(request)
                PlatformDetector.Platform.PINTEREST -> ingestorApi.importPinterest(request)
                PlatformDetector.Platform.UNKNOWN -> ingestorApi.importUrl(request)
            }

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.workout != null) {
                    Log.d(TAG, "Import successful: ${body.workout.name}")
                    NotificationHelper.showSuccessNotification(applicationContext, body.workout.name)

                    val outputData = Data.Builder()
                        .putString(KEY_WORKOUT_NAME, body.workout.name)
                        .putString(KEY_WORKOUT_ID, body.workout.id)
                        .build()

                    Result.success(outputData)
                } else {
                    val error = body?.message ?: "Import returned no workout"
                    Log.e(TAG, "Import failed: $error")
                    handleFailure(error)
                }
            } else {
                val code = response.code()
                val detail = response.errorBody()?.string()?.take(200) ?: ""
                val error = "HTTP $code: $detail"
                Log.e(TAG, "Import HTTP error: $error")

                // Don't retry on client errors (except 401/429)
                if (code in 400..499 && code != 401 && code != 429) {
                    NotificationHelper.showErrorNotification(applicationContext, "Import failed: $error")
                    Result.failure(Data.Builder().putString(KEY_ERROR, error).build())
                } else {
                    handleRetry(error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Import exception", e)
            handleRetry(e.message ?: "Unknown error")
        }
    }

    private fun handleFailure(error: String): Result {
        NotificationHelper.showErrorNotification(applicationContext, error)
        return Result.failure(Data.Builder().putString(KEY_ERROR, error).build())
    }

    private fun handleRetry(error: String): Result {
        return if (runAttemptCount < MAX_RETRIES) {
            Log.d(TAG, "Retrying import (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
            Result.retry()
        } else {
            NotificationHelper.showErrorNotification(applicationContext, error)
            Result.failure(Data.Builder().putString(KEY_ERROR, error).build())
        }
    }
}
