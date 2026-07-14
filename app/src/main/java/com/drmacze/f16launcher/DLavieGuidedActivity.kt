package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
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
import rikka.shizuku.Shizuku
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val GAME_PACKAGE = "com.ea.gp.fifaworld"
private const val MARKER_PATH = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private const val LOCAL_VERSION_CODE = 1
private const val LOCAL_VERSION_NAME = "v1"
private val SUPABASE_URL get() = BuildConfig.SUPABASE_URL
private val SUPABASE_KEY get() = BuildConfig.SUPABASE_ANON_KEY
private const val PREF_AUTH = "dlavie_auth_session"
private const val PREF_TOKEN = "access_token"
private const val PREF_REFRESH = "refresh_token"
private const val PREF_EMAIL = "email"
private const val SHIZUKU_REQUEST = 2026

class DLavieGuidedActivity : ComponentActivity() {

    // v6.8.4: Deep link callback state — saat Google OAuth redirect ke
    // dlavie://auth-callback#access_token=...&refresh_token=..., kita parse
    // token dan langsung save session → navigate ke launcher.
    // State ini di-observe oleh Composable untuk show success/error.
    private var deepLinkResult: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val existing = loadSession(this)
        if (existing != null) {
            syncToCommunityPrefs(this, existing)
            startActivity(Intent(this, ModernLauncherActivity::class.java))
            finish()
            return
        }
        // v6.8.4: Cek apakah activity dibuka via deep link (Google OAuth callback)
        handleDeepLink(intent)
        setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
    }

    // v6.8.4: singleTop launch mode → deep link redirect datang via onNewIntent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
        // Re-render composable dengan result baru
        setContent { DLavieGuidedApp(deepLinkResult = deepLinkResult) }
    }

    /**
     * v6.8.4: Parse deep link callback dari Supabase OAuth.
     * Format: dlavie://auth-callback#access_token=...&refresh_token=...&expires_in=...
     * Atau error: dlavie://auth-callback#error=...&error_description=...
     *
     * Token dikirim di URL fragment (#) bukan query (?), jadi pakai uri.fragment.
     */
    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data = intent.data ?: return
        if (action != Intent.ACTION_VIEW) return

        val fragment = data.fragment ?: ""
        val query = data.query ?: ""

        // Parse key=value pairs dari fragment (token) dan query (error)
        val params = mutableMapOf<String, String>()
        val pairs = (fragment + "&" + query).split("&").filter { it.contains("=") }
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = pair.substring(0, idx)
                val value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                params[key] = value
            }
        }

        val accessToken = params["access_token"]
        val refreshToken = params["refresh_token"]
        val error = params["error"]

        if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
            // v6.8.4: Success — save session & navigate to launcher
            val email = params["user_email"] ?: ""
            val session = AuthSession(accessToken, refreshToken, email)
            saveSession(this, session)
            syncToCommunityPrefs(this, session)
            // Clear guest flag (auto-upgrade dari guest ke user penuh)
            val api = CommunityApi(this)
            api.clearGuest()
            // Sync profile dari Supabase (retry 3x — trigger handle_new_user async)
            runCatching {
                val ca = CommunityApi(this)
                for (attempt in 1..3) {
                    try { ca.loadMyProfile(); break } catch (_: Exception) {
                        if (attempt < 3) Thread.sleep(500L)
                    }
                }
            }
            // Fire telemetry
            runCatching { Telemetry.track(this, Telemetry.EVT_LOGIN, mapOf("method" to "google_oauth")) }
            deepLinkResult = "OK: Login Google berhasil. Memuat launcher..."
            // Navigate setelah delay singkat supaya UI bisa show success message
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, ModernLauncherActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }, 1500)
        } else if (!error.isNullOrBlank()) {
            val desc = params["error_description"] ?: error
            deepLinkResult = "Error: Google login gagal — $desc"
        }
        // else: not a deep link callback → ignore (normal launch)
    }
}

// ─── Maintenance config (fetched at startup) ────────────────────────────────────
private data class MaintenanceState(
    val enabled: Boolean = false,
    val title: String = "",
    val message: String = "",
    val allowOffline: Boolean = false,
    val loaded: Boolean = false
)

// ─── Country picker list (21 entries per spec) ──────────────────────────────────
// v7.9.17: Made public supaya OnboardingModal bisa akses
val COUNTRY_LIST: List<String> = listOf(
    "Indonesia", "Malaysia", "Singapore", "Philippines", "Thailand",
    "Vietnam", "India", "USA", "UK", "Japan",
    "South Korea", "Brazil", "Germany", "France", "Canada",
    "Australia", "Saudi Arabia", "UAE", "Netherlands", "Spain",
    "Other"
)
const val DEFAULT_COUNTRY = "Indonesia"

private enum class GuidedTab(val label: String, val icon: String) {
    Home("Home", "⌂"), Data("Data", "▣"), Update("Update", "◎"), Me("Me", "♙")
}

private data class AuthSession(val accessToken: String, val refreshToken: String, val email: String)
private data class AuthResult(val session: AuthSession?, val message: String)
private data class BootstrapState(
    val displayName: String = "DLavie Player",
    val role: String = "user",
    val maintenance: Boolean = false,
    val maintenanceMessage: String = "",
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "Belum dicek",
    val updateAvailable: Boolean = false,
    val patchName: String = "",
    val patchUrl: String = "",
    val notices: List<String> = emptyList(),
    val unreadNotifications: Int = 0,
    val loaded: Boolean = false,
    val error: String = ""
)

private data class GuidedUpdateState(
    val localCode: Int = LOCAL_VERSION_CODE,
    val localName: String = LOCAL_VERSION_NAME,
    val latestCode: Int = 0,
    val latestName: String = "Belum dicek",
    val status: String = "LOGIN",
    val message: String = "Login diperlukan untuk menggunakan DLavie.",
    val shizuku: String = "Unknown",
    val marker: String = "Unknown",
    val patchName: String = "",
    val canDownload: Boolean = false,
    val canInstall: Boolean = false,
    val canAllowShizuku: Boolean = false,
    val working: Boolean = false,
    val progress: Float = 0f,
    val progressText: String = "",
    val sizeText: String = "-",
    val speedText: String = "-",
    val etaText: String = "-",
    val releaseNotes: List<String> = emptyList(),
    val knownIssues: List<String> = emptyList()
)

// v7.9.56 palette — match DLavie website (dlavie-web) CSS variables
// Pure black base, glass-stroke borders, soft white text, subtle accent colors
private val GuideGreen = Color(0xFF00D26A)        // --success
private val GuideCyan = Color(0xFF2ED3F6)
private val GuideRed = Color(0xFFFF4444)          // --danger
private val GuideAmber = Color(0xFFFFAA00)        // --amber
private val GuideWhite = Color(0xFFFFFFFF)        // --text-white
private val GuideSoftText = Color(0xFFCCCCCC)     // --soft-text
private val GuideMuted = Color(0xFF888888)        // --sub-text
private val GuideDim = Color(0xFF555555)          // --dim-text
private val GuideDark = Color(0xFF000000)         // --pure-black
private val GuideCarbon = Color(0xFF0A0A0A)       // --carbon
private val GuideSurface1 = Color(0xFF0E0E0E)     // --surface-1
private val GuideSurface2 = Color(0xFF141414)     // --surface-2
private val GuideCard = Color(0xFF0E0E0E)         // --surface-1 (portal-card bg)
private val GuideGlassStroke = Color(0x1AFFFFFF)  // --glass-stroke (rgba 0.10)
private val GuideGlassStrokeHi = Color(0x33FFFFFF)// --glass-stroke-hi (rgba 0.20)
private val GuideHairline = Color(0x0DFFFFFF)     // --hairline (rgba 0.05)
private val GuideFont = FontFamily.SansSerif      // Web pakai Clash Display + Space Grotesk, tapi Android pakai system sans (cukup similar)
private val GuideEase = androidx.compose.animation.core.CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
// Alias untuk backward compat (sebelumnya pakai GuideBorder)
private val GuideBorder = GuideGlassStroke

