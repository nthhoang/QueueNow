package com.example.queuenow.ui.owner.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.PaymentStatus
import com.example.queuenow.data.model.QueueTicket
import com.example.queuenow.data.model.Review
import com.example.queuenow.data.model.TicketStatus
import com.example.queuenow.data.model.WaitingRoom
import com.example.queuenow.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RoomStats(
    val room: WaitingRoom,
    val completed: Int    = 0,
    val canceled: Int     = 0,
    val revenue: Double   = 0.0
)

data class OwnerStatsState(
    // Date range
    val fromDate: String = "",
    val toDate: String   = "",
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    // Result
    val totalCompleted: Int     = 0,
    val totalCanceled: Int      = 0,
    val totalRevenue: Double    = 0.0,
    val roomStats: List<RoomStats> = emptyList(),
    // Reviews (luôn hiển thị, không phụ thuộc date range)
    val reviews: List<Review>   = emptyList(),
    val avgRating: Double       = 0.0,
    val reviewCount: Int        = 0,
    val reviewsLoading: Boolean = true,
    val error: String?          = null
)

class OwnerStatsViewModel(private val placeId: String) : ViewModel() {

    companion object {
        fun factory(placeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OwnerStatsViewModel(placeId) as T
            }
    }

    private val ticketRepo  = QueueTicketRepository()
    private val paymentRepo = PaymentRepository()
    private val roomRepo    = WaitingRoomRepository()
    private val reviewRepo  = ReviewRepository()

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _state = MutableStateFlow(OwnerStatsState())
    val state          = _state.asStateFlow()

    init {
        // Mặc định: fromDate = đầu tháng, toDate = hôm nay
        val today = sdf.format(Date())
        val startOfMonth = sdf.format(
            Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time
        )
        _state.update { it.copy(fromDate = startOfMonth, toDate = today) }

        // Load đánh giá ngay (không phụ thuộc date range)
        loadReviews()
    }

    fun setFromDate(date: String) = _state.update { it.copy(fromDate = date) }
    fun setToDate(date: String)   = _state.update { it.copy(toDate = date) }

    /** Nhấn "Thống kê" — tính theo khoảng ngày đã chọn */
    fun search() {
        val from = _state.value.fromDate
        val to   = _state.value.toDate

        if (from.isBlank() || to.isBlank()) {
            _state.update { it.copy(error = "Vui lòng chọn khoảng thời gian") }
            return
        }
        if (from > to) {
            _state.update { it.copy(error = "Ngày bắt đầu phải trước ngày kết thúc") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val tickets = ticketRepo.getTicketsByPlaceAndDateRange(placeId, from, to)
                val rooms   = roomRepo.getRooms(placeId).first()

                val totalCompleted = tickets.count { it.status == TicketStatus.COMPLETED.name }
                val totalCanceled  = tickets.count { it.status == TicketStatus.CANCELED.name }
                val totalRevenue   = calcRevenue(tickets)

                // Stats theo từng phòng
                val roomStatsList = rooms.map { room ->
                    val rTickets = tickets.filter { it.roomId == room.roomId }
                    RoomStats(
                        room      = room,
                        completed = rTickets.count { it.status == TicketStatus.COMPLETED.name },
                        canceled  = rTickets.count { it.status == TicketStatus.CANCELED.name },
                        revenue   = calcRevenue(rTickets)
                    )
                }

                _state.update {
                    it.copy(
                        isLoading      = false,
                        hasSearched    = true,
                        totalCompleted = totalCompleted,
                        totalCanceled  = totalCanceled,
                        totalRevenue   = totalRevenue,
                        roomStats      = roomStatsList
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Lỗi: ${e.message}") }
            }
        }
    }

    private fun loadReviews() {
        viewModelScope.launch {
            reviewRepo.getReviewsByPlace(placeId).collectLatest { reviews ->
                val avg = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
                _state.update {
                    it.copy(
                        reviews        = reviews,
                        avgRating      = avg,
                        reviewCount    = reviews.size,
                        reviewsLoading = false
                    )
                }
            }
        }
    }

    private suspend fun calcRevenue(tickets: List<QueueTicket>): Double {
        if (tickets.isEmpty()) return 0.0
        var total = 0.0
        tickets.forEach { ticket ->
            try {
                val payment = paymentRepo.getPaymentByTicket(ticket.ticketId)
                if (payment?.status == PaymentStatus.CONFIRMED.name) {
                    total += payment.amount
                }
            } catch (_: Exception) {}
        }
        return total
    }

    fun clearError() = _state.update { it.copy(error = null) }
}