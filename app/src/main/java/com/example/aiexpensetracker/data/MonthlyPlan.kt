package com.example.aiexpensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_plans")
data class MonthlyPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val month: Int,
    val year: Int,
    val monthlyAllowance: Double,
    val selectedPlans: List<String>,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)
