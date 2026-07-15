package com.drmacze.f16launcher

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════
// PS5 LAUNCHER — Design System (PlayStation 5 UI style)
// ═══════════════════════════════════════════════════════════════════════════
// Pure black bg + PS blue accent + landscape game cards + glow on focus
// ═══════════════════════════════════════════════════════════════════════════

// ── PS5 Design Tokens ──
object PS5Colors {
    val Bg          = Color(0xFF000000)   // Pure black
    val BgCard      = Color(0xFF0A0A0A)   // Card surface (barely visible)
    val BgNav       = Color(0xFF0D0D0D)   // Nav bar
    val Surface     = Color(0xFF1A1A1A)   // Elevated surface
    val GlassBg     = Color(0x99000000)   // 60% black for glassmorphism
    val Border      = Color(0x20FFFFFF)   // 12% white border
    val BorderHi    = Color(0x40FFFFFF)   // 25% white border (selected)
    val TextWhite   = Color(0xFFFFFFFF)
    val TextGray    = Color(0xFF999999)   // PS5 secondary text
    val TextDim     = Color(0xFF666666)
    val Accent      = Color(0xFF0070D1)   // PS Blue
    val AccentBright= Color(0xFF1F80FF)   // Brighter PS Blue for glow
    val AccentDim   = Color(0xFF0050A0)
    val Green       = Color(0xFF00C853)   // PS5 green for Play
    val Amber       = Color(0xFFFFB300)
    val Red         = Color(0xFFFF5252)
}

// ═══════════════════════════════════════════════════════════════════════════
// PS5 GAME CAROUSEL — landscape cards, focused scale + glow
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PS5GameCarousel(
    games: List<GameItem>,
    isInstalled: (String) -> Boolean,
    onGameClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusedIdx by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(games) { idx, game ->
            PS5GameCard(
                game = game,
                isInstalled = isInstalled(game.packageName),
                isFocused = idx == focusedIdx,
                onClick = { onGameClick(game.packageName) }
            )
        }
    }
}

@Composable
private fun PS5GameCard(
    game: GameItem,
    isInstalled: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    // PS5 focus animation: focused card bigger + glow
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "ps5_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.4f,
        animationSpec = tween(400),
        label = "ps5_alpha"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.6f else 0f,
        animationSpec = tween(400),
        label = "ps5_glow"
    )

    Column(
        Modifier.width(280.dp).graphicsLayer {
            scaleX = scale; scaleY = scale; this.alpha = alpha
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── CARD (landscape 16:9, full-bleed artwork, no border) ──
        Box(
            Modifier.width(280.dp).height(158.dp)  // 16:9 ratio
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(game.coverGradient))
                .clickable { onClick() }
        ) {
            // Cover image full bleed
            if (game.coverImageRes != null) {
                Image(
                    painter = painterResource(id = game.coverImageRes),
                    contentDescription = game.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // PS5 glow border on focused card
            if (isFocused) {
                Box(
                    Modifier.fillMaxSize()
                        .border(2.dp, PS5Colors.AccentBright.copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
                )
            }

            // Bottom gradient for text readability (PS5 style — subtle)
            Box(
                Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomStart)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f))))
            )

            // Status badge (top-right, PS5 style)
            val (sc, st) = when (game.serverStatus) {
                ServerStatus.ONLINE -> Pair(PS5Colors.Green, "ONLINE")
                ServerStatus.MAINTENANCE -> Pair(PS5Colors.Amber, "MAINT")
                ServerStatus.OFFLINE -> Pair(PS5Colors.Red, "OFFLINE")
                ServerStatus.BUSY -> Pair(PS5Colors.Amber, "BUSY")
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(sc.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(st, color = PS5Colors.TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Title overlay (bottom-left, PS5 style)
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(
                    game.title,
                    color = PS5Colors.TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,  // PS5 uses Medium, not Black
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    game.subtitle,
                    color = PS5Colors.TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── ACTION BUTTON (below card, only visible when focused) ──
        if (isFocused) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (isInstalled) PS5Colors.Green else PS5Colors.Accent)
                    .clickable { onClick() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                    null,
                    tint = PS5Colors.TextWhite,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    if (isInstalled) "Play" else "Install",
                    color = PS5Colors.TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PS5 TOP BAR — minimal, transparent, time/battery/profile
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PS5TopBar(
    currentTime: String,
    batteryLevel: Int,
    username: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Time
        Text(currentTime, color = PS5Colors.TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.width(12.dp))
        // Battery
        Icon(Icons.Rounded.BatteryFull, "Battery", tint = PS5Colors.TextGray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(3.dp))
        Text("$batteryLevel%", color = PS5Colors.TextGray, fontSize = 12.sp)
        Spacer(Modifier.width(12.dp))
        // Profile circle
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(PS5Colors.Accent)
                .border(1.dp, PS5Colors.BorderHi, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                username.take(1).ifEmpty { "D" },
                color = PS5Colors.TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PS5 FLOATING NAV — dark pill, blue glow on selected, center button
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PS5FloatingNav(
    page: Page,
    onPage: (Page) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pages = Page.values().toList()
    val centerPage = Page.GameHub

    Box(
        modifier = modifier
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp)
    ) {
        // ── Dark pill bar (PS5 style) ──
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = PS5Colors.BgNav,
            shadowElevation = 16.dp,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, PS5Colors.Border)
        ) {
            Row(
                Modifier.height(64.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Home, DLC
                pages.filter { it != centerPage }.take(2).forEach { item ->
                    PS5NavSideButton(item, page == item) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPage(item)
                    }
                }
                Spacer(Modifier.width(64.dp))
                // Right: Chat, Me
                pages.filter { it != centerPage }.drop(2).forEach { item ->
                    PS5NavSideButton(item, page == item) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPage(item)
                    }
                }
            }
        }

        // ── Center button (PS5 style — blue glow) ──
        Box(
            Modifier.align(Alignment.Center)
                .size(64.dp)
                .offset(y = (-22).dp)
                .shadow(elevation = 20.dp, shape = CircleShape, ambientColor = PS5Colors.AccentBright.copy(0.4f), spotColor = PS5Colors.AccentBright.copy(0.6f))
                .clip(CircleShape)
                .background(PS5Colors.Accent)
                .border(2.dp, PS5Colors.TextWhite.copy(0.3f), CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPage(centerPage)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, "GameHub", tint = PS5Colors.TextWhite, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun PS5NavSideButton(item: Page, selected: Boolean, onClick: () -> Unit) {
    val iconTint by animateColorAsState(
        if (selected) PS5Colors.AccentBright else PS5Colors.TextGray,
        tween(300), label = "ps5_nav_tint_${item.label}"
    )
    val labelColor by animateColorAsState(
        if (selected) PS5Colors.TextWhite else PS5Colors.TextDim,
        tween(300), label = "ps5_nav_label_${item.label}"
    )

    Box(
        modifier = Modifier.width(72.dp).height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(item.navIcon, item.label, tint = iconTint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(2.dp))
            Text(item.label, fontSize = 9.sp, color = labelColor, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
        }
    }
}
