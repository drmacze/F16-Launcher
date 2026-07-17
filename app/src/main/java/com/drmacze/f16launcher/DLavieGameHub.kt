package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

// ════════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB v309 — PS5-Style Complete Redesign
// PS5 home: full-screen cover art bg, horizontal card swipe, glow on focused
// card, Play Game + 3-dot (Delete/Info/Fav) only on active card.
// Bottom nav: 6 icon glass bar (Games|Settings|Notif|Fav|Friends|Exit).
// Settings: Permissions (Shizuku/root), Storage, Download (DLC Supabase), About.
// Sound: AudioTrack console tones. Shiny shimmer text effects.
// ════════════════════════════════════════════════════════════════════════════

// ── Design tokens ─────────────────────────────────────────────────────────
private val Bg         = Color(0xFF060608)
private val BgCard     = Color(0x16FFFFFF)
private val BgGlass    = Color(0x22FFFFFF)
private val White      = Color(0xFFFFFFFF)
private val Gray       = Color(0xFF9090A0)
private val GrayDim    = Color(0xFF484858)
private val Accent     = Color(0xFF00E5FF)
private val GreenLive  = Color(0xFF4ADE80)
// AmberWarn dipakai dari TapTapDesignSystem (public val AmberWarn)
private val RedAlert   = Color(0xFFEF4444)
private val Gold       = Color(0xFFFFD700)
private val DivCol     = Color(0x14FFFFFF)

// ── Shiny shimmer brush (animated gradient on text) ───────────────────────
@OptIn(ExperimentalTextApi::class)
@Composable
private fun shimmerBrush(): Brush {
    val inf = rememberInfiniteTransition(label = "shimmer")
    val off by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "sh")
    return Brush.linearGradient(
        colors = listOf(White.copy(0.6f), White, Accent.copy(0.75f), White, White.copy(0.6f)),
        start  = Offset((off * 900f) - 450f, 0f),
        end    = Offset((off * 900f), 30f)
    )
}

// ── Console sound effects (AudioTrack — no audio files needed) ─────────────
private fun playTone(freq: Double = 880.0, ms: Int = 55, vol: Float = 0.18f) {
    Thread {
        try {
            val sr = 44100; val n = sr * ms / 1000
            val buf = ShortArray(n)
            for (i in 0 until n) {
                val t = i.toDouble() / sr
                val env = when {
                    i < n / 5     -> i.toDouble() / (n / 5)
                    i > n * 4 / 5 -> (n - i).toDouble() / (n / 5)
                    else           -> 1.0
                }
                buf[i] = (32767 * sin(2 * PI * freq * t) * env * vol).toInt().toShort()
            }
            val track = AudioTrack(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                buf.size * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            track.write(buf, 0, buf.size)
            track.play()
            Thread.sleep(ms + 60L)
            track.stop(); track.release()
        } catch (_: Exception) {}
    }.start()
}
private fun sndNav()    = playTone(660.0, 45, 0.12f)
private fun sndSelect() = playTone(880.0, 65, 0.20f)
private fun sndBack()   = playTone(440.0, 50, 0.14f)
private fun sndMenu()   = playTone(1100.0, 38, 0.13f)
private fun sndDelete() = playTone(220.0, 120, 0.22f)
private fun sndDl()     = playTone(1320.0, 75, 0.17f)
private fun sndBoot()   { Thread { listOf(440.0 to 65, 660.0 to 65, 880.0 to 85).forEach { (f,t) -> playTone(f, t, 0.15f); Thread.sleep(t + 22L) } }.start() }

// ── System helpers ─────────────────────────────────────────────────────────
private fun ghBattery(c: Context) = try {
    (c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
} catch (_: Exception) { 100 }

private fun ghTime(c: Context) = try {
    val is24 = DateFormat.is24HourFormat(c)
    SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date())
} catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

private fun ghInstalled(c: Context, pkg: String) =
    try { c.packageManager.getPackageInfo(pkg, 0); true } catch (_: Throwable) { false }

private fun ghLaunch(c: Context, pkg: String) = try {
    c.packageManager.getLaunchIntentForPackage(pkg)
        ?.also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(it) } != null
} catch (_: Throwable) { false }

private const val GH_PREFS = "gh_fav_v309"
private fun ghLoadFav(c: Context): Set<String> = try {
    c.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE).getStringSet("f", emptySet()) ?: emptySet()
} catch (_: Exception) { emptySet() }
private fun ghToggleFav(c: Context, pkg: String): Set<String> {
    val s = ghLoadFav(c).toMutableSet()
    if (pkg in s) s.remove(pkg) else s.add(pkg)
    c.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE).edit().putStringSet("f", s).apply()
    return s
}

// ── Game data deletion (Shizuku/su shell) ─────────────────────────────────
private fun ghDeleteGameData(pkg: String): Boolean = try {
    val dirs = listOf("/sdcard/Android/data/$pkg", "/sdcard/Android/obb/$pkg")
    var ok = true
    dirs.forEach { dir ->
        val p = ProcessBuilder("su", "-c", "rm -rf $dir").start()
        p.waitFor()
        if (p.exitValue() != 0) {
            // Fallback: File API (works if MANAGE_EXTERNAL_STORAGE granted)
            File(dir).takeIf { it.exists() }?.deleteRecursively()
        }
    }
    ok
} catch (_: Exception) { false }

// ── Storage info ───────────────────────────────────────────────────────────
data class GHStorageInfo(val totalGb: Float, val usedGb: Float, val freeGb: Float)
private fun ghStorageInfo(): GHStorageInfo = try {
    val s = StatFs("/sdcard")
    val total = s.totalBytes / 1_073_741_824f
    val free  = s.availableBytes / 1_073_741_824f
    GHStorageInfo(total, total - free, free)
} catch (_: Exception) { GHStorageInfo(0f, 0f, 0f) }

