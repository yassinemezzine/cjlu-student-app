package com.cjlu.backend.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path

/** True for JSON/WebSocket endpoints consumed by the mobile app (not admin HTML). */
fun ApplicationCall.isStudentJsonApi(): Boolean {
    val path = request.path()
    if (path.startsWith("/uploads")) return true
    if (path.startsWith("/admin")) return false
    return path.startsWith("/auth") ||
        path.startsWith("/services") ||
        path.startsWith("/students") ||
        path.startsWith("/requests") ||
        path.startsWith("/updates")
}
