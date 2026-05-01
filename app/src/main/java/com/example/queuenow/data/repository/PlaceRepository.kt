package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.Place
import com.example.queuenow.data.model.PlaceStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PlaceRepository {
    private val db  = FirebaseFirestore.getInstance()
    private val col = db.collection("places")

    fun getOpenPlaces(): Flow<List<Place>> = callbackFlow {
        val listener = col.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("PlaceRepo", "getOpenPlaces: ${error.message}")
                trySend(emptyList()); return@addSnapshotListener
            }
            val list = snap?.toObjects(Place::class.java)
                ?.filter { it.status != PlaceStatus.LOCKED.name && !it.lockedByAdmin }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Lấy địa điểm theo ownerId — query server-side theo ownerId
     * Không bị ảnh hưởng bởi cache hay session cũ
     */
    fun getPlacesByOwner(ownerId: String): Flow<List<Place>> = callbackFlow {
        if (ownerId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        // Query chặt: whereEqualTo ownerId = uid hiện tại
        val listener = col
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("PlaceRepo", "getPlacesByOwner($ownerId): ${error.message}")
                    trySend(emptyList()); return@addSnapshotListener
                }
                val list = snap?.toObjects(Place::class.java)
                    // Client-side double check
                    ?.filter { it.ownerId == ownerId }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun getAllPlaces(): Flow<List<Place>> = callbackFlow {
        val listener = col.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("PlaceRepo", "getAllPlaces: ${error.message}")
                trySend(emptyList()); return@addSnapshotListener
            }
            trySend(
                snap?.toObjects(Place::class.java)
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
            )
        }
        awaitClose { listener.remove() }
    }

    suspend fun getPlace(placeId: String): Place? =
        col.document(placeId).get().await().toObject(Place::class.java)

    suspend fun savePlace(place: Place): String {
        return if (place.placeId.isEmpty()) {
            val ref = col.document()
            ref.set(place.copy(placeId = ref.id)).await()
            ref.id
        } else {
            col.document(place.placeId).set(place).await()
            place.placeId
        }
    }

    suspend fun updateStatus(placeId: String, status: PlaceStatus) {
        val place = getPlace(placeId) ?: return
        if (place.lockedByAdmin) {
            throw Exception("Địa điểm đang bị Admin khóa, không thể thay đổi trạng thái")
        }
        col.document(placeId).update("status", status.name).await()
    }

    suspend fun adminLockPlace(placeId: String) {
        col.document(placeId).update(
            mapOf("lockedByAdmin" to true, "status" to PlaceStatus.LOCKED.name)
        ).await()
    }

    suspend fun adminUnlockPlace(placeId: String) {
        col.document(placeId).update(
            mapOf("lockedByAdmin" to false, "status" to PlaceStatus.CLOSED.name)
        ).await()
    }

    suspend fun updateRatingWithCount(placeId: String, average: Double, count: Int) {
        col.document(placeId).update(
            mapOf("ratingAverage" to average, "ratingCount" to count)
        ).await()
    }
}