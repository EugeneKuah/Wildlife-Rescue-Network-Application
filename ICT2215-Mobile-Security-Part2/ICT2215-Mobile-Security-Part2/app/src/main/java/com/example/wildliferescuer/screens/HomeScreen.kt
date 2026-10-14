package com.example.wildliferescuer.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    onNavigateToCreateCase: () -> Unit,
    onNavigateToTeamMembers: () -> Unit,
    onNavigateToCases: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    ) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var displayName by remember { mutableStateOf<String?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    var role by remember { mutableStateOf("USER") }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            isLoadingProfile = false
            displayName = null
            return@LaunchedEffect
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                displayName = doc.getString("name") ?: "Friend"
                role = doc.getString("role") ?: "USER"
                isLoadingProfile = false
            }
            .addOnFailureListener {
                displayName = "Friend"
                isLoadingProfile = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
            .statusBarsPadding()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "🌱 Wildlife Rescue Network",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A place to coordinate help for animals in need.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            if (isLoadingProfile) Text("Loading…")
                            else Text("Hello, $displayName 🐾")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        NatureActionCard(
            emoji = "📝",
            title = "Create a Rescue Case",
            subtitle = "Log a new report with location and details.",
            onClick = onNavigateToCreateCase
        )

        Spacer(modifier = Modifier.height(12.dp))

        NatureActionCard(
            emoji = "🦉",
            title = "View Rescue Cases",
            subtitle = "Browse ongoing cases and status updates.",
            onClick = onNavigateToCases
        )

        Spacer(modifier = Modifier.height(12.dp))

        NatureActionCard(
            emoji = "🧑‍🌾",
            title = "Rescue Team",
            subtitle = "See members, roles, and availability.",
            onClick = onNavigateToTeamMembers
        )

        if (role == "RESCUE_TEAM") {
            Spacer(modifier = Modifier.height(12.dp))

            NatureActionCard(
                emoji = "🛡️",
                title = "Admin Case Dashboard",
                subtitle = "Manage cases and coordinate rescue actions.",
                onClick = onNavigateToAdminDashboard
            )
        }


        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(10.dp))

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                auth.signOut()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Log out")
        }
    }
}

@Composable
private fun NatureActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
