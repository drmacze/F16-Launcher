package com.drmacze.f16launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommunityApi {

    // Constants (moved from SupabaseClient — CommunityApi is standalone now)
    public static final String SUPABASE_URL = "https://lvmucsxbmadtsgrxuwmo.supabase.co";
    public static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx2bXVjc3hibWFkdHNncnh1d21vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI5ODUyODksImV4cCI6MjA5ODU2MTI4OX0.y-1sE6uYTn4Wbter6g6NozY6uojzD5x9YVeYif-5nJs";

    private static final String TAG = "DLavieApi";

    private final SharedPreferences prefs;

    public CommunityApi(Context ctx) { prefs = ctx.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE); }

    public boolean loggedIn() {
        String token = prefs.getString("access_token", "");
        if (token.isEmpty() || prefs.getString("user_id", "").isEmpty()) return false;
        // Cek apakah token akan expired dalam 5 menit ke depan
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = parts[1];
                String padded = payload + "=".repeat((4 - payload.length() % 4) % 4);
                byte[] decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE);
                JSONObject jwt = new JSONObject(new String(decoded));
                long exp = jwt.optLong("exp", 0) * 1000L;
                long now = System.currentTimeMillis();
                // Kalau expired dalam 5 menit, coba refresh (async, non-blocking)
                if (exp > 0 && exp - now < 300_000L) {
                    new Thread(() -> { try { refreshToken(); } catch (Throwable ignored) {} }).start();
                }
            }
        } catch (Throwable ignored) { }
        return true;
    }

    /**
     * v6.8.3: Guest mode — user pilih "Lanjutkan sebagai Guest" di login screen.
     * Guest bisa browse Beranda, Jelajahi, Update, Komunitas (read-only).
     * TIDAK bisa: post, comment, download APK, rate game, live chat.
     * Guest prefs: is_guest=true, no access_token, no user_id.
     */
    public boolean isGuest() {
        return prefs.getBoolean("is_guest", false) && !loggedIn();
    }

    /** v6.8.3: Set guest mode flag. Called when user taps "Lanjutkan sebagai Guest". */
    public void setGuest(boolean guest) {
        prefs.edit().putBoolean("is_guest", guest).apply();
    }

    /** v6.8.3: Clear guest flag (called on real login/register or explicit logout). */
    public void clearGuest() {
        prefs.edit().remove("is_guest").apply();
    }

    public String userId() { return prefs.getString("user_id", ""); }
    public String token() { return prefs.getString("access_token", ""); }
    public String username() { return prefs.getString("username", ""); }
    public String displayName() { return prefs.getString("display_name", ""); }
    public String country() { return prefs.getString("country", "Indonesia"); }
    public void setCountry(String country) {
        if (country == null || country.trim().isEmpty()) return;
        prefs.edit().putString("country", country.trim()).apply();
    }
    public void logout() { prefs.edit().clear().apply(); }

    public JSONObject register(String email, String password, String username, String displayName, String avatarUrl) throws Exception {
        validateProfile(username, displayName);
        JSONObject data = new JSONObject();
        data.put("username", username.trim());
        data.put("display_name", displayName.trim());
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) data.put("avatar_url", avatarUrl.trim());
        JSONObject body = new JSONObject();
        body.put("email", email.trim());
        body.put("password", password);
        body.put("data", data);
        JSONObject res = new JSONObject(request("POST", "/auth/v1/signup", body, false, false));
        storeSessionIfPresent(res);
        if (loggedIn()) ensureMyProfile(username, displayName, avatarUrl);
        // Task 5: log activity (register) — fire-and-forget, swallow errors.
        try {
            JSONObject meta = new JSONObject()
                .put("username", username.trim())
                .put("country", country());
            logActivity("register", meta);
            // Award badges (server-side RPC — checks login_count badge etc.)
            checkAndAwardBadges();
        } catch (Throwable ignored) { }
        return res;
    }

    public JSONObject login(String email, String password) throws Exception {
        return login(email, password, "", "", "");
    }

    public JSONObject login(String email, String password, String username, String displayName, String avatarUrl) throws Exception {
        JSONObject body = new JSONObject();
        body.put("email", email.trim());
        body.put("password", password);
        JSONObject res = new JSONObject(request("POST", "/auth/v1/token?grant_type=password", body, false, false));
        storeSessionIfPresent(res);
        try {
            loadMyProfile();
        } catch (Throwable missingProfile) {
            if (username != null && !username.trim().isEmpty() && displayName != null && !displayName.trim().isEmpty()) {
                ensureMyProfile(username, displayName, avatarUrl);
            } else {
                throw new IllegalStateException("Login berhasil, tapi profile community belum ada. Isi username + display name lalu tekan Login lagi.");
            }
        }
        // Task 5: log activity (login) + award badges — fire-and-forget.
        try {
            logActivity("login", null);
            checkAndAwardBadges();
        } catch (Throwable ignored) { }
        return res;
    }

    private void validateProfile(String username, String displayName) {
        String u = username == null ? "" : username.trim();
        String d = displayName == null ? "" : displayName.trim();
        if (!u.matches("[a-zA-Z0-9_]{3,24}")) throw new IllegalStateException("Username wajib 3-24 karakter: huruf, angka, underscore.");
        if (d.length() < 2 || d.length() > 40) throw new IllegalStateException("Display name wajib 2-40 karakter.");
    }

    public JSONObject ensureMyProfile(String username, String displayName, String avatarUrl) throws Exception {
        if (userId().isEmpty()) throw new IllegalStateException("Belum login.");
        validateProfile(username, displayName);
        // Cek apakah profile sudah ada (trigger handle_new_user sudah create)
        // Kalau sudah ada, hanya update display_name & avatar — JANGAN override username
        // (karena bisa conflict dengan unique constraint kalau username sudah dipakai user lain)
        JSONObject body = new JSONObject();
        body.put("id", userId());
        body.put("display_name", displayName.trim());
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) body.put("avatar_url", avatarUrl.trim());
        // Cek username existing — hanya set kalau berbeda dari yang ada
        try {
            JSONArray existing = new JSONArray(request("GET", "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=username", null, true, false));
            if (existing.length() > 0) {
                String currentUsername = existing.getJSONObject(0).optString("username", "");
                if (!currentUsername.isEmpty() && !currentUsername.equals(username.trim())) {
                    // Username di profile berbeda dari input — kemungkinan sudah diambil user lain
                    // atau di-auto-generate oleh trigger. Coba update kalau available.
                    try {
                        body.put("username", username.trim());
                    } catch (Throwable ignored) { }
                }
                // else: username sama, tidak perlu update (hindari duplicate check)
            } else {
                // Profile belum ada, set username
                body.put("username", username.trim());
            }
        } catch (Throwable ignored) {
            // Gagal cek existing, set username (kalau conflict, akan throw — caller handle)
            body.put("username", username.trim());
        }
        JSONArray arr = new JSONArray(request("POST", "/rest/v1/profiles?on_conflict=id", body, true, "resolution=merge-duplicates,return=representation"));
        try {
            JSONObject setting = new JSONObject();
            setting.put("user_id", userId());
            request("POST", "/rest/v1/user_settings?on_conflict=user_id", setting, true, "resolution=merge-duplicates,return=minimal");
        } catch (Throwable ignored) { }
        if (arr.length() > 0) {
            saveProfile(arr.getJSONObject(0));
            return arr.getJSONObject(0);
        }
        return loadMyProfile();
    }

    private void storeSessionIfPresent(JSONObject res) {
        String access = res.optString("access_token", "");
        String refresh = res.optString("refresh_token", "");
        JSONObject user = res.optJSONObject("user");
        if (!access.isEmpty() && user != null) {
            prefs.edit().putString("access_token", access).putString("refresh_token", refresh).putString("user_id", user.optString("id", "")).apply();
        }
    }

    public JSONObject loadMyProfile() throws Exception {
        String id = userId();
        if (id.isEmpty()) throw new IllegalStateException("Belum login.");
        JSONArray arr = new JSONArray(request("GET", "/rest/v1/profiles?id=eq." + enc(id) + "&select=*", null, true, false));
        if (arr.length() == 0) throw new IllegalStateException("Profile community belum tersedia.");
        JSONObject p = arr.getJSONObject(0);
        saveProfile(p);
        return p;
    }

    private void saveProfile(JSONObject p) {
        String country = p.optString("country", "");
        SharedPreferences.Editor e = prefs.edit()
            .putString("username", p.optString("username", ""))
            .putString("display_name", p.optString("display_name", ""))
            .putString("avatar_url", p.optString("avatar_url", ""))
            .putString("role", p.optString("role", "member"));
        if (!country.isEmpty()) e.putString("country", country);
        // v7.9.17: cache user_type, use_case, android_version untuk onboarding check
        e.putString("user_type", p.optString("user_type", ""));
        e.putString("use_case", p.optString("use_case", ""));
        if (!p.optString("android_version", "").isEmpty()) {
            e.putString("android_version", p.optString("android_version", ""));
        }
        e.apply();
    }

    public String role() { return prefs.getString("role", "member"); }
    public String avatarUrl() { return prefs.getString("avatar_url", ""); }

    public JSONArray feedPosts() throws Exception {
        // Authenticated request — uses user's access token so RLS policies can resolve author + visibility.
        return new JSONArray(request("GET", "/rest/v1/feed_posts?type=neq.issue&select=id,author_id,title,body,image_url,type,visibility,pinned,official,created_at&order=pinned.desc,created_at.desc&limit=10", null, true, (String) null));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Community follows (community_follows table — TapTap-style "Following")
    //  Schema:
    //    follower_id  uuid PK references profiles(id) on delete cascade
    //    following_id uuid PK references profiles(id) on delete cascade
    //    created_at   timestamptz default now()
    //  RLS: SELECT public, INSERT/UPDATE/DELETE owner (follower) only.
    // ──────────────────────────────────────────────────────────────────────

    /** Follow a user. Idempotent (on_conflict merge). Login required. */
    public void followUser(String userId) throws Exception {
        JSONObject body = new JSONObject().put("follower_id", userId()).put("following_id", userId);
        request("POST", "/rest/v1/community_follows?on_conflict=follower_id,following_id", body, true, "resolution=merge-duplicates,return=minimal");
        // Task 5: log activity + award badges — fire-and-forget.
        try {
            logActivity("follow", new JSONObject().put("user_id", userId));
            checkAndAwardBadges();
        } catch (Throwable ignored) { }
    }

    /** Unfollow a user. Login required. */
    public void unfollowUser(String userId) throws Exception {
        request("DELETE", "/rest/v1/community_follows?follower_id=eq." + enc(userId()) + "&following_id=eq." + enc(userId), null, true, false);
    }

    /** Check if current user follows a specific user. Login required. */
    public boolean isFollowing(String userId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/community_follows?follower_id=eq." + enc(userId()) + "&following_id=eq." + enc(userId) + "&select=follower_id",
            null, true, false));
        return arr.length() > 0;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Feed posts — TapTap-style "For You" (global) + "Following" tabs
    // ──────────────────────────────────────────────────────────────────────

    /**
     * v7.9.6: Fetch feed posts for "For You" (global) — with filter.
     * Filter official=eq.false supaya News/official posts TIDAK muncul di Komunitas.
     * News & official posts sekarang ADA DI tab Beranda (NewsScreen composite).
     * Komunitas = user posts only (official=false).
     */
    public JSONArray fetchFeedPostsGlobal(String sortBy, int limit) throws Exception {
        String order = "created_at.desc";
        if ("oldest".equals(sortBy)) order = "created_at.asc";
        return new JSONArray(request("GET",
            "/rest/v1/feed_posts?official=eq.false&type=neq.issue&order=" + order + "&limit=" + limit +
            "&select=id,author_id,title,body,image_url,type,pinned,official,created_at",
            null, false, false));
    }

    /**
     * v7.9.6: Fetch feed posts for "Following" — only from followed users.
     * Filter official=eq.false (Komunitas = user posts only, bukan news/official).
     */
    public JSONArray fetchFeedPostsFollowing(String sortBy, int limit) throws Exception {
        String order = "created_at.desc";
        if ("oldest".equals(sortBy)) order = "created_at.asc";
        // Get list of following IDs first
        JSONArray follows = new JSONArray(request("GET",
            "/rest/v1/community_follows?follower_id=eq." + enc(userId()) + "&select=following_id",
            null, true, false));
        if (follows.length() == 0) return new JSONArray();

        StringBuilder inList = new StringBuilder();
        for (int i = 0; i < follows.length(); i++) {
            if (i > 0) inList.append(",");
            inList.append(enc(follows.getJSONObject(i).getString("following_id")));
        }

        return new JSONArray(request("GET",
            "/rest/v1/feed_posts?type=neq.issue&author_id=in.(" + inList + ")&official=eq.false&order=" + order + "&limit=" + limit +
            "&select=id,author_id,title,body,image_url,type,pinned,official,created_at",
            null, true, false));
    }

    /** Create a new feed post with image. Login required. Returns the created row. */
    public JSONObject createFeedPost(String title, String body, String imageUrl, String type) throws Exception {
        return createFeedPost(title, body, imageUrl, type, false);
    }

    /**
     * Create a new feed post (or draft if isDraft=true) with image.
     * Login required. Returns the created row.
     *
     * is_draft=true → post disimpan sebagai draft (tidak muncul di feed publik,
     * hanya di tab Draft pada ProfileScreen). User bisa publish draft kapan saja
     * via publishDraft(postId).
     */
    public JSONObject createFeedPost(String title, String body, String imageUrl, String type, boolean isDraft) throws Exception {
        JSONObject payload = new JSONObject()
            .put("author_id", userId())
            .put("title", title.trim())
            .put("body", body.trim())
            .put("type", type != null ? type : "community")
            .put("is_draft", isDraft);
        if (imageUrl != null && !imageUrl.trim().isEmpty()) payload.put("image_url", imageUrl.trim());

        JSONArray arr = new JSONArray(request("POST", "/rest/v1/feed_posts",
            payload, true, "return=representation"));
        JSONObject created = arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
        // Task 5: log activity + award badges (only for published posts, not drafts)
        if (!isDraft) {
            try {
                logActivity("create_post", new JSONObject().put("post_id", created.optString("id", "")));
                checkAndAwardBadges();
            } catch (Throwable ignored) { }
        }
        return created;
    }

    /**
     * Publish a previously-saved draft post (set is_draft=false).
     * Login required. Returns the updated row.
     */
    public JSONObject publishDraft(String postId) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        JSONObject body = new JSONObject().put("is_draft", false);
        JSONArray arr = new JSONArray(request("PATCH",
            "/rest/v1/feed_posts?id=eq." + enc(postId) + "&author_id=eq." + enc(userId()),
            body, true, "return=representation"));
        return arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
    }

    /** Delete a feed post (own only — RLS enforced). Login required. */
    public void deleteFeedPost(String postId) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        request("DELETE",
            "/rest/v1/feed_posts?id=eq." + enc(postId) + "&author_id=eq." + enc(userId()),
            null, true, false);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Feed likes (feed_likes table)
    //  Schema:
    //    post_id  uuid PK references feed_posts(id) on delete cascade
    //    user_id  uuid PK references profiles(id) on delete cascade
    //    created_at timestamptz default now()
    //  RLS: SELECT public, INSERT/DELETE owner only.
    // ──────────────────────────────────────────────────────────────────────

    /** Like a post (insert into feed_likes). Login required. */
    public void likePost(String postId) throws Exception {
        JSONObject body = new JSONObject().put("post_id", postId).put("user_id", userId());
        request("POST", "/rest/v1/feed_likes", body, true, "return=minimal");
    }

    /** Unlike a post. Login required. */
    public void unlikePost(String postId) throws Exception {
        request("DELETE", "/rest/v1/feed_likes?post_id=eq." + enc(postId) + "&user_id=eq." + enc(userId()),
            null, true, false);
    }

    /** Get like count for a post. Public read. */
    public int getPostLikeCount(String postId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/feed_likes?post_id=eq." + enc(postId) + "&select=post_id",
            null, false, false));
        return arr.length();
    }

    /** Check if current user liked a post. Login required. */
    public boolean hasLikedPost(String postId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/feed_likes?post_id=eq." + enc(postId) + "&user_id=eq." + enc(userId()) + "&select=post_id",
            null, true, false));
        return arr.length() > 0;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Storage upload (Supabase Storage bucket 'community-images')
    //  Bucket setup: see /home/z/my-project/download/supabase-fix-v9-storage.sql
    //  Path: userId/timestamp.jpg — RLS enforces foldername(name)[1] = auth.uid()
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Upload image ke Supabase Storage bucket 'community-images'.
     * Path: userId/filename.jpg (foldername(name)[1] must equal auth.uid() — RLS).
     * Returns the public URL of the uploaded object.
     *
     * @param imageBytes raw JPEG bytes
     * @param filename   e.g. "post_1234567890.jpg"
     */
    public String uploadImage(byte[] imageBytes, String filename) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        if (imageBytes == null || imageBytes.length == 0) throw new IllegalArgumentException("Image bytes kosong.");
        if (filename == null || filename.trim().isEmpty()) throw new IllegalArgumentException("Filename kosong.");

        String path = userId() + "/" + filename;
        String encodedPath = java.net.URLEncoder.encode(path, "UTF-8");
        String uploadUrl = SUPABASE_URL + "/storage/v1/object/community-images/" + encodedPath;

        HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token());
            conn.setRequestProperty("Content-Type", "image/jpeg");
            conn.setRequestProperty("x-upsert", "true");
            conn.getOutputStream().write(imageBytes);

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String err = "";
                java.io.InputStream errStream = conn.getErrorStream();
                if (errStream != null) {
                    try {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(errStream));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line).append('\n');
                        err = sb.toString().trim();
                    } catch (Throwable ignored) { }
                }
                throw new IllegalStateException("Upload gagal: HTTP " + code + (err.isEmpty() ? "" : " " + err));
            }
        } finally {
            conn.disconnect();
        }

        // Return public URL (bucket is public)
        return SUPABASE_URL + "/storage/v1/object/public/community-images/" + encodedPath;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Comments (feed_comments table)
    //  Schema:
    //    id uuid PK, post_id uuid FK feed_posts, user_id uuid FK profiles,
    //    body text (1..2000), deleted boolean default false,
    //    created_at timestamptz default now()
    //  RLS: SELECT public (deleted=false), INSERT owner (user_id), DELETE owner
    //  (see supabase-fix-v10-comments-phase2.sql for DELETE policy)
    // ──────────────────────────────────────────────────────────────────────

    /** Fetch comments for a post (oldest first). Public read (deleted=false only). */
    public JSONArray fetchComments(String postId) throws Exception {
        return new JSONArray(request("GET",
            "/rest/v1/feed_comments?post_id=eq." + enc(postId)
                + "&order=created_at.asc&limit=200"
                + "&select=id,post_id,user_id,body,created_at",
            null, false, false));
    }

    /** Add comment to a post. Login required. Returns the created row. */
    public JSONObject addComment(String postId, String body) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        if (body == null || body.trim().isEmpty()) throw new IllegalArgumentException("Komentar kosong.");
        JSONObject payload = new JSONObject()
            .put("post_id", postId)
            .put("user_id", userId())
            .put("body", body.trim());
        JSONArray arr = new JSONArray(request("POST", "/rest/v1/feed_comments",
            payload, true, "return=representation"));
        JSONObject created = arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
        // Task 5: log activity + award badges — fire-and-forget.
        try {
            logActivity("create_comment", new JSONObject().put("post_id", postId));
            checkAndAwardBadges();
        } catch (Throwable ignored) { }
        return created;
    }

    /** Delete (physical) a comment. Owner only (RLS enforced). */
    public void deleteComment(String commentId) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        request("DELETE", "/rest/v1/feed_comments?id=eq." + enc(commentId)
            + "&user_id=eq." + enc(userId()), null, true, false);
    }

    /** Get comment count for a post. Public read. */
    public int getCommentCount(String postId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/feed_comments?post_id=eq." + enc(postId) + "&deleted=eq.false&select=id",
            null, false, false));
        return arr.length();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Push notification polling — new posts from followed users
    //  Used by MainShell's 60s polling loop to fire local notifications.
    // ──────────────────────────────────────────────────────────────────────

    /** Get list of following user IDs. Login required. */
    public JSONArray fetchFollowingIds() throws Exception {
        if (!loggedIn()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/community_follows?follower_id=eq." + enc(userId()) + "&select=following_id",
            null, true, false));
    }

    /**
     * Fetch new posts from followed users since the given timestamp (epoch millis).
     * Returns max 10 newest posts. Login required.
     *
     * @param followingIds  list of user IDs (from fetchFollowingIds)
     * @param sinceTimestamp epoch millis (UTC). Posts with created_at &gt; this will be returned.
     */
    public JSONArray fetchNewPostsFromFollowing(java.util.List<String> followingIds, long sinceTimestamp) throws Exception {
        if (followingIds == null || followingIds.isEmpty()) return new JSONArray();
        StringBuilder inList = new StringBuilder();
        for (int i = 0; i < followingIds.size(); i++) {
            if (i > 0) inList.append(",");
            inList.append(enc(followingIds.get(i)));
        }
        // Format timestamp as ISO-8601 UTC (Supabase timestamptz comparison)
        String sinceIso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .format(new java.util.Date(sinceTimestamp));
        return new JSONArray(request("GET",
            "/rest/v1/feed_posts?type=neq.issue&author_id=in.(" + inList + ")"
                + "&created_at=gt." + enc(sinceIso)
                + "&order=created_at.desc&limit=10"
                + "&select=id,author_id,title,body,created_at",
            null, true, false));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Saved posts (saved_posts table) — bookmark feature
    //  Schema:
    //    post_id  uuid PK references feed_posts(id) on delete cascade
    //    user_id  uuid PK references profiles(id) on delete cascade
    //    created_at timestamptz default now()
    //  RLS: SELECT/INSERT/DELETE owner only.
    // ──────────────────────────────────────────────────────────────────────

    /** Save (bookmark) a post. Login required. */
    public void savePost(String postId) throws Exception {
        JSONObject body = new JSONObject().put("post_id", postId).put("user_id", userId());
        request("POST", "/rest/v1/saved_posts?on_conflict=post_id,user_id", body, true,
            "resolution=merge-duplicates,return=minimal");
    }

    /** Unsave (remove bookmark) a post. Login required. */
    public void unsavePost(String postId) throws Exception {
        request("DELETE", "/rest/v1/saved_posts?post_id=eq." + enc(postId) + "&user_id=eq." + enc(userId()),
            null, true, false);
    }

    /** Check if current user saved a post. Login required. */
    public boolean hasSavedPost(String postId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/saved_posts?post_id=eq." + enc(postId) + "&user_id=eq." + enc(userId()) + "&select=post_id",
            null, true, false));
        return arr.length() > 0;
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Reports (reports table) — flag a post
    //  Schema:
    //    id uuid PK, reporter_id uuid, target_type text, target_id text,
    //    category text, reason text, status report_status, created_at
    //  RLS: INSERT owner (reporter) only.
    // ──────────────────────────────────────────────────────────────────────

    /** Report a feed post. Login required. category: spam|inappropriate|scam|other. */
    public void reportPost(String postId, String category, String reason) throws Exception {
        JSONObject body = new JSONObject()
            .put("reporter_id", userId())
            .put("target_type", "post")
            .put("target_id", postId)
            .put("category", category != null ? category : "other")
            .put("reason", reason != null ? reason.trim() : "");
        request("POST", "/rest/v1/reports", body, true, "return=minimal");
    }

    /**
     * Convenience overload — report a post with default category "community".
     * Matches the simplified call site signature `reportPost(postId, reason)`.
     * Login required.
     */
    public void reportPost(String postId, String reason) throws Exception {
        JSONObject body = new JSONObject()
            .put("reporter_id", userId())
            .put("target_type", "post")
            .put("target_id", postId)
            .put("category", "community")
            .put("reason", reason != null ? reason.trim() : "");
        request("POST", "/rest/v1/reports", body, true, "return=minimal");
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Profile avatar upload (Supabase Storage bucket 'community-images')
    //  Avatar path: userId/avatar_<userId>.jpg (foldername(name)[1] = auth.uid() — RLS).
    //  Avatar URL disimpan di kolom `avatar_url` profiles table.
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Upload avatar image ke Supabase Storage bucket 'community-images'.
     * Path: userId/avatar_<userId>.jpg (x-upsert:true → overwrite kalau sudah ada).
     * Returns the public URL of the uploaded avatar object.
     *
     * @param imageBytes raw JPEG bytes (size < 2MB recommended)
     */
    public String uploadAvatar(byte[] imageBytes) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        if (imageBytes == null || imageBytes.length == 0)
            throw new IllegalArgumentException("Image bytes kosong.");

        String filename = "avatar_" + userId() + ".jpg";
        String path = userId() + "/" + filename;
        String encodedPath = java.net.URLEncoder.encode(path, "UTF-8");
        String url = SUPABASE_URL + "/storage/v1/object/community-images/" + encodedPath;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + token());
            conn.setRequestProperty("Content-Type", "image/jpeg");
            conn.setRequestProperty("x-upsert", "true");
            conn.getOutputStream().write(imageBytes);

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String err = "";
                java.io.InputStream errStream = conn.getErrorStream();
                if (errStream != null) {
                    try {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(errStream));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line).append('\n');
                        err = sb.toString().trim();
                    } catch (Throwable ignored) { }
                }
                throw new IllegalStateException("Upload avatar gagal: HTTP " + code + (err.isEmpty() ? "" : " " + err));
            }
        } finally {
            conn.disconnect();
        }

        return SUPABASE_URL + "/storage/v1/object/public/community-images/" + encodedPath;
    }

    /**
     * Update `avatar_url` column on the logged-in user's profile row.
     * Also writes the new URL to local prefs so avatarUrl() returns it immediately.
     */
    public void updateAvatar(String avatarUrl) throws Exception {
        if (userId().isEmpty()) throw new IllegalStateException("Belum login.");
        if (avatarUrl == null || avatarUrl.trim().isEmpty())
            throw new IllegalArgumentException("Avatar URL kosong.");
        JSONObject body = new JSONObject().put("avatar_url", avatarUrl.trim());
        request("PATCH", "/rest/v1/profiles?id=eq." + enc(userId()), body, true, "return=minimal");
        // Update local prefs supaya avatarUrl() langsung return URL baru tanpa perlu reload profile
        prefs.edit().putString("avatar_url", avatarUrl.trim()).apply();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Profile lookup — for author info in feed cards
    // ──────────────────────────────────────────────────────────────────────

    /** Get profile by ID (for author info in feed + UserProfileScreen). Public read. */
    public JSONObject getProfileById(String profileId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/profiles?id=eq." + enc(profileId) + "&select=id,username,display_name,avatar_url,role,unique_id,bio",
            null, false, false));
        return arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
    }

    /**
     * PATCH country column on the logged-in user's profile row.
     * Called after successful registration.
     */
    public JSONObject updateCountry(String country) throws Exception {
        if (userId().isEmpty()) throw new IllegalStateException("Belum login.");
        if (country == null || country.trim().isEmpty()) throw new IllegalStateException("Country kosong.");
        JSONObject body = new JSONObject();
        body.put("country", country.trim());
        JSONArray arr = new JSONArray(request("PATCH", "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=id,country", body, true, "return=representation"));
        setCountry(country);
        if (arr.length() > 0) return arr.getJSONObject(0);
        return new JSONObject();
    }

    /**
     * v7.9.17: PATCH user_type column (e.g., "player", "modder", "explorer").
     * Dipanggil dari OnboardingModal setelah user pilih role.
     */
    public JSONObject updateUserType(String userType) throws Exception {
        if (userId().isEmpty()) throw new IllegalStateException("Belum login.");
        JSONObject body = new JSONObject();
        body.put("user_type", userType == null ? "" : userType.trim());
        JSONArray arr = new JSONArray(request("PATCH", "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=id,user_type", body, true, "return=representation"));
        if (arr.length() > 0) return arr.getJSONObject(0);
        return new JSONObject();
    }

    /**
     * v7.9.17: PATCH use_case column (e.g., "playing", "personal", "community").
     * Dipanggil dari OnboardingModal setelah user pilih use case.
     */
    public JSONObject updateUseCase(String useCase) throws Exception {
        if (userId().isEmpty()) throw new IllegalStateException("Belum login.");
        JSONObject body = new JSONObject();
        body.put("use_case", useCase == null ? "" : useCase.trim());
        JSONArray arr = new JSONArray(request("PATCH", "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=id,use_case", body, true, "return=representation"));
        if (arr.length() > 0) return arr.getJSONObject(0);
        return new JSONObject();
    }

    /**
     * v7.9.17: PATCH android_version column (user input dari onboarding).
     * Hanya angka + titik yang diterima (keyboard numeric).
     */
    public JSONObject updateAndroidVersion(String version) throws Exception {
        if (userId().isEmpty()) throw new IllegalStateException("Belum login.");
        JSONObject body = new JSONObject();
        body.put("android_version", version == null ? "" : version.trim());
        JSONArray arr = new JSONArray(request("PATCH", "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=id,android_version", body, true, "return=representation"));
        if (arr.length() > 0) return arr.getJSONObject(0);
        return new JSONObject();
    }

    /**
     * v7.9.17: PATCH country column (user input dari onboarding, pindah dari register form).
     */
    public JSONObject updateCountryFromOnboarding(String country) throws Exception {
        return updateCountry(country);  // reuse existing updateCountry
    }

    /** v7.9.17: Get current user_type dari prefs (cached from loadMyProfile). */
    public String userType() { return prefs.getString("user_type", ""); }
    public void setUserType(String ut) { prefs.edit().putString("user_type", ut == null ? "" : ut).apply(); }

    /** v7.9.17: Get current use_case dari prefs (cached from loadMyProfile). */
    public String useCase() { return prefs.getString("use_case", ""); }
    public void setUseCase(String uc) { prefs.edit().putString("use_case", uc == null ? "" : uc).apply(); }

    /** v7.9.17: Get current android_version dari prefs (cached). */
    public String androidVersion() { return prefs.getString("android_version", ""); }
    public void setAndroidVersion(String av) { prefs.edit().putString("android_version", av == null ? "" : av).apply(); }

    /**
     * Fetch a single key from app_config (e.g. "maintenance").
     * Returns the parsed JSON object inside value column (jsonb), or empty object on failure.
     */
    public JSONObject getAppConfig(String key) throws Exception {
        JSONArray arr = new JSONArray(request("GET", "/rest/v1/app_config?key=eq." + enc(key) + "&select=key,value", null, false, (String) null));
        if (arr.length() == 0) return new JSONObject();
        JSONObject row = arr.getJSONObject(0);
        Object value = row.opt("value");
        if (value instanceof JSONObject) return (JSONObject) value;
        if (value != null) return new JSONObject().put("raw", value.toString());
        return new JSONObject();
    }

    // Note (v3.0.0 cleanup): the legacy `logEvent(...)` method was removed.
    // It used the wrong column name (`event_data` instead of `metadata`) and was
    // never called from any caller. All telemetry now goes through `Telemetry.kt`,
    // which writes to `app_events` with the correct schema (user_id, event_type,
    // app_version, country, device_info jsonb, metadata jsonb).

    /**
     * Fetch recent sent notification campaigns ordered by sent_at desc.
     *
     * Bug 4 fix:
     *   - auth=true (pakai user access token, supaya RLS check auth.uid() pass).
     *   - Filter sent_at=not.null (hanya yang sudah dikirim).
     *   - Order by sent_at.desc (bukan created_at.desc, supaya yang baru dikirim di atas).
     *   - Limit max 20 (sebelumnya 50, terlalu banyak untuk inline banner).
     *
     * RLS requirement: notification_campaigns harus punya policy SELECT untuk
     * all authenticated users (lihat supabase-fix-v6-notif-rls.sql).
     */
    public JSONArray getNotifications(int limit) throws Exception {
        int safe = Math.max(1, Math.min(limit, 20));
        return new JSONArray(request("GET",
            "/rest/v1/notification_campaigns?sent_at=not.null&order=sent_at.desc&limit=" + safe +
            "&select=id,created_by,title,body,target,action,sent_at,created_at",
            null, true, (String) null));  // auth=true → pakai user access token (RLS)
    }

    /**
     * Fetch recent sent notification campaigns filtered by category.
     * Categories: "all", "update", "announcement", "maintenance", "community".
     *
     * Notes:
     *   - Column `category` ditambahkan ke notification_campaigns (default 'announcement').
     *     Kalau kolom belum ada di Supabase (schema lama), filter category=eq.X akan 400.
     *     Caller wajib wrap dengan try/catch dan fallback ke getNotifications(limit).
     *   - auth=true (RLS check auth.uid() pass).
     */
    public JSONArray getNotificationsByCategory(int limit, String category) throws Exception {
        int safe = Math.max(1, Math.min(limit, 20));
        String filter = "sent_at=not.null";
        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("all")) {
            filter += "&category=eq." + enc(category.trim());
        }
        return new JSONArray(request("GET",
            "/rest/v1/notification_campaigns?" + filter + "&order=sent_at.desc&limit=" + safe +
            "&select=id,created_by,title,body,target,action,sent_at,created_at,category",
            null, true, (String) null));  // auth=true → pakai user access token (RLS)
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Game ratings (game_ratings table — DLavie 26 community rating)
    //  Schema:
    //    user_id   uuid PK references profiles(id)
    //    rating    smallint NOT NULL CHECK (rating BETWEEN 1 AND 5)
    //    review    text
    //    created_at timestamptz default now()
    //    updated_at timestamptz default now()
    //  RLS: SELECT public (anon + auth), INSERT/UPDATE owner only.
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Fetch average rating (1-5) and total count.
     * Returns: { "avg": <double 1-5>, "count": <int> }.
     * Public read (anon key) — does NOT require login.
     */
    public JSONObject fetchRatingStats() throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/game_ratings?select=rating", null, false, false));
        if (arr.length() == 0) return new JSONObject().put("avg", 0.0).put("count", 0);
        double sum = 0;
        for (int i = 0; i < arr.length(); i++) sum += arr.getJSONObject(i).optInt("rating", 0);
        double avg = sum / arr.length();
        return new JSONObject().put("avg", avg).put("count", arr.length());
    }

    /**
     * Submit or update current user's rating (1-5 stars).
     * Uses upsert (on_conflict=user_id, resolution=merge-duplicates).
     * Login required.
     */
    public void submitRating(int rating, String review) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating harus 1-5.");
        JSONObject body = new JSONObject()
            .put("user_id", userId())
            .put("rating", rating);
        if (review != null && !review.trim().isEmpty()) body.put("review", review.trim());
        request("POST", "/rest/v1/game_ratings?on_conflict=user_id", body, true,
            "resolution=merge-duplicates,return=minimal");
        // Task 5: log activity + award badges — fire-and-forget.
        try {
            logActivity("rate_game", new JSONObject().put("rating", rating));
            checkAndAwardBadges();
        } catch (Throwable ignored) { }
    }

    /**
     * Get current user's rating (1-5), or 0 if not yet rated.
     * Login required.
     */
    public int getMyRating() throws Exception {
        if (!loggedIn()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/game_ratings?user_id=eq." + enc(userId()) + "&select=rating",
            null, true, false));
        return arr.length() > 0 ? arr.getJSONObject(0).optInt("rating", 0) : 0;
    }

    /**
     * Fetch published update_posts dari Supabase.
     * Update posts dibuat oleh developer via Dev Dashboard dan mewakili
     * patch announcements yang sudah published (published=true), diurutkan
     * by version_code desc (terbaru di index 0).
     *
     * Schema update_posts (Supabase):
     *   id, author_id, version_code, version_name, channel, title, body,
     *   release_notes (text[]), known_issues (text[]), patch_url, patch_sha256,
     *   patch_size_bytes, critical, restart_game_required, risk_level, published,
     *   created_at, updated_at
     *
     * @return JSONArray of published update posts (max 10).
     */
    public JSONArray fetchUpdatePosts() throws Exception {
        return new JSONArray(request("GET",
            "/rest/v1/update_posts?published=eq.true&order=version_code.desc&limit=10"
                + "&select=id,version_code,version_name,channel,title,body,release_notes,"
                + "known_issues,patch_url,patch_sha256,patch_size_bytes,critical,"
                + "restart_game_required,risk_level,created_at",
            null, false, false));
    }

    /**
     * Fetch latest published update_post (highest version_code).
     *
     * @return the newest published update post, or null kalau tidak ada satu pun.
     */
    public JSONObject fetchLatestUpdatePost() throws Exception {
        JSONArray arr = fetchUpdatePosts();
        return arr.length() > 0 ? arr.getJSONObject(0) : null;
    }

    public JSONObject refreshToken() throws Exception {
        String refresh = prefs.getString("refresh_token", "");
        if (refresh.isEmpty()) throw new IllegalStateException("No refresh token.");
        JSONObject body = new JSONObject();
        body.put("refresh_token", refresh);
        JSONObject res = new JSONObject(request("POST", "/auth/v1/token?grant_type=refresh_token", body, false, (String) null));
        storeSessionIfPresent(res);
        return res;
    }

    public JSONArray categories() throws Exception {
        return new JSONArray(request("GET", "/rest/v1/community_categories?select=id,slug,name,description&order=sort_order.asc", null, false, false));
    }

    /**
     * Fetch FAQ items from Supabase (faq_items table).
     * Schema (assumed):
     *   id, question, answer, category, sort_order, published, created_at
     * Public read (anon key) — does NOT require login.
     * Returns empty array kalau tabel belum ada / error (fail-open).
     */
    public JSONArray faqItems() throws Exception {
        return new JSONArray(request("GET",
            "/rest/v1/faq_items?published=eq.true&order=sort_order.asc&limit=50"
                + "&select=id,question,answer,category,sort_order",
            null, false, false));
    }

    /**
     * Fetch current user's support tickets from Supabase (support_tickets table).
     * Schema (assumed):
     *   id, user_id, subject, body, status, priority, created_at, updated_at
     * Auth read (pakai user access token, RLS owner-only).
     * Returns empty array kalau tabel belum ada / error / user belum punya tiket.
     */
    public JSONArray supportTickets() throws Exception {
        if (!loggedIn()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/support_tickets?user_id=eq." + enc(userId())
                + "&order=created_at.desc&limit=50"
                + "&select=id,subject,body,status,priority,created_at,updated_at",
            null, true, false));
    }

    public JSONArray topics(String categoryId) throws Exception {
        String path = "/rest/v1/topics?select=id,category_id,author_id,title,body,reply_count,last_post_at,created_at,is_pinned&order=last_post_at.desc&limit=40";
        if (categoryId != null && !categoryId.isEmpty()) path = "/rest/v1/topics?category_id=eq." + enc(categoryId) + "&select=id,category_id,author_id,title,body,reply_count,last_post_at,created_at,is_pinned&order=last_post_at.desc&limit=40";
        return new JSONArray(request("GET", path, null, false, false));
    }

    public JSONObject createTopic(String categoryId, String title, String bodyText) throws Exception {
        JSONObject body = new JSONObject();
        body.put("category_id", categoryId);
        body.put("author_id", userId());
        body.put("title", title.trim());
        body.put("body", bodyText.trim());
        JSONArray arr = new JSONArray(request("POST", "/rest/v1/topics", body, true, true));
        if (arr.length() == 0) throw new IllegalStateException("Topic tidak dibuat.");
        return arr.getJSONObject(0);
    }

    public JSONArray posts(String topicId) throws Exception {
        return new JSONArray(request("GET", "/rest/v1/posts?topic_id=eq." + enc(topicId) + "&select=id,topic_id,author_id,reply_to_id,body,created_at&order=created_at.asc&limit=120", null, false, false));
    }

    public JSONObject createPost(String topicId, String replyToId, String bodyText) throws Exception {
        JSONObject body = new JSONObject();
        body.put("topic_id", topicId);
        body.put("author_id", userId());
        if (replyToId != null && !replyToId.isEmpty()) body.put("reply_to_id", replyToId);
        body.put("body", bodyText.trim());
        JSONArray arr = new JSONArray(request("POST", "/rest/v1/posts", body, true, true));
        if (arr.length() == 0) throw new IllegalStateException("Reply tidak dibuat.");
        JSONObject post = arr.getJSONObject(0);
        createMentions(post.optString("id"), bodyText);
        return post;
    }

    private void createMentions(String postId, String bodyText) {
        try {
            ArrayList<String> names = mentionsFrom(bodyText);
            if (names.isEmpty()) return;
            StringBuilder in = new StringBuilder();
            for (int i = 0; i < names.size(); i++) { if (i > 0) in.append(','); in.append(names.get(i)); }
            JSONArray users = new JSONArray(request("GET", "/rest/v1/profiles?username=in.(" + in + ")&select=id,username", null, true, false));
            JSONArray payload = new JSONArray();
            for (int i = 0; i < users.length(); i++) {
                JSONObject u = users.getJSONObject(i);
                JSONObject m = new JSONObject();
                m.put("post_id", postId);
                m.put("mentioned_user_id", u.getString("id"));
                payload.put(m);
            }
            if (payload.length() > 0) request("POST", "/rest/v1/mentions", payload, true, false);
        } catch (Throwable ignored) { }
    }

    private ArrayList<String> mentionsFrom(String s) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        Matcher m = Pattern.compile("@([a-zA-Z0-9_]{3,24})").matcher(s == null ? "" : s);
        while (m.find()) set.add(m.group(1));
        return new ArrayList<>(set);
    }

    // ════════════════════════════════════════════════════════════════════
    //  PROFILE REMAKE — TapTap style stats (Following / Followers / Likes)
    //  + Badges + Game Saya (play time) + Tabs (Post / Tersimpan / Draft)
    // ════════════════════════════════════════════════════════════════════

    /** Count users that the current user is following. Login required. */
    public int getFollowingCount() throws Exception {
        if (!loggedIn()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/community_follows?follower_id=eq." + enc(userId()) + "&select=follower_id",
            null, true, false));
        return arr.length();
    }

    /** Count users that follow the current user. Login required. */
    public int getFollowerCount() throws Exception {
        if (!loggedIn()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/community_follows?following_id=eq." + enc(userId()) + "&select=following_id",
            null, true, false));
        return arr.length();
    }

    /**
     * Count total likes received across all of the current user's posts.
     * Login required (RLS on feed_likes is public read, but we restrict to logged-in
     * callers since this is a profile stat).
     */
    public int getMyLikesReceived() throws Exception {
        if (!loggedIn()) return 0;
        // Get all of my post IDs first
        JSONArray posts = new JSONArray(request("GET",
            "/rest/v1/feed_posts?author_id=eq." + enc(userId()) + "&is_draft=eq.false&select=id",
            null, true, false));
        if (posts.length() == 0) return 0;
        StringBuilder inList = new StringBuilder();
        for (int i = 0; i < posts.length(); i++) {
            if (i > 0) inList.append(",");
            inList.append(enc(posts.getJSONObject(i).optString("id", "")));
        }
        JSONArray likes = new JSONArray(request("GET",
            "/rest/v1/feed_likes?post_id=in.(" + inList + ")&select=post_id",
            null, false, false));
        return likes.length();
    }

    /** Fetch current user's published posts (is_draft=false), newest first. Login required. */
    public JSONArray getMyPosts() throws Exception {
        if (!loggedIn()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/feed_posts?author_id=eq." + enc(userId()) + "&is_draft=eq.false&order=created_at.desc&select=*",
            null, true, false));
    }

    /** Fetch current user's draft posts (is_draft=true), newest first. Login required. */
    public JSONArray getMyDrafts() throws Exception {
        if (!loggedIn()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/feed_posts?author_id=eq." + enc(userId()) + "&is_draft=eq.true&order=created_at.desc&select=*",
            null, true, false));
    }

    /**
     * Fetch current user's saved (bookmarked) posts, newest first.
     * Returns rows with nested feed_posts(*) so we can render the post card directly.
     * Login required (RLS on saved_posts is owner-only).
     */
    public JSONArray getSavedPosts() throws Exception {
        if (!loggedIn()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/saved_posts?user_id=eq." + enc(userId())
                + "&order=created_at.desc&select=post_id,created_at,feed_posts(*)",
            null, true, false));
    }

    /** Fetch current user's earned badges (badge_code + earned_at). Login required. */
    public JSONArray getMyBadges() throws Exception {
        if (!loggedIn()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/user_badges?user_id=eq." + enc(userId()) + "&select=badge_code,earned_at",
            null, true, false));
    }

    /** Fetch all badge definitions (code, name, description, icon, requirement_*). Public read. */
    public JSONArray getAllBadges() throws Exception {
        return new JSONArray(request("GET",
            "/rest/v1/badges?order=code.asc&select=code,name,description,icon,requirement_type,requirement_value",
            null, false, false));
    }

    /** Get the current user's unique_id (profiles.unique_id). Login required. Returns 0 if not set. */
    public int getUniqueId() throws Exception {
        if (!loggedIn()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=unique_id",
            null, true, false));
        return arr.length() > 0 ? arr.getJSONObject(0).optInt("unique_id", 0) : 0;
    }

    /**
     * Get the current user's total play time (in minutes) by summing all
     * game_sessions.duration_minutes rows. Login required.
     */
    public int getTotalPlayTime() throws Exception {
        if (!loggedIn()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/game_sessions?user_id=eq." + enc(userId()) + "&select=duration_minutes",
            null, true, false));
        int total = 0;
        for (int i = 0; i < arr.length(); i++) {
            total += arr.getJSONObject(i).optInt("duration_minutes", 0);
        }
        return total;
    }

    /** Get the current user's bio (profiles.bio). Login required. */
    public String getMyBio() throws Exception {
        if (!loggedIn()) return "";
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=bio",
            null, true, false));
        return arr.length() > 0 ? arr.getJSONObject(0).optString("bio", "") : "";
    }

    // ════════════════════════════════════════════════════════════════════
    //  PLAY TIME TRACKING — game_sessions + user_activities + auto-award badges
    // ════════════════════════════════════════════════════════════════════

    /**
     * Record a completed game session.
     * Inserts a row into game_sessions (started_at, ended_at, duration_minutes) and
     * also logs an activity into user_activities (activity_type="play_game").
     *
     * Task 2 + Task 5 fixes:
     *  - Logs `game_session` activity with duration_minutes metadata.
     *  - On failure, logs warning via Log.w (TAG=DLavieApi) so "game_sessions KOSONG"
     *    becomes diagnosable from logcat (RLS rejection, network, etc.).
     *
     * Both writes use Prefer: return=minimal (fire-and-forget).
     * Login required.
     *
     * @param startedAt        epoch millis when session began (set by GameSessionTracker)
     * @param durationMinutes  session length in whole minutes (>=1)
     */
    public void recordGameSession(long startedAt, int durationMinutes) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        if (durationMinutes <= 0) {
            Log.w(TAG, "recordGameSession: skip (durationMinutes=" + durationMinutes + " <= 0)");
            return;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
        JSONObject body = new JSONObject()
            .put("user_id", userId())
            .put("started_at", sdf.format(new java.util.Date(startedAt)))
            .put("ended_at", sdf.format(new java.util.Date()))
            .put("duration_minutes", durationMinutes);
        try {
            request("POST", "/rest/v1/game_sessions", body, true, "return=minimal");
            Log.i(TAG, "recordGameSession: OK (user=" + userId() + ", dur=" + durationMinutes + "min)");
        } catch (Throwable e) {
            // Task 2 fix: log warning supaya bisa debug dari logcat.
            // Filter: `adb logcat -s DLavieApi` untuk lihat error recordGameSession.
            Log.w(TAG, "recordGameSession FAILED: " + e.getMessage(), e);
            throw e;
        }

        // Task 5: log a user_activities row so the activity feed (Dev Dashboard) picks it up.
        try {
            JSONObject activity = new JSONObject()
                .put("user_id", userId())
                .put("activity_type", "play_game")
                .put("metadata", new JSONObject().put("duration_minutes", durationMinutes));
            request("POST", "/rest/v1/user_activities", activity, true, "return=minimal");
            // Also fire generic logActivity for the activity feed aggregator.
            logActivity("game_session", new JSONObject().put("duration_minutes", durationMinutes));
        } catch (Throwable ignored) { }
    }

    /**
     * Check all badge requirements and award any newly-earned badges to the current
     * user. Idempotent — already-earned badges are skipped.
     *
     * Task 3 fix (v3.0.0): replaced client-side loop with single server-side RPC call.
     * POST /rest/v1/rpc/check_and_award_badges { p_user_id }
     *   → returns jsonb array of newly-awarded badge codes (e.g. ["first_login","rate_1"])
     *
     * Benefits:
     *   - Single round-trip (was N+1 queries for catalog + per-badge check).
     *   - All requirement logic lives in SQL (security definer) — atomic + consistent.
     *   - RLS for `user_badges` insert uses `auth.uid() = user_id` (SQL v14) → works
     *     because RPC runs as `security definer` (bypasses RLS for the insert itself,
     *     but still respects p_user_id ownership).
     *
     * Returns the raw response (jsonb array or object). Callers may ignore it — the
     * awarding side-effect is what matters.
     *
     * @return parsed response (JSONArray if function returns array, JSONObject otherwise)
     */
    public Object checkAndAwardBadges() throws Exception {
        if (!loggedIn()) return new JSONArray();
        JSONObject body = new JSONObject().put("p_user_id", userId());
        try {
            String resp = request("POST", "/rest/v1/rpc/check_and_award_badges", body, true, null);
            // RPC response can be: array, object, or empty string.
            if (resp == null || resp.trim().isEmpty()) return new JSONArray();
            String trimmed = resp.trim();
            if (trimmed.startsWith("[")) {
                JSONArray awarded = new JSONArray(trimmed);
                if (awarded.length() > 0) {
                    Log.i(TAG, "checkAndAwardBadges: awarded " + awarded.length() + " badge(s) — " + awarded);
                }
                return awarded;
            } else if (trimmed.startsWith("{")) {
                return new JSONObject(trimmed);
            } else {
                // Unexpected shape — log + return empty
                Log.w(TAG, "checkAndAwardBadges: unexpected response shape — " + trimmed.substring(0, Math.min(120, trimmed.length())));
                return new JSONArray();
            }
        } catch (Throwable e) {
            // Log warning — badges not critical, swallow to avoid crashing caller.
            Log.w(TAG, "checkAndAwardBadges FAILED: " + e.getMessage());
            return new JSONArray();
        }
    }

    /**
     * Insert a row into user_activities (used by Dev Dashboard activity feed).
     * Fire-and-forget on failure. Login required.
     *
     * @param type     activity_type, e.g. "play_game", "post_created", "comment_added"
     * @param metadata optional jsonb metadata (pass null to omit)
     */
    public void logActivity(String type, JSONObject metadata) throws Exception {
        if (!loggedIn()) return;
        JSONObject body = new JSONObject()
            .put("user_id", userId())
            .put("activity_type", type);
        if (metadata != null) body.put("metadata", metadata);
        request("POST", "/rest/v1/user_activities", body, true, "return=minimal");
    }

    // ════════════════════════════════════════════════════════════════════
    //  USER SEARCH + VISIT PROFILE (Task 3 + Task 4)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Search profiles by username (case-insensitive ilike, * wildcard).
     * Returns up to 10 results. Public read (RLS profiles public read).
     */
    public JSONArray searchUsers(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) return new JSONArray();
        // v7.0.5: include last_seen_at + updated_at for presence (fallback to updated_at if last_seen_at null)
        return new JSONArray(request("GET",
            "/rest/v1/profiles?username=ilike.*" + enc(query.trim()) + "*&select=id,username,display_name,avatar_url,unique_id,role,last_seen_at,updated_at&limit=10",
            null, false, false));
    }

    /**
     * v7.0.5: Update last_seen_at timestamp (presence heartbeat).
     * Called every 60s while app is in foreground + on app open.
     * Used to show "Online" / "Last seen 5m ago" badge in user list.
     */
    public void updateLastSeen() throws Exception {
        if (userId().isEmpty()) {
            android.util.Log.w("DLaviePresence", "updateLastSeen skipped — userId empty (not logged in)");
            return;
        }
        // v7.0.5: send ISO 8601 UTC timestamp — Supabase stores as timestamptz
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new java.util.Date());
        android.util.Log.d("DLaviePresence", "Updating last_seen_at for user " + userId() + " → " + timestamp);
        JSONObject body = new JSONObject();
        body.put("last_seen_at", timestamp);
        try {
            String resp = request("PATCH", "/rest/v1/profiles?id=eq." + enc(userId()), body, true, "return=minimal");
            android.util.Log.d("DLaviePresence", "Update OK, resp: " + resp);
        } catch (Exception e) {
            android.util.Log.e("DLaviePresence", "Update FAILED: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fetch a profile by username (exact match). Returns empty object if not found.
     * Public read.
     */
    /**
     * v7.1.2: Fetch cached translation for a post. Returns null if not available.
     * Translations are pre-generated by translate-posts.cjs cron (GitHub Actions).
     *
     * @param postId UUID of the feed post
     * @param langCode Target language code (en, id, ms, pt, es, de, fr, ja, zh, ar)
     * @return JSONObject with translated_title, translated_body, source_lang — or null
     */
    public JSONObject getPostTranslation(String postId, String langCode) throws Exception {
        if (postId == null || postId.isEmpty() || langCode == null) return null;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/post_translations?post_id=eq." + enc(postId) + "&lang_code=eq." + enc(langCode) + "&select=translated_title,translated_body,source_lang&limit=1",
            null, false, false));
        return arr.length() > 0 ? arr.getJSONObject(0) : null;
    }

    public JSONObject getProfileByUsername(String username) throws Exception {
        if (username == null || username.trim().isEmpty()) return new JSONObject();
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/profiles?username=eq." + enc(username.trim())
                + "&select=id,username,display_name,avatar_url,unique_id,role,bio",
            null, false, false));
        return arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
    }

    /** Count users that the given user is following. Public read. */
    public int getFollowingCountForUser(String uid) throws Exception {
        if (uid == null || uid.isEmpty()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/community_follows?follower_id=eq." + enc(uid) + "&select=follower_id",
            null, false, false));
        return arr.length();
    }

    /** Count users that follow the given user. Public read. */
    public int getFollowerCountForUser(String uid) throws Exception {
        if (uid == null || uid.isEmpty()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/community_follows?following_id=eq." + enc(uid) + "&select=following_id",
            null, false, false));
        return arr.length();
    }

    /** Count total likes received across all of the given user's posts. Public read. */
    public int getLikesReceivedForUser(String uid) throws Exception {
        if (uid == null || uid.isEmpty()) return 0;
        JSONArray posts = new JSONArray(request("GET",
            "/rest/v1/feed_posts?author_id=eq." + enc(uid) + "&is_draft=eq.false&select=id",
            null, false, false));
        if (posts.length() == 0) return 0;
        StringBuilder inList = new StringBuilder();
        for (int i = 0; i < posts.length(); i++) {
            if (i > 0) inList.append(",");
            inList.append(enc(posts.getJSONObject(i).optString("id", "")));
        }
        JSONArray likes = new JSONArray(request("GET",
            "/rest/v1/feed_likes?post_id=in.(" + inList + ")&select=post_id",
            null, false, false));
        return likes.length();
    }

    /** Fetch a user's published posts (is_draft=false), newest first. Public read. */
    public JSONArray getPostsByUser(String uid) throws Exception {
        if (uid == null || uid.isEmpty()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/feed_posts?author_id=eq." + enc(uid)
                + "&is_draft=eq.false&order=created_at.desc&select=*",
            null, false, false));
    }

    /** Fetch a user's earned badges (badge_code + earned_at). Public read. */
    public JSONArray getBadgesByUser(String uid) throws Exception {
        if (uid == null || uid.isEmpty()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/user_badges?user_id=eq." + enc(uid) + "&select=badge_code,earned_at",
            null, false, false));
    }

    /** Get a user's total play time (in minutes). Public read. */
    public int getTotalPlayTimeByUser(String uid) throws Exception {
        if (uid == null || uid.isEmpty()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/game_sessions?user_id=eq." + enc(uid) + "&select=duration_minutes",
            null, false, false));
        int total = 0;
        for (int i = 0; i < arr.length(); i++) {
            total += arr.getJSONObject(i).optInt("duration_minutes", 0);
        }
        return total;
    }

    // ════════════════════════════════════════════════════════════════════

    private String request(String method, String path, Object body, boolean auth, boolean preferReturn) throws Exception {
        return request(method, path, body, auth, preferReturn ? "return=representation" : null);
    }

    private String request(String method, String path, Object body, boolean auth, String prefer) throws Exception {
        // First attempt
        try {
            return doRequest(method, path, body, auth, prefer);
        } catch (IllegalStateException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            // Task 5 fix: 401 → refresh JWT → retry once. Other 4xx/5xx propagate.
            if (auth && msg.contains("401")) {
                Log.w(TAG, "request 401 on " + method + " " + path + " — refreshing JWT & retrying once");
                try {
                    refreshToken();
                } catch (Throwable refreshErr) {
                    // Refresh gagal — wrap dengan pesan yang user-friendly.
                    Log.w(TAG, "refreshToken FAILED: " + refreshErr.getMessage());
                    throw new IllegalStateException("Sesi login kedaluwarsa. Silakan login ulang.", refreshErr);
                }
                // Retry dengan token baru
                return doRequest(method, path, body, auth, prefer);
            }
            // Task 5 fix: jaringan error → pesan jelas (bukan JSON parse error).
            if (msg.contains("Unable to resolve host") || msg.contains("Network is unreachable")
                || msg.contains("Connection timed out") || msg.contains("ECONNREFUSED")) {
                throw new IllegalStateException("Tidak dapat terhubung ke server. Periksa koneksi internet, lalu coba lagi.");
            }
            throw e;
        } catch (java.io.IOException io) {
            // Network-level failure (no HTTP response at all — e.g. no internet, DNS, TLS).
            Log.w(TAG, "request IOException on " + method + " " + path + " — " + io.getMessage());
            throw new IllegalStateException("Gangguan jaringan: " + io.getMessage()
                + ". Periksa koneksi internet, lalu coba lagi.", io);
        }
    }

    /**
     * Public request (pakai anon key, tidak butuh auth).
     * Untuk query yang RLS-nya allow public read (e.g. app_releases yang published).
     * Tapi kalau user login, pakai user token supaya RLS staff policy juga berlaku.
     */
    public String requestPublic(String method, String path) throws Exception {
        // Kalau user login, pakai auth=true supaya DAPAT draft releases (staff)
        // Kalau tidak login, pakai auth=false (anon key, hanya published)
        boolean auth = loggedIn();
        return request(method, path, null, auth, false);
    }

    // ── Admin delete — for Issue Manager (admin/dev only) ──
    // Uses the user's auth token to DELETE records. RLS allows DELETE for all,
    // so this works for admin/developer to delete any issue.
    public String adminDelete(String path) throws Exception {
        return request("DELETE", path, null, true, null);
    }

    // ── Admin get — for Issue Manager to fetch issues ──
    public String adminGet(String path) throws Exception {
        return request("GET", path, null, true, null);
    }

    private String doRequest(String method, String path, Object body, boolean auth, String prefer) throws Exception {
        URL url = new URL(SUPABASE_URL + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        c.setRequestProperty("apikey", SUPABASE_KEY);
        c.setRequestProperty("Authorization", "Bearer " + (auth && !token().isEmpty() ? token() : SUPABASE_KEY));
        c.setRequestProperty("Accept", "application/json");
        if (prefer != null && !prefer.trim().isEmpty()) c.setRequestProperty("Prefer", prefer);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes("UTF-8")); }
        }
        int code = c.getResponseCode();
        String text = read(code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream());
        c.disconnect();
        if (code < 200 || code >= 300) throw new IllegalStateException(cleanError(text, code));
        if (text == null || text.trim().isEmpty()) return "{}";
        return text;
    }

    /**
     * Register FCM token to user_fcm_tokens table (idempotent upsert).
     * Called by DLavieFirebaseMessagingService.onNewToken() and
     * FcmDiagnosticCard in ModernLauncherActivity.
     *
     * Uses the same doRequest() infrastructure as other API calls —
     * handles network errors, JWT refresh, etc. consistently.
     *
     * @param fcmToken FCM registration token from FirebaseMessaging.getInstance().token
     * @return true if upload succeeded, throws on failure
     */
    public boolean registerFcmToken(String fcmToken) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login");
        if (fcmToken == null || fcmToken.isEmpty()) throw new IllegalStateException("FCM token kosong");

        JSONObject payload = new JSONObject();
        payload.put("user_id", userId());
        payload.put("fcm_token", fcmToken);
        payload.put("device_info", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
            + " (Android " + android.os.Build.VERSION.RELEASE + ")");
        payload.put("is_active", true);

        // Use doRequest() — handles 401 refresh, network errors, etc.
        // Prefer: resolution=merge-duplicates for upsert on fcm_token unique constraint
        String result = request("POST", "/rest/v1/user_fcm_tokens", payload, true,
            "resolution=merge-duplicates,return=minimal");
        Log.i(TAG, "FCM token registered for user " + userId());
        return true;
    }

    private static String cleanError(String text, int code) {
        try {
            JSONObject o = new JSONObject(text == null ? "{}" : text);
            String msg = o.optString("msg", o.optString("message", text));
            if (msg == null || msg.trim().isEmpty()) msg = o.optString("hint", text);
            return String.format(Locale.US, "HTTP %d: %s", code, msg);
        } catch (Throwable t) { return "HTTP " + code + ": " + text; }
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static String enc(String s) throws Exception { return URLEncoder.encode(s, "UTF-8"); }
}
