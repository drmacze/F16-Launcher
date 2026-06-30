package com.drmacze.f16launcher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private const val HUB_GAME_PACKAGE = "com.ea.gp.fifaworld"
private const val HUB_PREFS = "dlavie_update_state"
private const val HUB_PREF_CODE = "installed_update_code"
private const val HUB_PREF_NAME = "installed_update_name"
private const val HUB_DEFAULT_CODE = 1
private const val HUB_DEFAULT_NAME = "v1"
private const val HUB_SHIZUKU_REQUEST_CODE = 2026

private enum class HubTab(val label: String, val icon: HubMark) {
    Home("Home", HubMark.Home),
    Data("Data", HubMark.Folder),
    Chat("Chat", HubMark.Chat),
    Me("Me", HubMark.User)
}

private enum class HubMark { Home, Folder, Chat, User, Play, Shield, Check, Alert }

private data class HubUpdateState(
    val checking: Boolean = false,
    val localCode: Int = HUB_DEFAULT_CODE,
    val localName: String = HUB_DEFAULT_NAME,
    val latestCode: Int = 0,
    val latestName: String = "Not checked",
    val dataMarker: String = "Unknown",
    val shizukuState: String = "Unknown",
    val badge: String = "CHECK",
    val message: String = "Tap Check Update.",
    val canInstall: Boolean = false,
    val canRequestPermission: Boolean = false,
    val showGuide: Boolean = false
)

class DLavieHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DLavieHubApp() }
    }
}

