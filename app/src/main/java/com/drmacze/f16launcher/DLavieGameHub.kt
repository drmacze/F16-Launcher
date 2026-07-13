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
// DLAVIE GAMEHUB v2 — Immersive + Side Menu + Download Tab
// ═══════════════════════════════════════════════════════════════════════════

private val GHBg = Color(0xFF050508)
private val GHDrawerBg = Color(0xFF0A0A14)
private val GHGlass = Color(0x10FFFFFF)
private val GHGlassHi = Color(0x18FFFFFF)
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextSoft = Color(0xFFAAAAAA)
private val GHTextDim = Color(0xFF555555)
private val GHAccent = Color(0xFF00D4FF)
private val GHGreen = Color(0xFF00D26A)
private val GHAmber = Color(0xFFFF9900)
private val GHRed = Color(0xFFFF5252)

// ─── Transition ──────────────────────────────────────────────────────────────
@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(visible) {
        if (visible) { phase = 1; delay(100); phase = 2; delay(600); phase = 3; delay(400); onComplete(); phase = 0 }
    }
    if (visible && phase > 0) {
        Box(Modifier.fillMaxSize().background(GHBg), contentAlignment = Alignment.Center) {
            val s by animateFloatAsState(when (phase) { 1 -> 1f; 2 -> 1f; 3 -> 1.5f; else -> 0f }, tween(if (phase == 1 || phase == 3) 400 else 0, easing = FastOutSlowInEasing), label = "s")
            val a by animateFloatAsState(when (phase) { 1 -> 1f; 2 -> 1f; 3 -> 0f; else -> 0f }, tween(if (phase == 1 || phase == 3) 400 else 0, easing = FastOutSlowInEasing), label = "a")
            val g by animateFloatAsState(if (phase == 2) 1.15f else 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "g")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s; this.alpha = a }) {
                Box(Modifier.size(72.dp * g).clip(RoundedCornerShape(18.dp)).background(GHTextWhite), contentAlignment = Alignment.Center) { Text("DL", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(14.dp))
                Text("DLAVIE", color = GHTextWhite, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
                Spacer(Modifier.height(2.dp))
                Text("Cloud Gaming Platform", color = GHTextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
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
    // v7.9.79: showTransition selalu true saat GameHub dibuka (fresh composition)
    var showTransition by remember { mutableStateOf(true) }
    // Force transition setiap kali GameHub overlay muncul
    LaunchedEffect(Unit) { showTransition = true }

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
    GameHubTransition(visible = showTransition) { showTransition = false }

    // ── User profile info ──
    val api = remember { CommunityApi(context) }
    val displayName = remember { api.displayName() }
    val username = remember { api.username() }

    Box(Modifier.fillMaxSize().background(GHBg)) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF0A0A14), GHBg), radius = 800f)))

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
        // ── TOP BAR (transparent, floating) ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = GHTextWhite, modifier = Modifier.size(22.dp).clickable { onMenu() })
            Spacer(Modifier.width(16.dp))
            GHCtrlBtn("LB")
            Spacer(Modifier.width(12.dp))
            Text("Dashboard", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(12.dp))
            GHCtrlBtn("RB")
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = GHTextSoft, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("$batteryLevel%", color = GHTextSoft, fontSize = 11.sp, fontFamily = InterFontFamily)
            Spacer(Modifier.width(6.dp))
            Text(currentTime, color = GHTextSoft, fontSize = 11.sp, fontFamily = InterFontFamily)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Exit", tint = GHTextSoft, modifier = Modifier.size(18.dp).clickable { onExit() })
        }

        // ── Category tabs (pill style, small) ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GHPillTab("DLavie", dlavieGames.size, selectedTab == 0, { onTabSelect(0) })
            GHPillTab("My Library", userGames.size, selectedTab == 1, { onTabSelect(1) })
            if (selectedTab == 1) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(28.dp).clip(CircleShape).background(GHAccent.copy(0.15f)).clickable { apkPickerLauncher.launch("application/vnd.android.package-archive") }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, contentDescription = "Add", tint = GHAccent, modifier = Modifier.size(16.dp)) }
            }
        }

        // ── Game cards ──
        Box(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            if (selectedTab == 0) {
                LazyRow(contentPadding = PaddingValues(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(dlavieGames) { game -> GHGlassCard(game, isPackageInstalled(context, game.packageName), { onGameClick(game.packageName) }, { onContextMenu(Pair(game.packageName, false)) }) }
                }
            } else {
                if (userGames.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No games yet", color = GHTextSoft, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap + to import an APK", color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                } else {
                    LazyRow(contentPadding = PaddingValues(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(userGames) { ug -> GHGlassUserCard(ug, isPackageInstalled(context, ug.packageName), { if (isPackageInstalled(context, ug.packageName)) launchGame(context, ug.packageName) else { val f = File(ug.sourcePath); if (f.exists()) { try { val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", f); context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) { } } } }, { onContextMenu(Pair(ug.packageName, true)) }) }
                    }
                }
            }
        }

        // ── Bottom bar (transparent) ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) { GHCtrlBtn("Y"); Spacer(Modifier.width(6.dp)); Text("Search", color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily) }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onMenu() }) { Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = GHTextSoft, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Menu", color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily) }
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
        // ── Top bar ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = GHTextWhite, modifier = Modifier.size(22.dp).clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Text("Download", color = GHTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.weight(1f))
        }

        // ── Tabs ──
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GHPillTab("Download Task", 0, selectedTab == 0, { selectedTab = 0 })
            GHPillTab("Game Management", 0, selectedTab == 1, { selectedTab = 1 })
        }

        // ── Content ──
        Column(Modifier.fillMaxWidth().weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Storage info
            Surface(shape = RoundedCornerShape(12.dp), color = GHGlass, border = BorderStroke(1.dp, GHGlass)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Storage", color = GHTextSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Game", color = GHTextDim, fontSize = 12.sp); Text(formatBytes(gameStorage), color = GHTextWhite, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Other", color = GHTextDim, fontSize = 12.sp); Text(formatBytes(totalStorage - availStorage - gameStorage), color = GHTextWhite, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Available", color = GHTextDim, fontSize = 12.sp); Text(formatBytes(availStorage), color = GHGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    // Storage bar
                    val usedPct = if (totalStorage > 0) ((totalStorage - availStorage).toFloat() / totalStorage.toFloat()) else 0f
                    LinearProgressIndicator(progress = { usedPct }, modifier = Modifier.fillMaxWidth().height(4.dp), color = GHAccent, trackColor = GHGlass)
                }
            }

            if (selectedTab == 0) {
                // Download Task
                Text("Download Soon (0)", color = GHTextSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                Surface(shape = RoundedCornerShape(12.dp), color = GHGlass, border = BorderStroke(1.dp, GHGlass), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("No Queued Tasks", color = GHTextDim, fontSize = 13.sp, fontFamily = InterFontFamily) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Completed", color = GHTextSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Text("Clear All", color = GHAccent, fontSize = 12.sp, fontFamily = InterFontFamily, modifier = Modifier.clickable { })
                }
                Surface(shape = RoundedCornerShape(12.dp), color = GHGlass, border = BorderStroke(1.dp, GHGlass), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("No completed tasks", color = GHTextDim, fontSize = 13.sp, fontFamily = InterFontFamily) }
                }
            } else {
                // Game Management
                val games = listOf(
                    Triple("FIFA 16 Mobile", GAME_PKG_16, "DLavie 26 Mod"),
                    Triple("FIFA 15 Mobile", GAME_PKG_15, "DLavie 15 Mod")
                )
                games.forEach { (title, pkg, subtitle) ->
                    val installed = isPackageInstalled(context, pkg)
                    Surface(shape = RoundedCornerShape(12.dp), color = GHGlass, border = BorderStroke(1.dp, GHGlass), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(GHGlassHi), contentAlignment = Alignment.Center) { Text(title.take(2), color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Black) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                Text(subtitle, color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily)
                                Text(if (installed) "Installed" else "Not installed", color = if (installed) GHGreen else GHTextDim, fontSize = 10.sp, fontFamily = InterFontFamily)
                            }
                            if (installed) {
                                IconButton(onClick = { try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { } }) { Icon(Icons.Rounded.Delete, contentDescription = "Uninstall", tint = GHRed.copy(0.7f), modifier = Modifier.size(20.dp)) }
                            } else {
                                IconButton(onClick = { /* reinstall */ }) { Icon(Icons.Rounded.Download, contentDescription = "Install", tint = GHAccent, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            }
        }

        // Bottom
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
            GHCtrlBtn("B"); Spacer(Modifier.width(6.dp)); Text("Back", color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily, modifier = Modifier.clickable { onBack() })
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = GHTextWhite, modifier = Modifier.size(22.dp).clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Text("Settings", color = GHTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Gamepad
            Text("GAMEPAD", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHGlass, border = BorderStroke(1.dp, GHGlass)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = if (gamepadCount > 0) GHGreen else GHTextDim, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Gamepad Connection", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(if (gamepadCount > 0) "$gamepadCount gamepad(s) connected" else "No gamepad detected", color = if (gamepadCount > 0) GHGreen else GHTextDim, fontSize = 12.sp) }
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (gamepadCount > 0) GHGreen else GHTextDim))
                    }
                    if (gamepadNames.isNotEmpty()) { Spacer(Modifier.height(10.dp)); gamepadNames.forEach { n -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) { Box(Modifier.size(6.dp).clip(CircleShape).background(GHGreen)); Spacer(Modifier.width(8.dp)); Text(n, color = GHTextSoft, fontSize = 12.sp) } } }
                }
            }
            // Display
            Text("DISPLAY", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHGlass, border = BorderStroke(1.dp, GHGlass)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Auto Rotate", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Landscape mode in GameHub", color = GHTextDim, fontSize = 12.sp) }
                    Box(Modifier.size(24.dp).clip(CircleShape).background(GHGreen), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp)) }
                }
            }
            // About
            Text("ABOUT", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = GHGlass, border = BorderStroke(1.dp, GHGlass)) {
                Column(Modifier.padding(16.dp)) { Text("DLavie GameHub", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Cloud Gaming Platform v7.9.79", color = GHTextDim, fontSize = 12.sp) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) { GHCtrlBtn("B"); Spacer(Modifier.width(6.dp)); Text("Back", color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily, modifier = Modifier.clickable { onBack() }) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SIDE MENU DRAWER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDrawer(displayName: String, username: String, currentScreen: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)).clickable { onDismiss() }) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(0.35f).background(GHDrawerBg).clickable { }) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                // Profile
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(GHAccent.copy(0.15f)), contentAlignment = Alignment.Center) { Text((displayName.ifEmpty { "D" }).take(1), color = GHAccent, fontSize = 18.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(displayName.ifEmpty { "DLavie Player" }, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("@${username.ifEmpty { "user" }}", color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily)
                    }
                }
                Spacer(Modifier.height(28.dp))
                // Menu items
                GHDrawerItem(Icons.Rounded.Home, "Home", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.SportsEsports, "Game", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.Download, "Download", currentScreen == 1) { onSelect(1) }
                GHDrawerItem(Icons.Rounded.Settings, "Settings", currentScreen == 2) { onSelect(2) }
                Spacer(Modifier.weight(1f))
                // Exit
                GHDrawerItem(Icons.Rounded.Close, "Exit GameHub", false) { onExit() }
            }
        }
    }
}

