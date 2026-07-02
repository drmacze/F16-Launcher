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
    public String userId() { return prefs.getString("user_id", ""); }
    public String token() { return prefs.getString("access_token", ""); }
    public String username() { return prefs.getString("username", ""); }
    public String displayName() { return prefs.getString("display_name", ""); }
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
        JSONObject body = new JSONObject();
        body.put("id", userId());
        body.put("username", username.trim());
        body.put("display_name", displayName.trim());
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) body.put("avatar_url", avatarUrl.trim());
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
        prefs.edit().putString("username", p.optString("username", "")).putString("display_name", p.optString("display_name", "")).putString("avatar_url", p.optString("avatar_url", "")).putString("role", p.optString("role", "member")).apply();
    }

    public String role() { return prefs.getString("role", "member"); }
    public String avatarUrl() { return prefs.getString("avatar_url", ""); }

    public JSONArray feedPosts() throws Exception {
        return new JSONArray(request("GET", "/rest/v1/feed_posts?select=id,title,body,type,pinned,official,created_at&order=pinned.desc,created_at.desc&limit=8", null, false, (String) null));
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
        // First attempt
        try {
            return doRequest(method, path, body, auth, prefer);
        } catch (IllegalStateException e) {
            // Kalau JWT expired (401), coba refresh token lalu retry sekali
            if (auth && e.getMessage() != null && e.getMessage().contains("401")) {
                try {
                    refreshToken();
                } catch (Throwable refreshErr) {
                    // Refresh gagal — throw original error
                    throw e;
                }
                // Retry dengan token baru
                return doRequest(method, path, body, auth, prefer);
            }
            throw e;
        }
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
