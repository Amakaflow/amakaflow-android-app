package com.amakaflow.companion.data.nutrition

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "ProteinNudgeWorker"

/**
 * AMA-1293: WorkManager worker that checks the protein nudge API
 * and shows a local notification if protein is below threshold.
 */
@HiltWorker
class ProteinNudgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val proteinNudgeService: ProteinNudgeService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running protein nudge check")
        return try {
            proteinNudgeService.checkAndNotify()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Protein nudge check failed", e)
            Result.failure()
        }
    }
}
