package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// ─── Design tokens ────────────────────────────────────────────────────────────
val Carbon      = Color(0xFF050812)
val GlassBase   = Color(0xFF101827)
val CandyCyan   = Color(0xFF27C8FF)
val CandyBlue   = Color(0xFF6C63FF)
val NeonGreen   = Color(0xFF1FDD90)
val SoftText    = Color(0xFFA8B5CC)
val GlassStroke = Color(0x665D8DFF)
val DangerRed   = Color(0xFFFF5269)

// ─── Constants ────────────────────────────────────────────────────────────────
private const val GAME_PKG       = "com.ea.gp.fifaworld"
private const val DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json"
private const val MARKER_PATH    = "/sdcard/Android/data/com.ea.gp.fifaworld/.dlavie26_data_installed"
private const val LOCAL_VER      = 1
private const val LOCAL_VER_NAME = "v1"

// ─── Data models ──────────────────────────────────────────────────────────────
data class CategoryItem(val id: String, val name: String, val description: String)
data class TopicItem(val id: String, val title: String, val body: String, val replyCount: Int, val createdAt: String)
data class PostItem(val id: String, val authorId: String, val body: String, val createdAt: String)
data class FeedItem(val id: String, val title: String, val body: String, val type: String, val pinned: Boolean, val official: Boolean)
data class UpdateInfo(val latestCode: Int, val latestName: String, val upToDate: Boolean, val releaseNotes: List<String>)

// ─── Navigation ───────────────────────────────────────────────────────────────
enum class Page(val label: String, val icon: String) {
    Home("Home", "⌂"), Data("Data", "▣"), Chat("Chat", "◉"), Me("Me", "☻")
}

// ─── Activity ─────────────────────────────────────────────────────────────────
class ModernLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DLavieModernApp() }
    }
}

// ─── Root ─────────────────────────────────────────────────────────────────────
@Composable
fun DLavieModernApp() {
    val context = LocalContext.current
    val api     = remember { CommunityApi(context) }
    MaterialTheme(colorScheme = darkColorScheme(
        background  = Carbon,  surface    = GlassBase,
        primary     = CandyCyan, secondary  = CandyBlue,
        onPrimary   = Color(0xFF00111D), onSecondary = Color.White,
        onBackground= Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Carbon) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Carbon, Color(0xFF071B2C), Carbon)))) {
                if (!api.loggedIn()) {
                    LaunchedEffect(Unit) {
                        context.startActivity(
                            Intent(context, DLavieGuidedActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = CandyCyan)
                            Text("Sesi berakhir, mengarahkan ke login...", color = SoftText, fontSize = 14.sp)
                        }
                    }
                } else {
                    MainShell(api) {
                        api.logout()
                        context.getSharedPreferences("dlavie_auth_session", Context.MODE_PRIVATE).edit().clear().apply()
                        context.startActivity(
                            Intent(context, DLavieGuidedActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        }
    }
}

// ─── Main shell ───────────────────────────────────────────────────────────────
@Composable
fun MainShell(api: CommunityApi, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(Page.Home) }

    // Token auto-refresh every 50 minutes
    LaunchedEffect(Unit) {
        while (true) {
            delay(50L * 60 * 1000)
            withContext(Dispatchers.IO) { runCatching { api.refreshToken() } }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "page", modifier = Modifier.fillMaxSize().padding(bottom = 92.dp)) { target ->
            when (target) {
                Page.Home -> HomeScreen(api, onNav = { page = it })
                Page.Data -> DataScreen(onNav  = { page = it })
                Page.Chat -> CommunityScreen(api)
                Page.Me   -> ProfileScreen(api, onLogout)
            }
        }
        FloatingNav(page = page, onPage = { page = it }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
    }
}

// ─── Floating nav ─────────────────────────────────────────────────────────────
@Composable
fun FloatingNav(page: Page, onPage: (Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.widthIn(max = 640.dp).padding(horizontal = 12.dp), shape = RoundedCornerShape(32.dp), color = Color(0xD80E1728), border = BorderStroke(1.dp, GlassStroke), shadowElevation = 16.dp, tonalElevation = 0.dp) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Page.values().forEach { item ->
                val selected = page == item
                Button(onClick = { onPage(item) }, modifier = Modifier.weight(1f).height(if (selected) 52.dp else 46.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) CandyBlue else Color.Transparent, contentColor = if (selected) Color.White else SoftText), elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(item.icon, fontSize = if (selected) 17.sp else 15.sp); Text(item.label, fontSize = if (selected) 11.sp else 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                }
            }
        }
    }
}

