package com.drmacze.f16launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Internal launcher tools host.
 *
 * The recovery console remains the default surface. GameActionPanel can open the
 * launcher-owned FIFA 16 base-data installer through [EXTRA_BASE_DATA_INSTALLER]
 * so first-run resources never depend on the retired EA/Nimble bootstrap path.
 */
const val EXTRA_BASE_DATA_INSTALLER = "open_base_data_installer"

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
                val openBaseInstaller = intent?.getBooleanExtra(EXTRA_BASE_DATA_INSTALLER, false) == true

                Surface(Modifier.fillMaxSize(), color = Carbon) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Carbon, Color(0xFF071B2C), Carbon)))
                            .systemBarsPadding()
                    ) {
                        if (openBaseInstaller) {
                            val gameInstalled = remember {
                                try {
                                    context.packageManager.getPackageInfo(GAME_PKG_16, 0)
                                    true
                                } catch (_: Throwable) {
                                    false
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "FIFA 16 Base Data",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Install dan verifikasi DATA + OBB langsung dari DLavie Launcher. Jalur ini tidak memakai bootstrap server EA/Nimble lama.",
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )

                                DLavieBaseInstallCard(isGameInstalled = gameInstalled)

                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = { finish() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Kembali ke GameHub", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Phase50ProRecoveryShell(api)
                        }
                    }
                }
            }
        }
    }
}
