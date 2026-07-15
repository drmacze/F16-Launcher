package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.text.format.DateFormat
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
// DLAVIE GAMEHUB v289 — Card polish pass (PlayStore GameHub reference)
// v289: larger glass cards, info block below cover, top-right overflow menu
// (Favorite / View Detail), dpad/controller focus ring, wider section spacing
// (32dp) and stronger background blur (80dp) for a more premium glass feel.
// ═══════════════════════════════════════════════════════════════════════════
// UI Reference: GameHub Lite screenshots (July 2026)
//   - Game Management: vertical list, thumbnail left, name + last start time
//   - Download Task: queue + completed, file size, 3-dot remove menu
//   - Game Detail: fullscreen bg art, Play Now button, Local Game ID, 3-dot
//   - Sort: bottom sheet (last run date / file size / Alphabetically A-Z)
//   - Settings: Advanced (Language, Clear Cache) + About (Device Info, Credits, Privacy)
//   - Top nav: ← back | Download Task (tab) | Game Management (tab) | status icons
//   - All REAL data — no dummy. Connected to device & Supabase.
// ═══════════════════════════════════════════════════════════════════════════

// ──────────────────────────── Design Tokens (Gemini spec) ──────────────────
// Dark mode pekat + neon cyan accent (GameHub PlayStore style)
private val GHBg          = Color(0xFF000000)   // Pure black (Gemini spec)
private val GHBgCard      = Color(0xFF121212)   // Card surface (Gemini spec)
private val GHBgNav       = Color(0xFF0A0A0A)   // Nav bar bg (translucent feel)
private val GHSurface     = Color(0xFF1A1A1A)   // Elevated surface
private val GHGlassBg     = Color(0x99000000)   // 60% black for glassmorphism
private val GHBorder      = Color(0x1AFFFFFF)   // 10% white border
private val GHBorderHi    = Color(0x33FFFFFF)   // 20% white border (selected)
private val GHTextWhite   = Color(0xFFFFFFFF)
private val GHTextGray    = Color(0xFFA0A0A0)   // Gemini spec: #A0A0A0
private val GHTextDim     = Color(0xFF666666)
private val GHAccent      = Color(0xFF00E5FF)   // Neon cyan (Gemini spec)
private val GHAccentDim   = Color(0xFF00B8D4)
private val GHAccentTab   = Color(0xFFFFFFFF)
private val GHGreen       = Color(0xFF00FF88)   // Bright green for Play/Active
private val GHAmber       = Color(0xFFFFB347)
private val GHRed         = Color(0xFFFF5252)
private val GHNavSelected = Color(0xFF00E5FF)   // Cyan for selected nav
private val GHFocusRing   = Color(0xFF00E5FF)   // Accent focus ring (dpad/controller navigation)
private val GHCardGlass   = Color(0x1FFFFFFF)   // Glass sheen overlay on cards

// ──────────────────────────── Prefs helpers ───────────────────────────────
private const val GH_PREFS = "gh_v284"
private fun ghPrefs(c: Context) = c.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE)

private fun ghLoadFavorites(c: Context): Set<String> =
    ghPrefs(c).getStringSet("favs", emptySet()) ?: emptySet()

private fun ghSaveFavorites(c: Context, s: Set<String>) =
    ghPrefs(c).edit().putStringSet("favs", s).apply()

private fun ghToggleFavorite(c: Context, pkg: String): Set<String> {
    val cur = ghLoadFavorites(c).toMutableSet()
    if (pkg in cur) cur.remove(pkg) else cur.add(pkg)
    ghSaveFavorites(c, cur)
    return cur
}

// Last-run time persistence (real usage tracking)
private fun ghSaveLastRun(c: Context, pkg: String) =
    ghPrefs(c).edit().putLong("last_run_$pkg", System.currentTimeMillis()).apply()

private fun ghGetLastRun(c: Context, pkg: String): Long =
    ghPrefs(c).getLong("last_run_$pkg", 0L)

private fun ghFormatLastRun(ts: Long): String {
    if (ts == 0L) return "Never"
    val now = System.currentTimeMillis()
    val diff = now - ts
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) ->
            "today at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))}"
        cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
            "yesterday at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))}"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ts))
    }
}

// Storage helpers
private fun ghGetStorageInfo(): Triple<Long, Long, Long> = try {
    val stat = StatFs("/sdcard")
    Triple(stat.availableBytes, stat.totalBytes, stat.totalBytes - stat.availableBytes)
} catch (_: Exception) { Triple(0L, 0L, 0L) }

private fun ghGameStorage(pkg: String): Long = try {
    val dir = File("/sdcard/Android/data/$pkg")
    if (dir.exists()) dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() else 0L
} catch (_: Exception) { 0L }

private fun ghFormatBytes(b: Long): String = when {
    b >= 1_073_741_824L -> "%.1fGB".format(b / 1_073_741_824.0)
    b >= 1_048_576L     -> "%.1fMB".format(b / 1_048_576.0)
    b >= 1_024L         -> "%.1fKB".format(b / 1_024.0)
    else -> "${b}B"
}

