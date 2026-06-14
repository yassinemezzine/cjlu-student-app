package com.cjlu.backend.admin

import com.cjlu.backend.CatalogServiceDto
import com.cjlu.backend.CourseAttendanceDto
import com.cjlu.backend.Database
import com.cjlu.backend.RequestStatus
import com.cjlu.backend.StudentAttendanceDetailDto
import com.cjlu.backend.StudentRequest
import com.cjlu.backend.StudentTranscriptDto
import com.cjlu.backend.StudentTimetableDto
import com.cjlu.backend.TimetableSlotDto
import com.cjlu.backend.AcademicRepository
import com.cjlu.contract.TranscriptCourseDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val submittedDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())

private fun formatSubmittedTime(millis: Long): String {
    return try {
        submittedDateFormatter.format(Instant.ofEpochMilli(millis))
    } catch (e: Exception) {
        ""
    }
}

private val categoryOrder = listOf("International", "Academic", "Learning", "Campus")

private fun RequestStatus.cssSuffix(): String = when (this) {
    RequestStatus.Submitted -> "submitted"
    RequestStatus.InReview -> "in_review"
    RequestStatus.ActionNeeded -> "action_needed"
    RequestStatus.Completed -> "completed"
}

private fun RequestStatus.displayLabel(): String = when (this) {
    RequestStatus.Submitted -> "Submitted"
    RequestStatus.InReview -> "In review"
    RequestStatus.ActionNeeded -> "Action needed"
    RequestStatus.Completed -> "Completed"
}

private fun requestRow(
    req: StudentRequest,
    studentLookup: Map<String, Database.StudentSummary>,
): Map<String, Any?> {
    val summary = studentLookup[req.studentId]
    return mapOf(
        "id" to req.id,
        "studentId" to req.studentId,
        "displayName" to (summary?.displayName ?: "Unknown student"),
        "classSection" to (summary?.classSection ?: ""),
        "serviceId" to req.serviceId,
        "statusCss" to req.status.cssSuffix(),
        "statusLabel" to req.status.displayLabel(),
        "attachmentUrl" to req.attachmentUrl,
        "contactInfo" to req.contactInfo,
        "notes" to req.notes,
        "submittedAt" to formatSubmittedTime(req.createdAtMillis),
    )
}

private fun studentSummaryRow(s: Database.StudentSummary): Map<String, Any?> = mapOf(
    "studentId" to s.studentId,
    "displayName" to s.displayName,
    "classSection" to s.classSection,
)

private fun courseAttendanceRow(c: CourseAttendanceDto): Map<String, Any?> = mapOf(
    "courseCode" to c.courseCode,
    "courseName" to c.courseName,
    "attendancePercent" to c.attendancePercent,
    "sessionsAttended" to c.sessionsAttended,
    "sessionsTotal" to c.sessionsTotal,
)

private fun timetableSlotRow(index: Int, slot: TimetableSlotDto): Map<String, Any?> = mapOf(
    "slotIndex" to index,
    "dayOfWeek" to slot.dayOfWeek,
    "dayLabel" to slot.dayLabel,
    "startTime" to slot.startTime,
    "endTime" to slot.endTime,
    "courseCode" to slot.courseCode,
    "courseName" to slot.courseName,
    "roomName" to slot.roomName,
)

private fun transcriptCourseRow(course: TranscriptCourseDto): Map<String, Any?> = mapOf(
    "courseCode" to course.courseCode,
    "courseName" to course.courseName,
    "credits" to course.credits,
    "scorePercent" to course.scorePercent,
    "gradePoint" to course.gradePoint,
)

fun loginTemplateModel(showError: Boolean): Map<String, Any?> =
    mapOf("showError" to showError)

fun dashboardErrorMessage(errorCode: String?): String? = when (errorCode) {
    "missing_fields" -> "Student ID and service are required."
    "student_required" -> "Student ID is required."
    "invalid_attendance" -> "Attendance must be a number between 0 and 100."
    "learning_no_changes" -> "Provide attendance and/or a class notice, or check “Clear schedule notice”."
    "unknown_student" -> "No student found with that ID."
    "invalid_timetable" -> "Timetable times must be HH:mm and each slot needs a course and room."
    "no_catalog_courses" -> "No courses in the catalog. Restart the backend to seed the class catalog."
    "no_registered_courses" -> "Enter attendance for at least one course before saving."
    "message_empty_fields" -> "Message title and body (English) are required."
    "message_unknown_student" -> "That student ID is not in the roster."
    "invalid_grade" -> "Score must be 0–100 and grade point must be ≥ 0."
    else -> null
}

fun dashboardSuccessMessage(successCode: String?): String? = when (successCode) {
    "attendance_saved" -> "Attendance saved and student app notified."
    "timetable_saved" -> "Class schedule saved and student app notified."
    "learning_saved" -> "Learning alerts saved and student app notified."
    "message_sent" -> "Inbox message saved and connected student apps notified."
    "transcript_saved" -> "Transcript saved and student app notified."
    else -> null
}

