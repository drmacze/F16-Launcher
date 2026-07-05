package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Storage
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

// ─── DLC Page (Cleaner Redesign) ──────────────────────────────────────────────
//
// Replaces the old UpdateScreen. Design goals:
// 1. Cleaner — each game (FIFA 16 + FIFA 15) gets its own card with consistent layout
// 2. Easier to understand — step-by-step action flow (Download APK → Download Data → Play)
// 3. No "miss data" — multi-source status detection per game + explicit progress per phase
// 4. FIFA 16 stays the mod priority; FIFA 15 only has base data install (no patch system)
//
// Sections:
//   1. Header (cleaner — title + subtitle only)
//   2. Storage Permission Card (only shown if MANAGE_EXTERNAL_STORAGE not granted)
//   3. Game Card: FIFA 16 (DLavie 26) — full mod support
//   4. Game Card: FIFA 15 (DLavie 15) — base data only, no mods
//   5. Storage Info Card (free space + backup summary)
// ──────────────────────────────────────────────────────────────────────────────

// Design tokens (matching existing monochrome theme)
private val DlcBlack     = Color(0xFF000000)
private val DlcCardBg    = Color(0xFF0A0A0A)
private val DlcCardBgAlt = Color(0xFF101010)
private val DlcBorder    = Color(0x1AFFFFFF)  // 10% white
private val DlcBorderHi  = Color(0x33FFFFFF)  // 20% white
private val DlcText      = Color(0xFFFFFFFF)
private val DlcSubText   = Color(0xFFAAAAAA)
private val DlcMuted     = Color(0xFF666666)
private val DlcGreen     = Color(0xFFFFFFFF)  // monochrome — use white for "ok"
private val DlcRed       = Color(0xFFFF5555)
private val DlcYellow    = Color(0xFFFFFF88)
private val DlcAccent    = Color(0xFFFFFFFF)  // primary accent (white-on-black)

