package com.example.aiexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.aiexpensetracker.data.AppDatabase
import com.example.aiexpensetracker.data.ExpenseRepository
import com.example.aiexpensetracker.ui.navigation.AppNavGraph
import com.example.aiexpensetracker.ui.theme.AIExpenseTrackerTheme
import com.example.aiexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.aiexpensetracker.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = ExpenseRepository(database.expenseDao(), database.recurringExpenseDao())
        val monthlyPlanRepository = com.example.aiexpensetracker.data.MonthlyPlanRepository(database.monthlyPlanDao())
        val categoryBudgetRepository = com.example.aiexpensetracker.data.CategoryBudgetRepository(database.categoryBudgetDao())
        val analyticsRepository = com.example.aiexpensetracker.data.AnalyticsRepository(database.expenseDao(), database.recurringExpenseDao(), database.monthlyPlanDao())
        val viewModelFactory = ViewModelFactory(repository, monthlyPlanRepository, categoryBudgetRepository, analyticsRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[ExpenseViewModel::class.java]
        val monthlyPlanViewModel = ViewModelProvider(this, viewModelFactory)[com.example.aiexpensetracker.ui.viewmodel.MonthlyPlanViewModel::class.java]
        val categoryBudgetViewModel = ViewModelProvider(this, viewModelFactory)[com.example.aiexpensetracker.ui.viewmodel.CategoryBudgetViewModel::class.java]
        val analyticsViewModel = ViewModelProvider(this, viewModelFactory)[com.example.aiexpensetracker.ui.viewmodel.AnalyticsViewModel::class.java]
        val backendHealthViewModel = ViewModelProvider(this, viewModelFactory)[com.example.aiexpensetracker.ui.viewmodel.BackendHealthViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            AIExpenseTrackerTheme {
                AppNavGraph(
                    viewModel = viewModel,
                    monthlyPlanViewModel = monthlyPlanViewModel,
                    categoryBudgetViewModel = categoryBudgetViewModel,
                    backendHealthViewModel = backendHealthViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.generateRecurringExpenses()
        }
    }
}