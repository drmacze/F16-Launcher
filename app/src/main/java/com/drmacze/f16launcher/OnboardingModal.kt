package com.drmacze.f16launcher

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v7.9.17: OnboardingModal — post-login onboarding (Mobbin-style).
 *
 * Ditampilkan setelah user login/register PERTAMA kali (jika user_type masih kosong).
 * User isi: user_type (player/modder/explorer), use_case (playing/personal/community),
 * android_version (numeric only), country (pindah dari register form).
 *
 * Setelah submit, data disimpan ke profiles table. User tidak perlu isi lagi
 * di login berikutnya (cek user_type != null → skip onboarding).
 *
 * Design: dark themed modal overlay, minimalis, Mobbin-inspired.
 */
@Composable
fun OnboardingModal(api: CommunityApi, onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }  // 0=welcome, 1=describe, 2=use_case, 3=more_info
    var userType by remember { mutableStateOf("") }
    var useCase by remember { mutableStateOf("") }
    var androidVersion by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(api.country().ifBlank { DEFAULT_COUNTRY }) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { /* prevent dismiss */ }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0A0A0A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo + heading
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SportsEsports, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    when (step) {
                        0 -> "Selamat Datang di DLavie"
                        1 -> "Apa yang menggambarkanmu?"
                        2 -> "Untuk apa kamu akan menggunakan DLavie?"
                        else -> "Informasi Tambahan"
                    },
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (step) {
                        0 -> "Mari personalisasi pengalamanmu. Hanya butuh 30 detik."
                        1 -> "Pilih peran yang paling sesuai denganmu."
                        2 -> "Ini membantu kami memberikan pengalaman yang lebih baik."
                        else -> "Lengkapi informasi berikut. Hanya diisi sekali."
                    },
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                // Step content
                when (step) {
                    0 -> {
                        // Welcome step — continue button
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Mulai", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                    1 -> {
                        // What describes you? (player, modder, explorer)
                        OnboardingOptionCard(
                            icon = Icons.Rounded.SportsEsports,
                            title = "Player",
                            subtitle = "Saya main game FIFA",
                            selected = userType == "player",
                            onClick = { userType = "player" }
                        )
                        Spacer(Modifier.height(10.dp))
                        OnboardingOptionCard(
                            icon = Icons.Rounded.Build,
                            title = "Modder",
                            subtitle = "Saya buat/maintain mod game",
                            selected = userType == "modder",
                            onClick = { userType = "modder" }
                        )
                        Spacer(Modifier.height(10.dp))
                        OnboardingOptionCard(
                            icon = Icons.Rounded.TravelExplore,
                            title = "Explorer",
                            subtitle = "Saya jelajahi komunitas & fitur",
                            selected = userType == "explorer",
                            onClick = { userType = "explorer" }
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { step = 2 },
                            enabled = userType.isNotBlank() && !submitting,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userType.isNotBlank()) Color.White else Color.White.copy(alpha = 0.2f),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Lanjut", fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                        }
                    }
                    2 -> {
                        // What will you use? (playing, personal, community)
                        OnboardingOptionCard(
                            icon = Icons.Rounded.PlayCircle,
                            title = "Playing",
                            subtitle = "Main game FIFA",
                            selected = useCase == "playing",
                            onClick = { useCase = "playing" }
                        )
                        Spacer(Modifier.height(10.dp))
                        OnboardingOptionCard(
                            icon = Icons.Rounded.Person,
                            title = "Personal",
                            subtitle = "Penggunaan pribadi",
                            selected = useCase == "personal",
                            onClick = { useCase = "personal" }
                        )
                        Spacer(Modifier.height(10.dp))
                        OnboardingOptionCard(
                            icon = Icons.Rounded.Forum,
                            title = "Community",
                            subtitle = "Berinteraksi di komunitas",
                            selected = useCase == "community",
                            onClick = { useCase = "community" }
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Back button
                            Surface(
                                Modifier.weight(1f).height(52.dp).clickable { step = 1 },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Kembali", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                }
                            }
                            // Next button
                            Button(
                                onClick = { step = 3 },
                                enabled = useCase.isNotBlank() && !submitting,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (useCase.isNotBlank()) Color.White else Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Lanjut", fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                            }
                        }
                    }
                    3 -> {
                        // More information: Android version (numeric) + Country
                        Text(
                            "Android Version",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = androidVersion,
                            onValueChange = { input ->
                                // v7.9.17: Hanya terima angka + titik (numeric keyboard)
                                val filtered = input.filter { it.isDigit() || it == '.' }.take(10)
                                androidVersion = filtered
                            },
                            placeholder = { Text("Contoh: 16", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp, fontFamily = InterFontFamily) },
                            leadingIcon = { Icon(Icons.Rounded.Android, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White,
                                focusedIndicatorColor = Color.White.copy(alpha = 0.3f),
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Negara",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        // Country picker (reuse existing from GuidedActivity)
                        OnboardingCountryPicker(selected = country, onSelect = { country = it })
                        Spacer(Modifier.height(8.dp))
                        if (error.isNotBlank()) {
                            Text(error, color = Color(0xFFFF5252), fontSize = 11.sp, fontFamily = InterFontFamily, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                Modifier.weight(1f).height(52.dp).clickable { step = 2 },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Kembali", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                }
                            }
                            Button(
                                onClick = {
                                    if (androidVersion.isBlank()) {
                                        error = "Mohon isi versi Android"
                                        return@Button
                                    }
                                    submitting = true
                                    error = ""
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                api.updateUserType(userType)
                                                api.updateUseCase(useCase)
                                                api.updateAndroidVersion(androidVersion)
                                                api.updateCountry(country)
                                                // Cache ke prefs
                                                api.setUserType(userType)
                                                api.setUseCase(useCase)
                                                api.setAndroidVersion(androidVersion)
                                            }
                                            submitting = false
                                            onComplete()
                                        } catch (e: Exception) {
                                            submitting = false
                                            error = "Gagal simpan: ${e.message}"
                                        }
                                    }
                                },
                                enabled = !submitting,
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                if (submitting) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Selesai", fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                                }
                            }
                        }
                    }
                }

                // Progress dots
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { index ->
                        Box(
                            Modifier.size(if (index == step) 20.dp else 6.dp, 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (index == step) Color.White else Color.White.copy(alpha = 0.2f))
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Kami tidak akan membagikan informasi Anda kepada siapa pun.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun OnboardingOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (selected) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (selected) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontFamily = InterFontFamily)
            }
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun OnboardingCountryPicker(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Public, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    selected.ifBlank { "Pilih negara" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A1A1A))
        ) {
            COUNTRY_LIST.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name, color = Color.White, fontSize = 14.sp, fontFamily = InterFontFamily) },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    }
                )
            }
        }
    }
}
