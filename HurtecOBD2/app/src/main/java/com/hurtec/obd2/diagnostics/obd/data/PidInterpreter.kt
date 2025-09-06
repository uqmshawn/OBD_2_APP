package com.hurtec.obd2.diagnostics.obd.data

import com.hurtec.obd2.diagnostics.obd.pid.PIDDatabase
import com.hurtec.obd2.diagnostics.obd.pid.PIDDefinition
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Real-time PID interpretation and parsing engine
 * Converts raw OBD responses into meaningful data
 */
@Singleton
class PidInterpreter @Inject constructor() {
    
    /**
     * Parse a raw OBD response into structured data
     */
    fun parseResponse(rawResponse: String, command: String): ParsedPidData? {
        return try {
            val cleanResponse = cleanResponse(rawResponse)
            
            if (!isValidResponse(cleanResponse)) {
                CrashHandler.logWarning("Invalid OBD response: $rawResponse")
                return null
            }
            
            // Extract mode and PID from response
            val mode = extractMode(cleanResponse)
            val pid = extractPid(cleanResponse)
            val dataBytes = extractDataBytes(cleanResponse)
            
            if (mode == null || pid == null || dataBytes.isEmpty()) {
                CrashHandler.logWarning("Could not extract mode/PID from response: $cleanResponse")
                return null
            }
            
            // Get PID definition
            val pidDefinition = PIDDatabase.getPID(pid)
            if (pidDefinition == null) {
                CrashHandler.logWarning("Unknown PID: $pid")
                return createUnknownPidData(pid, dataBytes, rawResponse)
            }
            
            // Parse the data using the PID definition
            val parsedValue = parseDataBytes(dataBytes, pidDefinition)
            
            ParsedPidData(
                pid = pid,
                pidDefinition = pidDefinition,
                rawResponse = rawResponse,
                cleanResponse = cleanResponse,
                dataBytes = dataBytes,
                parsedValue = parsedValue,
                timestamp = System.currentTimeMillis(),
                isValid = parsedValue != null
            )
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PidInterpreter.parseResponse")
            null
        }
    }
    
    /**
     * Parse multiple PID responses from a single command
     */
    fun parseMultipleResponses(rawResponse: String, command: String): List<ParsedPidData> {
        return try {
            val responses = rawResponse.split("\r", "\n")
                .filter { it.isNotBlank() }
                .mapNotNull { parseResponse(it, command) }
            
            CrashHandler.logInfo("Parsed ${responses.size} PID responses from multi-response")
            responses
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PidInterpreter.parseMultipleResponses")
            emptyList()
        }
    }
    
    /**
     * Clean the raw response by removing unwanted characters
     */
    private fun cleanResponse(rawResponse: String): String {
        return rawResponse
            .replace(">", "") // Remove prompt
            .replace("\r", "") // Remove carriage returns
            .replace("\n", "") // Remove newlines
            .replace(" ", "") // Remove spaces
            .trim()
            .uppercase()
    }
    
    /**
     * Check if the response is valid
     */
    private fun isValidResponse(response: String): Boolean {
        return when {
            response.isEmpty() -> false
            response.contains("NODATA") -> false
            response.contains("ERROR") -> false
            response.contains("?") -> false
            response.contains("UNABLETOCONNECT") -> false
            response.contains("BUSBUSY") -> false
            response.length < 4 -> false // Minimum response length
            else -> true
        }
    }
    
    /**
     * Extract mode from response (first byte after 4)
     */
    private fun extractMode(response: String): String? {
        return if (response.length >= 2) {
            response.substring(0, 2)
        } else null
    }
    
    /**
     * Extract PID from response (second byte after 4)
     */
    private fun extractPid(response: String): String? {
        return if (response.length >= 4) {
            response.substring(2, 4)
        } else null
    }
    
    /**
     * Extract data bytes from response (everything after mode and PID)
     */
    private fun extractDataBytes(response: String): List<Int> {
        return try {
            if (response.length <= 4) return emptyList()
            
            val dataHex = response.substring(4)
            val bytes = mutableListOf<Int>()
            
            for (i in dataHex.indices step 2) {
                if (i + 1 < dataHex.length) {
                    val byteHex = dataHex.substring(i, i + 2)
                    bytes.add(Integer.parseInt(byteHex, 16))
                }
            }
            
            bytes
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PidInterpreter.extractDataBytes")
            emptyList()
        }
    }
    
