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

private enum class SimplePage(val label: String, val icon: String) {
    Home("Home", "⌂"), Auto("Auto", "↻"), More("More", "☰")
}

@Composable
fun Phase37SimpleShell(api: CommunityApi) {
    var page by remember { mutableStateOf(SimplePage.Home) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "phase37", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                SimplePage.Home -> SimpleHome { page = SimplePage.Auto }
                SimplePage.Auto -> SimpleAutoUpdate()
                SimplePage.More -> SimpleMore(api) { page = SimplePage.Auto }
            }
        }
        SimpleNav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun SimpleNav(page: SimplePage, onPage: (SimplePage) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp, tonalElevation = 0.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SimplePage.values().forEach { item ->
                val selected = item == page
                Button(onClick = { onPage(item) }, modifier = Modifier.weight(1f).height(if (selected) 54.dp else 48.dp), shape = RoundedCornerShape(25.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText), elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(item.icon, fontSize = 16.sp, maxLines = 1); Text(item.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                }
            }
        }
    }
}

@Composable
private fun SimpleHome(openAuto: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { DevPatchEngine(context) {} }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("DLavie Launcher", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3.7 • Simple Auto Mode", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text("Mode dibuat lebih sederhana: cukup buka Auto lalu tekan satu tombol utama. Menu teknis dipindah ke More.", color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(onClick = openAuto, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Start Auto Update", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { launchGame(context) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Launch FIFA 16", fontWeight = FontWeight.Bold) }
        }
        SimpleSystemCard(isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE), engine.accessMode(), ShizukuSetup.status(context), engine.localVersion())
        GlassCard { Text("Cara paling simpel", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("1. Buka Auto\n2. Tekan tombol hijau\n3. Kalau Shizuku belum siap, app akan kasih tombol Grant/Open Shizuku\n4. Setelah Ready, tekan tombol hijau lagi", color = SoftText) }
    }
}

