package com.drmacze.f16launcher

import android.content.Context
import android.util.Log
import java.io.File

/**
 * v7.9.35: SaveGameManager — backup & restore FIFA 16 save data.
 *
 * VERIFIED structure (dari user screenshot):
 * /sdcard/Android/data/com.ea.gp.fifaworld/
 * ├── files/
 * │   ├── ShaderCache/
 * │   ├── Settings...Personal Settings 1 (470KB)
 * │   └── usagesharing.dat (1B)
 * ├── data/db/
 * │   ├── fifa_ng_db.db (3.58MB) ← SAVE UTAMA
 * │   └── fifa_ng_db-meta.xml (448KB)
 * ├── eng_us.db, ai.ini, cl.ini, dll
 * └── replay0.bin
 *
 * Backup: /sdcard/F16Launcher/saves/slot_N/
 * Max 5 slots. Uses MANAGE_EXTERNAL_STORAGE (no root).
 *
 * Save types:
 * - fifa_ng_db.db berisi SEMUA save data (career, tournament, settings, progress)
 * - Backup file ini = backup seluruh progress game
 * - Restore = timpa file ini = kembalikan ke save sebelumnya
 */
object SaveGameManager {

    private const val TAG = "SaveGame"
    private const val GAME_DATA_PATH = "/sdcard/Android/data/com.ea.gp.fifaworld/"
    private const val SAVE_BACKUP_PATH = "/sdcard/F16Launcher/saves/"
    private const val MAX_SLOTS = 5

    private val SAVE_FILES = listOf(
        "data/db/fifa_ng_db.db",
        "data/db/fifa_ng_db-meta.xml",
        "eng_us.db",
        "replay0.bin",
        "cl.ini",
        "player.ini",
        "options.viv"
    )

    private val SAVE_FOLDERS = listOf(
        "files"
    )

    data class SaveSlot(
        val slotNumber: Int,
        val label: String,
        val timestamp: Long,
        val sizeBytes: Long,
        val fileCount: Int,
        val exists: Boolean
    )

    data class SaveResult(
        val success: Boolean,
        val message: String,
        val slotNumber: Int = -1
    )

    fun backupSave(context: Context, slotNumber: Int = 0, label: String = ""): SaveResult {
        try {
            if (!StorageAccess.isGranted()) {
                return SaveResult(false, "Storage permission tidak diberikan.")
            }

            val gameDir = File(GAME_DATA_PATH)
            if (!gameDir.exists()) {
                return SaveResult(false, "Game data tidak ditemukan. Install FIFA 16 dulu.")
            }

            if (GameUtils.isGameRunning(context)) {
                return SaveResult(false, "Tutup game FIFA 16 terlebih dahulu.")
            }

            val actualSlot = if (slotNumber == 0) findEmptySlot() else slotNumber
            if (actualSlot == -1) {
                return SaveResult(false, "Semua slot save sudah penuh (max $MAX_SLOTS).")
            }

            val saveDir = File(SAVE_BACKUP_PATH, "slot_$actualSlot")
            if (saveDir.exists()) saveDir.deleteRecursively()
            saveDir.mkdirs()

            var totalFiles = 0
            var totalSize = 0L

            // Backup specific files
            for (relPath in SAVE_FILES) {
                val srcFile = File(gameDir, relPath)
                if (srcFile.exists()) {
                    val destFile = File(saveDir, relPath)
                    destFile.parentFile?.mkdirs()
                    srcFile.copyTo(destFile, overwrite = true)
                    totalFiles++
                    totalSize += srcFile.length()
                    Log.i(TAG, "Backed up: $relPath (${srcFile.length()} bytes)")
                }
            }

            // Backup folders
            for (folder in SAVE_FOLDERS) {
                val srcFolder = File(gameDir, folder)
                if (srcFolder.exists() && srcFolder.isDirectory) {
                    val destFolder = File(saveDir, folder)
                    srcFolder.copyRecursively(destFolder, overwrite = true)
                    totalFiles += srcFolder.walkTopDown().count { it.isFile }
                    totalSize += srcFolder.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                    Log.i(TAG, "Backed up folder: $folder")
                }
            }

            // Fallback: scan entire folder if 0 files
            if (totalFiles == 0) {
                Log.w(TAG, "No specific files found. Scanning entire game data...")
                gameDir.walkTopDown().forEach { file ->
                    if (file.isFile && !file.path.contains("/.dlavie") && !file.path.contains("/ShaderCache")) {
                        val relPath = file.relativeTo(gameDir).path
                        val destFile = File(saveDir, relPath)
                        destFile.parentFile?.mkdirs()
                        try {
                            file.copyTo(destFile, overwrite = true)
                            totalFiles++
                            totalSize += file.length()
                        } catch (e: Exception) {
                            Log.w(TAG, "Skip: $relPath")
                        }
                    }
                }
            }

            if (totalFiles == 0) {
                saveDir.deleteRecursively()
                val gameDirFiles = gameDir.listFiles()?.size ?: 0
                return SaveResult(false, "Tidak ada file ditemukan. Folder: $GAME_DATA_PATH ($gameDirFiles items).")
            }

            // Write metadata
            val meta = File(saveDir, ".save_meta")
            meta.writeText(buildString {
                appendLine("slot=$actualSlot")
                appendLine("label=${label.ifBlank { "Save $actualSlot" }}")
                appendLine("timestamp=${System.currentTimeMillis()}")
                appendLine("files=$totalFiles")
                appendLine("size=$totalSize")
                appendLine("date=${java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())}")
            })

            Log.i(TAG, "Backup complete: slot $actualSlot, $totalFiles files, $totalSize bytes")
            return SaveResult(true, "Save berhasil! Slot $actualSlot — $totalFiles file (${formatSize(totalSize)})", actualSlot)

        } catch (e: Exception) {
            Log.e(TAG, "backupSave failed", e)
            return SaveResult(false, "Error: ${e.message}")
        }
    }

