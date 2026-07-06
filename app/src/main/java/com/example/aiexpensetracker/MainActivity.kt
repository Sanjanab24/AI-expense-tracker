package com.example.aiexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.aiexpensetracker.data.AppDatabase
import com.example.aiexpensetracker.data.ExpenseRepository
import com.example.aiexpensetracker.ui.navigation.AppNavGraph
import com.example.aiexpensetracker.ui.theme.AIExpenseTrackerTheme
import com.example.aiexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.aiexpensetracker.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = ExpenseRepository(database.expenseDao())
        val viewModelFactory = ViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ExpenseViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            AIExpenseTrackerTheme {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }
}