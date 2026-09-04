package com.drmacze.f16launcher

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Historical function names are kept for binary/source compatibility with the
// current launcher shell. Their visual language is now DLavie 2026, not PS5.
object PS5Colors {
    val Bg get() = com.drmacze.f16launcher.PureBlack
    val BgCard get() = com.drmacze.f16launcher.GlassBase
    val BgNav get() = Color(0xF2101318)
    val Surface get() = com.drmacze.f16launcher.Surface2
    val GlassBg get() = Color(0xD90B0D10)
    val Border get() = com.drmacze.f16launcher.GlassStroke
    val BorderHi get() = com.drmacze.f16launcher.GlassStrokeHi
    val TextWhite get() = com.drmacze.f16launcher.TextWhite
    val TextGray get() = com.drmacze.f16launcher.SubText
    val TextDim get() = com.drmacze.f16launcher.DimText
    val Accent get() = com.drmacze.f16launcher.DLavieAccent
    val AccentBright get() = com.drmacze.f16launcher.TextWhite
    val AccentDim get() = com.drmacze.f16launcher.DLavieAccentDim
    val Green get() = com.drmacze.f16launcher.SuccessGreen
    val Amber get() = com.drmacze.f16launcher.AmberWarn
    val Red get() = com.drmacze.f16launcher.DangerRed
}

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
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(games) { index, game ->
            DLavieModernGameCard(
                game = game,
                isInstalled = isInstalled(game.packageName),
                focused = index == focusedIdx,
                onClick = { onGameClick(game.packageName) }
            )
        }
    }
}

@Composable
private fun DLavieModernGameCard(
    game: GameItem,
    isInstalled: Boolean,
    focused: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (focused) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "game_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0.72f,
        animationSpec = tween(220),
        label = "game_alpha"
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) GlassStrokeHi else GlassStroke,
        animationSpec = tween(220),
        label = "game_border"
    )

    Column(
        Modifier
            .width(292.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(164.dp),
            shape = RoundedCornerShape(20.dp),
            color = GlassBase,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = if (focused) 8.dp else 0.dp,
            onClick = onClick
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(game.coverGradient))
                )

                game.coverImageRes?.let { imageRes ->
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.04f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                val (statusColor, statusText) = when (game.serverStatus) {
                    ServerStatus.ONLINE -> SuccessGreen to "ONLINE"
                    ServerStatus.MAINTENANCE -> AmberWarn to "MAINTENANCE"
                    ServerStatus.OFFLINE -> DangerRed to "OFFLINE"
                    ServerStatus.BUSY -> AmberWarn to "BUSY"
                }

                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.66f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            statusText,
                            color = TextWhite,
                            fontFamily = InterFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        game.title,
                        color = TextWhite,
                        fontFamily = InterFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        game.subtitle,
                        color = SoftText,
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isInstalled) TextWhite else Surface2,
            border = if (isInstalled) null else BorderStroke(1.dp, GlassStroke),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isInstalled) Icons.Rounded.PlayArrow else Icons.Rounded.Download,
                    contentDescription = null,
                    tint = if (isInstalled) Carbon else TextWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (isInstalled) "Play" else "Install",
                    color = if (isInstalled) Carbon else TextWhite,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PS5TopBar(
    currentTime: String,
    batteryLevel: Int,
    username: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            currentTime,
            color = SoftText,
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.Rounded.BatteryFull,
            contentDescription = "Battery",
            tint = SubText,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "$batteryLevel%",
            color = SubText,
            fontFamily = InterFontFamily,
            fontSize = 11.sp
        )
        Spacer(Modifier.width(12.dp))
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = Surface2,
            border = BorderStroke(1.dp, GlassStrokeHi)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    username.take(1).ifEmpty { "D" }.uppercase(),
                    color = TextWhite,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

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
            .widthIn(max = 560.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF5101318),
            border = BorderStroke(1.dp, GlassStroke),
            shadowElevation = 18.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.filter { it != centerPage }.take(2).forEach { item ->
                    DLavieModernNavButton(item, page == item) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPage(item)
                    }
                }

                Spacer(Modifier.width(62.dp))

                pages.filter { it != centerPage }.drop(2).forEach { item ->
                    DLavieModernNavButton(item, page == item) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPage(item)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-10).dp)
                .size(58.dp)
                .shadow(12.dp, RoundedCornerShape(19.dp)),
            shape = RoundedCornerShape(19.dp),
            color = TextWhite,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPage(centerPage)
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "GameHub",
                    tint = Carbon,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun DLavieModernNavButton(
    item: Page,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconTint by animateColorAsState(
        if (selected) TextWhite else SubText,
        tween(180),
        label = "nav_icon_${item.label}"
    )
    val background by animateColorAsState(
        if (selected) Surface2 else Color.Transparent,
        tween(180),
        label = "nav_bg_${item.label}"
    )
    val labelColor by animateColorAsState(
        if (selected) TextWhite else DimText,
        tween(180),
        label = "nav_label_${item.label}"
    )

    Surface(
        modifier = Modifier.width(66.dp),
        shape = RoundedCornerShape(16.dp),
        color = background,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.height(52.dp).padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                item.navIcon,
                contentDescription = item.label,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                item.label,
                fontFamily = InterFontFamily,
                fontSize = 9.sp,
                color = labelColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
