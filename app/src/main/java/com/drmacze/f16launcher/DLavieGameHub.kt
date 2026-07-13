package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.text.format.DateFormat
import android.view.InputDevice
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB — Console-style TV launcher (100% match to reference)
// ═══════════════════════════════════════════════════════════════════════════

private val GHBlack = Color(0xFF000000)
private val GHSurface = Color(0xFF0A0A0A)
private val GHCard = Color(0xFF111111)
private val GHCardHi = Color(0xFF1A1A1A)
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextSoft = Color(0xFF999999)
private val GHTextDim = Color(0xFF555555)
private val GHStroke = Color(0x10FFFFFF)
private val GHAccent = Color(0xFF00D4FF)
private val GHAccentGreen = Color(0xFF00D26A)
private val GHAccentAmber = Color(0xFFFFAA00)
private val GHAccentRed = Color(0xFFFF5252)

// ─── PlayStation-style Transition ────────────────────────────────────────────
@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(0) }

    LaunchedEffect(visible) {
        if (visible) {
            phase = 1
            delay(100)
            phase = 2
            delay(600)
            phase = 3
            delay(400)
            onComplete()
            phase = 0
        }
    }

    if (visible && phase > 0) {
        Box(
            Modifier.fillMaxSize().background(GHBlack),
            contentAlignment = Alignment.Center
        ) {
            val scale by animateFloatAsState(
                when (phase) { 1 -> 1f; 2 -> 1f; 3 -> 1.5f; else -> 0f },
                tween(if (phase == 1 || phase == 3) 400 else 0, easing = FastOutSlowInEasing),
                label = "scale"
            )
            val alpha by animateFloatAsState(
                when (phase) { 1 -> 1f; 2 -> 1f; 3 -> 0f; else -> 0f },
                tween(if (phase == 1 || phase == 3) 400 else 0, easing = FastOutSlowInEasing),
                label = "alpha"
            )
            val glowSize by animateFloatAsState(
                if (phase == 2) 1.2f else 1f,
                infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "glow"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            ) {
                Box(
                    Modifier.size(80.dp * glowSize).clip(RoundedCornerShape(20.dp)).background(GHTextWhite),
                    contentAlignment = Alignment.Center
                ) { Text("DL", color = GHBlack, fontSize = 32.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(16.dp))
                Text("DLAVIE", color = GHTextWhite, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
                Spacer(Modifier.height(4.dp))
                Text("GAMEHUB", color = GHTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
            }
        }
    }
}

// ─── Battery + Time helpers ──────────────────────────────────────────────────
private fun getBatteryLevel(context: Context): Int {
    return try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (_: Exception) { 100 }
}

private fun getCurrentTime(): String {
    return try {
        val is24Hour = DateFormat.is24HourFormat(null) // can't pass context here, use default
        val format = if (is24Hour) "HH:mm" else "h:mm a"
        SimpleDateFormat(format, Locale.getDefault()).format(Date())
    } catch (_: Exception) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN: DLavie GameHub (console-style, 100% match to reference)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(
    onNav: (Page) -> Unit,
    onGameClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Transition ──
    var showTransition by remember { mutableStateOf(true) }

    // ── Auto-rotate landscape ──
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    // ── Real-time clock + battery ──
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var batteryLevel by remember { mutableStateOf(getBatteryLevel(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            batteryLevel = getBatteryLevel(context)
            delay(30_000) // update every 30 seconds
        }
    }

    // ── Category tabs ──
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

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

    // ── DLavie games ──
    val dlavieGames = remember(fifa16Status, fifa15Status) {
        listOf(
            GameItem(
                title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod",
                packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                coverText = "DL", coverImageRes = R.drawable.fifa16_cover,
                serverStatus = fifa16Status, description = "FIFA 16 Mobile dengan mod DLavie 26",
                version = "v26.0", sizeMb = "34 MB", apkUrl = FIFA16_APK_URL
            ),
            GameItem(
                title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod",
                packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15", coverImageRes = R.drawable.fifa15_cover,
                serverStatus = fifa15Status, description = "FIFA 15 Mobile dengan mod DLavie 15",
                version = "v15.0", sizeMb = "22 MB", apkUrl = FIFA15_APK_URL
            )
        )
    }

    // ── User games ──
    var userGames by remember { mutableStateOf<List<UserGame>>(emptyList()) }
    LaunchedEffect(selectedTab) { if (selectedTab == 1) userGames = withContext(Dispatchers.IO) { loadUserGames(context) } }

    // ── APK picker ──
    val apkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            } catch (_: Exception) { }
        }
    }

    var contextMenuGame by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // ── Transition overlay ──
    GameHubTransition(visible = showTransition) { showTransition = false }

    Box(Modifier.fillMaxSize().background(GHBlack)) {
        // ── Background image (blurred game cover) ──
        val bgGame = dlavieGames.getOrNull(0)
        if (bgGame?.coverImageRes != null) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = bgGame.coverImageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(40.dp).alpha(0.15f),
                contentScale = ContentScale.Crop
            )
        }

        Column(Modifier.fillMaxSize()) {
            // ════════════════════════════════════════════════════════════════════
            // TOP BAR — Console style (LB | Dashboard | RB ... Search WiFi Battery Time)
            // ════════════════════════════════════════════════════════════════════
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LB button label (left)
                GHControllerButton("LB")
                Spacer(Modifier.width(16.dp))
                // Dashboard title (center)
                Text("Dashboard", color = GHTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.width(16.dp))
                // RB button label (right)
                GHControllerButton("RB")
                Spacer(Modifier.width(16.dp))
                // Search icon
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = GHTextSoft, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                // WiFi icon
                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                // Battery
                Text("$batteryLevel%", color = GHTextSoft, fontSize = 13.sp, fontFamily = InterFontFamily)
                Spacer(Modifier.width(8.dp))
                // Time (device timezone)
                Text(currentTime, color = GHTextSoft, fontSize = 13.sp, fontFamily = InterFontFamily)
            }

            // ── Category tabs ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GHCategoryTab("DLavie", dlavieGames.size, selectedTab == 0, { selectedTab = 0 }, Modifier.weight(1f))
                GHCategoryTab("My Library", userGames.size, selectedTab == 1, { selectedTab = 1 }, Modifier.weight(1f))
            }

            // ════════════════════════════════════════════════════════════════════
            // GAME CARDS — Large, console-style horizontal scroll
            // ════════════════════════════════════════════════════════════════════
            if (selectedTab == 0) {
                LazyRow(
                    Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(dlavieGames) { game ->
                        GHConsoleGameCard(
                            game = game,
                            isInstalled = isPackageInstalled(context, game.packageName),
                            onClick = { onGameClick(game.packageName) },
                            onLongClick = { contextMenuGame = Pair(game.packageName, false) }
                        )
                    }
                }
            } else {
                // My Library
                if (userGames.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f).padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No games yet", color = GHTextSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(6.dp))
                            Text("Press [Add Game] to import an APK", color = GHTextDim, fontSize = 12.sp, fontFamily = InterFontFamily)
                        }
                    }
                } else {
                    LazyRow(
                        Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp),
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item { GHAddGameCard { apkPickerLauncher.launch("application/vnd.android.package-archive") } }
                        items(userGames) { userGame ->
                            GHConsoleUserCard(
                                userGame = userGame,
                                isInstalled = isPackageInstalled(context, userGame.packageName),
                                onClick = {
                                    if (isPackageInstalled(context, userGame.packageName)) {
                                        launchGame(context, userGame.packageName)
                                    } else {
                                        val file = File(userGame.sourcePath)
                                        if (file.exists()) {
                                            try {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                })
                                            } catch (_: Exception) { }
                                        }
                                    }
                                },
                                onLongClick = { contextMenuGame = Pair(userGame.packageName, true) }
                            )
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════════
            // BOTTOM BAR — Console style (Y | Search ... Menu)
            // ════════════════════════════════════════════════════════════════════
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Search with Y button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHControllerButton("Y", small = true)
                    Spacer(Modifier.width(8.dp))
                    Text("Search", color = GHTextSoft, fontSize = 13.sp, fontFamily = InterFontFamily)
                }
                // Center: Add Game (My Library only)
                if (selectedTab == 1) {
                    Button(
                        onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GHTextWhite, contentColor = GHBlack),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Game", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Right: Menu with hamburger
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showSettings = true }) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = GHTextSoft, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Menu", color = GHTextSoft, fontSize = 13.sp, fontFamily = InterFontFamily)
                }
            }
        }

        // ── Context menu ──
        contextMenuGame?.let { (pkg, isUser) ->
            GHContextMenu(
                packageName = pkg, isUserGame = isUser,
                isInstalled = isPackageInstalled(context, pkg),
                onDismiss = { contextMenuGame = null },
                onLaunch = { contextMenuGame = null; launchGame(context, pkg) },
                onViewDetails = { contextMenuGame = null; onGameClick(pkg) },
                onUninstall = { contextMenuGame = null; try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { } },
                onRemoveFromLibrary = { contextMenuGame = null; val u = userGames.filter { it.packageName != pkg }; saveUserGames(context, u); userGames = u },
                onClearData = { contextMenuGame = null; try { File("/sdcard/Android/data/$pkg").deleteRecursively() } catch (_: Exception) { } }
            )
        }

        // ── Settings overlay ──
        if (showSettings) { GHSettingsOverlay(onDismiss = { showSettings = false }) }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENTS — Console-style (match reference 100%)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHControllerButton(label: String, small: Boolean = false) {
    val size = if (small) 20.dp else 28.dp
    val fontSize = if (small) 9.sp else 10.sp
    Box(
        Modifier.size(size).clip(RoundedCornerShape(4.dp))
            .background(GHSurface)
            .border(1.dp, GHTextDim, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = GHTextSoft, fontSize = fontSize, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
    }
}

@Composable
private fun GHCategoryTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) GHTextWhite.copy(0.06f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) GHTextWhite.copy(0.15f) else GHStroke)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(label, color = if (selected) GHTextWhite else GHTextSoft, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontFamily = InterFontFamily)
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), color = if (selected) GHAccent else GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHConsoleGameCard(game: GameItem, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.width(280.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GHCard),
        border = BorderStroke(1.dp, GHStroke)
    ) {
        Column {
            // Cover image — large, fills card width
            Box(
                Modifier.fillMaxWidth().height(160.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Brush.linearGradient(game.coverGradient))
            ) {
                if (game.coverImageRes != null) {
                    Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(game.coverText, color = GHTextWhite, fontSize = 42.sp, fontWeight = FontWeight.Black) }
                }
                // Dark gradient overlay at bottom
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, GHBlack.copy(0.8f)))))

                // Launch Game button overlay (bottom of cover, with A button)
                Row(
                    Modifier.align(Alignment.BottomStart).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GHControllerButton("A", small = true)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isInstalled) "Launch Game" else "Install", color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                }

                // Server status (top-right)
                val stColor = when (game.serverStatus) { ServerStatus.ONLINE -> GHAccentGreen; ServerStatus.MAINTENANCE -> GHAccentAmber; ServerStatus.OFFLINE -> GHAccentRed; ServerStatus.BUSY -> GHAccentAmber }
                val stText = when (game.serverStatus) { ServerStatus.ONLINE -> "ONLINE"; ServerStatus.MAINTENANCE -> "MAINT"; ServerStatus.OFFLINE -> "OFFLINE"; ServerStatus.BUSY -> "BUSY" }
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(stColor.copy(0.8f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(stText, color = GHBlack, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            // Info section
            Column(Modifier.padding(14.dp)) {
                Text(game.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(game.subtitle, color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                // View Details button (semi-transparent dark, rounded)
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onClick() },
                    shape = RoundedCornerShape(8.dp),
                    color = GHCardHi,
                    border = BorderStroke(1.dp, GHStroke)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("View Details", color = GHTextSoft, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHConsoleUserCard(userGame: UserGame, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.width(280.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GHCard),
        border = BorderStroke(1.dp, GHStroke)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))), contentAlignment = Alignment.Center) {
                val iconDrawable = remember(userGame.packageName) { try { context.packageManager.getApplicationIcon(userGame.packageName) } catch (_: Exception) { null } }
                if (iconDrawable != null) {
                    AsyncImage(model = iconDrawable, contentDescription = userGame.title, modifier = Modifier.size(80.dp), contentScale = ContentScale.Fit)
                } else {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(64.dp))
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, GHBlack.copy(0.8f)))))
                Row(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GHControllerButton("A", small = true)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isInstalled) "Launch Game" else "Install", color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(userGame.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(userGame.packageName.take(25), color = GHTextDim, fontSize = 10.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(8.dp), color = GHCardHi, border = BorderStroke(1.dp, GHStroke)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("View Details", color = GHTextSoft, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun GHAddGameCard(onClick: () -> Unit) {
    Card(modifier = Modifier.width(280.dp).clickable { onClick() }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GHSurface), border = BorderStroke(1.dp, GHAccent.copy(0.15f))) {
        Column(Modifier.fillMaxWidth().height(240.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(GHAccent.copy(0.06f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = GHAccent, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Add Game", color = GHAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text("Import APK", color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun GHContextMenu(packageName: String, isUserGame: Boolean, isInstalled: Boolean, onDismiss: () -> Unit, onLaunch: () -> Unit, onViewDetails: () -> Unit, onUninstall: () -> Unit, onRemoveFromLibrary: () -> Unit, onClearData: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Game Options", color = GHTextWhite, fontWeight = FontWeight.Black) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isInstalled) { GHMenuItem(Icons.Rounded.PlayArrow, "Launch Game", onLaunch) }
            GHMenuItem(Icons.Rounded.Info, "View Details", onViewDetails)
            if (!isUserGame && isInstalled) { GHMenuItem(Icons.Rounded.CleaningServices, "Clear Data Only", onClearData) }
            if (isUserGame) { if (isInstalled) { GHMenuItem(Icons.Rounded.Delete, "Uninstall Game", onUninstall) }; GHMenuItem(Icons.Rounded.RemoveCircle, "Remove from Library", onRemoveFromLibrary) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GHTextSoft) } }, containerColor = GHCard)
}

@Composable
private fun GHMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = GHTextWhite, fontSize = 14.sp, fontFamily = InterFontFamily)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GAMEPAD SETTINGS OVERLAY
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHSettingsOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var gamepadCount by remember { mutableStateOf(0) }
    var gamepadNames by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val devices = InputDevice.getDeviceIds()
            val gamepads = mutableListOf<String>()
            for (id in devices) {
                val device = InputDevice.getDevice(id)
                if (device != null) {
                    val sources = device.sources
                    if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                        gamepads.add(device.name)
                    }
                }
            }
            gamepadCount = gamepads.size; gamepadNames = gamepads
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GameHub Settings", color = GHTextWhite, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("GAMEPAD", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Surface(shape = RoundedCornerShape(12.dp), color = GHCard, border = BorderStroke(1.dp, GHStroke)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = if (gamepadCount > 0) GHAccentGreen else GHTextDim, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Gamepad Connection", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(if (gamepadCount > 0) "$gamepadCount gamepad(s) connected" else "No gamepad detected", color = if (gamepadCount > 0) GHAccentGreen else GHTextDim, fontSize = 12.sp)
                            }
                            Box(Modifier.size(10.dp).clip(CircleShape).background(if (gamepadCount > 0) GHAccentGreen else GHTextDim))
                        }
                        if (gamepadNames.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            gamepadNames.forEach { name -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) { Box(Modifier.size(6.dp).clip(CircleShape).background(GHAccentGreen)); Spacer(Modifier.width(8.dp)); Text(name, color = GHTextSoft, fontSize = 12.sp) } }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Connect a Bluetooth or USB gamepad to your device. Games with gamepad support will use it automatically.", color = GHTextDim, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
                Text("DISPLAY", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Surface(shape = RoundedCornerShape(12.dp), color = GHCard, border = BorderStroke(1.dp, GHStroke)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text("Auto Rotate", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Landscape mode when entering GameHub", color = GHTextDim, fontSize = 12.sp) }
                            Box(Modifier.size(24.dp).clip(CircleShape).background(GHAccentGreen), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, contentDescription = null, tint = GHBlack, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
                Text("ABOUT", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Surface(shape = RoundedCornerShape(12.dp), color = GHCard, border = BorderStroke(1.dp, GHStroke)) {
                    Column(Modifier.padding(16.dp)) { Text("DLavie GameHub", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Cloud Gaming Platform v7.9.79", color = GHTextDim, fontSize = 12.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = GHAccent, fontWeight = FontWeight.Bold) } },
        containerColor = GHSurface
    )
}
