package com.hurtec.obd2.diagnostics.obd.communication

import kotlinx.coroutines.flow.Flow

/**
 * Interface for OBD communication implementations
 */
interface ObdCommunicator {
    
    /**
     * Connect to the OBD adapter
     */
    suspend fun connect(identifier: String): Result<Unit>
    
    /**
     * Send a command to the OBD adapter
     */
    suspend fun sendCommand(command: String): Result<String>
    
    /**
     * Get continuous data stream from the adapter
     */
    fun getDataStream(): Flow<String>
    
    /**
     * Disconnect from the adapter
     */
    suspend fun disconnect()
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean
    
    /**
     * Get connection information
     */
    fun getConnectionInfo(): ConnectionInfo?
}

/**
 * Connection information
 */
data class ConnectionInfo(
    val type: ConnectionType,
    val identifier: String,
    val name: String?,
    val isConnected: Boolean,
    val signalStrength: Int? = null,
    val lastActivity: Long = System.currentTimeMillis()
)
