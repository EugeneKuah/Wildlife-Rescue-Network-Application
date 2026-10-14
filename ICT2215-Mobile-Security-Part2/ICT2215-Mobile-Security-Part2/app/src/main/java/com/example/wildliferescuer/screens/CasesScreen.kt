package com.example.wildliferescuer.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.wildliferescuer.models.RescueCase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesScreen(
    onBack: () -> Unit,
    onOpenCase: (String) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    
    var cases by remember { mutableStateOf<List<RescueCase>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUserRole by remember { mutableStateOf<String?>(null) }
    
    var filterStatus by remember { mutableStateOf("All") }
    val statusOptions = listOf("All", "Open", "In Progress", "Resolved", "Closed")
    
    // Load current user role
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    currentUserRole = doc.getString("role") ?: "USER"
                }
        }
    }
    
    // Fetch cases from Firestore based on role
    LaunchedEffect(filterStatus, currentUserRole) {
        if (currentUserRole == null) return@LaunchedEffect
        
        isLoading = true
        errorMessage = null
        
        val uid = auth.currentUser?.uid
        var query: Query = db.collection("cases")
        
        // Role-based filtering: Users see only their cases, Rescue_Team sees all
        if (currentUserRole == "USER" && uid != null) {
            query = query.whereEqualTo("createdBy", uid)
        }
        
        if (filterStatus != "All") {
            query = query.whereEqualTo("status", filterStatus)
        }
        
        query.get()
            .addOnSuccessListener { snapshot ->
                val fetchedCases = snapshot.documents.mapNotNull { doc ->
                    try {
                        RescueCase(
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
                    } catch (e: Exception) {
                        null
                    }
                }
                // Sort by createdAt in code to avoid needing a composite index
                cases = fetchedCases.sortedByDescending { it.createdAt?.toDate()?.time ?: 0 }
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = e.message ?: "Failed to load cases"
                isLoading = false
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rescue Cases") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Filter section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🦉 Browse Rescue Cases",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Status filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.take(3).forEach { status ->
                            FilterChip(
                                selected = filterStatus == status,
                                onClick = { filterStatus = status },
                                label = { Text(status, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.drop(3).forEach { status ->
                            FilterChip(
                                selected = filterStatus == status,
                                onClick = { filterStatus = status },
                                label = { Text(status, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
            
            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Error loading cases",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                cases.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "🌿",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No cases found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (filterStatus == "All") "No rescue cases yet" else "No $filterStatus cases",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cases) { case ->
                            CaseCard(
                                case = case,
                                onClick = { onOpenCase(case.id) }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CaseCard(
    case: RescueCase,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when (case.urgency) {
                "Emergency" -> MaterialTheme.colorScheme.errorContainer
                "High" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with species and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = case.species,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = when (case.status) {
                        "Open" -> MaterialTheme.colorScheme.primary
                        "In Progress" -> MaterialTheme.colorScheme.tertiary
                        "Resolved" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = case.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            // Urgency
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (case.urgency) {
                        "Emergency" -> "🚨"
                        "High" -> "⚠️"
                        "Moderate" -> "📍"
                        else -> "ℹ️"
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${case.urgency} Priority",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📍")
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = case.location,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
            
            // Description preview
            if (case.description.isNotEmpty()) {
                Text(
                    text = case.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            
            // Timestamp
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            case.createdAt?.let { timestamp ->
                val formatter = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                Text(
                    text = "Created: ${formatter.format(timestamp.toDate())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
