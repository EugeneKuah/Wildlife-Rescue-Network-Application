package com.example.wildliferescuer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import android.annotation.SuppressLint
import android.location.Geocoder
import java.util.Locale
import com.google.android.gms.location.Priority
fun getAddressFromCoordinates(
    context: Context,
    latitude: Double,
    longitude: Double,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

        if (addresses != null && addresses.isNotEmpty()) {
            val address = addresses[0]
            val fullAddress = address.getAddressLine(0) ?: "Unknown location"
            onResult(fullAddress)
        } else {
            onError("No street name found.")
        }
    } catch (e: Exception) {
        onError("Failed to get street name: ${e.message}")
    }
}

@Composable
fun LiveLocationSection(
    onAddressReady: (String) -> Unit,
    onCoordinatesReady: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    var hasPermission by remember { mutableStateOf(false) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }

    var addressText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (!hasPermission) error = "Location permission denied."
    }

    fun openGoogleMaps(lat: Double, lng: Double) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Rescue%20Case%20Location)")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        error = null
        isLoading = true

        if (!hasPermission) {
            isLoading = false
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    isLoading = false
                    error = "Couldn’t get location. Turn on GPS and try again."
                } else {
                    lat = loc.latitude
                    lng = loc.longitude
                    onCoordinatesReady(loc.latitude, loc.longitude)

                    // Reverse geocode to get street name
                    getAddressFromCoordinates(
                        context = context,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        onResult = { street ->
                            isLoading = false
                            addressText = street
                            onAddressReady(street)
                        },
                        onError = { err ->
                            isLoading = false
                            error = err
                            // fallback: still show coordinates if address fails
                            addressText = "Lat: ${loc.latitude}, Lng: ${loc.longitude}"
                            onAddressReady(addressText!!)
                        }
                    )
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                error = e.message ?: "Failed to get location."
            }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "📍 Live GPS Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = when {
                    isLoading -> "Fetching your location…"
                    addressText != null -> addressText!!
                    lat != null && lng != null -> "Lat: $lat\nLng: $lng"
                    else -> "No location captured yet."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { fetchLocation() }) {
                    Text("Use Current Location")
                }

                val canOpen = (lat != null && lng != null)
                Button(
                    onClick = { openGoogleMaps(lat!!, lng!!) },
                    enabled = canOpen
                ) {
                    Text("Open in Google Maps")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCaseScreen(
    onBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    // Form fields
    var species by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val urgencyOptions = listOf("Low", "Moderate", "High", "Emergency")
    var urgencyExpanded by remember { mutableStateOf(false) }
    var urgency by remember { mutableStateOf(urgencyOptions[1]) } // default Moderate

    // UI states
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }

    // Guard: must be logged in
    val uid = auth.currentUser?.uid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Rescue Case") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🌿 Help responders act faster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Fill in the details below. A timestamp and your name will be attached automatically.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uid == null) {
                Text(
                    text = "You must be logged in to create a case.",
                    color = MaterialTheme.colorScheme.error
                )
                return@Column
            }

            // Species
            OutlinedTextField(
                value = species,
                onValueChange = { species = it },
                label = { Text("Species / Animal") },
                placeholder = { Text("e.g., injured bird, stray dog, snake") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            )

            // Urgency dropdown
            ExposedDropdownMenuBox(
                expanded = urgencyExpanded,
                onExpandedChange = { if (!isSubmitting) urgencyExpanded = !urgencyExpanded }
            ) {
                OutlinedTextField(
                    value = urgency,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Urgency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    enabled = !isSubmitting
                )

                ExposedDropdownMenu(
                    expanded = urgencyExpanded,
                    onDismissRequest = { urgencyExpanded = false }
                ) {
                    urgencyOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                urgency = option
                                urgencyExpanded = false
                            }
                        )
                    }
                }
            }

            LiveLocationSection(
                onAddressReady = { streetOrText ->
                    location = streetOrText
                },
                onCoordinatesReady = { newLat, newLng ->
                    lat = newLat
                    lng = newLng
                }
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                placeholder = { Text("e.g., Near Block 123, Pasir Ris (playground entrance)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("What happened? Any visible injuries? Animal behavior?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                enabled = !isSubmitting,
                maxLines = 6
            )

            // Error / success messages
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Submit button
            Button(
                onClick = {
                    errorMessage = null
                    successMessage = null

                    val s = species.trim()
                    val loc = location.trim()
                    val desc = description.trim()

                    if (s.isEmpty()) {
                        errorMessage = "Please enter the animal/species."
                        return@Button
                    }
                    if (loc.isEmpty()) {
                        errorMessage = "Please enter a location."
                        return@Button
                    }
                    if (desc.isEmpty()) {
                        errorMessage = "Please add a short description."
                        return@Button
                    }

                    isSubmitting = true

                    // Firestore document fields
                    val caseData = hashMapOf(
                        "species" to s,
                        "urgency" to urgency,
                        "location" to loc,
                        "lat" to lat,
                        "lng" to lng,
                        "description" to desc,
                        "status" to "Open",
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("cases")
                        .add(caseData)
                        .addOnSuccessListener {
                            isSubmitting = false
                            successMessage = "Case submitted successfully"
                            onNavigateHome()
                            // Optional: clear form after success
                            species = ""
                            location = ""
                            description = ""
                            urgency = urgencyOptions[1]
                        }
                        .addOnFailureListener { e ->
                            isSubmitting = false
                            errorMessage = e.message ?: "Failed to submit case."
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Submitting…")
                    }
                } else {
                    Text("🌱 Submit Case")
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}
