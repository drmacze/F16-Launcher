package com.drmacze.f16launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.OpenInBrowser
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * NotificationBanner — slide-down banner displayed on top of MainShell when a
 * new push notification campaign arrives from Supabase.
 *
 * Behavior:
 *   - Auto-dismiss handled by caller via onDismiss (typically 5s after show).
 *   - Action types:
 *       "open_app"  → onAction() == onDismiss (just closes the banner)
 *       "open_url"  → caller opens URL in browser, then onDismiss
 *   - User can manually dismiss via the [X] close button.
 *
 * @param title      Campaign title
 * @param body       Campaign body
 * @param action     Either "open_app" or "open_url"
 * @param actionUrl  URL when action == "open_url", otherwise null/empty
 * @param onDismiss  Invoked when banner is closed
 * @param onAction   Invoked when CTA button is tapped (caller routes by action type)
 */
@Composable
fun NotificationBanner(
    title: String,
    body: String,
    action: String,
    actionUrl: String?,
    onDismiss: () -> Unit,
    onAction: () -> Unit
) {
    val isUrlAction = action == "open_url" && !actionUrl.isNullOrBlank()
    val ctaLabel = if (isUrlAction) "Buka Link" else "Tutup"

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec  = tween(380)
        ) + fadeIn(tween(280)) + expandVertically(tween(380)),
        exit  = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec  = tween(280)
        ) + fadeOut(tween(220)) + shrinkVertically(tween(280))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape  = RoundedCornerShape(20.dp),
            color  = Color(0xF00B1320),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    listOf(CandyCyan.copy(alpha = 0.45f), PremiumViolet.copy(alpha = 0.45f))
                )
            ),
            shadowElevation = 24.dp,
            tonalElevation  = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pulsing megaphone icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(CandyCyan, PremiumViolet)),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.Campaign,
                            contentDescription = "Notifikasi baru",
                            tint               = Color(0xFF00111D),
                            modifier           = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title.ifBlank { "Notifikasi DLavie" },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            body,
                            color = SoftText,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    // Manual close button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(SubText.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.Close,
                            contentDescription = "Tutup notifikasi",
                            tint               = SoftText,
                            modifier           = Modifier.size(16.dp)
                        )
                    }
                }

                // CTA button row — only show URL action as a button; "open_app" just needs the X
                if (isUrlAction) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick  = onAction,
                            modifier = Modifier.height(40.dp),
                            shape    = RoundedCornerShape(12.dp),
                            border   = BorderStroke(1.dp, CandyCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Rounded.OpenInBrowser, null, tint = CandyCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ctaLabel, color = CandyCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
