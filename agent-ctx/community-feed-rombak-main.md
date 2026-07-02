# Phase: Community Feed Rombak (TapTap-style) — v1.6.0-community-feed

**Task ID:** community-feed-rombak
**Agent:** main
**Date:** 2026-07-02
**Version:** versionCode 73 / versionName 1.6.0-community-feed
**Commit:** 2fdf4fe3abd8c70347e297586b5c7f21beb99492

## Ringkasan

Rombak total halaman Komunitas di DLavie Launcher menjadi TapTap-style community
feed. Build sukses, APK dirilis ke GitHub Releases.

## Files Changed

1. **`/home/z/my-project/download/supabase-fix-v8-follows.sql`** (NEW)
   - Tabel `community_follows` (follower_id, following_id, created_at)
   - Indexes: follower_idx, following_idx
   - RLS policies: SELECT public, INSERT/UPDATE/DELETE owner (follower) only
   - **WAJIB di-run user di Supabase SQL Editor**

2. **`app/src/main/java/com/drmacze/f16launcher/CommunityApi.java`** (MODIFIED)
   - Added: `followUser`, `unfollowUser`, `isFollowing`
   - Added: `fetchFeedPostsGlobal`, `fetchFeedPostsFollowing`
   - Added: `createFeedPost` (with image URL)
   - Added: `likePost`, `unlikePost`, `getPostLikeCount`, `hasLikedPost`
   - Added: `savePost`, `unsavePost`, `hasSavedPost` (untuk bookmark feature)
   - Added: `reportPost` (untuk report feature)
   - Added: `getProfileById` (author info)

3. **`app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt`** (MODIFIED)
   - Replaced entire `CommunityScreen` function + old `DiscussionTab`/`FAQTab`/`SupportTab`/`FaqItem`/`SupportTicket` (3 old tabs) dengan TapTap-style feed
   - Added: `CommunityTopBar` (role badge + tabs + filter dropdown)
   - Added: `CommunityTab` (underline active indicator)
   - Added: `CommunityFilterChip` (clearable active filter)
   - Added: `FeedPostCard` (image + title + body + author + timestamp + like + ⋮ menu)
   - Added: `AuthorAvatar` (circular gradient + initial letter, AsyncImage if avatar_url)
   - Added: `CommunityEmptyState` (contextual empty/loading-error state)
   - Added: `CreatePostSheet` (ModalBottomSheet: title + body + image URL + type)
   - Added: `ReportPostDialog` (AlertDialog: spam/inappropriate/other + detail)
   - Added: `FeedPostData`, `AuthorInfo` data classes
   - Added: `parseIsoToMillis`, `relativeTime` helpers (SimpleDateFormat — API 24 safe)
   - Added imports: LazyColumn, items, AsyncImage, ModalBottomSheet, DatePicker, RadioButton, FilterList/ThumbUp/MoreVert/Flag/Share/Bookmark/Add/etc icons, KeyboardOptions, ImeAction
   - FAB: circular white "+" (Box + clickable, 56dp)

4. **`app/build.gradle`** (MODIFIED)
   - versionCode 72 → 73
   - versionName 1.5.0-auto-update → 1.6.0-community-feed

## Build Status

- **GitHub Actions run ID:** 28625339745
- **Status:** completed / success ✅
- **APK:** app-debug.apk (19,805,230 bytes)

## APK SHA1

```
8ff8daa195042c235842f520a4167c62547dc25c
```

## Release URL

https://github.com/drmacze/F16-Launcher/releases/tag/v1.6.0-community-feed

## SQL File Path (untuk user run)

```
/home/z/my-project/download/supabase-fix-v8-follows.sql
```

## Errors Encountered + Fixes

1. **`target_commitish` invalid (HTTP 422)** saat create release
   - Cause: short SHA (7-char) ditolak GitHub API
   - Fix: gunakan `target_commitish: "main"` (branch name)

2. **Layout concern: `fillMaxSize()` di feed Box dalam Column**
   - Cause: feed Box sebagai child terakhir Column dengan `fillMaxSize()` bisa overflow (top bar + filter row sudah ambil space)
   - Fix: ganti ke `Modifier.weight(1f).fillMaxWidth()` supaya feed ambil sisa space

3. **`java.time` tidak aman di API 24-25** (minSdk 24, no desugaring)
   - Cause: OffsetDateTime.parse() crash di API < 26
   - Fix: pakai `java.text.SimpleDateFormat` dengan timezone UTC untuk parse ISO timestamptz

4. **ExperimentalMaterial3Api** untuk DatePicker/ModalBottomSheet/RadioButton
   - Fix: `@OptIn(ExperimentalMaterial3Api::class)` di `CommunityScreen` dan `CreatePostSheet`

## Design Decisions

- **Image upload Phase 1**: URL paste (simpel, no Supabase Storage bucket setup needed). Gallery picker + Storage upload = Phase 2.
- **Like/Save**: optimistic update dengan revert on failure (instant feedback, network call background)
- **Author profiles**: cached di `authorCache` map (fetch hanya untuk author baru)
- **Filter role/date**: client-side (setelah fetch posts + profiles). Sort (newest/oldest) server-side.
- **For You = default tab** (supaya feed populated untuk user baru yang belum follow siapa-siapa)
- **Coil AsyncImage** untuk post image + author avatar
- **TTDesignSystem tokens**: Carbon bg, GlassBase cards, TTShapes.chip/input, TTSpacing, HalftoneBackground alpha 0.3
- **Three-dot menu**: emoji (🚩📤🔖) + text untuk clarity
- **Touch targets**: FAB 56dp, like button ~44dp, menu button 32dp (borderline — acceptable untuk secondary action)

## Constraints Dipatuhi

- ✅ Tidak hapus auth logic, Supabase config, maintenance logic, PIN lock, telemetry, rating system
- ✅ Tidak downgrade existing functionality (HomeScreen, UpdateScreen, ProfileScreen, SettingsScreen utuh)
- ✅ Build sukses sebelum push
- ✅ All real data (Supabase API, no dummy)
- ✅ Coil AsyncImage untuk image loading
- ✅ TTDesignSystem tokens
- ✅ Mobile-friendly (touch targets ≥44dp untuk primary actions)