@Composable
private fun DLavieHubApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = HubDark,
            surface = HubCard,
            primary = HubGreen,
            secondary = HubCyan,
            onPrimary = Color(0xFF001407),
            onSecondary = Color(0xFF001018),
            onBackground = HubWhite,
            onSurface = HubWhite
        )
    ) {
        var tab by remember { mutableStateOf(HubTab.Home) }
        Surface(color = HubDark, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF0B2419), HubDark, Color.Black),
                            radius = 900f
                        )
                    )
            ) {
                Box(Modifier.fillMaxSize().padding(bottom = 104.dp)) {
                    when (tab) {
                        HubTab.Home -> HubHomeScreen(openData = { tab = HubTab.Data }, openChat = { tab = HubTab.Chat })
                        HubTab.Data -> HubDataScreen()
                        HubTab.Chat -> HubComingSoonScreen("Chat", "Community real akan aktif setelah akun dan moderasi siap.", HubMark.Chat)
                        HubTab.Me -> HubComingSoonScreen("Profile", "Login, avatar, saved post, dan notifikasi akan hadir setelah backend aktif.", HubMark.User)
                    }
                }
                HubBottomNav(tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun HubHomeScreen(openData: () -> Unit, openChat: () -> Unit) {
    val context = LocalContext.current
    val installed = remember { hubIsGameInstalled(context) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubHeroCard()
        HubPrimaryActions(
            onPlay = { hubLaunchGame(context) },
            openData = openData,
            openChat = openChat
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            HubMiniStatus("Game", if (installed) "Ready" else "Missing", if (installed) HubMark.Check else HubMark.Alert, if (installed) HubGreen else HubRed, Modifier.weight(1f))
            HubMiniStatus("Update", "Check", HubMark.Shield, HubCyan, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HubHeroCard() {
    HubPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF0E3A22), Color(0xFF08100D))), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("DL", color = HubGreen, fontSize = 25.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("DLavie 26", color = HubWhite, fontSize = 31.sp, fontWeight = FontWeight.Black, fontFamily = HubFont, maxLines = 1)
                Text("FIFA 16 Mobile 2026", color = HubMuted, fontSize = 14.sp, fontFamily = HubFont, maxLines = 1)
            }
            HubPill("PROD", HubGreen)
        }
        Spacer(Modifier.height(18.dp))
        Text("Football Reborn", color = HubWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = HubFont, maxLines = 1)
        Text("Play, update, and connect with DLavie.", color = HubMuted, fontSize = 14.sp, fontFamily = HubFont, maxLines = 2)
    }
}

@Composable
private fun HubPrimaryActions(onPlay: () -> Unit, openData: () -> Unit, openChat: () -> Unit) {
    HubPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HubIconMark(HubMark.Play, HubGreen, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Actions", color = HubWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HubGreen, contentColor = Color(0xFF001407)),
            contentPadding = PaddingValues(0.dp)
        ) {
            HubIconMark(HubMark.Play, Color(0xFF001407), Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HubCompactButton("Data", openData, Modifier.weight(1f))
            HubCompactButton("Update", openData, Modifier.weight(1f))
            HubCompactButton("Chat", openChat, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HubDataScreen() {
    val context = LocalContext.current
    val installed = remember { hubIsGameInstalled(context) }
    var updateState by remember { mutableStateOf(hubInitialUpdateState(context)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        updateState = withContext(Dispatchers.IO) { hubCheckUpdate(context) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubPageTitle("Data", "Update, Shizuku, dan status game.")
        HubStatusCard(installed = installed, onPlay = { hubLaunchGame(context) })
        HubUpdateCard(
            state = updateState,
            onCheck = {
                scope.launch {
                    updateState = updateState.copy(checking = true, message = "Checking update...")
                    updateState = withContext(Dispatchers.IO) { hubCheckUpdate(context) }
                }
            },
            onAllowShizuku = {
                scope.launch {
                    hubRequestShizukuPermission()
                    delay(900)
                    updateState = withContext(Dispatchers.IO) { hubCheckUpdate(context) }
                }
            },
            onInstall = {
                scope.launch {
                    updateState = updateState.copy(checking = true, message = "Installing update...")
                    updateState = withContext(Dispatchers.IO) { hubInstallInlinePatch(context) }
                }
            }
        )
        HubDataRow("FIFA 16 Mobile", if (installed) "Installed" else "Not found", if (installed) "READY" else "MISSING", if (installed) HubGreen else HubRed, if (installed) HubMark.Check else HubMark.Alert)
    }
}

@Composable
private fun HubUpdateCard(
    state: HubUpdateState,
    onCheck: () -> Unit,
    onAllowShizuku: () -> Unit,
    onInstall: () -> Unit
) {
    val badgeColor = when (state.badge) {
        "UPDATE" -> HubCyan
        "LATEST" -> HubGreen
        "INSTALLED" -> HubGreen
        "OFFLINE" -> HubRed
        "FAILED" -> HubRed
        "SHIZUKU" -> HubRed
        else -> HubCyan
    }

    HubPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HubIconTile(HubMark.Shield, badgeColor)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Update Center", color = HubWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = HubFont, maxLines = 1)
                Text(state.message, color = HubMuted, fontSize = 14.sp, fontFamily = HubFont, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            HubPill(if (state.checking) "WAIT" else state.badge, badgeColor)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HubInfoBox("Local", state.localName, Modifier.weight(1f))
            HubInfoBox("Latest", state.latestName, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HubInfoBox("Shizuku", state.shizukuState, Modifier.weight(1f))
            HubInfoBox("Data", state.dataMarker, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onCheck,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HubCyan, contentColor = Color(0xFF001018)),
            contentPadding = PaddingValues(0.dp),
            enabled = !state.checking
        ) {
            Text("Check Update", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
        }
        if (state.canRequestPermission) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onAllowShizuku,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HubGreen, contentColor = Color(0xFF001407)),
                contentPadding = PaddingValues(0.dp),
                enabled = !state.checking
            ) {
                Text("Allow Shizuku", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
            }
        }
        if (state.canInstall) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HubGreen, contentColor = Color(0xFF001407)),
                contentPadding = PaddingValues(0.dp),
                enabled = !state.checking
            ) {
                Text("Install Update", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
            }
        }
        if (state.showGuide) {
            Spacer(Modifier.height(12.dp))
            HubGuideCard()
        }
    }
}

@Composable
private fun HubGuideCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0B0F0E),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, HubBorder)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Shizuku Setup", color = HubWhite, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
            Spacer(Modifier.height(6.dp))
            Text("1. Buka aplikasi Shizuku dan start service.", color = HubMuted, fontSize = 13.sp, fontFamily = HubFont)
            Text("2. Kembali ke DLavie lalu tap Allow Shizuku.", color = HubMuted, fontSize = 13.sp, fontFamily = HubFont)
            Text("3. Setelah status Ready, tap Install Update.", color = HubMuted, fontSize = 13.sp, fontFamily = HubFont)
        }
    }
}

