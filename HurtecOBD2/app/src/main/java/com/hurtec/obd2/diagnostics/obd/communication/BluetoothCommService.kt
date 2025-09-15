package com.hurtec.obd2.diagnostics.obd.communication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.utils.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// Import AndrOBD's real communication components
import com.fr3ts0n.ecu.prot.obd.ElmProt
import com.fr3ts0n.ecu.prot.obd.ObdProt
import com.fr3ts0n.prot.StreamHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

/**
 * Real Bluetooth communication service based on AndrOBD implementation
 * Uses Bluetooth SPP (Serial Port Profile) for ELM327 communication
 */
@Singleton
class BluetoothCommService @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) {
    companion object {
        // Standard SPP UUID for ELM327 adapters
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "BluetoothCommService"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    // AndrOBD real communication components
    private val elmProtocol = ElmProt()
    private val streamHandler = StreamHandler()
    private var bufferedReader: BufferedReader? = null
    private var printWriter: PrintWriter? = null
    private var isElmInitialized = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedData = MutableStateFlow<String?>(null)
    val receivedData: Flow<String?> = _receivedData.asStateFlow()

    /**
     * Get list of paired Bluetooth devices
     */
    fun getPairedDevices(): List<ObdDevice> {
        return try {
            // Check if Bluetooth is available
            if (bluetoothAdapter == null) {
                CrashHandler.logWarning("Bluetooth adapter not available")
                return emptyList()
            }

            if (!bluetoothAdapter!!.isEnabled) {
                CrashHandler.logWarning("Bluetooth is not enabled")
                return emptyList()
            }

            if (!permissionManager.hasBluetoothPermissions()) {
                CrashHandler.logError("Missing Bluetooth permissions - cannot scan for devices")
                return emptyList()
            }

            CrashHandler.logInfo("Scanning for paired Bluetooth devices...")

            val pairedDevices = bluetoothAdapter!!.bondedDevices
            CrashHandler.logInfo("Found ${pairedDevices?.size ?: 0} paired devices")

            val realDevices = pairedDevices?.mapNotNull { device ->
                try {
                    val deviceName = device.name ?: "Unknown Device"
                    val deviceAddress = device.address

                    CrashHandler.logInfo("Found device: $deviceName ($deviceAddress)")

                    // Create OBD device entry for all paired devices
                    ObdDevice(
                        id = deviceAddress,
                        name = deviceName,
                        address = deviceAddress,
                        type = DeviceType.BLUETOOTH,
                        isConnected = false,
                        isPaired = true
                    )
                } catch (e: SecurityException) {
                    CrashHandler.logWarning("Security exception accessing device: ${e.message}")
                    null
                }
            } ?: emptyList()

            CrashHandler.logInfo("Successfully found ${realDevices.size} Bluetooth devices")
            realDevices
        } catch (e: SecurityException) {
            CrashHandler.handleException(e, "BluetoothCommService.getPairedDevices - No Bluetooth permission")
            emptyList()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "BluetoothCommService.getPairedDevices")
            emptyList()
        }
    }



    /**
     * Connect to a Bluetooth OBD device
     */
    suspend fun connect(device: ObdDevice): Result<Unit> = withContext(Dispatchers.IO) {
        if (!permissionManager.hasBluetoothPermissions()) {
            return@withContext Result.failure(SecurityException("Missing Bluetooth permissions"))
        }

        try {
            _connectionState.value = ConnectionState.CONNECTING
            CrashHandler.logInfo("Connecting to Bluetooth device: ${device.name}")

            // Get the Bluetooth device
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter not available"))

            // Cancel discovery to improve connection performance
            bluetoothAdapter.cancelDiscovery()

            // Create socket
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID)

            // Connect to the device
            bluetoothSocket?.connect()

            // Get input and output streams
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream

            // Initialize AndrOBD stream handlers
            bufferedReader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            printWriter = PrintWriter(outputStream, true, StandardCharsets.UTF_8)

            // Initialize ELM327 protocol using AndrOBD
            initializeElm327Protocol()

            isConnected = true
            _connectionState.value = ConnectionState.CONNECTED
            CrashHandler.logInfo("Successfully connected to ${device.name} with AndrOBD protocol")

            // Start listening for data
            startListening()

            Result.success(Unit)
        } catch (e: IOException) {
            CrashHandler.handleException(e, "BluetoothCommService.connect")
            disconnect()
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        } catch (e: SecurityException) {
            CrashHandler.handleException(e, "BluetoothCommService.connect - Security")
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    /**
     * Send OBD command to the connected device using AndrOBD's real implementation
     */
    suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConnected || printWriter == null || bufferedReader == null) {
            return@withContext Result.failure(IllegalStateException("Not connected to device"))
        }

