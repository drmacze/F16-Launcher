package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * App Update Checker — cek versi terbaru launcher dari GitHub Releases.
 *
 * Flow:
 *  1. fetchLatestRelease() — GET /repos/drmacze/F16-Launcher/releases/latest
 *  2. Bandingkan versionCode dari tag_name dengan BuildConfig.VERSION_CODE
 *  3. Kalau ada versi baru → tampilkan UpdatePopup
 *  4. User tap "Update" → download APK → trigger install
 *
 * Anti-bentrok:
 *  - applicationId sama (com.drmacze.f16launcher) → install sebagai update, bukan paralel
 *  - versionCode di build.gradle harus increment setiap release
 *  - Signature: debug key (saat ini) — APK baru harus di-sign dengan key yang sama
 *  - User tinggal tap "Install" di dialog sistem Android
 */
object AppUpdateChecker {

    private const val GITHUB_API = "https://api.github.com/repos/drmacze/F16-Launcher/releases/latest"

    data class UpdateInfo(
        val versionName: String,        // e.g. "v1.5.0-auto-update"
        val versionCode: Int,           // parsed from tag_name or release body
        val releaseNotes: String,       // body dari release
        val apkUrl: String,             // browser_download_url dari asset APK
        val apkSize: Long,              // size dalam bytes
        val htmlUrl: String,            // URL halaman release
        val isUpdateAvailable: Boolean  // true kalau versionCode > BuildConfig.VERSION_CODE
    )

    /**
     * Fetch latest release dari GitHub API.
     * Returns UpdateInfo atau null kalau gagal.
     *
     * Parser:
     *  - tag_name: "v1.5.0-auto-update" → extract versionCode dari body atau hardcode
     *  - Cari asset yang namanya mengandung ".apk" (bukan "diagnostics")
     *  - Body release berisi "versionCode: XX" — parse dari sana
     */
    suspend fun checkForUpdate(): UpdateInfo? {
        return try {
            val url = URL(GITHUB_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DLavie-Launcher")
            }

            val code = conn.responseCode
            if (code !in 200..299) return null

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(text)
            val tagName = json.optString("tag_name", "")
            val body = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "")

            // Parse versionCode dari body release (format: "versionCode: XX")
            // Kalau tidak ada, coba parse dari tag_name
            val versionCode = parseVersionCode(body) ?: parseVersionCodeFromTag(tagName) ?: 0

            // Cari asset APK (bukan diagnostics)
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl = ""
            var apkSize = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk") && !name.contains("diagnostics", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }

            if (apkUrl.isEmpty()) return null

            val currentCode = BuildConfig.VERSION_CODE
            val isUpdate = versionCode > currentCode

            UpdateInfo(
                versionName = tagName,
                versionCode = versionCode,
                releaseNotes = body,
                apkUrl = apkUrl,
                apkSize = apkSize,
                htmlUrl = htmlUrl,
                isUpdateAvailable = isUpdate
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Parse "versionCode: XX" dari release body.
     */
    private fun parseVersionCode(body: String): Int? {
        return try {
            val regex = Regex("versionCode:\\s*(\\d+)", RegexOption.IGNORE_CASE)
            val match = regex.find(body) ?: return null
            match.groupValues[1].toIntOrNull()
        } catch (_: Throwable) { null }
    }

    /**
     * Parse versionCode dari tag_name seperti "v1.5.0-auto-update".
     * Format: vX.Y.Z-suffix → (X * 10000 + Y * 100 + Z)
     * Contoh: v1.5.0 → 10500, v1.4.0 → 10400
     * Hanya fallback kalau body tidak ada versionCode.
     */
    private fun parseVersionCodeFromTag(tag: String): Int? {
        return try {
            val regex = Regex("v(\\d+)\\.(\\d+)\\.(\\d+)")
            val match = regex.find(tag) ?: return null
            val major = match.groupValues[1].toInt()
            val minor = match.groupValues[2].toInt()
            val patch = match.groupValues[3].toInt()
            major * 10000 + minor * 100 + patch
        } catch (_: Throwable) { null }
    }

    /**
     * Download APK launcher ke cache dir.
     * Returns File atau null kalau gagal.
     */
    suspend fun downloadApk(context: Context, apkUrl: String, onProgress: ((Float) -> Unit)? = null): File? {
        return try {
            val cacheDir = File(context.cacheDir, "app-updates").also { it.mkdirs() }
            val apkFile = File(cacheDir, "dlavie-update.apk")
            if (apkFile.exists()) apkFile.delete()

            val url = URL(apkUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "DLavie-Launcher")
                connect()
            }

            val total = conn.contentLengthLong.toFloat().coerceAtLeast(1f)
            val buf = ByteArray(16 * 1024)
            conn.inputStream.use { inp ->
                apkFile.outputStream().use { out ->
                    var n: Int
                    var read = 0L
                    while (inp.read(buf).also { n = it } != -1) {
                        out.write(buf, 0, n)
                        read += n
                        onProgress?.invoke((read / total).coerceIn(0f, 0.99f))
                    }
                }
            }
            conn.disconnect()
            apkFile
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Trigger install APK.
     * Pakai ACTION_VIEW intent dengan FileProvider — user konfirmasi install.
     *
     * Anti-bentrok:
     *  - APK di-sign dengan key yang sama → install sebagai update
     *  - applicationId sama → replace existing app
     *  - Data user (preferences, session) tetap preserved
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            // Fallback: open browser ke release page
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/drmacze/F16-Launcher/releases/latest"))
                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(browserIntent)
            } catch (_: Throwable) { }
        }
    }
}
