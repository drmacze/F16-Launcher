package com.drmacze.f16launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Phase46RepairDataShell(api: CommunityApi) {
    RepairContentScreen()
}

@Composable
private fun RepairContentScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    val manager = remember { PublicInstallManager(context) }
    var message by remember { mutableStateOf("Mode content repair. Jika splash masih Messi, install updated OBB dulu, lalu DATA final.") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Ready") }
    var working by remember { mutableStateOf(false) }
    var obbDone by remember { mutableStateOf(false) }
    var dataDone by remember { mutableStateOf(prefs.getBoolean("dlavie_data_installed", false)) }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }

    fun refreshShizuku() {
        shizuku = ShizukuSetup.status(context)
        message = when (shizuku) {
            "Ready" -> "Shizuku siap. Untuk splash Messi, install updated OBB dari release terbaru lalu DATA final."
            "Need Permission" -> "Shizuku aktif, tapi izin launcher belum diberikan. Tekan Grant Shizuku."
            "Need Start" -> "Shizuku belum start. Buka Shizuku lalu start service."
            "Not Installed" -> "Shizuku belum terpasang. Install/buka Shizuku dulu."
            else -> "Status Shizuku: $shizuku"
        }
    }

    LaunchedEffect(Unit) {
        prefs.edit()
            .putBoolean("first_run_done", true)
            .putString("phase44_stage", "data_after_first_run")
            .apply()
        refreshShizuku()
    }

    fun requireShizuku(): Boolean {
        refreshShizuku()
        if (shizuku != "Ready") {
            step = "Need Shizuku"
            progress = 0
            return false
        }
        return true
    }

    fun installUpdatedObb() {
        if (!requireShizuku()) return
        working = true
        progress = 1
        step = "Preparing OBB"
        message = "Memasang updated OBB. Ini diperlukan karena splash awal dibaca dari OBB, bukan dari DATA eksternal."
        val installer = PublicContentInstaller(
            context,
            { text -> scope.launch(Dispatchers.Main) { message = text } },
            { p, s -> scope.launch(Dispatchers.Main) { progress = p; step = s } }
        )
        scope.launch {
            try {
                val manifest = withContext(Dispatchers.IO) { manager.fetchInstallManifest() }
                withContext(Dispatchers.IO) { installer.installObbOnly(manifest) }
                prefs.edit().putBoolean("dlavie_obb_installed", true).apply()
                obbDone = true
                progress = 100
                step = "OBB ready"
                message = "Updated OBB selesai. Sekarang jalankan FIFA sekali jika perlu, lalu install DATA final."
            } catch (t: Throwable) {
                message = "Install updated OBB gagal: ${t.message}"
                step = "Failed"
            } finally {
                working = false
                refreshShizuku()
            }
        }
    }

    fun installData() {
        if (!requireShizuku()) return
        working = true
        progress = 1
        step = "Preparing DATA"
        message = "Memasang DATA DLavie terakhir. Jangan tutup aplikasi sampai selesai."
        val installer = PublicContentInstaller(
            context,
            { text -> scope.launch(Dispatchers.Main) { message = text } },
            { p, s -> scope.launch(Dispatchers.Main) { progress = p; step = s } }
        )
        scope.launch {
            try {
                val manifest = withContext(Dispatchers.IO) { manager.fetchInstallManifest() }
                withContext(Dispatchers.IO) { installer.installDataOnly(manifest) }
                prefs.edit()
                    .putBoolean("dlavie_obb_installed", true)
                    .putBoolean("first_run_done", true)
                    .putBoolean("dlavie_data_installed", true)
                    .putString("phase44_stage", "ready")
                    .apply()
                progress = 100
                step = "Ready"
                message = "DATA DLavie selesai. Sekarang Play FIFA 16."
                dataDone = true
            } catch (t: Throwable) {
                message = "Install DATA gagal: ${t.message}"
                step = "Failed"
            } finally {
                working = false
                refreshShizuku()
            }
        }
    }

    fun playGame() {
        launchGame(context)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("DLavie Content Repair", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Fix OBB resource + DATA final", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text(message, color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(enabled = !working, onClick = { installUpdatedObb() }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text(if (working) "Working..." else "Install Updated OBB", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !working, onClick = { installData() }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text(if (working) "Working..." else "Install DATA DLavie", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !working, onClick = { playGame() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) {
                Text("Play / Test FIFA 16", fontWeight = FontWeight.Bold)
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Status", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("APK", "Installed")
            InfoLine("OBB", if (obbDone) "Updated this session" else "Needs updated release asset")
            InfoLine("First setup", "Done")
            InfoLine("DATA DLavie", if (dataDone) "Installed last" else "Need final install")
            InfoLine("Shizuku", shizuku)
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Shizuku Helper", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(ShizukuSetup.shortHint(context), color = SoftText)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { ShizukuSetup.openApp(context) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Shizuku", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { ShizukuSetup.requestPermission(); refreshShizuku() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Grant Shizuku", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { refreshShizuku() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Recheck Shizuku", fontWeight = FontWeight.Bold) }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Step", step)
            Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
    }
}
