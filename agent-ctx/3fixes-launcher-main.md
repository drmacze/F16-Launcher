# Task: 3-fixes-launcher (Report system + Storage reminder + Avatar upload)

## Date
2026-07-03

## Task ID
3fixes-launcher-main

## Summary
Fixed 3 bugs (plus existing fix 1) in DLavie Launcher (F16-Launcher Android app) and Dev Dashboard:

1. **Report system → Dev Dashboard**: `CommunityApi.reportPost` already existed as 3-arg version; added 2-arg overload `reportPost(postId, reason)` defaulting category to "community". `ReportPostDialog` extended with new "Penipuan/scam" (scam) option. Dev Dashboard `Tickets.tsx` rewritten with top-level tab switcher (Support Tickets | Reports) — Reports view fetches from `reports` table, displays reporter/target_type/target_id/reason/status/created_at, and allows admin to update status (open → reviewing → fixed/duplicate/rejected) + moderator note + audit_logs insert.

2. **Storage permission reminder in applyPatch**: `UpdateScreen.applyPatch()` now checks `StorageAccess.isGranted()` first. If not granted → shows `showStoragePermissionDialog` AlertDialog with "Izinkan" (opens Settings via `StorageAccess.request`) and "Nanti" (dismiss). If granted → continues existing patch apply flow.

3. **Avatar upload via Supabase Storage**: Added `CommunityApi.uploadAvatar(byte[])` (uploads to bucket `community-images` at path `userId/avatar_userId.jpg` with x-upsert:true) and `CommunityApi.updateAvatar(String)` (PATCH profiles.avatar_url + update local prefs). `ProfileScreen` avatar Box made clickable to launch `ActivityResultContracts.PickVisualMedia()` (image-only) → reads bytes from contentResolver → uploads → updates profile → sets `avatarUrlState` → AsyncImage (Coil) renders new avatar. Camera icon overlay bottom-right + progress spinner overlay during upload. Falls back to initial-letter DLavieLogoCover when no avatar_url set.

Also confirmed pre-existing Fix 1 in `AppUpdateChecker.kt` is already correct: returns null when table missing, when versionCode <= currentCode, or when apk_download_url is blank.

## Files Changed (Launcher)
- `app/build.gradle` — versionCode 76→77, versionName `1.8.0-draft-publish`→`1.8.1-fixes`
- `app/src/main/java/com/drmacze/f16launcher/AppUpdateChecker.kt` — already had Fix 1 (committed pre-existing diff)
- `app/src/main/java/com/drmacze/f16launcher/CommunityApi.java` — added `reportPost(postId, reason)` overload + `uploadAvatar(byte[])` + `updateAvatar(String)` methods
- `app/src/main/java/com/drmacze/f16launcher/ModernLauncherActivity.kt`:
  - Added `import androidx.compose.foundation.border` + `import androidx.compose.material.icons.rounded.CameraAlt`
  - `ReportPostDialog`: added "scam" option
  - `UpdateScreen.applyPatch()`: storage permission check + `showStoragePermissionDialog` AlertDialog
  - `ProfileScreen`: avatar image picker (PickVisualMedia) + AsyncImage display + camera overlay + upload progress

## Files Changed (Dev Dashboard)
- `src/pages/Tickets.tsx` — full rewrite with top-level tab switcher (Support Tickets | Reports). Reports view fetches reports table, supports status update (open/reviewing/fixed/duplicate/rejected), moderator note, audit_logs.

## Build Status
- Dev Dashboard: `npm run build` ✅ succeeded locally (Vite build, 5s)
- Launcher: no local Android SDK — relies on GitHub Actions CI (`Auto Release` workflow run id 28632835946, `Build Debug APK` workflow run id 28632817279)
- Commit `e043dc6` pushed to `main` of `drmacze/F16-Launcher`
- Commit `51fa218` pushed to `main` of `drmacze/DLavie-Dev-Dashboard`

## Supabase Schema Reference (reports table)
```sql
create table if not exists public.reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id uuid not null references public.profiles(id) on delete cascade,
    target_type text not null, -- post, comment, update, message, user, bug
    target_id text not null,
    category text not null default 'other',
    reason text not null,
    status public.report_status not null default 'open',
    moderator_id uuid references public.profiles(id),
    moderator_note text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
-- RLS: SELECT own or staff; INSERT owner (reporter); UPDATE staff only.
```
report_status enum: `open | reviewing | fixed | duplicate | rejected`

## Agent Notes for Future Tasks
- The `reportPost(postId, category, reason)` 3-arg version is the canonical method. The 2-arg `reportPost(postId, reason)` overload added here defaults category to "community" — useful for callers that don't need to specify category.
- `ProfileScreen` has a `scope` (rememberCoroutineScope) and `toast()` helper added at function top — can be reused by future Profile features.
- The "Tap avatar untuk edit profil ↓" hint was changed to "Tap avatar untuk ganti foto ↓" to reflect new behavior (avatar tap now opens gallery picker, not the profile edit section). If future agents want to restore the profile-edit-section behavior, they need to add a separate UI affordance (e.g. tap on display name or a separate button).
- The `StorageAccess.isGranted()` check in `applyPatch()` is BEFORE `patchLogs.clear()`. If user grants permission and re-taps, the patch will start fresh. If user dismisses with "Nanti", nothing happens — no patching state is mutated.
