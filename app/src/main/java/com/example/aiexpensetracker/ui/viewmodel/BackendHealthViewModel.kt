package com.example.aiexpensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiexpensetracker.network.HealthRepository
import com.example.aiexpensetracker.network.HealthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representing the backend health check outcome.
 */
sealed interface BackendHealthUiState {
    data object Idle : BackendHealthUiState
    data object Loading : BackendHealthUiState
    data class Success(val service: String, val version: String) : BackendHealthUiState
    data class Error(val message: String) : BackendHealthUiState
}

/**
 * ViewModel managing the backend health check.
 * Consistent with existing StateFlow and coroutine ViewModel patterns in the app.
 */
class BackendHealthViewModel(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackendHealthUiState>(BackendHealthUiState.Idle)
    val uiState: StateFlow<BackendHealthUiState> = _uiState.asStateFlow()

    fun checkHealth() {
        _uiState.value = BackendHealthUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = healthRepository.checkHealth()) {
                is HealthResult.Success -> {
                    BackendHealthUiState.Success(
                        service = result.response.service,
                        version = result.response.version
                    )
                }
                is HealthResult.Unreachable -> {
                    BackendHealthUiState.Error("Backend unreachable. Ensure FastAPI is running and 'adb reverse tcp:8000 tcp:8000' is executed.")
                }
                is HealthResult.Timeout -> {
                    BackendHealthUiState.Error("Connection timed out (10s). Ensure backend is running and 'adb reverse tcp:8000 tcp:8000' is executed.")
                }
                is HealthResult.ServerError -> {
                    BackendHealthUiState.Error("Server returned HTTP ${result.code}")
                }
                is HealthResult.Unknown -> {
                    BackendHealthUiState.Error(result.message ?: "Unknown error occurred")
                }
            }
        }
    }
}
