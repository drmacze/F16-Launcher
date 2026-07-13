package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.BatteryManager
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
// DLAVIE GAMEHUB — Console-style TV launcher (EXACT match to reference)
// ═══════════════════════════════════════════════════════════════════════════

// Colors — match reference exactly
private val GHBg = Color(0xFF0A0A1A)         // dark blue-black background
private val GHHeaderBg = Color(0xFF222222)    // header bar bg
private val GHBottomBg = Color(0xFF222222)    // bottom bar bg
private val GHCardBorder = Color(0xFF333333)  // card border
private val GHBtnBg = Color(0xFF222222)       // LB/RB/Y button bg
private val GHBtnBorder = Color(0xFF333333)   // button border
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextSoft = Color(0xFF999999)
private val GHTextDim = Color(0xFF666666)
private val GHBadgeOnline = Color(0xFF00D26A)
private val GHBadgeMaint = Color(0xFFFF9900)
private val GHBadgeOffline = Color(0xFFFF5252)
private val GHFabBg = Color(0xFFFFFFFF)
private val GHAccentBlue = Color(0xFF00AAFF)

// ─── PlayStation-style Transition ────────────────────────────────────────────
@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(visible) {
        if (visible) {
            phase = 1; delay(100); phase = 2; delay(600); phase = 3; delay(400); onComplete(); phase = 0
        }
    }
    if (visible && phase > 0) {
        Box(Modifier.fillMaxSize().background(GHBg), contentAlignment = Alignment.Center) {
            val scale by animateFloatAsState(when (phase) { 1 -> 1f; 2 -> 1f; 3 -> 1.5f; else -> 0f }, tween(if (phase == 1 || phase == 3) 400 else 0, easing = FastOutSlowInEasing), label = "s")
            val alpha by animateFloatAsState(when (phase) { 1 -> 1f; 2 -> 1f; 3 -> 0f; else -> 0f }, tween(if (phase == 1 || phase == 3) 400 else 0, easing = FastOutSlowInEasing), label = "a")
            val glow by animateFloatAsState(if (phase == 2) 1.2f else 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "g")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
                Box(Modifier.size(80.dp * glow).clip(RoundedCornerShape(20.dp)).background(GHTextWhite), contentAlignment = Alignment.Center) { Text("DL", color = Color.Black, fontSize = 32.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(16.dp))
                Text("DLAVIE", color = GHTextWhite, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
                Spacer(Modifier.height(4.dp))
                Text("GAMEHUB", color = GHTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
            }
        }
    }
}

// ─── Battery + Time ──────────────────────────────────────────────────────────
private fun getBatteryLevel(context: Context): Int = try { (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } catch (_: Exception) { 100 }
private fun getCurrentTime(context: Context): String = try { val is24 = DateFormat.is24HourFormat(context); SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date()) } catch (_: Exception) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

// ═══════════════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(onNav: (Page) -> Unit, onGameClick: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showTransition by remember { mutableStateOf(true) }

    // Auto-rotate landscape
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val orig = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = orig ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    // Real-time clock + battery
    var currentTime by remember { mutableStateOf(getCurrentTime(context)) }
    var batteryLevel by remember { mutableStateOf(getBatteryLevel(context)) }
    LaunchedEffect(Unit) { while (true) { currentTime = getCurrentTime(context); batteryLevel = getBatteryLevel(context); delay(30_000) } }

    // Category tabs
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    // Server status
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

    // DLavie games
    val dlavieGames = remember(fifa16Status, fifa15Status) {
        listOf(
            GameItem(title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod", packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity", coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)), coverText = "DL", coverImageRes = R.drawable.fifa16_cover, serverStatus = fifa16Status, description = "FIFA 16 Mobile dengan mod DLavie 26", version = "v26.0", sizeMb = "34 MB", apkUrl = FIFA16_APK_URL),
            GameItem(title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod", packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY, coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)), coverText = "D15", coverImageRes = R.drawable.fifa15_cover, serverStatus = fifa15Status, description = "FIFA 15 Mobile dengan mod DLavie 15", version = "v15.0", sizeMb = "22 MB", apkUrl = FIFA15_APK_URL)
        )
    }

    // User games
    var userGames by remember { mutableStateOf<List<UserGame>>(emptyList()) }
    LaunchedEffect(selectedTab) { if (selectedTab == 1) userGames = withContext(Dispatchers.IO) { loadUserGames(context) } }

    // APK picker
    val apkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { try { context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) { } }
    }

    var contextMenuGame by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // Transition
    GameHubTransition(visible = showTransition) { showTransition = false }

    // ═════════════════════════════════════════════════════════════════════════
    // LAYOUT — exact match to reference
    // ═════════════════════════════════════════════════════════════════════════
    Box(Modifier.fillMaxSize().background(GHBg)) {
        Column(Modifier.fillMaxSize()) {

            // ── HEADER BAR (LB | Dashboard | RB | Search | WiFi | Battery% | Time) ──
            Row(
                Modifier.fillMaxWidth().background(GHHeaderBg).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LB button
                GHBtn("LB")
                Spacer(Modifier.width(16.dp))
                // Dashboard title (center)
                Text("Dashboard", color = GHTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.width(16.dp))
                // RB button
                GHBtn("RB")
                Spacer(Modifier.width(12.dp))
                // Search icon
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = GHTextWhite, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                // WiFi
                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = GHTextWhite, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                // Battery
                Text("$batteryLevel%", color = GHTextWhite, fontSize = 13.sp, fontFamily = InterFontFamily)
                Spacer(Modifier.width(6.dp))
                // Time
                Text(currentTime, color = GHTextWhite, fontSize = 13.sp, fontFamily = InterFontFamily)
            }

            // ── CATEGORY TABS ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GHTab("DLavie", dlavieGames.size, selectedTab == 0, { selectedTab = 0 }, Modifier.weight(1f))
                GHTab("My Library", userGames.size, selectedTab == 1, { selectedTab = 1 }, Modifier.weight(1f))
            }

            // ── GAME CARDS AREA ──
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (selectedTab == 0) {
                    // DLavie games — 2 cards side-by-side (not scroll)
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        dlavieGames.forEach { game ->
                            GHCard(
                                game = game,
                                isInstalled = isPackageInstalled(context, game.packageName),
                                onClick = { onGameClick(game.packageName) },
                                onLongClick = { contextMenuGame = Pair(game.packageName, false) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // My Library
                    if (userGames.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("No games yet", color = GHTextSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                Spacer(Modifier.height(6.dp))
                                Text("Press + to import an APK", color = GHTextDim, fontSize = 12.sp, fontFamily = InterFontFamily)
                            }
                        }
                    } else {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            userGames.take(2).forEach { userGame ->
                                GHUserCard(
                                    userGame = userGame,
                                    isInstalled = isPackageInstalled(context, userGame.packageName),
                                    onClick = {
                                        if (isPackageInstalled(context, userGame.packageName)) launchGame(context, userGame.packageName)
                                        else { val f = File(userGame.sourcePath); if (f.exists()) { try { val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", f); context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) { } } }
                                    },
                                    onLongClick = { contextMenuGame = Pair(userGame.packageName, true) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ── FAB (+ button, top-right of cards area) ──
                if (selectedTab == 1) {
                    FloatingActionButton(
                        onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(48.dp),
                        shape = CircleShape,
                        containerColor = GHFabBg,
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Game", modifier = Modifier.size(24.dp))
                    }
                }
            }

            // ── BOTTOM BAR (Y | Search ... Hamburger | Menu) ──
            Row(
                Modifier.fillMaxWidth().background(GHBottomBg).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Y + Search
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHBtn("Y")
                    Spacer(Modifier.width(8.dp))
                    Text("Search", color = GHTextWhite, fontSize = 14.sp, fontFamily = InterFontFamily)
                }
                // Right: Hamburger + Menu
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showSettings = true }) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = GHTextWhite, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Menu", color = GHTextWhite, fontSize = 14.sp, fontFamily = InterFontFamily)
                }
            }
        }

        // Context menu
        contextMenuGame?.let { (pkg, isUser) ->
            GHContextMenu(pkg, isUser, isPackageInstalled(context, pkg), { contextMenuGame = null }, { contextMenuGame = null; launchGame(context, pkg) }, { contextMenuGame = null; onGameClick(pkg) }, { contextMenuGame = null; try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { } }, { contextMenuGame = null; val u = userGames.filter { it.packageName != pkg }; saveUserGames(context, u); userGames = u }, { contextMenuGame = null; try { File("/sdcard/Android/data/$pkg").deleteRecursively() } catch (_: Exception) { } })
        }

        // Settings
        if (showSettings) { GHSettingsOverlay { showSettings = false } }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENTS — exact match to reference
// ═══════════════════════════════════════════════════════════════════════════

// Controller button (LB/RB/Y/A) — dark gray rounded square
@Composable
private fun GHBtn(label: String) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(GHBtnBg).border(1.dp, GHBtnBorder, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) { Text(label, color = GHTextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) }
}

// Category tab — rectangular, dark gray, rounded
@Composable
private fun GHTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) GHHeaderBg else Color.Transparent,
        border = BorderStroke(1.dp, GHCardBorder)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(label, color = if (selected) GHTextWhite else GHTextSoft, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontFamily = InterFontFamily)
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), color = if (selected) GHAccentBlue else GHTextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Game card — text overlay ON cover (NO separate info section below)
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHCard(game: GameItem, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = BorderStroke(1.dp, GHCardBorder)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Cover image — fills entire card
            if (game.coverImageRes != null) {
                Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) { Text(game.coverText, color = GHTextWhite, fontSize = 42.sp, fontWeight = FontWeight.Black) }
            }

            // Dark gradient overlay (bottom fade for text readability)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(0.7f)))))

            // Status badge (top-right)
            val (stColor, stText) = when (game.serverStatus) {
                ServerStatus.ONLINE -> Pair(GHBadgeOnline, "ONLINE")
                ServerStatus.MAINTENANCE -> Pair(GHBadgeMaint, "MAINT")
                ServerStatus.OFFLINE -> Pair(GHBadgeOffline, "OFFLINE")
                ServerStatus.BUSY -> Pair(GHBadgeMaint, "BUSY")
            }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(stColor).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(stText, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }

            // Bottom: A button + "Launch Game" / "Install" text (ON the cover)
            Row(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GHBtn("A")
                Spacer(Modifier.width(8.dp))
                Text(if (isInstalled) "Launch Game" else "Install", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }

            // Title (bottom, above A button)
            Text(game.title, color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 42.dp))
        }
    }
}

