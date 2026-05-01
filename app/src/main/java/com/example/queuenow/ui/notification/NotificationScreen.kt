package com.example.queuenow.ui.notification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.AppNotification
import com.example.queuenow.data.model.NotificationType
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    vm: NotificationViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    // Khi màn hình load xong → mark all as read sau 300ms
    // (delay để UI render chấm đỏ trước rồi mới xóa)
    LaunchedEffect(Unit) {
        vm.markAllAsReadDelayed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Chỉ hiện tiêu đề, KHÔNG có badge hay số
                    Text("Thông báo", fontWeight = FontWeight.Bold)
                },
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
        when {
            state.isLoading -> {
                Box(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentAlignment    = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }
            }
            state.notifications.isEmpty() -> {
                Column(
                    modifier                = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment     = Alignment.CenterHorizontally,
                    verticalArrangement     = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.NotificationsNone, null,
                        tint     = Divider,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Không có thông báo nào", color = TextSecondary)
                }
            }
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.notifications,
                        key   = { it.notificationId }
                    ) { notif ->
                        // isNew = notif nằm trong danh sách chưa đọc lúc mở màn hình
                        val isNew = state.initialUnreadIds.contains(notif.notificationId)
                        NotificationCard(
                            notification = notif,
                            isNew        = isNew,
                            onClick      = {
                                // Nhấn vào item → xóa khỏi initialUnreadIds (bỏ chấm đỏ ngay)
                                if (isNew) vm.markAsRead(notif.notificationId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    isNew: Boolean,        // true = chưa đọc khi mở màn hình → hiện chấm đỏ
    onClick: () -> Unit
) {
    val (icon, iconColor) = notificationStyle(notification.type)

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            // Background nhạt nếu là thông báo mới
            containerColor = if (isNew) Primary.copy(alpha = 0.05f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(if (isNew) 2.dp else 1.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Surface(
                shape    = CircleShape,
                color    = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = notification.title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNew) FontWeight.Bold else FontWeight.SemiBold,
                        color      = OnBackground,
                        modifier   = Modifier.weight(1f)
                    )
                    // Chấm đỏ — chỉ hiện nếu là thông báo mới
                    if (isNew) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape    = CircleShape,
                            color    = Secondary,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = notification.createdAt.toFormattedDate("HH:mm · dd/MM/yyyy"),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun notificationStyle(
    type: String
): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (type) {
    NotificationType.TICKET_CALLED.name            -> Pair(Icons.Filled.PlayCircle,     StatusCalled)
    NotificationType.TICKET_SKIPPED.name           -> Pair(Icons.Filled.SkipNext,       StatusSkipped)
    NotificationType.TICKET_CANCELED_BY_OWNER.name -> Pair(Icons.Filled.Cancel,         StatusCanceled)
    NotificationType.PAYMENT_CONFIRMED.name        -> Pair(Icons.Filled.CheckCircle,    StatusCompleted)
    NotificationType.PAYMENT_REJECTED.name         -> Pair(Icons.Filled.MoneyOff,       StatusCanceled)
    NotificationType.OWNER_REQUEST_APPROVED.name   -> Pair(Icons.Filled.Verified,       StatusCompleted)
    NotificationType.OWNER_REQUEST_REJECTED.name   -> Pair(Icons.Filled.DoNotDisturb,   StatusCanceled)
    NotificationType.NEW_PAYMENT_PROOF.name        -> Pair(Icons.Filled.Receipt,        Secondary)
    NotificationType.NEW_TICKET_IN_ROOM.name       -> Pair(Icons.Filled.PersonAdd,      Primary)
    NotificationType.TICKET_CANCELED_BY_USER.name  -> Pair(Icons.Filled.PersonRemove,   StatusCanceled)
    NotificationType.PLACE_LOCKED_BY_ADMIN.name    -> Pair(Icons.Filled.Lock,           StatusCanceled)
    NotificationType.PLACE_UNLOCKED_BY_ADMIN.name  -> Pair(Icons.Filled.LockOpen,       StatusCompleted)
    NotificationType.NEW_REVIEW.name               -> Pair(Icons.Filled.Star,           androidx.compose.ui.graphics.Color(0xFFF59E0B))
    NotificationType.NEW_OWNER_REQUEST.name        -> Pair(Icons.Filled.PendingActions, Secondary)
    else -> Pair(Icons.Filled.Notifications, Primary)
}