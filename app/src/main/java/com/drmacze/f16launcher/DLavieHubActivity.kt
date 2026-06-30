package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val GAME_PACKAGE = "com.ea.gp.fifaworld"
private const val DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json"

private enum class HubTab(val title: String, val icon: ImageVector) {
    Feed("Feed", Icons.Rounded.Home),
    Library("Library", Icons.Rounded.Folder),
    Community("Community", Icons.Rounded.ChatBubble),
    Profile("Profile", Icons.Rounded.AccountCircle)
}

private data class ManifestState(
    val loading: Boolean = true,
    val status: String = "Checking manifest...",
    val versionName: String = "-",
    val versionCode: Int = 0,
    val releaseNotes: List<String> = emptyList()
)

class DLavieHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DLavie26HubApp() }
    }
}

@Composable
private fun DLavie26HubApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DlBlack,
            surface = DlCard,
            primary = DlGreen,
            secondary = DlCyan,
            onPrimary = Color(0xFF001407),
            onSecondary = Color(0xFF001018),
            onBackground = DlWhite,
            onSurface = DlWhite
        ),
        typography = DlTypography
    ) {
        var tab by remember { mutableStateOf(HubTab.Feed) }
        Surface(color = DlBlack, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF10251D), Color(0xFF050606), Color(0xFF000000)),
                            radius = 1100f
                        )
                    )
            ) {
                AnimatedContent(
                    targetState = tab,
                    label = "dlavie-hub-tab",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp)
                ) { selected ->
                    when (selected) {
                        HubTab.Feed -> FeedPage(onOpenLibrary = { tab = HubTab.Library }, onOpenCommunity = { tab = HubTab.Community })
                        HubTab.Library -> LibraryPage()
                        HubTab.Community -> CommunityPage()
                        HubTab.Profile -> ProfilePage()
                    }
                }
                BottomHubNavigation(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedPage(onOpenLibrary: () -> Unit, onOpenCommunity: () -> Unit) {
    val context = LocalContext.current
    var manifest by remember { mutableStateOf(ManifestState()) }
    val scope = rememberCoroutineScope()

    fun refreshManifest() {
        manifest = ManifestState(loading = true, status = "Checking manifest...")
        scope.launch {
            manifest = withContext(Dispatchers.IO) { loadManifestState() }
        }
    }

    LaunchedEffect(Unit) { manifest = withContext(Dispatchers.IO) { loadManifestState() } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderBlock()
        QuickActions(
            onPlay = { launchFifa(context) },
            onLibrary = onOpenLibrary,
            onCommunity = onOpenCommunity,
            onRepair = { openAdvancedUpdater(context) }
        )
        ManifestCard(manifest, onRefresh = { refreshManifest() }, onOpenUpdater = { openAdvancedUpdater(context) })
        ProductionPolicyCard()
    }
}

@Composable
private fun HeaderBlock() {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0F2E23), Color(0xFF0A1110)))),
                contentAlignment = Alignment.Center
            ) {
                Text("DL", color = DlGreen, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = DlFont)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("DLavie 26", color = DlWhite, fontSize = 34.sp, fontWeight = FontWeight.Black, fontFamily = DlFont)
                Text("FIFA 16 Mobile 2026 Mod Hub", color = DlMuted, fontSize = 14.sp, fontFamily = DlFont)
            }
            Pill("PROD", DlGreen)
        }
        Spacer(Modifier.height(18.dp))
        Text("Football Reborn", color = DlWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = DlFont)
        Text("Production shell only. Fake posts, fake counts, fake chat, and fake profile data are not shown.", color = DlMuted, fontSize = 14.sp, fontFamily = DlFont)
    }
}

@Composable
private fun QuickActions(onPlay: () -> Unit, onLibrary: () -> Unit, onCommunity: () -> Unit, onRepair: () -> Unit) {
    GlassPanel {
        SectionTitle(Icons.Rounded.PlayArrow, "Quick Actions")
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DlGreen, contentColor = Color(0xFF001407)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(8.dp))
            Text("Play FIFA 16", fontWeight = FontWeight.Black, fontFamily = DlFont)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("Library", Icons.Rounded.Folder, onLibrary, Modifier.weight(1f))
            ActionButton("Repair", Icons.Rounded.Build, onRepair, Modifier.weight(1f))
            ActionButton("Community", Icons.Rounded.ChatBubble, onCommunity, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ManifestCard(state: ManifestState, onRefresh: () -> Unit, onOpenUpdater: () -> Unit) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Refresh, DlCyan)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Update Manifest", color = DlWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = DlFont)
                    Text(DEFAULT_MANIFEST, color = DlMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, fontFamily = DlFont)
                }
            }
            Pill(if (state.loading) "CHECK" else "LIVE", if (state.loading) DlMuted else DlGreen)
        }
        Spacer(Modifier.height(12.dp))
        Text(state.status, color = DlMuted, fontFamily = DlFont)
        if (state.versionName != "-") {
            Spacer(Modifier.height(10.dp))
            InfoLine("Latest", state.versionName)
            InfoLine("Version code", state.versionCode.toString())
        }
        if (state.releaseNotes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Release Notes", color = DlWhite, fontWeight = FontWeight.Bold, fontFamily = DlFont)
            state.releaseNotes.forEach { note -> Text("• $note", color = DlMuted, fontSize = 13.sp, fontFamily = DlFont) }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onRefresh, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DlCyan, contentColor = Color(0xFF001018))) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh", fontWeight = FontWeight.Bold, fontFamily = DlFont)
            }
            Button(onClick = onOpenUpdater, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = DlGreen, contentColor = Color(0xFF001407))) {
                Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Updater", fontWeight = FontWeight.Bold, fontFamily = DlFont)
            }
        }
    }
}

