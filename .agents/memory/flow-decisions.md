---
name: App flow decisions
description: Activity navigation decisions and production architecture for DLavie26 launcher.
---

## Activity flow
1. DLavieGuidedActivity (LAUNCHER intent-filter in manifest)
2. onCreate: if session exists → syncToCommunityPrefs → startActivity(ModernLauncherActivity) → finish()
3. If no session → show premium login/register screen (GuidedLoginScreen)
4. After login → syncToCommunityPrefs → startActivity(ModernLauncherActivity) with CLEAR_TASK

## ModernLauncherActivity
- Exported=false, started from within same app only
- Checks api.loggedIn() on load; if false → redirect back to DLavieGuidedActivity
- Token auto-refresh via CommunityApi.refreshToken() every 50 min (LaunchedEffect in MainShell)
- 4 bottom nav tabs: Home, Data, Chat, Me

## Screens
- HomeScreen: real game install check, real data marker check, GitHub manifest update, Supabase feed_posts
- DataScreen: data status + DLavieHubActivity installer button + update check
- CommunityScreen: live Supabase forum (categories, topics, posts)
- ProfileScreen: real CommunityApi user data, role badge, confirm-before-logout dialog

## Phase files
All Phase34-Phase46, Phase3, Phase39x, DevelopmentMainShell deleted. Only Phase50ProRecoveryShell.kt kept (used by DevLauncherActivity).
