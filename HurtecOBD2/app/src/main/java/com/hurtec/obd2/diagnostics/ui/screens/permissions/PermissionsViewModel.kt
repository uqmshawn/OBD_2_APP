package com.hurtec.obd2.diagnostics.ui.screens.permissions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.utils.DeviceCapabilities
import com.hurtec.obd2.diagnostics.utils.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for permissions screen
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        loadPermissionState()
    }

    private fun loadPermissionState() {
        viewModelScope.launch {
            try {
                val capabilities = permissionManager.getDeviceCapabilities()
                val permissionItems = createPermissionItems(capabilities)
                
                _uiState.value = _uiState.value.copy(
                    capabilities = capabilities,
                    permissionItems = permissionItems,
                    isLoading = false
                )
                
                CrashHandler.logInfo("Permission state loaded: ${capabilities}")
            } catch (e: Exception) {
                CrashHandler.handleException(e, "PermissionsViewModel.loadPermissionState")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load permissions: ${e.message}"
                )
            }
        }
    }

    private fun createPermissionItems(capabilities: DeviceCapabilities): List<PermissionItem> {
        return listOf(
            PermissionItem(
                title = "Bluetooth Access",
                description = "Required to connect to Bluetooth OBD-II adapters and scan for devices",
                icon = Icons.Default.Bluetooth,
                permissions = permissionManager.getRequiredBluetoothPermissions(),
                isRequired = true,
                isGranted = capabilities.bluetoothPermissions
            ),
            PermissionItem(
                title = "Location Access",
                description = "Required for Bluetooth device scanning on Android 6+",
                icon = Icons.Default.LocationOn,
                permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                isRequired = true,
                isGranted = capabilities.locationPermissions
            ),
            PermissionItem(
                title = "Storage Access",
                description = "Required to save diagnostic data and export logs",
                icon = Icons.Default.Storage,
                permissions = permissionManager.getRequiredStoragePermissions(),
                isRequired = false,
                isGranted = capabilities.storagePermissions
            )
        )
    }

    fun onPermissionsResult(permissions: Map<String, Boolean>) {
        viewModelScope.launch {
            try {
                CrashHandler.logInfo("Permission results: $permissions")
                
                // Reload permission state after result
                loadPermissionState()
                
            } catch (e: Exception) {
                CrashHandler.handleException(e, "PermissionsViewModel.onPermissionsResult")
            }
        }
    }

    fun getMissingPermissions(): List<String> {
        return try {
            permissionManager.getMissingCriticalPermissions()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PermissionsViewModel.getMissingPermissions")
            emptyList()
        }
    }

    fun refreshPermissions() {
        loadPermissionState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for permissions screen
 */
data class PermissionsUiState(
    val capabilities: DeviceCapabilities? = null,
    val permissionItems: List<PermissionItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
