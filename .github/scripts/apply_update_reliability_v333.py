from pathlib import Path
import re


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing {label}")
    return text.replace(old, new, 1)


# Version
build = Path("app/build.gradle")
text = build.read_text(encoding="utf-8")
text = re.sub(r"versionCode\s+\d+", "versionCode 333", text, count=1)
text = re.sub(r'versionName\s+"[^"]+"', 'versionName "8.6.1"', text, count=1)
build.write_text(text, encoding="utf-8")


# The version manifest must never use a mutable branch CDN alias. jsDelivr caches
# branch URLs for hours, so a valid but stale response can make an old launcher
# believe it is current. Other public data may still use the CDN-first path.
database = Path("app/src/main/java/com/drmacze/f16launcher/GitHubPublicDatabase.kt")
text = database.read_text(encoding="utf-8")
old = '''    private fun fetchFromJsDelivr(file: PublicFile): String {
        // Ten-minute bucket keeps CDN useful while still allowing reasonably fresh data.
        val revisionBucket = System.currentTimeMillis() / (10 * 60 * 1000L)
'''
new = '''    private fun fetchFromJsDelivr(file: PublicFile): String {
        // A mutable branch CDN alias can remain stale for hours. Version checks
        // must fall through to GitHub Contents API / raw GitHub instead.
        if (file == PublicFile.MANIFEST) {
            throw IllegalStateException("Version manifest bypasses jsDelivr branch cache")
        }
        // Ten-minute bucket keeps CDN useful for non-version public content.
        val revisionBucket = System.currentTimeMillis() / (10 * 60 * 1000L)
'''
if "Version manifest bypasses jsDelivr branch cache" not in text:
    text = replace_once(text, old, new, "jsDelivr manifest bypass")
database.write_text(text, encoding="utf-8")


# A network or parsing failure is not equivalent to "up to date". Propagate a
# typed failure so auto-check callers can ignore it and manual checks can show a
# retry action.
checker = Path("app/src/main/java/com/drmacze/f16launcher/AppUpdateChecker.kt")
text = checker.read_text(encoding="utf-8")
old = '''        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "v325 GitHub manifest check failed: ${e.message}")
            null
        }
'''
new = '''        } catch (e: Throwable) {
            android.util.Log.w("AppUpdate", "GitHub manifest check failed: ${e.message}", e)
            throw IllegalStateException("Tidak dapat memeriksa versi launcher", e)
        }
'''
if "throw IllegalStateException(\"Tidak dapat memeriksa versi launcher\"" not in text:
    text = replace_once(text, old, new, "update failure propagation")

old = '''            val latestCode = launcher.optInt("latest_version_code", 0)
            android.util.Log.i(
'''
new = '''            val latestCode = launcher.optInt("latest_version_code", 0)
            require(latestCode > 0) { "Manifest tidak memiliki latest_version_code yang valid" }
            android.util.Log.i(
'''
if "Manifest tidak memiliki latest_version_code yang valid" not in text:
    text = replace_once(text, old, new, "version code validation")

old = '''            val apkUrl = launcher.optString("apk_url", "")
            if (apkUrl.isBlank()) {
                android.util.Log.w("AppUpdate", "Manifest: apk_url kosong")
                return null
            }
'''
new = '''            val apkUrl = launcher.optString("apk_url", "")
            require(
                apkUrl.startsWith(
                    "https://github.com/drmacze/DLavie-Launcher-Data/releases/download/"
                )
            ) { "Manifest memiliki apk_url yang tidak tepercaya" }
'''
if "Manifest memiliki apk_url yang tidak tepercaya" not in text:
    text = replace_once(text, old, new, "trusted APK URL validation")
checker.write_text(text, encoding="utf-8")


