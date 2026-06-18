package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReviewRepository {
    private val db  = FirebaseFirestore.getInstance()
    private val col = db.collection("reviews")

    fun getAllReviews(): Flow<List<Review>> = callbackFlow {
        val listener = col.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("ReviewRepo", "getAllReviews: ${error.message}")
                trySend(emptyList()); return@addSnapshotListener
            }
            trySend(snap?.toObjects(Review::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    fun getReviewsByPlace(placeId: String): Flow<List<Review>> = callbackFlow {
        val listener = col
            .whereEqualTo("placeId", placeId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("ReviewRepo", "getReviewsByPlace: ${error.message}")
                    trySend(emptyList()); return@addSnapshotListener
                }
                val list = snap?.toObjects(Review::class.java)
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getReviewByTicket(ticketId: String): Review? =
        col.whereEqualTo("ticketId", ticketId).get().await()
            .toObjects(Review::class.java).firstOrNull()

    suspend fun createReview(review: Review): String {
        val ref = col.document()
        ref.set(review.copy(reviewId = ref.id)).await()
        return ref.id
    }

    suspend fun replyReview(reviewId: String, reply: String) {
        col.document(reviewId).update(
            mapOf("reply" to reply, "replyAt" to System.currentTimeMillis())
        ).await()
    }

    suspend fun deleteReview(reviewId: String) {
        col.document(reviewId).delete().await()
    }

    /** Tính rating trung bình và tổng số đánh giá, trả về Pair(avg, count) */
    suspend fun getAverageRatingAndCount(placeId: String): Pair<Double, Int> {
        val snap    = col.whereEqualTo("placeId", placeId).get().await()
        val reviews = snap.toObjects(Review::class.java)
        if (reviews.isEmpty()) return Pair(0.0, 0)
        return Pair(reviews.map { it.rating }.average(), reviews.size)
    }
}