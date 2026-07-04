package com.drmacze.f16launcher

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * DLavie Language Manager
 *
 * Auto-detects device language on first launch.
 * User can manually toggle between supported languages via Settings.
 *
 * Supported languages:
 * - English (en) — default fallback
 * - Indonesian (id) — auto-detected if device locale is Indonesian
 *
 * Language preference stored in SharedPreferences "dlavie_lang".
 */
object LanguageManager {

    private const val PREFS_NAME = "dlavie_lang"
    private const val KEY_LANGUAGE = "language_code"
    private const val KEY_AUTO_DETECT = "auto_detect"

    enum class SupportedLanguage(val code: String, val displayName: String, val nativeName: String) {
        ENGLISH("en", "English", "English"),
        INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    }

    /**
     * Get current language code. Returns "en" or "id".
     * If auto-detect is enabled and no manual override, returns device language.
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoDetect = prefs.getBoolean(KEY_AUTO_DETECT, true)
        val savedCode = prefs.getString(KEY_LANGUAGE, null)

        if (savedCode != null) return savedCode

        // Auto-detect from device locale
        if (autoDetect) {
            return autoDetectLanguage()
        }

        return SupportedLanguage.ENGLISH.code
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
     * Manually set language. Disables auto-detect.
     */
    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .putBoolean(KEY_AUTO_DETECT, false)
            .apply()
    }

    /**
     * Re-enable auto-detect (use device language).
     */
    fun enableAutoDetect(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_DETECT, true)
            .remove(KEY_LANGUAGE)
            .apply()
    }

    /**
     * Check if auto-detect is currently enabled.
     */
    fun isAutoDetectEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_DETECT, true)
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
}
