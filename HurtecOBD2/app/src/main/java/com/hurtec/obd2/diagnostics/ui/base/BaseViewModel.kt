package com.hurtec.obd2.diagnostics.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import com.hurtec.obd2.diagnostics.utils.MemoryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Base ViewModel with proper lifecycle management and memory optimization
 */
abstract class BaseViewModel : ViewModel() {

    @Inject
    lateinit var memoryManager: MemoryManager

    // Base UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Job tracking for proper cleanup
    private val jobs = ConcurrentHashMap<String, Job>()

    // Coroutine scope with proper error handling
    protected val safeScope = CoroutineScope(
        viewModelScope.coroutineContext +
        SupervisorJob() +
        CoroutineExceptionHandler { _, throwable ->
            CrashHandler.handleException(throwable, "${this::class.simpleName}.safeScope")
            handleError(throwable)
        }
    )

    /**
     * Handle errors safely
     */
    protected open fun handleError(throwable: Throwable) {
        CrashHandler.handleException(throwable, this::class.java.simpleName)
        _error.value = throwable.message ?: "An unexpected error occurred"
        _isLoading.value = false
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Execute a suspending function safely with loading state
     */
    protected fun safeExecute(
        showLoading: Boolean = true,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (showLoading) _isLoading.value = true
                block()
            } catch (e: Exception) {
                onError?.invoke(e) ?: handleError(e)
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }

    /**
     * Execute a function safely without coroutines
     */
    protected fun safeTry(
        onError: ((Throwable) -> Unit)? = null,
        block: () -> Unit
    ) {
        try {
            block()
        } catch (e: Exception) {
            onError?.invoke(e) ?: handleError(e)
        }
    }
}
