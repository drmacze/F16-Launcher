package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.math.max

private const val GUIDE_GAME_PACKAGE = "com.ea.gp.fifaworld"
private const val GUIDE_PREFS = "dlavie_update_state"
private const val GUIDE_PREF_CODE = "installed_update_code"
private const val GUIDE_PREF_NAME = "installed_update_name"
private const val GUIDE_DEFAULT_CODE = 1
private const val GUIDE_DEFAULT_NAME = "v1"
private const val GUIDE_MARKER = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private const val GUIDE_SHIZUKU_REQUEST = 2026

class DLavieGuidedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DLavieGuidedApp() }
    }
}

private enum class GuidedTab(val label: String, val icon: GuidedIcon) {
    Home("Home", GuidedIcon.Home),
    Data("Data", GuidedIcon.Folder),
    Update("Update", GuidedIcon.Globe),
    Me("Me", GuidedIcon.User)
}

private enum class GuidedIcon { Home, Folder, Globe, User, Game, Shield, Download, Play, Help, Bot, Wrench, Check, Alert, Rocket }

private data class GuidedUpdateState(
    val localCode: Int = GUIDE_DEFAULT_CODE,
    val localName: String = GUIDE_DEFAULT_NAME,
    val latestCode: Int = 0,
    val latestName: String = "Belum dicek",
    val status: String = "CEK",
    val message: String = "Cek update untuk melihat patch terbaru.",
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

@Composable
private fun DLavieGuidedApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = GuideDark,
            surface = GuideCard,
            primary = GuideGreen,
            secondary = GuideCyan,
            onPrimary = Color(0xFF001407),
            onSecondary = Color(0xFF001018),
            onBackground = GuideWhite,
            onSurface = GuideWhite
        )
    ) {
        var tab by remember { mutableStateOf(GuidedTab.Home) }
        var faqOpen by remember { mutableStateOf(false) }
        Surface(color = GuideDark, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xFF092719), GuideDark, Color.Black), radius = 980f))
            ) {
                Box(Modifier.fillMaxSize().padding(bottom = 104.dp)) {
                    when (tab) {
                        GuidedTab.Home -> GuidedHomeScreen(openData = { tab = GuidedTab.Data }, openUpdate = { tab = GuidedTab.Update })
                        GuidedTab.Data -> GuidedDataScreen(openUpdate = { tab = GuidedTab.Update })
                        GuidedTab.Update -> GuidedUpdateScreen()
                        GuidedTab.Me -> GuidedHelpProfileScreen()
                    }
                }
                GuidedFaqButton(
                    expanded = faqOpen,
                    onClick = { faqOpen = !faqOpen },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 94.dp)
                )
                if (faqOpen) {
                    GuidedFaqPanel(
                        onClose = { faqOpen = false },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 118.dp)
                    )
                }
                GuidedBottomNav(tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun GuidedHomeScreen(openData: () -> Unit, openUpdate: () -> Unit) {
    val context = LocalContext.current
    val installed = remember { guidedIsGameInstalled(context) }
    var update by remember { mutableStateOf(guidedInitialUpdate(context)) }
    LaunchedEffect(Unit) { update = withContext(Dispatchers.IO) { guidedCheckUpdate(context) } }

    GuidedPage {
        GuidedHeaderCard(update = update, installed = installed)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GuidedMiniChip("Game", if (installed) "Installed" else "Missing", GuidedIcon.Game, if (installed) GuideGreen else GuideRed, Modifier.weight(1f))
            GuidedMiniChip("Shizuku", update.shizuku, GuidedIcon.Shield, if (update.shizuku == "Ready") GuideGreen else GuideCyan, Modifier.weight(1f))
            GuidedMiniChip("Patch", if (update.latestCode > update.localCode) "v${update.latestCode}" else update.localName, GuidedIcon.Globe, GuideCyan, Modifier.weight(1f))
        }
        GuidedQuickSteps(openData = openData, openUpdate = openUpdate, update = update)
        GuidedPrimaryCta(
            title = if (update.marker.startsWith("v26")) "Mainkan Game" else "Lanjutkan Instalasi",
            subtitle = if (update.marker.startsWith("v26")) "Data siap, buka FIFA 16." else "Install Full Data dulu.",
            icon = if (update.marker.startsWith("v26")) GuidedIcon.Play else GuidedIcon.Download,
            onClick = { if (update.marker.startsWith("v26")) guidedLaunchGame(context) else openData() }
        )
    }
}

@Composable
private fun GuidedDataScreen(openUpdate: () -> Unit) {
    val context = LocalContext.current
    val installed = remember { guidedIsGameInstalled(context) }
    GuidedPage {
        GuidedPageTitle("Data", "Pastikan file utama siap sebelum update patch.")
        GuidedBaseSummaryCard(installed = installed, openUpdate = openUpdate)
        DLavieBaseInstallCard(isGameInstalled = installed)
        GuidedShizukuGuideCard()
    }
}

