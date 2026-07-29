from pathlib import Path

source = Path("app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt")
if not source.exists():
    raise SystemExit("DLavieGuidedActivity.kt tidak ditemukan")

text = source.read_text(encoding="utf-8")

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

source.write_text(text, encoding="utf-8")

# v325: Replace the noisy update dialog with one focused, professional action.
update_source = Path("app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt")
if not update_source.exists():
    raise SystemExit("ModernLauncherActivity.kt tidak ditemukan")

update_text = update_source.read_text(encoding="utf-8")
start_marker = "// ─── App Update Popup"
end_marker = "// ─── Helper functions"

if start_marker not in update_text or end_marker not in update_text:
    raise SystemExit("Marker AppUpdatePopup tidak ditemukan")

start = update_text.index(start_marker)
end = update_text.index(end_marker, start)

minimal_update_popup = r'''// ─── App Update Popup ─────────────────────────────────────────────────────────
@Composable
fun AppUpdatePopup(
    info: AppUpdateChecker.UpdateInfo,
    downloading: Boolean,
    progress: Float,
    error: String = "",
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onOpenWebsite: () -> Unit = {}
) {
    val forceUpdate = info.forceUpdate
    val latestVersion = "v${info.versionName.removePrefix("v")}" 

    Dialog(
        onDismissRequest = {
            if (!downloading && !forceUpdate) onLater()
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = GlassBase,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, GlassStroke)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Pembaruan Launcher",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when {
                                forceUpdate -> "Diperlukan untuk melanjutkan"
                                !info.isPublished -> "Versi pratinjau"
                                else -> "Siap dipasang"
                            },
                            color = SubText,
                            fontSize = 11.sp
                        )
                    }

                    if (forceUpdate) {
                        Surface(
                            color = DangerRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.30f))
                        ) {
                            Text(
                                "WAJIB",
                                color = DangerRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.035f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "VERSI SAAT INI",
                                color = SubText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Build ${info.currentVersionCode}",
                                color = SoftText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = SubText,
                            modifier = Modifier.size(18.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "VERSI TERBARU",
                                color = SubText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                latestVersion,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    if (forceUpdate) {
                        "Perbarui launcher untuk tetap menggunakan layanan DLavie."
                    } else {
                        "Peningkatan stabilitas dan keamanan siap dipasang."
                    },
                    color = SoftText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                if (info.apkSizeMb.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            tint = SubText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "${info.apkSizeMb}  •  Android APK",
                            color = SubText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (downloading && !forceUpdate) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                    Text(
                        "Mengunduh ${(progress * 100).toInt()}%",
                        color = SubText,
                        fontSize = 10.sp
                    )
                }

                if (error.isNotBlank() && !forceUpdate) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DangerRed.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.20f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                error,
                                color = DangerRed,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Text(
                    "Data akun dan pengaturan Anda tetap tersimpan.",
                    color = SubText,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        if (forceUpdate) onOpenWebsite() else onUpdate()
                    },
                    enabled = !downloading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Carbon,
                        disabledContainerColor = Color.White.copy(alpha = 0.55f),
                        disabledContentColor = Carbon.copy(alpha = 0.65f)
                    )
                ) {
                    if (downloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            color = Carbon,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Mengunduh...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Perbarui Sekarang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!forceUpdate) {
                    TextButton(
                        onClick = onLater,
                        enabled = !downloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nanti", color = SubText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
'''

update_text = update_text[:start] + minimal_update_popup.strip() + "\n\n" + update_text[end:]

if 'Versi baru ${info.versionName} sudah tersedia.' in update_text:
    raise SystemExit("Teks versi lama masih ditemukan")
if 'Text("Perbarui Sekarang"' not in update_text:
    raise SystemExit("CTA update baru tidak ditemukan")

update_source.write_text(update_text, encoding="utf-8")
print("Recovery compile sanity OK + minimal update popup applied")
