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

private enum class P36Page(val label: String, val icon: String) {
    Home("Home", "⌂"), Update("Update", "↻"), Repair("Repair", "✦"), Tools("Tools", "⚙"), Me("Me", "☻")
}

@Composable
fun Phase36MainShell(api: CommunityApi) {
    var page by remember { mutableStateOf(P36Page.Home) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "phase36", modifier = Modifier.fillMaxSize().padding(bottom = 94.dp)) { target ->
            when (target) {
                P36Page.Home -> P36Home { page = P36Page.Update }
                P36Page.Update -> P36Update({ page = P36Page.Repair }, { page = P36Page.Tools })
                P36Page.Repair -> P36Repair()
                P36Page.Tools -> P36Tools { page = P36Page.Update }
                P36Page.Me -> P36Profile(api)
            }
        }
        P36Nav(page, { page = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun P36Nav(page: P36Page, onPage: (P36Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 660.dp).padding(horizontal = 10.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 18.dp, tonalElevation = 0.dp) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            P36Page.values().forEach { item ->
                val selected = item == page
                Button(onClick = { onPage(item) }, modifier = Modifier.weight(1f).height(if (selected) 52.dp else 46.dp), shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText), elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(item.icon, fontSize = if (selected) 16.sp else 14.sp, maxLines = 1); Text(item.label, fontSize = if (selected) 10.sp else 9.sp, maxLines = 1, overflow = TextOverflow.Clip, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                }
            }
        }
    }
}

@Composable
private fun P36Home(openUpdate: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { DevPatchEngine(context) {} }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("DLavie Launcher", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Phase 3.6 • Smart Update UX", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); Text("Update Center sekarang punya smart action, retest cepat, recovery state, dan ringkasan update yang lebih jelas.", color = SoftText); Spacer(Modifier.height(16.dp)); Button(onClick = openUpdate, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Smart Update Center", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { launchGame(context) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Launch FIFA 16", fontWeight = FontWeight.Bold) } }
        P36StatusCard(isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE), engine.accessMode(), engine.localVersion())
    }
}

@Composable
private fun P36Update(openRepair: () -> Unit, openTools: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var local by remember { mutableStateOf(1) }
    var latest by remember { mutableStateOf(0) }
    var latestName by remember { mutableStateOf("-") }
    var access by remember { mutableStateOf("Checking...") }
    var service by remember { mutableStateOf("-") }
    var status by remember { mutableStateOf("Ready") }
    var state by remember { mutableStateOf(prefs.getString("update_state", "idle") ?: "idle") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var safe by remember { mutableStateOf("Unknown") }
    var risk by remember { mutableStateOf("-") }
    var ready by remember { mutableStateOf(false) }
    var autoLaunch by remember { mutableStateOf(prefs.getBoolean("auto_launch_after_update", false)) }
    var patches by remember { mutableStateOf<List<PatchPreview>>(emptyList()) }
    var logs by remember { mutableStateOf(listOf("Siap.")) }
    fun log(s: String) { logs = (logs + s).takeLast(80); status = s }
    fun pct(p: Int, s: String) { progress = p.coerceIn(0, 100); step = s }
    val engine = remember { DevPatchEngine(context) { m -> scope.launch(Dispatchers.Main) { log(m) } } }
    fun refresh() { ready = false; pct(8, "Fetching manifest"); scope.launch { try { val m = withContext(Dispatchers.IO) { engine.fetchManifest() }; pct(55, "Parsing manifest"); local = engine.localVersion(); latest = m.optInt("latestVersionCode", local); latestName = m.optString("latestVersionName", "v$latest"); access = engine.accessMode(); service = m.optJSONObject("status")?.optString("message", "Online") ?: "Online"; patches = parsePatchPreviews(m); val p = m.optJSONArray("patches")?.optJSONObject(0); safe = if (p?.optBoolean("safeToAutoApply", false) == true) "Yes" else "No"; risk = p?.optString("riskLevel", "-") ?: "-"; ready = true; status = if (latest > local) "Update tersedia: v$local → v$latest" else "Sudah versi terbaru: v$local"; pct(100, "Manifest ready") } catch (t: Throwable) { prefs.edit().putString("update_last_error", t.message ?: "unknown").apply(); ready = false; pct(0, "Manifest failed"); log("Refresh gagal: ${t.message}") } } }
    fun apply(retest: Boolean) { prefs.edit().putString("update_state", "running").putLong("update_started_at", System.currentTimeMillis()).apply(); state = "running"; pct(5, if (retest) "Preparing retest" else "Preparing update"); scope.launch { try { if (retest) { withContext(Dispatchers.IO) { engine.resetLocalVersion(1) }; local = 1; log("Local version di-reset ke v1.") }; pct(25, "Applying update"); withContext(Dispatchers.IO) { engine.applyAvailableUpdates() }; local = engine.localVersion(); access = engine.accessMode(); prefs.edit().putString("update_state", "done").remove("update_last_error").apply(); state = "done"; pct(100, "Update completed"); log("Direct update selesai. Local v$local"); if (autoLaunch) launchGame(context) } catch (t: Throwable) { prefs.edit().putString("update_state", "failed").putString("update_last_error", t.message ?: "unknown").apply(); state = "failed"; pct(progress, "Update failed"); log("Direct update gagal: ${t.message}") } } }
    LaunchedEffect(Unit) { local = engine.localVersion(); access = engine.accessMode(); state = prefs.getString("update_state", "idle") ?: "idle"; if (state == "running") log("Update sebelumnya terputus. Gunakan Repair/Restore atau ulangi update."); refresh() }
    val updateAvailable = ready && latest > local
    val upToDate = ready && latest > 0 && latest <= local
    val hasAccess = access.contains("Shizuku") || access.contains("Root")
    val smart = when { state == "failed" -> "Needs recovery"; state == "running" -> "Running / interrupted"; !ready -> "Manifest not ready"; !hasAccess -> "Need access"; updateAvailable -> "Update available"; upToDate -> "Up to date"; else -> "Ready" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("Smart Update Center", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Phase 3.6 • Smart actions", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); Text(status, color = SoftText); InfoLine("Service", service) }
        P36StatusCard(isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE), access, local)
        GlassCard { Text("Smart Summary", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Status", smart); InfoLine("Latest", if (latest > 0) "$latestName / v$latest" else "-"); InfoLine("Safe auto apply", safe); InfoLine("Risk", risk); InfoLine("Auto launch after update", if (autoLaunch) "On" else "Off") }
        P36ProgressCard(progress, step, state, prefs.getString("update_last_error", "-") ?: "-")
        GlassCard { Text("Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold); Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Refresh Manifest", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(enabled = updateAvailable && hasAccess && state != "running", onClick = { apply(false) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) { Text(if (upToDate) "Already Up To Date" else "Apply Update Direct", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { apply(true) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("One Tap Retest v1 → Latest", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openRepair, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Repair / Restore", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { autoLaunch = !autoLaunch; prefs.edit().putBoolean("auto_launch_after_update", autoLaunch).apply() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF182238), contentColor = Color.White)) { Text(if (autoLaunch) "Auto Launch: ON" else "Auto Launch: OFF", fontWeight = FontWeight.Bold) } }
        GlassCard { Text("Recovery", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Kalau update gagal/terputus, buka Repair untuk restore atau Tools untuk clear state.", color = SoftText); Spacer(Modifier.height(8.dp)); Button(onClick = openTools, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Open Tools", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24334E))) { Text("Fallback Advanced Updater", fontWeight = FontWeight.Bold) } }
        if (patches.isNotEmpty()) GlassCard { Text("Patch Preview", fontSize = 20.sp, fontWeight = FontWeight.Bold); patches.forEach { PatchCard(it) } }
        GlassCard { Text("Activity Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P36Repair() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready") }
    var backup by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var access by remember { mutableStateOf("Checking...") }
    var progress by remember { mutableStateOf(0) }
    var step by remember { mutableStateOf("Idle") }
    var logs by remember { mutableStateOf(listOf("Repair Center siap.")) }
    fun log(s: String) { logs = (logs + s).takeLast(80); status = s }
    val engine = remember { DevPatchEngine(context) { m -> scope.launch(Dispatchers.Main) { log(m) } } }
    fun refresh() { backup = engine.latestBackupRoot(); target = engine.latestBackupTarget(); access = engine.accessMode(); progress = 100; step = "Backup state refreshed"; status = "Repair data refreshed." }
    fun restore() { progress = 10; step = "Preparing restore"; scope.launch { try { progress = 35; step = "Restoring latest backup"; withContext(Dispatchers.IO) { engine.restoreLastBackup() }; progress = 100; step = "Restore completed"; log("Restore selesai.") } catch (t: Throwable) { step = "Restore failed"; log("Restore gagal: ${t.message}") } } }
    LaunchedEffect(Unit) { refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("Repair / Restore", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Phase 3.6 • Guided recovery", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); Text(status, color = SoftText) }
        P36StatusCard(isPackageInstalled(context, DevPatchEngine.GAME_PACKAGE), access, engine.localVersion())
        P36ProgressCard(progress, step, if (backup.isBlank()) "no_backup" else "ready", "-")
        GlassCard { Text("Restore Point", fontSize = 20.sp, fontWeight = FontWeight.Bold); if (backup.isBlank()) Text("Belum ada restore point. Restore point dibuat saat patch mengganti file lama.", color = SoftText) else GlassListItem("Latest restore point", "$backup\nTarget: $target", true) {}; Spacer(Modifier.height(10.dp)); Button(onClick = { refresh() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Refresh Backup State", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(enabled = backup.isNotBlank(), onClick = { restore() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200), disabledContainerColor = Color(0x55303B4B), disabledContentColor = SoftText)) { Text("Restore Latest Backup Direct", fontWeight = FontWeight.Bold) } }
        GlassCard { Text("Repair Log", fontSize = 20.sp, fontWeight = FontWeight.Bold); logs.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) } }
    }
}

@Composable
private fun P36Tools(openUpdate: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var status by remember { mutableStateOf("Dev tools ready.") }
    var local by remember { mutableStateOf(prefs.getInt("local_version_code", 1)) }
    val engine = remember { DevPatchEngine(context) { status = it } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Text("Developer Tools", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Retest update tanpa reinstall APK", fontSize = 15.sp, color = CandyCyan); Spacer(Modifier.height(12.dp)); Text(status, color = SoftText) }
        GlassCard { Text("Version & State", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Current", "v$local"); Button(onClick = { engine.resetLocalVersion(1); local = 1; status = "Local version diset ke v1." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Reset Local Version to v1", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { prefs.edit().remove("update_state").remove("update_last_error").apply(); status = "Update state cleared." }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Clear Update State", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = openUpdate, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Go to Update Center", fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Legacy Advanced Updater", fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun P36StatusCard(gameInstalled: Boolean, access: String, local: Int) { GlassCard { Text("System Status", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Game", if (gameInstalled) "Detected" else "Missing"); InfoLine("Access", access); InfoLine("Local Version", "v$local") } }
@Composable
private fun P36ProgressCard(progress: Int, label: String, state: String, error: String) { GlassCard { Text("Update Progress", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("State", state); InfoLine("Step", label); Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp); Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55293650), RoundedCornerShape(8.dp))) { Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0.02f, 1f)).height(12.dp).background(CandyCyan, RoundedCornerShape(8.dp))) }; if (state == "failed") InfoLine("Last error", error) } }
@Composable
private fun P36Profile(api: CommunityApi) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { GlassCard { Text("Developer Profile", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Color.White); Text("Login sementara nonaktif", color = CandyCyan); InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" }); InfoLine("Target game", DevPatchEngine.GAME_PACKAGE); InfoLine("Updater", "Smart Direct Compose Engine") }; GlassCard { Text("Next Phase", fontSize = 20.sp, fontWeight = FontWeight.Bold); InfoLine("Phase 4", "Aktifkan kembali login/community dengan guard agar tidak ganggu Update Center."); InfoLine("After", "Upload screenshot/log bug report dari launcher.") } } }
