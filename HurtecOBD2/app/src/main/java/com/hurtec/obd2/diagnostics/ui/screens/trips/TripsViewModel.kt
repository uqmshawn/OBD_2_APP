package com.hurtec.obd2.diagnostics.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Trip History screen
 */
@HiltViewModel
class TripsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TripsUiState())
    val uiState: StateFlow<TripsUiState> = _uiState.asStateFlow()

    init {
        loadTrips()
    }

    private fun loadTrips() {
        viewModelScope.launch {
            // Simulate loading trips
            kotlinx.coroutines.delay(500)
            
            val sampleTrips = listOf(
                TripRecord(
                    id = "trip1",
                    startTime = System.currentTimeMillis() - 86400000, // Yesterday
                    endTime = System.currentTimeMillis() - 86400000 + 3600000, // 1 hour trip
                    duration = 3600, // 1 hour
                    distance = 25.3f,
                    averageSpeed = 25.3f,
                    maxSpeed = 55.0f,
                    fuelUsed = 1.2f,
                    fuelEconomy = 21.1f,
                    startLocation = "Home",
                    endLocation = "Work"
                ),
                TripRecord(
                    id = "trip2",
                    startTime = System.currentTimeMillis() - 172800000, // 2 days ago
                    endTime = System.currentTimeMillis() - 172800000 + 1800000, // 30 min trip
                    duration = 1800, // 30 minutes
                    distance = 12.7f,
                    averageSpeed = 25.4f,
                    maxSpeed = 45.0f,
                    fuelUsed = 0.6f,
                    fuelEconomy = 21.2f,
                    startLocation = "Work",
                    endLocation = "Store"
                ),
                TripRecord(
                    id = "trip3",
                    startTime = System.currentTimeMillis() - 259200000, // 3 days ago
                    endTime = System.currentTimeMillis() - 259200000 + 7200000, // 2 hour trip
                    duration = 7200, // 2 hours
                    distance = 85.6f,
                    averageSpeed = 42.8f,
                    maxSpeed = 75.0f,
                    fuelUsed = 3.8f,
                    fuelEconomy = 22.5f,
                    startLocation = "Home",
                    endLocation = "City Center"
                )
            )
            
            _uiState.value = _uiState.value.copy(
                trips = sampleTrips,
                isLoading = false
            )
        }
    }

    fun selectTrip(tripId: String) {
        val trip = _uiState.value.trips.find { it.id == tripId }
        _uiState.value = _uiState.value.copy(selectedTrip = trip)
    }

    fun deleteTrip(tripId: String) {
        val updatedTrips = _uiState.value.trips.filter { it.id != tripId }
        _uiState.value = _uiState.value.copy(trips = updatedTrips)
    }

    fun exportTrips() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            
            // Simulate export
            kotlinx.coroutines.delay(2000)
            
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportSuccess = true
            )
        }
    }

    fun clearExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false)
    }
}

/**
 * UI state for Trips screen
 */
data class TripsUiState(
    val trips: List<TripRecord> = emptyList(),
    val selectedTrip: TripRecord? = null,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val error: String? = null
)

/**
 * Trip record data
 */
data class TripRecord(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long, // seconds
    val distance: Float, // miles
    val averageSpeed: Float, // mph
    val maxSpeed: Float, // mph
    val fuelUsed: Float, // gallons
    val fuelEconomy: Float, // mpg
    val startLocation: String = "",
    val endLocation: String = "",
    val route: List<LocationPoint> = emptyList()
)

/**
 * Location point for route tracking
 */
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speed: Float = 0f
)
