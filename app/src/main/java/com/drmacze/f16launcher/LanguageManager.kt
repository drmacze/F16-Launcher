package com.drmacze.f16launcher

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * Central locale preference for DLavie Launcher.
 *
 * AUTO and MANUAL are stored independently. The previous implementation saved
 * the auto-detected code into the same key as a manual choice, which made Auto
 * mode appear disabled immediately after detection. v4 keeps the preference
 * mode explicit and resolves the current device locale only when needed.
 */
object LanguageManager {
    private const val PREFS_NAME = "dlavie_lang"
    private const val KEY_MODE = "preference_mode"
    private const val KEY_MANUAL_LANGUAGE = "manual_language_code"
    private const val LEGACY_KEY_LANGUAGE = "language_code"

    enum class PreferenceMode { AUTO, MANUAL }

    enum class SupportedLanguage(
        val code: String,
        val displayName: String,
        val nativeName: String,
        val flag: String,
        val rtl: Boolean = false,
    ) {
        ENGLISH("en", "English", "English", "🇬🇧"),
        INDONESIAN("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
        MALAY("ms", "Malay", "Bahasa Melayu", "🇲🇾"),
        PORTUGUESE("pt", "Portuguese", "Português", "🇧🇷"),
        SPANISH("es", "Spanish", "Español", "🇪🇸"),
        GERMAN("de", "German", "Deutsch", "🇩🇪"),
        FRENCH("fr", "French", "Français", "🇫🇷"),
        JAPANESE("ja", "Japanese", "日本語", "🇯🇵"),
        CHINESE("zh", "Chinese", "中文", "🇨🇳"),
        ARABIC("ar", "Arabic", "العربية", "🇸🇦", rtl = true),
    }

    data class LanguagePreference(
        val mode: PreferenceMode,
        val resolvedCode: String,
        val manualCode: String?,
    )

    private val supportedByCode = SupportedLanguage.entries.associateBy { it.code }

    fun normalizeLanguageCode(value: String?): String? {
        val normalized = value
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace('_', '-')
            ?.substringBefore('-')
            .orEmpty()
        return normalized.takeIf { it in supportedByCode }
    }

    /** Resolve the physical device language without mutating preferences. */
    fun autoDetectLanguage(context: Context? = null): String {
        val locales = context?.resources?.configuration?.locales
        val candidates = buildList {
            if (locales != null) {
                for (index in 0 until locales.size()) add(locales[index].language)
            }
            add(Locale.getDefault().language)
        }
        return candidates.firstNotNullOfOrNull(::normalizeLanguageCode)
            ?: SupportedLanguage.ENGLISH.code
    }

    private fun migrateLegacyPreference(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_MODE)) return
        val legacy = normalizeLanguageCode(prefs.getString(LEGACY_KEY_LANGUAGE, null))
        prefs.edit().apply {
            if (legacy != null) {
                putString(KEY_MODE, PreferenceMode.MANUAL.name)
                putString(KEY_MANUAL_LANGUAGE, legacy)
            } else {
                putString(KEY_MODE, PreferenceMode.AUTO.name)
            }
            remove(LEGACY_KEY_LANGUAGE)
        }.apply()
    }

    fun getPreference(context: Context): LanguagePreference {
        migrateLegacyPreference(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = runCatching {
            PreferenceMode.valueOf(prefs.getString(KEY_MODE, PreferenceMode.AUTO.name)!!)
        }.getOrDefault(PreferenceMode.AUTO)
        val manual = normalizeLanguageCode(prefs.getString(KEY_MANUAL_LANGUAGE, null))
        val resolved = if (mode == PreferenceMode.MANUAL && manual != null) {
            manual
        } else {
            autoDetectLanguage(context)
        }
        return LanguagePreference(mode, resolved, manual)
    }

    fun getCurrentLanguage(context: Context): String = getPreference(context).resolvedCode

    fun isAutoDetected(context: Context): Boolean =
        getPreference(context).mode == PreferenceMode.AUTO

    fun setLanguage(context: Context, languageCode: String) {
        val normalized = normalizeLanguageCode(languageCode)
            ?: throw IllegalArgumentException("Unsupported locale: $languageCode")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, PreferenceMode.MANUAL.name)
            .putString(KEY_MANUAL_LANGUAGE, normalized)
            .remove(LEGACY_KEY_LANGUAGE)
            .apply()
    }

    fun resetToAutoDetect(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, PreferenceMode.AUTO.name)
            .remove(KEY_MANUAL_LANGUAGE)
            .remove(LEGACY_KEY_LANGUAGE)
            .apply()
    }

    /** Adopt the account preference only when this device still follows Auto. */
    fun adoptAccountPreference(context: Context, preferredLocale: String?): Boolean {
        val normalized = normalizeLanguageCode(preferredLocale) ?: return false
        if (!isAutoDetected(context)) return false
        setLanguage(context, normalized)
        return true
    }

    /** Value written to profiles.preferred_locale; null means follow device. */
    fun getProfilePreference(context: Context): String? {
        val preference = getPreference(context)
        return if (preference.mode == PreferenceMode.AUTO) null else preference.manualCode
    }

    fun getCurrentLanguageName(context: Context): String =
        supportedByCode[getCurrentLanguage(context)]?.nativeName ?: SupportedLanguage.ENGLISH.nativeName

    fun getSupportedLanguages(): List<SupportedLanguage> = SupportedLanguage.entries.toList()

    fun isRtl(languageCode: String): Boolean = supportedByCode[languageCode]?.rtl == true

    /** Apply locale and layout direction before Activity composition begins. */
    fun applyLocale(context: Context): Context {
        val languageCode = getCurrentLanguage(context)
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(config)
    }
}
