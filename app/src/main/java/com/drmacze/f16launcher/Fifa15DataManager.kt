package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * FIFA 15 (DLavie 15) — Auto-Download & Install Manager
 *
 * Mirrors FIFA 16's `DevPatchEngine` flow but for FIFA 15 package
 * (com.ea.game.fifa14_row). Uses StorageAccess (MANAGE_EXTERNAL_STORAGE)
 * for direct file writes — NO Shizuku, NO root, NO ZArchiver needed.
 *
 * User flow:
 * 1. User taps "Download Data" in DLC page for FIFA 15
 * 2. System downloads DATA.zip (72.6 MB) + OBB.zip (1.1 GB) to app cache
 * 3. On success, extracts DATA.zip → /sdcard/Android/data/com.ea.game.fifa14_row/files/
 * 4. Extracts OBB.zip    → /sdcard/Android/obb/com.ea.game.fifa14_row/
 * 5. Writes marker `.dlavie15_data_installed` so we know it's verified
 *
 * All progress callbacks are suspend — caller decides which dispatcher to use.
 * Caller should wrap state updates in `withContext(Dispatchers.Main)` if updating
 * Compose state.
 *
 * NOTE: FIFA 15 is NOT a mod priority per user direction. This manager only
 * handles base data install (no patch system, no backups, no rollback).
 * The DLC UI for FIFA 15 is intentionally simpler than FIFA 16.
 */
class Fifa15DataManager(private val context: Context) {

    companion object {
        const val GAME_PACKAGE   = "com.ea.game.fifa14_row"
        const val MAIN_ACTIVITY  = "com.ea.game.fifa14.Fifa14Activity"

        // Target directories (created if missing)
        const val TARGET_DATA    = "/sdcard/Android/data/$GAME_PACKAGE/files/"
        const val TARGET_OBB     = "/sdcard/Android/obb/$GAME_PACKAGE/"
        const val MARKER_PATH    = "/sdcard/Android/data/$GAME_PACKAGE/.dlavie15_data_installed"

        // Marker content (used to verify install integrity)
        const val MARKER_CONTENT = "v15:dlavie15-data-installed:2026-07-05"

        // Approx sizes for UI display (actual sizes fetched from URL content-length)
        const val DATA_ZIP_SIZE_HINT  = 72_585_559L   // ~72.6 MB
        const val OBB_ZIP_SIZE_HINT   = 1_116_890_574L // ~1.1 GB

        // Total download size estimate (for progress bar)
        val TOTAL_SIZE_HINT = DATA_ZIP_SIZE_HINT + OBB_ZIP_SIZE_HINT

        // Cache dir name inside app's external files dir
        private const val CACHE_DIR_NAME = "fifa15-install"

        // URLs — DLavie proxy (Supabase Edge Function).
        // Protects source code privacy: user never sees GitHub repo URLs.
        // Proxy supports HTTP Range for resume + streams large files.
        const val PROXY_BASE = "https://lvmucsxbmadtsgrxuwmo.supabase.co/functions/v1/apk-proxy"
        const val APK_URL = "${PROXY_BASE}?f=fifa15-apk"
        const val DATA_URL = "${PROXY_BASE}?f=fifa15-data"
        const val OBB_URL = "${PROXY_BASE}?f=fifa15-obb"
    }

    private val cacheDir: File by lazy {
        File(context.getExternalFilesDir(null), CACHE_DIR_NAME).also { it.mkdirs() }
    }

