from pathlib import Path
import re

p = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
s = p.read_text()

if "import androidx.compose.material3.OutlinedTextField" not in s:
    s = s.replace("import androidx.compose.material3.MaterialTheme\n", "import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.OutlinedTextField\n")

# Kotlin compatibility
s = s.replace(
    "guidedDownloadPatch(context) { progress -> state = progress }",
    "guidedDownloadPatch(context) { progress: GuidedUpdateState -> state = progress }"
)
s = s.replace(
    "private fun GuidedPage(content: @Composable Column.() -> Unit) {",
    "private fun GuidedPage(content: @Composable () -> Unit) {"
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
    ) { content() }"""
)
s = s.replace(
    "private fun GuidedPanel(content: @Composable Column.() -> Unit) {",
    "private fun GuidedPanel(content: @Composable () -> Unit) {"
)
s = s.replace(
    "Column(Modifier.padding(18.dp), content = content)",
    "Column(Modifier.padding(16.dp)) { content() }"
)

# Compact visual polish
s = s.replace('Text("DLavie 26", color = GuideWhite, fontSize = 31.sp', 'Text("DLavie 26", color = GuideWhite, fontSize = 28.sp')
s = s.replace('modifier = Modifier.size(76.dp)', 'modifier = Modifier.size(68.dp)')
s = s.replace('modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(76.dp)', 'modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(68.dp)')
s = s.replace('GuidedIconTile(icon, color, 44)', 'GuidedIconTile(icon, color, 38)')
s = s.replace('fontSize = 15.sp, fontWeight = FontWeight.Black', 'fontSize = 14.sp, fontWeight = FontWeight.Black')
s = s.replace('Surface(modifier = modifier.height(70.dp)', 'Surface(modifier = modifier.height(62.dp)')
s = s.replace(
    'GuidedStepRow(4, "Mainkan Game", "Launch setelah data siap.", if (update.marker.startsWith("v26")) "READY" else "NANTI", GuidedIcon.Rocket, Color(0xFFB783FF), { })',
    'GuidedStepRow(4, "Mainkan Game", "Launch setelah data siap.", if (update.marker.startsWith("v26")) "READY" else "NANTI", GuidedIcon.Rocket, Color(0xFFB783FF), { guidedLaunchGame(LocalContext.current) })'
)

profile_func = '''@Composable
private fun GuidedHelpProfileScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dlavie_profile", Context.MODE_PRIVATE) }
    var display by remember { mutableStateOf(prefs.getString("display", "gibran al bukhary") ?: "gibran al bukhary") }
    var user by remember { mutableStateOf(prefs.getString("username", "gibran_al_bukhary") ?: "gibran_al_bukhary") }
    var bio by remember { mutableStateOf(prefs.getString("bio", "DLavie 26 player") ?: "DLavie 26 player") }
    var editing by remember { mutableStateOf(false) }
    val marker = guidedReadMarkerSmart()
    val shizuku = guidedShizukuState()
    GuidedPage {
        GuidedPageTitle("Profile", "Profil user, status data, dan akun DLavie.")
        GuidedPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(76.dp).background(Brush.linearGradient(listOf(Color(0xFF063B27), Color(0xFF09110F))), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                    Text(user.firstOrNull()?.uppercase() ?: "D", color = GuideGreen, fontSize = 30.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(display, color = GuideWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@$user", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont, maxLines = 1)
                    Text(bio, color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Spacer(Modifier.height(12.dp))
            if (editing) {
                OutlinedTextField(value = display, onValueChange = { display = it.take(28) }, label = { Text("Nama") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = user, onValueChange = { user = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(24) }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = bio, onValueChange = { bio = it.take(60) }, label = { Text("Bio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                GuidedActionButton("Simpan Profil", GuideGreen, { prefs.edit().putString("display", display).putString("username", user).putString("bio", bio).apply(); editing = false }, true)
            } else {
                GuidedActionButton("Edit Profil", GuideCyan, { editing = true }, true)
            }
        }
        GuidedPanel {
            Text("Bantuan User", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            Spacer(Modifier.height(10.dp))
            Text("Gunakan tombol bantuan mengambang untuk panduan Shizuku, Base Data, download, dan patch.", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont)
        }
    }
}'''
s = re.sub(r'@Composable\nprivate fun GuidedHelpProfileScreen\(\) \{.*?\n\}', profile_func, s, count=1, flags=re.S)

faq_panel = '''@Composable
private fun GuidedFaqPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf("Cara aktifkan Shizuku") }
    Surface(modifier = modifier.fillMaxWidth(), color = Color(0xF40B0F0E), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, GuideBorder), shadowElevation = 20.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GuidedIconTile(GuidedIcon.Bot, GuideGreen, 46)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Asisten DLavie", color = GuideWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                    Text("FAQ offline • tombol aktif", color = GuideGreen, fontSize = 12.sp, fontFamily = GuideFont)
                }
                GuidedActionTiny("Tutup", onClose)
            }
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF101716), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
                Column(Modifier.padding(12.dp)) {
                    Text(selected, color = GuideWhite, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                    Spacer(Modifier.height(6.dp))
                    Text(guidedFaqAnswer(selected), color = GuideMuted, fontSize = 12.sp, lineHeight = 18.sp, fontFamily = GuideFont)
                }
            }
            GuidedFaqChip("Cara aktifkan Shizuku") { selected = "Cara aktifkan Shizuku" }
            GuidedFaqChip("Apa itu Base Data?") { selected = "Apa itu Base Data?" }
            GuidedFaqChip("Patch belum masuk ke game") { selected = "Patch belum masuk ke game" }
            GuidedFaqChip("Download gagal / retry") { selected = "Download gagal / retry" }
            GuidedFaqChip("Cara cek versi lama / baru") { selected = "Cara cek versi lama / baru" }
        }
    }
}'''
s = re.sub(r'@Composable\nprivate fun GuidedFaqPanel\(onClose: \(\) -> Unit, modifier: Modifier = Modifier\) \{.*?\n\}', faq_panel, s, count=1, flags=re.S)

s = s.replace(
    '''@Composable
private fun GuidedFaqChip(label: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF101716), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GuidedIconMark(GuidedIcon.Help, GuideCyan, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, modifier = Modifier.weight(1f))
            Text("›", color = GuideMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}''',
    '''@Composable
private fun GuidedFaqChip(label: String, onClick: (() -> Unit)? = null) {
    Button(onClick = { onClick?.invoke() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101716), contentColor = GuideWhite), contentPadding = PaddingValues(horizontal = 14.dp)) {
        GuidedIconMark(GuidedIcon.Help, GuideCyan, Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("›", color = GuideMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}'''
)

helper = '''
private fun guidedFaqAnswer(title: String): String = when (title) {
    "Cara aktifkan Shizuku" -> "1. Buka aplikasi Shizuku.\n2. Aktifkan Wireless Debugging / Start.\n3. Balik ke DLavie.\n4. Tekan Izinkan Shizuku.\n5. Status harus Ready."
    "Apa itu Base Data?" -> "Base Data adalah OBB + data utama DLavie 26. Ini wajib sebelum patch. Kalau marker tidak ada, install full data dulu di tab Data."
    "Patch belum masuk ke game" -> "Urutannya: Download Patch → SHA OK → Apply Patch → restart FIFA 16. Jika game masih lama, tutup game total lalu buka ulang."
    "Download gagal / retry" -> "Launcher retry 3x dan cache file valid. Pastikan internet stabil, storage cukup, lalu tekan Download Patch lagi."
    else -> "Local adalah versi di HP. Latest adalah versi server. Jika Latest lebih tinggi, tombol Download Patch muncul. Local naik hanya setelah apply sukses."
}
'''
if "private fun guidedFaqAnswer" not in s:
    s = s.replace("private fun guidedInitialUpdate(context: Context): GuidedUpdateState {", helper + "\nprivate fun guidedInitialUpdate(context: Context): GuidedUpdateState {")

p.write_text(s)
print("Guided UX profile, FAQ, spacing, and buttons patched")
