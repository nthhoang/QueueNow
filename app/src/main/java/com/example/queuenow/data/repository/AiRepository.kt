package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AiRepository {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey.trim(),

        generationConfig = generationConfig {
            temperature = 0.15f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 512
        },

        requestOptions = RequestOptions(
            apiVersion = "v1beta"
        ),

        systemInstruction = content {
            text(
                """
                Bạn là QueueBot – Trợ lý AI chính thức của QueueNow.

                QUY TẮC:
                - Chỉ được trả lời dựa trên dữ liệu hệ thống được cung cấp.
                - Không suy đoán.
                - Không tự tạo dữ liệu.
                - Nếu dữ liệu không đủ hãy nói rõ và hướng người dùng liên hệ nhân viên.

                PHONG CÁCH:
                - Chuyên nghiệp
                - Lịch sự
                - Thân thiện
                - Trả lời ngắn gọn
                - Luôn tự xưng là QueueBot

                NGÔN NGỮ:
                - Trả lời cùng ngôn ngữ người dùng sử dụng.
                """.trimIndent()
            )
        }
    )

    suspend fun getAiResponse(
        prompt: String,
        contextData: String
    ): String = withContext(Dispatchers.IO) {

        val fullPrompt = """
            DỮ LIỆU HỆ THỐNG QUEUENOW (nguồn chính thức):

            $contextData

            Chỉ dùng dữ liệu trên để trả lời.

            CÂU HỎI KHÁCH:
            $prompt
        """.trimIndent()

        try {

            val response = model.generateContent(fullPrompt)

            response.text?.trim()
                ?: "QueueBot chưa có phản hồi cho câu hỏi này."

        }

        // ───── QUOTA / RATE LIMIT ─────
        catch (e: QuotaExceededException) {

            Log.e("AiRepository", "Quota exceeded", e)

            try {

                delay(30000)

                val retry = model.generateContent(fullPrompt)

                retry.text?.trim()
                    ?: "QueueBot chưa có phản hồi."

            } catch (ex: Exception) {

                Log.e("AiRepository", "Retry failed", ex)

                "QueueBot đang bận do quá nhiều yêu cầu. Vui lòng thử lại sau."
            }
        }

        // ───── CATCH ALL ─────
        catch (e: Exception) {

            val errorClass = e.javaClass.simpleName
            val errorMsg = e.localizedMessage ?: e.message ?: "null"
            val causeMsg = e.cause?.localizedMessage ?: "null"

            Log.e("AiRepository", "=== GEMINI ERROR ===")
            Log.e("AiRepository", "Class  : $errorClass")
            Log.e("AiRepository", "Message: $errorMsg")
            Log.e("AiRepository", "Cause  : $causeMsg")
            Log.e("AiRepository", e.stackTraceToString())

            when {

                // Model not found
                errorMsg.contains("404")
                        || errorMsg.contains("not found", true) ->
                    "QueueBot hiện chưa khả dụng."

                // Auth / API key
                errorMsg.contains("401")
                        || errorMsg.contains("403")
                        || errorMsg.contains("api key", true)
                        || errorMsg.contains("permission", true)
                        || errorMsg.contains("invalid", true) ->
                    "QueueBot đang gặp lỗi xác thực."

                // Network
                errorMsg.contains("Unable to resolve host", true)
                        || errorMsg.contains("timeout", true)
                        || errorMsg.contains("SocketException", true) ->
                    "Không thể kết nối mạng. Vui lòng kiểm tra Internet."

                // Service unavailable
                errorMsg.contains("503")
                        || errorMsg.contains("unavailable", true) ->
                    "Dịch vụ AI tạm thời không khả dụng."

                else ->
                    "QueueBot đang gặp sự cố tạm thời."
            }
        }
    }
}