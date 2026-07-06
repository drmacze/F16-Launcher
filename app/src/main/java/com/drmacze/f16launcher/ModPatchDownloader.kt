package com.drmacze.f16launcher

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * v7.9.31: ModPatchDownloader — download & apply mod patch ZIP ke game data folder.
 *
 * Flow:
 * 1. downloadPatch() — download ZIP dari patch_url ke cache dir
 * 2. verifySha256() — verify hash jika ada
 * 3. extractToGameData() — extract ZIP ke /sdcard/Android/data/com.ea.gp.fifaworld/
 * 4. writeMarker() — tulis marker file dengan version patch
 *
 * Supports ALL file types di repo F16:
 * - data/sceneassets/faces/ (face mods)
 * - data/sceneassets/kit/ (kit mods)
 * - data/attribdb/ (database mods)
 * - ai.ini, cl.ini, dll (config mods)
 * - Semua file di ZIP akan di-extract preserving relative path
 *
 * Target: /sdcard/Android/data/com.ea.gp.fifaworld/
 * (dengan MANAGE_EXTERNAL_STORAGE — no root needed)
 */
object ModPatchDownloader {

    private const val TAG = "ModPatchDL"
    private const val GAME_DATA_PATH = "/sdcard/Android/data/com.ea.gp.fifaworld/"
    private const val MARKER_FILE = ".dlavie_patch_installed"
    private const val BACKUP_DIR = "/sdcard/F16Launcher/backups/"

    data class PatchResult(
        val success: Boolean,
        val message: String,
        val filesApplied: Int = 0,
        val filesFailed: Int = 0
    )

    /**
     * Download patch ZIP dari URL, verify, extract ke game data.
     * Progress callback: (0.0 - 1.0)
     */
    suspend fun applyPatch(
        context: Context,
        patchUrl: String,
        sha256: String? = null,
        versionName: String = "",
        onProgress: (Float) -> Unit = {}
    ): PatchResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 1. Check StorageAccess
            if (!StorageAccess.isGranted()) {
                return@withContext PatchResult(false, "Storage permission tidak diberikan. Aktifkan di Settings.")
            }

            // 2. Check game not running
            if (GameUtils.isGameRunning(context)) {
                return@withContext PatchResult(false, "Tutup game FIFA 16 terlebih dahulu sebelum apply mod.")
            }

            // 3. Download ZIP
            onProgress(0.05f)
            val cacheDir = File(context.cacheDir, "mod-patches").also { it.mkdirs() }
            val zipFile = File(cacheDir, "patch_${System.currentTimeMillis()}.zip")
            Log.i(TAG, "Downloading patch from: $patchUrl")

            val downloaded = downloadFile(patchUrl, zipFile) { progress ->
                onProgress(0.05f + progress * 0.45f)  // download = 5%-50%
            }
            if (!downloaded) {
                return@withContext PatchResult(false, "Gagal download patch. Cek koneksi internet.")
            }
            Log.i(TAG, "Downloaded: ${zipFile.length()} bytes")

            // 4. Verify SHA-256 jika ada
            if (!sha256.isNullOrBlank()) {
                onProgress(0.52f)
                val actualHash = calculateSha256(zipFile)
                if (!actualHash.equals(sha256, ignoreCase = true)) {
                    zipFile.delete()
                    return@withContext PatchResult(false, "SHA-256 mismatch! Patch corrupt atau diubah.")
                }
                Log.i(TAG, "SHA-256 verified OK")
            }

            // 5. Backup existing files (yang akan di-overwrite)
            onProgress(0.55f)
            val backupPath = File(BACKUP_DIR, "${versionName.ifBlank { "unknown" }}_${System.currentTimeMillis()}")
            backupPath.mkdirs()

            // 6. Extract ZIP ke game data folder
            onProgress(0.60f)
            val gameDataDir = File(GAME_DATA_PATH)
            if (!gameDataDir.exists()) {
                gameDataDir.mkdirs()
                Log.i(TAG, "Created game data dir: $gameDataDir")
            }

            var filesApplied = 0
            var filesFailed = 0

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name
                        // Strip common prefixes yang tidak perlu
                        val cleanPath = entryName
                            .removePrefix("Android/data/com.ea.gp.fifaworld/")
                            .removePrefix("files/")
                            .removePrefix("./")

                        val targetFile = File(gameDataDir, cleanPath)
                        val parentDir = targetFile.parentFile

                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs()
                        }

                        try {
                            // Backup existing file
                            if (targetFile.exists()) {
                                val backupFile = File(backupPath, cleanPath)
                                backupFile.parentFile?.mkdirs()
                                targetFile.copyTo(backupFile, overwrite = true)
                            }

                            // Write new file
                            FileOutputStream(targetFile).use { fos ->
                                val buf = ByteArray(8192)
                                var len: Int
                                while (zis.read(buf).also { len = it } > 0) {
                                    fos.write(buf, 0, len)
                                }
                            }
                            filesApplied++
                            Log.i(TAG, "Applied: $cleanPath (${targetFile.length()} bytes)")
                        } catch (e: Exception) {
                            filesFailed++
                            Log.e(TAG, "Failed to apply: $cleanPath", e)
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            // 7. Write marker file
            onProgress(0.95f)
            val marker = File(gameDataDir, MARKER_FILE)
            marker.writeText("${versionName.ifBlank { "unknown" }}\n${System.currentTimeMillis()}\n$filesApplied files")

            // 8. Cleanup
            zipFile.delete()
            onProgress(1.0f)

            Log.i(TAG, "Patch applied: $filesApplied files, $filesFailed failed")

            if (filesFailed > 0) {
                PatchResult(true, "Patch diterapkan dengan $filesFailed error. $filesApplied file berhasil.", filesApplied, filesFailed)
            } else {
                PatchResult(true, "Patch berhasil! $filesApplied file diterapkan ke game data.", filesApplied, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "applyPatch failed", e)
            PatchResult(false, "Error: ${e.message}")
        }
    }

    /** Download file dengan redirect follow + progress callback. */
    private fun downloadFile(url: String, outputFile: File, onProgress: (Float) -> Unit): Boolean {
        var currentUrl = url
        var redirectCount = 0

        while (true) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 30000
                readTimeout = 120000
                setRequestProperty("User-Agent", "DLavie-Launcher")
                connect()
            }

            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank() || redirectCount >= 5) return false
                currentUrl = location
                redirectCount++
                continue
            }

            if (code !in 200..299) {
                conn.disconnect()
                return false
            }

            val total = conn.contentLengthLong.toFloat().coerceAtLeast(1f)
            FileOutputStream(outputFile).use { fos ->
                val buf = ByteArray(16 * 1024)
                var n: Int
                var read = 0L
                conn.inputStream.use { inp ->
                    while (inp.read(buf).also { n = it } != -1) {
                        fos.write(buf, 0, n)
                        read += n
                        onProgress((read / total).coerceIn(0f, 0.99f))
                    }
                }
            }
            conn.disconnect()
            return outputFile.exists() && outputFile.length() > 0
        }
    }

    /** Calculate SHA-256 hash of file. */
    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(8192)
            var n: Int
            while (fis.read(buf).also { n = it } > 0) {
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Check if a patch version is already installed. */
    fun isPatchInstalled(versionName: String): Boolean {
        val marker = File(GAME_DATA_PATH, MARKER_FILE)
        if (!marker.exists()) return false
        return marker.readText().contains(versionName)
    }
}
