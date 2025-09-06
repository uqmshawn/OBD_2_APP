package com.hurtec.obd2.diagnostics.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.performance.PerformanceOptimizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced navigation manager to handle navigation state and prevent freezing
 */
@Singleton
class NavigationManager @Inject constructor(
    private val performanceOptimizer: PerformanceOptimizer
) {

    // Thread-safe navigation state
    private val isNavigating = AtomicBoolean(false)
    private val lastNavigationTime = AtomicLong(0)
    private val navigationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Navigation state flow
    private val _navigationState = MutableStateFlow(NavigationState())
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    // Navigation debounce settings
    private val navigationDebounceMs = 500L
    private val maxNavigationRate = 2 // Max 2 navigations per second
    
    /**
     * Safe navigation that prevents multiple rapid navigations with advanced rate limiting
     */
    fun safeNavigate(
        navController: NavController,
        route: String,
        popUpTo: String? = null,
        inclusive: Boolean = false,
        clearStack: Boolean = false
    ) {
        try {
            val currentTime = System.currentTimeMillis()

            // Check if navigation is already in progress
            if (isNavigating.get()) {
                CrashHandler.logWarning("Navigation already in progress, ignoring: $route")
                updateNavigationState(NavigationState(isNavigating = true, error = "Navigation in progress"))
                return
            }

            // Check rate limiting
            val timeSinceLastNavigation = currentTime - lastNavigationTime.get()
            if (timeSinceLastNavigation < navigationDebounceMs) {
                CrashHandler.logWarning("Navigation rate limited, ignoring: $route")
                updateNavigationState(NavigationState(error = "Navigation rate limited"))
                return
            }

            // Set navigation state
            isNavigating.set(true)
            lastNavigationTime.set(currentTime)
            updateNavigationState(NavigationState(isNavigating = true, currentRoute = route))

            CrashHandler.logInfo("Navigating to: $route")

            // Register with performance optimizer
            performanceOptimizer.registerWeakReference("navigation_$route", navController)
            
            navController.navigate(route) {
                if (clearStack) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = false
                        this.inclusive = true
                    }
                } else if (popUpTo != null) {
                    popUpTo(popUpTo) {
                        this.inclusive = inclusive
                    }
                } else {
                    popUpTo(navController.graph.findStartDestination().id) {
                        // Default behavior
                    }
                }
                launchSingleTop = true
                restoreState = !clearStack
            }
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "NavigationManager.safeNavigate")
        } finally {
            // Reset navigation flag after a delay
            CoroutineScope(Dispatchers.Main).launch {
                delay(navigationDebounceMs)
                isNavigating.set(false)
                updateNavigationState(NavigationState(isNavigating = false))
            }
        }
    }
    
    /**
     * Navigate to bottom nav item safely
     */
    fun navigateToBottomNavItem(navController: NavController, item: BottomNavItem) {
        safeNavigate(
            navController = navController,
            route = item.route,
            popUpTo = navController.graph.findStartDestination().id.toString(),
            inclusive = false,
            clearStack = false
        )
    }
    
    /**
     * Navigate back safely
     */
    fun safeNavigateBack(navController: NavController): Boolean {
        return try {
            if (isNavigating.get()) {
                CrashHandler.logWarning("Navigation in progress, ignoring back navigation")
                return false
            }

            isNavigating.set(true)
            val result = navController.popBackStack()
            CrashHandler.logInfo("Navigate back result: $result")

            CoroutineScope(Dispatchers.Main).launch {
                delay(300)
                isNavigating.set(false)
                updateNavigationState(NavigationState(isNavigating = false))
            }

            result
        } catch (e: Exception) {
            CrashHandler.handleException(e, "NavigationManager.safeNavigateBack")
            isNavigating.set(false)
            updateNavigationState(NavigationState(isNavigating = false, error = e.message))
            false
        }
    }
    
    /**
     * Update navigation state safely
     */
    private fun updateNavigationState(state: NavigationState) {
        try {
            _navigationState.value = state
        } catch (e: Exception) {
            CrashHandler.handleException(e, "NavigationManager.updateNavigationState")
        }
    }

    /**
     * Clear navigation state
     */
    fun clearNavigationState() {
        isNavigating.set(false)
        updateNavigationState(NavigationState(isNavigating = false))
        CrashHandler.logInfo("Navigation state cleared")
    }

    /**
     * Check if navigation is currently in progress
     */
    fun isNavigationInProgress(): Boolean {
        return isNavigating.get()
    }

    /**
     * Get navigation statistics
     */
    fun getNavigationStats(): NavigationStats {
        return NavigationStats(
            isNavigating = isNavigating.get(),
            lastNavigationTime = lastNavigationTime.get(),
            currentRoute = _navigationState.value.currentRoute
        )
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            navigationScope.cancel()
            isNavigating.set(false)
            updateNavigationState(NavigationState(isNavigating = false))
            CrashHandler.logInfo("NavigationManager cleaned up")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "NavigationManager.cleanup")
        }
    }
}

/**
 * Navigation state data class
 */
data class NavigationState(
    val isNavigating: Boolean = false,
    val currentRoute: String? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Navigation statistics data class
 */
data class NavigationStats(
    val isNavigating: Boolean = false,
    val lastNavigationTime: Long = 0,
    val currentRoute: String? = null
)
