package com.drmacze.f16launcher

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════════════════════
// TAPTAP-LEVEL CUSTOM DESIGN SYSTEM
// Dark gaming theme — pekat, premium, dengan accent vibrant
//
// SINGLE SOURCE OF TRUTH untuk design tokens:
//   - Colors  (Carbon, GlassBase, Surface2, Surface3, accents)
//   - Typography (TTTypography)
//   - Spacing  (TTSpacing)
//   - Shapes   (TTShapes)
//   - Elevation(TTElevation)
//
// Premium* palette tetap di ModernUI.kt (PremiumBg, PremiumSurface, PremiumGold,
// PremiumViolet) sebagai extend — tidak duplicate.
// ════════════════════════════════════════════════════════════════════════════

// ─── Colors ───────────────────────────────────────────────────────────────────
// Base backgrounds (sangat pekat, near-black dengan tint biru sangat subtil)
val Carbon      = Color(0xFF0A0A0F)   // base — near black with slight blue tint
val GlassBase   = Color(0xFF121218)   // card background
val Surface2    = Color(0xFF1A1A24)   // elevated surface
val Surface3    = Color(0xFF22222E)   // highest surface

// Accents (vibrant, gaming-style)
val CandyCyan   = Color(0xFF00E5FF)   // electric cyan — primary accent
val CandyBlue   = Color(0xFF7C4DFF)   // deep violet — secondary
val NeonGreen   = Color(0xFF00E676)   // emerald — success/download
val SoftText    = Color(0xFFB0B0C0)   // body text
val SubText     = Color(0xFF606070)   // muted
val GlassStroke = Color(0x30FFFFFF)   // subtle white border
val DangerRed   = Color(0xFFFF1744)   // error
val AmberWarn   = Color(0xFFFFAB00)   // warning/rating

// TapTap-style accent
val TapTapGreen = Color(0xFF00C853)   // TapTap green untuk ratings/buttons
val TapTapGold  = Color(0xFFFFD700)   // gold untuk premium/verified

// ─── Typography ───────────────────────────────────────────────────────────────
// Custom font scale — gaming apps biasanya pakai tight tracking, bold weights
object TTTypography {
    val displayLarge  = 32.sp to FontWeight.Black
    val displayMedium = 24.sp to FontWeight.Black
    val headlineLarge = 22.sp to FontWeight.Bold
    val headlineMedium = 18.sp to FontWeight.Bold
    val titleLarge    = 16.sp to FontWeight.Bold
    val titleMedium   = 14.sp to FontWeight.SemiBold
    val bodyLarge     = 14.sp to FontWeight.Normal
    val bodyMedium    = 13.sp to FontWeight.Normal
    val bodySmall     = 12.sp to FontWeight.Normal
    val caption       = 11.sp to FontWeight.Medium
    val micro         = 10.sp to FontWeight.Bold
}

// ─── Spacing ──────────────────────────────────────────────────────────────────
object TTSpacing {
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
}

// ─── Shapes ───────────────────────────────────────────────────────────────────
object TTShapes {
    val card      = RoundedCornerShape(20.dp)
    val cardLarge = RoundedCornerShape(24.dp)
    val button    = RoundedCornerShape(12.dp)
    val chip      = RoundedCornerShape(999.dp)
    val input     = RoundedCornerShape(10.dp)
    val small     = RoundedCornerShape(8.dp)
}

// ─── Elevation ────────────────────────────────────────────────────────────────
object TTElevation {
    val card  = 0.dp     // flat design, pakai border instead
    val modal = 8.dp
    val nav   = 12.dp
}
