package com.drmacze.f16launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.input.InputManager
import android.media.AudioManager
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════════════════
// DLAVIE GAMEHUB — PlayStation-style transition + solid Melonx-inspired design
// ═══════════════════════════════════════════════════════════════════════════

private val GHBlack = Color(0xFF000000)
private val GHSurface = Color(0xFF0A0A0A)
private val GHSurfaceHi = Color(0xFF121212)
private val GHSurfaceCard = Color(0xFF1A1A1A)
private val GHTextWhite = Color(0xFFFFFFFF)
private val GHTextSoft = Color(0xFFAAAAAA)
private val GHTextDim = Color(0xFF666666)
private val GHStroke = Color(0x15FFFFFF)
private val GHAccent = Color(0xFF00D4FF)
private val GHAccentGreen = Color(0xFF00D26A)
private val GHAccentAmber = Color(0xFFFFAA00)
private val GHAccentRed = Color(0xFFFF5252)
private val GHAccentViolet = Color(0xFF7C4DFF)

// ─── Sound Manager ───────────────────────────────────────────────────────────
@Composable
fun rememberGameHubSound(): GameHubSound {
    val context = LocalContext.current
    return remember { GameHubSound(context) }
}

class GameHubSound(private val context: Context) {
    private val soundPool = SoundPool.Builder().setMaxStreams(4).build()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var enterSoundId = 0
    private var clickSoundId = 0
    private var loaded = false

    init {
        // Generate simple beep sounds programmatically (no external file needed)
        soundPool.setOnLoadCompleteListener { _, _, status -> loaded = true }
    }

    fun playEnter() {
        try {
            // Use system notification sound as fallback
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(100)
            }
        } catch (_: Exception) { }
    }

    fun playClick() {
        try {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(30, 50))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(30)
            }
        } catch (_: Exception) { }
    }
}

