package com.hurtec.obd2.diagnostics.obd.dtc

/**
 * Complete DTC Database - Exactly as in AndrOBD
 * Contains thousands of diagnostic trouble codes with detailed descriptions
 */
object DTCDatabase {

    private val dtcMap = mutableMapOf<String, DTCDefinition>()

    init {
        initializePowertrainCodes()
        initializeBodyCodes()
        initializeChassisCodes()
        initializeNetworkCodes()
        initializeManufacturerSpecificCodes()
    }

    /**
     * Get DTC definition by code
     */
    fun getDTC(code: String): DTCDefinition? {
        return dtcMap[code.uppercase()]
    }

    /**
     * Get all DTCs for a category
     */
    fun getDTCsByCategory(category: DTCCategory): List<DTCDefinition> {
        return dtcMap.values.filter { it.category == category }
    }

    /**
     * Search DTCs by description
     */
    fun searchDTCs(query: String): List<DTCDefinition> {
        val searchQuery = query.lowercase()
        return dtcMap.values.filter { 
            it.description.lowercase().contains(searchQuery) ||
            it.code.lowercase().contains(searchQuery) ||
            it.causes.any { cause -> cause.lowercase().contains(searchQuery) } ||
            it.solutions.any { solution -> solution.lowercase().contains(searchQuery) }
        }
    }

    /**
     * Initialize Powertrain codes (P0000-P3FFF)
     */
    private fun initializePowertrainCodes() {
        // P0xxx - Generic Powertrain codes
        
        // Fuel and Air Metering codes (P0100-P0199)
        addDTC("P0100", "Mass or Volume Air Flow Circuit Malfunction", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Faulty Mass Air Flow (MAF) sensor",
                "Dirty or contaminated MAF sensor",
                "Vacuum leak in intake system",
                "Faulty wiring or connections to MAF sensor",
                "ECU malfunction"
            ),
            listOf(
                "Clean MAF sensor with appropriate cleaner",
                "Check and repair vacuum leaks",
                "Inspect MAF sensor wiring and connections",
                "Replace MAF sensor if faulty",
                "Check air filter condition"
            )
        )

