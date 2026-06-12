package com.cjlu.studentapp.notifications

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.cjlu.studentapp.auth.AuthManager
import com.cjlu.studentapp.auth.toStudentProfileDto
import com.cjlu.studentapp.network.RealtimePushAction
import com.cjlu.studentapp.network.RealtimePushHandler
import com.cjlu.studentapp.network.api.StudentProfileDto
import com.cjlu.studentapp.prefs.AppNotificationPrefs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shows system notifications for FCM data payloads when the app is backgrounded
 * and the user has enabled update notifications.
 */
object FcmPushNotificationHandler {

    private val json = Json { ignoreUnknownKeys = true }

    fun handleDataPayload(context: Context, payload: String) {
        if (!AppNotificationPrefs.isNotifyUpdatesEnabled(context)) return
        val isBackground = !ProcessLifecycleOwner.get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.RESUMED)
        if (!isBackground) return

        CjluNotificationHelper.ensureChannels(context)

        val action = try {
            RealtimePushHandler.parse(payload)
        } catch (_: Exception) {
            return
        } ?: return

        when (action) {
            RealtimePushAction.RefreshMessages -> {
                CjluNotificationHelper.showMessagesRefreshNotification(context)
            }
            RealtimePushAction.SyncLearningAlerts -> {
                handleLearningPayload(context, payload, notify = true, isBackground = true)
            }
            RealtimePushAction.InvalidateAcademicCache -> {
                CjluNotificationHelper.showAttendanceUpdatedNotification(context)
            }
            is RealtimePushAction.RequestUpdated -> {
                CjluNotificationHelper.showRequestUpdatedNotification(context, action.request)
            }
            RealtimePushAction.RefreshRequests -> {
                // No dedicated notification for generic refresh.
            }
        }
    }

    private fun handleLearningPayload(
        context: Context,
        payload: String,
        notify: Boolean,
        isBackground: Boolean,
    ) {
        val session = AuthManager.loadSession(context)
        if (!session.isLoggedIn) return
        val profile = parseLearningProfile(payload, session.toStudentProfileDto()) ?: return
        LearningAlertsNotifier.onProfileSynced(
            context,
            session.studentId,
            profile,
            isBackground,
            notify,
        )
    }

    private fun parseLearningProfile(payload: String, fallback: StudentProfileDto): StudentProfileDto? {
        if (!payload.startsWith("{")) return fallback
        return try {
            val obj = json.parseToJsonElement(payload).jsonObject
            val percent = obj["overallAttendancePercent"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: return fallback
            val notice = obj["classUpdateNotice"]?.jsonPrimitive?.content
            val at = obj["classUpdateAtMillis"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            fallback.copy(
                overallAttendancePercent = percent,
                classUpdateNotice = notice?.takeIf { it.isNotBlank() },
                classUpdateAtMillis = at,
            )
        } catch (_: Exception) {
            fallback
        }
    }
}
