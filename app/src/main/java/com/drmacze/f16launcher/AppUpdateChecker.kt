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
 * App Update Checker v3 — GitHub-only (manifest.json).
 *
 * v322 CHANGE: Supabase strategy REMOVED entirely (quota exceeded, dead call).
 * Now goes straight to manifest.json on GitHub raw — fast, reliable, no auth needed.
 *
 * Sistem:
 *  - Baca manifest.json dari GitHub raw (DLavie-Launcher-Data repo)
 *  - Bandingkan latest_version_code dengan BuildConfig.VERSION_CODE
 *  - Kalau ada versi baru → return UpdateInfo (dengan forceUpdate flag)
 *  - forceUpdate = true kalau gap versi > 3 (user terlalu lama tidak update)
 *
 * Popup behavior:
 *  - forceUpdate = true → popup TIDAK bisa di-dismiss, user WAJIB update
 *  - forceUpdate = false → popup bisa di-dismiss (Later button)
 *
 * Website fallback:
 *  - Tombol "Buka Website DLavie" selalu tersedia
 *  - URL: https://drmacze.github.io/dlavie-web/
 */
object AppUpdateChecker {

    /** URL website DLavie — halaman download APK */
    const val DLAVIE_WEBSITE_URL = "https://drmacze.github.io/dlavie-web/"

    /** URL manifest.json (raw GitHub) — source of truth untuk versi terbaru */
    private const val MANIFEST_URL = "https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json"

