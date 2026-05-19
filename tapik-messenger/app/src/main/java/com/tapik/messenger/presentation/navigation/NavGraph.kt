package com.tapik.messenger.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tapik.messenger.presentation.ui.screens.auth.LoginScreen
import com.tapik.messenger.presentation.ui.screens.auth.LoginViewModel
import com.tapik.messenger.presentation.ui.screens.chats.ChatsScreen
import com.tapik.messenger.presentation.ui.screens.chats.ChatsViewModel
import com.tapik.messenger.presentation.ui.screens.profile.ProfileScreen
import com.tapik.messenger.presentation.ui.screens.profile.ProfileViewModel
import com.tapik.messenger.presentation.ui.screens.settings.SettingsScreen
import com.tapik.messenger.presentation.ui.screens.settings.SettingsViewModel

@Composable
fun TapikNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Chats.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
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
                onChatClick = { chatId -> navController.navigate(Screen.ChatDetail.createRoute(chatId)) },
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
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            // Chat detail screen placeholder
        }
    }
}
