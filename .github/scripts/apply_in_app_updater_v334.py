from pathlib import Path
import re


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing {label}")
    return text.replace(old, new, 1)


# Version metadata
build_file = Path("app/build.gradle")
build = build_file.read_text(encoding="utf-8")
build = re.sub(r"versionCode\s+\d+", "versionCode 334", build, count=1)
build = re.sub(r'versionName\s+"[^"]+"', 'versionName "8.6.2"', build, count=1)
build_file.write_text(build, encoding="utf-8")


# Harden the internal APK downloader. The manifest URL is checked again at the
# download boundary, partial files are never exposed to FileProvider, progress is
# delivered on the main dispatcher, and the downloaded archive must contain the
# launcher package with a newer version code.
checker_file = Path("app/src/main/java/com/drmacze/f16launcher/AppUpdateChecker.kt")
checker = checker_file.read_text(encoding="utf-8")

if "TRUSTED_APK_PREFIX" not in checker:
    checker = replace_once(
        checker,
        "    /** Any version gap triggers the non-dismissable website update flow. */\n"
        "    private const val FORCE_UPDATE_THRESHOLD = 1\n",
        "    /** Any version gap remains non-dismissable, but downloads stay in-app. */\n"
        "    private const val FORCE_UPDATE_THRESHOLD = 1\n"
        "    private const val TRUSTED_APK_PREFIX =\n"
        "        \"https://github.com/drmacze/DLavie-Launcher-Data/releases/download/\"\n",
        "trusted APK prefix",
    )

checker = checker.replace(
    "                apkUrl.startsWith(\n"
    "                    \"https://github.com/drmacze/DLavie-Launcher-Data/releases/download/\"\n"
    "                )\n",
    "                apkUrl.startsWith(TRUSTED_APK_PREFIX)\n",
    1,
)

new_download_block = r'''    /**
     * Download the official launcher APK into private cache with visible progress.
     * A `.part` file is used until the archive passes structural/package/version
     * validation, preventing an interrupted response from reaching the installer.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: ((Float) -> Unit)? = null,
    ): File? {
        require(apkUrl.startsWith(TRUSTED_APK_PREFIX)) {
            "Tautan pembaruan launcher tidak tepercaya"
        }

        val cacheDir = File(context.cacheDir, "app-updates").also { it.mkdirs() }
        val partialFile = File(cacheDir, "dlavie-update.apk.part")
        val finalFile = File(cacheDir, "dlavie-update.apk")
        partialFile.delete()
        finalFile.delete()

        var lastError: Throwable? = null
        val maxRetries = 3

        for (attempt in 1..maxRetries) {
            try {
                reportProgress(onProgress, 0f)
                android.util.Log.i("AppUpdate", "Download attempt $attempt/$maxRetries: $apkUrl")
                downloadApkAttempt(apkUrl, partialFile, onProgress)
                validateDownloadedApk(context, partialFile)

                if (!partialFile.renameTo(finalFile)) {
                    partialFile.copyTo(finalFile, overwrite = true)
                    partialFile.delete()
                }
                require(finalFile.isFile && finalFile.length() > 1_000_000L) {
                    "File pembaruan tidak lengkap"
                }
                reportProgress(onProgress, 1f)
                android.util.Log.i("AppUpdate", "Download verified: ${finalFile.length()} bytes")
                return finalFile
            } catch (error: Throwable) {
                lastError = error
                android.util.Log.w(
                    "AppUpdate",
                    "Download attempt $attempt/$maxRetries failed: ${error.message}",
                    error,
                )
                partialFile.delete()
                finalFile.delete()
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(1_500L * attempt)
                }
            }
        }

        throw IllegalStateException(
            lastError?.message ?: "Download gagal setelah $maxRetries percobaan",
            lastError,
        )
    }

    private suspend fun downloadApkAttempt(
        apkUrl: String,
        partialFile: File,
        onProgress: ((Float) -> Unit)?,
    ) {
        var currentUrl = apkUrl
        var redirectCount = 0
        var connection: HttpURLConnection? = null

        try {
            while (true) {
                val requestUrl = URL(currentUrl)
                connection = (requestUrl.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 30_000
                    readTimeout = 120_000
                    setRequestProperty("User-Agent", "DLavie-Launcher/${BuildConfig.VERSION_CODE} (Android)")
                    setRequestProperty(
                        "Accept",
                        "application/vnd.android.package-archive, application/octet-stream, */*",
                    )
                    setRequestProperty("Accept-Encoding", "identity")
                    connect()
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    connection = null
                    require(!location.isNullOrBlank()) {
                        "Redirect tanpa Location header (HTTP $responseCode)"
                    }
                    require(redirectCount < 5) { "Terlalu banyak redirect" }
                    currentUrl = URL(requestUrl, location).toString()
                    redirectCount += 1
                    continue
                }

                when (responseCode) {
                    403 -> throw IllegalStateException("Akses file pembaruan ditolak (HTTP 403)")
                    404 -> throw IllegalStateException("File pembaruan tidak ditemukan (HTTP 404)")
                }
                require(responseCode in 200..299) {
                    "Server pembaruan mengembalikan HTTP $responseCode"
                }

                val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
                val buffer = ByteArray(32 * 1024)
                var downloadedBytes = 0L
                var lastReportedPercent = -1

                connection.inputStream.use { input ->
                    partialFile.outputStream().buffered().use { output ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            downloadedBytes += count

                            if (totalBytes != null) {
                                val progress = (downloadedBytes.toDouble() / totalBytes.toDouble())
                                    .toFloat()
                                    .coerceIn(0f, 0.99f)
                                val percent = (progress * 100).toInt()
                                if (percent > lastReportedPercent) {
                                    lastReportedPercent = percent
                                    reportProgress(onProgress, progress)
                                }
                            }
                        }
                        output.flush()
                    }
                }

                if (totalBytes != null) {
                    require(downloadedBytes == totalBytes) {
                        "Download terputus: $downloadedBytes dari $totalBytes byte"
                    }
                }
                return
            }
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun reportProgress(
        callback: ((Float) -> Unit)?,
        value: Float,
    ) {
        if (callback == null) return
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
            callback(value.coerceIn(0f, 1f))
        }
    }

    @Suppress("DEPRECATION")
    private fun validateDownloadedApk(context: Context, apkFile: File) {
        require(apkFile.isFile && apkFile.length() > 1_000_000L) {
            "File pembaruan terlalu kecil atau tidak lengkap"
        }

        apkFile.inputStream().use { input ->
            val signature = ByteArray(4)
            require(input.read(signature) == signature.size) {
                "File pembaruan tidak dapat dibaca"
            }
            require(signature[0] == 0x50.toByte() && signature[1] == 0x4B.toByte()) {
                "File yang diterima bukan APK yang valid"
            }
        }

        val archive = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: throw IllegalStateException("Android tidak dapat membaca APK pembaruan")
        require(archive.packageName == context.packageName) {
            "APK pembaruan memiliki package yang tidak sesuai"
        }

        val archiveVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            archive.versionCode.toLong()
        }
        require(archiveVersion > BuildConfig.VERSION_CODE.toLong()) {
            "APK yang diunduh bukan versi yang lebih baru"
        }
    }

'''

