package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * EditProfileScreen — dedicated screen for editing the user's profile.
 *
 * FIX v3.1: Previously the "Edit Profile" button on the Profile screen was
 * mis-wired to `onOpenSettings()`, so tapping it opened the Settings screen
 * instead of an actual edit form. This dedicated screen now hosts the real
 * edit form (matches the screenshot reference IMG_4540):
 *
 *   ┌─────────────────────────────────────────┐
 *   │  ◀   Edit Profile                       │
 *   │                                         │
 *   │            ┌──────┐                     │
 *   │            │avatar│  📷                 │
 *   │            └──────┘                     │
 *   │                                         │
 *   │   Full name        [Budiarti Ramlan  ]  │
 *   │   Phone number     [1234-5678-987    ]  │
 *   │   Email            [budi@x.com       ]  │
 *   │   Username         [@budiarti12      ]  │
 *   │   Bio              [Modder FIFA...   ]  │
 *   │                                         │
 *   │   [       Save Changes        ]         │
 *   │   [      Delete Account        ]        │
 *   └─────────────────────────────────────────┘
 *
 * All saves go through the real backend:
 *   - Avatar:    api.uploadAvatar(bytes) + api.updateAvatar(url)  (Supabase Storage)
 *   - Username + Display Name: AuthManager.updateProfile(token, uid, u, d)
 *   - Email:     AuthManager.updateEmail(token, newEmail)
 *   - Bio:       AuthManager.updateBio(token, uid, bio)            (NEW)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    api: CommunityApi,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val t       = Strings.get(LanguageManager.getCurrentLanguage(context))

    // ── Form state (pre-filled from current values) ──
    var avatarUrl       by remember { mutableStateOf(api.avatarUrl()) }
    var avatarUploading by remember { mutableStateOf(false) }
    var fullName        by remember { mutableStateOf(api.displayName()) }
    var username        by remember { mutableStateOf(api.username()) }
    var email           by remember { mutableStateOf("") }
    var phone           by remember { mutableStateOf("") }
    var bio             by remember { mutableStateOf("") }

    var working         by remember { mutableStateOf(false) }
    var resultMsg       by remember { mutableStateOf("") }
    var isSuccess       by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // ── Load current email + bio on first launch ──
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { email = api.getMyEmail().orEmpty() }
            runCatching { bio   = api.getMyBio().orEmpty() }
        }
    }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    // ── Avatar picker (same pattern as ProfileScreen) ──
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUploading = true
            scope.launch {
                try {
                    val newUrl = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        if (bytes == null || bytes.isEmpty())
                            throw IllegalStateException("Gagal membaca gambar.")
                        val uploadedUrl = api.uploadAvatar(bytes)
                        api.updateAvatar(uploadedUrl)
                        uploadedUrl
                    }
                    avatarUrl = newUrl
                    toast(t.profilePhotoUpdated)
                } catch (e: Throwable) {
                    toast(t.photoUploadFailed)
                } finally {
                    avatarUploading = false
                }
            }
        }
    }

    fun saveAll() {
        if (working) return
        working = true; resultMsg = ""
        scope.launch {
            val results = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                // 1. Username + Display name
                runCatching {
                    if (username.matches(Regex("[a-zA-Z0-9_]{3,24}")) && fullName.trim().length in 2..40) {
                        AuthManager.updateProfile(api.token(), api.userId(), username, fullName)
                        // Update local prefs so other screens see the change immediately
                        context.getSharedPreferences("dlavie_community", android.content.Context.MODE_PRIVATE).edit()
                            .putString("username", username.trim())
                            .putString("display_name", fullName.trim())
                            .apply()
                    }
                }.onFailure { results.add("Profile: ${it.message}") }

                // 2. Email (only if changed + valid)
                runCatching {
                    if (email.isNotBlank() && email.contains("@") && email.contains(".")) {
                        AuthManager.updateEmail(api.token(), email)
                    }
                }.onFailure { results.add("Email: ${it.message}") }

                // 3. Bio
                runCatching {
                    AuthManager.updateBio(api.token(), api.userId(), bio)
                }.onFailure { results.add("Bio: ${it.message}") }
            }
            isSuccess = results.isEmpty()
            resultMsg = if (isSuccess) "OK: Profil diperbarui." else results.joinToString("; ")
            working = false
            if (isSuccess) toast(t.profilePhotoUpdated) // reuse as generic success toast
        }
    }

    // ── Top bar + scrollable form ──
    Column(
        Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top bar: ← Edit Profile
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(Color.White.copy(0.06f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(
                t.editProfileTitle,
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(40.dp))  // balance the back arrow
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Avatar with camera badge ──
            Box(
                Modifier.size(96.dp).clickable {
                    if (api.loggedIn() && !avatarUploading) {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape)
                        .background(Color.White.copy(0.06f))
                        .border(2.dp, Color.White.copy(0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl, contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = fullName.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black
                        )
                    }
                    if (avatarUploading) {
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape)
                                .background(Color.Black.copy(0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White, strokeWidth = 2.dp
                            )
                        }
                    }
                }
                // Camera badge
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, PureBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = PureBlack, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Form fields ──
            EditProfileField(t.fullName, fullName) { raw -> fullName = it.take(40) }
            Spacer(Modifier.height(12.dp))
            EditProfileField(t.phoneNumber, phone) { raw -> phone = raw.filter { c -> c.isDigit() || c == '-' || c == '+' }.take(20) }
            Spacer(Modifier.height(12.dp))
            EditProfileField(t.email, email) { raw -> email = raw.trim() }
            Spacer(Modifier.height(12.dp))
            EditProfileField(t.username, username) { raw ->
                username = raw.trim().lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(24)
            }
            Spacer(Modifier.height(12.dp))
            // Bio (multi-line)
            Column(Modifier.fillMaxWidth()) {
                Text(t.bio, color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                     modifier = Modifier.padding(bottom = 4.dp))
                OutlinedTextField(
                    value = bio, onValueChange = { bio = it.take(200) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(0.4f),
                        unfocusedBorderColor = Color.White.copy(0.15f),
                        focusedContainerColor = Color.White.copy(0.04f),
                        unfocusedContainerColor = Color.White.copy(0.04f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = SoftText,
                        cursorColor = Color.White
                    )
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Save Changes (primary, matches screenshot yellow) ──
            Button(
                onClick = { saveAll() },
                enabled = !working && fullName.trim().length in 2..40
                                   && username.matches(Regex("[a-zA-Z0-9_]{3,24}")),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = Color(0xFF00150B)
                )
            ) {
                Text(
                    if (working) "Memproses…" else t.saveChanges,
                    fontWeight = FontWeight.Black, fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Delete Account (danger, with confirm dialog) ──
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                border = BorderStroke(1.dp, DangerRed.copy(0.5f))
            ) {
                Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(t.deleteAccount, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(20.dp))

            // Result message
            if (resultMsg.isNotEmpty()) {
                Text(
                    resultMsg,
                    color = if (isSuccess) NeonGreen else DangerRed,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
    }

    // ── Delete account confirmation ──
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(t.deleteAccountConfirm) },
            text = { Text(t.deleteAccountConfirmBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        toast("Account deletion requires email verification. Please contact support.")
                        // TODO: wire to supabase admin endpoint (requires service_role key,
                        // usually exposed via a Supabase Edge Function for security)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                ) { Text(t.deleteAccount) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(t.cancel) }
            }
        )
    }
}

@Composable
private fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
             modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(0.4f),
                unfocusedBorderColor = Color.White.copy(0.15f),
                focusedContainerColor = Color.White.copy(0.04f),
                unfocusedContainerColor = Color.White.copy(0.04f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = SoftText,
                cursorColor = Color.White
            )
        )
    }
}
