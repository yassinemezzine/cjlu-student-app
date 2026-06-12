package com.cjlu.backend.routes

import com.cjlu.backend.Database
import com.cjlu.backend.JwtHelper
import com.cjlu.backend.websocket.WebSocketHub
import io.ktor.http.HttpHeaders
import io.ktor.server.request.header
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import java.util.Collections

fun Route.webSocketRoutes() {
    webSocket("/updates/{studentId}") {
        if (!Database.validateStudentApiKey(call.request.header("X-API-Key"))) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid or missing API key"))
            return@webSocket
        }
        val studentId = call.parameters["studentId"] ?: return@webSocket
        val authHeader = call.request.headers[HttpHeaders.Authorization]
        val token = authHeader?.removePrefix("Bearer ")?.trim()?.removePrefix("bearer ")?.trim()
        val sub = token?.let { JwtHelper.verifySubject(it) }
        if (sub == null || sub != studentId.trim()) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid or missing bearer token"))
            return@webSocket
        }
        val userSessions = WebSocketHub.sessions.computeIfAbsent(studentId) {
            Collections.synchronizedSet(LinkedHashSet<WebSocketSession>())
        }
        userSessions.add(this)
        try {
            for (frame in incoming) {
                // Keep alive
            }
        } finally {
            userSessions.remove(this)
        }
    }
}
