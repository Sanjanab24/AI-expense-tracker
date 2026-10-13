package com.example.aiexpensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecurringExpense(recurringExpense: RecurringExpense)

    @Update
    fun updateRecurringExpense(recurringExpense: RecurringExpense)

    @Delete
    fun deleteRecurringExpense(recurringExpense: RecurringExpense)

    @Query("SELECT * FROM recurring_expenses WHERE id = :id LIMIT 1")
    fun getRecurringExpenseById(id: Int): Flow<RecurringExpense?>

    @Query("SELECT * FROM recurring_expenses")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM recurring_expenses WHERE isActive = 1")
    fun getTotalActiveRecurringExpenses(): Flow<Double>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1")
    fun getActiveRecurringExpensesList(): List<RecurringExpense>
}
