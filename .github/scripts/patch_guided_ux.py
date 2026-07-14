from pathlib import Path

source = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
if not source.exists():
    raise SystemExit("DLavieGuidedActivity.kt tidak ditemukan")

text = source.read_text()

required = [
    "fun DLavieGuidedApp(",  # v6.8.4: match both () and (deepLinkResult: String? = null)
    "fun GuidedHomeScreen",
    "fun GuidedDataScreen",
    "fun GuidedUpdateScreen",
    "fun guidedDownloadPatch",
    "fun guidedInstallPatch",
]
missing = [item for item in required if item not in text]
if missing:
    raise SystemExit("Guided source tidak lengkap: " + ", ".join(missing))

# Recovery compile patch only. Tidak inject fitur baru, tidak tambah tombol gajelas.
text = text.replace(
    "guidedDownloadPatch(context) { progress -> state = progress }",
    "guidedDownloadPatch(context) { progress: GuidedUpdateState -> state = progress }",
)
text = text.replace(
    "private fun GuidedPage(content: @Composable Column.() -> Unit) {",
    "private fun GuidedPage(content: @Composable () -> Unit) {",
)
text = text.replace(
    '''    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )''',
    '''    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) { content() }''',
)
text = text.replace(
    "private fun GuidedPanel(content: @Composable Column.() -> Unit) {",
    "private fun GuidedPanel(content: @Composable () -> Unit) {",
)
text = text.replace(
    "Column(Modifier.padding(18.dp), content = content)",
    "Column(Modifier.padding(18.dp)) { content() }",
)
text = text.replace(
    'Text("DLavie 26", color = GuideWhite, fontSize = 31.sp',
    'Text("DLavie 26", color = GuideWhite, fontSize = 26.sp',
)

# Kotlin conditional expressions with lambdas were inferred as Any.
# Force them into one explicit () -> Unit lambda so Compose button callbacks compile.
text = text.replace(
    'GuidedPrimaryCta(if (marker.startsWith("v26")) "Mainkan Game" else "Install Full Data", if (marker.startsWith("v26")) "Data siap. Buka FIFA 16." else "Base data belum lengkap.", if (marker.startsWith("v26")) "▶" else "⬇", if (marker.startsWith("v26")) { guidedLaunchGame(context) } else openData)',
    'GuidedPrimaryCta(if (marker.startsWith("v26")) "Mainkan Game" else "Install Full Data", if (marker.startsWith("v26")) "Data siap. Buka FIFA 16." else "Base data belum lengkap.", if (marker.startsWith("v26")) "▶" else "⬇", { if (marker.startsWith("v26")) guidedLaunchGame(context) else openData() })',
)
text = text.replace(
    'GuidedActionButton(if (marker.startsWith("v26")) "Ke Update" else "Buka Installer Data", if (marker.startsWith("v26")) GuideCyan else GuideGreen, if (marker.startsWith("v26")) openUpdate else { guidedOpenClassicInstaller(context) }, true)',
    'GuidedActionButton(if (marker.startsWith("v26")) "Ke Update" else "Buka Installer Data", if (marker.startsWith("v26")) GuideCyan else GuideGreen, { if (marker.startsWith("v26")) openUpdate() else guidedOpenClassicInstaller(context) }, true)',
)

source.write_text(text)
print("Recovery compile sanity OK - lambda callbacks fixed")
