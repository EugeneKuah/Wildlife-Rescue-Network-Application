package com.example.wildliferescuer.models

import com.google.firebase.Timestamp

data class RescueCase(
    val id: String = "",
    val species: String = "",
    val urgency: String = "",
    val location: String = "",
    val description: String = "",
    val status: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val assignedTo: String? = null,
    val assignedToName: String? = null
)

