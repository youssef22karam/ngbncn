package com.jarvis.ai.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.ai.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Chat    : Screen("chat")
    object Models  : Screen("models")
    object Settings: Screen("settings")
    object Permissions: Screen("permissions")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Chat.route) {
        composable(Screen.Chat.route) {
            ChatScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Models.route) {
            ModelsScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel, navController = navController)
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(viewModel = viewModel, navController = navController)
        }
    }
}
