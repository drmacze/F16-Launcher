package com.drmacze.f16launcher

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * DLavie Language Manager v2
 *
 * Auto-detects device language on FIRST launch (no toggle needed).
 * User can manually override with language selection in Settings.
 * Once user manually selects a language, that overrides auto-detect.
 *
 * Supported languages:
 * - English (en) — default fallback
 * - Indonesian (id) — auto-detected if device locale is Indonesian
 *
 * Language preference stored in SharedPreferences "dlavie_lang".
 * Key "language_code": null = auto-detect, "en"/"id" = manual override
 */
object LanguageManager {

    private const val PREFS_NAME = "dlavie_lang"
    private const val KEY_LANGUAGE = "language_code"

    enum class SupportedLanguage(val code: String, val displayName: String, val nativeName: String) {
        ENGLISH("en", "English", "English"),
        INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    }

    /**
     * Get current language code. Returns "en" or "id".
     *
     * Logic:
     * 1. If user has manually selected a language → use that
     * 2. Otherwise → auto-detect from device locale
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE, null)

        // Manual override takes priority
        if (savedCode != null) return savedCode

        // Auto-detect from device locale (default behavior, no toggle needed)
        return autoDetectLanguage()
    }

    /**
     * Check if current language is auto-detected (no manual override).
     */
    fun isAutoDetected(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null) == null
    }

    /**
     * Auto-detect device language. Returns "id" if device is Indonesian, else "en".
     */
    fun autoDetectLanguage(): String {
        val deviceLang = Locale.getDefault().language
        return if (deviceLang == "id") {
            SupportedLanguage.INDONESIAN.code
        } else {
            SupportedLanguage.ENGLISH.code
        }
    }

    /**
     * Manually set language (overrides auto-detect).
     */
    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    /**
     * Reset to auto-detect (clears manual override).
     */
    fun resetToAutoDetect(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LANGUAGE)
            .apply()
    }

    /**
     * Get display name for current language.
     */
    fun getCurrentLanguageName(context: Context): String {
        val code = getCurrentLanguage(context)
        return SupportedLanguage.entries.find { it.code == code }?.nativeName ?: "English"
    }

    /**
     * Get all supported languages.
     */
    fun getSupportedLanguages(): List<SupportedLanguage> {
        return SupportedLanguage.entries.toList()
    }

    /**
     * Apply locale to the app context.
     * Call this in Activity's attachBaseContext or onCreate.
     *
     * This actually changes the app's locale so that string resources
     * (if translated) and Locale-sensitive operations use the correct language.
     */
    fun applyLocale(context: Context): Context {
        val langCode = getCurrentLanguage(context)
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
