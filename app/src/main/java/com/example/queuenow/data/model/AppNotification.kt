package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

enum class NotificationType {
    TICKET_CALLED, TICKET_SKIPPED, TICKET_CANCELED_BY_OWNER,
    PAYMENT_CONFIRMED, PAYMENT_REJECTED,
    OWNER_REQUEST_APPROVED, OWNER_REQUEST_REJECTED,
    NEW_PAYMENT_PROOF, NEW_TICKET_IN_ROOM, TICKET_CANCELED_BY_USER,
    PLACE_LOCKED_BY_ADMIN, PLACE_UNLOCKED_BY_ADMIN, NEW_REVIEW,
    NEW_OWNER_REQUEST
}

data class AppNotification(
    @DocumentId val notificationId: String = "",
    val userId: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val placeId: String = "",
    val ticketId: String = "",
    
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)