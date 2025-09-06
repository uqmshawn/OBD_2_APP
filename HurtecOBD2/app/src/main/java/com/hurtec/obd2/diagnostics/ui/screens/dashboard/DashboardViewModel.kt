package com.hurtec.obd2.diagnostics.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Modern Dashboard ViewModel with StateFlow and Coroutines
 * Replaces the old LiveData approach with modern reactive patterns
 */
@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Default PIDs to monitor
    private val defaultPids = listOf(
        "010C", // Engine RPM
        "010D", // Vehicle Speed
        "0105", // Engine Coolant Temperature
        "012F", // Fuel Tank Level
        "0104", // Calculated Engine Load
        "0111"  // Throttle Position
    )

    init {
        // For now, just simulate data
        simulateData()
    }

    fun refreshData() {
        simulateData()
    }

    fun simulateData() {
        // For testing purposes - simulate gauge data
        val simulatedGauges = listOf(
            GaugeData(
                id = "010C",
                name = "Engine RPM",
                value = (800..3000).random().toFloat(),
                unit = "RPM",
                minValue = 0f,
                maxValue = 8000f,
                color = EngineBlue,
                warningThreshold = 6000f,
                criticalThreshold = 7000f
            ),
            GaugeData(
                id = "010D",
                name = "Speed",
                value = (0..120).random().toFloat(),
                unit = "km/h",
                minValue = 0f,
                maxValue = 200f,
                color = EngineGreen,
                warningThreshold = 120f,
                criticalThreshold = 160f
            ),
            GaugeData(
                id = "0105",
                name = "Coolant Temp",
                value = (70..95).random().toFloat(),
                unit = "°C",
                minValue = -40f,
                maxValue = 120f,
                color = EngineBlue,
                warningThreshold = 95f,
                criticalThreshold = 105f
            ),
            GaugeData(
                id = "012F",
                name = "Fuel Level",
                value = (20..100).random().toFloat(),
                unit = "%",
                minValue = 0f,
                maxValue = 100f,
                color = EngineYellow,
                warningThreshold = 20f,
                criticalThreshold = 10f
            ),
            GaugeData(
                id = "0104",
                name = "Engine Load",
                value = (10..80).random().toFloat(),
                unit = "%",
                minValue = 0f,
                maxValue = 100f,
                color = EngineGreen,
                warningThreshold = 80f,
                criticalThreshold = 95f
            ),
            GaugeData(
                id = "0111",
                name = "Throttle",
                value = (0..50).random().toFloat(),
                unit = "%",
                minValue = 0f,
                maxValue = 100f,
                color = EngineRed
            )
        )

        _uiState.value = _uiState.value.copy(
            gaugeData = simulatedGauges,
            connectionState = ConnectionState.CONNECTED
        )
    }
}

/**
 * Dashboard UI state
 */
data class DashboardUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val gaugeData: List<GaugeData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
