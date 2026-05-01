package com.example.queuenow.ui.user.queue

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.WaitingRoom
import com.example.queuenow.ui.components.GradientButton
import com.example.queuenow.ui.components.LoadingOverlay
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeTicketScreen(
    navController: NavController,
    placeId: String,
    roomId: String,
    vm: TakeTicketViewModel = viewModel(
        factory = TakeTicketViewModel.factory(placeId, roomId)
    )
) {
    val state by vm.state.collectAsState()

    // Điều hướng khi tạo vé thành công
    LaunchedEffect(state.mode) {
        if (state.mode is TakeTicketUiMode.Success) {
            val ticketId = (state.mode as TakeTicketUiMode.Success).ticketId
            navController.navigate(Screen.QueueStatus.createRoute(ticketId)) {
                popUpTo(Screen.PlaceDetail.createRoute(placeId)) { inclusive = false }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lấy số thứ tự") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = OnBackground
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val mode = state.mode) {
                // ── Loading ──────────────────────────────────────────────
                is TakeTicketUiMode.Loading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Primary) }
                }

                // ── Có thể lấy số ───────────────────────────────────────
                is TakeTicketUiMode.CanTake -> {
                    CanTakeContent(
                        mode = mode,
                        isSubmitting = state.isSubmitting,
                        onTakeTicket = { vm.takeTicket() }
                    )
                }

                // ── Chờ xác nhận thanh toán ─────────────────────────────
                is TakeTicketUiMode.PendingPayment -> {
                    PendingPaymentContent(
                        mode = mode,
                        onViewStatus = {
                            navController.navigate(
                                Screen.QueueStatus.createRoute(mode.ticket.ticketId)
                            ) {
                                popUpTo(Screen.PlaceDetail.createRoute(placeId)) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }

                // ── Đã trong hàng đợi ───────────────────────────────────
                is TakeTicketUiMode.AlreadyInQueue -> {
                    AlreadyInQueueContent(
                        mode = mode,
                        onViewStatus = {
                            navController.navigate(
                                Screen.QueueStatus.createRoute(mode.ticket.ticketId)
                            ) {
                                popUpTo(Screen.PlaceDetail.createRoute(placeId)) {
                                    inclusive = false
                                }
                            }
                        }
                    )
                }

                // ── Lỗi ─────────────────────────────────────────────────
                is TakeTicketUiMode.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline, null,
                            tint = StatusCanceled,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            mode.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { vm.clearError() },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Thử lại") }
                    }
                }

                // Success handled by LaunchedEffect above
                is TakeTicketUiMode.Success -> {}
            }

            if (state.isSubmitting) LoadingOverlay()
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// CAN TAKE — Form lấy số
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun CanTakeContent(
    mode: TakeTicketUiMode.CanTake,
    isSubmitting: Boolean,
    onTakeTicket: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hero card: hiển thị số sẽ nhận ─────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                    )
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ConfirmationNumber, null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Bạn sẽ nhận số",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.85f)
                    )
                    Text(
                        String.format("%03d", mode.myPosition),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    // Badge hiển thị số người chờ
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 6.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.People, null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (mode.waitingCount == 0) "Chưa có ai chờ"
                                else "${mode.waitingCount} người đang chờ trước bạn",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Thông tin phòng ─────────────────────────────────────────────
        InfoCard(room = mode.room, place = mode.place)

        // ── Cảnh báo thanh toán ─────────────────────────────────────────
        if (mode.room.prepaymentRequired) {
            Spacer(Modifier.height(12.dp))
            PrepaymentWarningCard(amount = mode.room.prepaymentAmount)
        }

        Spacer(Modifier.height(24.dp))

        GradientButton(
            text = if (isSubmitting) "Đang xử lý..." else "Xác nhận lấy số",
            onClick = onTakeTicket,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))
        Text(
            "Vé sẽ được cấp ngay sau khi xác nhận",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ────────────────────────────────────────────────────────────────────────────
// PENDING PAYMENT — Chờ xác nhận thanh toán
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun PendingPaymentContent(
    mode: TakeTicketUiMode.PendingPayment,
    onViewStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hero card ───────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(StatusCalled, StatusCalled.copy(alpha = 0.7f))
                        )
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.HourglassTop, null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Số thứ tự của bạn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.85f)
                )
                Text(
                    mode.ticket.ticketNumber,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Text(
                        "⏳ Chờ xác nhận thanh toán",
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Thông báo ───────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = StatusCalled.copy(alpha = 0.08f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, StatusCalled.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info, null,
                        tint = StatusCalled,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Vé chưa vào hàng đợi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusCalled
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bạn cần upload ảnh chuyển khoản. Sau khi Owner xác nhận, " +
                            "vé sẽ được vào hàng đợi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground
                )

                mode.payment?.let { payment ->
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Divider)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Số tiền cần chuyển:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            "${String.format("%,.0f", payment.amount)} VNĐ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Secondary
                        )
                    }

                    val proofSent = payment.proofImageUrl.isNotEmpty()
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (proofSent) Icons.Filled.CheckCircle
                            else Icons.Filled.RadioButtonUnchecked,
                            null,
                            tint = if (proofSent) StatusCompleted else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (proofSent) "Đã gửi ảnh chuyển khoản"
                            else "Chưa gửi ảnh chuyển khoản",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (proofSent) StatusCompleted else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onViewStatus,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusCalled),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                Icons.Filled.Payment, null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Xem vé & Upload thanh toán",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ────────────────────────────────────────────────────────────────────────────
