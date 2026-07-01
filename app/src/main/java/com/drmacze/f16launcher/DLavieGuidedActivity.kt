package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val GAME_PACKAGE = "com.ea.gp.fifaworld"
private const val MARKER_PATH = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private const val LOCAL_VERSION_CODE = 1
private const val LOCAL_VERSION_NAME = "v1"
private const val SUPABASE_URL = "https://dlbayuearegnpmgbxgcf.supabase.co"
private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRsYmF5dWVhcmVnbnBtZ2J4Z2NmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYxOTg3NTMsImV4cCI6MjA5MTc3NDc1M30.-3TazMdyoJJ6F8GKmrh2aMMDNK3gFN8NAJMvLB0D0iU"
private const val PREF_AUTH = "dlavie_auth_session"
private const val PREF_TOKEN = "access_token"
private const val PREF_REFRESH = "refresh_token"
private const val PREF_EMAIL = "email"
private const val SHIZUKU_REQUEST = 2026

class DLavieGuidedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DLavieGuidedApp() }
    }
}

private enum class GuidedTab(val label: String, val icon: String) {
    Home("Home", "⌂"), Data("Data", "▣"), Update("Update", "◎"), Me("Me", "♙")
}

private data class AuthSession(val accessToken: String, val refreshToken: String, val email: String)
private data class AuthResult(val session: AuthSession?, val message: String)
private data class BootstrapState(
    val displayName: String = "DLavie Player",
    val role: String = "user",
    val maintenance: Boolean = false,
    val maintenanceMessage: String = "",
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "Belum dicek",
    val updateAvailable: Boolean = false,
    val patchName: String = "",
    val patchUrl: String = "",
    val notices: List<String> = emptyList(),
    val unreadNotifications: Int = 0,
    val loaded: Boolean = false,
    val error: String = ""
)

private data class GuidedUpdateState(
    val localCode: Int = LOCAL_VERSION_CODE,
    val localName: String = LOCAL_VERSION_NAME,
    val latestCode: Int = 0,
    val latestName: String = "Belum dicek",
    val status: String = "LOGIN",
    val message: String = "Login diperlukan untuk menggunakan DLavie.",
    val shizuku: String = "Unknown",
    val marker: String = "Unknown",
    val patchName: String = "",
    val canDownload: Boolean = false,
    val canInstall: Boolean = false,
    val canAllowShizuku: Boolean = false,
    val working: Boolean = false,
    val progress: Float = 0f,
    val progressText: String = "",
    val sizeText: String = "-",
    val speedText: String = "-",
    val etaText: String = "-",
    val releaseNotes: List<String> = emptyList(),
    val knownIssues: List<String> = emptyList()
)

private val GuideGreen = Color(0xFF22E678)
private val GuideCyan = Color(0xFF2ED3F6)
private val GuideRed = Color(0xFFFF5B64)
private val GuideAmber = Color(0xFFFFB84E)
private val GuideWhite = Color(0xFFF4F7F5)
private val GuideMuted = Color(0xFF8E9491)
private val GuideDark = Color(0xFF020403)
private val GuideCard = Color(0xDD101211)
private val GuideBorder = Color(0xFF25302B)
private val GuideFont = FontFamily.SansSerif

@Composable
private fun DLavieGuidedApp() {
    val context = LocalContext.current
    var session by remember { mutableStateOf(loadSession(context)) }
    MaterialTheme(darkColorScheme(background = GuideDark, surface = GuideCard, primary = GuideGreen, secondary = GuideCyan, onBackground = GuideWhite, onSurface = GuideWhite)) {
        Surface(color = GuideDark, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF082719), GuideDark, Color.Black), radius = 980f))) {
                if (session == null) GuidedLoginScreen(onLoggedIn = { session = it }) else GuidedLauncherScreen(session = session!!, onLogout = { clearSession(context); session = null })
            }
        }
    }
}

