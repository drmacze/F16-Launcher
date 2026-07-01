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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsSoccer
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
val Carbon      = Color(0xFF040810)
val GlassBase   = Color(0xFF0C1422)
val Surface2    = Color(0xFF111C2E)
val CandyCyan   = Color(0xFF27C8FF)
val CandyBlue   = Color(0xFF5F57FF)
val NeonGreen   = Color(0xFF1FDD90)
val SoftText    = Color(0xFF8899B0)
val SubText     = Color(0xFF556070)
val GlassStroke = Color(0x305D8DFF)
val DangerRed   = Color(0xFFFF4D6D)
val AmberWarn   = Color(0xFFFF9A30)

// ─── Constants ────────────────────────────────────────────────────────────────
private const val GAME_PKG         = "com.ea.gp.fifaworld"
private const val FIFA_APK_URL     = "https://github.com/drmacze/F16/releases/download/v1.0-dlavie26/DLavie26.apk"
private const val DEFAULT_MANIFEST = "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/manifest.json"
private const val MARKER_PATH      = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private const val LOCAL_VER        = 1
private const val LOCAL_VER_NAME   = "v1"

// ─── Data models ──────────────────────────────────────────────────────────────
data class CategoryItem(val id: String, val name: String, val description: String)
data class TopicItem(val id: String, val title: String, val body: String, val replyCount: Int, val createdAt: String)
data class PostItem(val id: String, val authorId: String, val body: String, val createdAt: String)
data class FeedItem(val id: String, val title: String, val body: String, val type: String, val pinned: Boolean, val official: Boolean)
data class UpdateInfo(val latestCode: Int, val latestName: String, val upToDate: Boolean, val releaseNotes: List<String>)

// ─── App setup state ──────────────────────────────────────────────────────────
enum class SetupState { LOADING, NEED_GAME, NEED_DATA, READY }

