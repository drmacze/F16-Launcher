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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB v279 — Self-contained design system
// ═══════════════════════════════════════════════════════════════════════════
// Match GameHub PlayStore reference:
//   - Background: dark blue gradient (#0A0A1A → #050508)
//   - Cards: SOLID with firm border (NOT glass effect)
//   - Accent: soft blue (#6B8AFF), NOT cyan
//   - Shapes: consistent 8-12dp radius
//   - LB/RB: square-ish (4dp), firm border
//   - Drawer: wider (0.45f), bigger icons (28dp)
//   - Spacing: spacious, not cramped
//
// This file is SELF-CONTAINED — all dependencies inline.
// Only external dep: coil (added to build.gradle).
// ═══════════════════════════════════════════════════════════════════════════

// ── Design tokens ──
private val GHBgGradientStart = Color(0xFF0A0A1A)
private val GHBgGradientEnd = Color(0xFF050508)
private val GHDrawerBg = Color(0xFF0D0D18)
private val GHDrawerItemActive = Color(0xFF1A1F3A)
private val GHCardBg = Color(0xFF14141F)
private val GHCardBgElevated = Color(0xFF1C1C2E)
private val GHCardBorder = Color(0xFF2A2A3E)
private val GHCardBorderActive = Color(0xFF3D3D5C)
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextSoft = Color(0xFFB0B0C8)
private val GHTextDim = Color(0xFF6B6B85)
private val GHAccent = Color(0xFF6B8AFF)
private val GHAccentDim = Color(0xFF4A6BCC)
private val GHAccentBright = Color(0xFF8BA3FF)
private val GHAccentBg = Color(0xFF1A1F3A)
private val GHGreen = Color(0xFF4ADE80)
private val GHAmber = Color(0xFFFBBF24)
private val GHRed = Color(0xFFEF4444)

// ── Self-contained data types (no external deps) ──
private enum class GHServerStatus { ONLINE, MAINTENANCE, OFFLINE, BUSY }