// ── DLC data model + Supabase fetch ────────────────────────────────────────
/**
 * Supabase table: dlc_catalog
 *   id           uuid pk default uuid_generate_v4()
 *   game_pkg     text   -- e.g. "com.ea.gp.fifaworld"
 *   game_title   text   -- e.g. "FIFA 16"
 *   title        text   -- nama DLC
 *   description  text
 *   file_url     text   -- direct download URL
 *   file_size_mb float
 *   category     text   -- "OBB" | "DATA" | "MOD" | "PATCH"
 *   version      text
 *   is_published boolean default true
 *   created_at   timestamp default now()
 */
data class DlcItem(
    val id: String, val gamePkg: String, val gameTitle: String,
    val title: String, val description: String, val fileUrl: String,
    val fileSizeMb: Float, val category: String, val version: String
)

private suspend fun ghFetchDlc(): List<DlcItem> = withContext(Dispatchers.IO) {
    try {
        val url = URL("${BuildConfig.SUPABASE_URL}/rest/v1/dlc_catalog?select=*&is_published=eq.true&order=created_at.desc")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            connectTimeout = 8000; readTimeout = 12000
        }
        if (conn.responseCode == 200) parseDlcJson(conn.inputStream.bufferedReader().readText())
        else emptyList()
    } catch (_: Exception) { emptyList() }
}

