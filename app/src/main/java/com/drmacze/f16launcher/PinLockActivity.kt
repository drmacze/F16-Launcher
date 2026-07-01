package com.drmacze.f16launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Full-screen PIN lock activity.
 *
 * Launch modes:
 *  - MODE_UNLOCK  : ask for existing PIN to enter the app
 *  - MODE_SETUP   : set a new PIN (asks twice to confirm)
 *  - MODE_CHANGE  : change existing PIN (verify old, then set new twice)
 *  - MODE_DISABLE : verify PIN then disable
 */
class PinLockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_UNLOCK  = "unlock"
        const val MODE_SETUP   = "setup"
        const val MODE_CHANGE  = "change"
        const val MODE_DISABLE = "disable"

        const val RESULT_SUCCESS = 100
        const val RESULT_CANCEL  = 101

        fun launch(context: Context, mode: String) {
            context.startActivity(
                Intent(context, PinLockActivity::class.java)
                    .putExtra(EXTRA_MODE, mode)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_UNLOCK

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF040810), surface = Color(0xFF0C1422),
                primary = Color(0xFF27C8FF), onBackground = Color.White, onSurface = Color.White
            )) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF040810)) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color(0xFF06101E), Color(0xFF040810), Color(0xFF060D18)))
                        )
                    ) {
                        PinLockScreen(
                            mode = mode,
                            onVerified = {
                                setResult(RESULT_SUCCESS)
                                finish()
                            },
                            onPinSet = {
                                setResult(RESULT_SUCCESS)
                                finish()
                            },
                            onDisabled = {
                                setResult(RESULT_SUCCESS)
                                finish()
                            },
                            onCancel = {
                                setResult(RESULT_CANCEL)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    /** Block back press during unlock mode — user must enter PIN. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_UNLOCK
        if (mode == MODE_UNLOCK) {
            // Send to home instead of allowing back
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(home)
        } else {
            super.onBackPressed()
        }
    }
}

@Composable
private fun PinLockScreen(
    mode: String,
    onVerified: () -> Unit,
    onPinSet: () -> Unit,
    onDisabled: () -> Unit,
    onCancel: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var pin by remember { mutableStateOf("") }
    var stage by remember {
        mutableStateOf(when (mode) {
            PinLockActivity.MODE_CHANGE -> "verify_old"   // verify old PIN first
            PinLockActivity.MODE_SETUP -> "enter_new"     // enter new PIN
            else -> "verify"                              // unlock: verify
        })
    }
    var firstPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    var shake by remember { mutableStateOf(false) }
    val shakeOffset by animateFloatAsState(
        if (shake) 12f else 0f,
        animationSpec = tween(50),
        label = "shake"
    )

    fun submitPin() {
        when (stage) {
            "verify" -> {
                if (PinManager.verifyPin(ctx, pin)) {
                    success = true
                    onVerified()
                } else {
                    error = "PIN salah. Coba lagi."
                    shake = true
                    pin = ""
                }
            }
            "verify_old" -> {
                if (PinManager.verifyPin(ctx, pin)) {
                    stage = "enter_new"
                    error = ""
                    pin = ""
                } else {
                    error = "PIN lama salah."
                    shake = true
                    pin = ""
                }
            }
            "enter_new" -> {
                firstPin = pin
                stage = "confirm_new"
                error = ""
                pin = ""
            }
            "confirm_new" -> {
                if (pin == firstPin) {
                    if (PinManager.setupPin(ctx, pin)) {
                        success = true
                        onPinSet()
                    } else {
                        error = "Gagal menyimpan PIN. Coba lagi."
                        shake = true
                        pin = ""
                        firstPin = ""
                        stage = "enter_new"
                    }
                } else {
                    error = "PIN tidak cocok. Ulangi."
                    shake = true
                    pin = ""
                    firstPin = ""
                    stage = "enter_new"
                }
            }
            "disable_verify" -> {
                if (PinManager.verifyPin(ctx, pin)) {
                    PinManager.clearPin(ctx)
                    success = true
                    onDisabled()
                } else {
                    error = "PIN salah. Tidak bisa menonaktifkan."
                    shake = true
                    pin = ""
                }
            }
        }
        // Reset shake animation
        if (shake) {
            kotlinx.coroutines.GlobalScope.launch {
                delay(300)
                shake = false
            }
        }
    }

    val title = when (stage) {
        "verify"         -> if (mode == PinLockActivity.MODE_UNLOCK) "Masukkan PIN" else "Verifikasi PIN"
        "verify_old"     -> "PIN Lama"
        "enter_new"      -> "PIN Baru (6 digit)"
        "confirm_new"    -> "Konfirmasi PIN Baru"
        "disable_verify" -> "PIN untuk Nonaktifkan"
        else -> "PIN"
    }
    val subtitle = when (stage) {
        "verify"      -> "Untuk membuka DLavie Launcher"
        "verify_old"  -> "Masukkan PIN saat ini"
        "enter_new"   -> "PIN akan dipakai tiap membuka launcher"
        "confirm_new" -> "Ulangi PIN yang sama"
        "disable_verify" -> "PIN akan dihapus setelah verifikasi"
        else -> ""
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ── Header ──
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).background(
                    Brush.linearGradient(listOf(Color(0xFF27C8FF), Color(0xFF5F57FF))),
                    RoundedCornerShape(22.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("DLavie 26", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(title, color = Color(0xFF27C8FF), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = Color(0xFF8899B0), fontSize = 12.sp)
            }
        }

        // ── PIN dots (6) ──
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier.offset(x = shakeOffset.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                repeat(6) { i ->
                    val filled = i < pin.length
                    Box(
                        Modifier.size(16.dp).clip(CircleShape).background(
                            if (filled) Color(0xFF27C8FF) else Color.Transparent
                        ).border(
                            2.dp,
                            if (filled) Color(0xFF27C8FF) else Color(0xFF305D8DFF),
                            CircleShape
                        )
                    )
                }
            }
        }

        // ── Error message ──
        AnimatedContent(
            targetState = error,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "err"
        ) { err ->
            if (err.isNotEmpty()) {
                Text(err, color = Color(0xFFFF4D6D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Number pad ──
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keys = listOf("1","2","3","4","5","6","7","8","9","del","0","ok")
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { k ->
                        when (k) {
                            "del" -> KeypadCircle(
                                icon = Icons.Rounded.Backspace,
                                enabled = pin.isNotEmpty(),
                                onClick = { if (pin.isNotEmpty()) { pin = pin.dropLast(1); error = "" } }
                            )
                            "ok"  -> KeypadCircle(
                                icon = Icons.Rounded.Fingerprint,
                                enabled = pin.length == 6,
                                accent = true,
                                onClick = { if (pin.length == 6) submitPin() }
                            )
                            else  -> KeypadDigit(digit = k, onClick = {
                                if (pin.length < 6) { pin += k; error = "" }
                            })
                        }
                    }
                }
            }
        }

        // ── Cancel button (only in setup/change/disable modes) ──
        if (mode != PinLockActivity.MODE_UNLOCK) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Batal",
                color = Color(0xFF8899B0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCancel() }
            )
        }
    }
}

@Composable
private fun KeypadDigit(digit: String, onClick: () -> Unit) {
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(
            Color(0xFF0F1828)
        ).border(1.dp, Color(0xFF22324D), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(digit, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KeypadCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    accent: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (accent && enabled) Color(0xFF5F57FF) else Color(0xFF0F1828)
    val tint = if (accent && enabled) Color.White else if (enabled) Color(0xFF27C8FF) else Color(0xFF44566E)
    val border = if (accent && enabled) Color(0xFF5F57FF) else Color(0xFF22324D)
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
    }
}

// Helper extension for offset
private fun Modifier.offset(x: androidx.compose.ui.unit.Dp) = this.then(
    androidx.compose.foundation.layout.offset(x = x, y = 0.dp)
)