    fun restoreSave(context: Context, slotNumber: Int): SaveResult {
        try {
            if (!StorageAccess.isGranted()) {
                return SaveResult(false, "Storage permission tidak diberikan.")
            }

            val saveDir = File(SAVE_BACKUP_PATH, "slot_$slotNumber")
            if (!saveDir.exists()) {
                return SaveResult(false, "Slot $slotNumber kosong.")
            }

            if (GameUtils.isGameRunning(context)) {
                return SaveResult(false, "Tutup game FIFA 16 terlebih dahulu.")
            }

            val gameDir = File(GAME_DATA_PATH)
            if (!gameDir.exists()) {
                return SaveResult(false, "Game data tidak ditemukan.")
            }

            var totalFiles = 0
            var totalSize = 0L

            for (relPath in SAVE_FILES) {
                val srcFile = File(saveDir, relPath)
                if (srcFile.exists()) {
                    val destFile = File(gameDir, relPath)
                    destFile.parentFile?.mkdirs()
                    srcFile.copyTo(destFile, overwrite = true)
                    totalFiles++
                    totalSize += srcFile.length()
                }
            }

            for (folder in SAVE_FOLDERS) {
                val srcFolder = File(saveDir, folder)
                if (srcFolder.exists() && srcFolder.isDirectory) {
                    val destFolder = File(gameDir, folder)
                    destFolder.mkdirs()
                    srcFolder.copyRecursively(destFolder, overwrite = true)
                    totalFiles += srcFolder.walkTopDown().count { it.isFile }
                    totalSize += srcFolder.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                }
            }

            // Fallback: restore all files from backup
            if (totalFiles == 0) {
                saveDir.walkTopDown().forEach { file ->
                    if (file.isFile && !file.name.startsWith(".save_meta")) {
                        val relPath = file.relativeTo(saveDir).path
                        val destFile = File(gameDir, relPath)
                        destFile.parentFile?.mkdirs()
                        try {
                            file.copyTo(destFile, overwrite = true)
                            totalFiles++
                            totalSize += file.length()
                        } catch (e: Exception) { }
                    }
                }
            }

            Log.i(TAG, "Restore complete: slot $slotNumber, $totalFiles files")
            return SaveResult(true, "Restore berhasil! Slot $slotNumber — $totalFiles file (${formatSize(totalSize)})", slotNumber)

        } catch (e: Exception) {
            Log.e(TAG, "restoreSave failed", e)
            return SaveResult(false, "Error: ${e.message}")
        }
    }

    fun deleteSlot(slotNumber: Int): SaveResult {
        val saveDir = File(SAVE_BACKUP_PATH, "slot_$slotNumber")
        if (!saveDir.exists()) return SaveResult(false, "Slot $slotNumber kosong.")
        return if (saveDir.deleteRecursively()) SaveResult(true, "Slot $slotNumber dihapus.", slotNumber)
        else SaveResult(false, "Gagal hapus slot $slotNumber.")
    }

    fun listSlots(): List<SaveSlot> {
        val result = mutableListOf<SaveSlot>()
        for (i in 1..MAX_SLOTS) {
            val slotDir = File(SAVE_BACKUP_PATH, "slot_$i")
            if (slotDir.exists()) {
                val meta = File(slotDir, ".save_meta")
                var label = "Save $i"
                var timestamp = slotDir.lastModified()
                var sizeBytes = 0L
                var fileCount = 0
                if (meta.exists()) {
                    meta.readText().lines().forEach { line ->
                        when {
                            line.startsWith("label=") -> label = line.removePrefix("label=")
                            line.startsWith("timestamp=") -> timestamp = line.removePrefix("timestamp=").toLongOrNull() ?: timestamp
                            line.startsWith("size=") -> sizeBytes = line.removePrefix("size=").toLongOrNull() ?: 0L
                            line.startsWith("files=") -> fileCount = line.removePrefix("files=").toIntOrNull() ?: 0
                        }
                    }
                }
                result.add(SaveSlot(i, label, timestamp, sizeBytes, fileCount, true))
            } else {
                result.add(SaveSlot(i, "Slot $i (kosong)", 0, 0, 0, false))
            }
        }
        return result
    }

    private fun findEmptySlot(): Int {
        for (i in 1..MAX_SLOTS) {
            if (!File(SAVE_BACKUP_PATH, "slot_$i").exists()) return i
        }
        return -1
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / 1024 / 1024} MB"
        }
    }
}
