package com.hurtec.obd2.diagnostics.obd.commands

import com.hurtec.obd2.diagnostics.utils.CrashHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced command queue management with prioritization for OBD-II commands
 * Handles command scheduling, execution, retries, and response processing
 */
@Singleton
class CommandQueue @Inject constructor() {
    
    // Command queue with priority ordering
    private val commandQueue = PriorityBlockingQueue<QueuedCommand>()
    
    // Command execution state
    private val isProcessing = AtomicBoolean(false)
    private val commandIdCounter = AtomicLong(0)
    
    // Coroutine scope for command processing
    private val queueScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Response channels
    private val responseChannel = Channel<CommandResponse>(Channel.UNLIMITED)
    val responses: Flow<CommandResponse> = responseChannel.receiveAsFlow()
    
    // Queue state monitoring
    private val _queueState = MutableStateFlow(QueueState())
    val queueState: Flow<QueueState> = _queueState.asStateFlow()
    
    // Active commands tracking
    private val activeCommands = mutableMapOf<Long, QueuedCommand>()
    
    // Command execution settings
    private val maxRetries = 3
    private val commandTimeoutMs = 5000L
    private val retryDelayMs = 1000L
    
    init {
        startQueueProcessor()
    }
    
    /**
     * Add a command to the queue with specified priority
     */
    fun enqueueCommand(
        command: ObdCommand,
        priority: CommandPriority = CommandPriority.NORMAL,
        callback: ((CommandResponse) -> Unit)? = null
    ): Long {
        val commandId = commandIdCounter.incrementAndGet()
        val queuedCommand = QueuedCommand(
            id = commandId,
            command = command,
            priority = priority,
            enqueuedAt = System.currentTimeMillis(),
            callback = callback
        )
        
        commandQueue.offer(queuedCommand)
        updateQueueState()
        
        CrashHandler.logInfo("Command enqueued: ${command.command} (ID: $commandId, Priority: $priority)")
        return commandId
    }
    
    /**
     * Add multiple commands as a batch
     */
    fun enqueueBatch(
        commands: List<ObdCommand>,
        priority: CommandPriority = CommandPriority.NORMAL
    ): List<Long> {
        return commands.map { command ->
            enqueueCommand(command, priority)
        }
    }
    
    /**
     * Cancel a specific command
     */
    fun cancelCommand(commandId: Long): Boolean {
        // Remove from queue if not yet processed
        val removed = commandQueue.removeIf { it.id == commandId }
        
        // Cancel if currently active
        activeCommands[commandId]?.let { activeCommand ->
            activeCommand.isCancelled = true
            activeCommands.remove(commandId)
        }
        
        if (removed) {
            updateQueueState()
            CrashHandler.logInfo("Command cancelled: $commandId")
        }
        
        return removed
    }
    
    /**
     * Clear all pending commands
     */
    fun clearQueue() {
        val clearedCount = commandQueue.size
        commandQueue.clear()
        activeCommands.clear()
        updateQueueState()
        CrashHandler.logInfo("Queue cleared: $clearedCount commands removed")
    }
    
    /**
     * Get queue statistics
     */
    fun getQueueStats(): QueueStats {
        return QueueStats(
            pendingCommands = commandQueue.size,
            activeCommands = activeCommands.size,
            totalProcessed = _queueState.value.totalProcessed,
            successfulCommands = _queueState.value.successfulCommands,
            failedCommands = _queueState.value.failedCommands
        )
    }
    
    /**
     * Start the command queue processor
     */
    private fun startQueueProcessor() {
        queueScope.launch {
            while (isActive) {
                try {
                    processNextCommand()
                } catch (e: Exception) {
                    CrashHandler.handleException(e, "CommandQueue.processNextCommand")
                    delay(1000) // Wait before retrying
                }
            }
        }
    }
    
    /**
     * Process the next command in the queue
     */
    private suspend fun processNextCommand() {
        if (isProcessing.get()) {
            delay(100) // Wait if already processing
            return
        }
        
        val queuedCommand = commandQueue.poll() ?: run {
            delay(100) // No commands, wait a bit
            return
        }
        
        if (queuedCommand.isCancelled) {
            return // Skip cancelled commands
        }
        
        isProcessing.set(true)
        activeCommands[queuedCommand.id] = queuedCommand
        
        try {
            executeCommand(queuedCommand)
        } finally {
            activeCommands.remove(queuedCommand.id)
            isProcessing.set(false)
            updateQueueState()
        }
    }
    
