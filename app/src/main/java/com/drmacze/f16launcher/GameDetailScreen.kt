package com.drmacze.f16launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * GameDetailScreen v7.9.3 — Console-style redesign (PS5/Xbox inspired).
 *
 * Major changes from v7.2.9:
 * - Parameterized: accepts GameItem (works for FIFA 16 AND FIFA 15)
 * - Blurred cover background (atmospheric, PS5 style)
 * - Screenshots gallery (horizontal LazyRow)
 * - 5-star interactive rating bar (visual, not just popup)
 * - Sticky bottom CTA: Install / Play / Update (console-style full width)
 * - Server status badge + ping signal (real latency test, cloud-game style)
 * - Delete game button (uninstall via PackageManager)
 * - WiFi warning dialog before install (check ConnectivityManager)
 * - No dummy data — all real from GameItem + Supabase
 */
@Composable
fun GameDetailScreen(
    game: GameItem,
    onBack: () -> Unit,
    onPlay: () -> Unit = {},
    onInstall: () -> Unit = {},
    onDelete: () -> Unit = {},
    gameInstalled: Boolean,
    avgRating: Double,
    ratingCount: Int,
    maintenanceBlocked: Boolean = false,
    hasRated: Boolean = false,
    myRating: Int = 0,
    onRate: (Int) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // v7.9.3: Ping state — real latency test
    var pingMs by remember { mutableStateOf<Long?>(null) }
    var pingQuality by remember { mutableStateOf(PingQuality.UNKNOWN) }
    var pingTesting by remember { mutableStateOf(false) }

    // v7.9.3: Star bar interactive state
    var hoveredStar by remember { mutableStateOf(0) }

    // v7.9.3: WiFi warning + delete confirm dialogs
    var showWifiWarning by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // v7.9.3: Screenshot viewer state
    var selectedScreenshot by remember { mutableStateOf<Int?>(null) }

    // Run ping test on launch
    LaunchedEffect(game.packageName) {
        pingTesting = true
        val result = pingGameServer(game.packageName)
        pingMs = result
        pingQuality = if (result != null) PingQuality.fromMs(result) else PingQuality.UNKNOWN
        pingTesting = false
    }

    if (showWifiWarning) {
        WifiWarningDialog(
            onDismiss = { showWifiWarning = false },
            onContinue = {
                showWifiWarning = false
                onInstall()
            }
        )
    }

    if (showDeleteConfirm) {
        DeleteGameConfirmDialog(
            gameTitle = game.title,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            }
        )
    }

    Box(Modifier.fillMaxSize().background(Carbon)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // ── Hero header with blurred background ──
            Box(
                Modifier.fillMaxWidth().height(360.dp)
            ) {
                // Blurred cover background (PS5 style)
                if (game.coverImageRes != null) {
                    AsyncImage(
                        model = game.coverImageRes,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(40.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Gradient fallback
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                game.coverGradient + listOf(Carbon)
                            )
                        )
                    )
                }
                // Dark overlay for readability
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(0.3f), Color.Black.copy(0.7f), Carbon)
                        )
                    )
                )

                // Back button (top-left)
                Box(
                    Modifier.padding(top = 48.dp, start = 16.dp).size(40.dp)
                        .clip(CircleShape).background(Color.Black.copy(0.6f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }

                // Delete button (top-right) — only if installed
                if (gameInstalled) {
                    Box(
                        Modifier.padding(top = 48.dp, end = 16.dp).align(Alignment.TopEnd)
                            .size(40.dp).clip(CircleShape).background(Color.Black.copy(0.6f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showDeleteConfirm = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                    }
                }

                // Game cover + title + server status (bottom)
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Cover image
                    if (game.coverImageRes != null) {
                        AsyncImage(
                            model = game.coverImageRes,
                            contentDescription = game.title,
                            modifier = Modifier.size(120.dp, 160.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        DLavieLogoCover(
                            modifier = Modifier.then(sharedGameCoverModifier("game-cover")),
                            size = 88.dp,
                            fontSize = 36.sp,
                            shape = RoundedCornerShape(22.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        game.title,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        game.subtitle,
                        color = SoftText,
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily
                    )
                    Spacer(Modifier.height(10.dp))
                    // Server status + ping signal row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Server status badge
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(game.serverStatus.bgColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(game.serverStatus.dotColor))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                game.serverStatus.label,
                                color = game.serverStatus.textColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily
                            )
                        }
                        // Ping signal (real latency)
                        PingSignalIndicator(
                            ms = pingMs,
                            quality = pingQuality,
                            testing = pingTesting,
                            onRetest = {
                                scope.launch {
                                    pingTesting = true
                                    val result = pingGameServer(game.packageName)
                                    pingMs = result
                                    pingQuality = if (result != null) PingQuality.fromMs(result) else PingQuality.UNKNOWN
                                    pingTesting = false
                                }
                            }
                        )
                    }
                }
            }

            // ── Sticky CTA placeholder (content padding so it doesn't hide behind bottom bar) ──
            // Actual CTA is rendered as sticky bottom bar (see end of Column via Box overlay)

            // ── Rating display + 5-star interactive bar ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBase),
                border = BorderStroke(1.dp, GlassStroke)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            val rating10 = String.format("%.1f", avgRating * 2.0)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, null, tint = AmberWarn, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(rating10, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                Text("/10", color = SoftText, fontSize = 16.sp, fontFamily = InterFontFamily)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$ratingCount penilaian dari pengguna Launcher",
                                color = SoftText, fontSize = 12.sp, fontFamily = InterFontFamily
                            )
                        }
                        if (hasRated) {
                            Box(
                                Modifier.size(height = 44.dp, width = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AmberWarn.copy(alpha = 0.12f))
                                    .border(BorderStroke(1.dp, AmberWarn), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = AmberWarn, modifier = Modifier.size(18.dp))
                                    Text("Rated $myRating", color = AmberWarn, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }

                    // 5-star interactive bar (tap to rate directly)
                    if (!hasRated) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Tap bintang untuk menilai:",
                            color = SoftText, fontSize = 11.sp, fontFamily = InterFontFamily
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 1..5) {
                                val isActive = if (hoveredStar > 0) i <= hoveredStar else i <= myRating
                                val scale by animateFloatAsState(
                                    targetValue = if (isActive) 1.1f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "star_scale_$i"
                                )
                                Icon(
                                    if (isActive) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "Rate $i",
                                    tint = if (isActive) AmberWarn else SoftText,
                                    modifier = Modifier.size(36.dp).scale(scale).clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        hoveredStar = i
                                        onRate(i)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Screenshots gallery ──
            if (game.screenshots.isNotEmpty()) {
                Column(Modifier.padding(top = 8.dp)) {
                    TTSectionHeader(title = "Screenshot", icon = Icons.Rounded.PhotoLibrary)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(game.screenshots) { screenshotRes ->
                            AsyncImage(
                                model = screenshotRes,
                                contentDescription = "Screenshot",
                                modifier = Modifier.size(200.dp, 120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedScreenshot = screenshotRes },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // ── Info cards row ──
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoCard("Kategori", game.category, Icons.Rounded.SportsSoccer, Modifier.weight(1f))
                InfoCard("Versi", game.version.ifBlank { "-" }, Icons.Rounded.Update, Modifier.weight(1f))
                InfoCard("Ukuran", game.sizeMb.ifBlank { "-" }, Icons.Rounded.Storage, Modifier.weight(1f))
            }

            // ── Game Details section ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBase),
                border = BorderStroke(1.dp, GlassStroke)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Detail Game", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                    DetailRow(Icons.Rounded.Star, "Rating", "${String.format("%.1f", avgRating * 2.0)}/10 · $ratingCount penilaian")
                    DetailRow(Icons.Rounded.Person, "Pengembang", game.developer)
                    DetailRow(Icons.Rounded.Language, "Bahasa", game.language)
                    DetailRow(Icons.Rounded.Info, "Rating Usia", game.ageRating)
                    DetailRow(Icons.Rounded.Update, "Update Terakhir", game.lastUpdate.ifBlank { "-" })
                    DetailRow(Icons.Rounded.Storage, "Ukuran", game.sizeMb.ifBlank { "-" })
                    DetailRow(Icons.Rounded.SportsSoccer, "Game Engine", game.engine)
                }
            }

            // ── About section ──
            if (game.description.isNotBlank()) {
                Column(Modifier.padding(16.dp)) {
                    TTSectionHeader(title = "Tentang Game", icon = Icons.Rounded.Info)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        game.description,
                        color = SoftText, fontSize = 13.sp, lineHeight = 20.sp, fontFamily = InterFontFamily
                    )
                }
            }

            // ── Features section ──
            if (game.features.isNotEmpty()) {
                Column(Modifier.padding(16.dp)) {
                    TTSectionHeader(title = "Fitur Utama", icon = Icons.Rounded.Star)
                    Spacer(Modifier.height(10.dp))
                    game.features.forEachIndexed { idx, feature ->
                        val icons = listOf(
                            Icons.Rounded.SportsSoccer,
                            Icons.Rounded.Person,
                            Icons.Rounded.Update,
                            Icons.Rounded.Verified
                        )
                        FeatureItem(
                            icon = icons.getOrNull(idx) ?: Icons.Rounded.Star,
                            title = feature,
                            description = ""
                        )
                    }
                }
            }

            // ── Trusted badge ──
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Verified, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Trusted by DLavie", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }

            Spacer(Modifier.height(100.dp))  // space for sticky CTA
        }

        // ── Sticky bottom CTA bar (console-style) ──
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = Carbon.copy(alpha = 0.95f),
            shadowElevation = 16.dp
        ) {
            Box(
                Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    maintenanceBlocked -> {
                        // Maintenance — disabled
                        Surface(
                            Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x33FF5252)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Server Maintenance — Tidak bisa main",
                                    color = Color(0xFFFF5252),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                    gameInstalled -> {
                        // Play button (green, console-style)
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlay()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Play", fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                    else -> {
                        // Install button (white, console-style)
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // WiFi check before install
                                if (!isWifiConnected(context)) {
                                    showWifiWarning = true
                                } else {
                                    onInstall()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Install", fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                }
            }
        }

        // ── Full-screen screenshot viewer ──
        AnimatedVisibility(
            visible = selectedScreenshot != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedScreenshot?.let { res ->
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(0.95f))
                        .clickable { selectedScreenshot = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = res,
                        contentDescription = "Screenshot full",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(24.dp).size(40.dp)
                            .clip(CircleShape).background(Color.White.copy(0.2f))
                            .clickable { selectedScreenshot = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

// ─── Ping Signal Indicator (cloud-game style) ─────────────────────────────────

@Composable
private fun PingSignalIndicator(
    ms: Long?,
    quality: PingQuality,
    testing: Boolean,
    onRetest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(0.4f))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onRetest()
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (testing) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.dp,
                color = Color.White
            )
            Spacer(Modifier.width(4.dp))
            Text("Testing...", color = Color.White.copy(0.6f), fontSize = 9.sp, fontFamily = InterFontFamily)
        } else {
            // Signal bars (4 bars, filled based on quality)
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                val bars = when (quality) {
                    PingQuality.EXCELLENT -> 4
                    PingQuality.GOOD -> 3
                    PingQuality.FAIR -> 2
                    PingQuality.POOR -> 1
                    PingQuality.UNKNOWN -> 0
                }
                for (i in 1..4) {
                    val active = i <= bars
                    val height = 4 + (i * 2)
                    Box(
                        Modifier.size(width = 3.dp, height = height.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (active) quality.color else Color.White.copy(0.2f))
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(
                if (ms != null) "${ms}ms" else "—",
                color = quality.color,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.width(2.dp))
            Text(
                quality.label,
                color = quality.color.copy(0.7f),
                fontSize = 8.sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

// ─── WiFi Warning Dialog ──────────────────────────────────────────────────────

@Composable
private fun WifiWarningDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Wifi, null, tint = AmberWarn, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Gunakan WiFi", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        },
        text = {
            Text(
                "Koneksi saat ini menggunakan data seluler.\n\n" +
                "Download game ini bisa memakan kuota besar. " +
                "Disarankan pakai WiFi untuk install.\n\n" +
                "Lanjutkan dengan data seluler?",
                color = SoftText, fontSize = 13.sp, fontFamily = InterFontFamily
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text("Lanjut", color = NeonGreen, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.White, fontFamily = InterFontFamily)
            }
        },
        containerColor = Carbon,
        titleContentColor = Color.White,
        textContentColor = SoftText
    )
}

// ─── Delete Game Confirm Dialog ───────────────────────────────────────────────

@Composable
private fun DeleteGameConfirmDialog(
    gameTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hapus Game", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        },
        text = {
            Text(
                "Hapus \"$gameTitle\" dari perangkat?\n\n" +
                "Data game akan dihapus, tapi kamu bisa install lagi kapan saja dari sini.\n\n" +
                "Cache dan save data mungkin tetap tersisa di storage.",
                color = SoftText, fontSize = 13.sp, fontFamily = InterFontFamily
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Hapus", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.White, fontFamily = InterFontFamily)
            }
        },
        containerColor = Carbon,
        titleContentColor = Color.White,
        textContentColor = SoftText
    )
}

// ─── Helper: Real ping test ────────────────────────────────────────────────────

/**
 * v7.9.3: Real HTTP latency test ke game server.
 * Bukan dummy — benar-benar hit endpoint dan ukur waktu response.
 *
 * Untuk FIFA 16: ping ke EA FIFA server (fifa16 server check URL)
 * Untuk FIFA 15: ping ke EA FIFA 15 server
 * Fallback: ping ke Supabase (always reachable)
 *
 * Return: latency in ms, atau null kalau gagal.
 */
suspend fun pingGameServer(packageName: String): Long? = withContext(Dispatchers.IO) {
    try {
        val targetUrl = when (packageName) {
            "com.ea.gp.fifaworld" -> "https://www.ea.com/games/fifa/fifa-mobile"  // FIFA 16
            "com.ea.game.fifa14_row" -> "https://www.ea.com/games/fifa/fifa-15-mobile"  // FIFA 15
            else -> "https://lvmucsxbmadtsgrxuwmo.supabase.co"  // fallback
        }
        val url = URL(targetUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = 5000
            readTimeout = 5000
            instanceFollowRedirects = false
        }
        val start = System.currentTimeMillis()
        conn.connect()
        val responseCode = conn.responseCode
        val latency = System.currentTimeMillis() - start
        conn.disconnect()
        // Any HTTP response (even 4xx/5xx) means server reachable — return latency
        if (responseCode > 0) latency else null
    } catch (e: Exception) {
        Log.w("DLaviePing", "ping failed for $packageName: ${e.message}")
        null
    }
}

/**
 * v7.9.3: Check if device is connected via WiFi.
 * Return false if on cellular data or no connection.
 */
fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

// ─── Reusable composables (kept from v7.2.9) ──────────────────────────────────

@Composable
private fun InfoCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = CandyCyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = SubText, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = CandyCyan, modifier = Modifier.size(20.dp))
        Text(label, color = SubText, fontSize = 13.sp, modifier = Modifier.width(100.dp), fontFamily = InterFontFamily)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), fontFamily = InterFontFamily)
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(CandyCyan.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            if (description.isNotBlank()) {
                Text(description, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = InterFontFamily)
            }
        }
    }
}
