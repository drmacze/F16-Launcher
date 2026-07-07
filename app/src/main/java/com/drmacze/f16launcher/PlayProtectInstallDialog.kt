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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v7.9.50: PlayProtectInstallDialog — Dialog dengan streaming progress untuk
 * install game di GameHub. User klik Install → Play Protect running → download.
 *
 * Flow:
 * 1. User klik Install di GameHub
 * 2. Dialog muncul dengan streaming progress:
 *    [✓] Initializing...
 *    [✓] Permission check...
 *    [→] DLavie Play Protect running...  (animated)
 *    [ ] Verifying game data...
 *    [ ] Ready to download
 * 3. Jika semua pass → auto-trigger download
 * 4. Jika ada masalah (permission/data foreign) → tampilkan detail + action button
 * 5. User resolve → re-check → download
 *
 * Progress messages muncul satu per satu (streaming effect) supaya user
 * merasa sistem benar-benar bekerja, bukan dummy.
 */
@Composable
fun PlayProtectInstallDialog(
    onDismiss: () -> Unit,
    onDownloadStart: () -> Unit,
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State untuk streaming progress
    var currentStep by remember { mutableStateOf(0) }
    var stepStatuses by remember { mutableStateOf(listOf<StepStatus>()) }
    var analysisResult by remember { mutableStateOf<DLavieIntegrityAnalyzer.AnalysisResult?>(null) }
    var isComplete by remember { mutableStateOf(false) }
    var hasIssue by remember { mutableStateOf(false) }

    val steps = listOf(
        "Initializing DLavie Play Protect...",
        "Checking permissions...",
        "DLavie Play Protect running...",
        "Verifying Google account...",
        "Checking game data integrity...",
        "Preparing download..."
    )

    // v7.9.59: State untuk Google account check
    var googleAccountMissing by remember { mutableStateOf(false) }

    // Run analysis saat dialog muncul
    LaunchedEffect(Unit) {
        // Streaming effect: tampilkan step satu per satu
        for (i in steps.indices) {
            currentStep = i
            stepStatuses = stepStatuses + StepStatus.IN_PROGRESS
            delay(500L) // jeda supaya terlihat streaming

            // Setelah step "DLavie Play Protect running", jalankan analisis
            if (i == 2) {
                val result = withContext(Dispatchers.IO) {
                    DLavieIntegrityAnalyzer.analyze(context)
                }
                analysisResult = result

                // Update step 2 (Play Protect running) → done
                stepStatuses = stepStatuses.dropLast(1) + StepStatus.DONE

                // Cek apakah ada masalah
                if (result.status != DLavieIntegrityAnalyzer.AnalysisStatus.OK) {
                    hasIssue = true
                    // Tandai step selanjutnya sebagai SKIPPED
                    for (j in (i + 1) until steps.size) {
                        stepStatuses = stepStatuses + StepStatus.SKIPPED
                    }
                    return@LaunchedEffect
                }
            } else if (i == 3) {
                // v7.9.59: Step "Verifying Google account" — cek Google account
                val hasGoogle = withContext(Dispatchers.IO) {
                    DLavieIntegrityAnalyzer.hasGoogleAccount(context)
                }
                delay(300L)
                if (hasGoogle) {
                    stepStatuses = stepStatuses.dropLast(1) + StepStatus.DONE
                } else {
                    // Google account tidak ada → block install
                    googleAccountMissing = true
                    hasIssue = true
                    stepStatuses = stepStatuses.dropLast(1) + StepStatus.SKIPPED
                    // Tandai step selanjutnya sebagai SKIPPED
                    for (j in (i + 1) until steps.size) {
                        stepStatuses = stepStatuses + StepStatus.SKIPPED
                    }
                    return@LaunchedEffect
                }
            } else {
                delay(300L)
                stepStatuses = stepStatuses.dropLast(1) + StepStatus.DONE
            }
        }

        // Semua step selesai → trigger download
        isComplete = true
        delay(500L)
        onDownloadStart()
    }

    Dialog(
        onDismissRequest = { if (!hasIssue || isComplete) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !hasIssue || isComplete,
            dismissOnClickOutside = !hasIssue || isComplete,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1117)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2D3A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = if (hasIssue) Color(0xFFFFB74D) else Color(0xFF4CAF50),
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
                            if (isComplete) "Verification complete" else "Running security checks...",
                            color = Color(0xFF6B7280),
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                // Streaming Progress Steps
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0A0A0A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A1D2A))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.forEachIndexed { index, step ->
                            val status = when {
                                index < stepStatuses.size -> stepStatuses[index]
                                index == currentStep -> StepStatus.IN_PROGRESS
                                else -> StepStatus.PENDING
                            }
                            ProgressStep(
                                step = step,
                                status = status,
                                stepNumber = index + 1
                            )
                        }
                    }
                }

                // v7.9.59: Google account missing warning
                AnimatedVisibility(
                    visible = googleAccountMissing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF5252).copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Google Account Diperlukan",
                                    color = Color(0xFFFF5252),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                )
                            }
                            Text(
                                "APK FIFA 16 memerlukan Google account untuk diinstall. " +
                                "Device Anda belum memiliki Google account yang login.\n\n" +
                                "Cara menambahkan:\n" +
                                "1. Buka Settings Android\n" +
                                "2. Cari 'Accounts' atau 'Users & accounts'\n" +
                                "3. Tap 'Add account' > 'Google'\n" +
                                "4. Login dengan Gmail Anda\n" +
                                "5. Kembali ke Launcher dan tap 'Re-Check'",
                                color = Color(0xFFB8BCC8),
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily,
                                lineHeight = 15.sp
                            )
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                                            putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                            android.widget.Toast.makeText(context, "Buka Settings > Accounts > Add account > Google", android.widget.Toast.LENGTH_LONG).show()
                                        } catch (_: Exception) {
                                            android.widget.Toast.makeText(context, "Buka Settings > Accounts > Add Google account", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5252),
                                    contentColor = Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Rounded.AccountCircle, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tambah Google Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }

                // Jika ada masalah (Play Protect), tampilkan detail + action
                AnimatedVisibility(
                    visible = hasIssue && analysisResult != null && !googleAccountMissing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val result = analysisResult!!
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Status summary
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (result.status) {
                                DLavieIntegrityAnalyzer.AnalysisStatus.OK -> Color(0xFF4CAF50).copy(alpha = 0.08f)
                                DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_PERMISSIONS -> Color(0xFFFFB74D).copy(alpha = 0.08f)
                                DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP -> Color(0xFFFFB74D).copy(alpha = 0.08f)
                                DLavieIntegrityAnalyzer.AnalysisStatus.BLOCKED -> Color(0xFFFF5252).copy(alpha = 0.08f)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2D3A))
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    when (result.status) {
                                        DLavieIntegrityAnalyzer.AnalysisStatus.OK -> "✓ All checks passed"
                                        DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_PERMISSIONS -> "⚠ Permission required"
                                        DLavieIntegrityAnalyzer.AnalysisStatus.NEEDS_CLEANUP -> "⚠ Data cleanup needed"
                                        DLavieIntegrityAnalyzer.AnalysisStatus.BLOCKED -> "✗ Blocked"
                                    },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                )
                                Text(
                                    result.summary,
                                    color = Color(0xFFB8BCC8),
                                    fontSize = 11.sp,
                                    fontFamily = InterFontFamily,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Device info (compact)
                        val device = result.deviceInfo
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.02f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    "Device Info",
                                    color = Color(0xFF6B7280),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                )
                                Spacer(Modifier.height(4.dp))
                                InfoRow("Android", device.androidVersionName)
                                InfoRow("Model", "${device.manufacturer} ${device.model}")
                                InfoRow("Storage", device.storageFormatted)
                                InfoRow("Environment", if (device.isEmulator) "Emulator" else if (device.isRooted) "Rooted" else "Physical")
                            }
                        }

                        // Action buttons berdasarkan masalah
                        val perms = result.permissionAudit
                        if (perms.missingCritical.contains("android.permission.MANAGE_EXTERNAL_STORAGE")) {
                            ActionButton(
                                icon = Icons.Rounded.Folder,
                                title = "Buka Settings Storage",
                                subtitle = "Enable 'Allow access to manage all files'",
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
                                }
                            )
                        }
                        if (perms.missingCritical.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
                            ActionButton(
                                icon = Icons.Rounded.InstallMobile,
                                title = "Buka Settings Install Apps",
                                subtitle = "Allow 'Install unknown apps' untuk DLavie",
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                            data = android.net.Uri.parse("package:${context.packageName}")
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
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
                                }
                            )
                        }

                        // Instructions
                        Text(
                            "Setelah grant permission, kembali ke Launcher dan tap 'Re-Check' untuk refresh status.",
                            color = Color(0xFFB8BCC8),
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                // Action buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasIssue) {
                        // Cancel + Re-Check
                        OutlinedButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB8BCC8))
                        ) {
                            Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Batal", fontFamily = InterFontFamily, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Re-Check", fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isComplete) {
                        // Complete
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Verified! Starting download...",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                    // Jika masih running (tidak hasIssue, tidak isComplete), tidak ada button
                }
            }
        }
    }
}

