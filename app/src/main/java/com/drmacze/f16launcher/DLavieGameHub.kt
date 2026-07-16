package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB v297 — Reference match (Screenshot_20260716-065844.jpg)
// ═══════════════════════════════════════════════════════════════════════════
// Full screen, dark gradient bg, glassmorphic bars, 2 game cards centered,
// View Detail + 3-dot menu below each card.
// ═══════════════════════════════════════════════════════════════════════════

// ── Design tokens ──
private val BgDark = Color(0xFF0A0A0A)
private val BgGradientStart = Color(0xFF000000)
private val BgGradientEnd = Color(0xFF1A1A1A)
private val GlassBg = Color(0x20000000)       // 12% black glass
private val CardBorder = Color(0x30FFFFFF)     // 19% white border
private val CardBorderFocused = Color(0x60FFFFFF) // 37% white focused
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF888888)
private val AccentBlue = Color(0xFF3498DB)
private val AccentPurple = Color(0xFF9B59B6)
private val GreenOnline = Color(0xFF4CAF50)
private val AmberMaint = Color(0xFFFFB347)
private val RedOff = Color(0xFFFF5252)
private val Gold = Color(0xFFFFD700)

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

private fun ghShareGame(context: Context, game: GameItem) {
    val shareUrl = "https://drmacze.github.io/dlavie-web/#/game?pkg=${game.packageName}"
    val shareText = "Main ${game.title} di DLavie Launcher! Download sekarang: $shareUrl"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "DLavie Launcher - ${game.title}")
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try { context.startActivity(Intent.createChooser(shareIntent, "Bagikan ${game.title}").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
}

private const val GH_PREFS = "gh_favorites_v297"
private fun ghLoadFavorites(c: Context): Set<String> = try {
    c.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE).getStringSet("fav_pkgs", emptySet()) ?: emptySet()
} catch (_: Exception) { emptySet() }
private fun ghToggleFavorite(c: Context, pkg: String): Set<String> {
    val current = ghLoadFavorites(c).toMutableSet()
    if (pkg in current) current.remove(pkg) else current.add(pkg)
    c.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE).edit().putStringSet("fav_pkgs", current).apply()
    return current
}

