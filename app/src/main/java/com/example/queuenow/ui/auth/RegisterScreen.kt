package com.example.queuenow.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val state by vm.state.collectAsState()
    var localError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Register.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd)))
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                
                Text(
                    "Tạo tài khoản mới", 
                    fontSize = 32.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White
                )
                Text(
                    "Đăng ký để bắt đầu trải nghiệm dịch vụ", 
                    color = Color.White.copy(0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(modifier = Modifier.padding(32.dp)) {
                        QueueTextField(
                            value = fullName, 
                            onValueChange = { fullName = it }, 
                            label = "Họ và tên",
                            leadingIcon = { Icon(Icons.Filled.Person, null, tint = Primary) }
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        QueueTextField(
                            value = phone, 
                            onValueChange = { phone = it }, 
                            label = "Số điện thoại",
                            leadingIcon = { Icon(Icons.Filled.Phone, null, tint = Primary) }
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        QueueTextField(
                            value = email, 
                            onValueChange = { email = it }, 
                            label = "Email",
                            leadingIcon = { Icon(Icons.Filled.Email, null, tint = Primary) }
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        QueueTextField(
                            value = password, 
                            onValueChange = { password = it }, 
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
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        QueueTextField(
                            value = confirmPassword, 
                            onValueChange = { confirmPassword = it }, 
                            label = "Xác nhận mật khẩu",
                            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Primary) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        if (confirmPasswordVisible) Icons.Filled.VisibilityOff
                                        else Icons.Filled.Visibility,
                                        null, tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = confirmPassword.isNotEmpty() && password != confirmPassword
                        )

                        val displayError = localError ?: state.error
                        AnimatedVisibility(
                            visible = displayError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            displayError?.let {
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Error, null, tint = StatusCanceled, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(it, color = StatusCanceled, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        
                        GradientButton(
                            text = if (state.isLoading) "Đang xử lý..." else "Đăng ký ngay",
                            onClick = {
                                when {
                                    fullName.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank() -> {
                                        localError = "Vui lòng điền đầy đủ thông tin"
                                    }
                                    password != confirmPassword -> {
                                        localError = "Mật khẩu xác nhận không khớp"
                                    }
                                    password.length < 6 -> {
                                        localError = "Mật khẩu phải có ít nhất 6 ký tự"
                                    }
                                    else -> {
                                        localError = null
                                        vm.register(email, password, fullName, phone)
                                    }
                                }
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Đã có tài khoản?", color = TextSecondary)
                            TextButton(onClick = { navController.popBackStack() }) {
                                Text("Đăng nhập", color = Primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
