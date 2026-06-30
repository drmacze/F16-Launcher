package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

private const val DL_GAME_PACKAGE = "com.ea.gp.fifaworld"

private enum class Tab(val label: String, val mark: Mark) {
    Home("Home", Mark.Home),
    Data("Data", Mark.Folder),
    Chat("Chat", Mark.Chat),
    Me("Me", Mark.User)
}

private enum class Mark { Home, Folder, Chat, User, Play, Shield, Check, Alert }

class DLavieHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DLavieHub() }
    }
}

@Composable
private fun DLavieHub() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Dark,
            surface = CardBg,
            primary = Green,
            secondary = Cyan,
            onPrimary = Color(0xFF001407),
            onSecondary = Color(0xFF001018),
            onBackground = White,
            onSurface = White
        )
    ) {
        var tab by remember { mutableStateOf(Tab.Home) }
        Surface(color = Dark, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xFF0B2419), Dark, Color.Black), radius = 900f))
            ) {
                Box(Modifier.fillMaxSize().padding(bottom = 104.dp)) {
                    when (tab) {
                        Tab.Home -> HomeScreen(openData = { tab = Tab.Data }, openChat = { tab = Tab.Chat })
                        Tab.Data -> DataScreen()
                        Tab.Chat -> ComingSoonScreen("Chat", "Community real akan aktif setelah akun dan moderasi siap.", Mark.Chat)
                        Tab.Me -> ComingSoonScreen("Profile", "Login, avatar, saved post, dan notifikasi akan hadir setelah backend aktif.", Mark.User)
                    }
                }
                BottomNav(tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun HomeScreen(openData: () -> Unit, openChat: () -> Unit) {
    val context = LocalContext.current
    val installed = remember { isGameInstalled(context) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeroCard()
        PrimaryActions(
            onPlay = { launchGame(context) },
            openData = openData,
            openChat = openChat
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MiniStatus(
                title = "Game",
                value = if (installed) "Ready" else "Missing",
                mark = if (installed) Mark.Check else Mark.Alert,
                color = if (installed) Green else Red,
                modifier = Modifier.weight(1f)
            )
            MiniStatus(
                title = "Update",
                value = "Secure",
                mark = Mark.Shield,
                color = Cyan,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroCard() {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF0E3A22), Color(0xFF08100D))), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("DL", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Black, fontFamily = AppFont)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("DLavie 26", color = White, fontSize = 31.sp, fontWeight = FontWeight.Black, fontFamily = AppFont, maxLines = 1)
                Text("FIFA 16 Mobile 2026", color = Muted, fontSize = 14.sp, fontFamily = AppFont, maxLines = 1)
            }
            Pill("PROD", Green)
        }
        Spacer(Modifier.height(18.dp))
        Text("Football Reborn", color = White, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = AppFont, maxLines = 1)
        Text("Play, update, and connect with DLavie.", color = Muted, fontSize = 14.sp, fontFamily = AppFont, maxLines = 2)
    }
}

@Composable
private fun PrimaryActions(onPlay: () -> Unit, openData: () -> Unit, openChat: () -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconMark(Mark.Play, Green, Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Actions", color = White, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = AppFont)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF001407)),
            contentPadding = PaddingValues(0.dp)
        ) {
            IconMark(Mark.Play, Color(0xFF001407), Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Play", fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = AppFont)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CompactButton("Data", Mark.Folder, openData, Modifier.weight(1f))
            CompactButton("Update", Mark.Shield, openData, Modifier.weight(1f))
            CompactButton("Chat", Mark.Chat, openChat, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DataScreen() {
    val context = LocalContext.current
    val installed = remember { isGameInstalled(context) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageTitle("Data", "Status game dan update DLavie.")
        StatusCard(installed = installed, onPlay = { launchGame(context) })
        DataRow("FIFA 16 Mobile", if (installed) "Installed" else "Not found", if (installed) "READY" else "MISSING", if (installed) Green else Red, if (installed) Mark.Check else Mark.Alert)
        DataRow("Update Channel", "Managed by DLavie", "SECURE", Green, Mark.Shield)
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = White, fontSize = 38.sp, fontWeight = FontWeight.Black, fontFamily = AppFont, maxLines = 1)
        Text(subtitle, color = Muted, fontSize = 14.sp, fontFamily = AppFont, maxLines = 1)
    }
}

@Composable
private fun StatusCard(installed: Boolean, onPlay: () -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (installed) Mark.Check else Mark.Alert, if (installed) Green else Red)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("DLavie 26", color = White, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = AppFont, maxLines = 1)
                Text(if (installed) "Ready to play" else "Game not found", color = Muted, fontSize = 15.sp, fontFamily = AppFont, maxLines = 1)
            }
            Pill(if (installed) "READY" else "INSTALL", if (installed) Green else Red)
        }
        if (installed) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color(0xFF001407)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Launch Game", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = AppFont)
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(title: String, subtitle: String, mark: Mark) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageTitle(title, subtitle)
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(mark, Green)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Coming Soon", color = White, fontSize = 23.sp, fontWeight = FontWeight.Black, fontFamily = AppFont)
                    Text("No dummy content.", color = Muted, fontSize = 14.sp, fontFamily = AppFont, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MiniStatus(title: String, value: String, mark: Mark, color: Color, modifier: Modifier = Modifier) {
    Panel(modifier = modifier) {
        IconMark(mark, color, Modifier.size(24.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont, maxLines = 1)
        Text(value, color = White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = AppFont, maxLines = 1)
    }
}

@Composable
private fun DataRow(title: String, subtitle: String, status: String, statusColor: Color, mark: Mark) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(mark, statusColor)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = AppFont)
                Text(subtitle, color = Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = AppFont)
            }
            Pill(status, statusColor)
        }
    }
}