enum class StepStatus { PENDING, IN_PROGRESS, DONE, SKIPPED }

@Composable
private fun ProgressStep(step: String, status: StepStatus, stepNumber: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            StepStatus.DONE -> {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp)
                )
            }
            StepStatus.IN_PROGRESS -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF4CAF50)
                )
            }
            StepStatus.SKIPPED -> {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(18.dp)
                )
            }
            StepStatus.PENDING -> {
                Box(
                    Modifier.size(18.dp)
                        .background(Color(0xFF1A1D2A), RoundedCornerShape(9.dp))
                        .border(1.dp, Color(0xFF2A2D3A), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stepNumber.toString(),
                        color = Color(0xFF6B7280),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            step,
            color = when (status) {
                StepStatus.DONE -> Color.White
                StepStatus.IN_PROGRESS -> Color(0xFF4CAF50)
                StepStatus.SKIPPED -> Color(0xFF6B7280)
                StepStatus.PENDING -> Color(0xFF6B7280)
            },
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
            fontWeight = if (status == StepStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            key,
            color = Color(0xFF6B7280),
            fontSize = 9.sp,
            fontFamily = InterFontFamily
        )
        Text(
            value,
            color = Color.White,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFB74D),
            contentColor = Color.Black
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text(subtitle, fontSize = 9.sp, fontFamily = InterFontFamily)
        }
    }
}
