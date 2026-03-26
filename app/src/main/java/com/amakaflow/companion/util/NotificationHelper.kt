package com.amakaflow.companion.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amakaflow.companion.R

/**
 * AMA-1258: Helper for showing import status notifications.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "workout_import"
    private const val CHANNEL_NAME = "Workout Import"
    private const val CHANNEL_DESCRIPTION = "Notifications for workout import progress and completion"

    const val NOTIFICATION_ID_IMPORT_PROGRESS = 2001
    const val NOTIFICATION_ID_IMPORT_SUCCESS = 2002
    const val NOTIFICATION_ID_IMPORT_ERROR = 2003

    /**
     * Create the notification channel. Safe to call multiple times.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Show a progress notification while the import is running.
     */
    fun showProgressNotification(context: Context, url: String) {
        createChannel(context)
        if (!hasNotificationPermission(context)) return

        val platform = PlatformDetector.detectPlatform(url)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("Importing workout")
        builder.setContentText("Importing from ${platform.displayName}...")
        builder.setPriority(NotificationCompat.PRIORITY_LOW)
        builder.setOngoing(true)
        builder.setProgress(0, 0, true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_IMPORT_PROGRESS, builder.build())
    }

    /**
     * Show a success notification when import completes.
     */
    fun showSuccessNotification(context: Context, workoutName: String) {
        createChannel(context)
        if (!hasNotificationPermission(context)) return

        // Cancel the progress notification
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_IMPORT_PROGRESS)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("Workout imported")
        builder.setContentText(workoutName)
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_IMPORT_SUCCESS, builder.build())
    }

    /**
     * Show an error notification when import fails.
     */
    fun showErrorNotification(context: Context, errorMessage: String) {
        createChannel(context)
        if (!hasNotificationPermission(context)) return

        // Cancel the progress notification
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_IMPORT_PROGRESS)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle("Import failed")
        builder.setContentText(errorMessage)
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_IMPORT_ERROR, builder.build())
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