@Composable
private fun GuidedUpdateScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(guidedInitialUpdate(context)) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { state = withContext(Dispatchers.IO) { guidedCheckUpdate(context) } }

    GuidedPage {
        GuidedPageTitle("Update", "Download patch lalu apply agar game berubah.")
        GuidedPatchCard(
            state = state,
            onCheck = {
                scope.launch {
                    state = state.copy(working = true, message = "Mengecek update...", progressText = "Checking")
                    state = withContext(Dispatchers.IO) { guidedCheckUpdate(context) }
                }
            },
            onDownload = {
                scope.launch {
                    state = state.copy(working = true, message = "Menyiapkan download...", progress = 0f, progressText = "Starting")
                    state = guidedDownloadPatch(context) { progress -> state = progress }
                }
            },
            onInstall = {
                scope.launch {
                    state = state.copy(working = true, message = "Apply patch...", progress = 0.06f, progressText = "Applying")
                    state = withContext(Dispatchers.IO) { guidedInstallPatch(context) }
                }
            },
            onAllow = {
                scope.launch {
                    guidedRequestShizuku()
                    delay(800)
                    state = withContext(Dispatchers.IO) { guidedCheckUpdate(context) }
                }
            }
        )
        GuidedCorrectOrderCard()
        GuidedReleaseNotesCard(state.releaseNotes, state.knownIssues)
    }
}

@Composable
private fun GuidedHelpProfileScreen() {
    GuidedPage {
        GuidedPageTitle("Bantuan Cepat", "Panduan lengkap tanpa bikin bingung.")
        GuidedFaqFullCard()
        GuidedCorrectOrderCard()
    }
}

@Composable
private fun GuidedPage(content: @Composable Column.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
private fun GuidedHeaderCard(update: GuidedUpdateState, installed: Boolean) {
    GuidedPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(76.dp).background(Brush.linearGradient(listOf(Color(0xFF063B27), Color(0xFF09110F))), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) { Text("DL", color = GuideGreen, fontSize = 30.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("DLavie 26", color = GuideWhite, fontSize = 31.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1)
                Text("FIFA 16 Mobile 2026", color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont, maxLines = 1)
            }
            GuidedPill(if (installed && update.marker.startsWith("v26")) "READY" else "SETUP", if (installed && update.marker.startsWith("v26")) GuideGreen else GuideCyan)
        }
    }
}

@Composable
private fun GuidedQuickSteps(openData: () -> Unit, openUpdate: () -> Unit, update: GuidedUpdateState) {
    GuidedPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuidedIconMark(GuidedIcon.Rocket, GuideCyan, Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Langkah Cepat", color = GuideWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                Text("Ikuti urutan agar patch aktif dengan benar.", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont)
            }
        }
        Spacer(Modifier.height(14.dp))
        GuidedStepRow(1, "Cek Base Data", "Pastikan data utama siap.", "WAJIB", GuidedIcon.Help, GuideCyan, openData)
        GuidedStepRow(2, "Install Full Data", "Unduh dan pasang data utama.", if (update.marker.startsWith("v26")) "SELESAI" else "BERIKUTNYA", GuidedIcon.Download, GuideGreen, openData)
        GuidedStepRow(3, "Cek Update Patch", "Cek versi dan patch terbaru.", if (update.latestCode > update.localCode) "TERSEDIA" else "CEK", GuidedIcon.Globe, GuideCyan, openUpdate)
        GuidedStepRow(4, "Mainkan Game", "Launch setelah data siap.", if (update.marker.startsWith("v26")) "READY" else "NANTI", GuidedIcon.Rocket, Color(0xFFB783FF), { })
    }
}

@Composable
private fun GuidedStepRow(number: Int, title: String, subtitle: String, chip: String, icon: GuidedIcon, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(76.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B0F0E), contentColor = GuideWhite),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        GuidedNumber(number, color)
        Spacer(Modifier.width(10.dp))
        GuidedIconTile(icon, color, 44)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, color = GuideWhite, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1)
            Text(subtitle, color = GuideMuted, fontSize = 11.sp, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GuidedPill(chip, color)
    }
}

@Composable
private fun GuidedBaseSummaryCard(installed: Boolean, openUpdate: () -> Unit) {
    val marker = remember { guidedReadMarkerSmart() }
    val ready = marker.startsWith("v26")
    GuidedPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuidedNumber(1, GuideGreen)
            Spacer(Modifier.width(10.dp))
            GuidedIconTile(GuidedIcon.Folder, GuideGreen, 58)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Base Data", color = GuideWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                Text(if (ready) "Data utama sudah siap." else "Install data utama dulu.", color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont, maxLines = 2)
            }
            GuidedPill(if (ready) "READY" else "BELUM", if (ready) GuideGreen else GuideRed)
        }
        Spacer(Modifier.height(12.dp))
        GuidedStatusLine("APK FIFA", if (installed) "Terpasang" else "Belum ada", installed)
        GuidedStatusLine("Marker", if (ready) marker else "Tidak ditemukan", ready)
        Spacer(Modifier.height(12.dp))
        GuidedActionButton("Lanjut ke Patch Update", GuideCyan, openUpdate, enabled = ready)
    }
}

