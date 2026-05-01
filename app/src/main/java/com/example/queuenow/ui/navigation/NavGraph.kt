package com.example.queuenow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.queuenow.ui.admin.*
import com.example.queuenow.ui.auth.*
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
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }

        // ── User ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.PlaceDetail.route) { back ->
            val placeId = back.arguments?.getString("placeId") ?: ""
            PlaceDetailScreen(navController, placeId)
        }
        composable(Screen.TakeTicket.route) { back ->
            val placeId = back.arguments?.getString("placeId") ?: ""
            val roomId  = back.arguments?.getString("roomId")  ?: ""
            TakeTicketScreen(navController, placeId, roomId)
        }
        composable(Screen.QueueStatus.route) { back ->
            val ticketId = back.arguments?.getString("ticketId") ?: ""
            QueueStatusScreen(navController, ticketId)
        }
        composable(Screen.TicketHistory.route) {
            TicketHistoryScreen(navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        composable(Screen.OwnerRequestScreen.route) {
            OwnerRequestScreen(navController)
        }
        composable(Screen.ReviewScreen.route) { back ->
            val ticketId = back.arguments?.getString("ticketId") ?: ""
            val placeId  = back.arguments?.getString("placeId")  ?: ""
            ReviewScreen(navController, ticketId, placeId)
        }

        // ── Owner ─────────────────────────────────────────────────────────────
        composable(Screen.OwnerDashboard.route) {
            OwnerDashboardScreen(navController)
        }
        composable(Screen.ManagePlace.route) {
            ManagePlaceScreen(navController)
        }
        composable(Screen.EditPlace.route) { back ->
            val placeId = back.arguments?.getString("placeId") ?: "new"
            EditPlaceScreen(navController, placeId)
        }
        composable(Screen.ManageRoom.route) { back ->
            val placeId = back.arguments?.getString("placeId") ?: ""
            ManageRoomScreen(navController, placeId)
        }
        composable(Screen.EditRoom.route) { back ->
            val placeId = back.arguments?.getString("placeId") ?: ""
            val roomId  = back.arguments?.getString("roomId")  ?: "new"
            EditRoomScreen(navController, placeId, roomId)
        }
        composable(Screen.LiveQueue.route) { back ->
            val placeId = back.arguments?.getString("placeId") ?: ""
            val roomId  = back.arguments?.getString("roomId")  ?: ""
            LiveQueueScreen(navController, placeId, roomId)
        }
        composable(Screen.PaymentConfirm.route) { back ->
            val placeId = back.arguments?.getString("placeId") ?: ""
            PaymentConfirmScreen(navController, placeId)
        }
        composable(Screen.OwnerStats.route) { back ->
            val placeId = back.arguments?.getString("placeId") ?: ""
            OwnerStatsScreen(navController, placeId)
        }

        // ── Admin ─────────────────────────────────────────────────────────────
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(navController)
        }
        composable(Screen.ManageAccounts.route) {
            ManageAccountsScreen(navController)
        }
        composable(Screen.ManagePlacesAdmin.route) {
            ManagePlacesAdminScreen(navController)
        }
        composable(Screen.OwnerRequestList.route) {
            OwnerRequestListScreen(navController)
        }

        // ── Shared ────────────────────────────────────────────────────────────
        composable(Screen.Notifications.route) {
            NotificationScreen(navController)
        }
    }
}