@Composable
private fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 680.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        color = Color(0xF00B0C0C),
        shape = RoundedCornerShape(34.dp),
        border = BorderStroke(1.dp, Border),
        shadowElevation = 18.dp
    ) {
        Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Tab.values().forEach { item ->
                val active = selected == item
                Button(
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) Color(0xFF0E3A22) else Color.Transparent,
                        contentColor = if (active) Green else Muted
                    ),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (active) 7.dp else 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconMark(item.mark, if (active) Green else Muted, Modifier.size(if (active) 22.dp else 19.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(item.label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Bold, maxLines = 1, fontFamily = AppFont)
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD101111)),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun CompactButton(label: String, mark: Mark, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101814), contentColor = Green),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = AppFont, maxLines = 1)
    }
}

@Composable
private fun IconTile(mark: Mark, tint: Color) {
    Box(
        Modifier.size(54.dp).background(Color(0xFF071F1E), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconMark(mark, tint, Modifier.size(26.dp))
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), maxLines = 1, fontFamily = AppFont)
    }
}

@Composable
private fun IconMark(type: Mark, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.09f
        fun p(x: Float, y: Float) = Offset(s * x, s * y)
        when (type) {
            Mark.Home -> {
                drawLine(tint, p(0.16f, 0.50f), p(0.50f, 0.20f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.50f, 0.20f), p(0.84f, 0.50f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.28f, 0.46f), p(0.28f, 0.82f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.72f, 0.46f), p(0.72f, 0.82f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.28f, 0.82f), p(0.72f, 0.82f), w, cap = StrokeCap.Round)
            }
            Mark.Folder -> {
                drawLine(tint, p(0.18f, 0.36f), p(0.42f, 0.36f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.42f, 0.36f), p(0.52f, 0.46f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.18f, 0.46f), p(0.82f, 0.46f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.18f, 0.46f), p(0.18f, 0.78f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.82f, 0.46f), p(0.82f, 0.78f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.18f, 0.78f), p(0.82f, 0.78f), w, cap = StrokeCap.Round)
            }
            Mark.Chat -> {
                drawLine(tint, p(0.20f, 0.28f), p(0.80f, 0.28f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.20f, 0.28f), p(0.20f, 0.65f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.80f, 0.28f), p(0.80f, 0.65f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.20f, 0.65f), p(0.42f, 0.65f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.42f, 0.65f), p(0.30f, 0.82f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.42f, 0.65f), p(0.80f, 0.65f), w, cap = StrokeCap.Round)
            }
            Mark.User -> {
                drawCircle(tint, s * 0.15f, p(0.50f, 0.34f), style = Stroke(w))
                drawArc(tint, 200f, 140f, false, p(0.25f, 0.52f), Size(s * 0.50f, s * 0.42f), style = Stroke(w, cap = StrokeCap.Round))
            }
            Mark.Play -> {
                val path = Path().apply {
                    moveTo(s * 0.34f, s * 0.22f)
                    lineTo(s * 0.34f, s * 0.78f)
                    lineTo(s * 0.78f, s * 0.50f)
                    close()
                }
                drawPath(path, tint)
            }
            Mark.Shield -> {
                drawCircle(tint, s * 0.32f, p(0.50f, 0.50f), style = Stroke(w))
                drawLine(tint, p(0.50f, 0.26f), p(0.50f, 0.74f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.26f, 0.50f), p(0.74f, 0.50f), w, cap = StrokeCap.Round)
            }
            Mark.Check -> {
                drawCircle(tint, s * 0.34f, p(0.50f, 0.50f), style = Stroke(w))
                drawLine(tint, p(0.34f, 0.52f), p(0.45f, 0.64f), w, cap = StrokeCap.Round)
                drawLine(tint, p(0.45f, 0.64f), p(0.70f, 0.38f), w, cap = StrokeCap.Round)
            }
            Mark.Alert -> {
                drawCircle(tint, s * 0.34f, p(0.50f, 0.50f), style = Stroke(w))
                drawLine(tint, p(0.50f, 0.28f), p(0.50f, 0.58f), w, cap = StrokeCap.Round)
                drawCircle(tint, s * 0.025f, p(0.50f, 0.72f))
            }
        }
    }
}

private fun isGameInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo(DL_GAME_PACKAGE, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

private fun launchGame(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(DL_GAME_PACKAGE)
    if (launch != null) {
        context.startActivity(launch)
    }
}

private val AppFont = FontFamily.SansSerif
private val Dark = Color(0xFF050606)
private val CardBg = Color(0xFF101111)
private val Border = Color(0xFF252A2C)
private val White = Color(0xFFF7F7F7)
private val Muted = Color(0xFF7A7F83)
private val Green = Color(0xFF20E070)
private val Cyan = Color(0xFF28D7FF)
private val Red = Color(0xFFFF4D4D)
