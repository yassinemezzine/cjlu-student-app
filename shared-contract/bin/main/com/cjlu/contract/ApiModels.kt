package com.cjlu.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RequestStatus {
    @SerialName("submitted") Submitted,
    @SerialName("in_review") InReview,
    @SerialName("action_needed") ActionNeeded,
    @SerialName("completed") Completed,
}

@Serializable
data class StudentRequest(
    val id: String,
    val serviceId: String,
    val studentId: String,
    val contactInfo: String,
    val notes: String,
    val status: RequestStatus,
    val createdAtMillis: Long,
    val attachmentUrl: String? = null,
)

@Serializable
data class RequestSubmission(
    val serviceId: String,
    val studentId: String,
    val contactInfo: String,
    val notes: String,
)

@Serializable
data class LoginRequest(
    val studentId: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val profile: StudentProfileDto,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
data class StudentProfileDto(
    val studentId: String,
    val displayName: String,
    val classSection: String,
    val major: String,
    val school: String,
    val overallAttendancePercent: Int = 96,
    val classUpdateNotice: String? = null,
    val classUpdateAtMillis: Long = 0L,
)

@Serializable
data class LearningAlertsPushDto(
    val type: String = "learning_alerts",
    val overallAttendancePercent: Int,
    val classUpdateNotice: String? = null,
    val classUpdateAtMillis: Long = 0L,
)

@Serializable
data class MessagesPushDto(
    val type: String = "messages",
)

@Serializable
data class AcademicUpdatedPushDto(
    val type: String = "academic_updated",
    val scope: String,
)

@Serializable
data class PatchProfileRequest(
    val major: String,
    val school: String,
)

@Serializable
data class CatalogServiceDto(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val turnaround: String,
    val checklist: List<String>,
    val isPopular: Boolean,
    val estimatedMinutes: Int = 0,
    val eligibilityNote: String? = null,
    val requiredDocuments: List<String> = emptyList(),
)

@Serializable
data class OkResponse(val ok: Boolean = true)

@Serializable
data class FcmTokenRequest(
    val token: String,
)

@Serializable
data class MessageReadBody(val read: Boolean = true)

@Serializable
data class MessageDto(
    val id: String,
    val category: String,
    val sender: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val relatedServiceId: String? = null,
    val requiresAction: Boolean,
    val isRead: Boolean,
)

@Serializable
data class CourseAttendanceDto(
    val courseCode: String,
    val courseName: String,
    val attendancePercent: Int,
    val sessionsAttended: Int,
    val sessionsTotal: Int,
)

@Serializable
data class WeeklyAttendanceDto(
    val weekLabel: String,
    val percent: Int,
)

@Serializable
data class StudentAttendanceDetailDto(
    val studentId: String,
    val classSection: String,
    val overallAttendancePercent: Int,
    val courses: List<CourseAttendanceDto>,
    val weeklyTrend: List<WeeklyAttendanceDto>,
)

@Serializable
data class TranscriptCourseDto(
    val courseCode: String,
    val courseName: String,
    val credits: Int,
    val scorePercent: Int,
    val gradePoint: Double,
)

@Serializable
data class StudentTranscriptDto(
    val studentId: String,
    val classSection: String,
    val semesterLabel: String,
    val courses: List<TranscriptCourseDto>,
    val cumulativeGpa: Double,
)

@Serializable
data class StudentDormitoryDto(
    val studentId: String,
    val buildingName: String,
    val roomNumber: String,
    val floor: Int,
    val bedLabel: String,
    val hasActiveLeave: Boolean,
    val leaveReason: String? = null,
    val leaveFromDate: String? = null,
    val leaveToDate: String? = null,
)

@Serializable
data class TimetableSlotDto(
    val dayOfWeek: Int,
    val dayLabel: String,
    val startTime: String,
    val endTime: String,
    val courseCode: String,
    val courseName: String,
    val roomName: String,
)

@Serializable
data class StudentTimetableDto(
    val studentId: String,
    val classSection: String,
    val semesterLabel: String,
    val slots: List<TimetableSlotDto>,
)

@Serializable
data class AcademicCalendarEventDto(
    val date: String,
    val title: String,
    val detail: String,
    val tone: String,
)

@Serializable
data class AcademicCalendarMonthDto(
    val monthLabel: String,
    val weekHeaders: List<String>,
    val rows: List<List<String>>,
)

@Serializable
data class StudentAcademicCalendarDto(
    val academicYearLabel: String,
    val semesterLabel: String,
    val months: List<AcademicCalendarMonthDto>,
    val events: List<AcademicCalendarEventDto>,
)