@Composable
private fun HubInfoBox(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0B0F0E),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, HubBorder)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = HubMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = HubFont, maxLines = 1)
            Text(value, color = HubWhite, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = HubFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HubPageTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = HubWhite, fontSize = 38.sp, fontWeight = FontWeight.Black, fontFamily = HubFont, maxLines = 1)
        Text(subtitle, color = HubMuted, fontSize = 14.sp, fontFamily = HubFont, maxLines = 1)
    }
}

@Composable
private fun HubStatusCard(installed: Boolean, onPlay: () -> Unit) {
    HubPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HubIconTile(if (installed) HubMark.Check else HubMark.Alert, if (installed) HubGreen else HubRed)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("DLavie 26", color = HubWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = HubFont, maxLines = 1)
                Text(if (installed) "Ready to play" else "Game not found", color = HubMuted, fontSize = 15.sp, fontFamily = HubFont, maxLines = 1)
            }
            HubPill(if (installed) "READY" else "INSTALL", if (installed) HubGreen else HubRed)
        }
        if (installed) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HubGreen, contentColor = Color(0xFF001407)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Launch Game", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
            }
        }
    }
}

@Composable
private fun HubComingSoonScreen(title: String, subtitle: String, icon: HubMark) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HubPageTitle(title, subtitle)
        HubPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HubIconTile(icon, HubGreen)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Coming Soon", color = HubWhite, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = HubFont)
                    Text("No dummy content.", color = HubMuted, fontSize = 14.sp, fontFamily = HubFont, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HubMiniStatus(title: String, value: String, icon: HubMark, color: Color, modifier: Modifier = Modifier) {
    HubPanel(modifier = modifier) {
        HubIconMark(icon, color, Modifier.size(24.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = HubMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = HubFont, maxLines = 1)
        Text(value, color = HubWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = HubFont, maxLines = 1)
    }
}

@Composable
private fun HubDataRow(title: String, subtitle: String, status: String, statusColor: Color, icon: HubMark) {
    HubPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HubIconTile(icon, statusColor)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = HubWhite, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = HubFont)
                Text(subtitle, color = HubMuted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = HubFont)
            }
            HubPill(status, statusColor)
        }
    }
}

@Composable
private fun HubBottomNav(selected: HubTab, onSelect: (HubTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 680.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        color = Color(0xF00B0C0C),
        shape = RoundedCornerShape(34.dp),
        border = BorderStroke(1.dp, HubBorder),
        shadowElevation = 18.dp
    ) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            HubTab.values().forEach { item ->
                val active = selected == item
                Button(
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) Color(0xFF0E3A22) else Color.Transparent,
                        contentColor = if (active) HubGreen else HubMuted
                    ),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (active) 7.dp else 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HubIconMark(item.icon, if (active) HubGreen else HubMuted, Modifier.size(if (active) 22.dp else 19.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(item.label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Bold, maxLines = 1, fontFamily = HubFont)
                    }
                }
            }
        }
    }
}

@Composable
private fun HubPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD101111)),
        border = BorderStroke(1.dp, HubBorder)
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun HubCompactButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101814), contentColor = HubGreen),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = HubFont, maxLines = 1)
    }
}

