package com.example.aiexpensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMonthlyPlan(monthlyPlan: MonthlyPlan)

    @Update
    fun updateMonthlyPlan(monthlyPlan: MonthlyPlan)

    @Query("SELECT * FROM monthly_plans WHERE month = :month AND year = :year ORDER BY id DESC LIMIT 1")
    fun getMonthlyPlan(month: Int, year: Int): Flow<MonthlyPlan?>

    @Query("SELECT * FROM monthly_plans WHERE month = :month AND year = :year ORDER BY id DESC LIMIT 1")
    fun getMonthlyPlanOnce(month: Int, year: Int): MonthlyPlan?

    @Query("SELECT * FROM monthly_plans")
    fun getAllMonthlyPlans(): Flow<List<MonthlyPlan>>
}
