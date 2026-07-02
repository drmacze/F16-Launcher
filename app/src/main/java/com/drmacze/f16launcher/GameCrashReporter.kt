package com.drmacze.f16launcher

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class GameCrashReporter(private val context: Context) {
    private val prefs = context.getSharedPreferences("f16_launcher", 0)

    fun prepareLaunch() {
        prefs.edit()
            .putBoolean("awaiting_game_launch", true)
            .putLong("game_launch_started_at", System.currentTimeMillis())
            .apply()
        try {
            runShell("logcat -c")
        } catch (_: Throwable) {
        }
    }

    fun consumeFastReturn(): String? {
        if (!prefs.getBoolean("awaiting_game_launch", false)) return null
        val startedAt = prefs.getLong("game_launch_started_at", 0L)
        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed < 1200L) return null
        if (elapsed > 25000L) {
            prefs.edit().putBoolean("awaiting_game_launch", false).apply()
            return null
        }
        prefs.edit().putBoolean("awaiting_game_launch", false).apply()
        val report = collectReport(elapsed)
        val file = reportFile()
        file.parentFile?.mkdirs()
        file.writeText(report)
        prefs.edit()
            .putString("last_game_crash_report", report)
            .putString("last_game_crash_report_path", file.absolutePath)
            .putLong("last_game_crash_at", System.currentTimeMillis())
            .apply()
        return summarize(report)
    }

    fun reportFile(): File = File(context.getExternalFilesDir(null), "diagnostics/FIFA16-crash-report.txt")

    private fun collectReport(elapsedMs: Long): String {
        val header = buildString {
            appendLine("DLavie FIFA 16 crash report")
            appendLine("package=com.ea.gp.fifaworld")
            appendLine("returnMs=$elapsedMs")
            appendLine("time=${System.currentTimeMillis()}")
            appendLine()
        }
        val checks = try {
            runShell(
                "echo '--- package ---'; " +
                    "dumpsys package com.ea.gp.fifaworld | grep -E 'versionCode|versionName|primaryCpuAbi|secondaryCpuAbi|nativeLibraryPath|signatures' | head -40; " +
                    "echo '--- data root ---'; ls -la /sdcard/Android/data/com.ea.gp.fifaworld | head -80; " +
                    "echo '--- obb root ---'; ls -la /sdcard/Android/obb/com.ea.gp.fifaworld | head -80; " +
                    "echo '--- logcat ---'; " +
                    "logcat -d -t 700 | grep -i -E 'com.ea.gp.fifaworld|AndroidRuntime|FATAL EXCEPTION|SIGSEGV|signal 11|UnsatisfiedLinkError|Exception|lib|obb|asset|fifa|eafc|ea' | tail -220"
            ).out
        } catch (t: Throwable) {
            "Gagal ambil diagnostics via Shizuku: ${t.message ?: "unknown"}"
        }
        return header + checks
    }

    private fun summarize(report: String): String {
        val lower = report.lowercase()
        return when {
            "unsatisfiedlinkerror" in lower -> "native library APK tidak cocok / hilang"
            "main.13" in lower || "patch.26" in lower || "obb" in lower -> "crash saat membaca OBB/DATA"
            "sigsegv" in lower || "signal 11" in lower -> "native crash dari engine game"
            "fatal exception" in lower || "androidruntime" in lower -> "fatal exception di game"
            "permission" in lower || "denied" in lower -> "permission/storage ditolak"
            else -> "game kembali cepat setelah Play"
        }
    }

    private fun runShell(command: String): ShellResult {
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

    data class ShellResult(val code: Int, val out: String)
}
