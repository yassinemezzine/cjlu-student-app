package com.cjlu.backend.routes

import com.cjlu.backend.Database
import com.cjlu.backend.auth.ensureApiKey
import com.cjlu.backend.auth.preferChinese
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.serviceCatalogRoutes() {
    get("/services") {
        if (!call.ensureApiKey()) return@get
        call.respond(Database.listCatalogServices(preferChinese(call)))
    }
}
