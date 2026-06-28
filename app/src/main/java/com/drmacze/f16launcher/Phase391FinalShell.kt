package com.drmacze.f16launcher

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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

private enum class P391Page(val label: String, val icon: String) { Auto("Auto", "↻"), Help("Help", "?"), More("More", "☰") }
private enum class P391Status(val title: String) { Checking("Checking"), NeedGame("Game Missing"), NeedShizuku("Need Shizuku"), NeedPermission("Need Permission"), UpdateAvailable("Update Available"), Ready("Ready"), Updating("Updating"), Recovery("Need Recovery"), Offline("Offline") }

@Composable
fun Phase391FinalShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P391Page.Auto) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "phase391", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                P391Page.Auto -> P391AutoPilot()
                P391Page.Help -> P391Help()
                P391Page.More -> P391More(api) { page = P391Page.Auto }
            }
        }
        P391Nav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun P391Card(content: @Composable ColumnScope.() -> Unit) = GlassCard(Modifier.fillMaxWidth(), content)

@Composable
private fun P391Nav(page: P391Page, onPage: (P391Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp, tonalElevation = 0.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            P391Page.values().forEach { item ->
                val selected = item == page
                Button(onClick = { onPage(item) }, modifier = Modifier.weight(1f).height(if (selected) 54.dp else 48.dp), shape = RoundedCornerShape(25.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText), elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(item.icon, fontSize = 16.sp, maxLines = 1); Text(item.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                }
            }
        }
    }
}

