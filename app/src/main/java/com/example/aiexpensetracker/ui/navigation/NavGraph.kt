package com.example.aiexpensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
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
import com.example.aiexpensetracker.ui.viewmodel.MonthlyPlanViewModel
import com.example.aiexpensetracker.ui.viewmodel.CategoryBudgetViewModel
import androidx.navigation.navArgument
import androidx.navigation.NavType

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Budget : Screen("budget", "Budget")
    object CategoryBudgets : Screen("category_budgets", "Allocations")
    object MonthlyPlan : Screen("monthly_plan", "Plan")
    object AddExpense : Screen("add_expense", "Add Expense")
    object EditExpense : Screen("edit_expense/{expenseId}", "Edit Expense") {
        fun createRoute(expenseId: Int) = "edit_expense/$expenseId"
    }
    object RecurringExpenses : Screen("recurring_expenses", "Recurring")
    object AddEditRecurringExpense : Screen("add_edit_recurring?expenseId={expenseId}", "Add Recurring") {
        fun createRoute(expenseId: Int? = null) = if (expenseId != null) "add_edit_recurring?expenseId=$expenseId" else "add_edit_recurring"
    }
}

@Composable
fun AppNavGraph(
    viewModel: ExpenseViewModel,
    monthlyPlanViewModel: MonthlyPlanViewModel,
    categoryBudgetViewModel: CategoryBudgetViewModel,
    backendHealthViewModel: com.example.aiexpensetracker.ui.viewmodel.BackendHealthViewModel? = null
) {
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
                HomeScreen(
                    viewModel = viewModel,
                    backendHealthViewModel = backendHealthViewModel,
                    onAddExpenseClick = { navController.navigate(Screen.AddExpense.route) },
                    onExpenseClick = { expenseId -> navController.navigate(Screen.EditExpense.createRoute(expenseId)) }
                )
            }
            composable(Screen.Budget.route) {
                BudgetScreen(
                    viewModel = viewModel, 
                    categoryBudgetViewModel = categoryBudgetViewModel,
                    onNavigateToCategoryBudgets = { navController.navigate(Screen.CategoryBudgets.route) }
                )
            }
            composable(Screen.CategoryBudgets.route) {
                com.example.aiexpensetracker.ui.screen.CategoryBudgetsScreen(
                    categoryBudgetViewModel = categoryBudgetViewModel,
                    expenseViewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MonthlyPlan.route) {
                com.example.aiexpensetracker.ui.screen.MonthlyPlanningScreen(
                    viewModel = monthlyPlanViewModel
                )
            }
            composable(Screen.AddExpense.route) {
                AddExpenseScreen(viewModel = viewModel, onNavigateBack = {
                    navController.popBackStack()
                })
            }
            composable(
                route = Screen.EditExpense.route,
                arguments = listOf(androidx.navigation.navArgument("expenseId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getInt("expenseId") ?: return@composable
                com.example.aiexpensetracker.ui.screen.EditExpenseScreen(
                    expenseId = expenseId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RecurringExpenses.route) {
                com.example.aiexpensetracker.ui.screen.RecurringExpensesScreen(
                    viewModel = viewModel,
                    onAddClick = { navController.navigate(Screen.AddEditRecurringExpense.createRoute()) },
                    onEditClick = { expenseId -> navController.navigate(Screen.AddEditRecurringExpense.createRoute(expenseId)) }
                )
            }
            composable(
                route = Screen.AddEditRecurringExpense.route,
                arguments = listOf(navArgument("expenseId") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getInt("expenseId")?.takeIf { it != -1 }
                com.example.aiexpensetracker.ui.screen.AddEditRecurringExpenseScreen(
                    viewModel = viewModel,
                    expenseId = expenseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home,
        Screen.Budget,
        Screen.MonthlyPlan,
        Screen.RecurringExpenses
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom bar on top-level screens
    if (currentRoute == Screen.Home.route || currentRoute == Screen.Budget.route || currentRoute == Screen.MonthlyPlan.route || currentRoute == Screen.RecurringExpenses.route) {
        NavigationBar {
            items.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        val icon = when (screen) {
                            Screen.Home -> Icons.Filled.Home
                            Screen.Budget -> Icons.Filled.Settings
                            Screen.MonthlyPlan -> Icons.Filled.Add // Or another icon
                            Screen.RecurringExpenses -> Icons.Filled.List
                            else -> Icons.Filled.Home
                        }
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
