package com.cjlu.backend.admin.routes

import com.cjlu.backend.AcademicRepository
import com.cjlu.backend.Database
import com.cjlu.backend.admin.AdminPaths
import com.cjlu.backend.admin.AdminSession
import com.cjlu.backend.admin.dashboardTemplateModel
import com.cjlu.backend.admin.loginTemplateModel
import com.cjlu.backend.admin.service.AdminAcademicService
import com.cjlu.backend.admin.service.AdminInboxService
import com.cjlu.backend.admin.service.AdminLearningService
import com.cjlu.backend.admin.service.RequestSubmissionService
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.freemarker.FreeMarkerContent
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

private fun ApplicationCall.buildDashboardModel(
    errorCode: String? = null,
    successCode: String? = null,
    selectedStudentId: String? = null,
) = dashboardTemplateModel(
    requests = Database.getAllRequests(),
    catalog = Database.listCatalogServices(preferChinese = false),
    rosterStudents = Database.listStudentSummaries(),
    selectedStudentId = selectedStudentId?.trim()?.takeIf { it.isNotEmpty() },
    attendanceDetail = selectedStudentId?.trim()?.takeIf { it.isNotEmpty() }?.let {
        AcademicRepository.getAttendanceDetail(it, preferChinese = false)
    },
    timetable = selectedStudentId?.trim()?.takeIf { it.isNotEmpty() }?.let {
        AcademicRepository.getTimetable(it, preferChinese = false)
    },
    calendarData = Database.getAcademicCalendarData(),
    classCourses = AcademicRepository.listClassCourses(),
    errorCode = errorCode,
    successCode = successCode,
)

