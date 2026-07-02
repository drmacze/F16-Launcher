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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
 * Premium splash screen — Grok-inspired minimal design with **shiny silver text**.
 *
 * Design philosophy:
 *  - Pure black background (no gradient noise)
 *  - **Animated gradient text** (silver shimmer sweep) — Bug 5 Part B fix.
 *    Uses official Compose `TextStyle(brush = Brush.linearGradient(...))` API,
 *    NOT buggy `BlendMode.SrcAtop`. Works on all devices.
 *  - Minimal motion: smooth fade + subtle scale + 1.2s shimmer sweep
 *  - Cinematic sound (MediaPlayer + raw WAV) plays at the start of sweep phase
 *  - Animated dots loader (3 pulsing dots) at bottom
 *
 * Flow (~4s total):
 *  1. Fade in "DLavie" text + scale-up (600-800ms, overlapping)
 *  2. Shimmer sweep across text (1.2s) + play cinematic sound at start
 *  3. Fade in tagline (400ms)
 *  4. Fade in dots loader (300ms)
 *  5. Hold (500ms)
 *  6. Fade out everything (500ms)
 *  7. Navigate to next screen
 */
class ShinySplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color.Black, surface = Color.Black,
                onBackground = Color.White, onSurface = Color.White
            )) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    GrokStyleSplash(
                        onFinished = {
                            // Decide next destination based on auth state
                            val prefs = getSharedPreferences("dlavie_auth_session", android.content.Context.MODE_PRIVATE)
                            val token = prefs.getString("access_token", null)
                            val target = if (!token.isNullOrBlank()) {
                                // Sync to community prefs
                                val refresh = prefs.getString("refresh_token", "") ?: ""
                                val userId = try {
                                    val payload = token.split(".").getOrNull(1) ?: ""
                                    val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
                                    val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
                                    org.json.JSONObject(String(decoded)).optString("sub", "")
                                } catch (_: Exception) { "" }
                                getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE).edit()
                                    .putString("access_token", token)
                                    .putString("refresh_token", refresh)
                                    .putString("user_id", userId)
                                    .apply()
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
private fun GrokStyleSplash(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Animation states
    val textAlpha = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.92f) }
    val taglineAlpha = remember { Animatable(0f) }
    val dotsAlpha = remember { Animatable(0f) }
    val sweepProgress = remember { Animatable(0f) }  // shimmer sweep 0→1

    // Phase machine
    LaunchedEffect(Unit) {
        // Phase 1: Fade in "DLavie" with slight scale-up (600ms text alpha, 800ms scale, overlapping)
        kotlinx.coroutines.coroutineScope {
            launch { textAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            launch { textScale.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        }

        // Phase 2: Shimmer sweep across text (1.2s) + play cinematic sound at start.
        // Sound: MediaPlayer + raw WAV (Bug 5 Part A fix) — reliable di semua device.
        scope.launch { SoundEffectHelper.playShinyChime(context) }
        sweepProgress.animateTo(1f, tween(1200, easing = LinearEasing))

        // Phase 3: Fade in tagline (400ms)
        taglineAlpha.animateTo(0.6f, tween(400, easing = FastOutSlowInEasing))

        // Phase 4: Fade in dots loader (300ms)
        dotsAlpha.animateTo(1f, tween(300))

        // Phase 5: Hold (500ms) — biarkan user menikmati shimmer + sound
        delay(500)

        // Phase 6: Fade out everything (400-500ms)
        kotlinx.coroutines.coroutineScope {
            launch { textAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
            launch { taglineAlpha.animateTo(0f, tween(300)) }
            launch { dotsAlpha.animateTo(0f, tween(300)) }
            launch { sweepProgress.animateTo(0f, tween(200)) }
        }

        onFinished()
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Main "DLavie" text — animated silver shimmer gradient ──
            // Bug 5 Part B: pakai TextStyle(brush = animatedGradient) untuk shiny effect.
            // Ini official Compose API yang WORKS di semua device (unlike BlendMode.SrcAtop).
            ShinySilverText(
                text = "DLavie",
                alpha = textAlpha.value,
                scale = textScale.value,
                sweepProgress = sweepProgress.value
            )

            Spacer(Modifier.height(12.dp))

            // ── Tagline — subtle gray, small ──
            Text(
                text = "FIFA 16 Mobile · Mod Launcher",
                color = Color(0xFF6B7280),  // subtle gray (Grok-style)
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,  // wide tracking for elegance
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(Modifier.height(60.dp))

            // ── Loading dots (3 dots pulsing in sequence) ──
            LoadingDots(
                modifier = Modifier
                    .alpha(dotsAlpha.value)
                    .size(40.dp, 8.dp)
            )
        }
    }
}

/**
 * Animated silver shimmer text — Bug 5 Part B fix.
 *
 * Pakai `TextStyle(brush = Brush.linearGradient(...))` (official Compose API)
 * dengan gradient yang animated offset-nya untuk efek "shiny sweep".
 *
 * Color stops: dark silver → bright white → light silver → mid silver → bright white → dark silver.
 * Sweep offset bergerak dari -300 ke +900 px (relative to text bounding box)
 * sesuai `sweepProgress` (0 → 1), menghasilkan highlight yang melintasi text.
 *
 * Catatan: `Brush.linearGradient` dengan `start`/`end` Offset adalah official API
 * yang support di Compose 1.5+ (compileSdk 35, Compose BOM 2024.10.00).
 * Tidak seperti BlendMode.SrcAtop yang buggy di beberapa device.
 */
@Composable
private fun ShinySilverText(
    text: String,
    alpha: Float,
    scale: Float,
    sweepProgress: Float
) {
    // Animated shimmer gradient — sweep dari kiri ke kanan.
    // sweepProgress 0 → 1 menggerakkan gradient offset dari -300 ke +900.
    val sweepX = -300f + sweepProgress * 1200f
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF6B7280),     // dark silver
            Color(0xFFF9FAFB),     // bright white
            Color(0xFFE5E7EB),     // light silver
            Color(0xFF9CA3AF),     // mid silver
            Color(0xFFF9FAFB),     // bright white
            Color(0xFF6B7280),     // dark silver
        ),
        start = Offset(x = sweepX, y = 0f),
        end = Offset(x = sweepX + 300f, y = 80f)
    )

    Text(
        text = text,
        style = TextStyle(
            brush = shimmerBrush,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,  // tight tracking for premium feel
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .alpha(alpha)
            .scale(scale)
    )
}

/**
 * Three pulsing dots — clean loading indicator.
 * Each dot fades in/out with 200ms stagger.
 */
@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, delayMillis = 0, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, delayMillis = 200, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
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
}

@Composable
private fun Dot(alpha: Float) {
    Box(
        Modifier
            .size(6.dp)
            .background(Color(0xFF9CA3AF).copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape)
    )
}
