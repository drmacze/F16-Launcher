package com.drmacze.f16launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════════════════════
// DLAVIE DESIGN SYSTEM 2026
//
// One visual language for the whole launcher:
// • graphite-black surfaces instead of flat pure black everywhere
// • crisp off-white typography with softer secondary hierarchy
// • subtle cool accent only for selection/focus — never decorative overload
// • consistent 12/16/20dp radii and predictable spacing
// • thin borders + restrained elevation instead of heavy glows
//
// Legacy aliases are intentionally preserved so older screens inherit the new
// visual system without requiring a risky app-wide rewrite in one release.
// ════════════════════════════════════════════════════════════════════════════

// ─── Core graphite palette ──────────────────────────────────────────────────
val PureBlack   = Color(0xFF070809)
val Carbon      = Color(0xFF0B0D10)
val GlassBase   = Color(0xFF101318)
val Surface1    = Color(0xFF15191F)
val Surface2    = Color(0xFF1B2028)
val Surface3    = Color(0xFF232A34)
val Surface4    = Color(0xFF2D3541)

// ─── Brand / focus ──────────────────────────────────────────────────────────
// Used sparingly for active navigation, focus rings, links, and progress.
val DLavieAccent     = Color(0xFF9DBBFF)
val DLavieAccentSoft = Color(0xFF6F8FD8)
val DLavieAccentDim  = Color(0xFF40577F)

// ─── Text ────────────────────────────────────────────────────────────────────
val TextWhite   = Color(0xFFF7F8FA)
val SoftText    = Color(0xFFD1D5DC)
val SubText     = Color(0xFF989FAA)
val DimText     = Color(0xFF646C78)

// ─── Borders & dividers ──────────────────────────────────────────────────────
val GlassStroke    = Color(0x24FFFFFF)   // ~14% white
val GlassStrokeHi  = Color(0x42FFFFFF)   // ~26% white
val Hairline       = Color(0x12FFFFFF)   // ~7% white

// ─── Background detail ───────────────────────────────────────────────────────
val HalftoneBright = Color(0xFFF7F8FA)
val HalftoneDim    = Color(0x527B828D)

// ─── Status colors — only for actual state communication ────────────────────
val DangerRed    = Color(0xFFFF5D67)
val AmberWarn    = Color(0xFFFFB84D)
val SuccessGreen = Color(0xFF67D89A)
val SuccessWhite = SuccessGreen

// ════════════════════════════════════════════════════════════════════════════
// LEGACY ALIASES
// Keep old names compiling while routing them into the 2026 palette.
// ════════════════════════════════════════════════════════════════════════════
val AccentGreen: Color    get() = DLavieAccent
val NeonGreen: Color      get() = SuccessGreen
val CandyCyan: Color      get() = DLavieAccent
val CandyBlue: Color      get() = DLavieAccentSoft
val TapTapGreen: Color    get() = DLavieAccent
val TapTapGold: Color     get() = AmberWarn
val PremiumGold: Color    get() = AmberWarn
val PremiumViolet: Color  get() = DLavieAccentSoft
val StarWhite: Color      get() = TextWhite
val HalftoneMid: Color    get() = SubText

// ─── DLavie glass surface aliases ────────────────────────────────────────────
object DLavieGlass {
    val SpaceBlack    = PureBlack
    val SpaceCharcoal = Carbon
    val SpaceSurface  = Surface2

    val GlassSurface     = Color(0xB8101318)
    val GlassSurfaceHigh = Color(0xE615191F)
    val GlassSurfaceLow  = Color(0x8F0B0D10)
    val GlassStroke      = com.drmacze.f16launcher.GlassStroke
    val GlassStrokeHigh  = com.drmacze.f16launcher.GlassStrokeHi

    val AuroraCyan    = DLavieAccent
    val AuroraViolet  = DLavieAccentSoft
    val AuroraMint    = SuccessGreen
    val AuroraCoral   = DangerRed
    val AuroraAmber   = AmberWarn

    val GlowCyan   = DLavieAccent.copy(alpha = 0.20f)
    val GlowViolet = DLavieAccentSoft.copy(alpha = 0.14f)
    val GlowMint   = SuccessGreen.copy(alpha = 0.18f)

    val TextPrimary   = TextWhite
    val TextSecondary = SoftText
    val TextMuted     = SubText
    val TextDim       = DimText

