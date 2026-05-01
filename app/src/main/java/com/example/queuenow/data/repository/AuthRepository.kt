package com.example.queuenow.data.repository

import com.example.queuenow.data.model.Account
import com.example.queuenow.data.model.AccountStatus
import com.example.queuenow.data.model.RoleType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Result<Account> = runCatching {
        // 1. Thử đăng nhập Firebase Auth
        val result = try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw Exception("Email hoặc mật khẩu không đúng")
        } catch (e: FirebaseAuthInvalidUserException) {
            throw Exception("Tài khoản không tồn tại")
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("There is no user") == true       -> "Tài khoản không tồn tại"
                e.message?.contains("password is invalid") == true    -> "Mật khẩu không đúng"
                e.message?.contains("badly formatted") == true        -> "Email không hợp lệ"
                e.message?.contains("network") == true                -> "Lỗi kết nối mạng"
                else                                                   -> "Đăng nhập thất bại. Vui lòng thử lại"
            }
            throw Exception(msg)
        }

        val uid = result.user?.uid ?: throw Exception("Đăng nhập thất bại")

        // 2. Lấy thông tin tài khoản từ Firestore
        val account = getAccount(uid)
            ?: throw Exception("Không tìm thấy thông tin tài khoản")

        // 3. Kiểm tra tài khoản bị khóa
        if (account.status == AccountStatus.LOCKED.name) {
            auth.signOut()
            throw Exception("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.")
        }

        account
    }

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String
    ): Result<Account> = runCatching {
        val result = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("email address is already") == true -> "Email này đã được sử dụng"
                e.message?.contains("weak-password") == true            -> "Mật khẩu quá yếu (tối thiểu 6 ký tự)"
                e.message?.contains("badly formatted") == true          -> "Địa chỉ email không hợp lệ"
                e.message?.contains("network") == true                  -> "Lỗi kết nối mạng"
                else                                                     -> "Đăng ký thất bại: ${e.message}"
            }
            throw Exception(msg)
        }

        val uid = result.user?.uid ?: throw Exception("Đăng ký thất bại")
        val account = Account(
            accountId = uid,
            fullName  = fullName.trim(),
            phone     = phone.trim(),
            email     = email.trim(),
            role      = RoleType.USER.name
        )
        db.collection("accounts").document(uid).set(account).await()
        account
    }

    suspend fun getAccount(uid: String): Account? =
        db.collection("accounts").document(uid).get().await()
            .toObject(Account::class.java)

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun logout() = auth.signOut()
}