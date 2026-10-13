package com.example.aiexpensetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class AnalyticsRepositoryTest {

    class FakeExpenseDao : ExpenseDao {
        val expenses = mutableListOf<Expense>()

        override fun insertExpense(expense: Expense) { expenses.add(expense) }
        override fun updateExpense(expense: Expense) {
            val index = expenses.indexOfFirst { it.id == expense.id }
            if (index != -1) expenses[index] = expense
        }
        override fun deleteExpense(expense: Expense) {
            expenses.removeAll { it.id == expense.id }
        }
        override fun getExpenseById(id: Int): Flow<Expense?> = kotlinx.coroutines.flow.flowOf(expenses.find { it.id == id })
        override fun getAllExpenses(): Flow<List<Expense>> = kotlinx.coroutines.flow.flowOf(expenses)
        override fun getCategorySummaries(): Flow<List<CategorySummary>> = throw NotImplementedError()
        override fun getTotalSpent(): Flow<Double> = throw NotImplementedError()
        override fun getTotalSpentInPeriod(startDate: Long, endDate: Long): Flow<Double> =
            kotlinx.coroutines.flow.flowOf(expenses.filter { it.date >= startDate && it.date < endDate }.sumOf { it.amount })
        override fun getCategorySummariesInPeriod(startDate: Long, endDate: Long): Flow<List<CategorySummary>> {
            val summaries = expenses.filter { it.date >= startDate && it.date < endDate }
                .groupBy { it.category }
                .map { CategorySummary(it.key, it.value.sumOf { exp -> exp.amount }) }
                .sortedByDescending { it.totalAmount }
            return kotlinx.coroutines.flow.flowOf(summaries)
        }
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
        override fun countRecurringExpenseInPeriod(recurringId: Long, periodStart: Long, periodEnd: Long): Int =
            expenses.count { it.recurringExpenseId == recurringId && it.date >= periodStart && it.date < periodEnd }
    }

    class FakeRecurringExpenseDao : RecurringExpenseDao {
        val recurringExpenses = mutableListOf<RecurringExpense>()

        override fun insertRecurringExpense(recurringExpense: RecurringExpense) { recurringExpenses.add(recurringExpense) }
        override fun updateRecurringExpense(recurringExpense: RecurringExpense) {
            val index = recurringExpenses.indexOfFirst { it.id == recurringExpense.id }
            if (index != -1) recurringExpenses[index] = recurringExpense
        }
        override fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
            recurringExpenses.removeAll { it.id == recurringExpense.id }
        }
        override fun getRecurringExpenseById(id: Int): Flow<RecurringExpense?> = kotlinx.coroutines.flow.flowOf(recurringExpenses.find { it.id == id })
        override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> = kotlinx.coroutines.flow.flowOf(recurringExpenses)
        override fun getTotalActiveRecurringExpenses(): Flow<Double> = throw NotImplementedError()
        override fun getActiveRecurringExpensesList(): List<RecurringExpense> = recurringExpenses.filter { it.isActive }
    }

    class FakeMonthlyPlanDao : MonthlyPlanDao {
        val monthlyPlans = mutableListOf<MonthlyPlan>()

        override fun insertMonthlyPlan(monthlyPlan: MonthlyPlan) { monthlyPlans.add(monthlyPlan) }
        override fun updateMonthlyPlan(monthlyPlan: MonthlyPlan) {
            val index = monthlyPlans.indexOfFirst { it.id == monthlyPlan.id }
            if (index != -1) monthlyPlans[index] = monthlyPlan
        }
        override fun getMonthlyPlan(month: Int, year: Int): Flow<MonthlyPlan?> =
            kotlinx.coroutines.flow.flowOf(monthlyPlans.filter { it.month == month && it.year == year }.maxByOrNull { it.id })
        override fun getMonthlyPlanOnce(month: Int, year: Int): MonthlyPlan? =
            monthlyPlans.filter { it.month == month && it.year == year }.maxByOrNull { it.id }
        override fun getAllMonthlyPlans(): Flow<List<MonthlyPlan>> = kotlinx.coroutines.flow.flowOf(monthlyPlans)
    }

    @Test
    fun testCalculateAnalytics_runsSuccessfullyAndComputesCorrectValues() = runBlocking {
        val expenseDao = FakeExpenseDao()
        val recurringExpenseDao = FakeRecurringExpenseDao()
        val monthlyPlanDao = FakeMonthlyPlanDao()

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) // 0-11
        val currentYear = cal.get(Calendar.YEAR)

        // 1. Setup Budget for Current Month
        monthlyPlanDao.insertMonthlyPlan(
            MonthlyPlan(id = 1, month = currentMonth + 1, year = currentYear, monthlyAllowance = 5000.0, selectedPlans = emptyList(), notes = "")
        )

        // 2. Setup historical budgets (prior months)
        val priorCal = Calendar.getInstance()
        priorCal.add(Calendar.MONTH, -1)
        monthlyPlanDao.insertMonthlyPlan(
            MonthlyPlan(id = 2, month = priorCal.get(Calendar.MONTH) + 1, year = priorCal.get(Calendar.YEAR), monthlyAllowance = 4500.0, selectedPlans = emptyList(), notes = "")
        )

        // 3. Setup current month expenses
        val currentMs = System.currentTimeMillis()
        expenseDao.insertExpense(Expense(id = 1, amount = 1500.0, description = "Groceries", category = "Food", date = currentMs))
        expenseDao.insertExpense(Expense(id = 2, amount = 600.0, description = "Cab", category = "Transport", date = currentMs))

        // 4. Setup historical expenses (1 month prior)
        val priorMs = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.timeInMillis
        expenseDao.insertExpense(Expense(id = 3, amount = 1000.0, description = "Lunch", category = "Food", date = priorMs))
        expenseDao.insertExpense(Expense(id = 4, amount = 800.0, description = "Fuel", category = "Transport", date = priorMs))

        // 5. Setup active recurring expenses (templates)
        recurringExpenseDao.insertRecurringExpense(
            RecurringExpense(id = 10, name = "Rent", category = "Bills", amount = 1200.0, isActive = true)
        )

        // Run calculations
        val repository = AnalyticsRepository(expenseDao, recurringExpenseDao, monthlyPlanDao)
        val completeAnalytics = repository.completeAnalytics.first()

        // Assertions:
        assertNotNull(completeAnalytics)

        // Current Month Analytics Checks
        val currentMonthAnalytics = completeAnalytics.currentMonth
        assertEquals(2100.0, currentMonthAnalytics.totalSpent, 0.0)
        assertEquals(5000.0, currentMonthAnalytics.totalBudget, 0.0)
        assertEquals(2900.0, currentMonthAnalytics.remainingBudget, 0.0)
        assertEquals(42.0, currentMonthAnalytics.utilizationPercentage, 0.0)
        assertEquals(2, currentMonthAnalytics.spendingByCategory.size)
        assertEquals("Food", currentMonthAnalytics.spendingByCategory[0].category)
        assertEquals(1500.0, currentMonthAnalytics.spendingByCategory[0].amount, 0.0)
        assertEquals("Transport", currentMonthAnalytics.spendingByCategory[1].category)
        assertEquals(600.0, currentMonthAnalytics.spendingByCategory[1].amount, 0.0)

        // Category Analytics Checks (across entire history)
        // Two unique months overall (current month and 1 month prior)
        val catAnalytics = completeAnalytics.categoryAnalytics
        assertEquals(2, catAnalytics.size)
        
        val foodCat = catAnalytics.find { it.category == "Food" }!!
        assertEquals(2500.0, foodCat.totalSpent, 0.0)
        assertEquals(1250.0, foodCat.averageMonthlySpending, 0.0) // 2500 / 2 unique months
        assertEquals(2, foodCat.transactionCount)
        assertEquals(1500.0, foodCat.highestTransaction, 0.0)
        assertEquals(2500.0 / 3900.0 * 100.0, foodCat.percentageOfTotal, 0.001)

        // Category Trends Checks
        // 3-month average for Food = prior month (1000) + two other months (0) = 1000 / 3 = 333.333
        // Current month = 1500
        val foodTrend = completeAnalytics.categoryTrends.find { it.category == "Food" }!!
        assertEquals(333.333, foodTrend.threeMonthAverage, 0.001)
        assertEquals(1500.0, foodTrend.currentMonthSpending, 0.0)
        assertEquals(1166.666, foodTrend.difference, 0.001)
        assertEquals((1166.666 / 333.333) * 100.0, foodTrend.percentageChange, 0.001)

        // Recurring Expense Checks
        val recurringAnalytics = completeAnalytics.recurring
        assertEquals(1200.0, recurringAnalytics.totalActiveRecurring, 0.0)
        assertEquals(24.0, recurringAnalytics.budgetCommittedPercentage, 0.0) // 1200 / 5000 * 100.0
        assertEquals(1, recurringAnalytics.recurringByCategory.size)
        assertEquals("Bills", recurringAnalytics.recurringByCategory[0].category)
        assertEquals(1200.0, recurringAnalytics.recurringByCategory[0].amount, 0.0)

        // Spending Patterns Checks
        val patterns = completeAnalytics.spendingPattern
        assertEquals("Food", patterns.mostExpensiveCategory)
        assertEquals("Food", patterns.mostFrequentCategory) 
        assertEquals(1500.0, patterns.highestIndividualExpense, 0.0)
        assertEquals(975.0, patterns.averageTransactionAmount, 0.0) // (1500 + 600 + 1000 + 800) / 4 = 3900 / 4 = 975
        assertEquals(4, patterns.totalTransactions)
    }
}