@Composable
private fun DLavieGuidedApp(deepLinkResult: String? = null) {
    val context = LocalContext.current
    // Maintenance state fetched at app startup BEFORE the login screen is shown.
    var maintenance by remember { mutableStateOf(MaintenanceState()) }
    var showLogin  by remember { mutableStateOf(false) }
    // v6.8.4: Deep link result dari Google OAuth callback
    var deepLinkMsg by remember { mutableStateOf(deepLinkResult ?: "") }

    LaunchedEffect(Unit) {
        maintenance = withContext(Dispatchers.IO) { fetchMaintenanceConfig() }
        if (!maintenance.enabled) showLogin = true
    }

    MaterialTheme(darkColorScheme(background = GuideDark, surface = GuideCard, primary = GuideGreen, secondary = GuideCyan, onBackground = GuideWhite, onSurface = GuideWhite)) {
        Surface(color = GuideDark, modifier = Modifier.fillMaxSize()) {
            // v3.0: Halftone particle background (replaces green radial gradient)
            Box(Modifier.fillMaxSize()) {
                HalftoneBackground(
                    modifier = Modifier.fillMaxSize(),
                    dotSize = 2.5f,
                    spacing = 24f,
                    baseColor = HalftoneBright,
                    alpha = 0.6f
                )
                if (maintenance.enabled && !showLogin) {
                    MaintenanceOverlay(
                        title        = maintenance.title,
                        message      = maintenance.message,
                        allowOffline = maintenance.allowOffline,
                        onContinueOffline = {
                            // User opted to play offline — show login screen anyway so they can still log in if they want.
                            showLogin = true
                        },
                        onClose = {
                            // Close the app
                            (context as? android.app.Activity)?.finishAffinity()
                        }
                    )
                } else if (showLogin || maintenance.loaded) {
                    GuidedLoginScreen(
                        deepLinkMessage = deepLinkMsg,
                        onLoggedIn = { session ->
                            saveSession(context, session)
                            syncToCommunityPrefs(context, session)
                            // v7.9.28: Save account to multi-account store
                            try {
                                val api = CommunityApi(context)
                                val account = AccountStore.Account()
                                account.userId = api.userId()
                                account.email = session.email
                                account.username = api.username()
                                account.displayName = api.displayName()
                                account.avatarUrl = api.avatarUrl()
                                account.role = api.role()
                                account.country = api.country()
                                account.accessToken = session.accessToken
                                account.refreshToken = session.refreshToken
                                AccountStore.saveAccount(context, account)
                            } catch (_: Exception) {}
                            context.startActivity(
                                Intent(context, ModernLauncherActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                } else {
                    // Initial loading state (very brief — maintenance fetch is fast).
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = GuideGreen, strokeWidth = 2.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedLoginScreen(
    deepLinkMessage: String = "",
    onLoggedIn: (AuthSession) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // v7.2.4: Localized strings
    val t = Strings.get(LanguageManager.getCurrentLanguage(context))
    //   - "chooser": Google + Email + Guest buttons (default)
    //   - "login" / "register": expanded email/password form
    //   - "forgot": reset password form
    var mode by remember { mutableStateOf("chooser") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    // v7.9.17: country state dihapus — pindah ke onboarding modal
    var showPass by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    // v6.8.4: Init message dari deep link callback (Google OAuth result)
    var message by remember { mutableStateOf(deepLinkMessage) }
    var isSuccess by remember { mutableStateOf(deepLinkMessage.startsWith("OK")) }

    Box(
        Modifier
            .fillMaxSize()
            .background(GuideDark)   // --pure-black base
    ) {
        // ── v7.9.56: Background match DLavie web ──
        // Layer 1: carbon gradient (pure black → carbon → surface-2)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to GuideDark,
                        0.5f to GuideCarbon,
                        1f to GuideSurface2
                    )
                )
        )

        // Layer 2: Interactive Halftone Background (v7.9.57 — Canvas-based, touch drift)
        InteractiveHalftoneBackground(
            modifier = Modifier.fillMaxSize(),
            dotSpacing = 28f,
            dotSize = 2.2f
        )

        // Layer 3: Glow orbs (match web .glow-orb-1 + .glow-orb-2)
        // Orb 1: top-right, white radial, animated
        val orbAnim1 = rememberInfiniteTransition(label = "orb1")
        val orbX1 by orbAnim1.animateFloat(
            initialValue = 0f, targetValue = -200f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = GuideEase),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb1x"
        )
        val orbScale1 by orbAnim1.animateFloat(
            initialValue = 1f, targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = GuideEase),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb1s"
        )
        Box(
            Modifier
                .size(800.dp)
                .align(Alignment.TopEnd)
                .offset(x = orbX1.dp, y = (-200 + orbX1 * 0.5f).dp)
                .graphicsLayer {
                    this.scaleX = orbScale1
                    this.scaleY = orbScale1
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
                .blur(80.dp)
        )

        // Orb 2: bottom-left, animated
        val orbAnim2 = rememberInfiniteTransition(label = "orb2")
        val orbX2 by orbAnim2.animateFloat(
            initialValue = 0f, targetValue = 200f,
            animationSpec = infiniteRepeatable(
                animation = tween(25000, easing = GuideEase),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb2x"
        )
        val orbScale2 by orbAnim2.animateFloat(
            initialValue = 1f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(25000, easing = GuideEase),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb2s"
        )
        Box(
            Modifier
                .size(800.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-200 + orbX2).dp, y = (200 - orbX2 * 0.5f).dp)
                .graphicsLayer {
                    this.scaleX = orbScale2
                    this.scaleY = orbScale2
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
                .blur(80.dp)
        )

        // Layer 4: Vignette (match web .vignette — radial darkening at edges)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.6f)
                    )
                )
        )

        // ── Main content column ──
        // v7.9.78: GSAP-style staggered entrance animation
        // Each element fades in + slides up with 80ms stagger delay
        val staggerSteps = 6
        val staggers = remember { List(staggerSteps) { mutableStateOf(false) } }
        LaunchedEffect(Unit) {
            staggers.forEachIndexed { i, s ->
                delay(80L * i)
                s.value = true
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 100.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── v7.9.57: Animated DL Logo (custom drawn, unique modern animation) ──
            // v7.9.78: GSAP-style staggered entrance
            val logoAlpha by animateFloatAsState(
                if (staggers[0].value) 1f else 0f,
                tween(600, easing = GuideEase), label = "logo_alpha"
            )
            val logoOffsetY by animateFloatAsState(
                if (staggers[0].value) 0f else 30f,
                tween(600, easing = GuideEase), label = "logo_offset"
            )
            Box(Modifier.graphicsLayer {
                this.alpha = logoAlpha
                this.translationY = logoOffsetY
            }) {
                AnimatedDLLogo(size = 80.dp)
            }

            Spacer(Modifier.height(24.dp))

            // ── Title: "DLavie Portal" (match web .portal-title) ──
            // v7.9.78: GSAP-style staggered entrance
            val titleAlpha by animateFloatAsState(
                if (staggers[1].value) 1f else 0f,
                tween(600, easing = GuideEase), label = "title_alpha"
            )
            val titleOffsetY by animateFloatAsState(
                if (staggers[1].value) 0f else 30f,
                tween(600, easing = GuideEase), label = "title_offset"
            )
            // v7.9.78: Apply staggered animation to title
            Box(Modifier.graphicsLayer {
                this.alpha = titleAlpha
                this.translationY = titleOffsetY
            }) {
                Text(
                    when (mode) {
                        "chooser"  -> "DLAVIE PORTAL"
                        "login"    -> t.loginSubtitle
                        "register" -> t.registerAccount
                        "forgot"   -> t.forgotPassword
                        else       -> "DLAVIE PORTAL"
                    },
                    color = GuideWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = GuideFont,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Subtitle (match web .portal-sub)
            // v7.9.78: GSAP-style staggered entrance
            val subAlpha by animateFloatAsState(
                if (staggers[2].value) 1f else 0f,
                tween(600, easing = GuideEase), label = "sub_alpha"
            )
            val subOffsetY by animateFloatAsState(
                if (staggers[2].value) 0f else 30f,
                tween(600, easing = GuideEase), label = "sub_offset"
            )
            Box(Modifier.graphicsLayer {
                this.alpha = subAlpha
                this.translationY = subOffsetY
            }) {
                Text(
                    when (mode) {
                        "chooser" -> "Sign in or connect your DLavie Launcher account\nto access all web features."
                        else -> ""
                    },
                    color = GuideSoftText,
                    fontSize = 13.sp,
                    fontFamily = GuideFont,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(48.dp))

            // ── Mode: CHOOSER — single "Connect via DLavie Portal" button ──
            // v8.0: Auth sekarang via web DLavie Portal. User login/register di web,
            // lalu klik "Connect to Launcher" → token dikirim ke launcher via deep link.
            // Launcher terima token → simpan di EncryptedSharedPreferences → auto-login.
            // Token persist across app updates (EncryptedSharedPreferences tidak dihapus saat update).
            //
            // v7.9.54: Tambah version info + update check + Connect Manual selalu visible
            if (mode == "chooser") {
                // ── v7.9.54: Version Info + Update Check ──
                val currentVersionCode = remember {
                    try {
                        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            pi.longVersionCode.toInt()
                        } else {
                            @Suppress("DEPRECATION")
                            pi.versionCode
                        }
                    } catch (_: Exception) { 0 }
                }
                val currentVersionName = remember {
                    try {
                        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
                        pi.versionName ?: "unknown"
                    } catch (_: Exception) { "unknown" }
                }

                // Check latest version from GitHub Releases API
                var latestVersionCode by remember { mutableStateOf<Int?>(null) }
                var latestVersionName by remember { mutableStateOf<String?>(null) }
                var latestApkUrl by remember { mutableStateOf<String?>(null) }
                var updateAvailable by remember { mutableStateOf(false) }
                var checkingUpdate by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        try {
                            val url = java.net.URL("https://api.github.com/repos/drmacze/F16-Launcher/releases/latest")
                            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                                connectTimeout = 10000
                                readTimeout = 15000
                                setRequestProperty("Accept", "application/vnd.github+json")
                                setRequestProperty("User-Agent", "DLavie-Launcher")
                                connect()
                            }
                            if (conn.responseCode == 200) {
                                val body = conn.inputStream.bufferedReader().use { it.readText() }
                                val json = org.json.JSONObject(body)
                                val tagName = json.optString("tag_name", "")  // e.g., "v219"
                                val tagNum = tagName.removePrefix("v").toIntOrNull() ?: 0
                                val releaseName = json.optString("name", "")
                                latestVersionCode = tagNum
                                latestVersionName = releaseName
                                updateAvailable = tagNum > currentVersionCode
                                // v7.9.55: Ambil APK URL dari assets untuk direct download
                                val assets = json.optJSONArray("assets")
                                if (assets != null) {
                                    for (i in 0 until assets.length()) {
                                        val asset = assets.optJSONObject(i)
                                        val name = asset?.optString("name", "") ?: ""
                                        val dlUrl = asset?.optString("browser_download_url", "") ?: ""
                                        if (name.endsWith(".apk", ignoreCase = true) && dlUrl.isNotBlank()) {
                                            latestApkUrl = dlUrl
                                            break
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // Network error — silent fail, just don't show update button
                        } finally {
                            checkingUpdate = false
                        }
                    }
                }

                // Version info display (top of page)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    color = Color(0x14000000),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.08f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color.White.copy(0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "DLavie Launcher v$currentVersionName (build $currentVersionCode)",
                                color = Color.White.copy(0.8f),
                                fontSize = 11.sp,
                                fontFamily = GuideFont,
                                fontWeight = FontWeight.Bold
                            )
                            if (checkingUpdate) {
                                Text(
                                    "Checking for updates...",
                                    color = Color.White.copy(0.4f),
                                    fontSize = 10.sp,
                                    fontFamily = GuideFont
                                )
                            } else if (updateAvailable && latestVersionCode != null) {
                                Text(
                                    "Update available: v$latestVersionName (build $latestVersionCode)",
                                    color = Color(0xFFFFAA00),
                                    fontSize = 10.sp,
                                    fontFamily = GuideFont,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (!checkingUpdate && latestVersionCode != null) {
                                Text(
                                    "Latest version",
                                    color = Color(0xFF00D26A),
                                    fontSize = 10.sp,
                                    fontFamily = GuideFont,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "Cannot check update (offline)",
                                    color = Color.White.copy(0.3f),
                                    fontSize = 10.sp,
                                    fontFamily = GuideFont
                                )
                            }
                        }
                    }
                }

                // Update button (hanya kalau update tersedia) — v7.9.55: with download progress + install
                if (updateAvailable && latestVersionCode != null) {
                    var downloadProgress by remember { mutableStateOf(0f) }
                    var downloadState by remember { mutableStateOf("idle") }  // idle | downloading | downloaded | installing | error
                    var downloadError by remember { mutableStateOf("") }
                    val scope = rememberCoroutineScope()

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        color = Color(0xFFFF5252).copy(0.1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(0.4f))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Update to Latest Version", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont)
                                    Text(
                                        "v$latestVersionName (build $latestVersionCode)",
                                        color = Color.White.copy(0.7f),
                                        fontSize = 10.sp,
                                        fontFamily = GuideFont
                                    )
                                }
                            }

                            // Progress bar (visible saat downloading/installing)
                            if (downloadState == "downloading" || downloadState == "installing") {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { if (downloadState == "downloading") downloadProgress else 1f },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = Color(0xFFFF5252),
                                        trackColor = Color.White.copy(0.1f)
                                    )
                                    Text(
                                        if (downloadState == "downloading")
                                            "Downloading... ${(downloadProgress * 100).toInt()}%"
                                        else "Installing APK...",
                                        color = Color.White.copy(0.7f),
                                        fontSize = 10.sp,
                                        fontFamily = GuideFont
                                    )
                                }
                            }

                            // Error message
                            if (downloadState == "error" && downloadError.isNotEmpty()) {
                                Text(
                                    downloadError,
                                    color = Color(0xFFFFAA00),
                                    fontSize = 10.sp,
                                    fontFamily = GuideFont
                                )
                            }

                            // Action button
                            Button(
                                onClick = {
                                    when (downloadState) {
                                        "idle", "error" -> {
                                            downloadState = "downloading"
                                            downloadProgress = 0f
                                            downloadError = ""
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    try {
                                                        // Fallback kalau latestApkUrl null (API gagal sebelumnya)
                                                        val apkUrl = latestApkUrl
                                                            ?: "https://github.com/drmacze/F16-Launcher/releases/latest/download/DLavie26-Launcher-debug.apk"
                                                        val apkFile = AppUpdateChecker.downloadApk(context, apkUrl) { progress ->
                                                            downloadProgress = progress
                                                        }
                                                        if (apkFile != null && apkFile.exists() && apkFile.length() > 1_000_000) {
                                                            downloadState = "installing"
                                                            withContext(Dispatchers.Main) {
                                                                // Small delay supaya user lihat 100% progress
                                                                kotlinx.coroutines.delay(500)
                                                                val installed = AppUpdateChecker.installApk(context, apkFile)
                                                                if (!installed) {
                                                                    downloadState = "error"
                                                                    downloadError = "Gagal buka installer. Buka browser untuk download manual."
                                                                }
                                                            }
                                                        } else {
                                                            downloadState = "error"
                                                            downloadError = "Download gagal — file tidak valid."
                                                        }
                                                    } catch (e: Exception) {
                                                        downloadState = "error"
                                                        downloadError = e.message ?: "Download gagal. Cek koneksi internet."
                                                    }
                                                }
                                            }
                                        }
                                        "downloading", "installing" -> {
                                            // Tidak bisa cancel mid-download (APK sedang ditulis)
                                        }
                                        "downloaded" -> {
                                            // Re-trigger install
                                            downloadState = "installing"
                                            scope.launch {
                                                val apkFile = java.io.File(context.cacheDir, "app-updates/dlavie-update.apk")
                                                val installed = AppUpdateChecker.installApk(context, apkFile)
                                                if (!installed) {
                                                    downloadState = "error"
                                                    downloadError = "Gagal buka installer."
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                enabled = downloadState != "downloading" && downloadState != "installing",
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5252),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFFFF5252).copy(0.5f),
                                    disabledContentColor = Color.White.copy(0.5f)
                                )
                            ) {
                                Text(
                                    when (downloadState) {
                                        "idle" -> "Download & Install"
                                        "downloading" -> "Downloading..."
                                        "installing" -> "Installing..."
                                        "downloaded" -> "Install APK"
                                        "error" -> "Retry Download"
                                        else -> "Download & Install"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GuideFont
                                )
                            }

                            // Fallback: open browser
                            Text(
                                "Tidak bisa update otomatis? Buka browser →",
                                color = Color.White.copy(0.5f),
                                fontSize = 10.sp,
                                fontFamily = GuideFont,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    val updateUrl = "https://github.com/drmacze/F16-Launcher/releases/latest"
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(updateUrl)
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    runCatching { context.startActivity(intent) }
                                }
                            )
                        }
                    }
                }

                // Info text
                Text(
                    "Sign in or register your DLavie account via the portal website.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontFamily = GuideFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
                Spacer(Modifier.height(16.dp))

                // 1. Connect via DLavie Portal (primary, white bg)
                AuthProviderButton(
                    label = "Connect to Portal",
                    icon = {
                        Icon(
                            Icons.Rounded.Public,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    onClick = {
                        // Open DLavie Portal website in browser
                        val portalUrl = "https://drmacze.github.io/dlavie-web/#/portal?from=launcher"
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(portalUrl)
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    },
                    enabled = !working
                )
                Spacer(Modifier.height(12.dp))

                // 2. Already connected? Check token
                Text(
                    "Already connected? The launcher will auto-login if your token is still valid.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    fontFamily = GuideFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                // ── v7.9.54: Connect Manual — SELALU VISIBLE (bukan toggle) ──
                // Untuk launcher versi lama yang tidak ada deep link handler,
                // user bisa copy URL connect dari web dan paste di sini.
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(0.1f)))
                    Text(
                        "OR CONNECT MANUALLY",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = GuideFont,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(0.1f)))
                }
                Spacer(Modifier.height(16.dp))

                var pasteUrl by remember { mutableStateOf("") }
                var pasteError by remember { mutableStateOf("") }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xF0111111),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.08f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.ContentPaste,
                                contentDescription = null,
                                tint = Color(0xFFFFAA00),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Connect Manual (for older versions)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = GuideFont,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "For launcher v218 and earlier without automatic deep link handling.\n\n" +
                            "How to use:\n" +
                            "1. Sign in at DLavie web\n" +
                            "2. Click \"Connect to DLavie\"\n" +
                            "3. After 2.5 seconds, the web will show a connect URL\n" +
                            "4. Copy that URL and paste it below\n" +
                            "5. Tap \"Connect Manual\"",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontFamily = GuideFont,
                            lineHeight = 14.sp
                        )
                        OutlinedTextField(
                            value = pasteUrl,
                            onValueChange = { pasteUrl = it; pasteError = "" },
                            placeholder = { Text("dlavie://connect?token=...&uid=...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
                                fontFamily = GuideFont,
                                color = Color.White
                            ),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedBorderColor = Color.White.copy(0.5f),
                                unfocusedBorderColor = Color.White.copy(0.2f),
                                focusedContainerColor = Color(0xFF1A1A1A),
                                unfocusedContainerColor = Color(0xFF1A1A1A)
                            )
                        )
                        if (pasteError.isNotEmpty()) {
                            Text(pasteError, color = Color(0xFFFF5252), fontSize = 11.sp, fontFamily = GuideFont)
                        }
                        Button(
                            onClick = {
                                val url = pasteUrl.trim()
                                if (url.isBlank()) {
                                    pasteError = "URL cannot be empty"
                                    return@Button
                                }
                                val uri = try { android.net.Uri.parse(url) } catch (e: Exception) {
                                    pasteError = "Invalid URL: ${e.message}"
                                    return@Button
                                }
                                val token = uri.getQueryParameter("token")
                                val uid = uri.getQueryParameter("uid")
                                val refresh = uri.getQueryParameter("refresh") ?: ""

                                if (token.isNullOrBlank() || uid.isNullOrBlank()) {
                                    pasteError = "URL does not contain valid token and uid"
                                    return@Button
                                }

                                context.getSharedPreferences("dlavie_auth_session", android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("access_token", token)
                                    .putString("refresh_token", refresh)
                                    .apply()
                                context.getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("access_token", token)
                                    .putString("refresh_token", refresh)
                                    .putString("user_id", uid)
                                    .putBoolean("portal_connected", true)
                                    .putString("portal_connected_at", System.currentTimeMillis().toString())
                                    .apply()

                                try { CommunityApi(context).loadMyProfile() } catch (_: Exception) {}

                                android.widget.Toast.makeText(
                                    context,
                                    "✓ DLavie Portal Connected! Welcome.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()

                                val intent = android.content.Intent(
                                    context,
                                    ModernLauncherActivity::class.java
                                ).addFlags(
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.finish()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFAA00),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Connect Manual", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont)
                        }
                    }
                }
            }

            // ── Mode: LOGIN / REGISTER / FORGOT (email form) ──
            if (mode != "chooser") {
                // Glass Card containing form
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xF0111111),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(0.08f))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // Tab switcher (Masuk | Daftar) — hidden in forgot mode
                        if (mode != "forgot") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0A0A0A), RoundedCornerShape(10.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (mode == "login") Color.White else Color.Transparent)
                                        .clickable { mode = "login"; message = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        t.login,
                                        color = if (mode == "login") Color.Black else GuideMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = GuideFont
                                    )
                                }
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (mode == "register") Color.White else Color.Transparent)
                                        .clickable { mode = "register"; message = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        t.register,
                                        color = if (mode == "register") Color.Black else GuideMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = GuideFont
                                    )
                                }
                            }
                        }

                        if (mode == "forgot") {
                            Text(
                                "Masukkan email akunmu. Kami akan kirim link reset password ke email tersebut.",
                                color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont, lineHeight = 17.sp
                            )
                        }

                        AuthField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            label = "Email",
                            leadingIcon = {
                                Icon(Icons.Rounded.Email, null, tint = GuideMuted, modifier = Modifier.size(18.dp))
                            }
                        )

                        if (mode != "forgot") {
                            AuthField(
                                value = password,
                                onValueChange = { password = it },
                                label = "Password",
                                leadingIcon = {
                                    Icon(Icons.Rounded.Lock, null, tint = GuideMuted, modifier = Modifier.size(18.dp))
                                },
                                isPassword = !showPass,
                                trailingLabel = if (showPass) "Sembunyikan" else "Tampilkan",
                                onTrailing = { showPass = !showPass }
                            )
                        }

                        if (mode == "register") {
                            AuthField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = "Konfirmasi Password",
                                leadingIcon = {
                                    Icon(Icons.Rounded.Lock, null, tint = GuideMuted, modifier = Modifier.size(18.dp))
                                },
                                isPassword = !showPass
                            )
                            AuthField(
                                value = username,
                                onValueChange = { raw ->
                                    username = raw.trim().lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(24)
                                },
                                label = "Username (3-24, a-z 0-9 _)",
                                leadingIcon = {
                                    Text("@", color = GuideMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont)
                                }
                            )
                            AuthField(
                                value = displayName,
                                onValueChange = { displayName = it.take(40) },
                                label = "Display Name (2-40)",
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, null, tint = GuideMuted, modifier = Modifier.size(18.dp))
                                }
                            )
                            // v7.9.17: Country picker DIHAPUS dari register — pindah ke onboarding modal
                        }

                        if (mode == "login") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text(
                                    "Lupa password?",
                                    color = GuideMuted,
                                    fontSize = 12.sp,
                                    fontFamily = GuideFont,
                                    modifier = Modifier.clickable { mode = "forgot"; message = ""; email = "" }
                                )
                            }
                        }

                        if (mode == "forgot") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Text(
                                    "← Kembali ke Login",
                                    color = GuideMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GuideFont,
                                    modifier = Modifier.clickable { mode = "login"; message = ""; email = "" }
                                )
                            }
                        }

                        AnimatedVisibility(visible = message.isNotBlank()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        (if (isSuccess) GuideGreen else GuideRed).copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        (if (isSuccess) GuideGreen else GuideRed).copy(alpha = 0.20f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isSuccess) GuideGreen else GuideRed,
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                )
                                Text(
                                    message,
                                    color = if (isSuccess) GuideGreen else GuideRed,
                                    fontSize = 12.sp,
                                    fontFamily = GuideFont,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        val canSubmit = when (mode) {
                            "forgot"  -> !working && email.isNotBlank() && email.contains("@")
                            "login"   -> !working && email.isNotBlank() && password.length >= 6
                            "register"-> !working && email.isNotBlank() && email.contains("@") &&
                                         password.length >= 6 && password == confirmPassword &&
                                         username.matches(Regex("[a-zA-Z0-9_]{3,24}")) &&
                                         displayName.trim().length >= 2
                            else -> false
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    working = true; message = ""
                                    val result = withContext(Dispatchers.IO) {
                                        when (mode) {
                                            "login"    -> loginWithPassword(context, email, password)
                                            "register" -> registerWithUsernamePassword(context, email, password, username.trim(), displayName.trim())
                                            "forgot"   -> {
                                                try {
                                                    val msg = AuthManager.requestPasswordReset(email)
                                                    AuthResult(null, msg)
                                                } catch (e: Exception) {
                                                    AuthResult(null, "Error: ${e.message ?: "gagal kirim email"}")
                                                }
                                            }
                                            else -> AuthResult(null, "Mode tidak dikenal")
                                        }
                                    }
                                    working = false
                                    isSuccess = result.session != null || result.message.startsWith("OK")
                                    message = result.message
                                    // v6.8.3: clear guest flag on successful login/register
                                    if (result.session != null) {
                                        val api = CommunityApi(context)
                                        api.clearGuest()
                                        when (mode) {
                                            "login"    -> Telemetry.track(context, Telemetry.EVT_LOGIN,    mapOf("email" to email.trim()))
                                            "register" -> Telemetry.track(context, Telemetry.EVT_REGISTER, mapOf("email" to email.trim(), "username" to username.trim()))
                                        }
                                    }
                                    result.session?.let(onLoggedIn)

                                    if (mode == "forgot" && isSuccess) {
                                        kotlinx.coroutines.delay(2500)
                                        mode = "login"
                                        password = ""
                                        message = ""
                                    }
                                }
                            },
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.White.copy(0.2f),
                                disabledContentColor = GuideMuted
                            )
                        ) {
                            if (working) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                when {
                                    working -> "Memproses..."
                                    mode == "login"    -> t.login
                                    mode == "register" -> t.register
                                    mode == "forgot"   -> t.forgotPassword
                                    else -> "→"
                                },
                                fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont
                            )
                        }
                    }
                }

                // Back to chooser link
                Spacer(Modifier.height(16.dp))
                Text(
                    "← Kembali",
                    color = GuideMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GuideFont,
                    modifier = Modifier.clickable {
                        mode = "chooser"; message = ""; email = ""; password = ""
                    }
                )
            }

            // ── Legal text (bottom) — v7.9.55: clickable Terms & Privacy links ──
            Spacer(Modifier.height(32.dp))
            // Split legalNotice jadi 3 bagian: prefix, terms link, mid, privacy link, suffix
            // Support multiple languages: detect "Ketentuan Layanan" / "Terms of Service" / etc.
            val termsUrl = "https://drmacze.github.io/dlavie-web/#/terms"
            val privacyUrl = "https://drmacze.github.io/dlavie-web/#/privacy"

            // Detect language-specific terms/privacy phrases
            val termsPhrases = listOf(
                "Ketentuan Layanan", "Terms of Service", "Terma Perkhidmatan",
                "Termos de Serviço", "Términos de Servicio", "Nutzungsbedingungen",
                "Conditions d'utilisation", "利用規約", "服务条款"
            )
            val privacyPhrases = listOf(
                "Kebijakan Privasi", "Privacy Policy", "Dasar Privasi",
                "Política de Privacidade", "Política de Privacidad", "Datenschutzrichtlinie",
                "Politique de confidentialité", "プライバシーポリシー", "隐私政策"
            )

            val legalText = t.legalNotice
            val termsPhrase = termsPhrases.firstOrNull { legalText.contains(it) }
            val privacyPhrase = privacyPhrases.firstOrNull { legalText.contains(it) }

            if (termsPhrase != null && privacyPhrase != null) {
                val termsStart = legalText.indexOf(termsPhrase)
                val termsEnd = termsStart + termsPhrase.length
                val privacyStart = legalText.indexOf(privacyPhrase)
                val privacyEnd = privacyStart + privacyPhrase.length

                // Build annotated string dengan clickable spans
                val annotated = buildAnnotatedString {
                    append(legalText.substring(0, termsStart))
                    withStyle(
                        SpanStyle(
                            color = Color.White.copy(alpha = 0.7f),
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        // Tag untuk detect tap
                        pushStringAnnotation(tag = "URL", annotation = termsUrl)
                        append(termsPhrase)
                        pop()
                    }
                    append(legalText.substring(termsEnd, privacyStart))
                    withStyle(
                        SpanStyle(
                            color = Color.White.copy(alpha = 0.7f),
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        pushStringAnnotation(tag = "URL", annotation = privacyUrl)
                        append(privacyPhrase)
                        pop()
                    }
                    append(legalText.substring(privacyEnd))
                }

                ClickableText(
                    text = annotated,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 10.sp,
                        fontFamily = GuideFont,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    onClick = { offset ->
                        // Cek apakah user tap di area terms/privacy link
                        annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(annotation.item)
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }
                            }
                    }
                )
            } else {
                // Fallback: plain Text kalau phrase tidak ketemu
                Text(
                    legalText,
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 10.sp,
                    fontFamily = GuideFont,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // ── Working overlay (top-center spinner) ──
        if (working && mode == "chooser") {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Memuat...", color = Color.White, fontSize = 12.sp, fontFamily = GuideFont)
                }
            }
        }
    }
}

