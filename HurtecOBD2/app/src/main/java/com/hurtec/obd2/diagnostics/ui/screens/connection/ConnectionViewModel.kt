package com.hurtec.obd2.diagnostics.ui.screens.connection

import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import com.hurtec.obd2.diagnostics.obd.communication.ConnectionState
import com.hurtec.obd2.diagnostics.obd.communication.ConnectionType
import com.hurtec.obd2.diagnostics.obd.communication.ObdDevice
import com.hurtec.obd2.diagnostics.obd.communication.DeviceType
import com.hurtec.obd2.diagnostics.ui.base.BaseViewModel
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the connection screen
 */
@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val communicationManager: CommunicationManager
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    val connectionState = communicationManager.connectionState
    val connectionType = communicationManager.connectionType

    init {
        // Start scanning for devices when ViewModel is created
        scanForDevices()
    }

    fun scanForDevices() {
        safeExecute(showLoading = true) {
            _uiState.value = _uiState.value.copy(isScanning = true, error = null)

            try {
                CrashHandler.logInfo("Starting device scan...")

                // Scan for Bluetooth devices
                val bluetoothDevices = communicationManager.getAvailableBluetoothDevices()
                CrashHandler.logInfo("Found ${bluetoothDevices.size} Bluetooth devices")

                // Scan for USB devices
                val usbDevices = communicationManager.getAvailableUsbDevices()
                CrashHandler.logInfo("Found ${usbDevices.size} USB devices")

                // Scan for WiFi devices
                val wifiDevices = communicationManager.getAvailableWifiDevices()
                CrashHandler.logInfo("Found ${wifiDevices.size} WiFi devices")

                val allDevices = bluetoothDevices + usbDevices + wifiDevices

                _uiState.value = _uiState.value.copy(
                    devices = allDevices,
                    isScanning = false,
                    error = null
                )

                CrashHandler.logInfo("Device scan completed: ${allDevices.size} total devices")

            } catch (e: Exception) {
                CrashHandler.handleException(e, "ConnectionViewModel.scanForDevices")
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = "Failed to scan for devices: ${e.message}"
                )
                throw e // Let safeExecute handle the error
            }
        }
    }

    fun connectToDevice(device: ObdDevice) {
        safeExecute(showLoading = true) {
            _uiState.value = _uiState.value.copy(
                connectingToDevice = device.id,
                error = null
            )

            try {
                CrashHandler.logInfo("Connecting to device: ${device.name} (${device.type})")

                val result = when (device.type) {
                    DeviceType.BLUETOOTH -> communicationManager.connectBluetooth(device)
                    DeviceType.USB -> communicationManager.connectUsb(device)
                    DeviceType.WIFI -> communicationManager.connectWifi(device)
                }

                if (result.isFailure) {
                    val errorMessage = "Connection failed: ${result.exceptionOrNull()?.message}"
                    _uiState.value = _uiState.value.copy(
                        connectingToDevice = null,
                        error = errorMessage
                    )
                    CrashHandler.logError("Connection failed: ${result.exceptionOrNull()?.message}")
                    throw Exception(errorMessage)
                } else {
                    _uiState.value = _uiState.value.copy(
                        connectingToDevice = null,
                        connectedDevice = device,
                        error = null
                    )
                    CrashHandler.logInfo("Successfully connected to device: ${device.name}")
                }

            } catch (e: Exception) {
                CrashHandler.handleException(e, "ConnectionViewModel.connectToDevice")
                _uiState.value = _uiState.value.copy(
                    connectingToDevice = null,
                    error = "Connection error: ${e.message}"
                )
                throw e // Let safeExecute handle the error
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                communicationManager.disconnect()
                _uiState.value = _uiState.value.copy(
                    connectedDevice = null,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Disconnect error: ${e.message}"
                )
            }
        }
    }

    fun clearUiError() {
        clearError() // Call BaseViewModel's clearError
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun testConnection() {
        viewModelScope.launch {
            try {
                val result = communicationManager.sendCommand("ATZ")
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        lastTestResult = "Connection test successful: ${result.getOrNull()}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Connection test failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Test error: ${e.message}"
                )
            }
        }
    }
}

/**
 * UI state for the connection screen
 */
data class ConnectionUiState(
    val devices: List<ObdDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectingToDevice: String? = null,
    val connectedDevice: ObdDevice? = null,
    val error: String? = null,
    val lastTestResult: String? = null
) {
    val bluetoothDevices: List<ObdDevice>
        get() = devices.filter { it.type == DeviceType.BLUETOOTH }

    val usbDevices: List<ObdDevice>
        get() = devices.filter { it.type == DeviceType.USB }

    val wifiDevices: List<ObdDevice>
        get() = devices.filter { it.type == DeviceType.WIFI }

    val hasDevices: Boolean
        get() = devices.isNotEmpty()
}
