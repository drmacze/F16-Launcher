package com.drmacze.f16launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════════════════════
// GAME DETAIL SCREEN (Phase 2 — TapTap-style detail page)
//
// Diakses dari Beranda saat user tap TTGameCard. Menampilkan hero header
// dengan gradient, rating, action button (Mainkan/Dapatkan/Diblokir),
// about section, dan info cards (Kategori, Versi, Ukuran).
//
// Pakai TapTapDesignSystem tokens (TTSpacing, TTShapes, colors).
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    gameInstalled: Boolean,
    avgRating: Double,
    ratingCount: Int,
    maintenanceBlocked: Boolean = false
) {
    Box(Modifier.fillMaxSize().background(Carbon)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // ── Hero header with gradient ──
            Box(
                Modifier.fillMaxWidth().height(280.dp)
            ) {
                // Gradient background (top: blue tint → bottom: Carbon blend)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(CandyBlue.copy(0.3f), Carbon)
                        )
                    )
                )

                // Back button (top-left, floating circle)
                Box(
                    Modifier.padding(TTSpacing.lg).size(40.dp)
                        .clip(CircleShape).background(Color.Black.copy(0.5f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack, null,
                        tint = Color.White, modifier = Modifier.size(24.dp)
                    )
                }

                // Bottom-anchored content: cover + title + subtitle
                Column(
                    Modifier.fillMaxSize().padding(TTSpacing.lg),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(listOf(CandyCyan, CandyBlue, PremiumViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DL", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(TTSpacing.md))
                    Text(
                        "DLavie 26: Football Game",
                        color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black
                    )
                    Text(
                        "FIFA 16 Mobile · Mod",
                        color = SoftText, fontSize = 13.sp
                    )
                }
            }

            // ── Rating + Action button row ──
            Row(
                Modifier.fillMaxWidth().padding(TTSpacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val rating10 = String.format("%.1f", avgRating * 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = AmberWarn, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(TTSpacing.xs))
                        Text(rating10, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("/10", color = SoftText, fontSize = 14.sp)
                    }
                    Text("$ratingCount ratings", color = SoftText, fontSize = 11.sp)
                }

                // Action button — state-aware (maintenanceBlocked > gameInstalled > download)
                when {
                    maintenanceBlocked -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            shape = TTShapes.chip
                        ) {
                            Text("Diblokir Maintenance", fontSize = 12.sp)
                        }
                    }
                    gameInstalled -> {
                        Button(
                            onClick = onPlay,
                            shape = TTShapes.chip,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Carbon
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(TTSpacing.xs))
                            Text("Mainkan", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            shape = TTShapes.chip,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CandyCyan,
                                contentColor = Carbon
                            )
                        ) {
                            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(TTSpacing.xs))
                            Text("Dapatkan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── About section ──
            Column(Modifier.padding(TTSpacing.lg)) {
                TTSectionHeader(title = "Tentang Game", icon = Icons.Rounded.Info)
                Spacer(Modifier.height(TTSpacing.sm))
                Text(
                    "DLavie 26: Football Game adalah mod FIFA 16 Mobile dengan gameplay yang ditingkatkan, " +
                    "roster update, dan fitur tambahan. Mainkan mode career, ultimate team, dan online match " +
                    "dengan komunitas DLavie.",
                    color = SoftText, fontSize = 13.sp, lineHeight = 20.sp
                )
            }

            // ── Info cards row (Kategori, Versi, Ukuran) ──
            Row(
                Modifier.fillMaxWidth().padding(TTSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(TTSpacing.md)
            ) {
                InfoCard("Kategori", "Olahraga", Icons.Rounded.SportsSoccer, Modifier.weight(1f))
                InfoCard("Versi", "1.2.0", Icons.Rounded.Update, Modifier.weight(1f))
                InfoCard("Ukuran", "33 MB", Icons.Rounded.Storage, Modifier.weight(1f))
            }

            // ── Inline maintenance note (kalau blocked) ──
            AnimatedVisibility(
                visible = maintenanceBlocked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(Modifier.padding(horizontal = TTSpacing.lg)) {
                    GlassInfoBox(
                        icon = Icons.Rounded.Info,
                        color = AmberWarn,
                        text = "Game tidak bisa dimainkan saat maintenance mode aktif (scope: partial). " +
                               "Coba lagi nanti setelah maintenance selesai."
                    )
                }
            }

            // Bottom padding (so content tidak ketutup FloatingNav)
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun InfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Column(
            Modifier.padding(TTSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = CandyCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(TTSpacing.xs))
            Text(label, color = SubText, fontSize = 10.sp)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
