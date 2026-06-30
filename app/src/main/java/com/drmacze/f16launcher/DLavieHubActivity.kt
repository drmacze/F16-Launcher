package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val GAME_PACKAGE = "com.ea.gp.fifaworld"
private const val DEFAULT_MANIFEST = "https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json"

private enum class HubTab(val title: String, val icon: String) {
    Feed("Feed", "⌂"),
    Library("Library", "⇩"),
    Community("Community", "◉"),
    Profile("Profile", "g")
}

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
        )
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
                        HubTab.Feed -> FeedPage(onOpenUpdate = { tab = HubTab.Library }, onOpenCommunity = { tab = HubTab.Community })
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
private fun FeedPage(onOpenUpdate: () -> Unit, onOpenCommunity: () -> Unit) {
    val context = LocalContext.current
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
            onUpdate = onOpenUpdate,
            onCommunity = onOpenCommunity,
            onRepair = { openAdvancedUpdater(context) }
        )
        OfficialUpdateCard(
            title = "DLavie 26 Global Hub",
            subtitle = "Developer Announcement",
            body = "DLavie 26 sedang diarahkan menjadi FIFA 16 Mobile 2026 Mod Hub: install, repair, update, community, profile, dan news dalam satu launcher.",
            action = "Open Library",
            onAction = onOpenUpdate,
            stats = "♡ 128   ◌ 32   ↗ Share   ☆ Save"
        )
        OfficialUpdateCard(
            title = "Gameplay Realism Patch v3",
            subtitle = "Update Preview · Stable Channel",
            body = "Rencana patch berikutnya: AI lebih realistis, tempo match lebih natural, attribdb/gameplay tuning, dan safety check cl.ini.",
            action = "Check Update",
            onAction = { openAdvancedUpdater(context) },
            stats = "♡ 84   ◌ 15   ↗ Share   ☆ Save"
        )
        CommunityPreviewCard(onOpenCommunity)
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
                Text("DL", color = DlGreen, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("DLavie 26", color = DlWhite, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text("FIFA 16 Mobile 2026 Mod Hub", color = DlMuted, fontSize = 14.sp)
            }
            Pill("ONLINE", DlGreen)
        }
        Spacer(Modifier.height(18.dp))
        Text("Football Reborn", color = DlWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Install, update, repair, community, and profile in one premium launcher.", color = DlMuted, fontSize = 14.sp)
    }
}

