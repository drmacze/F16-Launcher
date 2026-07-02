package com.drmacze.f16launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium splash screen — black background with "DLavie" text in silver
 * glassmorphism finish, animated light sweep (shiny), and elegant chime.
 *
 * Flow:
 *  1. Fade in text + subtle scale-up
 *  2. Light sweep moves left → right across text (1.2s)
 *  3. After sweep, hold 400ms then fade out
 *  4. Navigate to next activity (DLavieGuidedActivity or ModernLauncherActivity)
 *
 * Sound: shiny chime plays in sync with light sweep.
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
                    ShinySplashContent(
                        onFinished = {
                            // Decide next destination based on auth state (read SharedPreferences directly)
                            val prefs = getSharedPreferences("dlavie_auth_session", android.content.Context.MODE_PRIVATE)
                            val token = prefs.getString("access_token", null)
                            val target = if (!token.isNullOrBlank()) {
                                // Sync to community prefs (so CommunityApi works)
                                val refresh = prefs.getString("refresh_token", "") ?: ""
                                val email = prefs.getString("email", "") ?: ""
                                // Decode user ID from JWT (sub claim)
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
private fun ShinySplashContent(onFinished: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Phase: 0 = fade-in, 1 = shine sweep, 2 = hold, 3 = fade-out
    var phase by remember { mutableStateOf(0) }
    // Sweep progress 0 → 1
    val sweepProgress = remember { Animatable(0f) }
    // Fade in/out alpha
    val alphaAnim = remember { Animatable(0f) }
    // Scale (slight scale-up during fade-in)
    val scaleAnim = remember { Animatable(0.92f) }

    // Subtle ambient glow pulse behind text (continuous)
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val ambientGlow by infiniteTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ambient_glow"
    )

    // Phase machine
    LaunchedEffect(Unit) {
        // Phase 0: fade in (500ms)
        alphaAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        scaleAnim.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        phase = 1

        // Play chime in sync with shine sweep
        scope.launch { SoundEffectHelper.playShinyChime() }

        // Phase 1: shine sweep (1200ms)
        sweepProgress.animateTo(1f, tween(1200, easing = LinearEasing))
        phase = 2

        // Phase 2: hold (500ms)
        delay(500)
        phase = 3

        // Phase 3: fade out (500ms)
        alphaAnim.animateTo(0f, tween(500, easing = FastOutSlowInEasing))

        // Navigate
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Ambient radial glow behind text
        Canvas(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE5E7EB).copy(alpha = ambientGlow),
                                Color(0xFF9CA3AF).copy(alpha = ambientGlow * 0.5f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 0.7f
                        )
                    )
                }
        ) {}

        // Main content: animated DLavie text
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // ── DL Logo (silver glass circle) ──
            androidx.compose.animation.AnimatedVisibility(
                visible = alphaAnim.value > 0.01f,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(200))
            ) {
                SilverGlassLogo(
                    scale = scaleAnim.value,
                    alpha = alphaAnim.value,
                    sweepProgress = sweepProgress.value
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── "DLavie" text with silver glass + light sweep ──
            ShinySilverText(
                text = "DLavie",
                alpha = alphaAnim.value,
                scale = scaleAnim.value,
                sweepProgress = sweepProgress.value
            )

            Spacer(Modifier.height(12.dp))

            // ── Tagline (fades in after sweep starts) ──
            androidx.compose.animation.AnimatedVisibility(
                visible = phase >= 1 && alphaAnim.value > 0.1f,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(200))
            ) {
                Text(
                    "FIFA 16 Mobile · Mod Launcher",
                    color = Color(0xFF9CA3AF).copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp
                )
            }

            Spacer(Modifier.height(60.dp))

            // ── Loading indicator (small) ──
            androidx.compose.animation.AnimatedVisibility(
                visible = phase >= 2,
                enter = fadeIn(tween(400)),
                exit = fadeOut(tween(200))
            ) {
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "loader_pulse"
                )
                Text(
                    "• • •",
                    color = Color(0xFFE5E7EB).copy(alpha = pulse * 0.6f),
                    fontSize = 18.sp,
                    letterSpacing = 8.sp
                )
            }
        }
    }
}

/**
 * Silver glassmorphism "DL" logo circle with light sweep.
 */
