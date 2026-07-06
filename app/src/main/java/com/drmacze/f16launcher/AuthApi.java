package com.drmacze.f16launcher;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

// ════════════════════════════════════════════════════════════════════════════
// P4A — AuthApi
//
// Authentication & profile facade — extracted from the legacy CommunityApi.
// Owns: register, login (2 overloads), refreshToken, loadMyProfile,
//       ensureMyProfile, validateProfile.
//
// Shares SupabaseClient infrastructure (request, prefs, session helpers).
// Use this directly in new code that ONLY needs auth — e.g. login screens,
// session refreshers. The legacy CommunityApi facade still works for callers
// that need broader API surface.
// ════════════════════════════════════════════════════════════════════════════
public class AuthApi extends SupabaseClient {

    public AuthApi(Context ctx) { super(ctx); }

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

    protected void validateProfile(String username, String displayName) {
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
        body.put("display_name", displayName.trim());
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) body.put("avatar_url", avatarUrl.trim());
        try {
            JSONArray existing = new JSONArray(request("GET", "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=username", null, true, false));
            if (existing.length() > 0) {
                String currentUsername = existing.getJSONObject(0).optString("username", "");
                if (!currentUsername.isEmpty() && !currentUsername.equals(username.trim())) {
                    try { body.put("username", username.trim()); } catch (Throwable ignored) { }
                }
            } else {
                body.put("username", username.trim());
            }
        } catch (Throwable ignored) {
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

    public JSONObject loadMyProfile() throws Exception {
        String id = userId();
        if (id.isEmpty()) throw new IllegalStateException("Belum login.");
        JSONArray arr = new JSONArray(request("GET", "/rest/v1/profiles?id=eq." + enc(id) + "&select=*", null, true, false));
        if (arr.length() == 0) throw new IllegalStateException("Profile community belum tersedia.");
        JSONObject p = arr.getJSONObject(0);
        saveProfile(p);
        return p;
    }

    /** Refresh the JWT using the stored refresh token. Overrides the no-op
     *  in SupabaseClient so that the base `request()` 401-retry path actually
     *  refreshes the session. */
    @Override
    public JSONObject refreshToken() throws Exception {
        String refresh = prefs.getString("refresh_token", "");
        if (refresh.isEmpty()) throw new IllegalStateException("No refresh token.");
        JSONObject body = new JSONObject();
        body.put("refresh_token", refresh);
        JSONObject res = new JSONObject(request("POST", "/auth/v1/token?grant_type=refresh_token", body, false, (String) null));
        storeSessionIfPresent(res);
        return res;
    }
}
