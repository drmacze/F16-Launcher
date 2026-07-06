package com.drmacze.f16launcher;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

// ════════════════════════════════════════════════════════════════════════════
// P4A — FeedApi
//
// Feed / likes / comments / saves / reports facade — extracted from the
// legacy CommunityApi. Owns:
//   - feedPosts, fetchFeedPostsGlobal, fetchFeedPostsFollowing
//   - createFeedPost (2 overloads), publishDraft, deleteFeedPost
//   - likePost, unlikePost, getPostLikeCount, hasLikedPost
//   - uploadImage (Supabase Storage)
//   - fetchComments, addComment, deleteComment, getCommentCount
//   - savePost, unsavePost, hasSavedPost
//   - reportPost (2 overloads)
//
// NOTE: The legacy `createFeedPost` and `addComment` methods fire-and-forget
// `logActivity` + `checkAndAwardBadges` (badges). This facade omits those
// cross-cutting calls — callers that need badge awarding should use the
// full CommunityApi facade or invoke GameApi explicitly.
// ════════════════════════════════════════════════════════════════════════════
public class FeedApi extends SupabaseClient {

    public FeedApi(Context ctx) { super(ctx); }

    public JSONArray feedPosts() throws Exception {
        return new JSONArray(request("GET", "/rest/v1/feed_posts?select=id,author_id,title,body,image_url,type,visibility,pinned,official,created_at&order=pinned.desc,created_at.desc&limit=10", null, true, (String) null));
    }

    public JSONArray fetchFeedPostsGlobal(String sortBy, int limit) throws Exception {
        String order = "created_at.desc";
        if ("oldest".equals(sortBy)) order = "created_at.asc";
        return new JSONArray(request("GET",
            "/rest/v1/feed_posts?order=" + order + "&limit=" + limit +
            "&select=id,author_id,title,body,image_url,type,pinned,official,created_at",
            null, false, false));
    }

    public JSONArray fetchFeedPostsFollowing(String sortBy, int limit) throws Exception {
        String order = "created_at.desc";
        if ("oldest".equals(sortBy)) order = "created_at.asc";
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
            "/rest/v1/feed_posts?author_id=in.(" + inList + ")&order=" + order + "&limit=" + limit +
            "&select=id,author_id,title,body,image_url,type,pinned,official,created_at",
            null, true, false));
    }

    public JSONObject createFeedPost(String title, String body, String imageUrl, String type) throws Exception {
        return createFeedPost(title, body, imageUrl, type, false);
    }

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
        return arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
    }

    public JSONObject publishDraft(String postId) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        JSONObject body = new JSONObject().put("is_draft", false);
        JSONArray arr = new JSONArray(request("PATCH",
            "/rest/v1/feed_posts?id=eq." + enc(postId) + "&author_id=eq." + enc(userId()),
            body, true, "return=representation"));
        return arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
    }

    public void deleteFeedPost(String postId) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        request("DELETE",
            "/rest/v1/feed_posts?id=eq." + enc(postId) + "&author_id=eq." + enc(userId()),
            null, true, false);
    }

    // ── Likes ──────────────────────────────────────────────────────────────────
    public void likePost(String postId) throws Exception {
        JSONObject body = new JSONObject().put("post_id", postId).put("user_id", userId());
        request("POST", "/rest/v1/feed_likes", body, true, "return=minimal");
    }

    public void unlikePost(String postId) throws Exception {
        request("DELETE", "/rest/v1/feed_likes?post_id=eq." + enc(postId) + "&user_id=eq." + enc(userId()),
            null, true, false);
    }

    public int getPostLikeCount(String postId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/feed_likes?post_id=eq." + enc(postId) + "&select=post_id",
            null, false, false));
        return arr.length();
    }

    public boolean hasLikedPost(String postId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/feed_likes?post_id=eq." + enc(postId) + "&user_id=eq." + enc(userId()) + "&select=post_id",
            null, true, false));
        return arr.length() > 0;
    }

    // ── Image upload (Supabase Storage) ────────────────────────────────────────
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
        return SUPABASE_URL + "/storage/v1/object/public/community-images/" + encodedPath;
    }

    // ── Comments ───────────────────────────────────────────────────────────────
    public JSONArray fetchComments(String postId) throws Exception {
        return new JSONArray(request("GET",
            "/rest/v1/feed_comments?post_id=eq." + enc(postId)
                + "&order=created_at.asc&limit=200"
                + "&select=id,post_id,user_id,body,created_at",
            null, false, false));
    }

    public JSONObject addComment(String postId, String body) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        if (body == null || body.trim().isEmpty()) throw new IllegalArgumentException("Komentar kosong.");
        JSONObject payload = new JSONObject()
            .put("post_id", postId)
            .put("user_id", userId())
            .put("body", body.trim());
        JSONArray arr = new JSONArray(request("POST", "/rest/v1/feed_comments",
            payload, true, "return=representation"));
        return arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject();
    }

    public void deleteComment(String commentId) throws Exception {
        if (!loggedIn()) throw new IllegalStateException("Belum login.");
        request("DELETE", "/rest/v1/feed_comments?id=eq." + enc(commentId)
            + "&user_id=eq." + enc(userId()), null, true, false);
    }

    public int getCommentCount(String postId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/feed_comments?post_id=eq." + enc(postId) + "&deleted=eq.false&select=id",
            null, false, false));
        return arr.length();
    }

    // ── Saved posts ─────────────────────────────────────────────────────────────
    public void savePost(String postId) throws Exception {
        JSONObject body = new JSONObject().put("post_id", postId).put("user_id", userId());
        request("POST", "/rest/v1/saved_posts?on_conflict=post_id,user_id", body, true,
            "resolution=merge-duplicates,return=minimal");
    }

    public void unsavePost(String postId) throws Exception {
        request("DELETE", "/rest/v1/saved_posts?post_id=eq." + enc(postId) + "&user_id=eq." + enc(userId()),
            null, true, false);
    }

    public boolean hasSavedPost(String postId) throws Exception {
        JSONArray arr = new JSONArray(request("GET",
            "/rest/v1/saved_posts?post_id=eq." + enc(postId) + "&user_id=eq." + enc(userId()) + "&select=post_id",
            null, true, false));
        return arr.length() > 0;
    }

    // ── Reports ─────────────────────────────────────────────────────────────────
    public void reportPost(String postId, String category, String reason) throws Exception {
        JSONObject body = new JSONObject()
            .put("reporter_id", userId())
            .put("target_type", "post")
            .put("target_id", postId)
            .put("category", category != null ? category : "other")
            .put("reason", reason != null ? reason.trim() : "");
        request("POST", "/rest/v1/reports", body, true, "return=minimal");
    }

    public void reportPost(String postId, String reason) throws Exception {
        JSONObject body = new JSONObject()
            .put("reporter_id", userId())
            .put("target_type", "post")
            .put("target_id", postId)
            .put("category", "community")
            .put("reason", reason != null ? reason.trim() : "");
        request("POST", "/rest/v1/reports", body, true, "return=minimal");
    }
}
