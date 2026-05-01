package com.example.queuenow.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val account: Account? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel : ViewModel() {
    private val repo   = AuthRepository()
    private val _state = MutableStateFlow(AuthState())
    val state          = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState(error = "Vui lòng nhập đầy đủ email và mật khẩu")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            repo.login(email.trim(), password).fold(
                onSuccess = { _state.value = AuthState(account = it, isSuccess = true) },
                onFailure = { _state.value = AuthState(error = it.message ?: "Đăng nhập thất bại") }
            )
        }
    }

    fun register(email: String, password: String, fullName: String, phone: String) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = AuthState(error = "Vui lòng điền đầy đủ thông tin")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            repo.register(email.trim(), password, fullName.trim(), phone.trim()).fold(
                onSuccess = { _state.value = AuthState(account = it, isSuccess = true) },
                onFailure = { _state.value = AuthState(error = it.message ?: "Đăng ký thất bại") }
            )
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}