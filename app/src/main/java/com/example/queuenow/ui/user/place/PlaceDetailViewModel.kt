package com.example.queuenow.ui.user.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.model.Review
import com.example.queuenow.data.model.WaitingRoom
import com.example.queuenow.data.repository.PlaceRepository
import com.example.queuenow.data.repository.ReviewRepository
import com.example.queuenow.data.repository.WaitingRoomRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlaceDetailState(
    val isLoading: Boolean = true,
    val place: Place? = null,
    val rooms: List<WaitingRoom> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val error: String? = null
)

class PlaceDetailViewModel(private val placeId: String) : ViewModel() {

    companion object {
        fun factory(placeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PlaceDetailViewModel(placeId) as T
            }
    }

    private val placeRepo  = PlaceRepository()
    private val roomRepo   = WaitingRoomRepository()
    private val reviewRepo = ReviewRepository()

    private val _state = MutableStateFlow(PlaceDetailState())
    val state          = _state.asStateFlow()

    init {
        loadPlace()
        observeRooms()
        observeReviews()
    }

    private fun loadPlace() {
        viewModelScope.launch {
            val place = placeRepo.getPlace(placeId)
            _state.update { it.copy(place = place, isLoading = false) }
        }
    }

    private fun observeRooms() {
        viewModelScope.launch {
            roomRepo.getRooms(placeId).collectLatest { rooms ->
                _state.update { it.copy(rooms = rooms) }
            }
        }
    }

    private fun observeReviews() {
        viewModelScope.launch {
            // Không dùng orderBy → tránh cần composite index
            reviewRepo.getReviewsByPlace(placeId).collectLatest { reviews ->
                _state.update { it.copy(reviews = reviews) }
            }
        }
    }
}