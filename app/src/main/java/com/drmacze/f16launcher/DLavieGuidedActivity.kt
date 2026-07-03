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
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val existing = loadSession(this)
        if (existing != null) {
            syncToCommunityPrefs(this, existing)
            startActivity(Intent(this, ModernLauncherActivity::class.java))
            finish()
            return
        }
        setContent { DLavieGuidedApp() }
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
private val COUNTRY_LIST: List<String> = listOf(
    "Indonesia", "Malaysia", "Singapore", "Philippines", "Thailand",
    "Vietnam", "India", "USA", "UK", "Japan",
    "South Korea", "Brazil", "Germany", "France", "Canada",
    "Australia", "Saudi Arabia", "UAE", "Netherlands", "Spain",
    "Other"
)
private const val DEFAULT_COUNTRY = "Indonesia"

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

// v3.0 monochrome palette — match DLavie logo (white-on-black, halftone)
// GuideGreen tetap vibrant (functional status: success/download).
// GuideCyan/GuideRed/GuideAmber tetap vibrant untuk status indicators minimal.
private val GuideGreen = Color(0xFF22E678)
private val GuideCyan = Color(0xFF2ED3F6)
private val GuideRed = Color(0xFFFF5B64)
private val GuideAmber = Color(0xFFFFB84E)
private val GuideWhite = Color(0xFFF4F7F5)
private val GuideMuted = Color(0xFF8E9491)
private val GuideDark = Color(0xFF0A0A0A)        // v3.0 near pure black (match logo)
private val GuideCard = Color(0xDD111111)        // v3.0 monochrome card
private val GuideBorder = Color(0x30FFFFFF)      // v3.0 subtle white border (halftone-like)
private val GuideFont = FontFamily.SansSerif

