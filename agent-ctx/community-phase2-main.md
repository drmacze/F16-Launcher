# Phase: Community Phase 2 — Gallery, Video, Comments, Push Notif — v1.7.0-community-phase2

**Task ID:** community-phase2
**Agent:** main
**Date:** 2026-07-02
**Version:** versionCode 74 / versionName 1.7.0-community-phase2
**Base:** v1.6.0-community-feed (commit 2fdf4fe3abd8c70347e297586b5c7f21beb99492)

## Ringkasan

Implementasi 4 fitur Community Phase 2 di DLavie Launcher:
1. Gallery picker (ActivityResultContracts.PickVisualMedia) + upload ke Supabase Storage
   bucket `community-images`.
2. Video embed (YouTube + TikTok) — detect URL di body, render thumbnail + play icon.
3. Comments — fetchComments/addComment/deleteComment/getCommentCount + CommentsBottomSheet.
4. Push notification (Android 13+ POST_NOTIFICATIONS) + 60s polling for new posts from
   followed users → fire local notification.

## Files Changed

### 1. `app/src/main/java/com/drmacze/f16launcher/CommunityApi.java` (MODIFIED)
- Added: `uploadImage(byte[], String)` — POST image ke Supabase Storage bucket
  `community-images` (path `userId/filename.jpg`, header `x-upsert: true`, content-type
  `image/jpeg`). Returns public URL.
- Added: `fetchComments(postId)` — GET `/rest/v1/feed_comments?post_id=eq.X&order=created_at.asc`.
- Added: `addComment(postId, body)` — POST + `return=representation`.
- Added: `deleteComment(commentId)` — DELETE dengan filter `id=eq.X&user_id=eq.me`.
- Added: `getCommentCount(postId)` — count rows (`deleted=eq.false` filter).
- Added: `fetchFollowingIds()` — list following_id untuk polling.
- Added: `fetchNewPostsFromFollowing(List<String>, long sinceMillis)` — `created_at=gt.ISO`
  filter, max 10 newest.

### 2. `app/src/main/java/com/drmacze/f16launcher/NotificationHelper.kt` (NEW)
- `NotificationHelper.createChannel(context)` — create channel `dlavie_community`
  (IMPORTANCE_DEFAULT, enable vibration).
- `NotificationHelper.showNotification(context, title, body, postId)` — build + notify
  dengan small icon `R.mipmap.dlavie_launcher_icon`, PendingIntent → ModernLauncherActivity
  dengan extra `post_id`, FLAG_IMMUTABLE. Best-effort (silent fail kalau permission denied).
- Idempotent channel creation. Safe untuk dipanggil dari Activity.onCreate.

### 3. `app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt` (MODIFIED)
- **Imports**: added `android.os.Build`, `rememberLauncherForActivityResult`,
  `PickVisualMediaRequest`, `ActivityResultContracts`, `ChatBubbleOutline`, `IconButton`,
  `ContentScale`.
- **ModernLauncherActivity.onCreate**: pre-create notification channel; pass
  `initialPostId = intent?.getStringExtra("post_id")` ke DLavieModernApp. Override
  `onNewIntent` untuk update intent (deep-link subsequent notification taps).
- **DLavieModernApp(initialPostId)**: terima initialPostId, forward ke MainShell.
- **MainShell(initialPostId)**: jika initialPostId != null, default page = Chat.
  Hoist `pendingPostId` mutableState — di-consume oleh CommunityScreen saat comment
  sheet di-auto-open.
- **MainShell polling**: 60s loop → `api.fetchFollowingIds()` →
  `api.fetchNewPostsFromFollowing(follows, lastCheckTime)` → untuk setiap post baru,
  `NotificationHelper.showNotification(...)`. `lastCheckTime` di-init ke now() supaya
  tidak spam notif untuk post lama saat app baru dibuka.
- **MainShell permission**: `rememberLauncherForActivityResult(RequestPermission)` untuk
  POST_NOTIFICATIONS (Android 13+ / API 33+). Auto-launch saat app dibuka.
- **CommunityScreen**: tambah `pendingPostId` + `onConsumePostId` params;
  `commentCountState` map (post_id -> count) di-fetch per post di loadPosts();
  `commentsTarget` state (FeedPostData?) untuk CommentsBottomSheet; LaunchedEffect
  deep-link — kalau pendingPostId match post di feed, auto-open comments sheet;
  CommentBottomSheet render dengan onCommentAdded callback (optimistic +1 count).
- **FeedPostCard**: tambah `commentCount` + `onOpenComments` + `onOpenVideo` params;
  detect `extractVideoEmbed(post.body)` → render 160dp Box dengan thumbnail (AsyncImage
  for YouTube) atau placeholder gradien (TikTok), dark overlay + PlayCircle 48dp +
  platform badge (YOUTUBE/TIKTOK). Tap → `onOpenVideo(originalUrl)`. Comment button
  antara like dan ⋮ menu (ChatBubbleOutline + count).
- **CreatePostSheet**: tambah `uploading` state; `imagePicker` launcher
  (PickVisualMedia.ImageOnly) → read bytes from contentResolver → `api.uploadImage(bytes, filename)`
  → set `imageUrl`. OutlinedButton "Pilih Gambar" dengan loading state + 56dp preview
  AsyncImage + tiny remove (X) button. URL paste field tetap ada sebagai fallback.
  Post button disabled saat uploading.
- **CommentsBottomSheet** (NEW): ModalBottomSheet dengan header (icon + title + close),
  loading skeleton, error retry, empty state, LazyColumn komentar. Input row:
  OutlinedTextField (3 maxLines) + IconButton Send. Optimistic: append komentar lokal
  setelah addComment berhasil. Disabled saat tidak login (info text).
