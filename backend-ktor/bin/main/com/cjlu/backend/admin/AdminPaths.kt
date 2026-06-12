package com.cjlu.backend.admin

/** Canonical URLs for the staff administration portal. */
object AdminPaths {
    const val BASE = "/admin"
    const val LOGIN = "$BASE/login"
    const val LOGOUT = "$BASE/logout"
    const val DASHBOARD = BASE
    const val API_REQUESTS = "$BASE/api/requests"
    const val FORM_REQUESTS = "$BASE/requests"
    const val FORM_STUDENT_LEARNING = "$BASE/student-learning"
    const val FORM_STUDENT_ATTENDANCE = "$BASE/student-attendance"
    const val FORM_STUDENT_TIMETABLE = "$BASE/student-timetable"

    fun requestStatus(id: String): String = "$API_REQUESTS/$id/status"

    fun dashboardWithStudent(studentId: String): String = "$DASHBOARD?student=${studentId.trim()}"

    fun dashboardAttendance(studentId: String): String = "${dashboardWithStudent(studentId)}#attendance"
}
