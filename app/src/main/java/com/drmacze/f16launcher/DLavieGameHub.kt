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
import android.view.WindowManager
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB v278 — Design System Update (match GameHub PlayStore ref)
// Changes:
//   - Background: dark blue gradient (#0A0A1A → #050508), NOT pure black
//   - Cards     : SOLID with firm border, NOT glass effect
//   - Accent    : soft blue (#6B8AFF), NOT cyan
//   - Shapes    : consistent 8–12dp radius
//   - LB/RB     : square-ish (4dp), firm border
//   - Drawer    : wider (320dp / 0.45f), bigger icons (28dp)
//   - Spacing   : spacious, not cramped
//   - Font      : rounded medium-weight feel
// Preserved: all v277 logic (CommunityApi, UserGame, immersive, transition, etc.)
// ═══════════════════════════════════════════════════════════════════════════

// ── Background gradient (dark blue → near-black) ──
private val GHBgGradientStart = Color(0xFF0A0A1A)
private val GHBgGradientEnd = Color(0xFF050508)
private val GHBg = GHBgGradientEnd // fallback solid

// ── Drawer ──
private val GHDrawerBg = Color(0xFF0D0D18)
private val GHDrawerItemActive = Color(0xFF1A1F3A)

// ── Cards — SOLID with firm border (no glass) ──
private val GHCardBg = Color(0xFF14141F)
private val GHCardBgElevated = Color(0xFF1C1C2E)
private val GHCardBorder = Color(0xFF2A2A3E)
private val GHCardBorderActive = Color(0xFF3D3D5C)

// Legacy aliases (kept for back-compat with v277 component references)
private val GHGlass = GHCardBg
private val GHGlassHi = GHCardBgElevated

// ── Text ──
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextSoft = Color(0xFFB0B0C8)
private val GHTextDim = Color(0xFF6B6B85)

// ── Accent — SOFT BLUE (not cyan) ──
private val GHAccent = Color(0xFF6B8AFF)
private val GHAccentDim = Color(0xFF4A6BCC)
private val GHAccentBright = Color(0xFF8BA3FF)
private val GHAccentBg = Color(0xFF1A1F3A)

// ── Status ──
private val GHGreen = Color(0xFF4ADE80)
private val GHAmber = Color(0xFFFBBF24)
private val GHRed = Color(0xFFEF4444)

// ─── Transition — Match GameHub PlayStore loading + loading messages ─────────
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
            phase = 0
            delay(800)
            phase = 1
            delay(800)
            phase = 2
            for (i in fullText.indices) { typedText = fullText.substring(0, i + 1); delay(120) }
            delay(400)
            phase = 3
            for (msg in loadingMessages) { loadingMsg = msg; delay(700) }
            loadingMsg = ""
            delay(300)
            phase = 4
            delay(600)
            onComplete()
            phase = 5
        }
    }

    if (visible && phase < 5) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(GHBgGradientStart, GHBgGradientEnd))), contentAlignment = Alignment.Center) {
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
                    Text(loadingMsg, color = GHTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, modifier = Modifier.graphicsLayer { this.alpha = msgAlpha })
                }
            }
        }
    }
}

private fun getBatteryLevel(context: Context): Int = try { (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } catch (_: Exception) { 100 }
private fun getCurrentTime(context: Context): String = try { val is24 = DateFormat.is24HourFormat(context); SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date()) } catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
private fun getStorageInfo(): Pair<Long, Long> { return try { val stat = StatFs("/sdcard"); val total = stat.totalBytes; val avail = stat.availableBytes; Pair(avail, total) } catch (_: Exception) { Pair(0L, 0L) } }
private fun formatBytes(bytes: Long): String = when { bytes >= 1_000_000_000 -> "%.1fGB".format (bytes / 1_000_000_000.0); bytes >= 1_000_000 -> "%.1fMB".format (bytes / 1_000_000.0); bytes >= 1_000 -> "%.1fKB".format (bytes / 1_000.0); else -> "${bytes}B" }

