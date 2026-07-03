package com.drmacze.f16launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════════════════════
// DLAVIE DESIGN SYSTEM v4.0 PREMIUM — Strict TapTap-level design tokens
//
// PHILOSOPHY (v4.0 — final, strict):
//  - ONE accent color only: AccentGreen (00E676) — used for success / download /
//    play / like / verified. No other vibrant accent anywhere.
//  - Monochrome black/white/gray for everything else.
//  - Inter font bundled in APK — applied globally via MaterialTheme typography.
//  - Strict spacing scale (xs..xxxl) — no other dp values for layout.
//  - Strict radius scale (small/medium/large/xlarge/pill).
//  - Strict border: ONE value only — BorderStroke(1.dp, GlassStroke).
//
// LEGACY COLOR ALIASES:
//  CandyCyan / CandyBlue / NeonGreen / TapTapGreen / TapTapGold / PremiumGold /
//  PremiumViolet / HalftoneMid / HalftoneDim / StarWhite are KEPT as deprecated
//  aliases pointing to the canonical strict tokens above, so existing call
//  sites compile and visually use the strict palette. New code MUST use the
//  canonical names. See P1A in agent-ctx/v4.0.0-premium-main.md.
// ════════════════════════════════════════════════════════════════════════════

// ─── Base Backgrounds (strict — near-black palette) ──────────────────────────
val Carbon       = Color(0xFF0A0A0A)   // base bg
val GlassBase    = Color(0xFF111111)   // card bg
val Surface2     = Color(0xFF1A1A1A)   // elevated surface
val Surface3     = Color(0xFF222222)   // highest surface

// ─── Text (monochrome white-on-black) ─────────────────────────────────────────
val SoftText     = Color(0xFFCCCCCC)   // body text
val SubText      = Color(0xFF666666)   // muted
val GlassStroke  = Color(0x1AFFFFFF)   // STRICT border value (only border color)

// ─── Accent / status (STRICT — only these) ────────────────────────────────────
val AccentGreen  = Color(0xFF00E676)   // ONLY accent — success/download/play/like
val DangerRed    = Color(0xFFFF1744)   // error only
val AmberWarn    = Color(0xFFFFAB00)   // rating / warning only
val HalftoneBright = Color(0xFFFFFFFF) // halftone particle color

// ════════════════════════════════════════════════════════════════════════════
// DEPRECATED LEGACY ALIASES (kept for source-compat — visually identical to
// the strict canonical tokens above). Do NOT use in new code.
// Explicit `: Color` type annotation is REQUIRED so call sites don't trigger
// overload-resolution ambiguity (e.g. Brush.horizontalGradient(listOf(...))).
// ════════════════════════════════════════════════════════════════════════════
val CandyCyan: Color       get() = AccentGreen
val CandyBlue: Color       get() = SoftText
val NeonGreen: Color       get() = AccentGreen
val TapTapGreen: Color     get() = AccentGreen
val TapTapGold: Color      get() = AmberWarn
val PremiumGold: Color     get() = AmberWarn
val PremiumViolet: Color   get() = SubText
val HalftoneMid: Color     get() = SoftText
val HalftoneDim: Color     get() = SubText
val StarWhite: Color       get() = Color.White

// ─── Inter Font Family (bundled in APK — see res/font/) ───────────────────────
// P1B: Inter Regular/Bold/Black bundled. Applied globally via typography below
// and exposed for ad-hoc Text() usage (e.g. logos, custom displays).
val InterFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold,    FontWeight.Bold),
    Font(R.font.inter_black,   FontWeight.Black),
)

// ─── Typography (v4.0 — Inter everywhere) ─────────────────────────────────────
// Sizes/weights keep the v3.0 scale but now reference InterFontFamily. The
// MaterialTheme typography is built from these in DLavieModernApp so EVERY
// Text() in the app inherits Inter automatically (no per-call fontFamily
// needed).
object TTTypography {
    val displayLarge   = TextStyle(fontFamily = InterFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Black)
    val displayMedium  = TextStyle(fontFamily = InterFontFamily, fontSize = 24.sp, fontWeight = FontWeight.Black)
    val headlineLarge  = TextStyle(fontFamily = InterFontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    val headlineMedium = TextStyle(fontFamily = InterFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    val titleLarge     = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val titleMedium    = TextStyle(fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val bodyLarge      = TextStyle(fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val bodyMedium     = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal)
    val bodySmall      = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val caption        = TextStyle(fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val micro          = TextStyle(fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val labelMedium    = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

// ─── STRICT Spacing system (P1C) ──────────────────────────────────────────────
// Only these dp values are allowed for layout. No other hardcoded dp values.
object TTSpacing {
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
}

// ─── STRICT Radius system (P1D) ───────────────────────────────────────────────
// Only these corner radii. TTShapes.card alias kept for source-compat.
object TTShapes {
    val small   = RoundedCornerShape(8.dp)
    val medium  = RoundedCornerShape(12.dp)
    val large   = RoundedCornerShape(16.dp)
    val xlarge  = RoundedCornerShape(20.dp)
    val pill    = RoundedCornerShape(999.dp)

    // Backward-compat aliases (older code references these names).
    val card      get() = xlarge
    val cardLarge get() = large
    val button    get() = medium
    val chip      get() = pill
    val input     get() = medium
}

// ─── STRICT Border (P1E) — only one border value across the entire app ────────
// Usage: TTPortalBorder (or TTBorder) — replaces every `BorderStroke(1.dp, <x>)`
// across the codebase. Older code that constructs BorderStroke inline still
// works, but the canonical entry point is TTBorder.
val TTBorder: BorderStroke = BorderStroke(1.dp, GlassStroke)

// ─── Elevation ────────────────────────────────────────────────────────────────
object TTElevation {
    val card  = 0.dp     // flat design, pakai border instead
    val modal = 8.dp
    val nav   = 12.dp
}
