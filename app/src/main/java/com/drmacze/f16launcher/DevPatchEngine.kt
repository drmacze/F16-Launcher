package com.drmacze.f16launcher

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Enumeration
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class DevPatchEngine(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onProgress: ((current: Int, total: Int, label: String) -> Unit)? = null
) {
    companion object {
        const val GAME_PACKAGE   = "com.ea.gp.fifaworld"
        const val DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json"
        const val MARKER_PATH    = "/sdcard/Android/data/$GAME_PACKAGE/.dlavie26_data_installed"
        private const val PREFS  = "f16_launcher"

        /** Ordered list of candidate game-data root paths. First writable one wins. */
        private val GAME_DATA_CANDIDATES = listOf(
            "/sdcard/Android/data/$GAME_PACKAGE/",
            "/storage/emulated/0/Android/data/$GAME_PACKAGE/",
            "/data/media/0/Android/data/$GAME_PACKAGE/"
        )
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ─── Public API ───────────────────────────────────────────────────────────

    fun localVersion(): Int = prefs.getInt("local_version_code", 1)
    fun latestBackupRoot(): String = prefs.getString("last_backup_root", "") ?: ""
    fun latestBackupTarget(): String = prefs.getString("last_backup_target", GAME_DATA_CANDIDATES[0]) ?: GAME_DATA_CANDIDATES[0]

    fun accessMode(): String = when {
        StorageAccess.isGranted() -> "All-Files Access aktif"
        shizukuOk() -> "Shizuku aktif"
        rootOk()    -> "Root aktif"
        else        -> "Belum aktif"
    }

    /** Detect the actual game data root (first path that exists on device). */
    fun detectGameDataPath(): String {
        for (path in GAME_DATA_CANDIDATES) {
            if (File(path).exists()) {
                log("Game data path: $path")
                return path
            }
        }
        log("Peringatan: Tidak ada path data game yang terdeteksi. Pakai default.")
        return GAME_DATA_CANDIDATES[0]
    }

    fun fetchManifest(): JSONObject {
        val url = prefs.getString("manifest_url", DEFAULT_MANIFEST) ?: DEFAULT_MANIFEST
        log("Fetch manifest: $url")
        return JSONObject(readUrl(url))
    }

    /**
     * Download and apply all pending patches from the manifest.
     * Patches are applied sequentially. Each patch:
     *  1. Downloads & verifies ZIP (or uses inline files)
     *  2. Backs up existing files before overwrite
     *  3. Copies patch files via Shizuku/root (cp -af, no duplicates)
     *  4. Writes marker file to MARKER_PATH
     *  5. Cleans up temp work directory
     */
    fun applyAvailableUpdates() {
        val manifest  = fetchManifest()
        val latest    = manifest.getInt("latestVersionCode")
        var local     = localVersion()

        if (local >= latest) {
            log("Sudah versi terbaru: v$local")
            return
        }

        log("Update tersedia: v$local → v$latest")
        val patches = manifest.getJSONArray("patches")
        var step = 0
        val total = countPatchSteps(patches, local, latest)

        while (local < latest) {
            val patch = findPatch(patches, local)
                ?: throw IllegalStateException("Patch dari v$local tidak ditemukan dalam manifest.")
            val to = patch.getInt("to")
            val name = patch.optString("name", "v$to")
            step++
            log("[$step/$total] Apply patch: $name")
            onProgress?.invoke(step, total, "Apply patch $name...")
            applyPatch(patch)
            local = to
            prefs.edit().putInt("local_version_code", local).apply()
            log("Patch selesai. Local version: v$local")
        }

        onProgress?.invoke(total, total, "Selesai")
        log("Semua update berhasil diapply.")
    }

    fun restoreLastBackup() {
        val backup = latestBackupRoot()
        val target = latestBackupTarget()
        if (backup.isBlank()) throw IllegalStateException("Belum ada backup yang tersimpan.")
        log("Restore backup: $backup → $target")
        val result = privileged("set -e; mkdir -p ${q(target)}; cp -af ${q("$backup/.")} ${q(target)}; echo 'Restore selesai'")
        log(result.out.trim().ifEmpty { "Restore command selesai." })
        if (result.code != 0) throw IllegalStateException("Restore gagal (exit ${result.code}). Output:\n${result.out.take(400)}")
    }

    fun resetLocalVersion(version: Int = 1) {
        prefs.edit().putInt("local_version_code", version).apply()
        log("Local version diset ke v$version")
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun countPatchSteps(patches: JSONArray, from: Int, to: Int): Int {
        var count = 0; var cur = from
        while (cur < to) {
            val p = findPatch(patches, cur) ?: break
            count++; cur = p.getInt("to")
        }
        return count
    }

    private fun findPatch(arr: JSONArray, from: Int): JSONObject? {
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            if (p.optInt("from") == from) return p
        }
        return null
    }

    private fun applyPatch(patch: JSONObject) {
        val to = patch.getInt("to")

        // Resolve target — prefer manifest value, then auto-detect
        val rawTarget = patch.optString("target", "").trim()
        val target = when {
            rawTarget.isNotBlank() -> if (rawTarget.endsWith("/")) rawTarget else "$rawTarget/"
            else -> detectGameDataPath()
        }

        val work      = File(context.getExternalFilesDir(null), "updates/v$to")
        val extracted = File(work, "extracted")
        deleteRec(work)
        extracted.mkdirs()

        // ── Collect files to patch ─────────────────────────────────────────
        val entries = mutableListOf<String>()

        if (patch.has("files")) {
            // Inline patch — file content embedded in JSON
            val files = patch.getJSONArray("files")
            val root  = extracted.canonicalPath + File.separator
            for (i in 0 until files.length()) {
                val fileObj = files.getJSONObject(i)
                val rel = safeRelativePath(fileObj.getString("path"))
                val out = File(extracted, rel)
                if (!out.canonicalPath.startsWith(root)) throw IllegalStateException("Path patch tidak aman: $rel")
                out.parentFile?.mkdirs()
                FileOutputStream(out).use { it.write(fileObj.optString("content", "").toByteArray(Charsets.UTF_8)) }
                entries.add(rel)
            }
            log("Inline patch siap: ${entries.size} file.")

        } else {
            // ZIP patch — download, verify, extract
            val zip = File(work, "patch.zip")
            log("Download patch ZIP dari ${patch.getString("url")}...")
            onProgress?.invoke(0, 100, "Download...")
            downloadFile(patch.getString("url"), zip)

            val expectedSha = patch.optString("sha256", "").trim()
            if (expectedSha.isNotBlank()) {
                log("Verifikasi SHA-256...")
                val actual = sha256(zip)
                if (!expectedSha.equals(actual, ignoreCase = true))
                    throw IllegalStateException("SHA-256 tidak cocok.\nExpected: $expectedSha\nActual  : $actual")
                log("SHA-256 OK.")
            }

            log("Ekstrak ZIP...")
            unzip(zip, extracted)
            val rawEntries = zipEntries(zip)

            // Strip common top-level directory prefix if all entries share one
            entries.addAll(stripCommonPrefix(rawEntries))
            if (rawEntries.size != entries.size || (rawEntries.isNotEmpty() && rawEntries[0] != entries[0])) {
                // Re-extract to strip the prefix
                deleteRec(extracted); extracted.mkdirs()
                unzipStripping(zip, extracted, rawEntries[0].substringBefore('/') + "/")
            }

            log("ZIP siap: ${entries.size} file.")
        }

        if (entries.isEmpty()) throw IllegalStateException("Patch kosong — tidak ada file yang akan diapply.")

        // ── Backup + apply ────────────────────────────────────────────────
        val backup = "/sdcard/F16Launcher/backups/v$to/${System.currentTimeMillis()}"
        log("Backup file lama ke $backup ...")
        onProgress?.invoke(50, 100, "Apply patch...")

        // Try direct file access first (MANAGE_EXTERNAL_STORAGE).
        // This avoids Shizuku/root for patching files in Android/data/com.ea.gp.fifaworld/.
        val directOk = try {
            applyPatchDirect(extracted, target, backup, entries)
        } catch (e: Throwable) {
            log("Direct file apply error: ${e.message}. Akan coba Shizuku/root...")
            false
        }
        if (!directOk) {
            log("Direct file access tidak tersedia, fallback ke Shizuku/root...")
            val result = privileged(buildCopyCommand(extracted.absolutePath, target, backup, entries))
            log(result.out.trim().ifEmpty { "Copy selesai (${entries.size} file)." })
            if (result.code != 0)
                throw IllegalStateException("Apply gagal (exit ${result.code}).\nOutput:\n${result.out.take(600)}")
        } else {
            log("Apply via All-Files Access berhasil (${entries.size} file).")
        }

        prefs.edit()
            .putString("last_backup_root", backup)
            .putString("last_backup_target", target)
            .apply()

        // ── Write marker file ─────────────────────────────────────────────
        writeMarker(to)

        // ── Cleanup temp work directory ────────────────────────────────────
        deleteRec(work)
        log("Temp directory dibersihkan.")
    }

    /**
     * Apply patch using direct File() API (no shell command).
     * Requires MANAGE_EXTERNAL_STORAGE permission (StorageAccess.isGranted()).
     * Returns true on success, false if permission not granted.
     */
    private fun applyPatchDirect(extracted: File, targetRoot: String, backupRoot: String, entries: List<String>): Boolean {
        if (!StorageAccess.isGranted()) return false

        val targetDir = File(targetRoot)
        val backupDir = File(backupRoot)
        targetDir.mkdirs()
        backupDir.mkdirs()

        for (rel in entries) {
            val src = File(extracted, rel)
            val dst = File(targetDir, rel)
            val bak = File(backupDir, rel)

            // Backup existing file (if present) before overwriting
            if (dst.exists()) {
                bak.parentFile?.mkdirs()
                try { dst.copyTo(bak, overwrite = true) }
                catch (e: Throwable) { log("Backup gagal untuk $rel: ${e.message}") }
            }

            // Overwrite with new file
            dst.parentFile?.mkdirs()
            try {
                src.copyTo(dst, overwrite = true)
            } catch (e: Throwable) {
                throw IllegalStateException("Gagal menulis ${dst.absolutePath}: ${e.message}")
            }
        }
        return true
    }

    /**
     * Write the data-ready marker file inside Android/data.
     * Tries direct file write first (MANAGE_EXTERNAL_STORAGE),
     * falls back to privileged shell (Shizuku/root).
     */
    private fun writeMarker(version: Int) {
        val markerDir     = MARKER_PATH.substringBeforeLast('/')
        val markerContent = "v26-patch-$version-${System.currentTimeMillis()}"

        // Try direct file write first
        if (StorageAccess.isGranted()) {
            val ok = StorageAccess.writeMarker(markerContent)
            if (ok) { log("Marker diperbarui (direct): $markerContent"); return }
            log("Direct marker write gagal, fallback ke Shizuku/root...")
        }

        // Fallback: privileged shell
        val writeResult = privileged("mkdir -p ${q(markerDir)}; printf '%s' ${q(markerContent)} > ${q(MARKER_PATH)}")
        if (writeResult.code == 0) log("Marker diperbarui: $markerContent")
        else log("Peringatan: marker gagal ditulis (exit ${writeResult.code}): ${writeResult.out.take(200)}")
    }

    /**
     * Build a shell command that, for each file:
     *  1. Creates parent directories
     *  2. Backs up the existing file (if any)
     *  3. Copies the new file with cp -af (atomic overwrite — no duplicates)
     */
    private fun buildCopyCommand(srcRoot: String, targetRoot: String, backupRoot: String, entries: List<String>): String {
        val root = if (targetRoot.endsWith("/")) targetRoot else "$targetRoot/"
        val sb   = StringBuilder("set -e; mkdir -p ${q(root)}; mkdir -p ${q(backupRoot)}; ")
        for (rel in entries) {
            val src    = "$srcRoot/$rel"
            val dst    = "$root$rel"
            val bak    = "$backupRoot/$rel"
            sb.append("mkdir -p ${q(parent(dst))}; ")
            // Backup existing file (if present) before overwriting
            sb.append("if [ -e ${q(dst)} ]; then mkdir -p ${q(parent(bak))}; cp -af ${q(dst)} ${q(bak)}; fi; ")
            // Overwrite with new file (cp -af: archive mode, force)
            sb.append("cp -af ${q(src)} ${q(dst)}; ")
        }
        sb.append("echo 'Applied ${entries.size} file(s) to ${root}';")
        return sb.toString()
    }

    // ─── Shell execution ──────────────────────────────────────────────────────

    private fun privileged(command: String): ShellResult {
        if (shizukuOk()) {
            log("Eksekusi via Shizuku...")
            val method  = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            return readProcess(process)
        }
        if (rootOk()) {
            log("Eksekusi via root/su...")
            return normal(arrayOf("su", "-c", command))
        }
        throw IllegalStateException("Shizuku/root belum aktif. Aktifkan Shizuku terlebih dahulu.")
    }

    private fun shizukuOk(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    private fun rootOk(): Boolean = try {
        val result = normal(arrayOf("su", "-c", "id"))
        result.code == 0 && result.out.contains("uid=0")
    } catch (_: Throwable) { false }

    // ─── File helpers ─────────────────────────────────────────────────────────

    private fun readUrl(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000; conn.readTimeout = 30_000
        return try { BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() } }
        finally { conn.disconnect() }
    }

    private fun downloadFile(url: String, out: File) {
        out.parentFile?.mkdirs()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000; conn.readTimeout = 90_000
        try {
            val total = conn.contentLengthLong.coerceAtLeast(1L)
            var downloaded = 0L
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(out).use { output ->
                    val buffer = ByteArray(131_072)
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        downloaded += n
                        val pct = (downloaded * 100 / total).toInt()
                        onProgress?.invoke(pct, 100, "Download ${pct}%...")
                    }
                }
            }
        } finally { conn.disconnect() }
    }

    private fun unzip(zipFile: File, dest: File) {
        dest.mkdirs()
        val root = dest.canonicalPath + File.separator
        ZipInputStream(FileInputStream(zipFile)).use { input ->
            val buffer = ByteArray(131_072)
            while (true) {
                val entry: ZipEntry = input.nextEntry ?: break
                val out = File(dest, entry.name)
                if (!out.canonicalPath.startsWith(root)) throw IllegalStateException("Zip path tidak aman: ${entry.name}")
                if (entry.isDirectory) out.mkdirs()
                else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { output ->
                        while (true) { val n = input.read(buffer); if (n <= 0) break; output.write(buffer, 0, n) }
                    }
                }
                input.closeEntry()
            }
        }
    }

    /** Extract ZIP stripping a common leading prefix from all entry names. */
    private fun unzipStripping(zipFile: File, dest: File, stripPrefix: String) {
        dest.mkdirs()
        val root   = dest.canonicalPath + File.separator
        val buffer = ByteArray(131_072)
        ZipInputStream(FileInputStream(zipFile)).use { input ->
            while (true) {
                val entry: ZipEntry = input.nextEntry ?: break
                val name = entry.name.removePrefix(stripPrefix)
                if (name.isBlank()) { input.closeEntry(); continue }
                val out  = File(dest, name)
                if (!out.canonicalPath.startsWith(root)) throw IllegalStateException("Zip path tidak aman setelah strip: $name")
                if (entry.isDirectory) out.mkdirs()
                else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { output ->
                        while (true) { val n = input.read(buffer); if (n <= 0) break; output.write(buffer, 0, n) }
                    }
                }
                input.closeEntry()
            }
        }
    }

    private fun zipEntries(zipFile: File): List<String> {
        val result = mutableListOf<String>()
        ZipFile(zipFile).use { zip ->
            val entries: Enumeration<out ZipEntry> = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory) result.add(safeRelativePath(entry.name))
            }
        }
        return result
    }

    /** If all entries share a top-level directory prefix, strip it. */
    private fun stripCommonPrefix(entries: List<String>): List<String> {
        if (entries.isEmpty()) return entries
        val prefix = entries[0].substringBefore('/', "")
        if (prefix.isBlank()) return entries
        if (entries.all { it.startsWith("$prefix/") }) {
            return entries.map { it.removePrefix("$prefix/") }.filter { it.isNotBlank() }
        }
        return entries
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input: InputStream ->
            val buffer = ByteArray(131_072)
            while (true) { val n = input.read(buffer); if (n <= 0) break; md.update(buffer, 0, n) }
        }
        return md.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    private fun normal(cmd: Array<String>): ShellResult =
        readProcess(ProcessBuilder(*cmd).redirectErrorStream(true).start())

    private fun readProcess(process: Process): ShellResult {
        val out = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            while (true) { val line = reader.readLine() ?: break; out.append(line).append('\n') }
        }
        return ShellResult(process.waitFor(), out.toString())
    }

    private fun deleteRec(file: File?) {
        if (file == null || !file.exists()) return
        if (file.isDirectory) file.listFiles()?.forEach { deleteRec(it) }
        file.delete()
    }

    private fun safeRelativePath(input: String): String {
        val rel = input.replace('\\', '/').trimStart('/')
        if (rel.startsWith("../") || rel.contains("/../") || rel == "..") throw IllegalStateException("Path tidak aman: $input")
        return rel
    }

    private fun log(message: String) = onLog(message)
    private fun q(value: String): String = "'" + value.replace("'", "'\\''") + "'"
    private fun parent(path: String): String = path.substringBeforeLast('/', "/")

    data class ShellResult(val code: Int, val out: String)
}
