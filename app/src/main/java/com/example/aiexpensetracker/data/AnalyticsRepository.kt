package com.example.aiexpensetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsRepository(
    private val expenseDao: ExpenseDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val monthlyPlanDao: MonthlyPlanDao
) {
    val completeAnalytics: Flow<CompleteAnalytics> = combine(
        expenseDao.getAllExpenses(),
        recurringExpenseDao.getAllRecurringExpenses(),
        monthlyPlanDao.getAllMonthlyPlans()
    ) { expenses, recurringExpenses, monthlyPlans ->
        calculateAnalytics(expenses, recurringExpenses, monthlyPlans)
    }

    fun calculateAnalytics(
        expenses: List<Expense>,
        recurringExpenses: List<RecurringExpense>,
        monthlyPlans: List<MonthlyPlan>,
        currentTime: Long = System.currentTimeMillis()
    ): CompleteAnalytics {
        val cal = Calendar.getInstance()
        cal.timeInMillis = currentTime
        val currentMonth = cal.get(Calendar.MONTH) // 0-11
        val currentYear = cal.get(Calendar.YEAR)

        // 1. Current Month Analytics
        val (currentMonthStart, nextMonthStart) = DateUtils.getMonthRange(currentMonth + 1, currentYear)
        val currentMonthExpenses = expenses.filter { it.date >= currentMonthStart && it.date < nextMonthStart }
        val totalSpent = currentMonthExpenses.sumOf { it.amount }

        // MonthlyPlan month ranges from 1 to 12. Pick the latest matching plan if multiple exist.
        val currentMonthPlan = monthlyPlans.filter { it.month == currentMonth + 1 && it.year == currentYear }.maxByOrNull { it.id }
        val totalBudget = currentMonthPlan?.monthlyAllowance ?: 0.0
        val remainingBudget = totalBudget - totalSpent
        val utilizationPercentage = if (totalBudget > 0.0) (totalSpent / totalBudget) * 100.0 else 0.0

        val spendingByCategory = currentMonthExpenses.groupBy { it.category }
            .map { CategorySpend(it.key, it.value.sumOf { exp -> exp.amount }) }
            .sortedByDescending { it.amount }

        val currentMonthAnalytics = CurrentMonthAnalytics(
            totalSpent = totalSpent,
            totalBudget = totalBudget,
            remainingBudget = remainingBudget,
            utilizationPercentage = utilizationPercentage,
            spendingByCategory = spendingByCategory
        )

        // 2. Category Analytics (for entire history)
        val overallTotalSpent = expenses.sumOf { it.amount }

        // Find total unique months (Month+Year) across all expenses
        val uniqueMonthsCount = expenses.map {
            val c = Calendar.getInstance()
            c.timeInMillis = it.date
            "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}"
        }.distinct().size.coerceAtLeast(1)

        val categoriesInHistory = expenses.map { it.category }.distinct()
        val categoryAnalyticsList = categoriesInHistory.map { cat ->
            val catExpenses = expenses.filter { it.category == cat }
            val catTotalSpent = catExpenses.sumOf { it.amount }
            val catAverageMonthly = catTotalSpent / uniqueMonthsCount
            val catTransactionCount = catExpenses.size
            val catHighestTransaction = catExpenses.maxOfOrNull { it.amount } ?: 0.0
            val catPercentageOfTotal = if (overallTotalSpent > 0.0) (catTotalSpent / overallTotalSpent) * 100.0 else 0.0

            CategoryAnalytics(
                category = cat,
                totalSpent = catTotalSpent,
                averageMonthlySpending = catAverageMonthly,
                transactionCount = catTransactionCount,
                highestTransaction = catHighestTransaction,
                percentageOfTotal = catPercentageOfTotal
            )
        }.sortedByDescending { it.totalSpent }

        // 3. Historical Monthly Analytics (last 6 months)
        val last6Months = mutableListOf<MonthlySpend>()
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val tempCal = Calendar.getInstance()
        tempCal.timeInMillis = currentTime
        
        val targetMonths = mutableListOf<Triple<Int, Int, String>>() // month (0-11), year, monthName
        for (i in 0 until 6) {
            targetMonths.add(Triple(tempCal.get(Calendar.MONTH), tempCal.get(Calendar.YEAR), monthFormat.format(tempCal.time)))
            tempCal.add(Calendar.MONTH, -1)
        }
        targetMonths.reverse() // oldest to newest

        for ((m, y, name) in targetMonths) {
            val (startMs, nextMs) = DateUtils.getMonthRange(m + 1, y)
            val mExpenses = expenses.filter { it.date >= startMs && it.date < nextMs }
            val mTotalSpent = mExpenses.sumOf { it.amount }
            last6Months.add(MonthlySpend(name, y, m + 1, mTotalSpent))
        }

        val historicalAverageMonthlySpending = last6Months.map { it.totalSpent }.average().let { if (it.isNaN()) 0.0 else it }
        val highestSpendingMonth = last6Months.maxByOrNull { it.totalSpent }
        val lowestSpendingMonth = last6Months.minByOrNull { it.totalSpent }

        val historicalAnalytics = HistoricalAnalytics(
            last6MonthsSpending = last6Months,
            averageMonthlySpending = historicalAverageMonthlySpending,
            highestSpendingMonth = highestSpendingMonth,
            lowestSpendingMonth = lowestSpendingMonth
        )

        // 4. Category Trends
        val priorMonthsList = mutableListOf<Pair<Long, Long>>() // Pair of (startMs, nextMs)
        val priorCal = Calendar.getInstance()
        priorCal.timeInMillis = currentTime
        for (i in 0 until 3) {
            priorCal.add(Calendar.MONTH, -1)
            val range = DateUtils.getMonthRange(priorCal.get(Calendar.MONTH) + 1, priorCal.get(Calendar.YEAR))
            priorMonthsList.add(range)
        }

        val categoryTrends = categoriesInHistory.map { cat ->
            val catCurrentMonthSpending = currentMonthExpenses.filter { it.category == cat }.sumOf { it.amount }
            
            // Average of prior 3 months
            val priorSpendings = priorMonthsList.map { (start, next) ->
                expenses.filter { it.category == cat && it.date >= start && it.date < next }.sumOf { it.amount }
            }
            val threeMonthAvg = priorSpendings.average().let { if (it.isNaN()) 0.0 else it }
            val difference = catCurrentMonthSpending - threeMonthAvg
            val percentageChange = if (threeMonthAvg > 0.0) (difference / threeMonthAvg) * 100.0 else 0.0

            CategoryTrend(
                category = cat,
                threeMonthAverage = threeMonthAvg,
                currentMonthSpending = catCurrentMonthSpending,
                difference = difference,
                percentageChange = percentageChange
            )
        }.sortedByDescending { it.currentMonthSpending }

        // 5. Recurring Expense Analytics (treat templates only as committed commitments, actuals in expenses)
        val activeRecurring = recurringExpenses.filter { it.isActive }
        val totalActiveRecurring = activeRecurring.sumOf { it.amount }
        val recurringByCategory = activeRecurring.groupBy { it.category }
            .map { CategorySpend(it.key, it.value.sumOf { rec -> rec.amount }) }
            .sortedByDescending { it.amount }
        val budgetCommittedPercentage = if (totalBudget > 0.0) (totalActiveRecurring / totalBudget) * 100.0 else 0.0

        val recurringAnalytics = RecurringExpenseAnalytics(
            totalActiveRecurring = totalActiveRecurring,
            recurringByCategory = recurringByCategory,
            budgetCommittedPercentage = budgetCommittedPercentage
        )

        // 6. Spending Patterns
        val mostExpensiveCategory = categoryAnalyticsList.firstOrNull()?.category ?: "None"
        val mostFrequentCategory = categoryAnalyticsList.maxByOrNull { it.transactionCount }?.category ?: "None"
        val highestIndividualExpense = expenses.maxOfOrNull { it.amount } ?: 0.0
        val averageTransactionAmount = expenses.map { it.amount }.average().let { if (it.isNaN()) 0.0 else it }
        val totalTransactions = expenses.size

        val spendingPattern = SpendingPattern(
            mostExpensiveCategory = mostExpensiveCategory,
            mostFrequentCategory = mostFrequentCategory,
            highestIndividualExpense = highestIndividualExpense,
            averageTransactionAmount = averageTransactionAmount,
            totalTransactions = totalTransactions
        )

        return CompleteAnalytics(
            currentMonth = currentMonthAnalytics,
            categoryAnalytics = categoryAnalyticsList,
            historical = historicalAnalytics,
            categoryTrends = categoryTrends,
            recurring = recurringAnalytics,
            spendingPattern = spendingPattern
        )
    }
}
