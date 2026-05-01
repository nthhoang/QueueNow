package com.example.queuenow.ui.owner.stats

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.Review
import com.example.queuenow.ui.components.RatingBar
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerStatsScreen(
    navController: NavController,
    placeId: String,
    vm: OwnerStatsViewModel = viewModel(factory = OwnerStatsViewModel.factory(placeId))
) {
    val state   by vm.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearError() }
    }

    // ── Date picker helper ────────────────────────────────────────────────────
    fun showDatePicker(current: String, onPicked: (String) -> Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        if (current.isNotBlank()) {
            try { cal.time = sdf.parse(current) ?: Date() } catch (_: Exception) {}
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = String.format("%04d-%02d-%02d", year, month + 1, day)
                onPicked(picked)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Thống kê địa điểm", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Bộ lọc khoảng ngày ───────────────────────────────────────────
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(20.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DateRange, null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Chọn khoảng thời gian",
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Từ ngày
                            DatePickerField(
                                modifier = Modifier.weight(1f),
                                label    = "Từ ngày",
                                value    = state.fromDate,
                                onClick  = { showDatePicker(state.fromDate) { vm.setFromDate(it) } }
                            )
                            // Đến ngày
                            DatePickerField(
                                modifier = Modifier.weight(1f),
                                label    = "Đến ngày",
                                value    = state.toDate,
                                onClick  = { showDatePicker(state.toDate) { vm.setToDate(it) } }
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Quick select shortcuts
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "Hôm nay" to quickRange(0),
                                "7 ngày" to quickRange(6),
                                "Tháng này" to thisMonth()
                            ).forEach { (label, range) ->
                                FilterChip(
                                    selected = state.fromDate == range.first && state.toDate == range.second,
                                    onClick  = { vm.setFromDate(range.first); vm.setToDate(range.second) },
                                    label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryLight,
                                        selectedLabelColor     = Primary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick  = { vm.search() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled  = !state.isLoading,
                            colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Filled.BarChart, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (state.isLoading) "Đang tính toán..."
                                else "Xem thống kê",
                                fontWeight = FontWeight.Bold,
                                style      = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // ── Kết quả thống kê ─────────────────────────────────────────────
            if (state.hasSearched && !state.isLoading) {
                // Header kết quả
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                "Kết quả: ${state.fromDate} → ${state.toDate}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(0.85f)
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GradStatCard(Modifier.weight(1f), "${state.totalCompleted}", "Hoàn thành", Icons.Filled.CheckCircle)
                                GradStatCard(Modifier.weight(1f), "${state.totalCanceled}",  "Đã hủy",     Icons.Filled.Cancel)
                                GradStatCard(Modifier.weight(1f), fmtRevenue(state.totalRevenue), "Doanh thu", Icons.Filled.AttachMoney)
                            }
                        }
                    }
                }

                // Stats từng phòng
                if (state.roomStats.isNotEmpty()) {
                    item {
                        Text("Theo phòng chờ",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    items(state.roomStats, key = { it.room.roomId }) { rs ->
                        RoomStatCard(rs = rs)
                    }
                }
            } else if (!state.hasSearched && !state.isLoading) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        color    = PrimaryLight
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Chọn khoảng ngày và nhấn \"Xem thống kê\" để bắt đầu.",
                                style = MaterialTheme.typography.bodyMedium, color = Primary)
                        }
                    }
                }
            }

            // ── Đánh giá (luôn hiển thị) ─────────────────────────────────────
            item {
                HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null,
                        tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đánh giá từ khách hàng",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            if (state.reviewsLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp))
                    }
                }
            } else {
                // Tổng quan đánh giá
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (state.reviewCount == 0) {
                                Text("Chưa có đánh giá nào", color = TextSecondary,
                                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(String.format("%.1f", state.avgRating),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.ExtraBold, color = Color(0xFFF59E0B))
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        RatingBar(rating = state.avgRating, starSize = 22.dp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("${state.reviewCount} đánh giá",
                                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // Danh sách đánh giá
                if (state.reviews.isNotEmpty()) {
                    items(state.reviews, key = { it.reviewId }) { review ->
                        StatsReviewCard(review = review)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun quickRange(daysBefore: Int): Pair<String, String> {
    val sdf   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())
    val from  = sdf.format(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysBefore) }.time
    )
    return Pair(from, today)
}

private fun thisMonth(): Pair<String, String> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val start = sdf.format(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time)
    val end   = sdf.format(Date())
    return Pair(start, end)
}

private fun fmtRevenue(value: Double): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
    value >= 1_000     -> String.format("%.0fK", value / 1_000)
    else               -> "${value.toInt()}đ"
}

// ── Sub-composables ───────────────────────────────────────────────────────────
@Composable
private fun DatePickerField(
    modifier: Modifier,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier  = modifier.clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        color     = BackgroundLight,
        border    = androidx.compose.foundation.BorderStroke(1.dp, Divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CalendarToday, null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                    if (value.isBlank()) "Chọn ngày" else value,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (value.isBlank()) TextSecondary else OnBackground
                )
            }
        }
    }
}

@Composable
private fun GradStatCard(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color.White.copy(0.2f)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(5.dp))
            Text(value, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RoomStatCard(rs: RoomStats) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = PrimaryLight, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MeetingRoom, null, tint = Primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rs.room.roomName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomChip("✓ ${rs.completed}", StatusCompleted)
                    RoomChip("✗ ${rs.canceled}",  StatusCanceled)
                    if (rs.revenue > 0) RoomChip(fmtRevenue(rs.revenue), Accent)
                }
            }
        }
    }
}

@Composable
private fun RoomChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(0.1f)) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun StatsReviewCard(review: Review) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = PrimaryLight, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(review.accountName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.accountName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(review.createdAt.toFormattedDate("dd/MM/yyyy HH:mm"),
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                RatingBar(rating = review.rating.toDouble(), starSize = 14.dp)
            }
            if (review.comment.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = OnBackground)
            }
            if (review.reply.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = PrimaryLight) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Icon(Icons.Filled.Reply, null, tint = Primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(review.reply, style = MaterialTheme.typography.bodySmall, color = Primary)
                    }
                }
            }
        }
    }
}