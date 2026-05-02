package com.example.queuenow.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.ChatRoom
import com.example.queuenow.data.model.Message
import com.example.queuenow.data.repository.AuthRepository
import com.example.queuenow.data.repository.ChatRepository
import com.example.queuenow.data.repository.PlaceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ChatUiState(
    val isLoading: Boolean = true,
    val chatRoom: ChatRoom? = null,
    val messages: List<Message> = emptyList(),
    val currentUserId: String = "",
    val isOwnerMode: Boolean = false,   // true = đang xem với tư cách owner
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val chatRoomId: String,
    private val targetPlaceId: String = "",     // chỉ dùng khi tạo mới từ phía user
    private val targetOwnerId: String = ""      // chỉ dùng khi tạo mới từ phía user
) : ViewModel() {

    companion object {
        fun factoryFromPlace(placeId: String, ownerId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(
                        chatRoomId     = "",            // sẽ tự tính
                        targetPlaceId  = placeId,
                        targetOwnerId  = ownerId
                    ) as T
            }

        fun factoryFromRoomId(chatRoomId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(chatRoomId = chatRoomId) as T
            }
    }

    private val chatRepo  = ChatRepository()
    private val placeRepo = PlaceRepository()
    private val authRepo  = AuthRepository()

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private var resolvedRoomId: String = chatRoomId
    private var messagesJob: Job? = null

    init { initialize() }

    private fun initialize() {
        viewModelScope.launch {
            try {
                val uid = waitForUid() ?: run {
                    _state.update { it.copy(isLoading = false, error = "Chưa đăng nhập") }
                    return@launch
                }
                
                val account = authRepo.getAccount(uid) ?: run {
                    _state.update { it.copy(isLoading = false, error = "Không tìm thấy tài khoản") }
                    return@launch
                }

                if (chatRoomId.isNotBlank()) {
                    resolvedRoomId = chatRoomId
                    val room = chatRepo.getChatRoom(chatRoomId)
                    val isOwner = room?.ownerId == uid
                    _state.update { it.copy(currentUserId = uid, isOwnerMode = isOwner, chatRoom = room) }
                    markAsRead(uid, isOwner)
                } else {
                    val place = placeRepo.getPlace(targetPlaceId)
                    if (place == null) {
                        _state.update { it.copy(isLoading = false, error = "Không tìm thấy địa điểm") }
                        return@launch
                    }
                    val ownerAccount = authRepo.getAccount(place.ownerId)
                    val room = chatRepo.getOrCreateChatRoom(
                        userId        = uid,
                        userName      = account.fullName,
                        ownerId       = place.ownerId,
                        ownerName     = ownerAccount?.fullName ?: "Chủ địa điểm",
                        placeId       = targetPlaceId,
                        placeName     = place.placeName,
                        placeImageUrl = place.imageUrl
                    )
                    resolvedRoomId = room.chatRoomId
                    _state.update {
                        it.copy(
                            currentUserId = uid,
                            isOwnerMode   = false,
                            chatRoom      = room
                        )
                    }
                    markAsRead(uid, false)
                }

                startListeningMessages()
            } catch (e: Exception) {
                Log.e("ChatVM", "Initialize error: ${e.message}")
                _state.update { it.copy(isLoading = false, error = "Lỗi kết nối: ${e.message}") }
            }
        }
    }

    private fun startListeningMessages() {
        if (resolvedRoomId.isBlank()) return
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepo.getMessages(resolvedRoomId).collect { msgs ->
                _state.update { it.copy(messages = msgs, isLoading = false) }
            }
        }
    }

    private fun markAsRead(uid: String, isOwner: Boolean) {
        viewModelScope.launch {
            try {
                if (isOwner) chatRepo.markReadByOwner(resolvedRoomId)
                else chatRepo.markReadByUser(resolvedRoomId)
            } catch (e: Exception) {
                Log.e("ChatVM", "markAsRead error: ${e.message}")
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val s = _state.value
        if (s.currentUserId.isBlank() || resolvedRoomId.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            try {
                val account = authRepo.getAccount(s.currentUserId)
                chatRepo.sendMessage(
                    chatRoomId = resolvedRoomId,
                    senderId   = s.currentUserId,
                    senderName = account?.fullName ?: "Ẩn danh",
                    content    = content,
                    isOwner    = s.isOwnerMode
                )
            } catch (e: Exception) {
                Log.e("ChatVM", "sendMessage: ${e.message}")
                _state.update { it.copy(error = "Gửi thất bại: ${e.message}") }
            } finally {
                _state.update { it.copy(isSending = false) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private suspend fun waitForUid(): String? {
        return withTimeoutOrNull(5_000L) {
            var uid: String? = null
            while (uid == null) {
                uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) delay(300L)
            }
            uid
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
    }
}
