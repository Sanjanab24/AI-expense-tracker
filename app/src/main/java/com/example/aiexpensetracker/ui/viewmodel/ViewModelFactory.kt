package com.example.aiexpensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aiexpensetracker.data.ExpenseRepository
import com.example.aiexpensetracker.data.MonthlyPlanRepository
import com.example.aiexpensetracker.data.CategoryBudgetRepository
import com.example.aiexpensetracker.data.AnalyticsRepository
import com.example.aiexpensetracker.network.HealthRepository
import com.example.aiexpensetracker.network.HealthRepositoryImpl

class ViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val monthlyPlanRepository: MonthlyPlanRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val healthRepository: HealthRepository = HealthRepositoryImpl()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(expenseRepository, monthlyPlanRepository) as T
        }
        if (modelClass.isAssignableFrom(MonthlyPlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonthlyPlanViewModel(monthlyPlanRepository) as T
        }
        if (modelClass.isAssignableFrom(CategoryBudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryBudgetViewModel(categoryBudgetRepository) as T
        }
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(analyticsRepository) as T
        }
        if (modelClass.isAssignableFrom(BackendHealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackendHealthViewModel(healthRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
