package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Secure Portal-to-launcher account handoff.
 *
 * The Portal access token is never placed in a URI. The launcher receives only a
 * short-lived capability, creates its own PKCE verifier/state, then exchanges a
 * one-time authorization code directly with the backend over HTTPS.
 */
class PortalSsoActivity : ComponentActivity() {
    companion object {
        private const val PREFS = "dlavie_portal_sso_pending"
        private const val KEY_CAPABILITY = "capability"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_STATE = "state"
        private const val KEY_STARTED_AT = "started_at"
        private const val MAX_FLOW_AGE_MS = 3 * 60 * 1000L
        private const val PORTAL_URL = "https://drmacze.github.io/dlavie-web/"
    }

    private val status = mutableStateOf("Menyiapkan koneksi aman…")
    private val failed = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PortalSsoScreen(status.value, failed.value, ::returnToLogin) }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (intent?.action != Intent.ACTION_VIEW || data?.scheme != "dlavie") {
            showFailure("Permintaan koneksi tidak valid.")
            return
        }
        when (data.host) {
            "connect" -> beginPortalHandoff(data)
            "portal-complete" -> completePortalHandoff(data)
            else -> showFailure("Tujuan koneksi tidak dikenal.")
        }
    }

    private fun beginPortalHandoff(uri: Uri) {
        val capability = uri.getQueryParameter("cap").orEmpty()
        if (!isBase64Url(capability, 32, 128)) {
            showFailure("Capability Portal tidak tersedia atau sudah tidak valid.")
            return
        }

        val verifier = randomSecret(64)
        val state = randomSecret(32)
        val challenge = sha256Base64Url(verifier)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString(KEY_CAPABILITY, capability)
            .putString(KEY_VERIFIER, verifier)
            .putString(KEY_STATE, state)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .apply()

        status.value = "Memverifikasi akun Portal…"
        failed.value = false

        val authorizationUrl = Uri.parse(PORTAL_URL).buildUpon()
            .appendQueryParameter("launcher_sso", "1")
            .appendQueryParameter("cap", capability)
            .appendQueryParameter("challenge", challenge)
            .appendQueryParameter("state", state)
            .build()
            .toString() + "#/portal"

        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, Uri.parse(authorizationUrl))
        } catch (_: Exception) {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl)))
            }.onFailure {
                showFailure("Browser tidak dapat dibuka untuk menyelesaikan koneksi.")
            }
        }
    }

    private fun completePortalHandoff(uri: Uri) {
        val authCode = uri.getQueryParameter("code").orEmpty()
        val returnedState = uri.getQueryParameter("state").orEmpty()
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val verifier = prefs.getString(KEY_VERIFIER, null).orEmpty()
        val expectedState = prefs.getString(KEY_STATE, null).orEmpty()
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)

        if (!isBase64Url(authCode, 32, 128) ||
            !isBase64Url(returnedState, 32, 128) ||
            verifier.isBlank() || expectedState.isBlank() ||
            System.currentTimeMillis() - startedAt !in 0..MAX_FLOW_AGE_MS ||
            !constantTimeEquals(expectedState, returnedState)
        ) {
            clearPending()
            showFailure("Kode koneksi tidak lengkap, tidak cocok, atau sudah kedaluwarsa.")
            return
        }

        status.value = "Menghubungkan akun yang sama ke launcher…"
        failed.value = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { exchange(authCode, verifier, returnedState) }
            }
            result.onSuccess { session ->
                withContext(Dispatchers.IO) { persistAndLoadProfile(session) }
                clearPending()
                status.value = "Akun Portal berhasil terhubung. Membuka launcher…"
                startActivity(
                    Intent(this@PortalSsoActivity, ModernLauncherActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }.onFailure { error ->
                clearPending()
                showFailure(error.message ?: "Akun Portal tidak dapat dihubungkan.")
            }
        }
    }

    private fun exchange(authCode: String, verifier: String, state: String): PortalSession {
        val connection = (URL(BuildConfig.PORTAL_SSO_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12_000
            readTimeout = 25_000
            useCaches = false
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "DLavie-Launcher/${BuildConfig.VERSION_NAME}")
        }
        val request = JSONObject()
            .put("action", "exchange")
            .put("auth_code", authCode)
            .put("code_verifier", verifier)
            .put("state", state)
            .toString()
        connection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }

        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (code !in 200..299) {
                val reason = json.optString("error", "exchange_failed")
                throw IllegalStateException("Koneksi Portal gagal: $reason")
            }

            val accessToken = json.optString("access_token", "")
            val refreshToken = json.optString("refresh_token", "")
            if (accessToken.isBlank() || refreshToken.isBlank()) {
                throw IllegalStateException("Backend tidak mengembalikan sesi lengkap.")
            }
            val verified = PortalAuthSecurity.verifySession(accessToken)
                ?: throw IllegalStateException("Sesi baru tidak lolos verifikasi server.")
            val user = json.optJSONObject("user")
            val responseUserId = user?.optString("id", verified.id) ?: verified.id
            if (responseUserId != verified.id) {
                throw IllegalStateException("Identitas sesi tidak cocok.")
            }
            PortalSession(accessToken, refreshToken, verified.id, verified.email)
        } finally {
            connection.disconnect()
        }
    }

    private fun persistAndLoadProfile(session: PortalSession) {
        getSharedPreferences("dlavie_auth_session", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("email", session.email)
            .apply()

        getSharedPreferences("dlavie_community", Context.MODE_PRIVATE)
            .edit()
            .remove("username")
            .remove("display_name")
            .remove("avatar_url")
            .remove("cover_url")
            .remove("role")
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("user_id", session.userId)
            .putString("email", session.email)
            .putBoolean("portal_connected", true)
            .putString("portal_connected_at", System.currentTimeMillis().toString())
            .putBoolean("is_guest", false)
            .apply()

        runCatching {
            val api = CommunityApi(this)
            api.clearGuest()
            api.loadMyProfile()
        }
        runCatching {
            Telemetry.track(this, Telemetry.EVT_LOGIN, mapOf("method" to "portal_sso_pkce"))
        }
    }

    private fun showFailure(message: String) {
        status.value = message
        failed.value = true
    }

    private fun clearPending() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun returnToLogin() {
        clearPending()
        startActivity(
            Intent(this, DLavieGuidedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private data class PortalSession(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val email: String,
    )
}

private fun isBase64Url(value: String?, min: Int, max: Int): Boolean =
    value != null && value.length in min..max && value.all {
        it.isLetterOrDigit() || it == '-' || it == '_'
    }

private fun randomSecret(size: Int): String {
    val bytes = ByteArray(size).also { SecureRandom().nextBytes(it) }
    return android.util.Base64.encodeToString(
        bytes,
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
    )
}

private fun sha256Base64Url(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.US_ASCII))
    return android.util.Base64.encodeToString(
        digest,
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
    )
}

private fun constantTimeEquals(left: String, right: String): Boolean =
    MessageDigest.isEqual(left.toByteArray(Charsets.UTF_8), right.toByteArray(Charsets.UTF_8))

@Composable
private fun PortalSsoScreen(status: String, failed: Boolean, onBackToLogin: () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF0D0F0E),
            onBackground = Color.White,
            onSurface = Color.White,
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF151716),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("DL", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "DLavie Secure Connect",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        status,
                        color = if (failed) Color(0xFFFF6B6B) else Color(0xFFB8C0BC),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    if (!failed) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Button(
                            onClick = onBackToLogin,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                            ),
                        ) {
                            Text("Kembali ke Login", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Token Portal tidak pernah dikirim melalui tautan. Koneksi memakai kode sekali pakai, PKCE, state, dan verifikasi sesi server.",
                        color = Color.White.copy(alpha = 0.38f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
