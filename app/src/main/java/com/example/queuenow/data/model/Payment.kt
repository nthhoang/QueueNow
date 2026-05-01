package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

enum class PaymentStatus { PENDING, SUBMITTED, CONFIRMED, REJECTED, REFUNDED }

data class Payment(
    @DocumentId val paymentId: String = "",
    val ticketId: String = "",
    val placeId: String = "",          // ← THÊM MỚI
    val amount: Double = 0.0,
    val paymentMethod: String = "BANK_TRANSFER",
    val transactionCode: String = "",
    val submittedAt: Long = 0L,
    val confirmedAt: Long = 0L,
    val confirmedBy: String = "",
    val status: String = PaymentStatus.PENDING.name,
    val proofImageUrl: String = ""
)