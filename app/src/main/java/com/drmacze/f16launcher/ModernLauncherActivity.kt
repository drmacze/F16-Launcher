package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// ─── Design tokens ─────────────────────────────────────────────────────────────
val Carbon      = Color(0xFF050812)
val GlassBase   = Color(0xFF0D1520)
val CandyCyan   = Color(0xFF27C8FF)
val CandyBlue   = Color(0xFF5F57FF)
val NeonGreen   = Color(0xFF1FDD90)
val SoftText    = Color(0xFFA0B0C8)
val GlassStroke = Color(0x445D8DFF)
val DangerRed   = Color(0xFFFF4D6D)
val AmberWarn   = Color(0xFFFFB830)

// ─── Constants ────────────────────────────────────────────────────────────────
private const val GAME_PKG         = "com.ea.gp.fifaworld"
private const val DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json"
private const val MARKER_PATH      = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private const val LOCAL_VER        = 1
private const val LOCAL_VER_NAME   = "v1"

// ─── Data models ──────────────────────────────────────────────────────────────
data class CategoryItem(val id: String, val name: String, val description: String)
data class TopicItem(val id: String, val title: String, val body: String, val replyCount: Int, val createdAt: String)
data class PostItem(val id: String, val authorId: String, val body: String, val createdAt: String)
data class FeedItem(val id: String, val title: String, val body: String, val type: String, val pinned: Boolean, val official: Boolean)
data class UpdateInfo(val latestCode: Int, val latestName: String, val upToDate: Boolean, val releaseNotes: List<String>)

// ─── Navigation ───────────────────────────────────────────────────────────────
enum class Page(val label: String, val navIcon: ImageVector) {
    Home("Home",  Icons.Rounded.Home),
    Data("Data",  Icons.Rounded.FolderOpen),
    Chat("Chat",  Icons.Rounded.Forum),
    Me  ("Me",    Icons.Rounded.AccountCircle)
}

// ─── Activity ─────────────────────────────────────────────────────────────────
class ModernLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DLavieModernApp() }
    }
}

