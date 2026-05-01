package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

enum class TicketStatus {
    PENDING_PAYMENT,
    WAITING,
    CALLED,
    SKIPPED,
    COMPLETED,
    CANCELED
}

data class QueueTicket(
    @DocumentId val ticketId: String = "",
    val accountId: String = "",
    val accountName: String = "",
    val placeId: String = "",
    val roomId: String = "",
    val placeName: String = "",          // Lưu tên địa điểm tại thời điểm tạo vé
    val roomName: String = "",           // Lưu tên phòng chờ tại thời điểm tạo vé
    val roomEstimatedTime: Int = 10,     // Thời gian phục vụ ước tính/người (phút)
    val ticketNumber: String = "",
    val queueDate: String = "",
    val issueTime: Long = System.currentTimeMillis(),
    val estimatedWaitTime: Int = 0,
    val currentPosition: Int = 0,
    val qrCode: String = "",
    val status: String = TicketStatus.WAITING.name
)