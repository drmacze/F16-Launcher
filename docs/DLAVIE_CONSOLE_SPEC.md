# DLavie Console Spec

DLavie Console adalah aplikasi private untuk owner/developer/admin/moderator DLavie. Aplikasi ini terpisah dari DLavie 26 publik supaya user biasa tidak melihat developer menu.

## Purpose

```text
DLavie 26
= public launcher for players

DLavie Console
= private control center for developer/admin/moderator
```

## Access model

Developer Console boleh di-install sebagai APK private, tetapi keamanan tetap harus berada di backend.

Rules:

- APK private bukan pengganti authorization.
- Semua aksi admin harus mengecek role user dari backend.
- Jika APK Console bocor, akun user biasa tetap tidak bisa melakukan aksi admin.

## Roles

```text
user
verified_player
moderator
admin
developer
owner
```

Permissions:

| Role | Permissions |
| --- | --- |
| user | play, chat, comment, like, save, report |
| verified_player | same as user + verified badge |
| moderator | delete comments/messages, review reports, mute users |
| admin | ban/unban, pin posts, manage rooms |
| developer | publish update, maintenance mode, push notification |
| owner | full access |

## Main pages

```text
Dashboard
Updates
Notifications
Community
Reports
Users
Maintenance
Settings
```

## Dashboard

Shows:

- total users
- online users later
- latest version
- maintenance state
- pending reports
- latest update post
- failed update reports

## Maintenance

Fields:

- enabled: true/false
- scope: all/update/community/install
- title
- message
- estimated_end
- allow_offline_play

Example user-facing message:

```text
DLavie 26 Maintenance
Kami sedang menyiapkan update baru.
Update service sementara tidak tersedia.
```

## Push notification

Fields:

- title
- body
- target audience
- action type
- action payload

Targets:

- all users
- stable channel
- beta channel
- developer channel
- country
- specific user

Actions:

- open_update
- open_post
- open_community
- open_library
- open_url

## Update publishing

Developer can create update posts with:

- title
- body
- version code
- version name
- channel
- patch URL
- SHA-256
- risk level
- restart game required
- release notes
- known issues

After publish, DLavie 26 public launcher can display it in Feed and Update Center.

## Community moderation

Features:

- delete message/comment
- pin/unpin message
- mute user
- ban user
- unban user
- view report queue
- mark report resolved

## Bug report moderation

Reports can be grouped by:

- crash
- download error
- gameplay bug
- career mode bug
- graphic bug
- audio bug
- login problem
- update failed

Statuses:

- open
- reviewing
- fixed
- duplicate
- rejected

## Backend tables used by Console

- profiles
- app_config
- developer_announcements
- update_posts
- update_comments
- update_likes
- saved_updates
- community_rooms
- community_messages
- reports
- user_bans
- notification_campaigns
- audit_logs

## Audit log requirement

Every destructive or admin action must write audit log:

- actor_id
- action
- target_type
- target_id
- before
- after
- created_at

Examples:

- maintenance_enabled
- post_published
- comment_deleted
- user_banned
- notification_sent

## Build approach

Initial implementation can reuse the same Android project with a separate applicationId later:

```text
com.drmacze.dlavie26
com.drmacze.dlavieconsole
```

Recommended long-term approach:

- separate module/app for public launcher
- separate module/app for console
- shared Kotlin library for API models and theme

## First version scope

DLavie Console v0.1 should include only:

- login
- role check
- maintenance mode
- publish developer announcement
- view reports
- audit log

Push notification can be v0.2 because it requires Firebase Cloud Messaging setup.