    /**
     * Execute a single command with retry logic
     */
    private suspend fun executeCommand(queuedCommand: QueuedCommand) {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt <= maxRetries && !queuedCommand.isCancelled) {
            attempt++
            
            try {
                CrashHandler.logInfo("Executing command: ${queuedCommand.command.command} (Attempt $attempt)")
                
                val response = withTimeout(commandTimeoutMs) {
                    // This would be replaced with actual OBD communication
                    simulateCommandExecution(queuedCommand.command)
                }
                
                val commandResponse = CommandResponse(
                    commandId = queuedCommand.id,
                    command = queuedCommand.command,
                    response = response,
                    success = true,
                    executionTime = System.currentTimeMillis() - queuedCommand.enqueuedAt,
                    attempt = attempt
                )
                
                // Send response
                responseChannel.trySend(commandResponse)
                queuedCommand.callback?.invoke(commandResponse)
                
                // Update statistics
                _queueState.value = _queueState.value.copy(
                    totalProcessed = _queueState.value.totalProcessed + 1,
                    successfulCommands = _queueState.value.successfulCommands + 1
                )
                
                CrashHandler.logInfo("Command executed successfully: ${queuedCommand.command.command}")
                return
                
            } catch (e: TimeoutCancellationException) {
                lastException = Exception("Command timeout after ${commandTimeoutMs}ms")
                CrashHandler.logWarning("Command timeout: ${queuedCommand.command.command} (Attempt $attempt)")
            } catch (e: Exception) {
                lastException = e
                CrashHandler.handleException(e, "CommandQueue.executeCommand (Attempt $attempt)")
            }
            
            // Wait before retry (except on last attempt)
            if (attempt <= maxRetries) {
                delay(retryDelayMs * attempt) // Exponential backoff
            }
        }
        
        // All attempts failed
        val commandResponse = CommandResponse(
            commandId = queuedCommand.id,
            command = queuedCommand.command,
            response = null,
            success = false,
            error = lastException?.message ?: "Unknown error",
            executionTime = System.currentTimeMillis() - queuedCommand.enqueuedAt,
            attempt = attempt - 1
        )
        
        responseChannel.trySend(commandResponse)
        queuedCommand.callback?.invoke(commandResponse)
        
        // Update statistics
        _queueState.value = _queueState.value.copy(
            totalProcessed = _queueState.value.totalProcessed + 1,
            failedCommands = _queueState.value.failedCommands + 1
        )
        
        CrashHandler.logError("Command failed after $maxRetries attempts: ${queuedCommand.command.command}")
    }
    
    /**
     * Simulate command execution (replace with real OBD communication)
     */
    private suspend fun simulateCommandExecution(command: ObdCommand): String {
        delay(100) // Simulate communication delay
        
        return when (command.command) {
            "ATZ" -> "ELM327 v1.5"
            "ATE0" -> "OK"
            "ATL0" -> "OK"
            "ATS0" -> "OK"
            "ATH1" -> "OK"
            "ATSP0" -> "OK"
            "0100" -> "41 00 BE 3E B8 11" // Supported PIDs
            "010C" -> "41 0C 1A F8" // RPM
            "010D" -> "41 0D 3C" // Speed
            "0105" -> "41 05 5F" // Coolant temp
            else -> "NO DATA"
        }
    }
    
    /**
     * Update queue state for monitoring
     */
    private fun updateQueueState() {
        _queueState.value = _queueState.value.copy(
            pendingCommands = commandQueue.size,
            activeCommands = activeCommands.size
        )
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            queueScope.cancel()
            commandQueue.clear()
            activeCommands.clear()
            responseChannel.close()
            CrashHandler.logInfo("CommandQueue cleaned up")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "CommandQueue.cleanup")
        }
    }
}

/**
 * Command priority levels
 */
enum class CommandPriority(val value: Int) {
    CRITICAL(0),    // Emergency commands (DTC clearing, etc.)
    HIGH(1),        // Important real-time data (RPM, speed)
    NORMAL(2),      // Regular monitoring data
    LOW(3),         // Background data collection
    BATCH(4)        // Bulk operations
}

/**
 * Queued command wrapper
 */
data class QueuedCommand(
    val id: Long,
    val command: ObdCommand,
    val priority: CommandPriority,
    val enqueuedAt: Long,
    val callback: ((CommandResponse) -> Unit)? = null,
    var isCancelled: Boolean = false
) : Comparable<QueuedCommand> {
    override fun compareTo(other: QueuedCommand): Int {
        // Higher priority (lower value) comes first
        val priorityComparison = priority.value.compareTo(other.priority.value)
        return if (priorityComparison != 0) {
            priorityComparison
        } else {
            // Same priority, FIFO order
            enqueuedAt.compareTo(other.enqueuedAt)
        }
    }
}

/**
 * Command response
 */
data class CommandResponse(
    val commandId: Long,
    val command: ObdCommand,
    val response: String?,
    val success: Boolean,
    val error: String? = null,
    val executionTime: Long,
    val attempt: Int
)

/**
 * Queue state for monitoring
 */
data class QueueState(
    val pendingCommands: Int = 0,
    val activeCommands: Int = 0,
    val totalProcessed: Long = 0,
    val successfulCommands: Long = 0,
    val failedCommands: Long = 0
)

/**
 * Queue statistics
 */
data class QueueStats(
    val pendingCommands: Int,
    val activeCommands: Int,
    val totalProcessed: Long,
    val successfulCommands: Long,
    val failedCommands: Long
) {
    val successRate: Float
        get() = if (totalProcessed > 0) {
            (successfulCommands.toFloat() / totalProcessed.toFloat()) * 100f
        } else 0f
}
