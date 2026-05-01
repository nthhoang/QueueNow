package com.example.queuenow.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.model.OwnerRequest
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.repository.AccountRepository
import com.example.queuenow.data.repository.OwnerRequestRepository
import com.example.queuenow.data.repository.PlaceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminStats(
    val totalUsers: Int = 0,
    val totalPlaces: Int = 0,
    val pendingRequests: Int = 0,
    val lockedAccounts: Int = 0
)

data class AdminState(
    val isLoading: Boolean = true,
    val stats: AdminStats = AdminStats(),
    val accounts: List<Account> = emptyList(),
    val places: List<Place> = emptyList(),
    val pendingRequests: List<OwnerRequest> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

class AdminViewModel : ViewModel() {
    private val accountRepo = AccountRepository()
    private val placeRepo   = PlaceRepository()
    private val reqRepo     = OwnerRequestRepository()

    private val _state = MutableStateFlow(AdminState())
    val state = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            combine(
                accountRepo.getAllAccounts(),
                placeRepo.getAllPlaces(),
                reqRepo.getAllPendingRequests()
            ) { accounts, places, reqs ->
                Triple(accounts, places, reqs)
            }.collectLatest { (accounts, places, reqs) ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        accounts = accounts,
                        places = places,
                        pendingRequests = reqs,
                        stats = AdminStats(
                            totalUsers = accounts.size,
                            totalPlaces = places.size,
                            pendingRequests = reqs.size,
                            lockedAccounts = accounts.count { a -> a.status == "LOCKED" }
                        )
                    )
                }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, message = null) }
}