// ─── Root composable ──────────────────────────────────────────────────────────
@Composable
fun DLavieModernApp() {
    val context = LocalContext.current
    val api     = remember { CommunityApi(context) }
    MaterialTheme(colorScheme = darkColorScheme(
        background   = Carbon,     surface     = GlassBase,
        primary      = CandyCyan,  secondary   = CandyBlue,
        onPrimary    = Color(0xFF00111D), onSecondary = Color.White,
        onBackground = Color.White, onSurface   = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Carbon) {
            Box(Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF040A16), Carbon, Color(0xFF060D18))))) {
                if (!api.loggedIn()) {
                    LaunchedEffect(Unit) {
                        context.startActivity(
                            Intent(context, DLavieGuidedActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CircularProgressIndicator(color = CandyCyan, strokeWidth = 2.5.dp)
                            Text("Sesi berakhir, mengarahkan ke login...", color = SoftText, fontSize = 13.sp)
                        }
                    }
                } else {
                    MainShell(api) {
                        api.logout()
                        context.getSharedPreferences("dlavie_auth_session", Context.MODE_PRIVATE).edit().clear().apply()
                        context.startActivity(
                            Intent(context, DLavieGuidedActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        }
    }
}

// ─── Main shell ───────────────────────────────────────────────────────────────
@Composable
fun MainShell(api: CommunityApi, onLogout: () -> Unit) {
    var page by remember { mutableStateOf(Page.Home) }

    // Token auto-refresh every 50 min
    LaunchedEffect(Unit) {
        while (true) {
            delay(50L * 60_000)
            withContext(Dispatchers.IO) { runCatching { api.refreshToken() } }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState  = page,
            label        = "page_transition",
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            modifier     = Modifier.fillMaxSize().padding(bottom = 92.dp)
        ) { target ->
            when (target) {
                Page.Home -> HomeScreen(api, onNav = { page = it })
                Page.Data -> DataScreen(onNav  = { page = it })
                Page.Chat -> CommunityScreen(api)
                Page.Me   -> ProfileScreen(api, onLogout)
            }
        }
        FloatingNav(
            page     = page,
            onPage   = { page = it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
        )
    }
}

// ─── Floating navigation bar ──────────────────────────────────────────────────
@Composable
fun FloatingNav(page: Page, onPage: (Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier       = modifier.widthIn(max = 620.dp).padding(horizontal = 14.dp),
        shape          = RoundedCornerShape(36.dp),
        color          = Color(0xE60B1422),
        border         = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 20.dp,
        tonalElevation = 0.dp
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Page.values().forEach { item ->
                val selected  = page == item
                val iconAlpha by animateFloatAsState(if (selected) 1f else 0.5f, label = "nav_alpha")
                val scale     by animateFloatAsState(if (selected) 1f else 0.92f, label = "nav_scale")
                Button(
                    onClick      = { onPage(item) },
                    modifier     = Modifier.weight(1f).height(if (selected) 54.dp else 48.dp).scale(scale),
                    shape        = RoundedCornerShape(28.dp),
                    colors       = ButtonDefaults.buttonColors(
                        containerColor = if (selected) CandyBlue else Color.Transparent,
                        contentColor   = if (selected) Color.White else SoftText
                    ),
                    elevation    = ButtonDefaults.buttonElevation(if (selected) 6.dp else 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            item.navIcon,
                            contentDescription = item.label,
                            modifier           = Modifier.size(if (selected) 20.dp else 18.dp).alpha(iconAlpha)
                        )
                        Text(
                            item.label,
                            fontSize   = if (selected) 10.sp else 9.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines   = 1
                        )
                    }
                }
            }
        }
    }
}

// ─── Home screen ──────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(api: CommunityApi, onNav: (Page) -> Unit) {
    val context       = LocalContext.current
    val gameInstalled = remember { isGameInstalled(context) }
    val marker        = remember { readMarker() }
    val dataReady     = remember { marker.startsWith("v26", ignoreCase = true) }

    var updateInfo  by remember { mutableStateOf<UpdateInfo?>(null) }
    var feed        by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var loadingHome by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { updateInfo = fetchUpdateInfo() }
            runCatching { feed = parseFeed(api.feedPosts()) }
        }
        loadingHome = false
    }

    // Infinite pulse for play button glow
    val infiniteTransition = rememberInfiniteTransition(label = "play_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Header card ──────────────────────────────────────────────────────
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp)
                        .background(Brush.linearGradient(listOf(CandyCyan, CandyBlue)), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DL", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DLavie 26", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        AppBadge("PROD", NeonGreen)
                    }
                    Text("FIFA 16 Mobile 2026", color = SoftText, fontSize = 12.sp)
                }
                val name = api.displayName().ifEmpty { "Player" }.take(12)
                AppBadge("@$name", CandyCyan)
            }
            Spacer(Modifier.height(10.dp))
            Text("Football Reborn", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("Play, update, and connect — powered by DLavie.", color = SoftText, fontSize = 13.sp)
        }

        // ── Play button (animated) ────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().scale(if (gameInstalled && dataReady) pulseScale else 1f),
            contentAlignment = Alignment.Center
        ) {
            // Glow ring behind button
            if (gameInstalled && dataReady) {
                Box(
                    Modifier.fillMaxWidth().height(60.dp)
                        .background(NeonGreen.copy(alpha = glowAlpha), RoundedCornerShape(22.dp))
                )
            }
            Button(
                onClick = {
                    if (gameInstalled) launchGame(context)
                    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PKG")))
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape    = RoundedCornerShape(22.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (gameInstalled && dataReady) NeonGreen else CandyBlue.copy(0.35f),
                    contentColor   = if (gameInstalled && dataReady) Color(0xFF00150B) else Color.White
                )
            ) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = "Play", modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (gameInstalled) "Main FIFA 16" else "Download FIFA 16",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // ── Quick action row ──────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionButton("Data", Icons.Rounded.Storage, Modifier.weight(1f)) { onNav(Page.Data) }
            QuickActionButton("Update", Icons.Rounded.CloudSync, Modifier.weight(1f)) {
                context.startActivity(Intent(context, GameHubActivity::class.java))
            }
            QuickActionButton("Chat", Icons.Rounded.Forum, Modifier.weight(1f)) { onNav(Page.Chat) }
        }

        // ── Status chips ──────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusChip(
                title   = "Game",
                value   = if (gameInstalled) "Terinstall" else "Belum ada",
                ok      = gameInstalled,
                loading = false,
                icon    = if (gameInstalled) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                modifier = Modifier.weight(1f)
            ) {
                if (!gameInstalled) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PKG")))
            }
            StatusChip(
                title   = "Data",
                value   = if (dataReady) "Ready" else "Belum siap",
                ok      = dataReady,
                loading = false,
                icon    = if (dataReady) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                modifier = Modifier.weight(1f)
            ) { onNav(Page.Data) }
            StatusChip(
                title   = "Update",
                value   = if (loadingHome) "Cek..." else if (updateInfo?.upToDate != false) "Up-to-date" else "Tersedia",
                ok      = !loadingHome && updateInfo?.upToDate != false,
                loading = loadingHome,
                icon    = if (loadingHome) Icons.Rounded.Refresh else if (updateInfo?.upToDate != false) Icons.Rounded.Shield else Icons.Rounded.SystemUpdate,
                modifier = Modifier.weight(1f)
            ) { context.startActivity(Intent(context, GameHubActivity::class.java)) }
        }

        // ── Base data warning ─────────────────────────────────────────────────
        AnimatedVisibility(!dataReady, enter = fadeIn(), exit = fadeOut()) {
            GlassCard(borderColor = DangerRed.copy(0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Base Data Belum Siap", color = DangerRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Buka tab Data dan install base data FIFA 16. Patch tidak akan berfungsi tanpa data yang lengkap.",
                    color = SoftText, fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick  = { onNav(Page.Data) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Data Installer", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Update available card ─────────────────────────────────────────────
        val ui = updateInfo
        AnimatedVisibility(ui != null && !ui.upToDate, enter = fadeIn(), exit = fadeOut()) {
            if (ui != null) GlassCard(borderColor = CandyCyan.copy(0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Update Tersedia: ${ui.latestName}", color = CandyCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (ui.releaseNotes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ui.releaseNotes.take(3).forEach {
                        Text("• $it", color = SoftText, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick  = { onNav(Page.Data) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = CandyBlue)
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download Patch", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Announcements feed ────────────────────────────────────────────────
        AnimatedVisibility(feed.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pengumuman", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                feed.forEach { post ->
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(post.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (post.pinned)  AppBadge("PIN", NeonGreen)
                            if (post.official) AppBadge("OFFICIAL", CandyCyan)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(post.body, color = SoftText, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Loading indicator (shimmer)
        AnimatedVisibility(loadingHome && feed.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
            val loadAlpha by animateFloatAsState(
                targetValue = if (loadingHome) 1f else 0f,
                animationSpec = tween(400),
                label = "load_fade"
            )
            Row(
                Modifier.fillMaxWidth().alpha(loadAlpha),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = CandyCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Memuat feed...", color = SoftText, fontSize = 13.sp)
            }
        }
    }
}

// ─── Data & Patch screen ──────────────────────────────────────────────────────
@Composable
fun DataScreen(onNav: (Page) -> Unit) {
    val context       = LocalContext.current
    val gameInstalled = remember { isGameInstalled(context) }
    var marker        by remember { mutableStateOf(readMarker()) }
    val dataReady     by remember { mutableStateOf(marker.startsWith("v26", ignoreCase = true)) }

    var updateInfo  by remember { mutableStateOf<UpdateInfo?>(null) }
    var loading     by remember { mutableStateOf(true) }
    var updateError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Patch engine state
    val patchLogs   = remember { mutableStateListOf<String>() }
    var patching    by remember { mutableStateOf(false) }
    var patchStep   by remember { mutableStateOf(0) }
    var patchTotal  by remember { mutableStateOf(1) }
    var patchLabel  by remember { mutableStateOf("") }
    var patchError  by remember { mutableStateOf("") }
    var patchDone   by remember { mutableStateOf(false) }

    val engine = remember {
        DevPatchEngine(context,
            onLog      = { msg -> patchLogs.add(msg) },
            onProgress = { cur, tot, lbl -> patchStep = cur; patchTotal = tot; patchLabel = lbl }
        )
    }

    fun refreshMarker() { marker = readMarker() }

    fun checkUpdate() {
        loading = true; updateError = ""
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { fetchUpdateInfo() } }
                .fold(
                    onSuccess = { r -> r.fold({ updateInfo = it }, { updateError = it.message ?: "Gagal" }) },
                    onFailure = { updateError = it.message ?: "Gagal" }
                )
            loading = false
        }
    }

    fun applyPatch() {
        patchLogs.clear(); patchError = ""; patchDone = false; patching = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { engine.applyAvailableUpdates() } }
            result.onFailure { patchError = it.message ?: "Patch gagal" }
            result.onSuccess { patchDone = true }
            patching = false
            refreshMarker()
        }
    }

    LaunchedEffect(Unit) { checkUpdate() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Header ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DataObject, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Data & Patch", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Status data game + update patch FIFA 16 Mod.", color = SoftText, fontSize = 12.sp)
                }
            }
        }

        // ── Status grid ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("APK FIFA 16", if (gameInstalled) "Terinstall" else "Belum ada", gameInstalled, Icons.Rounded.CheckCircle, Modifier.weight(1f))
            InfoTile("Base Data", if (dataReady) "Ready" else "Belum siap", dataReady, Icons.Rounded.Storage, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("Versi Lokal", LOCAL_VER_NAME, true, Icons.Rounded.Shield, Modifier.weight(1f))
            InfoTile("Versi Terbaru", if (loading) "Cek..." else updateInfo?.latestName ?: "—",
                updateInfo?.upToDate != false, Icons.Rounded.CloudSync, Modifier.weight(1f))
        }

        // ── Marker detail ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Terminal, contentDescription = null, tint = SoftText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Data Marker", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                marker.take(100).ifBlank { "Marker belum terdeteksi — base data belum diinstall." },
                color = if (dataReady) NeonGreen else SoftText,
                fontSize = 13.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        // ── Storage detection ──
        val gamePath = remember { engine.detectGameDataPath() }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Storage Path", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(gamePath, color = Color.White, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 2)
            Text("Access: ${engine.accessMode()}", color = if (engine.accessMode().contains("aktif")) NeonGreen else AmberWarn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // ── Patch apply section ───────────────────────────────────────────────
        val ui = updateInfo
        val patchAvailable = ui != null && !ui.upToDate

        GlassCard(borderColor = when {
            patchDone  -> NeonGreen.copy(0.6f)
            patchError.isNotBlank() -> DangerRed.copy(0.5f)
            patchAvailable -> CandyCyan.copy(0.5f)
            else -> GlassStroke
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null,
                    tint = if (patchAvailable) CandyCyan else SoftText, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            patchDone  -> "Patch Berhasil Diapply"
                            patchError.isNotBlank() -> "Patch Gagal"
                            patchAvailable -> "Update Patch Tersedia"
                            loading -> "Memeriksa..."
                            else -> "Patch Terbaru"
                        },
                        color = when {
                            patchDone  -> NeonGreen
                            patchError.isNotBlank() -> DangerRed
                            patchAvailable -> CandyCyan
                            else -> Color.White
                        },
                        fontSize = 16.sp, fontWeight = FontWeight.Black
                    )
                    Text(
                        "Local $LOCAL_VER_NAME  →  Latest ${ui?.latestName ?: "..."}",
                        color = SoftText, fontSize = 12.sp
                    )
                }
            }

            // Release notes
            if (patchAvailable && ui != null && ui.releaseNotes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ui.releaseNotes.take(3).forEach {
                    Text("• $it", color = SoftText, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar while patching
            AnimatedVisibility(patching) {
                Column {
                    LinearProgressIndicator(
                        progress   = { if (patchTotal > 0) patchStep.toFloat() / patchTotal else 0f },
                        modifier   = Modifier.fillMaxWidth(),
                        color      = CandyCyan,
                        trackColor = GlassStroke
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(patchLabel, color = SoftText, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Error
            AnimatedVisibility(patchError.isNotBlank()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Error", color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(patchError, color = SoftText, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Button(
                onClick  = { applyPatch() },
                enabled  = !patching && !loading && patchAvailable,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor          = if (patchAvailable) CandyCyan else NeonGreen,
                    contentColor            = Color(0xFF00111D),
                    disabledContainerColor  = Color(0xFF1C2635),
                    disabledContentColor    = SoftText
                )
            ) {
                if (patching) {
                    CircularProgressIndicator(color = Color(0xFF00111D), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Mengapply Patch...", fontWeight = FontWeight.Black)
                } else {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        when {
                            patchDone  -> "Patch Sudah Diapply"
                            patchAvailable -> "Download & Apply Patch"
                            else -> "Versi Sudah Terbaru"
                        },
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // ── Patch log viewer ──────────────────────────────────────────────────
        AnimatedVisibility(patchLogs.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Patch", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                patchLogs.takeLast(30).forEach { line ->
                    Text(
                        "› $line",
                        color    = if (line.contains("Error", ignoreCase = true) || line.contains("gagal", ignoreCase = true))
                            DangerRed else if (line.contains("selesai", ignoreCase = true) || line.contains("OK", ignoreCase = true))
                            NeonGreen else SoftText,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        // ── Update check error ──
        AnimatedVisibility(updateError.isNotBlank()) {
            GlassCard(borderColor = DangerRed.copy(0.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gagal cek manifest", color = DangerRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Text(updateError, color = SoftText, fontSize = 12.sp)
            }
        }

        // ── Action buttons ────────────────────────────────────────────────────
        GlassCard {
            Text("Aksi Lainnya", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick  = { checkUpdate() },
                enabled  = !loading && !patching,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (loading) "Mengecek..." else "Cek Update Manifest", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { context.startActivity(Intent(context, DLavieHubActivity::class.java)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CandyBlue)
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Buka Data Installer Lengkap", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { context.startActivity(Intent(context, GameHubActivity::class.java)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF14203A))
            ) {
                Icon(Icons.Rounded.Terminal, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Advanced Shizuku Updater", fontWeight = FontWeight.Bold, color = CandyCyan)
            }
        }

        // ── Play if ready ─────────────────────────────────────────────────────
        if (dataReady && gameInstalled) {
            Button(
                onClick  = { launchGame(context) },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(18.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))
            ) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Main FIFA 16 Sekarang", fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }

        // ── Patch info ────────────────────────────────────────────────────────
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sistem Patch DLavie", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            listOf(
                Icons.Rounded.CloudDownload to ("Download" to "Manifest JSON dari GitHub, versi dibandingkan."),
                Icons.Rounded.Shield        to ("Verifikasi" to "SHA-256 dicek untuk keamanan file patch."),
                Icons.Rounded.FolderOpen    to ("Backup" to "File lama dibackup sebelum patch diapply."),
                Icons.Rounded.Storage       to ("Apply" to "File baru di-copy via Shizuku/root (cp -af, no duplikat)."),
                Icons.Rounded.Refresh       to ("Restore" to "Backup tersedia jika patch bermasalah.")
            ).forEach { (icon, item) ->
                val (title, desc) = item
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                    Icon(icon, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(title, color = CandyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(desc, color = SoftText, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ─── Community / Chat screen ───────────────────────────────────────────────────
@Composable
fun CommunityScreen(api: CommunityApi) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var topics by remember { mutableStateOf<List<TopicItem>>(emptyList()) }
    var selectedTopic by remember { mutableStateOf<TopicItem?>(null) }
    var posts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var status by remember { mutableStateOf("Memuat komunitas...") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }

    fun loadPosts(topic: TopicItem) {
        scope.launch {
            try { posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) } }
            catch (t: Throwable) { status = "Gagal load thread: ${t.message}" }
        }
    }
    fun loadTopics() {
        scope.launch {
            try {
                topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }
                status = if (topics.isEmpty()) "Belum ada topik." else "${topics.size} topik."
            } catch (t: Throwable) { status = "Gagal load topik: ${t.message}" }
        }
    }
    fun createTopic() {
        val cat = selectedCategory ?: run { status = "Pilih channel dulu."; return }
        if (title.trim().length < 4 || body.trim().isEmpty()) { status = "Judul min 4 karakter & isi wajib diisi."; return }
        scope.launch {
            try {
                val newTopic = withContext(Dispatchers.IO) { api.createTopic(cat.id, title, body) }
                title = ""; body = ""
                topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(cat.id)) }
                selectedTopic = topics.firstOrNull { it.id == newTopic.optString("id") }
                status = "Topik dibuat."
            } catch (t: Throwable) { status = "Gagal: ${t.message}" }
        }
    }
    fun sendReply() {
        val topic = selectedTopic ?: run { status = "Pilih topik dulu."; return }
        if (reply.trim().isEmpty()) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.createPost(topic.id, "", reply) }
                reply = ""
                posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) }
            } catch (t: Throwable) { status = "Gagal reply: ${t.message}" }
        }
    }

    LaunchedEffect(Unit) {
        try {
            categories    = withContext(Dispatchers.IO) { jsonCategories(api.categories()) }
            selectedCategory = categories.firstOrNull()
            topics        = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }
            status        = if (topics.isEmpty()) "Belum ada topik." else "Siap."
        } catch (t: Throwable) { status = "Community error: ${t.message}" }
    }

    Box(Modifier.fillMaxSize().padding(14.dp)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChannelPanel(categories, selectedCategory) { selectedCategory = it; selectedTopic = null; posts = emptyList(); loadTopics() }
            TopicPanel(status, title, body, topics, selectedTopic, onTitle = { title = it }, onBody = { body = it }, onCreate = { createTopic() }, onSelect = { t -> selectedTopic = t; loadPosts(t) })
            ThreadPanel(selectedTopic, posts, reply, onReply = { reply = it }, onSend = { sendReply() })
        }
    }
}

// ─── Profile / Me screen ───────────────────────────────────────────────────────
@Composable
fun ProfileScreen(api: CommunityApi, onLogout: () -> Unit) {
    val context       = LocalContext.current
    val gameInstalled = remember { isGameInstalled(context) }
    var confirmLogout by remember { mutableStateOf(false) }
    val initial = api.displayName().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    val role    = api.role()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Avatar + identity ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp)
                        .background(Brush.linearGradient(listOf(CandyCyan, CandyBlue)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(api.displayName().ifEmpty { "DLavie Player" }, fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("@${api.username().ifEmpty { "unknown" }}", color = SoftText, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    AppBadge(role.uppercase(), roleBadgeColor(role))
                }
            }
        }

        // ── Account info ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Info Akun", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            ProfileInfoLine("Username",     "@${api.username().ifEmpty { "-" }}")
            ProfileInfoLine("Display Name", api.displayName().ifEmpty { "-" })
            ProfileInfoLine("Role",         role)
            ProfileInfoLine("Server",       "Supabase Cloud")
            ProfileInfoLine("Game",         if (gameInstalled) "FIFA 16 terinstall" else "Belum terinstall")
        }

        // ── Game quick launch ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Game", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            if (gameInstalled) {
                Button(
                    onClick  = { launchGame(context) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))
                ) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Main FIFA 16", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick  = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PKG"))) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = CandyBlue)
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download FIFA 16", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Security info ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Keamanan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sesi JWT aktif — auto-refresh setiap 50 menit", color = SoftText, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Patch SHA-256 terverifikasi sebelum apply", color = SoftText, fontSize = 12.sp)
            }
        }

        // ── Logout ──
        AnimatedContent(targetState = confirmLogout, label = "logout_state") { confirm ->
            if (!confirm) {
                Button(
                    onClick  = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(0.15f), contentColor = DangerRed)
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            } else {
                GlassCard(borderColor = DangerRed.copy(0.5f)) {
                    Text("Konfirmasi Logout", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Sesi akan dihapus dan kamu harus login kembali.", color = SoftText, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = { confirmLogout = false },
                            modifier = Modifier.weight(1f),
                            border   = BorderStroke(1.dp, GlassStroke)
                        ) { Text("Batal", color = SoftText) }
                        Button(
                            onClick  = onLogout,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) { Text("Logout", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

// ─── Shared UI components ─────────────────────────────────────────────────────

@Composable
fun GlassCard(modifier: Modifier = Modifier, borderColor: Color = GlassStroke, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xCC0D1520)),
        border   = BorderStroke(1.dp, borderColor)
    ) { Column(modifier = Modifier.padding(18.dp), content = content) }
}

@Composable
fun StatusChip(
    title: String, value: String, ok: Boolean, loading: Boolean,
    icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val borderColor by androidx.compose.animation.core.animateColorAsState(
        when { loading -> GlassStroke; ok -> NeonGreen.copy(0.4f); else -> DangerRed.copy(0.4f) },
        animationSpec = tween(400), label = "chip_border"
    )
    val iconTint by androidx.compose.animation.core.animateColorAsState(
        if (ok) NeonGreen else if (loading) SoftText else DangerRed,
        animationSpec = tween(400), label = "chip_icon"
    )
    Surface(
        modifier  = modifier.height(88.dp).clickable { onClick() },
        shape     = RoundedCornerShape(20.dp),
        color     = Color(0xAA0A1422),
        border    = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            if (loading) CircularProgressIndicator(color = CandyCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, color = SoftText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, color = iconTint, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun InfoTile(label: String, value: String, ok: Boolean, icon: ImageVector, modifier: Modifier = Modifier) {
    val valueColor by androidx.compose.animation.core.animateColorAsState(
        if (ok) NeonGreen else DangerRed, animationSpec = tween(400), label = "tile_color"
    )
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = SoftText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun QuickActionButton(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(52.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF131D2E), contentColor = CandyCyan)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun AppBadge(text: String, color: Color) {
    Surface(color = color.copy(0.12f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, color.copy(0.4f))) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), maxLines = 1)
    }
}

@Composable
fun ProfileInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
        Text(value, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GlassListItem(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape     = RoundedCornerShape(18.dp),
        color     = if (selected) Color(0x5527C8FF) else Color(0x66101F34),
        border    = BorderStroke(1.dp, if (selected) CandyCyan else GlassStroke),
        modifier  = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = SoftText, fontSize = 11.sp)
        }
    }
}

@Composable
fun ChannelPanel(categories: List<CategoryItem>, selected: CategoryItem?, modifier: Modifier = Modifier, onSelect: (CategoryItem) -> Unit) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Forum, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Channels", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(10.dp))
        if (categories.isEmpty())
            Text("Memuat channels...", color = SoftText, fontSize = 12.sp)
        else categories.forEach { c ->
            OutlinedButton(
                onClick  = { onSelect(c) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                border   = BorderStroke(1.dp, if (selected?.id == c.id) CandyCyan else GlassStroke)
            ) { Text(c.name, color = if (selected?.id == c.id) CandyCyan else Color.White) }
        }
    }
}

