package com.cjlu.backend

/**
 * When false (default), secrets must come from environment variables and the server refuses
 * implicit hardcoded credentials. Set to true only on trusted local machines.
 */
object DevDefaults {
    val allowInsecure: Boolean =
        System.getenv("CJLU_ALLOW_INSECURE_DEV_DEFAULTS")?.equals("true", ignoreCase = true) == true
}
