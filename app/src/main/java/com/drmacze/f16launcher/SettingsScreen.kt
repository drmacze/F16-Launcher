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

@Composable
fun SettingsScreen(
    api: CommunityApi,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var autoCheckUpdates by remember { mutableStateOf(true) }
    var autoLaunchAfterPatch by remember { mutableStateOf(false) }
    var showAdvancedTools by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(PinManager.hasPin(context)) }
    var filesAccessGranted by remember { mutableStateOf(StorageAccess.isGranted()) }

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
