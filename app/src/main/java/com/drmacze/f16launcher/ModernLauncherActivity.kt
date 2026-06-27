package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

private const val GAME_PACKAGE = "com.ea.gp.fifaworld"

data class CategoryItem(val id: String, val name: String, val description: String)
data class TopicItem(val id: String, val title: String, val body: String, val replyCount: Int, val createdAt: String)
data class PostItem(val id: String, val authorId: String, val body: String, val createdAt: String)

enum class Page(val label: String, val icon: String) {
    Home("Home", "⌂"), Community("Community", "◉"), Plan("Plan", "◇"), Profile("Profile", "☻")
}

class ModernLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DLavieModernApp() }
    }
}

@Composable
fun DLavieModernApp() {
    val scheme = darkColorScheme(
        background = Carbon,
        surface = GlassBase,
        primary = CandyCyan,
        secondary = CandyBlue,
        onPrimary = Color(0xFF00111D),
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )
    MaterialTheme(colorScheme = scheme) {
        val context = LocalContext.current
        val api = remember { CommunityApi(context) }
        var loggedIn by remember { mutableStateOf(api.loggedIn()) }
        Surface(Modifier.fillMaxSize(), color = Carbon) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Carbon, Color(0xFF071B2C), Carbon)))
            ) {
                if (!loggedIn) AuthScreen(api) { loggedIn = true } else MainShell(api) { api.logout(); loggedIn = false }
            }
        }
    }
}

@Composable
fun AuthScreen(api: CommunityApi, onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Masuk dulu untuk membuka DLavie Hub.") }
    var loading by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp)) {
        val compact = maxWidth < 760.dp
        val hero: @Composable () -> Unit = {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("DLavie", fontSize = if (compact) 34.sp else 42.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Modern Mod Hub", fontSize = 18.sp, color = CandyCyan)
                Spacer(Modifier.height(14.dp))
                Text("Login page berada di awal. Setelah login, baru masuk Home, Community, Plan, dan Profile.", color = SoftText, fontSize = 15.sp)
                InfoLine("Username", "Wajib saat register, 3-24 karakter: huruf, angka, underscore.")
                InfoLine("Display name", "Wajib saat register. Ini nama tampil di community.")
                InfoLine("Avatar", "Opsional. Bisa dikosongkan dulu.")
            }
        }
        val form: @Composable () -> Unit = {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Account", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SoftText)
                Spacer(Modifier.height(10.dp))
                ModernField("Email", email) { email = it }
                ModernField("Password", password, password = true) { password = it }
                ModernField("Username wajib untuk register", username) { username = it }
                ModernField("Display name wajib untuk register", displayName) { displayName = it }
                ModernField("Avatar URL opsional", avatarUrl) { avatarUrl = it }
                Text(status, color = SoftText, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = !loading,
                    onClick = {
                        loading = true
                        status = "Login..."
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { api.login(email, password) }
                                status = "Login berhasil."
                                onSuccess()
                            } catch (t: Throwable) {
                                status = "Login gagal: ${t.message}. Jika akun belum dibuat, tekan Register."
                            } finally { loading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)
                ) { Text("Login", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = !loading,
                    onClick = {
                        if (!username.matches(Regex("[a-zA-Z0-9_]{3,24}"))) {
                            status = "Username wajib 3-24 karakter: huruf, angka, underscore."
                            return@Button
                        }
                        if (displayName.trim().length < 2) {
                            status = "Display name wajib minimal 2 karakter."
                            return@Button
                        }
                        loading = true
                        status = "Register..."
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { api.register(email, password, username, displayName, avatarUrl) }
                                if (api.loggedIn()) { status = "Register berhasil."; onSuccess() }
                                else status = "Register berhasil. Jika email confirmation aktif, cek email lalu login."
                            } catch (t: Throwable) {
                                status = "Register gagal: ${t.message}"
                            } finally { loading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))
                ) { Text("Register", fontWeight = FontWeight.Bold) }
            }
        }
        if (compact) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) { hero(); form() }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { hero() }
                Box(Modifier.weight(1f)) { form() }
            }
        }
    }
}

