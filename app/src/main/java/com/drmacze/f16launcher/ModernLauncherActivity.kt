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
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Design tokens sekarang ada di ModernUI.kt (premium palette v2.0)

// ─── Constants ────────────────────────────────────────────────────────────────
private const val GAME_PKG         = "com.ea.gp.fifaworld"
private const val FIFA_APK_URL     = "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/DLavie26.apk"
private const val DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json"
private const val MARKER_PATH      = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private val LOCAL_VER        = com.drmacze.f16launcher.BuildConfig.VERSION_CODE
private val LOCAL_VER_NAME   = com.drmacze.f16launcher.BuildConfig.VERSION_NAME

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
    var pinVerified by remember { mutableStateOf(!PinManager.hasPin(context)) }

    // If PIN is enabled and not yet verified, launch the PIN lock screen
    LaunchedEffect(Unit) {
        if (PinManager.hasPin(context) && !pinVerified) {
            PinLockActivity.launch(context, PinLockActivity.MODE_UNLOCK)
        }
    }

    // Refresh pinVerified state on resume (after returning from PinLockActivity)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (!PinManager.hasPin(context)) pinVerified = true
                // If PIN exists and was just verified externally (via PinLockActivity),
                // we can't easily know — but the activity launching back means user is allowed in
                // (or they pressed home — but we re-prompt on next foreground via onResume).
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        try { kotlinx.coroutines.awaitCancellation() } finally { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                } else if (!pinVerified && PinManager.hasPin(context)) {
                    // Wait for PIN verification — show lock screen placeholder
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Lock, null, tint = CandyCyan, modifier = Modifier.size(48.dp))
                            Text("Masukkan PIN untuk lanjut", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Aktivitas PIN lock terbuka di layar lain", color = SoftText, fontSize = 12.sp)
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { PinLockActivity.launch(context, PinLockActivity.MODE_UNLOCK) },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))
                            ) { Text("Buka PIN Lock", fontWeight = FontWeight.Black) }
                        }
                    }
                } else {
                    MainShell(api) {
                        api.logout()
                        context.getSharedPreferences("dlavie_auth_session", Context.MODE_PRIVATE).edit().clear().apply()
                        // Also clear PIN on logout for security
                        PinManager.clearPin(context)
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
            transitionSpec = {
                (fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                 androidx.compose.animation.slideInHorizontally(
                     initialOffsetX = { it / 12 },
                     animationSpec = tween(380, easing = FastOutSlowInEasing)
                 )) togetherWith
                (fadeOut(tween(220)) +
                 androidx.compose.animation.slideOutHorizontally(
                     targetOffsetX = { -it / 24 },
                     animationSpec = tween(280, easing = FastOutSlowInEasing)
                 ))
            },
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

// ─── Floating navigation bar (modern v2) ───────────────────────────────────────
@Composable
fun FloatingNav(page: Page, onPage: (Page) -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "nav_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(modifier = modifier.widthIn(max = 600.dp).padding(horizontal = 16.dp)) {
        // Multi-layer glow backdrop (cyan + violet)
        Box(
            Modifier.matchParentSize()
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            CandyCyan.copy(alpha = glowAlpha * 0.10f),
                            CandyBlue.copy(alpha = glowAlpha * 0.18f),
                            PremiumViolet.copy(alpha = glowAlpha * 0.10f)
                        )
                    )
                )
                .blur(20.dp)
        )
        Surface(
            shape           = RoundedCornerShape(36.dp),
            color           = Color(0xF00B1320),
            border          = BorderStroke(1.dp, Brush.horizontalGradient(
                listOf(GlassStroke, CandyCyan.copy(0.25f), GlassStroke)
            )),
            shadowElevation = 24.dp,
            tonalElevation  = 0.dp
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Page.values().forEach { item ->
                    val selected  = page == item
                    val bgAnim    by animateColorAsState(
                        if (selected) CandyCyan else Color.Transparent,
                        tween(380, easing = FastOutSlowInEasing), label = "nav_bg_${item.label}"
                    )
                    val iconTint  by animateColorAsState(
                        if (selected) Carbon else SubText,
                        tween(380, easing = FastOutSlowInEasing), label = "nav_tint_${item.label}"
                    )
                    val labelAlpha by animateFloatAsState(
                        if (selected) 1f else 0.7f,
                        tween(280), label = "nav_alpha_${item.label}"
                    )
                    val scaleAnim by animateFloatAsState(
                        if (selected) 1f else 0.92f, tween(280, easing = FastOutSlowInEasing),
                        label = "nav_scale_${item.label}"
                    )
                    val iconScale by animateFloatAsState(
                        if (selected) 1.1f else 1f, tween(280, easing = FastOutSlowInEasing),
                        label = "nav_iconscale_${item.label}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .scale(scaleAnim)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (selected) Brush.horizontalGradient(
                                    listOf(CandyCyan, CandyBlue.copy(0.85f))
                                )
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { onPage(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                item.navIcon,
                                contentDescription = item.label,
                                tint     = iconTint,
                                modifier = Modifier.size(20.dp).scale(iconScale)
                            )
                            androidx.compose.animation.AnimatedVisibility(
                                visible = selected,
                                enter = androidx.compose.animation.fadeIn(tween(200)) +
                                    androidx.compose.animation.expandHorizontally(tween(280, easing = FastOutSlowInEasing)),
                                exit = androidx.compose.animation.fadeOut(tween(160)) +
                                    androidx.compose.animation.shrinkHorizontally(tween(200, easing = FastOutSlowInEasing))
                            ) {
                                Text(
                                    item.label,
                                    fontSize   = 12.sp,
                                    fontWeight = FontWeight.Black,
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
    // v7.9.76: State untuk dialog bantuan install failure
    var showInstallHelpDialog by remember { mutableStateOf("") }

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

        // ── Hero Header (premium gradient + glow) ──────────────────────────────
        PremiumGlassCard(gradientBorder = true) {
            // Subtle animated background glow inside header
            val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
            val heroGlow by infiniteTransition.animateFloat(
                initialValue = 0.15f, targetValue = 0.35f,
                animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "hero_glow_val"
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                CandyCyan.copy(alpha = heroGlow * 0.4f),
                                CandyBlue.copy(alpha = heroGlow * 0.3f),
                                PremiumViolet.copy(alpha = heroGlow * 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(CandyCyan, CandyBlue, PremiumViolet)),
                                RoundedCornerShape(14.dp)
                            )
                            .softGlow(CandyCyan, radius = 18f, alpha = 0.35f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DL", color = Carbon, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("DLavie 26", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            ModernPill("PROD", NeonGreen)
                        }
                        Text("FIFA 16 Mobile · Mod Launcher", color = SoftText, fontSize = 11.sp)
                    }
                    val name = api.displayName().ifEmpty { "Player" }.take(11)
                    ModernPill("@$name", CandyCyan)
                }
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

                    // GameHub button — opens standalone GameHub overlay (v279 design)
                    var showGameHub by remember { mutableStateOf(false) }
                    Button(
                        onClick = { showGameHub = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1F3A),
                            contentColor = Color(0xFF8BA3FF)
                        )
                    ) {
                        Icon(Icons.Rounded.SportsEsports, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Buka GameHub", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    if (showGameHub) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showGameHub = false },
                            properties = androidx.compose.ui.window.DialogProperties(
                                usePlatformDefaultWidth = false,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = false
                            )
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                DLavieGameHub(onExit = { showGameHub = false })
                            }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModernStatusChip(
                    label  = "Game",
                    value  = if (gameInstalled) "Terinstall" else "Belum ada",
                    ok     = gameInstalled,
                    modifier = Modifier.weight(1f)
                ) { if (!gameInstalled) if (dlProgress < 0f) startDownload() }

                ModernStatusChip(
                    label  = "Data",
                    value  = if (dataReady) "Siap" else "Belum siap",
                    ok     = dataReady,
                    modifier = Modifier.weight(1f)
                ) { onNav(Page.Update) }

                ModernStatusChip(
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

    // ── v7.9.76: Fullscreen update popup with snooze + install-failure UX ──
    val updatePrefs = remember { context.getSharedPreferences("dlavie_update_popup", android.content.Context.MODE_PRIVATE) }
    val snoozeUntil = remember { updatePrefs.getLong("snooze_until_ms", 0L) }
    val nowMs       = remember { System.currentTimeMillis() }
    val snoozed     = snoozeUntil > nowMs
    val ui          = updateInfo
    val showPopup   = ui != null && !ui.upToDate && !snoozed

    if (showPopup) {
        FullscreenUpdatePopup(
            info           = ui!!,
            onLater        = {
                // Snooze 24 jam
                updatePrefs.edit()
                    .putLong("snooze_until_ms", System.currentTimeMillis() + 24L * 60L * 60L * 1000L)
                    .apply()
            },
            onInstallFail  = { msg ->
                // Tampilkan dialog bantuan signature mismatch
                showInstallHelpDialog = msg
            }
        )
    }

    if (showInstallHelpDialog.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showInstallHelpDialog = "" },
            title            = { Text("Update Gagal", color = DangerRed, fontWeight = FontWeight.Black) },
            text             = {
                Column {
                    Text(
                        showInstallHelpDialog,
                        color = SoftText, fontSize = 13.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Kemungkinan penyebab: signature APK lama berbeda dengan APK baru (debug keystore berbeda).\n\n" +
                        "Solusi (SEKALI SAJA):\n" +
                        "1. Uninstall DLavie Launcher yang sekarang.\n" +
                        "2. Install APK baru dari folder Download.\n" +
                        "3. Setelah ini, semua update berikutnya akan otomatis tanpa uninstall.",
                        color = SoftText, fontSize = 12.sp, lineHeight = 17.sp
                    )
                }
            },
            confirmButton    = {
                TextButton(onClick = { showInstallHelpDialog = "" }) {
                    Text("Mengerti", color = CandyCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor   = GlassBase
        )
    }
}

// ── v7.9.76: Fullscreen update popup ──────────────────────────────────────────
@Composable
fun FullscreenUpdatePopup(
    info: UpdateInfo,
    onLater: () -> Unit,
    onInstallFail: (String) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var downloading   by remember { mutableStateOf(false) }
    var downloadPct   by remember { mutableStateOf(0f) }
    var downloadErr   by remember { mutableStateOf("") }

    fun startApkDownload() {
        downloading = true; downloadPct = 0f; downloadErr = ""
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val apkUrl = fetchLauncherApkUrl()
                    android.util.Log.i("UpdateCheck", "Downloading launcher APK from: $apkUrl")
                    val outDir  = java.io.File(context.getExternalFilesDir(null), "launcher-updates").also { it.mkdirs() }
                    val apkFile = java.io.File(outDir, "dlavie-launcher-update.apk")
                    if (apkFile.exists()) apkFile.delete()
                    val conn    = URL(apkUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 30_000; conn.readTimeout = 300_000; conn.connect()
                    val total   = conn.contentLengthLong.toFloat().coerceAtLeast(1f)
                    val buf     = ByteArray(32 * 1024)
                    conn.inputStream.use { inp ->
                        apkFile.outputStream().use { out ->
                            var n: Int; var read = 0L
                            while (inp.read(buf).also { n = it } != -1) {
                                out.write(buf, 0, n); read += n
                                downloadPct = (read / total).coerceIn(0f, 0.99f)
                            }
                        }
                    }
                    apkFile
                }
            }.onSuccess { apkFile ->
                downloadPct = 1f
                // Launch installer
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.files", apkFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
                downloading = false
            }.onFailure { t ->
                android.util.Log.e("UpdateCheck", "Launcher APK download failed", t)
                downloadErr = t.message ?: "Unduhan gagal. Periksa koneksi internet."
                downloading = false
                onInstallFail(downloadErr)
            }
        }
    }

    // Fullscreen overlay (semi-transparent backdrop + centered card)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            borderColor = CandyCyan.copy(0.6f),
            modifier = Modifier.fillMaxWidth(0.92f).padding(horizontal = 8.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Title with download icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(CandyCyan.copy(0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, null, tint = CandyCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Update Tersedia!", color = CandyCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Versi baru ${info.latestName} sudah tersedia", color = SoftText, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Release notes
                if (info.releaseNotes.isNotEmpty()) {
                    Text("Catatan rilis:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        info.releaseNotes.take(5).forEach { note ->
                            Row {
                                Text("• ", color = CandyCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text(note, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Download progress (jika sedang download)
                if (downloading) {
                    LinearProgressIndicator(
                        progress   = { downloadPct },
                        modifier   = Modifier.fillMaxWidth(),
                        color      = CandyCyan,
                        trackColor = GlassStroke
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Mengunduh… ${(downloadPct * 100).toInt()}%",
                        color = SoftText, fontSize = 11.sp
                    )
                    Spacer(Modifier.height(14.dp))
                }

                // Action buttons
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick  = { onLater() },
                        enabled  = !downloading,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        border   = BorderStroke(1.dp, GlassStroke)
                    ) {
                        Text("Nanti", color = SoftText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick  = { startApkDownload() },
                        enabled  = !downloading,
                        modifier = Modifier.weight(2f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = CandyCyan,
                            contentColor   = Color(0xFF00111D)
                        )
                    ) {
                        if (downloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color    = Color(0xFF00111D),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Mengunduh…", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Update Sekarang", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                // Hint text
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tips: Jika update gagal dengan error signature, uninstall versi lama SEKALI saja. " +
                    "Setelah install APK baru, semua update berikutnya akan otomatis.",
                    color = SubText, fontSize = 10.sp, lineHeight = 14.sp
                )
            }
        }
    }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(onNav: (Page) -> Unit) {
    val context       = LocalContext.current
    // NOTE: All I/O moved to LaunchedEffect below — don't block main thread during composition.
    // Initialize with safe defaults; real values populate from background.
    var gameInstalled   by remember { mutableStateOf(false) }
    var marker          by remember { mutableStateOf("") }
    val dataReady       = marker.startsWith("v26", ignoreCase = true)
    var updateInfo      by remember { mutableStateOf<UpdateInfo?>(null) }
    var loading         by remember { mutableStateOf(true) }
    var updateError     by remember { mutableStateOf("") }
    val scope           = rememberCoroutineScope()

    // All-Files Access permission state — loaded async
    var filesAccessGranted by remember { mutableStateOf(false) }

    val patchLogs  = remember { mutableStateListOf<String>() }
    var patching   by remember { mutableStateOf(false) }
    var patchStep  by remember { mutableStateOf(0) }
    var patchTotal by remember { mutableStateOf(1) }
    var patchLabel by remember { mutableStateOf("") }
    var patchError by remember { mutableStateOf("") }
    var patchDone  by remember { mutableStateOf(false) }
    var showLog    by remember { mutableStateOf(false) }

    // Storage & backup state — loaded async
    var freeBytes       by remember { mutableStateOf(0L) }
    var totalBytes      by remember { mutableStateOf(0L) }
    var backupCount     by remember { mutableStateOf(0) }
    var backupTotalSize by remember { mutableStateOf(0L) }
    var rollbackBusy    by remember { mutableStateOf(false) }
    var rollbackMsg     by remember { mutableStateOf("") }
    var cleanupBusy     by remember { mutableStateOf(false) }
    var cleanupMsg      by remember { mutableStateOf("") }
    var showRollback    by remember { mutableStateOf(false) }
    var showCleanup     by remember { mutableStateOf(false) }

    val engine = remember {
        DevPatchEngine(context,
            onLog      = { msg -> patchLogs.add(msg) },
            onProgress = { cur, tot, lbl -> patchStep = cur; patchTotal = tot; patchLabel = lbl }
        )
    }

    // ── Async loaders — all I/O on Dispatchers.IO, never block main thread ──

    /** Reload storage info + backup list in background. */
    fun refreshStorage() {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    freeBytes = GameUtils.freeBytesSdcard()
                    totalBytes = GameUtils.totalBytesSdcard()
                    val backups = GameUtils.listBackups()
                    backupCount = backups.size
                    // Use sum of backups already listed — avoid walking folder twice
                    backupTotalSize = backups.sumOf { it.totalSize }
                }
            }
        }
    }

    /** Reload marker + storage (called after patch apply). */
    fun refresh() {
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { marker = readMarker() }
            }
            refreshStorage()
        }
    }

    /** Fetch update manifest from server. */
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

    // ── Initial load: ALL heavy I/O in a single LaunchedEffect on IO dispatcher ──
    LaunchedEffect(Unit) {
        // Load local state first (fast ops, but still off main thread)
        withContext(Dispatchers.IO) {
            runCatching { gameInstalled = isGameInstalled(context) }
            runCatching { marker = readMarker() }
            runCatching { filesAccessGranted = StorageAccess.isGranted() }
            runCatching {
                freeBytes = GameUtils.freeBytesSdcard()
                totalBytes = GameUtils.totalBytesSdcard()
                val backups = GameUtils.listBackups()
                backupCount = backups.size
                backupTotalSize = backups.sumOf { it.totalSize }
            }
        }
        // Then fetch network data (slow op)
        checkUpdate()
    }

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

        // ── All-Files Access permission card ──
        // Refresh state when screen becomes visible (e.g. after returning from settings)
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    filesAccessGranted = StorageAccess.isGranted()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            try { awaitCancellation() } finally { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        AnimatedVisibility(!filesAccessGranted, enter = fadeIn(), exit = fadeOut()) {
            GlassCard(borderColor = CandyCyan.copy(0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).background(CandyCyan.copy(0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Security, null, tint = CandyCyan, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Izinkan Akses File", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("Agar launcher bisa apply patch mod langsung tanpa Shizuku / root / file manager.", color = SoftText, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Launcher butuh izin 'Akses semua file' untuk menulis file patch (mis. platform.ini) ke folder data FIFA 16. " +
                    "Tanpa izin ini, kamu harus pakai Shizuku / root sebagai alternatif.",
                    color = SoftText, fontSize = 12.sp, lineHeight = 17.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick  = { StorageAccess.request(context) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = CandyCyan, contentColor = Color(0xFF00111D)
                    )
                ) {
                    Icon(Icons.Rounded.Security, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Izinkan Sekarang", fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Confirmation card when permission is granted
        AnimatedVisibility(filesAccessGranted, enter = fadeIn(), exit = fadeOut()) {
            GlassCard(borderColor = NeonGreen.copy(0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(36.dp).background(NeonGreen.copy(0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Akses File Aktif", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("Patch mod akan diapply otomatis tanpa Shizuku / root.", color = SoftText, fontSize = 11.sp)
                    }
                }
            }
        }

        // ── Storage Info card ──
        val freeLabel    = GameUtils.formatBytes(freeBytes)
        val totalLabel   = GameUtils.formatBytes(totalBytes)
        val usedPct      = if (totalBytes > 0) ((totalBytes - freeBytes) * 100 / totalBytes).toInt() else 0
        val lowStorage   = freeBytes in 1L..(5L * 1024 * 1024 * 1024)  // < 5 GB
        val noStorage    = freeBytes == 0L && totalBytes == 0L
        GlassCard(borderColor = if (lowStorage) AmberWarn.copy(0.5f) else GlassStroke) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(
                        if (lowStorage) AmberWarn.copy(0.12f) else CandyBlue.copy(0.10f),
                        RoundedCornerShape(12.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Storage, null, tint = if (lowStorage) AmberWarn else CandyCyan, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Penyimpanan Perangkat", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (noStorage) "Tidak dapat membaca penyimpanan" else "$freeLabel bebas dari $totalLabel total",
                        color = if (lowStorage) AmberWarn else SoftText, fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress   = { usedPct / 100f },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = if (lowStorage) AmberWarn else CandyCyan,
                trackColor = GlassStroke
            )
            if (lowStorage) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "⚠ Storage menipis. Patch OBB/data besar (>1 GB) mungkin gagal. Hapus file tidak terpakai atau backup lama.",
                    color = AmberWarn, fontSize = 11.sp, lineHeight = 15.sp
                )
            }
        }

        // ── Maintenance card: Backup Rollback + Cleanup ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).background(CandyBlue.copy(0.10f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Security, null, tint = CandyCyan, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Pemulihan & Pembersihan", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (backupCount == 0) "Belum ada backup patch."
                        else "$backupCount backup tersimpan · ${GameUtils.formatBytes(backupTotalSize)}",
                        color = SoftText, fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Rollback button (only enabled if backup exists)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = { showRollback = true },
                    enabled  = backupCount > 0 && !rollbackBusy && !patching,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, if (backupCount > 0 && !rollbackBusy) CandyCyan.copy(0.5f) else GlassStroke)
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = if (backupCount > 0 && !rollbackBusy) CandyCyan else SoftText, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rollback", color = if (backupCount > 0 && !rollbackBusy) CandyCyan else SoftText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick  = { showCleanup = true },
                    enabled  = backupCount > 0 && !cleanupBusy,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.dp, if (backupCount > 0 && !cleanupBusy) AmberWarn.copy(0.5f) else GlassStroke)
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = if (backupCount > 0 && !cleanupBusy) AmberWarn else SoftText, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bersihkan", color = if (backupCount > 0 && !cleanupBusy) AmberWarn else SoftText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            if (backupCount == 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Backup otomatis dibuat setiap kali kamu apply patch. Setelah apply patch pertama, kamu bisa rollback ke versi sebelumnya kalau ada masalah.",
                    color = SubText, fontSize = 11.sp, lineHeight = 15.sp
                )
            }

            // Rollback progress / result message
            AnimatedVisibility(rollbackBusy || rollbackMsg.isNotEmpty()) {
                GlassInfoBox(
                    icon  = if (rollbackBusy) Icons.Rounded.Refresh else Icons.Rounded.Info,
                    color = if (rollbackBusy) CandyCyan else if (rollbackMsg.startsWith("OK", true)) NeonGreen else DangerRed,
                    text  = if (rollbackBusy) "Memulihkan backup..." else rollbackMsg
                )
            }

            // Cleanup progress / result message
            AnimatedVisibility(cleanupBusy || cleanupMsg.isNotEmpty()) {
                GlassInfoBox(
                    icon  = if (cleanupBusy) Icons.Rounded.Refresh else Icons.Rounded.Info,
                    color = if (cleanupBusy) AmberWarn else if (cleanupMsg.startsWith("OK", true)) NeonGreen else DangerRed,
                    text  = if (cleanupBusy) "Membersihkan backup lama..." else cleanupMsg
                )
            }
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

    // ── Rollback confirmation dialog ──
    if (showRollback) {
        AlertDialog(
            onDismissRequest = { showRollback = false },
            title            = { Text("Kembalikan ke Backup Terakhir?", color = Color.White, fontWeight = FontWeight.Black) },
            text             = {
                Text(
                    "File patch terakhir akan diganti dengan versi backup sebelumnya. " +
                    "Gunakan ini kalau patch baru bikin game error / rusak.\n\n" +
                    "Catatan: game harus ditutup dulu sebelum rollback.",
                    color = SoftText, fontSize = 13.sp
                )
            },
            confirmButton    = {
                TextButton(onClick = {
                    showRollback = false
                    rollbackBusy = true; rollbackMsg = ""
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { engine.restoreLastBackup() }
                        }
                        result.onSuccess {
                            rollbackMsg = "OK: Backup berhasil dipulihkan. Buka FIFA 16 untuk cek."
                            // Decrement local version to allow re-apply later
                            engine.resetLocalVersion((engine.localVersion() - 1).coerceAtLeast(1))
                            refresh()
                        }
                        result.onFailure { rollbackMsg = "Error: ${it.message ?: "rollback gagal"}" }
                        rollbackBusy = false
                    }
                }) { Text("Pulihkan", color = CandyCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton    = {
                TextButton(onClick = { showRollback = false }) { Text("Batal", color = SoftText) }
            },
            containerColor   = GlassBase
        )
    }

    // ── Cleanup confirmation dialog ──
    if (showCleanup) {
        AlertDialog(
            onDismissRequest = { showCleanup = false },
            title            = { Text("Bersihkan Backup Lama?", color = Color.White, fontWeight = FontWeight.Black) },
            text             = {
                Text(
                    "Backup yang lebih lama dari 30 hari akan dihapus permanen untuk menghemat penyimpanan. " +
                    "Backup terbaru akan tetap dipertahankan untuk rollback.\n\n" +
                    "Backup saat ini: $backupCount (${GameUtils.formatBytes(backupTotalSize)})",
                    color = SoftText, fontSize = 13.sp
                )
            },
            confirmButton    = {
                TextButton(onClick = {
                    showCleanup = false
                    cleanupBusy = true; cleanupMsg = ""
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { GameUtils.cleanupOldBackups() }
                        }
                        result.onSuccess { (count, freed) ->
                            cleanupMsg = if (count == 0) "OK: Tidak ada backup lama (>30 hari) untuk dihapus."
                            else "OK: $count backup dihapus. ${GameUtils.formatBytes(freed)} dibebaskan."
                            refreshStorage()
                        }
                        result.onFailure { cleanupMsg = "Error: ${it.message ?: "cleanup gagal"}" }
                        cleanupBusy = false
                    }
                }) { Text("Bersihkan", color = AmberWarn, fontWeight = FontWeight.Bold) }
            },
            dismissButton    = {
                TextButton(onClick = { showCleanup = false }) { Text("Batal", color = SoftText) }
            },
            containerColor   = GlassBase
        )
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
    // Load gameInstalled async to avoid blocking main thread
    var gameInstalled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { runCatching { gameInstalled = isGameInstalled(context) } }
    }
    var confirmLogout by remember { mutableStateOf(false) }
    val initial = api.displayName().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    val role    = api.role()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Hero Avatar Card (premium gradient ring + glow) ──
        PremiumGlassCard(gradientBorder = true) {
            val infiniteTransition = rememberInfiniteTransition(label = "profile_glow")
            val avatarGlow by infiniteTransition.animateFloat(
                initialValue = 0.25f, targetValue = 0.55f,
                animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "avatar_glow_val"
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar with gradient ring + glow
                Box(
                    Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(CandyCyan.copy(avatarGlow * 0.6f), Color.Transparent),
                                    radius = 80f
                                )
                            )
                            .blur(12.dp)
                    )
                    // Rotating gradient ring
                    val ringRotation by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
                        label = "ring_rotation"
                    )
                    Canvas(
                        Modifier
                            .size(72.dp)
                            .graphicsLayer { rotationZ = ringRotation }
                    ) {
                        val stroke = 3.dp.toPx()
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    CandyCyan, PremiumViolet, CandyCyan.copy(0.3f), CandyCyan
                                )
                            ),
                            radius = (size.minDimension / 2f) - (stroke / 2f),
                            style = Stroke(width = stroke)
                        )
                    }
                    // Inner avatar
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(CandyCyan, CandyBlue, PremiumViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initial, fontSize = 24.sp, fontWeight = FontWeight.Black,
                            color = Carbon
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        api.displayName().ifEmpty { "DLavie Player" },
                        fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color.White
                    )
                    Text(
                        "@${api.username().ifEmpty { "unknown" }}",
                        color = SoftText, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    ModernPill(role.uppercase(), roleBadgeColor(role))
                }
            }
            Spacer(Modifier.height(14.dp))
            // Account details tiles
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

        // ── Akun & Keamanan (Account Settings) ──
        AccountSettingsCard(api = api, context = context)

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

// ─── Account Settings Card (Profile screen) ──────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsCard(api: CommunityApi, context: android.content.Context) {
    val scope = rememberCoroutineScope()
    var expandedSection by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(PinManager.hasPin(context)) }

    // Field states
    var newPass by remember { mutableStateOf("") }
    var newPassConfirm by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf(api.username()) }
    var newDisplayName by remember { mutableStateOf(api.displayName()) }

    fun execute(call: suspend () -> String) {
        working = true; resultMsg = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { call() } }
            result.onSuccess {
                isSuccess = it.startsWith("OK")
                resultMsg = it
                if (isSuccess && expandedSection == "profile") {
                    // Update local prefs from new values
                    context.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE).edit()
                        .putString("username", newUsername.trim())
                        .putString("display_name", newDisplayName.trim())
                        .apply()
                }
            }
            result.onFailure {
                isSuccess = false
                resultMsg = "Error: ${it.message ?: "operasi gagal"}"
            }
            working = false
        }
    }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Security, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Akun & Keamanan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Kelola password, email, profil, dan PIN launcher dari sini.",
            color = SoftText, fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))

        // ── Section: Ganti Password ──
        SettingRow(
            icon = Icons.Rounded.Lock,
            title = "Ganti Password",
            subtitle = "Minimal 6 karakter. Sesi lain akan otomatis logout.",
            expanded = expandedSection == "password",
            onToggle = { expandedSection = if (expandedSection == "password") null else "password"; resultMsg = "" }
        ) {
            ModernTextField(
                value = newPass,
                onValueChange = { newPass = it },
                label = "Password Baru",
                isPassword = true
            )
            Spacer(Modifier.height(8.dp))
            ModernTextField(
                value = newPassConfirm,
                onValueChange = { newPassConfirm = it },
                label = "Konfirmasi Password Baru",
                isPassword = true
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick  = {
                    if (newPass != newPassConfirm) {
                        isSuccess = false; resultMsg = "Error: Password tidak cocok."; return@Button
                    }
                    execute { AuthManager.updatePassword(api.token(), newPass) }
                    newPass = ""; newPassConfirm = ""
                },
                enabled  = !working && newPass.length >= 6 && newPass == newPassConfirm,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))
            ) {
                Text(if (working) "Memproses..." else "Ubah Password", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Section: Ganti Email ──
        SettingRow(
            icon = Icons.Rounded.Info,
            title = "Ganti Email",
            subtitle = "Email konfirmasi akan dikirim ke email baru.",
            expanded = expandedSection == "email",
            onToggle = { expandedSection = if (expandedSection == "email") null else "email"; resultMsg = "" }
        ) {
            ModernTextField(
                value = newEmail,
                onValueChange = { newEmail = it.trim() },
                label = "Email Baru"
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick  = { execute { AuthManager.updateEmail(api.token(), newEmail) }; newEmail = "" },
                enabled  = !working && newEmail.contains("@") && newEmail.contains("."),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CandyBlue, contentColor = Color.White)
            ) {
                Text(if (working) "Memproses..." else "Kirim Konfirmasi", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Section: Ganti Profil (Username & Display Name) ──
        SettingRow(
            icon = Icons.Rounded.AccountCircle,
            title = "Ganti Profil",
            subtitle = "Username (3-24, a-z 0-9 _) dan Display Name (2-40).",
            expanded = expandedSection == "profile",
            onToggle = { expandedSection = if (expandedSection == "profile") null else "profile"; resultMsg = "" }
        ) {
            ModernTextField(
                value = newUsername,
                onValueChange = { raw ->
                    newUsername = raw.trim().lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(24)
                },
                label = "Username"
            )
            Spacer(Modifier.height(8.dp))
            ModernTextField(
                value = newDisplayName,
                onValueChange = { newDisplayName = it.take(40) },
                label = "Display Name"
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick  = {
                    execute {
                        AuthManager.updateProfile(api.token(), api.userId(), newUsername, newDisplayName)
                    }
                },
                enabled  = !working &&
                           newUsername.matches(Regex("[a-zA-Z0-9_]{3,24}")) &&
                           newDisplayName.trim().length in 2..40,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))
            ) {
                Text(if (working) "Memproses..." else "Simpan Profil", fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Section: PIN Launcher ──
        SettingRow(
            icon = Icons.Rounded.Lock,
            title = if (pinEnabled) "PIN Launcher Aktif" else "PIN Launcher",
            subtitle = if (pinEnabled) "Klik untuk ubah / nonaktifkan PIN."
                       else "Lindungi launcher dengan PIN 6-digit.",
            expanded = expandedSection == "pin",
            onToggle = { expandedSection = if (expandedSection == "pin") null else "pin"; resultMsg = "" }
        ) {
            if (pinEnabled) {
                Button(
                    onClick  = {
                        PinLockActivity.launch(context, PinLockActivity.MODE_CHANGE)
                    },
                    enabled  = !working,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))
                ) {
                    Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ubah PIN", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick  = {
                        PinLockActivity.launch(context, PinLockActivity.MODE_DISABLE)
                        // After disable activity returns, refresh state on resume
                        expandedSection = null
                    },
                    enabled  = !working,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, DangerRed.copy(0.5f))
                ) {
                    Text("Nonaktifkan PIN", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Button(
                    onClick  = {
                        PinLockActivity.launch(context, PinLockActivity.MODE_SETUP)
                    },
                    enabled  = !working,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = CandyBlue, contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Setup PIN 6-digit", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }

        // Refresh PIN state on resume (after returning from PinLockActivity)
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    pinEnabled = PinManager.hasPin(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            try { kotlinx.coroutines.awaitCancellation() } finally { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Result message
        AnimatedVisibility(resultMsg.isNotEmpty()) {
            GlassInfoBox(
                icon  = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                color = if (isSuccess) NeonGreen else DangerRed,
                text  = resultMsg
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        color = Color(0xFF0F1828),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (expanded) CandyCyan.copy(0.5f) else GlassStroke)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { onToggle() }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(32.dp).background(CandyBlue.copy(0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = SoftText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null, tint = SoftText, modifier = Modifier.size(20.dp)
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp), content = content)
            }
        }
    }
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation()
                                else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CandyCyan,
            unfocusedBorderColor = GlassStroke,
            focusedLabelColor = CandyCyan,
            unfocusedLabelColor = SoftText,
            cursorColor = CandyCyan,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

// ─── Helper functions ─────────────────────────────────────────────────────────

fun isGameInstalled(context: android.content.Context): Boolean =
    try { context.packageManager.getPackageInfo(GAME_PKG, 0); true } catch (_: Exception) { false }

fun readMarker(): String =
    try { File(MARKER_PATH).readText().trim() } catch (_: Exception) { "" }

fun fetchUpdateInfo(): UpdateInfo {
    // v7.9.76: Cache-bust setiap fetch supaya CDN tidak return manifest lama.
    val url = if (DEFAULT_MANIFEST.contains("?")) "$DEFAULT_MANIFEST&_ts=${System.currentTimeMillis()}"
              else "$DEFAULT_MANIFEST?_ts=${System.currentTimeMillis()}"
    val json = fetchJson(url)
    // v7.9.74: Read from launcher section (new manifest format)
    val launcher = json.optJSONObject("launcher")
    val latestCode = if (launcher != null) {
        launcher.optInt("latest_version_code", json.optInt("version", LOCAL_VER))
    } else {
        json.optInt("version", json.optInt("latestVersionCode", LOCAL_VER))
    }
    val latestName = if (launcher != null) {
        launcher.optString("latest_version_name", "v$latestCode")
    } else {
        json.optString("latestVersionName", "v$latestCode")
    }
    val notesArr = if (launcher != null) {
        launcher.optJSONArray("release_notes")
    } else {
        json.optJSONArray("releaseNotes")
    }
    val notes = if (notesArr != null) List(notesArr.length()) { i -> notesArr.optString(i) } else emptyList()
    android.util.Log.i("UpdateCheck", "fetchUpdateInfo: latestCode=$latestCode, LOCAL_VER=$LOCAL_VER, upToDate=${latestCode <= LOCAL_VER}")
    return UpdateInfo(latestCode, latestName, latestCode <= LOCAL_VER, notes)
}

/**
 * v7.9.76: Ambil URL APK launcher dari manifest (untuk self-update).
 * Return apk_url dari launcher section, atau fallback ke hardcoded v248 URL.
 */
fun fetchLauncherApkUrl(): String {
    return try {
        val url = if (DEFAULT_MANIFEST.contains("?")) "$DEFAULT_MANIFEST&_ts=${System.currentTimeMillis()}"
                  else "$DEFAULT_MANIFEST?_ts=${System.currentTimeMillis()}"
        val json = fetchJson(url)
        val launcher = json.optJSONObject("launcher")
        launcher?.optString("apk_url", "")?.takeIf { it.isNotBlank() }
            ?: "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/DLavie26-Launcher-v248.apk"
    } catch (t: Throwable) {
        android.util.Log.e("UpdateCheck", "fetchLauncherApkUrl failed", t)
        "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/DLavie26-Launcher-v248.apk"
    }
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