@Composable
private fun HubIconTile(icon: HubMark, tint: Color) {
    Box(
        Modifier.size(54.dp).background(Color(0xFF071F1E), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        HubIconMark(icon, tint, Modifier.size(26.dp))
    }
}

@Composable
private fun HubPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1, fontFamily = HubFont)
    }
}

@Composable
private fun HubIconMark(type: HubMark, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.09f
        fun p(x: Float, y: Float) = Offset(s * x, s * y)
        when (type) {
            HubMark.Home -> {
                drawLine(tint, p(0.16f, 0.50f), p(0.50f, 0.20f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.50f, 0.20f), p(0.84f, 0.50f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.28f, 0.46f), p(0.28f, 0.82f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.72f, 0.46f), p(0.72f, 0.82f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.28f, 0.82f), p(0.72f, 0.82f), w, cap = StrokeCap.Round)
            }
            HubMark.Folder -> {
                drawLine(tint, p(0.18f, 0.36f), p(0.42f, 0.36f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.42f, 0.36f), p(0.52f, 0.46f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.18f, 0.46f), p(0.82f, 0.46f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.18f, 0.46f), p(0.18f, 0.78f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.82f, 0.46f), p(0.82f, 0.78f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.18f, 0.78f), p(0.82f, 0.78f), w, cap = StrokeCap.Round)
            }
            HubMark.Chat -> {
                drawLine(tint, p(0.20f, 0.28f), p(0.80f, 0.28f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.20f, 0.28f), p(0.20f, 0.65f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.80f, 0.28f), p(0.80f, 0.65f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.20f, 0.65f), p(0.42f, 0.65f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.42f, 0.65f), p(0.30f, 0.82f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.42f, 0.65f), p(0.80f, 0.65f), w, cap = StrokeCap.Round)
            }
            HubMark.User -> {
                drawCircle(tint, s * 0.15f, p(0.50f, 0.34f), style = Stroke(w))
                drawArc(tint, 200f, 140f, false, p(0.25f, 0.52f), Size(s * 0.50f, s * 0.42f), style = Stroke(w, cap = StrokeCap.Round))
            }
            HubMark.Play -> {
                val path = Path().apply { moveTo(s * 0.34f, s * 0.22f); lineTo(s * 0.34f, s * 0.78f); lineTo(s * 0.78f, s * 0.50f); close() }
                drawPath(path, tint)
            }
            HubMark.Shield -> {
                drawCircle(tint, s * 0.32f, p(0.50f, 0.50f), style = Stroke(w))
                drawLine(tint, p(0.50f, 0.26f), p(0.50f, 0.74f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.26f, 0.50f), p(0.74f, 0.50f), w, cap = StrokeCap.Round)
            }
            HubMark.Check -> {
                drawCircle(tint, s * 0.34f, p(0.50f, 0.50f), style = Stroke(w))
                drawLine(tint, p(0.34f, 0.52f), p(0.45f, 0.64f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.45f, 0.64f), p(0.70f, 0.38f), w, cap = StrokeCap.Round)
            }
            HubMark.Alert -> {
                drawCircle(tint, s * 0.34f, p(0.50f, 0.50f), style = Stroke(w))
                drawLine(tint, p(0.50f, 0.28f), p(0.50f, 0.58f), w, cap = StrokeCap.Round)
                drawCircle(tint, s * 0.025f, p(0.50f, 0.72f))
            }
        }
    }
}

private fun hubInitialUpdateState(context: Context): HubUpdateState {
    val prefs = context.getSharedPreferences(HUB_PREFS, Context.MODE_PRIVATE)
    return HubUpdateState(
        localCode = prefs.getInt(HUB_PREF_CODE, HUB_DEFAULT_CODE),
        localName = prefs.getString(HUB_PREF_NAME, HUB_DEFAULT_NAME) ?: HUB_DEFAULT_NAME,
        shizukuState = hubShizukuState(),
        dataMarker = hubReadDataMarker()
    )
}

