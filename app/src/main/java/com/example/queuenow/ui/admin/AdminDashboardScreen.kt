package com.example.queuenow.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.queuenow.ui.components.NotificationBell
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    vm: AdminViewModel = viewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản trị hệ thống", fontWeight = FontWeight.Bold) },
                actions = {
                    // ← NotificationBell
                    NotificationBell(navController)
                    IconButton(onClick = {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.AdminDashboard.route)
                        }
                    }) {
                        Icon(Icons.Filled.Person, null, tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Header ───────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AdminPanelSettings, null,
                                tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Admin Dashboard", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                                Text("QueueNow Management",
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                            }
                        }
                        Spacer(Modifier.height(18.dp))

                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AdminStatCard(Modifier.weight(1f), "${state.stats.totalUsers}", "Người dùng", Icons.Filled.People)
                                AdminStatCard(Modifier.weight(1f), "${state.stats.totalPlaces}", "Địa điểm", Icons.Filled.Store)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AdminStatCard(
                                    modifier = Modifier.weight(1f),
                                    value    = "${state.stats.pendingRequests}",
                                    label    = "Chờ duyệt",
                                    icon     = Icons.Filled.PendingActions,
                                    highlight = state.stats.pendingRequests > 0
                                )
                                AdminStatCard(
                                    modifier = Modifier.weight(1f),
                                    value    = "${state.stats.lockedAccounts}",
                                    label    = "Bị khóa",
                                    icon     = Icons.Filled.Lock,
                                    highlight = state.stats.lockedAccounts > 0
                                )
                            }
                        }
                    }
                }
            }

            // ── Menu ─────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quản lý", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                    AdminMenuCard(
                        icon     = Icons.Filled.People,
                        title    = "Quản lý tài khoản",
                        subtitle = "${state.stats.totalUsers} tài khoản · ${state.stats.lockedAccounts} bị khóa",
                        color    = Primary,
                        badge    = if (state.stats.lockedAccounts > 0) state.stats.lockedAccounts else null,
                        onClick  = { navController.navigate(Screen.ManageAccounts.route) }
                    )
                    Spacer(Modifier.height(10.dp))
                    AdminMenuCard(
                        icon     = Icons.Filled.Store,
                        title    = "Quản lý địa điểm",
                        subtitle = "${state.stats.totalPlaces} địa điểm đã đăng ký",
                        color    = Accent,
                        onClick  = { navController.navigate(Screen.ManagePlacesAdmin.route) }
                    )
                    Spacer(Modifier.height(10.dp))
                    AdminMenuCard(
                        icon     = Icons.Filled.PendingActions,
                        title    = "Duyệt yêu cầu chủ địa điểm",
                        subtitle = "${state.stats.pendingRequests} đơn chờ xét duyệt",
                        color    = Secondary,
                        badge    = if (state.stats.pendingRequests > 0) state.stats.pendingRequests else null,
                        onClick  = { navController.navigate(Screen.OwnerRequestList.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    highlight: Boolean = false
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp),
        color = if (highlight) Color.White.copy(0.3f) else Color.White.copy(0.2f)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f))
            }
        }
    }
}

@Composable
private fun AdminMenuCard(
    icon: ImageVector, title: String, subtitle: String,
    color: Color, badge: Int? = null, onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = color.copy(0.12f),
                modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (badge != null) {
                Surface(shape = RoundedCornerShape(20.dp), color = Secondary) {
                    Text("$badge", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
        }
    }
}