@Composable
private fun ProductionPolicyCard() {
    GlassPanel {
        SectionTitle(Icons.Rounded.Info, "Production Data Policy")
        Spacer(Modifier.height(8.dp))
        Text("Feed posts, likes, comments, saves, chat rooms, and profiles will appear only after a real backend is connected. Until then, this launcher shows only real local/game/manifest states.", color = DlMuted, fontFamily = DlFont)
    }
}

@Composable
private fun LibraryPage() {
    val context = LocalContext.current
    val installed = remember { isPackageInstalled(context, GAME_PACKAGE) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Library", color = DlWhite, fontSize = 42.sp, fontWeight = FontWeight.Black, fontFamily = DlFont)
        Text("Real install and update actions. No fake validation state.", color = DlMuted, fontFamily = DlFont)
        LibraryStatusHero(context, installed)
        LibraryItem("Game package", GAME_PACKAGE, if (installed) "INSTALLED" else "MISSING", if (installed) DlGreen else DlRed, if (installed) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline) { launchFifa(context) }
        LibraryItem("Update / Repair Center", "Open Advanced Shizuku/root updater for real patch and repair operations.", "OPEN", DlGreen, Icons.Rounded.Build) { openAdvancedUpdater(context) }
        LibraryItem("Manifest source", DEFAULT_MANIFEST, "REMOTE", DlCyan, Icons.Rounded.Refresh) { openAdvancedUpdater(context) }
        LibraryItem("File verification", "Full SHA validation must be executed by the updater/installer flow, not guessed by this screen.", "REAL ONLY", DlMuted, Icons.Rounded.Settings) { openAdvancedUpdater(context) }
    }
}

@Composable
private fun LibraryStatusHero(context: Context, installed: Boolean) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IconTile(if (installed) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, if (installed) DlGreen else DlRed)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("DLavie 26 Status", color = DlWhite, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = DlFont)
                    Text(if (installed) "FIFA package detected." else "FIFA package is not detected on this device.", color = DlMuted, fontFamily = DlFont)
                }
            }
            Pill(if (installed) "READY" else "INSTALL", if (installed) DlGreen else DlRed)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatChip("Package", if (installed) "Found" else "Missing", Modifier.weight(1f))
            StatChip("Channel", "Stable", Modifier.weight(1f))
            StatChip("Access", "Updater", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { openAdvancedUpdater(context) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DlCyan, contentColor = Color(0xFF001018)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open Real Update / Repair Center", fontWeight = FontWeight.Bold, fontFamily = DlFont)
        }
    }
}

@Composable
private fun CommunityPage() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Community", color = DlWhite, fontSize = 42.sp, fontWeight = FontWeight.Black, fontFamily = DlFont)
        Text("Chat will be enabled only after Supabase/Firebase backend is connected.", color = DlMuted, fontFamily = DlFont)
        GlassPanel {
            SectionTitle(Icons.Rounded.ChatBubble, "No backend connected")
            Spacer(Modifier.height(8.dp))
            Text("This page does not show fake messages, fake online users, fake rooms, or fake activity. Real global chat requires authentication, database, moderation, reports, and anti-spam rules.", color = DlMuted, fontFamily = DlFont)
        }
        GlassPanel {
            SectionTitle(Icons.Rounded.Settings, "Required backend features")
            InfoLine("Auth", "Real user accounts, username, avatar, role, bans.")
            InfoLine("Realtime", "Real community_messages table and realtime listener.")
            InfoLine("Moderation", "Report, delete, mute, ban, audit log.")
        }
    }
}

