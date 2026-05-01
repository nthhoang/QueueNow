package com.example.queuenow.ui.owner.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.model.PlaceStatus
import com.example.queuenow.data.repository.AuthRepository
import com.example.queuenow.data.repository.PlaceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OwnerDashboardState(
    val isLoading: Boolean = true,
    val account: Account? = null,
    val places: List<Place> = emptyList(),
    val currentOwnerId: String = ""
)

class OwnerDashboardViewModel : ViewModel() {
    private val authRepo  = AuthRepository()
    private val placeRepo = PlaceRepository()

    private val _state = MutableStateFlow(OwnerDashboardState())
    val state = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // Chờ Firebase Auth session
            var uid: String? = null
            repeat(8) {
                uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) return@repeat
                delay(500L)
            }

            if (uid == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            val account = authRepo.getAccount(uid!!)
            _state.update { it.copy(account = account, currentOwnerId = uid!!) }

            // Lắng nghe realtime, filter chặt theo ownerId = uid hiện tại
            placeRepo.getPlacesByOwner(uid!!).collectLatest { places ->
                // Double-check filter: chỉ giữ place.ownerId == uid
                val myPlaces = places.filter { it.ownerId == uid }
                _state.update {
                    it.copy(places = myPlaces, isLoading = false)
                }
            }
        }
    }
}