@Composable
private fun GuidedPatchCard(state: GuidedUpdateState, onCheck: () -> Unit, onDownload: () -> Unit, onInstall: () -> Unit, onAllow: () -> Unit) {
    val color = when (state.status) {
        "VERIFIED", "READY", "INSTALLED" -> GuideGreen
        "UPDATE", "TERSEDIA" -> GuideCyan
        "FAILED", "SHIZUKU" -> GuideRed
        else -> GuideCyan
    }
    GuidedPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuidedNumber(2, GuideCyan)
            Spacer(Modifier.width(10.dp))
            GuidedIconTile(GuidedIcon.Globe, color, 58)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Patch Update", color = GuideWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                Text(state.message, color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            GuidedPill(if (state.working) "WAIT" else state.status, color)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GuidedInfoBox("Lokal", state.localName, Modifier.weight(1f))
            GuidedInfoBox("Terbaru", state.latestName, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GuidedInfoBox("Shizuku", state.shizuku, Modifier.weight(1f))
            GuidedInfoBox("Data", state.marker, Modifier.weight(1f))
        }
        if (state.patchName.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            GuidedInfoBox("Patch", state.patchName, Modifier.fillMaxWidth())
        }
        if (state.progressText.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            GuidedProgress(state.progressText, state.progress, state.sizeText, state.speedText, state.etaText)
        }
        Spacer(Modifier.height(14.dp))
        GuidedActionButton("Check Update", GuideCyan, onCheck, !state.working)
        if (state.canAllowShizuku) {
            Spacer(Modifier.height(10.dp)); GuidedActionButton("Izinkan Shizuku", GuideGreen, onAllow, !state.working)
        }
        if (state.canDownload) {
            Spacer(Modifier.height(10.dp)); GuidedActionButton("Download Patch", GuideCyan, onDownload, !state.working)
        }
        if (state.canInstall) {
            Spacer(Modifier.height(10.dp)); GuidedActionButton("Apply Patch", GuideGreen, onInstall, !state.working)
        }
    }
}

@Composable
private fun GuidedShizukuGuideCard() {
    GuidedPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuidedNumber(3, GuideCyan)
            Spacer(Modifier.width(10.dp))
            GuidedIconTile(GuidedIcon.Shield, GuideCyan, 58)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Shizuku & Apply", color = GuideWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                Text("Dibutuhkan untuk menulis ke Android/data.", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont)
            }
            GuidedPill(guidedShizukuState(), if (guidedShizukuState() == "Ready") GuideGreen else GuideCyan)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            GuidedSmallStep("1", "Buka\nShizuku", Modifier.weight(1f))
            GuidedSmallStep("2", "Pair /\nStart", Modifier.weight(1f))
            GuidedSmallStep("3", "Izinkan\nakses", Modifier.weight(1f))
        }
    }
}

@Composable
private fun GuidedFaqButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        if (!expanded) {
            Surface(color = Color(0xEE101111), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, GuideBorder)) {
                Text("Butuh bantuan?", color = GuideWhite, fontSize = 11.sp, fontFamily = GuideFont, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Spacer(Modifier.height(6.dp))
        }
        Button(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GuideGreen, contentColor = Color(0xFF001407)),
            contentPadding = PaddingValues(0.dp)
        ) { GuidedIconMark(if (expanded) GuidedIcon.Check else GuidedIcon.Bot, Color(0xFF001407), Modifier.size(34.dp)) }
    }
}

@Composable
private fun GuidedFaqPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = Color(0xF40B0F0E), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, GuideBorder), shadowElevation = 20.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GuidedIconTile(GuidedIcon.Bot, GuideGreen, 46)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Asisten DLavie", color = GuideWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
                    Text("Online • FAQ dan panduan update", color = GuideGreen, fontSize = 12.sp, fontFamily = GuideFont)
                }
                GuidedActionTiny("Tutup", onClose)
            }
            Text("Halo! Saya bantu panduan update, Shizuku, base data, dan patch.", color = GuideWhite, fontSize = 14.sp, fontFamily = GuideFont)
            GuidedFaqChip("Cara aktifkan Shizuku")
            GuidedFaqChip("Apa itu Base Data?")
            GuidedFaqChip("Patch belum masuk ke game")
            GuidedFaqChip("Download gagal / retry")
            GuidedFaqChip("Cara cek versi lama / baru")
        }
    }
}

@Composable
private fun GuidedFaqFullCard() {
    GuidedPanel {
        Text("Topik Populer", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
        Spacer(Modifier.height(10.dp))
        GuidedFaqChip("Cara aktifkan Shizuku")
        GuidedFaqChip("Download gagal / retry")
        GuidedFaqChip("Apa itu Base Data?")
        GuidedFaqChip("Patch belum masuk ke game")
        GuidedFaqChip("Cara cek versi lama / baru")
    }
}

@Composable
private fun GuidedCorrectOrderCard() {
    GuidedPanel {
        Text("Urutan yang benar", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
        Spacer(Modifier.height(10.dp))
        GuidedOrderLine(1, "Install APK FIFA")
        GuidedOrderLine(2, "Install Full Data")
        GuidedOrderLine(3, "Check Update")
        GuidedOrderLine(4, "Download & Verify SHA")
        GuidedOrderLine(5, "Apply Patch")
        GuidedOrderLine(6, "Launch Game")
    }
}

@Composable
private fun GuidedReleaseNotesCard(notes: List<String>, issues: List<String>) {
    if (notes.isEmpty() && issues.isEmpty()) return
    GuidedPanel {
        Text("Release Notes", color = GuideWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
        notes.take(4).forEach { Text("• $it", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont) }
        if (issues.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Known Issues", color = GuideWhite, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            issues.take(3).forEach { Text("• $it", color = GuideMuted, fontSize = 13.sp, fontFamily = GuideFont) }
        }
    }
}

@Composable
private fun GuidedBottomNav(selected: GuidedTab, onSelect: (GuidedTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 680.dp).padding(horizontal = 16.dp, vertical = 12.dp), color = Color(0xF00B0C0C), shape = RoundedCornerShape(34.dp), border = BorderStroke(1.dp, GuideBorder), shadowElevation = 18.dp) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GuidedTab.values().forEach { item ->
                val active = item == selected
                Button(
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF0E3A22) else Color.Transparent, contentColor = if (active) GuideGreen else GuideMuted),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GuidedIconMark(item.icon, if (active) GuideGreen else GuideMuted, Modifier.size(if (active) 22.dp else 19.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(item.label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Bold, maxLines = 1, fontFamily = GuideFont)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedPanel(content: @Composable Column.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xDD101111), shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, GuideBorder)) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun GuidedPrimaryCta(title: String, subtitle: String, icon: GuidedIcon, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(22.dp), colors = ButtonDefaults.buttonColors(containerColor = GuideGreen, contentColor = Color(0xFF001407)), contentPadding = PaddingValues(horizontal = 18.dp)) {
        GuidedIconMark(icon, Color(0xFF001407), Modifier.size(26.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1)
            Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, maxLines = 1)
        }
        Text("→", fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GuidedActionButton(label: String, color: Color, onClick: () -> Unit, enabled: Boolean) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF001407)), contentPadding = PaddingValues(0.dp), enabled = enabled) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
    }
}