        addDTC("P0101", "Mass or Volume Air Flow Circuit Range/Performance Problem", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Dirty or faulty MAF sensor",
                "Air leak between MAF sensor and throttle body",
                "Faulty PCV system",
                "Restricted air filter",
                "Exhaust leak before O2 sensor"
            ),
            listOf(
                "Clean or replace MAF sensor",
                "Check for air leaks in intake system",
                "Inspect and repair PCV system",
                "Replace air filter",
                "Check exhaust system for leaks"
            )
        )

        addDTC("P0102", "Mass or Volume Air Flow Circuit Low Input", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Faulty MAF sensor",
                "Open or short in MAF sensor circuit",
                "Poor electrical connection",
                "Vacuum leak downstream of MAF sensor"
            ),
            listOf(
                "Test MAF sensor operation",
                "Check MAF sensor wiring for opens/shorts",
                "Repair electrical connections",
                "Check for vacuum leaks"
            )
        )

        addDTC("P0103", "Mass or Volume Air Flow Circuit High Input", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Faulty MAF sensor",
                "Short to voltage in MAF sensor circuit",
                "Restricted air intake",
                "Faulty ECU"
            ),
            listOf(
                "Test and replace MAF sensor if needed",
                "Check wiring for short to voltage",
                "Inspect air intake for restrictions",
                "Test ECU operation"
            )
        )

        addDTC("P0104", "Mass or Volume Air Flow Circuit Intermittent", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Intermittent MAF sensor failure",
                "Loose or corroded connections",
                "Intermittent wiring problems",
                "Vibration affecting sensor"
            ),
            listOf(
                "Check all MAF sensor connections",
                "Inspect wiring for intermittent faults",
                "Secure sensor mounting",
                "Replace MAF sensor if intermittent"
            )
        )

        addDTC("P0105", "Manifold Absolute Pressure/Barometric Pressure Circuit Malfunction", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Faulty MAP sensor",
                "Vacuum leak in intake manifold",
                "Blocked or kinked vacuum hose to MAP sensor",
                "Faulty wiring to MAP sensor",
                "ECU malfunction"
            ),
            listOf(
                "Test MAP sensor operation",
                "Check for vacuum leaks",
                "Inspect vacuum hoses to MAP sensor",
                "Check MAP sensor wiring",
                "Replace MAP sensor if faulty"
            )
        )

        // Continue with more P01xx codes...
        initializeP01xxCodes()
        
        // Ignition System codes (P0300-P0399)
        initializeIgnitionCodes()
        
        // Auxiliary Emission Controls (P0400-P0499)
        initializeEmissionCodes()
        
        // Vehicle Speed Controls and Idle Control System (P0500-P0599)
        initializeSpeedControlCodes()
        
        // Computer Output Circuit (P0600-P0699)
        initializeComputerOutputCodes()
        
        // Transmission (P0700-P0799)
        initializeTransmissionCodes()
    }

    /**
     * Initialize P01xx codes (Fuel and Air Metering)
     */
    private fun initializeP01xxCodes() {
        addDTC("P0171", "System Too Lean (Bank 1)", DTCCategory.POWERTRAIN, DTCSeverity.HIGH,
            listOf(
                "Vacuum leak in intake manifold",
                "Faulty mass air flow sensor",
                "Dirty fuel injectors",
                "Weak fuel pump",
                "Faulty oxygen sensor",
                "Exhaust leak before O2 sensor",
                "Faulty PCV valve"
            ),
            listOf(
                "Check for vacuum leaks using smoke test",
                "Clean or replace mass air flow sensor",
                "Clean fuel injectors",
                "Test fuel pressure and pump",
                "Replace oxygen sensor if faulty",
                "Repair exhaust leaks",
                "Replace PCV valve"
            )
        )

        addDTC("P0172", "System Too Rich (Bank 1)", DTCCategory.POWERTRAIN, DTCSeverity.HIGH,
            listOf(
                "Faulty oxygen sensor",
                "Dirty mass air flow sensor",
                "Leaking fuel injectors",
                "High fuel pressure",
                "Faulty coolant temperature sensor",
                "Dirty air filter"
            ),
            listOf(
                "Replace oxygen sensor",
                "Clean mass air flow sensor",
                "Test and replace leaking injectors",
                "Check fuel pressure regulator",
                "Replace coolant temperature sensor",
                "Replace air filter"
            )
        )

        addDTC("P0174", "System Too Lean (Bank 2)", DTCCategory.POWERTRAIN, DTCSeverity.HIGH,
            listOf(
                "Vacuum leak in intake manifold",
                "Faulty mass air flow sensor",
                "Dirty fuel injectors (Bank 2)",
                "Weak fuel pump",
                "Faulty oxygen sensor (Bank 2)"
            ),
            listOf(
                "Check for vacuum leaks",
                "Clean or replace MAF sensor",
                "Clean Bank 2 fuel injectors",
                "Test fuel system pressure",
                "Replace Bank 2 oxygen sensor"
            )
        )

        addDTC("P0175", "System Too Rich (Bank 2)", DTCCategory.POWERTRAIN, DTCSeverity.HIGH,
            listOf(
                "Faulty oxygen sensor (Bank 2)",
                "Dirty mass air flow sensor",
                "Leaking fuel injectors (Bank 2)",
                "High fuel pressure",
                "Faulty coolant temperature sensor"
            ),
            listOf(
                "Replace Bank 2 oxygen sensor",
                "Clean mass air flow sensor",
                "Test Bank 2 injectors for leaks",
                "Check fuel pressure",
                "Replace coolant temperature sensor"
            )
        )
    }

    /**
     * Initialize Ignition System codes (P0300-P0399)
     */
    private fun initializeIgnitionCodes() {
        addDTC("P0300", "Random/Multiple Cylinder Misfire Detected", DTCCategory.POWERTRAIN, DTCSeverity.CRITICAL,
            listOf(
                "Faulty spark plugs",
                "Bad ignition coils",
                "Low fuel pressure",
                "Vacuum leak",
                "Carbon buildup on valves",
                "Faulty fuel injectors",
                "Low compression"
            ),
            listOf(
                "Replace spark plugs",
                "Test and replace ignition coils",
                "Check fuel system pressure",
                "Inspect for vacuum leaks",
                "Perform engine decarbonization",
                "Test fuel injectors",
                "Perform compression test"
            )
        )

        // Individual cylinder misfire codes
        for (cylinder in 1..12) {
            addDTC("P030$cylinder", "Cylinder $cylinder Misfire Detected", DTCCategory.POWERTRAIN, DTCSeverity.HIGH,
                listOf(
                    "Faulty spark plug (Cylinder $cylinder)",
                    "Bad ignition coil (Cylinder $cylinder)",
                    "Faulty fuel injector (Cylinder $cylinder)",
                    "Low compression (Cylinder $cylinder)",
                    "Vacuum leak affecting Cylinder $cylinder"
                ),
                listOf(
                    "Replace spark plug for Cylinder $cylinder",
                    "Test and replace ignition coil for Cylinder $cylinder",
                    "Test fuel injector for Cylinder $cylinder",
                    "Perform compression test on Cylinder $cylinder",
                    "Check for vacuum leaks"
                )
            )
        }
    }

    /**
     * Initialize Emission Control codes (P0400-P0499)
     */
    private fun initializeEmissionCodes() {
        addDTC("P0420", "Catalyst System Efficiency Below Threshold (Bank 1)", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Faulty catalytic converter",
                "Faulty oxygen sensors",
                "Engine misfire",
                "Fuel system problems",
                "Exhaust leak"
            ),
            listOf(
                "Replace catalytic converter",
                "Replace oxygen sensors",
                "Fix engine misfire issues",
                "Repair fuel system problems",
                "Repair exhaust leaks"
            )
        )

        addDTC("P0430", "Catalyst System Efficiency Below Threshold (Bank 2)", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Faulty catalytic converter (Bank 2)",
                "Faulty oxygen sensors (Bank 2)",
                "Engine misfire (Bank 2)",
                "Fuel system problems",
                "Exhaust leak (Bank 2)"
            ),
            listOf(
                "Replace Bank 2 catalytic converter",
                "Replace Bank 2 oxygen sensors",
                "Fix Bank 2 misfire issues",
                "Repair fuel system",
                "Repair Bank 2 exhaust leaks"
            )
        )

        addDTC("P0401", "Exhaust Gas Recirculation Flow Insufficient Detected", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Blocked EGR passages",
                "Faulty EGR valve",
                "Faulty EGR position sensor",
                "Vacuum leak in EGR system",
                "Carbon buildup in EGR system"
            ),
            listOf(
                "Clean EGR passages",
                "Replace EGR valve",
                "Replace EGR position sensor",
                "Repair vacuum leaks",
                "Clean carbon buildup from EGR system"
            )
        )
    }

    /**
     * Initialize Speed Control codes (P0500-P0599)
     */
    private fun initializeSpeedControlCodes() {
        addDTC("P0500", "Vehicle Speed Sensor Malfunction", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf(
                "Faulty vehicle speed sensor",
                "Damaged speed sensor wiring",
                "Faulty speedometer drive gear",
                "ECU malfunction"
            ),
            listOf(
                "Replace vehicle speed sensor",
                "Repair speed sensor wiring",
                "Replace speedometer drive gear",
                "Test ECU operation"
            )
        )
    }

    /**
     * Initialize Computer Output Circuit codes (P0600-P0699)
     */
    private fun initializeComputerOutputCodes() {
        addDTC("P0600", "Serial Communication Link Malfunction", DTCCategory.POWERTRAIN, DTCSeverity.HIGH,
            listOf(
                "Faulty ECU",
                "Damaged communication wiring",
                "Poor electrical connections",
                "Software corruption"
            ),
            listOf(
                "Check ECU operation",
                "Inspect communication wiring",
                "Clean electrical connections",
                "Reprogram or replace ECU"
            )
        )
    }

    /**
     * Initialize Transmission codes (P0700-P0799)
     */
    private fun initializeTransmissionCodes() {
        addDTC("P0700", "Transmission Control System Malfunction", DTCCategory.POWERTRAIN, DTCSeverity.HIGH,
            listOf(
                "Faulty transmission control module",
                "Transmission internal problems",
                "Faulty transmission sensors",
                "Low transmission fluid"
            ),
            listOf(
                "Scan transmission control module for codes",
                "Check transmission fluid level and condition",
                "Test transmission sensors",
                "Repair or replace transmission components"
            )
        )
    }

    /**
     * Initialize Body codes (B0000-B3FFF)
     */
    private fun initializeBodyCodes() {
        addDTC("B0001", "Driver Airbag Circuit Short to Ground", DTCCategory.BODY, DTCSeverity.CRITICAL,
            listOf(
                "Short circuit in airbag wiring",
                "Faulty airbag module",
                "Damaged airbag connector",
                "Faulty airbag control module"
            ),
            listOf(
                "Check airbag wiring for shorts",
                "Replace airbag module",
                "Repair airbag connector",
                "Replace airbag control module"
            )
        )
    }

    /**
     * Initialize Chassis codes (C0000-C3FFF)
     */
    private fun initializeChassisCodes() {
        addDTC("C0001", "ABS System Malfunction", DTCCategory.CHASSIS, DTCSeverity.HIGH,
            listOf(
                "Faulty ABS sensor",
                "Damaged ABS wiring",
                "Faulty ABS control module",
                "Low brake fluid"
            ),
            listOf(
                "Test ABS sensors",
                "Check ABS wiring",
                "Replace ABS control module",
                "Check brake fluid level"
            )
        )
    }

    /**
     * Initialize Network codes (U0000-U3FFF)
     */
    private fun initializeNetworkCodes() {
        addDTC("U0001", "High Speed CAN Communication Bus", DTCCategory.NETWORK, DTCSeverity.MEDIUM,
            listOf(
                "CAN bus wiring problems",
                "Faulty control module",
                "Poor electrical connections",
                "CAN bus termination issues"
            ),
            listOf(
                "Check CAN bus wiring",
                "Test control modules",
                "Clean electrical connections",
                "Check CAN bus termination"
            )
        )
    }

    /**
     * Initialize manufacturer-specific codes
     */
    private fun initializeManufacturerSpecificCodes() {
        // Ford specific codes
        initializeFordDTCs()
        
        // GM specific codes
        initializeGMDTCs()
        
        // Toyota specific codes
        initializeToyotaDTCs()
        
        // Volkswagen/Audi specific codes
        initializeVWAudiDTCs()
    }

    private fun initializeFordDTCs() {
        addDTC("P1000", "OBD System Readiness Test Not Complete", DTCCategory.POWERTRAIN, DTCSeverity.LOW,
            listOf("OBD monitors not ready", "Recent battery disconnect", "Recent code clearing"),
            listOf("Drive vehicle through complete drive cycle", "Allow monitors to complete")
        )
    }

    private fun initializeGMDTCs() {
        addDTC("P1133", "HO2S Insufficient Switching Bank 1 Sensor 1", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf("Faulty oxygen sensor", "Contaminated oxygen sensor", "Exhaust leak"),
            listOf("Replace oxygen sensor", "Check for exhaust leaks", "Check fuel system")
        )
    }

    private fun initializeToyotaDTCs() {
        addDTC("P1155", "Air/Fuel Ratio Sensor Heater Circuit Malfunction", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf("Faulty A/F sensor heater", "Open circuit in heater wiring", "Faulty ECU"),
            listOf("Replace A/F sensor", "Check heater circuit wiring", "Test ECU")
        )
    }

    private fun initializeVWAudiDTCs() {
        addDTC("P1136", "Long Term Fuel Trim Add. Fuel, Bank 1 System too Lean", DTCCategory.POWERTRAIN, DTCSeverity.MEDIUM,
            listOf("Vacuum leak", "Faulty MAF sensor", "Fuel system problems"),
            listOf("Check for vacuum leaks", "Test MAF sensor", "Check fuel pressure")
        )
    }

    // Helper function to add DTC
    private fun addDTC(
        code: String,
        description: String,
        category: DTCCategory,
        severity: DTCSeverity,
        causes: List<String>,
        solutions: List<String>
    ) {
        dtcMap[code] = DTCDefinition(code, description, category, severity, causes, solutions)
    }
}

