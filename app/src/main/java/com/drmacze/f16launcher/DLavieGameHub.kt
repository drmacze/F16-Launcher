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
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
// DLAVIE GAMEHUB v310 — Liquid Glass / Glassmorphism Redesign
// Monochrome (black + white) console aesthetic. Clean, minimal, elegant.
// ════════════════════════════════════════════════════════════════════════════

// ── Design tokens — monochrome glassmorphism ──────────────────────────────
private val Bg         = Color(0xFF050507)
private val GlassCard  = Color(0x12FFFFFF)   // card fill — ultra-thin frost
private val GlassMid   = Color(0x20FFFFFF)   // focused card fill
private val GlassBrd   = Color(0x25FFFFFF)   // card border
private val GlassBrdHi = Color(0x55FFFFFF)   // focused border
private val NavBg      = Color(0x18FFFFFF)   // nav bar pill
private val NavBrd     = Color(0x20FFFFFF)
private val White      = Color(0xFFFFFFFF)
private val White70    = Color(0xB3FFFFFF)
private val White30    = Color(0x4DFFFFFF)
private val White10    = Color(0x1AFFFFFF)
private val Slate      = Color(0xFF909098)
private val SlateDim   = Color(0xFF3E3E4A)
private val DivCol     = Color(0x12FFFFFF)
private val GreenOnline = Color(0xFF52D68A)
private val OrangeWarn  = Color(0xFFFFA040)
private val RedAlert    = Color(0xFFFF4545)

// ── Shimmer brush — white/silver only ─────────────────────────────────────
@OptIn(ExperimentalTextApi::class)
@Composable
private fun shimmerBrush(width: Float = 900f): Brush {
    val inf = rememberInfiniteTransition(label = "sh")
    val off by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "sh_off")
    return Brush.linearGradient(
        colors = listOf(White30, White70, White, White70, White30),
        start  = Offset((off * width * 2) - width, 0f),
        end    = Offset((off * width * 2), 30f)
    )
}

// ── Console sound effects ─────────────────────────────────────────────────
private fun playTone(freq: Double = 880.0, ms: Int = 55, vol: Float = 0.16f) {
    Thread {
        try {
            val sr = 44100; val n = sr * ms / 1000
            val buf = ShortArray(n)
            for (i in 0 until n) {
                val t = i.toDouble() / sr
                val env = when {
                    i < n / 6     -> i.toDouble() / (n / 6)
                    i > n * 5 / 6 -> (n - i).toDouble() / (n / 6)
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
            track.write(buf, 0, buf.size); track.play()
            Thread.sleep(ms + 60L); track.stop(); track.release()
        } catch (_: Exception) {}
    }.start()
}
private fun sndNav()    = playTone(660.0, 42, 0.11f)
private fun sndSelect() = playTone(880.0, 60, 0.18f)
private fun sndBack()   = playTone(440.0, 48, 0.13f)
private fun sndMenu()   = playTone(1100.0, 36, 0.12f)
private fun sndDelete() = playTone(220.0, 110, 0.20f)
private fun sndDl()     = playTone(1320.0, 70, 0.15f)
private fun sndBoot()   { Thread { listOf(330.0 to 80, 550.0 to 80, 880.0 to 100).forEach { (f,t) -> playTone(f, t, 0.13f); Thread.sleep(t + 30L) } }.start() }

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

private const val GH_PREFS = "gh_fav_v310"
private fun ghLoadFav(c: Context): Set<String> = try {
    c.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE).getStringSet("f", emptySet()) ?: emptySet()
} catch (_: Exception) { emptySet() }
private fun ghToggleFav(c: Context, pkg: String): Set<String> {
    val s = ghLoadFav(c).toMutableSet()
    if (pkg in s) s.remove(pkg) else s.add(pkg)
    c.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE).edit().putStringSet("f", s).apply()
    return s
}

// ── Game data deletion ─────────────────────────────────────────────────────
private fun ghDeleteGameData(pkg: String): Boolean = try {
    listOf("/sdcard/Android/data/$pkg", "/sdcard/Android/obb/$pkg").forEach { dir ->
        val p = ProcessBuilder("su", "-c", "rm -rf $dir").start(); p.waitFor()
        if (p.exitValue() != 0) File(dir).takeIf { it.exists() }?.deleteRecursively()
    }
    true
} catch (_: Exception) { false }

// ── Storage info ───────────────────────────────────────────────────────────
data class GHStorageInfo(val totalGb: Float, val usedGb: Float, val freeGb: Float)
private fun ghStorageInfo(): GHStorageInfo = try {
    val s = StatFs("/sdcard"); val total = s.totalBytes / 1_073_741_824f; val free = s.availableBytes / 1_073_741_824f
    GHStorageInfo(total, total - free, free)
} catch (_: Exception) { GHStorageInfo(0f, 0f, 0f) }

