package com.cjlu.studentapp.network

import android.content.Context
import android.util.Log
import com.cjlu.studentapp.auth.AuthManager
import com.cjlu.studentapp.auth.toStudentProfileDto
import com.cjlu.studentapp.data.AcademicRepository
import com.cjlu.studentapp.data.AppDatabase
import com.cjlu.studentapp.data.MessagesRepository
import com.cjlu.studentapp.data.RequestManager
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.network.api.MessageDto
import com.cjlu.studentapp.notifications.LearningAlertsNotifier
import com.cjlu.studentapp.prefs.AppNotificationPrefs
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class RealtimeSyncEffect {
    /** @param notifyAttendance true only for realtime pushes (not cold-start sync). */
    data class AcademicCacheInvalidated(val notifyAttendance: Boolean = false) : RealtimeSyncEffect()

    data class RequestUpdated(
        val request: StudentRequest,
        val isBackground: Boolean,
    ) : RealtimeSyncEffect()

    data class MessagesSynced(
        val previousMessageIds: Set<String>,
        val messages: List<MessageDto>,
        val isBackground: Boolean,
    ) : RealtimeSyncEffect()
}

class RealtimeSyncCoordinator(
    private val context: Context,
    private val studentId: String,
    private val scope: CoroutineScope,
    private val onConnectionChanged: (Boolean) -> Unit,
    private val onEffect: suspend (RealtimeSyncEffect) -> Unit,
) {
    private val sid = studentId.trim()
    private var realtime: RealtimeManager? = null

    fun start() {
        realtime?.stop()
        realtime = RealtimeManager(
            studentId = sid,
            onStatusChanged = onConnectionChanged,
            onMessageReceived = { message ->
                scope.launch { handleMessage(message) }
            },
        ).also { it.start() }
    }

    fun stop() {
        realtime?.stop()
        realtime = null
    }

    private suspend fun handleMessage(message: String) {
        val isBackground = !ProcessLifecycleOwner.get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.RESUMED)

        val action = try {
            RealtimePushHandler.parse(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse pushed update", e)
            RequestManager.syncRequests(context, sid)
            return
        }

        when (action) {
            null -> {
                RequestManager.syncRequests(context, sid)
            }
            RealtimePushAction.RefreshRequests -> {
                RequestManager.syncRequests(context, sid)
            }
            RealtimePushAction.RefreshMessages -> {
                syncMessagesWithEffect(isBackground)
            }
            RealtimePushAction.SyncLearningAlerts -> {
                invalidateAcademicAndSyncProfile(isBackground, fromPush = true)
            }
            RealtimePushAction.InvalidateAcademicCache -> {
                invalidateAcademicAndSyncProfile(isBackground, fromPush = true)
            }
            is RealtimePushAction.RequestUpdated -> {
                AppDatabase.getDatabase(context)
                    .studentRequestDao()
                    .insert(action.request)
                onEffect(
                    RealtimeSyncEffect.RequestUpdated(
                        request = action.request,
                        isBackground = isBackground,
                    ),
                )
            }
        }
    }

    suspend fun syncMessagesWithEffect(isBackground: Boolean) {
        val dao = AppDatabase.getDatabase(context).inboxMessageDao()
        val previousMessageIds = dao.getForStudent(sid).map { it.id }.toSet()
        val (messages, _) = MessagesRepository.syncMessages(context, sid)
        onEffect(
            RealtimeSyncEffect.MessagesSynced(
                previousMessageIds = previousMessageIds,
                messages = messages,
                isBackground = isBackground,
            ),
        )
    }

    suspend fun invalidateAcademicAndSyncProfile(isBackground: Boolean, fromPush: Boolean = false) {
        if (sid.isEmpty()) return
        AcademicRepository.invalidateAll(context, sid)
        runCatching { AcademicRepository.loadAttendance(context, sid) }
        AuthManager.refreshProfileFromServer(context)
        val profile = AuthManager.loadSession(context).toStudentProfileDto()
        LearningAlertsNotifier.onProfileSynced(
            context,
            sid,
            profile,
            isBackground,
            AppNotificationPrefs.isNotifyUpdatesEnabled(context),
        )
        onEffect(RealtimeSyncEffect.AcademicCacheInvalidated(notifyAttendance = fromPush))
    }

    companion object {
        private const val TAG = "RealtimeSyncCoordinator"
    }
}
