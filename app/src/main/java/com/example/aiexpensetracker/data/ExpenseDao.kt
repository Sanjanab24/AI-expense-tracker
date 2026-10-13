package com.example.aiexpensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExpense(expense: Expense)

    @Update
    fun updateExpense(expense: Expense)

    @Delete
    fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: Int): Flow<Expense?>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT category, SUM(amount) as totalAmount FROM expenses GROUP BY category ORDER BY totalAmount DESC")
    fun getCategorySummaries(): Flow<List<CategorySummary>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses")
    fun getTotalSpent(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date >= :startDate AND date < :endDate")
    fun getTotalSpentInPeriod(startDate: Long, endDate: Long): Flow<Double>

    @Query("SELECT category, SUM(amount) as totalAmount FROM expenses WHERE date >= :startDate AND date < :endDate GROUP BY category ORDER BY totalAmount DESC")
    fun getCategorySummariesInPeriod(startDate: Long, endDate: Long): Flow<List<CategorySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBudget(budget: Budget)

    @Query("SELECT * FROM budgets ORDER BY id DESC LIMIT 1")
    fun getBudget(): Flow<Budget?>

    @Query("SELECT * FROM expenses WHERE (:category = 'All' OR category = :category) AND (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') ORDER BY date DESC")
    fun getExpensesSortedByDateDesc(searchQuery: String, category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE (:category = 'All' OR category = :category) AND (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') ORDER BY date ASC")
    fun getExpensesSortedByDateAsc(searchQuery: String, category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE (:category = 'All' OR category = :category) AND (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') ORDER BY amount DESC")
    fun getExpensesSortedByAmountDesc(searchQuery: String, category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE (:category = 'All' OR category = :category) AND (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') ORDER BY amount ASC")
    fun getExpensesSortedByAmountAsc(searchQuery: String, category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date < :endDate AND (:category = 'All' OR category = :category) AND (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') ORDER BY date DESC")
    fun getExpensesInPeriodSortedByDateDesc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date < :endDate AND (:category = 'All' OR category = :category) AND (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') ORDER BY date ASC")
    fun getExpensesInPeriodSortedByDateAsc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date < :endDate AND (:category = 'All' OR category = :category) AND (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') ORDER BY amount DESC")
    fun getExpensesInPeriodSortedByAmountDesc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date < :endDate AND (:category = 'All' OR category = :category) AND (description LIKE '%' || :searchQuery || '%' OR category LIKE '%' || :searchQuery || '%') ORDER BY amount ASC")
    fun getExpensesInPeriodSortedByAmountAsc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>>

    @Query("SELECT COUNT(*) FROM expenses WHERE recurringExpenseId = :recurringId AND date >= :periodStart AND date < :periodEnd")
    fun countRecurringExpenseInPeriod(recurringId: Long, periodStart: Long, periodEnd: Long): Int
}
