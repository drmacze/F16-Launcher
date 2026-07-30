from pathlib import Path
import re


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f'missing {label}')
    return text.replace(old, new, 1)

# Version
build = Path('app/build.gradle')
text = build.read_text(encoding='utf-8')
text = re.sub(r'versionCode\s+\d+', 'versionCode 332', text, count=1)
text = re.sub(r'versionName\s+"[^"]+"', 'versionName "8.6.0"', text, count=1)
build.write_text(text, encoding='utf-8')

# Android RTL capability
manifest = Path('app/src/main/AndroidManifest.xml')
text = manifest.read_text(encoding='utf-8')
if 'android:supportsRtl="true"' not in text:
    text = text.replace('android:roundIcon="@mipmap/dlavie_launcher_icon"', 'android:roundIcon="@mipmap/dlavie_launcher_icon"\n        android:supportsRtl="true"', 1)
manifest.write_text(text, encoding='utf-8')

# Expand the migration catalog with all Portal/update chrome used by the pre-auth flow.
locale_file = Path('app/src/main/java/com/drmacze/f16launcher/LocaleText.kt')
text = locale_file.read_text(encoding='utf-8')
english_anchor = '        "portal.failed" to "The connection could not be completed. No account data was saved.",\n'
english_extra = '''        "portal.checking_launcher" to "Checking launcher",
        "portal.launcher_ready" to "Launcher is ready",
        "portal.fetch_version" to "Fetching the latest version…",
        "portal.update_required" to "UPDATE REQUIRED",
        "portal.update_description" to "Update the launcher before connecting your Portal account.",
        "portal.download_failed" to "The download could not be completed.",
        "portal.installer_failed" to "Android installer could not be opened.",
        "portal.opening_installer" to "Opening installer…",
        "portal.downloading" to "Downloading {percent}%",
        "portal.preparing_installer" to "Preparing installer…",
        "portal.update_launcher" to "Update Launcher",
        "portal.open_download" to "Open download page",
        "portal.waiting_update" to "WAITING FOR UPDATE",
        "portal.ready_connect" to "READY TO CONNECT",
        "portal.use_account" to "Use your Portal account",
        "portal.complete_update" to "Complete the launcher update to continue connecting your account.",
        "portal.connection_description" to "Choose your account in Portal, approve the connection, and return to the launcher automatically.",
        "portal.step_login_detail" to "Use your email address or Google account.",
        "portal.step_approve_detail" to "Make sure the active account is correct.",
        "portal.step_return_detail" to "The launcher will use the same Portal account.",
        "portal.update_first" to "Update the launcher first",
        "portal.available_after_update" to "Connection is available after the launcher is updated",
        "portal.one_minute" to "About one minute • no token copying",
        "portal.preparing_request" to "The launcher is creating a secure request for your Portal account.",
        "portal.continue_in_portal" to "Continue in Portal",
        "portal.confirm_account" to "Confirm the account you want to use in the browser.",
        "portal.return_after_approval" to "You will return to the launcher automatically after approval.",
        "portal.browser_failed" to "The browser could not be opened. Check your default browser and try again.",
        "portal.session_ended" to "The connection session has ended. Start a new connection from the launcher.",
        "portal.verify_code" to "Verifying connection",
        "portal.verify_message" to "The launcher is checking the one-time code from Portal.",
        "portal.keep_open" to "Keep the application open until verification is complete.",
        "portal.sync_profile" to "Preparing account",
        "portal.sync_message" to "Your profile and preferences are being synced to the launcher.",
        "portal.same_account" to "The launcher now uses the same Portal account.",
        "portal.opening_home" to "Opening home…",
        "portal.account_mismatch" to "The returned Portal account did not match. Sign in again and choose the correct account.",
        "portal.network_failed" to "The internet connection was interrupted. Check your network and try again.",
'''
if '"portal.checking_launcher"' not in text:
    text = replace_once(text, english_anchor, english_anchor + english_extra, 'english portal anchor')

