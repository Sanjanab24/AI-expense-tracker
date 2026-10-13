package com.example.aiexpensetracker.network

import kotlinx.serialization.Serializable

/**
 * Data model for GET /health endpoint.
 */
@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String
)
