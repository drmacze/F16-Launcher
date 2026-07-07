package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * App Update Checker v2 — cek versi terbaru dari Supabase app_releases table.
 *
 * Sistem Draft/Publish:
 *  - Draft release: hanya admin/developer yang dapat popup update (untuk testing)
 *  - Published release: semua user dapat popup update
 *
 * Flow:
 *  1. fetchLatestRelease(api) — query Supabase app_releases table
 *     - Regular user: WHERE is_published = true AND version_code > current
 *     - Staff (admin/developer): WHERE version_code > current (draft atau published)
 *  2. Kalau ada versi baru → tampilkan UpdatePopup
 *  3. User tap "Update" → download APK dari GitHub release URL → trigger install
 *
 * Anti-bentrok:
 *  - Fixed signing key (sama untuk semua build)
 *  - applicationId sama → install sebagai update
 */
object AppUpdateChecker {

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val releaseNotes: String,
        val apkUrl: String,
        val isPublished: Boolean,
        val isUpdateAvailable: Boolean
    )

    /**
     * Cek update dari Supabase app_releases table.
     * - Regular user: hanya dapat published releases
     * - Staff (admin/developer): dapat draft + published releases
     *
     * v7.9.40: Fallback ke manifest.json (GitHub raw) kalau app_releases kosong/error.
     * Manifest berisi info launcher terbaru, jadi user tetap dapat popup update
     * tanpa perlu SQL insert manual ke app_releases.
     *
     * @param api CommunityApi instance (untuk auth + role check)
     * @return UpdateInfo atau null kalau tidak ada update
     */
    suspend fun checkForUpdate(api: CommunityApi): UpdateInfo? {
        val currentCode = BuildConfig.VERSION_CODE

        // ── Strategy 1: Cek dari Supabase app_releases (primary) ──
        try {
            val filter = "version_code=gt.$currentCode&is_published=eq.true"
            val response = api.requestPublic(
                "GET",
                "/rest/v1/app_releases?$filter&order=version_code.desc&limit=1&select=version_code,version_name,tag_name,apk_download_url,changelog,is_published"
            )

            val arr = JSONArray(response)
            if (arr.length() > 0) {
                val release = arr.getJSONObject(0)
                val versionCode = release.optInt("version_code", 0)
                if (versionCode > currentCode) {
                    val apkUrl = release.optString("apk_download_url", "")
                    if (apkUrl.isNotBlank()) {
                        android.util.Log.i("AppUpdate", "Update found via Supabase: v$versionCode")
                        return UpdateInfo(
                            versionName = release.optString("version_name", "unknown"),
                            versionCode = versionCode,
                            releaseNotes = release.optString("changelog", ""),
                            apkUrl = apkUrl,
                            isPublished = release.optBoolean("is_published", false),
                            isUpdateAvailable = true
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "Supabase check failed, fallback to manifest: ${e.message}")
        }

        // ── Strategy 2: Fallback ke manifest.json (GitHub raw URL) ──
        // v7.9.40: Manifest berisi launcher info, jadi user tetap dapat update
        // notification tanpa perlu SQL insert ke app_releases
        try {
            val manifestUrl = "https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json?t=${System.currentTimeMillis()}"
            val conn = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "DLavie-Launcher")
                connect()
            }
            try {
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val manifest = JSONObject(text)
                    val launcher = manifest.optJSONObject("launcher") ?: return null
                    val latestCode = launcher.optInt("latest_version_code", 0)
                    if (latestCode > currentCode) {
                        val apkUrl = launcher.optString("apk_url", "")
                        if (apkUrl.isNotBlank()) {
                            android.util.Log.i("AppUpdate", "Update found via manifest: v$latestCode")
                            return UpdateInfo(
                                versionName = launcher.optString("latest_version_name", "unknown"),
                                versionCode = latestCode,
                                releaseNotes = launcher.optString("release_notes", "Update terbaru tersedia").toString(),
                                apkUrl = apkUrl,
                                isPublished = true,
                                isUpdateAvailable = true
                            )
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "Manifest check failed: ${e.message}")
        }

        return null
    }

    /**
     * Download APK ke cache dir dengan progress callback.
     * Handles GitHub redirect (301/302) automatically.
     * v7.5.4: Throw exception dengan pesan yang jelas kalau gagal (jangan swallow).
     */
    suspend fun downloadApk(context: Context, apkUrl: String, onProgress: ((Float) -> Unit)? = null): File? {
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
                throw Exception("Update file not found on server. Release may not be published yet.")
            }
            if (responseCode !in 200..299) {
                conn.disconnect()
                throw Exception("Server returned HTTP $responseCode. Try again later.")
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
                throw Exception("Downloaded file too small. Server may have returned an error page.")
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
     * v7.2.8: Removed browser fallback — kalau FileProvider gagal, return false
     * (caller handle dengan menampilkan pesan error, bukan buka browser).
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
