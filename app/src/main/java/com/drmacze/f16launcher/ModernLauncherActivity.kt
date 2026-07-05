package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ThumbUpOffAlt
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
// FIFA 16 (DLavie 26)
// ⚠️  PRIVACY: All download URLs point to DLavie proxy (Supabase Edge Function).
//     This protects source code privacy — users cannot trace back to GitHub repos.
//     The proxy streams files from private backend storage without exposing URLs.
//     Proxy also supports HTTP Range requests for resume.
const val GAME_PKG_16       = "com.ea.gp.fifaworld"
const val DLAVIE_PROXY_URL  = "https://lvmucsxbmadtsgrxuwmo.supabase.co/functions/v1/apk-proxy"
const val FIFA16_APK_URL    = "${DLAVIE_PROXY_URL}?f=launcher-latest"
const val MARKER_PATH_16    = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"

// FIFA 15 (DLavie 15)
const val GAME_PKG_15       = "com.ea.game.fifa14_row"
const val FIFA15_APK_URL    = "${DLAVIE_PROXY_URL}?f=fifa15-apk"
const val FIFA15_DATA_URL   = "${DLAVIE_PROXY_URL}?f=fifa15-data"
const val FIFA15_OBB_URL    = "${DLAVIE_PROXY_URL}?f=fifa15-obb"
const val MARKER_PATH_15    = "/sdcard/Android/data/com.ea.game.fifa14_row/.dlavie15_data_installed"
const val FIFA15_MAIN_ACTIVITY = "com.ea.game.fifa14.Fifa14Activity"

// Legacy aliases (for existing code that references these)
private const val GAME_PKG          = GAME_PKG_16
private const val FIFA_APK_URL      = FIFA16_APK_URL
private const val MARKER_PATH       = MARKER_PATH_16

private const val DEFAULT_MANIFEST = DLAVIE_PROXY_URL + "?f=fifa16-manifest"
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
// v7.0 redesign: PS5-style nav (Home, DLC, GameHub center, Komunitas, Profile)
enum class Page(val label: String, val navIcon: ImageVector) {
    Home    ("Home",      Icons.Rounded.Home),
    DLC     ("DLC",       Icons.Rounded.Extension),
    GameHub ("GameHub",   Icons.Rounded.SportsEsports),  // center button (PS5 stick)
    Chat    ("Komunitas", Icons.Rounded.Forum),
    Me      ("Profile",   Icons.Rounded.AccountCircle)
}

// ─── Activity ─────────────────────────────────────────────────────────────────
class ModernLauncherActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        // Apply language locale before activity creates
        val langCode = LanguageManager.getCurrentLanguage(newBase)
        val locale = java.util.Locale(langCode)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pre-create notification channel (idempotent) so the channel is ready
        // before any local notification fires (Android O+).
        NotificationHelper.createChannel(this)
        setContent { DLavieModernApp(initialPostId = intent?.getStringExtra("post_id")) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update intent so subsequent DLavieModernApp reads pick up the new post_id.
        setIntent(intent)
    }
}

