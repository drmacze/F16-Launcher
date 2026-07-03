# 3 Fixes — Detection / Feed / Login Remake

**Task ID**: 3fixes-detection-feed-login
**Agent**: main
**Branch**: main
**Commit SHA**: c4248953251bb65d6e3ccee8bf2a965dee26ab2b
**Date**: 2026-07-03
**Version**: versionCode 78 / versionName 1.8.2-detection-feed-login

## Goal

Fix 3 masalah di DLavie Launcher:
1. **Game/data detection** — launcher bilang "Data: Belum siap" walaupun user sudah install FIFA 16 + download OBB dari dalam game.
2. **Community feed empty thumbnails** — post card menampilkan kotak gambar kosong kalau `image_url` kosong / "null" string.
3. **Login page remake** — rombak total UI GuidedLoginScreen jadi clean, modern (halftone bg + glass card + white inverted button).

## Files Changed

| File | Change |
|------|--------|
| `app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt` | +71 / -10 lines |
| `app/src/main/java/com/drmacze/f16launcher/DLavieGuidedActivity.kt` | +285 / -151 lines |
| `app/build.gradle` | versionCode 77 → 78, versionName 1.8.1-fixes → 1.8.2-detection-feed-login |

## Implementation Details

### Fix 1 — Game/data detection (`ModernLauncherActivity.kt`)

Added shared helper `isDataReady(): Boolean` (next to `readMarker()`):

```kotlin
fun isDataReady(): Boolean {
    // 1. Marker file (v26 marker via DevPatchEngine)
    if (readMarker().startsWith("v26", ignoreCase = true)) return true
    // 2. OBB files (main / patch)
    val obbMain  = File("/sdcard/Android/obb/com.ea.gp.fifaworld/main.13.com.ea.gp.fifaworld.obb")
    val obbPatch = File("/sdcard/Android/obb/com.ea.gp.fifaworld/patch.26.com.ea.gp.fifaworld.obb")
    if (obbMain.exists() || obbPatch.exists()) return true
    // 3. Game data folder exists & punya konten
    val gameDataDir = File("/sdcard/Android/data/com.ea.gp.fifaworld")
    if (gameDataDir.exists() && (gameDataDir.listFiles()?.isNotEmpty() == true)) return true
    // 4. files/ subfolder punya konten (FIFA sering download data ke sini)
    val filesDir = File("/sdcard/Android/data/com.ea.gp.fifaworld/files")
    if (filesDir.exists() && (filesDir.listFiles()?.isNotEmpty() == true)) return true
    return false
}
```

**Callers updated:**
- `HomeScreen.loadAllData()` — line 1221: `dataReady = isDataReady()`
- `UpdateScreen` — `dataReady` sekarang `var by remember` (bukan derived `val`), di-recompute di `LaunchedEffect(Unit)` + `refresh()` setelah patch apply.

Logic: `dataReady = markerReady || obbReady || filesReady` — fix bug user yang baru install game + download OBB dari dalam game (tidak punya marker file dari DevPatchEngine).

### Fix 2 — Community feed empty thumbnails (`ModernLauncherActivity.kt`)

**Root cause:** `o.optString("image_url", "")` bisa return string `"null"` (bukan `""`) kalau value di JSON adalah `JSONObject.NULL` (org.json behavior). String `"null"` lulus `isNotBlank()` check → AsyncImage render kotak kosong.

**Fix:**
1. **Normalize saat parse** (2 tempat: `CommunityScreen.loadPosts()` line 3278 + `parseFeed()` line 5969):
   ```kotlin
   imageUrl = o.optString("image_url", "").let { raw ->
       val s = raw.trim()
       if (s.isBlank() || s.equals("null", ignoreCase = true)) "" else s
   }
   ```
2. **Defensive check di `FeedPostCard`** (line 3832):
   ```kotlin
   if (post.imageUrl.isNotBlank() && !post.imageUrl.equals("null", ignoreCase = true)) {
       AsyncImage(...)
   }
   ```
3. **Video embed** — sudah guarded oleh `if (videoEmbed != null)` (line 3892). Tidak ada container kosong.

Post tanpa image: compact card (text only, tanpa 16:9 banner).

### Fix 3 — Login page remake (`DLavieGuidedActivity.kt`)

Replaced `GuidedLoginScreen` + `AuthInputField` dengan versi v4.0 monochrome.

**Design:**
- Background: pure black (`0xFF000000`) + HalftoneBackground (subtle, alpha 0.55)
- Layout: centered vertical scroll (`verticalArrangement = Arrangement.Center`)
- Logo: `Text("DLavie")` (36sp Black) + `Icon(Icons.Rounded.Star)` (white 0.8 alpha, 20dp)
- Subtitle: "Masuk untuk lanjut" (13sp, GuideMuted)
- Card: glass `Surface(color = 0xF0111111, border = Color.White.copy(0.08f), shape = RoundedCornerShape(24.dp))`
- Tab switcher (Masuk | Daftar): Box-based, white bg when active, black text; black text on transparent when inactive
- Form fields via new `AuthField` helper:
  - OutlinedTextField dengan `leadingIcon` (Email / Lock / Person / "@")
  - `trailingLabel` text untuk show/hide password toggle
  - RoundedCornerShape(12.dp), white-on-dark theme (focused border 0.3 alpha white, unfocused 0.1 alpha)
