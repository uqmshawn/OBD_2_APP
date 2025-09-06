package com.hurtec.obd2.diagnostics.obd.elm327

import kotlinx.coroutines.flow.Flow

/**
 * Interface for OBD protocol handlers
 */
interface ProtocolHandler {
    
    /**
     * Initialize the protocol handler
     */
    suspend fun initialize(): Result<InitializationResult>
    
    /**
     * Send an OBD command
     */
    suspend fun sendObdCommand(command: String): Result<ObdResponse>
    
    /**
     * Get supported PIDs for a mode
     */
    suspend fun getSupportedPids(mode: Int): Result<List<String>>
    
    /**
     * Get vehicle information
     */
    suspend fun getVehicleInfo(): Result<VehicleInfo>
    
    /**
     * Read diagnostic trouble codes
     */
    suspend fun readDtcs(): Result<List<DtcInfo>>
    
    /**
     * Clear diagnostic trouble codes
     */
    suspend fun clearDtcs(): Result<Unit>
    
    /**
     * Get continuous data stream for specified PIDs
     */
    fun getDataStream(pids: List<String>): Flow<ObdResponse>
    
    /**
     * Get current protocol information
     */
    fun getProtocolInfo(): ProtocolInfo?
}

/**
 * Initialization result
 */
data class InitializationResult(
    val success: Boolean,
    val elmVersion: String?,
    val protocol: String?,
    val supportedProtocols: List<String> = emptyList(),
    val error: String? = null
)

/**
 * OBD response
 */
data class ObdResponse(
    val command: String,
    val rawResponse: String,
    val data: List<String> = emptyList(),
    val isError: Boolean = false,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getByteData(): List<Int> {
        return data.mapNotNull { 
            try {
                it.toInt(16)
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}

/**
 * Vehicle information
 */
data class VehicleInfo(
    val vin: String? = null,
    val calibrationId: String? = null,
    val ecuName: String? = null,
    val supportedModes: List<Int> = emptyList()
)

/**
 * Diagnostic trouble code information
 */
data class DtcInfo(
    val code: String,
    val description: String? = null,
    val status: DtcStatus = DtcStatus.STORED,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * DTC status
 */
enum class DtcStatus {
    STORED,
    PENDING,
    PERMANENT
}

/**
 * Protocol information
 */
data class ProtocolInfo(
    val name: String,
    val description: String,
    val isAutomatic: Boolean = false
)
