package com.example.queuenow.ui.user.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.QueueTicket
import com.example.queuenow.data.repository.QueueTicketRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TicketHistoryViewModel : ViewModel() {
    private val ticketRepo = QueueTicketRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _tickets   = MutableStateFlow<List<QueueTicket>>(emptyList())
    val tickets    = _tickets.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading  = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error      = _error.asStateFlow()

    private var collectJob: Job? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            _isLoading.value = true
            // Chờ session Auth một cách chủ động hơn
            var uid = auth.currentUser?.uid
            if (uid == null) {
                withTimeoutOrNull(8000L) { // Tăng thời gian chờ lên 8s cho mạng yếu
                    while (uid == null) {
                        uid = auth.currentUser?.uid
                        if (uid == null) delay(500)
                    }
                }
            }

            if (uid != null) {
                loadTickets(uid!!)
            } else {
                _isLoading.value = false
                _error.value = "Không thể xác thực người dùng. Vui lòng kiểm tra kết nối mạng."
            }
        }
    }

    private fun loadTickets(uid: String) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            try {
                ticketRepo.getTicketsByUser(uid)
                    .onStart { _isLoading.value = true }
                    .catch { e ->
                        _isLoading.value = false
                        _error.value = "Lỗi tải dữ liệu: ${e.message}"
                    }
                    .collect { list ->
                        _tickets.value = list
                        _isLoading.value = false
                        _error.value = null
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _isLoading.value = false
                _error.value = "Có lỗi xảy ra: ${e.message}"
            }
        }
    }

    fun retry() {
        _error.value = null
        observeAuthState()
    }

    override fun onCleared() {
        super.onCleared()
        collectJob?.cancel()
    }
}
