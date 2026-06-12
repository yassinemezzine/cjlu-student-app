package com.cjlu.backend.fcm

import com.google.auth.oauth2.GoogleCredentials
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * Sends FCM HTTP v1 data messages using a Firebase service account JSON file.
 *
 * Configure with env `CJLU_FCM_SERVICE_ACCOUNT` (absolute path to JSON) or place
 * `fcm-service-account.json` in the backend working directory.
 */
object FcmClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient.newBuilder().build()

    @Serializable
    private data class FcmMessageBody(
        val message: FcmMessage,
    )

    @Serializable
    private data class FcmMessage(
        val token: String,
        val data: Map<String, String>,
    )

    private data class Config(
        val projectId: String,
        val credentials: GoogleCredentials,
    )

    @Volatile
    private var config: Config? = null

    @Volatile
    private var loadAttempted = false

    fun isConfigured(): Boolean = resolveConfig() != null

    fun sendDataMessage(deviceToken: String, payload: String): Boolean {
        val cfg = resolveConfig() ?: return false
        val token = deviceToken.trim()
        if (token.isEmpty()) return false
        return try {
            val accessToken = cfg.credentials.createScoped(
                listOf("https://www.googleapis.com/auth/firebase.messaging"),
            ).refreshAccessToken().tokenValue

            val body = json.encodeToString(
                FcmMessageBody(
                    message = FcmMessage(
                        token = token,
                        data = mapOf("payload" to payload),
                    ),
                ),
            )

            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://fcm.googleapis.com/v1/projects/${cfg.projectId}/messages:send"))
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build()

            val response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            if (response.statusCode() in 200..299) {
                true
            } else {
                System.err.println(
                    "FCM send failed (${response.statusCode()}): ${response.body().take(500)}",
                )
                false
            }
        } catch (e: Exception) {
            System.err.println("FCM send error: ${e.message}")
            false
        }
    }

    private fun resolveConfig(): Config? {
        config?.let { return it }
        if (loadAttempted) return null
        synchronized(this) {
            if (config != null) return config
            if (loadAttempted) return null
            loadAttempted = true
            config = loadConfig()
            return config
        }
    }

    private fun loadConfig(): Config? {
        val path = System.getenv("CJLU_FCM_SERVICE_ACCOUNT")?.trim()?.takeIf { it.isNotEmpty() }
            ?: run {
                val local = File("fcm-service-account.json")
                if (local.isFile) local.absolutePath else null
            }
        if (path == null) {
            System.err.println(
                "FCM not configured: set CJLU_FCM_SERVICE_ACCOUNT or add backend-ktor/fcm-service-account.json",
            )
            return null
        }
        return try {
            val file = File(path)
            val projectId = Regex(""""project_id"\s*:\s*"([^"]+)"""")
                .find(file.readText())
                ?.groupValues
                ?.get(1)
                ?.trim()
                .orEmpty()
            if (projectId.isEmpty()) {
                System.err.println("FCM service account JSON missing project_id")
                return null
            }
            file.inputStream().use { input ->
                val credentials = GoogleCredentials.fromStream(input)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
                Config(projectId = projectId, credentials = credentials)
            }
        } catch (e: Exception) {
            System.err.println("FCM config load failed: ${e.message}")
            null
        }
    }
}