@Composable
fun MainShell(api: CommunityApi, onLogout: () -> Unit) {
    var page by remember { mutableStateOf(Page.Home) }
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = page,
            label = "page",
            modifier = Modifier.fillMaxSize().padding(bottom = 92.dp)
        ) { target ->
            when (target) {
                Page.Home -> HomeScreen()
                Page.Community -> CommunityScreen(api)
                Page.Plan -> PlanScreen()
                Page.Profile -> ProfileScreen(api, onLogout)
            }
        }
        FloatingNav(
            page = page,
            onPage = { page = it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
        )
    }
}

@Composable
fun FloatingNav(page: Page, onPage: (Page) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 560.dp).padding(horizontal = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xD80E1728),
        border = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 16.dp,
        tonalElevation = 0.dp
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Page.values().forEach { item ->
                val selected = page == item
                Button(
                    onClick = { onPage(item) },
                    modifier = Modifier.weight(1f).height(if (selected) 52.dp else 46.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) CandyBlue else Color(0x00101827),
                        contentColor = if (selected) Color.White else SoftText
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 8.dp else 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(item.icon, fontSize = if (selected) 17.sp else 15.sp)
                        Text(item.label, fontSize = if (selected) 11.sp else 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Text("DLavie Hub", fontSize = 38.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("FIFA 16 Mobile Mod Center", fontSize = 16.sp, color = CandyCyan)
            Spacer(Modifier.height(14.dp))
            Text("Launcher baru tidak memaksa landscape. Navbar sekarang floating interactive seperti app modern.", color = SoftText)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val launch = context.packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
                    if (launch != null) context.startActivity(launch)
                    else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PACKAGE")))
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))
            ) { Text("Launch FIFA 16", fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Community", "Online", Modifier.weight(1f))
            SmallGlassStat("Backend", "Supabase", Modifier.weight(1f))
            SmallGlassStat("APK", "0.5.0", Modifier.weight(1f))
        }
        GlassCard {
            Text("Patch Engine", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Fitur update patch lama masih ada di codebase. Setelah UI stabil, patch engine akan dipindah penuh ke Compose agar seluruh app konsisten.", color = SoftText)
        }
    }
}

@Composable
fun CommunityScreen(api: CommunityApi) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var topics by remember { mutableStateOf<List<TopicItem>>(emptyList()) }
    var selectedTopic by remember { mutableStateOf<TopicItem?>(null) }
    var posts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var status by remember { mutableStateOf("Loading community...") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }

    fun loadTopics() {
        scope.launch {
            try {
                topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }
                status = if (topics.isEmpty()) "Belum ada topic." else "${topics.size} topic loaded."
            } catch (t: Throwable) { status = "Gagal load topic: ${t.message}" }
        }
    }

    LaunchedEffect(Unit) {
        try {
            categories = withContext(Dispatchers.IO) { jsonCategories(api.categories()) }
            selectedCategory = categories.firstOrNull()
            topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }
            status = "Community ready."
        } catch (t: Throwable) { status = "Community error: ${t.message}" }
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(14.dp)) {
        val compact = maxWidth < 900.dp
        if (compact) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChannelPanel(categories, selectedCategory) { selectedCategory = it; selectedTopic = null; posts = emptyList(); loadTopics() }
                TopicPanel(status, title, body, topics, selectedTopic, onTitle = { title = it }, onBody = { body = it }, onCreate = {
                    val cat = selectedCategory ?: return@TopicPanel
                    if (title.trim().length < 4 || body.trim().isEmpty()) { status = "Judul minimal 4 karakter dan isi wajib diisi."; return@TopicPanel }
                    scope.launch { try { val nt = withContext(Dispatchers.IO) { api.createTopic(cat.id, title, body) }; title = ""; body = ""; topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(cat.id)) }; selectedTopic = topics.firstOrNull { it.id == nt.optString("id") }; status = "Topic dibuat." } catch (t: Throwable) { status = "Gagal membuat topic: ${t.message}" } }
                }, onSelect = { topic -> selectedTopic = topic; scope.launch { try { posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) } } catch (t: Throwable) { status = "Gagal load thread: ${t.message}" } } })
                ThreadPanel(selectedTopic, posts, reply, onReply = { reply = it }, onSend = {
                    val topic = selectedTopic ?: return@ThreadPanel
                    if (reply.trim().isEmpty()) return@ThreadPanel
                    scope.launch { try { withContext(Dispatchers.IO) { api.createPost(topic.id, "", reply) }; reply = ""; posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) }; topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) } } catch (t: Throwable) { status = "Gagal reply: ${t.message}" } }
                })
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChannelPanel(categories, selectedCategory, Modifier.width(210.dp).fillMaxHeight()) { selectedCategory = it; selectedTopic = null; posts = emptyList(); loadTopics() }
                TopicPanel(status, title, body, topics, selectedTopic, Modifier.weight(1f).fillMaxHeight(), { title = it }, { body = it }, {
                    val cat = selectedCategory ?: return@TopicPanel
                    if (title.trim().length < 4 || body.trim().isEmpty()) { status = "Judul minimal 4 karakter dan isi wajib diisi."; return@TopicPanel }
                    scope.launch { try { val nt = withContext(Dispatchers.IO) { api.createTopic(cat.id, title, body) }; title = ""; body = ""; topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(cat.id)) }; selectedTopic = topics.firstOrNull { it.id == nt.optString("id") }; status = "Topic dibuat." } catch (t: Throwable) { status = "Gagal membuat topic: ${t.message}" } }
                }, { topic -> selectedTopic = topic; scope.launch { try { posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) } } catch (t: Throwable) { status = "Gagal load thread: ${t.message}" } } })
                ThreadPanel(selectedTopic, posts, reply, Modifier.weight(1f).fillMaxHeight(), { reply = it }) {
                    val topic = selectedTopic ?: return@ThreadPanel
                    if (reply.trim().isEmpty()) return@ThreadPanel
                    scope.launch { try { withContext(Dispatchers.IO) { api.createPost(topic.id, "", reply) }; reply = ""; posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) }; topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) } } catch (t: Throwable) { status = "Gagal reply: ${t.message}" } }
                }
            }
        }
    }
}

