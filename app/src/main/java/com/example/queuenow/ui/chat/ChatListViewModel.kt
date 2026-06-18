package com.example.queuenow.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.ChatRoom
import com.example.queuenow.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ChatListUiState(
    val isLoading: Boolean = true,
    val chatRooms: List<ChatRoom> = emptyList(),
    val isOwnerMode: Boolean = false,
    val currentUserId: String = "",
    val error: String? = null
)

class ChatListViewModel(private val isOwnerMode: Boolean) : ViewModel() {

    companion object {
        fun userFactory() = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatListViewModel(isOwnerMode = false) as T
        }

        fun ownerFactory() = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatListViewModel(isOwnerMode = true) as T
        }
    }

    private val chatRepo = ChatRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(ChatListUiState(isOwnerMode = isOwnerMode))
    val state = _state.asStateFlow()

    private var job: Job? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            // Lấy UID ngay lập tức nếu có, hoặc chờ session được khôi phục
            var uid = auth.currentUser?.uid
            
            if (uid == null) {
                // Chờ tối đa 5s để Firebase khôi phục session
                withTimeoutOrNull(5000L) {
                    while (uid == null) {
                        uid = auth.currentUser?.uid
                        if (uid == null) delay(500)
                    }
                }
            }

            if (uid != null) {
                _state.update { it.copy(currentUserId = uid!!, error = null) }
                loadChatRooms(uid!!)
            } else {
                _state.update { it.copy(isLoading = false, error = "Vui lòng đăng nhập lại") }
            }
        }
    }

    private fun loadChatRooms(userId: String) {
        job?.cancel()
        job = viewModelScope.launch {
            val flow = if (isOwnerMode)
                chatRepo.getChatRoomsForOwner(userId)
            else
                chatRepo.getChatRoomsForUser(userId)

            flow.catch { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }.collect { rooms ->
                _state.update { it.copy(chatRooms = rooms, isLoading = false) }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(isLoading = true, error = null) }
        observeAuthState()
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}
