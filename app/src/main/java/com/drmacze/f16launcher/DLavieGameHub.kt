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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB — Transparent, spacious, glass-effect, modern
// ═══════════════════════════════════════════════════════════════════════════

private val GHBg = Color(0xFF050508)
private val GHGlass = Color(0x10FFFFFF)
private val GHGlassHi = Color(0x18FFFFFF)
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextSoft = Color(0xFFAAAAAA)
private val GHTextDim = Color(0xFF555555)
private val GHAccent = Color(0xFF00D4FF)
private val GHGreen = Color(0xFF00D26A)
private val GHAmber = Color(0xFFFF9900)
private val GHRed = Color(0xFFFF5252)

// ─── Transition ──────────────────────────────────────────────────────────────
@Composable
fun GameHubTransition(visible: Boolean, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    LaunchedEffect(visible) {
        if (visible) { phase = 1; delay(100); phase = 2; delay(600); phase = 3; delay(400); onComplete(); phase = 0 }
    }
    if (visible && phase > 0) {
        Box(Modifier.fillMaxSize().background(GHBg), contentAlignment = Alignment.Center) {
            val s by animateFloatAsState(when (phase) { 1 -> 1f; 2 -> 1f; 3 -> 1.5f; else -> 0f }, tween(if (phase == 1 || phase == 3) 400 else 0, easing = FastOutSlowInEasing), label = "s")
            val a by animateFloatAsState(when (phase) { 1 -> 1f; 2 -> 1f; 3 -> 0f; else -> 0f }, tween(if (phase == 1 || phase == 3) 400 else 0, easing = FastOutSlowInEasing), label = "a")
            val g by animateFloatAsState(if (phase == 2) 1.15f else 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "g")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = s; scaleY = s; this.alpha = a }) {
                Box(Modifier.size(72.dp * g).clip(RoundedCornerShape(18.dp)).background(GHTextWhite), contentAlignment = Alignment.Center) { Text("DL", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(14.dp))
                Text("DLAVIE", color = GHTextWhite, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                Spacer(Modifier.height(2.dp))
                Text("GAMEHUB", color = GHTextDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp)
            }
        }
    }
}

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

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val orig = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = orig ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    var currentTime by remember { mutableStateOf(getCurrentTime(context)) }
    var batteryLevel by remember { mutableStateOf(getBatteryLevel(context)) }
    LaunchedEffect(Unit) { while (true) { currentTime = getCurrentTime(context); batteryLevel = getBatteryLevel(context); delay(30_000) } }

    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

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

    val dlavieGames = remember(fifa16Status, fifa15Status) {
        listOf(
            GameItem(title = "FIFA 16 Mobile", subtitle = "DLavie 26 Mod", packageName = GAME_PKG_16, mainActivity = "com.byfen.downloadzipsdk.MainActivity", coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)), coverText = "DL", coverImageRes = R.drawable.fifa16_cover, serverStatus = fifa16Status, description = "FIFA 16 Mobile dengan mod DLavie 26", version = "v26.0", sizeMb = "34 MB", apkUrl = FIFA16_APK_URL),
            GameItem(title = "FIFA 15 Mobile", subtitle = "DLavie 15 Mod", packageName = GAME_PKG_15, mainActivity = FIFA15_MAIN_ACTIVITY, coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)), coverText = "D15", coverImageRes = R.drawable.fifa15_cover, serverStatus = fifa15Status, description = "FIFA 15 Mobile dengan mod DLavie 15", version = "v15.0", sizeMb = "22 MB", apkUrl = FIFA15_APK_URL)
        )
    }

    var userGames by remember { mutableStateOf<List<UserGame>>(emptyList()) }
    LaunchedEffect(selectedTab) { if (selectedTab == 1) userGames = withContext(Dispatchers.IO) { loadUserGames(context) } }

    val apkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { try { context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) { } }
    }

    var contextMenuGame by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    GameHubTransition(visible = showTransition) { showTransition = false }

    Box(Modifier.fillMaxSize().background(GHBg)) {
        // ── Subtle radial glow background ──
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF0A0A14), GHBg), radius = 800f)))

        Column(Modifier.fillMaxSize()) {
            // ════════════════════════════════════════════════════════════════
            // TOP BAR — TRANSPARENT, floating, minimal
            // ════════════════════════════════════════════════════════════════
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (exit GameHub)
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Exit", tint = GHTextWhite, modifier = Modifier.size(22.dp).clickable { onNav(Page.Home) })
                Spacer(Modifier.width(20.dp))
                // LB
                GHCtrlBtn("LB")
                Spacer(Modifier.width(16.dp))
                Text("Dashboard", color = GHTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.width(16.dp))
                // RB
                GHCtrlBtn("RB")
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = GHTextSoft, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("$batteryLevel%", color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily)
                Spacer(Modifier.width(6.dp))
                Text(currentTime, color = GHTextSoft, fontSize = 12.sp, fontFamily = InterFontFamily)
            }

            // ════════════════════════════════════════════════════════════════
            // CATEGORY TABS — Small, pill-style, glass
            // ════════════════════════════════════════════════════════════════
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GHPillTab("DLavie", dlavieGames.size, selectedTab == 0, { selectedTab = 0 })
                GHPillTab("My Library", userGames.size, selectedTab == 1, { selectedTab = 1 })
                if (selectedTab == 1) {
                    Spacer(Modifier.weight(1f))
                    // Mini add button
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(GHAccent.copy(0.15f)).clickable { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Add, contentDescription = "Add", tint = GHAccent, modifier = Modifier.size(18.dp)) }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // GAME CARDS — Centered, NOT filling height, glass effect
            // ════════════════════════════════════════════════════════════════
            Box(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                if (selectedTab == 0) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(dlavieGames) { game ->
                            GHGlassCard(
                                game = game,
                                isInstalled = isPackageInstalled(context, game.packageName),
                                onClick = { onGameClick(game.packageName) },
                                onLongClick = { contextMenuGame = Pair(game.packageName, false) }
                            )
                        }
                    }
                } else {
                    if (userGames.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No games yet", color = GHTextSoft, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap + to import an APK", color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 40.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(userGames) { userGame ->
                                GHGlassUserCard(
                                    userGame = userGame,
                                    isInstalled = isPackageInstalled(context, userGame.packageName),
                                    onClick = {
                                        if (isPackageInstalled(context, userGame.packageName)) launchGame(context, userGame.packageName)
                                        else { val f = File(userGame.sourcePath); if (f.exists()) { try { val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", f); context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) { } } }
                                    },
                                    onLongClick = { contextMenuGame = Pair(userGame.packageName, true) }
                                )
                            }
                        }
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // BOTTOM BAR — TRANSPARENT, minimal floating
            // ════════════════════════════════════════════════════════════════
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHCtrlBtn("Y")
                    Spacer(Modifier.width(8.dp))
                    Text("Search", color = GHTextSoft, fontSize = 13.sp, fontFamily = InterFontFamily)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showSettings = true }) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = GHTextSoft, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Menu", color = GHTextSoft, fontSize = 13.sp, fontFamily = InterFontFamily)
                }
            }
        }

        // Context menu
        contextMenuGame?.let { (pkg, isUser) ->
            GHContextMenu(pkg, isUser, isPackageInstalled(context, pkg), { contextMenuGame = null }, { contextMenuGame = null; launchGame(context, pkg) }, { contextMenuGame = null; onGameClick(pkg) }, { contextMenuGame = null; try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { } }, { contextMenuGame = null; val u = userGames.filter { it.packageName != pkg }; saveUserGames(context, u); userGames = u }, { contextMenuGame = null; try { File("/sdcard/Android/data/$pkg").deleteRecursively() } catch (_: Exception) { } })
        }
        if (showSettings) { GHSettingsOverlay { showSettings = false } }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHCtrlBtn(label: String) {
    Box(Modifier.size(28.dp).clip(RoundedCornerShape(5.dp)).background(GHGlass).border(1.dp, GHTextDim.copy(0.4f), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
        Text(label, color = GHTextSoft, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
    }
}

@Composable
private fun GHPillTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) GHGlassHi else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = if (selected) GHTextWhite else GHTextDim, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontFamily = InterFontFamily)
            Spacer(Modifier.width(5.dp))
            Text(count.toString(), color = if (selected) GHAccent else GHTextDim.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGlassCard(game: GameItem, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.width(200.dp).height(280.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x08FFFFFF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, GHGlass)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Cover
            if (game.coverImageRes != null) {
                Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(game.coverGradient)), contentAlignment = Alignment.Center) { Text(game.coverText, color = GHTextWhite, fontSize = 36.sp, fontWeight = FontWeight.Black) }
            }
            // Glass gradient overlay
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.2f), Color.Transparent, Color.Black.copy(0.75f)))))

            // Status badge
            val (sc, st) = when (game.serverStatus) { ServerStatus.ONLINE -> Pair(GHGreen, "ONLINE"); ServerStatus.MAINTENANCE -> Pair(GHAmber, "MAINT"); ServerStatus.OFFLINE -> Pair(GHRed, "OFFLINE"); ServerStatus.BUSY -> Pair(GHAmber, "BUSY") }
            Box(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(sc).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(st, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black) }

            // Bottom: A button + text
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(game.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(game.subtitle, color = GHTextSoft, fontSize = 10.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHCtrlBtn("A")
                    Spacer(Modifier.width(6.dp))
                    Text(if (isInstalled) "Launch" else "Install", color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGlassUserCard(userGame: UserGame, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.width(200.dp).height(280.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x08FFFFFF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, GHGlass)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))), contentAlignment = Alignment.Center) {
                val icon = remember(userGame.packageName) { try { context.packageManager.getApplicationIcon(userGame.packageName) } catch (_: Exception) { null } }
                if (icon != null) AsyncImage(model = icon, contentDescription = userGame.title, modifier = Modifier.size(64.dp), contentScale = ContentScale.Fit)
                else Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(48.dp))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.2f), Color.Transparent, Color.Black.copy(0.75f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(userGame.title, color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(userGame.packageName.take(20), color = GHTextSoft, fontSize = 10.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GHCtrlBtn("A")
                    Spacer(Modifier.width(6.dp))
                    Text(if (isInstalled) "Launch" else "Install", color = GHTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                }
            }
        }
    }
}

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

