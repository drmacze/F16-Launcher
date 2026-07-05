package com.drmacze.f16launcher

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════════════════════
// DLAVIE v6.8 — TAPTAP / STEAM-STYLE REDESIGN COMPONENTS
//
// Pure monochrome (B/W) + halftone — matches existing v6.0 design system.
// New layout elements per user request:
//   1. DLavieTopBar       — hamburger + search + bell + profile avatar
//   2. StarRatingBar      — 5-star display (supports interactive mode)
//   3. DLavieHeroCarousel — full-width image cards + dots indicator + 5-star overlay
//   4. DLavieFullWidthGameCard — full-width image card with title + rating + CTA
//   5. DLavieBottomNav    — fixed bar with 5 icons (replaces FloatingNav pill)
//   6. DiscoverScreen     — new "Jelajahi" tab placeholder (5th bottom-nav tab)
// ════════════════════════════════════════════════════════════════════════════

// ─── 1. Top Bar ───────────────────────────────────────────────────────────────
// Hamburger (left) + Search (center-left) + Bell (center-right) + Profile (right).
// All icons white on transparent bg — minimal, Steam/TapTap style.
@Composable
fun DLavieTopBar(
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBellClick: () -> Unit,
    onProfileClick: () -> Unit,
    hasUnreadNotif: Boolean = false,
    profileInitial: String = "DL",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: Hamburger ──
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), CircleShape)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Center: Search pill (tap to open search) ──
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(22.dp))
                .clickable { onSearchClick() }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Cari",
                tint = SoftText,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "Cari game, komunitas, update…",
                color = SubText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── Right: Bell + Profile ──
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), CircleShape)
                .clickable { onBellClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = "Notifikasi",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            // Tiny unread dot
            if (hasUnreadNotif) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                        .border(BorderStroke(1.5.dp, PureBlack), CircleShape)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        DLavieLogoCover(
            size = 44.dp,
            text = profileInitial,
            fontSize = 14.sp,
            shape = CircleShape,
            borderWidth = 1.dp
        )
    }
}

// ─── 2. Star Rating Bar ───────────────────────────────────────────────────────
// 5 stars, supports display-only or interactive mode.
@Composable
fun StarRatingBar(
    rating: Float,                    // 0..5 (supports half-star via floor)
    interactive: Boolean = false,
    onRatingChange: ((Int) -> Unit)? = null,
    starSize: Int = 16,
    starColor: Color = Color.White,
    emptyColor: Color = Color.White.copy(alpha = 0.25f),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            val filled = i <= rating.toInt()
            val tint = if (filled) starColor else emptyColor
            val starModifier = if (interactive && onRatingChange != null) {
                Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRatingChange(i)
                }
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .size(starSize.dp)
                    .then(starModifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(starSize.dp)
                )
            }
        }
    }
}

// ─── 3. Hero Carousel ─────────────────────────────────────────────────────────
// Full-width image cards, swipeable, dots indicator below, 5-star overlay.
data class HeroSlide(
    val title: String,
    val subtitle: String,
    val rating: Float,           // 0..5
    val ratingCount: Int,
    val imageRes: Int?,          // local drawable resource for cover image
    val tag: String = "OFFICIAL"
)

