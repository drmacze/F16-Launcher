# F16 Launcher

Launcher/updater Android untuk FIFA 16 Mobile mod `com.ea.gp.fifaworld`.

Target utama:

- cek update dari GitHub manifest
- download patch kecil saja, bukan full data ulang
- verifikasi SHA-256 kalau tersedia
- backup file lama yang akan ditimpa
- apply update otomatis via Shizuku atau root
- launch FIFA 16 langsung dari launcher

## Cara update untuk user

1. Install APK `F16 Launcher`.
2. Aktifkan Shizuku di Android 13, atau gunakan root.
3. Buka F16 Launcher.
4. Tekan **Aktifkan / Cek Shizuku**.
5. Tekan **Check Update**.
6. Tekan **Update Now**.
7. Tekan **Launch FIFA 16**.

## URL manifest default

Launcher default membaca:

```text
https://raw.githubusercontent.com/drmacze/F16/main/updates/latest.json
```

Kalau manifest dipindahkan, URL bisa diganti dari kolom di launcher.

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

Repo ini hanya launcher/updater. File data FIFA 16 tetap dikelola di repo `drmacze/F16` melalui patch dan GitHub Releases.
