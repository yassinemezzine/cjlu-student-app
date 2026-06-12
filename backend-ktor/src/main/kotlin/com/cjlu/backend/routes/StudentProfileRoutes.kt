package com.cjlu.backend.routes

import com.cjlu.backend.Database
import com.cjlu.backend.PanValidation
import com.cjlu.backend.PatchProfileRequest
import com.cjlu.backend.auth.ensureBearerMatchesStudent
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route

fun Route.studentProfileRoutes() {
    route("/students/{studentId}/profile") {
        get {
            val studentId = call.parameters["studentId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            if (!call.ensureBearerMatchesStudent(studentId)) return@get
            val profile = Database.getStudentProfile(studentId)
            if (profile == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respond(profile)
        }
        patch {
            val studentId = call.parameters["studentId"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
            if (!call.ensureBearerMatchesStudent(studentId)) return@patch
            val body = try {
                call.receive<PatchProfileRequest>()
            } catch (e: Exception) {
                call.application.log.error("patch profile", e)
                call.respond(HttpStatusCode.BadRequest, "Invalid body")
                return@patch
            }
            if (PanValidation.containsLikelyPan(body.major + "\n" + body.school)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Do not enter full card numbers or long digit sequences in profile fields.",
                )
                return@patch
            }
            val updated = Database.patchStudentProfile(studentId, body.major, body.school)
            if (updated == null) {
                call.respond(HttpStatusCode.NotFound)
                return@patch
            }
            call.respond(updated)
        }
    }
}
