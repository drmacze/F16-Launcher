package com.drmacze.f16launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════════
// MODERN DESIGN SYSTEM v2.0
// Premium, aesthetic, clean — with depth, motion, and attention to detail
// ═══════════════════════════════════════════════════════════════════════════════
// NOTE: Base color tokens (Carbon, GlassBase, Surface2, Surface3, CandyCyan,
// CandyBlue, NeonGreen, SoftText, SubText, GlassStroke, DangerRed, AmberWarn,
// TapTapGreen, TapTapGold) sekarang ada di TapTapDesignSystem.kt sebagai
// single source of truth. File ini hanya simpan extend "Premium*" palette
// (untuk backward compat dengan komponen lama yang pakai premium tint).

// ─── Premium Palette (v4.0 — strict alias to canonical tokens) ────────────────
// Deep near-black base — alias ke Carbon/GlassBase untuk single source of truth.
// PremiumGold & PremiumViolet are declared in TapTapDesignSystem.kt (strict
// aliases to AmberWarn & SubText) — kept there for single source of truth.
val PremiumBg         = Color(0xFF050505)   // deepest base — near pure black
val PremiumSurface    get() = Carbon        // alias — same as GlassBase palette root
val PremiumSurfaceHi  get() = GlassBase     // alias — same as GlassBase

// ─── Typography Scale ──────────────────────────────────────────────────────────
// System font with proper weight hierarchy

object Type {
    val hero       = 28.sp to FontWeight.Black
    val title      = 20.sp to FontWeight.Bold
    val titleSmall = 17.sp to FontWeight.Bold
    val body       = 14.sp to FontWeight.Normal
    val bodyStrong = 14.sp to FontWeight.SemiBold
    val caption    = 12.sp to FontWeight.Normal
    val captionS   = 11.sp to FontWeight.Medium
    val micro      = 10.sp to FontWeight.Bold
}

// ─── Premium GlassCard v2 ──────────────────────────────────────────────────────
// Animated gradient border, subtle inner glow, press feedback

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    gradientBorder: Boolean = false,
    borderColor: Color = GlassStroke,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && onClick != null) 0.985f else 1f,
        tween(120), label = "card_press"
    )

    val baseModifier = if (onClick != null) {
        modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    } else modifier

    Card(
        modifier = baseModifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xCC0B0B0B)   // v3.0 monochrome — near-black glass
        ),
        border = if (gradientBorder) null else BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Gradient border overlay (when enabled)
            if (gradientBorder) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    CandyCyan.copy(0.0f),
                                    CandyBlue.copy(0.45f),
                                    PremiumViolet.copy(0.30f),
                                    CandyCyan.copy(0.0f)
                                )
                            )
                        )
                )
                // Inner mask to create border-only effect
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(1.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Carbon.copy(0.95f))
                )
            }
            Column(
                Modifier.padding(18.dp).gradientOverlay(),
                content = content
            )
        }
    }
}

/** Subtle radial overlay for depth — very faint, top-left highlight */
@Composable
private fun Modifier.gradientOverlay(): Modifier {
    return this.then(
        Modifier.drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(0.04f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.1f),
                    radius = size.maxDimension * 0.8f
                )
            )
        }
    )
}

// ─── Glow / Light Effects ──────────────────────────────────────────────────────

/** Soft glow behind an element — use sparingly for hero CTAs */
@Composable
fun Modifier.softGlow(color: Color, radius: Float = 30f, alpha: Float = 0.4f): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulse by infiniteTransition.animateFloat(
        initialValue = alpha * 0.7f,
        targetValue = alpha,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse"
    )
    return this.then(
        Modifier.drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(pulse), Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension
                )
            )
        }.blur(radius.dp)
    )
}

// ─── Section Header with dropdown chevron ──────────────────────────────────────

@Composable
fun ModernSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    expanded: Boolean = false,
    onToggle: (() -> Unit)? = null,
    accentColor: Color = CandyCyan
) {
    val chevronRotation by animateFloatAsState(
        if (expanded) 90f else 0f,
        tween(280, easing = FastOutSlowInEasing),
        label = "chevron"
    )

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .let { mod -> if (onToggle != null) mod.clickable { onToggle() } else mod }
    ) {
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Accent bar
            Box(
                Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(listOf(accentColor, accentColor.copy(0.3f)))
                    )
            )
            if (icon != null) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = SubText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            if (onToggle != null) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = SubText,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(1f, 1f)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

