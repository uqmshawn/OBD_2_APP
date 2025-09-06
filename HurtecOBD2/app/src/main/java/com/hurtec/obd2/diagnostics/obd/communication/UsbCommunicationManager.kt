package com.hurtec.obd2.diagnostics.obd.communication

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modern USB communication manager with coroutines
 * Replaces the old UsbCommService with modern async patterns
 */
@Singleton
class UsbCommunicationManager @Inject constructor(
    private val context: Context
) : ObdCommunicationInterface {

    companion object {
        private const val BAUD_RATE = 38400
        private const val DATA_BITS = 8
        private const val STOP_BITS = UsbSerialPort.STOPBITS_1
        private const val PARITY = UsbSerialPort.PARITY_NONE
        private const val READ_TIMEOUT = 5000
        private const val WRITE_TIMEOUT = 5000
    }

    private var usbManager: UsbManager? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var connectedDevice: UsbDevice? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _deviceList = MutableStateFlow<List<ObdDevice>>(emptyList())
    val deviceList: StateFlow<List<ObdDevice>> = _deviceList.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    override fun isConnected(): Boolean =
        usbSerialPort?.isOpen == true && _connectionState.value == ConnectionState.CONNECTED

    override suspend fun scanForDevices(): Result<List<ObdDevice>> {
        return withContext(Dispatchers.IO) {
            try {
                val manager = usbManager ?: return@withContext Result.failure(
                    Exception("USB Manager not available")
                )

                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
                val devices = availableDrivers.map { driver ->
                    val device = driver.device
                    ObdDevice(
                        id = "${device.vendorId}:${device.productId}",
                        name = device.productName ?: "USB OBD-II Adapter",
                        address = "${device.vendorId}:${device.productId}",
                        type = DeviceType.USB,
                        isPaired = true,
                        isConnected = device == connectedDevice
                    )
                }

                _deviceList.value = devices
                Result.success(devices)

            } catch (e: Exception) {
                Log.e("UsbComm", "Error scanning for USB devices", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun connect(device: ObdDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val manager = usbManager ?: return@withContext Result.failure(
                Exception("USB Manager not available")
            )

            _connectionState.value = ConnectionState.CONNECTING

            // Find the specific driver for this device
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            val driver = availableDrivers.find {
                "${it.device.vendorId}:${it.device.productId}" == device.address
            } ?: return@withContext Result.failure(Exception("USB device not found"))

            val connection = manager.openDevice(driver.device)
                ?: return@withContext Result.failure(Exception("Cannot open USB device"))

            val port = driver.ports.firstOrNull()
                ?: return@withContext Result.failure(Exception("No USB ports available"))

            port.open(connection)
            port.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY)

            usbSerialPort = port
            connectedDevice = driver.device

            _connectionState.value = ConnectionState.CONNECTED
            Log.i("UsbComm", "Connected to ${device.name}")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("UsbComm", "USB connection failed", e)
            _connectionState.value = ConnectionState.ERROR
            cleanupResources()
            Result.failure(e)
        }
    }

    override suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val port = usbSerialPort
                ?: return@withContext Result.failure(IllegalStateException("Not connected"))

            // Send command with carriage return
            val commandBytes = "$command\r".toByteArray()
            port.write(commandBytes, WRITE_TIMEOUT)

            // Read response
            val response = readResponse(port)
            Result.success(response)

        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.DISCONNECTING
            cleanupResources()
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.i("UsbComm", "Disconnected")
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    override fun getConnectedDevice(): ObdDevice? {
        return connectedDevice?.let { device ->
            ObdDevice(
                id = "${device.vendorId}:${device.productId}",
                name = device.productName ?: "USB OBD-II Adapter",
                address = "${device.vendorId}:${device.productId}",
                type = DeviceType.USB,
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



    private fun readResponse(port: UsbSerialPort): String {
        val buffer = ByteArray(1024)
        val response = StringBuilder()
        var totalBytesRead = 0
        
        do {
            val bytesRead = port.read(buffer, READ_TIMEOUT)
            if (bytesRead > 0) {
                val data = String(buffer, 0, bytesRead)
                response.append(data)
                totalBytesRead += bytesRead
                
                // Check for end of response (usually '>' character)
                if (data.contains('>')) {
                    break
                }
            }
        } while (bytesRead > 0 && totalBytesRead < buffer.size)
        
        return response.toString().trim()
    }

    private fun cleanupResources() {
        try {
            usbSerialPort?.close()
        } catch (e: IOException) {
            // Ignore
        }

        usbSerialPort = null
        connectedDevice = null
    }
}
