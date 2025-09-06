package com.hurtec.obd2.diagnostics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hurtec.obd2.diagnostics.R
import com.hurtec.obd2.diagnostics.utils.CrashHandler
import dagger.hilt.android.AndroidEntryPoint

/**
 * Background service for OBD-II communication
 * Handles continuous monitoring and data collection
 */
@AndroidEntryPoint
class ObdService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "obd_service_channel"
        private const val CHANNEL_NAME = "OBD Service"
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.logInfo("ObdService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CrashHandler.logInfo("ObdService started")
        
        try {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            
            // Start OBD monitoring here if needed
            // For now, just keep the service alive
            
        } catch (e: Exception) {
            CrashHandler.handleException(e, "ObdService.onStartCommand")
        }
        
        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    override fun onDestroy() {
        super.onDestroy()
        CrashHandler.logInfo("ObdService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "OBD-II background monitoring"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hurtec OBD-II")
            .setContentText("Monitoring vehicle data")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
