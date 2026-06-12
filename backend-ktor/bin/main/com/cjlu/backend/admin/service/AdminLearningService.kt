package com.cjlu.backend.admin.service

import com.cjlu.backend.Database
import com.cjlu.backend.StudentProfileDto
import com.cjlu.backend.websocket.WebSocketHub

object AdminLearningService {

    sealed class PatchResult {
        data class Success(val profile: StudentProfileDto) : PatchResult()
        data object NoChanges : PatchResult()
        data object UnknownStudent : PatchResult()
        data object InvalidAttendance : PatchResult()
    }

    suspend fun patchLearningAlerts(
        studentId: String,
        attendanceRaw: String,
        classNoticeRaw: String,
        clearClassNotice: Boolean,
    ): PatchResult {
        val attRaw = attendanceRaw.trim()
        val attendance =
            if (attRaw.isEmpty()) null else attRaw.toIntOrNull()?.coerceIn(0, 100)
        if (attRaw.isNotEmpty() && attendance == null) {
            return PatchResult.InvalidAttendance
        }
        val noticeRaw = classNoticeRaw.trim()
        val classNoticeUpdate: String? = when {
            clearClassNotice -> ""
            noticeRaw.isNotEmpty() -> noticeRaw
            else -> null
        }
        if (attendance == null && classNoticeUpdate == null) {
            return PatchResult.NoChanges
        }
        val updated = Database.patchStudentLearningAlerts(
            studentId = studentId,
            attendancePercent = attendance,
            classNoticeUpdate = classNoticeUpdate,
        ) ?: return PatchResult.UnknownStudent
        WebSocketHub.notifyLearningAlerts(updated)
        return PatchResult.Success(updated)
    }
}
