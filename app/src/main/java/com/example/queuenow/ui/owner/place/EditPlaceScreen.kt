package com.example.queuenow.ui.owner.place

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.model.PlaceStatus
import com.example.queuenow.data.repository.AuthRepository
import com.example.queuenow.data.repository.PlaceRepository
import com.example.queuenow.data.service.CloudinaryService
import com.example.queuenow.ui.components.GradientButton
import com.example.queuenow.ui.components.LoadingOverlay
import com.example.queuenow.ui.components.QueueTextField
import com.example.queuenow.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class EditPlaceState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingImage: Boolean = false,
    val place: Place? = null,
    val isNewPlace: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)

class EditPlaceViewModel(private val placeId: String) : ViewModel() {
    companion object {
        fun factory(placeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EditPlaceViewModel(placeId) as T
            }
    }

    private val placeRepo = PlaceRepository()
    private val authRepo  = AuthRepository()

    private val _state = MutableStateFlow(EditPlaceState())
    val state = _state.asStateFlow()

    init {
        if (placeId == "new") {
            _state.update { it.copy(isNewPlace = true, place = Place()) }
        } else {
            loadPlace()
        }
    }

    private fun loadPlace() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val place = placeRepo.getPlace(placeId)
            _state.update { it.copy(place = place, isLoading = false) }
        }
    }

    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingImage = true) }
            try {
                val url = CloudinaryService.uploadImage(uri, "places")
                _state.update { s ->
                    s.copy(place = s.place?.copy(imageUrl = url), isUploadingImage = false)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Upload ảnh thất bại: ${e.message}", isUploadingImage = false) }
            }
        }
    }

    fun save(name: String, address: String, phone: String,
             description: String, openTime: String, closeTime: String,
             isOpen: Boolean) {
        viewModelScope.launch {
            if (name.isBlank() || address.isBlank()) {
                _state.update { it.copy(error = "Tên và địa chỉ không được để trống") }
                return@launch
            }
            val uid = authRepo.getCurrentUserId() ?: return@launch
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val existing = _state.value.place ?: Place()

                // Nếu bị admin khóa → GIỮ nguyên status LOCKED, không cho đổi
                val finalStatus = if (existing.lockedByAdmin)
                    PlaceStatus.LOCKED.name
                else if (isOpen) PlaceStatus.OPEN.name else PlaceStatus.CLOSED.name

                val toSave = existing.copy(
                    ownerId     = if (_state.value.isNewPlace) uid else existing.ownerId,
                    placeName   = name.trim(),
                    address     = address.trim(),
                    phone       = phone.trim(),
                    description = description.trim(),
                    openTime    = openTime.trim(),
                    closeTime   = closeTime.trim(),
                    status      = finalStatus
                    // lockedByAdmin KHÔNG được thay đổi bởi owner
                )
                placeRepo.savePlace(toSave)
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
fun EditPlaceScreen(
    navController: NavController,
    placeId: String,
    vm: EditPlaceViewModel = viewModel(factory = EditPlaceViewModel.factory(placeId))
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var name        by remember { mutableStateOf("") }
    var address     by remember { mutableStateOf("") }
    var phone       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var openTime    by remember { mutableStateOf("") }
    var closeTime   by remember { mutableStateOf("") }
    var isOpen      by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    // Khởi tạo form từ dữ liệu loaded
    LaunchedEffect(state.place) {
        if (!initialized && state.place != null) {
            state.place!!.let { p ->
                name        = p.placeName
                address     = p.address
                phone       = p.phone
                description = p.description
                openTime    = p.openTime
                closeTime   = p.closeTime
                isOpen      = p.status == PlaceStatus.OPEN.name
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

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.uploadImage(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (placeId == "new") "Thêm địa điểm" else "Chỉnh sửa địa điểm") },
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
                // ── Image picker ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Divider, RoundedCornerShape(16.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val imgUrl = state.place?.imageUrl
                    if (!imgUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = imgUrl, contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isUploadingImage) {
                                CircularProgressIndicator(color = Color.White)
                            } else {
                                Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (state.isUploadingImage) {
                                CircularProgressIndicator(color = Primary)
                            } else {
                                Icon(Icons.Filled.AddAPhoto, null, tint = Primary, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Chọn ảnh đại diện", color = Primary)
                            }
                        }
                    }
                }

                // ── Form fields ──────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Thông tin địa điểm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        QueueTextField(
                            value = name, onValueChange = { name = it }, label = "Tên địa điểm *",
                            leadingIcon = { Icon(Icons.Filled.Store, null, tint = Primary) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        )
                        QueueTextField(
                            value = address, onValueChange = { address = it }, label = "Địa chỉ *",
                            leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = Primary) }
                        )
                        QueueTextField(
                            value = phone, onValueChange = { phone = it }, label = "Số điện thoại",
                            leadingIcon = { Icon(Icons.Filled.Phone, null, tint = Primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        QueueTextField(
                            value = description, onValueChange = { description = it },
                            label = "Mô tả",
                            leadingIcon = { Icon(Icons.Filled.Description, null, tint = Primary) },
                            singleLine = false, maxLines = 3
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Giờ hoạt động", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QueueTextField(
                                value = openTime, onValueChange = { openTime = it },
                                label = "Mở cửa (VD: 08:00)",
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Filled.AccessTime, null, tint = Primary) }
                            )
                            QueueTextField(
                                value = closeTime, onValueChange = { closeTime = it },
                                label = "Đóng cửa",
                                modifier = Modifier.weight(1f),
                                leadingIcon = { Icon(Icons.Filled.AccessTime, null, tint = TextSecondary) }
                            )
                        }
                    }
                }

                // ── Trạng thái (disabled nếu bị admin khóa) ─────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (state.place?.lockedByAdmin == true) {
                            // Bị Admin khóa → hiện thông báo, không cho toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Lock, null, tint = StatusCanceled, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Địa điểm đang bị Admin khóa",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold, color = StatusCanceled)
                                    Text("Bạn không thể thay đổi trạng thái cho đến khi Admin mở khóa.",
                                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Trạng thái mở cửa",
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (isOpen) "Địa điểm đang mở" else "Địa điểm đang đóng",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOpen) StatusCompleted else TextSecondary
                                    )
                                }
                                Switch(
                                    checked  = isOpen,
                                    onCheckedChange = { isOpen = it },
                                    colors   = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = StatusCompleted
                                    )
                                )
                            }
                        }
                    }
                }

                GradientButton(
                    text = if (state.isSaving) "Đang lưu..." else "Lưu địa điểm",
                    onClick = { vm.save(name, address, phone, description, openTime, closeTime, isOpen) },
                    enabled = !state.isSaving && !state.isUploadingImage,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
            }

            if (state.isSaving) LoadingOverlay()
        }
    }
}