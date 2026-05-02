package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

data class ChatRoom(
    @DocumentId val chatRoomId: String = "",
    val placeId: String = "",
    val placeName: String = "",
    val placeImageUrl: String = "",
    val userId: String = "",
    val userName: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastSenderId: String = "",
    val unreadByUser: Int = 0,    // Số tin nhắn user chưa đọc
    val unreadByOwner: Int = 0,   // Số tin nhắn owner chưa đọc
    val createdAt: Long = System.currentTimeMillis()
)