    val BrandMarkCyan   = TextWhite
    val BrandMarkMint   = DLavieAccent
    val BrandMarkStroke = GlassStrokeHi
}

// Convenience aliases
val DLAuroraCyan    get() = DLavieAccent
val DLAuroraViolet  get() = DLavieAccentSoft
val DLAuroraMint    get() = SuccessGreen
val DLAuroraCoral   get() = DangerRed
val DLGlassSurface  get() = DLavieGlass.GlassSurface
val DLGlassStroke   get() = DLavieGlass.GlassStroke
val DLTextPrimary   get() = TextWhite
val DLTextSecondary get() = SoftText
val DLTextMuted     get() = SubText

// ─── Inter font family (bundled in APK) ──────────────────────────────────────
val InterFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_black, FontWeight.Black),
)

// ─── Typography ──────────────────────────────────────────────────────────────
// Slightly calmer weights and line-heights make dense screens easier to scan.
object TTTypography {
    val displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp
    )
    val displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 25.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    )
    val headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    )
    val headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold
    )
    val titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold
    )
    val titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
    val bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal
    )
    val bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal
    )
    val bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    )
    val caption = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
    val micro = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.25.sp
    )
    val labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// ─── Spacing ─────────────────────────────────────────────────────────────────
object TTSpacing {
    val xxs  = 2.dp
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
}

// ─── Shapes ──────────────────────────────────────────────────────────────────
// One radius language across cards, sheets, controls, and navigation.
object TTShapes {
    val small   = RoundedCornerShape(10.dp)
    val medium  = RoundedCornerShape(14.dp)
    val large   = RoundedCornerShape(18.dp)
    val xlarge  = RoundedCornerShape(22.dp)
    val pill    = RoundedCornerShape(999.dp)

    val card      get() = large
    val cardLarge get() = xlarge
    val button    get() = medium
    val chip      get() = pill
    val input     get() = medium
}

// ─── Border ──────────────────────────────────────────────────────────────────
val TTBorder: BorderStroke = BorderStroke(1.dp, GlassStroke)

// ─── Elevation ───────────────────────────────────────────────────────────────
object TTElevation {
    val card  = 1.dp
    val modal = 10.dp
    val nav   = 18.dp
}

// ─── Gradients ───────────────────────────────────────────────────────────────
object DLavieGradients {
    // Quiet top-light gives cards depth without looking glossy.
    val glassCard = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.055f),
            Color.White.copy(alpha = 0.012f),
            Color.Transparent,
        )
    )

    // Primary CTA stays high contrast and brand-neutral.
    val primaryButton = Brush.horizontalGradient(
        colors = listOf(
            TextWhite,
            Color(0xFFE7EBF2),
        )
    )

    // Selection ring carries the single cool accent used across the app.
    val accentRing = Brush.sweepGradient(
        colors = listOf(
            DLavieAccent,
            TextWhite,
            DLavieAccentSoft,
            DLavieAccent,
        )
    )

    val auroraTopToBottom = Brush.verticalGradient(
        colors = listOf(
            DLavieAccent.copy(alpha = 0.07f),
            Color.White.copy(alpha = 0.018f),
            Color.Transparent,
        )
    )

    val appBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D1015),
            Carbon,
            PureBlack,
        )
    )
}

// ─── Glass spacing aliases ───────────────────────────────────────────────────
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

// ─── Glass radius aliases ────────────────────────────────────────────────────
object DLRadius {
    val xs     = 8.dp
    val sm     = 12.dp
    val md     = 16.dp
    val lg     = 18.dp
    val xl     = 22.dp
    val xxl    = 26.dp
    val pillDp = 999.dp

    val card   get() = RoundedCornerShape(xl)
    val cardLg get() = RoundedCornerShape(xxl)
    val button get() = RoundedCornerShape(md)
    val pill   get() = RoundedCornerShape(pillDp)
    val input  get() = RoundedCornerShape(md)
    val chip   get() = RoundedCornerShape(sm)
}

// ─── Glass border aliases ────────────────────────────────────────────────────
val DLBorderStroke: BorderStroke = BorderStroke(1.dp, DLavieGlass.GlassStroke)
val DLBorderActive: BorderStroke = BorderStroke(1.dp, DLavieGlass.GlassStrokeHigh)
