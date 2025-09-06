package com.hurtec.obd2.diagnostics.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Permission manager for OBD-II app permissions (AndrOBD-style)
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Check if all required Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Legacy permissions
            hasPermission(Manifest.permission.BLUETOOTH) &&
            hasPermission(Manifest.permission.BLUETOOTH_ADMIN) &&
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Check if USB permissions are available
     */
    fun hasUsbPermissions(): Boolean {
        // USB permissions are granted at runtime when device is connected
        return context.packageManager.hasSystemFeature("android.hardware.usb.host")
    }

    /**
     * Check if location permissions are granted (required for Bluetooth scanning)
     */
    fun hasLocationPermissions(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
               hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    /**
     * Check if storage permissions are granted
     */
    fun hasStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses scoped storage
            true
        } else {
            hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * Get required Bluetooth permissions for current Android version
     */
    fun getRequiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    /**
     * Get required storage permissions
     */
    fun getRequiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            emptyArray() // Scoped storage
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /**
     * Check if all critical permissions are granted
     */
    fun hasAllCriticalPermissions(): Boolean {
        return hasBluetoothPermissions() && hasLocationPermissions()
    }

    /**
     * Get missing critical permissions
     */
    fun getMissingCriticalPermissions(): List<String> {
        val missing = mutableListOf<String>()
        
        if (!hasBluetoothPermissions()) {
            missing.addAll(getRequiredBluetoothPermissions())
        }
        
        if (!hasLocationPermissions()) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        return missing.distinct()
    }

    /**
     * Check if a specific permission is granted
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get permission rationale messages
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT -> 
                "Bluetooth permission is required to connect to OBD-II adapters and scan for devices."
                
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION ->
                "Location permission is required for Bluetooth device scanning on Android 6+."
                
            Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                "Storage permission is required to save diagnostic data and export logs."
                
            else -> "This permission is required for the app to function properly."
        }
    }

    /**
     * Check if Bluetooth is supported on this device
     */
    fun isBluetoothSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    /**
     * Check if USB host mode is supported
     */
    fun isUsbHostSupported(): Boolean {
        return context.packageManager.hasSystemFeature("android.hardware.usb.host")
    }

    /**
     * Get device capability summary
     */
    fun getDeviceCapabilities(): DeviceCapabilities {
        return DeviceCapabilities(
            bluetoothSupported = isBluetoothSupported(),
            usbHostSupported = isUsbHostSupported(),
            bluetoothPermissions = hasBluetoothPermissions(),
            locationPermissions = hasLocationPermissions(),
            storagePermissions = hasStoragePermissions()
        )
    }
}

/**
 * Device capabilities data class
 */
data class DeviceCapabilities(
    val bluetoothSupported: Boolean,
    val usbHostSupported: Boolean,
    val bluetoothPermissions: Boolean,
    val locationPermissions: Boolean,
    val storagePermissions: Boolean
) {
    val canScanBluetooth: Boolean
        get() = bluetoothSupported && bluetoothPermissions && locationPermissions
        
    val canUseUsb: Boolean
        get() = usbHostSupported
        
    val canSaveData: Boolean
        get() = storagePermissions
        
    val isFullyFunctional: Boolean
        get() = canScanBluetooth && canUseUsb && canSaveData
}
