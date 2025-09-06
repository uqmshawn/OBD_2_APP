package com.hurtec.obd2.diagnostics.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Session entity for Room database
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vehicle_id"]),
        Index(value = ["session_id"], unique = true),
        Index(value = ["start_time"]),
        Index(value = ["end_time"]),
        Index(value = ["is_active"])
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: Long,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "connection_type")
    val connectionType: String,
    
    @ColumnInfo(name = "device_address")
    val deviceAddress: String? = null,
    
    @ColumnInfo(name = "obd_protocol")
    val obdProtocol: String? = null,
    
    @ColumnInfo(name = "total_data_points")
    val totalDataPoints: Int = 0,
    
    @ColumnInfo(name = "unique_pids")
    val uniquePids: Int = 0,
    
    @ColumnInfo(name = "distance_traveled")
    val distanceTraveled: Double? = null,
    
    @ColumnInfo(name = "distance_unit")
    val distanceUnit: String = "km",
    
    @ColumnInfo(name = "max_speed")
    val maxSpeed: Double? = null,
    
    @ColumnInfo(name = "avg_speed")
    val avgSpeed: Double? = null,
    
    @ColumnInfo(name = "max_rpm")
    val maxRpm: Double? = null,
    
    @ColumnInfo(name = "avg_rpm")
    val avgRpm: Double? = null,
    
    @ColumnInfo(name = "fuel_consumed")
    val fuelConsumed: Double? = null,
    
    @ColumnInfo(name = "fuel_unit")
    val fuelUnit: String = "L",
    
    @ColumnInfo(name = "avg_fuel_economy")
    val avgFuelEconomy: Double? = null,
    
    @ColumnInfo(name = "fuel_economy_unit")
    val fuelEconomyUnit: String = "L/100km",
    
    @ColumnInfo(name = "driving_conditions")
    val drivingConditions: String? = null,
    
    @ColumnInfo(name = "weather_conditions")
    val weatherConditions: String? = null,
    
    @ColumnInfo(name = "start_location")
    val startLocation: String? = null,
    
    @ColumnInfo(name = "end_location")
    val endLocation: String? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get session duration in milliseconds
     */
    fun getDurationMs(): Long {
        return (endTime ?: System.currentTimeMillis()) - startTime
    }
    
    /**
     * Get formatted duration
     */
    fun getFormattedDuration(): String {
        val durationMs = getDurationMs()
        val hours = durationMs / (1000 * 60 * 60)
        val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (durationMs % (1000 * 60)) / 1000
        
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
            else -> "${seconds}s"
        }
    }
    
    /**
     * Get formatted start time
     */
    fun getFormattedStartTime(): String {
        return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(startTime))
    }
    
    /**
     * Get formatted end time
     */
    fun getFormattedEndTime(): String? {
        return endTime?.let {
            java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(it))
        }
    }
    
    /**
     * Check if session is currently active
     */
    fun isCurrentlyActive(): Boolean {
        return isActive && endTime == null
    }
    
    /**
     * Get data points per minute
     */
    fun getDataPointsPerMinute(): Double {
        val durationMinutes = getDurationMs() / (1000.0 * 60.0)
        return if (durationMinutes > 0) totalDataPoints / durationMinutes else 0.0
    }
    
    /**
     * Get connection type display name
     */
    fun getConnectionTypeDisplay(): String {
        return when (connectionType.uppercase()) {
            "BLUETOOTH" -> "Bluetooth"
            "USB" -> "USB"
            "WIFI" -> "Wi-Fi"
            else -> connectionType
        }
    }
    
    /**
     * Get session summary
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()
        
        if (totalDataPoints > 0) {
            parts.add("$totalDataPoints data points")
        }
        
        if (distanceTraveled != null && distanceTraveled > 0) {
            parts.add("${String.format("%.1f", distanceTraveled)} $distanceUnit")
        }
        
        if (avgSpeed != null && avgSpeed > 0) {
            parts.add("Avg: ${String.format("%.1f", avgSpeed)} km/h")
        }
        
        return parts.joinToString(" • ")
    }
    
    /**
     * End the session
     */
    fun endSession(): SessionEntity {
        return copy(
            endTime = System.currentTimeMillis(),
            isActive = false,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Update statistics
     */
    fun updateStats(
        dataPoints: Int,
        uniquePids: Int,
        distance: Double? = null,
        maxSpeed: Double? = null,
        avgSpeed: Double? = null,
        maxRpm: Double? = null,
        avgRpm: Double? = null
    ): SessionEntity {
        return copy(
            totalDataPoints = dataPoints,
            uniquePids = uniquePids,
            distanceTraveled = distance ?: distanceTraveled,
            maxSpeed = maxSpeed ?: this.maxSpeed,
            avgSpeed = avgSpeed ?: this.avgSpeed,
            maxRpm = maxRpm ?: this.maxRpm,
            avgRpm = avgRpm ?: this.avgRpm,
            updatedAt = System.currentTimeMillis()
        )
    }
}
