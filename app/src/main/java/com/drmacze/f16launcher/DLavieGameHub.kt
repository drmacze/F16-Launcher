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
// DLAVIE GAMEHUB v300 — Modern Glassmorphism (clean, minimal, cinematic)
// ═══════════════════════════════════════════════════════════════════════════
// No colored borders. Blurred game cover bg. Glass cards with subtle shadow.
// Minimal UI. Clean spacing. Modern aesthetic.
// ═══════════════════════════════════════════════════════════════════════════

// ── Minimal palette ──
private val Bg = Color(0xFF050505)
private val GlassSurface = Color(0x15FFFFFF)     // 8% white — very subtle glass
private val GlassSurfaceHi = Color(0x25FFFFFF)   // 15% white — focused glass
private val Divider = Color(0x10FFFFFF)           // 6% white — barely visible
private val White = Color(0xFFFFFFFF)
private val Gray = Color(0xFF909090)
private val GrayDim = Color(0xFF555555)
private val GreenDot = Color(0xFF4ADE80)
private val AmberDot = Color(0xFFFBBF24)
private val RedDot = Color(0xFFEF4444)
private val Gold = Color(0xFFFFD700)

// ── Helpers ──
private fun ghBattery(c: Context): Int = try {
    (c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
} catch (_: Exception) { 100 }

private fun ghTime(c: Context): String = try {
    val is24 = DateFormat.is24HourFormat(c)
    SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date())
} catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

private fun ghInstalled(c: Context, pkg: String): Boolean = try { c.packageManager.getPackageInfo(pkg, 0); true } catch (_: Throwable) { false }
private fun ghLaunch(c: Context, pkg: String) = try { c.packageManager.getLaunchIntentForPackage(pkg)?.let { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(it); true } ?: false } catch (_: Throwable) { false }