@Composable
fun ChannelPanel(categories: List<CategoryItem>, selected: CategoryItem?, modifier: Modifier = Modifier, onSelect: (CategoryItem) -> Unit) {
    GlassCard(modifier) {
        Text("Channels", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        categories.forEach { c ->
            OutlinedButton(onClick = { onSelect(c) }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, if (selected?.id == c.id) CandyCyan else GlassStroke)) { Text(c.name) }
        }
    }
}

@Composable
fun TopicPanel(status: String, title: String, body: String, topics: List<TopicItem>, selected: TopicItem?, modifier: Modifier = Modifier, onTitle: (String) -> Unit, onBody: (String) -> Unit, onCreate: () -> Unit, onSelect: (TopicItem) -> Unit) {
    GlassCard(modifier) {
        Text("Topics", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(status, color = SoftText, fontSize = 12.sp)
        ModernField("Judul topic baru", title, onChange = onTitle)
        ModernField("Isi topic baru, bisa tag @username", body, onChange = onBody)
        Button(onClick = onCreate, colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D)), modifier = Modifier.fillMaxWidth()) { Text("New Topic") }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topics) { topic -> GlassListItem(topic.title, "${topic.replyCount} replies • ${topic.createdAt.take(10)}", selected?.id == topic.id) { onSelect(topic) } }
        }
    }
}

@Composable
fun ThreadPanel(topic: TopicItem?, posts: List<PostItem>, reply: String, modifier: Modifier = Modifier, onReply: (String) -> Unit, onSend: () -> Unit) {
    GlassCard(modifier) {
        Text(topic?.title ?: "Thread", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(topic?.body ?: "Pilih topic untuk membaca dan membalas.", color = SoftText)
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(posts) { p -> GlassListItem("user:${p.authorId.take(8)}", p.body, false) {} }
        }
        ModernField("Reply, bisa tag @username", reply, onChange = onReply)
        Button(onClick = onSend, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B)), modifier = Modifier.fillMaxWidth()) { Text("Send Reply") }
        Text("Upload file/screenshot: next step setelah Storage bucket aktif.", color = SoftText, fontSize = 12.sp)
    }
}