private fun hubCheckUpdate(context: Context): HubUpdateState {
    val prefs = context.getSharedPreferences(HUB_PREFS, Context.MODE_PRIVATE)
    val localCode = prefs.getInt(HUB_PREF_CODE, HUB_DEFAULT_CODE)
    val localName = prefs.getString(HUB_PREF_NAME, HUB_DEFAULT_NAME) ?: HUB_DEFAULT_NAME
    val shizuku = hubShizukuState()
    val marker = hubReadDataMarker()
    return try {
        val json = hubFetchManifestJson()
        val latestCode = json.optInt("latestVersionCode", 0)
        val latestName = json.optString("latestVersionName", "Unknown")
        val hasPatch = hubFindInlinePatch(json, localCode, latestCode) != null
        val needsUpdate = latestCode > localCode
        HubUpdateState(
            checking = false,
            localCode = localCode,
            localName = localName,
            latestCode = latestCode,
            latestName = latestName,
            dataMarker = marker,
            shizukuState = shizuku,
            badge = when {
                needsUpdate -> "UPDATE"
                latestCode > 0 -> "LATEST"
                else -> "CHECK"
            },
            message = when {
                needsUpdate && shizuku == "Ready" && hasPatch -> "Update tersedia. Siap install otomatis."
                needsUpdate && shizuku == "Permission" -> "Update tersedia. Izinkan Shizuku dulu."
                needsUpdate && (shizuku == "Inactive" || shizuku == "Missing") -> "Update tersedia. Aktifkan Shizuku untuk install otomatis."
                needsUpdate && !hasPatch -> "Update tersedia, tapi patch tidak cocok dengan versi lokal."
                latestCode > 0 -> "Versi kamu sudah terbaru."
                else -> "Channel aktif."
            },
            canInstall = needsUpdate && hasPatch && shizuku == "Ready",
            canRequestPermission = needsUpdate && hasPatch && shizuku == "Permission",
            showGuide = needsUpdate && shizuku != "Ready"
        )
    } catch (_: Throwable) {
        HubUpdateState(
            checking = false,
            localCode = localCode,
            localName = localName,
            latestCode = 0,
            latestName = "Offline",
            dataMarker = marker,
            shizukuState = shizuku,
            badge = "OFFLINE",
            message = "Gagal cek update. Periksa koneksi internet.",
            showGuide = false
        )
    }
}

