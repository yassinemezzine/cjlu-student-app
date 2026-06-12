package com.cjlu.backend.routes

import com.cjlu.backend.AcademicRepository
import com.cjlu.backend.auth.ensureBearerMatchesStudent
import com.cjlu.backend.auth.preferChinese
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.route

private suspend fun RoutingContext.respondIfStudentIdMissing(studentId: String?): String? {
    val trimmed = studentId?.trim()
    if (trimmed.isNullOrBlank()) {
        call.respond(HttpStatusCode.BadRequest, "Missing studentId")
        return null
    }
    return trimmed
}

fun Route.academicRoutes() {
    route("/students/{studentId}/academic") {
        get("/attendance") {
            val studentId = respondIfStudentIdMissing(call.parameters["studentId"]) ?: return@get
            if (!call.ensureBearerMatchesStudent(studentId)) return@get
            val detail = AcademicRepository.getAttendanceDetail(studentId, preferChinese(call))
            if (detail == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.response.header("X-CJLU-Data-Source", AcademicRepository.getAcademicSource(studentId))
            call.respond(detail)
        }
        get("/transcript") {
            val studentId = respondIfStudentIdMissing(call.parameters["studentId"]) ?: return@get
            if (!call.ensureBearerMatchesStudent(studentId)) return@get
            val transcript = AcademicRepository.getTranscript(studentId, preferChinese(call))
            if (transcript == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.response.header("X-CJLU-Data-Source", AcademicRepository.getAcademicSource(studentId))
            call.respond(transcript)
        }
        get("/timetable") {
            val studentId = respondIfStudentIdMissing(call.parameters["studentId"]) ?: return@get
            if (!call.ensureBearerMatchesStudent(studentId)) return@get
            val timetable = AcademicRepository.getTimetable(studentId, preferChinese(call))
            if (timetable == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.response.header("X-CJLU-Data-Source", AcademicRepository.getAcademicSource(studentId))
            call.respond(timetable)
        }
        get("/calendar") {
            val studentId = respondIfStudentIdMissing(call.parameters["studentId"]) ?: return@get
            if (!call.ensureBearerMatchesStudent(studentId)) return@get
            call.response.header("X-CJLU-Data-Source", AcademicRepository.getAcademicSource(studentId))
            call.respond(AcademicRepository.getAcademicCalendar(preferChinese(call)))
        }
    }

    get("/students/{studentId}/dormitory") {
        val studentId = respondIfStudentIdMissing(call.parameters["studentId"]) ?: return@get
        if (!call.ensureBearerMatchesStudent(studentId)) return@get
        val dorm = AcademicRepository.getDormitory(studentId)
        if (dorm == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        call.response.header("X-CJLU-Data-Source", AcademicRepository.getAcademicSource(studentId))
        call.respond(dorm)
    }
}
