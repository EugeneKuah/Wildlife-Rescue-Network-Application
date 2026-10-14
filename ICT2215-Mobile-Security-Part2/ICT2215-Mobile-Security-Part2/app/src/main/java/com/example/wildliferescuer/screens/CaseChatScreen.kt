package com.example.wildliferescuer.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.wildliferescuer.models.RescueCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import com.example.wildliferescuer.services.LocationTrackingService
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class ChatMessage(
    val id: String = "",
    val message: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val type: String = "text", // "text", "image", "location"
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationDuration: String? = null, // "15min", "1hour", "2hour"
    val lastUpdated: com.google.firebase.Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseChatScreen(
    caseId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    
    var rescueCase by remember { mutableStateOf<RescueCase?>(null) }
    var isLoadingCase by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var newMessage by remember { mutableStateOf("") }
    var isSendingMessage by remember { mutableStateOf(false) }
    
    var currentUserName by remember { mutableStateOf("User") }
    var currentUserRole by remember { mutableStateOf("USER") }
    
    var selectedTab by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Track location sharing state
    var isLocationSharingActive by remember { mutableStateOf(false) }
    var activeLocationMessageId by remember { mutableStateOf<String?>(null) }
    val isCaseClosed = remember(rescueCase) {
        rescueCase?.status?.trim()?.equals("Closed", ignoreCase = true) == true
    }
    
    // Check if current user can view chat
    val canViewChat = remember(rescueCase, auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        val case = rescueCase
        uid != null && case != null && (
            case.createdBy == uid || // Case creator can always view chat
            case.assignedTo == uid   // Assigned team member can view chat
        )
    }
    val canSendMessages = remember(canViewChat, isCaseClosed) {
        canViewChat && !isCaseClosed
    }
    
    // Load current user info
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    currentUserName = doc.getString("name") ?: "User"
                    currentUserRole = doc.getString("role") ?: "USER"
                }
        }
    }
    
    // Load case details with real-time updates
    LaunchedEffect(caseId) {
        db.collection("cases").document(caseId)
            .addSnapshotListener { doc, e ->
                if (e != null) {
                    errorMessage = e.message ?: "Failed to load case"
                    isLoadingCase = false
                    return@addSnapshotListener
                }
                
                if (doc != null && doc.exists()) {
                    rescueCase = RescueCase(
                        id = doc.id,
                        species = doc.getString("species") ?: "",
                        urgency = doc.getString("urgency") ?: "",
                        location = doc.getString("location") ?: "",
                        description = doc.getString("description") ?: "",
                        status = doc.getString("status") ?: "Open",
                        createdBy = doc.getString("createdBy") ?: "",
                        createdAt = doc.getTimestamp("createdAt"),
                        lat = doc.getDouble("lat"),
                        lng = doc.getDouble("lng"),
                        assignedTo = doc.getString("assignedTo"),
                        assignedToName = doc.getString("assignedToName")
                    )
                }
                isLoadingCase = false
            }
    }
    
    // Load messages only if user has access
    LaunchedEffect(caseId, canViewChat) {
        if (!canViewChat) {
            messages = emptyList()
            return@LaunchedEffect
        }
        
        db.collection("cases").document(caseId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                messages = snapshot?.documents?.mapNotNull { doc ->
                    ChatMessage(
                        id = doc.id,
                        message = doc.getString("message") ?: "",
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "Unknown",
                        timestamp = doc.getTimestamp("timestamp"),
                        type = doc.getString("type") ?: "text",
                        imageUrl = doc.getString("imageUrl"),
                        latitude = doc.getDouble("latitude"),
                        longitude = doc.getDouble("longitude"),
                        locationDuration = doc.getString("locationDuration"),
                        lastUpdated = doc.getTimestamp("lastUpdated")
                    )
                } ?: emptyList()
            }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && selectedTab == 1) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Case Details") },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                if (rescueCase != null) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Details") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { if (canViewChat) selectedTab = 1 },
                            enabled = canViewChat,
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Chat")
                                    if (messages.isNotEmpty() && canViewChat) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge { Text("${messages.size}") }
                                    }
                                    if (!canViewChat) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("🔒", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            isLoadingCase -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", style = MaterialTheme.typography.displayMedium)
                        Text(
                            text = errorMessage ?: "Error loading case",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            rescueCase != null -> {
                        when (selectedTab) {
                    0 -> CaseDetailsTab(
                        rescueCase = rescueCase!!,
                        padding = padding,
                        canUpdateStatus = currentUserRole == "RESCUE_TEAM" && rescueCase!!.assignedTo == auth.currentUser?.uid,
                        onStatusChange = { newStatus ->
                            db.collection("cases")
                                .document(caseId)
                                .update("status", newStatus)
                        }
                    )
                    1 -> {
                        if (canViewChat) {
                            ChatTab(
                                messages = messages,
                                newMessage = newMessage,
                                onMessageChange = { newMessage = it },
                                isSending = isSendingMessage,
                                currentUserId = auth.currentUser?.uid ?: "",
                                isLocationSharingActive = isLocationSharingActive,
                                isCaseClosed = isCaseClosed,
                                onStopSharing = {
                                    // Stop the service
                                    val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
                                        action = LocationTrackingService.ACTION_STOP_TRACKING
                                    }
                                    context.stopService(stopIntent)
                                    isLocationSharingActive = false
                                    activeLocationMessageId = null
                                },
                                onSend = {
                                    if (!canSendMessages) return@ChatTab
                                    if (newMessage.isBlank()) return@ChatTab
                                    
                                    isSendingMessage = true
                                    val uid = auth.currentUser?.uid ?: return@ChatTab
                                    
                                    val messageData = hashMapOf(
                                        "message" to newMessage.trim(),
                                        "senderId" to uid,
                                        "senderName" to currentUserName,
                                        "timestamp" to FieldValue.serverTimestamp(),
                                        "type" to "text"
                                    )
                                    
                                    db.collection("cases").document(caseId)
                                        .collection("messages")
                                        .add(messageData)
                                        .addOnSuccessListener {
                                            newMessage = ""
                                            isSendingMessage = false
                                        }
                                        .addOnFailureListener {
                                            isSendingMessage = false
                                        }
                                },
                                onSendImage = { imageUri, caption ->
                                    if (!canSendMessages) {
                                        isSendingMessage = false
                                        return@ChatTab
                                    }

                                    isSendingMessage = true
                                    scope.launch {
                                        try {
                                            val uid = auth.currentUser?.uid ?: return@launch
                                            
                                            // Convert image to base64
                                            val inputStream = context.contentResolver.openInputStream(imageUri)
                                            val bitmap = BitmapFactory.decodeStream(inputStream)
                                            inputStream?.close()
                                            
                                            // Compress bitmap to reduce size
                                            val outputStream = ByteArrayOutputStream()
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                                            val imageBytes = outputStream.toByteArray()
                                            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                                            
                                            val messageData = hashMapOf(
                                                "message" to caption,
                                                "senderId" to uid,
                                                "senderName" to currentUserName,
                                                "timestamp" to FieldValue.serverTimestamp(),
                                                "type" to "image",
                                                "imageUrl" to base64Image
                                            )
                                            
                                            db.collection("cases").document(caseId)
                                                .collection("messages")
                                                .add(messageData)
                                                .await()
                                            
                                            newMessage = ""
                                            isSendingMessage = false
                                        } catch (e: Exception) {
                                            isSendingMessage = false
                                            // Handle error
                                        }
                                    }
                                },
                                onSendLocation = { lat, lng, duration ->
                                    if (!canSendMessages) return@ChatTab
                                    val uid = auth.currentUser?.uid ?: return@ChatTab
                                    
                                    scope.launch {
                                        try {
                                            val messageData = hashMapOf(
                                                "message" to "Sharing live location",
                                                "senderId" to uid,
                                                "senderName" to currentUserName,
                                                "timestamp" to FieldValue.serverTimestamp(),
                                                "type" to "location",
                                                "latitude" to lat,
                                                "longitude" to lng,
                                                "locationDuration" to duration,
                                                "lastUpdated" to FieldValue.serverTimestamp()
                                            )
                                            
                                            val docRef = db.collection("cases").document(caseId)
                                                .collection("messages")
                                                .add(messageData)
                                                .await()
                                            
                                            // Start location tracking service
                                            try {
                                                val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                                                    action = LocationTrackingService.ACTION_START_TRACKING
                                                    putExtra(LocationTrackingService.EXTRA_CASE_ID, caseId)
                                                    putExtra(LocationTrackingService.EXTRA_MESSAGE_ID, docRef.id)
                                                    putExtra(LocationTrackingService.EXTRA_DURATION, duration)
                                                }
                                                
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                    context.startForegroundService(serviceIntent)
                                                } else {
                                                    context.startService(serviceIntent)
                                                }
                                                
                                                // Update UI state
                                                isLocationSharingActive = true
                                                activeLocationMessageId = docRef.id
                                                
                                                android.util.Log.d("CaseChat", "Started LocationTrackingService for case: $caseId, message: ${docRef.id}")
                                            } catch (e: Exception) {
                                                android.util.Log.e("CaseChat", "Failed to start LocationTrackingService", e)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("CaseChat", "Failed to create location message", e)
                                        }
                                    }
                                },
                                padding = padding,
                                listState = listState
                            )
                        } else {
                            // Show access denied message
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text("🔒", style = MaterialTheme.typography.displayMedium)
                                    Text(
                                        "Chat Access Restricted",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (isCaseClosed)
                                            "This case is closed. No further messages are allowed."
                                        else if (rescueCase!!.assignedTo != null)
                                            "This case is assigned to rescue team member ${rescueCase!!.assignedToName}. Only they can communicate with the reporter."
                                        else
                                            "You can only view case details. Assign the case to yourself from the admin dashboard to access chat.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CaseDetailsTab(
    rescueCase: RescueCase,
    padding: PaddingValues,
    canUpdateStatus: Boolean,
    onStatusChange: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with species and status
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = when (rescueCase.urgency) {
                    "Emergency" -> MaterialTheme.colorScheme.errorContainer
                    "High" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            rescueCase.species,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (rescueCase.urgency) {
                                    "Emergency" -> "🚨"
                                    "High" -> "⚠️"
                                    "Moderate" -> "📍"
                                    else -> "ℹ️"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${rescueCase.urgency} Priority",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                        Surface(
                            color = when (rescueCase.status) {
                                "Open" -> MaterialTheme.colorScheme.primary
                                "In Progress" -> MaterialTheme.colorScheme.tertiary
                                "Resolved" -> MaterialTheme.colorScheme.secondary
                                "Closed" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                        Text(
                            text = rescueCase.status,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Status
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚩", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Case Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (canUpdateStatus) {
                    var expanded by remember { mutableStateOf(false) }
                    val statuses = listOf("Open", "In Progress", "Resolved", "Closed")

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = rescueCase.status,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Current Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    enabled = status != rescueCase.status,
                                    onClick = {
                                        expanded = false
                                        onStatusChange(status)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        rescueCase.status,
                        style = MaterialTheme.typography.titleMedium,
                        color = when (rescueCase.status) {
                            "Closed" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Location Card
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📍", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    rescueCase.location,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                if (rescueCase.lat != null && rescueCase.lng != null) {
                    HorizontalDivider()
                    Text(
                        "Coordinates: ${String.format("%.6f", rescueCase.lat)}, ${String.format("%.6f", rescueCase.lng)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    FilledTonalButton(
                        onClick = {
                            val uri = Uri.parse("geo:${rescueCase.lat},${rescueCase.lng}?q=${rescueCase.lat},${rescueCase.lng}(Rescue Case)")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗺️ Open in Google Maps")
                    }
                }
            }
        }
        
        // Description Card
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📝", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    rescueCase.description.ifEmpty { "No description provided" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (rescueCase.description.isEmpty()) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Assigned To Card
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👤", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Assigned To Rescue Team Member:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        rescueCase.assignedToName ?: "Unassigned",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Timestamp Card
        rescueCase.createdAt?.let { timestamp ->
            val formatter = SimpleDateFormat("EEEE, MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏰", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Created",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatter.format(timestamp.toDate()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatTab(
    messages: List<ChatMessage>,
    newMessage: String,
    onMessageChange: (String) -> Unit,
    isSending: Boolean,
    currentUserId: String,
    isLocationSharingActive: Boolean,
    isCaseClosed: Boolean,
    onStopSharing: () -> Unit,
    onSend: () -> Unit,
    onSendImage: (Uri, String) -> Unit,
    onSendLocation: (Double, Double, String) -> Unit,
    padding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Camera permission
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Location permission
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    // Notification permission for Android 13+ (required for foreground service)
    val notificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { attachedImageUri = it }
    }
    
    // Camera launcher
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            attachedImageUri = photoUri
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Messages list
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("💬", style = MaterialTheme.typography.displayMedium)
                    Text(
                        "No messages yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Start the conversation below",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages.size) { index ->
                    MessageBubble(
                        message = messages[index],
                        isCurrentUser = messages[index].senderId == currentUserId
                    )
                }
            }
        }
        
        // Location sharing banner
        if (isLocationSharingActive) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Sharing live location...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    FilledTonalButton(
                        onClick = onStopSharing,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
        
        // Message input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                if (isCaseClosed) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "This case is closed. No further messages are allowed.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Image preview
                if (attachedImageUri != null) {
                    Card(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(120.dp)
                    ) {
                        Box {
                            Image(
                                painter = rememberAsyncImagePainter(attachedImageUri),
                                contentDescription = "Attached image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { attachedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        "✕",
                                        modifier = Modifier.padding(4.dp),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Attachment button
                    IconButton(
                        onClick = { showAttachmentMenu = !showAttachmentMenu },
                        enabled = !isCaseClosed,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Attach"
                        )
                    }
                    
                    OutlinedTextField(
                        value = newMessage,
                        onValueChange = onMessageChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        enabled = !isSending && !isCaseClosed,
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )
                    
                    FilledIconButton(
                        onClick = {
                            if (isCaseClosed) return@FilledIconButton

                            if (attachedImageUri != null) {
                                onSendImage(attachedImageUri!!, newMessage)
                                attachedImageUri = null
                            } else {
                                onSend()
                            }
                        },
                        enabled = !isSending && (newMessage.isNotBlank() || attachedImageUri != null) && !isCaseClosed,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
                
                // Attachment menu
                if (showAttachmentMenu) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Camera button
                        OutlinedButton(
                            enabled = !isCaseClosed,
                            onClick = {
                                showAttachmentMenu = false
                                if (cameraPermission.status.isGranted) {
                                    photoUri = FileProvider.getUriForFile(
                                        context,
                                        context.packageName + ".fileprovider",
                                        File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                                    )
                                    cameraLauncher.launch(photoUri!!)
                                } else {
                                    cameraPermission.launchPermissionRequest()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Camera")
                        }
                        
                        // Gallery button
                        OutlinedButton(
                            enabled = !isCaseClosed,
                            onClick = {
                                showAttachmentMenu = false
                                imagePickerLauncher.launch("image/*")
                            }
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Gallery")
                        }
                        
                        // Location button
                        OutlinedButton(
                            enabled = !isCaseClosed,
                            onClick = {
                                showAttachmentMenu = false
                                // Check notification permission first (Android 13+)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    if (notificationPermission?.status?.isGranted == true) {
                                        if (locationPermission.status.isGranted) {
                                            showLocationDialog = true
                                        } else {
                                            locationPermission.launchPermissionRequest()
                                        }
                                    } else {
                                        notificationPermission?.launchPermissionRequest()
                                    }
                                } else {
                                    if (locationPermission.status.isGranted) {
                                        showLocationDialog = true
                                    } else {
                                        locationPermission.launchPermissionRequest()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Location")
                        }
                    }
                }
            }
        }
        
        // Location duration dialog
        if (showLocationDialog) {
            AlertDialog(
                onDismissRequest = { showLocationDialog = false },
                title = { Text("Share Location") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("How long would you like to share your location?")
                        
                        OutlinedButton(
                            enabled = !isCaseClosed,
                            onClick = {
                                showLocationDialog = false
                                scope.launch {
                                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                    try {
                                        val location = fusedLocationClient.lastLocation.await()
                                        if (location != null) {
                                            onSendLocation(location.latitude, location.longitude, "15min")
                                        }
                                    } catch (e: SecurityException) {
                                        // Handle permission denied
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("15 Minutes")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                showLocationDialog = false
                                scope.launch {
                                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                    try {
                                        val location = fusedLocationClient.lastLocation.await()
                                        if (location != null) {
                                            onSendLocation(location.latitude, location.longitude, "1hour")
                                        }
                                    } catch (e: SecurityException) {
                                        // Handle permission denied
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("1 Hour")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                showLocationDialog = false
                                scope.launch {
                                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                    try {
                                        val location = fusedLocationClient.lastLocation.await()
                                        if (location != null) {
                                            onSendLocation(location.latitude, location.longitude, "2hour")
                                        }
                                    } catch (e: SecurityException) {
                                        // Handle permission denied
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("2 Hours")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLocationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isCurrentUser) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isCurrentUser) {
                    Text(
                        message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Display content based on message type
                when (message.type) {
                    "image" -> {
                        message.imageUrl?.let { base64Image ->
                            // Decode base64 to bitmap
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            
                            Image(
                                painter = rememberAsyncImagePainter(bitmap),
                                contentDescription = "Shared image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                contentScale = ContentScale.Crop
                            )
                            if (message.message.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    message.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    "location" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Live Location",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Embedded OpenStreetMap
                            if (message.latitude != null && message.longitude != null) {
                                val position = remember(message.latitude, message.longitude) {
                                    GeoPoint(message.latitude, message.longitude)
                                }
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            Configuration.getInstance().load(
                                                ctx,
                                                ctx.getSharedPreferences("osmdroid", 0)
                                            )
                                            Configuration.getInstance().userAgentValue = ctx.packageName
                                            
                                            MapView(ctx).apply {
                                                setTileSource(TileSourceFactory.MAPNIK)
                                                setMultiTouchControls(true)
                                                controller.setZoom(15.0)
                                                controller.setCenter(position)
                                                
                                                // Add marker
                                                val marker = Marker(this).apply {
                                                    this.position = position
                                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                    title = "Current Location"
                                                    snippet = message.lastUpdated?.toDate()?.let { 
                                                        SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(it) 
                                                    } ?: "now"
                                                }
                                                overlays.add(marker)
                                            }
                                        },
                                        update = { mapView ->
                                            // Update marker position when location changes
                                            mapView.overlays.clear()
                                            val marker = Marker(mapView).apply {
                                                this.position = position
                                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                title = "Current Location"
                                                snippet = message.lastUpdated?.toDate()?.let { 
                                                    SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(it) 
                                                } ?: "now"
                                            }
                                            mapView.overlays.add(marker)
                                            mapView.controller.animateTo(position)
                                            mapView.invalidate()
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                
                                message.locationDuration?.let { duration ->
                                    Text(
                                        "Sharing for: ${duration.replace("min", " minutes").replace("hour", " hour(s)")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                message.lastUpdated?.let { lastUpdate ->
                                    val formatter = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
                                    Text(
                                        "📍 Last updated: ${formatter.format(lastUpdate.toDate())}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    "${String.format("%.6f", message.latitude)}, ${String.format("%.6f", message.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            message.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                message.timestamp?.let { timestamp ->
                    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                    Text(
                        formatter.format(timestamp.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