// ─── v6.8.3: Auth provider button (Grok-style stacked) ──────────────────────
@Composable
private fun AuthProviderButton(
    label: String,
    icon: @Composable () -> Unit,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = if (borderColor != null) BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GuideFont
            )
        }
    }
}

// ─── v6.8.3: Google "G" logo icon (multicolor, matches Google brand) ────────
// Note: This is the official Google G logo colors. Even though the app is
// monochrome, the Google G icon itself uses Google's brand colors for brand
// recognition (standard practice — same as TapTap, Steam, Discord, etc.).
@Composable
private fun GoogleIcon() {
    val s = 20.dp
    Box(Modifier.size(s), contentAlignment = Alignment.Center) {
        // White "G" on white button — but Google G is multicolor. We'll draw
        // a simple white "G" text since the button bg is already white (Google
        // brand on white = use the colored G; on dark = use white G).
        // For our white button: use the multicolor Google G via Canvas.
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) * 0.42f

            // Draw 4 colored arcs (Google brand colors):
            // Top: Blue (#4285F4), Right: Red (#EA4335), Bottom: Yellow (#FBBC05), Left: Green (#34A853)
            // Actually Google G is: Blue top-left, Red top-right, Yellow bottom-right, Green bottom-left
            // But simpler: draw the "G" shape as 4 arcs.
            val stroke = r * 0.32f

            // Simplified: draw a white circle ring with "G" cutout — actually just draw the multicolor G
            // Use drawArc for each quadrant
            val colors = listOf(
                Color(0xFFEA4335),  // red - top-right
                Color(0xFFFBBC05),  // yellow - bottom-right
                Color(0xFF34A853),  // green - bottom-left
                Color(0xFF4285F4)   // blue - top-left
            )
            val startAngles = listOf(0f, 90f, 180f, 270f)  // 4 quadrants
            for (i in 0..3) {
                drawArc(
                    color = colors[i],
                    startAngle = startAngles[i] - 45f,  // offset so arcs meet at 45° diagonals
                    sweepAngle = 90f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                )
            }
        }
    }
}

