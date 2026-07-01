---
name: Auth architecture
description: Two SharedPreferences systems bridged for login/logout, Supabase keys, sync pattern.
---

## Two auth SharedPreferences
- `dlavie_auth_session` — Used by DLavieGuidedActivity. Keys: access_token, refresh_token, email.
- `dlavie_community`   — Used by CommunityApi. Keys: access_token, refresh_token, user_id, username, display_name, avatar_url, role.

## Bridge function (DLavieGuidedActivity.kt)
`syncToCommunityPrefs(context, session)` decodes user_id from JWT sub claim (Base64 URL decode) and writes both prefs. Called on login and on startup redirect.

## Logout must clear BOTH
- CommunityApi.logout() → clears dlavie_community
- ModernLauncherActivity logout lambda → ALSO clears dlavie_auth_session SharedPreferences

## Why two systems exist
Historical: GuidedActivity had its own auth before CommunityApi existed. Merge not worth breaking change risk; bridge pattern is stable.