private data class GHGameItem(
    val title: String,
    val subtitle: String,
    val packageName: String,
    val coverGradient: List<Color>,
    val coverText: String,
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
private const val GH_FIFA15_MAIN_ACTIVITY = "com.ea.game.fifa15_row.FIFA15Activity"
private const val GH_PREFS_USER_GAMES = "gh_user_games"

// ── Helper functions ──
private fun ghIsPackageInstalled(context: Context, pkg: String): Boolean = try {
    context.packageManager.getPackageInfo(pkg, 0); true
} catch (_: Throwable) { false }

private fun ghLaunchGame(context: Context, pkg: String): Boolean {
    return try {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else false
    } catch (_: Throwable) { false }
}

private fun ghGetBatteryLevel(context: Context): Int = try {
    (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
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
// TRANSITION
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
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(GHBgGradientStart, GHBgGradientEnd))
            ),
            contentAlignment = Alignment.Center
        ) {
            val logoAlpha by animateFloatAsState(
                when (phase) { 0 -> 0f; 1 -> 1f; 2 -> 1f; 3 -> 1f; 4 -> 0f; else -> 0f },
                tween(800, easing = FastOutSlowInEasing), label = "la"
            )
            val logoScale by animateFloatAsState(
                when (phase) { 0 -> 0.7f; 1 -> 1f; 2 -> 1f; 3 -> 1f; 4 -> 1.15f; else -> 1f },
                tween(800, easing = FastOutSlowInEasing), label = "ls"
            )
            val textAlpha by animateFloatAsState(
                when (phase) { 0 -> 0f; 1 -> 0f; 2 -> 1f; 3 -> 1f; 4 -> 0f; else -> 0f },
                tween(400, easing = FastOutSlowInEasing), label = "ta"
            )
            val fadeOut by animateFloatAsState(
                if (phase == 4) 0f else 1f, tween(600, easing = FastOutSlowInEasing), label = "fo"
            )
            val msgAlpha by animateFloatAsState(
                when (phase) { 3 -> 1f; 4 -> 0f; else -> 0f },
                tween(300, easing = FastOutSlowInEasing), label = "ma"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { this.alpha = fadeOut }
            ) {
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

                Text(
                    typedText,
                    color = GHTextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    modifier = Modifier.graphicsLayer { this.alpha = textAlpha }
                )

                Spacer(Modifier.height(16.dp))

                if (loadingMsg.isNotEmpty()) {
                    Text(
                        loadingMsg,
                        color = GHTextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.graphicsLayer { this.alpha = msgAlpha }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(onExit: () -> Unit) {
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
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = ghGetCurrentTime(context)
            batteryLevel = ghGetBatteryLevel(context)
            delay(30_000)
        }
    }

    var currentScreen by remember { mutableStateOf(0) } // 0=Home, 1=Download, 2=Settings
    var showDrawer by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0=DLavie, 1=My Library

    val dlavieGames = remember {
        listOf(
            GHGameItem(
                title = "FIFA 16 Mobile",
                subtitle = "DLavie 26 Mod",
                packageName = GH_GAME_PKG_16,
                coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                coverText = "DL",
                serverStatus = GHServerStatus.ONLINE,
                sizeMb = "34 MB",
                apkUrl = GH_FIFA16_APK_URL
            ),
            GHGameItem(
                title = "FIFA 15 Mobile",
                subtitle = "DLavie 15 Mod",
                packageName = GH_GAME_PKG_15,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15",
                serverStatus = GHServerStatus.MAINTENANCE,
                sizeMb = "22 MB",
                apkUrl = GH_FIFA15_APK_URL
            )
        )
    }

    var userGames by remember { mutableStateOf<List<GHUserGame>>(emptyList()) }
    LaunchedEffect(selectedTab, currentScreen) {
        if (selectedTab == 1) {
            userGames = withContext(kotlinx.coroutines.Dispatchers.IO) { ghLoadUserGames(context) }
        }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            } catch (_: Exception) {}
        }
    }

    var contextMenuGame by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(GHBgGradientStart, GHBgGradientEnd))
        )
    ) {
        if (!showTransition) {
            when (currentScreen) {
                0 -> GHHomeScreen(
                    context, scope, dlavieGames, userGames, selectedTab,
                    { selectedTab = it },
                    { pkg -> contextMenuGame = Pair(pkg, false) },
                    apkPickerLauncher, currentTime, batteryLevel,
                    { showDrawer = true }, { onExit() }
                )
                1 -> GHDownloadScreen(context) { currentScreen = 0 }
                2 -> GHSettingsScreen(context) { currentScreen = 0 }
            }

            if (showDrawer) {
                GHDrawer(
                    currentScreen = currentScreen,
                    onSelect = { screen -> currentScreen = screen; showDrawer = false },
                    onDismiss = { showDrawer = false },
                    onExit = { showDrawer = false; onExit() }
                )
            }

            contextMenuGame?.let { (pkg, isUser) ->
                GHContextMenu(
                    pkg, isUser, ghIsPackageInstalled(context, pkg),
                    { contextMenuGame = null },
                    { contextMenuGame = null; ghLaunchGame(context, pkg) },
                    { contextMenuGame = null },
                    {
                        contextMenuGame = null
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    },
                    {
                        contextMenuGame = null
                        val u = userGames.filter { it.packageName != pkg }
                        ghSaveUserGames(context, u); userGames = u
                    },
                    {
                        contextMenuGame = null
                        try { File("/sdcard/Android/data/$pkg").deleteRecursively() } catch (_: Exception) {}
                    }
                )
            }
        }

        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HOME SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHHomeScreen(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    dlavieGames: List<GHGameItem>,
    userGames: List<GHUserGame>,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onContextMenu: (Pair<String, Boolean>) -> Unit,
    apkPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    currentTime: String,
    batteryLevel: Int,
    onMenu: () -> Unit,
    onExit: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // ── TOP BAR ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GHTopIconButton(Icons.Rounded.Menu, "Menu", GHTextWhite) { onMenu() }
            Spacer(Modifier.width(16.dp))
            GHCtrlBtn("LB")
            Spacer(Modifier.width(12.dp))
            Text(
                "Dashboard",
                color = GHTextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(12.dp))
            GHCtrlBtn("RB")
            Spacer(Modifier.width(12.dp))
            GHTopIconButton(Icons.Rounded.Search, "Search", GHTextSoft) {}
            Spacer(Modifier.width(8.dp))
            val batteryTint = when {
                batteryLevel <= 15 -> GHRed
                batteryLevel <= 30 -> GHAmber
                else -> GHAccent
            }
            Icon(
                Icons.Rounded.BatteryFull, "Battery",
                tint = batteryTint, modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "$batteryLevel%",
                color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Rounded.Schedule, "Time",
                tint = GHTextSoft, modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                currentTime,
                color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(10.dp))
            GHTopIconButton(Icons.Rounded.ArrowBack, "Exit", GHTextSoft) { onExit() }
        }

        // ── Category tabs ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GHPillTab("DLavie", dlavieGames.size, selectedTab == 0) { onTabSelect(0) }
            GHPillTab("My Library", userGames.size, selectedTab == 1) { onTabSelect(1) }
            if (selectedTab == 1) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GHAccentBg)
                        .border(1.dp, GHAccent.copy(0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            apkPickerLauncher.launch("application/vnd.android.package-archive")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add, "Add",
                        tint = GHAccentBright, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── Game cards ──
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedTab == 0) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(dlavieGames) { game ->
                        GHGlassCard(
                            game,
                            ghIsPackageInstalled(context, game.packageName),
                            { onContextMenu(Pair(game.packageName, false)) }
                        )
                    }
                }
            } else {
                if (userGames.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.SportsEsports, null,
                            tint = GHTextDim, modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No games yet",
                            color = GHTextWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap + to import an APK",
                            color = GHTextSoft, fontSize = 12.sp
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(userGames) { ug ->
                            GHGlassUserCard(
                                ug,
                                ghIsPackageInstalled(context, ug.packageName),
                                {
                                    if (ghIsPackageInstalled(context, ug.packageName)) {
                                        ghLaunchGame(context, ug.packageName)
                                    } else {
                                        val f = File(ug.sourcePath)
                                        if (f.exists()) {
                                            try {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context, "${context.packageName}.files", f
                                                )
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "application/vnd.android.package-archive")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                )
                                            } catch (_: Exception) {}
                                        }
                                    }
                                },
                                { onContextMenu(Pair(ug.packageName, true)) }
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom bar ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GHBottomPill("Y", "Search")
            GHBottomPill("B", "Menu") { onMenu() }
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
        try {
            val dir = File("/sdcard/Android/data/$GH_GAME_PKG_16")
            if (dir.exists()) dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() else 0L
        } catch (_: Exception) { 0L }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GHTopIconButton(Icons.Rounded.ArrowBack, "Back", GHTextWhite) { onBack() }
            Spacer(Modifier.width(16.dp))
            Text(
                "Download",
                color = GHTextWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GHPillTab("Download Task", 0, selectedTab == 0) { selectedTab = 0 }
            GHPillTab("Game Management", 0, selectedTab == 1) { selectedTab = 1 }
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage card — SOLID with firm border
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GHCardBg,
                border = BorderStroke(1.dp, GHCardBorder)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("STORAGE", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Game", color = GHTextSoft, fontSize = 13.sp)
                        Text(ghFormatBytes(gameStorage), color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Other", color = GHTextSoft, fontSize = 13.sp)
                        Text(ghFormatBytes(totalStorage - availStorage - gameStorage), color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Available", color = GHTextSoft, fontSize = 13.sp)
                        Text(ghFormatBytes(availStorage), color = GHGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    val usedPct = if (totalStorage > 0) ((totalStorage - availStorage).toFloat() / totalStorage.toFloat()) else 0f
                    Box(
                        Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(3.dp)).background(GHCardBorder)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(usedPct).fillMaxHeight()
                                .background(Brush.linearGradient(listOf(GHAccentDim, GHAccent)))
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                Text("Download Soon (0)", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GHCardBg,
                    border = BorderStroke(1.dp, GHCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Text("No Queued Tasks", color = GHTextDim, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Completed", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Clear All", color = GHAccentBright, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GHCardBg,
                    border = BorderStroke(1.dp, GHCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Text("No completed tasks", color = GHTextDim, fontSize = 13.sp)
                    }
                }
            } else {
                val games = listOf(
                    Triple("FIFA 16 Mobile", GH_GAME_PKG_16, "DLavie 26 Mod"),
                    Triple("FIFA 15 Mobile", GH_GAME_PKG_15, "DLavie 15 Mod")
                )
                games.forEach { (title, pkg, subtitle) ->
                    val installed = ghIsPackageInstalled(context, pkg)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GHCardBg,
                        border = BorderStroke(1.dp, GHCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GHAccentBg)
                                    .border(1.dp, GHCardBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(title.take(2), color = GHAccentBright, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(subtitle, color = GHTextSoft, fontSize = 12.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (installed) "Installed" else "Not installed",
                                    color = if (installed) GHGreen else GHTextDim,
                                    fontSize = 11.sp, fontWeight = FontWeight.Medium
                                )
                            }
                            if (installed) {
                                IconButton(onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } catch (_: Exception) {}
                                }) {
                                    Icon(Icons.Rounded.Delete, "Uninstall", tint = GHRed.copy(0.7f), modifier = Modifier.size(22.dp))
                                }
                            } else {
                                IconButton(onClick = {}) {
                                    Icon(Icons.Rounded.Download, "Install", tint = GHAccentBright, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
            GHBottomPill("B", "Back") { onBack() }
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
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val ds = InputDevice.getDeviceIds()
            val gps = mutableListOf<String>()
            for (id in ds) {
                val d = InputDevice.getDevice(id)
                if (d != null) {
                    val s = d.sources
                    if (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                        s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                        gps.add(d.name)
                    }
                }
            }
            gamepadCount = gps.size
            gamepadNames = gps
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GHTopIconButton(Icons.Rounded.ArrowBack, "Back", GHTextWhite) { onBack() }
            Spacer(Modifier.width(16.dp))
            Text(
                "Settings",
                color = GHTextWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("GAMEPAD", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GHCardBg,
                border = BorderStroke(1.dp, GHCardBorder)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (gamepadCount > 0) GHAccentBg else GHCardBgElevated)
                                .border(1.dp, if (gamepadCount > 0) GHAccent else GHCardBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.SportsEsports, null,
                                tint = if (gamepadCount > 0) GHAccentBright else GHTextDim,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Gamepad Connection", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (gamepadCount > 0) "$gamepadCount gamepad(s) connected" else "No gamepad detected",
                                color = if (gamepadCount > 0) GHGreen else GHTextSoft,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            Modifier.size(12.dp)
                                .clip(CircleShape)
                                .background(if (gamepadCount > 0) GHGreen else GHTextDim)
                        )
                    }
                    if (gamepadNames.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        gamepadNames.forEach { n ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(GHGreen))
                                Spacer(Modifier.width(10.dp))
                                Text(n, color = GHTextSoft, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Text("DISPLAY", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GHCardBg,
                border = BorderStroke(1.dp, GHCardBorder)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GHAccentBg)
                            .border(1.dp, GHAccent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ScreenRotation, null, tint = GHAccentBright, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Auto Rotate", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Landscape mode in GameHub", color = GHTextSoft, fontSize = 12.sp)
                    }
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(GHGreen)
                            .border(1.dp, GHGreen.copy(0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text("ABOUT", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GHCardBg,
                border = BorderStroke(1.dp, GHCardBorder)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("DLavie GameHub", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Cloud Gaming Platform v7.9.81", color = GHTextSoft, fontSize = 12.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
            GHBottomPill("B", "Back") { onBack() }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SIDE MENU DRAWER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDrawer(
    currentScreen: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(0.55f))
            .clickable { onDismiss() }
    ) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.45f)
                .background(GHDrawerBg)
                .clickable {}
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                // Profile
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape)
                            .background(GHAccentBg)
                            .border(2.dp, GHAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("D", color = GHAccentBright, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("DLavie Player", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text("@user", color = GHTextSoft, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(GHGreen))
                            Spacer(Modifier.width(6.dp))
                            Text("Online", color = GHGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GHCardBorder))
                Spacer(Modifier.height(16.dp))
                GHDrawerItem(Icons.Rounded.Home, "Home", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.SportsEsports, "Game", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.Download, "Download", currentScreen == 1) { onSelect(1) }
                GHDrawerItem(Icons.Rounded.Settings, "Settings", currentScreen == 2) { onSelect(2) }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GHCardBorder))
                Spacer(Modifier.height(12.dp))
                GHDrawerItem(Icons.Rounded.Close, "Exit GameHub", false) { onExit() }
                Spacer(Modifier.height(8.dp))
                Text("DLavie GameHub v279", color = GHTextDim, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun GHDrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GHDrawerItemActive else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = if (selected) GHAccentBright else GHTextSoft,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            color = if (selected) GHTextWhite else GHTextSoft,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHCtrlBtn(label: String) {
    Box(
        Modifier.size(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(GHCardBg)
            .border(1.5.dp, GHCardBorder, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = GHTextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GHTopIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = GHTextWhite,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(GHCardBg)
            .border(1.dp, GHCardBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun GHBottomPill(ctrl: String, label: String, onClick: () -> Unit = {}) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(GHCardBg)
            .border(1.dp, GHCardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GHCtrlBtn(ctrl)
        Spacer(Modifier.width(10.dp))
        Text(label, color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GHPillTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) GHCardBgElevated else Color.Transparent
    val border = if (selected) GHCardBorderActive else GHCardBorder.copy(0f)
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = if (selected) GHTextWhite else GHTextSoft,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    count.toString(),
                    color = if (selected) GHAccentBright else GHTextDim,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GHGlassCard(
    game: GHGameItem,
    isInstalled: Boolean,
    onLongClick: () -> Unit
) {
    Card(
        Modifier.width(210.dp).height(290.dp)
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GHCardBg),
        border = BorderStroke(1.dp, GHCardBorder)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.linearGradient(game.coverGradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(game.coverText, color = GHTextWhite, fontSize = 38.sp, fontWeight = FontWeight.Black)
            }
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(
                        listOf(Color.Black.copy(0.15f), Color.Transparent, Color.Black.copy(0.85f))
                    ))
            )
            val (sc, st) = when (game.serverStatus) {
                GHServerStatus.ONLINE -> Pair(GHGreen, "ONLINE")
                GHServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT")
                GHServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE")
                GHServerStatus.BUSY -> Pair(GHAmber, "BUSY")
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(sc)
                    .border(1.dp, sc.copy(0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(st, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    game.title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    game.subtitle, color = GHTextSoft, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHCtrlBtn("A")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isInstalled) "Launch" else "Install",
                        color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GHGlassUserCard(
    userGame: GHUserGame,
    isInstalled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        Modifier.width(210.dp).height(290.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GHCardBg),
        border = BorderStroke(1.dp, GHCardBorder)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
                contentAlignment = Alignment.Center
            ) {
                val icon = remember(userGame.packageName) {
                    try { context.packageManager.getApplicationIcon(userGame.packageName) }
                    catch (_: Exception) { null }
                }
                if (icon != null) {
                    AsyncImage(
                        model = icon,
                        contentDescription = userGame.title,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Rounded.SportsEsports, null, tint = GHTextDim, modifier = Modifier.size(52.dp))
                }
            }
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(
                        listOf(Color.Black.copy(0.15f), Color.Transparent, Color.Black.copy(0.85f))
                    ))
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    userGame.title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    userGame.packageName.take(20), color = GHTextSoft, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHCtrlBtn("A")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isInstalled) "Launch" else "Install",
                        color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GHContextMenu(
    pkg: String,
    isUser: Boolean,
    installed: Boolean,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onView: () -> Unit,
    onUninstall: () -> Unit,
    onRemove: () -> Unit,
    onClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Options", color = GHTextWhite, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (installed) {
                    GHMenuItem(Icons.Rounded.PlayArrow, "Launch Game", onLaunch)
                }
                GHMenuItem(Icons.Rounded.Info, "View Details", onView)
                if (!isUser && installed) {
                    GHMenuItem(Icons.Rounded.CleaningServices, "Clear Data Only", onClear)
                }
                if (isUser) {
                    if (installed) {
                        GHMenuItem(Icons.Rounded.Delete, "Uninstall Game", onUninstall)
                    }
                    GHMenuItem(Icons.Rounded.RemoveCircle, "Remove from Library", onRemove)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GHAccentBright) }
        },
        containerColor = GHCardBg
    )
}

@Composable
private fun GHMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = GHTextSoft, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = GHTextWhite, fontSize = 15.sp)
    }
}
