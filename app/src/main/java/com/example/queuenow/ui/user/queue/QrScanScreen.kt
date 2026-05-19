package com.example.queuenow.ui.user.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.repository.PlaceRepository
import com.example.queuenow.ui.components.CameraQrScanner
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.QrScanTokenManager
import com.google.accompanist.permissions.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────
sealed class QrScanResult {
    object Idle       : QrScanResult()
    object Scanning   : QrScanResult()
    object Verifying  : QrScanResult()
    data class Success(val placeId: String) : QrScanResult()
    data class Error(val message: String)   : QrScanResult()
}

class QrScanViewModel(
    private val expectedPlaceId: String   // placeId cần xác thực
) : ViewModel() {

    companion object {
        fun factory(placeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QrScanViewModel(placeId) as T
            }
    }

    private val placeRepo = PlaceRepository()

    private val _result = MutableStateFlow<QrScanResult>(QrScanResult.Idle)
    val result = _result.asStateFlow()

    /**
     * Gọi khi camera scan được QR
     * Format hợp lệ: QUEUENOW|{placeId}|{secret}
     */
    fun onQrScanned(rawValue: String) {
        if (_result.value is QrScanResult.Verifying) return
        _result.value = QrScanResult.Verifying

        viewModelScope.launch {
            try {
                val parts = rawValue.split("|")
                if (parts.size != 3 || parts[0] != "QUEUENOW") {
                    _result.value = QrScanResult.Error("Mã QR không hợp lệ. Vui lòng dùng mã QR của QueueNow.")
                    return@launch
                }

                val placeId = parts[1]
                val secret  = parts[2]

                // Kiểm tra đúng địa điểm
                if (placeId != expectedPlaceId) {
                    _result.value = QrScanResult.Error(
                        "Mã QR không thuộc địa điểm này. Hãy quét đúng mã QR tại quầy lễ tân."
                    )
                    return@launch
                }

                // Xác thực secret với Firestore
                val isValid = placeRepo.verifyQrCode(placeId, secret)
                if (isValid) {
                    // Cấp token hợp lệ 5 phút
                    QrScanTokenManager.grantToken(placeId)
                    _result.value = QrScanResult.Success(placeId)
                } else {
                    _result.value = QrScanResult.Error(
                        "Mã QR đã hết hạn hoặc không hợp lệ.\nVui lòng yêu cầu nhân viên cập nhật mã QR."
                    )
                }
            } catch (e: Exception) {
                _result.value = QrScanResult.Error("Lỗi xác thực: ${e.message}")
            }
        }
    }

    fun retry() { _result.value = QrScanResult.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(
    navController: NavController,
    placeId: String,
    roomId: String,
    vm: QrScanViewModel = viewModel(factory = QrScanViewModel.factory(placeId))
) {
    val result          by vm.result.collectAsState()
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Khi scan thành công → navigate về TakeTicketScreen
    LaunchedEffect(result) {
        if (result is QrScanResult.Success) {
            // popBackStack để quay lại TakeTicketScreen với token đã có
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quét mã QR Check-in", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when {
            // ── Chưa cấp quyền camera ──────────────────────────────
            !cameraPermission.status.isGranted -> {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt, null,
                        tint     = Primary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Cần quyền truy cập camera",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "QueueNow cần camera để quét mã QR check-in tại địa điểm",
                        textAlign = TextAlign.Center,
                        color     = TextSecondary
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick  = { cameraPermission.launchPermissionRequest() },
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape    = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cấp quyền camera") }
                }
            }

            // ── Đang scan camera ───────────────────────────────────
            result is QrScanResult.Idle || result is QrScanResult.Verifying -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraQrScanner(
                        hint         = "Quét mã QR tại quầy lễ tân của địa điểm",
                        onQrScanned  = { vm.onQrScanned(it) }
                    )
                }
            }

            // ── Lỗi ────────────────────────────────────────────────
            result is QrScanResult.Error -> {
                val errorMsg = (result as QrScanResult.Error).message
                Column(
                    modifier            = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.ErrorOutline, null,
                        tint = StatusCanceled, modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Xác thực thất bại",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = StatusCanceled
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg, textAlign = TextAlign.Center, color = TextSecondary)
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick  = { vm.retry() },
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape    = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Quét lại")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick  = { navController.popBackStack() },
                        shape    = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Quay lại") }
                }
            }

            else -> {}
        }
    }
}