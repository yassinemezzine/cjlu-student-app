package com.cjlu.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtHelper {
    private const val INSECURE_FALLBACK_JWT_SECRET =
        "cjlu-insecure-local-jwt-secret-do-not-use-in-production-min-32-chars!!"

    private val secret: String =
        run {
            val fromEnv = System.getenv("JWT_SECRET")?.trim()?.takeIf { it.isNotEmpty() }
            when {
                fromEnv != null -> fromEnv
                DevDefaults.allowInsecure -> {
                    System.err.println(
                        "CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true: JWT_SECRET not set; using insecure local fallback.",
                    )
                    INSECURE_FALLBACK_JWT_SECRET
                }
                else -> error(
                    "JWT_SECRET environment variable is required. " +
                        "For a private dev machine only, set CJLU_ALLOW_INSECURE_DEV_DEFAULTS=true.",
                )
            }
        }

    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    private const val TTL_MS: Long = 180L * 24 * 60 * 60 * 1000

    fun issueToken(studentId: String): String =
        JWT.create()
            .withSubject(studentId.trim())
            .withExpiresAt(Date(System.currentTimeMillis() + TTL_MS))
            .sign(algorithm)

    fun verifySubject(token: String): String? =
        try {
            JWT.require(algorithm).build().verify(token.trim()).subject
        } catch (_: Exception) {
            null
        }
}
