package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

data class Message(
    @DocumentId val messageId: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)