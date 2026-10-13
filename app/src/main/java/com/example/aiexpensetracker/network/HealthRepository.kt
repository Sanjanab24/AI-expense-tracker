package com.example.aiexpensetracker.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Repository for checking backend health status.
 * Executes on an IO dispatcher and catches all exceptions to return safe HealthResult instances.
 */
interface HealthRepository {
    suspend fun checkHealth(): HealthResult
}

class HealthRepositoryImpl(
    private val apiService: HealthApiService = NetworkClient.healthApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HealthRepository {

    override suspend fun checkHealth(): HealthResult = withContext(ioDispatcher) {
        try {
            val response = apiService.getHealth()
            HealthResult.Success(response)
        } catch (e: SocketTimeoutException) {
            HealthResult.Timeout
        } catch (e: ConnectException) {
            HealthResult.Unreachable
        } catch (e: UnknownHostException) {
            HealthResult.Unreachable
        } catch (e: NoRouteToHostException) {
            HealthResult.Unreachable
        } catch (e: HttpException) {
            HealthResult.ServerError(e.code())
        } catch (e: IOException) {
            // General IO exception indicating networking / connection issues
            HealthResult.Unreachable
        } catch (e: Throwable) {
            // Never throw to the UI
            HealthResult.Unknown(e.localizedMessage ?: e.message)
        }
    }
}
