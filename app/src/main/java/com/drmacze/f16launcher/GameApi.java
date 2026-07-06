package com.drmacze.f16launcher;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

// ════════════════════════════════════════════════════════════════════════════
// P4A — GameApi
//
// Game session / badge / rating / activity-log facade — extracted from the
// legacy CommunityApi. Owns:
//   - recordGameSession, checkAndAwardBadges, logActivity
//   - fetchRatingStats, submitRating, getMyRating
//   - fetchUpdatePosts, fetchLatestUpdatePost
//   - getMyBadges, getAllBadges, getUniqueId, getTotalPlayTime, getMyBio
//
// Shares SupabaseClient infrastructure (request, prefs, session helpers).
// Use this directly in new code that ONLY needs game-side data — e.g. profile
// stats panel, badge grid, rating widget. The legacy CommunityApi facade
// continues to work for callers that need broader API surface.
// ════════════════════════════════════════════════════════════════════════════
public class GameApi extends SupabaseClient {

    private static final String TAG = "DLavieApi";

    public GameApi(Context ctx) { super(ctx); }

    // ── Ratings ─────────────────────────────────────────────────────────────────
    public JSONObject fetchRatingStats() throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/game_ratings?select=rating", null, false, false));
        if (arr.length() == 0) return new JSONObject().put("avg", 0.0).put("count", 0);
        double sum = 0;
        for (int i = 0; i < arr.length(); i++) sum += arr.getJSONObject(i).optInt("rating", 0);
        double avg = sum / arr.length();
        return new JSONObject().put("avg", avg).put("count", arr.length());
    }

    public void submitRating(int rating, String review) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating harus 1-5.");
        JSONObject body = new JSONObject()
            .put("user_id", userId())
            .put("rating", rating);
        if (review != null && !review.trim().isEmpty()) body.put("review", review.trim());
        request("POST", "/rest/v1/game_ratings?on_conflict=user_id", body, true,
            "resolution=merge-duplicates,return=minimal");
        try {
            logActivity("rate_game", new JSONObject().put("rating", rating));
            checkAndAwardBadges();
        } catch (Throwable ignored) { }
    }

    public int getMyRating() throws Exception {
        if (!loggedIn()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/game_ratings?user_id=eq." + enc(userId()) + "&select=rating",
            null, true, false));
        return arr.length() > 0 ? arr.getJSONObject(0).optInt("rating", 0) : 0;
    }

    // ── Update posts (announcements) ────────────────────────────────────────────
    public JSONArray fetchUpdatePosts() throws Exception {
        return new JSONArray(request("GET",
            "/rest/v1/update_posts?published=eq.true&order=version_code.desc&limit=10"
                + "&select=id,version_code,version_name,channel,title,body,release_notes,"
                + "known_issues,patch_url,patch_sha256,patch_size_bytes,critical,"
                + "restart_game_required,risk_level,created_at",
            null, false, false));
    }

    public JSONObject fetchLatestUpdatePost() throws Exception {
        JSONArray arr = fetchUpdatePosts();
        return arr.length() > 0 ? arr.getJSONObject(0) : null;
    }

    // ── Badges & stats ──────────────────────────────────────────────────────────
    public JSONArray getMyBadges() throws Exception {
        if (!loggedIn()) return new JSONArray();
        return new JSONArray(request("GET",
            "/rest/v1/user_badges?user_id=eq." + enc(userId()) + "&select=badge_code,earned_at",
            null, true, false));
    }

    public JSONArray getAllBadges() throws Exception {
        return new JSONArray(request("GET",
            "/rest/v1/badges?order=code.asc&select=code,name,description,icon,requirement_type,requirement_value",
            null, false, false));
    }

    public int getUniqueId() throws Exception {
        if (!loggedIn()) return 0;
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=unique_id",
            null, true, false));
        return arr.length() > 0 ? arr.getJSONObject(0).optInt("unique_id", 0) : 0;
    }

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

    public String getMyBio() throws Exception {
        if (!loggedIn()) return "";
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/profiles?id=eq." + enc(userId()) + "&select=bio",
            null, true, false));
        return arr.length() > 0 ? arr.getJSONObject(0).optString("bio", "") : "";
    }

    // ── Game session tracking ───────────────────────────────────────────────────
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
            Log.w(TAG, "recordGameSession FAILED: " + e.getMessage(), e);
            throw e;
        }
        try {
            JSONObject activity = new JSONObject()
                .put("user_id", userId())
                .put("activity_type", "play_game")
                .put("metadata", new JSONObject().put("duration_minutes", durationMinutes));
            request("POST", "/rest/v1/user_activities", activity, true, "return=minimal");
            logActivity("game_session", new JSONObject().put("duration_minutes", durationMinutes));
        } catch (Throwable ignored) { }
    }

    public Object checkAndAwardBadges() throws Exception {
        if (!loggedIn()) return new JSONArray();
        JSONObject body = new JSONObject().put("p_user_id", userId());
        try {
            String resp = request("POST", "/rest/v1/rpc/check_and_award_badges", body, true, null);
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
                Log.w(TAG, "checkAndAwardBadges: unexpected response shape — " + trimmed.substring(0, Math.min(120, trimmed.length())));
                return new JSONArray();
            }
        } catch (Throwable e) {
            Log.w(TAG, "checkAndAwardBadges FAILED: " + e.getMessage());
            return new JSONArray();
        }
    }

    public void logActivity(String type, JSONObject metadata) throws Exception {
        if (!loggedIn()) return;
        JSONObject body = new JSONObject()
            .put("user_id", userId())
            .put("activity_type", type);
        if (metadata != null) body.put("metadata", metadata);
        request("POST", "/rest/v1/user_activities", body, true, "return=minimal");
    }
}
