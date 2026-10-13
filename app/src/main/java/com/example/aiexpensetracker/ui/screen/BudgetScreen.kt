package com.example.aiexpensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.aiexpensetracker.ui.viewmodel.CategoryBudgetViewModel
import com.example.aiexpensetracker.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: ExpenseViewModel, 
    categoryBudgetViewModel: CategoryBudgetViewModel,
    onNavigateToCategoryBudgets: () -> Unit
) {
    var budgetInput by remember { mutableStateOf("") }
    val totalSpent by viewModel.totalSpent.collectAsState()
    val categorySummaries by viewModel.categorySummaries.collectAsState()
    val categoryBudgets by categoryBudgetViewModel.categoryBudgets.collectAsState()
    val currentMonthPlan by viewModel.currentMonthPlan.collectAsState()

    val budgetAmount = currentMonthPlan?.monthlyAllowance ?: 0.0
    val spent = totalSpent
    val remaining = budgetAmount - spent

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Overview", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onNavigateToCategoryBudgets) {
                        Text("Allocate Categories")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Set Monthly Budget", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = budgetInput,
                                onValueChange = { budgetInput = it },
                                label = { Text("Budget Amount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    budgetInput.toDoubleOrNull()?.let {
                                        viewModel.setBudget(it)
                                        budgetInput = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Budget")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text("Budget Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Budget", style = MaterialTheme.typography.bodyLarge)
                        Text("₹${budgetAmount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Spent", style = MaterialTheme.typography.bodyLarge)
                        Text("₹${spent}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Remaining", style = MaterialTheme.typography.bodyLarge)
                        Text("₹${remaining}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Category Utilization", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                items(categoryBudgets) { categoryBudget ->
                    val spentInCategory = categorySummaries.find { it.category == categoryBudget.category }?.totalAmount ?: 0.0
                    val remainingCategory = categoryBudget.allocatedAmount - spentInCategory
                    val utilization = if (categoryBudget.allocatedAmount > 0) {
                        (spentInCategory / categoryBudget.allocatedAmount * 100).coerceAtMost(100.0)
                    } else {
                        0.0
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(categoryBudget.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("₹${categoryBudget.allocatedAmount}", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Spent: ₹$spentInCategory", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                Text("Remaining: ₹$remainingCategory", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (utilization / 100).toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (utilization >= 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${String.format("%.1f", utilization)}% Utilized", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
