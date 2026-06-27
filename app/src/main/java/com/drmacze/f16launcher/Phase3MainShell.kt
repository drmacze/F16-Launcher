package com.drmacze.f16launcher

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private enum class P3Page(val label: String, val icon: String) {
    Home("Home", "⌂"),
    Update("Update", "↻"),
    Repair("Repair", "✦"),
    Chat("Chat", "◉"),
    Profile("Me", "☻")
}

@Composable
fun Phase3MainShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P3Page.Home) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "phase3-page", modifier = Modifier.fillMaxSize().padding(bottom = 92.dp)) { target ->
            when (target) {
                P3Page.Home -> P3HomeScreen { page = P3Page.Update }
                P3Page.Update -> P3UpdateScreen(openRepair = { page = P3Page.Repair })
                P3Page.Repair -> P3RepairScreen()
                P3Page.Chat -> CommunityScreen(api)
                P3Page.Profile -> P3ProfileScreen(api)
            }
        }
        P3FloatingNav(page = page, onPage = { page = it }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
    }
}

@Composable
private fun P3FloatingNav(page: P3Page, onPage: (P3Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 660.dp).padding(horizontal = 10.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xD80E1728),
        border = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 16.dp,
        tonalElevation = 0.dp
    ) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            P3Page.values().forEach { item ->
                val selected = item == page
                Button(
                    onClick = { onPage(item) },
                    modifier = Modifier.weight(1f).height(if (selected) 52.dp else 46.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.icon, fontSize = if (selected) 16.sp else 14.sp)
                        Text(item.label, fontSize = if (selected) 10.sp else 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun P3HomeScreen(openUpdate: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { DevPatchEngine(context) {} }
    val gameInstalled = remember { isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("DLavie Launcher", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3 • Direct Compose Updater", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text("Login masih nonaktif untuk development. Update Center sekarang mulai bisa apply patch langsung dari Compose, bukan hanya membuka updater lama.", color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(onClick = openUpdate, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Direct Update Center", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { launchGame(context) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Launch FIFA 16", fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Game", if (gameInstalled) "Detected" else "Missing", Modifier.weight(1f))
            SmallGlassStat("Access", engine.accessMode(), Modifier.weight(1f))
            SmallGlassStat("Local", "v${engine.localVersion()}", Modifier.weight(1f))
        }
        GlassCard {
            Text("Phase Target", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Now", "Direct apply inline/zip patch dari Compose dengan backup otomatis.")
            InfoLine("Safety", "Masih ada Advanced Updater sebagai fallback kalau direct mode gagal.")
        }
    }
}

@Composable
private fun P3UpdateScreen(openRepair: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready") }
    var localVersion by remember { mutableStateOf(1) }
    var latestVersion by remember { mutableStateOf("-") }
    var latestName by remember { mutableStateOf("Belum dicek") }
    var accessMode by remember { mutableStateOf("Checking...") }
    var service by remember { mutableStateOf("-") }
    var notes by remember { mutableStateOf<List<String>>(emptyList()) }
    var issues by remember { mutableStateOf<List<String>>(emptyList()) }
    var patchCards by remember { mutableStateOf<List<PatchPreview>>(emptyList()) }
    var logs by remember { mutableStateOf(listOf("Siap.")) }

    fun pushLog(message: String) {
        logs = (logs + message).takeLast(80)
        status = message
    }

    val engine = remember { DevPatchEngine(context) { message -> scope.launch(Dispatchers.Main) { pushLog(message) } } }

    fun refreshManifest() {
        status = "Mengecek manifest..."
        scope.launch {
            try {
                val manifest = withContext(Dispatchers.IO) { engine.fetchManifest() }
                val latestCode = manifest.optInt("latestVersionCode", engine.localVersion())
                localVersion = engine.localVersion()
                latestVersion = "v$latestCode"
                latestName = manifest.optString("latestVersionName", latestVersion)
                accessMode = engine.accessMode()
                service = manifest.optJSONObject("status")?.optString("message", "Online") ?: "Online"
                notes = jsonArrayStrings(manifest.optJSONArray("releaseNotes"))
                issues = jsonArrayStrings(manifest.optJSONArray("knownIssues"))
                patchCards = parsePatchPreviews(manifest)
                status = if (latestCode > localVersion) "Update tersedia: v$localVersion → $latestName" else "Sudah versi terbaru: v$localVersion"
            } catch (t: Throwable) {
                status = "Refresh gagal: ${t.message}"
                logs = (logs + status).takeLast(80)
            }
        }
    }

    fun directApply() {
        status = "Mulai direct apply..."
        scope.launch {
            try {
                withContext(Dispatchers.IO) { engine.applyAvailableUpdates() }
                localVersion = engine.localVersion()
                accessMode = engine.accessMode()
                status = "Direct update selesai. Local v$localVersion"
                logs = (logs + status).takeLast(80)
            } catch (t: Throwable) {
                status = "Direct update gagal: ${t.message}"
                logs = (logs + status).takeLast(80)
            }
        }
    }

    LaunchedEffect(Unit) {
        localVersion = engine.localVersion()
        accessMode = engine.accessMode()
        refreshManifest()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Direct Update Center", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3.1 • Compose apply engine", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text(status, color = SoftText)
            InfoLine("Service", service)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Local", "v$localVersion", Modifier.weight(1f))
            SmallGlassStat("Latest", latestVersion, Modifier.weight(1f))
            SmallGlassStat("Access", accessMode, Modifier.weight(1f))
        }
        GlassCard {
            Text("Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { refreshManifest() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Refresh Manifest", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { directApply() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Apply Update Direct", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = openRepair, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Repair / Restore", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Fallback Advanced Updater", fontWeight = FontWeight.Bold) }
        }
        if (patchCards.isNotEmpty()) GlassCard { Text("Patch Preview", fontSize = 20.sp, fontWeight = FontWeight.Bold); patchCards.forEach { PatchCard(it) } }
        if (notes.isNotEmpty()) GlassCard { Text("Release Notes", fontSize = 20.sp, fontWeight = FontWeight.Bold); notes.forEach { Text("• $it", color = SoftText) } }
        if (issues.isNotEmpty()) GlassCard { Text("Known Issues", fontSize = 20.sp, fontWeight = FontWeight.Bold); issues.forEach { Text("• $it", color = SoftText) } }
        GlassCard { Text("Activity Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P3RepairScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready") }
    var backupRoot by remember { mutableStateOf("") }
    var accessMode by remember { mutableStateOf("Checking...") }
    var logs by remember { mutableStateOf(listOf("Repair Center siap.")) }

    fun pushLog(message: String) {
        logs = (logs + message).takeLast(80)
        status = message
    }

    val engine = remember { DevPatchEngine(context) { message -> scope.launch(Dispatchers.Main) { pushLog(message) } } }
    val gameInstalled = remember { isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE) }

    fun refresh() {
        backupRoot = engine.latestBackupRoot()
        accessMode = engine.accessMode()
        status = "Repair data refreshed."
    }

    fun restore() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { engine.restoreLastBackup() }
                status = "Restore selesai."
                logs = (logs + status).takeLast(80)
            } catch (t: Throwable) {
                status = "Restore gagal: ${t.message}"
                logs = (logs + status).takeLast(80)
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Repair / Restore Center", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3.2 • Compose restore", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text(status, color = SoftText)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Game", if (gameInstalled) "Detected" else "Missing", Modifier.weight(1f))
            SmallGlassStat("Access", accessMode, Modifier.weight(1f))
            SmallGlassStat("Backup", if (backupRoot.isBlank()) "None" else "Ready", Modifier.weight(1f))
        }
        GlassCard {
            Text("Restore Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Latest backup", backupRoot.ifBlank { "Belum ada backup." })
            Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Refresh Backup State", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { restore() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Restore Last Backup Direct", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Fallback Advanced Restore", fontWeight = FontWeight.Bold) }
        }
        GlassCard { Text("Repair Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P3ProfileScreen(api: CommunityApi) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Developer Profile", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Login sementara nonaktif", color = CandyCyan)
            InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" })
            InfoLine("Target game", DevPatchEngine.GAME_PACKAGE)
            InfoLine("Updater", "Direct Compose Engine + Shizuku/root fallback")
        }
        GlassCard {
            Text("Next Phase", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Phase 3.3", "Progress bar real per file dan update state crash-safe.")
            InfoLine("Phase 3.4", "Hapus dependency layar updater lama setelah direct engine stabil.")
        }
    }
}
