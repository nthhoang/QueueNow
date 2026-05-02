package com.example.queuenow.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.Message
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatRoomId: String = "",
    placeId: String = "",
    ownerId: String = "",
    vm: ChatViewModel = viewModel(
        factory = if (chatRoomId.isNotBlank())
            ChatViewModel.factoryFromRoomId(chatRoomId)
        else
            ChatViewModel.factoryFromPlace(placeId, ownerId)
    )
) {
    val state        by vm.state.collectAsState()
    val listState    = rememberLazyListState()
    val scope        = rememberCoroutineScope()
    var inputText    by remember { mutableStateOf("") }
    val snackbarHost = remember { SnackbarHostState() }

    // Scroll xuống cuối khi có tin nhắn mới
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar(it); vm.clearError() }
    }

    // Tên hiển thị ở TopAppBar
    val topBarTitle = state.chatRoom?.let { room ->
        if (state.isOwnerMode) room.userName       // Owner xem → hiện tên user
        else room.placeName                        // User xem → hiện tên địa điểm
    } ?: "Nhắn tin"

    val topBarSubtitle = state.chatRoom?.let { room ->
        if (state.isOwnerMode) "Khách hàng"
        else room.ownerName
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(topBarTitle, fontWeight = FontWeight.Bold, maxLines = 1)
                        topBarSubtitle?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // ── Input bar ────────────────────────────────────────────────────
            Surface(
                color       = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        placeholder   = { Text("Nhập tin nhắn...", color = TextSecondary) },
                        modifier      = Modifier.weight(1f),
                        shape         = RoundedCornerShape(24.dp),
                        maxLines      = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction      = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    vm.sendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Primary,
                            unfocusedBorderColor = Divider
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    // Nút gửi
                    Surface(
                        shape  = CircleShape,
                        color  = if (inputText.isNotBlank()) Primary else Divider,
                        modifier = Modifier.size(46.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !state.isSending) {
                                    vm.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !state.isSending
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(
                                    color    = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Send, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }
            }
            else -> {
                if (state.messages.isEmpty()) {
                    // Gợi ý khi chưa có tin nhắn
                    Column(
                        modifier            = Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Chat, null,
                            tint     = Divider,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Bắt đầu cuộc trò chuyện!", color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (state.isOwnerMode)
                                "Hãy phản hồi khách hàng"
                            else "Hỏi chủ địa điểm bất cứ điều gì",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        state           = listState,
                        modifier        = Modifier.fillMaxSize().padding(padding),
                        contentPadding  = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.messages, key = { it.messageId }) { msg ->
                            MessageBubble(
                                message = msg,
                                isMe    = msg.senderId == state.currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMe: Boolean) {
    val alignment     = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor   = if (isMe) Primary else Color.White
    val textColor     = if (isMe) Color.White else OnBackground
    val cornerRadius  = 18.dp
    val myCorner      = 4.dp

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Tên người gửi (chỉ hiện bên nhận)
        if (!isMe) {
            Text(
                message.senderName,
                style    = MaterialTheme.typography.labelSmall,
                color    = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart    = if (isMe) cornerRadius else myCorner,
                topEnd      = if (isMe) myCorner else cornerRadius,
                bottomStart = cornerRadius,
                bottomEnd   = cornerRadius
            ),
            color = bubbleColor,
            shadowElevation = if (isMe) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    text       = message.content,
                    color      = textColor,
                    style      = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = message.timestamp.toFormattedDate("HH:mm"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) Color.White.copy(0.75f) else TextSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}