// User game card — same style
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHUserCard(userGame: UserGame, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxHeight().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = BorderStroke(1.dp, GHCardBorder)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))), contentAlignment = Alignment.Center) {
                val icon = remember(userGame.packageName) { try { context.packageManager.getApplicationIcon(userGame.packageName) } catch (_: Exception) { null } }
                if (icon != null) AsyncImage(model = icon, contentDescription = userGame.title, modifier = Modifier.size(72.dp), contentScale = ContentScale.Fit)
                else Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(56.dp))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(0.7f)))))
            // Bottom: A button + text
            Row(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GHBtn("A")
                Spacer(Modifier.width(8.dp))
                Text(if (isInstalled) "Launch Game" else "Install", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
            Text(userGame.title, color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 42.dp))
        }
    }
}

// Context menu
@Composable
private fun GHContextMenu(pkg: String, isUser: Boolean, installed: Boolean, onDismiss: () -> Unit, onLaunch: () -> Unit, onView: () -> Unit, onUninstall: () -> Unit, onRemove: () -> Unit, onClear: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Game Options", color = GHTextWhite, fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (installed) { GHMenuItem(Icons.Rounded.PlayArrow, "Launch Game", onLaunch) }
        GHMenuItem(Icons.Rounded.Info, "View Details", onView)
        if (!isUser && installed) { GHMenuItem(Icons.Rounded.CleaningServices, "Clear Data Only", onClear) }
        if (isUser) { if (installed) { GHMenuItem(Icons.Rounded.Delete, "Uninstall Game", onUninstall) }; GHMenuItem(Icons.Rounded.RemoveCircle, "Remove from Library", onRemove) }
    } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GHTextSoft) } }, containerColor = Color(0xFF111111))
}

