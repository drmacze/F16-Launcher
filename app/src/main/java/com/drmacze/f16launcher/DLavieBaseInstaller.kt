package com.drmacze.f16launcher

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.math.max

private const val BASE_GAME_PACKAGE = "com.ea.gp.fifaworld"
private const val BASE_TARGET_DATA = "/sdcard/Android/data/com.ea.gp.fifaworld/"
private const val BASE_TARGET_OBB = "/sdcard/Android/obb/com.ea.gp.fifaworld/"
private const val BASE_MARKER = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private const val BASE_VERSION_MARKER = "v26:7eb760ea663d019e3cb0e3ac70f9841d54255ac882b102b06eccb173212ac80a"

private data class BaseInstallAsset(
    val label: String,
    val fileName: String,
    val url: String,
    val sha256: String,
    val size: Long,
    val kind: String
)

private data class BaseInstallState(
    val status: String = "CHECK",
    val message: String = "Cek data utama DLavie 26.",
    val marker: String = "Unknown",
    val shizuku: String = "Unknown",
    val working: Boolean = false,
    val canInstall: Boolean = false,
    val canRepair: Boolean = false,
    val canClearCache: Boolean = false,
    val progress: Float = 0f,
    val progressText: String = "",
    val sizeText: String = "-",
    val speedText: String = "-",
    val etaText: String = "-"
)

@Composable
fun DLavieBaseInstallCard(isGameInstalled: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var state by remember { mutableStateOf(baseInitialState(context, isGameInstalled)) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xDD101111),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, BaseBorder)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Base Data", color = BaseWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = BaseFont)
                    Text(state.message, color = BaseMuted, fontSize = 14.sp, fontFamily = BaseFont, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                BasePill(if (state.working) "WAIT" else state.status, if (state.status == "READY" || state.status == "VERIFIED") BaseGreen else if (state.status == "INSTALL" || state.status == "REPAIR") BaseCyan else BaseRed)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                BaseInfo("Shizuku", state.shizuku, Modifier.weight(1f))
                BaseInfo("Data", state.marker, Modifier.weight(1f))
            }

            if (state.progressText.isNotBlank()) {
                BaseProgress(state)
            }

            Button(
                onClick = { state = baseInitialState(context, isGameInstalled) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BaseCyan, contentColor = Color(0xFF001018)),
                enabled = !state.working
            ) { Text("Recheck Base Data", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = BaseFont) }

            if (state.canInstall) {
                Button(
                    onClick = {
                        scope.launch {
                            state = state.copy(working = true, message = "Menyiapkan full install...", progressText = "Preparing")
                            state = withContext(Dispatchers.IO) {
                                baseInstallAll(context) { progress -> withContext(Dispatchers.Main) { state = progress } }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BaseGreen, contentColor = Color(0xFF001407)),
                    enabled = !state.working
                ) { Text("Install Full Data", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = BaseFont) }
            }

            if (state.canRepair) {
                Button(
                    onClick = {
                        scope.launch {
                            state = state.copy(working = true, message = "Repair data utama...", progressText = "Repairing")
                            state = withContext(Dispatchers.IO) {
                                baseInstallAll(context) { progress -> withContext(Dispatchers.Main) { state = progress } }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BaseGreen, contentColor = Color(0xFF001407)),
                    enabled = !state.working
                ) { Text("Repair Full Data", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = BaseFont) }
            }

            if (state.canClearCache) {
                Button(
                    onClick = {
                        baseCacheRoot(context).deleteRecursively()
                        state = baseInitialState(context, isGameInstalled).copy(message = "Cache DLavie dibersihkan.")
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121816), contentColor = BaseMuted),
                    enabled = !state.working
                ) { Text("Clear Cache", fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = BaseFont) }
            }
        }
    }
}

@Composable
private fun BaseInfo(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0xFF0B0F0E), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, BaseBorder)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = BaseMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = BaseFont, maxLines = 1)
            Text(value, color = BaseWhite, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = BaseFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BasePill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.16f), border = BorderStroke(1.dp, color.copy(alpha = 0.55f)), shape = RoundedCornerShape(999.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1, fontFamily = BaseFont)
    }
}

