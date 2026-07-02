package com.drmacze.f16launcher

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    // compose-shimmer 1.2.0 API: rememberShimmer() tanpa parameter
    val shimmerInstance = rememberShimmer()
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

// ─── Tappable Card dengan press animation ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTTappableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_press"
    )

    Card(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke),
        interactionSource = interactionSource
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
    onClick: () -> Unit = {}
) {
    TTTappableCard(onClick = onClick) {
        Row(
            Modifier.padding(TTSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image (rounded square gradient dengan text)
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(coverGradient)),
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
                onClick = onButtonClick,
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
