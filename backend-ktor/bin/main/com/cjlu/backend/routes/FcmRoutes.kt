package com.cjlu.backend.routes

import com.cjlu.backend.Database
import com.cjlu.backend.FcmTokenRequest
import com.cjlu.backend.OkResponse
import com.cjlu.backend.auth.ensureBearerMatchesStudent
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.fcmRoutes() {
    route("/students/{studentId}/fcm-token") {
        post {
            val studentId = call.parameters["studentId"] ?: ""
            if (!call.ensureBearerMatchesStudent(studentId)) return@post
            val body = try {
                call.receive<FcmTokenRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid body")
                return@post
            }
            val token = body.token.trim()
            if (token.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "Token is required")
                return@post
            }
            Database.upsertFcmToken(studentId, token)
            call.respond(OkResponse())
        }

        delete {
            val studentId = call.parameters["studentId"] ?: ""
            if (!call.ensureBearerMatchesStudent(studentId)) return@delete
            val body = try {
                call.receive<FcmTokenRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid body")
                return@delete
            }
            Database.removeFcmToken(body.token.trim())
            call.respond(OkResponse())
        }
    }
}
