package com.drmacze.f16launcher

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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class P40Page(val label: String, val icon: String) { Setup("Setup", "↻"), Help("Help", "?"), More("More", "☰") }
private enum class P40State(val title: String) { Checking("Checking"), NeedApk("Need Game APK"), NeedContent("Need DATA/OBB"), NeedShizuku("Need Shizuku"), NeedPermission("Need Permission"), UpdateAvailable("Update Available"), Ready("Ready"), Working("Working"), Recovery("Need Recovery"), Offline("Offline") }

@Composable
fun Phase40PublicSetupShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P40Page.Setup) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "phase40", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                P40Page.Setup -> P40AutoSetup()
                P40Page.Help -> P40Help()
                P40Page.More -> P40More(api) { page = P40Page.Setup }
            }
        }
        P40Nav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun P40Card(content: @Composable ColumnScope.() -> Unit) = GlassCard(Modifier.fillMaxWidth(), content)

@Composable
private fun P40Nav(page: P40Page, onPage: (P40Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp, tonalElevation = 0.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            P40Page.values().forEach { item ->
                val selected = item == page
                Button(onClick = { onPage(item) }, modifier = Modifier.weight(1f).height(if (selected) 54.dp else 48.dp), shape = RoundedCornerShape(25.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText), elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(item.icon, fontSize = 16.sp); Text(item.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                }
            }
        }
    }
}

