package com.example.queuenow.ui.user.queue

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.*
import com.example.queuenow.data.repository.*
import com.example.queuenow.utils.QrScanTokenManager
import com.example.queuenow.utils.getCurrentDateString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ── UI Modes ─────────────────────────────────────────────────────────────────
sealed class TakeTicketUiMode {
    object Loading : TakeTicketUiMode()

    /** Phòng yêu cầu quét QR, chưa có token hợp lệ */
    data class NeedQrScan(
        val room: WaitingRoom,
        val place: Place?,
        val placeId: String
    ) : TakeTicketUiMode()

    data class CanTake(
        val room: WaitingRoom,
        val place: Place?,
        val waitingCount: Int,
        val myPosition: Int
    ) : TakeTicketUiMode()

    data class PendingPayment(
        val ticket: QueueTicket,
        val payment: Payment?,
        val room: WaitingRoom?,
        val place: Place?
    ) : TakeTicketUiMode()

    data class AlreadyInQueue(
        val ticket: QueueTicket,
        val currentCalledNumber: String,
        val aheadCount: Int,
        val totalWaiting: Int,
        val estimatedWaitMinutes: Int,
        val room: WaitingRoom?,
        val place: Place?
    ) : TakeTicketUiMode()

    data class Success(val ticketId: String) : TakeTicketUiMode()
    data class Error(val message: String)    : TakeTicketUiMode()
}