private fun hubInstallInlinePatch(context: Context): HubUpdateState {
    val checked = hubCheckUpdate(context)
    if (!checked.canInstall) return checked
    return try {
        val json = hubFetchManifestJson()
        val patch = hubFindInlinePatch(json, checked.localCode, checked.latestCode)
            ?: return checked.copy(message = "Patch tidak cocok untuk versi lokal.", canInstall = false)
        val files = patch.optJSONArray("files") ?: return checked.copy(message = "Patch kosong.", canInstall = false)
        val target = patch.optString("target", "/sdcard/Android/data/$HUB_GAME_PACKAGE/").ifBlank { "/sdcard/Android/data/$HUB_GAME_PACKAGE/" }
        val script = StringBuilder()
        script.append("mkdir -p ").append(hubShellQuote(target)).append("\n")

        for (i in 0 until files.length()) {
            val obj = files.optJSONObject(i) ?: continue
            val relative = obj.optString("path", "").trim()
            val content = obj.optString("content", "")
            if (!hubSafeRelativePath(relative)) continue
            val out = target.trimEnd('/') + "/" + relative
            val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            script.append("mkdir -p $(dirname ").append(hubShellQuote(out)).append(")\n")
            script.append("printf %s ").append(hubShellQuote(encoded))
                .append(" | base64 -d > ").append(hubShellQuote(out)).append("\n")
        }

        val process = hubStartShizukuProcess(script.toString())
        val exit = process.waitFor()
        if (exit == 0) {
            context.getSharedPreferences(HUB_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(HUB_PREF_CODE, checked.latestCode)
                .putString(HUB_PREF_NAME, checked.latestName)
                .apply()
            hubCheckUpdate(context).copy(
                badge = "INSTALLED",
                message = "Update berhasil diinstall.",
                dataMarker = "Patch OK",
                canInstall = false,
                canRequestPermission = false,
                showGuide = false
            )
        } else {
            checked.copy(badge = "FAILED", message = "Install gagal. Shizuku tidak bisa menulis file.", canInstall = true)
        }
    } catch (_: Throwable) {
        checked.copy(badge = "FAILED", message = "Install gagal. Cek Shizuku lalu coba lagi.", canInstall = true)
    }
}

private fun hubStartShizukuProcess(script: String): Process {
    val method = Shizuku::class.java.getDeclaredMethod(
        "newProcess",
        Array<String>::class.java,
        Array<String>::class.java,
        String::class.java
    )
    method.isAccessible = true
    return method.invoke(null, arrayOf("sh", "-c", script), null, null) as Process
}

private fun hubRequestShizukuPermission() {
    try {
        Shizuku.requestPermission(HUB_SHIZUKU_REQUEST_CODE)
    } catch (_: Throwable) {
    }
}

private fun hubFetchManifestJson(): JSONObject {
    val connection = URL(hubManifestUrl()).openConnection() as HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 25000
    return try {
        BufferedReader(InputStreamReader(connection.inputStream)).use { JSONObject(it.readText()) }
    } finally {
        connection.disconnect()
    }
}

private fun hubManifestUrl(): String = arrayOf(
    "https://raw.", "githubusercontent.com/", "drmacze/", "F16/", "main/", "updates/", "latest.json"
).joinToString(separator = "")

private fun hubFindInlinePatch(json: JSONObject, from: Int, to: Int): JSONObject? {
    val patches = json.optJSONArray("patches") ?: return null
    for (i in 0 until patches.length()) {
        val patch = patches.optJSONObject(i) ?: continue
        if (patch.optInt("from") == from && patch.optInt("to") == to && patch.optString("type") == "inline") return patch
    }
    return null
}

private fun hubShizukuState(): String {
    return try {
        if (!Shizuku.pingBinder()) return "Inactive"
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) "Ready" else "Permission"
    } catch (_: Throwable) {
        "Missing"
    }
}

private fun hubReadDataMarker(): String {
    val markerPath = "/sdcard/Android/data/$HUB_GAME_PACKAGE/.dlavie26_data_installed"
    val testPath = "/sdcard/Android/data/$HUB_GAME_PACKAGE/DLavieLauncherTest.txt"
    return try {
        val marker = File(markerPath)
        if (marker.exists()) return marker.readText().trim().take(12).ifEmpty { "Detected" }
        val test = File(testPath)
        if (test.exists()) return "Patch OK"
        "No marker"
    } catch (_: Throwable) {
        "Protected"
    }
}

private fun hubSafeRelativePath(path: String): Boolean {
    return path.isNotBlank() && !path.startsWith("/") && !path.contains("..") && !path.contains("\\")
}

private fun hubShellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

private fun hubIsGameInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo(HUB_GAME_PACKAGE, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

private fun hubLaunchGame(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(HUB_GAME_PACKAGE)
    if (launch != null) context.startActivity(launch)
}

private val HubFont = FontFamily.SansSerif
private val HubDark = Color(0xFF050606)
private val HubCard = Color(0xFF101111)
private val HubBorder = Color(0xFF252A2C)
private val HubWhite = Color(0xFFF7F7F7)
private val HubMuted = Color(0xFF7A7F83)
private val HubGreen = Color(0xFF20E070)
private val HubCyan = Color(0xFF28D7FF)
private val HubRed = Color(0xFFFF4D4D)