id_anchor = '        "portal.failed" to "Koneksi belum dapat diselesaikan. Tidak ada data akun yang disimpan.",\n'
id_extra = '''        "portal.checking_launcher" to "Memeriksa launcher",
        "portal.launcher_ready" to "Launcher siap digunakan",
        "portal.fetch_version" to "Mengambil status versi terbaru…",
        "portal.update_required" to "PEMBARUAN DIPERLUKAN",
        "portal.update_description" to "Perbarui launcher terlebih dahulu, lalu hubungkan akun Portal Anda.",
        "portal.download_failed" to "Unduhan tidak dapat diselesaikan.",
        "portal.installer_failed" to "Installer Android tidak dapat dibuka.",
        "portal.opening_installer" to "Membuka installer…",
        "portal.downloading" to "Mengunduh {percent}%",
        "portal.preparing_installer" to "Menyiapkan installer…",
        "portal.update_launcher" to "Perbarui Launcher",
        "portal.open_download" to "Buka halaman download",
        "portal.waiting_update" to "MENUNGGU PEMBARUAN",
        "portal.ready_connect" to "SIAP DIHUBUNGKAN",
        "portal.use_account" to "Gunakan akun Portal Anda",
        "portal.complete_update" to "Selesaikan pembaruan launcher untuk melanjutkan koneksi akun.",
        "portal.connection_description" to "Pilih akun di Portal, setujui koneksi, lalu launcher akan kembali dan masuk secara otomatis.",
        "portal.step_login_detail" to "Gunakan email atau akun Google Anda.",
        "portal.step_approve_detail" to "Pastikan akun yang aktif sudah benar.",
        "portal.step_return_detail" to "Launcher akan memakai akun Portal yang sama.",
        "portal.update_first" to "Perbarui launcher terlebih dahulu",
        "portal.available_after_update" to "Koneksi tersedia setelah launcher diperbarui",
        "portal.one_minute" to "Sekitar satu menit • tanpa menyalin token",
        "portal.preparing_request" to "Launcher sedang membuat permintaan yang aman untuk akun Portal Anda.",
        "portal.continue_in_portal" to "Lanjutkan di Portal",
        "portal.confirm_account" to "Konfirmasi akun yang ingin digunakan di browser.",
        "portal.return_after_approval" to "Setelah disetujui, Anda akan kembali ke launcher secara otomatis.",
        "portal.browser_failed" to "Browser tidak dapat dibuka. Periksa browser default Anda lalu coba lagi.",
        "portal.session_ended" to "Sesi koneksi sudah berakhir. Mulai koneksi baru dari launcher.",
        "portal.verify_code" to "Memverifikasi koneksi",
        "portal.verify_message" to "Launcher sedang memeriksa kode sekali pakai dari Portal.",
        "portal.keep_open" to "Jangan tutup aplikasi sampai verifikasi selesai.",
        "portal.sync_profile" to "Menyiapkan akun",
        "portal.sync_message" to "Profil dan preferensi akun sedang disinkronkan ke launcher.",
        "portal.same_account" to "Launcher sekarang menggunakan akun Portal yang sama.",
        "portal.opening_home" to "Membuka halaman utama…",
        "portal.account_mismatch" to "Akun yang dikembalikan Portal tidak cocok. Login kembali dan pilih akun yang benar.",
        "portal.network_failed" to "Koneksi internet terputus. Periksa jaringan lalu coba lagi.",
'''
if '"portal.checking_launcher" to "Memeriksa launcher"' not in text:
    text = replace_once(text, id_anchor, id_anchor + id_extra, 'indonesian portal anchor')
locale_file.write_text(text, encoding='utf-8')

# Apply locale before every user-facing activity renders.
modern = Path('app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt')
text = modern.read_text(encoding='utf-8')
text = re.sub(
    r'    override fun attachBaseContext\(newBase: android\.content\.Context\) \{.*?\n    \}\n',
    '    override fun attachBaseContext(newBase: android.content.Context) {\n        super.attachBaseContext(LanguageManager.applyLocale(newBase))\n    }\n',
    text,
    count=1,
    flags=re.S,
)

