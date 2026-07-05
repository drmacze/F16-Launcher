package com.drmacze.f16launcher

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * In-App APK Downloader — download APK langsung dari launcher TANPA mengarahkan
 * ke browser. Menggunakan Android DownloadManager untuk reliability + resume.
 *
 * User flow:
 * 1. startDownload() → register BroadcastReceiver untuk ACTION_DOWNLOAD_COMPLETE
 * 2. Poll getProgress() secara periodic (every 1s) untuk update UI progress bar
 * 3. On complete → BroadcastReceiver fires → call openInstaller() to install APK
 *
 * Keunggulan vs Intent.ACTION_VIEW (browser):
 * - User tidak keluar dari app
 * - Progress terlihat real-time di launcher UI
 * - Download tetap jalan walau app di-background (DownloadManager handles)
 * - Auto-resume kalau koneksi putus
 * - File tersimpan di app cache (FileProvider) → bisa install langsung
 */
class ApkDownloader(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "dlavie_apk_downloads"
        private const val PREFIX_DL_ID = "dl_id_"
        private const val PREFIX_PATH = "dl_path_"
        private const val PREFIX_LABEL = "dl_label_"
    }

    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val activeDownloads = mutableMapOf<Long, String>()  // downloadId → fileKey
    private var completionListener: ((fileKey: String, file: File, success: Boolean, error: String?) -> Unit)? = null
    private var receiverRegistered = false

    private val completionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id <= 0L) return

            val fileKey = activeDownloads.remove(id) ?: return
            val path = prefs.getString(pathKey(fileKey), "") ?: ""
            val label = prefs.getString(labelKey(fileKey), "APK") ?: "APK"

            if (path.isBlank()) {
                completionListener?.invoke(fileKey, File(""), false, "Path tidak ditemukan")
                return
            }

            val file = File(path)
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id))
            cursor?.use {
                if (it.moveToFirst()) {
                    val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            // Verify file exists and is non-empty
                            if (file.exists() && file.length() > 1_000_000) {
                                completionListener?.invoke(fileKey, file, true, null)
                            } else {
                                file.delete()
                                completionListener?.invoke(fileKey, File(""), false, "File APK tidak valid (size < 1 MB)")
                            }
                        }
                        DownloadManager.STATUS_FAILED -> {
                            file.delete()
                            completionListener?.invoke(fileKey, File(""), false, "Download gagal. Reason: $reason")
                        }
                    }
                }
            }
        }
    }

    /**
     * Start download APK dari URL ke app cache.
     *
     * @param fileKey unique key untuk identify download (e.g. "fifa15-apk", "launcher-latest")
     * @param url download URL (akan diakses via DLavie proxy)
     * @param fileName target filename (e.g. "DLavie15.apk")
     * @param label human-readable label untuk notifikasi
     * @return true kalau download dimulai, false kalau sudah ada download aktif
     */
    fun startDownload(fileKey: String, url: String, fileName: String, label: String): Boolean {
        // Check if there's already an ACTIVE download for this key
        // (running/pending/paused in DownloadManager)
        val existingId = prefs.getLong(idKey(fileKey), -1L)
        if (existingId > 0L && isDownloadActive(existingId)) {
            // Active download exists — but check if it's the SAME url to avoid duplicate
            // If user taps download again, just continue polling existing download
            return false  // Already downloading — caller should poll getProgress()
        }

        // Clean up stale download ID (completed/failed/cancelled from previous session)
        if (existingId > 0L) {
            try { downloadManager.remove(existingId) } catch (_: Throwable) {}
            activeDownloads.remove(existingId)
        }

        // Cleanup old file (in case it's a partial/corrupt download from before)
        val outDir = File(context.getExternalFilesDir(null), "apk-downloads").also { it.mkdirs() }
        val outFile = File(outDir, fileName)
        if (outFile.exists()) outFile.delete()

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("DLavie: $label")
            setDescription("Mengunduh $fileName")
            setMimeType("application/vnd.android.package-archive")
            addRequestHeader("User-Agent", "DLavie-Launcher/7.0 (Android)")
            addRequestHeader("Accept", "application/vnd.android.package-archive, application/octet-stream")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setDestinationInExternalFilesDir(context, null, "apk-downloads/$fileName")
        }

        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Throwable) {
            return false
        }

        activeDownloads[downloadId] = fileKey
        // IMPORTANT: use commit() (sync) instead of apply() (async) so that
        // getProgress() called immediately after startDownload() returns the
        // correct downloadId. apply() is async and may not flush in time,
        // causing getProgress() to read stale downloadId and return wrong state.
        prefs.edit()
            .putLong(idKey(fileKey), downloadId)
            .putString(pathKey(fileKey), outFile.absolutePath)
            .putString(labelKey(fileKey), label)
            .commit()  // sync flush

        // Register receiver once
        if (!receiverRegistered) {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(completionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(completionReceiver, filter)
            }
            receiverRegistered = true
        }

        return true
    }

    /**
     * Get current download progress untuk fileKey.
     * Returns DownloadProgress(active=false, progress=100, ...) HANYA jika ada
     * download ID yang sudah selesai (STATUS_SUCCESSFUL) di DownloadManager.
     *
     * BUG FIX v7.2.4: Jangan return done=true hanya karena file ada di cache.
     * DownloadManager menulis ke file tujuan langsung selama download, jadi
     * file.exists() true tidak menjamin download selesai. Hanya rely ke
     * STATUS_SUCCESSFUL dari DownloadManager.Query.
     */
    fun getProgress(fileKey: String): DownloadProgress {
        val downloadId = prefs.getLong(idKey(fileKey), -1L)
        val path = prefs.getString(pathKey(fileKey), "") ?: ""
        val label = prefs.getString(labelKey(fileKey), "") ?: ""

        // No download ID — never started or cleared
        if (downloadId <= 0L) {
            return DownloadProgress(active = false, done = false, progress = 0, label = label)
        }

        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        cursor?.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val soFar = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))

                val progress = if (total > 0) ((soFar * 100L) / total).toInt().coerceIn(0, 100) else 0

                return when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadProgress(
                        active = false, done = true, progress = 100,
                        downloadedBytes = soFar, totalBytes = total, label = label,
                        filePath = path
                    )
                    DownloadManager.STATUS_FAILED -> DownloadProgress(
                        active = false, done = false, progress = progress,
                        downloadedBytes = soFar, totalBytes = total, label = label,
                        error = "Download gagal (reason=$reason)"
                    )
                    DownloadManager.STATUS_PAUSED -> DownloadProgress(
                        active = true, done = false, progress = progress,
                        downloadedBytes = soFar, totalBytes = total, label = label,
                        error = "Paused (reason=$reason)"
                    )
                    DownloadManager.STATUS_PENDING -> DownloadProgress(
                        active = true, done = false, progress = 0,
                        downloadedBytes = 0, totalBytes = total, label = label,
                        error = "Pending..."
                    )
                    else -> DownloadProgress(
                        active = true, done = false, progress = progress,
                        downloadedBytes = soFar, totalBytes = total, label = label
                    )
                }
            }
        }

        // Download ID exists in prefs but not found in DownloadManager.
        // This means: download completed & cleaned from DownloadManager, OR
        // download was cancelled. Check if file exists and is valid — if yes,
        // it's a completed download from previous session.
        if (path.isNotBlank()) {
            val file = File(path)
            if (file.exists() && file.length() > 1_000_000) {
                return DownloadProgress(
                    active = false,
                    done = true,
                    progress = 100,
                    downloadedBytes = file.length(),
                    totalBytes = file.length(),
                    label = label,
                    filePath = file.absolutePath
                )
            }
        }

        // Download ID not found in DownloadManager and file doesn't exist
        // → download was cancelled or failed silently. Reset state.
        return DownloadProgress(active = false, done = false, progress = 0, label = label)
    }

    /** True kalau download dengan ID ini masih aktif (running/pending/paused). */
    private fun isDownloadActive(downloadId: Long): Boolean {
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        cursor?.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                return status == DownloadManager.STATUS_RUNNING ||
                       status == DownloadManager.STATUS_PENDING ||
                       status == DownloadManager.STATUS_PAUSED
            }
        }
        return false
    }

    /** Cancel active download (kalau ada). */
    fun cancelDownload(fileKey: String) {
        val downloadId = prefs.getLong(idKey(fileKey), -1L)
        if (downloadId > 0L) {
            try { downloadManager.remove(downloadId) } catch (_: Throwable) {}
            activeDownloads.remove(downloadId)
        }
        val path = prefs.getString(pathKey(fileKey), "") ?: ""
        if (path.isNotBlank()) File(path).delete()
        prefs.edit().remove(idKey(fileKey)).remove(pathKey(fileKey)).remove(labelKey(fileKey)).apply()
    }

    /** Set listener untuk callback saat download selesai (sukses/gagal). */
    fun setCompletionListener(listener: (fileKey: String, file: File, success: Boolean, error: String?) -> Unit) {
        completionListener = listener
    }

    /**
     * Open APK installer (setelah download selesai).
     * Menggunakan FileProvider agar kompatibel dengan Android 7+.
     */
    fun openInstaller(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() < 1_000_000) return false
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Cleanup old downloads (call on app startup untuk reclaim storage). */
    fun cleanupOldDownloads() {
        val dir = File(context.getExternalFilesDir(null), "apk-downloads")
        if (!dir.exists()) return
        // Delete files older than 7 days
        val cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.lastModified() < cutoff) f.delete()
        }
    }

    /** Unregister receiver (call on Activity onDestroy). */
    fun cleanup() {
        if (receiverRegistered) {
            try { context.unregisterReceiver(completionReceiver) } catch (_: Throwable) {}
            receiverRegistered = false
        }
    }

    private fun idKey(fileKey: String) = PREFIX_DL_ID + fileKey
    private fun pathKey(fileKey: String) = PREFIX_PATH + fileKey
    private fun labelKey(fileKey: String) = PREFIX_LABEL + fileKey

    data class DownloadProgress(
        val active: Boolean,
        val done: Boolean,
        val progress: Int,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val label: String = "",
        val filePath: String = "",
        val error: String? = null
    )
}
