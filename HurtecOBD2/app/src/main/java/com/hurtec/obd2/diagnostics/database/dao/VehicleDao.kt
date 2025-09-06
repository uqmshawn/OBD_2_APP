package com.hurtec.obd2.diagnostics.database.dao

import androidx.room.*
import com.hurtec.obd2.diagnostics.database.entities.VehicleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Vehicle operations
 */
@Dao
interface VehicleDao {
    
    // ========== INSERT OPERATIONS ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleEntity>): List<Long>
    
    // ========== UPDATE OPERATIONS ==========
    
    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity): Int
    
    @Query("UPDATE vehicles SET is_active = 0")
    suspend fun deactivateAllVehicles()
    
    @Query("UPDATE vehicles SET is_active = 1, updated_at = :timestamp WHERE id = :vehicleId")
    suspend fun setActiveVehicle(vehicleId: Long, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE vehicles SET last_connected_at = :timestamp, updated_at = :updateTime WHERE id = :vehicleId")
    suspend fun updateLastConnectedTime(vehicleId: Long, timestamp: Long, updateTime: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE vehicles SET odometer = :odometer, odometer_unit = :unit, updated_at = :timestamp WHERE id = :vehicleId")
    suspend fun updateOdometer(vehicleId: Long, odometer: Double, unit: String, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE vehicles SET supported_pids = :pids, updated_at = :timestamp WHERE id = :vehicleId")
    suspend fun updateSupportedPids(vehicleId: Long, pids: String, timestamp: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE vehicles SET obd_protocol = :protocol, updated_at = :timestamp WHERE id = :vehicleId")
    suspend fun updateObdProtocol(vehicleId: Long, protocol: String, timestamp: Long = System.currentTimeMillis()): Int
    
    // ========== DELETE OPERATIONS ==========
    
    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity): Int
    
    @Query("DELETE FROM vehicles WHERE id = :vehicleId")
    suspend fun deleteVehicleById(vehicleId: Long): Int
    
    @Query("DELETE FROM vehicles WHERE updated_at < :cutoffTime")
    suspend fun deleteOldVehicles(cutoffTime: Long): Int
    
    // ========== SELECT OPERATIONS ==========
    
    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    suspend fun getVehicleById(vehicleId: Long): VehicleEntity?
    
    @Query("SELECT * FROM vehicles WHERE id = :vehicleId")
    fun getVehicleByIdFlow(vehicleId: Long): Flow<VehicleEntity?>
    
    @Query("SELECT * FROM vehicles WHERE vin = :vin LIMIT 1")
    suspend fun getVehicleByVin(vin: String): VehicleEntity?
    
    @Query("SELECT * FROM vehicles ORDER BY updated_at DESC")
    suspend fun getAllVehicles(): List<VehicleEntity>
    
    @Query("SELECT * FROM vehicles ORDER BY updated_at DESC")
    fun getAllVehiclesFlow(): Flow<List<VehicleEntity>>
    
    @Query("SELECT * FROM vehicles WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveVehicle(): VehicleEntity?
    
    @Query("SELECT * FROM vehicles WHERE is_active = 1 LIMIT 1")
    fun getActiveVehicleFlow(): Flow<VehicleEntity?>
    
    @Query("""
        SELECT * FROM vehicles 
        WHERE name LIKE '%' || :query || '%' 
           OR make LIKE '%' || :query || '%' 
           OR model LIKE '%' || :query || '%'
           OR vin LIKE '%' || :query || '%'
        ORDER BY updated_at DESC
    """)
    suspend fun searchVehicles(query: String): List<VehicleEntity>
    
    @Query("""
        SELECT * FROM vehicles 
        WHERE name LIKE '%' || :query || '%' 
           OR make LIKE '%' || :query || '%' 
           OR model LIKE '%' || :query || '%'
           OR vin LIKE '%' || :query || '%'
        ORDER BY updated_at DESC
    """)
    fun searchVehiclesFlow(query: String): Flow<List<VehicleEntity>>
    
    // ========== STATISTICS OPERATIONS ==========
    
    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun getVehicleCount(): Int
    
    @Query("SELECT COUNT(*) FROM vehicles")
    fun getVehicleCountFlow(): Flow<Int>
    
    @Query("SELECT DISTINCT make FROM vehicles WHERE make IS NOT NULL ORDER BY make")
    suspend fun getAllMakes(): List<String>
    
    @Query("SELECT DISTINCT model FROM vehicles WHERE make = :make AND model IS NOT NULL ORDER BY model")
    suspend fun getModelsByMake(make: String): List<String>
    
    @Query("SELECT DISTINCT year FROM vehicles WHERE year IS NOT NULL ORDER BY year DESC")
    suspend fun getAllYears(): List<Int>
    
    @Query("""
        SELECT v.*, 
               COALESCE(session_stats.session_count, 0) as session_count,
               COALESCE(data_stats.data_count, 0) as data_count,
               session_stats.last_session_time
        FROM vehicles v
        LEFT JOIN (
            SELECT vehicle_id, 
                   COUNT(*) as session_count,
                   MAX(start_time) as last_session_time
            FROM sessions 
            GROUP BY vehicle_id
        ) session_stats ON v.id = session_stats.vehicle_id
        LEFT JOIN (
            SELECT vehicle_id, 
                   COUNT(*) as data_count
            FROM obd_data 
            GROUP BY vehicle_id
        ) data_stats ON v.id = data_stats.vehicle_id
        ORDER BY v.updated_at DESC
    """)
    suspend fun getAllVehiclesWithStats(): List<VehicleWithStats>
    
    @Query("""
        SELECT v.*, 
               COALESCE(session_stats.session_count, 0) as session_count,
               COALESCE(data_stats.data_count, 0) as data_count,
               session_stats.last_session_time
        FROM vehicles v
        LEFT JOIN (
            SELECT vehicle_id, 
                   COUNT(*) as session_count,
                   MAX(start_time) as last_session_time
            FROM sessions 
            GROUP BY vehicle_id
        ) session_stats ON v.id = session_stats.vehicle_id
        LEFT JOIN (
            SELECT vehicle_id, 
                   COUNT(*) as data_count
            FROM obd_data 
            GROUP BY vehicle_id
        ) data_stats ON v.id = data_stats.vehicle_id
        ORDER BY v.updated_at DESC
    """)
    fun getAllVehiclesWithStatsFlow(): Flow<List<VehicleWithStats>>
}

/**
 * Vehicle with statistics data class
 */
data class VehicleWithStats(
    val id: Long,
    val name: String,
    val vin: String?,
    val make: String?,
    val model: String?,
    val year: Int?,
    @ColumnInfo(name = "engine_size") val engineSize: String?,
    @ColumnInfo(name = "fuel_type") val fuelType: String?,
    val transmission: String?,
    val odometer: Double?,
    @ColumnInfo(name = "odometer_unit") val odometerUnit: String,
    @ColumnInfo(name = "license_plate") val licensePlate: String?,
    val color: String?,
    val notes: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "last_connected_at") val lastConnectedAt: Long?,
    @ColumnInfo(name = "preferred_unit_system") val preferredUnitSystem: String,
    @ColumnInfo(name = "supported_pids") val supportedPids: String?,
    @ColumnInfo(name = "obd_protocol") val obdProtocol: String?,
    @ColumnInfo(name = "connection_type") val connectionType: String?,
    @ColumnInfo(name = "device_address") val deviceAddress: String?,
    @ColumnInfo(name = "avatar_path") val avatarPath: String?,
    @ColumnInfo(name = "session_count") val sessionCount: Int = 0,
    @ColumnInfo(name = "data_count") val dataCount: Int = 0,
    @ColumnInfo(name = "last_session_time") val lastSessionTime: Long? = null
) {
    /**
     * Convert to VehicleEntity
     */
    fun toVehicleEntity(): VehicleEntity {
        return VehicleEntity(
            id = id,
            name = name,
            vin = vin,
            make = make,
            model = model,
            year = year,
            engineSize = engineSize,
            fuelType = fuelType,
            transmission = transmission,
            odometer = odometer,
            odometerUnit = odometerUnit,
            licensePlate = licensePlate,
            color = color,
            notes = notes,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastConnectedAt = lastConnectedAt,
            preferredUnitSystem = preferredUnitSystem,
            supportedPids = supportedPids,
            obdProtocol = obdProtocol,
            connectionType = connectionType,
            deviceAddress = deviceAddress,
            avatarPath = avatarPath
        )
    }
}
