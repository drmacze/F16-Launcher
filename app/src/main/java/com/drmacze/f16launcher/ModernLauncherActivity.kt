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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import kotlin.math.sin
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Design tokens sekarang ada di ModernUI.kt (premium palette v2.0)

// ─── Constants ────────────────────────────────────────────────────────────────
private const val GAME_PKG         = "com.ea.gp.fifaworld"
private const val FIFA_APK_URL     = "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/DLavie26.apk"
private const val DEFAULT_MANIFEST = "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/manifest.json"
private const val MARKER_PATH      = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private const val LOCAL_VER        = 1
private const val LOCAL_VER_NAME   = "v1"

// ─── Data models ──────────────────────────────────────────────────────────────
data class CategoryItem(val id: String, val name: String, val description: String)
data class TopicItem(val id: String, val title: String, val body: String, val replyCount: Int, val createdAt: String)
data class PostItem(val id: String, val authorId: String, val body: String, val createdAt: String)
data class FeedItem(val id: String, val title: String, val body: String, val type: String, val pinned: Boolean, val official: Boolean, val imageUrl: String = "", val createdAt: String = "")

/**
 * Update info — sekarang mendukung dua sumber:
 *   - "supabase": dari Dev Dashboard update_posts (prioritas jika user login & ada data)
 *   - "manifest" : fallback ke GitHub manifest.json (DEFAULT_MANIFEST)
 *
 * Field patchUrl/sha256/size/critical/restartRequired hanya terisi kalau
 * source == "supabase" (manifest legacy tidak punya info ini).
 */
data class UpdateInfo(
    val latestCode: Int,
    val latestName: String,
    val upToDate: Boolean,
    val releaseNotes: List<String>,
    val patchUrl: String = "",
    val patchSha256: String = "",
    val patchSize: Long = 0L,
    val critical: Boolean = false,
    val restartRequired: Boolean = false,
    val source: String = "manifest"
)

/**
 * Maintenance mode info — dibaca dari Supabase app_config key="maintenance".
 * value jsonb shape: { enabled, title, message, scope, allow_offline_play }
 *
 * scope:
 *   - "none"    → maintenance disabled (default)
 *   - "partial" → launcher bisa dibuka, tapi download/apply/launch diblokir
 *   - "full"    → full-screen maintenance page, user tidak bisa masuk launcher
 */
data class MaintenanceInfo(
    val enabled: Boolean,
    val title: String,
    val message: String,
    val scope: String = "none",
    val allowOfflinePlay: Boolean = true
)

/**
 * Latest notification campaign — untuk inline banner di HomeScreen.
 * Berbeda dari NotificationItem (yang untuk slide-down overlay transient),
 * NotifCampaign ini persisten sampai user dismiss.
 */
data class NotifCampaign(
    val id: String,
    val title: String,
    val body: String,
    val sentAt: String
)

/**
 * Push notification campaign item — used by the polling receiver in MainShell.
 */
data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val actionType: String,
    val actionUrl: String?,
    val targetType: String,
    val targetRole: String?
)

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

    // ── Maintenance state (Bug 1-3: full/partial scope + staff bypass) ──
    var maintenanceState by remember { mutableStateOf<MaintenanceInfo?>(null) }
    var maintenanceChecked by remember { mutableStateOf(false) }
    var partialBypassed by remember { mutableStateOf(false) } // user tekan "Masuk Launcher" saat scope=partial

    // ── Staff bypass (Bug 3): admin/developer/moderator/owner skip maintenance entirely ──
    val userRole = api.role()
    val isStaff = userRole.equals("admin", ignoreCase = true)
               || userRole.equals("developer", ignoreCase = true)
               || userRole.equals("owner", ignoreCase = true)
               || userRole.equals("moderator", ignoreCase = true)

    // If PIN is enabled and not yet verified, launch the PIN lock screen
    LaunchedEffect(Unit) {
        if (PinManager.hasPin(context) && !pinVerified) {
            PinLockActivity.launch(context, PinLockActivity.MODE_UNLOCK)
        }
    }

    // ── Fetch maintenance HANYA untuk non-staff (Bug 3) ──
    // Staff bypass: tidak perlu fetch app_config.maintenance sama sekali.
    LaunchedEffect(Unit) {
        if (!isStaff) {
            withContext(Dispatchers.IO) {
                runCatching { maintenanceState = fetchMaintenanceInfo(api) }
            }
        }
        maintenanceChecked = true
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
                // Reusable logout lambda — fires telemetry before clearing session.
                val logoutAction: () -> Unit = {
                    Telemetry.track(api, context, Telemetry.EVT_LOGOUT)
                    api.logout()
                    context.getSharedPreferences("dlavie_auth_session", Context.MODE_PRIVATE).edit().clear().apply()
                    // Also clear PIN on logout for security
                    PinManager.clearPin(context)
                    context.startActivity(
                        Intent(context, DLavieGuidedActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                when {
                    // ── Belum login → redirect ke guided login ──
                    !api.loggedIn() -> {
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
                    }

                    // ── Staff bypass: skip maintenance entirely (Bug 3) ──
                    isStaff -> {
                        if (!pinVerified && PinManager.hasPin(context)) {
                            PinLockPlaceholder(context)
                        } else {
                            MainShell(api, maintenanceInfo = null, onLogout = logoutAction)
                        }
                    }

                    // ── Non-staff + scope=full → full-screen maintenance, NO enter button (Bug 1) ──
                    // User TIDAK BISA masuk launcher sampai Dev Dashboard menonaktifkan maintenance.
                    maintenanceChecked && maintenanceState?.enabled == true
                            && maintenanceState?.scope == "full" -> {
                        FullScreenMaintenance(maintenanceState!!) {
                            // onEnter — tidak pernah dipanggil karena scope=full tidak punya button.
                            // Tetap disediakan sebagai no-op supaya signature konsisten.
                        }
                    }

                    // ── Non-staff + scope=partial → full-screen maintenance WITH "Masuk Launcher" (Bug 1) ──
                    // Setelah tap, partialBypassed=true → user masuk launcher tapi Beranda & Update blur (Bug 2).
                    maintenanceChecked && maintenanceState?.enabled == true
                            && maintenanceState?.scope == "partial"
                            && !partialBypassed -> {
                        FullScreenMaintenance(maintenanceState!!) {
                            partialBypassed = true
                        }
                    }

                    // ── PIN lock (non-staff, post-maintenance-check) ──
                    !pinVerified && PinManager.hasPin(context) -> {
                        PinLockPlaceholder(context)
                    }

                    // ── Default: masuk launcher dengan maintenance info (untuk blur overlay Bug 2) ──
                    else -> {
                        MainShell(api, maintenanceInfo = maintenanceState, onLogout = logoutAction)
                    }
                }
            }
        }
    }
}