- **CommentRow** (NEW): 32dp avatar gradient + initial letter, displayName + relativeTime,
  body 13sp SoftText.
- **VideoEmbed data class + extractVideoEmbed(text)** (NEW top-level):
  - YOUTUBE_REGEX: `(https?://)?(www\.)?(youtube\.com/watch\?v=|youtu\.be/|youtube\.com/shorts/)([\w-]{6,})`
  - TIKTOK_REGEX: `(https?://)?(www\.)?tiktok\.com/@[\w.]+/video/(\d+)`
  - YouTube: return thumbnailUrl `https://img.youtube.com/vi/ID/hqdefault.jpg` + watch URL.
  - TikTok: return thumbnailUrl null + original URL (caller shows badge placeholder).
- **CommentItem data class** (NEW private): id, userId, username, displayName, body, createdAt.

### 4. `app/src/main/AndroidManifest.xml` (MODIFIED)
- Added `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
  (Android 13+ / API 33+).

### 5. `app/build.gradle` (MODIFIED)
- versionCode 73 → 74
- versionName `1.6.0-community-feed` → `1.7.0-community-phase2`

### 6. `/home/z/my-project/download/supabase-fix-v10-comments-phase2.sql` (NEW)
- `feed_comments` DELETE policy: owner atau staff (`auth.uid() = user_id or public.is_staff()`).
- `feed_comments` UPDATE policy: owner atau staff (untuk soft-delete kalau nanti dibutuhkan).
- Verify queries untuk cek policy setelah run.
- Catatan: storage bucket `community-images` sudah di-setup di v9
  (`/home/z/my-project/download/supabase-fix-v9-storage.sql`), harus sudah di-run.

## Build Status

- **GitHub Actions run ID (success):** 28627160316
- **GitHub Actions run ID (initial failure — lambda param syntax):** 28626963890
- **Status:** completed / success ✅
- **APK:** app-debug.apk (19,854,418 bytes)

## APK SHA1

```
540302242517a73c89d14ae92088d483d1c699e7
```

## Release URL

https://github.com/drmacze/F16-Launcher/releases/tag/v1.7.0-community-phase2

Release assets:
- `DLavie26-Launcher-v1.7.0-community-phase2.apk` (19.8 MB)
- `supabase-fix-v9-storage.sql` (storage bucket setup — prerequisite)
- `supabase-fix-v10-comments-phase2.sql` (comments DELETE/UPDATE policies)

## SQL File Path (untuk user run)

```
/home/z/my-project/download/supabase-fix-v10-comments-phase2.sql
```

**Prerequisite**: `supabase-fix-v9-storage.sql` juga harus sudah di-run untuk
storage bucket `community-images`.

## Design Decisions

- **Polling vs Realtime**: 60s polling dipilih (bukan Supabase Realtime) supaya
  tidak perlu dependency tambahan + hemat battery. Tradeoff: notifikasi delay max 60s.
- **lastCheckTime init = now()**: supaya tidak spam notifikasi untuk semua post existing
  saat app pertama dibuka. Hanya post yang dibuat setelah app dibuka yang trigger notif.
- **Comment optimistic**: setelah `api.addComment()` sukses, append comment lokal ke list
  + set `createdAt` = now() ISO. Comment count di post card di-update via `onCommentAdded`
  callback.
- **Video embed**: regex match di `remember(post.body)` supaya tidak re-parse tiap
  recompose. TikTok tidak punya public thumbnail API → placeholder gradien + badge.
- **Deep-link notification**: `pendingPostId` mutableState di-hoist ke MainShell.
  CommunityScreen consume via LaunchedEffect → auto-open comments sheet.
- **POST_NOTIFICATIONS**: launcher di MainShell, auto-request saat app dibuka.
  User bisa deny → next polling tetap jalan, tapi notif tidak tampil (silent fail).
- **Storage path**: `userId/filename.jpg` — RLS check `(storage.foldername(name))[1] = auth.uid()`.
- **uploadImage**: gunakan user access token (bukan anon key) supaya RLS `auth.uid()` resolve.
- **deleteComment**: physical DELETE dengan filter `id=eq.X&user_id=eq.me` (defense-in-depth
  — RLS juga enforce `auth.uid() = user_id`).

## Constraints Dipatuhi

- ✅ Tidak hapus existing functionality (Phase 1 feed, like, follow, filter, FAB, profile,
  settings, maintenance, PIN lock, telemetry, rating).
- ✅ Tidak ubah Supabase URL/key.
- ✅ POST_NOTIFICATIONS untuk Android 13+ (API 33+), NotificationCompat untuk backward compat.
- ✅ Coil AsyncImage untuk image loading (post image, video thumbnail, gallery preview,
  comment avatar fallback).
- ✅ All real data (Supabase REST API + Storage), no dummy.
- ✅ Build sukses sebelum push (verifikasi via GitHub Actions).

## Errors Encountered + Fixes

1. **`Unresolved reference: _`** di `MainShell` notification permission callback
   - Cause: Kotlin lambda `{ _ /* no-op */ }` — single `_` parameter tanpa arrow tidak
     diterima compiler untuk lambda `(Boolean) -> Unit` (Kotlin 1.9.24).
   - Build pertama gagal di commit 762fea9 (GitHub Actions run 28626963890).
   - Fix: ganti ke `{ granted -> if (!granted) { /* silent */ } }` — named parameter
     dengan empty if-block. Commit 3b87516. Build sukses (run 28627160316).
