package com.drmacze.f16launcher

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * DLavie Floating ChatBot
 *
 * Features:
 * - Floating button (bottom-right, always visible)
 * - Tap → menu: Report | Assistant
 * - Report: form (Android version, complaint, feedback, screenshot)
 * - Live chat: real-time messaging with admin, photo attachment
 * - Auto-close: 5 min inactivity → bot sends farewell message → close
 * - Manual close button
 * - Offline detection: if admin offline → bot sends "tim kami offline" message
 * - "Chat langsung dengan developer" text
 */
@Composable
fun FloatingChatBot(api: CommunityApi) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf<String?>(null) } // null = menu, "report", "live_chat", "assistant"

    if (!expanded) {
        // Floating button
        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        expanded = true
                        mode = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ChatBubble,
                    contentDescription = "DLavie Assistant",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    } else {
        when (mode) {
            null -> ChatBotMenu(
                onSelect = { selected ->
                    mode = selected
                },
                onClose = { expanded = false }
            )
            "report" -> ReportForm(
                api = api,
                onBack = { mode = null },
                onClose = { expanded = false; mode = null }
            )
            "live_chat" -> LiveChatScreen(
                api = api,
                onBack = { mode = null },
                onClose = { expanded = false; mode = null }
            )
            "assistant" -> LiveChatScreen(
                api = api,
                isAssistant = true,
                onBack = { mode = null },
                onClose = { expanded = false; mode = null }
            )
        }
    }
}

// ─── Menu ───────────────────────────────────────────────────────────────────

@Composable
private fun ChatBotMenu(onSelect: (String) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureBlack,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    Modifier.size(64.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ChatBubble, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("DLavie Assistant", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)
                Text("Chat langsung dengan developer", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = InterFontFamily)
                Spacer(Modifier.height(24.dp))

                // Report button
                ChatBotMenuItem(
                    icon = Icons.Rounded.BugReport,
                    title = "Report",
                    subtitle = "Laporkan bug, error, atau keluhan",
                    onClick = { onSelect("report") }
                )
                Spacer(Modifier.height(12.dp))

                // Live Chat button
                ChatBotMenuItem(
                    icon = Icons.Rounded.SupportAgent,
                    title = "Live Chat",
                    subtitle = "Chat langsung dengan tim developer",
                    onClick = { onSelect("live_chat") }
                )
                Spacer(Modifier.height(12.dp))

                // Assistant button
                ChatBotMenuItem(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Assistant",
                    subtitle = "Tanya AI tentang DLavie",
                    onClick = { onSelect("assistant") }
                )

                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onClose) {
                    Text("Tutup", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ChatBotMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontFamily = InterFontFamily)
            }
        }
    }
}

// ─── Report Form ────────────────────────────────────────────────────────────