/**
 * PIN lock placeholder — shown when user has PIN set but hasn't verified yet.
 * User taps "Buka PIN Lock" to launch PinLockActivity for verification.
 */
@Composable
private fun PinLockPlaceholder(context: android.content.Context) {
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
}

// ─── Full-screen maintenance (Bug 1: scope = "full" | "partial") ──────────────
// scope=full    → TIDAK ADA button "Masuk". User tidak bisa masuk launcher.
// scope=partial → ADA button "Masuk Launcher" (always enabled). Tap → onEnter → blur Beranda & Update.
@Composable
fun FullScreenMaintenance(
    maintenance: MaintenanceInfo,
    onEnter: () -> Unit  // dipanggil saat user tap "Masuk Launcher" (hanya untuk scope=partial)
) {
    Box(
        Modifier.fillMaxSize().background(Carbon),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Big warning icon dengan pulse
            val infiniteTransition = rememberInfiniteTransition(label = "maint_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.1f,
                animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "maint_pulse_scale"
            )
            Box(
                Modifier.size(96.dp).scale(pulseScale)
                    .background(AmberWarn.copy(0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Warning, null, tint = AmberWarn, modifier = Modifier.size(48.dp))
            }

            Text(
                maintenance.title.ifEmpty { "Sistem Sedang Maintenance" },
                color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            if (maintenance.message.isNotEmpty()) {
                Text(
                    maintenance.message,
                    color = SoftText, fontSize = 14.sp, lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Bug 1: HANYA tampilkan button jika scope=partial ──
            // scope=full → TIDAK ADA button "Masuk" sama sekali.
            if (maintenance.scope == "partial") {
                Button(
                    onClick = onEnter,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CandyCyan,
                        contentColor = Carbon
                    )
                ) {
                    Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Masuk Launcher", fontWeight = FontWeight.Black)
                }
                Text(
                    "Beberapa fitur dibatasi. Komunitas & Profil tetap tersedia.",
                    color = SubText, fontSize = 11.sp, textAlign = TextAlign.Center
                )
            } else {
                // scope=full — tidak ada button. User tidak bisa masuk launcher.
                Text(
                    "Launcher tidak dapat diakses saat maintenance penuh.",
                    color = SubText, fontSize = 12.sp, textAlign = TextAlign.Center
                )
                Text(
                    "Silakan coba lagi nanti.",
                    color = SubText, fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "DLavie 26 · ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
                color = SubText, fontSize = 10.sp
            )
        }
    }
}

// ─── Main shell ───────────────────────────────────────────────────────────────
@Composable
fun MainShell(api: CommunityApi, maintenanceInfo: MaintenanceInfo? = null, onLogout: () -> Unit) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(Page.Home) }

    // ── Active notification banner state (Module 3: Push Notification Receiver) ──
    var activeBanner by remember { mutableStateOf<NotificationItem?>(null) }

    LaunchedEffect(Unit) {
        // Fire app_open telemetry as soon as the shell mounts.
        Telemetry.track(api, context, Telemetry.EVT_APP_OPEN)
        // Initial token refresh loop (existing behavior).
        while (true) {
            delay(50L * 60_000)
            withContext(Dispatchers.IO) { runCatching { api.refreshToken() } }
        }
    }

    // ── Poll notification_campaigns every 60s for new sent campaigns ──
    LaunchedEffect(Unit) {
        // Run once immediately on mount.
        runCatching {
            val fresh = withContext(Dispatchers.IO) { fetchUnseenNotifications(context, api) }
            if (fresh != null) activeBanner = fresh
        }
        while (true) {
            delay(60L * 1000L)
            runCatching {
                val fresh = withContext(Dispatchers.IO) { fetchUnseenNotifications(context, api) }
                if (fresh != null) activeBanner = fresh
            }
        }
    }

    // ── Auto-dismiss the banner after 5 seconds ──
    LaunchedEffect(activeBanner?.id) {
        val banner = activeBanner ?: return@LaunchedEffect
        delay(5_000L)
        if (activeBanner?.id == banner.id) activeBanner = null
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
            // ── Bug 2: Partial maintenance → Beranda & Update blur total ──
            val isPartialMaintenance = maintenanceInfo?.enabled == true && maintenanceInfo?.scope == "partial"

            when (target) {
                Page.Home   -> Box {
                    HomeScreen(api, maintenanceInfo = maintenanceInfo, onNav = { page = it })
                    if (isPartialMaintenance) {
                        PartialMaintenanceOverlay(
                            title   = "Beranda Diblokir",
                            message = "Maintenance mode aktif. Hanya Komunitas & Profil yang tersedia.",
                            onNavChat = { page = Page.Chat },
                            onNavMe   = { page = Page.Me }
                        )
                    }
                }
                Page.Update -> Box {
                    UpdateScreen(api, maintenanceInfo = maintenanceInfo, onNav  = { page = it })
                    if (isPartialMaintenance) {
                        PartialMaintenanceOverlay(
                            title   = "Update Diblokir",
                            message = "Maintenance mode aktif.",
                            onNavChat = { page = Page.Chat },
                            onNavMe   = { page = Page.Me }
                        )
                    }
                }
                Page.Chat   -> CommunityScreen(api)   // normal, no blur
                Page.Me     -> ProfileScreen(api, onLogout)   // normal, no blur
            }
        }
        FloatingNav(
            page     = page,
            onPage   = { page = it },
            modifier = Modifier.align(Alignment.BottomCenter)
                               .navigationBarsPadding()
                               .padding(bottom = 12.dp)
        )

        // ── Notification banner overlay (slides down from the top) ──
        activeBanner?.let { banner ->
            Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                NotificationBanner(
                    title      = banner.title,
                    body       = banner.body,
                    action     = banner.actionType,
                    actionUrl  = banner.actionUrl,
                    onDismiss  = { activeBanner = null },
                    onAction   = {
                        if (banner.actionType == "open_url" && !banner.actionUrl.isNullOrBlank()) {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(banner.actionUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                        activeBanner = null
                    }
                )
            }
        }
    }
}

// ─── Partial maintenance blur overlay (Bug 2) ──────────────────────────────────
// Saat scope=partial, Beranda & Update ditutup overlay gelap + blur,
// user TIDAK BISA interact dengan content di belakangnya.
// Komunitas & Profil tetap normal.
@Composable
private fun PartialMaintenanceOverlay(
    title: String,
    message: String,
    onNavChat: () -> Unit,
    onNavMe: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))   // 80% black tint
            .blur(20.dp),                     // total blur untuk content di belakang
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(Icons.Rounded.Lock, null, tint = AmberWarn, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                color = SoftText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNavChat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CandyCyan,
                        contentColor = Carbon
                    )
                ) { Text("Ke Komunitas", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = onNavMe,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CandyBlue,
                        contentColor = Color.White
                    )
                ) { Text("Ke Profil", fontWeight = FontWeight.Bold) }
            }
        }
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
fun HomeScreen(api: CommunityApi, maintenanceInfo: MaintenanceInfo? = null, onNav: (Page) -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Setup state detection ──
    var setupState   by remember { mutableStateOf(SetupState.LOADING) }
    var gameInstalled by remember { mutableStateOf(false) }
    var dataReady     by remember { mutableStateOf(false) }
    var updateInfo    by remember { mutableStateOf<UpdateInfo?>(null) }
    var feed          by remember { mutableStateOf<List<FeedItem>>(emptyList()) }

    // ── Maintenance & notification banner state (Dev Dashboard integration) ──
    // maintenanceInfo (dari MainShell) dipakai untuk blocking logic (Bug 2).
    // maintenanceState (lokal, di-refresh saat pull-to-refresh) dipakai untuk banner display.
    var maintenanceState by remember { mutableStateOf(maintenanceInfo) }
    var latestNotif      by remember { mutableStateOf<NotifCampaign?>(null) }

    // ── NEW: Rating state (game_ratings table, real data from Supabase) ──
    var avgRating      by remember { mutableStateOf(0.0) }   // 1-5 scale
    var ratingCount    by remember { mutableStateOf(0) }
    var myRating       by remember { mutableStateOf(0) }     // 1-5, or 0 if not rated
    var showRatingPopup by remember { mutableStateOf(false) }
    var ratingSubmitError by remember { mutableStateOf("") }

    // ── NEW: Notification category popup state ──
    var showNotifPopup  by remember { mutableStateOf(false) }
    // notifCategory tracks the user's current filter (default "all"). Tidak dipakai
    // untuk navigasi ke screen lain — hanya popup filter lokal di Home.
    var notifCategory   by remember { mutableStateOf("all") }
    // Filtered notification list (fetched saat kategori dipilih). Bisa kosong.
    var notifList       by remember { mutableStateOf<List<NotifCampaign>>(emptyList()) }
    var notifListOpen   by remember { mutableStateOf(false) }

    // ── Bug 2: Partial maintenance blocking ──
    // Kalau scope=partial, block download/apply/launch tapi allow komunitas & profile.
    val maintenanceBlocked = maintenanceInfo?.enabled == true && maintenanceInfo?.scope == "partial"

    // ── Pull-to-refresh state ──
    val pullState    = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Reusable data loader — dipanggil oleh LaunchedEffect awal & onRefresh
    suspend fun loadAllData() {
        withContext(Dispatchers.IO) {
            gameInstalled = isGameInstalled(context)
            dataReady     = readMarker().startsWith("v26", ignoreCase = true)
            runCatching { updateInfo = fetchUpdateInfo(api) }
            runCatching { feed       = parseFeed(api.feedPosts()) }
            // Banner data — fail-open, tidak pernah crash launcher.
            runCatching { maintenanceState = fetchMaintenanceInfo(api) }
            // Hanya fetch notif campaign kalau banner sebelumnya sudah di-dismiss
            // (latestNotif == null) supaya tidak menimpa dismiss user saat refresh.
            if (latestNotif == null) {
                runCatching { latestNotif = fetchLatestNotifCampaign(api) }
            }
            // ── NEW: rating stats (fail-open — table mungkin belum ada di schema lama) ──
            runCatching {
                val stats = api.fetchRatingStats()
                avgRating   = stats.optDouble("avg", 0.0)
                ratingCount = stats.optInt("count", 0)
            }
            // ── NEW: my rating (hanya kalau login) ──
            if (api.loggedIn()) {
                runCatching { myRating = api.getMyRating() }
            }
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
        // Telemetry: download_apk event — fire-and-forget.
        Telemetry.track(api, context, Telemetry.EVT_DOWNLOAD_APK, mapOf("source" to "github_releases", "url" to FIFA_APK_URL))
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

        // ── Maintenance banner (cek app_config.maintenance via Supabase) ──
        // Hanya tampil kalau Dev Dashboard mengaktifkan maintenance mode.
        maintenanceState?.let { m ->
            if (m.enabled) {
                GlassCard(borderColor = AmberWarn.copy(alpha = 0.6f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Warning, null,
                            tint = AmberWarn, modifier = Modifier.size(22.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                m.title.ifEmpty { "Maintenance Mode" },
                                color = AmberWarn, fontSize = 14.sp, fontWeight = FontWeight.Black
                            )
                            if (m.message.isNotEmpty()) {
                                Text(
                                    m.message,
                                    color = SoftText, fontSize = 11.sp, lineHeight = 14.sp
                                )
                            }
                            if (m.scope == "partial") {
                                Text(
                                    "Mode partial: download/apply/launch diblokir. Komunitas & Profil tetap bisa diakses.",
                                    color = AmberWarn, fontSize = 10.sp, lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Notification banner (dari notification_campaigns, latest sent) ──
        // Inline persisten — berbeda dari slide-down overlay di MainShell yang
        // auto-dismiss 5 detik. Banner ini tetap tampil sampai user tutup.
        latestNotif?.let { notif ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + expandVertically()
            ) {
                GlassCard(borderColor = CandyCyan.copy(alpha = 0.5f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Notifications, null,
                            tint = CandyCyan, modifier = Modifier.size(20.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                notif.title.ifEmpty { "Notifikasi DLavie" },
                                color = Color.White, fontSize = 13.sp,
                                fontWeight = FontWeight.Black, maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (notif.body.isNotEmpty()) {
                                Text(
                                    notif.body,
                                    color = SoftText, fontSize = 11.sp, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            Icons.Rounded.Close, "Tutup notifikasi",
                            tint = SubText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { latestNotif = null }
                        )
                    }
                }
            }
        }

        // ── Top Bar ─────────────────────────────────────────────────────────────
        // Kiri: "DLavie 26" text (bold, white). Kanan: notification bell icon.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(32.dp)
                        .background(
                            Brush.linearGradient(listOf(CandyCyan, CandyBlue, PremiumViolet)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DL", color = Carbon, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                Text("DLavie 26", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Box {
                Icon(
                    Icons.Rounded.Notifications, "Notifikasi",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).clickable { showNotifPopup = true }
                )
                // Tiny unread dot indicator (subtle, kalau ada latestNotif belum di-dismiss)
                if (latestNotif != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 1.dp, y = (-1).dp)
                            .size(7.dp)
                            .background(DangerRed, CircleShape)
                    )
                }
                DropdownMenu(
                    expanded = showNotifPopup,
                    onDismissRequest = { showNotifPopup = false },
                    modifier = Modifier.background(GlassBase)
                ) {
                    NotificationCategoryItems(
                        onCategorySelected = { cat ->
                            notifCategory = cat
                            showNotifPopup = false
                            // Fetch filtered notifications (fail-open).
                            scope.launch {
                                val list = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val arr = api.getNotificationsByCategory(15, cat)
                                        (0 until arr.length()).mapNotNull { i ->
                                            runCatching {
                                                val o = arr.getJSONObject(i)
                                                NotifCampaign(
                                                    id = o.optString("id"),
                                                    title = o.optString("title"),
                                                    body = o.optString("body"),
                                                    sentAt = o.optString("sent_at")
                                                )
                                            }.getOrNull()
                                        }
                                    }.getOrDefault(emptyList())
                                }
                                notifList = list
                                notifListOpen = true
                            }
                        }
                    )
                }
            }
        }

        // ── Hero Banner (FIFA 16 Mobile — TapTap-style) ────────────────────────
        // Pure black background + animated mesh gradient grey wave + shiny title
        // + typewriter subtitle + rating badge + DL logo cover.
        Box(
            Modifier.fillMaxWidth().height(200.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            MeshGradientBackground(Modifier.fillMaxSize())

            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: title + rating badge
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        ShinyTitle("FIFA 16 Mobile")
                        Spacer(Modifier.height(10.dp))
                        TypewriterText(
                            texts = listOf(
                                "Play FIFA 16 Mobile everywhere",
                                "Play offline, Always update, More improvement."
                            )
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    // Rating badge — pojok kanan atas banner
                    Column(horizontalAlignment = Alignment.End) {
                        val rating10 = String.format("%.1f", avgRating * 2.0)
                        Text("⭐ $rating10", color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("$ratingCount ratings", color = SoftText, fontSize = 10.sp)
                    }
                }

                // Bottom row: DL logo + small "verified" pill
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(60.dp)
                            .background(
                                Brush.linearGradient(listOf(CandyCyan, CandyBlue, PremiumViolet)),
                                CircleShape
                            )
                            .softGlow(CandyCyan, radius = 22f, alpha = 0.4f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DL", color = Carbon, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Surface(
                        color = NeonGreen.copy(0.16f),
                        border = BorderStroke(1.dp, NeonGreen.copy(0.45f)),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Verified, null, tint = NeonGreen, modifier = Modifier.size(11.dp))
                            Text("OFFICIAL", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // ── "Beri Rating DLavie 26" button (conditional: belum rate & login) ────
        AnimatedVisibility(
            visible = myRating == 0 && api.loggedIn(),
            enter = fadeIn(tween(300)) + expandVertically(),
            exit = fadeOut(tween(200)) + shrinkVertically()
        ) {
            OutlinedButton(
                onClick = { ratingSubmitError = ""; showRatingPopup = true },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AmberWarn.copy(0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarn)
            ) {
                Icon(Icons.Rounded.Star, null, tint = AmberWarn, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Beri Rating DLavie 26", color = AmberWarn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text("· bantu kami naik ranking", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Normal)
            }
        }

        // ── Game Card: "DLavie 26: Football Game" (Dapatkan / Mainkan / Diblokir) ─
        PremiumGlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp)
                        .background(
                            Brush.linearGradient(listOf(CandyCyan, CandyBlue, PremiumViolet)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DL", color = Carbon, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("DLavie 26: Football Game", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("⚽ Olahraga · FIFA 16 Mod", color = SoftText, fontSize = 11.sp)
                    val rating10 = String.format("%.1f", avgRating * 2.0)
                    Text("⭐ $rating10 · $ratingCount ratings", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                // Button: Dapatkan / Mainkan / Diblokir Maintenance
                when {
                    maintenanceBlocked -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Surface2),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, null, tint = SoftText, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Diblokir", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    gameInstalled -> {
                        Button(
                            onClick = { launchGame(context) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mainkan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = { if (dlProgress < 0f) startDownload() },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, NeonGreen),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.CloudDownload, null, tint = NeonGreen, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dapatkan", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // Inline download progress (kalau sedang unduh dari tombol "Dapatkan")
            AnimatedVisibility(
                visible = dlProgress >= 0f && dlProgress < 2f,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { dlProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = NeonGreen,
                        trackColor = NeonGreen.copy(0.15f)
                    )
                    Text(
                        "Mengunduh APK… ${(dlProgress * 100).toInt()}%",
                        color = SoftText, fontSize = 10.sp, modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            AnimatedVisibility(
                visible = dlError.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(13.dp))
                    Text(dlError, color = DangerRed, fontSize = 11.sp)
                }
            }
        }

        // ── Section "Trusted by DLavie" ────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Verified, "Verified", tint = NeonGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Trusted by DLavie", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
        Text(
            "Game resmi yang diverifikasi oleh DLavie — aman, ter-update, dan didukung komunitas.",
            color = SoftText, fontSize = 11.sp, lineHeight = 15.sp
        )

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
                        enabled  = !maintenanceBlocked && (dlProgress < 0f || dlProgress >= 2f),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = when {
                                maintenanceBlocked -> Surface2
                                dlProgress >= 2f -> NeonGreen
                                dlProgress >= 0f -> CandyBlue.copy(0.6f)
                                else             -> CandyBlue
                            },
                            contentColor   = if (maintenanceBlocked) SoftText else if (dlProgress >= 2f) Color(0xFF00150B) else Color.White,
                            disabledContainerColor = if (maintenanceBlocked) Surface2 else CandyBlue.copy(0.4f),
                            disabledContentColor   = if (maintenanceBlocked) SoftText else Color.White.copy(0.7f)
                        )
                    ) {
                        when {
                            maintenanceBlocked -> {
                                Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Diblokir Maintenance", fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
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

                    AnimatedVisibility(visible = maintenanceBlocked) {
                        Row(
                            Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Warning, null, tint = AmberWarn, modifier = Modifier.size(14.dp))
                            Text(
                                "Download APK diblokir saat maintenance mode aktif (scope: partial).",
                                color = AmberWarn, fontSize = 12.sp
                            )
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
                        onClick  = { if (!maintenanceBlocked) launchGame(context) },
                        enabled  = !maintenanceBlocked,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (maintenanceBlocked) Surface2 else AmberWarn,
                            contentColor   = if (maintenanceBlocked) SoftText else Color(0xFF1A0F00),
                            disabledContainerColor = Surface2,
                            disabledContentColor   = SoftText
                        )
                    ) {
                        if (maintenanceBlocked) {
                            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Diblokir Maintenance", fontSize = 15.sp, fontWeight = FontWeight.Black)
                        } else {
                            Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Buka FIFA 16 & Siapkan Data", fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
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
                            onClick  = { if (!maintenanceBlocked) launchGame(context) },
                            enabled  = !maintenanceBlocked,
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape    = RoundedCornerShape(24.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (maintenanceBlocked) Surface2 else NeonGreen,
                                contentColor   = if (maintenanceBlocked) SoftText else Color(0xFF00150B),
                                disabledContainerColor = Surface2,
                                disabledContentColor   = SoftText
                            )
                        ) {
                            if (maintenanceBlocked) {
                                Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Diblokir Maintenance", fontSize = 19.sp, fontWeight = FontWeight.Black)
                            } else {
                                Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Main FIFA 16", fontSize = 19.sp, fontWeight = FontWeight.Black)
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
                ) { if (!gameInstalled && !maintenanceBlocked) if (dlProgress < 0f) startDownload() }

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

        // ── Berita / Feed (Module 5: improved with type icons + pinned/official badges) ───
        AnimatedVisibility(visible = feed.isNotEmpty(), enter = fadeIn(tween(400)), exit = fadeOut()) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Notifications, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Berita Terbaru", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(10.dp))
                feed.take(4).forEach { item ->
                    FeedRow(item)
                }
            }
        }

        // Bottom spacer
        Spacer(Modifier.height(8.dp))
    }
    } // end PullToRefreshBox

    // ── Rating popup (Play Store style, 5 stars + optional review) ──────────────
    if (showRatingPopup) {
        RatingPopup(
            currentRating = myRating,
            submitError = ratingSubmitError,
            onDismiss = { showRatingPopup = false },
            onSubmit = { rating, review ->
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            api.submitRating(rating, review)
                            // Refresh stats + my rating after submit
                            val stats = api.fetchRatingStats()
                            avgRating   = stats.optDouble("avg", 0.0)
                            ratingCount = stats.optInt("count", 0)
                            myRating    = rating
                            true
                        }.getOrDefault(false)
                    }
                    if (ok) {
                        ratingSubmitError = ""
                        showRatingPopup = false
                    } else {
                        ratingSubmitError = "Gagal kirim rating. Coba lagi nanti."
                    }
                }
            }
        )
    }

    // ── Filtered notification list dialog (muncul setelah pilih kategori) ──────
    if (notifListOpen) {
        NotificationListDialog(
            category = notifCategory,
            items = notifList,
            onDismiss = { notifListOpen = false }
        )
    }
}

// ─── Mesh gradient grey wave background (animated, subtle) ──────────────────────
// Pure black base + 2 radial grey gradients (animated) + faint sine wave lines.
// Designed for Hero Banner background. Subtle (not too bright).
@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "wave1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "wave2"
    )

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base: pure black
        drawRect(Color.Black)

        // Mesh gradient 1: grey wave (top-left, animated center)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A1A1A).copy(alpha = 0.6f + wave1 * 0.2f),
                    Color(0xFF0A0A0A).copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(w * (0.2f + wave1 * 0.3f), h * (0.3f + wave1 * 0.2f)),
                radius = w * 0.8f
            )
        )

        // Mesh gradient 2: subtle grey wave (bottom-right, animated center)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF222222).copy(alpha = 0.4f + wave2 * 0.2f),
                    Color(0xFF111111).copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = Offset(w * (0.8f - wave2 * 0.2f), h * (0.7f - wave2 * 0.1f)),
                radius = w * 0.7f
            )
        )

        // Subtle wave lines (animated sine) — faint grey
        val wavePath = Path()
        for (i in 0..5) {
            val y = h * (0.2f + i * 0.15f)
            wavePath.reset()
            wavePath.moveTo(0f, y)
            var x = 0
            while (x <= w.toInt()) {
                val yOffset = sin((x + wave1 * 200 + i * 50) * 0.01f) * 15f
                wavePath.lineTo(x.toFloat(), y + yOffset)
                x += 20
            }
            drawPath(
                wavePath,
                color = Color(0xFF333333).copy(alpha = 0.08f),
                style = Stroke(width = 1f)
            )
        }
    }
}

