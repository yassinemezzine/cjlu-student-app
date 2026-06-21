package com.cjlu.studentapp.network

import com.cjlu.core.network.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    @Volatile
    private var overrideBaseUrl: String? = null

    @Volatile
    private var overrideClient: OkHttpClient? = null

    private val baseUrl: String
        get() {
            val scheme = if (BuildConfig.API_PORT == 443) "https" else "http"
            return overrideBaseUrl ?: "$scheme://${BuildConfig.API_HOST}:${BuildConfig.API_PORT}/"
        }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun createClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val b = chain.request().newBuilder()
                .header("X-API-Key", BuildConfig.STUDENT_API_KEY)
                .header("Accept-Language", ApiLanguageStore.acceptLanguageHeader)
            val token = AuthTokenStore.accessToken?.trim().orEmpty()
            if (token.isNotEmpty()) {
                b.header("Authorization", "Bearer $token")
            }
            chain.proceed(b.build())
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    private val okHttpClient = createClient()

    val instance: CJLUService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(overrideClient ?: okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CJLUService::class.java)
    }

    fun setTestServer(baseUrl: String, client: OkHttpClient) {
        overrideBaseUrl = baseUrl
        overrideClient = client
    }

    fun testClient(): OkHttpClient = overrideClient ?: createClient()

    fun clearTestServer() {
        overrideBaseUrl = null
        overrideClient = null
    }
}
