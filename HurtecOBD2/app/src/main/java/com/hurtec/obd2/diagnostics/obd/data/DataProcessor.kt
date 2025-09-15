package com.hurtec.obd2.diagnostics.obd.data

import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.database.repository.ObdDataRepository
import com.hurtec.obd2.diagnostics.database.entities.ObdDataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time data processing pipeline with unit conversions and validation
 */
@Singleton
class DataProcessor @Inject constructor(
    private val pidInterpreter: PidInterpreter,
    private val unitConverter: UnitConverter,
    private val dataValidator: DataValidator,
    private val obdDataRepository: ObdDataRepository
) {
    
    // Data streams
    private val _processedData = MutableSharedFlow<ProcessedObdData>(replay = 1)
    val processedData: Flow<ProcessedObdData> = _processedData.asSharedFlow()
    
    private val _dataErrors = MutableSharedFlow<DataError>()
    val dataErrors: Flow<DataError> = _dataErrors.asSharedFlow()
    
    // Data buffer for streaming
    private val dataBuffer = ConcurrentHashMap<String, MutableList<ProcessedObdData>>()
    private val maxBufferSize = 100
    
    // Processing statistics
    private var totalProcessed = 0L
    private var validData = 0L
    private var invalidData = 0L

    // Current session and vehicle
    private var currentSessionId: String? = null
    private var currentVehicleId: Long? = null
    private var persistDataToDatabase = true
    
    /**
     * Process raw OBD response data
     */
    suspend fun processRawData(
        rawResponse: String,
        command: String,
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): ProcessedObdData? {
        return try {
            totalProcessed++
            
            // Parse the raw response
            val parsedData = pidInterpreter.parseResponse(rawResponse, command)
            if (parsedData == null) {
                invalidData++
                emitError(DataError(DataError.PARSING_FAILED, "Failed to parse response: $rawResponse", System.currentTimeMillis()))
                return null
            }
            
            // Validate the parsed data
            val validationResult = dataValidator.validatePidData(parsedData)
            if (!validationResult.isValid) {
                invalidData++
                emitError(DataError(DataError.VALIDATION_FAILED, validationResult.errorMessage ?: "Validation failed", System.currentTimeMillis()))
                return null
            }
            
            // Convert units if needed
            val convertedData = if (unitSystem != UnitSystem.METRIC) {
                convertUnits(parsedData, unitSystem)
            } else {
                parsedData
            }
            
            // Create processed data
            val processedData = ProcessedObdData(
                pid = convertedData.pid,
                pidDefinition = convertedData.pidDefinition,
                rawValue = convertedData.numericValue,
                processedValue = convertedData.numericValue,
                stringValue = convertedData.stringValue,
                formattedValue = convertedData.formattedValue,
                unit = convertedData.pidDefinition?.unit ?: "",
                unitSystem = unitSystem,
                timestamp = convertedData.timestamp,
                isValid = true,
                quality = calculateDataQuality(convertedData),
                metadata = DataMetadata(
                    rawResponse = rawResponse,
                    command = command,
                    processingTime = System.currentTimeMillis() - convertedData.timestamp,
                    dataBytes = convertedData.dataBytes
                )
            )
            
            // Buffer the data
            bufferData(processedData)
            
            // Store to database if enabled and session/vehicle are set
            if (persistDataToDatabase && currentSessionId != null && currentVehicleId != null) {
                try {
                    // Create ObdDataEntity from processed data
                    val obdDataEntity = createObdDataEntity(processedData, currentVehicleId!!, currentSessionId!!)
                    obdDataRepository.storeObdData(obdDataEntity)
                    CrashHandler.logInfo("Stored OBD data to database for session $currentSessionId")
                } catch (e: Exception) {
                    CrashHandler.handleException(e, "DataProcessor.storeToDatabase")
                    // Don't fail the processing if database storage fails
                }
            }

            // Emit the processed data
            _processedData.emit(processedData)

            validData++
            CrashHandler.logInfo("Processed data for PID ${processedData.pid}: ${processedData.formattedValue}")

            processedData
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "DataProcessor.processRawData")
            invalidData++
            emitError(DataError(DataError.PROCESSING_FAILED, "Processing failed: ${e.message}", System.currentTimeMillis()))
            null
        }
    }
    
    /**
     * Process multiple responses in batch
     */
    suspend fun processBatchData(
        responses: List<Pair<String, String>>, // (rawResponse, command) pairs
        unitSystem: UnitSystem = UnitSystem.METRIC
    ): List<ProcessedObdData> {
        return responses.mapNotNull { (rawResponse, command) ->
            processRawData(rawResponse, command, unitSystem)
        }
    }
    
    /**
     * Convert units based on unit system
     */
    private fun convertUnits(parsedData: ParsedPidData, unitSystem: UnitSystem): ParsedPidData {
        return try {
            val pidDefinition = parsedData.pidDefinition ?: return parsedData
            val numericValue = parsedData.numericValue ?: return parsedData
            
            val convertedValue = unitConverter.convert(
                value = numericValue,
                fromUnit = pidDefinition.unit,
                toUnitSystem = unitSystem
            )
            
            val convertedUnit = unitConverter.getConvertedUnit(pidDefinition.unit, unitSystem)
            
            // Create new PID definition with converted unit
            val convertedPidDefinition = pidDefinition.copy(unit = convertedUnit)
            
            parsedData.copy(
                pidDefinition = convertedPidDefinition,
                parsedValue = convertedValue
            )
        } catch (e: Exception) {
            CrashHandler.handleException(e, "DataProcessor.convertUnits")
            parsedData
        }
    }
    
    /**
     * Calculate data quality score
     */
    private fun calculateDataQuality(parsedData: ParsedPidData): DataQuality {
        return try {
            val pidDefinition = parsedData.pidDefinition
            val numericValue = parsedData.numericValue
            
            when {
                pidDefinition == null -> DataQuality.UNKNOWN
                numericValue == null -> DataQuality.INVALID
                !parsedData.isValid -> DataQuality.INVALID
                isValueInRange(numericValue, pidDefinition) -> DataQuality.GOOD
                isValueNearRange(numericValue, pidDefinition) -> DataQuality.FAIR
                else -> DataQuality.POOR
            }
        } catch (e: Exception) {
            DataQuality.UNKNOWN
        }
    }
    
    /**
     * Check if value is within expected range
     */
    private fun isValueInRange(value: Double, pidDefinition: com.hurtec.obd2.diagnostics.obd.pid.PIDDefinition): Boolean {
        // Since PIDDefinition doesn't have minValue/maxValue, use PID-specific ranges
        val (minValue, maxValue) = getPidRange(pidDefinition.pid)
        return value >= minValue && value <= maxValue
    }
    
    /**
     * Check if value is near expected range (within 10% tolerance)
     */
    private fun isValueNearRange(value: Double, pidDefinition: com.hurtec.obd2.diagnostics.obd.pid.PIDDefinition): Boolean {
        val (minValue, maxValue) = getPidRange(pidDefinition.pid)
        val tolerance = (maxValue - minValue) * 0.1
        return value >= (minValue - tolerance) && value <= (maxValue + tolerance)
    }
    
    /**
     * Buffer data for streaming and analytics
     */
    private fun bufferData(data: ProcessedObdData) {
        val pidBuffer = dataBuffer.getOrPut(data.pid) { mutableListOf() }
        
        pidBuffer.add(data)
        
        // Maintain buffer size
        if (pidBuffer.size > maxBufferSize) {
            pidBuffer.removeAt(0)
        }
    }
    
    /**
     * Get buffered data for a specific PID
     */
    fun getBufferedData(pid: String): List<ProcessedObdData> {
        return dataBuffer[pid]?.toList() ?: emptyList()
    }
    
    /**
     * Get all buffered data
     */
    fun getAllBufferedData(): Map<String, List<ProcessedObdData>> {
        return dataBuffer.mapValues { it.value.toList() }
    }
    
    /**
     * Clear buffer for specific PID
     */
    fun clearBuffer(pid: String) {
        dataBuffer[pid]?.clear()
    }
    
    /**
     * Clear all buffers
     */
    fun clearAllBuffers() {
        dataBuffer.clear()
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
     * Get processing statistics
     */
    fun getProcessingStats(): ProcessingStats {
        return ProcessingStats(
            totalProcessed = totalProcessed,
            validData = validData,
            invalidData = invalidData,
            successRate = if (totalProcessed > 0) (validData.toFloat() / totalProcessed.toFloat()) * 100f else 0f,
            bufferedPids = dataBuffer.size,
            totalBufferedData = dataBuffer.values.sumOf { it.size }
        )
    }
    
    /**
     * Emit data error
     */
    private suspend fun emitError(dataError: DataError) {
        try {
            _dataErrors.emit(dataError)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "DataProcessor.emitError")
        }
    }
    
    /**
     * Reset processing statistics
     */
    fun resetStats() {
        totalProcessed = 0L
        validData = 0L
        invalidData = 0L
    }

    // ========== DATABASE INTEGRATION METHODS ==========

    /**
     * Set current session and vehicle for database storage
     */
    fun setCurrentSession(sessionId: String, vehicleId: Long) {
        currentSessionId = sessionId
        currentVehicleId = vehicleId
        CrashHandler.logInfo("DataProcessor session set: $sessionId for vehicle $vehicleId")
    }

    /**
     * Clear current session
     */
    fun clearCurrentSession() {
        currentSessionId = null
        currentVehicleId = null
        CrashHandler.logInfo("DataProcessor session cleared")
    }

    /**
     * Enable/disable database persistence
     */
    fun setPersistToDatabase(persist: Boolean) {
        persistDataToDatabase = persist
        CrashHandler.logInfo("Database persistence ${if (persist) "enabled" else "disabled"}")
    }

    /**
     * Get current session info
     */
    fun getCurrentSessionInfo(): Pair<String?, Long?> {
        return Pair(currentSessionId, currentVehicleId)
    }

    /**
     * Store batch data to database
     */
    suspend fun storeBatchToDatabase(
        vehicleId: Long,
        sessionId: String,
        processedDataList: List<ProcessedObdData>
    ): Result<List<Long>> {
        return try {
            // Convert ProcessedObdData to ObdDataEntity
            val obdDataEntities = processedDataList.map { processedData ->
                createObdDataEntity(processedData, vehicleId, sessionId)
            }

            val result = obdDataRepository.storeObdDataBatch(obdDataEntities)
            CrashHandler.logInfo("Stored batch of ${processedDataList.size} OBD data entries to database")
            result
        } catch (e: Exception) {
            CrashHandler.handleException(e, "DataProcessor.storeBatchToDatabase")
            Result.failure(e)
        }
    }

    /**
     * Start a new data recording session
     */
    fun startSession(sessionId: String, vehicleId: Long) {
        currentSessionId = sessionId
        currentVehicleId = vehicleId
        CrashHandler.logInfo("Started data recording session: $sessionId for vehicle: $vehicleId")
    }

    /**
     * Stop the current data recording session
     */
    fun stopSession() {
        currentSessionId = null
        currentVehicleId = null
        CrashHandler.logInfo("Stopped data recording session")
    }

    /**
     * Create ObdDataEntity from ProcessedObdData
     */
    private fun createObdDataEntity(
        processedData: ProcessedObdData,
        vehicleId: Long,
        sessionId: String
    ): ObdDataEntity {
        val pid = processedData.metadata.command.substring(2) // Remove "01" prefix
        val pidName = getPidName(pid)

        return ObdDataEntity(
            vehicleId = vehicleId,
            sessionId = sessionId,
            pid = pid,
            pidName = pidName,
            rawValue = processedData.rawValue, // Already a Double?
            processedValue = processedData.processedValue, // Already a Double?
            stringValue = processedData.stringValue,
            formattedValue = processedData.formattedValue,
            unit = processedData.unit,
            unitSystem = processedData.unitSystem.name,
            quality = processedData.quality.name,
            timestamp = processedData.timestamp,
            rawResponse = processedData.metadata.rawResponse,
            command = processedData.metadata.command,
            processingTimeMs = processedData.metadata.processingTime,
            dataBytes = processedData.metadata.dataBytes.joinToString(" ") { it.toString(16).uppercase().padStart(2, '0') },
            isValid = processedData.isValid
        )
    }

    /**
     * Get PID name from PID code
     */
    private fun getPidName(pid: String): String {
        return when (pid.uppercase()) {
            "0C" -> "Engine RPM"
            "0D" -> "Vehicle Speed"
            "05" -> "Engine Coolant Temperature"
            "04" -> "Calculated Engine Load"
            "11" -> "Throttle Position"
            "2F" -> "Fuel Tank Level Input"
            "0F" -> "Intake Air Temperature"
            "42" -> "Control Module Voltage"
            else -> "PID $pid"
        }
    }
}

