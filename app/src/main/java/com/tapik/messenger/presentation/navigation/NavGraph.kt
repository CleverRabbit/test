package com.tapik.messenger.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tapik.messenger.presentation.ui.screens.auth.AuthScreen
import com.tapik.messenger.presentation.ui.screens.auth.AuthViewModel
import com.tapik.messenger.presentation.ui.screens.chats.ChatsScreen
import com.tapik.messenger.presentation.ui.screens.chats.ChatsViewModel
import com.tapik.messenger.presentation.ui.screens.profile.ProfileScreen
import com.tapik.messenger.presentation.ui.screens.profile.ProfileViewModel
import com.tapik.messenger.presentation.ui.screens.settings.SettingsScreen
import com.tapik.messenger.presentation.ui.screens.settings.SettingsViewModel
import com.tapik.messenger.presentation.ui.screens.chatManagement.ChatManagementScreen
import com.tapik.messenger.presentation.ui.screens.chatManagement.ChatManagementViewModel

@Composable
fun TapikNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Auth.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Auth.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Chats.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }

        composable(Screen.Chats.route) {
            val viewModel: ChatsViewModel = hiltViewModel()
            ChatsScreen(
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onChatClick = { chatId ->
                    navController.navigate(Screen.ChatManagement.createRoute(chatId))
                },
                onSearchClick = { /* Search is modal, handled in screen */ },
                viewModel = viewModel
            )
        }

        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = {
                    navController.popBackStack()
                    navController.navigate(Screen.Profile.route)
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.ChatManagement.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType }
            )
        ) {
            val viewModel: ChatManagementViewModel = hiltViewModel()
            ChatManagementScreen(
                chatId = it.arguments?.getString("chatId") ?: "",
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}
