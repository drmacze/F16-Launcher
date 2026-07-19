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
        val isUpdateAvailable: Boolean,
        val apkSizeMb: String = ""  // v7.9.90: APK size for update popup
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
    suspend fun checkForUpdate(api: CommunityApi, context: Context? = null): UpdateInfo? {
        val currentCode = BuildConfig.VERSION_CODE

        // v8.0.22: GITHUB-ONLY MODE — hapus Supabase dependency untuk update checker
        // Supabase restricted (quota exceeded), pakai manifest.json sebagai sumber tunggal
        // manifest.json auto-update oleh GitHub Actions workflow (auto-release.yml)
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
                            val sizeMb = if (context != null) fetchUpdateDeltaSize(context, apkUrl) else ""
                            // Get release notes from array
                            val notesArr = launcher.optJSONArray("release_notes")
                            val notes = if (notesArr != null && notesArr.length() > 0) notesArr.getString(0) else "Update terbaru tersedia"
                            return UpdateInfo(
                                versionName = launcher.optString("latest_version_name", "unknown"),
                                versionCode = latestCode,
                                releaseNotes = notes,
                                apkUrl = apkUrl,
                                isPublished = true,
                                isUpdateAvailable = true,
                                apkSizeMb = sizeMb
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
     * v7.9.49: Tambah retry logic (3 attempts) supaya download tidak gagal.
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

    /**
     * v7.9.93: Fetch UPDATE DELTA size — perubahan size dari APK terinstall ke APK baru.
     *
     * Cara kerja:
     * 1. Get size APK yang sedang terinstall (PackageInfo.sourceDir → File.length())
     * 2. Get size APK baru via HTTP HEAD request (follow redirects)
     * 3. Delta = new_size - installed_size
     * 4. Format sebagai "+X.X MB" (naik) atau "-X.X MB" (turun) atau "0 MB" (sama)
     *
     * Ini menampilkan SIZE PERUBAHAN, bukan full APK download size.
     * User tahu "oh cuma 4MB perubahan" bukan "28MB full download".
     *
     * Return formatted string atau "" kalau gagal.
     */
    private fun fetchUpdateDeltaSize(context: Context, apkUrl: String): String {
        return try {
            // Step 1: Get installed APK size
            val installedSize = try {
                val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                java.io.File(pkgInfo.applicationInfo?.sourceDir ?: "").length()
            } catch (e: Exception) {
                android.util.Log.w("AppUpdate", "Cannot get installed APK size: ${e.message}")
                0L
            }

            // Step 2: Get new APK size via HEAD request
            val newSize = fetchRemoteApkSizeBytes(apkUrl)

            if (newSize <= 0L) {
                android.util.Log.w("AppUpdate", "Cannot fetch remote APK size")
                return ""
            }

            // v8.0.08: Show actual DOWNLOAD size (not delta) — user downloads full APK
            // Delta was misleading (showing "0 MB" when APKs are similar size)
            val sizeMb = newSize / (1024.0 * 1024.0)
            val formatted = if (sizeMb >= 1.0) "%.1f MB".format(sizeMb) else "${newSize / 1024} KB"
            android.util.Log.i("AppUpdate", "Download size: ${newSize} bytes = $formatted")
            formatted
        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "fetchUpdateDeltaSize failed: ${e.message}")
            ""
        }
    }

    /**
     * Fetch raw byte size dari remote APK URL via HTTP HEAD (follow redirects).
     */
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

    /**
     * Format delta bytes ke human-readable string.
     * Positif (naik): "+4.2 MB"
     * Negatif (turun): "-1.3 MB"
     * Nol: "0 MB"
     * Kecil (< 1MB): "+450 KB"
     */
    private fun formatDeltaSize(deltaBytes: Long): String {
        val absBytes = kotlin.math.abs(deltaBytes)
        val sign = if (deltaBytes > 0) "+" else if (deltaBytes < 0) "-" else ""
        return when {
            absBytes >= 1_000_000 -> {
                val mb = absBytes / (1024.0 * 1024.0)
                "${sign}%.1f MB".format(mb)
            }
            absBytes >= 1_000 -> "${sign}${absBytes / 1024} KB"
            absBytes > 0 -> "${sign}${absBytes} B"
            else -> "0 MB"
        }
    }
}
