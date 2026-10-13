package com.example.aiexpensetracker.network

/**
 * Sealed result type representing all possible outcomes of a backend health check.
 * Encapsulates errors so that exceptions are never thrown to the UI.
 */
sealed interface HealthResult {
    data class Success(val response: HealthResponse) : HealthResult
    data class ServerError(val code: Int) : HealthResult
    data object Timeout : HealthResult
    data object Unreachable : HealthResult
    data class Unknown(val message: String?) : HealthResult
}
