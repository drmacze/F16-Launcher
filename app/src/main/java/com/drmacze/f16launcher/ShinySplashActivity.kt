package com.drmacze.f16launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * DLavie v3.0 Splash — Monochrome White-on-Black + Halftone Particles.
 *
 * Design philosophy (match new DLavie logo):
 *  - Background: HalftoneBackground (near-black + dot grid + corner glows)
 *  - "DLavie" text: pure white, bold, modern sans-serif
 *  - Star icon: small white star di kanan text, dengan pulse glow animation
 *  - Loading dots: pure white (bukan silver)
 *  - Tagline: subtle, near-invisible (1 line, very small)
 *
 * Flow (~4s total):
 *  1. Fade in "DLavie" text + scale-up (600-800ms, overlapping)
 *  2. Star glow pulse start (continuous) + play cinematic sound
 *  3. Fade in tagline (400ms) — very subtle
 *  4. Fade in dots loader (300ms) — white
 *  5. Hold (500ms)
 *  6. Fade out everything (400-500ms)
 *  7. Navigate to next screen
 */
class ShinySplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── DLavie Portal Connect: check for deep link ──
        // When user clicks "Connect to DLavie" on the web FAQ page,
        // Android opens this activity via dlavie://connect?callback=URL
        // We handle it IMMEDIATELY — before splash animation.
        // ── DLavie Portal Connect: check for deep link ──
        // Inline ALL logic here — no separate function (avoids scope issues)
        val portalData = intent?.data
        if (portalData != null && portalData.scheme == "dlavie" && portalData.host == "connect") {
            val callback = portalData.getQueryParameter("callback")
            val api = CommunityApi(this)

            if (callback == null || !api.loggedIn()) {
                android.widget.Toast.makeText(this,
                    "Silakan login dulu di launcher, lalu coba Connect lagi dari web.",
                    android.widget.Toast.LENGTH_LONG).show()
                val loginIntent = android.content.Intent(this, DLavieGuidedActivity::class.java)
                loginIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(loginIntent)
                finish()
                return
            }

            val portalToken = api.token()
            val portalUid = api.userId()

            // Mark portal as connected
            getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("portal_connected", true)
                .putString("portal_connected_at", System.currentTimeMillis().toString())
                .apply()

            // Show status
            android.widget.Toast.makeText(this,
                "✓ DLavie Portal Connected! Mengalihkan kembali ke web…",
                android.widget.Toast.LENGTH_LONG).show()

            // Redirect back to web after 2s
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val redirectUrl = "$callback?token=$portalToken&uid=$portalUid"
                    val browserIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(redirectUrl)
                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "Gagal redirect: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
                finish()
            }, 2000)
            return  // Don't start splash
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color.Black, surface = Color.Black,
                onBackground = Color.White, onSurface = Color.White
            )) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    DLavieHalftoneSplash(
                        onFinished = {
                            // Decide next destination based on auth state
                            val prefs = getSharedPreferences("dlavie_auth_session", android.content.Context.MODE_PRIVATE)
                            val token = prefs.getString("access_token", null)
                            // v6.8.3: Guest mode check — if is_guest=true in dlavie_community prefs, skip login
                            val communityPrefs = getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE)
                            val isGuest = communityPrefs.getBoolean("is_guest", false)
                            val target = if (!token.isNullOrBlank()) {
                                // Sync to community prefs
                                val refresh = prefs.getString("refresh_token", "") ?: ""
                                val userId = try {
                                    val payload = token.split(".").getOrNull(1) ?: ""
                                    val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
                                    val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
                                    org.json.JSONObject(String(decoded)).optString("sub", "")
                                } catch (_: Exception) { "" }
                                communityPrefs.edit()
                                    .putString("access_token", token)
                                    .putString("refresh_token", refresh)
                                    .putString("user_id", userId)
                                    .apply()
                                Intent(this, ModernLauncherActivity::class.java)
                            } else if (isGuest) {
                                // v6.8.3: Guest mode — bypass login, go straight to launcher
                                Intent(this, ModernLauncherActivity::class.java)
                            } else {
                                Intent(this, DLavieGuidedActivity::class.java)
                            }
                            target.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                            startActivity(target)
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DLavieHalftoneSplash(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Animation states
    val textAlpha = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.92f) }
    val starAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val dotsAlpha = remember { Animatable(0f) }

    // Continuous star glow pulse (independent dari phase machine)
    val infiniteTransition = rememberInfiniteTransition(label = "star_glow")
    val starGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "star_glow_val"
    )
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "star_scale"
    )

    // Phase machine
    LaunchedEffect(Unit) {
        // Phase 1: Fade in "DLavie" text + scale-up (600-800ms, overlapping)
        kotlinx.coroutines.coroutineScope {
            launch { textAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            launch { textScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        }

        // Phase 2: Fade in star icon (300ms) + play cinematic sound at start.
        scope.launch { SoundEffectHelper.playShinyChime(context) }
        kotlinx.coroutines.coroutineScope {
            launch { starAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        }

        // Phase 3: Fade in tagline (400ms) — very subtle
        taglineAlpha.animateTo(0.5f, tween(400, easing = FastOutSlowInEasing))

        // Phase 4: Fade in dots loader (300ms)
        dotsAlpha.animateTo(1f, tween(300))

        // Phase 5: Hold (500ms) — biarkan user menikmati star glow + sound
        delay(500)

        // Phase 6: Fade out everything (400-500ms)
        kotlinx.coroutines.coroutineScope {
            launch { textAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
            launch { starAlpha.animateTo(0f, tween(300)) }
            launch { taglineAlpha.animateTo(0f, tween(300)) }
            launch { dotsAlpha.animateTo(0f, tween(300)) }
        }

        onFinished()
    }

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // ── Halftone particle background (match DLavie logo) ──
        HalftoneBackground(
            modifier = Modifier.fillMaxSize(),
            dotSize = 3f,
            spacing = 26f,
            baseColor = HalftoneBright,
            alpha = 1f
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // ── "DLavie" text + star icon (match logo layout) ──
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .scale(textScale.value)
            ) {
                Text(
                    text = "DLavie",
                    color = Color.White,   // pure white (match logo)
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(6.dp))
                // Star icon dengan pulse glow (alpha animated by starGlow)
                Box(
                    modifier = Modifier
                        .alpha(starAlpha.value)
                        .size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow layer (soft white circle behind star)
                    Box(
                        Modifier
                            .matchParentSize()
                            .alpha(starGlow * 0.4f)
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    listOf(Color.White, Color.Transparent)
                                ),
                                CircleShape
                            )
                    )
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = StarWhite.copy(alpha = starGlow),
                        modifier = Modifier
                            .size(24.dp)
                            .scale(starScale)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Tagline — very subtle, near-invisible ──
            Text(
                text = "FIFA 16 Mobile · Mod Launcher",
                color = Color(0xFF555555),   // very subtle dark gray
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(Modifier.height(60.dp))

            // ── Loading dots (3 dots pulsing — pure white) ──
            LoadingDots(
                modifier = Modifier
                    .alpha(dotsAlpha.value)
                    .size(40.dp, 8.dp)
            )
        }
    }
}

/**
 * Three pulsing dots — pure white (v3.0 monochrome).
 * Each dot fades in/out with 200ms stagger.
 */
@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val dotsTransition = rememberInfiniteTransition(label = "dots")
    val dot1 by dotsTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, delayMillis = 0, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by dotsTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, delayMillis = 200, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by dotsTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, delayMillis = 400, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(alpha = dot1)
        Dot(alpha = dot2)
        Dot(alpha = dot3)
    }

    // ── DLavie Portal Connect handler ──
    // Called when deep link dlavie://connect?callback=URL is received.
    // Shows "DLavie Portal Connected" status, then redirects back to web
    // with the user's auth token so the web can auto-login.
}
}