@Composable
private fun GuidedActionTiny(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.height(34.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111816), contentColor = GuideMuted), contentPadding = PaddingValues(horizontal = 10.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont)
    }
}

@Composable
private fun GuidedMiniChip(title: String, value: String, icon: GuidedIcon, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(70.dp), color = Color(0xFF0B0F0E), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            GuidedIconMark(icon, color, Modifier.size(23.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = GuideMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, maxLines = 1)
                Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun GuidedInfoBox(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0xFF0B0F0E), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = GuideMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, maxLines = 1)
            Text(value, color = GuideWhite, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GuidedStatusLine(label: String, value: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        GuidedIconMark(if (ok) GuidedIcon.Check else GuidedIcon.Alert, if (ok) GuideGreen else GuideRed, Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = GuideMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, modifier = Modifier.weight(1f))
        Text(value, color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GuidedSmallStep(number: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(76.dp), color = Color(0xFF0B0F0E), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GuidedNumber(number.toInt(), GuideGreen)
            Spacer(Modifier.height(4.dp))
            Text(label, color = GuideWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, maxLines = 2)
        }
    }
}

@Composable
private fun GuidedFaqChip(label: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF101716), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GuidedIconMark(GuidedIcon.Help, GuideCyan, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, modifier = Modifier.weight(1f))
            Text("›", color = GuideMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun GuidedOrderLine(number: Int, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        GuidedNumber(number, GuideGreen)
        Spacer(Modifier.width(10.dp))
        Text(label, color = GuideWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = GuideFont, modifier = Modifier.weight(1f))
        Text("›", color = GuideMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GuidedProgress(title: String, progress: Float, sizeText: String, speedText: String, etaText: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF0B0F0E), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, GuideBorder)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(title, color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", color = GuideWhite, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(9.dp).background(Color(0xFF17201C), RoundedCornerShape(99.dp))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0.02f, 1f)).height(9.dp).background(GuideCyan, RoundedCornerShape(99.dp)))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(sizeText, color = GuideMuted, fontSize = 11.sp, fontFamily = GuideFont, maxLines = 1, modifier = Modifier.weight(1f))
                Text(speedText, color = GuideMuted, fontSize = 11.sp, fontFamily = GuideFont, maxLines = 1)
                Text("ETA $etaText", color = GuideMuted, fontSize = 11.sp, fontFamily = GuideFont, maxLines = 1)
            }
        }
    }
}

@Composable
private fun GuidedNumber(number: Int, color: Color) {
    Box(Modifier.size(27.dp).background(color, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Text(number.toString(), color = Color(0xFF001407), fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont)
    }
}

@Composable
private fun GuidedIconTile(icon: GuidedIcon, color: Color, size: Int) {
    Box(Modifier.size(size.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape((size / 3).dp)), contentAlignment = Alignment.Center) {
        GuidedIconMark(icon, color, Modifier.size((size * 0.48f).dp))
    }
}

@Composable
private fun GuidedPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.16f), border = BorderStroke(1.dp, color.copy(alpha = 0.55f)), shape = RoundedCornerShape(999.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1, fontFamily = GuideFont)
    }
}

@Composable
private fun GuidedPageTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = GuideWhite, fontSize = 38.sp, fontWeight = FontWeight.Black, fontFamily = GuideFont, maxLines = 1)
        Text(subtitle, color = GuideMuted, fontSize = 14.sp, fontFamily = GuideFont, maxLines = 2)
    }
}

