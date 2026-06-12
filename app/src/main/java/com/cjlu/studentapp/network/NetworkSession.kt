package com.cjlu.studentapp.network

import com.cjlu.studentapp.localization.AppLanguage

object AuthTokenStore {
    @Volatile
    var accessToken: String? = null
}

object ApiLanguageStore {
    @Volatile
    var acceptLanguageHeader: String = "en-US,en;q=0.9"

    fun setFromAppLanguage(language: AppLanguage) {
        acceptLanguageHeader = when (language) {
            AppLanguage.CHINESE -> "zh-CN,zh;q=0.9,en;q=0.8"
            AppLanguage.ENGLISH -> "en-US,en;q=0.9"
            AppLanguage.SYSTEM -> "en-US,en;q=0.9,zh-CN;q=0.8"
        }
    }
}
