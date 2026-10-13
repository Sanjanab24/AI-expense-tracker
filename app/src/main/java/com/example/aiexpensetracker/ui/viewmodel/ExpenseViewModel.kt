package com.example.aiexpensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiexpensetracker.data.Budget
import com.example.aiexpensetracker.data.CategorySummary
import com.example.aiexpensetracker.data.Expense
import com.example.aiexpensetracker.data.ExpenseRepository
import com.example.aiexpensetracker.data.RecurringExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.example.aiexpensetracker.data.SortOrder
import com.example.aiexpensetracker.data.MonthlyPlan
import com.example.aiexpensetracker.data.MonthlyPlanRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.Calendar

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val monthlyPlanRepository: MonthlyPlanRepository
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.generateRecurringExpenses()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow("All")
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear = _selectedYear.asStateFlow()

    fun selectMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<Expense>> = combine(
        _searchQuery,
        _categoryFilter,
        _sortOrder,
        _selectedMonth,
        _selectedYear
    ) { query, category, sort, month, year ->
        val (startDate, endDate) = com.example.aiexpensetracker.data.DateUtils.getMonthRange(month, year)
        repository.getFilteredExpensesInPeriod(startDate, endDate, query, category, sort)
    }.flatMapLatest { flow ->
        flow
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val categorySummaries: StateFlow<List<CategorySummary>> = combine(_selectedMonth, _selectedYear) { m, y ->
        com.example.aiexpensetracker.data.DateUtils.getMonthRange(m, y)
    }.flatMapLatest { (startDate, endDate) ->
        repository.getCategorySummariesInPeriod(startDate, endDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSpent: StateFlow<Double> = combine(_selectedMonth, _selectedYear) { m, y ->
        com.example.aiexpensetracker.data.DateUtils.getMonthRange(m, y)
    }.flatMapLatest { (startDate, endDate) ->
        repository.getTotalSpentInPeriod(startDate, endDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonthPlan: StateFlow<MonthlyPlan?> = combine(_selectedMonth, _selectedYear) { m, y ->
        Pair(m, y)
    }.flatMapLatest { (m, y) ->
        monthlyPlanRepository.getMonthlyPlan(m, y)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val remainingSpendableBudget: StateFlow<Double> = combine(currentMonthPlan, totalSpent) { plan, spent ->
        (plan?.monthlyAllowance ?: 0.0) - spent
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allRecurringExpenses: StateFlow<List<RecurringExpense>> = repository.getAllRecurringExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun addExpense(amount: Double, description: String, category: String, date: Long) {
        viewModelScope.launch {
            repository.insertExpense(Expense(amount = amount, description = description, category = category, date = date))
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun getExpenseById(id: Int): Flow<Expense?> {
        return repository.getExpenseById(id)
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch {
            monthlyPlanRepository.insertOrUpdateMonthlyPlan(
                month = _selectedMonth.value,
                year = _selectedYear.value,
                monthlyAllowance = amount
            )
        }
    }

    fun addRecurringExpense(name: String, category: String, amount: Double, frequency: String) {
        viewModelScope.launch {
            repository.insertRecurringExpense(
                RecurringExpense(name = name, category = category, amount = amount, frequency = frequency)
            )
            repository.generateRecurringExpenses()
        }
    }

    fun updateRecurringExpense(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            repository.updateRecurringExpense(recurringExpense)
            repository.generateRecurringExpenses()
        }
    }

    fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(recurringExpense)
        }
    }

    fun toggleRecurringExpenseActive(recurringExpense: RecurringExpense) {
        viewModelScope.launch {
            repository.updateRecurringExpense(recurringExpense.copy(isActive = !recurringExpense.isActive))
            repository.generateRecurringExpenses()
        }
    }

    fun getRecurringExpenseById(id: Int): Flow<RecurringExpense?> {
        return repository.getRecurringExpenseById(id)
    }

    fun generateRecurringExpenses() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.generateRecurringExpenses()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }
}
