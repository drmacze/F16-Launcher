from pathlib import Path

ROOT = Path("app/src/main/java/com/drmacze/f16launcher")
MARKER = "// DLAVIE_SCREEN_REFRESH_V2"


def load(name: str) -> tuple[Path, str]:
    path = ROOT / name
    if not path.exists():
        raise SystemExit(f"Missing UI source: {name}")
    return path, path.read_text(encoding="utf-8")


def require_replace(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f"UI refresh anchor not found: {label}")
    return text.replace(old, new, 1)


def mark(text: str) -> str:
    if MARKER in text:
        return text
    package_line = "package com.drmacze.f16launcher\n"
    if package_line not in text:
        raise SystemExit("Kotlin package line missing")
    return text.replace(package_line, package_line + "\n" + MARKER + "\n", 1)


# ---------------------------------------------------------------------------
# Shared reusable components: Home, lists, community cards and game cards.
# ---------------------------------------------------------------------------
path, text = load("TapTapComponents.kt")
text = mark(text)
text = require_replace(
    text,
    "targetValue = if (isPressed) 0.97f else 1f,\n        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),",
    "targetValue = if (isPressed) 0.985f else 1f,\n        animationSpec = tween(120),",
    "calm card press animation",
)
text = require_replace(
    text,
    "modifier = Modifier.fillMaxWidth().height(180.dp),",
    "modifier = Modifier.fillMaxWidth().height(196.dp),",
    "banner height",
)
text = require_replace(
    text,
    "if (isSelected) AccentGreen else Color.White.copy(0.3f)",
    "if (isSelected) TextWhite else TextWhite.copy(alpha = 0.18f)",
    "neutral pager indicator",
)
text = require_replace(
    text,
    "banner.title,\n                        color = Color.White,\n                        fontSize = 24.sp,\n                        fontWeight = FontWeight.Black",
    "banner.title,\n                        color = TextWhite,\n                        fontFamily = InterFontFamily,\n                        fontSize = 22.sp,\n                        lineHeight = 27.sp,\n                        fontWeight = FontWeight.Bold",
    "banner title hierarchy",
)
text = require_replace(
    text,
    "banner.subtitle,\n                        color = Color.White.copy(0.7f),\n                        fontSize = 12.sp",
    "banner.subtitle,\n                        color = SoftText,\n                        fontFamily = InterFontFamily,\n                        fontSize = 12.sp,\n                        lineHeight = 17.sp",
    "banner subtitle hierarchy",
)
text = require_replace(
    text,
    "Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))\n                    .background(Brush.linearGradient(coverGradient))\n                    .then(sharedGameCoverModifier(sharedContentKey))",
    "Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))\n                    .background(Brush.linearGradient(coverGradient))\n                    .then(sharedGameCoverModifier(sharedContentKey))",
    "shared game cover size",
)
text = require_replace(
    text,
    "Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))\n                    .background(Brush.linearGradient(coverGradient))",
    "Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))\n                    .background(Brush.linearGradient(coverGradient))",
    "game cover size",
)
text = require_replace(
    text,
    "Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)",
    "Text(title, color = TextWhite, fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)",
    "game card title",
)
text = require_replace(
    text,
    "Text(subtitle, color = SoftText, fontSize = 11.sp)",
    "Text(subtitle, color = SoftText, fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp)",
    "game card subtitle",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Premium/shared layout helpers: remove the remaining cyan/violet template feel.
# ---------------------------------------------------------------------------
path, text = load("ModernUI.kt")
text = mark(text)
text = require_replace(
    text,
    "shape = RoundedCornerShape(24.dp),",
    "shape = RoundedCornerShape(22.dp),",
    "premium card radius",
)
text = require_replace(
    text,
    "containerColor = Color(0xCC0B0B0B)",
    "containerColor = Color(0xE60F1217)",
    "premium card surface",
)
text = require_replace(
    text,
    "Brush.sweepGradient(\n                                listOf(\n                                    CandyCyan.copy(0.0f),\n                                    CandyBlue.copy(0.45f),\n                                    PremiumViolet.copy(0.30f),\n                                    CandyCyan.copy(0.0f)\n                                )\n                            )",
    "Brush.sweepGradient(\n                                listOf(\n                                    TextWhite.copy(alpha = 0.02f),\n                                    TextWhite.copy(alpha = 0.22f),\n                                    GlassStrokeHi,\n                                    TextWhite.copy(alpha = 0.02f)\n                                )\n                            )",
    "neutral premium border",
)
text = require_replace(
    text,
    ".clip(RoundedCornerShape(24.dp))",
    ".clip(RoundedCornerShape(22.dp))",
    "premium border outer radius",
)
text = require_replace(
    text,
    ".clip(RoundedCornerShape(23.dp))",
    ".clip(RoundedCornerShape(21.dp))",
    "premium border inner radius",
)
text = require_replace(
    text,
    "accentColor: Color = CandyCyan",
    "accentColor: Color = TextWhite",
    "neutral section accent",
)
text = require_replace(
    text,
    "containerColor: Color = CandyCyan,\n    contentColor: Color = Carbon,",
    "containerColor: Color = TextWhite,\n    contentColor: Color = Carbon,",
    "neutral action button",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# DLC: adopt shared DLavie palette and cleaner page spacing.
# ---------------------------------------------------------------------------
path, text = load("DlcScreen.kt")
text = mark(text)
old_palette = '''private val DlcBlack     = Color(0xFF000000)
private val DlcCardBg    = Color(0xFF0A0A0A)
private val DlcCardBgAlt = Color(0xFF101010)
private val DlcBorder    = Color(0x1AFFFFFF)
private val DlcBorderHi  = Color(0x33FFFFFF)
private val DlcText      = Color(0xFFFFFFFF)
private val DlcSubText   = Color(0xFFAAAAAA)
private val DlcMuted     = Color(0xFF666666)
private val DlcGreen     = Color(0xFFFFFFFF)
private val DlcRed       = Color(0xFFFF5555)
private val DlcYellow    = Color(0xFFFFFF88)'''
new_palette = '''private val DlcBlack     get() = Carbon
private val DlcCardBg    get() = GlassBase
private val DlcCardBgAlt get() = Surface2
private val DlcBorder    get() = GlassStroke
private val DlcBorderHi  get() = GlassStrokeHi
private val DlcText      get() = TextWhite
private val DlcSubText   get() = SoftText
private val DlcMuted     get() = DimText
private val DlcGreen     get() = SuccessGreen
private val DlcRed       get() = DangerRed
private val DlcYellow    get() = AmberWarn'''
text = require_replace(text, old_palette, new_palette, "DLC palette aliases")
text = require_replace(
    text,
    ".background(DlcBlack)\n            .verticalScroll(rememberScrollState())\n            .padding(horizontal = 16.dp, vertical = 20.dp),\n        verticalArrangement = Arrangement.spacedBy(14.dp)",
    ".background(Brush.verticalGradient(listOf(Carbon, PureBlack)))\n            .verticalScroll(rememberScrollState())\n            .padding(horizontal = 18.dp, vertical = 22.dp),\n        verticalArrangement = Arrangement.spacedBy(16.dp)",
    "DLC page canvas",
)
text = require_replace(
    text,
    "Modifier.size(width = 3.dp, height = 18.dp)\n                    .clip(RoundedCornerShape(2.dp))\n                    .background(DlcText)",
    "Modifier.size(width = 4.dp, height = 18.dp)\n                    .clip(RoundedCornerShape(999.dp))\n                    .background(DLavieAccent)",
    "DLC section marker",
)
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Settings: premium page header + unified cards. Functional callbacks untouched.
# ---------------------------------------------------------------------------
path, text = load("SettingsScreen.kt")
text = mark(text)
text = require_replace(
    text,
    "Modifier.fillMaxSize().background(Carbon).verticalScroll(rememberScrollState())",
    "Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Carbon, PureBlack))).verticalScroll(rememberScrollState())",
    "settings page canvas",
)
old_header = '''        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.ArrowBack, null,
                tint = Color.White,
                modifier = Modifier.size(24.dp).clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }
            )
            Spacer(Modifier.width(16.dp))
            Text("Pengaturan", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }'''
new_header = '''        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = Surface2,
                border = BorderStroke(1.dp, GlassStroke),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBack()
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = TextWhite, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Pengaturan",
                    color = TextWhite,
                    fontFamily = InterFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Launcher, penyimpanan, keamanan, dan akun",
                    color = SubText,
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp
                )
            }
        }'''
text = require_replace(text, old_header, new_header, "settings header")
text = require_replace(
    text,
    "Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)",
    "Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)",
    "settings section spacing",
)
text = require_replace(
    text,
    "Icon(icon, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))\n        Spacer(Modifier.width(8.dp))\n        Text(title, color = Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)",
    "Icon(icon, null, tint = TextWhite, modifier = Modifier.size(17.dp))\n        Spacer(Modifier.width(9.dp))\n        Text(title, color = SoftText, fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)",
    "settings section header typography",
)
# Apply the common card spacing/surface to Toggle, Action and Info (three occurrences).
old_card = "modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),\n        shape = TTShapes.card,\n        colors = CardDefaults.cardColors(containerColor = GlassBase),\n        border = BorderStroke(1.dp, GlassStroke)"
new_card = "modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp),\n        shape = RoundedCornerShape(20.dp),\n        colors = CardDefaults.cardColors(containerColor = Surface2.copy(alpha = 0.72f)),\n        border = BorderStroke(1.dp, GlassStroke)"
for index in range(3):
    text = require_replace(text, old_card, new_card, f"settings common card {index + 1}")
text = text.replace(
    "Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))\n                    .background(Color.White.copy(0.05f))",
    "Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))\n                    .background(Surface3)",
)
text = text.replace(
    "Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)",
    "Text(title, color = TextWhite, fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)",
)
text = text.replace(
    "Text(subtitle, color = SubText, fontSize = 11.sp)",
    "Text(subtitle, color = SubText, fontFamily = InterFontFamily, fontSize = 11.sp, lineHeight = 15.sp)",
)
text = text.replace("Color(0xFF4CAF50)", "SuccessGreen")
text = text.replace("Color(0xFFFF5252)", "DangerRed")
path.write_text(text, encoding="utf-8")


# ---------------------------------------------------------------------------
# Game detail: less console-template styling, more compact mobile-first DLavie.
# ---------------------------------------------------------------------------
path, text = load("GameDetailScreen.kt")
text = mark(text)
text = text.replace(
    "GameDetailScreen v7.9.3 — Console-style redesign (PS5/Xbox inspired).",
    "GameDetailScreen — DLavie mobile-first game experience.",
)
text = require_replace(text, "Modifier.fillMaxWidth().height(360.dp)", "Modifier.fillMaxWidth().height(336.dp)", "game hero height")
text = require_replace(text, ".fillMaxSize().blur(40.dp)", ".fillMaxSize().blur(24.dp)", "game hero blur")
text = text.replace(".clip(CircleShape).background(Color.Black.copy(0.6f))", ".clip(RoundedCornerShape(14.dp)).background(Surface2.copy(alpha = 0.88f)).border(1.dp, GlassStroke, RoundedCornerShape(14.dp))")
text = require_replace(
    text,
    "modifier = Modifier.size(120.dp, 160.dp)\n                                .clip(RoundedCornerShape(16.dp)),",
    "modifier = Modifier.size(112.dp, 150.dp)\n                                .clip(RoundedCornerShape(20.dp))\n                                .border(1.dp, GlassStrokeHi, RoundedCornerShape(20.dp)),",
    "game cover geometry",
)
text = require_replace(
    text,
    "fontSize = 26.sp,\n                        fontWeight = FontWeight.Black,",
    "fontSize = 24.sp,\n                        lineHeight = 29.sp,\n                        fontWeight = FontWeight.Bold,",
    "game title hierarchy",
)
text = text.replace("Modifier.clip(RoundedCornerShape(8.dp))", "Modifier.clip(RoundedCornerShape(999.dp))", 1)
text = require_replace(
    text,
    "modifier = Modifier.size(200.dp, 120.dp)\n                                    .clip(RoundedCornerShape(12.dp))",
    "modifier = Modifier.size(214.dp, 128.dp)\n                                    .clip(RoundedCornerShape(16.dp))\n                                    .border(1.dp, GlassStroke, RoundedCornerShape(16.dp))",
    "screenshot geometry",
)
text = require_replace(
    text,
    "shape = RoundedCornerShape(16.dp),\n                colors = CardDefaults.cardColors(containerColor = GlassBase),",
    "shape = RoundedCornerShape(20.dp),\n                colors = CardDefaults.cardColors(containerColor = Surface2.copy(alpha = 0.72f)),",
    "detail card surface",
)
text = require_replace(
    text,
    "Icon(Icons.Rounded.Verified, null, tint = NeonGreen, modifier = Modifier.size(18.dp))\n                Spacer(Modifier.width(6.dp))\n                Text(\"Trusted by DLavie\", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)",
    "Icon(Icons.Rounded.Verified, null, tint = TextWhite, modifier = Modifier.size(18.dp))\n                Spacer(Modifier.width(6.dp))\n                Text(\"Verified by DLavie\", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily)",
    "verified badge",
)
text = require_replace(
    text,
    "color = Carbon.copy(alpha = 0.95f),\n            shadowElevation = 16.dp",
    "color = Color(0xF2101318),\n            shadowElevation = 10.dp,\n            border = BorderStroke(1.dp, GlassStroke)",
    "sticky action surface",
)
text = require_replace(
    text,
    "containerColor = NeonGreen,\n                                contentColor = Color.Black",
    "containerColor = TextWhite,\n                                contentColor = Carbon",
    "play CTA palette",
)
text = require_replace(
    text,
    "Text(\"Play\", fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)",
    "Text(\"Mainkan\", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)",
    "play CTA label",
)
text = require_replace(
    text,
    "containerColor = Color.White,\n                                contentColor = Color.Black",
    "containerColor = TextWhite,\n                                contentColor = Carbon",
    "install CTA palette",
)
text = require_replace(
    text,
    "Text(\"Install\", fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)",
    "Text(\"Install Game\", fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)",
    "install CTA label",
)
path.write_text(text, encoding="utf-8")

print("DLavie screen refresh v2 materialized")
