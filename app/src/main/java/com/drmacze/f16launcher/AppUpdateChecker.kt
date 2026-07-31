package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * App Update Checker v4 — GitHub public database only.
 *
 * v325 CHANGE:
 * - All manifest reads go through GitHubPublicDatabase.
 * - Anonymous GET only; no Supabase request and no embedded GitHub token.
 * - jsDelivr, GitHub Contents API and raw GitHub are used as ordered fallbacks.
 */
object AppUpdateChecker {

    /** URL website DLavie — halaman download APK */
    const val DLAVIE_WEBSITE_URL = "https://drmacze.github.io/dlavie-web/"

    /** Any version gap remains non-dismissable, but downloads stay in-app. */
    private const val FORCE_UPDATE_THRESHOLD = 1
    private const val TRUSTED_APK_PREFIX =
        "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/"

    data class UpdateInfo(
        val versionName: String,
        val versionCode: Int,
        val releaseNotes: String,
        val apkUrl: String,
        val isPublished: Boolean,
        val isUpdateAvailable: Boolean,
        val apkSizeMb: String = "",
        val forceUpdate: Boolean = false,
        val currentVersionCode: Int = 0,
        val websiteUrl: String = DLAVIE_WEBSITE_URL
    )

    /**
     * Checks the launcher section in the public GitHub manifest.
     *
     * @param api retained for source compatibility; no Supabase call is made.
     * @param context optional context used to obtain the remote APK size.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun checkForUpdate(api: CommunityApi? = null, context: Context? = null): UpdateInfo? {
        val currentCode = BuildConfig.VERSION_CODE
        val manifestUrl = GitHubPublicDatabase.rawUrl(GitHubPublicDatabase.PublicFile.MANIFEST)
        android.util.Log.i("AppUpdate", "v325 checkForUpdate: current=$currentCode, manifest=$manifestUrl")

        return try {
            // maxAgeMillis=0 forces a fresh version check while retaining multi-source fallback.
            val manifest = GitHubPublicDatabase.fetchObject(
                GitHubPublicDatabase.PublicFile.MANIFEST,
                maxAgeMillis = 0L
            )
            val launcher = manifest.optJSONObject("launcher") ?: run {
                android.util.Log.w("AppUpdate", "Manifest: 'launcher' object missing")
                return null
            }
            val latestCode = launcher.optInt("latest_version_code", 0)
            require(latestCode > 0) { "Manifest tidak memiliki latest_version_code yang valid" }
            android.util.Log.i(
                "AppUpdate",
                "Manifest: latest=$latestCode, current=$currentCode, gap=${latestCode - currentCode}"
            )

            if (latestCode <= currentCode) {
                android.util.Log.i("AppUpdate", "Already up-to-date (current=$currentCode, latest=$latestCode)")
                return null
            }

            val apkUrl = launcher.optString("apk_url", "")
            require(
                apkUrl.startsWith(TRUSTED_APK_PREFIX)
            ) { "Manifest memiliki apk_url yang tidak tepercaya" }

            val gap = latestCode - currentCode
            val forceUpdate = gap >= FORCE_UPDATE_THRESHOLD
            android.util.Log.i("AppUpdate", "Update available: v$latestCode (gap=$gap, forceUpdate=$forceUpdate)")

            val notesRaw = launcher.opt("release_notes")
            val notes = when (notesRaw) {
                is org.json.JSONArray ->
                    (0 until notesRaw.length()).joinToString("\n") { i -> "• ${notesRaw.optString(i)}" }
                is String -> notesRaw
                else -> "Update terbaru tersedia"
            }

            val sizeMb = if (context != null) fetchUpdateDeltaSize(context, apkUrl) else ""

            UpdateInfo(
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
        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "GitHub manifest check failed: ${e.message}", e)
            throw IllegalStateException("Tidak dapat memeriksa versi launcher", e)
        }
    }

    /**
     * Download the official launcher APK into private cache with visible progress.
     * A `.part` file is used until the archive passes structural/package/version
     * validation, preventing an interrupted response from reaching the installer.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: ((Float) -> Unit)? = null,
    ): File? {
        require(apkUrl.startsWith(TRUSTED_APK_PREFIX)) {
            "Tautan pembaruan launcher tidak tepercaya"
        }

        val cacheDir = File(context.cacheDir, "app-updates").also { it.mkdirs() }
        val partialFile = File(cacheDir, "dlavie-update.apk.part")
        val finalFile = File(cacheDir, "dlavie-update.apk")
        partialFile.delete()
        finalFile.delete()

        var lastError: Throwable? = null
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                reportProgress(onProgress, 0f)
                android.util.Log.i("AppUpdate", "Download attempt $attempt/$maxRetries: $apkUrl")
                downloadApkAttempt(apkUrl, partialFile, onProgress)
                validateDownloadedApk(context, partialFile)

                if (!partialFile.renameTo(finalFile)) {
                    partialFile.copyTo(finalFile, overwrite = true)
                    partialFile.delete()
                }
                require(finalFile.isFile && finalFile.length() > 1_000_000L) {
                    "File pembaruan tidak lengkap"
                }
                reportProgress(onProgress, 1f)
                android.util.Log.i("AppUpdate", "Download verified: ${finalFile.length()} bytes")
                return finalFile
            } catch (error: Throwable) {
                lastError = error
                android.util.Log.w(
                    "AppUpdate",
                    "Download attempt $attempt/$maxRetries failed: ${error.message}",
                    error,
                )
                partialFile.delete()
                finalFile.delete()
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(1_500L * attempt)
                }
            }
        }

        throw IllegalStateException(
            lastError?.message ?: "Download gagal setelah $maxRetries percobaan",
            lastError,
        )
    }

    private suspend fun downloadApkAttempt(
        apkUrl: String,
        partialFile: File,
        onProgress: ((Float) -> Unit)?,
    ) {
        var currentUrl = apkUrl
        var redirectCount = 0
        var connection: HttpURLConnection? = null

        try {
            while (true) {
                val requestUrl = URL(currentUrl)
                connection = (requestUrl.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 30_000
                    readTimeout = 120_000
                    setRequestProperty("User-Agent", "DLavie-Launcher/${BuildConfig.VERSION_CODE} (Android)")
                    setRequestProperty(
                        "Accept",
                        "application/vnd.android.package-archive, application/octet-stream, */*",
                    )
                    setRequestProperty("Accept-Encoding", "identity")
                    connect()
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    connection = null
                    require(!location.isNullOrBlank()) {
                        "Redirect tanpa Location header (HTTP $responseCode)"
                    }
                    require(redirectCount < 5) { "Terlalu banyak redirect" }
                    currentUrl = URL(requestUrl, location).toString()
                    redirectCount += 1
                    continue
                }

                when (responseCode) {
                    403 -> throw IllegalStateException("Akses file pembaruan ditolak (HTTP 403)")
                    404 -> throw IllegalStateException("File pembaruan tidak ditemukan (HTTP 404)")
                }
                require(responseCode in 200..299) {
                    "Server pembaruan mengembalikan HTTP $responseCode"
                }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
                val buffer = ByteArray(32 * 1024)
                var downloadedBytes = 0L
                var lastReportedPercent = -1

                connection.inputStream.use { input ->
                    partialFile.outputStream().buffered().use { output ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloadedBytes += count

                            if (totalBytes != null) {
                                val progress = (downloadedBytes.toDouble() / totalBytes.toDouble())
                                    .toFloat()
                                    .coerceIn(0f, 0.99f)
                                val percent = (progress * 100).toInt()
                                if (percent > lastReportedPercent) {
                                    lastReportedPercent = percent
                                    reportProgress(onProgress, progress)
                                }
                            }
                        }
                        output.flush()
                    }
                }

                if (totalBytes != null) {
                    require(downloadedBytes == totalBytes) {
                        "Download terputus: $downloadedBytes dari $totalBytes byte"
                    }
                }
                return
            }
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun reportProgress(
        callback: ((Float) -> Unit)?,
        value: Float,
    ) {
        if (callback == null) return
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
            callback(value.coerceIn(0f, 1f))
        }
    }

    @Suppress("DEPRECATION")
    private fun validateDownloadedApk(context: Context, apkFile: File) {
        require(apkFile.isFile && apkFile.length() > 1_000_000L) {
            "File pembaruan terlalu kecil atau tidak lengkap"
        }

        apkFile.inputStream().use { input ->
            val signature = ByteArray(4)
            require(input.read(signature) == signature.size) {
                "File pembaruan tidak dapat dibaca"
            }
            require(signature[0] == 0x50.toByte() && signature[1] == 0x4B.toByte()) {
                "File yang diterima bukan APK yang valid"
            }
        }

        val archive = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: throw IllegalStateException("Android tidak dapat membaca APK pembaruan")
        require(archive.packageName == context.packageName) {
            "APK pembaruan memiliki package yang tidak sesuai"
        }

        val archiveVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            archive.versionCode.toLong()
        }
        require(archiveVersion > BuildConfig.VERSION_CODE.toLong()) {
            "APK yang diunduh bukan versi yang lebih baru"
        }
    }

    /** Trigger the Android package installer for a verified cached APK. */
    @Suppress("DEPRECATION")
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                apkFile,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("DLavie update", uri)
            }

            context.packageManager.queryIntentActivities(intent, 0).forEach { target ->
                context.grantUriPermission(
                    target.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            context.startActivity(intent)
            true
        } catch (error: Throwable) {
            android.util.Log.e("DLavie", "installApk failed", error)
            false
        }
    }

    /** Opens the official website as the safe manual-install fallback. */
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

    /** Fetches the full remote APK size. */
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
                    setRequestProperty("User-Agent", "DLavie-Launcher/${BuildConfig.VERSION_CODE}")
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
