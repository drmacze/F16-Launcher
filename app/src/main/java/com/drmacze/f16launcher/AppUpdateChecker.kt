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
 * App Update Checker v3 — cek versi terbaru langsung dari GitHub Releases API.
 *
 * ⚠️ v3 BREAKING: tidak lagi memakai Supabase `app_releases` table.
 *    Supabase sering 404 / munculin "Update file not found on server" karena
 *    `apk_download_url` di tabel gak pernah di-update tiap build. Makanya
 *    sekarang kita ambil APK langsung dari GitHub Releases repo F16-Launcher,
 *    yang auto-published sama GitHub Actions workflow setiap push ke main.
 *
 * Flow:
 *  1. fetchLatestRelease() — GET https://api.github.com/repos/drmacze/F16-Launcher/releases/latest
 *     - Public endpoint (no auth needed, rate-limited 60/hr/IP — caching handles this)
 *     - Returns tag `latest` release yang berisi APK paling baru dari CI build
 *  2. Parse `version_code` dari release body, `versionName` dari release name,
 *     `apkUrl` dari `assets[0].browser_download_url`
 *  3. Kalau versionCode > current → tampilkan UpdatePopup
 *  4. User tap "Update" → download APK dari GitHub release asset URL → install
 *
 * Cache:
 *  - Response di-cache in-memory 5 menit (process lifetime).
 *    Ngurangin GitHub API rate-limit hit (60/hr/IP untuk unauth request).
 *
 * Anti-bentrok:
 *  - Fixed signing key (sama untuk semua build)
 *  - applicationId sama → install sebagai update
 */
object AppUpdateChecker {