if "private fun validateDownloadedApk" not in checker:
    pattern = re.compile(
        r"    /\*\* Download APK to cache with progress and retry handling\. \*/\n"
        r".*?"
        r"(?=    /\*\* Trigger APK installation through FileProvider\. \*/)",
        re.S,
    )
    checker, count = pattern.subn(new_download_block, checker, count=1)
    if count != 1:
        raise SystemExit("missing legacy APK download block")

old_install = r'''    /** Trigger APK installation through FileProvider. */
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            true
        } catch (e: Throwable) {
            android.util.Log.e("DLavie", "installApk: FileProvider failed", e)
            false
        }
    }
'''
new_install = r'''    /** Trigger the Android package installer for a verified cached APK. */
    @Suppress("DEPRECATION")
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                apkFile,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("DLavie update", uri)
            }

            context.packageManager.queryIntentActivities(intent, 0).forEach { target ->
                context.grantUriPermission(
                    target.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            context.startActivity(intent)
            true
        } catch (error: Throwable) {
            android.util.Log.e("DLavie", "installApk failed", error)
            false
        }
    }
'''
if "clipData = android.content.ClipData.newRawUri" not in checker:
    checker = replace_once(checker, old_install, new_install, "installer function")

checker_file.write_text(checker, encoding="utf-8")


# Route mandatory updates through the same in-launcher downloader and replace the
# old website-first dialog with a unified progress/error surface.
activity_file = Path("app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt")
activity = activity_file.read_text(encoding="utf-8")

new_on_update = r'''                        onUpdate = {
                            if (!updateDownloading) {
                                val currentInfo = updateInfo
                                if (currentInfo == null || currentInfo.apkUrl.isBlank()) {
                                    updateDownloadError =
                                        "Tautan APK resmi tidak tersedia. Periksa pembaruan lagi."
                                    return@AppUpdatePopup
                                }

                                updateDownloading = true
                                updateDownloadProgress = 0f
                                updateDownloadError = ""
                                updateScope.launch {
                                    try {
                                        val apkFile = withContext(Dispatchers.IO) {
                                            AppUpdateChecker.downloadApk(
                                                context = context,
                                                apkUrl = currentInfo.apkUrl,
                                                onProgress = { value ->
                                                    updateDownloadProgress = value
                                                },
                                            )
                                        }
                                        updateDownloading = false

                                        if (apkFile == null || !apkFile.exists()) {
                                            updateDownloadError =
                                                "Download belum selesai. Coba lagi dari launcher."
                                            return@launch
                                        }

                                        val installerOpened = AppUpdateChecker.installApk(context, apkFile)
                                        if (installerOpened) {
                                            showUpdatePopup = false
                                        } else {
                                            updateDownloadError =
                                                "Installer Android tidak dapat dibuka. Gunakan download manual di bawah."
                                        }
                                    } catch (error: Throwable) {
                                        updateDownloading = false
                                        updateDownloadProgress = 0f
                                        updateDownloadError = error.message
                                            ?: "Download gagal. Periksa koneksi lalu coba lagi."
                                    }
                                }
                            }
                        },
                        onLater = {'''

