package com.example.queuenow.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.model.AccountStatus
import com.example.queuenow.data.model.AppNotification
import com.example.queuenow.data.model.NotificationType
import com.example.queuenow.data.model.RoleType
import com.example.queuenow.data.repository.AccountRepository
import com.example.queuenow.data.repository.NotificationRepository
import com.example.queuenow.ui.components.StatusChip
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class ManageAccountsState(
    val isLoading: Boolean = true,
    val accounts: List<Account> = emptyList(),
    val filteredAccounts: List<Account> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "ALL",
    val error: String? = null,
    val message: String? = null
)

class ManageAccountsViewModel : ViewModel() {
    private val repo = AccountRepository()

    private val _state = MutableStateFlow(ManageAccountsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllAccounts().collectLatest { accounts ->
                _state.update { s ->
                    s.copy(
                        accounts = accounts,
                        filteredAccounts = applyFilter(accounts, s.searchQuery, s.selectedFilter),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun search(query: String) {
        _state.update { s ->
            s.copy(
                searchQuery = query,
                filteredAccounts = applyFilter(s.accounts, query, s.selectedFilter)
            )
        }
    }

    fun setFilter(filter: String) {
        _state.update { s ->
            s.copy(
                selectedFilter = filter,
                filteredAccounts = applyFilter(s.accounts, s.searchQuery, filter)
            )
        }
    }

    private fun applyFilter(
        accounts: List<Account>,
        query: String,
        filter: String
    ): List<Account> {
        var result = accounts
        if (query.isNotBlank()) {
            result = result.filter {
                it.fullName.contains(query, true) ||
                        it.email.contains(query, true) ||
                        it.phone.contains(query, true)
            }
        }
        result = when (filter) {
            "USER"   -> result.filter { it.role == RoleType.USER.name }
            "OWNER"  -> result.filter { it.role == RoleType.OWNER.name }
            "ADMIN"  -> result.filter { it.role == RoleType.ADMIN.name }
            "LOCKED" -> result.filter { it.status == AccountStatus.LOCKED.name }
            else     -> result
        }
        return result
    }

    fun toggleLock(account: Account) {
        viewModelScope.launch {
            val newStatus = if (account.status == AccountStatus.LOCKED.name)
                AccountStatus.ACTIVE else AccountStatus.LOCKED
            try {
                repo.updateStatus(account.accountId, newStatus)
                // Gửi thông báo cho user (nếu đang bị khóa)
                if (newStatus == AccountStatus.LOCKED) {
                    NotificationRepository().sendNotification(
                        AppNotification(
                            userId = account.accountId,
                            type = NotificationType.PLACE_LOCKED_BY_ADMIN.name,
                            title = "Tài khoản bị khóa",
                            message = "Tài khoản của bạn đã bị Admin khóa. Vui lòng liên hệ hỗ trợ."
                        )
                    )
                }
                val msg = if (newStatus == AccountStatus.LOCKED)
                    "Đã khóa tài khoản ${account.fullName}"
                else "Đã mở khóa tài khoản ${account.fullName}"
                _state.update { it.copy(message = msg) }
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
fun ManageAccountsScreen(
    navController: NavController,
    vm: ManageAccountsViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var accountToToggle by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }

    val filterOptions = listOf(
        "ALL" to "Tất cả",
        "USER" to "User",
        "OWNER" to "Owner",
        "ADMIN" to "Admin",
        "LOCKED" to "Bị khóa"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Quản lý tài khoản", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Search bar ───────────────────────────────────────────
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Column {
                    TextField(
                        value = state.searchQuery,
                        onValueChange = { vm.search(it) },
                        placeholder = { Text("Tìm tên, email, SĐT...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Primary) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { vm.search("") }) {
                                    Icon(Icons.Filled.Clear, null, tint = TextSecondary)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    // Filter chips
                    ScrollableTabRow(
                        selectedTabIndex = filterOptions.indexOfFirst { it.first == state.selectedFilter }
                            .coerceAtLeast(0),
                        containerColor = Color.White,
                        contentColor = Primary,
                        edgePadding = 16.dp,
                        indicator = {}
                    ) {
                        filterOptions.forEach { (key, label) ->
                            val isSelected = state.selectedFilter == key
                            Tab(
                                selected = isSelected,
                                onClick = { vm.setFilter(key) },
                                text = {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) Primary else Divider,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            label,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 5.dp
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold
                                            else FontWeight.Normal
                                        )
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Count ─────────────────────────────────────────────────
            Text(
                "${state.filteredAccounts.size} tài khoản",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── List ──────────────────────────────────────────────────
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        state.filteredAccounts,
                        key = { it.accountId }
                    ) { account ->
                        AccountCard(
                            account = account,
                            onToggleLock = { accountToToggle = account }
                        )
                    }
                }
            }
        }
    }

    // ── Confirm dialog ───────────────────────────────────────────────────────
    accountToToggle?.let { acc ->
        val isLocking = acc.status != AccountStatus.LOCKED.name
        AlertDialog(
            onDismissRequest = { accountToToggle = null },
            icon = {
                Icon(
                    if (isLocking) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    null,
                    tint = if (isLocking) StatusCanceled else StatusCompleted
                )
            },
            title = { Text(if (isLocking) "Khóa tài khoản?" else "Mở khóa?") },
            text = {
                Text(
                    if (isLocking)
                        "Khóa tài khoản \"${acc.fullName}\"? Người dùng sẽ không thể đăng nhập."
                    else
                        "Mở khóa tài khoản \"${acc.fullName}\"?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.toggleLock(acc)
                        accountToToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocking) StatusCanceled else StatusCompleted
                    )
                ) { Text(if (isLocking) "Khóa" else "Mở khóa") }
            },
            dismissButton = {
                TextButton(onClick = { accountToToggle = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun AccountCard(
    account: Account,
    onToggleLock: () -> Unit
) {
    val isLocked = account.status == AccountStatus.LOCKED.name
    val roleColor = when (account.role) {
        RoleType.ADMIN.name -> Secondary
        RoleType.OWNER.name -> Accent
        else                -> Primary
    }
    val roleLabel = when (account.role) {
        RoleType.ADMIN.name -> "Admin"
        RoleType.OWNER.name -> "Owner"
        else                -> "User"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) StatusCanceled.copy(alpha = 0.04f)
            else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = roleColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        account.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = roleColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        account.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLocked) TextSecondary else OnBackground
                    )
                    Spacer(Modifier.width(6.dp))
                    // Role badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = roleColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            roleLabel,
                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 2.dp
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = roleColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    account.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (account.phone.isNotEmpty()) {
                    Text(
                        account.phone,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    "Ngày tạo: ${account.createdAt.toFormattedDate("dd/MM/yyyy")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Action
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(account.status)
                Spacer(Modifier.height(6.dp))
                if (account.role != RoleType.ADMIN.name) {
                    IconButton(
                        onClick = onToggleLock,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            null,
                            tint = if (isLocked) StatusCompleted else StatusCanceled,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}