private fun parseDlcJson(json: String): List<DlcItem> = try {
    val items = mutableListOf<DlcItem>()
    json.trim('[', ']').split("},{").forEach { raw ->
        fun f(key: String) = Regex(""""$key"\s*:\s*"([^"]*)"""").find(raw)?.groupValues?.get(1) ?: ""
        fun n(key: String) = Regex(""""$key"\s*:\s*([\d.]+)""").find(raw)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        if (f("id").isNotEmpty()) items += DlcItem(f("id"), f("game_pkg"), f("game_title"),
            f("title"), f("description"), f("file_url"), n("file_size_mb"), f("category"), f("version"))
    }
    items
} catch (_: Exception) { emptyList() }

// ════════════════════════════════════════════════════════════════════════════
// GAMEHUB LOADING TRANSITION — boot screen with streaming status text
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalTextApi::class)
@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase     by remember { mutableStateOf(0) }
    var typed     by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }

    val bootMsgs = listOf(
        "Menginisialisasi sistem...",
        "Menghubungkan server DLavie...",
        "Memvalidasi sesi pengguna...",
        "Mengambil data library game...",
        "Memindai assets game...",
        "Memeriksa pembaruan DLC...",
        "Memuat antarmuka GameHub...",
        "Menyiapkan sesi permainan...",
        "Siap!"
    )

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        sndBoot()
        phase = 0; delay(500)
        phase = 1; delay(400)
        phase = 2
        "DLAVIE".forEach { typed += it; delay(105) }
        delay(250); phase = 3
        bootMsgs.forEach { msg -> statusMsg = msg; delay(630) }
        statusMsg = ""; delay(180)
        phase = 4; delay(700)
        onComplete(); phase = 5
    }

    if (visible && phase < 5) {
        val alpha by animateFloatAsState(
            when (phase) { 0 -> 0f; 4 -> 0f; else -> 1f }, tween(700, easing = FastOutSlowInEasing), label = "la")
        val logoScale by animateFloatAsState(
            when (phase) { 0 -> 0.55f; 4 -> 1.25f; else -> 1f }, spring(0.55f, 180f), label = "ls")
        val textAlpha by animateFloatAsState(
            if (phase in 2..3) 1f else 0f, tween(380), label = "ta")

        Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
            // Subtle PS5-style background grid
            Canvas(Modifier.fillMaxSize()) {
                val lc = Color(0x07FFFFFF)
                val sp = 38.dp.toPx()
                var x = 0f; while (x < size.width) { drawLine(lc, Offset(x, 0f), Offset(x, size.height), 0.5f); x += sp }
                var y = 0f; while (y < size.height) { drawLine(lc, Offset(0f, y), Offset(size.width, y), 0.5f); y += sp }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            ) {
                // DL logo tile
                Box(
                    Modifier.size(84.dp)
                        .graphicsLayer { scaleX = logoScale; scaleY = logoScale }
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Accent, Color(0xFF0070F3))))
                        .border(1.dp, White.copy(0.25f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("DL", color = White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp) }

                Spacer(Modifier.height(26.dp))

                // Shiny "DLAVIE" typed text
                val brush = shimmerBrush()
                Text(typed,
                    style = TextStyle(brush = brush, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 7.sp),
                    modifier = Modifier.graphicsLayer { this.alpha = textAlpha })
                Text("GAMEHUB", color = Accent.copy(0.55f), fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 5.5.sp,
                    modifier = Modifier.graphicsLayer { this.alpha = textAlpha })

                Spacer(Modifier.height(36.dp))

                // Progress bar + streaming status
                if (phase == 3) {
                    val msgIdx = bootMsgs.indexOf(statusMsg).coerceAtLeast(0)
                    val rawProg = (msgIdx + 1).toFloat() / bootMsgs.size
                    val animProg by animateFloatAsState(rawProg, tween(600), label = "prog")

                    Box(Modifier.width(230.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(White.copy(0.08f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(animProg).clip(RoundedCornerShape(1.dp))
                            .background(Brush.horizontalGradient(listOf(Accent, Color(0xFF0070F3)))))
                    }
                    Spacer(Modifier.height(14.dp))
                    AnimatedContent(statusMsg, transitionSpec = {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(130))
                    }, label = "smsg") { msg ->
                        Text(msg, color = Gray, fontSize = 11.sp, letterSpacing = 0.3.sp)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// MAIN ENTRY — DLavieGameHub (same public signature as before)
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(
    onExit: () -> Unit = {},
    onNav: (Page) -> Unit = {},
    onGameClick: (String) -> Unit = {},
    api: CommunityApi? = null
) {
    val context = LocalContext.current
    var showTransition by remember { mutableStateOf(true) }

    val displayName = remember { api?.displayName()?.ifEmpty { "Player" } ?: "Player" }
    val avatarUrl   = remember { api?.avatarUrl() ?: "" }
    val username    = remember { api?.username()?.ifBlank { "user" } ?: "user" }
    val role        = remember { api?.role() ?: "member" }

    // Immersive full-screen (hide system bars)
    DisposableEffect(Unit) {
        val act = context as? Activity
        act?.window?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                w.setDecorFitsSystemWindows(false)
                w.insetsController?.hide(WindowInsets.Type.systemBars())
                w.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                w.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            }
        }
        onDispose {
            act?.window?.let { w ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    w.setDecorFitsSystemWindows(true)
                    w.insetsController?.show(WindowInsets.Type.systemBars())
                } else { @Suppress("DEPRECATION") w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE }
            }
        }
    }

    var time by remember { mutableStateOf(ghTime(context)) }
    var batt by remember { mutableStateOf(ghBattery(context)) }
    LaunchedEffect(Unit) { while (true) { time = ghTime(context); batt = ghBattery(context); delay(30_000) } }

    val games = remember {
        listOf(
            GameItem(title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod",
                packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                coverGradient = listOf(Color(0xFF0A1628), Color(0xFF1A3A6B)),
                coverText = "DL", coverImageRes = R.drawable.fifa16_cover,
                serverStatus = ServerStatus.ONLINE, description = "FIFA 16 Mobile dengan mod DLavie 26. Update roster, kit, dan gameplay terbaru.",
                developer = "DLavie Company", version = "v26.0", sizeMb = "34 MB",
                category = "Olahraga", ageRating = "9+", apkUrl = FIFA16_APK_URL),
            GameItem(title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod",
                packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15", coverImageRes = R.drawable.fifa15_cover,
                serverStatus = ServerStatus.MAINTENANCE, description = "FIFA 15 Mobile mod DLavie 15 dengan pemain dan kit classic.",
                developer = "DLavie Company", version = "v15.0", sizeMb = "22 MB",
                category = "Olahraga", ageRating = "9+", apkUrl = FIFA15_APK_URL)
        )
    }

    var favorites  by remember { mutableStateOf(ghLoadFav(context)) }
    var selectedNav by remember { mutableIntStateOf(0) }
    var showDetail  by remember { mutableStateOf<GameItem?>(null) }

    // Card swipe + snap state
    val listState = rememberLazyListState()
    val focusedIdx by remember {
        derivedStateOf {
            val vis = listState.firstVisibleItemIndex
            val off = listState.firstVisibleItemScrollOffset
            (vis + if (off > 130) 1 else 0).coerceIn(0, games.lastIndex)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().filter { !it }.collect {
            delay(260)
            if (!listState.isScrollInProgress) {
                sndNav()
                listState.animateScrollToItem(focusedIdx)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        showDetail?.let { g ->
            GHDetailPage(g, context, favorites,
                onBack     = { sndBack(); showDetail = null },
                onPlay     = { sndSelect(); ghLaunch(context, g.packageName); onGameClick(g.packageName) },
                onToggleFav = { favorites = ghToggleFav(context, g.packageName) }
            )
        } ?: when (selectedNav) {
            0 -> GHHomeScreen(
                games = games, focusedIdx = focusedIdx, listState = listState,
                time = time, batt = batt, favorites = favorites, context = context,
                onNavSelect = { idx -> if (idx == 5) { sndBack(); onExit() } else { sndNav(); selectedNav = idx } },
                onGameDetail = { sndSelect(); showDetail = it },
                onGamePlay   = { g -> sndSelect(); ghLaunch(context, g.packageName); onGameClick(g.packageName) },
                onToggleFav  = { favorites = ghToggleFav(context, it) }
            )
            1 -> GHSettingsScreen(games = games, context = context, onBack = { sndBack(); selectedNav = 0 })
            2 -> GHNotificationsScreen(onBack = { sndBack(); selectedNav = 0 })
            3 -> GHFavoritesScreen(games = games, favorites = favorites, context = context,
                onBack      = { sndBack(); selectedNav = 0 },
                onGameDetail = { sndSelect(); showDetail = it },
                onToggleFav  = { favorites = ghToggleFav(context, it) })
            4 -> GHFriendsScreen(onBack = { sndBack(); selectedNav = 0 })
            else -> GHHomeScreen(
                games = games, focusedIdx = focusedIdx, listState = listState,
                time = time, batt = batt, favorites = favorites, context = context,
                onNavSelect = { idx -> if (idx == 5) { sndBack(); onExit() } else { sndNav(); selectedNav = idx } },
                onGameDetail = { sndSelect(); showDetail = it },
                onGamePlay   = { g -> sndSelect(); ghLaunch(context, g.packageName); onGameClick(g.packageName) },
                onToggleFav  = { favorites = ghToggleFav(context, it) }
            )
        }
        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// HOME SCREEN — PS5-style fullscreen cover art + horizontal card strip
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GHHomeScreen(
    games: List<GameItem>, focusedIdx: Int, listState: LazyListState,
    time: String, batt: Int, favorites: Set<String>, context: Context,
    onNavSelect: (Int) -> Unit, onGameDetail: (GameItem) -> Unit,
    onGamePlay: (GameItem) -> Unit, onToggleFav: (String) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<GameItem?>(null) }

    Box(Modifier.fillMaxSize()) {
        // Adaptive full-screen blurred cover art background
        val focused = games.getOrNull(focusedIdx)
        key(focused?.packageName) {
            AnimatedVisibility(visible = true, enter = fadeIn(tween(700))) {
                if (focused?.coverImageRes != null) {
                    Image(painterResource(focused.coverImageRes), null,
                        Modifier.fillMaxSize().blur(72.dp), contentScale = ContentScale.Crop)
                } else if (focused != null) {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(focused.coverGradient)))
                }
            }
        }

        // Dark vignette: lighter at top-center, darker at edges and bottom for card contrast
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xBB000000), Color(0x66000000), Color(0xCC000000), Color(0xEE000000)))
        ))

        // Top-right: time + battery (PS5 style)
        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(time, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Rounded.BatteryFull, null, tint = Gray, modifier = Modifier.size(14.dp))
            Text("$batt%", color = Gray, fontSize = 11.sp)
        }

        // Bottom area: game title + card strip + nav bar
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Focused game title + subtitle (shiny)
            focused?.let { g ->
                Column(Modifier.padding(start = 32.dp, end = 32.dp, bottom = 18.dp)) {
                    val brush = shimmerBrush()
                    Text(g.title,
                        style = TextStyle(brush = brush, fontSize = 30.sp, fontWeight = FontWeight.Black),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(g.subtitle, color = Gray, fontSize = 13.sp)
                }
            }

            // Horizontal card strip (PS5 game library)
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(games) { idx, game ->
                    val dist = abs(idx - focusedIdx)
                    val isFocused = dist == 0
                    val scale = when { isFocused -> 1f; dist == 1 -> 0.76f; else -> 0.60f }
                    val alpha = when { isFocused -> 1f; dist == 1 -> 0.52f; else -> 0.20f }
                    GHGameCard(
                        game = game, isFocused = isFocused, isFav = game.packageName in favorites,
                        scale = scale, alpha = alpha, context = context,
                        onPlay   = { onGamePlay(game) },
                        onDetail = { onGameDetail(game) },
                        onFav    = { onToggleFav(game.packageName) },
                        onDelete = { deleteTarget = game }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 6-icon bottom nav bar
            GHBottomNavBar(selectedNav = 0, onSelect = onNavSelect)
            Spacer(Modifier.height(12.dp))
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { g ->
        GHDeleteDialog(game = g,
            onConfirm = { deleteTarget = null },
            onDismiss = { deleteTarget = null })
    }
}

// ── Game card (focused: big + glow + buttons; others: dimmed) ─────────────
@Composable
private fun GHGameCard(
    game: GameItem, isFocused: Boolean, isFav: Boolean,
    scale: Float, alpha: Float, context: Context,
    onPlay: () -> Unit, onDetail: () -> Unit, onFav: () -> Unit, onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val animScale by animateFloatAsState(scale, spring(0.62f, 240f), label = "csc")
    val animAlpha by animateFloatAsState(alpha, tween(280), label = "cal")
    val borderColor by animateColorAsState(
        if (isFocused) Accent.copy(0.75f) else White.copy(0.08f), tween(300), label = "bc")
    val cardW = 160.dp; val cardH = 215.dp
    val installed = if (isFocused) ghInstalled(context, game.packageName) else false

    val (dotC, statL) = when (game.serverStatus) {
        ServerStatus.ONLINE      -> GreenLive to "Online"
        ServerStatus.MAINTENANCE -> AmberWarn to "Maintenance"
        ServerStatus.OFFLINE     -> RedAlert  to "Offline"
        ServerStatus.BUSY        -> AmberWarn to "Busy"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(cardW).graphicsLayer { scaleX = animScale; scaleY = animScale; this.alpha = animAlpha }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Radial glow bloom (only focused card, pulsing)
            if (isFocused) {
                val glowPulse by rememberInfiniteTransition(label = "gp").animateFloat(
                    0.22f, 0.40f, infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ga")
                Box(Modifier.size(250.dp).blur(36.dp).background(
                    Brush.radialGradient(listOf(Accent.copy(glowPulse), White.copy(glowPulse * 0.4f), Color.Transparent))))
            }

            // Card body
            Box(
                Modifier.width(cardW).height(cardH)
                    .shadow(if (isFocused) 22.dp else 3.dp, RoundedCornerShape(16.dp),
                        spotColor = Accent.copy(if (isFocused) 0.45f else 0f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable { if (isFocused) onDetail() }
            ) {
                // Cover image
                if (game.coverImageRes != null) {
                    Image(painterResource(game.coverImageRes), game.title,
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)),
                        contentAlignment = Alignment.Center) {
                        Text(game.coverText, color = White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    }
                }
                // Glass sheen overlay
                Box(Modifier.fillMaxSize().background(BgCard))
                // Bottom gradient
                Box(Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomStart)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE8000000)))))

                // Status pill (top-left)
                Row(
                    Modifier.align(Alignment.TopStart).padding(9.dp)
                        .clip(RoundedCornerShape(20.dp)).background(Color(0xCC000000))
                        .border(1.dp, dotC.copy(0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                        0.4f, 1f, infiniteRepeatable(tween(950), RepeatMode.Reverse), label = "dp")
                    Box(Modifier.size(5.dp).clip(CircleShape).background(dotC.copy(pulse)))
                    Text(statL, color = White.copy(0.8f), fontSize = 8.sp, fontWeight = FontWeight.Medium)
                }

                // 3-dot menu button (top-right, only on focused card)
                if (isFocused) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .size(28.dp).clip(CircleShape)
                            .background(Color.Black.copy(0.5f))
                            .border(1.dp, White.copy(0.18f), CircleShape)
                            .clickable { sndMenu(); showMenu = true },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.MoreVert, "Menu", tint = White, modifier = Modifier.size(16.dp)) }
                }
            }
        }

        // Below focused card: Play Game button
        if (isFocused) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { sndSelect(); onPlay() },
                modifier = Modifier.width(140.dp).height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (installed) GreenLive else White)
            ) {
                Icon(if (installed) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                    null, tint = Color.Black, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (installed) "Play Game" else "Install",
                    color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 3-dot dropdown (focused card only)
    if (showMenu && isFocused) {
        Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF0E1016),
                border = BorderStroke(1.dp, White.copy(0.12f)), shadowElevation = 18.dp,
                modifier = Modifier.width(176.dp)) {
                Column(Modifier.padding(6.dp)) {
                    GHMenuRow(Icons.Rounded.Delete, "Hapus Data Game", RedAlert)  { showMenu = false; onDelete() }
                    GHMenuRow(Icons.Rounded.Info,   "Info & Detail")              { showMenu = false; onDetail() }
                    GHMenuRow(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (isFav) "Hapus Favorit" else "Tambah Favorit",
                        if (isFav) RedAlert else White)                            { showMenu = false; onFav() }
                }
            }
        }
    }
}

