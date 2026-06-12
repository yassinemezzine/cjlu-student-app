package com.cjlu.backend.routes

import com.cjlu.backend.Database
import com.cjlu.backend.MessageReadBody
import com.cjlu.backend.OkResponse
import com.cjlu.backend.auth.ensureBearerMatchesStudent
import com.cjlu.backend.auth.preferChinese
import com.cjlu.backend.websocket.WebSocketHub
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.messageRoutes() {
    route("/students/{studentId}/messages") {
        get {
            val studentId = call.parameters["studentId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            if (!call.ensureBearerMatchesStudent(studentId)) return@get
            call.respond(Database.listMessagesForStudent(studentId, preferChinese(call)))
        }
        post("/{messageId}/read") {
            val studentId = call.parameters["studentId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val messageId = call.parameters["messageId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            if (!call.ensureBearerMatchesStudent(studentId)) return@post
            val read = try {
                call.receive<MessageReadBody>().read
            } catch (_: Exception) {
                true
            }
            if (!Database.markMessageRead(studentId, messageId, read)) {
                call.respond(HttpStatusCode.NotFound, "Unknown message")
                return@post
            }
            WebSocketHub.notifyMessagesChanged(studentId)
            call.respond(HttpStatusCode.OK, OkResponse())
        }
        patch("/{messageId}/read") {
            val studentId = call.parameters["studentId"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
            val messageId = call.parameters["messageId"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
            if (!call.ensureBearerMatchesStudent(studentId)) return@patch
            val read = try {
                call.receive<MessageReadBody>().read
            } catch (_: Exception) {
                true
            }
            if (!Database.markMessageRead(studentId, messageId, read)) {
                call.respond(HttpStatusCode.NotFound, "Unknown message")
                return@patch
            }
            WebSocketHub.notifyMessagesChanged(studentId)
            call.respond(HttpStatusCode.OK, OkResponse())
        }
    }
}
