package com.hurtec.obd2.diagnostics.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.database.entities.VehicleEntity
import com.hurtec.obd2.diagnostics.database.repository.VehicleRepository
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Vehicle Setup Screen
 */
@HiltViewModel
class VehicleSetupViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
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
