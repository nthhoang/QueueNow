package com.example.queuenow.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.data.model.RoleType
import com.example.queuenow.ui.components.GradientButton
import com.example.queuenow.ui.components.QueueTextField
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*

@Composable
fun LoginScreen(navController: NavController, vm: AuthViewModel = viewModel()) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val state           by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Điều hướng khi đăng nhập thành công
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess && state.account != null) {
            val dest = when (state.account!!.role) {
                RoleType.ADMIN.name -> Screen.AdminDashboard.route
                RoleType.OWNER.name -> Screen.OwnerDashboard.route
                else                -> Screen.Home.route
            }
            navController.navigate(dest) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    // Hiển thị lỗi qua Snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(80.dp))

                // Logo
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.QueuePlayNext, null,
                        tint = Color.White, modifier = Modifier.size(50.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("QueueNow", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Đặt số thứ tự thông minh", color = Color.White.copy(0.8f))

                Spacer(Modifier.height(40.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(28.dp)) {
                        Text("Đăng nhập", style = MaterialTheme.typography.headlineMedium,
                            color = OnBackground, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))

                        QueueTextField(
                            value = email, onValueChange = { email = it },
                            label = "Email",
                            leadingIcon = { Icon(Icons.Filled.Email, null, tint = Primary) }
                        )
                        Spacer(Modifier.height(14.dp))
                        QueueTextField(
                            value = password, onValueChange = { password = it },
                            label = "Mật khẩu",
                            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Primary) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Filled.VisibilityOff
                                        else Icons.Filled.Visibility,
                                        null, tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None else PasswordVisualTransformation()
                        )

                        // Hiển thị lỗi inline (đặc biệt cho tài khoản bị khóa)
                        if (state.error != null) {
                            Spacer(Modifier.height(10.dp))
                            val isLocked = state.error?.contains("bị khóa") == true
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isLocked) StatusCanceled.copy(0.1f)
                                else StatusCanceled.copy(0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        if (isLocked) Icons.Filled.Lock else Icons.Filled.Error,
                                        null, tint = StatusCanceled,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(state.error!!, style = MaterialTheme.typography.bodySmall,
                                        color = StatusCanceled)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        GradientButton(
                            text = if (state.isLoading) "Đang đăng nhập..." else "Đăng nhập",
                            onClick = { vm.login(email, password) },
                            enabled = !state.isLoading && email.isNotEmpty() && password.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chưa có tài khoản?", color = TextSecondary)
                            TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                                Text("Đăng ký", color = Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}