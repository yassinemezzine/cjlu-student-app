package com.cjlu.backend.plugins

import com.cjlu.backend.auth.isStudentJsonApi
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.request.path
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText

fun Application.configureStatusPages() {
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            if (call.isStudentJsonApi()) {
                call.respondText(status.description, status = status)
            } else {
                call.respond(status)
            }
        }
        status(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.BadRequest) { call, status ->
            if (call.isStudentJsonApi()) {
                val message = call.response.headers["X-Error-Message"]
                    ?: status.description
                call.respondText(message, status = status)
            } else {
                call.respond(status)
            }
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error on ${call.request.path()}", cause)
            if (call.isStudentJsonApi()) {
                call.respondText(
                    "Internal server error",
                    status = HttpStatusCode.InternalServerError,
                )
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
