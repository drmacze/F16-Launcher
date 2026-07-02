# DLavie 26 Product Spec

DLavie 26 adalah hub resmi untuk FIFA 16 Mobile mod 2026. Targetnya bukan hanya downloader, tetapi satu aplikasi profesional untuk install, update, repair, community, profile, dan launch game.

## Branding

- Public name: **DLavie 26**
- Meaning: 26 = 2026, dengan arah visual seperti football game modern 2026.
- Tagline: **FIFA 16 Mobile 2026 Mod Hub**
- Visual direction: dark premium, glassmorphism, neon green/cyan accent, rounded cards, floating bottom navigation.

## App separation

### Public app: DLavie 26

Untuk semua player.

Core features:

- Play FIFA 16 Mobile
- First install / full data bootstrap
- OBB/data status
- Repair data
- Update Center
- Feed/news
- Community chat
- Profile
- Account settings
- Comment / like / share / save
- Bug report

### Private app: DLavie Console

Untuk owner, developer, admin, moderator.

Core features:

- Maintenance mode
- Push notification
- Publish update post
- Publish changelog
- Pin/unpin posts
- Moderate comments/chat
- Ban/unban user
- Review reports
- Manage update channels
- Manage community rooms

Developer/admin actions must be protected by backend roles, not only by hidden menus or a separate APK.

## Main navigation

Recommended public navigation:

```text
Feed
Library
Community
Profile
```

Alternative compact navigation:

```text
Feed
Library
Profile
```

with Community and Update available as cards/buttons inside Feed and Library.

## Feed

Purpose: social feed + official updates.

Cards:

- Developer announcement
- Update release post
- Gameplay preview
- Tutorial
- Bug fix note
- Community post

Actions:

- Like
- Comment
- Share
- Save
- Update Now when post has patch metadata

Official post layout:

```text
[image/banner]
DLavie Official ✓  Developer  Public
DLavie 26 Gameplay Realism Patch v3
- Better AI defending
- Updated attribdb
- Fixed cl.ini crash
[Update Now]
Like · Comment · Share · Save
```

## Library

Purpose: game install manager and local file status.

Cards:

- DLavie 26 Base Game
- Main OBB
- Patch OBB
- Data package
- Installed patches
- Saved updates

Actions:

- Play
- Verify
- Repair
- Redownload
- Open Advanced Updater
- Clear cache

Status colors:

- Green: installed/valid
- Yellow: update available
- Red: missing/corrupt
- Gray: not installed

## Community

Purpose: realtime in-launcher community, not external browser only.

Rooms:

- Global Chat
- Indonesia Chat
- Bug Report
- Mod Request
- Gameplay Discussion
- Update Feedback
- Help Installation

Features:

- Realtime messages
- Username/avatar
- Admin/moderator/developer badges
- Reply message
- Like message
- Delete own message
- Report message
- Slow mode / anti-spam
- Pinned developer message
- Online users later

## Profile

Fields:

- Avatar
- Display name
- Username
- Role badge
- Country
- Join date
- Posts count
- Followers count
- Saved updates/posts
- Comments
- Liked posts
- Following

Badges:

- Founder
- Developer
- Moderator
- Verified Player
- Beta Tester
- Bug Hunter
- Early Supporter

## Settings

Sections:

### Account

- Edit display name
- Edit username
- Change avatar
- Logout
- Delete account later

### Game

- Launch FIFA
- Force stop FIFA
- Verify files
- Repair data
- Clear game cache

### Download

- Wi-Fi only
- Resume download
- Verify SHA after download
- Clear download cache

### Update

- Auto check update
- Channel: stable/beta/developer
- Backup before patch
- Rollback last patch

### Community

- Chat notifications
- Hide offensive words
- Blocked users
- Report history

### Advanced

- Debug mode
- View logs
- Export logs
- Reset marker
- Recheck manifest
- Force redownload data

## Update system

Initial install uses large files from GitHub Releases:

- main OBB
- patch OBB
- dlavie26-data.zip

Small updates use patch manifests and patch ZIP/inline files:

- cl.ini
- attribdb
- Lua/UI flow
- small textures
- config files
- gameplay/database patches

The large data archive should not be re-uploaded for every small update.

## Security principles

- No public developer menu in the user launcher.
- Admin actions must require backend role checks.
- Public launcher can show admin badge, but cannot expose admin actions.
- Developer APK can be private, but must still verify role on backend.
- Maintenance mode, push notification, ban, and publish update must be restricted to owner/developer/admin roles.

## Release milestones

```text
v0.9.0
Public DLavie 26 Hub entry, hidden recovery shell, product direction locked.

v0.10.0
Feed + Library visual redesign based on dark glassmorphism reference.

v0.11.0
Update posts: like, comment, share, save.

v0.12.0
Realtime community chat.

v0.13.0
Profile/account settings and badges.

v0.14.0
Bug report center and moderation.

v1.0.0
DLavie 26 Global Launcher stable.
```