// ALREADY IN QUEUE — Đã trong hàng đợi
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun AlreadyInQueueContent(
    mode: TakeTicketUiMode.AlreadyInQueue,
    onViewStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hero card: số của mình ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                    )
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Số của bạn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.85f)
                )
                Text(
                    mode.ticket.ticketNumber,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Text(
                        "✅ Đang trong hàng đợi",
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Thông tin hàng đợi ──────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Trạng thái hàng đợi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // Số đang được phục vụ
                QueueInfoRow(
                    icon       = Icons.Filled.PlayCircle,
                    label      = "Đang phục vụ số",
                    value      = mode.currentCalledNumber,
                    valueColor = if (mode.currentCalledNumber == "---")
                        TextSecondary else StatusCalled,
                    highlight  = mode.currentCalledNumber != "---"
                )

                HorizontalDivider(
                    color = Divider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Số người đang chờ trước mình
                QueueInfoRow(
                    icon       = Icons.Filled.People,
                    label      = "Người chờ trước bạn",
                    value      = if (mode.aheadCount == 0) "Không có"
                    else "${mode.aheadCount} người",
                    valueColor = if (mode.aheadCount == 0) StatusCompleted
                    else StatusWaiting,
                    highlight  = mode.aheadCount == 0
                )

                HorizontalDivider(
                    color = Divider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Tổng người đang chờ
                QueueInfoRow(
                    icon       = Icons.Filled.Queue,
                    label      = "Tổng số đang chờ",
                    value      = "${mode.totalWaiting} người",
                    valueColor = TextSecondary,
                    highlight  = false
                )

                HorizontalDivider(
                    color = Divider,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Phòng
                QueueInfoRow(
                    icon       = Icons.Filled.MeetingRoom,
                    label      = "Phòng chờ",
                    value      = mode.room?.roomName ?: "—",
                    valueColor = OnBackground,
                    highlight  = false
                )

                // Địa điểm
                mode.place?.let {
                    HorizontalDivider(
                        color = Divider,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    QueueInfoRow(
                        icon       = Icons.Filled.Store,
                        label      = "Địa điểm",
                        value      = it.placeName,
                        valueColor = OnBackground,
                        highlight  = false
                    )
                }
            }
        }

        // Banner nếu sắp đến lượt
        if (mode.aheadCount == 0 && mode.currentCalledNumber != "---") {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = StatusCompleted.copy(alpha = 0.1f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, StatusCompleted.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.NotificationsActive, null,
                        tint = StatusCompleted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Gần đến lượt bạn! Hãy chuẩn bị sẵn sàng.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusCompleted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onViewStatus,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Visibility, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Xem vé của tôi",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Shared components
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun InfoCard(room: WaitingRoom, place: com.example.queuenow.data.model.Place?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Thông tin hàng đợi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(12.dp))
            place?.let {
                TakeInfoRow(Icons.Filled.Store, "Địa điểm", it.placeName)
            }
            TakeInfoRow(Icons.Filled.MeetingRoom, "Phòng chờ", room.roomName)
            if (room.roomType.isNotEmpty())
                TakeInfoRow(Icons.Filled.Category, "Loại dịch vụ", room.roomType)
            TakeInfoRow(
                Icons.Filled.Timer,
                "Thời gian ước tính",
                "~${room.estimatedServiceTime} phút/người"
            )
            if (room.locationNote.isNotEmpty())
                TakeInfoRow(Icons.Filled.Info, "Lưu ý vị trí", room.locationNote)
        }
    }
}

@Composable
private fun PrepaymentWarningCard(amount: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Secondary.copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Secondary.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Payment, null,
                    tint = Secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Yêu cầu thanh toán trước",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Số tiền: ${String.format("%,.0f", amount)} VNĐ",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Sau khi lấy số, bạn cần upload ảnh chuyển khoản. " +
                        "Vé sẽ vào hàng đợi sau khi Owner xác nhận.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun TakeInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = OnBackground
            )
        }
    }
}

@Composable
private fun QueueInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    highlight: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}