// ─── Root composable ──────────────────────────────────────────────────────────
@Composable
fun DLavieModernApp(initialPostId: String? = null) {
    val context = LocalContext.current
    val api     = remember { CommunityApi(context) }
    var pinVerified by remember { mutableStateOf(!PinManager.hasPin(context)) }

    // ── Maintenance state (Bug 1-3: full/partial scope + staff bypass) ──
    var maintenanceState by remember { mutableStateOf<MaintenanceInfo?>(null) }
    var maintenanceChecked by remember { mutableStateOf(false) }
    var partialBypassed by remember { mutableStateOf(false) } // user tekan "Masuk Launcher" saat scope=partial

    // ── App update state ──
    var updateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var showUpdatePopup by remember { mutableStateOf(false) }
    var updateDownloading by remember { mutableStateOf(false) }
    var updateDownloadProgress by remember { mutableStateOf(0f) }
    val updateScope = rememberCoroutineScope()

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

    // ── Fetch maintenance untuk SEMUA user (termasuk staff) ──
    // Staff tetap lihat maintenance screen, tapi bisa bypass (tap "Masuk" untuk partial,
    // atau langsung masuk untuk full dengan tombol bypass khusus staff).
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { maintenanceState = fetchMaintenanceInfo(api) }
        }
        maintenanceChecked = true
    }

    // ── Cek app update saat app dibuka ──
    // Cek SharedPreferences: kalau user sudah dismiss versi ini, jangan show lagi
    val updatePrefs = remember { context.getSharedPreferences("dlavie_update_prefs", Context.MODE_PRIVATE) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val info = AppUpdateChecker.checkForUpdate(api)
                if (info != null && info.isUpdateAvailable) {
                    // Cek apakah user sudah dismiss versi ini
                    val dismissedVersion = updatePrefs.getInt("dismissed_version_code", -1)
                    if (dismissedVersion != info.versionCode) {
                        updateInfo = info
                        showUpdatePopup = true
                    }
                }
            }
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

    MaterialTheme(
        colorScheme = darkColorScheme(
            background   = Carbon,    surface      = GlassBase,
            primary      = AccentGreen, secondary  = SoftText,
            tertiary     = AmberWarn,
            onPrimary    = Carbon, onSecondary = Carbon, onTertiary = Carbon,
            onBackground = Color.White, onSurface  = Color.White
        ),
        // P1B: Inter font applied globally — every Text() in the app inherits
        // Inter via MaterialTheme typography. No per-call fontFamily needed.
        typography = androidx.compose.material3.Typography(
            displayLarge   = TTTypography.displayLarge,
            displayMedium  = TTTypography.displayMedium,
            displaySmall   = TTTypography.headlineLarge,
            headlineLarge  = TTTypography.headlineLarge,
            headlineMedium = TTTypography.headlineMedium,
            headlineSmall  = TTTypography.titleLarge,
            titleLarge     = TTTypography.titleLarge,
            titleMedium    = TTTypography.titleMedium,
            titleSmall     = TTTypography.labelMedium,
            bodyLarge      = TTTypography.bodyLarge,
            bodyMedium     = TTTypography.bodyMedium,
            bodySmall      = TTTypography.bodySmall,
            labelLarge     = TTTypography.labelMedium,
            labelMedium    = TTTypography.labelMedium,
            labelSmall     = TTTypography.caption
        )
    ) {
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

                // ── App Update Popup ──
                if (showUpdatePopup && updateInfo != null) {
                    AppUpdatePopup(
                        info = updateInfo!!,
                        downloading = updateDownloading,
                        progress = updateDownloadProgress,
                        onUpdate = {
                            if (!updateDownloading) {
                                val currentInfo = updateInfo
                                if (currentInfo == null || currentInfo.apkUrl.isBlank()) {
                                    // Tidak ada URL download — buka browser ke halaman release
                                    try {
                                        // PRIVACY: Don't expose GitHub repo URL. Use DLavie proxy instead.
                                        val fallbackUrl = currentInfo?.apkUrl?.takeIf { it.isNotBlank() } ?: (DLAVIE_PROXY_URL + "?f=launcher-latest")
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                                        browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(browserIntent)
                                    } catch (_: Throwable) { }
                                    showUpdatePopup = false
                                    return@AppUpdatePopup
                                }
                                updateDownloading = true
                                updateDownloadProgress = 0f
                                updateScope.launch {
                                    try {
                                        val apkFile = withContext(Dispatchers.IO) {
                                            AppUpdateChecker.downloadApk(context, currentInfo.apkUrl) { progress ->
                                                updateDownloadProgress = progress
                                            }
                                        }
                                        updateDownloading = false
                                        if (apkFile != null && apkFile.exists() && apkFile.length() > 0) {
                                            AppUpdateChecker.installApk(context, apkFile)
                                        } else {
                                            // Download gagal — buka browser sebagai fallback
                                            try {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentInfo.apkUrl))
                                                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                context.startActivity(browserIntent)
                                            } catch (_: Throwable) { }
                                        }
                                    } catch (e: Throwable) {
                                        updateDownloading = false
                                        // Error — buka browser sebagai fallback
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentInfo.apkUrl))
                                            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(browserIntent)
                                        } catch (_: Throwable) { }
                                    }
                                }
                            }
                        },
                        onLater = {
                            // Simpan dismissed versionCode — jangan show lagi untuk versi ini
                            updateInfo?.let { info ->
                                updatePrefs.edit().putInt("dismissed_version_code", info.versionCode).apply()
                            }
                            showUpdatePopup = false
                        }
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
                            MainShell(api, maintenanceInfo = null, onLogout = logoutAction, initialPostId = initialPostId)
                        }
                    }

                    // ── scope=full → full-screen maintenance ──
                    // Non-staff: NO button (blocked total)
                    // Staff: show "Masuk sebagai Admin" bypass button
                    maintenanceChecked && maintenanceState?.enabled == true
                            && maintenanceState?.scope == "full" -> {
                        FullScreenMaintenance(
                            maintenance = maintenanceState!!,
                            isStaff = isStaff,
                            onEnter = {
                                // Staff only: bypass full maintenance
                                partialBypassed = true
                            }
                        )
                    }

                    // ── scope=partial → full-screen maintenance WITH "Masuk Launcher" ──
                    maintenanceChecked && maintenanceState?.enabled == true
                            && maintenanceState?.scope == "partial"
                            && !partialBypassed -> {
                        FullScreenMaintenance(
                            maintenance = maintenanceState!!,
                            isStaff = isStaff,
                            onEnter = { partialBypassed = true }
                        )
                    }

                    // ── PIN lock (non-staff, post-maintenance-check) ──
                    !pinVerified && PinManager.hasPin(context) -> {
                        PinLockPlaceholder(context)
                    }

                    // ── Default: masuk launcher dengan maintenance info (untuk blur overlay Bug 2) ──
                    else -> {
                        MainShell(api, maintenanceInfo = maintenanceState, onLogout = logoutAction, initialPostId = initialPostId)
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
// scope=partial → ADA button "Masuk Launcher" (always enabled). Tap → onEnter.
// scope=full → Non-staff: NO button. Staff: "Masuk sebagai Admin" bypass button.
@Composable
fun FullScreenMaintenance(
    maintenance: MaintenanceInfo,
    isStaff: Boolean = false,
    onEnter: () -> Unit
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

            when {
                // scope=partial → always show "Masuk Launcher" button
                maintenance.scope == "partial" -> {
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
                }
                // scope=full + staff → show bypass button
                isStaff -> {
                    Button(
                        onClick = onEnter,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.AdminPanelSettings, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Masuk sebagai Admin", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Anda adalah admin — bypass maintenance mode.",
                        color = SubText, fontSize = 11.sp, textAlign = TextAlign.Center
                    )
                }
                // scope=full + non-staff → NO button, blocked
                else -> {
                    Text(
                        "Launcher tidak dapat diakses saat maintenance penuh.",
                        color = SubText, fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                    Text(
                        "Silakan coba lagi nanti.",
                        color = SubText, fontSize = 12.sp, textAlign = TextAlign.Center
                    )
                }
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
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainShell(
    api: CommunityApi,
    maintenanceInfo: MaintenanceInfo? = null,
    onLogout: () -> Unit,
    initialPostId: String? = null
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    // ── Phase 2 Community: deep-link dari notification tap ──
    // Jika initialPostId != null, default page = Chat (Komunitas) supaya user
    // langsung lihat feed. Kita juga tampilkan toast singkat untuk konfirmasi.
    var page by remember { mutableStateOf(if (initialPostId != null) Page.Chat else Page.Home) }
    // ── P3A: HorizontalPager for tab navigation (swipe between Home/Update/Chat/Me) ──
    // pagePagerState is declared at MainShell scope so it survives the
    // showGameDetail AnimatedContent toggling (which would otherwise dispose
    // any state declared inside its branches). Sync is bidirectional:
    //   - User swipes pager → page updated (so FloatingNav highlights the right tab)
    //   - User taps FloatingNav / onNav callback → pager animates to that page
    val pagePagerState = rememberPagerState(
        initialPage = page.ordinal,
        pageCount = { Page.values().size }
    )
    // v7.0.2 FIX: track apakah swipe sedang in-flight untuk avoid feedback loop.
    //   Sebelumnya: tap nav → set page → LaunchedEffect(page) animateScrollToPage
    //   → swipe selesai → LaunchedEffect(currentPage) set page lagi → re-trigger.
    //   Sekarang: pakai flag `isProgrammaticScroll` untuk bedakan swipe user vs
    //   programmatic animate. Hanya sync `page` saat USER swipe, bukan saat
    //   programmatic scroll settling.
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    // Pager → page (only when user swipes, not during programmatic scroll)
    LaunchedEffect(pagePagerState.currentPage) {
        if (!isProgrammaticScroll) {
            val newPage = Page.values()[pagePagerState.currentPage]
            if (newPage != page) page = newPage
        }
    }
    // page → pager (animate when page changes from outside the pager — e.g. nav tap)
    LaunchedEffect(page) {
        if (page.ordinal != pagePagerState.currentPage) {
            isProgrammaticScroll = true
            try {
                pagePagerState.animateScrollToPage(page.ordinal)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }
    val pendingPostId = remember { mutableStateOf(initialPostId) }

    // ── Active notification banner state (Module 3: Push Notification Receiver) ──
    var activeBanner by remember { mutableStateOf<NotificationItem?>(null) }

    // ── Phase 2: Game Detail screen state ──
    // Saat user tap TTGameCard di Beranda, set showGameDetail=true + capture state.
    // GameDetailScreen rendered sebagai overlay (menggantikan AnimatedContent).
    var showGameDetail by remember { mutableStateOf(false) }
    var detailGameInstalled     by remember { mutableStateOf(false) }
    var detailAvgRating         by remember { mutableStateOf(0.0) }
    var detailRatingCount       by remember { mutableStateOf(0) }
    var detailMaintenanceBlocked by remember { mutableStateOf(false) }
    // v6.8.1: lifted myRating state — shared antara HomeScreen & GameDetailScreen
    // supaya Rate button di detail screen bisa cek "sudah rating atau belum".
    // 0 = belum rating, 1-5 = sudah rating.
    var detailMyRating          by remember { mutableStateOf(0) }
    var showRatingPopup         by remember { mutableStateOf(false) }
    var ratingSubmitError       by remember { mutableStateOf("") }

    // ── Phase 4: Settings overlay state + lifted Profile expand state ──
    // profileExpandedSection di-lift ke MainShell supaya SettingsScreen bisa
    // membuka Profile dengan section tertentu (password/email/profile) ter-expand.
    var showSettings           by remember { mutableStateOf(false) }
    var profileExpandedSection by remember { mutableStateOf<String?>(null) }

    // ── Visit Profile (Task 4): user ID being viewed in UserProfileScreen overlay.
    // null = not visiting anyone; non-null = overlay shown on top of current page.
    // Tapped from CommunityScreen search results or post author names.
    var visitingUserId         by remember { mutableStateOf<String?>(null) }

    // ── Phase 2: Lifted download state (shared antara HomeScreen & GameDetailScreen) ──
    // dlProgress: -1f = idle, 0f..0.99f = downloading, 2f = done (waiting install)
    var dlProgress by remember { mutableStateOf(-1f) }
    var dlError    by remember { mutableStateOf("") }

    fun startDownload() {
        if (dlProgress >= 0f && dlProgress < 2f) return  // already downloading
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

    LaunchedEffect(Unit) {
        // Fire app_open telemetry as soon as the shell mounts.
        Telemetry.track(api, context, Telemetry.EVT_APP_OPEN)
        // v7.0.5: Presence heartbeat — update last_seen_at on app open + every 60s.
        //   Lets other users see "Online" / "Last seen 5m ago" badge in user list.
        //   Fire-and-forget (errors swallowed) — non-critical, never block UI.
        if (api.loggedIn()) {
            withContext(Dispatchers.IO) { runCatching { api.updateLastSeen() } }
        }
        // Initial token refresh loop (existing behavior) + heartbeat every 60s.
        while (true) {
            delay(60_000L)  // v7.0.5: 1 min heartbeat (was 50 min for token refresh)
            withContext(Dispatchers.IO) {
                runCatching { api.refreshToken() }
                runCatching { if (api.loggedIn()) api.updateLastSeen() }
            }
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

    // ── Phase 2 Community: request POST_NOTIFICATIONS permission (Android 13+) ──
    // Diperlukan supaya NotificationHelper.showNotification() bisa menampilkan notif.
    // Pada Android < 13, permission otomatis granted saat install.
    // Callback result diabaikan: polling tetap jalan walau permission denied (notif
    // silent fail di NotificationHelper.showNotification). User bisa enable later via
    // system settings.
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // User denied — silent. Notifikasi tidak akan tampil, tapi polling tetap jalan.
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Phase 2 Community: poll for new posts from followed users every 60s ──
    // Setelah dapat post baru → fire local notification via NotificationHelper.
    // lastCheckTime di-init ke now() agar tidak spam notif untuk post lama saat app baru dibuka.
    LaunchedEffect(Unit) {
        var lastCheckTime = System.currentTimeMillis()
        while (true) {
            delay(60_000L)  // check every 60s
            runCatching {
                withContext(Dispatchers.IO) {
                    if (api.loggedIn()) {
                        val followsArr = api.fetchFollowingIds()
                        if (followsArr.length() > 0) {
                            val follows = mutableListOf<String>()
                            for (i in 0 until followsArr.length()) {
                                follows.add(followsArr.getJSONObject(i).optString("following_id", ""))
                            }
                            val newPosts = api.fetchNewPostsFromFollowing(follows, lastCheckTime)
                            if (newPosts.length() > 0) {
                                for (i in 0 until newPosts.length()) {
                                    val post = newPosts.getJSONObject(i)
                                    val postId = post.optString("id", "")
                                    val title = post.optString("title", "")
                                    val authorId = post.optString("author_id", "")
                                    val author = runCatching { api.getProfileById(authorId) }
                                        .getOrNull() ?: JSONObject()
                                    val authorName = author.optString("display_name",
                                        author.optString("username", "User"))
                                    NotificationHelper.showNotification(
                                        context = context,
                                        title  = "Post baru dari $authorName",
                                        body   = title.ifBlank { "Cek post terbaru di komunitas." },
                                        postId = postId
                                    )
                                }
                                // Update lastCheckTime ke now() supaya next poll hanya cari post setelah ini.
                                lastCheckTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Auto-dismiss the banner after 5 seconds ──
    LaunchedEffect(activeBanner?.id) {
        val banner = activeBanner ?: return@LaunchedEffect
        delay(5_000L)
        if (activeBanner?.id == banner.id) activeBanner = null
    }

    // ── FCM Token sync (Task: push notifications) ──────────────────────────────
    // On app open: get FCM token, upload to user_fcm_tokens table (if logged in).
    // Token refresh is handled by DLavieFirebaseMessagingService.onNewToken().
    //
    // BUG FIX v5.4.2: Previously, this only ran once on app open BEFORE user logged in.
    // Now we aggressively poll every 5 seconds and re-upload on every login transition.
    LaunchedEffect(Unit) {
        var hasUploadedToken = false
        var lastLoggedIn = false
        var attemptCount = 0
        while (true) {
            attemptCount++
            val loggedInNow = api.loggedIn()
            android.util.Log.d("DLavieFCM", "Poll #$attemptCount: loggedIn=$loggedInNow hasUploaded=$hasUploadedToken lastLoggedIn=$lastLoggedIn")
            // Detect login transition (false → true): upload token
            if (loggedInNow && !lastLoggedIn) {
                hasUploadedToken = false  // reset so we upload again
            }
            lastLoggedIn = loggedInNow

            if (loggedInNow && !hasUploadedToken) {
                withContext(Dispatchers.IO) {
                    try {
                        android.util.Log.d("DLavieFCM", "Fetching FCM token...")
                        val tokenResult = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                        android.util.Log.d("DLavieFCM", "FCM Token fetched: ${tokenResult.take(20)}...${tokenResult.takeLast(10)} (len=${tokenResult.length})")
                        // Persist locally
                        context.getSharedPreferences("dlavie_fcm", Context.MODE_PRIVATE)
                            .edit().putString("fcm_token", tokenResult).apply()
                        // Upload to Supabase
                        if (api.loggedIn()) {
                            android.util.Log.d("DLavieFCM", "Uploading token to Supabase as user ${api.userId()}...")
                            uploadFcmTokenToSupabase(api, tokenResult)
                            // Also upload android_version to profiles table (for Dev Dashboard Users tab)
                            uploadAndroidVersion(api)
                            hasUploadedToken = true
                            android.util.Log.d("DLavieFCM", "Upload complete!")
                        } else {
                            android.util.Log.w("DLavieFCM", "User logged out before upload could happen")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DLavieFCM", "FCM token fetch/upload failed", e)
                    }
                }
            }
            delay(5_000L)  // poll every 5 seconds (more aggressive)
        }
    }

    // ── Play-time tracking (Task 2): on resume, if a game session is in flight,
    // compute duration and persist via api.recordGameSession + checkAndAwardBadges.
    // The session is considered terminated when our activity returns to foreground
    // (ON_RESUME), which means the user has navigated away from FIFA 16 back to us.
    // ──────────────────────────────────────────────────────────────────────────
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val startedAt = GameSessionTracker.consume()
                if (startedAt > 0L) {
                    val durationMin = ((System.currentTimeMillis() - startedAt) / 60_000L).toInt()
                    if (durationMin > 0) {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    api.recordGameSession(startedAt, durationMin)
                                    api.checkAndAwardBadges()
                                }.onFailure { t ->
                                    // Task 2 fix: log warning supaya bisa debug dari logcat.
                                    // Filter: `adb logcat -s DLavieApi DLavieTelemetry`.
                                    android.util.Log.w(
                                        "DLavieApi",
                                        "recordGameSession failed in ON_RESUME: ${t.javaClass.simpleName}: ${t.message}"
                                    )
                                }
                            }
                        }
                    } else {
                        // Session terlalu pendek (< 1 menit) — skip, log untuk debugging.
                        android.util.Log.i(
                            "DLavieApi",
                            "ON_RESUME: session too short ($durationMin min) — skip recordGameSession"
                        )
                    }
                }
                // else: no active game session (user hanya switch tab) — no-op, expected.
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        try { awaitCancellation() } finally { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        // ── Phase 4: Settings overlay ──
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(tween(300)) + androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(200)) + androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            )
        ) {
            SettingsScreen(
                api = api,
                onBack = { showSettings = false },
                onLogout = {
                    showSettings = false
                    onLogout()
                }
            )
        }

        // ── Phase 4: SharedTransitionLayout wraps the Beranda ↔ GameDetail nav ──
        // The SharedTransitionScope is exposed via LocalSharedTransitionScope so
        // deep composables (TTGameCard cover, GameDetailScreen cover) can attach
        // sharedElement modifiers without threading scopes through every signature.
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                // ── Outer AnimatedContent: page list ↔ GameDetailScreen ──
                // Its AnimatedVisibilityScope (exposed via LocalNavAnimatedVisibilityScope)
                // is the scope the shared element morphs within.
                AnimatedContent(
                    targetState    = showGameDetail,
                    label          = "detail_anim",
                    // Task 4 UI polish: fade + slight scale (0.98 → 1.0), 300ms (was 380ms fade-only).
                    transitionSpec = {
                        (fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                         scaleIn(initialScale = 0.98f, animationSpec = tween(300, easing = FastOutSlowInEasing))) togetherWith
                        (fadeOut(tween(220, easing = FastOutSlowInEasing)) +
                         scaleOut(targetScale = 0.98f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                    }
                ) { showDetail ->
                    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                        if (showDetail) {
                            // ── Phase 2: GameDetailScreen overlay (replaces page content) ──
                            GameDetailScreen(
                                onBack = { showGameDetail = false },
                                onPlay = {
                                    launchGame(context)
                                    showGameDetail = false
                                },
                                onDownload = {
                                    showGameDetail = false
                                    // Trigger download via lifted state (dlProgress/dlError/startDownload)
                                    // — user kembali ke Beranda dan download otomatis dimulai.
                                    startDownload()
                                },
                                gameInstalled      = detailGameInstalled,
                                avgRating          = detailAvgRating,
                                ratingCount        = detailRatingCount,
                                maintenanceBlocked = detailMaintenanceBlocked,
                                // v6.8.1: pass myRating supaya Rate button tahu state-nya.
                                hasRated           = detailMyRating > 0,
                                myRating           = detailMyRating,
                                onRate             = {
                                    // v6.8.4: Guest restriction — rate requires login (dialog, not popup)
                                    if (api.isGuest()) {
                                        ratingSubmitError = "Login diperlukan untuk rate game. Guest mode hanya untuk browse."
                                        showRatingPopup = true
                                    } else if (!api.loggedIn()) {
                                        // Cek login dulu — rating wajib login (Supabase RLS).
                                        ratingSubmitError = "Login dulu untuk rate game ini."
                                        showRatingPopup = true
                                    } else if (detailMyRating > 0) {
                                        // Sudah rate — tidak bisa rate lagi. Tombol sudah jadi checkmark
                                        // (visual cue), jadi seharusnya tidak sampai sini. Tapi kalau
                                        // somehow terpanggil, silent ignore.
                                    } else {
                                        showRatingPopup = true
                                    }
                                }
                            )
                        } else {
                            // ── P3A: HorizontalPager replaces AnimatedContent for tab nav ──
                            // v7.0.2 FIX: beyondViewportPageCount = 1 (pre-render adjacent pages)
                            //   Sebelumnya = 0 → setiap tab switch dispose page lama + re-mount page baru
                            //   → LaunchedEffect(Unit) re-fire → re-fetch data dari Supabase
                            //   → blank frame + frame drop (glitch saat transisi cepat).
                            //   Dengan = 1, page tetangga sudah pre-rendered → swipe smooth.
                            //   Trade-off: memory sedikit naik (3 pages alive), tapi UX jauh lebih baik.
                            HorizontalPager(
                                state = pagePagerState,
                                modifier = Modifier.fillMaxSize().padding(bottom = 84.dp),
                                pageSpacing = 0.dp,
                                beyondViewportPageCount = 1
                            ) { pageIndex ->
                                val target = Page.values()[pageIndex]
                                // v7.0.2 FIX: key() supaya Compose track page identity stabil.
                                //   Tanpa key, Compose bisa salah-identifikasi item saat swipe cepat
                                //   → state tertukar antar page → visual glitch.
                                key(target) {
                                // v7.0.5 FIX: solid background per page supaya page tetangga
                                //   yang pre-rendered (beyondViewportPageCount=1) TIDAK tembus
                                //   ke page aktif. Sebelumnya background transparan → konten
                                //   page sebelah terlihat menembus → glitch overlap.
                                Box(Modifier.fillMaxSize().background(PureBlack)) {
                                // ── Partial maintenance: NO blur, just block action buttons ──
                                when (target) {
                                    Page.Home   -> HomeScreen(
                                            api             = api,
                                            maintenanceInfo = maintenanceInfo,
                                            onNav           = { page = it },
                                            dlProgress      = dlProgress,
                                            dlError         = dlError,
                                            startDownload   = { startDownload() },
                                            onOpenSettings  = { showSettings = true },
                                            onGameCardClick = { inst, avg, count, blocked, myR ->
                                                detailGameInstalled      = inst
                                                detailAvgRating          = avg
                                                detailRatingCount        = count
                                                detailMaintenanceBlocked = blocked
                                                detailMyRating           = myR
                                                showGameDetail           = true
                                            }
                                        )
                                    Page.DLC -> DlcScreen(api, maintenanceInfo = maintenanceInfo, onNav  = { page = it })
                                    Page.GameHub -> GameHubScreen(
                                            onNav = { page = it },
                                            onGameClick = { gameTitle ->
                                                // Navigate to game detail
                                                detailGameInstalled      = false
                                                detailAvgRating          = 0.0
                                                detailRatingCount        = 0
                                                detailMaintenanceBlocked = false
                                                detailMyRating           = 0
                                                showGameDetail           = true
                                            }
                                        )
                                    Page.Chat   -> CommunityScreen(
                                        api             = api,
                                        pendingPostId   = pendingPostId.value,
                                        onConsumePostId = { pendingPostId.value = null },
                                        onVisitProfile  = { uid -> visitingUserId = uid }
                                    )
                                    Page.Me     -> ProfileScreen(
                                        api                     = api,
                                        onLogout                = onLogout,
                                        onOpenSettings          = { showSettings = true },
                                        expandedSection         = profileExpandedSection,
                                        onExpandedSectionChange = { profileExpandedSection = it },
                                        onVisitProfile          = { uid -> visitingUserId = uid }
                                    )
                                }
                                } // end Box (solid bg)
                                } // end key(target)
                            }
                        }
                    }
                }
            }
        }

        // ── Phase 4: Settings overlay (full-screen, on top of everything) ──
        if (showSettings) {
            SettingsScreen(
                api           = api,
                onBack        = { showSettings = false },
                onLogout      = {
                    showSettings = false
                    onLogout()
                }
            )
        }

        // ── Visit Profile overlay (Task 4): shown when user taps a username in
        // CommunityScreen search results or in a feed post card. Displays the
        // selected user's profile (avatar, name, unique ID, stats, badges, posts)
        // + Follow/Unfollow button (Task 5). On back, returns to previous page.
        // ──────────────────────────────────────────────────────────────────────────
        visitingUserId?.let { uid ->
            UserProfileScreen(
                userId         = uid,
                api            = api,
                onBack         = { visitingUserId = null },
                onVisitProfile = { otherUid -> visitingUserId = otherUid }
            )
        }

        // ── v6.8 redesign: Bottom navigation (fixed bar, 5 icons) replaces FloatingNav pill ──
        // Tetap accessible dari Home (tidak tampil saat GameDetail/Settings/Visit aktif).
        if (!showGameDetail && !showSettings && visitingUserId == null) {
            DLavieBottomNav(
                page     = page,
                onPage   = { page = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // ── Floating ChatBot (v6.4) — always visible when logged in ──
        if (!showGameDetail && !showSettings && visitingUserId == null && api.loggedIn()) {
            FloatingChatBot(api = api)
        }

        // ── v6.8.1: Rating popup (lifted to MainShell — dipakai dari GameDetailScreen) ──
        // Setelah submit: refresh avg/count + myRating (Supabase upsert merge-duplicates).
        if (showRatingPopup) {
            RatingPopup(
                currentRating = detailMyRating,
                submitError = ratingSubmitError,
                onDismiss = {
                    showRatingPopup = false
                    ratingSubmitError = ""
                },
                onSubmit = { rating, review ->
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                api.submitRating(rating, review)
                                // Refresh stats + my rating after submit
                                val stats = api.fetchRatingStats()
                                detailAvgRating   = stats.optDouble("avg", 0.0)
                                detailRatingCount = stats.optInt("count", 0)
                                detailMyRating    = rating
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

        // ── Notification banner overlay (slides down from the top) ──
        // Sembunyikan saat GameDetail / Settings / Visit aktif supaya tidak menumpuk header.
        if (!showGameDetail && !showSettings && visitingUserId == null) {
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

// ─── Floating navigation bar (TapTap-style pill v3) ───────────────────────────
// Cleaner pill shape (rounded 999), active tab filled bg dengan scale animation,
// inactive hanya icon dengan muted color. Smooth transition antar tabs via
// animateColorAsState + animateFloatAsState + AnimatedVisibility label.
@Composable
fun FloatingNav(page: Page, onPage: (Page) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val pages = Page.values().toList()
    val centerPage = Page.GameHub

    Box(modifier = modifier.widthIn(max = 600.dp).padding(horizontal = 16.dp)) {
        // Main pill bar
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Home + DLC
                pages.filter { it != centerPage }.forEach { item ->
                    val selected = page == item
                    val iconTint by animateColorAsState(
                        if (selected) Color.Black else Color.Gray,
                        tween(300), label = "nav_tint_${item.label}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPage(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                item.navIcon,
                                contentDescription = item.label,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                item.label,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = iconTint,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Center: floating black button (PS5 stick)
                Spacer(Modifier.width(52.dp)) // space for floating button
            }
        }

        // Floating center button (overlaps the bar)
        Box(
            Modifier
                .align(Alignment.Center)
                .size(56.dp)
                .offset(y = (-4).dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(3.dp, Color.White, CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPage(centerPage)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.SportsEsports,
                contentDescription = "GameHub",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ─── Home screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    api: CommunityApi,
    maintenanceInfo: MaintenanceInfo? = null,
    onNav: (Page) -> Unit,
    // ── Phase 2: lifted download state (shared dengan MainShell & GameDetailScreen) ──
    dlProgress: Float = -1f,
    dlError: String = "",
    startDownload: () -> Unit = {},
    // ── v6.8 redesign: hamburger menu opens Settings ──
    onOpenSettings: () -> Unit = {},
    // ── Phase 2: tap TTGameCard → navigate ke GameDetailScreen ──
    // v6.8.1: tambah myR param supaya MainShell bisa pass rating state ke GameDetailScreen
    onGameCardClick: (gameInstalled: Boolean, avgRating: Double, ratingCount: Int, maintenanceBlocked: Boolean, myRating: Int) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // ── Onboarding slider state (v6.0 — show on first open) ──
    var showOnboarding by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("dlavie_onboarding", android.content.Context.MODE_PRIVATE)
        val hasSeen = prefs.getBoolean("has_seen_onboarding_v6", false)
        if (!hasSeen) {
            showOnboarding = true
        }
    }

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
    // v6.8.1: showRatingPopup + ratingSubmitError dipindah ke MainShell (dipakai
    // dari GameDetailScreen, bukan dari HomeScreen lagi).
    var avgRating      by remember { mutableStateOf(0.0) }   // 1-5 scale
    var ratingCount    by remember { mutableStateOf(0) }
    var myRating       by remember { mutableStateOf(0) }     // 1-5, or 0 if not rated

    // ── NEW: Notification category popup state ──
    var showNotifPopup  by remember { mutableStateOf(false) }
    // notifCategory tracks the user's current filter (default "all"). Tidak dipakai
    // untuk navigasi ke screen lain — hanya popup filter lokal di Home.
    var notifCategory   by remember { mutableStateOf("all") }
    // Filtered notification list (fetched saat kategori dipilih). Bisa kosong.
    var notifList       by remember { mutableStateOf<List<NotifCampaign>>(emptyList()) }
    var notifListOpen   by remember { mutableStateOf(false) }
    // v6.8.4: Guest upgrade dialog state — ditampilkan saat guest coba download/rate
    var showGuestDialog by remember { mutableStateOf(false) }
    var guestDialogFeature by remember { mutableStateOf("") }

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
            // Cek data ready dari MULTIPLE sources (fix: user yang baru install
            // game + download OBB tidak punya marker file dari DevPatchEngine).
            // 1. Marker file (kalau patch sudah diapply)
            // 2. OBB main / patch file
            // 3. Game data folder exists & punya konten
            // 4. files/ subfolder punya konten
            // → dataReady = markerReady || obbReady || filesReady
            dataReady     = isDataReady()
            // Phase 4: always check updates (settings toggle can be added later)
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
                // Phase 4: medium haptic when pull-to-refresh triggers.
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
            // P2D: custom halftone dot spinner replaces default Material indicator.
            HalftonePullIndicator(state = pullState, isRefreshing = isRefreshing)
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

        // ── v6.8 Top Bar: hamburger + search + bell + profile ───────────────────
        // Hamburger → Settings overlay. Search → Komunitas (search aktif di sana).
        // Bell → notification category popup (existing logic). Profile → Me tab.
        Box {
            DLavieTopBar(
                onMenuClick    = { onOpenSettings() },
                onSearchClick  = { onNav(Page.Chat) },
                onBellClick    = { showNotifPopup = true },
                onProfileClick = { onNav(Page.Me) },
                hasUnreadNotif = latestNotif != null,
                profileInitial = "DL"
            )
            // Hidden dropdown — still driven by showNotifPopup state (unchanged logic)
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

        // ── v6.8 Hero Carousel (full-width image + dots + 5-star rating) ────────
        // Replaces single decorative hero banner. Swipeable, auto-scroll 5s,
        // dots indicator below, 5-star rating overlay on each slide.
        // v6.8.1: Banner slides — NO rating (banner is promotional, not a game card).
        // Slide 1: FIFA 16 Mobile cover image (dlavie_game_logo).
        // Slide 2: Komunitas promo — large Forum icon + halftone + gradient.
        // Slide 3: Update promo — large CloudSync icon + halftone + gradient.
        val heroSlides = listOf(
            HeroSlide(
                title    = "FIFA 16 Mobile",
                subtitle = "DLavie 26 Mod — Play offline, Always update, More improvement",
                imageRes = R.drawable.dlavie_game_logo,
                tag      = "OFFICIAL"
            ),
            HeroSlide(
                title         = "Komunitas DLavie",
                subtitle      = "Berbagi patch, tips, dan diskusi dengan ribuan pemain",
                imageRes      = null,
                tag           = "KOMUNITAS",
                promoIcon     = Icons.Rounded.Forum,
                promoGradient = listOf(PureBlack, Surface2, Carbon)
            ),
            HeroSlide(
                title         = "Update v6.7.0",
                subtitle      = "Patch terbaru — perbaikan bug & peningkatan performa",
                imageRes      = null,
                tag           = "UPDATE",
                promoIcon     = Icons.Rounded.CloudSync,
                promoGradient = listOf(Surface3, Carbon, PureBlack)
            )
        )
        DLavieHeroCarousel(
            slides = heroSlides,
            onSlideClick = { slide ->
                when (slide.tag) {
                    "OFFICIAL" -> onGameCardClick(gameInstalled, avgRating, ratingCount, maintenanceBlocked, myRating)
                    "KOMUNITAS" -> onNav(Page.Chat)
                    "UPDATE" -> onNav(Page.DLC)
                }
            }
        )

        // ── Beranda CLEAN (v5.0): Hanya game library DLavie 26 FIFA 16 ──────────
        // Banner carousel, Berita/Feed, Trusted by DLavie, Status bar (3 chips)
        // dipindahkan ke tab Community/Update. Beranda fokus ONLY ke game card.

        Spacer(Modifier.height(DLSpacing.md))

        // ── Game Card: "DLavie 26: Football Game" (TapTap-style TTGameCard) ──
        // Phase 2 fix: saat setupState==LOADING, tampilkan TTGameCardSkeleton
        // (bukan card dengan data kosong) untuk loading state yang konsisten.
        // Tombol adaptif: Dapatkan / Mainkan / Diblokir Maintenance.
        // Inline download progress + error ditampilkan di bawah card (TTGameCard
        // tidak punya slot untuk itu, jadi kita wrap dalam Column).
        val rating10 = String.format("%.1f", avgRating * 2.0)
        // v5.0 aurora cover gradient (deep space + cyan glow)
        val coverGradient = listOf(
            DLavieGlass.SpaceBlack,
            DLavieGlass.SpaceCharcoal,
            DLavieGlass.SpaceSurface
        )
        val ttButtonLabel: String = when {
            maintenanceBlocked -> "Diblokir"
            gameInstalled      -> "Mainkan"
            else               -> "Dapatkan"
        }
        val ttButtonEnabled: Boolean = !maintenanceBlocked
        val ttButtonClick: () -> Unit = when {
            maintenanceBlocked -> ({ })
            gameInstalled      -> ({ launchGame(context) })
            else               -> ({ if (dlProgress < 0f) startDownload() })
        }
        // Phase 2: tap card body → navigate ke GameDetailScreen (pass state ke MainShell)
        // v6.8.1: tambah myRating supaya Rate button di detail screen tahu state-nya.
        val ttCardClick: () -> Unit = {
            onGameCardClick(gameInstalled, avgRating, ratingCount, maintenanceBlocked, myRating)
        }

        if (setupState == SetupState.LOADING) {
            // Loading state: skeleton (bukan card dengan data kosong)
            TTGameCardSkeleton()
        } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TTGameCard(
                title          = "DLavie 26: Football Game",
                subtitle       = "Olahraga · FIFA 16 Mod",
                rating         = "$rating10 · $ratingCount ratings",
                coverGradient  = coverGradient,
                coverText      = "DL",
                coverImageRes  = R.drawable.dlavie_game_logo,
                buttonLabel    = ttButtonLabel,
                buttonEnabled  = ttButtonEnabled,
                onButtonClick  = ttButtonClick,
                onClick        = ttCardClick,
                // Phase 4: long-press haptic + shared element key (morphs to detail cover).
                onLongClick    = { /* haptic-only acknowledgment */ },
                sharedContentKey = "game-cover"
            )
            // Inline download progress (kalau sedang unduh dari tombol "Dapatkan")
            AnimatedVisibility(
                visible = dlProgress >= 0f && dlProgress < 2f,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { dlProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = DLavieGlass.AuroraCyan,
                        trackColor = DLavieGlass.AuroraCyan.copy(0.15f)
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
                    Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(13.dp))
                    Text(dlError, color = DangerRed, fontSize = 11.sp)
                }
            }
        }
        } // end if (setupState == LOADING) else { ... }

        // ── Main action card (state-aware) ────────────────────────────────────
        // This is the install/play CTA — kept because it handles NEED_GAME / NEED_DATA / READY states.
        AnimatedContent(
            targetState    = setupState,
            label          = "setup_state",
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }
        ) { state ->
            when (state) {

                // Loading — TapTap-style shimmer skeletons + Lottie loading (Phase 3)
                SetupState.LOADING -> Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // v3.0 Phase 3: Lottie loading animation (monochrome white ring)
                    LottieLoading(size = 56.dp)
                    TTGameCardSkeleton()
                    TTGameCardSkeleton()
                    TTGameCardSkeleton()
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
                        onClick  = {
                            // v6.8.4: Guest restriction — download APK requires login (dialog, not toast)
                            if (api.isGuest()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                guestDialogFeature = "download APK FIFA 16"
                                showGuestDialog = true
                                return@Button
                            }
                            if (dlProgress < 0f || dlProgress >= 2f) {
                                // Phase 4: light haptic on download trigger.
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                startDownload()
                            }
                        },
                        enabled  = !maintenanceBlocked && (dlProgress < 0f || dlProgress >= 2f),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = ButtonDefaults.buttonColors(
                            // v3.0 monochrome — default: white bg + black text (inverted premium)
                            containerColor = when {
                                maintenanceBlocked -> Surface2
                                dlProgress >= 2f -> NeonGreen
                                dlProgress >= 0f -> Color.White.copy(0.5f)   // dimmed while downloading
                                else             -> Color.White              // premium inverted
                            },
                            contentColor   = if (maintenanceBlocked) SoftText else if (dlProgress >= 2f) Color(0xFF00150B) else Carbon,
                            disabledContainerColor = if (maintenanceBlocked) Surface2 else Color.White.copy(0.3f),
                            disabledContentColor   = if (maintenanceBlocked) SoftText else Carbon.copy(0.6f)
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
                                onClick  = { onNav(Page.DLC) },
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

        // ── Beranda CLEAN: Status bar (3 chips) & Berita/Feed REMOVED ──────────
        // These cluttered the home screen. Status info is now in the Update tab,
        // and Feed/Berita is in the Community tab. Beranda stays minimal.

        // Bottom spacer
        Spacer(Modifier.height(8.dp))
    }
    } // end PullToRefreshBox

    // ── Onboarding slider (v6.0 — shows on first open, dismissible) ──────────
    if (showOnboarding) {
        OnboardingSlider(
            onDismiss = {
                showOnboarding = false
                context.getSharedPreferences("dlavie_onboarding", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("has_seen_onboarding_v6", true)
                    .apply()
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

    // v6.8.4: Guest upgrade dialog — ditampilkan saat guest coba download/rate
    if (showGuestDialog) {
        GuestUpgradeDialog(
            feature = guestDialogFeature,
            onLogin = {
                showGuestDialog = false
                // Clear guest flag + go to login screen
                api.clearGuest()
                api.logout()
                context.startActivity(
                    Intent(context, DLavieGuidedActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onDismiss = { showGuestDialog = false }
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
    val haptic = LocalHapticFeedback.current

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
                                    // Phase 4: medium haptic on star selection.
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

// ─── Feed row (Phase 2: TapTap-style, wrapped dalam TTTappableCard) ────────────
// Padding horizontal disesuaikan supaya nyaman di dalam TTTappableCard (yang
// tidak punya internal padding sendiri).
@Composable
fun FeedRow(item: FeedItem) {
    val icon     = feedIcon(item.type)
    val iconTint = feedColor(item.type)
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = TTSpacing.lg, vertical = TTSpacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(TTSpacing.md)
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
    // dataReady = multi-source detection: marker file (DevPatchEngine) ATAU
    // OBB file exists ATAU files/ folder punya konten. Fix bug "Belum siap"
    // untuk user yang baru install game + download OBB dari dalam game.
    var dataReady       by remember { mutableStateOf(false) }
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
    // Storage permission reminder saat apply patch (Task 2)
    var showStoragePermissionDialog by remember { mutableStateOf(false) }

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
                // Re-evaluate dataReady dari multiple sources (marker / OBB / files dir).
                runCatching { dataReady = isDataReady() }
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
        // Task 2: Cek storage permission (MANAGE_EXTERNAL_STORAGE) dulu sebelum apply patch.
        // Kalau belum granted → tampilkan dialog remind user untuk allow storage access,
        // jangan lanjut apply patch (akan gagal karena tidak bisa write ke folder game).
        if (!StorageAccess.isGranted()) {
            showStoragePermissionDialog = true
            return
        }
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
            // Multi-source data ready detection (marker / OBB / files dir).
            runCatching { dataReady = isDataReady() }
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
                    "Storage menipis. Patch OBB/data besar (>1 GB) mungkin gagal. Hapus file tidak terpakai atau backup lama.",
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

        // ── Patch / update card (atau shimmer skeleton saat loading) ──
        val ui             = updateInfo
        val patchAvailable = ui != null && !ui.upToDate

        if (loading) {
            // TapTap-style shimmer skeletons saat fetchUpdateInfo sedang berlangsung
            TTGameCardSkeleton()
            TTGameCardSkeleton()
        } else {
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
                                patchDone      -> "Sudah Diperbarui"
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
        } // end if (loading) else { ... }

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

    // ── Storage permission reminder dialog (Task 2) ──
    // Saat user tap "Terapkan Pembaruan" tanpa MANAGE_EXTERNAL_STORAGE,
    // tampilkan dialog → "Izinkan" (buka Settings) atau "Nanti" (dismiss).
    if (showStoragePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showStoragePermissionDialog = false },
            title = { Text("Izinkan Akses Penyimpanan", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "Untuk menerapkan patch mod, launcher membutuhkan izin akses penyimpanan " +
                    "(MANAGE_EXTERNAL_STORAGE). Tanpa izin ini, patch tidak bisa ditulis ke folder " +
                    "data FIFA 16 (Android/data/com.ea.gp.fifaworld).\n\n" +
                    "Tap \"Izinkan\" untuk membuka pengaturan, lalu aktifkan toggle \"Izinkan akses ke semua file\".",
                    color = SoftText, fontSize = 13.sp, lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStoragePermissionDialog = false
                        StorageAccess.request(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CandyCyan, contentColor = Color(0xFF00111D)
                    )
                ) {
                    Icon(Icons.Rounded.Security, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Izinkan", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStoragePermissionDialog = false }) {
                    Text("Nanti", color = SubText)
                }
            },
            containerColor = GlassBase
        )
    }
}

// ─── Community screen — TapTap-style feed (Following / For You) ────────────────
// Top bar: user role badge + tabs (Following | For You) + filter icon.
// Feed: post cards with optional image, title, body, author avatar + username,
//       relative timestamp, like button (animated bounce + haptic), three-dot
//       menu (Report / Share / Save).
// FAB: circular white "+" → create post bottom sheet (title + body + image URL + type).
// Loading: shimmer skeletons. Pull-to-refresh. Empty states per tab.
//
// Data sources (Supabase):
//   - feed_posts          → global + following feed
//   - community_follows   → "Following" tab author set
//   - feed_likes          → like count + has-liked
//   - saved_posts         → bookmark toggle
//   - reports             → report-a-post
//   - profiles            → author avatar / username / role
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    api: CommunityApi,
    pendingPostId: String? = null,
    onConsumePostId: () -> Unit = {},
    onVisitProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val haptic  = LocalHapticFeedback.current

    // 0 = Following, 1 = For You (default to For You so feed is populated for new users)
    var selectedTab by remember { mutableStateOf(1) }

    // Filter state
    var sortBy           by remember { mutableStateOf("newest") }        // newest | oldest
    var roleFilter       by remember { mutableStateOf<String?>(null) }   // admin | developer | user | null
    var dateFilterMillis by remember { mutableStateOf<Long?>(null) }     // epoch millis (UTC midnight) | null
    var showFilterMenu   by remember { mutableStateOf(false) }
    var showDatePicker   by remember { mutableStateOf(false) }

    // ── Task 3: User search state ──
    // searchQuery: text typed into the search bar (>= 2 chars triggers api.searchUsers)
    // searchResults: list of profiles matching the query (max 10)
    var searchQuery   by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    var searching     by remember { mutableStateOf(false) }

    // Feed state
    var posts     by remember { mutableStateOf<List<FeedPostData>>(emptyList()) }
    var loading   by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf("") }

    // Author cache (author_id -> AuthorInfo)
    var authorCache by remember { mutableStateOf<Map<String, AuthorInfo>>(emptyMap()) }
    // Like state (post_id -> Pair(liked, count))
    var likeState   by remember { mutableStateOf<Map<String, Pair<Boolean, Int>>>(emptyMap()) }
    // Saved state (post_id -> saved)
    var savedState  by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    // ── Phase 2: Comment count state (post_id -> count) ──
    var commentCountState by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Create post sheet + report dialog + comments sheet
    var showCreateSheet by remember { mutableStateOf(false) }
    var reportTarget    by remember { mutableStateOf<FeedPostData?>(null) }
    // ── Phase 2: post yang comment-sheet-nya sedang dibuka (null = tutup) ──
    var commentsTarget  by remember { mutableStateOf<FeedPostData?>(null) }

    val role     = remember { api.role().ifBlank { "user" } }
    val roleBadge = role.uppercase()

    fun toast(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun loadPosts() {
        scope.launch {
            if (posts.isEmpty()) loading = true
            errorMsg = ""
            try {
                val arr = withContext(Dispatchers.IO) {
                    if (selectedTab == 0) {
                        if (!api.loggedIn()) JSONArray()
                        else api.fetchFeedPostsFollowing(sortBy, 30)
                    } else {
                        api.fetchFeedPostsGlobal(sortBy, 30)
                    }
                }
                val raw = (0 until arr.length()).mapNotNull { i ->
                    runCatching {
                        val o = arr.getJSONObject(i)
                        FeedPostData(
                            id        = o.optString("id"),
                            authorId  = o.optString("author_id"),
                            title     = o.optString("title"),
                            body      = o.optString("body"),
                            // Normalize image_url: treat null/blank/"null" string as empty
                            // supaya FeedPostCard tidak render kotak gambar kosong.
                            // (org.json.optString bisa return "null" string kalau value == JSONObject.NULL)
                            imageUrl  = o.optString("image_url", "").let { raw ->
                                val s = raw.trim()
                                if (s.isBlank() || s.equals("null", ignoreCase = true)) "" else s
                            },
                            type      = o.optString("type", "community"),
                            pinned    = o.optBoolean("pinned"),
                            official  = o.optBoolean("official"),
                            createdAt = o.optString("created_at", "")
                        )
                    }.getOrNull()
                }

                // ── Fetch author profiles (only missing ones) ──
                val missing = raw.map { it.authorId }.distinct()
                    .filter { it.isNotBlank() && !authorCache.containsKey(it) }
                if (missing.isNotEmpty()) {
                    val fetched = withContext(Dispatchers.IO) {
                        missing.associateWith { id ->
                            try {
                                val p = api.getProfileById(id)
                                AuthorInfo(
                                    id          = id,
                                    username    = p.optString("username", ""),
                                    displayName = p.optString("display_name", ""),
                                    avatarUrl   = p.optString("avatar_url", ""),
                                    role        = p.optString("role", "user")
                                )
                            } catch (_: Throwable) {
                                AuthorInfo(id, "", "Anonim", "", "user")
                            }
                        }
                    }
                    authorCache = authorCache + fetched
                }

                // ── Fetch like + saved state per post (concurrent-safe sequential) ──
                if (api.loggedIn()) {
                    val newLike = withContext(Dispatchers.IO) {
                        raw.associate { p ->
                            try {
                                val count = api.getPostLikeCount(p.id)
                                val liked = api.hasLikedPost(p.id)
                                p.id to (liked to count)
                            } catch (_: Throwable) { p.id to (false to 0) }
                        }
                    }
                    val newSaved = withContext(Dispatchers.IO) {
                        raw.associate { p ->
                            try { p.id to api.hasSavedPost(p.id) }
                            catch (_: Throwable) { p.id to false }
                        }
                    }
                    likeState  = likeState + newLike
                    savedState = savedState + newSaved
                } else {
                    // Not logged in: only public like counts, no liked/saved flags.
                    val newLike = withContext(Dispatchers.IO) {
                        raw.associate { p ->
                            try { p.id to (false to api.getPostLikeCount(p.id)) }
                            catch (_: Throwable) { p.id to (false to 0) }
                        }
                    }
                    likeState = likeState + newLike
                }

                // ── Phase 2: Fetch comment count per post (public read, fire-and-forget per post) ──
                val newCommentCounts = withContext(Dispatchers.IO) {
                    raw.associate { p ->
                        try { p.id to api.getCommentCount(p.id) }
                        catch (_: Throwable) { p.id to 0 }
                    }
                }
                commentCountState = commentCountState + newCommentCounts

                // ── Apply role + date filters (client-side) ──
                var filtered = raw
                if (roleFilter != null) {
                    filtered = filtered.filter { post ->
                        (authorCache[post.authorId]?.role?.lowercase() ?: "user") == roleFilter
                    }
                }
                if (dateFilterMillis != null) {
                    val startMs = dateFilterMillis!!
                    val endMs   = startMs + 24L * 60L * 60L * 1000L
                    filtered = filtered.filter { post ->
                        val ts = parseIsoToMillis(post.createdAt)
                        ts in startMs until endMs
                    }
                }
                // Pinned first, then by createdAt desc
                filtered = filtered.sortedWith(
                    compareByDescending<FeedPostData> { it.pinned }
                        .thenByDescending { it.createdAt }
                )
                posts = filtered
            } catch (t: Throwable) {
                errorMsg = t.message ?: "Gagal memuat feed"
                posts = emptyList()
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    // Reload when tab or sort changes (role/date filters apply client-side → also reload)
    LaunchedEffect(selectedTab, sortBy, roleFilter, dateFilterMillis) { loadPosts() }

    // ── P3C: Debounced real-time search ──
    // LaunchedEffect(searchQuery) auto-cancels the previous run when the query
    // changes → the delay(300) effectively debounces. Only fires api.searchUsers
    // when the trimmed query is >= 2 chars; clears results otherwise.
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) {
            searching = false
            searchResults = emptyList()
            return@LaunchedEffect
        }
        delay(300L)  // debounce window — cancels if user keeps typing
        searching = true
        try {
            val arr = withContext(Dispatchers.IO) { api.searchUsers(q) }
            val list = (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    UserSearchResult(
                        id          = o.optString("id"),
                        username    = o.optString("username"),
                        displayName = o.optString("display_name"),
                        avatarUrl   = o.optString("avatar_url", "").let { s ->
                            val ss = s.trim()
                            if (ss.isBlank() || ss.equals("null", ignoreCase = true)) "" else ss
                        },
                        uniqueId    = o.optInt("unique_id", 0),
                        role        = o.optString("role", "user"),
                        lastSeenAt  = o.optString("last_seen_at", ""),  // v7.0.5: presence
                        updatedAt   = o.optString("updated_at", "")     // v7.0.5: fallback presence
                    )
                }.getOrNull()
            }.filter { it.id.isNotBlank() && it.id != api.userId() }
            searchResults = list
        } catch (_: Throwable) {
            searchResults = emptyList()
        } finally {
            searching = false
        }
    }

    // ── Phase 2: Deep-link dari notification tap ──
    // Saat pendingPostId di-set (dari MainShell), cari post di feed dan auto-open comments sheet.
    // Konsumsi pendingPostId (set null) setelah diproses supaya tidak re-trigger.
    LaunchedEffect(pendingPostId, posts) {
        val pid = pendingPostId ?: return@LaunchedEffect
        if (posts.isEmpty()) return@LaunchedEffect
        val match = posts.firstOrNull { it.id == pid }
        if (match != null) {
            commentsTarget = match
            onConsumePostId()
        }
    }

    // ── Date picker dialog (By Date filter) ──
    if (showDatePicker) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    val ms = dpState.selectedDateMillis
                    if (ms != null) { dateFilterMillis = ms }
                }) { Text("Pilih", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal", color = SoftText) }
            }
        ) {
            DatePicker(state = dpState)
        }
    }

    // ── Create post bottom sheet ──
    if (showCreateSheet) {
        CreatePostSheet(
            api = api,
            onDismiss = { showCreateSheet = false },
            onPosted = {
                showCreateSheet = false
                toast("Post terkirim!")
                loading = true
                loadPosts()
            },
            onError = { msg -> toast(msg) }
        )
    }

    // ── Report dialog ──
    reportTarget?.let { target ->
        ReportPostDialog(
            onDismiss = { reportTarget = null },
            onSubmit = { category, reason ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { api.reportPost(target.id, category, reason) }
                        toast("Laporan terkirim. Terima kasih.")
                    } catch (t: Throwable) {
                        toast("Gagal melaporkan: ${t.message}")
                    }
                }
                reportTarget = null
            }
        )
    }

    // ── Phase 2: Comments bottom sheet ──
    commentsTarget?.let { target ->
        CommentsBottomSheet(
            postId    = target.id,
            api       = api,
            onDismiss = { commentsTarget = null },
            onCommentAdded = {
                // Optimistic +1 ke comment count
                val cur = commentCountState[target.id] ?: 0
                commentCountState = commentCountState + (target.id to (cur + 1))
            }
        )
    }

    Box(Modifier.fillMaxSize().background(Carbon)) {
        // Subtle halftone texture behind the feed
        HalftoneBackground(modifier = Modifier.fillMaxSize(), alpha = 0.3f)

        Column(Modifier.fillMaxSize()) {
            // ── Top Bar: role badge | tabs | filter icon ──
            CommunityTopBar(
                roleBadge = roleBadge,
                roleColor = roleBadgeColor(role),
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                showFilterMenu = showFilterMenu,
                onToggleFilter = { showFilterMenu = !showFilterMenu },
                onFilterSelect = { choice ->
                    showFilterMenu = false
                    when (choice) {
                        "newest"         -> { sortBy = "newest"; roleFilter = null; dateFilterMillis = null }
                        "oldest"         -> { sortBy = "oldest"; roleFilter = null; dateFilterMillis = null }
                        "role_admin"     -> roleFilter = "admin"
                        "role_developer" -> roleFilter = "developer"
                        "role_member"    -> roleFilter = "user"
                        "date"           -> showDatePicker = true
                    }
                }
            )

            // ── Active filter chips row ──
            if (roleFilter != null || dateFilterMillis != null) {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = TTSpacing.lg, vertical = TTSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                ) {
                    if (roleFilter != null) {
                        CommunityFilterChip(
                            label = "Role: ${roleFilter!!.replaceFirstChar { it.uppercase() }}",
                            onClear = { roleFilter = null }
                        )
                    }
                    if (dateFilterMillis != null) {
                        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(dateFilterMillis!!))
                        CommunityFilterChip(label = "Tanggal: $dateStr", onClear = { dateFilterMillis = null })
                    }
                }
            }

            // ── Task 3: Username search bar ──
            // Triggers api.searchUsers() when query length >= 2. Results render as a
            // dropdown overlay (max 10 rows). Tapping a row → onVisitProfile(uid).
            // P3C: Debounced search — the actual api.searchUsers call is made in
            // the LaunchedEffect(searchQuery) below, 300ms after the user stops
            // typing. The onValueChange here just updates the query text.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { raw -> searchQuery = raw },
                placeholder = { Text("Cari username komunitas…", fontSize = 13.sp, color = SubText) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = SubText, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SubText, strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape).clickable {
                                searchQuery = ""
                                searchResults = emptyList()
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus",
                                tint = SubText, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                shape = TTShapes.input,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = TTSpacing.lg, vertical = TTSpacing.xs),
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlassStroke,
                    unfocusedBorderColor = GlassStroke.copy(0.5f),
                    cursorColor = Color.White,
                    focusedLeadingIconColor = Color.White,
                    unfocusedLeadingIconColor = SubText
                )
            )

            // ── Search results dropdown (overlay-ish, in-flow) ──
            if (searchResults.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = TTSpacing.lg, vertical = TTSpacing.xs)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassBase)
                        .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                ) {
                    searchResults.forEachIndexed { idx, user ->
                        if (idx > 0) {
                            Box(
                                Modifier.fillMaxWidth().height(1.dp)
                                    .background(Color.White.copy(0.05f))
                            )
                        }
                        UserSearchRow(
                            user = user,
                            onClick = {
                                val uid = user.id
                                searchQuery = ""
                                searchResults = emptyList()
                                onVisitProfile(uid)
                            }
                        )
                    }
                }
            }

            // ── Feed (pull-to-refresh + lazy list) — takes remaining space below top bar ──
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // P2D: custom halftone pull indicator (same as HomeScreen).
                val communityPullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        if (!refreshing) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            refreshing = true
                            loadPosts()
                        }
                    },
                    state = communityPullState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        HalftonePullIndicator(state = communityPullState, isRefreshing = refreshing)
                    }
                ) {
                    when {
                        loading -> {
                            Column(
                                Modifier.fillMaxSize().padding(TTSpacing.lg).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(TTSpacing.md)
                            ) {
                                repeat(4) { TTGameCardSkeleton() }
                            }
                        }
                        posts.isEmpty() -> {
                            CommunityEmptyState(
                                isFollowing = selectedTab == 0,
                                loggedIn = api.loggedIn(),
                                errorMsg = errorMsg,
                                onRetry = { loading = true; loadPosts() },
                                onSwitchToForYou = { selectedTab = 1 }
                            )
                        }
                        else -> {
                            // ── P3B: Infinite scroll — reveal posts in pages of 10 ──
                            // The /feed_posts RLS endpoint returns up to N posts in one
                            // shot (no cursor), so we client-side paginate: take the
                            // first `displayCount` from the full `posts` list, and when
                            // the user reaches the bottom, show a skeleton + reveal
                            // the next 10 after a short delay. This gives the visual
                            // feel of infinite scroll without backend pagination.
                            val pageSize = 10
                            var displayCount by remember { mutableStateOf(pageSize) }
                            // Reset pagination whenever a fresh fetch happens
                            LaunchedEffect(posts) { displayCount = pageSize }
                            val displayedPosts = posts.take(displayCount)
                            val canLoadMore = displayedPosts.size < posts.size

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = TTSpacing.lg, end = TTSpacing.lg,
                                    top = TTSpacing.md, bottom = 96.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(TTSpacing.md)
                            ) {
                                items(displayedPosts, key = { it.id }) { post ->
                                    val author = authorCache[post.authorId]
                                    val like = likeState[post.id] ?: (false to 0)
                                    val saved = savedState[post.id] ?: false
                                    val commentCount = commentCountState[post.id] ?: 0
                                    FeedPostCard(
                                        post = post,
                                        author = author,
                                        liked = like.first,
                                        likeCount = like.second,
                                        commentCount = commentCount,
                                        saved = saved,
                                        loggedIn = api.loggedIn(),
                                        onLike = {
                                            if (!api.loggedIn()) {
                                                toast("Login dulu untuk like post")
                                                return@FeedPostCard
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val wasLiked = likeState[post.id]?.first ?: false
                                            val curCount = likeState[post.id]?.second ?: 0
                                            // Optimistic update
                                            likeState = likeState + (post.id to (!wasLiked to (curCount + if (wasLiked) -1 else 1)))
                                            scope.launch {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        if (wasLiked) api.unlikePost(post.id) else api.likePost(post.id)
                                                    }
                                                } catch (t: Throwable) {
                                                    // Revert
                                                    likeState = likeState + (post.id to (wasLiked to curCount))
                                                    toast("Gagal: ${t.message}")
                                                }
                                            }
                                        },
                                        onOpenComments = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            commentsTarget = post
                                        },
                                        onSave = {
                                            if (!api.loggedIn()) {
                                                toast("Login dulu untuk menyimpan post")
                                                return@FeedPostCard
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val wasSaved = savedState[post.id] ?: false
                                            savedState = savedState + (post.id to !wasSaved)
                                            scope.launch {
                                                try {
                                                    withContext(Dispatchers.IO) {
                                                        if (wasSaved) api.unsavePost(post.id) else api.savePost(post.id)
                                                    }
                                                    toast(if (wasSaved) "Post dihapus dari simpanan." else "Post disimpan.")
                                                } catch (t: Throwable) {
                                                    savedState = savedState + (post.id to wasSaved)
                                                    toast("Gagal: ${t.message}")
                                                }
                                            }
                                        },
                                        onShare = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT,
                                                    "${post.title}\n\n${post.body}\n\n— via DLavie 26 Launcher")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Post"))
                                        },
                                        onReport = { reportTarget = post },
                                        onOpenVideo = { url ->
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            }.onFailure { toast("Tidak bisa membuka URL") }
                                        },
                                        onVisitProfile = { uid -> onVisitProfile(uid) }
                                    )
                                }
                                // P3B: Load-more footer — when this item enters the
                                // viewport, LaunchedEffect(Unit) fires once and reveals
                                // the next `pageSize` posts. Skeleton gives the visual
                                // cue that more is loading.
                                if (canLoadMore) {
                                    item(key = "load_more_footer") {
                                        LaunchedEffect(Unit) {
                                            delay(250L)  // brief skeleton flash
                                            displayCount = (displayCount + pageSize).coerceAtMost(posts.size)
                                        }
                                        TTGameCardSkeleton()
                                    }
                                }
                            }
                        }
                    }
                }

                // ── FAB: create new post (bottom-right, circular white bg + black +) ──
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = TTSpacing.xl, bottom = TTSpacing.xxl)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (api.isGuest()) {
                                toast("Login diperlukan untuk posting. Guest mode hanya untuk browse.")
                            } else if (!api.loggedIn()) {
                                toast("Login dulu untuk membuat post")
                            } else {
                                showCreateSheet = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Buat post baru",
                        tint = Color.Black, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

// ─── Top bar: role badge + tabs + filter dropdown ──────────────────────────────
@Composable
private fun CommunityTopBar(
    roleBadge: String,
    roleColor: Color,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    showFilterMenu: Boolean,
    onToggleFilter: () -> Unit,
    onFilterSelect: (String) -> Unit
) {
    Box {
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = TTSpacing.lg, vertical = TTSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: user role badge
            Surface(
                color = roleColor.copy(0.15f),
                border = BorderStroke(1.dp, roleColor.copy(0.40f)),
                shape = TTShapes.chip
            ) {
                Text(
                    roleBadge,
                    color = roleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Center: tabs (Following | For You) — underline active
            Row(horizontalArrangement = Arrangement.spacedBy(TTSpacing.lg)) {
                CommunityTab("Following", selectedTab == 0) { onTabSelect(0) }
                CommunityTab("For You", selectedTab == 1) { onTabSelect(1) }
            }

            Spacer(Modifier.weight(1f))

            // Right: filter icon + dropdown
            Box {
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(Surface2)
                        .clickable { onToggleFilter() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FilterList, contentDescription = "Filter",
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { onToggleFilter() }
                ) {
                    DropdownMenuItem(
                        text = { Text("Terbaru", color = Color.White, fontSize = 13.sp) },
                        onClick = { onFilterSelect("newest") }
                    )
                    DropdownMenuItem(
                        text = { Text("Terlama", color = Color.White, fontSize = 13.sp) },
                        onClick = { onFilterSelect("oldest") }
                    )
                    Box(Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(0.08f)))
                    DropdownMenuItem(
                        text = { Text("By Role: Admin", color = Color.White, fontSize = 13.sp) },
                        onClick = { onFilterSelect("role_admin") }
                    )
                    DropdownMenuItem(
                        text = { Text("By Role: Developer", color = Color.White, fontSize = 13.sp) },
                        onClick = { onFilterSelect("role_developer") }
                    )
                    DropdownMenuItem(
                        text = { Text("By Role: Member", color = Color.White, fontSize = 13.sp) },
                        onClick = { onFilterSelect("role_member") }
                    )
                    Box(Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(0.08f)))
                    DropdownMenuItem(
                        text = { Text("By Date…", color = Color.White, fontSize = 13.sp) },
                        trailingIcon = { Icon(Icons.Rounded.CalendarMonth, null, tint = SoftText, modifier = Modifier.size(16.dp)) },
                        onClick = { onFilterSelect("date") }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityTab(label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (active) Color.White else SubText,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .width(if (active) 24.dp else 0.dp)
                .height(2.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
        )
    }
}

// ─── Active filter chip (clearable) ────────────────────────────────────────────
@Composable
private fun CommunityFilterChip(label: String, onClear: () -> Unit) {
    Surface(
        color = CandyCyan.copy(0.10f),
        border = BorderStroke(1.dp, CandyCyan.copy(0.30f)),
        shape = TTShapes.chip
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Box(
                Modifier.size(16.dp).clip(CircleShape).clickable { onClear() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Hapus filter",
                    tint = SoftText, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ─── Feed post card (TapTap-style) ─────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedPostCard(
    post: FeedPostData,
    author: AuthorInfo?,
    liked: Boolean,
    likeCount: Int,
    commentCount: Int,
    saved: Boolean,
    loggedIn: Boolean,
    onLike: () -> Unit,
    onOpenComments: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onVisitProfile: ((String) -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Like bounce: scale up briefly when liked toggles
    val likeScale by animateFloatAsState(
        targetValue = if (liked) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "like_bounce"
    )
    // ── P2B: Lottie heart-burst animation on like ──
    // Loads like_animation.json (AccentGreen heart + 6 particles). Plays once when
    // the user toggles liked false → true. The LottieAnimation overlay is sized
    // 48dp centered over the Like button so the burst visibly emanates from it.
    val likeComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.like_animation)
    )
    var playLikeBurst by remember { mutableStateOf(false) }
    // `by` delegation gives us a Float (animation progress 0..1) — matches the
    // existing LottieLoading pattern. We can't read isPlaying from a Float, so
    // we reset the overlay via a fixed delay after the one-shot finishes.
    val likeProgress by animateLottieCompositionAsState(
        composition = likeComposition,
        isPlaying = playLikeBurst,
        restartOnPlay = true,
        iterations = 1,
        speed = 1.4f
    )
    LaunchedEffect(playLikeBurst) {
        // The animation runs 60 frames @ 60fps / 1.4x speed ≈ 715ms.
        // Keep the overlay visible slightly longer, then dismiss.
        if (playLikeBurst) {
            delay(900L)
            playLikeBurst = false
        }
    }
    // ── Phase 2: detect video URL in body (YouTube/TikTok) ──
    val videoEmbed = remember(post.body) { extractVideoEmbed(post.body) }

    TTTappableCard(onClick = onOpenComments) {
        Column(Modifier.fillMaxWidth()) {
            // ── Image banner (16:9) — HANYA kalau image_url valid (tidak kosong / bukan "null"). ──
            // Fix: jangan render kotak gambar kosong untuk post tanpa image.
            // image_url sudah di-normalize saat parse (lihat FeedPostData parsing di
            // CommunityScreen.loadPosts dan parseFeed), jadi isNotBlank() cukup di sini.
            if (post.imageUrl.isNotBlank() && !post.imageUrl.equals("null", ignoreCase = true)) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Column(Modifier.padding(horizontal = TTSpacing.lg, vertical = TTSpacing.md)) {
                // Pinned / official badges row
                if (post.pinned || post.official) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (post.pinned) {
                            Surface(color = AmberWarn.copy(0.15f), shape = TTShapes.chip) {
                                Row(Modifier.padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.PushPin, null, tint = AmberWarn, modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("PINNED", color = AmberWarn, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        if (post.official) {
                            Surface(color = NeonGreen.copy(0.15f), shape = TTShapes.chip) {
                                Row(Modifier.padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Verified, null, tint = NeonGreen, modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("OFFICIAL", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(TTSpacing.sm))
                }

                // Title (bold white, 15sp)
                Text(
                    post.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Body preview (gray, 13sp, 2 lines)
                if (post.body.isNotBlank()) {
                    Spacer(Modifier.height(TTSpacing.xs))
                    Text(
                        post.body,
                        color = SoftText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // ── Phase 2: Video embed preview (YouTube thumbnail / TikTok badge) ──
                if (videoEmbed != null) {
                    Spacer(Modifier.height(TTSpacing.md))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .clickable { onOpenVideo(videoEmbed.originalUrl) }
                    ) {
                        if (videoEmbed.thumbnailUrl != null) {
                            AsyncImage(
                                model = videoEmbed.thumbnailUrl,
                                contentDescription = "Thumbnail ${videoEmbed.platform}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // TikTok — tidak ada thumbnail publik. Tampilkan placeholder gradien.
                            Box(
                                Modifier.fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF111111), Color(0xFF222222))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TIKTOK", color = Color.White.copy(0.85f),
                                        fontSize = 11.sp, fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Tap untuk buka di browser",
                                        color = Color.White.copy(0.6f), fontSize = 10.sp)
                                }
                            }
                        }
                        // Dark overlay supaya play icon kontras
                        Box(
                            Modifier.fillMaxSize().background(Color.Black.copy(0.28f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.PlayCircle,
                                contentDescription = "Putar video",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        // Platform badge (top-left)
                        Surface(
                            color = Color.Black.copy(0.55f),
                            shape = TTShapes.chip,
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        ) {
                            Text(
                                videoEmbed.platform.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(TTSpacing.md))

                // Author + timestamp + like + comment + menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar (circular, initial letter with gradient) — tap to visit profile
                    Box(
                        Modifier.clickable(enabled = onVisitProfile != null && author?.id?.isNotBlank() == true) {
                            val aid = author?.id ?: return@clickable
                            if (aid.isNotBlank()) onVisitProfile?.invoke(aid)
                        }
                    ) {
                        AuthorAvatar(author = author)
                    }
                    Spacer(Modifier.width(TTSpacing.sm))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                author?.displayName?.ifBlank { author.username } ?: "Anonim",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable(enabled = onVisitProfile != null && author?.id?.isNotBlank() == true) {
                                    val aid = author?.id ?: return@clickable
                                    if (aid.isNotBlank()) onVisitProfile?.invoke(aid)
                                }
                            )
                            // Verified badge for admin/developer (Instagram-style blue check)
                            if (author?.role == "admin" || author?.role == "developer") {
                                Icon(
                                    Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint = TextWhite,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Schedule, null, tint = SubText, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(relativeTime(post.createdAt), color = SubText, fontSize = 10.sp)
                            if (post.type.isNotBlank() && post.type != "community") {
                                Text(" · ${post.type}", color = SubText, fontSize = 10.sp)
                            }
                        }
                    }

                    // Like button (thumbs up + count) with Lottie heart-burst overlay.
                    // P2B: when user likes (false → true), playLikeBurst toggles true
                    // and the Lottie animation plays once on top of the icon.
                    Box(contentAlignment = Alignment.Center) {
                        if (playLikeBurst) {
                            LottieAnimation(
                                composition = likeComposition,
                                progress = { likeProgress },
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(TTShapes.chip)
                                .clickable {
                                    if (!liked) playLikeBurst = true
                                    onLike()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (liked) Icons.Rounded.ThumbUp else Icons.Rounded.ThumbUpOffAlt,
                                contentDescription = "Like",
                                tint = if (liked) AccentGreen else SoftText,
                                modifier = Modifier.size(18.dp).scale(likeScale)
                            )
                            Text(
                                if (likeCount > 0) likeCount.toString() else "",
                                color = if (liked) AccentGreen else SoftText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(TTSpacing.xs))

                    // ── Phase 2: Comment button (chat bubble + count) ──
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(TTShapes.chip)
                            .clickable { onOpenComments() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Komentar",
                            tint = SoftText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            if (commentCount > 0) commentCount.toString() else "",
                            color = SoftText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(TTSpacing.xs))

                    // Three-dot menu (⋮)
                    Box {
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).clickable { menuOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Menu",
                                tint = SoftText, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = DLavieGlass.AuroraCoral, modifier = Modifier.size(16.dp))
                                        Text("Lapor", color = Color.White, fontSize = 13.sp)
                                    }
                                },
                                onClick = { menuOpen = false; onReport() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, tint = DLavieGlass.AuroraCyan, modifier = Modifier.size(16.dp))
                                        Text("Bagikan", color = Color.White, fontSize = 13.sp)
                                    }
                                },
                                onClick = { menuOpen = false; onShare() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(if (saved) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkAdd, contentDescription = null, tint = DLavieGlass.AuroraViolet, modifier = Modifier.size(16.dp))
                                        Text(
                                            if (saved) "Hapus Simpanan" else "Simpan",
                                            color = Color.White, fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = { menuOpen = false; onSave() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Author avatar (circular, initial letter with gradient) ────────────────────
@Composable
private fun AuthorAvatar(author: AuthorInfo?) {
    val name = author?.displayName?.ifBlank { author.username } ?: "A"
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
    val avatarUrl = author?.avatarUrl ?: ""
    Box(
        Modifier.size(28.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(CandyCyan, CandyBlue))),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Text(initial, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ─── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun CommunityEmptyState(
    isFollowing: Boolean,
    loggedIn: Boolean,
    errorMsg: String,
    onRetry: () -> Unit,
    onSwitchToForYou: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(TTSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isFollowing) Icons.Rounded.AccountCircle else Icons.Rounded.Article,
            contentDescription = null,
            tint = SubText,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(TTSpacing.lg))
        Text(
            if (isFollowing) "Belum ada post dari yang kamu follow"
            else "Belum ada post",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(TTSpacing.xs))
        Text(
            when {
                errorMsg.isNotEmpty() -> errorMsg
                isFollowing && !loggedIn -> "Login dulu, lalu follow user lain untuk melihat post mereka di sini."
                isFollowing -> "Follow user lain untuk melihat post mereka di sini."
                else -> "Jadilah yang pertama membuat post di komunitas DLavie 26."
            },
            color = SoftText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(TTSpacing.lg))
        Button(
            onClick = { if (isFollowing) onSwitchToForYou() else onRetry() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = TTShapes.button
        ) {
            Text(
                if (isFollowing) "Lihat For You" else "Coba lagi",
                fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Create post bottom sheet ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostSheet(
    api: CommunityApi,
    onDismiss: () -> Unit,
    onPosted: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("community") }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var posting by remember { mutableStateOf(false) }
    // ── Phase 2: gallery picker state ──
    var uploading by remember { mutableStateOf(false) }
    // ── Task 6: "Simpan sebagai Draft" toggle ──
    // true → api.createFeedPost(..., isDraft=true) → post muncul di tab Draft di Profile,
    // tidak muncul di feed publik. User bisa publish kapan saja via tombol Publish di tab Draft.
    var saveAsDraft by remember { mutableStateOf(false) }

    val types = listOf("community" to "Community", "developer" to "Developer",
        "update" to "Update", "tutorial" to "Tutorial", "bugfix" to "Bugfix")
    val typeLabel = types.firstOrNull { it.first == type }?.second ?: "Community"

    // ── Phase 2: Gallery picker (ActivityResultContracts.PickVisualMedia) ──
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                uploading = true
                try {
                    val url = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalStateException("Tidak bisa membuka gambar.")
                        val bytes = inputStream.use { it.readBytes() }
                        if (bytes.isEmpty()) throw IllegalStateException("Gambar kosong.")
                        val filename = "post_${System.currentTimeMillis()}.jpg"
                        api.uploadImage(bytes, filename)
                    }
                    imageUrl = url
                } catch (t: Throwable) {
                    onError("Gagal upload gambar: ${t.message}")
                } finally {
                    uploading = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!posting && !uploading) onDismiss() },
        sheetState = sheetState,
        containerColor = GlassBase,
        dragHandle = null
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding()
                .padding(horizontal = TTSpacing.xl, vertical = TTSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TTSpacing.md)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Buat Post Baru", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(32.dp).clip(CircleShape).clickable { if (!posting && !uploading) onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = SoftText, modifier = Modifier.size(20.dp))
                }
            }

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul (wajib)", fontSize = 12.sp, color = SubText) },
                modifier = Modifier.fillMaxWidth(),
                shape = TTShapes.input,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
            )

            // Body input (multiline)
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Deskripsi (wajib)", fontSize = 12.sp, color = SubText) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = TTShapes.input,
                minLines = 4,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp, lineHeight = 19.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // ── Phase 2: Image picker (gallery) + preview ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !uploading && !posting,
                    shape = TTShapes.button,
                    border = BorderStroke(1.dp, GlassStroke),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.White.copy(0.04f)
                    )
                ) {
                    if (uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mengunggah…", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih Gambar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (imageUrl.isNotBlank()) {
                    Spacer(Modifier.weight(1f))
                    Box {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Preview gambar",
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Tiny remove button
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.75f))
                                .clickable { if (!posting) imageUrl = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hapus gambar",
                                tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // Image URL input (optional — manual paste sebagai fallback)
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("URL Gambar (opsional — atau pilih dari gallery di atas)", fontSize = 12.sp, color = SubText) },
                leadingIcon = { Icon(Icons.Rounded.Image, null, tint = SubText, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = TTShapes.input,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
            )

            // Type dropdown
            Box {
                OutlinedTextField(
                    value = typeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipe Post", fontSize = 12.sp, color = SubText) },
                    modifier = Modifier.fillMaxWidth().clickable { typeMenuOpen = true },
                    shape = TTShapes.input,
                    singleLine = true,
                    trailingIcon = {
                        Icon(if (typeMenuOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            null, tint = SoftText, modifier = Modifier.size(18.dp))
                    },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp)
                )
                DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    types.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White, fontSize = 13.sp) },
                            onClick = { type = value; typeMenuOpen = false }
                        )
                    }
                }
            }

            // ── Task 6: "Simpan sebagai Draft" toggle (Switch + label) ──
            Surface(
                color = Color.White.copy(0.04f),
                shape = TTShapes.input,
                border = BorderStroke(1.dp, GlassStroke.copy(0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { saveAsDraft = !saveAsDraft }
                        .padding(horizontal = TTSpacing.md, vertical = TTSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (saveAsDraft) Icons.Rounded.Save else Icons.Rounded.Drafts,
                        null, tint = if (saveAsDraft) NeonGreen else SubText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(TTSpacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Simpan sebagai Draft",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Draft tidak tampil di feed. Bisa di-publish nanti dari tab Draft di Profil.",
                            color = SubText, fontSize = 10.sp, lineHeight = 13.sp
                        )
                    }
                    Switch(
                        checked = saveAsDraft,
                        onCheckedChange = { saveAsDraft = it },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonGreen.copy(0.45f),
                            uncheckedThumbColor = SoftText,
                            uncheckedTrackColor = Surface2
                        )
                    )
                }
            }

            // Post button (label tergantung saveAsDraft)
            Button(
                onClick = {
                    if (title.trim().length < 3) { onError("Judul minimal 3 karakter."); return@Button }
                    if (body.trim().isEmpty()) { onError("Deskripsi wajib diisi."); return@Button }
                    if (posting || uploading) return@Button
                    posting = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                api.createFeedPost(title, body, imageUrl, type, saveAsDraft)
                            }
                            onPosted()
                        } catch (t: Throwable) {
                            onError("Gagal membuat post: ${t.message}")
                        } finally {
                            posting = false
                        }
                    }
                },
                enabled = !posting && !uploading,
                modifier = Modifier.fillMaxWidth(),
                shape = TTShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (saveAsDraft) Surface3 else Color.White,
                    contentColor = if (saveAsDraft) Color.White else Color.Black
                )
            ) {
                if (posting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = if (saveAsDraft) Color.White else Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (saveAsDraft) "Simpan Draft" else "Posting",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Report post dialog ────────────────────────────────────────────────────────
@Composable
private fun ReportPostDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, reason: String) -> Unit
) {
    val reasons = listOf(
        "spam" to "Spam",
        "inappropriate" to "Konten tidak pantas",
        "scam" to "Penipuan/scam",
        "other" to "Lainnya"
    )
    var selected by remember { mutableStateOf("spam") }
    var detail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Laporkan Post", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TTSpacing.sm)) {
                Text("Pilih alasan:", color = SoftText, fontSize = 12.sp)
                reasons.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = value }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = { selected = value },
                            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = SubText)
                        )
                        Spacer(Modifier.width(TTSpacing.sm))
                        Text(label, color = Color.White, fontSize = 13.sp)
                    }
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Detail (opsional)", fontSize = 12.sp, color = SubText) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = TTShapes.input,
                    minLines = 2,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected, detail) }) {
                Text("Kirim Laporan", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = SoftText) }
        },
        containerColor = GlassBase
    )
}

// ─── Phase 2 Community: Comments bottom sheet ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
    postId: String,
    api: CommunityApi,
    onDismiss: () -> Unit,
    onCommentAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    fun toast(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun loadComments() {
        scope.launch {
            loading = true
            errorMsg = ""
            try {
                val list = withContext(Dispatchers.IO) {
                    val arr = api.fetchComments(postId)
                    val out = mutableListOf<CommentItem>()
                    for (i in 0 until arr.length()) {
                        val c = arr.getJSONObject(i)
                        val uid = c.optString("user_id", "")
                        val profile = runCatching { api.getProfileById(uid) }.getOrNull() ?: JSONObject()
                        out.add(
                            CommentItem(
                                id          = c.optString("id"),
                                userId      = uid,
                                username    = profile.optString("username", "unknown"),
                                displayName = profile.optString("display_name", "User"),
                                body        = c.optString("body", ""),
                                createdAt   = c.optString("created_at", "")
                            )
                        )
                    }
                    out
                }
                comments = list
            } catch (t: Throwable) {
                errorMsg = t.message ?: "Gagal memuat komentar"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(postId) { loadComments() }

    ModalBottomSheet(
        onDismissRequest = { if (!sending) onDismiss() },
        sheetState = sheetState,
        containerColor = GlassBase,
        dragHandle = null
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding()
                .padding(horizontal = TTSpacing.xl, vertical = TTSpacing.lg)
                .heightIn(min = 280.dp, max = 560.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Komentar", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(32.dp).clip(CircleShape).clickable { if (!sending) onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = SoftText, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            // List / loading / empty
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(TTSpacing.md)) {
                            repeat(2) { TTGameCardSkeleton() }
                        }
                    }
                    errorMsg.isNotEmpty() && comments.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(errorMsg, color = SoftText, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { loadComments() }) {
                                Text("Coba lagi", color = Color.White)
                            }
                        }
                    }
                    comments.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.ChatBubbleOutline, null, tint = SubText, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Belum ada komentar.", color = SubText, fontSize = 13.sp)
                            Text("Jadilah yang pertama.", color = SubText, fontSize = 11.sp)
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(comments, key = { it.id }) { comment ->
                                CommentRow(
                                    comment = comment,
                                    onReply = { name ->
                                        commentText = "@$name "
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Comment input
            if (!api.loggedIn()) {
                Surface(
                    color = Color.White.copy(0.04f),
                    shape = TTShapes.input,
                    border = BorderStroke(1.dp, GlassStroke)
                ) {
                    Text(
                        "Login dulu untuk menulis komentar.",
                        color = SubText, fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Tulis komentar…", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = TTShapes.input,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(0.3f),
                            unfocusedBorderColor = GlassStroke,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = SubText,
                            unfocusedPlaceholderColor = SubText
                        ),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    // Send button — use Surface+clickable instead of IconButton for reliability
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(enabled = commentText.isNotBlank() && !sending) {
                                val text = commentText.trim()
                                if (text.isEmpty() || sending) return@clickable
                                sending = true
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { api.addComment(postId, text) }
                                        val me = CommentItem(
                                            id          = "local_${System.currentTimeMillis()}",
                                            userId      = api.userId(),
                                            username    = api.username().ifBlank { "you" },
                                            displayName = api.displayName().ifBlank { "You" },
                                            body        = text,
                                            createdAt   = java.text.SimpleDateFormat(
                                                "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US
                                            ).format(java.util.Date())
                                        )
                                        comments = comments + me
                                        commentText = ""
                                        onCommentAdded()
                                    } catch (t: Throwable) {
                                        toast("Gagal: ${t.message}")
                                    } finally {
                                        sending = false
                                    }
                                }
                            },
                        shape = CircleShape,
                        color = if (commentText.isNotBlank() && !sending) Color.White else Color.White.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (sending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Send,
                                    contentDescription = "Kirim komentar",
                                    tint = if (commentText.isNotBlank()) Color.Black else SubText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Phase 2 Community: Single comment row ────────────────────────────────────
@Composable
private fun CommentRow(comment: CommentItem, onReply: (String) -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color.White.copy(0.22f), Color.White.copy(0.10f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                comment.displayName.firstOrNull()?.uppercase() ?: "U",
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.displayName.ifBlank { comment.username.ifBlank { "Anonim" } },
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Text("·", color = SubText, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                Text(relativeTime(comment.createdAt), color = SubText, fontSize = 11.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(comment.body, color = SoftText, fontSize = 13.sp, lineHeight = 18.sp)
            // Reply button
            Text(
                "Balas",
                color = SubText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onReply(comment.displayName.ifBlank { comment.username }) }
                    .padding(top = 4.dp)
            )
        }
    }
}

// ─── Community feed data models ────────────────────────────────────────────────
private data class FeedPostData(
    val id: String,
    val authorId: String,
    val title: String,
    val body: String,
    val imageUrl: String,
    val type: String,
    val pinned: Boolean,
    val official: Boolean,
    val createdAt: String
)

/**
 * User search result (Task 3): lightweight profile row returned by api.searchUsers().
 * Used to render the dropdown list under the search bar in CommunityScreen.
 * Tapping a row navigates to [UserProfileScreen] with the user's id.
 */
data class UserSearchResult(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val uniqueId: Int,
    val role: String,
    val lastSeenAt: String = "",  // v7.0.5: ISO timestamp from profiles.last_seen_at (empty = never)
    val updatedAt: String = ""   // v7.0.5: fallback presence dari updated_at (saat last_seen_at null)
)

/**
 * Earned badge row (Task 1 / Task 2): badge_code + ISO timestamp of when it was
 * awarded. Used by ProfileScreen + UserProfileScreen to render the "Lencana" section.
 */
data class EarnedBadge(
    val badgeCode: String,
    val earnedAt: String
)

/**
 * Parse a feed_posts JSON object into a [FeedPostData].
 * Used by ProfileScreen (Post / Tersimpan / Draft tabs) and UserProfileScreen.
 *
 * Handles the "null" string quirk from org.json.optString (returns "null" string
 * when the value is JSONObject.NULL) by normalizing to "".
 */
private fun parseFeedPostData(o: JSONObject): FeedPostData {
    return FeedPostData(
        id        = o.optString("id"),
        authorId  = o.optString("author_id"),
        title     = o.optString("title"),
        body      = o.optString("body"),
        imageUrl  = o.optString("image_url", "").let { raw ->
            val s = raw.trim()
            if (s.isBlank() || s.equals("null", ignoreCase = true)) "" else s
        },
        type      = o.optString("type", "community"),
        pinned    = o.optBoolean("pinned"),
        official  = o.optBoolean("official"),
        createdAt = o.optString("created_at", "")
    )
}

/** Format a badge code (e.g. "first_login" → "First Login") for display. */
fun formatBadgeName(code: String): String {
    return code.split('_').joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

/** Map a badge code to a representative Material icon (replaces emoji). */
@Composable
fun badgeIcon(code: String): ImageVector = when {
    code.contains("login")            -> Icons.Rounded.HowToReg
    code.contains("gamer") || code.contains("play") -> Icons.Rounded.SportsEsports
    code.contains("streak") || code.contains("daily") -> Icons.Rounded.LocalFireDepartment
    code.contains("post")             -> Icons.Rounded.Edit
    code.contains("social") || code.contains("follow") -> Icons.Rounded.Group
    code.contains("rate")             -> Icons.Rounded.Star
    code.contains("comment")          -> Icons.Rounded.ChatBubbleOutline
    else                              -> Icons.Rounded.EmojiEvents
}

/** Legacy alias kept for source-compat — returns the icon name string. */
fun badgeEmoji(code: String): String = when {
    code.contains("login")            -> "icon_login"
    code.contains("gamer") || code.contains("play") -> "icon_gamer"
    code.contains("streak") || code.contains("daily") -> "icon_streak"
    code.contains("post")             -> "icon_post"
    code.contains("social") || code.contains("follow") -> "icon_social"
    code.contains("rate")             -> "icon_rate"
    code.contains("comment")          -> "icon_comment"
    else                              -> "icon_trophy"
}

private data class AuthorInfo(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val role: String
)

// ─── Time helpers (SimpleDateFormat — safe on all API levels incl. 24/25) ──────
/** Parse an ISO-8601 timestamptz (Supabase) to epoch millis (UTC). Returns 0 on failure. */
fun parseIsoToMillis(iso: String): Long {
    if (iso.isBlank()) return 0L
    return try {
        // Truncate to "yyyy-MM-dd'T'HH:mm:ss" (drop fractional + tz offset), assume UTC.
        val core = iso.substringBefore('.').substringBefore('+').take(19)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        sdf.parse(core)?.time ?: 0L
    } catch (_: Throwable) { 0L }
}

/** Relative time in Indonesian: "baru saja" / "Xm" / "Xj" / "Xd" / date. */
fun relativeTime(iso: String): String {
    val ts = parseIsoToMillis(iso)
    if (ts == 0L) return ""
    val diff = System.currentTimeMillis() - ts
    if (diff < 0) return "baru saja"
    val mins  = diff / 60_000L
    val hours = diff / 3_600_000L
    val days  = diff / 86_400_000L
    return when {
        mins  < 1   -> "baru saja"
        mins  < 60  -> "${mins}m"
        hours < 24  -> "${hours}j"
        days  < 30  -> "${days}d"
        else        -> iso.take(10)
    }
}

// ─── Phase 2 Community: Video embed (YouTube / TikTok) ────────────────────────

/** Detected video embed inside a post body. */
data class VideoEmbed(
    val platform: String,        // "youtube" | "tiktok"
    val thumbnailUrl: String?,   // null for TikTok (no public thumbnail API)
    val originalUrl: String      // full URL to open in browser
)

private val YOUTUBE_REGEX = Regex(
    "(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/shorts/)([\\w-]{6,})"
)
private val TIKTOK_REGEX = Regex(
    "(https?://)?(www\\.)?tiktok\\.com/@[\\w.]+/video/(\\d+)"
)

/**
 * Cari URL YouTube atau TikTok di dalam teks post body.
 * - YouTube: return thumbnail URL (img.youtube.com/vi/ID/hqdefault.jpg) + original watch URL.
 * - TikTok: return null thumbnail (caller shows TikTok badge) + original URL.
 * - Tidak ketemu: return null.
 */
fun extractVideoEmbed(text: String): VideoEmbed? {
    if (text.isBlank()) return null
    // YouTube
    val ytMatch = YOUTUBE_REGEX.find(text)
    if (ytMatch != null) {
        val videoId = ytMatch.groupValues[4]
        return VideoEmbed(
            platform     = "youtube",
            thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            originalUrl  = "https://www.youtube.com/watch?v=$videoId"
        )
    }
    // TikTok
    val ttMatch = TIKTOK_REGEX.find(text)
    if (ttMatch != null) {
        return VideoEmbed(
            platform     = "tiktok",
            thumbnailUrl = null,  // TikTok tidak punya public thumbnail API
            originalUrl  = ttMatch.value.let {
                if (it.startsWith("http")) it else "https://$it"
            }
        )
    }
    return null
}

// ─── Phase 2 Community: Comments ──────────────────────────────────────────────

private data class CommentItem(
    val id: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val body: String,
    val createdAt: String
)

// ─── Profile / Me screen — TapTap-style remake (Task 1) ───────────────────────
// Layout (top → bottom):
//   A. Header — avatar (tap to change), display name, "ID: {unique_id}", stats row
//      (Following | Followers | Likes), bio (optional)
//   B. Two-column — Kiri: "Lencana" (badges, horizontal scroll), Kanan: "Game Saya"
//      (FIFA 16 + play time, hanya kalau gameInstalled)
//   C. Tab bar — Post | Tersimpan | Draft (underline indicator)
//   D. Content per tab — Post: published posts; Tersimpan: saved posts; Draft:
//      draft posts with Publish button per draft
//   E. Bottom — Pengaturan entry, AccountSettingsCard (password/email/profile/pin),
//      Keamanan info, Logout button (preserved dari versi sebelumnya)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    api: CommunityApi,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {},
    expandedSection: String? = null,
    onExpandedSectionChange: (String?) -> Unit = {},
    onVisitProfile: (String) -> Unit = {}
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    // ── gameInstalled async (existing behavior) ──
    var gameInstalled by remember { mutableStateOf(false) }
    // profileLoading: true saat initial load, false setelah gameInstalled ter-resolve.
    var profileLoading by remember { mutableStateOf(true) }

    // ── Stats state (Task 1) ──
    var followingCount by remember { mutableStateOf(0) }
    var followerCount  by remember { mutableStateOf(0) }
    var likesCount     by remember { mutableStateOf(0) }
    var uniqueId       by remember { mutableStateOf(0) }
    var totalPlayMin   by remember { mutableStateOf(0) }
    var bio            by remember { mutableStateOf("") }

    // ── Badges state ──
    var myBadges by remember { mutableStateOf<List<EarnedBadge>>(emptyList()) }

    // ── Posts / Drafts / Saved state ──
    var myPosts      by remember { mutableStateOf<List<FeedPostData>>(emptyList()) }
    var myDrafts     by remember { mutableStateOf<List<FeedPostData>>(emptyList()) }
    var mySavedPosts by remember { mutableStateOf<List<FeedPostData>>(emptyList()) }
    var postsLoading by remember { mutableStateOf(true) }
    var publishingId by remember { mutableStateOf<String?>(null) }

    // ── Tab state: 0 = Post, 1 = Tersimpan, 2 = Draft ──
    var selectedTab by remember { mutableStateOf(0) }

    var confirmLogout by remember { mutableStateOf(false) }
    val initial = api.displayName().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    val role    = api.role()

    // ── Avatar image picker + upload state (preserved from v1.x) ──
    var avatarUrlState  by remember { mutableStateOf(api.avatarUrl()) }
    var avatarUploading by remember { mutableStateOf(false) }

    fun toast(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUploading = true
            scope.launch {
                try {
                    val newUrl = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        if (bytes == null || bytes.isEmpty())
                            throw IllegalStateException("Gagal membaca gambar dari gallery.")
                        val uploadedUrl = api.uploadAvatar(bytes)
                        api.updateAvatar(uploadedUrl)
                        uploadedUrl
                    }
                    avatarUrlState = newUrl
                    toast("Foto profil diperbarui.")
                } catch (t: Throwable) {
                    toast("Gagal upload foto: ${t.message}")
                } finally {
                    avatarUploading = false
                }
            }
        }
    }

    // ── Initial load: gameInstalled + stats + badges ──
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { runCatching { gameInstalled = isGameInstalled(context) } }
        profileLoading = false

        // Fetch stats + bio + badges + play time. Wrap each in runCatching so a
        // single failure (e.g. column missing) doesn't abort the rest.
        withContext(Dispatchers.IO) {
            runCatching { followingCount = api.getFollowingCount() }
            runCatching { followerCount  = api.getFollowerCount() }
            runCatching { likesCount     = api.getMyLikesReceived() }
            runCatching { uniqueId       = api.getUniqueId() }
            runCatching { totalPlayMin   = api.getTotalPlayTime() }
            runCatching { bio            = api.getMyBio() }

            // Try awarding login-based badge on profile open (e.g. first_login).
            runCatching { api.checkAndAwardBadges() }

            runCatching {
                val arr = api.getMyBadges()
                val list = (0 until arr.length()).mapNotNull { i ->
                    runCatching {
                        val o = arr.getJSONObject(i)
                        EarnedBadge(
                            badgeCode = o.optString("badge_code"),
                            earnedAt  = o.optString("earned_at")
                        )
                    }.getOrNull()
                }.filter { it.badgeCode.isNotBlank() }
                myBadges = list
            }
        }
    }

    // ── Load posts / drafts / saved based on selected tab ──
    fun loadTab() {
        postsLoading = true
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    when (selectedTab) {
                        0 -> {
                            val arr = api.getMyPosts()
                            myPosts = (0 until arr.length()).mapNotNull { i ->
                                runCatching { parseFeedPostData(arr.getJSONObject(i)) }.getOrNull()
                            }
                        }
                        1 -> {
                            val arr = api.getSavedPosts()
                            mySavedPosts = (0 until arr.length()).mapNotNull { i ->
                                runCatching {
                                    val o = arr.getJSONObject(i)
                                    val fp = o.optJSONObject("feed_posts")
                                    if (fp != null) parseFeedPostData(fp) else null
                                }.getOrNull()
                            }
                        }
                        2 -> {
                            val arr = api.getMyDrafts()
                            myDrafts = (0 until arr.length()).mapNotNull { i ->
                                runCatching { parseFeedPostData(arr.getJSONObject(i)) }.getOrNull()
                            }
                        }
                    }
                }
            }
            postsLoading = false
        }
    }
    LaunchedEffect(selectedTab) { loadTab() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = TTSpacing.lg, vertical = TTSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(TTSpacing.md)
    ) {

        // ════════════════════════════════════════════════════════════════════
        // A. Profile Header (avatar + display name + unique ID + stats + bio)
        // ════════════════════════════════════════════════════════════════════
        if (profileLoading) {
            TTGameCardSkeleton()
        } else {
            PremiumGlassCard(gradientBorder = true) {
                val infiniteTransition = rememberInfiniteTransition(label = "profile_glow")
                val avatarGlow by infiniteTransition.animateFloat(
                    initialValue = 0.25f, targetValue = 0.55f,
                    animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "avatar_glow_val"
                )
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                ) {
                    // Avatar (tap to change) with rotating gradient ring + glow
                    Box(
                        Modifier.size(88.dp).clickable {
                            if (!api.loggedIn()) {
                                toast("Login dulu untuk ganti foto profil.")
                                return@clickable
                            }
                            if (avatarUploading) return@clickable
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.matchParentSize().clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(CandyCyan.copy(avatarGlow * 0.6f), Color.Transparent),
                                        radius = 90f
                                    )
                                )
                                .blur(12.dp)
                        )
                        val ringRotation by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
                            label = "ring_rotation"
                        )
                        Canvas(
                            Modifier.size(88.dp).graphicsLayer { rotationZ = ringRotation }
                        ) {
                            val stroke = 3.dp.toPx()
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(CandyCyan, PremiumViolet, CandyCyan.copy(0.3f), CandyCyan)
                                ),
                                radius = (size.minDimension / 2f) - (stroke / 2f),
                                style = Stroke(width = stroke)
                            )
                        }
                        if (avatarUrlState.isNotEmpty()) {
                            AsyncImage(
                                model = avatarUrlState,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(72.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            DLavieLogoCover(
                                size = 72.dp, text = initial,
                                fontSize = 28.sp, shape = CircleShape
                            )
                        }
                        if (avatarUploading) {
                            Box(
                                Modifier.matchParentSize().clip(CircleShape)
                                    .background(Color.Black.copy(0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White, strokeWidth = 2.dp
                                )
                            }
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(26.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Carbon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = Carbon, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Display name
                    Text(
                        api.displayName().ifEmpty { "DLavie Player" },
                        fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    // Unique ID (small gray)
                    if (uniqueId > 0) {
                        Text(
                            "ID: $uniqueId",
                            color = SubText, fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Username + role pill row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TTSpacing.xs)
                    ) {
                        Text(
                            "@${api.username().ifEmpty { "unknown" }}",
                            color = SoftText, fontSize = 12.sp
                        )
                        ModernPill(role.uppercase(), roleBadgeColor(role))
                    }

                    // Bio (optional)
                    if (bio.isNotBlank()) {
                        Text(
                            bio,
                            color = SoftText, fontSize = 12.sp, lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = TTSpacing.md)
                        )
                    }

                    Spacer(Modifier.height(TTSpacing.xs))

                    // ── Stats row: Following | Followers | Likes (3 column) ──
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                    ) {
                        ProfileStatColumn(
                            label = "Mengikuti",
                            value = followingCount,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatColumn(
                            label = "Pengikut",
                            value = followerCount,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatColumn(
                            label = "Likes",
                            value = likesCount,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // B. Two-Column: Lencana (Badges) | Game Saya
        // ════════════════════════════════════════════════════════════════════
        if (!profileLoading) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
            ) {
                // ── KIRI: Lencana ──
                GlassCard(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = AmberWarn, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(TTSpacing.xs))
                        Text("Lencana", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(TTSpacing.sm))
                    if (myBadges.isEmpty()) {
                        Text(
                            "Dapatkan Lencana",
                            color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Mainkan game & aktif di komunitas untuk membuka lencana.",
                            color = SubText, fontSize = 9.sp, lineHeight = 12.sp
                        )
                    } else {
                        // Horizontal scrollable row of badges
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                        ) {
                            myBadges.take(8).forEach { badge ->
                                BadgeChip(
                                    icon = badgeIcon(badge.badgeCode),
                                    label = formatBadgeName(badge.badgeCode)
                                )
                            }
                        }
                        if (myBadges.size > 8) {
                            Spacer(Modifier.height(TTSpacing.xs))
                            Text(
                                "+${myBadges.size - 8} lainnya",
                                color = SubText, fontSize = 10.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── KANAN: Game Saya ──
                GlassCard(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SportsEsports, null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(TTSpacing.xs))
                        Text("Game Saya", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(TTSpacing.sm))
                    if (gameInstalled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(28.dp).background(NeonGreen.copy(0.12f), TTShapes.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.SportsSoccer, null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(TTSpacing.xs))
                            Column {
                                Text("FIFA 16 Mobile", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    "Main ${if (totalPlayMin >= 60) "${totalPlayMin / 60}j " else ""}${totalPlayMin % 60}m",
                                    color = SoftText, fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            "Belum ada game",
                            color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Install FIFA 16 untuk mulai main.",
                            color = SubText, fontSize = 9.sp, lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // C. Tab Bar (Post | Tersimpan | Draft) with underline indicator
        // ════════════════════════════════════════════════════════════════════
        if (!profileLoading) {
            Row(
                Modifier.fillMaxWidth().clip(TTShapes.input)
                    .background(Surface2)
                    .padding(TTSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(TTSpacing.xs)
            ) {
                ProfileTabButton(
                    label = "Post",
                    count = myPosts.size,
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 0 }
                ProfileTabButton(
                    label = "Tersimpan",
                    count = mySavedPosts.size,
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 1 }
                ProfileTabButton(
                    label = "Draft",
                    count = myDrafts.size,
                    selected = selectedTab == 2,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = 2 }
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // D. Content per tab
        // ════════════════════════════════════════════════════════════════════
        if (!profileLoading) {
            when {
                postsLoading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(TTSpacing.md)) {
                        repeat(2) { TTGameCardSkeleton() }
                    }
                }
                selectedTab == 0 && myPosts.isEmpty() -> ProfileEmptyPosts(
                    text = "Belum ada post. Buat post pertamamu di Komunitas!"
                )
                selectedTab == 1 && mySavedPosts.isEmpty() -> ProfileEmptyPosts(
                    text = "Belum ada post tersimpan. Tap ikon bookmark di post mana pun untuk menyimpan."
                )
                selectedTab == 2 && myDrafts.isEmpty() -> ProfileEmptyPosts(
                    text = "Belum ada draft. Aktifkan toggle 'Simpan sebagai Draft' saat membuat post."
                )
                else -> {
                    val list = when (selectedTab) {
                        0 -> myPosts
                        1 -> mySavedPosts
                        else -> myDrafts
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(TTSpacing.md)) {
                        list.forEach { post ->
                            ProfilePostCard(
                                post = post,
                                isDraft = selectedTab == 2,
                                publishing = publishingId == post.id,
                                onPublish = {
                                    publishingId = post.id
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { api.publishDraft(post.id) }
                                            toast("Draft di-publish!")
                                            publishingId = null
                                            loadTab()
                                        } catch (t: Throwable) {
                                            toast("Gagal publish: ${t.message}")
                                            publishingId = null
                                        }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { api.deleteFeedPost(post.id) }
                                            toast("Post dihapus.")
                                            loadTab()
                                        } catch (t: Throwable) {
                                            toast("Gagal hapus: ${t.message}")
                                        }
                                    }
                                },
                                onVisitProfile = if (selectedTab == 1 && post.authorId != api.userId()) {
                                    { onVisitProfile(post.authorId) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // E. Bottom: Pengaturan entry + AccountSettings + Keamanan + Logout
        // (preserved from v1.x — unchanged behavior)
        // ════════════════════════════════════════════════════════════════════
        if (!profileLoading) {
            TTTappableCard(
                onClick = { if (gameInstalled) launchGame(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = TTSpacing.lg, vertical = TTSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).background(NeonGreen.copy(0.12f), TTShapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.SportsSoccer, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(TTSpacing.md))
                    Column(Modifier.weight(1f)) {
                        Text("FIFA 16 Mobile", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(
                            if (gameInstalled) "Tap untuk mainkan" else "Game belum terinstall",
                            color = SoftText, fontSize = 11.sp
                        )
                    }
                    if (gameInstalled) {
                        Icon(Icons.Rounded.PlayCircle, null, tint = NeonGreen, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        if (!profileLoading) {
            TTTappableCard(
                onClick = { onOpenSettings() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Pengaturan", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, null, tint = SubText, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ── Info akun ──
        GlassCard {
            TTSectionHeader(title = "Detail Akun", icon = Icons.Rounded.AccountCircle)
            Spacer(Modifier.height(TTSpacing.sm))
            ProfRow("Username",    "@${api.username().ifEmpty { "-" }}")
            ProfRow("Nama",        api.displayName().ifEmpty { "-" })
            ProfRow("Role",        role.replaceFirstChar { it.uppercase() })
            ProfRow("Server",      "DLavie Cloud")
        }

        // ── Language Settings Card (v6.2) ──────────────────────────────────────
        LanguageSettingsCard(context = context)

        // ── FCM Diagnostic Card (v5.4.3) — ADMIN/DEVELOPER ONLY ────────────────
        // Shows real-time FCM token + upload status on screen (no laptop needed).
        // Only visible to admin & developer roles — regular users don't see this.
        if (role == "admin" || role == "developer") {
            FcmDiagnosticCard(api = api, context = context)
        }

        // ── Akun & Keamanan (Account Settings — password/email/profile/pin) ──
        AccountSettingsCard(
            api = api,
            context = context,
            expandedSection = expandedSection,
            onExpandedSectionChange = onExpandedSectionChange
        )

        // ── Keamanan ──
        GlassCard {
            TTSectionHeader(title = "Keamanan", icon = Icons.Rounded.Security)
            Spacer(Modifier.height(TTSpacing.sm))
            listOf(
                "Sesi terenkripsi — diperbarui otomatis setiap 50 menit.",
                "Setiap pembaruan diverifikasi sebelum diterapkan.",
                "Data login tidak pernah disimpan di penyimpanan lokal."
            ).forEach { text ->
                Row(
                    Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(13.dp).padding(top = 2.dp))
                    Text(text, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }

        // ── Logout (preserved from v1.x) ──
        AnimatedContent(targetState = confirmLogout, label = "logout") { confirm ->
            if (!confirm) {
                OutlinedButton(
                    onClick  = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = TTShapes.button,
                    border   = BorderStroke(1.dp, DangerRed.copy(0.45f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                ) {
                    Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(TTSpacing.sm))
                    Text("Keluar dari Akun", color = DangerRed, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            } else {
                GlassCard(borderColor = DangerRed.copy(0.55f)) {
                    Text("Konfirmasi Keluar", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(TTSpacing.xs))
                    Text("Kamu harus login kembali setelah keluar.", color = SoftText, fontSize = 13.sp)
                    Spacer(Modifier.height(TTSpacing.md))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)) {
                        OutlinedButton(
                            onClick  = { confirmLogout = false },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = TTShapes.button,
                            border   = BorderStroke(1.dp, GlassStroke),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = SoftText)
                        ) { Text("Batal", fontWeight = FontWeight.Bold) }
                        Button(
                            onClick  = onLogout,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape    = TTShapes.button,
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(TTSpacing.xs))
                            Text("Keluar", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(TTSpacing.sm))
    }
}

// ─── Helper composables for the new ProfileScreen ──────────────────────────────

/** Single column stat tile (label + count) used in the profile header stats row. */
@Composable
private fun ProfileStatColumn(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clip(TTShapes.small).background(Surface2.copy(0.6f))
            .padding(vertical = TTSpacing.sm, horizontal = TTSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            label,
            color = SubText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Pill-shaped badge chip (icon + label) for the Lencana horizontal scroll. */
@Composable
private fun BadgeChip(icon: ImageVector, label: String) {
    Surface(
        color = DLavieGlass.GlassSurface,
        border = BorderStroke(1.dp, DLavieGlass.GlassStroke),
        shape = TTShapes.small
    ) {
        Column(
            Modifier.padding(horizontal = TTSpacing.sm, vertical = TTSpacing.xs)
                .width(72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(DLavieGlass.AuroraCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = DLavieGlass.AuroraCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = DLavieGlass.TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp
            )
        }
    }
}

/** Tab button with underline indicator + count badge. Used by ProfileScreen. */
@Composable
private fun ProfileTabButton(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) Color.White.copy(0.10f) else Color.Transparent,
        tween(220), label = "tab_bg_$label"
    )
    val tint by animateColorAsState(
        if (selected) Color.White else SubText,
        tween(220), label = "tab_tint_$label"
    )
    Column(
        modifier = modifier
            .clip(TTShapes.small)
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = TTSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = tint,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold
            )
            if (count > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    count.toString(),
                    color = SubText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .width(if (selected) 22.dp else 0.dp)
                .height(2.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
        )
    }
}

/** Empty-state for profile tabs. */
@Composable
private fun ProfileEmptyPosts(text: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = TTSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Article, null, tint = SubText, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(TTSpacing.sm))
        Text(
            text,
            color = SoftText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TTSpacing.lg)
        )
    }
}

/**
 * Compact post card for ProfileScreen tabs (Post / Tersimpan / Draft).
 *
 * Renders: image (if any), title, body preview, timestamp, type tag.
 * For drafts: shows a "Publish" + "Hapus" button row at the bottom.
 * For saved posts: shows a small "tap to view author" hint (author name tappable).
 *
 * Lighter than FeedPostCard (no like/comment/save buttons — those are feed-only).
 */
@Composable
private fun ProfilePostCard(
    post: FeedPostData,
    isDraft: Boolean = false,
    publishing: Boolean = false,
    onPublish: () -> Unit = {},
    onDelete: () -> Unit = {},
    onVisitProfile: (() -> Unit)? = null
) {
    GlassCard {
        Column(Modifier.fillMaxWidth()) {
            // Image (if any) — 16:9 banner
            if (post.imageUrl.isNotBlank() && !post.imageUrl.equals("null", ignoreCase = true)) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(Modifier.padding(horizontal = TTSpacing.md, vertical = TTSpacing.sm)) {
                // Draft badge
                if (isDraft) {
                    Surface(color = AmberWarn.copy(0.15f), shape = TTShapes.chip) {
                        Row(
                            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Drafts, null, tint = AmberWarn, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("DRAFT", color = AmberWarn, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(TTSpacing.xs))
                }

                // Title
                Text(
                    post.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Body preview
                if (post.body.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        post.body,
                        color = SoftText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(TTSpacing.sm))

                // Footer: timestamp + type + visit-profile (for saved posts)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = SubText, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(relativeTime(post.createdAt), color = SubText, fontSize = 10.sp)
                    if (post.type.isNotBlank() && post.type != "community") {
                        Text(" · ${post.type}", color = SubText, fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    if (onVisitProfile != null) {
                        Text(
                            "Lihat penulis →",
                            color = CandyCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onVisitProfile() }
                        )
                    }
                }

                // Draft action buttons (Publish + Hapus)
                if (isDraft) {
                    Spacer(Modifier.height(TTSpacing.sm))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                    ) {
                        Button(
                            onClick = onPublish,
                            enabled = !publishing,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = TTShapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color(0xFF00150B)
                            )
                        ) {
                            if (publishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color(0xFF00150B), strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Memproses…", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            } else {
                                Icon(Icons.Rounded.Send, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Publish", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            enabled = !publishing,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = TTShapes.button,
                            border = BorderStroke(1.dp, DangerRed.copy(0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hapus", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

/** User search result row (used by CommunityScreen search dropdown). */
@Composable
private fun UserSearchRow(
    user: UserSearchResult,
    onClick: () -> Unit
) {
    val initial = (user.displayName.ifBlank { user.username }).firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    // v7.0.5: Compute presence — prefer last_seen_at (heartbeat), fallback to updated_at
    //   - Online: < 2 min ago (white dot + "Online")
    //   - Recent: < 10 min (gray dot + "Last seen Xm ago")
    //   - Away:   < 1 hour (dim dot + "Last seen Xm ago")
    //   - Offline: > 1 hour or empty (dim dot + "Last seen Xh ago" / "Offline")
    val presenceTimestamp = user.lastSeenAt.ifBlank { user.updatedAt }
    val presence = computePresence(presenceTimestamp)
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = TTSpacing.md, vertical = TTSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar (circular, foto kalau ada, initial kalau tidak) + presence dot
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CandyCyan, CandyBlue))),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.displayName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(initial, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
            // v7.0.5: Presence dot (bottom-right of avatar)
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(presence.dotColor)
                    .border(1.5.dp, PureBlack, CircleShape)  // ring supaya kontras dgn avatar
            )
        }
        Spacer(Modifier.width(TTSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(
                user.displayName.ifBlank { user.username },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("@${user.username}", color = SoftText, fontSize = 11.sp, maxLines = 1)
                if (user.uniqueId > 0) {
                    Text(" · ID: ${user.uniqueId}", color = SubText, fontSize = 10.sp)
                }
            }
            // v7.0.5: Presence status text (below username)
            Text(
                presence.label,
                color = presence.labelColor,
                fontSize = 10.sp,
                fontWeight = if (presence.isOnline) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
        ModernPill(user.role.uppercase(), roleBadgeColor(user.role))
        Spacer(Modifier.width(TTSpacing.xs))
        Icon(Icons.Rounded.ChevronRight, null, tint = SubText, modifier = Modifier.size(18.dp))
    }
}

// v7.0.5: Presence data class + computePresence helper
private data class Presence(
    val isOnline: Boolean,
    val dotColor: Color,
    val label: String,
    val labelColor: Color
)

/**
 * Compute presence from last_seen_at ISO timestamp.
 *   - Online: < 2 min ago (white dot + "Online")
 *   - Recent: < 10 min ago (gray dot + "Last seen Xm ago")
 *   - Away:   < 1 hour (dim dot + "Last seen Xm ago")
 *   - Offline: > 1 hour or empty (dim dot + "Last seen Xh ago" / "Offline")
 */
private fun computePresence(lastSeenAt: String): Presence {
    if (lastSeenAt.isBlank() || lastSeenAt == "null") {
        return Presence(
            isOnline = false,
            dotColor = SubText,
            label = "Offline",
            labelColor = SubText
        )
    }
    return try {
        // v7.0.5: Supabase kirim format dengan microseconds: "2026-07-05T06:17:18.81963+00:00"
        // atau "2026-07-05T06:17:18.81963Z". Trim microseconds ke millis (3 digit) supaya
        // SimpleDateFormat bisa parse. Juga handle zone offset +00:00 vs Z.
        var ts = lastSeenAt.trim()
        // Normalisasi: ganti "+00:00" dengan "Z" (ISO 8601 UTC indicator)
        if (ts.endsWith("+00:00")) ts = ts.substring(0, ts.length - 6) + "Z"
        // Trim fractional seconds ke 3 digit (millis)
        val dotIdx = ts.indexOf('.')
        if (dotIdx > 0) {
            val afterDot = ts.substring(dotIdx + 1)
            // Find where fractional part ends (Z or + or -)
            var endIdx = afterDot.length
            for (i in afterDot.indices) {
                val c = afterDot[i]
                if (c == 'Z' || c == '+' || c == '-') { endIdx = i; break }
            }
            val frac = afterDot.substring(0, endIdx).take(3).padEnd(3, '0')
            val tz = afterDot.substring(endIdx)
            ts = ts.substring(0, dotIdx) + "." + frac + tz
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val seen = sdf.parse(ts)?.time ?: System.currentTimeMillis()
        val diffMs = System.currentTimeMillis() - seen
        val diffMin = diffMs / 60_000L
        val diffHour = diffMin / 60
        when {
            diffMin < 2 -> Presence(
                isOnline = true,
                dotColor = Color.White,
                label = "Online",
                labelColor = Color.White
            )
            diffMin < 10 -> Presence(
                isOnline = false,
                dotColor = SoftText,
                label = "Last seen ${diffMin}m ago",
                labelColor = SoftText
            )
            diffHour < 1 -> Presence(
                isOnline = false,
                dotColor = SubText,
                label = "Last seen ${diffMin}m ago",
                labelColor = SubText
            )
            diffHour < 24 -> Presence(
                isOnline = false,
                dotColor = SubText,
                label = "Last seen ${diffHour}h ago",
                labelColor = SubText
            )
            else -> Presence(
                isOnline = false,
                dotColor = SubText,
                label = "Last seen ${diffHour / 24}d ago",
                labelColor = SubText
            )
        }
    } catch (e: Exception) {
        android.util.Log.w("DLaviePresence", "computePresence parse failed: '${lastSeenAt}' → ${e.message}")
        Presence(
            isOnline = false,
            dotColor = SubText,
            label = "Offline",
            labelColor = SubText
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  USER PROFILE SCREEN (Task 4 + Task 5)
//
//  Shown as a full-screen overlay when user taps a username in:
//    - CommunityScreen search results
//    - FeedPostCard author name/avatar
//    - ProfileScreen → Tersimpan → "Lihat penulis →" hint
//
//  Layout mirrors ProfileScreen's header (avatar, name, unique ID, stats, bio,
//  badges) but WITHOUT:
//    - Account settings (password/email/profile/pin) — those are owner-only
//    - Logout button
//    - Tab bar (we only show their published posts — no access to their drafts
//      or saved posts, both are owner-only)
//
//  WITH:
//    - Follow / Unfollow button (Task 5) — optimistic update
//    - Back arrow at top
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun UserProfileScreen(
    userId: String,
    api: CommunityApi,
    onBack: () -> Unit,
    onVisitProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val haptic  = LocalHapticFeedback.current

    // ── Profile state ──
    var profile by remember { mutableStateOf<JSONObject?>(null) }
    var followingCount by remember { mutableStateOf(0) }
    var followerCount  by remember { mutableStateOf(0) }
    var likesCount     by remember { mutableStateOf(0) }
    var totalPlayMin   by remember { mutableStateOf(0) }
    var userBadges     by remember { mutableStateOf<List<EarnedBadge>>(emptyList()) }
    var userPosts      by remember { mutableStateOf<List<FeedPostData>>(emptyList()) }
    var loading        by remember { mutableStateOf(true) }
    var isFollowing    by remember { mutableStateOf(false) }
    var followBusy     by remember { mutableStateOf(false) }

    // Author info for the viewed user (kept in state for future FeedPostCard use).
    var authorCache by remember { mutableStateOf<Map<String, AuthorInfo>>(emptyMap()) }

    fun toast(msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    // ── Initial load ──
    LaunchedEffect(userId) {
        loading = true
        withContext(Dispatchers.IO) {
            runCatching {
                val p = api.getProfileById(userId)
                profile = p
            }
            runCatching { followingCount = api.getFollowingCountForUser(userId) }
            runCatching { followerCount  = api.getFollowerCountForUser(userId) }
            runCatching { likesCount     = api.getLikesReceivedForUser(userId) }
            runCatching { totalPlayMin   = api.getTotalPlayTimeByUser(userId) }
            runCatching {
                val arr = api.getBadgesByUser(userId)
                userBadges = (0 until arr.length()).mapNotNull { i ->
                    runCatching {
                        val o = arr.getJSONObject(i)
                        EarnedBadge(o.optString("badge_code"), o.optString("earned_at"))
                    }.getOrNull()
                }.filter { it.badgeCode.isNotBlank() }
            }
            runCatching {
                val arr = api.getPostsByUser(userId)
                userPosts = (0 until arr.length()).mapNotNull { i ->
                    runCatching { parseFeedPostData(arr.getJSONObject(i)) }.getOrNull()
                }
            }
            // Fetch follow status (only meaningful if current user is logged in)
            if (api.loggedIn() && userId != api.userId()) {
                runCatching { isFollowing = api.isFollowing(userId) }
            }
        }
        loading = false
    }

    // Resolve author cache for the viewed user (used to render post cards via
    // FeedPostCard — but since the profile is already loaded, we synthesize it).
    LaunchedEffect(profile) {
        val p = profile ?: return@LaunchedEffect
        val info = AuthorInfo(
            id          = p.optString("id"),
            username    = p.optString("username", ""),
            displayName = p.optString("display_name", ""),
            avatarUrl   = p.optString("avatar_url", "").let { raw ->
                val s = raw.trim()
                if (s.isBlank() || s.equals("null", ignoreCase = true)) "" else s
            },
            role        = p.optString("role", "user")
        )
        authorCache = authorCache + (info.id to info)
    }

    Box(Modifier.fillMaxSize().background(Carbon)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize(), alpha = 0.3f)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = TTSpacing.lg, vertical = TTSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TTSpacing.md)
        ) {
            // ── Top bar: back arrow + "Profil" title ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(Surface2).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali",
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(TTSpacing.md))
                Text("Profil User", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }

            if (loading) {
                repeat(3) { TTGameCardSkeleton() }
            } else if (profile == null) {
                // Profile not found / error
                Column(
                    Modifier.fillMaxWidth().padding(top = TTSpacing.xxxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Person, null, tint = SubText, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(TTSpacing.md))
                    Text("Profil tidak ditemukan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(TTSpacing.xs))
                    Text("User mungkin sudah dihapus atau terjadi kesalahan jaringan.",
                        color = SoftText, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            } else {
                val p = profile!!
                val displayName = p.optString("display_name", p.optString("username", "User"))
                val username = p.optString("username", "")
                val avatarUrl = p.optString("avatar_url", "").let { raw ->
                    val s = raw.trim()
                    if (s.isBlank() || s.equals("null", ignoreCase = true)) "" else s
                }
                val uniqueId = p.optInt("unique_id", 0)
                val role = p.optString("role", "user")
                val bio = p.optString("bio", "").let { raw ->
                    val s = raw.trim()
                    if (s.isBlank() || s.equals("null", ignoreCase = true)) "" else s
                }
                val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

                // ── Profile header (mirror of ProfileScreen header, no edit affordance) ──
                PremiumGlassCard(gradientBorder = true) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                    ) {
                        // Avatar (static — no tap to change)
                        Box(
                            Modifier.size(88.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.matchParentSize().clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(CandyCyan.copy(0.4f), Color.Transparent),
                                            radius = 90f
                                        )
                                    )
                                    .blur(12.dp)
                            )
                            if (avatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = displayName,
                                    modifier = Modifier.size(72.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    Modifier.size(72.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(CandyCyan, CandyBlue))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initial, color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        // Display name
                        Text(
                            displayName.ifEmpty { "DLavie Player" },
                            fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        // Unique ID
                        if (uniqueId > 0) {
                            Text("ID: $uniqueId", color = SubText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        // Username + role pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(TTSpacing.xs)
                        ) {
                            if (username.isNotEmpty()) {
                                Text("@$username", color = SoftText, fontSize = 12.sp)
                            }
                            ModernPill(role.uppercase(), roleBadgeColor(role))
                        }

                        // Bio
                        if (bio.isNotBlank()) {
                            Text(
                                bio,
                                color = SoftText, fontSize = 12.sp, lineHeight = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = TTSpacing.md)
                            )
                        }

                        Spacer(Modifier.height(TTSpacing.xs))

                        // ── Follow / Unfollow button (Task 5) ──
                        // Hidden if user is viewing their own profile.
                        if (api.loggedIn() && userId != api.userId()) {
                            val btnColors = if (isFollowing) {
                                ButtonDefaults.buttonColors(
                                    containerColor = Surface2,
                                    contentColor = SoftText
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            }
                            Button(
                                onClick = {
                                    if (followBusy) return@Button
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    // Optimistic update
                                    val prev = isFollowing
                                    isFollowing = !prev
                                    followerCount += if (prev) -1 else 1
                                    followBusy = true
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                if (prev) api.unfollowUser(userId)
                                                else api.followUser(userId)
                                            }
                                            toast(if (prev) "Berhenti mengikuti" else "Mengikuti $displayName")
                                        } catch (t: Throwable) {
                                            // Revert
                                            isFollowing = prev
                                            followerCount += if (prev) 1 else -1
                                            toast("Gagal: ${t.message}")
                                        } finally {
                                            followBusy = false
                                        }
                                    }
                                },
                                enabled = !followBusy,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = TTShapes.button,
                                colors = btnColors
                            ) {
                                if (followBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = if (isFollowing) SoftText else Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        if (isFollowing) Icons.Rounded.PersonRemove else Icons.Rounded.PersonAdd,
                                        null, modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    if (isFollowing) "Mengikuti" else "Ikuti",
                                    fontSize = 13.sp, fontWeight = FontWeight.Black
                                )
                            }
                        } else if (!api.loggedIn()) {
                            // Not logged in — show disabled "Login untuk ikuti"
                            OutlinedButton(
                                onClick = { toast("Login dulu untuk mengikuti user ini.") },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = TTShapes.button,
                                border = BorderStroke(1.dp, GlassStroke),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftText)
                            ) {
                                Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Login untuk Ikuti", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(TTSpacing.xs))

                        // ── Stats row ──
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                        ) {
                            ProfileStatColumn("Mengikuti", followingCount, Modifier.weight(1f))
                            ProfileStatColumn("Pengikut", followerCount, Modifier.weight(1f))
                            ProfileStatColumn("Likes", likesCount, Modifier.weight(1f))
                        }
                    }
                }

                // ── Two-column: Lencana + Play time (compact) ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                ) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = AmberWarn, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(TTSpacing.xs))
                            Text("Lencana", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(TTSpacing.sm))
                        if (userBadges.isEmpty()) {
                            Text("Belum ada lencana", color = SubText, fontSize = 11.sp)
                        } else {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
                            ) {
                                userBadges.take(8).forEach { badge ->
                                    BadgeChip(
                                        icon = badgeIcon(badge.badgeCode),
                                        label = formatBadgeName(badge.badgeCode)
                                    )
                                }
                            }
                            if (userBadges.size > 8) {
                                Spacer(Modifier.height(TTSpacing.xs))
                                Text("+${userBadges.size - 8} lainnya",
                                    color = SubText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SportsEsports, null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(TTSpacing.xs))
                            Text("Play Time", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(TTSpacing.sm))
                        if (totalPlayMin > 0) {
                            Text(
                                if (totalPlayMin >= 60) "${totalPlayMin / 60}j ${totalPlayMin % 60}m" else "${totalPlayMin}m",
                                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black
                            )
                            Text("Total waktu bermain", color = SubText, fontSize = 10.sp)
                        } else {
                            Text("Belum main", color = SubText, fontSize = 11.sp)
                            Text("User belum track sesi game.", color = SubText, fontSize = 9.sp, lineHeight = 12.sp)
                        }
                    }
                }

                // ── Posts section header ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Article, null, tint = CandyCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(TTSpacing.xs))
                    Text("Post (${userPosts.size})",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }

                // ── Posts list (compact cards) ──
                if (userPosts.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = TTSpacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.Article, null, tint = SubText, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(TTSpacing.sm))
                        Text("Belum ada post dari user ini.",
                            color = SoftText, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(TTSpacing.md)) {
                        userPosts.forEach { post ->
                            ProfilePostCard(
                                post = post,
                                isDraft = false,
                                publishing = false,
                                onPublish = {},
                                onDelete = {},
                                onVisitProfile = null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(TTSpacing.xxl))
            }
        }
    }
}

// ─── Profile stat tile (Phase 2: TapTap-style, pakai TTShapes.cardLarge) ──────
@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    ok: Boolean,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        if (ok) NeonGreen else DangerRed,
        tween(400), label = "stat_tile_c_$label"
    )
    Card(
        modifier = modifier,
        shape = TTShapes.cardLarge,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, if (ok) NeonGreen.copy(0.25f) else DangerRed.copy(0.25f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(TTSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(color.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    null, tint = color, modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(TTSpacing.xs))
            Text(label, color = SubText, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
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
// expandedSection & onExpandedSectionChange di-lift dari ProfileScreen supaya
// avatar card bisa trigger expand "profile" section dari luar.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsCard(
    api: CommunityApi,
    context: android.content.Context,
    expandedSection: String? = null,
    onExpandedSectionChange: (String?) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
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
            onToggle = { onExpandedSectionChange(if (expandedSection == "password") null else "password"); resultMsg = "" }
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
            onToggle = { onExpandedSectionChange(if (expandedSection == "email") null else "email"); resultMsg = "" }
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
            onToggle = { onExpandedSectionChange(if (expandedSection == "profile") null else "profile"); resultMsg = "" }
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
            onToggle = { onExpandedSectionChange(if (expandedSection == "pin") null else "pin"); resultMsg = "" }
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
                        onExpandedSectionChange(null)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    // Phase 2: TTTappableCard style — press scale spring bounce on header row
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "setting_row_press"
    )
    Card(
        modifier = Modifier.fillMaxWidth().scale(scale),
        shape = TTShapes.button,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, if (expanded) CandyCyan.copy(0.5f) else GlassStroke),
        interactionSource = interactionSource,
        onClick = onToggle
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(TTSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(32.dp).background(CandyBlue.copy(0.12f), TTShapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(TTSpacing.md))
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
                Column(Modifier.padding(horizontal = TTSpacing.md).padding(bottom = TTSpacing.md), content = content)
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

// ─── App Update Popup ─────────────────────────────────────────────────────────
@Composable
fun AppUpdatePopup(
    info: AppUpdateChecker.UpdateInfo,
    downloading: Boolean,
    progress: Float,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!downloading) onLater() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, null, tint = Color.White, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Update Tersedia!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                if (!info.isPublished) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = AmberWarn.copy(0.15f),
                        border = BorderStroke(1.dp, AmberWarn.copy(0.4f)),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            "DRAFT",
                            color = AmberWarn,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    "Versi baru ${info.versionName} sudah tersedia.",
                    color = SoftText, fontSize = 13.sp
                )
                if (!info.isPublished) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = AmberWarn, modifier = Modifier.size(12.dp))
                        Text(
                            "Versi draft — belum dirilis ke publik. Hanya untuk testing.",
                            color = AmberWarn, fontSize = 10.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Release notes (truncate kalau terlalu panjang)
                val notes = info.releaseNotes.take(500)
                if (notes.isNotEmpty()) {
                    Text(
                        notes,
                        color = SubText, fontSize = 11.sp, lineHeight = 15.sp,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Download progress
                if (downloading) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Surface2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Mengunduh... ${(progress * 100).toInt()}%",
                        color = SoftText, fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                enabled = !downloading,
                shape = TTShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Carbon
                )
            ) {
                if (downloading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Carbon, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Mengunduh...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.SystemUpdate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Update Sekarang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLater,
                enabled = !downloading
            ) {
                Text("Nanti saja", color = SubText, fontSize = 13.sp)
            }
        },
        containerColor = GlassBase
    )
}

// ─── Helper functions ─────────────────────────────────────────────────────────

fun isGameInstalled(context: android.content.Context): Boolean =
    try { context.packageManager.getPackageInfo(GAME_PKG, 0); true } catch (_: Exception) { false }

fun readMarker(): String =
    try { File(MARKER_PATH).readText().trim() } catch (_: Exception) { "" }

/**
 * Cek apakah data FIFA 16 sudah siap. Mendeteksi MULTIPLE indicators:
 *
 * 1. Marker file `.dlavie26_data_installed` (kalau patch sudah diapply via DevPatchEngine).
 * 2. OBB main / patch file di /sdcard/Android/obb/com.ea.gp.fifaworld/.
 * 3. Folder game data /sdcard/Android/data/com.ea.gp.fifaworld/ exists & punya konten.
 * 4. Subfolder files/ punya konten (FIFA sering download data ke sini).
 *
 * Data dianggap ready kalau SALAH SATU indicator terpenuhi. Ini fix bug
 * "Data: Belum siap" untuk user yang baru install game + download OBB
 * dari dalam game (tidak punya marker file dari DevPatchEngine).
 */
fun isDataReady(): Boolean {
    // 1. Marker file (v26 marker via DevPatchEngine)
    if (readMarker().startsWith("v26", ignoreCase = true)) return true

    // 2. OBB files (main / patch)
    val obbMain  = File("/sdcard/Android/obb/com.ea.gp.fifaworld/main.13.com.ea.gp.fifaworld.obb")
    val obbPatch = File("/sdcard/Android/obb/com.ea.gp.fifaworld/patch.26.com.ea.gp.fifaworld.obb")
    if (obbMain.exists() || obbPatch.exists()) return true

    // 3. Game data folder exists dan punya konten
    val gameDataDir = File("/sdcard/Android/data/com.ea.gp.fifaworld")
    if (gameDataDir.exists() && (gameDataDir.listFiles()?.isNotEmpty() == true)) return true

    // 4. files/ subfolder punya konten (FIFA sering download data ke sini)
    val filesDir = File("/sdcard/Android/data/com.ea.gp.fifaworld/files")
    if (filesDir.exists() && (filesDir.listFiles()?.isNotEmpty() == true)) return true

    return false
}

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
            // Normalize image_url (treat null/blank/"null" string as empty)
            imageUrl  = o.optString("image_url", "").let { raw ->
                val s = raw.trim()
                if (s.isBlank() || s.equals("null", ignoreCase = true)) "" else s
            },
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

/**
 * Global in-memory tracker for the current game session start time.
 *
 * Flow:
 *   1. User taps "Mainkan" → [launchGame] sets [startTimeMs] = System.currentTimeMillis()
 *      and then opens FIFA 16 (which moves our activity to the background).
 *   2. User returns to the launcher → activity ON_RESUME fires → MainShell's lifecycle
 *      observer reads [startTimeMs], computes duration in minutes, calls
 *      [CommunityApi.recordGameSession] + [CommunityApi.checkAndAwardBadges],
 *      and resets [startTimeMs] to 0L.
 *
 * Lives in-memory only (no persistence) — if the process is killed mid-session we
 * lose the start time, which is fine: an unterminated session shouldn't be billed
 * as play time anyway.
 */
object GameSessionTracker {
    @Volatile
    var startTimeMs: Long = 0L

    fun start() { startTimeMs = System.currentTimeMillis() }

    fun consume(): Long {
        val v = startTimeMs
        startTimeMs = 0L
        return v
    }
}

fun launchGame(context: android.content.Context, gamePackage: String = GAME_PKG_16, mainActivity: String? = null) {
    GameSessionTracker.start()
    Telemetry.track(context, Telemetry.EVT_GAME_LAUNCH, mapOf("game_package" to gamePackage))
    try {
        val appCtx = context.applicationContext
        Thread {
            runCatching {
                val api = CommunityApi(appCtx)
                if (api.loggedIn()) api.logActivity("game_launch", null)
            }
        }.start()
    } catch (_: Throwable) { }

    val intent = android.content.Intent().apply {
        if (mainActivity != null) {
            setClassName(gamePackage, mainActivity)
        } else {
            setClassName(gamePackage, "com.byfen.downloadzipsdk.MainActivity")
        }
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallbackIntent = context.packageManager.getLaunchIntentForPackage(gamePackage)
        if (fallbackIntent != null) {
            context.startActivity(fallbackIntent)
        } else {
            // PRIVACY: Don't expose GitHub repo URL. Use DLavie proxy instead.
            val proxyUrl = if (gamePackage == GAME_PKG_15) (DLAVIE_PROXY_URL + "?f=fifa15-apk") else (DLAVIE_PROXY_URL + "?f=launcher-latest")
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(proxyUrl)))
        }
    }
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

/**
 * Upload FCM token to Supabase user_fcm_tokens table.
 * Idempotent: uses upsert on fcm_token (unique constraint).
 * Best-effort: silently fails if Supabase returns error (table missing, RLS, etc).
 *
 * Called when:
 * - Launcher opens (DLavieModernApp LaunchedEffect)
 * - User logs in (after successful auth)
 * - Token refreshes (DLavieFirebaseMessagingService.onNewToken via SharedPreferences)
 */
fun uploadFcmTokenToSupabase(api: CommunityApi, fcmToken: String) {
    try {
        val result = api.registerFcmToken(fcmToken)
        android.util.Log.d("DLavieFCM", "FCM token uploaded: $result")
    } catch (e: Exception) {
        android.util.Log.w("DLavieFCM", "FCM token upload error: ${e.message}", e)
    }
}

/**
 * FCM Diagnostic Card — displays real-time FCM token + upload status on screen.
 * No laptop/ADB needed. User can see exactly what's happening with push setup.
 *
 * Shows:
 * - FCM token (or "Belum didapat" if Firebase hasn't returned one yet)
 * - Login status (logged in user ID)
 * - Upload status (idle / uploading / success / failed + error message)
 * - Last attempt timestamp
 *
 * Auto-refreshes every 3 seconds.
 */
@Composable
fun FcmDiagnosticCard(api: CommunityApi, context: android.content.Context) {
    var fcmToken by remember { mutableStateOf("") }
    var uploadStatus by remember { mutableStateOf("idle") }  // idle | uploading | success | failed
    var uploadError by remember { mutableStateOf("") }
    var lastAttempt by remember { mutableStateOf("") }
    var attemptCount by remember { mutableStateOf(0) }

    // Auto-refresh every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            // Read cached FCM token from SharedPreferences
            val cachedToken = context.getSharedPreferences("dlavie_fcm", android.content.Context.MODE_PRIVATE)
                .getString("fcm_token", "") ?: ""
            if (cachedToken.isNotEmpty() && cachedToken != fcmToken) {
                fcmToken = cachedToken
            }

            // If logged in and have token but not yet uploaded → attempt upload
            val loggedIn = api.loggedIn()
            if (loggedIn && fcmToken.isNotEmpty() && uploadStatus != "success" && uploadStatus != "uploading") {
                attemptCount++
                uploadStatus = "uploading"
                uploadError = ""
                lastAttempt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())

                withContext(Dispatchers.IO) {
                    try {
                        // Use CommunityApi.registerFcmToken() — same infrastructure as other API calls
                        // (handles 401 refresh, network errors, etc. — no more "Socket is closed")
                        api.registerFcmToken(fcmToken)
                        uploadStatus = "success"
                        uploadError = ""
                    } catch (e: Exception) {
                        uploadStatus = "failed"
                        uploadError = e.message ?: e.toString()
                    }
                }
            }

            delay(3_000L)
        }
    }

    GlassCard {
        TTSectionHeader(title = "Notifikasi Push (FCM)", icon = Icons.Rounded.Notifications)
        Spacer(Modifier.height(TTSpacing.sm))

        // Login status
        val loggedIn = api.loggedIn()
        ProfRow("Login", if (loggedIn) "Ya (${api.userId().take(8)}...)" else "Tidak")

        // FCM token
        val tokenDisplay = if (fcmToken.isEmpty()) "Belum didapat" else "${fcmToken.take(20)}... (${fcmToken.length} chars)"
        ProfRow("FCM Token", tokenDisplay)

        // Upload status with color
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Status Upload", color = SoftText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            val statusColor = when (uploadStatus) {
                "success" -> DLavieGlass.AuroraMint
                "uploading" -> DLavieGlass.AuroraCyan
                "failed" -> DLavieGlass.AuroraCoral
                else -> DLavieGlass.TextMuted
            }
            Text("● $uploadStatus", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Attempt count + last attempt time
        if (attemptCount > 0) {
            ProfRow("Percobaan", "$attemptCount kali")
            ProfRow("Terakhir", lastAttempt)
        }

        // Error message (if any)
        if (uploadError.isNotEmpty()) {
            Spacer(Modifier.height(TTSpacing.xs))
            Surface(
                color = DLavieGlass.AuroraCoral.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, DLavieGlass.AuroraCoral.copy(alpha = 0.40f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uploadError,
                    color = DLavieGlass.AuroraCoral,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Help text
        if (!loggedIn) {
            Spacer(Modifier.height(TTSpacing.xs))
            Text(
                "⚠ Login dulu supaya token ter-upload ke server.",
                color = AmberWarn,
                fontSize = 11.sp
            )
        } else if (fcmToken.isEmpty()) {
            Spacer(Modifier.height(TTSpacing.xs))
            Text(
                "⏳ Menunggu Firebase generate token... Tunggu 10-30 detik.",
                color = DLavieGlass.AuroraCyan,
                fontSize = 11.sp
            )
        } else if (uploadStatus == "success") {
            Spacer(Modifier.height(TTSpacing.xs))
            Text(
                "✅ Token ter-upload! Push notification siap diterima.",
                color = DLavieGlass.AuroraMint,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Language Settings Card — shows current language + toggle to switch.
 * Auto-detects device language on first launch.
 * User can manually override with English/Indonesian toggle.
 */
@Composable
fun LanguageSettingsCard(context: android.content.Context) {
    var currentLang by remember { mutableStateOf(LanguageManager.getCurrentLanguage(context)) }
    var isAuto by remember { mutableStateOf(LanguageManager.isAutoDetected(context)) }
    val haptic = LocalHapticFeedback.current

    GlassCard {
        TTSectionHeader(title = "Bahasa", icon = Icons.Rounded.Language)
        Spacer(Modifier.height(TTSpacing.sm))

        // Current language + mode
        ProfRow("Bahasa Saat Ini", LanguageManager.getCurrentLanguageName(context))
        ProfRow("Mode", if (isAuto) "Auto (mengikuti perangkat)" else "Manual")
        Spacer(Modifier.height(TTSpacing.md))

        // Language selection buttons — always visible
        Text("Pilih Bahasa", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
        Spacer(Modifier.height(TTSpacing.xs))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)
        ) {
            LanguageManager.getSupportedLanguages().forEach { lang ->
                val isSelected = currentLang == lang.code
                Surface(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            LanguageManager.setLanguage(context, lang.code)
                            currentLang = lang.code
                            isAuto = false
                            // Recreate activity to apply language immediately
                            (context as? android.app.Activity)?.recreate()
                        },
                    color = if (isSelected) TextWhite.copy(alpha = 0.15f) else Surface1,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) TextWhite.copy(alpha = 0.5f) else GlassStroke
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Language,
                            contentDescription = null,
                            tint = if (isSelected) TextWhite else SubText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            lang.nativeName,
                            color = if (isSelected) TextWhite else SoftText,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }

        // Auto-detect reset button (only show when user has manually selected)
        if (!isAuto) {
            Spacer(Modifier.height(TTSpacing.sm))
            Surface(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        LanguageManager.resetToAutoDetect(context)
                        currentLang = LanguageManager.getCurrentLanguage(context)
                        isAuto = true
                        // Recreate activity to apply language immediately
                        (context as? android.app.Activity)?.recreate()
                    },
                color = Surface1,
                border = BorderStroke(1.dp, GlassStroke),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, null, tint = SubText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gunakan bahasa perangkat (Auto)", color = SubText, fontSize = 12.sp, fontFamily = InterFontFamily)
                }
            }
        }

        Spacer(Modifier.height(TTSpacing.xs))
        Text(
            "Restart aplikasi untuk menerapkan perubahan bahasa.",
            color = SubText,
            fontSize = 10.sp,
            fontFamily = InterFontFamily
        )
    }
}

/**
 * Upload Android version to profiles table (for Dev Dashboard Users tab).
 * Best-effort — silently fails if error.
 */
fun uploadAndroidVersion(api: CommunityApi) {
    try {
        val androidVersion = android.os.Build.VERSION.RELEASE ?: return
        val userId = api.userId()
        if (userId.isEmpty()) return

        val payload = org.json.JSONObject().apply {
            put("android_version", androidVersion)
        }

        val url = java.net.URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/profiles?id=eq.${userId}")
        val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray()) }
        val code = conn.responseCode
        conn.disconnect()
        if (code in 200..299) {
            android.util.Log.d("DLavieFCM", "Android version uploaded: $androidVersion")
        }
    } catch (e: Exception) {
        android.util.Log.w("DLavieFCM", "Android version upload failed (ignored): ${e.message}")
    }
}

// ─── GameHub Screen (PS5-style landscape game library) ──────────────────────
@Composable
fun GameHubScreen(
    onNav: (Page) -> Unit,
    onGameClick: (String) -> Unit
) {
    val context = LocalContext.current
    val games = remember {
        listOf(
            GameItem(
                title = "FIFA 16 Mobile",
                subtitle = "DLavie 26 Mod · Sports",
                packageName = GAME_PKG_16,
                mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                coverText = "DL",
                coverImageRes = R.drawable.fifa16_cover  // v7.0.3: real cover art
            ),
            GameItem(
                title = "FIFA 15 Mobile",
                subtitle = "DLavie 15 Mod · Sports",
                packageName = GAME_PKG_15,
                mainActivity = FIFA15_MAIN_ACTIVITY,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15",
                coverImageRes = R.drawable.fifa15_cover  // v7.0.3: real cover art
            )
        )
    }

    // ── Floating action panel state ──
    // When user taps a game card, show floating panel (ala Kickstarter) with steps:
    // 1. Install APK → 2. Install Data → 3. Apply Mod (FIFA 16 only) → 4. Play
    var activeGame by remember { mutableStateOf<GameItem?>(null) }

    Box(Modifier.fillMaxSize().background(PureBlack)) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.SportsEsports, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("GameHub", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    Text("${games.size} games available — tap to install & play", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontFamily = InterFontFamily)
                }
            }

            // Game cards (horizontal scroll, landscape-style)
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(games) { game ->
                    GameCard(
                        game = game,
                        onClick = {
                            // v7.0.7: Show floating action panel instead of directly launching
                            activeGame = game
                        }
                    )
                }
            }

            // Recently played / All games section
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(width = 3.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                Spacer(Modifier.width(8.dp))
                Text("All Games", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
            Spacer(Modifier.height(12.dp))

            // Game list (vertical)
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(games) { game ->
                    GameListItem(
                        game = game,
                        onClick = {
                            // v7.0.7: Show floating action panel
                            activeGame = game
                        }
                    )
                }
            }
        }

        // ── Floating Action Panel (overlay) ──
        // Shown when user taps a game card. Tap background to dismiss.
        activeGame?.let { game ->
            GameActionPanel(
                game = game,
                onDismiss = { activeGame = null },
                onGoToDlc = {
                    activeGame = null
                    onNav(Page.DLC)
                }
            )
        }
    }
}

@Composable
private fun GameCard(game: GameItem, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Box(Modifier.fillMaxSize()) {
            // v7.0.3: Cover image (real cover art) OR gradient fallback
            if (game.coverImageRes != null) {
                coil.compose.AsyncImage(
                    model = game.coverImageRes,
                    contentDescription = game.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Scrim overlay for text readability (bottom gradient)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
            } else {
                // Fallback: gradient + text
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(game.coverGradient)
                    )
                )
                Box(
                    Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        game.coverText,
                        color = Color.White.copy(alpha = 0.15f),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                }
            }
            // Bottom info
            Column(
                Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Text(game.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
                Text(game.subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Text("Play", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun GameListItem(game: GameItem, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // v7.0.3: Cover thumbnail — real image OR gradient fallback
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(game.coverGradient)),
            contentAlignment = Alignment.Center
        ) {
            if (game.coverImageRes != null) {
                coil.compose.AsyncImage(
                    model = game.coverImageRes,
                    contentDescription = game.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(game.coverText, color = Color.White.copy(alpha = 0.3f), fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(game.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
            Text(game.subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

data class GameItem(
    val title: String,
    val subtitle: String,
    val packageName: String,
    val mainActivity: String,
    val coverGradient: List<Color>,
    val coverText: String,
    val coverImageRes: Int? = null  // v7.0.3: real cover image (nullable — fallback to gradient + text)
)