@Composable
private fun QuickActions(onPlay: () -> Unit, onUpdate: () -> Unit, onCommunity: () -> Unit, onRepair: () -> Unit) {
    GlassPanel {
        Text("Quick Actions", color = DlWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DlGreen, contentColor = Color(0xFF001407)),
            shape = RoundedCornerShape(20.dp)
        ) { Text("Play FIFA 16", fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("Update", onUpdate, Modifier.weight(1f))
            ActionButton("Repair", onRepair, Modifier.weight(1f))
            ActionButton("Chat", onCommunity, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LibraryPage() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Library", color = DlWhite, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Text("Game files, updater, repair tools, and saved patches.", color = DlMuted)
        LibraryStatusHero(context)
        LibraryItem("DLavie 26 Base Data", "1.45 GB · Installed via DLavie installer", "VALID", DlGreen) { openAdvancedUpdater(context) }
        LibraryItem("Main OBB", "main.13.com.ea.gp.fifaworld.obb", "CHECK", DlCyan) { openAdvancedUpdater(context) }
        LibraryItem("Patch OBB", "patch.26.com.ea.gp.fifaworld.obb", "CHECK", DlCyan) { openAdvancedUpdater(context) }
        LibraryItem("Advanced Shizuku Updater", "Apply small mod updates without downloading full data again", "OPEN", DlGreen) { openAdvancedUpdater(context) }
        LibraryItem("Manifest", DEFAULT_MANIFEST, "STABLE", DlMuted) { openAdvancedUpdater(context) }
    }
}

@Composable
private fun LibraryStatusHero(context: Context) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DLavie 26 Status", color = DlWhite, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Ready for install, repair, and update flow.", color = DlMuted)
            }
            Pill("v0.10", DlGreen)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatChip("Data", "v26", Modifier.weight(1f))
            StatChip("Channel", "Stable", Modifier.weight(1f))
            StatChip("Access", "Shizuku", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { openAdvancedUpdater(context) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DlCyan, contentColor = Color(0xFF001018)),
            shape = RoundedCornerShape(20.dp)
        ) { Text("Open Update / Repair Center", fontWeight = FontWeight.Bold) }
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
        Text("Community", color = DlWhite, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Text("Realtime chat direction for the global DLavie 26 community.", color = DlMuted)
        CommunityRoom("Global Chat", "Discuss DLavie 26 with global players.", "1.2k online")
        CommunityRoom("Indonesia Chat", "Ruang komunitas Indonesia.", "428 online")
        CommunityRoom("Bug Report", "Report crash, data error, gameplay bug, or update failed.", "Support")
        CommunityRoom("Mod Request", "Request kits, database, career mode, faces, or UI features.", "Open")
        CommunityRoom("Gameplay Discussion", "Balancing, realism, tempo, AI, attribdb, cl.ini.", "Live")
        GlassPanel {
            Text("Next Backend Step", color = DlWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Supabase schema sudah disiapkan untuk profile, feed, likes, comments, saves, reports, and community messages.", color = DlMuted)
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
                    .background(Color(0xFF485B63)),
                contentAlignment = Alignment.Center
            ) {
                Text("g", color = DlWhite, fontSize = 46.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("gibran al bukhary", color = DlWhite, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("@gibran_al_bukhary", color = DlMuted, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Founder", DlGreen)
                    Pill("Developer", DlCyan)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileStat("0", "Posts", Modifier.weight(1f))
            ProfileStat("0", "Followers", Modifier.weight(1f))
            ProfileStat("0", "Saved", Modifier.weight(1f))
        }
        GlassPanel {
            Text("Account Settings", color = DlWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            SettingLine("Edit Profile", "Username, display name, avatar, country")
            SettingLine("Notifications", "Update alerts, replies, mentions")
            SettingLine("Privacy", "Online status, country visibility, mentions")
            SettingLine("Saved", "Saved updates, tutorials, comments")
        }
        GlassPanel {
            Text("Developer Console", color = DlWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Developer/admin tools will be moved to a private DLavie Console APK. Public users will not see maintenance, push notification, ban, or publish controls.", color = DlMuted)
        }
    }
}

@Composable
private fun OfficialUpdateCard(title: String, subtitle: String, body: String, action: String, onAction: () -> Unit, stats: String) {
    GlassPanel {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2C0745), Color(0xFF0A1A14), Color(0xFF141414))))
        ) {
            Text("DLavie 26", color = Color.White.copy(alpha = 0.85f), fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
            Pill("OFFICIAL", DlGreen, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(DlGreen), contentAlignment = Alignment.Center) { Text("DL", color = Color(0xFF001407), fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("DLavie Official  ✓", color = DlWhite, fontWeight = FontWeight.Black)
                Text(subtitle, color = DlMuted, fontSize = 12.sp)
            }
            Pill("Public", DlMuted)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = DlWhite, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(body, color = DlMuted, fontSize = 15.sp)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = DlGreen, contentColor = Color(0xFF001407)), shape = RoundedCornerShape(18.dp)) { Text(action, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        Text(stats, color = DlMuted, fontSize = 13.sp)
    }
}

@Composable
private fun CommunityPreviewCard(onClick: () -> Unit) {
    GlassPanel(modifier = Modifier.clickable { onClick() }) {
        Text("Community Rooms", color = DlWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Global Chat · Bug Report · Mod Request · Gameplay Discussion", color = DlMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Global", DlGreen)
            Pill("Bug Report", DlCyan)
            Pill("Mod Request", DlMuted)
        }
    }
}

@Composable
private fun LibraryItem(title: String, subtitle: String, status: String, statusColor: Color, onClick: () -> Unit) {
    GlassPanel(modifier = Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF071F1E)), contentAlignment = Alignment.Center) {
                Text("▧", color = DlCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = DlWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = DlMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Pill(status, statusColor)
        }
    }
}

@Composable
private fun CommunityRoom(title: String, body: String, meta: String) {
    GlassPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF08231A)), contentAlignment = Alignment.Center) { Text("#", color = DlGreen, fontSize = 24.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = DlWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(body, color = DlMuted)
            }
            Pill(meta, DlGreen)
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
                        Text(item.icon, fontSize = if (active) 20.sp else 16.sp, fontWeight = FontWeight.Bold)
                        Text(item.title, fontSize = if (active) 11.sp else 10.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Normal, maxLines = 1)
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
private fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF24302A))) {
        Text(label, color = DlGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = color.copy(alpha = 0.16f), border = BorderStroke(1.dp, color.copy(alpha = 0.55f)), shape = RoundedCornerShape(999.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1)
    }
}

@Composable
private fun StatChip(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color(0xFF0C1110), border = BorderStroke(1.dp, Color(0xFF202724)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = DlMuted, fontSize = 11.sp)
            Text(value, color = DlWhite, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    GlassPanel(modifier) {
        Text(value, color = DlWhite, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(label, color = DlMuted, fontSize = 13.sp)
    }
}

@Composable
private fun SettingLine(title: String, body: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, color = DlWhite, fontWeight = FontWeight.Bold)
    Text(body, color = DlMuted, fontSize = 13.sp)
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

private fun shareDLavie(context: Context) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "DLavie 26 - FIFA 16 Mobile 2026 Mod Hub")
    }
    context.startActivity(Intent.createChooser(share, "Share DLavie 26"))
}

private val DlBlack = Color(0xFF050606)
private val DlCard = Color(0xFF101111)
private val DlWhite = Color(0xFFF7F7F7)
private val DlMuted = Color(0xFF7A7F83)
private val DlGreen = Color(0xFF20E070)
private val DlCyan = Color(0xFF28D7FF)
