package com.cjlu.backend

import com.cjlu.backend.plugins.configureAdminTemplates
import com.cjlu.backend.plugins.configureSecurity
import com.cjlu.backend.plugins.configureSerialization
import com.cjlu.backend.plugins.configureStatusPages
import com.cjlu.backend.plugins.configureWebSockets
import com.cjlu.backend.admin.AdminPaths
import com.cjlu.backend.routes.academicRoutes
import com.cjlu.backend.routes.adminRoutes
import com.cjlu.backend.routes.authRoutes
import com.cjlu.backend.routes.fcmRoutes
import com.cjlu.backend.routes.messageRoutes
import com.cjlu.backend.routes.requestRoutes
import com.cjlu.backend.routes.serviceCatalogRoutes
import com.cjlu.backend.routes.studentProfileRoutes
import com.cjlu.backend.routes.uploadRoutes
import com.cjlu.backend.routes.webSocketRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // Force early initialization of the Database singleton to connect and seed on startup
    Database

    configureSerialization()
    configureSecurity()
    configureWebSockets()
    configureStatusPages()
    configureAdminTemplates()

    routing {
        get("/") {
            call.respondRedirect(AdminPaths.DASHBOARD)
        }
        staticResources("/admin/static", "admin/static")
        adminRoutes()

        authRoutes()
        serviceCatalogRoutes()
        studentProfileRoutes()
        academicRoutes()
        messageRoutes()
        fcmRoutes()
        requestRoutes()
        uploadRoutes()
        webSocketRoutes()
    }
}
