package com.tapik.messenger.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Chats : Screen("chats")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object ChatDetail : Screen("chat_detail/{chatId}") {
        fun createRoute(chatId: String) = "chat_detail/$chatId"
    }
}