- Register fields: confirm password, username (@ prefix), display name, **CountryPickerDropdown** (juga di-update ke monochrome: white text + GuideMuted icon)
- Forgot password link (right-aligned, login mode only)
- Back to login link (left-aligned, forgot mode only)
- Error/success message: inline box dengan subtle background (alpha 0.10) + border (alpha 0.28)
- CTA button: full width, 50dp height, white bg + black text (inverted), RoundedCornerShape(12.dp), spinner + "Memproses..." saat working
- Footer text: "Belum punya akun? Tap Daftar." (12sp GuideMuted center)

**Auth logic preserved (NOT changed):**
- `loginWithPassword(context, email, password)`
- `registerWithUsernamePassword(context, email, password, username, displayName, country)`
- `AuthManager.requestPasswordReset(email)` untuk forgot mode
- `Telemetry.track(...)` untuk login/register events
- `result.session?.let(onLoggedIn)` callback
- Auto-switch dari forgot → login setelah 2.5s kalau sukses
- Validation: email contains @, password length >= 6, username matches `[a-zA-Z0-9_]{3,24}`, displayName length >= 2, password == confirmPassword

**Function signature unchanged:** `private fun GuidedLoginScreen(onLoggedIn: (AuthSession) -> Unit)`

## New Imports Added (DLavieGuidedActivity.kt)

```kotlin
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
```

## Build Status

| Workflow | Run | Status | Conclusion | URL |
|----------|-----|--------|------------|-----|
| Build Debug APK | 276 | completed | **success** | https://github.com/drmacze/F16-Launcher/actions/runs/28633615206 |
| Auto Release (manual trigger) | 8 | completed | **success** | https://github.com/drmacze/F16-Launcher/actions/runs/28633864439 |

Build steps all green:
- Checkout ✓
- Set up JDK 17 ✓
- Set up Gradle 8.7 ✓
- Install Android SDK (platforms;android-35, build-tools;35.0.0) ✓
- Patch guided UX ✓
- Build debug APK (`gradle assembleDebug`) ✓
- Sign APK with fixed keystore (apksigner) ✓
- Upload APK artifact ✓
- Create Release (tag `1.8.2-detection-feed-login`) ✓

## APK Artifact

| Property | Value |
|----------|-------|
| Filename | `app-debug.apk` |
| Size | 19,915,832 bytes (18.99 MB) |
| **File SHA1** | `fb43a6f5ba1eae127ae5a53826098aae46c8d7b8` |
| File SHA256 | `7afbbb69a8efe0ce1115b937849122620c8c4d5e7811e88dafe355e9fd061949` |
| File MD5 | `36270d3bd848006890b6a2abe37bd209` |
| **Signing cert SHA1** (fixed keystore) | `39:21:FE:4D:2F:E3:E1:C9:79:C0:DD:FF:EA:2C:A4:6A:C3:2A:24:61` |
| Signing cert SHA256 | `90:7F:24:39:2A:05:D7:5D:94:EF:ED:4C:CA:6E:21:78:AC:D6:67:19:8E:FA:11:C1:06:D3:49:60:02:00:43:B5` |
| Signer | CN=DLavie, OU=Development, O=drmacze, L=Jakarta, ST=Jakarta, C=ID |
| Sig algo | SHA384withRSA, 2048-bit RSA |

## Release URL

**https://github.com/drmacze/F16-Launcher/releases/tag/1.8.2-detection-feed-login**

Direct APK download:
**https://github.com/drmacze/F16-Launcher/releases/download/1.8.2-detection-feed-login/app-debug.apk**

## Errors Encountered

None. Build succeeded on first push — no compile errors, no test failures.

## Constraints Honored

- ✅ Auth logic (loginWithPassword, registerWithUsernamePassword, forgot password, country picker) — preserved
- ✅ Supabase config — untouched (BuildConfig.SUPABASE_URL, SUPABASE_ANON_KEY)
- ✅ Maintenance overlay — untouched (MaintenanceOverlay, fetchMaintenanceConfig)
- ✅ PIN system — untouched (PinManager, PinLockActivity)
- ✅ Telemetry — preserved (Telemetry.track calls for login/register events)
- ✅ All real data (no mocks)
- ✅ Function signatures preserved — `GuidedLoginScreen(onLoggedIn: (AuthSession) -> Unit)`, `loadAllData()`, `UpdateScreen(api, maintenanceInfo, onNav)`
- ✅ Build success
- ✅ APK signed with fixed keystore (same SHA1 across builds → no update conflict)

## Post-verify Checklist

- [x] Build Debug APK workflow passed (run #276)
- [x] Auto Release workflow passed (run #8)
- [x] Release tag `1.8.2-detection-feed-login` published
- [x] APK asset uploaded (18.99 MB)
- [x] APK SHA1 computed (`fb43a6f5...`)
- [x] Signing cert SHA1 verified via keytool (`39:21:FE:4D:...`)
- [x] Agent-ctx record written (this file)