@Composable
private fun ReportForm(api: CommunityApi, onBack: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var androidVersion by remember { mutableStateOf(android.os.Build.VERSION.RELEASE ?: "") }
    var complaint by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf<Uri?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> screenshotUri = uri }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureBlack,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp).clickable { onBack() })
                    Spacer(Modifier.width(12.dp))
                    Text("Report", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
                Spacer(Modifier.height(4.dp))
                Text("Chat langsung dengan developer", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontFamily = InterFontFamily)
                Spacer(Modifier.height(20.dp))

                if (submitted) {
                    // Success state
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Report Terkirim!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(8.dp))
                            Text("Tim kami akan meninjau laporan Anda. Terima kasih!", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(24.dp))
                            Surface(
                                Modifier.clickable { onClose() },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White
                            ) {
                                Text("Tutup", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                            }
                        }
                    }
                } else {
                    // Form fields
                    Text("Versi Android", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(androidVersion, color = Color.White, fontSize = 14.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.height(16.dp))

                    Text("Keluhan / Masalah", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        TextField(
                            value = complaint,
                            onValueChange = { complaint = it },
                            placeholder = { Text("Jelaskan masalah yang Anda alami...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    Text("Feedback / Harapan Anda", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        TextField(
                            value = feedback,
                            onValueChange = { feedback = it },
                            placeholder = { Text("Apa yang Anda inginkan agar DLavie lebih baik?", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // Screenshot upload
                    Text("Screenshot Bug/Error (opsional)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        Modifier.fillMaxWidth().clickable { imagePicker.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.AttachFile, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (screenshotUri != null) "Screenshot dipilih ✓" else "Pilih screenshot",
                                color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontFamily = InterFontFamily
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // Submit button
                    Surface(
                        Modifier.fillMaxWidth().clickable(enabled = !submitting && complaint.isNotBlank()) {
                            submitting = true
                            scope.launch {
                                submitReport(api, androidVersion, complaint, feedback, screenshotUri, context)
                                submitting = false
                                submitted = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (complaint.isNotBlank()) Color.White else Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            if (submitting) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Kirim Report", color = if (complaint.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.3f), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Live Chat ──────────────────────────────────────────────────────────────

@Composable
private fun LiveChatScreen(api: CommunityApi, isAssistant: Boolean = false, onBack: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var ticketId by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var adminOnline by remember { mutableStateOf(false) }
    var lastActivityTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var inactivityWarningSent by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Image picker for attachments
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && ticketId != null) {
            scope.launch {
                sending = true
                uploadAndSendImage(api, ticketId!!, uri, context)
                sending = false
                lastActivityTime = System.currentTimeMillis()
            }
        }
    }

    // Initialize: create ticket + check admin online status
    LaunchedEffect(Unit) {
        if (!isAssistant) {
            // Check if admin is online (active within 5 min)
            adminOnline = checkAdminOnline(api)
            if (!adminOnline) {
                messages = messages + ChatMessage(
                    senderType = "bot",
                    body = "Saat ini tim kami offline. Mohon tunggu sampai tim kami merespon. 🙏",
                    timestamp = System.currentTimeMillis()
                )
            }
        } else {
            // Assistant mode
            messages = messages + ChatMessage(
                senderType = "bot",
                body = "Halo! Saya DLavie Assistant. Saya bisa membantu seputar DLavie Launcher dan FIFA 16. Apa yang bisa saya bantu?",
                timestamp = System.currentTimeMillis()
            )
        }

        // Create ticket
        ticketId = createTicket(api, if (isAssistant) "assistant" else "live_chat")
        lastActivityTime = System.currentTimeMillis()
    }

    // Poll for new messages (every 5 seconds for live chat)
    LaunchedEffect(ticketId) {
        if (ticketId != null && !isAssistant) {
            while (true) {
                delay(5000)
                val newMsgs = pollMessages(api, ticketId!!)
                if (newMsgs.isNotEmpty()) {
                    messages = (messages + newMsgs).distinctBy { it.id }
                    lastActivityTime = System.currentTimeMillis()
                    inactivityWarningSent = false
                }

                // Auto-close check: 5 min inactivity
                val idleTime = System.currentTimeMillis() - lastActivityTime
                if (idleTime > 5 * 60 * 1000 && !inactivityWarningSent) {
                    inactivityWarningSent = true
                    messages = messages + ChatMessage(
                        senderType = "bot",
                        body = "Apakah kamu masih disini? jika tidak, live chat akan kami tutup. Terima kasih atas masukan dan bantuanmu agar DLavie tetap berkembang 🙏",
                        timestamp = System.currentTimeMillis()
                    )
                    // Auto-close after 30 more seconds
                    delay(30000)
                    closeTicket(api, ticketId!!)
                    onClose()
                    break
                }
            }
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureBlack,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                // Header
                Surface(
                    Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp).clickable { onBack() })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isAssistant) "DLavie Assistant" else "Live Chat",
                                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                            )
                            Text("Chat langsung dengan developer", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = InterFontFamily)
                        }
                        // Close session button
                        Icon(
                            Icons.Rounded.Close,
                            null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp).clickable {
                                ticketId?.let { closeTicket(api, it) }
                                onClose()
                            }
                        )
                    }
                }

                // Messages list
                LazyColumn(
                    Modifier.weight(1f).padding(horizontal = 12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }
                }

                // Input bar
                Surface(
                    Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Row(
                        Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment icon
                        Icon(
                            Icons.Rounded.AttachFile,
                            null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp).clickable {
                                imagePicker.launch("image/*")
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        // Text input
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ketik pesan...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 3
                        )
                        Spacer(Modifier.width(8.dp))
                        // Send button
                        Surface(
                            Modifier.size(36.dp).clickable(enabled = inputText.isNotBlank() && !sending) {
                                if (inputText.isNotBlank() && ticketId != null) {
                                    val text = inputText
                                    inputText = ""
                                    sending = true
                                    scope.launch {
                                        sendMessage(api, ticketId!!, text)
                                        messages = messages + ChatMessage(
                                            id = UUID.randomUUID().toString(),
                                            senderType = "user",
                                            body = text,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        lastActivityTime = System.currentTimeMillis()
                                        inactivityWarningSent = false

                                        // If assistant mode → call AI Edge Function
                                        if (isAssistant) {
                                            val aiResponse = callAIAssistant(text, messages.map { mapOf("role" to it.senderType, "text" to it.body) })
                                            messages = messages + ChatMessage(
                                                id = UUID.randomUUID().toString(),
                                                senderType = "bot",
                                                body = aiResponse,
                                                timestamp = System.currentTimeMillis()
                                            )
                                        }

                                        sending = false
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = if (inputText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Chat Bubble ────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.senderType == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = when (msg.senderType) {
        "user" -> Color.White
        "bot" -> Color.White.copy(alpha = 0.1f)
        else -> Color.White.copy(alpha = 0.05f)
    }
    val textColor = if (isUser) Color.Black else Color.White

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = bgColor
        ) {
            Column(Modifier.padding(12.dp)) {
                if (msg.imageUrll != null) {
                    // TODO: show image
                }
                if (msg.body.isNotBlank()) {
                    Text(msg.body, color = textColor, fontSize = 13.sp, fontFamily = InterFontFamily)
                }
            }
        }
        Text(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 9.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// ─── Data Class ─────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderType: String, // "user", "admin", "bot"
    val body: String = "",
    val imageUrll: String? = null,
    val timestamp: Long
)

// ─── API Helpers ────────────────────────────────────────────────────────────

private suspend fun createTicket(api: CommunityApi, category: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("user_id", api.userId())
                put("category", category)
                put("status", "open")
                put("android_version", android.os.Build.VERSION.RELEASE ?: "")
            }
            val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${api.token()}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=representation")
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            if (code in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(response)
                if (arr.length() > 0) {
                    return@withContext arr.getJSONObject(0).optString("id")
                }
            }
            conn.disconnect()
            null
        } catch (e: Exception) {
            Log.e("DLavieChat", "createTicket failed", e)
            null
        }
    }
}

private suspend fun submitReport(
    api: CommunityApi,
    androidVersion: String,
    complaint: String,
    feedback: String,
    screenshotUri: Uri?,
    context: Context
) {
    withContext(Dispatchers.IO) {
        try {
            var screenshotUrl: String? = null
            // Upload screenshot if provided
            if (screenshotUri != null) {
                screenshotUrl = uploadImageToSupabase(api, screenshotUri, context)
            }

            val payload = JSONObject().apply {
                put("user_id", api.userId())
                put("category", "report")
                put("status", "open")
                put("android_version", androidVersion)
                put("complaint", complaint)
                put("user_feedback", feedback)
                if (screenshotUrl != null) put("screenshot_url", screenshotUrl)
            }
            val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${api.token()}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("DLavieChat", "submitReport failed", e)
        }
    }
}

private suspend fun sendMessage(api: CommunityApi, ticketId: String, body: String) {
    withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("ticket_id", ticketId)
                put("sender_type", "user")
                put("sender_id", api.userId())
                put("body", body)
            }
            val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/ticket_messages").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${api.token()}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            conn.responseCode
            conn.disconnect()

            // Update last_activity_at on ticket
            val updateConn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets?id=eq.$ticketId").openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${api.token()}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
            }
            val updatePayload = JSONObject().apply {
                put("last_activity_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()))
            }
            updateConn.outputStream.use { it.write(updatePayload.toString().toByteArray()) }
            updateConn.responseCode
            updateConn.disconnect()
        } catch (e: Exception) {
            Log.e("DLavieChat", "sendMessage failed", e)
        }
    }
}

private suspend fun uploadAndSendImage(api: CommunityApi, ticketId: String, uri: Uri, context: Context) {
    withContext(Dispatchers.IO) {
        try {
            val imageUrl = uploadImageToSupabase(api, uri, context)
            if (imageUrl != null) {
                val payload = JSONObject().apply {
                    put("ticket_id", ticketId)
                    put("sender_type", "user")
                    put("sender_id", api.userId())
                    put("image_url", imageUrl)
                }
                val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/ticket_messages").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${api.token()}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                conn.responseCode
                conn.disconnect()
            } else {
                Log.w("DLavieChat", "Image upload returned null — skipping message send")
            }
        } catch (e: Exception) {
            Log.e("DLavieChat", "uploadAndSendImage failed", e)
        }
    }
}

private fun uploadImageToSupabase(api: CommunityApi, uri: Uri, context: Context): String? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val imageData = inputStream.readBytes()
        inputStream.close()

        val fileName = "ticket_${System.currentTimeMillis()}.jpg"
        val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/storage/v1/object/community-images/$fileName").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
            setRequestProperty("Content-Type", "image/jpeg")
        }
        conn.outputStream.use { it.write(imageData) }
        val code = conn.responseCode
        conn.disconnect()

        if (code in 200..299) {
            return "https://lvmucsxbmadtsgrxuwmo.supabase.co/storage/v1/object/public/community-images/$fileName"
        }
    } catch (e: Exception) {
        Log.e("DLavieChat", "uploadImage failed", e)
    }
    return null
}

private suspend fun pollMessages(api: CommunityApi, ticketId: String): List<ChatMessage> {
    return withContext(Dispatchers.IO) {
        try {
            val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/ticket_messages?ticket_id=eq.$ticketId&sender_type=neq.user&order=created_at.asc").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${api.token()}")
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                val result = mutableListOf<ChatMessage>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    result.add(ChatMessage(
                        id = obj.optString("id"),
                        senderType = obj.optString("sender_type"),
                        body = obj.optString("body", ""),
                        imageUrll = obj.optString("image_url", "").ifEmpty { null },
                        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(obj.optString("created_at"))?.time ?: System.currentTimeMillis()
                    ))
                }
                conn.disconnect()
                return@withContext result
            }
            conn.disconnect()
            emptyList()
        } catch (e: Exception) {
            Log.e("DLavieChat", "pollMessages failed", e)
            emptyList()
        }
    }
}

private fun checkAdminOnline(api: CommunityApi): Boolean {
    return try {
        val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/rpc/is_admin_online").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
            setRequestProperty("Content-Type", "application/json")
        }
        conn.outputStream.use { it.write("{}".toByteArray()) }
        val code = conn.responseCode
        if (code in 200..299) {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            text.contains("true", ignoreCase = true)
        } else {
            conn.disconnect()
            false
        }
    } catch (e: Exception) {
        Log.e("DLavieChat", "checkAdminOnline failed", e)
        false
    }
}

