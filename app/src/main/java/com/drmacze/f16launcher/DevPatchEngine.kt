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
    private val onLog: (String) -> Unit
) {
    companion object {
        const val GAME_PACKAGE = "com.ea.gp.fifaworld"
        const val DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json"
        private const val PREFS = "f16_launcher"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun localVersion(): Int = prefs.getInt("local_version_code", 1)
    fun latestBackupRoot(): String = prefs.getString("last_backup_root", "") ?: ""
    fun latestBackupTarget(): String = prefs.getString("last_backup_target", "/sdcard/Android/data/$GAME_PACKAGE/") ?: "/sdcard/Android/data/$GAME_PACKAGE/"

    fun accessMode(): String = when {
        shizukuOk() -> "Shizuku aktif"
        rootOk() -> "Root aktif"
        else -> "Belum aktif"
    }

    fun fetchManifest(): JSONObject {
        val url = prefs.getString("manifest_url", DEFAULT_MANIFEST) ?: DEFAULT_MANIFEST
        log("Fetch manifest: $url")
        return JSONObject(readUrl(url))
    }

    fun applyAvailableUpdates() {
        val manifest = fetchManifest()
        val latest = manifest.getInt("latestVersionCode")
        var local = localVersion()
        if (local >= latest) {
            log("Sudah versi terbaru: v$local")
            return
        }
        log("Mulai direct Compose update: v$local → v$latest")
        val patches = manifest.getJSONArray("patches")
        while (local < latest) {
            val patch = findPatch(patches, local) ?: throw IllegalStateException("Patch dari v$local tidak ditemukan.")
            val to = patch.getInt("to")
            log("Apply patch: ${patch.optString("name", "v$to")}")
            applyPatch(patch)
            local = to
            prefs.edit().putInt("local_version_code", local).apply()
            log("Patch selesai. Local version sekarang v$local")
        }
        log("Semua update selesai.")
    }

    fun restoreLastBackup() {
        val backup = latestBackupRoot()
        val target = latestBackupTarget()
        if (backup.isBlank()) throw IllegalStateException("Belum ada backup yang tersimpan.")
        log("Restore backup: $backup")
        val result = privileged("set -e; mkdir -p ${q(target)}; cp -af ${q("$backup/.")} ${q(target)}; echo 'Restore backup selesai';")
        log(result.out.trim().ifEmpty { "Restore command selesai." })
        if (result.code != 0) throw IllegalStateException("Restore gagal. Exit code ${result.code}")
    }

    fun resetLocalVersion(version: Int = 1) {
        prefs.edit().putInt("local_version_code", version).apply()
        log("Local version diset ke v$version")
    }

    private fun findPatch(arr: JSONArray, from: Int): JSONObject? {
        for (i in 0 until arr.length()) {
            val patch = arr.getJSONObject(i)
            if (patch.optInt("from") == from) return patch
        }
        return null
    }

    private fun applyPatch(patch: JSONObject) {
        val to = patch.getInt("to")
        var target = patch.optString("target", "/sdcard/Android/data/$GAME_PACKAGE/")
        if (!target.endsWith("/")) target += "/"

        val work = File(context.getExternalFilesDir(null), "updates/v$to")
        val extracted = File(work, "extracted")
        deleteRec(work)
        extracted.mkdirs()

        val entries = mutableListOf<String>()
        if (patch.has("files")) {
            val files = patch.getJSONArray("files")
            val root = extracted.canonicalPath + File.separator
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
            val zip = File(work, "patch.zip")
            val url = patch.getString("url")
            log("Download zip patch...")
            downloadFile(url, zip)
            val expectedSha = patch.optString("sha256", "").trim()
            if (expectedSha.isNotEmpty()) {
                log("Verify SHA-256...")
                val actual = sha256(zip)
                if (!expectedSha.equals(actual, ignoreCase = true)) throw IllegalStateException("SHA-256 tidak cocok.")
            }
            unzip(zip, extracted)
            entries.addAll(zipEntries(zip))
            log("Zip patch siap: ${entries.size} file.")
        }

        if (entries.isEmpty()) throw IllegalStateException("Patch kosong.")
        val backup = "/sdcard/F16Launcher/backups/v$to/${System.currentTimeMillis()}"
        log("Backup target sebelum copy...")
        val result = privileged(copyCommand(extracted.absolutePath, target, backup, entries))
        log(result.out.trim().ifEmpty { "Copy selesai." })
        if (result.code != 0) throw IllegalStateException("Apply gagal. Exit code ${result.code}")
        prefs.edit().putString("last_backup_root", backup).putString("last_backup_target", target).apply()
        log("Backup tersimpan: $backup")
    }

    private fun copyCommand(srcRoot: String, targetRootInput: String, backupRoot: String, entries: List<String>): String {
        var targetRoot = targetRootInput
        if (!targetRoot.endsWith("/")) targetRoot += "/"
        val command = StringBuilder("set -e; mkdir -p ${q(targetRoot)}; mkdir -p ${q(backupRoot)}; ")
        for (rel in entries) {
            val src = "$srcRoot/$rel"
            val dst = "$targetRoot$rel"
            val backup = "$backupRoot/$rel"
            command.append("mkdir -p ${q(parent(dst))}; ")
            command.append("if [ -e ${q(dst)} ]; then mkdir -p ${q(parent(backup))}; cp -af ${q(dst)} ${q(backup)}; fi; ")
            command.append("cp -af ${q(src)} ${q(dst)}; ")
        }
        command.append("echo 'Applied ${entries.size} file(s)';")
        return command.toString()
    }

    private fun privileged(command: String): ShellResult {
        if (shizukuOk()) {
            log("Apply menggunakan Shizuku...")
            val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            return readProcess(process)
        }
        if (rootOk()) {
            log("Apply menggunakan root/su...")
            return normal(arrayOf("su", "-c", command))
        }
        throw IllegalStateException("Shizuku/root belum aktif.")
    }

    private fun shizukuOk(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    private fun rootOk(): Boolean = try {
        val result = normal(arrayOf("su", "-c", "id"))
        result.code == 0 && result.out.contains("uid=0")
    } catch (_: Throwable) {
        false
    }

    private fun readUrl(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        return try {
            BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun downloadFile(url: String, out: File) {
        out.parentFile?.mkdirs()
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 60000
        try {
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(out).use { output ->
                    val buffer = ByteArray(131072)
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun unzip(zipFile: File, dest: File) {
        dest.mkdirs()
        val root = dest.canonicalPath + File.separator
        ZipInputStream(FileInputStream(zipFile)).use { input ->
            val buffer = ByteArray(131072)
            while (true) {
                val entry: ZipEntry = input.nextEntry ?: break
                val out = File(dest, entry.name)
                if (!out.canonicalPath.startsWith(root)) throw IllegalStateException("Zip path tidak aman.")
                if (entry.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { output ->
                        while (true) {
                            val n = input.read(buffer)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                        }
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

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input: InputStream ->
            val buffer = ByteArray(131072)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
    }

    private fun normal(cmd: Array<String>): ShellResult = readProcess(ProcessBuilder(*cmd).redirectErrorStream(true).start())

    private fun readProcess(process: Process): ShellResult {
        val out = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                out.append(line).append('\n')
            }
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