    /**
     * Parse data bytes using PID definition
     */
    private fun parseDataBytes(dataBytes: List<Int>, pidDefinition: PIDDefinition): Any? {
        return try {
            // Use generic parsing since we don't have dataType in existing PIDDefinition
            parseGeneric(dataBytes, pidDefinition)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PidInterpreter.parseDataBytes")
            null
        }
    }
    
    /**
     * Parse unsigned 8-bit integer
     */
    private fun parseUInt8(dataBytes: List<Int>, pidDefinition: PIDDefinition): Double? {
        if (dataBytes.isEmpty()) return null
        
        val rawValue = dataBytes[0].toDouble()
        return applyFormula(rawValue, pidDefinition, dataBytes)
    }
    
    /**
     * Parse unsigned 16-bit integer
     */
    private fun parseUInt16(dataBytes: List<Int>, pidDefinition: PIDDefinition): Double? {
        if (dataBytes.size < 2) return null
        
        val rawValue = (dataBytes[0] * 256 + dataBytes[1]).toDouble()
        return applyFormula(rawValue, pidDefinition, dataBytes)
    }
    
    /**
     * Parse signed 8-bit integer
     */
    private fun parseInt8(dataBytes: List<Int>, pidDefinition: PIDDefinition): Double? {
        if (dataBytes.isEmpty()) return null
        
        val rawValue = if (dataBytes[0] > 127) {
            (dataBytes[0] - 256).toDouble()
        } else {
            dataBytes[0].toDouble()
        }
        return applyFormula(rawValue, pidDefinition, dataBytes)
    }
    
    /**
     * Parse signed 16-bit integer
     */
    private fun parseInt16(dataBytes: List<Int>, pidDefinition: PIDDefinition): Double? {
        if (dataBytes.size < 2) return null
        
        val rawValue = (dataBytes[0] * 256 + dataBytes[1])
        val signedValue = if (rawValue > 32767) {
            (rawValue - 65536).toDouble()
        } else {
            rawValue.toDouble()
        }
        return applyFormula(signedValue, pidDefinition, dataBytes)
    }
    
    /**
     * Parse floating point value
     */
    private fun parseFloat(dataBytes: List<Int>, pidDefinition: PIDDefinition): Double? {
        if (dataBytes.size < 4) return null
        
        // IEEE 754 float parsing would go here
        // For now, use generic parsing
        return parseGeneric(dataBytes, pidDefinition)
    }
    
    /**
     * Parse bitmap data
     */
    private fun parseBitmap(dataBytes: List<Int>, pidDefinition: PIDDefinition): Map<String, Boolean>? {
        if (dataBytes.isEmpty()) return null
        
        val bitmap = mutableMapOf<String, Boolean>()
        
        dataBytes.forEachIndexed { byteIndex, byte ->
            for (bitIndex in 0..7) {
                val bitValue = (byte shr bitIndex) and 1 == 1
                val bitName = "Byte${byteIndex}_Bit$bitIndex"
                bitmap[bitName] = bitValue
            }
        }
        
        return bitmap
    }
    
