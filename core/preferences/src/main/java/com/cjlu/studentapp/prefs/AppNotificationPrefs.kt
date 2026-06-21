package com.cjlu.studentapp.prefs

import android.content.Context

private const val PREFS_NAME = "cjlu_app_prefs"
private const val KEY_NOTIFY_UPDATES = "notify_me_about_updates"

object AppNotificationPrefs {
    fun isNotifyUpdatesEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFY_UPDATES, true)
    }

    fun setNotifyUpdatesEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFY_UPDATES, enabled)
            .apply()
    }
}
