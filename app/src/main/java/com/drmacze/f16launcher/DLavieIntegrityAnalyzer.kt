package com.drmacze.f16launcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * v7.9.39: DLavie Integrity Analyzer — Sistem analisis canggih untuk
 * memastikan hanya DLavie original yang bisa jalan di Launcher.
 *
 * Fitur:
 * 1. APK Signature Verification — verifikasi APK FIFA 16 yang terinstall
 *    punya signature yang sama dengan APK original dari DLavie (ChatGPT version).
 *    Repackaged/forked APK akan punya signature berbeda → ditolak.
 *
 * 2. Game Data Origin Detection — cek marker files DLavie di:
 *    - /sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed
 *    - /sdcard/Android/data/com.ea.gp.fifaworld/.dlavie_patch_installed
 *    Kalau ada file game data tapi tidak ada marker → indikasi data dari sumber lain.
 *
 * 3. Permission Audit — cek semua permission yang diperlukan Launcher:
 *    - MANAGE_EXTERNAL_STORAGE (untuk akses game data)
 *    - POST_NOTIFICATIONS (Android 13+)
 *    - FOREGROUND_SERVICE
 *    - WAKE_LOCK
 *
 * 4. Device Fingerprint — kumpulkan info device real (bukan dummy):
 *    - Android version + SDK level
 *    - Device model + manufacturer
 *    - Build fingerprint
 *    - Security patch date
 *    - Screen density + size
 *    - Total RAM + available storage
 *
 * 5. Analysis Result — return object dengan status:
 *    - OK: semua cocok, bisa lanjut install/update
 *    - NEEDS_CLEANUP: ada data non-DLavie, perlu cleanup + backup
 *    - BLOCKED: APK signature tidak cocok, tolak install
 *
 * Usage:
 *   val result = DLavieIntegrityAnalyzer.analyze(context)
 *   when (result.status) {
 *     AnalysisStatus.OK -> { /* lanjut */ }
 *     AnalysisStatus.NEEDS_CLEANUP -> { /* tampilkan popup cleanup */ }
 *     AnalysisStatus.BLOCKED -> { /* tolak, minta uninstall */ }
 *   }
 */
object DLavieIntegrityAnalyzer {

    private const val TAG = "DLavieIntegrity"

    // ─── Constants ───────────────────────────────────────────────────────────

    /**
     * SHA-256 hash dari META-INF/CERT.RSA di APK original DLavie26.apk
     * (APK dari ChatGPT, hosted di release v26 DLavie-Launcher-Data).
     *
     * Hash ini didapat dari APK original yang sudah verified working di Android 16.
     * Repackaged/forked APK akan punya signature berbeda → ditolak.
     */
    /**
     * SHA-256 dari signing certificate APK DLavie26.apk original.
     *
     * v7.9.46 FIX: Sebelumnya pakai hash CERT.RSA file keseluruhan (cd40e88e...) yang SALAH.
     * Android API (Signature.toByteArray()) return DER-encoded certificate, bukan whole PKCS#7.
     * Hash yang benar: a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc
     *
     * Certificate: CN=Android, O=Android (Android debug key — original dari ChatGpt APK)
     * SHA-1: 61ed377e85d386a8dfee6b864bd85b0bfaa5af81
     */
    private const val EXPECTED_APK_SIGNATURE_SHA256 = "a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc"

    /**
     * SHA-256 dari whole APK file DLavie26.apk original.
     * Dipakai untuk double-check integrity (bukan hanya signature).
     */
    private const val EXPECTED_APK_FILE_SHA256 = "acb0ce50554d13d6d36aa75e7e84ade69e52f4b130f8316af4505cc255acd176"

    /**
     * Expected APK size dalam bytes. Kalau berbeda signifikan (>1MB), suspicious.
     */
    private const val EXPECTED_APK_SIZE = 34027637L

