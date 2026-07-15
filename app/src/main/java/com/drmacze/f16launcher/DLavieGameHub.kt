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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.*
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB v292 — Full-screen redesign (reference match)
// ═══════════════════════════════════════════════════════════════════════════
// Full screen, no edge padding. Adaptive blurred background.
// Glassmorphic top/bottom bars. Featured game card + horizontal grid.
// ═══════════════════════════════════════════════════════════════════════════

// ── Design tokens ──
private val GHBg = Color(0xFF000000)
private val GHGlassBar = Color(0x1A000000)       // 10% black for glass bars
private val GHGlassCard = Color(0x26FFFFFF)       // 15% white for glass cards
private val GHGlassCardHi = Color(0x40FFFFFF)     // 25% white for focused
private val GHBorder = Color(0x1AFFFFFF)          // 10% white border
private val GHBorderHi = Color(0x40FFFFFF)        // 25% white border
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextGray = Color(0xFFAAAAAA)
private val GHTextDim = Color(0xFF666666)
private val GHAccent = Color(0xFF00E5FF)          // Cyan accent
private val GHGreen = Color(0xFF00FF00)           // Progress/achievement
private val GHGold = Color(0xFFFFD700)            // Badges/ratings
private val GHRed = Color(0xFFFF5252)
private val GHAmber = Color(0xFFFFB347)

// ── Helpers ──
private fun ghBattery(c: Context): Int = try {
    (c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
} catch (_: Exception) { 100 }

private fun ghTime(c: Context): String = try {
    val is24 = DateFormat.is24HourFormat(c)
    SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date())
} catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

private fun ghIsInstalled(c: Context, pkg: String): Boolean = try {
    c.packageManager.getPackageInfo(pkg, 0); true
} catch (_: Throwable) { false }

private fun ghLaunch(c: Context, pkg: String) = try {
    val intent = c.packageManager.getLaunchIntentForPackage(pkg)
    if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(intent); true } else false
} catch (_: Throwable) { false }

