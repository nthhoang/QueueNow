package com.example.queuenow.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.Message
import com.example.queuenow.data.model.QueueTicket
import com.example.queuenow.data.model.TicketStatus
import com.example.queuenow.data.model.WaitingRoom
import com.example.queuenow.data.repository.AiRepository
import com.example.queuenow.data.repository.PlaceRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class AiChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val placeName: String = ""
)

class AiChatViewModel(
    private val placeId: String,
    private val aiRepo: AiRepository = AiRepository(),
    private val placeRepo: PlaceRepository = PlaceRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AiChatUiState())
    val state = _state.asStateFlow()

    init {
        loadPlaceInfo()
        addMessage(Message(
            senderId = "ai",
            senderName = "QueueBot",
            content = "Chào bạn! Tôi là trợ lý AI của đia điểm này. Bạn có thể hỏi tôi về số người đang đợi, thời gian chờ dự kiến hoặc các yêu cầu check-in/thanh toán của từng phòng."
        ))
    }

    private fun loadPlaceInfo() {
        viewModelScope.launch {
            val place = placeRepo.getPlace(placeId)
            _state.update { it.copy(placeName = place?.placeName ?: "Địa điểm") }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _state.value.isLoading) return

        val userMessage = Message(
            messageId = System.currentTimeMillis().toString(),
            senderId = "user",
            senderName = "Bạn",
            content = content,
            timestamp = System.currentTimeMillis()
        )
        addMessage(userMessage)

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // 1. Fetch live data from Firestore with detailed queue stats
                val contextData = fetchQueueContext()
                
                // 2. Get AI Response
                val aiResponseContent = aiRepo.getAiResponse(content, contextData)
                
                val aiMessage = Message(
                    messageId = (System.currentTimeMillis() + 1).toString(),
                    senderId = "ai",
                    senderName = "QueueBot",
                    content = aiResponseContent,
                    timestamp = System.currentTimeMillis()
                )
                addMessage(aiMessage)
            } catch (e: Exception) {
                addMessage(Message(
                    senderId = "ai",
                    senderName = "QueueBot",
                    content = "Rất tiếc, đã có lỗi xảy ra khi xử lý yêu cầu của bạn."
                ))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun fetchQueueContext(): String {
        val db = FirebaseFirestore.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return try {
            // 1. Lấy danh sách phòng chờ tại địa điểm
            val roomsSnapshot = db.collection("places").document(placeId)
                .collection("rooms").get().await()
            val rooms = roomsSnapshot.toObjects(WaitingRoom::class.java)
            
            if (rooms.isEmpty()) return "Địa điểm ${_state.value.placeName} hiện chưa có phòng chờ nào được cấu hình."

            // 2. Lấy toàn bộ vé của địa điểm trong ngày hôm nay để tính toán chính xác
            val ticketsSnapshot = db.collection("tickets")
                .whereEqualTo("placeId", placeId)
                .whereEqualTo("queueDate", today)
                .get().await()
            val allTickets = ticketsSnapshot.toObjects(QueueTicket::class.java)

            val info = StringBuilder("DỮ LIỆU HÀNG ĐỢI THỰC TẾ TẠI ${_state.value.placeName} (Ngày: $today):\n\n")
            
            rooms.forEach { room ->
                // Chỉ tính các vé thuộc phòng này và được tạo sau lần reset cuối cùng
                val roomTickets = allTickets.filter { 
                    it.roomId == room.roomId && it.issueTime >= room.lastResetTime 
                }

                val calledTicket = roomTickets.find { it.status == TicketStatus.CALLED.name }
                val waitingCount = roomTickets.count { it.status == TicketStatus.WAITING.name }
                val waitingList = roomTickets.filter { it.status == TicketStatus.WAITING.name }
                    .sortedBy { it.ticketNumber }
                
                val nextNumber = if (waitingList.isNotEmpty()) waitingList.first().ticketNumber else "Không có"
                val avgServiceTime = room.estimatedServiceTime
                val totalWaitTime = waitingCount * avgServiceTime

                info.append("● Phòng: ${room.roomName}\n")
                info.append("  - Trạng thái phòng: ${room.status}\n")
                info.append("  - Số đang được gọi (phục vụ): ${calledTicket?.ticketNumber ?: "Chưa có số nào"}\n")
                info.append("  - Số người đang đợi trong hàng: $waitingCount người\n")
                info.append("  - Số tiếp theo sẽ đến lượt: $nextNumber\n")
                info.append("  - Thời gian phục vụ trung bình: $avgServiceTime phút/người (do chủ cấu hình)\n")
                info.append("  - Tổng thời gian chờ ước tính cho người mới: $totalWaitTime phút\n")
                
                val rules = mutableListOf<String>()
                if (room.prepaymentRequired) rules.add("Yêu cầu trả trước ${room.prepaymentAmount} VNĐ")
                if (room.requireQrScan) rules.add("Bắt buộc quét mã QR tại quầy để lấy số")
                else rules.add("Có thể lấy số trực tiếp trên app")
                
                info.append("  - Quy định & Lưu ý: ${rules.joinToString(", ")}\n")
                if (room.locationNote.isNotBlank()) info.append("  - Vị trí/Ghi chú: ${room.locationNote}\n")
                if (room.description.isNotBlank()) info.append("  - Mô tả: ${room.description}\n")
                info.append("\n")
            }
            
            info.append("GHI CHÚ CHO AI: Dựa vào 'Số người đang đợi' và 'Thời gian phục vụ trung bình' để trả lời về thời gian chờ. ")
            info.append("Nếu khách hỏi 'khi nào đến lượt tôi', hãy nhắc họ xem số vé của mình và so sánh với 'Số đang được gọi'. ")
            info.append("Luôn cập nhật thông tin chính xác theo dữ liệu thực tế này.")
            
            info.toString()
        } catch (e: Exception) {
            "Hệ thống đang gặp sự cố khi lấy dữ liệu hàng đợi: ${e.localizedMessage}"
        }
    }

    private fun addMessage(msg: Message) {
        _state.update { it.copy(messages = it.messages + msg) }
    }
}