private fun ghShare(ctx: Context, g: GameItem) {
    val url = "https://drmacze.github.io/dlavie-web/#/game?pkg=${g.packageName}"
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "DLavie - ${g.title}"); putExtra(Intent.EXTRA_TEXT, "Main ${g.title} di DLavie! $url"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try { ctx.startActivity(Intent.createChooser(i, "Bagikan").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
}

private const val PREFS = "gh_fav_v300"
private fun ghLoadFav(c: Context): Set<String> = try { c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("f", emptySet()) ?: emptySet() } catch (_: Exception) { emptySet() }
private fun ghToggleFav(c: Context, p: String): Set<String> { val s = ghLoadFav(c).toMutableSet(); if (p in s) s.remove(p) else s.add(p); c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("f", s).apply(); return s }

// ═══════════════════════════════════════════════════════════════════════════
// TRANSITION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    var typed by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val full = "DLAVIE"
    val msgs = listOf("Memuat aset game...", "Menata antarmuka...", "Memindai data...", "Menghubungkan ke server...", "Menyiapkan GameHub...")
    LaunchedEffect(visible) {
        if (visible) {
            phase = 0; delay(800); phase = 1; delay(800); phase = 2
            for (i in full.indices) { typed = full.substring(0, i + 1); delay(120) }
            delay(400); phase = 3
            for (m in msgs) { msg = m; delay(700) }
            msg = ""; delay(300); phase = 4; delay(600); onComplete(); phase = 5
        }
    }
    if (visible && phase < 5) {
        Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
            val la by animateFloatAsState(when (phase) { 0 -> 0f; 1 -> 1f; 2 -> 1f; 3 -> 1f; 4 -> 0f; else -> 0f }, tween(800, easing = FastOutSlowInEasing), label = "la")
            val ls by animateFloatAsState(when (phase) { 0 -> 0.7f; 1 -> 1f; 4 -> 1.15f; else -> 1f }, tween(800, easing = FastOutSlowInEasing), label = "ls")
            val ta by animateFloatAsState(when (phase) { 2 -> 1f; 4 -> 0f; else -> 0f }, tween(400), label = "ta")
            val fo by animateFloatAsState(if (phase == 4) 0f else 1f, tween(600), label = "fo")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { this.alpha = fo }) {
                Box(Modifier.size(72.dp).graphicsLayer { scaleX = ls; scaleY = ls; this.alpha = la }
                    .clip(androidx.compose.foundation.shape.GenericShape { _, _ -> val r = 70f; moveTo(0f, -r); lineTo(r * 0.866f, -r * 0.5f); lineTo(r * 0.866f, r * 0.5f); lineTo(0f, r); lineTo(-r * 0.866f, r * 0.5f); lineTo(-r * 0.866f, -r * 0.5f); close() }).background(White), contentAlignment = Alignment.Center) { Text("DL", color = Bg, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(20.dp))
                Text(typed, color = White, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.graphicsLayer { this.alpha = ta })
                if (msg.isNotEmpty()) { Spacer(Modifier.height(16.dp)); Text(msg, color = Gray, fontSize = 12.sp, modifier = Modifier.graphicsLayer { this.alpha = ta }) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN — Blurred bg + glass cards + minimal UI
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

    Box(Modifier.fillMaxSize().background(Bg)) {
        // ── ADAPTIVE BLURRED BACKGROUND (from first game cover) ──
        games.getOrNull(focusedIdx)?.let { game ->
            if (game.coverImageRes != null) {
                Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = null, modifier = Modifier.fillMaxSize().blur(80.dp), contentScale = ContentScale.Crop)
            }
            // Dark overlay for depth
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xD9000000), Color(0xEE000000), Color(0xD9000000)))))
        }

        if (!showTransition) {
            showDetail?.let { g -> DetailPage(g, context, { showDetail = null }, { ghLaunch(context, g.packageName); onGameClick(g.packageName) }) } ?: run {
                Column(Modifier.fillMaxSize()) {
                    // ── TOP BAR (fully transparent, minimal) ──
                    TopBar(time, batt, displayName, avatarUrl, username, role, selectedTab, { selectedTab = it }, onExit)

                    // ── CONTENT (centered, one screen) ──
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (selectedTab == 1) {
                            LazyRow(contentPadding = PaddingValues(horizontal = 40.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                itemsIndexed(games) { idx, g ->
                                    GlassGameCard(
                                        game = g,
                                        installed = ghInstalled(context, g.packageName),
                                        isFav = g.packageName in favorites,
                                        isFocused = idx == focusedIdx,
                                        onClick = { showDetail = g },
                                        onPlay = { ghLaunch(context, g.packageName); onGameClick(g.packageName) },
                                        onFav = { favorites = ghToggleFav(context, g.packageName) },
                                        onShare = { ghShare(context, g) },
                                        onFocus = { focusedIdx = idx }
                                    )
                                }
                            }
                        } else {
                            Text(when(selectedTab) { 0 -> "Store"; 2 -> "Videos"; 3 -> "Settings"; else -> "" }, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // ── BOTTOM NAV (fully transparent, minimal) ──
                    BottomNav(selectedTab, { selectedTab = it }, onExit)
                }
            }
        }
        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TOP BAR — transparent, minimal profile + tabs
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TopBar(time: String, batt: Int, name: String, avatar: String, user: String, role: String, tab: Int, onTab: (Int) -> Unit, onExit: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        // Row 1: Profile + status
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar (glass circle, no colored bg)
            Box(Modifier.size(34.dp).clip(CircleShape).background(GlassSurface).border(1.dp, Divider, CircleShape), contentAlignment = Alignment.Center) {
                if (avatar.isNotEmpty()) AsyncImage(model = avatar, contentDescription = "Avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                else Text(name.take(1).uppercase(), color = White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 130.dp))
                    Spacer(Modifier.width(6.dp))
                    val badge = when (role.lowercase()) { "admin" -> "ADMIN"; "developer" -> "DEV"; "owner" -> "OWNER"; "moderator" -> "MOD"; else -> "" }
                    if (badge.isNotEmpty()) { Text(badge, color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                }
                Text("@$user", color = Gray, fontSize = 11.sp)
            }
            // Status (right side, minimal)
            Text(time, color = White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.BatteryFull, null, tint = Gray, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(2.dp))
            Text("$batt%", color = Gray, fontSize = 11.sp)
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Rounded.ArrowBack, "Exit", tint = White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp).clickable { onExit() })
        }
        // Row 2: Tabs (minimal, no underline — just color change)
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("Store", "Library", "Videos", "Settings").forEachIndexed { i, label ->
                val sel = tab == i
                Text(label, color = if (sel) White else GrayDim, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.clickable { onTab(i) }.padding(vertical = 6.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS GAME CARD — no border, glass surface, subtle shadow, minimal content
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GlassGameCard(
    game: GameItem, installed: Boolean, isFav: Boolean, isFocused: Boolean,
    onClick: () -> Unit, onPlay: () -> Unit, onFav: () -> Unit, onShare: () -> Unit,
    onFocus: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dotColor = when (game.serverStatus) { ServerStatus.ONLINE -> GreenDot; ServerStatus.MAINTENANCE -> AmberDot; ServerStatus.OFFLINE -> RedDot; ServerStatus.BUSY -> AmberDot }

    // Focus animation — smooth scale
    val scale by animateFloatAsState(if (isFocused) 1f else 0.88f, spring(dampingRatio = 0.6f, stiffness = 200f), label = "focus_scale")
    val alpha by animateFloatAsState(if (isFocused) 1f else 0.5f, tween(400), label = "focus_alpha")

    Column(Modifier.width(170.dp).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
        // ── CARD (170x240dp, glass surface, NO border, rounded) ──
        Box(
            Modifier.width(170.dp).height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(GlassSurface)
                .clickable { onFocus(); onClick() }
        ) {
            // Cover image (fills card, slightly faded)
            if (game.coverImageRes != null) {
                Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) { Text(game.coverText, color = White, fontSize = 32.sp, fontWeight = FontWeight.Black) }
            }

            // Glass overlay (subtle white tint for glassmorphism)
            Box(Modifier.fillMaxSize().background(GlassSurface))

            // Bottom gradient for text
            Box(Modifier.fillMaxWidth().height(90.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))

            // Status dot (top-left, minimal — just a dot, no text badge)
            Box(Modifier.align(Alignment.TopStart).padding(12.dp).size(8.dp).clip(CircleShape).background(dotColor))

            // Heart icon (top-right, subtle)
            Icon(
                if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Fav", tint = if (isFav) RedDot else White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(18.dp).clickable { onFav() }
            )

            // Title + subtitle (bottom, minimal)
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(game.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(game.subtitle, color = Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // ── VIEW DETAIL + 3-DOT (below card, glass, minimal) ──
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // View Detail
            Box(
                Modifier.weight(1f).height(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(GlassSurface).clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = Gray, modifier = Modifier.size(13.dp))
                    Text("View Detail", color = White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            // 3-dot
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(GlassSurface).clickable { showMenu = !showMenu },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.MoreVert, "Menu", tint = White.copy(alpha = 0.8f), modifier = Modifier.size(15.dp)) }
        }

        // ── DROPDOWN ──
        if (showMenu) {
            Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
                Column(Modifier.width(160.dp).clip(RoundedCornerShape(12.dp)).background(Bg.copy(alpha = 0.95f)).padding(4.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onShare(); showMenu = false }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Rounded.Share, null, tint = White.copy(alpha = 0.7f), modifier = Modifier.size(15.dp)); Text("Share", color = White, fontSize = 12.sp) }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onFav(); showMenu = false }.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFav) RedDot else White.copy(alpha = 0.7f), modifier = Modifier.size(15.dp)); Text(if (isFav) "Remove Favorite" else "Add Favorite", color = White, fontSize = 12.sp) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BOTTOM NAV — transparent, minimal, no background
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun BottomNav(tab: Int, onTab: (Int) -> Unit, onExit: () -> Unit) {
    val items = listOf(Triple("Home", Icons.Rounded.Home, 1), Triple("Library", Icons.Rounded.SportsEsports, 1), Triple("Store", Icons.Rounded.Store, 0), Triple("Videos", Icons.Rounded.VideoLibrary, 2), Triple("Exit", Icons.Rounded.Close, -1))
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (label, icon, t) ->
            val sel = tab == t && t != -1
            Column(Modifier.clickable { if (t == -1) onExit() else onTab(t) }.padding(horizontal = 12.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, label, tint = if (sel) White else GrayDim, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, color = if (sel) White else GrayDim, fontSize = 9.sp, fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal)
            }
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
        // Blurred bg
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
