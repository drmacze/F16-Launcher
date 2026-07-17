package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch as coroutinesLaunch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB v311 — Complete redesign matching reference
// ═══════════════════════════════════════════════════════════════════════════
// - Swipe carousel (PS5 style, LazyRow + snap)
// - Center card: glow + Play Game + 3-dot (delete/info/favorite)
// - Other cards: no buttons, dimmed
// - Bottom bar: 6 transparent icons (game/settings/notif/fav/friends/power)
// - Settings: Permission(Shizuku/root) + Storage + Download(DLC) + About(links)
// ═══════════════════════════════════════════════════════════════════════════

private val Bg = Color(0xFF050505)
private val Glass = Color(0x15FFFFFF)
private val GlassHi = Color(0x25FFFFFF)
private val Divider = Color(0x10FFFFFF)
private val White = Color(0xFFFFFFFF)
private val Gray = Color(0xFF909090)
private val GrayDim = Color(0xFF555555)
private val GreenDot = Color(0xFF4ADE80)
private val AmberDot = Color(0xFFFBBF24)
private val RedDot = Color(0xFFEF4444)
private val Gold = Color(0xFFFFD700)

private fun ghBattery(c: Context): Int = try { (c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } catch (_: Exception) { 100 }
private fun ghTime(c: Context): String = try { val is24 = DateFormat.is24HourFormat(c); SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date()) } catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
private fun ghInstalled(c: Context, pkg: String): Boolean = try { c.packageManager.getPackageInfo(pkg, 0); true } catch (_: Throwable) { false }
private fun ghLaunch(c: Context, pkg: String) = try { c.packageManager.getLaunchIntentForPackage(pkg)?.let { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(it); true } ?: false } catch (_: Throwable) { false }
private fun ghShare(ctx: Context, g: GameItem) { val url = "https://drmacze.github.io/dlavie-web/#/game?pkg=${g.packageName}"; val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "DLavie - ${g.title}"); putExtra(Intent.EXTRA_TEXT, "Main ${g.title} di DLavie! $url"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; try { ctx.startActivity(Intent.createChooser(i, "Bagikan").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {} }
private const val PREFS = "gh_fav_v311"
private fun ghLoadFav(c: Context): Set<String> = try { c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("f", emptySet()) ?: emptySet() } catch (_: Exception) { emptySet() }
private fun ghToggleFav(c: Context, p: String): Set<String> { val s = ghLoadFav(c).toMutableSet(); if (p in s) s.remove(p) else s.add(p); c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("f", s).apply(); return s }

// v311: Delete game data from device
private fun ghDeleteGameData(context: Context, onInfo: () -> Unit = {}, packageName: String): Boolean {
    return try {
        val dataDir = File("/sdcard/Android/data/$packageName")
        if (dataDir.exists()) {
            dataDir.deleteRecursively()
            android.widget.Toast.makeText(context, "Data game dihapus: $packageName", android.widget.Toast.LENGTH_SHORT).show()
            true
        } else {
            android.widget.Toast.makeText(context, "Tidak ada data game untuk dihapus", android.widget.Toast.LENGTH_SHORT).show()
            false
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Gagal hapus: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        false
    }
}

// v311: Storage info
private fun ghStorageInfo(): Pair<Long, Long> = try { val stat = StatFs("/sdcard"); Pair(stat.availableBytes, stat.totalBytes) } catch (_: Exception) { Pair(0L, 0L) }
private fun ghGameStorage(pkg: String): Long = try { val dir = File("/sdcard/Android/data/$pkg"); if (dir.exists()) dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() else 0L } catch (_: Exception) { 0L }
private fun ghFormatBytes(b: Long): String = when { b >= 1_000_000_000 -> "%.1fGB".format(b / 1e9); b >= 1_000_000 -> "%.1fMB".format(b / 1e6); b >= 1_000 -> "%.1fKB".format(b / 1e3); else -> "${b}B" }

// ═══════════════════════════════════════════════════════════════════════════
// TRANSITION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }; var typed by remember { mutableStateOf("") }; var msg by remember { mutableStateOf("") }
    val full = "DLAVIE"; val msgs = listOf("Memuat aset game...", "Menata antarmuka...", "Memindai data...", "Menghubungkan ke server...", "Menyiapkan GameHub...")
    LaunchedEffect(visible) { if (visible) { phase = 0; delay(800); phase = 1; delay(800); phase = 2; for (i in full.indices) { typed = full.substring(0, i + 1); delay(120) }; delay(400); phase = 3; for (m in msgs) { msg = m; delay(700) }; msg = ""; delay(300); phase = 4; delay(600); onComplete(); phase = 5 } }
    if (visible && phase < 5) {
        Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
            val la by animateFloatAsState(when (phase) { 0 -> 0f; 1 -> 1f; 2 -> 1f; 3 -> 1f; 4 -> 0f; else -> 0f }, tween(800, easing = FastOutSlowInEasing), label = "la")
            val ls by animateFloatAsState(when (phase) { 0 -> 0.7f; 1 -> 1f; 4 -> 1.15f; else -> 1f }, tween(800, easing = FastOutSlowInEasing), label = "ls")
            val ta by animateFloatAsState(when (phase) { 2 -> 1f; 4 -> 0f; else -> 0f }, tween(400), label = "ta")
            val fo by animateFloatAsState(if (phase == 4) 0f else 1f, tween(600), label = "fo")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { this.alpha = fo }) {
                Box(Modifier.size(72.dp).graphicsLayer { scaleX = ls; scaleY = ls; this.alpha = la }.clip(androidx.compose.foundation.shape.GenericShape { _, _ -> val r = 70f; moveTo(0f, -r); lineTo(r * 0.866f, -r * 0.5f); lineTo(r * 0.866f, r * 0.5f); lineTo(0f, r); lineTo(-r * 0.866f, r * 0.5f); lineTo(-r * 0.866f, -r * 0.5f); close() }).background(White), contentAlignment = Alignment.Center) { Text("DL", color = Bg, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(20.dp))
                Text(typed, color = White, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.graphicsLayer { this.alpha = ta })
                if (msg.isNotEmpty()) { Spacer(Modifier.height(16.dp)); Text(msg, color = Gray, fontSize = 12.sp, modifier = Modifier.graphicsLayer { this.alpha = ta }) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(onExit: () -> Unit = {}, onNav: (Page) -> Unit = {}, onGameClick: (String) -> Unit = {}, api: CommunityApi? = null) {
    val context = LocalContext.current
    var showTransition by remember { mutableStateOf(true) }
    val displayName = remember { api?.displayName()?.ifEmpty { "Player" } ?: "Player" }
    val avatarUrl = remember { api?.avatarUrl() ?: "" }
    val username = remember { api?.username()?.ifBlank { "user" } ?: "user" }
    val role = remember { api?.role() ?: "member" }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { w.setDecorFitsSystemWindows(false); w.insetsController?.hide(WindowInsets.Type.systemBars()); w.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
            else { @Suppress("DEPRECATION") w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN }
        }
        onDispose { activity?.window?.let { w -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { w.setDecorFitsSystemWindows(true); w.insetsController?.show(WindowInsets.Type.systemBars()) } else { @Suppress("DEPRECATION") w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE } } }
    }

    var time by remember { mutableStateOf(ghTime(context)) }
    var batt by remember { mutableStateOf(ghBattery(context)) }
    LaunchedEffect(Unit) { while (true) { time = ghTime(context); batt = ghBattery(context); delay(30_000) } }

    val games = remember {
        listOf(
            GameItem(title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod", packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity", coverGradient = listOf(Color(0xFF0A1628), Color(0xFF1A3A6B)), coverText = "DL", coverImageRes = R.drawable.fifa16_cover, serverStatus = ServerStatus.ONLINE, description = "FIFA 16 Mobile dengan mod DLavie 26", version = "v26.0", sizeMb = "34 MB", apkUrl = FIFA16_APK_URL),
            GameItem(title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod", packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY, coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)), coverText = "D15", coverImageRes = R.drawable.fifa15_cover, serverStatus = ServerStatus.MAINTENANCE, description = "FIFA 15 Mobile dengan mod DLavie 15", version = "v15.0", sizeMb = "22 MB", apkUrl = FIFA15_APK_URL)
        )
    }

    var showDetail by remember { mutableStateOf<GameItem?>(null) }
    var favorites by remember { mutableStateOf(ghLoadFav(context)) }
    var currentScreen by remember { mutableStateOf("library") } // library, settings, download
    var showSidebar by remember { mutableStateOf(false) }

    // v311: LazyRow scroll + snap
    val scrollState = rememberLazyListState()
    val focusedIdx by remember { derivedStateOf {
        val center = scrollState.firstVisibleItemIndex + if (scrollState.firstVisibleItemScrollOffset > 150) 1 else 0
        center.coerceIn(0, games.lastIndex)
    }}
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                if (games.isNotEmpty()) {
                    delay(300)
                    if (!scrollState.isScrollInProgress) {
                        val target = focusedIdx.coerceIn(0, games.lastIndex)
                        scrollState.animateScrollToItem(target)
                    }
                }
            }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        // ── ADAPTIVE BLURRED BACKGROUND ──
        games.getOrNull(focusedIdx)?.let { game ->
            if (game.coverImageRes != null) {
                Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = null, modifier = Modifier.fillMaxSize().blur(80.dp), contentScale = ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xD9000000), Color(0xEE000000), Color(0xD9000000)))))
        }

        if (!showTransition) {
            showDetail?.let { g -> DetailPage(g, context, { showDetail = null }, { ghLaunch(context, g.packageName); onGameClick(g.packageName) }) } ?: run {
                Column(Modifier.fillMaxSize()) {
                    // ── TOP BAR (minimal: just time + battery, right-aligned) ──
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text(time, color = White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.BatteryFull, null, tint = Gray, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("$batt%", color = Gray, fontSize = 10.sp)
                    }

                    // ── CONTENT ──
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when (currentScreen) {
                            "library" -> {
                                LazyRow(state = scrollState, contentPadding = PaddingValues(horizontal = 60.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    itemsIndexed(games) { idx, game ->
                                        val isCenter = idx == focusedIdx
                                        val isAdjacent = kotlin.math.abs(idx - focusedIdx) == 1
                                        val scaleVal = when { isCenter -> 1f; isAdjacent -> 0.82f; else -> 0.65f }
                                        val alphaVal = when { isCenter -> 1f; isAdjacent -> 0.4f; else -> 0.12f }
                                        PS5GameCard(
                                            game = game, installed = ghInstalled(context, game.packageName), isFav = game.packageName in favorites, isCenter = isCenter,
                                            scaleVal = scaleVal, alphaVal = alphaVal,
                                            onClick = { if (isCenter) showDetail = game },
                                            onPlay = { ghLaunch(context, game.packageName); onGameClick(game.packageName) },
                                            onFav = { favorites = ghToggleFav(context, game.packageName) },
                                            onShare = { ghShare(context, game) },
                                            onDelete = { ghDeleteGameData(context, game.packageName) },
                                            context = context, onInfo = { showDetail = game }
                                        )
                                    }
                                }
                            }
                            "settings" -> SettingsScreen(context, api, displayName, avatarUrl, username, role)
                            "download" -> DownloadScreen(context)
                        }
                    }

                    // ── BOTTOM BAR (6 transparent icons) ──
                    BottomBarIcons(
                        currentScreen = currentScreen,
                        onGames = { currentScreen = "library" },
                        onSettings = { currentScreen = "settings" },
                        onDownload = { currentScreen = "download" },
                        onExit = onExit
                    )
                }
            }
        }
        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PS5 GAME CARD — center: glow + Play Game + 3-dot; others: no buttons
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun PS5GameCard(
    game: GameItem, installed: Boolean, isFav: Boolean, isCenter: Boolean,
    scaleVal: Float, alphaVal: Float,
    onClick: () -> Unit, onPlay: () -> Unit, onFav: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit,
    context: Context, onInfo: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val glowAlpha = if (isCenter) 0.35f else 0f

    val (dotColor, statusLabel) = when (game.serverStatus) {
        ServerStatus.ONLINE -> Pair(GreenDot, "Online")
        ServerStatus.MAINTENANCE -> Pair(AmberDot, "Maintenance")
        ServerStatus.OFFLINE -> Pair(RedDot, "Offline")
        ServerStatus.BUSY -> Pair(AmberDot, "Busy")
    }

    Column(Modifier.width(180.dp).graphicsLayer { scaleX = scaleVal; scaleY = scaleVal; this.alpha = alphaVal }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Glow behind center card
            if (glowAlpha > 0.01f) {
                Box(Modifier.size(240.dp).background(Brush.radialGradient(colors = listOf(White.copy(alpha = glowAlpha), Color.Transparent), radius = 120f)))
            }

            // Card
            Box(Modifier.width(180.dp).height(240.dp).clip(RoundedCornerShape(16.dp)).background(Glass).clickable { onClick() }) {
                if (game.coverImageRes != null) {
                    Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) { Text(game.coverText, color = White, fontSize = 32.sp, fontWeight = FontWeight.Black) }
                }
                Box(Modifier.fillMaxSize().background(Glass))
                Box(Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))

                // Status pill (top-left)
                Row(Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xCC000000)).border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(initialValue = 0.5f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "dot_pulse")
                    Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor.copy(alpha = pulseAlpha)))
                    Text(statusLabel, color = White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }

                // 3-dot menu (top-right) — ONLY on center card
                if (isCenter) {
                    Icon(Icons.Rounded.MoreVert, "Menu", tint = White.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(18.dp).clickable { showMenu = !showMenu })
                }

                // Title (bottom)
                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    Text(game.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(game.subtitle, color = Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // ── PLAY GAME button — ONLY on center card ──
        if (isCenter) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Play Game button
                Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp)).background(if (installed) GreenDot else White).clickable { onPlay() }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(if (installed) Icons.Rounded.PlayArrow else Icons.Rounded.Download, null, tint = Bg, modifier = Modifier.size(14.dp))
                        Text(if (installed) "Play Game" else "Install", color = Bg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── DROPDOWN MENU (delete, info, favorite) ──
            if (showMenu) {
                Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
                    Column(Modifier.width(150.dp).clip(RoundedCornerShape(12.dp)).background(Bg.copy(alpha = 0.95f)).padding(4.dp)) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showMenu = false; onInfo() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Rounded.Info, null, tint = White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)); Text("Info", color = White, fontSize = 11.sp) }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onFav(); showMenu = false }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFav) RedDot else White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)); Text(if (isFav) "Remove Favorite" else "Add Favorite", color = White, fontSize = 11.sp) }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onShare(); showMenu = false }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Rounded.Share, null, tint = White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)); Text("Share", color = White, fontSize = 11.sp) }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onDelete(); showMenu = false }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Rounded.Delete, null, tint = RedDot, modifier = Modifier.size(14.dp)); Text("Delete Game Data", color = RedDot, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

// Helper: show detail page


// ═══════════════════════════════════════════════════════════════════════════
// BOTTOM BAR — 6 transparent icons (game/settings/notif/fav/friends/power)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BottomBarIcons(
    currentScreen: String,
    onGames: () -> Unit, onSettings: () -> Unit, onDownload: () -> Unit, onExit: () -> Unit
) {
    val items = listOf(
        Triple("game", Icons.Rounded.SportsEsports, { onGames() }),
        Triple("settings", Icons.Rounded.Settings, { onSettings() }),
        Triple("notif", Icons.Rounded.Notifications, { }),
        Triple("fav", Icons.Rounded.Favorite, { }),
        Triple("friends", Icons.Rounded.People, { }),
        Triple("power", Icons.Rounded.PowerSettingsNew, { onExit() })
    )
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (id, icon, action) ->
            val sel = when (id) { "game" -> currentScreen == "library"; "settings" -> currentScreen == "settings"; "download" -> currentScreen == "download"; else -> false }
            Column(Modifier.clickable { action() }.padding(horizontal = 10.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, id, tint = if (sel) White else GrayDim, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN — Permission + Storage + Download + About
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsScreen(context: Context, onInfo: () -> Unit = {}, api: CommunityApi?, name: String, avatar: String, user: String, role: String) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Permission, 1=Storage, 2=Download, 3=About
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        // Tab selector
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Permission", "Storage", "Download", "About").forEachIndexed { i, label ->
                val sel = selectedTab == i
                Text(label, color = if (sel) White else GrayDim, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.clickable { selectedTab = i }.padding(vertical = 6.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        when (selectedTab) {
            0 -> PermissionTab(context)
            1 -> StorageTab(context)
            2 -> DownloadTab(context)
            3 -> AboutTab(context)
        }
    }
}

@Composable
private fun PermissionTab(context: Context, onInfo: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Shizuku / Root Permission", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        // Check Shizuku
        val shizukuInstalled = remember { try { context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0); true } catch (_: Exception) { false } }
        SettingCard("Shizuku", if (shizukuInstalled) "Terinstall" else "Tidak terinstall", shizukuInstalled, Icons.Rounded.Shield)
        // Check Root
        val rootAvailable = remember { try { Runtime.getRuntime().exec("su -c id").waitFor() == 0 } catch (_: Exception) { false } }
        SettingCard("Root Access", if (rootAvailable) "Tersedia" else "Tidak tersedia", rootAvailable, Icons.Rounded.Security)
        Spacer(Modifier.height(16.dp))
        Text("Izin yang diperlukan:", color = Gray, fontSize = 12.sp)
        SettingCard("Storage Access", "Untuk install game data", true, Icons.Rounded.Storage)
        SettingCard("Install Unknown Apps", "Untuk install APK", true, Icons.Rounded.Apps)
        SettingCard("Overlay Permission", "Untuk floating chatbot", false, Icons.Rounded.Layers)
    }
}

@Composable
private fun StorageTab(context: Context, onInfo: () -> Unit = {}) {
    val (avail, total) = remember { ghStorageInfo() }
    val usedPct = if (total > 0) ((total - avail).toFloat() / total) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Device Storage", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        // Storage bar
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(GrayDim.copy(alpha = 0.3f))) {
            Box(Modifier.fillMaxWidth(usedPct).fillMaxHeight().background(Brush.linearGradient(listOf(Gray, White))))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Used: ${ghFormatBytes(total - avail)}", color = Gray, fontSize = 11.sp)
            Text("Free: ${ghFormatBytes(avail)}", color = GreenDot, fontSize = 11.sp)
            Text("Total: ${ghFormatBytes(total)}", color = Gray, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text("Installed Games", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        val games = listOf(
            Triple("FIFA 16 Mobile", GAME_PKG_16, "DLavie 26 Mod"),
            Triple("FIFA 15 Mobile", GAME_PKG_15, "DLavie 15 Mod")
        )
        games.forEach { (title, pkg, subtitle) ->
            val installed = ghInstalled(context, pkg)
            val gameSize = remember(pkg) { ghGameStorage(pkg) }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Glass).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(GlassHi), contentAlignment = Alignment.Center) { Text(title.take(2), color = White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("$subtitle • ${ghFormatBytes(gameSize)}", color = Gray, fontSize = 10.sp)
                }
                Text(if (installed) "Installed" else "Not installed", color = if (installed) GreenDot else GrayDim, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DownloadTab(context: Context, onInfo: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DLC & Patches", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Download mod, patch, dan data untuk game DLavie", color = Gray, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))

        // FIFA 16 category
        Text("FIFA 16 Mobile", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        DownloadItem("DLavie 26 Base Data", "2.7GB", "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/dlavie26-data.zip", context)
        DownloadItem("Roster Update 2025/2026", "45MB", "${DLAVIE_DATA_BASE}/roster-2026.zip", context)
        DownloadItem("Gameplay Mod v3", "12MB", "${DLAVIE_DATA_BASE}/gameplay-v3.zip", context)

        Spacer(Modifier.height(8.dp))
        // FIFA 15 category
        Text("FIFA 15 Mobile", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        DownloadItem("DLavie 15 Base Data", "1.8GB", "${DLAVIE_DATA_BASE}/dlavie15-data.zip", context)
        DownloadItem("Roster Update 2014/2015", "38MB", "${DLAVIE_DATA_BASE}/roster-2015.zip", context)
    }
}

@Composable
private fun DownloadItem(title: String, size: String, url: String, context: Context, onInfo: () -> Unit = {}) {
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Glass).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Download, null, tint = White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(size, color = Gray, fontSize = 10.sp)
            if (downloading) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = White, trackColor = GrayDim)
            }
        }
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (downloading) GrayDim else White).clickable {
            if (!downloading) {
                downloading = true; progress = 0f
                // Download + install
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val outDir = File(context.getExternalFilesDir(null), "dlc-downloads").also { it.mkdirs() }
                        val outFile = File(outDir, title.replace(" ", "_") + ".zip")
                        val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply { connectTimeout = 30000; readTimeout = 120000; connect() }
                        if (conn.responseCode in 300..399) { val loc = conn.getHeaderField("Location"); conn.disconnect(); val conn2 = (java.net.URL(loc).openConnection() as java.net.HttpURLConnection).apply { connect() }; conn2.inputStream.use { it.copyTo(outFile.outputStream()) }; conn2.disconnect() }
                        else { conn.inputStream.use { it.copyTo(outFile.outputStream()) }; conn.disconnect() }
                        progress = 1f
                        downloading = false
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Download selesai: ${outFile.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        downloading = false
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Download gagal: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }, contentAlignment = Alignment.Center) {
            Icon(if (downloading) Icons.Rounded.Close else Icons.Rounded.Download, null, tint = Bg, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun AboutTab(context: Context, onInfo: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("About DLavie GameHub", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        AboutLink("FAQ", "Pertanyaan yang sering diajukan", "https://drmacze.github.io/dlavie-web/#/faq", context)
        AboutLink("Terms", "Syarat dan ketentuan", "https://drmacze.github.io/dlavie-web/#/terms", context)
        AboutLink("Privacy", "Kebijakan privasi", "https://drmacze.github.io/dlavie-web/#/privacy", context)
        AboutLink("About", "Tentang DLavie", "https://drmacze.github.io/dlavie-web/#/about", context)
        Spacer(Modifier.height(16.dp))
        Text("DLavie GameHub v311", color = GrayDim, fontSize = 10.sp)
        Text("Cloud Gaming Platform", color = GrayDim, fontSize = 10.sp)
    }
}

@Composable
private fun AboutLink(title: String, subtitle: String, url: String, context: Context, onInfo: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Glass).clickable {
        try { val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(i) } catch (_: Exception) {}
    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium); Text(subtitle, color = Gray, fontSize = 10.sp) }
        Icon(Icons.Rounded.ChevronRight, null, tint = GrayDim, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingCard(title: String, status: String, ok: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Glass).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (ok) GreenDot else GrayDim, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { Text(title, color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text(status, color = if (ok) GreenDot else GrayDim, fontSize = 10.sp) }
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (ok) GreenDot else GrayDim))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DETAIL PAGE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DetailPage(game: GameItem, context: Context, onInfo: () -> Unit = {}, onBack: () -> Unit, onPlay: () -> Unit) {
    val installed = ghInstalled(context, game.packageName)
    Box(Modifier.fillMaxSize().background(Bg)) {
        if (game.coverImageRes != null) { Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = null, modifier = Modifier.fillMaxSize().blur(60.dp), contentScale = ContentScale.Crop); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xCC000000), Color(0xEE000000))))) }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.ArrowBack, "Back", tint = White, modifier = Modifier.size(24.dp).clickable { onBack() }); Spacer(Modifier.width(16.dp)); Text(game.title, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            Box(Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(Glass)) { if (game.coverImageRes != null) Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop); Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Bg)))); Text(game.title, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) }
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Description", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(game.description, color = Gray, fontSize = 13.sp); Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { Column { Text("Version", color = GrayDim, fontSize = 10.sp); Text(game.version, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }; Column { Text("Size", color = GrayDim, fontSize = 10.sp); Text(game.sizeMb, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }; Column { Text("Status", color = GrayDim, fontSize = 10.sp); Text(when(game.serverStatus) { ServerStatus.ONLINE -> "Online"; ServerStatus.MAINTENANCE -> "Maintenance"; else -> "Offline" }, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium) } }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(if (installed) GreenDot else White).clickable { onPlay() }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(if (installed) Icons.Rounded.PlayArrow else Icons.Rounded.Download, null, tint = Bg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(if (installed) "Play Now" else "Install", color = Bg, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
