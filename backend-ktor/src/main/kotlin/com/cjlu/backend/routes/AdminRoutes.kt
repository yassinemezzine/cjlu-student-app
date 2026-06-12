package com.cjlu.backend.routes

import com.cjlu.backend.admin.routes.adminApiRoutes
import com.cjlu.backend.admin.routes.adminPageRoutes
import io.ktor.server.routing.Route

fun Route.adminRoutes() {
    adminPageRoutes()
    adminApiRoutes()
}
