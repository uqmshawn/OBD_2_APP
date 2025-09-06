package com.hurtec.obd2.diagnostics.database.repository

import com.hurtec.obd2.diagnostics.database.HurtecObdDatabase
import com.hurtec.obd2.diagnostics.database.entities.VehicleEntity
import com.hurtec.obd2.diagnostics.database.dao.VehicleWithStats
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for vehicle data operations
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val database: HurtecObdDatabase
) {
    private val vehicleDao = database.vehicleDao()
    
    // ========== VEHICLE OPERATIONS ==========
    
    /**
     * Add a new vehicle
     */
    suspend fun addVehicle(vehicle: VehicleEntity): Result<Long> {
        return try {
            val vehicleId = vehicleDao.insertVehicle(vehicle)
            CrashHandler.logInfo("Vehicle added successfully: ${vehicle.name} (ID: $vehicleId)")
            Result.success(vehicleId)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.addVehicle")
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing vehicle
     */
    suspend fun updateVehicle(vehicle: VehicleEntity): Result<Boolean> {
        return try {
            val updatedRows = vehicleDao.updateVehicle(vehicle.copy(updatedAt = System.currentTimeMillis()))
            val success = updatedRows > 0
            if (success) {
                CrashHandler.logInfo("Vehicle updated successfully: ${vehicle.name}")
            }
            Result.success(success)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.updateVehicle")
            Result.failure(e)
        }
    }
    
    /**
     * Delete a vehicle
     */
    suspend fun deleteVehicle(vehicleId: Long): Result<Boolean> {
        return try {
            val deletedRows = vehicleDao.deleteVehicleById(vehicleId)
            val success = deletedRows > 0
            if (success) {
                CrashHandler.logInfo("Vehicle deleted successfully: ID $vehicleId")
            }
            Result.success(success)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.deleteVehicle")
            Result.failure(e)
        }
    }
    
    /**
     * Get vehicle by ID
     */
    suspend fun getVehicle(vehicleId: Long): Result<VehicleEntity?> {
        return try {
            val vehicle = vehicleDao.getVehicleById(vehicleId)
            Result.success(vehicle)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.getVehicle")
            Result.failure(e)
        }
    }
    
    /**
     * Get vehicle by ID as Flow
     */
    fun getVehicleFlow(vehicleId: Long): Flow<VehicleEntity?> {
        return vehicleDao.getVehicleByIdFlow(vehicleId)
            .catch { e ->
                CrashHandler.handleException(e, "VehicleRepository.getVehicleFlow")
                emit(null)
            }
    }
    
    /**
     * Get all vehicles
     */
    suspend fun getAllVehicles(): Result<List<VehicleEntity>> {
        return try {
            val vehicles = vehicleDao.getAllVehicles()
            Result.success(vehicles)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.getAllVehicles")
            Result.failure(e)
        }
    }
    
    /**
     * Get all vehicles as Flow
     */
    fun getAllVehiclesFlow(): Flow<List<VehicleEntity>> {
        return vehicleDao.getAllVehiclesFlow()
            .catch { e ->
                CrashHandler.handleException(e, "VehicleRepository.getAllVehiclesFlow")
                emit(emptyList())
            }
    }
    
    /**
     * Get all vehicles with statistics
     */
    suspend fun getAllVehiclesWithStats(): Result<List<VehicleWithStats>> {
        return try {
            val vehicles = vehicleDao.getAllVehiclesWithStats()
            Result.success(vehicles)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.getAllVehiclesWithStats")
            Result.failure(e)
        }
    }
    
    /**
     * Get all vehicles with statistics as Flow
     */
    fun getAllVehiclesWithStatsFlow(): Flow<List<VehicleWithStats>> {
        return vehicleDao.getAllVehiclesWithStatsFlow()
            .catch { e ->
                CrashHandler.handleException(e, "VehicleRepository.getAllVehiclesWithStatsFlow")
                emit(emptyList())
            }
    }
    
    // ========== ACTIVE VEHICLE OPERATIONS ==========
    
    /**
     * Get the currently active vehicle
     */
    suspend fun getActiveVehicle(): Result<VehicleEntity?> {
        return try {
            val vehicle = vehicleDao.getActiveVehicle()
            Result.success(vehicle)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.getActiveVehicle")
            Result.failure(e)
        }
    }
    
    /**
     * Get the currently active vehicle as Flow
     */
    fun getActiveVehicleFlow(): Flow<VehicleEntity?> {
        return vehicleDao.getActiveVehicleFlow()
            .catch { e ->
                CrashHandler.handleException(e, "VehicleRepository.getActiveVehicleFlow")
                emit(null)
            }
    }
    
    /**
     * Set a vehicle as active
     */
    suspend fun setActiveVehicle(vehicleId: Long): Result<Boolean> {
        return try {
            // First deactivate all vehicles
            vehicleDao.deactivateAllVehicles()
            
            // Then activate the selected vehicle
            val updatedRows = vehicleDao.setActiveVehicle(vehicleId)
            val success = updatedRows > 0
            
            if (success) {
                CrashHandler.logInfo("Active vehicle set: ID $vehicleId")
            }
            
            Result.success(success)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.setActiveVehicle")
            Result.failure(e)
        }
    }
    
    // ========== SEARCH OPERATIONS ==========
    
    /**
     * Search vehicles by query
     */
    suspend fun searchVehicles(query: String): Result<List<VehicleEntity>> {
        return try {
            val vehicles = vehicleDao.searchVehicles(query)
            Result.success(vehicles)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.searchVehicles")
            Result.failure(e)
        }
    }
    
    /**
     * Search vehicles by query as Flow
     */
    fun searchVehiclesFlow(query: String): Flow<List<VehicleEntity>> {
        return vehicleDao.searchVehiclesFlow(query)
            .catch { e ->
                CrashHandler.handleException(e, "VehicleRepository.searchVehiclesFlow")
                emit(emptyList())
            }
    }
    
    // ========== VEHICLE MAINTENANCE OPERATIONS ==========
    
    /**
     * Update vehicle's last connected time
     */
    suspend fun updateLastConnectedTime(vehicleId: Long): Result<Boolean> {
        return try {
            val timestamp = System.currentTimeMillis()
            val updatedRows = vehicleDao.updateLastConnectedTime(vehicleId, timestamp)
            val success = updatedRows > 0
            
            if (success) {
                CrashHandler.logInfo("Last connected time updated for vehicle ID: $vehicleId")
            }
            
            Result.success(success)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.updateLastConnectedTime")
            Result.failure(e)
        }
    }
    
    /**
     * Update vehicle's supported PIDs
     */
    suspend fun updateSupportedPids(vehicleId: Long, supportedPids: List<String>): Result<Boolean> {
        return try {
            val pidsString = supportedPids.joinToString(",")
            val updatedRows = vehicleDao.updateSupportedPids(vehicleId, pidsString)
            val success = updatedRows > 0
            
            if (success) {
                CrashHandler.logInfo("Supported PIDs updated for vehicle ID: $vehicleId (${supportedPids.size} PIDs)")
            }
            
            Result.success(success)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.updateSupportedPids")
            Result.failure(e)
        }
    }
    
    /**
     * Update vehicle's OBD protocol
     */
    suspend fun updateObdProtocol(vehicleId: Long, protocol: String): Result<Boolean> {
        return try {
            val updatedRows = vehicleDao.updateObdProtocol(vehicleId, protocol)
            val success = updatedRows > 0
            
            if (success) {
                CrashHandler.logInfo("OBD protocol updated for vehicle ID: $vehicleId to $protocol")
            }
            
            Result.success(success)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.updateObdProtocol")
            Result.failure(e)
        }
    }
    
    // ========== STATISTICS OPERATIONS ==========
    
    /**
     * Get vehicle count
     */
    suspend fun getVehicleCount(): Result<Int> {
        return try {
            val count = vehicleDao.getVehicleCount()
            Result.success(count)
        } catch (e: Exception) {
            CrashHandler.handleException(e, "VehicleRepository.getVehicleCount")
            Result.failure(e)
        }
    }
    
    /**
     * Get vehicle count as Flow
     */
    fun getVehicleCountFlow(): Flow<Int> {
        return vehicleDao.getVehicleCountFlow()
            .catch { e ->
                CrashHandler.handleException(e, "VehicleRepository.getVehicleCountFlow")
                emit(0)
            }
    }
}
