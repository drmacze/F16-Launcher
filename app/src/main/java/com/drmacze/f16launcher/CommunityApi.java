package com.drmacze.f16launcher;

import android.content.Context;
import android.content.SharedPreferences;

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
    public static final String SUPABASE_URL = "https://lvmucsxbmadtsgrxuwmo.supabase.co";
    public static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx2bXVjc3hibWFkdHNncnh1d21vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI5ODUyODksImV4cCI6MjA5ODU2MTI4OX0.y-1sE6uYTn4Wbter6g6NozY6uojzD5x9YVeYif-5nJs";

    private final SharedPreferences prefs;

    public CommunityApi(Context ctx) { prefs = ctx.getSharedPreferences("dlavie_community", Context.MODE_PRIVATE); }

    public boolean loggedIn() { return !prefs.getString("access_token", "").isEmpty() && !prefs.getString("user_id", "").isEmpty(); }
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
        e.apply();
    }

    public String role() { return prefs.getString("role", "member"); }
    public String avatarUrl() { return prefs.getString("avatar_url", ""); }

    public JSONArray feedPosts() throws Exception {
        // Authenticated request — uses user's access token so RLS policies can resolve author + visibility.
        return new JSONArray(request("GET", "/rest/v1/feed_posts?select=id,author_id,title,body,image_url,type,visibility,pinned,official,created_at&order=pinned.desc,created_at.desc&limit=10", null, true, (String) null));
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

    /**
     * Insert a telemetry event into app_events table.
     * Fire-and-forget — caller wraps in try/catch + Dispatchers.IO.
     */
    public void logEvent(String eventType, JSONObject eventData, String appVersion, String country, JSONObject deviceInfo) throws Exception {
        if (!loggedIn()) return;
        JSONObject body = new JSONObject();
        body.put("user_id", userId());
        body.put("event_type", eventType);
        if (eventData != null) body.put("event_data", eventData); else body.put("event_data", new JSONObject());
        if (appVersion != null && !appVersion.isEmpty()) body.put("app_version", appVersion);
        if (country != null && !country.isEmpty()) body.put("country", country);
        if (deviceInfo != null) body.put("device_info", deviceInfo);
        request("POST", "/rest/v1/app_events", body, true, "return=minimal");
    }

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

    private String request(String method, String path, Object body, boolean auth, boolean preferReturn) throws Exception {
        return request(method, path, body, auth, preferReturn ? "return=representation" : null);
    }

    private String request(String method, String path, Object body, boolean auth, String prefer) throws Exception {
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