@Composable
private fun GHDrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) GHGlassHi else Color.Transparent).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = if (selected) GHAccent else GHTextSoft, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (selected) GHTextWhite else GHTextSoft, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontFamily = InterFontFamily)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHCtrlBtn(label: String) {
    Box(Modifier.size(26.dp).clip(RoundedCornerShape(5.dp)).background(GHGlass).border(1.dp, GHTextDim.copy(0.3f), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) { Text(label, color = GHTextSoft, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) }
}

@Composable
private fun GHPillTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) GHGlassHi else Color.Transparent).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (selected) GHTextWhite else GHTextDim, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontFamily = InterFontFamily)
            if (count > 0) { Spacer(Modifier.width(4.dp)); Text(count.toString(), color = if (selected) GHAccent else GHTextDim.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGlassCard(game: GameItem, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(Modifier.width(200.dp).height(280.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0x08FFFFFF)), border = BorderStroke(1.dp, GHGlass)) {
        Box(Modifier.fillMaxSize()) {
            if (game.coverImageRes != null) { Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            else { Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) { Text(game.coverText, color = GHTextWhite, fontSize = 36.sp, fontWeight = FontWeight.Black) } }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.2f), Color.Transparent, Color.Black.copy(0.75f)))))
            val (sc, st) = when (game.serverStatus) { ServerStatus.ONLINE -> Pair(GHGreen, "ONLINE"); ServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT"); ServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE"); ServerStatus.BUSY -> Pair(GHAmber, "BUSY") }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(sc).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(st, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black) }
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(game.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp)); Text(game.subtitle, color = GHTextSoft, fontSize = 10.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { GHCtrlBtn("A"); Spacer(Modifier.width(6.dp)); Text(if (isInstalled) "Launch" else "Install", color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGlassUserCard(userGame: UserGame, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    Card(Modifier.width(200.dp).height(280.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0x08FFFFFF)), border = BorderStroke(1.dp, GHGlass)) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))), contentAlignment = Alignment.Center) {
                val icon = remember(userGame.packageName) { try { context.packageManager.getApplicationIcon(userGame.packageName) } catch (_: Exception) { null } }
                if (icon != null) AsyncImage(model = icon, contentDescription = userGame.title, modifier = Modifier.size(64.dp), contentScale = ContentScale.Fit)
                else Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(48.dp))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.2f), Color.Transparent, Color.Black.copy(0.75f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(userGame.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp)); Text(userGame.packageName.take(20), color = GHTextSoft, fontSize = 10.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { GHCtrlBtn("A"); Spacer(Modifier.width(6.dp)); Text(if (isInstalled) "Launch" else "Install", color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily) }
            }
        }
    }
}

@Composable
private fun GHContextMenu(pkg: String, isUser: Boolean, installed: Boolean, onDismiss: () -> Unit, onLaunch: () -> Unit, onView: () -> Unit, onUninstall: () -> Unit, onRemove: () -> Unit, onClear: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Game Options", color = GHTextWhite, fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (installed) { GHMenuItem(Icons.Rounded.PlayArrow, "Launch Game", onLaunch) }
        GHMenuItem(Icons.Rounded.Info, "View Details", onView)
        if (!isUser && installed) { GHMenuItem(Icons.Rounded.CleaningServices, "Clear Data Only", onClear) }
        if (isUser) { if (installed) { GHMenuItem(Icons.Rounded.Delete, "Uninstall Game", onUninstall) }; GHMenuItem(Icons.Rounded.RemoveCircle, "Remove from Library", onRemove) }
    } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GHTextSoft) } }, containerColor = Color(0xFF111111))
}

@Composable
private fun GHMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(14.dp)); Text(label, color = GHTextWhite, fontSize = 14.sp, fontFamily = InterFontFamily)
    }
}
