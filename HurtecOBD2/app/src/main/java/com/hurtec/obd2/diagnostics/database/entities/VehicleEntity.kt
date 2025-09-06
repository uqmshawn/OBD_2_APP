package com.hurtec.obd2.diagnostics.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Vehicle entity for Room database
 */
@Entity(
    tableName = "vehicles",
    indices = [
        Index(value = ["name"]),
        Index(value = ["vin"], unique = true),
        Index(value = ["is_active"]),
        Index(value = ["created_at"])
    ]
)
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "vin")
    val vin: String? = null,
    
    @ColumnInfo(name = "make")
    val make: String? = null,
    
    @ColumnInfo(name = "model")
    val model: String? = null,
    
    @ColumnInfo(name = "year")
    val year: Int? = null,
    
    @ColumnInfo(name = "engine_size")
    val engineSize: String? = null,
    
    @ColumnInfo(name = "fuel_type")
    val fuelType: String? = null,
    
    @ColumnInfo(name = "transmission")
    val transmission: String? = null,
    
    @ColumnInfo(name = "odometer")
    val odometer: Double? = null,
    
    @ColumnInfo(name = "odometer_unit")
    val odometerUnit: String = "km",
    
    @ColumnInfo(name = "license_plate")
    val licensePlate: String? = null,
    
    @ColumnInfo(name = "color")
    val color: String? = null,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_connected_at")
    val lastConnectedAt: Long? = null,
    
    @ColumnInfo(name = "preferred_unit_system")
    val preferredUnitSystem: String = "METRIC",
    
    @ColumnInfo(name = "supported_pids")
    val supportedPids: String? = null,
    
    @ColumnInfo(name = "obd_protocol")
    val obdProtocol: String? = null,
    
    @ColumnInfo(name = "connection_type")
    val connectionType: String? = null,
    
    @ColumnInfo(name = "device_address")
    val deviceAddress: String? = null,
    
    @ColumnInfo(name = "avatar_path")
    val avatarPath: String? = null
) {
    /**
     * Get display name for the vehicle
     */
    fun getDisplayName(): String {
        return when {
            make != null && model != null && year != null -> "$year $make $model"
            make != null && model != null -> "$make $model"
            name.isNotBlank() -> name
            else -> "Vehicle #$id"
        }
    }
    
    /**
     * Get short description
     */
    fun getShortDescription(): String {
        return when {
            year != null && make != null -> "$year $make"
            make != null -> make
            else -> "Unknown Vehicle"
        }
    }
    
    /**
     * Check if vehicle has basic info
     */
    fun hasBasicInfo(): Boolean {
        return make != null && model != null && year != null
    }
    
    /**
     * Get connection status text
     */
    fun getConnectionStatus(): String {
        return when {
            lastConnectedAt == null -> "Never connected"
            System.currentTimeMillis() - lastConnectedAt < 5 * 60 * 1000 -> "Recently connected"
            System.currentTimeMillis() - lastConnectedAt < 24 * 60 * 60 * 1000 -> "Connected today"
            else -> "Last connected ${(System.currentTimeMillis() - lastConnectedAt) / (24 * 60 * 60 * 1000)} days ago"
        }
    }
    
    /**
     * Get supported PIDs as list
     */
    fun getSupportedPidsList(): List<String> {
        return supportedPids?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    
    /**
     * Update supported PIDs from list
     */
    fun withSupportedPids(pids: List<String>): VehicleEntity {
        return copy(supportedPids = pids.joinToString(","))
    }
    
    /**
     * Update last connected time
     */
    fun withLastConnected(): VehicleEntity {
        return copy(lastConnectedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
    }
    
    /**
     * Set as active vehicle
     */
    fun withActive(active: Boolean): VehicleEntity {
        return copy(isActive = active, updatedAt = System.currentTimeMillis())
    }
}
