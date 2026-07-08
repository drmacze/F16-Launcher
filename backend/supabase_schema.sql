-- DLavie 26 Supabase schema draft
-- Purpose: account profile, feed posts, update posts, comments, likes, saves,
-- realtime community chat, reports, maintenance mode, and developer console.

create extension if not exists pgcrypto;

-- ---------- ENUMS ----------

do $$ begin
    create type public.user_role as enum ('user', 'verified_player', 'moderator', 'admin', 'developer', 'owner');
exception when duplicate_object then null;
end $$;

do $$ begin
    create type public.update_channel as enum ('stable', 'beta', 'developer');
exception when duplicate_object then null;
end $$;

do $$ begin
    create type public.report_status as enum ('open', 'reviewing', 'fixed', 'duplicate', 'rejected');
exception when duplicate_object then null;
end $$;

-- ---------- PROFILES ----------

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username text unique not null check (username ~ '^[a-zA-Z0-9_]{3,24}$'),
    display_name text not null check (char_length(display_name) between 2 and 40),
    avatar_url text,
    cover_url text,  -- v7.9.62: Profile cover/banner image URL
    country text,
    role public.user_role not null default 'user',
    badges text[] not null default '{}',
    banned boolean not null default false,
    ban_reason text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists profiles_role_idx on public.profiles(role);
create index if not exists profiles_username_idx on public.profiles(username);

-- ---------- APP CONFIG / MAINTENANCE ----------

create table if not exists public.app_config (
    key text primary key,
    value jsonb not null,
    updated_by uuid references public.profiles(id),
    updated_at timestamptz not null default now()
);

insert into public.app_config(key, value)
values
('maintenance', '{"enabled": false, "scope": "none", "title": "", "message": "", "allow_offline_play": true}'::jsonb),
('launcher', '{"minimum_version_code": 30, "latest_version_name": "0.9.0-dlavie26-hub", "stable": true, "beta": false, "developer": false}'::jsonb)
on conflict (key) do nothing;

-- ---------- FEED / ANNOUNCEMENTS ----------

