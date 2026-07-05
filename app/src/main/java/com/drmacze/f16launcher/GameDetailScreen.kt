package com.drmacze.f16launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * GameDetailScreen v7.2.9 — App Store style game info + rating.
 *
 * Redesign per user request:
 * - Hapus tombol Download/Play (install & play hanya di GameHub)
 * - Ganti dengan tombol "Rate Game" prominent
 * - Tampil info real: developer, version, size, language, age rating, last update
 * - Rating real dari Supabase game_ratings table (bukan dummy)
 * - Layout mirip App Store (screenshot referensi IMG_4529, IMG_4530)
 */
@Composable
fun GameDetailScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit = {},
    onDownload: () -> Unit = {},
    gameInstalled: Boolean,
    avgRating: Double,
    ratingCount: Int,
    maintenanceBlocked: Boolean = false,
    hasRated: Boolean = false,
    myRating: Int = 0,
    onRate: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Box(Modifier.fillMaxSize().background(Carbon)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // ── Hero header ──
            Box(
                Modifier.fillMaxWidth().height(280.dp)
            ) {
                HalftoneBackground(
                    modifier = Modifier.fillMaxSize(),
                    dotSize = 2.5f,
                    spacing = 20f,
                    baseColor = HalftoneBright
                )

                // Back button (top-left)
                Box(
                    Modifier
                        .padding(top = 48.dp, start = 16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.6f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }

                // Game cover + title (bottom)
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    DLavieLogoCover(
                        modifier = Modifier.then(sharedGameCoverModifier("game-cover")),
                        size = 88.dp,
                        fontSize = 36.sp,
                        shape = RoundedCornerShape(22.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "DLavie 26: Football Game",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        "FIFA 16 Mobile · Mod · Sports",
                        color = SoftText,
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }

            // ── Rating display (App Store style) ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBase),
                border = BorderStroke(1.dp, GlassStroke)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
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
                            color = SoftText,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily
                        )
                    }

                    // Rate button (App Store style)
                    if (hasRated) {
                        Box(
                            modifier = Modifier
                                .size(height = 44.dp, width = 110.dp)
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
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRate()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberWarn,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.size(height = 44.dp, width = 110.dp)
                        ) {
                            Icon(Icons.Rounded.StarBorder, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Rate", fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                }
            }

            // ── Status info (App Store style — "Dimainkan X lalu" / "Belum dimainkan") ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBase),
                border = BorderStroke(1.dp, GlassStroke)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(40.dp)
                            .background(if (gameInstalled) NeonGreen.copy(0.15f) else DangerRed.copy(0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (gameInstalled) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                            null,
                            tint = if (gameInstalled) NeonGreen else DangerRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (gameInstalled) "Sudah Terinstall" else "Belum Terinstall",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            if (gameInstalled) "Buka GameHub untuk main" else "Buka GameHub untuk install",
                            color = SoftText,
                            fontSize = 11.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }

            // ── Info cards row (real data — bukan dummy) ──
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoCard("Kategori", "Olahraga", Icons.Rounded.SportsSoccer, Modifier.weight(1f))
                InfoCard("Versi", "v7.2.8", Icons.Rounded.Update, Modifier.weight(1f))
                InfoCard("Ukuran", "34 MB", Icons.Rounded.Storage, Modifier.weight(1f))
            }

            // ── Game Details section (App Store style) ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBase),
                border = BorderStroke(1.dp, GlassStroke)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Detail Game",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    DetailRow(Icons.Rounded.Star, "Rating", "${
                        String.format("%.1f", avgRating * 2.0)
                    }/10 · $ratingCount penilaian")
                    DetailRow(Icons.Rounded.Person, "Pengembang", "DLavie Company")
                    DetailRow(Icons.Rounded.Language, "Bahasa", "Indonesia, Inggris")
                    DetailRow(Icons.Rounded.Info, "Rating Usia", "9+ (Semua umur)")
                    DetailRow(Icons.Rounded.Update, "Update Terakhir", "5 Juli 2026")
                    DetailRow(Icons.Rounded.Storage, "Ukuran APK", "34 MB")
                    DetailRow(Icons.Rounded.SportsSoccer, "Game Engine", "EA Sports FIFA 16")
                }
            }

            // ── About section ──
            Column(Modifier.padding(16.dp)) {
                TTSectionHeader(title = "Tentang Game", icon = Icons.Rounded.Info)
                Spacer(Modifier.height(10.dp))
                Text(
                    "DLavie 26: Football Game adalah mod FIFA 16 Mobile dengan gameplay yang ditingkatkan, " +
                    "roster update musim 2025/2026, dan fitur tambahan. Mainkan mode career, ultimate team, " +
                    "dan online match dengan komunitas DLavie.\n\n" +
                    "Game ini dimaintain oleh DLavie Company dan didistribusikan melalui DLavie Launcher. " +
                    "Untuk install dan main, buka GameHub dari menu navigasi.",
                    color = SoftText, fontSize = 13.sp, lineHeight = 20.sp, fontFamily = InterFontFamily
                )
            }

            // ── Features section ──
            Column(Modifier.padding(16.dp)) {
                TTSectionHeader(title = "Fitur Utama", icon = Icons.Rounded.Star)
                Spacer(Modifier.height(10.dp))
                FeatureItem(Icons.Rounded.SportsSoccer, "Gameplay Realistis", "Mod gameplay yang ditingkatkan untuk pengalaman lebih realistis")
                FeatureItem(Icons.Rounded.Person, "Komunitas Aktif", "Bergabung dengan ribuan pemain di komunitas DLavie")
                FeatureItem(Icons.Rounded.Update, "Update Rutin", "Patch mod baru secara berkala via launcher")
                FeatureItem(Icons.Rounded.Verified, "Aman & Terverifikasi", "Trusted by DLavie — game resmi yang diverifikasi")
            }

            // ── Note: Install via GameHub ──
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GlassBase),
                border = BorderStroke(1.dp, GlassStroke)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Info, null, tint = CandyCyan, modifier = Modifier.size(18.dp))
                        Text(
                            "Cara Install & Main",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                    Text(
                        "1. Buka tab GameHub di navigasi bawah\n" +
                        "2. Tap card FIFA 16 Mobile\n" +
                        "3. Follow step-by-step install di floating panel\n" +
                        "4. Setelah selesai, tap Play untuk main",
                        color = SoftText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = InterFontFamily
                    )
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

            Spacer(Modifier.height(100.dp))
        }
    }
}

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
            Text(description, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp, fontFamily = InterFontFamily)
        }
    }
}
