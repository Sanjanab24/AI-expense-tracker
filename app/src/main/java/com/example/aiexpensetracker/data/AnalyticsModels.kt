package com.example.aiexpensetracker.data

data class CurrentMonthAnalytics(
    val totalSpent: Double,
    val totalBudget: Double,
    val remainingBudget: Double,
    val utilizationPercentage: Double,
    val spendingByCategory: List<CategorySpend>
)

data class CategorySpend(
    val category: String,
    val amount: Double
)

data class CategoryAnalytics(
    val category: String,
    val totalSpent: Double,
    val averageMonthlySpending: Double,
    val transactionCount: Int,
    val highestTransaction: Double,
    val percentageOfTotal: Double
)

data class MonthlySpend(
    val monthName: String, // e.g. "February" or "Feb 2026"
    val year: Int,
    val month: Int, // 1-12
    val totalSpent: Double
)

data class HistoricalAnalytics(
    val last6MonthsSpending: List<MonthlySpend>,
    val averageMonthlySpending: Double,
    val highestSpendingMonth: MonthlySpend?,
    val lowestSpendingMonth: MonthlySpend?
)

data class CategoryTrend(
    val category: String,
    val threeMonthAverage: Double,
    val currentMonthSpending: Double,
    val difference: Double,
    val percentageChange: Double
)

data class RecurringExpenseAnalytics(
    val totalActiveRecurring: Double,
    val recurringByCategory: List<CategorySpend>,
    val budgetCommittedPercentage: Double
)

data class SpendingPattern(
    val mostExpensiveCategory: String,
    val mostFrequentCategory: String,
    val highestIndividualExpense: Double,
    val averageTransactionAmount: Double,
    val totalTransactions: Int
)

data class CompleteAnalytics(
    val currentMonth: CurrentMonthAnalytics,
    val categoryAnalytics: List<CategoryAnalytics>,
    val historical: HistoricalAnalytics,
    val categoryTrends: List<CategoryTrend>,
    val recurring: RecurringExpenseAnalytics,
    val spendingPattern: SpendingPattern
)
