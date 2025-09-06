package com.hurtec.obd2.diagnostics.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * OBD data entity for Room database
 */
@Entity(
    tableName = "obd_data",
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
        Index(value = ["session_id"]),
        Index(value = ["pid"]),
        Index(value = ["timestamp"]),
        Index(value = ["vehicle_id", "pid"]),
        Index(value = ["vehicle_id", "timestamp"]),
        Index(value = ["session_id", "timestamp"])
    ]
)
data class ObdDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: Long,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    
    @ColumnInfo(name = "pid")
    val pid: String,
    
    @ColumnInfo(name = "pid_name")
    val pidName: String,
    
    @ColumnInfo(name = "raw_value")
    val rawValue: Double?,
    
    @ColumnInfo(name = "processed_value")
    val processedValue: Double?,
    
    @ColumnInfo(name = "string_value")
    val stringValue: String,
    
    @ColumnInfo(name = "formatted_value")
    val formattedValue: String,
    
    @ColumnInfo(name = "unit")
    val unit: String,
    
    @ColumnInfo(name = "unit_system")
    val unitSystem: String,
    
    @ColumnInfo(name = "quality")
    val quality: String,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "raw_response")
    val rawResponse: String,
    
    @ColumnInfo(name = "command")
    val command: String,
    
    @ColumnInfo(name = "processing_time_ms")
    val processingTimeMs: Long,
    
    @ColumnInfo(name = "data_bytes")
    val dataBytes: String,
    
    @ColumnInfo(name = "is_valid")
    val isValid: Boolean,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,
    
    @ColumnInfo(name = "altitude")
    val altitude: Double? = null,
    
    @ColumnInfo(name = "speed_gps")
    val speedGps: Float? = null
) {
    /**
     * Get data bytes as list
     */
    fun getDataBytesList(): List<Int> {
        return dataBytes.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
    
    /**
     * Check if data has GPS location
     */
    fun hasLocation(): Boolean {
        return latitude != null && longitude != null
    }
    
    /**
     * Get formatted timestamp
     */
    fun getFormattedTimestamp(): String {
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
    
    /**
     * Get age in milliseconds
     */
    fun getAgeMs(): Long {
        return System.currentTimeMillis() - timestamp
    }
    
    /**
     * Check if data is recent (within last 5 seconds)
     */
    fun isRecent(): Boolean {
        return getAgeMs() < 5000
    }
    
    /**
     * Get quality as enum
     */
    fun getQualityLevel(): DataQuality {
        return try {
            DataQuality.valueOf(quality)
        } catch (e: Exception) {
            DataQuality.UNKNOWN
        }
    }
    
    /**
     * Get unit system as enum
     */
    fun getUnitSystemType(): UnitSystem {
        return try {
            UnitSystem.valueOf(unitSystem)
        } catch (e: Exception) {
            UnitSystem.METRIC
        }
    }
    
    /**
     * Create display value with unit
     */
    fun getDisplayValue(): String {
        return if (unit.isNotEmpty()) {
            "$formattedValue $unit"
        } else {
            formattedValue
        }
    }
    
    /**
     * Check if this is a numeric PID
     */
    fun isNumeric(): Boolean {
        return processedValue != null
    }
    
    /**
     * Get short PID name for display
     */
    fun getShortPidName(): String {
        return when {
            pidName.length > 20 -> pidName.take(17) + "..."
            else -> pidName
        }
    }
}

/**
 * Data quality enumeration
 */
enum class DataQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    INVALID,
    UNKNOWN
}

/**
 * Unit system enumeration
 */
enum class UnitSystem {
    METRIC,
    IMPERIAL,
    MIXED
}
