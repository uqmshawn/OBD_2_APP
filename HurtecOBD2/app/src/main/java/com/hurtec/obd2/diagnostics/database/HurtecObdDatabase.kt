package com.hurtec.obd2.diagnostics.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hurtec.obd2.diagnostics.database.dao.ObdDataDao
import com.hurtec.obd2.diagnostics.database.dao.SessionDao
import com.hurtec.obd2.diagnostics.database.dao.VehicleDao
import com.hurtec.obd2.diagnostics.database.entities.ObdDataEntity
import com.hurtec.obd2.diagnostics.database.entities.SessionEntity
import com.hurtec.obd2.diagnostics.database.entities.VehicleEntity
import com.hurtec.obd2.diagnostics.utils.CrashHandler

/**
 * Room database for Hurtec OBD-II app
 */
@Database(
    entities = [
        VehicleEntity::class,
        ObdDataEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class HurtecObdDatabase : RoomDatabase() {
    
    abstract fun vehicleDao(): VehicleDao
    abstract fun obdDataDao(): ObdDataDao
    abstract fun sessionDao(): SessionDao
    
    companion object {
        private const val DATABASE_NAME = "hurtec_obd_database"
        
        @Volatile
        private var INSTANCE: HurtecObdDatabase? = null
        
        /**
         * Get database instance (singleton)
         */
        fun getDatabase(context: Context): HurtecObdDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HurtecObdDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // For development only
                    .addCallback(DatabaseCallback())
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Migration from version 1 to 2 (placeholder for future use)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Future migration logic will go here
                CrashHandler.logInfo("Database migration 1->2 executed")
            }
        }
        
        /**
         * Database callback for initialization
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CrashHandler.logInfo("Hurtec OBD Database created successfully")
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                CrashHandler.logInfo("Hurtec OBD Database opened")
            }
        }
        
        /**
         * Close database instance (for testing)
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
    
    /**
     * Perform database maintenance
     */
    suspend fun performMaintenance(retentionDays: Int = 30, maxRecordsPerVehicle: Int = 10000) {
        try {
            CrashHandler.logInfo("Starting database maintenance...")
            
            val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
            
            // Clean up old OBD data
            val deletedObdData = obdDataDao().deleteOldObdDataForVehicle(-1, cutoffTime)
            CrashHandler.logInfo("Deleted $deletedObdData old OBD data records")
            
            // Clean up old sessions
            val deletedSessions = sessionDao().deleteOldSessions(cutoffTime)
            CrashHandler.logInfo("Deleted $deletedSessions old sessions")
            
            // Limit data records per vehicle
            val vehicles = vehicleDao().getAllVehicles()
            for (vehicle in vehicles) {
                val limitedRecords = obdDataDao().limitDataRecords(vehicle.id, maxRecordsPerVehicle)
                if (limitedRecords > 0) {
                    CrashHandler.logInfo("Limited ${vehicle.name} to $maxRecordsPerVehicle records, deleted $limitedRecords")
                }
            }
            
            CrashHandler.logInfo("Database maintenance completed successfully")
        } catch (e: Exception) {
            CrashHandler.handleException(e, "HurtecObdDatabase.performMaintenance")
        }
    }
    
    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): DatabaseStats {
        return try {
            DatabaseStats(
                vehicleCount = vehicleDao().getVehicleCount(),
                sessionCount = sessionDao().getSessionCount(),
                obdDataCount = obdDataDao().getObdDataCount(-1), // -1 for all vehicles
                activeSessionCount = sessionDao().getActiveSessionCount(),
                databaseSizeBytes = getDatabaseSize()
            )
        } catch (e: Exception) {
            CrashHandler.handleException(e, "HurtecObdDatabase.getDatabaseStats")
            DatabaseStats()
        }
    }
    
    /**
     * Get database file size
     */
    private fun getDatabaseSize(): Long {
        return try {
            val dbFile = openHelper.readableDatabase.path?.let { java.io.File(it) }
            dbFile?.length() ?: 0L
        } catch (e: Exception) {
            CrashHandler.handleException(e, "HurtecObdDatabase.getDatabaseSize")
            0L
        }
    }
    
    /**
     * Export database to JSON (for backup)
     */
    suspend fun exportToJson(): String {
        return try {
            val vehicles = vehicleDao().getAllVehicles()
            val sessions = sessionDao().getRecentSessions(1000)

            // Simple JSON export without kotlinx.serialization for now
            val json = StringBuilder()
            json.append("{")
            json.append("\"vehicles\": ${vehicles.size},")
            json.append("\"sessions\": ${sessions.size},")
            json.append("\"exportTime\": ${System.currentTimeMillis()},")
            json.append("\"version\": 1")
            json.append("}")

            json.toString()
        } catch (e: Exception) {
            CrashHandler.handleException(e, "HurtecObdDatabase.exportToJson")
            "{\"error\": \"Export failed: ${e.message}\"}"
        }
    }
}

/**
 * Database statistics data class
 */
data class DatabaseStats(
    val vehicleCount: Int = 0,
    val sessionCount: Int = 0,
    val obdDataCount: Int = 0,
    val activeSessionCount: Int = 0,
    val databaseSizeBytes: Long = 0L
) {
    fun getFormattedSize(): String {
        return when {
            databaseSizeBytes < 1024 -> "$databaseSizeBytes B"
            databaseSizeBytes < 1024 * 1024 -> "${databaseSizeBytes / 1024} KB"
            databaseSizeBytes < 1024 * 1024 * 1024 -> "${databaseSizeBytes / (1024 * 1024)} MB"
            else -> "${databaseSizeBytes / (1024 * 1024 * 1024)} GB"
        }
    }
}

/**
 * Database export data class
 */
data class DatabaseExport(
    val vehicleCount: Int,
    val sessionCount: Int,
    val obdDataCount: Int,
    val exportTime: Long,
    val version: Int
)

/**
 * Type converters for Room database
 */
class DatabaseConverters {

    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }

    @androidx.room.TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @androidx.room.TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }
}
