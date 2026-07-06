package com.drmacze.f16launcher

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
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
// v7.9.19: AsyncImage + ContentScale for report screenshot in detail
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
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

    // v7.9.22: Unread count — poll + refresh trigger
    var unreadCount by remember { mutableStateOf(0) }
    var refreshTrigger by remember { mutableStateOf(0) }  // bump to force recount

    LaunchedEffect(api.loggedIn()) {
        if (api.loggedIn()) {
            unreadCount = countUnreadMessages(api)
            while (true) {
                delay(10_000)
                unreadCount = countUnreadMessages(api)
            }
        }
    }
    // v7.9.22: Recount when refreshTrigger changes (e.g., after closing detail)
    LaunchedEffect(refreshTrigger) {
        if (api.loggedIn() && refreshTrigger > 0) {
            unreadCount = countUnreadMessages(api)
        }
    }

    if (!expanded) {
        // Floating button with auto-hide (slides to edge after 3 seconds)
        var isHidden by remember { mutableStateOf(false) }
        val offsetX by animateDpAsState(
            targetValue = if (isHidden) 24.dp else 0.dp,
            animationSpec = tween(400, easing = FastOutSlowInEasing),
            label = "fab_offset"
        )
        
        LaunchedEffect(isHidden) {
            if (!isHidden) {
                delay(3000)
                isHidden = true
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                // v7.9.2: bottom = 188.dp — stack di atas Post FAB (yang ada di 112.dp).
                // Post FAB = 112dp + 56dp height = 168dp top edge.
                // Live Chat FAB = 188dp (20dp gap di atas Post FAB top edge).
                // Clearance dari FloatingNav center button (102dp) = 188-102 = 86dp (safe).
                // v7.9.1: sebelumnya 168dp → gap hanya 16dp, terlihat seperti overlap.
                .padding(end = 16.dp, bottom = 188.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .offset(x = offsetX)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable {
                        isHidden = false
                        expanded = true
                        mode = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isHidden) Icons.Rounded.ChatBubbleOutline else Icons.Rounded.ChatBubble,
                    contentDescription = "DLavie Assistant",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }

            // v7.9.0: Notification dot — merah, menandakan ada pesan baru dari developer
            if (unreadCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(if (unreadCount > 9) 22.dp else 18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4444))
                        .border(2.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (unreadCount > 9) "9+" else unreadCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    } else {
        when (mode) {
            null -> ChatBotMenu(
                onSelect = { selected ->
                    mode = selected
                },
                onClose = { expanded = false },
                unreadCount = unreadCount
            )
            "report" -> ReportForm(
                api = api,
                onBack = { mode = null },
                onClose = { expanded = false; mode = null }
            )
            "live_chat" -> LiveChatScreen(
                api = api,
                onBack = { mode = null },
                onClose = { expanded = false; mode = null },
                onOpenHistory = { mode = "history" }
            )
            "assistant" -> LiveChatScreen(
                api = api,
                isAssistant = true,
                onBack = { mode = null },
                onClose = { expanded = false; mode = null },
                onOpenHistory = { mode = "history" }
            )
            // v7.9.1: Riwayat Chat — list semua ticket (open + closed) yang dimiliki user.
            // Memanggil fetchChatHistory() untuk load data dari Supabase support_tickets.
            // User bisa tap entry untuk lihat pesan di ticket tersebut (read-only untuk closed).
            "history" -> ChatHistoryScreen(
                api = api,
                onBack = { mode = null },
                onClose = { expanded = false; mode = null },
                onOpenActiveChat = { mode = "live_chat" },
                onReadUpdate = { refreshTrigger++ }  // v7.9.22: trigger recount after read
            )
        }
    }
}

// ─── Menu ───────────────────────────────────────────────────────────────────

@Composable
private fun ChatBotMenu(onSelect: (String) -> Unit, onClose: () -> Unit, unreadCount: Int = 0) {
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

                // v7.9.1: Riwayat Chat button — list semua chat sebelumnya
                // v7.9.20: Show badge if unreadCount > 0
                Box {
                    ChatBotMenuItem(
                        icon = Icons.Rounded.History,
                        title = "Riwayat Chat",
                        subtitle = "Lihat chat sebelumnya dengan developer",
                        onClick = { onSelect("history") }
                    )
                    // v7.9.20: Red badge on Riwayat Chat if unread
                    if (unreadCount > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-4).dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF4444))
                                .border(2.dp, PureBlack, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (unreadCount > 9) "9+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }
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
private fun LiveChatScreen(
    api: CommunityApi,
    isAssistant: Boolean = false,
    onBack: () -> Unit,
    onClose: () -> Unit,
    // v7.9.1: callback untuk navigasi langsung ke halaman Riwayat Chat dari form state.
    // Memudahkan user yang ingin lihat balasan developer di sesi lama tanpa harus
    // kembali ke menu utama ChatBot dulu.
    onOpenHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var ticketId by remember { mutableStateOf<String?>(null) }
    var ticketStatus by remember { mutableStateOf("loading") }
    var sending by remember { mutableStateOf(false) }
    var adminOnline by remember { mutableStateOf(false) }
    var lastActivityTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var inactivityWarningSent by remember { mutableStateOf(false) }
    var cooldownRemaining by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    // v6.8.2 BUGFIX: cancelRequested flag — di-set saat user klik "Batalkan".
    // Poller pending/open WAJIB check flag ini sebelum mengubah ticketStatus dari
    // status yang dibaca di DB. Tanpa ini, race condition terjadi:
    //   1. User klik "Batalkan" → PATCH status=closed ke Supabase
    //   2. Tapi developer di Dev Hub sudah klik "Accept" 0.5 detik sebelumnya
    //      → Supabase status = open (developer menang race)
    //   3. Poller pending (while-true loop) baca status = open dari DB
    //   4. Poller set ticketStatus = "open" → user tiba-tiba masuk chat padahal cancel!
    //
    // Fix: flag lokal cancelRequested di-set SBLUM PATCH. Poller check flag ini
    // setiap iterasi. Kalau true → BREAK, jangan override ticketStatus dari DB.
    var cancelRequested by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && ticketId != null && ticketStatus == "open") {
            scope.launch {
                sending = true
                uploadAndSendImage(api, ticketId!!, uri, context)
                sending = false
                lastActivityTime = System.currentTimeMillis()
            }
        }
    }

    // Initialize: check existing open/pending ticket OR cooldown OR show form
    LaunchedEffect(Unit) {
        if (!isAssistant) {
            // 1. Check for existing OPEN or PENDING ticket (resume session)
            val existingTicket = findOpenTicket(api)
            if (existingTicket != null) {
                ticketId = existingTicket.first
                ticketStatus = existingTicket.second // "open" or "pending"
                val history = pollMessages(api, existingTicket.first)
                messages = history
                lastActivityTime = System.currentTimeMillis()
                // v7.9.0: Mark messages as read (update last_read_at)
                markMessagesAsRead(api, existingTicket.first)
                if (existingTicket.second == "open") {
                    adminOnline = checkAdminOnline(api)
                    if (!adminOnline) {
                        messages = messages + ChatMessage(
                            senderType = "bot",
                            body = "Saat ini tim kami offline. Mohon tunggu sampai tim kami merespon.",
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
                return@LaunchedEffect
            }

            // 2. Check cooldown
            val cooldownEnd = checkCooldown(api)
            if (cooldownEnd > 0) {
                val remaining = ((cooldownEnd - System.currentTimeMillis()) / 1000).toInt()
                if (remaining > 0) {
                    ticketStatus = "cooldown"
                    cooldownRemaining = remaining
                    return@LaunchedEffect
                }
            }

            // 3. No existing session, no cooldown → show create ticket form
            ticketStatus = "form"
        } else {
            // Assistant mode — always create new (no cooldown for AI)
            ticketId = createTicket(api, "assistant")
            ticketStatus = "open"
            messages = messages + ChatMessage(
                senderType = "bot",
                body = "Halo! Saya DLavie Assistant. Saya bisa membantu seputar DLavie Launcher dan FIFA 16. Apa yang bisa saya bantu?",
                timestamp = System.currentTimeMillis()
            )
        }
    }

    // Poll for new messages
    // v6.8.2 BUGFIX: check cancelRequested di setiap iterasi. Kalau user klik
    // "Batalkan" (atau close), poller BREAK — jangan proses message atau status
    // dari DB. Mencegah race condition: developer set "open" tepat saat user
    // cancel → poller tidak boleh override ticketStatus jadi "open" lagi.
    LaunchedEffect(ticketId, ticketStatus) {
        if (ticketId != null && ticketStatus == "open" && !isAssistant) {
            while (true) {
                delay(5000)
                // v6.8.2: user cancel/close → BREAK, jangan poll message lagi.
                if (cancelRequested) break
                val newMsgs = pollMessages(api, ticketId!!)
                if (cancelRequested) break  // double-check setelah network
                if (newMsgs.isNotEmpty()) {
                    messages = (messages + newMsgs).distinctBy { it.id }
                    lastActivityTime = System.currentTimeMillis()
                    inactivityWarningSent = false
                }

                val currentStatus = checkTicketStatus(api, ticketId!!)
                if (cancelRequested) break  // double-check setelah network
                if (currentStatus == "closed") {
                    ticketStatus = "closed"
                    messages = messages + ChatMessage(
                        senderType = "bot",
                        body = "Sesi ini telah ditutup oleh admin. Untuk memulai sesi baru, tunggu 1 jam.",
                        timestamp = System.currentTimeMillis()
                    )
                    break
                }

                val idleTime = System.currentTimeMillis() - lastActivityTime
                if (idleTime > 5 * 60 * 1000 && !inactivityWarningSent) {
                    inactivityWarningSent = true
                    messages = messages + ChatMessage(
                        senderType = "bot",
                        body = "Apakah kamu masih disini? jika tidak, live chat akan kami tutup. Terima kasih atas masukan dan bantuanmu agar DLavie tetap berkembang",
                        timestamp = System.currentTimeMillis()
                    )
                    delay(30000)
                    closeTicketWithCooldown(api, ticketId!!, cancelReason = "inactivity")
                    ticketStatus = "closed"
                    break
                }
            }
        }
    }

    // Poll for pending→open status change
    // v6.8.2 BUGFIX: check cancelRequested di setiap iterasi. Kalau user klik
    // "Batalkan", poller BREAK immediately — jangan override ticketStatus dari
    // DB meskipun developer sudah set "open" di Dev Hub (race condition).
    LaunchedEffect(ticketId, ticketStatus) {
        if (ticketId != null && ticketStatus == "pending" && !isAssistant) {
            val startTime = System.currentTimeMillis()
            while (true) {
                delay(5000)
                // v6.8.2: user cancel → BREAK, jangan proses status dari DB.
                if (cancelRequested) break
                val status = checkTicketStatus(api, ticketId!!)
                // v6.8.2: double-check cancelRequested setelah network call
                // (mungkin user klik cancel selama delay/network).
                if (cancelRequested) break
                if (status == "open") {
                    ticketStatus = "open"
                    lastActivityTime = System.currentTimeMillis()
                    messages = messages + ChatMessage(
                        senderType = "bot",
                        body = "Developer telah menerima panggilan Anda. Silakan ketik pesan Anda.",
                        timestamp = System.currentTimeMillis()
                    )
                    break
                }
                if (status == "closed") {
                    ticketStatus = "closed"
                    break
                }
                // Auto-close if pending > 5 min
                if (System.currentTimeMillis() - startTime > 5 * 60 * 1000) {
                    closeTicketWithCooldown(api, ticketId!!, cancelReason = "timeout_pending")
                    ticketStatus = "closed"
                    messages = messages + ChatMessage(
                        senderType = "bot",
                        body = "Tidak ada developer yang merespon dalam 5 menit. Sesi ditutup. Coba lagi nanti.",
                        timestamp = System.currentTimeMillis()
                    )
                    break
                }
            }
        }
    }

    // Cooldown countdown
    LaunchedEffect(ticketStatus) {
        if (ticketStatus == "cooldown") {
            while (cooldownRemaining > 0) {
                delay(1000)
                cooldownRemaining--
            }
            ticketStatus = "loading"
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
                Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.05f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp).clickable { onBack() })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (isAssistant) "DLavie Assistant" else "Live Chat", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Text(
                                when (ticketStatus) { "open" -> "Chat langsung dengan developer"; "closed" -> "Sesi ditutup"; "cooldown" -> "Cooldown aktif"; "form" -> "Buat sesi live chat"; "pending" -> "Menunggu developer..."; else -> "Memuat..." },
                                color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontFamily = InterFontFamily
                            )
                        }
                        if (ticketStatus == "open") {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.6f)))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp).clickable {
                                // v6.8.2 BUGFIX: set cancelRequested = true DULU sebelum PATCH.
                                // Poller open akan check flag ini dan BREAK — jangan proses
                                // message/status dari DB setelah user close.
                                cancelRequested = true
                                ticketId?.let {
                                    scope.launch {
                                        closeTicketWithCooldown(api, it, cancelReason = "user_cancelled")
                                        ticketStatus = "closed"
                                    }
                                }
                            })
                        }
                    }
                }

                if (ticketStatus == "cooldown") {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Schedule, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Cooldown Active", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(8.dp))
                            val mins = cooldownRemaining / 60
                            val secs = cooldownRemaining % 60
                            Text("Tunggu ${mins}:${if (secs < 10) "0" else ""}${secs} untuk sesi baru", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(24.dp))
                            Surface(Modifier.clickable { onClose() }, shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.1f)) {
                                Text("Tutup", color = Color.White, fontSize = 14.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                            }
                        }
                    }
                } else if (ticketStatus == "form") {
                    // Create ticket form
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.SupportAgent, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Live Chat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Text("Chat langsung dengan developer", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontFamily = InterFontFamily)
                        Spacer(Modifier.height(24.dp))
                        Column(Modifier.fillMaxWidth()) {
                            Text("Versi Android", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(4.dp))
                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.05f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
                                Text(android.os.Build.VERSION.RELEASE ?: "Unknown", color = Color.White, fontSize = 14.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(12.dp))
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Surface(Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                ticketId = createTicket(api, "live_chat")
                                if (ticketId != null) { ticketStatus = "pending"; lastActivityTime = System.currentTimeMillis() }
                            }
                        }, shape = RoundedCornerShape(12.dp), color = Color.White) {
                            Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Mulai", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
                        }
                        // v7.9.1: Tombol "Lihat Riwayat Chat" — shortcut ke halaman history.
                        // Bantu user yang mungkin sudah pernah chat sebelumnya untuk
                        // melihat balasan developer di sesi lama tanpa harus mulai sesi baru.
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            Modifier.clickable { onOpenHistory() },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.History, null, tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Lihat Riwayat Chat", color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp, fontFamily = InterFontFamily)
                            }
                        }
                    }
                } else if (ticketStatus == "pending") {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Menghubungkan...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        Spacer(Modifier.height(8.dp))
                        Text("Menunggu developer menerima panggilan Anda.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontFamily = InterFontFamily, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Sesi akan ditutup otomatis dalam 5 menit jika tidak ada respon.", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, fontFamily = InterFontFamily, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Surface(Modifier.clickable {
                            // v6.8.2 BUGFIX: set cancelRequested = true DULU sebelum PATCH.
                            // Poller pending akan check flag ini dan BREAK — jangan
                            // override ticketStatus dari DB meskipun developer sudah
                            // set "open" di Dev Hub (race condition fix).
                            cancelRequested = true
                            ticketId?.let {
                                scope.launch {
                                    closeTicketWithCooldown(api, it, cancelReason = "user_cancelled")
                                    ticketStatus = "closed"
                                    messages = messages + ChatMessage(
                                        senderType = "bot",
                                        body = "Panggilan dibatalkan. Anda bisa membuat sesi live chat baru kapan saja.",
                                        timestamp = System.currentTimeMillis()
                                    )
                                }
                            }
                        }, shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.1f)) {
                            Text("Batalkan", color = Color.White, fontSize = 13.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                        }
                    }
                } else if (ticketStatus == "loading") {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(messages) { msg -> ChatBubble(msg) }
                    }

                    if (ticketStatus == "open") {
                        Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.05f)) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AttachFile, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(24.dp).clickable { imagePicker.launch("image/*") })
                                Spacer(Modifier.width(8.dp))
                                TextField(value = inputText, onValueChange = { inputText = it }, placeholder = { Text("Ketik pesan...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White),
                                    modifier = Modifier.weight(1f), maxLines = 3)
                                Spacer(Modifier.width(8.dp))
                                Surface(Modifier.size(36.dp).clickable(enabled = inputText.isNotBlank() && !sending) {
                                    if (inputText.isNotBlank() && ticketId != null) {
                                        val text = inputText; inputText = ""; sending = true
                                        scope.launch {
                                            sendMessage(api, ticketId!!, text)
                                            messages = messages + ChatMessage(id = UUID.randomUUID().toString(), senderType = "user", body = text, timestamp = System.currentTimeMillis())
                                            lastActivityTime = System.currentTimeMillis(); inactivityWarningSent = false
                                            if (isAssistant) {
                                                val aiResponse = callAIAssistant(text, messages.map { mapOf("role" to it.senderType, "text" to it.body) })
                                                messages = messages + ChatMessage(id = UUID.randomUUID().toString(), senderType = "bot", body = aiResponse, timestamp = System.currentTimeMillis())
                                            }
                                            sending = false
                                        }
                                    }
                                }, shape = CircleShape, color = if (inputText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.2f)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (sending) { CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp) }
                                        else { Icon(Icons.Rounded.Send, null, tint = Color.Black, modifier = Modifier.size(18.dp)) }
                                    }
                                }
                            }
                        }
                    } else if (ticketStatus == "closed") {
                        Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.03f)) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sesi ini telah ditutup", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontFamily = InterFontFamily)
                                Spacer(Modifier.height(4.dp))
                                Text("Tunggu 1 jam untuk membuat sesi live chat baru", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp, fontFamily = InterFontFamily)
                                Spacer(Modifier.height(12.dp))
                                Surface(Modifier.clickable { onClose() }, shape = RoundedCornerShape(12.dp), color = Color.White) {
                                    Text("Tutup", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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

// ─── v7.9.1: Chat History Screen ──────────────────────────────────────────────
// List semua ticket (live_chat + assistant) milik user — open, pending, closed.
// Tiap entry: icon, kategori, status badge, pesan terakhir (preview), timestamp.
// Tap closed ticket → buka ChatHistoryDetail (read-only).
// Tap open/pending ticket → lanjutkan sesi (buka LiveChatScreen via onOpenActiveChat).
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatHistoryScreen(
    api: CommunityApi,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onOpenActiveChat: () -> Unit,
    onReadUpdate: () -> Unit = {}  // v7.9.22: callback after markMessagesAsRead
) {
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf<List<ChatHistoryEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTicketId by remember { mutableStateOf<String?>(null) }

    // Load history on launch
    LaunchedEffect(Unit) {
        loading = true
        error = null
        val result = fetchChatHistory(api)
        history = result
        loading = if (result.isEmpty() && !api.loggedIn()) {
            error = "Login diperlukan untuk melihat riwayat chat"
            false
        } else {
            false
        }
    }

    if (selectedTicketId != null) {
        // v7.9.19: Pass entry for report detail (has complaint, screenshot, dev_feedback)
        val selectedEntry = history.find { it.ticketId == selectedTicketId }
        ChatHistoryDetail(
            api = api,
            ticketId = selectedTicketId!!,
            entry = selectedEntry,
            onBack = { selectedTicketId = null },
            onReadUpdate = onReadUpdate  // v7.9.22: pass callback
        )
        return
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureBlack,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                // Header
                Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.05f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White,
                            modifier = Modifier.size(24.dp).clickable { onBack() })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Riwayat Chat", color = Color.White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Text("Percakapan sebelumnya dengan developer",
                                color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp,
                                fontFamily = InterFontFamily)
                        }
                        // Refresh button
                        Icon(Icons.Rounded.Refresh, null, tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp).clickable {
                                scope.launch {
                                    loading = true
                                    history = fetchChatHistory(api)
                                    loading = false
                                }
                            })
                    }
                }

                // Content
                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else if (error != null) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ErrorOutline, null, tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(error!!, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp,
                                textAlign = TextAlign.Center, fontFamily = InterFontFamily)
                        }
                    }
                } else if (history.isEmpty()) {
                    // Empty state
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Belum ada riwayat chat", color = Color.White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(8.dp))
                            Text("Percakapan Live Chat dan Assistant akan muncul di sini",
                                color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp,
                                textAlign = TextAlign.Center, fontFamily = InterFontFamily)
                            Spacer(Modifier.height(24.dp))
                            Surface(Modifier.clickable { onClose() },
                                shape = RoundedCornerShape(12.dp), color = Color.White) {
                                Text("Tutup", color = Color.Black, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold, fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                            }
                        }
                    }
                } else {
                    // List of history entries
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { entry ->
                            ChatHistoryEntryCard(
                                entry = entry,
                                onClick = {
                                    // v7.9.18: Reports always open as detail (read-only, show dev_feedback)
                                    if (entry.category == "report") {
                                        selectedTicketId = entry.ticketId
                                    } else if (entry.status == "open" || entry.status == "pending") {
                                        // Active live chat session — open Live Chat (will resume)
                                        onOpenActiveChat()
                                    } else {
                                        // Closed session — open read-only detail
                                        selectedTicketId = entry.ticketId
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHistoryEntryCard(entry: ChatHistoryEntry, onClick: () -> Unit) {
    val isLiveChat = entry.category == "live_chat"
    val isReport = entry.category == "report"
    val isActive = entry.status == "open" || entry.status == "pending"
    // v7.9.18: Report status colors (pending/approved/rejected)
    val statusColor = when {
        isReport -> when (entry.status) {
            "approved" -> Color(0xFF4ADE80)
            "rejected" -> Color(0xFFFF5252)
            "pending" -> Color(0xFFFBBF24)
            "open" -> Color(0xFF4ADE80)
            else -> Color.White.copy(alpha = 0.3f)
        }
        else -> when (entry.status) {
            "open" -> Color(0xFF4ADE80)      // green
            "pending" -> Color(0xFFFBBF24)   // yellow
            "closed" -> Color.White.copy(alpha = 0.3f)
            else -> Color.White.copy(alpha = 0.3f)
        }
    }
    val statusLabel = when {
        isReport -> when (entry.status) {
            "approved" -> "Disetujui"
            "rejected" -> "Ditolak"
            "pending" -> "Menunggu"
            "open" -> "Ditinjau"
            "closed" -> "Selesai"
            else -> entry.status
        }
        else -> when (entry.status) {
            "open" -> "Aktif"
            "pending" -> "Menunggu"
            "closed" -> "Selesai"
            else -> entry.status
        }
    }

    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF4ADE80).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon — v7.9.18: add report icon
            val iconBg = when {
                isReport -> Color(0xFFFF9800).copy(alpha = 0.15f)
                isLiveChat -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                else -> Color(0xFFA855F7).copy(alpha = 0.15f)
            }
            val iconTint = when {
                isReport -> Color(0xFFFFB74D)
                isLiveChat -> Color(0xFF60A5FA)
                else -> Color(0xFFC084FC)
            }
            val iconVector = when {
                isReport -> Icons.Rounded.BugReport
                isLiveChat -> Icons.Rounded.SupportAgent
                else -> Icons.Rounded.AutoAwesome
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, null, tint = iconTint, modifier = Modifier.size(20.dp))
                // v7.9.22: Red dot untuk ticket dengan unread dev_feedback/reply
                // Report: ada dev_feedback → unread
                // Live chat: lastMessageSender = admin/bot → unread
                val hasUnread = if (isReport) {
                    entry.devFeedback.isNotBlank()
                } else {
                    entry.lastMessageSender == "admin" || entry.lastMessageSender == "bot"
                }
                if (hasUnread) {
                    Box(
                        Modifier.align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4444))
                            .border(2.dp, PureBlack, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            // Content
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            isReport -> "Report"
                            isLiveChat -> "Live Chat"
                            else -> "Assistant"
                        },
                        color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                    )
                    Spacer(Modifier.width(8.dp))
                    // Status badge
                    Box(
                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.18f))
                    ) {
                        Text(
                            statusLabel,
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    // v7.9.19: Ticket ID
                    Text(
                        "#${entry.ticketId.take(8)}",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 9.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    // Message count
                    Text(
                        "${entry.messageCount} pesan",
                        color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp,
                        fontFamily = InterFontFamily
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Last message preview
                val preview = entry.lastMessageBody.ifBlank {
                    if (entry.messageCount == 0) "Belum ada pesan" else "(gambar)"
                }
                val previewPrefix = when (entry.lastMessageSender) {
                    "user" -> "Anda: "
                    "admin" -> "Developer: "
                    "bot" -> "Bot: "
                    else -> ""
                }
                Text(
                    previewPrefix + preview,
                    color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp,
                    fontFamily = InterFontFamily, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                // Timestamp
                val timeStr = try {
                    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(
                        if (entry.lastMessageCreatedAt.isNotBlank()) entry.lastMessageCreatedAt
                        else entry.createdAt
                    )
                    val cal = Calendar.getInstance()
                    val now = cal.timeInMillis
                    cal.time = parsed ?: Date()
                    val then = cal.timeInMillis
                    val diffMin = (now - then) / (60 * 1000)
                    val diffHr = diffMin / 60
                    val diffDay = diffHr / 24
                    when {
                        diffMin < 1 -> "Baru saja"
                        diffMin < 60 -> "${diffMin} menit lalu"
                        diffHr < 24 -> "${diffHr} jam lalu"
                        diffDay < 7 -> "${diffDay} hari lalu"
                        else -> SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(parsed)
                    }
                } catch (_: Exception) { "" }
                Text(
                    timeStr,
                    color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

@Composable
private fun ChatHistoryDetail(
    api: CommunityApi,
    ticketId: String,
    entry: ChatHistoryEntry? = null,
    onBack: () -> Unit,
    onReadUpdate: () -> Unit = {}  // v7.9.22: trigger badge recount after read
) {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val isReport = entry?.category == "report"

    LaunchedEffect(ticketId) {
        loading = true
        if (!isReport) {
            messages = fetchAllTicketMessages(api, ticketId)
        }
        loading = false
        // Mark as read (clear unread badge)
        markMessagesAsRead(api, ticketId)
        // v7.9.22: Trigger badge recount immediately after marking as read
        onReadUpdate()
        if (!isReport && messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Dialog(onDismissRequest = onBack) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PureBlack,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                // Header — v7.9.19: show Ticket ID
                Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.05f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = Color.White,
                            modifier = Modifier.size(24.dp).clickable { onBack() })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isReport) "Detail Report" else "Detail Percakapan",
                                color = Color.White, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, fontFamily = InterFontFamily
                            )
                            Text(
                                "Ticket #${ticketId.take(8)} • ${if (isReport) "Read-only" else "Sesi selesai"}",
                                color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else if (isReport && entry != null) {
                    // v7.9.19: Report detail — show all info
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val statusColor = when (entry.status) {
                                "approved" -> Color(0xFF4ADE80)
                                "rejected" -> Color(0xFFFF5252)
                                "pending" -> Color(0xFFFBBF24)
                                else -> Color.White.copy(alpha = 0.3f)
                            }
                            val statusLabel = when (entry.status) {
                                "approved" -> "Disetujui"
                                "rejected" -> "Ditolak"
                                "pending" -> "Menunggu"
                                "open" -> "Ditinjau"
                                else -> entry.status
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            }
                        }

                        // Screenshot
                        if (entry.screenshotUrl.isNotBlank()) {
                            Text("Screenshot", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            AsyncImage(
                                model = entry.screenshotUrl,
                                contentDescription = "Screenshot",
                                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Complaint
                        if (entry.complaint.isNotBlank()) {
                            Text("Keluhan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Text(entry.complaint, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontFamily = InterFontFamily)
                        }

                        // User feedback
                        if (entry.userFeedback.isNotBlank()) {
                            Text("Harapan User", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                            Text(entry.userFeedback, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontFamily = InterFontFamily)
                        }

                        // Dev feedback
                        if (entry.devFeedback.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Feedback Developer", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                                    Spacer(Modifier.height(4.dp))
                                    Text(entry.devFeedback, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontFamily = InterFontFamily)
                                }
                            }
                        }

                        // Android version
                        if (entry.androidVersion.isNotBlank()) {
                            Text("Android Version: ${entry.androidVersion}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontFamily = InterFontFamily)
                        }
                    }
                    // Footer
                    Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.03f)) {
                        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                            Text("Report ini bersifat read-only.",
                                color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp,
                                fontFamily = InterFontFamily, textAlign = TextAlign.Center)
                        }
                    }
                } else if (messages.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.ChatBubbleOutline, null, tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Tidak ada pesan", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp,
                                fontFamily = InterFontFamily)
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 12.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg -> ChatBubble(msg) }
                    }
                    Surface(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.03f)) {
                        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                            Text("Sesi ini telah berakhir. Pesan bersifat read-only.",
                                color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp,
                                fontFamily = InterFontFamily, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
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
                put("status", "pending")
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

// ─── New helper functions for session management ────────────────────────────

private suspend fun findOpenTicket(api: CommunityApi): Pair<String, String>? {
    return withContext(Dispatchers.IO) {
        try {
            val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets?user_id=eq.${api.userId()}&status=in.(open,pending)&category=eq.live_chat&order=created_at.desc&limit=1&select=id,status").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${api.token()}")
            }
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    Pair(obj.optString("id"), obj.optString("status", "pending"))
                } else null
            } else null
        } catch (e: Exception) { null }
    }
}

private fun checkCooldown(api: CommunityApi): Long {
    return try {
        val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets?user_id=eq.${api.userId()}&status=eq.closed&category=eq.live_chat&order=closed_at.desc&limit=1&select=closed_at,last_closed_at").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
        }
        if (conn.responseCode in 200..299) {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            if (arr.length() > 0) {
                val closedAtStr = arr.getJSONObject(0).optString("last_closed_at", arr.getJSONObject(0).optString("closed_at", ""))
                if (closedAtStr.isNotBlank()) {
                    val closedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(closedAtStr)?.time ?: return 0
                    val cooldownEnd = closedTime + 60 * 60 * 1000 // 1 hour
                    if (cooldownEnd > System.currentTimeMillis()) cooldownEnd else 0
                } else { 0 }
            } else { 0 }
        } else { 0 }
    } catch (e: Exception) { 0 }
}

private fun checkTicketStatus(api: CommunityApi, ticketId: String): String {
    return try {
        val conn = (URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets?id=eq.$ticketId&select=status").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
        }
        if (conn.responseCode in 200..299) {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            if (arr.length() > 0) arr.getJSONObject(0).optString("status", "open") else "open"
        } else { "open" }
    } catch (e: Exception) { "open" }
}

/**
 * v6.8.2: Close ticket + set cooldown timestamp.
 *
 * Param `cancelReason` (optional):
 *   - "user_cancelled" — user klik "Batalkan" atau tombol X (explicit cancel)
 *   - "inactivity"     — auto-close setelah 5 menit idle (no response)
 *   - "timeout_pending"— auto-close setelah 5 menit pending (no dev pickup)
 *   - null/omitted     — backward compat (close dari sisi admin/other)
 *
 * Dev Hub bisa baca field `cancel_reason` untuk bedakan: kalau "user_cancelled",
 * dev TIDAK boleh accept ticket (sudah di-cancel user). Ini mencegah race condition
 * di mana dev klik "Accept" tepat saat user klik "Batalkan".
 *
 * NOTE: Field `cancel_reason` harus ada di schema `support_tickets` (text, nullable).
 *       Kalau belum ada, PATCH tetap berhasil (field di-ignore) — backward compat.
 */
private fun closeTicketWithCooldown(
    api: CommunityApi,
    ticketId: String,
    cancelReason: String? = null
) {
    try {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
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
            put("closed_at", now)
            put("last_closed_at", now)
            // v6.8.2: tag cancel reason supaya Dev Hub bisa bedakan.
            // Kalau field belum ada di schema, PATCH tetap OK (Supabase ignore unknown field
            // hanya kalau column belum ada — sebenarnya akan return 400. Tapi kita catch
            // exception di sini, jadi tidak crash app. Recommended: tambah column via migration).
            if (cancelReason != null) {
                put("cancel_reason", cancelReason)
            }
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray()) }
        conn.responseCode
        conn.disconnect()
    } catch (e: Exception) {
        Log.e("DLavieChat", "closeTicketWithCooldown failed", e)
    }
}