    /**
     * Parse string data
     */
    private fun parseString(dataBytes: List<Int>, pidDefinition: PIDDefinition): String? {
        return try {
            dataBytes.map { it.toChar() }.joinToString("")
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generic parsing using formula
     */
    private fun parseGeneric(dataBytes: List<Int>, pidDefinition: PIDDefinition): Double? {
        if (dataBytes.isEmpty()) return null
        
        // Use the first available bytes based on expected length
        val rawValue = when (dataBytes.size) {
            1 -> dataBytes[0].toDouble()
            2 -> (dataBytes[0] * 256 + dataBytes[1]).toDouble()
            3 -> (dataBytes[0] * 65536 + dataBytes[1] * 256 + dataBytes[2]).toDouble()
            4 -> (dataBytes[0] * 16777216 + dataBytes[1] * 65536 + dataBytes[2] * 256 + dataBytes[3]).toDouble()
            else -> dataBytes[0].toDouble() // Default to first byte
        }
        
        return applyFormula(rawValue, pidDefinition, dataBytes)
    }
    
    /**
     * Apply the formula from PID definition
     */
    private fun applyFormula(rawValue: Double, pidDefinition: PIDDefinition, dataBytes: List<Int>): Double? {
        return try {
            // Use the PIDDefinition's calculateValue method directly
            val pidValue = pidDefinition.calculateValue(dataBytes)
            when (pidValue) {
                is com.hurtec.obd2.diagnostics.obd.pid.PIDValue.NumericValue -> pidValue.value
                else -> rawValue // Fallback to raw value
            }
        } catch (e: Exception) {
            CrashHandler.handleException(e, "PidInterpreter.applyFormula")
            rawValue // Return raw value if formula fails
        }
    }
    
    /**
     * Evaluate mathematical formula
     */
    private fun evaluateFormula(formula: String, rawValue: Double): Double {
        return try {
            // Simple formula evaluation for common patterns
            when {
                formula.contains("*") && formula.contains("/") -> {
                    // Handle multiplication and division
                    val parts = formula.split("*", "/")
                    if (parts.size >= 3) {
                        val multiplier = parts[1].toDoubleOrNull() ?: 1.0
                        val divisor = parts[2].toDoubleOrNull() ?: 1.0
                        rawValue * multiplier / divisor
                    } else rawValue
                }
                formula.contains("*") -> {
                    val parts = formula.split("*")
                    if (parts.size >= 2) {
                        val multiplier = parts[1].toDoubleOrNull() ?: 1.0
                        rawValue * multiplier
                    } else rawValue
                }
                formula.contains("/") -> {
                    val parts = formula.split("/")
                    if (parts.size >= 2) {
                        val divisor = parts[1].toDoubleOrNull() ?: 1.0
                        rawValue / divisor
                    } else rawValue
                }
                formula.contains("+") -> {
                    val parts = formula.split("+")
                    if (parts.size >= 2) {
                        val addend = parts[1].toDoubleOrNull() ?: 0.0
                        rawValue + addend
                    } else rawValue
                }
                formula.contains("-") -> {
                    val parts = formula.split("-")
                    if (parts.size >= 2) {
                        val subtrahend = parts[1].toDoubleOrNull() ?: 0.0
                        rawValue - subtrahend
                    } else rawValue
                }
                else -> rawValue
            }
        } catch (e: Exception) {
            rawValue
        }
    }
    
    /**
     * Create data for unknown PID
     */
    private fun createUnknownPidData(pid: String, dataBytes: List<Int>, rawResponse: String): ParsedPidData {
        return ParsedPidData(
            pid = pid,
            pidDefinition = null,
            rawResponse = rawResponse,
            cleanResponse = rawResponse,
            dataBytes = dataBytes,
            parsedValue = dataBytes.firstOrNull(),
            timestamp = System.currentTimeMillis(),
            isValid = false
        )
    }
}

/**
 * Parsed PID data result
 */
data class ParsedPidData(
    val pid: String,
    val pidDefinition: PIDDefinition?,
    val rawResponse: String,
    val cleanResponse: String,
    val dataBytes: List<Int>,
    val parsedValue: Any?,
    val timestamp: Long,
    val isValid: Boolean
) {
    /**
     * Get the value as a double (for numeric data)
     */
    val numericValue: Double?
        get() = when (parsedValue) {
            is Double -> parsedValue
            is Int -> parsedValue.toDouble()
            is Float -> parsedValue.toDouble()
            is Long -> parsedValue.toDouble()
            else -> null
        }
    
    /**
     * Get the value as a string
     */
    val stringValue: String
        get() = parsedValue?.toString() ?: "N/A"
    
    /**
     * Get formatted value with units
     */
    val formattedValue: String
        get() = if (pidDefinition != null && numericValue != null) {
            "${String.format("%.2f", numericValue)} ${pidDefinition.unit}"
        } else {
            stringValue
        }
}