@Composable
private fun GuidedIconMark(type: GuidedIcon, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.09f
        fun p(x: Float, y: Float) = Offset(s * x, s * y)
        when (type) {
            GuidedIcon.Home -> {
                drawLine(tint, p(0.16f, 0.50f), p(0.50f, 0.20f), w, cap = StrokeCap.Round); drawLine(tint, p(0.50f, 0.20f), p(0.84f, 0.50f), w, cap = StrokeCap.Round); drawLine(tint, p(0.28f, 0.46f), p(0.28f, 0.82f), w, cap = StrokeCap.Round); drawLine(tint, p(0.72f, 0.46f), p(0.72f, 0.82f), w, cap = StrokeCap.Round); drawLine(tint, p(0.28f, 0.82f), p(0.72f, 0.82f), w, cap = StrokeCap.Round)
            }
            GuidedIcon.Folder -> {
                drawLine(tint, p(0.18f, 0.36f), p(0.42f, 0.36f), w, cap = StrokeCap.Round); drawLine(tint, p(0.42f, 0.36f), p(0.52f, 0.46f), w, cap = StrokeCap.Round); drawLine(tint, p(0.18f, 0.46f), p(0.82f, 0.46f), w, cap = StrokeCap.Round); drawLine(tint, p(0.18f, 0.46f), p(0.18f, 0.78f), w, cap = StrokeCap.Round); drawLine(tint, p(0.82f, 0.46f), p(0.82f, 0.78f), w, cap = StrokeCap.Round); drawLine(tint, p(0.18f, 0.78f), p(0.82f, 0.78f), w, cap = StrokeCap.Round)
            }
            GuidedIcon.Globe -> {
                drawCircle(tint, s * 0.32f, p(0.50f, 0.50f), style = Stroke(w)); drawLine(tint, p(0.18f, 0.50f), p(0.82f, 0.50f), w, cap = StrokeCap.Round); drawArc(tint, 80f, 200f, false, p(0.32f, 0.18f), Size(s * 0.36f, s * 0.64f), style = Stroke(w, cap = StrokeCap.Round)); drawArc(tint, -100f, 200f, false, p(0.32f, 0.18f), Size(s * 0.36f, s * 0.64f), style = Stroke(w, cap = StrokeCap.Round))
            }
            GuidedIcon.User -> {
                drawCircle(tint, s * 0.15f, p(0.50f, 0.34f), style = Stroke(w)); drawArc(tint, 200f, 140f, false, p(0.25f, 0.52f), Size(s * 0.50f, s * 0.42f), style = Stroke(w, cap = StrokeCap.Round))
            }
            GuidedIcon.Game -> {
                drawRoundRect(tint, topLeft = p(0.18f, 0.36f), size = Size(s * 0.64f, s * 0.34f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f), style = Stroke(w)); drawCircle(tint, s * 0.03f, p(0.34f, 0.53f)); drawLine(tint, p(0.29f, 0.53f), p(0.39f, 0.53f), w, cap = StrokeCap.Round); drawCircle(tint, s * 0.03f, p(0.65f, 0.49f)); drawCircle(tint, s * 0.03f, p(0.73f, 0.57f))
            }
            GuidedIcon.Shield -> { drawCircle(tint, s * 0.32f, p(0.50f, 0.50f), style = Stroke(w)); drawLine(tint, p(0.50f, 0.25f), p(0.50f, 0.75f), w, cap = StrokeCap.Round); drawLine(tint, p(0.25f, 0.50f), p(0.75f, 0.50f), w, cap = StrokeCap.Round) }
            GuidedIcon.Download -> { drawLine(tint, p(0.50f, 0.20f), p(0.50f, 0.62f), w, cap = StrokeCap.Round); drawLine(tint, p(0.32f, 0.46f), p(0.50f, 0.64f), w, cap = StrokeCap.Round); drawLine(tint, p(0.68f, 0.46f), p(0.50f, 0.64f), w, cap = StrokeCap.Round); drawLine(tint, p(0.25f, 0.80f), p(0.75f, 0.80f), w, cap = StrokeCap.Round) }
            GuidedIcon.Play -> { val path = Path().apply { moveTo(s * 0.34f, s * 0.22f); lineTo(s * 0.34f, s * 0.78f); lineTo(s * 0.78f, s * 0.50f); close() }; drawPath(path, tint) }
            GuidedIcon.Help -> { drawCircle(tint, s * 0.34f, p(0.50f, 0.50f), style = Stroke(w)); drawArc(tint, 205f, 220f, false, p(0.33f, 0.24f), Size(s * 0.34f, s * 0.34f), style = Stroke(w, cap = StrokeCap.Round)); drawCircle(tint, s * 0.025f, p(0.50f, 0.72f)) }
            GuidedIcon.Bot -> { drawRoundRect(tint, topLeft = p(0.20f, 0.30f), size = Size(s * 0.60f, s * 0.44f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f), style = Stroke(w)); drawCircle(tint, s * 0.035f, p(0.38f, 0.52f)); drawCircle(tint, s * 0.035f, p(0.62f, 0.52f)); drawLine(tint, p(0.50f, 0.18f), p(0.50f, 0.30f), w, cap = StrokeCap.Round) }
            GuidedIcon.Wrench -> { drawLine(tint, p(0.26f, 0.74f), p(0.70f, 0.30f), w, cap = StrokeCap.Round); drawCircle(tint, s * 0.08f, p(0.72f, 0.28f), style = Stroke(w)) }
            GuidedIcon.Check -> { drawCircle(tint, s * 0.34f, p(0.50f, 0.50f), style = Stroke(w)); drawLine(tint, p(0.34f, 0.52f), p(0.45f, 0.64f), w, cap = StrokeCap.Round); drawLine(tint, p(0.45f, 0.64f), p(0.70f, 0.38f), w, cap = StrokeCap.Round) }
            GuidedIcon.Alert -> { drawCircle(tint, s * 0.34f, p(0.50f, 0.50f), style = Stroke(w)); drawLine(tint, p(0.50f, 0.28f), p(0.50f, 0.58f), w, cap = StrokeCap.Round); drawCircle(tint, s * 0.025f, p(0.50f, 0.72f)) }
            GuidedIcon.Rocket -> { val path = Path().apply { moveTo(s * 0.50f, s * 0.15f); lineTo(s * 0.72f, s * 0.58f); lineTo(s * 0.50f, s * 0.78f); lineTo(s * 0.28f, s * 0.58f); close() }; drawPath(path, tint); drawCircle(Color(0xFF001407), s * 0.05f, p(0.50f, 0.42f)) }
        }
    }
}

