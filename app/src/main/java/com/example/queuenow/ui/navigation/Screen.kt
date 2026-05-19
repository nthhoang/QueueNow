package com.example.queuenow.ui.navigation

sealed class Screen(val route: String) {
    object Login    : Screen("login")
    object Register : Screen("register")

    object Home    : Screen("home")
    object PlaceDetail : Screen("place_detail/{placeId}") {
        fun createRoute(placeId: String) = "place_detail/$placeId"
    }
    object TakeTicket : Screen("take_ticket/{placeId}/{roomId}") {
        fun createRoute(placeId: String, roomId: String) = "take_ticket/$placeId/$roomId"
    }
    object QueueStatus : Screen("queue_status/{ticketId}") {
        fun createRoute(ticketId: String) = "queue_status/$ticketId"
    }
    object TicketHistory : Screen("ticket_history")
    object Profile       : Screen("profile")
    object OwnerRequestScreen : Screen("owner_request")
    object ReviewScreen : Screen("review/{ticketId}/{placeId}") {
        fun createRoute(ticketId: String, placeId: String) = "review/$ticketId/$placeId"
    }

    // ── Chat ─────────────────────────────────────────────────────────────────
    /** Màn hình chat với 1 người (user ↔ owner) */
    object ChatScreen : Screen("chat/{chatRoomId}") {
        fun createRoute(chatRoomId: String) = "chat/$chatRoomId"
    }
    /** Danh sách hội thoại của user */
    object UserChatList : Screen("user_chat_list")
    /** Danh sách hội thoại của owner */
    object OwnerChatList : Screen("owner_chat_list")

    // ── Owner ────────────────────────────────────────────────────────────────
    object OwnerDashboard : Screen("owner_dashboard")
    object ManagePlace    : Screen("manage_place")
    object EditPlace : Screen("edit_place/{placeId}") {
        fun createRoute(placeId: String) = "edit_place/$placeId"
    }
    object ManageRoom : Screen("manage_room/{placeId}") {
        fun createRoute(placeId: String) = "manage_room/$placeId"
    }
    object EditRoom : Screen("edit_room/{placeId}/{roomId}") {
        fun createRoute(placeId: String, roomId: String) = "edit_room/$placeId/$roomId"
    }
    object LiveQueue : Screen("live_queue/{placeId}/{roomId}") {
        fun createRoute(placeId: String, roomId: String) = "live_queue/$placeId/$roomId"
    }
    object PaymentConfirm : Screen("payment_confirm/{placeId}") {
        fun createRoute(placeId: String) = "payment_confirm/$placeId"
    }
    object OwnerStats : Screen("owner_stats/{placeId}") {
        fun createRoute(placeId: String) = "owner_stats/$placeId"
    }

    // ── Admin ────────────────────────────────────────────────────────────────
    object AdminDashboard    : Screen("admin_dashboard")
    object ManageAccounts    : Screen("manage_accounts")
    object ManagePlacesAdmin : Screen("manage_places_admin")
    object OwnerRequestList  : Screen("owner_request_list")

    // ── Shared ────────────────────────────────────────────────────────────────
    object Notifications : Screen("notifications")

    /** Màn hình quét QR check-in (user) */
    object QrScan : Screen("qr_scan/{placeId}/{roomId}") {
        fun createRoute(placeId: String, roomId: String) = "qr_scan/$placeId/$roomId"
    }

    /** Màn hình xem QR của địa điểm (owner) */
    object PlaceQr : Screen("place_qr/{placeId}") {
        fun createRoute(placeId: String) = "place_qr/$placeId"
    }
}