@Composable
fun TopicPanel(
    status: String, title: String, body: String, topics: List<TopicItem>, selected: TopicItem?,
    modifier: Modifier = Modifier, onTitle: (String) -> Unit, onBody: (String) -> Unit,
    onCreate: () -> Unit, onSelect: (TopicItem) -> Unit
) {
    GlassCard(modifier) {
        Text("Topik", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(status, color = SoftText, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = title, onValueChange = onTitle, label = { Text("Judul topik baru", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = body, onValueChange = onBody, label = { Text("Isi topik", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), minLines = 2)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick  = onCreate,
            colors   = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D)),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
        ) { Text("Buat Topik Baru", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            topics.forEach { t -> GlassListItem(t.title, "${t.replyCount} replies · ${t.createdAt.take(10)}", selected?.id == t.id) { onSelect(t) } }
        }
    }
}

@Composable
fun ThreadPanel(topic: TopicItem?, posts: List<PostItem>, reply: String, modifier: Modifier = Modifier, onReply: (String) -> Unit, onSend: () -> Unit) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Forum, contentDescription = null, tint = CandyCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(topic?.title ?: "Thread", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (topic != null) Text(topic.body, color = SoftText, fontSize = 13.sp)
        else Text("Pilih topik untuk membaca dan membalas.", color = SoftText, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            posts.forEach { p -> GlassListItem("@${p.authorId.take(8)}", p.body, false) {} }
        }
        if (topic != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = reply, onValueChange = onReply, label = { Text("Tulis balasan", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = false, minLines = 2)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onSend,
                colors   = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B)),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
            ) { Text("Kirim Balasan", fontWeight = FontWeight.Bold) }
        }
    }
}

