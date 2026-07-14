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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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
// DLAVIE GAMEHUB v282 — PlayStore-style with adaptive bg + glassmorphism
// ═══════════════════════════════════════════════════════════════════════════
// Match GameHub PlayStore reference:
//   - Adaptive blurred background from focused game cover
//   - Glassmorphic cards (blur + semi-transparent)
//   - Larger cards (200x280dp) with View Detail + 3-dot menu
//   - Platform label dynamic: .apk→Android, .ipa→iPhone, .exe→Windows
//   - Favorite system with persistence + sidebar tab
//   - Uses REAL GameItem + GameDetailScreen (NOT dummy)
//   - Click card → onGameClick → GameDetailScreen (preserved from v277)
// ═══════════════════════════════════════════════════════════════════════════

// ── Design tokens ──
private val GHBgBlack = Color(0xFF000000)
private val GHOverlayDark = Color(0xCC000000)
private val GHGlassBg = Color(0x801A1A1A)         // 50% opacity dark gray
private val GHGlassBgHi = Color(0xA0222222)       // 63% opacity
private val GHGlassBorder = Color(0x33FFFFFF)     // 20% white
private val GHGlassBorderHi = Color(0x66FFFFFF)   // 40% white
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextGray = Color(0xFFAAAAAA)
private val GHTextDim = Color(0xFF666666)
private val GHAccent = Color(0xFF00E5FF)          // Cyan (GameHub PlayStore accent)
private val GHGreen = Color(0xFF4CAF50)
private val GHAmber = Color(0xFFFFC107)
private val GHRed = Color(0xFFFF5252)
private val GHPillBg = Color(0x40FFFFFF)          // 25% white for pills

// ── Favorite persistence ──
private const val GH_PREFS = "gh_favorites"
private fun ghLoadFavorites(context: Context): Set<String> = try {
    context.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE).getStringSet("fav_pkgs", emptySet()) ?: emptySet()
} catch (_: Exception) { emptySet() }

private fun ghSaveFavorites(context: Context, favs: Set<String>) = try {
    context.getSharedPreferences(GH_PREFS, Context.MODE_PRIVATE).edit().putStringSet("fav_pkgs", favs).apply()
} catch (_: Exception) {}

private fun ghToggleFavorite(context: Context, packageName: String): Set<String> {
    val current = ghLoadFavorites(context).toMutableSet()
    if (packageName in current) current.remove(packageName) else current.add(packageName)
    ghSaveFavorites(context, current)
    return current
}

// ── Platform detection from file extension ──
private enum class GHPlatform(val label: String, val color: Color) {
    ANDROID("Android", Color(0xFF3DDC84)),   // Android green
    IOS("iPhone", Color(0xFF007AFF)),        // iOS blue
    WINDOWS("Windows", Color(0xFF00A4EF)),   // Windows blue
    MACOS("macOS", Color(0xFF999999)),
    LINUX("Linux", Color(0xFFFCC624)),
    UNKNOWN("Other", Color(0xFF888888));

    companion object {
        fun fromUrl(url: String): GHPlatform {
            val lower = url.lowercase()
            return when {
                lower.endsWith(".apk") || "android" in lower -> ANDROID
                lower.endsWith(".ipa") || "ios" in lower || "iphone" in lower -> IOS
                lower.endsWith(".exe") || "windows" in lower -> WINDOWS
                lower.endsWith(".dmg") || "macos" in lower || "darwin" in lower -> MACOS
                lower.endsWith(".deb") || "linux" in lower -> LINUX
                else -> UNKNOWN
            }
        }
        fun fromPackage(packageName: String): GHPlatform {
            // Android packages always have dots and look like com.xxx
            return if (packageName.contains(".") && !packageName.contains(" ")) ANDROID else UNKNOWN
        }
    }
}

// ── Helpers (use existing from PlayStoreGameHub.kt) ──
private fun ghGetBatteryLevel(context: Context): Int = try {
    (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
} catch (_: Exception) { 100 }

private fun ghGetCurrentTime(context: Context): String = try {
    val is24 = DateFormat.is24HourFormat(context)
    SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date())
} catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

