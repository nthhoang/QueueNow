package com.example.queuenow.utils

/**
 * Sinh chuỗi VietQR chuẩn EMV QRCPS (NAPAS)
 * Tương thích mọi app ngân hàng hỗ trợ VietQR (VCB, MB, Techcombank...)
 *
 * Tài khoản: 1051459405 — Vietcombank (BIN 970436)
 */
object VietQRGenerator {

    // ── Thông tin tài khoản ───────────────────────────────────────────────────
    private const val VCB_BIN       = "970436"
    private const val ACCOUNT_NO    = "1051459405"
    private const val MERCHANT_NAME = "QUEUENOW"      // max 25 chars, ASCII only
    private const val MERCHANT_CITY = "HO CHI MINH"  // max 15 chars, ASCII only

    /**
     * Sinh chuỗi QR hoàn chỉnh (bao gồm CRC-16/CCITT)
     *
     * @param amount      Số tiền VNĐ (0 = người dùng tự nhập)
     * @param description Nội dung CK (chỉ ASCII, tối đa 25 ký tự)
     */
    fun generate(amount: Long = 0L, description: String = ""): String {

        // ── Làm sạch description — chỉ giữ A-Z 0-9 space ──────────────────────
        val cleanDesc = description
            .uppercase()
            .replace(Regex("[^A-Z0-9 ]"), "")
            .trim()
            .take(25)

        // ── Tag 38: Merchant Account Information (NAPAS) ────────────────────────
        val merchantAccInfo = buildString {
            append(tlv("00", "A000000727"))   // NAPAS GUID
            append(tlv("01", VCB_BIN))        // Bank BIN (VCB = 970436)
            append(tlv("02", ACCOUNT_NO))     // Số tài khoản
        }

        // ── Tag 62: Additional Data Field Template ──────────────────────────────
        val additionalData = if (cleanDesc.isNotBlank()) {
            tlv("62", tlv("08", cleanDesc))  // Tag 08 = Purpose of Transaction
        } else ""

        // ── Xây dựng QR payload (chưa có CRC) ──────────────────────────────────
        val payload = buildString {
            append(tlv("00", "01"))                        // Payload Format Indicator
            append(tlv("01", "12"))                        // Point of Initiation: 12 = dynamic
            append(tlv("38", merchantAccInfo))             // Merchant Account Info NAPAS
            append(tlv("52", "0000"))                      // MCC (unspecified)
            append(tlv("53", "704"))                       // Currency: VND = 704
            if (amount > 0L) append(tlv("54", amount.toString())) // Amount (optional)
            append(tlv("58", "VN"))                        // Country Code
            append(tlv("59", MERCHANT_NAME.take(25)))      // Merchant Name
            append(tlv("60", MERCHANT_CITY.take(15)))      // Merchant City
            if (additionalData.isNotEmpty()) append(additionalData)
            append("6304")                                 // CRC tag (value = 4 hex chars)
        }

        // ── Tính CRC-16/CCITT và nối vào cuối ──────────────────────────────────
        return payload + String.format("%04X", crc16Ccitt(payload))
    }

    /**
     * EMV TLV encoding:
     * Format = {TAG 2 digits}{LENGTH 2 decimal digits}{VALUE}
     */
    private fun tlv(tag: String, value: String): String {
        require(tag.length == 2) { "Tag phải có đúng 2 ký tự" }
        val length = value.length.toString().padStart(2, '0')
        return "$tag$length$value"
    }

    /**
     * CRC-16/CCITT
     * Polynomial : 0x1021
     * Initial    : 0xFFFF
     * No input/output reflection
     */
    private fun crc16Ccitt(data: String): Int {
        var crc = 0xFFFF
        for (ch in data) {
            crc = crc xor (ch.code shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) ((crc shl 1) xor 0x1021) and 0xFFFF
                else (crc shl 1) and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }
}