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
    private val prefs = context.getSharedPreferences("f16_launcher", 0)

    fun hasOfficialContent(manifest: PublicInstallManifest): Boolean = hasOfficialData(manifest) && hasOfficialObb(manifest)

    fun hasOfficialData(manifest: PublicInstallManifest): Boolean = hasMarker(manifest.data.target)

    fun hasOfficialObb(manifest: PublicInstallManifest): Boolean = hasMarker(manifest.obb.target)

    fun cleanRuntime(manifest: PublicInstallManifest) {
        val dataTarget = normalizeTarget(manifest.data.target)
        val obbTarget = normalizeTarget(manifest.obb.target)
        onProgress(5, "Force-stop game")
        val command = "set -e; " +
            "am force-stop com.ea.gp.fifaworld >/dev/null 2>&1 || true; " +
            "rm -rf ${quote(dataTarget)} ${quote(obbTarget)}; " +
            "mkdir -p ${quote(dataTarget)} ${quote(obbTarget)}; " +
            "echo 'Clean runtime complete';"
        val result = runWithShizuku(command)
        if (result.code != 0) throw IllegalStateException("Clean runtime gagal. Exit ${result.code}")
        prefs.edit()
            .putBoolean("dlavie_obb_installed", false)
            .putBoolean("dlavie_data_installed", false)
            .putBoolean("first_run_done", false)
            .putBoolean("dlavie_content_installed", false)
            .apply()
        onProgress(15, "Runtime cleaned")
        onLog(result.out.trim().ifEmpty { "Clean runtime complete." })
    }

    fun installObbOnly(manifest: PublicInstallManifest) {
        installAsset(manifest.obb, 5, 90)
        prefs.edit()
            .putBoolean("dlavie_obb_installed", true)
            .putString("dlavie_content_product", manifest.productName)
            .apply()
        onProgress(100, "OBB ready")
        onLog("DLavie OBB siap.")
    }

    fun installDataOnly(manifest: PublicInstallManifest) {
        installAsset(manifest.data, 5, 90)
        prefs.edit()
            .putBoolean("dlavie_data_installed", true)
            .putString("dlavie_content_product", manifest.productName)
            .apply()
        onProgress(100, "DATA ready")
        onLog("DLavie DATA siap.")
    }

    fun installDataAndObb(manifest: PublicInstallManifest) {
        installObbOnly(manifest)
        installDataOnly(manifest)
        prefs.edit()
            .putBoolean("dlavie_content_installed", true)
            .apply()
        onProgress(100, "Content ready")
        onLog("DLavie DATA/OBB siap.")
    }

    fun doctorReport(): String {
        val command = "echo '--- package ---'; " +
            "dumpsys package com.ea.gp.fifaworld | grep -E 'versionCode|versionName|primaryCpuAbi|nativeLibraryPath' | head -40 || true; " +
            "echo '--- data root ---'; ls -la /sdcard/Android/data/com.ea.gp.fifaworld 2>/dev/null | head -80 || true; " +
            "echo '--- data files ---'; ls -la /sdcard/Android/data/com.ea.gp.fifaworld/files 2>/dev/null | head -80 || true; " +
            "echo '--- obb root ---'; ls -la /sdcard/Android/obb/com.ea.gp.fifaworld 2>/dev/null | head -80 || true; " +
            "echo '--- recent fifa log ---'; logcat -d -t 500 | grep -i -E 'com.ea.gp.fifaworld|fifa|easports|AndroidRuntime|FATAL EXCEPTION|SIGSEGV|signal 11|obb|asset|permission|denied|exception' | tail -160 || true"
        return runWithShizuku(command).out
    }

    private fun hasMarker(target: String): Boolean {
        return try {
            if (target.isBlank()) return false
            val command = "[ -f ${quote(markerPath(target))} ]"
            runWithShizuku(command).code == 0
        } catch (_: Throwable) {
            false
        }
    }

    private fun installAsset(asset: InstallAsset, progressBase: Int, progressSpan: Int): File {
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
        copyDirWithShizuku(extracted.absolutePath, asset.target, asset.fileName)
        writeMarker(asset)
        onProgress(progressBase + progressSpan, "Installed ${asset.fileName}")
        onLog("Installed ${asset.fileName}")
        return extracted
    }

    private fun writeMarker(asset: InstallAsset) {
        val target = normalizeTarget(asset.target)
        val text = "DLavie official content\n${asset.fileName}\n${asset.versionName}\n"
        val command = "mkdir -p ${quote(target)}; printf ${quote(text)} > ${quote(markerPath(target))}"
        val result = runWithShizuku(command)
        if (result.code != 0) onLog("Marker warning: exit ${result.code}")
    }

    private fun copyDirWithShizuku(srcRoot: String, targetRoot: String, fileName: String) {
        val target = normalizeTarget(targetRoot)
        val clean = if (fileName.contains("obb", ignoreCase = true) || target.contains("/Android/obb/")) {
            "rm -f ${quote(target)}main.*.com.ea.gp.fifaworld.obb ${quote(target)}patch.*.com.ea.gp.fifaworld.obb;"
        } else {
            ""
        }
        val command = "set -e; mkdir -p ${quote(target)}; $clean cp -af ${quote("$srcRoot/.")} ${quote(target)}; echo 'Content copied';"
        val result = runWithShizuku(command)
        if (result.code != 0) throw IllegalStateException("Copy content gagal. Exit ${result.code}")
        onLog(result.out.trim().ifEmpty { "Content copied." })
    }

    private fun runWithShizuku(command: String): ShellResult {
        if (!shizukuOk()) throw IllegalStateException("Shizuku belum aktif atau belum diberi izin.")
        val methodName = "new" + "Process"
        val method = Shizuku::class.java.getDeclaredMethod(
            methodName,
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
        return readProcess(process)
    }

    private fun shizukuOk(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
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

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    data class ShellResult(val code: Int, val out: String)
}
