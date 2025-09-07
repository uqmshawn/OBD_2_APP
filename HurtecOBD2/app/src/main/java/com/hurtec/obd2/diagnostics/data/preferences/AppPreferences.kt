package com.hurtec.obd2.diagnostics.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App preferences manager for persistent settings
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "hurtec_obd_prefs"
        
        // Keys
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ACTIVE_VEHICLE_ID = "active_vehicle_id"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_UNIT_SYSTEM = "unit_system"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_DEMO_MODE = "demo_mode"
        private const val KEY_LAST_CONNECTED_DEVICE = "last_connected_device"
        private const val KEY_DATA_LOGGING_ENABLED = "data_logging_enabled"
        private const val KEY_EXPORT_FORMAT = "export_format"
        private const val KEY_LOCATION_ENABLED = "location_enabled"
    }
    
    // First launch and onboarding
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH, value) }
    
    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, value) }
    
    // Vehicle settings
    var activeVehicleId: Long
        get() = prefs.getLong(KEY_ACTIVE_VEHICLE_ID, -1L)
        set(value) = prefs.edit { putLong(KEY_ACTIVE_VEHICLE_ID, value) }
    
    // Display settings
    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) = prefs.edit { putBoolean(KEY_KEEP_SCREEN_ON, value) }
    
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit { putString(KEY_THEME_MODE, value) }
    
    // Unit system
    var unitSystem: String
        get() = prefs.getString(KEY_UNIT_SYSTEM, "metric") ?: "metric"
        set(value) = prefs.edit { putString(KEY_UNIT_SYSTEM, value) }
    
    // Connection settings
    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_CONNECT, value) }
    
    var lastConnectedDevice: String?
        get() = prefs.getString(KEY_LAST_CONNECTED_DEVICE, null)
        set(value) = prefs.edit { putString(KEY_LAST_CONNECTED_DEVICE, value) }
    
    // Demo mode
    var demoMode: Boolean
        get() = prefs.getBoolean(KEY_DEMO_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DEMO_MODE, value) }
    
    // Data logging
    var dataLoggingEnabled: Boolean
        get() = prefs.getBoolean(KEY_DATA_LOGGING_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_DATA_LOGGING_ENABLED, value) }
    
    var exportFormat: String
        get() = prefs.getString(KEY_EXPORT_FORMAT, "csv") ?: "csv"
        set(value) = prefs.edit { putString(KEY_EXPORT_FORMAT, value) }
    
    // Location
    var locationEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCATION_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_LOCATION_ENABLED, value) }
    
    /**
     * Check if app setup is complete
     */
    fun isAppSetupComplete(): Boolean {
        return isOnboardingCompleted && activeVehicleId != -1L
    }
    
    /**
     * Mark app setup as complete
     */
    fun markSetupComplete(vehicleId: Long) {
        prefs.edit {
            putBoolean(KEY_FIRST_LAUNCH, false)
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
            putLong(KEY_ACTIVE_VEHICLE_ID, vehicleId)
        }
        CrashHandler.logInfo("App setup marked as complete with vehicle ID: $vehicleId")
    }
    
    /**
     * Reset all preferences (for testing or factory reset)
     */
    fun resetAll() {
        prefs.edit().clear().apply()
        CrashHandler.logInfo("All preferences reset")
    }
    
    /**
     * Get all preferences as map for debugging
     */
    fun getAllPreferences(): Map<String, Any?> {
        return prefs.all
    }
    
    /**
     * Export preferences to string
     */
    fun exportPreferences(): String {
        val allPrefs = prefs.all
        val sb = StringBuilder()
        sb.append("Hurtec OBD-II App Preferences Export\n")
        sb.append("=====================================\n")
        sb.append("Export Time: ${java.util.Date()}\n\n")
        
        allPrefs.forEach { (key, value) ->
            sb.append("$key = $value\n")
        }
        
        return sb.toString()
    }
    
    /**
     * Get theme mode as enum
     */
    enum class ThemeMode {
        LIGHT, DARK, SYSTEM;
        
        companion object {
            fun fromString(value: String): ThemeMode {
                return when (value.lowercase()) {
                    "light" -> LIGHT
                    "dark" -> DARK
                    else -> SYSTEM
                }
            }
        }
    }
    
    /**
     * Get unit system as enum
     */
    enum class UnitSystem {
        METRIC, IMPERIAL;
        
        companion object {
            fun fromString(value: String): UnitSystem {
                return when (value.lowercase()) {
                    "imperial" -> IMPERIAL
                    else -> METRIC
                }
            }
        }
    }
}
