package com.example.aiexpensetracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.aiexpensetracker.ui.viewmodel.MonthlyPlanViewModel
import java.text.DateFormatSymbols

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonthlyPlanningScreen(viewModel: MonthlyPlanViewModel) {
    val currentPlan by viewModel.currentPlan.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()

    var allowance by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPlans by remember { mutableStateOf(setOf<String>()) }
    var isInitialized by remember { mutableStateOf(false) }

    // Update state when currentPlan is fetched
    LaunchedEffect(currentPlan) {
        if (currentPlan != null && !isInitialized) {
            allowance = currentPlan?.monthlyAllowance?.toString() ?: ""
            notes = currentPlan?.notes ?: ""
            selectedPlans = currentPlan?.selectedPlans?.toSet() ?: emptySet()
            isInitialized = true
        } else if (currentPlan == null && !isInitialized) {
            isInitialized = true
        }
    }

    val predefinedPlans = listOf(
        "College Project", "Hangout", "Trip", "Festival", "Birthday",
        "Internship", "Vacation", "Exams", "Nothing Special"
    )

    val monthName = DateFormatSymbols().months[selectedMonth - 1]
    
    var showSnackbar by remember { mutableStateOf(false) }
    
    if (showSnackbar) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Monthly Planning", fontWeight = FontWeight.Bold) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Plan for $monthName $selectedYear", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = allowance,
                onValueChange = { allowance = it },
                label = { Text("Monthly Allowance (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Special Plans for this Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                predefinedPlans.forEach { plan ->
                    FilterChip(
                        selected = selectedPlans.contains(plan),
                        onClick = {
                            selectedPlans = if (selectedPlans.contains(plan)) {
                                selectedPlans - plan
                            } else {
                                selectedPlans + plan
                            }
                        },
                        label = { Text(plan) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val allowanceValue = allowance.toDoubleOrNull()
                    if (allowanceValue != null) {
                        viewModel.saveMonthlyPlan(allowanceValue, selectedPlans.toList(), notes)
                        showSnackbar = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = allowance.isNotBlank() && allowance.toDoubleOrNull() != null
            ) {
                Text(if (currentPlan == null) "Create Plan" else "Update Plan")
            }
            
            if (showSnackbar) {
                Spacer(modifier = Modifier.height(16.dp))
                Snackbar {
                    Text("Monthly Plan saved successfully!")
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
