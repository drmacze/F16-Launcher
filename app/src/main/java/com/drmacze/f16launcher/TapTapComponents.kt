package com.drmacze.f16launcher

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.sqrt

// ════════════════════════════════════════════════════════════════════════════
// PHASE 4 — Shared Element Transition locals + helper
//
// SharedTransitionLayout (Compose 1.7+, stable in BOM 2024.12.01) provides a
// SharedTransitionScope. The active AnimatedContent provides an
// AnimatedVisibilityScope. Both are exposed via CompositionLocals so deep
// composables (TTGameCard cover, GameDetailScreen cover) can opt-in to a
// shared element transition WITHOUT threading scopes through every signature.
//
// If either local is null (e.g. composable rendered outside the shared shell),
// the helper degrades gracefully to a no-op Modifier — zero behavior change.
// ════════════════════════════════════════════════════════════════════════════
val LocalSharedTransitionScope =
    compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope =
    compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Returns a Modifier that attaches a shared element with the given [key] when
 * both the SharedTransitionScope and the active AnimatedVisibilityScope are
 * present in the composition. Returns an empty Modifier otherwise.
 *
 * Used by TTGameCard cover (source) and GameDetailScreen cover (target) so the
 * small cover morphs into the large cover during the Beranda → Detail transition.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedGameCoverModifier(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return Modifier
    val animatedScope = LocalNavAnimatedVisibilityScope.current ?: return Modifier
    return with(sharedScope) {
        Modifier.sharedElement(
            rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// TAPTAP-LEVEL CUSTOM COMPONENTS
// Shimmer skeletons, tappable cards (press scale), banner carousel (auto-scroll
// HorizontalPager), game card (TapTap-style), section header.
// ════════════════════════════════════════════════════════════════════════════

// ─── Shimmer Skeleton ─────────────────────────────────────────────────────────
// Pakai valentinilk shimmer library untuk efek shimmer yang smooth.
// rememberShimmer() menyediakan instance shimmer yang di-share antar composable
// dalam tree yang sama (lebih efisien daripada animasi manual).

@Composable
fun TTShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = TTShapes.small
) {
    // compose-shimmer 1.2.0 API: rememberShimmer dengan explicit ShimmerBounds
    val shimmerInstance = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    Box(
        modifier
            .clip(shape)
            .shimmer(shimmerInstance)
            .background(Surface2)
    )
}

@Composable
fun TTGameCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Row(
            Modifier.padding(TTSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TTShimmerBox(Modifier.size(56.dp), RoundedCornerShape(14.dp))
            Spacer(Modifier.width(TTSpacing.lg))
            Column(Modifier.weight(1f)) {
                TTShimmerBox(Modifier.fillMaxWidth(0.7f).height(16.dp))
                Spacer(Modifier.height(TTSpacing.sm))
                TTShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp))
            }
        }
    }
}

// ─── Tappable Card dengan press animation + haptic feedback ───────────────────
// Phase 4: adds light haptic on click and (optional) long-press haptic via
// combinedClickable. When onLongClick is non-null, long-press fires a
// HapticFeedbackType.LongPress tick then the callback. The press-scale spring
// + ripple are preserved via the shared interactionSource + LocalIndication.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TTTappableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_press"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .combinedClickable(
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
            ),
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Column(content = content)
    }
}

// ─── Banner Carousel (HorizontalPager dengan auto-scroll) ─────────────────────

data class BannerItem(
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val icon: ImageVector? = null
)

