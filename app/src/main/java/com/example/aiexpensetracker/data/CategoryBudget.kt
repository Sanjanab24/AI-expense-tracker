package com.example.aiexpensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: Int,
    val year: Int,
    val category: String,
    val allocatedAmount: Double
)