@Composable
private fun GuidedLoginScreen(onLoggedIn: (AuthSession) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Login wajib. Tidak ada guest mode.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.Center) {
        GuidedPanel {
            Text("DLavie 26", color = GuideWhite, fontSize = 36.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            Text("Login untuk membuka launcher, profile, ticket, komunitas, dan update.", color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it.trim() }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Text(message, color = if (message.startsWith("OK")) GuideGreen else GuideMuted, fontSize = 12.sp, fontFamily = GuideFont)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedSmallAction("Login", GuideGreen, {
                    scope.launch {
                        working = true
                        val result = withContext(Dispatchers.IO) { loginWithPassword(context, email, password) }
                        working = false
                        message = result.message
                        result.session?.let(onLoggedIn)
                    }
                }, enabled = !working && email.isNotBlank() && password.length >= 6, modifier = Modifier.weight(1f))
                GuidedSmallAction("Register", GuideCyan, {
                    scope.launch {
                        working = true
                        val result = withContext(Dispatchers.IO) { registerWithPassword(context, email, password) }
                        working = false
                        message = result.message
                        result.session?.let(onLoggedIn)
                    }
                }, enabled = !working && email.isNotBlank() && password.length >= 6, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GuidedLauncherScreen(session: AuthSession, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(GuidedTab.Home) }
    var faqOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(bottom = 104.dp)) {
            when (tab) {
                GuidedTab.Home -> GuidedHomeScreen(session, openData = { tab = GuidedTab.Data }, openUpdate = { tab = GuidedTab.Update })
                GuidedTab.Data -> GuidedDataScreen(openUpdate = { tab = GuidedTab.Update })
                GuidedTab.Update -> GuidedUpdateScreen(session)
                GuidedTab.Me -> GuidedProfileScreen(session, onLogout)
            }
        }
        GuidedHelpButton(expanded = faqOpen, onClick = { faqOpen = !faqOpen }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 96.dp))
        if (faqOpen) GuidedFaqPanel(onClose = { faqOpen = false }, modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 118.dp))
        GuidedBottomNav(tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun GuidedHomeScreen(session: AuthSession, openData: () -> Unit, openUpdate: () -> Unit) {
    val context = LocalContext.current
    var bootstrap by remember { mutableStateOf(BootstrapState()) }
    LaunchedEffect(session.accessToken) { bootstrap = withContext(Dispatchers.IO) { loadBootstrap(session) } }
    val marker = guidedReadMarkerSmart()
    GuidedPage {
        GuidedHeaderCard(session, bootstrap, marker)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GuidedMiniChip("Game", if (guidedIsGameInstalled(context)) "Installed" else "Missing", "🎮", if (guidedIsGameInstalled(context)) GuideGreen else GuideRed, Modifier.weight(1f))
            GuidedMiniChip("Shizuku", guidedShizukuState(), "🛡", if (guidedShizukuState() == "Ready") GuideGreen else GuideCyan, Modifier.weight(1f))
            GuidedMiniChip("Update", if (bootstrap.updateAvailable) "v${bootstrap.latestVersionCode}" else LOCAL_VERSION_NAME, "🔄", if (bootstrap.updateAvailable) GuideCyan else GuideGreen, Modifier.weight(1f))
        }
        GuidedQuickSteps(marker, bootstrap, openData, openUpdate) { guidedLaunchGame(context) }
        GuidedPrimaryCta(if (marker.startsWith("v26")) "Mainkan Game" else "Install Full Data", if (marker.startsWith("v26")) "Data siap. Buka FIFA 16." else "Base data belum lengkap.", if (marker.startsWith("v26")) "▶" else "⬇", if (marker.startsWith("v26")) { guidedLaunchGame(context) } else openData)
        if (bootstrap.notices.isNotEmpty()) GuidedNoticeCard(bootstrap.notices)
        if (bootstrap.error.isNotBlank()) GuidedErrorCard(bootstrap.error)
    }
}

@Composable
private fun GuidedDataScreen(openUpdate: () -> Unit) {
    val context = LocalContext.current
    val marker = guidedReadMarkerSmart()
    GuidedPage {
        GuidedPageTitle("Data", "Base data wajib siap sebelum update patch.")
        GuidedPanel(border = if (marker.startsWith("v26")) GuideGreen else GuideCyan) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📁", fontSize = 34.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Base Data", color = GuideWhite, fontSize = 25.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                    Text(if (marker.startsWith("v26")) "Full data terdeteksi." else "Full data belum lengkap.", color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont)
                }
                GuidedPill(if (marker.startsWith("v26")) "READY" else "BELUM", if (marker.startsWith("v26")) GuideGreen else GuideRed)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedInfoBox("APK FIFA", if (guidedIsGameInstalled(context)) "Terpasang" else "Belum", Modifier.weight(1f))
                GuidedInfoBox("Marker", guidedShortMarker(marker), Modifier.weight(1f))
            }
            GuidedActionButton(if (marker.startsWith("v26")) "Ke Update" else "Buka Installer Data", if (marker.startsWith("v26")) GuideCyan else GuideGreen, if (marker.startsWith("v26")) openUpdate else { guidedOpenClassicInstaller(context) }, true)
        }
        GuidedShizukuCard()
    }
}

