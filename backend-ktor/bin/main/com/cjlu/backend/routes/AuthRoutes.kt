package com.cjlu.backend.routes

import com.cjlu.backend.ChangePasswordRequest
import com.cjlu.backend.Database
import com.cjlu.backend.JwtHelper
import com.cjlu.backend.LoginRequest
import com.cjlu.backend.LoginResponse
import com.cjlu.backend.OkResponse
import com.cjlu.backend.auth.bearerStudentId
import com.cjlu.backend.auth.ensureApiKey
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post

private suspend fun RoutingContext.requireStudentTokenOrRespond(): String? {
    val studentId = call.bearerStudentId()
    if (studentId == null) {
        call.respond(HttpStatusCode.Unauthorized, "Invalid or missing bearer token")
    }
    return studentId
}

private suspend inline fun <reified T : Any> RoutingContext.receiveBodyOrRespond(endpointName: String): T? {
    return try {
        call.receive<T>()
    } catch (e: Exception) {
        call.application.log.error("$endpointName parse", e)
        call.respond(HttpStatusCode.BadRequest, "Invalid body")
        null
    }
}

fun Route.authRoutes() {
    post("/auth/login") {
        if (!call.ensureApiKey()) return@post
        val body = receiveBodyOrRespond<LoginRequest>("login") ?: return@post
        val profile = Database.login(body.studentId.trim(), body.password)
        if (profile == null) {
            call.respond(HttpStatusCode.Unauthorized, "Invalid student ID or password")
            return@post
        }
        val token = JwtHelper.issueToken(profile.studentId)
        call.respond(LoginResponse(token = token, profile = profile))
    }

    post("/auth/change-password") {
        if (!call.ensureApiKey()) return@post
        val sub = requireStudentTokenOrRespond() ?: return@post
        val body = receiveBodyOrRespond<ChangePasswordRequest>("change-password") ?: return@post
        if (body.newPassword.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, "New password must not be blank")
            return@post
        }
        if (!Database.changePassword(sub, body.currentPassword, body.newPassword)) {
            call.respond(HttpStatusCode.BadRequest, "Current password incorrect")
            return@post
        }
        call.respond(HttpStatusCode.OK, OkResponse())
    }

    get("/auth/me") {
        if (!call.ensureApiKey()) return@get
        val sub = requireStudentTokenOrRespond() ?: return@get
        val profile = Database.getStudentProfile(sub)
        if (profile == null) {
            call.respond(HttpStatusCode.NotFound, "Student not found")
            return@get
        }
        call.respond(profile)
    }
}