new_language_card = '''@Composable
fun LanguageSettingsCard(context: android.content.Context) {
    var preference by remember { mutableStateOf(LanguageManager.getPreference(context)) }
    val haptic = LocalHapticFeedback.current
    fun label(key: String) = LocaleText.get(context, key)

    GlassCard {
        TTSectionHeader(title = label("language.title"), icon = Icons.Rounded.Language)
        Spacer(Modifier.height(TTSpacing.sm))
        ProfRow(label("language.current"), LanguageManager.getCurrentLanguageName(context))
        ProfRow(
            label("language.mode"),
            if (preference.mode == LanguageManager.PreferenceMode.AUTO) label("language.auto_mode") else label("language.manual_mode"),
        )
        Spacer(Modifier.height(TTSpacing.md))
        Text(label("language.select"), color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
        Spacer(Modifier.height(TTSpacing.xs))

        val autoSelected = preference.mode == LanguageManager.PreferenceMode.AUTO
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                LanguageManager.resetToAutoDetect(context)
                preference = LanguageManager.getPreference(context)
                LocalePreferenceSync.pushAsync(context)
                (context as? android.app.Activity)?.recreate()
            },
            color = if (autoSelected) TextWhite.copy(alpha = 0.15f) else Surface1,
            border = BorderStroke(1.dp, if (autoSelected) TextWhite.copy(alpha = 0.5f) else GlassStroke),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🌐", fontSize = 19.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(label("language.use_device"), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(label("language.auto_description"), color = SubText, fontSize = 10.sp, lineHeight = 14.sp)
                }
                if (autoSelected) Icon(Icons.Rounded.CheckCircle, null, tint = TextWhite, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(TTSpacing.sm))
        LanguageManager.getSupportedLanguages().chunked(2).forEach { rowLanguages ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TTSpacing.sm)) {
                rowLanguages.forEach { lang ->
                    val selected = preference.mode == LanguageManager.PreferenceMode.MANUAL && preference.resolvedCode == lang.code
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            LanguageManager.setLanguage(context, lang.code)
                            preference = LanguageManager.getPreference(context)
                            LocalePreferenceSync.pushAsync(context)
                            (context as? android.app.Activity)?.recreate()
                        },
                        color = if (selected) TextWhite.copy(alpha = 0.15f) else Surface1,
                        border = BorderStroke(1.dp, if (selected) TextWhite.copy(alpha = 0.5f) else GlassStroke),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 11.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(lang.flag, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(lang.nativeName, color = if (selected) TextWhite else SoftText, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                                Text(lang.displayName, color = SubText, fontSize = 9.sp, maxLines = 1)
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = TextWhite, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (rowLanguages.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(TTSpacing.sm))
        }

        Text(label("language.applied"), color = SubText, fontSize = 10.sp, fontFamily = InterFontFamily)
    }
}

/**
 * Upload Android version'''
