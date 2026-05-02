package com.example.queuenow.ui.user.place

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.queuenow.data.model.Review
import com.example.queuenow.data.model.RoomStatus
import com.example.queuenow.data.model.WaitingRoom
import com.example.queuenow.ui.components.RatingBar
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    navController: NavController,
    placeId: String,
    vm: PlaceDetailViewModel = viewModel(factory = PlaceDetailViewModel.factory(placeId))
) {
    val state      by vm.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Phòng chờ", "Đánh giá")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.place?.placeName ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White, titleContentColor = OnBackground
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        val place = state.place ?: return@Scaffold

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Cover ────────────────────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    if (place.imageUrl.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Store, null,
                                tint = Color.White, modifier = Modifier.size(72.dp))
                        }
                    } else {
                        AsyncImage(
                            model = place.imageUrl, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.4f)))
                            )
                        )
                    }
                }
            }

            // ── Info card ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(place.placeName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = OnBackground,
                                modifier = Modifier.weight(1f))
                            StatusChip(place.status)
                        }
                        Spacer(Modifier.height(10.dp))

                        // Rating + count
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RatingBar(rating = place.ratingAverage, starSize = 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(String.format("%.1f", place.ratingAverage),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            // Ưu tiên ratingCount trong Place doc, fallback state.reviews.size
                            val count = if (place.ratingCount > 0) place.ratingCount
                            else state.reviews.size
                            Text(
                                if (count > 0) " ($count đánh giá)" else " (chưa có đánh giá)",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        PlaceDetailInfoRow(Icons.Filled.LocationOn, place.address)
                        if (place.phone.isNotEmpty()) {
                            Spacer(Modifier.height(5.dp))
                            PlaceDetailInfoRow(Icons.Filled.Phone, place.phone)
                        }
                        if (place.openTime.isNotEmpty()) {
                            Spacer(Modifier.height(5.dp))
                            PlaceDetailInfoRow(Icons.Filled.AccessTime,
                                "Mở cửa: ${place.openTime} – ${place.closeTime}")
                        }
                        if (place.description.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Divider)
                            Spacer(Modifier.height(10.dp))
                            Text(place.description, style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary)
                        }
                        // ── Nút Nhắn tin ─────────────────────────────────────────────────────────
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Divider)
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                navController.navigate(
                                    "chat_new?placeId=${placeId}&ownerId=${place.ownerId}"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Chat, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Nhắn tin với chủ địa điểm", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Tabs ─────────────────────────────────────────────────
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = Color.White,
                    contentColor     = Primary,
                    modifier         = Modifier.padding(horizontal = 16.dp),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color    = Primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = selectedTab == i,
                            onClick  = { selectedTab = i },
                            text = {
                                Text(title, fontWeight =
                                    if (selectedTab == i) FontWeight.Bold else FontWeight.Normal)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Phòng chờ ────────────────────────────────────────────
            if (selectedTab == 0) {
                if (state.rooms.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Chưa có phòng chờ nào", color = TextSecondary)
                        }
                    }
                } else {
                    items(state.rooms, key = { it.roomId }) { room ->
                        PlaceDetailRoomCard(
                            room         = room,
                            onTakeTicket = {
                                navController.navigate(
                                    Screen.TakeTicket.createRoute(placeId, room.roomId)
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── Đánh giá ─────────────────────────────────────────────
            else {
                if (state.reviews.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Chưa có đánh giá nào", color = TextSecondary)
                        }
                    }
                } else {
                    items(state.reviews, key = { it.reviewId }) { review ->
                        PlaceDetailReviewCard(
                            review   = review,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PlaceDetailInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnBackground)
    }
}

@Composable
private fun PlaceDetailRoomCard(
    room: WaitingRoom,
    onTakeTicket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOpen = room.status == RoomStatus.OPEN.name
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape    = RoundedCornerShape(12.dp),
                color    = if (isOpen) PrimaryLight else Divider,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MeetingRoom, null,
                        tint = if (isOpen) Primary else TextSecondary,
                        modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(room.roomName, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                if (room.roomType.isNotEmpty())
                    Text(room.roomType, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, null,
                        tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("~${room.estimatedServiceTime} phút/người",
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    if (room.prepaymentRequired) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = Secondary.copy(0.15f)) {
                            Text("Cần TT trước",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = Secondary)
                        }
                    }
                }
            }
            if (isOpen) {
                Button(onClick = onTakeTicket,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape  = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Lấy số", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                StatusChip(room.status)
            }
        }
    }
}

@Composable
private fun PlaceDetailReviewCard(review: Review, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = PrimaryLight, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            review.accountName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary, fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.accountName, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
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
                Spacer(Modifier.height(8.dp))
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