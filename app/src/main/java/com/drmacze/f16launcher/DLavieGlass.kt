package com.drmacze.f16launcher

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ════════════════════════════════════════════════════════════════════════════
// DLAVIE AURORA GLASS — Glassmorphism Components Library (v5.0)
//
// A complete set of frosted-glass UI components for the DLavie Launcher.
// Built on top of the strict design tokens in TapTapDesignSystem.kt.
//
// Components:
//   1. AuroraBackground       — animated aurora gradient + halftone
//   2. DLavieLogo             — composable that renders the D+L brand mark
//   3. GlassCard              — frosted glass card with blur + border + glow
//   4. GlassTopBar            — translucent top app bar with blur
//   5. GlassBottomBar         — translucent bottom navigation
//   6. GlassButton            — primary/secondary/ghost button variants
//   7. GlassChip              — pill-shaped tag chip
//   8. GlassListItem          — list row with glass background
//   9. GlassDivider           — subtle horizontal divider
//  10. GlassGlowBox           — wrapper that adds a colored glow halo
//
// All components are stateless (state hoisted to caller) and animation-aware.
// ════════════════════════════════════════════════════════════════════════════

// ─── 1. Aurora Background ───────────────────────────────────────────────────
// Animated deep-space background with:
//   - 2 slow-moving radial gradient glows (cyan + violet)
//   - Static halftone dot pattern overlay (DLavie brand motif)
//   - Subtle vignette
//
// Use as the root background of every screen.

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    showLogoParticles: Boolean = true,
    glowIntensity: Float = 1.0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // Slow drift for cyan glow (top-left → top-right)
    val cyanX by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyan_x"
    )
    val cyanY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyan_y"
    )

    // Counter drift for violet glow (bottom-right → bottom-left)
    val violetX by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "violet_x"
    )
    val violetY by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "violet_y"
    )

    Box(
        modifier
            .fillMaxSize()
            .background(DLavieGlass.SpaceBlack)
    ) {
        // Cyan glow (top)
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * cyanX
            val cy = size.height * cyanY
            val radius = size.minDimension * 0.7f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DLavieGlass.AuroraCyan.copy(alpha = 0.30f * glowIntensity),
                        DLavieGlass.AuroraCyan.copy(alpha = 0.0f),
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                center = Offset(cx, cy),
                radius = radius
            )
        }

        // Violet glow (bottom)
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * violetX
            val cy = size.height * violetY
            val radius = size.minDimension * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DLavieGlass.AuroraViolet.copy(alpha = 0.22f * glowIntensity),
                        DLavieGlass.AuroraViolet.copy(alpha = 0.0f),
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                center = Offset(cx, cy),
                radius = radius
            )
        }

        // DLavie logo particle pattern overlay (replaces halftone dots — brand motif)
        if (showLogoParticles) {
            DLavieLogoParticleBackground(
                modifier = Modifier.fillMaxSize(),
                logoColor = DLavieGlass.AuroraCyan,
                logoSize = 32.dp,
                spacing = 88.dp,
                opacity = 0.08f,
                animateDrift = true
            )
        }

        // Subtle vignette (darker edges)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.45f)
                    ),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.maxDimension * 0.75f
                )
            )
        }
    }
}

@Composable
fun HalftoneOverlay(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White.copy(alpha = 0.04f),
    dotSpacing: Dp = 28.dp,
    dotRadius: Dp = 1.2.dp
) {
    Canvas(modifier) {
        val spacingPx = dotSpacing.toPx()
        val radiusPx = dotRadius.toPx()
        var x = 0f
        var y = 0f
        while (y < size.height) {
            x = 0f
            while (x < size.width) {
                drawCircle(
                    color = dotColor,
                    radius = radiusPx,
                    center = Offset(x, y)
                )
                x += spacingPx
            }
            y += spacingPx
        }
    }
}