@Composable
private fun SilverGlassLogo(scale: Float, alpha: Float, sweepProgress: Float) {
    Box(
        Modifier
            .androidx_scale(scale)
            .size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sizePx = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer glow
            drawCircle(
                color = Color(0xFFE5E7EB).copy(alpha = 0.15f * alpha),
                radius = sizePx / 2f * 1.15f,
                center = center
            )

            // Silver glass body (radial gradient)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF9FAFB).copy(alpha = 0.25f * alpha),
                        Color(0xFF9CA3AF).copy(alpha = 0.15f * alpha),
                        Color(0xFF374151).copy(alpha = 0.20f * alpha)
                    ),
                    center = Offset(size.width * 0.35f, size.height * 0.35f),
                    radius = sizePx / 2f
                ),
                radius = sizePx / 2f,
                center = center
            )

            // Silver border ring
            drawCircle(
                color = Color(0xFFE5E7EB).copy(alpha = 0.7f * alpha),
                radius = sizePx / 2f,
                center = center,
                style = Stroke(width = 2f)
            )

            // Light sweep across the circle (clipped to circle)
            val sweepX = -0.3f + sweepProgress * 1.6f  // -0.3 → 1.3 (extends beyond circle)
            if (sweepX in -0.5f..1.5f) {
                val cx = size.width * sweepX
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.6f * alpha),
                            Color(0xFFE5E7EB).copy(alpha = 0.4f * alpha),
                            Color.Transparent
                        ),
                        start = Offset(cx - size.width * 0.15f, 0f),
                        end = Offset(cx + size.width * 0.15f, size.height)
                    ),
                    radius = sizePx / 2f,
                    center = center
                )
            }
        }

        // "DL" text inside
        Text(
            "DL",
            color = Color(0xFFF9FAFB).copy(alpha = alpha),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.androidx_scale(scale)
        )
    }
}

/**
 * "DLavie" text with:
 *  - Silver glassmorphism gradient (multi-stop: bright silver → mid → dark silver → bright)
 *  - Light sweep overlay (clipped to text bounds) — moving highlight
 *  - Soft drop shadow
 *  - Animated scale on entry
 */
@Composable
private fun ShinySilverText(
    text: String,
    alpha: Float,
    scale: Float,
    sweepProgress: Float
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.androidx_scale(scale)
    ) {
        // Main text with silver glassmorphism gradient via drawWithContent
        Text(
            text = text,
            color = Color.White,  // overridden by drawWithContent brush
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            shadow = Shadow(
                color = Color(0xFFE5E7EB).copy(alpha = 0.5f * alpha),
                blurRadius = 12f
            ),
            modifier = Modifier
                .drawWithContent {
                    // Draw the text first (with white color)
                    drawContent()
                    // Overlay silver glass gradient on top using srcatop-like blend
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6B7280).copy(alpha = alpha),
                            Color(0xFFF9FAFB).copy(alpha = alpha),
                            Color(0xFFD1D5DB).copy(alpha = alpha),
                            Color(0xFF9CA3AF).copy(alpha = alpha),
                            Color(0xFFF9FAFB).copy(alpha = alpha),
                            Color(0xFF6B7280).copy(alpha = alpha)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                    drawRect(brush = brush, blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop)
                }
        )

        // Light sweep highlight overlay — moving vertical band across text
        val sweepX = -0.3f + sweepProgress * 1.6f
        if (sweepX in -0.5f..1.5f && alpha > 0.1f) {
            Box(
                Modifier
                    .height(80.dp)
                    .drawWithContent {
                        // Draw the text in white as a mask
                        drawContent()
                    }
            ) {
                // The actual sweep is drawn as a separate layer over the text
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width * sweepX
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.85f * alpha),
                                Color(0xFFF9FAFB).copy(alpha = 0.95f * alpha),
                                Color.White.copy(alpha = 0.85f * alpha),
                                Color.Transparent
                            ),
                            start = Offset(cx - size.width * 0.08f, 0f),
                            end = Offset(cx + size.width * 0.08f, size.height)
                        )
                    )
                }
            }
        }
    }
}

// Local helper to apply scale cleanly
private fun Modifier.androidx_scale(scale: Float): Modifier =
    this.then(Modifier.scale(scale))
