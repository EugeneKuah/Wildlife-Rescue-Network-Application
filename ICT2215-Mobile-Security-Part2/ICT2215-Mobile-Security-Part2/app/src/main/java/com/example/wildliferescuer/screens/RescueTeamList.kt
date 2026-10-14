package com.example.wildliferescuer.screens

import android.R.attr.onClick
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
import com.google.firebase.firestore.FirebaseFirestore
import com.example.wildliferescuer.models.RescueTeamMember
import androidx.compose.ui.tooling.preview.Preview
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescueTeamListScreen(
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var rescueTeamMembers by remember { mutableStateOf<List<RescueTeamMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch rescue team members from Firestore
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        db.collection("users")
            .whereEqualTo("role", "RESCUE_TEAM")
            .get()
            .addOnSuccessListener { snapshot ->
                rescueTeamMembers = snapshot.documents.map { doc ->
                    RescueTeamMember(
                        name = doc.getString("name") ?: "Unknown",
                        email = doc.getString("email") ?: "No email",
                        role = doc.getString("role") ?: "Unknown",
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.let {
                            SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(it)
                        } ?: "N/A", // Handle the timestamp as a Date and format it
                        specialisation = doc.getString("specialisation") ?: "Unknown"
                    )
                }
                isLoading = false
            }

            .addOnFailureListener { e ->
                errorMessage = e.message ?: "Failed to load rescue team members"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rescue Team") },
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
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Error loading team members",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Empty data
            if (rescueTeamMembers.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Rescue Team Members found")
                }
            }

            // Display the list of rescue team members
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rescueTeamMembers) { member ->
                    RescueTeamCard(
                        member = member
                    )
                }
            }
        }
    }
}

@Composable
fun RescueTeamCard(
    member: RescueTeamMember
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Member name and role
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = member.role,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Email
            Text(
                text = "Email: ${member.email}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Created at timestamp
            Text(
                text = "Joined: ${member.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Specialisation
            Text(
                text = "Specialisation: ${member.specialisation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun PreviewRescueTeamListScreen() {
    RescueTeamListScreen(
        onBack = {}
    )
}
