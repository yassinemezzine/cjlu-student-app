package com.cjlu.backend.plugins

import com.cjlu.backend.DevDefaults
import com.cjlu.backend.admin.AdminPaths
import com.cjlu.backend.admin.AdminSession
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.form
import io.ktor.server.auth.session
import io.ktor.server.response.respondRedirect
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<AdminSession>("ADMIN_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600
        }
    }

    install(Authentication) {
        form("auth-form") {
            userParamName = "username"
            passwordParamName = "password"
            validate { credentials ->
                val envUser = System.getenv("ADMIN_USERNAME")?.trim()?.takeIf { it.isNotEmpty() }
                val envPass = System.getenv("ADMIN_PASSWORD")?.trim()?.takeIf { it.isNotEmpty() }
                val ok =
                    when {
                        envUser != null && envPass != null ->
                            credentials.name == envUser && credentials.password == envPass
                        DevDefaults.allowInsecure ->
                            credentials.name == "admin" && credentials.password == "cjlu2026"
                        else -> false
                    }
                if (ok) UserIdPrincipal(credentials.name) else null
            }
            challenge {
                call.respondRedirect("${AdminPaths.LOGIN}?error=invalid")
            }
        }

        session<AdminSession>("auth-session") {
            validate { session ->
                if (session.username.isNotBlank()) session else null
            }
            challenge {
                call.respondRedirect(AdminPaths.LOGIN)
            }
        }
    }
}
