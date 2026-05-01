package com.example.queuenow.ui.user.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.queuenow.data.model.RoleType
import com.example.queuenow.ui.components.GradientButton
import com.example.queuenow.ui.components.LoadingOverlay
import com.example.queuenow.ui.components.QueueTextField
import com.example.queuenow.ui.components.UserBottomNav
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    vm: ProfileViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    LaunchedEffect(state.isEditing) {
        if (state.isEditing) {
            editName = state.account?.fullName ?: ""
            editPhone = state.account?.phone ?: ""
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessages() }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.uploadAvatar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { UserBottomNav(navController) },
        containerColor = BackgroundLight
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        val account = state.account ?: return@Scaffold

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Header gradient ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                        )
                        .padding(top = 32.dp, bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Avatar
                        Box {
                            if (account.avatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = account.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, Color.White, CircleShape)
                                )
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .border(3.dp, Color.White, CircleShape),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.3f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            account.fullName.firstOrNull()?.uppercaseChar()
                                                ?.toString() ?: "?",
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            // Camera icon
                            Surface(
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.BottomEnd)
                                    .clickable { avatarPicker.launch("image/*") },
                                shape = CircleShape,
                                color = Primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (state.isUploadingAvatar) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.CameraAlt, null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            account.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        // Role badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                when (account.role) {
                                    RoleType.ADMIN.name -> "👑 Admin"
                                    RoleType.OWNER.name -> "🏪 Chủ địa điểm"
                                    else -> "👤 Người dùng"
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ── Cards section (offset lên header) ───────────────
                Column(
                    modifier = Modifier
                        .offset(y = (-24).dp)
                        .padding(horizontal = 16.dp)
                ) {
                    // Thông tin cá nhân
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Thông tin cá nhân",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!state.isEditing) {
                                    TextButton(onClick = { vm.setEditing(true) }) {
                                        Icon(
                                            Icons.Filled.Edit, null,
                                            tint = Primary, modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Sửa", color = Primary)
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))

                            if (state.isEditing) {
                                QueueTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = "Họ và tên",
                                    leadingIcon = { Icon(Icons.Filled.Person, null, tint = Primary) }
                                )
                                Spacer(Modifier.height(12.dp))
                                QueueTextField(
                                    value = editPhone,
                                    onValueChange = { editPhone = it },
                                    label = "Số điện thoại",
                                    leadingIcon = { Icon(Icons.Filled.Phone, null, tint = Primary) }
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { vm.setEditing(false) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Hủy") }
                                    Button(
                                        onClick = { vm.updateProfile(editName, editPhone) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !state.isSaving,
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (state.isSaving) "Đang lưu..." else "Lưu")
                                    }
                                }
                            } else {
                                ProfileInfoRow(Icons.Filled.Email, "Email", account.email)
                                Spacer(Modifier.height(8.dp))
                                ProfileInfoRow(Icons.Filled.Phone, "Điện thoại",
                                    account.phone.ifEmpty { "Chưa cập nhật" })
                                Spacer(Modifier.height(8.dp))
                                ProfileInfoRow(Icons.Filled.Badge, "Trạng thái", account.status)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Chức năng theo role
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            when (account.role) {
                                RoleType.USER.name -> {
                                    ProfileActionItem(
                                        icon = Icons.Filled.Store,
                                        title = "Trở thành chủ địa điểm",
                                        subtitle = "Đăng ký quản lý địa điểm của bạn",
                                        iconColor = Accent
                                    ) {
                                        navController.navigate(Screen.OwnerRequestScreen.route)
                                    }
                                }
                                RoleType.OWNER.name -> {
                                    ProfileActionItem(
                                        icon = Icons.Filled.Dashboard,
                                        title = "Bảng điều khiển",
                                        subtitle = "Quản lý địa điểm của bạn",
                                        iconColor = Primary
                                    ) {
                                        navController.navigate(Screen.OwnerDashboard.route)
                                    }
                                }
                                RoleType.ADMIN.name -> {
                                    ProfileActionItem(
                                        icon = Icons.Filled.AdminPanelSettings,
                                        title = "Quản trị hệ thống",
                                        subtitle = "Trang quản trị Admin",
                                        iconColor = Secondary
                                    ) {
                                        navController.navigate(Screen.AdminDashboard.route)
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ProfileActionItem(
                                icon = Icons.Filled.History,
                                title = "Lịch sử vé",
                                subtitle = "Xem tất cả vé đã sử dụng",
                                iconColor = StatusWaiting
                            ) {
                                navController.navigate(Screen.TicketHistory.route)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Đăng xuất
                    OutlinedButton(
                        onClick = {
                            vm.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCanceled),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusCanceled)
                    ) {
                        Icon(Icons.Filled.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Đăng xuất", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (state.isUploadingAvatar) LoadingOverlay()
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = PrimaryLight,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProfileActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconColor.copy(alpha = 0.12f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary)
    }
}