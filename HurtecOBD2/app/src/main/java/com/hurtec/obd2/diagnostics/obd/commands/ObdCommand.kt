package com.hurtec.obd2.diagnostics.obd.commands

/**
 * Enhanced OBD-II command with advanced features
 */
data class ObdCommand(
    val command: String,
    val description: String,
    val pid: String? = null,
    val expectedResponseLength: Int? = null,
    val timeoutMs: Long = 5000,
    val retryCount: Int = 3,
    val commandType: CommandType = CommandType.DATA,
    val responseParser: ((String) -> Any?)? = null,
    val validator: ((String) -> Boolean)? = null
) {
    /**
     * Check if this is a PID command
     */
    val isPidCommand: Boolean
        get() = pid != null && command.startsWith("01")
    
    /**
     * Check if this is an AT command
     */
    val isAtCommand: Boolean
        get() = command.startsWith("AT")
    
    /**
     * Get the mode from the command
     */
    val mode: String?
        get() = if (command.length >= 2) command.substring(0, 2) else null
    
    /**
     * Get the PID from the command
     */
    val pidFromCommand: String?
        get() = if (command.length >= 4) command.substring(2, 4) else null
}

/**
 * Types of OBD commands
 */
enum class CommandType {
    INITIALIZATION,  // AT commands for setup
    DATA,           // Data retrieval commands
    DIAGNOSTIC,     // DTC related commands
    CONTROL,        // Control commands
    CUSTOM          // Custom commands
}
