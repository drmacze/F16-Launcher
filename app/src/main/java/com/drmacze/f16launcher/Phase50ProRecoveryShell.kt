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
fun Phase50ProRecoveryShell(api: CommunityApi) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { PublicInstallManager(context) }
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var manifest by remember { mutableStateOf<PublicInstallManifest?>(null) }
    var message by remember { mutableStateOf("Mode recovery lengkap: install APK, pasang OBB stabil, first setup, lalu DATA final.") }
    var step by remember { mutableStateOf("Ready") }
    var progress by remember { mutableStateOf(0) }
    var working by remember { mutableStateOf(false) }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var report by remember { mutableStateOf("") }
    var gameInstalled by remember { mutableStateOf(isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) }

    fun refreshState() {
        shizuku = ShizukuSetup.status(context)
        gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
        if (shizuku != "Ready") {
            message = when (shizuku) {
                "Need Permission" -> "Shizuku aktif tapi izin belum diberikan. Tekan Grant Shizuku."
                "Need Start" -> "Shizuku belum berjalan. Buka Shizuku lalu Start."
                "Not Installed" -> "Shizuku belum terpasang. Install/buka Shizuku dulu."
                else -> "Status Shizuku: $shizuku"
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshState()
        try {
            manifest = withContext(Dispatchers.IO) { manager.fetchInstallManifest() }
        } catch (t: Throwable) {
            message = "Manifest gagal: ${t.message}"
        }
    }

    fun installer() = PublicContentInstaller(
        context,
        { text -> scope.launch(Dispatchers.Main) { message = text } },
        { p, s -> scope.launch(Dispatchers.Main) { progress = p; step = s } }
    )

    fun runShizukuJob(label: String, block: suspend () -> Unit) {
        refreshState()
        if (shizuku != "Ready") {
            step = "Need Shizuku"
            progress = 0
            return
        }
        working = true
        message = label
        scope.launch {
            try {
                block()
            } catch (t: Throwable) {
                message = "$label gagal: ${t.message}"
                step = "Failed"
            } finally {
                working = false
                refreshState()
            }
        }
    }

    fun runNormalJob(label: String, block: suspend () -> Unit) {
        working = true
        message = label
        scope.launch {
            try {
                block()
            } catch (t: Throwable) {
                message = "$label gagal: ${t.message}"
                step = "Failed"
            } finally {
                working = false
                refreshState()
            }
        }
    }

    fun installApkGame() {
        val current = manifest ?: return
        runNormalJob("Menyiapkan APK game DLavie 26...") {
            step = "Download APK"
            progress = 1
            val apk = withContext(Dispatchers.IO) {
                manager.downloadAsset(current.apk) { p ->
                    scope.launch(Dispatchers.Main) {
                        progress = p
                        step = "Download APK"
                    }
                }
            }
            progress = 100
            step = "Open APK installer"
            message = "APK siap. Android Package Installer dibuka. Setelah install selesai, kembali ke launcher. Jangan buka game dulu."
            manager.openApkInstaller(apk)
        }
    }

    fun cleanRuntimeOnly() {
        val current = manifest ?: return
        runShizukuJob("Membersihkan runtime DATA/OBB yang rusak.") {
            withContext(Dispatchers.IO) { installer().cleanRuntime(current) }
            progress = 100
            step = "Runtime clean"
            message = "Runtime bersih. Sekarang install APK jika belum, lalu Install OBB Stable."
        }
    }

    fun installObbStable() {
        val current = manifest ?: return
        runShizukuJob("Memasang OBB stabil main.13 dan patch.26.") {
            withContext(Dispatchers.IO) { installer().installObbOnly(current) }
            prefs.edit().putBoolean("first_run_done", false).putBoolean("dlavie_data_installed", false).apply()
            progress = 100
            step = "OBB ready"
            message = "OBB stabil selesai. Sekarang Open First Setup / Game. Kalau installer hijau muncul, tunggu sampai selesai."
        }
    }

    fun cleanAndInstallObb() {
        val current = manifest ?: return
        runShizukuJob("Membersihkan runtime lalu memasang OBB stabil.") {
            val content = installer()
            withContext(Dispatchers.IO) { content.cleanRuntime(current) }
            progress = 20
            step = "Install OBB"
            withContext(Dispatchers.IO) { content.installObbOnly(current) }
            prefs.edit().putBoolean("first_run_done", false).putBoolean("dlavie_data_installed", false).apply()
            progress = 100
            step = "OBB ready"
            message = "Runtime bersih dan OBB stabil sudah dipasang. Sekarang Open First Setup / Game."
        }
    }

    fun installDataFinal() {
        val current = manifest ?: return
        runShizukuJob("Install DATA final dimulai. Jangan tutup app sampai selesai.") {
            val content = installer()
            withContext(Dispatchers.IO) { content.installDataOnly(current) }
            prefs.edit().putBoolean("first_run_done", true).putBoolean("dlavie_data_installed", true).apply()
            progress = 100
            step = "Ready"
            message = "DATA final selesai. Sekarang Play/Test FIFA 16. Jika masih gagal, tekan Collect Doctor Report."
        }
    }

    fun collectDoctorReport() {
        runShizukuJob("Mengambil Doctor Report via Shizuku...") {
            val text = withContext(Dispatchers.IO) { installer().doctorReport() }
            report = text.takeLast(6000)
            progress = 100
            step = "Report ready"
            message = "Doctor Report sudah diambil. Kirim isi report/log ini kalau game masih gagal."
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("DLavie Pro Recovery", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("APK + clean runtime + OBB + DATA final", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text(message, color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(enabled = !working && manifest != null, onClick = { installApkGame() }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text("1. Install APK Game", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !working && manifest != null, onClick = { cleanRuntimeOnly() }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text("2. Clean Runtime Only", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !working && manifest != null, onClick = { installObbStable() }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text("3. Install OBB Stable", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !working && manifest != null, onClick = { cleanAndInstallObb() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263D63), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text("Clean + Install OBB", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !working && gameInstalled, onClick = { launchGame(context) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue, disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text("4. Open First Setup / Game", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !working && manifest != null, onClick = { installDataFinal() }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text("5. Install DATA Final", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = !working && gameInstalled, onClick = { launchGame(context) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text("6. Play / Test FIFA 16", fontWeight = FontWeight.Bold)
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Status", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Game APK", if (gameInstalled) "Installed" else "Not installed")
            InfoLine("Shizuku", shizuku)
            InfoLine("Manifest", if (manifest != null) "Ready" else "Loading/failed")
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Shizuku", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(ShizukuSetup.shortHint(context), color = SoftText)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { ShizukuSetup.openApp(context) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Shizuku", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { ShizukuSetup.requestPermission(); refreshState() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Grant Shizuku", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { refreshState() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Recheck Status", fontWeight = FontWeight.Bold) }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Step", step)
            Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Doctor", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Kalau masih gagal, ambil report untuk melihat error asli.", color = SoftText)
            Spacer(Modifier.height(10.dp))
            Button(enabled = !working, onClick = { collectDoctorReport() }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5269))) { Text("Collect Doctor Report", fontWeight = FontWeight.Bold) }
            if (report.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(report, color = SoftText, fontSize = 11.sp)
            }
        }
    }
}
