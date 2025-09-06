package com.hurtec.obd2.diagnostics.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app preferences with crash safety
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "hurtec_obd_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_UNITS_METRIC = "units_metric"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_REFRESH_RATE = "refresh_rate"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: Flow<Int> = _themeMode.asStateFlow()

    private val _unitsMetric = MutableStateFlow(getUnitsMetric())
    val unitsMetric: Flow<Boolean> = _unitsMetric.asStateFlow()

    /**
     * Theme mode (0 = System, 1 = Light, 2 = Dark)
     */
    fun getThemeMode(): Int {
        return try {
            prefs.getInt(KEY_THEME_MODE, 0)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.getThemeMode")
            0 // Default to system theme
        }
    }

    fun setThemeMode(mode: Int) {
        try {
            prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
            _themeMode.value = mode
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.setThemeMode")
        }
    }

    /**
     * Units preference (true = metric, false = imperial)
     */
    fun getUnitsMetric(): Boolean {
        return try {
            prefs.getBoolean(KEY_UNITS_METRIC, true)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.getUnitsMetric")
            true // Default to metric
        }
    }

    fun setUnitsMetric(metric: Boolean) {
        try {
            prefs.edit().putBoolean(KEY_UNITS_METRIC, metric).apply()
            _unitsMetric.value = metric
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.setUnitsMetric")
        }
    }

    /**
     * Auto-connect preference
     */
    fun getAutoConnect(): Boolean {
        return try {
            prefs.getBoolean(KEY_AUTO_CONNECT, false)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.getAutoConnect")
            false
        }
    }

    fun setAutoConnect(autoConnect: Boolean) {
        try {
            prefs.edit().putBoolean(KEY_AUTO_CONNECT, autoConnect).apply()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.setAutoConnect")
        }
    }

    /**
     * Refresh rate in milliseconds
     */
    fun getRefreshRate(): Long {
        return try {
            prefs.getLong(KEY_REFRESH_RATE, 1000L)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.getRefreshRate")
            1000L // Default to 1 second
        }
    }

    fun setRefreshRate(rate: Long) {
        try {
            prefs.edit().putLong(KEY_REFRESH_RATE, rate).apply()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.setRefreshRate")
        }
    }

    /**
     * First launch flag
     */
    fun isFirstLaunch(): Boolean {
        return try {
            prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.isFirstLaunch")
            true
        }
    }

    fun setFirstLaunchComplete() {
        try {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.setFirstLaunchComplete")
        }
    }

    /**
     * Clear all preferences (for testing or reset)
     */
    fun clearAll() {
        try {
            prefs.edit().clear().apply()
            _themeMode.value = 0
            _unitsMetric.value = true
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PreferencesManager.clearAll")
        }
    }
}
