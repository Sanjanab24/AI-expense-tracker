package com.example.aiexpensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aiexpensetracker.data.Expense
import com.example.aiexpensetracker.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ExpenseViewModel, onAddExpenseClick: () -> Unit) {
    val expenses by viewModel.expenses.collectAsState()
    val categorySummaries by viewModel.categorySummaries.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val currentBudget by viewModel.currentBudget.collectAsState()

    val budgetAmount = currentBudget?.monthlyBudget ?: 0.0
    val remaining = budgetAmount - totalSpent

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AI Budget Tracker", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpenseClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                BudgetSummaryCard(budgetAmount, totalSpent, remaining)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Spending by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(categorySummaries) { summary ->
                CategoryItem(summary.category, summary.totalAmount)
                Divider()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(expenses) { expense ->
                ExpenseItem(expense)
                Divider()
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
            }
        }
    }
}

@Composable
fun BudgetSummaryCard(budget: Double, spent: Double, remaining: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Current Monthly Budget", style = MaterialTheme.typography.titleMedium)
            Text("₹${budget}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Spent", style = MaterialTheme.typography.bodyMedium)
                    Text("₹${spent}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
                Column {
                    Text("Remaining Balance", style = MaterialTheme.typography.bodyMedium)
                    Text("₹${remaining}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun CategoryItem(category: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(category, style = MaterialTheme.typography.bodyLarge)
        Text("₹${amount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ExpenseItem(expense: Expense) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(expense.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("${expense.category} • ${dateFormat.format(Date(expense.date))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("₹${expense.amount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
