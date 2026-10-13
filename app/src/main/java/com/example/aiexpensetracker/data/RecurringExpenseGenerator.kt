package com.example.aiexpensetracker.data

import java.util.Calendar

class RecurringExpenseGenerator(
    private val expenseDao: ExpenseDao,
    private val recurringExpenseDao: RecurringExpenseDao
) {
    suspend fun generateRecurringExpenses(targetTime: Long = System.currentTimeMillis()) {
        val activeRecurring = recurringExpenseDao.getActiveRecurringExpensesList()
        val cal = Calendar.getInstance().apply { timeInMillis = targetTime }
        val (startOfMonth, nextMonthStart) = DateUtils.getMonthRange(
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )

        for (recurring in activeRecurring) {
            // Check if this recurring expense template has already been generated for the current month
            val count = expenseDao.countRecurringExpenseInPeriod(
                recurringId = recurring.id.toLong(),
                periodStart = startOfMonth,
                periodEnd = nextMonthStart
            )
            if (count == 0) {
                val newExpense = Expense(
                    amount = recurring.amount,
                    description = recurring.name,
                    category = recurring.category,
                    date = targetTime,
                    isRecurring = true,
                    recurringExpenseId = recurring.id.toLong()
                )
                expenseDao.insertExpense(newExpense)
            }
        }
    }
}
