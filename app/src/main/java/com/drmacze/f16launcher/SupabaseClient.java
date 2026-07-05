package com.drmacze.f16launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.Locale;

// ════════════════════════════════════════════════════════════════════════════
// P4A — SupabaseClient base class
//
// Extracted from CommunityApi.java as the shared infrastructure layer:
//   - Supabase URL / anon key constants
//   - SharedPreferences-backed session (access_token, refresh_token, user_id,
//     username, display_name, avatar_url, role, country)
//   - HTTP `request()` method with JWT auto-refresh on 401 + network-error
//     rewriting
//   - URL-encoder helper `enc()`
//
// The four API facade classes inherit from this base:
//   - AuthApi      → register / login / refreshToken / profile
//   - FeedApi      → feed posts / likes / comments / saves / reports
//   - CommunityApi → follows / topics / search / notifications (legacy facade
//                    that also still holds ALL methods for source-compat)
//   - GameApi      → game sessions / badges / ratings / activity log
//
// CommunityApi.kt callers continue to use the legacy `CommunityApi` facade.
// New code SHOULD pick the narrowest facade (AuthApi / FeedApi / GameApi /
// CommunityApi) that fits the call site — see P4A in agent-ctx.
// ════════════════════════════════════════════════════════════════════════════
public class SupabaseClient {

    private static final String TAG = "DLavieApi";
    public static final String SUPABASE_URL = "https://lvmucsxbmadtsgrxuwmo.supabase.co";
    public static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx2bXVjc3hibWFkdHNncnh1d21vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI5ODUyODksImV4cCI6MjA5ODU2MTI4OX0.y-1sE6uYTn4Wbter6g6NozY6uojzD5x9YVeYif-5nJs";

    private static final String PREFS_NAME = "dlavie_community";

    /** Shared preferences — accessible to subclasses for session reads. */
    protected final SharedPreferences prefs;

    public SupabaseClient(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ─── Session helpers (shared by all API facades) ────────────────────────────
    public boolean loggedIn() {
        String token = prefs.getString("access_token", "");
        if (token.isEmpty() || prefs.getString("user_id", "").isEmpty()) return false;
        // Cek apakah token akan expired dalam 5 menit ke depan — kalau ya, coba
        // refresh asynchronously (non-blocking). Subclasses with auth scope
        // (AuthApi) override refreshToken() to do the real work.
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = parts[1];
                String padded = payload + "=".repeat((4 - payload.length() % 4) % 4);
                byte[] decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE);
                JSONObject jwt = new JSONObject(new String(decoded));
                long exp = jwt.optLong("exp", 0) * 1000L;
                long now = System.currentTimeMillis();
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
     * Untuk upgrade ke akun penuh, user logout (clears guest flag) lalu login/register.
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

    public String userId()     { return prefs.getString("user_id", ""); }
    public String token()      { return prefs.getString("access_token", ""); }
    public String username()   { return prefs.getString("username", ""); }
    public String displayName(){ return prefs.getString("display_name", ""); }
    public String country()    { return prefs.getString("country", "Indonesia"); }
    public void setCountry(String country) {
        if (country == null || country.trim().isEmpty()) return;
        prefs.edit().putString("country", country.trim()).apply();
    }
    public void logout()       { prefs.edit().clear().apply(); }  // v6.8.3: clear() also removes is_guest flag
    public String role()       { return prefs.getString("role", "member"); }
    public String avatarUrl()  { return prefs.getString("avatar_url", ""); }

    /** Save profile fields into prefs. Subclasses (AuthApi) call this after
     *  login / register / loadMyProfile to populate session metadata. */
    protected void saveProfile(JSONObject p) {
        String country = p.optString("country", "");
        SharedPreferences.Editor e = prefs.edit()
            .putString("username", p.optString("username", ""))
            .putString("display_name", p.optString("display_name", ""))
            .putString("avatar_url", p.optString("avatar_url", ""))
            .putString("role", p.optString("role", "member"));
        if (!country.isEmpty()) e.putString("country", country);
        e.apply();
    }

    /** Store session tokens (access + refresh + user_id) from an auth response. */
    protected void storeSessionIfPresent(JSONObject res) {
        String access = res.optString("access_token", "");
        String refresh = res.optString("refresh_token", "");
        JSONObject user = res.optJSONObject("user");
        if (!access.isEmpty() && user != null) {
            prefs.edit().putString("access_token", access).putString("refresh_token", refresh).putString("user_id", user.optString("id", "")).apply();
        }
    }

    /** Default no-op refresh — overridden by AuthApi. Subclasses that don't
     *  override will simply swallow the call (no refresh). */
    public JSONObject refreshToken() throws Exception {
        // Base impl: no-op (AuthApi overrides this with real Supabase call).
        return new JSONObject();
    }

    // ─── HTTP request infrastructure ────────────────────────────────────────────
    protected String request(String method, String path, Object body, boolean auth, boolean preferReturn) throws Exception {
        return request(method, path, body, auth, preferReturn ? "return=representation" : null);
    }

    protected String request(String method, String path, Object body, boolean auth, String prefer) throws Exception {
        // First attempt
        try {
            return doRequest(method, path, body, auth, prefer);
        } catch (IllegalStateException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            // 401 → refresh JWT → retry once. Other 4xx/5xx propagate.
            if (auth && msg.contains("401")) {
                Log.w(TAG, "request 401 on " + method + " " + path + " — refreshing JWT & retrying once");
                try {
                    refreshToken();
                } catch (Throwable refreshErr) {
                    Log.w(TAG, "refreshToken FAILED: " + refreshErr.getMessage());
                    throw new IllegalStateException("Sesi login kedaluwarsa. Silakan login ulang.", refreshErr);
                }
                return doRequest(method, path, body, auth, prefer);
            }
            if (msg.contains("Unable to resolve host") || msg.contains("Network is unreachable")
                || msg.contains("Connection timed out") || msg.contains("ECONNREFUSED")) {
                throw new IllegalStateException("Tidak dapat terhubung ke server. Periksa koneksi internet, lalu coba lagi.");
            }
            throw e;
        } catch (java.io.IOException io) {
            Log.w(TAG, "request IOException on " + method + " " + path + " — " + io.getMessage());
            throw new IllegalStateException("Gangguan jaringan: " + io.getMessage()
                + ". Periksa koneksi internet, lalu coba lagi.", io);
        }
    }

    /** Public request — uses anon key (or user token if logged in, for RLS staff). */
    public String requestPublic(String method, String path) throws Exception {
        boolean auth = loggedIn();
        return request(method, path, null, auth, false);
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

    /** URL-encode helper for query string values. */
    protected static String enc(String s) throws Exception { return URLEncoder.encode(s, "UTF-8"); }
}