@Composable
private fun GuidedUpdateScreen(session: AuthSession) {
    var bootstrap by remember { mutableStateOf(BootstrapState()) }
    LaunchedEffect(session.accessToken) { bootstrap = withContext(Dispatchers.IO) { loadBootstrap(session) } }
    GuidedPage {
        GuidedPageTitle("Update", "Versi dan patch dibaca dari backend DLavie.")
        GuidedPanel(border = if (bootstrap.updateAvailable) GuideCyan else GuideGreen) {
            Text("Patch Update", color = GuideWhite, fontSize = 25.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            Text(if (bootstrap.updateAvailable) "Update tersedia dari backend." else "Versi kamu sudah terbaru / belum ada release aktif.", color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                GuidedInfoBox("Local", LOCAL_VERSION_NAME, Modifier.weight(1f))
                GuidedInfoBox("Latest", bootstrap.latestVersionName, Modifier.weight(1f))
            }
            if (bootstrap.patchName.isNotBlank()) GuidedInfoBox("Patch ZIP", bootstrap.patchName, Modifier.fillMaxWidth())
            GuidedActionButton(if (bootstrap.updateAvailable) "Download Patch" else "Check Update", if (bootstrap.updateAvailable) GuideCyan else GuideGreen, {}, enabled = false)
            Text("Download/apply patch akan disambungkan setelah login foundation ini build hijau.", color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont)
        }
    }
}

@Composable
private fun GuidedProfileScreen(session: AuthSession, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var ticketMessage by remember { mutableStateOf("") }
    var ticketResult by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    GuidedPage {
        GuidedPageTitle("Profile", "Akun login, ticket, dan status DLavie.")
        GuidedPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(74.dp).background(Brush.linearGradient(listOf(Color(0xFF063B27), Color(0xFF09110F))), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                    Text(session.email.firstOrNull()?.uppercase() ?: "D", color = GuideGreen, fontSize = 31.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(session.email, color = GuideWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont)
                    Text("Login aktif • no guest mode", color = GuideMuted, fontSize = 13.sp, maxLines = 1, fontFamily = GuideFont)
                }
                GuidedPill("USER", GuideGreen)
            }
            GuidedActionButton("Logout", GuideRed, onLogout, true)
        }
        GuidedPanel {
            Text("Support Ticket", color = GuideWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            Text("Ticket hanya bisa dibuat setelah login. Pesan masuk ke backend support.", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont)
            OutlinedTextField(value = ticketMessage, onValueChange = { ticketMessage = it.take(1000) }, label = { Text("Tulis masalah") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            GuidedActionButton("Buat Ticket", GuideGreen, {
                scope.launch {
                    working = true
                    ticketResult = withContext(Dispatchers.IO) { createSupportTicket(session, ticketMessage) }
                    working = false
                    if (ticketResult.startsWith("OK")) ticketMessage = ""
                }
            }, enabled = !working && ticketMessage.isNotBlank())
            if (ticketResult.isNotBlank()) Text(ticketResult, color = if (ticketResult.startsWith("OK")) GuideGreen else GuideRed, fontSize = 12.sp, fontFamily = GuideFont)
        }
        GuidedFaqFullCard()
    }
}

@Composable private fun GuidedPage(content: @Composable () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() } }
@Composable private fun GuidedPanel(border: Color = GuideBorder, content: @Composable () -> Unit) { Surface(modifier = Modifier.fillMaxWidth(), color = GuideCard, shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, border.copy(alpha = 0.85f))) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } } }
@Composable private fun GuidedHeaderCard(session: AuthSession, bootstrap: BootstrapState, marker: String) { GuidedPanel { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(72.dp).background(Brush.linearGradient(listOf(Color(0xFF06462A), Color(0xFF07130F))), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) { Text("DL", color = GuideGreen, fontSize = 29.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text("DLavie 26", color = GuideWhite, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(session.email, color = GuideMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) }; GuidedPill(if (marker.startsWith("v26")) "READY" else if (bootstrap.maintenance) "MAINT" else "SETUP", if (marker.startsWith("v26")) GuideGreen else GuideCyan) } } }
@Composable private fun GuidedQuickSteps(marker: String, bootstrap: BootstrapState, openData: () -> Unit, openUpdate: () -> Unit, launch: () -> Unit) { GuidedPanel { Text("⚡ Langkah Cepat", color = GuideWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont); Text("Ikuti urutan agar patch aktif dengan benar.", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont); GuidedStepRow(1, "Cek Base Data", "Pastikan OBB dan marker siap.", if (marker.startsWith("v26")) "OK" else "WAJIB", "🔎", if (marker.startsWith("v26")) GuideGreen else GuideAmber, openData); GuidedStepRow(2, "Install Full Data", "Unduh dan pasang data utama.", if (marker.startsWith("v26")) "SELESAI" else "LANJUT", "⬇", GuideGreen, openData); GuidedStepRow(3, "Update Patch", "Cek versi dari backend.", if (bootstrap.updateAvailable) "TERSEDIA" else "CEK", "🌐", GuideCyan, openUpdate); GuidedStepRow(4, "Mainkan Game", "Launch setelah data siap.", if (marker.startsWith("v26")) "READY" else "NANTI", "🚀", Color(0xFFB783FF), launch) } }
@Composable private fun GuidedStepRow(no: Int, title: String, subtitle: String, chip: String, icon: String, color: Color, onClick: () -> Unit) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(74.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xBB0C1110), contentColor = GuideWhite), contentPadding = PaddingValues(horizontal = 12.dp)) { Box(Modifier.size(28.dp).background(color, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(no.toString(), color = Color(0xFF001407), fontWeight = FontWeight.Black, fontSize = 13.sp) }; Spacer(Modifier.width(10.dp)); Text(icon, fontSize = 23.sp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(subtitle, color = GuideMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) }; GuidedPill(chip, color); Text("›", color = GuideMuted, fontSize = 20.sp) } }
@Composable private fun GuidedPrimaryCta(title: String, subtitle: String, icon: String, onClick: () -> Unit) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = GuideGreen, contentColor = Color(0xFF001407)), contentPadding = PaddingValues(horizontal = 18.dp)) { Text(icon, fontSize = 23.sp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) }; Text("→", fontSize = 24.sp, fontWeight = FontWeight.Black) } }
@Composable private fun GuidedMiniChip(title: String, value: String, icon: String, color: Color, modifier: Modifier) { Surface(modifier = modifier.height(74.dp), color = Color(0xCC101211), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) { Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.Center) { Text(icon, fontSize = 18.sp); Text(title, color = GuideMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1); Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun GuidedInfoBox(label: String, value: String, modifier: Modifier) { Surface(modifier = modifier.height(78.dp), color = Color(0xAA0A0D0C), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) { Text(label, color = GuideMuted, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(value, color = GuideWhite, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) } } }
@Composable private fun GuidedActionButton(label: String, color: Color, onClick: () -> Unit, enabled: Boolean = true) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(58.dp), enabled = enabled, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF001407), disabledContainerColor = Color(0xFF333635), disabledContentColor = GuideMuted)) { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun GuidedSmallAction(label: String, color: Color, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) { Button(onClick = onClick, modifier = modifier.height(48.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF001407), disabledContainerColor = Color(0xFF333635), disabledContentColor = GuideMuted), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = GuideFont) } }
@Composable private fun GuidedPill(text: String, color: Color) { Surface(color = color.copy(alpha = 0.16f), border = BorderStroke(1.dp, color.copy(alpha = 0.58f)), shape = RoundedCornerShape(999.dp)) { Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), maxLines = 1, fontFamily = GuideFont) } }
@Composable private fun GuidedPageTitle(title: String, subtitle: String) { Column { Text(title, color = GuideWhite, fontSize = 34.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont); Text(subtitle, color = GuideMuted, fontSize = 14.sp, maxLines = 2, fontFamily = GuideFont) } }
@Composable private fun GuidedNoticeCard(notices: List<String>) { GuidedPanel(border = GuideCyan) { Text("Developer Notice", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black); notices.take(3).forEach { Text("• $it", color = GuideMuted, fontSize = 13.sp) } } }
@Composable private fun GuidedErrorCard(error: String) { GuidedPanel(border = GuideRed) { Text("Backend Error", color = GuideRed, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(error, color = GuideMuted, fontSize = 12.sp) } }
@Composable private fun GuidedShizukuCard() { val status = guidedShizukuState(); GuidedPanel { Text("🛡 Shizuku Access", color = GuideWhite, fontSize = 23.sp, fontWeight = FontWeight.Black); Text(if (status == "Ready") "Siap untuk apply patch otomatis." else "Buka Shizuku, aktifkan Start, lalu izinkan DLavie.", color = GuideMuted, fontSize = 13.sp); GuidedActionButton(if (status == "Ready") "Shizuku Ready" else "Izinkan / Cek Shizuku", if (status == "Ready") GuideGreen else GuideCyan, { guidedRequestShizuku() }, status != "Ready") } }
@Composable private fun GuidedFaqFullCard() { GuidedPanel { Text("Topik Bantuan", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont); GuidedFaqLine("🛡 Cara aktifkan Shizuku", "Buka Shizuku → Pairing/Start → kembali ke DLavie → Izinkan akses."); GuidedFaqLine("📁 Apa itu Base Data?", "OBB dan data utama FIFA 16. Patch tidak bekerja kalau base belum lengkap."); GuidedFaqLine("🌐 Cara cek versi", "Buka Update. Local dan Latest dibandingkan dari backend.") } }
@Composable private fun GuidedFaqLine(title: String, body: String) { Surface(color = Color(0xAA0B100F), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, GuideBorder)) { Column(Modifier.padding(12.dp)) { Text(title, color = GuideWhite, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont); Text(body, color = GuideMuted, fontSize = 12.sp, fontFamily = GuideFont) } } }
@Composable private fun GuidedHelpButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier) { Row(modifier, verticalAlignment = Alignment.CenterVertically) { if (!expanded) Surface(color = Color(0xEE101211), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, GuideBorder)) { Text("Butuh bantuan?", color = GuideWhite, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), fontSize = 12.sp, fontFamily = GuideFont) }; Spacer(Modifier.width(8.dp)); Button(onClick = onClick, modifier = Modifier.size(68.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = GuideGreen, contentColor = Color(0xFF001407)), contentPadding = PaddingValues(0.dp)) { Text(if (expanded) "×" else "🤖", fontSize = if (expanded) 28.sp else 25.sp, fontWeight = FontWeight.Black) } } }
@Composable private fun GuidedFaqPanel(onClose: () -> Unit, modifier: Modifier) { Surface(modifier = modifier.fillMaxWidth(), color = Color(0xF00D0F0E), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, GuideBorder), shadowElevation = 20.dp) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Asisten DLavie", color = GuideWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), fontFamily = GuideFont); GuidedSmallAction("Tutup", GuideRed, onClose, true, Modifier.width(86.dp)) }; GuidedFaqFullCard() } } }
@Composable private fun GuidedBottomNav(selected: GuidedTab, onSelect: (GuidedTab) -> Unit, modifier: Modifier) { Surface(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color(0xF00B0C0C), shape = RoundedCornerShape(34.dp), border = BorderStroke(1.dp, GuideBorder), shadowElevation = 18.dp) { Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { GuidedTab.values().forEach { item -> val active = item == selected; Button(onClick = { onSelect(item) }, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF0E3A22) else Color.Transparent, contentColor = if (active) GuideGreen else GuideMuted), contentPadding = PaddingValues(0.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(item.icon, fontSize = 19.sp); Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, fontFamily = GuideFont) } } } } } }