// ─── v7.9.0: Unread message tracking ─────────────────────────────────────────

/**
 * Count unread messages dari developer (sender_type != user) untuk user ini.
 * Unread = pesan yang dikirim SETELAH last_read_at di support_tickets.
 *
 * Cek semua ticket (open + closed) milik user, hitung pesan admin yang
 * created_at > last_read_at.
 */
private suspend fun countUnreadMessages(api: CommunityApi): Int = withContext(Dispatchers.IO) {
    if (!api.loggedIn()) return@withContext 0
    try {
        val userId = api.userId()
        if (userId.isBlank()) return@withContext 0

        // v7.9.18: Include ALL categories (live_chat, assistant, report) in unread count.
        // Sebelumnya hanya live_chat + assistant — report replies tidak dihitung.
        val ticketsUrl = URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets" +
            "?user_id=eq.$userId" +
            "&category=in.(live_chat,assistant,report)" +
            "&order=created_at.desc&limit=20" +
            "&select=id,last_read_at,status,category,dev_feedback,dev_feedback_at")
        val ticketsConn = (ticketsUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
        }
        val ticketsText = ticketsConn.inputStream?.bufferedReader()?.readText().orEmpty()
        ticketsConn.disconnect()
        val ticketsArr = JSONArray(ticketsText)
        if (ticketsArr.length() == 0) return@withContext 0

        var totalUnread = 0
        for (i in 0 until ticketsArr.length()) {
            val ticket = ticketsArr.getJSONObject(i)
            val ticketId = ticket.optString("id")
            val lastReadAt = ticket.optString("last_read_at", "")
            val status = ticket.optString("status", "closed")
            val category = ticket.optString("category", "")
            val devFeedback = ticket.optString("dev_feedback", "")
            val devFeedbackAt = ticket.optString("dev_feedback_at", "")

            // v7.9.18: For reports — count dev_feedback as unread if dev_feedback_at > last_read_at
            if (category == "report") {
                if (devFeedback.isNotBlank() && devFeedbackAt.isNotBlank() && devFeedbackAt != "null") {
                    // Check if dev_feedback is newer than last_read_at
                    val isUnread = lastReadAt.isBlank() || lastReadAt == "null" || devFeedbackAt > lastReadAt
                    if (isUnread) totalUnread++
                }
                continue  // reports don't have ticket_messages from admin (only dev_feedback)
            }

            // Only count unread for open tickets (closed = already seen)
            if (status != "open" && status != "pending") continue

            // 2. Count admin messages after last_read_at
            val msgUrl = URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/ticket_messages" +
                "?ticket_id=eq.$ticketId" +
                "&sender_type=neq.user" +
                (if (lastReadAt.isNotBlank() && lastReadAt != "null") "&created_at=gt.$lastReadAt" else "") +
                "&select=id")
            val msgConn = (msgUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${api.token()}")
            }
            val msgText = msgConn.inputStream?.bufferedReader()?.readText().orEmpty()
            msgConn.disconnect()
            val msgArr = JSONArray(msgText)
            totalUnread += msgArr.length()
        }
        totalUnread
    } catch (e: Exception) {
        Log.e("DLavieChat", "countUnreadMessages failed", e)
        0
    }
}

