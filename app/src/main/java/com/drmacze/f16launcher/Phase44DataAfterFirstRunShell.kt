package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.systemBarsPadding
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val P44_STAGE = "phase44_stage"
private const val P44_APK = "apk"
private const val P44_PERMISSION = "permission"
private const val P44_OBB = "obb"
private const val P44_FINALIZE = "finalize"
private const val P44_FIRST_RUN = "first_run"
private const val P44_DATA = "data_after_first_run"
private const val P44_READY = "ready"

private enum class P44Page(val label: String, val icon: String) { Setup("Setup", "↻"), Help("Help", "?"), More("More", "☰") }
private enum class P44Status(val title: String) { Checking("Checking"), Action("Action Needed"), Downloading("Downloading"), Installing("Installing"), Ready("Ready"), Error("Error") }

@Composable
fun Phase44DataAfterFirstRunShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P44Page.Setup) }
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        AnimatedContent(targetState = page, label = "phase44", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                P44Page.Setup -> P44Setup()
                P44Page.Help -> P44Help()
                P44Page.More -> P44More(api) { page = P44Page.Setup }
            }
        }
        P44Nav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun P44Card(content: @Composable ColumnScope.() -> Unit) = GlassCard(Modifier.fillMaxWidth(), content)

@Composable
private fun P44Nav(page: P44Page, onPage: (P44Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            P44Page.values().forEach { item ->
                val selected = item == page
                Button(onClick = { onPage(item) }, modifier = Modifier.weight(1f).height(if (selected) 54.dp else 48.dp), shape = RoundedCornerShape(25.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.icon, fontSize = 16.sp)
                        Text(item.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun P44Setup() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    val manager = remember { PublicInstallManager(context) }
    val crashReporter = remember { GameCrashReporter(context) }

    var manifest by remember { mutableStateOf<PublicInstallManifest?>(null) }
    var status by remember { mutableStateOf(P44Status.Checking) }
    var stage by remember { mutableStateOf(prefs.getString(P44_STAGE, P44_APK) ?: P44_APK) }
    var mode by remember { mutableStateOf("check") }
    var message by remember { mutableStateOf("Mengecek setup DLavie...") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var gameInstalled by remember { mutableStateOf(false) }
    var obbReady by remember { mutableStateOf(prefs.getBoolean("dlavie_obb_installed", false)) }
    var dataReady by remember { mutableStateOf(prefs.getBoolean("dlavie_data_installed", false)) }
    var firstRunDone by remember { mutableStateOf(prefs.getBoolean("first_run_done", false)) }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var logs by remember { mutableStateOf(listOf("DLavie setup siap. DATA dipasang terakhir setelah first setup.")) }

    fun setStage(next: String) { stage = next; prefs.edit().putString(P44_STAGE, next).apply() }
    fun log(text: String) { logs = (logs + text).takeLast(20); message = text }
    fun pct(value: Int, label: String) { progress = value.coerceIn(0, 100); step = label }
    fun openGameSettings() { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${DevPatchEngine.GAME_PACKAGE}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun contentInstaller() = PublicContentInstaller(context, { text -> scope.launch(Dispatchers.Main) { log(text) } }, { p, s -> scope.launch(Dispatchers.Main) { pct(p, s) } })

    suspend fun currentManifest(): PublicInstallManifest {
        val cached = manifest
        if (cached != null) return cached
        return withContext(Dispatchers.IO) { manager.fetchInstallManifest() }.also { manifest = it }
    }

    fun refresh(silent: Boolean = false) {
        scope.launch {
            status = P44Status.Checking
            pct(progress, "Checking")
            if (!silent) log("Mengecek tahap saat ini...")
            val current = try { currentManifest() } catch (t: Throwable) { status = P44Status.Error; mode = "retry"; pct(0, "Manifest failed"); log("Manifest gagal: ${t.message}"); return@launch }

            gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
            shizuku = ShizukuSetup.status(context)
            obbReady = prefs.getBoolean("dlavie_obb_installed", false)
            dataReady = prefs.getBoolean("dlavie_data_installed", false)
            firstRunDone = prefs.getBoolean("first_run_done", false)

            if (!gameInstalled) {
                setStage(P44_APK)
                val apk = withContext(Dispatchers.IO) { manager.downloadStatus(current.apk) }
                when {
                    apk.active -> { status = P44Status.Downloading; mode = "wait_apk"; pct(apk.progress, "Downloading APK"); log("Download APK berjalan: ${apk.progress}%") }
                    apk.done -> { status = P44Status.Action; mode = "install_apk"; pct(100, "APK ready"); log("APK selesai. Install APK, lalu jangan buka game dulu.") }
                    else -> { status = P44Status.Action; mode = "download_apk"; pct(0, "Need APK"); log("Langkah 1/7: download APK game.") }
                }
                return@launch
            }

            if (shizuku == "Not Installed" || shizuku == "Need Start") { status = P44Status.Action; mode = "open_shizuku"; pct(20, "Need Shizuku"); log("Buka Shizuku dan start service."); return@launch }
            if (shizuku == "Need Permission") { status = P44Status.Action; mode = "grant_shizuku"; pct(25, "Need Shizuku permission"); log("Beri izin Shizuku untuk DLavie Launcher."); return@launch }

            if (stage == P44_APK) setStage(P44_PERMISSION)
            if (stage == P44_PERMISSION) { status = P44Status.Action; mode = "permission"; pct(32, "Game permission"); log(if (prefs.getBoolean("game_permission_opened", false)) "Jika permission sudah diberi, tekan Permission Done." else "Buka permission FIFA 16 dan izinkan Files/Storage."); return@launch }

            if (!obbReady || stage == P44_OBB) {
                setStage(P44_OBB)
                val obb = withContext(Dispatchers.IO) { manager.downloadStatus(current.obb) }
                when {
                    obb.active -> { status = P44Status.Downloading; mode = "wait_obb"; pct(obb.progress, "Downloading OBB"); log("Download OBB berjalan: ${obb.progress}%") }
                    obb.done -> { status = P44Status.Action; mode = "install_obb"; pct(100, "OBB downloaded"); log("Install OBB dulu. DATA belum dipasang agar tidak ditimpa installer internal.") }
                    else -> { status = P44Status.Action; mode = "download_obb"; pct(0, "Need OBB"); log("Langkah 3/7: download OBB. Boleh keluar app saat download.") }
                }
                return@launch
            }

            if (stage != P44_FINALIZE && stage != P44_FIRST_RUN && stage != P44_DATA && stage != P44_READY) setStage(P44_FINALIZE)
            if (stage == P44_FINALIZE) {
                val apk = withContext(Dispatchers.IO) { manager.downloadStatus(current.apk) }
                when {
                    apk.active -> { status = P44Status.Downloading; mode = "wait_finalize"; pct(apk.progress, "Preparing APK"); log("Menyiapkan APK finalize: ${apk.progress}%") }
                    apk.done -> { status = P44Status.Action; mode = "finalize_apk"; pct(100, "APK ready"); log("Finalize APK. Jika muncul parse error tapi game lanjut, biarkan first setup berjalan.") }
                    else -> { status = P44Status.Action; mode = "download_finalize"; pct(0, "Need APK"); log("Menyiapkan APK untuk finalize.") }
                }
                return@launch
            }

            if (stage == P44_FIRST_RUN || !firstRunDone) {
                setStage(P44_FIRST_RUN)
                status = P44Status.Action; mode = "first_run"; pct(72, "First setup")
                log(if (prefs.getBoolean("first_run_started", false)) "Jika installer internal/green screen sudah selesai, kembali lalu tekan First Setup Done." else "Buka First Setup dan tunggu installer internal sampai selesai. DATA akan dipasang setelah ini.")
                return@launch
            }

            if (!dataReady || stage == P44_DATA) {
                setStage(P44_DATA)
                val data = withContext(Dispatchers.IO) { manager.downloadStatus(current.data) }
                when {
                    data.active -> { status = P44Status.Downloading; mode = "wait_data"; pct(data.progress, "Downloading DATA"); log("Download DATA berjalan: ${data.progress}%") }
                    data.done -> { status = P44Status.Action; mode = "install_data"; pct(100, "DATA downloaded"); log("Sekarang install DATA DLavie terakhir agar splash/mod terbaca.") }
                    else -> { status = P44Status.Action; mode = "download_data"; pct(0, "Need DATA"); log("Download DATA DLavie setelah first setup selesai.") }
                }
                return@launch
            }

            setStage(P44_READY)
            status = P44Status.Ready; mode = "play"; pct(100, "Ready"); log("Semua siap. DATA DLavie terpasang terakhir. Play FIFA 16.")
        }
    }

    fun startDownload(asset: InstallAsset, label: String) {
        status = P44Status.Downloading; mode = "wait_${label.lowercase()}"; pct(0, "Downloading $label"); log("Download $label dimulai. Boleh keluar app saat download.")
        scope.launch { try { withContext(Dispatchers.IO) { manager.downloadAsset(asset) { p -> scope.launch(Dispatchers.Main) { pct(p, "Downloading $label") } } }; log("Download $label selesai.") } catch (t: Throwable) { status = P44Status.Error; log("Download $label gagal: ${t.message}") }; refresh(true) }
    }

    fun installObb(current: PublicInstallManifest) {
        status = P44Status.Installing; mode = "installing_obb"; pct(0, "Copy OBB"); log("Copy OBB dimulai. Tunggu sampai selesai.")
        scope.launch { try { withContext(Dispatchers.IO) { contentInstaller().installObbOnly(current) }; prefs.edit().putBoolean("dlavie_obb_installed", true).apply(); obbReady = true; setStage(P44_FINALIZE); log("OBB selesai. Lanjut finalize/first setup.") } catch (t: Throwable) { status = P44Status.Error; log("Install OBB gagal: ${t.message}") }; refresh(true) }
    }

    fun installData(current: PublicInstallManifest) {
        status = P44Status.Installing; mode = "installing_data"; pct(0, "Copy DATA"); log("Copy DATA DLavie terakhir dimulai. Tunggu sampai selesai.")
        scope.launch { try { withContext(Dispatchers.IO) { contentInstaller().installDataOnly(current) }; prefs.edit().putBoolean("dlavie_data_installed", true).apply(); dataReady = true; setStage(P44_READY); log("DATA DLavie selesai dipasang setelah first setup.") } catch (t: Throwable) { status = P44Status.Error; log("Install DATA gagal: ${t.message}") }; refresh(true) }
    }

    fun mainAction() {
        val current = manifest ?: return refresh(false)
        when (mode) {
            "download_apk" -> startDownload(current.apk, "APK")
            "install_apk" -> { prefs.edit().putBoolean("awaiting_apk_install", true).apply(); manager.openApkInstaller(manager.cachedAssetFile(current.apk)) }
            "open_shizuku" -> ShizukuSetup.openApp(context)
            "grant_shizuku" -> ShizukuSetup.requestPermission()
            "permission" -> if (!prefs.getBoolean("game_permission_opened", false)) { prefs.edit().putBoolean("game_permission_opened", true).apply(); openGameSettings() } else { prefs.edit().putBoolean("game_permission_ready", true).apply(); setStage(P44_OBB); refresh(true) }
            "download_obb" -> startDownload(current.obb, "OBB")
            "install_obb" -> installObb(current)
            "download_finalize" -> startDownload(current.apk, "APK")
            "finalize_apk" -> { prefs.edit().putBoolean("awaiting_apk_finalize", true).apply(); manager.openApkInstaller(manager.cachedAssetFile(current.apk)) }
            "first_run" -> if (!prefs.getBoolean("first_run_started", false)) { prefs.edit().putBoolean("first_run_started", true).apply(); crashReporter.prepareLaunch(); launchGame(context) } else { prefs.edit().putBoolean("first_run_done", true).putBoolean("dlavie_data_installed", false).apply(); dataReady = false; setStage(P44_DATA); refresh(true) }
            "download_data" -> startDownload(current.data, "DATA")
            "install_data" -> installData(current)
            "play" -> { crashReporter.prepareLaunch(); launchGame(context) }
            else -> refresh(false)
        }
    }

    fun buttonLabel(): String = when (mode) {
        "download_apk" -> "Download DLavie 26"
        "install_apk" -> "Install APK"
        "open_shizuku" -> "Open Shizuku"
        "grant_shizuku" -> "Grant Shizuku"
        "permission" -> if (prefs.getBoolean("game_permission_opened", false)) "Permission Done" else "Open Permission"
        "download_obb" -> "Download OBB"
        "install_obb" -> "Install OBB"
        "download_finalize" -> "Prepare APK"
        "finalize_apk" -> "Finalize APK"
        "first_run" -> if (prefs.getBoolean("first_run_started", false)) "First Setup Done"
        else "Start First Setup"
        "download_data" -> "Download DATA"
        "install_data" -> "Install DATA DLavie"
        "play" -> "Play FIFA 16"
        else -> "Refresh"
    }

    val busy = status == P44Status.Checking || status == P44Status.Installing

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (prefs.getBoolean("awaiting_apk_install", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) { prefs.edit().putBoolean("awaiting_apk_install", false).apply(); setStage(P44_PERMISSION) }
                if (prefs.getBoolean("awaiting_apk_finalize", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) { prefs.edit().putBoolean("awaiting_apk_finalize", false).apply(); setStage(P44_FIRST_RUN); status = P44Status.Action; mode = "first_run"; pct(72, "First setup"); log("Finalize selesai/diabaikan. Lanjut Start First Setup."); return@LifecycleEventObserver }
                val crashReason = crashReporter.consumeFastReturn()
                if (crashReason != null && stage != P44_FIRST_RUN) { status = P44Status.Error; pct(0, "Game crash"); log("FIFA 16 crash: $crashReason") } else refresh(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { refresh(true) }
    LaunchedEffect(mode) { while (mode.startsWith("wait_")) { delay(2500L); refresh(true) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P44Card { Text("DLavie Auto Setup", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White); Text("DATA after first setup", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); P44StatusPill(status); Spacer(Modifier.height(10.dp)); Text(message, color = SoftText); Spacer(Modifier.height(16.dp)); Button(enabled = !busy, onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) { Text(buttonLabel(), fontWeight = FontWeight.Black, fontSize = 17.sp) } }
        P44Readiness(gameInstalled, obbReady, dataReady, firstRunDone, shizuku, stage)
        P44Progress(progress, step)
        P44Card { Text("Recent Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.takeLast(7).forEach { Text("- $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P44StatusPill(status: P44Status) { val color = when (status) { P44Status.Ready -> NeonGreen; P44Status.Action -> Color(0xFFFFB84D); P44Status.Downloading, P44Status.Installing, P44Status.Checking -> CandyBlue; P44Status.Error -> Color(0xFFFF5269) }; Surface(shape = RoundedCornerShape(22.dp), color = Color(0x33101827), border = BorderStroke(1.dp, color)) { Text(status.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold) } }
@Composable
private fun P44Readiness(game: Boolean, obb: Boolean, data: Boolean, firstRun: Boolean, shizuku: String, stage: String) = P44Card { Text("Setup Readiness", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Game APK", if (game) "Installed" else "Need download"); InfoLine("OBB", if (obb) "Ready" else "Need install"); InfoLine("First setup", if (firstRun) "Done" else "Need run"); InfoLine("DATA DLavie", if (data) "Installed last" else "Need final install"); InfoLine("Shizuku", shizuku); InfoLine("Stage", stage) }
@Composable
private fun P44Progress(progress: Int, label: String) = P44Card { Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Step", label); Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp); Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp))) } }
@Composable
private fun P44Help() { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { P44Card { Text("Help", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("DATA dipasang terakhir", color = CandyCyan); Spacer(Modifier.height(12.dp)); Text("Game akan extract OBB dulu. Setelah green installer selesai, DLavie Launcher memasang DATA terakhir supaya splash/mod tidak tertimpa oleh file bawaan OBB.", color = SoftText) }; P44Card { Text("Urutan aman", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("1", "APK + permission"); InfoLine("2", "OBB"); InfoLine("3", "Finalize / First Setup"); InfoLine("4", "Install DATA DLavie terakhir"); InfoLine("5", "Play") } } }
@Composable
private fun P44More(api: CommunityApi, openSetup: () -> Unit) { val context = LocalContext.current; val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }; var status by remember { mutableStateOf("Advanced tools ready.") }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { P44Card { Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Tools untuk retest.", color = SoftText) }; P44Card { Text("Advanced Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold); Button(onClick = { prefs.edit().putString(P44_STAGE, P44_DATA).putBoolean("first_run_done", true).putBoolean("dlavie_data_installed", false).apply(); status = "Siap repair DATA setelah first setup." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Repair DATA After First Setup", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { prefs.edit().putString(P44_STAGE, P44_PERMISSION).remove("game_permission_opened").remove("first_run_started").remove("first_run_done").putBoolean("dlavie_data_installed", false).apply(); status = "Flow diulang dari permission." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Restart Safe Flow", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openSetup, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Setup", fontWeight = FontWeight.Bold) } }; P44Card { Text("Roadmap", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Now", "DATA after first setup"); InfoLine("Login/community", "After production launch"); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status) } } }
