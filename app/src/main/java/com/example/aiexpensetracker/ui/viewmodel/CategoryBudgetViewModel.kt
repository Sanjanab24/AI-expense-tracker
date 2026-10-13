package com.example.aiexpensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiexpensetracker.data.CategoryBudget
import com.example.aiexpensetracker.data.CategoryBudgetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

class CategoryBudgetViewModel(
    private val repository: CategoryBudgetRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear = _selectedYear.asStateFlow()

    fun selectMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryBudgets: StateFlow<List<CategoryBudget>> = combine(_selectedMonth, _selectedYear) { m, y ->
        Pair(m, y)
    }.flatMapLatest { (m, y) ->
        repository.getAllCategoryBudgetsForMonth(m, y)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun allocateBudget(category: String, amount: Double) {
        viewModelScope.launch {
            repository.insertOrUpdateCategoryBudget(_selectedMonth.value, _selectedYear.value, category, amount)
        }
    }
}
