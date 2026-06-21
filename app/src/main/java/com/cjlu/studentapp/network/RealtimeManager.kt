package com.cjlu.studentapp.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cjlu.studentapp.BuildConfig
import com.cjlu.studentapp.network.AuthTokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RealtimeManager(
    private val studentId: String,
    private val onStatusChanged: (Boolean) -> Unit = {},
    private val onMessageReceived: (String) -> Unit,
) {
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webSocket: WebSocket? = null
    private var stopped = false
    private var hasConnectedOnce = false

    private val wsUrl: String
        get() {
            val scheme = if (BuildConfig.API_PORT == 443) "wss" else "ws"
            return "$scheme://${BuildConfig.API_HOST}:${BuildConfig.API_PORT}/updates/$studentId"
        }

    private val reconnectRunnable = Runnable {
        if (!stopped) {
            Log.d("RealtimeManager", "Reconnecting WebSocket for $studentId")
            startInternal()
        }
    }

    fun start() {
        stopped = false
        mainHandler.removeCallbacks(reconnectRunnable)
        startInternal()
    }

    private fun startInternal() {
        webSocket?.cancel()
        webSocket = null
        val token = AuthTokenStore.accessToken?.trim().orEmpty()
        val requestBuilder = Request.Builder()
            .url(wsUrl)
            .header("X-API-Key", BuildConfig.STUDENT_API_KEY)
        if (token.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        val request = requestBuilder.build()
        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("RealtimeManager", "Connected to WebSocket for $studentId")
                    hasConnectedOnce = true
                    onStatusChanged(true)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("RealtimeManager", "Received message: $text")
                    onMessageReceived(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d("RealtimeManager", "Closing WebSocket: $reason")
                    onStatusChanged(false)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("RealtimeManager", "WebSocket Failure", t)
                    onStatusChanged(false)
                    if (!stopped && hasConnectedOnce) {
                        mainHandler.removeCallbacks(reconnectRunnable)
                        mainHandler.postDelayed(reconnectRunnable, 5_000L)
                    }
                }
            },
        )
    }

    fun stop() {
        stopped = true
        mainHandler.removeCallbacks(reconnectRunnable)
        webSocket?.close(1000, "Goodbye")
        webSocket = null
    }
}
