package com.hurtec.obd2.diagnostics.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.utils.PermissionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware manager to utilize all device capabilities for maximum performance
 */
@Singleton
class HardwareManager @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Hardware state flows
    private val _accelerometerData = MutableStateFlow(AccelerometerData())
    val accelerometerData: Flow<AccelerometerData> = _accelerometerData.asStateFlow()

    private val _gyroscopeData = MutableStateFlow(GyroscopeData())
    val gyroscopeData: Flow<GyroscopeData> = _gyroscopeData.asStateFlow()

    private val _locationData = MutableStateFlow<Location?>(null)
    val locationData: Flow<Location?> = _locationData.asStateFlow()

    private val _hardwareCapabilities = MutableStateFlow(HardwareCapabilities())
    val hardwareCapabilities: Flow<HardwareCapabilities> = _hardwareCapabilities.asStateFlow()

    // Sensors
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    private var barometer: Sensor? = null
    private var thermometer: Sensor? = null

    // Performance optimization
    private var wakeLock: PowerManager.WakeLock? = null
    private var isOptimized = false

    init {
        initializeHardware()
    }

    /**
     * Initialize all available hardware
     */
    private fun initializeHardware() {
        try {
            CrashHandler.logInfo("Initializing hardware capabilities")

            // Initialize sensors
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
            thermometer = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

            // Update capabilities
            _hardwareCapabilities.value = HardwareCapabilities(
                hasAccelerometer = accelerometer != null,
                hasGyroscope = gyroscope != null,
                hasMagnetometer = magnetometer != null,
                hasBarometer = barometer != null,
                hasThermometer = thermometer != null,
                hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER),
                hasVibrator = vibrator.hasVibrator(),
                cpuCores = Runtime.getRuntime().availableProcessors(),
                totalMemoryMB = getTotalMemoryMB(),
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = Build.VERSION.RELEASE
            )

            CrashHandler.logInfo("Hardware initialized: ${_hardwareCapabilities.value}")

        } catch (e: Exception) {
            CrashHandler.handleException(e, "HardwareManager.initializeHardware")
        }
    }

    /**
     * Start hardware monitoring for maximum performance
     */
    fun startHardwareMonitoring() {
        try {
            CrashHandler.logInfo("Starting hardware monitoring")

            // Register sensor listeners
            accelerometer?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }

            gyroscope?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            }

            magnetometer?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }

            barometer?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }

            // Start location monitoring if permissions available
            if (permissionManager.hasLocationPermissions()) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L, // 1 second
                        1.0f,  // 1 meter
                        this
                    )
                } catch (e: SecurityException) {
                    CrashHandler.handleException(e, "Location permission denied")
                }
            }

            // Optimize performance
            optimizePerformance()

        } catch (e: Exception) {
            CrashHandler.handleException(e, "HardwareManager.startHardwareMonitoring")
        }
    }

    /**
     * Stop hardware monitoring to save battery
     */
    fun stopHardwareMonitoring() {
        try {
            CrashHandler.logInfo("Stopping hardware monitoring")

            sensorManager.unregisterListener(this)
            locationManager.removeUpdates(this)

            // Release wake lock
            wakeLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                }
            }
            wakeLock = null
            isOptimized = false

        } catch (e: Exception) {
            CrashHandler.handleException(e, "HardwareManager.stopHardwareMonitoring")
        }
    }

    /**
     * Optimize device performance for OBD operations
     */
    private fun optimizePerformance() {
        try {
            if (isOptimized) return

            // Acquire partial wake lock to prevent CPU sleep during OBD communication
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "HurtecOBD2::OBDCommunication"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes max
            }

            // Set thread priorities for better performance
            Thread.currentThread().priority = Thread.MAX_PRIORITY

            isOptimized = true
            CrashHandler.logInfo("Performance optimization enabled")

        } catch (e: Exception) {
            CrashHandler.handleException(e, "HardwareManager.optimizePerformance")
        }
    }

    /**
     * Provide haptic feedback
     */
    fun provideHapticFeedback(type: HapticFeedbackType) {
        try {
            if (!vibrator.hasVibrator()) return

            when (type) {
                HapticFeedbackType.LIGHT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                }
                HapticFeedbackType.MEDIUM -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
                HapticFeedbackType.STRONG -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(200)
                    }
                }
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "HardwareManager.provideHapticFeedback")
        }
    }

    // Sensor event handling
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    _accelerometerData.value = AccelerometerData(
                        x = sensorEvent.values[0],
                        y = sensorEvent.values[1],
                        z = sensorEvent.values[2],
                        timestamp = sensorEvent.timestamp
                    )
                }
                Sensor.TYPE_GYROSCOPE -> {
                    _gyroscopeData.value = GyroscopeData(
                        x = sensorEvent.values[0],
                        y = sensorEvent.values[1],
                        z = sensorEvent.values[2],
                        timestamp = sensorEvent.timestamp
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    // Location event handling
    override fun onLocationChanged(location: Location) {
        _locationData.value = location
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    /**
     * Get total device memory in MB
     */
    private fun getTotalMemoryMB(): Long {
        return try {
            val memInfo = android.app.ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            CrashHandler.handleException(e, "HardwareManager.getTotalMemoryMB")
            0L
        }
    }
}

/**
 * Hardware capabilities data class
 */
data class HardwareCapabilities(
    val hasAccelerometer: Boolean = false,
    val hasGyroscope: Boolean = false,
    val hasMagnetometer: Boolean = false,
    val hasBarometer: Boolean = false,
    val hasThermometer: Boolean = false,
    val hasGPS: Boolean = false,
    val hasVibrator: Boolean = false,
    val cpuCores: Int = 1,
    val totalMemoryMB: Long = 0,
    val deviceModel: String = "Unknown",
    val androidVersion: String = "Unknown"
)

/**
 * Accelerometer data
 */
data class AccelerometerData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestamp: Long = 0L
)

/**
 * Gyroscope data
 */
data class GyroscopeData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestamp: Long = 0L
)

/**
 * Haptic feedback types
 */
enum class HapticFeedbackType {
    LIGHT,
    MEDIUM,
    STRONG
}