data class TakeTicketState(
    val mode: TakeTicketUiMode = TakeTicketUiMode.Loading,
    val isSubmitting: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class TakeTicketViewModel(
    private val placeId: String,
    private val roomId: String
) : ViewModel() {

    companion object {
        private const val TAG = "TakeTicketVM"

        fun factory(placeId: String, roomId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TakeTicketViewModel(placeId, roomId) as T
            }
    }

    private val placeRepo   = PlaceRepository()
    private val roomRepo    = WaitingRoomRepository()
    private val ticketRepo  = QueueTicketRepository()
    private val paymentRepo = PaymentRepository()
    private val authRepo    = AuthRepository()
    private val notifRepo   = NotificationRepository()

    private val _state = MutableStateFlow(TakeTicketState())
    val state = _state.asStateFlow()

    private var initJob: Job? = null
    private var queueObserveJob: Job? = null

    init { loadInitialState() }

    fun loadInitialState() {
        // Hủy job cũ trước khi chạy mới — tránh race condition
        initJob?.cancel()
        queueObserveJob?.cancel()

        initJob = viewModelScope.launch {
            _state.update { it.copy(mode = TakeTicketUiMode.Loading) }
            try {
                val uid   = authRepo.getCurrentUserId()
                val room  = roomRepo.getRoom(placeId, roomId)
                val place = placeRepo.getPlace(placeId)

                if (uid == null) {
                    _state.update { it.copy(mode = TakeTicketUiMode.Error("Bạn chưa đăng nhập")) }
                    return@launch
                }
                if (room == null) {
                    _state.update { it.copy(mode = TakeTicketUiMode.Error("Không tìm thấy phòng chờ")) }
                    return@launch
                }

                // ── CHECK 1: Phòng yêu cầu quét QR ──────────────────────────
                if (room.requireQrScan && !QrScanTokenManager.isValid(placeId)) {
                    _state.update {
                        it.copy(
                            mode = TakeTicketUiMode.NeedQrScan(
                                room    = room,
                                place   = place,
                                placeId = placeId
                            )
                        )
                    }
                    return@launch   // ← Dừng tại đây, không làm gì thêm
                }

                // ── CHECK 2: User đã có vé active chưa ───────────────────────
                val existing = ticketRepo.getUserActiveTicketInRoom(uid, roomId)
                if (existing != null) {
                    handleExistingTicket(existing, room, place)
                    return@launch
                }

                // ── CHECK 3: Đủ điều kiện lấy số ─────────────────────────────
                val waitingCount = ticketRepo.getWaitingCountInRoom(roomId, room.lastResetTime)
                _state.update {
                    it.copy(
                        mode = TakeTicketUiMode.CanTake(
                            room         = room,
                            place        = place,
                            waitingCount = waitingCount,
                            myPosition   = waitingCount + 1
                        )
                    )
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "loadInitialState: ${e.message}", e)
                _state.update {
                    it.copy(mode = TakeTicketUiMode.Error("Lỗi: ${e.message}"))
                }
            }
        }
    }

    private fun handleExistingTicket(
        ticket: QueueTicket,
        room: WaitingRoom?,
        place: Place?
    ) {
        when (ticket.status) {
            TicketStatus.PENDING_PAYMENT.name -> {
                viewModelScope.launch {
                    val payment = try { paymentRepo.getPaymentByTicket(ticket.ticketId) }
                    catch (_: Exception) { null }
                    _state.update {
                        it.copy(
                            mode = TakeTicketUiMode.PendingPayment(ticket, payment, room, place)
                        )
                    }
                }
            }
            else -> startRealtimeQueueObservation(ticket, room, place)
        }
    }

    private fun startRealtimeQueueObservation(
        ticket: QueueTicket,
        room: WaitingRoom?,
        place: Place?
    ) {
        queueObserveJob?.cancel()
        queueObserveJob = viewModelScope.launch {
            ticketRepo.getTicketsByRoom(roomId).collectLatest { roomTickets ->
                val called  = roomTickets.firstOrNull { it.status == TicketStatus.CALLED.name }
                val waiting = roomTickets.filter { it.status == TicketStatus.WAITING.name }
                val ahead   = waiting.count { it.ticketNumber < ticket.ticketNumber }
                val estWait = ahead * (room?.estimatedServiceTime ?: 10)

                _state.update {
                    it.copy(
                        mode = TakeTicketUiMode.AlreadyInQueue(
                            ticket               = ticket,
                            currentCalledNumber  = called?.ticketNumber ?: "---",
                            aheadCount           = ahead,
                            totalWaiting         = waiting.size,
                            estimatedWaitMinutes = estWait,
                            room                 = room,
                            place                = place
                        )
                    )
                }
            }
        }
    }

    fun takeTicket() {
        val currentMode = _state.value.mode
        if (currentMode !is TakeTicketUiMode.CanTake) return

        viewModelScope.launch {
            val uid     = authRepo.getCurrentUserId() ?: return@launch
            val account = authRepo.getAccount(uid) ?: return@launch
            val room    = currentMode.room

            _state.update { it.copy(isSubmitting = true) }
            try {
                val today        = getCurrentDateString()
                val number       = ticketRepo.getNextTicketNumber(roomId, today, room.lastResetTime)
                val ticketNumber = String.format("%03d", number)

                val initialStatus = if (room.prepaymentRequired)
                    TicketStatus.PENDING_PAYMENT.name
                else TicketStatus.WAITING.name

                val waitingAhead = currentMode.waitingCount

                val ticket = QueueTicket(
                    accountId         = uid,
                    accountName       = account.fullName,
                    placeId           = placeId,
                    roomId            = roomId,
                    placeName         = currentMode.place?.placeName ?: "",
                    roomName          = room.roomName,
                    roomEstimatedTime = room.estimatedServiceTime,
                    ticketNumber      = ticketNumber,
                    queueDate         = today,
                    issueTime         = System.currentTimeMillis(),
                    estimatedWaitTime = room.estimatedServiceTime * (waitingAhead + 1),
                    currentPosition   = if (room.prepaymentRequired) 0 else waitingAhead + 1,
                    qrCode            = "QUEUENOW_${placeId}_${roomId}_${ticketNumber}_$today",
                    status            = initialStatus
                )

                val ticketId = ticketRepo.createTicket(ticket)
                Log.d(TAG, "Ticket $ticketNumber created, status=$initialStatus")

                // Tăng currentNumber phòng
                try { roomRepo.incrementCurrentNumber(placeId, roomId) }
                catch (e: Exception) { Log.w(TAG, "incrementCurrentNumber: ${e.message}") }

                // Tạo Payment nếu cần prepayment
                if (room.prepaymentRequired) {
                    try {
                        paymentRepo.createPayment(
                            Payment(
                                ticketId = ticketId,
                                placeId  = placeId,
                                amount   = room.prepaymentAmount,
                                status   = PaymentStatus.PENDING.name
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "createPayment: ${e.message}")
                    }
                }

                // Thông báo owner
                try {
                    currentMode.place?.ownerId?.let { ownerId ->
                        if (ownerId.isNotBlank()) {
                            notifRepo.sendNotification(
                                AppNotification(
                                    userId   = ownerId,
                                    type     = NotificationType.NEW_TICKET_IN_ROOM.name,
                                    title    = "Khách hàng mới lấy số",
                                    message  = "${account.fullName} lấy số $ticketNumber tại ${room.roomName}.",
                                    placeId  = placeId,
                                    ticketId = ticketId
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "notify owner: ${e.message}")
                }

                // Revoke QR token sau khi lấy số thành công
                QrScanTokenManager.revokeToken(placeId)

                _state.update {
                    it.copy(mode = TakeTicketUiMode.Success(ticketId), isSubmitting = false)
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "takeTicket: ${e.message}", e)
                _state.update {
                    it.copy(
                        mode = TakeTicketUiMode.Error(
                            when {
                                e.message?.contains("PERMISSION_DENIED") == true ->
                                    "Lỗi quyền. Vui lòng đăng xuất và đăng nhập lại."
                                else -> "Lỗi: ${e.message}"
                            }
                        ),
                        isSubmitting = false
                    )
                }
            }
        }
    }

    fun clearError() { loadInitialState() }

    override fun onCleared() {
        super.onCleared()
        initJob?.cancel()
        queueObserveJob?.cancel()
    }
}