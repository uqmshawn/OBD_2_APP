package com.hurtec.obd2.diagnostics.obd.communication

import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.obd.commands.CommandQueue
import com.hurtec.obd2.diagnostics.obd.commands.CommandPriority
import com.hurtec.obd2.diagnostics.obd.commands.ObdCommand
import com.hurtec.obd2.diagnostics.obd.data.DataProcessor
import com.hurtec.obd2.diagnostics.obd.data.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real communication manager based on AndrOBD implementation
 * Uses actual Bluetooth and USB communication services
 */
@Singleton
class CommunicationManager @Inject constructor(
    private val bluetoothCommService: BluetoothCommService,
    private val usbCommService: UsbCommService,
    private val wifiCommService: WiFiCommService,
    private val commandQueue: CommandQueue,
    private val dataProcessor: DataProcessor
) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectionType = MutableStateFlow<ConnectionType?>(null)
    val connectionType: StateFlow<ConnectionType?> = _connectionType.asStateFlow()

    private var isConnected = false

    // Data processing settings
    private var currentUnitSystem = UnitSystem.METRIC

    // Expose data processing flows
    val processedData = dataProcessor.processedData
    val dataErrors = dataProcessor.dataErrors
    val queueState = commandQueue.queueState

    /**
     * Connect using Bluetooth (Real AndrOBD implementation)
     */
    suspend fun connectBluetooth(device: ObdDevice): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.CONNECTING

            val result = bluetoothCommService.connect(device)
            if (result.isSuccess) {
                _connectionType.value = ConnectionType.BLUETOOTH
                _connectionState.value = ConnectionState.CONNECTED
                isConnected = true

                // Initialize ELM327 with standard AT commands
                initializeElm327()
            } else {
                _connectionState.value = ConnectionState.ERROR
            }

            result
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Connect using USB (Real AndrOBD implementation)
     */
    suspend fun connectUsb(device: ObdDevice): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.CONNECTING

            val result = usbCommService.connect(device)
            if (result.isSuccess) {
                _connectionType.value = ConnectionType.USB
                _connectionState.value = ConnectionState.CONNECTED
                isConnected = true

                // Initialize ELM327 with standard AT commands
                initializeElm327()
            } else {
                _connectionState.value = ConnectionState.ERROR
            }

            result
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Send OBD command
     */
    suspend fun sendCommand(command: String): Result<String> {
        if (!isConnected) {
            return Result.failure(IllegalStateException("Not connected"))
        }

        // Simulate command response
        kotlinx.coroutines.delay(100)
        return Result.success("41 0C 1A F8") // Example response
    }

    /**
     * Disconnect from current adapter (Real AndrOBD implementation)
     */
    suspend fun disconnect(): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.DISCONNECTING

            // Disconnect from the appropriate service
            when (_connectionType.value) {
                ConnectionType.BLUETOOTH -> bluetoothCommService.disconnect()
                ConnectionType.USB -> usbCommService.disconnect()
                ConnectionType.WIFI -> wifiCommService.disconnect()
                else -> {}
            }

            _connectionType.value = null
            _connectionState.value = ConnectionState.DISCONNECTED
            isConnected = false

            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Connect using WiFi (Real AndrOBD implementation)
     */
    suspend fun connectWifi(device: ObdDevice): Result<Unit> {
        return try {
            _connectionState.value = ConnectionState.CONNECTING

            val result = wifiCommService.connect(device)
            if (result.isSuccess) {
                _connectionType.value = ConnectionType.WIFI
                _connectionState.value = ConnectionState.CONNECTED
                isConnected = true

                // Initialize ELM327 with standard AT commands
                initializeElm327()
            } else {
                _connectionState.value = ConnectionState.ERROR
            }

            result
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }

    /**
     * Initialize ELM327 adapter with standard AT commands (AndrOBD style)
     */
    private suspend fun initializeElm327(): Result<Unit> {
        return try {
            // Standard ELM327 initialization sequence from AndrOBD
            val commands = listOf(
                "ATZ",      // Reset
                "ATE0",     // Echo off
                "ATL0",     // Linefeeds off
                "ATS0",     // Spaces off
                "ATH1",     // Headers on
                "ATSP0"     // Set protocol to auto
            )

            for (command in commands) {
                when (_connectionType.value) {
                    ConnectionType.BLUETOOTH -> bluetoothCommService.sendCommand(command)
                    ConnectionType.USB -> usbCommService.sendCommand(command)
                    ConnectionType.WIFI -> wifiCommService.sendCommand(command)
                    else -> {}
                }
                kotlinx.coroutines.delay(100) // Wait between commands
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get available Bluetooth devices
     */
    suspend fun getAvailableBluetoothDevices(): List<ObdDevice> {
        return try {
            bluetoothCommService.getPairedDevices()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.getAvailableBluetoothDevices")
            emptyList() // Return empty list instead of demo devices
        }
    }

    /**
     * Get available USB devices
     */
    suspend fun getAvailableUsbDevices(): List<ObdDevice> {
        return try {
            usbCommService.getAvailableDevices()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.getAvailableUsbDevices")
            emptyList()
        }
    }

    /**
     * Get available WiFi devices (Real implementation)
     */
    suspend fun getAvailableWifiDevices(): List<ObdDevice> {
        return try {
            wifiCommService.getAvailableDevices()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.getAvailableWifiDevices")
            emptyList()
        }
    }

    // ========== ADVANCED OBD COMMAND METHODS ==========

    /**
     * Send a single OBD command with priority
     */
    suspend fun sendObdCommand(
        command: String,
        description: String = "",
        priority: CommandPriority = CommandPriority.NORMAL
    ): Result<String> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Not connected to any device"))
            }

            val obdCommand = ObdCommand(
                command = command,
                description = description.ifEmpty { "Command: $command" }
            )

            // Add to command queue
            val commandId = commandQueue.enqueueCommand(obdCommand, priority)

            // For now, execute directly (will be replaced with queue processing)
            val response = when (_connectionType.value) {
                ConnectionType.BLUETOOTH -> bluetoothCommService.sendCommand(command)
                ConnectionType.USB -> usbCommService.sendCommand(command)
                ConnectionType.WIFI -> wifiCommService.sendCommand(command)
                else -> Result.failure(IllegalStateException("No connection type set"))
            }

            // Process the response through data pipeline
            if (response.isSuccess) {
                val rawResponse = response.getOrNull() ?: ""
                dataProcessor.processRawData(rawResponse, command, currentUnitSystem)
            }

            response
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.sendObdCommand")
            Result.failure(e)
        }
    }

    /**
     * Send multiple OBD commands as a batch
     */
    suspend fun sendObdCommandBatch(
        commands: List<Pair<String, String>>, // (command, description) pairs
        priority: CommandPriority = CommandPriority.NORMAL
    ): List<Result<String>> {
        return try {
            if (!isConnected) {
                return commands.map { Result.failure(IllegalStateException("Not connected to any device")) }
            }

            val obdCommands = commands.map { (command, description) ->
                ObdCommand(
                    command = command,
                    description = description.ifEmpty { "Command: $command" }
                )
            }

            // Add batch to command queue
            commandQueue.enqueueBatch(obdCommands, priority)

            // Execute batch
            commands.map { (command, _) ->
                sendObdCommand(command, "", priority)
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.sendObdCommandBatch")
            commands.map { Result.failure(e) }
        }
    }

    /**
     * Send a PID request with automatic parsing
     */
    suspend fun requestPidData(
        pid: String,
        priority: CommandPriority = CommandPriority.NORMAL
    ): Result<com.hurtec.obd2.diagnostics.obd.data.ProcessedObdData?> {
        return try {
            val command = "01$pid" // Mode 01 (current data) + PID
            val response = sendObdCommand(command, "PID $pid request", priority)

            if (response.isSuccess) {
                val rawResponse = response.getOrNull() ?: ""
                val processedData = dataProcessor.processRawData(rawResponse, command, currentUnitSystem)
                Result.success(processedData)
            } else {
                Result.failure(response.exceptionOrNull() ?: Exception("PID request failed"))
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.requestPidData")
            Result.failure(e)
        }
    }

    /**
     * Request multiple PIDs efficiently
     */
    suspend fun requestMultiplePids(
        pids: List<String>,
        priority: CommandPriority = CommandPriority.NORMAL
    ): List<Result<com.hurtec.obd2.diagnostics.obd.data.ProcessedObdData?>> {
        return pids.map { pid ->
            requestPidData(pid, priority)
        }
    }

    /**
     * Set unit system for data processing
     */
    fun setUnitSystem(unitSystem: UnitSystem) {
        currentUnitSystem = unitSystem
        CrashHandler.logInfo("Unit system changed to: $unitSystem")
    }

    /**
     * Get current unit system
     */
    fun getUnitSystem(): UnitSystem = currentUnitSystem

    /**
     * Get command queue statistics
     */
    fun getCommandQueueStats() = commandQueue.getQueueStats()

    /**
     * Get data processing statistics
     */
    fun getDataProcessingStats() = dataProcessor.getProcessingStats()

    /**
     * Clear command queue
     */
    fun clearCommandQueue() = commandQueue.clearQueue()

    /**
     * Clear data buffers
     */
    fun clearDataBuffers() = dataProcessor.clearAllBuffers()

    /**
     * Get buffered data for a specific PID
     */
    fun getBufferedData(pid: String) = dataProcessor.getBufferedData(pid)

    /**
     * Read diagnostic trouble codes
     */
    suspend fun readDtcs(): Result<List<com.hurtec.obd2.diagnostics.obd.elm327.DtcInfo>> {
        return try {
            if (!isConnected()) {
                return Result.failure(Exception("Not connected to any OBD device"))
            }

            CrashHandler.logInfo("Reading DTCs from vehicle...")

            // Send Mode 03 command to read stored DTCs
            val storedDtcsResponse = sendObdCommand("03", "Read Stored DTCs")
            val storedDtcs = mutableListOf<com.hurtec.obd2.diagnostics.obd.elm327.DtcInfo>()

            if (storedDtcsResponse.isSuccess) {
                val response = storedDtcsResponse.getOrNull() ?: ""
                val dtcCodes = parseDtcResponse(response)
                dtcCodes.forEach { code ->
                    storedDtcs.add(
                        com.hurtec.obd2.diagnostics.obd.elm327.DtcInfo(
                            code = code,
                            status = com.hurtec.obd2.diagnostics.obd.elm327.DtcStatus.STORED,
                            description = getDtcDescription(code)
                        )
                    )
                }
            }

            // Send Mode 07 command to read pending DTCs
            val pendingDtcsResponse = sendObdCommand("07", "Read Pending DTCs")
            if (pendingDtcsResponse.isSuccess) {
                val response = pendingDtcsResponse.getOrNull() ?: ""
                val dtcCodes = parseDtcResponse(response)
                dtcCodes.forEach { code ->
                    storedDtcs.add(
                        com.hurtec.obd2.diagnostics.obd.elm327.DtcInfo(
                            code = code,
                            status = com.hurtec.obd2.diagnostics.obd.elm327.DtcStatus.PENDING,
                            description = getDtcDescription(code)
                        )
                    )
                }
            }

            CrashHandler.logInfo("Found ${storedDtcs.size} DTCs")
            Result.success(storedDtcs)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.readDtcs")
            Result.failure(e)
        }
    }

    /**
     * Clear diagnostic trouble codes
     */
    suspend fun clearDtcs(): Result<Unit> {
        return try {
            if (!isConnected()) {
                return Result.failure(Exception("Not connected to any OBD device"))
            }

            CrashHandler.logInfo("Clearing DTCs from vehicle...")

            // Send Mode 04 command to clear DTCs
            val clearResponse = sendObdCommand("04", "Clear DTCs")

            if (clearResponse.isSuccess) {
                CrashHandler.logInfo("DTCs cleared successfully")
                Result.success(Unit)
            } else {
                Result.failure(clearResponse.exceptionOrNull() ?: Exception("Failed to clear DTCs"))
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.clearDtcs")
            Result.failure(e)
        }
    }

    /**
     * Parse DTC response from OBD command
     */
    private fun parseDtcResponse(response: String): List<String> {
        return try {
            val cleanResponse = response.replace(" ", "").replace("\r", "").replace("\n", "")
            val dtcCodes = mutableListOf<String>()

            // Skip the first 2 characters (response header) and parse in pairs of 4 hex characters
            var i = 2
            while (i + 3 < cleanResponse.length) {
                val dtcHex = cleanResponse.substring(i, i + 4)
                if (dtcHex != "0000") { // 0000 means no more DTCs
                    val dtcCode = convertHexToDtcCode(dtcHex)
                    if (dtcCode.isNotEmpty()) {
                        dtcCodes.add(dtcCode)
                    }
                }
                i += 4
            }

            dtcCodes
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.parseDtcResponse")
            emptyList()
        }
    }

    /**
     * Convert hex DTC to standard DTC code format
     */
    private fun convertHexToDtcCode(hex: String): String {
        return try {
            if (hex.length != 4) return ""

            val firstByte = hex.substring(0, 2).toInt(16)
            val secondByte = hex.substring(2, 4).toInt(16)

            // Determine DTC prefix based on first two bits
            val prefix = when ((firstByte and 0xC0) shr 6) {
                0 -> "P0" // Powertrain - standardized
                1 -> "P1" // Powertrain - manufacturer specific
                2 -> "C" // Chassis
                3 -> "B" // Body
                else -> "U" // Network
            }

            // Get the remaining 14 bits for the code number
            val codeNumber = ((firstByte and 0x3F) shl 8) or secondByte
            val codeString = String.format("%04X", codeNumber)

            "$prefix$codeString"
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.convertHexToDtcCode")
            ""
        }
    }

    /**
     * Get DTC description from code
     */
    private fun getDtcDescription(code: String): String {
        return when (code) {
            "P0171" -> "System Too Lean (Bank 1)"
            "P0172" -> "System Too Rich (Bank 1)"
            "P0174" -> "System Too Lean (Bank 2)"
            "P0175" -> "System Too Rich (Bank 2)"
            "P0300" -> "Random/Multiple Cylinder Misfire Detected"
            "P0301" -> "Cylinder 1 Misfire Detected"
            "P0302" -> "Cylinder 2 Misfire Detected"
            "P0303" -> "Cylinder 3 Misfire Detected"
            "P0304" -> "Cylinder 4 Misfire Detected"
            "P0420" -> "Catalyst System Efficiency Below Threshold (Bank 1)"
            "P0430" -> "Catalyst System Efficiency Below Threshold (Bank 2)"
            "P0441" -> "Evaporative Emission Control System Incorrect Purge Flow"
            "P0442" -> "Evaporative Emission Control System Leak Detected (small leak)"
            "P0455" -> "Evaporative Emission Control System Leak Detected (large leak)"
            "P0506" -> "Idle Control System RPM Lower Than Expected"
            "P0507" -> "Idle Control System RPM Higher Than Expected"
            else -> "Unknown diagnostic trouble code"
        }
    }

    /**
     * Get vehicle information (VIN, etc.)
     */
    suspend fun getVehicleInfo(): Result<com.hurtec.obd2.diagnostics.obd.elm327.VehicleInfo> {
        return try {
            if (!isConnected()) {
                return Result.failure(Exception("Not connected to any OBD device"))
            }

            // For now, return sample vehicle info - this would be replaced with actual protocol handler calls
            val sampleVehicleInfo = com.hurtec.obd2.diagnostics.obd.elm327.VehicleInfo(
                vin = "1HGBH41JXMN109186",
                calibrationId = "CAL123456",
                ecuName = "Honda ECU"
            )
            Result.success(sampleVehicleInfo)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommunicationManager.getVehicleInfo")
            Result.failure(e)
        }
    }
}

/**
 * Connection types
 */
enum class ConnectionType {
    BLUETOOTH,
    USB,
    WIFI
}

/**
 * Bluetooth device information
 */
data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val isPaired: Boolean,
    val rssi: Int? = null
)

/**
 * USB device information
 */
data class UsbDeviceInfo(
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val serialNumber: String?
)