@Composable
fun PlanScreen() {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        GlassCard {
            Text("Upgrade Plan", fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text("Coming Soon", fontSize = 22.sp, color = CandyCyan, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Subscription dikosongkan dulu. Tidak ada checkout palsu, benefit palsu, atau tombol pembayaran dummy.", color = SoftText)
        }
    }
}

@Composable
fun ProfileScreen(api: CommunityApi, onLogout: () -> Unit) {
    var autoCheck by remember { mutableStateOf(false) }
    var autoLaunch by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(70.dp).background(Brush.linearGradient(listOf(CandyCyan, CandyBlue)), CircleShape), contentAlignment = Alignment.Center) { Text("DL", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White) }
                Spacer(Modifier.width(14.dp))
                Column { Text(api.displayName().ifEmpty { "DLavie User" }, fontSize = 26.sp, fontWeight = FontWeight.Black); Text("@${api.username().ifEmpty { "unknown" }}", color = SoftText) }
            }
            Spacer(Modifier.height(14.dp))
            InfoLine("Profile avatar", "Opsional. Avatar upload akan aktif setelah Storage bucket aktif.")
            InfoLine("Backend", CommunityApi.SUPABASE_URL)
            InfoLine("Target game", GAME_PACKAGE)
        }
        GlassCard {
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            SettingRow("Auto check update", autoCheck) { autoCheck = it }
            SettingRow("Auto launch after patch", autoLaunch) { autoLaunch = it }
            Text("Theme toggle light/dark akan saya aktifkan penuh setelah patch engine selesai dipindah ke Compose.", color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5269)), modifier = Modifier.fillMaxWidth()) { Text("Logout", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable Column.() -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC101827)), border = BorderStroke(1.dp, GlassStroke)) { Column(modifier = Modifier.padding(18.dp), content = content) }
}

@Composable
fun ModernField(label: String, value: String, password: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = false)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun InfoLine(title: String, body: String) { Spacer(Modifier.height(10.dp)); Text(title, color = SoftText, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(body, color = Color.White, fontSize = 14.sp) }

@Composable
fun SmallGlassStat(title: String, value: String, modifier: Modifier = Modifier) { GlassCard(modifier = modifier) { Text(title, color = SoftText, fontSize = 12.sp); Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }

@Composable
fun GlassListItem(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = if (selected) Color(0x5539D8FF) else Color(0x66172132), border = BorderStroke(1.dp, if (selected) CandyCyan else GlassStroke), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(subtitle, color = SoftText, fontSize = 12.sp) }
    }
}

@Composable
fun SettingRow(title: String, value: Boolean, onChange: (Boolean) -> Unit) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, modifier = Modifier.weight(1f), color = Color.White); Switch(checked = value, onCheckedChange = onChange) } }

fun jsonCategories(arr: JSONArray): List<CategoryItem> = List(arr.length()) { i -> val o = arr.getJSONObject(i); CategoryItem(o.optString("id"), o.optString("name"), o.optString("description")) }
fun jsonTopics(arr: JSONArray): List<TopicItem> = List(arr.length()) { i -> val o = arr.getJSONObject(i); TopicItem(o.optString("id"), o.optString("title"), o.optString("body"), o.optInt("reply_count"), o.optString("created_at")) }
fun jsonPosts(arr: JSONArray): List<PostItem> = List(arr.length()) { i -> val o = arr.getJSONObject(i); PostItem(o.optString("id"), o.optString("author_id"), o.optString("body"), o.optString("created_at")) }

val Carbon = Color(0xFF050812)
val GlassBase = Color(0xFF101827)
val CandyCyan = Color(0xFF27C8FF)
val CandyBlue = Color(0xFF6C63FF)
val NeonGreen = Color(0xFF1FDD90)
val SoftText = Color(0xFFA8B5CC)
val GlassStroke = Color(0x665D8DFF)
