package com.example.queuenow.ui.user.profile

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.model.AppNotification
import com.example.queuenow.data.model.NotificationType
import com.example.queuenow.data.model.OwnerRequest
import com.example.queuenow.data.model.RequestStatus
import com.example.queuenow.data.model.RoleType
import com.example.queuenow.data.repository.AccountRepository
import com.example.queuenow.data.repository.AuthRepository
import com.example.queuenow.data.repository.NotificationRepository
import com.example.queuenow.data.repository.OwnerRequestRepository
import com.example.queuenow.ui.components.GradientButton
import com.example.queuenow.ui.components.LoadingOverlay
import com.example.queuenow.ui.components.QueueTextField
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── ViewModel ────────────────────────────────────────────────────────────────
data class OwnerRequestState(
    val isLoading: Boolean = true,
    val existingRequest: OwnerRequest? = null,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class OwnerRequestViewModel : ViewModel() {
    private val repo     = OwnerRequestRepository()
    private val authRepo = AuthRepository()

    private val _state = MutableStateFlow(OwnerRequestState())
    val state = _state.asStateFlow()

    init { checkExisting() }

    private fun checkExisting() {
        viewModelScope.launch {
            val uid = authRepo.getCurrentUserId() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val req = repo.getRequestByUser(uid)
            _state.update { it.copy(existingRequest = req, isLoading = false) }
        }
    }

    fun submitRequest(
        businessName: String,
        businessType: String,
        address: String,
        description: String
    ) {
        viewModelScope.launch {
            if (businessName.isBlank() || address.isBlank()) {
                _state.update { it.copy(error = "Tên doanh nghiệp và địa chỉ không được để trống") }
                return@launch
            }
            val uid = authRepo.getCurrentUserId() ?: return@launch
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                val req = OwnerRequest(
                    accountId    = uid,
                    businessName = businessName.trim(),
                    businessType = businessType.trim(),
                    address      = address.trim(),
                    description  = description.trim()
                )
                repo.submitRequest(req)

                // Gửi notification cho tất cả Admin
                try {
                    val admins = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("accounts")
                        .whereEqualTo("role", RoleType.ADMIN.name)
                        .get().await()
                        .toObjects(Account::class.java)
                    admins.forEach { admin ->
                        NotificationRepository().sendNotification(
                            AppNotification(
                                userId = admin.accountId,
                                type = NotificationType.NEW_OWNER_REQUEST.name,
                                title = "Đơn đăng ký Owner mới",
                                message = "\"${businessName.trim()}\" đã gửi đơn xin làm chủ địa điểm."
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("OwnerRequestVM", "Notify admin failed: ${e.message}")
                }

                checkExisting()
                _state.update { it.copy(isSubmitting = false, isSubmitted = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Lỗi gửi đơn", isSubmitting = false) }
            }
        }
    }

    fun cancelRequest() {
        viewModelScope.launch {
            val reqId = _state.value.existingRequest?.requestId ?: return@launch
            try {
                repo.cancelRequest(reqId)
                _state.update { it.copy(existingRequest = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, message = null) }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerRequestScreen(
    navController: NavController,
    vm: OwnerRequestViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var address      by remember { mutableStateOf("") }
    var description  by remember { mutableStateOf("") }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Đăng ký Chủ địa điểm") },
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
                    .padding(20.dp)
            ) {
                // ── Đã có request pending ────────────────────────────
                val existing = state.existingRequest
                if (existing != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (existing.status) {
                                RequestStatus.APPROVED.name -> StatusCompleted.copy(alpha = 0.08f)
                                RequestStatus.REJECTED.name -> StatusCanceled.copy(alpha = 0.08f)
                                else -> StatusWaiting.copy(alpha = 0.08f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Đơn đăng ký",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusChip(existing.status)
                            }
                            Spacer(Modifier.height(16.dp))
                            InfoRow("Tên doanh nghiệp", existing.businessName)
                            InfoRow("Loại hình", existing.businessType)
                            InfoRow("Địa chỉ", existing.address)
                            InfoRow("Ngày nộp", existing.submittedAt.toFormattedDate())
                            if (existing.rejectionReason.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = StatusCanceled.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        "Lý do từ chối: ${existing.rejectionReason}",
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusCanceled
                                    )
                                }
                            }
                            if (existing.status == RequestStatus.PENDING.name) {
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { vm.cancelRequest() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCanceled),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusCanceled)
                                ) {
                                    Text("Hủy đơn")
                                }
                            }
                            if (existing.status == RequestStatus.APPROVED.name) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "🎉 Đơn đã được duyệt! Tài khoản của bạn đã được nâng cấp.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusCompleted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    return@Column
                }

                // ── Form nộp đơn mới ─────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Icon(
                            Icons.Filled.Store, null,
                            tint = Primary, modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Đăng ký Chủ địa điểm",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Điền thông tin để Admin xét duyệt",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(20.dp))

                        QueueTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = "Tên doanh nghiệp *",
                            leadingIcon = { Icon(Icons.Filled.Business, null, tint = Primary) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                        )
                        Spacer(Modifier.height(12.dp))
                        QueueTextField(
                            value = businessType,
                            onValueChange = { businessType = it },
                            label = "Loại hình kinh doanh (VD: Nhà hàng, Ngân hàng...)",
                            leadingIcon = { Icon(Icons.Filled.Category, null, tint = Primary) }
                        )
                        Spacer(Modifier.height(12.dp))
                        QueueTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = "Địa chỉ *",
                            leadingIcon = { Icon(Icons.Filled.LocationOn, null, tint = Primary) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                        Spacer(Modifier.height(12.dp))
                        QueueTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Mô tả thêm",
                            leadingIcon = { Icon(Icons.Filled.Description, null, tint = Primary) },
                            singleLine = false,
                            maxLines = 4
                        )
                        Spacer(Modifier.height(20.dp))
                        GradientButton(
                            text = if (state.isSubmitting) "Đang gửi..." else "Gửi đơn đăng ký",
                            onClick = {
                                vm.submitRequest(businessName, businessType, address, description)
                            },
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            if (state.isSubmitting) LoadingOverlay()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}