package com.example.aiexpensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiexpensetracker.data.Budget
import com.example.aiexpensetracker.data.CategorySummary
import com.example.aiexpensetracker.data.Expense
import com.example.aiexpensetracker.data.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorySummaries: StateFlow<List<CategorySummary>> = repository.categorySummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpent: StateFlow<Double> = repository.totalSpent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentBudget: StateFlow<Budget?> = repository.currentBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addExpense(amount: Double, description: String, category: String, date: Long) {
        viewModelScope.launch {
            repository.insertExpense(Expense(amount = amount, description = description, category = category, date = date))
        }
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch {
            repository.insertBudget(Budget(monthlyBudget = amount))
        }
    }
}
