package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import java.io.File

/**
 * Helper for managing MANAGE_EXTERNAL_STORAGE ("All Files Access") permission.
 *
 * Why this exists:
 * - On Android 11+, apps with targetSdk >= 30 cannot write to
 *   /sdcard/Android/data/<other_package>/ using normal File() APIs.
 * - MANAGE_EXTERNAL_STORAGE grants full access to shared storage INCLUDING
 *   Android/data/<any_package>/ (except Android/obb on Android 13+).
 * - This lets the launcher apply mod patches (platform.ini, etc.) directly
 *   without Shizuku or root.
 *
 * User flow:
 * 1. User taps "Izinkan Akses File" in launcher
 * 2. System Settings opens to "All files access" page for DLavie Launcher
 * 3. User toggles ON
 * 4. Returns to launcher — now can read/write Android/data/com.ea.gp.fifaworld/
 *
 * Fallback for OBB on Android 13+ or denied permission: Shizuku/root (existing code path).
 */
object StorageAccess {

    const val GAME_PACKAGE = "com.ea.gp.fifaworld"

    /** True if user has granted MANAGE_EXTERNAL_STORAGE. */
    fun isGranted(): Boolean = try {
        Environment.isExternalStorageManager()
    } catch (_: Throwable) {
        false
    }

    /**
     * Open system Settings page to grant All Files Access for this app.
     * Falls back to generic storage settings if the dedicated intent isn't available.
     */
    fun request(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            // Fallback: open generic "All files access" settings page
            val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (_: Throwable) {
                // Last resort: open app details settings
                val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(details)
            }
        }
    }

    // ─── File operations on game data folder ──────────────────────────────────

    /** Resolve the absolute path inside the game's Android/data folder. */
    fun gameDataFile(relativePath: String): File {
        val cleanRel = relativePath.trimStart('/')
        return File("/sdcard/Android/data/$GAME_PACKAGE/$cleanRel")
    }

    /** True if the game's Android/data folder exists at all. */
    fun gameDataExists(): Boolean = File("/sdcard/Android/data/$GAME_PACKAGE").exists()

    /** Write bytes to a file inside the game's Android/data folder. Returns true on success. */
    fun writeBytes(relativePath: String, content: ByteArray): Boolean {
        if (!isGranted()) return false
        return try {
            val target = gameDataFile(relativePath)
            target.parentFile?.mkdirs()
            target.writeBytes(content)
            true
        } catch (_: Throwable) { false }
    }

    /** Copy a source file into the game's Android/data folder. Returns true on success. */
    fun copyInto(srcFile: File, relativePath: String): Boolean {
        if (!isGranted()) return false
        return try {
            val target = gameDataFile(relativePath)
            target.parentFile?.mkdirs()
            srcFile.copyTo(target, overwrite = true)
            true
        } catch (_: Throwable) { false }
    }

    /**
     * Backup an existing game data file (if it exists) to the backup path.
     * Returns true if backup succeeded OR if there was nothing to back up.
     */
    fun backupIfExists(relativePath: String, backupFile: File): Boolean {
        if (!isGranted()) return false
        val src = gameDataFile(relativePath)
        if (!src.exists()) return true  // Nothing to back up — not an error
        return try {
            backupFile.parentFile?.mkdirs()
            src.copyTo(backupFile, overwrite = true)
            true
        } catch (_: Throwable) { false }
    }

    /** Write the data-ready marker file. Returns true on success. */
    fun writeMarker(content: String): Boolean {
        if (!isGranted()) return false
        return try {
            val marker = gameDataFile(".dlavie26_data_installed")
            marker.parentFile?.mkdirs()
            marker.writeText(content)
            true
        } catch (_: Throwable) { false }
    }

    /** Delete a file inside the game data folder. Returns true on success. */
    fun delete(relativePath: String): Boolean {
        if (!isGranted()) return false
        return try {
            gameDataFile(relativePath).delete()
        } catch (_: Throwable) { false }
    }
}
