package com.example.queuenow.ui.owner.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.queuenow.data.model.*
import com.example.queuenow.data.repository.*
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PaymentConfirmState(
    val isLoading: Boolean = true,
    val payments: List<Payment> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class PaymentConfirmViewModel(private val placeId: String) : ViewModel() {

    companion object {
        fun factory(placeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PaymentConfirmViewModel(placeId) as T
            }
    }

    private val paymentRepo = PaymentRepository()
    private val ticketRepo  = QueueTicketRepository()
    private val authRepo    = AuthRepository()
    private val notifRepo   = NotificationRepository()
    private val db          = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(PaymentConfirmState())
    val state          = _state.asStateFlow()

    init { observe() }

    private fun observe() {
        viewModelScope.launch {
            paymentRepo.getSubmittedPaymentsByPlace(placeId).collectLatest { payments ->
                _state.update { it.copy(payments = payments, isLoading = false) }
            }
        }
    }

    fun confirm(paymentId: String, ticketId: String) {
        viewModelScope.launch {
            val uid = authRepo.getCurrentUserId() ?: return@launch
            try {
                paymentRepo.updatePaymentStatus(paymentId, PaymentStatus.CONFIRMED, confirmedBy = uid)
                ticketRepo.updateStatus(ticketId, TicketStatus.WAITING)
                // Thông báo user
                val accountId = getTicketAccountId(ticketId)
                if (!accountId.isNullOrBlank()) {
                    notifRepo.sendNotification(
                        AppNotification(
                            userId   = accountId,
                            type     = NotificationType.PAYMENT_CONFIRMED.name,
                            title    = "Thanh toán được xác nhận ✅",
                            message  = "Thanh toán của bạn đã được xác nhận! Vé đã vào hàng đợi.",
                            placeId  = placeId,
                            ticketId = ticketId
                        )
                    )
                }
                _state.update { it.copy(message = "✅ Đã xác nhận — Vé vào hàng đợi") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Lỗi xác nhận") }
            }
        }
    }

    fun reject(paymentId: String, ticketId: String) {
        viewModelScope.launch {
            try {
                paymentRepo.updatePaymentStatus(paymentId, PaymentStatus.REJECTED)
                ticketRepo.updateStatus(ticketId, TicketStatus.CANCELED)
                val accountId = getTicketAccountId(ticketId)
                if (!accountId.isNullOrBlank()) {
                    notifRepo.sendNotification(
                        AppNotification(
                            userId   = accountId,
                            type     = NotificationType.PAYMENT_REJECTED.name,
                            title    = "Thanh toán bị từ chối",
                            message  = "Ảnh thanh toán của bạn bị từ chối. Vé đã bị hủy.",
                            placeId  = placeId,
                            ticketId = ticketId
                        )
                    )
                }
                _state.update { it.copy(message = "Đã từ chối thanh toán") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Lỗi từ chối") }
            }
        }
    }

    /** Lấy accountId của ticket bằng cách query Firestore trực tiếp */
    private suspend fun getTicketAccountId(ticketId: String): String? {
        return try {
            db.collection("tickets").document(ticketId)
                .get().await()
                .toObject(QueueTicket::class.java)
                ?.accountId
        } catch (e: Exception) {
            android.util.Log.e("PaymentConfirmVM", "getTicketAccountId: ${e.message}")
            null
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, message = null) }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmScreen(
    navController: NavController,
    placeId: String,
    vm: PaymentConfirmViewModel = viewModel(factory = PaymentConfirmViewModel.factory(placeId))
) {
    val state             by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        if (state.payments.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = StatusCompleted, modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(16.dp))
                Text("Không có thanh toán cần xác nhận", color = TextSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    color = StatusWaiting.copy(0.1f)) {
                    Text("${state.payments.size} thanh toán chờ xác nhận",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusWaiting, fontWeight = FontWeight.SemiBold)
                }
            }
            items(state.payments, key = { it.paymentId }) { payment ->
                PaymentCard(
                    payment   = payment,
                    onConfirm = { vm.confirm(payment.paymentId, payment.ticketId) },
                    onReject  = { vm.reject(payment.paymentId, payment.ticketId) }
                )
            }
        }
    }
}

@Composable
private fun PaymentCard(payment: Payment, onConfirm: () -> Unit, onReject: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    var showReject  by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top) {
                Column {
                    Text("Mã vé: ${payment.ticketId.take(8)}…",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Gửi lúc: ${payment.submittedAt.toFormattedDate()}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                StatusChip(payment.status)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${String.format("%,.0f", payment.amount)} VNĐ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Primary)
                Text("BANK_TRANSFER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            if (payment.proofImageUrl.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Ảnh chuyển khoản:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                AsyncImage(model = payment.proofImageUrl, contentDescription = "Ảnh CK",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop)
            }
            if (payment.status == PaymentStatus.SUBMITTED.name) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showReject = true }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCanceled),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusCanceled)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("Từ chối")
                    }
                    Button(onClick = { showConfirm = true }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                        shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("Xác nhận")
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = StatusCompleted) },
            title = { Text("Xác nhận thanh toán?") },
            text = { Text("Xác nhận ${String.format("%,.0f", payment.amount)} VNĐ?\nVé sẽ vào hàng đợi ngay.") },
            confirmButton = {
                Button(onClick = { showConfirm = false; onConfirm() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted)) { Text("Xác nhận") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Hủy") } }
        )
    }
    if (showReject) {
        AlertDialog(onDismissRequest = { showReject = false },
            icon = { Icon(Icons.Filled.Cancel, null, tint = StatusCanceled) },
            title = { Text("Từ chối thanh toán?") },
            text = { Text("Từ chối thanh toán này?\nVé của khách sẽ bị hủy.") },
            confirmButton = {
                Button(onClick = { showReject = false; onReject() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCanceled)) { Text("Từ chối") }
            },
            dismissButton = { TextButton(onClick = { showReject = false }) { Text("Hủy") } }
        )
    }
}