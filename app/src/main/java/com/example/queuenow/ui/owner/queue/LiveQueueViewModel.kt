package com.example.queuenow.ui.owner.queue

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.*
import com.example.queuenow.data.repository.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LiveQueueState(
    val isLoading: Boolean = true,
    val room: WaitingRoom? = null,
    val calledTicket: QueueTicket? = null,
    val waitingTickets: List<QueueTicket> = emptyList(),
    val skippedTickets: List<QueueTicket> = emptyList(),
    val canceledTickets: List<QueueTicket> = emptyList(),
    val completedTickets: List<QueueTicket> = emptyList(),   // ← THÊM
    val canReset: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class LiveQueueViewModel(
    private val placeId: String,
    private val roomId: String
) : ViewModel() {

    companion object {
        private const val TAG = "LiveQueueVM"

        fun factory(placeId: String, roomId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LiveQueueViewModel(placeId, roomId) as T
            }
    }

    private val ticketRepo = QueueTicketRepository()
    private val roomRepo   = WaitingRoomRepository()
    private val notifRepo  = NotificationRepository()
    private val db         = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(LiveQueueState())
    val state          = _state.asStateFlow()

    init {
        observeRoom()
        observeTickets()
    }

    private fun observeRoom() {
        viewModelScope.launch {
            roomRepo.getRooms(placeId).collectLatest { rooms ->
                val room = rooms.find { it.roomId == roomId }
                _state.update { it.copy(room = room) }
            }
        }
    }

    private fun observeTickets() {
        viewModelScope.launch {
            ticketRepo.getTicketsByRoom(roomId).collectLatest { all ->
                val called    = all.firstOrNull { it.status == TicketStatus.CALLED.name }
                val waiting   = all.filter { it.status == TicketStatus.WAITING.name  }.sortedBy  { it.ticketNumber }
                val skipped   = all.filter { it.status == TicketStatus.SKIPPED.name  }.sortedByDescending { it.issueTime }
                val canceled  = all.filter { it.status == TicketStatus.CANCELED.name }.sortedByDescending { it.issueTime }
                val completed = all.filter { it.status == TicketStatus.COMPLETED.name }.sortedByDescending { it.issueTime }
                val hasActive = called != null || waiting.isNotEmpty() || skipped.isNotEmpty()

                _state.update { s ->
                    s.copy(
                        calledTicket     = called,
                        waitingTickets   = waiting,
                        skippedTickets   = skipped,
                        canceledTickets  = canceled,
                        completedTickets = completed,
                        canReset         = !hasActive,
                        isLoading        = false
                    )
                }
            }
        }
    }

    fun callNext() {
        viewModelScope.launch {
            if (_state.value.calledTicket != null) {
                _state.update { it.copy(message = "Hoàn thành hoặc bỏ qua số đang phục vụ trước!") }
                return@launch
            }
            val next = ticketRepo.getNextWaiting(roomId)
            if (next != null) {
                ticketRepo.updateStatus(next.ticketId, TicketStatus.CALLED)
                _state.update { it.copy(message = "Đã gọi số ${next.ticketNumber}") }
                pushNotif(
                    userId   = next.accountId,
                    type     = NotificationType.TICKET_CALLED.name,
                    title    = "🔔 Đến lượt bạn!",
                    message  = "Số ${next.ticketNumber} đang được gọi. Vui lòng đến quầy ngay.",
                    ticketId = next.ticketId
                )
            } else {
                _state.update { it.copy(message = "Không còn ai đang chờ") }
            }
        }
    }

    fun complete(ticketId: String) {
        viewModelScope.launch {
            ticketRepo.updateStatus(ticketId, TicketStatus.COMPLETED)
            _state.update { it.copy(message = "✓ Đã hoàn thành phục vụ") }
        }
    }

    fun skip(ticketId: String) {
        viewModelScope.launch {
            val ticket = _state.value.calledTicket
            ticketRepo.updateStatus(ticketId, TicketStatus.SKIPPED)
            _state.update { it.copy(message = "Đã bỏ qua số ${ticket?.ticketNumber}") }
            ticket?.let {
                pushNotif(
                    userId   = it.accountId,
                    type     = NotificationType.TICKET_SKIPPED.name,
                    title    = "Số của bạn bị bỏ qua",
                    message  = "Số ${it.ticketNumber} đã bị bỏ qua. Liên hệ nhân viên nếu cần.",
                    ticketId = ticketId
                )
            }
        }
    }

    fun recallSkipped(ticketId: String, ticketNumber: String) {
        viewModelScope.launch {
            ticketRepo.updateStatus(ticketId, TicketStatus.WAITING)
            _state.update { it.copy(message = "Đã đưa số $ticketNumber trở lại hàng đợi") }
        }
    }

    fun cancelSkipped(ticketId: String, ticketNumber: String, accountId: String) {
        viewModelScope.launch {
            ticketRepo.updateStatus(ticketId, TicketStatus.CANCELED)
            _state.update { it.copy(message = "Đã hủy số $ticketNumber") }
            pushNotif(
                userId   = accountId,
                type     = NotificationType.TICKET_CANCELED_BY_OWNER.name,
                title    = "Vé bị hủy",
                message  = "Số $ticketNumber của bạn đã bị nhân viên hủy.",
                ticketId = ticketId
            )
        }
    }

    fun resetQueue() {
        viewModelScope.launch {
            val hasActive = ticketRepo.hasActiveTickets(roomId)
            if (hasActive) {
                _state.update { it.copy(error = "Không thể reset khi còn vé đang hoạt động") }
                return@launch
            }
            try {
                val now = System.currentTimeMillis()
                db.collection("places").document(placeId)
                    .collection("rooms").document(roomId)
                    .update(mapOf("currentNumber" to 0, "lastResetTime" to now))
                    .await()
                Log.d(TAG, "Queue reset at $now — next will be 001")
                _state.update { it.copy(message = "✓ Đã reset — Số tiếp theo sẽ là 001") }
            } catch (e: Exception) {
                Log.e(TAG, "resetQueue: ${e.message}", e)
                _state.update { it.copy(error = "Lỗi reset: ${e.message}") }
            }
        }
    }

    fun pauseQueue()  { viewModelScope.launch { roomRepo.updateStatus(placeId, roomId, RoomStatus.PAUSED) } }
    fun resumeQueue() { viewModelScope.launch { roomRepo.updateStatus(placeId, roomId, RoomStatus.OPEN) } }
    fun closeQueue()  { viewModelScope.launch { roomRepo.updateStatus(placeId, roomId, RoomStatus.CLOSED) } }

    private suspend fun pushNotif(
        userId: String, type: String, title: String, message: String, ticketId: String = ""
    ) {
        notifRepo.sendNotification(
            AppNotification(
                userId = userId, type = type, title = title,
                message = message, placeId = placeId, ticketId = ticketId
            )
        )
    }

    fun clearMessages() = _state.update { it.copy(error = null, message = null) }
}