@Composable
private fun GHSettingsOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var gpCount by remember { mutableStateOf(0) }
    var gpNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { val ds = InputDevice.getDeviceIds(); val gps = mutableListOf<String>(); for (id in ds) { val d = InputDevice.getDevice(id); if (d != null) { val s = d.sources; if (s and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD || s and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) gps.add(d.name) } }; gpCount = gps.size; gpNames = gps } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("GameHub Settings", color = GHTextWhite, fontWeight = FontWeight.Black) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("GAMEPAD", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0A0A0A), border = androidx.compose.foundation.BorderStroke(1.dp, GHGlass)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = if (gpCount > 0) GHGreen else GHTextDim, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text("Gamepad Connection", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(if (gpCount > 0) "$gpCount gamepad(s) connected" else "No gamepad detected", color = if (gpCount > 0) GHGreen else GHTextDim, fontSize = 12.sp) }
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (gpCount > 0) GHGreen else GHTextDim))
                    }
                    if (gpNames.isNotEmpty()) { Spacer(Modifier.height(12.dp)); gpNames.forEach { n -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) { Box(Modifier.size(6.dp).clip(CircleShape).background(GHGreen)); Spacer(Modifier.width(8.dp)); Text(n, color = GHTextSoft, fontSize = 12.sp) } } }
                    Spacer(Modifier.height(12.dp)); Text("Connect a Bluetooth or USB gamepad. Games with gamepad support will use it automatically.", color = GHTextDim, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = GHAccent, fontWeight = FontWeight.Bold) } }, containerColor = Color(0xFF080810))
}
