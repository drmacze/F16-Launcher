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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PrivacyTip
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
import androidx.compose.material.icons.rounded.Logout
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
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.draw.shadow
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
//
// ⚠️  CRITICAL: FIFA16_APK_URL must point to ?f=fifa16-apk (the FIFA 16 GAME APK),
//     NOT ?f=launcher-latest (the DLavie Launcher APK). Pointing to launcher-latest
//     causes infinite download loop because user installs launcher (same package)
//     instead of FIFA 16 game → FIFA 16 never detected as installed.
const val GAME_PKG_16       = "com.ea.gp.fifaworld"
// v7.7.0: APK FIFA 16 ORIGINAL dari ChatGPT (DLavie26.apk) — hosted di GitHub
// DLavie-Launcher-Data release v26. APK TIDAK dimodify (signature original,
// targetSdk=26 native). Compatible Android 7-16 (verified working on Android 16).
// Repackaging APK akan break signature → "App not installed as app isn't
// compatible with your phone" error (lihat IMG_4623.png). JANGAN repack APK.
// OBB/Data/Manifest juga dari release v26 (original).
// SHA-256: acb0ce50554d13d6d36aa75e7e84ade69e52f4b130f8316af4505cc255acd176
const val DLAVIE_PROXY_URL  = "https://lvmucsxbmadtsgrxuwmo.supabase.co/functions/v1/apk-proxy"
const val DLAVIE_DATA_BASE  = "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26"
const val FIFA16_APK_URL    = "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/DLavie26.apk"
const val FIFA16_MANIFEST   = "${DLAVIE_DATA_BASE}/manifest.json"
const val FIFA16_OBB_MAIN   = "${DLAVIE_DATA_BASE}/main.13.com.ea.gp.fifaworld.obb"
const val FIFA16_OBB_PATCH  = "${DLAVIE_DATA_BASE}/patch.26.com.ea.gp.fifaworld.obb"
const val LAUNCHER_APK_URL  = "https://github.com/drmacze/F16-Launcher/releases/latest/download/DLavie26-Launcher-debug.apk"
const val MARKER_PATH_16    = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"

// FIFA 15 (DLavie 15)
const val GAME_PKG_15       = "com.ea.game.fifa14_row"
const val FIFA15_APK_URL    = "https://github.com/drmacze/F15/releases/download/v2.1.8/DLavie15-Android16-Compatible.apk"
const val FIFA15_DATA_URL   = "${DLAVIE_PROXY_URL}?f=fifa15-data"
const val FIFA15_OBB_URL    = "${DLAVIE_PROXY_URL}?f=fifa15-obb"
const val MARKER_PATH_15    = "/sdcard/Android/data/com.ea.game.fifa14_row/.dlavie15_data_installed"
const val FIFA15_MAIN_ACTIVITY = "com.ea.game.fifa14.Fifa14Activity"

// Legacy aliases (for existing code that references these)
private const val GAME_PKG          = GAME_PKG_16
private const val FIFA_APK_URL      = FIFA16_APK_URL
private const val MARKER_PATH       = MARKER_PATH_16