@Composable
fun DlcScreen(
    api: CommunityApi,
    maintenanceInfo: MaintenanceInfo? = null,
    onNav: (Page) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Patch Tools overlay state ──
    // When true, render the existing UpdateScreen (full patch system UI:
    // apply patch, backup list, rollback, logs) as a full-screen overlay.
    // User taps "Apply Patch" on FIFA 16 card → opens this overlay.
    // UpdateScreen's onNav(Page.DLC) is intercepted to close the overlay.
    var showPatchTools by remember { mutableStateOf(false) }

    if (showPatchTools) {
        UpdateScreen(
            api = api,
            maintenanceInfo = maintenanceInfo,
            onNav = { target ->
                // Any nav target (including Page.DLC) closes the overlay
                if (target == Page.DLC) showPatchTools = false
                else onNav(target)
            }
        )
        return
    }

    // ── Storage permission state ──
    var filesAccessGranted by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                filesAccessGranted = StorageAccess.isGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        try { kotlinx.coroutines.awaitCancellation() } finally { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { filesAccessGranted = StorageAccess.isGranted() }

    // ── FIFA 16 state ──
    var fifa16ApkInstalled by remember { mutableStateOf(false) }
    var fifa16DataReady    by remember { mutableStateOf(false) }
    var fifa16Loading      by remember { mutableStateOf(true) }

    // ── FIFA 15 state ──
    val fifa15Manager = remember { Fifa15DataManager(context) }
    var fifa15ApkInstalled by remember { mutableStateOf(false) }
    var fifa15DataReady    by remember { mutableStateOf(false) }
    var fifa15Loading      by remember { mutableStateOf(true) }
    var fifa15Installing   by remember { mutableStateOf(false) }
    var fifa15Phase        by remember { mutableStateOf("") }
    var fifa15Progress     by remember { mutableStateOf(0f) }      // 0..1
    var fifa15Message      by remember { mutableStateOf("") }
    var fifa15Error        by remember { mutableStateOf("") }
    var fifa15CacheBytes   by remember { mutableStateOf(0L) }

    // ── Storage info ──
    var freeBytes   by remember { mutableStateOf(0L) }
    var totalBytes  by remember { mutableStateOf(0L) }
    var backupCount by remember { mutableStateOf(0) }

    // ── Refresh functions ──
    fun refreshFifa16() {
        scope.launch(Dispatchers.IO) {
            fifa16ApkInstalled = isGameInstalled(context)
            fifa16DataReady = isDataReady()
            withContext(Dispatchers.Main) { fifa16Loading = false }
        }
    }
    fun refreshFifa15() {
        scope.launch(Dispatchers.IO) {
            fifa15ApkInstalled = fifa15Manager.isApkInstalled()
            fifa15DataReady = fifa15Manager.isDataInstalled()
            fifa15CacheBytes = fifa15Manager.cacheSize()
            withContext(Dispatchers.Main) { fifa15Loading = false }
        }
    }
    fun refreshStorage() {
        scope.launch(Dispatchers.IO) {
            freeBytes = GameUtils.freeBytesSdcard()
            totalBytes = GameUtils.totalBytesSdcard()
            backupCount = GameUtils.listBackups().size
        }
    }

    // ── Initial load ──
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            fifa16ApkInstalled = isGameInstalled(context)
            fifa16DataReady = isDataReady()
            fifa15ApkInstalled = fifa15Manager.isApkInstalled()
            fifa15DataReady = fifa15Manager.isDataInstalled()
            fifa15CacheBytes = fifa15Manager.cacheSize()
            freeBytes = GameUtils.freeBytesSdcard()
            totalBytes = GameUtils.totalBytesSdcard()
            backupCount = GameUtils.listBackups().size
        }
        fifa16Loading = false
        fifa15Loading = false
    }

    // ── FIFA 15 install action ──
    fun startFifa15Install() {
        if (fifa15Installing) return
        if (!StorageAccess.isGranted()) {
            StorageAccess.request(context)
            return
        }
        fifa15Installing = true
        fifa15Error = ""
        fifa15Progress = 0f
        fifa15Phase = "starting"
        fifa15Message = "Memulai instalasi..."
        scope.launch {
            val result = fifa15Manager.downloadAndInstall(
                onProgress = { phase, current, total, message ->
                    withContext(Dispatchers.Main) {
                        fifa15Phase = phase
                        fifa15Message = message
                        fifa15Progress = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    }
                }
            )
            withContext(Dispatchers.Main) {
                fifa15Installing = false
                result.onSuccess {
                    fifa15Message = "FIFA 15 data berhasil dipasang!"
                    fifa15Phase = "done"
                    fifa15Progress = 1f
                    refreshFifa15()
                }.onFailure { err ->
                    fifa15Error = err.message ?: "Gagal instalasi FIFA 15 data"
                    fifa15Phase = "error"
                }
            }
        }
    }

    // ── Layout ──
    Column(
        Modifier
            .fillMaxSize()
            .background(DlcBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ─── Header (cleaner) ───
        DlcHeader()

        // ─── Storage Permission Card (only if not granted) ───
        AnimatedVisibility(!filesAccessGranted, enter = fadeIn(), exit = fadeOut()) {
            DlcStoragePermissionCard(
                onGrant = { StorageAccess.request(context) }
            )
        }

        // ─── FIFA 16 Card (mod priority) ───
        DlcGameCardFifa16(
            loading = fifa16Loading,
            apkInstalled = fifa16ApkInstalled,
            dataReady = fifa16DataReady,
            onRefresh = { refreshFifa16() },
            onGoToPatches = { onNav(Page.DLC) },  // No-op (already on DLC) — patches section below
            onLaunchGame = { launchGame(context, GAME_PKG_16, "com.byfen.downloadzipsdk.MainActivity") },
            onDownloadApk = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FIFA16_APK_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            onApplyPatch = {
                // Open the full patch system UI (UpdateScreen) as an overlay
                showPatchTools = true
            }
        )

        // ─── FIFA 16 Patch System (summary card) ───
        DlcPatchSystemCard(
            filesAccessGranted = filesAccessGranted,
            onApplyPatch = {
                if (!StorageAccess.isGranted()) {
                    StorageAccess.request(context)
                } else {
                    // Open full patch system UI
                    showPatchTools = true
                }
            }
        )

        // ─── FIFA 15 Card (base data only, no mods) ───
        DlcGameCardFifa15(
            loading = fifa15Loading,
            apkInstalled = fifa15ApkInstalled,
            dataReady = fifa15DataReady,
            installing = fifa15Installing,
            phase = fifa15Phase,
            progress = fifa15Progress,
            message = fifa15Message,
            error = fifa15Error,
            cacheBytes = fifa15CacheBytes,
            onStartInstall = { startFifa15Install() },
            onRefresh = { refreshFifa15() },
            onLaunchGame = { launchGame(context, GAME_PKG_15, FIFA15_MAIN_ACTIVITY) },
            onDownloadApk = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FIFA15_APK_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            onClearCache = {
                scope.launch(Dispatchers.IO) {
                    val freed = fifa15Manager.clearCache()
                    withContext(Dispatchers.Main) {
                        refreshFifa15()
                        refreshStorage()
                    }
                }
            }
        )

        // ─── Storage Info Card ───
        DlcStorageInfoCard(
            freeBytes = freeBytes,
            totalBytes = totalBytes,
            backupCount = backupCount,
            onRefresh = { refreshStorage() }
        )

        // ─── Footer note (cleaner, no artificial ending) ───
        Spacer(Modifier.height(4.dp))
        Text(
            "DLavie Launcher v7.0 — DLC Manager",
            color = DlcMuted,
            fontSize = 11.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

// ─── Helper for lifecycle observer (avoid breaking cancellation) ──────────────
// (Removed — use kotlinx.coroutines.awaitCancellation() directly to match existing pattern)

// ─── UI Components ────────────────────────────────────────────────────────────

@Composable
private fun DlcHeader() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(DlcCardBgAlt, RoundedCornerShape(14.dp))
                .border(1.dp, DlcBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Extension, null, tint = DlcText, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "DLC & Data Manager",
                color = DlcText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
            Text(
                "Kelola APK, data, dan patch untuk FIFA 16 & FIFA 15",
                color = DlcSubText,
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DlcStoragePermissionCard(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DlcCardBg),
        border = BorderStroke(1.dp, DlcRed.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Security, null, tint = DlcRed, modifier = Modifier.size(22.dp))
                Text(
                    "Izin Akses File Diperlukan",
                    color = DlcText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
            Text(
                "Launcher butuh izin 'Akses semua file' untuk menulis data game ke folder Android/data dan Android/obb. " +
                "Tanpa izin ini, auto-download FIFA 15 dan apply patch FIFA 16 tidak akan berfungsi.",
                color = DlcSubText,
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                lineHeight = 17.sp
            )
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DlcText, contentColor = DlcBlack)
            ) {
                Text("Izinkan Akses File", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun DlcGameCardFifa16(
    loading: Boolean,
    apkInstalled: Boolean,
    dataReady: Boolean,
    onRefresh: () -> Unit,
    onGoToPatches: () -> Unit,
    onLaunchGame: () -> Unit,
    onDownloadApk: () -> Unit,
    onApplyPatch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DlcCardBg),
        border = BorderStroke(1.dp, DlcBorder)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF222222)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DL", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("FIFA 16 Mobile", color = DlcText, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Spacer(Modifier.width(8.dp))
                        DlcPill("MOD PRIORITY", DlcText)
                    }
                    Text("DLavie 26 Mod · Sports · Mod utama", color = DlcSubText, fontSize = 12.sp, fontFamily = InterFontFamily, maxLines = 1)
                }
                IconButtonRefresh(onRefresh)
            }

            // Status tiles (3 columns)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DlcStatusTile(
                    label = "APK",
                    value = if (loading) "…" else if (apkInstalled) "Terpasang" else "Belum ada",
                    ok = apkInstalled,
                    icon = Icons.Rounded.Android,
                    modifier = Modifier.weight(1f)
                )
                DlcStatusTile(
                    label = "Data",
                    value = if (loading) "…" else if (dataReady) "Siap" else "Belum siap",
                    ok = dataReady,
                    icon = Icons.Rounded.FolderOpen,
                    modifier = Modifier.weight(1f)
                )
                DlcStatusTile(
                    label = "Patch",
                    value = if (loading) "…" else readMarker().ifBlank { "—" }.take(8),
                    ok = readMarker().startsWith("v26"),
                    icon = Icons.Rounded.Extension,
                    modifier = Modifier.weight(1f)
                )
            }

            // Step-by-step actions
            DlcActionRow(
                step = "1",
                title = "Install APK FIFA 16",
                subtitle = "Unduh APK DLavie 26 (launcher-only, Android 14+ compatible)",
                actionText = if (apkInstalled) "Terpasang" else "Unduh APK",
                actionEnabled = !loading && !apkInstalled,
                actionDone = apkInstalled,
                onClick = onDownloadApk
            )
            DlcActionRow(
                step = "2",
                title = "Pasang Data & Patch Mod",
                subtitle = "Apply patch dari Dev Dashboard — mod files, kits, transfer, dll.",
                actionText = "Apply Patch",
                actionEnabled = !loading && apkInstalled,
                actionDone = dataReady,
                onClick = onApplyPatch
            )
            DlcActionRow(
                step = "3",
                title = "Mainkan FIFA 16",
                subtitle = "Launch game setelah APK + data siap.",
                actionText = "Play",
                actionEnabled = !loading && apkInstalled && dataReady,
                actionDone = false,
                isPrimary = true,
                onClick = onLaunchGame
            )
        }
    }
}

