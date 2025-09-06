package com.hurtec.obd2.diagnostics.obd.processor

import com.hurtec.obd2.diagnostics.obd.elm327.ObdResponse
import kotlinx.coroutines.flow.Flow

/**
 * Interface for OBD data processing
 */
interface ObdDataProcessor {
    
    /**
     * Process a single OBD response
     */
    suspend fun processResponse(response: ObdResponse): Result<ProcessedObdData>
    
    /**
     * Process a stream of OBD responses
     */
    fun processResponseStream(responses: Flow<ObdResponse>): Flow<ProcessedObdData>
    
    /**
     * Get parameter statistics
     */
    suspend fun getParameterStatistics(pid: String, timeRange: LongRange? = null): ParameterStatistics?
    
    /**
     * Get data quality metrics
     */
    suspend fun getDataQualityMetrics(): DataQualityMetrics
    
    /**
     * Clear all cached data
     */
    suspend fun clearCache()
}

/**
 * Processed OBD data
 */
data class ProcessedObdData(
    val pid: String,
    val parameterName: String,
    val rawValue: String,
    val processedValue: Double?,
    val unit: String,
    val timestamp: Long,
    val quality: DataQuality,
    val isValid: Boolean = true,
    val anomaly: DataAnomaly? = null,
    val trend: DataTrend = DataTrend.STABLE
) {
    fun getDisplayValue(): String {
        return processedValue?.let { value ->
            when (unit) {
                "%" -> String.format("%.1f%%", value)
                "°C" -> String.format("%.1f°C", value)
                "°F" -> String.format("%.1f°F", value)
                "RPM" -> String.format("%.0f RPM", value)
                "km/h" -> String.format("%.1f km/h", value)
                "mph" -> String.format("%.1f mph", value)
                "kPa" -> String.format("%.1f kPa", value)
                "psi" -> String.format("%.1f psi", value)
                "V" -> String.format("%.2f V", value)
                "A" -> String.format("%.2f A", value)
                "g/s" -> String.format("%.2f g/s", value)
                "L/h" -> String.format("%.2f L/h", value)
                else -> String.format("%.2f %s", value, unit)
            }
        } ?: rawValue
    }
}

/**
 * Parameter statistics
 */
data class ParameterStatistics(
    val pid: String,
    val parameterName: String,
    val count: Int,
    val min: Double,
    val max: Double,
    val average: Double,
    val standardDeviation: Double,
    val lastValue: Double,
    val trend: DataTrend,
    val timeRange: LongRange
) {
    val range: Double get() = max - min
    val coefficientOfVariation: Double get() = if (average != 0.0) standardDeviation / average else 0.0
}

/**
 * Data quality metrics
 */
data class DataQualityMetrics(
    val totalDataPoints: Int = 0,
    val validDataPoints: Int = 0,
    val errorRate: Double = 0.0,
    val averageResponseTime: Double = 0.0,
    val anomalyCount: Int = 0,
    val qualityScore: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val validityRate: Double get() = if (totalDataPoints > 0) validDataPoints.toDouble() / totalDataPoints else 0.0
    val anomalyRate: Double get() = if (totalDataPoints > 0) anomalyCount.toDouble() / totalDataPoints else 0.0
}

/**
 * Data quality levels
 */
enum class DataQuality(val score: Int) {
    EXCELLENT(4),
    GOOD(3),
    FAIR(2),
    POOR(1)
}

/**
 * Data anomaly types
 */
enum class DataAnomaly {
    OUTLIER,
    EXTREME_OUTLIER,
    SUDDEN_CHANGE,
    STUCK_VALUE,
    OUT_OF_RANGE
}

/**
 * Data trend types
 */
enum class DataTrend {
    INCREASING,
    DECREASING,
    STABLE,
    VOLATILE
}
