package com.example.queuenow.utils

import java.text.SimpleDateFormat
import java.util.*

fun Long.toFormattedDate(pattern: String = "dd/MM/yyyy HH:mm"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))

fun Long.toDateOnly(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(this))

fun getCurrentDateString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

fun String.toStatusColor() = when (this) {
    "PENDING_PAYMENT" -> com.example.queuenow.ui.theme.StatusCalled
    "WAITING"         -> com.example.queuenow.ui.theme.StatusWaiting
    "CALLED"          -> com.example.queuenow.ui.theme.StatusCalled
    "COMPLETED"       -> com.example.queuenow.ui.theme.StatusCompleted
    "CANCELED"        -> com.example.queuenow.ui.theme.StatusCanceled
    "SKIPPED"         -> com.example.queuenow.ui.theme.StatusSkipped
    else              -> com.example.queuenow.ui.theme.TextSecondary
}

fun String.toStatusLabel(): String = when (this) {
    "PENDING_PAYMENT" -> "Chờ xác nhận TT"
    "WAITING"         -> "Đang chờ"
    "CALLED"          -> "Đang được gọi"
    "COMPLETED"       -> "Đã hoàn thành"
    "CANCELED"        -> "Đã hủy"
    "SKIPPED"         -> "Bị bỏ qua"
    "OPEN"            -> "Đang mở"
    "CLOSED"          -> "Đã đóng"
    "LOCKED"          -> "Bị khóa"
    "PAUSED"          -> "Tạm dừng"
    "PENDING"         -> "Chờ duyệt"
    "APPROVED"        -> "Đã duyệt"
    "REJECTED"        -> "Từ chối"
    "SUBMITTED"       -> "Đã gửi"
    "CONFIRMED"       -> "Xác nhận"
    "REFUNDED"        -> "Hoàn tiền"
    "ACTIVE"          -> "Hoạt động"
    "INACTIVE"        -> "Không hoạt động"
    else              -> this
}