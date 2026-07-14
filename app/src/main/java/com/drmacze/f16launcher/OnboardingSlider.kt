package com.drmacze.f16launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * DLavie Onboarding Slider — 3 slides shown on Beranda (first open only).
 *
 * Slide 1: Beta Version — "Maaf jika ada error/bug, kami akan selalu memperbaiki"
 * Slide 2: Features — "Patch system, komunitas, auto-update"
 * Slide 3: Community — "Join komunitas DLavie"
 *
 * Theme: Pure monochrome — matches launcher theme (black bg, halftone, white text).
 * Dismissible: tap X or "Lewati" → saved to SharedPreferences (won't show again).
 */
@Composable
fun OnboardingSlider(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        // Halftone background overlay (matches launcher theme)
        MonochromeHalftoneBackground(
            modifier = Modifier.fillMaxSize(),
            showLogoParticles = true
        )

        // Close button (top-right)
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Tutup",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Pager content
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                pageSpacing = 32.dp
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        icon = Icons.Rounded.BugReport,
                        title = "Beta Version",
                        description = "Launcher DLavie masih dalam versi Beta.\n\nMaaf jika ada error atau bug. Kami akan selalu memperbaiki dan meningkatkan launcher agar lebih bagus, stabil, dan nyaman dipakai.\n\nTerima kasih atas pengertian Anda!"
                    )
                    1 -> OnboardingPage(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Patch System Baru",
                        description = "Update mod FIFA 16 langsung dari launcher — tanpa ZArchiver, tanpa file manager.\n\nCukup tap \"Update\" dan patch terbaru otomatis terpasang. Rollback instan kalau ada masalah."
                    )
                    2 -> OnboardingPage(
                        icon = Icons.Rounded.SportsEsports,
                        title = "Komunitas DLavie",
                        description = "Bergabung dengan komunitas DLavie — chat dengan sesama player, share tips, dan dapatkan update terbaru.\n\nMain FIFA 16 Mobile kini lebih mudah dan terhubung."
                    )
                }
            }

            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dot_$index"
                    )
                    Box(
                        Modifier
                            .size(width = width, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Action button
            val isLastPage = pagerState.currentPage == 2
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isLastPage) Color.White else Color.White.copy(alpha = 0.1f)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (isLastPage) {
                            onDismiss()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage + 1,
                                    animationSpec = tween(400)
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLastPage) "Mulai Main" else "Selanjutnya",
                    color = if (isLastPage) Color.Black else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily
                )
            }

            // Skip button
            if (!isLastPage) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Lewati",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon in circle
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Title
        Text(
            text = title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = InterFontFamily,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Description
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = InterFontFamily,
            textAlign = TextAlign.Center
        )
    }
}
