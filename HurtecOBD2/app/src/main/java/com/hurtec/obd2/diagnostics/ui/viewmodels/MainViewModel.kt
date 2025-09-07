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
    private fun checkAppState() {
        viewModelScope.launch {
            try {
                val isFirstLaunch = appPreferences.isFirstLaunch
                val isOnboardingCompleted = appPreferences.isOnboardingCompleted
                val hasActiveVehicle = appPreferences.activeVehicleId != -1L

                // Check if we have any vehicles in database
                val vehicleCount = try {
                    val result = vehicleRepository.getVehicleCount()
                    result.getOrNull() ?: 0
                } catch (e: Exception) {
                    0
                }

                val startDestination = when {
                    isFirstLaunch -> "welcome"
                    !isOnboardingCompleted -> "onboarding"
                    vehicleCount == 0 -> "vehicle_setup"
                    !hasActiveVehicle && vehicleCount > 0 -> {
                        // Set first vehicle as active if none is set
                        val vehicles = vehicleRepository.getAllVehicles().getOrNull()
                        if (!vehicles.isNullOrEmpty()) {
                            setActiveVehicle(vehicles.first().id)
                        }
                        "dashboard"
                    }
                    else -> "dashboard"
                }

                _uiState.value = _uiState.value.copy(
                    startDestination = startDestination,
                    isLoading = false,
                    demoMode = appPreferences.demoMode,
                    keepScreenOn = appPreferences.keepScreenOn,
                    themeMode = appPreferences.themeMode,
                    unitSystem = appPreferences.unitSystem
                )

                CrashHandler.logInfo("App start destination: $startDestination (vehicles: $vehicleCount, active: ${appPreferences.activeVehicleId})")
            } catch (e: Exception) {
                CrashHandler.handleException(e, "MainViewModel.checkAppState")
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
