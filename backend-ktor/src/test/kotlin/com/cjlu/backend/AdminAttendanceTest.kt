package com.cjlu.backend

import com.cjlu.backend.admin.AdminPaths
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminAttendanceTest {

    private val studentId = "20230901"

    @Test
    fun attendancePanel_rendersDefaultCoursesForLoggedInAdmin() = testApplication {
        application { module() }
        val client = createClient { followRedirects = false }
        val login = client.submitForm(
            url = AdminPaths.LOGIN,
            formParameters = Parameters.build {
                append("username", "admin")
                append("password", "cjlu2026")
            },
        )
        assertEquals(HttpStatusCode.Found, login.status)
        val cookie = login.headers.getAll("Set-Cookie")?.joinToString("; ") { it.substringBefore(';') }.orEmpty()
        val dashboard = client.get("${AdminPaths.DASHBOARD}?student=$studentId&panel=attendance") {
            headers.append("Cookie", cookie)
        }
        assertEquals(HttpStatusCode.OK, dashboard.status)
        val html = dashboard.bodyAsText()
        assertTrue(html.contains("Attendance rate"))
        assertTrue(html.contains("Per-course attendance"))
        assertTrue(html.contains("Data Structures"))
        assertFalse(html.contains("Generate registered courses"))
        assertFalse(html.contains("Fill random attendance"))
        assertFalse(html.contains("Preview random values"))
    }
}
