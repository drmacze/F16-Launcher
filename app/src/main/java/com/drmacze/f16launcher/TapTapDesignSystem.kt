package com.drmacze.f16launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════════════════════
// DLAVIE DESIGN SYSTEM v6.0 — PURE MONOCHROME + HALFTONE
//
// PHILOSOPHY:
//   Pure black & white. No colorful accents. No cyan, no violet, no green.
//   - Background: #000000 (pure black) with halftone dot pattern overlay
//   - Surfaces: #0A0A0A → #1A1A1A → #222222 (subtle gray elevation)
//   - Text: #FFFFFF (pure white) for primary, #999999 for secondary
//   - Borders: white at 10-20% alpha (subtle, elegant)
//   - Icons: solid white or thick white outline
//   - Status colors (error/warning) kept MINIMAL — only for actual alerts
//
// The halftone dot pattern is the SIGNATURE element — small white dots
// varying in size/opacity across the background, creating depth and a
// retro-digital/holographic feel.
//
// All legacy color aliases (CandyCyan, NeonGreen, etc.) are remapped to
// monochrome equivalents so existing code compiles without changes.
// ════════════════════════════════════════════════════════════════════════════

// ─── Core Monochrome Palette ────────────────────────────────────────────────
val PureBlack   = Color(0xFF000000)   // deepest background
val Carbon      = Color(0xFF050505)   // base background (near-black)
val GlassBase   = Color(0xFF0A0A0A)   // card background
val Surface1    = Color(0xFF111111)   // elevated surface
val Surface2    = Color(0xFF1A1A1A)   // higher elevation
val Surface3    = Color(0xFF222222)   // highest surface
val Surface4    = Color(0xFF2D2D2D)   // active/pressed state

// ─── Text ────────────────────────────────────────────────────────────────────
val TextWhite   = Color(0xFFFFFFFF)   // primary text (pure white)
val SoftText    = Color(0xFFCCCCCC)   // body text (light gray)
val SubText     = Color(0xFF888888)   // secondary text (medium gray)
val DimText     = Color(0xFF555555)   // disabled/hint text (dark gray)

// ─── Borders & Strokes ───────────────────────────────────────────────────────
val GlassStroke    = Color(0x1AFFFFFF)   // 10% white — default border
val GlassStrokeHi  = Color(0x33FFFFFF)   // 20% white — active/focused border
val Hairline       = Color(0x0DFFFFFF)   // 5% white — subtle dividers

// ─── Halftone ────────────────────────────────────────────────────────────────
val HalftoneBright = Color(0xFFFFFFFF)   // halftone dot color (pure white)
val HalftoneDim    = Color(0x66666666)   // dimmer halftone dots

// ─── Status Colors (MINIMAL — only for actual alerts) ───────────────────────
val DangerRed    = Color(0xFFFF4444)   // error/danger only (slightly muted)
val AmberWarn    = Color(0xFFFFAA00)   // warning/rating only
val SuccessWhite = Color(0xFFFFFFFF)   // success = white checkmark (no green)

// ════════════════════════════════════════════════════════════════════════════
// LEGACY ALIASES — all remapped to monochrome. Existing code compiles
// without changes but visually uses the new pure B/W palette.
// ════════════════════════════════════════════════════════════════════════════
val AccentGreen: Color    get() = TextWhite        // was: #00E676 → now: white
val NeonGreen: Color      get() = TextWhite        // was: #00E676 → now: white
val CandyCyan: Color      get() = TextWhite        // was: cyan → now: white
val CandyBlue: Color      get() = SoftText         // was: blue → now: gray
val TapTapGreen: Color    get() = TextWhite        // was: green → now: white
val TapTapGold: Color     get() = AmberWarn        // kept: amber for ratings
val PremiumGold: Color    get() = AmberWarn        // kept: amber for ratings
val PremiumViolet: Color  get() = SubText          // was: violet → now: gray
val StarWhite: Color      get() = TextWhite
val HalftoneMid: Color    get() = SoftText

// ─── DLavie Glass (v5.0 legacy — remapped to monochrome) ────────────────────
object DLavieGlass {
    val SpaceBlack    = PureBlack
    val SpaceCharcoal = Carbon
    val SpaceSurface  = Surface2

    val GlassSurface     = Color(0x14FFFFFF)   // 8% white
    val GlassSurfaceHigh = Color(0x1FFFFFFF)   // 12% white
    val GlassSurfaceLow  = Color(0x0AFFFFFF)   // 4% white
    val GlassStroke      = Color(0x1AFFFFFF)   // 10% white
    val GlassStrokeHigh  = Color(0x33FFFFFF)   // 20% white

