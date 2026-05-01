package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.Payment
import com.example.queuenow.data.model.PaymentStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PaymentRepository {
    private val db  = FirebaseFirestore.getInstance()
    private val col = db.collection("payments")

    suspend fun createPayment(payment: Payment): String {
        val ref = col.document()
        ref.set(payment.copy(paymentId = ref.id)).await()
        return ref.id
    }

    suspend fun getPaymentByTicket(ticketId: String): Payment? =
        col.whereEqualTo("ticketId", ticketId)
            .get().await()
            .toObjects(Payment::class.java)
            .firstOrNull()

    /**
     * Owner: lấy payments SUBMITTED của địa điểm — filter in-memory tránh composite index
     */
    fun getSubmittedPaymentsByPlace(placeId: String): Flow<List<Payment>> = callbackFlow {
        val listener = col
            .whereEqualTo("placeId", placeId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("PaymentRepo", "getSubmittedPaymentsByPlace error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap
                    ?.toObjects(Payment::class.java)
                    ?.filter { it.status == PaymentStatus.SUBMITTED.name }
                    ?.sortedByDescending { it.submittedAt }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getAllPaymentsByPlace(placeId: String): Flow<List<Payment>> = callbackFlow {
        val listener = col
            .whereEqualTo("placeId", placeId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("PaymentRepo", "getAllPaymentsByPlace error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(
                    snap?.toObjects(Payment::class.java)
                        ?.sortedByDescending { it.submittedAt }
                        ?: emptyList()
                )
            }
        awaitClose { listener.remove() }
    }

    suspend fun updatePaymentStatus(
        paymentId: String,
        status: PaymentStatus,
        confirmedBy: String = "",
        transactionCode: String = ""
    ) {
        val updates = mutableMapOf<String, Any>("status" to status.name)
        if (confirmedBy.isNotEmpty()) {
            updates["confirmedBy"] = confirmedBy
            updates["confirmedAt"] = System.currentTimeMillis()
        }
        if (transactionCode.isNotEmpty()) updates["transactionCode"] = transactionCode
        col.document(paymentId).update(updates).await()
    }

    suspend fun submitPayment(paymentId: String, proofUrl: String) {
        col.document(paymentId).update(
            mapOf(
                "status"       to PaymentStatus.SUBMITTED.name,
                "proofImageUrl" to proofUrl,
                "submittedAt"  to System.currentTimeMillis()
            )
        ).await()
    }
}