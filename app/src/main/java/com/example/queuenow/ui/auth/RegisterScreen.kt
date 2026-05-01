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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.queuenow.ui.components.GradientButton
import com.example.queuenow.ui.components.QueueTextField
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*

@Composable
fun RegisterScreen(navController: NavController, vm: AuthViewModel = viewModel()) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val state by vm.state.collectAsState()
    var localError by remember { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Register.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            Text("Tạo tài khoản", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Đăng ký để trải nghiệm dịch vụ", color = Color.White.copy(0.8f))
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    QueueTextField(fullName, { fullName = it }, "Họ và tên",
                        leadingIcon = { Icon(Icons.Filled.Person, null, tint = Primary) })
                    Spacer(Modifier.height(14.dp))
                    QueueTextField(phone, { phone = it }, "Số điện thoại",
                        leadingIcon = { Icon(Icons.Filled.Phone, null, tint = Primary) })
                    Spacer(Modifier.height(14.dp))
                    QueueTextField(email, { email = it }, "Email",
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = Primary) })
                    Spacer(Modifier.height(14.dp))
                    QueueTextField(password, { password = it }, "Mật khẩu",
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Primary) },
                        visualTransformation = PasswordVisualTransformation())
                    Spacer(Modifier.height(14.dp))
                    QueueTextField(confirmPassword, { confirmPassword = it }, "Xác nhận mật khẩu",
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Primary) },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword)

                    if (localError.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(localError, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(24.dp))
                    GradientButton(
                        text = if (state.isLoading) "Đang đăng ký..." else "Đăng ký",
                        onClick = {
                            if (password != confirmPassword) { localError = "Mật khẩu không khớp"; return@GradientButton }
                            if (fullName.isBlank() || email.isBlank() || password.isBlank()) { localError = "Vui lòng điền đầy đủ"; return@GradientButton }
                            localError = ""
                            vm.register(email, password, fullName, phone)
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.Center, Alignment.CenterVertically) {
                        Text("Đã có tài khoản?", color = TextSecondary)
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Đăng nhập", color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}