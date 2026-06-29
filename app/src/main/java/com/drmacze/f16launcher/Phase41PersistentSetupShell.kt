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

private const val P41_STAGE = "phase41_stage"
private const val P41_APK = "apk"
private const val P41_PERMISSION = "permission"
private const val P41_OBB = "obb"
private const val P41_DATA = "data"
private const val P41_FINALIZE = "finalize"
private const val P41_FIRST_RUN = "first_run"
private const val P41_READY = "ready"

private enum class P41Page(val label: String, val icon: String) { Setup("Setup", "↻"), Help("Help", "?"), More("More", "☰") }
private enum class P41Status(val title: String) { Checking("Checking"), Action("Action Needed"), Downloading("Downloading"), Installing("Installing"), Ready("Ready"), Error("Error") }

@Composable
fun Phase41PersistentSetupShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P41Page.Setup) }
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        AnimatedContent(targetState = page, label = "phase41", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                P41Page.Setup -> P41Setup()
                P41Page.Help -> P41Help()
                P41Page.More -> P41More(api) { page = P41Page.Setup }
            }
        }
        P41Nav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun P41Card(content: @Composable ColumnScope.() -> Unit) = GlassCard(Modifier.fillMaxWidth(), content)

@Composable
private fun P41Nav(page: P41Page, onPage: (P41Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xD80E1728),
        border = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 18.dp
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            P41Page.values().forEach { item ->
                val selected = item == page
                Button(
                    onClick = { onPage(item) },
                    modifier = Modifier.weight(1f).height(if (selected) 54.dp else 48.dp),
                    shape = RoundedCornerShape(25.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText)
                ) {
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
private fun P41Setup() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    val manager = remember { PublicInstallManager(context) }
    val engine = remember { DevPatchEngine(context) {} }
    val crashReporter = remember { GameCrashReporter(context) }

    var manifest by remember { mutableStateOf<PublicInstallManifest?>(null) }
    var status by remember { mutableStateOf(P41Status.Checking) }
    var stage by remember { mutableStateOf(prefs.getString(P41_STAGE, P41_APK) ?: P41_APK) }
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
    var logs by remember { mutableStateOf(listOf("DLavie persistent setup siap.")) }

    fun setStage(next: String) { stage = next; prefs.edit().putString(P41_STAGE, next).apply() }
    fun log(text: String) { logs = (logs + text).takeLast(24); message = text }
    fun pct(value: Int, label: String) { progress = value.coerceIn(0, 100); step = label }

    fun openGameSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${DevPatchEngine.GAME_PACKAGE}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun decide(silent: Boolean = false) {
        scope.launch {
            status = P41Status.Checking
            pct(progress, "Checking")
            if (!silent) log("Mengecek status download dan install...")

            val current = try { withContext(Dispatchers.IO) { manager.fetchInstallManifest() } } catch (t: Throwable) {
                status = P41Status.Error; mode = "check"; pct(0, "Manifest failed"); log("Manifest gagal: ${t.message}"); return@launch
            }
            manifest = current
            gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
            shizuku = ShizukuSetup.status(context)
            access = engine.accessMode()
            local = engine.localVersion()
            latest = try { withContext(Dispatchers.IO) { engine.fetchManifest().optInt("latestVersionCode", local) } } catch (_: Throwable) { local }

            val checker = PublicContentInstaller(context, {}, { _, _ -> })
            obbReady = if (shizuku == "Ready") withContext(Dispatchers.IO) { checker.hasOfficialObb(current) } else false
            dataReady = if (shizuku == "Ready") withContext(Dispatchers.IO) { checker.hasOfficialData(current) } else false

            val apkDownload = manager.downloadStatus(current.apk)
            val obbDownload = manager.downloadStatus(current.obb)
            val dataDownload = manager.downloadStatus(current.data)

            if (!gameInstalled) {
                setStage(P41_APK)
                when {
                    apkDownload.active -> { status = P41Status.Downloading; mode = "wait_apk"; pct(apkDownload.progress, "Downloading APK"); log("Download APK masih berjalan di background: ${apkDownload.progress}%") }
                    apkDownload.done -> { status = P41Status.Action; mode = "install_apk"; pct(100, "APK downloaded"); log("APK sudah selesai. Tekan Install APK, lalu jangan buka game dulu.") }
                    else -> { status = P41Status.Action; mode = "download_apk"; pct(0, "Need APK"); log("Langkah 1/7: download APK DLavie 26.") }
                }
                return@launch
            }

            if (shizuku == "Not Installed" || shizuku == "Need Start") { status = P41Status.Action; mode = "open_shizuku"; pct(20, "Need Shizuku"); log("Buka Shizuku dan start service."); return@launch }
            if (shizuku == "Need Permission") { status = P41Status.Action; mode = "grant_shizuku"; pct(25, "Need Shizuku permission"); log("Beri izin Shizuku untuk DLavie Launcher."); return@launch }

            if (stage == P41_APK) setStage(P41_PERMISSION)
            if (stage == P41_PERMISSION) { status = P41Status.Action; mode = "permission"; pct(32, "Grant game permission"); log(if (prefs.getBoolean("game_permission_opened", false)) "Jika permission sudah diberi, tekan Permission Done." else "Langkah 2/7: buka permission FIFA 16 dan izinkan Files/Storage."); return@launch }

            if (!obbReady || stage == P41_OBB) {
                setStage(P41_OBB)
                when {
                    obbDownload.active -> { status = P41Status.Downloading; mode = "wait_obb"; pct(obbDownload.progress, "Downloading OBB"); log("Download OBB tetap berjalan walau pindah halaman/app: ${obbDownload.progress}%") }
                    obbDownload.done -> { status = P41Status.Action; mode = "install_obb"; pct(100, "OBB downloaded"); log("Download OBB selesai. Tekan Install OBB untuk extract/copy. Jangan keluar saat copy.") }
                    else -> { status = P41Status.Action; mode = "download_obb"; pct(0, "Need OBB"); log("Langkah 3/7: download OBB dulu. Kamu boleh keluar app saat download.") }
                }
                return@launch
            }

            if (!dataReady || stage == P41_DATA) {
                setStage(P41_DATA)
                when {
                    dataDownload.active -> { status = P41Status.Downloading; mode = "wait_data"; pct(dataDownload.progress, "Downloading DATA"); log("Download DATA tetap berjalan walau pindah halaman/app: ${dataDownload.progress}%") }
                    dataDownload.done -> { status = P41Status.Action; mode = "install_data"; pct(100, "DATA downloaded"); log("Download DATA selesai. Tekan Install DATA untuk extract/copy. Jangan keluar saat copy.") }
                    else -> { status = P41Status.Action; mode = "download_data"; pct(0, "Need DATA"); log("Langkah 4/7: download DATA. Kamu boleh keluar app saat download.") }
                }
                return@launch
            }

            if (stage != P41_FINALIZE && stage != P41_FIRST_RUN && stage != P41_READY) setStage(P41_FINALIZE)
            if (stage == P41_FINALIZE) {
                when {
                    apkDownload.active -> { status = P41Status.Downloading; mode = "wait_finalize_apk"; pct(apkDownload.progress, "Preparing APK finalize"); log("APK finalize sedang disiapkan di background: ${apkDownload.progress}%") }
                    apkDownload.done -> { status = P41Status.Action; mode = "finalize_apk"; pct(100, "APK ready"); log("Langkah 5/7: install ulang APK sebagai update/finalize. Jangan buka game dulu.") }
                    else -> { status = P41Status.Action; mode = "download_finalize_apk"; pct(0, "Need APK finalize"); log("Menyiapkan APK untuk finalize.") }
                }
                return@launch
            }

            if (stage == P41_FIRST_RUN) { status = P41Status.Action; mode = "first_run"; pct(90, "First setup"); log(if (prefs.getBoolean("first_run_started", false)) "Jika installer hijau/Chinese selesai, tekan First Setup Done." else "Langkah 6/7: buka First Setup dan tunggu installer internal selesai."); return@launch }

            status = P41Status.Ready; mode = "play"; pct(100, "Ready"); log("Langkah 7/7 selesai. Siap Play FIFA 16.")
        }
    }

    fun startDownload(asset: InstallAsset, label: String) {
        status = P41Status.Downloading
        mode = "wait_${label.lowercase()}"
        pct(0, "Downloading $label")
        log("Download $label dimulai. Aman buka halaman lain atau aplikasi lain.")
        scope.launch {
            try {
                withContext(Dispatchers.IO) { manager.downloadAsset(asset) { p -> pct(p, "Downloading $label") } }
                log("Download $label selesai.")
            } catch (t: Throwable) {
                status = P41Status.Error; log("Download $label gagal: ${t.message}")
            }
            decide(true)
        }
    }

    fun installObb(current: PublicInstallManifest) {
        status = P41Status.Installing; mode = "installing_obb"; pct(0, "Copy OBB"); log("Extract/copy OBB dimulai. Jangan tutup app sampai selesai.")
        scope.launch {
            try {
                withContext(Dispatchers.IO) { PublicContentInstaller(context, { text -> log(text) }, { p, s -> pct(p, s) }).installObbOnly(current) }
                setStage(P41_DATA); log("OBB selesai. Lanjut DATA.")
            } catch (t: Throwable) { status = P41Status.Error; log("Install OBB gagal: ${t.message}") }
            decide(true)
        }
    }

    fun installData(current: PublicInstallManifest) {
        status = P41Status.Installing; mode = "installing_data"; pct(0, "Copy DATA"); log("Extract/copy DATA dimulai. Jangan tutup app sampai selesai.")
        scope.launch {
            try {
                withContext(Dispatchers.IO) { PublicContentInstaller(context, { text -> log(text) }, { p, s -> pct(p, s) }).installDataOnly(current) }
                setStage(P41_FINALIZE); log("DATA selesai. Lanjut finalize APK.")
            } catch (t: Throwable) { status = P41Status.Error; log("Install DATA gagal: ${t.message}") }
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
            "permission" -> {
                if (!prefs.getBoolean("game_permission_opened", false)) { prefs.edit().putBoolean("game_permission_opened", true).apply(); openGameSettings() }
                else { prefs.edit().putBoolean("game_permission_ready", true).apply(); setStage(P41_OBB); decide(true) }
            }
            "download_obb" -> startDownload(current.obb, "OBB")
            "install_obb" -> installObb(current)
            "download_data" -> startDownload(current.data, "DATA")
            "install_data" -> installData(current)
            "download_finalize_apk" -> startDownload(current.apk, "APK")
            "finalize_apk" -> { prefs.edit().putBoolean("awaiting_apk_finalize", true).apply(); manager.openApkInstaller(manager.cachedAssetFile(current.apk)) }
            "first_run" -> {
                if (!prefs.getBoolean("first_run_started", false)) { prefs.edit().putBoolean("first_run_started", true).apply(); crashReporter.prepareLaunch(); launchGame(context) }
                else { prefs.edit().putBoolean("first_run_done", true).apply(); setStage(P41_READY); decide(true) }
            }
            "play" -> { crashReporter.prepareLaunch(); launchGame(context) }
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

    val busy = status == P41Status.Downloading || status == P41Status.Installing || status == P41Status.Checking

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (prefs.getBoolean("awaiting_apk_install", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) { prefs.edit().putBoolean("awaiting_apk_install", false).apply(); setStage(P41_PERMISSION) }
                if (prefs.getBoolean("awaiting_apk_finalize", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) { prefs.edit().putBoolean("awaiting_apk_finalize", false).putBoolean("first_run_started", false).apply(); setStage(P41_FIRST_RUN) }
                val crashReason = crashReporter.consumeFastReturn()
                if (crashReason != null && stage != P41_FIRST_RUN) { status = P41Status.Error; pct(0, "Game crash"); log("FIFA 16 crash: $crashReason") } else decide(true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { decide(true) }
    LaunchedEffect(mode, stage) {
        while (mode.startsWith("wait_")) {
            delay(1500L)
            decide(true)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P41Card {
            Text("DLavie Auto Setup", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Persistent Android 12+ installer", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            P41StatusPill(status)
            Spacer(Modifier.height(10.dp))
            Text(message, color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(enabled = !busy, onClick = { mainAction() }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) {
                Text(buttonLabel(), fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
        }
        P41Readiness(gameInstalled, obbReady, dataReady, shizuku, access, local, latest, stage)
        P41FlowCard(stage, mode)
        P41Progress(progress, step)
        P41Card { Text("Recent Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.takeLast(7).forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P41StatusPill(status: P41Status) {
    val color = when (status) {
        P41Status.Ready -> NeonGreen
        P41Status.Action -> Color(0xFFFFB84D)
        P41Status.Downloading, P41Status.Installing, P41Status.Checking -> CandyBlue
        P41Status.Error -> Color(0xFFFF5269)
    }
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0x33101827), border = BorderStroke(1.dp, color)) {
        Text(status.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun P41Readiness(game: Boolean, obb: Boolean, data: Boolean, shizuku: String, access: String, local: Int, latest: Int, stage: String) = P41Card {
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
private fun P41FlowCard(stage: String, mode: String) = P41Card {
    Text("Safe Install Flow", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Download boleh ditinggal. Extract/copy jangan ditutup.", color = SoftText, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    InfoLine("1", if (stage == P41_APK) "APK ← current" else "APK")
    InfoLine("2", if (stage == P41_PERMISSION) "Permission ← current" else "Permission")
    InfoLine("3", if (stage == P41_OBB || mode.contains("obb")) "OBB ← current" else "OBB")
    InfoLine("4", if (stage == P41_DATA || mode.contains("data")) "DATA ← current" else "DATA")
    InfoLine("5", if (stage == P41_FINALIZE) "Finalize APK ← current" else "Finalize APK")
    InfoLine("6", if (stage == P41_FIRST_RUN) "First setup ← current" else "First setup")
    InfoLine("7", if (stage == P41_READY) "Ready ← current" else "Ready")
}

@Composable
private fun P41Progress(progress: Int, label: String) = P41Card {
    Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    InfoLine("Step", label)
    Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) {
        Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp)))
    }
}

@Composable
private fun P41Help() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P41Card {
            Text("Help", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Download background", color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text("APK, OBB, dan DATA sekarang di-download oleh Android DownloadManager. Kamu boleh buka Help, minimize app, atau buka aplikasi lain. Saat kembali ke Setup, progress dibaca lagi dari sistem Android.", color = SoftText)
        }
        P41Card {
            Text("Catatan penting", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Boleh", "keluar app saat Download.")
            InfoLine("Jangan", "keluar saat Extract/Copy ke Android/data atau Android/obb.")
            InfoLine("Kalau selesai", "tekan tombol tahap yang sama untuk lanjut install/copy.")
        }
    }
}

@Composable
private fun P41More(api: CommunityApi, openSetup: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var status by remember { mutableStateOf("Advanced tools ready.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P41Card { Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Tools untuk retest.", color = SoftText) }
        P41Card {
            Text("Advanced Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { prefs.edit().putString(P41_STAGE, P41_PERMISSION).remove("game_permission_opened").remove("first_run_started").remove("first_run_done").apply(); status = "Flow diulang dari permission." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Restart Safe Flow", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { prefs.edit().putString(P41_STAGE, P41_READY).putBoolean("first_run_done", true).apply(); status = "Dipaksa Ready untuk test." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Force Ready Test", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = openSetup, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Setup", fontWeight = FontWeight.Bold) }
        }
        P41Card { Text("Roadmap", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Now", "Persistent public installer"); InfoLine("Login/community", "After production launch"); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Status", status) }
    }
}