    private const val REPO_OWNER = "drmacze"
    private const val REPO_NAME = "F16-Launcher"
    private const val GITHUB_API = "https://api.github.com"
    private const val RELEASES_LATEST_ENDPOINT =
        "$GITHUB_API/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    // In-memory cache: 5 menit untuk ngurangin rate-limit hit.
    // Process lifetime di Android launcher biasanya 5–30 menit, jadi cukup.
    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    private var cachedResponse: String? = null
    private var cachedAt: Long = 0L

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val releaseNotes: String,
        val apkUrl: String,
        val isPublished: Boolean,
        val isUpdateAvailable: Boolean
    )

    /**
     * Cek update dari GitHub Releases API (tag `latest`).
     *
     * @param api CommunityApi instance — KEPT FOR BACKWARD COMPAT, tidak dipakai di v3.
     *            Signature tetap sama biar caller (ModernLauncherActivity) gak perlu diubah.
     * @return UpdateInfo kalau ada versi lebih baru, null kalau up-to-date atau error.
     */
    suspend fun checkForUpdate(api: CommunityApi): UpdateInfo? {
        return try {
            val currentCode = BuildConfig.VERSION_CODE

            // 1. Fetch latest release JSON (dengan cache in-memory)
            val jsonStr = fetchReleaseJsonWithCache() ?: return null

            val release = JSONObject(jsonStr)

            // 2. Extract versionCode dari body (workflow naro "version_code: N" di line pertama)
            val body = release.optString("body", "")
            val tagName = release.optString("tag_name", "")
            val versionCode = extractVersionCode(body, tagName)

            // 3. Extract versionName — preferensi: dari release name, fallback ke tag_name
            val versionName = release.optString("name", "").ifBlank {
                tagName.ifBlank { "unknown" }
            }

            // 4. Cari APK asset di array `assets`
            val assets = release.optJSONArray("assets") ?: return null
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                // Cari asset dengan ekstensi .apk
                if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                    apkUrl = url
                    break
                }
            }

            // 5. Kalau gak ada APK asset, jangan tampilkan popup (link download gak valid)
            if (apkUrl.isBlank()) return null

            // 6. Cek apakah versionCode lebih besar dari current
            val isUpdateAvailable = versionCode > currentCode

            // 7. isPublished: GitHub release yang bukan draft & bukan prerelease = published
            val isPublished = !release.optBoolean("draft", false) &&
                              !release.optBoolean("prerelease", false)

            UpdateInfo(
                versionName = versionName,
                versionCode = versionCode,
                releaseNotes = body.trim(),
                apkUrl = apkUrl,
                isPublished = isPublished,
                isUpdateAvailable = isUpdateAvailable
            )
        } catch (_: Throwable) {
            // Network error, JSON parse error, etc → silent fail (jangan ganggu user)
            null
        }
    }

    /**
     * Fetch release JSON dengan in-memory cache 5 menit.
     * Returns null kalau gagal total (network error + no cache).
     */
    private fun fetchReleaseJsonWithCache(): String? {
        val now = System.currentTimeMillis()

        // Cek cache dulu — kalau masih fresh (< 5 menit), pakai cache
        val cached = cachedResponse
        if (cached != null && (now - cachedAt) < CACHE_TTL_MS) {
            return cached
        }

        // Cache stale atau belum ada → fetch fresh
        return try {
            val fresh = fetchReleaseJsonRaw()
            if (fresh != null) {
                cachedResponse = fresh
                cachedAt = now
                fresh
            } else {
                // Network gagal — fallback ke cache stale (lebih baik stale daripada nothing)
                cached
            }
        } catch (_: Throwable) {
            // Rate-limited atau network error → fallback ke cache
            cached
        }
    }

    /**
     * Raw HTTP GET ke GitHub Releases API. Returns JSON string atau null kalau gagal.
     * Handles:
     *  - 200 → return body
     *  - 403 (rate limit) → null (caller fallback ke cache)
     *  - 404 (no releases yet) → null
     *  - 5xx → null (caller fallback ke cache)
     */
    private fun fetchReleaseJsonRaw(): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(RELEASES_LATEST_ENDPOINT)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DLavie-Launcher")  // GitHub requires UA
                instanceFollowRedirects = false  // API endpoint tidak redirect
                connect()
            }

            when (conn.responseCode) {
                200 -> {
                    conn.inputStream.bufferedReader().use { it.readText() }
                }
                304, 403, 404, 500, 502, 503 -> {
                    // Cached/rate-limited/not-found/server-error → caller handles fallback
                    null
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Extract versionCode dari release body.
     * Workflow menaruh "version_code: N" di baris pertama body release.
     * Fallback: parse dari tag_name (e.g., "v210" → 210).
     */
    private fun extractVersionCode(body: String, tagName: String): Int {
        // 1. Cari "version_code: N" atau "version_code=N" di body (case-insensitive)
        val pattern = Regex("""(?i)version[_\s-]*code\s*[:=]\s*(\d+)""")
        val m = pattern.find(body)
        if (m != null) {
            return m.groupValues[1].toIntOrNull() ?: 0
        }

        // 2. Fallback: parse dari tag_name (e.g., "v210" → 210)
        val tagMatch = Regex("""v?(\d+)""").find(tagName)
        if (tagMatch != null) {
            return tagMatch.groupValues[1].toIntOrNull() ?: 0
        }

        // 3. Tidak ketemu — return 0 (berarti "tidak ada update")
        return 0
    }

    /**
     * Download APK ke cache dir dengan progress callback.
     * Handles GitHub redirect (301/302) automatically.
     *
     * v3: Tetap sama kayak v2 — GitHub release asset URLs redirect ke
     * objects.githubusercontent.com, jadi redirect handling wajib.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: ((Float) -> Unit)? = null
    ): File? {
        val cacheDir = File(context.cacheDir, "app-updates").also { it.mkdirs() }
        val apkFile = File(cacheDir, "dlavie-update.apk")
        if (apkFile.exists()) apkFile.delete()

        // Follow redirects manually (GitHub release URLs redirect to objects.githubusercontent.com)
        var currentUrl = apkUrl
        var redirectCount = 0
        var conn: HttpURLConnection? = null

        try {
            while (true) {
                val url = URL(currentUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false  // handle manually
                    connectTimeout = 30_000
                    readTimeout = 120_000
                    setRequestProperty("User-Agent", "DLavie-Launcher")
                    setRequestProperty("Accept", "application/octet-stream")
                    connect()
                }

                val responseCode = conn.responseCode
                if (responseCode in 300..399) {
                    // Redirect — follow Location header
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank() || redirectCount >= 5) {
                        throw Exception("Too many redirects. Download URL may be invalid.")
                    }
                    currentUrl = location
                    redirectCount++
                    continue
                }
                break
            }

            val responseCode = conn!!.responseCode
            if (responseCode == 404) {
                conn.disconnect()
                throw Exception("APK file not found on GitHub release. Check the release assets.")
            }
            if (responseCode !in 200..299) {
                conn.disconnect()
                throw Exception("GitHub returned HTTP $responseCode. Try again later.")
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

            // Verify file isn't empty or error page
            if (apkFile.length() < 1_000_000) {
                apkFile.delete()
                throw Exception("Downloaded file too small. GitHub may have returned an error page.")
            }

            return apkFile
        } catch (e: Exception) {
            conn?.disconnect()
            if (apkFile.exists()) apkFile.delete()
            // Re-throw dengan pesan yang user-friendly
            throw Exception(e.message ?: "Download failed. Check your connection.")
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
}
