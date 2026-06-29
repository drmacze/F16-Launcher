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

private const val P42_STAGE = "phase42_stage"
private const val P42_APK = "apk"
private const val P42_PERMISSION = "permission"
private const val P42_OBB = "obb"
private const val P42_DATA = "data"
private const val P42_FINALIZE = "finalize"
private const val P42_FIRST_RUN = "first_run"
private const val P42_READY = "ready"

private enum class P42Page(val label: String, val icon: String) { Setup("Setup", "↻"), Help("Help", "?"), More("More", "☰") }
private enum class P42Status(val title: String) { Checking("Checking"), Action("Action Needed"), Downloading("Downloading"), Installing("Installing"), Ready("Ready"), Error("Error") }

@Composable
fun Phase42NoFreezeSetupShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P42Page.Setup) }
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        AnimatedContent(targetState = page, label = "phase42", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                P42Page.Setup -> P42Setup()
                P42Page.Help -> P42Help()
                P42Page.More -> P42More(api) { page = P42Page.Setup }
            }
        }
        P42Nav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun P42Card(content: @Composable ColumnScope.() -> Unit) = GlassCard(Modifier.fillMaxWidth(), content)

@Composable
private fun P42Nav(page: P42Page, onPage: (P42Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            P42Page.values().forEach { item ->
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
private fun P42Setup() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    val manager = remember { PublicInstallManager(context) }
    val engine = remember { DevPatchEngine(context) {} }
    val crashReporter = remember { GameCrashReporter(context) }

    var manifest by remember { mutableStateOf<PublicInstallManifest?>(null) }
    var status by remember { mutableStateOf(P42Status.Checking) }
    var stage by remember { mutableStateOf(prefs.getString(P42_STAGE, P42_APK) ?: P42_APK) }
    var mode by remember { mutableStateOf("check") }
    var message by remember { mutableStateOf("Mengecek setup DLavie...") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var gameInstalled by remember { mutableStateOf(false) }
    var obbReady by remember { mutableStateOf(false) }
    var dataReady by remember { mutableStateOf(false) }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var access by remember { mutableStateOf("Checking...") }
    var local by remember { mutableStateOf(1) }
    var latest by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(listOf("DLavie no-freeze setup siap.")) }

    fun setStage(next: String) { stage = next; prefs.edit().putString(P42_STAGE, next).apply() }
    fun log(text: String) { logs = (logs + text).takeLast(20); message = text }
    fun pct(value: Int, label: String) { progress = value.coerceIn(0, 100); step = label }
    fun installerWithMainCallbacks() = PublicContentInstaller(
        context,
        { text -> scope.launch(Dispatchers.Main) { log(text) } },
        { p, s -> scope.launch(Dispatchers.Main) { pct(p, s) } }
    )

    fun openGameSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${DevPatchEngine.GAME_PACKAGE}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun playGameGuarded() {
        crashReporter.prepareLaunch()
        log("Membuka FIFA 16...")
        launchGame(context)
    }

    fun decide(silent: Boolean = false) {
        scope.launch {
            status = P42Status.Checking
            pct(progress, "Checking")
            if (!silent) log("Mengecek status...")
            val current = try { withContext(Dispatchers.IO) { manager.fetchInstallManifest() } } catch (t: Throwable) {
                status = P42Status.Error; mode = "check"; pct(0, "Manifest failed"); log("Manifest gagal: ${t.message}"); return@launch
            }
            manifest = current
            gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
            shizuku = ShizukuSetup.status(context)
            access = engine.accessMode()
            local = engine.localVersion()
            latest = try { withContext(Dispatchers.IO) { engine.fetchManifest().optInt("latestVersionCode", local) } } catch (_: Throwable) { local }

            if (!gameInstalled) {
                setStage(P42_APK)
                val apk = withContext(Dispatchers.IO) { manager.downloadStatus(current.apk) }
                when {
                    apk.active -> { status = P42Status.Downloading; mode = "wait_apk"; pct(apk.progress, "Downloading APK"); log("Download APK berjalan: ${apk.progress}%") }
                    apk.done -> { status = P42Status.Action; mode = "install_apk"; pct(100, "APK downloaded"); log("APK sudah selesai. Tekan Install APK. Jangan buka game dulu.") }
                    else -> { status = P42Status.Action; mode = "download_apk"; pct(0, "Need APK"); log("Langkah 1/7: download APK.") }
                }
                return@launch
            }

            if (shizuku == "Not Installed" || shizuku == "Need Start") { status = P42Status.Action; mode = "open_shizuku"; pct(20, "Need Shizuku"); log("Buka Shizuku dan start service."); return@launch }
            if (shizuku == "Need Permission") { status = P42Status.Action; mode = "grant_shizuku"; pct(25, "Need Shizuku permission"); log("Beri izin Shizuku untuk DLavie Launcher."); return@launch }

            val checker = PublicContentInstaller(context, {}, { _, _ -> })
            obbReady = withContext(Dispatchers.IO) { checker.hasOfficialObb(current) }
            dataReady = withContext(Dispatchers.IO) { checker.hasOfficialData(current) }

            if (stage == P42_APK) setStage(P42_PERMISSION)
            if (stage == P42_PERMISSION) {
                status = P42Status.Action; mode = "permission"; pct(32, "Grant game permission")
                log(if (prefs.getBoolean("game_permission_opened", false)) "Jika permission sudah diberi, tekan Permission Done." else "Langkah 2/7: buka permission FIFA 16 dan izinkan Files/Storage.")
                return@launch
            }

            if (!obbReady || stage == P42_OBB) {
                setStage(P42_OBB)
                val obb = withContext(Dispatchers.IO) { manager.downloadStatus(current.obb) }
                when {
                    obb.active -> { status = P42Status.Downloading; mode = "wait_obb"; pct(obb.progress, "Downloading OBB"); log("Download OBB berjalan: ${obb.progress}%") }
                    obb.done -> { status = P42Status.Action; mode = "install_obb"; pct(100, "OBB downloaded"); log("Download OBB selesai. Tekan Install OBB untuk extract/copy.") }
                    else -> { status = P42Status.Action; mode = "download_obb"; pct(0, "Need OBB"); log("Langkah 3/7: download OBB. Boleh keluar app saat download.") }
                }
                return@launch
            }

            if (!dataReady || stage == P42_DATA) {
                setStage(P42_DATA)
                val data = withContext(Dispatchers.IO) { manager.downloadStatus(current.data) }
                when {
                    data.active -> { status = P42Status.Downloading; mode = "wait_data"; pct(data.progress, "Downloading DATA"); log("Download DATA berjalan: ${data.progress}%") }
                    data.done -> { status = P42Status.Action; mode = "install_data"; pct(100, "DATA downloaded"); log("Download DATA selesai. Tekan Install DATA untuk extract/copy.") }
                    else -> { status = P42Status.Action; mode = "download_data"; pct(0, "Need DATA"); log("Langkah 4/7: download DATA. Boleh keluar app saat download.") }
                }
                return@launch
            }

            if (stage != P42_FINALIZE && stage != P42_FIRST_RUN && stage != P42_READY) setStage(P42_FINALIZE)
            if (stage == P42_FINALIZE) {
                val apk = withContext(Dispatchers.IO) { manager.downloadStatus(current.apk) }
                when {
                    apk.active -> { status = P42Status.Downloading; mode = "wait_finalize_apk"; pct(apk.progress, "Preparing APK"); log("Menyiapkan APK finalize: ${apk.progress}%") }
                    apk.done -> { status = P42Status.Action; mode = "finalize_apk"; pct(100, "APK ready"); log("Langkah 5/7: install ulang APK sebagai update/finalize. Jangan buka game dulu.") }
                    else -> { status = P42Status.Action; mode = "download_finalize_apk"; pct(0, "Need APK"); log("Menyiapkan APK untuk finalize.") }
                }
                return@launch
            }

            if (stage == P42_FIRST_RUN) {
                status = P42Status.Action; mode = "first_run"; pct(90, "First setup")
                log(if (prefs.getBoolean("first_run_started", false)) "Jika installer internal sudah selesai, tekan First Setup Done." else "Langkah 6/7: buka First Setup dan tunggu installer internal selesai.")
                return@launch
            }

            status = P42Status.Ready; mode = "play"; pct(100, "Ready"); log("Langkah 7/7 selesai. Siap Play FIFA 16.")
        }
    }

    fun startDownload(asset: InstallAsset, label: String) {
        status = P42Status.Downloading
        mode = "wait_${label.lowercase()}"
        pct(0, "Downloading $label")
        log("Download $label dimulai. Boleh buka halaman lain atau app lain.")
        scope.launch {
            try { withContext(Dispatchers.IO) { manager.downloadAsset(asset) { p -> scope.launch(Dispatchers.Main) { pct(p, "Downloading $label") } } }; log("Download $label selesai.") }
            catch (t: Throwable) { status = P42Status.Error; log("Download $label gagal: ${t.message}") }
            decide(true)
        }
    }

    fun installObb(current: PublicInstallManifest) {
        status = P42Status.Installing; mode = "installing_obb"; pct(0, "Copy OBB"); log("Extract/copy OBB dimulai. Jangan tutup app.")
        scope.launch {
            try { withContext(Dispatchers.IO) { installerWithMainCallbacks().installObbOnly(current) }; setStage(P42_DATA); log("OBB selesai. Lanjut DATA.") }
            catch (t: Throwable) { status = P42Status.Error; log("Install OBB gagal: ${t.message}") }
            decide(true)
        }
    }

    fun installData(current: PublicInstallManifest) {
        status = P42Status.Installing; mode = "installing_data"; pct(0, "Copy DATA"); log("Extract/copy DATA dimulai. Jangan tutup app.")
        scope.launch {
            try { withContext(Dispatchers.IO) { installerWithMainCallbacks().installDataOnly(current) }; setStage(P42_FINALIZE); log("DATA selesai. Lanjut finalize APK.") }
            catch (t: Throwable) { status = P42Status.Error; log("Install DATA gagal: ${t.message}") }
            decide(true)
        }
    }

    fun mainAction() {
        val current = manifest ?: return decide(false)
        when (mode) {
            "download_apk" -> startDownload(current.apk, "APK")
            "install_apk" -> { prefs.edit().putBoolean("awaiting_apk_install", true).apply(); manager.openApkInstaller(manager.cachedAssetFile(current.apk)) }
            "open_shizuku" -> ShizukuSetup.openApp(context)
            "grant_shizuku" -> ShizukuSetup.requestPermission()
            "permission" -> if (!prefs.getBoolean("game_permission_opened", false)) { prefs.edit().putBoolean("game_permission_opened", true).apply(); openGameSettings() } else { prefs.edit().putBoolean("game_permission_ready", true).apply(); setStage(P42_OBB); decide(true) }
            "download_obb" -> startDownload(current.obb, "OBB")
            "install_obb" -> installObb(current)
            "download_data" -> startDownload(current.data, "DATA")
            "install_data" -> installData(current)
            "download_finalize_apk" -> startDownload(current.apk, "APK")
            "finalize_apk" -> { prefs.edit().putBoolean("awaiting_apk_finalize", true).apply(); manager.openApkInstaller(manager.cachedAssetFile(current.apk)) }
            "first_run" -> if (!prefs.getBoolean("first_run_started", false)) { prefs.edit().putBoolean("first_run_started", true).apply(); crashReporter.prepareLaunch(); launchGame(context) } else { prefs.edit().putBoolean("first_run_done", true).apply(); setStage(P42_READY); decide(true) }
            "play" -> playGameGuarded()
            else -> decide(false)
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
        "download_finalize_apk" -> "Prepare APK"
        "finalize_apk" -> "Finalize APK"
        "first_run" -> if (prefs.getBoolean("first_run_started", false)) "First Setup Done" else "Start First Setup"
        "play" -> "Play FIFA 16"
        else -> "Working..."
    }

    val busy = status == P42Status.Installing || status == P42Status.Checking

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (prefs.getBoolean("awaiting_apk_install", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) { prefs.edit().putBoolean("awaiting_apk_install", false).apply(); setStage(P42_PERMISSION) }
                if (prefs.getBoolean("awaiting_apk_finalize", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) { prefs.edit().putBoolean("awaiting_apk_finalize", false).putBoolean("first_run_started", false).apply(); setStage(P42_FIRST_RUN) }
                val crashReason = crashReporter.consumeFastReturn()
                if (crashReason != null && stage != P42_FIRST_RUN) { status = P42Status.Error; pct(0, "Game crash"); log("FIFA 16 crash: $crashReason") } else decide(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { decide(true) }
    LaunchedEffect(mode, stage) {
        while (mode.startsWith("wait_")) {
            delay(2000L)
            decide(true)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P42Card {
            Text("DLavie Auto Setup", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("No-freeze Android 12+ installer", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            P42StatusPill(status)
            Spacer(Modifier.height(10.dp))
            Text(message, color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(enabled = !busy, onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) { Text(buttonLabel(), fontWeight = FontWeight.Black, fontSize = 17.sp) }
        }
        P42Readiness(gameInstalled, obbReady, dataReady, shizuku, access, local, latest, stage)
        P42FlowCard(stage, mode)
        P42Progress(progress, step)
        P42Card { Text("Recent Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.takeLast(7).forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P42StatusPill(status: P42Status) {
    val color = when (status) { P42Status.Ready -> NeonGreen; P42Status.Action -> Color(0xFFFFB84D); P42Status.Downloading, P42Status.Installing, P42Status.Checking -> CandyBlue; P42Status.Error -> Color(0xFFFF5269) }
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0x33101827), border = BorderStroke(1.dp, color)) { Text(status.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold) }
}

@Composable
private fun P42Readiness(game: Boolean, obb: Boolean, data: Boolean, shizuku: String, access: String, local: Int, latest: Int, stage: String) = P42Card {
    Text("Setup Readiness", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    InfoLine("Game APK", if (game) "Installed" else "Need download")
    InfoLine("OBB", if (obb) "Ready" else "Need install")
    InfoLine("DATA", if (data) "Ready" else "Need install")
    InfoLine("Shizuku", shizuku)
    InfoLine("Access", access)
    InfoLine("Version", if (latest > 0) "v$local / latest v$latest" else "v$local")
    InfoLine("Stage", stage)
}

@Composable
private fun P42FlowCard(stage: String, mode: String) = P42Card {
    Text("Safe Install Flow", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Download boleh ditinggal. Extract/copy jangan ditutup.", color = SoftText, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    InfoLine("1", if (stage == P42_APK) "APK -> current" else "APK")
    InfoLine("2", if (stage == P42_PERMISSION) "Permission -> current" else "Permission")
    InfoLine("3", if (stage == P42_OBB || mode.contains("obb")) "OBB -> current" else "OBB")
    InfoLine("4", if (stage == P42_DATA || mode.contains("data")) "DATA -> current" else "DATA")
    InfoLine("5", if (stage == P42_FINALIZE) "Finalize APK -> current" else "Finalize APK")
    InfoLine("6", if (stage == P42_FIRST_RUN) "First setup -> current" else "First setup")
    InfoLine("7", if (stage == P42_READY) "Ready -> current" else "Ready")
}

@Composable
private fun P42Progress(progress: Int, label: String) = P42Card {
    Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    InfoLine("Step", label)
    Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp))) }
}

@Composable
private fun P42Help() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P42Card { Text("Help", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("No-freeze installer", color = CandyCyan); Spacer(Modifier.height(12.dp)); Text("Launcher tidak menghitung SHA file besar di UI lagi. Download boleh ditinggal. Saat extract/copy, tetap tunggu sampai selesai.", color = SoftText) }
        P42Card { Text("Catatan penting", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Boleh", "keluar app saat Download."); InfoLine("Jangan", "keluar saat Extract/Copy."); InfoLine("APK finalize", "setelah installer APK selesai, kembali ke launcher.") }
    }
}

@Composable
private fun P42More(api: CommunityApi, openSetup: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var status by remember { mutableStateOf("Advanced tools ready.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P42Card { Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Tools untuk retest.", color = SoftText) }
        P42Card {
            Text("Advanced Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { prefs.edit().putString(P42_STAGE, P42_PERMISSION).remove("game_permission_opened").remove("first_run_started").remove("first_run_done").apply(); status = "Flow diulang dari permission." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Restart Safe Flow", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { prefs.edit().putString(P42_STAGE, P42_READY).putBoolean("first_run_done", true).apply(); status = "Dipaksa Ready untuk test." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Force Ready Test", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = openSetup, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Setup", fontWeight = FontWeight.Bold) }
        }
        P42Card { Text("Roadmap", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Now", "No-freeze public installer"); InfoLine("Login/community", "After production launch"); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status) }
    }
}
