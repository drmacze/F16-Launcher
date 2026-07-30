from pathlib import Path

SOURCE = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
text = SOURCE.read_text(encoding="utf-8")

text = text.replace(
    ".padding(top = 100.dp, bottom = 40.dp)",
    ".padding(top = 56.dp, bottom = 32.dp)",
    1,
)
text = text.replace("AnimatedDLLogo(size = 80.dp)", "AnimatedDLLogo(size = 64.dp)", 1)
text = text.replace('"chooser"  -> "DLAVIE PORTAL"', '"chooser"  -> "DLavie Portal"', 1)
text = text.replace('else       -> "DLAVIE PORTAL"', 'else       -> "DLavie Portal"', 1)
text = text.replace(
    '"chooser" -> "Sign in or connect your DLavie Launcher account\\nto access all web features."',
    '"chooser" -> "Hubungkan akun DLavie Anda dengan aman."',
    1,
)
text = text.replace(
    "            Spacer(Modifier.height(48.dp))\n\n            // ── Mode: CHOOSER",
    "            Spacer(Modifier.height(28.dp))\n\n            // ── Mode: CHOOSER",
    1,
)

start_marker = "            // ── Mode: CHOOSER"
end_marker = "\n            // ── Mode: LOGIN / REGISTER / FORGOT"
start = text.find(start_marker)
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit("Portal chooser markers not found")

replacement = '''            // ── Mode: CHOOSER — focused Portal connection experience ──
            if (mode == "chooser") {
                ProfessionalPortalConnectContent(
                    onOpenPortal = {
                        val portalUrl = "https://drmacze.github.io/dlavie-web/#/portal?from=launcher"
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(portalUrl),
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    },
                )
            }
'''
text = text[:start] + replacement + text[end:]

for forbidden in (
    "Update to Latest Version",
    "Download & Install",
    "Update available: v$latestVersionName",
    "Tidak bisa update otomatis? Buka browser",
    "Login aman dilakukan langsung di launcher. Portal web tidak mengirim token ke aplikasi.",
):
    if forbidden in text:
        raise SystemExit(f"Legacy Portal copy still present: {forbidden}")

required = (
    "ProfessionalPortalConnectContent(",
    "Hubungkan akun DLavie Anda dengan aman.",
    "https://drmacze.github.io/dlavie-web/#/portal?from=launcher",
)
for token in required:
    if token not in text:
        raise SystemExit(f"Required Portal redesign token missing: {token}")

SOURCE.write_text(text, encoding="utf-8")
print("Portal connection screen redesign materialized.")
