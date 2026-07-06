package com.example.aiexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aiexpensetracker.ui.screen.AddExpenseScreen
import com.example.aiexpensetracker.ui.screen.BudgetScreen
import com.example.aiexpensetracker.ui.screen.HomeScreen
import com.example.aiexpensetracker.ui.viewmodel.ExpenseViewModel

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Budget : Screen("budget", "Budget")
    object AddExpense : Screen("add_expense", "Add Expense")
}

@Composable
fun AppNavGraph(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(viewModel = viewModel, onAddExpenseClick = {
                    navController.navigate(Screen.AddExpense.route)
                })
            }
            composable(Screen.Budget.route) {
                BudgetScreen(viewModel = viewModel)
            }
            composable(Screen.AddExpense.route) {
                AddExpenseScreen(viewModel = viewModel, onNavigateBack = {
                    navController.popBackStack()
                })
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home,
        Screen.Budget
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom bar on top-level screens
    if (currentRoute == Screen.Home.route || currentRoute == Screen.Budget.route) {
        NavigationBar {
            items.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        val icon = if (screen == Screen.Home) Icons.Filled.Home else Icons.Filled.Settings
                        Icon(icon, contentDescription = screen.title)
                    },
                    label = { Text(screen.title) },
                    selected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
