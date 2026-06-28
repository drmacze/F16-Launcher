package com.drmacze.f16launcher

import android.content.Intent
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class P34Page(val label: String, val icon: String) {
    Home("Home", "⌂"),
    Update("Update", "↻"),
    Repair("Repair", "✦"),
    Chat("Chat", "◉"),
    Me("Me", "☻")
}

@Composable
fun Phase34MainShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P34Page.Home) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "phase34", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                P34Page.Home -> P34Home { page = P34Page.Update }
                P34Page.Update -> P34Update { page = P34Page.Repair }
                P34Page.Repair -> P34Repair()
                P34Page.Chat -> CommunityScreen(api)
                P34Page.Me -> P34Profile(api)
            }
        }
        P34Nav(page = page, onPage = { page = it }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun P34Nav(page: P34Page, onPage: (P34Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 660.dp).padding(horizontal = 10.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xD80E1728),
        border = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 18.dp,
        tonalElevation = 0.dp
    ) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            P34Page.values().forEach { item ->
                val selected = item == page
                Button(
                    onClick = { onPage(item) },
                    modifier = Modifier.weight(1f).height(if (selected) 52.dp else 46.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.icon, fontSize = if (selected) 16.sp else 14.sp, maxLines = 1)
                        Text(item.label, fontSize = if (selected) 10.sp else 9.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun P34Home(openUpdate: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { DevPatchEngine(context) {} }
    val gameInstalled = remember { isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("DLavie Launcher", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3.4 • Progress + Restore Points", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text("Direct updater sudah berjalan. Phase ini menambahkan progress visual, update state, restore point preview, dan flow repair yang lebih jelas.", color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(onClick = openUpdate, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Update Center", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { launchGame(context) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Launch FIFA 16", fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Game", if (gameInstalled) "Detected" else "Missing", Modifier.weight(1f))
            SmallGlassStat("Access", engine.accessMode(), Modifier.weight(1f))
            SmallGlassStat("Local", "v${engine.localVersion()}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun P34Update(openRepair: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var localVersion by remember { mutableStateOf(1) }
    var latestVersion by remember { mutableStateOf("-") }
    var accessMode by remember { mutableStateOf("Checking...") }
    var service by remember { mutableStateOf("-") }
    var status by remember { mutableStateOf("Ready") }
    var updateState by remember { mutableStateOf(prefs.getString("update_state", "idle") ?: "idle") }
    var progress by remember { mutableStateOf(0) }
    var progressLabel by remember { mutableStateOf("Idle") }
    var notes by remember { mutableStateOf<List<String>>(emptyList()) }
    var issues by remember { mutableStateOf<List<String>>(emptyList()) }
    var patches by remember { mutableStateOf<List<PatchPreview>>(emptyList()) }
    var logs by remember { mutableStateOf(listOf("Siap.")) }

    fun log(message: String) {
        logs = (logs + message).takeLast(80)
        status = message
    }

    fun setProgress(percent: Int, label: String) {
        progress = percent.coerceIn(0, 100)
        progressLabel = label
    }

    val engine = remember { DevPatchEngine(context) { message -> scope.launch(Dispatchers.Main) { log(message) } } }

    fun refreshManifest() {
        setProgress(8, "Fetching manifest")
        status = "Mengecek manifest..."
        scope.launch {
            try {
                val manifest = withContext(Dispatchers.IO) { engine.fetchManifest() }
                setProgress(55, "Parsing manifest")
                val latestCode = manifest.optInt("latestVersionCode", engine.localVersion())
                localVersion = engine.localVersion()
                latestVersion = "v$latestCode"
                accessMode = engine.accessMode()
                service = manifest.optJSONObject("status")?.optString("message", "Online") ?: "Online"
                notes = jsonArrayStrings(manifest.optJSONArray("releaseNotes"))
                issues = jsonArrayStrings(manifest.optJSONArray("knownIssues"))
                patches = parsePatchPreviews(manifest)
                status = if (latestCode > localVersion) "Update tersedia: v$localVersion → v$latestCode" else "Sudah versi terbaru: v$localVersion"
                setProgress(100, "Manifest ready")
            } catch (t: Throwable) {
                prefs.edit().putString("update_last_error", t.message ?: "unknown").apply()
                setProgress(0, "Manifest failed")
                log("Refresh gagal: ${t.message}")
            }
        }
    }

    fun applyDirect() {
        prefs.edit().putString("update_state", "running").putLong("update_started_at", System.currentTimeMillis()).apply()
        updateState = "running"
        setProgress(5, "Preparing update")
        log("Mulai direct update...")
        scope.launch {
            try {
                setProgress(25, "Reading manifest")
                withContext(Dispatchers.IO) { engine.applyAvailableUpdates() }
                setProgress(90, "Finalizing")
                localVersion = engine.localVersion()
                accessMode = engine.accessMode()
                prefs.edit().putString("update_state", "done").apply()
                updateState = "done"
                setProgress(100, "Update completed")
                log("Direct update selesai. Local v$localVersion")
            } catch (t: Throwable) {
                prefs.edit().putString("update_state", "failed").putString("update_last_error", t.message ?: "unknown").apply()
                updateState = "failed"
                setProgress(progress, "Update failed")
                log("Direct update gagal: ${t.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        localVersion = engine.localVersion()
        accessMode = engine.accessMode()
        val saved = prefs.getString("update_state", "idle") ?: "idle"
        updateState = saved
        if (saved == "running") log("Update sebelumnya terputus. Gunakan Repair/Restore atau ulangi Apply Update Direct.")
        refreshManifest()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Direct Update Center", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3.4 • Progress flow", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text(status, color = SoftText)
            InfoLine("Service", service)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Local", "v$localVersion", Modifier.weight(1f))
            SmallGlassStat("Latest", latestVersion, Modifier.weight(1f))
            SmallGlassStat("Access", accessMode, Modifier.weight(1f))
        }
        P34ProgressCard(progress, progressLabel, updateState, prefs.getString("update_last_error", "-") ?: "-")
        GlassCard {
            Text("Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { refreshManifest() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Refresh Manifest", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { applyDirect() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Apply Update Direct", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = openRepair, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Repair / Restore", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Fallback Advanced Updater", fontWeight = FontWeight.Bold) }
        }
        if (patches.isNotEmpty()) GlassCard { Text("Patch Preview", fontSize = 20.sp, fontWeight = FontWeight.Bold); patches.forEach { PatchCard(it) } }
        if (notes.isNotEmpty()) GlassCard { Text("Release Notes", fontSize = 20.sp, fontWeight = FontWeight.Bold); notes.forEach { Text("• $it", color = SoftText) } }
        if (issues.isNotEmpty()) GlassCard { Text("Known Issues", fontSize = 20.sp, fontWeight = FontWeight.Bold); issues.forEach { Text("• $it", color = SoftText) } }
        GlassCard { Text("Activity Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P34ProgressCard(progress: Int, label: String, state: String, error: String) {
    GlassCard {
        Text("Update Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        InfoLine("State", state)
        InfoLine("Step", label)
        Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) {
            Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp)))
        }
        if (state == "failed") InfoLine("Last error", error)
    }
}

@Composable
private fun P34Repair() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready") }
    var backupRoot by remember { mutableStateOf("") }
    var backupTarget by remember { mutableStateOf("") }
    var accessMode by remember { mutableStateOf("Checking...") }
    var progress by remember { mutableStateOf(0) }
    var progressLabel by remember { mutableStateOf("Idle") }
    var logs by remember { mutableStateOf(listOf("Repair Center siap.")) }

    fun log(message: String) {
        logs = (logs + message).takeLast(80)
        status = message
    }

    fun setProgress(percent: Int, label: String) {
        progress = percent.coerceIn(0, 100)
        progressLabel = label
    }

    val engine = remember { DevPatchEngine(context) { message -> scope.launch(Dispatchers.Main) { log(message) } } }
    val gameInstalled = remember { isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE) }

    fun refresh() {
        backupRoot = engine.latestBackupRoot()
        backupTarget = engine.latestBackupTarget()
        accessMode = engine.accessMode()
        setProgress(100, "Backup state refreshed")
        status = "Repair data refreshed."
    }

    fun restore() {
        setProgress(10, "Preparing restore")
        scope.launch {
            try {
                setProgress(35, "Restoring latest backup")
                withContext(Dispatchers.IO) { engine.restoreLastBackup() }
                setProgress(100, "Restore completed")
                log("Restore selesai.")
            } catch (t: Throwable) {
                setProgress(progress, "Restore failed")
                log("Restore gagal: ${t.message}")
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Repair / Restore Center", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3.4 • Restore point preview", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text(status, color = SoftText)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Game", if (gameInstalled) "Detected" else "Missing", Modifier.weight(1f))
            SmallGlassStat("Access", accessMode, Modifier.weight(1f))
            SmallGlassStat("Backup", if (backupRoot.isBlank()) "None" else "Ready", Modifier.weight(1f))
        }
        P34ProgressCard(progress, progressLabel, if (backupRoot.isBlank()) "no_backup" else "ready", "-")
        GlassCard {
            Text("Restore Point", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (backupRoot.isBlank()) {
                Text("Belum ada restore point. Restore point akan dibuat otomatis saat patch mengganti file lama.", color = SoftText)
            } else {
                GlassListItem("Latest restore point", "$backupRoot\nTarget: $backupTarget", true) {}
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Refresh Backup State", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { restore() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Restore Latest Backup Direct", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Fallback Advanced Restore", fontWeight = FontWeight.Bold) }
        }
        GlassCard { Text("Repair Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P34Profile(api: CommunityApi) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Developer Profile", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Login sementara nonaktif", color = CandyCyan)
            InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" })
            InfoLine("Target game", DevPatchEngine.GAME_PACKAGE)
            InfoLine("Updater", "Direct Compose Engine + Shizuku/root fallback")
        }
        GlassCard {
            Text("Next Phase", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Phase 3.5", "Merapikan direct engine agar memberi progress asli dari dalam copy engine.")
            InfoLine("Phase 4", "Aktifkan kembali login + community setelah update flow stabil.")
        }
    }
}
