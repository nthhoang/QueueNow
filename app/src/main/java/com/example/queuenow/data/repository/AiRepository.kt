package com.example.queuenow.data.repository

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository {

    private val apiKey = "AIzaSyDpqCw98b5EgxvWPB7MW_9tW10SdOEVGXA"

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey    = apiKey.trim(),
        generationConfig = generationConfig {
            temperature     = 0.15f
            topK            = 40
            topP            = 0.95f
            maxOutputTokens = 2048
        },
        requestOptions    = RequestOptions(apiVersion = "v1beta"),
        systemInstruction = content {
            text(
                """
                Bạn là QueueBot – Trợ lý AI chính thức của ứng dụng QueueNow.
                Nhiệm vụ: Dựa trên dữ liệu hàng đợi thực tế được cung cấp để trả lời khách hàng chính xác, ngắn gọn.
                Nếu dữ liệu không đủ để trả lời, hãy hướng khách liên hệ trực tiếp với nhân viên.
                Phong cách: Chuyên nghiệp, lịch sự, thân thiện. Luôn tự xưng là QueueBot.
                Ngôn ngữ: Trả lời cùng ngôn ngữ mà khách hàng sử dụng.
                """.trimIndent()
            )
        }
    )

    suspend fun getAiResponse(prompt: String, contextData: String): String =
        withContext(Dispatchers.IO) {
            try {
                val fullPrompt = """
                    DỮ LIỆU HÀNG ĐỢI HIỆN TẠI:
                    $contextData

                    CÂU HỎI CỦA KHÁCH: $prompt
                """.trimIndent()

                val response = model.generateContent(fullPrompt)
                response.text?.trim() ?: "QueueBot chưa có phản hồi cho câu hỏi này."

            } catch (e: Exception) {
                // ── LOG ĐẦY ĐỦ để debug ──────────────────────────────────────
                val errorClass   = e.javaClass.simpleName
                val errorMsg     = e.localizedMessage ?: e.message ?: "null"
                val causeMsg     = e.cause?.localizedMessage ?: "null"
                Log.e("AiRepository", "=== GEMINI ERROR ===")
                Log.e("AiRepository", "Class  : $errorClass")
                Log.e("AiRepository", "Message: $errorMsg")
                Log.e("AiRepository", "Cause  : $causeMsg")
                Log.e("AiRepository", "Full   : ${e.stackTraceToString()}")
                // ─────────────────────────────────────────────────────────────

                when {
                    // Rate limit
                    errorMsg.contains("429") || errorMsg.contains("quota", ignoreCase = true) ->
                        "Hệ thống đang bận, vui lòng thử lại sau vài giây."

                    // Model không tìm thấy
                    errorMsg.contains("404") || errorMsg.contains("not found", ignoreCase = true) ->
                        "❌ [DEBUG] Model không tìm thấy. Class=$errorClass | $errorMsg"

                    // Auth / API key
                    errorMsg.contains("403")
                            || errorMsg.contains("401")
                            || errorMsg.contains("API_KEY", ignoreCase = true)
                            || errorMsg.contains("api key", ignoreCase = true)
                            || errorMsg.contains("PERMISSION", ignoreCase = true)
                            || errorMsg.contains("invalid", ignoreCase = true) ->
                        "❌ [DEBUG] Auth lỗi. Class=$errorClass | $errorMsg"

                    // Mạng
                    errorMsg.contains("Unable to resolve host", ignoreCase = true)
                            || errorMsg.contains("timeout", ignoreCase = true)
                            || errorMsg.contains("SocketException", ignoreCase = true) ->
                        "❌ [DEBUG] Lỗi mạng. Class=$errorClass | $errorMsg"

                    // Service unavailable
                    errorMsg.contains("503") || errorMsg.contains("unavailable", ignoreCase = true) ->
                        "Dịch vụ Gemini tạm thời không khả dụng."

                    // Catch-all – HIỆN LỖI THẬT để debug
                    else ->
                        "❌ [DEBUG] Class=$errorClass | $errorMsg | Cause=$causeMsg"
                }
            }
        }
}