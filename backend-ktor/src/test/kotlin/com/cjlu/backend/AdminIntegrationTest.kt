package com.cjlu.backend

import com.cjlu.backend.admin.AdminPaths
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminIntegrationTest {

    @Test
    fun loginPage_rendersFreemarkerTemplate() = testApplication {
        application { module() }
        val response = client.get(AdminPaths.LOGIN)
        assertEquals(HttpStatusCode.OK, response.status)
        val html = response.bodyAsText()
        assertTrue(html.contains("admin.css"))
        assertTrue(html.contains("Administration"))
    }

    @Test
    fun dashboard_requiresSession() = testApplication {
        application { module() }
        val response = createClient { followRedirects = false }.get(AdminPaths.DASHBOARD)
        assertEquals(HttpStatusCode.Found, response.status)
        assertTrue(response.headers["Location"]?.contains(AdminPaths.LOGIN) == true)
    }

    @Test
    fun requestsJson_requiresAdminSession() = testApplication {
        application { module() }
        val response = createClient { followRedirects = false }.get(AdminPaths.API_REQUESTS) {
            header("X-API-Key", Database.getStudentApiKey())
        }
        assertEquals(HttpStatusCode.Found, response.status)
        assertTrue(response.headers["Location"]?.contains(AdminPaths.LOGIN) == true)
    }

    @Test
    fun loginPage_showsErrorQuery() = testApplication {
        application { module() }
        val response = client.get(AdminPaths.LOGIN) {
            parameter("error", "invalid")
        }
        assertTrue(response.bodyAsText().contains("Invalid username or password"))
    }

    @Test
    fun legacyRoot_redirectsToAdminDashboard() = testApplication {
        application { module() }
        val response = createClient { followRedirects = false }.get("/")
        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals(AdminPaths.DASHBOARD, response.headers["Location"])
    }
}
