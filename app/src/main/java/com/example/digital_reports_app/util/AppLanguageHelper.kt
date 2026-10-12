package com.example.digital_reports_app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object AppLanguageHelper {

    fun applyLanguageConfig(context: Context, language: String) {
        val languageCode = if (language.equals("English", ignoreCase = true)) "en" else "es"
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
