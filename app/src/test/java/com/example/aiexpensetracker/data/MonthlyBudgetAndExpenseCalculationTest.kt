package com.example.aiexpensetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MonthlyBudgetAndExpenseCalculationTest {

    class InMemoryExpenseDao : ExpenseDao {
        val expenses = mutableListOf<Expense>()
        private var nextId = 1

        override fun insertExpense(expense: Expense) {
            val toInsert = if (expense.id == 0) expense.copy(id = nextId++) else expense
            expenses.add(toInsert)
        }

        override fun updateExpense(expense: Expense) {
            val index = expenses.indexOfFirst { it.id == expense.id }
            if (index != -1) {
                expenses[index] = expense
            }
        }

        override fun deleteExpense(expense: Expense) {
            expenses.removeAll { it.id == expense.id }
        }

        override fun getExpenseById(id: Int): Flow<Expense?> =
            kotlinx.coroutines.flow.flow { emit(expenses.find { it.id == id }) }

        override fun getAllExpenses(): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(expenses.toList()) }

        override fun getCategorySummaries(): Flow<List<CategorySummary>> =
            kotlinx.coroutines.flow.flow {
                val summaries = expenses.groupBy { it.category }
                    .map { CategorySummary(it.key, it.value.sumOf { exp -> exp.amount }) }
                    .sortedByDescending { it.totalAmount }
                emit(summaries)
            }

        override fun getTotalSpent(): Flow<Double> =
            kotlinx.coroutines.flow.flow { emit(expenses.sumOf { it.amount }) }

        override fun getTotalSpentInPeriod(startDate: Long, endDate: Long): Flow<Double> =
            kotlinx.coroutines.flow.flow {
                val total = expenses
                    .filter { it.date >= startDate && it.date < endDate }
                    .sumOf { it.amount }
                emit(total)
            }

        override fun getCategorySummariesInPeriod(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
            kotlinx.coroutines.flow.flow {
                val summaries = expenses
                    .filter { it.date >= startDate && it.date < endDate }
                    .groupBy { it.category }
                    .map { CategorySummary(it.key, it.value.sumOf { exp -> exp.amount }) }
                    .sortedByDescending { it.totalAmount }
                emit(summaries)
            }

        override fun insertBudget(budget: Budget) {}
        override fun getBudget(): Flow<Budget?> = kotlinx.coroutines.flow.flow { emit(null) }

        override fun getExpensesSortedByDateDesc(searchQuery: String, category: String): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(expenses.sortedByDescending { it.date }) }

        override fun getExpensesSortedByDateAsc(searchQuery: String, category: String): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(expenses.sortedBy { it.date }) }

        override fun getExpensesSortedByAmountDesc(searchQuery: String, category: String): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(expenses.sortedByDescending { it.amount }) }

        override fun getExpensesSortedByAmountAsc(searchQuery: String, category: String): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(expenses.sortedBy { it.amount }) }

        private fun filterExpensesInPeriod(
            startDate: Long,
            endDate: Long,
            searchQuery: String,
            category: String
        ): List<Expense> {
            return expenses.filter {
                it.date >= startDate && it.date < endDate &&
                (category == "All" || it.category == category) &&
                (it.description.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true))
            }
        }

        override fun getExpensesInPeriodSortedByDateDesc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(filterExpensesInPeriod(startDate, endDate, searchQuery, category).sortedByDescending { it.date }) }

        override fun getExpensesInPeriodSortedByDateAsc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(filterExpensesInPeriod(startDate, endDate, searchQuery, category).sortedBy { it.date }) }

        override fun getExpensesInPeriodSortedByAmountDesc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(filterExpensesInPeriod(startDate, endDate, searchQuery, category).sortedByDescending { it.amount }) }

        override fun getExpensesInPeriodSortedByAmountAsc(startDate: Long, endDate: Long, searchQuery: String, category: String): Flow<List<Expense>> =
            kotlinx.coroutines.flow.flow { emit(filterExpensesInPeriod(startDate, endDate, searchQuery, category).sortedBy { it.amount }) }

        override fun countRecurringExpenseInPeriod(recurringId: Long, periodStart: Long, periodEnd: Long): Int {
            return expenses.count {
                it.recurringExpenseId == recurringId && it.date >= periodStart && it.date < periodEnd
            }
        }
    }

    class InMemoryMonthlyPlanDao : MonthlyPlanDao {
        val plans = mutableListOf<MonthlyPlan>()
        private var nextId = 1

        override fun insertMonthlyPlan(monthlyPlan: MonthlyPlan) {
            val toInsert = if (monthlyPlan.id == 0) monthlyPlan.copy(id = nextId++) else monthlyPlan
            plans.add(toInsert)
        }

        override fun updateMonthlyPlan(monthlyPlan: MonthlyPlan) {
            val index = plans.indexOfFirst { it.id == monthlyPlan.id }
            if (index != -1) {
                plans[index] = monthlyPlan
            }
        }

        override fun getMonthlyPlan(month: Int, year: Int): Flow<MonthlyPlan?> {
            val plan = plans.filter { it.month == month && it.year == year }.maxByOrNull { it.id }
            return flowOf(plan)
        }

        override fun getMonthlyPlanOnce(month: Int, year: Int): MonthlyPlan? {
            return plans.filter { it.month == month && it.year == year }.maxByOrNull { it.id }
        }

        override fun getAllMonthlyPlans(): Flow<List<MonthlyPlan>> =
            flowOf(plans.toList())
    }

    class InMemoryCategoryBudgetDao : CategoryBudgetDao {
        val categoryBudgets = mutableListOf<CategoryBudget>()
        private var nextId = 1

        override fun insertCategoryBudget(categoryBudget: CategoryBudget) {
            val toInsert = if (categoryBudget.id == 0) categoryBudget.copy(id = nextId++) else categoryBudget
            categoryBudgets.add(toInsert)
        }

        override fun updateCategoryBudget(categoryBudget: CategoryBudget) {
            val index = categoryBudgets.indexOfFirst { it.id == categoryBudget.id }
            if (index != -1) {
                categoryBudgets[index] = categoryBudget
            }
        }

        override fun getCategoryBudget(month: Int, year: Int, category: String): CategoryBudget? {
            return categoryBudgets.find { it.month == month && it.year == year && it.category == category }
        }

        override fun getAllCategoryBudgetsForMonth(month: Int, year: Int): Flow<List<CategoryBudget>> {
            return flowOf(categoryBudgets.filter { it.month == month && it.year == year })
        }
    }

    class InMemoryRecurringExpenseDao : RecurringExpenseDao {
        val recurringExpenses = mutableListOf<RecurringExpense>()
        private var nextId = 1

        override fun insertRecurringExpense(recurringExpense: RecurringExpense) {
            val toInsert = if (recurringExpense.id == 0) recurringExpense.copy(id = nextId++) else recurringExpense
            recurringExpenses.add(toInsert)
        }

        override fun updateRecurringExpense(recurringExpense: RecurringExpense) {
            val index = recurringExpenses.indexOfFirst { it.id == recurringExpense.id }
            if (index != -1) {
                recurringExpenses[index] = recurringExpense
            }
        }

        override fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
            recurringExpenses.removeAll { it.id == recurringExpense.id }
        }

        override fun getRecurringExpenseById(id: Int): Flow<RecurringExpense?> =
            flowOf(recurringExpenses.find { it.id == id })

        override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> =
            flowOf(recurringExpenses.toList())

        override fun getTotalActiveRecurringExpenses(): Flow<Double> =
            flowOf(recurringExpenses.filter { it.isActive }.sumOf { it.amount })

        override fun getActiveRecurringExpensesList(): List<RecurringExpense> =
            recurringExpenses.filter { it.isActive }
    }

    private fun makeTimestamp(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun testDateUtils_monthRange_startInclusive_endExclusive() {
        val (augStart, augEnd) = DateUtils.getMonthRange(8, 2026)
        val (sepStart, sepEnd) = DateUtils.getMonthRange(9, 2026)

        // August 1, 2026 00:00:00.000 to September 1, 2026 00:00:00.000
        assertEquals(augEnd, sepStart)

        val aug31_2359 = makeTimestamp(2026, 8, 31, 23, 59)
        assertTrue(aug31_2359 >= augStart && aug31_2359 < augEnd)
        assertFalse(aug31_2359 >= sepStart)

        val sep1_0000 = makeTimestamp(2026, 9, 1, 0, 0)
        assertFalse(sep1_0000 < augEnd)
        assertTrue(sep1_0000 >= sepStart && sep1_0000 < sepEnd)
    }

    @Test
    fun testDateUtils_februaryLeapYearAndNonLeapYear() {
        // Leap year 2024: Feb has 29 days
        val (feb2024Start, feb2024End) = DateUtils.getMonthRange(2, 2024)
        val feb29_2024 = makeTimestamp(2024, 2, 29, 15, 0)
        assertTrue(feb29_2024 >= feb2024Start && feb29_2024 < feb2024End)

        // Non-leap year 2026: Feb has 28 days
        val (feb2026Start, feb2026End) = DateUtils.getMonthRange(2, 2026)
        val feb28_2026 = makeTimestamp(2026, 2, 28, 23, 59)
        val mar1_2026 = makeTimestamp(2026, 3, 1, 0, 0)
        assertTrue(feb28_2026 >= feb2026Start && feb28_2026 < feb2026End)
        assertEquals(feb2026End, mar1_2026)
    }

    @Test
    fun testStep2Scenario_consecutiveMonths_augustAndSeptember2026() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val monthlyPlanDao = InMemoryMonthlyPlanDao()
        val categoryBudgetDao = InMemoryCategoryBudgetDao()

        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)
        val monthlyPlanRepo = MonthlyPlanRepository(monthlyPlanDao)
        val categoryBudgetRepo = CategoryBudgetRepository(categoryBudgetDao)
        val analyticsRepo = AnalyticsRepository(expenseDao, recurringExpenseDao, monthlyPlanDao)

        // August 2026: Budget ₹5,000, Expenses ₹3,200
        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 8, year = 2026, monthlyAllowance = 5000.0)
        val aug10 = makeTimestamp(2026, 8, 10)
        val aug25 = makeTimestamp(2026, 8, 25)
        expenseRepo.insertExpense(Expense(amount = 2000.0, description = "Groceries", category = "Food", date = aug10))
        expenseRepo.insertExpense(Expense(amount = 1200.0, description = "Train ticket", category = "Transport", date = aug25))

        // September 2026: Budget ₹7,000, Expenses ₹2,000
        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 9, year = 2026, monthlyAllowance = 7000.0)
        val sep5 = makeTimestamp(2026, 9, 5)
        val sep18 = makeTimestamp(2026, 9, 18)
        expenseRepo.insertExpense(Expense(amount = 1500.0, description = "Clothes", category = "Shopping", date = sep5))
        expenseRepo.insertExpense(Expense(amount = 500.0, description = "Electricity", category = "Bills", date = sep18))

        // 1. Verify August 2026 calculations
        val (augStart, augEnd) = DateUtils.getMonthRange(8, 2026)
        val augPlan = monthlyPlanRepo.getMonthlyPlan(8, 2026).first()
        val augSpent = expenseRepo.getTotalSpentInPeriod(augStart, augEnd).first()
        assertNotNull(augPlan)
        assertEquals(5000.0, augPlan!!.monthlyAllowance, 0.0)
        assertEquals(3200.0, augSpent, 0.0)
        assertEquals(1800.0, augPlan.monthlyAllowance - augSpent, 0.0)

        // 2. Verify September 2026 calculations (August expenses EXCLUDED from September total)
        val (sepStart, sepEnd) = DateUtils.getMonthRange(9, 2026)
        val sepPlan = monthlyPlanRepo.getMonthlyPlan(9, 2026).first()
        val sepSpent = expenseRepo.getTotalSpentInPeriod(sepStart, sepEnd).first()
        assertNotNull(sepPlan)
        assertEquals(7000.0, sepPlan!!.monthlyAllowance, 0.0)
        assertEquals(2000.0, sepSpent, 0.0)
        assertEquals(5000.0, sepPlan.monthlyAllowance - sepSpent, 0.0)

        // 3. Verify AnalyticsRepository computes matching results for September 2026
        val sepAnalytics = analyticsRepo.calculateAnalytics(
            expenses = expenseDao.expenses,
            recurringExpenses = recurringExpenseDao.recurringExpenses,
            monthlyPlans = monthlyPlanDao.plans,
            currentTime = makeTimestamp(2026, 9, 15)
        )
        assertEquals(7000.0, sepAnalytics.currentMonth.totalBudget, 0.0)
        assertEquals(2000.0, sepAnalytics.currentMonth.totalSpent, 0.0)
        assertEquals(5000.0, sepAnalytics.currentMonth.remainingBudget, 0.0)
        assertEquals((2000.0 / 7000.0) * 100.0, sepAnalytics.currentMonth.utilizationPercentage, 0.001)

        // Verify August expenses excluded from September analytics
        assertEquals(2, sepAnalytics.currentMonth.spendingByCategory.size)
        assertEquals("Shopping", sepAnalytics.currentMonth.spendingByCategory[0].category)
        assertEquals(1500.0, sepAnalytics.currentMonth.spendingByCategory[0].amount, 0.0)
        assertEquals("Bills", sepAnalytics.currentMonth.spendingByCategory[1].category)
        assertEquals(500.0, sepAnalytics.currentMonth.spendingByCategory[1].amount, 0.0)
    }

    @Test
    fun testChangingSeptemberBudget_doesNotOverwriteAugustBudget() = runBlocking {
        val monthlyPlanDao = InMemoryMonthlyPlanDao()
        val monthlyPlanRepo = MonthlyPlanRepository(monthlyPlanDao)

        // Set August budget
        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 8, year = 2026, monthlyAllowance = 5000.0)
        // Set September budget
        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 9, year = 2026, monthlyAllowance = 7000.0)

        // Update September budget to ₹8,500
        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 9, year = 2026, monthlyAllowance = 8500.0)

        // August budget MUST remain ₹5,000
        val augPlan = monthlyPlanRepo.getMonthlyPlan(8, 2026).first()
        assertNotNull(augPlan)
        assertEquals(5000.0, augPlan!!.monthlyAllowance, 0.0)

        // September budget is updated to ₹8,500
        val sepPlan = monthlyPlanRepo.getMonthlyPlan(9, 2026).first()
        assertNotNull(sepPlan)
        assertEquals(8500.0, sepPlan!!.monthlyAllowance, 0.0)
    }

    @Test
    fun testDecemberToJanuaryTransition() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val monthlyPlanDao = InMemoryMonthlyPlanDao()

        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)
        val monthlyPlanRepo = MonthlyPlanRepository(monthlyPlanDao)

        // Dec 2026: Budget ₹10,000, Expenses ₹8,000
        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 12, year = 2026, monthlyAllowance = 10000.0)
        val dec15 = makeTimestamp(2026, 12, 15)
        val dec31_end = makeTimestamp(2026, 12, 31, 23, 59)
        expenseRepo.insertExpense(Expense(amount = 5000.0, description = "Rent", category = "Bills", date = dec15))
        expenseRepo.insertExpense(Expense(amount = 3000.0, description = "New Year Eve", category = "Entertainment", date = dec31_end))

        // Jan 2027: Budget ₹12,000, Expenses ₹3,000
        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 1, year = 2027, monthlyAllowance = 12000.0)
        val jan1_start = makeTimestamp(2027, 1, 1, 0, 0)
        expenseRepo.insertExpense(Expense(amount = 3000.0, description = "New Year Gift", category = "Shopping", date = jan1_start))

        // December calculation
        val (decStart, decEnd) = DateUtils.getMonthRange(12, 2026)
        val decSpent = expenseRepo.getTotalSpentInPeriod(decStart, decEnd).first()
        val decPlan = monthlyPlanRepo.getMonthlyPlan(12, 2026).first()!!
        assertEquals(8000.0, decSpent, 0.0)
        assertEquals(2000.0, decPlan.monthlyAllowance - decSpent, 0.0)

        // January calculation
        val (janStart, janEnd) = DateUtils.getMonthRange(1, 2027)
        val janSpent = expenseRepo.getTotalSpentInPeriod(janStart, janEnd).first()
        val janPlan = monthlyPlanRepo.getMonthlyPlan(1, 2027).first()!!
        assertEquals(3000.0, janSpent, 0.0)
        assertEquals(9000.0, janPlan.monthlyAllowance - janSpent, 0.0)

        // Jan 1 00:00:00 is excluded from December and included in January
        assertFalse(jan1_start < decEnd)
        assertTrue(jan1_start >= janStart && jan1_start < janEnd)
    }

    @Test
    fun testCategorySpendingByMonth_andRemainingCategoryBudget() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val categoryBudgetDao = InMemoryCategoryBudgetDao()

        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)
        val categoryBudgetRepo = CategoryBudgetRepository(categoryBudgetDao)

        // August 2026: Allocate ₹3,000 to Food
        categoryBudgetRepo.insertOrUpdateCategoryBudget(month = 8, year = 2026, category = "Food", allocatedAmount = 3000.0)
        expenseRepo.insertExpense(Expense(amount = 2000.0, description = "Dinner", category = "Food", date = makeTimestamp(2026, 8, 12)))

        // September 2026: Allocate ₹2,500 to Food
        categoryBudgetRepo.insertOrUpdateCategoryBudget(month = 9, year = 2026, category = "Food", allocatedAmount = 2500.0)
        expenseRepo.insertExpense(Expense(amount = 500.0, description = "Lunch", category = "Food", date = makeTimestamp(2026, 9, 10)))

        // August category summaries
        val (augStart, augEnd) = DateUtils.getMonthRange(8, 2026)
        val augCategories = expenseRepo.getCategorySummariesInPeriod(augStart, augEnd).first()
        val augFoodSpent = augCategories.find { it.category == "Food" }?.totalAmount ?: 0.0
        val augFoodBudget = categoryBudgetRepo.getAllCategoryBudgetsForMonth(8, 2026).first().find { it.category == "Food" }!!.allocatedAmount
        assertEquals(2000.0, augFoodSpent, 0.0)
        assertEquals(1000.0, augFoodBudget - augFoodSpent, 0.0)

        // September category summaries (August Food spending MUST be excluded!)
        val (sepStart, sepEnd) = DateUtils.getMonthRange(9, 2026)
        val sepCategories = expenseRepo.getCategorySummariesInPeriod(sepStart, sepEnd).first()
        val sepFoodSpent = sepCategories.find { it.category == "Food" }?.totalAmount ?: 0.0
        val sepFoodBudget = categoryBudgetRepo.getAllCategoryBudgetsForMonth(9, 2026).first().find { it.category == "Food" }!!.allocatedAmount
        assertEquals(500.0, sepFoodSpent, 0.0)
        assertEquals(2000.0, sepFoodBudget - sepFoodSpent, 0.0)
    }

    @Test
    fun testEditingAndDeletingExpense_updatesMonthlySpendingAndRemaining() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val monthlyPlanDao = InMemoryMonthlyPlanDao()

        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)
        val monthlyPlanRepo = MonthlyPlanRepository(monthlyPlanDao)

        // August: Budget ₹5,000
        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 8, year = 2026, monthlyAllowance = 5000.0)
        val (augStart, augEnd) = DateUtils.getMonthRange(8, 2026)

        val exp1 = Expense(id = 1, amount = 1000.0, description = "Food item", category = "Food", date = makeTimestamp(2026, 8, 5))
        val exp2 = Expense(id = 2, amount = 2200.0, description = "Transport item", category = "Transport", date = makeTimestamp(2026, 8, 15))
        expenseRepo.insertExpense(exp1)
        expenseRepo.insertExpense(exp2)

        // Initial check: spent ₹3,200, remaining ₹1,800
        var spent = expenseRepo.getTotalSpentInPeriod(augStart, augEnd).first()
        var plan = monthlyPlanRepo.getMonthlyPlan(8, 2026).first()!!
        assertEquals(3200.0, spent, 0.0)
        assertEquals(1800.0, plan.monthlyAllowance - spent, 0.0)

        // Edit Expense 1: Change amount from ₹1,000 to ₹1,500
        expenseRepo.updateExpense(exp1.copy(amount = 1500.0))
        spent = expenseRepo.getTotalSpentInPeriod(augStart, augEnd).first()
        assertEquals(3700.0, spent, 0.0)
        assertEquals(1300.0, plan.monthlyAllowance - spent, 0.0)

        // Delete Expense 2
        expenseRepo.deleteExpense(exp2)
        spent = expenseRepo.getTotalSpentInPeriod(augStart, augEnd).first()
        assertEquals(1500.0, spent, 0.0)
        assertEquals(3500.0, plan.monthlyAllowance - spent, 0.0)
    }

    @Test
    fun testRecurringExpenses_countedExactlyOnceInMonth_andNotDuplicatedOnRestart() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val generator = RecurringExpenseGenerator(expenseDao, recurringExpenseDao)

        // Active template: ₹1,200
        recurringExpenseDao.insertRecurringExpense(
            RecurringExpense(id = 1, name = "Gym", category = "Healthcare", amount = 1200.0, isActive = true)
        )

        // Generate for August 2026
        val augTime = makeTimestamp(2026, 8, 1)
        generator.generateRecurringExpenses(targetTime = augTime)

        val (augStart, augEnd) = DateUtils.getMonthRange(8, 2026)
        val augSpent = expenseDao.getTotalSpentInPeriod(augStart, augEnd).first()
        assertEquals(1200.0, augSpent, 0.0)
        assertEquals(1, expenseDao.expenses.size)

        // Simulate app restart / regeneration in the same month (August 2026)
        generator.generateRecurringExpenses(targetTime = makeTimestamp(2026, 8, 15))
        // Must NOT duplicate transactions
        assertEquals(1, expenseDao.expenses.size)
        assertEquals(1200.0, expenseDao.getTotalSpentInPeriod(augStart, augEnd).first(), 0.0)

        // Generate for September 2026
        val sepTime = makeTimestamp(2026, 9, 1)
        generator.generateRecurringExpenses(targetTime = sepTime)
        // Now exactly 2 transactions total (1 in Aug, 1 in Sep)
        assertEquals(2, expenseDao.expenses.size)

        val (sepStart, sepEnd) = DateUtils.getMonthRange(9, 2026)
        assertEquals(1200.0, expenseDao.getTotalSpentInPeriod(sepStart, sepEnd).first(), 0.0)
        assertEquals(1200.0, expenseDao.getTotalSpentInPeriod(augStart, augEnd).first(), 0.0)
    }

    @Test
    fun testMonthWithNoExpenses() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val monthlyPlanDao = InMemoryMonthlyPlanDao()

        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)
        val monthlyPlanRepo = MonthlyPlanRepository(monthlyPlanDao)

        monthlyPlanRepo.insertOrUpdateMonthlyPlan(month = 10, year = 2026, monthlyAllowance = 6000.0)
        val (start, end) = DateUtils.getMonthRange(10, 2026)

        val spent = expenseRepo.getTotalSpentInPeriod(start, end).first()
        val plan = monthlyPlanRepo.getMonthlyPlan(10, 2026).first()!!
        val categories = expenseRepo.getCategorySummariesInPeriod(start, end).first()

        assertEquals(0.0, spent, 0.0)
        assertEquals(6000.0, plan.monthlyAllowance - spent, 0.0)
        assertTrue(categories.isEmpty())
    }

    @Test
    fun testMonthWithNoSavedBudget() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val monthlyPlanDao = InMemoryMonthlyPlanDao()

        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)
        val monthlyPlanRepo = MonthlyPlanRepository(monthlyPlanDao)

        // Add expense for November 2026 without setting any budget
        expenseRepo.insertExpense(Expense(amount = 2500.0, description = "Groceries", category = "Food", date = makeTimestamp(2026, 11, 10)))

        val (start, end) = DateUtils.getMonthRange(11, 2026)
        val plan = monthlyPlanRepo.getMonthlyPlan(11, 2026).first()
        val spent = expenseRepo.getTotalSpentInPeriod(start, end).first()

        val budgetAmount = plan?.monthlyAllowance ?: 0.0
        val remaining = budgetAmount - spent

        assertEquals(0.0, budgetAmount, 0.0)
        assertEquals(2500.0, spent, 0.0)
        assertEquals(-2500.0, remaining, 0.0)
    }

    @Test
    fun testHomeScreen_onlyDisplaysCurrentMonthExpenses_excludesPastMonths() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)

        // Historical expenses from August and September 2026
        expenseRepo.insertExpense(Expense(amount = 3200.0, description = "Aug Vacation", category = "Entertainment", date = makeTimestamp(2026, 8, 15)))
        expenseRepo.insertExpense(Expense(amount = 2000.0, description = "Sep Rent", category = "Bills", date = makeTimestamp(2026, 9, 10)))

        // Current month (October 2026) expenses
        val oct5 = makeTimestamp(2026, 10, 5)
        val oct20 = makeTimestamp(2026, 10, 20)
        expenseRepo.insertExpense(Expense(amount = 1500.0, description = "Oct Groceries", category = "Food", date = oct5))
        expenseRepo.insertExpense(Expense(amount = 700.0, description = "Oct Fuel", category = "Transport", date = oct20))

        val (octStart, octEnd) = DateUtils.getMonthRange(10, 2026)

        // Query filtered expenses for October 2026 (DATE_DESC)
        val octExpenses = expenseRepo.getFilteredExpensesInPeriod(
            startDate = octStart,
            endDate = octEnd,
            searchQuery = "",
            category = "All",
            sortOrder = SortOrder.DATE_DESC
        ).first()

        // 1. Exactly 2 expenses from October appear
        assertEquals(2, octExpenses.size)
        assertEquals("Oct Fuel", octExpenses[0].description)
        assertEquals("Oct Groceries", octExpenses[1].description)

        // 2. August and September expenses are completely excluded
        assertTrue(octExpenses.none { it.description.contains("Aug") || it.description.contains("Sep") })

        // 3. October monthly total excludes past months
        val octTotalSpent = expenseRepo.getTotalSpentInPeriod(octStart, octEnd).first()
        assertEquals(2200.0, octTotalSpent, 0.0)

        // 4. Past expenses are still safely preserved in database
        val allExpenses = expenseRepo.allExpenses.first()
        assertEquals(4, allExpenses.size)
    }

    @Test
    fun testHomeScreen_searchAndCategoryFilters_operateOnCurrentMonthExpensesOnly() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)

        // September has Food "Pizza" (1000)
        expenseRepo.insertExpense(Expense(amount = 1000.0, description = "Pizza Party", category = "Food", date = makeTimestamp(2026, 9, 20)))

        // October has Food "Pizza" (500) and Transport "Uber" (300)
        expenseRepo.insertExpense(Expense(amount = 500.0, description = "Personal Pizza", category = "Food", date = makeTimestamp(2026, 10, 12)))
        expenseRepo.insertExpense(Expense(amount = 300.0, description = "Uber Ride", category = "Transport", date = makeTimestamp(2026, 10, 14)))

        val (octStart, octEnd) = DateUtils.getMonthRange(10, 2026)

        // Search "Pizza" in October 2026
        val searchResults = expenseRepo.getFilteredExpensesInPeriod(
            startDate = octStart,
            endDate = octEnd,
            searchQuery = "Pizza",
            category = "All",
            sortOrder = SortOrder.DATE_DESC
        ).first()

        // Must only return October's Pizza (500), NOT September's (1000)
        assertEquals(1, searchResults.size)
        assertEquals("Personal Pizza", searchResults[0].description)
        assertEquals(500.0, searchResults[0].amount, 0.0)

        // Filter Category "Food" in October 2026
        val foodResults = expenseRepo.getFilteredExpensesInPeriod(
            startDate = octStart,
            endDate = octEnd,
            searchQuery = "",
            category = "Food",
            sortOrder = SortOrder.DATE_DESC
        ).first()

        assertEquals(1, foodResults.size)
        assertEquals(500.0, foodResults[0].amount, 0.0)

        // Filter Category "Transport" in October 2026
        val transportResults = expenseRepo.getFilteredExpensesInPeriod(
            startDate = octStart,
            endDate = octEnd,
            searchQuery = "",
            category = "Transport",
            sortOrder = SortOrder.DATE_DESC
        ).first()

        assertEquals(1, transportResults.size)
        assertEquals("Uber Ride", transportResults[0].description)
    }

    @Test
    fun testHomeScreen_emptyStateWhenNoExpensesInCurrentMonth() = runBlocking {
        val expenseDao = InMemoryExpenseDao()
        val recurringExpenseDao = InMemoryRecurringExpenseDao()
        val expenseRepo = ExpenseRepository(expenseDao, recurringExpenseDao)

        // Add expenses in August and September only
        expenseRepo.insertExpense(Expense(amount = 1500.0, description = "Old expense 1", category = "Food", date = makeTimestamp(2026, 8, 10)))
        expenseRepo.insertExpense(Expense(amount = 800.0, description = "Old expense 2", category = "Transport", date = makeTimestamp(2026, 9, 15)))

        val (octStart, octEnd) = DateUtils.getMonthRange(10, 2026)

        // Query October 2026 expenses
        val octExpenses = expenseRepo.getFilteredExpensesInPeriod(
            startDate = octStart,
            endDate = octEnd,
            searchQuery = "",
            category = "All",
            sortOrder = SortOrder.DATE_DESC
        ).first()

        // Current month's expense list is completely empty -> triggers "No expenses this month yet" empty state
        assertTrue(octExpenses.isEmpty())

        // Current month's total spent is 0.0
        val octTotalSpent = expenseRepo.getTotalSpentInPeriod(octStart, octEnd).first()
        assertEquals(0.0, octTotalSpent, 0.0)

        // Previous months remain intact in database
        val allExpenses = expenseRepo.allExpenses.first()
        assertEquals(2, allExpenses.size)
    }
}
