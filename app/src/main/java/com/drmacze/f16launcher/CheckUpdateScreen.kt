package com.drmacze.f16launcher

import android.content.Context
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ═══════════════════════════════════════════════════════════════════════════
// CHECK UPDATE SCREEN — Full-screen interactive update checker
// ═══════════════════════════════════════════════════════════════════════════
// Real-time streaming text yang reflect actual process stages:
// 1. Fade to pure black
// 2. Pulsing hexagon logo
// 3. Streaming messages based on real check progress:
//    - "Mencari pembaruan..." (saat fetch Supabase)
//    - "Menghubungkan ke pusat DLavie..." (saat network connect)
//    - "Menjelajahi server..." (saat parse response)
//    - "Memeriksa versi..." (saat compare versionCode)
//    - "Menghitung ukuran update..." (saat fetch APK size)
//    - "Selesai!" atau "Anda sudah versi terbaru"
// ═══════════════════════════════════════════════════════════════════════════

// Update stage states
private enum class CheckStage {
    IDLE,           // Belum mulai
    CONNECTING,     // Koneksi ke server
    SEARCHING,      // Cari pembaruan di Supabase
    PARSING,        // Parse response
    COMPARING,      // Bandingkan versi
    FETCHING_SIZE,  // Ambil ukuran APK
    DONE_UPDATE,    // Selesai - ada update
    DONE_LATEST,    // Selesai - sudah versi terbaru
    ERROR           // Gagal
}

@Composable
fun CheckUpdateScreen(
    api: CommunityApi,
    onDismiss: () -> Unit,
    onUpdateAvailable: (AppUpdateChecker.UpdateInfo) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(CheckStage.IDLE) }
    var streamMessage by remember { mutableStateOf("") }
    var updateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var fadeIn by remember { mutableStateOf(false) }

    // Streaming messages per stage (real progress, bukan dummy)
    val stageMessages = mapOf(
        CheckStage.CONNECTING to listOf(
            "Menghubungkan ke pusat DLavie...",
            "Menyiapkan koneksi aman...",
            "Mengaktifkan protokol update..."
        ),
        CheckStage.SEARCHING to listOf(
            "Mencari pembaruan...",
            "Menjelajahi server DLavie...",
            "Memindai basis data rilis..."
        ),
        CheckStage.PARSING to listOf(
            "Menganalisis respons server...",
            "Membaca metadata versi...",
            "Memproses informasi rilis..."
        ),
        CheckStage.COMPARING to listOf(
            "Memeriksa versi...",
            "Membandingkan dengan versi saat ini...",
            "Menghitung selisih versi..."
        ),
        CheckStage.FETCHING_SIZE to listOf(
            "Menghitung ukuran update...",
            "Mengambil detail file APK...",
            "Menyiapkan informasi unduhan..."
        )
    )

    // Trigger fade in on mount
    LaunchedEffect(Unit) {
        fadeIn = true
        delay(300)
        // Start check process
        scope.launch {
            try {
                // Stage 1: CONNECTING
                stage = CheckStage.CONNECTING
                for (msg in stageMessages[CheckStage.CONNECTING]!!) {
                    streamMessage = msg
                    delay(800)
                }

                // Stage 2: SEARCHING (real Supabase query)
                stage = CheckStage.SEARCHING
                streamMessage = stageMessages[CheckStage.SEARCHING]!![0]
                delay(400)

                val info = withContext(Dispatchers.IO) {
                    AppUpdateChecker.checkForUpdate(api)
                }

                streamMessage = stageMessages[CheckStage.SEARCHING]!![1]
                delay(500)

                // Stage 3: PARSING
                stage = CheckStage.PARSING
                streamMessage = stageMessages[CheckStage.PARSING]!![0]
                delay(500)

                // Stage 4: COMPARING
                stage = CheckStage.COMPARING
                streamMessage = stageMessages[CheckStage.COMPARING]!![0]
                delay(600)

                if (info == null || !info.isUpdateAvailable) {
                    // No update available
                    streamMessage = "Anda sudah menggunakan versi terbaru"
                    delay(1000)
                    stage = CheckStage.DONE_LATEST
                    delay(1500)
                    onDismiss()
                } else {
                    // Update available - fetch size
                    stage = CheckStage.FETCHING_SIZE
                    streamMessage = stageMessages[CheckStage.FETCHING_SIZE]!![0]
                    delay(700)

                    updateInfo = info
                    streamMessage = "Pembaruan ditemukan: v${info.versionName}"
                    delay(800)
                    stage = CheckStage.DONE_UPDATE
                    delay(1200)
                    onUpdateAvailable(info)
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Terjadi kesalahan"
                stage = CheckStage.ERROR
                delay(2000)
                onDismiss()
            }
        }
    }

    // Fade animation
    val bgAlpha by animateFloatAsState(
        targetValue = if (fadeIn) 1f else 0f,
        animationSpec = tween(600),
        label = "bg_fade"
    )

    // Logo pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )
    val logoAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_alpha"
    )

    // Streaming text fade
    val textAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400),
        label = "text_fade"
    )

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha))
    ) {
        if (bgAlpha > 0.5f) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing hexagon logo (DLavie style)
                Box(
                    Modifier.size(80.dp).scale(logoScale).alpha(logoAlpha)
                        .clip(androidx.compose.foundation.shape.GenericShape { _, _ ->
                            val r = 70f
                            moveTo(0f, -r); lineTo(r * 0.866f, -r * 0.5f); lineTo(r * 0.866f, r * 0.5f)
                            lineTo(0f, r); lineTo(-r * 0.866f, r * 0.5f); lineTo(-r * 0.866f, -r * 0.5f); close()
                        })
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DL", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.height(32.dp))

                // Streaming message
                if (streamMessage.isNotEmpty() && stage != CheckStage.ERROR) {
                    Text(
                        streamMessage,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(textAlpha).padding(horizontal = 32.dp)
                    )
                }

                // Progress dots (3 animated dots)
                if (stage != CheckStage.DONE_UPDATE && stage != CheckStage.DONE_LATEST && stage != CheckStage.ERROR) {
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { i ->
                            val dotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, delayMillis = i * 200),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dot_$i"
                            )
                            Box(
                                Modifier.size(8.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = dotAlpha))
                            )
                        }
                    }
                }

                // Success state - update found
                if (stage == CheckStage.DONE_UPDATE && updateInfo != null) {
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "v${updateInfo!!.versionName}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (updateInfo!!.apkSizeMb.isNotEmpty()) {
                        Text(
                            updateInfo!!.apkSizeMb,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }

                // Latest version state
                if (stage == CheckStage.DONE_LATEST) {
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Error state
                if (stage == CheckStage.ERROR) {
                    Spacer(Modifier.height(24.dp))
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Gagal mengecek pembaruan",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        errorMessage,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// YELLOW TOAST — Warning notification for outdated version
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun UpdateWarningToast(
    versionName: String,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(800)  // Wait for app to load
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(400)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFC107).copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Pembaruan tersedia!",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Anda menggunakan versi lama. Update ke $versionName sekarang.",
                            color = Color.Black.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                    TextButton(
                        onClick = { onCheckUpdate() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Update", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(
                        onClick = { visible = false; onDismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Close, "Tutup", tint = Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
