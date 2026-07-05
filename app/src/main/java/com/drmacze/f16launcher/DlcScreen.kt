package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

// ─── DLC Page (Redesign v2 — Mod Patches Only) ───────────────────────────────
//
// PURPOSE:
//   DLC page SEKARANG khusus untuk mod patch game saja.
//   - User upload mod gameplay via Dev Dashboard → muncul di sini
//   - User tap "Update" → apply patch via existing DevPatchEngine
//
//   Untuk download APK + data + OBB → pindah ke GameHub (floating panel
//   muncul saat user tap game card). Lihat GameActionPanel.kt
//
// Sections:
//   1. Header (DLC & Mods)
//   2. Storage Permission Card (only if MANAGE_EXTERNAL_STORAGE not granted)
//   3. Currently Installed Patch (status card)
//   4. Available Mods List (from Supabase update_posts)
//      - Each card: title, version, release date, "Update" / "Installed" button
//      - Critical patches highlighted with red accent
// ──────────────────────────────────────────────────────────────────────────────

// Design tokens (matching existing monochrome theme)
private val DlcBlack     = Color(0xFF000000)
private val DlcCardBg    = Color(0xFF0A0A0A)
private val DlcCardBgAlt = Color(0xFF101010)
private val DlcBorder    = Color(0x1AFFFFFF)
private val DlcBorderHi  = Color(0x33FFFFFF)
private val DlcText      = Color(0xFFFFFFFF)
private val DlcSubText   = Color(0xFFAAAAAA)
private val DlcMuted     = Color(0xFF666666)
private val DlcGreen     = Color(0xFFFFFFFF)
private val DlcRed       = Color(0xFFFF5555)
private val DlcYellow    = Color(0xFFFFFF88)

data class ModPatch(
    val id: String,
    val versionCode: Int,
    val versionName: String,
    val title: String,
    val body: String,
    val releaseNotes: List<String>,
    val knownIssues: List<String>,
    val patchUrl: String,
    val patchSha256: String,
    val patchSizeBytes: Long,
    val critical: Boolean,
    val restartRequired: Boolean,
    val riskLevel: String,
    val createdAt: String
)