// ═══════════════════════════════════════════════════════════════════════════
// TRANSITION — hexagon logo + typing DLAVIE + loading
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    var typedText by remember { mutableStateOf("") }
    var loadingMsg by remember { mutableStateOf("") }
    val fullText = "DLAVIE"
    val loadingMessages = listOf("Memuat aset game...", "Menata antarmuka...", "Memindai data pengguna...", "Menghubungkan ke server...", "Menyiapkan GameHub...")

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
                Box(Modifier.size(72.dp).graphicsLayer { scaleX = logoScale; scaleY = logoScale; this.alpha = logoAlpha }
                    .clip(androidx.compose.foundation.shape.GenericShape { _, _ ->
                        val r = 70f
                        moveTo(0f, -r); lineTo(r * 0.866f, -r * 0.5f); lineTo(r * 0.866f, r * 0.5f)
                        lineTo(0f, r); lineTo(-r * 0.866f, r * 0.5f); lineTo(-r * 0.866f, -r * 0.5f); close()
                    }).background(GHTextWhite), contentAlignment = Alignment.Center
                ) { Text("DL", color = GHBg, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(20.dp))
                Text(typedText, color = GHTextWhite, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.graphicsLayer { this.alpha = textAlpha })
                Spacer(Modifier.height(16.dp))
                if (loadingMsg.isNotEmpty()) { Text(loadingMsg, color = GHTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.graphicsLayer { this.alpha = msgAlpha }) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE — Full screen, adaptive bg, glassmorphic bars
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(
    onExit: () -> Unit = {},
    onNav: (Page) -> Unit = {},
    onGameClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showTransition by remember { mutableStateOf(true) }

    // Immersive mode
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
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }
        onDispose {
            activity?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { window.setDecorFitsSystemWindows(true); window.insetsController?.show(WindowInsets.Type.systemBars()) }
                else { @Suppress("DEPRECATION") window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE }
            }
        }
    }

    var currentTime by remember { mutableStateOf(ghTime(context)) }
    var batteryLevel by remember { mutableStateOf(ghBattery(context)) }
    LaunchedEffect(Unit) { while (true) { currentTime = ghTime(context); batteryLevel = ghBattery(context); delay(30_000) } }

    // Game data (REAL GameItem)
    val games = remember {
        listOf(
            GameItem(title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod", packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity", coverGradient = listOf(Color(0xFF0A1628), Color(0xFF1A3A6B)), coverText = "DL", coverImageRes = R.drawable.fifa16_cover, serverStatus = ServerStatus.ONLINE, description = "FIFA 16 Mobile dengan mod DLavie 26", version = "v26.0", sizeMb = "34 MB", apkUrl = FIFA16_APK_URL),
            GameItem(title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod", packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY, coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)), coverText = "D15", coverImageRes = R.drawable.fifa15_cover, serverStatus = ServerStatus.MAINTENANCE, description = "FIFA 15 Mobile dengan mod DLavie 15", version = "v15.0", sizeMb = "22 MB", apkUrl = FIFA15_APK_URL)
        )
    }

    var featuredGame by remember { mutableStateOf(games.firstOrNull()) }
    var selectedTab by remember { mutableStateOf(1) } // 0=Store, 1=Library, 2=Videos, 3=Settings
    var showDetail by remember { mutableStateOf<GameItem?>(null) }

    Box(Modifier.fillMaxSize().background(GHBg)) {
        // ── ADAPTIVE BLURRED BACKGROUND ──
        featuredGame?.let { game ->
            if (game.coverImageRes != null) {
                Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = null, modifier = Modifier.fillMaxSize().blur(60.dp), contentScale = ContentScale.Crop)
            }
            // Dark overlay
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x99000000), Color(0xCC000000), Color(0x99000000)))))
        }

        if (!showTransition) {
            // ── DETAIL OVERLAY ──
            showDetail?.let { game ->
                GameDetailCompact(game = game, context = context, onBack = { showDetail = null }, onPlay = { ghLaunch(context, game.packageName); onGameClick(game.packageName) })
            } ?: run {
                // ── MAIN LAYOUT (full screen) ──
                Column(Modifier.fillMaxSize()) {
                    // TOP BAR (glassmorphic, transparent)
                    GlassTopBar(
                        currentTime = currentTime,
                        batteryLevel = batteryLevel,
                        selectedTab = selectedTab,
                        onTabSelect = { selectedTab = it },
                        onExit = onExit
                    )

                    // CONTENT (fills remaining space)
                    Box(Modifier.weight(1f)) {
                        when (selectedTab) {
                            1 -> { // Library (default)
                                LibraryContent(
                                    games = games,
                                    context = context,
                                    featuredGame = featuredGame,
                                    onFeaturedChange = { featuredGame = it },
                                    onOpenDetail = { showDetail = it },
                                    onPlay = { pkg -> ghLaunch(context, pkg); onGameClick(pkg) }
                                )
                            }
                            else -> {
                                // Store/Videos/Settings placeholder
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(when(selectedTab) { 0 -> "Store"; 2 -> "Videos"; 3 -> "Settings"; else -> "" }, color = GHTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // BOTTOM NAV (glassmorphic)
                    GlassBottomNav(selectedTab = selectedTab, onTabSelect = { selectedTab = it }, onExit = onExit)
                }
            }
        }

        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS TOP BAR — profile + tabs + time
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassTopBar(
    currentTime: String,
    batteryLevel: Int,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onExit: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(GHGlassBar)) {
        // Row 1: Profile + time
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Profile avatar
            Box(Modifier.size(32.dp).clip(CircleShape).background(GHAccent).border(2.dp, GHTextWhite, CircleShape), contentAlignment = Alignment.Center) {
                Text("D", color = GHBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DLavie Player", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(GHBg).border(1.dp, GHGold, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                        Text("ULTIMATE", color = GHGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("18291 points", color = GHTextGray, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            // Time + battery
            Text(currentTime, color = GHTextWhite, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.BatteryFull, "Battery", tint = GHTextGray, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(2.dp))
            Text("$batteryLevel%", color = GHTextGray, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowBack, "Exit", tint = GHTextWhite, modifier = Modifier.size(20.dp).clickable { onExit() })
        }

        // Row 2: Navigation tabs
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val tabs = listOf("Store", "Library", "Videos", "Settings")
            tabs.forEachIndexed { idx, label ->
                val selected = selectedTab == idx
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTabSelect(idx) }) {
                    Text(label, color = if (selected) GHTextWhite else GHTextGray, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.height(4.dp))
                    if (selected) { Box(Modifier.width(24.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(GHTextWhite)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIBRARY CONTENT — featured game + horizontal grid
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LibraryContent(
    games: List<GameItem>,
    context: Context,
    featuredGame: GameItem?,
    onFeaturedChange: (GameItem) -> Unit,
    onOpenDetail: (GameItem) -> Unit,
    onPlay: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // ── FEATURED GAME CARD (large) ──
        featuredGame?.let { game ->
            FeaturedGameCard(game = game, isInstalled = ghIsInstalled(context, game.packageName), onPlay = { onPlay(game.packageName) }, onOpenDetail = { onOpenDetail(game) })
            Spacer(Modifier.height(16.dp))
        }

        // ── SECTION HEADER ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("All Games", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("See All", color = GHAccent, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        // ── HORIZONTAL GAME GRID ──
        val listState = rememberLazyListState()
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { idx -> if (idx < games.size) onFeaturedChange(games[idx]) }
        }

        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            itemsIndexed(games) { idx, game ->
                GameGridCard(
                    game = game,
                    isInstalled = ghIsInstalled(context, game.packageName),
                    isFocused = game.packageName == featuredGame?.packageName,
                    onClick = { onOpenDetail(game) },
                    onPlay = { onPlay(game.packageName) }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FEATURED GAME CARD — large, with progress + achievement
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FeaturedGameCard(game: GameItem, isInstalled: Boolean, onPlay: () -> Unit, onOpenDetail: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).clickable { onOpenDetail() }) {
        // Cover image
        if (game.coverImageRes != null) {
            Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) {
                Text(game.coverText, color = GHTextWhite, fontSize = 36.sp, fontWeight = FontWeight.Black)
            }
        }

        // Dark gradient overlay
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Black.copy(0.85f)))))

        // Content
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(game.title, color = GHTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(game.subtitle, color = GHTextGray, fontSize = 13.sp)
                if (isInstalled) {
                    Text("Installed", color = GHGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))

            // Play button
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(if (isInstalled) GHGreen else GHAccent).clickable { onPlay() }.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.Download, null, tint = GHBg, modifier = Modifier.size(16.dp))
                Text(if (isInstalled) "Play" else "Install", color = GHBg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Status badge
        val (sc, st) = when (game.serverStatus) {
            ServerStatus.ONLINE -> Pair(GHGreen, "ONLINE")
            ServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT")
            ServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE")
            ServerStatus.BUSY -> Pair(GHAmber, "BUSY")
        }
        Box(Modifier.align(Alignment.TopEnd).padding(12.dp).clip(RoundedCornerShape(4.dp)).background(sc.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(st, color = GHTextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAME GRID CARD — smaller, portrait, scrollable
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GameGridCard(
    game: GameItem,
    isInstalled: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val scale by animateFloatAsState(if (isFocused) 1f else 0.9f, spring(dampingRatio = 0.5f, stiffness = 200f), label = "scale")
    val alpha by animateFloatAsState(if (isFocused) 1f else 0.6f, tween(300), label = "alpha")

    Column(Modifier.width(120.dp).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
        // Card cover (120x160dp, portrait)
        Box(Modifier.width(120.dp).height(160.dp).clip(RoundedCornerShape(8.dp)).clickable { onClick() }) {
            if (game.coverImageRes != null) {
                Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) {
                    Text(game.coverText, color = GHTextWhite, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }

            // Bottom gradient
            Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f)))))

            // Status dot
            val dotColor = when (game.serverStatus) {
                ServerStatus.ONLINE -> GHGreen
                ServerStatus.MAINTENANCE -> GHAmber
                ServerStatus.OFFLINE -> GHRed
                ServerStatus.BUSY -> GHAmber
            }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).clip(CircleShape).background(dotColor))

            // Installed badge
            if (isInstalled) {
                Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(3.dp)).background(GHGreen.copy(alpha = 0.8f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                    Text("✓", color = GHBg, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Title below card
        Spacer(Modifier.height(6.dp))
        Text(game.title, color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(game.version, color = GHTextGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS BOTTOM NAV — 5 items
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassBottomNav(selectedTab: Int, onTabSelect: (Int) -> Unit, onExit: () -> Unit) {
    val items = listOf(
        Triple("Home", Icons.Rounded.Home, 0),
        Triple("Library", Icons.Rounded.SportsEsports, 1),
        Triple("Store", Icons.Rounded.Store, 0),
        Triple("Videos", Icons.Rounded.VideoLibrary, 2),
        Triple("Exit", Icons.Rounded.Close, -1)
    )
    Row(Modifier.fillMaxWidth().background(GHGlassBar).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (label, icon, tab) ->
            val selected = when (label) {
                "Home" -> selectedTab == 1 // Library is home
                "Library" -> selectedTab == 1
                "Store" -> selectedTab == 0
                "Videos" -> selectedTab == 2
                else -> false
            }
            Column(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                    if (tab == -1) onExit()
                    else onTabSelect(tab)
                }.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, label, tint = if (selected) GHTextWhite else GHTextGray, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, color = if (selected) GHTextWhite else GHTextGray, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAME DETAIL COMPACT — overlay saat klik game
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GameDetailCompact(game: GameItem, context: Context, onBack: () -> Unit, onPlay: () -> Unit) {
    val isInstalled = ghIsInstalled(context, game.packageName)

    Box(Modifier.fillMaxSize().background(GHBg)) {
        // Blurred bg
        if (game.coverImageRes != null) {
            Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = null, modifier = Modifier.fillMaxSize().blur(40.dp), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xAA000000), Color(0xDD000000)))))

        Column(Modifier.fillMaxSize()) {
            // Top bar with back
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = GHTextWhite, modifier = Modifier.size(24.dp).clickable { onBack() })
                Spacer(Modifier.width(16.dp))
                Text(game.title, color = GHTextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Cover image
            Box(Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp))) {
                if (game.coverImageRes != null) {
                    Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                Text(game.title, color = GHTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
            }

            // Info
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Description", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(game.description, color = GHTextGray, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoChip("Version", game.version)
                    InfoChip("Size", game.sizeMb)
                    InfoChip("Status", when(game.serverStatus) { ServerStatus.ONLINE -> "Online"; ServerStatus.MAINTENANCE -> "Maintenance"; else -> "Offline" })
                }

                Spacer(Modifier.height(20.dp))

                // Play button
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(if (isInstalled) GHGreen else GHAccent).clickable { onPlay() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.Download, null, tint = GHBg, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isInstalled) "Play Now" else "Install", color = GHBg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(label, color = GHTextGray, fontSize = 10.sp)
        Text(value, color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
