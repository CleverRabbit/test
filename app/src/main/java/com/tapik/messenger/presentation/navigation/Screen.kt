package com.tapik.messenger.presentation.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Chats : Screen("chats")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object ChatManagement : Screen("chat_management/{chatId}") {
        fun createRoute(chatId: String) = "chat_management/$chatId"
    }
}
