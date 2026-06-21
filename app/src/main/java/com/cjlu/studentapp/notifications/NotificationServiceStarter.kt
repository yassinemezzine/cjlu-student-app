package com.cjlu.studentapp.notifications

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Utility helper to safely start and stop the persistent NotificationWebSocketService.
 * Handles API 26+ startForegroundService requirements.
 */
object NotificationServiceStarter {
    private const val TAG = "NotificationStarter"

    /**
     * Starts the WebSocket notification Foreground Service.
     */
    fun start(context: Context) {
        Log.d(TAG, "Request to start NotificationWebSocketService")
        val intent = Intent(context, NotificationWebSocketService::class.java).apply {
            action = NotificationWebSocketService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    /**
     * Stops the WebSocket notification Foreground Service.
     */
    fun stop(context: Context) {
        Log.d(TAG, "Request to stop NotificationWebSocketService")
        val intent = Intent(context, NotificationWebSocketService::class.java).apply {
            action = NotificationWebSocketService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop service", e)
        }
    }
}
