package com.cjlu.studentapp.localization

import androidx.annotation.StringRes
import androidx.core.os.LocaleListCompat
import com.cjlu.core.resources.R

enum class AppLanguage(
    val storageValue: String,
    val languageTag: String?,
    @param:StringRes val labelRes: Int
) {
    SYSTEM(
        storageValue = "system",
        languageTag = null,
        labelRes = R.string.language_option_system
    ),
    ENGLISH(
        storageValue = "en",
        languageTag = "en",
        labelRes = R.string.language_option_english
    ),
    CHINESE(
        storageValue = "zh",
        languageTag = "zh",
        labelRes = R.string.language_option_chinese
    );

    fun toLocaleList(): LocaleListCompat {
        return if (languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
    }

    companion object {
        fun fromStorageValue(value: String?): AppLanguage {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}
