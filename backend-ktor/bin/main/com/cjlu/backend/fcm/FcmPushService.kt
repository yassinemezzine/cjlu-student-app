package com.cjlu.backend.fcm

import com.cjlu.backend.Database

object FcmPushService {

    /**
     * Mirrors a WebSocket payload to all registered FCM device tokens for [studentId].
     * No-op when FCM is not configured or the student has no tokens.
     */
    fun sendToStudent(studentId: String, payload: String) {
        if (!FcmClient.isConfigured()) return
        val tokens = Database.getFcmTokensForStudent(studentId)
        if (tokens.isEmpty()) return
        for (token in tokens) {
            val ok = FcmClient.sendDataMessage(token, payload)
            if (!ok) {
                Database.removeFcmToken(token)
            }
        }
    }
}
