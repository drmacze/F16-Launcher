package com.drmacze.f16launcher

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Security boundary for Portal and OAuth login flows.
 *
 * Rules:
 * - dlavie://connect never accepts credentials.
 * - Google OAuth uses Authorization Code + PKCE; the callback only carries a
 *   short-lived code and cannot be exchanged without the device-held verifier.
 * - Every new access token must also pass Supabase /auth/v1/user verification.
 */
object PortalAuthSecurity {
    private const val OAUTH_PREFS = "dlavie_portal_security"
    private const val OAUTH_STARTED_AT = "oauth_started_at"
    private const val OAUTH_CODE_VERIFIER = "oauth_code_verifier"
    private const val OAUTH_WINDOW_MS = 10 * 60 * 1000L
    private const val CLOCK_SKEW_SECONDS = 60L

    data class VerifiedUser(
        val id: String,
        val email: String,
        val expiresAtSeconds: Long
    )

    data class VerifiedSession(
        val accessToken: String,
        val refreshToken: String,
        val user: VerifiedUser
    )

    /**
     * Starts a single PKCE transaction and returns the S256 code challenge.
     */
    fun beginOAuthAttempt(context: Context): String {
        val verifierBytes = ByteArray(64).also { SecureRandom().nextBytes(it) }
        val verifier = Base64.encodeToString(
            verifierBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val challengeBytes = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = Base64.encodeToString(
            challengeBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        context.getSharedPreferences(OAUTH_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(OAUTH_STARTED_AT, System.currentTimeMillis())
            .putString(OAUTH_CODE_VERIFIER, verifier)
            .apply()
        return challenge
    }

    /**
     * Returns and removes the verifier. The same callback therefore cannot be replayed.
     */
    fun consumeOAuthVerifier(context: Context): String? {
        val prefs = context.getSharedPreferences(OAUTH_PREFS, Context.MODE_PRIVATE)
        val startedAt = prefs.getLong(OAUTH_STARTED_AT, 0L)
        val verifier = prefs.getString(OAUTH_CODE_VERIFIER, null)
        prefs.edit().remove(OAUTH_STARTED_AT).remove(OAUTH_CODE_VERIFIER).apply()
        if (startedAt <= 0L || verifier.isNullOrBlank()) return null
        val age = System.currentTimeMillis() - startedAt
        return verifier.takeIf { age in 0..OAUTH_WINDOW_MS }
    }

    fun isTrustedAuthCallback(uri: android.net.Uri?): Boolean =
        uri != null &&
            uri.scheme.equals("dlavie", ignoreCase = true) &&
            uri.host.equals("auth-callback", ignoreCase = true)

    fun containsLegacyPortalSecrets(uri: android.net.Uri?): Boolean {
        if (uri == null) return false
        val secretKeys = listOf("token", "uid", "refresh", "access_token", "refresh_token")
        return secretKeys.any { !uri.getQueryParameter(it).isNullOrBlank() } ||
            uri.fragment.orEmpty().contains("access_token=") ||
            uri.fragment.orEmpty().contains("refresh_token=")
    }

    /**
     * Exchanges the one-time OAuth code using the locally held verifier, then
     * independently verifies the returned session against Supabase Auth.
     */
    fun exchangePkceCode(authCode: String, codeVerifier: String): VerifiedSession? {
        if (authCode.isBlank() || codeVerifier.length < 43) return null
        val connection = (URL(
            BuildConfig.SUPABASE_URL.trimEnd('/') + "/auth/v1/token?grant_type=pkce"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "DLavie-Launcher/${BuildConfig.VERSION_NAME}")
        }

        return try {
            val requestBody = JSONObject()
                .put("auth_code", authCode)
                .put("code_verifier", codeVerifier)
                .toString()
            connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return null
            val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val accessToken = response.optString("access_token", "")
            val refreshToken = response.optString("refresh_token", "")
            if (accessToken.isBlank() || refreshToken.isBlank()) return null
            val verified = verifySession(accessToken) ?: return null
            VerifiedSession(accessToken, refreshToken, verified)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Local checks are only an early rejection layer. verifySession() remains mandatory
     * before persisting a newly received OAuth/password session.
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
     * with the signed JWT subject.
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
            if (connection.responseCode !in 200..299) return null
            val user = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
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
        context.getSharedPreferences(OAUTH_PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("dlavie_auth_session", Context.MODE_PRIVATE)
            .edit().clear().apply()
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
