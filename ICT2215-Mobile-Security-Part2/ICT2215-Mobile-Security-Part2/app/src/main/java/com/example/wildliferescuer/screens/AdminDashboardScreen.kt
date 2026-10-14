package com.example.wildliferescuer.screens

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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.example.wildliferescuer.models.RescueCase
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onOpenCase: (String) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // Role guard
    var role by remember { mutableStateOf<String?>(null) }
    var isLoadingRole by remember { mutableStateOf(true) }
    var currentUserName by remember { mutableStateOf("") }

    // Filters
    val urgencyOptions = listOf("All", "Low", "Moderate", "High", "Emergency")
    val statusOptions = listOf("All", "Open", "Assigned", "Resolved")

    var selectedUrgency by remember { mutableStateOf("All") }
    var selectedStatus by remember { mutableStateOf("All") }

    var urgencyExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    // Cases list
    var cases by remember { mutableStateOf<List<RescueCase>>(emptyList()) }
    var isLoadingCases by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 1) Load role
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            role = null
            isLoadingRole = false
            return@LaunchedEffect
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                role = doc.getString("role")
                currentUserName = doc.getString("name") ?: "Rescue Team"
                isLoadingRole = false
            }
            .addOnFailureListener {
                role = null
                isLoadingRole = false
            }
    }

    // 2) Listen to cases (live updates)
    DisposableEffect(Unit) {
        var reg: ListenerRegistration? = null

        reg = db.collection("cases")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = e.message ?: "Failed to load cases."
                    isLoadingCases = false
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents.orEmpty()
                val list = docs.map { d ->
                    RescueCase(
                        id = d.id,
                        species = d.getString("species") ?: "(Unknown)",
                        urgency = d.getString("urgency") ?: "Moderate",
                        status = d.getString("status") ?: "Open",
                        location = d.getString("location") ?: "-",
                        description = d.getString("description") ?: "",
                        createdBy = d.getString("createdBy") ?: "",
                        createdAt = d.getTimestamp("createdAt"),
                        lat = d.getDouble("lat"),
                        lng = d.getDouble("lng"),
                        assignedTo = d.getString("assignedTo"),
                        assignedToName = d.getString("assignedToName")
                    )
                }

                cases = list
                isLoadingCases = false
                errorMessage = null
            }

        onDispose {
            reg?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛡️ Admin Case Dashboard") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Role loading / guard
            if (isLoadingRole) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
                return@Column
            }

            if (role != "RESCUE_TEAM") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Access denied",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "This page is only available to Rescue Team members.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@Column
            }

            // Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Manage all rescue cases reported by the community.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Use filters to prioritise urgent cases and update statuses.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Filters row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Urgency filter
                ExposedDropdownMenuBox(
                    expanded = urgencyExpanded,
                    onExpandedChange = { urgencyExpanded = !urgencyExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUrgency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Urgency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = urgencyExpanded,
                        onDismissRequest = { urgencyExpanded = false }
                    ) {
                        urgencyOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedUrgency = opt
                                    urgencyExpanded = false
                                }
                            )
                        }
                    }
                }

                // Status filter
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedStatus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedStatus = opt
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            if (isLoadingCases) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
                return@Column
            }

            val filtered = cases.filter { c ->
                val urgencyOk = (selectedUrgency == "All" || c.urgency == selectedUrgency)
                val statusOk = (selectedStatus == "All" || c.status == selectedStatus)
                urgencyOk && statusOk
            }

            Text(
                text = "Cases (${filtered.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Text("No cases match your filters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { c ->
                    AdminCaseCard(
                        item = c,
                        onManage = {
                            val uid = auth.currentUser?.uid ?: return@AdminCaseCard
                            
                            // If case is unassigned, assign it first
                            if (c.assignedTo == null) {
                                val updates = hashMapOf<String, Any?>(
                                    "assignedTo" to uid,
                                    "assignedToName" to currentUserName,
                                    "status" to "In Progress"
                                )
                                
                                db.collection("cases").document(c.id)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        // Add system message
                                        val messageData = hashMapOf(
                                            "message" to "$currentUserName has been assigned to this case and will assist you shortly.",
                                            "senderId" to "system",
                                            "senderName" to "System",
                                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                        )
                                        db.collection("cases").document(c.id)
                                            .collection("messages")
                                            .add(messageData)
                                        
                                        // Redirect to chat
                                        onOpenCase(c.id)
                                    }
                            } else {
                                // Already assigned, just open chat
                                onOpenCase(c.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminCaseCard(
    item: RescueCase,
    onManage: () -> Unit
) {
    val urgencyBadge = when (item.urgency) {
        "Emergency" -> "🟥 Emergency"
        "High" -> "🟧 High"
        "Moderate" -> "🟨 Moderate"
        "Low" -> "🟩 Low"
        else -> "🟨 ${item.urgency}"
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "$urgencyBadge — ${item.species}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "📍 ${item.location}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Status: ${item.status} • ${timeAgo(item.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (item.assignedTo != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "👤 Assigned to: ${item.assignedToName ?: "Rescue Team"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onManage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (item.assignedTo == null) "Assign & Open" else "Open Chat")
            }
        }
    }
}

private fun timeAgo(ts: Timestamp?): String {
    if (ts == null) return "time unknown"
    val diffMs = System.currentTimeMillis() - ts.toDate().time
    if (diffMs < 0) return "just now"

    val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hrs = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)

    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        hrs < 24 -> "${hrs}h ago"
        else -> "${days}d ago"
    }
}
