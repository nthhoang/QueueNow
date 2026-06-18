package com.example.queuenow.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val account: Account? = null,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val message: String? = null
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

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(error = "Vui lòng nhập email") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, message = null) }
            repo.forgotPassword(email.trim()).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, message = "Link đặt lại mật khẩu đã được gửi về email của bạn") }
                },
                onFailure = {
                    _state.update { it.copy(isLoading = false, error = it.message ?: "Có lỗi xảy ra") }
                }
            )
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }
    fun clearMessage() { _state.update { it.copy(message = null) } }
}