from pathlib import Path

# Stabilizer step.
# Jangan patch Kotlin secara agresif di workflow, karena membuat hasil build berubah-ubah.
# UX besar berikutnya harus masuk langsung ke source file, bukan injected saat CI.
source = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
if not source.exists():
    raise SystemExit("DLavieGuidedActivity.kt tidak ditemukan")

text = source.read_text()
required = [
    "fun DLavieGuidedApp()",
    "fun GuidedHomeScreen",
    "fun GuidedDataScreen",
    "fun GuidedUpdateScreen",
    "fun guidedDownloadPatch",
    "fun guidedInstallPatch",
]
missing = [item for item in required if item not in text]
if missing:
    raise SystemExit("Guided source tidak lengkap: " + ", ".join(missing))

print("Guided UX source sanity OK - no runtime Kotlin injection")