    /**
     * v323: Threshold gap versi untuk force update.
     * Set to 1 — ANY version gap triggers force update.
     * Old version users will see non-dismissable popup directing to DLavie website.
     */
    private const val FORCE_UPDATE_THRESHOLD = 1

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val releaseNotes: String,
        val apkUrl: String,
        val isPublished: Boolean,
        val isUpdateAvailable: Boolean,
        val apkSizeMb: String = "",
        /** v322: Force update kalau gap versi > threshold — popup tidak bisa di-dismiss */
        val forceUpdate: Boolean = false,
        /** v322: Versi saat ini di device user (untuk ditampilkan di popup) */
        val currentVersionCode: Int = 0,
        /** v322: Website URL untuk download manual */
        val websiteUrl: String = DLAVIE_WEBSITE_URL
    )

    /**
     * Cek update dari manifest.json (GitHub raw URL).
     * v322: Supabase dihapus, langsung ke manifest — cepat & reliable.
     *
     * @param api CommunityApi instance (tidak dipakai lagi, tetap ada untuk backward compat)
     * @param context Context untuk ambil installed APK size
     * @return UpdateInfo atau null kalau tidak ada update
     */
    suspend fun checkForUpdate(api: CommunityApi? = null, context: Context? = null): UpdateInfo? {
        val currentCode = BuildConfig.VERSION_CODE
        android.util.Log.i("AppUpdate", "v322 checkForUpdate: current=$currentCode, manifest=$MANIFEST_URL")

        try {
            // Cache-bust supaya tidak dapat versi stale
            val manifestUrl = "$MANIFEST_URL?t=${System.currentTimeMillis()}"
            val conn = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000   // v322: faster timeout (was 10s)
                readTimeout = 12_000     // v322: faster timeout (was 15s)
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "DLavie-Launcher-v322")
                connect()
            }
            try {
                if (conn.responseCode !in 200..299) {
                    android.util.Log.w("AppUpdate", "Manifest HTTP ${conn.responseCode}")
                    return null
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val manifest = JSONObject(text)
                val launcher = manifest.optJSONObject("launcher") ?: run {
                    android.util.Log.w("AppUpdate", "Manifest: 'launcher' object missing")
                    return null
                }
                val latestCode = launcher.optInt("latest_version_code", 0)
                android.util.Log.i("AppUpdate", "Manifest: latest=$latestCode, current=$currentCode, gap=${latestCode - currentCode}")

                if (latestCode <= currentCode) {
                    android.util.Log.i("AppUpdate", "Already up-to-date (current=$currentCode, latest=$latestCode)")
                    return null
                }

                val apkUrl = launcher.optString("apk_url", "")
                if (apkUrl.isBlank()) {
                    android.util.Log.w("AppUpdate", "Manifest: apk_url kosong")
                    return null
                }

                // v322: Force update kalau gap versi > threshold
                val gap = latestCode - currentCode
                val forceUpdate = gap >= FORCE_UPDATE_THRESHOLD
                android.util.Log.i("AppUpdate", "Update available: v$latestCode (gap=$gap, forceUpdate=$forceUpdate)")

                // v322: Release notes bisa array atau string
                val notesRaw = launcher.opt("release_notes")
                val notes = when (notesRaw) {
                    is org.json.JSONArray -> {
                        // Join array elements dengan newline
                        (0 until notesRaw.length()).joinToString("\n") { i -> "• ${notesRaw.optString(i)}" }
                    }
                    is String -> notesRaw
                    else -> "Update terbaru tersedia"
                }

                val sizeMb = if (context != null) fetchUpdateDeltaSize(context, apkUrl) else ""

                return UpdateInfo(
                    versionName = launcher.optString("latest_version_name", "unknown"),
                    versionCode = latestCode,
                    releaseNotes = notes,
                    apkUrl = apkUrl,
                    isPublished = true,
                    isUpdateAvailable = true,
                    apkSizeMb = sizeMb,
                    forceUpdate = forceUpdate,
                    currentVersionCode = currentCode,
                    websiteUrl = DLAVIE_WEBSITE_URL
                )
            } finally {
                conn.disconnect()
            }
        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "v322 manifest check failed: ${e.message}")
            return null
        }
    }

    /**
     * Download APK ke cache dir dengan progress callback + retry logic.
     */
    suspend fun downloadApk(context: Context, apkUrl: String, onProgress: ((Float) -> Unit)? = null): File? {
        val cacheDir = File(context.cacheDir, "app-updates").also { it.mkdirs() }
        val apkFile = File(cacheDir, "dlavie-update.apk")
        if (apkFile.exists()) apkFile.delete()

        var lastError: String? = null
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                android.util.Log.i("AppUpdate", "Download attempt $attempt/$maxRetries: $apkUrl")
                val success = downloadApkAttempt(apkUrl, apkFile, onProgress)
                if (success && apkFile.length() > 1_000_000) {
                    android.util.Log.i("AppUpdate", "Download success: ${apkFile.length()} bytes")
                    return apkFile
                } else if (apkFile.exists()) {
                    apkFile.delete()
                }
            } catch (e: Exception) {
                android.util.Log.w("AppUpdate", "Attempt $attempt failed: ${e.message}")
                lastError = e.message
                if (apkFile.exists()) apkFile.delete()
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(2000L * attempt)
                }
            }
        }
        throw Exception(lastError ?: "Download failed after $maxRetries attempts")
    }

    private fun downloadApkAttempt(apkUrl: String, apkFile: File, onProgress: ((Float) -> Unit)?): Boolean {
        var currentUrl = apkUrl
        var redirectCount = 0
        var conn: HttpURLConnection? = null

        try {
            while (true) {
                val url = URL(currentUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 30_000
                    readTimeout = 120_000
                    setRequestProperty("User-Agent", "DLavie-Launcher/7.9.49 (Android)")
                    setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                    setRequestProperty("Accept-Encoding", "identity")
                    connect()
                }

                val responseCode = conn.responseCode
                if (responseCode in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    conn = null
                    if (location.isNullOrBlank()) {
                        throw Exception("Redirect tanpa Location header (HTTP $responseCode)")
                    }
                    if (redirectCount >= 5) {
                        throw Exception("Too many redirects (max 5)")
                    }
                    currentUrl = location
                    redirectCount++
                    continue
                }

                if (responseCode == 404) {
                    throw Exception("File tidak ditemukan di server (HTTP 404)")
                }
                if (responseCode == 403) {
                    throw Exception("Akses ditolak (HTTP 403). Signed URL mungkin expired, retry...")
                }
                if (responseCode !in 200..299) {
                    throw Exception("Server return HTTP $responseCode")
                }

                val total = conn.contentLengthLong.toFloat().coerceAtLeast(1f)
                val buf = ByteArray(32 * 1024)
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
                return true
            }
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Trigger install APK via ACTION_VIEW + FileProvider.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
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
            true
        } catch (e: Throwable) {
            android.util.Log.e("DLavie", "installApk: FileProvider failed", e)
            false
        }
    }

    /**
     * v322: Buka website DLavie di browser default.
     * Fallback kalau download/install APK gagal — user bisa download manual dari website.
     */
    fun openWebsite(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DLAVIE_WEBSITE_URL)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Throwable) {
            android.util.Log.e("DLavie", "openWebsite failed", e)
            false
        }
    }

    /**
     * Fetch actual download size (full APK size, bukan delta).
     */
    private fun fetchUpdateDeltaSize(context: Context, apkUrl: String): String {
        return try {
            val newSize = fetchRemoteApkSizeBytes(apkUrl)
            if (newSize <= 0L) {
                android.util.Log.w("AppUpdate", "Cannot fetch remote APK size")
                return ""
            }
            val sizeMb = newSize / (1024.0 * 1024.0)
            val formatted = if (sizeMb >= 1.0) "%.1f MB".format(sizeMb) else "${newSize / 1024} KB"
            android.util.Log.i("AppUpdate", "Download size: $newSize bytes = $formatted")
            formatted
        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "fetchUpdateDeltaSize failed: ${e.message}")
            ""
        }
    }

    private fun fetchRemoteApkSizeBytes(apkUrl: String): Long {
        return try {
            var currentUrl = apkUrl
            var redirectCount = 0
            while (redirectCount < 5) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "HEAD"
                    connectTimeout = 8_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "DLavie-Launcher")
                    connect()
                }
                val responseCode = conn.responseCode
                if (responseCode in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) break
                    currentUrl = location
                    redirectCount++
                    continue
                }
                if (responseCode in 200..299) {
                    val size = conn.contentLengthLong
                    conn.disconnect()
                    return size
                }
                conn.disconnect()
                break
            }
            0L
        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "fetchRemoteApkSizeBytes failed: ${e.message}")
            0L
        }
    }
}
