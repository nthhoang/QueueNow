package com.example.queuenow.ui.user.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.repository.AccountRepository
import com.example.queuenow.data.repository.AuthRepository
import com.example.queuenow.data.service.CloudinaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val isLoading: Boolean = true,
    val account: Account? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class ProfileViewModel : ViewModel() {
    private val authRepo    = AuthRepository()
    private val accountRepo = AccountRepository()

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val uid = authRepo.getCurrentUserId() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val account = authRepo.getAccount(uid)
            _state.update { it.copy(account = account, isLoading = false) }
        }
    }

    fun updateProfile(fullName: String, phone: String) {
        viewModelScope.launch {
            val account = _state.value.account ?: return@launch
            _state.update { it.copy(isSaving = true) }
            try {
                val updated = account.copy(fullName = fullName.trim(), phone = phone.trim())
                accountRepo.updateAccount(updated)
                _state.update { it.copy(account = updated, isSaving = false, isEditing = false, message = "Đã cập nhật hồ sơ") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Lỗi cập nhật", isSaving = false) }
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            val uid = authRepo.getCurrentUserId() ?: return@launch
            _state.update { it.copy(isUploadingAvatar = true) }
            try {
                val url = CloudinaryService.uploadImage(uri, "avatars")
                accountRepo.updateAvatar(uid, url)
                _state.update { s ->
                    s.copy(
                        account = s.account?.copy(avatarUrl = url),
                        isUploadingAvatar = false,
                        message = "Đã cập nhật ảnh đại diện"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Upload thất bại: ${e.message}", isUploadingAvatar = false) }
            }
        }
    }

    fun setEditing(v: Boolean) = _state.update { it.copy(isEditing = v) }
    fun logout() = authRepo.logout()
    fun clearMessages() = _state.update { it.copy(error = null, message = null) }
}