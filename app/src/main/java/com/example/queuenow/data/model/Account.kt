package com.example.queuenow.data.model

import com.google.firebase.firestore.DocumentId

enum class RoleType { USER, OWNER, ADMIN }
enum class AccountStatus { ACTIVE, LOCKED, INACTIVE }

data class Account(
    @DocumentId val accountId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val role: String = RoleType.USER.name,
    val status: String = AccountStatus.ACTIVE.name,
    val createdAt: Long = System.currentTimeMillis()
)