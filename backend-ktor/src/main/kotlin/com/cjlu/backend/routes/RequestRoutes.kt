package com.cjlu.backend.routes

import com.cjlu.backend.Database
import com.cjlu.backend.RequestStatus
import com.cjlu.backend.RequestSubmission
import com.cjlu.backend.admin.service.RequestSubmissionService
import com.cjlu.backend.auth.bearerStudentId
import com.cjlu.backend.auth.ensureApiKey
import com.cjlu.backend.auth.ensureBearerMatchesStudent
import com.cjlu.backend.websocket.WebSocketHub
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun Route.requestRoutes() {
    route("/students/{studentId}/requests") {
        get {
            val studentId = call.parameters["studentId"]?.trim().orEmpty()
            if (studentId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing studentId")
                return@get
            }
            if (!call.ensureBearerMatchesStudent(studentId)) return@get
            call.respond(Database.getRequestsForStudent(studentId))
        }
    }

    route("/requests") {
        post {
            if (!call.ensureApiKey()) return@post
            val sub = call.bearerStudentId()?.trim().orEmpty()
            if (sub.isBlank()) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid or missing bearer token")
                return@post
            }
            val body = try {
                call.receive<RequestSubmission>()
            } catch (e: Exception) {
                call.application.log.error("request submit parse", e)
                call.respond(HttpStatusCode.BadRequest, "Invalid body")
                return@post
            }
            val studentId = body.studentId.trim()
            val serviceId = body.serviceId.trim()
            val contactInfo = body.contactInfo.trim()
            val notes = body.notes.trim()
            if (studentId.isBlank() || serviceId.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "studentId and serviceId are required")
                return@post
            }
            if (sub != studentId) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    "You can only submit requests for your own student account.",
                )
                return@post
            }
            if (!Database.catalogServiceExists(serviceId)) {
                call.respond(HttpStatusCode.BadRequest, "Unknown service id")
                return@post
            }
            if (contactInfo.length > 120 || notes.length > 1000) {
                call.respond(HttpStatusCode.BadRequest, "Contact info or notes are too long")
                return@post
            }
            val newRequest = RequestSubmissionService.createRequest(
                studentId = studentId,
                serviceId = serviceId,
                contactInfo = contactInfo.ifBlank { "—" },
                notes = notes.ifBlank { "—" },
            )
            if (serviceId == "ask_leave") {
                Database.updateRequestStatus(newRequest.id, RequestStatus.InReview)
            }
            call.respond(Database.getRequestById(newRequest.id) ?: newRequest)
        }

        post("/{id}/upload") {
            if (!call.ensureApiKey()) return@post
            val sub = call.bearerStudentId()
            if (sub == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid or missing bearer token")
                return@post
            }
            val id = call.parameters["id"]?.trim().orEmpty()
            if (id.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "Missing request id")
                return@post
            }
            val existing = Database.getRequestById(id)
            if (existing == null) {
                call.respond(HttpStatusCode.NotFound, "Request not found")
                return@post
            }
            if (existing.studentId != sub) {
                call.respond(HttpStatusCode.Forbidden, "You can only upload files for your own requests.")
                return@post
            }
            if (!existing.attachmentUrl.isNullOrBlank()) {
                call.respond(HttpStatusCode.Conflict, "This request already has an attachment")
                return@post
            }
            val uploadDir = File("uploads").apply { mkdirs() }
            var savedRelativeUrl: String? = null
            call.receiveMultipart().forEachPart { part ->
                try {
                    if (part is PartData.FileItem && savedRelativeUrl == null) {
                        val rawName = part.originalFileName ?: "file"
                        val ext = rawName.substringAfterLast('.', "")
                            .lowercase()
                            .filter { it in 'a'..'z' || it in '0'..'9' }
                            .take(8)
                            .ifEmpty { "bin" }
                        val fname = "${UUID.randomUUID()}.$ext"
                        val dest = File(uploadDir, fname)
                        FileOutputStream(dest).use { out ->
                            part.provider().copyTo(out)
                        }
                        savedRelativeUrl = "/uploads/$fname"
                    }
                } finally {
                    part.dispose()
                }
            }
            val url = savedRelativeUrl
            if (url == null) {
                call.respond(HttpStatusCode.BadRequest, "No file part found")
                return@post
            }
            Database.updateRequestAttachment(id, url)
            val updated = Database.getRequestById(id)
            if (updated != null) {
                WebSocketHub.notifyStudentRequest(updated)
                call.respond(updated)
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Upload saved but request not found")
            }
        }
    }
}
