package com.hurtec.obd2.diagnostics

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.profileinstaller.ProfileInstaller
import androidx.work.Configuration
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.performance.PerformanceOptimizer
import com.hurtec.obd2.diagnostics.utils.MemoryManager
import com.hurtec.obd2.diagnostics.database.HurtecObdDatabase
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Hurtec OBD-II Application class with Hilt and WorkManager integration
 */
@HiltAndroidApp
class HurtecApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var performanceOptimizer: PerformanceOptimizer

    @Inject
    lateinit var memoryManager: MemoryManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize crash handler first
        CrashHandler.initialize(this)

        // Initialize Timber for advanced logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            enableStrictMode()
        }

        // Initialize performance optimizations
        initializePerformanceOptimizations()

        // Initialize profile installer for faster startup
        ProfileInstaller.writeProfile(this)

        // Initialize database early to prevent crashes
        initializeDatabase()

        CrashHandler.logInfo("Hurtec OBD-II Application started with full optimizations")
        Timber.i("Application initialized successfully")
    }

    /**
     * Initialize performance optimizations
     */
    private fun initializePerformanceOptimizations() {
        try {
            // Set up enhanced global exception handler
            Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
                CrashHandler.handleException(exception, "UncaughtException.${thread.name}")

                // Try to perform emergency cleanup
                try {
                    if (::performanceOptimizer.isInitialized) {
                        performanceOptimizer.performEmergencyCleanup()
                    }
                    if (::memoryManager.isInitialized) {
                        memoryManager.optimizeMemoryUsage()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HurtecApp", "Emergency cleanup failed", e)
                }

                // Let the system handle the crash
                System.exit(1)
            }

        } catch (e: Exception) {
            CrashHandler.handleException(e, "HurtecApplication.initializePerformanceOptimizations")
        }
    }

    /**
     * Initialize database early to prevent crashes
     */
    private fun initializeDatabase() {
        try {
            CrashHandler.logInfo("Initializing database...")
            val database = HurtecObdDatabase.getDatabase(this)
            CrashHandler.logInfo("Database initialized successfully")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "HurtecApplication.initializeDatabase")
            CrashHandler.logError("Database initialization failed, app may have limited functionality")
        }
    }

    /**
     * Enable StrictMode for debugging
     */
    private fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        CrashHandler.logWarning("System low memory detected")

        try {
            if (::performanceOptimizer.isInitialized) {
                performanceOptimizer.performEmergencyCleanup()
            }
            if (::memoryManager.isInitialized) {
                memoryManager.optimizeMemoryUsage()
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "HurtecApplication.onLowMemory")
        }
    }
}
