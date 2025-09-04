package com.hurtec.obd2.diagnostics.obd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.logging.Logger

/**
 * Modern Kotlin implementation of OBD-II communication service
 * Integrates with original AndrOBD backend while providing modern coroutine-based API
 */
class HurtecCommService(
    private val context: Context
) {
    companion object {
        private val TAG = "HurtecCommService"
        private val logger = Logger.getLogger(TAG)
        
        // Standard SerialPortService ID
        private val MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    // Connection state management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // OBD data streams
    private val _obdData = MutableStateFlow<Map<String, ObdDataPoint>>(emptyMap())
    val obdData: StateFlow<Map<String, ObdDataPoint>> = _obdData.asStateFlow()

    // Bluetooth components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var currentSocket: BluetoothSocket? = null

    // Coroutine scope for background operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // OBD Protocol handler (simplified version of ElmProt)
    private val obdProtocol = HurtecObdProtocol()

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    /**
     * Connect to Bluetooth OBD-II adapter
     */
    fun connectBluetooth(device: BluetoothDevice, secure: Boolean = true) {
        serviceScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                
                // Cancel any existing connection
                disconnect()
                
                // Start connection thread
                connectThread = ConnectThread(device, secure)
                connectThread?.start()
                
            } catch (e: Exception) {
                logger.severe("Connection failed: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    /**
     * Disconnect from OBD-II adapter
     */
    fun disconnect() {
        serviceScope.launch {
            _connectionState.value = ConnectionState.DISCONNECTING
            
            connectThread?.cancel()
            connectedThread?.cancel()
            
            try {
                currentSocket?.close()
            } catch (e: IOException) {
                logger.warning("Error closing socket: ${e.message}")
            }
            
            connectThread = null
            connectedThread = null
            currentSocket = null
            
            _connectionState.value = ConnectionState.DISCONNECTED
            _obdData.value = emptyMap()
        }
    }

    /**
     * Send OBD command
     */
    fun sendObdCommand(command: String) {
        connectedThread?.write(command.toByteArray())
    }

    /**
     * Start continuous data polling
     */
    private fun startDataPolling() {
        serviceScope.launch {
            // Initialize ELM327
            initializeElm327()
            
            // Start polling common PIDs
            val commonPids = listOf(
                "010C", // Engine RPM
                "010D", // Vehicle Speed
                "0105", // Engine Coolant Temperature
                "012F"  // Fuel Tank Level
            )
            
            while (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    for (pid in commonPids) {
                        if (_connectionState.value != ConnectionState.CONNECTED) break
                        
                        sendObdCommand("$pid\r")
                        delay(250) // 250ms between commands
                    }
                    delay(1000) // 1 second cycle time
                } catch (e: Exception) {
                    logger.severe("Data polling error: ${e.message}")
                    _connectionState.value = ConnectionState.ERROR
                    break
                }
            }
        }
    }

    /**
     * Initialize ELM327 adapter
     */
    private suspend fun initializeElm327() {
        val initCommands = listOf(
            "ATZ",      // Reset
            "ATE0",     // Echo off
            "ATL0",     // Linefeeds off
            "ATS0",     // Spaces off
            "ATH1",     // Headers on
            "ATSP0"     // Auto protocol
        )
        
        for (command in initCommands) {
            sendObdCommand("$command\r")
            delay(100)
        }
    }

    /**
     * Thread for connecting to Bluetooth device
     */
    private inner class ConnectThread(
        private val device: BluetoothDevice,
        private val secure: Boolean
    ) : Thread() {
        
        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            if (secure) {
                device.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
            } else {
                device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE)
            }
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()

            try {
                socket?.connect()
                
                // Connection successful
                currentSocket = socket
                
                // Start connected thread
                connectedThread = ConnectedThread(socket!!)
                connectedThread?.start()
                
                _connectionState.value = ConnectionState.CONNECTED
                
                // Start data polling
                startDataPolling()
                
            } catch (e: IOException) {
                logger.severe("Connection failed: ${e.message}")
                try {
                    socket?.close()
                } catch (e2: IOException) {
                    logger.warning("Error closing socket: ${e2.message}")
                }
                _connectionState.value = ConnectionState.ERROR
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                logger.warning("Error closing socket: ${e.message}")
            }
        }
    }

    /**
     * Thread for handling connected communication
     */
    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream = socket.inputStream
        private val outputStream: OutputStream = socket.outputStream
        private val buffer = ByteArray(1024)

        override fun run() {
            var numBytes: Int

            while (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    numBytes = inputStream.read(buffer)
                    val receivedData = String(buffer, 0, numBytes)
                    
                    // Process received OBD data
                    processObdResponse(receivedData)
                    
                } catch (e: IOException) {
                    logger.warning("Input stream disconnected: ${e.message}")
                    _connectionState.value = ConnectionState.ERROR
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                logger.severe("Error writing to output stream: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                logger.warning("Error closing connected socket: ${e.message}")
            }
        }
    }

    /**
     * Process OBD response and update data
     */
    private fun processObdResponse(response: String) {
        serviceScope.launch {
            try {
                val processedData = obdProtocol.parseResponse(response)
                if (processedData.isNotEmpty()) {
                    val currentData = _obdData.value.toMutableMap()
                    currentData.putAll(processedData)
                    _obdData.value = currentData
                }
            } catch (e: Exception) {
                logger.warning("Error processing OBD response: ${e.message}")
            }
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        serviceScope.cancel()
        disconnect()
    }
}

/**
 * Connection state enum
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * OBD data point
 */
data class ObdDataPoint(
    val name: String,
    val value: Float,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis()
)
