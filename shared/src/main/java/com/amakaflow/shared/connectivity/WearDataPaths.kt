package com.amakaflow.shared.connectivity

/**
 * Constants for DataLayer API paths and message paths.
 * Shared between phone app and wear app to ensure consistency.
 */
object WearDataPaths {

    // =========================================================================
    // DataItem paths (synced data, persisted on device)
    // =========================================================================

    /** List of workouts available for today. Phone -> Watch. */
    const val WORKOUTS_PATH = "/amakaflow/workouts"

    /** Today's schedule. Phone -> Watch. */
    const val SCHEDULE_PATH = "/amakaflow/schedule"

    /** Readiness score. Phone -> Watch. */
    const val READINESS_PATH = "/amakaflow/readiness"

    // =========================================================================
    // Message paths (fire-and-forget, real-time)
    // =========================================================================

    /** Start a workout on watch. Phone -> Watch. */
    const val MSG_WORKOUT_START = "/amakaflow/msg/workout/start"

    /** Pause current workout. Bidirectional. */
    const val MSG_WORKOUT_PAUSE = "/amakaflow/msg/workout/pause"

    /** Resume current workout. Bidirectional. */
    const val MSG_WORKOUT_RESUME = "/amakaflow/msg/workout/resume"

    /** Complete current workout. Watch -> Phone. */
    const val MSG_WORKOUT_COMPLETE = "/amakaflow/msg/workout/complete"

    /** Cancel current workout. Bidirectional. */
    const val MSG_WORKOUT_CANCEL = "/amakaflow/msg/workout/cancel"

    /** Heart rate update during workout. Watch -> Phone. */
    const val MSG_HEART_RATE = "/amakaflow/msg/heartrate"

    /** Workout state sync (full state). Bidirectional. */
    const val MSG_STATE_SYNC = "/amakaflow/msg/state"

    /** Request sync from watch. Watch -> Phone. */
    const val MSG_REQUEST_SYNC = "/amakaflow/msg/request_sync"

    /** Health snapshot. Watch -> Phone. */
    const val MSG_HEALTH_SNAPSHOT = "/amakaflow/msg/health"

    // =========================================================================
    // Capability names (for node discovery)
    // =========================================================================

    /** Phone app capability for watch to discover. */
    const val PHONE_CAPABILITY = "amakaflow_phone"

    /** Watch app capability for phone to discover. */
    const val WATCH_CAPABILITY = "amakaflow_watch"

    // =========================================================================
    // DataMap keys
    // =========================================================================

    const val KEY_PAYLOAD = "payload"
    const val KEY_TIMESTAMP = "timestamp"
}
