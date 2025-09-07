package com.hurtec.obd2.diagnostics.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.database.entities.VehicleEntity
import com.hurtec.obd2.diagnostics.database.repository.VehicleRepository
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.data.preferences.AppPreferences
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Vehicle Setup Screen
 */
@HiltViewModel
class VehicleSetupViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val communicationManager: CommunicationManager,
    private val appPreferences: AppPreferences
) : ViewModel() {
    
    private val _uiState = mutableStateOf<UiState>(UiState.Idle)
    val uiState: State<UiState> = _uiState
    
    /**
     * Add a new vehicle
     */
    fun addVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                
                val result = vehicleRepository.addVehicle(vehicle)
                
                if (result.isSuccess) {
                    val vehicleId = result.getOrThrow()

                    // Mark this vehicle as active and setup as complete
                    appPreferences.markSetupComplete(vehicleId)

                    CrashHandler.logInfo("Vehicle added successfully with ID: $vehicleId")
                    _uiState.value = UiState.Success(vehicleId)
                } else {
                    val error = result.exceptionOrNull()
                    CrashHandler.handleException(error ?: Exception("Unknown error"), "VehicleSetupViewModel.addVehicle")
                    _uiState.value = UiState.Error(error?.message ?: "Failed to add vehicle")
                }
            } catch (e: Exception) {
                CrashHandler.handleException(e, "VehicleSetupViewModel.addVehicle")
                _uiState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    /**
     * Detect VIN from OBD connection
     */
    fun detectVinFromObd(onVinDetected: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                // Check if connected to OBD
                if (!communicationManager.isConnected()) {
                    _uiState.value = UiState.Error("Please connect to OBD adapter first")
                    return@launch
                }

                // Request VIN using Mode 09 PID 02
                val vinResult = communicationManager.getVehicleInfo()

                if (vinResult.isSuccess) {
                    val vehicleInfo = vinResult.getOrNull()
                    val detectedVin = vehicleInfo?.vin ?: ""

                    if (detectedVin.isNotEmpty() && detectedVin.length == 17) {
                        onVinDetected(detectedVin)
                        _uiState.value = UiState.Idle
                        CrashHandler.logInfo("VIN detected from OBD: $detectedVin")
                    } else {
                        _uiState.value = UiState.Error("Invalid VIN received from vehicle")
                    }
                } else {
                    _uiState.value = UiState.Error("Failed to read VIN from vehicle: ${vinResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                CrashHandler.handleException(e, "VehicleSetupViewModel.detectVinFromObd")
                _uiState.value = UiState.Error("VIN detection failed: ${e.message}")
            }
        }
    }

    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = UiState.Idle
    }
    
    /**
     * UI State sealed class
     */
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val vehicleId: Long) : UiState()
        data class Error(val message: String) : UiState()
    }
}