@Composable
fun TTBannerCarousel(
    banners: List<BannerItem>,
    modifier: Modifier = Modifier,
    onBannerClick: (BannerItem) -> Unit = {}
) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })
    val scope = rememberCoroutineScope()

    // Auto-scroll setiap 4 detik
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % banners.size
            scope.launch {
                pagerState.animateScrollToPage(
                    nextPage,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Box(modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            pageSpacing = TTSpacing.md
        ) { page ->
            TTBannerItem(banners[page]) { onBannerClick(banners[page]) }
        }

        // Page indicator dots
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = TTSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(banners.size) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 20.dp else 6.dp,
                    animationSpec = tween(300),
                    label = "dot_$index"
                )
                Box(
                    Modifier
                        .size(width = width, height = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) CandyCyan else Color.White.copy(0.3f)
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TTBannerItem(
    banner: BannerItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        shape = TTShapes.cardLarge,
        colors = CardDefaults.cardColors(containerColor = Carbon),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(banner.gradientColors)
                )
        ) {
            // Mesh gradient overlay (radial highlight top-left)
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(0.08f), Color.Transparent),
                        center = Offset(w * 0.2f, h * 0.2f),
                        radius = w * 0.6f
                    )
                )
            }

            Column(
                Modifier.fillMaxSize().padding(TTSpacing.xl),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        banner.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(TTSpacing.xs))
                    Text(
                        banner.subtitle,
                        color = Color.White.copy(0.7f),
                        fontSize = 12.sp
                    )
                }
                banner.icon?.let {
                    Icon(it, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

// ─── Game Card (TapTap style) ─────────────────────────────────────────────────
// Phase 4: optional sharedContentKey attaches a shared element to the cover so
// it morphs into GameDetailScreen's cover during the Beranda → Detail transition.
// onLongClick enables long-press haptic feedback (e.g. context preview later).

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TTGameCard(
    title: String,
    subtitle: String,
    rating: String,
    coverGradient: List<Color>,
    coverText: String,
    buttonLabel: String,
    buttonEnabled: Boolean = true,
    onButtonClick: () -> Unit,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    sharedContentKey: String? = null
) {
    val haptic = LocalHapticFeedback.current
    TTTappableCard(onClick = onClick, onLongClick = onLongClick) {
        Row(
            Modifier.padding(TTSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image (rounded square gradient dengan text) — shared element
            // target when sharedContentKey is set & the shell provides scopes.
            val coverModifier = if (sharedContentKey != null) {
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(coverGradient))
                    .then(sharedGameCoverModifier(sharedContentKey))
            } else {
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(coverGradient))
            }
            Box(
                coverModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(coverText, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(TTSpacing.lg))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = SoftText, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Text("★ $rating", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            // Button
            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onButtonClick()
                },
                enabled = buttonEnabled,
                shape = TTShapes.chip,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = NeonGreen.copy(0.15f),
                    contentColor = NeonGreen,
                    disabledContainerColor = Surface2,
                    disabledContentColor = SubText
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun TTSectionHeader(
    title: String,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = TTSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(TTSpacing.sm))
        }
        Text(
            title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

// ─── Verified Badge (TapTap-style, untuk "Trusted by DLavie") ─────────────────
@Composable
fun TTVerifiedBadge(text: String = "OFFICIAL") {
    androidx.compose.material3.Surface(
        color = NeonGreen.copy(0.16f),
        border = BorderStroke(1.dp, NeonGreen.copy(0.45f)),
        shape = TTShapes.chip
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Rounded.Verified, null, tint = NeonGreen, modifier = Modifier.size(11.dp))
            Text(text, color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// DLAVIE v3.0 — Halftone Particle Background + Lottie Loading
// Signature visual elements inspired by DLavie logo (halftone dots, white star).
// ════════════════════════════════════════════════════════════════════════════

// ─── Halftone particle background — inspired by DLavie logo ───────────────────
// Grid of dots dengan varying opacity (radial gradient: brighter at corners,
// dimmer at center) + subtle animated wave. Pure black base. Optional alpha
// overlay untuk subtle usage (e.g. behind content di Beranda).
@Composable
fun HalftoneBackground(
    modifier: Modifier = Modifier,
    dotSize: Float = 3f,
    spacing: Float = 24f,
    baseColor: Color = HalftoneBright,
    alpha: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "halftone")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(6000, easing = LinearEasing), RepeatMode.Reverse
        ),
        label = "wave"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        // Base: pure near-black (match logo bg)
        drawRect(Color(0xFF0A0A0A).copy(alpha = alpha))

        // Halftone dots grid — brighter di pinggir, dimmer di tengah (seperti logo)
        val maxDist = sqrt(w * w + h * h) / 2f
        var y = 0f
        while (y < h) {
            var x = 0f
            while (x < w) {
                // Distance from center for radial gradient effect
                val dx = x - w / 2f
                val dy = y - h / 2f
                val dist = sqrt(dx * dx + dy * dy)
                val normalizedDist = (dist / maxDist).coerceIn(0f, 1f)

                // Brighter di pinggir (normalizedDist tinggi), dimmer di tengah
                val baseAlpha = 0.10f + normalizedDist * 0.25f

                // Wave animation (subtle moving highlight)
                val waveOffset = sin((x + y) * 0.01f + waveProgress * 6f) * 0.08f
                val dotAlpha = (baseAlpha + waveOffset).coerceIn(0.04f, 0.40f) * alpha

                drawCircle(
                    color = baseColor.copy(alpha = dotAlpha),
                    radius = dotSize,
                    center = Offset(x, y)
                )
                x += spacing
            }
            y += spacing
        }

        // Subtle radial glow di pojok (seperti logo — brighter di corners)
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(0.04f * alpha),
                    Color.Transparent
                ),
                center = Offset(0f, 0f),  // top-left corner
                radius = w * 0.6f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(0.04f * alpha),
                    Color.Transparent
                ),
                center = Offset(w, h),  // bottom-right corner
                radius = w * 0.6f
            )
        )
    }
}

// ─── Lottie Loading (Phase 3) ─────────────────────────────────────────────────
// Rotating white circle outline — minimal, monochrome, matches DLavie theme.
// Pakai lottie-compose (already in dependencies). Falls back gracefully kalau
// composition belum ready (empty canvas).
@Composable
fun LottieLoading(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading_animation)
    )
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}

// ─── DLavie Logo Cover (v3.0 monochrome — match logo) ─────────────────────────
// Black bg + white "DL" text + subtle halftone dots + white border.
// Replaces the old gradient-based DL cover (cyan/violet) di semua screens.
// Shape configurable (default CircleShape untuk avatar-style).
@Composable
fun DLavieLogoCover(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    text: String = "DL",
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp
) {
    val sizeDp = size   // alias supaya tidak shadow DrawScope.size di Canvas
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(shape)
            .background(Color(0xFF0A0A0A))   // near-black (match logo bg)
            .border(BorderStroke(borderWidth, Color.White.copy(0.25f)), shape),
        contentAlignment = Alignment.Center
    ) {
        // Subtle halftone dots overlay (very faint)
        Canvas(Modifier.matchParentSize()) {
            val w = this.size.width    // DrawScope size (pixels)
            val h = this.size.height
            val spacing = 8f
            var y = spacing / 2f
            while (y < h) {
                var x = spacing / 2f
                while (x < w) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = 1f,
                        center = Offset(x, y)
                    )
                    x += spacing
                }
                y += spacing
            }
        }
        // White "DL" text (inverted from old black-on-gradient)
        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Black
        )
    }
}