// ─── v6.8.4: Start Google OAuth via Supabase with deep-link redirect ────────
// Supabase Auth /auth/v1/authorize?provider=google&redirect_to=dlavie://auth-callback
// User signs in with Google in Custom Tabs browser → Supabase redirects ke
// dlavie://auth-callback#access_token=...&refresh_token=... → Android OS
// opens DLavieGuidedActivity via intent-filter → handleDeepLink() parses token.
//
// PREREQUISITE (one-time setup by user):
//   1. Google Cloud Console → Create OAuth 2.0 Client ID (Web application)
//      - Authorized redirect URI: https://lvmucsxbmadtsgrxuwmo.supabase.co/auth/v1/callback
//   2. Supabase Dashboard → Auth → Providers → Google
//      - Enable Google
//      - Paste Client ID + Client Secret
//      - (redirect_to dlavie://auth-callback already added to URL allow list)
//
// Run helper script /home/z/my-project/scripts/configure-google-oauth.sh
// with CLIENT_ID and CLIENT_SECRET to configure via Management API.
private fun startGoogleOAuth(context: Context): String {
    return try {
        // v6.8.4: redirect ke deep link dlavie://auth-callback (bukan /auth/v1/callback)
        val redirect = "dlavie://auth-callback"
        val url = "${SUPABASE_URL}/auth/v1/authorize?provider=google&redirect_to=${java.net.URLEncoder.encode(redirect, "UTF-8")}"
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Try Custom Tabs first (preferred — stays in-app)
        try {
            val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.intent.data = android.net.Uri.parse(url)
            context.startActivity(customTabsIntent.intent)
        } catch (_: Exception) {
            // Fallback: open in default browser
            context.startActivity(intent)
        }
        "OK: Membuka Google login. Setelah login, Anda akan otomatis kembali ke DLavie."
    } catch (e: Exception) {
        "Error: ${e.message ?: "gagal buka Google OAuth"}"
    }
}

