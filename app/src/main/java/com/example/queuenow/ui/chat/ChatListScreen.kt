package com.example.queuenow.ui.chat

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.queuenow.data.model.ChatRoom
import com.example.queuenow.ui.components.OwnerBottomNav
import com.example.queuenow.ui.components.UserBottomNav
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate

// ── User Chat List ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserChatListScreen(
    navController: NavController,
    vm: ChatListViewModel = viewModel(factory = ChatListViewModel.userFactory())
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhắn tin", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar      = { UserBottomNav(navController) },
        containerColor = BackgroundLight
    ) { padding ->
        ChatListContent(
            isLoading  = state.isLoading,
            chatRooms  = state.chatRooms,
            isOwner    = false,
            currentUid = state.currentUserId,
            padding    = padding,
            onClickRoom = { room ->
                navController.navigate(Screen.ChatScreen.createRoute(room.chatRoomId))
            }
        )
    }
}

// ── Owner Chat List ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerChatListScreen(
    navController: NavController,
    vm: ChatListViewModel = viewModel(factory = ChatListViewModel.ownerFactory())
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhắn tin với khách", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar      = { OwnerBottomNav(navController) },
        containerColor = BackgroundLight
    ) { padding ->
        ChatListContent(
            isLoading  = state.isLoading,
            chatRooms  = state.chatRooms,
            isOwner    = true,
            currentUid = state.currentUserId,
            padding    = padding,
            onClickRoom = { room ->
                navController.navigate(Screen.ChatScreen.createRoute(room.chatRoomId))
            }
        )
    }
}

// ── Shared Content ────────────────────────────────────────────────────────────
@Composable
private fun ChatListContent(
    isLoading: Boolean,
    chatRooms: List<ChatRoom>,
    isOwner: Boolean,
    currentUid: String,
    padding: PaddingValues,
    onClickRoom: (ChatRoom) -> Unit
) {
    when {
        isLoading -> {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Primary) }
        }
        chatRooms.isEmpty() -> {
            Column(
                modifier            = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.ChatBubbleOutline, null,
                    tint = Divider, modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isOwner) "Chưa có khách hàng nào nhắn tin"
                    else "Chưa có cuộc trò chuyện nào",
                    color = TextSecondary
                )
            }
        }
        else -> {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(vertical = 8.dp)
            ) {
                items(chatRooms, key = { it.chatRoomId }) { room ->
                    val unread = if (isOwner) room.unreadByOwner else room.unreadByUser
                    ChatRoomItem(
                        room    = room,
                        isOwner = isOwner,
                        unread  = unread,
                        onClick = { onClickRoom(room) }
                    )
                    HorizontalDivider(
                        color    = Divider,
                        modifier = Modifier.padding(start = 76.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRoomItem(
    room: ChatRoom,
    isOwner: Boolean,
    unread: Int,
    onClick: () -> Unit
) {
    // Tên hiển thị
    val displayName = if (isOwner) room.userName else room.placeName
    // Avatar chữ cái đầu
    val avatarChar  = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    // Preview tin nhắn cuối
    val preview = when {
        room.lastMessage.isBlank() -> "Bắt đầu cuộc trò chuyện..."
        else -> room.lastMessage
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box {
            if (!isOwner && room.placeImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = room.placeImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .then(Modifier.clip(CircleShape))
                )
            } else {
                Surface(
                    shape    = CircleShape,
                    color    = PrimaryLight,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            avatarChar,
                            style      = MaterialTheme.typography.titleLarge,
                            color      = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Badge unread
            if (unread > 0) {
                Surface(
                    shape    = CircleShape,
                    color    = Secondary,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text     = if (unread > 9) "9+" else "$unread",
                            fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                            color    = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                    displayName,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )
                if (room.lastMessageTime > 0) {
                    Text(
                        room.lastMessageTime.toFormattedDate("HH:mm"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unread > 0) Secondary else TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                preview,
                style     = MaterialTheme.typography.bodySmall,
                color     = if (unread > 0) OnBackground else TextSecondary,
                fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis
            )
            // Label phụ (tên địa điểm nếu là owner view)
            if (isOwner && room.placeName.isNotEmpty()) {
                Text(
                    "📍 ${room.placeName}",
                    style   = MaterialTheme.typography.labelSmall,
                    color   = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}