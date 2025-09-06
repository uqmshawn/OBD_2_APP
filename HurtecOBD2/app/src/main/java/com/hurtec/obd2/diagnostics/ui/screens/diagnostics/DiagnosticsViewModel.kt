package com.hurtec.obd2.diagnostics.ui.screens.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.obd.elm327.DtcInfo
import com.hurtec.obd2.diagnostics.obd.elm327.DtcStatus
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the diagnostics screen
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val communicationManager: CommunicationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    val connectionState = communicationManager.connectionState

    init {
        // Load DTCs when ViewModel is created
        loadDtcs()
    }

    fun loadDtcs() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Simulate DTC reading
                kotlinx.coroutines.delay(2000)

                // Generate sample DTCs for demonstration
                val sampleDtcs = listOf(
                    DtcInfo(code = "P0171", status = DtcStatus.STORED),
                    DtcInfo(code = "P0300", status = DtcStatus.PENDING),
                    DtcInfo(code = "P0420", status = DtcStatus.STORED)
                )

                _uiState.value = _uiState.value.copy(
                    dtcs = sampleDtcs,
                    isLoading = false,
                    lastScanTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                CrashHandler.handleException(e, "DiagnosticsViewModel.loadDtcs")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error reading DTCs: ${e.message}"
                )
            }
        }
    }

    fun clearDtcs() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isClearing = true, error = null)

                // Simulate clearing DTCs
                kotlinx.coroutines.delay(1500)

                _uiState.value = _uiState.value.copy(
                    dtcs = emptyList(),
                    isClearing = false,
                    clearSuccess = true
                )
            } catch (e: Exception) {
                CrashHandler.handleException(e, "DiagnosticsViewModel.clearDtcs")
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    error = "Error clearing DTCs: ${e.message}"
                )
            }
        }
    }

    fun getFreezeFrameData(dtcCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadingFreezeFrame = dtcCode,
                error = null
            )

            try {
                // Simulate freeze frame data for now
                // In a real implementation, you would send specific commands to get freeze frame data
                val freezeFrameData = mapOf(
                    "Engine RPM" to "2500 rpm",
                    "Vehicle Speed" to "65 mph", 
                    "Engine Load" to "45%",
                    "Coolant Temperature" to "195°F",
                    "Fuel Trim" to "+2.5%",
                    "Intake Air Temperature" to "75°F"
                )

                _uiState.value = _uiState.value.copy(
                    loadingFreezeFrame = null,
                    selectedDtcFreezeFrame = dtcCode to freezeFrameData
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loadingFreezeFrame = null,
                    error = "Error loading freeze frame data: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearClearSuccess() {
        _uiState.value = _uiState.value.copy(clearSuccess = false)
    }

    fun clearFreezeFrameData() {
        _uiState.value = _uiState.value.copy(selectedDtcFreezeFrame = null)
    }

    fun getReadinessMonitors() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingReadiness = true, error = null)

            try {
                // Simulate readiness monitor data
                // In a real implementation, you would send Mode 01 PID 01 command
                val monitors = mapOf(
                    "Catalyst" to ReadinessStatus.READY,
                    "Heated Catalyst" to ReadinessStatus.READY,
                    "Evaporative System" to ReadinessStatus.NOT_READY,
                    "Secondary Air System" to ReadinessStatus.NOT_APPLICABLE,
                    "A/C System Refrigerant" to ReadinessStatus.READY,
                    "Oxygen Sensor" to ReadinessStatus.READY,
                    "Oxygen Sensor Heater" to ReadinessStatus.READY,
                    "EGR System" to ReadinessStatus.READY
                )

                _uiState.value = _uiState.value.copy(
                    isLoadingReadiness = false,
                    readinessMonitors = monitors
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingReadiness = false,
                    error = "Error loading readiness monitors: ${e.message}"
                )
            }
        }
    }
}

/**
 * UI state for the diagnostics screen
 */
data class DiagnosticsUiState(
    val dtcs: List<DtcInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isClearing: Boolean = false,
    val isLoadingReadiness: Boolean = false,
    val loadingFreezeFrame: String? = null,
    val error: String? = null,
    val clearSuccess: Boolean = false,
    val lastScanTime: Long? = null,
    val selectedDtcFreezeFrame: Pair<String, Map<String, String>>? = null,
    val readinessMonitors: Map<String, ReadinessStatus> = emptyMap()
) {
    val hasDtcs: Boolean get() = dtcs.isNotEmpty()
    val activeDtcs: List<DtcInfo> get() = dtcs.filter { it.status == DtcStatus.STORED }
    val pendingDtcs: List<DtcInfo> get() = dtcs.filter { it.status == DtcStatus.PENDING }
    val permanentDtcs: List<DtcInfo> get() = dtcs.filter { it.status == DtcStatus.PERMANENT }
}

/**
 * Readiness monitor status
 */
enum class ReadinessStatus {
    READY,
    NOT_READY,
    NOT_APPLICABLE
}