// ─── Skeleton Loader (shimmer) ─────────────────────────────────────────────────

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer_x"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PremiumSurfaceHi.copy(0.6f),
                        Color(0xFF1A1A1A).copy(0.9f),   // v3.0 monochrome shimmer highlight
                        PremiumSurfaceHi.copy(0.6f)
                    ),
                    start = Offset(shimmer * 200f, 0f),
                    end = Offset(shimmer * 200f + 400f, 200f)
                )
            )
    )
}

@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    PremiumGlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(Modifier.size(40.dp), cornerRadius = 12)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
                SkeletonBox(Modifier.fillMaxWidth(0.4f).height(11.dp))
            }
        }
    }
}

// ─── Modern Action Button with press animation ─────────────────────────────────

@Composable
fun ModernActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = CandyCyan,
    contentColor: Color = Carbon,
    height: Int = 52,
    glow: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed && enabled) 0.96f else 1f,
        tween(120), label = "btn_press"
    )

    Surface(
        modifier = modifier
            .height(height.dp)
            .scale(scale)
            .let { if (glow && enabled) it.softGlow(containerColor, radius = 24f, alpha = 0.5f) else it },
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) containerColor else containerColor.copy(0.3f),
        contentColor = if (enabled) contentColor else contentColor.copy(0.5f),
        enabled = enabled,
        interactionSource = interactionSource,
        onClick = onClick
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// ─── Animated Counter ──────────────────────────────────────────────────────────

@Composable
fun AnimatedCounterText(
    target: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    color: Color = Color.White,
    fontSize: Int = 14,
    fontWeight: FontWeight = FontWeight.Black
) {
    val animatable = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(target) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(900, easing = FastOutSlowInEasing)
        )
    }
    Text(
        "${animatable.value.toInt()}$suffix",
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

// ─── Animated Expandable Section (smooth height + fade) ────────────────────────

@Composable
fun ExpandableSection(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = CandyCyan,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val chevronRotation by animateFloatAsState(
        if (expanded) 180f else 0f,
        tween(300, easing = FastOutSlowInEasing),
        label = "expand_chevron"
    )

    PremiumGlassCard(
        borderColor = if (expanded) accentColor.copy(0.5f) else GlassStroke,
        onClick = onToggle
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(
                        subtitle, color = SubText, fontSize = 11.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = if (expanded) accentColor else SubText,
                modifier = Modifier
                    .size(22.dp)
                    .scale(scaleX = 1f, scaleY = 1f)
                    .drawBehind { }
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(160))
        ) {
            Column(
                Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

// ─── Modern Status Chip (used in Home screen status bar) ──────────────────────

@Composable
fun ModernStatusChip(
    label: String,
    value: String,
    ok: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f, tween(120), label = "chip_press"
    )

    val borderColor by animateColorAsState(
        if (ok) NeonGreen.copy(0.45f) else DangerRed.copy(0.40f),
        tween(500, easing = FastOutSlowInEasing), label = "chip_border"
    )
    val glowColor = if (ok) NeonGreen else DangerRed
    val iconTint by animateColorAsState(
        if (ok) NeonGreen else DangerRed,
        tween(500), label = "chip_icon"
    )

    Box(modifier = modifier.scale(scale)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(TTShapes.card)
                .clickable(interactionSource = interactionSource, indication = null) { onClick() },
            shape = TTShapes.card,
            color = Color(0xCC0B0B0B),   // v3.0 monochrome glass
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                Modifier.padding(TTSpacing.md),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    Modifier.size(20.dp).clip(CircleShape)
                        .background(glowColor.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                        null, tint = iconTint, modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.height(TTSpacing.sm))
                Text(
                    label, color = SubText, fontSize = 9.sp,
                    fontWeight = FontWeight.Black, maxLines = 1,
                    letterSpacing = 0.5.sp
                )
                Text(
                    value, color = iconTint, fontSize = 12.sp,
                    fontWeight = FontWeight.Black, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ModernPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        color = color.copy(0.12f),
        border = BorderStroke(1.dp, color.copy(0.35f)),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
            }
            Text(
                text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}