@Composable
private fun GHMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, tint: Color = White, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = White, fontSize = 13.sp)
    }
}

// ── Bottom navigation bar (6 icons, transparent glass pill) ───────────────
@Composable
private fun GHBottomNavBar(selectedNav: Int, onSelect: (Int) -> Unit) {
    val navItems = listOf(
        Icons.Rounded.SportsEsports   to "Games",
        Icons.Rounded.Settings        to "Settings",
        Icons.Rounded.Notifications   to "Notifikasi",
        Icons.Rounded.Favorite        to "Favorit",
        Icons.Rounded.Group           to "Teman",
        Icons.Rounded.PowerSettingsNew to "Keluar"
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 44.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black.copy(0.38f))
            .border(1.dp, White.copy(0.07f), RoundedCornerShape(32.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
    ) {
        navItems.forEachIndexed { i, (icon, desc) ->
            val sel   = selectedNav == i
            val isExit = i == 5
            var press by remember { mutableStateOf(false) }
            val sc by animateFloatAsState(if (press) 0.78f else 1f, spring(0.4f, 500f), label = "nb$i")
            Box(
                Modifier.size(44.dp).graphicsLayer { scaleX = sc; scaleY = sc }
                    .clip(CircleShape)
                    .background(if (sel) Accent.copy(0.18f) else Color.Transparent)
                    .border(if (sel) 1.dp else 0.dp, Accent.copy(if (sel) 0.5f else 0f), CircleShape)
                    .clickable { press = true; sndNav(); onSelect(i); press = false },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, desc,
                    tint = when { isExit -> RedAlert.copy(0.8f); sel -> Accent; else -> Gray },
                    modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ── Delete confirmation dialog ─────────────────────────────────────────────
@Composable
private fun GHDeleteDialog(game: GameItem, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var deleting by remember { mutableStateOf(false) }
    var done     by remember { mutableStateOf(false) }
    val scope    = rememberCoroutineScope()

    if (done) { LaunchedEffect(Unit) { delay(1600); onConfirm() } }

    Dialog(onDismissRequest = { if (!deleting) onDismiss() }) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF0C0E14),
            border = BorderStroke(1.dp, RedAlert.copy(0.3f))) {
            Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Delete, null, tint = RedAlert, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("Hapus Data Game", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Ini akan menghapus seluruh data & OBB \"${game.title}\" dari storage.\nTidak bisa dibatalkan.",
                    color = Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                when {
                    deleting -> { CircularProgressIndicator(color = RedAlert, modifier = Modifier.size(30.dp)); Spacer(Modifier.height(8.dp)); Text("Menghapus...", color = Gray, fontSize = 12.sp) }
                    done     -> { Icon(Icons.Rounded.CheckCircle, null, tint = GreenLive, modifier = Modifier.size(30.dp)); Spacer(Modifier.height(8.dp)); Text("Berhasil dihapus", color = GreenLive, fontSize = 12.sp) }
                    else     -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GrayDim), modifier = Modifier.weight(1f)) {
                            Text("Batal", color = Gray)
                        }
                        Button(onClick = {
                            scope.launch { deleting = true; withContext(Dispatchers.IO) { ghDeleteGameData(game.packageName) }; deleting = false; sndDelete(); done = true }
                        }, shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                            modifier = Modifier.weight(1f)) {
                            Text("Hapus", color = White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN — 4 tabs: Permissions | Storage | Download | About
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun GHSettingsScreen(games: List<GameItem>, context: Context, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Izin", "Storage", "Download", "Tentang")

    Column(Modifier.fillMaxSize().background(Bg)) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = White,
                modifier = Modifier.size(24.dp).clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Text("Settings", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        // Tab bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tabs.forEachIndexed { i, label ->
                val sel = tab == i
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) Accent.copy(0.14f) else BgCard)
                    .border(1.dp, if (sel) Accent.copy(0.5f) else DivCol, RoundedCornerShape(10.dp))
                    .clickable { sndNav(); tab = i }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center) {
                    Text(label, color = if (sel) Accent else Gray, fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        when (tab) {
            0 -> GHPermissionsTab(context)
            1 -> GHStorageTab(games, context)
            2 -> GHDownloadTab(games, context)
            3 -> GHAboutTab(context)
        }
    }
}

// ── Permissions tab ────────────────────────────────────────────────────────
@Composable
private fun GHPermissionsTab(context: Context) {
    var shizukuAvail   by remember { mutableStateOf(false) }
    var shizukuGranted by remember { mutableStateOf(false) }
    var rootAvail      by remember { mutableStateOf(false) }
    var storageOk      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                shizukuAvail   = rikka.shizuku.Shizuku.pingBinder()
                shizukuGranted = shizukuAvail && rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (_: Exception) {}
            try {
                val p = ProcessBuilder("su", "-c", "echo ok").start(); p.waitFor()
                rootAvail = p.exitValue() == 0
            } catch (_: Exception) {}
            storageOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || android.os.Environment.isExternalStorageManager()
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            GHSettSection("Shizuku")
            GHPermRow("Shizuku Service", "Diperlukan untuk install/hapus game data tanpa root", shizukuAvail)
            if (shizukuAvail) {
                Spacer(Modifier.height(6.dp))
                GHPermRow("Shizuku Permission", "Izin akses API Shizuku", shizukuGranted)
                if (!shizukuGranted) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { try { rikka.shizuku.Shizuku.requestPermission(1001) } catch (_: Exception) {} },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent.copy(0.85f))) {
                        Text("Minta Izin Shizuku", color = Bg, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            val i = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(i)
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Accent.copy(0.5f))
                ) { Text("Install Shizuku", color = Accent) }
            }
        }
        item {
            GHSettSection("Root Access")
            GHPermRow("Root (su)", "Akses root untuk operasi tingkat sistem (opsional, Shizuku lebih aman)", rootAvail)
        }
        item {
            GHSettSection("Storage Permission")
            GHPermRow("Manage All Files", "Akses penuh ke storage untuk instalasi data game", storageOk)
            if (!storageOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                .setData(Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {
                            try { context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent.copy(0.85f))
                ) { Text("Buka Pengaturan Izin", color = Bg, fontWeight = FontWeight.Bold) }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable private fun GHSettSection(title: String) {
    Text(title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
}

@Composable
private fun GHPermRow(title: String, desc: String, ok: Boolean) {
    Surface(shape = RoundedCornerShape(12.dp), color = BgCard, border = BorderStroke(1.dp, DivCol),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(desc,  color = Gray, fontSize = 11.sp)
            }
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (ok) GreenLive else RedAlert))
            Spacer(Modifier.width(7.dp))
            Text(if (ok) "OK" else "Perlu", color = if (ok) GreenLive else RedAlert,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Storage tab ────────────────────────────────────────────────────────────
@Composable
private fun GHStorageTab(games: List<GameItem>, context: Context) {
    var deleteTarget by remember { mutableStateOf<GameItem?>(null) }
    val storage = remember { ghStorageInfo() }
    val usedFrac = if (storage.totalGb > 0f) (storage.usedGb / storage.totalGb).coerceIn(0f, 1f) else 0f
    val animFrac by animateFloatAsState(usedFrac, tween(900), label = "sf")

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            GHSettSection("Penyimpanan Device")
            Surface(shape = RoundedCornerShape(16.dp), color = BgCard, border = BorderStroke(1.dp, DivCol),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Terpakai: ${"%.1f".format(storage.usedGb)} GB", color = White, fontSize = 13.sp)
                        Text("Bebas: ${"%.1f".format(storage.freeGb)} GB", color = GreenLive, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(White.copy(0.07f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(animFrac).clip(RoundedCornerShape(4.dp))
                            .background(Brush.horizontalGradient(listOf(Accent, Color(0xFF0070F3)))))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Total: ${"%.1f".format(storage.totalGb)} GB", color = GrayDim, fontSize = 11.sp)
                }
            }
        }
        item { GHSettSection("Game Terinstall di DLavie") }
        items(games) { game ->
            val installed = ghInstalled(context, game.packageName)
            var showMenu by remember { mutableStateOf(false) }
            Surface(shape = RoundedCornerShape(14.dp), color = BgCard, border = BorderStroke(1.dp, DivCol),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(game.coverGradient))) {
                        if (game.coverImageRes != null)
                            Image(painterResource(game.coverImageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(game.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(game.sizeMb, color = Gray, fontSize = 11.sp)
                        Box(Modifier.clip(RoundedCornerShape(4.dp))
                            .background((if (installed) GreenLive else GrayDim).copy(0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(if (installed) "Terinstall" else "Belum Install",
                                color = if (installed) GreenLive else GrayDim,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).background(BgGlass).clickable { showMenu = true },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MoreVert, "Menu", tint = Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (showMenu) {
                Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0E1016),
                        border = BorderStroke(1.dp, White.copy(0.1f))) {
                        Column(Modifier.width(162.dp).padding(6.dp)) {
                            GHMenuRow(Icons.Rounded.Delete, "Hapus Data Game", RedAlert) { showMenu = false; deleteTarget = game }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }

    deleteTarget?.let { g ->
        GHDeleteDialog(game = g, onConfirm = { deleteTarget = null }, onDismiss = { deleteTarget = null })
    }
}

// ── Download (DLC) tab ─────────────────────────────────────────────────────
@Composable
private fun GHDownloadTab(games: List<GameItem>, context: Context) {
    var dlcList by remember { mutableStateOf<List<DlcItem>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }
    var errMsg   by remember { mutableStateOf("") }
    val dlProg   = remember { mutableStateMapOf<String, Float>() }
    val dlStatus = remember { mutableStateMapOf<String, String>() }
    val scope    = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true; errMsg = ""
        try { dlcList = ghFetchDlc() } catch (e: Exception) { errMsg = e.message ?: "Gagal memuat" }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            GHSettSection("DLC & Mod Resmi dari DLavie")
            Text("Patch, OBB, dan mod yang diupload developer DLavie. Download & install otomatis ke device Anda.",
                color = Gray, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
        }
        when {
            loading -> item {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
            errMsg.isNotEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = RedAlert, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Gagal memuat DLC", color = White, fontSize = 14.sp)
                    Text(errMsg, color = Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            dlcList.isEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CloudQueue, null, tint = GrayDim, modifier = Modifier.size(50.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada DLC tersedia", color = Gray, fontSize = 14.sp)
                    Text("DLC resmi dari developer DLavie akan muncul di sini.", color = GrayDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
            else -> {
                val grouped = dlcList.groupBy { it.gamePkg }
                grouped.forEach { (_, group) ->
                    item { GHSettSection(group.first().gameTitle) }
                    items(group) { dlc ->
                        GHDlcCard(dlc, dlProg[dlc.id], dlStatus[dlc.id] ?: "") {
                            sndDl()
                            scope.launch {
                                dlStatus[dlc.id] = "Mengunduh..."
                                dlProg[dlc.id] = 0f
                                try {
                                    val ext = when (dlc.category.uppercase()) { "OBB" -> "obb"; "APK" -> "apk"; else -> "zip" }
                                    val out = File(context.externalCacheDir, "${dlc.id}.$ext")
                                    withContext(Dispatchers.IO) {
                                        val conn = (URL(dlc.fileUrl).openConnection() as HttpURLConnection).apply { connect() }
                                        val total = conn.contentLength.toFloat()
                                        var downloaded = 0L
                                        conn.inputStream.use { inp -> out.outputStream().use { o ->
                                            val buf = ByteArray(65536); var n: Int
                                            while (inp.read(buf).also { n = it } != -1) {
                                                o.write(buf, 0, n); downloaded += n
                                                if (total > 0) dlProg[dlc.id] = downloaded / total
                                            }
                                        }}
                                    }
                                    dlProg[dlc.id] = 1f; dlStatus[dlc.id] = "Menginstall..."
                                    val pkg = games.find { it.title.contains(dlc.gameTitle, true) }?.packageName ?: ""
                                    if (pkg.isNotEmpty()) withContext(Dispatchers.IO) {
                                        when (dlc.category.uppercase()) {
                                            "OBB"            -> ProcessBuilder("su", "-c", "mkdir -p /sdcard/Android/obb/$pkg && cp '${out.absolutePath}' /sdcard/Android/obb/$pkg/").start().waitFor()
                                            "DATA","MOD","PATCH" -> ProcessBuilder("su", "-c", "mkdir -p /sdcard/Android/data/$pkg && cd /sdcard/Android/data/$pkg && unzip -o '${out.absolutePath}'").start().waitFor()
                                        }
                                        out.delete()
                                    }
                                    dlStatus[dlc.id] = "✓ Selesai!"
                                    delay(3000); dlStatus.remove(dlc.id); dlProg.remove(dlc.id)
                                } catch (e: Exception) {
                                    dlStatus[dlc.id] = "Gagal: ${e.message?.take(38)}"
                                    dlProg.remove(dlc.id); delay(4000); dlStatus.remove(dlc.id)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun GHDlcCard(dlc: DlcItem, progress: Float?, status: String, onDownload: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = BgCard, border = BorderStroke(1.dp, DivCol),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Accent.copy(0.13f))
                    .border(1.dp, Accent.copy(0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)) {
                    Text(dlc.category, color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(dlc.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("v${dlc.version}  •  ${"%.1f".format(dlc.fileSizeMb)} MB", color = Gray, fontSize = 10.sp)
                }
                if (status.isEmpty() && progress == null) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Accent.copy(0.12f)).clickable { onDownload() },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Download, "Download", tint = Accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (dlc.description.isNotEmpty()) { Spacer(Modifier.height(4.dp)); Text(dlc.description, color = Gray, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            if (progress != null || status.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                if (progress != null && progress < 1f) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color = Accent, trackColor = White.copy(0.07f))
                    Spacer(Modifier.height(4.dp))
                    Text("${(progress * 100).toInt()}%", color = Gray, fontSize = 10.sp)
                } else {
                    Text(status, color = if (status.startsWith("Gagal")) RedAlert else GreenLive,
                        fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── About tab ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalTextApi::class)
@Composable
private fun GHAboutTab(context: Context) {
    fun openUrl(url: String) { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {} }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            GHSettSection("DLavie GameHub")
            Surface(shape = RoundedCornerShape(16.dp), color = BgCard, border = BorderStroke(1.dp, DivCol),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Accent, Color(0xFF0070F3)))),
                        contentAlignment = Alignment.Center) {
                        Text("DL", color = White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        val brush = shimmerBrush()
                        Text("DLavie GameHub", style = TextStyle(brush = brush, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        Text("v${BuildConfig.VERSION_NAME}", color = Gray, fontSize = 11.sp)
                        Text("by DLavie Company", color = GrayDim, fontSize = 10.sp)
                    }
                }
            }
        }
        item { GHSettSection("Bantuan & Legal") }
        item { GHAboutRow(Icons.Rounded.Help,       "FAQ",            "Pertanyaan umum seputar DLavie")    { sndSelect(); openUrl("https://drmacze.github.io/dlavie-web/#/faq") } }
        item { GHAboutRow(Icons.Rounded.Gavel,      "Terms of Service","Syarat & ketentuan penggunaan")     { sndSelect(); openUrl("https://drmacze.github.io/dlavie-web/#/terms") } }
        item { GHAboutRow(Icons.Rounded.PrivacyTip, "Privacy Policy", "Kebijakan privasi DLavie")          { sndSelect(); openUrl("https://drmacze.github.io/dlavie-web/#/privacy") } }
        item { GHAboutRow(Icons.Rounded.Info,       "Tentang DLavie", "Tentang platform & tim DLavie")     { sndSelect(); openUrl("https://drmacze.github.io/dlavie-web/#/about") } }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun GHAboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, onClick: () -> Unit) {
    var press by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (press) 0.97f else 1f, spring(0.4f, 400f), label = "ar")
    Surface(shape = RoundedCornerShape(14.dp), color = BgCard, border = BorderStroke(1.dp, DivCol),
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = sc; scaleY = sc }) {
        Row(Modifier.clickable { press = true; onClick(); press = false }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(0.11f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(desc, color = Gray, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = GrayDim, modifier = Modifier.size(18.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// SECONDARY SCREENS — Notifications, Favorites, Friends
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun GHNotificationsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        GHScrnHeader("Notifikasi", onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.NotificationsNone, null, tint = GrayDim, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Belum ada notifikasi", color = Gray, fontSize = 15.sp)
                Text("Notifikasi dari DLavie akan muncul di sini", color = GrayDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GHFavoritesScreen(
    games: List<GameItem>, favorites: Set<String>, context: Context,
    onBack: () -> Unit, onGameDetail: (GameItem) -> Unit, onToggleFav: (String) -> Unit
) {
    val favGames = games.filter { it.packageName in favorites }
    Column(Modifier.fillMaxSize().background(Bg)) {
        GHScrnHeader("Favorit", onBack)
        if (favGames.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FavoriteBorder, null, tint = GrayDim, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada game favorit", color = Gray, fontSize = 15.sp)
                    Text("Tambahkan dari menu ⋮ pada game card", color = GrayDim, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favGames) { g ->
                    GHGameListRow(g, context, onClick = { onGameDetail(g) },
                        onFav = { onToggleFav(g.packageName) }, isFav = true)
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun GHFriendsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        GHScrnHeader("Teman", onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.PeopleOutline, null, tint = GrayDim, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("Friends list coming soon", color = Gray, fontSize = 15.sp)
                Text("Fitur teman akan hadir di update mendatang", color = GrayDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GHScrnHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.ArrowBack, "Back", tint = White,
            modifier = Modifier.size(24.dp).clickable { onBack() })
        Spacer(Modifier.width(16.dp))
        Text(title, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GHGameListRow(game: GameItem, context: Context, onClick: () -> Unit, onFav: () -> Unit, isFav: Boolean) {
    Surface(shape = RoundedCornerShape(14.dp), color = BgCard, border = BorderStroke(1.dp, DivCol),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.clickable { onClick() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(game.coverGradient))) {
                if (game.coverImageRes != null)
                    Image(painterResource(game.coverImageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(game.subtitle, color = Gray, fontSize = 11.sp)
                Text(game.sizeMb,  color = GrayDim, fontSize = 10.sp)
            }
            IconButton(onClick = onFav, modifier = Modifier.size(36.dp)) {
                Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null, tint = if (isFav) RedAlert else Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// GAME DETAIL PAGE — fullscreen art + Play Game + metadata
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GHDetailPage(
    game: GameItem, context: Context, favorites: Set<String>,
    onBack: () -> Unit, onPlay: () -> Unit, onToggleFav: () -> Unit
) {
    val installed = ghInstalled(context, game.packageName)
    val isFav     = game.packageName in favorites

    Box(Modifier.fillMaxSize().background(Bg)) {
        if (game.coverImageRes != null) {
            Image(painterResource(game.coverImageRes), null,
                Modifier.fillMaxSize().blur(65.dp), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xBB000000), Color(0xEE000000)))))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                // Top bar
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ArrowBack, "Back", tint = White,
                        modifier = Modifier.size(24.dp).clickable { onBack() })
                    Spacer(Modifier.width(14.dp))
                    val brush = shimmerBrush()
                    Text(game.title, style = TextStyle(brush = brush, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Fav", tint = if (isFav) RedAlert else Gray,
                        modifier = Modifier.size(24.dp).clickable { onToggleFav() })
                }
                // Cover art
                Box(Modifier.fillMaxWidth().height(230.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp))) {
                    if (game.coverImageRes != null) {
                        Image(painterResource(game.coverImageRes), game.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)))
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Bg.copy(0.7f)))))
                }
                // Meta + description
                Column(Modifier.padding(20.dp)) {
                    // Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(game.version, game.sizeMb, game.category, game.ageRating).filter { it.isNotEmpty() }.forEach { info ->
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(BgCard)
                                .border(1.dp, DivCol, RoundedCornerShape(6.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)) {
                                Text(info, color = Gray, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Deskripsi", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(game.description.ifEmpty { "Game mod DLavie." }, color = Gray, fontSize = 13.sp)
                    if (game.features.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Fitur", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(6.dp))
                        game.features.forEach { f ->
                            Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = Accent); Text(f, color = Gray, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    // Server status row
                    val (dotC, statL) = when (game.serverStatus) {
                        ServerStatus.ONLINE      -> GreenLive to "Server Online"
                        ServerStatus.MAINTENANCE -> AmberWarn to "Maintenance"
                        ServerStatus.OFFLINE     -> RedAlert  to "Server Offline"
                        ServerStatus.BUSY        -> AmberWarn to "Sinyal Lemah"
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BgCard)
                        .border(1.dp, DivCol, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(dotC))
                        Spacer(Modifier.width(8.dp))
                        Text(statL, color = dotC, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text(game.developer, color = GrayDim, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(22.dp))
                    // Play button
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (installed) GreenLive else White)
                    ) {
                        Icon(if (installed) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                            null, tint = Color.Black, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(if (installed) "Play Game" else "Install Game",
                            color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}
