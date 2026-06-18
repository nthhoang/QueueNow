package com.example.queuenow.ui.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.*
import com.example.queuenow.data.repository.*
import com.example.queuenow.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class AdminActivity(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val timestamp: Long
)

data class AdminStats(
    val totalUsers: Int = 0,
    val totalPlaces: Int = 0,
    val pendingRequests: Int = 0,
    val lockedAccounts: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalTickets: Int = 0,
    val completedTickets: Int = 0,
    val canceledTickets: Int = 0,
    val dailyTicketStats: Map<String, Int> = emptyMap(),
    val revenueByMethod: Map<String, Double> = emptyMap(),
    val topPlaces: List<Pair<String, Int>> = emptyList(),
    val newUserCountLast7Days: Int = 0,
    val roleDistribution: Map<String, Int> = emptyMap(),
    val ticketStatusDistribution: Map<String, Int> = emptyMap()
)

data class AdminState(
    val isLoading: Boolean = true,
    val stats: AdminStats = AdminStats(),
    val accounts: List<Account> = emptyList(),
    val places: List<Place> = emptyList(),
    val pendingRequests: List<OwnerRequest> = emptyList(),
    val tickets: List<QueueTicket> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val activities: List<AdminActivity> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class AdminViewModel : ViewModel() {
    private val accountRepo = AccountRepository()
    private val placeRepo   = PlaceRepository()
    private val reqRepo     = OwnerRequestRepository()
    private val paymentRepo = PaymentRepository()
    private val ticketRepo  = QueueTicketRepository()
    private val reviewRepo  = ReviewRepository()
    private val notifRepo   = NotificationRepository()

    private val _state = MutableStateFlow(AdminState())
    val state = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            combine(
                accountRepo.getAllAccounts(),
                placeRepo.getAllPlaces(),
                reqRepo.getAllPendingRequests(),
                paymentRepo.getAllPayments(),
                ticketRepo.getAllTickets(),
                reviewRepo.getAllReviews()
            ) { args ->
                val accounts = args[0] as List<Account>
                val places   = args[1] as List<Place>
                val reqs     = args[2] as List<OwnerRequest>
                val payments = args[3] as List<Payment>
                val tickets  = args[4] as List<QueueTicket>
                val reviews  = args[5] as List<Review>

                val stateWithStats = calculateStats(accounts, places, reqs, payments, tickets, reviews)
                val activities = generateActivities(accounts, reqs, payments, reviews, tickets)
                stateWithStats.copy(activities = activities)
            }.collectLatest { newState ->
                _state.value = newState
            }
        }
    }

    private fun calculateStats(
        accounts: List<Account>,
        places: List<Place>,
        reqs: List<OwnerRequest>,
        payments: List<Payment>,
        tickets: List<QueueTicket>,
        reviews: List<Review>
    ): AdminState {
        val confirmedPayments = payments.filter { it.status == PaymentStatus.CONFIRMED.name }
        val totalRevenue = confirmedPayments.sumOf { it.amount }
        
        val dailyTickets = tickets.groupBy { it.queueDate }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.first }
            .take(7)
            .toMap()

        val revByMethod = confirmedPayments.groupBy { it.paymentMethod }
            .mapValues { it.value.sumOf { p -> p.amount } }

        val placeMap = places.associateBy { it.placeId }
        val topPlaces = tickets.groupBy { it.placeId }
            .map { (pid, tks) -> (placeMap[pid]?.placeName ?: "Unknown") to tks.size }
            .sortedByDescending { it.second }
            .take(5)

        val roles = accounts.groupBy { it.role }.mapValues { it.value.size }
        val ticketStatuses = tickets.groupBy { it.status }.mapValues { it.value.size }

        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val newUsers = accounts.count { it.createdAt >= sevenDaysAgo }

        return AdminState(
            isLoading = false,
            accounts = accounts,
            places = places,
            pendingRequests = reqs,
            tickets = tickets,
            payments = payments,
            reviews = reviews,
            stats = AdminStats(
                totalUsers = accounts.size,
                totalPlaces = places.size,
                pendingRequests = reqs.size,
                lockedAccounts = accounts.count { a -> a.status == AccountStatus.LOCKED.name },
                totalRevenue = totalRevenue,
                totalTickets = tickets.size,
                completedTickets = tickets.count { it.status == TicketStatus.COMPLETED.name },
                canceledTickets = tickets.count { it.status == TicketStatus.CANCELED.name },
                dailyTicketStats = dailyTickets,
                revenueByMethod = revByMethod,
                topPlaces = topPlaces,
                newUserCountLast7Days = newUsers,
                roleDistribution = roles,
                ticketStatusDistribution = ticketStatuses
            )
        )
    }

    private fun generateActivities(
        accounts: List<Account>,
        reqs: List<OwnerRequest>,
        payments: List<Payment>,
        reviews: List<Review>,
        tickets: List<QueueTicket>
    ): List<AdminActivity> {
        val list = mutableListOf<AdminActivity>()
        
        // 1. Người dùng mới
        accounts.take(5).forEach {
            list.add(AdminActivity("Người dùng mới", it.fullName, Icons.Default.PersonAdd, Primary, it.createdAt))
        }

        // 2. Yêu cầu làm đối tác
        reqs.take(5).forEach {
            list.add(AdminActivity("Yêu cầu đối tác", it.businessName, Icons.Default.Storefront, Secondary, it.submittedAt))
        }

        // 3. Giao dịch (Thành công & Chờ duyệt)
        payments.forEach {
            if (it.status == PaymentStatus.CONFIRMED.name) {
                list.add(AdminActivity("Giao dịch thành công", "+${it.amount.toInt()}đ - ${it.transactionCode}", Icons.Default.CheckCircle, StatusCompleted, it.confirmedAt))
            } else if (it.status == PaymentStatus.SUBMITTED.name) {
                list.add(AdminActivity("Thanh toán mới chờ duyệt", "${it.amount.toInt()}đ", Icons.Default.AccountBalanceWallet, Color(0xFFF59E0B), it.submittedAt))
            }
        }

        // 4. Vé đặt mới
        tickets.sortedByDescending { it.issueTime }.take(15).forEach {
            val title = if (it.status == TicketStatus.WAITING.name) "Đã đặt vé mới" else "Vé: ${it.status}"
            list.add(AdminActivity(title, "Số ${it.ticketNumber} tại ${it.placeName}", Icons.Default.ConfirmationNumber, Primary, it.issueTime))
        }
        
        // 5. Đánh giá mới
        reviews.take(5).forEach {
            list.add(AdminActivity("Đánh giá mới", "${it.rating} sao - ${it.accountName}", Icons.Default.RateReview, Color(0xFFF59E0B), it.createdAt))
        }
        
        return list.sortedByDescending { it.timestamp }.take(20)
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            try {
                reviewRepo.deleteReview(reviewId)
                _state.update { it.copy(message = "Đã xóa đánh giá vi phạm") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun sendGlobalNotification(title: String, message: String) {
        viewModelScope.launch {
            try {
                _state.value.accounts.forEach { acc ->
                    notifRepo.sendNotification(
                        AppNotification(
                            userId = acc.accountId,
                            type = NotificationType.SYSTEM.name,
                            title = "📢 $title",
                            message = message
                        )
                    )
                }
                _state.update { it.copy(message = "Đã gửi thông báo đến ${_state.value.accounts.size} người dùng") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, message = null) }
}
