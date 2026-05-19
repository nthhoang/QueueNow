package com.example.queuenow.ui.owner.place

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.repository.PlaceRepository
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.QrCodeUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────
data class PlaceQrState(
    val isLoading: Boolean = true,
    val place: Place? = null,
    val qrContent: String = "",
    val isRotating: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class PlaceQrViewModel(private val placeId: String) : ViewModel() {
    companion object {
        fun factory(placeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PlaceQrViewModel(placeId) as T
            }
        fun buildQrContent(placeId: String, secret: String) =
            "QUEUENOW|$placeId|$secret"
    }

    private val repo = PlaceRepository()
    private val _state = MutableStateFlow(PlaceQrState())
    val state = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                // Đảm bảo place có secret
                val secret = repo.ensureQrSecret(placeId)
                val place  = repo.getPlace(placeId)
                val content = buildQrContent(placeId, secret)
                _state.update {
                    it.copy(isLoading = false, place = place, qrContent = content)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** Owner muốn đổi QR (vô hiệu hóa QR cũ) */
    fun rotateSecret() {
        viewModelScope.launch {
            _state.update { it.copy(isRotating = true) }
            try {
                val newSecret = repo.rotateQrSecret(placeId)
                val place     = repo.getPlace(placeId)
                val content   = buildQrContent(placeId, newSecret)
                _state.update {
                    it.copy(
                        isRotating = false,
                        place      = place,
                        qrContent  = content,
                        message    = "✓ Đã đổi mã QR mới. Mã cũ không còn hiệu lực."
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isRotating = false, error = e.message) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(message = null, error = null) }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceQrScreen(
    navController: NavController,
    placeId: String,
    vm: PlaceQrViewModel = viewModel(factory = PlaceQrViewModel.factory(placeId))
) {
    val state         by vm.state.collectAsState()
    val snackbarHost  = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHost.showSnackbar(it); vm.clearMessages() }
    }

    val qrBitmap = remember(state.qrContent) {
        if (state.qrContent.isBlank()) null
        else runCatching { QrCodeUtils.generateQrCode(state.qrContent, 600) }.getOrNull()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Mã QR Check-in", fontWeight = FontWeight.Bold) },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Tên địa điểm ───────────────────────────────────────
            Text(
                state.place?.placeName ?: "",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                state.place?.address ?: "",
                style   = MaterialTheme.typography.bodyMedium,
                color   = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // ── QR Code ─────────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap              = qrBitmap.asImageBitmap(),
                            contentDescription  = "QR Code",
                            modifier            = Modifier.size(260.dp)
                        )
                    } else {
                        Box(
                            modifier            = Modifier
                                .size(260.dp)
                                .background(BackgroundLight, RoundedCornerShape(12.dp)),
                            contentAlignment    = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Dành cho khách scan để lấy số",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Hướng dẫn ───────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = PrimaryLight),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hướng dẫn sử dụng", fontWeight = FontWeight.Bold, color = Primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "In hoặc hiển thị mã QR này tại quầy lễ tân",
                        "Khách phải đến địa điểm và dùng app để scan",
                        "Mã xác thực có hiệu lực 5 phút sau khi scan",
                        "Nhấn \"Đổi mã QR\" nếu muốn vô hiệu mã cũ"
                    ).forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", color = Primary, fontWeight = FontWeight.Bold)
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = Primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Nút đổi mã QR ───────────────────────────────────────
            OutlinedButton(
                onClick  = { vm.rotateSecret() },
                enabled  = !state.isRotating,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = StatusCanceled),
                border   = androidx.compose.foundation.BorderStroke(1.dp, StatusCanceled)
            ) {
                if (state.isRotating) {
                    CircularProgressIndicator(
                        color       = StatusCanceled,
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Đang đổi mã...")
                } else {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đổi mã QR (vô hiệu mã cũ)", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "⚠️ Khi đổi mã, tất cả mã QR cũ sẽ không còn hoạt động",
                style     = MaterialTheme.typography.labelSmall,
                color     = StatusCanceled,
                textAlign = TextAlign.Center
            )
        }
    }
}