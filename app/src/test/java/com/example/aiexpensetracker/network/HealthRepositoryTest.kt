package com.example.aiexpensetracker.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class HealthRepositoryTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `checkHealth returns Success when server responds with 200 and valid JSON`() = runBlocking {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"status":"ok","service":"ai-expense-tracker-backend","version":"0.1.0"}""")
        mockWebServer.enqueue(mockResponse)

        val apiService = NetworkClient.createHealthApiService(mockWebServer.url("/").toString())
        val repository = HealthRepositoryImpl(apiService = apiService, ioDispatcher = Dispatchers.Unconfined)

        val result = repository.checkHealth()

        assertTrue("Expected Success, got $result", result is HealthResult.Success)
        val success = result as HealthResult.Success
        assertEquals("ok", success.response.status)
        assertEquals("ai-expense-tracker-backend", success.response.service)
        assertEquals("0.1.0", success.response.version)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/health", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun `checkHealth returns ServerError when server responds with HTTP 500`() = runBlocking {
        val mockResponse = MockResponse()
            .setResponseCode(500)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"status":"error","message":"An unexpected server error occurred."}""")
        mockWebServer.enqueue(mockResponse)

        val apiService = NetworkClient.createHealthApiService(mockWebServer.url("/").toString())
        val repository = HealthRepositoryImpl(apiService = apiService, ioDispatcher = Dispatchers.Unconfined)

        val result = repository.checkHealth()

        assertTrue("Expected ServerError, got $result", result is HealthResult.ServerError)
        val serverError = result as HealthResult.ServerError
        assertEquals(500, serverError.code)
    }

    @Test
    fun `checkHealth returns Timeout when server response takes longer than client timeout`() = runBlocking {
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"status":"ok","service":"ai-expense-tracker-backend","version":"0.1.0"}""")
            .setBodyDelay(1000, TimeUnit.MILLISECONDS)
        mockWebServer.enqueue(mockResponse)

        // OkHttp client with a short 200ms read timeout to test timeout behavior promptly
        val shortTimeoutClient = OkHttpClient.Builder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .connectTimeout(200, TimeUnit.MILLISECONDS)
            .build()

        val apiService = NetworkClient.createHealthApiService(
            baseUrl = mockWebServer.url("/").toString(),
            client = shortTimeoutClient
        )
        val repository = HealthRepositoryImpl(apiService = apiService, ioDispatcher = Dispatchers.Unconfined)

        val result = repository.checkHealth()

        assertTrue("Expected Timeout, got $result", result is HealthResult.Timeout)
    }

    @Test
    fun `checkHealth returns Unreachable when server is offline or unreachable`() = runBlocking {
        val serverUrl = mockWebServer.url("/").toString()
        // Shut down the server immediately to simulate an offline / unreachable backend
        mockWebServer.shutdown()

        val shortConnectClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .build()

        val apiService = NetworkClient.createHealthApiService(
            baseUrl = serverUrl,
            client = shortConnectClient
        )
        val repository = HealthRepositoryImpl(apiService = apiService, ioDispatcher = Dispatchers.Unconfined)

        val result = repository.checkHealth()

        assertTrue("Expected Unreachable, got $result", result is HealthResult.Unreachable)
    }
}
