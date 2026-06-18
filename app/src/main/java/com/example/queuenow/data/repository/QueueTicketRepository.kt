package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.QueueTicket
import com.example.queuenow.data.model.TicketStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class QueueTicketRepository {
    private val db  = FirebaseFirestore.getInstance()
    private val col = db.collection("tickets")

    private val activeStatuses = setOf(
        TicketStatus.PENDING_PAYMENT.name,
        TicketStatus.WAITING.name,
        TicketStatus.CALLED.name,
        TicketStatus.SKIPPED.name
    )

    fun getAllTickets(): Flow<List<QueueTicket>> = callbackFlow {
        val listener = col.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("TicketRepo", "getAllTickets: ${error.message}")
                trySend(emptyList()); return@addSnapshotListener
            }
            trySend(snap?.toObjects(QueueTicket::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    suspend fun getUserActiveTicketInRoom(userId: String, roomId: String): QueueTicket? {
        val today = today()
        return col
            .whereEqualTo("accountId", userId)
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("queueDate", today)
            .get().await()
            .toObjects(QueueTicket::class.java)
            .firstOrNull { it.status in activeStatuses }
    }

    /**
     * LiveQueue owner: lấy tất cả vé hôm nay trong phòng
     * (trừ PENDING_PAYMENT vì chưa chính thức vào hàng đợi)
     */
    fun getTicketsByRoom(roomId: String): Flow<List<QueueTicket>> = callbackFlow {
        val today = today()
        val listener = col
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("queueDate", today)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TicketRepo", "getTicketsByRoom: ${error.message}")
                    trySend(emptyList()); return@addSnapshotListener
                }
                val list = snap?.toObjects(QueueTicket::class.java)
                    ?.filter { it.status != TicketStatus.PENDING_PAYMENT.name }
                    ?.sortedBy { it.ticketNumber }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getTicketsByUser(userId: String): Flow<List<QueueTicket>> = callbackFlow {
        val listener = col.whereEqualTo("accountId", userId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TicketRepo", "getTicketsByUser: ${error.message}")
                    trySend(emptyList()); return@addSnapshotListener
                }
                trySend(
                    snap?.toObjects(QueueTicket::class.java)
                        ?.sortedByDescending { it.issueTime }
                        ?: emptyList()
                )
            }
        awaitClose { listener.remove() }
    }

    fun getTicketLive(ticketId: String): Flow<QueueTicket?> = callbackFlow {
        val listener = col.document(ticketId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TicketRepo", "getTicketLive: ${error.message}")
                    return@addSnapshotListener
                }
                trySend(snap?.toObject(QueueTicket::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun createTicket(ticket: QueueTicket): String {
        val ref = col.document()
        ref.set(ticket.copy(ticketId = ref.id)).await()
        return ref.id
    }

    suspend fun updateStatus(ticketId: String, status: TicketStatus) {
        col.document(ticketId).update("status", status.name).await()
    }

    /**
     * Số thứ tự tiếp theo, tính từ sau lastResetTime.
     * Nếu lastResetTime > 0 → chỉ đếm vé được tạo SAU thời điểm reset.
     */
    suspend fun getNextTicketNumber(
        roomId: String,
        date: String,
        lastResetTime: Long = 0L
    ): Int {
        val snap = col
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("queueDate", date)
            .get().await()
        val all   = snap.toObjects(QueueTicket::class.java)
        val count = if (lastResetTime > 0L) {
            // Chỉ đếm vé tạo AFTER thời điểm reset
            all.count { it.issueTime > lastResetTime }
        } else {
            all.size
        }
        return count + 1
    }

    /** Đếm số vé WAITING hiện tại trong phòng hôm nay */
    suspend fun getWaitingCountInRoom(roomId: String, lastResetTime: Long = 0L): Int {
        val today = today()
        val snap = col
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("queueDate", today)
            .whereEqualTo("status", TicketStatus.WAITING.name)
            .get().await()
        val all = snap.toObjects(QueueTicket::class.java)
        return if (lastResetTime > 0L) {
            all.count { it.issueTime > lastResetTime }
        } else {
            all.size
        }
    }

    suspend fun hasActiveTickets(roomId: String): Boolean {
        val today = today()
        return col
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("queueDate", today)
            .get().await()
            .toObjects(QueueTicket::class.java)
            .any { it.status in activeStatuses }
    }

    suspend fun getNextWaiting(roomId: String): QueueTicket? {
        return col
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("queueDate", today())
            .whereEqualTo("status", TicketStatus.WAITING.name)
            .get().await()
            .toObjects(QueueTicket::class.java)
            .minByOrNull { it.ticketNumber }
    }

    suspend fun getCalledTicket(roomId: String): QueueTicket? {
        return col
            .whereEqualTo("roomId", roomId)
            .whereEqualTo("queueDate", today())
            .whereEqualTo("status", TicketStatus.CALLED.name)
            .get().await()
            .toObjects(QueueTicket::class.java)
            .firstOrNull()
    }

    suspend fun getTicketsByPlaceAndDateRange(
        placeId: String,
        startDate: String,
        endDate: String
    ): List<QueueTicket> {
        return col.whereEqualTo("placeId", placeId)
            .get().await()
            .toObjects(QueueTicket::class.java)
            .filter { it.queueDate in startDate..endDate }
    }

    private fun today() =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}