// ─── Navigation pages ─────────────────────────────────────────────────────────
enum class Page(val label: String, val navIcon: ImageVector) {
    Home  ("Beranda",   Icons.Rounded.Home),
    Update("Update",    Icons.Rounded.CloudSync),
    Chat  ("Komunitas", Icons.Rounded.Forum),
    Me    ("Profil",    Icons.Rounded.AccountCircle)
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
        background   = Carbon,    surface      = GlassBase,
        primary      = CandyCyan, secondary    = CandyBlue,
        onPrimary    = Color(0xFF00111D), onSecondary = Color.White,
        onBackground = Color.White, onSurface  = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Carbon) {
            Box(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF06101E), Carbon, Color(0xFF060D18))))
            ) {
                if (!api.loggedIn()) {
                    LaunchedEffect(Unit) {
                        context.startActivity(
                            Intent(context, DLavieGuidedActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = CandyCyan, strokeWidth = 2.5.dp)
                            Text("Memuat sesi...", color = SoftText, fontSize = 13.sp)
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

    LaunchedEffect(Unit) {
        while (true) {
            delay(50L * 60_000)
            withContext(Dispatchers.IO) { runCatching { api.refreshToken() } }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState    = page,
            label          = "page_anim",
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            modifier       = Modifier.fillMaxSize().padding(bottom = 100.dp)
        ) { target ->
            when (target) {
                Page.Home   -> HomeScreen(api, onNav = { page = it })
                Page.Update -> UpdateScreen(onNav  = { page = it })
                Page.Chat   -> CommunityScreen(api)
                Page.Me     -> ProfileScreen(api, onLogout)
            }
        }
        FloatingNav(
            page     = page,
            onPage   = { page = it },
            modifier = Modifier.align(Alignment.BottomCenter)
                               .navigationBarsPadding()
                               .padding(bottom = 12.dp)
        )
    }
}

// ─── Floating navigation bar ──────────────────────────────────────────────────
@Composable
fun FloatingNav(page: Page, onPage: (Page) -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "nav_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(modifier = modifier.widthIn(max = 600.dp).padding(horizontal = 16.dp)) {
        // Glow backdrop
        Box(
            Modifier.matchParentSize()
                .clip(RoundedCornerShape(36.dp))
                .background(CandyBlue.copy(alpha = glowAlpha * 0.18f))
        )
        Surface(
            shape           = RoundedCornerShape(36.dp),
            color           = Color(0xF00B1628),
            border          = BorderStroke(1.dp, GlassStroke),
            shadowElevation = 24.dp,
            tonalElevation  = 0.dp
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Page.values().forEach { item ->
                    val selected   = page == item
                    val bgAnim     by animateColorAsState(
                        if (selected) CandyBlue else Color.Transparent,
                        tween(300), label = "nav_bg"
                    )
                    val iconTint   by animateColorAsState(
                        if (selected) Color.White else SubText,
                        tween(300), label = "nav_tint"
                    )
                    val scaleAnim  by animateFloatAsState(
                        if (selected) 1f else 0.9f, tween(250), label = "nav_scale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .scale(scaleAnim)
                            .clip(RoundedCornerShape(28.dp))
                            .background(bgAnim)
                            .clickable { onPage(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                item.navIcon,
                                contentDescription = item.label,
                                tint     = iconTint,
                                modifier = Modifier.size(if (selected) 22.dp else 20.dp)
                            )
                            Text(
                                item.label,
                                fontSize   = if (selected) 10.sp else 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color      = iconTint,
                                maxLines   = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Home screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(api: CommunityApi, onNav: (Page) -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Setup state detection ──
    var setupState   by remember { mutableStateOf(SetupState.LOADING) }
    var gameInstalled by remember { mutableStateOf(false) }
    var dataReady     by remember { mutableStateOf(false) }
    var updateInfo    by remember { mutableStateOf<UpdateInfo?>(null) }
    var feed          by remember { mutableStateOf<List<FeedItem>>(emptyList()) }

    // ── Pull-to-refresh state ──
    val pullState    = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Reusable data loader — dipanggil oleh LaunchedEffect awal & onRefresh
    suspend fun loadAllData() {
        withContext(Dispatchers.IO) {
            gameInstalled = isGameInstalled(context)
            dataReady     = readMarker().startsWith("v26", ignoreCase = true)
            runCatching { updateInfo = fetchUpdateInfo() }
            runCatching { feed       = parseFeed(api.feedPosts()) }
        }
        setupState = when {
            !gameInstalled -> SetupState.NEED_GAME
            !dataReady     -> SetupState.NEED_DATA
            else           -> SetupState.READY
        }
    }

    LaunchedEffect(Unit) { loadAllData() }

    // ── In-app APK download state ──
    var dlProgress by remember { mutableStateOf(-1f) }
    var dlError    by remember { mutableStateOf("") }

    fun startDownload() {
        dlProgress = 0f; dlError = ""
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val outDir  = java.io.File(context.getExternalFilesDir(null), "public-install").also { it.mkdirs() }
                    val apkFile = java.io.File(outDir, "DLavie26.apk")
                    val conn    = URL(FIFA_APK_URL).openConnection() as HttpURLConnection
                    conn.connectTimeout = 30_000; conn.readTimeout = 120_000; conn.connect()
                    val total   = conn.contentLengthLong.toFloat().coerceAtLeast(1f)
                    val buf     = ByteArray(16 * 1024)
                    conn.inputStream.use { inp ->
                        apkFile.outputStream().use { out ->
                            var n: Int; var read = 0L
                            while (inp.read(buf).also { n = it } != -1) {
                                out.write(buf, 0, n); read += n
                                dlProgress = (read / total).coerceIn(0f, 0.99f)
                            }
                        }
                    }
                    apkFile
                }
            }.onSuccess { apkFile ->
                dlProgress = 2f
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.files", apkFile)
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                })
            }.onFailure { dlError = it.message ?: "Unduhan gagal. Periksa koneksi internet."; dlProgress = -1f }
        }
    }

    // ── Pulse animation (for play state) ──
    val infiniteTransition = rememberInfiniteTransition(label = "home_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true
                scope.launch {
                    runCatching { loadAllData() }
                    isRefreshing = false
                }
            }
        },
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                color = CandyCyan
            )
        }
    ) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(50.dp)
                        .background(
                            Brush.linearGradient(listOf(CandyCyan, CandyBlue)),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DL", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DLavie 26", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        DLBadge("PROD", NeonGreen)
                    }
                    Text("FIFA 16 Mobile · Mod Launcher", color = SoftText, fontSize = 12.sp)
                }
                val name = api.displayName().ifEmpty { "Player" }.take(11)
                DLBadge("@$name", CandyCyan)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                Box(Modifier.weight(1f).height(2.dp)
                    .background(Brush.horizontalGradient(listOf(CandyBlue, CandyCyan, NeonGreen)), RoundedCornerShape(1.dp)))
            }
        }

        // ── Main action card (state-aware) ────────────────────────────────────
        AnimatedContent(
            targetState    = setupState,
            label          = "setup_state",
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }
        ) { state ->
            when (state) {

                // Loading
                SetupState.LOADING -> GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CandyCyan, strokeWidth = 2.5.dp)
                        Column {
                            Text("Memeriksa perangkat...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Mohon tunggu sebentar.", color = SoftText, fontSize = 12.sp)
                        }
                    }
                }

                // Step 1: Game APK belum terinstall
                SetupState.NEED_GAME -> GlassCard(borderColor = CandyBlue.copy(0.5f)) {
                    // Step indicator
                    StepIndicator(currentStep = 1, totalSteps = 2)
                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(48.dp)
                                .background(CandyBlue.copy(0.15f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CloudDownload, null, tint = CandyCyan, modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text("Instal FIFA 16", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text("Game belum ditemukan di perangkat ini.", color = SoftText, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Unduh dan instal file APK FIFA 16 terlebih dahulu. Proses ini hanya dilakukan sekali.",
                        color = SoftText, fontSize = 13.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(14.dp))

                    // Download button with inline progress
                    Button(
                        onClick  = { if (dlProgress < 0f || dlProgress >= 2f) startDownload() },
                        enabled  = dlProgress < 0f || dlProgress >= 2f,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = when {
                                dlProgress >= 2f -> NeonGreen
                                dlProgress >= 0f -> CandyBlue.copy(0.6f)
                                else             -> CandyBlue
                            },
                            contentColor   = if (dlProgress >= 2f) Color(0xFF00150B) else Color.White,
                            disabledContainerColor = CandyBlue.copy(0.4f),
                            disabledContentColor   = Color.White.copy(0.7f)
                        )
                    ) {
                        when {
                            dlProgress >= 2f -> {
                                Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Selesai — Instal Sekarang", fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                            dlProgress >= 0f -> {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CandyCyan, strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Mengunduh… ${(dlProgress * 100).toInt()}%", fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                            else -> {
                                Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Unduh FIFA 16 Sekarang", fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    AnimatedVisibility(visible = dlProgress >= 0f && dlProgress < 2f) {
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            LinearProgressIndicator(
                                progress   = { dlProgress },
                                modifier   = Modifier.fillMaxWidth(),
                                color      = CandyCyan,
                                trackColor = CandyCyan.copy(0.15f)
                            )
                            Text(
                                "${(dlProgress * 34f).toInt()} MB dari 34 MB — ${(dlProgress * 100).toInt()}% selesai",
                                color = SoftText, fontSize = 11.sp, modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                    AnimatedVisibility(visible = dlError.isNotEmpty()) {
                        Row(
                            Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(14.dp))
                            Text(dlError, color = DangerRed, fontSize = 12.sp)
                        }
                    }
                }

                // Step 2: Game terinstall, data belum siap
                SetupState.NEED_DATA -> GlassCard(borderColor = AmberWarn.copy(0.5f)) {
                    StepIndicator(currentStep = 2, totalSteps = 2)
                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(48.dp)
                                .background(NeonGreen.copy(0.12f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text("Game Terinstall!", color = NeonGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Text("Satu langkah lagi untuk mulai bermain.", color = SoftText, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassInfoBox(
                        icon  = Icons.Rounded.Storage,
                        color = AmberWarn,
                        text  = "Data game belum tersedia. Buka FIFA 16 — game akan otomatis mengunduh OBB dan data saat pertama kali dibuka."
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick  = { launchGame(context) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AmberWarn, contentColor = Color(0xFF1A0F00))
                    ) {
                        Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Buka FIFA 16 & Siapkan Data", fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Ready: semua siap!
                SetupState.READY -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Play button with glow
                    Box(
                        modifier = Modifier.fillMaxWidth().scale(pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(64.dp)
                                .background(NeonGreen.copy(alpha = glowAlpha), RoundedCornerShape(24.dp))
                        )
                        Button(
                            onClick  = { launchGame(context) },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape    = RoundedCornerShape(24.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor   = Color(0xFF00150B)
                            )
                        ) {
                            Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Main FIFA 16", fontSize = 19.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Update available banner
                    val ui = updateInfo
                    AnimatedVisibility(
                        visible = ui != null && !ui.upToDate,
                        enter = fadeIn() + expandVertically(),
                        exit  = fadeOut() + shrinkVertically()
                    ) {
                        if (ui != null) GlassCard(borderColor = CandyCyan.copy(0.5f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(40.dp)
                                        .background(CandyCyan.copy(0.12f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.SystemUpdate, null, tint = CandyCyan, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Pembaruan Tersedia", color = CandyCyan, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                    Text("Versi ${ui.latestName} siap diunduh.", color = SoftText, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick  = { onNav(Page.Update) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))
                            ) {
                                Icon(Icons.Rounded.CloudSync, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Lihat & Terapkan Update", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        // ── Status bar (3 chips) ───────────────────────────────────────────────
        AnimatedVisibility(visible = setupState != SetupState.LOADING) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(
                    label  = "Game",
                    value  = if (gameInstalled) "Terinstall" else "Belum ada",
                    ok     = gameInstalled,
                    modifier = Modifier.weight(1f)
                ) { if (!gameInstalled) if (dlProgress < 0f) startDownload() }

                StatusChip(
                    label  = "Data",
                    value  = if (dataReady) "Siap" else "Belum siap",
                    ok     = dataReady,
                    modifier = Modifier.weight(1f)
                ) { onNav(Page.Update) }

                StatusChip(
                    label  = "Update",
                    value  = when {
                        updateInfo == null        -> "Memeriksa"
                        updateInfo!!.upToDate     -> "Terbaru"
                        else                      -> "Tersedia"
                    },
                    ok     = updateInfo?.upToDate != false,
                    modifier = Modifier.weight(1f)
                ) { onNav(Page.Update) }
            }
        }

        // ── Berita / Feed ─────────────────────────────────────────────────────
        AnimatedVisibility(visible = feed.isNotEmpty(), enter = fadeIn(tween(400)), exit = fadeOut()) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Notifications, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Berita Terbaru", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(10.dp))
                feed.take(3).forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.size(8.dp).padding(top = 5.dp)
                                .background(
                                    if (item.pinned) AmberWarn else CandyCyan,
                                    CircleShape
                                )
                        )
                        Column {
                            Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.body, color = SoftText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }

        // Bottom spacer
        Spacer(Modifier.height(8.dp))
    }
    } // end PullToRefreshBox
}

// ─── Step progress indicator ──────────────────────────────────────────────────
@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..totalSteps).forEach { step ->
            val done     = step < currentStep
            val active   = step == currentStep
            val color    = when { done -> NeonGreen; active -> CandyCyan; else -> SubText }
            Box(
                Modifier.size(22.dp)
                    .background(color.copy(if (active || done) 0.15f else 0.07f), CircleShape)
                    .then(if (active) Modifier else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Rounded.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                else Text("$step", color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            if (step < totalSteps) {
                Box(Modifier.weight(1f).height(2.dp).background(
                    if (done) NeonGreen.copy(0.5f) else SubText.copy(0.25f), RoundedCornerShape(1.dp)
                ))
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "Langkah $currentStep dari $totalSteps",
            color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
    }
}

// ─── Glass info box ───────────────────────────────────────────────────────────
@Composable
fun GlassInfoBox(icon: ImageVector, color: Color, text: String) {
    Row(
        Modifier.fillMaxWidth()
            .background(color.copy(0.08f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Text(text, color = color.copy(0.9f), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

// ─── Update & Data screen ─────────────────────────────────────────────────────
@Composable
fun UpdateScreen(onNav: (Page) -> Unit) {
    val context       = LocalContext.current
    val gameInstalled = remember { isGameInstalled(context) }
    var marker        by remember { mutableStateOf(readMarker()) }
    val dataReady     = marker.startsWith("v26", ignoreCase = true)
    var updateInfo    by remember { mutableStateOf<UpdateInfo?>(null) }
    var loading       by remember { mutableStateOf(true) }
    var updateError   by remember { mutableStateOf("") }
    val scope         = rememberCoroutineScope()

    val patchLogs  = remember { mutableStateListOf<String>() }
    var patching   by remember { mutableStateOf(false) }
    var patchStep  by remember { mutableStateOf(0) }
    var patchTotal by remember { mutableStateOf(1) }
    var patchLabel by remember { mutableStateOf("") }
    var patchError by remember { mutableStateOf("") }
    var patchDone  by remember { mutableStateOf(false) }
    var showLog    by remember { mutableStateOf(false) }

    val engine = remember {
        DevPatchEngine(context,
            onLog      = { msg -> patchLogs.add(msg) },
            onProgress = { cur, tot, lbl -> patchStep = cur; patchTotal = tot; patchLabel = lbl }
        )
    }

    fun refresh() { marker = readMarker() }

    fun checkUpdate() {
        loading = true; updateError = ""
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { fetchUpdateInfo() } }
                .fold(onSuccess = { updateInfo = it }, onFailure = { updateError = it.message ?: "Gagal terhubung" })
            loading = false
        }
    }

    fun applyPatch() {
        patchLogs.clear(); patchError = ""; patchDone = false; patching = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { engine.applyAvailableUpdates() } }
            result.onFailure { patchError = it.message ?: "Pembaruan gagal diterapkan" }
            result.onSuccess { patchDone = true }
            patching = false
            refresh()
        }
    }

    LaunchedEffect(Unit) { checkUpdate() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Header ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).background(CandyBlue.copy(0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CloudSync, null, tint = CandyCyan, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text("Update & Pembaruan", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text("Kelola data dan pembaruan game FIFA 16.", color = SoftText, fontSize = 12.sp)
                }
            }
        }

        // ── Status ringkasan ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStatusTile("Game", if (gameInstalled) "Terinstall" else "Belum ada", gameInstalled, Modifier.weight(1f))
            MiniStatusTile("Data", if (dataReady) "Siap" else "Belum siap", dataReady, Modifier.weight(1f))
            MiniStatusTile("Versi",
                if (loading) "…" else updateInfo?.latestName ?: "—",
                updateInfo?.upToDate != false, Modifier.weight(1f))
        }

        // ── Data game belum siap ──
        AnimatedVisibility(!dataReady, enter = fadeIn(), exit = fadeOut()) {
            GlassCard(borderColor = AmberWarn.copy(0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(40.dp).background(AmberWarn.copy(0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Warning, null, tint = AmberWarn, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Data Game Belum Siap", color = AmberWarn, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("Game tidak dapat dimainkan sebelum data tersedia.", color = SoftText, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Terapkan pembaruan di bawah ini untuk mengunduh dan menyiapkan data FIFA 16 secara otomatis.",
                    color = SoftText, fontSize = 12.sp, lineHeight = 17.sp
                )
            }
        }

        // ── Patch / update card ──
        val ui             = updateInfo
        val patchAvailable = ui != null && !ui.upToDate

        GlassCard(borderColor = when {
            patchDone              -> NeonGreen.copy(0.5f)
            patchError.isNotBlank()-> DangerRed.copy(0.4f)
            patchAvailable         -> CandyCyan.copy(0.4f)
            else                   -> GlassStroke
        }) {
            // Status row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).background(
                        when {
                            patchDone   -> NeonGreen.copy(0.12f)
                            patchAvailable -> CandyCyan.copy(0.10f)
                            else        -> SubText.copy(0.08f)
                        }, RoundedCornerShape(14.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (patchDone) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                        null,
                        tint     = if (patchDone) NeonGreen else if (patchAvailable) CandyCyan else SoftText,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            patchDone               -> "Pembaruan Berhasil!"
                            patchError.isNotBlank() -> "Pembaruan Gagal"
                            patchAvailable          -> "Pembaruan Tersedia"
                            loading                 -> "Memeriksa server…"
                            else                    -> "Game Sudah Terbaru"
                        },
                        color = when {
                            patchDone   -> NeonGreen
                            patchError.isNotBlank() -> DangerRed
                            patchAvailable -> CandyCyan
                            else        -> Color.White
                        },
                        fontSize = 16.sp, fontWeight = FontWeight.Black
                    )
                    Text(
                        "Versi kamu: $LOCAL_VER_NAME  ·  Terbaru: ${ui?.latestName ?: "…"}",
                        color = SoftText, fontSize = 12.sp
                    )
                }
            }

            // Progress bar saat patching
            AnimatedVisibility(patching) {
                Column(Modifier.padding(top = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CandyCyan, strokeWidth = 2.dp)
                        Text(patchLabel.ifEmpty { "Memperbarui game…" }, color = SoftText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress   = { if (patchTotal > 0) patchStep.toFloat() / patchTotal else 0f },
                        modifier   = Modifier.fillMaxWidth(),
                        color      = CandyCyan,
                        trackColor = GlassStroke
                    )
                }
            }

            // Error
            AnimatedVisibility(patchError.isNotBlank()) {
                GlassInfoBox(Icons.Rounded.ErrorOutline, DangerRed, patchError)
            }

            // Update error
            AnimatedVisibility(updateError.isNotBlank()) {
                GlassInfoBox(Icons.Rounded.ErrorOutline, DangerRed, "Tidak dapat terhubung ke server. Periksa koneksi internet, lalu coba lagi.")
            }

            Spacer(Modifier.height(14.dp))

            // Action buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = { checkUpdate() },
                    enabled  = !loading && !patching,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, if (!loading && !patching) CandyCyan.copy(0.5f) else GlassStroke)
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = if (!loading && !patching) CandyCyan else SoftText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (loading) "Memeriksa…" else "Periksa", color = if (!loading && !patching) CandyCyan else SoftText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick  = { applyPatch() },
                    enabled  = !patching && !loading && patchAvailable,
                    modifier = Modifier.weight(2f).height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = if (patchAvailable) CandyCyan else NeonGreen,
                        contentColor           = Color(0xFF00111D),
                        disabledContainerColor = Surface2,
                        disabledContentColor   = SoftText
                    )
                ) {
                    if (patching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00111D), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Memperbarui…", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                patchDone      -> "Sudah Diperbarui ✓"
                                patchAvailable -> "Terapkan Pembaruan"
                                else           -> "Sudah Terbaru"
                            },
                            fontWeight = FontWeight.Black, fontSize = 13.sp
                        )
                    }
                }
            }

            // Log toggle
            AnimatedVisibility(patchLogs.isNotEmpty()) {
                Column(Modifier.padding(top = 10.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { showLog = !showLog }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(if (showLog) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = SubText, modifier = Modifier.size(14.dp))
                        Text(if (showLog) "Sembunyikan log teknis" else "Lihat log teknis", color = SubText, fontSize = 11.sp)
                    }
                    AnimatedVisibility(showLog) {
                        Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            patchLogs.takeLast(20).forEach { line ->
                                Text(
                                    "› $line",
                                    color    = if (line.contains("error", ignoreCase = true) || line.contains("gagal", ignoreCase = true))
                                                   DangerRed else if (line.contains("ok", ignoreCase = true) || line.contains("selesai", ignoreCase = true))
                                                   NeonGreen else SoftText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Play button (jika siap) ──
        AnimatedVisibility(dataReady && gameInstalled, enter = fadeIn(tween(400)), exit = fadeOut()) {
            Button(
                onClick  = { launchGame(context) },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(20.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))
            ) {
                Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Main FIFA 16 Sekarang", fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }

        // ── Jika game belum terinstall ──
        AnimatedVisibility(!gameInstalled) {
            GlassInfoBox(
                icon  = Icons.Rounded.Info,
                color = CandyCyan,
                text  = "Instal game FIFA 16 terlebih dahulu sebelum melakukan pembaruan. Kembali ke tab Beranda untuk mengunduh."
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Community / Chat screen ──────────────────────────────────────────────────
@Composable
fun CommunityScreen(api: CommunityApi) {
    val scope = rememberCoroutineScope()
    var categories       by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var topics           by remember { mutableStateOf<List<TopicItem>>(emptyList()) }
    var selectedTopic    by remember { mutableStateOf<TopicItem?>(null) }
    var posts            by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var status           by remember { mutableStateOf("Memuat komunitas…") }
    var title            by remember { mutableStateOf("") }
    var body             by remember { mutableStateOf("") }
    var reply            by remember { mutableStateOf("") }

    fun loadPosts(topic: TopicItem) {
        scope.launch {
            try { posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) } }
            catch (t: Throwable) { status = "Gagal memuat percakapan: ${t.message}" }
        }
    }
    fun loadTopics() {
        scope.launch {
            try {
                topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }
                status = if (topics.isEmpty()) "Belum ada topik." else "${topics.size} topik tersedia."
            } catch (t: Throwable) { status = "Gagal memuat topik: ${t.message}" }
        }
    }
    fun createTopic() {
        val cat = selectedCategory ?: run { status = "Pilih saluran terlebih dahulu."; return }
        if (title.trim().length < 4 || body.trim().isEmpty()) { status = "Judul minimal 4 karakter dan isi wajib diisi."; return }
        scope.launch {
            try {
                val newTopic = withContext(Dispatchers.IO) { api.createTopic(cat.id, title, body) }
                title = ""; body = ""
                topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(cat.id)) }
                selectedTopic = topics.firstOrNull { it.id == newTopic.optString("id") }
                status = "Topik berhasil dibuat."
            } catch (t: Throwable) { status = "Gagal: ${t.message}" }
        }
    }
    fun sendReply() {
        val topic = selectedTopic ?: run { status = "Pilih topik terlebih dahulu."; return }
        if (reply.trim().isEmpty()) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) { api.createPost(topic.id, "", reply) }
                reply = ""
                posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) }
            } catch (t: Throwable) { status = "Gagal mengirim: ${t.message}" }
        }
    }

    LaunchedEffect(Unit) {
        try {
            categories       = withContext(Dispatchers.IO) { jsonCategories(api.categories()) }
            selectedCategory = categories.firstOrNull()
            topics           = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }
            status           = if (topics.isEmpty()) "Belum ada topik." else "Siap."
        } catch (t: Throwable) { status = "Error komunitas: ${t.message}" }
    }

    Box(Modifier.fillMaxSize().padding(14.dp)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChannelPanel(categories, selectedCategory) { selectedCategory = it; selectedTopic = null; posts = emptyList(); loadTopics() }
            TopicPanel(status, title, body, topics, selectedTopic,
                onTitle = { title = it }, onBody = { body = it },
                onCreate = { createTopic() }, onSelect = { t -> selectedTopic = t; loadPosts(t) })
            ThreadPanel(selectedTopic, posts, reply, onReply = { reply = it }, onSend = { sendReply() })
        }
    }
}

// ─── Profile / Me screen ──────────────────────────────────────────────────────
@Composable
fun ProfileScreen(api: CommunityApi, onLogout: () -> Unit) {
    val context       = LocalContext.current
    val gameInstalled = remember { isGameInstalled(context) }
    var confirmLogout by remember { mutableStateOf(false) }
    val initial = api.displayName().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    val role    = api.role()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Avatar + identity ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(68.dp)
                        .background(Brush.linearGradient(listOf(CandyCyan, CandyBlue)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(api.displayName().ifEmpty { "DLavie Player" }, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("@${api.username().ifEmpty { "unknown" }}", color = SoftText, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    DLBadge(role.uppercase(), roleBadgeColor(role))
                }
            }
            Spacer(Modifier.height(10.dp))
            // Account details
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniStatusTile("Game", if (gameInstalled) "Terinstall" else "Belum ada", gameInstalled, Modifier.weight(1f))
                MiniStatusTile("Sesi", "Aktif", true, Modifier.weight(1f))
                MiniStatusTile("Server", "Online", true, Modifier.weight(1f))
            }
        }

        // ── Game action ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SportsSoccer, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("FIFA 16 Mobile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            if (gameInstalled) {
                Button(
                    onClick  = { launchGame(context) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))
                ) {
                    Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Main FIFA 16", fontWeight = FontWeight.Bold)
                }
            } else {
                GlassInfoBox(Icons.Rounded.Info, CandyCyan, "Game belum terinstall. Kembali ke Beranda untuk mengunduh dan menginstal FIFA 16.")
            }
        }

        // ── Info akun ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountCircle, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Detail Akun", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            ProfRow("Username",    "@${api.username().ifEmpty { "-" }}")
            ProfRow("Nama",        api.displayName().ifEmpty { "-" })
            ProfRow("Role",        role.replaceFirstChar { it.uppercase() })
            ProfRow("Server",      "DLavie Cloud")
        }

        // ── Keamanan ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Keamanan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            listOf(
                "Sesi terenkripsi — diperbarui otomatis setiap 50 menit.",
                "Setiap pembaruan diverifikasi sebelum diterapkan.",
                "Data login tidak pernah disimpan di penyimpanan lokal."
            ).forEach { text ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(13.dp).padding(top = 2.dp))
                    Text(text, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }

        // ── Logout ──
        AnimatedContent(targetState = confirmLogout, label = "logout") { confirm ->
            if (!confirm) {
                OutlinedButton(
                    onClick  = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, DangerRed.copy(0.4f))
                ) {
                    Text("Keluar dari Akun", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            } else {
                GlassCard(borderColor = DangerRed.copy(0.5f)) {
                    Text("Konfirmasi Keluar", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Kamu harus login kembali setelah keluar.", color = SoftText, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick  = { confirmLogout = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.dp, GlassStroke)
                        ) { Text("Batal", color = SoftText) }
                        Button(
                            onClick  = onLogout,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) { Text("Keluar", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Shared UI components ─────────────────────────────────────────────────────

@Composable
fun GlassCard(modifier: Modifier = Modifier, borderColor: Color = GlassStroke, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xBE0C1422)),
        border   = BorderStroke(1.dp, borderColor)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun StatusChip(label: String, value: String, ok: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val borderAnim by animateColorAsState(
        if (ok) NeonGreen.copy(0.35f) else DangerRed.copy(0.35f),
        tween(400), label = "chip_border"
    )
    val iconTint by animateColorAsState(
        if (ok) NeonGreen else DangerRed,
        tween(400), label = "chip_icon"
    )
    Surface(
        modifier  = modifier.height(76.dp).clip(RoundedCornerShape(18.dp)).clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        color     = Color(0xA00A1422),
        border    = BorderStroke(1.dp, borderAnim)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) {
            Icon(
                if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                null, tint = iconTint, modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(label, color = SubText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, color = iconTint, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MiniStatusTile(label: String, value: String, ok: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(if (ok) NeonGreen else DangerRed, tween(400), label = "tile_c")
    GlassCard(modifier = modifier) {
        Text(label, color = SubText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DLBadge(text: String, color: Color) {
    Surface(
        color  = color.copy(0.12f),
        shape  = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(0.35f))
    ) {
        Text(
            text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), maxLines = 1
        )
    }
}

@Composable
fun ProfRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = SubText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
        Text(value, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GlassListItem(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = if (selected) Color(0x4427C8FF) else Color(0x66101F34),
        border   = BorderStroke(1.dp, if (selected) CandyCyan else GlassStroke),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
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
            Icon(Icons.Rounded.Forum, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Saluran", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(10.dp))
        if (categories.isEmpty())
            Text("Memuat saluran…", color = SoftText, fontSize = 12.sp)
        else categories.forEach { c ->
            OutlinedButton(
                onClick  = { onSelect(c) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.dp, if (selected?.id == c.id) CandyCyan else GlassStroke)
            ) { Text(c.name, color = if (selected?.id == c.id) CandyCyan else Color.White, fontSize = 13.sp) }
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
        Text("Topik Diskusi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(status, color = SoftText, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = title, onValueChange = onTitle, label = { Text("Judul topik baru", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = body, onValueChange = onBody, label = { Text("Isi topik", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), minLines = 2)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick  = onCreate,
            colors   = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D)),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        ) { Text("Buat Topik Baru", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            topics.forEach { t -> GlassListItem(t.title, "${t.replyCount} balasan · ${t.createdAt.take(10)}", selected?.id == t.id) { onSelect(t) } }
        }
    }
}

@Composable
fun ThreadPanel(topic: TopicItem?, posts: List<PostItem>, reply: String, modifier: Modifier = Modifier, onReply: (String) -> Unit, onSend: () -> Unit) {
    GlassCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Forum, null, tint = CandyCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(topic?.title ?: "Pilih Topik", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (topic != null) Text(topic.body, color = SoftText, fontSize = 12.sp)
        else Text("Pilih topik di sebelah kiri untuk mulai membaca.", color = SoftText, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            posts.forEach { p -> GlassListItem("@${p.authorId.take(8)}", p.body, false) {} }
        }
        if (topic != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = reply, onValueChange = onReply, label = { Text("Tulis balasan…", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = false, minLines = 2)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = onSend,
                colors   = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B)),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
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
    val json       = fetchJson(DEFAULT_MANIFEST)
    // Support both DLavie-Launcher-Data format {version:26} and legacy {latestVersionCode:N}
    val latestCode = json.optInt("version", json.optInt("latestVersionCode", LOCAL_VER))
    val latestName = json.optString("latestVersionName", "v$latestCode")
    val notesArr   = json.optJSONArray("releaseNotes")
    val notes      = if (notesArr != null) List(notesArr.length()) { i -> notesArr.optString(i) } else emptyList()
    return UpdateInfo(latestCode, latestName, latestCode <= LOCAL_VER, notes)
}

fun parseFeed(arr: JSONArray): List<FeedItem> = try {
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        FeedItem(o.optString("id"), o.optString("title"), o.optString("body"), o.optString("type","info"), o.optBoolean("pinned"), o.optBoolean("official"))
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
    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FIFA_APK_URL)))
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
