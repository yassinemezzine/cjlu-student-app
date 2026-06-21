package com.cjlu.studentapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cjlu.studentapp.BuildConfig
import com.cjlu.studentapp.MainActivity
import com.cjlu.studentapp.R
import com.cjlu.studentapp.auth.AuthManager
import com.cjlu.studentapp.auth.toStudentProfileDto
import com.cjlu.studentapp.data.AcademicRepository
import com.cjlu.studentapp.data.AppDatabase
import com.cjlu.studentapp.data.MessagesRepository
import com.cjlu.studentapp.data.RequestManager
import com.cjlu.studentapp.navigation.Screen
import com.cjlu.studentapp.prefs.AppNotificationPrefs
import com.cjlu.studentapp.prefs.WidgetStatsStore
import com.cjlu.studentapp.widget.CjluAppWidget
import com.cjlu.studentapp.widget.CjluWidget
import com.cjlu.studentapp.data.toDto
import com.cjlu.studentapp.data.toDomain
import com.cjlu.studentapp.data.toEntity
import androidx.glance.appwidget.updateAll
import com.cjlu.contract.AcademicUpdatedPushDto
import com.cjlu.contract.LearningAlertsPushDto
import com.cjlu.contract.MessagesPushDto
import com.cjlu.contract.StudentRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * Foreground Service that establishes and maintains a persistent WebSocket connection to the Ktor backend.
 * It handles real-time updates and triggers high-priority lock-screen notifications using unique IDs,
 * bypassing Google services (FCM).
 */
class NotificationWebSocketService : Service() {

