package com.drmacze.f16launcher

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Security boundary for Portal and OAuth login flows.
 *
 * Important rules:
 * - Never accept access or refresh tokens from dlavie://connect links.
 * - OAuth callbacks are accepted only shortly after the launcher starts OAuth.
 * - A JWT is never trusted from its payload alone; it must also pass /auth/v1/user.
 */
object PortalAuthSecurity {
    private const val OAUTH_PREFS = "dlavie_portal_security"
    private const val OAUTH_STARTED_AT = "oauth_started_at"
    private const val OAUTH_WINDOW_MS = 10 * 60 * 1000L
    private const val CLOCK_SKEW_SECONDS = 60L

    data class VerifiedUser(
        val id: String,
        val email: String,
        val expiresAtSeconds: Long
    )

    fun markOAuthAttempt(context: Context) {
        context.getSharedPreferences(OAUTH_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(OAUTH_STARTED_AT, System.currentTimeMillis())
            .apply()
    }

    /**
     * Consumes the pending OAuth marker so the same callback cannot be replayed.
     */
    fun consumeOAuthAttempt(context: Context): Boolean {
        val prefs = context.getSharedPreferences(OAUTH_PREFS, Context.MODE_PRIVATE)
        val startedAt = prefs.getLong(OAUTH_STARTED_AT, 0L)
        prefs.edit().remove(OAUTH_STARTED_AT).apply()
        if (startedAt <= 0L) return false
        val age = System.currentTimeMillis() - startedAt
        return age in 0..OAUTH_WINDOW_MS
    }

    fun isTrustedAuthCallback(uri: android.net.Uri?): Boolean =
        uri != null &&
            uri.scheme.equals("dlavie", ignoreCase = true) &&
            uri.host.equals("auth-callback", ignoreCase = true)

    fun containsLegacyPortalSecrets(uri: android.net.Uri?): Boolean {
        if (uri == null) return false
        val secretKeys = listOf("token", "uid", "refresh", "access_token", "refresh_token")
        return secretKeys.any { !uri.getQueryParameter(it).isNullOrBlank() }
    }

    /**
     * Local checks are only an early rejection layer. verifySession() remains mandatory
     * before persisting a newly received OAuth session.
     */
    fun isJwtUsable(accessToken: String, nowSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        val claims = decodeClaims(accessToken) ?: return false
        val issuer = claims.optString("iss", "")
        val subject = claims.optString("sub", "")
        val role = claims.optString("role", "")
        val expiresAt = claims.optLong("exp", 0L)
        val expectedIssuer = BuildConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1"

        if (issuer != expectedIssuer) return false
        if (subject.isBlank()) return false
        if (expiresAt <= nowSeconds + CLOCK_SKEW_SECONDS) return false
        if (role.isNotBlank() && role != "authenticated") return false

        val audienceValid = when (val audience = claims.opt("aud")) {
            is String -> audience == "authenticated"
            is JSONArray -> (0 until audience.length()).any {
                audience.optString(it) == "authenticated"
            }
            else -> false
        }
        return audienceValid
    }

    /**
     * Verifies the token against Supabase Auth and cross-checks the returned user ID
     * with the signed JWT subject. Returns null for every invalid or failed response.
     */
    fun verifySession(accessToken: String): VerifiedUser? {
        if (!isJwtUsable(accessToken)) return null
        val claims = decodeClaims(accessToken) ?: return null
        val expectedUserId = claims.optString("sub", "")
        val expiresAt = claims.optLong("exp", 0L)

        val connection = (URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1/user")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "DLavie-Launcher/${BuildConfig.VERSION_NAME}")
        }

        return try {
            val status = connection.responseCode
            if (status !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val user = JSONObject(body)
            val userId = user.optString("id", "")
            if (userId.isBlank() || userId != expectedUserId) return null
            VerifiedUser(
                id = userId,
                email = user.optString("email", ""),
                expiresAtSeconds = expiresAt
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences("dlavie_auth_session", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        context.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE)
            .edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("user_id")
            .remove("portal_connected")
            .remove("portal_connected_at")
            .apply()
    }

    private fun decodeClaims(token: String): JSONObject? = try {
        val payload = token.split('.').getOrNull(1) ?: return null
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP)
        JSONObject(String(decoded, Charsets.UTF_8))
    } catch (_: Exception) {
        null
    }
}
