package com.example.aiexpensetracker.network

import retrofit2.http.GET

/**
 * Retrofit API interface for backend health checking.
 */
interface HealthApiService {
    @GET("health")
    suspend fun getHealth(): HealthResponse
}