private fun guidedInitialUpdate(context: Context): GuidedUpdateState {
    val prefs = context.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE)
    return GuidedUpdateState(
        localCode = prefs.getInt(GUIDE_PREF_CODE, GUIDE_DEFAULT_CODE),
        localName = prefs.getString(GUIDE_PREF_NAME, GUIDE_DEFAULT_NAME) ?: GUIDE_DEFAULT_NAME,
        shizuku = guidedShizukuState(),
        marker = guidedReadMarkerSmart()
    )
}

private fun guidedCheckUpdate(context: Context): GuidedUpdateState {
    val prefs = context.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE)
    val localCode = prefs.getInt(GUIDE_PREF_CODE, GUIDE_DEFAULT_CODE)
    val localName = prefs.getString(GUIDE_PREF_NAME, GUIDE_DEFAULT_NAME) ?: GUIDE_DEFAULT_NAME
    val marker = guidedReadMarkerSmart()
    val shizuku = guidedShizukuState()
    return try {
        val json = guidedFetchManifest()
        val latestCode = json.optInt("latestVersionCode", 0)
        val latestName = json.optString("latestVersionName", "Unknown")
        val patch = guidedFindZipPatch(json, localCode, latestCode)
        val cached = patch != null && guidedCachedZipVerified(context, patch, localCode, latestCode)
        val needsUpdate = latestCode > localCode
        GuidedUpdateState(
            localCode = localCode,
            localName = localName,
            latestCode = latestCode,
            latestName = latestName,
            shizuku = shizuku,
            marker = marker,
            status = when {
                needsUpdate -> "UPDATE"
                marker.startsWith("v26") -> "VERIFIED"
                else -> "CEK"
            },
            message = when {
                needsUpdate && cached && shizuku == "Ready" -> "Patch sudah terdownload. Tekan Apply Patch."
                needsUpdate && patch != null -> "Patch tersedia. Download dan verifikasi SHA terlebih dulu."
                needsUpdate -> "Update tersedia, tapi patch belum cocok untuk versi lokal."
                marker.startsWith("v26") -> "Versi dan data sudah terverifikasi."
                else -> "Data belum lengkap. Install Full Data dulu."
            },
            patchName = patch?.optString("name", "") ?: "",
            canDownload = needsUpdate && patch != null && !cached,
            canInstall = needsUpdate && cached && shizuku == "Ready",
            canAllowShizuku = needsUpdate && cached && shizuku == "Permission",
            releaseNotes = guidedStringList(json.optJSONArray("releaseNotes")),
            knownIssues = guidedStringList(json.optJSONArray("knownIssues")),
            sizeText = if (patch != null) guidedPatchSizeLabel(patch) else "-"
        )
    } catch (_: Throwable) {
        GuidedUpdateState(localCode = localCode, localName = localName, latestName = "Offline", status = "OFFLINE", message = "Gagal cek update. Cek koneksi internet.", shizuku = shizuku, marker = marker)
    }
}

private suspend fun guidedDownloadPatch(context: Context, onProgress: suspend (GuidedUpdateState) -> Unit): GuidedUpdateState {
    val base = withContext(Dispatchers.IO) { guidedCheckUpdate(context) }
    val json = try { withContext(Dispatchers.IO) { guidedFetchManifest() } } catch (_: Throwable) { return base.copy(status = "OFFLINE", message = "Gagal mengambil manifest.", working = false) }
    val patch = guidedFindZipPatch(json, base.localCode, base.latestCode) ?: return base.copy(status = "FAILED", message = "Patch ZIP belum tersedia.", working = false)
    val url = guidedPatchUrl(patch)
    if (url.isBlank()) return base.copy(status = "FAILED", message = "Patch tidak memiliki URL download.", working = false)
    val expectedSha = guidedPatchSha(patch)
    val out = guidedPatchCacheFile(context, base.localCode, base.latestCode)
    out.parentFile?.mkdirs()
    var lastError = "Download gagal."
    for (attempt in 1..3) {
        try {
            withContext(Dispatchers.IO) {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.instanceFollowRedirects = true
                val total = if (conn.contentLengthLong > 0) conn.contentLengthLong else patch.optLong("estimatedSizeBytes", -1L)
                val digest = MessageDigest.getInstance("SHA-256")
                val started = System.currentTimeMillis()
                var downloaded = 0L
                var lastUi = 0L
                FileOutputStream(out).use { output ->
                    conn.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastUi > 450L) {
                                lastUi = now
                                val speed = downloaded * 1000L / max(1L, now - started)
                                val eta = if (total > 0L && speed > 0L) (total - downloaded) / speed else -1L
                                val prog = if (total > 0L) downloaded.toFloat() / total.toFloat() else 0.05f
                                withContext(Dispatchers.Main) {
                                    onProgress(base.copy(working = true, status = "WAIT", message = "Downloading patch...", progress = prog.coerceIn(0f, 1f), progressText = "Patch attempt $attempt/3", sizeText = "${guidedFormatBytes(downloaded)} / ${if (total > 0L) guidedFormatBytes(total) else "unknown"}", speedText = "${guidedFormatBytes(speed)}/s", etaText = if (eta >= 0) guidedFormatSeconds(eta) else "-"))
                                }
                            }
                        }
                    }
                }
                conn.disconnect()
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (expectedSha.isNotBlank() && actual != expectedSha) {
                    out.delete()
                    throw IllegalStateException("SHA mismatch")
                }
            }
            return guidedCheckUpdate(context).copy(status = "READY", message = "Patch berhasil didownload dan SHA valid. Tekan Apply Patch.", progress = 1f, progressText = "SHA256 OK", sizeText = guidedFormatBytes(out.length()), speedText = "Cache", etaText = "0s", canDownload = false, canInstall = guidedShizukuState() == "Ready")
        } catch (e: Throwable) {
            lastError = e.message ?: "Download gagal."
            out.delete()
            if (attempt < 3) delay(900)
        }
    }
    return base.copy(status = "FAILED", message = "Download gagal setelah 3 percobaan. $lastError", working = false, canDownload = true)
}

