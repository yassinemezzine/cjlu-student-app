package com.cjlu.backend.admin.routes

import com.cjlu.backend.Database
import com.cjlu.backend.RequestStatus
import com.cjlu.backend.admin.AdminPaths
import com.cjlu.backend.admin.service.RequestSubmissionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route

fun Route.adminApiRoutes() {
    route(AdminPaths.BASE) {
    authenticate("auth-session") {
        route("api/requests") {
            get {
                call.respond(Database.getAllRequests())
            }

            patch("/{id}/status") {
                val id = call.parameters["id"]
                    ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val newStatus = try {
                    RequestStatus.valueOf(call.receiveParameters()["status"] ?: "")
                } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, "Invalid status")
                }
                if (RequestSubmissionService.updateStatus(id, newStatus)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
    }
}