/**
 * v4.0 monochrome AuthField — OutlinedTextField dengan leading icon, optional
 * password toggle (trailing text), white-on-dark theme, subtle white border.
 * Replaces the old AuthInputField (prefix-string based) dengan leading icon.
 */
@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    trailingLabel: String? = null,
    onTrailing: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = leadingIcon,
        trailingIcon = if (trailingLabel != null && onTrailing != null) {
            {
                Text(
                    trailingLabel,
                    color = GuideMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GuideFont,
                    modifier = Modifier
                        .clickable { onTrailing() }
                        .padding(end = 12.dp)
                )
            }
        } else null,
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color.White.copy(0.3f),
            unfocusedBorderColor = Color.White.copy(0.1f),
            cursorColor          = Color.White,
            focusedTextColor     = Color.White,
            unfocusedTextColor   = Color.White,
            focusedLabelColor    = Color.White.copy(0.6f),
            unfocusedLabelColor  = GuideMuted,
            focusedLeadingIconColor    = GuideMuted,
            unfocusedLeadingIconColor  = GuideMuted,
            focusedTrailingIconColor   = GuideMuted,
            unfocusedTrailingIconColor = GuideMuted
        )
    )
}

/**
 * Country picker dropdown for the register form.
 * v4.0 monochrome — uses leading Icon (Public) + trailing ArrowDropDown,
 * white-on-dark theme matching AuthField. Dropdown list tetap monochrome
 * (selected = white highlight, others = white text).
 */
@Composable
private fun CountryPickerDropdown(
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value      = selected,
            onValueChange = { /* read-only */ },
            readOnly   = true,
            enabled    = false,
            label      = { Text("Negara", fontSize = 12.sp, maxLines = 1) },
            leadingIcon = {
                Icon(
                    imageVector        = Icons.Rounded.Public,
                    contentDescription = null,
                    tint               = GuideMuted,
                    modifier           = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector        = Icons.Rounded.ArrowDropDown,
                    contentDescription = "Pilih negara",
                    tint               = GuideMuted,
                    modifier           = Modifier
                        .size(22.dp)
                        .clickable { expanded = true }
                )
            },
            singleLine  = true,
            modifier    = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape       = RoundedCornerShape(12.dp),
            colors      = OutlinedTextFieldDefaults.colors(
                disabledBorderColor         = Color.White.copy(0.1f),
                disabledTextColor           = Color.White,
                disabledLabelColor          = GuideMuted,
                disabledTrailingIconColor   = GuideMuted,
                disabledLeadingIconColor    = GuideMuted
            )
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(Color(0xFF111111), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp))
        ) {
            COUNTRY_LIST.forEach { name ->
                val isSelected = name == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            name,
                            color      = if (isSelected) Color.White else Color.White.copy(0.7f),
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = GuideFont
                        )
                    },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    },
                    modifier = Modifier.background(if (isSelected) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun GuidedLauncherScreen(session: AuthSession, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(GuidedTab.Home) }
    var faqOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(bottom = 104.dp)) {
            when (tab) {
                GuidedTab.Home -> GuidedHomeScreen(session, openData = { tab = GuidedTab.Data }, openUpdate = { tab = GuidedTab.Update })
                GuidedTab.Data -> GuidedDataScreen(openUpdate = { tab = GuidedTab.Update })
                GuidedTab.Update -> GuidedUpdateScreen(session)
                GuidedTab.Me -> GuidedProfileScreen(session, onLogout)
            }
        }
        GuidedHelpButton(expanded = faqOpen, onClick = { faqOpen = !faqOpen }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 96.dp))
        if (faqOpen) GuidedFaqPanel(onClose = { faqOpen = false }, modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 118.dp))
        GuidedBottomNav(tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun GuidedHomeScreen(session: AuthSession, openData: () -> Unit, openUpdate: () -> Unit) {
    val context = LocalContext.current
    var bootstrap by remember { mutableStateOf(BootstrapState()) }
    LaunchedEffect(session.accessToken) { bootstrap = withContext(Dispatchers.IO) { loadBootstrap(session) } }
    val marker = guidedReadMarkerSmart()
    GuidedPage {
        GuidedHeaderCard(session, bootstrap, marker)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GuidedMiniChip("Game", if (guidedIsGameInstalled(context)) "Installed" else "Missing", "🎮", if (guidedIsGameInstalled(context)) GuideGreen else GuideRed, Modifier.weight(1f))
            GuidedMiniChip("Shizuku", guidedShizukuState(), "🛡", if (guidedShizukuState() == "Ready") GuideGreen else GuideCyan, Modifier.weight(1f))
            GuidedMiniChip("Update", if (bootstrap.updateAvailable) "v${bootstrap.latestVersionCode}" else LOCAL_VERSION_NAME, "🔄", if (bootstrap.updateAvailable) GuideCyan else GuideGreen, Modifier.weight(1f))
        }
        GuidedQuickSteps(marker, bootstrap, openData, openUpdate) { guidedLaunchGame(context) }
        GuidedPrimaryCta(if (marker.startsWith("v26")) "Mainkan Game" else "Install Full Data", if (marker.startsWith("v26")) "Data siap. Buka FIFA 16." else "Base data belum lengkap.", if (marker.startsWith("v26")) "▶" else "⬇", if (marker.startsWith("v26")) { { guidedLaunchGame(context) } } else openData)
        if (bootstrap.notices.isNotEmpty()) GuidedNoticeCard(bootstrap.notices)
        if (bootstrap.error.isNotBlank()) GuidedErrorCard(bootstrap.error)
    }
}

@Composable
private fun GuidedDataScreen(openUpdate: () -> Unit) {
    val context = LocalContext.current
    val marker = guidedReadMarkerSmart()
    GuidedPage {
        GuidedPageTitle("Data", "Base data wajib siap sebelum update patch.")
        GuidedPanel(border = if (marker.startsWith("v26")) GuideGreen else GuideCyan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📁", fontSize = 34.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Base Data", color = GuideWhite, fontSize = 25.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                    Text(if (marker.startsWith("v26")) "Full data terdeteksi." else "Full data belum lengkap.", color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont)
                }
                GuidedPill(if (marker.startsWith("v26")) "READY" else "BELUM", if (marker.startsWith("v26")) GuideGreen else GuideRed)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedInfoBox("APK FIFA", if (guidedIsGameInstalled(context)) "Terpasang" else "Belum", Modifier.weight(1f))
                GuidedInfoBox("Marker", guidedShortMarker(marker), Modifier.weight(1f))
            }
            GuidedActionButton(if (marker.startsWith("v26")) "Ke Update" else "Buka Installer Data", if (marker.startsWith("v26")) GuideCyan else GuideGreen, if (marker.startsWith("v26")) openUpdate else { { guidedOpenClassicInstaller(context) } }, true)
        }
        GuidedShizukuCard()
    }
}

