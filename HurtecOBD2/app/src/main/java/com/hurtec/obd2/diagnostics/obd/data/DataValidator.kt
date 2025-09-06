package com.hurtec.obd2.diagnostics.obd.data

import com.hurtec.obd2.diagnostics.obd.pid.PIDDefinition
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Comprehensive data validation system for OBD-II data
 */
@Singleton
class DataValidator @Inject constructor() {
    
    // Validation history for trend analysis
    private val validationHistory = mutableMapOf<String, MutableList<ValidationResult>>()
    private val maxHistorySize = 50
    
    // Outlier detection settings
    private val outlierThresholdMultiplier = 3.0 // Standard deviations
    private val minSamplesForOutlierDetection = 5
    
    /**
     * Validate parsed PID data
     */
    fun validatePidData(parsedData: ParsedPidData): ValidationResult {
        return try {
            val validationErrors = mutableListOf<String>()
            val validationWarnings = mutableListOf<String>()
            
            // Basic validation
            if (!validateBasicData(parsedData, validationErrors)) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = validationErrors.joinToString("; "),
                    warnings = validationWarnings,
                    validationType = ValidationType.BASIC
                )
            }
            
            // Range validation
            if (!validateRange(parsedData, validationErrors, validationWarnings)) {
                // Range validation failure is a warning, not an error
                validationWarnings.add("Value outside expected range")
            }
            
            // Trend validation
            validateTrend(parsedData, validationWarnings)
            
            // Outlier detection
            validateOutliers(parsedData, validationWarnings)
            
            // Store validation history
            storeValidationHistory(parsedData)
            