private fun guidedInstallPatch(context: Context): GuidedUpdateState {
    val checked = guidedCheckUpdate(context)
    if (checked.shizuku != "Ready") return checked.copy(status = "SHIZUKU", message = "Shizuku belum Ready.", canAllowShizuku = checked.shizuku == "Permission")
    val json = try { guidedFetchManifest() } catch (_: Throwable) { return checked.copy(status = "OFFLINE", message = "Gagal mengambil manifest.") }
    val patch = guidedFindZipPatch(json, checked.localCode, checked.latestCode) ?: return checked.copy(status = "FAILED", message = "Patch tidak ditemukan.")
    val cache = guidedPatchCacheFile(context, checked.localCode, checked.latestCode)
    if (!cache.exists() || !guidedCachedZipVerified(context, patch, checked.localCode, checked.latestCode)) return checked.copy(status = "FAILED", message = "Patch belum valid. Download ulang.", canDownload = true, canInstall = false)
    return try {
        val extract = File(guidedCacheRoot(context), "extract_${checked.localCode}_${checked.latestCode}")
        if (extract.exists()) extract.deleteRecursively()
        extract.mkdirs()
        guidedExtractZip(cache, extract)
        val target = patch.optString("target", "/sdcard/Android/data/$GUIDE_GAME_PACKAGE/").ifBlank { "/sdcard/Android/data/$GUIDE_GAME_PACKAGE/" }
        val script = "mkdir -p ${guidedShellQuote(target)}\ncp -af ${guidedShellQuote(extract.absolutePath + "/.")} ${guidedShellQuote(target)}\n"
        val result = guidedRunShizuku(script)
        if (result.first != 0) return checked.copy(status = "FAILED", message = "Apply patch gagal via Shizuku.", canInstall = true)
        if (!guidedVerifyFiles(target, patch.optJSONArray("files"))) return checked.copy(status = "FAILED", message = "File patch tidak cocok setelah install.", canInstall = true)
        context.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE).edit().putInt(GUIDE_PREF_CODE, checked.latestCode).putString(GUIDE_PREF_NAME, checked.latestName).apply()
        guidedCheckUpdate(context).copy(status = "VERIFIED", message = "Patch berhasil diinstall dan diverifikasi.", progress = 1f, progressText = "Patch installed", sizeText = guidedFormatBytes(cache.length()), speedText = "Verified", etaText = "0s", canInstall = false, canDownload = false)
    } catch (_: Throwable) {
        checked.copy(status = "FAILED", message = "Install patch gagal. Coba ulang.", canInstall = true)
    }
}

private fun guidedFetchManifest(): JSONObject {
    val conn = URL(guidedManifestUrl()).openConnection() as HttpURLConnection
    conn.connectTimeout = 15000
    conn.readTimeout = 25000
    return try { BufferedReader(InputStreamReader(conn.inputStream)).use { JSONObject(it.readText()) } } finally { conn.disconnect() }
}

private fun guidedManifestUrl(): String = arrayOf("https://raw.", "githubusercontent.com/", "drmacze/", "F16/", "main/", "updates/", "latest.json").joinToString("")
private fun guidedPatchUrl(patch: JSONObject): String = patch.optString("url", patch.optString("downloadUrl", patch.optString("patchUrl", ""))).trim()
private fun guidedPatchSha(patch: JSONObject): String = patch.optString("sha256", patch.optString("hash", "")).trim().lowercase()
private fun guidedPatchSizeLabel(patch: JSONObject): String { val size = patch.optLong("estimatedSizeBytes", patch.optLong("sizeBytes", -1L)); return if (size > 0L) guidedFormatBytes(size) else "Patch zip" }

private fun guidedFindZipPatch(json: JSONObject, from: Int, to: Int): JSONObject? {
    val patches = json.optJSONArray("patches") ?: return null
    for (i in 0 until patches.length()) {
        val p = patches.optJSONObject(i) ?: continue
        val type = p.optString("type").lowercase()
        if (p.optInt("from") == from && p.optInt("to") == to && (type == "zip" || guidedPatchUrl(p).isNotBlank())) return p
    }
    return null
}

private fun guidedStringList(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    val out = mutableListOf<String>()
    for (i in 0 until array.length()) array.optString(i).trim().takeIf { it.isNotBlank() }?.let { out += it }
    return out
}

private fun guidedCachedZipVerified(context: Context, patch: JSONObject, from: Int, to: Int): Boolean {
    val file = guidedPatchCacheFile(context, from, to)
    if (!file.exists() || file.length() == 0L) return false
    val expected = guidedPatchSha(patch)
    if (expected.isBlank()) return true
    return try { guidedSha256(file) == expected } catch (_: Throwable) { false }
}

