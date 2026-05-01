package com.example.queuenow.ui.owner.queue

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.QueueTicket
import com.example.queuenow.data.model.RoomStatus
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveQueueScreen(
    navController: NavController,
    placeId: String,
    roomId: String,
    vm: LiveQueueViewModel = viewModel(factory = LiveQueueViewModel.factory(placeId, roomId))
) {
    val state             by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog   by remember { mutableStateOf(false) }

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
                title = {
                    Column {
                        Text(state.room?.roomName ?: "Hàng đợi", fontWeight = FontWeight.Bold)
                        Text(
                            buildString {
                                append("${state.waitingTickets.size} chờ")
                                if (state.skippedTickets.isNotEmpty())
                                    append(" · ${state.skippedTickets.size} bỏ qua")
                                if (state.completedTickets.isNotEmpty())
                                    append(" · ${state.completedTickets.size} xong")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    val room = state.room
                    if (room != null) {
                        when (room.status) {
                            RoomStatus.OPEN.name -> {
                                IconButton(onClick = { vm.pauseQueue() }) {
                                    Icon(Icons.Filled.Pause, "Tạm dừng", tint = StatusCalled)
                                }
                            }
                            RoomStatus.PAUSED.name -> {
                                IconButton(onClick = { vm.resumeQueue() }) {
                                    Icon(Icons.Filled.PlayArrow, "Tiếp tục", tint = StatusCompleted)
                                }
                            }
                            else -> {}
                        }
                        IconButton(onClick = { vm.closeQueue() }) {
                            Icon(Icons.Filled.Stop, "Đóng", tint = StatusCanceled)
                        }
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

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Trạng thái phòng ─────────────────────────────────────
            item {
                state.room?.let { room ->
                    val sColor = when (room.status) {
                        RoomStatus.OPEN.name   -> StatusCompleted
                        RoomStatus.PAUSED.name -> StatusCalled
                        else                   -> StatusCanceled
                    }
                    Surface(modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), color = sColor.copy(0.1f)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(room.status)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                when (room.status) {
                                    RoomStatus.OPEN.name   -> "Đang nhận khách"
                                    RoomStatus.PAUSED.name -> "Tạm dừng nhận khách"
                                    else                   -> "Đã đóng hàng đợi"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = sColor, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Đang phục vụ ─────────────────────────────────────────
            item {
                Text("Đang phục vụ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                if (state.calledTicket != null) {
                    CalledTicketCard(
                        ticket     = state.calledTicket!!,
                        onComplete = { vm.complete(state.calledTicket!!.ticketId) },
                        onSkip     = { vm.skip(state.calledTicket!!.ticketId) }
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp), color = PrimaryLight) {
                        Column(modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.HourglassEmpty, null,
                                tint = Primary, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có ai đang được phục vụ", color = Primary)
                        }
                    }
                }
            }

            // ── Nút Gọi số tiếp theo ─────────────────────────────────
            item {
                Button(
                    onClick  = { vm.callNext() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled  = state.calledTicket == null && state.waitingTickets.isNotEmpty(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.NavigateNext, null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gọi số tiếp theo",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            // ── Danh sách chờ ────────────────────────────────────────
            item {
                SectionHeader(
                    title = "Đang chờ",
                    count = state.waitingTickets.size,
                    color = StatusWaiting
                )
            }
            if (state.waitingTickets.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Không còn ai đang chờ", color = TextSecondary)
                    }
                }
            } else {
                items(state.waitingTickets, key = { it.ticketId }) { ticket ->
                    WaitingTicketRow(
                        ticket   = ticket,
                        position = state.waitingTickets.indexOf(ticket) + 1
                    )
                }
            }

            // ── Bị bỏ qua ────────────────────────────────────────────
            if (state.skippedTickets.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(
                        title = "Bị bỏ qua",
                        count = state.skippedTickets.size,
                        color = StatusSkipped
                    )
                }
                items(state.skippedTickets, key = { "sk_${it.ticketId}" }) { ticket ->
                    SkippedTicketCard(
                        ticket   = ticket,
                        onRecall = { vm.recallSkipped(ticket.ticketId, ticket.ticketNumber) },
                        onCancel = { vm.cancelSkipped(ticket.ticketId, ticket.ticketNumber, ticket.accountId) }
                    )
                }
            }

            // ── Đã hủy ───────────────────────────────────────────────
            if (state.canceledTickets.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(
                        title = "Đã hủy",
                        count = state.canceledTickets.size,
                        color = StatusCanceled
                    )
                }
                items(state.canceledTickets, key = { "ca_${it.ticketId}" }) { ticket ->
                    SimpleTicketRow(ticket = ticket, accentColor = StatusCanceled)
                }
            }

            // ── Đã hoàn thành ─────────────────────────────────────────
            if (state.completedTickets.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(
                        title = "Hoàn thành",
                        count = state.completedTickets.size,
                        color = StatusCompleted
                    )
                }
                items(state.completedTickets, key = { "co_${it.ticketId}" }) { ticket ->
                    SimpleTicketRow(ticket = ticket, accentColor = StatusCompleted)
                }
            }

            // ── Reset hàng đợi ────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick  = { if (state.canReset) showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    enabled  = state.canReset,
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (state.canReset) Primary else TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.canReset) Primary.copy(0.5f) else Divider
                    )
                ) {
                    Icon(Icons.Filled.RestartAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.canReset) "Reset hàng đợi (về STT 001)"
                        else "Cần hàng đợi trống để reset",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon  = { Icon(Icons.Filled.RestartAlt, null, tint = Primary) },
            title = { Text("Reset hàng đợi?") },
            text  = {
                Text(
                    "Số thứ tự sẽ bắt đầu lại từ 001.\n" +
                            "Lịch sử vé vẫn được giữ nguyên.\n\n" +
                            "⚠️ Thao tác này không thể hoàn tác!"
                )
            },
            confirmButton = {
                Button(
                    onClick = { showResetDialog = false; vm.resetQueue() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Reset ngay") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Surface(shape = RoundedCornerShape(20.dp), color = color.copy(0.15f)) {
            Text(
                "$count",
                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style      = MaterialTheme.typography.labelLarge,
                color      = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CalledTicketCard(
    ticket: QueueTicket,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Đang được gọi",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f))
            Text(ticket.ticketNumber, fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(ticket.accountName,
                style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Lấy số lúc ${ticket.issueTime.toFormattedDate("HH:mm")}",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Bỏ qua")
                }
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null,
                        tint = StatusCompleted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Xong", color = StatusCompleted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WaitingTicketRow(ticket: QueueTicket, position: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape    = RoundedCornerShape(10.dp),
                color    = if (position == 1) PrimaryLight else Divider,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(ticket.ticketNumber,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (position == 1) Primary else TextSecondary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ticket.accountName,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Lấy số lúc ${ticket.issueTime.toFormattedDate("HH:mm")}",
                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Text("#$position",
                style      = MaterialTheme.typography.titleMedium,
                color      = if (position == 1) Primary else TextSecondary,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SkippedTicketCard(
    ticket: QueueTicket,
    onRecall: () -> Unit,
    onCancel: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = StatusSkipped.copy(0.05f)),
        border    = androidx.compose.foundation.BorderStroke(1.dp, StatusSkipped.copy(0.3f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = StatusSkipped.copy(0.15f),
                    modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(ticket.ticketNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = StatusSkipped)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(ticket.accountName,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Bỏ qua lúc ${ticket.issueTime.toFormattedDate("HH:mm")}",
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                StatusChip(ticket.status)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusCanceled),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, StatusCanceled),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hủy luôn", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onRecall,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape    = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Replay, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Gọi lại", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon    = { Icon(Icons.Filled.Cancel, null, tint = StatusCanceled) },
            title   = { Text("Hủy số ${ticket.ticketNumber}?") },
            text    = { Text("Vé của ${ticket.accountName} sẽ bị hủy vĩnh viễn.") },
            confirmButton = {
                Button(
                    onClick = { showCancelDialog = false; onCancel() },
                    colors  = ButtonDefaults.buttonColors(containerColor = StatusCanceled)
                ) { Text("Hủy luôn") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Không") }
            }
        )
    }
}

/** Dùng chung cho Canceled và Completed — chỉ hiển thị, không có action */
@Composable
private fun SimpleTicketRow(ticket: QueueTicket, accentColor: androidx.compose.ui.graphics.Color) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = accentColor.copy(0.04f)),
        border    = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = accentColor.copy(0.12f),
                modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(ticket.ticketNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = accentColor)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ticket.accountName, style = MaterialTheme.typography.bodyMedium)
                Text(ticket.issueTime.toFormattedDate("HH:mm"),
                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            StatusChip(ticket.status)
        }
    }
}