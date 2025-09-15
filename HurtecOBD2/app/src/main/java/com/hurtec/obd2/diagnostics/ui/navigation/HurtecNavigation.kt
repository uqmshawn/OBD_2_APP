package com.hurtec.obd2.diagnostics.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.ui.components.ErrorBoundary
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.ui.screens.connection.ConnectionScreen
import com.hurtec.obd2.diagnostics.ui.screens.dashboard.DashboardScreen
import com.hurtec.obd2.diagnostics.ui.screens.diagnostics.DiagnosticsScreen
import com.hurtec.obd2.diagnostics.ui.screens.livedata.LiveDataScreen
import com.hurtec.obd2.diagnostics.ui.screens.onboarding.OnboardingScreen
import com.hurtec.obd2.diagnostics.ui.screens.performance.PerformanceScreen
import com.hurtec.obd2.diagnostics.ui.screens.permissions.PermissionsScreen
import com.hurtec.obd2.diagnostics.ui.screens.settings.SettingsScreen
import com.hurtec.obd2.diagnostics.ui.screens.trips.TripsScreen
import com.hurtec.obd2.diagnostics.ui.screens.welcome.WelcomeScreen
import com.hurtec.obd2.diagnostics.ui.screens.setup.VehicleSetupScreen
import com.hurtec.obd2.diagnostics.data.preferences.AppPreferences
import androidx.hilt.navigation.compose.hiltViewModel
import com.hurtec.obd2.diagnostics.ui.viewmodels.MainViewModel

/**
 * Modern navigation system with Jetpack Compose Navigation
 * Replaces the old fragment-based navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HurtecNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val mainUiState by mainViewModel.uiState.collectAsState()
    // Simplified navigation for now
    fun navigateToBottomNavItem(item: BottomNavItem) {
        try {
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "Navigation.navigateToBottomNavItem")
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Bottom navigation items
    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.LiveData,
        BottomNavItem.Diagnostics,
        BottomNavItem.Connection,
        BottomNavItem.Settings
    )

    // Show/hide bottom navigation based on current screen
    val showBottomNav = currentDestination?.route in bottomNavItems.map { it.route } &&
                       currentDestination?.route != "onboarding"

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { 
                            it.route == item.route 
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.titleRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(item.titleRes),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = selected,
                            onClick = {
                                navigateToBottomNavItem(item)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Show loading while determining start destination
        if (mainUiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CrashHandler.logInfo("HurtecNavigation: Starting navigation with destination: ${mainUiState.startDestination}")

            NavHost(
                navController = navController,
                startDestination = if (mainUiState.startDestination.isNotEmpty()) mainUiState.startDestination else "welcome",
                modifier = Modifier.padding(innerPadding)
            ) {
            composable("welcome") {
                WelcomeScreen(
                    navController = navController,
                    onGetStarted = {
                        try {
                            CrashHandler.logInfo("HurtecNavigation: Navigating from welcome to onboarding")
                            navController.navigate("onboarding") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            CrashHandler.handleException(e, "HurtecNavigation.welcome.onGetStarted")
                        }
                    },
                    onSkip = {
                        try {
                            CrashHandler.logInfo("HurtecNavigation: Skip welcome, updating preferences and going to dashboard")
                            mainViewModel.skipToMain()
                            navController.navigate("dashboard") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            CrashHandler.handleException(e, "HurtecNavigation.welcome.onSkip")
                        }
                    }
                )
            }

            composable("onboarding") {
                OnboardingScreen(
                    navController = navController,
                    onComplete = {
                        mainViewModel.completeOnboarding()
                        navController.navigate("permissions") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("permissions") {
                PermissionsScreen(
                    navController = navController,
                    onPermissionsGranted = {
                        navController.navigate("vehicle_setup") {
                            popUpTo("permissions") { inclusive = true }
                        }
                    }
                )
            }

            composable("vehicle_setup") {
                VehicleSetupScreen(navController = navController)
            }

            composable("main") {
                // Navigate to dashboard after setup is complete
                LaunchedEffect(Unit) {
                    navController.navigate(BottomNavItem.Dashboard.route) {
                        popUpTo("main") { inclusive = true }
                    }
                }
            }

            composable("permissions") {
                PermissionsScreen(
                    navController = navController,
                    onPermissionsGranted = {
                        navController.navigate(BottomNavItem.Dashboard.route) {
                            popUpTo("permissions") { inclusive = true }
                        }
                    }
                )
            }

            composable(BottomNavItem.Dashboard.route) {
                ErrorBoundary {
                    DashboardScreen(navController = navController)
                }
            }

            composable(BottomNavItem.Diagnostics.route) {
                ErrorBoundary {
                    DiagnosticsScreen(navController = navController)
                }
            }

            composable(BottomNavItem.Connection.route) {
                ErrorBoundary {
                    ConnectionScreen(navController = navController)
                }
            }

            composable(BottomNavItem.Settings.route) {
                ErrorBoundary {
                    SettingsScreen(navController = navController)
                }
            }

            composable(BottomNavItem.LiveData.route) {
                ErrorBoundary {
                    LiveDataScreen(navController = navController)
                }
            }

            composable("performance") {
                ErrorBoundary {
                    PerformanceScreen(navController = navController)
                }
            }

            composable("trips") {
                ErrorBoundary {
                    TripsScreen(navController = navController)
                }
            }
        }
        }
    }
}

/**
 * Bottom navigation items
 */
sealed class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Dashboard : BottomNavItem(
        route = "dashboard",
        titleRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Filled.Dashboard
    )

    object LiveData : BottomNavItem(
        route = "livedata",
        titleRes = R.string.nav_livedata,
        selectedIcon = Icons.Filled.Timeline,
        unselectedIcon = Icons.Filled.Timeline
    )

    object Diagnostics : BottomNavItem(
        route = "diagnostics",
        titleRes = R.string.nav_diagnostics,
        selectedIcon = Icons.Filled.BugReport,
        unselectedIcon = Icons.Filled.BugReport
    )

    object Connection : BottomNavItem(
        route = "connection",
        titleRes = R.string.nav_connection,
        selectedIcon = Icons.Filled.Bluetooth,
        unselectedIcon = Icons.Filled.BluetoothDisabled
    )

    object Settings : BottomNavItem(
        route = "settings",
        titleRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Filled.Settings
    )
}
