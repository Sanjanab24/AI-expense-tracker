package com.example.aiexpensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT category, SUM(amount) as totalAmount FROM expenses GROUP BY category ORDER BY totalAmount DESC")
    fun getCategorySummaries(): Flow<List<CategorySummary>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses")
    fun getTotalSpent(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBudget(budget: Budget)

    @Query("SELECT * FROM budgets ORDER BY id DESC LIMIT 1")
    fun getBudget(): Flow<Budget?>
}
