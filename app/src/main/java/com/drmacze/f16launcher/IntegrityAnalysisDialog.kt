package com.drmacze.f16launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v7.9.39: IntegrityAnalysisDialog — Popup analisis DLavie yang muncul saat:
 * - User klik Install FIFA 16 di GameHub
 * - User klik Update DLC mod
 * - User klik Play game
 *
 * Popup akan:
 * 1. Tampilkan hasil analisis real (device info, APK signature, game data, permissions)
 * 2. Jika ada masalah → tampilkan detail + rekomendasi
 * 3. Jika perlu cleanup → tampilkan checkbox agreement + backup option
 * 4. User harus centang setuju sebelum bisa Continue
 * 5. Continue → trigger cleanup + lanjut install/update
 *
 * Semua data yang ditampilkan REAL dari device, bukan dummy.
 * User bisa verifikasi sendiri bahwa sistem benar-benar jalan.
 */
@Composable
fun IntegrityAnalysisDialog(
    result: DLavieIntegrityAnalyzer.AnalysisResult,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    onRequestPermission: () -> Unit = {},
    onInstallGame: () -> Unit = {},
    onInstallApk: () -> Unit = {}
) {
    var agreeCleanup by remember { mutableStateOf(false) }
    var agreeBackup by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var processLog by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isProcessing,
            dismissOnClickOutside = !isProcessing,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1117)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2D3A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── Header ───
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = statusColor(result.status),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "DLavie Play Protect",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            "Cek real-time • ${result.timestampFormatted}",
                            color = Color(0xFF6B7280),
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                // ─── Status Summary ───
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = statusColor(result.status).copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor(result.status).copy(alpha = 0.3f))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                statusIcon(result.status),
                                contentDescription = null,
                                tint = statusColor(result.status),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                statusTitle(result.status),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            result.summary,
                            color = Color(0xFFB8BCC8),
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            lineHeight = 16.sp
                        )
                    }
                }

                // ─── Device Info (real, bukan dummy) ───
                SectionHeader("Informasi Perangkat", Icons.Rounded.Smartphone)
                val device = result.deviceInfo
                InfoGrid(
                    items = listOf(
                        "Android" to device.androidVersionName,
                        "Security Patch" to device.securityPatch,
                        "Build ID" to device.buildId,
                        "Manufacturer" to device.manufacturer,
                        "Model" to device.model,
                        "Brand" to device.brand,
                        "Hardware" to device.hardware,
                        "Screen" to device.screenFormatted,
                        "RAM Total" to device.ramFormatted,
                        "Storage" to device.storageFormatted,
                        "Android ID" to device.androidId.take(16) + "…",
                        "Environment" to when {
                            device.isEmulator -> "Emulator/Sandbox"
                            device.isRooted -> "Rooted Device"
                            else -> "Physical Device"
                        }
                    )
                )

                // ─── APK Signature Verification ───
                SectionHeader("Verifikasi APK FIFA 16", Icons.Rounded.Security)
                VerificationCard(
                    title = "Signature Status",
                    status = result.apkVerification,
                    details = when (result.apkVerification) {
                        DLavieIntegrityAnalyzer.ApkVerificationStatus.VERIFIED ->
                            "✓ Signature cocok dengan APK original DLavie (SHA-256: ${DLavieIntegrityAnalyzer.run { "cd40e88e..." }})\n" +
                            "APK ini resmi dari DLavie, aman untuk install."
                        DLavieIntegrityAnalyzer.ApkVerificationStatus.NOT_INSTALLED ->
                            "APK FIFA 16 belum terinstall. Install dari DLavie Launcher untuk mulai."
                        DLavieIntegrityAnalyzer.ApkVerificationStatus.BLOCKED ->
                            "✗ Signature TIDAK cocok!\n" +
                            "APK yang terinstall bukan versi original DLavie.\n" +
                            "Kemungkinan APK dimodifikasi/repackaged oleh pihak lain.\n" +
                            "Install ditolak demi keamanan data Anda."
                        DLavieIntegrityAnalyzer.ApkVerificationStatus.UNKNOWN ->
                            "Tidak bisa memverifikasi signature APK (error). Coba lagi."
                    }
                )
                // v7.9.47: Button install APK jika NOT_INSTALLED
                if (result.apkVerification == DLavieIntegrityAnalyzer.ApkVerificationStatus.NOT_INSTALLED) {
                    Button(
                        onClick = { onInstallApk() },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.Black
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Install APK FIFA 16 Sekarang", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }

                // ─── Game Data Status ───
                SectionHeader("Status Game Data", Icons.Rounded.Folder)
                val gameData = result.gameDataStatus
                VerificationCard(
                    title = "Data Origin",
                    status = gameData.status,
                    details = when (gameData.status) {
                        DLavieIntegrityAnalyzer.GameDataStatusType.NO_DATA ->
                            "Belum ada game data. Aman untuk install fresh dari DLavie."
                        DLavieIntegrityAnalyzer.GameDataStatusType.DLAVIE_ORIGINAL ->
                            "✓ Data original DLavie terdeteksi (marker files ada).\n" +
                            "Original files: ${gameData.existingOriginalFilesCount}/${DLavieIntegrityAnalyzer.DLAVIE_ORIGINAL_FILES.size}\n" +
                            "Safe untuk lanjut."
                        DLavieIntegrityAnalyzer.GameDataStatusType.FOREIGN_DATA ->
                            "⚠ Terdeteksi data dari sumber lain (non-DLavie)!\n" +
                            "Folder game data ada tapi tidak ada marker DLavie.\n" +
                            "Data ini bisa korup/konflik dengan DLavie.\n" +
                            "Backup + cleanup direkomendasikan sebelum install."
                        DLavieIntegrityAnalyzer.GameDataStatusType.INCOMPLETE ->
                            "Data game ada tapi tidak lengkap.\n" +
                            "Original files: ${gameData.existingOriginalFilesCount}/${DLavieIntegrityAnalyzer.DLAVIE_ORIGINAL_FILES.size}\n" +
                            "Sebaiknya reinstall data DLavie."
                    }
                )
                // v7.9.47: Button install game data jika NO_DATA atau INCOMPLETE
                if (gameData.status == DLavieIntegrityAnalyzer.GameDataStatusType.NO_DATA ||
                    gameData.status == DLavieIntegrityAnalyzer.GameDataStatusType.INCOMPLETE) {
                    Button(
                        onClick = { onInstallGame() },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.Black
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Install Game Data Sekarang", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }

                if (gameData.suspiciousFiles.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "File terdeteksi (${gameData.suspiciousFiles.size}):",
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    gameData.suspiciousFiles.take(5).forEach { path ->
                        Text(
                            "• $path",
                            color = Color(0xFF6B7280),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (gameData.suspiciousFiles.size > 5) {
                        Text(
                            "... dan ${gameData.suspiciousFiles.size - 5} file lainnya",
                            color = Color(0xFF6B7280),
                            fontSize = 9.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                // ─── Permission Audit ───
                SectionHeader("Audit Permission", Icons.Rounded.AdminPanelSettings)
                val perms = result.permissionAudit
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PermissionRow("MANAGE_EXTERNAL_STORAGE", !perms.missingCritical.contains("android.permission.MANAGE_EXTERNAL_STORAGE"), critical = true)
                    PermissionRow("REQUEST_INSTALL_PACKAGES", !perms.missingCritical.contains("android.permission.REQUEST_INSTALL_PACKAGES"), critical = true)
                    PermissionRow("POST_NOTIFICATIONS", !perms.missingOptional.contains("android.permission.POST_NOTIFICATIONS"), critical = false)
                    PermissionRow("WAKE_LOCK", !perms.missingOptional.contains("android.permission.WAKE_LOCK"), critical = false)
                    PermissionRow("INTERNET", !perms.missingOptional.contains("android.permission.INTERNET"), critical = false)
                    PermissionRow("FOREGROUND_SERVICE", !perms.missingOptional.contains("android.permission.FOREGROUND_SERVICE"), critical = false)
                }

                // v7.9.48: Button "Buka Settings" — SELALU muncul kalau ada permission critical missing
                // MANAGE_EXTERNAL_STORAGE + REQUEST_INSTALL_PACKAGES tidak bisa di-request via runtime,
                // HARUS buka Settings Android. Button ini langsung buka Settings.
                val hasCriticalMissing = perms.missingCritical.isNotEmpty()
                if (hasCriticalMissing) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFB74D).copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "⚠ Permission penting belum di-grant. Tap button di bawah untuk buka Settings Android dan enable:",
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                fontFamily = InterFontFamily,
                                lineHeight = 14.sp
                            )
                            // Button 1: Buka Settings All Files Access (untuk MANAGE_EXTERNAL_STORAGE)
                            if (perms.missingCritical.contains("android.permission.MANAGE_EXTERNAL_STORAGE")) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                android.widget.Toast.makeText(context, "Buka Settings > Apps > DLavie > Storage", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFB74D),
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Buka Settings Storage", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                        Text("Enable 'Allow access to manage all files'", fontSize = 9.sp, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                            // Button 2: Buka Settings Install Unknown Apps (untuk REQUEST_INSTALL_PACKAGES)
                            if (perms.missingCritical.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback: buka general unknown sources settings
                                            try {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply {
                                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                                android.widget.Toast.makeText(context, "Cari 'Install unknown apps' > DLavie > Allow", android.widget.Toast.LENGTH_LONG).show()
                                            } catch (_: Exception) {
                                                android.widget.Toast.makeText(context, "Buka Settings > Security > Install unknown apps > DLavie", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFB74D),
                                        contentColor = Color.Black
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Buka Settings Install Apps", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                        Text("Allow 'Install unknown apps' untuk DLavie", fontSize = 9.sp, fontFamily = InterFontFamily)
                                    }
                                }
                            }
                            Text(
                                "Setelah grant, kembali ke Launcher dan tap 'Re-Check' untuk refresh status.",
                                color = Color(0xFFB8BCC8),
                                fontSize = 9.sp,
                                fontFamily = InterFontFamily,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                // v7.9.48: Button request runtime permissions (POST_NOTIFICATIONS, WAKE_LOCK, dll)
                // Hanya muncul kalau ada optional permission missing DAN tidak ada critical missing
                if (perms.missingOptional.isNotEmpty() && !hasCriticalMissing) {
                    Button(
                        onClick = { onRequestPermission() },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.Black
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Grant Permissions Lainnya", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }

                // ─── Cleanup Agreement (only if NEEDS_CLEANUP) ───
                AnimatedVisibility(
                    visible = result.status == DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader("Cleanup Agreement", Icons.Rounded.CleaningServices)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF5252).copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f))
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Tindakan yang akan dilakukan:",
                                    color = Color(0xFFFF8A80),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                )
                                Text(
                                    "1. Backup data lama ke /sdcard/F16Launcher/backups/\n" +
                                    "2. Hapus semua file di Android/data/com.ea.gp.fifaworld/\n" +
                                    "3. Hapus OBB files di Android/obb/com.ea.gp.fifaworld/\n" +
                                    "4. Lanjut install data DLavie original",
                                    color = Color(0xFFB8BCC8),
                                    fontSize = 10.sp,
                                    fontFamily = InterFontFamily,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // Checkbox: agree to backup
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Checkbox(
                                checked = agreeBackup,
                                onCheckedChange = { agreeBackup = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
                            )
                            Text(
                                "Saya ingin backup data lama dulu (rekomendasi)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily
                            )
                        }

                        // Checkbox: agree to delete
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Checkbox(
                                checked = agreeCleanup,
                                onCheckedChange = { agreeCleanup = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF5252))
                            )
                            Text(
                                "Saya setuju menghapus data lama dan lanjut install DLavie original",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }

                // ─── Blocked Message (only if BLOCKED) ───
                AnimatedVisibility(
                    visible = result.status == DLavieIntegrityAnalyzer.AnalysisStatus.BLOCKED,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF5252).copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "🚫 INSTALL DITOLAK",
                                color = Color(0xFFFF5252),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                            Text(
                                "APK FIFA 16 yang terinstall di device ini BUKAN versi original DLavie.\n\n" +
                                "Untuk menggunakan DLavie Launcher, Anda harus:\n" +
                                "1. Uninstall APK FIFA 16 yang sekarang\n" +
                                "2. Install ulang APK original dari DLavie Launcher (GameHub > Install)\n" +
                                "3. Install ulang game data dari DLavie\n\n" +
                                "DLavie Launcher hanya kompatibel dengan versi original untuk mencegah konflik dan korupsi data.",
                                color = Color(0xFFB8BCC8),
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // ─── Processing Log (when backup/cleanup running) ───
                AnimatedVisibility(visible = isProcessing) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.3f)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Processing...",
                                color = Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                processLog,
                                color = Color(0xFFB8BCC8),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ─── Action Buttons (v7.9.48: 2 baris supaya teks tidak terpotong) ───
                // Baris 1: Cancel + Re-Check (50/50)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = { if (!isProcessing) onCancel() },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFB8BCC8)
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Batal", fontFamily = InterFontFamily, fontSize = 12.sp)
                    }

                    // Re-Check button — re-run analysis setelah user install/grant permission
                    OutlinedButton(
                        onClick = { if (!isProcessing) onDismiss() },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Re-Check", fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Baris 2: Continue (full-width)
                Spacer(Modifier.height(8.dp))
                val canContinue = when (result.status) {
                    DLavieIntegrityAnalyzer.AnalysisStatus.OK -> true
                    DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_PERMISSIONS -> false
                    DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP -> agreeCleanup && !isProcessing
                    DLavieIntegrityAnalyzer.AnalysisStatus.BLOCKED -> false
                }

                Button(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            processLog = ""

                            // Jika perlu backup + cleanup, kerjakan dulu
                            if (result.status == DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP) {
                                if (agreeBackup) {
                                    processLog += "[1/3] Backup data lama...\n"
                                    val backupPath = withContext(Dispatchers.IO) {
                                        DLavieIntegrityAnalyzer.backupForeignGameData()
                                    }
                                    processLog += if (backupPath != null) {
                                        "[1/3] ✓ Backup selesai: $backupPath\n"
                                    } else {
                                        "[1/3] ⚠ Backup gagal, lanjut cleanup\n"
                                    }
                                } else {
                                    processLog += "[1/3] Skip backup (user pilih)\n"
                                }

                                processLog += "[2/3] Hapus data foreign...\n"
                                val cleanupResult = withContext(Dispatchers.IO) {
                                    DLavieIntegrityAnalyzer.cleanupForeignGameData()
                                }
                                processLog += "[2/3] ✓ ${cleanupResult.deletedFiles} file dihapus, ${cleanupResult.failedFiles} gagal\n"

                                processLog += "[3/3] Selesai. Melanjutkan install...\n"
                            }

                            isProcessing = false
                            onContinue()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canContinue,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (result.status) {
                            DLavieIntegrityAnalyzer.AnalysisStatus.OK -> Color(0xFF4CAF50)
                            DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP -> Color(0xFFFF5252)
                            else -> Color(0xFF6B7280)
                        }
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        when (result.status) {
                            DLavieIntegrityAnalyzer.AnalysisStatus.OK -> "Lanjut Install"
                            DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP -> "Backup & Cleanup"
                            DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_PERMISSIONS -> "Permission Kurang"
                            DLavieIntegrityAnalyzer.AnalysisStatus.BLOCKED -> "Ditolak"
                        },
                        color = Color.White,
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── Helper Composables ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Box(
            Modifier.size(width = 3.dp, height = 16.dp)
                .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Icon(icon, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = InterFontFamily
        )
    }
}

@Composable
private fun InfoGrid(items: List<Pair<String, String>>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.02f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(Modifier.padding(12.dp)) {
            items.forEachIndexed { index, (key, value) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        key,
                        color = Color(0xFF6B7280),
                        fontSize = 10.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        value,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.6f)
                    )
                }
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        thickness = 0.5.dp,
                        color = Color.White.copy(alpha = 0.05f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerificationCard(
    title: String,
    status: Any,
    details: String
) {
    val statusColor = when (status) {
        DLavieIntegrityAnalyzer.ApkVerificationStatus.VERIFIED -> Color(0xFF4CAF50)
        DLavieIntegrityAnalyzer.ApkVerificationStatus.NOT_INSTALLED -> Color(0xFF6B7280)
        DLavieIntegrityAnalyzer.ApkVerificationStatus.BLOCKED -> Color(0xFFFF5252)
        DLavieIntegrityAnalyzer.ApkVerificationStatus.UNKNOWN -> Color(0xFFFFB74D)
        DLavieIntegrityAnalyzer.GameDataStatusType.NO_DATA -> Color(0xFF6B7280)
        DLavieIntegrityAnalyzer.GameDataStatusType.DLAVIE_ORIGINAL -> Color(0xFF4CAF50)
        DLavieIntegrityAnalyzer.GameDataStatusType.FOREIGN_DATA -> Color(0xFFFFB74D)
        DLavieIntegrityAnalyzer.GameDataStatusType.INCOMPLETE -> Color(0xFFFFB74D)
        else -> Color(0xFF6B7280)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).background(statusColor, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
            Text(
                details,
                color = Color(0xFFB8BCC8),
                fontSize = 10.sp,
                fontFamily = InterFontFamily,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun PermissionRow(name: String, granted: Boolean, critical: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            contentDescription = null,
            tint = if (granted) Color(0xFF4CAF50) else if (critical) Color(0xFFFF5252) else Color(0xFFFFB74D),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (granted) "GRANTED" else if (critical) "MISSING" else "OPTIONAL",
            color = if (granted) Color(0xFF4CAF50) else if (critical) Color(0xFFFF5252) else Color(0xFFFFB74D),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun statusColor(status: DLavieIntegrityAnalyzer.AnalysisStatus): Color = when (status) {
    DLavieIntegrityAnalyzer.AnalysisStatus.OK -> Color(0xFF4CAF50)
    DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_PERMISSIONS -> Color(0xFFFFB74D)
    DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP -> Color(0xFFFFB74D)
    DLavieIntegrityAnalyzer.AnalysisStatus.BLOCKED -> Color(0xFFFF5252)
}

private fun statusIcon(status: DLavieIntegrityAnalyzer.AnalysisStatus) = when (status) {
    DLavieIntegrityAnalyzer.AnalysisStatus.OK -> Icons.Rounded.CheckCircle
    DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_PERMISSIONS -> Icons.Rounded.Warning
    DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP -> Icons.Rounded.Warning
    DLavieIntegrityAnalyzer.AnalysisStatus.BLOCKED -> Icons.Rounded.Block
}

private fun statusTitle(status: DLavieIntegrityAnalyzer.AnalysisStatus) = when (status) {
    DLavieIntegrityAnalyzer.AnalysisStatus.OK -> "✓ Siap Install"
    DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_PERMISSIONS -> "⚠ Permission Tidak Lengkap"
    DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP -> "⚠ Perlu Cleanup Data"
    DLavieIntegrityAnalyzer.AnalysisStatus.BLOCKED -> "🚫 Ditolak — Bukan DLavie Original"
}