@Composable
private fun P40AutoSetup() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    val engine = remember { DevPatchEngine(context) {} }
    val installer = remember { PublicInstallManager(context) }
    var manifest by remember { mutableStateOf<PublicInstallManifest?>(null) }
    var state by remember { mutableStateOf(P40State.Checking) }
    var message by remember { mutableStateOf("Mengecek DLavie setup...") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var gameInstalled by remember { mutableStateOf(false) }
    var contentInstalled by remember { mutableStateOf(false) }
    var local by remember { mutableStateOf(1) }
    var latest by remember { mutableStateOf(0) }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var access by remember { mutableStateOf("Checking...") }
    var updateState by remember { mutableStateOf(prefs.getString("update_state", "idle") ?: "idle") }
    var backupReady by remember { mutableStateOf(false) }
    var autoLaunch by remember { mutableStateOf(prefs.getBoolean("auto_launch_after_update", true)) }
    var logs by remember { mutableStateOf(listOf("DLavie Auto Setup siap.")) }

    fun log(text: String) { logs = (logs + text).takeLast(20); message = text }
    fun pct(value: Int, label: String) { progress = value.coerceIn(0, 100); step = label }
    fun label(): String = when (state) {
        P40State.NeedApk -> "Download DLavie 26"
        P40State.NeedContent -> "Install DLavie Data"
        P40State.NeedShizuku -> "Open Shizuku"
        P40State.NeedPermission -> "Grant Permission"
        P40State.UpdateAvailable -> "Update DLavie Data"
        P40State.Ready -> "Play FIFA 16"
        P40State.Recovery -> if (backupReady) "Auto Recover" else "Clear Error"
        P40State.Offline -> "Retry Check"
        P40State.Working -> "Working..."
        P40State.Checking -> "Checking..."
    }

    fun refresh(silent: Boolean = false) {
        state = P40State.Checking
        pct(10, "Checking")
        if (!silent) log("Mengecek setup...")
        scope.launch {
            val current = try { withContext(Dispatchers.IO) { installer.fetchInstallManifest() } } catch (t: Throwable) { state = P40State.Offline; pct(0, "Install manifest failed"); log("Gagal baca install manifest: ${t.message}"); return@launch }
            manifest = current
            gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
            shizuku = ShizukuSetup.status(context)
            access = engine.accessMode()
            local = engine.localVersion()
            backupReady = engine.latestBackupRoot().isNotBlank()
            updateState = prefs.getString("update_state", "idle") ?: "idle"
            if (gameInstalled) prefs.edit().putBoolean("awaiting_apk_install", false).apply()
            if (!gameInstalled) { contentInstalled = false; state = P40State.NeedApk; pct(20, "Need APK"); log("FIFA 16 DLavie belum terinstall. Download dari DLavie Launcher."); return@launch }
            if (updateState == "failed") { state = P40State.Recovery; pct(progress, "Recovery needed"); log("Update sebelumnya gagal. Jalankan recovery."); return@launch }
            if (shizuku == "Not Installed" || shizuku == "Need Start") { state = P40State.NeedShizuku; pct(30, "Need Shizuku"); log(ShizukuSetup.shortHint(context)); return@launch }
            if (shizuku == "Need Permission") { state = P40State.NeedPermission; pct(40, "Need permission"); log(ShizukuSetup.shortHint(context)); return@launch }
            contentInstalled = withContext(Dispatchers.IO) { PublicContentInstaller(context, {}, { _, _ -> }).hasOfficialContent(current) }
            if (!contentInstalled) { state = P40State.NeedContent; pct(50, "Need DATA/OBB"); log("DATA/OBB resmi DLavie belum terpasang. Tekan Install DLavie Data."); return@launch }
            try {
                pct(70, "Checking data update")
                val latestManifest = withContext(Dispatchers.IO) { engine.fetchManifest() }
                latest = latestManifest.optInt("latestVersionCode", local)
                local = engine.localVersion()
                state = if (latest > local) P40State.UpdateAvailable else P40State.Ready
                pct(100, "Ready")
                log(if (latest > local) "Update data tersedia: v$local → v$latest" else "Semua siap. DLavie 26 v$local.")
            } catch (t: Throwable) { state = P40State.Offline; pct(0, "Update manifest failed"); log("Gagal cek update data: ${t.message}") }
        }
    }

    fun downloadApk() {
        val current = manifest
        if (current == null) { refresh(false); return }
        state = P40State.Working
        pct(5, "Downloading APK")
        log("Download APK DLavie 26...")
        scope.launch {
            try {
                val apk = withContext(Dispatchers.IO) { installer.downloadAsset(current.apk) { pct(it, "Downloading APK") } }
                pct(100, "Open installer")
                prefs.edit().putBoolean("awaiting_apk_install", true).apply()
                log("APK selesai. Installer Android dibuka. Setelah install selesai, kembali ke DLavie; status akan dicek otomatis.")
                installer.openApkInstaller(apk)
            } catch (t: Throwable) { state = P40State.NeedApk; pct(0, "APK download failed"); log("Download APK gagal: ${t.message}") }
        }
    }

    fun installContent() {
        val current = manifest
        if (current == null) { refresh(false); return }
        state = P40State.Working
        pct(3, "Install DATA/OBB")
        log("Install DATA/OBB DLavie dimulai...")
        val contentInstaller = PublicContentInstaller(
            context,
            { text -> scope.launch(Dispatchers.Main) { log(text) } },
            { p, s -> scope.launch(Dispatchers.Main) { pct(p, s) } }
        )
        scope.launch {
            try {
                withContext(Dispatchers.IO) { contentInstaller.installDataAndObb(current) }
                contentInstalled = true
                state = P40State.Ready
                pct(100, "Ready")
                log("DATA/OBB DLavie selesai. Siap main.")
                if (autoLaunch) launchGame(context)
            } catch (t: Throwable) { state = P40State.NeedContent; pct(progress, "Content install failed"); log("Install DATA/OBB gagal: ${t.message}") }
        }
    }

    fun recoverOrClear() { prefs.edit().remove("update_state").remove("update_last_error").apply(); updateState = "idle"; log("State dibersihkan. Cek ulang."); refresh(true) }
    fun applyUpdate() {
        state = P40State.Working
        prefs.edit().putString("update_state", "running").apply()
        updateState = "running"
        pct(15, "Updating data")
        log("Update data DLavie dimulai...")
        scope.launch { try { withContext(Dispatchers.IO) { engine.applyAvailableUpdates() }; local = engine.localVersion(); prefs.edit().putString("update_state", "done").remove("update_last_error").apply(); updateState = "done"; state = P40State.Ready; pct(100, "Ready"); log("Update selesai. Siap main."); if (autoLaunch) launchGame(context) } catch (t: Throwable) { prefs.edit().putString("update_state", "failed").putString("update_last_error", t.message ?: "unknown").apply(); updateState = "failed"; state = P40State.Recovery; pct(progress, "Failed"); log("Update gagal: ${t.message}") } }
    }

    fun mainAction() {
        when (state) {
            P40State.NeedApk -> downloadApk()
            P40State.NeedContent -> installContent()
            P40State.NeedShizuku -> { ShizukuSetup.openApp(context); log("Buka Shizuku, start service, lalu kembali ke DLavie.") }
            P40State.NeedPermission -> { ShizukuSetup.requestPermission(); log("Pilih Allow di dialog Shizuku, lalu tekan tombol utama lagi.") }
            P40State.UpdateAvailable -> applyUpdate()
            P40State.Ready -> launchGame(context)
            P40State.Recovery -> recoverOrClear()
            P40State.Offline, P40State.Checking -> refresh(false)
            P40State.Working -> log("Proses sedang berjalan. Tunggu selesai.")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val waitingApkInstall = prefs.getBoolean("awaiting_apk_install", false)
                val installedNow = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
                if (waitingApkInstall || state == P40State.Working || installedNow != gameInstalled) refresh(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { refresh(true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P40Card { Text("DLavie Auto Setup", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Public FIFA 16 Mobile installer", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); P40StatusPill(state); Spacer(Modifier.height(10.dp)); Text(message, color = SoftText); Spacer(Modifier.height(16.dp)); Button(enabled = state != P40State.Working, onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) { Text(label(), fontWeight = FontWeight.Black, fontSize = 17.sp) } }
        P40Readiness(gameInstalled, contentInstalled, shizuku, access, local, latest, updateState, backupReady, autoLaunch)
        P40ContentCard(manifest)
        P40Progress(progress, step)
        P40Card { Text("Shizuku Helper", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(ShizukuSetup.shortHint(context), color = SoftText); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { Button(onClick = { ShizukuSetup.requestPermission(); refresh(true) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Grant", fontWeight = FontWeight.Bold) }; Button(onClick = { ShizukuSetup.openApp(context) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open", fontWeight = FontWeight.Bold) } } }
        P40Card { Text("Quick Controls", fontSize = 20.sp, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { Button(onClick = { refresh(false) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Recheck", fontWeight = FontWeight.Bold) }; Button(onClick = { autoLaunch = !autoLaunch; prefs.edit().putBoolean("auto_launch_after_update", autoLaunch).apply(); log(if (autoLaunch) "Auto launch ON." else "Auto launch OFF.") }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text(if (autoLaunch) "Auto ON" else "Auto OFF", fontWeight = FontWeight.Bold) } } }
        P40Card { Text("Recent Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.takeLast(5).forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P40StatusPill(state: P40State) { val color = when (state) { P40State.Ready -> NeonGreen; P40State.UpdateAvailable -> CandyCyan; P40State.NeedApk, P40State.NeedContent, P40State.NeedShizuku, P40State.NeedPermission -> Color(0xFFFFB84D); P40State.Recovery, P40State.Offline -> Color(0xFFFF5269); P40State.Working, P40State.Checking -> CandyBlue }; Surface(shape = RoundedCornerShape(22.dp), color = Color(0x33101827), border = BorderStroke(1.dp, color)) { Text(state.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold) } }
@Composable
private fun P40Readiness(game: Boolean, content: Boolean, shizuku: String, access: String, local: Int, latest: Int, updateState: String, backup: Boolean, autoLaunch: Boolean) = P40Card { Text("Setup Readiness", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Game", if (game) "Installed" else "Need download"); InfoLine("DATA/OBB", if (content) "Official DLavie" else "Need install"); InfoLine("Data update", if (latest > 0 && local >= latest) "Ready" else "Check required"); InfoLine("Shizuku", shizuku); InfoLine("Access", access); InfoLine("Version", if (latest > 0) "v$local / latest v$latest" else "v$local"); InfoLine("Backup", if (backup) "Ready" else "None yet"); InfoLine("Auto launch", if (autoLaunch) "On" else "Off"); InfoLine("Community", "After production launch") }
@Composable
private fun P40ContentCard(manifest: PublicInstallManifest?) = P40Card { Text("DLavie Content Source", fontSize = 20.sp, fontWeight = FontWeight.Bold); if (manifest == null) Text("Manifest belum terbaca.", color = SoftText) else { InfoLine("Product", manifest.productName); InfoLine("APK", manifest.apk.versionName); InfoLine("DATA", manifest.data.versionName); InfoLine("OBB", manifest.obb.versionName); Text("Semua file wajib berasal dari manifest DLavie dan diverifikasi SHA-256.", color = SoftText, fontSize = 13.sp) } }
@Composable
private fun P40Progress(progress: Int, label: String) = P40Card { Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Step", label); Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp); Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp))) } }
@Composable
private fun P40Help() { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { P40Card { Text("Help", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Alur publik DLavie", color = CandyCyan); Spacer(Modifier.height(12.dp)); Text("User cukup install DLavie Launcher. APK game, DATA, OBB, dan update akan diarahkan dari manifest DLavie.", color = SoftText) }; P40Card { Text("Alur setup", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("1", "Download DLavie Launcher."); InfoLine("2", "Tekan tombol utama di Setup."); InfoLine("3", "Install APK game jika belum ada."); InfoLine("4", "Tekan Install DLavie Data untuk memasang DATA/OBB resmi."); InfoLine("5", "Tekan Play FIFA 16 saat Ready.") } } }
@Composable
private fun P40More(api: CommunityApi, openSetup: () -> Unit) { val context = LocalContext.current; val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }; var local by remember { mutableStateOf(prefs.getInt("local_version_code", 1)) }; var status by remember { mutableStateOf("Advanced tools ready.") }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { P40Card { Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Developer tools disimpan di sini agar Setup tetap bersih.", color = SoftText) }; P40Card { Text("Advanced Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Local", "v$local"); Button(onClick = { prefs.edit().putInt("local_version_code", 1).remove("update_state").remove("update_last_error").apply(); local = 1; status = "Local di-reset ke v1." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Dev Retest: Reset Local to v1", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { prefs.edit().remove("update_state").remove("update_last_error").apply(); status = "Update state dibersihkan." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Clear Update State", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openSetup, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Setup", fontWeight = FontWeight.Bold) } }; P40Card { Text("Roadmap", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Now", "Public installer"); InfoLine("Dashboard", "Separate developer app later"); InfoLine("Login/community", "After production launch"); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status) } } }
