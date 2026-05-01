package com.example.queuenow.ui.user.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.repository.AuthRepository
import com.example.queuenow.data.repository.PlaceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val places: List<Place> = emptyList(),
    val filteredPlaces: List<Place> = emptyList(),
    val searchQuery: String = "",
    val account: Account? = null,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val placeRepo = PlaceRepository()
    private val authRepo = AuthRepository()

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init {
        loadAccount()
        loadPlaces()
    }

    private fun loadAccount() {
        viewModelScope.launch {
            val uid = authRepo.getCurrentUserId() ?: return@launch
            _state.update { it.copy(account = authRepo.getAccount(uid)) }
        }
    }

    private fun loadPlaces() {
        viewModelScope.launch {
            placeRepo.getOpenPlaces().collectLatest { places ->
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        places = places,
                        filteredPlaces = filter(places, s.searchQuery)
                    )
                }
            }
        }
    }

    fun search(query: String) {
        _state.update { s ->
            s.copy(searchQuery = query, filteredPlaces = filter(s.places, query))
        }
    }

    private fun filter(places: List<Place>, q: String) =
        if (q.isBlank()) places
        else places.filter {
            it.placeName.contains(q, true) ||
                    it.address.contains(q, true) ||
                    it.description.contains(q, true)
        }
}