private fun ghGetStorageInfo(): Pair<Long, Long> = try {
    val stat = StatFs("/sdcard"); Pair(stat.availableBytes, stat.totalBytes)
} catch (_: Exception) { Pair(0L, 0L) }

private fun ghFormatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1fGB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1fMB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1fKB".format(bytes / 1_000.0)
    else -> "${bytes}B"
}

// ═══════════════════════════════════════════════════════════════════════════
// TRANSITION — hexagon logo + typing DLAVIE + loading messages
// ═══════════════════════════════════════════════════════════════════════════

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
            phase = 0; delay(800); phase = 1; delay(800); phase = 2
            for (i in fullText.indices) { typedText = fullText.substring(0, i + 1); delay(120) }
            delay(400); phase = 3
            for (msg in loadingMessages) { loadingMsg = msg; delay(700) }
            loadingMsg = ""; delay(300); phase = 4; delay(600); onComplete(); phase = 5
        }
    }

    if (visible && phase < 5) {
        Box(Modifier.fillMaxSize().background(GHBgBlack), contentAlignment = Alignment.Center) {
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
                ) { Text("DL", color = GHBgBlack, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(20.dp))
                Text(typedText, color = GHTextWhite, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, modifier = Modifier.graphicsLayer { this.alpha = textAlpha })
                Spacer(Modifier.height(16.dp))
                if (loadingMsg.isNotEmpty()) {
                    Text(loadingMsg, color = GHTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.graphicsLayer { this.alpha = msgAlpha })
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN COMPOSABLE — uses REAL GameItem + onGameClick → GameDetailScreen
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(
    onExit: () -> Unit = {},
    onNav: (Page) -> Unit = {},
    onGameClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTransition by remember { mutableStateOf(true) }

    // ── Immersive mode ──
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
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
            }
        }
        onDispose {
            activity?.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(true)
                    window.insetsController?.show(WindowInsets.Type.systemBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    var currentTime by remember { mutableStateOf(ghGetCurrentTime(context)) }
    var batteryLevel by remember { mutableStateOf(ghGetBatteryLevel(context)) }
    LaunchedEffect(Unit) { while (true) { currentTime = ghGetCurrentTime(context); batteryLevel = ghGetBatteryLevel(context); delay(30_000) } }

    var currentScreen by remember { mutableStateOf(0) }  // 0=Home, 1=Download, 2=Settings, 3=Favorite
    var showDrawer by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }  // 0=DLavie, 1=My Library, 2=Favorite
    var favorites by remember { mutableStateOf(ghLoadFavorites(context)) }

    // ── Use REAL GameItem from GameItem.kt (NOT dummy) ──
    val dlavieGames = remember {
        listOf(
            GameItem(
                title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod",
                packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                coverText = "DL", coverImageRes = R.drawable.fifa16_cover,
                serverStatus = ServerStatus.ONLINE,
                description = "FIFA 16 Mobile dengan mod DLavie 26",
                version = "v26.0", sizeMb = "34 MB", apkUrl = FIFA16_APK_URL
            ),
            GameItem(
                title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod",
                packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15", coverImageRes = R.drawable.fifa15_cover,
                serverStatus = ServerStatus.MAINTENANCE,
                description = "FIFA 15 Mobile dengan mod DLavie 15",
                version = "v15.0", sizeMb = "22 MB", apkUrl = FIFA15_APK_URL
            )
        )
    }

    var userGames by remember { mutableStateOf<List<UserGame>>(emptyList()) }
    LaunchedEffect(selectedTab, currentScreen) {
        if (selectedTab == 1) userGames = withContext(Dispatchers.IO) { loadUserGames(context) }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { try { context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) {} }
    }

    // ── Adaptive background: track focused game cover ──
    var focusedGame by remember { mutableStateOf<GameItem?>(dlavieGames.firstOrNull()) }

    Box(
        Modifier.fillMaxSize().background(GHBgBlack)
    ) {
        // ── ADAPTIVE BLURRED BACKGROUND (from focused game cover) ──
        focusedGame?.let { game ->
            AdaptiveGameBackground(game)
        }

        if (!showTransition) {
            when (currentScreen) {
                0 -> GHHomeScreen(
                    context, scope, dlavieGames, userGames, favorites,
                    selectedTab, { selectedTab = it },
                    onGameClick,  // ← trigger GameDetailScreen
                    apkPickerLauncher, currentTime, batteryLevel,
                    { showDrawer = true }, { onExit() },
                    { favs -> favorites = favs },  // favorite toggle callback
                    { game -> focusedGame = game }  // focused game change
                )
                1 -> GHDownloadScreen(context) { currentScreen = 0 }
                2 -> GHSettingsScreen(context) { currentScreen = 0 }
                3 -> GHFavoriteScreen(
                    context, dlavieGames, userGames, favorites,
                    onGameClick, { currentScreen = 0 },
                    { favs -> favorites = favs }
                )
            }

            if (showDrawer) {
                GHDrawer(
                    currentScreen = currentScreen,
                    onSelect = { screen -> currentScreen = screen; showDrawer = false },
                    onDismiss = { showDrawer = false },
                    onExit = { showDrawer = false; onExit() }
                )
            }
        }

        GameHubTransition(visible = showTransition) { showTransition = false }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ADAPTIVE BACKGROUND — blurred game cover (PlayStore style)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AdaptiveGameBackground(game: GameItem) {
    Box(Modifier.fillMaxSize()) {
        // Blurred cover image
        if (game.coverImageRes != null) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(50.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback: gradient from cover colors
            Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient))
                    .blur(50.dp)
            )
        }
        // Dark overlay for readability
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xE6000000), Color(0xCC000000), Color(0xE6000000))
                )
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HOME SCREEN — larger cards, glassmorphism, View Detail + 3-dot menu
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHHomeScreen(
    context: Context, scope: kotlinx.coroutines.CoroutineScope,
    dlavieGames: List<GameItem>, userGames: List<UserGame>,
    favorites: Set<String>,
    selectedTab: Int, onTabSelect: (Int) -> Unit,
    onGameClick: (String) -> Unit,
    apkPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    currentTime: String, batteryLevel: Int,
    onMenu: () -> Unit, onExit: () -> Unit,
    onFavoritesChanged: (Set<String>) -> Unit,
    onFocusedGame: (GameItem) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // ── TOP BAR (glassmorphic) ──
        GHGlassTopBar(currentTime, batteryLevel, onMenu, onExit)

        // ── CATEGORY TABS (glassmorphic pills) ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GHGlassPillTab("DLavie", dlavieGames.size, selectedTab == 0) { onTabSelect(0) }
            GHGlassPillTab("My Library", userGames.size, selectedTab == 1) { onTabSelect(1) }
            GHGlassPillTab("Favorite", favorites.size, selectedTab == 2) { onTabSelect(2) }
            if (selectedTab == 1) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(GHGlassBg).border(1.dp, GHGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Add, "Add", tint = GHTextWhite, modifier = Modifier.size(24.dp)) }
            }
        }

        // ── GAME CAROUSEL (larger cards, scrollable, fills width) ──
        Box(
            Modifier.fillMaxWidth().weight(1f)
        ) {
            when (selectedTab) {
                0 -> {
                    val listState = rememberLazyListState()
                    val focusedIdx by remember { derivedStateOf { listState.firstVisibleItemIndex } }
                    LaunchedEffect(focusedIdx) {
                        if (focusedIdx < dlavieGames.size) onFocusedGame(dlavieGames[focusedIdx])
                    }
                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize().align(Alignment.Center)
                    ) {
                        itemsIndexed(dlavieGames) { idx, game ->
                            GHGameCardLarge(
                                game = game,
                                isInstalled = isPackageInstalled(context, game.packageName),
                                isFavorite = game.packageName in favorites,
                                onGameClick = { onGameClick(game.packageName) },
                                onToggleFavorite = {
                                    val newFavs = ghToggleFavorite(context, game.packageName)
                                    onFavoritesChanged(newFavs)
                                },
                                onRemove = {},
                                isFocused = idx == focusedIdx
                            )
                        }
                    }
                }
                1 -> {
                    if (userGames.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            GHEmptyState("No games yet", "Tap + to import an APK")
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize().align(Alignment.Center)
                        ) {
                            items(userGames) { ug ->
                                GHUserCardLarge(
                                    userGame = ug,
                                    isInstalled = isPackageInstalled(context, ug.packageName),
                                    isFavorite = ug.packageName in favorites,
                                    onGameClick = { onGameClick(ug.packageName) },
                                    onToggleFavorite = {
                                        val newFavs = ghToggleFavorite(context, ug.packageName)
                                        onFavoritesChanged(newFavs)
                                    },
                                    onRemove = {
                                        val u = userGames.filter { it.packageName != ug.packageName }
                                        saveUserGames(context, u)
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Favorite tab content
                    val favGames = dlavieGames.filter { it.packageName in favorites }
                    val favUserGames = userGames.filter { it.packageName in favorites }
                    if (favGames.isEmpty() && favUserGames.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            GHEmptyState("No favorites yet", "Tap the heart icon on any game")
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize().align(Alignment.Center)
                        ) {
                            items(favGames) { game ->
                                GHGameCardLarge(
                                    game = game,
                                    isInstalled = isPackageInstalled(context, game.packageName),
                                    isFavorite = true,
                                    onGameClick = { onGameClick(game.packageName) },
                                    onToggleFavorite = {
                                        val newFavs = ghToggleFavorite(context, game.packageName)
                                        onFavoritesChanged(newFavs)
                                    },
                                    onRemove = {}
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── BOTTOM BAR (glassmorphic) ──
        GHGlassBottomBar(onMenu)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAME CARD LARGE — 200x280dp, full-bleed cover, View Detail + 3-dot menu
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHGameCardLarge(
    game: GameItem,
    isInstalled: Boolean,
    isFavorite: Boolean,
    onGameClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    isFocused: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    val platform = GHPlatform.fromUrl(game.apkUrl)

    // Scale animation: focused card is bigger (1.0), others smaller (0.85)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "card_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.6f,
        animationSpec = tween(300),
        label = "card_alpha"
    )

    Column(
        Modifier.width(200.dp).padding(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        // ── CARD COVER (200x240dp, full-bleed, glassmorphic border) ──
        Box(
            Modifier.width(200.dp).height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(game.coverGradient))
                .border(1.dp, GHGlassBorderHi, RoundedCornerShape(16.dp))
                .clickable { onGameClick() }
        ) {
            // Cover image (full bleed)
            if (game.coverImageRes != null) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes),
                    contentDescription = game.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(game.coverText, color = GHTextWhite, fontSize = 42.sp, fontWeight = FontWeight.Black)
                }
            }

            // Top gradient overlay for label readability
            Box(
                Modifier.fillMaxWidth().height(56.dp).align(Alignment.TopStart)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.8f), Color.Transparent)))
            )

            // ── PLATFORM LABEL (top-left, dynamic: Android/iPhone/etc) ──
            Row(
                Modifier.align(Alignment.TopStart).padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(platform.color.copy(alpha = 0.85f))
                    .border(1.dp, platform.color, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    when (platform) {
                        GHPlatform.ANDROID -> Icons.Rounded.Android
                        GHPlatform.IOS -> Icons.Rounded.PhoneIphone
                        GHPlatform.WINDOWS -> Icons.Rounded.DesktopWindows
                        GHPlatform.MACOS -> Icons.Rounded.LaptopMac
                        GHPlatform.LINUX -> Icons.Rounded.Terminal
                        GHPlatform.UNKNOWN -> Icons.Rounded.Devices
                    },
                    contentDescription = platform.label,
                    tint = GHTextWhite,
                    modifier = Modifier.size(12.dp)
                )
                Text(platform.label, color = GHTextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // ── STATUS BADGE (top-right) ──
            val (sc, st) = when (game.serverStatus) {
                ServerStatus.ONLINE -> Pair(GHGreen, "ONLINE")
                ServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT")
                ServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE")
                ServerStatus.BUSY -> Pair(GHAmber, "BUSY")
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(sc.copy(alpha = 0.9f))
                    .border(1.dp, sc, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(st, color = GHTextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // ── FAVORITE ICON (bottom-right of card) ──
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) GHRed else GHTextWhite,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(22.dp).clickable { onToggleFavorite() }
            )

            // Bottom gradient overlay
            Box(
                Modifier.fillMaxWidth().height(64.dp).align(Alignment.BottomStart)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f))))
            )

            // Game title at bottom of card
            Text(
                game.title,
                color = GHTextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
            )
        }

        // ── VIEW DETAIL + 3-DOT MENU (below card) ──
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // View Detail button (glassmorphic)
            Box(
                Modifier.weight(1f).height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GHGlassBg)
                    .border(1.dp, GHGlassBorder, RoundedCornerShape(8.dp))
                    .clickable { onGameClick() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = GHTextWhite, modifier = Modifier.size(14.dp))
                    Text("View Detail", color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // 3-dot menu button
            Box(
                Modifier.size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GHGlassBg)
                    .border(1.dp, GHGlassBorder, RoundedCornerShape(8.dp))
                    .clickable { showMenu = !showMenu }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MoreVert, "Menu", tint = GHTextWhite, modifier = Modifier.size(18.dp))
            }
        }

        // ── DROPDOWN MENU (glassmorphic) ──
        if (showMenu) {
            Popup(
                onDismissRequest = { showMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier.width(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GHGlassBgHi)
                        .border(1.dp, GHGlassBorderHi, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    // Favorite toggle
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onToggleFavorite()
                                showMenu = false
                            }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            null,
                            tint = if (isFavorite) GHRed else GHTextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (isFavorite) "Remove from Favorite" else "Add to Favorite",
                            color = GHTextWhite, fontSize = 13.sp
                        )
                    }
                    // Remove from Library
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onRemove()
                                showMenu = false
                            }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, null, tint = GHRed, modifier = Modifier.size(18.dp))
                        Text("Remove Library", color = GHRed, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GHUserCardLarge(
    userGame: UserGame,
    isInstalled: Boolean,
    isFavorite: Boolean,
    onGameClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    isFocused: Boolean = false
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val platform = GHPlatform.fromPackage(userGame.packageName)

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f),
        label = "user_card_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.6f,
        animationSpec = tween(300),
        label = "user_card_alpha"
    )

    Column(Modifier.width(200.dp).padding(4.dp).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
        Box(
            Modifier.width(200.dp).height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E))))
                .border(1.dp, GHGlassBorderHi, RoundedCornerShape(16.dp))
                .clickable { onGameClick() }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val icon = remember(userGame.packageName) { try { context.packageManager.getApplicationIcon(userGame.packageName) } catch (_: Exception) { null } }
                if (icon != null) AsyncImage(model = icon, contentDescription = userGame.title, modifier = Modifier.size(80.dp), contentScale = ContentScale.Fit)
                else Icon(Icons.Rounded.SportsEsports, null, tint = GHTextDim, modifier = Modifier.size(56.dp))
            }

            // Platform label
            Row(
                Modifier.align(Alignment.TopStart).padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(platform.color.copy(alpha = 0.85f))
                    .border(1.dp, platform.color, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Rounded.Android, platform.label, tint = GHTextWhite, modifier = Modifier.size(12.dp))
                Text(platform.label, color = GHTextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Favorite icon
            Icon(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Favorite",
                tint = if (isFavorite) GHRed else GHTextWhite,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(22.dp).clickable { onToggleFavorite() }
            )

            Box(Modifier.fillMaxWidth().height(64.dp).align(Alignment.BottomStart).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f)))))
            Text(userGame.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp)).background(GHGlassBg).border(1.dp, GHGlassBorder, RoundedCornerShape(8.dp)).clickable { onGameClick() }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = GHTextWhite, modifier = Modifier.size(14.dp))
                    Text("View Detail", color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(GHGlassBg).border(1.dp, GHGlassBorder, RoundedCornerShape(8.dp)).clickable { showMenu = !showMenu }.padding(6.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MoreVert, "Menu", tint = GHTextWhite, modifier = Modifier.size(18.dp))
            }
        }

        if (showMenu) {
            Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
                Column(Modifier.width(180.dp).clip(RoundedCornerShape(12.dp)).background(GHGlassBgHi).border(1.dp, GHGlassBorderHi, RoundedCornerShape(12.dp)).padding(4.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onToggleFavorite(); showMenu = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFavorite) GHRed else GHTextWhite, modifier = Modifier.size(18.dp))
                        Text(if (isFavorite) "Remove from Favorite" else "Add to Favorite", color = GHTextWhite, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onRemove(); showMenu = false }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, null, tint = GHRed, modifier = Modifier.size(18.dp))
                        Text("Remove Library", color = GHRed, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FAVORITE SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHFavoriteScreen(
    context: Context,
    dlavieGames: List<GameItem>,
    userGames: List<UserGame>,
    favorites: Set<String>,
    onGameClick: (String) -> Unit,
    onBack: () -> Unit,
    onFavoritesChanged: (Set<String>) -> Unit
) {
    val favGames = dlavieGames.filter { it.packageName in favorites }
    val favUserGames = userGames.filter { it.packageName in favorites }

    Column(Modifier.fillMaxSize()) {
        GHGlassTopBar("", 0, onBack, onBack, title = "Favorite")
        if (favGames.isEmpty() && favUserGames.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GHEmptyState("No favorites yet", "Tap the heart icon on any game to add it here")
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                items(favGames) { game ->
                    GHGameCardLarge(
                        game = game,
                        isInstalled = isPackageInstalled(context, game.packageName),
                        isFavorite = true,
                        onGameClick = { onGameClick(game.packageName) },
                        onToggleFavorite = {
                            val newFavs = ghToggleFavorite(context, game.packageName)
                            onFavoritesChanged(newFavs)
                        },
                        onRemove = {}
                    )
                }
                items(favUserGames) { ug ->
                    GHUserCardLarge(
                        userGame = ug,
                        isInstalled = isPackageInstalled(context, ug.packageName),
                        isFavorite = true,
                        onGameClick = { onGameClick(ug.packageName) },
                        onToggleFavorite = {
                            val newFavs = ghToggleFavorite(context, ug.packageName)
                            onFavoritesChanged(newFavs)
                        },
                        onRemove = {
                            val u = userGames.filter { it.packageName != ug.packageName }
                            saveUserGames(context, u)
                        }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EMPTY STATE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHEmptyState(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.SportsEsports, null, tint = GHTextDim, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = GHTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = GHTextGray, fontSize = 13.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASSMORPHIC TOP BAR
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHGlassTopBar(currentTime: String, batteryLevel: Int, onMenu: () -> Unit, onExit: () -> Unit, title: String = "Dashboard") {
    Row(
        Modifier.fillMaxWidth().height(64.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Menu, "Menu", tint = GHTextWhite, modifier = Modifier.size(28.dp).clickable { onMenu() })
        Spacer(Modifier.width(20.dp))
        GHCtrlBtn("LB")
        Spacer(Modifier.width(16.dp))
        Text(title, color = GHTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Spacer(Modifier.width(16.dp))
        GHCtrlBtn("RB")
        Spacer(Modifier.width(20.dp))
        Icon(Icons.Rounded.Search, "Search", tint = GHTextWhite, modifier = Modifier.size(24.dp))
        if (batteryLevel > 0) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.BatteryFull, "Battery", tint = GHTextWhite, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("$batteryLevel%", color = GHTextWhite, fontSize = 13.sp)
        }
        if (currentTime.isNotEmpty()) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.Schedule, "Time", tint = GHTextWhite, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(currentTime, color = GHTextWhite, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Rounded.ArrowBack, "Exit", tint = GHTextWhite, modifier = Modifier.size(28.dp).clickable { onExit() })
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASSMORPHIC BOTTOM BAR
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHGlassBottomBar(onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GHCtrlBtn("Y")
            Text("Search", color = GHTextGray, fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clickable { onMenu() }) {
            Text("Menu", color = GHTextGray, fontSize = 14.sp)
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(GHGlassBg).border(1.dp, GHGlassBorder, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Menu, null, tint = GHTextGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DOWNLOAD SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDownloadScreen(context: Context, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val (availStorage, totalStorage) = remember { ghGetStorageInfo() }
    val gameStorage = remember { try { val dir = File("/sdcard/Android/data/$GAME_PKG_16"); if (dir.exists()) dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() else 0L } catch (_: Exception) { 0L } }

    Column(Modifier.fillMaxSize()) {
        GHGlassTopBar("", 0, onBack, onBack, title = "Download")
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GHGlassPillTab("Download Task", 0, selectedTab == 0) { selectedTab = 0 }
            GHGlassPillTab("Game Management", 0, selectedTab == 1) { selectedTab = 1 }
        }
        Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Storage card — glassmorphic
            Surface(shape = RoundedCornerShape(16.dp), color = GHGlassBg, border = BorderStroke(1.dp, GHGlassBorder)) {
                Column(Modifier.padding(20.dp)) {
                    Text("STORAGE", color = GHTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Game", color = GHTextGray, fontSize = 13.sp); Text(ghFormatBytes(gameStorage), color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Other", color = GHTextGray, fontSize = 13.sp); Text(ghFormatBytes(totalStorage - availStorage - gameStorage), color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Available", color = GHTextGray, fontSize = 13.sp); Text(ghFormatBytes(availStorage), color = GHGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    val usedPct = if (totalStorage > 0) ((totalStorage - availStorage).toFloat() / totalStorage.toFloat()) else 0f
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(GHGlassBorder)) {
                        Box(Modifier.fillMaxWidth(usedPct).fillMaxHeight().background(Brush.linearGradient(listOf(GHAccent.copy(0.7f), GHAccent))))
                    }
                }
            }
            if (selectedTab == 0) {
                Text("Download Soon (0)", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(16.dp), color = GHGlassBg, border = BorderStroke(1.dp, GHGlassBorder), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No Queued Tasks", color = GHTextDim, fontSize = 13.sp) }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Completed", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Clear All", color = GHAccent, fontSize = 12.sp, modifier = Modifier.clickable { })
                }
                Surface(shape = RoundedCornerShape(16.dp), color = GHGlassBg, border = BorderStroke(1.dp, GHGlassBorder), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No completed tasks", color = GHTextDim, fontSize = 13.sp) }
                }
            } else {
                val games = listOf(Triple("FIFA 16 Mobile", GAME_PKG_16, "DLavie 26 Mod"), Triple("FIFA 15 Mobile", GAME_PKG_15, "DLavie 15 Mod"))
                games.forEach { (title, pkg, subtitle) ->
                    val installed = isPackageInstalled(context, pkg)
                    Surface(shape = RoundedCornerShape(16.dp), color = GHGlassBg, border = BorderStroke(1.dp, GHGlassBorder), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(GHGlassBgHi).border(1.dp, GHGlassBorder, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text(title.take(2), color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Black) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(subtitle, color = GHTextGray, fontSize = 12.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(if (installed) "Installed" else "Not installed", color = if (installed) GHGreen else GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            if (installed) {
                                IconButton(onClick = { try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {} }) { Icon(Icons.Rounded.Delete, "Uninstall", tint = GHRed.copy(0.7f), modifier = Modifier.size(22.dp)) }
                            } else {
                                IconButton(onClick = {}) { Icon(Icons.Rounded.Download, "Install", tint = GHAccent, modifier = Modifier.size(22.dp)) }
                            }
                        }
                    }
                }
            }
        }
        GHGlassBottomBar(onBack)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHSettingsScreen(context: Context, onBack: () -> Unit) {
    var gamepadCount by remember { mutableStateOf(0) }
    var gamepadNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val ds = InputDevice.getDeviceIds(); val gps = mutableListOf<String>()
            for (id in ds) { val d = InputDevice.getDevice(id); if (d != null) { val s = d.sources; if (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) gps.add(d.name) } }
            gamepadCount = gps.size; gamepadNames = gps
        }
    }

    Column(Modifier.fillMaxSize()) {
        GHGlassTopBar("", 0, onBack, onBack, title = "Settings")
        Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("GAMEPAD", color = GHTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(16.dp), color = GHGlassBg, border = BorderStroke(1.dp, GHGlassBorder)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (gamepadCount > 0) GHAccent.copy(0.15f) else GHGlassBgHi).border(1.dp, if (gamepadCount > 0) GHAccent else GHGlassBorder, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.SportsEsports, null, tint = if (gamepadCount > 0) GHAccent else GHTextDim, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) { Text("Gamepad Connection", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(if (gamepadCount > 0) "$gamepadCount gamepad(s) connected" else "No gamepad detected", color = if (gamepadCount > 0) GHGreen else GHTextGray, fontSize = 12.sp) }
                        Box(Modifier.size(12.dp).clip(CircleShape).background(if (gamepadCount > 0) GHGreen else GHTextDim))
                    }
                    if (gamepadNames.isNotEmpty()) { Spacer(Modifier.height(12.dp)); gamepadNames.forEach { n -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) { Box(Modifier.size(8.dp).clip(CircleShape).background(GHGreen)); Spacer(Modifier.width(10.dp)); Text(n, color = GHTextGray, fontSize = 12.sp) } } }
                }
            }
            Text("DISPLAY", color = GHTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(16.dp), color = GHGlassBg, border = BorderStroke(1.dp, GHGlassBorder)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(GHAccent.copy(0.15f)).border(1.dp, GHAccent, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ScreenRotation, null, tint = GHAccent, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text("Auto Rotate", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text("Landscape mode in GameHub", color = GHTextGray, fontSize = 12.sp) }
                    Box(Modifier.size(28.dp).clip(CircleShape).background(GHGreen).border(1.dp, GHGreen.copy(0.5f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
                }
            }
            Text("ABOUT", color = GHTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(16.dp), color = GHGlassBg, border = BorderStroke(1.dp, GHGlassBorder)) {
                Column(Modifier.padding(20.dp)) { Text("DLavie GameHub", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text("Cloud Gaming Platform v7.9.84", color = GHTextGray, fontSize = 12.sp) }
            }
        }
        GHGlassBottomBar(onBack)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SIDE DRAWER — with Favorite tab
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHDrawer(currentScreen: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)).clickable { onDismiss() }) {
        Box(Modifier.fillMaxHeight().width(320.dp).background(Color(0xFF121826)).clickable {}) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                // Profile
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(GHAccent, Color(0xFF009688)))), contentAlignment = Alignment.Center) { Text("D", color = GHTextWhite, fontSize = 26.sp, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(16.dp))
                    Column { Text("DLavie Player", color = GHTextWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold); Text("@user", color = GHTextGray, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(28.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GHGlassBorder))
                Spacer(Modifier.height(16.dp))
                GHDrawerItem(Icons.Rounded.Home, "Home", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.SportsEsports, "Game", currentScreen == 0) { onSelect(0) }
                GHDrawerItem(Icons.Rounded.Download, "Download", currentScreen == 1) { onSelect(1) }
                GHDrawerItem(Icons.Rounded.Favorite, "Favorite", currentScreen == 3) { onSelect(3) }
                GHDrawerItem(Icons.Rounded.Settings, "Settings", currentScreen == 2) { onSelect(2) }
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(1.dp).background(GHGlassBorder))
                Spacer(Modifier.height(12.dp))
                GHDrawerItem(Icons.Rounded.Close, "Exit GameHub", false) { onExit() }
                Spacer(Modifier.height(8.dp))
                Text("DLavie GameHub v282", color = GHTextDim, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun GHDrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (selected) GHAccent.copy(0.15f) else Color.Transparent).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) GHAccent else GHTextGray, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = if (selected) GHTextWhite else GHTextGray, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHCtrlBtn(label: String, size: Int = 40) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(8.dp))
            .background(GHGlassBg).border(1.dp, GHGlassBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) { Text(label, color = GHTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GHGlassPillTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp))
            .background(if (selected) GHAccent.copy(0.2f) else GHGlassBg)
            .border(1.dp, if (selected) GHAccent else GHGlassBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = if (selected) GHTextWhite else GHTextGray, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        if (count > 0) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(if (selected) GHAccent else GHGlassBgHi), contentAlignment = Alignment.Center) {
                Text(count.toString(), color = GHTextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
