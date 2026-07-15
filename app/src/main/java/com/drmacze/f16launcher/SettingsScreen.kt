package com.drmacze.f16launcher

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    api: CommunityApi,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onCheckUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var autoCheckUpdates by remember { mutableStateOf(true) }
    var autoLaunchAfterPatch by remember { mutableStateOf(false) }
    var showAdvancedTools by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(PinManager.hasPin(context)) }
    var filesAccessGranted by remember { mutableStateOf(StorageAccess.isGranted()) }

    // ── Save Game state ──
    var saveSlots by remember { mutableStateOf(SaveGameManager.listSlots()) }
    var saveExpanded by remember { mutableStateOf(false) }
    var saveBackupInProgress by remember { mutableStateOf(false) }
    var saveRestoreSlot by remember { mutableStateOf(-1) }
    var saveMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Refresh save slots on resume (e.g., after returning from another screen)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                saveSlots = SaveGameManager.listSlots()
                filesAccessGranted = StorageAccess.isGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        try { kotlinx.coroutines.awaitCancellation() } finally { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier.fillMaxSize().background(Carbon).verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.ArrowBack, null,
                tint = Color.White,
                modifier = Modifier.size(24.dp).clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }
            )
            Spacer(Modifier.width(16.dp))
            Text("Pengaturan", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }

        // ═══════════════════════════════════════════════════════════════
        // SAVE GAME MANAGER (NEW — v7.9.48)
        // ═══════════════════════════════════════════════════════════════
        SettingsSectionHeader("Save Game", Icons.Rounded.Save)

        // Backup current save button
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = TTShapes.card,
            colors = CardDefaults.cardColors(containerColor = GlassBase),
            border = BorderStroke(1.dp, GlassStroke)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF4CAF50).copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Save, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Backup Save Sekarang", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Simpan progress FIFA 16 ke slot kosong",
                            color = SubText, fontSize = 11.sp
                        )
                    }
                    if (saveBackupInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Rounded.ArrowForward, null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp).clickable {
                                if (!StorageAccess.isGranted()) {
                                    saveMessage = Pair(false, "Izinkan akses file dulu di bagian Penyimpanan")
                                    return@clickable
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                saveBackupInProgress = true
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        SaveGameManager.backupSave(context)
                                    }
                                    saveBackupInProgress = false
                                    saveMessage = Pair(result.success, result.message)
                                    if (result.success) {
                                        saveSlots = SaveGameManager.listSlots()
                                        saveExpanded = true
                                    }
                                }
                            }
                        )
                    }
                }

                // Status message
                saveMessage?.let { (success, msg) ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = if (success) Color(0xFF4CAF50).copy(0.1f) else Color(0xFFFF5252).copy(0.1f),
                        border = BorderStroke(1.dp, if (success) Color(0xFF4CAF50).copy(0.3f) else Color(0xFFFF5252).copy(0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            msg,
                            color = if (success) Color(0xFF4CAF50) else Color(0xFFFF5252),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Expandable list of save slots
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    saveExpanded = !saveExpanded
                    if (saveExpanded) saveSlots = SaveGameManager.listSlots()
                },
            shape = TTShapes.card,
            colors = CardDefaults.cardColors(containerColor = GlassBase),
            border = BorderStroke(1.dp, GlassStroke)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Folder, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Slot Save (${saveSlots.count { it.exists }}/${saveSlots.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (saveExpanded) "Tutup daftar slot" else "Lihat & kelola slot save",
                        color = SubText, fontSize = 11.sp
                    )
                }
                Icon(
                    if (saveExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, tint = SubText, modifier = Modifier.size(20.dp)
                )
            }
        }

        // Slot list (only when expanded)
        if (saveExpanded) {
            saveSlots.forEach { slot ->
                SaveSlotCard(
                    slot = slot,
                    isRestoring = saveRestoreSlot == slot.slotNumber,
                    onRestore = {
                        if (!StorageAccess.isGranted()) {
                            saveMessage = Pair(false, "Izinkan akses file dulu")
                            return@SaveSlotCard
                        }
                        if (!slot.exists) {
                            saveMessage = Pair(false, "Slot ${slot.slotNumber} kosong")
                            return@SaveSlotCard
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        saveRestoreSlot = slot.slotNumber
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                SaveGameManager.restoreSave(context, slot.slotNumber)
                            }
                            saveRestoreSlot = -1
                            saveMessage = Pair(result.success, result.message)
                        }
                    },
                    onDelete = {
                        if (!slot.exists) return@SaveSlotCard
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val result = SaveGameManager.deleteSlot(slot.slotNumber)
                        saveMessage = Pair(result.success, result.message)
                        if (result.success) {
                            saveSlots = SaveGameManager.listSlots()
                        }
                    }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // APLIKASI
        // ═══════════════════════════════════════════════════════════════
        SettingsSectionHeader("Aplikasi", Icons.Rounded.Apps)

        SettingsToggle(
            icon = Icons.Rounded.Update,
            title = "Cek Update Otomatis",
            subtitle = "Cek patch baru saat app dibuka",
            checked = autoCheckUpdates,
            onChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                autoCheckUpdates = it
            }
        )

        SettingsToggle(
            icon = Icons.Rounded.PlayArrow,
            title = "Auto Launch Setelah Patch",
            subtitle = "Buka FIFA 16 otomatis setelah apply patch",
            checked = autoLaunchAfterPatch,
            onChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                autoLaunchAfterPatch = it
            }
        )

        SettingsToggle(
            icon = Icons.Rounded.Build,
            title = "Tampilkan Tools Advanced",
            subtitle = "Show Shizuku, root, debug options",
            checked = showAdvancedTools,
            onChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showAdvancedTools = it
            }
        )

        SettingsSectionHeader("Keamanan", Icons.Rounded.Security)

        SettingsToggle(
            icon = Icons.Rounded.Lock,
            title = "PIN Launcher",
            subtitle = if (pinEnabled) "Aktif — PIN diperlukan untuk buka app" else "Nonaktif",
            checked = pinEnabled,
            onChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (it) {
                    PinLockActivity.launch(context, PinLockActivity.MODE_SETUP)
                } else {
                    PinLockActivity.launch(context, PinLockActivity.MODE_DISABLE)
                }
            }
        )

        SettingsSectionHeader("Penyimpanan", Icons.Rounded.Storage)

        SettingsAction(
            icon = Icons.Rounded.Folder,
            title = "Akses File",
            subtitle = if (filesAccessGranted) "Aktif — bisa apply patch tanpa Shizuku" else "Nonaktif — perlu Shizuku untuk patch",
            actionLabel = if (filesAccessGranted) "Aktif" else "Izinkan",
            onAction = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                StorageAccess.request(context)
            }
        )

        SettingsSectionHeader("Akun", Icons.Rounded.AccountCircle)

        SettingsAction(
            icon = Icons.Rounded.Lock,
            title = "Ganti Password",
            subtitle = "Ubah password akun DLavie",
            onAction = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        )

        SettingsAction(
            icon = Icons.Rounded.Email,
            title = "Ganti Email",
            subtitle = "Ubah email akun",
            onAction = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        )

        SettingsAction(
            icon = Icons.Rounded.Person,
            title = "Ganti Profil",
            subtitle = "Username & display name",
            onAction = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        )

        SettingsSectionHeader("Tentang", Icons.Rounded.Info)

        SettingsInfo(
            icon = Icons.Rounded.Info,
            title = "Versi App",
            value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )

        SettingsInfo(
            icon = Icons.Rounded.Code,
            title = "Build",
            value = "DLavie 26 Phase 4"
        )

        SettingsAction(
            icon = Icons.Rounded.Verified,
            title = "Trusted by DLavie",
            subtitle = "Game resmi yang diverifikasi",
            onAction = {}
        )

        // v7.9.92: Manual Check Update button
        SettingsAction(
            icon = Icons.Rounded.SystemUpdate,
            title = "Cek Pembaruan",
            subtitle = "Periksa versi terbaru DLavie Launcher",
            onAction = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckUpdate()
            }
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLogout()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
            shape = TTShapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = DangerRed,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Keluar dari Akun", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(100.dp))
    }
}

