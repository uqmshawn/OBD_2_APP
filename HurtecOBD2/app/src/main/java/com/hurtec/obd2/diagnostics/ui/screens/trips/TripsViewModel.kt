package com.hurtec.obd2.diagnostics.ui.screens.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hurtec.obd2.diagnostics.utils.CrashHandler
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
            try {
                _uiState.value = _uiState.value.copy(isExporting = true)

                // Generate CSV export
                val csvData = generateCsvExport(_uiState.value.trips)

                // Simulate export delay
                kotlinx.coroutines.delay(2000)

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccess = true,
                    exportData = csvData
                )

                CrashHandler.logInfo("Trip data exported successfully")
            } catch (e: Exception) {
                CrashHandler.handleException(e, "TripsViewModel.exportTrips")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun applyFilter(filter: TripFilter) {
        _uiState.value = _uiState.value.copy(currentFilter = filter)
        // Apply filter logic here
        CrashHandler.logInfo("Trip filter applied: $filter")
    }

    fun clearExportSuccess() {
        _uiState.value = _uiState.value.copy(exportSuccess = false, exportData = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun generateCsvExport(trips: List<TripRecord>): String {
        val header = "Trip ID,Start Time,End Time,Duration (min),Distance (km),Avg Speed (km/h),Max Speed (km/h),Fuel Used (L),Fuel Economy (L/100km),Start Location,End Location\n"

        val rows = trips.joinToString("\n") { trip ->
            "${trip.id}," +
            "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(trip.startTime))}," +
            "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(trip.endTime))}," +
            "${trip.duration / 60}," +
            "${trip.distance}," +
            "${trip.averageSpeed}," +
            "${trip.maxSpeed}," +
            "${trip.fuelUsed}," +
            "${trip.fuelEconomy}," +
            "\"${trip.startLocation}\"," +
            "\"${trip.endLocation}\""
        }

        return header + rows
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
    val exportData: String? = null,
    val currentFilter: TripFilter = TripFilter.ALL,
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

/**
 * Trip filter options
 */
enum class TripFilter(val displayName: String) {
    ALL("All Trips"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LONG_TRIPS("Long Trips (>30 min)"),
    SHORT_TRIPS("Short Trips (<30 min)")
}
