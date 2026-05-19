package com.example.queuenow.utils

/**
 * Lưu scan token trong memory — tồn tại trong phiên app
 * Key: placeId, Value: timestamp khi scan
 * Token hợp lệ trong 5 phút
 */
object QrScanTokenManager {

    private const val VALID_DURATION_MS = 5 * 60 * 1_000L // 5 phút

    // placeId → timestamp quét thành công
    private val scanTokens = mutableMapOf<String, Long>()

    /** Lưu token sau khi quét thành công */
    fun grantToken(placeId: String) {
        scanTokens[placeId] = System.currentTimeMillis()
    }

    /** Kiểm tra token còn hợp lệ không */
    fun isValid(placeId: String): Boolean {
        val ts = scanTokens[placeId] ?: return false
        return (System.currentTimeMillis() - ts) < VALID_DURATION_MS
    }

    /** Thời gian còn lại (ms), 0 nếu hết hạn */
    fun remainingMs(placeId: String): Long {
        val ts = scanTokens[placeId] ?: return 0L
        val elapsed = System.currentTimeMillis() - ts
        return maxOf(0L, VALID_DURATION_MS - elapsed)
    }

    /** Xóa token (khi lấy số xong) */
    fun revokeToken(placeId: String) {
        scanTokens.remove(placeId)
    }

    fun clearAll() = scanTokens.clear()
}