package com.example.aiexpensetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurringExpenseGeneratorTest {

    class FakeExpenseDao : ExpenseDao {
        val expenses = mutableListOf<Expense>()

        override fun insertExpense(expense: Expense) {
            expenses.add(expense)
        }

        override fun updateExpense(expense: Expense) {}
        override fun deleteExpense(expense: Expense) {}
        override fun getExpenseById(id: Int): Flow<Expense?> = throw NotImplementedError()
        override fun getAllExpenses(): Flow<List<Expense>> = throw NotImplementedError()
        override fun getCategorySummaries(): Flow<List<CategorySummary>> = throw NotImplementedError()
        override fun getTotalSpent(): Flow<Double> = throw NotImplementedError()
        override fun getTotalSpentInPeriod(startDate: Long, endDate: Long): Flow<Double> =
            kotlinx.coroutines.flow.flowOf(expenses.filter { it.date >= startDate && it.date < endDate }.sumOf { it.amount })
        override fun getCategorySummariesInPeriod(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
            kotlinx.coroutines.flow.flowOf(emptyList())
        override fun insertBudget(budget: Budget) {}
        override fun getBudget(): Flow<Budget?> = throw NotImplementedError()
        override fun getExpensesSortedByDateDesc(searchQuery: String, category: String): Flow<List<Expense>> = throw NotImplementedError()
        override fun getExpensesSortedByDateAsc(searchQuery: String, category: String): Flow<List<Expense>> = throw NotImplementedError()
        override fun getExpensesSortedByAmountDesc(searchQuery: String, category: String): Flow<List<Expense>> = throw NotImplementedError()
        override fun getExpensesSortedByAmountAsc(searchQuery: String, category: String): Flow<List<Expense>> = throw NotImplementedError()
        override fun getExpensesInPeriodSortedByDateDesc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>> = throw NotImplementedError()
        override fun getExpensesInPeriodSortedByDateAsc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>> = throw NotImplementedError()
        override fun getExpensesInPeriodSortedByAmountDesc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>> = throw NotImplementedError()
        override fun getExpensesInPeriodSortedByAmountAsc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>> = throw NotImplementedError()

        override fun countRecurringExpenseInPeriod(
            recurringId: Long,
            periodStart: Long,
            periodEnd: Long
        ): Int {
            return expenses.count {
                it.recurringExpenseId == recurringId && it.date >= periodStart && it.date < periodEnd
            }
        }
    }

    class FakeRecurringExpenseDao : RecurringExpenseDao {
        val recurringExpenses = mutableListOf<RecurringExpense>()

        override fun insertRecurringExpense(recurringExpense: RecurringExpense) {
            recurringExpenses.add(recurringExpense)
        }

        override fun updateRecurringExpense(recurringExpense: RecurringExpense) {}
        override fun deleteRecurringExpense(recurringExpense: RecurringExpense) {}
        override fun getRecurringExpenseById(id: Int): Flow<RecurringExpense?> = throw NotImplementedError()
        override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> = throw NotImplementedError()
        override fun getTotalActiveRecurringExpenses(): Flow<Double> = throw NotImplementedError()

        override fun getActiveRecurringExpensesList(): List<RecurringExpense> {
            return recurringExpenses.filter { it.isActive }
        }
    }

    @Test
    fun testGenerateRecurringExpenses_whenNoneExist_doesNothing() = runBlocking {
        val expenseDao = FakeExpenseDao()
        val recurringExpenseDao = FakeRecurringExpenseDao()
        val generator = RecurringExpenseGenerator(expenseDao, recurringExpenseDao)

        generator.generateRecurringExpenses()

        assertTrue(expenseDao.expenses.isEmpty())
    }

    @Test
    fun testGenerateRecurringExpenses_createsNewExpense_whenNotAlreadyGenerated() = runBlocking {
        val expenseDao = FakeExpenseDao()
        val recurringExpenseDao = FakeRecurringExpenseDao()
        
        recurringExpenseDao.insertRecurringExpense(
            RecurringExpense(id = 1, name = "Rent", category = "Bills", amount = 1000.0, isActive = true)
        )
        
        val generator = RecurringExpenseGenerator(expenseDao, recurringExpenseDao)
        generator.generateRecurringExpenses()

        assertEquals(1, expenseDao.expenses.size)
        val generated = expenseDao.expenses[0]
        assertEquals(1000.0, generated.amount, 0.0)
        assertEquals("Rent", generated.description)
        assertEquals("Bills", generated.category)
        assertTrue(generated.isRecurring)
        assertEquals(1L, generated.recurringExpenseId)
    }

    @Test
    fun testGenerateRecurringExpenses_doesNotDuplicate_whenAlreadyGeneratedForCurrentMonth() = runBlocking {
        val expenseDao = FakeExpenseDao()
        val recurringExpenseDao = FakeRecurringExpenseDao()
        
        recurringExpenseDao.insertRecurringExpense(
            RecurringExpense(id = 1, name = "Rent", category = "Bills", amount = 1000.0, isActive = true)
        )

        // Pre-populate an expense for the current month
        val currentTime = System.currentTimeMillis()
        expenseDao.insertExpense(
            Expense(
                amount = 1000.0,
                description = "Rent",
                category = "Bills",
                date = currentTime,
                isRecurring = true,
                recurringExpenseId = 1L
            )
        )
        
        val generator = RecurringExpenseGenerator(expenseDao, recurringExpenseDao)
        generator.generateRecurringExpenses()

        // Size should remain 1 (no new expense generated)
        assertEquals(1, expenseDao.expenses.size)
    }
}
