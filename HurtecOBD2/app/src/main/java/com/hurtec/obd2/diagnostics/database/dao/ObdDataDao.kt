package com.hurtec.obd2.diagnostics.database.dao

import androidx.room.*
import com.hurtec.obd2.diagnostics.database.entities.ObdDataEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for OBD data operations
 */
@Dao
interface ObdDataDao {
    
    // ========== INSERT OPERATIONS ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObdData(obdData: ObdDataEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObdDataBatch(obdDataList: List<ObdDataEntity>): List<Long>
    
    // ========== DELETE OPERATIONS ==========
    
    @Query("DELETE FROM obd_data WHERE vehicle_id = :vehicleId")
    suspend fun deleteObdDataByVehicle(vehicleId: Long): Int
    
    @Query("DELETE FROM obd_data WHERE session_id = :sessionId")
    suspend fun deleteObdDataBySession(sessionId: String): Int
    
    @Query("DELETE FROM obd_data WHERE vehicle_id = :vehicleId AND timestamp < :cutoffTime")
    suspend fun deleteOldObdDataForVehicle(vehicleId: Long, cutoffTime: Long): Int
    
    @Query("DELETE FROM obd_data WHERE id IN (SELECT id FROM obd_data WHERE vehicle_id = :vehicleId ORDER BY timestamp DESC LIMIT -1 OFFSET :maxRecords)")
    suspend fun limitDataRecords(vehicleId: Long, maxRecords: Int): Int
    
    // ========== SELECT OPERATIONS ==========
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId ORDER BY timestamp DESC")
    suspend fun getObdDataByVehicle(vehicleId: Long): List<ObdDataEntity>
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId ORDER BY timestamp DESC")
    fun getObdDataByVehicleFlow(vehicleId: Long): Flow<List<ObdDataEntity>>
    
    @Query("SELECT * FROM obd_data WHERE session_id = :sessionId ORDER BY timestamp DESC")
    suspend fun getObdDataBySession(sessionId: String): List<ObdDataEntity>
    
    @Query("SELECT * FROM obd_data WHERE session_id = :sessionId ORDER BY timestamp DESC")
    fun getObdDataBySessionFlow(sessionId: String): Flow<List<ObdDataEntity>>
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId AND pid = :pid ORDER BY timestamp DESC")
    suspend fun getObdDataByVehicleAndPid(vehicleId: Long, pid: String): List<ObdDataEntity>
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId AND pid = :pid ORDER BY timestamp DESC")
    fun getObdDataByVehicleAndPidFlow(vehicleId: Long, pid: String): Flow<List<ObdDataEntity>>
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId AND pid = :pid ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentObdDataByPid(vehicleId: Long, pid: String, limit: Int): List<ObdDataEntity>
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getObdDataByTimeRange(vehicleId: Long, startTime: Long, endTime: Long): List<ObdDataEntity>
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getObdDataByTimeRangeFlow(vehicleId: Long, startTime: Long, endTime: Long): Flow<List<ObdDataEntity>>
    
    // ========== LATEST DATA OPERATIONS ==========
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId AND pid = :pid ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestObdDataByPid(vehicleId: Long, pid: String): ObdDataEntity?
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId AND pid = :pid ORDER BY timestamp DESC LIMIT 1")
    fun getLatestObdDataByPidFlow(vehicleId: Long, pid: String): Flow<ObdDataEntity?>
    
    @Query("""
        SELECT * FROM obd_data o1
        WHERE o1.vehicle_id = :vehicleId 
        AND o1.timestamp = (
            SELECT MAX(o2.timestamp) 
            FROM obd_data o2 
            WHERE o2.vehicle_id = o1.vehicle_id 
            AND o2.pid = o1.pid
        )
        ORDER BY o1.timestamp DESC
    """)
    suspend fun getLatestObdDataForAllPids(vehicleId: Long): List<ObdDataEntity>
    
    @Query("""
        SELECT * FROM obd_data o1
        WHERE o1.vehicle_id = :vehicleId 
        AND o1.timestamp = (
            SELECT MAX(o2.timestamp) 
            FROM obd_data o2 
            WHERE o2.vehicle_id = o1.vehicle_id 
            AND o2.pid = o1.pid
        )
        ORDER BY o1.timestamp DESC
    """)
    fun getLatestObdDataForAllPidsFlow(vehicleId: Long): Flow<List<ObdDataEntity>>
    
    // ========== STATISTICS OPERATIONS ==========
    
    @Query("""
        SELECT pid,
               COUNT(*) as count,
               AVG(processed_value) as avg_value,
               MIN(processed_value) as min_value,
               MAX(processed_value) as max_value,
               MIN(timestamp) as first_timestamp,
               MAX(timestamp) as last_timestamp
        FROM obd_data 
        WHERE vehicle_id = :vehicleId AND processed_value IS NOT NULL
        GROUP BY pid
        ORDER BY count DESC
    """)
    suspend fun getPidStatistics(vehicleId: Long): List<PidStatistics>
    
    @Query("SELECT DISTINCT pid FROM obd_data WHERE vehicle_id = :vehicleId ORDER BY pid")
    suspend fun getAvailablePids(vehicleId: Long): List<String>
    
    @Query("SELECT COUNT(*) FROM obd_data WHERE vehicle_id = :vehicleId")
    suspend fun getObdDataCount(vehicleId: Long): Int
    
    @Query("SELECT COUNT(*) FROM obd_data WHERE vehicle_id = :vehicleId")
    fun getObdDataCountFlow(vehicleId: Long): Flow<Int>
    
    @Query("""
        SELECT session_id,
               COUNT(*) as data_count,
               COUNT(DISTINCT pid) as unique_pids,
               MIN(timestamp) as start_time,
               MAX(timestamp) as end_time,
               AVG(processing_time_ms) as avg_processing_time
        FROM obd_data 
        WHERE vehicle_id = :vehicleId
        GROUP BY session_id
        ORDER BY start_time DESC
    """)
    suspend fun getSessionStatistics(vehicleId: Long): List<SessionStatistics>
    
    @Query("SELECT * FROM obd_data WHERE vehicle_id = :vehicleId AND is_valid = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getInvalidData(vehicleId: Long, limit: Int = 100): List<ObdDataEntity>
    
    @Query("SELECT COUNT(*) FROM obd_data WHERE vehicle_id = :vehicleId AND timestamp > :since")
    suspend fun getDataCountSince(vehicleId: Long, since: Long): Int
    
    @Query("SELECT AVG(processing_time_ms) FROM obd_data WHERE vehicle_id = :vehicleId AND timestamp > :since")
    suspend fun getAverageProcessingTime(vehicleId: Long, since: Long): Double?
}

/**
 * PID statistics data class
 */
data class PidStatistics(
    val pid: String,
    val count: Int,
    @ColumnInfo(name = "avg_value") val avgValue: Double?,
    @ColumnInfo(name = "min_value") val minValue: Double?,
    @ColumnInfo(name = "max_value") val maxValue: Double?,
    @ColumnInfo(name = "first_timestamp") val firstTimestamp: Long,
    @ColumnInfo(name = "last_timestamp") val lastTimestamp: Long
)

/**
 * Session statistics data class
 */
data class SessionStatistics(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "data_count") val dataCount: Int,
    @ColumnInfo(name = "unique_pids") val uniquePids: Int,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long,
    @ColumnInfo(name = "avg_processing_time") val avgProcessingTime: Double?
)
