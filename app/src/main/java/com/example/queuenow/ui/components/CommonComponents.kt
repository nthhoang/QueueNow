package com.example.queuenow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.queuenow.ui.navigation.Screen
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toStatusLabel

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled)
                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                    else
                        Brush.horizontalGradient(listOf(Color.Gray, Color.Gray)),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QueueTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            focusedLabelColor = Primary
        )
    )
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "OPEN", "ACTIVE", "APPROVED", "CONFIRMED" -> StatusCompleted
        "CLOSED", "INACTIVE", "REJECTED"           -> StatusCanceled
        "WAITING", "PENDING", "SUBMITTED"          -> StatusWaiting
        "CALLED"                                   -> StatusCalled
        "LOCKED", "SKIPPED"                        -> StatusSkipped
        "PAUSED"                                   -> StatusCalled
        else                                       -> TextSecondary
    }
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            text = status.toStatusLabel(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator(color = Primary) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = OnBackground,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun UserBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        listOf(
            Triple(Screen.Home.route,        Icons.Filled.Home,               "Trang chủ"),
            Triple(Screen.TicketHistory.route, Icons.Filled.ConfirmationNumber, "Vé của tôi"),
            Triple(Screen.UserChatList.route, Icons.Filled.Chat,              "Nhắn tin"),
            Triple(Screen.Profile.route,     Icons.Filled.Person,             "Hồ sơ")
        ).forEach { (route, icon, label) ->
            NavigationBarItem(
                icon     = { Icon(icon, contentDescription = label) },
                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                selected = currentRoute == route,
                onClick  = {
                    if (currentRoute != route) navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Primary, selectedTextColor   = Primary,
                    unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary,
                    indicatorColor      = PrimaryLight
                )
            )
        }
    }
}

@Composable
fun OwnerBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        listOf(
            Triple(Screen.OwnerDashboard.route, Icons.Filled.Dashboard,  "Tổng quan"),
            Triple(Screen.ManagePlace.route,    Icons.Filled.Store,      "Địa điểm"),
            Triple(Screen.OwnerChatList.route,  Icons.Filled.Chat,       "Nhắn tin"),
            Triple(Screen.Profile.route,        Icons.Filled.Person,     "Hồ sơ")
        ).forEach { (route, icon, label) ->
            NavigationBarItem(
                icon     = { Icon(icon, contentDescription = label) },
                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                selected = currentRoute == route,
                onClick  = {
                    if (currentRoute != route) navController.navigate(route) {
                        popUpTo(Screen.OwnerDashboard.route) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Primary, selectedTextColor   = Primary,
                    unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary,
                    indicatorColor      = PrimaryLight
                )
            )
        }
    }
}