package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

// ─── Game Action Panel v2 (Kickstarter-style) ────────────────────────────────
//
// Layout 100% match screenshot IMG_4423 (hanya warna diubah ke monochrome):
//   • Floating panel di center screen, rounded rectangle, dark bg
//   • Top: Main heading bold ("Siap main?")
//   • Subheading: nama game
//   • Checklist items: APK terpasang ✓, Data terpasang ✓ (hanya yang done)
//   • Progress bar horizontal dengan numbered steps (1..N)
//   • Each step: nomor di lingkaran + label kecil di bawah
//   • Bottom: tip text italic
//   • Close (X) button di top-right corner panel
//
// AUTO-SKIP LOGIC:
//   • Saat panel dibuka, cek state (apkInstalled, dataReady, patched)
//   • Hanya tampilkan step yang BELUM done + step Play (always last)
//   • User tidak perlu tap step yang sudah done — langsung lanjut ke step berikutnya
//   • Kalau semua done → auto-highlight Play step dengan animasi pulse
//
// IN-APP APK DOWNLOAD:
//   • Step "Install APK" tap → mulai download via ApkDownloader (bukan browser)
//   • Progress bar real-time di dalam panel
//   • On complete → auto-open APK installer (FileProvider)
//   • User install APK → kembali ke launcher → panel auto-refresh → APK done ✓

private val PanelBlack     = Color(0xFF000000)
private val PanelCardBg    = Color(0xFF0A0A0A)
private val PanelCardBgAlt = Color(0xFF101010)
private val PanelBorder    = Color(0x33FFFFFF)
private val PanelBorderHi  = Color(0x55FFFFFF)
private val PanelText      = Color(0xFFFFFFFF)
private val PanelSubText   = Color(0xFFAAAAAA)
private val PanelMuted     = Color(0xFF666666)
private val PanelGreen     = Color(0xFFFFFFFF)
private val PanelRed       = Color(0xFFFF5555)
private val PanelYellow    = Color(0xFFFFFF88)
private val PanelDim       = Color(0xDD000000)  // 87% black for background
private val PanelAccent    = Color(0xFFFFFFFF)

