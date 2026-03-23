package com.amakaflow.companion.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amakaflow.companion.data.TestConfig
import com.amakaflow.companion.ui.screens.calendar.CalendarScreen
import com.amakaflow.companion.ui.screens.completion.CompletionDetailScreen
import com.amakaflow.companion.ui.screens.debug.DebugLogScreen
import com.amakaflow.companion.ui.screens.debug.ErrorLogScreen
import com.amakaflow.companion.ui.screens.debug.WorkoutDebugScreen
import com.amakaflow.companion.ui.screens.history.HistoryScreen
import com.amakaflow.companion.ui.screens.home.HomeScreen
import com.amakaflow.companion.ui.screens.more.MoreScreen
import com.amakaflow.companion.ui.screens.pairing.PairingScreen
import com.amakaflow.companion.ui.screens.player.WorkoutPlayerScreen
import com.amakaflow.companion.ui.screens.settings.SettingsScreen
import com.amakaflow.companion.ui.screens.settings.SettingsViewModel
import com.amakaflow.companion.ui.screens.settings.TranscriptionSettingsScreen
import com.amakaflow.companion.ui.screens.aiimport.AIImportScreen
import com.amakaflow.companion.ui.screens.coach.CoachChatScreen
import com.amakaflow.companion.ui.screens.fatigue.FatigueAdvisorScreen
import com.amakaflow.companion.ui.screens.followalong.FollowAlongPlayerScreen
import com.amakaflow.companion.ui.screens.feed.ActivityFeedScreen
import com.amakaflow.companion.ui.screens.preferences.TrainingPreferencesScreen
import com.amakaflow.companion.ui.screens.shoes.ShoeComparisonScreen
import com.amakaflow.companion.ui.screens.voice.VoiceWorkoutScreen
import com.amakaflow.companion.ui.screens.workouts.WorkoutDetailScreen
import com.amakaflow.companion.ui.screens.workouts.WorkoutsScreen
import com.amakaflow.companion.ui.theme.AmakaColors

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Workouts : Screen("workouts")
    data object WorkoutDetail : Screen("workout/{workoutId}") {
        fun createRoute(workoutId: String) = "workout/$workoutId"
    }
    data object Sources : Screen("sources")
    data object Calendar : Screen("calendar")
    data object More : Screen("more")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Pairing : Screen("pairing")
    data object WorkoutPlayer : Screen("player/{workoutId}") {
        fun createRoute(workoutId: String) = "player/$workoutId"
    }
    data object WorkoutDebug : Screen("workout_debug")
    data object ErrorLog : Screen("error_log")
    data object DebugLog : Screen("debug_log")
    data object TranscriptionSettings : Screen("transcription_settings")
    data object VoiceWorkout : Screen("voice_workout")
    data object AIImport : Screen("ai_import")
    data object CompletionDetail : Screen("completion/{completionId}") {
        fun createRoute(completionId: String) = "completion/$completionId"
    }
    // AMA-1182: Follow-along video playback
    data object FollowAlongPlayer : Screen("follow_along/{workoutId}") {
        fun createRoute(workoutId: String) = "follow_along/$workoutId"
    }
    // AMA-1148: New screens
    data object CoachChat : Screen("coach_chat")
    data object ActivityFeed : Screen("activity_feed")
    data object TrainingPreferences : Screen("training_preferences")
    data object ShoeComparison : Screen("shoe_comparison")
    data object FatigueAdvisor : Screen("fatigue_advisor")
}

