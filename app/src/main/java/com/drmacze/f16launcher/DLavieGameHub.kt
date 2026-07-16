package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
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
// DLAVIE GAMEHUB v302 — Sidebar + Swipe Card + Glow
// ═══════════════════════════════════════════════════════════════════════════
// No bottom nav. Hamburger (3 lines) in top bar opens sidebar drawer.
// Swipe cards: focused card scales up + white glow behind.
// Status label: clean pill, minimal.
// ═══════════════════════════════════════════════════════════════════════════

private val Bg = Color(0xFF050505)
private val GlassSurface = Color(0x12FFFFFF)
private val GlassSurfaceHi = Color(0x22FFFFFF)
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
private const val PREFS = "gh_fav_v302"
private fun ghLoadFav(c: Context): Set<String> = try { c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("f", emptySet()) ?: emptySet() } catch (_: Exception) { emptySet() }
private fun ghToggleFav(c: Context, p: String): Set<String> { val s = ghLoadFav(c).toMutableSet(); if (p in s) s.remove(p) else s.add(p); c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("f", s).apply(); return s }

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
// MAIN — hamburger sidebar + swipe cards + glow
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
    var selectedTab by remember { mutableStateOf(1) }
    var focusedIdx by remember { mutableStateOf(0) }
    var showSidebar by remember { mutableStateOf(false) }

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
                    // ── TOP BAR (hamburger + profile + tabs + status) ──
                    TopBarWithHamburger(time, batt, displayName, avatarUrl, username, role, selectedTab, { selectedTab = it }, onExit, { showSidebar = true })

                    // ── CONTENT (swipe cards) ──
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (selectedTab == 1) {
                            val listState = rememberLazyListState()
                            LaunchedEffect(listState) { snapshotFlow { listState.firstVisibleItemIndex }.collect { idx -> if (idx < games.size) focusedIdx = idx } }

                            LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 60.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                                itemsIndexed(games) { idx, g ->
                                    SwipeGameCard(
                                        game = g, installed = ghInstalled(context, g.packageName), isFav = g.packageName in favorites, isFocused = idx == focusedIdx,
                                        onClick = { showDetail = g }, onPlay = { ghLaunch(context, g.packageName); onGameClick(g.packageName) },
                                        onFav = { favorites = ghToggleFav(context, g.packageName) }, onShare = { ghShare(context, g) }
                                    )
                                }
                            }

                            // Page indicator dots
                            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(games.size) { i -> Box(Modifier.size(if (i == focusedIdx) 8.dp else 5.dp).clip(CircleShape).background(if (i == focusedIdx) White else GrayDim.copy(alpha = 0.4f))) }
                            }
                        } else {
                            Text(when(selectedTab) { 0 -> "Store"; 2 -> "Videos"; 3 -> "Settings"; else -> "" }, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── SIDEBAR DRAWER ──
        if (showSidebar) {
            SidebarDrawer(displayName, avatarUrl, username, role, selectedTab, { selectedTab = it; showSidebar = false }, { showSidebar = false }, onExit)
        }

        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TOP BAR — hamburger (3 lines) + profile + tabs
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TopBarWithHamburger(time: String, batt: Int, name: String, avatar: String, user: String, role: String, tab: Int, onTab: (Int) -> Unit, onExit: () -> Unit, onMenu: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Hamburger icon (3 lines)
            Icon(Icons.Rounded.Menu, "Menu", tint = White, modifier = Modifier.size(24.dp).clickable { onMenu() })
            Spacer(Modifier.width(12.dp))
            // Avatar
            Box(Modifier.size(32.dp).clip(CircleShape).background(GlassSurface).border(1.dp, Divider, CircleShape), contentAlignment = Alignment.Center) {
                if (avatar.isNotEmpty()) AsyncImage(model = avatar, contentDescription = "Avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                else Text(name.take(1).uppercase(), color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                    Spacer(Modifier.width(5.dp))
                    val badge = when (role.lowercase()) { "admin" -> "ADMIN"; "developer" -> "DEV"; "owner" -> "OWNER"; "moderator" -> "MOD"; else -> "" }
                    if (badge.isNotEmpty()) Text(badge, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Text("@$user", color = Gray, fontSize = 10.sp)
            }
            // Status
            Text(time, color = White.copy(alpha = 0.6f), fontSize = 11.sp)
            Spacer(Modifier.width(5.dp))
            Icon(Icons.Rounded.BatteryFull, null, tint = Gray, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(2.dp))
            Text("$batt%", color = Gray, fontSize = 10.sp)
        }
        // Tabs (minimal text)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            listOf("Store", "Library", "Videos", "Settings").forEachIndexed { i, label ->
                val sel = tab == i
                Text(label, color = if (sel) White else GrayDim, fontSize = 12.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.clickable { onTab(i) }.padding(vertical = 4.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SWIPE GAME CARD — focused card scales up + white glow behind
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SwipeGameCard(
    game: GameItem, installed: Boolean, isFav: Boolean, isFocused: Boolean,
    onClick: () -> Unit, onPlay: () -> Unit, onFav: () -> Unit, onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Focus animation: scale + glow
    val scale by animateFloatAsState(if (isFocused) 1f else 0.78f, spring(dampingRatio = 0.5f, stiffness = 200f), label = "scale")
    val alpha by animateFloatAsState(if (isFocused) 1f else 0.35f, tween(400), label = "alpha")
    val glowAlpha by animateFloatAsState(if (isFocused) 0.4f else 0f, tween(500), label = "glow")

    // Status config
    val (dotColor, statusLabel) = when (game.serverStatus) {
        ServerStatus.ONLINE -> Pair(GreenDot, "Online")
        ServerStatus.MAINTENANCE -> Pair(AmberDot, "Maintenance")
        ServerStatus.OFFLINE -> Pair(RedDot, "Offline")
        ServerStatus.BUSY -> Pair(AmberDot, "Busy")
    }

    Column(Modifier.width(180.dp).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // ── WHITE GLOW BEHIND CARD (only when focused) ──
            if (isFocused) {
                Box(Modifier.width(190.dp).height(250.dp).blur(30.dp).background(White.copy(alpha = glowAlpha), RoundedCornerShape(20.dp)))
            }

            // ── CARD (180x240dp, glass, no border, 16dp radius) ──
            Box(
                Modifier.width(180.dp).height(240.dp).clip(RoundedCornerShape(16.dp)).background(GlassSurface).clickable { onClick() }
            ) {
                // Cover image
                if (game.coverImageRes != null) {
                    Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) { Text(game.coverText, color = White, fontSize = 32.sp, fontWeight = FontWeight.Black) }
                }

                // Glass overlay
                Box(Modifier.fillMaxSize().background(GlassSurface))

                // Bottom gradient
                Box(Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))

                // ── STATUS PILL (top-left, clean creative) ──
                Row(
                    Modifier.align(Alignment.TopStart).padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC000000))
                        .border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pulsing dot
                    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
                        initialValue = 0.5f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
                        label = "dot_pulse"
                    )
                    Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor.copy(alpha = pulseAlpha)))
                    Text(statusLabel, color = White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }

                // Heart icon (top-right)
                Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Fav", tint = if (isFav) RedDot else White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(16.dp).clickable { onFav() })

                // Title + subtitle (bottom)
                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    Text(game.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(game.subtitle, color = Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // ── VIEW DETAIL + 3-DOT (below card) ──
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(10.dp)).background(GlassSurface).clickable { onClick() }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = Gray, modifier = Modifier.size(12.dp))
                    Text("View Detail", color = White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(GlassSurface).clickable { showMenu = !showMenu }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MoreVert, "Menu", tint = White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
        }

        // ── DROPDOWN ──
        if (showMenu) {
            Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
                Column(Modifier.width(150.dp).clip(RoundedCornerShape(12.dp)).background(Bg.copy(alpha = 0.95f)).padding(4.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onShare(); showMenu = false }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Rounded.Share, null, tint = White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)); Text("Share", color = White, fontSize = 11.sp) }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onFav(); showMenu = false }.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFav) RedDot else White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)); Text(if (isFav) "Remove Favorite" else "Add Favorite", color = White, fontSize = 11.sp) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SIDEBAR DRAWER — slide from left
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SidebarDrawer(name: String, avatar: String, user: String, role: String, tab: Int, onTab: (Int) -> Unit, onDismiss: () -> Unit, onExit: () -> Unit) {
    val drawerOffset by animateFloatAsState(if (true) 0f else -1f, tween(300), label = "drawer")

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { onDismiss() }) {
        Column(
            Modifier.fillMaxHeight().width(280.dp).background(Bg.copy(alpha = 0.98f)).clickable { },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Profile header
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(GlassSurface).border(1.dp, Divider, CircleShape), contentAlignment = Alignment.Center) {
                    if (avatar.isNotEmpty()) AsyncImage(model = avatar, contentDescription = "Avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    else Text(name.take(1).uppercase(), color = White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(12.dp))
                Text(name, color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("@$user", color = Gray, fontSize = 12.sp)
                val badge = when (role.lowercase()) { "admin" -> "ADMIN"; "developer" -> "DEV"; "owner" -> "OWNER"; "moderator" -> "MOD"; else -> "" }
                if (badge.isNotEmpty()) { Spacer(Modifier.height(4.dp)); Text(badge, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))

            // Nav items
            val items = listOf(
                Triple("Home", Icons.Rounded.Home, 1),
                Triple("Library", Icons.Rounded.SportsEsports, 1),
                Triple("Store", Icons.Rounded.Store, 0),
                Triple("Videos", Icons.Rounded.VideoLibrary, 2),
                Triple("Settings", Icons.Rounded.Settings, 3)
            )
            items.forEach { (label, icon, t) ->
                val sel = tab == t
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (sel) GlassSurfaceHi else Color.Transparent).clickable { onTab(t) }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, label, tint = if (sel) White else Gray, modifier = Modifier.size(20.dp))
                    Text(label, color = if (sel) White else Gray, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            Row(Modifier.fillMaxWidth().clickable { onExit() }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Close, "Exit", tint = Gray, modifier = Modifier.size(20.dp))
                Text("Exit GameHub", color = Gray, fontSize = 14.sp)
            }
            Text("DLavie GameHub v302", color = GrayDim, fontSize = 10.sp, modifier = Modifier.padding(20.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DETAIL PAGE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DetailPage(game: GameItem, context: Context, onBack: () -> Unit, onPlay: () -> Unit) {
    val installed = ghInstalled(context, game.packageName)
    Box(Modifier.fillMaxSize().background(Bg)) {
        if (game.coverImageRes != null) {
            Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = null, modifier = Modifier.fillMaxSize().blur(60.dp), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xCC000000), Color(0xEE000000)))))
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.ArrowBack, "Back", tint = White, modifier = Modifier.size(24.dp).clickable { onBack() }); Spacer(Modifier.width(16.dp)); Text(game.title, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            Box(Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(GlassSurface)) {
                if (game.coverImageRes != null) Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Bg))))
                Text(game.title, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
            }
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Description", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(game.description, color = Gray, fontSize = 13.sp); Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column { Text("Version", color = GrayDim, fontSize = 10.sp); Text(game.version, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    Column { Text("Size", color = GrayDim, fontSize = 10.sp); Text(game.sizeMb, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    Column { Text("Status", color = GrayDim, fontSize = 10.sp); Text(when(game.serverStatus) { ServerStatus.ONLINE -> "Online"; ServerStatus.MAINTENANCE -> "Maintenance"; else -> "Offline" }, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(if (installed) GreenDot else White).clickable { onPlay() }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (installed) Icons.Rounded.PlayArrow else Icons.Rounded.Download, null, tint = Bg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(if (installed) "Play Now" else "Install", color = Bg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
