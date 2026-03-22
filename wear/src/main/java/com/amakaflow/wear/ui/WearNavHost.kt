package com.amakaflow.wear.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.amakaflow.wear.ui.screens.ReadinessScreen
import com.amakaflow.wear.ui.screens.TodayScheduleScreen
import com.amakaflow.wear.ui.screens.WorkoutExecutionScreen
import com.amakaflow.wear.ui.screens.WorkoutListScreen

/**
 * Navigation routes for the Wear OS app.
 */
object WearRoutes {
    const val WORKOUT_LIST = "workout_list"
    const val WORKOUT_EXECUTION = "workout_execution/{workoutId}"
    const val READINESS = "readiness"
    const val TODAY_SCHEDULE = "today_schedule"

    fun workoutExecution(workoutId: String) = "workout_execution/$workoutId"
}

@Composable
fun WearNavHost() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearRoutes.WORKOUT_LIST
    ) {
        composable(WearRoutes.WORKOUT_LIST) {
            WorkoutListScreen(
                onWorkoutSelected = { workoutId ->
                    navController.navigate(WearRoutes.workoutExecution(workoutId))
                },
                onReadinessClick = {
                    navController.navigate(WearRoutes.READINESS)
                },
                onScheduleClick = {
                    navController.navigate(WearRoutes.TODAY_SCHEDULE)
                }
            )
        }

        composable(WearRoutes.WORKOUT_EXECUTION) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
            WorkoutExecutionScreen(
                workoutId = workoutId,
                onFinished = { navController.popBackStack() }
            )
        }

        composable(WearRoutes.READINESS) {
            ReadinessScreen()
        }

        composable(WearRoutes.TODAY_SCHEDULE) {
            TodayScheduleScreen(
                onWorkoutSelected = { workoutId ->
                    navController.navigate(WearRoutes.workoutExecution(workoutId))
                }
            )
        }
    }
}
