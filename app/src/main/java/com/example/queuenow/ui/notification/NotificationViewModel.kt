package com.example.queuenow.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.AppNotification
import com.example.queuenow.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class NotificationUiState(
    val notifications: List<AppNotification> = emptyList(),
    /**
     * Set các notificationId chưa đọc TẠI THỜI ĐIỂM mở màn hình.
     * Dùng để hiển thị chấm đỏ đúng cách.
     */
    val initialUnreadIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

class NotificationViewModel : ViewModel() {
    private val repo = NotificationRepository()

    private val _state = MutableStateFlow(NotificationUiState())
    val state = _state.asStateFlow()

    private var collectJob: Job? = null

    init {
        startListening()
    }

    private fun startListening() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            // Lấy userId hiện tại
            val auth = FirebaseAuth.getInstance()
            var userId = auth.currentUser?.uid
            
            // Nếu chưa có (đang load auth), chờ tối đa 5s
            if (userId == null) {
                withTimeoutOrNull(5000L) {
                    while (userId == null) {
                        delay(200)
                        userId = auth.currentUser?.uid
                    }
                }
            }

            if (userId == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            val finalUserId = userId!!

            // 1. Lấy danh sách ID chưa đọc trước khi đánh dấu là đã đọc
            try {
                val unreadIds = repo.getInitialUnreadIds(finalUserId)
                _state.update { it.copy(initialUnreadIds = unreadIds) }
            } catch (e: Exception) {
                _state.update { it.copy(initialUnreadIds = emptySet()) }
            }

            // 2. Lắng nghe thông báo realtime
            repo.getNotifications(finalUserId).collect { list ->
                _state.update { it.copy(notifications = list, isLoading = false) }
            }
        }
    }

    /**
     * Đánh dấu tất cả đã đọc trên server sau một khoảng delay ngắn
     * để đảm bảo UI đã kịp lấy được initialUnreadIds và hiển thị chấm đỏ.
     */
    fun markAllAsReadDelayed() {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            delay(1000L) // Delay 1s để người dùng thấy chấm đỏ trước khi "đã đọc" âm thầm
            repo.markAllAsRead(userId)
            // Lưu ý: Không xóa initialUnreadIds trong state ở đây để chấm đỏ vẫn còn
            // cho đến khi người dùng thoát màn hình hoặc nhấn vào.
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            repo.markAsRead(userId, notificationId)
            // Xóa chấm đỏ của riêng thông báo này khi nhấn vào
            _state.update { s ->
                s.copy(initialUnreadIds = s.initialUnreadIds - notificationId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectJob?.cancel()
    }
}