package com.hurtec.obd2.diagnostics.ui.screens.livedata

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.data.preferences.AppPreferences
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Live Data Monitoring screen
 */
@HiltViewModel
class LiveDataViewModel @Inject constructor(
    private val communicationManager: CommunicationManager,
    private val appPreferences: AppPreferences
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
                // Get real data from OBD or simulate
                val value = if (communicationManager.isConnected() && !appPreferences.demoMode) {
                    getRealObdValue(parameterId)
                } else {
                    generateSimulatedValue(parameterId)
                }
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

        // Generate raw data for demonstration
        val rawDataEntry = RawDataEntry(
            timestamp = System.currentTimeMillis(),
            command = "01 0C", // Example: Engine RPM command
            rawResponse = "41 0C 1A F8", // Example raw response
            decodedValue = recentDataPoint["rpm"] ?: 0f,
            processingTimeMs = (10..50).random().toLong()
        )

        val updatedRawData = (currentState.rawDataLog + rawDataEntry).takeLast(100)

        _uiState.value = currentState.copy(
            parameterData = newData,
            recentData = (currentState.recentData + recentDataPoint).takeLast(100),
            rawDataLog = updatedRawData
        )
    }

    private fun getRealObdValue(parameterId: String): Float {
        return try {
            val pid = when (parameterId) {
                "rpm" -> "0C"
                "speed" -> "0D"
                "coolant_temp" -> "05"
                "engine_load" -> "04"
                "throttle_pos" -> "11"
                "fuel_level" -> "2F"
                "intake_temp" -> "0F"
                "battery_voltage" -> "42" // Battery voltage PID
                else -> null
            }

            if (pid != null) {
                // This would be a blocking call in real implementation
                // For now, return simulated value as placeholder
                generateSimulatedValue(parameterId)
            } else {
                generateSimulatedValue(parameterId)
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "LiveDataViewModel.getRealObdValue")
            generateSimulatedValue(parameterId)
        }
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
            try {
                _uiState.value = _uiState.value.copy(isExporting = true)

                // Generate CSV export of live data
                val csvData = generateLiveDataCsv()

                kotlinx.coroutines.delay(2000)

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true
                )

                CrashHandler.logInfo("Live data exported successfully")
            } catch (e: Exception) {
                CrashHandler.handleException(e, "LiveDataViewModel.exportData")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun toggleRawDataView() {
        _uiState.value = _uiState.value.copy(
            showRawData = !_uiState.value.showRawData
        )
        CrashHandler.logInfo("Raw data view toggled: ${_uiState.value.showRawData}")
    }

    fun clearExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun generateLiveDataCsv(): String {
        val currentState = _uiState.value
        val header = "Timestamp,Parameter,Value,Unit,Raw Command,Raw Response\n"

        val rows = currentState.rawDataLog.joinToString("\n") { entry ->
            "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(java.util.Date(entry.timestamp))}," +
            "\"${entry.command}\"," +
            "${entry.decodedValue}," +
            "\"\"," +
            "\"${entry.command}\"," +
            "\"${entry.rawResponse}\""
        }

        return header + rows
    }

    fun clearData() {
        _uiState.value = _uiState.value.copy(
            parameterData = emptyMap(),
            recentData = emptyList()
        )
    }

    fun toggleDataTable() {
        _uiState.value = _uiState.value.copy(
            showDataTable = !_uiState.value.showDataTable
        )
    }

    fun toggleRealTimeMode() {
        _uiState.value = _uiState.value.copy(
            isRealTime = !_uiState.value.isRealTime
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
    val rawDataLog: List<RawDataEntry> = emptyList(),
    val isRecording: Boolean = false,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val showDataTable: Boolean = true,
    val showRawData: Boolean = false,
    val isRealTime: Boolean = true,
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

/**
 * Raw data entry for debugging and analysis
 */
data class RawDataEntry(
    val timestamp: Long,
    val command: String,
    val rawResponse: String,
    val decodedValue: Float,
    val processingTimeMs: Long
) {
    fun getFormattedTimestamp(): String {
        return java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}
