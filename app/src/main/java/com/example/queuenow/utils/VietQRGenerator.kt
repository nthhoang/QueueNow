package com.example.queuenow.utils

/**
 * Sinh chuỗi VietQR chuẩn EMV QRCPS (NAPAS)
 * Tương thích mọi app ngân hàng hỗ trợ VietQR (VCB, MB, Techcombank...)
 *
 * Tài khoản: 1051459405 — Vietcombank (BIN 970436)
 */
object VietQRGenerator {

    private const val VCB_BIN       = "970436"
    private const val ACCOUNT_NO    = "1051459405"
    private const val MERCHANT_NAME = "QUEUENOW"
    private const val MERCHANT_CITY = "HO CHI MINH"

    /**
     * Sinh chuỗi QR hoàn chỉnh
     * @param amount Số tiền (0 = tự nhập)
     * @param description Nội dung (ASCII, max 25 chars)
     */
    fun generate(amount: Long = 0L, description: String = ""): String {

        val cleanDesc = description
            .uppercase()
            .replace(Regex("[^A-Z0-9 ]"), "")
            .trim()
            .take(25)

        // ── Tag 38: Merchant Account Information (NAPAS) ────────────────────────
        // Theo chuẩn Napas: Tag 38 chứa Tag 00 (GUID) và Tag 01 (Thông tin thụ hưởng lồng nhau)
        val beneficiary = buildString {
            append(tlv("00", VCB_BIN))    // BIN ngân hàng
            append(tlv("01", ACCOUNT_NO)) // Số tài khoản
        }

        val merchantAccInfo = buildString {
            append(tlv("00", "A000000727")) // GUID của NAPAS
            append(tlv("01", beneficiary))  // Thông tin thụ hưởng phải nằm trong Tag 01
            append(tlv("02", "QRIBFTTA"))   // Service Code: Chuyển nhanh qua TK
        }

        // ── Tag 62: Additional Data ─────────────────────────────────────────────
        val additionalData = if (cleanDesc.isNotBlank()) {
            tlv("62", tlv("08", cleanDesc)) // Tag 08: Nội dung giao dịch
        } else ""

        // ── Xây dựng QR payload ────────────────────────────────────────────────
        val payload = buildString {
            append(tlv("00", "01"))                          // Payload Format Indicator
            append(tlv("01", if (amount > 0) "12" else "11")) // 12=Dynamic (có tiền), 11=Static
            append(tlv("38", merchantAccInfo))               // Merchant Account Info
            append(tlv("52", "0000"))                        // Merchant Category Code
            append(tlv("53", "704"))                         // Currency: VND
            if (amount > 0) append(tlv("54", amount.toString()))
            append(tlv("58", "VN"))                          // Country Code
            append(tlv("59", MERCHANT_NAME))                 // Merchant Name
            append(tlv("60", MERCHANT_CITY))                 // Merchant City
            if (additionalData.isNotEmpty()) append(additionalData)
            append("6304")                                   // CRC Tag
        }

        return payload + String.format("%04X", crc16Ccitt(payload))
    }

    private fun tlv(tag: String, value: String): String {
        val length = value.length.toString().padStart(2, '0')
        return "$tag$length$value"
    }

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
