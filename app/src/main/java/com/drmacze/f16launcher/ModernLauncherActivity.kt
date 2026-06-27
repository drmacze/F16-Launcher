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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
        background = Color(0xFF050812), surface = Color(0xFF101827), primary = CandyCyan,
        secondary = CandyBlue, onPrimary = Color(0xFF00111D), onSecondary = Color.White,
        onBackground = Color(0xFFF7FAFF), onSurface = Color(0xFFF7FAFF)
    )
    MaterialTheme(colorScheme = scheme) {
        val context = LocalContext.current
        val api = remember { CommunityApi(context) }
        var loggedIn by remember { mutableStateOf(api.loggedIn()) }
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF050812)) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFF050812), Color(0xFF081525), Color(0xFF050812)))
                )
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
    Row(Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard(Modifier.weight(1f).fillMaxSize()) {
            Text("DLavie", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Modern Mod Hub", fontSize = 18.sp, color = CandyCyan)
            Spacer(Modifier.height(18.dp))
            Text("Login page dibuat sebagai halaman pertama. Setelah login, baru masuk Home, Community, Upgrade Plan, dan Profile.", color = SoftText, fontSize = 15.sp)
            Spacer(Modifier.height(22.dp))
            InfoLine("Username", "Wajib saat register, 3-24 karakter: huruf, angka, underscore.")
            InfoLine("Display name", "Wajib saat register. Ini nama tampil di community.")
            InfoLine("Avatar", "Opsional. Bisa dikosongkan dulu.")
            InfoLine("Community", "Topic, reply, mention @username sudah memakai Supabase DlavieAPP.")
        }
        GlassCard(Modifier.weight(1f).fillMaxSize()) {
            Text("Account", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SoftText)
            Spacer(Modifier.height(10.dp))
            ModernField("Email", email) { email = it }
            ModernField("Password", password, password = true) { password = it }
            ModernField("Username wajib untuk register", username) { username = it }
            ModernField("Display name wajib untuk register", displayName) { displayName = it }
            ModernField("Avatar URL opsional", avatarUrl) { avatarUrl = it }
            Spacer(Modifier.height(8.dp))
            Text(status, color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !loading,
                onClick = {
                    loading = true; status = "Login..."
                    scope.launch {
                        try { withContext(Dispatchers.IO) { api.login(email, password) }; status = "Login berhasil."; onSuccess() }
                        catch (t: Throwable) { status = "Login gagal: ${t.message}. Jika belum punya akun, tekan Register." }
                        finally { loading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CandyBlue)
            ) { Text("Login", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            Button(
                enabled = !loading,
                onClick = {
                    if (!username.matches(Regex("[a-zA-Z0-9_]{3,24}"))) { status = "Username wajib 3-24 karakter: huruf, angka, underscore."; return@Button }
                    if (displayName.trim().length < 2) { status = "Display name wajib minimal 2 karakter."; return@Button }
                    loading = true; status = "Register..."
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { api.register(email, password, username, displayName, avatarUrl) }
                            if (api.loggedIn()) { status = "Register berhasil."; onSuccess() } else status = "Register berhasil. Jika email confirmation aktif, cek email lalu login."
                        } catch (t: Throwable) { status = "Register gagal: ${t.message}" }
                        finally { loading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D))
            ) { Text("Register", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun MainShell(api: CommunityApi, onLogout: () -> Unit) {
    var page by remember { mutableStateOf(Page.Home) }
    Column(Modifier.fillMaxSize()) {
        AnimatedContent(targetState = page, label = "page", modifier = Modifier.weight(1f)) { target ->
            when (target) {
                Page.Home -> HomeScreen()
                Page.Community -> CommunityScreen(api)
                Page.Plan -> PlanScreen()
                Page.Profile -> ProfileScreen(api, onLogout)
            }
        }
        NavigationBar(containerColor = Color(0xEE0E1728), tonalElevation = 0.dp) {
            Page.values().forEach { item ->
                NavigationBarItem(selected = page == item, onClick = { page = item }, icon = { Text(item.icon, fontSize = 18.sp) }, label = { Text(item.label) })
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
            Text("UI baru ini tidak memaksa landscape. Layout mengikuti orientasi device seperti app modern biasa.", color = SoftText)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val launch = context.packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
                    if (launch != null) context.startActivity(launch) else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PACKAGE")))
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B))
            ) { Text("Launch FIFA 16", fontWeight = FontWeight.Bold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SmallGlassStat("Community", "Online", Modifier.weight(1f))
            SmallGlassStat("Backend", "Supabase", Modifier.weight(1f))
            SmallGlassStat("APK", "0.4.0", Modifier.weight(1f))
        }
        GlassCard {
            Text("Patch Engine", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Fitur update patch lama masih dipertahankan di codebase. Tahap berikutnya akan saya port ke Compose agar seluruh launcher konsisten dan tidak membuka UI lama.", color = SoftText)
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
    fun loadTopics() { scope.launch { try { topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }; status = if (topics.isEmpty()) "Belum ada topic." else "${topics.size} topic loaded." } catch (t: Throwable) { status = "Gagal load topic: ${t.message}" } } }
    LaunchedEffect(Unit) { try { categories = withContext(Dispatchers.IO) { jsonCategories(api.categories()) }; selectedCategory = categories.firstOrNull(); topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) }; status = "Community ready." } catch (t: Throwable) { status = "Community error: ${t.message}" } }
    Row(Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.width(210.dp).fillMaxSize()) {
            Text("Channels", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            categories.forEach { c -> OutlinedButton(onClick = { selectedCategory = c; selectedTopic = null; posts = emptyList(); loadTopics() }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, if (selectedCategory?.id == c.id) CandyCyan else GlassStroke)) { Text(c.name) } }
        }
        GlassCard(Modifier.weight(1f).fillMaxSize()) {
            Text("Topics", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(status, color = SoftText, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            ModernField("Judul topic baru", title) { title = it }
            ModernField("Isi topic baru, bisa tag @username", body) { body = it }
            Button(onClick = {
                val cat = selectedCategory ?: return@Button
                if (title.trim().length < 4 || body.trim().isEmpty()) { status = "Judul minimal 4 karakter dan isi wajib diisi."; return@Button }
                scope.launch { try { val newTopic = withContext(Dispatchers.IO) { api.createTopic(cat.id, title, body) }; title = ""; body = ""; topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(cat.id)) }; selectedTopic = topics.firstOrNull { it.id == newTopic.optString("id") }; status = "Topic dibuat." } catch (t: Throwable) { status = "Gagal membuat topic: ${t.message}" } }
            }, colors = ButtonDefaults.buttonColors(containerColor = CandyCyan, contentColor = Color(0xFF00111D)), modifier = Modifier.fillMaxWidth()) { Text("New Topic") }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(topics) { topic -> GlassListItem(topic.title, "${topic.replyCount} replies • ${topic.createdAt.take(10)}", selectedTopic?.id == topic.id) { selectedTopic = topic; scope.launch { try { posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) } } catch (t: Throwable) { status = "Gagal load thread: ${t.message}" } } } } }
        }
        GlassCard(Modifier.weight(1f).fillMaxSize()) {
            Text(selectedTopic?.title ?: "Thread", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(selectedTopic?.body ?: "Pilih topic untuk membaca dan membalas.", color = SoftText)
            Spacer(Modifier.height(10.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(posts) { p -> GlassListItem("user:${p.authorId.take(8)}", p.body, false) {} } }
            ModernField("Reply, bisa tag @username", reply) { reply = it }
            Button(onClick = {
                val topic = selectedTopic ?: return@Button
                if (reply.trim().isEmpty()) return@Button
                scope.launch { try { withContext(Dispatchers.IO) { api.createPost(topic.id, "", reply) }; reply = ""; posts = withContext(Dispatchers.IO) { jsonPosts(api.posts(topic.id)) }; topics = withContext(Dispatchers.IO) { jsonTopics(api.topics(selectedCategory?.id ?: "")) } } catch (t: Throwable) { status = "Gagal reply: ${t.message}" } }
            }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF00150B)), modifier = Modifier.fillMaxWidth()) { Text("Send Reply") }
            Text("Upload file/screenshot: next step setelah Storage bucket aktif.", color = SoftText, fontSize = 12.sp)
        }
    }
}

@Composable
fun PlanScreen() { Column(Modifier.fillMaxSize().padding(18.dp)) { GlassCard { Text("Upgrade Plan", fontSize = 34.sp, fontWeight = FontWeight.Black); Text("Coming Soon", fontSize = 22.sp, color = CandyCyan, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text("Subscription dikosongkan dulu. Tidak ada checkout palsu, benefit palsu, atau tombol pembayaran dummy.", color = SoftText) } } }

@Composable
fun ProfileScreen(api: CommunityApi, onLogout: () -> Unit) {
    var autoCheck by remember { mutableStateOf(false) }
    var autoLaunch by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(70.dp).background(Brush.linearGradient(listOf(CandyCyan, CandyBlue)), CircleShape), contentAlignment = Alignment.Center) { Text("DL", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White) }; Spacer(Modifier.width(14.dp)); Column { Text(api.displayName().ifEmpty { "DLavie User" }, fontSize = 26.sp, fontWeight = FontWeight.Black); Text("@${api.username().ifEmpty { "unknown" }}", color = SoftText) } }; Spacer(Modifier.height(14.dp)); InfoLine("Profile avatar", "Opsional. Avatar upload akan aktif setelah Storage bucket aktif."); InfoLine("Backend", CommunityApi.SUPABASE_URL); InfoLine("Target game", GAME_PACKAGE) }
        GlassCard { Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold); SettingRow("Auto check update", autoCheck) { autoCheck = it }; SettingRow("Auto launch after patch", autoLaunch) { autoLaunch = it }; Text("Theme toggle light/dark akan saya aktifkan penuh di tahap berikutnya setelah port semua patch engine ke Compose.", color = SoftText, fontSize = 13.sp); Spacer(Modifier.height(10.dp)); Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5269)), modifier = Modifier.fillMaxWidth()) { Text("Logout", fontWeight = FontWeight.Bold) } }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable Column.() -> Unit) { Card(modifier = modifier, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xCC101827)), border = BorderStroke(1.dp, GlassStroke)) { Column(modifier = Modifier.padding(18.dp), content = content) } }