/**
 * Unit system enumeration
 */
enum class UnitSystem {
    METRIC,     // Celsius, km/h, kPa, etc.
    IMPERIAL,   // Fahrenheit, mph, psi, etc.
    MIXED       // User-defined mix
}

/**
 * Data quality levels
 */
enum class DataQuality {
    GOOD,       // Data is within expected range
    FAIR,       // Data is near expected range
    POOR,       // Data is outside expected range
    INVALID,    // Data failed validation
    UNKNOWN     // Cannot determine quality
}

/**
 * Processed OBD data
 */
data class ProcessedObdData(
    val pid: String,
    val pidDefinition: com.hurtec.obd2.diagnostics.obd.pid.PIDDefinition?,
    val rawValue: Double?,
    val processedValue: Double?,
    val stringValue: String,
    val formattedValue: String,
    val unit: String,
    val unitSystem: UnitSystem,
    val timestamp: Long,
    val isValid: Boolean,
    val quality: DataQuality,
    val metadata: DataMetadata
)

/**
 * Data metadata
 */
data class DataMetadata(
    val rawResponse: String,
    val command: String,
    val processingTime: Long,
    val dataBytes: List<Int>
)

/**
 * Data error types
 */
data class DataError(
    val type: String,
    val message: String,
    val timestamp: Long
) {
    companion object {
        const val PARSING_FAILED = "PARSING_FAILED"
        const val VALIDATION_FAILED = "VALIDATION_FAILED"
        const val PROCESSING_FAILED = "PROCESSING_FAILED"
        const val CONVERSION_FAILED = "CONVERSION_FAILED"
    }
}

/**
 * Processing statistics
 */
data class ProcessingStats(
    val totalProcessed: Long,
    val validData: Long,
    val invalidData: Long,
    val successRate: Float,
    val bufferedPids: Int,
    val totalBufferedData: Int
)