// ═══════════════════════════════════════════════════════════════════════════
// TRANSITION
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
        Box(Modifier.fillMaxSize().background(BgDark), contentAlignment = Alignment.Center) {
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
                    }).background(TextWhite), contentAlignment = Alignment.Center
                ) { Text("DL", color = BgDark, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(20.dp))
                Text(typedText, color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.graphicsLayer { this.alpha = textAlpha })
                Spacer(Modifier.height(16.dp))
                if (loadingMsg.isNotEmpty()) { Text(loadingMsg, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.graphicsLayer { this.alpha = msgAlpha }) }
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
    onGameClick: (String) -> Unit = {},
    api: CommunityApi? = null
) {
    val context = LocalContext.current
    var showTransition by remember { mutableStateOf(true) }

    val displayName = remember { api?.displayName()?.ifEmpty { "DLavie Player" } ?: "DLavie Player" }
    val avatarUrl = remember { api?.avatarUrl() ?: "" }
    val username = remember { api?.username()?.ifBlank { "user" } ?: "user" }
    val role = remember { api?.role() ?: "member" }

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

    val games = remember {
        listOf(
            GameItem(title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod", packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity", coverGradient = listOf(Color(0xFF0A1628), Color(0xFF1A3A6B)), coverText = "DL", coverImageRes = R.drawable.fifa16_cover, serverStatus = ServerStatus.ONLINE, description = "FIFA 16 Mobile dengan mod DLavie 26", version = "v26.0", sizeMb = "34 MB", apkUrl = FIFA16_APK_URL),
            GameItem(title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod", packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY, coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)), coverText = "D15", coverImageRes = R.drawable.fifa15_cover, serverStatus = ServerStatus.MAINTENANCE, description = "FIFA 15 Mobile dengan mod DLavie 15", version = "v15.0", sizeMb = "22 MB", apkUrl = FIFA15_APK_URL)
        )
    }

    var showDetail by remember { mutableStateOf<GameItem?>(null) }
    var favorites by remember { mutableStateOf(ghLoadFavorites(context)) }
    var selectedTab by remember { mutableStateOf(1) } // 0=Store, 1=Library, 2=Videos, 3=Settings

    Box(Modifier.fillMaxSize().background(BgDark)) {
        // ── DARK GRADIENT BACKGROUND (no game cover blur) ──
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgGradientStart, BgGradientEnd))))

        if (!showTransition) {
            showDetail?.let { game ->
                GameDetailCompact(game = game, context = context, onBack = { showDetail = null }, onPlay = { ghLaunch(context, game.packageName); onGameClick(game.packageName) })
            } ?: run {
                Column(Modifier.fillMaxSize()) {
                    // ── TOP BAR (glassmorphic, transparent) ──
                    RefTopBar(
                        currentTime = currentTime, batteryLevel = batteryLevel,
                        displayName = displayName, avatarUrl = avatarUrl,
                        username = username, role = role,
                        selectedTab = selectedTab, onTabSelect = { selectedTab = it },
                        onExit = onExit
                    )

                    // ── CONTENT (centered, one screen, no scroll) ──
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when (selectedTab) {
                            1 -> { // Library
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 32.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    itemsIndexed(games) { idx, game ->
                                        RefGameCard(
                                            game = game,
                                            isInstalled = ghIsInstalled(context, game.packageName),
                                            isFavorite = game.packageName in favorites,
                                            onClick = { showDetail = game },
                                            onPlay = { ghLaunch(context, game.packageName); onGameClick(game.packageName) },
                                            onToggleFavorite = { favorites = ghToggleFavorite(context, game.packageName) },
                                            onShare = { ghShareGame(context, game) }
                                        )
                                    }
                                }
                            }
                            else -> {
                                Text(when(selectedTab) { 0 -> "Store"; 2 -> "Videos"; 3 -> "Settings"; else -> "" }, color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ── BOTTOM NAV (glassmorphic, transparent) ──
                    RefBottomNav(selectedTab = selectedTab, onTabSelect = { selectedTab = it }, onExit = onExit)
                }
            }
        }

        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REFERENCE TOP BAR — profile + tabs + time
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RefTopBar(
    currentTime: String, batteryLevel: Int,
    displayName: String, avatarUrl: String,
    username: String, role: String,
    selectedTab: Int, onTabSelect: (Int) -> Unit,
    onExit: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // Row 1: Profile + time
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(Modifier.size(36.dp).clip(CircleShape).background(AccentBlue).border(2.dp, TextWhite, CircleShape), contentAlignment = Alignment.Center) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(model = avatarUrl, contentDescription = "Avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Text(displayName.take(1).uppercase(), color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(displayName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
                    Spacer(Modifier.width(6.dp))
                    val badge = when (role.lowercase()) { "admin" -> "ADMIN"; "developer" -> "DEV"; "owner" -> "OWNER"; "moderator" -> "MOD"; else -> "" }
                    if (badge.isNotEmpty()) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(BgDark).border(1.dp, Gold, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text(badge, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("@$username", color = TextGray, fontSize = 12.sp)
            }
            // Time + battery
            Text(currentTime, color = TextWhite, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.BatteryFull, "Battery", tint = TextGray, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(2.dp))
            Text("$batteryLevel%", color = TextGray, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowBack, "Exit", tint = TextWhite, modifier = Modifier.size(20.dp).clickable { onExit() })
        }

        // Row 2: Tabs
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("Store", "Library", "Videos", "Settings").forEachIndexed { idx, label ->
                val selected = selectedTab == idx
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onTabSelect(idx) }) {
                    Text(label, color = if (selected) TextWhite else TextGray, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.height(4.dp))
                    if (selected) { Box(Modifier.width(24.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(TextWhite)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REFERENCE GAME CARD — full cover, status badge, heart, View Detail + 3-dot
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RefGameCard(
    game: GameItem, isInstalled: Boolean, isFavorite: Boolean,
    onClick: () -> Unit, onPlay: () -> Unit,
    onToggleFavorite: () -> Unit, onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val borderColor = if (isInstalled) AccentBlue else AccentPurple

    Column(Modifier.width(180.dp)) {
        // ── CARD COVER (180x260dp) ──
        Box(Modifier.width(180.dp).height(260.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, borderColor, RoundedCornerShape(12.dp)).clickable { onClick() }) {
            // Cover image
            if (game.coverImageRes != null) {
                Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) {
                    Text(game.coverText, color = TextWhite, fontSize = 36.sp, fontWeight = FontWeight.Black)
                }
            }

            // Bottom gradient
            Box(Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f)))))

            // Status badge (top-left)
            val (sc, st) = when (game.serverStatus) {
                ServerStatus.ONLINE -> Pair(GreenOnline, "ONLINE")
                ServerStatus.MAINTENANCE -> Pair(AmberMaint, "MAINT")
                ServerStatus.OFFLINE -> Pair(RedOff, "OFFLINE")
                ServerStatus.BUSY -> Pair(AmberMaint, "BUSY")
            }
            Box(Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(4.dp)).background(sc.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(st, color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Heart icon (top-right)
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Favorite", tint = if (isFavorite) RedOff else TextWhite,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(22.dp).clickable { onToggleFavorite() }
            )

            // Title + subtitle (bottom)
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(game.title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(game.subtitle, color = TextGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // ── VIEW DETAIL + 3-DOT MENU (below card) ──
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // View Detail button
            Box(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(GlassBg).border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = TextGray, modifier = Modifier.size(15.dp))
                    Text("View Detail", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            // 3-dot menu
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(GlassBg).border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .clickable { showMenu = !showMenu },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.MoreVert, "Menu", tint = TextWhite, modifier = Modifier.size(18.dp)) }
        }

        // ── DROPDOWN MENU ──
        if (showMenu) {
            Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
                Column(Modifier.width(170.dp).clip(RoundedCornerShape(10.dp)).background(BgDark.copy(alpha = 0.95f)).border(1.dp, CardBorderFocused, RoundedCornerShape(10.dp)).padding(4.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { onShare(); showMenu = false }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Share, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Text("Share", color = TextWhite, fontSize = 12.sp)
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { onToggleFavorite(); showMenu = false }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFavorite) RedOff else TextWhite, modifier = Modifier.size(16.dp))
                        Text(if (isFavorite) "Remove Favorite" else "Add to Favorite", color = TextWhite, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// REFERENCE BOTTOM NAV — 5 items, transparent
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RefBottomNav(selectedTab: Int, onTabSelect: (Int) -> Unit, onExit: () -> Unit) {
    val items = listOf(
        Triple("Home", Icons.Rounded.Home, 1),
        Triple("Library", Icons.Rounded.SportsEsports, 1),
        Triple("Store", Icons.Rounded.Store, 0),
        Triple("Videos", Icons.Rounded.VideoLibrary, 2),
        Triple("Exit", Icons.Rounded.Close, -1)
    )
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (label, icon, tab) ->
            val selected = selectedTab == tab && tab != -1
            Column(Modifier.clip(RoundedCornerShape(8.dp)).clickable { if (tab == -1) onExit() else onTabSelect(tab) }.padding(horizontal = 14.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, label, tint = if (selected) TextWhite else TextGray, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, color = if (selected) TextWhite else TextGray, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAME DETAIL COMPACT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GameDetailCompact(game: GameItem, context: Context, onBack: () -> Unit, onPlay: () -> Unit) {
    val isInstalled = ghIsInstalled(context, game.packageName)
    Box(Modifier.fillMaxSize().background(BgDark)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgGradientStart, BgGradientEnd))))
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = TextWhite, modifier = Modifier.size(24.dp).clickable { onBack() })
                Spacer(Modifier.width(16.dp))
                Text(game.title, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp))) {
                if (game.coverImageRes != null) { Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
                Text(game.title, color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
            }
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Description", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(game.description, color = TextGray, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column { Text("Version", color = TextGray, fontSize = 10.sp); Text(game.version, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    Column { Text("Size", color = TextGray, fontSize = 10.sp); Text(game.sizeMb, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    Column { Text("Status", color = TextGray, fontSize = 10.sp); Text(when(game.serverStatus) { ServerStatus.ONLINE -> "Online"; ServerStatus.MAINTENANCE -> "Maintenance"; else -> "Offline" }, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(if (isInstalled) GreenOnline else AccentBlue).clickable { onPlay() }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.Download, null, tint = BgDark, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isInstalled) "Play Now" else "Install", color = BgDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