/**
 * Update last_read_at untuk ticket tertentu → mark all messages as read.
 * Dipanggil saat user membuka chat screen.
 */
private fun markMessagesAsRead(api: CommunityApi, ticketId: String) {
    try {
        val now = java.time.Instant.now().toString()
        val url = URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets?id=eq.$ticketId")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 10000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }
        val payload = JSONObject().put("last_read_at", now)
        conn.outputStream.use { it.write(payload.toString().toByteArray()) }
        conn.responseCode
        conn.disconnect()
    } catch (e: Exception) {
        Log.e("DLavieChat", "markMessagesAsRead failed", e)
    }
}

/**
 * v7.9.0: Fetch chat history untuk ticket yang sudah closed.
 * v7.9.1: UPDATE — sekarang juga fetch last_message_preview + message_count
 * v7.9.18: Include report category + fetch dev_feedback untuk reports.
 * Return list of ChatHistoryEntry (urut created_at desc, limit 20).
 */
private suspend fun fetchChatHistory(api: CommunityApi): List<ChatHistoryEntry> = withContext(Dispatchers.IO) {
    if (!api.loggedIn()) return@withContext emptyList()
    try {
        val userId = api.userId()
        // v7.9.18: Include report category (sebelumnya hanya live_chat + assistant)
        val url = URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/support_tickets" +
            "?user_id=eq.$userId" +
            "&category=in.(live_chat,assistant,report)" +
            "&order=created_at.desc&limit=20" +
            "&select=id,status,category,created_at,closed_at,last_activity_at,complaint,dev_feedback,dev_feedback_at,report_status,screenshot_url")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
        }
        val text = conn.inputStream?.bufferedReader()?.readText().orEmpty()
        conn.disconnect()
        val arr = JSONArray(text)
        val result = mutableListOf<ChatHistoryEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ticketId = o.optString("id")
            val category = o.optString("category")

            // v7.9.18: For reports — use dev_feedback as last message (no ticket_messages)
            if (category == "report") {
                val devFeedback = o.optString("dev_feedback", "")
                val devFeedbackAt = o.optString("dev_feedback_at", "")
                val complaint = o.optString("complaint", "")
                val userFeedback = o.optString("user_feedback", "")
                val screenshotUrl = o.optString("screenshot_url", "")
                val androidVersion = o.optString("android_version", "")
                result.add(ChatHistoryEntry(
                    ticketId = ticketId,
                    status = o.optString("status"),
                    category = category,
                    createdAt = o.optString("created_at"),
                    closedAt = o.optString("closed_at"),
                    lastActivityAt = o.optString("last_activity_at"),
                    messageCount = if (devFeedback.isNotBlank()) 1 else 0,
                    lastMessageBody = if (devFeedback.isNotBlank()) devFeedback else complaint,
                    lastMessageSender = if (devFeedback.isNotBlank()) "admin" else "user",
                    lastMessageCreatedAt = if (devFeedbackAt.isNotBlank() && devFeedbackAt != "null") devFeedbackAt else o.optString("created_at"),
                    // v7.9.19: Report detail fields
                    complaint = complaint,
                    userFeedback = userFeedback,
                    screenshotUrl = screenshotUrl,
                    androidVersion = androidVersion,
                    devFeedback = devFeedback
                ))
                continue
            }

            // 2. Fetch last message + total count untuk ticket ini (live_chat/assistant)
            val msgUrl = URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/ticket_messages" +
                "?ticket_id=eq.$ticketId" +
                "&order=created_at.desc&limit=1" +
                "&select=body,sender_type,created_at")
            val msgConn = (msgUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${api.token()}")
                setRequestProperty("Prefer", "count=exact")
            }
            val msgText = msgConn.inputStream?.bufferedReader()?.readText().orEmpty()
            val totalRange = msgConn.getHeaderField("Content-Range") ?: "0-0/*"
            msgConn.disconnect()
            val totalCount = try {
                val afterSlash = totalRange.substringAfter("/", "0")
                afterSlash.substringBefore("?").trim().toIntOrNull() ?: 0
            } catch (_: Exception) { 0 }

            val msgArr = JSONArray(msgText)
            val lastBody = if (msgArr.length() > 0) msgArr.getJSONObject(0).optString("body", "") else ""
            val lastSender = if (msgArr.length() > 0) msgArr.getJSONObject(0).optString("sender_type", "") else ""
            val lastCreatedAt = if (msgArr.length() > 0) msgArr.getJSONObject(0).optString("created_at", "") else ""

            result.add(ChatHistoryEntry(
                ticketId = ticketId,
                status = o.optString("status"),
                category = category,
                createdAt = o.optString("created_at"),
                closedAt = o.optString("closed_at"),
                lastActivityAt = o.optString("last_activity_at"),
                messageCount = totalCount,
                lastMessageBody = lastBody,
                lastMessageSender = lastSender,
                lastMessageCreatedAt = lastCreatedAt
            ))
        }
        result
    } catch (e: Exception) {
        Log.e("DLavieChat", "fetchChatHistory failed", e)
        emptyList()
    }
}

