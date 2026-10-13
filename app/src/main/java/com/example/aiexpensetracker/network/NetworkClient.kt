package com.example.aiexpensetracker.network

import com.example.aiexpensetracker.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton network client configuring OkHttp and Retrofit.
 * - 10-second connect, read, and write timeouts.
 * - OkHttp logging enabled in debug builds only at BASIC level (no request/response bodies).
 * - Safe JSON deserialization using kotlinx.serialization.
 */
object NetworkClient {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            .build()
    }

    /**
     * Detects if running on an Android Studio emulator vs a real physical device.
     */
    private val isEmulator: Boolean by lazy {
        (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT
                || android.os.Build.HARDWARE.contains("goldfish")
                || android.os.Build.HARDWARE.contains("ranchu"))
    }

    /**
     * Resolves the base URL for the active environment:
     * - Release: Uses strict production BuildConfig.BASE_URL.
     * - Debug on Emulator: Uses http://10.0.2.2:8000/ (host loopback alias).
     * - Debug on Physical Device (USB): Uses http://127.0.0.1:8000/ (tunnels via adb reverse).
     */
    val resolvedBaseUrl: String by lazy {
        if (BuildConfig.DEBUG && BuildConfig.BASE_URL.contains("10.0.2.2")) {
            if (!isEmulator) {
                "http://127.0.0.1:8000/"
            } else {
                BuildConfig.BASE_URL
            }
        } else {
            BuildConfig.BASE_URL
        }
    }

    private val retrofit: Retrofit by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(resolvedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    val healthApiService: HealthApiService by lazy {
        retrofit.create(HealthApiService::class.java)
    }

    /**
     * Factory function allowing creation of HealthApiService pointing to an arbitrary URL (e.g., MockWebServer).
     */
    fun createHealthApiService(baseUrl: String, client: OkHttpClient = okHttpClient): HealthApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(HealthApiService::class.java)
    }
}
