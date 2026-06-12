package com.cjlu.backend.auth

import com.cjlu.backend.Database
import com.cjlu.backend.JwtHelper
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respond

suspend fun ApplicationCall.ensureApiKey(): Boolean {
    if (Database.validateStudentApiKey(request.header("X-API-Key"))) return true
    respond(HttpStatusCode.Unauthorized, "Invalid or missing API key")
    return false
}

fun ApplicationCall.bearerStudentId(): String? {
    val auth = request.headers[HttpHeaders.Authorization] ?: return null
    if (!auth.startsWith("Bearer ", ignoreCase = true)) return null
    val raw = auth.substring(7).trim()
    return JwtHelper.verifySubject(raw)
}

suspend fun ApplicationCall.ensureBearerMatchesStudent(studentId: String): Boolean {
    if (!ensureApiKey()) return false
    val sub = bearerStudentId()
    if (sub == null) {
        respond(HttpStatusCode.Unauthorized, "Invalid or missing bearer token")
        return false
    }
    if (sub != studentId.trim()) {
        respond(HttpStatusCode.Forbidden, "Token does not match this student")
        return false
    }
    return true
}
