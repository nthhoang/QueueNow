package com.example.queuenow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.queuenow.ui.admin.*
import com.example.queuenow.ui.auth.*
import com.example.queuenow.ui.chat.*
import com.example.queuenow.ui.notification.NotificationScreen
import com.example.queuenow.ui.owner.dashboard.OwnerDashboardScreen
import com.example.queuenow.ui.owner.payment.PaymentConfirmScreen
import com.example.queuenow.ui.owner.place.*
import com.example.queuenow.ui.owner.queue.LiveQueueScreen
import com.example.queuenow.ui.owner.room.*
import com.example.queuenow.ui.owner.stats.OwnerStatsScreen
import com.example.queuenow.ui.user.home.HomeScreen
import com.example.queuenow.ui.user.place.PlaceDetailScreen
import com.example.queuenow.ui.user.profile.*
import com.example.queuenow.ui.user.queue.*
import com.example.queuenow.ui.user.review.ReviewScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Login.route)    { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }

        // ── User ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route)         { HomeScreen(navController) }
        composable(Screen.TicketHistory.route){ TicketHistoryScreen(navController) }
        composable(Screen.Profile.route)      { ProfileScreen(navController) }
        composable(Screen.OwnerRequestScreen.route) { OwnerRequestScreen(navController) }
        composable(Screen.UserChatList.route) { UserChatListScreen(navController) }

        composable(Screen.PlaceDetail.route) { back ->
            PlaceDetailScreen(navController, back.arguments?.getString("placeId") ?: "")
        }
        composable(Screen.TakeTicket.route) { back ->
            TakeTicketScreen(
                navController,
                back.arguments?.getString("placeId") ?: "",
                back.arguments?.getString("roomId")  ?: ""
            )
        }
        composable(Screen.QueueStatus.route) { back ->
            QueueStatusScreen(navController, back.arguments?.getString("ticketId") ?: "")
        }
        composable(Screen.ReviewScreen.route) { back ->
            ReviewScreen(
                navController,
                back.arguments?.getString("ticketId") ?: "",
                back.arguments?.getString("placeId")  ?: ""
            )
        }

        // ── Chat — hỗ trợ 2 cách navigate ────────────────────────────────────
        // Cách 1: dùng chatRoomId (từ ChatList)
        // Route: "chat/{chatRoomId}"
        composable(
            route = "chat/{chatRoomId}",
            arguments = listOf(navArgument("chatRoomId") { type = NavType.StringType })
        ) { back ->
            val chatRoomId = back.arguments?.getString("chatRoomId") ?: ""
            ChatScreen(navController = navController, chatRoomId = chatRoomId)
        }

        // Cách 2: dùng placeId + ownerId (từ PlaceDetailScreen)
        // Route: "chat_new?placeId={placeId}&ownerId={ownerId}"
        composable(
            route = "chat_new?placeId={placeId}&ownerId={ownerId}",
            arguments = listOf(
                navArgument("placeId") { type = NavType.StringType; defaultValue = "" },
                navArgument("ownerId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { back ->
            ChatScreen(
                navController = navController,
                placeId       = back.arguments?.getString("placeId") ?: "",
                ownerId       = back.arguments?.getString("ownerId") ?: ""
            )
        }

        // ── Owner ─────────────────────────────────────────────────────────────
        composable(Screen.OwnerDashboard.route)  { OwnerDashboardScreen(navController) }
        composable(Screen.ManagePlace.route)     { ManagePlaceScreen(navController) }
        composable(Screen.OwnerChatList.route)   { OwnerChatListScreen(navController) }

        composable(Screen.EditPlace.route) { back ->
            EditPlaceScreen(navController, back.arguments?.getString("placeId") ?: "new")
        }
        composable(Screen.ManageRoom.route) { back ->
            ManageRoomScreen(navController, back.arguments?.getString("placeId") ?: "")
        }
        composable(Screen.EditRoom.route) { back ->
            EditRoomScreen(
                navController,
                back.arguments?.getString("placeId") ?: "",
                back.arguments?.getString("roomId")  ?: "new"
            )
        }
        composable(Screen.LiveQueue.route) { back ->
            LiveQueueScreen(
                navController,
                back.arguments?.getString("placeId") ?: "",
                back.arguments?.getString("roomId")  ?: ""
            )
        }
        composable(Screen.PaymentConfirm.route) { back ->
            PaymentConfirmScreen(navController, back.arguments?.getString("placeId") ?: "")
        }
        composable(Screen.OwnerStats.route) { back ->
            OwnerStatsScreen(navController, back.arguments?.getString("placeId") ?: "")
        }

        // ── Admin ─────────────────────────────────────────────────────────────
        composable(Screen.AdminDashboard.route)    { AdminDashboardScreen(navController) }
        composable(Screen.ManageAccounts.route)    { ManageAccountsScreen(navController) }
        composable(Screen.ManagePlacesAdmin.route) { ManagePlacesAdminScreen(navController) }
        composable(Screen.OwnerRequestList.route)  { OwnerRequestListScreen(navController) }

        // ── Shared ────────────────────────────────────────────────────────────
        composable(Screen.Notifications.route) { NotificationScreen(navController) }

        // ── QR ────────────────────────────────────────────────────────────────
        composable(Screen.QrScan.route) { back ->
            QrScanScreen(
                navController = navController,
                placeId = back.arguments?.getString("placeId") ?: "",
                roomId  = back.arguments?.getString("roomId")  ?: ""
            )
        }

        composable(Screen.PlaceQr.route) { back ->
            PlaceQrScreen(
                navController = navController,
                placeId = back.arguments?.getString("placeId") ?: ""
            )
        }
    }
}