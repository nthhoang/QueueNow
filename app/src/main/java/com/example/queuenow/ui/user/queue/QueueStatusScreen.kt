package com.example.queuenow.ui.user.queue

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.*
import com.example.queuenow.data.repository.*
import com.example.queuenow.data.service.CloudinaryService
import com.example.queuenow.ui.components.LoadingOverlay
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.QrCodeUtils
import com.example.queuenow.utils.VietQRGenerator
import com.example.queuenow.utils.toFormattedDate
import com.example.queuenow.utils.toStatusLabel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class QueueStatusState(
    val isLoading: Boolean = true,
    val ticket: QueueTicket? = null,
    val payment: Payment? = null,
    // Realtime queue data
    val currentCalledNumber: String = "---",
    val aheadCount: Int = 0,
    val totalWaiting: Int = 0,
    val estimatedWaitMinutes: Int = 0,
    // Notification dialog
    val showCalledDialog: Boolean = false,
    // Loading states
    val isCanceling: Boolean = false,
    val isUploadingProof: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class QueueStatusViewModel(private val ticketId: String) : ViewModel() {
    companion object {
        fun factory(ticketId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QueueStatusViewModel(ticketId) as T
            }
    }

    private val ticketRepo  = QueueTicketRepository()
    private val paymentRepo = PaymentRepository()

    private val _state = MutableStateFlow(QueueStatusState())
    val state          = _state.asStateFlow()

    private var prevStatus: String? = null
    private var roomObserveStarted = false

    init { observeTicket() }

    private fun observeTicket() {
        viewModelScope.launch {
            ticketRepo.getTicketLive(ticketId).collect { ticket ->
                if (ticket == null) return@collect

                // Detect transition → CALLED để show dialog
                val justCalled = prevStatus != null
                        && prevStatus != TicketStatus.CALLED.name
                        && ticket.status == TicketStatus.CALLED.name
                prevStatus = ticket.status

                // Load payment một lần hoặc khi status thay đổi
                if (_state.value.payment == null ||
                    ticket.status == TicketStatus.PENDING_PAYMENT.name) {
                    try {
                        val p = paymentRepo.getPaymentByTicket(ticketId)
                        _state.update { it.copy(payment = p) }
                    } catch (_: Exception) {}
                }

                // Bắt đầu observe room khi vé đang WAITING hoặc CALLED
                if (!roomObserveStarted && ticket.roomId.isNotEmpty()
                    && ticket.status in listOf(TicketStatus.WAITING.name, TicketStatus.CALLED.name)) {
                    roomObserveStarted = true
                    observeRoom(ticket.roomId, ticket.ticketNumber)
                }

                _state.update { s ->
                    s.copy(
                        ticket          = ticket,
                        isLoading       = false,
                        showCalledDialog = if (justCalled) true else s.showCalledDialog
                    )
                }
            }
        }
    }

    private fun observeRoom(roomId: String, myTicketNumber: String) {
        viewModelScope.launch {
            ticketRepo.getTicketsByRoom(roomId).collectLatest { roomTickets ->
                val called  = roomTickets.firstOrNull { it.status == TicketStatus.CALLED.name }
                val waiting = roomTickets.filter { it.status == TicketStatus.WAITING.name }
                val ahead   = waiting.count { it.ticketNumber < myTicketNumber }
                val estTime = ahead * (_state.value.ticket?.roomEstimatedTime ?: 10)

                _state.update { s ->
                    s.copy(
                        currentCalledNumber  = called?.ticketNumber ?: "---",
                        aheadCount           = ahead,
                        totalWaiting         = waiting.size,
                        estimatedWaitMinutes = estTime
                    )
                }
            }
        }
    }

    fun cancelTicket(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isCanceling = true) }
            try {
                ticketRepo.updateStatus(ticketId, TicketStatus.CANCELED)
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isCanceling = false) }
            }
        }
    }

    fun uploadPaymentProof(uri: Uri) {
        viewModelScope.launch {
            val paymentId = _state.value.payment?.paymentId ?: return@launch
            _state.update { it.copy(isUploadingProof = true, error = null) }
            try {
                val url = CloudinaryService.uploadImage(uri, "payments")
                paymentRepo.submitPayment(paymentId, url)
                // Reload payment
                val updated = paymentRepo.getPaymentByTicket(ticketId)
                _state.update { it.copy(payment = updated, message = "Đã gửi ảnh! Chờ Owner xác nhận.") }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Upload thất bại: ${e.message}") }
            } finally {
                _state.update { it.copy(isUploadingProof = false) }
            }
        }
    }

    fun dismissCalledDialog() = _state.update { it.copy(showCalledDialog = false) }
    fun clearMessages() = _state.update { it.copy(error = null, message = null) }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueStatusScreen(
    navController: NavController,
    ticketId: String,
    vm: QueueStatusViewModel = viewModel(factory = QueueStatusViewModel.factory(ticketId))
) {
    val state             by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog  by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.uploadPaymentProof(it) } }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }

    // ── Dialog khi được gọi ──────────────────────────────────────────────────
    if (state.showCalledDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissCalledDialog() },
            icon = {
                Icon(Icons.Filled.NotificationsActive, null,
                    tint = StatusCalled, modifier = Modifier.size(40.dp))
            },
            title = { Text("🔔 Đến lượt bạn rồi!", fontWeight = FontWeight.Bold) },
            text = {
                Text("Số ${state.ticket?.ticketNumber} đang được gọi!\nVui lòng đến quầy ngay.",
                    textAlign = TextAlign.Center)
            },
            confirmButton = {
                Button(
                    onClick = { vm.dismissCalledDialog() },
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusCalled)
                ) { Text("Đã hiểu") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Trạng thái vé") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                    }) { Icon(Icons.Filled.Home, null, tint = Primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        val ticket = state.ticket
        if (state.isLoading || ticket == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Hero Card ────────────────────────────────────────────
                QSHeroCard(ticket = ticket)

                // ── Chi tiết vé ──────────────────────────────────────────
                QSTicketInfoCard(ticket = ticket)

                // ── Trạng thái hàng đợi realtime (WAITING / CALLED) ──────
                if (ticket.status in listOf(
                        TicketStatus.WAITING.name,
                        TicketStatus.CALLED.name
                    )
                ) {
                    QSQueueInfoCard(state = state)
                }

                // ── QR & Payment section ─────────────────────────────────
                // LOGIC: chỉ hiện QR khi PENDING_PAYMENT + payment.status == PENDING
                if (ticket.status == TicketStatus.PENDING_PAYMENT.name) {
                    when (state.payment?.status) {
                        // Chưa gửi ảnh → hiện QR VietQR + nút upload
                        PaymentStatus.PENDING.name -> {
                            QSVietQRCard(
                                amount      = state.payment?.amount?.toLong() ?: 0L,
                                ticketId    = ticketId,
                                isUploading = state.isUploadingProof,
                                onUpload    = { imagePicker.launch("image/*") }
                            )
                        }
                        // Đã gửi ảnh, chờ xác nhận → ẨN QR
                        PaymentStatus.SUBMITTED.name -> {
                            QSPaymentSubmittedCard(payment = state.payment!!)
                        }
                        // Confirmed nhưng ticket chưa kịp đổi → hiển thị thông báo
                        PaymentStatus.CONFIRMED.name -> {
                            QSPaymentConfirmedCard()
                        }
                        else -> {
                            // Payment chưa load → loading
                            Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                // WAITING nhưng đã có payment confirmed (từ phòng prepayment) → hiện badge
                else if (ticket.status == TicketStatus.WAITING.name &&
                    state.payment?.status == PaymentStatus.CONFIRMED.name) {
                    QSPaymentConfirmedCard()
                }

                // ── Action buttons ────────────────────────────────────────
                when (ticket.status) {
                    TicketStatus.WAITING.name -> {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusCanceled),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, StatusCanceled)
                        ) {
                            Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Hủy vé", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TicketStatus.COMPLETED.name -> {
                        Button(
                            onClick = {
                                navController.navigate(
                                    Screen.ReviewScreen.createRoute(ticketId, ticket.placeId)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Accent),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Star, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Đánh giá dịch vụ", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }

                Spacer(Modifier.height(16.dp))
            }

            if (state.isCanceling || state.isUploadingProof) LoadingOverlay()
        }
    }

    // ── Cancel dialog ──────────────────────────────────────────────────────────
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon    = { Icon(Icons.Filled.Cancel, null, tint = StatusCanceled) },
            title   = { Text("Hủy vé?") },
            text    = { Text("Bạn có chắc muốn hủy số thứ tự ${state.ticket?.ticketNumber}?") },
            confirmButton = {
                Button(
                    onClick = { showCancelDialog = false; vm.cancelTicket { navController.popBackStack() } },
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusCanceled)
                ) { Text("Hủy vé") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Giữ lại") }
            }
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun QSHeroCard(ticket: QueueTicket) {
    val statusColor = when (ticket.status) {
        TicketStatus.CALLED.name          -> StatusCalled
        TicketStatus.COMPLETED.name       -> StatusCompleted
        TicketStatus.CANCELED.name        -> StatusCanceled
        TicketStatus.SKIPPED.name         -> StatusSkipped
        TicketStatus.PENDING_PAYMENT.name -> StatusCalled.copy(alpha = 0.75f)
        else                              -> StatusWaiting
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(6.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(statusColor, statusColor.copy(0.7f))))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (ticket.placeName.isNotEmpty())
                Text(ticket.placeName, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.8f))
            if (ticket.roomName.isNotEmpty())
                Text(ticket.roomName, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.9f), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text("Số thứ tự", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f))
            Text(ticket.ticketNumber, fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold, color = Color.White)

            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(0.25f)) {
                Text(ticket.status.toStatusLabel(),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (ticket.status == TicketStatus.CALLED.name) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(0.2f)) {
                    Text("🔔 Đến lượt bạn! Vui lòng đến quầy ngay.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun QSTicketInfoCard(ticket: QueueTicket) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Chi tiết vé", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(12.dp))
            if (ticket.placeName.isNotEmpty())
                QSInfoRow("Địa điểm", ticket.placeName)
            if (ticket.roomName.isNotEmpty())
                QSInfoRow("Phòng chờ", ticket.roomName)
            QSInfoRow("Ngày", ticket.queueDate)
            QSInfoRow("Giờ lấy số", ticket.issueTime.toFormattedDate("HH:mm · dd/MM/yyyy"))
        }
    }
}

@Composable
private fun QSQueueInfoCard(state: QueueStatusState) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryLight),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Queue, null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Trạng thái hàng đợi • Realtime",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = Primary)
            }
            Spacer(Modifier.height(14.dp))

            QSQueueStatRow(Icons.Filled.PlayCircle, "Đang phục vụ số",
                state.currentCalledNumber,
                if (state.currentCalledNumber == "---") TextSecondary else StatusCalled)
            HorizontalDivider(color = Primary.copy(0.15f), modifier = Modifier.padding(vertical = 8.dp))

            QSQueueStatRow(Icons.Filled.PeopleAlt, "Người chờ trước bạn",
                if (state.aheadCount == 0) "Không có" else "${state.aheadCount} người",
                if (state.aheadCount == 0) StatusCompleted else StatusWaiting)
            HorizontalDivider(color = Primary.copy(0.15f), modifier = Modifier.padding(vertical = 8.dp))

            QSQueueStatRow(Icons.Filled.Timer, "Thời gian chờ ước tính",
                if (state.aheadCount == 0) "Rất nhanh" else "~${state.estimatedWaitMinutes} phút",
                if (state.aheadCount == 0) StatusCompleted else OnBackground)
            HorizontalDivider(color = Primary.copy(0.15f), modifier = Modifier.padding(vertical = 8.dp))

            QSQueueStatRow(Icons.Filled.Group, "Tổng số đang chờ",
                "${state.totalWaiting} người", TextSecondary)

            if (state.aheadCount == 0 && state.currentCalledNumber != "---") {
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = StatusCompleted.copy(0.15f)) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, null,
                            tint = StatusCompleted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sắp đến lượt! Hãy chuẩn bị sẵn sàng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusCompleted, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** VietQR card — chỉ hiện khi PENDING_PAYMENT + payment.status == PENDING */
