package com.drmacze.f16launcher

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class PublicContentInstaller(
    private val context: Context,
    private val onLog: (String) -> Unit,
    private val onProgress: (Int, String) -> Unit
) {
    private val manager = PublicInstallManager(context)

    fun hasOfficialContent(manifest: PublicInstallManifest): Boolean {
        return try {
            val dataMarker = markerPath(manifest.data.target)
            val obbMarker = markerPath(manifest.obb.target)
            privileged("[ -f ${q(dataMarker)} ] && [ -f ${q(obbMarker)} ]") .code == 0
        } catch (_: Throwable) {
            false
        }
    }

    fun installDataAndObb(manifest: PublicInstallManifest) {
        installAsset(manifest.data, 5, 45)
        installAsset(manifest.obb, 52, 45)
        context.getSharedPreferences("f16_launcher", 0).edit()
            .putBoolean("dlavie_content_installed", true)
            .putString("dlavie_content_product", manifest.productName)
            .apply()
        onProgress(100, "Content ready")
        onLog("DLavie DATA/OBB siap.")
    }

    private fun installAsset(asset: InstallAsset, progressBase: Int, progressSpan: Int) {
        if (asset.target.isBlank()) throw IllegalStateException("Target belum diisi untuk ${asset.fileName}")
        onProgress(progressBase, "Download ${asset.fileName}")
        val zip = manager.downloadAsset(asset) { p ->
            onProgress(progressBase + (p * progressSpan / 200), "Download ${asset.fileName}")
        }
        val work = File(context.getExternalFilesDir(null), "content-install/${asset.fileName.removeSuffix(".zip")}")
        val extracted = File(work, "extracted")
        deleteRec(work)
        extracted.mkdirs()
        onProgress(progressBase + progressSpan / 2, "Extract ${asset.fileName}")
        unzip(zip, extracted)
        onProgress(progressBase + progressSpan * 70 / 100, "Copy to game folder")
        copyDirPrivileged(extracted.absolutePath, asset.target)
        writeMarker(asset)
        onProgress(progressBase + progressSpan, "Installed ${asset.fileName}")
        onLog("Installed ${asset.fileName}")
    }

    private fun writeMarker(asset: InstallAsset) {
        val target = normalizeTarget(asset.target)
        val text = "DLavie official content\n${asset.fileName}\n${asset.versionName}\n"
        val command = "mkdir -p ${q(target)}; printf ${q(text)} > ${q(markerPath(target))}"
        val result = privileged(command)
        if (result.code != 0) onLog("Marker warning: exit ${result.code}")
    }

    private fun copyDirPrivileged(srcRoot: String, targetRoot: String) {
        val target = normalizeTarget(targetRoot)
        val command = "set -e; mkdir -p ${q(target)}; cp -af ${q("$srcRoot/.")} ${q(target)}; echo 'Content copied';"
        val result = privileged(command)
        if (result.code != 0) throw IllegalStateException("Copy content gagal. Exit ${result.code}")
        onLog(result.out.trim().ifEmpty { "Content copied." })
    }

    private fun privileged(command: String): ShellResult {
        if (shizukuOk()) {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            return readProcess(process)
        }
        if (rootOk()) return readProcess(ProcessBuilder("su", "-c", command).redirectErrorStream(true).start())
        throw IllegalStateException("Shizuku/root belum aktif.")
    }

    private fun shizukuOk(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    private fun rootOk(): Boolean = try {
        val result = readProcess(ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start())
        result.code == 0 && result.out.contains("uid=0")
    } catch (_: Throwable) { false }

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

    private fun unzip(zipFile: File, dest: File) {
        dest.mkdirs()
        val root = dest.canonicalPath + File.separator
        ZipInputStream(FileInputStream(zipFile)).use { input ->
            val buffer = ByteArray(131072)
            while (true) {
                val entry: ZipEntry = input.nextEntry ?: break
                val out = File(dest, safeZipPath(entry.name))
                if (!out.canonicalPath.startsWith(root)) throw IllegalStateException("Zip path tidak aman: ${entry.name}")
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

    private fun safeZipPath(path: String): String {
        val rel = path.replace('\\', '/').trimStart('/')
        if (rel == ".." || rel.startsWith("../") || rel.contains("/../")) throw IllegalStateException("Zip path tidak aman: $path")
        return rel
    }

    private fun normalizeTarget(target: String): String = if (target.endsWith("/")) target else "$target/"
    private fun markerPath(target: String): String = normalizeTarget(target) + ".dlavie_marker"

    private fun deleteRec(file: File?) {
        if (file == null || !file.exists()) return
        if (file.isDirectory) file.listFiles()?.forEach { deleteRec(it) }
        file.delete()
    }

    private fun q(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    data class ShellResult(val code: Int, val out: String)
}