text, count = re.subn(
    r'@Composable\nfun LanguageSettingsCard\(context: android\.content\.Context\) \{.*?\n\}\n\n/\*\*\n \* Upload Android version',
    new_language_card,
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit('Could not replace LanguageSettingsCard')
modern.write_text(text, encoding='utf-8')

# Localized startup screen.
splash = Path('app/src/main/java/com/drmacze/f16launcher/ShinySplashActivity.kt')
text = splash.read_text(encoding='utf-8')
if 'import androidx.compose.ui.platform.LocalContext' not in text:
    text = text.replace('import androidx.compose.ui.graphics.Color\n', 'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.platform.LocalContext\n', 1)
text = text.replace(
    'class ShinySplashActivity : ComponentActivity() {\n    override fun onCreate',
    'class ShinySplashActivity : ComponentActivity() {\n    override fun attachBaseContext(newBase: android.content.Context) {\n        super.attachBaseContext(LanguageManager.applyLocale(newBase))\n    }\n\n    override fun onCreate',
    1,
)
text = text.replace(
    'private fun DLavieStartupScreen(onFinished: () -> Unit) {\n    val contentAlpha',
    'private fun DLavieStartupScreen(onFinished: () -> Unit) {\n    val context = LocalContext.current\n    val contentAlpha',
    1,
)
text = text.replace('var status by remember { mutableStateOf("Memeriksa sesi") }', 'var status by remember { mutableStateOf(LocaleText.get(context, "startup.check_session")) }')
text = text.replace('status = "Menyiapkan layanan"', 'status = LocaleText.get(context, "startup.prepare_services")')
text = text.replace('status = "Menyiapkan antarmuka"', 'status = LocaleText.get(context, "startup.prepare_interface")')
text = text.replace('status = "Siap"', 'status = LocaleText.get(context, "startup.ready")')
text = text.replace('text = "MOBILE LAUNCHER"', 'text = LocaleText.get(context, "startup.product")')
splash.write_text(text, encoding='utf-8')

# Locale-aware login activity.
guided = Path('app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt')
text = guided.read_text(encoding='utf-8')
text = text.replace(
    'class DLavieGuidedActivity : ComponentActivity() {\n',
    'class DLavieGuidedActivity : ComponentActivity() {\n\n    override fun attachBaseContext(newBase: Context) {\n        super.attachBaseContext(LanguageManager.applyLocale(newBase))\n    }\n',
    1,
)
guided.write_text(text, encoding='utf-8')

# Portal pre-auth screen.
portal = Path('app/src/main/java/com/drmacze/f16launcher/PortalConnectScreen.kt')
text = portal.read_text(encoding='utf-8')
text = text.replace('private fun PortalVersionStatus(checking: Boolean) {\n', 'private fun PortalVersionStatus(checking: Boolean) {\n    val context = LocalContext.current\n', 1)
text = text.replace('private fun PortalUpdateCard(\n', 'private fun PortalUpdateCard(\n', 1)
text = text.replace(') {\n    val busy = phase == PortalUpdatePhase.DOWNLOADING', ') {\n    val context = LocalContext.current\n    val busy = phase == PortalUpdatePhase.DOWNLOADING', 1)
text = text.replace('private fun PortalAccountCard(\n', 'private fun PortalAccountCard(\n', 1)
# The second matching function body after PortalAccountCard.
account_signature = '''private fun PortalAccountCard(
    enabled: Boolean,
    updateRequired: Boolean,
    onOpenPortal: () -> Unit,
) {
'''
text = text.replace(account_signature, account_signature + '    val context = LocalContext.current\n', 1)
replacements = {
    'if (checking) "Memeriksa launcher" else "Launcher siap digunakan"': 'if (checking) LocaleText.get(context, "portal.checking_launcher") else LocaleText.get(context, "portal.launcher_ready")',
    '"Mengambil status versi terbaru…"': 'LocaleText.get(context, "portal.fetch_version")',
    '"Unduhan tidak dapat diselesaikan."': 'LocaleText.get(context, "portal.download_failed")',
    '"Installer Android tidak dapat dibuka."': 'LocaleText.get(context, "portal.installer_failed")',
    'text = "PEMBARUAN DIPERLUKAN"': 'text = LocaleText.get(context, "portal.update_required")',
    'text = "Perbarui launcher terlebih dahulu, lalu hubungkan akun Portal Anda."': 'text = LocaleText.get(context, "portal.update_description")',
    '"Membuka installer…"': 'LocaleText.get(context, "portal.opening_installer")',
    '"Mengunduh ${(progress * 100).toInt()}%"': 'LocaleText.get(context, "portal.downloading", "percent" to (progress * 100).toInt())',
    'PortalUpdatePhase.DOWNLOADING -> "Mengunduh…"': 'PortalUpdatePhase.DOWNLOADING -> LocaleText.get(context, "generic.loading")',
    'PortalUpdatePhase.INSTALLING -> "Menyiapkan installer…"': 'PortalUpdatePhase.INSTALLING -> LocaleText.get(context, "portal.preparing_installer")',
    'PortalUpdatePhase.ERROR -> "Coba Lagi"': 'PortalUpdatePhase.ERROR -> LocaleText.get(context, "portal.retry")',
    'PortalUpdatePhase.IDLE -> "Perbarui Launcher"': 'PortalUpdatePhase.IDLE -> LocaleText.get(context, "portal.update_launcher")',
    '"Buka halaman download"': 'LocaleText.get(context, "portal.open_download")',
    'if (updateRequired) "MENUNGGU PEMBARUAN" else "SIAP DIHUBUNGKAN"': 'if (updateRequired) LocaleText.get(context, "portal.waiting_update") else LocaleText.get(context, "portal.ready_connect")',
    'text = "Gunakan akun Portal Anda"': 'text = LocaleText.get(context, "portal.use_account")',
    '"Selesaikan pembaruan launcher untuk melanjutkan koneksi akun."': 'LocaleText.get(context, "portal.complete_update")',
    '"Pilih akun di Portal, setujui koneksi, lalu launcher akan kembali dan masuk secara otomatis."': 'LocaleText.get(context, "portal.connection_description")',
    'title = "Masuk ke Portal"': 'title = LocaleText.get(context, "portal.step_login")',
    'detail = "Gunakan email atau akun Google Anda."': 'detail = LocaleText.get(context, "portal.step_login_detail")',
    'title = "Setujui koneksi"': 'title = LocaleText.get(context, "portal.step_approve")',
    'detail = "Pastikan akun yang aktif sudah benar."': 'detail = LocaleText.get(context, "portal.step_approve_detail")',
    'title = "Kembali otomatis"': 'title = LocaleText.get(context, "portal.step_return")',
    'detail = "Launcher akan memakai akun Portal yang sama."': 'detail = LocaleText.get(context, "portal.step_return_detail")',
    'if (updateRequired) "Perbarui launcher terlebih dahulu" else "Mulai Koneksi Aman"': 'if (updateRequired) LocaleText.get(context, "portal.update_first") else LocaleText.get(context, "portal.start")',
    '"Koneksi tersedia setelah launcher diperbarui"': 'LocaleText.get(context, "portal.available_after_update")',
    '"Sekitar satu menit • tanpa menyalin token"': 'LocaleText.get(context, "portal.one_minute")',
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f'missing PortalConnect text: {old}')
    text = text.replace(old, new, 1)
portal.write_text(text, encoding='utf-8')

# Portal SSO progress and errors.
sso = Path('app/src/main/java/com/drmacze/f16launcher/PortalSsoActivity.kt')
text = sso.read_text(encoding='utf-8')
text = text.replace(
    'class PortalSsoActivity : ComponentActivity() {\n',
    'class PortalSsoActivity : ComponentActivity() {\n    override fun attachBaseContext(newBase: Context) {\n        super.attachBaseContext(LanguageManager.applyLocale(newBase))\n    }\n\n',
    1,
)
text = text.replace(
    '        super.onCreate(savedInstanceState)\n        setContent {',
    '        super.onCreate(savedInstanceState)\n        uiState.value = PortalConnectUiState(\n            stage = PortalConnectStage.PREPARING,\n            title = LocaleText.get(this, "portal.preparing"),\n            message = LocaleText.get(this, "portal.preparing_request"),\n        )\n        setContent {',
    1,
)
sso_replacements = {
    'title = "Lanjutkan di Portal"': 'title = LocaleText.get(this, "portal.continue_in_portal")',
    'message = "Konfirmasi akun yang ingin digunakan di browser."': 'message = LocaleText.get(this, "portal.confirm_account")',
    'detail = "Setelah disetujui, Anda akan kembali ke launcher secara otomatis."': 'detail = LocaleText.get(this, "portal.return_after_approval")',
    'message = "Browser tidak dapat dibuka. Periksa browser default Anda lalu coba lagi."': 'message = LocaleText.get(this, "portal.browser_failed")',
    'showFailure("Sesi koneksi sudah berakhir. Kembali dan mulai koneksi baru.")': 'showFailure(LocaleText.get(this, "portal.session_ended"))',
    'title = "Memverifikasi koneksi"': 'title = LocaleText.get(this, "portal.verify_code")',
    'message = "Launcher sedang memeriksa kode sekali pakai dari Portal."': 'message = LocaleText.get(this, "portal.verify_message")',
    'detail = "Jangan tutup aplikasi sampai verifikasi selesai."': 'detail = LocaleText.get(this, "portal.keep_open")',
    'title = "Menyiapkan akun"': 'title = LocaleText.get(this, "portal.sync_profile")',
    'message = "Profil dan preferensi akun sedang disinkronkan ke launcher."': 'message = LocaleText.get(this, "portal.sync_message")',
    'title = "Akun berhasil terhubung"': 'title = LocaleText.get(this, "portal.connected")',
    'message = "Launcher sekarang menggunakan akun Portal yang sama."': 'message = LocaleText.get(this, "portal.same_account")',
    'detail = "Membuka halaman utama…"': 'detail = LocaleText.get(this, "portal.opening_home")',
    '"Sesi koneksi sudah berakhir. Mulai kembali dari halaman awal launcher."': 'LocaleText.get(this, "portal.session_ended")',
    '"Akun yang dikembalikan Portal tidak cocok. Login kembali dan pilih akun yang benar."': 'LocaleText.get(this, "portal.account_mismatch")',
    '"Koneksi internet terputus saat memverifikasi akun. Periksa jaringan lalu coba lagi."': 'LocaleText.get(this, "portal.network_failed")',
    'else -> "Akun belum dapat dihubungkan. Kembali dan mulai koneksi sekali lagi."': 'else -> LocaleText.get(this, "portal.failed")',
}
for old, new in sso_replacements.items():
    if old in text:
        text = text.replace(old, new)
sso.write_text(text, encoding='utf-8')

# Maintenance chrome follows locale while server-authored title/message remain intact.
maintenance = Path('app/src/main/java/com/drmacze/f16launcher/MaintenanceSystem.kt')
text = maintenance.read_text(encoding='utf-8')
text = text.replace('Text("Coba Lagi", fontWeight = FontWeight.Bold, fontSize = 14.sp)', 'Text(LocaleText.get(context, "maintenance.retry"), fontWeight = FontWeight.Bold, fontSize = 14.sp)')
text = text.replace('Text("Main Offline", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)', 'Text(LocaleText.get(context, "maintenance.offline"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)')
text = text.replace('if (isStaff) "Masuk dengan akses staff" else "Login staff"', 'LocaleText.get(context, "maintenance.staff")')
text = text.replace('"STAFF ACCESS"', 'LocaleText.get(LocalContext.current, "maintenance.staff").uppercase(Locale.getDefault())')
text = text.replace('maintenance.title.ifBlank { "Pemeliharaan layanan" }', 'maintenance.title.ifBlank { LocaleText.get(LocalContext.current, "maintenance.label") }')
maintenance.write_text(text, encoding='utf-8')