// ─── Helper functions ─────────────────────────────────────────────────────────

fun isGameInstalled(context: android.content.Context): Boolean =
    try { context.packageManager.getPackageInfo(GAME_PKG, 0); true } catch (_: Exception) { false }

fun readMarker(): String =
    try { File(MARKER_PATH).readText().trim() } catch (_: Exception) { "" }

fun fetchUpdateInfo(): UpdateInfo {
    val json      = fetchJson(DEFAULT_MANIFEST)
    val latestCode = json.optInt("latestVersionCode", LOCAL_VER)
    val latestName = json.optString("latestVersionName", "v$latestCode")
    val notesArr   = json.optJSONArray("releaseNotes")
    val notes      = if (notesArr != null) List(notesArr.length()) { i -> notesArr.optString(i) } else emptyList()
    return UpdateInfo(latestCode, latestName, latestCode <= LOCAL_VER, notes)
}

fun parseFeed(arr: JSONArray): List<FeedItem> = try {
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        FeedItem(o.optString("id"), o.optString("title"), o.optString("body"), o.optString("type", "info"), o.optBoolean("pinned"), o.optBoolean("official"))
    }
} catch (_: Exception) { emptyList() }

fun roleBadgeColor(role: String): Color = when (role.lowercase()) {
    "admin"     -> DangerRed
    "moderator" -> NeonGreen
    "vip"       -> Color(0xFFFFD700)
    else        -> CandyCyan
}

fun launchGame(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(GAME_PKG)
    if (intent != null) context.startActivity(intent)
    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PKG")))
}

fun fetchJson(url: String): JSONObject {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 20_000; c.readTimeout = 30_000
    return try { BufferedReader(InputStreamReader(c.inputStream)).use { JSONObject(it.readText()) } } finally { c.disconnect() }
}

fun jsonCategories(arr: JSONArray): List<CategoryItem> = List(arr.length()) { i ->
    val o = arr.getJSONObject(i); CategoryItem(o.optString("id"), o.optString("name"), o.optString("description")) }
fun jsonTopics(arr: JSONArray): List<TopicItem> = List(arr.length()) { i ->
    val o = arr.getJSONObject(i); TopicItem(o.optString("id"), o.optString("title"), o.optString("body"), o.optInt("reply_count"), o.optString("created_at")) }
fun jsonPosts(arr: JSONArray): List<PostItem> = List(arr.length()) { i ->
    val o = arr.getJSONObject(i); PostItem(o.optString("id"), o.optString("author_id"), o.optString("body"), o.optString("created_at")) }
