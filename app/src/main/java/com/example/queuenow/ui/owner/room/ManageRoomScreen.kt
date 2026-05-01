package com.example.queuenow.ui.owner.room

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.RoomStatus
import com.example.queuenow.data.model.WaitingRoom
import com.example.queuenow.data.repository.WaitingRoomRepository
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ManageRoomViewModel(private val placeId: String) : ViewModel() {
    companion object {
        fun factory(placeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ManageRoomViewModel(placeId) as T
            }
    }

    private val repo = WaitingRoomRepository()
    private val _rooms = MutableStateFlow<List<WaitingRoom>>(emptyList())
    val rooms = _rooms.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getRooms(placeId).collectLatest {
                _rooms.value = it
                _isLoading.value = false
            }
        }
    }

    fun toggleStatus(room: WaitingRoom) {
        viewModelScope.launch {
            val newStatus = when (room.status) {
                RoomStatus.OPEN.name   -> RoomStatus.CLOSED
                RoomStatus.CLOSED.name -> RoomStatus.OPEN
                RoomStatus.PAUSED.name -> RoomStatus.OPEN
                else -> RoomStatus.CLOSED
            }
            repo.updateStatus(placeId, room.roomId, newStatus)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRoomScreen(
    navController: NavController,
    placeId: String,
    vm: ManageRoomViewModel = viewModel(factory = ManageRoomViewModel.factory(placeId))
) {
    val rooms by vm.rooms.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý phòng chờ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Thêm phòng") },
                icon = { Icon(Icons.Filled.Add, null) },
                onClick = { navController.navigate(Screen.EditRoom.createRoute(placeId, "new")) },
                containerColor = Primary,
                contentColor = Color.White
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }
        if (rooms.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.MeetingRoom, null, tint = Divider, modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(16.dp))
                Text("Chưa có phòng chờ", color = TextSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(rooms, key = { it.roomId }) { room ->
                RoomManageCard(
                    room = room,
                    onEdit = { navController.navigate(Screen.EditRoom.createRoute(placeId, room.roomId)) },
                    onLiveQueue = { navController.navigate(Screen.LiveQueue.createRoute(placeId, room.roomId)) },
                    onToggle = { vm.toggleStatus(room) }
                )
            }
        }
    }
}

@Composable
private fun RoomManageCard(
    room: WaitingRoom,
    onEdit: () -> Unit,
    onLiveQueue: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(room.roomName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (room.roomType.isNotEmpty())
                        Text(room.roomType, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(room.status)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = room.status == RoomStatus.OPEN.name,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusCompleted)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("~${room.estimatedServiceTime} phút/người", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (room.prepaymentRequired) {
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.Payment, null, tint = Secondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${String.format("%,.0f", room.prepaymentAmount)}đ", style = MaterialTheme.typography.bodySmall, color = Secondary)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sửa")
                }
                Button(
                    onClick = onLiveQueue,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.LiveTv, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hàng đợi")
                }
            }
        }
    }
}