    // All aurora colors → monochrome
    val AuroraCyan    = TextWhite
    val AuroraViolet  = SubText
    val AuroraMint    = TextWhite
    val AuroraCoral   = DangerRed
    val AuroraAmber   = AmberWarn

    val GlowCyan   = Color(0x33FFFFFF)   // 20% white glow
    val GlowViolet = Color(0x22FFFFFF)   // 13% white glow
    val GlowMint   = Color(0x33FFFFFF)   // 20% white glow

    val TextPrimary   = TextWhite
    val TextSecondary = SoftText
    val TextMuted     = SubText
    val TextDim       = DimText

    val BrandMarkCyan   = TextWhite
    val BrandMarkMint   = SoftText
    val BrandMarkStroke = SubText
}

// Convenience aliases
val DLAuroraCyan    get() = TextWhite
val DLAuroraViolet  get() = SubText
val DLAuroraMint    get() = TextWhite
val DLAuroraCoral   get() = DangerRed
val DLGlassSurface     get() = DLavieGlass.GlassSurface
val DLGlassStroke      get() = DLavieGlass.GlassStroke
val DLTextPrimary   get() = TextWhite
val DLTextSecondary get() = SoftText
val DLTextMuted     get() = SubText

// ─── Inter Font Family (bundled in APK) ──────────────────────────────────────
val InterFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold,    FontWeight.Bold),
    Font(R.font.inter_black,   FontWeight.Black),
)

// ─── Typography ──────────────────────────────────────────────────────────────
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

// ─── Spacing ─────────────────────────────────────────────────────────────────
object TTSpacing {
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
}

// ─── Shapes (subtle rounded corners — 8-16dp) ────────────────────────────────
object TTShapes {
    val small   = RoundedCornerShape(8.dp)
    val medium  = RoundedCornerShape(12.dp)
    val large   = RoundedCornerShape(16.dp)
    val xlarge  = RoundedCornerShape(20.dp)
    val pill    = RoundedCornerShape(999.dp)

    val card      get() = large
    val cardLarge get() = large
    val button    get() = medium
    val chip      get() = pill
    val input     get() = medium
}

// ─── Border ──────────────────────────────────────────────────────────────────
val TTBorder: BorderStroke = BorderStroke(1.dp, GlassStroke)

// ─── Elevation ───────────────────────────────────────────────────────────────
object TTElevation {
    val card  = 0.dp
    val modal = 8.dp
    val nav   = 12.dp
}

// ════════════════════════════════════════════════════════════════════════════
// v6.0 MONOCHROME HALFTONE — Gradients & Backgrounds
// ════════════════════════════════════════════════════════════════════════════

object DLavieGradients {
    // Card gradient — subtle white top to transparent bottom
    val glassCard = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.02f),
        )
    )

    // Button primary — pure white (for primary CTAs)
    val primaryButton = Brush.horizontalGradient(
        colors = listOf(
            Color.White,
            Color.White.copy(alpha = 0.90f),
        )
    )

    // Accent ring — pure white (no color)
    val accentRing = Brush.sweepGradient(
        colors = listOf(
            Color.White,
            Color.White.copy(alpha = 0.5f),
            Color.White,
        )
    )

    // Legacy: auroraTopToBottom — now just subtle white glow
    val auroraTopToBottom = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.04f),
            Color.Transparent,
        )
    )
}

// ─── Glass Spacing ───────────────────────────────────────────────────────────
object DLSpacing {
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp
}

// ─── Glass Radius ────────────────────────────────────────────────────────────
object DLRadius {
    val xs    = 8.dp
    val sm    = 12.dp
    val md    = 16.dp
    val lg    = 20.dp
    val xl    = 24.dp
    val xxl   = 28.dp
    val pillDp = 999.dp

    val card     get() = RoundedCornerShape(xl)
    val cardLg   get() = RoundedCornerShape(xxl)
    val button   get() = RoundedCornerShape(md)
    val pill     get() = RoundedCornerShape(pillDp)
    val input    get() = RoundedCornerShape(md)
    val chip     get() = RoundedCornerShape(sm)
}

// ─── Glass Border ────────────────────────────────────────────────────────────
val DLBorderStroke: BorderStroke = BorderStroke(1.dp, DLavieGlass.GlassStroke)
val DLBorderActive: BorderStroke = BorderStroke(1.5.dp, DLavieGlass.GlassStrokeHigh)