if "Tautan APK resmi tidak tersedia. Periksa pembaruan lagi." not in activity:
    on_update_pattern = re.compile(
        r"                        onUpdate = \{\n"
        r".*?"
        r"                        \},\n"
        r"                        onLater = \{",
        re.S,
    )
    activity, count = on_update_pattern.subn(new_on_update, activity, count=1)
    if count != 1:
        raise SystemExit("missing AppUpdatePopup onUpdate callback")

new_popup = r'''// ─── App Update Popup ─────────────────────────────────────────────────────────
@Composable
fun AppUpdatePopup(
    info: AppUpdateChecker.UpdateInfo,
    downloading: Boolean,
    progress: Float,
    error: String = "",
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onOpenWebsite: () -> Unit = {},
) {
    val forceUpdate = info.forceUpdate
    val progressPercent = (progress.coerceIn(0f, 1f) * 100f).toInt()

    AlertDialog(
        onDismissRequest = {
            if (!downloading && !forceUpdate) onLater()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (forceUpdate) Icons.Rounded.PriorityHigh else Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = if (forceUpdate) AmberWarn else Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (forceUpdate) "Pembaruan wajib" else "Pembaruan tersedia",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                if (forceUpdate) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = DangerRed.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            "WAJIB",
                            color = DangerRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    "Versi ${info.versionName} siap dipasang langsung dari launcher.",
                    color = SoftText,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Build ${info.currentVersionCode}  →  Build ${info.versionCode}",
                    color = SubText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )

                if (forceUpdate) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(DangerRed.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .border(1.dp, DangerRed.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "Versi ini sudah kedaluwarsa. Pembaruan harus dipasang untuk melanjutkan.",
                            color = DangerRed,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (info.apkSizeMb.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Surface2.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, GlassStroke, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            tint = CandyCyan,
                            modifier = Modifier.size(16.dp),
                        )
                        Column {
                            Text("Ukuran download", color = SubText, fontSize = 10.sp)
                            Text(
                                info.apkSizeMb,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text("Build ${info.versionCode}", color = SubText, fontSize = 11.sp)
                    }
                }

                val notes = info.releaseNotes.take(500)
                if (notes.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        notes,
                        color = SubText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 7,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CandyCyan.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, CandyCyan.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(
                        Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = CandyCyan,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Download langsung di launcher",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Setelah selesai, installer Android akan terbuka otomatis.",
                            color = SubText,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                        )
                    }
                }

                if (downloading) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Surface2,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (progressPercent >= 100) {
                                "Menyiapkan installer…"
                            } else {
                                "Mengunduh pembaruan…"
                            },
                            color = SoftText,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "$progressPercent%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (error.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(DangerRed.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, DangerRed.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(16.dp).padding(top = 1.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                error,
                                color = DangerRed,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                "Download manual dari website",
                                modifier = Modifier.clickable(onClick = onOpenWebsite),
                                color = CandyCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                enabled = !downloading,
                shape = TTShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (forceUpdate) DangerRed else Color.White,
                    contentColor = if (forceUpdate) Color.White else Carbon,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = if (forceUpdate) Color.White else Carbon,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Mengunduh $progressPercent%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (forceUpdate) "Unduh & perbarui sekarang" else "Perbarui dari launcher",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        dismissButton = {
            if (!forceUpdate) {
                TextButton(
                    onClick = onLater,
                    enabled = !downloading,
                ) {
                    Text("Nanti", color = SubText, fontSize = 13.sp)
                }
            }
        },
        containerColor = GlassBase,
    )
}

// ─── Helper functions ─────────────────────────────────────────────────────────
'''

if "Unduh & perbarui sekarang" not in activity:
    popup_pattern = re.compile(
        r"// ─── App Update Popup ─+\n"
        r"@Composable\n"
        r"fun AppUpdatePopup\(.*?"
        r"// ─── Helper functions ─+\n",
        re.S,
    )
    activity, count = popup_pattern.subn(new_popup, activity, count=1)
    if count != 1:
        raise SystemExit("missing legacy AppUpdatePopup function")

activity_file.write_text(activity, encoding="utf-8")