@Composable
private fun GHMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(14.dp)); Text(label, color = GHTextWhite, fontSize = 14.sp, fontFamily = InterFontFamily)
    }
}

// Gamepad settings
@Composable
private fun GHSettingsOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var gpCount by remember { mutableStateOf(0) }
    var gpNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { val ds = InputDevice.getDeviceIds(); val gps = mutableListOf<String>(); for (id in ds) { val d = InputDevice.getDevice(id); if (d != null) { val s = d.sources; if (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) gps.add(d.name) } }; gpCount = gps.size; gpNames = gps } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("GameHub Settings", color = GHTextWhite, fontWeight = FontWeight.Black) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("GAMEPAD", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF111111), border = BorderStroke(1.dp, GHCardBorder)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = if (gpCount > 0) GHBadgeOnline else GHTextDim, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Gamepad Connection", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(if (gpCount > 0) "$gpCount gamepad(s) connected" else "No gamepad detected", color = if (gpCount > 0) GHBadgeOnline else GHTextDim, fontSize = 12.sp) }
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (gpCount > 0) GHBadgeOnline else GHTextDim))
                    }
                    if (gpNames.isNotEmpty()) { Spacer(Modifier.height(12.dp)); gpNames.forEach { n -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) { Box(Modifier.size(6.dp).clip(CircleShape).background(GHBadgeOnline)); Spacer(Modifier.width(8.dp)); Text(n, color = GHTextSoft, fontSize = 12.sp) } } }
                    Spacer(Modifier.height(12.dp)); Text("Connect a Bluetooth or USB gamepad. Games with gamepad support will use it automatically.", color = GHTextDim, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            Text("DISPLAY", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF111111), border = BorderStroke(1.dp, GHCardBorder)) {
                Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Auto Rotate", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text("Landscape mode when entering GameHub", color = GHTextDim, fontSize = 12.sp) }; Box(Modifier.size(24.dp).clip(CircleShape).background(GHBadgeOnline), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp)) } } }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = GHAccentBlue, fontWeight = FontWeight.Bold) } }, containerColor = Color(0xFF0A0A1A))
}
