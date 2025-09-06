package com.hurtec.obd2.diagnostics.obd.communication

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for OBD communication managers
 */
interface ObdCommunicationInterface {
    
    val connectionState: StateFlow<ConnectionState>
    
    fun isConnected(): Boolean
    
    suspend fun scanForDevices(): Result<List<ObdDevice>>
    
    suspend fun connect(device: ObdDevice): Result<Unit>
    
    suspend fun disconnect(): Result<Unit>
    
    suspend fun sendCommand(command: String): Result<String>
    
    fun getConnectedDevice(): ObdDevice?
    
    fun cleanup()
}

/**
 * Connection states
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Device types
 */
enum class DeviceType {
    BLUETOOTH,
    USB,
    WIFI
}

/**
 * OBD device information
 */
data class ObdDevice(
    val id: String,
    val name: String,
    val address: String,
    val type: DeviceType,
    val isPaired: Boolean = false,
    val isConnected: Boolean = false,
    val signalStrength: Int? = null,
    val lastSeen: Long = System.currentTimeMillis()
)