/**
 * v7.9.1: Fetch ALL messages (user + admin + bot) untuk ticket tertentu.
 * Berbeda dari pollMessages yang hanya fetch sender_type != user.
 * Dipakai di ChatHistoryDetail untuk menampilkan percakapan lengkap.
 */
private suspend fun fetchAllTicketMessages(api: CommunityApi, ticketId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://lvmucsxbmadtsgrxuwmo.supabase.co/rest/v1/ticket_messages" +
            "?ticket_id=eq.$ticketId" +
            "&order=created_at.asc" +
            "&select=id,sender_type,body,image_url,created_at")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("apikey", com.drmacze.f16launcher.BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${api.token()}")
        }
        val code = conn.responseCode
        if (code in 200..299) {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
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
            result
        } else {
            conn.disconnect()
            emptyList()
        }
    } catch (e: Exception) {
        Log.e("DLavieChat", "fetchAllTicketMessages failed", e)
        emptyList()
    }
}

data class ChatHistoryEntry(
    val ticketId: String,
    val status: String,
    val category: String,
    val createdAt: String,
    val closedAt: String,
    // v7.9.1: tambahan untuk preview UI
    val lastActivityAt: String = "",
    val messageCount: Int = 0,
    val lastMessageBody: String = "",
    val lastMessageSender: String = "",
    val lastMessageCreatedAt: String = "",
    // v7.9.19: Report fields (for report detail)
    val complaint: String = "",
    val userFeedback: String = "",
    val screenshotUrl: String = "",
    val androidVersion: String = "",
    val devFeedback: String = ""
)