@Composable
fun ModernField(label: String, value: String, password: Boolean = false, onChange: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), singleLine = false); Spacer(Modifier.height(8.dp)) }

@Composable
fun InfoLine(title: String, body: String) { Spacer(Modifier.height(10.dp)); Text(title, color = SoftText, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(body, color = Color.White, fontSize = 14.sp) }

@Composable
fun SmallGlassStat(title: String, value: String, modifier: Modifier = Modifier) { GlassCard(modifier = modifier) { Text(title, color = SoftText, fontSize = 12.sp); Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassListItem(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) { Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = if (selected) Color(0x5539D8FF) else Color(0x66172132), border = BorderStroke(1.dp, if (selected) CandyCyan else GlassStroke), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(subtitle, color = SoftText, fontSize = 12.sp) } } }

@Composable
fun SettingRow(title: String, value: Boolean, onChange: (Boolean) -> Unit) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, modifier = Modifier.weight(1f), color = Color.White); Switch(checked = value, onCheckedChange = onChange) } }

fun jsonCategories(arr: JSONArray): List<CategoryItem> = List(arr.length()) { i -> val o = arr.getJSONObject(i); CategoryItem(o.optString("id"), o.optString("name"), o.optString("description")) }
fun jsonTopics(arr: JSONArray): List<TopicItem> = List(arr.length()) { i -> val o = arr.getJSONObject(i); TopicItem(o.optString("id"), o.optString("title"), o.optString("body"), o.optInt("reply_count"), o.optString("created_at")) }
fun jsonPosts(arr: JSONArray): List<PostItem> = List(arr.length()) { i -> val o = arr.getJSONObject(i); PostItem(o.optString("id"), o.optString("author_id"), o.optString("body"), o.optString("created_at")) }

val CandyCyan = Color(0xFF27C8FF)
val CandyBlue = Color(0xFF6C63FF)
val NeonGreen = Color(0xFF1FDD90)
val SoftText = Color(0xFFA8B5CC)
val GlassStroke = Color(0x665D8DFF)
