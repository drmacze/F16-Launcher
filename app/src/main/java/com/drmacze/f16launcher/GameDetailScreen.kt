package com.drmacze.f16launcher

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

/**
 * Game Detail Screen — TapTap-style game detail page.
 *
 * Features:
 *  - Hero header dengan gradient + game cover
 *  - Rating display (real dari Supabase game_ratings)
 *  - Action button (Mainkan / Dapatkan / Diblokir Maintenance)
 *  - About section
 *  - Info cards (Kategori, Versi, Ukuran)
 *  - Smooth scroll
 */
@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    gameInstalled: Boolean,
    avgRating: Double,
    ratingCount: Int,
    maintenanceBlocked: Boolean = false,
    onRate: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(Carbon)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // ── Hero header ──
            Box(
                Modifier.fillMaxWidth().height(320.dp)
            ) {
                // Animated gradient background
                val infiniteTransition = rememberInfiniteTransition(label = "detail_bg")
                val gradientProgress by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse
                    ),
                    label = "gradient"
                )

                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            colors = listOf(
                                CandyBlue.copy(0.4f + gradientProgress * 0.2f),
                                CandyCyan.copy(0.2f),
                                Carbon
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                )

                // Mesh overlay
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(0.06f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.3f, h * 0.2f),
                            radius = w * 0.8f
                        )
                    )
                }

                // Back button (top-left)
                Box(
                    Modifier
                        .padding(top = 48.dp, start = 16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.6f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }

                // Game cover + title (bottom)
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier.size(88.dp).clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(listOf(CandyCyan, CandyBlue, PremiumViolet))
                            )
                            .softGlow(CandyCyan, radius = 20f, alpha = 0.4f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DL", color = Carbon, fontSize = 36.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "DLavie 26: Football Game",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("FIFA 16 Mobile · Mod", color = SoftText, fontSize = 13.sp)
                }
            }

            // ── Rating + Action ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val rating10 = String.format("%.1f", avgRating * 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = AmberWarn, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(rating10, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("/10", color = SoftText, fontSize = 14.sp)
                    }
                    Text("$ratingCount ratings", color = SoftText, fontSize = 11.sp)
                }

                // Rate button
                OutlinedButton(
                    onClick = onRate,
                    shape = TTShapes.chip,
                    border = BorderStroke(1.dp, AmberWarn.copy(0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarn)
                ) {
                    Icon(Icons.Rounded.StarBorder, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Action button (Mainkan/Dapatkan) ──
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                when {
                    maintenanceBlocked -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = TTShapes.button,
                            colors = ButtonDefaults.outlinedButtonColors(
                                disabledContainerColor = Surface2,
                                disabledContentColor = SubText
                            )
                        ) {
                            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Diblokir Maintenance", fontWeight = FontWeight.Bold)
                        }
                    }
                    gameInstalled -> {
                        Button(
                            onClick = onPlay,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = TTShapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Carbon
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mainkan Sekarang", fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                                .softGlow(CandyCyan, radius = 24f, alpha = 0.4f),
                            shape = TTShapes.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CandyCyan,
                                contentColor = Carbon
                            )
                        ) {
                            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Dapatkan", fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // ── Info cards row ──
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoCard("Kategori", "Olahraga", Icons.Rounded.SportsSoccer, Modifier.weight(1f))
                InfoCard("Versi", "1.2.0", Icons.Rounded.Update, Modifier.weight(1f))
                InfoCard("Ukuran", "33 MB", Icons.Rounded.Storage, Modifier.weight(1f))
            }

            // ── About section ──
            Column(Modifier.padding(16.dp)) {
                TTSectionHeader(title = "Tentang Game", icon = Icons.Rounded.Info)
                Spacer(Modifier.height(10.dp))
                Text(
                    "DLavie 26: Football Game adalah mod FIFA 16 Mobile dengan gameplay yang ditingkatkan, " +
                    "roster update, dan fitur tambahan. Mainkan mode career, ultimate team, dan online match " +
                    "dengan komunitas DLavie.",
                    color = SoftText, fontSize = 13.sp, lineHeight = 20.sp
                )
            }

            // ── Features section ──
            Column(Modifier.padding(16.dp)) {
                TTSectionHeader(title = "Fitur Unggulan", icon = Icons.Rounded.Star)
                Spacer(Modifier.height(10.dp))
                FeatureItem(Icons.Rounded.SportsSoccer, "Gameplay Realistis", "Mod gameplay yang ditingkatkan untuk pengalaman lebih realistis")
                FeatureItem(Icons.Rounded.Group, "Komunitas Aktif", "Bergabung dengan ribuan pemain di komunitas DLavie")
                FeatureItem(Icons.Rounded.Update, "Update Rutin", "Patch mod baru secara berkala via launcher")
                FeatureItem(Icons.Rounded.Security, "Aman & Terverifikasi", "Trusted by DLavie — game resmi yang diverifikasi")
            }

            // ── Trusted badge ──
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Verified, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Trusted by DLavie", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = TTShapes.card,
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = CandyCyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = SubText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
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
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
