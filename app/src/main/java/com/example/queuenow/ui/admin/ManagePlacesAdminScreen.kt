package com.example.queuenow.ui.admin

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.queuenow.data.model.AppNotification
import com.example.queuenow.data.model.NotificationType
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.repository.NotificationRepository
import com.example.queuenow.data.repository.PlaceRepository
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class ManagePlacesAdminState(
    val isLoading: Boolean = true,
    val places: List<Place> = emptyList(),
    val filteredPlaces: List<Place> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "ALL",
    val error: String? = null,
    val message: String? = null
)

class ManagePlacesAdminViewModel : ViewModel() {
    private val repo = PlaceRepository()

    private val _state = MutableStateFlow(ManagePlacesAdminState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllPlaces().collectLatest { places ->
                _state.update { s ->
                    s.copy(
                        places         = places,
                        filteredPlaces = applyFilter(places, s.searchQuery, s.selectedFilter),
                        isLoading      = false
                    )
                }
            }
        }
    }

    fun search(query: String) {
        _state.update { s ->
            s.copy(searchQuery = query, filteredPlaces = applyFilter(s.places, query, s.selectedFilter))
        }
    }

    fun setFilter(filter: String) {
        _state.update { s ->
            s.copy(selectedFilter = filter, filteredPlaces = applyFilter(s.places, s.searchQuery, filter))
        }
    }

    private fun applyFilter(places: List<Place>, query: String, filter: String): List<Place> {
        var result = places
        if (query.isNotBlank()) {
            result = result.filter {
                it.placeName.contains(query, true) || it.address.contains(query, true)
            }
        }
        result = when (filter) {
            "OPEN"   -> result.filter { it.status == "OPEN" && !it.lockedByAdmin }
            "CLOSED" -> result.filter { it.status == "CLOSED" && !it.lockedByAdmin }
            "LOCKED" -> result.filter { it.lockedByAdmin }
            else     -> result
        }
        return result
    }

    fun toggleLock(place: Place) {
        viewModelScope.launch {
            try {
                if (place.lockedByAdmin) {
                    repo.adminUnlockPlace(place.placeId)
                    // Thông báo owner mở khóa
                    NotificationRepository().sendNotification(
                        AppNotification(
                            userId = place.ownerId,
                            type = NotificationType.PLACE_UNLOCKED_BY_ADMIN.name,
                            title = "Địa điểm đã được mở khóa",
                            message = "\"${place.placeName}\" đã được Admin mở khóa. Bạn có thể quản lý lại bình thường.",
                            placeId = place.placeId
                        )
                    )
                    _state.update { it.copy(message = "Đã mở khóa \"${place.placeName}\"") }
                } else {
                    repo.adminLockPlace(place.placeId)
                    // Thông báo owner bị khóa
                    NotificationRepository().sendNotification(
                        AppNotification(
                            userId  = place.ownerId,
                            type    = NotificationType.PLACE_LOCKED_BY_ADMIN.name,
                            title   = "⚠️ Địa điểm bị Admin khóa",
                            message = "\"${place.placeName}\" đã bị Admin khóa. Vui lòng liên hệ để biết thêm thông tin.",
                            placeId = place.placeId
                        )
                    )
                    _state.update { it.copy(message = "Đã khóa \"${place.placeName}\"") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Lỗi xử lý") }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, message = null) }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlacesAdminScreen(
    navController: NavController,
    vm: ManagePlacesAdminViewModel = viewModel()
) {
    val state             by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var placeToToggle     by remember { mutableStateOf<Place?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }

    val filterOptions = listOf(
        "ALL" to "Tất cả",
        "OPEN" to "Đang mở",
        "CLOSED" to "Đóng",
        "LOCKED" to "Bị khóa"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Quản lý địa điểm", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Search + filter ──────────────────────────────────────
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Column {
                    TextField(
                        value = state.searchQuery,
                        onValueChange = { vm.search(it) },
                        placeholder   = { Text("Tìm tên, địa chỉ...", color = TextSecondary) },
                        leadingIcon   = { Icon(Icons.Filled.Search, null, tint = Primary) },
                        trailingIcon  = {
                            if (state.searchQuery.isNotEmpty())
                                IconButton(onClick = { vm.search("") }) {
                                    Icon(Icons.Filled.Clear, null, tint = TextSecondary)
                                }
                        },
                        modifier   = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape      = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors     = TextFieldDefaults.colors(
                            focusedContainerColor   = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    ScrollableTabRow(
                        selectedTabIndex = filterOptions.indexOfFirst { it.first == state.selectedFilter }
                            .coerceAtLeast(0),
                        containerColor = Color.White,
                        contentColor   = Primary,
                        edgePadding    = 16.dp,
                        indicator      = {}
                    ) {
                        filterOptions.forEach { (key, label) ->
                            val isSelected = state.selectedFilter == key
                            Tab(
                                selected = isSelected,
                                onClick  = { vm.setFilter(key) },
                                text = {
                                    Surface(
                                        shape  = RoundedCornerShape(20.dp),
                                        color  = if (isSelected) Primary else Divider,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(label,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Text("${state.filteredPlaces.size} địa điểm",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredPlaces, key = { it.placeId }) { place ->
                        AdminPlaceCard(
                            place        = place,
                            onToggleLock = { placeToToggle = place }
                        )
                    }
                }
            }
        }
    }

    // ── Confirm dialog ──────────────────────────────────────────────────────
    placeToToggle?.let { place ->
        val isLocking = !place.lockedByAdmin
        AlertDialog(
            onDismissRequest = { placeToToggle = null },
            icon = {
                Icon(
                    if (isLocking) Icons.Filled.Lock else Icons.Filled.LockOpen, null,
                    tint = if (isLocking) StatusCanceled else StatusCompleted
                )
            },
            title = { Text(if (isLocking) "Khóa địa điểm?" else "Mở khóa địa điểm?") },
            text  = {
                Text(
                    if (isLocking)
                        "Khóa \"${place.placeName}\"?\nOwner sẽ không thể mở địa điểm này cho đến khi Admin mở khóa."
                    else
                        "Mở khóa \"${place.placeName}\"?\nOwner có thể tự quản lý trạng thái địa điểm trở lại."
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.toggleLock(place); placeToToggle = null },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = if (isLocking) StatusCanceled else StatusCompleted
                    )
                ) { Text(if (isLocking) "Khóa" else "Mở khóa") }
            },
            dismissButton = {
                TextButton(onClick = { placeToToggle = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun AdminPlaceCard(place: Place, onToggleLock: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (place.lockedByAdmin) StatusCanceled.copy(0.04f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail
            Surface(shape = RoundedCornerShape(10.dp), color = PrimaryLight,
                modifier = Modifier.size(56.dp)) {
                if (place.imageUrl.isNotEmpty()) {
                    AsyncImage(model = place.imageUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Store, null, tint = Primary, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(place.placeName,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (place.lockedByAdmin) TextSecondary else OnBackground)
                Text(place.address, style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (place.lockedByAdmin) {
                        // Hiện badge "Admin khóa" thay vì status thường
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = StatusCanceled.copy(0.15f)) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, null,
                                    tint = StatusCanceled, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Admin khóa",
                                    style = MaterialTheme.typography.labelSmall, color = StatusCanceled,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        StatusChip(place.status)
                    }
                    if (place.ratingAverage > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null,
                                tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(String.format("%.1f", place.ratingAverage),
                                style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }

            IconButton(onClick = onToggleLock) {
                Icon(
                    if (place.lockedByAdmin) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = if (place.lockedByAdmin) "Mở khóa" else "Khóa Admin",
                    tint = if (place.lockedByAdmin) StatusCompleted else StatusCanceled
                )
            }
        }
    }
}