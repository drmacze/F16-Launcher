package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Single maintenance model shared by login, launcher shell and Dev Hub control.
 */
data class MaintenanceInfo(
    val enabled: Boolean = false,
    val title: String = "",
    val message: String = "",
    val scope: String = "none",
    val allowOfflinePlay: Boolean = true,
    val statusLabel: String = "Pemeliharaan terjadwal",
    val estimatedEndAt: String? = null,
    val supportUrl: String = "https://drmacze.github.io/dlavie-web/#/issues",
    val updatedAt: String = "",
    val revision: Int = 0,
    val source: String = "fallback",
    val staffBypass: Boolean = false,
) {
    val isFull: Boolean get() = enabled && scope == "full"
    val isPartial: Boolean get() = enabled && scope == "partial"
}

/**
 * Maintenance configuration source order:
 * 1. Supabase maintenance-control endpoint (live Dev Hub state)
 * 2. Fresh local last-known-good cache (up to six hours)
 * 3. Public GitHub app_config.json fallback
 * 4. Safe operational default
 */
object MaintenanceRepository {
    private const val PREFS = "dlavie_maintenance_cache"
    private const val KEY_PAYLOAD = "payload"
    private const val KEY_SAVED_AT = "saved_at"
    private const val CACHE_MAX_AGE_MS = 6 * 60 * 60 * 1000L
    private const val MEMORY_MAX_AGE_MS = 60 * 1000L

    @Volatile
    private var memory: Pair<Long, MaintenanceInfo>? = null

