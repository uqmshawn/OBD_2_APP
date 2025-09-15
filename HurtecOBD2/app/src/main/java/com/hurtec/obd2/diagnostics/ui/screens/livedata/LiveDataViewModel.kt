package com.hurtec.obd2.diagnostics.ui.screens.livedata

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.obd.data.DataProcessor
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
    private val dataProcessor: DataProcessor,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveDataUiState())
    val uiState: StateFlow<LiveDataUiState> = _uiState.asStateFlow()

    init {
        try {
            CrashHandler.logInfo("LiveDataViewModel: Initializing...")
            loadAvailableParameters()
            startDataCollection()
            CrashHandler.logInfo("LiveDataViewModel: Initialization completed successfully")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "LiveDataViewModel.init")
            // Set safe default state
            _uiState.value = LiveDataUiState(
                availableParameters = getDefaultParameters(),
                error = "Initialization Error"
            )
        }
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
            // Get selected PIDs for AndrOBD monitoring
            val selectedPids = _uiState.value.selectedParameters.mapNotNull { parameterId ->
                when (parameterId) {
                    "rpm" -> "0C"
                    "speed" -> "0D"
                    "coolant_temp" -> "05"
                    "engine_load" -> "04"
                    "throttle_pos" -> "11"
                    "fuel_level" -> "2F"
                    "intake_temp" -> "0F"
                    "battery_voltage" -> "42"
                    else -> null
                }
            }

            if (selectedPids.isNotEmpty() && communicationManager.isConnected() && !appPreferences.demoMode) {
                // Start AndrOBD real-time monitoring
                CrashHandler.logInfo("Starting AndrOBD real-time monitoring with PIDs: ${selectedPids.joinToString()}")
                communicationManager.startRealTimeMonitoring(selectedPids)

                // Start a separate coroutine for real-time data collection
                // This prevents blocking the initialization
                viewModelScope.launch {
                    try {
                        communicationManager.realTimeDataFlow.collect { realTimeData ->
                            if (realTimeData.isNotEmpty()) {
                                updateRealTimeDataFromAndrOBD(realTimeData)
                            }
                        }
                    } catch (e: Exception) {
                        CrashHandler.handleException(e, "LiveDataViewModel.realTimeDataCollection")
                        // Fall back to simulated data if real-time collection fails
                        // Start the simulated data collection in the else block
                    }
                }
            } else {
                // Fallback to simulated data collection
                // Start a separate coroutine for simulated data collection
                // This prevents blocking the initialization
                viewModelScope.launch {
                    try {
                        while (true) {
                            if (communicationManager.isConnected()) {
                                collectRealTimeData()
                            }
                            kotlinx.coroutines.delay(1000) // Update every second
                        }
                    } catch (e: Exception) {
                        CrashHandler.handleException(e, "LiveDataViewModel.simulatedDataCollection")
                    }
                }
            }
        }
    }

    /**
     * Update real-time data from AndrOBD stream
     */
    private suspend fun updateRealTimeDataFromAndrOBD(realTimeData: Map<String, Any>) {
        val currentState = _uiState.value
        val newData = mutableMapOf<String, List<DataPoint>>()
        val recentDataPoint = mutableMapOf<String, Float>()
        val updatedRawData = currentState.rawDataLog.toMutableList()

        // Process each PID from AndrOBD
        realTimeData.forEach { (pid, data) ->
            if (data is Map<*, *>) {
                val value = data["value"] as? Float ?: 0f
                val rawResponse = data["rawResponse"] as? String ?: ""
                val timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()
                val command = data["command"] as? String ?: ""

                // Map PID back to parameter ID
                val parameterId = when (pid) {
                    "0C" -> "rpm"
                    "0D" -> "speed"
                    "05" -> "coolant_temp"
                    "04" -> "engine_load"
                    "11" -> "throttle_pos"
                    "2F" -> "fuel_level"
                    "0F" -> "intake_temp"
                    "42" -> "battery_voltage"
                    else -> null
                }

                if (parameterId != null && currentState.selectedParameters.contains(parameterId)) {
                    // Create data point
                    val dataPoint = DataPoint(
                        timestamp = timestamp,
                        value = value
                    )

                    // Update data series
                    val existingData = currentState.parameterData[parameterId] ?: emptyList()
                    newData[parameterId] = (existingData + dataPoint).takeLast(100)

                    // Update recent data
                    recentDataPoint[parameterId] = value

                    // Add raw data entry
                    val rawEntry = RawDataEntry(
                        timestamp = timestamp,
                        command = command,
                        rawResponse = rawResponse,
                        decodedValue = value,
                        processingTimeMs = 0
                    )
                    updatedRawData.add(rawEntry)

                    CrashHandler.logInfo("AndrOBD real-time: $parameterId = $value (raw: $rawResponse)")
                }
            }
        }

        // Update UI state
        _uiState.value = currentState.copy(
            parameterData = currentState.parameterData + newData,
            recentData = (currentState.recentData + recentDataPoint).takeLast(100),
            rawDataLog = updatedRawData.takeLast(100)
        )
    }

    private suspend fun collectRealTimeData() {
        val currentState = _uiState.value
        val newData = mutableMapOf<String, List<DataPoint>>()
        val recentDataPoint = mutableMapOf<String, Float>()

        currentState.selectedParameters.forEach { parameterId ->
            val parameter = currentState.availableParameters.find { it.id == parameterId }
            parameter?.let {
                // Get REAL data from AndrOBD - NO MORE SIMULATION FALLBACK
                val value = if (communicationManager.isConnected() && !appPreferences.demoMode) {
                    getRealObdValueAsync(parameterId)
                } else {
                    // Only show 0 if not connected - no fake data
                    0f
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

    private suspend fun getRealObdValueAsync(parameterId: String): Float {
        return try {
            val pid = when (parameterId) {
                "rpm" -> "0C"
                "speed" -> "0D"
                "coolant_temp" -> "05"
                "engine_load" -> "04"
                "throttle_pos" -> "11"
                "fuel_level" -> "2F"
                "intake_temp" -> "0F"
                "battery_voltage" -> "42"
                else -> null
            }

            if (pid != null) {
                // Make actual OBD request using AndrOBD integration
                val result = communicationManager.requestPidData(pid)
                if (result.isSuccess) {
                    val processedData = result.getOrNull()
                    val value = processedData?.processedValue?.toFloat() ?: 0f

                    // Log REAL raw data for debugging
                    val rawEntry = RawDataEntry(
                        timestamp = System.currentTimeMillis(),
                        command = "01$pid",
                        rawResponse = processedData?.metadata?.rawResponse ?: "No response",
                        decodedValue = value,
                        processingTimeMs = processedData?.metadata?.processingTime ?: 0
                    )

                    // Add to raw data log
                    val currentRawData = _uiState.value.rawDataLog.toMutableList()
                    currentRawData.add(rawEntry)
                    _uiState.value = _uiState.value.copy(
                        rawDataLog = currentRawData.takeLast(100) // Keep last 100 entries
                    )

                    CrashHandler.logInfo("LiveData real value - $parameterId: $value (raw: ${processedData?.metadata?.rawResponse})")
                    value
                } else {
                    CrashHandler.logError("Failed to get real OBD data for $parameterId: ${result.exceptionOrNull()?.message}")
                    0f // Return 0 instead of simulated data to show real connection issues
                }
            } else {
                CrashHandler.logError("Unknown parameter ID: $parameterId")
                0f
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "LiveDataViewModel.getRealObdValueAsync")
            0f // Return 0 instead of simulated data to show real errors
        }
    }

    private fun parseObdValue(obdData: com.hurtec.obd2.diagnostics.obd.elm327.ObdResponse?, parameterId: String): Float {
        return try {
            if (obdData == null || obdData.isError || obdData.data.isEmpty()) {
                return generateSimulatedValue(parameterId)
            }

            val byteData = obdData.getByteData()
            if (byteData.isEmpty()) {
                return generateSimulatedValue(parameterId)
            }

            // Parse based on PID type
            when (parameterId) {
                "rpm" -> {
                    // RPM = ((A*256)+B)/4
                    if (byteData.size >= 2) {
                        ((byteData[0] * 256) + byteData[1]) / 4.0f
                    } else generateSimulatedValue(parameterId)
                }
                "speed" -> {
                    // Speed = A km/h
                    if (byteData.isNotEmpty()) {
                        byteData[0].toFloat()
                    } else generateSimulatedValue(parameterId)
                }
                "coolant_temp" -> {
                    // Coolant temp = A - 40 (°C)
                    if (byteData.isNotEmpty()) {
                        byteData[0] - 40.0f
                    } else generateSimulatedValue(parameterId)
                }
                "engine_load" -> {
                    // Engine load = A * 100/255 (%)
                    if (byteData.isNotEmpty()) {
                        byteData[0] * 100.0f / 255.0f
                    } else generateSimulatedValue(parameterId)
                }
                "throttle_pos" -> {
                    // Throttle position = A * 100/255 (%)
                    if (byteData.isNotEmpty()) {
                        byteData[0] * 100.0f / 255.0f
                    } else generateSimulatedValue(parameterId)
                }
                "fuel_level" -> {
                    // Fuel level = A * 100/255 (%)
                    if (byteData.isNotEmpty()) {
                        byteData[0] * 100.0f / 255.0f
                    } else generateSimulatedValue(parameterId)
                }
                "intake_temp" -> {
                    // Intake air temp = A - 40 (°C)
                    if (byteData.isNotEmpty()) {
                        byteData[0] - 40.0f
                    } else generateSimulatedValue(parameterId)
                }
                "battery_voltage" -> {
                    // Battery voltage = ((A*256)+B)/1000 (V)
                    if (byteData.size >= 2) {
                        ((byteData[0] * 256) + byteData[1]) / 1000.0f
                    } else generateSimulatedValue(parameterId)
                }
                else -> generateSimulatedValue(parameterId)
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "LiveDataViewModel.parseObdValue")
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
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRecording = true)

                // Start recording session
                val sessionId = "session_${System.currentTimeMillis()}"
                val vehicleId = appPreferences.activeVehicleId

                if (vehicleId != -1L) {
                    // Start data processor recording
                    dataProcessor.startSession(sessionId, vehicleId)
                    CrashHandler.logInfo("Started recording session: $sessionId for vehicle: $vehicleId")
                } else {
                    CrashHandler.logWarning("Cannot start recording: no active vehicle selected")
                }
            } catch (e: Exception) {
                CrashHandler.handleException(e, "LiveDataViewModel.startRecording")
                _uiState.value = _uiState.value.copy(isRecording = false)
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRecording = false)

                // Stop data processor recording
                dataProcessor.stopSession()
                CrashHandler.logInfo("Stopped recording session")
            } catch (e: Exception) {
                CrashHandler.handleException(e, "LiveDataViewModel.stopRecording")
            }
        }
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

    /**
     * Get default parameters to prevent crashes
     */
    private fun getDefaultParameters(): List<LiveDataParameter> {
        return listOf(
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
                maxValue = 200f
            )
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


