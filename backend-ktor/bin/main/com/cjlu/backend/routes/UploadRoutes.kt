package com.cjlu.backend.routes

import com.cjlu.backend.Database
import com.cjlu.backend.admin.AdminSession
import com.cjlu.backend.auth.bearerStudentId
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import java.io.File

private val uploadsRoot: File
    get() = File("uploads").apply { mkdirs() }

private fun isValidUploadName(rawName: String): Boolean {
    return rawName.isNotEmpty() && !rawName.contains("..") && !rawName.contains('/')
}

private fun unauthorizedMessage(studentIdPresent: Boolean, apiKeyValid: Boolean): String {
    return when {
        !studentIdPresent -> "Invalid or missing bearer token"
        !apiKeyValid -> "Invalid or missing API key"
        else -> "You do not have access to this file"
    }
}

fun Route.uploadRoutes() {
    get("/uploads/{fileName}") {
        val rawName = call.parameters["fileName"]?.trim().orEmpty()
        if (!isValidUploadName(rawName)) {
            call.respondText("Invalid file name", status = HttpStatusCode.BadRequest)
            return@get
        }

        val relativeUrl = "/uploads/$rawName"
        val admin = call.sessions.get<AdminSession>()
        val studentId = call.bearerStudentId()
        val apiKey = call.request.headers["X-API-Key"]
        val apiKeyValid = Database.validateStudentApiKey(apiKey)
        val allowed =
            when {
                admin != null -> Database.attachmentExists(relativeUrl)
                studentId != null && apiKeyValid -> Database.studentOwnsAttachment(studentId, relativeUrl)
                else -> false
            }
        if (!allowed) {
            val status = when {
                admin == null && studentId == null -> HttpStatusCode.Unauthorized
                admin == null && !apiKeyValid -> HttpStatusCode.Unauthorized
                else -> HttpStatusCode.Forbidden
            }
            call.respondText(
                unauthorizedMessage(studentId != null, apiKeyValid),
                status = status,
            )
            return@get
        }

        val file = File(uploadsRoot, rawName).canonicalFile
        val root = uploadsRoot.canonicalFile
        if (!file.path.startsWith(root.path + File.separator)) {
            call.respondText("Invalid file path", status = HttpStatusCode.Forbidden)
            return@get
        }
        if (!file.isFile) {
            call.respondText("File not found", status = HttpStatusCode.NotFound)
            return@get
        }
        call.respondFile(file)
    }
}