@Composable
private fun GuidedUpdateScreen(session: AuthSession) {
    var bootstrap by remember { mutableStateOf(BootstrapState()) }
    LaunchedEffect(session.accessToken) { bootstrap = withContext(Dispatchers.IO) { loadBootstrap(session) } }
    GuidedPage {
        GuidedPageTitle("Update", "Versi dan patch dibaca dari backend DLavie.")
        GuidedPanel(border = if (bootstrap.updateAvailable) GuideCyan else GuideGreen) {
            Text("Patch Update", color = GuideWhite, fontSize = 25.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            Text(if (bootstrap.updateAvailable) "Update tersedia dari backend." else "Versi kamu sudah terbaru / belum ada release aktif.", color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedInfoBox("Local", LOCAL_VERSION_NAME, Modifier.weight(1f))
                GuidedInfoBox("Latest", bootstrap.latestVersionName, Modifier.weight(1f))
            }
            if (bootstrap.patchName.isNotBlank()) GuidedInfoBox("Patch ZIP", bootstrap.patchName, Modifier.fillMaxWidth())
            GuidedActionButton(if (bootstrap.updateAvailable) "Download Patch" else "Check Update", if (bootstrap.updateAvailable) GuideCyan else GuideGreen, {}, enabled = false)
            Text("Download/apply patch akan disambungkan setelah login foundation ini build hijau.", color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont)
        }
    }
}

@Composable
private fun GuidedProfileScreen(session: AuthSession, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var ticketMessage by remember { mutableStateOf("") }
    var ticketResult by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    GuidedPage {
        GuidedPageTitle("Profile", "Akun login, ticket, dan status DLavie.")
        GuidedPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(74.dp).background(Brush.linearGradient(listOf(Color(0xFF0A0A0A), Color(0xFF1A1A1A))), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                    Text(session.email.firstOrNull()?.uppercase() ?: "D", color = GuideGreen, fontSize = 31.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(session.email, color = GuideWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont)
                    Text("Login aktif • no guest mode", color = GuideMuted, fontSize = 13.sp, maxLines = 1, fontFamily = GuideFont)
                }
                GuidedPill("USER", GuideGreen)
            }
            GuidedActionButton("Logout", GuideRed, onLogout, true)
        }
        GuidedPanel {
            Text("Support Ticket", color = GuideWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            Text("Ticket hanya bisa dibuat setelah login. Pesan masuk ke backend support.", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont)
            OutlinedTextField(value = ticketMessage, onValueChange = { ticketMessage = it.take(1000) }, label = { Text("Tulis masalah") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            GuidedActionButton("Buat Ticket", GuideGreen, {
                scope.launch {
                    working = true
                    ticketResult = withContext(Dispatchers.IO) { createSupportTicket(session, ticketMessage) }
                    working = false
                    if (ticketResult.startsWith("OK")) ticketMessage = ""
                }
            }, enabled = !working && ticketMessage.isNotBlank())
            if (ticketResult.isNotBlank()) Text(ticketResult, color = if (ticketResult.startsWith("OK")) GuideGreen else GuideRed, fontSize = 12.sp, fontFamily = GuideFont)
        }
        GuidedFaqFullCard()
    }
}

@Composable private fun GuidedPage(content: @Composable () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() } }
@Composable private fun GuidedPanel(border: Color = GuideBorder, content: @Composable () -> Unit) { Surface(modifier = Modifier.fillMaxWidth(), color = GuideCard, shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, border.copy(alpha = 0.85f))) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } } }
@Composable private fun GuidedHeaderCard(session: AuthSession, bootstrap: BootstrapState, marker: String) { GuidedPanel { Row(verticalAlignment = Alignment.CenterVertically) { DLavieLogoCover(size = 72.dp, fontSize = 29.sp, shape = RoundedCornerShape(24.dp)); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text("DLavie 26", color = GuideWhite, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(session.email, color = GuideMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) }; GuidedPill(if (marker.startsWith("v26")) "READY" else if (bootstrap.maintenance) "MAINT" else "SETUP", if (marker.startsWith("v26")) GuideGreen else GuideCyan) } } }
@Composable private fun GuidedQuickSteps(marker: String, bootstrap: BootstrapState, openData: () -> Unit, openUpdate: () -> Unit, launch: () -> Unit) { GuidedPanel { Text("⚡ Langkah Cepat", color = GuideWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont); Text("Ikuti urutan agar patch aktif dengan benar.", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont); GuidedStepRow(1, "Cek Base Data", "Pastikan OBB dan marker siap.", if (marker.startsWith("v26")) "OK" else "WAJIB", "🔎", if (marker.startsWith("v26")) GuideGreen else GuideAmber, openData); GuidedStepRow(2, "Install Full Data", "Unduh dan pasang data utama.", if (marker.startsWith("v26")) "SELESAI" else "LANJUT", "⬇", GuideGreen, openData); GuidedStepRow(3, "Update Patch", "Cek versi dari backend.", if (bootstrap.updateAvailable) "TERSEDIA" else "CEK", "🌐", GuideCyan, openUpdate); GuidedStepRow(4, "Mainkan Game", "Launch setelah data siap.", if (marker.startsWith("v26")) "READY" else "NANTI", "🚀", Color(0xFFB783FF), launch) } }
@Composable private fun GuidedStepRow(no: Int, title: String, subtitle: String, chip: String, icon: String, color: Color, onClick: () -> Unit) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(74.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xBB111111), contentColor = GuideWhite), contentPadding = PaddingValues(horizontal = 12.dp)) { Box(Modifier.size(28.dp).background(color, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(no.toString(), color = Color(0xFF001407), fontWeight = FontWeight.Black, fontSize = 13.sp) }; Spacer(Modifier.width(10.dp)); Text(icon, fontSize = 23.sp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(subtitle, color = GuideMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) }; GuidedPill(chip, color); Text("›", color = GuideMuted, fontSize = 20.sp) } }
@Composable private fun GuidedPrimaryCta(title: String, subtitle: String, icon: String, onClick: () -> Unit) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = GuideGreen, contentColor = Color(0xFF001407)), contentPadding = PaddingValues(horizontal = 18.dp)) { Text(icon, fontSize = 23.sp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) }; Text("→", fontSize = 24.sp, fontWeight = FontWeight.Black) } }
@Composable private fun GuidedMiniChip(title: String, value: String, icon: String, color: Color, modifier: Modifier) { Surface(modifier = modifier.height(74.dp), color = Color(0xCC111111), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) { Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) { Text(icon, fontSize = 18.sp); Text(title, color = GuideMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1); Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun GuidedInfoBox(label: String, value: String, modifier: Modifier) { Surface(modifier = modifier.height(78.dp), color = Color(0xAA0A0A0A), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) { Text(label, color = GuideMuted, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(value, color = GuideWhite, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) } } }
@Composable private fun GuidedActionButton(label: String, color: Color, onClick: () -> Unit, enabled: Boolean = true) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(58.dp), enabled = enabled, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF001407), disabledContainerColor = Color(0xFF2A2A2A), disabledContentColor = GuideMuted)) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun GuidedSmallAction(label: String, color: Color, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) { Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF001407), disabledContainerColor = Color(0xFF2A2A2A), disabledContentColor = GuideMuted), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) } }
@Composable private fun GuidedPill(text: String, color: Color) { Surface(color = color.copy(alpha = 0.16f), border = BorderStroke(1.dp, color.copy(alpha = 0.58f)), shape = RoundedCornerShape(999.dp)) { Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), maxLines = 1, fontFamily = GuideFont) } }
@Composable private fun GuidedPageTitle(title: String, subtitle: String) { Column { Text(title, color = GuideWhite, fontSize = 34.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(subtitle, color = GuideMuted, fontSize = 14.sp, maxLines = 2, fontFamily = GuideFont) } }
@Composable private fun GuidedNoticeCard(notices: List<String>) { GuidedPanel(border = GuideCyan) { Text("Developer Notice", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black); notices.take(3).forEach { Text("• $it", color = GuideMuted, fontSize = 13.sp) } } }
@Composable private fun GuidedErrorCard(error: String) { GuidedPanel(border = GuideRed) { Text("Backend Error", color = GuideRed, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(error, color = GuideMuted, fontSize = 12.sp) } }
@Composable private fun GuidedShizukuCard() { val status = guidedShizukuState(); GuidedPanel { Text("🛡 Shizuku Access", color = GuideWhite, fontSize = 23.sp, fontWeight = FontWeight.Black); Text(if (status == "Ready") "Siap untuk apply patch otomatis." else "Buka Shizuku, aktifkan Start, lalu izinkan DLavie.", color = GuideMuted, fontSize = 13.sp); GuidedActionButton(if (status == "Ready") "Shizuku Ready" else "Izinkan / Cek Shizuku", if (status == "Ready") GuideGreen else GuideCyan, { guidedRequestShizuku() }, status != "Ready") } }
@Composable private fun GuidedFaqFullCard() { GuidedPanel { Text("Topik Bantuan", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont); GuidedFaqLine("🛡 Cara aktifkan Shizuku", "Buka Shizuku → Pairing/Start → kembali ke DLavie → Izinkan akses."); GuidedFaqLine("📁 Apa itu Base Data?", "OBB dan data utama FIFA 16. Patch tidak bekerja kalau base belum lengkap."); GuidedFaqLine("🌐 Cara cek versi", "Buka Update. Local dan Latest dibandingkan dari backend.") } }
@Composable private fun GuidedFaqLine(title: String, body: String) { Surface(color = Color(0xAA0B100F), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, GuideBorder)) { Column(Modifier.padding(12.dp)) { Text(title, color = GuideWhite, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont); Text(body, color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont) } } }
@Composable private fun GuidedHelpButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier) { Row(modifier, verticalAlignment = Alignment.CenterVertically) { if (!expanded) Surface(color = Color(0xEE101211), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, GuideBorder)) { Text("Butuh bantuan?", color = GuideWhite, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), fontSize = 12.sp, fontFamily = GuideFont) }; Spacer(Modifier.width(8.dp)); Button(onClick = onClick, modifier = Modifier.size(68.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = GuideGreen, contentColor = Color(0xFF001407)), contentPadding = PaddingValues(0.dp)) { Text(if (expanded) "×" else "🤖", fontSize = if (expanded) 28.sp else 25.sp, fontWeight = FontWeight.Black) } } }
@Composable private fun GuidedFaqPanel(onClose: () -> Unit, modifier: Modifier) { Surface(modifier = modifier.fillMaxWidth(), color = Color(0xF00D0F0E), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, GuideBorder), shadowElevation = 20.dp) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Asisten DLavie", color = GuideWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), fontFamily = GuideFont); GuidedSmallAction("Tutup", GuideRed, onClose, true, Modifier.width(86.dp)) }; GuidedFaqFullCard() } } }
@Composable private fun GuidedBottomNav(selected: GuidedTab, onSelect: (GuidedTab) -> Unit, modifier: Modifier) { Surface(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color(0xF00B0C0C), shape = RoundedCornerShape(34.dp), border = BorderStroke(1.dp, GuideBorder), shadowElevation = 18.dp) { Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { GuidedTab.values().forEach { item -> val active = item == selected; Button(onClick = { onSelect(item) }, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF0E3A22) else Color.Transparent, contentColor = if (active) GuideGreen else GuideMuted), contentPadding = PaddingValues(0.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(item.icon, fontSize = 19.sp); Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont) } } } } } }

