package com.hurtec.obd2.diagnostics.utils

import android.content.Context
import android.util.Log

/**
 * Enhanced crash handler for debugging and production
 */
object CrashHandler {
    private const val TAG = "HurtecOBD2"
    private var context: Context? = null

    /**
     * Initialize the crash handler
     */
    fun initialize(appContext: Context) {
        context = appContext.applicationContext
        logInfo("CrashHandler initialized")
    }

    fun handleException(throwable: Throwable, context: String = "") {
        Log.e(TAG, "Exception in $context: ${throwable.message}", throwable)
        
        // In a production app, you might want to:
        // - Send crash reports to a service like Crashlytics
        // - Show user-friendly error messages
        // - Attempt recovery
        
        // For now, just log the error
        throwable.printStackTrace()
    }

    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun logWarning(message: String) {
        Log.w(TAG, message)
    }

    fun logInfo(message: String) {
        Log.i(TAG, message)
    }
}
