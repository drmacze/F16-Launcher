package com.drmacze.f16launcher

import android.content.Context
import android.util.Log
import java.io.File

/**
 * v7.9.32: SaveGameManager — backup & restore FIFA 16 career mode save data.
 *
 * Save files location (di game data folder external storage):
 *   /sdcard/Android/data/com.ea.gp.fifaworld/data/db/fifa_ng_db.db  (career database)
 *   /sdcard/Android/data/com.ea.gp.fifaworld/data/db/fifa_ng_db-meta.xml
 *   /sdcard/Android/data/com.ea.gp.fifaworld/eng_us.db  (language database)
 *   /sdcard/Android/data/com.ea.gp.fifaworld/replay0.bin  (replay data)
 *   /sdcard/Android/data/com.ea.gp.fifaworld/files/  (settings, personal data)
 *
 * Backup location:
 *   /sdcard/F16Launcher/saves/slot_<N>/
 *     ├── fifa_ng_db.db
 *     ├── fifa_ng_db-meta.xml
 *     ├── eng_us.db
 *     ├── replay0.bin
 *     ├── files/ (entire files folder if exists)
 *     └── .save_meta (metadata: timestamp, label, size)
 *
 * Max 5 save slots.
 * Uses MANAGE_EXTERNAL_STORAGE (no root needed).
 */
object SaveGameManager {

    private const val TAG = "SaveGame"
    private const val GAME_DATA_PATH = "/sdcard/Android/data/com.ea.gp.fifaworld/"
    private const val SAVE_BACKUP_PATH = "/sdcard/F16Launcher/saves/"
    private const val MAX_SLOTS = 5

    /**
     * Files/folders yang di-backup (relative to game data path).
     * Hanya file yang berisi save/user data — TIDAK mengubah file game (mod, OBB, dll).
     */
    private val SAVE_FILES = listOf(
        "data/db/fifa_ng_db.db",           // Career mode database (save utama)
        "data/db/fifa_ng_db-meta.xml",     // Database metadata
        "eng_us.db",                       // Language/progress database
        "replay0.bin",                     // Replay data
        "cl.ini",                          // User settings (controller, display)
        "player.ini",                      // Player settings
        "options.viv"                      // Game options
    )

    private val SAVE_FOLDERS = listOf(
        "files"  // Settings, personal data, user preferences
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

    /**
     * Backup save game ke slot tertentu.
     * Slot 0 = auto (pilih slot kosong pertama).
     */
    fun backupSave(context: Context, slotNumber: Int = 0, label: String = ""): SaveResult {
        try {
            if (!StorageAccess.isGranted()) {
                return SaveResult(false, "Storage permission tidak diberikan.")
            }

            val gameDir = File(GAME_DATA_PATH)
            if (!gameDir.exists()) {
                return SaveResult(false, "Game data tidak ditemukan. Install FIFA 16 dulu.")
            }

            // Check if game is running
            if (GameUtils.isGameRunning(context)) {
                return SaveResult(false, "Tutup game FIFA 16 terlebih dahulu.")
            }

            // Determine slot
            val actualSlot = if (slotNumber == 0) findEmptySlot() else slotNumber
            if (actualSlot == -1) {
                return SaveResult(false, "Semua slot save sudah penuh (max $MAX_SLOTS). Hapus salah satu.")
            }

            val saveDir = File(SAVE_BACKUP_PATH, "slot_$actualSlot")
            if (saveDir.exists()) {
                saveDir.deleteRecursively()
            }
            saveDir.mkdirs()

            var totalFiles = 0
            var totalSize = 0L

            // Backup individual files
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

            if (totalFiles == 0) {
                saveDir.deleteRecursively()
                return SaveResult(false, "Tidak ada save file ditemukan. Main game dulu untuk membuat save.")
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

    /**
     * Restore save game dari slot tertentu.
     */
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
                return SaveResult(false, "Game data tidak ditemukan. Install FIFA 16 dulu.")
            }

            var totalFiles = 0
            var totalSize = 0L

            // Restore individual files
            for (relPath in SAVE_FILES) {
                val srcFile = File(saveDir, relPath)
                if (srcFile.exists()) {
                    val destFile = File(gameDir, relPath)
                    destFile.parentFile?.mkdirs()
                    srcFile.copyTo(destFile, overwrite = true)
                    totalFiles++
                    totalSize += srcFile.length()
                    Log.i(TAG, "Restored: $relPath")
                }
            }

            // Restore folders
            for (folder in SAVE_FOLDERS) {
                val srcFolder = File(saveDir, folder)
                if (srcFolder.exists() && srcFolder.isDirectory) {
                    val destFolder = File(gameDir, folder)
                    destFolder.mkdirs()
                    srcFolder.copyRecursively(destFolder, overwrite = true)
                    totalFiles += srcFolder.walkTopDown().count { it.isFile }
                    totalSize += srcFolder.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                    Log.i(TAG, "Restored folder: $folder")
                }
            }

            Log.i(TAG, "Restore complete: slot $slotNumber, $totalFiles files")
            return SaveResult(true, "Restore berhasil! Slot $slotNumber — $totalFiles file (${formatSize(totalSize)})", slotNumber)

        } catch (e: Exception) {
            Log.e(TAG, "restoreSave failed", e)
            return SaveResult(false, "Error: ${e.message}")
        }
    }

    /**
     * Delete save slot.
     */
    fun deleteSlot(slotNumber: Int): SaveResult {
        val saveDir = File(SAVE_BACKUP_PATH, "slot_$slotNumber")
        if (!saveDir.exists()) {
            return SaveResult(false, "Slot $slotNumber kosong.")
        }
        return if (saveDir.deleteRecursively()) {
            SaveResult(true, "Slot $slotNumber dihapus.", slotNumber)
        } else {
            SaveResult(false, "Gagal hapus slot $slotNumber.")
        }
    }

    /**
     * List semua save slots.
     */
    fun listSlots(): List<SaveSlot> {
        val result = mutableListOf<SaveSlot>()
        val savesDir = File(SAVE_BACKUP_PATH)

        for (i in 1..MAX_SLOTS) {
            val slotDir = File(savesDir, "slot_$i")
            if (slotDir.exists()) {
                val meta = File(slotDir, ".save_meta")
                var label = "Save $i"
                var timestamp = slotDir.lastModified()
                var sizeBytes = 0L
                var fileCount = 0

                if (meta.exists()) {
                    val metaText = meta.readText()
                    metaText.lines().forEach { line ->
                        when {
                            line.startsWith("label=") -> label = line.removePrefix("label=")
                            line.startsWith("timestamp=") -> timestamp = line.removePrefix("timestamp=").toLongOrNull() ?: timestamp
                            line.startsWith("size=") -> sizeBytes = line.removePrefix("size=").toLongOrNull() ?: 0L
                            line.startsWith("files=") -> fileCount = line.removePrefix("files=").toIntOrNull() ?: 0
                        }
                    }
                } else {
                    sizeBytes = slotDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                    fileCount = slotDir.walkTopDown().count { it.isFile }
                }

                result.add(SaveSlot(i, label, timestamp, sizeBytes, fileCount, true))
            } else {
                result.add(SaveSlot(i, "Slot $i (kosong)", 0, 0, 0, false))
            }
        }
        return result
    }

    private fun findEmptySlot(): Int {
        val savesDir = File(SAVE_BACKUP_PATH)
        for (i in 1..MAX_SLOTS) {
            if (!File(savesDir, "slot_$i").exists()) return i
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
