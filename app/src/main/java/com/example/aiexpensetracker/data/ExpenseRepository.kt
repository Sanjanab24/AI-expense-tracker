package com.example.aiexpensetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val categorySummaries: Flow<List<CategorySummary>> = expenseDao.getCategorySummaries()
    val totalSpent: Flow<Double> = expenseDao.getTotalSpent()
    val currentBudget: Flow<Budget?> = expenseDao.getBudget()

    suspend fun insertExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            expenseDao.insertExpense(expense)
        }
    }

    suspend fun insertBudget(budget: Budget) {
        withContext(Dispatchers.IO) {
            expenseDao.insertBudget(budget)
        }
    }
}
