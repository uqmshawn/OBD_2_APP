package com.hurtec.obd2.diagnostics.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.data.preferences.AppPreferences
import com.hurtec.obd2.diagnostics.database.repository.VehicleRepository
import com.hurtec.obd2.diagnostics.database.repository.ObdDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val vehicleRepository: VehicleRepository,
    private val obdDataRepository: ObdDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadStatistics()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Load settings from actual preferences
            _uiState.value = _uiState.value.copy(
                autoConnect = appPreferences.autoConnect,
                keepScreenOn = appPreferences.keepScreenOn,
                enableLogging = appPreferences.dataLoggingEnabled,
                demoMode = appPreferences.demoMode,
                dataRetentionDays = 30, // Default value for now
                temperatureUnit = TemperatureUnit.FAHRENHEIT, // Default value for now
                distanceUnit = DistanceUnit.MILES, // Default value for now
                pressureUnit = PressureUnit.PSI, // Default value for now
                theme = AppTheme.SYSTEM // Default value for now
            )
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                // Simulate statistics loading
                kotlinx.coroutines.delay(500)

                _uiState.value = _uiState.value.copy(
                    statistics = AppStatistics(
                        totalVehicles = 2,
                        totalSessions = 25,
                        totalDataPoints = 15420,
                        databaseSize = "2.3 MB"
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load statistics: ${e.message}"
                )
            }
        }
    }

    fun updateAutoConnect(enabled: Boolean) {
        appPreferences.autoConnect = enabled
        _uiState.value = _uiState.value.copy(autoConnect = enabled)
        CrashHandler.logInfo("Auto connect updated: $enabled")
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        appPreferences.keepScreenOn = enabled
        _uiState.value = _uiState.value.copy(keepScreenOn = enabled)
        CrashHandler.logInfo("Keep screen on updated: $enabled")
    }

    fun updateEnableLogging(enabled: Boolean) {
        appPreferences.dataLoggingEnabled = enabled
        _uiState.value = _uiState.value.copy(enableLogging = enabled)
        CrashHandler.logInfo("Data logging updated: $enabled")
    }

    fun updateDataRetentionDays(days: Int) {
        // Save to preferences (we'll add this key to AppPreferences)
        _uiState.value = _uiState.value.copy(dataRetentionDays = days)
        CrashHandler.logInfo("Data retention days updated: $days")
    }

    fun updateTemperatureUnit(unit: TemperatureUnit) {
        // Save to preferences (we'll add this key to AppPreferences)
        _uiState.value = _uiState.value.copy(temperatureUnit = unit)
        CrashHandler.logInfo("Temperature unit updated: ${unit.displayName}")
    }

    fun updateDistanceUnit(unit: DistanceUnit) {
        // Save to preferences (we'll add this key to AppPreferences)
        _uiState.value = _uiState.value.copy(distanceUnit = unit)
        CrashHandler.logInfo("Distance unit updated: ${unit.displayName}")
    }

    fun updatePressureUnit(unit: PressureUnit) {
        // Save to preferences (we'll add this key to AppPreferences)
        _uiState.value = _uiState.value.copy(pressureUnit = unit)
        CrashHandler.logInfo("Pressure unit updated: ${unit.displayName}")
    }

    fun updateTheme(theme: AppTheme) {
        // Save to preferences (we'll add this key to AppPreferences)
        _uiState.value = _uiState.value.copy(theme = theme)
        CrashHandler.logInfo("Theme updated: ${theme.displayName}")
    }

    fun updateDemoMode(enabled: Boolean) {
        appPreferences.demoMode = enabled
        _uiState.value = _uiState.value.copy(demoMode = enabled)
        CrashHandler.logInfo("Demo mode updated: $enabled")
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, error = null)

            try {
                CrashHandler.logInfo("Starting data export...")

                // Export vehicle data
                val vehicleResult = vehicleRepository.getAllVehicles()
                val vehicles = vehicleResult.getOrNull() ?: emptyList()

                // Create export data
                val exportData = buildString {
                    appendLine("Hurtec OBD-II Data Export")
                    appendLine("Export Date: ${java.util.Date()}")
                    appendLine("=====================================")
                    appendLine()

                    appendLine("VEHICLES (${vehicles.size}):")
                    vehicles.forEach { vehicle ->
                        appendLine("- ${vehicle.year} ${vehicle.make} ${vehicle.model}")
                        appendLine("  VIN: ${vehicle.vin}")
                        appendLine("  Engine: ${vehicle.engineSize}")
                        appendLine()

                        // Export OBD data for each vehicle
                        try {
                            val obdDataResult = obdDataRepository.getObdDataByVehicle(vehicle.id)
                            val obdData = obdDataResult.getOrNull() ?: emptyList()
                            appendLine("  OBD Data Entries: ${obdData.size}")
                            obdData.take(10).forEach { data -> // Limit to first 10 entries per vehicle
                                appendLine("    - ${java.util.Date(data.timestamp)}: PID ${data.pid} = ${data.formattedValue}")
                            }
                            if (obdData.size > 10) {
                                appendLine("    ... and ${obdData.size - 10} more entries")
                            }
                        } catch (e: Exception) {
                            appendLine("    Error loading OBD data: ${e.message}")
                        }
                        appendLine()
                    }

                    appendLine("App Preferences:")
                    val prefs = appPreferences.getAllPreferences()
                    prefs.forEach { (key, value) ->
                        appendLine("$key = $value")
                    }
                }

                // In a real app, you would save this to a file or share it
                CrashHandler.logInfo("Export data prepared: ${exportData.length} characters")

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true
                )
            } catch (e: Exception) {
                CrashHandler.handleException(e, "SettingsViewModel.exportData")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearing = true, error = null)

            try {
                CrashHandler.logInfo("Starting data clear operation...")

                // Get all vehicles and clear their OBD data
                val vehicleResult = vehicleRepository.getAllVehicles()
                val vehicles = vehicleResult.getOrNull() ?: emptyList()

                var totalDeleted = 0
                vehicles.forEach { vehicle ->
                    try {
                        // Delete OBD data for each vehicle (keeping last 7 days)
                        val cleanupResult = obdDataRepository.cleanupOldData(vehicle.id, 0) // 0 days = delete all
                        val deletedCount = cleanupResult.getOrNull() ?: 0
                        totalDeleted += deletedCount
                    } catch (e: Exception) {
                        CrashHandler.handleException(e, "Failed to clear data for vehicle ${vehicle.id}")
                    }
                }

                // Optionally clear vehicle data (uncomment if needed)
                // val vehicleClearResult = vehicleRepository.deleteAllVehicles()

                // Optionally reset app preferences (uncomment if needed)
                // appPreferences.resetAll()

                CrashHandler.logInfo("Data clear completed successfully: deleted $totalDeleted OBD entries")

                loadStatistics() // Refresh statistics

                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    clearSuccess = true
                )
            } catch (e: Exception) {
                CrashHandler.handleException(e, "SettingsViewModel.clearAllData")
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    error = "Clear failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false)
    }

    fun clearClearSuccess() {
        _uiState.value = _uiState.value.copy(clearSuccess = false)
    }
}

