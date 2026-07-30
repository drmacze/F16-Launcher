package com.drmacze.f16launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Entry splash for DLavie Launcher.
 *
 * v331 replaces the decorative four-second splash with a short, functional
 * startup sequence. The screen communicates what the launcher is preparing,
 * avoids technical copy, and transitions as soon as the local startup steps
 * are complete.
 */
class ShinySplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Portal links never carry session credentials.
        val portalData = intent?.data
        if (portalData != null && portalData.scheme == "dlavie" && portalData.host == "connect") {
            val legacySecrets = PortalAuthSecurity.containsLegacyPortalSecrets(portalData)
            if (legacySecrets) {
                android.util.Log.w("DLaviePortal", "Rejected legacy connect link containing credentials")
                android.widget.Toast.makeText(
                    this,
                    "Tautan login lama ditolak demi keamanan. Silakan login langsung di launcher.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

            val target = Intent(this, DLavieGuidedActivity::class.java).apply {
                putExtra("portal_login_requested", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(target)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
            return
        }

        // Game sharing deep link: dlavie://game?pkg=PACKAGE_NAME
        if (portalData != null && portalData.scheme == "dlavie" && portalData.host == "game") {
            val gamePkg = portalData.getQueryParameter("pkg")
            if (!gamePkg.isNullOrBlank()) {
                getSharedPreferences("dlavie_deep_link", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString("target_game_pkg", gamePkg)
                    .putLong("deep_link_timestamp", System.currentTimeMillis())
                    .apply()
                android.util.Log.i("DLavie", "Deep link: open game $gamePkg")
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = StartupBackground,
                    surface = StartupBackground,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = StartupBackground) {
                    DLavieStartupScreen(onFinished = ::launchDestination)
                }
            }
        }
    }

    private fun launchDestination() {
        val prefs = getSharedPreferences("dlavie_auth_session", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", null)
        val communityPrefs = getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE)
        val isGuest = communityPrefs.getBoolean("is_guest", false)

        val target = if (!token.isNullOrBlank() && PortalAuthSecurity.isJwtUsable(token)) {
            val refresh = prefs.getString("refresh_token", "") ?: ""
            val userId = try {
                val payload = token.split(".").getOrNull(1) ?: ""
                val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
                val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
                org.json.JSONObject(String(decoded)).optString("sub", "")
            } catch (_: Exception) {
                ""
            }

            communityPrefs.edit()
                .putString("access_token", token)
                .putString("refresh_token", refresh)
                .putString("user_id", userId)
                .apply()

            Intent(this, ModernLauncherActivity::class.java)
        } else {
            if (!token.isNullOrBlank()) PortalAuthSecurity.clearSession(this)
            if (isGuest) {
                Intent(this, ModernLauncherActivity::class.java)
            } else {
                Intent(this, DLavieGuidedActivity::class.java)
            }
        }

        target.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(target)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val data = intent.data
        if (data != null && data.scheme == "dlavie" && data.host == "connect") {
            recreate()
        }
    }
}

private val StartupBackground = Color(0xFF080908)
private val StartupAccent = Color(0xFFE5484D)

@Composable
private fun DLavieStartupScreen(onFinished: () -> Unit) {
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.975f) }
    val progress = remember { Animatable(0f) }
    var status by remember { mutableStateOf("Memeriksa sesi") }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { contentAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
            launch { contentScale.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        }

        progress.animateTo(0.28f, tween(260, easing = FastOutSlowInEasing))
        status = "Menyiapkan layanan"
        progress.animateTo(0.72f, tween(420, easing = FastOutSlowInEasing))
        status = "Menyiapkan antarmuka"
        progress.animateTo(0.94f, tween(300, easing = FastOutSlowInEasing))
        status = "Siap"
        progress.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
        delay(120)
        contentAlpha.animateTo(0f, tween(170, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF171917), StartupBackground),
                    radius = 1050f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(contentAlpha.value)
                .scale(contentScale.value)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.045f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "DL",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 15.dp)
                            .width(24.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(StartupAccent)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "DLavie",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.7).sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "MOBILE LAUNCHER",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.6.sp
            )

            Spacer(Modifier.height(52.dp))

            Text(
                text = status,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(13.dp))

            Box(
                modifier = Modifier
                    .width(196.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White)
                )
            }
        }

        Text(
            text = "v${BuildConfig.VERSION_NAME}  •  Build ${BuildConfig.VERSION_CODE}",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(contentAlpha.value),
            color = Color.White.copy(alpha = 0.30f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.7.sp
        )
    }
}
