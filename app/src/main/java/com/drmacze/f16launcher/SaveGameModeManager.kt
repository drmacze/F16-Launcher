package com.drmacze.f16launcher

import android.content.Context
import android.util.Log
import java.io.File

/**
 * v7.9.37: SaveGameModeManager — multi-mode save game system.
 *
 * Game modes (dari analisis UX folder F16 repo):
 * - LIGA (Career/League) — folder GameModes/Liga/ (Cores, Newgame, PremierLeague, Restart, Squad)
 * - TOURNAMENT — file Gameplay_Tournament.nav, InGameFlow_Tournament.nav
 * - QUICK_MATCH — melalui MainMenu/MenuPlay/
 *
 * fifa_ng_db.db (3.58MB) menyimpan SEMUA progress game dalam satu file.
 * Save.lua berisi data match, goals, team list, sheets, dates.
 *
 * Struktur backup:
 * /sdcard/F16Launcher/saves/
 * ├── liga/
 * │   ├── slot_1/ (fifa_ng_db.db + meta + files/)
 * │   ├── slot_2/
 * │   └── slot_3/
 * ├── tournament/
 * │   ├── slot_1/
 * │   └── slot_2/
 * └── quick_match/
 *     ├── slot_1/
 *     └── slot_2/
 *
 * Max 3 slots per mode = 9 total save slots.
 */
object SaveGameModeManager {

    private const val TAG = "SaveGameMode"
    private const val GAME_DATA_PATH = "/sdcard/Android/data/com.ea.gp.fifaworld/"
    private const val SAVE_BASE_PATH = "/sdcard/F16Launcher/saves/"
    private const val MAX_SLOTS = 3

    enum class GameMode(val label: String, val desc: String) {
        LIGA("Liga / Career", "Mode liga musim panjang"),
        TOURNAMENT("Tournament", "Mode turnamen elimination"),
        QUICK_MATCH("Quick Match", "Pertandingan cepat")
    }

    data class SaveSlot(
        val slotNumber: Int,
        val mode: GameMode,
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

    // Files yang di-backup (verified dari user screenshot)
    private val SAVE_FILES = listOf(
        "data/db/fifa_ng_db.db",
        "data/db/fifa_ng_db-meta.xml",
        "eng_us.db",
        "replay0.bin",
        "cl.ini",
        "player.ini",
        "options.viv"
    )

    private val SAVE_FOLDERS = listOf("files")

    /**
     * Backup save game ke mode + slot tertentu.
     */
    fun backupSave(context: Context, mode: GameMode, slotNumber: Int, label: String): SaveResult {
        try {
            if (!StorageAccess.isGranted()) {
                return SaveResult(false, "Storage permission tidak diberikan.")
            }

            val gameDir = File(GAME_DATA_PATH)
            if (!gameDir.exists()) {
                return SaveResult(false, "Game data tidak ditemukan.")
            }

            if (GameUtils.isGameRunning(context)) {
                return SaveResult(false, "Tutup game FIFA 16 terlebih dahulu.")
            }

            val saveDir = File(SAVE_BASE_PATH + mode.name.lowercase() + "/slot_$slotNumber")
            if (saveDir.exists()) saveDir.deleteRecursively()
            saveDir.mkdirs()

            var totalFiles = 0
            var totalSize = 0L

            for (relPath in SAVE_FILES) {
                val srcFile = File(gameDir, relPath)
                if (srcFile.exists()) {
                    val destFile = File(saveDir, relPath)
                    destFile.parentFile?.mkdirs()
                    srcFile.copyTo(destFile, overwrite = true)
                    totalFiles++
                    totalSize += srcFile.length()
                }
            }

            for (folder in SAVE_FOLDERS) {
                val srcFolder = File(gameDir, folder)
                if (srcFolder.exists() && srcFolder.isDirectory) {
                    val destFolder = File(saveDir, folder)
                    srcFolder.copyRecursively(destFolder, overwrite = true)
                    totalFiles += srcFolder.walkTopDown().count { it.isFile }
                    totalSize += srcFolder.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                }
            }

            if (totalFiles == 0) {
                saveDir.deleteRecursively()
                return SaveResult(false, "Tidak ada save file ditemukan. Main game dulu.")
            }

            // Write metadata
            val meta = File(saveDir, ".save_meta")
            meta.writeText(buildString {
                appendLine("mode=${mode.name}")
                appendLine("slot=$slotNumber")
                appendLine("label=${label.ifBlank { "${mode.label} $slotNumber" }}")
                appendLine("timestamp=${System.currentTimeMillis()}")
                appendLine("files=$totalFiles")
                appendLine("size=$totalSize")
                appendLine("date=${java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())}")
            })

            return SaveResult(true, "Save ${mode.label} berhasil! Slot $slotNumber — $totalFiles file (${formatSize(totalSize)})", slotNumber)

        } catch (e: Exception) {
            Log.e(TAG, "backupSave failed", e)
            return SaveResult(false, "Error: ${e.message}")
        }
    }

    /**
     * Restore save game dari mode + slot.
     */
    fun restoreSave(context: Context, mode: GameMode, slotNumber: Int): SaveResult {
        try {
            if (!StorageAccess.isGranted()) return SaveResult(false, "Storage permission tidak diberikan.")
            val saveDir = File(SAVE_BASE_PATH + mode.name.lowercase() + "/slot_$slotNumber")
            if (!saveDir.exists()) return SaveResult(false, "Slot $slotNumber ${mode.label} kosong.")
            if (GameUtils.isGameRunning(context)) return SaveResult(false, "Tutup game FIFA 16 terlebih dahulu.")

            val gameDir = File(GAME_DATA_PATH)
            if (!gameDir.exists()) return SaveResult(false, "Game data tidak ditemukan.")

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

            return SaveResult(true, "Restore ${mode.label} berhasil! Slot $slotNumber — $totalFiles file.", slotNumber)

        } catch (e: Exception) {
            Log.e(TAG, "restoreSave failed", e)
            return SaveResult(false, "Error: ${e.message}")
        }
    }

    fun deleteSlot(mode: GameMode, slotNumber: Int): SaveResult {
        val saveDir = File(SAVE_BASE_PATH + mode.name.lowercase() + "/slot_$slotNumber")
        if (!saveDir.exists()) return SaveResult(false, "Slot kosong.")
        return if (saveDir.deleteRecursively()) SaveResult(true, "Slot $slotNumber ${mode.label} dihapus.", slotNumber)
        else SaveResult(false, "Gagal hapus slot.")
    }

    fun listSlots(mode: GameMode): List<SaveSlot> {
        val result = mutableListOf<SaveSlot>()
        val modeDir = File(SAVE_BASE_PATH + mode.name.lowercase())
        for (i in 1..MAX_SLOTS) {
            val slotDir = File(modeDir, "slot_$i")
            if (slotDir.exists()) {
                val meta = File(slotDir, ".save_meta")
                var label = "${mode.label} $i"
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
                result.add(SaveSlot(i, mode, label, timestamp, sizeBytes, fileCount, true))
            } else {
                result.add(SaveSlot(i, mode, "Slot $i (kosong)", 0, 0, 0, false))
            }
        }
        return result
    }

    fun findEmptySlot(mode: GameMode): Int {
        val modeDir = File(SAVE_BASE_PATH + mode.name.lowercase())
        for (i in 1..MAX_SLOTS) {
            if (!File(modeDir, "slot_$i").exists()) return i
        }
        return -1
    }

    fun getMaxSlots() = MAX_SLOTS

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / 1024 / 1024} MB"
        }
    }
}