@Composable
private fun SimpleAutoUpdate() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var local by remember { mutableStateOf(1) }
    var latest by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Ready") }
    var mainLabel by remember { mutableStateOf("Start Auto Update") }
    var shizukuStatus by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var access by remember { mutableStateOf("Checking...") }
    var state by remember { mutableStateOf(prefs.getString("update_state", "idle") ?: "idle") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var logs by remember { mutableStateOf(listOf("Mode simple siap.")) }

    fun log(s: String) { logs = (logs + s).takeLast(50); status = s }
    fun pct(p: Int, s: String) { progress = p.coerceIn(0, 100); step = s }
    fun refreshAccess(engine: DevPatchEngine) { shizukuStatus = ShizukuSetup.status(context); access = engine.accessMode(); local = engine.localVersion() }
    val engine = remember { DevPatchEngine(context) { m -> scope.launch(Dispatchers.Main) { log(m) } } }

    fun refreshManifest(silent: Boolean = false) {
        pct(10, "Checking manifest")
        if (!silent) log("Mengecek update...")
        scope.launch {
            try {
                val m = withContext(Dispatchers.IO) { engine.fetchManifest() }
                local = engine.localVersion()
                latest = m.optInt("latestVersionCode", local)
                refreshAccess(engine)
                state = prefs.getString("update_state", "idle") ?: "idle"
                mainLabel = when {
                    shizukuStatus != "Ready" -> "Setup Shizuku"
                    latest > local -> "Update Now"
                    else -> "Ready - Launch FIFA 16"
                }
                pct(100, "Ready")
                if (!silent) log(if (latest > local) "Update tersedia: v$local → v$latest" else "Sudah terbaru: v$local")
            } catch (t: Throwable) { pct(0, "Manifest failed"); log("Gagal cek update: ${t.message}") }
        }
    }

    fun mainAction() {
        refreshAccess(engine)
        when {
            shizukuStatus == "Not Installed" || shizukuStatus == "Need Start" -> { ShizukuSetup.openApp(context); log(ShizukuSetup.shortHint(context)); mainLabel = "Cek Lagi Setelah Shizuku Ready" }
            shizukuStatus == "Need Permission" -> { val ok = ShizukuSetup.requestPermission(); log(if (ok) "Dialog izin Shizuku dibuka. Pilih Allow lalu tekan tombol ini lagi." else "Gagal meminta izin. Buka Shizuku manual."); mainLabel = "Cek Izin Shizuku" }
            latest <= 0 -> refreshManifest(false)
            latest > local -> {
                prefs.edit().putString("update_state", "running").apply(); state = "running"; pct(15, "Applying update"); log("Mulai auto update...")
                scope.launch { try { withContext(Dispatchers.IO) { engine.applyAvailableUpdates() }; local = engine.localVersion(); prefs.edit().putString("update_state", "done").remove("update_last_error").apply(); state = "done"; pct(100, "Update completed"); mainLabel = "Launch FIFA 16"; log("Update selesai. Local v$local") } catch (t: Throwable) { prefs.edit().putString("update_state", "failed").putString("update_last_error", t.message ?: "unknown").apply(); state = "failed"; pct(progress, "Update failed"); mainLabel = "Repair / Restore"; log("Update gagal: ${t.message}") } }
            }
            else -> launchGame(context)
        }
    }

    LaunchedEffect(Unit) { refreshAccess(engine); refreshManifest(true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("Auto Update", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Satu tombol utama", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); Text(status, color = SoftText); Spacer(Modifier.height(16.dp)); Button(onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text(mainLabel, fontWeight = FontWeight.Black, fontSize = 16.sp) } }
        SimpleSystemCard(isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE), access, shizukuStatus, local)
        SimpleProgressCard(progress, step, state)
        GlassCard { Text("Shizuku Setup", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Status", shizukuStatus); Text(ShizukuSetup.shortHint(context), color = SoftText); Spacer(Modifier.height(10.dp)); Button(onClick = { ShizukuSetup.requestPermission(); refreshAccess(engine) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Grant Permission", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { ShizukuSetup.openApp(context) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Shizuku", fontWeight = FontWeight.Bold) } }
        GlassCard { Text("Advanced", fontSize = 20.sp, fontWeight = FontWeight.Bold); Button(onClick = { refreshManifest(false) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Refresh Manifest", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { prefs.edit().putInt("local_version_code", 1).remove("update_state").apply(); local = 1; latest = 0; mainLabel = "Start Auto Update"; log("Local version di-reset ke v1 untuk retest.") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Retest v1 → Latest", fontWeight = FontWeight.Bold) } }
        GlassCard { Text("Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun SimpleMore(api: CommunityApi, openAuto: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var status by remember { mutableStateOf("More tools") }
    var local by remember { mutableStateOf(prefs.getInt("local_version_code", 1)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("More", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Advanced tools dipindah ke sini agar Auto tidak ramai.", color = SoftText) }
        GlassCard { Text("Quick Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Current local", "v$local"); Button(onClick = { prefs.edit().putInt("local_version_code", 1).remove("update_state").remove("update_last_error").apply(); local = 1; status = "Reset ke v1. Buka Auto untuk update ulang." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Reset Local Version to v1", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openAuto, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Go to Auto Update", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Legacy Advanced Updater", fontWeight = FontWeight.Bold) } }
        GlassCard { Text("Developer", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status); InfoLine("Login", "Masih nonaktif untuk development.") }
    }
}

@Composable
private fun SimpleSystemCard(gameInstalled: Boolean, access: String, shizuku: String, local: Int) { GlassCard { Text("Status", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Game", if (gameInstalled) "Detected" else "Missing"); InfoLine("Access", access); InfoLine("Shizuku", shizuku); InfoLine("Local", "v$local") } }
@Composable
private fun SimpleProgressCard(progress: Int, label: String, state: String) { GlassCard { Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("State", state); InfoLine("Step", label); Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp); Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp))) } } }
