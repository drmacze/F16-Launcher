package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ═══════════════════════════════════════════════════════════════════════════
// CINEMATIC PORTAL SCREEN — Modern redesign with halftone + GSAP-style animations
// ═══════════════════════════════════════════════════════════════════════════

// Design tokens
private val CinematicBlack = Color(0xFF000000)
private val CinematicWhite = Color(0xFFFFFFFF)
private val CinematicGray = Color(0xFF888888)
private val CinematicDim = Color(0xFF333333)
private val AccentAmber = Color(0xFFFFAA00)
private val AccentGreen = Color(0xFF00D26A)
private val AccentRed = Color(0xFFFF5252)

@Composable
fun CinematicPortalScreen(
    onConnectPortal: () -> Unit,
    onManualConnect: (String) -> Boolean,
    working: Boolean = false
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ── GSAP-style staggered entrance animation ──
    // Each element fades in + slides up with a stagger delay
    val animations = remember { mutableStateListOf(false, false, false, false, false, false) }
    LaunchedEffect(Unit) {
        animations.indices.forEach { i ->
            delay(80L * i)  // 80ms stagger
            animations[i] = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(CinematicBlack)
    ) {
        // ── Layer 1: Halftone dot pattern background ──
        HalftoneBackground()

        // ── Layer 2: Cinematic glow orbs (animated) ──
        GlowOrbTopRight()
        GlowOrbBottomLeft()

        // ── Layer 3: Vignette ──
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    0f to Color.Transparent,
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.7f)
                )
            )
        )

        // ── Layer 4: Main content (smooth scroll) ──
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp)
                .padding(top = 120.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Logo: Minimal "DL" mark ──
            CinematicAnimatedElement(visible = animations[0]) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CinematicWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "DL",
                        color = CinematicBlack,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Title: "DLavie Portal" ──
            CinematicAnimatedElement(visible = animations[1]) {
                Text(
                    "DLavie Portal",
                    color = CinematicWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Subtitle ──
            CinematicAnimatedElement(visible = animations[2]) {
                Text(
                    "Login or connect your DLavie Launcher account\nto access all web features.",
                    color = CinematicWhite.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── Version info card (minimal, solid) ──
            CinematicAnimatedElement(visible = animations[3]) {
                VersionInfoCard()
            }

            Spacer(Modifier.height(20.dp))

            // ── Primary: Connect via DLavie Portal ──
            CinematicAnimatedElement(visible = animations[4]) {
                Button(
                    onClick = onConnectPortal,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CinematicWhite,
                        contentColor = CinematicBlack,
                        disabledContainerColor = CinematicWhite.copy(alpha = 0.5f),
                        disabledContentColor = CinematicBlack.copy(alpha = 0.5f)
                    ),
                    enabled = !working
                ) {
                    Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Connect via DLavie Portal", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Auto-login hint ──
            CinematicAnimatedElement(visible = animations[5]) {
                Text(
                    "Already connected? Launcher will auto-login if token is valid.",
                    color = CinematicWhite.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Divider ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(CinematicWhite.copy(0.08f)))
                Text(
                    "OR CONNECT MANUALLY",
                    color = CinematicWhite.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Box(Modifier.weight(1f).height(1.dp).background(CinematicWhite.copy(0.08f)))
            }

            Spacer(Modifier.height(20.dp))

            // ── Manual connect card ──
            ManualConnectCard(onConnect = onManualConnect)
        }
    }
}

// ─── GSAP-style animated element (fade + slide up) ───────────────────────────
@Composable
private fun CinematicAnimatedElement(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "offsetY"
    )
    Box(
        Modifier.offset(y = offsetY.dp).graphicsLayer(alpha = alpha)
    ) {
        content()
    }
}

// ─── Halftone dot pattern background ─────────────────────────────────────────
@Composable
private fun HalftoneBackground() {
    Canvas(Modifier.fillMaxSize()) {
        val dotSpacing = 24f
        val dotRadius = 1.5f
        val rows = (size.height / dotSpacing).toInt() + 1
        val cols = (size.width / dotSpacing).toInt() + 1

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = col * dotSpacing
                val y = row * dotSpacing
                // Offset alternate rows for hexagonal pattern
                val xOffset = if (row % 2 == 0) 0f else dotSpacing / 2
                // Distance from center for vignette effect on dots
                val centerX = size.width / 2
                val centerY = size.height / 2
                val dist = kotlin.math.sqrt(
                    (x + xOffset - centerX).pow(2) + (y - centerY).pow(2)
                )
                val maxDist = kotlin.math.sqrt(centerX.pow(2) + centerY.pow(2))
                val alpha = (1f - (dist / maxDist) * 0.8f).coerceIn(0.05f, 0.25f)

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = dotRadius,
                    center = Offset(x + xOffset, y)
                )
            }
        }
    }
}

