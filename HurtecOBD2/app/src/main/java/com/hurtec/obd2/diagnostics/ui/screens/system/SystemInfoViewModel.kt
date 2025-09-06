package com.hurtec.obd2.diagnostics.ui.screens.system

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.hardware.HardwareCapabilities
import com.hurtec.obd2.diagnostics.hardware.HardwareManager
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.utils.MemoryInfo
import com.hurtec.obd2.diagnostics.utils.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for system information screen
 */
@HiltViewModel
class SystemInfoViewModel @Inject constructor(
    private val memoryManager: MemoryManager,
    private val hardwareManager: HardwareManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemInfoUiState())
    val uiState: StateFlow<SystemInfoUiState> = _uiState.asStateFlow()

    // Expose flows from managers
    val memoryInfo: StateFlow<MemoryInfo> = memoryManager.memoryInfo as StateFlow<MemoryInfo>
    val hardwareCapabilities: StateFlow<HardwareCapabilities> = hardwareManager.hardwareCapabilities as StateFlow<HardwareCapabilities>

    init {
        loadSystemInfo()
        startPerformanceMonitoring()
    }

    private fun loadSystemInfo() {
        viewModelScope.launch {
            try {
                val systemDetails = SystemDetails(
                    items = mapOf(
                        "Build Version" to Build.VERSION.RELEASE,
                        "SDK Level" to Build.VERSION.SDK_INT.toString(),
                        "Device Model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                        "Board" to Build.BOARD,
                        "Bootloader" to Build.BOOTLOADER,
                        "Brand" to Build.BRAND,
                        "Device" to Build.DEVICE,
                        "Display" to Build.DISPLAY,
                        "Fingerprint" to Build.FINGERPRINT.take(50) + "...",
                        "Hardware" to Build.HARDWARE,
                        "Host" to Build.HOST,
                        "ID" to Build.ID,
                        "Product" to Build.PRODUCT,
                        "Tags" to Build.TAGS,
                        "Type" to Build.TYPE,
                        "User" to Build.USER,
                        "CPU ABI" to Build.SUPPORTED_ABIS.joinToString(", "),
                        "Available Processors" to Runtime.getRuntime().availableProcessors().toString()
                    )
                )

                _uiState.value = _uiState.value.copy(
                    systemDetails = systemDetails,
                    isLoading = false
                )

                CrashHandler.logInfo("System info loaded")

            } catch (e: Exception) {
                CrashHandler.handleException(e, "SystemInfoViewModel.loadSystemInfo")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load system info: ${e.message}"
                )
            }
        }
    }

    private fun startPerformanceMonitoring() {
        viewModelScope.launch {
            try {
                // Simulate performance metrics (in real app, you'd use actual performance monitoring)
                val metrics = PerformanceMetrics(
                    fps = 60,
                    frameTimeMs = 16.7f,
                    cpuUsage = (10..30).random()
                )

                _uiState.value = _uiState.value.copy(
                    performanceMetrics = metrics
                )

            } catch (e: Exception) {
                CrashHandler.handleException(e, "SystemInfoViewModel.startPerformanceMonitoring")
            }
        }
    }

    fun optimizeMemory() {
        viewModelScope.launch {
            try {
                CrashHandler.logInfo("Optimizing memory from UI")
                memoryManager.optimizeMemoryUsage()
                
                _uiState.value = _uiState.value.copy(
                    lastAction = "Memory optimized"
                )

            } catch (e: Exception) {
                CrashHandler.handleException(e, "SystemInfoViewModel.optimizeMemory")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to optimize memory: ${e.message}"
                )
            }
        }
    }

    fun startHardwareMonitoring() {
        viewModelScope.launch {
            try {
                CrashHandler.logInfo("Starting hardware monitoring from UI")
                hardwareManager.startHardwareMonitoring()
                
                _uiState.value = _uiState.value.copy(
                    lastAction = "Hardware monitoring started"
                )

            } catch (e: Exception) {
                CrashHandler.handleException(e, "SystemInfoViewModel.startHardwareMonitoring")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start hardware monitoring: ${e.message}"
                )
            }
        }
    }

    fun stopHardwareMonitoring() {
        viewModelScope.launch {
            try {
                CrashHandler.logInfo("Stopping hardware monitoring from UI")
                hardwareManager.stopHardwareMonitoring()
                
                _uiState.value = _uiState.value.copy(
                    lastAction = "Hardware monitoring stopped"
                )

            } catch (e: Exception) {
                CrashHandler.handleException(e, "SystemInfoViewModel.stopHardwareMonitoring")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to stop hardware monitoring: ${e.message}"
                )
            }
        }
    }

    fun forceGarbageCollection() {
        viewModelScope.launch {
            try {
                CrashHandler.logInfo("Forcing garbage collection from UI")
                memoryManager.forceGarbageCollection()
                
                _uiState.value = _uiState.value.copy(
                    lastAction = "Garbage collection forced"
                )

            } catch (e: Exception) {
                CrashHandler.handleException(e, "SystemInfoViewModel.forceGarbageCollection")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to force GC: ${e.message}"
                )
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                CrashHandler.logInfo("Clearing cache from UI")
                // In a real app, you'd clear various caches here
                memoryManager.optimizeMemoryUsage()
                
                _uiState.value = _uiState.value.copy(
                    lastAction = "Cache cleared"
                )

            } catch (e: Exception) {
                CrashHandler.handleException(e, "SystemInfoViewModel.clearCache")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to clear cache: ${e.message}"
                )
            }
        }
    }

    fun optimizePerformance() {
        viewModelScope.launch {
            try {
                CrashHandler.logInfo("Optimizing performance from UI")
                
                // Optimize memory
                memoryManager.optimizeMemoryUsage()
                
                // Start hardware monitoring for better performance
                hardwareManager.startHardwareMonitoring()
                
                _uiState.value = _uiState.value.copy(
                    lastAction = "Performance optimized"
                )

            } catch (e: Exception) {
                CrashHandler.handleException(e, "SystemInfoViewModel.optimizePerformance")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to optimize performance: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            hardwareManager.stopHardwareMonitoring()
            CrashHandler.logInfo("SystemInfoViewModel cleared")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "SystemInfoViewModel.onCleared")
        }
    }
}

/**
 * UI state for system info screen
 */
data class SystemInfoUiState(
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val systemDetails: SystemDetails = SystemDetails(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val lastAction: String? = null
)

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    val fps: Int = 60,
    val frameTimeMs: Float = 16.7f,
    val cpuUsage: Int = 0
)

/**
 * System details data class
 */
data class SystemDetails(
    val items: Map<String, String> = emptyMap()
)
