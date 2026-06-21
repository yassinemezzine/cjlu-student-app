package com.cjlu.studentapp.prefs

import android.content.Context
import com.cjlu.studentapp.data.RequestStatus
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.network.api.MessageDto

object WidgetStatsStore {
    const val PREFS_NAME = "cjlu_widget_stats"
    const val KEY_UNREAD_MESSAGE_COUNT = "unread_message_count"
    const val KEY_ACTIVE_REQUEST_COUNT = "active_request_count"
    const val KEY_LAST_UPDATED_MILLIS = "last_updated_millis"

    fun writeFromAppState(
        context: Context,
        requests: List<StudentRequest>,
        messages: List<MessageDto>,
    ) {
        val unread = messages.count { !it.isRead }
        val activeRequests = requests.count { it.status != RequestStatus.Completed }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_UNREAD_MESSAGE_COUNT, unread)
            .putInt(KEY_ACTIVE_REQUEST_COUNT, activeRequests)
            .putLong(KEY_LAST_UPDATED_MILLIS, System.currentTimeMillis())
            .apply()
    }
}
