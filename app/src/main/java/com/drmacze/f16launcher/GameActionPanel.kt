package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// ─── Game Action Panel (Floating Overlay ala Kickstarter) ─────────────────────
//
// Dipanggil dari GameHubScreen saat user tap game card.
// Muncul sebagai floating panel di tengah screen (bukan full-screen modal).
//
// Steps shown:
//   1. Download APK        → if not installed
//   2. Install Data + OBB  → if not data-ready
//   3. Apply Mod           → if patch available (FIFA 16 only)
//   4. Play                → launch game
//
// Each step has:
//   - Step number badge (or ✓ if done)
//   - Title + subtitle
//   - Action button (Download / Install / Apply / Play)
//   - Status indicator (done / active / pending)
//
// Background overlay: dimmed + tap to dismiss
// Panel: centered, rounded, white-on-black monochrome theme

private val PanelBlack     = Color(0xFF000000)
private val PanelCardBg    = Color(0xFF0A0A0A)
private val PanelCardBgAlt = Color(0xFF101010)
private val PanelBorder    = Color(0x33FFFFFF)
private val PanelText      = Color(0xFFFFFFFF)
private val PanelSubText   = Color(0xFFAAAAAA)
private val PanelMuted     = Color(0xFF666666)
private val PanelGreen     = Color(0xFFFFFFFF)
private val PanelRed       = Color(0xFFFF5555)
private val PanelYellow    = Color(0xFFFFFF88)
private val PanelDim       = Color(0xCC000000)  // 80% black for background

/** Step definition for GameActionPanel — built per render based on game state. */
private data class StepDef(
    val num: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val done: Boolean,
    val actionable: Boolean,
    val actionText: String,
    val isPrimary: Boolean = false
)

