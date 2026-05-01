package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

data class Review(
    @DocumentId val reviewId: String = "",
    val accountId: String = "",
    val accountName: String = "",
    val avatarUrl: String = "",
    val placeId: String = "",
    val ticketId: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val reply: String = "",
    val replyAt: Long = 0L
)