// ═══════════════════════════════════════════════════════════════
// SAVE SLOT CARD — tampilkan info slot + tombol restore/delete
// ═══════════════════════════════════════════════════════════════
@Composable
private fun SaveSlotCard(
    slot: SaveGameManager.SaveSlot,
    isRestoring: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (slot.exists) GlassBase else Color(0xFF1A1A1A)
        ),
        border = BorderStroke(
            1.dp,
            if (slot.exists) Color(0xFF4CAF50).copy(0.2f) else GlassStroke
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Slot number badge
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(
                            if (slot.exists) Color(0xFF4CAF50).copy(0.2f)
                            else Color.White.copy(0.05f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        slot.slotNumber.toString(),
                        color = if (slot.exists) Color(0xFF4CAF50) else SubText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        if (slot.exists) slot.label else "Slot ${slot.slotNumber} (kosong)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (slot.exists) {
                        Text(
                            "${dateFormat.format(Date(slot.timestamp))} • ${slot.fileCount} file • ${formatSaveSize(slot.sizeBytes)}",
                            color = SubText,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            "Belum ada save di slot ini",
                            color = SubText,
                            fontSize = 10.sp
                        )
                    }
                }

                // Action buttons
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else if (slot.exists) {
                    // Restore button
                    Icon(
                        Icons.Rounded.Restore, null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(22.dp).clickable { onRestore() }
                    )
                    Spacer(Modifier.width(8.dp))
                    // Delete button
                    Icon(
                        Icons.Rounded.Delete, null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp).clickable { onDelete() }
                    )
                }
            }
        }
    }
}

private fun formatSaveSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = SubText, fontSize = 11.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.White.copy(0.3f),
                    uncheckedThumbColor = SubText,
                    uncheckedTrackColor = Surface2
                )
            )
        }
    }
}

@Composable
private fun SettingsAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String = "",
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onAction() },
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = SubText, fontSize = 11.sp)
            }
            if (actionLabel.isNotEmpty()) {
                Text(actionLabel, color = Color.White.copy(0.5f), fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SubText, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsInfo(
    icon: ImageVector,
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(value, color = SubText, fontSize = 12.sp)
        }
    }
}
