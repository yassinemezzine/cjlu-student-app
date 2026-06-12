package com.cjlu.backend

import com.cjlu.contract.LoginResponse
import com.cjlu.contract.RequestStatus
import com.cjlu.contract.StudentRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class UploadAuthTest {

    private val apiKey = "cjlu-insecure-local-student-api-key-do-not-use-in-production"
    private val studentId = "20230901"
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun uploads_requireAuth() = testApplication {
        application { module() }
        val response = client.get("/uploads/nonexistent.bin")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun studentCanDownloadOwnAttachment_notOthers() = testApplication {
        application { module() }
        val uploads = File("uploads").apply { mkdirs() }
        val fileName = "contract-test-${System.currentTimeMillis()}.txt"
        val file = File(uploads, fileName)
        file.writeText("hello")

        val relativeUrl = "/uploads/$fileName"
        Database.addRequest(
            StudentRequest(
                id = "CJLU-upload-test-${System.currentTimeMillis()}",
                serviceId = "visa_extension",
                studentId = studentId,
                contactInfo = "test",
                notes = "upload test",
                status = RequestStatus.Submitted,
                createdAtMillis = System.currentTimeMillis(),
                attachmentUrl = relativeUrl,
            ),
        )

        val token = loginToken(studentId)
        val ok = client.get("/uploads/$fileName") {
            header("X-API-Key", apiKey)
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, ok.status)
        assertTrue(ok.bodyAsText().contains("hello"))

        val otherToken = loginToken("20230928")
        val denied = client.get("/uploads/$fileName") {
            header("X-API-Key", apiKey)
            header(HttpHeaders.Authorization, "Bearer $otherToken")
        }
        assertEquals(HttpStatusCode.Forbidden, denied.status)

        file.delete()
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.loginToken(id: String): String {
        val response = client.post("/auth/login") {
            header("X-API-Key", apiKey)
            contentType(ContentType.Application.Json)
            setBody("""{"studentId":"$id","password":"$id"}""")
        }
        return json.decodeFromString<LoginResponse>(response.bodyAsText()).token
    }
}
