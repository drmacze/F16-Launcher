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

private enum class FinalPage(val label: String, val icon: String) {
    Auto("Auto", "↻"), Help("Help", "?"), More("More", "☰")
}

private enum class FinalStatus(val title: String, val hint: String) {
    CHECKING("Checking", "DLavie sedang mengecek game, Shizuku, dan update."),
    NEED_GAME("Game Missing", "FIFA 16 belum terdeteksi di perangkat."),
    NEED_SHIZUKU("Need Shizuku", "Aktifkan Shizuku dulu agar DLavie bisa menulis patch ke Android/data."),
    NEED_PERMISSION("Need Permission", "Shizuku sudah berjalan. Berikan izin untuk DLavie."),
    UPDATE_AVAILABLE("Update Available", "Update tersedia dan siap dipasang."),
    UP_TO_DATE("Ready", "Game sudah versi terbaru. Kamu bisa langsung main."),
    UPDATING("Updating", "Jangan tutup aplikasi sampai proses selesai."),
    ERROR("Need Recovery", "Update gagal/terputus. Jalankan recovery atau clear state."),
    OFFLINE("Offline", "Manifest GitHub belum terbaca. Coba refresh koneksi.")
}

@Composable
fun Phase39FinalShell(api: CommunityApi) {
    var page by remember { mutableStateOf(FinalPage.Auto) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "phase39", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                FinalPage.Auto -> FinalAutoPilot()
                FinalPage.Help -> FinalHelp()
                FinalPage.More -> FinalMore(api) { page = FinalPage.Auto }
            }
        }
        FinalNav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun FinalNav(page: FinalPage, onPage: (FinalPage) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp, tonalElevation = 0.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FinalPage.values().forEach { item ->
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
private fun FinalAutoPilot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }

    var local by remember { mutableStateOf(1) }
    var latest by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(FinalStatus.CHECKING) }
    var message by remember { mutableStateOf("Menyiapkan Auto Pilot...") }
    var mainLabel by remember { mutableStateOf("Check & Fix") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var access by remember { mutableStateOf("Checking...") }
    var updateState by remember { mutableStateOf(prefs.getString("update_state", "idle") ?: "idle") }
    var backupReady by remember { mutableStateOf(false) }
    var autoLaunch by remember { mutableStateOf(prefs.getBoolean("auto_launch_after_update", true)) }
    var logs by remember { mutableStateOf(listOf("Auto Pilot siap.")) }

    fun log(text: String) {
        logs = (logs + text).takeLast(30)
        message = text
    }

    fun setProgress(value: Int, label: String) {
        progress = value.coerceIn(0, 100)
        step = label
    }

    val engine = remember { DevPatchEngine(context) { text -> scope.launch(Dispatchers.Main) { log(text) } } }

    fun computeButton() {
        mainLabel = when (status) {
            FinalStatus.NEED_GAME -> "Open FIFA 16"
            FinalStatus.NEED_SHIZUKU -> "Open Shizuku"
            FinalStatus.NEED_PERMISSION -> "Grant Permission"
            FinalStatus.UPDATE_AVAILABLE -> "Update Now"
            FinalStatus.UP_TO_DATE -> "Play FIFA 16"
            FinalStatus.ERROR -> if (backupReady) "Auto Recover" else "Clear Error"
            FinalStatus.OFFLINE -> "Retry Check"
            FinalStatus.UPDATING -> "Updating..."
            FinalStatus.CHECKING -> "Checking..."
        }
    }

    fun refresh(silent: Boolean = false) {
        setProgress(12, "Checking")
        status = FinalStatus.CHECKING
        if (!silent) log("Mengecek status...")
        scope.launch {
            val gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
            shizuku = ShizukuSetup.status(context)
            access = engine.accessMode()
            local = engine.localVersion()
            backupReady = engine.latestBackupRoot().isNotBlank()
            updateState = prefs.getString("update_state", "idle") ?: "idle"

            if (!gameInstalled) {
                status = FinalStatus.NEED_GAME
                setProgress(0, "Game missing")
                log("FIFA 16 belum terdeteksi.")
                computeButton()
                return@launch
            }
            if (updateState == "failed") {
                status = FinalStatus.ERROR
                setProgress(progress, "Recovery needed")
                log("Update sebelumnya gagal. Gunakan Auto Recover.")
                computeButton()
                return@launch
            }
            if (shizuku == "Not Installed" || shizuku == "Need Start") {
                status = FinalStatus.NEED_SHIZUKU
                setProgress(20, "Need Shizuku")
                log(ShizukuSetup.shortHint(context))
                computeButton()
                return@launch
            }
            if (shizuku == "Need Permission") {
                status = FinalStatus.NEED_PERMISSION
                setProgress(35, "Need permission")
                log(ShizukuSetup.shortHint(context))
                computeButton()
                return@launch
            }

            try {
                setProgress(55, "Reading manifest")
                val manifest = withContext(Dispatchers.IO) { engine.fetchManifest() }
                latest = manifest.optInt("latestVersionCode", local)
                local = engine.localVersion()
                access = engine.accessMode()
                status = if (latest > local) FinalStatus.UPDATE_AVAILABLE else FinalStatus.UP_TO_DATE
                setProgress(100, "Ready")
                log(if (latest > local) "Update tersedia: v$local → v$latest" else "Siap. FIFA 16 sudah v$local.")
            } catch (t: Throwable) {
                status = FinalStatus.OFFLINE
                setProgress(0, "Manifest failed")
                log("Gagal membaca update GitHub: ${t.message}")
            }
            computeButton()
        }
    }

    fun clearError() {
        prefs.edit().remove("update_state").remove("update_last_error").apply()
        updateState = "idle"
        status = FinalStatus.CHECKING
        log("Error state dibersihkan.")
        refresh(true)
    }

    fun recover() {
        status = FinalStatus.UPDATING
        setProgress(15, "Recovering")
        log("Recovery dimulai...")
        scope.launch {
            try {
                if (backupReady) withContext(Dispatchers.IO) { engine.restoreLastBackup() }
                prefs.edit().remove("update_state").remove("update_last_error").apply()
                setProgress(100, "Recovered")
                log("Recovery selesai. Mengecek ulang...")
                refresh(true)
            } catch (t: Throwable) {
                status = FinalStatus.ERROR
                log("Recovery gagal: ${t.message}")
                computeButton()
            }
        }
    }

    fun update(retest: Boolean = false) {
        status = FinalStatus.UPDATING
        prefs.edit().putString("update_state", "running").apply()
        updateState = "running"
        setProgress(10, if (retest) "Retest" else "Updating")
        mainLabel = "Updating..."
        log(if (retest) "Retest update dimulai..." else "Update dimulai...")
        scope.launch {
            try {
                if (retest) withContext(Dispatchers.IO) { engine.resetLocalVersion(1) }
                setProgress(35, "Applying patch")
                withContext(Dispatchers.IO) { engine.applyAvailableUpdates() }
                local = engine.localVersion()
                prefs.edit().putString("update_state", "done").remove("update_last_error").apply()
                updateState = "done"
                backupReady = engine.latestBackupRoot().isNotBlank()
                setProgress(100, "Completed")
                status = FinalStatus.UP_TO_DATE
                log("Update selesai. Local v$local")
                if (autoLaunch) launchGame(context) else computeButton()
            } catch (t: Throwable) {
                prefs.edit().putString("update_state", "failed").putString("update_last_error", t.message ?: "unknown").apply()
                backupReady = engine.latestBackupRoot().isNotBlank()
                updateState = "failed"
                status = FinalStatus.ERROR
                setProgress(progress, "Failed")
                log("Update gagal: ${t.message}")
                computeButton()
            }
        }
    }

    fun mainAction() {
        when (status) {
            FinalStatus.NEED_GAME -> launchGame(context)
            FinalStatus.NEED_SHIZUKU -> { ShizukuSetup.openApp(context); log("Buka Shizuku, start service, lalu kembali ke DLavie.") }
            FinalStatus.NEED_PERMISSION -> { ShizukuSetup.requestPermission(); log("Pilih Allow di dialog Shizuku, lalu tekan tombol utama lagi.") }
            FinalStatus.UPDATE_AVAILABLE -> update(false)
            FinalStatus.UP_TO_DATE -> launchGame(context)
            FinalStatus.ERROR -> if (backupReady) recover() else clearError()
            FinalStatus.OFFLINE -> refresh(false)
            FinalStatus.CHECKING -> refresh(false)
            FinalStatus.UPDATING -> log("Update sedang berjalan. Tunggu selesai.")
        }
    }

    LaunchedEffect(Unit) { refresh(true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Auto Pilot", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 3.9 • Final Cleanup", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            FinalStatusPill(status)
            Spacer(Modifier.height(10.dp))
            Text(message, color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(enabled = status != FinalStatus.UPDATING, onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text(mainLabel, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
        }
        FinalStatusCard(shizuku, access, local, latest, updateState, backupReady, autoLaunch)
        FinalProgress(progress, step)
        GlassCard {
            Text("Shizuku Helper", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(ShizukuSetup.shortHint(context), color = SoftText)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { ShizukuSetup.requestPermission(); refresh(true) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Grant", fontWeight = FontWeight.Bold) }
                Button(onClick = { ShizukuSetup.openApp(context) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open", fontWeight = FontWeight.Bold) }
            }
        }
        GlassCard {
            Text("Quick Controls", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { refresh(false) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Recheck", fontWeight = FontWeight.Bold) }
                Button(onClick = { autoLaunch = !autoLaunch; prefs.edit().putBoolean("auto_launch_after_update", autoLaunch).apply(); log(if (autoLaunch) "Auto launch ON." else "Auto launch OFF.") }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text(if (autoLaunch) "Auto ON" else "Auto OFF", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { update(true) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Dev Retest v1 → Latest", fontWeight = FontWeight.Bold) }
        }
        GlassCard { Text("Recent Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.takeLast(5).forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun FinalStatusPill(status: FinalStatus) {
    val color = when (status) {
        FinalStatus.UP_TO_DATE -> NeonGreen
        FinalStatus.UPDATE_AVAILABLE -> CandyCyan
        FinalStatus.NEED_SHIZUKU, FinalStatus.NEED_PERMISSION -> Color(0xFFFFB84D)
        FinalStatus.ERROR, FinalStatus.OFFLINE, FinalStatus.NEED_GAME -> Color(0xFFFF5269)
        FinalStatus.UPDATING, FinalStatus.CHECKING -> CandyBlue
    }
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0x33101827), border = BorderStroke(1.dp, color)) {
        Text(status.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FinalStatusCard(shizuku: String, access: String, local: Int, latest: Int, updateState: String, backup: Boolean, autoLaunch: Boolean) {
    GlassCard {
        Text("Status", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        InfoLine("Shizuku", shizuku)
        InfoLine("Access", access)
        InfoLine("Version", if (latest > 0) "v$local / latest v$latest" else "v$local")
        InfoLine("Update state", updateState)
        InfoLine("Backup", if (backup) "Ready" else "None")
        InfoLine("Auto launch", if (autoLaunch) "On" else "Off")
    }
}

@Composable
private fun FinalProgress(progress: Int, label: String) {
    GlassCard {
        Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        InfoLine("Step", label)
        Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) {
            Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun FinalHelp() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("Help", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Panduan sederhana", color = CandyCyan); Spacer(Modifier.height(12.dp)); Text("Untuk user normal, cukup pakai halaman Auto. Tombol utama akan berubah sendiri sesuai kebutuhan.", color = SoftText) }
        GlassCard { Text("Alur final", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("1", "Tekan tombol utama di Auto Pilot."); InfoLine("2", "Kalau diminta Shizuku, tekan Open/Grant."); InfoLine("3", "Setelah Ready, tombol utama akan update atau menjalankan FIFA 16."); InfoLine("4", "Jika gagal, tombol berubah menjadi Auto Recover.") }
        GlassCard { Text("Batas Android", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("DLavie bisa membuka Shizuku dan meminta izin, tapi Android tetap mewajibkan user menyalakan service Shizuku dan memberi izin secara manual.", color = SoftText) }
    }
}

@Composable
private fun FinalMore(api: CommunityApi, openAuto: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var local by remember { mutableStateOf(prefs.getInt("local_version_code", 1)) }
    var status by remember { mutableStateOf("Advanced tools ready.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Tools lanjutan disimpan di sini agar Auto tetap simpel.", color = SoftText) }
        GlassCard { Text("Advanced Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Local", "v$local"); Button(onClick = { prefs.edit().putInt("local_version_code", 1).remove("update_state").remove("update_last_error").apply(); local = 1; status = "Local di-reset ke v1." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Reset Local Version to v1", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { prefs.edit().remove("update_state").remove("update_last_error").apply(); status = "Update state dibersihkan." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Clear Update State", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openAuto, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Auto", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Legacy Advanced Updater", fontWeight = FontWeight.Bold) } }
        GlassCard { Text("Developer", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status); InfoLine("Login", "Masih nonaktif sampai Phase 4.") }
    }
}