private fun closeTicket(api: CommunityApi, ticketId: String) {
    try {
        val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets?id=eq.$ticketId").openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }
        val payload = JSONObject().apply {
            put("status", "closed")
            put("closed_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()))
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray()) }
        conn.responseCode
        conn.disconnect()
    } catch (e: Exception) {
        Log.e("DLavieChat", "closeTicket failed", e)
    }
}

/**
 * Call DLavie Assistant AI via Supabase Edge Function.
 * Falls back to rule-based responses if Edge Function fails.
 */
private suspend fun callAIAssistant(message: String, history: List<Map<String, String>>): String {
    return withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("message", message)
                put("history", JSONArray().apply {
                    history.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg["role"] ?: "user")
                            put("text", msg["text"] ?: "")
                        })
                    }
                })
            }
            val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/functions/v1/dlavie-assistant").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(text)
                json.optString("response", "Maaf, saya tidak bisa memproses permintaan Anda saat ini.")
            } else {
                conn.disconnect()
                "Maaf, assistant sedang tidak tersedia. Coba lagi nanti atau gunakan Live Chat."
            }
        } catch (e: Exception) {
            Log.e("DLavieChat", "callAIAssistant failed", e)
            "Maaf, terjadi kesalahan. Silakan coba lagi atau hubungi developer via Live Chat."
        }
    }
}