@Composable
private fun ProfilePage() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1A1D1C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = DlGreen, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Not signed in", color = DlWhite, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = DlFont)
                Text("Account backend is not connected yet.", color = DlMuted, fontSize = 16.sp, fontFamily = DlFont)
                Spacer(Modifier.height(8.dp))
                Pill("LOCAL", DlMuted)
            }
        }
        GlassPanel {
            SectionTitle(Icons.Rounded.AccountCircle, "Account Settings")
            Spacer(Modifier.height(8.dp))
            Text("Profile, username, avatar, saved posts, comments, and notification settings will be enabled after real authentication is connected. This screen does not display fake user identity.", color = DlMuted, fontFamily = DlFont)
        }
        GlassPanel {
            SectionTitle(Icons.Rounded.Settings, "Developer Console")
            Spacer(Modifier.height(8.dp))
            Text("Developer/admin tools are not exposed in this public launcher. Maintenance mode, push notification, publishing, banning, and moderation belong in private DLavie Console with backend role checks.", color = DlMuted, fontFamily = DlFont)
        }
    }
}

@Composable
private fun LibraryItem(title: String, subtitle: String, status: String, statusColor: Color, icon: ImageVector, onClick: () -> Unit) {
    GlassPanel(modifier = Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, statusColor)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = DlWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = DlFont)
                Text(subtitle, color = DlMuted, maxLines = 3, overflow = TextOverflow.Ellipsis, fontFamily = DlFont)
            }
            Pill(status, statusColor)
        }
    }
}

@Composable
private fun BottomHubNavigation(selected: HubTab, onSelect: (HubTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 680.dp).padding(horizontal = 16.dp),
        color = Color(0xE60B0C0C),
        shape = RoundedCornerShape(34.dp),
        border = BorderStroke(1.dp, Color(0xFF252A2C)),
        shadowElevation = 18.dp
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            HubTab.values().forEach { item ->
                val active = selected == item
                Button(
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f).height(if (active) 58.dp else 50.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF0E3A22) else Color.Transparent, contentColor = if (active) DlGreen else DlMuted),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (active) 8.dp else 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(if (active) 22.dp else 19.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(item.title, fontSize = if (active) 11.sp else 10.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Medium, maxLines = 1, fontFamily = DlFont)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC101111)),
        border = BorderStroke(1.dp, Color(0xFF252A2C))
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF24302A))) {
        Icon(icon, contentDescription = null, tint = DlGreen, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = DlGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = DlFont, maxLines = 1)
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF071F1E)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = DlGreen, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = DlWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = DlFont)
    }
}

@Composable
private fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = color.copy(alpha = 0.16f), border = BorderStroke(1.dp, color.copy(alpha = 0.55f)), shape = RoundedCornerShape(999.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1, fontFamily = DlFont)
    }
}

@Composable
private fun StatChip(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0xFF0C1110), border = BorderStroke(1.dp, Color(0xFF202724)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = DlMuted, fontSize = 11.sp, fontFamily = DlFont)
            Text(value, color = DlWhite, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = DlFont)
        }
    }
}

@Composable
private fun InfoLine(title: String, body: String) {
    Spacer(Modifier.height(8.dp))
    Text(title, color = DlMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = DlFont)
    Text(body, color = DlWhite, fontSize = 14.sp, fontFamily = DlFont)
}

private fun loadManifestState(): ManifestState {
    return try {
        val json = fetchJson(DEFAULT_MANIFEST)
        val notes = json.optJSONArray("releaseNotes")
        ManifestState(
            loading = false,
            status = json.optJSONObject("status")?.optString("message") ?: "Manifest loaded from GitHub.",
            versionName = json.optString("latestVersionName", "-"),
            versionCode = json.optInt("latestVersionCode", 0),
            releaseNotes = if (notes != null) List(notes.length()) { i -> notes.optString(i) } else emptyList()
        )
    } catch (t: Throwable) {
        ManifestState(loading = false, status = "Manifest check failed: ${t.message}")
    }
}

private fun fetchJson(url: String): JSONObject {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 20000
    c.readTimeout = 30000
    return try {
        BufferedReader(InputStreamReader(c.inputStream)).use { JSONObject(it.readText()) }
    } finally {
        c.disconnect()
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

private fun launchFifa(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GAME_PACKAGE")))
    }
}

private fun openAdvancedUpdater(context: Context) {
    context.startActivity(Intent(context, GameHubActivity::class.java))
}

private val DlFont = FontFamily.SansSerif

private val DlTypography = Typography(
    displayLarge = TextStyle(fontFamily = DlFont, fontWeight = FontWeight.Black, fontSize = 42.sp),
    titleLarge = TextStyle(fontFamily = DlFont, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = DlFont, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = DlFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = DlFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
)

private val DlBlack = Color(0xFF050606)
private val DlCard = Color(0xFF101111)
private val DlWhite = Color(0xFFF7F7F7)
private val DlMuted = Color(0xFF7A7F83)
private val DlGreen = Color(0xFF20E070)
private val DlCyan = Color(0xFF28D7FF)
private val DlRed = Color(0xFFFF4D4D)