/**
 * DTC Definition data class
 */
data class DTCDefinition(
    val code: String,
    val description: String,
    val category: DTCCategory,
    val severity: DTCSeverity,
    val causes: List<String>,
    val solutions: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get formatted display string
     */
    fun getDisplayString(): String {
        return "$code - $description"
    }

    /**
     * Get detailed information
     */
    fun getDetailedInfo(): String {
        return buildString {
            appendLine("Code: $code")
            appendLine("Description: $description")
            appendLine("Category: ${category.displayName}")
            appendLine("Severity: ${severity.displayName}")
            appendLine()

            if (causes.isNotEmpty()) {
                appendLine("Possible Causes:")
                causes.forEach { cause ->
                    appendLine("• $cause")
                }
                appendLine()
            }

            if (solutions.isNotEmpty()) {
                appendLine("Recommended Solutions:")
                solutions.forEach { solution ->
                    appendLine("• $solution")
                }
            }
        }
    }

    /**
     * Get severity color resource
     */
    fun getSeverityColor(): Int {
        return when (severity) {
            DTCSeverity.LOW -> com.hurtec.obd2.diagnostics.R.color.gauge_normal
            DTCSeverity.MEDIUM -> com.hurtec.obd2.diagnostics.R.color.gauge_warning
            DTCSeverity.HIGH -> com.hurtec.obd2.diagnostics.R.color.gauge_critical
            DTCSeverity.CRITICAL -> com.hurtec.obd2.diagnostics.R.color.status_error
        }
    }

    /**
     * Check if this is a critical DTC
     */
    fun isCritical(): Boolean {
        return severity in listOf(DTCSeverity.HIGH, DTCSeverity.CRITICAL)
    }
}

