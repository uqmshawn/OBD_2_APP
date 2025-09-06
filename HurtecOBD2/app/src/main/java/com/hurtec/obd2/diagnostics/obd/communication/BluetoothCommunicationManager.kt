package com.hurtec.obd2.diagnostics.obd.communication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modern Bluetooth communication manager with coroutines
 * Replaces the old BtCommService with modern async patterns
 */
@Singleton
class BluetoothCommunicationManager @Inject constructor(
    private val context: Context
) : ObdCommunicationInterface {

    companion object {
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val COMMAND_TIMEOUT = 5000L
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var connectedDevice: BluetoothDevice? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _deviceList = MutableStateFlow<List<ObdDevice>>(emptyList())
    val deviceList: StateFlow<List<ObdDevice>> = _deviceList.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    override suspend fun scanForDevices(): Result<List<ObdDevice>> {
        return withContext(Dispatchers.IO) {
            try {
                if (bluetoothAdapter == null) {
                    return@withContext Result.failure(Exception("Bluetooth not supported"))
                }

                if (!bluetoothAdapter!!.isEnabled) {
                    return@withContext Result.failure(Exception("Bluetooth not enabled"))
                }

                val devices = mutableListOf<ObdDevice>()

                // Get paired devices
                val pairedDevices = bluetoothAdapter!!.bondedDevices
                for (device in pairedDevices) {
                    if (isObdDevice(device)) {
                        devices.add(
                            ObdDevice(
                                id = device.address,
                                name = device.name ?: "Unknown Device",
                                address = device.address,
                                type = DeviceType.BLUETOOTH,
                                isPaired = true,
                                isConnected = device.address == connectedDevice?.address
                            )
                        )
                    }
                }

                _deviceList.value = devices
                Result.success(devices)

            } catch (e: Exception) {
                Log.e("BluetoothComm", "Error scanning for devices", e)
                Result.failure(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun isObdDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.uppercase() ?: ""
        return name.contains("OBD") ||
               name.contains("ELM") ||
               name.contains("OBDII") ||
               name.contains("VGATE") ||
               name.contains("KONNWEI") ||
               name.contains("VEEPEAK") ||
               name.contains("BAFX")
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: ObdDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val adapter = bluetoothAdapter
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth not available"))

            if (!adapter.isEnabled) {
                return@withContext Result.failure(IllegalStateException("Bluetooth not enabled"))
            }

            _connectionState.value = ConnectionState.CONNECTING

            val bluetoothDevice = adapter.getRemoteDevice(device.address)
            val socket = bluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID)

            // Cancel discovery to improve connection speed
            adapter.cancelDiscovery()

            socket.connect()

            bluetoothSocket = socket
            inputStream = socket.inputStream
            outputStream = socket.outputStream
            connectedDevice = bluetoothDevice

            _connectionState.value = ConnectionState.CONNECTED
            Log.i("BluetoothComm", "Connected to ${device.name}")

            Result.success(Unit)
        } catch (e: IOException) {
            _connectionState.value = ConnectionState.ERROR
            cleanupResources()
            Result.failure(e)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            cleanupResources()
            Result.failure(e)
        }
    }

    override suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConnected()) {
                return@withContext Result.failure(Exception("Not connected"))
            }

            val output = outputStream
                ?: return@withContext Result.failure(Exception("Output stream not available"))

            val input = inputStream
                ?: return@withContext Result.failure(Exception("Input stream not available"))

            // Send command with carriage return
            val commandWithCR = "$command\r"
            output.write(commandWithCR.toByteArray())
            output.flush()

            Log.d("BluetoothComm", "Sent: $command")

            // Read response with timeout
            val response = withTimeout(COMMAND_TIMEOUT) {
                readResponse(input)
            }

            Log.d("BluetoothComm", "Received: $response")
            Result.success(response)

        } catch (e: Exception) {
            Log.e("BluetoothComm", "Command failed: $command", e)
            Result.failure(e)
        }
    }



    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.DISCONNECTING
            cleanupResources()
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.i("BluetoothComm", "Disconnected")
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean =
        bluetoothSocket?.isConnected == true && _connectionState.value == ConnectionState.CONNECTED

    override fun getConnectedDevice(): ObdDevice? {
        return connectedDevice?.let { device ->
            ObdDevice(
                id = device.address,
                name = device.name ?: "Unknown Device",
                address = device.address,
                type = DeviceType.BLUETOOTH,
                isPaired = true,
                isConnected = true
            )
        }
    }

    override fun cleanup() {
        scope.cancel()
        runBlocking {
            disconnect()
        }
        cleanupResources()
    }



    @SuppressLint("MissingPermission")
    private fun getDeviceName(device: BluetoothDevice): String? {
        return try {
            device.name
        } catch (e: SecurityException) {
            null
        }
    }

    private suspend fun readResponse(input: InputStream): String {
        return withContext(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            val response = StringBuilder()
            var bytesRead: Int

            while (true) {
                bytesRead = input.read(buffer)
                if (bytesRead > 0) {
                    val data = String(buffer, 0, bytesRead)
                    response.append(data)

                    // Check for end of response
                    if (data.contains(">") || data.contains("ERROR") || data.contains("NO DATA")) {
                        break
                    }
                }

                // Small delay to prevent busy waiting
                delay(10)
            }

            response.toString().trim()
        }
    }

    private fun cleanupResources() {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            // Ignore
        }

        try {
            outputStream?.close()
        } catch (e: IOException) {
            // Ignore
        }

        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            // Ignore
        }

        inputStream = null
        outputStream = null
        bluetoothSocket = null
        connectedDevice = null
    }
}
