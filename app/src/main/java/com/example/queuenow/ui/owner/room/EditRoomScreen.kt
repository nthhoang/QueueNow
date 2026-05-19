package com.example.queuenow.ui.owner.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.RoomStatus
import com.example.queuenow.data.model.WaitingRoom
import com.example.queuenow.data.repository.WaitingRoomRepository
import com.example.queuenow.ui.components.GradientButton
import com.example.queuenow.ui.components.LoadingOverlay
import com.example.queuenow.ui.components.QueueTextField
import com.example.queuenow.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class EditRoomState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val room: WaitingRoom? = null,
    val isNew: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

class EditRoomViewModel(
    private val placeId: String,
    private val roomId: String
) : ViewModel() {
    companion object {
        fun factory(placeId: String, roomId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditRoomViewModel(placeId, roomId) as T
            }
    }

    private val repo = WaitingRoomRepository()
    private val _state = MutableStateFlow(EditRoomState())
    val state = _state.asStateFlow()

    init {
        if (roomId == "new") {
            _state.update { it.copy(isNew = true, room = WaitingRoom(placeId = placeId)) }
        } else {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                val room = repo.getRoom(placeId, roomId)
                _state.update { it.copy(room = room, isLoading = false) }
            }
        }
    }

    fun save(
        roomName: String, roomType: String, locationNote: String,
        description: String, estimatedTime: Int,
        prepaymentRequired: Boolean, prepaymentAmount: Double,
        isOpen: Boolean, requireQrScan: Boolean
    ) {
        viewModelScope.launch {
            if (roomName.isBlank()) {
                _state.update { it.copy(error = "Tên phòng không được để trống") }
                return@launch
            }
            val existing = _state.value.room ?: return@launch
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val toSave = existing.copy(
                    placeId = placeId,
                    roomName = roomName.trim(),
                    roomType = roomType.trim(),
                    locationNote = locationNote.trim(),
                    description = description.trim(),
                    estimatedServiceTime = estimatedTime,
                    prepaymentRequired = prepaymentRequired,
                    prepaymentAmount = prepaymentAmount,
                    status = if (isOpen) RoomStatus.OPEN.name else RoomStatus.CLOSED.name,
                    requireQrScan = requireQrScan
                )
                repo.saveRoom(placeId, toSave)
                _state.update { it.copy(savedSuccessfully = true, isSaving = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Lỗi lưu", isSaving = false) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoomScreen(
    navController: NavController,
    placeId: String,
    roomId: String,
    vm: EditRoomViewModel = viewModel(factory = EditRoomViewModel.factory(placeId, roomId))
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var roomName         by remember { mutableStateOf("") }
    var roomType         by remember { mutableStateOf("") }
    var locationNote     by remember { mutableStateOf("") }
    var description      by remember { mutableStateOf("") }
    var estimatedTime    by remember { mutableStateOf(10) }
    var prepayRequired   by remember { mutableStateOf(false) }
    var prepayAmount     by remember { mutableStateOf("") }
    var isOpen           by remember { mutableStateOf(false) }
    var initialized      by remember { mutableStateOf(false) }
    var requireQrScan    by remember { mutableStateOf(false) }

    LaunchedEffect(state.room) {
        if (!initialized && state.room != null) {
            state.room!!.let { r ->
                roomName      = r.roomName
                roomType      = r.roomType
                locationNote  = r.locationNote
                description   = r.description
                estimatedTime = r.estimatedServiceTime
                prepayRequired = r.prepaymentRequired
                prepayAmount  = if (r.prepaymentAmount > 0) r.prepaymentAmount.toInt().toString() else ""
                isOpen        = r.status == RoomStatus.OPEN.name
                requireQrScan = r.requireQrScan
            }
            initialized = true
        }
    }
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) navController.popBackStack()
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (roomId == "new") "Thêm phòng chờ" else "Chỉnh sửa phòng") },
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
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Thông tin cơ bản ─────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Thông tin phòng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        QueueTextField(
                            value = roomName, onValueChange = { roomName = it },
                            label = "Tên phòng *",
                            leadingIcon = { Icon(Icons.Filled.MeetingRoom, null, tint = Primary) }
                        )
                        QueueTextField(
                            value = roomType, onValueChange = { roomType = it },
                            label = "Loại dịch vụ (VD: Tư vấn, Thủ tục...)",
                            leadingIcon = { Icon(Icons.Filled.Category, null, tint = Primary) }
                        )
                        QueueTextField(
                            value = locationNote, onValueChange = { locationNote = it },
                            label = "Lưu ý vị trí (VD: Tầng 2, Cửa A)",
                            leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = Primary) }
                        )
                        QueueTextField(
                            value = description, onValueChange = { description = it },
                            label = "Mô tả", singleLine = false, maxLines = 3,
                            leadingIcon = { Icon(Icons.Filled.Description, null, tint = Primary) }
                        )
                    }
                }

                // ── Thời gian ────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Thời gian phục vụ ước tính", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$estimatedTime phút / người", style = MaterialTheme.typography.bodyLarge, color = Primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = estimatedTime.toFloat(),
                            onValueChange = { estimatedTime = it.toInt() },
                            valueRange = 1f..60f,
                            steps = 58,
                            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1 phút", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("60 phút", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }

                // ── Thanh toán trước ─────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Yêu cầu thanh toán trước", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Khách phải upload ảnh CK trước khi xếp hàng", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Switch(
                                checked = prepayRequired,
                                onCheckedChange = { prepayRequired = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Secondary)
                            )
                        }
                        if (prepayRequired) {
                            Spacer(Modifier.height(12.dp))
                            QueueTextField(
                                value = prepayAmount, onValueChange = { prepayAmount = it },
                                label = "Số tiền (VNĐ)",
                                leadingIcon = { Icon(Icons.Filled.AttachMoney, null, tint = Secondary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                // Thêm Card toggle (sau prepayment card):
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.QrCodeScanner, null,
                                    tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Yêu cầu quét QR", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "User phải đến địa điểm và quét mã QR mới lấy được số",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked  = requireQrScan,
                            onCheckedChange = { requireQrScan = it },
                            colors   = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary
                            )
                        )
                    }
                }

                // ── Trạng thái ───────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mở hàng đợi ngay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (isOpen) "Phòng đang nhận khách" else "Phòng chưa mở", style = MaterialTheme.typography.bodySmall, color = if (isOpen) StatusCompleted else TextSecondary)
                        }
                        Switch(
                            checked = isOpen, onCheckedChange = { isOpen = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusCompleted)
                        )
                    }
                }

                GradientButton(
                    text = if (state.isSaving) "Đang lưu..." else "Lưu phòng chờ",
                    onClick = {
                        val amount = prepayAmount.toDoubleOrNull() ?: 0.0
                        vm.save(roomName, roomType, locationNote, description, estimatedTime, prepayRequired, amount, isOpen, requireQrScan)
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            if (state.isSaving) LoadingOverlay()
        }
    }
}
