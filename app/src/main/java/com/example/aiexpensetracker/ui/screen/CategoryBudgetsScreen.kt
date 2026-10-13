package com.example.aiexpensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.aiexpensetracker.ui.viewmodel.CategoryBudgetViewModel
import com.example.aiexpensetracker.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBudgetsScreen(
    categoryBudgetViewModel: CategoryBudgetViewModel,
    expenseViewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit
) {
    val categoryBudgets by categoryBudgetViewModel.categoryBudgets.collectAsState()
    
    val currentMonthPlan by expenseViewModel.currentMonthPlan.collectAsState()
    val totalMonthlyBudget = currentMonthPlan?.monthlyAllowance ?: 0.0
    val totalAllocated = categoryBudgets.sumOf { it.allocatedAmount }
    val remainingBudget = totalMonthlyBudget - totalAllocated

    val showWarning = totalAllocated > totalMonthlyBudget

    val categories = listOf(
        "Food", "Transport", "Shopping", "Groceries", "Bills", 
        "Subscriptions", "Entertainment", "Healthcare", "Education", "Others"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Category Budgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Monthly Budget: ₹$totalMonthlyBudget", style = MaterialTheme.typography.titleMedium)
                    Text("Allocated Budget: ₹$totalAllocated", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Remaining Budget: ₹$remainingBudget", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (remainingBudget < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showWarning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Warning: Allocated budget exceeds total monthly budget!",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(categories) { category ->
                    val budgetForCategory = categoryBudgets.find { it.category == category }
                    var amountInput by remember(budgetForCategory) { 
                        mutableStateOf(budgetForCategory?.allocatedAmount?.toString() ?: "") 
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(category, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(120.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                val amount = amountInput.toDoubleOrNull() ?: 0.0
                                val previousAmount = budgetForCategory?.allocatedAmount ?: 0.0
                                val newTotalAllocated = totalAllocated - previousAmount + amount
                                
                                if (newTotalAllocated > totalMonthlyBudget) {
                                    // Normally show a snackbar or dialog, but for now we'll just allow it 
                                    // because the warning banner covers the validation UI requirement,
                                    // however the requirement says "Users should NOT be able to allocate more than the total monthly budget."
                                    // So we prevent it.
                                } else {
                                    categoryBudgetViewModel.allocateBudget(category, amount)
                                }
                            },
                            enabled = (amountInput.toDoubleOrNull() ?: 0.0).let { newAmt ->
                                val prevAmt = budgetForCategory?.allocatedAmount ?: 0.0
                                (totalAllocated - prevAmt + newAmt) <= totalMonthlyBudget
                            }
                        ) {
                            Text("Save")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
