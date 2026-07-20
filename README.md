# DLavie F16 Launcher

> ⚠️ **README ini WAJIB dibaca oleh AI agent sebelum melakukan perubahan apa pun di repo ini.**
> Jika kamu AI agent dan tidak membaca README ini, kamu **akan membuat kesalahan** yang sudah ada dokumennya di sini.

Android launcher (Kotlin + Jetpack Compose) untuk FIFA 16 Mobile mod `com.ea.gp.fifaworld`.

- **Package**: `com.drmacze.f16launcher`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 16)
- **Current version**: `8.0.26-fix-news-beranda` (versionCode `324`) — see `app/build.gradle`

---

## 🚨 CRITICAL RULES FOR AI AGENTS

### 1. Jangan tebak-tebakan struktur data

Sumber kebenaran tunggal untuk setiap data sudah didefinisikan di bawah.
**JANGAN** improvisasi. Jika sebuah konstanta sudah didefinisikan, **pakai apa adanya**.

### 2. Supabase sudah mati — jangan pakai untuk data publik

Supabase project `lvmucsxbmadtsgrxuwmo` sudah exceed egress quota sejak ~July 2026.
- ❌ **JANGAN** tambah code baru yang query Supabase untuk data publik (news, banner, version, dll).
- ❌ **JANGAN** edit workflow untuk insert ke Supabase table `app_releases`.
- ✅ **HANYA** gunakan Supabase untuk fitur auth + community (user login, posts, follows) — itu masih work via RLS.
- ✅ Untuk **data publik** (manifest, news, banner, version), gunakan **GitHub raw** atau **jsdelivr CDN**.

### 3. Sumber versi launcher: `manifest.json` di repo `DLavie-Launcher-Data`

```text
https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json
```

`AppUpdateChecker.kt` membaca file ini. **JANGAN** ubah URL ini tanpa update konstanta `MANIFEST_URL` di `AppUpdateChecker.kt`.

### 4. Version bump protocol

Setiap kali melakukan perubahan code:
1. Edit `app/build.gradle` → bump `versionCode` (+1) dan `versionName` (deskriptif).
2. Commit dengan prefix `feat(vXXX):` atau `fix(vXXX):`.
3. Push ke `main` → workflow `auto-release.yml` otomatis build + upload APK + update manifest.
4. **JANGAN** update `manifest.json` manual kecuali workflow gagal (lihat troubleshooting).

### 5. Jangan ubah workflow `auto-release.yml` tanpa reason kuat

Workflow ini sudah diperbaiki berkali-kali. Step 12 (Create GitHub Release) pakai **direct curl + 5 retries** karena `softprops/action-gh-release@v2` sering 503. Jangan revert ke action itu.

### 6. Signing key FIXED

