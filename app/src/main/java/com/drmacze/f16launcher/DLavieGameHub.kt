package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.text.format.DateFormat
import android.view.InputDevice
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB v281 — 100% Match GameHub Guangzhou (PlayStore reference)
// ═══════════════════════════════════════════════════════════════════════════
// Pixel-perfect match to GameHub by Guangzhou Bat Network Technology:
//   - Background: Pure black #000000
//   - Game cards: 160x240dp (2:3 ratio), full-bleed artwork, 16dp radius
//   - Card top bar: PS logo + platform + status badge (green/orange)
//   - Card bottom: 48dp overlay with title + subtitle + action
//   - Top bar: hamburger, LB, "Dashboard", RB, search, battery, time, back
//   - Bottom bar: Y+Search (left), Menu+hamburger (right)
//   - Drawer: 280dp wide, dark blue-gray #121826, teal #009688 accent
//   - Tabs: 32dp pills with number badges
//   - LB/RB: 40x40dp dark gray #1E1E1E rounded squares
// ═══════════════════════════════════════════════════════════════════════════

// ── Design tokens (exact match to GameHub Guangzhou) ──
private val GHBg = Color(0xFF000000)              // Pure black
private val GHDrawerBg = Color(0xFF121826)        // Dark blue-gray
private val GHButtonBg = Color(0xFF1E1E1E)        // Dark gray (LB/RB/buttons)
private val GHDivider = Color(0xFF333333)         // Medium gray dividers
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextGray = Color(0xFFAAAAAA)        // Subtitle gray
private val GHTextDim = Color(0xFF666666)         // Inactive text
private val GHAccent = Color(0xFF009688)          // Teal accent
private val GHAccentDim = Color(0xFF00796B)
private val GHGreen = Color(0xFF4CAF50)           // ONLINE status
private val GHAmber = Color(0xFFFF9800)           // MAINT status
private val GHRed = Color(0xFFEF4444)
private val GHBlue = Color(0xFF2196F3)            // Progress bar

// ── Self-contained data types ──
private enum class GHServerStatus { ONLINE, MAINTENANCE, OFFLINE, BUSY }

private data class GHGameItem(
    val title: String,
    val subtitle: String,
    val packageName: String,
    val coverGradient: List<Color>,
    val coverText: String,
    val platform: String = "PS4",  // PS-style platform label
    val serverStatus: GHServerStatus = GHServerStatus.ONLINE,
    val sizeMb: String = "",
    val apkUrl: String = ""
)

private data class GHUserGame(
    val title: String,
    val packageName: String,
    val sourcePath: String
)

// ── Constants ──
private const val GH_GAME_PKG_16 = "com.ea.gp.fifaworld"
private const val GH_GAME_PKG_15 = "com.ea.game.fifa15_row"
private const val GH_FIFA16_APK_URL = "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/DLavie26.apk"
private const val GH_FIFA15_APK_URL = "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/DLavie15.apk"
private const val GH_PREFS_USER_GAMES = "gh_user_games"

// ── Helpers ──
private fun ghIsPackageInstalled(context: Context, pkg: String): Boolean = try {
    context.packageManager.getPackageInfo(pkg, 0); true
} catch (_: Throwable) { false }

private fun ghLaunchGame(context: Context, pkg: String): Boolean = try {
    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
    if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent); true } else false
} catch (_: Throwable) { false }

private fun ghGetBatteryLevel(context: Context): Int = try {
    (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
} catch (_: Exception) { 100 }

private fun ghGetCurrentTime(context: Context): String = try {
    val is24 = DateFormat.is24HourFormat(context)
    SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date())
} catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

private fun ghGetStorageInfo(): Pair<Long, Long> = try {
    val stat = StatFs("/sdcard"); Pair(stat.availableBytes, stat.totalBytes)
} catch (_: Exception) { Pair(0L, 0L) }

private fun ghFormatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1fGB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1fMB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1fKB".format(bytes / 1_000.0)
    else -> "${bytes}B"
}

private fun ghLoadUserGames(context: Context): List<GHUserGame> = try {
    val prefs = context.getSharedPreferences(GH_PREFS_USER_GAMES, Context.MODE_PRIVATE)
    val raw = prefs.getString("games", "") ?: ""
    if (raw.isBlank()) emptyList()
    else raw.split("\n").mapNotNull { line ->
        val parts = line.split("|")
        if (parts.size >= 3) GHUserGame(parts[0], parts[1], parts[2]) else null
    }
} catch (_: Exception) { emptyList() }