private const val DEFAULT_MANIFEST = FIFA16_MANIFEST
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

    // ── DLavie Portal Connect: deep link handler ──
    // v7.9.53: Handle BOTH flows:
    //   OLD flow (v7.x): dlavie://connect?callback=URL — launcher kirim token ke web
    //   NEW flow (v8.0+): dlavie://connect?token=JWT&uid=USER_ID&refresh=REFRESH — web kirim token ke launcher
    //
    // ModernLauncherActivity adalah entry point deep link (registered di AndroidManifest).
    // ShinySplashActivity juga handle new flow, tapi hanya kalau launcher dibuka fresh
    // via deep link. Kalau launcher sudah running (singleTop), deep link masuk ke
    // onNewIntent ModernLauncherActivity → juga harus handle.
    private fun handlePortalConnectIntent(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme != "dlavie" || data.host != "connect") return

        // ── NEW FLOW: web kirim token ke launcher ──
        // dlavie://connect?token=JWT&uid=USER_ID&refresh=REFRESH
        val webToken = data.getQueryParameter("token")
        val webUid = data.getQueryParameter("uid")
        val webRefresh = data.getQueryParameter("refresh") ?: ""

        if (!webToken.isNullOrBlank() && !webUid.isNullOrBlank()) {
            // v7.9.60 FIX: Proper account connection dengan profile loading
            // Sebelumnya: loadMyProfile() gagal silently → profile kosong → user lihat "DLavie Player"
            // Sekarang: clear old data → save new token → load OR create profile → log errors
            connectPortalAccount(webToken, webUid, webRefresh)

            // v7.9.58 FIX: Clear intent data supaya tidak re-process setelah recreate()
            setIntent(Intent(this, ModernLauncherActivity::class.java))

            // Re-render UI supaya profile card muncul (bukan halaman login)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                recreate()
            }, 800)
            return
        }

        // ── OLD FLOW: launcher kirim token ke web (callback URL) ──
        // dlavie://connect?callback=URL
        val api = CommunityApi(this)
        val callback = data.getQueryParameter("callback") ?: return

        if (!api.loggedIn()) {
            android.widget.Toast.makeText(
                this,
                "Silakan login dulu di launcher, lalu coba Connect lagi dari web.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        // Show "DLavie Portal Connected" status
        android.widget.Toast.makeText(
            this,
            "✓ DLavie Portal Connected!\nMengalihkan kembali ke web…",
            android.widget.Toast.LENGTH_LONG
        ).show()

        // Delay 1.5s so user can see the status, then redirect back to web
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val token = api.token()
                val uid = api.userId()
                val redirectUrl = "$callback?token=$token&uid=$uid"

                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(redirectUrl)
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(browserIntent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this,
                    "Gagal redirect ke web: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }, 1500)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update intent so subsequent DLavieModernApp reads pick up the new post_id.
        setIntent(intent)
        // v7.9.53: Handle deep link saat launcher sudah running (singleTop)
        // Kalau user klik Connect to DLavie di web saat launcher sudah terbuka,
        // deep link masuk ke onNewIntent, bukan onCreate.
        handlePortalConnectIntent(intent)
    }

    // ════════════════════════════════════════════════════════════════════
    // v7.9.60: connectPortalAccount — Proper account connection dengan profile loading
    //
    // BUG SEBELUMNYA:
    // - loadMyProfile() gagal silently (catch (_: Exception) {})
    // - Profile tidak ter-save → displayName() return empty → "DLavie Player" fallback
    // - User lihat profile kosong padahal sudah connect dengan akun yang valid
    //
    // ROOT CAUSE:
    // - Profile belum ada di `profiles` table (user register via web Supabase Auth,
    //   tapi trigger handle_new_user belum create profile, atau RLS block read)
    // - Network error saat fetch profile
    // - Exception di-swallow, tidak ada fallback
    //
    // FIX:
    // 1. Clear prefs lama (guest data, profile dari akun berbeda) sebelum save token baru
    // 2. Decode JWT untuk dapat email (untuk default display_name + username)
    // 3. Save token + uid + email + default display_name + username
    // 4. loadMyProfile() dengan fallback:
    //    - Kalau gagal "Profile community belum tersedia" → call ensureMyProfile()
    //    - Kalau gagal network → log error, tetap navigate (user bisa refresh nanti)
    // 5. Log semua error supaya tidak silent fail
    // ════════════════════════════════════════════════════════════════════
    private fun connectPortalAccount(token: String, uid: String, refresh: String) {
        val TAG = "DLavieConnect"

        // Step 1: Decode JWT untuk dapat email
        var email = ""
        try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payload = parts[1]
                val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
                val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
                val jwt = org.json.JSONObject(String(decoded))
                email = jwt.optString("email", "")
                android.util.Log.i(TAG, "JWT decoded: uid=$uid, email=$email")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to decode JWT: ${e.message}")
        }

        // Step 2: Clear prefs lama (guest data, profile dari akun berbeda)
        // INI PENTING: kalau tidak di-clear, profile lama bisa muncul (akun sebelumnya)
        val communityPrefs = getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE)
        val authPrefs = getSharedPreferences("dlavie_auth_session", android.content.Context.MODE_PRIVATE)

        // Preserve accounts list (multi-account switcher) tapi clear profile data lama
        val savedAccounts = communityPrefs.getString("accounts", "[]")
        val savedActiveId = communityPrefs.getString("active_user_id", "")

        communityPrefs.edit().clear().apply()
        authPrefs.edit().clear().apply()

        // Restore accounts list
        communityPrefs.edit()
            .putString("accounts", savedAccounts)
            .putString("active_user_id", savedActiveId)
            .apply()

        // Step 3: Save token + uid + email ke both prefs
        authPrefs.edit()
            .putString("access_token", token)
            .putString("refresh_token", refresh)
            .apply()

        // Default display_name + username dari email (sebelum profile ter-load)
        val defaultDisplayName = if (email.isNotEmpty()) {
            email.substringBefore("@").replace(".", " ").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        } else "DLavie Player"
        val defaultUsername = if (email.isNotEmpty()) {
            email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "_").take(20)
        } else "user_${uid.take(6)}"

        communityPrefs.edit()
            .putString("access_token", token)
            .putString("refresh_token", refresh)
            .putString("user_id", uid)
            .putString("email", email)
            .putString("display_name", defaultDisplayName)
            .putString("username", defaultUsername)
            .putString("role", "member")
            .putBoolean("portal_connected", true)
            .putString("portal_connected_at", System.currentTimeMillis().toString())
            .putBoolean("is_guest", false)
            .apply()

        android.util.Log.i(TAG, "Token saved: uid=$uid, email=$email, defaultName=$defaultDisplayName")

        // Step 4: Load profile dengan fallback ke ensureMyProfile
        val api = CommunityApi(this)
        var profileLoaded = false
        var displayName = defaultDisplayName

        try {
            val profile = api.loadMyProfile()
            profileLoaded = true
            displayName = api.displayName().ifEmpty { defaultDisplayName }
            android.util.Log.i(TAG, "Profile loaded: username=${api.username()}, name=${api.displayName()}")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "loadMyProfile failed: ${e.message}")

            // Fallback: kalau profile belum ada, create dengan default values
            if (e.message?.contains("Profile community belum tersedia") == true ||
                e.message?.contains("belum ada") == true) {
                try {
                    android.util.Log.i(TAG, "Profile belum ada, mencoba ensureMyProfile...")
                    api.ensureMyProfile(defaultUsername, defaultDisplayName, null)
                    try {
                        api.loadMyProfile()
                        profileLoaded = true
                        displayName = api.displayName().ifEmpty { defaultDisplayName }
                        android.util.Log.i(TAG, "Profile created + loaded: ${api.displayName()}")
                    } catch (e2: Exception) {
                        android.util.Log.w(TAG, "loadMyProfile after ensure masih gagal: ${e2.message}")
                    }
                } catch (e2: Exception) {
                    android.util.Log.e(TAG, "ensureMyProfile juga gagal: ${e2.message}")
                }
            }
        }

        // Step 5: Show toast
        android.widget.Toast.makeText(
            this,
            "✓ DLavie Portal Connected! Welcome, $displayName.",
            android.widget.Toast.LENGTH_LONG
        ).show()

        android.util.Log.i(TAG, "connectPortalAccount selesai: profileLoaded=$profileLoaded, name=$displayName")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlePortalConnectIntent(intent)
        // Pre-create notification channel (idempotent) so the channel is ready
        // before any local notification fires (Android O+).
        NotificationHelper.createChannel(this)
        setContent { DLavieModernApp(initialPostId = intent?.getStringExtra("post_id")) }
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
    var updateDownloadError by remember { mutableStateOf("") }  // v7.2.8: error state (no browser fallback)
    var manualCheckLoading by remember { mutableStateOf(false) }
    var manualCheckMessage by remember { mutableStateOf("") }
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
    // v7.9.42: Auto-reset dismissed_version_code kalau ada versi baru
    // Sebelumnya: kalau user dismiss v208, popup v209/v210 tidak muncul (BUG)
    // Sekarang: dismissed hanya berlaku untuk versi yang sama. Versi baru tetap show popup.
    val updatePrefs = remember { context.getSharedPreferences("dlavie_update_prefs", Context.MODE_PRIVATE) }

    /**
     * v7.9.42: Manual check update logic — dipanggil dari tombol "Cek Update" di DLC tab.
     * Force check ke server, ignore dismissed_version_code, tampilkan popup kalau ada update.
     * Implementasi inline di DlcScreen call site (lihat Page.DLC di MainShell).
     */

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val info = AppUpdateChecker.checkForUpdate(api)
                if (info != null && info.isUpdateAvailable) {
                    // v7.9.42: Auto-reset dismissed kalau versi baru tersedia
                    // Jadi kalau user dismiss v208, popup v210 tetap muncul
                    val dismissedVersion = updatePrefs.getInt("dismissed_version_code", -1)
                    if (dismissedVersion != info.versionCode) {
                        updateInfo = info
                        showUpdatePopup = true
                    } else {
                        android.util.Log.i("AppUpdate", "Update v${info.versionCode} tersedia tapi user sudah dismiss. Manual check untuk force show.")
                    }
                }
            }.onFailure { e ->
                android.util.Log.w("AppUpdate", "Auto check failed (non-fatal)", e)
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
                    // v7.9.29: Remove current account from list, then logout
                    val currentUserId = api.userId()
                    if (currentUserId.isNotEmpty()) {
                        AccountStore.removeAccount(context, currentUserId)
                    }
                    api.logout()
                    context.getSharedPreferences("dlavie_auth_session", Context.MODE_PRIVATE).edit().clear().apply()
                    PinManager.clearPin(context)
                    // v7.9.29: If there are remaining accounts, switch to first one instead of going to login
                    val remaining = AccountStore.listAccounts(context)
                    if (remaining.isNotEmpty()) {
                        AccountStore.switchAccount(context, remaining[0].userId)
                        context.startActivity(
                            Intent(context, ModernLauncherActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } else {
                        context.startActivity(
                            Intent(context, DLavieGuidedActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }

                // ── App Update Popup ──
                if (showUpdatePopup && updateInfo != null) {
                    AppUpdatePopup(
                        info = updateInfo!!,
                        downloading = updateDownloading,
                        progress = updateDownloadProgress,
                        error = updateDownloadError,
                        onUpdate = {
                            if (!updateDownloading) {
                                val currentInfo = updateInfo
                                if (currentInfo == null || currentInfo.apkUrl.isBlank()) {
                                    // Tidak ada URL download — buka browser ke halaman release
                                    try {
                                        // PRIVACY: Don't expose GitHub repo URL. Use DLavie proxy instead.
                                        val fallbackUrl = currentInfo?.apkUrl?.takeIf { it.isNotBlank() } ?: (LAUNCHER_APK_URL)
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                                        browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(browserIntent)
                                    } catch (_: Throwable) { }
                                    showUpdatePopup = false
                                    return@AppUpdatePopup
                                }
                                updateDownloading = true
                                updateDownloadProgress = 0f
                                updateDownloadError = ""
                                updateScope.launch {
                                    try {
                                        val apkFile = withContext(Dispatchers.IO) {
                                            AppUpdateChecker.downloadApk(context, currentInfo.apkUrl) { progress ->
                                                updateDownloadProgress = progress
                                            }
                                        }
                                        updateDownloading = false
                                        if (apkFile != null && apkFile.exists() && apkFile.length() > 0) {
                                            // v7.5.4: grantUriPermission untuk Android 13+
                                            runCatching {
                                                context.grantUriPermission(
                                                    "com.android.packageinstaller",
                                                    androidx.core.content.FileProvider.getUriForFile(
                                                        context, "${context.packageName}.files", apkFile
                                                    ),
                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                )
                                            }
                                            val installed = AppUpdateChecker.installApk(context, apkFile)
                                            if (!installed) {
                                                updateDownloadError = "Unable to open installer. Please try again."
                                            }
                                        } else {
                                            updateDownloadError = "Download failed. Please try again."
                                        }
                                    } catch (e: Throwable) {
                                        updateDownloading = false
                                        updateDownloadError = e.message ?: "Download failed. Check your connection."
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
                    // ── Belum login DAN bukan guest → redirect ke guided login ──
                    // v7.2.5: Fix guest redirect loop — guest tidak punya token,
                    // tapi isGuest() return true. Jangan redirect guest ke login.
                    !api.loggedIn() && !api.isGuest() -> {
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
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))
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
    // v7.9.3: Selected GameItem for GameDetailScreen — supports FIFA 16 AND FIFA 15.
    // Di-set saat user klik game card di GameHub atau Beranda. Default: FIFA 16.
    var detailGameItem          by remember { mutableStateOf<GameItem?>(null) }
    // v7.9.3: Show GameActionPanel dari GameDetailScreen (untuk install flow).
    var detailShowActionPanel   by remember { mutableStateOf(false) }
    // v7.9.34: Save game auto-backup sebelum play
    var showSaveLabelDialog     by remember { mutableStateOf(false) }
    var saveLabelInput          by remember { mutableStateOf("") }
    var pendingGameLaunch       by remember { mutableStateOf<GameItem?>(null) }

    // ── Phase 4: Settings overlay state + lifted Profile expand state ──
    // profileExpandedSection di-lift ke MainShell supaya SettingsScreen bisa
    // membuka Profile dengan section tertentu (password/email/profile) ter-expand.
    var showSettings           by remember { mutableStateOf(false) }
    var showEditProfile        by remember { mutableStateOf(false) }
    var profileExpandedSection by remember { mutableStateOf<String?>(null) }
    // v7.9.17: Onboarding modal — show setelah login/register jika user_type masih kosong
    var showOnboardingModal    by remember { mutableStateOf(false) }

    // ── Visit Profile (Task 4): user ID being viewed in UserProfileScreen overlay.
    // null = not visiting anyone; non-null = overlay shown on top of current page.
    // Tapped from CommunityScreen search results or post author names.
    var visitingUserId         by remember { mutableStateOf<String?>(null) }

    // ── Phase 2: Lifted download state (shared antara HomeScreen & GameDetailScreen) ──
    // dlProgress: -1f = idle, 0f..0.99f = downloading, 2f = done (waiting install)
    var dlProgress by remember { mutableStateOf(-1f) }
    var dlError    by remember { mutableStateOf("") }

    // v7.9.57: Play Protect Install Dialog state
    var showPlayProtectInstall by remember { mutableStateOf(false) }

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

                // v7.3.4 FIX: Revert v7.3.2 uninstall logic yang merusak install flow.
                // Root cause "App not installed":
                //   1. ApkDownloader saves ke "apk-downloads/" TAPI file_paths.xml
                //      hanya expose "public-install/" → getUriForFile crash → install gagal
                //   2. v7.3.2 uninstall logic memaksa uninstall padahal user mungkin
                //      mau reinstall versi yang sama (signature sama, tidak perlu uninstall)
                //
                // Fix:
                //   - file_paths.xml: added apk-downloads/ + root path (done)
                //   - startDownload: kembali ke flow sederhana yang work sebelum v7.3.2
                //   - Added APK size validation (catch error pages)
                //   - Added grantUriPermission untuk Android 13+ strict mode

                // Verify APK file valid (minimal size check — < 1MB = probably error page)
                if (apkFile.length() < 1_000_000) {
                    dlError = "Download failed — file too small. Check your connection and try again."
                    dlProgress = -1f
                } else {
                    android.util.Log.d("DLavieInstall", "APK downloaded: ${apkFile.length()} bytes, launching installer...")

                    // Launch APK installer — same flow yang work di Android 7-15 sebelum v7.3.2
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, "${context.packageName}.files", apkFile)

                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        // v7.3.4: Grant URI permission ke package installer (Android 13+ strict)
                        runCatching {
                            context.grantUriPermission(
                                "com.android.packageinstaller",
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                        context.startActivity(installIntent)
                    } catch (e: Exception) {
                        android.util.Log.e("DLavieInstall", "Install launch failed", e)
                        dlError = "Unable to open installer. Try again or install manually."
                        dlProgress = -1f
                    }
                }
            }.onFailure { dlError = it.message ?: "Download failed. Check your internet connection."; dlProgress = -1f }
        }
    }

    // v7.9.57: Play Protect Install Dialog
    if (showPlayProtectInstall) {
        PlayProtectInstallDialog(
            onDismiss = { showPlayProtectInstall = false },
            onDownloadStart = {
                showPlayProtectInstall = false
                startDownload()
                detailShowActionPanel = true
            }
        )
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
                            // v7.9.3: Pass GameItem (FIFA 16 or FIFA 15). Default to FIFA 16 if null.
                            // v7.9.4: Default includes screenshots (real gameplay from user upload).
                            val currentGame = detailGameItem ?: GameItem(
                                title = "FIFA 16 Mobile",
                                subtitle = "DLavie 26 Mod · Sports",
                                packageName = GAME_PKG_16,
                                mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                                coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                                coverText = "DL",
                                coverImageRes = R.drawable.fifa16_cover,
                                serverStatus = ServerStatus.ONLINE,
                                description = "FIFA 16 Mobile dengan mod DLavie 26.",
                                version = "v26.0",
                                sizeMb = "34 MB",
                                category = "Olahraga",
                                ageRating = "9+",
                                lastUpdate = "5 Juli 2026",
                                features = listOf("Gameplay Realistis", "Roster Update 2025/2026", "Komunitas Aktif", "Update Rutin"),
                                screenshots = listOf(
                                    R.drawable.fifa16_screenshot_1,
                                    R.drawable.fifa16_screenshot_2,
                                    R.drawable.fifa16_screenshot_3,
                                    R.drawable.fifa16_screenshot_4
                                ),
                                apkUrl = FIFA16_APK_URL
                            )
                            GameDetailScreen(
                                game = currentGame,
                                onBack = { showGameDetail = false },
                                onPlay = {
                                    // v7.9.34: Auto-backup save sebelum play + cek server status
                                    scope.launch {
                                        when (currentGame.serverStatus) {
                                            ServerStatus.MAINTENANCE -> {
                                                android.widget.Toast.makeText(context, "${currentGame.title} sedang diperbaiki", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                            ServerStatus.OFFLINE -> {
                                                android.widget.Toast.makeText(context, "Server ${currentGame.title} sedang offline", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                            else -> {
                                                val pingMs = withContext(Dispatchers.IO) { pingGameServer(currentGame.packageName) }
                                                if (PingQuality.isWeakSignal(pingMs)) {
                                                    android.widget.Toast.makeText(context, "Kekuatan sinyalmu lemah, coba lagi nanti", android.widget.Toast.LENGTH_LONG).show()
                                                } else {
                                                    // v7.9.34: Auto-backup save game sebelum launch
                                                    val prefs = context.getSharedPreferences("dlavie_save_prefs", android.content.Context.MODE_PRIVATE)
                                                    val hasLabel = prefs.getBoolean("save_label_set", false)
                                                    if (!hasLabel) {
                                                        // First time — suruh user isi label save satu kali
                                                        pendingGameLaunch = currentGame
                                                        saveLabelInput = ""
                                                        showSaveLabelDialog = true
                                                    } else {
                                                        // Sudah pernah isi label — auto-backup + langsung play
                                                        val label = prefs.getString("save_label", "My Save") ?: "My Save"
                                                        withContext(Dispatchers.IO) {
                                                            val result = SaveGameManager.backupSave(context, 0, label)
                                                            if (result.success) {
                                                                android.util.Log.i("DLavie", "Auto-backup: ${result.message}")
                                                            }
                                                        }
                                                        launchGame(context, currentGame.packageName, currentGame.mainActivity)
                                                        showGameDetail = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onInstall = {
                                    // v7.9.57: Play Protect running sebelum install
                                    scope.launch {
                                        when (currentGame.serverStatus) {
                                            ServerStatus.MAINTENANCE -> {
                                                android.widget.Toast.makeText(context, "${currentGame.title} sedang diperbaiki", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                            ServerStatus.OFFLINE -> {
                                                android.widget.Toast.makeText(context, "Server ${currentGame.title} sedang offline", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                            else -> {
                                                val pingMs = withContext(Dispatchers.IO) { pingGameServer(currentGame.packageName) }
                                                if (PingQuality.isWeakSignal(pingMs)) {
                                                    android.widget.Toast.makeText(context, "Kekuatan sinyalmu lemah, coba lagi nanti", android.widget.Toast.LENGTH_LONG).show()
                                                } else {
                                                    showPlayProtectInstall = true
                                                }
                                            }
                                        }
                                    }
                                },
                                onDelete = {
                                    // v7.9.3: Uninstall game via PackageManager
                                    scope.launch {
                                        val ok = uninstallGame(context, currentGame.packageName)
                                        if (ok) {
                                            detailGameInstalled = false
                                            android.widget.Toast.makeText(context, "Game berhasil dihapus", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Gagal hapus game — uninstall manual dari Settings", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                gameInstalled      = detailGameInstalled,
                                avgRating          = detailAvgRating,
                                ratingCount        = detailRatingCount,
                                maintenanceBlocked = detailMaintenanceBlocked,
                                // v6.8.1: pass myRating supaya Rate button tahu state-nya.
                                hasRated           = detailMyRating > 0,
                                myRating           = detailMyRating,
                                onRate             = { rating ->
                                    // v7.9.3: onRate now receives rating directly (1-5) from star bar.
                                    if (api.isGuest()) {
                                        ratingSubmitError = t.loginToRateGuest
                                        showRatingPopup = true
                                    } else if (!api.loggedIn()) {
                                        ratingSubmitError = t.loginToRate
                                        showRatingPopup = true
                                    } else if (detailMyRating > 0) {
                                        // Sudah rate — silent ignore
                                    } else {
                                        // Submit rating directly (star bar already selected the value)
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { api.submitRating(rating, "") }
                                                android.util.Log.i("GameDetail", "Rating submitted: $rating")
                                                // Re-fetch stats untuk update aggregate
                                                val stats = withContext(Dispatchers.IO) { api.fetchRatingStats() }
                                                detailAvgRating   = stats.optDouble("avg", 0.0)
                                                detailRatingCount = stats.optInt("count", 0)
                                                detailMyRating    = rating
                                                android.util.Log.i("GameDetail", "Updated: avg=$detailAvgRating, count=$detailRatingCount")
                                            } catch (e: Exception) {
                                                android.util.Log.e("GameDetail", "submitRating failed", e)
                                                android.widget.Toast.makeText(context, "Gagal submit rating: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            )

                            // v7.9.3: GameActionPanel overlay (triggered from detail's Install button)
                            if (detailShowActionPanel) {
                                GameActionPanel(
                                    game = currentGame,
                                    onDismiss = { detailShowActionPanel = false },
                                    onGoToDlc = {
                                        detailShowActionPanel = false
                                        showGameDetail = false
                                        page = Page.DLC
                                    }
                                )
                            }
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
                                            // v7.2.9: Game card "Lihat" → buka GameDetailScreen (info + rating)
                                            // Install & play hanya di GameHub (bukan di Beranda)
                                            onGameCardClick = { inst, avg, count, blocked, myR ->
                                                detailGameInstalled      = inst
                                                detailAvgRating          = avg
                                                detailRatingCount        = count
                                                detailMaintenanceBlocked = blocked
                                                detailMyRating           = myR
                                                showGameDetail           = true
                                            }
                                        )
                                    Page.DLC -> DlcScreen(
                                        api,
                                        maintenanceInfo = maintenanceInfo,
                                        onNav  = { page = it }
                                    )
                                    Page.GameHub -> GameHubScreen(
                                            onNav = { page = it },
                                            onGameClick = { gamePackage ->
                                                // v7.9.13: Find GameItem by packageName, set as detail target.
                                                // v7.9.13 FIX: Fetch server status dari Supabase (jangan hardcode!).
                                                // Sebelumnya FIFA 16 di-hardcode ONLINE, FIFA 15 di-hardcode MAINTENANCE
                                                // → status di detail screen tidak sesuai dengan Supabase.
                                                // Sekarang fetch real-time dari app_config key='game_server_status'.
                                                scope.launch {
                                                    val baseGame = when (gamePackage) {
                                                        GAME_PKG_16 -> GameItem(
                                                            title = "FIFA 16 Mobile",
                                                            subtitle = "DLavie 26 Mod · Sports",
                                                            packageName = GAME_PKG_16,
                                                            mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                                                            coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                                                            coverText = "DL",
                                                            coverImageRes = R.drawable.fifa16_cover,
                                                            serverStatus = ServerStatus.ONLINE,  // placeholder, akan di-override
                                                            description = "FIFA 16 Mobile dengan mod DLavie 26 — gameplay yang ditingkatkan, roster update musim 2025/2026, " +
                                                                "dan fitur tambahan. Mainkan mode career, ultimate team, dan online match dengan komunitas DLavie.",
                                                            developer = "DLavie Company",
                                                            version = "v26.0",
                                                            sizeMb = "34 MB",
                                                            category = "Olahraga",
                                                            ageRating = "9+",
                                                            lastUpdate = "5 Juli 2026",
                                                            features = listOf("Gameplay Realistis", "Roster Update 2025/2026", "Komunitas Aktif", "Update Rutin"),
                                                            screenshots = listOf(
                                                                R.drawable.fifa16_screenshot_1,
                                                                R.drawable.fifa16_screenshot_2,
                                                                R.drawable.fifa16_screenshot_3,
                                                                R.drawable.fifa16_screenshot_4
                                                            ),
                                                            apkUrl = FIFA16_APK_URL
                                                        )
                                                        GAME_PKG_15 -> GameItem(
                                                            title = "FIFA 15 Mobile",
                                                            subtitle = "DLavie 15 Mod · Sports",
                                                            packageName = GAME_PKG_15,
                                                            mainActivity = FIFA15_MAIN_ACTIVITY,
                                                            coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                                                            coverText = "D15",
                                                            coverImageRes = R.drawable.fifa15_cover,
                                                            serverStatus = ServerStatus.MAINTENANCE,  // placeholder
                                                            description = "FIFA 15 Mobile dengan mod DLavie 15 — versi klasik dengan gameplay retro.",
                                                            developer = "DLavie Company",
                                                            version = "v15.0",
                                                            sizeMb = "22 MB",
                                                            category = "Olahraga",
                                                            ageRating = "9+",
                                                            lastUpdate = "4 Juli 2026",
                                                            features = listOf("Gameplay Klasik", "Roster 2014/2015", "Mode Nostalgia", "Android 16 Ready"),
                                                            apkUrl = FIFA15_APK_URL
                                                        )
                                                        else -> return@launch
                                                    }

                                                    // v7.9.13: Fetch REAL server status dari Supabase app_config
                                                    val realStatus = withContext(Dispatchers.IO) {
                                                        runCatching {
                                                            val config = api.getAppConfig("game_server_status")
                                                            val key = if (baseGame.packageName == GAME_PKG_16) "fifa16" else "fifa15"
                                                            val statusStr = config.optString(key, if (baseGame.packageName == GAME_PKG_16) "online" else "maintenance").lowercase()
                                                            when (statusStr) {
                                                                "online" -> ServerStatus.ONLINE
                                                                "maintenance" -> ServerStatus.MAINTENANCE
                                                                "offline" -> ServerStatus.OFFLINE
                                                                // v7.9.13: "busy" tidak lagi dari Supabase — auto-detect sinyal
                                                                else -> if (baseGame.packageName == GAME_PKG_16) ServerStatus.ONLINE else ServerStatus.MAINTENANCE
                                                            }
                                                        }.getOrDefault(if (baseGame.packageName == GAME_PKG_16) ServerStatus.ONLINE else ServerStatus.MAINTENANCE)
                                                    }

                                                    val game = baseGame.copy(serverStatus = realStatus)
                                                    detailGameItem = game
                                                    // Cek installed status
                                                    detailGameInstalled = try {
                                                        context.packageManager.getPackageInfo(game.packageName, 0); true
                                                    } catch (_: Throwable) { false }
                                                    // Fetch rating stats — v7.9.61: proper error handling, no swallow
                                                    try {
                                                        val stats = withContext(Dispatchers.IO) { api.fetchRatingStats() }
                                                        detailAvgRating   = stats.optDouble("avg", 0.0)
                                                        detailRatingCount = stats.optInt("count", 0)
                                                        android.util.Log.i("GameDetail", "Rating stats: avg=$detailAvgRating, count=$detailRatingCount")
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("GameDetail", "fetchRatingStats failed", e)
                                                    }
                                                    // Fetch my rating (hanya kalau login)
                                                    if (api.loggedIn()) {
                                                        try {
                                                            detailMyRating = withContext(Dispatchers.IO) { api.getMyRating() }
                                                            android.util.Log.i("GameDetail", "My rating: $detailMyRating")
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("GameDetail", "getMyRating failed", e)
                                                        }
                                                    }
                                                    // Cek maintenance — block install/play kalau maintenance atau offline
                                                    detailMaintenanceBlocked = game.serverStatus == ServerStatus.MAINTENANCE ||
                                                        game.serverStatus == ServerStatus.OFFLINE
                                                    showGameDetail = true
                                                }
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
                                        onOpenEditProfile       = { showEditProfile = true },
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

        // ── v3.1: Edit Profile overlay (separate from Settings) ──
        // Triggered by the "Edit Profile" button on the Profile screen.
        // Previously mis-wired to open Settings — now opens a real edit form.
        if (showEditProfile) {
            EditProfileScreen(
                api    = api,
                onBack = { showEditProfile = false }
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

        // ── v7.0.9 redesign: FloatingNav pill (PS5-style with elevated center play button) ──
        // v7.8.2: Tampilkan FloatingNav di SEMUA page (termasuk Chat/Komunitas).
        // Sebelumnya di-hidden di Chat karena menutupi FAB, tapi FAB sudah dihapus.
        // User butuh navbar untuk navigasi dari Komunitas ke page lain.
        // Tambah bottom padding di CommunityScreen supaya content tidak tertutup navbar.
        if (!showGameDetail && !showSettings && visitingUserId == null) {
            FloatingNav(
                page     = page,
                onPage   = { page = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)  // floating above bottom edge
            )
        }

        // ── Floating ChatBot (v6.4) — always visible when logged in ──
        // v7.8.2: Tampilkan di SEMUA page (tidak di-hidden di Chat).
        if (!showGameDetail && !showSettings && visitingUserId == null && api.loggedIn()) {
            FloatingChatBot(api = api)
        }

        // ── v7.9.17: Onboarding modal (post-login) ──────────────────────────
        // Show jika user sudah login TAPI user_type masih kosong (belum isi onboarding).
        // Cek dilakukan di MainShell supaya modal muncul di atas semua page.
        LaunchedEffect(api.loggedIn()) {
            if (api.loggedIn() && api.userType().isBlank()) {
                kotlinx.coroutines.delay(1500)  // delay supaya app sempat load
                showOnboardingModal = true
            }
        }
        if (showOnboardingModal && api.loggedIn() && api.userType().isBlank()) {
            OnboardingModal(
                api = api,
                onComplete = { showOnboardingModal = false }
            )
        }

        // v7.9.34: Save game label dialog — muncul sekali saat pertama kali klik Play
        if (showSaveLabelDialog) {
            AlertDialog(
                onDismissRequest = { showSaveLabelDialog = false; pendingGameLaunch = null },
                title = { Text("Save Game Label", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) },
                text = {
                    Column {
                        Text("Beri nama untuk save game Anda. Ini hanya diisi sekali.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontFamily = InterFontFamily)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = saveLabelInput,
                            onValueChange = { saveLabelInput = it },
                            placeholder = { Text("Contoh: Career Mode Saya", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val label = saveLabelInput.ifBlank { "My Save" }
                        // Save label ke prefs (hanya sekali)
                        context.getSharedPreferences("dlavie_save_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("save_label_set", true).putString("save_label", label).apply()
                        showSaveLabelDialog = false
                        // Auto-backup + launch game
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                SaveGameManager.backupSave(context, 0, label)
                            }
                            pendingGameLaunch?.let { game ->
                                launchGame(context, game.packageName, game.mainActivity)
                                showGameDetail = false
                            }
                            pendingGameLaunch = null
                        }
                    }) { Text("Simpan & Main", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Skip save, langsung main
                        showSaveLabelDialog = false
                        pendingGameLaunch?.let { game ->
                            launchGame(context, game.packageName, game.mainActivity)
                            showGameDetail = false
                        }
                        pendingGameLaunch = null
                    }) { Text("Lewati", color = Color.White.copy(alpha = 0.5f), fontFamily = InterFontFamily) }
                },
                containerColor = Carbon
            )
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
                            try {
                                api.submitRating(rating, review)
                                android.util.Log.i("GameDetail", "Rating submitted via popup: $rating")
                                val stats = api.fetchRatingStats()
                                detailAvgRating   = stats.optDouble("avg", 0.0)
                                detailRatingCount = stats.optInt("count", 0)
                                detailMyRating    = rating
                                android.util.Log.i("GameDetail", "Updated: avg=$detailAvgRating, count=$detailRatingCount")
                                true
                            } catch (e: Exception) {
                                android.util.Log.e("GameDetail", "submitRating popup failed", e)
                                ratingSubmitError = "Gagal: ${e.message}"
                                false
                            }
                        }
                        if (ok) {
                            ratingSubmitError = ""
                            showRatingPopup = false
                        } else {
                            ratingSubmitError = t.ratingFailed
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

    // ─── Layout per screenshot IMG_4449 ───────────────────────────────────────
    // • Pill-shaped navbar (rounded rectangle, full radius)
    // • White background with subtle shadow elevation
    // • 4 side buttons (2 left + 2 right) — icon + small label below
    // • Center button: solid BLACK circle, ELEVATED (overlaps top edge of navbar),
    //   with white PLAY/TRIANGLE icon (PS-style)
    // • Center button has its own shadow for depth
    Box(
        modifier = modifier
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp)
    ) {
        // ── Main pill bar ────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                Modifier
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Left side: first 2 non-center pages (Home, DLC) ──
                pages.filter { it != centerPage }.take(2).forEach { item ->
                    NavSideButton(item, page == item) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPage(item)
                    }
                }

                // ── Spacer for elevated center button ──
                Spacer(Modifier.width(64.dp))

                // ── Right side: remaining 2 non-center pages (Chat, Me) ──
                pages.filter { it != centerPage }.drop(2).forEach { item ->
                    NavSideButton(item, page == item) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPage(item)
                    }
                }
            }
        }

        // ── Elevated center button (overlaps top edge of pill bar) ────────────
        // Per screenshot: solid black circle, white triangle/play icon, raised
        // so half of it sits above the navbar (creates "floating" effect).
        Box(
            Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .offset(y = (-22).dp)  // elevate: half above navbar top edge
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(CircleShape)
                .background(Color.Black)
                .border(3.dp, Color.White, CircleShape)  // white ring separator
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPage(centerPage)
                },
            contentAlignment = Alignment.Center
        ) {
            // PS-style PLAY TRIANGLE icon (white on black)
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "GameHub",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

/** Side button for floating navbar: icon + small label below. */
@Composable
private fun NavSideButton(item: Page, selected: Boolean, onClick: () -> Unit) {
    val iconTint by animateColorAsState(
        if (selected) Color.Black else Color.Gray,
        tween(300), label = "nav_tint_${item.label}"
    )
    val labelColor by animateColorAsState(
        if (selected) Color.Black else Color.Gray.copy(alpha = 0.7f),
        tween(300), label = "nav_label_${item.label}"
    )

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() },
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
            Spacer(Modifier.height(2.dp))
            Text(
                item.label,
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = labelColor,
                maxLines = 1,
                fontFamily = InterFontFamily
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
    // v7.2.2: Localized strings untuk HomeScreen
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))

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
    // v7.0.8: Language picker state + current flag
    var showLanguagePicker by remember { mutableStateOf(false) }
    val currentLangCode = remember { LanguageManager.getCurrentLanguage(context) }
    val currentLangFlag = remember(currentLangCode) {
        LanguageManager.SupportedLanguage.entries.find { it.code == currentLangCode }?.flag ?: "🌐"
    }
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
        // v7.0.8: Language toggle (globe icon) → show language picker dialog.
        Box {
            DLavieTopBar(
                onMenuClick      = { onOpenSettings() },
                onSearchClick    = { onNav(Page.Chat) },
                onBellClick      = { showNotifPopup = true },
                onProfileClick   = { onNav(Page.Me) },
                onLanguageClick  = { showLanguagePicker = true },
                hasUnreadNotif   = latestNotif != null,
                profileInitial   = "DL",
                currentLangFlag  = currentLangFlag
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

        // ── v7.9.3: Beranda = NEWS ONLY ──────────────────────────────────────────
        // Hapus: hero carousel (3 slides), TTGameCard "DLavie 26", Play button, install hint card.
        // Game info & install sekarang ADA DI GameHub (klik game card → GameDetailScreen).
        // Beranda fokus ke berita: news hero carousel + news feed (composite).
        Spacer(Modifier.height(DLSpacing.md))
        NewsScreen(api = api)

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

    // ── v7.9.17: Onboarding modal (post-login) — DIPINDAH ke MainShell ──────
    // (was here, moved to MainShell supaya bisa akses showOnboardingModal state)
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

    // v7.0.8: Language picker dialog — tampilkan saat user tap globe icon
    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLangCode = currentLangCode,
            onSelect = { selectedCode ->
                showLanguagePicker = false
                if (selectedCode == "auto") {
                    LanguageManager.resetToAutoDetect(context)
                } else {
                    LanguageManager.setLanguage(context, selectedCode)
                }
                // Recreate activity untuk apply locale change
                (context as? android.app.Activity)?.recreate()
            },
            onDismiss = { showLanguagePicker = false }
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
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))
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
                .fold(onSuccess = { updateInfo = it }, onFailure = { updateError = it.message ?: t.connectionFailed })
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
                        Text(t.blockedMaintenance, fontWeight = FontWeight.Black, fontSize = 13.sp)
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
                    Text(t.blockedMaintenance, fontSize = 17.sp, fontWeight = FontWeight.Black)
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
                        result.onFailure { rollbackMsg = t.operationError }
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
                        result.onFailure { cleanupMsg = t.operationError }
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
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))

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
            } catch (e: Throwable) {
                errorMsg = e.message ?: t.feedLoadFailed
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
                toast(t.postSent)
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
                        toast(t.reportSent)
                    } catch (e: Throwable) {
                        toast(t.reportFailed)
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
                                    top = TTSpacing.md, bottom = 200.dp  // v7.9.2: tambah padding untuk floating navbar + Post FAB + Live Chat FAB
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
                                                toast(t.loginToLike)
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
                                                } catch (e: Throwable) {
                                                    // Revert
                                                    likeState = likeState + (post.id to (wasLiked to curCount))
                                                    toast(t.operationFailed)
                                                }
                                            }
                                        },
                                        onOpenComments = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            commentsTarget = post
                                        },
                                        onSave = {
                                            if (!api.loggedIn()) {
                                                toast(t.loginToSave)
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
                                                } catch (e: Throwable) {
                                                    savedState = savedState + (post.id to wasSaved)
                                                    toast(t.operationFailed)
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
                                            }.onFailure { toast(t.cannotOpenUrl) }
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
                // v7.9.2: FIX OVERLAP — FloatingNav center button (64dp, offset -22dp)
                // extends to 16+64+22=102dp from bottom. Post FAB sebelumnya di 96dp
                // → overlap 6dp dengan center button. Sekarang di bottom=112dp
                // (10dp clearance di atas center button top edge).
                // Live Chat FAB (di FloatingChatBot.kt) stack di atas dengan bottom=188dp
                // (112 + 56 Post FAB height + 20dp gap).
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = TTSpacing.lg, bottom = 112.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(8.dp, CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (api.isGuest()) {
                                toast(t.guestPostBlocked)
                            } else if (!api.loggedIn()) {
                                toast(t.loginToPost)
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
// v7.9.1: REDESIGN — gunakan Box dengan tabs di-align Center, role badge kiri,
// filter kanan. Sebelumnya pakai Row + 2 Spacer(weight=1f), tapi lebar role badge
// yang variabel (MEMBER=6 char, DEVELOPER=9 char) menyebabkan tabs tidak centered
// dan terlihat "terpotong"/off-balance di layar kecil. Dengan Box overlay, tabs
// SELALU di tengah layar terlepas dari lebar badge/filter.
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
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = TTSpacing.lg, vertical = TTSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        // ── Layer 1 (background): Left role badge + Right filter icon ──
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

        // ── Layer 2 (overlay): Centered tabs — selalu di tengah layar ──
        // Pakai Box dengan Center alignment supaya posisi tabs tidak
        // terpengaruh lebar role badge (kiri) atau filter icon (kanan).
        Row(horizontalArrangement = Arrangement.spacedBy(TTSpacing.xxl)) {
            CommunityTab("Following", selectedTab == 0) { onTabSelect(0) }
            CommunityTab("For You", selectedTab == 1) { onTabSelect(1) }
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
    // v7.1.2: Auto-translation system (like X/Twitter)
    // Fetch translation for current app language. If available + different from original,
    // show translated text with "Show original" toggle.
    val context = LocalContext.current
    var translation by remember(post.id) { mutableStateOf<JSONObject?>(null) }
    var showOriginal by remember(post.id) { mutableStateOf(false) }
    var translationLoaded by remember(post.id) { mutableStateOf(false) }
    LaunchedEffect(post.id) {
        val langCode = LanguageManager.getCurrentLanguage(context)
        if (langCode.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val trans = CommunityApi(context).getPostTranslation(post.id, langCode)
                    if (trans != null) {
                        // Only show translation if it's different from original
                        val transBody = trans.optString("translated_body", "")
                        if (transBody.isNotBlank() && transBody != post.body) {
                            translation = trans
                        }
                    }
                }
            }
            translationLoaded = true
        }
    }
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

                // v7.1.2: Title — show translation if available + not showing original
                val displayTitle = if (translation != null && !showOriginal) {
                    translation!!.optString("translated_title", post.title)
                } else {
                    post.title
                }
                Text(
                    displayTitle,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // v7.1.2: Body — show translation if available + not showing original
                val displayBody = if (translation != null && !showOriginal) {
                    translation!!.optString("translated_body", post.body)
                } else {
                    post.body
                }
                if (displayBody.isNotBlank()) {
                    Spacer(Modifier.height(TTSpacing.xs))
                    Text(
                        displayBody,
                        color = SoftText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // v7.1.2: "Show original" / "Show translation" toggle (like X/Twitter)
                if (translation != null) {
                    Spacer(Modifier.height(TTSpacing.xs))
                    Text(
                        if (showOriginal) "Show translation" else "Show original",
                        color = CandyCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showOriginal = !showOriginal }
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
                isFollowing && !loggedIn -> "Login untuk follow user lain"
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
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))
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
                } catch (e: Throwable) {
                    onError("Gagal upload gambar. Coba lagi.")
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
                        } catch (e: Throwable) {
                            onError("Gagal membuat post. Coba lagi.")
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
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))
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
            } catch (e: Throwable) {
                errorMsg = e.message ?: t.commentsLoadFailed
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
                        t.loginToComment,
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
                                    } catch (e: Throwable) {
                                        toast(t.operationFailed)
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

/**
 * v7.9.28: Account Switcher Section — tampil di Profile screen sebelum logout.
 * List semua akun tersimpan. Tap untuk switch. Long-press untuk remove.
 * Button "Tambah Akun" di bawah.
 */
@Composable
private fun AccountSwitcherSection(
    api: CommunityApi,
    onAccountSwitched: () -> Unit,
    onAddAccount: () -> Unit
) {
    val context = LocalContext.current
    var accounts by remember { mutableStateOf(AccountStore.listAccounts(context)) }
    val activeUserId by remember { mutableStateOf(AccountStore.getActiveUserId(context)) }
    var showRemoveConfirm by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Section header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 3.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
            Spacer(Modifier.width(8.dp))
            Text("Akun", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Spacer(Modifier.weight(1f))
            Text("${accounts.size}/5", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontFamily = InterFontFamily)
        }
        Spacer(Modifier.height(12.dp))

        // Account list
        accounts.forEach { account ->
            val isActive = account.userId == activeUserId
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    // v7.9.30: Fix — pakai detectTapGestures untuk BOTH onTap + onLongPress
                    // Sebelumnya clickable + pointerInput bersaing → tap tidak terdaftar.
                    .pointerInput(account.userId) {
                        detectTapGestures(
                            onTap = {
                                if (!isActive) {
                                    AccountStore.switchAccount(context, account.userId)
                                    onAccountSwitched()
                                }
                            },
                            onLongPress = {
                                showRemoveConfirm = account.userId
                            }
                        )
                    },
                shape = RoundedCornerShape(14.dp),
                color = if (isActive) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, if (isActive) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (account.avatarUrl != null && account.avatarUrl!!.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = account.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                (account.displayName.ifBlank { account.username.ifBlank { "?" } }).firstOrNull()?.uppercase() ?: "?",
                                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    // Name + email
                    Column(Modifier.weight(1f)) {
                        Text(
                            account.displayName.ifBlank { account.username.ifBlank { "Unknown" } },
                            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily,
                            maxLines = 1
                        )
                        if (account.email.isNotEmpty()) {
                            Text(account.email, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1)
                        }
                    }
                    // Active indicator
                    if (isActive) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Remove confirmation dialog
        showRemoveConfirm?.let { removeId ->
            val accountToRemove = accounts.find { it.userId == removeId }
            AlertDialog(
                onDismissRequest = { showRemoveConfirm = null },
                title = { Text("Hapus Akun?", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) },
                text = { Text("Akun ${accountToRemove?.displayName ?: ""} akan dihapus dari launcher. Anda bisa login lagi kapan saja.", color = Color.White.copy(alpha = 0.7f), fontFamily = InterFontFamily) },
                confirmButton = {
                    TextButton(onClick = {
                        AccountStore.removeAccount(context, removeId)
                        accounts = AccountStore.listAccounts(context)
                        showRemoveConfirm = null
                        // If removed active account, switch to another or go to login
                        if (removeId == activeUserId) {
                            val remaining = AccountStore.listAccounts(context)
                            if (remaining.isEmpty()) {
                                onAddAccount()
                            } else {
                                onAccountSwitched()
                            }
                        }
                    }) { Text("Hapus", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontFamily = InterFontFamily) }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveConfirm = null }) { Text("Batal", color = Color.White, fontFamily = InterFontFamily) }
                },
                containerColor = Carbon
            )
        }

        // Add account button
        if (accounts.size < 5) {
            Spacer(Modifier.height(8.dp))
            Surface(
                Modifier.fillMaxWidth().clickable { onAddAccount() },
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PersonAdd, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Akun", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    api: CommunityApi,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenEditProfile: () -> Unit = {},
    expandedSection: String? = null,
    onExpandedSectionChange: (String?) -> Unit = {},
    onVisitProfile: (String) -> Unit = {}
) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))
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
    var myPostsCount by remember { mutableStateOf(0) }  // v7.3.0: for stat card
    var myDrafts     by remember { mutableStateOf<List<FeedPostData>>(emptyList()) }
    var myIssues     by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var deletingIssueId by remember { mutableStateOf<String?>(null) }
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
                    toast(t.profilePhotoUpdated)
                } catch (e: Throwable) {
                    toast(t.photoUploadFailed)
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
                            myPostsCount = myPosts.size
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
                        }
                        3 -> {
                            // Issues tab — fetch feed_posts type=issue
                            val resp = api.adminGet("/rest/v1/feed_posts?type=eq.issue&order=created_at.desc&limit=30&select=id,title,body,created_at")
                            val arr = JSONArray(resp)
                            val issueList = mutableListOf<JSONObject>()
                            for (i in 0 until arr.length()) { issueList.add(arr.getJSONObject(i)) }
                            myIssues = issueList
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ════════════════════════════════════════════════════════════════════
        // ════════════════════════════════════════════════════════════════════
        // v7.9.59 TOTAL REMAKE — Glassmorphism, Clean Navigation, Minimalist Modern
        // No duplicate functionality. Settings hanya di SettingsScreen (via gear icon).
        // Profile fokus: identity + content (posts/saved/drafts) + portal status + logout.
        // ════════════════════════════════════════════════════════════════════
        if (profileLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LottieLoading(size = 48.dp)
            }
        } else {

            // ── Top bar: "Profile" + streak + settings gear ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(t.profile, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Badges count (glass pill)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFFAA00), modifier = Modifier.size(14.dp))
                            Text("${myBadges.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Settings gear (glass circle)
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(Color.White.copy(0.06f))
                            .border(1.dp, Color.White.copy(0.1f), CircleShape)
                            .clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ── Glass Hero Card: avatar + name + stats (glassmorphism) ──
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(0.03f),
                border = BorderStroke(1.dp, Color.White.copy(0.08f))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with glass ring
                    Box(
                        Modifier.size(92.dp).clickable {
                            if (api.loggedIn() && !avatarUploading) {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                    ) {
                        // Glass ring around avatar
                        Box(
                            Modifier.size(92.dp).clip(CircleShape)
                                .background(Color.White.copy(0.08f))
                                .border(1.dp, Color.White.copy(0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUrlState.isNotEmpty()) {
                                AsyncImage(
                                    model = avatarUrlState,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                DLavieLogoCover(
                                    size = 80.dp, text = initial,
                                    fontSize = 32.sp, shape = CircleShape
                                )
                            }
                        }
                        if (avatarUploading) {
                            Box(
                                Modifier.fillMaxSize().clip(CircleShape)
                                    .background(Color.Black.copy(0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            }
                        }
                        // Camera badge (glass)
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(28.dp)
                                .background(Color.White, CircleShape)
                                .border(3.dp, Color(0xFF0A0A0A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Name + verified badge
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            api.displayName().ifEmpty { "DLavie Player" },
                            color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp
                        )
                        if (role.equals("admin", true) || role.equals("developer", true)) {
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.Rounded.Verified, null, tint = Color(0xFF2ED3F6), modifier = Modifier.size(16.dp))
                        }
                    }

                    // Username
                    Text(
                        "@${api.username().ifEmpty { "unknown" }}",
                        color = SubText, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                    )

                    Spacer(Modifier.height(12.dp))

                    // Stats row (3 columns, glass)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatItem(count = followerCount, label = t.followers.lowercase())
                        // Divider
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.1f)))
                        ProfileStatItem(count = followingCount, label = t.followingCount.lowercase())
                        Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.1f)))
                        ProfileStatItem(count = likesCount, label = "likes")
                    }

                    // Bio (optional)
                    if (bio.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            bio,
                            color = SoftText, fontSize = 12.sp, lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Edit Profile button (glass pill)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clickable { onOpenEditProfile() },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(0.12f))
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t.editProfile, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Glass Pill Tabs (Posts | Saved | Drafts | Issues) ──
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(0.03f),
                border = BorderStroke(1.dp, Color.White.copy(0.08f))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GlassPillTab(
                        icon = Icons.Rounded.Article,
                        label = t.myPosts,
                        selected = selectedTab == 0,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 0 }
                    GlassPillTab(
                        icon = Icons.Rounded.BookmarkBorder,
                        label = t.savedPosts,
                        selected = selectedTab == 1,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 1 }
                    GlassPillTab(
                        icon = Icons.Rounded.Drafts,
                        label = "Drafts",
                        selected = selectedTab == 2,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 2 }
                    if (role.equals("admin", true) || role.equals("developer", true)) {
                        GlassPillTab(
                            icon = Icons.Rounded.Warning,
                            label = "Issues",
                            selected = selectedTab == 3,
                            modifier = Modifier.weight(1f)
                        ) { selectedTab = 3 }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Content per tab (existing logic, preserved) ──
            when {
                postsLoading -> {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(2) { TTGameCardSkeleton() }
                    }
                }
                selectedTab == 0 && myPosts.isEmpty() -> ProfileEmptyPosts(
                    text = "No posts yet. Create your first post in Community!"
                )
                selectedTab == 1 && mySavedPosts.isEmpty() -> ProfileEmptyPosts(
                    text = "No saved posts. Tap bookmark on any post to save."
                )
                selectedTab == 2 && myDrafts.isEmpty() -> ProfileEmptyPosts(
                    text = "No drafts. Toggle 'Save as Draft' when creating a post."
                )
                selectedTab == 3 && myIssues.isEmpty() -> ProfileEmptyPosts(
                    text = "No issues reported yet."
                )
                selectedTab == 3 -> {
                    Column(
                        Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        myIssues.forEach { issue ->
                            val issueId = issue.optString("id", "")
                            val issueTitle = issue.optString("title", "")
                            val issueDate = issue.optString("created_at", "").take(10)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(0.03f),
                                border = BorderStroke(1.dp, Color.White.copy(0.08f))
                            ) {
                                Row(
                                    Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Warning, null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(issueTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$issueDate · ${issueId.take(8)}", color = SubText, fontSize = 10.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    if (deletingIssueId == issueId) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DangerRed, strokeWidth = 2.dp)
                                    } else {
                                        Surface(
                                            modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                                                deletingIssueId = issueId
                                                scope.launch {
                                                    val errMsg = withContext(Dispatchers.IO) {
                                                        try {
                                                            api.adminDelete("/rest/v1/feed_comments?post_id=eq.$issueId")
                                                            api.adminDelete("/rest/v1/feed_posts?id=eq.$issueId")
                                                            null
                                                        } catch (e: Exception) { e.message }
                                                    }
                                                    if (errMsg == null) {
                                                        myIssues = myIssues.filterNot { it.optString("id") == issueId }
                                                        toast("Issue dihapus")
                                                    } else {
                                                        toast("Gagal: $errMsg")
                                                    }
                                                    deletingIssueId = null
                                                }
                                            },
                                            color = DangerRed.copy(0.15f),
                                            border = BorderStroke(1.dp, DangerRed.copy(0.4f)),
                                            shape = CircleShape
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Delete, "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    val list = when (selectedTab) {
                        0 -> myPosts
                        1 -> mySavedPosts
                        else -> myDrafts
                    }
                    Column(
                        Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                            toast(t.draftPublished)
                                            publishingId = null
                                            loadTab()
                                        } catch (e: Throwable) {
                                            toast(t.publishFailed)
                                            publishingId = null
                                        }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { api.deleteFeedPost(post.id) }
                                            toast(t.postDeleted)
                                            loadTab()
                                        } catch (e: Throwable) {
                                            toast(t.deleteFailed)
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

            Spacer(Modifier.height(16.dp))

            // ── DLavie Portal Connect card (glassmorphism) ──
            var portalConnected by remember { mutableStateOf(
                context.getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE)
                    .getBoolean("portal_connected", false)
            ) }
            val portalLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            LaunchedEffect(portalLifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        portalConnected = context.getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE)
                            .getBoolean("portal_connected", false)
                    }
                }
                portalLifecycleOwner.lifecycle.addObserver(observer)
                try { kotlinx.coroutines.awaitCancellation() } finally { portalLifecycleOwner.lifecycle.removeObserver(observer) }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (portalConnected) Color(0xFF00D26A).copy(0.06f) else Color.White.copy(0.03f),
                border = BorderStroke(
                    1.dp,
                    if (portalConnected) Color(0xFF00D26A).copy(0.3f) else Color.White.copy(0.08f)
                )
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (portalConnected) Brush.radialGradient(listOf(Color(0xFF00D26A), Color(0xFF00A050)))
                                else Brush.radialGradient(listOf(Color.White.copy(0.1f), Color.White.copy(0.03f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (portalConnected) {
                            Icon(Icons.Rounded.CheckCircle, "Connected", tint = Color.Black, modifier = Modifier.size(22.dp))
                        } else {
                            Icon(Icons.Rounded.Language, "Not connected", tint = Color.White.copy(0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DLavie Portal", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).sp)
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(6.dp).clip(CircleShape)
                                    .background(if (portalConnected) Color(0xFF00D26A) else Color(0xFF666666))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (portalConnected) "Connected to Web Portal" else "Not connected",
                                color = if (portalConnected) Color(0xFF00D26A) else SubText,
                                fontSize = 11.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (portalConnected) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF00D26A).copy(0.15f),
                            border = BorderStroke(1.dp, Color(0xFF00D26A).copy(0.3f))
                        ) {
                            Text("ACTIVE", color = Color(0xFF00D26A), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.08.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            // FCM (admin only) — preserved
            if (role == "admin" || role == "developer") {
                Spacer(Modifier.height(8.dp))
                FcmDiagnosticCard(api = api, context = context)
            }

            // Account switcher — preserved
            AccountSwitcherSection(api = api, onAccountSwitched = {
                context.startActivity(
                    android.content.Intent(context, ModernLauncherActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }, onAddAccount = {
                val prefs = context.getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE)
                val savedAccounts = prefs.getString("accounts", "[]")
                val savedActiveId = prefs.getString("active_user_id", "")
                prefs.edit().clear().apply()
                prefs.edit().putString("accounts", savedAccounts).putString("active_user_id", savedActiveId).apply()
                context.getSharedPreferences("dlavie_auth_session", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                context.startActivity(
                    android.content.Intent(context, DLavieGuidedActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            })

            // ── Logout (glass danger button) ──
            AnimatedContent(targetState = confirmLogout, label = "logout") { confirm ->
                if (!confirm) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clickable { confirmLogout = true },
                            shape = RoundedCornerShape(14.dp),
                            color = DangerRed.copy(0.06f),
                            border = BorderStroke(1.dp, DangerRed.copy(0.3f))
                        ) {
                            Row(
                                Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Logout, null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(t.logout, color = DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(0.04f),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f))
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Logout", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(4.dp))
                            Text("You will need to sign in again.", color = SoftText, fontSize = 12.sp)
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { confirmLogout = false },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftText)
                                ) { Text(t.cancel, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                Button(
                                    onClick = onLogout,
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                                ) { Text(t.logout, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// v7.9.59 NEW: Glass composable helpers for Profile redesign
// ════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileStatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$count",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.3).sp
        )
        Text(
            label,
            color = SubText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GlassPillTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White.copy(0.12f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) Color.White.copy(0.25f) else Color.Transparent
        )
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = if (selected) Color.White else SubText,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = if (selected) Color.White else SubText,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// v7.5.0: Helper composables
@Composable
private fun PillTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier
            .height(38.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(19.dp),
        color = if (selected) Color.White else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, Color.White.copy(0.12f))
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (selected) PureBlack else SubText, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = if (selected) PureBlack else SubText,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingsContainer(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = GlassBase,
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingText: String? = null
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (trailingText != null) {
            Text(trailingText, color = SubText, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = SubText, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 48.dp).height(1.dp).background(GlassStroke))
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
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))
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
                                        } catch (e: Throwable) {
                                            // Revert
                                            isFollowing = prev
                                            followerCount += if (prev) 1 else -1
                                            toast(t.operationFailed)
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
                                onClick = { toast(t.loginToFollow) },
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
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))
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
                resultMsg = it.message ?: "Operasi gagal. Coba lagi."
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
                        isSuccess = false; resultMsg = "Password tidak cocok."; return@Button
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
    error: String = "",
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
                        "Downloading... ${(progress * 100).toInt()}%",
                        color = SoftText, fontSize = 11.sp
                    )
                }

                // v7.5.4: Error display (modern, with icon)
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth()
                            .background(DangerRed.copy(0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, DangerRed.copy(0.20f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                        Text(error, color = DangerRed, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f))
                    }
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
                    Text("Downloading...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.SystemUpdate, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Update Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLater,
                enabled = !downloading
            ) {
                Text("Later", color = SubText, fontSize = 13.sp)
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

/**
 * Launch game by package name + main activity.
 *
 * v7.2.7: CRITICAL FIX — removed browser fallback yang bikin user kecewa.
 * Sebelumnya, kalau game belum terinstall, launchGame buka URL download
 * di browser (Chrome) — ini UX yang sangat buruk.
 *
 * Sekarang:
 * - Pre-check: getPackageInfo() untuk verify APK terinstall
 * - Kalau belum terinstall → return false (caller handle, tampilkan error)
 * - Kalau terinstall → coba setClassName, fallback ke getLaunchIntentForPackage
 * - TIDAK ADA browser fallback lagi
 *
 * @return true kalau game berhasil di-launch, false kalau gagal/belum terinstall
 */
fun launchGame(context: android.content.Context, gamePackage: String = GAME_PKG_16, mainActivity: String? = null): Boolean {
    // Pre-check: verify APK terinstall
    val isInstalled = try {
        context.packageManager.getPackageInfo(gamePackage, 0)
        true
    } catch (_: Throwable) {
        false
    }
    if (!isInstalled) {
        android.util.Log.w("DLavie", "launchGame: $gamePackage not installed — refusing to launch")
        return false
    }

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
    return try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        // Fallback: getLaunchIntentForPackage (TIDAK ADA browser fallback)
        val fallbackIntent = context.packageManager.getLaunchIntentForPackage(gamePackage)
        if (fallbackIntent != null) {
            fallbackIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(fallbackIntent)
                true
            } catch (_: Throwable) {
                android.util.Log.e("DLavie", "launchGame: fallback launch failed for $gamePackage", e)
                false
            }
        } else {
            android.util.Log.e("DLavie", "launchGame: no launch intent for $gamePackage")
            false
        }
    }
}

/**
 * v7.9.3: Uninstall game via PackageManager.
 * Return true kalau berhasil, false kalau gagal (e.g. game tidak installed, atau permission denied).
 *
 * Catatan: Untuk uninstall tanpa root, launcher hanya bisa trigger
 * uninstall confirmation dialog (Intent.ACTION_DELETE) — user konfirmasi manual.
 * Tapi kalau pakai @Suppress("DEPRECATION") packageManager.deletePackage,
 * perlu signature/system permission. Untuk launcher biasa, pakai Intent approach.
 *
 * Disini kita cek dulu game installed apa nggak, lalu buka uninstall confirmation.
 */
suspend fun uninstallGame(context: android.content.Context, packageName: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // Cek installed
            context.packageManager.getPackageInfo(packageName, 0)
            // Trigger uninstall confirmation (user konfirmasi manual di system dialog)
            val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:$packageName")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
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
    val scope = rememberCoroutineScope()

    // v7.8.1: Fetch server status dari Supabase app_config
    var fifa16Status by remember { mutableStateOf(ServerStatus.ONLINE) }
    var fifa15Status by remember { mutableStateOf(ServerStatus.MAINTENANCE) } // default: maintenance (masih error)

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val api = CommunityApi(context)
                // Fetch game_server_status dari app_config
                // v7.9.13: "busy" tidak lagi dari Supabase — auto-detect dari sinyal user
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

    val games = remember(fifa16Status, fifa15Status) {
        listOf(
            GameItem(
                title = "FIFA 16 Mobile",
                subtitle = "DLavie 26 Mod · Sports",
                packageName = GAME_PKG_16,
                mainActivity = "com.byfen.downloadzipsdk.MainActivity",
                coverGradient = listOf(Color(0xFF0A0A0A), Color(0xFF222222)),
                coverText = "DL",
                coverImageRes = R.drawable.fifa16_cover,
                serverStatus = fifa16Status,
                // v7.9.3: Enhanced metadata
                description = "FIFA 16 Mobile dengan mod DLavie 26 — gameplay yang ditingkatkan, roster update musim 2025/2026, " +
                    "dan fitur tambahan. Mainkan mode career, ultimate team, dan online match dengan komunitas DLavie.\n\n" +
                    "Game ini dimaintain oleh DLavie Company dan didistribusikan melalui DLavie Launcher.",
                developer = "DLavie Company",
                version = "v26.0",
                sizeMb = "34 MB",
                category = "Olahraga",
                ageRating = "9+",
                language = "Indonesia, English",
                engine = "EA Sports FIFA 16",
                lastUpdate = "5 Juli 2026",
                features = listOf(
                    "Gameplay Realistis — Mod gameplay yang ditingkatkan",
                    "Roster Update 2025/2026 — Pemain dan tim terbaru",
                    "Komunitas Aktif — Ribuan pemain DLavie",
                    "Update Rutin — Patch mod baru berkala"
                ),
                // v7.9.4: Real screenshots dari gameplay FIFA 16 (user upload)
                screenshots = listOf(
                    R.drawable.fifa16_screenshot_1,
                    R.drawable.fifa16_screenshot_2,
                    R.drawable.fifa16_screenshot_3,
                    R.drawable.fifa16_screenshot_4
                ),
                apkUrl = FIFA16_APK_URL
            ),
            GameItem(
                title = "FIFA 15 Mobile",
                subtitle = "DLavie 15 Mod · Sports",
                packageName = GAME_PKG_15,
                mainActivity = FIFA15_MAIN_ACTIVITY,
                coverGradient = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)),
                coverText = "D15",
                coverImageRes = R.drawable.fifa15_cover,
                serverStatus = fifa15Status,
                // v7.9.3: Enhanced metadata
                description = "FIFA 15 Mobile dengan mod DLavie 15 — versi klasik dengan gameplay retro, " +
                    "roster musim 2014/2015, dan feel nostalgia FIFA era lama.\n\n" +
                    "Saat ini dalam maintenance — pantau pengumuman komunitas untuk info ketersediaan.",
                developer = "DLavie Company",
                version = "v15.0",
                sizeMb = "22 MB",
                category = "Olahraga",
                ageRating = "9+",
                language = "Indonesia, English",
                engine = "EA Sports FIFA 15",
                lastUpdate = "4 Juli 2026",
                features = listOf(
                    "Gameplay Klasik — Feel FIFA 15 asli",
                    "Roster 2014/2015 — Pemain era itu",
                    "Mode Nostalgia — Untuk fans lama",
                    "Kompatibilitas Luas — Android 16 ready"
                ),
                apkUrl = FIFA15_APK_URL
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
                            // v7.9.3: Open GameDetailScreen (console-style) — NOT GameActionPanel.
                            // Pass game packageName so MainShell can find the right GameItem.
                            onGameClick(game.packageName)
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
                            // v7.9.3: Open GameDetailScreen — same as card click.
                            onGameClick(game.packageName)
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
            .height(180.dp)
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
                            0f to Color.Black.copy(alpha = 0.3f),
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.9f)
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

            // v7.8.0: Server status badge (top-right, cloud gaming style)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                shape = RoundedCornerShape(999.dp),
                color = game.serverStatus.bgColor,
                border = BorderStroke(1.dp, game.serverStatus.dotColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pulsing dot
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(game.serverStatus.dotColor)
                    )
                    Text(
                        game.serverStatus.label,
                        color = game.serverStatus.textColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        maxLines = 1
                    )
                }
            }

            // Bottom info
            Column(
                Modifier.align(Alignment.BottomStart).padding(14.dp)
            ) {
                Text(game.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
                Text(game.subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = InterFontFamily, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                // Play button — cloud gaming style
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.PlayArrow,
                            null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Play", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
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
            // v7.8.0: Server status indicator (cloud gaming style)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier.size(5.dp)
                        .clip(CircleShape)
                        .background(game.serverStatus.dotColor)
                )
                Text(
                    game.serverStatus.label,
                    color = game.serverStatus.textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    maxLines = 1
                )
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}

// GameItem moved to GameItem.kt (v7.3.7)


// ─── Issue Manager Card (admin/dev) — delete web FAQ issues ───
@Composable
fun IssueManagerCard(api: CommunityApi, context: android.content.Context) {
    val scope = rememberCoroutineScope()
    var issues by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var deletingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.adminGet("/rest/v1/feed_posts?type=eq.issue&order=created_at.desc&limit=20&select=id,title,body,created_at")
                val arr = JSONArray(resp)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) { list.add(arr.getJSONObject(i)) }
                issues = list
            }
            loading = false
        }
    }

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = DangerRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Issue Manager", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${issues.size}", color = SubText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Text("Hapus issue yang tidak relevan atau sudah resolved.", color = SubText, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            }
        } else if (issues.isEmpty()) {
            Text("Tidak ada issue.", color = SubText, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        } else {
            issues.forEach { issue ->
                val issueId = issue.optString("id", "")
                val issueTitle = issue.optString("title", "")
                val issueDate = issue.optString("created_at", "").take(10)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(0.04f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(issueTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$issueDate · ${issueId.take(8)}", color = SubText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (deletingId == issueId) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DangerRed, strokeWidth = 2.dp)
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    deletingId = issueId
                                    scope.launch {
                                        val errorMsg = withContext(Dispatchers.IO) {
                                            try {
                                                api.adminDelete("/rest/v1/feed_comments?post_id=eq.$issueId")
                                                api.adminDelete("/rest/v1/feed_posts?id=eq.$issueId")
                                                null  // success
                                            } catch (e: Exception) {
                                                e.message ?: "Gagal menghapus issue"
                                            }
                                        }
                                        if (errorMsg == null) {
                                            issues = issues.filterNot { it.optString("id") == issueId }
                                            android.widget.Toast.makeText(context, "Issue dihapus", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Gagal: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                        deletingId = null
                                    }
                                },
                            color = DangerRed.copy(0.15f),
                            border = BorderStroke(1.dp, DangerRed.copy(0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Delete, "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
