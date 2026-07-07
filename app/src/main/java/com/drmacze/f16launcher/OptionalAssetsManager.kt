package com.drmacze.f16launcher

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * v7.9.43: OptionalAssetsManager — Manage download sceneassets (HD textures) yang OPTIONAL.
 *
 * Skema:
 * - Game data inti (dlavie26-data.zip) WAJIB di-install
 * - Sceneassets (dlavie26-sceneassets.zip) OPTIONAL — user pilih download atau tidak
 *
 * UI Flow:
 * 1. User install game data inti (required) → bisa main game
 * 2. User bisa tap "Download HD Sceneassets (Optional)" di GameHub
 * 3. Launcher download sceneassets.zip → verify SHA-256 → extract ke
 *    /sdcard/Android/data/com.ea.gp.fifaworld/files/data/sceneassets/
 * 4. Game langsung dapat HD textures tanpa restart
 *
 * State:
 * - NOT_DOWNLOADED: sceneassets belum di-download
 * - DOWNLOADING: sedang download
 * - EXTRACTING: sedang extract
 * - INSTALLED: sceneassets sudah terinstall dan aktif
 * - ERROR: gagal download/extract
 */
object OptionalAssetsManager {

    private const val TAG = "OptionalAssets"
    private const val GAME_FILES_PATH = "/sdcard/Android/data/com.ea.gp.fifaworld/files"
    private const val SCENEASSETS_PATH = "$GAME_FILES_PATH/data/sceneassets"
    private const val SCENEASSETS_MARKER = "$GAME_FILES_PATH/.sceneassets_installed"

    /**
     * Cek apakah sceneassets sudah terinstall.
     * Indikator: marker file ada ATAU folder sceneassets punya konten signifikan.
     */
    fun isInstalled(): Boolean {
        val marker = File(SCENEASSETS_MARKER)
        if (marker.exists()) return true

        // Fallback: cek folder sceneassets punya konten
        val sceneDir = File(SCENEASSETS_PATH)
        if (!sceneDir.exists() || !sceneDir.isDirectory) return false

        // Count files di sceneassets (sampai 100 files)
        var count = 0
        sceneDir.walkTopDown().forEach { f ->
            if (f.isFile) {
                count++
                if (count >= 50) return true  // 50+ files = installed
            }
        }
        return count >= 10
    }

