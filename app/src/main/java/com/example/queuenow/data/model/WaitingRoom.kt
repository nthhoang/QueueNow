package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

enum class RoomStatus { OPEN, PAUSED, CLOSED, LOCKED }

data class WaitingRoom(
    @DocumentId val roomId: String = "",
    val placeId: String = "",
    val roomName: String = "",
    val roomType: String = "",
    val locationNote: String = "",
    val description: String = "",
    val estimatedServiceTime: Int = 10,
    val currentNumber: Int = 0,
    val prepaymentRequired: Boolean = false,
    val prepaymentAmount: Double = 0.0,
    val status: String = RoomStatus.CLOSED.name,
    val lastResetTime: Long = 0L
)