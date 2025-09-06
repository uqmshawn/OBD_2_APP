package com.hurtec.obd2.diagnostics.ui.screens.livedata

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Live Data Monitoring screen
 */
@HiltViewModel
class LiveDataViewModel @Inject constructor(
    private val communicationManager: CommunicationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveDataUiState())
    val uiState: StateFlow<LiveDataUiState> = _uiState.asStateFlow()

    init {
        loadAvailableParameters()
        startDataCollection()
    }

    private fun loadAvailableParameters() {
        val parameters = listOf(
            LiveDataParameter(
                id = "rpm",
                name = "Engine RPM",
                shortName = "RPM",
                unit = "rpm",
                color = Color.Red,
                minValue = 0f,
                maxValue = 8000f
            ),
            LiveDataParameter(
                id = "speed",
                name = "Vehicle Speed",
                shortName = "Speed",
                unit = "mph",
                color = Color.Blue,
                minValue = 0f,
                maxValue = 120f
            ),
            LiveDataParameter(
                id = "coolant_temp",
                name = "Coolant Temperature",
                shortName = "Coolant",
                unit = "°F",
                color = Color.Green,
                minValue = 32f,
                maxValue = 250f
            ),
            LiveDataParameter(
                id = "engine_load",
                name = "Engine Load",
                shortName = "Load",
                unit = "%",
                color = Color.Magenta,
                minValue = 0f,
                maxValue = 100f
            ),
            LiveDataParameter(
                id = "throttle_pos",
                name = "Throttle Position",
                shortName = "Throttle",
                unit = "%",
                color = Color.Cyan,
                minValue = 0f,
                maxValue = 100f
            ),
            LiveDataParameter(
                id = "intake_temp",
                name = "Intake Air Temperature",
                shortName = "Intake",
                unit = "°F",
                color = Color.Yellow,
                minValue = -40f,
                maxValue = 200f
            ),
            LiveDataParameter(
                id = "fuel_level",
                name = "Fuel Tank Level",
                shortName = "Fuel",
                unit = "%",
                color = Color.Gray,
                minValue = 0f,
                maxValue = 100f
            ),
            LiveDataParameter(
                id = "battery_voltage",
                name = "Battery Voltage",
                shortName = "Battery",
                unit = "V",
                color = Color(0xFF9C27B0),
                minValue = 10f,
                maxValue = 16f
            )
        )

        _uiState.value = _uiState.value.copy(
            availableParameters = parameters,
            selectedParameters = listOf("rpm", "speed", "coolant_temp", "engine_load")
        )
    }

    private fun startDataCollection() {
        viewModelScope.launch {
            // Simulate real-time data collection
            while (true) {
                if (communicationManager.isConnected()) {
                    collectRealTimeData()
                }
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    private suspend fun collectRealTimeData() {
        val currentState = _uiState.value
        val newData = mutableMapOf<String, List<DataPoint>>()
        val recentDataPoint = mutableMapOf<String, Float>()

        currentState.selectedParameters.forEach { parameterId ->
            val parameter = currentState.availableParameters.find { it.id == parameterId }
            parameter?.let {
                // Simulate getting real data from OBD
                val value = generateSimulatedValue(parameterId)
                val dataPoint = DataPoint(
                    timestamp = System.currentTimeMillis(),
                    value = value
                )

                val existingData = currentState.parameterData[parameterId] ?: emptyList()
                val updatedData = (existingData + dataPoint).takeLast(50) // Keep last 50 points

                newData[parameterId] = updatedData
                recentDataPoint[parameterId] = value
            }
        }

        _uiState.value = currentState.copy(
            parameterData = newData,
            recentData = (currentState.recentData + recentDataPoint).takeLast(100)
        )
    }

    private fun generateSimulatedValue(parameterId: String): Float {
        return when (parameterId) {
            "rpm" -> kotlin.random.Random.nextInt(800, 3000).toFloat()
            "speed" -> kotlin.random.Random.nextInt(0, 60).toFloat()
            "coolant_temp" -> kotlin.random.Random.nextInt(180, 210).toFloat()
            "engine_load" -> kotlin.random.Random.nextInt(10, 80).toFloat()
            "throttle_pos" -> kotlin.random.Random.nextInt(0, 50).toFloat()
            "intake_temp" -> kotlin.random.Random.nextInt(70, 120).toFloat()
            "fuel_level" -> kotlin.random.Random.nextInt(20, 100).toFloat()
            "battery_voltage" -> kotlin.random.Random.nextDouble(12.0, 14.5).toFloat()
            else -> 0f
        }
    }

    fun toggleParameter(parameterId: String) {
        val currentState = _uiState.value
        val selectedParameters = if (parameterId in currentState.selectedParameters) {
            currentState.selectedParameters - parameterId
        } else {
            currentState.selectedParameters + parameterId
        }

        _uiState.value = currentState.copy(selectedParameters = selectedParameters)
    }

    fun startRecording() {
        _uiState.value = _uiState.value.copy(isRecording = true)
        // TODO: Implement actual recording logic
    }

    fun stopRecording() {
        _uiState.value = _uiState.value.copy(isRecording = false)
        // TODO: Implement stop recording logic
    }

    fun exportData() {
        viewModelScope.launch {
            // TODO: Implement data export
            // For now, just simulate export
            _uiState.value = _uiState.value.copy(isExporting = true)
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(isExporting = false)
        }
    }

    fun clearData() {
        _uiState.value = _uiState.value.copy(
            parameterData = emptyMap(),
            recentData = emptyList()
        )
    }
}

/**
 * UI state for Live Data screen
 */
data class LiveDataUiState(
    val availableParameters: List<LiveDataParameter> = emptyList(),
    val selectedParameters: List<String> = emptyList(),
    val parameterData: Map<String, List<DataPoint>> = emptyMap(),
    val recentData: List<Map<String, Float>> = emptyList(),
    val isRecording: Boolean = false,
    val isExporting: Boolean = false,
    val showDataTable: Boolean = true,
    val refreshRate: Int = 1000, // milliseconds
    val error: String? = null
)

/**
 * Live data parameter definition
 */
data class LiveDataParameter(
    val id: String,
    val name: String,
    val shortName: String,
    val unit: String,
    val color: Color,
    val minValue: Float,
    val maxValue: Float,
    val warningThreshold: Float? = null,
    val criticalThreshold: Float? = null
)

/**
 * Data point for time series data
 */
data class DataPoint(
    val timestamp: Long,
    val value: Float
)
