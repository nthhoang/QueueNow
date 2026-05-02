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
    val currentUserId: String = ""
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

    private val _state = MutableStateFlow(ChatListUiState(isOwnerMode = isOwnerMode))
    val state = _state.asStateFlow()

    private var job: Job? = null

    init { load() }

    private fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            // Chờ Auth
            var uid: String? = null
            withTimeoutOrNull(5_000L) {
                while (uid == null) {
                    uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) delay(300L)
                }
            }
            val userId = uid ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            _state.update { it.copy(currentUserId = userId) }

            val flow = if (isOwnerMode)
                chatRepo.getChatRoomsForOwner(userId)
            else
                chatRepo.getChatRoomsForUser(userId)

            flow.collect { rooms ->
                _state.update { it.copy(chatRooms = rooms, isLoading = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}