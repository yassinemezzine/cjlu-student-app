package com.cjlu.studentapp.notifications

import android.content.Context
import android.util.Log
import com.cjlu.studentapp.auth.AuthManager
import com.cjlu.studentapp.network.RetrofitClient
import com.cjlu.studentapp.network.api.FcmTokenRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FcmTokenRegistrar {
    private const val TAG = "FcmTokenRegistrar"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerTokenAsync(context: Context, token: String) {
        scope.launch {
            registerToken(context, token)
        }
    }

    suspend fun registerCurrentToken(context: Context) {
        if (!isFirebaseMessagingAvailable()) {
            Log.w(TAG, "Skipping FCM token registration: Firebase Messaging is not configured for this build")
            return
        }
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            registerToken(context, token)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain FCM token", e)
        }
    }

    private fun isFirebaseMessagingAvailable(): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun registerToken(context: Context, token: String) {
        val session = AuthManager.loadSession(context)
        if (!session.isLoggedIn) return
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return
        try {
            RetrofitClient.instance.registerFcmToken(
                studentId = session.studentId,
                body = FcmTokenRequest(token = trimmed),
            )
            Log.d(TAG, "FCM token registered for ${session.studentId}")
        } catch (e: Exception) {
            Log.w(TAG, "FCM token registration failed", e)
        }
    }
}
