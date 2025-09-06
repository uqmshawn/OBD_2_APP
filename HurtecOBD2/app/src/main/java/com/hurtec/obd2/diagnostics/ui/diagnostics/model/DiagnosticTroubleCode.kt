package com.hurtec.obd2.diagnostics.ui.diagnostics.model

/**
 * Data class representing a Diagnostic Trouble Code (DTC)
 */
data class DiagnosticTroubleCode(
    val code: String,
    val description: String,
    val status: String, // "Confirmed", "Pending", "Permanent"
    val severity: String, // "Low", "Medium", "High", "Critical"
    val causes: List<String> = emptyList(),
    val solutions: List<String> = emptyList(),
    val freezeFrameData: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    
    /**
     * Get the DTC category based on the first character
     */
    fun getCategory(): String {
        return when (code.firstOrNull()) {
            'P' -> "Powertrain"
            'B' -> "Body"
            'C' -> "Chassis"
            'U' -> "Network/Communication"
            else -> "Unknown"
        }
    }
    
    /**
     * Get the severity color resource
     */
    fun getSeverityColor(): Int {
        return when (severity.lowercase()) {
            "low" -> com.hurtec.obd2.diagnostics.R.color.gauge_normal
            "medium" -> com.hurtec.obd2.diagnostics.R.color.gauge_warning
            "high" -> com.hurtec.obd2.diagnostics.R.color.gauge_critical
            "critical" -> com.hurtec.obd2.diagnostics.R.color.status_error
            else -> com.hurtec.obd2.diagnostics.R.color.hurtec_secondary
        }
    }
    
    /**
     * Get formatted timestamp
     */
    fun getFormattedTimestamp(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }
    
    /**
     * Check if this is a critical DTC that requires immediate attention
     */
    fun isCritical(): Boolean {
        return severity.lowercase() in listOf("high", "critical") ||
               status.lowercase() == "confirmed"
    }
    
    /**
     * Get detailed information about the DTC
     */
    fun getDetailedInfo(): String {
        return buildString {
            appendLine("Code: $code")
            appendLine("Description: $description")
            appendLine("Category: ${getCategory()}")
            appendLine("Status: $status")
            appendLine("Severity: $severity")
            appendLine("Detected: ${getFormattedTimestamp()}")
            
            if (causes.isNotEmpty()) {
                appendLine("\nPossible Causes:")
                causes.forEach { cause ->
                    appendLine("• $cause")
                }
            }
            
            if (solutions.isNotEmpty()) {
                appendLine("\nRecommended Solutions:")
                solutions.forEach { solution ->
                    appendLine("• $solution")
                }
            }
            
            if (freezeFrameData.isNotEmpty()) {
                appendLine("\nFreeze Frame Data:")
                freezeFrameData.forEach { (key, value) ->
                    appendLine("• $key: $value")
                }
            }
        }
    }
}

/**
 * Common DTC definitions and descriptions
 */
object DtcDatabase {
    
    private val commonDtcs = mapOf(
        "P0171" to DiagnosticTroubleCode(
            code = "P0171",
            description = "System Too Lean (Bank 1)",
            status = "Confirmed",
            severity = "Medium",
            causes = listOf(
                "Vacuum leak in intake manifold",
                "Faulty mass air flow sensor",
                "Dirty fuel injectors",
                "Weak fuel pump",
                "Faulty oxygen sensor"
            ),
            solutions = listOf(
                "Check for vacuum leaks using smoke test",
                "Clean or replace mass air flow sensor",
                "Clean fuel injectors",
                "Test fuel pressure and pump",
                "Replace oxygen sensor if faulty"
            )
        ),
        "P0300" to DiagnosticTroubleCode(
            code = "P0300",
            description = "Random/Multiple Cylinder Misfire Detected",
            status = "Confirmed",
            severity = "High",
            causes = listOf(
                "Faulty spark plugs",
                "Bad ignition coils",
                "Low fuel pressure",
                "Vacuum leak",
                "Carbon buildup on valves"
            ),
            solutions = listOf(
                "Replace spark plugs",
                "Test and replace ignition coils",
                "Check fuel system pressure",
                "Inspect for vacuum leaks",
                "Perform engine decarbonization"
            )
        ),
        "P0420" to DiagnosticTroubleCode(
            code = "P0420",
            description = "Catalyst System Efficiency Below Threshold (Bank 1)",
            status = "Confirmed",
            severity = "Medium",
            causes = listOf(
                "Faulty catalytic converter",
                "Faulty oxygen sensors",
                "Engine misfire",
                "Fuel system problems"
            ),
            solutions = listOf(
                "Replace catalytic converter",
                "Replace oxygen sensors",
                "Fix engine misfire issues",
                "Clean fuel system"
            )
        )
    )
    
    /**
     * Get DTC information from database
     */
    fun getDtcInfo(code: String): DiagnosticTroubleCode? {
        return commonDtcs[code]
    }
    
    /**
     * Get all known DTCs
     */
    fun getAllKnownDtcs(): List<DiagnosticTroubleCode> {
        return commonDtcs.values.toList()
    }
}