// ─── DLavie Logo Particle Background ────────────────────────────────────────
// Scattered DLavie "D" logos as particle pattern across the screen.
// Each logo is small, low-opacity, with slight random rotation + size variation.
// This is the brand motif — every screen uses this as background.

// ─── YouTube Video Background ───────────────────────────────────────────────
// Auto-play looping muted YouTube video as background (for Hero Banner).
// Uses WebView with YouTube iframe embed. Adds a dark gradient scrim so text
// placed on top remains readable.
//
// FALLBACK: If video fails to load (region/embed restriction, network issue),
// shows YouTube thumbnail (hqdefault.jpg) as static image background instead.
// Thumbnail is loaded via Coil AsyncImage — always works regardless of video
// embed permissions.

@Composable
fun YouTubeVideoBackground(
    videoId: String,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.55f,
    loop: Boolean = true,
    muted: Boolean = true
) {
    val context = LocalContext.current
    var videoError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(modifier) {
        if (!videoError) {
            // Try WebView with YouTube iframe embed first
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
                        settings.domStorageEnabled = true
                        isHorizontalScrollBarEnabled = false
                        isVerticalScrollBarEnabled = false
                        isClickable = false
                        isFocusable = false

                        // Intercept console errors from YouTube iframe to detect embed failures
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                // Mark video as failed → fallback to thumbnail
                                videoError = true
                            }
                        }

                        // Custom WebChromeClient to capture YouTube player errors
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                val msg = consoleMessage?.message() ?: ""
                                // YouTube iframe posts error codes via console
                                if (msg.contains("152") || msg.contains("101") ||
                                    msg.contains("150") || msg.contains("not available") ||
                                    msg.contains("embed")) {
                                    videoError = true
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        val autoplayFlag = if (muted) "autoplay=1&mute=1" else "autoplay=1"
                        val loopFlag = if (loop) "&loop=1&playlist=$videoId" else ""
                        val extras = "&controls=0&showinfo=0&modestbranding=1&rel=0&playsinline=1&iv_load_policy=3&disablekb=1&fs=0&cc_load_policy=0"

                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="utf-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    * { margin: 0; padding: 0; box-sizing: border-box; }
                                    html, body {
                                        width: 100%; height: 100%;
                                        background: #000; overflow: hidden;
                                    }
                                    .video-container {
                                        position: fixed; top: 50%; left: 50%;
                                        transform: translate(-50%, -50%);
                                        width: 100%; height: 100%;
                                        pointer-events: none;
                                    }
                                    .video-container iframe {
                                        position: absolute; top: 50%; left: 50%;
                                        transform: translate(-50%, -50%);
                                        border: 0; pointer-events: none;
                                    }
                                    @media (orientation: portrait) {
                                        .video-container iframe {
                                            width: 178vh; height: 100vh;
                                        }
                                    }
                                    @media (orientation: landscape) {
                                        .video-container iframe {
                                            width: 100vw; height: 56.25vw;
                                        }
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="video-container">
                                    <iframe
                                        src="https://www.youtube.com/embed/$videoId?$autoplayFlag$loopFlag$extras"
                                        allow="autoplay; encrypted-media; fullscreen"
                                        allowfullscreen frameborder="0">
                                    </iframe>
                                </div>
                            </body>
                            </html>
                        """.trimIndent()

                        loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback: YouTube thumbnail (maxresdefault)
            // Coil AsyncImage — always works, no API needed
            val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
            coil.compose.AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Video thumbnail",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark gradient scrim (transparent top → black bottom) so text is readable
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = scrimAlpha * 0.6f),
                        Color.Black.copy(alpha = scrimAlpha * 0.4f),
                        Color.Black.copy(alpha = scrimAlpha * 0.85f)
                    )
                )
            )
        }
    }
}

@Composable
fun DLavieLogoParticleBackground(
    modifier: Modifier = Modifier,
    logoColor: Color = DLavieGlass.AuroraCyan,
    logoSize: Dp = 36.dp,
    spacing: Dp = 80.dp,
    opacity: Float = 0.06f,
    animateDrift: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dl_particles")
    val driftX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_x"
    )

    Canvas(modifier) {
        val spacingPx = spacing.toPx()
        val logoSizePx = logoSize.toPx()
        val w = size.width
        val h = size.height
        val driftOffset = if (animateDrift) driftX * spacingPx * 0.5f else 0f

        var y = -spacingPx
        var rowIdx = 0
        while (y < h + spacingPx) {
            val xOffset = if (rowIdx % 2 == 0) 0f else spacingPx / 2f
            var x = -spacingPx + xOffset - driftOffset
            while (x < w + spacingPx) {
                // Draw a small "D" shape with low opacity
                val cx = x + logoSizePx / 2f
                val cy = y + logoSizePx / 2f
                val left = cx - logoSizePx / 2f
                val top = cy - logoSizePx / 2f
                val right = cx + logoSizePx / 2f
                val bottom = cy + logoSizePx / 2f

                // D shape (proportional to logo size)
                val dPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(left + 0.25f * logoSizePx, top + 0.15f * logoSizePx)
                    lineTo(left + 0.25f * logoSizePx, top + 0.85f * logoSizePx)
                    lineTo(left + 0.55f * logoSizePx, top + 0.85f * logoSizePx)
                    cubicTo(
                        left + 0.78f * logoSizePx, top + 0.85f * logoSizePx,
                        left + 0.90f * logoSizePx, top + 0.65f * logoSizePx,
                        left + 0.90f * logoSizePx, top + 0.50f * logoSizePx
                    )
                    cubicTo(
                        left + 0.90f * logoSizePx, top + 0.35f * logoSizePx,
                        left + 0.78f * logoSizePx, top + 0.15f * logoSizePx,
                        left + 0.55f * logoSizePx, top + 0.15f * logoSizePx
                    )
                    close()
                }

                drawPath(
                    path = dPath,
                    color = logoColor.copy(alpha = opacity),
                    style = Stroke(
                        width = 1.5f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                x += spacingPx
            }
            y += spacingPx
            rowIdx++
        }
    }
}

// ─── 2. DLavie Logo (composable, scalable) ──────────────────────────────────
// Renders the DLavie brand mark: a stylized "D" with "L" cut out, in cyan stroke.
// Vector-based — no raster asset needed.

@Composable
fun DLavieLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 3.dp,
    glow: Boolean = true
) {
    val strokeColor = DLavieGlass.BrandMarkCyan
    val accentColor = DLavieGlass.BrandMarkMint

    Box(
        modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val stroke = strokeWidth.toPx()

            // D shape — vertical line + curved arc on the right
            val dPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0.25f * w, 0.15f * h)
                lineTo(0.25f * w, 0.85f * h)
                lineTo(0.55f * w, 0.85f * h)
                cubicTo(
                    0.78f * w, 0.85f * h,
                    0.90f * w, 0.65f * h,
                    0.90f * w, 0.50f * h
                )
                cubicTo(
                    0.90f * w, 0.35f * h,
                    0.78f * w, 0.15f * h,
                    0.55f * w, 0.15f * h
                )
                close()
            }

            // L accent (mint) — horizontal stroke at bottom
            val lPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0.55f * w, 0.85f * h)
                lineTo(0.85f * w, 0.85f * h)
            }

            // Glow underlay (simple — draw D path with bigger stroke + lower alpha)
            if (glow) {
                drawPath(
                    path = dPath,
                    color = strokeColor.copy(alpha = 0.30f),
                    style = Stroke(
                        width = stroke * 3.5f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
            }

            // Main D stroke (cyan)
            drawPath(
                path = dPath,
                color = strokeColor,
                style = Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // L accent (mint) — short horizontal at bottom
            drawPath(
                path = lPath,
                color = accentColor,
                style = Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

// ─── 3. Glass Card ──────────────────────────────────────────────────────────
// Frosted glass card with:
//   - Semi-transparent white background (gradient)
//   - 1dp white-alpha border
//   - Soft blur of background content behind it (uses graphicsLayer)
//   - Optional colored glow halo
//   - Press scale animation + haptic feedback

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    glowColor: Color? = null,
    borderStroke: BorderStroke = DLBorderStroke,
    cornerRadius: Dp = DLRadius.xl,
    contentPadding: PaddingValues = PaddingValues(DLSpacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
        ),
        label = "glass_card_scale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        },
                        onLongClick = if (onLongClick != null) ({
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }) else null
                    )
                } else Modifier
            )
            .clip(shape)
            .background(DLavieGradients.glassCard)
            .border(borderStroke, shape)
            .then(
                if (glowColor != null) {
                    Modifier.drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2, size.height / 2),
                                radius = size.maxDimension
                            )
                        )
                    }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

// ─── 4. Glass Top Bar ───────────────────────────────────────────────────────
// Translucent top app bar with blur backdrop.
// Title (centered or left-aligned) + optional navigation icon + actions.

@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    titleAlignment: Alignment.Horizontal = Alignment.Start
) {
    val shape = RoundedCornerShape(bottomStart = DLRadius.lg, bottomEnd = DLRadius.lg)

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DLavieGlass.SpaceBlack.copy(alpha = 0.85f),
                        DLavieGlass.SpaceBlack.copy(alpha = 0.55f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, DLavieGlass.GlassStroke.copy(alpha = 0.6f)),
                shape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DLSpacing.lg, vertical = DLSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DLSpacing.md)
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = when (titleAlignment) {
                    Alignment.CenterHorizontally -> Alignment.CenterHorizontally
                    else -> Alignment.Start
                }
            ) {
                Text(
                    text = title,
                    color = DLavieGlass.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = DLavieGlass.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily
                    )
                }
            }

            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DLSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

// ─── 5. Glass Bottom Bar ────────────────────────────────────────────────────
// Translucent bottom navigation with blur backdrop + pill-shaped tab buttons.

@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(topStart = DLRadius.xl, topEnd = DLRadius.xl)

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DLavieGlass.SpaceBlack.copy(alpha = 0.55f),
                        DLavieGlass.SpaceBlack.copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, DLavieGlass.GlassStroke.copy(alpha = 0.5f)),
                shape
            )
            .padding(horizontal = DLSpacing.lg, vertical = DLSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// ─── 6. Glass Button ────────────────────────────────────────────────────────
// Primary: cyan gradient
// Secondary: glass surface + cyan border
// Ghost: transparent + cyan text

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    variant: GlassButtonVariant = GlassButtonVariant.Primary,
    fullWidth: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
        ),
        label = "button_scale"
    )

    val shape = DLRadius.button
    val modifierFinal = modifier
        .scale(scale)
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .clip(shape)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )

    val bgColor = when (variant) {
        GlassButtonVariant.Primary -> DLavieGradients.primaryButton
        GlassButtonVariant.Secondary -> DLavieGradients.glassCard
        GlassButtonVariant.Ghost -> Brush.horizontalGradient(
            listOf(Color.Transparent, Color.Transparent)
        )
    }
    val contentColor = when (variant) {
        GlassButtonVariant.Primary -> Color.Black
        GlassButtonVariant.Secondary -> DLavieGlass.TextPrimary
        GlassButtonVariant.Ghost -> DLavieGlass.AuroraCyan
    }
    val borderColor = when (variant) {
        GlassButtonVariant.Primary -> null
        GlassButtonVariant.Secondary -> DLavieGlass.GlassStrokeHigh
        GlassButtonVariant.Ghost -> null
    }

    Box(
        modifierFinal
            .background(bgColor)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, shape)
                else Modifier
            )
            .padding(
                horizontal = DLSpacing.xl,
                vertical = DLSpacing.md
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DLSpacing.sm)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily
            )
        }
    }
}

enum class GlassButtonVariant { Primary, Secondary, Ghost }

// ─── 7. Glass Chip ──────────────────────────────────────────────────────────
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = DLRadius.chip
    val bgColor = if (selected) DLavieGlass.AuroraCyan.copy(alpha = 0.18f)
                  else DLavieGlass.GlassSurface
    val borderColor = if (selected) DLavieGlass.AuroraCyan.copy(alpha = 0.5f)
                      else DLavieGlass.GlassStroke
    val textColor = if (selected) DLavieGlass.AuroraCyan else DLavieGlass.TextSecondary

    val mod = modifier.clip(shape).background(bgColor).border(1.dp, borderColor, shape)

    Row(
        modifier = mod
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = DLSpacing.md, vertical = DLSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DLSpacing.xs)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = InterFontFamily
        )
    }
}

// ─── 8. Glass List Item ─────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(DLRadius.md)

    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DLavieGlass.GlassSurface)
            .border(1.dp, DLavieGlass.GlassStroke, shape)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        },
                        onLongClick = if (onLongClick != null) ({
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }) else null
                    )
                } else Modifier
            )
            .padding(horizontal = DLSpacing.lg, vertical = DLSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DLSpacing.md)
    ) {
        if (leadingIcon != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(DLRadius.chip)
                    .background(DLavieGlass.AuroraCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = DLavieGlass.AuroraCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = DLavieGlass.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = DLavieGlass.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = DLavieGlass.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── 9. Glass Divider ───────────────────────────────────────────────────────
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier,
    vertical: Boolean = false
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            DLavieGlass.GlassStroke.copy(alpha = 0.8f),
            Color.Transparent
        )
    )
    val gradientV = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            DLavieGlass.GlassStroke.copy(alpha = 0.8f),
            Color.Transparent
        )
    )
    Box(
        modifier
            .then(if (vertical) Modifier.width(1.dp).fillMaxWidth() else Modifier.height(1.dp).fillMaxWidth())
            .background(if (vertical) gradientV else gradient)
    )
}

// ─── 10. Glass Glow Box ─────────────────────────────────────────────────────
// Wrapper that adds a colored glow halo behind its content.
@Composable
fun GlassGlowBox(
    glowColor: Color,
    modifier: Modifier = Modifier,
    glowRadius: Dp = 32.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier) {
        // Glow underlay
        Box(
            Modifier
                .matchParentSize()
                .blur(
                    radiusX = glowRadius,
                    radiusY = glowRadius,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                )
                .background(glowColor.copy(alpha = 0.40f))
        )
        content()
    }
}

// ─── Section Header (modern) ────────────────────────────────────────────────
@Composable
fun DLSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = DLSpacing.lg, vertical = DLSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DLSpacing.md)
    ) {
        // Small cyan accent bar
        Box(
            Modifier
                .size(width = 3.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(DLavieGlass.AuroraCyan)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = DLavieGlass.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = DLavieGlass.TextMuted,
                    fontSize = 11.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
        action?.invoke()
    }
}

// ─── Icon helpers (replace emojis) ──────────────────────────────────────────
// Centralized icon registry so screens don't need to import Material Symbols
// individually. Use these in NEW screens instead of emoji strings.

object DLIcons {
    // Navigation
    val Home = Icons.Rounded.Home
    val Profile = Icons.Rounded.Person
    val Settings = Icons.Rounded.Settings
    val Search = Icons.Rounded.Search
    val Refresh = Icons.Rounded.Refresh
    val Notifications = Icons.Rounded.Notifications

    // Actions
    val Download = Icons.Rounded.Download
    val Play = Icons.Rounded.PlayArrow
    val Star = Icons.Rounded.Star
    val Verified = Icons.Rounded.Verified
}