    /**
     * Get info sceneassets dari manifest.
     */
    suspend fun fetchSceneassetsInfo(context: Context): SceneassetsInfo? =
        withContext(Dispatchers.IO) {
            try {
                val manifest = ManifestApi.fetchManifest(context, forceRefresh = false)
                val gameData = manifest?.let { m ->
                    // Fetch raw manifest JSON untuk akses optional_files
                    val rawUrl = ManifestApi.MANIFEST_URL
                    val conn = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10_000
                        readTimeout = 15_000
                        setRequestProperty("Cache-Control", "no-cache")
                    }
                    try {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        JSONObject(text).optJSONObject("game_data")
                    } finally {
                        conn.disconnect()
                    }
                } ?: return@withContext null

                val optionalFiles = gameData?.optJSONArray("optional_files") ?: return@withContext null
                for (i in 0 until optionalFiles.length()) {
                    val f = optionalFiles.getJSONObject(i)
                    if (f.optString("name") == "dlavie26-sceneassets.zip") {
                        return@withContext SceneassetsInfo(
                            url = f.optString("url"),
                            size = f.optLong("size", 0),
                            sha256 = f.optString("sha256"),
                            description = f.optString("description", "HD Sceneassets"),
                            estimatedSizeMb = f.optInt("estimated_size_mb", 0)
                        )
                    }
                }
                null
            } catch (e: Exception) {
                Log.w(TAG, "fetchSceneassetsInfo failed: ${e.message}")
                null
            }
        }

    /**
     * Download + extract sceneassets.
     *
     * @param context Application context
     * @param info SceneassetsInfo dari manifest
     * @param onProgress callback (0.0 - 1.0)
     * @return DownloadResult
     */
    suspend fun downloadAndInstall(
        context: Context,
        info: SceneassetsInfo,
        onProgress: (Float) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            // 1. Check URL valid (bukan placeholder)
            if (info.url.isBlank() || info.url == "PLACEHOLDER_UPLOAD_FIRST") {
                return@withContext DownloadResult(
                    success = false,
                    message = "Sceneassets belum di-upload developer. Coba lagi nanti."
                )
            }

            // 2. Check storage permission
            if (!StorageAccess.isGranted()) {
                return@withContext DownloadResult(
                    success = false,
                    message = "Storage permission tidak diberikan. Aktifkan di Settings."
                )
            }

            // 3. Check game data inti sudah terinstall
            val gameDataDir = File(GAME_FILES_PATH)
            if (!gameDataDir.exists()) {
                return@withContext DownloadResult(
                    success = false,
                    message = "Game data inti belum terinstall. Install FIFA 16 data dulu sebelum download sceneassets."
                )
            }

            // 4. Check storage space (butuh 2x size sceneassets untuk download + extract)
            val statFs = android.os.StatFs("/sdcard")
            val freeBytes = statFs.availableBytes
            val requiredBytes = info.size * 2
            if (freeBytes < requiredBytes) {
                return@withContext DownloadResult(
                    success = false,
                    message = "Storage tidak cukup. Butuh ${(requiredBytes / 1024 / 1024)} MB, tersedia ${(freeBytes / 1024 / 1024)} MB."
                )
            }

            // 5. Download ZIP dengan redirect follow
            onProgress(0.01f)
            val cacheDir = File(context.cacheDir, "sceneassets").also { it.mkdirs() }
            val zipFile = File(cacheDir, "sceneassets_${System.currentTimeMillis()}.zip")
            Log.i(TAG, "Downloading sceneassets from: ${info.url}")

            val downloaded = downloadFile(info.url, zipFile) { progress ->
                onProgress(0.01f + progress * 0.6f)  // download = 1%-60%
            }
            if (!downloaded) {
                return@withContext DownloadResult(
                    success = false,
                    message = "Gagal download sceneassets. Cek koneksi internet."
                )
            }
            Log.i(TAG, "Downloaded: ${zipFile.length()} bytes")

            // 6. Verify SHA-256 jika ada
            if (info.sha256.isNotBlank()) {
                onProgress(0.62f)
                val actualHash = calculateSha256(zipFile)
                if (!actualHash.equals(info.sha256, ignoreCase = true)) {
                    zipFile.delete()
                    return@withContext DownloadResult(
                        success = false,
                        message = "SHA-256 mismatch! File corrupt atau diubah."
                    )
                }
                Log.i(TAG, "SHA-256 verified OK")
            }

            // 7. Extract ke sceneassets folder
            onProgress(0.65f)
            val extractDir = File(SCENEASSETS_PATH)
            if (!extractDir.exists()) extractDir.mkdirs()

            Log.i(TAG, "Extracting to: $extractDir")
            var filesExtracted = 0
            var filesFailed = 0

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name
                        // Sanitize path — prevent path traversal
                        val cleanPath = entryName
                            .removePrefix("./")
                            .removePrefix("sceneassets/")
                            .removePrefix("data/sceneassets/")

                        val targetFile = File(extractDir, cleanPath)
                        val parentDir = targetFile.parentFile

                        if (parentDir != null && !parentDir.exists()) {
                            parentDir.mkdirs()
                        }

                        try {
                            FileOutputStream(targetFile).use { fos ->
                                val buf = ByteArray(16 * 1024)
                                var len: Int
                                while (zis.read(buf).also { len = it } > 0) {
                                    fos.write(buf, 0, len)
                                }
                            }
                            filesExtracted++
                            if (filesExtracted % 100 == 0) {
                                Log.d(TAG, "Extracted $filesExtracted files...")
                                onProgress(0.65f + (filesExtracted.toFloat() / 1000).coerceAtMost(0.34f))
                            }
                        } catch (e: Exception) {
                            filesFailed++
                            Log.w(TAG, "Failed extract: $cleanPath — ${e.message}")
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            // 8. Write marker file
            onProgress(0.99f)
            val marker = File(SCENEASSETS_MARKER)
            marker.writeText("installed_at=${System.currentTimeMillis()}\nfiles=$filesExtracted")

            // 9. Cleanup
            zipFile.delete()
            onProgress(1.0f)

            Log.i(TAG, "Sceneassets installed: $filesExtracted files, $filesFailed failed")

            DownloadResult(
                success = true,
                message = "Sceneassets HD berhasil di-install! $filesExtracted file diterapkan.",
                filesExtracted = filesExtracted,
                filesFailed = filesFailed
            )
        } catch (e: Exception) {
            Log.e(TAG, "downloadAndInstall failed", e)
            DownloadResult(
                success = false,
                message = "Error: ${e.message}"
            )
        }
    }

    /**
     * Hapus sceneassets dari device (uninstall).
     */
    fun uninstall(): Boolean {
        return try {
            val sceneDir = File(SCENEASSETS_PATH)
            val marker = File(SCENEASSETS_MARKER)

            var deleted = 0
            if (sceneDir.exists()) {
                sceneDir.walkTopDown().sortedDescending().forEach { f ->
                    if (f.isFile) {
                        f.delete()
                        deleted++
                    } else if (f.isDirectory && f != sceneDir) {
                        f.delete()
                    }
                }
            }
            marker.delete()

            Log.i(TAG, "Sceneassets uninstalled: $deleted files deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Uninstall failed", e)
            false
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun downloadFile(url: String, outputFile: File, onProgress: (Float) -> Unit): Boolean {
        var currentUrl = url
        var redirectCount = 0

        while (true) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 30_000
                readTimeout = 120_000
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

    // ─── Data classes ────────────────────────────────────────────────────────

    data class SceneassetsInfo(
        val url: String,
        val size: Long,
        val sha256: String,
        val description: String,
        val estimatedSizeMb: Int
    ) {
        val sizeFormatted: String
            get() = when {
                size >= 1024 * 1024 * 1024 -> String.format("%.1f GB", size / 1024.0 / 1024 / 1024)
                size >= 1024 * 1024 -> String.format("%.1f MB", size / 1024.0 / 1024)
                size >= 1024 -> String.format("%.1f KB", size / 1024.0)
                else -> "$size B"
            }
    }

    data class DownloadResult(
        val success: Boolean,
        val message: String,
        val filesExtracted: Int = 0,
        val filesFailed: Int = 0
    )
}
