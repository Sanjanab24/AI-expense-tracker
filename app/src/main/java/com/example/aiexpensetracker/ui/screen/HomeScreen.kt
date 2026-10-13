package com.example.aiexpensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aiexpensetracker.data.Expense
import com.example.aiexpensetracker.data.SortOrder
import com.example.aiexpensetracker.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    backendHealthViewModel: com.example.aiexpensetracker.ui.viewmodel.BackendHealthViewModel? = null,
    onAddExpenseClick: () -> Unit,
    onExpenseClick: (Int) -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val categorySummaries by viewModel.categorySummaries.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val currentMonthPlan by viewModel.currentMonthPlan.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    val remainingSpendableBudget by viewModel.remainingSpendableBudget.collectAsState()
    
    val budgetAmount = currentMonthPlan?.monthlyAllowance ?: 0.0
    // If the remainingSpendableBudget uses currentBudget instead of currentMonthPlan, we will just use it directly, 
    // or calculate it here if needed. Since the view model exposes it:
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

                // --- STEP 2 TEMPORARY BACKEND HEALTH CHECK (EASY TO REMOVE) ---
                if (backendHealthViewModel != null) {
                    val healthState by backendHealthViewModel.uiState.collectAsState()
                    Spacer(modifier = Modifier.height(16.dp))
                    com.example.aiexpensetracker.ui.components.BackendHealthCard(
                        uiState = healthState,
                        onCheckBackend = { backendHealthViewModel.checkHealth() }
                    )
                }
                // -----------------------------------------------------------------

                Spacer(modifier = Modifier.height(24.dp))
                Text("Spending by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(categorySummaries) { summary ->
                CategoryItem(summary.category, summary.totalAmount)
                HorizontalDivider()
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("Latest First") }, onClick = { viewModel.updateSortOrder(SortOrder.DATE_DESC); expanded = false })
                            DropdownMenuItem(text = { Text("Oldest First") }, onClick = { viewModel.updateSortOrder(SortOrder.DATE_ASC); expanded = false })
                            DropdownMenuItem(text = { Text("Highest Amount") }, onClick = { viewModel.updateSortOrder(SortOrder.AMOUNT_DESC); expanded = false })
                            DropdownMenuItem(text = { Text("Lowest Amount") }, onClick = { viewModel.updateSortOrder(SortOrder.AMOUNT_ASC); expanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("Search description or merchant") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val categories = listOf("All", "Food", "Transport", "Shopping", "Groceries", "Bills", "Subscriptions", "Entertainment", "Healthcare", "Education", "Others")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = categoryFilter == category,
                            onClick = { viewModel.updateCategoryFilter(category) },
                            label = { Text(category) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (expenses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses this month yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(expenses) { expense ->
                    ExpenseItem(expense, onClick = { onExpenseClick(expense.id) })
                    HorizontalDivider()
                }
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
                Column(horizontalAlignment = Alignment.End) {
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
fun ExpenseItem(expense: Expense, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
