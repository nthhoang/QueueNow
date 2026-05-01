package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.AppNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun col(userId: String) =
        db.collection("notifications").document(userId).collection("items")

    suspend fun sendNotification(notification: AppNotification) {
        if (notification.userId.isBlank()) return
        try {
            val ref = col(notification.userId).document()
            val finalNotif = notification.copy(
                notificationId = ref.id,
                isRead = false,
                createdAt = System.currentTimeMillis()
            )
            ref.set(finalNotif).await()
        } catch (e: Exception) {
            Log.e("NotifRepo", "sendNotification failed: ${e.message}")
        }
    }

    /** Lấy thông báo realtime */
    fun getNotifications(userId: String): Flow<List<AppNotification>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = col(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("NotifRepo", "getNotifications error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(AppNotification::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** 
     * Đếm số thông báo chưa đọc realtime.
     * Sử dụng logic an toàn: đếm tất cả các doc mà isRead != true.
     */
    fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        if (userId.isBlank()) {
            trySend(0)
            return@callbackFlow
        }
        val listener = col(userId).addSnapshotListener { snap, error ->
            if (error != null) {
                trySend(0)
                return@addSnapshotListener
            }
            val count = snap?.documents?.count { doc ->
                doc.getBoolean("isRead") != true
            } ?: 0
            trySend(count)
        }
        awaitClose { listener.remove() }
    }

    /** 
     * Lấy danh sách ID các thông báo chưa đọc tại thời điểm gọi.
     * Dùng để đánh dấu "thông báo mới" trên UI.
     */
    suspend fun getInitialUnreadIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()
        return try {
            val snap = col(userId).get().await()
            snap.documents
                .filter { it.getBoolean("isRead") != true }
                .map { it.id }
                .toSet()
        } catch (e: Exception) {
            Log.e("NotifRepo", "getInitialUnreadIds error: ${e.message}")
            emptySet()
        }
    }

    suspend fun markAsRead(userId: String, notificationId: String) {
        if (userId.isBlank() || notificationId.isBlank()) return
        try {
            col(userId).document(notificationId).update("isRead", true).await()
        } catch (e: Exception) {
            Log.e("NotifRepo", "markAsRead failed: ${e.message}")
        }
    }

    /** Đánh dấu tất cả thông báo hiện có là đã đọc */
    suspend fun markAllAsRead(userId: String) {
        if (userId.isBlank()) return
        try {
            val snap = col(userId).get().await()
            val unreadDocs = snap.documents.filter { it.getBoolean("isRead") != true }
            if (unreadDocs.isEmpty()) return
            
            val batch = db.batch()
            unreadDocs.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            Log.d("NotifRepo", "Marked ${unreadDocs.size} notifications as read for $userId")
        } catch (e: Exception) {
            Log.e("NotifRepo", "markAllAsRead error: ${e.message}")
        }
    }
}