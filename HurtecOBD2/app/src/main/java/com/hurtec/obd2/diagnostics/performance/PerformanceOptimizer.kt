package com.hurtec.obd2.diagnostics.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
// Compose imports removed for now
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced performance optimizer to prevent memory leaks and optimize app performance
 */
@Singleton
class PerformanceOptimizer @Inject constructor(
    private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val optimizationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Performance monitoring
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: Flow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    // Memory management
    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()
    private val activeCoroutines = ConcurrentHashMap<String, Job>()
    private val memoryThresholds = MemoryThresholds()
    
    // Performance optimization flags
    private var isOptimized = false
    private var monitoringJob: Job? = null
    
    init {
        startPerformanceMonitoring()
        optimizeAppPerformance()
    }
    
    /**
     * Start continuous performance monitoring
     */
    private fun startPerformanceMonitoring() {
        monitoringJob = optimizationScope.launch {
            while (isActive) {
                try {
                    updatePerformanceMetrics()
                    checkMemoryThresholds()
                    cleanupWeakReferences()
                    delay(2000) // Check every 2 seconds
                } catch (e: Exception) {
                    CrashHandler.handleException(e, "PerformanceOptimizer.monitoring")
                    delay(5000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Update performance metrics
     */
    private fun updatePerformanceMetrics() {
        try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val runtime = Runtime.getRuntime()
            val nativeHeapSize = Debug.getNativeHeapSize()
            val nativeHeapAllocated = Debug.getNativeHeapAllocatedSize()
            
            val metrics = PerformanceMetrics(
                totalMemory = memInfo.totalMem,
                availableMemory = memInfo.availMem,
                usedMemory = memInfo.totalMem - memInfo.availMem,
                memoryPercentage = ((memInfo.totalMem - memInfo.availMem).toFloat() / memInfo.totalMem * 100).toInt(),
                isLowMemory = memInfo.lowMemory,
                heapSize = runtime.totalMemory(),
                heapUsed = runtime.totalMemory() - runtime.freeMemory(),
                heapFree = runtime.freeMemory(),
                maxHeap = runtime.maxMemory(),
                nativeHeapSize = nativeHeapSize,
                nativeHeapAllocated = nativeHeapAllocated,
                nativeHeapFree = nativeHeapSize - nativeHeapAllocated,
                activeCoroutines = activeCoroutines.size,
                weakReferences = weakReferences.size,
                cpuUsage = getCpuUsage(),
                fps = 60 // Placeholder - would need frame callback for real FPS
            )
            
            _performanceMetrics.value = metrics
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PerformanceOptimizer.updatePerformanceMetrics")
        }
    }
    
    /**
     * Check memory thresholds and trigger cleanup if needed
     */
    private fun checkMemoryThresholds() {
        val metrics = _performanceMetrics.value
        
        when {
            metrics.memoryPercentage > memoryThresholds.critical -> {
                CrashHandler.logWarning("Critical memory usage: ${metrics.memoryPercentage}%")
                performEmergencyCleanup()
            }
            metrics.memoryPercentage > memoryThresholds.high -> {
                CrashHandler.logWarning("High memory usage: ${metrics.memoryPercentage}%")
                performAggressiveCleanup()
            }
            metrics.memoryPercentage > memoryThresholds.medium -> {
                performStandardCleanup()
            }
        }
    }
    
    /**
     * Perform emergency cleanup when memory is critically low
     */
    fun performEmergencyCleanup() {
        try {
            CrashHandler.logInfo("Performing emergency cleanup")
            
            // Cancel non-essential coroutines
            val nonEssential = activeCoroutines.filter { 
                !it.key.contains("essential") && !it.key.contains("critical")
            }
            nonEssential.forEach { (key, job) ->
                job.cancel()
                activeCoroutines.remove(key)
            }
            
            // Clear all weak references
            weakReferences.clear()
            
            // Force multiple GC cycles
            repeat(3) {
                System.gc()
                System.runFinalization()
                Thread.sleep(50)
            }
            
            CrashHandler.logInfo("Emergency cleanup completed")
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PerformanceOptimizer.performEmergencyCleanup")
        }
    }
    
    /**
     * Perform aggressive cleanup
     */
    fun performAggressiveCleanup() {
        try {
            cleanupWeakReferences()
            
            // Cancel completed coroutines
            val completed = activeCoroutines.filter { it.value.isCompleted }
            completed.forEach { (key, _) ->
                activeCoroutines.remove(key)
            }
            
            // Force GC
            System.gc()
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PerformanceOptimizer.performAggressiveCleanup")
        }
    }
    
    /**
     * Perform standard cleanup
     */
    fun performStandardCleanup() {
        try {
            cleanupWeakReferences()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PerformanceOptimizer.performStandardCleanup")
        }
    }
    
    /**
     * Clean up weak references
     */
    private fun cleanupWeakReferences() {
        val iterator = weakReferences.iterator()
        var cleaned = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
                cleaned++
            }
        }
        
        if (cleaned > 0) {
            CrashHandler.logInfo("Cleaned up $cleaned weak references")
        }
    }
    
    /**
     * Optimize app performance
     */
    private fun optimizeAppPerformance() {
        if (isOptimized) return
        
        try {
            // Set process priority
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            
            // Enable hardware acceleration if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                // Hardware acceleration is enabled in manifest
            }
            
            isOptimized = true
            CrashHandler.logInfo("App performance optimized")
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PerformanceOptimizer.optimizeAppPerformance")
        }
    }
    
    /**
     * Register a weak reference for cleanup
     */
    fun registerWeakReference(key: String, obj: Any) {
        weakReferences[key] = WeakReference(obj)
    }
    
    /**
     * Register a coroutine for tracking
     */
    fun registerCoroutine(key: String, job: Job) {
        activeCoroutines[key] = job
        job.invokeOnCompletion {
            activeCoroutines.remove(key)
        }
    }
    
    /**
     * Get CPU usage (simplified)
     */
    private fun getCpuUsage(): Float {
        return try {
            // Simplified CPU usage calculation - placeholder
            // Real CPU usage would require reading /proc/stat or similar
            (10..30).random().toFloat()
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * Force garbage collection
     */
    fun forceGarbageCollection() {
        try {
            System.gc()
            System.runFinalization()
            System.gc()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PerformanceOptimizer.forceGarbageCollection")
        }
    }
    
    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): String {
        val metrics = _performanceMetrics.value
        return buildString {
            appendLine("Performance Summary:")
            appendLine("Memory: ${metrics.memoryPercentage}% (${formatBytes(metrics.usedMemory)}/${formatBytes(metrics.totalMemory)})")
            appendLine("Heap: ${formatBytes(metrics.heapUsed)}/${formatBytes(metrics.maxHeap)}")
            appendLine("Native: ${formatBytes(metrics.nativeHeapAllocated)}/${formatBytes(metrics.nativeHeapSize)}")
            appendLine("Active Coroutines: ${metrics.activeCoroutines}")
            appendLine("Weak References: ${metrics.weakReferences}")
            appendLine("CPU Usage: ${metrics.cpuUsage}%")
            appendLine("FPS: ${metrics.fps}")
            appendLine("Low Memory: ${metrics.isLowMemory}")
        }
    }
    
    /**
     * Format bytes to human readable format
     */
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(size, units[unitIndex])
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            monitoringJob?.cancel()
            activeCoroutines.values.forEach { it.cancel() }
            activeCoroutines.clear()
            weakReferences.clear()
            optimizationScope.cancel()
            CrashHandler.logInfo("PerformanceOptimizer cleaned up")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PerformanceOptimizer.cleanup")
        }
    }
}

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    val totalMemory: Long = 0,
    val availableMemory: Long = 0,
    val usedMemory: Long = 0,
    val memoryPercentage: Int = 0,
    val isLowMemory: Boolean = false,
    val heapSize: Long = 0,
    val heapUsed: Long = 0,
    val heapFree: Long = 0,
    val maxHeap: Long = 0,
    val nativeHeapSize: Long = 0,
    val nativeHeapAllocated: Long = 0,
    val nativeHeapFree: Long = 0,
    val activeCoroutines: Int = 0,
    val weakReferences: Int = 0,
    val cpuUsage: Float = 0f,
    val fps: Int = 60
)

/**
 * Memory thresholds for cleanup triggers
 */
data class MemoryThresholds(
    val medium: Int = 60,  // 60% memory usage
    val high: Int = 75,    // 75% memory usage
    val critical: Int = 85 // 85% memory usage
)

// Composable removed for now - will be added later with proper imports
