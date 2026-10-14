package com.example.wildliferescuer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.example.wildliferescuer.MainActivity
import com.example.wildliferescuer.R
import kotlinx.coroutines.*

class LocationTrackingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var stopTimeMillis: Long = 0
    
    companion object {
        private const val TAG = "LocationTrackingService"
        const val ACTION_START_TRACKING = "com.example.wildliferescuer.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.wildliferescuer.STOP_TRACKING"
        const val EXTRA_CASE_ID = "case_id"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_DURATION = "duration"
        const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1001
    }
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("LocationTrackingService", "Service onCreate()")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("LocationTrackingService", "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val caseId = intent.getStringExtra(EXTRA_CASE_ID) ?: return START_NOT_STICKY
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return START_NOT_STICKY
                val duration = intent.getStringExtra(EXTRA_DURATION) ?: "15min"
                
                android.util.Log.d("LocationTrackingService", "Starting tracking for case: $caseId, message: $messageId, duration: $duration")
                startTracking(caseId, messageId, duration)
            }
            ACTION_STOP_TRACKING -> {
                Log.d(TAG, "🛑 STOP_TRACKING action received, stopping service...")
                stopTracking()
            }
        }
        return START_STICKY
    }
    
    private fun startTracking(caseId: String, messageId: String, duration: String) {
        android.util.Log.d("LocationTrackingService", "startTracking called")
        // Calculate stop time
        val durationMillis = when (duration) {
            "15min" -> 15 * 60 * 1000L
            "1hour" -> 60 * 60 * 1000L
            "2hour" -> 2 * 60 * 60 * 1000L
            else -> 15 * 60 * 1000L
        }
        stopTimeMillis = System.currentTimeMillis() + durationMillis
        
        // Start foreground service
        val notification = createNotification(duration)
        startForeground(NOTIFICATION_ID, notification)
        
        // Request location updates
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L // Update every 30 seconds
        ).apply {
            setMinUpdateIntervalMillis(15000L) // At least 15 seconds between updates
            setWaitForAccurateLocation(false)
        }.build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(TAG, "onLocationResult called")
                val location = locationResult.lastLocation
                if (location == null) {
                    Log.w(TAG, "onLocationResult: location is null")
                    return
                }
                
                Log.d(TAG, "Got location: ${location.latitude}, ${location.longitude}")
                
                // Check if duration expired
                if (System.currentTimeMillis() >= stopTimeMillis) {
                    Log.d(TAG, "Duration expired, stopping tracking")
                    stopTracking()
                    return
                }
                
                // Update Firestore
                updateLocationInFirestore(caseId, messageId, location)
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopTracking()
        }
    }
    
    private fun updateLocationInFirestore(caseId: String, messageId: String, location: Location) {
        Log.d(TAG, "Updating Firestore: case=$caseId, message=$messageId, lat=${location.latitude}, lng=${location.longitude}")
        val db = FirebaseFirestore.getInstance()
        
        serviceScope.launch {
            try {
                db.collection("cases").document(caseId)
                    .collection("messages").document(messageId)
                    .update(
                        mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    ).addOnSuccessListener {
                        Log.d(TAG, "✅ Firestore update SUCCESS")
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "❌ Firestore update FAILED: ${e.message}", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in updateLocationInFirestore: ${e.message}", e)
            }
        }
    }
    
    private fun stopTracking() {
        Log.d(TAG, "stopTracking: Removing location updates and stopping service")
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "stopTracking: Service stopped")
    }
    
    private fun createNotification(duration: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1, // Different request code from the main intent
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sharing Live Location")
            .setContentText("Location sharing active for $duration. Tap to stop.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(
                0, // Use 0 for no icon to ensure action shows
                "Stop Sharing",
                stopPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Location sharing active for $duration. Tap 'Stop Sharing' button below to stop."))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when location is being shared"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