# Localization keys for the new update status surface.
locale_file = Path("app/src/main/java/com/drmacze/f16launcher/LocaleText.kt")
text = locale_file.read_text(encoding="utf-8")
english_anchor = '        "generic.error" to "Something went wrong",\n'
english_extra = '''        "update.title" to "Launcher update",
        "update.checking" to "Checking for updates",
        "update.checking_desc" to "DLavie is reading the latest signed release information.",
        "update.available" to "A new version is available",
        "update.available_desc" to "Update the launcher to receive the latest improvements and fixes.",
        "update.latest" to "Your launcher is up to date",
        "update.latest_desc" to "No newer launcher build is currently published.",
        "update.failed" to "The update check could not be completed",
        "update.failed_desc" to "Your version status was not changed. Check the connection and try again.",
        "update.current_label" to "Installed",
        "update.public_label" to "Available",
        "update.install" to "Update launcher",
        "update.manual" to "Open download page",
        "update.retry" to "Check again",
        "update.close" to "Close",
        "update.version_check_failed" to "Version status is temporarily unavailable.",
'''
if '"update.title"' not in text:
    text = replace_once(text, english_anchor, english_anchor + english_extra, "English update catalog")

id_anchor = '        "generic.error" to "Terjadi kesalahan",\n'
id_extra = '''        "update.title" to "Pembaruan launcher",
        "update.checking" to "Memeriksa pembaruan",
        "update.checking_desc" to "DLavie sedang membaca informasi rilis resmi terbaru.",
        "update.available" to "Versi baru tersedia",
        "update.available_desc" to "Perbarui launcher untuk mendapatkan peningkatan dan perbaikan terbaru.",
        "update.latest" to "Launcher sudah menggunakan versi terbaru",
        "update.latest_desc" to "Belum ada build launcher yang lebih baru saat ini.",
        "update.failed" to "Pemeriksaan pembaruan belum berhasil",
        "update.failed_desc" to "Status versi Anda tidak diubah. Periksa koneksi lalu coba lagi.",
        "update.current_label" to "Terpasang",
        "update.public_label" to "Tersedia",
        "update.install" to "Perbarui launcher",
        "update.manual" to "Buka halaman download",
        "update.retry" to "Periksa lagi",
        "update.close" to "Tutup",
        "update.version_check_failed" to "Status versi sementara tidak dapat diperiksa.",
'''
if '"update.title" to "Pembaruan launcher"' not in text:
    text = replace_once(text, id_anchor, id_anchor + id_extra, "Indonesian update catalog")
locale_file.write_text(text, encoding="utf-8")


# The pre-auth Portal screen must recover from update-check failure instead of
# spinning forever or calling the launcher current.
portal = Path("app/src/main/java/com/drmacze/f16launcher/PortalConnectScreen.kt")
text = portal.read_text(encoding="utf-8")
if "var checkAttempt by remember" not in text:
    text = replace_once(
        text,
        '    var checked by remember { mutableStateOf(false) }\n',
        '    var checked by remember { mutableStateOf(false) }\n    var checkAttempt by remember { mutableStateOf(0) }\n    var checkFailed by remember { mutableStateOf(false) }\n',
        "Portal update check state",
    )

old = '''    LaunchedEffect(Unit) {
        updateInfo = withContext(Dispatchers.IO) {
            AppUpdateChecker.checkForUpdate(context = context)
        }
        checked = true
    }
'''
new = '''    LaunchedEffect(checkAttempt) {
        checked = false
        checkFailed = false
        val result = runCatching {
            withContext(Dispatchers.IO) {
                AppUpdateChecker.checkForUpdate(context = context)
            }
        }
        updateInfo = result.getOrNull()
        checkFailed = result.isFailure
        checked = true
    }
'''
if "LaunchedEffect(checkAttempt)" not in text:
    text = replace_once(text, old, new, "Portal update check recovery")

old = '''        when {
            !checked -> PortalVersionStatus(checking = true)
            updateInfo != null -> PortalUpdateCard(
'''
new = '''        when {
            !checked -> PortalVersionStatus(checking = true)
            checkFailed -> PortalVersionCheckError(
                onRetry = { checkAttempt += 1 },
            )
            updateInfo != null -> PortalUpdateCard(
'''
if "checkFailed -> PortalVersionCheckError" not in text:
    text = replace_once(text, old, new, "Portal error branch")

marker = '''@Composable
private fun PortalVersionStatus(checking: Boolean) {
'''
error_component = '''@Composable
private fun PortalVersionCheckError(onRetry: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PortalSurfaceRaised,
        border = BorderStroke(1.dp, PortalBorder),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = PortalMuted,
                modifier = Modifier.size(20.dp),
            )
            Text(
                LocaleText.get(context, "update.version_check_failed"),
                modifier = Modifier.weight(1f),
                color = PortalMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
            Text(
                LocaleText.get(context, "update.retry"),
                modifier = Modifier.clickable(onClick = onRetry),
                color = PortalText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

'''
if "private fun PortalVersionCheckError" not in text:
    text = replace_once(text, marker, error_component + marker, "Portal error component")
