package com.cjlu.studentapp.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object LanguageManager {
    private const val PREFS_NAME = "cjlu_language_preferences"
    private const val KEY_APP_LANGUAGE = "app_language"

    fun getSavedLanguage(context: Context): AppLanguage {
        val storedValue = context
            .applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.storageValue)

        return AppLanguage.fromStorageValue(storedValue)
    }

    fun applyLanguage(language: AppLanguage) {
        val targetLocales = language.toLocaleList()
        val currentLocales = AppCompatDelegate.getApplicationLocales()

        if (currentLocales.toLanguageTags() != targetLocales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(targetLocales)
        }
    }

    fun saveAndApplyLanguage(context: Context, language: AppLanguage) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_APP_LANGUAGE, language.storageValue)
            }

        applyLanguage(language)
    }
}
