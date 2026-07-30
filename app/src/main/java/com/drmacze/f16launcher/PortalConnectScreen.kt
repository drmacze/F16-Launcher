package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PortalSurface = Color(0xFF0D0D0D)
private val PortalSurfaceRaised = Color(0xFF121212)
private val PortalBorder = Color(0x1AFFFFFF)
private val PortalText = Color.White
private val PortalMuted = Color(0xFF929292)
private val PortalDim = Color(0xFF626262)
private val PortalRed = Color(0xFFFF3B3B)
private val PortalGreen = Color(0xFF35D07F)

private enum class PortalUpdatePhase {
    IDLE,
    DOWNLOADING,
    INSTALLING,
    ERROR,
}

/**
 * Focused pre-auth experience used when the launcher has not been connected to
 * a DLavie Portal account. Update and account connection are deliberately kept
 * as two clear, sequential actions.
 */
@Composable
internal fun ProfessionalPortalConnectContent(
    onOpenPortal: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var checked by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var updatePhase by remember { mutableStateOf(PortalUpdatePhase.IDLE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var updateError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        updateInfo = withContext(Dispatchers.IO) {
            AppUpdateChecker.checkForUpdate(context = context)
        }
        checked = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            !checked -> PortalVersionStatus(checking = true)
            updateInfo != null -> PortalUpdateCard(
                info = updateInfo!!,
                phase = updatePhase,
                progress = progress,
                error = updateError,
                onUpdate = {
                    if (updatePhase == PortalUpdatePhase.DOWNLOADING ||
                        updatePhase == PortalUpdatePhase.INSTALLING
                    ) return@PortalUpdateCard

                    updatePhase = PortalUpdatePhase.DOWNLOADING
                    progress = 0f
                    updateError = ""
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                AppUpdateChecker.downloadApk(context, updateInfo!!.apkUrl) { value ->
                                    mainHandler.post { progress = value.coerceIn(0f, 1f) }
                                }
                            }
                        }

                        val apk = result.getOrNull()
                        if (apk == null || !apk.exists() || apk.length() <= 1_000_000L) {
                            updatePhase = PortalUpdatePhase.ERROR
                            updateError = result.exceptionOrNull()?.message
                                ?: "Unduhan tidak dapat diselesaikan."
                            return@launch
                        }

                        progress = 1f
                        updatePhase = PortalUpdatePhase.INSTALLING
                        delay(350)
                        if (!AppUpdateChecker.installApk(context, apk)) {
                            updatePhase = PortalUpdatePhase.ERROR
                            updateError = "Installer Android tidak dapat dibuka."
                        }
                    }
                },
                onOpenWebsite = {
                    openTrustedUrl(context, updateInfo!!.websiteUrl)
                },
            )
            else -> PortalVersionStatus(checking = false)
        }

        PortalAccountCard(
            enabled = checked && updateInfo == null,
            updateRequired = updateInfo != null,
            onOpenPortal = onOpenPortal,
        )
    }
}

@Composable
private fun PortalVersionStatus(checking: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFFBDBDBD),
                    strokeWidth = 1.8.dp,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(PortalGreen, CircleShape),
                )
            }
            Column {
                Text(
                    text = if (checking) "Memeriksa launcher" else "Launcher siap digunakan",
                    color = PortalText.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (checking) {
                        "Mengambil status versi terbaru…"
                    } else {
                        "${BuildConfig.VERSION_NAME}  •  Build ${BuildConfig.VERSION_CODE}"
                    },
                    color = PortalDim,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun PortalUpdateCard(
    info: AppUpdateChecker.UpdateInfo,
    phase: PortalUpdatePhase,
    progress: Float,
    error: String,
    onUpdate: () -> Unit,
    onOpenWebsite: () -> Unit,
) {
    val busy = phase == PortalUpdatePhase.DOWNLOADING || phase == PortalUpdatePhase.INSTALLING
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF120D0D),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PortalRed.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = PortalRed.copy(alpha = 0.12f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = PortalRed,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PEMBARUAN DIPERLUKAN",
                        color = PortalRed.copy(alpha = 0.92f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Launcher ${info.versionName}",
                        color = PortalText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append("Build ${info.versionCode}")
                            if (info.apkSizeMb.isNotBlank()) append("  •  ${info.apkSizeMb}")
                        },
                        color = PortalMuted,
                        fontSize = 11.sp,
                    )
                }
            }

            Text(
                text = "Perbarui launcher terlebih dahulu, lalu hubungkan akun Portal Anda.",
                color = PortalMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )

            if (busy) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    LinearProgressIndicator(
                        progress = { if (phase == PortalUpdatePhase.INSTALLING) 1f else progress },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = PortalRed,
                        trackColor = Color.White.copy(alpha = 0.08f),
                    )
                    Text(
                        text = if (phase == PortalUpdatePhase.INSTALLING) {
                            "Membuka installer…"
                        } else {
                            "Mengunduh ${(progress * 100).toInt()}%"
                        },
                        color = PortalMuted,
                        fontSize = 10.sp,
                    )
                }
            }

            if (phase == PortalUpdatePhase.ERROR && error.isNotBlank()) {
                Text(
                    text = error,
                    color = Color(0xFFFFA36C),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }

            Button(
                onClick = onUpdate,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PortalRed,
                    contentColor = Color.White,
                    disabledContainerColor = PortalRed.copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.65f),
                ),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(9.dp))
                }
                Text(
                    text = when (phase) {
                        PortalUpdatePhase.DOWNLOADING -> "Mengunduh…"
                        PortalUpdatePhase.INSTALLING -> "Menyiapkan installer…"
                        PortalUpdatePhase.ERROR -> "Coba Lagi"
                        PortalUpdatePhase.IDLE -> "Perbarui Launcher"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (phase == PortalUpdatePhase.ERROR) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onOpenWebsite)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "Buka halaman download",
                        color = PortalMuted,
                        fontSize = 10.sp,
                        textDecoration = TextDecoration.Underline,
                    )
                    Icon(
                        Icons.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = PortalMuted,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PortalAccountCard(
    enabled: Boolean,
    updateRequired: Boolean,
    onOpenPortal: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PortalSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, PortalBorder),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = PortalSurfaceRaised,
                    border = BorderStroke(1.dp, PortalBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Public,
                            contentDescription = null,
                            tint = PortalText,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AKUN DLAVIE",
                        color = PortalDim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Hubungkan ke Portal",
                        color = PortalText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                text = if (updateRequired) {
                    "Selesaikan pembaruan launcher untuk melanjutkan koneksi akun."
                } else {
                    "Masuk melalui browser. Setelah disetujui, akun yang sama akan terhubung otomatis ke launcher."
                },
                color = PortalMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )

            Button(
                onClick = onOpenPortal,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF242424),
                    disabledContentColor = Color(0xFF707070),
                ),
            ) {
                Icon(
                    Icons.Rounded.Public,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(9.dp))
                Text(
                    text = if (updateRequired) "Perbarui launcher terlebih dahulu" else "Hubungkan ke Portal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = PortalDim,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Koneksi aman • tanpa menyalin token",
                    color = PortalDim,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun openTrustedUrl(context: Context, url: String) {
    val safe = url.takeIf {
        runCatching {
            val uri = Uri.parse(it)
            uri.scheme == "https" && uri.host == "drmacze.github.io"
        }.getOrDefault(false)
    } ?: AppUpdateChecker.DLAVIE_WEBSITE_URL

    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(safe))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