portal.write_text(text, encoding="utf-8")


# Replace the old theatrical checker. The old implementation treated null as
# success, auto-dismissed, and kept the streaming status text visible beside the
# final state. The new screen has one source of truth and one visible message.
check_screen = Path("app/src/main/java/com/drmacze/f16launcher/CheckUpdateScreen.kt")
check_screen.write_text('''package com.drmacze.f16launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class UpdateCheckStage {
    CHECKING,
    UPDATE_AVAILABLE,
    UP_TO_DATE,
    ERROR,
}

private val UpdateBackground = Color(0xFF080808)
private val UpdateSurface = Color(0xFF111111)
private val UpdateBorder = Color(0x1FFFFFFF)
private val UpdateText = Color.White
private val UpdateMuted = Color(0xFF969696)
private val UpdateGreen = Color(0xFF35D07F)
private val UpdateRed = Color(0xFFFF5B5B)

@Composable
fun CheckUpdateScreen(
    api: CommunityApi,
    onDismiss: () -> Unit,
    onUpdateAvailable: (AppUpdateChecker.UpdateInfo) -> Unit,
) {
    val context = LocalContext.current
    var attempt by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf(UpdateCheckStage.CHECKING) }
    var updateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(attempt) {
        stage = UpdateCheckStage.CHECKING
        updateInfo = null
        errorMessage = ""
        val startedAt = System.currentTimeMillis()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                AppUpdateChecker.checkForUpdate(api = api, context = context)
            }
        }
        val remaining = 450L - (System.currentTimeMillis() - startedAt)
        if (remaining > 0L) delay(remaining)

        result.onSuccess { info ->
            updateInfo = info
            stage = if (info != null && info.isUpdateAvailable) {
                UpdateCheckStage.UPDATE_AVAILABLE
            } else {
                UpdateCheckStage.UP_TO_DATE
            }
        }.onFailure { error ->
            errorMessage = error.message.orEmpty()
            stage = UpdateCheckStage.ERROR
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UpdateBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        LocaleText.get(context, "update.title"),
                        color = UpdateText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "DLavie ${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                        color = UpdateMuted,
                        fontSize = 11.sp,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = UpdateMuted)
                }
            }

            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                UpdateStatusIcon(stage)
                Spacer(Modifier.height(22.dp))
                Text(
                    text = when (stage) {
                        UpdateCheckStage.CHECKING -> LocaleText.get(context, "update.checking")
                        UpdateCheckStage.UPDATE_AVAILABLE -> LocaleText.get(context, "update.available")
                        UpdateCheckStage.UP_TO_DATE -> LocaleText.get(context, "update.latest")
                        UpdateCheckStage.ERROR -> LocaleText.get(context, "update.failed")
                    },
                    color = UpdateText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (stage) {
                        UpdateCheckStage.CHECKING -> LocaleText.get(context, "update.checking_desc")
                        UpdateCheckStage.UPDATE_AVAILABLE -> LocaleText.get(context, "update.available_desc")
                        UpdateCheckStage.UP_TO_DATE -> LocaleText.get(context, "update.latest_desc")
                        UpdateCheckStage.ERROR -> LocaleText.get(context, "update.failed_desc")
                    },
                    color = UpdateMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }

            Spacer(Modifier.height(30.dp))

            VersionComparisonCard(
                latest = updateInfo,
                checking = stage == UpdateCheckStage.CHECKING,
            )

            if (stage == UpdateCheckStage.ERROR && errorMessage.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    errorMessage,
                    modifier = Modifier.fillMaxWidth(),
                    color = UpdateMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }

            Spacer(Modifier.weight(1f))

            when (stage) {
                UpdateCheckStage.CHECKING -> {
                    Text(
                        LocaleText.get(context, "update.checking"),
                        modifier = Modifier.fillMaxWidth(),
                        color = UpdateMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                UpdateCheckStage.UPDATE_AVAILABLE -> {
                    Button(
                        onClick = { updateInfo?.let(onUpdateAvailable) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(LocaleText.get(context, "update.install"), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { AppUpdateChecker.openWebsite(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, UpdateBorder),
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = UpdateText)
                        Spacer(Modifier.size(8.dp))
                        Text(LocaleText.get(context, "update.manual"), color = UpdateText)
                    }
                }
                UpdateCheckStage.UP_TO_DATE -> {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Text(LocaleText.get(context, "update.close"), fontWeight = FontWeight.Bold)
                    }
                }
                UpdateCheckStage.ERROR -> {
                    Button(
                        onClick = { attempt += 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(LocaleText.get(context, "update.retry"), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        LocaleText.get(context, "update.close"),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismiss)
                            .padding(10.dp),
                        color = UpdateMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusIcon(stage: UpdateCheckStage) {
    Surface(
        modifier = Modifier.size(76.dp),
        shape = CircleShape,
        color = when (stage) {
            UpdateCheckStage.CHECKING -> Color.White.copy(alpha = 0.07f)
            UpdateCheckStage.UPDATE_AVAILABLE -> Color.White.copy(alpha = 0.09f)
            UpdateCheckStage.UP_TO_DATE -> UpdateGreen.copy(alpha = 0.14f)
            UpdateCheckStage.ERROR -> UpdateRed.copy(alpha = 0.14f)
        },
        border = BorderStroke(1.dp, UpdateBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (stage) {
                UpdateCheckStage.CHECKING -> CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                UpdateCheckStage.UPDATE_AVAILABLE -> Icon(
                    Icons.Rounded.SystemUpdate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
                UpdateCheckStage.UP_TO_DATE -> Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = UpdateGreen,
                    modifier = Modifier.size(36.dp),
                )
                UpdateCheckStage.ERROR -> Icon(
                    Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = UpdateRed,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
private fun VersionComparisonCard(
    latest: AppUpdateChecker.UpdateInfo?,
    checking: Boolean,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = UpdateSurface,
        border = BorderStroke(1.dp, UpdateBorder),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VersionCell(
                modifier = Modifier.weight(1f),
                label = LocaleText.get(context, "update.current_label"),
                version = BuildConfig.VERSION_NAME,
                build = BuildConfig.VERSION_CODE.toString(),
            )
            VersionCell(
                modifier = Modifier.weight(1f),
                label = LocaleText.get(context, "update.public_label"),
                version = when {
                    checking -> "—"
                    latest != null -> latest.versionName
                    else -> BuildConfig.VERSION_NAME
                },
                build = when {
                    checking -> "—"
                    latest != null -> latest.versionCode.toString()
                    else -> BuildConfig.VERSION_CODE.toString()
                },
            )
        }
    }
}

@Composable
private fun VersionCell(
    modifier: Modifier,
    label: String,
    version: String,
    build: String,
) {
    Column(modifier = modifier) {
        Text(label.uppercase(), color = UpdateMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(version, color = UpdateText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Build $build", color = UpdateMuted, fontSize = 10.sp)
    }
}

@Composable
fun UpdateWarningToast(
    versionName: String,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFC107),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = Color.Black)
            Column(modifier = Modifier.weight(1f)) {
                Text("Pembaruan tersedia", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Versi $versionName siap dipasang", color = Color.Black.copy(alpha = 0.72f), fontSize = 11.sp)
            }
            Text(
                "Periksa",
                modifier = Modifier.clickable(onClick = onCheckUpdate),
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Black)
            }
        }
    }
}
''', encoding="utf-8")


# Keep the existing localization workflow version-agnostic after v332.
i18n_workflow = Path(".github/workflows/apply-i18n-v2.yml")
text = i18n_workflow.read_text(encoding="utf-8")
text = text.replace(
    "          grep -F 'versionCode 332' app/build.gradle\n          grep -F 'versionName \"8.6.0\"' app/build.gradle\n",
    "          VERSION_CODE=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*/\\1/p' app/build.gradle | head -1)\n          test -n \"$VERSION_CODE\"\n          test \"$VERSION_CODE\" -ge 332\n",
)
text = text.replace(
    "            && grep -Fq 'versionCode 332' app/build.gradle; then",
    "            && VERSION_CODE=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*/\\1/p' app/build.gradle | head -1) \\\n            && test \"$VERSION_CODE\" -ge 332; then",
)
i18n_workflow.write_text(text, encoding="utf-8")