create table if not exists public.feed_posts (
    id uuid primary key default gen_random_uuid(),
    author_id uuid not null references public.profiles(id) on delete cascade,
    type text not null default 'community', -- developer, update, tutorial, community, bugfix
    title text not null,
    body text not null,
    image_url text,
    visibility text not null default 'public',
    pinned boolean not null default false,
    official boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists feed_posts_created_idx on public.feed_posts(created_at desc);
create index if not exists feed_posts_type_idx on public.feed_posts(type);

create table if not exists public.feed_comments (
    id uuid primary key default gen_random_uuid(),
    post_id uuid not null references public.feed_posts(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    body text not null check (char_length(body) between 1 and 2000),
    deleted boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists feed_comments_post_idx on public.feed_comments(post_id, created_at);

create table if not exists public.feed_likes (
    post_id uuid not null references public.feed_posts(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (post_id, user_id)
);

create table if not exists public.saved_posts (
    post_id uuid not null references public.feed_posts(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (post_id, user_id)
);

-- ---------- UPDATE POSTS ----------

create table if not exists public.update_posts (
    id uuid primary key default gen_random_uuid(),
    author_id uuid not null references public.profiles(id) on delete cascade,
    version_code integer not null,
    version_name text not null,
    channel public.update_channel not null default 'stable',
    title text not null,
    body text not null,
    release_notes text[] not null default '{}',
    known_issues text[] not null default '{}',
    patch_url text,
    patch_sha256 text,
    patch_size_bytes bigint,
    critical boolean not null default false,
    restart_game_required boolean not null default false,
    risk_level text not null default 'low',
    published boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists update_posts_version_idx on public.update_posts(version_code desc);
create index if not exists update_posts_channel_idx on public.update_posts(channel, published);

create table if not exists public.update_comments (
    id uuid primary key default gen_random_uuid(),
    update_id uuid not null references public.update_posts(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    body text not null check (char_length(body) between 1 and 2000),
    deleted boolean not null default false,
    created_at timestamptz not null default now()
);

create table if not exists public.update_likes (
    update_id uuid not null references public.update_posts(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (update_id, user_id)
);

create table if not exists public.saved_updates (
    update_id uuid not null references public.update_posts(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (update_id, user_id)
);

-- ---------- COMMUNITY CHAT ----------

create table if not exists public.community_rooms (
    id text primary key,
    name text not null,
    description text not null default '',
    icon text,
    sort_order integer not null default 0,
    locked boolean not null default false,
    created_at timestamptz not null default now()
);

insert into public.community_rooms(id, name, description, sort_order)
values
('global', 'Global Chat', 'Global DLavie 26 discussion.', 10),
('indonesia', 'Indonesia Chat', 'Ruang komunitas Indonesia.', 20),
('bug-report', 'Bug Report', 'Laporkan crash, data error, gameplay bug.', 30),
('mod-request', 'Mod Request', 'Request fitur, kits, database, career mode.', 40),
('gameplay', 'Gameplay Discussion', 'Diskusi gameplay realism dan balancing.', 50),
('update-feedback', 'Update Feedback', 'Feedback setelah update baru.', 60),
('help-installation', 'Help Installation', 'Bantuan install data, OBB, Shizuku/root.', 70)
on conflict (id) do nothing;

create table if not exists public.community_messages (
    id uuid primary key default gen_random_uuid(),
    room_id text not null references public.community_rooms(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    reply_to uuid references public.community_messages(id) on delete set null,
    body text not null check (char_length(body) between 1 and 2000),
    deleted boolean not null default false,
    pinned boolean not null default false,
    created_at timestamptz not null default now()
);

create index if not exists community_messages_room_idx on public.community_messages(room_id, created_at desc);

create table if not exists public.community_message_likes (
    message_id uuid not null references public.community_messages(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (message_id, user_id)
);

-- ---------- REPORTS / BANS ----------

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

create index if not exists reports_status_idx on public.reports(status, created_at desc);

create table if not exists public.user_bans (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    banned_by uuid references public.profiles(id),
    reason text,
    expires_at timestamptz,
    created_at timestamptz not null default now()
);

-- ---------- NOTIFICATION CAMPAIGNS ----------

create table if not exists public.notification_campaigns (
    id uuid primary key default gen_random_uuid(),
    created_by uuid not null references public.profiles(id),
    title text not null,
    body text not null,
    target jsonb not null default '{"type": "all"}'::jsonb,
    action jsonb not null default '{"type": "open_app"}'::jsonb,
    category text not null default 'announcement', -- update | announcement | maintenance | community
    sent_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists notification_campaigns_sent_at_idx
    on public.notification_campaigns(sent_at desc) where sent_at is not null;
create index if not exists notification_campaigns_category_idx
    on public.notification_campaigns(category, sent_at desc) where sent_at is not null;

-- ---------- GAME RATINGS (DLavie 26 community rating) ----------

create table if not exists public.game_ratings (
    user_id    uuid primary key references public.profiles(id) on delete cascade,
    rating     smallint not null check (rating between 1 and 5),
    review     text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists game_ratings_rating_idx on public.game_ratings(rating);

-- ---------- AUDIT LOGS ----------

create table if not exists public.audit_logs (
    id uuid primary key default gen_random_uuid(),
    actor_id uuid references public.profiles(id),
    action text not null,
    target_type text,
    target_id text,
    before jsonb,
    after jsonb,
    created_at timestamptz not null default now()
);

create index if not exists audit_logs_actor_idx on public.audit_logs(actor_id, created_at desc);
create index if not exists audit_logs_action_idx on public.audit_logs(action, created_at desc);

-- ---------- HELPERS ----------

create or replace function public.is_staff()
returns boolean
language sql
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles
        where id = auth.uid()
        and role in ('moderator', 'admin', 'developer', 'owner')
        and banned = false
    );
$$;

create or replace function public.is_developer_staff()
returns boolean
language sql
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles
        where id = auth.uid()
        and role in ('developer', 'owner')
        and banned = false
    );
$$;

-- ---------- RLS ----------

alter table public.profiles enable row level security;
alter table public.app_config enable row level security;
alter table public.feed_posts enable row level security;
alter table public.feed_comments enable row level security;
alter table public.feed_likes enable row level security;
alter table public.saved_posts enable row level security;
alter table public.update_posts enable row level security;
alter table public.update_comments enable row level security;
alter table public.update_likes enable row level security;
alter table public.saved_updates enable row level security;
alter table public.community_rooms enable row level security;
alter table public.community_messages enable row level security;
alter table public.community_message_likes enable row level security;
alter table public.reports enable row level security;
alter table public.user_bans enable row level security;
alter table public.notification_campaigns enable row level security;
alter table public.audit_logs enable row level security;

-- Public read policies

drop policy if exists "profiles public read" on public.profiles;
create policy "profiles public read" on public.profiles for select using (true);

drop policy if exists "profiles own update" on public.profiles;
create policy "profiles own update" on public.profiles for update using (id = auth.uid()) with check (id = auth.uid());

drop policy if exists "app_config public read" on public.app_config;
create policy "app_config public read" on public.app_config for select using (true);

drop policy if exists "app_config developer write" on public.app_config;
create policy "app_config developer write" on public.app_config for all using (public.is_developer_staff()) with check (public.is_developer_staff());

drop policy if exists "feed public read" on public.feed_posts;
create policy "feed public read" on public.feed_posts for select using (visibility = 'public' or public.is_staff());

drop policy if exists "feed authenticated create" on public.feed_posts;
create policy "feed authenticated create" on public.feed_posts for insert with check (auth.uid() = author_id);

drop policy if exists "feed author or staff update" on public.feed_posts;
create policy "feed author or staff update" on public.feed_posts for update using (auth.uid() = author_id or public.is_staff());

drop policy if exists "comments public read" on public.feed_comments;
create policy "comments public read" on public.feed_comments for select using (deleted = false or public.is_staff());

drop policy if exists "comments authenticated create" on public.feed_comments;
create policy "comments authenticated create" on public.feed_comments for insert with check (auth.uid() = user_id);

drop policy if exists "likes own write" on public.feed_likes;
create policy "likes own write" on public.feed_likes for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "saved own write" on public.saved_posts;
create policy "saved own write" on public.saved_posts for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "updates public read" on public.update_posts;
create policy "updates public read" on public.update_posts for select using (published = true or public.is_staff());

drop policy if exists "updates developer write" on public.update_posts;
create policy "updates developer write" on public.update_posts for all using (public.is_developer_staff()) with check (public.is_developer_staff());

drop policy if exists "update comments public read" on public.update_comments;
create policy "update comments public read" on public.update_comments for select using (deleted = false or public.is_staff());

drop policy if exists "update comments own insert" on public.update_comments;
create policy "update comments own insert" on public.update_comments for insert with check (auth.uid() = user_id);

drop policy if exists "update likes own write" on public.update_likes;
create policy "update likes own write" on public.update_likes for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "saved updates own write" on public.saved_updates;
create policy "saved updates own write" on public.saved_updates for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "rooms public read" on public.community_rooms;
create policy "rooms public read" on public.community_rooms for select using (true);

drop policy if exists "messages public read" on public.community_messages;
create policy "messages public read" on public.community_messages for select using (deleted = false or public.is_staff());

drop policy if exists "messages authenticated insert" on public.community_messages;
create policy "messages authenticated insert" on public.community_messages for insert with check (auth.uid() = user_id);

drop policy if exists "message likes own write" on public.community_message_likes;
create policy "message likes own write" on public.community_message_likes for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "reports own or staff read" on public.reports;
create policy "reports own or staff read" on public.reports for select using (auth.uid() = reporter_id or public.is_staff());

drop policy if exists "reports authenticated insert" on public.reports;
create policy "reports authenticated insert" on public.reports for insert with check (auth.uid() = reporter_id);

drop policy if exists "reports staff update" on public.reports;
create policy "reports staff update" on public.reports for update using (public.is_staff()) with check (public.is_staff());

drop policy if exists "bans staff read" on public.user_bans;
create policy "bans staff read" on public.user_bans for select using (public.is_staff());

drop policy if exists "bans admin write" on public.user_bans;
create policy "bans admin write" on public.user_bans for all using (public.is_staff()) with check (public.is_staff());

drop policy if exists "notifications developer read" on public.notification_campaigns;
create policy "notifications developer read" on public.notification_campaigns for select using (public.is_developer_staff());

drop policy if exists "notifications developer write" on public.notification_campaigns;
create policy "notifications developer write" on public.notification_campaigns for all using (public.is_developer_staff()) with check (public.is_developer_staff());

-- Authenticated (logged-in) users may read SENT notifications (sent_at not null).
-- Required so the launcher can show inline banner + notification popup for non-staff users.
drop policy if exists "notifications auth read sent" on public.notification_campaigns;
create policy "notifications auth read sent" on public.notification_campaigns
    for select using (auth.role() = 'authenticated' and sent_at is not null);

-- ── Game ratings RLS ──
alter table public.game_ratings enable row level security;

-- Anyone (anon + auth) may read aggregate ratings (so the launcher can show avg on Home).
drop policy if exists "game ratings public read" on public.game_ratings;
create policy "game ratings public read" on public.game_ratings for select using (true);

-- A user may insert/update ONLY their own rating row.
drop policy if exists "game ratings own upsert" on public.game_ratings;
create policy "game ratings own upsert" on public.game_ratings
    for insert with check (auth.uid() = user_id);

drop policy if exists "game ratings own update" on public.game_ratings;
create policy "game ratings own update" on public.game_ratings
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists "game ratings own delete" on public.game_ratings;
create policy "game ratings own delete" on public.game_ratings
    for delete using (auth.uid() = user_id);

drop policy if exists "audit staff read" on public.audit_logs;
create policy "audit staff read" on public.audit_logs for select using (public.is_staff());

drop policy if exists "audit staff insert" on public.audit_logs;
create policy "audit staff insert" on public.audit_logs for insert with check (public.is_staff());