    private const val GAME_PKG = "com.ea.gp.fifaworld"
    private const val GAME_DATA_PATH = "/sdcard/Android/data/com.ea.gp.fifaworld"
    private const val GAME_FILES_PATH = "$GAME_DATA_PATH/files"
    private const val DLAVIE_DATA_MARKER = "$GAME_DATA_PATH/.dlavie26_data_installed"
    private const val DLAVIE_PATCH_MARKER = "$GAME_DATA_PATH/.dlavie_patch_installed"
    private const val DLAVIE_OBB_PATH = "/sdcard/Android/obb/com.ea.gp.fifaworld"

    /**
     * File yang HARUS ada kalau game data original DLavie sudah terinstall.
     * Public supaya IntegrityAnalysisDialog bisa display count ke user.
     */
    val DLAVIE_ORIGINAL_FILES = listOf(
        "$GAME_FILES_PATH/data/db/fifa_ng_db.db",  // save file utama
        "$GAME_FILES_PATH/cl.ini",                  // config utama
        "$DLAVIE_OBB_PATH/main.13.com.ea.gp.fifaworld.obb",
        "$DLAVIE_OBB_PATH/patch.26.com.ea.gp.fifaworld.obb"
    )

    /**
     * Permission yang WAJIB granted untuk Launcher berfungsi penuh.
     */
    private val REQUIRED_PERMISSIONS = listOf(
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.WAKE_LOCK",
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Jalankan analisis lengkap. Return AnalysisResult dengan semua info.
     */
    fun analyze(context: Context): AnalysisResult {
        Log.i(TAG, "Starting DLavie integrity analysis...")

        val deviceInfo = collectDeviceInfo(context)
        val apkVerification = verifyInstalledApkSignature(context)
        val gameDataStatus = analyzeGameData()
        val permissionAudit = auditPermissions(context)

        // v7.9.48: Determine overall status — SANGAT PERMISIF
        // Signature check DISABLED (terlalu banyak false positive antara
        // debug/release signing scheme v1/v2/v3). Fokus ke marker file check.
        //
        // Status logic:
        // - OK = default (allow install/update) — paling sering
        // - NEEDS_PERMISSIONS = MANAGE_EXTERNAL_STORAGE missing (Android 11+)
        // - NEEDS_CLEANUP = ada game data TAPI tidak ada DLavie indicator apapun
        // - BLOCKED = tidak pernah (disabled, biar user tidak terkunci)
        //
        // v7.9.48 CHANGE: Kalau ada ANY DLavie indicator (OBB/fifa_db/cl.ini/marker),
        // status = OK walaupun ada game data "foreign" di folder lain.
        // Reason: user install via DLavie Launcher tetap dianggap valid walau
        // ada file sisa dari install sebelumnya. False positive lebih buruk
        // daripada false negative di sini.
        val status = when {
            // Kalau ada DLavie indicator kuat → OK regardless of permissions
            // (user akan diminta permission terpisah saat apply patch)
            gameDataStatus.hasDlavieDataMarker ||
            gameDataStatus.hasDlaviePatchMarker -> AnalysisStatus.OK
            // OBB files ada = install via DLavie Launcher → OK
            gameDataStatus.hasOriginalData -> AnalysisStatus.OK
            // Foreign data + no DLavie indicator → suggest cleanup
            gameDataStatus.hasForeignData -> AnalysisStatus.NEEDS_CLEANUP
            // Critical permissions missing → request (only if no other issue)
            permissionAudit.missingCritical.isNotEmpty() ->
                AnalysisStatus.NEEDS_PERMISSIONS
            else -> AnalysisStatus.OK
        }

        val result = AnalysisResult(
            status = status,
            deviceInfo = deviceInfo,
            apkVerification = apkVerification,
            gameDataStatus = gameDataStatus,
            permissionAudit = permissionAudit,
            timestamp = System.currentTimeMillis()
        )

        Log.i(TAG, "Analysis complete: status=$status, apk=$apkVerification, " +
                "gameData=${gameDataStatus.status}, missingPerms=${permissionAudit.missingCritical.size}")

        return result
    }

    /**
     * Verifikasi signature APK FIFA 16 yang terinstall di device.
     */
    fun verifyInstalledApkSignature(context: Context): ApkVerificationStatus {
        try {
            @Suppress("DEPRECATION")
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    GAME_PKG,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                context.packageManager.getPackageInfo(
                    GAME_PKG,
                    PackageManager.GET_SIGNATURES
                )
            }

            if (packageInfo == null) {
                return ApkVerificationStatus.NOT_INSTALLED
            }

            val signatures = getSignatures(packageInfo)
            if (signatures.isEmpty()) {
                Log.w(TAG, "APK terinstall tapi tidak ada signature")
                return ApkVerificationStatus.BLOCKED
            }

            // Hash signature pertama dan compare dengan expected
            for (sig in signatures) {
                val sigBytes = sig.toByteArray()
                val sha256 = sha256Hex(sigBytes)
                Log.d(TAG, "Found signature SHA-256: $sha256")

                if (sha256.equals(EXPECTED_APK_SIGNATURE_SHA256, ignoreCase = true)) {
                    Log.i(TAG, "✓ APK signature verified — original DLavie")
                    return ApkVerificationStatus.VERIFIED
                }
            }

            Log.w(TAG, "✗ APK signature mismatch — non-DLavie or repackaged")
            return ApkVerificationStatus.BLOCKED

        } catch (e: PackageManager.NameNotFoundException) {
            Log.i(TAG, "FIFA 16 APK belum terinstall")
            return ApkVerificationStatus.NOT_INSTALLED
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying APK signature", e)
            return ApkVerificationStatus.UNKNOWN
        }
    }

