package com.example.aiexpensetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val recurringExpenseDao: RecurringExpenseDao
) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val categorySummaries: Flow<List<CategorySummary>> = expenseDao.getCategorySummaries()
    val totalSpent: Flow<Double> = expenseDao.getTotalSpent()
    val currentBudget: Flow<Budget?> = expenseDao.getBudget()

    fun getTotalSpentInPeriod(startDate: Long, endDate: Long): Flow<Double> =
        expenseDao.getTotalSpentInPeriod(startDate, endDate)

    fun getCategorySummariesInPeriod(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        expenseDao.getCategorySummariesInPeriod(startDate, endDate)

    fun getTotalActiveRecurringExpenses(): Flow<Double> {
        return recurringExpenseDao.getTotalActiveRecurringExpenses()
    }

    suspend fun generateRecurringExpenses(targetTime: Long = System.currentTimeMillis()) {
        withContext(Dispatchers.IO) {
            val generator = RecurringExpenseGenerator(expenseDao, recurringExpenseDao)
            generator.generateRecurringExpenses(targetTime)
        }
    }

    suspend fun insertExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            expenseDao.insertExpense(expense)
        }
    }

    suspend fun updateExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            expenseDao.updateExpense(expense)
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            expenseDao.deleteExpense(expense)
        }
    }

    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> = recurringExpenseDao.getAllRecurringExpenses()

    fun getRecurringExpenseById(id: Int): Flow<RecurringExpense?> = recurringExpenseDao.getRecurringExpenseById(id)

    suspend fun insertRecurringExpense(recurringExpense: RecurringExpense) {
        withContext(Dispatchers.IO) {
            recurringExpenseDao.insertRecurringExpense(recurringExpense)
        }
    }

    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense) {
        withContext(Dispatchers.IO) {
            recurringExpenseDao.updateRecurringExpense(recurringExpense)
        }
    }

    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        withContext(Dispatchers.IO) {
            recurringExpenseDao.deleteRecurringExpense(recurringExpense)
        }
    }

    fun getExpenseById(id: Int): Flow<Expense?> {
        return expenseDao.getExpenseById(id)
    }

    suspend fun insertBudget(budget: Budget) {
        withContext(Dispatchers.IO) {
            expenseDao.insertBudget(budget)
        }
    }

    fun getFilteredExpenses(searchQuery: String, category: String, sortOrder: SortOrder): Flow<List<Expense>> {
        return when (sortOrder) {
            SortOrder.DATE_DESC -> expenseDao.getExpensesSortedByDateDesc(searchQuery, category)
            SortOrder.DATE_ASC -> expenseDao.getExpensesSortedByDateAsc(searchQuery, category)
            SortOrder.AMOUNT_DESC -> expenseDao.getExpensesSortedByAmountDesc(searchQuery, category)
            SortOrder.AMOUNT_ASC -> expenseDao.getExpensesSortedByAmountAsc(searchQuery, category)
        }
    }

    fun getFilteredExpensesInPeriod(
        startDate: Long,
        endDate: Long,
        searchQuery: String,
        category: String,
        sortOrder: SortOrder
    ): Flow<List<Expense>> {
        return when (sortOrder) {
            SortOrder.DATE_DESC -> expenseDao.getExpensesInPeriodSortedByDateDesc(startDate, endDate, searchQuery, category)
            SortOrder.DATE_ASC -> expenseDao.getExpensesInPeriodSortedByDateAsc(startDate, endDate, searchQuery, category)
            SortOrder.AMOUNT_DESC -> expenseDao.getExpensesInPeriodSortedByAmountDesc(startDate, endDate, searchQuery, category)
            SortOrder.AMOUNT_ASC -> expenseDao.getExpensesInPeriodSortedByAmountAsc(startDate, endDate, searchQuery, category)
        }
    }
}

enum class SortOrder {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC
}
