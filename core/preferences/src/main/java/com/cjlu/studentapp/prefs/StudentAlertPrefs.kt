package com.cjlu.studentapp.prefs

import android.content.Context

private const val PREFS = "cjlu_student_alert_prefs"

object StudentAlertPrefs {
    private fun attendanceLowKey(studentId: String) = "att_low_$studentId"

    private fun classSeenKey(studentId: String) = "class_seen_$studentId"

    fun getLastSeenClassUpdateAtMillis(context: Context, studentId: String): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(classSeenKey(studentId), -1L)

    fun setLastSeenClassUpdateAtMillis(context: Context, studentId: String, millis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(classSeenKey(studentId), millis)
            .apply()
    }

    fun isAttendanceBelow75Notified(context: Context, studentId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(attendanceLowKey(studentId), false)

    fun setAttendanceBelow75Notified(context: Context, studentId: String, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(attendanceLowKey(studentId), value)
            .apply()
    }
}
