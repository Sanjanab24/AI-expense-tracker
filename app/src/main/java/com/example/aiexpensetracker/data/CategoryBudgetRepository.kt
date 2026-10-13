package com.example.aiexpensetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryBudgetRepository(private val categoryBudgetDao: CategoryBudgetDao) {

    suspend fun insertOrUpdateCategoryBudget(month: Int, year: Int, category: String, allocatedAmount: Double) {
        withContext(Dispatchers.IO) {
            val existing = categoryBudgetDao.getCategoryBudget(month, year, category)
            if (existing != null) {
                categoryBudgetDao.updateCategoryBudget(existing.copy(allocatedAmount = allocatedAmount))
            } else {
                categoryBudgetDao.insertCategoryBudget(
                    CategoryBudget(month = month, year = year, category = category, allocatedAmount = allocatedAmount)
                )
            }
        }
    }

    fun getAllCategoryBudgetsForMonth(month: Int, year: Int): Flow<List<CategoryBudget>> {
        return categoryBudgetDao.getAllCategoryBudgetsForMonth(month, year)
    }
}
