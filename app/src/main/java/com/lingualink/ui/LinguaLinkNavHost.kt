package com.lingualink.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lingualink.ui.screens.HistoryScreen
import com.lingualink.ui.screens.HomeScreen
import com.lingualink.ui.screens.SettingsScreen

sealed class Screen(val route: String, val label: String) {
    data object Home : Screen("home", "首页")
    data object History : Screen("history", "历史")
    data object Settings : Screen("settings", "设置")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinguaLinkNavHost() {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.History, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                when (screen) {
                                    Screen.Home -> Icons.Default.Home
                                    Screen.History -> Icons.Default.History
                                    Screen.Settings -> Icons.Default.Settings
                                },
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
