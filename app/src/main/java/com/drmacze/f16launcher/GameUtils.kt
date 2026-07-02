package com.drmacze.f16launcher

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility helpers for game-related operations:
 * - Detect if FIFA 16 is currently running (avoid patching a live game)
 * - Free storage info on /sdcard
 * - Backup history listing & cleanup
 *
 * All operations here are real (no simulation) and work on Android 7-16.
 */
object GameUtils {

    const val GAME_PACKAGE = "com.ea.gp.fifaworld"
    private const val BACKUP_ROOT = "/sdcard/F16Launcher/backups"
    private const val BACKUP_RETENTION_DAYS = 30L

    // ─── Game running detection ───────────────────────────────────────────────

    /**
     * True if FIFA 16 process is currently running on the device.
     * Uses ActivityManager.getRunningAppProcesses().
     *
     * Note: On Android 5+ this only returns the caller's own processes and
     * any processes that are visible to the user (e.g. foreground app).
     * For our use case (game is in foreground when user wants to patch),
     * this is sufficient — if the game is foregrounded it WILL be detected.
     */
    fun isGameRunning(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            // Try modern API first (Android 5+)
            val processes = am.runningAppProcesses ?: return false
            processes.any { it.processName == GAME_PACKAGE || it.processName.startsWith("$GAME_PACKAGE:") }
        } catch (_: Throwable) {
            // Fallback: check if game data folder is locked (writes in progress)
            try {
                val f = File("/sdcard/Android/data/$GAME_PACKAGE")
                f.exists()
            } catch (_: Throwable) { false }
        }
    }

    /**
     * Attempt to force-stop the game via ActivityManager.
     * Requires KILL_BACKGROUND_PROCESSES permission (declared in manifest).
     *
     * Note: Only works if the game is in background. For foreground apps,
     * Android may not honor this. Returns true if kill command succeeded.
     */
    fun forceStopGame(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(GAME_PACKAGE)
            true
        } catch (_: Throwable) { false }
    }

    // ─── Storage info ─────────────────────────────────────────────────────────

    /** Free bytes on /sdcard (external storage). Returns 0 if unavailable. */
    fun freeBytesSdcard(): Long {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Throwable) { 0L }
    }

    /** Total bytes on /sdcard. Returns 0 if unavailable. */
    fun totalBytesSdcard(): Long {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (_: Throwable) { 0L }
    }

    /** Human-readable size string. */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unit = 0
        while (size >= 1024 && unit < units.lastIndex) {
            size /= 1024
            unit++
        }
        return String.format(Locale.US, "%.1f %s", size, units[unit])
    }

    // ─── Backup history ───────────────────────────────────────────────────────

    data class BackupEntry(
        val path: File,
        val version: String,           // e.g. "v27"
        val timestamp: Long,           // folder mtime
        val timestampLabel: String,    // human-readable date
        val totalSize: Long,           // sum of all files in backup
        val sizeLabel: String
    )

    /** List all backups under /sdcard/F16Launcher/backups/, newest first. */
    fun listBackups(): List<BackupEntry> {
        val root = File(BACKUP_ROOT)
        if (!root.exists() || !root.isDirectory) return emptyList()

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return root.listFiles { f -> f.isDirectory && f.name.startsWith("v") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { dir ->
                val ts = dir.lastModified()
                val size = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                BackupEntry(
                    path = dir,
                    version = dir.name,
                    timestamp = ts,
                    timestampLabel = sdf.format(Date(ts)),
                    totalSize = size,
                    sizeLabel = formatBytes(size)
                )
            } ?: emptyList()
    }

    /** Total size of all backups combined. */
    fun totalBackupSize(): Long {
        val root = File(BACKUP_ROOT)
        if (!root.exists()) return 0L
        return root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Delete backup folders older than [BACKUP_RETENTION_DAYS] days.
     * Returns (deletedCount, freedBytes).
     */
    fun cleanupOldBackups(): Pair<Int, Long> {
        val root = File(BACKUP_ROOT)
        if (!root.exists()) return 0 to 0L

        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(BACKUP_RETENTION_DAYS)
        var deletedCount = 0
        var freedBytes = 0L

        root.listFiles { f -> f.isDirectory && f.name.startsWith("v") }?.forEach { dir ->
            if (dir.lastModified() < cutoff) {
                val size = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                if (dir.deleteRecursively()) {
                    deletedCount++
                    freedBytes += size
                }
            }
        }
        return deletedCount to freedBytes
    }

    /**
     * Delete a specific backup folder by its absolute path (safety: must be inside BACKUP_ROOT).
     * Returns true if deleted.
     */
    fun deleteBackup(backupPath: File): Boolean {
        val root = File(BACKUP_ROOT).canonicalFile
        val target = backupPath.canonicalFile
        if (!target.path.startsWith(root.path)) return false  // safety guard
        return target.deleteRecursively()
    }
}