@Composable
private fun DLavieGuidedApp() {
    val context = LocalContext.current
    // Maintenance state fetched at app startup BEFORE the login screen is shown.
    var maintenance by remember { mutableStateOf(MaintenanceState()) }
    var showLogin  by remember { mutableStateOf(false) }

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
                    GuidedLoginScreen(onLoggedIn = { session ->
                        saveSession(context, session)
                        syncToCommunityPrefs(context, session)
                        context.startActivity(
                            Intent(context, ModernLauncherActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    })
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
private fun GuidedLoginScreen(onLoggedIn: (AuthSession) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Mode: "login" | "register" | "forgot"
    var mode by remember { mutableStateOf("login") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(DEFAULT_COUNTRY) }
    var showPass by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    // v4.0 monochrome login remake — halftone bg + DLavie text logo + star,
    // glass card, white inverted CTA button, inline error/success message.
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))   // v4.0 pure-black base
    ) {
        // Halftone particle background (subtle, monochrome)
        HalftoneBackground(
            modifier = Modifier.fillMaxSize(),
            dotSize = 2.5f,
            spacing = 24f,
            baseColor = HalftoneBright,
            alpha = 0.55f
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(60.dp))

            // ── Logo: "DLavie" text + star icon (sama seperti splash screen) ──
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DLavie",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = GuideFont,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Rounded.Star,
                    null,
                    tint = Color.White.copy(0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when (mode) {
                    "login"    -> "Masuk untuk lanjut"
                    "register" -> "Buat akun baru"
                    "forgot"   -> "Reset password via email"
                    else        -> "FIFA 16 Mobile 2026 Mod Hub"
                },
                color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont
            )
            Spacer(Modifier.height(32.dp))

            // ── Glass Card (subtle white border) ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xF0111111),                  // v4.0 near-opaque dark glass
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.08f))
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // ── Tab switcher (Masuk | Daftar) — hidden in forgot mode ──
                    if (mode != "forgot") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0A0A), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Login tab
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (mode == "login") Color.White else Color.Transparent)
                                    .clickable { mode = "login"; message = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Masuk",
                                    color = if (mode == "login") Color.Black else GuideMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GuideFont
                                )
                            }
                            // Register tab
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (mode == "register") Color.White else Color.Transparent)
                                    .clickable { mode = "register"; message = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Daftar",
                                    color = if (mode == "register") Color.Black else GuideMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GuideFont
                                )
                            }
                        }
                    }

                    // ── Forgot password mode ── hint text
                    if (mode == "forgot") {
                        Text(
                            "Masukkan email akunmu. Kami akan kirim link reset password ke email tersebut.",
                            color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont, lineHeight = 17.sp
                        )
                    }

                    // ── Email field (always shown) ──
                    AuthField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = "Email",
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Email,
                                contentDescription = null,
                                tint = GuideMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    // ── Password field (login + register only) ──
                    if (mode != "forgot") {
                        AuthField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = GuideMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            isPassword = !showPass,
                            trailingLabel = if (showPass) "Sembunyikan" else "Tampilkan",
                            onTrailing = { showPass = !showPass }
                        )
                    }

                    // ── Register-only fields ──
                    if (mode == "register") {
                        AuthField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Konfirmasi Password",
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = GuideMuted,
                                    modifier = Modifier.size(18.dp)
                                )
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
                                Text(
                                    "@",
                                    color = GuideMuted,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GuideFont
                                )
                            }
                        )
                        AuthField(
                            value = displayName,
                            onValueChange = { displayName = it.take(40) },
                            label = "Display Name (2-40)",
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = GuideMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        CountryPickerDropdown(
                            selected = country,
                            onSelect = { country = it }
                        )
                    }

                    // ── Forgot password link (login mode only) ──
                    if (mode == "login") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "Lupa password?",
                                color = GuideMuted,
                                fontSize = 12.sp,
                                fontFamily = GuideFont,
                                modifier = Modifier.clickable {
                                    mode = "forgot"; message = ""; email = ""
                                }
                            )
                        }
                    }

                    // ── Back to login link (forgot mode only) ──
                    if (mode == "forgot") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                "← Kembali ke Login",
                                color = GuideMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = GuideFont,
                                modifier = Modifier.clickable {
                                    mode = "login"; message = ""; email = ""
                                }
                            )
                        }
                    }

                    // ── Error / success message (inline, subtle background) ──
                    AnimatedVisibility(visible = message.isNotBlank()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    (if (isSuccess) GuideGreen else GuideRed).copy(alpha = 0.10f),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    (if (isSuccess) GuideGreen else GuideRed).copy(alpha = 0.28f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                message,
                                color = if (isSuccess) GuideGreen else GuideRed,
                                fontSize = 12.sp,
                                fontFamily = GuideFont,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // ── CTA button (full width, white inverted) ──
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
                                        "register" -> registerWithUsernamePassword(context, email, password, username.trim(), displayName.trim(), country)
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
                                // Fire telemetry for login / register events
                                if (result.session != null) {
                                    when (mode) {
                                        "login"    -> Telemetry.track(context, Telemetry.EVT_LOGIN,    mapOf("email" to email.trim()))
                                        "register" -> Telemetry.track(context, Telemetry.EVT_REGISTER, mapOf("email" to email.trim(), "username" to username.trim(), "country" to country))
                                    }
                                }
                                result.session?.let(onLoggedIn)

                                // If forgot password success, switch back to login
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                working -> "Memproses..."
                                mode == "login"    -> "Masuk"
                                mode == "register" -> "Daftar"
                                mode == "forgot"   -> "Kirim Link Reset"
                                else -> "→"
                            },
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                when (mode) {
                    "login"    -> "Belum punya akun? Tap Daftar."
                    "register" -> "Sudah punya akun? Tap Masuk."
                    "forgot"   -> "Link reset hanya berlaku 1 jam."
                    else -> ""
                },
                color = GuideMuted, fontSize = 12.sp, textAlign = TextAlign.Center, fontFamily = GuideFont
            )
            Spacer(Modifier.height(40.dp))
        }
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
        GuidedPrimaryCta(if (marker.startsWith("v26")) "Mainkan Game" else "Install Full Data", if (marker.startsWith("v26")) "Data siap. Buka FIFA 16." else "Base data belum lengkap.", if (marker.startsWith("v26")) "▶" else "⬇", if (marker.startsWith("v26")) { guidedLaunchGame(context) } else openData)
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
            GuidedActionButton(if (marker.startsWith("v26")) "Ke Update" else "Buka Installer Data", if (marker.startsWith("v26")) GuideCyan else GuideGreen, if (marker.startsWith("v26")) openUpdate else { guidedOpenClassicInstaller(context) }, true)
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
private fun registerWithUsernamePassword(context: Context, email: String, password: String, username: String, displayName: String, country: String = DEFAULT_COUNTRY): AuthResult = try {
    val meta = JSONObject().put("username", username).put("display_name", displayName).put("country", country)
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
            // Best-effort: PATCH country (fire-and-forget).
            runCatching {
                if (api.loggedIn()) api.updateCountry(country)
                else httpPatch("/rest/v1/profiles?id=eq." + userId, token, JSONObject().put("country", country))
            }
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
} catch (e: Exception) { AuthResult(null, "Error: ${e.message ?: "register gagal"}") }
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
} catch (e: Exception) { AuthResult(null, "Error: ${e.message ?: "auth gagal"}") }
private fun loadBootstrap(session: AuthSession): BootstrapState = try { val json = httpPost("/rest/v1/rpc/dlavie_v2_get_launcher_bootstrap", session.accessToken, JSONObject().put("p_local_version_code", LOCAL_VERSION_CODE)); parseBootstrap(json) } catch (e: Exception) { BootstrapState(loaded = false, error = e.message ?: "backend gagal") }
private fun parseBootstrap(json: JSONObject): BootstrapState { val profile = json.optJSONObject("profile") ?: JSONObject(); val update = json.optJSONObject("update") ?: JSONObject(); val patch = update.optJSONObject("patch"); val maintenance = json.optJSONObject("maintenance") ?: JSONObject(); val notices = jsonArrayObjectsToTitles(json.optJSONArray("notices")); return BootstrapState(displayName = profile.optString("display_name", "DLavie Player"), role = profile.optString("role", "user"), maintenance = maintenance.optBoolean("enabled", false), maintenanceMessage = maintenance.optString("message", ""), latestVersionCode = update.optInt("latestVersionCode", 0), latestVersionName = update.optString("latestVersionName", "Belum dicek"), updateAvailable = update.optBoolean("updateAvailable", false), patchName = patch?.optString("name", "") ?: "", patchUrl = patch?.optString("url", "") ?: "", notices = notices, unreadNotifications = json.optInt("unreadNotifications", 0), loaded = true) }
private fun createSupportTicket(session: AuthSession, message: String): String = try { val marker = guidedReadMarkerSmart(); val json = httpPost("/rest/v1/rpc/dlavie_v2_create_ticket", session.accessToken, JSONObject().put("p_title", "DLavie Support").put("p_category", "general").put("p_message", message).put("p_app_version", "0.19.0-login-foundation").put("p_local_version", LOCAL_VERSION_NAME).put("p_latest_version", "").put("p_data_marker", marker).put("p_shizuku_status", guidedShizukuState())); "OK: ticket dibuat #${json.optString("public_code", json.optString("id", ""))}" } catch (e: Exception) { "Error: ${e.message ?: "ticket gagal"}" }
private fun httpPost(path: String, token: String?, body: JSONObject): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Accept", "application/json"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); conn.outputStream.use { it.write(body.toString().toByteArray()) }; val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else JSONObject(text) }
private fun httpPostWithPrefer(path: String, token: String?, body: JSONObject, prefer: String): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Accept", "application/json"); conn.setRequestProperty("Prefer", prefer); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); conn.outputStream.use { it.write(body.toString().toByteArray()) }; val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject() } }
private fun httpGet(path: String, token: String? = null): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "GET"; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Accept", "application/json"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject() } }
private fun httpGetArray(path: String, token: String? = null): JSONArray { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "GET"; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Accept", "application/json"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONArray() else JSONArray(text) }
private fun httpPatch(path: String, token: String?, body: JSONObject): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "PATCH"; conn.doOutput = true; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Accept", "application/json"); conn.setRequestProperty("Prefer", "return=minimal"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); conn.outputStream.use { it.write(body.toString().toByteArray()) }; val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject() } }
private fun parseError(text: String): String = runCatching { JSONObject(text).optString("message", text.take(160)) }.getOrElse { text.take(160).ifBlank { "request gagal" } }

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
