package com.example.queuenow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.queuenow.data.model.RoleType
import com.example.queuenow.data.repository.AuthRepository
import com.example.queuenow.data.service.CloudinaryService
import com.example.queuenow.ui.navigation.AppNavGraph
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.Primary
import com.example.queuenow.ui.theme.QueueNowTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        CloudinaryService.init(this)

        setContent {
            QueueNowTheme {
                val navController = rememberNavController()
                // startDestination = null → đang load; "" → chưa login
                var startDestination by remember { mutableStateOf<String?>(null) }

                // Xác định màn hình khởi đầu theo role — chạy trên IO thread
                LaunchedEffect(Unit) {
                    val repo = AuthRepository()
                    val uid  = repo.getCurrentUserId()
                    startDestination = if (uid == null) {
                        Screen.Login.route
                    } else {
                        val account = withContext(Dispatchers.IO) { repo.getAccount(uid) }
                        when (account?.role) {
                            RoleType.ADMIN.name -> Screen.AdminDashboard.route
                            RoleType.OWNER.name -> Screen.OwnerDashboard.route
                            else                -> Screen.Home.route
                        }
                    }
                }

                when (val dest = startDestination) {
                    null -> {
                        // Splash loading
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    else -> {
                        AppNavGraph(
                            navController = navController,
                            startDestination = dest
                        )
                    }
                }
            }
        }
    }
}