/**
 * Bottom navigation tabs - matching iOS design
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val testTagId: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        route = Screen.Home.route,
        title = "Home",
        testTagId = "nav_home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    data object Workouts : BottomNavItem(
        route = Screen.Workouts.route,
        title = "Workouts",
        testTagId = "nav_workouts",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    )
    data object Sources : BottomNavItem(
        route = Screen.Sources.route,
        title = "Sources",
        testTagId = "nav_history",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download
    )
    data object Calendar : BottomNavItem(
        route = Screen.Calendar.route,
        title = "Calendar",
        testTagId = "nav_calendar",
        selectedIcon = Icons.Filled.DateRange,
        unselectedIcon = Icons.Outlined.DateRange
    )
    data object More : BottomNavItem(
        route = Screen.More.route,
        title = "More",
        testTagId = "nav_more",
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Workouts,
    BottomNavItem.Sources,
    BottomNavItem.Calendar,
    BottomNavItem.More
)

@Composable
fun AmakaFlowBottomNavBar(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier.testTag("bottom_navigation"),
        containerColor = AmakaColors.surface,
        contentColor = AmakaColors.textPrimary
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                modifier = Modifier.testTag(item.testTagId),
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AmakaColors.accentBlue,
                    selectedTextColor = AmakaColors.accentBlue,
                    unselectedIconColor = AmakaColors.textSecondary,
                    unselectedTextColor = AmakaColors.textSecondary,
                    indicatorColor = AmakaColors.accentBlue.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(testConfig: TestConfig) {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // In test mode with skipOnboarding, treat as paired regardless
    val effectivelyPaired = settingsState.isPaired ||
        (testConfig.isTestModeEnabled && testConfig.skipOnboarding)

    // Show bottom bar only when paired and not on pairing screen
    val showBottomBar = effectivelyPaired && currentRoute != Screen.Pairing.route

    // Start destination based on pairing state (test mode can bypass pairing)
    val startDestination = if (effectivelyPaired) Screen.Home.route else Screen.Pairing.route

    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        bottomBar = {
            if (showBottomBar) {
                AmakaFlowBottomNavBar(navController = navController)
            }
        },
        containerColor = AmakaColors.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToWorkouts = {
                        navController.navigate(Screen.Workouts.route)
                    },
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    },
                    onNavigateToVoiceWorkout = {
                        navController.navigate(Screen.VoiceWorkout.route)
                    }
                )
            }

            composable(Screen.Workouts.route) {
                WorkoutsScreen(
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    }
                )
            }

            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
                WorkoutDetailScreen(
                    workoutId = workoutId,
                    onNavigateBack = { navController.popBackStack() },
                    onStartWorkout = {
                        navController.navigate(Screen.WorkoutPlayer.createRoute(workoutId))
                    },
                    onStartFollowAlong = {
                        navController.navigate(Screen.FollowAlongPlayer.createRoute(workoutId))
                    }
                )
            }

            composable(
                route = Screen.WorkoutPlayer.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
                WorkoutPlayerScreen(
                    workoutId = workoutId,
                    onDismiss = { navController.popBackStack() }
                )
            }

            // AMA-1182: Follow-along video player
            composable(
                route = Screen.FollowAlongPlayer.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
                FollowAlongPlayerScreen(
                    workoutId = workoutId,
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.Sources.route) {
                // Sources screen - placeholder for now, shows History as similar content
                HistoryScreen(
                    onNavigateToCompletionDetail = { completionId ->
                        navController.navigate(Screen.CompletionDetail.createRoute(completionId))
                    }
                )
            }

            composable(Screen.AIImport.route) {
                AIImportScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onNavigateToWorkouts = {
                        navController.navigate(Screen.Workouts.route)
                    }
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToAIImport = {
                        navController.navigate(Screen.AIImport.route)
                    },
                    onNavigateToCoachChat = {
                        navController.navigate(Screen.CoachChat.route)
                    },
                    onNavigateToActivityFeed = {
                        navController.navigate(Screen.ActivityFeed.route)
                    },
                    onNavigateToTrainingPreferences = {
                        navController.navigate(Screen.TrainingPreferences.route)
                    },
                    onNavigateToShoeComparison = {
                        navController.navigate(Screen.ShoeComparison.route)
                    },
                    onNavigateToFatigueAdvisor = {
                        navController.navigate(Screen.FatigueAdvisor.route)
                    },
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCompletionDetail = { completionId ->
                        navController.navigate(Screen.CompletionDetail.createRoute(completionId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPairing = {
                        navController.navigate(Screen.Pairing.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    },
                    onNavigateToWorkoutDebug = {
                        navController.navigate(Screen.WorkoutDebug.route)
                    },
                    onNavigateToDebugLog = {
                        navController.navigate(Screen.DebugLog.route)
                    },
                    onNavigateToTranscriptionSettings = {
                        navController.navigate(Screen.TranscriptionSettings.route)
                    }
                )
            }

            composable(Screen.WorkoutDebug.route) {
                WorkoutDebugScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.ErrorLog.route) {
                ErrorLogScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.DebugLog.route) {
                DebugLogScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.TranscriptionSettings.route) {
                TranscriptionSettingsScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.VoiceWorkout.route) {
                VoiceWorkoutScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CompletionDetail.route,
                arguments = listOf(navArgument("completionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val completionId = backStackEntry.arguments?.getString("completionId") ?: return@composable
                CompletionDetailScreen(
                    completionId = completionId,
                    onNavigateBack = { navController.popBackStack() },
                    onRunAgain = { workoutId ->
                        navController.navigate(Screen.WorkoutPlayer.createRoute(workoutId))
                    },
                    onSaveToMyWorkouts = null // TODO: implement in follow-up ticket
                )
            }

            composable(Screen.Pairing.route) {
                PairingScreen(
                    onPairingComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Pairing.route) {
                                inclusive = true
                            }
                        }
                    },
                    testConfig = testConfig
                )
            }

            // AMA-1148: New screens
            composable(Screen.CoachChat.route) {
                CoachChatScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ActivityFeed.route) {
                ActivityFeedScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.TrainingPreferences.route) {
                TrainingPreferencesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ShoeComparison.route) {
                ShoeComparisonScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.FatigueAdvisor.route) {
                FatigueAdvisorScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