@Composable
private fun DlcGameCardFifa15(
    loading: Boolean,
    apkInstalled: Boolean,
    dataReady: Boolean,
    installing: Boolean,
    phase: String,
    progress: Float,
    message: String,
    error: String,
    cacheBytes: Long,
    onStartInstall: () -> Unit,
    onRefresh: () -> Unit,
    onLaunchGame: () -> Unit,
    onDownloadApk: () -> Unit,
    onClearCache: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DlcCardBg),
        border = BorderStroke(1.dp, DlcBorder)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("D15", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("FIFA 15 Mobile", color = DlcText, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        Spacer(Modifier.width(8.dp))
                        DlcPill("BASE ONLY", DlcSubText)
                    }
                    Text("DLavie 15 · Sports · Tanpa mod", color = DlcSubText, fontSize = 12.sp, fontFamily = InterFontFamily, maxLines = 1)
                }
                IconButtonRefresh(onRefresh)
            }

            // Status tiles (3 columns)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DlcStatusTile(
                    label = "APK",
                    value = if (loading) "…" else if (apkInstalled) "Terpasang" else "Belum ada",
                    ok = apkInstalled,
                    icon = Icons.Rounded.Android,
                    modifier = Modifier.weight(1f)
                )
                DlcStatusTile(
                    label = "Data",
                    value = if (loading) "…" else if (dataReady) "Siap" else "Belum siap",
                    ok = dataReady,
                    icon = Icons.Rounded.FolderOpen,
                    modifier = Modifier.weight(1f)
                )
                DlcStatusTile(
                    label = "Cache",
                    value = if (cacheBytes > 0) formatBytesHelper(cacheBytes) else "Kosong",
                    ok = false,
                    icon = Icons.Rounded.Storage,
                    modifier = Modifier.weight(1f)
                )
            }

            // Install progress (only when installing)
            AnimatedVisibility(installing, enter = fadeIn(), exit = fadeOut()) {
                DlcProgressCard(phase = phase, progress = progress, message = message)
            }

            // Error message
            AnimatedVisibility(error.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                DlcErrorBanner(message = error)
            }

            // Step-by-step actions
            DlcActionRow(
                step = "1",
                title = "Install APK FIFA 15",
                subtitle = "Unduh APK DLavie 15 (Android 16 compatible)",
                actionText = if (apkInstalled) "Terpasang" else "Unduh APK",
                actionEnabled = !loading && !installing && !apkInstalled,
                actionDone = apkInstalled,
                onClick = onDownloadApk
            )
            DlcActionRow(
                step = "2",
                title = "Auto-Download Data + OBB",
                subtitle = "DATA (72.6 MB) + OBB (1.1 GB) — auto-extract, tanpa ZArchiver",
                actionText = if (installing) "Memasang..." else if (dataReady) "Terpasang" else "Unduh & Pasang",
                actionEnabled = !loading && !installing && apkInstalled && !dataReady,
                actionDone = dataReady,
                isPrimary = !dataReady,
                onClick = onStartInstall
            )
            DlcActionRow(
                step = "3",
                title = "Mainkan FIFA 15",
                subtitle = "Launch game setelah APK + data siap.",
                actionText = "Play",
                actionEnabled = !loading && !installing && apkInstalled && dataReady,
                actionDone = false,
                isPrimary = dataReady,
                onClick = onLaunchGame
            )

            // Cache management (only if cache exists)
            AnimatedVisibility(cacheBytes > 0 && !installing, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onClearCache() },
                    shape = RoundedCornerShape(14.dp),
                    color = DlcCardBgAlt,
                    border = BorderStroke(1.dp, DlcBorder)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Storage, null, tint = DlcSubText, modifier = Modifier.size(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Cache: ${formatBytesHelper(cacheBytes)}", color = DlcText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Text("Ketuk untuk menghapus cache penghemat storage", color = DlcSubText, fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DlcPatchSystemCard(
    filesAccessGranted: Boolean,
    onApplyPatch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DlcCardBg),
        border = BorderStroke(1.dp, DlcBorder)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp)
                        .background(DlcCardBgAlt, RoundedCornerShape(12.dp))
                        .border(1.dp, DlcBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Extension, null, tint = DlcText, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Patch System (FIFA 16)", color = DlcText, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text("Sistem mod files dari GitHub Tree Sync — auto backup + rollback", color = DlcSubText, fontSize = 11.sp, fontFamily = InterFontFamily)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!filesAccessGranted) {
                    Icon(Icons.Rounded.Warning, null, tint = DlcYellow, modifier = Modifier.size(16.dp))
                    Text("Butuh izin Akses File untuk apply patch", color = DlcYellow, fontSize = 11.sp, fontFamily = InterFontFamily)
                } else {
                    Icon(Icons.Rounded.CheckCircle, null, tint = DlcGreen, modifier = Modifier.size(16.dp))
                    Text("Izin Akses File aktif — patch siap diapply", color = DlcGreen, fontSize = 11.sp, fontFamily = InterFontFamily)
                }
            }

            // Note: Full patch UI (apply, backup list, rollback) lives in the existing
            // UpdateScreen. Tapping the button below opens it as a full-screen overlay.
            Button(
                onClick = onApplyPatch,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (filesAccessGranted) DlcText else DlcCardBgAlt,
                    contentColor = if (filesAccessGranted) DlcBlack else DlcSubText
                ),
                enabled = true
            ) {
                Icon(Icons.Rounded.Extension, null, tint = if (filesAccessGranted) DlcBlack else DlcSubText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (filesAccessGranted) "Buka Patch System" else "Aktifkan izin file dulu",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun DlcStorageInfoCard(
    freeBytes: Long,
    totalBytes: Long,
    backupCount: Int,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DlcCardBg),
        border = BorderStroke(1.dp, DlcBorder)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp)
                        .background(DlcCardBgAlt, RoundedCornerShape(12.dp))
                        .border(1.dp, DlcBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Storage, null, tint = DlcText, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Storage Info", color = DlcText, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text("Ruang kosong & backup FIFA 16", color = DlcSubText, fontSize = 11.sp, fontFamily = InterFontFamily)
                }
                IconButtonRefresh(onRefresh)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DlcStatusTile(
                    label = "Kosong",
                    value = formatBytesHelper(freeBytes),
                    ok = freeBytes > 2L * 1024 * 1024 * 1024,  // > 2 GB
                    icon = Icons.Rounded.Storage,
                    modifier = Modifier.weight(1f)
                )
                DlcStatusTile(
                    label = "Total",
                    value = formatBytesHelper(totalBytes),
                    ok = true,
                    icon = Icons.Rounded.Storage,
                    modifier = Modifier.weight(1f)
                )
                DlcStatusTile(
                    label = "Backup",
                    value = "$backupCount file",
                    ok = backupCount > 0,
                    icon = Icons.Rounded.History,
                    modifier = Modifier.weight(1f)
                )
            }

            if (freeBytes in 1L..2L * 1024 * 1024 * 1024) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Warning, null, tint = DlcYellow, modifier = Modifier.size(14.dp))
                    Text(
                        "Ruang kurang dari 2 GB — auto-download FIFA 15 butuh ~1.2 GB",
                        color = DlcYellow,
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun DlcStatusTile(
    label: String,
    value: String,
    ok: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(if (ok) DlcGreen else DlcRed, tween(400), label = "tile_c")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DlcCardBgAlt,
        border = BorderStroke(1.dp, if (ok) DlcBorder else DlcRed.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
                Text(label, color = DlcSubText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
            }
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DlcActionRow(
    step: String,
    title: String,
    subtitle: String,
    actionText: String,
    actionEnabled: Boolean,
    actionDone: Boolean,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DlcCardBgAlt,
        border = BorderStroke(1.dp, if (isPrimary && actionEnabled) DlcBorderHi else DlcBorder)
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
                        if (actionDone) DlcGreen else if (isPrimary && actionEnabled) DlcText else DlcCardBg,
                        CircleShape
                    )
                    .border(1.dp, DlcBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (actionDone) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = DlcBlack, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        step,
                        color = if (isPrimary && actionEnabled) DlcBlack else DlcSubText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(title, color = DlcText, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
                Text(subtitle, color = DlcSubText, fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            }

            // Action button
            Button(
                onClick = onClick,
                enabled = actionEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPrimary && actionEnabled) DlcText else DlcCardBg,
                    contentColor = if (isPrimary && actionEnabled) DlcBlack else DlcSubText,
                    disabledContainerColor = if (actionDone) DlcGreen.copy(alpha = 0.2f) else DlcCardBgAlt,
                    disabledContentColor = if (actionDone) DlcGreen else DlcMuted
                ),
                modifier = Modifier.height(38.dp)
            ) {
                Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DlcProgressCard(phase: String, progress: Float, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DlcCardBgAlt,
        border = BorderStroke(1.dp, DlcBorderHi)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = DlcText
                )
                Text(
                    phaseLabel(phase),
                    color = DlcText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${(progress * 100).toInt()}%",
                    color = DlcText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = DlcText,
                trackColor = DlcCardBg,
            )
            Text(
                message,
                color = DlcSubText,
                fontSize = 11.sp,
                fontFamily = InterFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DlcErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DlcRed.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, DlcRed.copy(alpha = 0.4f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = DlcRed, modifier = Modifier.size(18.dp))
            Text(
                message,
                color = DlcRed,
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DlcPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            fontFamily = InterFontFamily
        )
    }
}

@Composable
private fun IconButtonRefresh(onClick: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        Modifier.size(36.dp)
            .clip(CircleShape)
            .border(1.dp, DlcBorder, CircleShape)
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Refresh, null, tint = DlcSubText, modifier = Modifier.size(18.dp))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

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

private fun formatBytesHelper(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unit = 0
    while (size >= 1024 && unit < units.lastIndex) {
        size /= 1024
        unit++
    }
    return String.format(Locale.US, "%.1f %s", size, units[unit])
}
