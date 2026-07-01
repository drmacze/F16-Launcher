from pathlib import Path

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

# Recovery compile patch. Only deterministic Kotlin compatibility fixes.
text = text.replace(
    "guidedDownloadPatch(context) { progress -> state = progress }",
    "guidedDownloadPatch(context) { progress: GuidedUpdateState -> state = progress }",
)
text = text.replace(
    "private fun GuidedPage(content: @Composable Column.() -> Unit) {",
    "private fun GuidedPage(content: @Composable () -> Unit) {",
)
text = text.replace(
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
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) { content() }""",
)
text = text.replace(
    "private fun GuidedPanel(content: @Composable Column.() -> Unit) {",
    "private fun GuidedPanel(content: @Composable () -> Unit) {",
)
text = text.replace(
    "Column(Modifier.padding(18.dp), content = content)",
    "Column(Modifier.padding(18.dp)) { content() }",
)

# Make the Me tab a real user profile page, not a duplicate help page.
old_profile = '''@Composable
private fun GuidedHelpProfileScreen() {
    GuidedPage {
        GuidedPageTitle("Bantuan Cepat", "Panduan lengkap tanpa bikin bingung.")
        GuidedFaqFullCard()
        GuidedCorrectOrderCard()
    }
}'''
new_profile = '''@Composable
private fun GuidedHelpProfileScreen() {
    val context = LocalContext.current
    val installed = remember { guidedIsGameInstalled(context) }
    val marker = remember { guidedReadMarkerSmart() }
    GuidedPage {
        GuidedPageTitle("Profile", "Akun, ticket bantuan, dan status DLavie.")
        GuidedPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GuidedIconTile(GuidedIcon.User, GuideGreen, 62)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("DLavie Player", color = GuideWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1)
                    Text("Support ticket aktif melalui tombol di bawah.", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont, maxLines = 2)
                }
                GuidedPill("USER", GuideGreen)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedInfoBox("Game", if (installed) "Installed" else "Missing", Modifier.weight(1f))
                GuidedInfoBox("Data", marker, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            GuidedActionButton("Buat Ticket / Chat Developer", GuideGreen, { guidedOpenSupportTicket(context, "Profile Support") }, true)
        }
        GuidedFaqFullCard()
    }
}'''
if old_profile in text:
    text = text.replace(old_profile, new_profile)
else:
    print("Profile block already changed or not found")

# Add context-aware support button to assistant panel.
text = text.replace(
    "private fun GuidedFaqPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {\n    Surface(",
    "private fun GuidedFaqPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {\n    val context = LocalContext.current\n    Surface(",
)
text = text.replace(
    """            Text("Halo! Saya bantu panduan update, Shizuku, base data, dan patch.", color = GuideWhite, fontSize = 14.sp, fontFamily = GuideFont)
            GuidedFaqChip("Cara aktifkan Shizuku")""",
    """            Text("Halo! Saya bantu panduan update, Shizuku, base data, dan patch.", color = GuideWhite, fontSize = 14.sp, fontFamily = GuideFont)
            GuidedActionButton("Buat Ticket / Chat Developer", GuideGreen, { guidedOpenSupportTicket(context, "Assistant Support") }, true)
            GuidedFaqChip("Cara aktifkan Shizuku")""",
)

# Add context-aware support button to full FAQ card.
text = text.replace(
    "private fun GuidedFaqFullCard() {\n    GuidedPanel {",
    "private fun GuidedFaqFullCard() {\n    val context = LocalContext.current\n    GuidedPanel {",
)
text = text.replace(
    """        GuidedFaqChip("Cara cek versi lama / baru")
    }
}

@Composable
private fun GuidedCorrectOrderCard()""",
    """        GuidedFaqChip("Cara cek versi lama / baru")
        Spacer(Modifier.height(10.dp))
        GuidedActionButton("Buat Ticket / Chat Developer", GuideGreen, { guidedOpenSupportTicket(context, "FAQ Support") }, true)
    }
}

@Composable
private fun GuidedCorrectOrderCard()""",
)

# Make FAQ topic rows actually clickable and expandable.
old_chip = '''@Composable
private fun GuidedFaqChip(label: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF101716), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GuidedIconMark(GuidedIcon.Help, GuideCyan, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, modifier = Modifier.weight(1f))
            Text("›", color = GuideMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}'''
new_chip = '''@Composable
private fun GuidedFaqChip(label: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101716), contentColor = GuideWhite),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            GuidedIconMark(GuidedIcon.Help, GuideCyan, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (expanded) "⌄" else "›", color = GuideMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        if (expanded) {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B0F0E), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, GuideBorder)) {
                Text(guidedFaqAnswer(label), color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont, modifier = Modifier.padding(12.dp))
            }
        }
    }
}'''
if old_chip in text:
    text = text.replace(old_chip, new_chip)
else:
    print("FAQ chip block already changed or not found")

# Reduce header text size to stop title overlap on small screens.
text = text.replace('Text("DLavie 26", color = GuideWhite, fontSize = 31.sp', 'Text("DLavie 26", color = GuideWhite, fontSize = 26.sp')

# Add FAQ answer and real ticket handoff function.
insert_before = "private fun guidedSha256(file: File): String"
if "private fun guidedFaqAnswer(label: String): String" not in text and insert_before in text:
    support_code = '''private fun guidedFaqAnswer(label: String): String = when (label) {
    "Cara aktifkan Shizuku" -> "Buka aplikasi Shizuku, lakukan Pairing/Start, kembali ke DLavie, lalu tekan Izinkan Shizuku. Jika status belum Ready, ulangi Start di Shizuku."
    "Apa itu Base Data?" -> "Base Data adalah OBB dan data utama FIFA 16. Patch tidak akan masuk ke game jika Base Data belum lengkap atau marker tidak ditemukan."
    "Patch belum masuk ke game" -> "Urutan benar: Check Update, Download Patch, SHA harus OK, Shizuku Ready, lalu Apply Patch. Setelah itu launch game ulang."
    "Download gagal / retry" -> "Tekan Download Patch lagi. DLavie memakai retry dan cache; file tidak akan dipakai jika SHA tidak cocok."
    "Cara cek versi lama / baru" -> "Buka tab Update. Local adalah versi di HP kamu. Terbaru adalah versi dari manifest DLavie. Jika terbaru lebih tinggi, tombol Download Patch muncul."
    else -> "Pilih topik bantuan atau buat ticket agar developer bisa membaca detail masalah kamu."
}

private fun guidedOpenSupportTicket(context: Context, topic: String) {
    val marker = guidedReadMarkerSmart()
    val shizuku = guidedShizukuState()
    val body = buildString {
        appendLine("DLavie Support Ticket")
        appendLine("Topic: $topic")
        appendLine("Game package: $GUIDE_GAME_PACKAGE")
        appendLine("Data marker: $marker")
        appendLine("Shizuku: $shizuku")
        appendLine()
        appendLine("Tulis masalah kamu di bawah ini:")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "DLavie Ticket - $topic")
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Kirim ticket ke developer").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

'''
    text = text.replace(insert_before, support_code + insert_before)

source.write_text(text)
print("Guided FAQ, profile, and support ticket actions patched")