@Composable
fun GameActionPanel(
    game: GameItem,
    onDismiss: () -> Unit,
    onGoToDlc: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── State ──
    var apkInstalled by remember { mutableStateOf(false) }
    var dataReady by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var installing by remember { mutableStateOf(false) }
    var installPhase by remember { mutableStateOf("") }
    var installProgress by remember { mutableStateOf(0f) }
    var installMessage by remember { mutableStateOf("") }
    var installError by remember { mutableStateOf("") }
    var filesAccessGranted by remember { mutableStateOf(false) }

    // FIFA 15 manager (only used if game is FIFA 15)
    val fifa15Manager = remember(game.packageName) {
        if (game.packageName == GAME_PKG_15) Fifa15DataManager(context) else null
    }

    // ── Refresh functions ──
    fun refresh() {
        scope.launch(Dispatchers.IO) {
            apkInstalled = try {
                context.packageManager.getPackageInfo(game.packageName, 0); true
            } catch (_: Throwable) { false }

            dataReady = if (game.packageName == GAME_PKG_15) {
                fifa15Manager?.isDataInstalled() ?: false
            } else {
                isDataReady()
            }
            filesAccessGranted = StorageAccess.isGranted()
            withContext(Dispatchers.Main) { loading = false }
        }
    }

    LaunchedEffect(game.packageName) { refresh() }

    // ── Install action (for FIFA 15) ──
    fun startFifa15Install() {
        if (installing) return
        if (!StorageAccess.isGranted()) {
            StorageAccess.request(context)
            return
        }
        fifa15Manager?.let { manager ->
            installing = true
            installError = ""
            installProgress = 0f
            installPhase = "starting"
            installMessage = "Memulai instalasi..."
            scope.launch {
                val result = manager.downloadAndInstall(
                    onProgress = { phase, current, total, message ->
                        installPhase = phase
                        installMessage = message
                        installProgress = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    }
                )
                installing = false
                result.onSuccess {
                    installMessage = "Data berhasil dipasang!"
                    installPhase = "done"
                    installProgress = 1f
                    refresh()
                }.onFailure { err ->
                    installError = err.message ?: "Gagal instalasi"
                    installPhase = "error"
                }
            }
        }
    }

    // ── Step definitions (built per render based on current state) ──
    val steps = remember(apkInstalled, dataReady, installing, game) {
        val list = mutableListOf<StepDef>()

        // Step 1: Install APK
        list.add(
            StepDef(
                num = 1,
                title = "Install APK",
                subtitle = if (apkInstalled) "APK ${game.title} sudah terpasang"
                           else "Unduh APK ${game.title} (~${if (game.packageName == GAME_PKG_15) "22" else "23"} MB)",
                icon = Icons.Rounded.Android,
                done = apkInstalled,
                actionable = !apkInstalled && !installing,
                actionText = "Unduh APK"
            )
        )

        // Step 2: Install Data
        val dataSubtitle = if (game.packageName == GAME_PKG_15) {
            if (dataReady) "DATA + OBB siap"
            else "Auto-download DATA (72 MB) + OBB (1.1 GB) — tanpa ZArchiver"
        } else {
            if (dataReady) "Data game siap"
            else "Apply base data + mod via Patch System"
        }
        list.add(
            StepDef(
                num = 2,
                title = "Install Data",
                subtitle = dataSubtitle,
                icon = Icons.Rounded.FolderOpen,
                done = dataReady,
                actionable = apkInstalled && !dataReady && !installing,
                actionText = if (installing) "Memasang..." else if (dataReady) "Terpasang" else "Unduh & Pasang",
                isPrimary = !dataReady && apkInstalled
            )
        )

        // Step 3: Apply Mod (FIFA 16 only)
        if (game.packageName == GAME_PKG_16) {
            val patched = readMarker().startsWith("v26")
            list.add(
                StepDef(
                    num = 3,
                    title = "Apply Mod Patch",
                    subtitle = if (patched) "Mod terpasang: ${readMarker().take(20)}"
                               else "Pilih mod dari DLC page → apply patch",
                    icon = Icons.Rounded.Extension,
                    done = patched,
                    actionable = !patched && !installing,
                    actionText = "Buka DLC"
                )
            )
        }

        // Final step: Play
        val playStepNum = if (game.packageName == GAME_PKG_16) 4 else 3
        list.add(
            StepDef(
                num = playStepNum,
                title = "Mainkan",
                subtitle = "Launch ${game.title}",
                icon = Icons.Rounded.PlayCircle,
                done = false,
                actionable = apkInstalled && dataReady && !installing,
                actionText = "Play",
                isPrimary = apkInstalled && dataReady
            )
        )

        list
    }

    // ── Layout: Background dim + centered panel ──
    Box(
        Modifier
            .fillMaxSize()
            .background(PanelDim)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // ── Panel ──
        Card(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {} // consume clicks so they don't pass through to background
            ,
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = PanelCardBg),
            border = BorderStroke(1.dp, PanelBorder)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── Header ───
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(game.coverGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            game.coverText,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Siap main?",
                            color = PanelText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            game.title,
                            color = PanelSubText,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Close button
                    Box(
                        Modifier.size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, PanelBorder, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = PanelSubText, modifier = Modifier.size(16.dp))
                    }
                }

                // ─── Storage permission warning (if not granted) ───
                AnimatedVisibility(!filesAccessGranted && !loading, enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PanelYellow.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, PanelYellow.copy(alpha = 0.4f))
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Security, null, tint = PanelYellow, modifier = Modifier.size(14.dp))
                            Text(
                                "Aktifkan izin Akses File untuk install data otomatis",
                                color = PanelYellow,
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ─── Install progress (FIFA 15 only, when installing) ───
                AnimatedVisibility(installing, enter = fadeIn(), exit = fadeOut()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PanelText
                            )
                            Text(
                                phaseLabel(installPhase),
                                color = PanelText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(installProgress * 100).toInt()}%",
                                color = PanelText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                        }
                        LinearProgressIndicator(
                            progress = { installProgress },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = PanelText,
                            trackColor = PanelCardBgAlt
                        )
                        Text(
                            installMessage,
                            color = PanelSubText,
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ─── Error ───
                AnimatedVisibility(installError.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PanelRed.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, PanelRed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, null, tint = PanelRed, modifier = Modifier.size(14.dp))
                            Text(
                                installError,
                                color = PanelRed,
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ─── Steps list ───
                if (loading) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = PanelText
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Memeriksa status...",
                            color = PanelSubText,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.forEach { step ->
                            StepRow(
                                step = step,
                                onAction = {
                                    when (step.num) {
                                        1 -> {
                                            // Download APK
                                            val url = if (game.packageName == GAME_PKG_15) FIFA15_APK_URL else FIFA16_APK_URL
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        }
                                        2 -> {
                                            // Install data
                                            if (game.packageName == GAME_PKG_15) {
                                                startFifa15Install()
                                            } else {
                                                // FIFA 16 → go to DLC for patch system
                                                onGoToDlc()
                                            }
                                        }
                                        3 -> {
                                            // For FIFA 16: apply mod → go to DLC
                                            // For FIFA 15: play
                                            if (game.packageName == GAME_PKG_16) {
                                                onGoToDlc()
                                            } else {
                                                launchGame(context, game.packageName, game.mainActivity)
                                                onDismiss()
                                            }
                                        }
                                        4 -> {
                                            // Play (FIFA 16)
                                            launchGame(context, game.packageName, game.mainActivity)
                                            onDismiss()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // ─── Tip ───
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PanelCardBgAlt
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = PanelMuted, modifier = Modifier.size(12.dp))
                        Text(
                            "Tip: Selesaikan step 1 & 2 dulu sebelum main untuk pengalaman terbaik.",
                            color = PanelMuted,
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    step: StepDef,
    onAction: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PanelCardBgAlt,
        border = BorderStroke(1.dp, if (step.isPrimary && step.actionable) PanelBorder else PanelBorder.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step badge
            Box(
                Modifier.size(28.dp)
                    .background(
                        if (step.done) PanelGreen else if (step.isPrimary && step.actionable) PanelText else PanelCardBg,
                        CircleShape
                    )
                    .border(1.dp, PanelBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (step.done) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = PanelBlack, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        step.num.toString(),
                        color = if (step.isPrimary && step.actionable) PanelBlack else PanelSubText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                }
            }

            // Icon + text
            Icon(step.icon, null, tint = if (step.done) PanelGreen else PanelText, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    step.title,
                    color = PanelText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    maxLines = 1
                )
                Text(
                    step.subtitle,
                    color = PanelSubText,
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }

            // Action button
            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onAction()
                },
                enabled = step.actionable,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (step.isPrimary && step.actionable) PanelText else PanelCardBg,
                    contentColor = if (step.isPrimary && step.actionable) PanelBlack else PanelSubText,
                    disabledContainerColor = if (step.done) PanelGreen.copy(alpha = 0.15f) else PanelCardBgAlt,
                    disabledContentColor = if (step.done) PanelGreen else PanelMuted
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(step.actionText, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
            }
        }
    }
}

private fun phaseLabel(phase: String): String = when (phase) {
    "starting"      -> "Memulai..."
    "download_data" -> "Mengunduh DATA"
    "download_obb"  -> "Mengunduh OBB"
    "extract_data"  -> "Mengekstrak DATA"
    "extract_obb"   -> "Mengekstrak OBB"
    "verify"        -> "Verifikasi"
    "done"          -> "Selesai"
    "error"         -> "Error"
    else            -> phase
}
