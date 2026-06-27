package com.drmacze.f16launcher

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val DEV_GAME_PACKAGE = "com.ea.gp.fifaworld"
private const val DEV_MANIFEST_URL = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json"
private const val DEV_NEWS_URL = "https://raw.githubusercontent.com/drmacze/F16/main/updates/news.json"

enum class DevPage(val label: String, val icon: String) {
    Home("Home", "⌂"),
    Update("Update", "↻"),
    Repair("Repair", "✦"),
    Chat("Chat", "◉"),
    Profile("Me", "☻")
}

data class NewsItem(val title: String, val body: String)
data class PatchPreview(val name: String, val type: String, val risk: String, val size: String, val description: String)

@Composable
fun DevelopmentMainShell(api: CommunityApi) {
    var page by remember { mutableStateOf(DevPage.Home) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "dev-page", modifier = Modifier.fillMaxSize().padding(bottom = 92.dp)) { target ->
            when (target) {
                DevPage.Home -> DevHomeScreen { page = DevPage.Update }
                DevPage.Update -> DevUpdateCenterScreen(openRepair = { page = DevPage.Repair })
                DevPage.Repair -> DevRepairScreen()
                DevPage.Chat -> CommunityScreen(api)
                DevPage.Profile -> DevProfileScreen(api)
            }
        }
        DevFloatingNav(page = page, onPage = { page = it }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
    }
}

@Composable
fun DevFloatingNav(page: DevPage, onPage: (DevPage) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 660.dp).padding(horizontal = 10.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xD80E1728),
        border = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 16.dp,
        tonalElevation = 0.dp
    ) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            DevPage.values().forEach { item ->
                val selected = page == item
                Button(
                    onClick = { onPage(item) },
                    modifier = Modifier.weight(1f).height(if (selected) 52.dp else 46.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.icon, fontSize = if (selected) 16.sp else 14.sp)
                        Text(item.label, fontSize = if (selected) 10.sp else 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun DevHomeScreen(openUpdate: () -> Unit) {
    val context = LocalContext.current
    val gameInstalled = remember { isPackageInstalled(context, DEV_GAME_PACKAGE) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("DLavie Launcher", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Development Mode • Login disabled", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text("Phase berikutnya aktif: Update Center, Repair Mode, remote news, dan akses Advanced Shizuku Updater dalam satu hub.", color = SoftText)
            Spacer(Modifier.height(16.dp))
            Button(onClick = openUpdate, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Update Center", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { launchGame(context) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) { Text("Launch FIFA 16", fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Game", if (gameInstalled) "Detected" else "Missing", Modifier.weight(1f))
            SmallGlassStat("Mode", "Dev", Modifier.weight(1f))
            SmallGlassStat("Build", "0.6.1", Modifier.weight(1f))
        }
        GlassCard {
            Text("Development Focus", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Now", "Stabilkan update GitHub + Shizuku/root flow.")
            InfoLine("Next", "Port apply patch langsung ke Compose tanpa membuka layar updater lama.")
        }
    }
}

@Composable
fun DevUpdateCenterScreen(openRepair: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("f16_launcher", 0) }
    var localVersion by remember { mutableStateOf(prefs.getInt("local_version_code", 1)) }
    var latestVersion by remember { mutableStateOf("-") }
    var latestName by remember { mutableStateOf("Belum dicek") }
    var serviceStatus by remember { mutableStateOf("Checking service...") }
    var status by remember { mutableStateOf("Ready") }
    var releaseNotes by remember { mutableStateOf<List<String>>(emptyList()) }
    var knownIssues by remember { mutableStateOf<List<String>>(emptyList()) }
    var patches by remember { mutableStateOf<List<PatchPreview>>(emptyList()) }
    var news by remember { mutableStateOf<List<NewsItem>>(emptyList()) }

    fun checkManifest() {
        status = "Mengecek GitHub manifest..."
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) { fetchJson(DEV_MANIFEST_URL) }
                val latestCode = json.optInt("latestVersionCode", localVersion)
                latestVersion = "v$latestCode"
                latestName = json.optString("latestVersionName", "v$latestCode")
                serviceStatus = json.optJSONObject("status")?.optString("message", "Online") ?: "Online"
                releaseNotes = jsonArrayStrings(json.optJSONArray("releaseNotes"))
                knownIssues = jsonArrayStrings(json.optJSONArray("knownIssues"))
                patches = parsePatchPreviews(json)
                status = if (latestCode > localVersion) "Update tersedia: v$localVersion → $latestName" else "Sudah versi terbaru: v$localVersion"
            } catch (t: Throwable) {
                status = "Check update gagal: ${t.message}"
                serviceStatus = "Offline / manifest tidak terbaca"
            }
        }
    }

    fun loadNews() {
        scope.launch {
            try {
                val json = withContext(Dispatchers.IO) { fetchJson(DEV_NEWS_URL) }
                val items = json.optJSONArray("items")
                news = if (items != null) List(items.length().coerceAtMost(4)) { i ->
                    val o = items.optJSONObject(i) ?: JSONObject()
                    NewsItem(o.optString("title", "News"), o.optString("body", ""))
                } else emptyList()
            } catch (_: Throwable) { }
        }
    }

    LaunchedEffect(Unit) { checkManifest(); loadNews() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("DLavie Update Center", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 2.2 • Smart update dashboard", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text(status, color = SoftText)
            InfoLine("Service", serviceStatus)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Local", "v$localVersion", Modifier.weight(1f))
            SmallGlassStat("Latest", latestVersion, Modifier.weight(1f))
            SmallGlassStat("Access", "Shizuku", Modifier.weight(1f))
        }
        GlassCard {
            Text("Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { checkManifest(); loadNews() }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) { Text("Refresh Manifest", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Advanced Shizuku Updater", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = openRepair, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB84D), contentColor = Color(0xFF1A1200))) { Text("Repair / Restore Center", fontWeight = FontWeight.Bold) }
        }
        if (patches.isNotEmpty()) GlassCard { Text("Patch Preview", fontSize = 20.sp, fontWeight = FontWeight.Bold); patches.forEach { PatchCard(it) } }
        if (releaseNotes.isNotEmpty()) GlassCard { Text("Release Notes", fontSize = 20.sp, fontWeight = FontWeight.Bold); releaseNotes.forEach { Text("• $it", color = SoftText) } }
        if (knownIssues.isNotEmpty()) GlassCard { Text("Known Issues", fontSize = 20.sp, fontWeight = FontWeight.Bold); knownIssues.forEach { Text("• $it", color = SoftText) } }
        if (news.isNotEmpty()) GlassCard { Text("DLavie News", fontSize = 20.sp, fontWeight = FontWeight.Bold); news.forEach { GlassListItem(it.title, it.body, false) {} } }
    }
}

