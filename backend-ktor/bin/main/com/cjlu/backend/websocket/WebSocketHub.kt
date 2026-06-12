package com.cjlu.backend.websocket

import com.cjlu.backend.AcademicUpdatedPushDto
import com.cjlu.backend.LearningAlertsPushDto
import com.cjlu.backend.MessagesPushDto
import com.cjlu.backend.StudentProfileDto
import com.cjlu.backend.StudentRequest
import com.cjlu.backend.fcm.FcmPushService
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object WebSocketHub {
    val sessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    suspend fun notifyStudentRequest(request: StudentRequest) {
        val message = Json.encodeToString(request)
        broadcast(request.studentId, message)
    }

    suspend fun notifyAcademicUpdated(studentId: String, scope: String) {
        val message = Json.encodeToString(AcademicUpdatedPushDto(scope = scope))
        broadcast(studentId.trim(), message)
    }

    suspend fun notifyLearningAlerts(profile: StudentProfileDto) {
        val message = Json.encodeToString(
            LearningAlertsPushDto(
                overallAttendancePercent = profile.overallAttendancePercent,
                classUpdateNotice = profile.classUpdateNotice,
                classUpdateAtMillis = profile.classUpdateAtMillis,
            ),
        )
        broadcast(profile.studentId, message)
    }

    suspend fun notifyMessagesChanged(studentId: String) {
        broadcast(studentId.trim(), Json.encodeToString(MessagesPushDto()))
    }

    private suspend fun broadcast(studentId: String, message: String) {
        val userSessions = sessions[studentId]?.toList() ?: emptyList()
        for (session in userSessions) {
            try {
                session.send(message)
            } catch (_: Exception) {
            }
        }
        FcmPushService.sendToStudent(studentId, message)
    }
}