fun dashboardTemplateModel(
    requests: List<StudentRequest>,
    catalog: List<CatalogServiceDto>,
    rosterStudents: List<Database.StudentSummary>,
    selectedStudentId: String? = null,
    attendanceDetail: StudentAttendanceDetailDto? = null,
    timetable: StudentTimetableDto? = null,
    transcript: StudentTranscriptDto? = null,
    calendarData: Database.AcademicCalendarData? = null,
    classCourses: List<AcademicRepository.ClassCourseOption> = emptyList(),
    errorCode: String? = null,
    successCode: String? = null,
): Map<String, Any?> {
    val studentLookup = rosterStudents.associateBy { it.studentId }
    val byService = requests.groupBy { it.serviceId }
    val catalogById = catalog.associateBy { it.id }

    val servicePanels = catalog.map { svc ->
        val svcRequests = byService[svc.id].orEmpty()
        mapOf(
            "serviceId" to svc.id,
            "title" to svc.title,
            "category" to svc.category,
            "isPopular" to svc.isPopular,
            "requests" to svcRequests.map { requestRow(it, studentLookup) },
            "totalCount" to svcRequests.size,
            "activeCount" to svcRequests.count { it.status != RequestStatus.Completed },
            "pendingCount" to svcRequests.count {
                it.status == RequestStatus.Submitted || it.status == RequestStatus.InReview
            },
        )
    }

    val categories = categoryOrder.mapNotNull { categoryName ->
        val servicesInCategory = servicePanels.filter { it["category"] == categoryName }
        if (servicesInCategory.isEmpty()) return@mapNotNull null
        val categoryRequests = servicesInCategory.sumOf { (it["totalCount"] as Int) }
        val categoryActive = servicesInCategory.sumOf { (it["activeCount"] as Int) }
        mapOf(
            "name" to categoryName,
            "slug" to categoryName.lowercase(),
            "services" to servicesInCategory,
            "serviceCount" to servicesInCategory.size,
            "requestCount" to categoryRequests,
            "activeCount" to categoryActive,
        )
    }

    val orphanServiceIds = byService.keys - catalogById.keys
    val orphanPanels = orphanServiceIds.map { serviceId ->
        val svcRequests = byService[serviceId].orEmpty()
        mapOf(
            "serviceId" to serviceId,
            "title" to serviceId,
            "category" to "Other",
            "isPopular" to false,
            "requests" to svcRequests.map { requestRow(it, studentLookup) },
            "totalCount" to svcRequests.size,
            "activeCount" to svcRequests.count { it.status != RequestStatus.Completed },
            "pendingCount" to svcRequests.count {
                it.status == RequestStatus.Submitted || it.status == RequestStatus.InReview
            },
        )
    }

    val catalogRows = catalog
        .sortedWith(compareBy({ it.category }, { it.title }))
        .map { svc ->
            mapOf("id" to svc.id, "title" to svc.title, "category" to svc.category)
        }

    val selectedSummary = selectedStudentId?.let { studentLookup[it] }
    val weeklyTrend = attendanceDetail?.weeklyTrend.orEmpty().map { w ->
        mapOf("weekLabel" to w.weekLabel, "percent" to w.percent)
    }

    return mapOf(
        "totalRequests" to requests.size,
        "pendingReview" to requests.count { it.status == RequestStatus.Submitted },
        "inReviewCount" to requests.count { it.status == RequestStatus.InReview },
        "actionNeededCount" to requests.count { it.status == RequestStatus.ActionNeeded },
        "completedCount" to requests.count { it.status == RequestStatus.Completed },
        "activeRequests" to requests.count { it.status != RequestStatus.Completed },
        "serviceCount" to catalog.size,
        "categories" to categories,
        "servicePanels" to servicePanels,
        "orphanPanels" to orphanPanels,
        "catalog" to catalogRows,
        "rosterStudents" to rosterStudents.map(::studentSummaryRow),
        "selectedStudentId" to (selectedStudentId ?: ""),
        "selectedStudentName" to (selectedSummary?.displayName ?: ""),
        "selectedStudentClass" to (selectedSummary?.classSection ?: ""),
        "hasSelectedStudent" to (selectedStudentId != null),
        "overallAttendancePercent" to (attendanceDetail?.overallAttendancePercent ?: 0),
        "courseAttendanceRows" to attendanceDetail?.courses.orEmpty().map(::courseAttendanceRow),
        "weeklyTrend" to weeklyTrend,
        "timetableSlots" to timetable?.slots.orEmpty().mapIndexed(::timetableSlotRow),
        "transcriptCourses" to transcript?.courses.orEmpty().map(::transcriptCourseRow),
        "transcriptGpa" to (transcript?.cumulativeGpa ?: 0.0),
        "calendarEvents" to (calendarData?.events ?: emptyList()),
        "classCourses" to classCourses.map { c ->
            mapOf("code" to c.code, "name" to c.nameEn)
        },
        "errorMessage" to dashboardErrorMessage(errorCode),
        "successMessage" to dashboardSuccessMessage(successCode),
    )
}
