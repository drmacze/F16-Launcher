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

private const val P43_STAGE = "phase43_stage"
private const val P43_APK = "apk"
private const val P43_PERMISSION = "permission"
private const val P43_OBB = "obb"
private const val P43_DATA = "data"
private const val P43_FINALIZE = "finalize"
private const val P43_FIRST_RUN = "first_run"
private const val P43_READY = "ready"

private enum class P43Page(val label: String, val icon: String) { Setup("Setup", "↻"), Help("Help", "?"), More("More", "☰") }
private enum class P43Status(val title: String) { Checking("Checking"), Action("Action Needed"), Downloading("Downloading"), Installing("Installing"), Ready("Ready"), Error("Error") }

@Composable
fun Phase43StableSetupShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P43Page.Setup) }
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        AnimatedContent(targetState = page, label = "phase43", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                P43Page.Setup -> P43Setup()
                P43Page.Help -> P43Help()
                P43Page.More -> P43More(api) { page = P43Page.Setup }
            }
        }
        P43Nav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun P43Card(content: @Composable ColumnScope.() -> Unit) = GlassCard(Modifier.fillMaxWidth(), content)

@Composable
private fun P43Nav(page: P43Page, onPage: (P43Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            P43Page.values().forEach { item ->
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
private fun P43Setup() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    val manager = remember { PublicInstallManager(context) }
    val crashReporter = remember { GameCrashReporter(context) }

    var manifest by remember { mutableStateOf<PublicInstallManifest?>(null) }
    var status by remember { mutableStateOf(P43Status.Checking) }
    var stage by remember { mutableStateOf(prefs.getString(P43_STAGE, P43_APK) ?: P43_APK) }
    var mode by remember { mutableStateOf("check") }
    var message by remember { mutableStateOf("Mengecek setup DLavie...") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var gameInstalled by remember { mutableStateOf(false) }
    var obbReady by remember { mutableStateOf(prefs.getBoolean("dlavie_obb_installed", false)) }
    var dataReady by remember { mutableStateOf(prefs.getBoolean("dlavie_data_installed", false)) }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var logs by remember { mutableStateOf(listOf("DLavie stable setup siap.")) }

    fun setStage(next: String) { stage = next; prefs.edit().putString(P43_STAGE, next).apply() }
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
            status = P43Status.Checking
            pct(progress, "Checking")
            if (!silent) log("Mengecek status tahap saat ini...")
            val current = try { currentManifest() } catch (t: Throwable) { status = P43Status.Error; mode = "retry"; pct(0, "Manifest failed"); log("Manifest gagal: ${t.message}"); return@launch }

            gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
            shizuku = ShizukuSetup.status(context)
            obbReady = prefs.getBoolean("dlavie_obb_installed", false)
            dataReady = prefs.getBoolean("dlavie_data_installed", false)

            if (!gameInstalled) {
                setStage(P43_APK)
                val apk = withContext(Dispatchers.IO) { manager.downloadStatus(current.apk) }
                when {
                    apk.active -> { status = P43Status.Downloading; mode = "wait_apk"; pct(apk.progress, "Downloading APK"); log("Download APK berjalan: ${apk.progress}%") }
                    apk.done -> { status = P43Status.Action; mode = "install_apk"; pct(100, "APK ready"); log("APK selesai. Tekan Install APK. Jangan buka game dulu.") }
                    else -> { status = P43Status.Action; mode = "download_apk"; pct(0, "Need APK"); log("Langkah 1/7: download APK game.") }
                }
                return@launch
            }

            if (shizuku == "Not Installed" || shizuku == "Need Start") { status = P43Status.Action; mode = "open_shizuku"; pct(20, "Need Shizuku"); log("Buka Shizuku dan start service."); return@launch }
            if (shizuku == "Need Permission") { status = P43Status.Action; mode = "grant_shizuku"; pct(25, "Need Shizuku permission"); log("Beri izin Shizuku untuk DLavie Launcher."); return@launch }

            if (stage == P43_APK) setStage(P43_PERMISSION)
            if (stage == P43_PERMISSION) { status = P43Status.Action; mode = "permission"; pct(32, "Game permission"); log(if (prefs.getBoolean("game_permission_opened", false)) "Jika permission sudah diberi, tekan Permission Done." else "Buka permission FIFA 16 dan izinkan Files/Storage."); return@launch }

            if (!obbReady || stage == P43_OBB) {
                setStage(P43_OBB)
                val obb = withContext(Dispatchers.IO) { manager.downloadStatus(current.obb) }
                when {
                    obb.active -> { status = P43Status.Downloading; mode = "wait_obb"; pct(obb.progress, "Downloading OBB"); log("Download OBB berjalan: ${obb.progress}%") }
                    obb.done -> { status = P43Status.Action; mode = "install_obb"; pct(100, "OBB downloaded"); log("OBB sudah di-download. Tekan Install OBB. Jangan keluar saat copy.") }
                    else -> { status = P43Status.Action; mode = "download_obb"; pct(0, "Need OBB"); log("Langkah 3/7: download OBB. Boleh keluar app saat download.") }
                }
                return@launch
            }

            if (!dataReady || stage == P43_DATA) {
                setStage(P43_DATA)
                val data = withContext(Dispatchers.IO) { manager.downloadStatus(current.data) }
                when {
                    data.active -> { status = P43Status.Downloading; mode = "wait_data"; pct(data.progress, "Downloading DATA"); log("Download DATA berjalan: ${data.progress}%") }
                    data.done -> { status = P43Status.Action; mode = "install_data"; pct(100, "DATA downloaded"); log("DATA sudah di-download. Tekan Install DATA. Jangan keluar saat copy.") }
                    else -> { status = P43Status.Action; mode = "download_data"; pct(0, "Need DATA"); log("Langkah 4/7: download DATA. Boleh keluar app saat download.") }
                }
                return@launch
            }

            if (stage != P43_FINALIZE && stage != P43_FIRST_RUN && stage != P43_READY) setStage(P43_FINALIZE)
            if (stage == P43_FINALIZE) {
                val apk = withContext(Dispatchers.IO) { manager.downloadStatus(current.apk) }
                when {
                    apk.active -> { status = P43Status.Downloading; mode = "wait_finalize"; pct(apk.progress, "Preparing APK"); log("Menyiapkan APK finalize: ${apk.progress}%") }
                    apk.done -> { status = P43Status.Action; mode = "finalize_apk"; pct(100, "APK ready"); log("Install ulang APK sebagai update/finalize. Setelah installer selesai, kembali ke DLavie.") }
                    else -> { status = P43Status.Action; mode = "download_finalize"; pct(0, "Need APK"); log("Menyiapkan APK untuk finalize.") }
                }
                return@launch
            }

            if (stage == P43_FIRST_RUN) { status = P43Status.Action; mode = "first_run"; pct(90, "First setup"); log(if (prefs.getBoolean("first_run_started", false)) "Jika installer internal selesai, tekan First Setup Done." else "Buka First Setup dan tunggu installer internal game selesai."); return@launch }
            status = P43Status.Ready; mode = "play"; pct(100, "Ready"); log("Semua siap. FIFA 16 bisa dimainkan.")
        }
    }

    fun startDownload(asset: InstallAsset, label: String) {
        status = P43Status.Downloading; mode = "wait_${label.lowercase()}"; pct(0, "Downloading $label"); log("Download $label dimulai. Boleh keluar app saat download.")
        scope.launch { try { withContext(Dispatchers.IO) { manager.downloadAsset(asset) { p -> scope.launch(Dispatchers.Main) { pct(p, "Downloading $label") } } }; log("Download $label selesai.") } catch (t: Throwable) { status = P43Status.Error; log("Download $label gagal: ${t.message}") }; refresh(true) }
    }

    fun installObb(current: PublicInstallManifest) {
        status = P43Status.Installing; mode = "installing_obb"; pct(0, "Copy OBB"); log("Copy OBB dimulai. Tunggu sampai selesai.")
        scope.launch { try { withContext(Dispatchers.IO) { contentInstaller().installObbOnly(current) }; prefs.edit().putBoolean("dlavie_obb_installed", true).apply(); obbReady = true; setStage(P43_DATA); log("OBB selesai.") } catch (t: Throwable) { status = P43Status.Error; log("Install OBB gagal: ${t.message}") }; refresh(true) }
    }

    fun installData(current: PublicInstallManifest) {
        status = P43Status.Installing; mode = "installing_data"; pct(0, "Copy DATA"); log("Copy DATA dimulai. Tunggu sampai selesai.")
        scope.launch { try { withContext(Dispatchers.IO) { contentInstaller().installDataOnly(current) }; prefs.edit().putBoolean("dlavie_data_installed", true).apply(); dataReady = true; setStage(P43_FINALIZE); log("DATA selesai.") } catch (t: Throwable) { status = P43Status.Error; log("Install DATA gagal: ${t.message}") }; refresh(true) }
    }

    fun mainAction() {
        val current = manifest ?: return refresh(false)
        when (mode) {
            "download_apk" -> startDownload(current.apk, "APK")
            "install_apk" -> { prefs.edit().putBoolean("awaiting_apk_install", true).apply(); manager.openApkInstaller(manager.cachedAssetFile(current.apk)) }
            "open_shizuku" -> ShizukuSetup.openApp(context)
            "grant_shizuku" -> ShizukuSetup.requestPermission()
            "permission" -> if (!prefs.getBoolean("game_permission_opened", false)) { prefs.edit().putBoolean("game_permission_opened", true).apply(); openGameSettings() } else { prefs.edit().putBoolean("game_permission_ready", true).apply(); setStage(P43_OBB); refresh(true) }
            "download_obb" -> startDownload(current.obb, "OBB")
            "install_obb" -> installObb(current)
            "download_data" -> startDownload(current.data, "DATA")
            "install_data" -> installData(current)
            "download_finalize" -> startDownload(current.apk, "APK")
            "finalize_apk" -> { prefs.edit().putBoolean("awaiting_apk_finalize", true).apply(); manager.openApkInstaller(manager.cachedAssetFile(current.apk)) }
            "first_run" -> if (!prefs.getBoolean("first_run_started", false)) { prefs.edit().putBoolean("first_run_started", true).apply(); crashReporter.prepareLaunch(); launchGame(context) } else { prefs.edit().putBoolean("first_run_done", true).apply(); setStage(P43_READY); refresh(true) }
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
        "download_data" -> "Download DATA"
        "install_data" -> "Install DATA"
        "download_finalize" -> "Prepare APK"
        "finalize_apk" -> "Finalize APK"
        "first_run" -> if (prefs.getBoolean("first_run_started", false)) "First Setup Done" else "Start First Setup"
        "play" -> "Play FIFA 16"
        else -> "Refresh"
    }

    val busy = status == P43Status.Checking || status == P43Status.Installing

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (prefs.getBoolean("awaiting_apk_install", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) { prefs.edit().putBoolean("awaiting_apk_install", false).apply(); setStage(P43_PERMISSION) }
                if (prefs.getBoolean("awaiting_apk_finalize", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) { prefs.edit().putBoolean("awaiting_apk_finalize", false).putBoolean("first_run_started", false).apply(); setStage(P43_FIRST_RUN); status = P43Status.Action; mode = "first_run"; pct(90, "First setup"); log("Finalize selesai. Sekarang Start First Setup."); return@LifecycleEventObserver }
                val crashReason = crashReporter.consumeFastReturn()
                if (crashReason != null && stage != P43_FIRST_RUN) { status = P43Status.Error; pct(0, "Game crash"); log("FIFA 16 crash: $crashReason") } else refresh(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { refresh(true) }
    LaunchedEffect(mode) { while (mode.startsWith("wait_")) { delay(2500L); refresh(true) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P43Card { Text("DLavie Auto Setup", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Stable Android 12+ installer", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); P43StatusPill(status); Spacer(Modifier.height(10.dp)); Text(message, color = SoftText); Spacer(Modifier.height(16.dp)); Button(enabled = !busy, onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) { Text(buttonLabel(), fontWeight = FontWeight.Black, fontSize = 17.sp) } }
        P43Readiness(gameInstalled, obbReady, dataReady, shizuku, stage)
        P43Progress(progress, step)
        P43Card { Text("Recent Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.takeLast(7).forEach { Text("- $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P43StatusPill(status: P43Status) { val color = when (status) { P43Status.Ready -> NeonGreen; P43Status.Action -> Color(0xFFFFB84D); P43Status.Downloading, P43Status.Installing, P43Status.Checking -> CandyBlue; P43Status.Error -> Color(0xFFFF5269) }; Surface(shape = RoundedCornerShape(22.dp), color = Color(0x33101827), border = BorderStroke(1.dp, color)) { Text(status.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold) } }
@Composable
private fun P43Readiness(game: Boolean, obb: Boolean, data: Boolean, shizuku: String, stage: String) = P43Card { Text("Setup Readiness", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Game APK", if (game) "Installed" else "Need download"); InfoLine("OBB", if (obb) "Ready" else "Need install"); InfoLine("DATA", if (data) "Ready" else "Need install"); InfoLine("Shizuku", shizuku); InfoLine("Stage", stage) }
@Composable
private fun P43Progress(progress: Int, label: String) = P43Card { Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Step", label); Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp); Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp))) } }
@Composable
private fun P43Help() { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { P43Card { Text("Help", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Stable installer", color = CandyCyan); Spacer(Modifier.height(12.dp)); Text("Download boleh ditinggal. Extract/copy jangan ditutup. Launcher tidak melakukan cek berat berulang setelah finalize APK.", color = SoftText) }; P43Card { Text("Catatan", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Boleh", "keluar app saat Download."); InfoLine("Jangan", "keluar saat Copy DATA/OBB."); InfoLine("Finalize", "setelah install ulang APK, kembali dan lanjut First Setup.") } } }
@Composable
private fun P43More(api: CommunityApi, openSetup: () -> Unit) { val context = LocalContext.current; val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }; var status by remember { mutableStateOf("Advanced tools ready.") }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { P43Card { Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Tools untuk retest.", color = SoftText) }; P43Card { Text("Advanced Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold); Button(onClick = { prefs.edit().putString(P43_STAGE, P43_PERMISSION).remove("game_permission_opened").remove("first_run_started").remove("first_run_done").apply(); status = "Flow diulang dari permission." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Restart Safe Flow", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { prefs.edit().putString(P43_STAGE, P43_READY).putBoolean("first_run_done", true).apply(); status = "Dipaksa Ready untuk test." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Force Ready Test", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openSetup, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Setup", fontWeight = FontWeight.Bold) } }; P43Card { Text("Roadmap", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Now", "Stable public installer"); InfoLine("Login/community", "After production launch"); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status) } } }