private fun loadSession(context: Context): AuthSession? { val p = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE); val token = p.getString(PREF_TOKEN, null) ?: return null; val refresh = p.getString(PREF_REFRESH, "") ?: ""; val email = p.getString(PREF_EMAIL, "") ?: ""; return AuthSession(token, refresh, email) }
private fun saveSession(context: Context, s: AuthSession) { context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE).edit().putString(PREF_TOKEN, s.accessToken).putString(PREF_REFRESH, s.refreshToken).putString(PREF_EMAIL, s.email).apply() }
private fun clearSession(context: Context) { context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE).edit().clear().apply() }
private fun loginWithPassword(context: Context, email: String, password: String): AuthResult = authPassword(context, "/auth/v1/token?grant_type=password", email, password, "OK: login berhasil.")
private fun registerWithPassword(context: Context, email: String, password: String): AuthResult = authPassword(context, "/auth/v1/signup", email, password, "OK: akun dibuat.")
private fun authPassword(context: Context, path: String, email: String, password: String, okMessage: String): AuthResult = try { val json = httpPost(path, null, JSONObject().put("email", email).put("password", password)); val token = json.optString("access_token", ""); val refresh = json.optString("refresh_token", ""); val userEmail = json.optJSONObject("user")?.optString("email", email) ?: email; if (token.isBlank()) AuthResult(null, "Akun dibuat. Cek email lalu login.") else { val session = AuthSession(token, refresh, userEmail); saveSession(context, session); runCatching { httpPost("/rest/v1/rpc/dlavie_v2_create_profile_if_missing", session.accessToken, JSONObject().put("p_display_name", "DLavie Player")) }; AuthResult(session, okMessage) } } catch (e: Exception) { AuthResult(null, "Error: ${e.message ?: "auth gagal"}") }
private fun loadBootstrap(session: AuthSession): BootstrapState = try { val json = httpPost("/rest/v1/rpc/dlavie_v2_get_launcher_bootstrap", session.accessToken, JSONObject().put("p_local_version_code", LOCAL_VERSION_CODE)); parseBootstrap(json) } catch (e: Exception) { BootstrapState(loaded = false, error = e.message ?: "backend gagal") }
private fun parseBootstrap(json: JSONObject): BootstrapState { val profile = json.optJSONObject("profile") ?: JSONObject(); val update = json.optJSONObject("update") ?: JSONObject(); val patch = update.optJSONObject("patch"); val maintenance = json.optJSONObject("maintenance") ?: JSONObject(); val notices = jsonArrayObjectsToTitles(json.optJSONArray("notices")); return BootstrapState(displayName = profile.optString("display_name", "DLavie Player"), role = profile.optString("role", "user"), maintenance = maintenance.optBoolean("enabled", false), maintenanceMessage = maintenance.optString("message", ""), latestVersionCode = update.optInt("latestVersionCode", 0), latestVersionName = update.optString("latestVersionName", "Belum dicek"), updateAvailable = update.optBoolean("updateAvailable", false), patchName = patch?.optString("name", "") ?: "", patchUrl = patch?.optString("url", "") ?: "", notices = notices, unreadNotifications = json.optInt("unreadNotifications", 0), loaded = true) }
private fun createSupportTicket(session: AuthSession, message: String): String = try { val marker = guidedReadMarkerSmart(); val json = httpPost("/rest/v1/rpc/dlavie_v2_create_ticket", session.accessToken, JSONObject().put("p_title", "DLavie Support").put("p_category", "general").put("p_message", message).put("p_app_version", "0.19.0-login-foundation").put("p_local_version", LOCAL_VERSION_NAME).put("p_latest_version", "").put("p_data_marker", marker).put("p_shizuku_status", guidedShizukuState())); "OK: ticket dibuat #${json.optString("public_code", json.optString("id", ""))}" } catch (e: Exception) { "Error: ${e.message ?: "ticket gagal"}" }
private fun httpPost(path: String, token: String?, body: JSONObject): JSONObject { val conn = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection); conn.requestMethod = "POST"; conn.doOutput = true; conn.setRequestProperty("apikey", SUPABASE_KEY); conn.setRequestProperty("Content-Type", "application/json"); conn.setRequestProperty("Accept", "application/json"); if (!token.isNullOrBlank()) conn.setRequestProperty("Authorization", "Bearer $token"); conn.outputStream.use { it.write(body.toString().toByteArray()) }; val code = conn.responseCode; val stream = if (code in 200..299) conn.inputStream else conn.errorStream; val text = stream?.bufferedReader()?.readText().orEmpty(); conn.disconnect(); if (code !in 200..299) throw IllegalStateException(parseError(text)); return if (text.isBlank()) JSONObject() else JSONObject(text) }
private fun parseError(text: String): String = runCatching { JSONObject(text).optString("message", text.take(160)) }.getOrElse { text.take(160).ifBlank { "request gagal" } }
private fun jsonArrayObjectsToTitles(arr: JSONArray?): List<String> { if (arr == null) return emptyList(); val out = mutableListOf<String>(); for (i in 0 until arr.length()) { val o = arr.optJSONObject(i); if (o != null) out += (o.optString("title", "Notice") + ": " + o.optString("body", "")) }; return out }
private fun guidedShizukuState(): String = try { when { !Shizuku.pingBinder() -> "Start"; Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> "Ready"; else -> "Permission" } } catch (_: Exception) { "Start" }
private fun guidedRequestShizuku() { runCatching { if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(SHIZUKU_REQUEST) } }
private fun guidedIsGameInstalled(context: Context): Boolean = try { context.packageManager.getPackageInfo(GAME_PACKAGE, 0); true } catch (_: Exception) { false }
private fun guidedLaunchGame(context: Context) { context.packageManager.getLaunchIntentForPackage(GAME_PACKAGE)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
private fun guidedOpenClassicInstaller(context: Context) { runCatching { context.startActivity(Intent(context, DLavieHubActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
private fun guidedReadMarkerSmart(): String = runCatching { File(MARKER_PATH).readText().trim() }.getOrElse { "No marker" }
private fun guidedShortMarker(marker: String): String = if (marker.length > 12) marker.take(12) else marker
private suspend fun guidedDownloadPatch(context: Context, onProgress: (GuidedUpdateState) -> Unit): GuidedUpdateState { val s = GuidedUpdateState(message = "Download patch akan masuk setelah login foundation hijau."); onProgress(s); return s }
private suspend fun guidedInstallPatch(context: Context): GuidedUpdateState = GuidedUpdateState(message = "Apply patch akan masuk setelah login foundation hijau.")
