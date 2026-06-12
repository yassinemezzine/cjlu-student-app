package com.cjlu.backend.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header

fun preferChinese(call: ApplicationCall): Boolean {
    val q = call.request.queryParameters["lang"]?.lowercase()
    if (q == "zh" || q == "zh-cn" || q == "zh_cn") return true
    if (q == "en") return false
    val accept = call.request.header("Accept-Language") ?: return false
    return accept.lowercase().startsWith("zh")
}
