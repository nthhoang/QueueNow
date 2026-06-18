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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.TicketStatus
import com.example.queuenow.ui.components.QueueTicketCard
import com.example.queuenow.ui.components.UserBottomNav
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*

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
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Quay lại")
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
                                fontWeight = if (selectedFilter == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = StatusCanceled, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(error ?: "Đã có lỗi xảy ra", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { vm.retry() }) { Text("Thử lại") }
                    }
                }
                filteredTickets.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.ConfirmationNumber, null, tint = Divider, modifier = Modifier.size(80.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(if (selectedFilter == 0) "Chưa có vé nào" else "Không có vé trong mục này", color = TextSecondary)
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
                                    navController.navigate(Screen.QueueStatus.createRoute(ticket.ticketId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