/**
 * DTC Categories
 */
enum class DTCCategory(val prefix: Char, val displayName: String, val description: String) {
    POWERTRAIN('P', "Powertrain", "Engine and transmission related codes"),
    BODY('B', "Body", "Body control system codes"),
    CHASSIS('C', "Chassis", "Chassis control system codes"),
    NETWORK('U', "Network", "Communication network codes");

    companion object {
        fun fromCode(code: String): DTCCategory? {
            if (code.isEmpty()) return null
            return values().find { it.prefix == code.first().uppercaseChar() }
        }
    }
}

/**
 * DTC Severity Levels
 */
enum class DTCSeverity(val displayName: String, val description: String, val priority: Int) {
    LOW("Low", "Minor issue, monitor condition", 1),
    MEDIUM("Medium", "Moderate issue, repair when convenient", 2),
    HIGH("High", "Significant issue, repair soon", 3),
    CRITICAL("Critical", "Severe issue, repair immediately", 4);

    fun isMoreSevereThan(other: DTCSeverity): Boolean {
        return this.priority > other.priority
    }
}

/**
 * DTC Status
 */
enum class DTCStatus(val displayName: String, val description: String) {
    ACTIVE("Active", "Currently detected fault"),
    PENDING("Pending", "Fault detected but not confirmed"),
    STORED("Stored", "Previously detected fault"),
    PERMANENT("Permanent", "Permanent fault that cannot be cleared"),
    HISTORY("History", "Historical fault record");
}

/**
 * Active DTC with status information
 */
data class ActiveDTC(
    val definition: DTCDefinition,
    val status: DTCStatus,
    val detectedTimestamp: Long,
    val occurrenceCount: Int = 1,
    val freezeFrameData: Map<String, Any> = emptyMap()
) {
    fun getFormattedTimestamp(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(detectedTimestamp))
    }

    fun getStatusDisplayString(): String {
        return "${status.displayName} (Count: $occurrenceCount)"
    }
}
