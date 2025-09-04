package com.hurtec.obd2.diagnostics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.obd.HurtecCommService
import com.hurtec.obd2.diagnostics.obd.ObdDataPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service for continuous OBD-II communication
 * Integrates with the original AndrOBD backend while providing modern API
 */
class ObdService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hurtec_obd_service"
        private const val CHANNEL_NAME = "Hurtec OBD-II Service"
    }

    private val binder = ObdBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Connection state management
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // OBD data streams
    private val _obdData = MutableStateFlow<Map<String, ObdDataPoint>>(emptyMap())
    val obdData: StateFlow<Map<String, ObdDataPoint>> = _obdData.asStateFlow()

    // Real OBD communication service
    private lateinit var hurtecCommService: HurtecCommService

    override fun onCreate() {
        super.onCreate()

        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())

            // Initialize real OBD communication service
            hurtecCommService = HurtecCommService(this)

            // Observe connection state changes
            serviceScope.launch {
                try {
                    hurtecCommService.connectionState.collect { state ->
                        _connectionState.value = when (state) {
                            com.hurtec.obd2.diagnostics.obd.ConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
                            com.hurtec.obd2.diagnostics.obd.ConnectionState.CONNECTING -> ConnectionState.CONNECTING
                            com.hurtec.obd2.diagnostics.obd.ConnectionState.CONNECTED -> ConnectionState.CONNECTED
                            com.hurtec.obd2.diagnostics.obd.ConnectionState.DISCONNECTING -> ConnectionState.DISCONNECTING
                            com.hurtec.obd2.diagnostics.obd.ConnectionState.ERROR -> ConnectionState.ERROR
                        }
                    }
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.ERROR
                }
            }

            // Observe OBD data changes
            serviceScope.launch {
                try {
                    hurtecCommService.obdData.collect { data ->
                        _obdData.value = data
                    }
                } catch (e: Exception) {
                    // Ignore data collection errors
                }
            }
        } catch (e: Exception) {
            // Service initialization failed - continue with basic functionality
            _connectionState.value = ConnectionState.ERROR
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Keep service running
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        disconnect()
    }

    /**
     * Connect to Bluetooth OBD-II adapter
     */
    fun connectBluetooth(device: BluetoothDevice, secure: Boolean = true) {
        hurtecCommService.connectBluetooth(device, secure)
    }

    /**
     * Connect to OBD-II adapter (legacy method)
     */
    fun connect(deviceAddress: String, connectionType: ConnectionType) {
        // This method is kept for compatibility but delegates to specific connection methods
        when (connectionType) {
            ConnectionType.BLUETOOTH -> {
                // For Bluetooth, we need the actual BluetoothDevice object
                // This is a simplified implementation - in practice, you'd resolve the device
                // from the address using BluetoothAdapter
            }
            ConnectionType.USB -> {
                // TODO: Implement USB connection
            }
            ConnectionType.WIFI -> {
                // TODO: Implement WiFi connection
            }
        }
    }

    /**
     * Disconnect from OBD-II adapter
     */
    fun disconnect() {
        hurtecCommService.disconnect()
    }

    /**
     * Start continuous data polling from OBD-II
     */
    private fun startDataPolling() {
        serviceScope.launch {
            while (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    // TODO: Poll data using original AndrOBD protocols
                    // val rpm = elmProtocol.getEngineRPM()
                    // val speed = elmProtocol.getVehicleSpeed()
                    // etc.
                    
                    // Simulate data for now
                    val simulatedData = mapOf(
                        "ENGINE_RPM" to ObdDataPoint("Engine RPM", (1000..6000).random().toFloat(), "RPM"),
                        "VEHICLE_SPEED" to ObdDataPoint("Vehicle Speed", (0..120).random().toFloat(), "km/h"),
                        "ENGINE_TEMP" to ObdDataPoint("Engine Temperature", (70..95).random().toFloat(), "°C"),
                        "FUEL_LEVEL" to ObdDataPoint("Fuel Level", (20..100).random().toFloat(), "%")
                    )
                    
                    _obdData.value = simulatedData
                    
                    delay(1000) // Poll every second
                    
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.ERROR
                    break
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hurtec OBD-II background service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hurtec OBD-II")
            .setContentText("Connected to vehicle diagnostics")
            .setSmallIcon(R.drawable.ic_diagnostics_24)
            .setColor(getColor(R.color.hurtec_primary))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    inner class ObdBinder : Binder() {
        fun getService(): ObdService = this@ObdService
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
 * Connection type enum
 */
enum class ConnectionType {
    BLUETOOTH,
    USB,
    WIFI
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
