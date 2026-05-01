package com.example.queuenow.ui.user.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.queuenow.data.model.QueueTicket
import com.example.queuenow.data.model.TicketStatus
import com.example.queuenow.data.repository.QueueTicketRepository
import com.example.queuenow.ui.components.QueueTicketCard
import com.example.queuenow.ui.components.UserBottomNav
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ── ViewModel ────────────────────────────────────────────────────────────────
class TicketHistoryViewModel : ViewModel() {
    private val ticketRepo = QueueTicketRepository()

    private val _tickets   = MutableStateFlow<List<QueueTicket>>(emptyList())
    val tickets    = _tickets.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading  = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error      = _error.asStateFlow()

    private var collectJob: Job? = null

    init { loadTickets() }

    fun loadTickets() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null

            // Chờ Firebase Auth restore session (tối đa 4 giây, poll mỗi 500ms)
            val uid = withTimeoutOrNull(4_000L) {
                var id: String? = null
                while (id == null) {
                    id = FirebaseAuth.getInstance().currentUser?.uid
                    if (id == null) delay(500L)
                }
                id
            }

            if (uid == null) {
                _isLoading.value = false
                _error.value = "Không tìm thấy thông tin đăng nhập. Vui lòng đăng xuất và đăng nhập lại."
                return@launch
            }

            try {
                ticketRepo.getTicketsByUser(uid).collectLatest { list ->
                    _tickets.value   = list
                    _isLoading.value = false
                    _error.value     = null
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _isLoading.value = false
                _error.value = "Lỗi tải dữ liệu: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectJob?.cancel()
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketHistoryScreen(
    navController: NavController,
    vm: TicketHistoryViewModel = viewModel()
) {
    val tickets   by vm.tickets.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()

    val prevRoute = navController.previousBackStackEntry?.destination?.route
    val showBack  = prevRoute == Screen.Profile.route

    val filterOptions = listOf("Tất cả", "Đang chờ", "Hoàn thành", "Đã hủy")
    var selectedFilter by remember { mutableIntStateOf(0) }

    val filteredTickets = remember(tickets, selectedFilter) {
        when (selectedFilter) {
            1 -> tickets.filter {
                it.status in listOf(
                    TicketStatus.PENDING_PAYMENT.name,
                    TicketStatus.WAITING.name,
                    TicketStatus.CALLED.name
                )
            }
            2 -> tickets.filter { it.status == TicketStatus.COMPLETED.name }
            3 -> tickets.filter {
                it.status in listOf(TicketStatus.CANCELED.name, TicketStatus.SKIPPED.name)
            }
            else -> tickets
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vé của tôi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Quay lại"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color.White,
                    titleContentColor = OnBackground
                )
            )
        },
        bottomBar      = { UserBottomNav(navController) },
        containerColor = BackgroundLight
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Filter tabs ──────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedFilter,
                containerColor   = Color.White,
                contentColor     = Primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilter]),
                        color    = Primary
                    )
                }
            ) {
                filterOptions.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedFilter == index,
                        onClick  = { selectedFilter = index },
                        text = {
                            Text(
                                label,
                                style      = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedFilter == index) FontWeight.Bold
                                else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // ── Content ──────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary)
                            Spacer(Modifier.height(12.dp))
                            Text("Đang tải vé...", color = TextSecondary)
                        }
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.ErrorOutline, null,
                            tint = StatusCanceled, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(error ?: "", color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { vm.loadTickets() },
                            colors  = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape   = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) { Text("Thử lại") }
                    }
                }
                filteredTickets.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.ConfirmationNumber, null,
                            tint = Divider, modifier = Modifier.size(80.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (selectedFilter == 0) "Chưa có vé nào"
                            else "Không có vé trong mục này",
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { navController.navigate(Screen.Home.route) }) {
                            Text("Khám phá địa điểm", color = Primary)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTickets, key = { it.ticketId }) { ticket ->
                            QueueTicketCard(
                                ticket  = ticket,
                                onClick = {
                                    navController.navigate(
                                        Screen.QueueStatus.createRoute(ticket.ticketId)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}