private fun guidedExtractZip(zipFile: File, outputDir: File) {
    ZipInputStream(zipFile.inputStream()).use { zis ->
        while (true) {
            val entry = zis.nextEntry ?: break
            val name = entry.name.trim().replace('\\', '/')
            if (!guidedSafeRelativePath(name)) throw IllegalStateException("Unsafe zip path")
            val out = File(outputDir, name).canonicalFile
            val root = outputDir.canonicalFile
            if (!out.path.startsWith(root.path)) throw IllegalStateException("Zip path escape")
            if (entry.isDirectory) out.mkdirs() else { out.parentFile?.mkdirs(); FileOutputStream(out).use { zis.copyTo(it) } }
            zis.closeEntry()
        }
    }
}

private fun guidedVerifyFiles(target: String, files: JSONArray?): Boolean {
    if (files == null) return true
    val script = StringBuilder()
    for (i in 0 until files.length()) {
        val obj = files.optJSONObject(i) ?: continue
        val rel = obj.optString("path", "").trim()
        if (!guidedSafeRelativePath(rel)) continue
        val out = target.trimEnd('/') + "/" + rel.trimStart('/')
        script.append("[ -f ${guidedShellQuote(out)} ] || exit 12\n")
        val sha = obj.optString("sha256", "").trim().lowercase()
        if (sha.isNotBlank()) script.append("sha256sum ${guidedShellQuote(out)} | grep -qi ${guidedShellQuote("^$sha")} || exit 13\n")
    }
    script.append("echo VERIFY_OK\n")
    val result = guidedRunShizuku(script.toString())
    return result.first == 0 && result.second.contains("VERIFY_OK")
}

private fun guidedReadMarkerSmart(): String {
    val direct = try { val f = File(GUIDE_MARKER); if (f.exists()) f.readText().trim().take(12).ifEmpty { "Verified" } else "No marker" } catch (_: Throwable) { "Protected" }
    if (direct != "No marker" && direct != "Protected") return direct
    if (guidedShizukuState() != "Ready") return direct
    val result = guidedRunShizuku("if [ -f ${guidedShellQuote(GUIDE_MARKER)} ]; then head -c 32 ${guidedShellQuote(GUIDE_MARKER)}; else echo NO_MARKER; fi")
    val out = result.second.trim()
    return if (out.startsWith("v26")) out.take(12) else "No marker"
}

private fun guidedShizukuState(): String = try { if (!Shizuku.pingBinder()) "Inactive" else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) "Ready" else "Permission" } catch (_: Throwable) { "Missing" }
private fun guidedRequestShizuku() { try { Shizuku.requestPermission(GUIDE_SHIZUKU_REQUEST) } catch (_: Throwable) {} }
private fun guidedRunShizuku(script: String): Pair<Int, String> = try { val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java); method.isAccessible = true; val process = method.invoke(null, arrayOf("sh", "-c", script), null, null) as Process; val exit = process.waitFor(); val output = process.inputStream.bufferedReader().readText() + process.errorStream.bufferedReader().readText(); exit to output } catch (e: Throwable) { -1 to (e.message ?: "Shizuku failed") }

private fun guidedCacheRoot(context: Context): File = context.externalCacheDir ?: context.cacheDir
private fun guidedPatchCacheFile(context: Context, from: Int, to: Int): File = File(guidedCacheRoot(context), "dlavie_patch_${from}_${to}.zip")
private fun guidedSafeRelativePath(path: String): Boolean = path.isNotBlank() && !path.startsWith("/") && !path.contains("..") && !path.contains("\\")
private fun guidedShellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
private fun guidedIsGameInstalled(context: Context): Boolean = try { context.packageManager.getPackageInfo(GUIDE_GAME_PACKAGE, 0); true } catch (_: PackageManager.NameNotFoundException) { false }
private fun guidedLaunchGame(context: Context) { context.packageManager.getLaunchIntentForPackage(GUIDE_GAME_PACKAGE)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }

private fun guidedSha256(file: File): String { val digest = MessageDigest.getInstance("SHA-256"); file.inputStream().use { input -> val buffer = ByteArray(64 * 1024); while (true) { val read = input.read(buffer); if (read <= 0) break; digest.update(buffer, 0, read) } }; return digest.digest().joinToString("") { "%02x".format(it) } }
private fun guidedFormatBytes(bytes: Long): String { if (bytes < 1024L) return "${bytes} B"; val kb = bytes / 1024.0; if (kb < 1024.0) return "${"%.1f".format(kb)} KB"; val mb = kb / 1024.0; if (mb < 1024.0) return "${"%.1f".format(mb)} MB"; return "${"%.2f".format(mb / 1024.0)} GB" }
private fun guidedFormatSeconds(seconds: Long): String { if (seconds < 0L) return "-"; if (seconds < 60L) return "${seconds}s"; val m = seconds / 60L; val s = seconds % 60L; return "${m}m ${s}s" }

private val GuideFont = FontFamily.SansSerif
private val GuideDark = Color(0xFF050606)
private val GuideCard = Color(0xFF101111)
private val GuideBorder = Color(0xFF252A2C)
private val GuideWhite = Color(0xFFF7F7F7)
private val GuideMuted = Color(0xFF7A7F83)
private val GuideGreen = Color(0xFF20E070)
private val GuideCyan = Color(0xFF28D7FF)
private val GuideRed = Color(0xFFFF4D4D)
