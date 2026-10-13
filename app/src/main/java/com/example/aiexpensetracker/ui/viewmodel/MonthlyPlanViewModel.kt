package com.example.aiexpensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiexpensetracker.data.MonthlyPlan
import com.example.aiexpensetracker.data.MonthlyPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.Calendar

class MonthlyPlanViewModel(private val repository: MonthlyPlanRepository) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) // 1-12
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear = _selectedYear.asStateFlow()

    fun selectMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentPlan: StateFlow<MonthlyPlan?> = combine(_selectedMonth, _selectedYear) { m, y ->
        Pair(m, y)
    }.flatMapLatest { (m, y) ->
        repository.getMonthlyPlan(m, y)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveMonthlyPlan(allowance: Double, selectedPlans: List<String>, notes: String) {
        viewModelScope.launch {
            repository.insertOrUpdateMonthlyPlan(
                month = _selectedMonth.value,
                year = _selectedYear.value,
                monthlyAllowance = allowance,
                selectedPlans = selectedPlans,
                notes = notes
            )
        }
    }
}