// ─── Home screen ──────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(api: CommunityApi, onNav: (Page) -> Unit) {
    val context = LocalContext.current
    val gameInstalled = remember { isGameInstalled(context) }
    val marker        = remember { readMarker() }
    val dataReady     = marker.startsWith("v26") || marker.startsWith("V26")

    var updateInfo    by remember { mutableStateOf<UpdateInfo?>(null) }
    var feed          by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var loadingHome   by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { updateInfo = fetchUpdateInfo() }
            runCatching { feed = parseFeed(api.feedPosts()) }
        }
        loadingHome = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ── Header ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Brush.linearGradient(listOf(CandyCyan, CandyBlue)), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text("DL", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DLavie 26", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Surface(color = NeonGreen.copy(0.15f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, NeonGreen.copy(0.5f))) {
                            Text("PROD", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                    Text("FIFA 16 Mobile 2026", color = SoftText, fontSize = 12.sp)
                }
                Surface(color = CandyBlue.copy(0.15f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, CandyBlue.copy(0.4f))) {
                    Text(
                        api.displayName().ifEmpty { "Player" }.take(12),
                        color = CandyCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Football Reborn", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Play, update, and connect with DLavie.", color = SoftText, fontSize = 13.sp)
        }

        // ── Actions ──
        GlassCard {
            Text("▶  Actions", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (gameInstalled) launchGame(context)
                    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PKG")))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (gameInstalled) NeonGreen else SoftText.copy(0.3f), contentColor = if (gameInstalled) Color(0xFF00150B) else Color.White)
            ) { Text("▶   Play FIFA 16", fontSize = 17.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("Data", color = CandyCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNav(Page.Data) }.padding(horizontal = 14.dp, vertical = 8.dp))
                Text("Update", color = CandyCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { context.startActivity(Intent(context, GameHubActivity::class.java)) }.padding(horizontal = 14.dp, vertical = 8.dp))
                Text("Chat", color = CandyCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNav(Page.Chat) }.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }

        // ── Status chips ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusChip(
                title = "Game",
                value = if (gameInstalled) "Ready" else "Missing",
                ok    = gameInstalled,
                loading = false,
                modifier = Modifier.weight(1f)
            ) { if (!gameInstalled) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PKG"))) }
            StatusChip(
                title   = "Update",
                value   = if (loadingHome) "Cek..." else if (updateInfo?.upToDate != false) "Secure" else "Tersedia",
                ok      = updateInfo?.upToDate != false,
                loading = loadingHome,
                modifier = Modifier.weight(1f)
            ) { context.startActivity(Intent(context, GameHubActivity::class.java)) }
        }

        // ── Data warning ──
        AnimatedVisibility(!dataReady) {
            GlassCard(borderColor = DangerRed.copy(0.5f)) {
                Text("⚠  Base Data Belum Siap", color = DangerRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Buka tab Data untuk install base data FIFA 16. Patch tidak akan bekerja tanpa data.", color = SoftText, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onNav(Page.Data) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = DangerRed)) {
                    Text("Buka Data Installer", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Update available ──
        val ui = updateInfo
        AnimatedVisibility(ui != null && !ui.upToDate) {
            if (ui != null) GlassCard(borderColor = CandyCyan.copy(0.5f)) {
                Text("🔄  Update Tersedia: ${ui.latestName}", color = CandyCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (ui.releaseNotes.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    ui.releaseNotes.take(3).forEach { Text("• $it", color = SoftText, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) {
                    Text("Buka Advanced Updater", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Feed announcements ──
        AnimatedVisibility(feed.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📢  Announcements", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                feed.forEach { post ->
                    GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(post.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (post.pinned) Surface(color = NeonGreen.copy(0.2f), shape = RoundedCornerShape(999.dp)) {
                                Text("PIN", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                            if (post.official) Surface(color = CandyCyan.copy(0.2f), shape = RoundedCornerShape(999.dp)) {
                                Text("OFFICIAL", color = CandyCyan, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(post.body, color = SoftText, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ─── Data screen ──────────────────────────────────────────────────────────────
@Composable
fun DataScreen(onNav: (Page) -> Unit) {
    val context       = LocalContext.current
    val gameInstalled = remember { isGameInstalled(context) }
    val marker        = remember { readMarker() }
    val dataReady     = marker.startsWith("v26") || marker.startsWith("V26")

    var updateInfo  by remember { mutableStateOf<UpdateInfo?>(null) }
    var loading     by remember { mutableStateOf(true) }
    var updateError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun checkUpdate() {
        loading = true; updateError = ""
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { fetchUpdateInfo() } }
            r.fold({ updateInfo = it }, { updateError = it.message ?: "Gagal cek update" })
            loading = false
        }
    }

    LaunchedEffect(Unit) { checkUpdate() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        GlassCard {
            Text("Data & Update", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Status base data + update manifest dari GitHub.", color = SoftText, fontSize = 13.sp)
        }

        // ── Status row ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("APK FIFA", if (gameInstalled) "Terinstall" else "Belum ada", gameInstalled, Modifier.weight(1f))
            InfoTile("Base Data", if (dataReady) "Ready" else "Belum siap", dataReady, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("Local", LOCAL_VER_NAME, true, Modifier.weight(1f))
            InfoTile("Latest", if (loading) "..." else updateInfo?.latestName ?: "—", updateInfo?.upToDate != false, Modifier.weight(1f))
        }

        // ── Marker ──
        GlassCard {
            Text("Data Marker", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(marker.take(80).ifBlank { "Marker belum terdeteksi — base data belum diinstall." }, color = Color.White, fontSize = 13.sp)
        }

        // ── Update status ──
        if (updateError.isNotBlank()) {
            GlassCard(borderColor = DangerRed.copy(0.4f)) {
                Text("Gagal cek manifest", color = DangerRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(updateError, color = SoftText, fontSize = 12.sp)
            }
        }
        val ui = updateInfo
        if (ui != null && !ui.upToDate) {
            GlassCard(borderColor = CandyCyan.copy(0.5f)) {
                Text("🔄  Update Tersedia: ${ui.latestName}", color = CandyCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (ui.releaseNotes.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    ui.releaseNotes.forEach { Text("• $it", color = SoftText, fontSize = 12.sp) }
                }
            }
        }

        // ── Actions ──
        GlassCard {
            Text("Aksi", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { checkUpdate() }, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))) {
                Text(if (loading) "Mengecek..." else "Check Update", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, DLavieHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) {
                Text("Buka Data Installer", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { context.startActivity(Intent(context, GameHubActivity::class.java)) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2840))) {
                Text("Advanced Shizuku Updater", fontWeight = FontWeight.Bold, color = CandyCyan)
            }
        }

        if (dataReady && gameInstalled) {
            Button(onClick = { launchGame(context) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) {
                Text("▶   Main FIFA 16", fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }

        // ── Patch flow info ──
        GlassCard {
            Text("Patch Flow", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            listOf(
                "1. Check"  to "Baca latest.json dari GitHub manifest.",
                "2. Verify" to "SHA-256 diverifikasi untuk keamanan patch.",
                "3. Backup" to "File lama dibackup sebelum patch diapply.",
                "4. Apply"  to "Patch dicopy via Shizuku/root ke Android/data atau obb.",
                "5. Restore"to "Restore backup tersedia jika patch gagal."
            ).forEach { (t, b) -> Spacer(Modifier.height(8.dp)); Text(t, color = CandyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(b, color = SoftText, fontSize = 12.sp) }
            Spacer(Modifier.height(10.dp))
            Text("Manifest: $DEFAULT_MANIFEST", color = SoftText, fontSize = 11.sp)
        }
    }
}

// ─── Community screen ─────────────────────────────────────────────────────────
@Composable
fun CommunityScreen(api: CommunityApi) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var topics by remember { mutableStateOf<List<TopicItem>>(emptyList()) }
    var selectedTopic by remember { mutableStateOf<TopicItem?>(null) }
    var posts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var status by remember { mutableStateOf("Loading komunitas...") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }

    fun loadPosts(topic: TopicItem) { scope.launch { try { posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) } } catch (t: Throwable) { status = "Gagal load thread: ${t.message}" } } }
    fun loadTopics() { scope.launch { try { topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }; status = if (topics.isEmpty()) "Belum ada topik." else "${topics.size} topik." } catch (t: Throwable) { status = "Gagal load topik: ${t.message}" } } }
    fun createTopic() { val cat = selectedCategory; if (cat == null) { status = "Pilih channel dulu."; return }; if (title.trim().length < 4 || body.trim().isEmpty()) { status = "Judul minimal 4 karakter dan isi wajib diisi."; return }; scope.launch { try { val newTopic = withContext(Dispatchers.IO) { api.createTopic(cat.id, title, body) }; title = ""; body = ""; topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(cat.id)) }; selectedTopic = topics.firstOrNull { it.id == newTopic.optString("id") }; status = "Topik dibuat." } catch (t: Throwable) { status = "Gagal: ${t.message}" } } }
    fun sendReply() { val topic = selectedTopic; if (topic == null) { status = "Pilih topik dulu."; return }; if (reply.trim().isEmpty()) return; scope.launch { try { withContext(Dispatchers.IO) { api.createPost(topic.id, "", reply) }; reply = ""; posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) } } catch (t: Throwable) { status = "Gagal reply: ${t.message}" } } }

    LaunchedEffect(Unit) {
        try {
            categories = withContext(Dispatchers.IO) { jsonCategories(api.categories()) }
            selectedCategory = categories.firstOrNull()
            topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }
            status = if (topics.isEmpty()) "Belum ada topik." else "Siap."
        } catch (t: Throwable) { status = "Community error: ${t.message}" }
    }

    Box(Modifier.fillMaxSize().padding(14.dp)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChannelPanel(categories, selectedCategory) { selectedCategory = it; selectedTopic = null; posts = emptyList(); loadTopics() }
            TopicPanel(status, title, body, topics, selectedTopic, onTitle = { title = it }, onBody = { body = it }, onCreate = { createTopic() }, onSelect = { t -> selectedTopic = t; loadPosts(t) })
            ThreadPanel(selectedTopic, posts, reply, onReply = { reply = it }, onSend = { sendReply() })
        }
    }
}

// ─── Profile screen ───────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(api: CommunityApi, onLogout: () -> Unit) {
    val context = LocalContext.current
    val gameInstalled = remember { isGameInstalled(context) }
    var confirmLogout by remember { mutableStateOf(false) }
    val initial = api.displayName().firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    val role    = api.role()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ── Avatar + name ──
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).background(Brush.linearGradient(listOf(CandyCyan, CandyBlue)), CircleShape), contentAlignment = Alignment.Center) {
                    Text(initial, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(api.displayName().ifEmpty { "DLavie Player" }, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("@${api.username().ifEmpty { "unknown" }}", color = SoftText, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Surface(color = roleBadgeColor(role).copy(0.15f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, roleBadgeColor(role).copy(0.4f))) {
                        Text(role.uppercase(), color = roleBadgeColor(role), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
        }

        // ── Account info ──
        GlassCard {
            Text("Info Akun", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            ProfileInfoLine("Username",     "@${api.username().ifEmpty { "-" }}")
            ProfileInfoLine("Display Name", api.displayName().ifEmpty { "-" })
            ProfileInfoLine("Role",         role)
            ProfileInfoLine("Server",       CommunityApi.SUPABASE_URL)
            ProfileInfoLine("Game",         if (gameInstalled) "FIFA 16 terinstall ✓" else "FIFA 16 belum terinstall")
        }

        // ── Game ──
        GlassCard {
            Text("Game", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (gameInstalled) {
                Button(onClick = { launchGame(context) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))) {
                    Text("▶  Main FIFA 16", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PKG"))) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)) {
                    Text("Download FIFA 16", fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Logout ──
        if (!confirmLogout) {
            Button(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF5269), contentColor = DangerRed)) {
                Text("Logout", fontWeight = FontWeight.Bold)
            }
        } else {
            GlassCard(borderColor = DangerRed.copy(0.5f)) {
                Text("Konfirmasi Logout", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Kamu akan keluar dari akun. Sesi akan dihapus.", color = SoftText, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { confirmLogout = false }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, GlassStroke)) { Text("Batal", color = SoftText) }
                    Button(onClick = onLogout, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = DangerRed)) { Text("Logout", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─── Shared UI components ─────────────────────────────────────────────────────
@Composable
fun GlassCard(modifier: Modifier = Modifier, borderColor: Color = GlassStroke, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC101827)), border = BorderStroke(1.dp, borderColor)) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun StatusChip(title: String, value: String, ok: Boolean, loading: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(86.dp).clickable { onClick() }, shape = RoundedCornerShape(20.dp), color = Color(0xAA0D1525), border = BorderStroke(1.dp, if (ok) NeonGreen.copy(0.35f) else if (loading) GlassStroke else DangerRed.copy(0.35f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            if (loading) CircularProgressIndicator(color = CandyCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(if (ok) "✓" else "✗", color = if (ok) NeonGreen else DangerRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(title, color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = if (ok) NeonGreen else if (loading) SoftText else DangerRed, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
fun InfoTile(label: String, value: String, ok: Boolean, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Text(label, color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = if (ok) NeonGreen else DangerRed, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ProfileInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
        Text(value, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ModernField(label: String, value: String, password: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label, fontSize = 13.sp) }, visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = false)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun InfoLine(title: String, body: String) {
    Spacer(Modifier.height(8.dp)); Text(title, color = SoftText, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(body, color = Color.White, fontSize = 13.sp)
}

@Composable
fun SmallGlassStat(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) { Text(title, color = SoftText, fontSize = 11.sp); Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun GlassListItem(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = if (selected) Color(0x5539D8FF) else Color(0x66172132), border = BorderStroke(1.dp, if (selected) CandyCyan else GlassStroke), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(12.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(subtitle, color = SoftText, fontSize = 11.sp) }
    }
}

@Composable
fun ChannelPanel(categories: List<CategoryItem>, selected: CategoryItem?, modifier: Modifier = Modifier, onSelect: (CategoryItem) -> Unit) {
    GlassCard(modifier) {
        Text("Channels", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        if (categories.isEmpty()) Text("Memuat channels...", color = SoftText, fontSize = 12.sp)
        else categories.forEach { c ->
            OutlinedButton(onClick = { onSelect(c) }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), border = BorderStroke(1.dp, if (selected?.id == c.id) CandyCyan else GlassStroke)) {
                Text(c.name, color = if (selected?.id == c.id) CandyCyan else Color.White)
            }
        }
    }
}

@Composable
fun TopicPanel(status: String, title: String, body: String, topics: List<TopicItem>, selected: TopicItem?, modifier: Modifier = Modifier, onTitle: (String) -> Unit, onBody: (String) -> Unit, onCreate: () -> Unit, onSelect: (TopicItem) -> Unit) {
    GlassCard(modifier) {
        Text("Topik", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(status, color = SoftText, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        ModernField("Judul topik baru", title, onChange = onTitle)
        ModernField("Isi topik, bisa tag @username", body, onChange = onBody)
        Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Buat Topik Baru", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            topics.forEach { topic -> GlassListItem(topic.title, "${topic.replyCount} replies · ${topic.createdAt.take(10)}", selected?.id == topic.id) { onSelect(topic) } }
        }
    }
}

@Composable
fun ThreadPanel(topic: TopicItem?, posts: List<PostItem>, reply: String, modifier: Modifier = Modifier, onReply: (String) -> Unit, onSend: () -> Unit) {
    GlassCard(modifier) {
        Text(topic?.title ?: "Thread", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (topic != null) Text(topic.body, color = SoftText, fontSize = 13.sp)
        else Text("Pilih topik untuk membaca dan membalas.", color = SoftText, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            posts.forEach { p -> GlassListItem("user:${p.authorId.take(8)}", p.body, false) {} }
        }
        if (topic != null) {
            Spacer(Modifier.height(8.dp))
            ModernField("Tulis balasan (@username untuk mention)", reply, onChange = onReply)
            Button(onClick = onSend, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Kirim Balasan", fontWeight = FontWeight.Bold) }
        }
    }
}

// ─── Helper functions ─────────────────────────────────────────────────────────
fun isGameInstalled(context: android.content.Context): Boolean = try {
    context.packageManager.getPackageInfo(GAME_PKG, 0); true
} catch (_: Exception) { false }

fun readMarker(): String = try { File(MARKER_PATH).readText().trim() } catch (_: Exception) { "" }

fun fetchUpdateInfo(): UpdateInfo {
    val json = fetchJson(DEFAULT_MANIFEST)
    val latestCode = json.optInt("latestVersionCode", LOCAL_VER)
    val latestName = json.optString("latestVersionName", "v$latestCode")
    val notes = json.optJSONArray("releaseNotes")
    val releaseNotes = if (notes != null) List(notes.length()) { i -> notes.optString(i) } else emptyList()
    return UpdateInfo(latestCode, latestName, latestCode <= LOCAL_VER, releaseNotes)
}

fun parseFeed(arr: JSONArray): List<FeedItem> = try {
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        FeedItem(o.optString("id"), o.optString("title"), o.optString("body"), o.optString("type", "info"), o.optBoolean("pinned"), o.optBoolean("official"))
    }
} catch (_: Exception) { emptyList() }

fun roleBadgeColor(role: String): Color = when (role.lowercase()) {
    "admin"     -> DangerRed
    "moderator" -> NeonGreen
    "vip"       -> Color(0xFFFFD700)
    else        -> CandyCyan
}

fun launchGame(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(GAME_PKG)
    if (intent != null) context.startActivity(intent)
    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PKG")))
}

fun fetchJson(url: String): JSONObject {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 20000; c.readTimeout = 30000
    return try { BufferedReader(InputStreamReader(c.inputStream)).use { JSONObject(it.readText()) } } finally { c.disconnect() }
}

fun jsonCategories(arr: JSONArray): List<CategoryItem> = List(arr.length()) { i -> val o = arr.getJSONObject(i); CategoryItem(o.optString("id"), o.optString("name"), o.optString("description")) }
fun jsonTopics(arr: JSONArray): List<TopicItem>     = List(arr.length()) { i -> val o = arr.getJSONObject(i); TopicItem(o.optString("id"), o.optString("title"), o.optString("body"), o.optInt("reply_count"), o.optString("created_at")) }
fun jsonPosts(arr: JSONArray): List<PostItem>        = List(arr.length()) { i -> val o = arr.getJSONObject(i); PostItem(o.optString("id"), o.optString("author_id"), o.optString("body"), o.optString("created_at")) }
