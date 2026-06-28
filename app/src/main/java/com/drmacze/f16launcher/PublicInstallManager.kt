package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

const val INSTALL_MANIFEST_URL = "https://raw.githubusercontent.com/drmacze/F16/main/updates/install.json"

data class InstallAsset(
    val fileName: String,
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
    val versionName: String,
    val target: String = "",
    val required: Boolean = true,
    val published: Boolean = false
) {
    fun isPublished(): Boolean = published && url.isNotBlank()
}

data class PublicInstallManifest(
    val productName: String,
    val gamePackage: String,
    val statusMessage: String,
    val apk: InstallAsset,
    val data: InstallAsset,
    val obb: InstallAsset
)

class PublicInstallManager(private val context: Context) {
    fun fetchInstallManifest(): PublicInstallManifest {
        val json = JSONObject(readUrl(INSTALL_MANIFEST_URL))
        fun asset(name: String): InstallAsset {
            val obj = json.getJSONObject(name)
            return InstallAsset(
                fileName = obj.optString("fileName", "$name.zip"),
                url = obj.optString("url", ""),
                sha256 = obj.optString("sha256", ""),
                sizeBytes = obj.optLong("sizeBytes", 0L),
                versionName = obj.optString("versionName", "-"),
                target = obj.optString("target", ""),
                required = obj.optBoolean("required", true),
                published = obj.optBoolean("published", obj.optString("url", "").isNotBlank())
            )
        }
        return PublicInstallManifest(
            productName = json.optString("productName", "DLavie 26"),
            gamePackage = json.optString("gamePackage", DevPatchEngine.GAME_PACKAGE),
            statusMessage = json.optJSONObject("status")?.optString("message", "Ready") ?: "Ready",
            apk = asset("apk"),
            data = asset("data"),
            obb = asset("obb")
        )
    }

    fun downloadAsset(asset: InstallAsset, onProgress: (Int) -> Unit): File {
        if (!asset.isPublished()) throw IllegalStateException("Konten belum dipublish: ${asset.fileName}")
        val dir = File(context.getExternalFilesDir(null), "public-install")
        dir.mkdirs()
        val out = File(dir, asset.fileName)
        val conn = URL(asset.url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 90000
        try {
            val total = if (asset.sizeBytes > 0) asset.sizeBytes else conn.contentLengthLong
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(out).use { output ->
                    val buffer = ByteArray(131072)
                    var copied = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        copied += n
                        if (total > 0) onProgress(((copied * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        if (asset.sha256.isNotBlank()) {
            val actual = sha256(out)
            if (!asset.sha256.equals(actual, ignoreCase = true)) throw IllegalStateException("SHA-256 tidak cocok untuk ${asset.fileName}")
        }
        onProgress(100)
        return out
    }

    fun openApkInstaller(apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apkFile)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    private fun readUrl(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 30000
        return try { BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() } } finally { conn.disconnect() }
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(131072)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
    }
}
