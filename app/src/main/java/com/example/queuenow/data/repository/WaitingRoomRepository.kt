package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.RoomStatus
import com.example.queuenow.data.model.WaitingRoom
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WaitingRoomRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun col(placeId: String) =
        db.collection("places").document(placeId).collection("rooms")

    fun getRooms(placeId: String): Flow<List<WaitingRoom>> = callbackFlow {
        val listener = col(placeId).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("RoomRepo", "getRooms error: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snap?.toObjects(WaitingRoom::class.java) ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    suspend fun getRoom(placeId: String, roomId: String): WaitingRoom? =
        col(placeId).document(roomId).get().await().toObject(WaitingRoom::class.java)

    suspend fun saveRoom(placeId: String, room: WaitingRoom): String {
        return if (room.roomId.isEmpty()) {
            val ref = col(placeId).document()
            ref.set(room.copy(roomId = ref.id, placeId = placeId)).await()
            ref.id
        } else {
            col(placeId).document(room.roomId).set(room).await()
            room.roomId
        }
    }

    suspend fun updateStatus(placeId: String, roomId: String, status: RoomStatus) {
        col(placeId).document(roomId).update("status", status.name).await()
    }

    /**
     * Dùng FieldValue.increment — atomic, không cần transaction
     * Security rule cho phép user update field currentNumber
     */
    suspend fun incrementCurrentNumber(placeId: String, roomId: String) {
        col(placeId).document(roomId)
            .update("currentNumber", FieldValue.increment(1)).await()
    }
}