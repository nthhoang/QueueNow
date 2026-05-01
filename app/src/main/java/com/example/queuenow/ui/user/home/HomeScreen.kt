package com.example.queuenow.ui.user.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.ui.components.NotificationBell
import com.example.queuenow.ui.components.PlaceCard
import com.example.queuenow.ui.components.UserBottomNav
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(
        bottomBar      = { UserBottomNav(navController) },
        containerColor = BackgroundLight
    ) { padding ->
        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(bottom = 16.dp)
        ) {
            // ── Header gradient ──────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        // Greeting + Bell
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Xin chào! 👋",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.85f)
                                )
                                Text(
                                    state.account?.fullName ?: "Người dùng",
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                            }
                            // ← NotificationBell ở đây
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NotificationBell(navController)
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape    = CircleShape,
                                    color    = Color.White.copy(0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Person, null,
                                            tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Search bar
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
                            shadowElevation = 4.dp) {
                            TextField(
                                value       = state.searchQuery,
                                onValueChange = { vm.search(it) },
                                placeholder = { Text("Tìm kiếm địa điểm...", color = TextSecondary) },
                                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Primary) },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { vm.search("") }) {
                                            Icon(Icons.Filled.Clear, null, tint = TextSecondary)
                                        }
                                    }
                                },
                                modifier   = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors     = TextFieldDefaults.colors(
                                    focusedContainerColor   = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            // ── Section title ────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        if (state.searchQuery.isBlank()) "Địa điểm nổi bật" else "Kết quả tìm kiếm",
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                    )
                    Text("${state.filteredPlaces.size} địa điểm",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            // ── Loading ──────────────────────────────────────────────
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            }
            // ── Empty ────────────────────────────────────────────────
            else if (state.filteredPlaces.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.SearchOff, null,
                            tint = Divider, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(14.dp))
                        Text(
                            if (state.searchQuery.isBlank()) "Chưa có địa điểm nào"
                            else "Không tìm thấy \"${state.searchQuery}\"",
                            color = TextSecondary
                        )
                    }
                }
            }
            // ── List ─────────────────────────────────────────────────
            else {
                items(state.filteredPlaces, key = { it.placeId }) { place ->
                    PlaceCard(
                        place    = place,
                        onClick  = { navController.navigate(Screen.PlaceDetail.createRoute(place.placeId)) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}