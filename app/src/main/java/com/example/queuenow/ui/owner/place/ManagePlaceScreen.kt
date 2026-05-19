package com.example.queuenow.ui.owner.place

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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.model.PlaceStatus
import com.example.queuenow.data.repository.PlaceRepository
import com.example.queuenow.ui.components.OwnerBottomNav
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ── ViewModel ────────────────────────────────────────────────────────────────
class ManagePlaceViewModel : ViewModel() {
    private val placeRepo = PlaceRepository()

    private val _places    = MutableStateFlow<List<Place>>(emptyList())
    val places = _places.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _message   = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    // UID của owner hiện tại — lấy từ Firebase Auth trực tiếp
    private var currentOwnerId: String? = null

    private var collectJob: Job? = null

    init { loadPlaces() }

    private fun loadPlaces() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            _isLoading.value = true

            // Chờ Firebase Auth session
            var uid: String? = null
            repeat(8) {
                uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) return@repeat
                delay(500L)
            }

            if (uid == null) {
                _isLoading.value = false
                return@launch
            }

            currentOwnerId = uid

            placeRepo.getPlacesByOwner(uid!!).collectLatest { places ->
                // Double-check: chỉ hiển thị place của chính mình
                val myPlaces = places.filter { it.ownerId == uid }
                _places.value    = myPlaces
                _isLoading.value = false
            }
        }
    }

    fun toggleStatus(place: Place) {
        // Kiểm tra quyền sở hữu
        if (place.ownerId != currentOwnerId) {
            _message.value = "Bạn không có quyền thay đổi địa điểm này"
            return
        }
        if (place.lockedByAdmin) {
            _message.value = "Địa điểm đang bị Admin khóa, không thể thay đổi trạng thái"
            return
        }
        viewModelScope.launch {
            val newStatus = if (place.status == PlaceStatus.OPEN.name)
                PlaceStatus.CLOSED else PlaceStatus.OPEN
            try {
                placeRepo.updateStatus(place.placeId, newStatus)
            } catch (e: Exception) {
                _message.value = e.message ?: "Lỗi thay đổi trạng thái"
            }
        }
    }

    fun clearMessage() { _message.value = null }

    override fun onCleared() {
        super.onCleared()
        collectJob?.cancel()
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlaceScreen(
    navController: NavController,
    vm: ManagePlaceViewModel = viewModel()
) {
    val places    by vm.places.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val message   by vm.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); vm.clearMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Địa điểm của tôi", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text           = { Text("Thêm địa điểm") },
                icon           = { Icon(Icons.Filled.Add, null) },
                onClick        = { navController.navigate(Screen.EditPlace.createRoute("new")) },
                containerColor = Primary,
                contentColor   = Color.White
            )
        },
        bottomBar      = { OwnerBottomNav(navController) },
        containerColor = BackgroundLight
    ) { padding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            places.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Store, null, tint = Divider, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Chưa có địa điểm nào", color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Text("Nhấn + để thêm địa điểm mới",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(places, key = { it.placeId }) { place ->
                        OwnerPlaceManageCard(
                            place          = place,
                            onEdit         = {
                                navController.navigate(Screen.EditPlace.createRoute(place.placeId))
                            },
                            onManageRooms  = {
                                navController.navigate(Screen.ManageRoom.createRoute(place.placeId))
                            },
                            onToggleStatus = { vm.toggleStatus(place) },
                            onPayments     = {
                                navController.navigate(Screen.PaymentConfirm.createRoute(place.placeId))
                            },
                            onViewStats    = {
                                navController.navigate(Screen.OwnerStats.createRoute(place.placeId))
                            },
                            onViewQr = {
                                navController.navigate(Screen.PlaceQr.createRoute(place.placeId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerPlaceManageCard(
    place: Place,
    onEdit: () -> Unit,
    onManageRooms: () -> Unit,
    onToggleStatus: () -> Unit,
    onPayments: () -> Unit,
    onViewStats: () -> Unit,
    onViewQr: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tên + trạng thái
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(place.placeName,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(place.address,
                        style   = MaterialTheme.typography.bodySmall,
                        color   = TextSecondary, maxLines = 1)
                }
                Spacer(Modifier.width(8.dp))
                if (place.lockedByAdmin) {
                    Surface(shape = RoundedCornerShape(8.dp), color = StatusCanceled.copy(0.1f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lock, null,
                                tint = StatusCanceled, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Admin khóa",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusCanceled, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(place.status)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked  = place.status == PlaceStatus.OPEN.name,
                            onCheckedChange = { onToggleStatus() },
                            colors   = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StatusCompleted
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(10.dp))

            // Nút hành động
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onManageRooms, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) {
                    Icon(Icons.Filled.MeetingRoom, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Phòng", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onPayments, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) {
                    Icon(Icons.Filled.Payment, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("TT", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onViewStats, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) {
                    Icon(Icons.Filled.BarChart, null, tint = Accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Thống kê", style = MaterialTheme.typography.labelMedium, color = Accent)
                }
                Button(
                    onClick  = onEdit,
                    enabled  = !place.lockedByAdmin,
                    colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = onViewQr,
                    modifier = Modifier.size(40.dp),
                    shape    = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Secondary),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Secondary.copy(0.5f))
                ) {
                    Icon(Icons.Filled.QrCode, null, tint = Secondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}