@Composable
fun DevRepairScreen() {
    val context = LocalContext.current
    val gameInstalled = remember { isPackageInstalled(context, DEV_GAME_PACKAGE) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Repair / Restore Center", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Phase 2.3 preparation", fontSize = 15.sp, color = CandyCyan)
            Spacer(Modifier.height(12.dp))
            Text("Mode ini disiapkan untuk scan file penting, restore backup, dan repair patch yang gagal.", color = SoftText)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Game", if (gameInstalled) "Detected" else "Missing", Modifier.weight(1f))
            SmallGlassStat("Backup", "Legacy", Modifier.weight(1f))
            SmallGlassStat("Repair", "Ready", Modifier.weight(1f))
        }
        GlassCard {
            Text("Repair Checklist", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("1. Detect game", "Cek package FIFA 16 dan folder target.")
            InfoLine("2. Check manifest", "Bandingkan local version dengan manifest GitHub.")
            InfoLine("3. Backup/restore", "Gunakan restore dari Advanced Updater kalau patch gagal.")
            InfoLine("4. Re-apply", "Terapkan patch ulang via Shizuku/root.")
            Spacer(Modifier.height(10.dp))
            Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) { Text("Open Advanced Restore", fontWeight = FontWeight.Bold) }
        }
        GlassCard {
            Text("Restore Points", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            GlassListItem("Latest backup", "Dikelola oleh Advanced Updater lama. Akan dipindah ke Compose pada step berikutnya.", false) {}
            GlassListItem("Before next update", "Restore point otomatis akan dibuat sebelum patch real berikutnya.", false) {}
        }
    }
}

@Composable
fun DevProfileScreen(api: CommunityApi) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("Developer Mode", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Login sementara nonaktif", color = CandyCyan)
            InfoLine("User", api.displayName().ifEmpty { "DLavie Developer" })
            InfoLine("Username", "@${api.username().ifEmpty { "dev" }}")
            InfoLine("Target game", DEV_GAME_PACKAGE)
            InfoLine("Manifest", DEV_MANIFEST_URL)
        }
        GlassCard {
            Text("Next Implementation", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            InfoLine("Priority", "Port apply patch langsung dari Update Center Compose.")
            InfoLine("After", "Aktifkan login kembali setelah update flow stabil.")
        }
    }
}

@Composable
fun PatchCard(patch: PatchPreview) {
    GlassListItem(
        title = patch.name,
        subtitle = "${patch.type} • risk ${patch.risk} • ${patch.size}\n${patch.description}",
        selected = false
    ) {}
}

fun parsePatchPreviews(json: JSONObject): List<PatchPreview> {
    val arr = json.optJSONArray("patches") ?: return emptyList()
    return List(arr.length().coerceAtMost(5)) { i ->
        val p = arr.optJSONObject(i) ?: JSONObject()
        PatchPreview(
            name = p.optString("name", "Patch"),
            type = p.optString("type", "unknown"),
            risk = p.optString("riskLevel", "unknown"),
            size = bytesLabel(p.optLong("estimatedSizeBytes", 0L)),
            description = p.optString("description", "")
        )
    }
}

fun jsonArrayStrings(arr: org.json.JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
}

fun bytesLabel(bytes: Long): String {
    if (bytes <= 0L) return "unknown size"
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.1f KB", kb)
    return String.format("%.1f MB", kb / 1024.0)
}

fun isPackageInstalled(context: android.content.Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