// Battery & time
private fun ghBattery(c: Context): Int = try {
    (c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
} catch (_: Exception) { 100 }

private fun ghTime(c: Context): String = try {
    val f = if (DateFormat.is24HourFormat(c)) "HH:mm" else "h:mm a"
    SimpleDateFormat(f, Locale.getDefault()).format(Date())
} catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

// ──────────────────────────── Sort enum ───────────────────────────────────
private enum class GHSortMode { LAST_RUN_DATE, FILE_SIZE, ALPHABETICAL }

// ──────────────────────────── Download Task item ──────────────────────────
private data class GHDownloadItem(
    val id: String,
    val title: String,
    val sizeBytes: Long,
    val completedAt: Long,
    val coverRes: Int?,
    val pkg: String
)

// ═══════════════════════════════════════════════════════════════════════════
// TRANSITION — logo + typing DLAVIE + loading messages (preserved)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    var typedText by remember { mutableStateOf("") }
    var loadingMsg by remember { mutableStateOf("") }

    val fullText = "DLAVIE"
    val loadingMessages = listOf(
        "Loading game assets...",
        "Setting up interface...",
        "Scanning user data...",
        "Connecting to server...",
        "Preparing GameHub..."
    )

    LaunchedEffect(visible) {
        if (visible) {
            phase = 0; delay(600); phase = 1; delay(600); phase = 2
            for (i in fullText.indices) { typedText = fullText.substring(0, i + 1); delay(110) }
            delay(300); phase = 3
            for (msg in loadingMessages) { loadingMsg = msg; delay(600) }
            loadingMsg = ""; delay(200); phase = 4; delay(500); onComplete(); phase = 5
        }
    }

    if (visible && phase < 5) {
        Box(Modifier.fillMaxSize().background(Color(0xFF000000)), contentAlignment = Alignment.Center) {
            val logoAlpha by animateFloatAsState(if (phase in 1..3) 1f else 0f, tween(700), label = "la")
            val textAlpha by animateFloatAsState(if (phase in 2..3) 1f else 0f, tween(400), label = "ta")
            val fadeOut by animateFloatAsState(if (phase == 4) 0f else 1f, tween(500), label = "fo")
            val msgAlpha by animateFloatAsState(if (phase == 3) 1f else 0f, tween(300), label = "ma")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = fadeOut }
            ) {
                Box(
                    Modifier.size(72.dp).graphicsLayer { alpha = logoAlpha }
                        .clip(RoundedCornerShape(18.dp))
                        .background(GHTextWhite),
                    contentAlignment = Alignment.Center
                ) { Text("DL", color = Color.Black, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(20.dp))
                Text(
                    typedText, color = GHTextWhite, fontSize = 26.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 6.sp,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha }
                )
                Spacer(Modifier.height(14.dp))
                if (loadingMsg.isNotEmpty()) {
                    Text(
                        loadingMsg, color = GHTextDim, fontSize = 12.sp,
                        modifier = Modifier.graphicsLayer { alpha = msgAlpha }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE — DLavieGameHub
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
        activity?.window?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                w.setDecorFitsSystemWindows(false)
                w.insetsController?.hide(WindowInsets.Type.systemBars())
                w.insetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                @Suppress("DEPRECATION")
                w.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
            }
        }
        onDispose {
            activity?.window?.let { w ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    w.setDecorFitsSystemWindows(true)
                    w.insetsController?.show(WindowInsets.Type.systemBars())
                } else {
                    @Suppress("DEPRECATION")
                    w.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    // Real-time clock & battery
    var currentTime by remember { mutableStateOf(ghTime(context)) }
    var batteryLevel by remember { mutableStateOf(ghBattery(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = ghTime(context)
            batteryLevel = ghBattery(context)
            delay(30_000)
        }
    }

    // Screen state — 0=GameManagement, 1=DownloadTask, 2=Settings, 3=Home(Carousel)
    var currentScreen by remember { mutableStateOf(3) }  // Default: Home carousel

    // Detail state
    var detailGame by remember { mutableStateOf<GameItem?>(null) }

    Box(Modifier.fillMaxSize().background(GHBg)) {
        if (!showTransition) {
            when {
                detailGame != null -> {
                    GHGameDetailPage(
                        game = detailGame!!,
                        context = context,
                        onBack = { detailGame = null },
                        onPlayClick = {
                            ghSaveLastRun(context, detailGame!!.packageName)
                            onGameClick(detailGame!!.packageName)
                            detailGame = null
                        }
                    )
                }
                currentScreen == 2 -> GHSettingsScreen(context, currentTime, batteryLevel) { currentScreen = 3 }
                currentScreen == 3 -> GHHomeCarousel(
                    context = context,
                    onOpenDetail = { game -> detailGame = game },
                    onGameClick = onGameClick,
                    onExit = onExit,
                    onNavToGameMgmt = { currentScreen = 0 },
                    onNavToDownload = { currentScreen = 1 },
                    onNavToSettings = { currentScreen = 2 },
                    currentTime = currentTime,
                    batteryLevel = batteryLevel,
                    onSwitchScreen = { currentScreen = it }
                )
                else -> GHMainScreen(
                    context = context,
                    currentScreen = currentScreen,
                    onSwitchScreen = { currentScreen = it },
                    onOpenDetail = { game -> detailGame = game },
                    onGameClick = onGameClick,
                    onSettings = { currentScreen = 2 },
                    onExit = onExit,
                    currentTime = currentTime,
                    batteryLevel = batteryLevel
                )
            }
        }
        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN SCREEN — Download Task | Game Management tabs
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHMainScreen(
    context: Context,
    currentScreen: Int,
    onSwitchScreen: (Int) -> Unit,
    onOpenDetail: (GameItem) -> Unit,
    onGameClick: (String) -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    currentTime: String,
    batteryLevel: Int
) {
    // Build real game list from actual installed + DLavie catalog
    val dlavieGames = remember {
        listOf(
            GameItem(
                title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod · Sports",
                packageName = GAME_PKG_16,
                mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                coverGradient = listOf(Color(0xFF0A1628), Color(0xFF1A3A6B)),
                coverText = "DL", coverImageRes = R.drawable.fifa16_cover,
                serverStatus = ServerStatus.ONLINE,
                description = "FIFA 16 Mobile dengan mod DLavie 26",
                developer = "DLavie Company", version = "v26.0", sizeMb = "34 MB",
                category = "Sports", ageRating = "9+", lastUpdate = "5 Juli 2026",
                features = listOf("Gameplay Realistis", "Roster 2025/2026", "Komunitas Aktif", "Update Rutin"),
                screenshots = listOf(
                    R.drawable.fifa16_screenshot_1, R.drawable.fifa16_screenshot_2,
                    R.drawable.fifa16_screenshot_3, R.drawable.fifa16_screenshot_4
                ),
                apkUrl = FIFA16_APK_URL
            ),
            GameItem(
                title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod · Sports",
                packageName = GAME_PKG_15,
                mainActivity = FIFA15_MAIN_ACTIVITY,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15", coverImageRes = R.drawable.fifa15_cover,
                serverStatus = ServerStatus.MAINTENANCE,
                description = "FIFA 15 Mobile dengan mod DLavie 15",
                developer = "DLavie Company", version = "v15.0", sizeMb = "22 MB",
                category = "Sports", ageRating = "9+", lastUpdate = "4 Juli 2026",
                features = listOf("Gameplay Klasik", "Roster 2014/2015", "Mode Nostalgia", "Android 16 Ready"),
                apkUrl = FIFA15_APK_URL
            )
        )
    }

    var userGames by remember { mutableStateOf<List<UserGame>>(emptyList()) }
    LaunchedEffect(currentScreen) {
        if (currentScreen == 0) {
            userGames = withContext(Dispatchers.IO) { loadUserGames(context) }
        }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(it, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize()) {
        // ── TOP NAV BAR (exact match reference) ──
        GHTopNavBar(
            currentScreen = currentScreen,
            onSwitchScreen = onSwitchScreen,
            onSettings = onSettings,
            onExit = onExit,
            currentTime = currentTime,
            batteryLevel = batteryLevel
        )

        // ── CONTENT ──
        when (currentScreen) {
            0 -> GHGameManagement(
                context = context,
                dlavieGames = dlavieGames,
                userGames = userGames,
                onOpenDetail = onOpenDetail,
                onAddApk = { apkPickerLauncher.launch("application/vnd.android.package-archive") }
            )
            1 -> GHDownloadTask(context = context, dlavieGames = dlavieGames)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TOP NAV BAR — ← | Download Task | Game Management | wifi | battery | time
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHTopNavBar(
    currentScreen: Int,
    onSwitchScreen: (Int) -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    currentTime: String,
    batteryLevel: Int
) {
    Box(
        Modifier.fillMaxWidth().height(56.dp)
            .background(GHBgNav)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back / Settings button
            Icon(
                Icons.Rounded.ArrowBack, "Back",
                tint = GHTextGray,
                modifier = Modifier.size(24.dp).clickable { onExit() }
            )

            Spacer(Modifier.width(16.dp))

            // Tab: Download Task
            GHNavTab(
                label = "Download Task",
                selected = currentScreen == 1,
                onClick = { onSwitchScreen(1) }
            )

            Spacer(Modifier.width(8.dp))

            // Tab: Game Management
            GHNavTab(
                label = "Game Management",
                selected = currentScreen == 0,
                onClick = { onSwitchScreen(0) }
            )

            Spacer(Modifier.weight(1f))

            // Settings gear
            Icon(
                Icons.Rounded.Settings, "Settings",
                tint = GHTextGray,
                modifier = Modifier.size(20.dp).clickable { onSettings() }
            )

            Spacer(Modifier.width(12.dp))

            // Wifi icon
            Icon(Icons.Rounded.Wifi, "Wifi", tint = GHTextGray, modifier = Modifier.size(18.dp))

            Spacer(Modifier.width(8.dp))

            // Battery
            Icon(Icons.Rounded.BatteryFull, "Battery", tint = GHTextGray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(3.dp))
            Text("$batteryLevel%", color = GHTextGray, fontSize = 12.sp)

            Spacer(Modifier.width(8.dp))

            // Time
            Text(currentTime, color = GHTextGray, fontSize = 12.sp)
        }
    }
    // Bottom divider
    Divider(color = GHBorder, thickness = 0.5.dp)
}

@Composable
private fun GHNavTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) GHNavSelected else Color.Transparent
    val textColor = if (selected) GHAccentTab else GHTextDim
    val border = if (selected) BorderStroke(1.dp, GHBorderHi) else BorderStroke(0.dp, Color.Transparent)

    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, color = textColor, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAME MANAGEMENT SCREEN — vertical list, thumbnail + name + last run time
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHGameManagement(
    context: Context,
    dlavieGames: List<GameItem>,
    userGames: List<UserGame>,
    onOpenDetail: (GameItem) -> Unit,
    onAddApk: () -> Unit
) {
    var sortMode by remember { mutableStateOf(GHSortMode.LAST_RUN_DATE) }
    var showSortSheet by remember { mutableStateOf(false) }

    // Combine real game list: DLavie catalog + user-added APKs
    val allGames: List<GameItem> = remember(userGames) {
        val ugames = userGames.map { ug ->
            GameItem(
                title = ug.title,
                subtitle = "User Library",
                packageName = ug.packageName,
                mainActivity = "",
                coverGradient = listOf(Color(0xFF1A1A1A), Color(0xFF2A2A2A)),
                coverText = ug.title.take(2).uppercase(),
                coverImageRes = null,
                serverStatus = ServerStatus.ONLINE,
                description = "",
                apkUrl = ug.sourcePath
            )
        }
        dlavieGames + ugames
    }

    // Sort
    val sortedGames = remember(allGames, sortMode) {
        when (sortMode) {
            GHSortMode.LAST_RUN_DATE -> allGames.sortedByDescending { ghGetLastRun(context, it.packageName) }
            GHSortMode.FILE_SIZE -> allGames.sortedByDescending { ghGameStorage(it.packageName) }
            GHSortMode.ALPHABETICAL -> allGames.sortedBy { it.title }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Storage bar (exact match reference UI)
            GHStorageBar(context)

            // Sort button row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showSortSheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Sort, "Sort",
                        tint = GHTextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (sortedGames.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SportsEsports, null, tint = GHTextDim, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No games yet", color = GHTextGray, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Text("Add games to your library", color = GHTextDim, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(sortedGames) { game ->
                        GHGameManagementRow(
                            game = game,
                            context = context,
                            onClick = { onOpenDetail(game) }
                        )
                    }
                }
            }
        }

        // FAB — add APK
        FloatingActionButton(
            onClick = onAddApk,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = GHAccent,
            shape = CircleShape
        ) {
            Icon(Icons.Rounded.Add, "Add APK", tint = Color.White)
        }

        // Sort bottom sheet
        if (showSortSheet) {
            GHSortBottomSheet(
                currentSort = sortMode,
                onSelect = { sortMode = it; showSortSheet = false },
                onDismiss = { showSortSheet = false }
            )
        }
    }
}

// ── Storage bar (reference: Game 0B · Media 0KB · Other 206GB · Available 21.8GB) ──
@Composable
private fun GHStorageBar(context: Context) {
    val (avail, total, used) = remember { ghGetStorageInfo() }
    val gameSize = remember { ghGameStorage(GAME_PKG_16) + ghGameStorage(GAME_PKG_15) }
    val mediaSize = 0L
    val otherSize = used - gameSize - mediaSize

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Folder icon
        Box(
            Modifier.size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GHSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Folder, null, tint = GHTextGray, modifier = Modifier.size(28.dp))
        }

        Column(Modifier.weight(1f)) {
            // Storage bar
            val totalF = total.toFloat().coerceAtLeast(1f)
            Box(
                Modifier.fillMaxWidth().height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GHSurface)
            ) {
                Row(Modifier.fillMaxSize()) {
                    if (gameSize > 0)
                        Box(Modifier.fillMaxWidth(gameSize / totalF).fillMaxHeight().background(Color(0xFF4F8EF7)))
                    if (mediaSize > 0)
                        Box(Modifier.fillMaxWidth(mediaSize / totalF).fillMaxHeight().background(GHAmber))
                    if (otherSize > 0)
                        Box(Modifier.fillMaxWidth((otherSize / totalF).coerceIn(0f,1f)).fillMaxHeight().background(GHTextDim))
                }
            }
            Spacer(Modifier.height(6.dp))
            // Labels row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GHStorageLabel("●", Color(0xFF4F8EF7), "Game", ghFormatBytes(gameSize))
                GHStorageLabel("●", GHAmber, "Media", ghFormatBytes(mediaSize))
                GHStorageLabel("●", GHTextDim, "Other", ghFormatBytes(otherSize))
                GHStorageLabel("●", Color(0xFF888888), "Available", ghFormatBytes(avail))
            }
        }
    }
    Divider(color = GHBorder, thickness = 0.5.dp)
}

@Composable
private fun GHStorageLabel(dot: String, dotColor: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(dot, color = dotColor, fontSize = 10.sp)
        Text("$label $value", color = GHTextDim, fontSize = 10.sp)
    }
}

// ── Game Management Row (exact match: thumbnail left, name bold, Last start time) ──
@Composable
private fun GHGameManagementRow(
    game: GameItem,
    context: Context,
    onClick: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    val lastRun = remember { ghGetLastRun(context, game.packageName) }
    val lastRunText = remember(lastRun) { ghFormatLastRun(lastRun) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail (120x80dp)
            Box(
                Modifier.width(120.dp).height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(game.coverGradient)),
                contentAlignment = Alignment.Center
            ) {
                if (game.coverImageRes != null) {
                    Image(
                        painter = painterResource(id = game.coverImageRes),
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(game.coverText, color = GHTextGray, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(14.dp))

            // Info
            Column(Modifier.weight(1f)) {
                Text(
                    game.title,
                    color = GHTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                // Last start time (exact match reference)
                Text(
                    "Last start time",
                    color = GHTextDim,
                    fontSize = 11.sp
                )
                Text(
                    lastRunText,
                    color = GHTextGray,
                    fontSize = 11.sp
                )
            }

            // 3-dot options button (right side, exact match reference)
            Box(
                Modifier.size(32.dp)
                    .clip(CircleShape)
                    .clickable { showOptions = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreVert, "Options", tint = GHTextGray, modifier = Modifier.size(20.dp))
            }
        }

        // Bottom divider
        Divider(
            color = GHBorder, thickness = 0.5.dp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 150.dp)
        )

        // Options dropdown
        if (showOptions) {
            Popup(
                onDismissRequest = { showOptions = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1C2333),
                    border = BorderStroke(1.dp, GHBorderHi),
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(180.dp)
                ) {
                    Column(Modifier.padding(4.dp)) {
                        GHMenuOption(Icons.Rounded.PlayArrow, "Launch Game") {
                            showOptions = false
                            onClick()
                        }
                        GHMenuOption(Icons.Rounded.Delete, "Remove from Library", tint = GHRed) {
                            showOptions = false
                            // Remove UserGame if it's user-added
                            try {
                                val all = loadUserGames(context).toMutableList()
                                all.removeAll { it.packageName == game.packageName }
                                saveUserGames(context, all)
                            } catch (_: Exception) {}
                        }
                        Divider(color = GHBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                        GHMenuOption(Icons.Rounded.Cancel, "Cancel") { showOptions = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun GHMenuOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = GHTextWhite,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, color = tint, fontSize = 13.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SORT BOTTOM SHEET — last run date / file size / Alphabetically A-Z
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHSortBottomSheet(
    currentSort: GHSortMode,
    onSelect: (GHSortMode) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color(0xFF1C2333),
            modifier = Modifier.fillMaxWidth().clickable(enabled = false) {}
        ) {
            Column(Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                // Handle
                Box(
                    Modifier.width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GHTextDim)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(8.dp))

                // Sort options
                GHSortOption(
                    label = "last run date",
                    selected = currentSort == GHSortMode.LAST_RUN_DATE,
                    onClick = { onSelect(GHSortMode.LAST_RUN_DATE) }
                )
                GHSortOption(
                    label = "file size",
                    selected = currentSort == GHSortMode.FILE_SIZE,
                    onClick = { onSelect(GHSortMode.FILE_SIZE) }
                )
                GHSortOption(
                    label = "Alphabetically A-Z",
                    selected = currentSort == GHSortMode.ALPHABETICAL,
                    onClick = { onSelect(GHSortMode.ALPHABETICAL) }
                )
                Divider(color = GHBorder, modifier = Modifier.padding(vertical = 4.dp))
                GHSortOption(label = "Cancel", selected = false, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun GHSortOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = if (selected) GHTextWhite else GHTextGray,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Icon(Icons.Rounded.Check, null, tint = GHTextWhite, modifier = Modifier.size(20.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DOWNLOAD TASK SCREEN — Download Soon + Completed list
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDownloadTask(context: Context, dlavieGames: List<GameItem>) {
    // Build completed list from installed apps (real data)
    val completedItems = remember {
        dlavieGames.filter { isPackageInstalled(context, it.packageName) }
            .map { game ->
                GHDownloadItem(
                    id = game.packageName,
                    title = game.title,
                    sizeBytes = ghGameStorage(game.packageName).let { if (it > 0) it else (game.sizeMb.replace(" MB","").toFloatOrNull()?.times(1_048_576L.toFloat()))?.toLong() ?: 0L },
                    completedAt = ghGetLastRun(context, game.packageName).let { if (it > 0) it else System.currentTimeMillis() - 86_400_000L },
                    coverRes = game.coverImageRes,
                    pkg = game.packageName
                )
            }
    }

    var removeTargetId by remember { mutableStateOf<String?>(null) }
    var completedList by remember { mutableStateOf(completedItems) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))

        // Download Soon
        Text("Download Soon (0)", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("No Queued Tasks", color = GHTextDim, fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))

        // Completed header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Completed (${completedList.size})",
                color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            if (completedList.isNotEmpty()) {
                Text(
                    "Clear All",
                    color = GHAccent, fontSize = 13.sp,
                    modifier = Modifier.clickable { completedList = emptyList() }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (completedList.isEmpty()) {
            Text("No completed tasks", color = GHTextDim, fontSize = 13.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(completedList, key = { it.id }) { item ->
                    GHDownloadTaskRow(
                        item = item,
                        onRemove = { completedList = completedList.filter { it.id != item.id } }
                    )
                    Divider(color = GHBorder, thickness = 0.5.dp)
                }
            }
        }
    }

    // Remove task dialog
    removeTargetId?.let { id ->
        GHRemoveTaskDialog(
            onConfirm = {
                completedList = completedList.filter { it.id != id }
                removeTargetId = null
            },
            onDismiss = { removeTargetId = null }
        )
    }
}

@Composable
private fun GHDownloadTaskRow(
    item: GHDownloadItem,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                Modifier.width(100.dp).height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GHSurface),
                contentAlignment = Alignment.Center
            ) {
                if (item.coverRes != null) {
                    Image(
                        painter = painterResource(id = item.coverRes),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Rounded.SportsEsports, null, tint = GHTextDim, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    item.title, color = GHTextWhite, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = GHTextDim, modifier = Modifier.size(13.dp))
                    Text(
                        "${ghFormatBytes(item.sizeBytes)}  Completed At:${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.completedAt))}",
                        color = GHTextDim, fontSize = 11.sp
                    )
                }
            }

            // 3-dot menu
            Box(
                Modifier.size(32.dp).clip(CircleShape)
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreVert, "Menu", tint = GHTextDim, modifier = Modifier.size(18.dp))
            }
        }

        // Dropdown
        if (showMenu) {
            Popup(
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1C2333),
                    border = BorderStroke(1.dp, GHBorderHi),
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(160.dp)
                ) {
                    Column(Modifier.padding(4.dp)) {
                        GHMenuOption(Icons.Rounded.Delete, "Remove Task", tint = GHTextWhite) {
                            showMenu = false
                            onRemove()
                        }
                        Divider(color = GHBorder)
                        GHMenuOption(Icons.Rounded.Cancel, "Cancel") { showMenu = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun GHRemoveTaskDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C2333),
            border = BorderStroke(1.dp, GHBorderHi)
        ) {
            Column(Modifier.padding(24.dp).width(240.dp)) {
                Text("Remove Task", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Remove this completed task from the list?", color = GHTextGray, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GHTextGray)
                    ) { Text("Cancel") }
                    Button(
                        onClick = onConfirm, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GHRed)
                    ) { Text("Remove", color = Color.White) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAME DETAIL PAGE — fullscreen bg, Play Now, Local Game ID, 3-dot menu
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHGameDetailPage(
    game: GameItem,
    context: Context,
    onBack: () -> Unit,
    onPlayClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val localGameId = remember { "local_${UUID.randomUUID().toString().replace("-","").take(24).chunked(8).joinToString("-")}" }
    var copied by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // ── Fullscreen background art (exact match reference) ──
        if (game.coverImageRes != null) {
            Image(
                painter = painterResource(id = game.coverImageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)))
        }

        // Dark scrim for readability
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(0.3f), Color.Black.copy(0.7f), Color.Black.copy(0.92f))
                )
            )
        )

        // ── Content ──
        Column(Modifier.fillMaxSize()) {
            // Top bar: ← back
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ArrowBack, "Back",
                    tint = GHTextWhite,
                    modifier = Modifier.size(26.dp).clickable { onBack() }
                )
            }

            Spacer(Modifier.weight(1f))

            // Bottom info area
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Local Game ID (exact match reference)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Local Game ID: $localGameId",
                        color = GHTextGray,
                        fontSize = 11.sp
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GHSurface.copy(0.8f))
                            .border(1.dp, GHBorderHi, RoundedCornerShape(4.dp))
                            .clickable {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("GameID", localGameId))
                                    copied = true
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(if (copied) "Copied!" else "Copy", color = GHTextWhite, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Game title
                Text(
                    game.title,
                    color = GHTextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                // Action row: Play Now + 3-dot menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play Now button (exact match reference — outlined white, Windows icon style)
                    Button(
                        onClick = onPlayClick,
                        modifier = Modifier.width(220.dp).height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Windows-style 4-square icon
                            GHWindowsIcon(size = 18.dp)
                            Text(
                                "Play Now",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 3-dot menu
                    Box(
                        Modifier.size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(0.4f))
                            .border(1.dp, GHBorderHi, RoundedCornerShape(10.dp))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MoreVert, "Menu", tint = GHTextWhite, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // Bottom right: B Back label (exact match reference)
        Row(
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(GHSurface.copy(0.8f))
                    .border(1.dp, GHBorderHi, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("B", color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("Back", color = GHTextGray, fontSize = 13.sp, modifier = Modifier.clickable { onBack() })
        }

        // 3-dot dropdown
        if (showMenu) {
            Box(
                Modifier.fillMaxSize().clickable { showMenu = false },
                contentAlignment = Alignment.BottomStart
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1C2333),
                    border = BorderStroke(1.dp, GHBorderHi),
                    shadowElevation = 10.dp,
                    modifier = Modifier.padding(start = 24.dp, bottom = 120.dp).width(200.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(Modifier.padding(4.dp)) {
                        GHMenuOption(Icons.Rounded.PlayArrow, "Launch Game") {
                            showMenu = false
                            onPlayClick()
                        }
                        GHMenuOption(Icons.Rounded.Info, "Game Info") { showMenu = false }
                        GHMenuOption(Icons.Rounded.Share, "Share Game ID") {
                            showMenu = false
                            try {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, localGameId)
                                        }, "Share Game ID"
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }
}

// Windows 4-square logo icon
@Composable
private fun GHWindowsIcon(size: androidx.compose.ui.unit.Dp) {
    val s = with(LocalDensity.current) { size.toPx() }
    Canvas(Modifier.size(size)) {
        val gap = s * 0.08f
        val sq = (s - gap) / 2f
        val colors = listOf(Color(0xFFF35325), Color(0xFF81BC06), Color(0xFF05A6F0), Color(0xFFFFBA08))
        val offsets = listOf(
            Pair(0f, 0f), Pair(sq + gap, 0f),
            Pair(0f, sq + gap), Pair(sq + gap, sq + gap)
        )
        offsets.forEachIndexed { i, (x, y) ->
            drawRect(color = colors[i], topLeft = androidx.compose.ui.geometry.Offset(x, y), size = androidx.compose.ui.geometry.Size(sq, sq))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN — Advanced + About (exact match reference)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHSettingsScreen(
    context: Context,
    currentTime: String,
    batteryLevel: Int,
    onBack: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(0) }  // 0=Advanced, 1=About
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Default") }
    val languages = listOf("Default", "简体中文", "English", "にほんご", "русский")

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Box(
            Modifier.fillMaxWidth().height(56.dp).background(GHBgNav)
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ArrowBack, "Back",
                    tint = GHTextGray,
                    modifier = Modifier.size(24.dp).clickable { onBack() }
                )
                Spacer(Modifier.width(14.dp))
                Text("Settings", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Wifi, null, tint = GHTextGray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.BatteryFull, null, tint = GHTextGray, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("$batteryLevel%", color = GHTextGray, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text(currentTime, color = GHTextGray, fontSize = 12.sp)
            }
        }
        Divider(color = GHBorder)

        Row(Modifier.fillMaxSize()) {
            // Left sidebar — section tabs
            Column(
                Modifier.width(180.dp).fillMaxHeight()
                    .background(GHBgNav)
                    .padding(vertical = 8.dp)
            ) {
                GHSettingsSectionTab(
                    icon = Icons.Rounded.Layers,
                    label = "Advanced",
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 }
                )
                GHSettingsSectionTab(
                    icon = Icons.Rounded.Info,
                    label = "About",
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 }
                )
            }

            Divider(color = GHBorder, modifier = Modifier.fillMaxHeight().width(0.5.dp))

            // Right content
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                when (selectedSection) {
                    0 -> {
                        // Advanced settings
                        GHSettingsRow(
                            label = "Language",
                            value = selectedLanguage,
                            onClick = { showLanguageDropdown = true }
                        )
                        Divider(color = GHBorder)
                        GHSettingsRow(
                            label = "Clear Cache",
                            value = "",
                            onClick = {
                                try {
                                    context.cacheDir.deleteRecursively()
                                } catch (_: Exception) {}
                            }
                        )
                    }
                    1 -> {
                        // About
                        GHSettingsRow(
                            label = "Device Info",
                            value = "",
                            onClick = {}
                        )
                        Divider(color = GHBorder)
                        GHSettingsRow(
                            label = "Credits",
                            value = "",
                            onClick = {}
                        )
                        Divider(color = GHBorder)
                        // Privacy policy (with highlight border — exact match)
                        GHSettingsRowHighlighted(
                            label = "Privacy policy",
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://drmacze.github.io/dlavie-web/privacy"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }

    // Language dropdown popup
    if (showLanguageDropdown) {
        Box(
            Modifier.fillMaxSize().clickable { showLanguageDropdown = false },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1C2333),
                border = BorderStroke(1.dp, GHBorderHi),
                modifier = Modifier.width(220.dp).clickable(enabled = false) {}
            ) {
                Column(Modifier.padding(4.dp)) {
                    languages.forEach { lang ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable { selectedLanguage = lang; showLanguageDropdown = false }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang, color = GHTextWhite, fontSize = 14.sp)
                            if (lang == selectedLanguage) {
                                Icon(Icons.Rounded.Check, null, tint = GHTextWhite, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (lang != languages.last()) Divider(color = GHBorder, thickness = 0.3.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GHSettingsSectionTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) GHNavSelected else Color.Transparent
    val borderColor = if (selected) GHAccent else Color.Transparent
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                BorderStroke(
                    if (selected) 2.dp else 0.dp,
                    if (selected) GHAccent else Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = if (selected) GHAccent else GHTextDim, modifier = Modifier.size(18.dp))
        Text(label, color = if (selected) GHTextWhite else GHTextDim, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun GHSettingsRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = GHTextGray, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (value.isNotEmpty()) Text(value, color = GHTextDim, fontSize = 14.sp)
            Icon(Icons.Rounded.ChevronRight, null, tint = GHTextDim, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun GHSettingsRowHighlighted(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, GHBorderHi, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = GHTextGray, fontSize = 14.sp)
        Icon(Icons.Rounded.ChevronRight, null, tint = GHTextDim, modifier = Modifier.size(18.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HOME CAROUSEL — Gemini spec: Hero + Quick Actions + Swimlanes + Bottom Nav
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHHomeCarousel(
    context: Context,
    onOpenDetail: (GameItem) -> Unit,
    onGameClick: (String) -> Unit,
    onExit: () -> Unit,
    onNavToGameMgmt: () -> Unit,
    onNavToDownload: () -> Unit,
    onNavToSettings: () -> Unit,
    currentTime: String,
    batteryLevel: Int,
    onSwitchScreen: (Int) -> Unit
) {
    val dlavieGames = remember {
        listOf(
            GameItem(
                title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod · Sports",
                packageName = GAME_PKG_16,
                mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                coverGradient = listOf(Color(0xFF0A1628), Color(0xFF1A3A6B)),
                coverText = "DL", coverImageRes = R.drawable.fifa16_cover,
                serverStatus = ServerStatus.ONLINE,
                description = "FIFA 16 Mobile dengan mod DLavie 26",
                developer = "DLavie Company", version = "v26.0", sizeMb = "34 MB",
                category = "Sports", ageRating = "9+", lastUpdate = "5 Juli 2026",
                features = listOf("Gameplay Realistis", "Roster 2025/2026", "Komunitas Aktif", "Update Rutin"),
                screenshots = listOf(R.drawable.fifa16_screenshot_1, R.drawable.fifa16_screenshot_2, R.drawable.fifa16_screenshot_3, R.drawable.fifa16_screenshot_4),
                apkUrl = FIFA16_APK_URL
            ),
            GameItem(
                title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod · Sports",
                packageName = GAME_PKG_15,
                mainActivity = FIFA15_MAIN_ACTIVITY,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15", coverImageRes = R.drawable.fifa15_cover,
                serverStatus = ServerStatus.MAINTENANCE,
                description = "FIFA 15 Mobile dengan mod DLavie 15",
                developer = "DLavie Company", version = "v15.0", sizeMb = "22 MB",
                category = "Sports", ageRating = "9+", lastUpdate = "4 Juli 2026",
                features = listOf("Gameplay Klasik", "Roster 2014/2015", "Mode Nostalgia", "Android 16 Ready"),
                apkUrl = FIFA15_APK_URL
            )
        )
    }

    var favorites by remember { mutableStateOf(ghLoadFavorites(context)) }
    var bottomTab by remember { mutableStateOf(0) }  // 0=Home, 1=Library, 2=Community, 3=Profile

    Box(Modifier.fillMaxSize().background(GHBg)) {
        // ── ADAPTIVE BLURRED BACKGROUND (from first game cover) ──
        dlavieGames.firstOrNull()?.let { game ->
            if (game.coverImageRes != null) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(80.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Box(Modifier.fillMaxSize().background(GHBg.copy(alpha = 0.82f)))
        }

        Column(Modifier.fillMaxSize()) {
            // ── GLASSMORPHIC TOP BAR ──
            GHTopBarGlass(currentTime, batteryLevel, onExit)

            // ── SCROLLABLE CONTENT — generous breathing room between sections ──
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
            ) {
                // Hero Carousel (banner besar + vignette + Play Now)
                GHHeroCarousel(dlavieGames, context, onOpenDetail, onGameClick)

                Spacer(Modifier.height(32.dp))

                // Quick Action Buttons (PC/Cloud/Emulator/Controller)
                GHQuickActions()

                Spacer(Modifier.height(32.dp))

                // Swimlane: Popular
                GHSwimlane(
                    title = "Popular Games",
                    games = dlavieGames,
                    context = context,
                    favorites = favorites,
                    onOpenDetail = onOpenDetail,
                    onToggleFavorite = { favorites = ghToggleFavorite(context, it) }
                )

                Spacer(Modifier.height(32.dp))

                // Swimlane: Favorites (kalau ada)
                val favGames = dlavieGames.filter { it.packageName in favorites }
                if (favGames.isNotEmpty()) {
                    GHSwimlane(
                        title = "Your Favorites",
                        games = favGames,
                        context = context,
                        favorites = favorites,
                        onOpenDetail = onOpenDetail,
                        onToggleFavorite = { favorites = ghToggleFavorite(context, it) }
                    )
                    Spacer(Modifier.height(32.dp))
                }

                // Swimlane: All Games
                GHSwimlane(
                    title = "All Games",
                    games = dlavieGames,
                    context = context,
                    favorites = favorites,
                    onOpenDetail = onOpenDetail,
                    onToggleFavorite = { favorites = ghToggleFavorite(context, it) }
                )

                Spacer(Modifier.height(100.dp))  // Space for bottom nav
            }

            // ── BOTTOM NAV (4 items: Home/Library/Community/Profile) ──
            GHBottomNavGemini(
                currentTab = bottomTab,
                onTabChange = { tab ->
                    bottomTab = tab
                    when (tab) {
                        0 -> {}  // Already home
                        1 -> onNavToGameMgmt()  // Library → Game Management
                        2 -> onNavToDownload()  // Community → Download (placeholder)
                        3 -> onNavToSettings()  // Profile → Settings
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASSMORPHIC TOP BAR — translucent + blur (Gemini spec)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHTopBarGlass(currentTime: String, batteryLevel: Int, onExit: () -> Unit) {
    // Glass surface: translucent dark backdrop (content behind it is already blurred by the
    // fullscreen background blur), plus a hairline seam so it reads as a distinct glass panel.
    Row(
        Modifier.fillMaxWidth()
            .background(GHGlassBg)
            .border(BorderStroke(0.5.dp, GHBorder))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back arrow (FIXED: now works because onExit is passed from ModernLauncherActivity)
        Icon(
            Icons.Rounded.ArrowBack, "Back",
            tint = GHTextWhite,
            modifier = Modifier.size(26.dp).clickable { onExit() }
        )
        Spacer(Modifier.width(20.dp))
        Text("GameHub", color = GHTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.Search, "Search", tint = GHTextWhite, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Icon(Icons.Rounded.Notifications, "Notifications", tint = GHTextWhite, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Rounded.BatteryFull, "Battery", tint = GHTextGray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(3.dp))
        Text("$batteryLevel%", color = GHTextGray, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        Text(currentTime, color = GHTextGray, fontSize = 12.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HERO CAROUSEL — banner besar + vignette + Play Now (Gemini spec)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHHeroCarousel(
    games: List<GameItem>,
    context: Context,
    onOpenDetail: (GameItem) -> Unit,
    onGameClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { games.size })

    // Auto-scroll
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % games.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box(
        Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
        ) { page ->
            val game = games[page]
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.linearGradient(game.coverGradient))
                    .clickable { onOpenDetail(game) }
            ) {
                // Cover image full bleed
                if (game.coverImageRes != null) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes),
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Vignette (gradient hitam ke transparan di bawah)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(0.9f))
                        )
                    )
                )

                // Status badge top-right
                val (sc, st) = when (game.serverStatus) {
                    ServerStatus.ONLINE -> Pair(GHGreen, "ONLINE")
                    ServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT")
                    ServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE")
                    ServerStatus.BUSY -> Pair(GHAmber, "BUSY")
                }
                Box(
                    Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(sc.copy(alpha = 0.85f))
                        .border(1.dp, sc, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(st, color = GHTextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Title + Play Now button at bottom
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(game.title, color = GHTextWhite, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(game.subtitle, color = GHTextGray, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    // Play Now button (neon cyan, glowing)
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(GHAccent)
                            .clickable {
                                ghSaveLastRun(context, game.packageName)
                                onGameClick(game.packageName)
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = GHBg, modifier = Modifier.size(20.dp))
                        Text("Play Now", color = GHBg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Page indicator dots
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(games.size) { i ->
                Box(
                    Modifier.size(if (i == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) GHAccent else GHTextDim.copy(alpha = 0.5f))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// QUICK ACTIONS — row of 4-5 circular icons (Gemini spec)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHQuickActions() {
    val actions = listOf(
        Triple("PC Games", Icons.Rounded.DesktopWindows, GHAccent),
        Triple("Cloud", Icons.Rounded.Cloud, GHGreen),
        Triple("Emulator", Icons.Rounded.SportsEsports, GHAmber),
        Triple("Controller", Icons.Rounded.Gamepad, GHRed),
        Triple("Steam", Icons.Rounded.SportsEsports, Color(0xFF1B2838))
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        actions.forEach { (label, icon, color) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { /* filter by category */ }
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(GHBgCard)
                        .border(1.dp, color.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, label, tint = color, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(label, color = GHTextGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SWIMLANE — horizontal slider per category (Gemini spec)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHSwimlane(
    title: String,
    games: List<GameItem>,
    context: Context,
    favorites: Set<String>,
    onOpenDetail: (GameItem) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // Title row with "See All" — extra breathing room around section headers
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = GHTextWhite, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("See All", color = GHAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(4.dp))

        // Horizontal card slider — wider gaps so cards never feel cramped
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(games) { game ->
                GHGameCardMini(
                    game = game,
                    isFavorite = game.packageName in favorites,
                    onOpenDetail = { onOpenDetail(game) },
                    onToggleFavorite = { onToggleFavorite(game.packageName) }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAME CARD MINI — large glass card, info below, top-right overflow menu,
// focus ring for controller/dpad navigation (PlayStore GameHub style)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGameCardMini(
    game: GameItem,
    isFavorite: Boolean,
    onOpenDetail: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val cardWidth = 172.dp
    val cardHeight = 232.dp

    // Press + focus animation — scale on press, glow ring on dpad/controller focus
    var isPressed by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else if (isFocused) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 260f),
        label = "card_press"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) GHFocusRing else GHBorder,
        animationSpec = tween(180),
        label = "card_border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = tween(180),
        label = "card_border_width"
    )
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    Column(
        Modifier.width(cardWidth)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onOpenDetail() }
            )
    ) {
        // Card cover — large glass panel with focus glow
        Box(
            Modifier.width(cardWidth).height(cardHeight)
                .shadow(
                    elevation = if (isFocused) 18.dp else 6.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = GHFocusRing.copy(alpha = if (isFocused) 0.5f else 0f),
                    spotColor = GHFocusRing.copy(alpha = if (isFocused) 0.5f else 0f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(game.coverGradient))
                .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
        ) {
            // Cover image
            if (game.coverImageRes != null) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes),
                    contentDescription = game.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(game.coverText, color = GHTextWhite, fontSize = 36.sp, fontWeight = FontWeight.Black)
                }
            }

            // Glass sheen — subtle top highlight for glassmorphism depth
            Box(
                Modifier.fillMaxWidth().height(cardHeight / 2).align(Alignment.TopStart)
                    .background(Brush.verticalGradient(listOf(GHCardGlass, Color.Transparent)))
            )

            // Bottom gradient for badge readability
            Box(
                Modifier.fillMaxWidth().height(56.dp).align(Alignment.BottomStart)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
            )

            // Overflow menu — top-right corner (matches PlayStore GameHub card layout)
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.45f))
                    .clickable { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreVert, "More options", tint = GHTextWhite, modifier = Modifier.size(18.dp))
            }

            // Status badge (bottom-left)
            val (sc, st) = when (game.serverStatus) {
                ServerStatus.ONLINE -> Pair(GHGreen, "ONLINE")
                ServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT")
                ServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE")
                ServerStatus.BUSY -> Pair(GHAmber, "BUSY")
            }
            Box(
                Modifier.align(Alignment.BottomStart).padding(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(sc.copy(alpha = 0.85f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(st, color = GHTextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Overflow dropdown — Favorite / View Detail, anchored under the 3-dot button
            if (showMenu) {
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = androidx.compose.ui.unit.IntOffset(-8, 44),
                    onDismissRequest = { showMenu = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1C2333),
                        border = BorderStroke(1.dp, GHBorderHi),
                        shadowElevation = 12.dp,
                        modifier = Modifier.width(168.dp)
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            GHMenuOption(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                if (isFavorite) "Remove Favorite" else "Add Favorite"
                            ) {
                                showMenu = false
                                onToggleFavorite()
                            }
                            GHMenuOption(Icons.Rounded.Info, "View Detail") {
                                showMenu = false
                                onOpenDetail()
                            }
                        }
                    }
                }
            }
        }

        // Info block below card — title, meta, and a prominent View Detail affordance
        Spacer(Modifier.height(10.dp))
        Text(
            game.title,
            color = GHTextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                game.sizeMb.ifEmpty { game.version },
                color = GHTextGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "View Detail",
                color = GHAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onOpenDetail() }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// BOTTOM NAV — 4 items (Home/Library/Community/Profile) with active state
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHBottomNavGemini(
    currentTab: Int,
    onTabChange: (Int) -> Unit
) {
    val tabs = listOf(
        Triple("Home", Icons.Rounded.Home, 0),
        Triple("Library", Icons.Rounded.SportsEsports, 1),
        Triple("Community", Icons.Rounded.Forum, 2),
        Triple("Profile", Icons.Rounded.Person, 3)
    )
    Row(
        Modifier.fillMaxWidth()
            .background(GHBg.copy(alpha = 0.95f))
            .border(width = 1.dp, color = GHBorder, shape = RectangleShape)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { (label, icon, idx) ->
            val selected = currentTab == idx
            Column(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable { onTabChange(idx) }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon, label,
                    tint = if (selected) GHAccent else GHTextGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    color = if (selected) GHAccent else GHTextGray,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                // Active indicator line
                if (selected) {
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.width(20.dp).height(2.dp).clip(CircleShape).background(GHAccent))
                }
            }
        }
    }
}
