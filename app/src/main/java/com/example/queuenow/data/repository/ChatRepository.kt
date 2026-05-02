package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.ChatRoom
import com.example.queuenow.data.model.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatsCol = db.collection("chats")

    /**
     * ChatRoomId luôn là {userId}_{placeId} — deterministic
     * Đảm bảo mỗi cặp user-place chỉ có 1 phòng chat
     */
    fun buildChatRoomId(userId: String, placeId: String) = "${userId}_${placeId}"

    private fun messagesCol(chatRoomId: String) =
        chatsCol.document(chatRoomId).collection("messages")

    // ── Tạo hoặc lấy phòng chat ───────────────────────────────────────────────
    suspend fun getOrCreateChatRoom(
        userId: String,
        userName: String,
        ownerId: String,
        ownerName: String,
        placeId: String,
        placeName: String,
        placeImageUrl: String = ""
    ): ChatRoom {
        val roomId = buildChatRoomId(userId, placeId)
        val docRef = chatsCol.document(roomId)
        val snap   = docRef.get().await()

        return if (snap.exists()) {
            snap.toObject(ChatRoom::class.java) ?: ChatRoom(chatRoomId = roomId)
        } else {
            val room = ChatRoom(
                chatRoomId    = roomId,
                placeId       = placeId,
                placeName     = placeName,
                placeImageUrl = placeImageUrl,
                userId        = userId,
                userName      = userName,
                ownerId       = ownerId,
                ownerName     = ownerName,
                createdAt     = System.currentTimeMillis()
            )
            docRef.set(room).await()
            room
        }
    }

    // ── Gửi tin nhắn ──────────────────────────────────────────────────────────
    suspend fun sendMessage(
        chatRoomId: String,
        senderId: String,
        senderName: String,
        content: String,
        isOwner: Boolean   // true = owner gửi, false = user gửi
    ) {
        val msgRef = messagesCol(chatRoomId).document()
        val msg = Message(
            messageId  = msgRef.id,
            chatRoomId = chatRoomId,
            senderId   = senderId,
            senderName = senderName,
            content    = content.trim(),
            timestamp  = System.currentTimeMillis()
        )
        msgRef.set(msg).await()

        // Cập nhật ChatRoom: lastMessage + tăng unread của bên kia
        val updateMap = mutableMapOf<String, Any>(
            "lastMessage"     to content.trim(),
            "lastMessageTime" to msg.timestamp,
            "lastSenderId"    to senderId
        )
        if (isOwner) {
            // Owner gửi → tăng unread của user
            updateMap["unreadByUser"] = FieldValue.increment(1)
        } else {
            // User gửi → tăng unread của owner
            updateMap["unreadByOwner"] = FieldValue.increment(1)
        }
        chatsCol.document(chatRoomId).update(updateMap).await()
    }

    // ── Lắng nghe messages realtime ───────────────────────────────────────────
    fun getMessages(chatRoomId: String): Flow<List<Message>> = callbackFlow {
        val listener = messagesCol(chatRoomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("ChatRepo", "getMessages: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.toObjects(Message::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ── Lấy danh sách chat rooms của user ────────────────────────────────────
    fun getChatRoomsForUser(userId: String): Flow<List<ChatRoom>> = callbackFlow {
        val listener = chatsCol
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("ChatRepo", "getChatRoomsForUser: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(ChatRoom::class.java)
                    ?.filter { it.lastMessageTime > 0 }
                    ?.sortedByDescending { it.lastMessageTime }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Lấy danh sách chat rooms của owner ───────────────────────────────────
    fun getChatRoomsForOwner(ownerId: String): Flow<List<ChatRoom>> = callbackFlow {
        val listener = chatsCol
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("ChatRepo", "getChatRoomsForOwner: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(ChatRoom::class.java)
                    ?.filter { it.lastMessageTime > 0 }
                    ?.sortedByDescending { it.lastMessageTime }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    // ── Đánh dấu đã đọc ──────────────────────────────────────────────────────
    suspend fun markReadByUser(chatRoomId: String) {
        try {
            chatsCol.document(chatRoomId).update("unreadByUser", 0).await()
        } catch (e: Exception) {
            Log.e("ChatRepo", "markReadByUser: ${e.message}")
        }
    }

    suspend fun markReadByOwner(chatRoomId: String) {
        try {
            chatsCol.document(chatRoomId).update("unreadByOwner", 0).await()
        } catch (e: Exception) {
            Log.e("ChatRepo", "markReadByOwner: ${e.message}")
        }
    }

    // ── Đếm tổng unread của owner ────────────────────────────────────────────
    fun getTotalUnreadForOwner(ownerId: String): Flow<Int> = callbackFlow {
        val listener = chatsCol
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(0); return@addSnapshotListener }
                val total = snap?.toObjects(ChatRoom::class.java)
                    ?.sumOf { it.unreadByOwner }
                    ?: 0
                trySend(total)
            }
        awaitClose { listener.remove() }
    }

    // ── Đếm tổng unread của user ─────────────────────────────────────────────
    fun getTotalUnreadForUser(userId: String): Flow<Int> = callbackFlow {
        val listener = chatsCol
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(0); return@addSnapshotListener }
                val total = snap?.toObjects(ChatRoom::class.java)
                    ?.sumOf { it.unreadByUser }
                    ?: 0
                trySend(total)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getChatRoom(chatRoomId: String): ChatRoom? {
        return try {
            chatsCol.document(chatRoomId).get().await()
                .toObject(ChatRoom::class.java)
        } catch (e: Exception) { null }
    }
}