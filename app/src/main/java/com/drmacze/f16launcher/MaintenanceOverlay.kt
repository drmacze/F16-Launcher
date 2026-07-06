package com.drmacze.f16launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MaintenanceOverlay — full-screen modal that blocks the launcher when the
 * backend reports maintenance mode is enabled.
 *
 * Layout:
 *   - Centered icon (amber warning)
 *   - Title (from app_config.value.title)
 *   - Message body (from app_config.value.message)
 *   - Buttons:
 *       allowOffline = true  → [Lanjut Offline]   [Tutup]
 *       allowOffline = false → [Tutup Aplikasi]
 *
 * Callers should gate visibility with AnimatedVisibility so the overlay
 * slides / fades in cleanly.
 *
 * @param title         Title text from maintenance config
 * @param message       Body text from maintenance config
 * @param allowOffline  Whether the offline play button should be shown
 * @param onContinueOffline  Invoked when user taps "Lanjut Offline"
 * @param onClose       Invoked when user taps "Tutup"/"Tutup Aplikasi"
 */
@Composable
fun MaintenanceOverlay(
    title: String,
    message: String,
    allowOffline: Boolean,
    onContinueOffline: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit  = fadeOut() + scaleOut(targetScale = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1A1205), Color(0xFF0A0602), Color.Black),
                        radius = 1400f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape  = RoundedCornerShape(28.dp),
                color  = Color(0xF0160E03),
                border = BorderStroke(1.dp, Color(0xFF8A5A12).copy(alpha = 0.55f)),
                shadowElevation = 30.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Warning icon with subtle amber glow
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFFB84E).copy(alpha = 0.30f), Color.Transparent)
                                ),
                                RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.WarningAmber,
                            contentDescription = "Peringatan maintenance",
                            tint               = Color(0xFFFFB84E),
                            modifier           = Modifier.size(60.dp)
                        )
                    }

                    // Hard-coded label so the overlay always has a header even if backend omits title
                    Text(
                        "Mode Maintenance",
                        color = Color(0xFFFFB84E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 2.sp
                    )

                    // Title from config
                    Text(
                        title.ifBlank { "Sedang Dalam Pemeliharaan" },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )

                    // Body from config
                    Text(
                        message.ifBlank { "DLavie 26 sedang dalam pemeliharaan. Coba lagi nanti." },
                        color = Color(0xFFC7B7A1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // Buttons
                    if (allowOffline) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick  = onClose,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape    = RoundedCornerShape(16.dp),
                                border   = BorderStroke(1.dp, Color(0xFF8A5A12).copy(alpha = 0.7f))
                            ) {
                                Icon(Icons.Rounded.PowerSettingsNew, null, tint = Color(0xFFFFB84E), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tutup", color = Color(0xFFFFB84E), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Button(
                                onClick  = onContinueOffline,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape    = RoundedCornerShape(16.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB84E),
                                    contentColor   = Color(0xFF1A0F00)
                                )
                            ) {
                                Icon(Icons.Rounded.PlayCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Lanjut Offline", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick  = onClose,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape    = RoundedCornerShape(18.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB84E),
                                contentColor   = Color(0xFF1A0F00)
                            )
                        ) {
                            Icon(Icons.Rounded.PowerSettingsNew, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Tutup Aplikasi", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }

                    // Footer construction icon row
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Construction, null, tint = Color(0xFF6B5235), modifier = Modifier.size(14.dp))
                        Text(
                            "Tim DLavie sedang bekerja — terima kasih atas kesabarannya.",
                            color = Color(0xFF6B5235),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