/**
 * UI state for the settings screen
 */
data class SettingsUiState(
    val autoConnect: Boolean = true,
    val keepScreenOn: Boolean = false,
    val enableLogging: Boolean = true,
    val dataRetentionDays: Int = 30,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val distanceUnit: DistanceUnit = DistanceUnit.MILES,
    val pressureUnit: PressureUnit = PressureUnit.PSI,
    val theme: AppTheme = AppTheme.SYSTEM,
    val demoMode: Boolean = false,
    val statistics: AppStatistics? = null,
    val isExporting: Boolean = false,
    val isClearing: Boolean = false,
    val exportSuccess: Boolean = false,
    val clearSuccess: Boolean = false,
    val error: String? = null
)

/**
 * App statistics
 */
data class AppStatistics(
    val totalVehicles: Int,
    val totalSessions: Int,
    val totalDataPoints: Int,
    val databaseSize: String
)

/**
 * Temperature units
 */
enum class TemperatureUnit(val displayName: String) {
    CELSIUS("Celsius (°C)"),
    FAHRENHEIT("Fahrenheit (°F)")
}

/**
 * Distance units
 */
enum class DistanceUnit(val displayName: String) {
    KILOMETERS("Kilometers"),
    MILES("Miles")
}

/**
 * Pressure units
 */
enum class PressureUnit(val displayName: String) {
    PSI("PSI"),
    BAR("Bar"),
    KPA("kPa")
}

/**
 * App themes
 */
enum class AppTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}
