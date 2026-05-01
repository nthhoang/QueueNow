package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

enum class PlaceStatus { OPEN, CLOSED, LOCKED }

data class Place(
    @DocumentId val placeId: String = "",
    val ownerId: String = "",
    val placeName: String = "",
    val address: String = "",
    val phone: String = "",
    val description: String = "",
    val openTime: String = "",
    val closeTime: String = "",
    val imageUrl: String = "",
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val status: String = PlaceStatus.CLOSED.name,
    val lockedByAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)