    private val dataZipFile: File get() = File(cacheDir, "DATA.zip")
    private val obbZipFile: File get() = File(cacheDir, "OBB.zip")
    private val apkFile: File get() = File(cacheDir, "DLavie15.apk")

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Multi-source detection: true kalau FIFA 15 data sudah siap untuk dimainkan.
     *
     * 1. Marker file (`.dlavie15_data_installed`) — our explicit install marker
     * 2. OBB files in Android/obb/com.ea.game.fifa14_row/
     * 3. Game data folder exists & punya konten
     * 4. files/ subfolder punya konten (FIFA 15 sering download data ke sini)
     */
    fun isDataInstalled(): Boolean {
        // 1. Marker file
        if (File(MARKER_PATH).exists()) return true

        // 2. OBB files
        val obbDir = File(TARGET_OBB)
        if (obbDir.exists()) {
            val obbFiles = obbDir.listFiles { f -> f.isFile && f.name.endsWith(".obb", ignoreCase = true) }
            if (obbFiles != null && obbFiles.isNotEmpty()) return true
        }

        // 3. Game data folder exists & non-empty
        val gameDataDir = File("/sdcard/Android/data/$GAME_PACKAGE")
        if (gameDataDir.exists() && (gameDataDir.listFiles()?.isNotEmpty() == true)) return true

        // 4. files/ subfolder non-empty
        val filesDir = File(TARGET_DATA)
        if (filesDir.exists() && (filesDir.listFiles()?.isNotEmpty() == true)) return true

        return false
    }