@Composable
private fun BaseProgress(state: BaseInstallState) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B0F0E), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, BaseBorder)) {
        Column(Modifier.padding(12.dp)) {
            Text(state.progressText, color = BaseWhite, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = BaseFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(9.dp).background(Color(0xFF17201C), RoundedCornerShape(99.dp))) {
                Box(Modifier.fillMaxWidth(state.progress.coerceIn(0.02f, 1f)).height(9.dp).background(BaseCyan, RoundedCornerShape(99.dp)))
            }
            Spacer(Modifier.height(8.dp))
            Text("${state.sizeText} • ${state.speedText} • ETA ${state.etaText}", color = BaseMuted, fontSize = 12.sp, fontFamily = BaseFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun baseInitialState(context: Context, gameInstalled: Boolean): BaseInstallState {
    val shizuku = baseShizukuState()
    val marker = baseReadMarkerSmart()
    val cacheReady = baseAssets().any { File(baseCacheRoot(context), it.fileName).exists() }
    if (!gameInstalled) {
        return BaseInstallState(status = "MISSING", message = "FIFA 16 Mobile belum terinstall.", marker = marker, shizuku = shizuku, canClearCache = cacheReady)
    }
    if (marker.startsWith("v26") || marker == "Verified") {
        return BaseInstallState(status = "VERIFIED", message = "Full data DLavie 26 sudah terverifikasi.", marker = marker, shizuku = shizuku, canClearCache = cacheReady)
    }
    return when (shizuku) {
        "Ready" -> BaseInstallState(status = "INSTALL", message = "Full data belum lengkap. Install OBB dan data utama dari DLavie.", marker = marker, shizuku = shizuku, canInstall = true, canRepair = marker != "No marker", canClearCache = cacheReady)
        "Permission" -> BaseInstallState(status = "SHIZUKU", message = "Izinkan Shizuku dulu agar DLavie bisa memasang data.", marker = marker, shizuku = shizuku, canClearCache = cacheReady)
        else -> BaseInstallState(status = "SHIZUKU", message = "Aktifkan Shizuku untuk full install otomatis.", marker = marker, shizuku = shizuku, canClearCache = cacheReady)
    }
}

private suspend fun baseInstallAll(context: Context, onProgress: suspend (BaseInstallState) -> Unit): BaseInstallState {
    if (baseShizukuState() != "Ready") return baseInitialState(context, baseGameInstalled(context)).copy(status = "SHIZUKU", message = "Shizuku belum Ready.")
    val root = baseCacheRoot(context)
    root.mkdirs()
    val assets = baseAssets()
    for ((index, asset) in assets.withIndex()) {
        val file = File(root, asset.fileName)
        if (!file.exists() || file.length() != asset.size || baseSha256(file) != asset.sha256) {
            val ok = baseDownloadWithRetry(asset, file, index, assets.size, onProgress)
            if (!ok) return baseInitialState(context, true).copy(status = "FAILED", message = "Download ${asset.label} gagal setelah 3 percobaan.", canInstall = true)
        }
        if (baseSha256(file) != asset.sha256) {
            file.delete()
            return baseInitialState(context, true).copy(status = "FAILED", message = "SHA ${asset.label} tidak valid.", canInstall = true)
        }
    }

    onProgress(BaseInstallState(status = "WAIT", message = "Memasang OBB...", marker = baseReadMarkerSmart(), shizuku = "Ready", working = true, progress = 0.62f, progressText = "Copying OBB", sizeText = "Verified", speedText = "-", etaText = "-"))
    val mainObb = File(root, "main.13.com.ea.gp.fifaworld.obb")
    val patchObb = File(root, "patch.26.com.ea.gp.fifaworld.obb")
    val obbCmd = buildString {
        append("mkdir -p ${baseShellQuote(BASE_TARGET_OBB)}\n")
        append("cp -af ${baseShellQuote(mainObb.absolutePath)} ${baseShellQuote(BASE_TARGET_OBB + mainObb.name)}\n")
        append("cp -af ${baseShellQuote(patchObb.absolutePath)} ${baseShellQuote(BASE_TARGET_OBB + patchObb.name)}\n")
        append("ls -l ${baseShellQuote(BASE_TARGET_OBB + mainObb.name)} >/dev/null || exit 21\n")
        append("ls -l ${baseShellQuote(BASE_TARGET_OBB + patchObb.name)} >/dev/null || exit 22\n")
    }
    val obbResult = baseRunShizuku(obbCmd)
    if (obbResult.first != 0) return baseInitialState(context, true).copy(status = "FAILED", message = "Gagal memasang OBB.", canInstall = true)

    onProgress(BaseInstallState(status = "WAIT", message = "Extract full data...", marker = baseReadMarkerSmart(), shizuku = "Ready", working = true, progress = 0.72f, progressText = "Extracting data", sizeText = "1.35 GB", speedText = "-", etaText = "-"))
    val extractDir = File(root, "extract_full_v26")
    if (extractDir.exists()) extractDir.deleteRecursively()
    extractDir.mkdirs()
    try {
        baseExtractDataZip(File(root, "dlavie26-data.zip"), extractDir)
    } catch (_: Throwable) {
        return baseInitialState(context, true).copy(status = "FAILED", message = "Extract full data gagal.", canInstall = true)
    }

    onProgress(BaseInstallState(status = "WAIT", message = "Memasang full data...", marker = baseReadMarkerSmart(), shizuku = "Ready", working = true, progress = 0.86f, progressText = "Applying data", sizeText = "Verified", speedText = "-", etaText = "-"))
    val safeIni = File(extractDir, "cl.ini")
    val applyCmd = buildString {
        append("mkdir -p ${baseShellQuote(BASE_TARGET_DATA)}\n")
        append("cp -af ${baseShellQuote(extractDir.absolutePath + "/.")} ${baseShellQuote(BASE_TARGET_DATA)}\n")
        append("rm -rf ${baseShellQuote(BASE_TARGET_DATA + "dlavie26-data")}\n")
        if (safeIni.exists()) append("cp -af ${baseShellQuote(safeIni.absolutePath)} ${baseShellQuote(BASE_TARGET_DATA + "cl.ini")}\n")
        val encoded = Base64.encodeToString(BASE_VERSION_MARKER.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        append("printf %s ${baseShellQuote(encoded)} | base64 -d > ${baseShellQuote(BASE_MARKER)}\n")
        append("[ -f ${baseShellQuote(BASE_MARKER)} ] || exit 31\n")
    }
    val applyResult = baseRunShizuku(applyCmd)
    if (applyResult.first != 0) return baseInitialState(context, true).copy(status = "FAILED", message = "Gagal memasang full data.", canInstall = true)

    delay(300)
    val marker = baseReadMarkerSmart()
    return if (marker.startsWith("v26")) {
        BaseInstallState(status = "VERIFIED", message = "Full data DLavie 26 berhasil dipasang.", marker = marker, shizuku = "Ready", progress = 1f, progressText = "Base data verified", sizeText = "Done", speedText = "Verified", etaText = "0s", canClearCache = true)
    } else {
        BaseInstallState(status = "FAILED", message = "Install selesai, tapi marker belum terverifikasi.", marker = marker, shizuku = "Ready", canRepair = true, canClearCache = true)
    }
}

private suspend fun baseDownloadWithRetry(asset: BaseInstallAsset, output: File, index: Int, totalAssets: Int, onProgress: suspend (BaseInstallState) -> Unit): Boolean {
    for (attempt in 1..3) {
        try {
            baseDownload(asset, output, index, totalAssets, attempt, onProgress)
            return true
        } catch (_: Throwable) {
            output.delete()
            if (attempt < 3) delay(900)
        }
    }
    return false
}

private suspend fun baseDownload(asset: BaseInstallAsset, output: File, index: Int, totalAssets: Int, attempt: Int, onProgress: suspend (BaseInstallState) -> Unit) {
    output.parentFile?.mkdirs()
    val connection = URL(asset.url).openConnection() as HttpURLConnection
    connection.connectTimeout = 20000
    connection.readTimeout = 45000
    connection.instanceFollowRedirects = true
    val total = if (connection.contentLengthLong > 0) connection.contentLengthLong else asset.size
    val started = System.currentTimeMillis()
    var downloaded = 0L
    var lastUi = 0L
    val digest = MessageDigest.getInstance("SHA-256")
    FileOutputStream(output).use { out ->
        connection.inputStream.use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
                digest.update(buffer, 0, read)
                downloaded += read
                val now = System.currentTimeMillis()
                if (now - lastUi > 650L) {
                    lastUi = now
                    val speed = downloaded * 1000L / max(1L, now - started)
                    val eta = if (speed > 0L) (total - downloaded) / speed else -1L
                    val assetProgress = downloaded.toFloat() / max(1L, total).toFloat()
                    val totalProgress = ((index.toFloat() + assetProgress) / totalAssets.toFloat()).coerceIn(0f, 1f) * 0.58f
                    onProgress(BaseInstallState(status = "WAIT", message = "Downloading ${asset.label}...", marker = baseReadMarkerSmart(), shizuku = "Ready", working = true, progress = totalProgress, progressText = "${asset.label} attempt $attempt/3", sizeText = "${baseFormatBytes(downloaded)} / ${baseFormatBytes(total)}", speedText = "${baseFormatBytes(speed)}/s", etaText = baseFormatSeconds(eta)))
                }
            }
        }
    }
    connection.disconnect()
    val actual = digest.digest().joinToString("") { "%02x".format(it) }
    if (actual != asset.sha256) throw IllegalStateException("SHA mismatch")
}

private fun baseExtractDataZip(zipFile: File, outputDir: File) {
    ZipInputStream(zipFile.inputStream()).use { zis ->
        while (true) {
            val entry = zis.nextEntry ?: break
            var name = entry.name.trim().replace('\\', '/')
            if (name.startsWith("dlavie26-data/")) name = name.removePrefix("dlavie26-data/")
            if (name.startsWith("com.ea.gp.fifaworld/")) name = name.removePrefix("com.ea.gp.fifaworld/")
            if (name.isBlank()) { zis.closeEntry(); continue }
            if (!baseSafeRelativePath(name)) throw IllegalStateException("Unsafe zip path")
            val out = File(outputDir, name).canonicalFile
            val root = outputDir.canonicalFile
            if (!out.path.startsWith(root.path)) throw IllegalStateException("Zip path escape")
            if (entry.isDirectory) out.mkdirs() else {
                out.parentFile?.mkdirs()
                FileOutputStream(out).use { output -> zis.copyTo(output) }
            }
            zis.closeEntry()
        }
    }
}

private fun baseAssets(): List<BaseInstallAsset> = listOf(
    BaseInstallAsset(
        "Main OBB",
        "main.13.com.ea.gp.fifaworld.obb",
        // PRIVACY: Use DLavie proxy instead of GitHub direct URL.
        DLAVIE_PROXY_URL + "?f=fifa16-obb-main",
        "fe3e66c5e8c804656d8ee9ca62ace64a1fe968669f5c397b23ce174b0b8c720c",
        1376082760L,
        "obb"
    ),
    BaseInstallAsset(
        "Patch OBB",
        "patch.26.com.ea.gp.fifaworld.obb",
        DLAVIE_PROXY_URL + "?f=fifa16-obb-patch",
        "bdca1604e7fc8dc80d96d656ae0e21ff3bd1ccf75a62ecaab0109dd269ef38a",
        102869675L,
        "obb"
    ),
    BaseInstallAsset(
        "Full Data",
        "dlavie26-data.zip",
        DLAVIE_PROXY_URL + "?f=fifa16-data",
        "7eb760ea663d019e3cb0e3ac70f9841d54255ac882b102b06eccb173212ac80a",
        1454191840L,
        "data"
    )
)

private fun baseCacheRoot(context: Context): File = File(context.externalCacheDir ?: context.cacheDir, "full-base-v26")
private fun baseGameInstalled(context: Context): Boolean = try { context.packageManager.getPackageInfo(BASE_GAME_PACKAGE, 0); true } catch (_: PackageManager.NameNotFoundException) { false }
private fun baseSafeRelativePath(path: String): Boolean = path.isNotBlank() && !path.startsWith("/") && !path.contains("..") && !path.contains("\\")
private fun baseShellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

private fun baseShizukuState(): String = try {
    if (!Shizuku.pingBinder()) "Inactive" else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) "Ready" else "Permission"
} catch (_: Throwable) { "Missing" }

private fun baseReadMarkerSmart(): String {
    val direct = try {
        val f = File(BASE_MARKER)
        if (f.exists()) f.readText().trim().take(12).ifEmpty { "Verified" } else "No marker"
    } catch (_: Throwable) { "Protected" }
    if (direct != "No marker" && direct != "Protected") return direct
    if (baseShizukuState() != "Ready") return direct
    val cmd = "if [ -f ${baseShellQuote(BASE_MARKER)} ]; then head -c 32 ${baseShellQuote(BASE_MARKER)}; else echo NO_MARKER; fi"
    val out = baseRunShizuku(cmd).second.trim()
    return if (out.startsWith("v26")) out.take(12) else "No marker"
}

private fun baseRunShizuku(script: String): Pair<Int, String> = try {
    val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
    method.isAccessible = true
    val process = method.invoke(null, arrayOf("sh", "-c", script), null, null) as Process
    val exit = process.waitFor()
    val output = process.inputStream.bufferedReader().readText() + process.errorStream.bufferedReader().readText()
    exit to output
} catch (e: Throwable) { -1 to (e.message ?: "Shizuku failed") }

private fun baseSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(128 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun baseFormatBytes(bytes: Long): String {
    if (bytes < 1024L) return "${bytes} B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "${"%.1f".format(kb)} KB"
    val mb = kb / 1024.0
    if (mb < 1024.0) return "${"%.1f".format(mb)} MB"
    return "${"%.2f".format(mb / 1024.0)} GB"
}

private fun baseFormatSeconds(seconds: Long): String {
    if (seconds < 0L) return "-"
    if (seconds < 60L) return "${seconds}s"
    val m = seconds / 60L
    val s = seconds % 60L
    return "${m}m ${s}s"
}

private val BaseFont = FontFamily.SansSerif
private val BaseWhite = Color(0xFFF7F7F7)
private val BaseMuted = Color(0xFF7A7F83)
private val BaseBorder = Color(0xFF252A2C)
private val BaseGreen = Color(0xFF20E070)
private val BaseCyan = Color(0xFF28D7FF)
private val BaseRed = Color(0xFFFF4D4D)
