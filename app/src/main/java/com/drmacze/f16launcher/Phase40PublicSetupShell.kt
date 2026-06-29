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

private const val STAGE_KEY = "public_setup_stage"
private const val STAGE_APK = "apk"
private const val STAGE_APK_INSTALL = "apk_install"
private const val STAGE_PERMISSION = "permission"
private const val STAGE_OBB = "obb"
private const val STAGE_DATA = "data"
private const val STAGE_FINALIZE = "finalize"
private const val STAGE_FIRST_RUN = "first_run"
private const val STAGE_READY = "ready"

private enum class P40Page(val label: String, val icon: String) { Setup("Setup", "↻"), Help("Help", "?"), More("More", "☰") }
private enum class P40State(val title: String) {
    Checking("Checking"), NeedApk("Need Game APK"), NeedApkInstall("Install APK"), NeedShizuku("Need Shizuku"),
    NeedShizukuPermission("Need Permission"), NeedGamePermission("Grant Game Permission"), NeedObb("Install OBB"),
    NeedData("Install DATA"), NeedFinalizeApk("Finalize APK"), NeedFirstRun("First Setup"), Ready("Ready"),
    Working("Working"), Recovery("Need Recovery"), Offline("Offline")
}

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
    Surface(
        modifier = modifier.widthIn(max = 500.dp).padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 18.dp, tonalElevation = 0.dp
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            P40Page.values().forEach { item ->
                val selected = item == page
                Button(
                    onClick = { onPage(item) }, modifier = Modifier.weight(1f).height(if (selected) 54.dp else 48.dp),
                    shape = RoundedCornerShape(25.dp), contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)
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
private fun P40AutoSetup() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    val installer = remember { PublicInstallManager(context) }
    val engine = remember { DevPatchEngine(context) {} }
    val crashReporter = remember { GameCrashReporter(context) }

    var manifest by remember { mutableStateOf<PublicInstallManifest?>(null) }
    var state by remember { mutableStateOf(P40State.Checking) }
    var message by remember { mutableStateOf("Mengecek DLavie setup Android 12+...") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var stage by remember { mutableStateOf(prefs.getString(STAGE_KEY, STAGE_APK) ?: STAGE_APK) }
    var gameInstalled by remember { mutableStateOf(false) }
    var obbReady by remember { mutableStateOf(false) }
    var dataReady by remember { mutableStateOf(false) }
    var shizuku by remember { mutableStateOf(ShizukuSetup.status(context)) }
    var access by remember { mutableStateOf("Checking...") }
    var local by remember { mutableStateOf(prefs.getInt("local_version_code", 1)) }
    var latest by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(listOf("DLavie staged installer siap.")) }

    fun setStage(next: String) {
        stage = next
        prefs.edit().putString(STAGE_KEY, next).apply()
    }
    fun log(text: String) { logs = (logs + text).takeLast(20); message = text }
    fun pct(value: Int, label: String) { progress = value.coerceIn(0, 100); step = label }
    fun contentInstaller(): PublicContentInstaller = PublicContentInstaller(
        context,
        { text -> scope.launch(Dispatchers.Main) { log(text) } },
        { p, s -> scope.launch(Dispatchers.Main) { pct(p, s) } }
    )

    fun openGameSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${DevPatchEngine.GAME_PACKAGE}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun playGameGuarded() {
        crashReporter.prepareLaunch()
        log("Membuka FIFA 16...")
        launchGame(context)
    }

    fun buttonLabel(): String = when (state) {
        P40State.NeedApk -> "Download DLavie 26"
        P40State.NeedApkInstall -> "Install APK"
        P40State.NeedShizuku -> "Open Shizuku"
        P40State.NeedShizukuPermission -> "Grant Shizuku"
        P40State.NeedGamePermission -> if (prefs.getBoolean("game_permission_opened", false)) "Permission Done" else "Open Permission"
        P40State.NeedObb -> "Install OBB"
        P40State.NeedData -> "Install DATA"
        P40State.NeedFinalizeApk -> "Finalize APK"
        P40State.NeedFirstRun -> if (prefs.getBoolean("first_run_started", false)) "First Setup Done" else "Start First Setup"
        P40State.Ready -> "Play FIFA 16"
        P40State.Recovery -> "Open Report / Retry"
        P40State.Offline -> "Retry Check"
        P40State.Working -> "Working..."
        P40State.Checking -> "Checking..."
    }

    fun decideState(silent: Boolean = false) {
        scope.launch {
            state = P40State.Checking
            pct(8, "Checking")
            if (!silent) log("Mengecek staged setup...")
            val current = try {
                withContext(Dispatchers.IO) { installer.fetchInstallManifest() }
            } catch (t: Throwable) {
                state = P40State.Offline
                pct(0, "Install manifest failed")
                log("Gagal baca install manifest: ${t.message}")
                return@launch
            }
            manifest = current
            gameInstalled = isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)
            shizuku = ShizukuSetup.status(context)
            access = engine.accessMode()
            local = engine.localVersion()
            latest = try { withContext(Dispatchers.IO) { engine.fetchManifest().optInt("latestVersionCode", local) } } catch (_: Throwable) { local }

            if (!gameInstalled) {
                setStage(STAGE_APK)
                obbReady = false
                dataReady = false
                state = P40State.NeedApk
                pct(12, "Need APK")
                log("Langkah 1/7: download dan install APK game. Jangan buka game dulu.")
                return@launch
            }

            if (shizuku == "Not Installed" || shizuku == "Need Start") {
                state = P40State.NeedShizuku
                pct(25, "Need Shizuku")
                log("Shizuku dibutuhkan untuk memasang DATA/OBB ke Android/data dan Android/obb.")
                return@launch
            }
            if (shizuku == "Need Permission") {
                state = P40State.NeedShizukuPermission
                pct(30, "Need Shizuku permission")
                log("Beri izin Shizuku untuk DLavie Launcher.")
                return@launch
            }

            val checker = PublicContentInstaller(context, {}, { _, _ -> })
            obbReady = withContext(Dispatchers.IO) { checker.hasOfficialObb(current) }
            dataReady = withContext(Dispatchers.IO) { checker.hasOfficialData(current) }

            if (stage == STAGE_APK || stage == STAGE_APK_INSTALL) setStage(STAGE_PERMISSION)
            if (stage == STAGE_PERMISSION) {
                state = P40State.NeedGamePermission
                pct(35, "Grant game storage")
                log("Langkah 2/7: beri izin penyimpanan ke FIFA 16. Setelah kembali, tekan Permission Done.")
                return@launch
            }
            if (!obbReady || stage == STAGE_OBB) {
                setStage(STAGE_OBB)
                state = P40State.NeedObb
                pct(45, "Need OBB")
                log("Langkah 3/7: pasang OBB dulu sebelum DATA.")
                return@launch
            }
            if (!dataReady || stage == STAGE_DATA) {
                setStage(STAGE_DATA)
                state = P40State.NeedData
                pct(62, "Need DATA")
                log("Langkah 4/7: pasang DATA game setelah OBB siap.")
                return@launch
            }
            if (stage != STAGE_FIRST_RUN && stage != STAGE_READY) setStage(STAGE_FINALIZE)
            if (stage == STAGE_FINALIZE) {
                state = P40State.NeedFinalizeApk
                pct(78, "Need APK finalize")
                log("Langkah 5/7: install ulang APK sebagai update/finalize. Jangan buka game dulu.")
                return@launch
            }
            if (stage == STAGE_FIRST_RUN) {
                state = P40State.NeedFirstRun
                pct(88, "First setup")
                log(if (prefs.getBoolean("first_run_started", false)) "Jika installer hijau/Chinese sudah selesai, tekan First Setup Done." else "Langkah 6/7: buka first setup game dan tunggu installer internal selesai.")
                return@launch
            }
            state = P40State.Ready
            pct(100, "Ready")
            log("Langkah 7/7 selesai. FIFA 16 siap dimainkan.")
        }
    }

    fun downloadApk(openAfterDownload: Boolean = true) {
        val current = manifest ?: return decideState(false)
        state = P40State.Working
        pct(5, "Downloading APK")
        log("Download APK DLavie 26...")
        scope.launch {
            try {
                val apk = withContext(Dispatchers.IO) { installer.downloadAsset(current.apk) { p -> pct(p, "Downloading APK") } }
                pct(100, "APK downloaded")
                if (openAfterDownload) {
                    setStage(STAGE_APK_INSTALL)
                    prefs.edit().putBoolean("awaiting_apk_install", true).apply()
                    log("Installer Android dibuka. Setelah install, kembali ke DLavie. Jangan buka game dulu.")
                    installer.openApkInstaller(apk)
                } else {
                    log("APK siap dipakai untuk finalize.")
                }
            } catch (t: Throwable) {
                state = P40State.NeedApk
                pct(0, "APK download failed")
                log("Download APK gagal: ${t.message}")
            }
        }
    }

    fun installObb() {
        val current = manifest ?: return decideState(false)
        state = P40State.Working
        pct(5, "Install OBB")
        log("Install OBB DLavie dimulai...")
        scope.launch {
            try {
                withContext(Dispatchers.IO) { contentInstaller().installObbOnly(current) }
                obbReady = true
                setStage(STAGE_DATA)
                pct(100, "OBB ready")
                log("OBB selesai. Lanjut install DATA.")
                decideState(true)
            } catch (t: Throwable) {
                state = P40State.NeedObb
                pct(progress, "OBB install failed")
                log("Install OBB gagal: ${t.message}")
            }
        }
    }

    fun installData() {
        val current = manifest ?: return decideState(false)
        state = P40State.Working
        pct(5, "Install DATA")
        log("Install DATA DLavie dimulai...")
        scope.launch {
            try {
                withContext(Dispatchers.IO) { contentInstaller().installDataOnly(current) }
                dataReady = true
                setStage(STAGE_FINALIZE)
                pct(100, "DATA ready")
                log("DATA selesai. Sekarang finalize APK.")
                decideState(true)
            } catch (t: Throwable) {
                state = P40State.NeedData
                pct(progress, "DATA install failed")
                log("Install DATA gagal: ${t.message}")
            }
        }
    }

    fun finalizeApk() {
        val current = manifest ?: return decideState(false)
        state = P40State.Working
        pct(8, "Preparing APK finalize")
        log("Membuka installer APK ulang sebagai update/finalize...")
        scope.launch {
            try {
                val apk = withContext(Dispatchers.IO) { installer.downloadAsset(current.apk) { p -> pct(p, "Preparing APK finalize") } }
                prefs.edit().putBoolean("awaiting_apk_finalize", true).apply()
                installer.openApkInstaller(apk)
            } catch (t: Throwable) {
                state = P40State.NeedFinalizeApk
                pct(0, "Finalize failed")
                log("Finalize APK gagal: ${t.message}")
            }
        }
    }

    fun firstRunAction() {
        if (!prefs.getBoolean("first_run_started", false)) {
            prefs.edit().putBoolean("first_run_started", true).apply()
            crashReporter.prepareLaunch()
            log("First setup dibuka. Jika muncul installer hijau/Chinese, tunggu sampai selesai.")
            launchGame(context)
        } else {
            setStage(STAGE_READY)
            prefs.edit().putBoolean("first_run_done", true).apply()
            state = P40State.Ready
            pct(100, "Ready")
            log("First setup ditandai selesai. Siap Play FIFA 16.")
        }
    }

    fun mainAction() {
        when (state) {
            P40State.NeedApk -> downloadApk(true)
            P40State.NeedApkInstall -> downloadApk(true)
            P40State.NeedShizuku -> { ShizukuSetup.openApp(context); log("Buka Shizuku, start service, lalu kembali ke DLavie.") }
            P40State.NeedShizukuPermission -> { ShizukuSetup.requestPermission(); log("Pilih Allow di dialog Shizuku.") }
            P40State.NeedGamePermission -> {
                if (!prefs.getBoolean("game_permission_opened", false)) {
                    prefs.edit().putBoolean("game_permission_opened", true).apply()
                    log("Buka Permission/App Info FIFA 16. Aktifkan Files/Storage jika ada, lalu kembali.")
                    openGameSettings()
                } else {
                    prefs.edit().putBoolean("game_permission_ready", true).apply()
                    setStage(STAGE_OBB)
                    log("Permission ditandai selesai. Lanjut OBB.")
                    decideState(true)
                }
            }
            P40State.NeedObb -> installObb()
            P40State.NeedData -> installData()
            P40State.NeedFinalizeApk -> finalizeApk()
            P40State.NeedFirstRun -> firstRunAction()
            P40State.Ready -> playGameGuarded()
            P40State.Recovery -> decideState(false)
            P40State.Offline, P40State.Checking -> decideState(false)
            P40State.Working -> log("Proses sedang berjalan. Tunggu selesai.")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val crashReason = crashReporter.consumeFastReturn()
                if (crashReason != null && stage != STAGE_FIRST_RUN) {
                    state = P40State.Recovery
                    pct(0, "Game crash detected")
                    log("FIFA 16 crash: $crashReason. Report: ${crashReporter.reportFile().absolutePath}")
                } else {
                    if (prefs.getBoolean("awaiting_apk_install", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) {
                        prefs.edit().putBoolean("awaiting_apk_install", false).apply()
                        setStage(STAGE_PERMISSION)
                    }
                    if (prefs.getBoolean("awaiting_apk_finalize", false) && isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE)) {
                        prefs.edit().putBoolean("awaiting_apk_finalize", false).putBoolean("first_run_started", false).apply()
                        setStage(STAGE_FIRST_RUN)
                    }
                    decideState(true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { decideState(true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P40Card {
            Text("DLavie Auto Setup", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Android 12+ staged installer", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            P40StatusPill(state)
            Spacer(Modifier.height(10.dp))
            Text(message, color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = state != P40State.Working,
                onClick = { mainAction() },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)
            ) { Text(buttonLabel(), fontWeight = FontWeight.Black, fontSize = 17.sp) }
        }
        P40Readiness(gameInstalled, obbReady, dataReady, shizuku, access, local, latest, stage)
        P40FlowCard(stage, state)
        P40ContentCard(manifest)
        P40Progress(progress, step)
        P40Card {
            Text("Shizuku Helper", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(ShizukuSetup.shortHint(context), color = SoftText)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { ShizukuSetup.requestPermission(); decideState(true) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Grant", fontWeight = FontWeight.Bold) }
                Button(onClick = { ShizukuSetup.openApp(context) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open", fontWeight = FontWeight.Bold) }
            }
        }
        P40Card {
            Text("Quick Controls", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { decideState(false) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Recheck", fontWeight = FontWeight.Bold) }
                Button(onClick = { setStage(STAGE_PERMISSION); prefs.edit().remove("first_run_started").remove("first_run_done").apply(); decideState(true) }, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Restart Flow", fontWeight = FontWeight.Bold) }
            }
        }
        P40Card {
            Text("Recent Log", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            logs.takeLast(6).forEach { Text("• $it", color = SoftText, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun P40StatusPill(state: P40State) {
    val color = when (state) {
        P40State.Ready -> NeonGreen
        P40State.NeedApk, P40State.NeedApkInstall, P40State.NeedGamePermission, P40State.NeedObb, P40State.NeedData, P40State.NeedFinalizeApk, P40State.NeedFirstRun, P40State.NeedShizuku, P40State.NeedShizukuPermission -> Color(0xFFFFB84D)
        P40State.Recovery, P40State.Offline -> Color(0xFFFF5269)
        P40State.Working, P40State.Checking -> CandyBlue
    }
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0x33101827), border = BorderStroke(1.dp, color)) {
        Text(state.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun P40Readiness(game: Boolean, obb: Boolean, data: Boolean, shizuku: String, access: String, local: Int, latest: Int, stage: String) = P40Card {
    Text("Setup Readiness", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    InfoLine("Game APK", if (game) "Installed" else "Need download")
    InfoLine("OBB", if (obb) "Ready" else "Need install")
    InfoLine("DATA", if (data) "Ready" else "Need install")
    InfoLine("Shizuku", shizuku)
    InfoLine("Access", access)
    InfoLine("Version", if (latest > 0) "v$local / latest v$latest" else "v$local")
    InfoLine("Stage", stage)
    InfoLine("Community", "After production launch")
}

@Composable
private fun P40FlowCard(stage: String, state: P40State) = P40Card {
    Text("Safe Install Flow", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Android 12+ wajib bertahap. Jangan langsung Play sebelum finalize dan first setup.", color = SoftText, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    InfoLine("1", if (stage == STAGE_APK) "Download APK ← current" else "Download APK")
    InfoLine("2", if (state == P40State.NeedGamePermission) "Grant permission ← current" else "Grant permission")
    InfoLine("3", if (stage == STAGE_OBB) "Install OBB ← current" else "Install OBB")
    InfoLine("4", if (stage == STAGE_DATA) "Install DATA ← current" else "Install DATA")
    InfoLine("5", if (stage == STAGE_FINALIZE) "Finalize APK ← current" else "Finalize APK")
    InfoLine("6", if (stage == STAGE_FIRST_RUN) "First setup ← current" else "First setup")
    InfoLine("7", if (stage == STAGE_READY) "Ready / Play ← current" else "Ready / Play")
}

@Composable
private fun P40ContentCard(manifest: PublicInstallManifest?) = P40Card {
    Text("DLavie Content Source", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    if (manifest == null) Text("Manifest belum terbaca.", color = SoftText) else {
        InfoLine("Product", manifest.productName)
        InfoLine("APK", manifest.apk.versionName)
        InfoLine("DATA", manifest.data.versionName)
        InfoLine("OBB", manifest.obb.versionName)
        Text("Semua file berasal dari manifest DLavie dan diverifikasi SHA-256.", color = SoftText, fontSize = 13.sp)
    }
}

@Composable
private fun P40Progress(progress: Int, label: String) = P40Card {
    Text("Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    InfoLine("Step", label)
    Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) {
        Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp)))
    }
}

@Composable
private fun P40Help() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P40Card {
            Text("Help", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Alur Android 12+", color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text("DLavie sekarang mengikuti urutan manual yang aman: APK → permission → OBB → DATA → reinstall/update APK → first setup → play.", color = SoftText)
        }
        P40Card {
            Text("Catatan penting", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Jangan", "buka game setelah install APK pertama.")
            InfoLine("Tunggu", "installer hijau/Chinese di first setup sampai selesai.")
            InfoLine("Kalau crash", "launcher akan membuat crash report otomatis.")
        }
    }
}

@Composable
private fun P40More(api: CommunityApi, openSetup: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var status by remember { mutableStateOf("Advanced tools ready.") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        P40Card {
            Text("More", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Developer tools disimpan di sini agar Setup publik tetap bersih.", color = SoftText)
        }
        P40Card {
            Text("Advanced Tools", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { prefs.edit().putString(STAGE_KEY, STAGE_PERMISSION).remove("game_permission_opened").remove("first_run_started").remove("first_run_done").apply(); status = "Flow diulang dari permission." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Restart Safe Flow", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { prefs.edit().putString(STAGE_KEY, STAGE_READY).putBoolean("first_run_done", true).apply(); status = "Dipaksa Ready untuk test." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Force Ready Test", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = openSetup, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Back to Setup", fontWeight = FontWeight.Bold) }
        }
        P40Card {
            Text("Roadmap", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Now", "Safe staged public installer")
            InfoLine("Dashboard", "Separate developer app later")
            InfoLine("Login/community", "After production launch")
            InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" })
            InfoLine("Status", status)
        }
    }
}
