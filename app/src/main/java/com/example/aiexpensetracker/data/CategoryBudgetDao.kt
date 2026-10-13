package com.example.aiexpensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategoryBudget(categoryBudget: CategoryBudget)

    @Update
    fun updateCategoryBudget(categoryBudget: CategoryBudget)

    @Query("SELECT * FROM category_budgets WHERE month = :month AND year = :year AND category = :category LIMIT 1")
    fun getCategoryBudget(month: Int, year: Int, category: String): CategoryBudget?

    @Query("SELECT * FROM category_budgets WHERE month = :month AND year = :year")
    fun getAllCategoryBudgetsForMonth(month: Int, year: Int): Flow<List<CategoryBudget>>
}
