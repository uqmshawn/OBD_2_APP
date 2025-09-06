package com.hurtec.obd2.diagnostics.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory manager to prevent leaks and optimize performance
 */
@Singleton
class MemoryManager @Inject constructor(
    private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Memory monitoring
    private val _memoryInfo = MutableStateFlow(MemoryInfo())
    val memoryInfo: Flow<MemoryInfo> = _memoryInfo.asStateFlow()
    
    // Weak reference cache for cleanup
    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()
    
    // Coroutine job tracking
    private val activeJobs = ConcurrentHashMap<String, Job>()
    
    // Memory monitoring job
    private var monitoringJob: Job? = null
    
    init {
        startMemoryMonitoring()
    }
    
    /**
     * Start memory monitoring
     */
    private fun startMemoryMonitoring() {
        monitoringJob = memoryScope.launch {
            while (isActive) {
                try {
                    updateMemoryInfo()
                    
                    // Check for low memory
                    val currentInfo = _memoryInfo.value
                    if (currentInfo.isLowMemory) {
                        CrashHandler.logWarning("Low memory detected, triggering cleanup")
                        performEmergencyCleanup()
                    }
                    
                    // Clean up weak references
                    cleanupWeakReferences()
                    
                    delay(5000) // Check every 5 seconds
                } catch (e: Exception) {
                    CrashHandler.handleException(e, "MemoryManager.startMemoryMonitoring")
                    delay(10000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Update memory information
     */
    private fun updateMemoryInfo() {
        try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val runtime = Runtime.getRuntime()
            val nativeHeapSize = Debug.getNativeHeapSize()
            val nativeHeapAllocated = Debug.getNativeHeapAllocatedSize()
            val nativeHeapFree = nativeHeapSize - nativeHeapAllocated
            
            _memoryInfo.value = MemoryInfo(
                totalMemory = memInfo.totalMem,
                availableMemory = memInfo.availMem,
                usedMemory = memInfo.totalMem - memInfo.availMem,
                memoryPercentage = ((memInfo.totalMem - memInfo.availMem).toFloat() / memInfo.totalMem * 100).toInt(),
                isLowMemory = memInfo.lowMemory,
                threshold = memInfo.threshold,
                heapSize = runtime.totalMemory(),
                heapUsed = runtime.totalMemory() - runtime.freeMemory(),
                heapFree = runtime.freeMemory(),
                maxHeap = runtime.maxMemory(),
                nativeHeapSize = nativeHeapSize,
                nativeHeapAllocated = nativeHeapAllocated,
                nativeHeapFree = nativeHeapFree
            )
        } catch (e: Exception) {
            CrashHandler.handleException(e, "MemoryManager.updateMemoryInfo")
        }
    }
    
    /**
     * Register a weak reference for cleanup
     */
    fun registerWeakReference(key: String, obj: Any) {
        weakReferences[key] = WeakReference(obj)
    }
    
    /**
     * Clean up weak references
     */
    private fun cleanupWeakReferences() {
        val iterator = weakReferences.iterator()
        var cleanedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
                cleanedCount++
            }
        }
        
        if (cleanedCount > 0) {
            CrashHandler.logInfo("Cleaned up $cleanedCount weak references")
        }
    }
    
    /**
     * Register a coroutine job for tracking
     */
    fun registerJob(key: String, job: Job) {
        // Cancel existing job if any
        activeJobs[key]?.cancel()
        activeJobs[key] = job
        
        // Auto-cleanup when job completes
        job.invokeOnCompletion {
            activeJobs.remove(key)
        }
    }
    
    /**
     * Cancel a specific job
     */
    fun cancelJob(key: String) {
        activeJobs[key]?.cancel()
        activeJobs.remove(key)
    }
    
    /**
     * Cancel all active jobs
     */
    fun cancelAllJobs() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        CrashHandler.logInfo("Cancelled ${activeJobs.size} active jobs")
    }
    
    /**
     * Perform emergency cleanup when memory is low
     */
    private fun performEmergencyCleanup() {
        try {
            CrashHandler.logInfo("Performing emergency memory cleanup")
            
            // Cancel non-essential jobs
            val nonEssentialJobs = activeJobs.filter { 
                !it.key.contains("critical") && !it.key.contains("essential")
            }
            
            nonEssentialJobs.forEach { (key, job) ->
                job.cancel()
                activeJobs.remove(key)
            }
            
            // Clear weak references
            cleanupWeakReferences()
            
            // Force garbage collection
            System.gc()
            
            // Wait a bit for GC to complete
            Thread.sleep(100)
            
            CrashHandler.logInfo("Emergency cleanup completed")
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "MemoryManager.performEmergencyCleanup")
        }
    }
    
    /**
     * Force garbage collection (use sparingly)
     */
    fun forceGarbageCollection() {
        try {
            CrashHandler.logInfo("Forcing garbage collection")
            System.gc()
            System.runFinalization()
            System.gc()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "MemoryManager.forceGarbageCollection")
        }
    }
    
    /**
     * Get memory usage summary
     */
    fun getMemoryUsageSummary(): String {
        val info = _memoryInfo.value
        return buildString {
            appendLine("Memory Usage Summary:")
            appendLine("Total: ${formatBytes(info.totalMemory)}")
            appendLine("Used: ${formatBytes(info.usedMemory)} (${info.memoryPercentage}%)")
            appendLine("Available: ${formatBytes(info.availableMemory)}")
            appendLine("Heap Used: ${formatBytes(info.heapUsed)}")
            appendLine("Heap Free: ${formatBytes(info.heapFree)}")
            appendLine("Max Heap: ${formatBytes(info.maxHeap)}")
            appendLine("Native Heap: ${formatBytes(info.nativeHeapAllocated)}/${formatBytes(info.nativeHeapSize)}")
            appendLine("Active Jobs: ${activeJobs.size}")
            appendLine("Weak References: ${weakReferences.size}")
            appendLine("Low Memory: ${info.isLowMemory}")
        }
    }
    
    /**
     * Check if memory usage is critical
     */
    fun isMemoryUsageCritical(): Boolean {
        val info = _memoryInfo.value
        return info.isLowMemory || info.memoryPercentage > 85
    }
    
    /**
     * Optimize memory usage
     */
    fun optimizeMemoryUsage() {
        memoryScope.launch {
            try {
                CrashHandler.logInfo("Optimizing memory usage")
                
                // Clean up weak references
                cleanupWeakReferences()
                
                // Cancel completed jobs
                val completedJobs = activeJobs.filter { it.value.isCompleted }
                completedJobs.forEach { (key, _) ->
                    activeJobs.remove(key)
                }
                
                // Suggest garbage collection
                System.gc()
                
                CrashHandler.logInfo("Memory optimization completed")
                
            } catch (e: Exception) {
                CrashHandler.handleException(e, "MemoryManager.optimizeMemoryUsage")
            }
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
            cancelAllJobs()
            weakReferences.clear()
            memoryScope.cancel()
            CrashHandler.logInfo("MemoryManager cleaned up")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "MemoryManager.cleanup")
        }
    }
}

/**
 * Memory information data class
 */
data class MemoryInfo(
    val totalMemory: Long = 0,
    val availableMemory: Long = 0,
    val usedMemory: Long = 0,
    val memoryPercentage: Int = 0,
    val isLowMemory: Boolean = false,
    val threshold: Long = 0,
    val heapSize: Long = 0,
    val heapUsed: Long = 0,
    val heapFree: Long = 0,
    val maxHeap: Long = 0,
    val nativeHeapSize: Long = 0,
    val nativeHeapAllocated: Long = 0,
    val nativeHeapFree: Long = 0
)