private fun ghSaveUserGames(context: Context, games: List<GHUserGame>) = try {
    val prefs = context.getSharedPreferences(GH_PREFS_USER_GAMES, Context.MODE_PRIVATE)
    prefs.edit().putString("games", games.joinToString("\n") { "${it.title}|${it.packageName}|${it.sourcePath}" }).apply()
} catch (_: Exception) {}

// ═══════════════════════════════════════════════════════════════════════════
// TRANSITION — hexagon logo + typing DLAVIE + loading messages
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    var typedText by remember { mutableStateOf("") }
    var loadingMsg by remember { mutableStateOf("") }

    val fullText = "DLAVIE"
    val loadingMessages = listOf(
        "Memuat aset game...",
        "Menata antarmuka...",
        "Memindai data pengguna...",
        "Menghubungkan ke server...",
        "Menyiapkan GameHub..."
    )

    LaunchedEffect(visible) {
        if (visible) {
            phase = 0; delay(800); phase = 1; delay(800); phase = 2
            for (i in fullText.indices) { typedText = fullText.substring(0, i + 1); delay(120) }
            delay(400); phase = 3
            for (msg in loadingMessages) { loadingMsg = msg; delay(700) }
            loadingMsg = ""; delay(300); phase = 4; delay(600); onComplete(); phase = 5
        }
    }

    if (visible && phase < 5) {
        Box(Modifier.fillMaxSize().background(GHBg), contentAlignment = Alignment.Center) {
            val logoAlpha by animateFloatAsState(when (phase) { 0 -> 0f; 1 -> 1f; 2 -> 1f; 3 -> 1f; 4 -> 0f; else -> 0f }, tween(800, easing = FastOutSlowInEasing), label = "la")
            val logoScale by animateFloatAsState(when (phase) { 0 -> 0.7f; 1 -> 1f; 2 -> 1f; 3 -> 1f; 4 -> 1.15f; else -> 1f }, tween(800, easing = FastOutSlowInEasing), label = "ls")
            val textAlpha by animateFloatAsState(when (phase) { 0 -> 0f; 1 -> 0f; 2 -> 1f; 3 -> 1f; 4 -> 0f; else -> 0f }, tween(400, easing = FastOutSlowInEasing), label = "ta")
            val fadeOut by animateFloatAsState(if (phase == 4) 0f else 1f, tween(600, easing = FastOutSlowInEasing), label = "fo")
            val msgAlpha by animateFloatAsState(when (phase) { 3 -> 1f; 4 -> 0f; else -> 0f }, tween(300, easing = FastOutSlowInEasing), label = "ma")

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { this.alpha = fadeOut }) {
                Box(
                    Modifier.size(72.dp)
                        .graphicsLayer { scaleX = logoScale; scaleY = logoScale; this.alpha = logoAlpha }
                        .clip(androidx.compose.foundation.shape.GenericShape { _, _ ->
                            val r = 70f
                            moveTo(0f, -r); lineTo(r * 0.866f, -r * 0.5f); lineTo(r * 0.866f, r * 0.5f)
                            lineTo(0f, r); lineTo(-r * 0.866f, r * 0.5f); lineTo(-r * 0.866f, -r * 0.5f); close()
                        })
                        .background(GHTextWhite),
                    contentAlignment = Alignment.Center
                ) { Text("DL", color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Black) }

                Spacer(Modifier.height(20.dp))
                Text(typedText, color = GHTextWhite, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.graphicsLayer { this.alpha = textAlpha })
                Spacer(Modifier.height(16.dp))
                if (loadingMsg.isNotEmpty()) {
                    Text(loadingMsg, color = GHTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.graphicsLayer { this.alpha = msgAlpha })
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(
    onExit: () -> Unit = {},
    onNav: (Page) -> Unit = {},
    onGameClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTransition by remember { mutableStateOf(true) }

    // ── Immersive mode ──
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                val controller = window.insetsController
                controller?.hide(WindowInsets.Type.systemBars())
                controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
            }
        }
        onDispose {
            activity?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(true)
                    window.insetsController?.show(WindowInsets.Type.systemBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    var currentTime by remember { mutableStateOf(ghGetCurrentTime(context)) }
    var batteryLevel by remember { mutableStateOf(ghGetBatteryLevel(context)) }
    LaunchedEffect(Unit) { while (true) { currentTime = ghGetCurrentTime(context); batteryLevel = ghGetBatteryLevel(context); delay(30_000) } }

    var currentScreen by remember { mutableStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val dlavieGames = remember {
        listOf(
            GHGameItem(
                title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod",
                packageName = GH_GAME_PKG_16,
                coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                coverText = "DL", platform = "PS4",
                serverStatus = GHServerStatus.ONLINE, sizeMb = "34 MB", apkUrl = GH_FIFA16_APK_URL
            ),
            GHGameItem(
                title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod",
                packageName = GH_GAME_PKG_15,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15", platform = "PS4",
                serverStatus = GHServerStatus.MAINTENANCE, sizeMb = "22 MB", apkUrl = GH_FIFA15_APK_URL
            )
        )
    }

    var userGames by remember { mutableStateOf<List<GHUserGame>>(emptyList()) }
    LaunchedEffect(selectedTab, currentScreen) {
        if (selectedTab == 1) userGames = withContext(Dispatchers.IO) { ghLoadUserGames(context) }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { try { context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) {} }
    }

    var contextMenuGame by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    Box(Modifier.fillMaxSize().background(GHBg)) {
        if (!showTransition) {
            when (currentScreen) {
                0 -> GHHomeScreen(context, scope, dlavieGames, userGames, selectedTab, { selectedTab = it }, { pkg -> contextMenuGame = Pair(pkg, false) }, apkPickerLauncher, currentTime, batteryLevel, { showDrawer = true }, { onExit() })
                1 -> GHDownloadScreen(context) { currentScreen = 0 }
                2 -> GHSettingsScreen(context) { currentScreen = 0 }
            }
            if (showDrawer) {
                GHDrawer(currentScreen = currentScreen, onSelect = { screen -> currentScreen = screen; showDrawer = false }, onDismiss = { showDrawer = false }, onExit = { showDrawer = false; onExit() })
            }
            contextMenuGame?.let { (pkg, isUser) ->
                GHContextMenu(pkg, isUser, ghIsPackageInstalled(context, pkg),
                    { contextMenuGame = null }, { contextMenuGame = null; ghLaunchGame(context, pkg) },
                    { contextMenuGame = null },
                    { contextMenuGame = null; try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {} },
                    { contextMenuGame = null; val u = userGames.filter { it.packageName != pkg }; ghSaveUserGames(context, u); userGames = u },
                    { contextMenuGame = null; try { File("/sdcard/Android/data/$pkg").deleteRecursively() } catch (_: Exception) {} }
                )
            }
        }
        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HOME SCREEN — Exact GameHub Guangzhou layout
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHHomeScreen(
    context: Context, scope: kotlinx.coroutines.CoroutineScope,
    dlavieGames: List<GHGameItem>, userGames: List<GHUserGame>,
    selectedTab: Int, onTabSelect: (Int) -> Unit,
    onContextMenu: (String) -> Unit,
    apkPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    currentTime: String, batteryLevel: Int,
    onMenu: () -> Unit, onExit: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // ── TOP BAR (56dp, exact layout: hamburger, LB, title, RB, search, battery, time, back) ──
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Menu, "Menu", tint = GHTextWhite, modifier = Modifier.size(24.dp).clickable { onMenu() })
            Spacer(Modifier.width(16.dp))
            GHCtrlBtn("LB")
            Spacer(Modifier.width(12.dp))
            Text("Dashboard", color = GHTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(12.dp))
            GHCtrlBtn("RB")
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Rounded.Search, "Search", tint = GHTextWhite, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.BatteryFull, "Battery", tint = GHTextWhite, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("$batteryLevel%", color = GHTextWhite, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.Schedule, "Time", tint = GHTextWhite, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(currentTime, color = GHTextWhite, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowBack, "Exit", tint = GHTextWhite, modifier = Modifier.size(24.dp).clickable { onExit() })
        }

        // ── CATEGORY TABS (32dp pills with number badges) ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GHPillTab("DLavie", dlavieGames.size, selectedTab == 0) { onTabSelect(0) }
            GHPillTab("My Library", userGames.size, selectedTab == 1) { onTabSelect(1) }
            if (selectedTab == 1) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(GHButtonBg).clickable { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Add, "Add", tint = GHTextWhite, modifier = Modifier.size(20.dp)) }
            }
        }

        // ── GAME CAROUSEL (160x240 cards, 16dp gap, 16dp edge padding) ──
        Box(
            Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTab == 0) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(dlavieGames) { game ->
                        GHGameCard(game, ghIsPackageInstalled(context, game.packageName)) { onContextMenu(game.packageName) }
                    }
                }
            } else {
                if (userGames.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SportsEsports, null, tint = GHTextDim, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No games yet", color = GHTextGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap + to import an APK", color = GHTextDim, fontSize = 11.sp)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(userGames) { ug ->
                            GHUserCard(ug, ghIsPackageInstalled(context, ug.packageName),
                                onClick = {
                                    if (ghIsPackageInstalled(context, ug.packageName)) ghLaunchGame(context, ug.packageName)
                                    else {
                                        val f = File(ug.sourcePath)
                                        if (f.exists()) {
                                            try {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", f)
                                                context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) })
                                            } catch (_: Exception) {}
                                        }
                                    }
                                },
                                onLongClick = { onContextMenu(ug.packageName) }
                            )
                        }
                    }
                }
            }
        }

        // ── BOTTOM BAR (56dp, Y+Search left, Menu+hamburger right) ──
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GHCtrlBtn("Y")
                Spacer(Modifier.width(6.dp))
                Text("Search", color = GHTextGray, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onMenu() }) {
                Text("Menu", color = GHTextGray, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(GHButtonBg), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Menu, null, tint = GHTextGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAME CARD — 160x240dp, full-bleed artwork, PS-style top bar + status badge
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGameCard(game: GHGameItem, isInstalled: Boolean, onLongClick: () -> Unit) {
    // 160dp × 240dp (2:3 aspect ratio), 16dp radius
    Card(
        Modifier.width(160.dp).height(240.dp).combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GHButtonBg),
        border = null  // No border per reference
    ) {
        Box(Modifier.fillMaxSize()) {
            // Full-bleed artwork (gradient + cover text)
            Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(game.coverText, color = GHTextWhite, fontSize = 36.sp, fontWeight = FontWeight.Black)
            }

            // Top gradient overlay for top bar readability
            Box(
                Modifier.fillMaxWidth().height(48.dp).align(Alignment.TopStart)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))
            )

            // Bottom gradient overlay for info readability
            Box(
                Modifier.fillMaxWidth().height(96.dp).align(Alignment.BottomStart)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
            )

            // ── TOP BAR (32dp): PS platform label + status badge ──
            Row(
                Modifier.fillMaxWidth().align(Alignment.TopStart).padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform label (PS4 style)
                Text(game.platform, color = GHTextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                // Status badge (green ONLINE / orange MAINT)
                val (sc, st) = when (game.serverStatus) {
                    GHServerStatus.ONLINE -> Pair(GHGreen, "ONLINE")
                    GHServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT")
                    GHServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE")
                    GHServerStatus.BUSY -> Pair(GHAmber, "BUSY")
                }
                Box(
                    Modifier.clip(RoundedCornerShape(4.dp)).background(sc).padding(horizontal = 6.dp, vertical = 2.dp)
                ) { Text(st, color = GHTextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            }

            // ── BOTTOM SECTION (48dp): title + subtitle + action ──
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(game.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(game.subtitle, color = GHTextGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHCtrlBtn("A", size = 24)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isInstalled) "Launch" else "Install", color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHUserCard(userGame: GHUserGame, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        Modifier.width(160.dp).height(240.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GHButtonBg),
        border = null
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
                contentAlignment = Alignment.Center
            ) {
                val icon = remember(userGame.packageName) { try { context.packageManager.getApplicationIcon(userGame.packageName) } catch (_: Exception) { null } }
                if (icon != null) AsyncImage(model = icon, contentDescription = userGame.title, modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit)
                else Icon(Icons.Rounded.SportsEsports, null, tint = GHTextDim, modifier = Modifier.size(40.dp))
            }
            Box(Modifier.fillMaxWidth().height(96.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(userGame.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(userGame.packageName.take(20), color = GHTextGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHCtrlBtn("A", size = 24)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isInstalled) "Launch" else "Install", color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DOWNLOAD SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDownloadScreen(context: Context, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val (availStorage, totalStorage) = remember { ghGetStorageInfo() }
    val gameStorage = remember {
        try { val dir = File("/sdcard/Android/data/$GH_GAME_PKG_16"); if (dir.exists()) dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() else 0L } catch (_: Exception) { 0L }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = GHTextWhite, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Text("Download", color = GHTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GHPillTab("Download Task", 0, selectedTab == 0) { selectedTab = 0 }
            GHPillTab("Game Management", 0, selectedTab == 1) { selectedTab = 1 }
        }

        Column(Modifier.fillMaxWidth().weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Storage card
            Surface(shape = RoundedCornerShape(12.dp), color = GHButtonBg) {
                Column(Modifier.padding(16.dp)) {
                    Text("STORAGE", color = GHTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Game", color = GHTextDim, fontSize = 12.sp); Text(ghFormatBytes(gameStorage), color = GHTextWhite, fontSize = 12.sp) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Other", color = GHTextDim, fontSize = 12.sp); Text(ghFormatBytes(totalStorage - availStorage - gameStorage), color = GHTextWhite, fontSize = 12.sp) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Available", color = GHTextDim, fontSize = 12.sp); Text(ghFormatBytes(availStorage), color = GHGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    val usedPct = if (totalStorage > 0) ((totalStorage - availStorage).toFloat() / totalStorage.toFloat()) else 0f
                    LinearProgressIndicator(progress = { usedPct }, modifier = Modifier.fillMaxWidth().height(4.dp), color = GHBlue, trackColor = GHDivider)
                }
            }

            if (selectedTab == 0) {
                Text("Download Soon (0)", color = GHTextGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(12.dp), color = GHButtonBg, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("No Queued Tasks", color = GHTextDim, fontSize = 13.sp) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Completed", color = GHTextGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Clear All", color = GHAccent, fontSize = 12.sp, modifier = Modifier.clickable { })
                }
                Surface(shape = RoundedCornerShape(12.dp), color = GHButtonBg, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("No completed tasks", color = GHTextDim, fontSize = 13.sp) }
                }
            } else {
                val games = listOf(Triple("FIFA 16 Mobile", GH_GAME_PKG_16, "DLavie 26 Mod"), Triple("FIFA 15 Mobile", GH_GAME_PKG_15, "DLavie 15 Mod"))
                games.forEach { (title, pkg, subtitle) ->
                    val installed = ghIsPackageInstalled(context, pkg)
                    Surface(shape = RoundedCornerShape(12.dp), color = GHButtonBg, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(GHDrawerBg), contentAlignment = Alignment.Center) { Text(title.take(2), color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Black) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(subtitle, color = GHTextDim, fontSize = 11.sp)
                                Text(if (installed) "Installed" else "Not installed", color = if (installed) GHGreen else GHTextDim, fontSize = 10.sp)
                            }
                            if (installed) {
                                IconButton(onClick = { try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {} }) { Icon(Icons.Rounded.Delete, "Uninstall", tint = GHRed.copy(0.7f), modifier = Modifier.size(20.dp)) }
                            } else {
                                IconButton(onClick = {}) { Icon(Icons.Rounded.Download, "Install", tint = GHAccent, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) { GHCtrlBtn("B"); Spacer(Modifier.width(6.dp)); Text("Back", color = GHTextGray, fontSize = 12.sp, modifier = Modifier.clickable { onBack() }) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHSettingsScreen(context: Context, onBack: () -> Unit) {
    var gamepadCount by remember { mutableStateOf(0) }
    var gamepadNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val ds = InputDevice.getDeviceIds(); val gps = mutableListOf<String>()
            for (id in ds) { val d = InputDevice.getDevice(id); if (d != null) { val s = d.sources; if (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) gps.add(d.name) } }
            gamepadCount = gps.size; gamepadNames = gps
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = GHTextWhite, modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Text("Settings", color = GHTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("GAMEPAD", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHButtonBg) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SportsEsports, null, tint = if (gamepadCount > 0) GHGreen else GHTextDim, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Gamepad Connection", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(if (gamepadCount > 0) "$gamepadCount gamepad(s) connected" else "No gamepad detected", color = if (gamepadCount > 0) GHGreen else GHTextDim, fontSize = 12.sp) }
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (gamepadCount > 0) GHGreen else GHTextDim))
                    }
                    if (gamepadNames.isNotEmpty()) { Spacer(Modifier.height(10.dp)); gamepadNames.forEach { n -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) { Box(Modifier.size(6.dp).clip(CircleShape).background(GHGreen)); Spacer(Modifier.width(8.dp)); Text(n, color = GHTextGray, fontSize = 12.sp) } } }
                }
            }
            Text("DISPLAY", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHButtonBg) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ScreenRotation, null, tint = GHTextGray, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Auto Rotate", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Landscape mode in GameHub", color = GHTextDim, fontSize = 12.sp) }
                    Box(Modifier.size(24.dp).clip(CircleShape).background(GHGreen), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp)) }
                }
            }
            Text("ABOUT", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHButtonBg) {
                Column(Modifier.padding(16.dp)) { Text("DLavie GameHub", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Cloud Gaming Platform v7.9.83", color = GHTextDim, fontSize = 12.sp) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) { Row(verticalAlignment = Alignment.CenterVertically) { GHCtrlBtn("B"); Spacer(Modifier.width(6.dp)); Text("Back", color = GHTextGray, fontSize = 12.sp, modifier = Modifier.clickable { onBack() }) } }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SIDE DRAWER — 280dp wide, dark blue-gray #121826, teal avatar
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDrawer(currentScreen: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable { onDismiss() }) {
        Box(Modifier.fillMaxHeight().width(280.dp).background(GHDrawerBg).clickable {}) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                // Profile section (56dp avatar, teal bg)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(GHAccent), contentAlignment = Alignment.Center) { Text("D", color = GHTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("DLavie Player", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("@user", color = GHTextDim, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(28.dp))
                GHDrawerItem(Icons.Rounded.Home, "Home", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.SportsEsports, "Game", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.Download, "Download", currentScreen == 1) { onSelect(1) }
                GHDrawerItem(Icons.Rounded.Settings, "Settings", currentScreen == 2) { onSelect(2) }
                Spacer(Modifier.weight(1f))
                GHDrawerItem(Icons.Rounded.Close, "Exit GameHub", false) { onExit() }
            }
        }
    }
}

@Composable
private fun GHDrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (selected) GHButtonBg else Color.Transparent).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) GHAccent else GHTextGray, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (selected) GHTextWhite else GHTextGray, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS — exact GameHub style
// ═══════════════════════════════════════════════════════════════════════════

/**
 * LB/RB/Y/B/A control button — 40x40dp dark gray #1E1E1E rounded square
 * (matches GameHub Guangzhou reference exactly)
 */
@Composable
private fun GHCtrlBtn(label: String, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(8.dp)).background(GHButtonBg),
        contentAlignment = Alignment.Center
    ) { Text(label, color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

/**
 * Category pill tab — 32dp height, 16dp radius, number badge for count
 */
@Composable
private fun GHPillTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height(32.dp).clip(RoundedCornerShape(16.dp))
            .background(if (selected) GHButtonBg else Color.Transparent)
            .clickable { onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) GHTextWhite else GHTextDim, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(20.dp).clip(CircleShape).background(if (selected) GHAccent else GHDivider), contentAlignment = Alignment.Center) {
                Text(count.toString(), color = GHTextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CONTEXT MENU
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHContextMenu(pkg: String, isUser: Boolean, installed: Boolean, onDismiss: () -> Unit, onLaunch: () -> Unit, onView: () -> Unit, onUninstall: () -> Unit, onRemove: () -> Unit, onClear: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Options", color = GHTextWhite, fontWeight = FontWeight.Black) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (installed) { GHMenuItem(Icons.Rounded.PlayArrow, "Launch Game", onLaunch) }
            GHMenuItem(Icons.Rounded.Info, "View Details", onView)
            if (!isUser && installed) { GHMenuItem(Icons.Rounded.CleaningServices, "Clear Data Only", onClear) }
            if (isUser) { if (installed) { GHMenuItem(Icons.Rounded.Delete, "Uninstall Game", onUninstall) }; GHMenuItem(Icons.Rounded.RemoveCircle, "Remove from Library", onRemove) }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GHTextGray) } },
        containerColor = Color(0xFF111111)
    )
}

@Composable
private fun GHMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GHTextGray, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(14.dp)); Text(label, color = GHTextWhite, fontSize = 14.sp)
    }
}