// ═══════════════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(onNav: (Page) -> Unit, onGameClick: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // v7.9.79: showTransition SELALU true saat composable di-create.
    // Karena GameHub di-render inside `if (showGameHub) { ... }`, setiap kali
    // showGameHub berubah true → composable di-create fresh → showTransition = true.
    var showTransition by remember { mutableStateOf(true) }

    // ── Immersive mode (hide Android system status bar + nav bar, keep GameHub bar) ──
    // v7.9.79: Orientation control DIPINDAH ke MainShell (LaunchedEffect(showGameHub))
    // Disini hanya handle immersive mode (hide system bars)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        // Hide Android system bars (clock, battery, wifi, notifications, nav buttons)
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                val controller = window.insetsController
                controller?.hide(WindowInsets.Type.systemBars())
                controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }
        onDispose {
            // Restore system UI
            activity?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(true)
                    val controller = window.insetsController
                    controller?.show(WindowInsets.Type.systemBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    var currentTime by remember { mutableStateOf(getCurrentTime(context)) }
    var batteryLevel by remember { mutableStateOf(getBatteryLevel(context)) }
    LaunchedEffect(Unit) { while (true) { currentTime = getCurrentTime(context); batteryLevel = getBatteryLevel(context); delay(30_000) } }

    // ── Navigation: 0=Home, 1=Download, 2=Settings ──
    var currentScreen by remember { mutableStateOf(0) }
    var showDrawer by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0=DLavie, 1=My Library

    // ── Server status ──
    var fifa16Status by remember { mutableStateOf(ServerStatus.ONLINE) }
    var fifa15Status by remember { mutableStateOf(ServerStatus.MAINTENANCE) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val api = CommunityApi(context)
                val config = api.getAppConfig("game_server_status")
                val f16 = config.optString("fifa16", "online").lowercase()
                val f15 = config.optString("fifa15", "maintenance").lowercase()
                fifa16Status = when (f16) { "online" -> ServerStatus.ONLINE; "maintenance" -> ServerStatus.MAINTENANCE; "offline" -> ServerStatus.OFFLINE; else -> ServerStatus.ONLINE }
                fifa15Status = when (f15) { "online" -> ServerStatus.ONLINE; "maintenance" -> ServerStatus.MAINTENANCE; "offline" -> ServerStatus.OFFLINE; else -> ServerStatus.MAINTENANCE }
            }
        }
    }

    val dlavieGames = remember(fifa16Status, fifa15Status) {
        listOf(
            GameItem(title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod", packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity", coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)), coverText = "DL", coverImageRes = R.drawable.fifa16_cover, serverStatus = fifa16Status, description = "FIFA 16 Mobile dengan mod DLavie 26", version = "v26.0", sizeMb = "34 MB", apkUrl = FIFA16_APK_URL),
            GameItem(title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod", packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY, coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)), coverText = "D15", coverImageRes = R.drawable.fifa15_cover, serverStatus = fifa15Status, description = "FIFA 15 Mobile dengan mod DLavie 15", version = "v15.0", sizeMb = "22 MB", apkUrl = FIFA15_APK_URL)
        )
    }

    var userGames by remember { mutableStateOf<List<UserGame>>(emptyList()) }
    LaunchedEffect(selectedTab, currentScreen) { if (selectedTab == 1) userGames = withContext(Dispatchers.IO) { loadUserGames(context) } }

    val apkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { try { context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) { } }
    }

    var contextMenuGame by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // ── User profile info ──
    val api = remember { CommunityApi(context) }
    val displayName = remember { api.displayName() }
    val username = remember { api.username() }

    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(GHBgGradientStart, GHBgGradientEnd)))) {
        // Content ONLY renders after transition completes
        if (!showTransition) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(GHBgGradientStart, GHBgGradientEnd))))

            when (currentScreen) {
                0 -> GHHomeScreen(context, scope, dlavieGames, userGames, selectedTab, { selectedTab = it }, { onGameClick(it) }, { contextMenuGame = it }, apkPickerLauncher, currentTime, batteryLevel, { showDrawer = true }, { onNav(Page.Home) })
                1 -> GHDownloadScreen(context, { currentScreen = 0 })
                2 -> GHSettingsScreen(context, { currentScreen = 0 })
            }

            // ── Side Menu Drawer ──
            if (showDrawer) {
                GHDrawer(
                    displayName = displayName,
                    username = username,
                    currentScreen = currentScreen,
                    onSelect = { screen -> currentScreen = screen; showDrawer = false },
                onDismiss = { showDrawer = false },
                onExit = { showDrawer = false; onNav(Page.Home) }
            )
        }

        // Context menu
        contextMenuGame?.let { (pkg, isUser) ->
            GHContextMenu(pkg, isUser, isPackageInstalled(context, pkg), { contextMenuGame = null }, { contextMenuGame = null; launchGame(context, pkg) }, { contextMenuGame = null; onGameClick(pkg) }, { contextMenuGame = null; try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { } }, { contextMenuGame = null; val u = userGames.filter { it.packageName != pkg }; saveUserGames(context, u); userGames = u }, { contextMenuGame = null; try { File("/sdcard/Android/data/$pkg").deleteRecursively() } catch (_: Exception) { } })
        }
        } // end if (!showTransition)

        // Transition overlay — rendered ON TOP of content (last = top z-index in Box)
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
    dlavieGames: List<GameItem>,
    userGames: List<UserGame>,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onGameClick: (String) -> Unit,
    onContextMenu: (Pair<String, Boolean>) -> Unit,
    apkPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    currentTime: String,
    batteryLevel: Int,
    onMenu: () -> Unit,
    onExit: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // ── TOP BAR (spacious, solid icon buttons) ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            GHTopIconButton(Icons.Rounded.Menu, "Menu", GHTextWhite) { onMenu() }
            Spacer(Modifier.width(16.dp))
            GHCtrlBtn("LB")
            Spacer(Modifier.width(12.dp))
            Text("Dashboard", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(12.dp))
            GHCtrlBtn("RB")
            Spacer(Modifier.width(12.dp))
            GHTopIconButton(Icons.Rounded.Search, "Search", GHTextSoft) { }
            Spacer(Modifier.width(8.dp))
            // Battery icon with percentage (soft blue when full, amber/red when low)
            val batteryTint = when { batteryLevel <= 15 -> GHRed; batteryLevel <= 30 -> GHAmber; else -> GHAccent }
            Icon(Icons.Rounded.BatteryFull, contentDescription = "Battery", tint = batteryTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("$batteryLevel%", color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            Spacer(Modifier.width(10.dp))
            // Clock icon + time
            Icon(Icons.Rounded.Schedule, contentDescription = "Time", tint = GHTextSoft, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(currentTime, color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            Spacer(Modifier.width(10.dp))
            GHTopIconButton(Icons.Rounded.ArrowBack, "Exit", GHTextSoft) { onExit() }
        }

        // ── Category tabs (pill style, spacious, solid when selected) ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GHPillTab("DLavie", dlavieGames.size, selectedTab == 0, { onTabSelect(0) })
            GHPillTab("My Library", userGames.size, selectedTab == 1, { onTabSelect(1) })
            if (selectedTab == 1) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(GHAccentBg).border(1.dp, GHAccent.copy(0.5f), RoundedCornerShape(10.dp)).clickable { apkPickerLauncher.launch("application/vnd.android.package-archive") }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, contentDescription = "Add", tint = GHAccentBright, modifier = Modifier.size(20.dp)) }
            }
        }

        // ── Game cards (spacious padding) ──
        Box(Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            if (selectedTab == 0) {
                LazyRow(contentPadding = PaddingValues(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(dlavieGames) { game -> GHGlassCard(game, isPackageInstalled(context, game.packageName), { onGameClick(game.packageName) }, { onContextMenu(Pair(game.packageName, false)) }) }
                }
            } else {
                if (userGames.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No games yet", color = GHTextWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap + to import an APK", color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily)
                    }
                } else {
                    LazyRow(contentPadding = PaddingValues(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(userGames) { ug -> GHGlassUserCard(ug, isPackageInstalled(context, ug.packageName), { if (isPackageInstalled(context, ug.packageName)) launchGame(context, ug.packageName) else { val f = File(ug.sourcePath); if (f.exists()) { try { val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", f); context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) { } } } }, { onContextMenu(Pair(ug.packageName, true)) }) }
                    }
                }
            }
        }

        // ── Bottom bar (spacious, solid pill buttons) ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            GHBottomPill("Y", "Search")
            GHBottomPill("B", "Menu") { onMenu() }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DOWNLOAD SCREEN — Download Task + Game Management
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDownloadScreen(context: Context, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Download Task, 1=Game Management
    val (availStorage, totalStorage) = remember { getStorageInfo() }
    val gameStorage = remember { try { val dir = File("/sdcard/Android/data/$GAME_PKG_16"); if (dir.exists()) dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() else 0L } catch (_: Exception) { 0L } }

    Column(Modifier.fillMaxSize()) {
        // ── Top bar (spacious, solid back button) ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            GHTopIconButton(Icons.Rounded.ArrowBack, "Back", GHTextWhite) { onBack() }
            Spacer(Modifier.width(16.dp))
            Text("Download", color = GHTextWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, modifier = Modifier.weight(1f))
        }

        // ── Tabs (spacious) ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GHPillTab("Download Task", 0, selectedTab == 0, { selectedTab = 0 })
            GHPillTab("Game Management", 0, selectedTab == 1, { selectedTab = 1 })
        }

        // ── Content (spacious, solid cards) ──
        Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Storage info — solid card with firm border
            Surface(shape = RoundedCornerShape(12.dp), color = GHCardBg, border = BorderStroke(1.dp, GHCardBorder)) {
                Column(Modifier.padding(20.dp)) {
                    Text("STORAGE", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Game", color = GHTextSoft, fontSize = 13.sp); Text(formatBytes(gameStorage), color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Other", color = GHTextSoft, fontSize = 13.sp); Text(formatBytes(totalStorage - availStorage - gameStorage), color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Available", color = GHTextSoft, fontSize = 13.sp); Text(formatBytes(availStorage), color = GHGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    // Storage bar — gradient fill, rounded ends
                    val usedPct = if (totalStorage > 0) ((totalStorage - availStorage).toFloat() / totalStorage.toFloat()) else 0f
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(GHCardBorder)) {
                        Box(Modifier.fillMaxWidth(usedPct).fillMaxHeight().background(Brush.linearGradient(listOf(GHAccentDim, GHAccent))))
                    }
                }
            }

            if (selectedTab == 0) {
                // Download Task — solid cards
                Text("Download Soon (0)", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                Surface(shape = RoundedCornerShape(12.dp), color = GHCardBg, border = BorderStroke(1.dp, GHCardBorder), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { Text("No Queued Tasks", color = GHTextDim, fontSize = 13.sp, fontFamily = InterFontFamily) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Completed", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                    Text("Clear All", color = GHAccentBright, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, modifier = Modifier.clickable { })
                }
                Surface(shape = RoundedCornerShape(12.dp), color = GHCardBg, border = BorderStroke(1.dp, GHCardBorder), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { Text("No completed tasks", color = GHTextDim, fontSize = 13.sp, fontFamily = InterFontFamily) }
                }
            } else {
                // Game Management — solid cards with firm border
                val games = listOf(
                    Triple("FIFA 16 Mobile", GAME_PKG_16, "DLavie 26 Mod"),
                    Triple("FIFA 15 Mobile", GAME_PKG_15, "DLavie 15 Mod")
                )
                games.forEach { (title, pkg, subtitle) ->
                    val installed = isPackageInstalled(context, pkg)
                    Surface(shape = RoundedCornerShape(12.dp), color = GHCardBg, border = BorderStroke(1.dp, GHCardBorder), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(GHAccentBg).border(1.dp, GHCardBorder, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(title.take(2), color = GHAccentBright, fontSize = 15.sp, fontWeight = FontWeight.Black) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)
                                Spacer(Modifier.height(2.dp))
                                Text(subtitle, color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily)
                                Spacer(Modifier.height(2.dp))
                                Text(if (installed) "Installed" else "Not installed", color = if (installed) GHGreen else GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                            }
                            if (installed) {
                                IconButton(onClick = { try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { } }) { Icon(Icons.Rounded.Delete, contentDescription = "Uninstall", tint = GHRed.copy(0.7f), modifier = Modifier.size(22.dp)) }
                            } else {
                                IconButton(onClick = { /* reinstall */ }) { Icon(Icons.Rounded.Download, contentDescription = "Install", tint = GHAccentBright, modifier = Modifier.size(22.dp)) }
                            }
                        }
                    }
                }
            }
        }

        // Bottom (spacious)
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
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { val ds = InputDevice.getDeviceIds(); val gps = mutableListOf<String>(); for (id in ds) { val d = InputDevice.getDevice(id); if (d != null) { val s = d.sources; if (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) gps.add(d.name) } }; gamepadCount = gps.size; gamepadNames = gps } }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            GHTopIconButton(Icons.Rounded.ArrowBack, "Back", GHTextWhite) { onBack() }
            Spacer(Modifier.width(16.dp))
            Text("Settings", color = GHTextWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, modifier = Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Gamepad — solid card
            Text("GAMEPAD", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHCardBg, border = BorderStroke(1.dp, GHCardBorder)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(if (gamepadCount > 0) GHAccentBg else GHCardBgElevated).border(1.dp, if (gamepadCount > 0) GHAccent else GHCardBorder, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = if (gamepadCount > 0) GHAccentBright else GHTextDim, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) { Text("Gamepad Connection", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(if (gamepadCount > 0) "$gamepadCount gamepad(s) connected" else "No gamepad detected", color = if (gamepadCount > 0) GHGreen else GHTextSoft, fontSize = 12.sp) }
                        Box(Modifier.size(12.dp).clip(CircleShape).background(if (gamepadCount > 0) GHGreen else GHTextDim))
                    }
                    if (gamepadNames.isNotEmpty()) { Spacer(Modifier.height(12.dp)); gamepadNames.forEach { n -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) { Box(Modifier.size(8.dp).clip(CircleShape).background(GHGreen)); Spacer(Modifier.width(10.dp)); Text(n, color = GHTextSoft, fontSize = 12.sp) } } }
                }
            }
            // Display — solid card
            Text("DISPLAY", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHCardBg, border = BorderStroke(1.dp, GHCardBorder)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(GHAccentBg).border(1.dp, GHAccent, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = GHAccentBright, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text("Auto Rotate", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text("Landscape mode in GameHub", color = GHTextSoft, fontSize = 12.sp) }
                    Box(Modifier.size(28.dp).clip(CircleShape).background(GHGreen).border(1.dp, GHGreen.copy(0.5f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
                }
            }
            // About — solid card
            Text("ABOUT", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHCardBg, border = BorderStroke(1.dp, GHCardBorder)) {
                Column(Modifier.padding(20.dp)) { Text("DLavie GameHub", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text("Cloud Gaming Platform v7.9.79", color = GHTextSoft, fontSize = 12.sp) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) { GHBottomPill("B", "Back") { onBack() } }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SIDE MENU DRAWER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDrawer(displayName: String, username: String, currentScreen: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.55f)).clickable { onDismiss() }) {
        // Wider drawer: 0.45f (was 0.35f) ≈ 320dp on most landscape phones
        Box(Modifier.fillMaxHeight().fillMaxWidth(0.45f).background(GHDrawerBg).clickable { }) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                // Profile (bigger avatar, accent ring)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(GHAccentBg).border(2.dp, GHAccent, CircleShape), contentAlignment = Alignment.Center) { Text((displayName.ifEmpty { "D" }).take(1), color = GHAccentBright, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(displayName.ifEmpty { "DLavie Player" }, color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text("@${username.ifEmpty { "user" }}", color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(6.dp).clip(CircleShape).background(GHGreen)); Spacer(Modifier.width(6.dp)); Text("Online", color = GHGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                    }
                }
                Spacer(Modifier.height(28.dp))
                // Divider
                Box(Modifier.fillMaxWidth().height(1.dp).background(GHCardBorder))
                Spacer(Modifier.height(16.dp))
                // Menu items (bigger icons, spacious)
                GHDrawerItem(Icons.Rounded.Home, "Home", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.SportsEsports, "Game", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.Download, "Download", currentScreen == 1) { onSelect(1) }
                GHDrawerItem(Icons.Rounded.Settings, "Settings", currentScreen == 2) { onSelect(2) }
                Spacer(Modifier.weight(1f))
                // Divider
                Box(Modifier.fillMaxWidth().height(1.dp).background(GHCardBorder))
                Spacer(Modifier.height(12.dp))
                // Exit
                GHDrawerItem(Icons.Rounded.Close, "Exit GameHub", false) { onExit() }
                Spacer(Modifier.height(8.dp))
                Text("DLavie GameHub v278", color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily)
            }
        }
    }
}

@Composable
private fun GHDrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) GHDrawerItemActive else Color.Transparent).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        // BIGGER ICON: 28dp (was 20dp)
        Icon(icon, contentDescription = null, tint = if (selected) GHAccentBright else GHTextSoft, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = if (selected) GHTextWhite else GHTextSoft, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, fontFamily = InterFontFamily)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHCtrlBtn(label: String) {
    // SQUARE-ISH (4dp radius), firm border (was 5dp + dim border)
    Box(Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(GHCardBg).border(1.5.dp, GHCardBorder, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text(label, color = GHTextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) }
}

// ── NEW: top bar icon button (solid circular, firm border) ──
@Composable
private fun GHTopIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, tint: Color = GHTextWhite, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(GHCardBg).border(1.dp, GHCardBorder, CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ── NEW: bottom bar pill button (spacious, solid) ──
@Composable
private fun GHBottomPill(ctrl: String, label: String, onClick: () -> Unit = {}) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(GHCardBg).border(1.dp, GHCardBorder, RoundedCornerShape(8.dp)).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        GHCtrlBtn(ctrl)
        Spacer(Modifier.width(10.dp))
        Text(label, color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
    }
}

@Composable
private fun GHPillTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    // SOLID when selected (was glass), spacious padding, firm border when selected
    val bg = if (selected) GHCardBgElevated else Color.Transparent
    val border = if (selected) GHCardBorderActive else GHCardBorder.copy(0f)
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(bg).border(1.dp, border, RoundedCornerShape(10.dp)).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (selected) GHTextWhite else GHTextSoft, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, fontFamily = InterFontFamily)
            if (count > 0) { Spacer(Modifier.width(6.dp)); Text(count.toString(), color = if (selected) GHAccentBright else GHTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGlassCard(game: GameItem, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    // SOLID card with firm border (was glass 0x08FFFFFF), 12dp radius (was 16dp)
    Card(Modifier.width(210.dp).height(290.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GHCardBg), border = BorderStroke(1.dp, GHCardBorder)) {
        Box(Modifier.fillMaxSize()) {
            if (game.coverImageRes != null) { Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            else { Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) { Text(game.coverText, color = GHTextWhite, fontSize = 38.sp, fontWeight = FontWeight.Black) } }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.15f), Color.Transparent, Color.Black.copy(0.85f)))))
            val (sc, st) = when (game.serverStatus) { ServerStatus.ONLINE -> Pair(GHGreen, "ONLINE"); ServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT"); ServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE"); ServerStatus.BUSY -> Pair(GHAmber, "BUSY") }
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(sc).border(1.dp, sc.copy(0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(st, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp) }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(game.title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp)); Text(game.subtitle, color = GHTextSoft, fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { GHCtrlBtn("A"); Spacer(Modifier.width(8.dp)); Text(if (isInstalled) "Launch" else "Install", color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGlassUserCard(userGame: UserGame, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    // SOLID card with firm border (was glass), 12dp radius (was 16dp)
    Card(Modifier.width(210.dp).height(290.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GHCardBg), border = BorderStroke(1.dp, GHCardBorder)) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))), contentAlignment = Alignment.Center) {
                val icon = remember(userGame.packageName) { try { context.packageManager.getApplicationIcon(userGame.packageName) } catch (_: Exception) { null } }
                if (icon != null) AsyncImage(model = icon, contentDescription = userGame.title, modifier = Modifier.size(72.dp), contentScale = ContentScale.Fit)
                else Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(52.dp))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.15f), Color.Transparent, Color.Black.copy(0.85f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(userGame.title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp)); Text(userGame.packageName.take(20), color = GHTextSoft, fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { GHCtrlBtn("A"); Spacer(Modifier.width(8.dp)); Text(if (isInstalled) "Launch" else "Install", color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily) }
            }
        }
    }
}

@Composable
private fun GHContextMenu(pkg: String, isUser: Boolean, installed: Boolean, onDismiss: () -> Unit, onLaunch: () -> Unit, onView: () -> Unit, onUninstall: () -> Unit, onRemove: () -> Unit, onClear: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Game Options", color = GHTextWhite, fontWeight = FontWeight.SemiBold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (installed) { GHMenuItem(Icons.Rounded.PlayArrow, "Launch Game", onLaunch) }
        GHMenuItem(Icons.Rounded.Info, "View Details", onView)
        if (!isUser && installed) { GHMenuItem(Icons.Rounded.CleaningServices, "Clear Data Only", onClear) }
        if (isUser) { if (installed) { GHMenuItem(Icons.Rounded.Delete, "Uninstall Game", onUninstall) }; GHMenuItem(Icons.Rounded.RemoveCircle, "Remove from Library", onRemove) }
    } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GHAccentBright) } }, containerColor = GHCardBg)
}

@Composable
private fun GHMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(14.dp)); Text(label, color = GHTextWhite, fontSize = 15.sp, fontFamily = InterFontFamily)
    }
}