        try {
            CrashHandler.logInfo("Sending real OBD command: $command")

            // Send command using AndrOBD's method with carriage return
            printWriter?.println(command)
            printWriter?.flush()

            // Read real response from ELM327 device until ">" prompt
            val response = readElm327Response()

            CrashHandler.logInfo("Received real response: $response")
            Result.success(response)
        } catch (e: IOException) {
            CrashHandler.handleException(e, "BluetoothCommService.sendCommand")
            Result.failure(e)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "BluetoothCommService.sendCommand - Unexpected error")
            Result.failure(e)
        }
    }

    /**
     * Read real ELM327 response until ">" prompt (AndrOBD method)
     */
    private fun readElm327Response(): String {
        val response = StringBuilder()
        var char: Int
        var timeoutCount = 0
        val maxTimeout = 50 // 5 seconds timeout

        try {
            while (timeoutCount < maxTimeout) {
                if (bufferedReader?.ready() == true) {
                    char = bufferedReader?.read() ?: -1
                    if (char == -1) break

                    val charValue = char.toChar()
                    response.append(charValue)

                    // ELM327 ends responses with ">" prompt
                    if (charValue == '>') {
                        break
                    }

                    timeoutCount = 0 // Reset timeout on data received
                } else {
                    Thread.sleep(100)
                    timeoutCount++
                }
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "readElm327Response")
        }

        // Clean up response (remove carriage returns, line feeds, and prompt)
        return response.toString()
            .replace("\r", "")
            .replace("\n", " ")
            .replace(">", "")
            .trim()
    }

    /**
     * Initialize ELM327 protocol using AndrOBD's proven sequence
     */
    private suspend fun initializeElm327Protocol() = withContext(Dispatchers.IO) {
        try {
            CrashHandler.logInfo("Initializing ELM327 using AndrOBD protocol sequence...")

            // AndrOBD's ELM327 initialization sequence
            val initCommands = listOf(
                "ATZ",      // Reset ELM327
                "ATE0",     // Echo off
                "ATL0",     // Line feeds off
                "ATS0",     // Spaces off
                "ATH1",     // Headers on
                "ATSP0",    // Set protocol to auto
                "0100"      // Test communication with supported PIDs
            )

            for (command in initCommands) {
                CrashHandler.logInfo("Sending init command: $command")
                printWriter?.println(command)
                printWriter?.flush()

                // Wait for response
                Thread.sleep(200)
                val response = readElm327Response()
                CrashHandler.logInfo("Init response for $command: $response")

                // Check for errors
                if (response.contains("ERROR") || response.contains("?")) {
                    CrashHandler.logError("ELM327 initialization error for command $command: $response")
                }
            }

            // Initialize AndrOBD protocol handler
            elmProtocol.service = ObdProt.OBD_SVC_NONE
            isElmInitialized = true

            CrashHandler.logInfo("ELM327 initialization completed successfully")

        } catch (e: Exception) {
            CrashHandler.handleException(e, "initializeElm327Protocol")
            isElmInitialized = false
        }
    }

    /**
     * Start listening for incoming data
     */
    private fun startListening() {
        Thread {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (isConnected && inputStream != null) {
                try {
                    bytes = inputStream?.read(buffer) ?: 0
                    if (bytes > 0) {
                        val receivedMessage = String(buffer, 0, bytes)
                        _receivedData.value = receivedMessage
                        CrashHandler.logInfo("Received data: $receivedMessage")
                    }
                } catch (e: IOException) {
                    CrashHandler.handleException(e, "BluetoothCommService.startListening")
                    break
                }
            }
        }.start()
    }

    /**
     * Disconnect from the device
     */
    fun disconnect() {
        try {
            isConnected = false
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            _connectionState.value = ConnectionState.DISCONNECTED
            CrashHandler.logInfo("Disconnected from Bluetooth device")
        } catch (e: IOException) {
            CrashHandler.handleException(e, "BluetoothCommService.disconnect")
        }
    }

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }


}
