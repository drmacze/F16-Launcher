package com.drmacze.f16launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════
// PLAY STORE STYLE GAME HUB — Landscape, horizontal scroll, 2 categories
// ═══════════════════════════════════════════════════════════════════════════

// Color constants dari TapTapDesignSystem.kt (PureBlack, Surface1, Surface2, TextWhite,
// SoftText, SubText, GlassStroke, AccentGreen sudah ada di package)
private val AccentCyan = Color(0xFF00D4FF)
private val AccentAmber = Color(0xFFFFAA00)
private val AccentRed = Color(0xFFFF5252)

// ─── User Game data model ────────────────────────────────────────────────────
data class UserGame(
    val packageName: String,
    val title: String,
    val sourcePath: String,   // APK file path atau "installed"
    val addedAt: Long
)

// ─── User Game storage (SharedPreferences) ───────────────────────────────────
private fun loadUserGames(context: Context): List<UserGame> {
    val prefs = context.getSharedPreferences("dlavie_user_games", Context.MODE_PRIVATE)
    val json = prefs.getString("games", "[]") ?: "[]"
    val arr = JSONArray(json)
    val result = mutableListOf<UserGame>()
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        result.add(UserGame(
            packageName = o.optString("packageName"),
            title = o.optString("title"),
            sourcePath = o.optString("sourcePath"),
            addedAt = o.optLong("addedAt")
        ))
    }
    return result
}

private fun saveUserGames(context: Context, games: List<UserGame>) {
    val arr = JSONArray()
    games.forEach { g ->
        arr.put(JSONObject().apply {
            put("packageName", g.packageName)
            put("title", g.title)
            put("sourcePath", g.sourcePath)
            put("addedAt", g.addedAt)
        })
    }
    context.getSharedPreferences("dlavie_user_games", Context.MODE_PRIVATE)
        .edit().putString("games", arr.toString()).apply()
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) { false }
}