    /** True kalau APK FIFA 15 sudah terinstall di device. */
    fun isApkInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(GAME_PACKAGE, 0)
        true
    } catch (_: Throwable) { false }

    /**
     * Full install: download + extract DATA + OBB.
     *
     * @param onProgress suspend callback (phase, currentBytes, totalBytes, message) → caller updates UI
     * @return Result<Unit> — Result.success(Unit) kalau sukses, Result.failure(Throwable) kalau gagal
     *
     * Phases:
     *   "download_data"  — downloading DATA.zip (0%–40%)
     *   "download_obb"   — downloading OBB.zip (40%–80%)
     *   "extract_data"   — extracting DATA.zip → Android/data/.../files/ (80%–95%)
     *   "extract_obb"    — extracting OBB.zip → Android/obb/.../ (95%–100%)
     *   "verify"         — writing marker & verifying (100%)
     */
    suspend fun downloadAndInstall(
        onProgress: suspend (phase: String, current: Long, total: Long, message: String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {

        try {
            // Pre-check: storage permission
            if (!StorageAccess.isGranted()) {
                return@withContext Result.failure(IllegalStateException(
                    "Izin 'Akses semua file' belum diberikan. Aktifkan di Settings → Aplikasi → DLavie Launcher → Akses semua file."
                ))
            }

            // ── Phase 1: Download DATA.zip ───────────────────────────────────
            onProgress("download_data", 0L, DATA_ZIP_SIZE_HINT, "Mengunduh DATA.zip (72.6 MB)...")
            val dataOk = downloadFile(
                url = DATA_URL,
                target = dataZipFile,
                expectedSize = DATA_ZIP_SIZE_HINT,
                onProgress = { cur, tot -> onProgress("download_data", cur, tot, "Mengunduh DATA.zip...") }
            )
            if (!dataOk) return@withContext Result.failure(
                IllegalStateException("Download DATA.zip gagal. Cek koneksi internet dan coba lagi.")
            )

            // ── Phase 2: Download OBB.zip ────────────────────────────────────
            onProgress("download_obb", 0L, OBB_ZIP_SIZE_HINT, "Mengunduh OBB.zip (1.1 GB)...")
            val obbOk = downloadFile(
                url = OBB_URL,
                target = obbZipFile,
                expectedSize = OBB_ZIP_SIZE_HINT,
                onProgress = { cur, tot -> onProgress("download_obb", cur, tot, "Mengunduh OBB.zip...") }
            )
            if (!obbOk) return@withContext Result.failure(
                IllegalStateException("Download OBB.zip gagal. Cek koneksi internet dan coba lagi.")
            )

            // ── Phase 3: Extract DATA.zip → Android/data/com.ea.game.fifa14_row/files/ ──
            onProgress("extract_data", 0L, dataZipFile.length(), "Mengekstrak DATA.zip...")
            val dataTargetDir = File(TARGET_DATA).also { it.mkdirs() }
            val extractDataResult = extractZip(
                zipFile = dataZipFile,
                targetDir = dataTargetDir,
                onProgress = { cur, tot -> onProgress("extract_data", cur, tot, "Mengekstrak DATA.zip...") }
            )
            if (!extractDataResult) return@withContext Result.failure(
                IllegalStateException("Gagal mengekstrak DATA.zip. File mungkin korup — hapus cache dan coba lagi.")
            )

            // ── Phase 4: Extract OBB.zip → Android/obb/com.ea.game.fifa14_row/ ──
            onProgress("extract_obb", 0L, obbZipFile.length(), "Mengekstrak OBB.zip...")
            val obbTargetDir = File(TARGET_OBB).also { it.mkdirs() }
            val extractObbResult = extractZip(
                zipFile = obbZipFile,
                targetDir = obbTargetDir,
                onProgress = { cur, tot -> onProgress("extract_obb", cur, tot, "Mengekstrak OBB.zip...") }
            )
            if (!extractObbResult) return@withContext Result.failure(
                IllegalStateException("Gagal mengekstrak OBB.zip. File mungkin korup — hapus cache dan coba lagi.")
            )

            // ── Phase 5: Write marker & verify ───────────────────────────────
            onProgress("verify", 1L, 1L, "Menulis marker verifikasi...")
            val markerFile = File(MARKER_PATH)
            try {
                markerFile.parentFile?.mkdirs()
                markerFile.writeText(MARKER_CONTENT)
            } catch (e: Throwable) {
                return@withContext Result.failure(IllegalStateException(
                    "Gagal menulis marker file: ${e.message}"
                ))
            }

            // Final verify
            if (!isDataInstalled()) {
                return@withContext Result.failure(IllegalStateException(
                    "Install selesai tapi data tidak terdeteksi. Coba restart launcher."
                ))
            }

            onProgress("verify", 1L, 1L, "FIFA 15 data berhasil dipasang!")
            Result.success(Unit)

        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Hapus cache (DATA.zip + OBB.zip + APK) untuk menghemat storage setelah install sukses.
     * Dipanggil otomatis atau manual dari DLC page.
     */
    fun clearCache(): Long {
        var freed = 0L
        if (dataZipFile.exists()) { freed += dataZipFile.length(); dataZipFile.delete() }
        if (obbZipFile.exists())  { freed += obbZipFile.length(); obbZipFile.delete() }
        if (apkFile.exists())     { freed += apkFile.length(); apkFile.delete() }
        return freed
    }

    /** Total ukuran cache yang masih tersimpan (untuk display). */
    fun cacheSize(): Long {
        var size = 0L
        if (dataZipFile.exists()) size += dataZipFile.length()
        if (obbZipFile.exists())  size += obbZipFile.length()
        if (apkFile.exists())     size += apkFile.length()
        return size
    }

    /**
     * Download APK FIFA 15 ke cache. Setelah selesai, panggil openApkInstaller().
     */
    suspend fun downloadApk(
        onProgress: suspend (current: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val ok = downloadFile(
                url = APK_URL,
                target = apkFile,
                expectedSize = 0L, // unknown
                onProgress = onProgress
            )
            if (ok) Result.success(apkFile) else Result.failure(
                IllegalStateException("Download APK FIFA 15 gagal.")
            )
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Buka installer APK FIFA 15 (untuk user yang belum install APK).
     * Menggunakan FileProvider agar kompatibel dengan Android 7+.
     */
    fun openApkInstaller(): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() < 1_000_000) {
                return Result.failure(IllegalStateException("APK belum diunduh. Panggil downloadApk() dulu."))
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /** Buka URL download di browser (fallback kalau auto-download gagal). */
    fun openApkInBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) { /* ignore */ }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Download file dengan resume support + size verify (kalau expected size known).
     * Returns true on success.
     */
    private suspend fun downloadFile(
        url: String,
        target: File,
        expectedSize: Long,
        onProgress: suspend (current: Long, total: Long) -> Unit
    ): Boolean {
        target.parentFile?.mkdirs()
        val partFile = File(target.parentFile, "${target.name}.part")

        // If target already exists with correct size, skip download
        if (target.exists() && target.length() > 0L) {
            if (expectedSize <= 0L || target.length() == expectedSize) {
                onProgress(target.length(), target.length())
                return true
            }
            target.delete()
        }

        // Resume from .part file if exists
        var resumeFromBytes = if (partFile.exists()) partFile.length() else 0L

        var attempt = 0
        while (attempt < 3) {
            attempt++
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 120_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "DLavie-Launcher/7.0 (Android)")
                    if (resumeFromBytes > 0L) setRequestProperty("Range", "bytes=$resumeFromBytes-")
                }

                // Handle HTTP errors / range-not-satisfiable
                if (conn.responseCode !in 200..299) {
                    // 416 Range Not Satisfiable — file might already be complete
                    if (conn.responseCode == 416 && resumeFromBytes > 0L && expectedSize > 0L && resumeFromBytes >= expectedSize) {
                        conn.disconnect()
                        partFile.renameTo(target)
                        onProgress(target.length(), target.length())
                        return true
                    }
                    // Server didn't honor Range — restart from scratch on next attempt
                    if (resumeFromBytes > 0L && conn.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                        conn.disconnect()
                        partFile.delete()
                        resumeFromBytes = 0L
                        continue  // restart while loop with fresh state
                    }
                    conn.disconnect()
                    return false
                }

                val total = when {
                    expectedSize > 0L -> expectedSize
                    resumeFromBytes > 0L && conn.contentLengthLong > 0L -> resumeFromBytes + conn.contentLengthLong
                    else -> conn.contentLengthLong
                }

                BufferedInputStream(conn.inputStream).use { input ->
                    FileOutputStream(partFile, resumeFromBytes > 0L).use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var copied = resumeFromBytes
                        while (true) {
                            val n = input.read(buffer)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                            copied += n
                            onProgress(copied, total)
                            // Yield every ~1MB to keep coroutine responsive
                            if (copied % (1024 * 1024) < 256 * 1024) {
                                yield()
                            }
                        }
                    }
                }
                conn.disconnect()

                // Verify size
                if (expectedSize > 0L && partFile.length() != expectedSize) {
                    partFile.delete()
                    return false
                }

                // Promote .part → final
                if (target.exists()) target.delete()
                if (!partFile.renameTo(target)) return false

                return true

            } catch (e: Throwable) {
                // Retry — keep .part file for resume
                resumeFromBytes = if (partFile.exists()) partFile.length() else 0L
                delay(2000L * attempt)
            }
        }
        return false
    }

    /**
     * Extract zip ke target dir dengan path traversal protection.
     * Returns true on success.
     */
    private suspend fun extractZip(
        zipFile: File,
        targetDir: File,
        onProgress: suspend (current: Long, total: Long) -> Unit
    ): Boolean {
        if (!zipFile.exists() || zipFile.length() <= 0L) return false
        targetDir.mkdirs()
        val root = targetDir.canonicalPath + File.separator
        val totalSize = zipFile.length()

        return try {
            ZipInputStream(FileInputStream(zipFile)).use { input ->
                val buffer = ByteArray(131_072) // 128 KB
                var bytesProcessed = 0L

                while (true) {
                    val entry: ZipEntry = input.nextEntry ?: break
                    val out = File(targetDir, safeZipPath(entry.name))

                    // Path traversal protection
                    if (!out.canonicalPath.startsWith(root)) {
                        throw IllegalStateException("Zip path tidak aman: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { output ->
                            while (true) {
                                val n = input.read(buffer)
                                if (n <= 0) break
                                output.write(buffer, 0, n)
                                bytesProcessed += n
                            }
                        }
                    }
                    input.closeEntry()

                    // Progress (rough estimate based on bytes processed vs zip size)
                    onProgress(bytesProcessed.coerceAtMost(totalSize), totalSize)

                    // Yield every ~10MB to keep UI responsive
                    if (bytesProcessed % (10 * 1024 * 1024) < 131_072) {
                        yield()
                    }
                }
            }
            true
        } catch (e: Throwable) {
            false
        }
    }

    private fun safeZipPath(path: String): String {
        val rel = path.replace('\\', '/').trimStart('/')
        if (rel == ".." || rel.startsWith("../") || rel.contains("/../")) {
            throw IllegalStateException("Zip path tidak aman: $path")
        }
        return rel
    }

    /** Format bytes ke human-readable string. */
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
}