// ─── Shiny title — silver gradient sweep animation (TextStyle brush) ────────────
// Uses Brush.linearGradient inside TextStyle — NOT BlendMode. Sweep loops 3s.
@Composable
fun ShinyTitle(text: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shiny")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )
    // sweepX travels from -1 to 2 (extends beyond text bounds for full sweep)
    val sweepX = -1f + sweepProgress * 3f

    Text(
        text = text,
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF6B7280),   // dark silver
                    Color(0xFFF9FAFB),   // bright white
                    Color(0xFFE5E7EB),   // light silver
                    Color(0xFF9CA3AF),   // mid silver
                    Color(0xFFF9FAFB),   // bright white
                    Color(0xFF6B7280)    // dark silver
                ),
                start = Offset(sweepX * 500f, 0f),
                end = Offset(sweepX * 500f + 300f, 60f)
            ),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        ),
        modifier = modifier
    )
}

// ─── Typewriter animation — loop through list of strings ───────────────────────
// 50ms/char typing, 1.5s hold, 25ms/char deleting, 300ms pause between phrases.
@Composable
fun TypewriterText(
    texts: List<String>,
    modifier: Modifier = Modifier
) {
    var textIndex by remember { mutableStateOf(0) }
    var displayedText by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }

    // Single long-lived coroutine — keys on `texts` identity only.
    LaunchedEffect(texts) {
        if (texts.isEmpty()) { displayedText = ""; return@LaunchedEffect }
        while (true) {
            val fullText = texts[textIndex]
            if (!isDeleting) {
                // Typing
                for (i in 0..fullText.length) {
                    displayedText = fullText.substring(0, i)
                    delay(50)
                }
                delay(1500)  // hold
                isDeleting = true
            } else {
                // Deleting (faster)
                for (i in fullText.length downTo 0) {
                    displayedText = fullText.substring(0, i)
                    delay(25)
                }
                isDeleting = false
                textIndex = (textIndex + 1) % texts.size
                delay(300)
            }
        }
    }

    Text(
        text = displayedText,
        color = Color(0xFF9CA3AF),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

// ─── Rating popup (Play Store style — 5 stars + optional review) ────────────────
@Composable
fun RatingPopup(
    currentRating: Int = 0,
    submitError: String = "",
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, review: String) -> Unit
) {
    var selectedRating by remember { mutableStateOf(currentRating) }
    var review by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    // Reset submitting state if user dismisses/reopens or on submit error.
    LaunchedEffect(Unit) { submitting = false }
    LaunchedEffect(submitError) { if (submitError.isNotEmpty()) submitting = false }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, null, tint = AmberWarn, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Rate DLavie 26", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bagaimana pengalaman Anda?", color = SoftText, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))

                // 5 stars — tap to select (simple, no hover, no glitch)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        val filled = i <= selectedRating
                        // Bounce animation saat star di-tap
                        val scale by animateFloatAsState(
                            targetValue = if (filled) 1.1f else 1f,
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            label = "star_scale_$i"
                        )
                        Icon(
                            if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "$i star",
                            tint = if (filled) AmberWarn else Color(0xFF374151),
                            modifier = Modifier
                                .size(40.dp)
                                .scale(scale)
                                .clickable {
                                    selectedRating = i
                                }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Review text field (optional)
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Ulasan (opsional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CandyCyan,
                        unfocusedBorderColor = GlassStroke,
                        cursorColor = CandyCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = CandyCyan,
                        unfocusedLabelColor = SoftText
                    )
                )

                if (submitError.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(submitError, color = DangerRed, fontSize = 11.sp)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        submitting = true
                        onSubmit(selectedRating, review)
                    },
                    enabled = selectedRating > 0 && !submitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Carbon)
                ) {
                    if (submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Carbon, strokeWidth = 2.dp)
                    } else {
                        Text("Kirim Rating", fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { if (!submitting) onDismiss() }) {
                Text("Nanti saja", color = SoftText)
            }
        },
        containerColor = GlassBase
    )
}