private fun getAppLabel(context: Context, packageName: String): String {
    return try {
        val pi = context.packageManager.getPackageInfo(packageName, 0)
        context.packageManager.getApplicationLabel(pi.applicationInfo!!).toString()
    } catch (_: Exception) { packageName }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAIN: Play Store Style Game Hub
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PlayStoreGameHub(
    onNav: (Page) -> Unit,
    onGameClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Auto-rotate to landscape when entering GameHub ──
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ── Category tabs ──
    var selectedTab by remember { mutableStateOf(0) }  // 0 = DLavie, 1 = My Library

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

    // ── DLavie official games (hardcoded, can't delete) ──
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
                version = "v26.0",
                sizeMb = "34 MB",
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
                version = "v15.0",
                sizeMb = "22 MB",
                apkUrl = FIFA15_APK_URL
            )
        )
    }

    // ── User games (My Library) ──
    var userGames by remember { mutableStateOf<List<UserGame>>(emptyList()) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        userGames = loadUserGames(context)
    }

    // ── APK file picker for "Add Game" ──
    var showAddGameDialog by remember { mutableStateOf(false) }
    val apkPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Install APK from URI
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    // ── Long press context menu ──
    var contextMenuGame by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // (packageName, isUserGame)

    Box(Modifier.fillMaxSize().background(PureBlack)) {
        Column(Modifier.fillMaxSize()) {
            // ── Header ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF64B5F6)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DL", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("GameHub", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text("Cloud Gaming Platform", color = SubText, fontSize = 11.sp, fontFamily = InterFontFamily)
                }
                // Live badge
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(AccentGreen.copy(0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                        Spacer(Modifier.width(6.dp))
                        Text("LIVE", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Category Tabs (Play Store style) ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryTab(
                    label = "DLavie",
                    count = dlavieGames.size,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                CategoryTab(
                    label = "My Library",
                    count = userGames.size,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Game Library (horizontal scroll, Play Store style) ──
            if (selectedTab == 0) {
                // ── DLavie official games ──
                LazyRow(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(dlavieGames) { game ->
                        PlayStoreGameCard(
                            game = game,
                            isInstalled = isPackageInstalled(context, game.packageName),
                            isUserGame = false,
                            onClick = { onGameClick(game.packageName) },
                            onLongClick = { contextMenuGame = Pair(game.packageName, false) }
                        )
                    }
                }

                // Section: installed games info
                Text(
                    "Official DLavie games — cannot be deleted. Data can be cleared after install.",
                    color = SubText,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else {
                // ── My Library (user games) ──
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Games",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    // Add Game button
                    Button(
                        onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = Color(0xFF00111D)
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Game", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                if (userGames.isEmpty()) {
                    // Empty state
                    Box(
                        Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = SubText.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No games yet", color = SoftText, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap 'Add Game' to import an APK from your device", color = SubText, fontSize = 12.sp, fontFamily = InterFontFamily)
                        }
                    }
                } else {
                    LazyRow(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Add game card (first item)
                        item {
                            AddGameCard(onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") })
                        }
                        items(userGames) { userGame ->
                            val installed = isPackageInstalled(context, userGame.packageName)
                            PlayStoreUserGameCard(
                                userGame = userGame,
                                isInstalled = installed,
                                onClick = {
                                    if (installed) {
                                        launchGame(context, userGame.packageName)
                                    } else {
                                        // Try to re-install from source path
                                        val file = File(userGame.sourcePath)
                                        if (file.exists()) {
                                            try {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context, "${context.packageName}.files", file)
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

        // ── Context menu (long press) ──
        contextMenuGame?.let { (pkg, isUser) ->
            GameContextMenu(
                packageName = pkg,
                isUserGame = isUser,
                isInstalled = isPackageInstalled(context, pkg),
                onDismiss = { contextMenuGame = null },
                onLaunch = {
                    contextMenuGame = null
                    launchGame(context, pkg)
                },
                onViewDetails = {
                    contextMenuGame = null
                    onGameClick(pkg)
                },
                onUninstall = {
                    contextMenuGame = null
                    try {
                        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) { }
                },
                onRemoveFromLibrary = {
                    contextMenuGame = null
                    val updated = userGames.filter { it.packageName != pkg }
                    saveUserGames(context, updated)
                    userGames = updated
                },
                onClearData = {
                    contextMenuGame = null
                    // For DLavie games: clear data (not uninstall)
                    try {
                        val dataDir = File("/sdcard/Android/data/$pkg")
                        if (dataDir.exists()) dataDir.deleteRecursively()
                    } catch (_: Exception) { }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun CategoryTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "tab_bg")
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) TextWhite.copy(alpha = 0.1f) else Surface2,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) TextWhite.copy(0.3f) else GlassStroke
        )
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                color = if (selected) TextWhite else SoftText,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.width(8.dp))
            Text(
                count.toString(),
                color = if (selected) AccentCyan else SubText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayStoreGameCard(
    game: GameItem,
    isInstalled: Boolean,
    isUserGame: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .width(200.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassStroke)
    ) {
        Column {
            // Cover image
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(game.coverGradient.let { Brush.linearGradient(it) })
            ) {
                if (game.coverImageRes != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = game.coverImageRes),
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(game.coverText, color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Server status badge
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (game.serverStatus) {
                                ServerStatus.ONLINE -> AccentGreen.copy(0.8f)
                                ServerStatus.MAINTENANCE -> AccentAmber.copy(0.8f)
                                ServerStatus.OFFLINE -> AccentRed.copy(0.8f)
                                ServerStatus.BUSY -> AccentAmber.copy(0.8f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        when (game.serverStatus) {
                            ServerStatus.ONLINE -> "ONLINE"
                            ServerStatus.MAINTENANCE -> "MAINT"
                            ServerStatus.OFFLINE -> "OFFLINE"
                            ServerStatus.BUSY -> "BUSY"
                        },
                        color = Color.Black,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Installed badge
                if (isInstalled) {
                    Box(
                        Modifier.align(Alignment.TopStart).padding(8.dp)
                            .size(24.dp).clip(CircleShape).background(AccentGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Info
            Column(Modifier.padding(12.dp)) {
                Text(
                    game.title,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    game.subtitle,
                    color = SubText,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                // Play/Install button
                Button(
                    onClick = { onClick() },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInstalled) AccentGreen else TextWhite,
                        contentColor = if (isInstalled) Color(0xFF00150B) else Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isInstalled) "Play" else "Install",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayStoreUserGameCard(
    userGame: UserGame,
    isInstalled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .width(200.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassStroke)
    ) {
        Column {
            // Cover — try to get app icon
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
                contentAlignment = Alignment.Center
            ) {
                // Try to load app icon
                val iconDrawable = remember(userGame.packageName) {
                    try {
                        context.packageManager.getApplicationIcon(userGame.packageName)
                    } catch (_: Exception) { null }
                }
                if (iconDrawable != null) {
                    AsyncImage(
                        model = iconDrawable,
                        contentDescription = userGame.title,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null, tint = SubText, modifier = Modifier.size(48.dp))
                }

                // Installed badge
                if (isInstalled) {
                    Box(
                        Modifier.align(Alignment.TopStart).padding(8.dp)
                            .size(24.dp).clip(CircleShape).background(AccentGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(Modifier.padding(12.dp)) {
                Text(
                    userGame.title,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    userGame.packageName,
                    color = SubText,
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onClick() },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInstalled) AccentGreen else AccentCyan,
                        contentColor = if (isInstalled) Color(0xFF00150B) else Color(0xFF00111D)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.InstallMobile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isInstalled) "Play" else "Install",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun AddGameCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(200.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface2),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if (true) AccentCyan.copy(0.3f) else GlassStroke
        )
    ) {
        Column(
            Modifier.fillMaxWidth().height(200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(AccentCyan.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Add Game", color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Text("Import APK from device", color = SubText, fontSize = 10.sp, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun GameContextMenu(
    packageName: String,
    isUserGame: Boolean,
    isInstalled: Boolean,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onViewDetails: () -> Unit,
    onUninstall: () -> Unit,
    onRemoveFromLibrary: () -> Unit,
    onClearData: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Game Options", color = TextWhite, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isInstalled) {
                    ContextMenuItem(Icons.Rounded.PlayArrow, "Launch Game", onLaunch)
                }
                ContextMenuItem(Icons.Rounded.Info, "View Details", onViewDetails)
                if (!isUserGame && isInstalled) {
                    ContextMenuItem(Icons.Rounded.Delete, "Clear Data Only", onClearData)
                }
                if (isUserGame) {
                    if (isInstalled) {
                        ContextMenuItem(Icons.Rounded.Delete, "Uninstall Game", onUninstall)
                    }
                    ContextMenuItem(Icons.Rounded.RemoveCircle, "Remove from Library", onRemoveFromLibrary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SoftText) }
        },
        containerColor = Surface1
    )
}

@Composable
private fun ContextMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SoftText, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextWhite, fontSize = 14.sp, fontFamily = InterFontFamily)
    }
}
