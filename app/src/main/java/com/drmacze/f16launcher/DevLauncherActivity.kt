package com.drmacze.f16launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

class DevLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Carbon,
                    surface = GlassBase,
                    primary = CandyCyan,
                    secondary = CandyBlue,
                    onPrimary = Color(0xFF00111D),
                    onSecondary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                val context = LocalContext.current
                val api = remember { CommunityApi(context) }
                Surface(Modifier.fillMaxSize(), color = Carbon) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Carbon, Color(0xFF071B2C), Carbon)))
                            .systemBarsPadding()
                    ) {
                        Phase41PersistentSetupShell(api)
                    }
                }
            }
        }
    }
}
