package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

enum class RequestStatus { PENDING, APPROVED, REJECTED, CANCELED }

data class OwnerRequest(
    @DocumentId val requestId: String = "",
    val accountId: String = "",
    val reviewedBy: String = "",
    val businessName: String = "",
    val businessType: String = "",
    val address: String = "",
    val description: String = "",
    val status: String = RequestStatus.PENDING.name,
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long = 0L,
    val rejectionReason: String = ""
)