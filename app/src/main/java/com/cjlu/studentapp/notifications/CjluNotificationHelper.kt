package com.cjlu.studentapp.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cjlu.studentapp.MainActivity
import com.cjlu.studentapp.R
import com.cjlu.core.resources.R as CoreR
import com.cjlu.studentapp.data.RequestStatus
import com.cjlu.studentapp.data.StudentRequest
import com.cjlu.studentapp.navigation.Screen
import com.cjlu.studentapp.widget.CjluWidget

internal const val CHANNEL_REQUESTS = "cjlu_requests_v2"
internal const val CHANNEL_MESSAGES = "cjlu_messages_v2"
internal const val CHANNEL_LEARNING = "cjlu_learning"

object CjluNotificationHelper {

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(context: Context, id: Int, notification: android.app.Notification) {
        if (!canPostNotifications(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission may be revoked between the check and the call.
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REQUESTS,
                context.getString(CoreR.string.notification_channel_requests_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(CoreR.string.notification_channel_requests_description)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(CoreR.string.notification_channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(CoreR.string.notification_channel_messages_description)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LEARNING,
                context.getString(CoreR.string.notification_channel_learning_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(CoreR.string.notification_channel_learning_description)
            },
        )
    }

    fun showRequestUpdatedNotification(context: Context, request: StudentRequest) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CjluWidget.EXTRA_OPEN_TAB, CjluWidget.TAB_REQUESTS)
        }
        val pending = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val statusLabel = context.getString(request.status.labelRes)
        val title = if (request.status == RequestStatus.Completed) {
            context.getString(CoreR.string.notification_request_completed_title)
        } else {
            context.getString(CoreR.string.notification_request_update_title)
        }
        val text = if (request.status == RequestStatus.Completed) {
            context.getString(CoreR.string.notification_request_completed_body, request.id, statusLabel)
        } else {
            context.getString(CoreR.string.notification_request_update_body, request.id, statusLabel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_REQUESTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(
                if (request.status == RequestStatus.Completed) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                },
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notifySafely(context, request.id.hashCode(), notification)
    }

    fun showNewMessageNotification(context: Context, messageId: String, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CjluWidget.EXTRA_OPEN_TAB, CjluWidget.TAB_MESSAGES)
        }
        val pending = PendingIntent.getActivity(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(CoreR.string.notification_new_message_title, title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notifySafely(context, 3000 + messageId.hashCode() % 100_000, notification)
    }

    fun showAttendanceLowNotification(context: Context, percent: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CjluWidget.EXTRA_OPEN_TAB, CjluWidget.TAB_HOME)
        }
        val pending = PendingIntent.getActivity(
            context,
            4,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_LEARNING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(CoreR.string.notification_attendance_low_title))
            .setContentText(context.getString(CoreR.string.notification_attendance_low_body, percent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notifySafely(context, 4001, notification)
    }

    fun showClassUpdateNotification(context: Context, notice: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CjluWidget.EXTRA_OPEN_TAB, CjluWidget.TAB_HOME)
        }
        val pending = PendingIntent.getActivity(
            context,
            5,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_LEARNING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(CoreR.string.notification_class_update_title))
            .setContentText(notice)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notifySafely(context, 4002, notification)
    }

    fun showAttendanceUpdatedNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CjluWidget.EXTRA_OPEN_ROUTE, Screen.AttendanceDetail.route)
        }
        val pending = PendingIntent.getActivity(
            context,
            6,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_LEARNING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(CoreR.string.notification_attendance_updated_title))
            .setContentText(context.getString(CoreR.string.notification_attendance_updated_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notifySafely(context, 4003, notification)
    }

    /** Fallback when the inbox changed but no new row could be detected locally. */
    fun showMessagesRefreshNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CjluWidget.EXTRA_OPEN_TAB, CjluWidget.TAB_MESSAGES)
        }
        val pending = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(CoreR.string.notification_messages_refresh_title))
            .setContentText(context.getString(CoreR.string.notification_messages_refresh_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        notifySafely(context, 2002, notification)
    }
}
