package com.drmacze.f16launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Auth manager — handles Supabase auth account operations.
 *
 * Operations:
 *  - requestPasswordReset(email)    : send reset email via Supabase
 *  - updatePassword(token, newPass) : change password (user must be logged in)
 *  - updateEmail(token, newEmail)   : change email (Supabase sends verification to new email)
 *  - updateProfile(token, username, displayName) : update profile row in profiles table
 *
 * All methods throw IllegalStateException on failure with a user-friendly message.
 */
object AuthManager {

    private val SUPABASE_URL get() = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY get() = BuildConfig.SUPABASE_ANON_KEY

    /** Send a password-reset email. User clicks the link in their inbox to set a new password. */
    fun requestPasswordReset(email: String): String {
        val body = JSONObject().put("email", email.trim())
        val (_, text) = http("POST", "/auth/v1/recover", body, token = null)
        return "OK: Email reset password telah dikirim ke ${email.trim()}. Cek inbox (dan folder spam)."
    }

    /**
     * Change password for the currently logged-in user.
     * Requires a valid access token.
     */
    fun updatePassword(accessToken: String, newPassword: String): String {
        if (newPassword.length < 4) throw IllegalStateException("Password must be at least 4 characters.")
        val body = JSONObject().put("password", newPassword)
        val (_, _) = http("PUT", "/auth/v1/user", body, token = accessToken)
        return "OK: Password berhasil diubah. Sesi lain akan otomatis logout."
    }

    /**
     * Change email for the currently logged-in user.
     * Supabase will send a confirmation email to the NEW address — the change only takes effect
     * after the user clicks the link in that email.
     */
    fun updateEmail(accessToken: String, newEmail: String): String {
        if (!newEmail.contains("@") || !newEmail.contains(".")) {
            throw IllegalStateException("Format email tidak valid.")
        }
        val body = JSONObject().put("email", newEmail.trim())
        val (_, _) = http("PUT", "/auth/v1/user", body, token = accessToken)
        return "OK: Email konfirmasi dikirim ke ${newEmail.trim()}. Klik link di email tersebut untuk mengaktifkan email baru."
    }

    /**
     * Update profile row (username + display_name) for the currently logged-in user.
     * Validates username format and uniqueness is enforced by the database.
     */
    fun updateProfile(accessToken: String, userId: String, username: String, displayName: String): String {
        val u = username.trim()
        val d = displayName.trim()
        if (!u.matches(Regex("[a-zA-Z0-9_]{3,24}"))) {
            throw IllegalStateException("Username wajib 3-24 karakter: huruf, angka, underscore.")
        }
        if (d.length < 2 || d.length > 40) {
            throw IllegalStateException("Display name wajib 2-40 karakter.")
        }
        val body = JSONObject()
            .put("id", userId)
            .put("username", u)
            .put("display_name", d)
        val (_, _) = http(
            "PATCH",
            "/rest/v1/profiles?id=eq.$userId",
            body,
            token = accessToken,
            prefer = "return=minimal"
        )
        return "OK: Profile diperbarui."
    }

    // ─── HTTP helper ───────────────────────────────────────────────────────────

    private data class Resp(val code: Int, val body: String)

    private fun http(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null,
        prefer: String? = null
    ): Resp {
        val url = URL(SUPABASE_URL + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("apikey", SUPABASE_KEY)
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (!prefer.isNullOrBlank()) setRequestProperty("Prefer", prefer)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) {
            val msg = try { JSONObject(text).optString("msg", JSONObject(text).optString("message", text)) }
                      catch (_: Throwable) { text.take(200).ifBlank { "HTTP $code" } }
            throw IllegalStateException("HTTP $code: $msg")
        }
        return Resp(code, text)
    }
}
