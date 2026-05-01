package com.example.queuenow.ui.owner.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.model.PlaceStatus
import com.example.queuenow.ui.components.NotificationBell
import com.example.queuenow.ui.components.OwnerBottomNav
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    navController: NavController,
    vm: OwnerDashboardViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    // ← NotificationBell
                    NotificationBell(navController)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GradientStart)
            )
        },
        bottomBar      = { OwnerBottomNav(navController) },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Header gradient ──────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        Text("Xin chào! 👋",
                            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f))
                        Text(state.account?.fullName ?: "Chủ địa điểm",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(16.dp))

                        // Stats row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashStatCard(Modifier.weight(1f), "${state.places.size}", "Địa điểm", Icons.Filled.Store)
                            DashStatCard(
                                Modifier.weight(1f),
                                "${state.places.count { it.status == PlaceStatus.OPEN.name }}",
                                "Đang mở", Icons.Filled.CheckCircle
                            )
                            DashStatCard(
                                Modifier.weight(1f),
                                if (state.places.isEmpty()) "—"
                                else String.format("%.1f", state.places.map { it.ratingAverage }.average()),
                                "Đánh giá", Icons.Filled.Star
                            )
                        }
                    }
                }
            }

            // ── Quick Actions ────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thao tác nhanh", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickCard(Modifier.weight(1f), Icons.Filled.AddBusiness, "Thêm địa điểm", Primary) {
                            navController.navigate(Screen.EditPlace.createRoute("new"))
                        }
                        QuickCard(Modifier.weight(1f), Icons.Filled.Receipt, "Xác nhận TT", Secondary) {
                            state.places.firstOrNull()?.let {
                                navController.navigate(Screen.PaymentConfirm.createRoute(it.placeId))
                            }
                        }
                    }
                }
            }

            // ── Địa điểm ────────────────────────────────────────────
            item {
                Text("Địa điểm của tôi", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (state.places.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Store, null, tint = Divider, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Chưa có địa điểm nào", color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate(Screen.EditPlace.createRoute("new")) },
                            colors  = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape   = RoundedCornerShape(12.dp)
                        ) { Text("Thêm địa điểm mới") }
                    }
                }
            } else {
                items(state.places, key = { it.placeId }) { place ->
                    DashPlaceCard(
                        place       = place,
                        onRooms     = { navController.navigate(Screen.ManageRoom.createRoute(place.placeId)) },
                        onEdit      = { navController.navigate(Screen.EditPlace.createRoute(place.placeId)) },
                        onStats     = { navController.navigate(Screen.OwnerStats.createRoute(place.placeId)) },
                        modifier    = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashStatCard(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color.White.copy(0.2f)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
        }
    }
}

@Composable
private fun QuickCard(modifier: Modifier, icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Card(modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.1f))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(0.15f)) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DashPlaceCard(
    place: Place,
    onRooms: () -> Unit,
    onEdit: () -> Unit,
    onStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(place.placeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(place.address, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                }
                if (place.lockedByAdmin) {
                    Surface(shape = RoundedCornerShape(8.dp), color = StatusCanceled.copy(0.1f)) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, null, tint = StatusCanceled, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Admin khóa", style = MaterialTheme.typography.labelSmall, color = StatusCanceled)
                        }
                    }
                } else {
                    StatusChip(place.status)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRooms, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                    Icon(Icons.Filled.MeetingRoom, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Phòng chờ", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(onClick = onStats, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
                    Icon(Icons.Filled.BarChart, null, tint = Accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thống kê", style = MaterialTheme.typography.labelMedium, color = Accent)
                }
                Button(onClick = onEdit, enabled = !place.lockedByAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}