@Composable
fun DLavieHeroCarousel(
    slides: List<HeroSlide>,
    modifier: Modifier = Modifier,
    onSlideClick: (HeroSlide) -> Unit = {}
) {
    if (slides.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    // Auto-scroll every 5 seconds
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % slides.size
            scope.launch {
                pagerState.animateScrollToPage(next, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            pageSpacing = 12.dp
        ) { page ->
            DLavieHeroSlide(
                slide = slides[page],
                onClick = { onSlideClick(slides[page]) }
            )
        }

        // Dots indicator — centered, 5dp below the carousel
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slides.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (selected) 22.dp else 6.dp,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "hero_dot_$index"
                )
                Box(
                    Modifier
                        .size(width = width, height = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) Color.White else Color.White.copy(alpha = 0.30f)
                        )
                )
                if (index < slides.size - 1) Spacer(Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun DLavieHeroSlide(
    slide: HeroSlide,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Carbon)
            .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        // ── Full-width background image (or fallback gradient) ──
        if (slide.imageRes != null) {
            coil.compose.AsyncImage(
                model = slide.imageRes,
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(PureBlack, Surface2, Carbon)
                        )
                    )
            )
        }

        // ── Halftone dots overlay (subtle brand motif) ──
        HalftoneBackground(
            modifier = Modifier.fillMaxSize(),
            dotSize = 1.8f,
            spacing = 20f,
            alpha = 0.35f
        )

        // ── Bottom-to-top scrim for text readability ──
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Black.copy(alpha = 0.30f),
                        1f to Color.Black.copy(alpha = 0.88f)
                    )
                )
        )

        // ── Top-right: OFFICIAL tag ──
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Text(
                slide.tag,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        // ── Bottom content: title + subtitle + 5-star rating ──
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                slide.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                slide.subtitle,
                color = SoftText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StarRatingBar(
                    rating = slide.rating,
                    starSize = 14,
                    starColor = Color.White,
                    emptyColor = Color.White.copy(alpha = 0.30f)
                )
                Text(
                    String.format("%.1f", slide.rating),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "• ${slide.ratingCount} ratings",
                    color = SubText,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ─── 4. Full-Width Image Game Card ────────────────────────────────────────────
// Used in vertical lists (Discover, etc.). Full-width image on top, content below.
@Composable
fun DLavieFullWidthGameCard(
    title: String,
    subtitle: String,
    rating: Float,
    ratingCount: Int,
    imageRes: Int?,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBase)
            .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(20.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCardClick()
            }
    ) {
        // ── Image header (16:9-ish) ──
        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(Surface2)
        ) {
            if (imageRes != null) {
                coil.compose.AsyncImage(
                    model = imageRes,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback: halftone gradient
                HalftoneBackground(
                    modifier = Modifier.fillMaxSize(),
                    dotSize = 2f,
                    spacing = 18f,
                    alpha = 0.5f
                )
            }

            // Top-right rating badge
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    Text(
                        String.format("%.1f", rating),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Content row ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = SoftText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StarRatingBar(rating = rating, starSize = 12)
                    Text(
                        "$ratingCount ratings",
                        color = SubText,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // CTA pill button (monochrome — white bg, black text)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCtaClick()
                }
            ) {
                Text(
                    ctaLabel,
                    color = PureBlack,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// ─── 5. Bottom Navigation (5 icons, fixed bar) ────────────────────────────────
// Replaces the FloatingNav pill. Material3 NavigationBar-style but custom-styled
// to match the DLavie monochrome theme.
@Composable
fun DLavieBottomNav(
    page: Page,
    onPage: (Page) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = PureBlack.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Page.values().forEach { item ->
                val selected = page == item
                val iconTint by animateColorAsState(
                    if (selected) Color.White else SubText,
                    tween(280, easing = FastOutSlowInEasing),
                    label = "bnav_tint_${item.label}"
                )
                val iconScale by animateFloatAsState(
                    if (selected) 1.15f else 1f,
                    tween(280, easing = FastOutSlowInEasing),
                    label = "bnav_scale_${item.label}"
                )
                // Active pill behind icon
                val pillWidth by animateDpAsState(
                    if (selected) 56.dp else 0.dp,
                    tween(280, easing = FastOutSlowInEasing),
                    label = "bnav_pill_${item.label}"
                )

                Column(
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPage(item)
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.navIcon,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                        )
                    }
                    Text(
                        item.label,
                        color = iconTint,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─── 6. Discover Screen (5th tab placeholder) ─────────────────────────────────
// Simple "Jelajahi" page — full-width image cards list + featured carousel.
// Static content (no Supabase) — visual showcase of new design.
@Composable
fun DiscoverScreen(
    onNav: (Page) -> Unit,
    onGameCardClick: () -> Unit = {}
) {
    // Static slides for featured carousel
    val heroSlides = remember {
        listOf(
            HeroSlide(
                title = "FIFA 16 Mobile",
                subtitle = "DLavie 26 Mod — Play offline, always updated",
                rating = 4.7f,
                ratingCount = 1250,
                imageRes = R.drawable.dlavie_game_logo,
                tag = "OFFICIAL"
            ),
            HeroSlide(
                title = "Komunitas DLavie",
                subtitle = "Berbagi patch, tips, dan diskusi dengan ribuan pemain",
                rating = 4.5f,
                ratingCount = 820,
                imageRes = null,
                tag = "KOMUNITAS"
            ),
            HeroSlide(
                title = "Update Terbaru",
                subtitle = "Patch v6.7.0 — perbaikan bug & peningkatan performa",
                rating = 4.8f,
                ratingCount = 412,
                imageRes = null,
                tag = "UPDATE"
            )
        )
    }

    // Static list of full-width cards
    val cards = remember {
        listOf(
            Triple("FIFA 16 Mobile — Season 2026", "Olahraga · Mod Football", 4.7f),
            Triple("DLavie Assistant", "Live chat dengan developer", 4.5f),
            Triple("Patch Engine v3", "DevPatchEngine untuk FIFA 16", 4.6f)
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Section header ──
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Jelajahi",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Icon(
                Icons.Rounded.Explore,
                contentDescription = null,
                tint = SubText,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Featured carousel ──
        DLavieHeroCarousel(
            slides = heroSlides,
            onSlideClick = { slide ->
                when (slide.tag) {
                    "OFFICIAL" -> onGameCardClick()
                    "KOMUNITAS" -> onNav(Page.Chat)
                    "UPDATE" -> onNav(Page.Update)
                }
            }
        )

        // ── Section: Trending ──
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Trending Sekarang",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "Lihat semua",
                color = SubText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onNav(Page.Chat) }
            )
        }

        // ── Vertical list of full-width cards ──
        cards.forEach { (title, subtitle, rating) ->
            DLavieFullWidthGameCard(
                title = title,
                subtitle = subtitle,
                rating = rating,
                ratingCount = (800..1500).random(),
                imageRes = R.drawable.dlavie_game_logo,
                ctaLabel = "Lihat",
                onCtaClick = { onGameCardClick() },
                onCardClick = { onGameCardClick() }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