// ─── Cinematic glow orbs (animated, like website) ────────────────────────────
@Composable
private fun GlowOrbTopRight() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb1")
    val x by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1_x"
    )
    val y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1_y"
    )
    Canvas(Modifier.fillMaxSize()) {
        val orbSize = 800f
        val px = size.width * (0.7f + x * 0.2f) - orbSize / 2
        val py = size.height * (-0.1f + y * 0.2f) - orbSize / 2
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = Offset(px + orbSize / 2, py + orbSize / 2),
                radius = orbSize / 2
            ),
            radius = orbSize / 2,
            center = Offset(px + orbSize / 2, py + orbSize / 2)
        )
    }
}

@Composable
private fun GlowOrbBottomLeft() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb2")
    val x by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2_x"
    )
    val y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2_y"
    )
    Canvas(Modifier.fillMaxSize()) {
        val orbSize = 700f
        val px = size.width * (-0.1f + x * 0.2f) - orbSize / 2
        val py = size.height * (0.8f + y * 0.15f) - orbSize / 2
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.04f),
                    Color.White.copy(alpha = 0.01f),
                    Color.Transparent
                ),
                center = Offset(px + orbSize / 2, py + orbSize / 2),
                radius = orbSize / 2
            ),
            radius = orbSize / 2,
            center = Offset(px + orbSize / 2, py + orbSize / 2)
        )
    }
}

// ─── Version Info Card (minimal, solid) ──────────────────────────────────────
@Composable
private fun VersionInfoCard() {
    val context = LocalContext.current
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

    // Check latest version
    var latestVersionCode by remember { mutableStateOf<Int?>(null) }
    var checkingUpdate by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/drmacze/F16-Launcher/releases/latest")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "DLavie-Launcher")
                    connect()
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "")
                    latestVersionCode = tagName.removePrefix("v").toIntOrNull() ?: 0
                }
            } catch (_: Exception) { }
            finally { checkingUpdate = false }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x0AFFFFFF),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.06f))
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Version text
            Column(Modifier.weight(1f)) {
                Text(
                    "DLavie Launcher v$currentVersionName",
                    color = Color.White.copy(0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "build $currentVersionCode",
                    color = Color.White.copy(0.4f),
                    fontSize = 11.sp
                )
            }
            // Status
            when {
                checkingUpdate -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White.copy(0.4f),
                        strokeWidth = 2.dp
                    )
                }
                latestVersionCode != null && latestVersionCode!! > currentVersionCode -> {
                    Text(
                        "Update available",
                        color = AccentAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                latestVersionCode != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Latest",
                            color = AccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    Text(
                        "Offline",
                        color = Color.White.copy(0.3f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ─── Manual Connect Card ─────────────────────────────────────────────────────
@Composable
private fun ManualConnectCard(onConnect: (String) -> Boolean) {
    var pasteUrl by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xF00E0E0E),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.06f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.ContentPaste,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Connect Manual",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "For older launcher versions without deep link handler.\n\n" +
                "1. Login at DLavie web\n" +
                "2. Click \"Connect to DLavie\"\n" +
                "3. Copy the connect URL\n" +
                "4. Paste below and tap Connect",
                color = Color.White.copy(0.4f),
                fontSize = 11.sp,
                lineHeight = 16.sp
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
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White.copy(0.5f),
                    unfocusedBorderColor = Color.White.copy(0.15f),
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A)
                )
            )
            if (pasteError.isNotEmpty()) {
                Text(pasteError, color = AccentRed, fontSize = 11.sp)
            }
            Button(
                onClick = {
                    val success = onConnect(pasteUrl.trim())
                    if (!success) {
                        pasteError = "Invalid URL. Make sure it contains token and uid."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(0.1f),
                    contentColor = Color.White
                )
            ) {
                Text("Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// Helper: Float.pow(2) extension
private fun Float.pow(n: Int): Float = Math.pow(this.toDouble(), n.toDouble()).toFloat()