// ─── Notification category items (used inside DropdownMenu) ─────────────────────
@Composable
fun ColumnScope.NotificationCategoryItems(
    onCategorySelected: (String) -> Unit
) {
    data class Cat(val id: String, val label: String, val icon: ImageVector)
    val categories = listOf(
        Cat("all",           "Semua",        Icons.Rounded.Notifications),
        Cat("update",        "Pembaruan",    Icons.Rounded.SystemUpdate),
        Cat("announcement",  "Pengumuman",   Icons.Rounded.Campaign),
        Cat("maintenance",   "Maintenance",  Icons.Rounded.Build),
        Cat("community",     "Komunitas",    Icons.Rounded.Forum)
    )
    categories.forEach { c ->
        DropdownMenuItem(
            text = { Text(c.label, color = Color.White, fontSize = 13.sp) },
            leadingIcon = { Icon(c.icon, null, tint = CandyCyan, modifier = Modifier.size(18.dp)) },
            onClick = { onCategorySelected(c.id) },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ─── Notification list dialog (shows filtered notifications) ───────────────────
@Composable
fun NotificationListDialog(
    category: String,
    items: List<NotifCampaign>,
    onDismiss: () -> Unit
) {
    val title = when (category) {
        "update"       -> "Notifikasi · Pembaruan"
        "announcement" -> "Notifikasi · Pengumuman"
        "maintenance"  -> "Notifikasi · Maintenance"
        "community"    -> "Notifikasi · Komunitas"
        else           -> "Semua Notifikasi"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Notifications, null, tint = CandyCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            if (items.isEmpty()) {
                Text("Belum ada notifikasi di kategori ini.", color = SoftText, fontSize = 13.sp)
            } else {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEach { n ->
                        Column(Modifier.fillMaxWidth().background(Surface2.copy(0.5f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                            Text(n.title.ifEmpty { "Notifikasi DLavie" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (n.body.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(n.body, color = SoftText, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = CandyCyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {},
        containerColor = GlassBase
    )
}

// ─── Feed row (Module 5 — icon per type, pinned indicator, official badge) ────
@Composable
fun FeedRow(item: FeedItem) {
    val icon     = feedIcon(item.type)
    val iconTint = feedColor(item.type)
    Row(
        Modifier.fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Per-type icon (announcement / event / patch / update / info / default)
        Box(
            Modifier.size(28.dp)
                .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = item.type, tint = iconTint, modifier = Modifier.size(16.dp))
        }

        Column(Modifier.weight(1f)) {
            // Title row with badges
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.pinned) {
                    Surface(
                        color = AmberWarn.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.PushPin, null, tint = AmberWarn, modifier = Modifier.size(9.dp))
                            Text("PIN", color = AmberWarn, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (item.official) {
                    Surface(
                        color = NeonGreen.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.Verified, null, tint = NeonGreen, modifier = Modifier.size(9.dp))
                            Text("OFFICIAL", color = NeonGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text(
                    item.title.ifBlank { "(Tanpa judul)" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (item.pinned || item.official) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            // Body
            Text(
                item.body,
                color = SoftText,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }
    }
}

/** Resolve icon per feed type. Defaults to an Article icon. */
private fun feedIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector = when (type.lowercase().trim()) {
    "announcement", "notice"     -> Icons.Rounded.Campaign
    "event"                       -> Icons.Rounded.Event
    "patch", "update_post"        -> Icons.Rounded.SystemUpdate
    "update"                      -> Icons.Rounded.CloudSync
    "warning", "alert"            -> Icons.Rounded.Warning
    "info"                        -> Icons.Rounded.Info
    else                          -> Icons.Rounded.Article
}

private fun feedColor(type: String): Color = when (type.lowercase().trim()) {
    "announcement", "notice"     -> CandyCyan
    "event"                       -> PremiumViolet
    "patch", "update_post"        -> NeonGreen
    "update"                      -> CandyBlue
    "warning", "alert"            -> DangerRed
    "info"                        -> CandyCyan
    else                          -> SoftText
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
fun UpdateScreen(api: CommunityApi, maintenanceInfo: MaintenanceInfo? = null, onNav: (Page) -> Unit) {
    val context       = LocalContext.current
    // ── Bug 2: Partial maintenance blocking ──
    val maintenanceBlocked = maintenanceInfo?.enabled == true && maintenanceInfo?.scope == "partial"
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
            withContext(Dispatchers.IO) { runCatching { fetchUpdateInfo(api) } }
                .fold(onSuccess = { updateInfo = it }, onFailure = { updateError = it.message ?: "Gagal terhubung" })
            loading = false
        }
    }

    fun applyPatch() {
        patchLogs.clear(); patchError = ""; patchDone = false; patching = true
        scope.launch {
            // Telemetry for patch_apply (status ok/failed/noop/blocked) is fired inside
            // DevPatchEngine.applyAvailableUpdates() itself — see DevPatchEngine.kt.
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
                    enabled  = !maintenanceBlocked && !patching && !loading && patchAvailable,
                    modifier = Modifier.weight(2f).height(50.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = if (maintenanceBlocked) Surface2 else if (patchAvailable) CandyCyan else NeonGreen,
                        contentColor           = if (maintenanceBlocked) SoftText else Color(0xFF00111D),
                        disabledContainerColor = Surface2,
                        disabledContentColor   = SoftText
                    )
                ) {
                    if (maintenanceBlocked) {
                        Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Diblokir Maintenance", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    } else if (patching) {
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
                onClick  = { if (!maintenanceBlocked) launchGame(context) },
                enabled  = !maintenanceBlocked,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(20.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (maintenanceBlocked) Surface2 else NeonGreen,
                    contentColor   = if (maintenanceBlocked) SoftText else Color(0xFF00150B),
                    disabledContainerColor = Surface2,
                    disabledContentColor   = SoftText
                )
            ) {
                if (maintenanceBlocked) {
                    Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Diblokir Maintenance", fontSize = 17.sp, fontWeight = FontWeight.Black)
                } else {
                    Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Main FIFA 16 Sekarang", fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
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
                        // Telemetry for patch_rollback (status ok/failed) is fired inside
                        // DevPatchEngine.restoreLastBackup() itself — see DevPatchEngine.kt.
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

/**
 * Fetch update info — prioritas Supabase update_posts (Dev Dashboard),
 * fallback ke GitHub manifest (DEFAULT_MANIFEST) jika Supabase error / kosong /
 * user belum login.
 *
 * @param api CommunityApi instance; kalau null atau belum login, langsung fallback
 *            ke manifest. Boleh null agar caller lama tetap jalan.
 */
fun fetchUpdateInfo(api: CommunityApi? = null): UpdateInfo {
    // ── 1. Coba Supabase update_posts dulu (kalau user login & ada data) ──
    if (api != null) {
        try {
            if (api.loggedIn()) {
                val latest = api.fetchLatestUpdatePost()
                if (latest != null) {
                    val code = latest.optInt("version_code", LOCAL_VER)
                    val name = latest.optString("version_name", "").ifBlank { "v$code" }
                    val notesArr = latest.optJSONArray("release_notes")
                    val notes = if (notesArr != null) List(notesArr.length()) { i -> notesArr.optString(i) } else emptyList()
                    return UpdateInfo(
                        latestCode      = code,
                        latestName      = name,
                        upToDate        = code <= LOCAL_VER,
                        releaseNotes    = notes,
                        patchUrl        = latest.optString("patch_url", ""),
                        patchSha256     = latest.optString("patch_sha256", ""),
                        patchSize       = latest.optLong("patch_size_bytes", 0L),
                        critical        = latest.optBoolean("critical", false),
                        restartRequired = latest.optBoolean("restart_game_required", false),
                        source          = "supabase"
                    )
                }
            }
        } catch (_: Throwable) {
            // Supabase error / RLS / network — fall through ke manifest.
        }
    }

    // ── 2. Fallback ke GitHub manifest ──
    val json = fetchJson(DEFAULT_MANIFEST)
    // Support both DLavie-Launcher-Data format {version:26} and legacy {latestVersionCode:N}
    val latestCode = json.optInt("version", json.optInt("latestVersionCode", LOCAL_VER))
    val latestName = json.optString("latestVersionName", "v$latestCode")
    val notesArr   = json.optJSONArray("releaseNotes")
    val notes      = if (notesArr != null) List(notesArr.length()) { i -> notesArr.optString(i) } else emptyList()
    return UpdateInfo(latestCode, latestName, latestCode <= LOCAL_VER, notes, source = "manifest")
}

/**
 * Fetch maintenance info dari Supabase app_config (key="maintenance").
 * Mengembalikan MaintenanceInfo(enabled=false) kalau gagal / belum ada row,
 * supaya launcher tetap jalan (fail-open).
 */
fun fetchMaintenanceInfo(api: CommunityApi): MaintenanceInfo {
    return try {
        val obj = api.getAppConfig("maintenance")
        MaintenanceInfo(
            enabled         = obj.optBoolean("enabled", false),
            title           = obj.optString("title", ""),
            message         = obj.optString("message", ""),
            scope           = obj.optString("scope", "none"),
            allowOfflinePlay = obj.optBoolean("allow_offline_play", true)
        )
    } catch (_: Throwable) {
        MaintenanceInfo(enabled = false, title = "", message = "", scope = "none", allowOfflinePlay = true)
    }
}

/**
 * Fetch latest sent notification campaign (untuk inline banner di HomeScreen).
 * Mengembalikan null kalau user belum login, belum ada campaign, atau error.
 */
fun fetchLatestNotifCampaign(api: CommunityApi): NotifCampaign? {
    return try {
        if (!api.loggedIn()) return null
        val arr = api.getNotifications(1)
        if (arr.length() == 0) return null
        val n = arr.getJSONObject(0)
        NotifCampaign(
            id     = n.optString("id", ""),
            title  = n.optString("title", ""),
            body   = n.optString("body", ""),
            sentAt = n.optString("sent_at", n.optString("created_at", ""))
        )
    } catch (_: Throwable) { null }
}

fun parseFeed(arr: JSONArray): List<FeedItem> = try {
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        FeedItem(
            id        = o.optString("id"),
            title     = o.optString("title"),
            body      = o.optString("body"),
            type      = o.optString("type", "info"),
            pinned    = o.optBoolean("pinned"),
            official  = o.optBoolean("official"),
            imageUrl  = o.optString("image_url", ""),
            createdAt = o.optString("created_at", "")
        )
    }
} catch (_: Exception) { emptyList() }

fun roleBadgeColor(role: String): Color = when (role.lowercase()) {
    "admin"     -> DangerRed
    "moderator" -> NeonGreen
    "vip"       -> Color(0xFFFFD700)
    else        -> CandyCyan
}

fun launchGame(context: android.content.Context) {
    // Fire-and-forget telemetry — game_launch event.
    Telemetry.track(context, Telemetry.EVT_GAME_LAUNCH, mapOf("game_package" to GAME_PKG))
    val intent = context.packageManager.getLaunchIntentForPackage(GAME_PKG)
    if (intent != null) context.startActivity(intent)
    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FIFA_APK_URL)))
}

// ─── Push Notification polling helpers (Module 3) ────────────────────────────────
private const val SEEN_NOTIFS_PREFS = "dlavie_seen_notifs"

private fun loadSeenNotifs(context: android.content.Context): MutableSet<String> {
    return context.getSharedPreferences(SEEN_NOTIFS_PREFS, android.content.Context.MODE_PRIVATE)
        .getStringSet("seen_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
}

private fun saveSeenNotifs(context: android.content.Context, ids: Set<String>) {
    context.getSharedPreferences(SEEN_NOTIFS_PREFS, android.content.Context.MODE_PRIVATE)
        .edit().putStringSet("seen_ids", ids).apply()
}

/**
 * Polls the most recent sent notification_campaigns and returns the first unseen
 * (per SharedPreferences) one that targets the current user.
 *
 * Target resolution:
 *   - {type: "all"}           → always show
 *   - {type: "role", role: X} → show only if api.role() == X
 *
 * Side effects:
 *   - Marks fetched campaign IDs as seen (so subsequent polls don't re-show them).
 *
 * @return the newest unseen NotificationItem matching the target, or null if none.
 */
fun fetchUnseenNotifications(context: android.content.Context, api: CommunityApi): NotificationItem? {
    return try {
        val arr = api.getNotifications(5)
        if (arr.length() == 0) return null
        val seen = loadSeenNotifs(context)
        val myRole = api.role().lowercase()

        // Iterate from newest to oldest; first unseen match wins.
        for (i in 0 until arr.length()) {
            val row = arr.getJSONObject(i)
            val id = row.optString("id", "")
            if (id.isBlank() || seen.contains(id)) continue

            // Mark as seen immediately so we never re-show it.
            seen.add(id)

            // Resolve target spec — should be a jsonb object {type:"all"|"role", role?}.
            val targetObj = row.opt("target")
            val targetType: String
            val targetRole: String?
            when (targetObj) {
                is JSONObject -> {
                    targetType = targetObj.optString("type", "all")
                    targetRole = targetObj.optString("role", "").ifBlank { null }
                }
                else -> { targetType = "all"; targetRole = null }
            }

            // Target filter
            val matches = when (targetType.lowercase()) {
                "all"  -> true
                "role" -> targetRole != null && myRole == targetRole.lowercase()
                else   -> true // unknown target type — show anyway
            }

            if (!matches) continue

            // Persist seen set before returning so we don't re-poll the same id.
            saveSeenNotifs(context, seen)

            // Resolve action spec — {type:"open_app"|"open_url", url?}.
            val actionObj = row.opt("action")
            val actionType: String
            val actionUrl: String?
            when (actionObj) {
                is JSONObject -> {
                    actionType = actionObj.optString("type", "open_app")
                    actionUrl  = actionObj.optString("url", "").ifBlank { null }
                }
                else -> { actionType = "open_app"; actionUrl = null }
            }

            return NotificationItem(
                id         = id,
                title      = row.optString("title", "Notifikasi DLavie"),
                body       = row.optString("body", ""),
                actionType = actionType,
                actionUrl  = actionUrl,
                targetType = targetType,
                targetRole = targetRole
            )
        }
        // No matching unseen notification — persist seen set anyway.
        saveSeenNotifs(context, seen)
        null
    } catch (_: Exception) {
        // Network/parse error — silent failure, don't crash the UI.
        null
    }
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
