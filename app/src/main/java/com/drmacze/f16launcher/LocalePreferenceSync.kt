package com.drmacze.f16launcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Best-effort account sync for the shared Portal/Launcher language preference. */
object LocalePreferenceSync {
    private const val AUTH_PREFS = "dlavie_auth_session"
    private const val TOKEN_KEY = "access_token"
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun pushAsync(context: Context) {
        val applicationContext = context.applicationContext
        applicationScope.launch { runCatching { push(applicationContext) } }
    }

    suspend fun push(context: Context): Boolean = withContext(Dispatchers.IO) {
        val token = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, null)
            ?.takeIf { it.isNotBlank() }
            ?: return@withContext false
        val userId = runCatching {
            val payload = token.split('.').getOrNull(1).orEmpty()
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
            JSONObject(String(decoded)).optString("sub")
        }.getOrDefault("")
        if (userId.isBlank()) return@withContext false

        val preferred = LanguageManager.getProfilePreference(context)
        val body = JSONObject().put("preferred_locale", preferred).toString()
        val connection = (URL("${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.$userId")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 12_000
            readTimeout = 12_000
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUB_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }
}