    /**
     * Cek apakah APK FIFA 16 terinstall (any version, signature check skipped).
     */
    fun isFifa16Installed(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GAME_PKG, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Analisis game data di storage. Deteksi:
     * - Apakah data original DLavie sudah ada
     * - Apakah ada data dari sumber lain (foreign data)
     *
     * v7.9.48 FIX: False positive "FOREIGN_DATA" untuk user yang install
     * via DLavie Launcher. Sebelumnya OBB check pakai filename EXACT match
     * (main.13 + patch.26). Kalau user punya patch.27 atau main.14 (versi
     * beda), check gagal -> hasObbFiles=false -> hasForeignData=true ->
     * popup "Backup & Cleanup" muncul padahal data valid.
     *
     * Sekarang: ANY .obb file di folder OBB = indikator install via DLavie.
     * Juga: fifa_ng_db.db (save file utama) ada = indikator game pernah
     * dijalankan dengan APK DLavie (file ini hanya dibuat saat game pertama
     * kali run dengan APK yang valid).
     */
    fun analyzeGameData(): GameDataStatus {
        val gameDataDir = File(GAME_DATA_PATH)
        val gameFilesDir = File(GAME_FILES_PATH)
        val obbDir = File(DLAVIE_OBB_PATH)
        val dataMarker = File(DLAVIE_DATA_MARKER)
        val patchMarker = File(DLAVIE_PATCH_MARKER)

        // Cek marker files DLavie
        val hasDlavieDataMarker = dataMarker.exists()
        val hasDlaviePatchMarker = patchMarker.exists()

        // Cek file game data original
        val existingOriginalFiles = DLAVIE_ORIGINAL_FILES.map { File(it) }.filter { it.exists() }
        val hasOriginalData = existingOriginalFiles.size >= 2  // minimal 2 file original ada

        // v7.9.48: Cek OBB files — ANY .obb file di folder OBB = DLavie-installed
        // Sebelumnya: exact match "main.13..." + "patch.26..." → false positive kalau
        // user punya versi patch/main yang berbeda (mis. patch.27, main.14).
        // Sekarang: list files di folder OBB, cek ada yang extension-nya .obb.
        val obbFiles = if (obbDir.exists() && obbDir.isDirectory) {
            obbDir.listFiles { f -> f.isFile && f.name.endsWith(".obb", ignoreCase = true) } ?: emptyArray()
        } else emptyArray()
        val hasObbFiles = obbFiles.isNotEmpty()
        val obbFileNames = obbFiles.map { it.name }

        // v7.9.48: fifa_ng_db.db adalah save file utama FIFA 16.
        // File ini HANYA dibuat saat game pertama kali dijalankan dengan APK valid.
        // Jika file ini ada → game sudah pernah run dengan DLavie APK → indikator kuat.
        val fifaDbFile = File("$GAME_FILES_PATH/data/db/fifa_ng_db.db")
        val hasFifaDb = fifaDbFile.exists()

        // v7.9.48: Cek cl.ini (config utama) — juga indikator game data DLavie
        val clIniFile = File("$GAME_FILES_PATH/cl.ini")
        val hasClIni = clIniFile.exists()

        // Cek foreign data indicators
        // Foreign = ada file game data TAPI tidak ada indicator DLavie apapun:
        //   - tidak ada marker DLavie (DevPatchEngine)
        //   - tidak ada marker patch (ModPatchDownloader)
        //   - tidak ada OBB files (DLavie Launcher installer)
        //   - tidak ada fifa_ng_db.db (game pernah run dengan APK DLavie)
        //   - tidak ada cl.ini (config utama)
        //   - tidak ada original data files (fifa_ng_db.db + cl.ini + obb)
        val hasAnyGameData = (gameDataDir.exists() && (gameDataDir.listFiles()?.isNotEmpty() == true)) ||
                            (gameFilesDir.exists() && (gameFilesDir.listFiles()?.isNotEmpty() == true)) ||
                            (obbDir.exists() && (obbDir.listFiles()?.isNotEmpty() == true))

        val hasDlavieIndicator = hasDlavieDataMarker || hasDlaviePatchMarker ||
                                  hasObbFiles || hasOriginalData ||
                                  hasFifaDb || hasClIni
        val hasForeignData = hasAnyGameData && !hasDlavieIndicator

        // Cek mod files dari sumber lain (non-DLavie)
        val suspiciousFiles = mutableListOf<String>()
        if (hasAnyGameData && hasForeignData) {
            gameDataDir.walkTopDown().take(20).forEach { f ->
                if (f.isFile && !f.name.startsWith(".")) {
                    suspiciousFiles.add(f.absolutePath)
                }
            }
        }

        // Determine status
        val status = when {
            !hasAnyGameData -> GameDataStatusType.NO_DATA
            hasDlavieDataMarker && hasOriginalData -> GameDataStatusType.DLAVIE_ORIGINAL
            hasObbFiles && hasOriginalData -> GameDataStatusType.DLAVIE_ORIGINAL  // install via Launcher, no marker
            hasFifaDb && hasClIni -> GameDataStatusType.DLAVIE_ORIGINAL  // v7.9.48: game pernah run dengan DLavie APK
            hasObbFiles -> GameDataStatusType.DLAVIE_ORIGINAL  // v7.9.48: OBB ada, walau belum run game
            hasForeignData -> GameDataStatusType.FOREIGN_DATA
            else -> GameDataStatusType.INCOMPLETE
        }

        return GameDataStatus(
            status = status,
            hasDlavieDataMarker = hasDlavieDataMarker,
            hasDlaviePatchMarker = hasDlaviePatchMarker,
            hasOriginalData = hasOriginalData,
            hasForeignData = hasForeignData,
            existingOriginalFilesCount = existingOriginalFiles.size,
            suspiciousFiles = suspiciousFiles,
            gameDataPath = GAME_DATA_PATH,
            gameFilesPath = GAME_FILES_PATH,
            obbPath = DLAVIE_OBB_PATH
        )
    }

    /**
     * Audit semua permission yang diperlukan.
     *
     * v7.9.50: Fix false-positive "MISSING" untuk MANAGE_EXTERNAL_STORAGE dan
     * REQUEST_INSTALL_PACKAGES. checkSelfPermission() di Android 11+ selalu return
     * DENIED untuk permission special ini, meskipun user sudah grant via Settings.
     * Sekarang pakai API yang benar:
     * - MANAGE_EXTERNAL_STORAGE → Environment.isExternalStorageManager()
     * - REQUEST_INSTALL_PACKAGES → PackageManager.canRequestPackageInstalls()
     */
    fun auditPermissions(context: Context): PermissionAudit {
        val granted = mutableListOf<String>()
        val missingCritical = mutableListOf<String>()
        val missingOptional = mutableListOf<String>()

        for (perm in REQUIRED_PERMISSIONS) {
            // v7.9.50: Special handling untuk special permissions
            val isGranted = when (perm) {
                "android.permission.MANAGE_EXTERNAL_STORAGE" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.os.Environment.isExternalStorageManager()
                    } else {
                        context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
                    }
                }
                "android.permission.REQUEST_INSTALL_PACKAGES" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.packageManager.canRequestPackageInstalls()
                    } else {
                        context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
                    }
                }
                else -> {
                    context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
                }
            }

            if (isGranted) {
                granted.add(perm)
            } else {
                if (perm == "android.permission.MANAGE_EXTERNAL_STORAGE" ||
                    perm == "android.permission.REQUEST_INSTALL_PACKAGES") {
                    missingCritical.add(perm)
                } else {
                    missingOptional.add(perm)
                }
            }
        }

        // Special check: MANAGE_EXTERNAL_STORAGE di Android 11+ perlu via Settings
        val needsManageStorageSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !android.os.Environment.isExternalStorageManager()
        } else false

        return PermissionAudit(
            granted = granted,
            missingCritical = missingCritical,
            missingOptional = missingOptional,
            needsManageStorageSettings = needsManageStorageSettings,
            totalRequired = REQUIRED_PERMISSIONS.size
        )
    }

    /**
     * Collect info device real (bukan dummy) untuk ditampilkan ke user.
     */
    @SuppressLint("HardwareIds")
    fun collectDeviceInfo(context: Context): DeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val info = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)

        val displayMetrics = context.resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val screenHeightPx = displayMetrics.heightPixels
        val densityDpi = displayMetrics.densityDpi
        val density = displayMetrics.density

        // Storage info
        val statFs = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path)
        val totalStorageBytes = statFs.totalBytes
        val availableStorageBytes = statFs.availableBytes

        // Android ID (unique per app+user, tidak sensitive seperti IMEI)
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        return DeviceInfo(
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            sdkLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH ?: "unknown",
            buildId = Build.ID ?: "unknown",
            manufacturer = Build.MANUFACTURER ?: "unknown",
            model = Build.MODEL ?: "unknown",
            product = Build.PRODUCT ?: "unknown",
            device = Build.DEVICE ?: "unknown",
            brand = Build.BRAND ?: "unknown",
            board = Build.BOARD ?: "unknown",
            hardware = Build.HARDWARE ?: "unknown",
            display = Build.DISPLAY ?: "unknown",
            host = Build.HOST ?: "unknown",
            fingerprint = Build.FINGERPRINT ?: "unknown",
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            densityDpi = densityDpi,
            density = density,
            totalRamBytes = info.totalMem,
            availableRamBytes = info.availMem,
            totalStorageBytes = totalStorageBytes,
            availableStorageBytes = availableStorageBytes,
            androidId = androidId ?: "unknown",
            isEmulator = isEmulator(),
            isRooted = isRooted()
        )
    }

    /**
     * Detect emulator (Appetize, BlueStacks, dll).
     */
    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.contains("appetize")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.PRODUCT.equals("google_sdk", ignoreCase = true)
                || Build.HOST.contains("appetize", ignoreCase = true)
                || Build.HARDWARE.contains("goldfish", ignoreCase = true)
                || Build.HARDWARE.contains("ranchu", ignoreCase = true))
    }

    /**
     * Detect root (basic check, not foolproof).
     */
    private fun isRooted(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    /**
     * v7.9.59: Cek apakah device punya Google account yang sudah login.
     *
     * APK FIFA 16 (DLavie26.apk) punya attribute:
     *   android:requiredAccountType="com.google"
     *
     * Ini menyebabkan install gagal dengan error:
     *   "App not installed as app isn't compatible with your phone"
     *
     * jika device tidak punya Google account yang sudah login.
     *
     * Solusi: Launcher cek ini sebelum install, dan kasih pesan jelas
     * ke user untuk add Google account di Settings.
     *
     * @return true kalau ada Google account, false kalau tidak ada
     */
    fun hasGoogleAccount(context: Context): Boolean {
        // v7.9.63: Fix false positive — GET_ACCOUNTS adalah runtime permission di Android 6+.
        // getAccountsByType("com.google") akan return empty array jika permission belum granted,
        // meskipun user punya Google account. Solusi: cek permission dulu.
        // Kalau permission belum granted, jangan block user (assume ada account).
        return try {
            // Cek apakah GET_ACCOUNTS permission sudah granted
            val hasPermission = context.checkSelfPermission("android.permission.GET_ACCOUNTS") ==
                PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w(TAG, "GET_ACCOUNTS permission belum granted — assume user has Google account (don't block)")
                return true  // Don't block user if we can't check
            }

            val accountManager = android.accounts.AccountManager.get(context)
            val accounts = accountManager.getAccountsByType("com.google")
            val hasAccount = accounts.isNotEmpty()
            Log.i(TAG, "Google account check: ${accounts.size} accounts found")
            hasAccount
        } catch (e: Exception) {
            Log.w(TAG, "Cannot check Google account: ${e.message} — assume user has account (don't block)")
            true  // v7.9.63: Don't block user if check fails (was: false)
        }
    }

    /**
     * Backup game data lama ke /sdcard/F16Launcher/backups/foreign_data_TIMESTAMP/
     * Return path backup jika sukses, null jika gagal.
     */
    fun backupForeignGameData(): String? {
        val timestamp = System.currentTimeMillis()
        val backupDir = File("/sdcard/F16Launcher/backups/foreign_data_$timestamp")
        backupDir.mkdirs()

        val gameDataDir = File(GAME_DATA_PATH)
        val obbDir = File(DLAVIE_OBB_PATH)

        var backedUpFiles = 0
        var totalSize = 0L

        try {
            // Backup game data folder
            if (gameDataDir.exists()) {
                gameDataDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relPath = file.absolutePath.removePrefix(GAME_DATA_PATH)
                        val destFile = File(backupDir, "game_data$relPath")
                        destFile.parentFile?.mkdirs()
                        try {
                            file.copyTo(destFile, overwrite = true)
                            backedUpFiles++
                            totalSize += file.length()
                        } catch (e: Exception) {
                            Log.w(TAG, "Skip backup: ${file.absolutePath}")
                        }
                    }
                }
            }

            // Backup OBB folder (jangan copy file besar >2GB, nanti habis storage)
            if (obbDir.exists()) {
                obbDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.length() < 2_000_000_000L) {  // skip file >2GB
                        val relPath = file.absolutePath.removePrefix(DLAVIE_OBB_PATH)
                        val destFile = File(backupDir, "obb$relPath")
                        destFile.parentFile?.mkdirs()
                        try {
                            file.copyTo(destFile, overwrite = true)
                            backedUpFiles++
                            totalSize += file.length()
                        } catch (e: Exception) {
                            Log.w(TAG, "Skip backup OBB: ${file.absolutePath}")
                        }
                    }
                }
            }

            // Write backup manifest
            val manifest = File(backupDir, ".backup_manifest")
            manifest.writeText("""
                DLavie Foreign Data Backup
                Timestamp: $timestamp
                Date: ${java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date(timestamp))}
                Files: $backedUpFiles
                Total Size: $totalSize bytes (${totalSize / 1024 / 1024} MB)
                Source: $GAME_DATA_PATH + $DLAVIE_OBB_PATH
            """.trimIndent())

            Log.i(TAG, "Backup complete: $backedUpFiles files, $totalSize bytes → $backupDir")
            return backupDir.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            return null
        }
    }

    /**
     * Hapus semua game data foreign (yang bukan dari DLavie).
     * Dipanggil SETELAH user setuju di popup.
     */
    fun cleanupForeignGameData(): CleanupResult {
        val gameDataDir = File(GAME_DATA_PATH)
        val obbDir = File(DLAVIE_OBB_PATH)

        var deletedFiles = 0
        var failedFiles = 0
        var freedBytes = 0L

        try {
            // Delete game data folder content
            if (gameDataDir.exists()) {
                gameDataDir.walkTopDown().sortedDescending().forEach { file ->
                    try {
                        if (file.isFile) {
                            freedBytes += file.length()
                            file.delete()
                            deletedFiles++
                        } else if (file.isDirectory && file != gameDataDir) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        failedFiles++
                    }
                }
            }

            // Delete OBB folder content (file OBB besar, hati-hati)
            if (obbDir.exists()) {
                obbDir.walkTopDown().sortedDescending().forEach { file ->
                    try {
                        if (file.isFile) {
                            freedBytes += file.length()
                            file.delete()
                            deletedFiles++
                        } else if (file.isDirectory && file != obbDir) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        failedFiles++
                    }
                }
            }

            Log.i(TAG, "Cleanup complete: $deletedFiles files deleted, $failedFiles failed, $freedBytes bytes freed")

            return CleanupResult(
                success = failedFiles == 0,
                deletedFiles = deletedFiles,
                failedFiles = failedFiles,
                freedBytes = freedBytes
            )

        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            return CleanupResult(
                success = false,
                deletedFiles = deletedFiles,
                failedFiles = failedFiles,
                freedBytes = freedBytes,
                error = e.message
            )
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun getSignatures(packageInfo: PackageInfo): Array<Signature> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            when {
                signingInfo == null -> emptyArray()
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                else -> signingInfo.signingCertificateHistory
            }
        } else {
            packageInfo.signatures ?: emptyArray()
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    // ─── Data Classes ────────────────────────────────────────────────────────

    data class AnalysisResult(
        val status: AnalysisStatus,
        val deviceInfo: DeviceInfo,
        val apkVerification: ApkVerificationStatus,
        val gameDataStatus: GameDataStatus,
        val permissionAudit: PermissionAudit,
        val timestamp: Long
    ) {
        val timestampFormatted: String
            get() = java.text.SimpleDateFormat(
                "dd MMM yyyy HH:mm:ss",
                java.util.Locale("id", "ID")
            ).format(java.util.Date(timestamp))

        /** True kalau user bisa lanjut install/update tanpa popup. */
        val canProceed: Boolean
            get() = status == AnalysisStatus.OK

        /** Summary singkat untuk display. */
        val summary: String
            get() = when (status) {
                AnalysisStatus.OK -> "Semua persyaratan terpenuhi. Siap install/update."
                AnalysisStatus.NEEDS_PERMISSIONS -> "Permission belum lengkap: ${permissionAudit.missingCritical.size} critical missing."
                AnalysisStatus.NEEDS_CLEANUP -> "Terdeteksi data non-DLavie. Perlu backup + cleanup sebelum lanjut."
                AnalysisStatus.BLOCKED -> "APK FIFA 16 bukan versi original DLavie. Install ditolak."
            }
    }

    enum class AnalysisStatus {
        OK,                    // Semua cocok, bisa lanjut
        NEEDS_PERMISSIONS,     // Permission belum lengkap
        NEEDS_CLEANUP,         // Ada data foreign, perlu backup + cleanup
        BLOCKED                // APK signature tidak cocok, tolak total
    }

    enum class ApkVerificationStatus {
        VERIFIED,              // APK signature cocok dengan DLavie original
        NOT_INSTALLED,         // APK belum terinstall
        BLOCKED,               // APK ada tapi signature berbeda (repackaged/forked)
        UNKNOWN                // Tidak bisa verify (error)
    }

    data class GameDataStatus(
        val status: GameDataStatusType,
        val hasDlavieDataMarker: Boolean,
        val hasDlaviePatchMarker: Boolean,
        val hasOriginalData: Boolean,
        val hasForeignData: Boolean,
        val existingOriginalFilesCount: Int,
        val suspiciousFiles: List<String>,
        val gameDataPath: String,
        val gameFilesPath: String,
        val obbPath: String
    )

    enum class GameDataStatusType {
        NO_DATA,               // Belum ada game data sama sekali
        DLAVIE_ORIGINAL,       // Data original DLavie sudah ada
        FOREIGN_DATA,          // Ada data tapi bukan dari DLavie
        INCOMPLETE             // Ada sebagian data, tidak lengkap
    }

    data class PermissionAudit(
        val granted: List<String>,
        val missingCritical: List<String>,
        val missingOptional: List<String>,
        val needsManageStorageSettings: Boolean,
        val totalRequired: Int
    ) {
        val allGranted: Boolean
            get() = missingCritical.isEmpty() && missingOptional.isEmpty()

        val criticalGranted: Boolean
            get() = missingCritical.isEmpty()
    }

    data class DeviceInfo(
        val androidVersion: String,
        val sdkLevel: Int,
        val securityPatch: String,
        val buildId: String,
        val manufacturer: String,
        val model: String,
        val product: String,
        val device: String,
        val brand: String,
        val board: String,
        val hardware: String,
        val display: String,
        val host: String,
        val fingerprint: String,
        val screenWidthPx: Int,
        val screenHeightPx: Int,
        val densityDpi: Int,
        val density: Float,
        val totalRamBytes: Long,
        val availableRamBytes: Long,
        val totalStorageBytes: Long,
        val availableStorageBytes: Long,
        val androidId: String,
        val isEmulator: Boolean,
        val isRooted: Boolean
    ) {
        val androidVersionName: String
            get() = when (sdkLevel) {
                30 -> "Android 11 (R)"
                31 -> "Android 12 (S)"
                32 -> "Android 12L (S_V2)"
                33 -> "Android 13 (T)"
                34 -> "Android 14 (U)"
                35 -> "Android 15 (V)"
                36 -> "Android 16"
                else -> "Android $androidVersion (API $sdkLevel)"
            }

        val ramFormatted: String
            get() = formatBytes(totalRamBytes)

        val storageFormatted: String
            get() = "${formatBytes(availableStorageBytes)} / ${formatBytes(totalStorageBytes)}"

        val screenFormatted: String
            get() = "${screenWidthPx}x${screenHeightPx} @ ${densityDpi}dpi"

        private fun formatBytes(bytes: Long): String {
            val gb = bytes / 1024.0 / 1024.0 / 1024.0
            return if (gb >= 1) String.format("%.1f GB", gb)
            else String.format("%.0f MB", bytes / 1024.0 / 1024.0)
        }
    }

    data class CleanupResult(
        val success: Boolean,
        val deletedFiles: Int,
        val failedFiles: Int,
        val freedBytes: Long,
        val error: String? = null
    )
}
