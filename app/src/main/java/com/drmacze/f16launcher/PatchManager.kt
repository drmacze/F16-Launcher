package com.drmacze.f16launcher

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * DLavie Patch Manager — GitHub Tree Sync System
 *
 * Sistem patch modular tanpa zip. Setiap patch = folder di GitHub repo
 * drmacze/DLavie-Patches. Launcher download file langsung dari raw URL,
 * backup file lama, apply file baru.
 *
 * Flow:
 * 1. fetchManifest() → baca manifest.json dari GitHub raw URL
 * 2. checkForUpdates() → bandingkan latest_version dengan installed_version
 * 3. applyPatch() → download semua file di patches/{version}/files/
 *                  backup file lama → copy file baru ke game folder
 * 4. rollbackTo() → restore dari backup folder
 *
 * Game folder: Android/data/com.ea.gp.fifaworld/files/
 * Backup folder: app's internal storage/dlavie_backup/{version}/
 */
object PatchManager {

    private const val TAG = "DLaviePatch"
    private const val MANIFEST_URL = "https://raw.githubusercontent.com/drmacze/DLavie-Patches/main/manifest.json"
    private const val RAW_BASE = "https://raw.githubusercontent.com/drmacze/DLavie-Patches/main/"
    private const val GAME_FILES_PATH = "Android/data/com.ea.gp.fifaworld/files/"
    private const val PREFS_NAME = "dlavie_patch_prefs"