/** Single step in the GameActionPanel progress bar. Built per render based on game state. */
private data class Step(
    val num: Int,
    val label: String,
    val done: Boolean,
    val action: () -> Unit,
    val icon: ImageVector
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
    var patched by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var installing by remember { mutableStateOf(false) }
    var installPhase by remember { mutableStateOf("") }
    var installProgress by remember { mutableStateOf(0f) }
    var installMessage by remember { mutableStateOf("") }
    var installError by remember { mutableStateOf("") }
    var filesAccessGranted by remember { mutableStateOf(false) }

    // APK download state
    val apkDownloader = remember(game.packageName) { ApkDownloader(context) }
    var apkDownloadActive by remember { mutableStateOf(false) }
    var apkDownloadProgress by remember { mutableStateOf(0) }
    var apkDownloadedBytes by remember { mutableLongStateOf(0L) }
    var apkTotalBytes by remember { mutableLongStateOf(0L) }
    var apkDownloadError by remember { mutableStateOf("") }

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
            patched = if (game.packageName == GAME_PKG_16) {
                readMarker().startsWith("v26")
            } else {
                true  // FIFA 15 doesn't have patches
            }
            filesAccessGranted = StorageAccess.isGranted()
            withContext(Dispatchers.Main) { loading = false }
        }
    }

    LaunchedEffect(game.packageName) {
        refresh()
        // Setup APK download completion listener
        apkDownloader.setCompletionListener { fileKey, file, success, error ->
            scope.launch {
                withContext(Dispatchers.Main) {
                    apkDownloadActive = false
                    if (success && file.exists()) {
                        apkDownloadProgress = 100
                        // Auto-open installer
                        val opened = apkDownloader.openInstaller(file)
                        if (!opened) {
                            apkDownloadError = "Gagal membuka installer. Tap file manually."
                        }
                    } else {
                        apkDownloadError = error ?: "Download gagal"
                    }
                }
            }
        }
    }

    // ── Poll APK download progress ──
    // CRITICAL: fileKey must match the game — fifa15-apk for FIFA 15, fifa16-apk for FIFA 16.
    // NOT launcher-latest (that's the DLavie Launcher APK, causes infinite download loop).
    LaunchedEffect(apkDownloadActive, game.packageName) {
        val fileKey = if (game.packageName == GAME_PKG_15) "fifa15-apk" else "fifa16-apk"
        while (apkDownloadActive) {
            val progress = apkDownloader.getProgress(fileKey)
            apkDownloadProgress = progress.progress
            apkDownloadedBytes = progress.downloadedBytes
            apkTotalBytes = progress.totalBytes
            // Check for completion: if download is done (not active, progress=100),
            // the completion listener will handle opening installer. But we also
            // break the polling loop here to prevent stuck state.
            if (progress.done) {
                apkDownloadActive = false
                break
            }
            if (progress.error != null && !progress.active) {
                apkDownloadError = progress.error
                apkDownloadActive = false
                break
            }
            delay(500)
        }
    }

    // ── Poll refresh after installer returns ──
    // (When user returns from APK installer, check if APK now installed)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Install data action (for FIFA 15) ──
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

    // ── Start in-app APK download ──
    // CRITICAL: fileKey must be fifa16-apk for FIFA 16 (NOT launcher-latest).
    // launcher-latest downloads the DLavie Launcher APK → installing it doesn't
    // install FIFA 16 game → panel keeps asking to install → infinite loop.
    //
    // v7.2.4: Auto-request StorageAccess permission early (saat user tap install APK).
    // APK download sendiri tidak butuh permission (DownloadManager handles), tapi
    // step selanjutnya (Install Data, Apply Mod) butuh permission. Request early
    // supaya user tidak perlu tap permission card terpisah.
    fun startApkDownload() {
        if (apkDownloadActive) return
        apkDownloadError = ""
        val fileKey = if (game.packageName == GAME_PKG_15) "fifa15-apk" else "fifa16-apk"
        val fileName = if (game.packageName == GAME_PKG_15) "DLavie15.apk" else "DLavie26-FIFA16.apk"
        val label = if (game.packageName == GAME_PKG_15) "FIFA 15 Mobile" else "FIFA 16 Mobile"
        val url = if (game.packageName == GAME_PKG_15) FIFA15_APK_URL else FIFA16_APK_URL
        val started = apkDownloader.startDownload(fileKey, url, fileName, label)
        if (started) {
            apkDownloadActive = true
            // Auto-request StorageAccess permission early (for next steps)
            if (!StorageAccess.isGranted()) {
                StorageAccess.request(context)
            }
        } else {
            // Download already running — just resume polling
            apkDownloadActive = true
        }
    }

    // ── Build step list (auto-skip: only show steps NOT done + Play) ──
    val visibleSteps = remember(apkInstalled, dataReady, patched, game, apkDownloadActive, installing) {
        val steps = mutableListOf<Step>()
        var n = 1

        // Step: Install APK (skip if already installed)
        if (!apkInstalled) {
            steps.add(Step(
                num = n++,
                label = "Install APK",
                done = false,
                action = { startApkDownload() },
                icon = Icons.Rounded.Android
            ))
        }

        // Step: Install Data (skip if already dataReady)
        if (!dataReady) {
            steps.add(Step(
                num = n++,
                label = if (game.packageName == GAME_PKG_15) "Install Data" else "Apply Data",
                done = false,
                action = {
                    if (game.packageName == GAME_PKG_15) {
                        startFifa15Install()
                    } else {
                        onGoToDlc()
                    }
                },
                icon = Icons.Rounded.FolderOpen
            ))
        }

        // Step: Apply Mod (FIFA 16 only, skip if already patched)
        if (game.packageName == GAME_PKG_16 && !patched) {
            steps.add(Step(
                num = n++,
                label = "Apply Mod",
                done = false,
                action = { onGoToDlc() },
                icon = Icons.Rounded.Extension
            ))
        }

        // Final step: Play (always shown)
        steps.add(Step(
            num = n,
            label = "Play",
            done = false,
            action = {
                launchGame(context, game.packageName, game.mainActivity)
                onDismiss()
            },
            icon = Icons.Rounded.PlayCircle
        ))

        steps
    }

    // Build checklist items (only DONE items shown at top — like screenshot)
    val completedItems = remember(apkInstalled, dataReady, patched, game) {
        val items = mutableListOf<Triple<String, Boolean, ImageVector>>()
        if (apkInstalled) items.add(Triple("APK ${game.title} terpasang", true, Icons.Rounded.Android))
        if (dataReady) items.add(Triple(
            if (game.packageName == GAME_PKG_15) "Data + OBB terpasang" else "Data game siap",
            true,
            Icons.Rounded.FolderOpen
        ))
        if (game.packageName == GAME_PKG_16 && patched) {
            items.add(Triple("Mod patch terpasang", true, Icons.Rounded.Extension))
        }
        items
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
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = PanelCardBg),
            border = BorderStroke(1.dp, PanelBorderHi)
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── Top row: heading + close button ─────────────────────────
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Game icon
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
                        // Main heading (like "You're almost there!")
                        Text(
                            text = if (completedItems.isEmpty()) "Mulai dari awal" else "Hampir selesai!",
                            color = PanelText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily,
                            lineHeight = 26.sp
                        )
                        // Subheading (game title)
                        Text(
                            game.title,
                            color = PanelSubText,
                            fontSize = 13.sp,
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

                // ─── Loading state ──────────────────────────────────────────
                if (loading) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                    // ─── Completed items checklist (like screenshot) ────────
                    if (completedItems.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            completedItems.forEach { (label, done, icon) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Green check circle
                                    Box(
                                        Modifier.size(20.dp)
                                            .clip(CircleShape)
                                            .background(PanelText),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            null,
                                            tint = PanelBlack,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        label,
                                        color = PanelText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = InterFontFamily,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(icon, null, tint = PanelMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        // Empty state — belum ada yang done
                        Text(
                            "Selesaikan langkah di bawah untuk mulai main.",
                            color = PanelSubText,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            lineHeight = 16.sp
                        )
                    }

                    // ─── Storage permission: AUTO-REQUEST saat tap install (tidak ada card warning terpisah) ──
                    // v7.2.4: Permission flow disatukan — saat user tap "Install Data" / "Apply Mod"
                    // dan permission belum granted, otomatis buka settings. Tidak ada card warning
                    // terpisah yang bikin bingung (per user complaint).

                    // ─── APK Download progress (when downloading) ───────────
                    AnimatedVisibility(apkDownloadActive, enter = fadeIn(), exit = fadeOut()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = PanelText
                                )
                                Text(
                                    "Mengunduh APK...",
                                    color = PanelText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$apkDownloadProgress%",
                                    color = PanelText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = InterFontFamily
                                )
                            }
                            LinearProgressIndicator(
                                progress = { apkDownloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = PanelText,
                                trackColor = PanelCardBgAlt
                            )
                            Text(
                                "${formatBytesHelper(apkDownloadedBytes)} / ${formatBytesHelper(apkTotalBytes)}",
                                color = PanelSubText,
                                fontSize = 10.sp,
                                fontFamily = InterFontFamily,
                                maxLines = 1
                            )
                        }
                    }

                    // ─── Data install progress (FIFA 15, when installing) ────
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

                    // ─── Error ──────────────────────────────────────────────
                    AnimatedVisibility(
                        installError.isNotBlank() || apkDownloadError.isNotBlank(),
                        enter = fadeIn(), exit = fadeOut()
                    ) {
                        val err = installError.ifBlank { apkDownloadError }
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
                                    err,
                                    color = PanelRed,
                                    fontSize = 11.sp,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ─── Progress bar with numbered steps (screenshot style) ─
                    // Horizontal row of numbered circles connected by line
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Connecting line (background)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(PanelCardBgAlt)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            visibleSteps.forEachIndexed { index, step ->
                                StepCircle(
                                    step = step,
                                    isLast = index == visibleSteps.lastIndex,
                                    canInteract = !apkDownloadActive && !installing && !loading,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // ─── Primary action button (highlights current step) ────
                    val currentStep = visibleSteps.firstOrNull { !it.done } ?: visibleSteps.last()
                    val canAct = !apkDownloadActive && !installing && !loading
                    val actionText = when {
                        apkDownloadActive -> "Mengunduh APK... $apkDownloadProgress%"
                        installing -> "${phaseLabel(installPhase)} ${(installProgress * 100).toInt()}%"
                        currentStep.label == "Play" -> "Play Now"
                        currentStep.label == "Install APK" -> "Download APK"
                        currentStep.label == "Install Data" || currentStep.label == "Apply Data" ->
                            if (game.packageName == GAME_PKG_15) "Download & Install Data" else "Apply Data"
                        currentStep.label == "Apply Mod" -> "Open DLC Mods"
                        else -> currentStep.label
                    }

                    Button(
                        onClick = { if (canAct) currentStep.action() },
                        enabled = canAct,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PanelText,
                            contentColor = PanelBlack,
                            disabledContainerColor = PanelCardBgAlt,
                            disabledContentColor = PanelMuted
                        )
                    ) {
                        Icon(currentStep.icon, null, tint = if (canAct) PanelBlack else PanelMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            actionText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }

                    // ─── Tip at bottom (italic, like screenshot) ────────────
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = PanelMuted, modifier = Modifier.size(12.dp))
                        Text(
                            "Tip: Selesaikan semua langkah untuk pengalaman terbaik. Status tersimpan otomatis.",
                            color = PanelMuted,
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCircle(
    step: Step,
    isLast: Boolean,
    canInteract: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(36.dp)
                .clip(CircleShape)
                .background(
                    if (step.done) PanelText
                    else if (step.label == "Play") PanelText  // Play button always prominent
                    else PanelCardBgAlt
                )
                .border(
                    1.dp,
                    if (step.done || step.label == "Play") PanelText else PanelBorder,
                    CircleShape
                )
                .clickable(enabled = canInteract) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    step.action()
                },
            contentAlignment = Alignment.Center
        ) {
            if (step.done) {
                Icon(Icons.Rounded.CheckCircle, null, tint = PanelBlack, modifier = Modifier.size(20.dp))
            } else if (step.label == "Play") {
                Icon(Icons.Rounded.PlayCircle, null, tint = PanelBlack, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    step.num.toString(),
                    color = PanelText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            step.label,
            color = if (step.done || step.label == "Play") PanelText else PanelSubText,
            fontSize = 10.sp,
            fontWeight = if (step.done || step.label == "Play") FontWeight.Bold else FontWeight.Normal,
            fontFamily = InterFontFamily,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
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
