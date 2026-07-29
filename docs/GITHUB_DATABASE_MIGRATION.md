# v325 GitHub Data Migration

## Tujuan

Memindahkan seluruh data **publik dan read-only** dari Supabase ke repository
`drmacze/DLavie-Launcher-Data`, sehingga launcher tidak bergantung pada kuota
egress Supabase untuk versi aplikasi, manifest game, berita, banner, konfigurasi,
notifikasi resmi, dan posting resmi.

## Arsitektur v325

Aplikasi membaca file JSON publik melalui urutan berikut:

1. jsDelivr CDN
2. GitHub Contents API
3. raw.githubusercontent.com

Semua request bersifat anonymous `GET`. APK tidak menyimpan GitHub PAT dan tidak
mempunyai izin menulis ke repository.

File publik yang didukung:

- `manifest.json`
- `banner_slides.json`
- `news_posts.json`
- `public_database.json`
- `app_config.json`
- `notification_campaigns.json`
- `update_posts.json`
- `official_feed.json`

## Batas keamanan

GitHub repository publik **bukan database autentikasi**. Access token, refresh
token, email privat, password, dan data profil privat tidak boleh dimasukkan ke
file JSON repository.

Aplikasi Android juga tidak boleh melakukan write langsung menggunakan GitHub
PAT. Token yang ditanam dalam APK dapat diekstrak dan kemudian digunakan pihak
lain untuk mengubah atau menghapus data repository.

Karena itu, v325 menetapkan komunitas ke mode transisi:

- data resmi/public: GitHub
- data user-writable: tetap legacy sampai tersedia broker/backend aman
- bila backend legacy tidak tersedia: komunitas harus turun ke read-only/guest,
  bukan menyimpan write token di APK

## Perubahan keamanan deep link

`ModernLauncherActivity` sekarang internal-only. Semua deep link eksternal
`dlavie://` masuk melalui `ShinySplashActivity`, sehingga alur lama
`dlavie://connect?callback=...` tidak dapat dipakai aplikasi eksternal untuk
meminta launcher meneruskan token sesi ke URL arbitrer.

## Langkah migrasi berikutnya

1. Ganti pembacaan `app_config`, campaign, update posts, dan official feed agar
   memakai `GitHubPublicDatabase`.
2. Tambahkan cache persisten terakhir-yang-valid untuk penggunaan offline.
3. Ubah seluruh UI komunitas menjadi read-only saat backend write tidak sehat.
4. Pilih backend write aman dan gratis/berbiaya rendah untuk auth dan konten
   pengguna. Pilihan ini harus menyimpan secret di server, bukan di APK.
5. Setelah semua caller berpindah, hapus konstanta dan request Supabase legacy.

## Operasional

Perubahan konten publik dilakukan melalui commit/PR di
`DLavie-Launcher-Data`. Validasi JSON wajib dijalankan sebelum merge. Data privat
atau kredensial harus ditolak saat review.
