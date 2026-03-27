package com.amakaflow.companion.data.nutrition

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProteinNudgeService"
private const val CHANNEL_ID = "protein_nudge"
private const val CHANNEL_NAME = "Post-Workout Protein Reminders"
private const val NOTIFICATION_ID = 1293
private const val WORK_TAG = "protein_nudge_check"

/**
 * AMA-1293: Schedules a local notification 30-60 min after workout completion
 * if the user's protein intake is below 60% of their target.
 *
 * Uses WorkManager for reliable deferred execution.
 */
@Singleton
class ProteinNudgeService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fuelingRepository: FuelingRepository
) {
    init {
        createNotificationChannel()
    }

    /**
     * Schedule a protein nudge check to run [delayMinutes] after now.
     * Called when a workout completes.
     */
    fun schedulePostWorkoutCheck(delayMinutes: Long = 30) {
        Log.d(TAG, "Scheduling protein nudge check in $delayMinutes minutes")

        val workRequest = OneTimeWorkRequestBuilder<ProteinNudgeWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    /**
     * Cancel any pending protein nudge checks.
     */
    fun cancelPendingChecks() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
    }

    /**
     * Directly check and show notification if needed (called by ProteinNudgeWorker).
     */
    suspend fun checkAndNotify(): Boolean {
        val nudge = fuelingRepository.checkProteinNudge() ?: return false

        if (nudge.shouldNudge) {
            showNotification(nudge.message)
            return true
        }
        return false
    }

    private fun showNotification(message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Post-Workout Protein")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to eat protein after your workout"
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val DELAY_MINUTES_DEFAULT = 30L
    }
}