            ValidationResult(
                isValid = validationErrors.isEmpty(),
                errorMessage = if (validationErrors.isNotEmpty()) validationErrors.joinToString("; ") else null,
                warnings = validationWarnings,
                validationType = ValidationType.COMPREHENSIVE,
                confidence = calculateConfidence(parsedData, validationWarnings.size)
            )
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "DataValidator.validatePidData")
            ValidationResult(
                isValid = false,
                errorMessage = "Validation failed: ${e.message}",
                validationType = ValidationType.ERROR
            )
        }
    }
    
    /**
     * Basic data validation
     */
    private fun validateBasicData(parsedData: ParsedPidData, errors: MutableList<String>): Boolean {
        var isValid = true
        
        // Check if PID is valid
        if (parsedData.pid.isBlank()) {
            errors.add("PID is empty")
            isValid = false
        }
        
        // Check if response is valid
        if (parsedData.rawResponse.isBlank()) {
            errors.add("Raw response is empty")
            isValid = false
        }
        
        // Check if data bytes are present
        if (parsedData.dataBytes.isEmpty()) {
            errors.add("No data bytes found")
            isValid = false
        }
        
        // Check for error responses
        val cleanResponse = parsedData.cleanResponse.uppercase()
        when {
            cleanResponse.contains("NODATA") -> {
                errors.add("No data response")
                isValid = false
            }
            cleanResponse.contains("ERROR") -> {
                errors.add("Error response")
                isValid = false
            }
            cleanResponse.contains("?") -> {
                errors.add("Unknown command response")
                isValid = false
            }
            cleanResponse.contains("UNABLETOCONNECT") -> {
                errors.add("Unable to connect")
                isValid = false
            }
            cleanResponse.contains("BUSBUSY") -> {
                errors.add("Bus busy")
                isValid = false
            }
        }
        
        return isValid
    }
    
    /**
     * Range validation
     */
    private fun validateRange(
        parsedData: ParsedPidData,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): Boolean {
        val pidDefinition = parsedData.pidDefinition ?: return true
        val numericValue = parsedData.numericValue ?: return true
        
        // Since PIDDefinition doesn't have minValue/maxValue, use PID-specific ranges
        val (minValue, maxValue) = getPidRange(pidDefinition.pid)

        return when {
            numericValue < minValue -> {
                warnings.add("Value ${numericValue} below minimum ${minValue}")
                false
            }
            numericValue > maxValue -> {
                warnings.add("Value ${numericValue} above maximum ${maxValue}")
                false
            }
            else -> true
        }
    }
    
    /**
     * Trend validation - check for unrealistic changes
     */
    private fun validateTrend(parsedData: ParsedPidData, warnings: MutableList<String>) {
        val history = validationHistory[parsedData.pid] ?: return
        if (history.size < 2) return
        
        val currentValue = parsedData.numericValue ?: return
        val lastValidation = history.lastOrNull() ?: return
        val lastValue = lastValidation.value ?: return
        
        val pidDefinition = parsedData.pidDefinition ?: return
        
        // Calculate maximum reasonable change based on PID type
        val maxReasonableChange = calculateMaxReasonableChange(pidDefinition, parsedData.timestamp - lastValidation.timestamp)
        
        val actualChange = abs(currentValue - lastValue)
        
        if (actualChange > maxReasonableChange) {
            warnings.add("Unrealistic change: ${actualChange} (max expected: ${maxReasonableChange})")
        }
    }
    
    /**
     * Outlier detection using statistical methods
     */
    private fun validateOutliers(parsedData: ParsedPidData, warnings: MutableList<String>) {
        val history = validationHistory[parsedData.pid] ?: return
        if (history.size < minSamplesForOutlierDetection) return
        
        val currentValue = parsedData.numericValue ?: return
        val historicalValues = history.mapNotNull { it.value }
        
        if (historicalValues.size < minSamplesForOutlierDetection) return
        
        val mean = historicalValues.average()
        val standardDeviation = calculateStandardDeviation(historicalValues, mean)
        
        val zScore = abs(currentValue - mean) / standardDeviation
        
        if (zScore > outlierThresholdMultiplier) {
            warnings.add("Statistical outlier detected (z-score: ${String.format("%.2f", zScore)})")
        }
    }
    
    /**
     * Calculate maximum reasonable change for a PID
     */
    private fun calculateMaxReasonableChange(pidDefinition: PIDDefinition, timeDeltaMs: Long): Double {
        val timeDeltaSeconds = timeDeltaMs / 1000.0
        
        return when (pidDefinition.pid) {
            "0C" -> 1000.0 * timeDeltaSeconds // RPM can change quickly
            "0D" -> 50.0 * timeDeltaSeconds   // Speed changes
            "05" -> 5.0 * timeDeltaSeconds    // Coolant temp changes slowly
            "0F" -> 2.0 * timeDeltaSeconds    // Intake air temp
            "11" -> 20.0 * timeDeltaSeconds   // Throttle position
            "04" -> 10.0 * timeDeltaSeconds   // Engine load
            else -> {
                val (minValue, maxValue) = getPidRange(pidDefinition.pid)
                (maxValue - minValue) * 0.1 * timeDeltaSeconds
            }
        }
    }

    /**
     * Get PID-specific value range
     */
    private fun getPidRange(pid: String): Pair<Double, Double> {
        return when (pid.uppercase()) {
            "0C", "010C" -> Pair(0.0, 16383.75)  // RPM
            "0D", "010D" -> Pair(0.0, 255.0)     // Speed km/h
            "05", "0105" -> Pair(-40.0, 215.0)   // Coolant temp °C
            "0F", "010F" -> Pair(-40.0, 215.0)   // Intake air temp °C
            "11", "0111" -> Pair(0.0, 100.0)     // Throttle position %
            "04", "0104" -> Pair(0.0, 100.0)     // Engine load %
            "06", "0106" -> Pair(-100.0, 99.2)   // Short term fuel trim %
            "07", "0107" -> Pair(-100.0, 99.2)   // Long term fuel trim %
            "0A", "010A" -> Pair(0.0, 765.0)     // Fuel pressure kPa
            "0B", "010B" -> Pair(0.0, 255.0)     // Intake manifold pressure kPa
            "10", "0110" -> Pair(0.0, 655.35)    // MAF air flow g/s
            else -> Pair(0.0, 1000.0)            // Default range
        }
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStandardDeviation(values: List<Double>, mean: Double): Double {
        if (values.size <= 1) return 0.0
        
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * Calculate confidence score
     */
    private fun calculateConfidence(parsedData: ParsedPidData, warningCount: Int): Double {
        var confidence = 1.0
        
        // Reduce confidence based on warnings
        confidence -= warningCount * 0.1
        
        // Reduce confidence if no PID definition
        if (parsedData.pidDefinition == null) {
            confidence -= 0.3
        }
        
        // Reduce confidence if no numeric value
        if (parsedData.numericValue == null) {
            confidence -= 0.2
        }
        
        // Increase confidence based on historical consistency
        val history = validationHistory[parsedData.pid]
        if (history != null && history.size > 5) {
            val recentValidations = history.takeLast(5)
            val validCount = recentValidations.count { it.isValid }
            confidence += (validCount / 5.0) * 0.1
        }
        
        return maxOf(0.0, minOf(1.0, confidence))
    }
    
    /**
     * Store validation history
     */
    private fun storeValidationHistory(parsedData: ParsedPidData) {
        val history = validationHistory.getOrPut(parsedData.pid) { mutableListOf() }
        
        history.add(
            ValidationResult(
                isValid = true,
                value = parsedData.numericValue,
                timestamp = parsedData.timestamp,
                validationType = ValidationType.HISTORICAL
            )
        )
        
        // Maintain history size
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        }
    }
    
    /**
     * Get validation statistics for a PID
     */
    fun getValidationStats(pid: String): ValidationStats? {
        val history = validationHistory[pid] ?: return null
        
        val validCount = history.count { it.isValid }
        val totalCount = history.size
        val successRate = if (totalCount > 0) (validCount.toFloat() / totalCount.toFloat()) * 100f else 0f
        
        val values = history.mapNotNull { it.value }
        val mean = if (values.isNotEmpty()) values.average() else 0.0
        val standardDeviation = if (values.size > 1) calculateStandardDeviation(values, mean) else 0.0
        
        return ValidationStats(
            pid = pid,
            totalValidations = totalCount,
            validValidations = validCount,
            successRate = successRate,
            mean = mean,
            standardDeviation = standardDeviation,
            minValue = values.minOrNull() ?: 0.0,
            maxValue = values.maxOrNull() ?: 0.0
        )
    }
    
    /**
     * Clear validation history for a PID
     */
    fun clearHistory(pid: String) {
        validationHistory[pid]?.clear()
    }
    
    /**
     * Clear all validation history
     */
    fun clearAllHistory() {
        validationHistory.clear()
    }
    
    /**
     * Get overall validation statistics
     */
    fun getOverallStats(): OverallValidationStats {
        val allValidations = validationHistory.values.flatten()
        val totalValidations = allValidations.size
        val validValidations = allValidations.count { it.isValid }
        val successRate = if (totalValidations > 0) (validValidations.toFloat() / totalValidations.toFloat()) * 100f else 0f
        
        return OverallValidationStats(
            totalPids = validationHistory.size,
            totalValidations = totalValidations,
            validValidations = validValidations,
            overallSuccessRate = successRate,
            averageConfidence = allValidations.mapNotNull { it.confidence }.average()
        )
    }
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val warnings: List<String> = emptyList(),
    val validationType: ValidationType = ValidationType.BASIC,
    val confidence: Double = 1.0,
    val value: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Validation types
 */
enum class ValidationType {
    BASIC,
    RANGE,
    TREND,
    OUTLIER,
    COMPREHENSIVE,
    HISTORICAL,
    ERROR
}

/**
 * Validation statistics for a specific PID
 */
data class ValidationStats(
    val pid: String,
    val totalValidations: Int,
    val validValidations: Int,
    val successRate: Float,
    val mean: Double,
    val standardDeviation: Double,
    val minValue: Double,
    val maxValue: Double
)

/**
 * Overall validation statistics
 */
data class OverallValidationStats(
    val totalPids: Int,
    val totalValidations: Int,
    val validValidations: Int,
    val overallSuccessRate: Float,
    val averageConfidence: Double
)
