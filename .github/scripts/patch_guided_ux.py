from pathlib import Path

p = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
s = p.read_text()

# Kotlin compatibility fixes that are safe and idempotent.
s = s.replace(
    "guidedDownloadPatch(context) { progress -> state = progress }",
    "guidedDownloadPatch(context) { progress: GuidedUpdateState -> state = progress }",
)
s = s.replace(
    "private fun GuidedPage(content: @Composable Column.() -> Unit) {",
    "private fun GuidedPage(content: @Composable () -> Unit) {",
)
s = s.replace(
    """    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )""",
    """    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }""",
)
s = s.replace(
    "private fun GuidedPanel(content: @Composable Column.() -> Unit) {",
    "private fun GuidedPanel(content: @Composable () -> Unit) {",
)
s = s.replace(
    "Column(Modifier.padding(18.dp), content = content)",
    "Column(Modifier.padding(16.dp)) { content() }",
)

# Public UX routing: Me is profile, floating help is FAQ assistant.
s = s.replace(
    "GuidedTab.Me -> GuidedHelpProfileScreen()",
    "GuidedTab.Me -> GuidedProfileScreen()",
)
s = s.replace(
    "GuidedFaqPanel(\n                        onClose = { faqOpen = false },",
    "GuidedFaqPanelPro(\n                        onClose = { faqOpen = false },",
)

# Compact visual polish and working launch step.
s = s.replace('Text("DLavie 26", color = GuideWhite, fontSize = 31.sp', 'Text("DLavie 26", color = GuideWhite, fontSize = 28.sp')
s = s.replace('modifier = Modifier.size(76.dp)', 'modifier = Modifier.size(68.dp)')
s = s.replace('modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(76.dp)', 'modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(68.dp)')
s = s.replace('GuidedIconTile(icon, color, 44)', 'GuidedIconTile(icon, color, 38)')
s = s.replace('fontSize = 15.sp, fontWeight = FontWeight.Black', 'fontSize = 14.sp, fontWeight = FontWeight.Black')
s = s.replace('Surface(modifier = modifier.height(70.dp)', 'Surface(modifier = modifier.height(62.dp)')
s = s.replace(
    'GuidedStepRow(4, "Mainkan Game", "Launch setelah data siap.", if (update.marker.startsWith("v26")) "READY" else "NANTI", GuidedIcon.Rocket, Color(0xFFB783FF), { })',
    'GuidedStepRow(4, "Mainkan Game", "Launch setelah data siap.", if (update.marker.startsWith("v26")) "READY" else "NANTI", GuidedIcon.Rocket, Color(0xFFB783FF), { guidedLaunchGame(LocalContext.current) })',
)

extra = r'''

@Composable
private fun GuidedProfileScreen() {
    val context = LocalContext.current
    val marker = guidedReadMarkerSmart()
    val shizuku = guidedShizukuState()
    GuidedPage {
        GuidedPageTitle("Profile", "Profil user dan status DLavie.")
        GuidedPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp).background(Brush.linearGradient(listOf(Color(0xFF063B27), Color(0xFF09110F))), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("g", color = GuideGreen, fontSize = 31.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("gibran al bukhary", color = GuideWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@gibran_al_bukhary", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont, maxLines = 1)
                    Text("DLavie 26 player", color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont, maxLines = 1)
                }
                GuidedPill("LOCAL", GuideCyan)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedInfoBox("Game", if (guidedIsGameInstalled(context)) "Installed" else "Missing", Modifier.weight(1f))
                GuidedInfoBox("Shizuku", shizuku, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedInfoBox("Data", marker, Modifier.weight(1f))
                GuidedInfoBox("Account", "Local", Modifier.weight(1f))
            }
        }
        GuidedPanel {
            Text("Pengaturan", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            Spacer(Modifier.height(10.dp))
            GuidedActionButton("Buka FIFA 16", GuideGreen, { guidedLaunchGame(context) }, true)
            Spacer(Modifier.height(8.dp))
            Text("Login online dan avatar cloud akan masuk build backend berikutnya. Tidak ada dummy chat/post.", color = GuideMuted, fontSize = 13.sp, lineHeight = 19.sp, fontFamily = GuideFont)
        }
    }
}

@Composable
private fun GuidedFaqPanelPro(onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf("Cara aktifkan Shizuku") }
    Surface(modifier = modifier.fillMaxWidth(), color = Color(0xF40B0F0E), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, GuideBorder), shadowElevation = 20.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GuidedIconTile(GuidedIcon.Bot, GuideGreen, 46)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Asisten DLavie", color = GuideWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                    Text("FAQ offline • panduan aktif", color = GuideGreen, fontSize = 12.sp, fontFamily = GuideFont)
                }
                GuidedActionTiny("Tutup", onClose)
            }
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF101716), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
                Column(Modifier.padding(12.dp)) {
                    Text(selected, color = GuideWhite, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                    Spacer(Modifier.height(6.dp))
                    Text(guidedFaqAnswerPro(selected), color = GuideMuted, fontSize = 12.sp, lineHeight = 18.sp, fontFamily = GuideFont)
                }
            }
            GuidedFaqButtonRow("Cara aktifkan Shizuku") { selected = "Cara aktifkan Shizuku" }
            GuidedFaqButtonRow("Apa itu Base Data?") { selected = "Apa itu Base Data?" }
            GuidedFaqButtonRow("Patch belum masuk ke game") { selected = "Patch belum masuk ke game" }
            GuidedFaqButtonRow("Download gagal / retry") { selected = "Download gagal / retry" }
            GuidedFaqButtonRow("Cara cek versi lama / baru") { selected = "Cara cek versi lama / baru" }
        }
    }
}

@Composable
private fun GuidedFaqButtonRow(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101716), contentColor = GuideWhite),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        GuidedIconMark(GuidedIcon.Help, GuideCyan, Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("›", color = GuideMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

private fun guidedFaqAnswerPro(title: String): String = when (title) {
    "Cara aktifkan Shizuku" -> "1. Buka aplikasi Shizuku.\n2. Aktifkan Wireless Debugging / Start.\n3. Balik ke DLavie.\n4. Tekan Izinkan Shizuku.\n5. Status harus Ready."
    "Apa itu Base Data?" -> "Base Data adalah OBB + data utama DLavie 26. Ini wajib sebelum patch. Kalau marker tidak ada, install full data dulu di tab Data."
    "Patch belum masuk ke game" -> "Urutannya: Download Patch → SHA OK → Apply Patch → restart FIFA 16. Jika game masih lama, tutup game total lalu buka ulang."
    "Download gagal / retry" -> "Launcher retry 3x dan cache file valid. Pastikan internet stabil, storage cukup, lalu tekan Download Patch lagi."
    else -> "Local adalah versi di HP. Latest adalah versi server. Jika Latest lebih tinggi, tombol Download Patch muncul. Local naik hanya setelah apply sukses."
}
'''

if "private fun GuidedProfileScreen" not in s:
    s += extra

p.write_text(s)
print("Guided UX patch applied safely")