fun Route.adminPageRoutes() {
    route(AdminPaths.BASE) {
    get("login") {
        val error = call.request.queryParameters["error"]
        call.respond(FreeMarkerContent("login.ftl", loginTemplateModel(error != null)))
    }

    authenticate("auth-form") {
        post("login") {
            val principal = call.principal<UserIdPrincipal>()
            if (principal != null) {
                call.sessions.set(AdminSession(principal.name))
                call.respondRedirect(AdminPaths.DASHBOARD)
            }
        }
    }

    get("logout") {
        call.sessions.clear<AdminSession>()
        call.respondRedirect(AdminPaths.LOGIN)
    }

    authenticate("auth-session") {
        get {
            val errorCode = call.request.queryParameters["error"]
            val successCode = call.request.queryParameters["success"]
            val selectedStudent = call.request.queryParameters["student"]
            call.respond(
                FreeMarkerContent(
                    "dashboard.ftl",
                    call.buildDashboardModel(errorCode, successCode, selectedStudent),
                ),
            )
        }

        post("requests") {
            val params = call.receiveParameters()
            val studentId = params["studentId"]?.trim().orEmpty()
            val serviceId = params["serviceId"]?.trim().orEmpty()
            val contactInfo = params["contactInfo"]?.trim().orEmpty()
            val notes = params["notes"]?.trim().orEmpty()
            if (studentId.isEmpty() || serviceId.isEmpty()) {
                call.respondRedirect("${AdminPaths.DASHBOARD}?error=missing_fields")
                return@post
            }
            RequestSubmissionService.createRequest(
                studentId = studentId,
                serviceId = serviceId,
                contactInfo = contactInfo.ifBlank { "Registered by administration" },
                notes = notes,
            )
            call.respondRedirect(AdminPaths.DASHBOARD)
        }

        post("student-learning") {
            val params = call.receiveParameters()
            val studentId = params["studentId"]?.trim().orEmpty()
            if (studentId.isEmpty()) {
                call.respondRedirect("${AdminPaths.DASHBOARD}?error=student_required")
                return@post
            }
            when (
                AdminLearningService.patchLearningAlerts(
                    studentId = studentId,
                    attendanceRaw = params["attendancePercent"].orEmpty(),
                    classNoticeRaw = params["classUpdateNotice"].orEmpty(),
                    clearClassNotice = params["clearClassNotice"] == "on",
                )
            ) {
                is AdminLearningService.PatchResult.Success ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&success=learning_saved#learning",
                    )
                AdminLearningService.PatchResult.InvalidAttendance ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&error=invalid_attendance#learning",
                    )
                AdminLearningService.PatchResult.NoChanges ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&error=learning_no_changes#learning",
                    )
                AdminLearningService.PatchResult.UnknownStudent ->
                    call.respondRedirect("${AdminPaths.DASHBOARD}?error=unknown_student")
            }
        }

        post("student-attendance") {
            val params = call.receiveParameters()
            val studentId = params["studentId"]?.trim().orEmpty()
            if (studentId.isEmpty()) {
                call.respondRedirect("${AdminPaths.DASHBOARD}?error=student_required")
                return@post
            }
            when (
                AdminAcademicService.saveAttendance(
                    studentId = studentId,
                    courseCodes = params.getAll("courseCode") ?: emptyList(),
                    percents = params.getAll("attendancePercent") ?: emptyList(),
                    sessionsAttended = params.getAll("sessionsAttended") ?: emptyList(),
                    sessionsTotal = params.getAll("sessionsTotal") ?: emptyList(),
                )
            ) {
                is AdminAcademicService.AttendanceResult.Success ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&success=attendance_saved#attendance",
                    )
                AdminAcademicService.AttendanceResult.InvalidPercent ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&error=invalid_attendance#attendance",
                    )
                AdminAcademicService.AttendanceResult.NoCourses ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&error=no_registered_courses#attendance",
                    )
                AdminAcademicService.AttendanceResult.UnknownStudent ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&error=unknown_student#attendance",
                    )
            }
        }

        post("student-timetable") {
            val params = call.receiveParameters()
            val studentId = params["studentId"]?.trim().orEmpty()
            if (studentId.isEmpty()) {
                call.respondRedirect("${AdminPaths.DASHBOARD}?error=student_required")
                return@post
            }
            when (
                AdminAcademicService.saveTimetable(
                    studentId = studentId,
                    dayOfWeek = params.getAll("dayOfWeek") ?: emptyList(),
                    dayLabels = params.getAll("dayLabel") ?: emptyList(),
                    startTimes = params.getAll("startTime") ?: emptyList(),
                    endTimes = params.getAll("endTime") ?: emptyList(),
                    courseCodes = params.getAll("courseCode") ?: emptyList(),
                    roomNames = params.getAll("roomName") ?: emptyList(),
                )
            ) {
                is AdminAcademicService.TimetableResult.Success ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&success=timetable_saved#learning",
                    )
                AdminAcademicService.TimetableResult.InvalidSlots ->
                    call.respondRedirect(
                        "${AdminPaths.dashboardWithStudent(studentId)}&error=invalid_timetable#learning",
                    )
                AdminAcademicService.TimetableResult.UnknownStudent ->
                    call.respondRedirect("${AdminPaths.DASHBOARD}?error=unknown_student")
            }
        }

        post("student-academic-calendar") {
            val params = call.receiveParameters()
            when (
                AdminAcademicService.saveAcademicCalendar(
                    dates = params.getAll("eventDate") ?: emptyList(),
                    titlesEn = params.getAll("eventTitleEn") ?: emptyList(),
                    titlesZh = params.getAll("eventTitleZh") ?: emptyList(),
                    detailsEn = params.getAll("eventDetailEn") ?: emptyList(),
                    detailsZh = params.getAll("eventDetailZh") ?: emptyList(),
                    tones = params.getAll("eventTone") ?: emptyList()
                )
            ) {
                is AdminAcademicService.CalendarResult.Success ->
                    call.respondRedirect("${AdminPaths.DASHBOARD}?success=calendar_saved#calendar")
                AdminAcademicService.CalendarResult.InvalidEvent ->
                    call.respondRedirect("${AdminPaths.DASHBOARD}?error=invalid_calendar_event#calendar")
            }
        }

        post("student-inbox-message") {
            val params = call.receiveParameters()
            val audienceAll = params["audienceAll"] == "on"
            val targetStudent = params["targetStudentId"]?.trim().orEmpty()
            when (
                AdminInboxService.sendFromAdminForm(
                    audienceAll = audienceAll,
                    studentId = targetStudent,
                    categoryRaw = params["messageCategory"].orEmpty(),
                    titleEn = params["messageTitleEn"].orEmpty(),
                    bodyEn = params["messageBodyEn"].orEmpty(),
                    titleZh = params["messageTitleZh"].orEmpty(),
                    bodyZh = params["messageBodyZh"].orEmpty(),
                    requiresAction = params["messageRequiresAction"] == "on",
                    relatedServiceIdRaw = params["messageRelatedServiceId"].orEmpty(),
                )
            ) {
                is AdminInboxService.SendResult.Success ->
                    call.respondRedirect("${AdminPaths.DASHBOARD}?panel=messages&success=message_sent#messages")
                AdminInboxService.SendResult.EmptyFields ->
                    call.respondRedirect("${AdminPaths.DASHBOARD}?panel=messages&error=message_empty_fields#messages")
                AdminInboxService.SendResult.UnknownStudent ->
                    call.respondRedirect("${AdminPaths.DASHBOARD}?panel=messages&error=message_unknown_student#messages")
            }
        }
    }
    }
}