// ── DLC data model + Supabase fetch ────────────────────────────────────────
/**
 * Supabase table: dlc_catalog
 *   id           uuid pk default uuid_generate_v4()
 *   game_pkg     text   -- e.g. "com.ea.gp.fifaworld"
 *   game_title   text
 *   title        text
 *   description  text
 *   file_url     text
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
// GAMEHUB LOADING TRANSITION — clean minimal boot (text only, no logo box)
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
        "Memuat library game...",
        "Memeriksa pembaruan DLC...",
        "Menyiapkan antarmuka...",
        "Siap!"
    )

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        sndBoot(); phase = 0; delay(400)
        phase = 1; delay(300)
        phase = 2
        "DLAVIE".forEach { typed += it; delay(110) }
        delay(200); phase = 3
        bootMsgs.forEach { msg -> statusMsg = msg; delay(550) }
        statusMsg = ""; delay(150)
        phase = 4; delay(600)
        onComplete(); phase = 5
    }

    if (visible && phase < 5) {
        val alpha by animateFloatAsState(
            when (phase) { 0 -> 0f; 4 -> 0f; else -> 1f }, tween(600), label = "la")
        val titleScale by animateFloatAsState(
            when (phase) { 0 -> 0.80f; 4 -> 1.10f; else -> 1f }, spring(0.65f, 200f), label = "ts")

        Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {

            // Subtle radial vignette
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(
                    listOf(Color(0x00000000), Color(0x88000000)),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width * 0.9f
                ))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            ) {
                // "DLAVIE" — shiny typed text, no box logo
                val brush = shimmerBrush(600f)
                Text(
                    typed,
                    style = TextStyle(
                        brush = brush,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 14.sp
                    ),
                    modifier = Modifier.graphicsLayer { scaleX = titleScale; scaleY = titleScale }
                )

                // "GAMEHUB" subtitle
                Spacer(Modifier.height(4.dp))
                Text(
                    "GAMEHUB",
                    color = White30,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 6.sp
                )

                Spacer(Modifier.height(52.dp))

                // Progress bar + streaming status
                if (phase == 3) {
                    val msgIdx = bootMsgs.indexOf(statusMsg).coerceAtLeast(0)
                    val rawProg = (msgIdx + 1).toFloat() / bootMsgs.size
                    val animProg by animateFloatAsState(rawProg, tween(500), label = "prog")

                    Box(
                        Modifier.width(180.dp).height(1.dp)
                            .clip(RoundedCornerShape(1.dp)).background(White10)
                    ) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(animProg)
                                .clip(RoundedCornerShape(1.dp)).background(White70)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    AnimatedContent(statusMsg, transitionSpec = {
                        fadeIn(tween(160)) togetherWith fadeOut(tween(100))
                    }, label = "smsg") { msg ->
                        Text(msg, color = Slate, fontSize = 11.sp, letterSpacing = 0.4.sp)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// MAIN ENTRY — DLavieGameHub
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

    // Immersive full-screen
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
                    w.setDecorFitsSystemWindows(true); w.insetsController?.show(WindowInsets.Type.systemBars())
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

    var favorites   by remember { mutableStateOf(ghLoadFav(context)) }
    var selectedNav  by remember { mutableIntStateOf(0) }
    var showDetail   by remember { mutableStateOf<GameItem?>(null) }

    // ── Card swipe + snap (uses rememberSnapFlingBehavior for reliable snap) ──
    val listState = rememberLazyListState()
    val snapFling = rememberSnapFlingBehavior(listState)

    val focusedIdx by remember {
        derivedStateOf {
            // Use firstVisibleItemIndex as base; offset > half card (160dp@~2.75x ≈ 220px) → advance
            val vis = listState.firstVisibleItemIndex
            val off = listState.firstVisibleItemScrollOffset
            (vis + if (off > 220) 1 else 0).coerceIn(0, games.lastIndex)
        }
    }

    // Play sound when focused card changes
    var prevIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(focusedIdx) { if (focusedIdx != prevIdx) { prevIdx = focusedIdx; sndNav() } }

    Box(Modifier.fillMaxSize().background(Bg)) {
        showDetail?.let { g ->
            GHDetailPage(g, context, favorites,
                onBack      = { sndBack(); showDetail = null },
                onPlay      = { sndSelect(); ghLaunch(context, g.packageName); onGameClick(g.packageName) },
                onToggleFav = { favorites = ghToggleFav(context, g.packageName) }
            )
        } ?: when (selectedNav) {
            0 -> GHHomeScreen(
                games = games, focusedIdx = focusedIdx, listState = listState, snapFling = snapFling,
                time = time, batt = batt, favorites = favorites, context = context,
                onNavSelect  = { idx -> if (idx == 5) { sndBack(); onExit() } else { sndNav(); selectedNav = idx } },
                onGameDetail = { sndSelect(); showDetail = it },
                onGamePlay   = { g -> sndSelect(); ghLaunch(context, g.packageName); onGameClick(g.packageName) },
                onToggleFav  = { favorites = ghToggleFav(context, it) }
            )
            1 -> GHSettingsScreen(games = games, context = context, onBack = { sndBack(); selectedNav = 0 })
            2 -> GHNotificationsScreen(onBack = { sndBack(); selectedNav = 0 })
            3 -> GHFavoritesScreen(games = games, favorites = favorites, context = context,
                onBack       = { sndBack(); selectedNav = 0 },
                onGameDetail = { sndSelect(); showDetail = it },
                onToggleFav  = { favorites = ghToggleFav(context, it) })
            4 -> GHFriendsScreen(onBack = { sndBack(); selectedNav = 0 })
            else -> GHHomeScreen(
                games = games, focusedIdx = focusedIdx, listState = listState, snapFling = snapFling,
                time = time, batt = batt, favorites = favorites, context = context,
                onNavSelect  = { idx -> if (idx == 5) { sndBack(); onExit() } else { sndNav(); selectedNav = idx } },
                onGameDetail = { sndSelect(); showDetail = it },
                onGamePlay   = { g -> sndSelect(); ghLaunch(context, g.packageName); onGameClick(g.packageName) },
                onToggleFav  = { favorites = ghToggleFav(context, it) }
            )
        }
        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// HOME SCREEN — fullscreen blurred cover art + card strip + glass nav
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GHHomeScreen(
    games: List<GameItem>, focusedIdx: Int, listState: LazyListState,
    snapFling: androidx.compose.foundation.gestures.FlingBehavior,
    time: String, batt: Int, favorites: Set<String>, context: Context,
    onNavSelect: (Int) -> Unit, onGameDetail: (GameItem) -> Unit,
    onGamePlay: (GameItem) -> Unit, onToggleFav: (String) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<GameItem?>(null) }
    val focused = games.getOrNull(focusedIdx)

    Box(Modifier.fillMaxSize()) {

        // ── Fullscreen crossfading blurred cover art ──
        key(focused?.packageName) {
            AnimatedVisibility(visible = true, enter = fadeIn(tween(800))) {
                if (focused?.coverImageRes != null) {
                    androidx.compose.foundation.Image(
                        painterResource(focused.coverImageRes), null,
                        Modifier.fillMaxSize().blur(80.dp), contentScale = ContentScale.Crop
                    )
                } else if (focused != null) {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(focused.coverGradient)))
                }
            }
        }

        // ── Layered dark scrim: keeps top clear, bottom darker for cards ──
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(
                Color(0x99000000), Color(0x55000000),
                Color(0x88000000), Color(0xCC000000), Color(0xF2000000)
            ))
        ))

        // ── Top-right: time + battery ──
        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(time, color = White70, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
            Icon(Icons.Rounded.BatteryFull, null, tint = Slate, modifier = Modifier.size(13.dp))
            Text("$batt%", color = Slate, fontSize = 11.sp)
        }

        // ── Bottom area: title + card strip + nav ──
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {

            // Focused game title + subtitle
            focused?.let { g ->
                Column(Modifier.padding(start = 28.dp, end = 28.dp, bottom = 20.dp)) {
                    val brush = shimmerBrush()
                    Text(g.title,
                        style = TextStyle(brush = brush, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(g.subtitle, color = Slate, fontSize = 12.sp, letterSpacing = 0.3.sp)
                }
            }

            // ── PS5-style horizontal card strip with snap fling ──
            LazyRow(
                state = listState,
                flingBehavior = snapFling,
                contentPadding = PaddingValues(horizontal = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(games) { idx, game ->
                    val dist = abs(idx - focusedIdx)
                    val isFocused = dist == 0
                    GHGameCard(
                        game = game, isFocused = isFocused, isFav = game.packageName in favorites,
                        scale     = when { isFocused -> 1f; dist == 1 -> 0.78f; else -> 0.62f },
                        alpha     = when { isFocused -> 1f; dist == 1 -> 0.48f; else -> 0.18f },
                        context   = context,
                        onPlay    = { onGamePlay(game) },
                        onDetail  = { onGameDetail(game) },
                        onFav     = { onToggleFav(game.packageName) },
                        onDelete  = { deleteTarget = game }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 6-icon glass bottom nav bar ──
            GHBottomNavBar(selectedNav = 0, onSelect = onNavSelect)
            Spacer(Modifier.height(4.dp))
        }
    }

    deleteTarget?.let { g ->
        GHDeleteDialog(game = g, onConfirm = { deleteTarget = null }, onDismiss = { deleteTarget = null })
    }
}

// ── Game card — liquid glass style ────────────────────────────────────────
@Composable
private fun GHGameCard(
    game: GameItem, isFocused: Boolean, isFav: Boolean,
    scale: Float, alpha: Float, context: Context,
    onPlay: () -> Unit, onDetail: () -> Unit, onFav: () -> Unit, onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val animScale by animateFloatAsState(scale, spring(0.60f, 220f), label = "csc")
    val animAlpha by animateFloatAsState(alpha, tween(260), label = "cal")
    val borderAlpha by animateFloatAsState(if (isFocused) 0.55f else 0.15f, tween(280), label = "ba")

    val cardW = 158.dp; val cardH = 180.dp

    val (statusColor, statusLabel) = when (game.serverStatus) {
        ServerStatus.ONLINE      -> GreenOnline to "Online"
        ServerStatus.MAINTENANCE -> OrangeWarn  to "Maintenance"
        ServerStatus.OFFLINE     -> RedAlert    to "Offline"
        ServerStatus.BUSY        -> OrangeWarn  to "Busy"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(cardW)
            .graphicsLayer { scaleX = animScale; scaleY = animScale; this.alpha = animAlpha }
    ) {
        Box(contentAlignment = Alignment.Center) {

            // White glow behind focused card (subtle pulse)
            if (isFocused) {
                val glowAnim by rememberInfiniteTransition(label = "glow").animateFloat(
                    0.12f, 0.24f,
                    infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "ga"
                )
                Box(
                    Modifier.size(220.dp).blur(38.dp)
                        .background(Brush.radialGradient(
                            listOf(White.copy(glowAnim), Color.Transparent)
                        ))
                )
            }

            // ── Card glass body ──
            Box(
                Modifier
                    .width(cardW).height(cardH)
                    .shadow(if (isFocused) 16.dp else 2.dp, RoundedCornerShape(18.dp),
                        spotColor = White.copy(if (isFocused) 0.20f else 0f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isFocused) GlassMid else GlassCard)
                    .border(
                        width = if (isFocused) 1.5.dp else 1.dp,
                        color = White.copy(borderAlpha),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { if (isFocused) onDetail() }
            ) {
                // Cover image
                if (game.coverImageRes != null) {
                    androidx.compose.foundation.Image(
                        painterResource(game.coverImageRes), game.title,
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)),
                        contentAlignment = Alignment.Center) {
                        Text(game.coverText, color = White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Frosted glass bottom gradient
                Box(
                    Modifier.fillMaxWidth().height(90.dp).align(Alignment.BottomStart)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000))))
                )

                // Status pill — top left
                Row(
                    Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xBB000000))
                        .border(0.5.dp, statusColor.copy(0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val pulse by rememberInfiniteTransition(label = "p").animateFloat(
                        0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dp")
                    Box(Modifier.size(5.dp).clip(CircleShape).background(statusColor.copy(pulse)))
                    Text(statusLabel, color = White70, fontSize = 8.5.sp, fontWeight = FontWeight.Medium)
                }

                // 3-dot menu — top right (focused only)
                if (isFocused) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .size(28.dp).clip(CircleShape)
                            .background(Color(0x99000000))
                            .border(0.5.dp, White.copy(0.25f), CircleShape)
                            .clickable { sndMenu(); showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MoreVert, "Menu", tint = White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Play button — below focused card only
        if (isFocused) {
            val installed = ghInstalled(context, game.packageName)
            Spacer(Modifier.height(14.dp))
            // Glass pill play button
            Box(
                Modifier.width(144.dp).height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(White.copy(0.12f))
                    .border(1.dp, White.copy(0.40f), RoundedCornerShape(19.dp))
                    .clickable { sndSelect(); onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (installed) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                        null, tint = White, modifier = Modifier.size(16.dp)
                    )
                    Text(
                        if (installed) "Play Game" else "Install",
                        color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }

    // 3-dot dropdown
    if (showMenu && isFocused) {
        val isFavNow = isFav
        Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xEE0A0A0F),
                border = BorderStroke(1.dp, White.copy(0.14f)),
                shadowElevation = 20.dp,
                modifier = Modifier.width(182.dp)
            ) {
                Column(Modifier.padding(6.dp)) {
                    GHMenuRow(Icons.Rounded.Delete,       "Hapus Data Game",   RedAlert)    { showMenu = false; onDelete() }
                    GHMenuRow(Icons.Rounded.Info,         "Info & Detail")                  { showMenu = false; onDetail() }
                    GHMenuRow(
                        if (isFavNow) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        if (isFavNow) "Hapus Favorit" else "Tambah Favorit",
                        if (isFavNow) RedAlert else White
                    ) { showMenu = false; onFav() }
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = White, fontSize = 13.sp, fontWeight = FontWeight.Normal)
    }
}

// ── Bottom nav bar — 6 icons, liquid glass pill ────────────────────────────
@Composable
private fun GHBottomNavBar(selectedNav: Int, onSelect: (Int) -> Unit) {
    // Icon list: imageVector + description
    val navItems = listOf(
        Icons.Rounded.SportsEsports    to "Games",
        Icons.Rounded.Settings         to "Settings",
        Icons.Rounded.Notifications    to "Notifikasi",
        Icons.Rounded.Favorite         to "Favorit",
        Icons.Rounded.Group            to "Teman",
        Icons.Rounded.PowerSettingsNew to "Keluar"
    )

    Box(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(NavBg)
            .border(1.dp, NavBrd, RoundedCornerShape(36.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEachIndexed { i, (icon, desc) ->
                val isSelected = selectedNav == i
                val isExit     = i == 5
                val iconTint by animateColorAsState(
                    when { isExit -> RedAlert; isSelected -> White; else -> White70 },
                    tween(200), label = "nt$i"
                )
                val bgAlpha by animateFloatAsState(if (isSelected) 0.20f else 0f, tween(200), label = "nb$i")
                var pressed by remember { mutableStateOf(false) }
                val sc by animateFloatAsState(if (pressed) 0.75f else 1f, spring(0.4f, 500f), label = "np$i")

                Box(
                    Modifier.size(46.dp)
                        .graphicsLayer { scaleX = sc; scaleY = sc }
                        .clip(CircleShape)
                        .background(White.copy(bgAlpha))
                        .clickable {
                            pressed = true
                            onSelect(i)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = desc,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
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

    if (done) { LaunchedEffect(Unit) { delay(1500); onConfirm() } }

    Dialog(onDismissRequest = { if (!deleting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF00A0A0E),
            border = BorderStroke(1.dp, RedAlert.copy(0.30f))
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Delete, null, tint = RedAlert, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(12.dp))
                Text("Hapus Data Game", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Ini akan menghapus seluruh data & OBB\n\"${game.title}\" dari storage. Tidak bisa dibatalkan.",
                    color = Slate, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                when {
                    deleting -> { CircularProgressIndicator(color = RedAlert, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(8.dp)); Text("Menghapus...", color = Slate, fontSize = 12.sp) }
                    done     -> { Icon(Icons.Rounded.CheckCircle, null, tint = GreenOnline, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(8.dp)); Text("Berhasil dihapus", color = GreenOnline, fontSize = 12.sp) }
                    else     -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SlateDim), modifier = Modifier.weight(1f)) {
                            Text("Batal", color = Slate)
                        }
                        Button(onClick = {
                            scope.launch { deleting = true; withContext(Dispatchers.IO) { ghDeleteGameData(game.packageName) }; deleting = false; sndDelete(); done = true }
                        }, shape = RoundedCornerShape(12.dp),
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
// SETTINGS SCREEN — 4 tabs: Izin | Storage | Download | Tentang
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun GHSettingsScreen(games: List<GameItem>, context: Context, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Izin", "Storage", "Download", "Tentang")

    Column(Modifier.fillMaxSize().background(Bg)) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(GlassCard)
                .border(1.dp, GlassBrd, CircleShape).clickable { onBack() },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Settings", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        }
        // Tab bar — glass pills
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tabs.forEachIndexed { i, label ->
                val sel = tab == i
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) White.copy(0.12f) else GlassCard)
                        .border(1.dp, if (sel) White.copy(0.35f) else DivCol, RoundedCornerShape(12.dp))
                        .clickable { sndNav(); tab = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (sel) White else Slate, fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
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
            try { val p = ProcessBuilder("su", "-c", "echo ok").start(); p.waitFor(); rootAvail = p.exitValue() == 0 } catch (_: Exception) {}
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
                    GHGlassButton("Minta Izin Shizuku") { try { rikka.shizuku.Shizuku.requestPermission(1001) } catch (_: Exception) {} }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                GHGlassButton("Install Shizuku") {
                    try {
                        val i = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(i)
                    } catch (_: Exception) {}
                }
            }
        }
        item {
            GHSettSection("Root Access")
            GHPermRow("Root (su)", "Akses root untuk operasi tingkat sistem (opsional)", rootAvail)
        }
        item {
            GHSettSection("Storage Permission")
            GHPermRow("Manage All Files", "Akses penuh ke storage untuk instalasi data game", storageOk)
            if (!storageOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Spacer(Modifier.height(8.dp))
                GHGlassButton("Buka Pengaturan Izin") {
                    try {
                        context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            .setData(Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {
                        try { context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable private fun GHSettSection(title: String) {
    Text(title.uppercase(), color = White30, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
}

@Composable
private fun GHPermRow(title: String, desc: String, ok: Boolean) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(GlassCard).border(1.dp, DivCol, RoundedCornerShape(14.dp))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(desc,  color = Slate, fontSize = 11.sp)
            }
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (ok) GreenOnline else RedAlert))
            Spacer(Modifier.width(7.dp))
            Text(if (ok) "OK" else "Perlu", color = if (ok) GreenOnline else RedAlert,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GHGlassButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(GlassCard)
            .border(1.dp, GlassBrd, RoundedCornerShape(12.dp))
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 11.dp)
    ) { Text(label, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
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
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassCard)
                .border(1.dp, DivCol, RoundedCornerShape(16.dp))) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Terpakai: ${"%.1f".format(storage.usedGb)} GB", color = White, fontSize = 13.sp)
                        Text("Bebas: ${"%.1f".format(storage.freeGb)} GB", color = GreenOnline, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(White10)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(animFrac).clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(White70, White30))))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Total: ${"%.1f".format(storage.totalGb)} GB", color = SlateDim, fontSize = 11.sp)
                }
            }
        }
        item { GHSettSection("Game Terinstall di DLavie") }
        items(games) { game ->
            val installed = ghInstalled(context, game.packageName)
            var showMenu by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassCard)
                .border(1.dp, DivCol, RoundedCornerShape(14.dp))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(game.coverGradient))) {
                        if (game.coverImageRes != null)
                            androidx.compose.foundation.Image(painterResource(game.coverImageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(game.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(game.sizeMb, color = Slate, fontSize = 11.sp)
                        Spacer(Modifier.height(2.dp))
                        Box(Modifier.clip(RoundedCornerShape(4.dp))
                            .background((if (installed) GreenOnline else SlateDim).copy(0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(if (installed) "Terinstall" else "Belum Install",
                                color = if (installed) GreenOnline else SlateDim,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(Modifier.size(32.dp).clip(CircleShape).background(GlassCard)
                        .border(0.5.dp, DivCol, CircleShape).clickable { showMenu = true },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.MoreVert, "Menu", tint = Slate, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (showMenu) {
                Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xEE0A0A0E),
                        border = BorderStroke(1.dp, White.copy(0.10f))) {
                        Column(Modifier.width(168.dp).padding(6.dp)) {
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
            GHSettSection("DLC & Mod Resmi DLavie")
            Text("Patch, OBB, dan mod dari developer DLavie. Download & install otomatis.",
                color = Slate, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
        }
        when {
            loading -> item {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = White70, strokeWidth = 1.5.dp, modifier = Modifier.size(28.dp))
                }
            }
            errMsg.isNotEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = RedAlert, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Gagal memuat DLC", color = White, fontSize = 14.sp)
                    Text(errMsg, color = Slate, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            dlcList.isEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CloudQueue, null, tint = SlateDim, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada DLC tersedia", color = Slate, fontSize = 14.sp)
                    Text("DLC resmi dari developer DLavie akan muncul di sini.", color = SlateDim, fontSize = 12.sp, textAlign = TextAlign.Center)
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
                                dlStatus[dlc.id] = "Mengunduh..."; dlProg[dlc.id] = 0f
                                try {
                                    val ext = when (dlc.category.uppercase()) { "OBB" -> "obb"; "APK" -> "apk"; else -> "zip" }
                                    val out = File(context.externalCacheDir, "${dlc.id}.$ext")
                                    withContext(Dispatchers.IO) {
                                        val conn = (URL(dlc.fileUrl).openConnection() as HttpURLConnection).apply { connect() }
                                        val total = conn.contentLength.toFloat(); var downloaded = 0L
                                        conn.inputStream.use { inp -> out.outputStream().use { o ->
                                            val buf = ByteArray(65536); var n: Int
                                            while (inp.read(buf).also { n = it } != -1) { o.write(buf, 0, n); downloaded += n; if (total > 0) dlProg[dlc.id] = downloaded / total }
                                        }}
                                    }
                                    dlProg[dlc.id] = 1f; dlStatus[dlc.id] = "Menginstall..."
                                    val pkg = games.find { it.title.contains(dlc.gameTitle, true) }?.packageName ?: ""
                                    if (pkg.isNotEmpty()) withContext(Dispatchers.IO) {
                                        when (dlc.category.uppercase()) {
                                            "OBB" -> ProcessBuilder("su", "-c", "mkdir -p /sdcard/Android/obb/$pkg && cp '${out.absolutePath}' /sdcard/Android/obb/$pkg/").start().waitFor()
                                            "DATA","MOD","PATCH" -> ProcessBuilder("su", "-c", "mkdir -p /sdcard/Android/data/$pkg && cd /sdcard/Android/data/$pkg && unzip -o '${out.absolutePath}'").start().waitFor()
                                        }
                                        out.delete()
                                    }
                                    dlStatus[dlc.id] = "✓ Selesai!"; delay(3000); dlStatus.remove(dlc.id); dlProg.remove(dlc.id)
                                } catch (e: Exception) {
                                    dlStatus[dlc.id] = "Gagal: ${e.message?.take(40)}"; dlProg.remove(dlc.id); delay(4000); dlStatus.remove(dlc.id)
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
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassCard)
        .border(1.dp, DivCol, RoundedCornerShape(14.dp))) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(White.copy(0.08f))
                    .border(0.5.dp, White.copy(0.20f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)) {
                    Text(dlc.category, color = White70, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(dlc.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("v${dlc.version}  •  ${"%.1f".format(dlc.fileSizeMb)} MB", color = Slate, fontSize = 10.sp)
                }
                if (status.isEmpty() && progress == null) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(GlassMid)
                        .border(1.dp, GlassBrd, CircleShape).clickable { onDownload() },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Download, "Download", tint = White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (dlc.description.isNotEmpty()) { Spacer(Modifier.height(4.dp)); Text(dlc.description, color = Slate, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            if (progress != null || status.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                if (progress != null && progress < 1f) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)), color = White70, trackColor = White10)
                    Spacer(Modifier.height(4.dp)); Text("${(progress * 100).toInt()}%", color = Slate, fontSize = 10.sp)
                } else {
                    Text(status, color = if (status.startsWith("Gagal")) RedAlert else GreenOnline, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassCard)
                .border(1.dp, DivCol, RoundedCornerShape(16.dp))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Logo: simple glass circle with "D" text
                    Box(Modifier.size(52.dp).clip(CircleShape).background(White.copy(0.10f))
                        .border(1.dp, White.copy(0.25f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("D", color = White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        val brush = shimmerBrush(400f)
                        Text("DLavie GameHub", style = TextStyle(brush = brush, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        Text("v${BuildConfig.VERSION_NAME}", color = Slate, fontSize = 11.sp)
                        Text("by DLavie Company", color = SlateDim, fontSize = 10.sp)
                    }
                }
            }
        }
        item { GHSettSection("Bantuan & Legal") }
        item { GHAboutRow(Icons.Rounded.Help,       "FAQ",             "Pertanyaan umum seputar DLavie")   { openUrl("https://drmacze.github.io/dlavie-web/#/faq") } }
        item { GHAboutRow(Icons.Rounded.Gavel,      "Terms of Service","Syarat & ketentuan penggunaan")    { openUrl("https://drmacze.github.io/dlavie-web/#/terms") } }
        item { GHAboutRow(Icons.Rounded.PrivacyTip, "Privacy Policy",  "Kebijakan privasi DLavie")         { openUrl("https://drmacze.github.io/dlavie-web/#/privacy") } }
        item { GHAboutRow(Icons.Rounded.Info,       "Tentang DLavie",  "Tentang platform & tim DLavie")    { openUrl("https://drmacze.github.io/dlavie-web/#/about") } }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun GHAboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassCard)
        .border(1.dp, DivCol, RoundedCornerShape(14.dp)).clickable { sndSelect(); onClick() }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(GlassMid)
                .border(0.5.dp, GlassBrd, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = White70, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(desc,  color = Slate, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SlateDim, modifier = Modifier.size(17.dp))
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
                Icon(Icons.Rounded.NotificationsNone, null, tint = SlateDim, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(12.dp))
                Text("Belum ada notifikasi", color = Slate, fontSize = 15.sp)
                Text("Notifikasi dari DLavie akan muncul di sini", color = SlateDim, fontSize = 12.sp)
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
                    Icon(Icons.Rounded.FavoriteBorder, null, tint = SlateDim, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada game favorit", color = Slate, fontSize = 15.sp)
                    Text("Tambahkan dari menu ⋮ pada game card", color = SlateDim, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favGames) { g ->
                    GHGameListRow(g, context, onClick = { onGameDetail(g) }, onFav = { onToggleFav(g.packageName) }, isFav = true)
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
                Icon(Icons.Rounded.PeopleOutline, null, tint = SlateDim, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(12.dp))
                Text("Friends coming soon", color = Slate, fontSize = 15.sp)
                Text("Fitur teman akan hadir di update mendatang", color = SlateDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GHScrnHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(GlassCard)
            .border(1.dp, GlassBrd, CircleShape).clickable { onBack() },
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun GHGameListRow(game: GameItem, context: Context, onClick: () -> Unit, onFav: () -> Unit, isFav: Boolean) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassCard)
        .border(1.dp, DivCol, RoundedCornerShape(14.dp)).clickable { onClick() }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(game.coverGradient))) {
                if (game.coverImageRes != null)
                    androidx.compose.foundation.Image(painterResource(game.coverImageRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(game.title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(game.subtitle, color = Slate, fontSize = 11.sp)
                Text(game.sizeMb,   color = SlateDim, fontSize = 10.sp)
            }
            IconButton(onClick = onFav, modifier = Modifier.size(36.dp)) {
                Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null, tint = if (isFav) RedAlert else Slate, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// GAME DETAIL PAGE — fullscreen art + Play + metadata
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GHDetailPage(
    game: GameItem, context: Context, favorites: Set<String>,
    onBack: () -> Unit, onPlay: () -> Unit, onToggleFav: () -> Unit
) {
    val installed = ghInstalled(context, game.packageName)
    val isFav     = game.packageName in favorites

    val (statusColor, statusLabel) = when (game.serverStatus) {
        ServerStatus.ONLINE      -> GreenOnline to "Server Online"
        ServerStatus.MAINTENANCE -> OrangeWarn  to "Maintenance"
        ServerStatus.OFFLINE     -> RedAlert    to "Server Offline"
        ServerStatus.BUSY        -> OrangeWarn  to "Sinyal Lemah"
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        if (game.coverImageRes != null) {
            androidx.compose.foundation.Image(painterResource(game.coverImageRes), null,
                Modifier.fillMaxSize().blur(70.dp), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xCC000000), Color(0xF5000000)))))
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item {
                // Top bar
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(GlassCard)
                        .border(1.dp, GlassBrd, CircleShape).clickable { onBack() },
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    val brush = shimmerBrush()
                    Text(game.title, style = TextStyle(brush = brush, fontSize = 19.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(36.dp).clip(CircleShape).background(GlassCard)
                        .border(1.dp, GlassBrd, CircleShape).clickable { onToggleFav() },
                        contentAlignment = Alignment.Center) {
                        Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            "Fav", tint = if (isFav) RedAlert else White, modifier = Modifier.size(18.dp))
                    }
                }

                // Cover art
                Box(Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(22.dp))) {
                    if (game.coverImageRes != null) {
                        androidx.compose.foundation.Image(painterResource(game.coverImageRes), game.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)))
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Bg.copy(0.65f)))))
                    // Glass border overlay
                    Box(Modifier.fillMaxSize().border(1.dp, GlassBrd, RoundedCornerShape(22.dp)))
                }

                // Meta
                Column(Modifier.padding(20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(game.version, game.sizeMb, game.category, game.ageRating).filter { it.isNotEmpty() }.forEach { info ->
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(GlassCard)
                                .border(0.5.dp, DivCol, RoundedCornerShape(6.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp)) {
                                Text(info, color = Slate, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("DESKRIPSI", color = White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(game.description.ifEmpty { "Game mod DLavie." }, color = White70, fontSize = 13.sp)

                    if (game.features.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("FITUR", color = White30, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(6.dp))
                        game.features.forEach { f ->
                            Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("—", color = White30); Text(f, color = White70, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Server status glass card
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassCard)
                        .border(1.dp, DivCol, RoundedCornerShape(14.dp))) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                            Spacer(Modifier.width(8.dp))
                            Text(statusLabel, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            Text(game.developer, color = SlateDim, fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Play button — glass pill
                    Box(
                        Modifier.fillMaxWidth().height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (installed) GreenOnline.copy(0.18f) else White.copy(0.12f))
                            .border(1.dp, if (installed) GreenOnline.copy(0.50f) else White.copy(0.35f), RoundedCornerShape(16.dp))
                            .clickable { onPlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(if (installed) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                                null, tint = if (installed) GreenOnline else White, modifier = Modifier.size(22.dp))
                            Text(if (installed) "Play Game" else "Install Game",
                                color = if (installed) GreenOnline else White,
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}
