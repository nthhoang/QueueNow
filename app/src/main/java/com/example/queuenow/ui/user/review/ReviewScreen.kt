package com.example.queuenow.ui.user.review

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.AppNotification
import com.example.queuenow.data.model.NotificationType
import com.example.queuenow.data.model.Review
import com.example.queuenow.data.repository.AuthRepository
import com.example.queuenow.data.repository.PlaceRepository
import com.example.queuenow.data.repository.ReviewRepository
import com.example.queuenow.ui.components.GradientButton
import com.example.queuenow.ui.components.InteractiveRatingBar
import com.example.queuenow.ui.components.LoadingOverlay
import com.example.queuenow.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ViewModel ────────────────────────────────────────────────────────────────
data class ReviewState(
    val isCheckingExisting: Boolean = true,
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val alreadyReviewed: Boolean = false,
    val placeName: String = "",
    val error: String? = null
)

class ReviewViewModel(
    private val ticketId: String,
    private val placeId: String
) : ViewModel() {

    companion object {
        private const val TAG = "ReviewVM"

        fun factory(ticketId: String, placeId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReviewViewModel(ticketId, placeId) as T
            }
    }

    private val reviewRepo = ReviewRepository()
    private val authRepo   = AuthRepository()
    private val placeRepo  = PlaceRepository()

    private val _state = MutableStateFlow(ReviewState())
    val state          = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Lấy tên place
            try {
                val place = placeRepo.getPlace(placeId)
                _state.update { it.copy(placeName = place?.placeName ?: "") }
            } catch (_: Exception) {}

            // Kiểm tra đã review chưa
            try {
                val existing = reviewRepo.getReviewByTicket(ticketId)
                _state.update { it.copy(alreadyReviewed = existing != null, isCheckingExisting = false) }
            } catch (e: Exception) {
                Log.e(TAG, "checkExisting: ${e.message}")
                _state.update { it.copy(isCheckingExisting = false) }
            }
        }
    }

    fun submitReview(rating: Int, comment: String) {
        if (rating == 0) {
            _state.update { it.copy(error = "Vui lòng chọn số sao đánh giá") }
            return
        }
        viewModelScope.launch {
            val uid     = authRepo.getCurrentUserId() ?: return@launch
            val account = authRepo.getAccount(uid) ?: return@launch

            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 1. Tạo review
                val review = Review(
                    accountId   = uid,
                    accountName = account.fullName,
                    avatarUrl   = account.avatarUrl,
                    placeId     = placeId,
                    ticketId    = ticketId,
                    rating      = rating,
                    comment     = comment.trim()
                )
                reviewRepo.createReview(review)
                Log.d(TAG, "Review created OK")
                try {
                    val place = placeRepo.getPlace(placeId)
                    if (place != null && place.ownerId.isNotBlank()) {
                        com.example.queuenow.data.repository.NotificationRepository()
                            .sendNotification(
                                AppNotification(
                                    userId = place.ownerId,
                                    type = NotificationType.NEW_REVIEW.name,
                                    title = "⭐ Đánh giá mới",
                                    message = "${account.fullName} đã đánh giá $rating sao cho \"${place.placeName}\".",
                                    placeId = placeId
                                )
                            )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Notify owner review failed: ${e.message}")
                }

                // 2. Cập nhật ratingAverage + ratingCount — bắt lỗi riêng (non-critical)
                // Security rule cho phép update ratingAverage field
                try {
                    val (avg, count) = reviewRepo.getAverageRatingAndCount(placeId)
                    placeRepo.updateRatingWithCount(placeId, avg, count)
                    Log.d(TAG, "Rating updated: avg=$avg, count=$count")
                } catch (e: Exception) {
                    Log.w(TAG, "updateRatingWithCount failed (non-critical): ${e.message}")
                }

                _state.update { it.copy(isSubmitted = true, isLoading = false) }

            } catch (e: Exception) {
                Log.e(TAG, "submitReview: ${e.message}", e)
                val msg = when {
                    e.message?.contains("PERMISSION_DENIED") == true ->
                        "Lỗi quyền truy cập. Vui lòng đăng xuất và đăng nhập lại."
                    else -> "Lỗi gửi đánh giá: ${e.message}"
                }
                _state.update { it.copy(error = msg, isLoading = false) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}

// ── Screen ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    ticketId: String,
    placeId: String,
    vm: ReviewViewModel = viewModel(factory = ReviewViewModel.factory(ticketId, placeId))
) {
    val state  by vm.state.collectAsState()
    var rating  by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Đánh giá dịch vụ") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isCheckingExisting -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                state.isSubmitted || state.alreadyReviewed -> {
                    // Thành công / đã đánh giá
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = StatusCompleted.copy(0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.CheckCircle, null,
                                    tint = StatusCompleted, modifier = Modifier.size(72.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    if (state.alreadyReviewed && !state.isSubmitted)
                                        "Bạn đã đánh giá địa điểm này"
                                    else "Cảm ơn bạn đã đánh giá! ⭐",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusCompleted, textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Đánh giá của bạn giúp cộng đồng lựa chọn tốt hơn.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = { navController.popBackStack() },
                                    colors  = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                                    shape   = RoundedCornerShape(14.dp)
                                ) { Text("Quay lại") }
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.RateReview, null,
                            tint = Primary, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(10.dp))
                        if (state.placeName.isNotEmpty()) {
                            Text(state.placeName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = OnBackground,
                                textAlign = TextAlign.Center)
                        }
                        Text("Trải nghiệm của bạn thế nào?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary, textAlign = TextAlign.Center)

                        Spacer(Modifier.height(24.dp))

                        // Star rating card
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Đánh giá của bạn",
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(20.dp))
                                InteractiveRatingBar(rating = rating, onRatingChange = { rating = it }, starSize = 48.dp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    when (rating) {
                                        0 -> "Chọn số sao để đánh giá"
                                        1 -> "😞 Rất tệ"
                                        2 -> "😐 Tệ"
                                        3 -> "🙂 Bình thường"
                                        4 -> "😊 Tốt"
                                        else -> "🤩 Tuyệt vời!"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (rating == 0) TextSecondary else Color(0xFFF59E0B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Comment card
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Nhận xét (không bắt buộc)",
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = comment,
                                    onValueChange = { if (it.length <= 500) comment = it },
                                    placeholder = {
                                        Text("Chia sẻ trải nghiệm của bạn...", color = TextSecondary)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(130.dp),
                                    shape    = RoundedCornerShape(14.dp),
                                    maxLines = 6,
                                    colors   = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary, focusedLabelColor = Primary)
                                )
                                Text("${comment.length}/500",
                                    style = MaterialTheme.typography.labelSmall, color = TextSecondary,
                                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        GradientButton(
                            text    = if (state.isLoading) "Đang gửi..." else "Gửi đánh giá",
                            onClick = { vm.submitReview(rating, comment) },
                            enabled = !state.isLoading && rating > 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
            if (state.isLoading) LoadingOverlay()
        }
    }
}