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
     * @param api CommunityApi instance (untuk auth + role check)
     * @return UpdateInfo atau null kalau tidak ada update
     */
    suspend fun checkForUpdate(api: CommunityApi): UpdateInfo? {
        return try {
            val currentCode = BuildConfig.VERSION_CODE

            // HANYA cek PUBLISHED releases — draft tidak show popup ke siapapun
            // (termasuk admin/developer — mereka test via Dev Dashboard)
            val filter = "version_code=gt.$currentCode&is_published=eq.true"

            val response = api.requestPublic(
                "GET",
                "/rest/v1/app_releases?$filter&order=version_code.desc&limit=1&select=version_code,version_name,tag_name,apk_download_url,changelog,is_published"
            )

            val arr = JSONArray(response)
            if (arr.length() == 0) return null

            val release = arr.getJSONObject(0)
            val versionCode = release.optInt("version_code", 0)
            // Jika versionCode <= currentCode, tidak ada update
            if (versionCode <= currentCode) return null

            // Jika apk_download_url kosong, jangan tampilkan popup (tidak bisa download)
            val apkUrl = release.optString("apk_download_url", "")
            if (apkUrl.isBlank()) return null

            UpdateInfo(
                versionName = release.optString("version_name", "unknown"),
                versionCode = versionCode,
                releaseNotes = release.optString("changelog", ""),
                apkUrl = apkUrl,
                isPublished = release.optBoolean("is_published", false),
                isUpdateAvailable = true
            )
        } catch (_: Throwable) {
            // Tabel app_releases belum ada atau error → tidak show popup (bukan error fatal)
            null
        }
    }

    /**
     * Download APK ke cache dir dengan progress callback.
     * Handles GitHub redirect (301/302) automatically.
     */
    suspend fun downloadApk(context: Context, apkUrl: String, onProgress: ((Float) -> Unit)? = null): File? {
        return try {
            val cacheDir = File(context.cacheDir, "app-updates").also { it.mkdirs() }
            val apkFile = File(cacheDir, "dlavie-update.apk")
            if (apkFile.exists()) apkFile.delete()

            // Follow redirects manually (GitHub release URLs redirect to objects.githubusercontent.com)
            var currentUrl = apkUrl
            var redirectCount = 0
            var conn: HttpURLConnection

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
                    if (location.isNullOrBlank() || redirectCount >= 5) return null
                    currentUrl = location
                    redirectCount++
                    continue
                }
                break
            }

            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
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
