package com.hurtec.obd2.diagnostics.ui.screens.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.obd.communication.CommunicationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Performance Monitoring screen
 */
@HiltViewModel
class PerformanceViewModel @Inject constructor(
    private val communicationManager: CommunicationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerformanceUiState())
    val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

    init {
        loadPerformanceData()
        startTripMonitoring()
    }

    private fun loadPerformanceData() {
        _uiState.value = _uiState.value.copy(
            currentTrip = TripData(
                distance = 15.3f,
                duration = 1800, // 30 minutes
                averageSpeed = 30.6f,
                maxSpeed = 65.0f,
                fuelUsed = 0.8f,
                fuelEconomy = 19.1f,
                isActive = true
            ),
            fuelEconomy = FuelEconomyData(
                currentMpg = 22.5f,
                averageMpg = 24.8f,
                estimatedRange = 285f
            ),
            powerEstimates = PowerEstimates(
                horsepower = 185f,
                torque = 220f,
                powerToWeight = 0.055f
            ),
            best060Time = 7.2f,
            bestQuarterMileTime = 15.8f
        )
    }

    private fun startTripMonitoring() {
        viewModelScope.launch {
            while (true) {
                if (communicationManager.isConnected() && _uiState.value.currentTrip.isActive) {
                    updateTripData()
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun updateTripData() {
        val currentTrip = _uiState.value.currentTrip
        val updatedTrip = currentTrip.copy(
            distance = currentTrip.distance + 0.01f, // Simulate distance increment
            duration = currentTrip.duration + 1,
            averageSpeed = (currentTrip.distance / (currentTrip.duration / 3600f)).coerceAtLeast(0f),
            fuelUsed = currentTrip.fuelUsed + 0.0001f // Simulate fuel consumption
        )

        _uiState.value = _uiState.value.copy(currentTrip = updatedTrip)
    }

    fun start060Test() {
        val test = PerformanceTest(
            type = TestType.ZERO_TO_SIXTY,
            startTime = System.currentTimeMillis(),
            currentTime = 0f,
            currentSpeed = 0f,
            isActive = true
        )

        _uiState.value = _uiState.value.copy(currentTest = test)
        
        viewModelScope.launch {
            runPerformanceTest(test)
        }
    }

    fun startQuarterMileTest() {
        val test = PerformanceTest(
            type = TestType.QUARTER_MILE,
            startTime = System.currentTimeMillis(),
            currentTime = 0f,
            currentSpeed = 0f,
            isActive = true
        )

        _uiState.value = _uiState.value.copy(currentTest = test)
        
        viewModelScope.launch {
            runPerformanceTest(test)
        }
    }

    private suspend fun runPerformanceTest(test: PerformanceTest) {
        var currentTest = test
        
        while (currentTest.isActive && _uiState.value.currentTest?.isActive == true) {
            val elapsedTime = (System.currentTimeMillis() - currentTest.startTime) / 1000f
            val simulatedSpeed = when (test.type) {
                TestType.ZERO_TO_SIXTY -> minOf(60f, elapsedTime * 8.5f) // Simulate acceleration
                TestType.QUARTER_MILE -> minOf(100f, elapsedTime * 12f)
            }

            currentTest = currentTest.copy(
                currentTime = elapsedTime,
                currentSpeed = simulatedSpeed
            )

            _uiState.value = _uiState.value.copy(currentTest = currentTest)

            // Check completion conditions
            val isComplete = when (test.type) {
                TestType.ZERO_TO_SIXTY -> simulatedSpeed >= 60f
                TestType.QUARTER_MILE -> elapsedTime >= 15f // Simulate quarter mile completion
            }

            if (isComplete) {
                completeTest(currentTest)
                break
            }

            kotlinx.coroutines.delay(100) // Update every 100ms
        }
    }

    private fun completeTest(test: PerformanceTest) {
        val record = PerformanceRecord(
            testType = test.type,
            time = test.currentTime,
            timestamp = System.currentTimeMillis()
        )

        val updatedHistory = (_uiState.value.performanceHistory + record)
            .sortedByDescending { it.timestamp }

        val updatedState = when (test.type) {
            TestType.ZERO_TO_SIXTY -> {
                val newBest = _uiState.value.best060Time?.let { 
                    minOf(it, test.currentTime) 
                } ?: test.currentTime
                
                _uiState.value.copy(
                    currentTest = null,
                    best060Time = newBest,
                    performanceHistory = updatedHistory
                )
            }
            TestType.QUARTER_MILE -> {
                val newBest = _uiState.value.bestQuarterMileTime?.let { 
                    minOf(it, test.currentTime) 
                } ?: test.currentTime
                
                _uiState.value.copy(
                    currentTest = null,
                    bestQuarterMileTime = newBest,
                    performanceHistory = updatedHistory
                )
            }
        }

        _uiState.value = updatedState
    }

    fun stopCurrentTest() {
        _uiState.value = _uiState.value.copy(currentTest = null)
    }

    fun resetTrip() {
        _uiState.value = _uiState.value.copy(
            currentTrip = TripData(
                distance = 0f,
                duration = 0,
                averageSpeed = 0f,
                maxSpeed = 0f,
                fuelUsed = 0f,
                fuelEconomy = 0f,
                isActive = false
            )
        )
    }

    fun startTrip() {
        _uiState.value = _uiState.value.copy(
            currentTrip = _uiState.value.currentTrip.copy(
                isActive = true,
                duration = 0
            )
        )
    }

    fun resetFuelEconomy() {
        _uiState.value = _uiState.value.copy(
            fuelEconomy = FuelEconomyData(
                currentMpg = 0f,
                averageMpg = 0f,
                estimatedRange = 0f
            )
        )
    }
}

/**
 * UI state for Performance screen
 */
data class PerformanceUiState(
    val currentTest: PerformanceTest? = null,
    val currentTrip: TripData = TripData(),
    val fuelEconomy: FuelEconomyData = FuelEconomyData(),
    val powerEstimates: PowerEstimates = PowerEstimates(),
    val best060Time: Float? = null,
    val bestQuarterMileTime: Float? = null,
    val performanceHistory: List<PerformanceRecord> = emptyList(),
    val error: String? = null
)

/**
 * Performance test data
 */
data class PerformanceTest(
    val type: TestType,
    val startTime: Long,
    val currentTime: Float,
    val currentSpeed: Float,
    val isActive: Boolean
)

/**
 * Test types
 */
enum class TestType(val displayName: String) {
    ZERO_TO_SIXTY("0-60 mph"),
    QUARTER_MILE("Quarter Mile")
}

/**
 * Trip data
 */
data class TripData(
    val distance: Float = 0f,
    val duration: Long = 0, // seconds
    val averageSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val fuelUsed: Float = 0f,
    val fuelEconomy: Float = 0f,
    val isActive: Boolean = false
)

/**
 * Fuel economy data
 */
data class FuelEconomyData(
    val currentMpg: Float = 0f,
    val averageMpg: Float = 0f,
    val estimatedRange: Float = 0f
)

/**
 * Power estimates
 */
data class PowerEstimates(
    val horsepower: Float = 0f,
    val torque: Float = 0f,
    val powerToWeight: Float = 0f
)

/**
 * Performance record
 */
data class PerformanceRecord(
    val testType: TestType,
    val time: Float,
    val timestamp: Long
)