    companion object {
        private const val TAG = "NotificationWSService"
        private const val CHANNEL_SERVICE_ID = "cjlu_service_channel"
        private const val NOTIFICATION_ID = 9999

        // Action intents for starting/stopping the service
        const val ACTION_START = "com.cjlu.studentapp.ACTION_START"
        const val ACTION_STOP = "com.cjlu.studentapp.ACTION_STOP"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private val json = Json { ignoreUnknownKeys = true }

    // Lazy initialization of Ktor client using CIO engine and WebSockets plugin
    private val httpClient by lazy {
        HttpClient(CIO) {
            install(WebSockets) {
                // Keep the connection alive through carrier NATs and firewalls
                pingIntervalMillis = 20_000L
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createServiceNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")

        if (action == ACTION_STOP) {
            Log.d(TAG, "Stopping service via action")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground immediately with a persistent sticky notification
        val notification = NotificationCompat.Builder(this, CHANNEL_SERVICE_ID)
            .setContentTitle("Student Portal Sync")
            .setContentText("Live connection active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Launch persistent WebSocket connection loop
        startWebSocketLoop()

        return START_STICKY
    }

    private fun startWebSocketLoop() {
        serviceScope.launch {
            val session = AuthManager.loadSession(this@NotificationWebSocketService)
            if (!session.isLoggedIn || session.studentId.isBlank()) {
                Log.w(TAG, "Student not logged in or ID blank. Stopping service.")
                stopSelf()
                return@launch
            }

            val studentId = session.studentId.trim()
            var attempt = 0

            while (isActive) {
                // Fetch the latest fresh token in case it got updated
                val token = AuthManager.loadSession(this@NotificationWebSocketService).studentId // wait, AuthTokenStore is set by loadSession
                val accessToken = com.cjlu.studentapp.network.AuthTokenStore.accessToken?.trim().orEmpty()

                if (accessToken.isBlank()) {
                    Log.w(TAG, "No access token found. Waiting to retry...")
                    delay(5000)
                    continue
                }

                // Calculate Delay with Exponential Backoff + Jitter
                if (attempt > 0) {
                    val backoffBase = (1 shl min(attempt, 5)) * 1000L // 2^attempt * 1000 ms, max 32s
                    val cap = 30_000L
                    val delayMs = min(backoffBase, cap)
                    val jitter = (0..500).random().toLong()
                    val finalDelay = delayMs + jitter

                    Log.d(TAG, "Reconnecting attempt $attempt in ${finalDelay}ms")
                    delay(finalDelay)
                }

                attempt++

                try {
                    val scheme = if (BuildConfig.API_PORT == 443) "wss" else "ws"
                    val wsUrl = "$scheme://${BuildConfig.API_HOST}:${BuildConfig.API_PORT}/updates/$studentId"
                    Log.d(TAG, "Connecting to WebSocket: $wsUrl")

                    val webSocketSession = httpClient.webSocketSession {
                        url(wsUrl)
                        header("X-API-Key", BuildConfig.STUDENT_API_KEY)
                        header("Authorization", "Bearer $accessToken")
                    }

                    Log.d(TAG, "WebSocket connected successfully for $studentId")
                    attempt = 0 // Reset backoff upon successful connection

                    for (frame in webSocketSession.incoming) {
                        if (frame is Frame.Text) {
                            val payload = frame.readText()
                            Log.d(TAG, "Received message: $payload")
                            handleWebSocketPayload(studentId, payload)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "WebSocket error/connection lost", e)
                }
            }
        }
    }

    private suspend fun handleWebSocketPayload(studentId: String, payload: String) {
        if (!payload.startsWith("{")) return

        try {
            val jsonObject = json.parseToJsonElement(payload).jsonObject
            val type = jsonObject["type"]?.jsonPrimitive?.content ?: when {
                jsonObject.isEmpty() -> "messages"
                jsonObject.containsKey("scope") -> "academic_updated"
                jsonObject.containsKey("overallAttendancePercent") -> "learning_alerts"
                else -> null
            }

            when (type) {
                "learning_alerts" -> {
                    val dto = json.decodeFromJsonElement<LearningAlertsPushDto>(jsonObject)
                    Log.d(TAG, "Decoded LearningAlertsPushDto: $dto")

                    // Invalidate and sync profile in the background
                    AuthManager.refreshProfileFromServer(applicationContext)
                    val profile = AuthManager.loadSession(applicationContext).toStudentProfileDto()
                    LearningAlertsNotifier.onProfileSynced(
                        context = applicationContext,
                        studentId = studentId,
                        profile = profile,
                        isBackground = true,
                        notify = true
                    )

                    // Show custom lock-screen alert
                    val notice = dto.classUpdateNotice?.takeIf { it.isNotBlank() }
                        ?: "Your attendance or class schedule has been updated."
                    showHighPriorityNotification(
                        id = 4000 + (dto.classUpdateAtMillis % 1000).toInt(),
                        title = "Class & Attendance Alert",
                        text = notice,
                        channelId = CHANNEL_LEARNING,
                        tab = CjluWidget.TAB_HOME
                    )
                }
                "messages" -> {
                    val dto = json.decodeFromJsonElement<MessagesPushDto>(jsonObject)
                    Log.d(TAG, "Decoded MessagesPushDto: $dto")

                    val dao = AppDatabase.getDatabase(applicationContext).inboxMessageDao()
                    val previousIds = dao.getForStudent(studentId).map { it.id }.toSet()
                    val (messages, _) = MessagesRepository.syncMessages(applicationContext, studentId)

                    updateWidgetStats(studentId)

                    val newUnread = messages.filter { it.id !in previousIds && !it.isRead }
                    if (newUnread.isNotEmpty()) {
                        val first = newUnread.first()
                        showHighPriorityNotification(
                            id = 3000 + (first.id.hashCode() % 100_000),
                            title = "New Message: ${first.title}",
                            text = first.body,
                            channelId = CHANNEL_MESSAGES,
                            tab = CjluWidget.TAB_MESSAGES
                        )
                    } else {
                        showHighPriorityNotification(
                            id = 2002,
                            title = "Inbox Refreshed",
                            text = "Your inbox has been updated.",
                            channelId = CHANNEL_MESSAGES,
                            tab = CjluWidget.TAB_MESSAGES
                        )
                    }
                }
                "academic_updated" -> {
                    val dto = json.decodeFromJsonElement<AcademicUpdatedPushDto>(jsonObject)
                    Log.d(TAG, "Decoded AcademicUpdatedPushDto: $dto")

                    AcademicRepository.invalidateAll(applicationContext, studentId)
                    runCatching { AcademicRepository.loadAttendance(applicationContext, studentId) }
                    AuthManager.refreshProfileFromServer(applicationContext)

                    showHighPriorityNotification(
                        id = 4003,
                        title = "Academic Record Updated",
                        text = "Your academic and attendance cache has been updated.",
                        channelId = CHANNEL_LEARNING,
                        route = Screen.AttendanceDetail.route
                    )
                }
                else -> {
                    // Check if it matches StudentRequest
                    if (jsonObject.containsKey("serviceId")) {
                        val contractReq = json.decodeFromJsonElement<StudentRequest>(jsonObject)
                        Log.d(TAG, "Decoded StudentRequest: $contractReq")

                        val appReq = mapContractRequest(contractReq)
                        AppDatabase.getDatabase(applicationContext)
                            .studentRequestDao()
                            .insert(appReq.toEntity())

                        updateWidgetStats(studentId)
                        RequestManager.syncRequests(applicationContext, studentId)

                        val statusLabel = when (appReq.status) {
                            com.cjlu.studentapp.data.RequestStatus.Submitted -> "Submitted"
                            com.cjlu.studentapp.data.RequestStatus.InReview -> "In review"
                            com.cjlu.studentapp.data.RequestStatus.ActionNeeded -> "Action needed"
                            com.cjlu.studentapp.data.RequestStatus.Completed -> "Completed"
                        }
                        val title = if (appReq.status == com.cjlu.studentapp.data.RequestStatus.Completed) {
                            "Request Completed"
                        } else {
                            "Request Updated"
                        }
                        showHighPriorityNotification(
                            id = appReq.id.hashCode(),
                            title = title,
                            text = "Your request ID ${appReq.id} status is now $statusLabel.",
                            channelId = CHANNEL_REQUESTS,
                            tab = CjluWidget.TAB_REQUESTS
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing/handling push payload", e)
        }
    }

    private suspend fun updateWidgetStats(studentId: String) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val requests = db.studentRequestDao().getRequestsForStudent(studentId).map { it.toDomain() }
            val messages = db.inboxMessageDao().getForStudent(studentId).map { it.toDto() }

            WidgetStatsStore.writeFromAppState(applicationContext, requests, messages)
            CjluAppWidget().updateAll(applicationContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget stats", e)
        }
    }

    private fun showHighPriorityNotification(
        id: Int,
        title: String,
        text: String,
        channelId: String,
        tab: String? = null,
        route: String? = null
    ) {
        // Only trigger updates if notifications are enabled in preferences
        if (!AppNotificationPrefs.isNotifyUpdatesEnabled(applicationContext)) return

        CjluNotificationHelper.ensureChannels(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (tab != null) {
                putExtra(CjluWidget.EXTRA_OPEN_TAB, tab)
            }
            if (route != null) {
                putExtra(CjluWidget.EXTRA_OPEN_ROUTE, route)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Student Portal Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps connection open to receive campus updates in real time"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun mapContractRequest(req: StudentRequest): com.cjlu.studentapp.data.StudentRequest {
        val mappedStatus = when (req.status) {
            com.cjlu.contract.RequestStatus.Submitted -> com.cjlu.studentapp.data.RequestStatus.Submitted
            com.cjlu.contract.RequestStatus.InReview -> com.cjlu.studentapp.data.RequestStatus.InReview
            com.cjlu.contract.RequestStatus.ActionNeeded -> com.cjlu.studentapp.data.RequestStatus.ActionNeeded
            com.cjlu.contract.RequestStatus.Completed -> com.cjlu.studentapp.data.RequestStatus.Completed
        }
        return com.cjlu.studentapp.data.StudentRequest(
            id = req.id,
            serviceId = req.serviceId,
            studentId = req.studentId,
            contactInfo = req.contactInfo,
            notes = req.notes,
            status = mappedStatus,
            createdAtMillis = req.createdAtMillis,
            attachmentUrl = req.attachmentUrl
        )
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        serviceScope.cancel()
        runCatching {
            httpClient.close()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
