package com.drmacze.f16launcher

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

const val INSTALL_MANIFEST_URL = "https://raw.githubusercontent.com/drmacze/F16/main/updates/install.json"

data class InstallAsset(
    val fileName: String,
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
    val versionName: String,
    val target: String = "",
    val required: Boolean = true,
    val published: Boolean = false
) {
    fun isPublished(): Boolean = published && url.isNotBlank()
}

data class PublicInstallManifest(
    val productName: String,
    val gamePackage: String,
    val statusMessage: String,
    val apk: InstallAsset,
    val data: InstallAsset,
    val obb: InstallAsset
)

data class PublicDownloadStatus(
    val active: Boolean,
    val done: Boolean,
    val progress: Int,
    val label: String
)

class PublicInstallManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("f16_launcher", 0)
    private val downloadManager: DownloadManager by lazy { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    fun fetchInstallManifest(): PublicInstallManifest {
        val json = JSONObject(readUrl(INSTALL_MANIFEST_URL))
        fun asset(name: String): InstallAsset {
            val obj = json.getJSONObject(name)
            return InstallAsset(
                fileName = obj.optString("fileName", "$name.zip"),
                url = obj.optString("url", ""),
                sha256 = obj.optString("sha256", ""),
                sizeBytes = obj.optLong("sizeBytes", 0L),
                versionName = obj.optString("versionName", "-"),
                target = obj.optString("target", ""),
                required = obj.optBoolean("required", true),
                published = obj.optBoolean("published", obj.optString("url", "").isNotBlank())
            )
        }
        return PublicInstallManifest(
            productName = json.optString("productName", "DLavie 26"),
            gamePackage = json.optString("gamePackage", DevPatchEngine.GAME_PACKAGE),
            statusMessage = json.optJSONObject("status")?.optString("message", "Ready") ?: "Ready",
            apk = asset("apk"),
            data = asset("data"),
            obb = asset("obb")
        )
    }

    fun startPersistentDownload(asset: InstallAsset) {
        if (!asset.isPublished()) throw IllegalStateException("Konten belum dipublish: ${asset.fileName}")
        val out = assetFile(asset)
        out.parentFile?.mkdirs()
        if (isValidCachedFile(asset, out)) {
            val key = PersistentDownloadService.key(asset.fileName)
            prefs.edit()
                .putString(PersistentDownloadService.statusKey(key), "done")
                .putBoolean(PersistentDownloadService.activeKey(key), false)
                .putInt(PersistentDownloadService.progressKey(key), 100)
                .putString(PersistentDownloadService.pathKey(key), out.absolutePath)
                .apply()
            return
        }
        PersistentDownloadService.start(context, asset)
    }

    fun downloadAsset(asset: InstallAsset, onProgress: (Int) -> Unit): File {
        if (!asset.isPublished()) throw IllegalStateException("Konten belum dipublish: ${asset.fileName}")
        val out = assetFile(asset)
        out.parentFile?.mkdirs()
        if (isValidCachedFile(asset, out)) {
            onProgress(100)
            return out
        }

        val id = activeOrNewDownload(asset, out)
        prefs.edit()
            .putString("active_download_file", asset.fileName)
            .putString("active_download_label", asset.versionName)
            .apply()

        while (true) {
            val row = queryDownload(id)
            if (row == null) {
                clearDownload(asset)
                return downloadAsset(asset, onProgress)
            }
            when (row.status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    clearDownload(asset)
                    if (!out.exists()) throw IllegalStateException("Download selesai tapi file tidak ditemukan: ${asset.fileName}")
                    if (!isValidCachedFile(asset, out)) throw IllegalStateException("SHA-256 tidak cocok untuk ${asset.fileName}")
                    onProgress(100)
                    prefs.edit().remove("active_download_file").remove("active_download_label").apply()
                    return out
                }
                DownloadManager.STATUS_FAILED -> {
                    clearDownload(asset)
                    if (out.exists()) out.delete()
                    throw IllegalStateException("Download gagal untuk ${asset.fileName}. Reason ${row.reason}")
                }
                else -> {
                    onProgress(row.progress)
                    try {
                        Thread.sleep(1000L)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IllegalStateException("Download berjalan di background. Buka Setup lagi untuk melanjutkan.")
                    }
                }
            }
        }
    }

    fun downloadStatus(asset: InstallAsset): PublicDownloadStatus {
        val out = assetFile(asset)
        if (isValidCachedFile(asset, out)) return PublicDownloadStatus(active = false, done = true, progress = 100, label = "Downloaded")

        val key = PersistentDownloadService.key(asset.fileName)
        val serviceStatus = prefs.getString(PersistentDownloadService.statusKey(key), "") ?: ""
        val serviceActive = prefs.getBoolean(PersistentDownloadService.activeKey(key), false)
        val serviceProgress = prefs.getInt(PersistentDownloadService.progressKey(key), 0).coerceIn(0, 100)
        when (serviceStatus) {
            "downloading" -> if (serviceActive) return PublicDownloadStatus(active = true, done = false, progress = serviceProgress, label = "Downloading")
            "done" -> return PublicDownloadStatus(active = false, done = isValidCachedFile(asset, out), progress = 100, label = if (out.exists()) "Downloaded" else "Missing")
            "failed" -> return PublicDownloadStatus(active = false, done = false, progress = serviceProgress, label = "Failed ${prefs.getString(PersistentDownloadService.errorKey(key), "")}")
        }

        val id = prefs.getLong(downloadIdKey(asset), -1L)
        if (id <= 0L) return PublicDownloadStatus(active = false, done = false, progress = 0, label = "Not started")
        val row = queryDownload(id) ?: return PublicDownloadStatus(active = false, done = false, progress = 0, label = "Missing")
        return when (row.status) {
            DownloadManager.STATUS_SUCCESSFUL -> PublicDownloadStatus(active = false, done = out.exists(), progress = 100, label = "Downloaded")
            DownloadManager.STATUS_FAILED -> PublicDownloadStatus(active = false, done = false, progress = row.progress, label = "Failed ${row.reason}")
            DownloadManager.STATUS_PAUSED -> PublicDownloadStatus(active = true, done = false, progress = row.progress, label = "Paused")
            DownloadManager.STATUS_PENDING -> PublicDownloadStatus(active = true, done = false, progress = row.progress, label = "Pending")
            else -> PublicDownloadStatus(active = true, done = false, progress = row.progress, label = "Downloading")
        }
    }

    fun cachedAssetFile(asset: InstallAsset): File = assetFile(asset)

    fun isAssetDownloaded(asset: InstallAsset): Boolean = isValidCachedFile(asset, assetFile(asset))

    fun openApkInstaller(apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    private fun activeOrNewDownload(asset: InstallAsset, out: File): Long {
        val existingId = prefs.getLong(downloadIdKey(asset), -1L)
        if (existingId > 0L) {
            val row = queryDownload(existingId)
            if (row != null && row.status != DownloadManager.STATUS_FAILED) return existingId
            clearDownload(asset)
        }
        if (out.exists()) out.delete()
        val request = DownloadManager.Request(Uri.parse(asset.url))
            .setTitle(asset.fileName)
            .setDescription("DLavie download: ${asset.versionName}")
            .setMimeType(if (asset.fileName.endsWith(".apk", true)) "application/vnd.android.package-archive" else "application/zip")
            .addRequestHeader("User-Agent", "DLavie-Launcher")
            .addRequestHeader("Accept", "application/octet-stream")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(context, null, "public-install/${asset.fileName}")
        val id = downloadManager.enqueue(request)
        prefs.edit()
            .putLong(downloadIdKey(asset), id)
            .putString(downloadPathKey(asset), out.absolutePath)
            .apply()
        return id
    }

    private fun queryDownload(id: Long): DownloadRow? {
        val query = DownloadManager.Query().setFilterById(id)
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return DownloadRow(
                status = cursor.intValue(DownloadManager.COLUMN_STATUS),
                reason = cursor.intValue(DownloadManager.COLUMN_REASON),
                soFar = cursor.longValue(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                total = cursor.longValue(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            )
        }
    }

    private fun clearDownload(asset: InstallAsset) {
        prefs.edit().remove(downloadIdKey(asset)).remove(downloadPathKey(asset)).apply()
    }

    private fun assetFile(asset: InstallAsset): File = File(File(context.getExternalFilesDir(null), "public-install"), asset.fileName)

    private fun isValidCachedFile(asset: InstallAsset, file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        if (asset.sizeBytes > 0L && file.length() != asset.sizeBytes) return false
        if (asset.sha256.isNotBlank()) return asset.sha256.equals(sha256(file), ignoreCase = true)
        return true
    }

    private fun readUrl(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        return try { BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() } } finally { conn.disconnect() }
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(131072)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    private fun downloadIdKey(asset: InstallAsset): String = "download_id_${safeKey(asset.fileName)}"
    private fun downloadPathKey(asset: InstallAsset): String = "download_path_${safeKey(asset.fileName)}"
    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9_]"), "_")

    private fun Cursor.intValue(column: String): Int = getInt(getColumnIndexOrThrow(column))
    private fun Cursor.longValue(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private data class DownloadRow(val status: Int, val reason: Int, val soFar: Long, val total: Long) {
        val progress: Int = if (total > 0L) ((soFar * 100L) / total).toInt().coerceIn(0, 100) else 0
    }
}
