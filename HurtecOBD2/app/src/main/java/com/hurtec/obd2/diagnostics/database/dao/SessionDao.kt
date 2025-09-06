package com.hurtec.obd2.diagnostics.database.dao

import androidx.room.*
import com.hurtec.obd2.diagnostics.database.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Session operations
 */
@Dao
interface SessionDao {
    
    // ========== INSERT OPERATIONS ==========
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>): List<Long>
    
    // ========== UPDATE OPERATIONS ==========
    
    @Update
    suspend fun updateSession(session: SessionEntity): Int
    
    @Query("UPDATE sessions SET is_active = 0, end_time = :endTime, updated_at = :updateTime WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, endTime: Long = System.currentTimeMillis(), updateTime: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE sessions SET is_active = 0, end_time = :endTime, updated_at = :updateTime WHERE session_id = :sessionId")
    suspend fun endSessionBySessionId(sessionId: String, endTime: Long = System.currentTimeMillis(), updateTime: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE sessions SET is_active = 0, end_time = :endTime, updated_at = :updateTime WHERE vehicle_id = :vehicleId AND is_active = 1")
    suspend fun endActiveSessionsForVehicle(vehicleId: Long, endTime: Long = System.currentTimeMillis(), updateTime: Long = System.currentTimeMillis()): Int
    
    @Query("""
        UPDATE sessions SET 
            total_data_points = :dataPoints,
            unique_pids = :uniquePids,
            distance_traveled = :distance,
            max_speed = :maxSpeed,
            avg_speed = :avgSpeed,
            max_rpm = :maxRpm,
            avg_rpm = :avgRpm,
            updated_at = :updateTime
        WHERE id = :sessionId
    """)
    suspend fun updateSessionStats(
        sessionId: Long,
        dataPoints: Int,
        uniquePids: Int,
        distance: Double? = null,
        maxSpeed: Double? = null,
        avgSpeed: Double? = null,
        maxRpm: Double? = null,
        avgRpm: Double? = null,
        updateTime: Long = System.currentTimeMillis()
    ): Int
    
    // ========== DELETE OPERATIONS ==========
    
    @Delete
    suspend fun deleteSession(session: SessionEntity): Int
    
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long): Int
    
    @Query("DELETE FROM sessions WHERE session_id = :sessionId")
    suspend fun deleteSessionBySessionId(sessionId: String): Int
    
    @Query("DELETE FROM sessions WHERE vehicle_id = :vehicleId")
    suspend fun deleteSessionsByVehicle(vehicleId: Long): Int
    
    @Query("DELETE FROM sessions WHERE start_time < :cutoffTime")
    suspend fun deleteOldSessions(cutoffTime: Long): Int
    
    // ========== SELECT OPERATIONS ==========
    
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): SessionEntity?
    
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionByIdFlow(sessionId: Long): Flow<SessionEntity?>
    
    @Query("SELECT * FROM sessions WHERE session_id = :sessionId LIMIT 1")
    suspend fun getSessionBySessionId(sessionId: String): SessionEntity?
    
    @Query("SELECT * FROM sessions WHERE session_id = :sessionId LIMIT 1")
    fun getSessionBySessionIdFlow(sessionId: String): Flow<SessionEntity?>
    
    @Query("SELECT * FROM sessions WHERE vehicle_id = :vehicleId ORDER BY start_time DESC")
    suspend fun getSessionsByVehicle(vehicleId: Long): List<SessionEntity>
    
    @Query("SELECT * FROM sessions WHERE vehicle_id = :vehicleId ORDER BY start_time DESC")
    fun getSessionsByVehicleFlow(vehicleId: Long): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE vehicle_id = :vehicleId AND is_active = 1 ORDER BY start_time DESC LIMIT 1")
    suspend fun getActiveSessionForVehicle(vehicleId: Long): SessionEntity?
    
    @Query("SELECT * FROM sessions WHERE vehicle_id = :vehicleId AND is_active = 1 ORDER BY start_time DESC LIMIT 1")
    fun getActiveSessionForVehicleFlow(vehicleId: Long): Flow<SessionEntity?>
    
    @Query("SELECT * FROM sessions WHERE is_active = 1 ORDER BY start_time DESC")
    suspend fun getAllActiveSessions(): List<SessionEntity>
    
    @Query("SELECT * FROM sessions WHERE is_active = 1 ORDER BY start_time DESC")
    fun getAllActiveSessionsFlow(): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions ORDER BY start_time DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 50): List<SessionEntity>
    
    @Query("SELECT * FROM sessions ORDER BY start_time DESC LIMIT :limit")
    fun getRecentSessionsFlow(limit: Int = 50): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE start_time BETWEEN :startTime AND :endTime ORDER BY start_time DESC")
    suspend fun getSessionsByTimeRange(startTime: Long, endTime: Long): List<SessionEntity>
    
    @Query("SELECT * FROM sessions WHERE start_time BETWEEN :startTime AND :endTime ORDER BY start_time DESC")
    fun getSessionsByTimeRangeFlow(startTime: Long, endTime: Long): Flow<List<SessionEntity>>
    
    @Query("""
        SELECT * FROM sessions 
        WHERE name LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY start_time DESC
    """)
    suspend fun searchSessions(query: String): List<SessionEntity>
    
    @Query("""
        SELECT * FROM sessions 
        WHERE name LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY start_time DESC
    """)
    fun searchSessionsFlow(query: String): Flow<List<SessionEntity>>
    
    // ========== STATISTICS OPERATIONS ==========
    
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int
    
    @Query("SELECT COUNT(*) FROM sessions")
    fun getSessionCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM sessions WHERE vehicle_id = :vehicleId")
    suspend fun getSessionCountByVehicle(vehicleId: Long): Int
    
    @Query("SELECT COUNT(*) FROM sessions WHERE vehicle_id = :vehicleId")
    fun getSessionCountByVehicleFlow(vehicleId: Long): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM sessions WHERE is_active = 1")
    suspend fun getActiveSessionCount(): Int
    
    @Query("SELECT COUNT(*) FROM sessions WHERE is_active = 1")
    fun getActiveSessionCountFlow(): Flow<Int>
    
    @Query("SELECT SUM(total_data_points) FROM sessions WHERE vehicle_id = :vehicleId")
    suspend fun getTotalDataPointsByVehicle(vehicleId: Long): Int?
    
    @Query("SELECT AVG(CASE WHEN end_time IS NOT NULL THEN end_time - start_time ELSE NULL END) FROM sessions WHERE vehicle_id = :vehicleId")
    suspend fun getAverageSessionDuration(vehicleId: Long): Long?
    
    @Query("SELECT SUM(distance_traveled) FROM sessions WHERE vehicle_id = :vehicleId AND distance_traveled IS NOT NULL")
    suspend fun getTotalDistanceByVehicle(vehicleId: Long): Double?
    
    @Query("SELECT MAX(max_speed) FROM sessions WHERE vehicle_id = :vehicleId AND max_speed IS NOT NULL")
    suspend fun getMaxSpeedByVehicle(vehicleId: Long): Double?
    
    @Query("SELECT AVG(avg_speed) FROM sessions WHERE vehicle_id = :vehicleId AND avg_speed IS NOT NULL")
    suspend fun getOverallAverageSpeedByVehicle(vehicleId: Long): Double?
    
    @Query("""
        SELECT connection_type,
               COUNT(*) as session_count,
               SUM(total_data_points) as total_data_points,
               AVG(CASE WHEN end_time IS NOT NULL THEN end_time - start_time ELSE NULL END) as avg_duration,
               SUM(distance_traveled) as total_distance,
               AVG(avg_speed) as avg_speed
        FROM sessions 
        WHERE vehicle_id = :vehicleId
        GROUP BY connection_type
        ORDER BY session_count DESC
    """)
    suspend fun getConnectionTypeStatistics(vehicleId: Long): List<ConnectionTypeStatistics>
    
    @Query("SELECT * FROM sessions WHERE vehicle_id = :vehicleId ORDER BY start_time DESC LIMIT 1")
    suspend fun getLastSessionForVehicle(vehicleId: Long): SessionEntity?
    
    @Query("SELECT MAX(start_time) FROM sessions WHERE vehicle_id = :vehicleId")
    suspend fun getLastSessionTimeForVehicle(vehicleId: Long): Long?
}

/**
 * Connection type statistics data class
 */
data class ConnectionTypeStatistics(
    @ColumnInfo(name = "connection_type") val connectionType: String,
    @ColumnInfo(name = "session_count") val sessionCount: Int,
    @ColumnInfo(name = "total_data_points") val totalDataPoints: Int,
    @ColumnInfo(name = "avg_duration") val avgDuration: Double?,
    @ColumnInfo(name = "total_distance") val totalDistance: Double?,
    @ColumnInfo(name = "avg_speed") val avgSpeed: Double?
)