    /**
     * Fetch manifest.json from GitHub.
     */
    suspend fun fetchManifest(): JSONObject = withContext(Dispatchers.IO) {
        val conn = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 30000
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code fetching manifest")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Get currently installed patch version from SharedPreferences.
     */
    fun getInstalledVersion(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("installed_version", "0.0.0") ?: "0.0.0"
    }

    /**
     * Save installed patch version to SharedPreferences.
     */
    fun setInstalledVersion(context: Context, version: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("installed_version", version)
            .apply()
        Log.i(TAG, "Installed version set to: $version")
    }

    /**
     * Check if update is available.
     * Returns PatchUpdateInfo with latest version + list of patches to apply.
     * Returns null if already up-to-date.
     */
    suspend fun checkForUpdates(context: Context): PatchUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val manifest = fetchManifest()
            val latest = manifest.optString("latest_version", "0.0.0")
            val installed = getInstalledVersion(context)

            if (latest == installed || compareVersions(latest, installed) <= 0) {
                Log.i(TAG, "Up-to-date: installed=$installed, latest=$latest")
                return@withContext null
            }

            val patchesArr = manifest.optJSONArray("patches") ?: return@withContext null
            val patchesToApply = mutableListOf<PatchInfo>()
            for (i in 0 until patchesArr.length()) {
                val p = patchesArr.optJSONObject(i) ?: continue
                val ver = p.optString("version", "")
                val status = p.optString("status", "stable")
                if (compareVersions(ver, installed) <= 0) continue
                if (status == "beta" && !isBetaOptIn(context)) continue
                patchesToApply.add(
                    PatchInfo(
                        version = ver,
                        title = p.optString("title", ""),
                        description = p.optString("description", ""),
                        category = p.optString("category", ""),
                        githubPath = p.optString("github_path", ""),
                        baseVersion = p.optString("base_version", "").ifEmpty { null },
                        status = status,
                        releasedAt = p.optString("released_at", ""),
                        changelog = p.optString("changelog", "")
                    )
                )
            }

            if (patchesToApply.isEmpty()) return@withContext null

            PatchUpdateInfo(
                currentVersion = installed,
                latestVersion = latest,
                patches = patchesToApply,
                minLauncherVersion = manifest.optInt("min_launcher_version", 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            throw e
        }
    }

    /**
     * Apply a single patch — backup + download + copy files.
     */
    suspend fun applyPatch(
        context: Context,
        patch: PatchInfo,
        progress: (current: Int, total: Int, fileName: String) -> Unit = { _, _, _ -> }
    ): PatchApplyResult = withContext(Dispatchers.IO) {
        val gameFolder = File(android.os.Environment.getExternalStorageDirectory(), GAME_FILES_PATH)
        val backupFolder = File(context.filesDir, "dlavie_backup/${patch.version}")

        Log.i(TAG, "Applying patch ${patch.version} → gameFolder=${gameFolder.absolutePath}")

        if (!gameFolder.exists()) gameFolder.mkdirs()
        backupFolder.mkdirs()

        // Fetch patch.json to get file list
        val patchJsonUrl = "${RAW_BASE}${patch.githubPath}/patch.json"
        val patchJson = try {
            val conn = (URL(patchJsonUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            JSONObject(text)
        } catch (e: Exception) {
            return@withContext PatchApplyResult(
                success = false,
                error = "Failed to fetch patch.json: ${e.message}"
            )
        }

        val filesArr = patchJson.optJSONArray("files") ?: org.json.JSONArray()
        val totalFiles = filesArr.length()
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()

        for (i in 0 until totalFiles) {
            val fileEntry = filesArr.optJSONObject(i) ?: continue
            val filePath = fileEntry.optString("path", "")
            val expectedSha256 = fileEntry.optString("sha256", "")
            if (filePath.isEmpty()) continue

            progress(i + 1, totalFiles, filePath)

            try {
                val fileUrl = "${RAW_BASE}${patch.githubPath}/files/${filePath}"
                Log.d(TAG, "Downloading: $fileUrl")

                val conn = (URL(fileUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 60000
                }
                if (conn.responseCode !in 200..299) {
                    throw IllegalStateException("HTTP ${conn.responseCode}")
                }
                val downloadedBytes = conn.inputStream.use { it.readBytes() }
                conn.disconnect()

                // Verify SHA256 if provided
                if (expectedSha256.isNotEmpty()) {
                    val actualSha = sha256(downloadedBytes)
                    if (actualSha != expectedSha256) {
                        throw IllegalStateException("SHA256 mismatch: expected=$expectedSha256, got=$actualSha")
                    }
                }

                // Backup existing file (if exists)
                val targetFile = File(gameFolder, filePath)
                if (targetFile.exists()) {
                    val backupFile = File(backupFolder, filePath)
                    backupFile.parentFile?.mkdirs()
                    targetFile.copyTo(backupFile, overwrite = true)
                    Log.d(TAG, "Backed up: ${targetFile.name} → ${backupFile.absolutePath}")
                }

                // Copy new file to game folder
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { it.write(downloadedBytes) }
                Log.d(TAG, "Applied: ${targetFile.absolutePath} (${downloadedBytes.size} bytes)")

                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply file: $filePath", e)
                failureCount++
                errors.add("$filePath: ${e.message}")
            }
        }

        if (successCount > 0) {
            setInstalledVersion(context, patch.version)
        }

        PatchApplyResult(
            success = failureCount == 0,
            successCount = successCount,
            failureCount = failureCount,
            errors = errors
        )
    }

    /**
     * Rollback to a previously applied patch version.
     */
    suspend fun rollbackTo(
        context: Context,
        targetVersion: String,
        progress: (current: Int, total: Int, fileName: String) -> Unit = { _, _, _ -> }
    ): PatchApplyResult = withContext(Dispatchers.IO) {
        val backupFolder = File(context.filesDir, "dlavie_backup/$targetVersion")
        val gameFolder = File(android.os.Environment.getExternalStorageDirectory(), GAME_FILES_PATH)

        if (!backupFolder.exists()) {
            return@withContext PatchApplyResult(
                success = false,
                error = "Backup folder not found: $targetVersion"
            )
        }

        val backupFiles = backupFolder.walkTopDown().filter { it.isFile }.toList()
        val totalFiles = backupFiles.size
        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()

        backupFiles.forEachIndexed { index, backupFile ->
            val relativePath = backupFile.absolutePath.removePrefix(backupFolder.absolutePath + "/")
            progress(index + 1, totalFiles, relativePath)

            try {
                val targetFile = File(gameFolder, relativePath)
                targetFile.parentFile?.mkdirs()
                backupFile.copyTo(targetFile, overwrite = true)
                successCount++
                Log.d(TAG, "Restored: $relativePath")
            } catch (e: Exception) {
                failureCount++
                errors.add("$relativePath: ${e.message}")
            }
        }

        if (successCount > 0) {
            setInstalledVersion(context, targetVersion)
        }

        PatchApplyResult(
            success = failureCount == 0,
            successCount = successCount,
            failureCount = failureCount,
            errors = errors
        )
    }

    /**
     * Get list of backup versions available for rollback.
     */
    fun getAvailableBackups(context: Context): List<String> {
        val backupRoot = File(context.filesDir, "dlavie_backup")
        if (!backupRoot.exists()) return emptyList()
        return backupRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sortedByDescending { parseVersion(it).joinToString(".") }
            ?: emptyList()
    }

    private fun isBetaOptIn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("beta_opt_in", false)
    }

    fun setBetaOptIn(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("beta_opt_in", enabled).apply()
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    private fun parseVersion(v: String): List<Int> {
        return v.split(".").map { it.toIntOrNull() ?: 0 }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}

// ─── Data Classes ──────────────────────────────────────────────────────────

data class PatchUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val patches: List<PatchInfo>,
    val minLauncherVersion: Int
)

data class PatchInfo(
    val version: String,
    val title: String,
    val description: String,
    val category: String,
    val githubPath: String,
    val baseVersion: String?,
    val status: String,
    val releasedAt: String,
    val changelog: String
)

data class PatchApplyResult(
    val success: Boolean,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val errors: List<String> = emptyList(),
    val error: String? = null
)
