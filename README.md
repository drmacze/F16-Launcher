# DLavie 26 Launcher

Launcher/updater Android untuk FIFA 16 Mobile mod `com.ea.gp.fifaworld`.

DLavie 26 bukan hanya downloader. Target finalnya adalah **mod hub profesional** untuk install game, update patch, repair data, community, profile, feed, dan launch FIFA 16 Mobile.

## Branding

- Public name: **DLavie 26**
- Meaning: 26 = 2026, searah dengan konsep football season 2026.
- Tagline: **FIFA 16 Mobile 2026 Mod Hub**

## Aplikasi publik vs developer

### DLavie 26

APK publik untuk semua user/player.

Fitur utama:

- Play FIFA 16 Mobile
- Install OBB + data mod
- Repair/verify data
- Update Center dari GitHub manifest
- Community chat
- Profile/account
- Feed/news/update post
- Like/comment/share/save
- Bug report

### DLavie Console

APK private untuk owner/developer/admin/moderator.

Fitur utama:

- Maintenance mode
- Push notification
- Publish update post
- Publish changelog
- Moderate chat/comment
- Ban/unban user
- Review reports
- Manage update channels

Developer/admin actions harus divalidasi oleh backend role, bukan hanya hidden menu di APK.

## Status repo saat ini

- App module: Android Kotlin + Jetpack Compose
- Package: `com.drmacze.f16launcher`
- Public launcher entry: `ModernLauncherActivity`
- Internal recovery shell: `DevLauncherActivity`, tidak dijadikan launcher publik
- Update manifest default: `https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json`
- Target game: `com.ea.gp.fifaworld`

## Target utama teknis

- cek update dari GitHub manifest
- download patch kecil saja, bukan full data ulang
- verifikasi SHA-256 kalau tersedia
- backup file lama yang akan ditimpa
- apply update otomatis via Shizuku atau root
- launch FIFA 16 langsung dari launcher

## Cara update untuk user

1. Install APK `DLavie 26`.
2. Buka DLavie 26.
3. Login/register akun DLavie.
4. Buka **Library** untuk install/repair data.
5. Buka **Update** untuk cek patch baru.
6. Tekan **Update Now** jika ada update.
7. Tekan **Play Game**.

## URL manifest default

Launcher default membaca:

```text
https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json
```

Kalau manifest dipindahkan, URL bisa diganti dari setting launcher nanti.

## Format patch

Patch harus berupa `.zip` yang isinya memakai path relatif terhadap target.

Contoh patch career mode:

```text
patch-v2-career.zip
└── data/ux/Flows/MainFlow/GameModes/Liga/Newgame/Newgame.lua
```

Jika target manifest adalah:

```text
/sdcard/Android/data/com.ea.gp.fifaworld/
```

maka file di atas akan disalin ke:

```text
/sdcard/Android/data/com.ea.gp.fifaworld/data/ux/Flows/MainFlow/GameModes/Liga/Newgame/Newgame.lua
```

## Format `updates/latest.json`

```json
{
  "latestVersionCode": 2,
  "latestVersionName": "v2 - Career Mode Update",
  "gamePackage": "com.ea.gp.fifaworld",
  "patches": [
    {
      "from": 1,
      "to": 2,
      "name": "Career Mode Update",
      "url": "https://github.com/drmacze/F16/releases/download/v2/patch-v2-career.zip",
      "sha256": "",
      "target": "/sdcard/Android/data/com.ea.gp.fifaworld/"
    }
  ]
}
```

`sha256` boleh dikosongkan saat testing, tetapi untuk rilis sebaiknya diisi.

## Dokumen desain

- [`docs/DLAVIE_26_PRODUCT_SPEC.md`](docs/DLAVIE_26_PRODUCT_SPEC.md)
- [`docs/DLAVIE_CONSOLE_SPEC.md`](docs/DLAVIE_CONSOLE_SPEC.md)
- [`backend/supabase_schema.sql`](backend/supabase_schema.sql)
- [`backend/remote_config_examples.json`](backend/remote_config_examples.json)

## Build APK

Buka tab **Actions** di GitHub, jalankan workflow **Build Debug APK**, lalu download artifact `F16-Launcher-debug-apk`.

Atau build lokal:

```bash
gradle assembleDebug
```

APK debug akan ada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Catatan

Repo ini adalah launcher/updater. File data besar FIFA 16 tetap dikelola lewat GitHub Releases dan patch repo `drmacze/F16`.

Untuk update kecil, jangan upload ulang data 1.4GB. Upload patch kecil dan update manifest saja.
