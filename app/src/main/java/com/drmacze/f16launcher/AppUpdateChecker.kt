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
            val isStaff = api.role().equals("admin", ignoreCase = true) ||
                         api.role().equals("developer", ignoreCase = true) ||
                         api.role().equals("owner", ignoreCase = true) ||
                         api.role().equals("moderator", ignoreCase = true)

            val currentCode = BuildConfig.VERSION_CODE

            // Query Supabase app_releases
            // Staff: semua releases dengan version_code > current
            // Regular user: hanya published releases dengan version_code > current
            val filter = if (isStaff) {
                "version_code=gt.$currentCode"
            } else {
                "version_code=gt.$currentCode&is_published=eq.true"
            }

            val response = api.requestPublic(
                "GET",
                "/rest/v1/app_releases?$filter&order=version_code.desc&limit=1&select=version_code,version_name,tag_name,apk_download_url,changelog,is_published"
            )

            val arr = JSONArray(response)
            if (arr.length() == 0) return null

            val release = arr.getJSONObject(0)
            val versionCode = release.optInt("version_code", 0)
            if (versionCode <= currentCode) return null

            UpdateInfo(
                versionName = release.optString("version_name", "unknown"),
                versionCode = versionCode,
                releaseNotes = release.optString("changelog", ""),
                apkUrl = release.optString("apk_download_url", ""),
                isPublished = release.optBoolean("is_published", false),
                isUpdateAvailable = true
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Download APK ke cache dir dengan progress callback.
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
     * Trigger install APK via ACTION_VIEW + FileProvider.
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
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/drmacze/F16-Launcher/releases"))
                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(browserIntent)
            } catch (_: Throwable) { }
        }
    }
}
