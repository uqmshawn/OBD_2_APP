package com.hurtec.obd2.diagnostics.database.repository

import com.hurtec.obd2.diagnostics.database.HurtecObdDatabase
import com.hurtec.obd2.diagnostics.database.entities.ObdDataEntity
import com.hurtec.obd2.diagnostics.database.dao.PidStatistics
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for OBD data operations
 */
@Singleton
class ObdDataRepository @Inject constructor(
    private val database: HurtecObdDatabase
) {
    private val obdDataDao = database.obdDataDao()
    
    // ========== DATA STORAGE OPERATIONS ==========
    
    /**
     * Store OBD data
     */
    suspend fun storeObdData(obdData: ObdDataEntity): Result<Long> {
        return try {
            val dataId = obdDataDao.insertObdData(obdData)
            CrashHandler.logInfo("OBD data stored: PID ${obdData.pid} for vehicle ${obdData.vehicleId}")
            Result.success(dataId)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.storeObdData")
            Result.failure(e)
        }
    }
    
    /**
     * Store multiple OBD data entries in batch
     */
    suspend fun storeObdDataBatch(obdDataList: List<ObdDataEntity>): Result<List<Long>> {
        return try {
            val dataIds = obdDataDao.insertObdDataBatch(obdDataList)
            CrashHandler.logInfo("OBD data batch stored: ${obdDataList.size} entries")
            Result.success(dataIds)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.storeObdDataBatch")
            Result.failure(e)
        }
    }
    
    // ========== DATA RETRIEVAL OPERATIONS ==========
    
    /**
     * Get OBD data by vehicle
     */
    suspend fun getObdDataByVehicle(vehicleId: Long): Result<List<ObdDataEntity>> {
        return try {
            val data = obdDataDao.getObdDataByVehicle(vehicleId)
            Result.success(data)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getObdDataByVehicle")
            Result.failure(e)
        }
    }
    
    /**
     * Get OBD data by vehicle as Flow
     */
    fun getObdDataByVehicleFlow(vehicleId: Long): Flow<List<ObdDataEntity>> {
        return obdDataDao.getObdDataByVehicleFlow(vehicleId)
            .catch { e ->
                CrashHandler.handleException(e, "ObdDataRepository.getObdDataByVehicleFlow")
                emit(emptyList())
            }
    }
    
    /**
     * Get OBD data by session
     */
    suspend fun getObdDataBySession(sessionId: String): Result<List<ObdDataEntity>> {
        return try {
            val data = obdDataDao.getObdDataBySession(sessionId)
            Result.success(data)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getObdDataBySession")
            Result.failure(e)
        }
    }
    
    /**
     * Get OBD data by session as Flow
     */
    fun getObdDataBySessionFlow(sessionId: String): Flow<List<ObdDataEntity>> {
        return obdDataDao.getObdDataBySessionFlow(sessionId)
            .catch { e ->
                CrashHandler.handleException(e, "ObdDataRepository.getObdDataBySessionFlow")
                emit(emptyList())
            }
    }
    
    /**
     * Get OBD data by vehicle and PID
     */
    suspend fun getObdDataByVehicleAndPid(vehicleId: Long, pid: String): Result<List<ObdDataEntity>> {
        return try {
            val data = obdDataDao.getObdDataByVehicleAndPid(vehicleId, pid)
            Result.success(data)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getObdDataByVehicleAndPid")
            Result.failure(e)
        }
    }
    
    /**
     * Get OBD data by vehicle and PID as Flow
     */
    fun getObdDataByVehicleAndPidFlow(vehicleId: Long, pid: String): Flow<List<ObdDataEntity>> {
        return obdDataDao.getObdDataByVehicleAndPidFlow(vehicleId, pid)
            .catch { e ->
                CrashHandler.handleException(e, "ObdDataRepository.getObdDataByVehicleAndPidFlow")
                emit(emptyList())
            }
    }
    
    /**
     * Get recent OBD data by PID
     */
    suspend fun getRecentObdDataByPid(vehicleId: Long, pid: String, limit: Int = 100): Result<List<ObdDataEntity>> {
        return try {
            val data = obdDataDao.getRecentObdDataByPid(vehicleId, pid, limit)
            Result.success(data)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getRecentObdDataByPid")
            Result.failure(e)
        }
    }
    
    /**
     * Get OBD data by time range
     */
    suspend fun getObdDataByTimeRange(
        vehicleId: Long,
        startTime: Long,
        endTime: Long
    ): Result<List<ObdDataEntity>> {
        return try {
            val data = obdDataDao.getObdDataByTimeRange(vehicleId, startTime, endTime)
            Result.success(data)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getObdDataByTimeRange")
            Result.failure(e)
        }
    }
    
    /**
     * Get OBD data by time range as Flow
     */
    fun getObdDataByTimeRangeFlow(
        vehicleId: Long,
        startTime: Long,
        endTime: Long
    ): Flow<List<ObdDataEntity>> {
        return obdDataDao.getObdDataByTimeRangeFlow(vehicleId, startTime, endTime)
            .catch { e ->
                CrashHandler.handleException(e, "ObdDataRepository.getObdDataByTimeRangeFlow")
                emit(emptyList())
            }
    }
    
    // ========== LATEST DATA OPERATIONS ==========
    
    /**
     * Get latest OBD data by PID
     */
    suspend fun getLatestObdDataByPid(vehicleId: Long, pid: String): Result<ObdDataEntity?> {
        return try {
            val data = obdDataDao.getLatestObdDataByPid(vehicleId, pid)
            Result.success(data)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getLatestObdDataByPid")
            Result.failure(e)
        }
    }
    
    /**
     * Get latest OBD data by PID as Flow
     */
    fun getLatestObdDataByPidFlow(vehicleId: Long, pid: String): Flow<ObdDataEntity?> {
        return obdDataDao.getLatestObdDataByPidFlow(vehicleId, pid)
            .catch { e ->
                CrashHandler.handleException(e, "ObdDataRepository.getLatestObdDataByPidFlow")
                emit(null)
            }
    }
    
    /**
     * Get latest OBD data for all PIDs
     */
    suspend fun getLatestObdDataForAllPids(vehicleId: Long): Result<List<ObdDataEntity>> {
        return try {
            val data = obdDataDao.getLatestObdDataForAllPids(vehicleId)
            Result.success(data)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getLatestObdDataForAllPids")
            Result.failure(e)
        }
    }
    
    /**
     * Get latest OBD data for all PIDs as Flow
     */
    fun getLatestObdDataForAllPidsFlow(vehicleId: Long): Flow<List<ObdDataEntity>> {
        return obdDataDao.getLatestObdDataForAllPidsFlow(vehicleId)
            .catch { e ->
                CrashHandler.handleException(e, "ObdDataRepository.getLatestObdDataForAllPidsFlow")
                emit(emptyList())
            }
    }
    
    // ========== STATISTICS OPERATIONS ==========
    
    /**
     * Get PID statistics
     */
    suspend fun getPidStatistics(vehicleId: Long): Result<List<PidStatistics>> {
        return try {
            val stats = obdDataDao.getPidStatistics(vehicleId)
            Result.success(stats)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getPidStatistics")
            Result.failure(e)
        }
    }
    
    /**
     * Get available PIDs for vehicle
     */
    suspend fun getAvailablePids(vehicleId: Long): Result<List<String>> {
        return try {
            val pids = obdDataDao.getAvailablePids(vehicleId)
            Result.success(pids)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getAvailablePids")
            Result.failure(e)
        }
    }
    
    /**
     * Get OBD data count
     */
    suspend fun getObdDataCount(vehicleId: Long): Result<Int> {
        return try {
            val count = obdDataDao.getObdDataCount(vehicleId)
            Result.success(count)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.getObdDataCount")
            Result.failure(e)
        }
    }
    
    /**
     * Get OBD data count as Flow
     */
    fun getObdDataCountFlow(vehicleId: Long): Flow<Int> {
        return obdDataDao.getObdDataCountFlow(vehicleId)
            .catch { e ->
                CrashHandler.handleException(e, "ObdDataRepository.getObdDataCountFlow")
                emit(0)
            }
    }
    
    // ========== MAINTENANCE OPERATIONS ==========
    
    /**
     * Clean up old OBD data
     */
    suspend fun cleanupOldData(vehicleId: Long, retentionDays: Int): Result<Int> {
        return try {
            val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
            val deletedCount = obdDataDao.deleteOldObdDataForVehicle(vehicleId, cutoffTime)
            
            if (deletedCount > 0) {
                CrashHandler.logInfo("Cleaned up $deletedCount old OBD data entries for vehicle $vehicleId")
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.cleanupOldData")
            Result.failure(e)
        }
    }
    
    /**
     * Limit data records per vehicle
     */
    suspend fun limitDataRecords(vehicleId: Long, maxRecords: Int): Result<Int> {
        return try {
            val deletedCount = obdDataDao.limitDataRecords(vehicleId, maxRecords)
            
            if (deletedCount > 0) {
                CrashHandler.logInfo("Limited OBD data to $maxRecords records for vehicle $vehicleId, deleted $deletedCount entries")
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.limitDataRecords")
            Result.failure(e)
        }
    }
    
    /**
     * Delete OBD data by session
     */
    suspend fun deleteObdDataBySession(sessionId: String): Result<Int> {
        return try {
            val deletedCount = obdDataDao.deleteObdDataBySession(sessionId)
            
            if (deletedCount > 0) {
                CrashHandler.logInfo("Deleted $deletedCount OBD data entries for session $sessionId")
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdDataRepository.deleteObdDataBySession")
            Result.failure(e)
        }
    }
}
