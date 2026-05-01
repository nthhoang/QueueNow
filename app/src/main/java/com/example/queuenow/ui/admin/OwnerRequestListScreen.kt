package com.example.queuenow.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
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
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class OwnerRequestListState(
    val isLoading: Boolean = true,
    val requests: List<OwnerRequest> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class OwnerRequestListViewModel : ViewModel() {
    private val reqRepo     = OwnerRequestRepository()
    private val accountRepo = AccountRepository()
    private val authRepo    = AuthRepository()
    private val notifRepo = NotificationRepository()

    private val _state = MutableStateFlow(OwnerRequestListState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            reqRepo.getAllPendingRequests().collectLatest { requests ->
                _state.update { it.copy(requests = requests, isLoading = false) }
            }
        }
    }

    fun approveRequest(request: OwnerRequest) {
        viewModelScope.launch {
            val adminUid = authRepo.getCurrentUserId() ?: return@launch
            try {
                reqRepo.updateRequest(request.requestId, RequestStatus.APPROVED, reviewedBy = adminUid)
                accountRepo.updateRole(request.accountId, RoleType.OWNER)
                // Notify user
                notifRepo.sendNotification(
                    AppNotification(
                        userId = request.accountId,
                        type = NotificationType.OWNER_REQUEST_APPROVED.name,
                        title = "🎉 Đơn đăng ký được duyệt!",
                        message = "Chúc mừng! \"${request.businessName}\" đã được phê duyệt. Bạn giờ là chủ địa điểm."
                    )
                )
                _state.update { it.copy(message = "✓ Đã duyệt \"${request.businessName}\"") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun rejectRequest(request: OwnerRequest, reason: String) {
        viewModelScope.launch {
            val adminUid = authRepo.getCurrentUserId() ?: return@launch
            try {
                reqRepo.updateRequest(request.requestId, RequestStatus.REJECTED,
                    reviewedBy = adminUid, reason = reason)
                // Notify user
                notifRepo.sendNotification(
                    AppNotification(
                        userId  = request.accountId,
                        type    = NotificationType.OWNER_REQUEST_REJECTED.name,
                        title   = "Đơn đăng ký không được chấp nhận",
                        message = buildString {
                            append("Đơn \"${request.businessName}\" đã bị từ chối.")
                            if (reason.isNotBlank()) append(" Lý do: $reason")
                        }
                    )
                )
                _state.update { it.copy(message = "Đã từ chối đơn của \"${request.businessName}\"") }
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
fun OwnerRequestListScreen(
    navController: NavController,
    vm: OwnerRequestListViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Reject dialog state
    var rejectTarget by remember { mutableStateOf<OwnerRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    // Approve dialog state
    var approveTarget by remember { mutableStateOf<OwnerRequest?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Duyệt đơn Owner", fontWeight = FontWeight.Bold)
                        Text(
                            "${state.requests.size} đơn chờ duyệt",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
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
        containerColor = BackgroundLight
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Primary) }
            return@Scaffold
        }

        if (state.requests.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = StatusCompleted,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Không có đơn nào chờ duyệt",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.requests, key = { it.requestId }) { request ->
                OwnerRequestCard(
                    request = request,
                    onApprove = { approveTarget = request },
                    onReject  = { rejectTarget = request; rejectReason = "" }
                )
            }
        }
    }

    // ── Approve dialog ───────────────────────────────────────────────────────
    approveTarget?.let { req ->
        AlertDialog(
            onDismissRequest = { approveTarget = null },
            icon = {
                Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = StatusCompleted
                )
            },
            title = { Text("Duyệt đơn?") },
            text = {
                Text(
                    "Duyệt đơn của \"${req.businessName}\"?\n" +
                            "Tài khoản người dùng sẽ được nâng cấp lên quyền Owner."
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.approveRequest(req); approveTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted)
                ) { Text("Duyệt") }
            },
            dismissButton = {
                TextButton(onClick = { approveTarget = null }) { Text("Hủy") }
            }
        )
    }

    // ── Reject dialog ────────────────────────────────────────────────────────
    rejectTarget?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            icon = {
                Icon(Icons.Filled.Cancel, null, tint = StatusCanceled)
            },
            title = { Text("Từ chối đơn") },
            text = {
                Column {
                    Text("Từ chối đơn của \"${req.businessName}\"?")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Lý do từ chối (không bắt buộc)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StatusCanceled,
                            focusedLabelColor = StatusCanceled
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.rejectRequest(req, rejectReason)
                        rejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCanceled)
                ) { Text("Từ chối") }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun OwnerRequestCard(
    request: OwnerRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.businessName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    if (request.businessType.isNotEmpty()) {
                        Text(
                            request.businessType,
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary
                        )
                    }
                }
                StatusChip(request.status)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Divider)
            Spacer(Modifier.height(10.dp))

            // Info rows
            RequestInfoRow(Icons.Filled.LocationOn, "Địa chỉ", request.address)
            if (request.description.isNotEmpty()) {
                RequestInfoRow(
                    Icons.Filled.Description, "Mô tả", request.description
                )
            }
            RequestInfoRow(
                Icons.Filled.Schedule,
                "Nộp lúc",
                request.submittedAt.toFormattedDate()
            )
            RequestInfoRow(
                Icons.Filled.Person,
                "Mã tài khoản",
                request.accountId.take(12) + "…"
            )

            Spacer(Modifier.height(14.dp))

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StatusCanceled
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, StatusCanceled
                    )
                ) {
                    Icon(
                        Icons.Filled.Close, null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Từ chối", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusCompleted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Check, null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Duyệt", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RequestInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon, null,
            tint = Primary,
            modifier = Modifier
                .size(15.dp)
                .padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = OnBackground,
            fontWeight = FontWeight.Medium
        )
    }
}