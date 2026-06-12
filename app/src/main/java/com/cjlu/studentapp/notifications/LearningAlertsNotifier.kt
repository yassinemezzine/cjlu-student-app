package com.cjlu.studentapp.notifications

import android.content.Context
import com.cjlu.studentapp.network.api.StudentProfileDto
import com.cjlu.studentapp.prefs.StudentAlertPrefs
import kotlin.math.max

object LearningAlertsNotifier {

    /** Call after profile is synced from the server (login, pull-to-refresh, or WebSocket). */
    fun onProfileSynced(
        context: Context,
        studentId: String,
        profile: StudentProfileDto,
        isBackground: Boolean,
        notify: Boolean,
    ) {
        val sid = studentId.trim()
        if (sid.isEmpty()) return

        val seen = StudentAlertPrefs.getLastSeenClassUpdateAtMillis(context, sid)
        val at = profile.classUpdateAtMillis
        val notice = profile.classUpdateNotice?.trim().orEmpty()

        if (profile.overallAttendancePercent >= 75) {
            StudentAlertPrefs.setAttendanceBelow75Notified(context, sid, false)
        } else if (notify && !StudentAlertPrefs.isAttendanceBelow75Notified(context, sid)) {
            // Alert whenever synced data shows overall attendance below 75% (admin push or
            // refresh), not only while the app is in the background—students often keep the app open.
            CjluNotificationHelper.showAttendanceLowNotification(
                context,
                profile.overallAttendancePercent,
            )
            StudentAlertPrefs.setAttendanceBelow75Notified(context, sid, true)
        }

        if (notify && isBackground && notice.isNotEmpty() && at > seen) {
            CjluNotificationHelper.showClassUpdateNotification(context, notice)
        }

        StudentAlertPrefs.setLastSeenClassUpdateAtMillis(context, sid, max(seen, at))
    }
}
