package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.OwnerRequest
import com.example.queuenow.data.model.RequestStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OwnerRequestRepository {
    private val db  = FirebaseFirestore.getInstance()
    private val col = db.collection("ownerRequests")

    suspend fun submitRequest(request: OwnerRequest): String {
        val ref = col.document()
        ref.set(request.copy(requestId = ref.id)).await()
        return ref.id
    }

    /**
     * Lấy đơn mới nhất của user — chỉ dùng 1 field để tránh cần composite index
     */
    suspend fun getRequestByUser(userId: String): OwnerRequest? =
        col.whereEqualTo("accountId", userId)
            .get().await()
            .toObjects(OwnerRequest::class.java)
            .filter { it.status != RequestStatus.CANCELED.name }
            .maxByOrNull { it.submittedAt }  // sort in-memory

    /**
     * Admin: lấy tất cả đơn PENDING — KHÔNG dùng orderBy để tránh cần composite index
     */
    fun getAllPendingRequests(): Flow<List<OwnerRequest>> = callbackFlow {
        val listener = col
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("OwnerRequestRepo", "getAllPendingRequests error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap
                    ?.toObjects(OwnerRequest::class.java)
                    ?.sortedByDescending { it.submittedAt }  // sort in-memory
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateRequest(
        requestId: String,
        status: RequestStatus,
        reviewedBy: String = "",
        reason: String = ""
    ) {
        val updates = mutableMapOf<String, Any>(
            "status"     to status.name,
            "reviewedAt" to System.currentTimeMillis()
        )
        if (reviewedBy.isNotEmpty()) updates["reviewedBy"] = reviewedBy
        if (reason.isNotEmpty())     updates["rejectionReason"] = reason
        col.document(requestId).update(updates).await()
    }

    suspend fun cancelRequest(requestId: String) {
        col.document(requestId)
            .update("status", RequestStatus.CANCELED.name).await()
    }
}