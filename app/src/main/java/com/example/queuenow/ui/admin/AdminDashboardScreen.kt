package com.example.queuenow.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.ui.components.NotificationBell
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    vm: AdminViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }
    var showNotifDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Hệ thống Quản trị", fontWeight = FontWeight.ExtraBold)
                        Text("QueueNow Enterprise", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                },
                actions = {
                    NotificationBell(navController)
                    IconButton(onClick = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.AdminDashboard.route)
                        }
                    }) {
                        Icon(Icons.Filled.AccountCircle, null, tint = Primary, modifier = Modifier.size(30.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Hero Section: Financials & Growth ──────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
                        .padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("TỔNG QUAN TÀI CHÍNH", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.7f))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            currencyFormatter.format(state.stats.totalRevenue),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        
                        Spacer(Modifier.height(24.dp))

                        if (state.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color.White)
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HighlightStatCard(
                                    Modifier.weight(1f), 
                                    "${state.stats.totalUsers}", 
                                    "Người dùng", 
                                    "+${state.stats.newUserCountLast7Days} mới", 
                                    Icons.Filled.Group
                                )
                                HighlightStatCard(
                                    Modifier.weight(1f), 
                                    "${state.stats.totalPlaces}", 
                                    "Đối tác", 
                                    "${state.stats.pendingRequests} chờ duyệt", 
                                    Icons.Filled.Storefront
                                )
                            }
                        }
                    }
                }
            }

            // ── Analytics Section: Charts ─────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Phân tích hoạt động", "7 ngày gần nhất")
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TicketMiniStat("Thành công", state.stats.completedTickets, StatusCompleted)
                                VerticalDivider(modifier = Modifier.height(30.dp).padding(horizontal = 8.dp))
                                TicketMiniStat("Đã hủy", state.stats.canceledTickets, StatusCanceled)
                                VerticalDivider(modifier = Modifier.height(30.dp).padding(horizontal = 8.dp))
                                TicketMiniStat("Tổng lượt", state.stats.totalTickets, Primary)
                            }
                            
                            Spacer(Modifier.height(20.dp))
                            
                            SimpleBarChart(
                                data = state.stats.dailyTicketStats,
                                modifier = Modifier.fillMaxWidth().height(180.dp)
                            )
                        }
                    }
                }
            }

            // ── Top Performing Places ──────────────────────────────────────────────────
            if (state.stats.topPlaces.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader("Địa điểm tiêu biểu", "Theo số lượng vé")
                        Spacer(Modifier.height(12.dp))
                        state.stats.topPlaces.forEachIndexed { index, pair ->
                            TopPlaceRow(index + 1, pair.first, pair.second)
                            if (index < state.stats.topPlaces.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Divider.copy(0.5f))
                            }
                        }
                    }
                }
            }

            // ── Management Controls ───────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Công cụ quản trị", "Chức năng hệ thống")
                    Spacer(Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.VerifiedUser,
                            title = "Duyệt đối tác",
                            count = state.stats.pendingRequests,
                            color = Secondary,
                            onClick = { navController.navigate(Screen.OwnerRequestList.route) }
                        )
                        QuickActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Send,
                            title = "Thông báo",
                            color = Primary,
                            onClick = { showNotifDialog = true }
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    AdminMenuCard(
                        icon     = Icons.Filled.ManageAccounts,
                        title    = "Quản lý Tài khoản",
                        subtitle = "Phân quyền, khóa/mở khóa ${state.stats.totalUsers} người dùng",
                        color    = Primary,
                        onClick  = { navController.navigate(Screen.ManageAccounts.route) }
                    )
                    Spacer(Modifier.height(12.dp))
                    AdminMenuCard(
                        icon     = Icons.Filled.Business,
                        title    = "Quản lý Cơ sở",
                        subtitle = "Kiểm soát ${state.stats.totalPlaces} địa điểm trên hệ thống",
                        color    = Accent,
                        onClick  = { navController.navigate(Screen.ManagePlacesAdmin.route) }
                    )
                    Spacer(Modifier.height(12.dp))
                    AdminMenuCard(
                        icon     = Icons.Filled.RateReview,
                        title    = "Quản lý Đánh giá",
                        subtitle = "Kiểm duyệt nội dung phản hồi từ khách hàng",
                        color    = Color(0xFFF59E0B),
                        onClick  = { navController.navigate(Screen.ManageReviewsAdmin.route) }
                    )
                }
            }

            // ── Recent Activity Feed ─────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Hoạt động gần đây", "Dòng sự kiện")
                    Spacer(Modifier.height(12.dp))
                    
                    state.activities.forEach { activity ->
                        ActivityRow(activity)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showNotifDialog) {
        GlobalNotificationDialog(
            onDismiss = { showNotifDialog = false },
            onSend = { title, msg ->
                vm.sendGlobalNotification(title, msg)
                showNotifDialog = false
            }
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun HighlightStatCard(
    modifier: Modifier,
    value: String,
    label: String,
    trend: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
            Spacer(Modifier.height(4.dp))
            Text(trend, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActivityRow(activity: AdminActivity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = activity.color.copy(0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(activity.icon, null, tint = activity.color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(activity.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(activity.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            activity.timestamp.toFormattedDate("HH:mm"),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    count: Int = 0,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
                if (count > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp),
                        shape = CircleShape,
                        color = Color.Red
                    ) {
                        Text(
                            "$count", 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TicketMiniStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun TopPlaceRow(rank: Int, name: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (rank <= 3) Primary else Color.LightGray.copy(0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$rank", color = if (rank <= 3) Color.White else TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count vé", fontWeight = FontWeight.Bold, color = Primary)
    }
}

@Composable
fun AdminMenuCard(
    icon: ImageVector, title: String, subtitle: String,
    color: Color, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(0.1f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
        }
    }
}

@Composable
fun SimpleBarChart(data: Map<String, Int>, modifier: Modifier = Modifier) {
    val sortedData = data.toList().sortedBy { it.first }
    val maxVal = (sortedData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(5)

    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (sortedData.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có dữ liệu thống kê", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            sortedData.forEach { (date, count) ->
                val heightFactor = count.toFloat() / maxVal
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$count", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Primary)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .fillMaxHeight(heightFactor)
                            .background(
                                brush = Brush.verticalGradient(listOf(Primary, PrimaryLight)),
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(date.substringAfterLast("-"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun GlobalNotificationDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thông báo hệ thống", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Nội dung này sẽ được gửi tới tất cả người dùng ứng dụng.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Tiêu đề") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = message, onValueChange = { message = it },
                    label = { Text("Nội dung thông báo") }, modifier = Modifier.fillMaxWidth(),
                    minLines = 3, shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && message.isNotBlank()) onSend(title, message) },
                enabled = title.isNotBlank() && message.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Gửi thông báo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
