package com.cjlu.studentapp.network

object AuthTokenStore {
    @Volatile
    var accessToken: String? = null
}

object ApiLanguageStore {
    @Volatile
    var acceptLanguageHeader: String = "en-US,en;q=0.9"

    fun setFromLanguageTag(languageTag: String?) {
        acceptLanguageHeader = when (languageTag) {
            "zh" -> "zh-CN,zh;q=0.9,en;q=0.8"
            "en" -> "en-US,en;q=0.9"
            else -> "en-US,en;q=0.9,zh-CN;q=0.8"
        }
    }
}
