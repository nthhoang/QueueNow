package com.example.queuenow.data.repository

import android.util.Log
import com.example.queuenow.data.model.Account
import com.example.queuenow.data.model.AccountStatus
import com.example.queuenow.data.model.RoleType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AccountRepository {
    private val db  = FirebaseFirestore.getInstance()
    private val col = db.collection("accounts")

    fun getAllAccounts(): Flow<List<Account>> = callbackFlow {
        val listener = col.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("AccountRepo", "getAllAccounts error: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(
                snap?.toObjects(Account::class.java)
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
            )
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateAccount(account: Account) {
        col.document(account.accountId).set(account).await()
    }

    suspend fun updateStatus(userId: String, status: AccountStatus) {
        col.document(userId).update("status", status.name).await()
    }

    suspend fun updateRole(userId: String, role: RoleType) {
        col.document(userId).update("role", role.name).await()
    }

    suspend fun updateAvatar(userId: String, avatarUrl: String) {
        col.document(userId).update("avatarUrl", avatarUrl).await()
    }
}