@Composable
private fun P391AutoPilot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    val engine = remember { DevPatchEngine(context) {} }
    var local by remember { mutableStateOf(1) }
    var latest by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(P391Status.Checking) }
    var message by remember { mutableStateOf("Menyiapkan Auto Pilot...") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var access by remember { mutableStateOf("Checking...") }
    var updateState by remember { mutableStateOf(prefs.getString("update_state", "idle") ?: "idle") }
    var backupReady by remember { mutableStateOf(false) }
    var autoLaunch by remember { mutableStateOf(prefs.getBoolean("auto_launch_after_update", true)) }
    var logs by remember { mutableStateOf(listOf("Auto Pilot siap.")) }

    fun log(text: String) { logs = (logs + text).takeLast(20); message = text }
    fun pct(value: Int, label: String) { progress = value.coerceIn(0, 100); step = label }
    fun label(): String = when (status) {
        P391Status.NeedGame -> "Open FIFA 16"
        P391Status.NeedShizuku -> "Open Shizuku"
        P391Status.NeedPermission -> "Grant Permission"
        P391Status.UpdateAvailable -> "Update Now"
        P391Status.Ready -> "Play FIFA 16"
        P391Status.Recovery -> if (backupReady) "Auto Recover" else "Clear Error"
        P391Status.Offline -> "Retry Check"
        P391Status.Updating -> "Updating..."
        P391Status.Checking -> "Checking..."
    }

    fun refresh(silent: Boolean = false) {
        status = P391Status.Checking
        pct(12, "Checking")
        if (!silent) log("Mengecek status...")
        scope.launch {
            val installed = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
            shizuku = ShizukuSetup.status(context)
            access = engine.accessMode()
            local = engine.localVersion()
            backupReady = engine.latestBackupRoot().isNotBlank()
            updateState = prefs.getString("update_state", "idle") ?: "idle"
            when {
                !installed -> { status = P391Status.NeedGame; pct(0, "Game missing"); log("FIFA 16 belum terdeteksi."); return@launch }
                updateState == "failed" -> { status = P391Status.Recovery; pct(progress, "Recovery needed"); log("Update sebelumnya gagal. Tekan Auto Recover."); return@launch }
                shizuku == "Not Installed" || shizuku == "Need Start" -> { status = P391Status.NeedShizuku; pct(20, "Need Shizuku"); log(ShizukuSetup.shortHint(context)); return@launch }
                shizuku == "Need Permission" -> { status = P391Status.NeedPermission; pct(35, "Need permission"); log(ShizukuSetup.shortHint(context)); return@launch }
            }
            try {
                pct(55, "Reading manifest")
                val manifest = withContext(Dispatchers.IO) { engine.fetchManifest() }
                latest = manifest.optInt("latestVersionCode", local)
                local = engine.localVersion()
                access = engine.accessMode()
                status = if (latest > local) P391Status.UpdateAvailable else P391Status.Ready
                updateState = prefs.getString("update_state", "idle") ?: "idle"
                pct(100, "Ready")
                log(if (latest > local) "Update tersedia: v$local → v$latest" else "Siap. FIFA 16 sudah v$local.")
            } catch (t: Throwable) { status = P391Status.Offline; pct(0, "Manifest failed"); log("Gagal membaca update GitHub: ${t.message}") }
        }
    }

    fun recoverOrClear() {
        status = P391Status.Updating
        pct(15, "Recovering")
        log("Recovery dimulai...")
        scope.launch {
            try {
                if (backupReady) withContext(Dispatchers.IO) { engine.restoreLastBackup() }
                prefs.edit().remove("update_state").remove("update_last_error").apply()
                updateState = "idle"
                pct(100, "Recovered")
                log("Recovery selesai.")
                refresh(true)
            } catch (t: Throwable) { status = P391Status.Recovery; log("Recovery gagal: ${t.message}") }
        }
    }

    fun update() {
        status = P391Status.Updating
        prefs.edit().putString("update_state", "running").apply()
        updateState = "running"
        pct(10, "Updating")
        log("Update dimulai...")
        scope.launch {
            try {
                pct(35, "Applying patch")
                withContext(Dispatchers.IO) { engine.applyAvailableUpdates() }
                local = engine.localVersion()
                latest = local.coerceAtLeast(latest)
                prefs.edit().putString("update_state", "done").remove("update_last_error").apply()
                updateState = "done"
                backupReady = engine.latestBackupRoot().isNotBlank()
                pct(100, "Completed")
                status = P391Status.Ready
                log(if (autoLaunch) "Update selesai. Membuka FIFA 16..." else "Update selesai. Tekan Play FIFA 16.")
                if (autoLaunch) launchGame(context)
            } catch (t: Throwable) {
                prefs.edit().putString("update_state", "failed").putString("update_last_error", t.message ?: "unknown").apply()
                updateState = "failed"
                backupReady = engine.latestBackupRoot().isNotBlank()
                status = P391Status.Recovery
                pct(progress, "Failed")
                log("Update gagal: ${t.message}")
            }
        }
    }

    fun mainAction() {
        when (status) {
            P391Status.NeedGame -> launchGame(context)
            P391Status.NeedShizuku -> { ShizukuSetup.openApp(context); log("Buka Shizuku, start service, lalu kembali ke DLavie.") }
            P391Status.NeedPermission -> { ShizukuSetup.requestPermission(); log("Pilih Allow di dialog Shizuku, lalu tekan tombol utama lagi.") }
            P391Status.UpdateAvailable -> update()
            P391Status.Ready -> launchGame(context)
            P391Status.Recovery -> recoverOrClear()
            P391Status.Offline, P391Status.Checking -> refresh(false)
            P391Status.Updating -> log("Update sedang berjalan. Tunggu selesai.")
        }
    }

    LaunchedEffect(Unit) { refresh(true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P391Card { Text("DLavie Auto Pilot", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Phase 3.9.2 • Launch Prep", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); P391StatusPill(status); Spacer(Modifier.height(10.dp)); Text(message, color = SoftText); Spacer(Modifier.height(16.dp)); Button(enabled = status != P391Status.Updating, onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) { Text(label(), fontWeight = FontWeight.Black, fontSize = 17.sp) } }
        P391Readiness(shizuku, access, local, latest, updateState, backupReady, autoLaunch)
        P391Progress(progress, step)
        P391Card { Text("Shizuku Helper", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(ShizukuSetup.shortHint(context), color = SoftText); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { Button(onClick = { ShizukuSetup.requestPermission(); refresh(true) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Grant", fontWeight = FontWeight.Bold) }; Button(onClick = { ShizukuSetup.openApp(context) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open", fontWeight = FontWeight.Bold) } } }
        P391Card { Text("Quick Controls", fontSize = 20.sp, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { Button(onClick = { refresh(false) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Recheck", fontWeight = FontWeight.Bold) }; Button(onClick = { autoLaunch = !autoLaunch; prefs.edit().putBoolean("auto_launch_after_update", autoLaunch).apply(); log(if (autoLaunch) "Auto launch ON." else "Auto launch OFF.") }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text(if (autoLaunch) "Auto ON" else "Auto OFF", fontWeight = FontWeight.Bold) } } }
        P391Card { Text("Recent Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.takeLast(5).forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P391StatusPill(status: P391Status) { val color = when (status) { P391Status.Ready -> NeonGreen; P391Status.UpdateAvailable -> CandyCyan; P391Status.NeedShizuku, P391Status.NeedPermission -> Color(0xFFFFB84D); P391Status.Recovery, P391Status.Offline, P391Status.NeedGame -> Color(0xFFFF5269); P391Status.Updating, P391Status.Checking -> CandyBlue }; Surface(shape = RoundedCornerShape(22.dp), color = Color(0x33101827), border = BorderStroke(1.dp, color)) { Text(status.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold) } }
@Composable
private fun P391Readiness(shizuku: String, access: String, local: Int, latest: Int, updateState: String, backup: Boolean, autoLaunch: Boolean) = P391Card { Text("Launch Readiness", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Update", if (latest > 0 && local >= latest) "Ready" else "Check required"); InfoLine("Shizuku", shizuku); InfoLine("Access", access); InfoLine("Version", if (latest > 0) "v$local / latest v$latest" else "v$local"); InfoLine("Backup", if (backup) "Ready" else "None yet"); InfoLine("Auto launch", if (autoLaunch) "On" else "Off"); InfoLine("Community", "Parked until production launch") }
@Composable
private fun P391Progress(progress: Int, label: String) = P391Card { Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Step", label); Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp); Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp))) } }
@Composable
private fun P391Help() { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { P391Card { Text("Help", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Panduan sederhana", color = CandyCyan); Spacer(Modifier.height(12.dp)); Text("Untuk user normal, cukup pakai Auto. Login dan community sengaja ditaruh di tahap akhir setelah APK siap production.", color = SoftText) }; P391Card { Text("Alur final", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("1", "Tekan tombol utama di Auto Pilot."); InfoLine("2", "Kalau diminta Shizuku, tekan Open/Grant."); InfoLine("3", "Kalau sudah Ready, tombol utama menjadi Play FIFA 16."); InfoLine("4", "Jika gagal, tombol menjadi Auto Recover.") }; P391Card { Text("Fokus sekarang", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Current", "Production hardening update/launcher."); InfoLine("Later", "Login + community setelah update flow benar-benar siap launch.") } } }
@Composable
private fun P391More(api: CommunityApi, openAuto: () -> Unit) { val context = LocalContext.current; val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }; var local by remember { mutableStateOf(prefs.getInt("local_version_code", 1)) }; var status by remember { mutableStateOf("Advanced tools ready.") }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { P391Card { Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Developer tools disimpan di sini agar Auto tetap bersih.", color = SoftText) }; P391Card { Text("Advanced Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Local", "v$local"); Button(onClick = { prefs.edit().putInt("local_version_code", 1).remove("update_state").remove("update_last_error").apply(); local = 1; status = "Local di-reset ke v1." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Dev Retest: Reset Local to v1", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { prefs.edit().remove("update_state").remove("update_last_error").apply(); status = "Update state dibersihkan." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Clear Update State", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openAuto, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Auto", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Legacy Advanced Updater", fontWeight = FontWeight.Bold) } }; P391Card { Text("Roadmap", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Now", "Production hardening"); InfoLine("Login", "At the end, before production launch"); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status) } } }