APK ditandatangani dengan keystore yang sama untuk semua build (lihat secrets: `SIGNING_KEYSTORE`, `SIGNING_KEY_ALIAS`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_PASSWORD`). Jangan ubah signing config — user sudah install APK dengan signature ini, ganti signature = "App not installed".

### 7. FIFA 16 APK TIDAK BOLEH di-repack

`DLavie26.apk` di repo `DLavie-Launcher-Data` adalah APK ORIGINAL dari ChatGPT. Signature EA intact. Repack akan break signature → user can't install. JANGAN sentuh APK ini.

---

## 📁 Struktur Repo

```
F16-Launcher/
├── app/
│   ├── build.gradle                          # versionCode + versionName + buildConfig
│   └── src/main/java/com/drmacze/f16launcher/
│       ├── ModernLauncherActivity.kt         # Main host (10438+ lines, Page enum nav)
│       ├── AppUpdateChecker.kt               # Manifest-only update system (no Supabase)
│       ├── DLavieGameHub.kt                  # Game hub overlay (landscape, immersive)
│       ├── NewsScreen.kt                     # News fetcher (GitHub-first, Supabase dead)
│       ├── CheckUpdateScreen.kt              # Streaming-text update screen
│       ├── CommunityApi.java                 # Supabase auth + community (still used)
│       ├── DlcScreen.kt                      # DLC download manager
│       ├── SettingsScreen.kt                 # Settings (Cek Pembaruan button)
│       ├── EditProfileScreen.kt
│       ├── ShizukuSetup.kt                   # Shizuku permission flow
│       ├── ManifestApi.kt                    # Game data manifest fetcher
│       └── ... (70 total files)
├── .github/workflows/
│   ├── auto-release.yml                      # MAIN: build + sign + upload APK + update manifest
│   ├── build-debug-apk.yml                   # Quick debug build (no release)
│   └── ... (other workflows, mostly legacy)
└── README.md (this file)
```

---

## 🔄 Update System (v322+)

### Arsitektur

```
User Device (App)
    │
    ├─ AppUpdateChecker.checkForUpdate()
    │   └─ GET https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json
    │       (cache-bust via ?t=timestamp)
    │
    ├─ Compare latest_version_code vs BuildConfig.VERSION_CODE
    │
    ├─ If gap >= FORCE_UPDATE_THRESHOLD (1):
    │   └─ Show force-update popup (non-dismissable)
    │       └─ Button "Install Latest Version" → open https://drmacze.github.io/dlavie-web/
    │
    └─ If gap < threshold but > 0:
        └─ Show normal update popup (dismissable)
            └─ Button "Update Now" → in-app download + install
```

### Konstanta penting (di `AppUpdateChecker.kt`)

```kotlin
const val DLAVIE_WEBSITE_URL = "https://drmacze.github.io/dlavie-web/"
private const val MANIFEST_URL = "https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json"
private const val FORCE_UPDATE_THRESHOLD = 1  // ANY gap = force update
```

### Update popup behavior

- **forceUpdate=true** (gap >= 1): popup merah "Update Wajib!", tombol "Install Latest Version" buka website, tidak bisa dismiss.
- **forceUpdate=false** (gap == 0): tidak ada popup (sudah latest).
- Popup dirender di root composable `DLavieModernApp()` — muncul SEBELUM login check, jadi semua user (termasuk belum login) lihat popup.

---

## 📰 News System (v324+)

`NewsScreen.kt` fetch dari **GitHub FIRST** (Supabase mati):

```text
1. cdn.jsdelivr.net/gh/drmacze/DLavie-Launcher-Data@main/banner_slides.json
   cdn.jsdelivr.net/gh/drmacze/DLavie-Launcher-Data@main/news_posts.json
   (jsdelivr CDN, refresh ~10 min)

2. Fallback: api.github.com/repos/drmacze/DLavie-Launcher-Data/contents/<file>
   (always fresh, Base64 decode, no CDN cache)
```

### Format `banner_slides.json`

```json
[
  {
    "id": 1,
    "sort_order": 1,
    "title": "Slide Title",
    "subtitle": "Slide subtitle",
    "media_type": "image",
    "media_url": "https://...",
    "link_url": "https://...",
    "duration_seconds": 5,
    "starts_at": "2026-07-20T00:00:00Z",
    "ends_at": "2026-12-31T23:59:59Z",
    "is_active": true
  }
]
```

### Format `news_posts.json`

```json
[
  {
    "id": 1,
    "title": "Post Title",
    "body": "Post body (Markdown-style \\n for newlines)",
    "footer_text": "Tim DLavie",
    "image_url": "",
    "label_type": "info",
    "official": true,
    "scheduled_at": null,
    "published_at": "2026-07-20T10:00:00Z",
    "created_at": "2026-07-20T10:00:00Z",
    "is_active": true
  }
]
```

### Aturan konten news (WAJIB)

- ❌ **JANGAN** sebut technical jargon: "bug", "fix", "quota exceeded", "Supabase", "JSON parse error"
- ❌ **JANGAN** sebut nomor versi spesifik di title (kecuali major release seperti "v8.0")
- ❌ **JANGAN** sebut "Update Wajib" di news (terlalu aggressive)
- ✅ Tulis professional, user-facing, fokus benefit
- ✅ Bahasa Indonesia (default) atau English (optional)
- ✅ Signed "Tim DLavie"

---

## 🎮 Game Data System

Game data FIFA 16 (APK + OBB main + OBB patch) di-host di GitHub Release `DLavie-Launcher-Data` repo, tag `v26`.

```text
APK:    https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/DLavie26.apk
OBB Main:  https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/main.13.com.ea.gp.fifaworld.obb
OBB Patch: https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/patch.26.com.ea.gp.fifaworld.obb
```

### SHA-256 (jangan ubah)

```text
APK:        acb0ce50554d13d6d36aa75e7e84ade69e52f4b130f8316af4505cc255acd176
OBB Main:   fe3e66c5e8c804656d8ee9ca62ace64a1fe968669f5c397b23ce174b0b8c720c
OBB Patch:  bdca1604e7fc8dc80d96d656ae0e21ff3bd1ccf75a62ecaab0109dd269ef38a
```

`ManifestApi.kt` membaca `manifest.json` `game_data` section untuk download + verify SHA.

---

## 🏗️ CI/CD: `auto-release.yml`

Trigger: push ke `main` (yang mengubah file di `app/**` atau `build.gradle`).

### Steps

1. Checkout code
2. Setup JDK 17 + Gradle
3. Install Android SDK
4. Read `versionCode` + `versionName` from `app/build.gradle`
5. Check if release `v{versionCode}` already exists (skip upload if yes)
6. Patch guided UX
7. Build debug APK
8. Sign APK dengan fixed keystore
9. **Create GitHub Release** — direct curl + 5 retries (jangan pakai softprops action!)
10. Upload APK artifact
11. Upload APK ke `DLavie-Launcher-Data` repo release `v26` (tag `v26`, asset name `DLavie26-Launcher-v{versionCode}.apk`)
12. Supabase insert (NON-BLOCKING, `continue-on-error: true`) — boleh gagal
13. Update `manifest.json` di `DLavie-Launcher-Data` (latest_version_code, apk_url, release_notes)

### Manual manifest update (jika workflow gagal)

Lihat `/home/z/my-project/scripts/upload_v324_final.py` sebagai template. Pakai GitHub API langsung:

```python
import urllib.request, json, base64
# 1. Fetch manifest.json (get SHA)
# 2. Modify latest_version_code, latest_version_name, apk_url
# 3. PUT with new content + SHA
```

---

## 🌐 External URLs (konsisten lintas repo)

| Resource | URL |
|----------|-----|
| Website | `https://drmacze.github.io/dlavie-web/` |
| Manifest | `https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json` |
| Banner slides JSON | `https://cdn.jsdelivr.net/gh/drmacze/DLavie-Launcher-Data@main/banner_slides.json` |
| News posts JSON | `https://cdn.jsdelivr.net/gh/drmacze/DLavie-Launcher-Data@main/news_posts.json` |
| Game data base | `https://github.com/drmacze/DLavie-Launcher-Data/releases/download/v26/` |
| Dev Dashboard | `https://drmacze.github.io/DLavie-Dev-Dashboard/` |
| Supabase URL | `https://lvmucsxbmadtsgrxuwmo.supabase.co` (auth + community only) |

---

## 🔐 Secrets (di GitHub repo settings)

| Secret | Usage |
|--------|-------|
| `SIGNING_KEYSTORE` | Base64-encoded keystore file |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEYSTORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_PASSWORD` | Key password |
| `DLAVIE_DATA_TOKEN` | PAT untuk push ke `DLavie-Launcher-Data` repo |
| `SUPABASE_URL` | `https://lvmucsxbmadtsgrxuwmo.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | Service role key (untuk insert ke app_releases — boleh gagal) |
| `SUPABASE_ANON_KEY` | Anon key (embedded di APK) |

---

## 🐛 Troubleshooting

### Build workflow gagal di step "Create GitHub Release"

**Penyebab**: GitHub API 503 transient error.

**Fix**:
1. Hapus empty release `v{versionCode}` di GitHub UI atau via API:
   ```bash
   curl -X DELETE -H "Authorization: token <PAT>" \
     https://api.github.com/repos/drmacze/F16-Launcher/releases/<release_id>
   ```
2. Hapus tag `v{versionCode}`:
   ```bash
   curl -X DELETE -H "Authorization: token <PAT>" \
     https://api.github.com/repos/drmacze/F16-Launcher/git/refs/tags/v<versionCode>
   ```
3. Re-trigger workflow manual via Actions UI atau API dispatch.

### News tidak muncul di beranda

**Cek**:
1. `banner_slides.json` / `news_posts.json` di `DLavie-Launcher-Data` repo — harus **valid JSON array** (bukan pesan error Supabase).
2. Verifikasi via API: `https://api.github.com/repos/drmacze/DLavie-Launcher-Data/contents/banner_slides.json` — decode Base64, pastikan starts with `[`.
3. Jika file korup, replace dengan valid JSON (lihat `/home/z/my-project/scripts/fix_news_content_professional.py`).

### Update popup tidak muncul

**Cek**:
1. `manifest.json` `launcher.latest_version_code` > `BuildConfig.VERSION_CODE` user.
2. User mungkin sudah dismiss versi ini (cek SharedPreferences `dlavie_update_prefs` → `dismissed_version_code`).
3. forceUpdate=true (gap >= 1) akan ignore dismissed state.

### Landscape bug saat klik tab Community/Profile/DLC

`DLavieGameHub.kt` set `requestedOrientation = SENSOR_LANDSCAPE` di DisposableEffect. Pastikan `onDispose` restore ke `UNSPECIFIED`. Jangan tambah `SENSOR_LANDSCAPE` di tempat lain.

---

## 📋 Naming conventions

- Commit: `feat(vXXX):` / `fix(vXXX):` / `chore:` / `docs:` / `ci:`
- Branch: `feat/vXXX-<short-desc>` / `fix/vXXX-<short-desc>`
- Tag: `v{versionCode}` (e.g., `v324`)
- APK file: `DLavie26-Launcher-v{versionCode}.apk`

---

## 📚 Related Repos

| Repo | Purpose | URL |
|------|---------|-----|
| `dlavie-web` | Website (landing, FAQ, portal, issues) | https://github.com/drmacze/dlavie-web |
| `DLavie-Launcher-Data` | Data resource (manifest, news, APK, OBB) | https://github.com/drmacze/DLavie-Launcher-Data |
| `DLavie-Dev-Dashboard` | Admin dashboard (manage patches, users) | https://github.com/drmacze/DLavie-Dev-Dashboard |
| `DLavie-Patches` | FIFA 16 mod patches | https://github.com/drmacze/DLavie-Patches |

---

## ❓ Pertanyaan yang sering muncul di AI agent

**Q: Bolehkah aku pakai Supabase untuk fitur X?**
A: Hanya untuk auth + community (user login, posts, follows, comments). Untuk data publik (news, version, banner, config), **WAJIB** pakai GitHub raw / jsdelivr.

**Q: Bagaimana cara bump version?**
A: Edit `app/build.gradle` (versionCode +1, versionName deskriptif), commit, push. Workflow otomatis build + release.

**Q: APK gagal di-upload, apa yang salah?**
A: Cek step 12 logs di workflow run. Biasanya empty release sudah ada (delete dulu), atau GitHub API 503 (retry).

**Q: Boleh ubah UI GameHub?**
A: Hati-hati. User sudah review positif dengan v310 Replit base. Baca commit history `DLavieGameHub.kt` sebelum ubah — banyak iterasi yang sudah dikerjakan.

**Q: Bagaimana cara test update popup?**
A: Install APK dengan versionCode lama (misal v310), lalu buka app. Popup merah akan muncul (karena latest v324, gap 14 >= 1 = force update).

**Q: Boleh hapus code Supabase yang sudah mati?**
A: YA, silakan. Tapi test dulu — beberapa masih dipakai untuk auth. Lihat `CommunityApi.java` untuk konfigurasi Supabase yang masih aktif.

---

## 📞 Kontak

- Owner: `drmacze` (GitHub)
- Developer email: `dlaviecom@gmail.com` (bypass maintenance)
- Website: https://drmacze.github.io/dlavie-web/

---

**Terakhir diperbarui**: v324 (2026-07-20)
