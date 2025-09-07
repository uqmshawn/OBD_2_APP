package com.hurtec.obd2.diagnostics.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.obd.communication.ConnectionState
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.obd.elm327.DtcInfo
import com.hurtec.obd2.diagnostics.obd.elm327.DtcStatus
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Diagnostics Screen
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val communicationManager: CommunicationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    init {
        // Initialize with some sample DTCs for demo
        loadSampleDtcs()
    }
    
    /**
     * Scan for DTCs
     */
    fun scanDtcs() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isScanning = true,
                    error = null
                )

                // Check if connected to OBD
                if (!communicationManager.isConnected()) {
                    // Load sample DTCs for demo mode
                    loadSampleDtcs()
                    _uiState.value = _uiState.value.copy(isScanning = false)
                    return@launch
                }

                // Read real DTCs from vehicle
                val dtcResult = communicationManager.readDtcs()

                if (dtcResult.isSuccess) {
                    val dtcs = dtcResult.getOrNull() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        dtcs = dtcs,
                        isScanning = false
                    )
                    CrashHandler.logInfo("DTC scan completed: ${dtcs.size} codes found")
                } else {
                    // Fallback to sample data if real scan fails
                    loadSampleDtcs()
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        error = "Real DTC scan failed, showing demo data: ${dtcResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                CrashHandler.handleException(e, "DiagnosticsViewModel.scanDtcs")
                // Fallback to sample data
                loadSampleDtcs()
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = "DTC scan failed, showing demo data: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear DTCs
     */
    fun clearDtcs() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isClearing = true,
                    error = null
                )

                // Check if connected to OBD
                if (!communicationManager.isConnected()) {
                    // Simulate clearing for demo mode
                    kotlinx.coroutines.delay(1500)
                    _uiState.value = _uiState.value.copy(
                        dtcs = emptyList(),
                        isClearing = false,
                        clearSuccess = true
                    )
                    return@launch
                }

                // Clear real DTCs from vehicle
                val clearResult = communicationManager.clearDtcs()

                if (clearResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        dtcs = emptyList(),
                        isClearing = false,
                        clearSuccess = true
                    )
                    CrashHandler.logInfo("DTCs cleared successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isClearing = false,
                        error = "Failed to clear DTCs: ${clearResult.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                CrashHandler.handleException(e, "DiagnosticsViewModel.clearDtcs")
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    error = "Failed to clear DTCs: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Clear success message
     */
    fun clearClearSuccess() {
        _uiState.value = _uiState.value.copy(clearSuccess = false)
    }
    
    /**
     * Load sample DTCs for demo
     */
    private fun loadSampleDtcs() {
        val sampleDtcs = listOf(
            DtcInfo(
                code = "P0171",
                description = "System Too Lean (Bank 1)",
                status = DtcStatus.STORED
            ),
            DtcInfo(
                code = "P0300",
                description = "Random/Multiple Cylinder Misfire Detected",
                status = DtcStatus.PENDING
            ),
            DtcInfo(
                code = "P0420",
                description = "Catalyst System Efficiency Below Threshold (Bank 1)",
                status = DtcStatus.PERMANENT
            )
        )
        
        _uiState.value = _uiState.value.copy(dtcs = sampleDtcs)
    }
    
    /**
     * Toggle connection state for demo
     */
    fun toggleConnection() {
        _connectionState.value = when (_connectionState.value) {
            ConnectionState.DISCONNECTED -> ConnectionState.CONNECTING
            ConnectionState.CONNECTING -> ConnectionState.CONNECTED
            ConnectionState.CONNECTED -> ConnectionState.DISCONNECTED
            ConnectionState.DISCONNECTING -> ConnectionState.DISCONNECTED
            ConnectionState.ERROR -> ConnectionState.DISCONNECTED
        }
    }
}

/**
 * UI State for Diagnostics Screen
 */
data class DiagnosticsUiState(
    val dtcs: List<DtcInfo> = emptyList(),
    val isScanning: Boolean = false,
    val isClearing: Boolean = false,
    val clearSuccess: Boolean = false,
    val error: String? = null
)