private fun loadSession(context: Context): AuthSession? { val p = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE); val token = p.getString(PREF_TOKEN, null) ?: return null; val refresh = p.getString(PREF_REFRESH, "") ?: ""; val email = p.getString(PREF_EMAIL, "") ?: ""; return AuthSession(token, refresh, email) }
private fun saveSession(context: Context, s: AuthSession) { context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE).edit().putString(PREF_TOKEN, s.accessToken).putString(PREF_REFRESH, s.refreshToken).putString(PREF_EMAIL, s.email).apply() }
private fun clearSession(context: Context) { context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE).edit().clear().apply() }
private fun userIdFromJwt(token: String): String { return try { val payload = token.split(".").getOrNull(1) ?: return ""; val padded = payload + "=".repeat((4 - payload.length % 4) % 4); val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE); JSONObject(String(decoded)).optString("sub", "") } catch (_: Exception) { "" } }
private fun syncToCommunityPrefs(context: Context, session: AuthSession) { val userId = userIdFromJwt(session.accessToken); context.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE).edit().putString("access_token", session.accessToken).putString("refresh_token", session.refreshToken).putString("user_id", userId).apply() }
private fun loginWithPassword(context: Context, email: String, password: String): AuthResult = authPassword(context, "/auth/v1/token?grant_type=password", email, password, "OK: login berhasil.")
private fun registerWithPassword(context: Context, email: String, password: String): AuthResult = authPassword(context, "/auth/v1/signup", email, password, "OK: akun dibuat.")
private fun registerWithUsernamePassword(context: Context, email: String, password: String, username: String, displayName: String): AuthResult = try {
    // v7.9.17: country dihapus dari register — pindah ke onboarding modal
    val meta = JSONObject().put("username", username).put("display_name", displayName)
    val signupBody = JSONObject().put("email", email).put("password", password).put("data", meta)
    val json = httpPost("/auth/v1/signup", null, signupBody)
    val token = json.optString("access_token", "")
    val refresh = json.optString("refresh_token", "")
    val userObj = json.optJSONObject("user")
    val userEmail = userObj?.optString("email", email) ?: email
    val userId = userObj?.optString("id", "") ?: ""
    if (token.isBlank()) {
        AuthResult(null, "Akun dibuat! Cek email untuk verifikasi, lalu Login.")
    } else {
        val session = AuthSession(token, refresh, userEmail)
        saveSession(context, session)
        syncToCommunityPrefs(context, session)

        // ── FIX (Bug 1): Populate CommunityApi prefs via login + loadMyProfile ──
        // Sebelumnya, POST /rest/v1/profiles dengan return=minimal tidak return data,
        // dan RPC dlavie_v2_create_profile_if_missing tidak ada di project baru,
        // jadi CommunityApi.username() / displayName() tetap empty setelah register.
        //
        // Sekarang: call CommunityApi.login(email, password, username, displayName, "")
        //   - storeSessionIfPresent → save token + user_id ke dlavie_community prefs
        //   - loadMyProfile()       → fetch profile dari DB, save username/display_name/role ke prefs
        //
        // Karena trigger handle_new_user butuh waktu untuk fire (async), retry 3x dengan delay.
        var profileLoaded = false
        try {
            val api = CommunityApi(context)
            for (attempt in 1..3) {
                try {
                    api.login(email, password, username, displayName, "")
                    profileLoaded = true
                    break
                } catch (_: Exception) {
                    if (attempt < 3) Thread.sleep(500L)
                }
            }
            if (!profileLoaded) {
                // Fallback 1: manual ensureMyProfile
                try { api.ensureMyProfile(username, displayName, "") } catch (_: Exception) { }
            }
            // Fallback 2: kalau profile masih belum ter-load, save prefs manual supaya
            // CommunityApi.username() / displayName() minimal tidak empty.
            if (!profileLoaded) {
                val prefs = context.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("username", username)
                    .putString("display_name", displayName)
                    .putString("access_token", token)
                    .putString("refresh_token", refresh)
                    .putString("user_id", userId)
                    .apply()
            }
            // v7.9.17: country update dihapus dari sini — pindah ke onboarding modal
        } catch (_: Exception) {
            // CommunityApi totally broken — last resort: manual prefs save.
            val prefs = context.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("username", username)
                .putString("display_name", displayName)
                .putString("access_token", token)
                .putString("refresh_token", refresh)
                .putString("user_id", userId)
                .apply()
        }
        AuthResult(session, "OK: akun dibuat. Selamat datang, $displayName!")
    }
} catch (e: Exception) {
    // v7.2.5: Show clean error message without "Error:" prefix or raw JSON.
    // parseError() already converts Supabase errors to human-friendly Indonesian.
    val cleanMsg = e.message?.let { msg ->
        // If message still contains raw JSON/code (parseError failed somewhere),
        // run parseError again as fallback.
        if (msg.contains("{") || msg.contains("error_code") || msg.contains("code:")) {
            parseError(msg)
        } else {
            msg
        }
    } ?: "Pendaftaran gagal. Coba lagi."
    AuthResult(null, cleanMsg)
}
private fun authPassword(context: Context, path: String, email: String, password: String, okMessage: String): AuthResult = try {
    val json = httpPost(path, null, JSONObject().put("email", email).put("password", password))
    val token = json.optString("access_token", "")
    val refresh = json.optString("refresh_token", "")
    val userEmail = json.optJSONObject("user")?.optString("email", email) ?: email
    val userId = json.optJSONObject("user")?.optString("id", "") ?: ""
    if (token.isBlank()) AuthResult(null, "Akun dibuat. Cek email lalu login.") else {
        val session = AuthSession(token, refresh, userEmail)
        saveSession(context, session)
        syncToCommunityPrefs(context, session)

        // ── FIX (Bug 1): Populate CommunityApi prefs via login + loadMyProfile ──
        // Sebelumnya hanya call RPC dlavie_v2_create_profile_if_missing yang tidak ada
        // di project baru, jadi prefs dlavie_community (username/display_name) tetap empty.
        // Sekarang: CommunityApi.login() → storeSessionIfPresent + loadMyProfile.
        // Retry 3x karena trigger handle_new_user butuh waktu untuk fire (async).
        try {
            val api = CommunityApi(context)
            var loaded = false
            for (attempt in 1..3) {
                try {
                    api.login(email, password, "", "", "")
                    loaded = true
                    break
                } catch (_: Exception) {
                    if (attempt < 3) Thread.sleep(500L)
                }
            }
            if (!loaded) {
                // Trigger profile belum fire — save prefs minimal manual supaya user_id minimal ter-set.
                val prefs = context.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("access_token", token)
                    .putString("refresh_token", refresh)
                    .putString("user_id", userId)
                    .apply()
            }
        } catch (_: Exception) {
            // CommunityApi totally broken — last resort: manual prefs save.
            val prefs = context.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("access_token", token)
                .putString("refresh_token", refresh)
                .putString("user_id", userId)
                .apply()
        }
        AuthResult(session, okMessage)
    }
} catch (e: Exception) {
    // v7.2.5: Clean error message (parseError already converts Supabase JSON to human-friendly)
    val cleanMsg = e.message?.let { msg ->
        if (msg.contains("{") || msg.contains("error_code") || msg.contains("code:")) {
            parseError(msg)
        } else {
            msg
        }
    } ?: "Login gagal. Coba lagi."
    AuthResult(null, cleanMsg)
}
private fun loadBootstrap(session: AuthSession): BootstrapState = try { val json = httpPost("/rest/v1/rpc/dlavie_v2_get_launcher_bootstrap", session.accessToken, JSONObject().put("p_local_version_code", LOCAL_VERSION_CODE)); parseBootstrap(json) } catch (e: Exception) { BootstrapState(loaded = false, error = e.message ?: "backend gagal") }
private fun parseBootstrap(json: JSONObject): BootstrapState { val profile = json.optJSONObject("profile") ?: JSONObject(); val update = json.optJSONObject("update") ?: JSONObject(); val patch = update.optJSONObject("patch"); val maintenance = json.optJSONObject("maintenance") ?: JSONObject(); val notices = jsonArrayObjectsToTitles(json.optJSONArray("notices")); return BootstrapState(displayName = profile.optString("display_name", "DLavie Player"), role = profile.optString("role", "user"), maintenance = maintenance.optBoolean("enabled", false), maintenanceMessage = maintenance.optString("message", ""), latestVersionCode = update.optInt("latestVersionCode", 0), latestVersionName = update.optString("latestVersionName", "Belum dicek"), updateAvailable = update.optBoolean("updateAvailable", false), patchName = patch?.optString("name", "") ?: "", patchUrl = patch?.optString("url", "") ?: "", notices = notices, unreadNotifications = json.optInt("unreadNotifications", 0), loaded = true) }
private fun createSupportTicket(session: AuthSession, message: String): String = try { val marker = guidedReadMarkerSmart(); val json = httpPost("/rest/v1/rpc/dlavie_v2_create_ticket", session.accessToken, JSONObject().put("p_title", "DLavie Support").put("p_category", "general").put("p_message", message).put("p_app_version", "0.19.0-login-foundation").put("p_local_version", LOCAL_VERSION_NAME).put("p_latest_version", "").put("p_data_marker", marker).put("p_shizuku_status", guidedShizukuState())); "OK: ticket dibuat #${json.optString("public_code", json.optString("id", ""))}" } catch (e: Exception) { "Error: ${e.message ?: "ticket gagal"}" }
private fun httpPost(path: String, token: String?, body: JSONObject): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Accept", "application/json"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); conn.outputStream.use { it.write(body.toString().toByteArray()) }; val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else JSONObject(text) }
private fun httpPostWithPrefer(path: String, token: String?, body: JSONObject, prefer: String): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Accept", "application/json"); conn.setRequestProperty("Prefer", prefer); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); conn.outputStream.use { it.write(body.toString().toByteArray()) }; val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject() } }
private fun httpGet(path: String, token: String? = null): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "GET"; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Accept", "application/json"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject() } }
private fun httpGetArray(path: String, token: String? = null): JSONArray { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "GET"; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Accept", "application/json"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONArray() else JSONArray(text) }
private fun httpPatch(path: String, token: String?, body: JSONObject): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "PATCH"; conn.doOutput = true; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Accept", "application/json"); conn.setRequestProperty("Prefer", "return=minimal"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); conn.outputStream.use { it.write(body.toString().toByteArray()) }; val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject() } }
/**
 * Parse error response dari Supabase / HTTP ke pesan yang professional dan
 * human-friendly dalam Bahasa Indonesia.
 *
 * Handles common Supabase auth error codes:
 * - weak_password → "Password terlalu lemah. Gunakan minimal 8 karakter..."
 * - user_already_exists → "Email sudah terdaftar..."
 * - invalid_credentials → "Email atau password salah..."
 * - email_not_confirmed → "Email belum diverifikasi..."
 * - rate_limit_exceeded → "Terlalu banyak percobaan..."
 * - signup_disabled → "Pendaftaran sedang dinonaktifkan..."
 * - etc.
 *
 * Falls back to generic "Terjadi kesalahan" + brief technical hint untuk
 * error yang tidak dikenali.
 */
private fun parseError(text: String): String {
    if (text.isBlank()) return "Permintaan gagal. Coba lagi."

    return runCatching {
        val obj = JSONObject(text)
        // Supabase auth error format: {"code": "422", "error_code": "weak_password", "msg": "..."}
        val errorCode = obj.optString("error_code", "").lowercase()
        val msg = obj.optString("msg", obj.optString("message", ""))
        val code = obj.optString("code", "")

        when (errorCode) {
            "weak_password" -> {
                "Password needs to be stronger. Try adding numbers or symbols."
            }
            "user_already_exists" -> {
                "This email is already registered. Try signing in instead."
            }
            "invalid_credentials" -> {
                "Incorrect email or password. Please try again."
            }
            "email_not_confirmed" -> {
                "Please verify your email. Check your inbox for a confirmation link."
            }
            "email_exists" -> {
                "This email is already registered. Try signing in instead."
            }
            "phone_exists" -> {
                "This phone number is already registered."
            }
            "rate_limit_exceeded" -> {
                "Too many attempts. Please wait a few minutes and try again."
            }
            "signup_disabled" -> {
                "Registration is temporarily disabled. Please try again later."
            }
            "session_expired" -> {
                "Your session has expired. Please sign in again."
            }
            "session_not_found" -> {
                "Session not found. Please sign in again."
            }
            "user_not_found" -> {
                "Account not found. Check your email or create a new account."
            }
            "no_authorization" -> {
                "Authorization required. Please sign in again."
            }
            "reauthentication_needed" -> {
                "Please verify your identity and try again."
            }
            "same_password" -> {
                "New password must be different from your current password."
            }
            "password_too_short" -> {
                "Password is too short. Use at least 6 characters."
            }
            "password_too_long" -> {
                "Password is too long. Maximum 72 characters."
            }
            "validation_failed" -> {
                "Invalid input. Please check your details and try again."
            }
            "captcha_failed" -> {
                "Verification failed. Please try again."
            }
            "provider_email_needs_verification" -> {
                "Email verification required. Please verify your email."
            }
            "user_banned" -> {
                "Your account has been suspended. Contact support for help."
            }
            "email_address_not_authorized" -> {
                "This email is not authorized. Contact support."
            }
            else -> {
                // Modern generic error — no technical jargon
                when {
                    msg.contains("password", ignoreCase = true) && msg.contains("weak") -> "Password needs to be stronger. Try adding numbers or symbols."
                    msg.contains("already", ignoreCase = true) -> "This email is already registered. Try signing in instead."
                    msg.contains("invalid", ignoreCase = true) -> "Invalid credentials. Please check and try again."
                    msg.contains("rate", ignoreCase = true) -> "Too many attempts. Please wait and try again."
                    msg.contains("network", ignoreCase = true) || msg.contains("connect", ignoreCase = true) -> "Network error. Check your connection and try again."
                    else -> "Something went wrong. Please try again."
                }
            }
        }
    }.getOrElse {
        // JSON parse failed — clean modern message
        "Something went wrong. Please try again."
    }
}

/**
 * Fetch maintenance config from app_config (key = 'maintenance').
 * Returns MaintenanceState(loaded = true) on success.
 * On any failure, returns a non-maintenance state so users can still log in.
 *
 * Expected shape:
 *   { "enabled": true, "scope": "all", "title": "...", "message": "...", "allow_offline_play": true }
 */
private fun fetchMaintenanceConfig(): MaintenanceState {
    return try {
        val arr = httpGetArray("/rest/v1/app_config?key=eq.maintenance&select=key,value")
        if (arr.length() == 0) return MaintenanceState(loaded = true)
        val row = arr.getJSONObject(0)
        val value = row.opt("value")
        val cfg = when (value) {
            is JSONObject -> value
            is org.json.JSONArray -> if (value.length() > 0) value.optJSONObject(0) else JSONObject()
            else -> JSONObject()
        }
        MaintenanceState(
            enabled      = cfg.optBoolean("enabled", false),
            title        = cfg.optString("title", ""),
            message      = cfg.optString("message", ""),
            allowOffline = cfg.optBoolean("allow_offline_play", cfg.optBoolean("allowOffline", false)),
            loaded       = true
        )
    } catch (_: Exception) {
        // Network/parse failure — don't block login.
        MaintenanceState(loaded = true)
    }
}
private fun jsonArrayObjectsToTitles(arr: JSONArray?): List<String> { if (arr == null) return emptyList(); val out = mutableListOf<String>(); for (i in 0 until arr.length()) { val o = arr.optJSONObject(i); if (o != null) out += (o.optString("title", "Notice") + ": " + o.optString("body", "")) }; return out }
private fun guidedShizukuState(): String = try { when { !Shizuku.pingBinder() -> "Start"; Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> "Ready"; else -> "Permission" } } catch (_: Exception) { "Start" }
private fun guidedRequestShizuku() { runCatching { if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(SHIZUKU_REQUEST) } }
private fun guidedIsGameInstalled(context: Context): Boolean = try { context.packageManager.getPackageInfo(GAME_PACKAGE, 0); true } catch (_: Exception) { false }
private fun guidedLaunchGame(context: Context) { context.packageManager.getLaunchIntentForPackage(GAME_PACKAGE)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
private fun guidedOpenClassicInstaller(context: Context) { runCatching { context.startActivity(Intent(context, ModernLauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
private fun guidedReadMarkerSmart(): String = runCatching { File(MARKER_PATH).readText().trim() }.getOrElse { "No marker" }
private fun guidedShortMarker(marker: String): String = if (marker.length > 12) marker.take(12) else marker
private suspend fun guidedDownloadPatch(context: Context, onProgress: (GuidedUpdateState) -> Unit): GuidedUpdateState { val s = GuidedUpdateState(message = "Download patch akan masuk setelah login foundation hijau."); onProgress(s); return s }
private suspend fun guidedInstallPatch(context: Context): GuidedUpdateState = GuidedUpdateState(message = "Apply patch akan masuk setelah login foundation hijau.")

// ═══════════════════════════════════════════════════════════════════════════════
// v7.9.57 TOTAL REMAKE — Interactive Halftone Background + Animated DL Logo
// Match DLavie website style: Canvas-based halftone with touch drift,
// custom drawn animated DL logo, loading screen
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Interactive Halftone Background — Canvas-based, dots drift on touch.
 * Match web .halftone-canvas: auto-drift + touch response.
 * Dots arranged in grid, opacity varies by distance from center (vignette).
 */
@Composable
private fun InteractiveHalftoneBackground(
    modifier: Modifier = Modifier,
    dotSpacing: Float = 28f,
    dotSize: Float = 2.2f
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { config.screenHeightDp.dp.toPx() }

    // Touch position for parallax drift
    var touchX by remember { mutableStateOf(screenWidth / 2f) }
    var touchY by remember { mutableStateOf(screenHeight / 2f) }
    // Target touch (smooth lerp)
    var targetX by remember { mutableStateOf(screenWidth / 2f) }
    var targetY by remember { mutableStateOf(screenHeight / 2f) }

    // Auto-drift animation
    val autoDrift = rememberInfiniteTransition(label = "halftoneDrift")
    val driftX by autoDrift.animateFloat(
        initialValue = 0f, targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = GuideEase),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftX"
    )
    val driftY by autoDrift.animateFloat(
        initialValue = 0f, targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = GuideEase),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    )

    // Smooth lerp touch position toward target
    LaunchedEffect(targetX, targetY) {
        while (true) {
            touchX += (targetX - touchX) * 0.08f
            touchY += (targetY - touchY) * 0.08f
            kotlinx.coroutines.delay(16) // ~60fps
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val cols = (size.width / dotSpacing).toInt() + 2
        val rows = (size.height / dotSpacing).toInt() + 2

        // Center of screen for vignette
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxDist = kotlin.math.sqrt(cx * cx + cy * cy)

        // Parallax offset from touch (lerped)
        val parallaxX = (touchX - cx) * 0.02f
        val parallaxY = (touchY - cy) * 0.02f

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val baseX = col * dotSpacing
                val baseY = row * dotSpacing

                // Distance from touch → parallax depth
                val dx = baseX - touchX
                val dy = baseY - touchY
                val distFromTouch = kotlin.math.sqrt(dx * dx + dy * dy)
                val depthFactor = (1f - (distFromTouch / 400f).coerceIn(0f, 1f)) * 8f

                // Parallax: dots closer to touch move more
                val px = baseX + parallaxX * (1f + depthFactor) + driftX
                val py = baseY + parallaxY * (1f + depthFactor) + driftY

                // Vignette: dots further from center are dimmer
                val distFromCenter = kotlin.math.sqrt((baseX - cx) * (baseX - cx) + (baseY - cy) * (baseY - cy))
                val vignette = 1f - (distFromCenter / maxDist).coerceIn(0f, 1f) * 0.7f

                // Dot size varies slightly
                val sz = dotSize * (0.6f + vignette * 0.6f)
                val alpha = 0.04f + vignette * 0.08f

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = sz,
                    center = androidx.compose.ui.geometry.Offset(px, py)
                )
            }
        }
    }
}

/**
 * Animated DL Logo — custom drawn dengan Canvas.
 * "DL" letters dengan breathing + sweep animation.
 * Unique modern look, bukan text biasa.
 */
@Composable
private fun AnimatedDLLogo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 80.dp
) {
    val breathAnim = rememberInfiniteTransition(label = "dlBreath")
    val breathScale by breathAnim.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = GuideEase),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val sweepAnim = rememberInfiniteTransition(label = "dlSweep")
    val sweepAngle by sweepAnim.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val glowAnim = rememberInfiniteTransition(label = "dlGlow")
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = GuideEase),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring (sweep)
        Canvas(
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = breathScale; scaleY = breathScale }
        ) {
            val w = this.size.width
            val h = this.size.height
            val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)
            val radius = (minOf(w, h) / 2f) * 0.9f

            // Sweep arc (rotating gradient ring)
            drawArc(
                color = Color.White.copy(alpha = glowAlpha * 0.3f),
                startAngle = sweepAngle,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            drawArc(
                color = Color.White.copy(alpha = glowAlpha * 0.15f),
                startAngle = sweepAngle + 180f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
        }

        // White square card (match web .portal-logo)
        Surface(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .graphicsLayer { scaleX = breathScale; scaleY = breathScale },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Custom drawn "DL" with Canvas
                Canvas(Modifier.fillMaxSize(0.6f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = minOf(w, h) * 0.12f

                    // Draw "D"
                    val dPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.05f, h * 0.1f)
                        lineTo(w * 0.05f, h * 0.9f)
                        // D curve
                        cubicTo(
                            w * 0.6f, h * 0.9f,
                            w * 0.55f, h * 0.1f,
                            w * 0.05f, h * 0.1f
                        )
                    }
                    drawPath(
                        dPath,
                        color = Color.Black,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )

                    // Draw "L"
                    val lPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.55f, h * 0.1f)
                        lineTo(w * 0.55f, h * 0.9f)
                        lineTo(w * 0.95f, h * 0.9f)
                    }
                    drawPath(
                        lPath,
                        color = Color.Black,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

/**
 * Loading Screen dengan animated DL logo.
 * Tampil saat app pertama kali buka / processing auth.
 */
@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(GuideDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedDLLogo(size = 96.dp)
            Spacer(Modifier.height(24.dp))
            // Loading dots animation
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    val dotAnim = rememberInfiniteTransition(label = "dot$index")
                    val dotAlpha by dotAnim.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200, easing = GuideEase),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha$index"
                    )
                    Box(
                        Modifier.size(6.dp).background(
                            Color.White.copy(alpha = dotAlpha),
                            CircleShape
                        )
                    )
                }
            }
        }
    }
}
