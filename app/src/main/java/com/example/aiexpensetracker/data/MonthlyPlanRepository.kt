package com.example.aiexpensetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MonthlyPlanRepository(private val monthlyPlanDao: MonthlyPlanDao) {

    fun getMonthlyPlan(month: Int, year: Int): Flow<MonthlyPlan?> {
        return monthlyPlanDao.getMonthlyPlan(month, year)
    }

    suspend fun getMonthlyPlanOnce(month: Int, year: Int): MonthlyPlan? {
        return withContext(Dispatchers.IO) {
            monthlyPlanDao.getMonthlyPlanOnce(month, year)
        }
    }

    suspend fun insertMonthlyPlan(monthlyPlan: MonthlyPlan) {
        withContext(Dispatchers.IO) {
            monthlyPlanDao.insertMonthlyPlan(monthlyPlan)
        }
    }

    suspend fun updateMonthlyPlan(monthlyPlan: MonthlyPlan) {
        withContext(Dispatchers.IO) {
            monthlyPlanDao.updateMonthlyPlan(monthlyPlan)
        }
    }

    suspend fun insertOrUpdateMonthlyPlan(
        month: Int,
        year: Int,
        monthlyAllowance: Double,
        selectedPlans: List<String> = emptyList(),
        notes: String = ""
    ) {
        withContext(Dispatchers.IO) {
            val existing = monthlyPlanDao.getMonthlyPlanOnce(month, year)
            if (existing != null) {
                monthlyPlanDao.updateMonthlyPlan(
                    existing.copy(
                        monthlyAllowance = monthlyAllowance,
                        selectedPlans = if (selectedPlans.isNotEmpty()) selectedPlans else existing.selectedPlans,
                        notes = if (notes.isNotEmpty()) notes else existing.notes
                    )
                )
            } else {
                monthlyPlanDao.insertMonthlyPlan(
                    MonthlyPlan(
                        month = month,
                        year = year,
                        monthlyAllowance = monthlyAllowance,
                        selectedPlans = selectedPlans,
                        notes = notes
                    )
                )
            }
        }
    }
}