@Composable
fun DlcScreen(
    api: CommunityApi,
    maintenanceInfo: MaintenanceInfo? = null,
    onNav: (Page) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Storage permission state ──
    var filesAccessGranted by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                filesAccessGranted = StorageAccess.isGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        try { kotlinx.coroutines.awaitCancellation() } finally { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { filesAccessGranted = StorageAccess.isGranted() }

    // ── Currently installed patch state ──
    var installedMarker by remember { mutableStateOf("") }
    var gameInstalled by remember { mutableStateOf(false) }
    var loadingStatus by remember { mutableStateOf(true) }

    // ── Available mods list ──
    val mods = remember { mutableStateListOf<ModPatch>() }
    var loadingMods by remember { mutableStateOf(true) }
    var modsError by remember { mutableStateOf("") }

    // ── Refresh functions ──
    fun refreshStatus() {
        scope.launch(Dispatchers.IO) {
            installedMarker = readMarker()
            gameInstalled = isGameInstalled(context)
            withContext(Dispatchers.Main) { loadingStatus = false }
        }
    }
    fun refreshMods() {
        loadingMods = true
        modsError = ""
        scope.launch(Dispatchers.IO) {
            try {
                val arr = api.fetchUpdatePosts()
                mods.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    mods.add(
                        ModPatch(
                            id = o.optString("id", ""),
                            versionCode = o.optInt("version_code", 0),
                            versionName = o.optString("version_name", ""),
                            title = o.optString("title", "Untitled Mod"),
                            body = o.optString("body", ""),
                            releaseNotes = jsonArrayToStringList(o.optJSONArray("release_notes")),
                            knownIssues = jsonArrayToStringList(o.optJSONArray("known_issues")),
                            patchUrl = o.optString("patch_url", ""),
                            patchSha256 = o.optString("patch_sha256", ""),
                            patchSizeBytes = o.optLong("patch_size_bytes", 0L),
                            critical = o.optBoolean("critical", false),
                            restartRequired = o.optBoolean("restart_game_required", false),
                            riskLevel = o.optString("risk_level", "low"),
                            createdAt = o.optString("created_at", "")
                        )
                    )
                }
            } catch (e: Throwable) {
                modsError = e.message ?: "Gagal memuat mod patches"
            }
            withContext(Dispatchers.Main) { loadingMods = false }
        }
    }

    // ── Initial load ──
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            installedMarker = readMarker()
            gameInstalled = isGameInstalled(context)
        }
        loadingStatus = false
        refreshMods()
    }

    // ── Layout ──
    Column(
        Modifier
            .fillMaxSize()
            .background(DlcBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ─── Header ───
        DlcHeader()

        // v7.2.4: Removed storage permission card — permission auto-requested saat
        // user tap "Update" di mod card (per user complaint: "jangan di pisah pisah gitu").

        // ─── Currently Installed Patch Status ───
        DlcInstalledStatusCard(
            loading = loadingStatus,
            installedMarker = installedMarker,
            gameInstalled = gameInstalled,
            onRefresh = { refreshStatus() }
        )

        // ─── Available Mods Section ───
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(width = 3.dp, height = 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DlcText)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Mod Patches Tersedia",
                color = DlcText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                modifier = Modifier.weight(1f)
            )
            IconButtonRefresh { refreshMods() }
        }

        // ─── Loading State ───
        if (loadingMods) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DlcCardBg),
                border = BorderStroke(1.dp, DlcBorder)
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DlcText
                    )
                    Text(
                        "Memuat mod patches...",
                        color = DlcSubText,
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }

        // ─── Error State ───
        if (modsError.isNotBlank() && !loadingMods) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DlcRed.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, DlcRed.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Gagal Memuat Mods",
                        color = DlcRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        modsError,
                        color = DlcSubText,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily
                    )
                    Button(
                        onClick = { refreshMods() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DlcText, contentColor = DlcBlack)
                    ) {
                        Text("Coba Lagi", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
            }
        }

        // ─── Empty State ───
        if (mods.isEmpty() && !loadingMods && modsError.isBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DlcCardBg),
                border = BorderStroke(1.dp, DlcBorder)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Extension, null, tint = DlcMuted, modifier = Modifier.size(40.dp))
                    Text(
                        "Belum Ada Mod Patches",
                        color = DlcText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        "Mod patches yang di-upload oleh developer akan muncul di sini.\nTap 'Update' untuk apply mod terbaru.",
                        color = DlcSubText,
                        fontSize = 12.sp,
                        fontFamily = InterFontFamily,
                        lineHeight = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // ─── Mods List ───
        if (mods.isNotEmpty()) {
            mods.forEach { mod ->
                DlcModCard(
                    mod = mod,
                    isInstalled = installedMarker.startsWith("v26") && installedMarker.contains(mod.versionName, ignoreCase = true),
                    canApply = gameInstalled,  // permission auto-requested on tap
                    onApply = {
                        // v7.2.4: Auto-request permission saat tap Update (no separate card)
                        if (!StorageAccess.isGranted()) {
                            StorageAccess.request(context)
                            return@DlcModCard
                        }
                        // Apply via existing patch system — open UpdateScreen overlay
                        onNav(Page.DLC)
                    }
                )
            }
        }

        // ─── Footer ───
        Spacer(Modifier.height(4.dp))
        Text(
            "DLavie Launcher v7.0.7 — Mod Manager",
            color = DlcMuted,
            fontSize = 11.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

// ─── UI Components ────────────────────────────────────────────────────────────

@Composable
private fun DlcHeader() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(DlcCardBgAlt, RoundedCornerShape(14.dp))
                .border(1.dp, DlcBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Extension, null, tint = DlcText, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "DLC & Mods",
                color = DlcText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = InterFontFamily
            )
            Text(
                "Mod patches untuk FIFA 16 — download & apply dari sini",
                color = DlcSubText,
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// v7.2.4: DlcStoragePermissionCard removed — permission auto-requested saat
// user tap "Update" di DlcModCard (per user complaint: "jangan di pisah pisah gitu").


@Composable
private fun DlcInstalledStatusCard(
    loading: Boolean,
    installedMarker: String,
    gameInstalled: Boolean,
    onRefresh: () -> Unit
) {
    val isPatched = installedMarker.startsWith("v26")
    val statusColor by animateColorAsState(
        if (loading) DlcYellow else if (isPatched && gameInstalled) DlcGreen else DlcRed,
        tween(400), label = "status_c"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DlcCardBg),
        border = BorderStroke(1.dp, DlcBorder)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp)
                        .background(DlcCardBgAlt, RoundedCornerShape(12.dp))
                        .border(1.dp, DlcBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SportsSoccer, null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Status Mod FIFA 16",
                        color = DlcText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        if (loading) "Memeriksa status..."
                        else if (!gameInstalled) "Game belum terinstall"
                        else if (isPatched) "Mod terpasang: ${installedMarker.take(20)}"
                        else "Belum ada mod terpasang",
                        color = DlcSubText,
                        fontSize = 11.sp,
                        fontFamily = InterFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButtonRefresh(onRefresh)
            }

            // Status tiles
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DlcStatusTileCompact(
                    label = "Game",
                    value = if (loading) "…" else if (gameInstalled) "Terinstall" else "Belum ada",
                    ok = gameInstalled,
                    modifier = Modifier.weight(1f)
                )
                DlcStatusTileCompact(
                    label = "Mod",
                    value = if (loading) "…" else if (isPatched) "Aktif" else "Tidak ada",
                    ok = isPatched,
                    modifier = Modifier.weight(1f)
                )
                DlcStatusTileCompact(
                    label = "Izin File",
                    value = if (StorageAccess.isGranted()) "Aktif" else "Belum",
                    ok = StorageAccess.isGranted(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DlcStatusTileCompact(
    label: String,
    value: String,
    ok: Boolean,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(if (ok) DlcGreen else DlcRed, tween(400), label = "tile_c")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DlcCardBgAlt,
        border = BorderStroke(1.dp, if (ok) DlcBorder else DlcRed.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = DlcSubText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily, maxLines = 1)
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DlcModCard(
    mod: ModPatch,
    isInstalled: Boolean,
    canApply: Boolean,
    onApply: () -> Unit
) {
    val accentColor = if (mod.critical) DlcRed else DlcText
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(mod.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            dateFormat.format(sdf.parse(mod.createdAt) ?: java.util.Date())
        } catch (_: Throwable) { mod.createdAt.take(10) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DlcCardBg),
        border = BorderStroke(1.dp, if (mod.critical) DlcRed.copy(alpha = 0.4f) else DlcBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(36.dp)
                        .background(DlcCardBgAlt, RoundedCornerShape(10.dp))
                        .border(1.dp, DlcBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (mod.critical) Icons.Rounded.LocalFireDepartment else Icons.Rounded.NewReleases,
                        null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            mod.versionName.ifBlank { "v${mod.versionCode}" },
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = InterFontFamily
                        )
                        Spacer(Modifier.width(6.dp))
                        if (mod.critical) {
                            Surface(
                                color = DlcRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, DlcRed.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    "CRITICAL",
                                    color = DlcRed,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            formattedDate,
                            color = DlcMuted,
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                    Text(
                        mod.title,
                        color = DlcText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Body
            if (mod.body.isNotBlank()) {
                Text(
                    mod.body,
                    color = DlcSubText,
                    fontSize = 12.sp,
                    fontFamily = InterFontFamily,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Release notes
            if (mod.releaseNotes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    mod.releaseNotes.take(3).forEach { note ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = DlcText, fontSize = 11.sp)
                            Text(
                                note,
                                color = DlcSubText,
                                fontSize = 11.sp,
                                fontFamily = InterFontFamily,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    if (mod.releaseNotes.size > 3) {
                        Text(
                            "+${mod.releaseNotes.size - 3} lainnya",
                            color = DlcMuted,
                            fontSize = 10.sp,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(start = 14.dp)
                        )
                    }
                }
            }

            // Known issues warning
            if (mod.knownIssues.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Warning, null, tint = DlcYellow, modifier = Modifier.size(12.dp))
                    Text(
                        "${mod.knownIssues.size} known issue(s)",
                        color = DlcYellow,
                        fontSize = 10.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }

            // Footer: size + button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mod.patchSizeBytes > 0) {
                    Text(
                        formatBytesHelper(mod.patchSizeBytes),
                        color = DlcMuted,
                        fontSize = 10.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Button(
                    onClick = onApply,
                    enabled = canApply && !isInstalled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInstalled) DlcCardBgAlt else accentColor,
                        contentColor = if (isInstalled) DlcGreen else DlcBlack,
                        disabledContainerColor = DlcCardBgAlt,
                        disabledContentColor = DlcMuted
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        if (isInstalled) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                        null,
                        tint = if (isInstalled) DlcGreen else (if (canApply) DlcBlack else DlcMuted),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isInstalled) "Terpasang" else "Update",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun IconButtonRefresh(onClick: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        Modifier.size(36.dp)
            .clip(CircleShape)
            .border(1.dp, DlcBorder, CircleShape)
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Refresh, null, tint = DlcSubText, modifier = Modifier.size(18.dp))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun jsonArrayToStringList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
}

private fun formatBytesHelper(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unit = 0
    while (size >= 1024 && unit < units.lastIndex) {
        size /= 1024
        unit++
    }
    return String.format(Locale.US, "%.1f %s", size, units[unit])
}
