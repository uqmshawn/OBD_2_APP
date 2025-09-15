package com.hurtec.obd2.diagnostics.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.data.preferences.AppPreferences
import com.hurtec.obd2.diagnostics.database.repository.VehicleRepository
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for app-wide state management
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        checkAppState()
    }
    
    /**
     * Check app state and determine start destination
     */
    fun checkAppState() {
        viewModelScope.launch {
            try {
                CrashHandler.logInfo("MainViewModel: Starting app state check...")

                val isFirstLaunch = appPreferences.isFirstLaunch
                val isOnboardingCompleted = appPreferences.isOnboardingCompleted

                CrashHandler.logInfo("MainViewModel: First launch: $isFirstLaunch, Onboarding completed: $isOnboardingCompleted")

                // Simplified logic to avoid database issues
                val startDestination = when {
                    isFirstLaunch -> {
                        CrashHandler.logInfo("MainViewModel: First launch detected, going to welcome")
                        "welcome"
                    }
                    !isOnboardingCompleted -> {
                        CrashHandler.logInfo("MainViewModel: Onboarding not completed, going to onboarding")
                        "onboarding"
                    }
                    else -> {
                        CrashHandler.logInfo("MainViewModel: App setup complete, going to dashboard")
                        "dashboard"
                    }
                }

                _uiState.value = _uiState.value.copy(
                    startDestination = startDestination,
                    isLoading = false,
                    demoMode = appPreferences.demoMode,
                    keepScreenOn = appPreferences.keepScreenOn,
                    themeMode = appPreferences.themeMode,
                    unitSystem = appPreferences.unitSystem
                )

                CrashHandler.logInfo("MainViewModel: App start destination set to: $startDestination")
            } catch (e: Exception) {
                CrashHandler.handleException(e, "MainViewModel.checkAppState")
                CrashHandler.logError("MainViewModel: Error in checkAppState, defaulting to welcome screen")

                // Always default to welcome on error
                _uiState.value = _uiState.value.copy(
                    startDestination = "welcome",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Mark onboarding as completed
     */
    fun completeOnboarding() {
        appPreferences.isOnboardingCompleted = true
        CrashHandler.logInfo("Onboarding completed")
    }

    /**
     * Skip welcome and onboarding - go directly to dashboard
     */
    fun skipToMain() {
        try {
            CrashHandler.logInfo("MainViewModel: Skipping to main dashboard")
            appPreferences.isFirstLaunch = false
            appPreferences.isOnboardingCompleted = true

            // Update UI state to show dashboard
            _uiState.value = _uiState.value.copy(
                startDestination = "dashboard",
                isLoading = false
            )

            CrashHandler.logInfo("MainViewModel: Skip to main completed successfully")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "MainViewModel.skipToMain")
        }
    }
    
    /**
     * Mark setup as completed
     */
    fun completeSetup(vehicleId: Long) {
        appPreferences.markSetupComplete(vehicleId)
        CrashHandler.logInfo("Setup completed with vehicle ID: $vehicleId")
    }
    
    /**
     * Get app preferences
     */
    fun getAppPreferences(): AppPreferences = appPreferences
    
    /**
     * Toggle demo mode
     */
    fun toggleDemoMode() {
        appPreferences.demoMode = !appPreferences.demoMode
        _uiState.value = _uiState.value.copy(demoMode = appPreferences.demoMode)
        CrashHandler.logInfo("Demo mode ${if (appPreferences.demoMode) "enabled" else "disabled"}")
    }
    
    /**
     * Update keep screen on setting
     */
    fun setKeepScreenOn(enabled: Boolean) {
        appPreferences.keepScreenOn = enabled
        _uiState.value = _uiState.value.copy(keepScreenOn = enabled)
        CrashHandler.logInfo("Keep screen on: $enabled")
    }
    
    /**
     * Update theme mode
     */
    fun setThemeMode(themeMode: String) {
        appPreferences.themeMode = themeMode
        _uiState.value = _uiState.value.copy(themeMode = themeMode)
        CrashHandler.logInfo("Theme mode set to: $themeMode")
    }
    
    /**
     * Update unit system
     */
    fun setUnitSystem(unitSystem: String) {
        appPreferences.unitSystem = unitSystem
        _uiState.value = _uiState.value.copy(unitSystem = unitSystem)
        CrashHandler.logInfo("Unit system set to: $unitSystem")
    }
    
    /**
     * Get current active vehicle
     */
    fun getActiveVehicleId(): Long = appPreferences.activeVehicleId
    
    /**
     * Set active vehicle
     */
    fun setActiveVehicle(vehicleId: Long) {
        viewModelScope.launch {
            try {
                val result = vehicleRepository.setActiveVehicle(vehicleId)
                if (result.isSuccess) {
                    appPreferences.activeVehicleId = vehicleId
                    CrashHandler.logInfo("Active vehicle set to: $vehicleId")
                }
            } catch (e: Exception) {
                CrashHandler.handleException(e, "MainViewModel.setActiveVehicle")
            }
        }
    }
}

/**
 * Main UI State
 */
data class MainUiState(
    val startDestination: String = "welcome",
    val isLoading: Boolean = true,
    val demoMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val themeMode: String = "system",
    val unitSystem: String = "metric"
)