// ─── PlayStation-style Transition ────────────────────────────────────────────
@Composable
fun GameHubTransition(
    visible: Boolean,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val sound = rememberGameHubSound()

    // Animation states
    var phase by remember { mutableStateOf(0) }  // 0=hidden, 1=logo in, 2=logo hold, 3=logo out

    LaunchedEffect(visible) {
        if (visible) {
            phase = 1
            sound.playEnter()
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
            Modifier
                .fillMaxSize()
                .background(GHBlack)
            contentAlignment = Alignment.Center
        ) {
            // Logo scale animation
            val scale by animateFloatAsState(
                targetValue = when (phase) {
                    1 -> 1f    // scale up
                    2 -> 1f    // hold
                    3 -> 1.5f  // scale out
                    else -> 0f
                },
                animationSpec = tween(
                    durationMillis = when (phase) {
                        1 -> 400
                        3 -> 400
                        else -> 0
                    },
                    easing = FastOutSlowInEasing
                ),
                label = "logo_scale"
            )

            // Logo alpha
            val alpha by animateFloatAsState(
                targetValue = when (phase) {
                    1 -> 1f
                    2 -> 1f
                    3 -> 0f
                    else -> 0f
                },
                animationSpec = tween(
                    durationMillis = when (phase) {
                        1 -> 300
                        3 -> 400
                        else -> 0
                    },
                    easing = FastOutSlowInEasing
                ),
                label = "logo_alpha"
            )

            // Glow effect
            val glowSize by animateFloatAsState(
                targetValue = if (phase == 2) 1.2f else 1f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "glow"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.alpha = alpha
                }
            ) {
                // DL logo badge
                Box(
                    Modifier
                        .size(80.dp * glowSize)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GHTextWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DL", color = GHBlack, fontSize = 32.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(16.dp))
                // "DLAVIE" text with letter-spacing
                Text(
                    "DLAVIE",
                    color = GHTextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "GAMEHUB",
                    color = GHTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 4.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN: DLavie GameHub (solid, Melonx-inspired)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DLavieGameHub(
    onNav: (Page) -> Unit,
    onGameClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sound = rememberGameHubSound()

    // ── Transition state ──
    var showTransition by remember { mutableStateOf(true) }

    // ── Auto-rotate to landscape ──
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ── Category tabs ──
    var selectedTab by remember { mutableStateOf(0) }  // 0=DLavie, 1=My Library

    // ── Settings overlay ──
    var showSettings by remember { mutableStateOf(false) }

    // ── Server status ──
    var fifa16Status by remember { mutableStateOf(ServerStatus.ONLINE) }
    var fifa15Status by remember { mutableStateOf(ServerStatus.MAINTENANCE) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val api = CommunityApi(context)
                val config = api.getAppConfig("game_server_status")
                val fifa16 = config.optString("fifa16", "online").lowercase()
                val fifa15 = config.optString("fifa15", "maintenance").lowercase()
                fifa16Status = when (fifa16) {
                    "online" -> ServerStatus.ONLINE
                    "maintenance" -> ServerStatus.MAINTENANCE
                    "offline" -> ServerStatus.OFFLINE
                    else -> ServerStatus.ONLINE
                }
                fifa15Status = when (fifa15) {
                    "online" -> ServerStatus.ONLINE
                    "maintenance" -> ServerStatus.MAINTENANCE
                    "offline" -> ServerStatus.OFFLINE
                    else -> ServerStatus.MAINTENANCE
                }
            }
        }
    }

    // ── DLavie official games ──
    val dlavieGames = remember(fifa16Status, fifa15Status) {
        listOf(
            GameItem(
                title = "FIFA 16 Mobile",
                subtitle = "DLavie 26 Mod",
                packageName = GAME_PKG_16,
                mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                coverText = "DL",
                coverImageRes = R.drawable.fifa16_cover,
                serverStatus = fifa16Status,
                description = "FIFA 16 Mobile dengan mod DLavie 26",
                version = "v26.0", sizeMb = "34 MB",
                apkUrl = FIFA16_APK_URL
            ),
            GameItem(
                title = "FIFA 15 Mobile",
                subtitle = "DLavie 15 Mod",
                packageName = GAME_PKG_15,
                mainActivity = FIFA15_MAIN_ACTIVITY,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15",
                coverImageRes = R.drawable.fifa15_cover,
                serverStatus = fifa15Status,
                description = "FIFA 15 Mobile dengan mod DLavie 15",
                version = "v15.0", sizeMb = "22 MB",
                apkUrl = FIFA15_APK_URL
            )
        )
    }

    // ── User games ──
    var userGames by remember { mutableStateOf<List<UserGame>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(refreshTrigger) {
        userGames = withContext(Dispatchers.IO) { loadUserGames(context) }
    }

    // ── APK file picker ──
    val apkPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    // ── Context menu ──
    var contextMenuGame by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // ── Transition overlay ──
    GameHubTransition(visible = showTransition) {
        showTransition = false
    }

    Box(Modifier.fillMaxSize().background(GHBlack)) {
        Column(Modifier.fillMaxSize()) {
            // ── Top Bar (solid, minimal) ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(onClick = {
                    sound.playClick()
                    onNav(Page.Home)
                }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = GHTextWhite, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                // Title
                Column(Modifier.weight(1f)) {
                    Text("GameHub", color = GHTextWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text("${dlavieGames.size + userGames.size} games · Cloud Gaming", color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily)
                }
                // Settings button
                IconButton(onClick = {
                    sound.playClick()
                    showSettings = true
                }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = GHTextSoft, modifier = Modifier.size(22.dp))
                }
            }

            // ── Category Tabs (solid pill style) ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GHCategoryTab("DLavie", dlavieGames.size, selectedTab == 0, { sound.playClick(); selectedTab = 0 }, Modifier.weight(1f))
                GHCategoryTab("My Library", userGames.size, selectedTab == 1, { sound.playClick(); selectedTab = 1 }, Modifier.weight(1f))
            }

            // ── Game Library ──
            if (selectedTab == 0) {
                // DLavie official
                Text(
                    "Official Games",
                    color = GHTextSoft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                LazyRow(
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(dlavieGames) { game ->
                        GHGameCard(
                            game = game,
                            isInstalled = isPackageInstalled(context, game.packageName),
                            onClick = { sound.playClick(); onGameClick(game.packageName) },
                            onLongClick = { contextMenuGame = Pair(game.packageName, false) }
                        )
                    }
                }
                // Info
                Text(
                    "DLavie official games — cannot be uninstalled. Clear data after install if needed.",
                    color = GHTextDim,
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            } else {
                // My Library
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Games", color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    Button(
                        onClick = { sound.playClick(); apkPickerLauncher.launch("application/vnd.android.package-archive") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GHAccent, contentColor = Color(0xFF00111D))
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Game", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }

                if (userGames.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f).padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No games yet", color = GHTextSoft, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(6.dp))
                            Text("Tap 'Add Game' to import an APK from your device", color = GHTextDim, fontSize = 12.sp, fontFamily = InterFontFamily, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyRow(
                        Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { GHAddGameCard { sound.playClick(); apkPickerLauncher.launch("application/vnd.android.package-archive") } }
                        items(userGames) { userGame ->
                            GHUserGameCard(
                                userGame = userGame,
                                isInstalled = isPackageInstalled(context, userGame.packageName),
                                onClick = {
                                    sound.playClick()
                                    if (isPackageInstalled(context, userGame.packageName)) {
                                        launchGame(context, userGame.packageName)
                                    } else {
                                        val file = File(userGame.sourcePath)
                                        if (file.exists()) {
                                            try {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
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
        }

        // ── Context menu ──
        contextMenuGame?.let { (pkg, isUser) ->
            GHContextMenu(
                packageName = pkg, isUserGame = isUser,
                isInstalled = isPackageInstalled(context, pkg),
                onDismiss = { contextMenuGame = null },
                onLaunch = { contextMenuGame = null; launchGame(context, pkg) },
                onViewDetails = { contextMenuGame = null; onGameClick(pkg) },
                onUninstall = {
                    contextMenuGame = null
                    try { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
                },
                onRemoveFromLibrary = {
                    contextMenuGame = null
                    val updated = userGames.filter { it.packageName != pkg }
                    saveUserGames(context, updated)
                    userGames = updated
                },
                onClearData = {
                    contextMenuGame = null
                    try { File("/sdcard/Android/data/$pkg").deleteRecursively() } catch (_: Exception) { }
                }
            )
        }

        // ── Settings overlay ──
        if (showSettings) {
            GHSettingsOverlay(
                onDismiss = { showSettings = false }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GHCategoryTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GHTextWhite.copy(0.08f) else GHSurface,
        border = BorderStroke(1.dp, if (selected) GHTextWhite.copy(0.2f) else GHStroke)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(label, color = if (selected) GHTextWhite else GHTextSoft, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, fontFamily = InterFontFamily)
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), color = if (selected) GHAccent else GHTextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHGameCard(game: GameItem, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier.width(220.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GHSurfaceCard),
        border = BorderStroke(1.dp, GHStroke)
    ) {
        Column {
            // Cover
            Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Brush.linearGradient(game.coverGradient))) {
                if (game.coverImageRes != null) {
                    Image(painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes), contentDescription = game.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(game.coverText, color = GHTextWhite, fontSize = 36.sp, fontWeight = FontWeight.Black) }
                }
                // Status badge
                val statusColor = when (game.serverStatus) { ServerStatus.ONLINE -> GHAccentGreen; ServerStatus.MAINTENANCE -> GHAccentAmber; ServerStatus.OFFLINE -> GHAccentRed; ServerStatus.BUSY -> GHAccentAmber }
                val statusText = when (game.serverStatus) { ServerStatus.ONLINE -> "ONLINE"; ServerStatus.MAINTENANCE -> "MAINT"; ServerStatus.OFFLINE -> "OFFLINE"; ServerStatus.BUSY -> "BUSY" }
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(statusColor.copy(0.85f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(statusText, color = GHBlack, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                if (isInstalled) {
                    Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp).clip(CircleShape).background(GHAccentGreen), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = GHBlack, modifier = Modifier.size(16.dp))
                    }
                }
            }
            // Info
            Column(Modifier.padding(14.dp)) {
                Text(game.title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(game.subtitle, color = GHTextDim, fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isInstalled) GHAccentGreen else GHTextWhite, contentColor = if (isInstalled) Color(0xFF00150B) else GHBlack), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isInstalled) "Play" else "Install", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GHUserGameCard(userGame: UserGame, isInstalled: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.width(220.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GHSurfaceCard),
        border = BorderStroke(1.dp, GHStroke)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))), contentAlignment = Alignment.Center) {
                val iconDrawable = remember(userGame.packageName) { try { context.packageManager.getApplicationIcon(userGame.packageName) } catch (_: Exception) { null } }
                if (iconDrawable != null) {
                    AsyncImage(model = iconDrawable, contentDescription = userGame.title, modifier = Modifier.size(72.dp), contentScale = ContentScale.Fit)
                } else {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = GHTextDim, modifier = Modifier.size(56.dp))
                }
                if (isInstalled) {
                    Box(Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp).clip(CircleShape).background(GHAccentGreen), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = GHBlack, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(userGame.title, color = GHTextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(userGame.packageName.take(20), color = GHTextDim, fontSize = 10.sp, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isInstalled) GHAccentGreen else GHAccent, contentColor = if (isInstalled) Color(0xFF00150B) else Color(0xFF00111D)), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isInstalled) "Play" else "Install", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun GHAddGameCard(onClick: () -> Unit) {
    Card(modifier = Modifier.width(220.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = GHSurface), border = BorderStroke(1.dp, GHAccent.copy(0.2f))) {
        Column(Modifier.fillMaxWidth().height(220.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(GHAccent.copy(0.08f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = GHAccent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Add Game", color = GHAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
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
            if (isUserGame) {
                if (isInstalled) { GHMenuItem(Icons.Rounded.Delete, "Uninstall Game", onUninstall) }
                GHMenuItem(Icons.Rounded.RemoveCircle, "Remove from Library", onRemoveFromLibrary)
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GHTextSoft) } }, containerColor = GHSurfaceCard)
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

    // Detect connected gamepads
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
                    if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                        sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                        gamepads.add(device.name)
                    }
                }
            }
            gamepadCount = gamepads.size
            gamepadNames = gamepads
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GameHub Settings", color = GHTextWhite, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // ── Gamepad Section ──
                Text("GAMEPAD", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

                Surface(shape = RoundedCornerShape(12.dp), color = GHSurfaceCard, border = BorderStroke(1.dp, GHStroke)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = if (gamepadCount > 0) GHAccentGreen else GHTextDim,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Gamepad Connection", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (gamepadCount > 0) "$gamepadCount gamepad(s) connected" else "No gamepad detected",
                                    color = if (gamepadCount > 0) GHAccentGreen else GHTextDim,
                                    fontSize = 12.sp
                                )
                            }
                            // Status indicator
                            Box(
                                Modifier.size(10.dp).clip(CircleShape).background(
                                    if (gamepadCount > 0) GHAccentGreen else GHTextDim
                                )
                            )
                        }

                        if (gamepadNames.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            gamepadNames.forEach { name ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(GHAccentGreen))
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, color = GHTextSoft, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Connect a Bluetooth or USB gamepad to your device. Games with gamepad support will use it automatically.",
                            color = GHTextDim,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // ── Display Section ──
                Text("DISPLAY", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

                Surface(shape = RoundedCornerShape(12.dp), color = GHSurfaceCard, border = BorderStroke(1.dp, GHStroke)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ScreenRotation, contentDescription = null, tint = GHTextSoft, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Auto Rotate", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Landscape mode when entering GameHub", color = GHTextDim, fontSize = 12.sp)
                            }
                            Box(Modifier.size(24.dp).clip(CircleShape).background(GHAccentGreen), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = GHBlack, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // ── About ──
                Text("ABOUT", color = GHTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Surface(shape = RoundedCornerShape(12.dp), color = GHSurfaceCard, border = BorderStroke(1.dp, GHStroke)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("DLavie GameHub", color = GHTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Cloud Gaming Platform v7.9.79", color = GHTextDim, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = GHAccent, fontWeight = FontWeight.Bold) } },
        containerColor = GHSurface
    )
}
