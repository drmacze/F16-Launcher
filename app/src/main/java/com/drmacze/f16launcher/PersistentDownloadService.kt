package com.drmacze.f16launcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

class PersistentDownloadService : Service() {
    private val prefs by lazy { getSharedPreferences("f16_launcher", 0) }
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        val fileName = intent.getStringExtra(EXTRA_FILE) ?: return START_NOT_STICKY
        val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val sha = intent.getStringExtra(EXTRA_SHA) ?: ""
        val size = intent.getLongExtra(EXTRA_SIZE, 0L)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: fileName
        val key = key(fileName)

        if (worker?.isAlive == true) {
            updateNotification(label, progressFor(key), "Download berjalan")
            return START_STICKY
        }

        createChannel()
        startForeground(NOTIF_ID, notification(label, progressFor(key), "Mulai download"))
        worker = Thread {
            runDownload(key, fileName, url, sha, size, label)
        }.also { it.start() }
        return START_STICKY
    }

    private fun runDownload(key: String, fileName: String, url: String, sha: String, size: Long, label: String) {
        val out = File(File(getExternalFilesDir(null), "public-install"), fileName)
        val part = File(out.parentFile, "$fileName.part")
        out.parentFile?.mkdirs()
        prefs.edit()
            .putString(statusKey(key), "downloading")
            .putBoolean(activeKey(key), true)
            .putString(errorKey(key), "")
            .putString(pathKey(key), out.absolutePath)
            .apply()

        try {
            if (out.exists() && isValid(out, sha, size)) {
                markDone(key, out, label)
                return
            }

            var existing = if (part.exists()) part.length() else 0L
            var conn = open(url, existing)
            if (existing > 0L && conn.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                part.delete()
                existing = 0L
                conn.disconnect()
                conn = open(url, 0L)
            }
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")

            val total = when {
                size > 0L -> size
                existing > 0L && conn.contentLengthLong > 0L -> existing + conn.contentLengthLong
                else -> conn.contentLengthLong
            }

            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(part, existing > 0L).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var copied = existing
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        copied += n
                        val progress = if (total > 0L) ((copied * 100L) / total).toInt().coerceIn(0, 100) else 0
                        prefs.edit()
                            .putInt(progressKey(key), progress)
                            .putLong(bytesKey(key), copied)
                            .putLong(totalKey(key), total)
                            .apply()
                        updateNotification(label, progress, "Downloading")
                    }
                }
            }
            conn.disconnect()

            if (out.exists()) out.delete()
            if (!part.renameTo(out)) throw IllegalStateException("Gagal simpan file final")
            if (!isValid(out, sha, size)) throw IllegalStateException("SHA-256 atau size tidak cocok")
            markDone(key, out, label)
        } catch (t: Throwable) {
            prefs.edit()
                .putString(statusKey(key), "failed")
                .putBoolean(activeKey(key), false)
                .putString(errorKey(key), t.message ?: "unknown")
                .apply()
            updateNotification(label, progressFor(key), "Gagal: ${t.message ?: "unknown"}")
        } finally {
            stopForeground(false)
            stopSelf()
        }
    }

    private fun markDone(key: String, out: File, label: String) {
        prefs.edit()
            .putString(statusKey(key), "done")
            .putBoolean(activeKey(key), false)
            .putInt(progressKey(key), 100)
            .putString(pathKey(key), out.absolutePath)
            .putString(errorKey(key), "")
            .apply()
        updateNotification(label, 100, "Download selesai")
    }

    private fun open(url: String, existing: Long): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 120000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "DLavie-Launcher")
        if (existing > 0L) conn.setRequestProperty("Range", "bytes=$existing-")
        return conn
    }

    private fun isValid(file: File, sha: String, size: Long): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        if (size > 0L && file.length() != size) return false
        if (sha.isBlank()) return true
        return sha.equals(sha256(file), ignoreCase = true)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "DLavie Downloads", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun updateNotification(title: String, progress: Int, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification(title, progress, text))
    }

    private fun notification(title: String, progress: Int, text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("DLavie: $title")
        .setContentText(text)
        .setOngoing(progress in 0..99)
        .setOnlyAlertOnce(true)
        .setProgress(100, progress.coerceIn(0, 100), false)
        .build()

    private fun progressFor(key: String): Int = prefs.getInt(progressKey(key), 0)

    companion object {
        const val ACTION_START = "com.drmacze.f16launcher.DOWNLOAD_START"
        const val EXTRA_FILE = "file"
        const val EXTRA_URL = "url"
        const val EXTRA_SHA = "sha"
        const val EXTRA_SIZE = "size"
        const val EXTRA_LABEL = "label"
        private const val CHANNEL_ID = "dlavie_downloads"
        private const val NOTIF_ID = 2601

        fun start(context: Context, asset: InstallAsset) {
            val intent = Intent(context, PersistentDownloadService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_FILE, asset.fileName)
                .putExtra(EXTRA_URL, asset.url)
                .putExtra(EXTRA_SHA, asset.sha256)
                .putExtra(EXTRA_SIZE, asset.sizeBytes)
                .putExtra(EXTRA_LABEL, asset.fileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }

        fun key(fileName: String): String = fileName.replace(Regex("[^A-Za-z0-9_]"), "_")
        fun statusKey(key: String) = "pdl_status_$key"
        fun activeKey(key: String) = "pdl_active_$key"
        fun progressKey(key: String) = "pdl_progress_$key"
        fun errorKey(key: String) = "pdl_error_$key"
        fun pathKey(key: String) = "pdl_path_$key"
        fun bytesKey(key: String) = "pdl_bytes_$key"
        fun totalKey(key: String) = "pdl_total_$key"
    }
}
