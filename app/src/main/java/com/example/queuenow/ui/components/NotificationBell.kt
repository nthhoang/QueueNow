package com.example.queuenow.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.queuenow.data.repository.NotificationRepository
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.StatusCanceled
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun NotificationBell(
    navController: NavController,
    iconColor: Color = Color.White
) {
    val repo = remember { NotificationRepository() }
    
    var count by remember { mutableIntStateOf(0) }
    var userId by remember { mutableStateOf<String?>(null) }

    // 1. Lấy userId với cơ chế retry
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        var currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            userId = currentUid
        } else {
            withTimeoutOrNull(5000L) {
                while (userId == null) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        userId = uid
                        break
                    }
                    delay(300L)
                }
            }
        }
    }

    // 2. Theo dõi số lượng thông báo chưa đọc realtime
    LaunchedEffect(userId) {
        val uid = userId ?: return@LaunchedEffect
        repo.getUnreadCount(uid)
            .catch { emit(0) }
            .collect { newCount ->
                count = newCount
            }
    }

    // 3. Hiển thị Badge (Số lượng thông báo chưa đọc)
    BadgedBox(
        badge = {
            if (count > 0) {
                Badge(
                    containerColor = StatusCanceled,
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (count > 99) "99+" else "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) {
        IconButton(
            onClick = {
                // CHỈ Chuyển hướng màn hình. 
                // VIỆC ĐÁNH DẤU ĐÃ ĐỌC SẼ ĐƯỢC XỬ LÝ TRONG NotificationViewModel
                // Để đảm bảo màn hình thông báo kịp nhận diện cái nào là "mới".
                navController.navigate(Screen.Notifications.route) {
                    launchSingleTop = true
                }
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = iconColor
            )
        }
    }
}