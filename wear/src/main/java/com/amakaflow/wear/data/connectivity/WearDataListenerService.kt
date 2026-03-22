package com.amakaflow.wear.data.connectivity

import android.util.Log
import com.amakaflow.shared.connectivity.WearDataPaths
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Background service that listens for DataLayer events from the phone.
 * Handles both DataItem changes (persistent sync) and Messages (real-time).
 */
@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearDataListener"
    }

    @Inject
    lateinit var phoneConnectivityManager: PhoneConnectivityManager

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val uri = event.dataItem.uri
            val path = uri.path ?: continue
            Log.d(TAG, "Data changed: $path")

            try {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val payload = dataMap.getString(WearDataPaths.KEY_PAYLOAD) ?: continue

                when (path) {
                    WearDataPaths.WORKOUTS_PATH -> {
                        phoneConnectivityManager.onWorkoutsReceived(payload)
                    }
                    WearDataPaths.SCHEDULE_PATH -> {
                        phoneConnectivityManager.onScheduleReceived(payload)
                    }
                    WearDataPaths.READINESS_PATH -> {
                        phoneConnectivityManager.onReadinessReceived(payload)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing data event: $path", e)
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        val path = event.path
        val payload = String(event.data, Charsets.UTF_8)
        Log.d(TAG, "Message received: $path")

        try {
            when (path) {
                WearDataPaths.MSG_WORKOUT_START -> {
                    phoneConnectivityManager.onWorkoutStartReceived(payload)
                }
                WearDataPaths.MSG_WORKOUT_PAUSE -> {
                    phoneConnectivityManager.onWorkoutPauseReceived()
                }
                WearDataPaths.MSG_WORKOUT_RESUME -> {
                    phoneConnectivityManager.onWorkoutResumeReceived()
                }
                WearDataPaths.MSG_WORKOUT_CANCEL -> {
                    phoneConnectivityManager.onWorkoutCancelReceived()
                }
                WearDataPaths.MSG_STATE_SYNC -> {
                    phoneConnectivityManager.onStateSyncReceived(payload)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message: $path", e)
        }
    }
}
