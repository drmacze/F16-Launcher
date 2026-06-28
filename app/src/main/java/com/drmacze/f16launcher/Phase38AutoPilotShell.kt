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

private enum class PilotPage(val label: String, val icon: String) {
    Auto("Auto", "↻"), Guide("Guide", "?"), More("More", "☰")
}

@Composable
fun Phase38AutoPilotShell(api: CommunityApi) {
    var page by remember { mutableStateOf(PilotPage.Auto) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "phase38", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                PilotPage.Auto -> PilotAutoUpdate()
                PilotPage.Guide -> PilotGuide()
                PilotPage.More -> PilotMore(api) { page = PilotPage.Auto }
            }
        }
        PilotNav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun PilotNav(page: PilotPage, onPage: (PilotPage) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp, tonalElevation = 0.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PilotPage.values().forEach { item ->
                val selected = item == page
                Button(onClick = { onPage(item) }, modifier = Modifier.weight(1f).height(if (selected) 54.dp else 48.dp), shape = RoundedCornerShape(25.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText), elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.icon, fontSize = 16.sp, maxLines = 1)
                        Text(item.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun PilotAutoUpdate() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var local by remember { mutableStateOf(1) }
    var latest by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Auto Pilot siap.") }
    var mainLabel by remember { mutableStateOf("Start Auto Pilot") }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var access by remember { mutableStateOf("Checking...") }
    var state by remember { mutableStateOf(prefs.getString("update_state", "idle") ?: "idle") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var gameInstalled by remember { mutableStateOf(false) }
    var backupReady by remember { mutableStateOf(false) }
    var autoLaunch by remember { mutableStateOf(prefs.getBoolean("auto_launch_after_update", true)) }
    var logs by remember { mutableStateOf(listOf("Auto Pilot dimulai.")) }

    fun log(s: String) { logs = (logs + s).takeLast(60); status = s }
    fun pct(p: Int, s: String) { progress = p.coerceIn(0, 100); step = s }
    val engine = remember { DevPatchEngine(context) { m -> scope.launch(Dispatchers.Main) { log(m) } } }

    fun refreshAll(silent: Boolean = false) {
        gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
        shizuku = ShizukuSetup.status(context)
        access = engine.accessMode()
        local = engine.localVersion()
        backupReady = engine.latestBackupRoot().isNotBlank()
        state = prefs.getString("update_state", "idle") ?: "idle"
        pct(12, "Checking")
        if (!silent) log("Mengecek semua status...")
        scope.launch {
            try {
                val manifest = withContext(Dispatchers.IO) { engine.fetchManifest() }
                latest = manifest.optInt("latestVersionCode", local)
                pct(100, "Ready")
                mainLabel = when {
                    !gameInstalled -> "Launch / Install FIFA 16"
                    shizuku != "Ready" -> "Setup Shizuku"
                    state == "failed" -> if (backupReady) "Auto Recover" else "Clear Error"
                    latest > local -> "Update Now"
                    else -> "Play FIFA 16"
                }
                if (!silent) log(if (latest > local) "Update tersedia: v$local → v$latest" else "Semua siap. Versi v$local sudah terbaru.")
            } catch (t: Throwable) {
                latest = 0
                pct(0, "Manifest failed")
                mainLabel = if (shizuku != "Ready") "Setup Shizuku" else "Retry Check"
                log("Gagal membaca manifest: ${t.message}")
            }
        }
    }

    fun recover() {
        scope.launch {
            try {
                pct(20, "Preparing recovery")
                if (backupReady) withContext(Dispatchers.IO) { engine.restoreLastBackup() }
                prefs.edit().remove("update_state").remove("update_last_error").apply()
                state = "idle"
                pct(100, "Recovered")
                log("Recovery selesai. Kamu bisa update ulang.")
                refreshAll(true)
            } catch (t: Throwable) {
                pct(progress, "Recovery failed")
                log("Recovery gagal: ${t.message}")
            }
        }
    }

    fun applyUpdate(retest: Boolean = false) {
        prefs.edit().putString("update_state", "running").apply()
        state = "running"
        pct(10, if (retest) "Retest" else "Updating")
        log(if (retest) "Retest auto update dimulai..." else "Auto update dimulai...")
        scope.launch {
            try {
                if (retest) withContext(Dispatchers.IO) { engine.resetLocalVersion(1) }
                pct(35, "Applying patch")
                withContext(Dispatchers.IO) { engine.applyAvailableUpdates() }
                local = engine.localVersion()
                prefs.edit().putString("update_state", "done").remove("update_last_error").apply()
                state = "done"
                pct(100, "Completed")
                mainLabel = if (autoLaunch) "Launching..." else "Play FIFA 16"
                log("Update selesai. Local v$local")
                if (autoLaunch) launchGame(context)
            } catch (t: Throwable) {
                prefs.edit().putString("update_state", "failed").putString("update_last_error", t.message ?: "unknown").apply()
                state = "failed"
                backupReady = engine.latestBackupRoot().isNotBlank()
                mainLabel = if (backupReady) "Auto Recover" else "Clear Error"
                pct(progress, "Failed")
                log("Update gagal: ${t.message}")
            }
        }
    }

    fun mainAction() {
        gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
        shizuku = ShizukuSetup.status(context)
        access = engine.accessMode()
        backupReady = engine.latestBackupRoot().isNotBlank()
        when {
            !gameInstalled -> launchGame(context)
            shizuku == "Not Installed" || shizuku == "Need Start" -> { ShizukuSetup.openApp(context); log(ShizukuSetup.shortHint(context)); mainLabel = "Recheck Shizuku" }
            shizuku == "Need Permission" -> { ShizukuSetup.requestPermission(); log("Izinkan DLavie di dialog Shizuku, lalu tekan tombol utama lagi."); mainLabel = "Recheck Permission" }
            state == "failed" -> recover()
            latest <= 0 -> refreshAll(false)
            latest > local -> applyUpdate(false)
            else -> launchGame(context)
        }
    }

    LaunchedEffect(Unit) { refreshAll(true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Auto Pilot", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3.8 • One screen update", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text(status, color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(62.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text(mainLabel, fontWeight = FontWeight.Black, fontSize = 16.sp) }
        }
        PilotBigStatus(gameInstalled, shizuku, access, local, latest, state, backupReady)
        PilotProgress(progress, step)
        GlassCard {
            Text("Shizuku", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Status", shizuku)
            Text(ShizukuSetup.shortHint(context), color = SoftText)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { ShizukuSetup.requestPermission(); refreshAll(true) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Grant Permission", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { ShizukuSetup.openApp(context) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Shizuku", fontWeight = FontWeight.Bold) }
        }
        GlassCard {
            Text("Quick Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { refreshAll(false) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Recheck Status", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { applyUpdate(true) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Dev Retest v1 → Latest", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { autoLaunch = !autoLaunch; prefs.edit().putBoolean("auto_launch_after_update", autoLaunch).apply(); log(if (autoLaunch) "Auto launch aktif." else "Auto launch nonaktif.") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF182238))) { Text(if (autoLaunch) "Auto Launch: ON" else "Auto Launch: OFF", fontWeight = FontWeight.Bold) }
        }
        GlassCard { Text("Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun PilotGuide() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("Guide", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Buat user awam", color = CandyCyan); Spacer(Modifier.height(12.dp)); Text("Sekarang cukup fokus ke tab Auto. Semua tombol teknis hanya cadangan.", color = SoftText) }
        GlassCard { Text("Alur utama", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("1", "Tekan tombol utama di Auto Pilot."); InfoLine("2", "Kalau Shizuku belum ready, buka/izinkan Shizuku dari kartu Shizuku."); InfoLine("3", "Tekan tombol utama lagi."); InfoLine("4", "Jika sudah latest, tombol akan menjalankan FIFA 16.") }
        GlassCard { Text("Kenapa Shizuku tidak bisa 100% otomatis?", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Android tetap membutuhkan service Shizuku berjalan dan izin user. DLavie sekarang hanya menyederhanakan prosesnya: buka Shizuku, minta izin, cek lagi, lalu update dari satu halaman.", color = SoftText) }
    }
}

@Composable
private fun PilotMore(api: CommunityApi, openAuto: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var status by remember { mutableStateOf("More tools ready.") }
    var local by remember { mutableStateOf(prefs.getInt("local_version_code", 1)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Tools lanjutan disimpan di sini.", color = SoftText) }
        GlassCard { Text("Developer Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Current local", "v$local"); Button(onClick = { prefs.edit().putInt("local_version_code", 1).remove("update_state").remove("update_last_error").apply(); local = 1; status = "Reset ke v1. Buka Auto untuk update ulang." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Reset Local Version to v1", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { prefs.edit().remove("update_state").remove("update_last_error").apply(); status = "Update state cleared." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Clear Update State", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openAuto, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Auto", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Legacy Advanced Updater", fontWeight = FontWeight.Bold) } }
        GlassCard { Text("Developer", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status); InfoLine("Login", "Masih nonaktif untuk development.") }
    }
}

@Composable
private fun PilotBigStatus(gameInstalled: Boolean, shizuku: String, access: String, local: Int, latest: Int, state: String, backupReady: Boolean) {
    GlassCard { Text("Status", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Game", if (gameInstalled) "Detected" else "Missing"); InfoLine("Shizuku", shizuku); InfoLine("Access", access); InfoLine("Version", if (latest > 0) "v$local / latest v$latest" else "v$local"); InfoLine("Update state", state); InfoLine("Backup", if (backupReady) "Ready" else "None") }
}

@Composable
private fun PilotProgress(progress: Int, label: String) {
    GlassCard { Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Step", label); Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp); Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp))) } }
}
