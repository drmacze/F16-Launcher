package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

private enum class PortalConnectStage(val progressStep: Int) {
    PREPARING(1),
    WAITING_PORTAL(1),
    VERIFYING(2),
    SYNCING(3),
    SUCCESS(3),
    ERROR(0),
}

private data class PortalConnectUiState(
    val stage: PortalConnectStage,
    val title: String,
    val message: String,
    val detail: String = "",
    val canReopenPortal: Boolean = false,
    val accountEmail: String = "",
)

/**
 * Secure Portal-to-launcher account handoff.
 *
 * The Portal access token is never placed in a URI. The launcher receives only a
 * short-lived capability, creates its own PKCE verifier/state, then exchanges a
 * one-time authorization code directly with the backend over HTTPS.
 */
class PortalSsoActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLocale(newBase))
    }

    companion object {
        private const val PREFS = "dlavie_portal_sso_pending"
        private const val KEY_CAPABILITY = "capability"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_STATE = "state"
        private const val KEY_STARTED_AT = "started_at"
        private const val MAX_FLOW_AGE_MS = 3 * 60 * 1000L
        private const val PORTAL_URL = "https://drmacze.github.io/dlavie-web/"
    }

    private val uiState = mutableStateOf(
        PortalConnectUiState(
            stage = PortalConnectStage.PREPARING,
            title = "Menyiapkan koneksi",
            message = "Launcher sedang membuat permintaan yang aman untuk akun Portal Anda.",
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uiState.value = PortalConnectUiState(
            stage = PortalConnectStage.PREPARING,
            title = LocaleText.get(this, "portal.preparing"),
            message = LocaleText.get(this, "portal.preparing_request"),
        )
        setContent {
            PortalSsoScreen(
                state = uiState.value,
                onReopenPortal = ::reopenPortal,
                onBackToLogin = ::returnToLogin,
            )
        }
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
            showFailure("Permintaan koneksi tidak dikenali. Mulai kembali dari halaman awal launcher.")
            return
        }
        when (data.host) {
            "connect" -> beginPortalHandoff(data)
            "portal-complete" -> completePortalHandoff(data)
            else -> showFailure("Tujuan koneksi tidak dikenali. Mulai kembali dari halaman awal launcher.")
        }
    }

    private fun beginPortalHandoff(uri: Uri) {
        val capability = uri.getQueryParameter("cap").orEmpty()
        if (!isBase64Url(capability, 32, 128)) {
            showFailure("Permintaan Portal sudah tidak berlaku. Kembali dan mulai koneksi sekali lagi.")
            return
        }

        val verifier = randomSecret(64)
        val state = randomSecret(32)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putString(KEY_CAPABILITY, capability)
            .putString(KEY_VERIFIER, verifier)
            .putString(KEY_STATE, state)
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .apply()

        uiState.value = PortalConnectUiState(
            stage = PortalConnectStage.WAITING_PORTAL,
            title = LocaleText.get(this, "portal.continue_in_portal"),
            message = LocaleText.get(this, "portal.confirm_account"),
            detail = LocaleText.get(this, "portal.return_after_approval"),
            canReopenPortal = true,
        )
        openAuthorizationPage(capability, verifier, state)
    }

    private fun authorizationUrl(capability: String, verifier: String, state: String): String {
        val challenge = sha256Base64Url(verifier)
        return Uri.parse(PORTAL_URL).buildUpon()
            .appendQueryParameter("launcher_sso", "1")
            .appendQueryParameter("cap", capability)
            .appendQueryParameter("challenge", challenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("callback_uri", BuildConfig.PORTAL_SSO_CALLBACK_URL)
            .build()
            .toString() + "#/portal"
    }

    private fun openAuthorizationPage(capability: String, verifier: String, state: String) {
        val url = authorizationUrl(capability, verifier, state)
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, Uri.parse(url))
        } catch (_: Exception) {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }.onFailure {
                showFailure(
                    message = LocaleText.get(this, "portal.browser_failed"),
                    canReopenPortal = true,
                )
            }
        }
    }

    private fun reopenPortal() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val capability = prefs.getString(KEY_CAPABILITY, null).orEmpty()
        val verifier = prefs.getString(KEY_VERIFIER, null).orEmpty()
        val state = prefs.getString(KEY_STATE, null).orEmpty()
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        val age = System.currentTimeMillis() - startedAt

        if (!isBase64Url(capability, 32, 128) ||
            !isBase64Url(verifier, 43, 128) ||
            !isBase64Url(state, 32, 128) ||
            age !in 0..MAX_FLOW_AGE_MS
        ) {
            clearPending()
            showFailure(LocaleText.get(this, "portal.session_ended"))
            return
        }

        uiState.value = PortalConnectUiState(
            stage = PortalConnectStage.WAITING_PORTAL,
            title = LocaleText.get(this, "portal.continue_in_portal"),
            message = LocaleText.get(this, "portal.confirm_account"),
            detail = "Kembali ke launcher setelah Portal menyelesaikan persetujuan.",
            canReopenPortal = true,
        )
        openAuthorizationPage(capability, verifier, state)
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
            showFailure("Kode koneksi tidak cocok atau sudah kedaluwarsa. Mulai kembali dari halaman awal.")
            return
        }

        uiState.value = PortalConnectUiState(
            stage = PortalConnectStage.VERIFYING,
            title = LocaleText.get(this, "portal.verify_code"),
            message = LocaleText.get(this, "portal.verify_message"),
            detail = LocaleText.get(this, "portal.keep_open"),
        )

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { exchange(authCode, verifier, returnedState) }
            }
            result.onSuccess { session ->
                uiState.value = PortalConnectUiState(
                    stage = PortalConnectStage.SYNCING,
                    title = LocaleText.get(this@PortalSsoActivity, "portal.sync_profile"),
                    message = LocaleText.get(this@PortalSsoActivity, "portal.sync_message"),
                    detail = session.email,
                )
                withContext(Dispatchers.IO) { persistAndLoadProfile(session) }
                clearPending()
                uiState.value = PortalConnectUiState(
                    stage = PortalConnectStage.SUCCESS,
                    title = LocaleText.get(this@PortalSsoActivity, "portal.connected"),
                    message = LocaleText.get(this@PortalSsoActivity, "portal.same_account"),
                    detail = LocaleText.get(this@PortalSsoActivity, "portal.opening_home"),
                    accountEmail = session.email,
                )
                delay(900)
                startActivity(
                    Intent(this@PortalSsoActivity, ModernLauncherActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                )
                finish()
            }.onFailure { error ->
                clearPending()
                showFailure(friendlyError(error))
            }
        }
    }

    private fun friendlyError(error: Throwable): String {
        val raw = error.message.orEmpty().lowercase()
        return when {
            "expired" in raw || "kedaluwarsa" in raw ->
                LocaleText.get(this, "portal.session_ended")
            "identity" in raw || "identitas" in raw || "tidak cocok" in raw ->
                LocaleText.get(this, "portal.account_mismatch")
            "network" in raw || "unable to resolve" in raw || "timeout" in raw ->
                LocaleText.get(this, "portal.network_failed")
            else -> LocaleText.get(this, "portal.failed")
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

    private fun showFailure(message: String, canReopenPortal: Boolean = false) {
        uiState.value = PortalConnectUiState(
            stage = PortalConnectStage.ERROR,
            title = "Koneksi belum selesai",
            message = message,
            detail = if (canReopenPortal) {
                "Permintaan Anda masih aktif dan Portal dapat dibuka kembali."
            } else {
                "Tidak ada akun atau token yang disimpan dari proses yang gagal."
            },
            canReopenPortal = canReopenPortal,
        )
    }

    private fun clearPending() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun returnToLogin() {
        clearPending()
        startActivity(
            Intent(this, DLavieGuidedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
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
private fun PortalSsoScreen(
    state: PortalConnectUiState,
    onReopenPortal: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF0D0F0E),
            onBackground = Color.White,
            onSurface = Color.White,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(62.dp),
                        shape = RoundedCornerShape(19.dp),
                        color = Color(0xFF151716),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("DL", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Text(
                        text = state.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = state.message,
                        color = if (state.stage == PortalConnectStage.ERROR) {
                            Color(0xFFFF8A8A)
                        } else {
                            Color(0xFFB8C0BC)
                        },
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    if (state.detail.isNotBlank()) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = state.detail,
                            color = Color.White.copy(alpha = 0.42f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    PortalConnectProgress(state)
                    Spacer(Modifier.height(24.dp))

                    when (state.stage) {
                        PortalConnectStage.ERROR -> {
                            if (state.canReopenPortal) {
                                Button(
                                    onClick = onReopenPortal,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black,
                                    ),
                                ) {
                                    Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Buka Portal Lagi", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            OutlinedButton(
                                onClick = onBackToLogin,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            ) {
                                Text("Kembali ke Halaman Awal", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        PortalConnectStage.WAITING_PORTAL -> {
                            Button(
                                onClick = onReopenPortal,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black,
                                ),
                            ) {
                                Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Buka Portal Lagi", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = onBackToLogin) {
                                Text("Batalkan koneksi", color = Color.White.copy(alpha = 0.48f), fontSize = 12.sp)
                            }
                        }
                        PortalConnectStage.SUCCESS -> {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF35D07F),
                                modifier = Modifier.size(32.dp),
                            )
                            if (state.accountEmail.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    state.accountEmail,
                                    color = Color.White.copy(alpha = 0.62f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        else -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.30f),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Kode sekali pakai • tanpa membagikan password atau token",
                            color = Color.White.copy(alpha = 0.34f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalConnectProgress(state: PortalConnectUiState) {
    val activeStep = state.stage.progressStep
    val error = state.stage == PortalConnectStage.ERROR
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0D0F0E),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            PortalProgressRow(
                number = 1,
                label = "Akun Portal",
                detail = "Masuk dan pilih akun",
                completed = activeStep > 1 || state.stage == PortalConnectStage.SUCCESS,
                active = activeStep == 1 && !error,
            )
            PortalProgressRow(
                number = 2,
                label = "Verifikasi aman",
                detail = "Kode sekali pakai diperiksa",
                completed = activeStep > 2 || state.stage == PortalConnectStage.SUCCESS,
                active = activeStep == 2 && !error,
            )
            PortalProgressRow(
                number = 3,
                label = "Launcher terhubung",
                detail = "Profil akun disiapkan",
                completed = state.stage == PortalConnectStage.SUCCESS,
                active = activeStep == 3 && state.stage != PortalConnectStage.SUCCESS && !error,
            )
        }
    }
}

@Composable
private fun PortalProgressRow(
    number: Int,
    label: String,
    detail: String,
    completed: Boolean,
    active: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = when {
                completed -> Color(0xFF35D07F).copy(alpha = 0.16f)
                active -> Color.White.copy(alpha = 0.10f)
                else -> Color.White.copy(alpha = 0.04f)
            },
            border = BorderStroke(
                1.dp,
                when {
                    completed -> Color(0xFF35D07F).copy(alpha = 0.45f)
                    active -> Color.White.copy(alpha = 0.28f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF35D07F),
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        number.toString(),
                        color = if (active) Color.White else Color.White.copy(alpha = 0.38f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = if (completed || active) Color.White else Color.White.copy(alpha = 0.46f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                detail,
                color = Color.White.copy(alpha = if (completed || active) 0.42f else 0.25f),
                fontSize = 10.sp,
            )
        }
        if (stateIconVisible(completed, active)) {
            if (completed) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF35D07F).copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color.White.copy(alpha = 0.72f),
                    strokeWidth = 1.6.dp,
                )
            }
        }
    }
}

private fun stateIconVisible(completed: Boolean, active: Boolean): Boolean = completed || active
