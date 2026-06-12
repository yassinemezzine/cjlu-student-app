package com.cjlu.studentapp.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CjluFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        FcmTokenRegistrar.registerTokenAsync(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = message.data["payload"]?.trim().orEmpty()
        if (payload.isEmpty()) return
        FcmPushNotificationHandler.handleDataPayload(applicationContext, payload)
    }
}