@Composable
private fun QSVietQRCard(
    amount: Long,
    ticketId: String,
    isUploading: Boolean,
    onUpload: () -> Unit
) {
    val qrString = remember(amount, ticketId) {
        VietQRGenerator.generate(
            amount      = amount,
            description = "TT ${ticketId.take(8)}"
        )
    }
    val qrBitmap = remember(qrString) {
        runCatching { QrCodeUtils.generateQrCode(qrString, 512) }.getOrNull()
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Secondary.copy(0.5f))) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.QrCode2, null, tint = Secondary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Quét mã để chuyển khoản",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = Secondary)
            }
            Spacer(Modifier.height(6.dp))
            Text("Ngân hàng: Vietcombank (VCB)",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("STK: 1051459405 — QueueNow",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text(
                "${String.format("%,.0f", amount.toDouble())} VNĐ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold, color = Primary
            )
            Spacer(Modifier.height(16.dp))

            // QR bitmap
            if (qrBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Divider)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "VietQR",
                        modifier = Modifier.size(220.dp).padding(10.dp)
                    )
                }
            } else {
                Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(12.dp))
            Text("Sau khi chuyển khoản, upload ảnh xác nhận:",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))

            Button(
                onClick  = onUpload,
                enabled  = !isUploading,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Secondary),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isUploading) "Đang tải lên..." else "Upload ảnh chuyển khoản")
            }
        }
    }
}

@Composable
private fun QSPaymentSubmittedCard(payment: Payment) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StatusWaiting.copy(0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusWaiting.copy(0.3f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.HourglassTop, null,
                tint = StatusWaiting, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Đã gửi ảnh — Chờ Owner xác nhận",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = StatusWaiting)
                Text("Gửi lúc ${payment.submittedAt.toFormattedDate("HH:mm · dd/MM")}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("Vé sẽ tự động vào hàng đợi sau khi được xác nhận.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun QSPaymentConfirmedCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StatusCompleted.copy(0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusCompleted.copy(0.3f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, null,
                tint = StatusCompleted, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("Thanh toán đã được xác nhận ✅",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = StatusCompleted)
        }
    }
}

@Composable
private fun QSInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, color = OnBackground)
    }
}

@Composable
private fun QSQueueStatRow(icon: ImageVector, label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = valueColor)
    }
}