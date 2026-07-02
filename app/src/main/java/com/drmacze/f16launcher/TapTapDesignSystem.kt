package com.drmacze.f16launcher

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════════════════════
// DLAVIE DESIGN SYSTEM v3.0 — Monochrome White-on-Black + Halftone Particles
//
// Inspired by: DLavie logo (deep near-black bg, white text, halftone dots,
// small white star accent).
//
// Design philosophy:
//  - Monochrome white-on-black (pekat, premium, futuristic)
//  - Halftone dot pattern sebagai signature texture
//  - Vibrant accents DIHAPUS — ganti dengan monochrome gray-white
//  - Status colors (NeonGreen/DangerRed/AmberWarn) TETAP dipertahankan untuk
//    functional indicators (download/success, error, rating) — minimal use.
//
// SINGLE SOURCE OF TRUTH untuk design tokens:
//   - Colors  (Carbon, GlassBase, Surface2, Surface3, accents, halftone)
//   - Typography (TTTypography)
//   - Spacing  (TTSpacing)
//   - Shapes   (TTShapes)
//   - Elevation(TTElevation)
//
// Premium* palette tetap di ModernUI.kt (PremiumBg, PremiumSurface, PremiumGold,
// PremiumViolet) sebagai extend — sekarang juga monochrome (gray).
// ════════════════════════════════════════════════════════════════════════════

// ─── Base Backgrounds (near-black, pekat — match logo bg) ─────────────────────
val Carbon      = Color(0xFF0A0A0A)   // base — near pure black (match logo bg)
val GlassBase   = Color(0xFF111111)   // card background — slightly lighter
val Surface2    = Color(0xFF1A1A1A)   // elevated surface
val Surface3    = Color(0xFF222222)   // highest surface

// ─── Text (white-on-black, monochrome) ────────────────────────────────────────
val SoftText    = Color(0xFFCCCCCC)   // body text — light gray-white
val SubText     = Color(0xFF666666)   // muted — medium gray
val GlassStroke = Color(0x30FFFFFF)   // subtle white border (halftone-like)

// ─── Accents (monochrome — ganti vibrant cyan/violet) ─────────────────────────
// CandyCyan/CandyBlue sebelumnya vibrant — sekarang light gray-white & gray,
// supaya komponen lama yang reference nama ini otomatis jadi monochrome.
val CandyCyan   = Color(0xFFE0E0E0)   // monochrome accent — light gray-white
val CandyBlue   = Color(0xFFAAAAAA)   // monochrome secondary — gray

// Status colors (TETAP vibrant — hanya untuk functional indicators, minimal use)
val NeonGreen   = Color(0xFF00E676)   // emerald — success/download status
val DangerRed   = Color(0xFFFF1744)   // error
val AmberWarn   = Color(0xFFFFAB00)   // warning/rating stars

// TapTap-style accent (tetap untuk ratings/buttons, tetap vibrant)
val TapTapGreen = Color(0xFF00C853)   // TapTap green untuk ratings/buttons
val TapTapGold  = Color(0xFFFFD700)   // gold untuk premium/verified badge only

// ─── NEW: Halftone particle colors (signature DLavie texture) ─────────────────
val HalftoneBright = Color(0xFFFFFFFF)  // bright dots
val HalftoneMid    = Color(0xFF888888)  // mid dots
val HalftoneDim    = Color(0xFF333333)  // dim dots
val StarWhite      = Color(0xFFFFFFF0)  // star glow (slightly warm white)

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