    suspend fun fetch(context: Context, forceRefresh: Boolean = false): MaintenanceInfo =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!forceRefresh) {
                memory?.takeIf { now - it.first <= MEMORY_MAX_AGE_MS }?.second?.let {
                    return@withContext it
                }
            }

            val remote = runCatching { fetchRemote() }.getOrNull()
            if (remote != null) {
                saveCache(context, remote, now)
                memory = now to remote
                return@withContext remote
            }

            readCache(context, now)?.let {
                memory = now to it
                return@withContext it
            }

            val github = runCatching { fetchGitHubFallback() }.getOrNull()
            if (github != null) {
                memory = now to github
                return@withContext github
            }

            MaintenanceInfo(source = "safe_default").also { memory = now to it }
        }

    fun clearMemoryCache() {
        memory = null
        GitHubPublicDatabase.clearMemoryCache()
    }

    private fun fetchRemote(): MaintenanceInfo {
        val connection = (URL(BuildConfig.MAINTENANCE_CONTROL_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8_000
            readTimeout = 12_000
            useCaches = false
            instanceFollowRedirects = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUB_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_PUB_KEY}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("User-Agent", "DLavie-Launcher/${BuildConfig.VERSION_NAME}")
        }
        connection.outputStream.use {
            it.write(JSONObject().put("action", "get").toString().toByteArray(Charsets.UTF_8))
        }

        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("maintenance HTTP $status")
            val root = JSONObject(body)
            parseConfig(
                config = root.optJSONObject("config") ?: JSONObject(),
                updatedAt = root.optString("updated_at", ""),
                source = "dev_hub",
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetchGitHubFallback(): MaintenanceInfo {
        val root = GitHubPublicDatabase.fetchObject(
            GitHubPublicDatabase.PublicFile.APP_CONFIG,
            maxAgeMillis = 0L,
        )
        val config = root.optJSONObject("maintenance") ?: JSONObject()
        return parseConfig(
            config = config,
            updatedAt = root.optString("updated_at", ""),
            source = "github_fallback",
        )
    }

    private fun parseConfig(config: JSONObject, updatedAt: String, source: String): MaintenanceInfo {
        val enabled = config.optBoolean("enabled", false)
        val rawScope = config.optString("scope", if (enabled) "full" else "none")
        val scope = when {
            !enabled -> "none"
            rawScope == "partial" -> "partial"
            else -> "full"
        }
        val estimate = config.optString("estimated_end_at", "")
            .trim()
            .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
        return MaintenanceInfo(
            enabled = enabled,
            title = config.optString("title", "Kami sedang meningkatkan layanan")
                .trim().ifBlank { "Kami sedang meningkatkan layanan" },
            message = config.optString(
                "message",
                "Beberapa fitur untuk sementara tidak tersedia. Silakan coba lagi sebentar lagi.",
            ).trim().ifBlank {
                "Beberapa fitur untuk sementara tidak tersedia. Silakan coba lagi sebentar lagi."
            },
            scope = scope,
            allowOfflinePlay = config.optBoolean("allow_offline_play", true),
            statusLabel = config.optString("status_label", "Pemeliharaan terjadwal")
                .trim().ifBlank { "Pemeliharaan terjadwal" },
            estimatedEndAt = estimate,
            supportUrl = config.optString(
                "support_url",
                "https://drmacze.github.io/dlavie-web/#/issues",
            ).trim().ifBlank { "https://drmacze.github.io/dlavie-web/#/issues" },
            updatedAt = updatedAt,
            revision = config.optInt("revision", 0),
            source = source,
        )
    }

    private fun saveCache(context: Context, info: MaintenanceInfo, savedAt: Long) {
        val payload = JSONObject()
            .put("enabled", info.enabled)
            .put("title", info.title)
            .put("message", info.message)
            .put("scope", info.scope)
            .put("allow_offline_play", info.allowOfflinePlay)
            .put("status_label", info.statusLabel)
            .put("estimated_end_at", info.estimatedEndAt)
            .put("support_url", info.supportUrl)
            .put("updated_at", info.updatedAt)
            .put("revision", info.revision)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PAYLOAD, payload.toString())
            .putLong(KEY_SAVED_AT, savedAt)
            .apply()
    }

    private fun readCache(context: Context, now: Long): MaintenanceInfo? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        if (savedAt <= 0L || now - savedAt > CACHE_MAX_AGE_MS) return null
        val payload = prefs.getString(KEY_PAYLOAD, null) ?: return null
        return runCatching {
            val json = JSONObject(payload)
            parseConfig(
                config = json,
                updatedAt = json.optString("updated_at", ""),
                source = "local_cache",
            )
        }.getOrNull()
    }

    fun launchOfflineGame(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.ea.gp.fifaworld")
        if (launchIntent == null) {
            Toast.makeText(context, "Game belum terpasang di perangkat ini.", Toast.LENGTH_SHORT).show()
            return
        }
        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openSupport(context: Context, url: String) {
        val safeUrl = url.takeIf {
            it == "https://drmacze.github.io/dlavie-web/" ||
                it == "https://drmacze.github.io/dlavie-web/#/issues"
        } ?: "https://drmacze.github.io/dlavie-web/#/issues"
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
fun ProfessionalMaintenanceScreen(
    maintenance: MaintenanceInfo,
    refreshing: Boolean = false,
    isStaff: Boolean = false,
    allowStaffLogin: Boolean = false,
    onRetry: () -> Unit,
    onStaffEnter: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF15130E),
                border = BorderStroke(1.dp, Color(0x33FFB34D)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(Color(0xFFFFB34D), CircleShape),
                    )
                    Text(
                        maintenance.statusLabel.uppercase(Locale.getDefault()),
                        color = Color(0xFFFFC46B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.size(62.dp),
                shape = RoundedCornerShape(19.dp),
                color = Color(0xFF111211),
                border = BorderStroke(1.dp, Color(0x18FFFFFF)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("DL", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = maintenance.title.ifBlank { "Kami sedang meningkatkan layanan" },
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = maintenance.message,
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )

            formatEstimate(maintenance.estimatedEndAt)?.let { estimate ->
                Spacer(Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0E0F0E),
                    border = BorderStroke(1.dp, Color(0x12FFFFFF)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(17.dp),
                        )
                        Column {
                            Text("Estimasi selesai", color = Color(0xFF666666), fontSize = 9.sp)
                            Text(estimate, color = Color(0xFFD4D4D4), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onRetry,
                enabled = !refreshing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFFCCCCCC),
                    disabledContentColor = Color(0xFF555555),
                ),
            ) {
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Coba Lagi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            if (maintenance.allowOfflinePlay) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { MaintenanceRepository.launchOfflineGame(context) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x20FFFFFF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Main Offline", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            if (isStaff || allowStaffLogin) {
                TextButton(onClick = onStaffEnter, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color(0xFF777777),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (isStaff) "Masuk dengan akses staff" else "Login staff",
                        color = Color(0xFF777777),
                        fontSize = 11.sp,
                    )
                }
            }

            TextButton(
                onClick = { MaintenanceRepository.openSupport(context, maintenance.supportUrl) },
            ) {
                Text("Pusat bantuan", color = Color(0xFF777777), fontSize = 11.sp)
                Spacer(Modifier.width(5.dp))
                Icon(
                    Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = Color(0xFF777777),
                    modifier = Modifier.size(14.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "DLavie Services · rev ${maintenance.revision}",
                color = Color(0xFF444444),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.7.sp,
            )
        }
    }
}

@Composable
fun MaintenanceStatusBanner(maintenance: MaintenanceInfo) {
    if (!maintenance.enabled) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF11110F),
        border = BorderStroke(1.dp, Color(0x26FFB34D)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .background(Color(0xFFFFB34D), CircleShape),
            )
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        maintenance.statusLabel.uppercase(Locale.getDefault()),
                        color = Color(0xFFFFC46B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    if (maintenance.staffBypass) {
                        Surface(shape = RoundedCornerShape(999.dp), color = Color(0x14FFFFFF)) {
                            Text(
                                "STAFF ACCESS",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                color = Color(0xFF999999),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    maintenance.title.ifBlank { "Pemeliharaan layanan" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    maintenance.message,
                    color = Color(0xFF858585),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatEstimate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val parsers = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
    val parsed: Date = parsers.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                if (pattern.endsWith("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
            }.parse(raw)
        }.getOrNull()
    } ?: return null
    return SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(parsed)
}
