package com.drmacze.f16launcher

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Telemetry — fire-and-forget event tracking via Supabase `app_events` table.
 *
 * Contract (per project spec):
 *  - Never blocks UI; runs on Dispatchers.IO inside a SupervisorJob.
 *  - Silent failure: errors are logged (Log.w) but never propagated.
 *  - Tracks events even when user is NOT logged in (user_id = null) — per RLS policy
 *    `app_events anon insert` + `app_events authenticated insert` (SQL v14).
 *  - Sends directly to Supabase REST `/rest/v1/app_events` with the correct schema:
 *      user_id     uuid nullable  (FK to profiles.id, set only when logged in)
 *      event_type  text  not null
 *      app_version text  nullable
 *      country     text  nullable
 *      device_info jsonb nullable
 *      metadata    jsonb not null default '{}'
 *
 * Auth handling (Task 1 fix):
 *  - Logged in  → use user JWT (Bearer <token>), include user_id + country.
 *  - Logged out → use anon key (Bearer <SUPABASE_KEY>), user_id omitted (null),
 *                  country omitted. RLS `app_events anon insert` allows this.
 *
 * Debugging (Task 1 fix):
 *  - All failures logged via Log.w(TAG, ...) with HTTP code + body excerpt (200 chars).
 *  - This makes "app_events KOSONG" diagnosable from logcat.
 *
 * Events fired across the launcher:
 *  - app_open         (MainShell)
 *  - login            (DLavieGuidedActivity)
 *  - register         (DLavieGuidedActivity)
 *  - patch_apply      (DevPatchEngine — richer status metadata)
 *  - patch_rollback   (DevPatchEngine rollback action)
 *  - game_launch      (launchGame in ModernLauncherActivity)
 *  - download_apk     (HomeScreen startDownload)
 *  - logout           (MainShell onLogout)
 */
object Telemetry {

    private const val TAG = "DLavieTelemetry"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Stable event type names — keep these in sync with backend dashboards.
    const val EVT_APP_OPEN        = "app_open"
    const val EVT_LOGIN           = "login"
    const val EVT_REGISTER        = "register"
    const val EVT_PATCH_APPLY     = "patch_apply"
    const val EVT_PATCH_ROLLBACK  = "patch_rollback"
    const val EVT_GAME_LAUNCH     = "game_launch"
    const val EVT_DOWNLOAD_APK    = "download_apk"
    const val EVT_LOGOUT          = "logout"

    // Supabase REST endpoint + anon key (sourced from CommunityApi to stay DRY).
    private val SUPABASE_URL = CommunityApi.SUPABASE_URL
    private val SUPABASE_KEY = CommunityApi.SUPABASE_KEY
    private const val APP_EVENTS_PATH = "/rest/v1/app_events"

    /**
     * Track an event. Fire-and-forget.
     * Safe to call from the main thread — it dispatches to Dispatchers.IO.
     *
     * RLS (SQL v14): anon INSERT allowed (user_id null), authenticated INSERT allowed.
     */
    fun track(context: Context, eventType: String, metadata: Map<String, Any?> = emptyMap()) {
        val api = CommunityApi(context.applicationContext)
        trackInternal(api, eventType, metadata)
    }

    /**
     * Convenience variant for callers that already hold a CommunityApi instance
     * (avoids re-instantiating SharedPreferences).
     */
    fun track(api: CommunityApi, context: Context, eventType: String, metadata: Map<String, Any?> = emptyMap()) {
        trackInternal(api, eventType, metadata)
    }

    // ─── Core dispatch ─────────────────────────────────────────────────────────

    private fun trackInternal(api: CommunityApi, eventType: String, metadata: Map<String, Any?>) {
        scope.launch {
            runCatching {
                pushToSupabase(api, eventType, metadata)
            }.onFailure { t ->
                // Task 1 fix: log warning supaya bisa debug dari logcat.
                // Filter: `adb logcat -s DLavieTelemetry` untuk lihat telemetry errors.
                Log.w(TAG, "track($eventType) failed: ${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }

    /**
     * POST a single row to `app_events` table via Supabase REST.
     *
     * Auth strategy (Task 1 fix):
     *  - Logged in  → Bearer <user JWT> (RLS: auth.uid() = user_id)
     *  - Logged out → Bearer <anon key> (RLS: app_events anon insert — user_id null)
     *
     * `Prefer: return=minimal` skips the response body — we don't need it.
     *
     * Response code is logged (but not thrown) for diagnostics:
     *   - 2xx → success
     *   - 4xx → RLS rejection or schema mismatch (log body excerpt)
     *   - 5xx → Supabase server error (transient — caller should not retry)
     */
    private fun pushToSupabase(api: CommunityApi, eventType: String, metadata: Map<String, Any?>) {
        val loggedIn = api.loggedIn()
        val userId   = if (loggedIn) api.userId() else null
        val country  = if (loggedIn) api.country() else null
        val userTok  = if (loggedIn) api.token() else ""

        val body = JSONObject().apply {
            // Logged-out events (app_open before login) → omit user_id (column is nullable).
            // RLS `app_events anon insert` allows null user_id.
            if (!userId.isNullOrEmpty()) put("user_id", userId)
            put("event_type", eventType)
            put("app_version", appVersion())
            if (!country.isNullOrEmpty()) put("country", country)
            put("device_info", deviceInfo())
            put("metadata", toJsonObject(metadata))
        }

        // Authorization: user JWT kalau login, anon key sebaliknya.
        val bearer = if (loggedIn && userTok.isNotEmpty()) userTok else SUPABASE_KEY

        val conn = (URL(SUPABASE_URL + APP_EVENTS_PATH).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout    = 15_000
            setRequestProperty("apikey", SUPABASE_KEY)
            setRequestProperty("Authorization", "Bearer $bearer")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
            doOutput = true
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            // Task 1 fix: log non-2xx codes with body excerpt so "app_events kosong"
            // becomes diagnosable. Body excerpt dibatasi 200 chars (limit log noise).
            if (code < 200 || code >= 300) {
                val errText = runCatching {
                    conn.errorStream?.bufferedReader()?.use { br ->
                        val sb = StringBuilder()
                        var line = br.readLine()
                        while (line != null && sb.length < 200) {
                            sb.append(line).append(' ')
                            line = br.readLine()
                        }
                        sb.toString().trim()
                    } ?: ""
                }.getOrDefault("")
                Log.w(TAG, "track($eventType) HTTP $code — $errText")
            } else {
                // Verbose success log (filtered out by default — only show via Logcat
                // filter `DLavieTelemetry:V`). Memudahkan konfirmasi event terkirim.
                Log.i(TAG, "track($eventType) OK (HTTP $code) loggedIn=$loggedIn")
            }
        } finally {
            conn.disconnect()
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * App version label (e.g. "3.0.0-major-upgrade").
     * Uses BuildConfig.VERSION_NAME generated by Gradle — falls back to "unknown"
     * if the field isn't resolvable at compile time.
     */
    private fun appVersion(): String {
        return try {
            @Suppress("DEPRECATION")
            BuildConfig.VERSION_NAME ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
    }

    private fun deviceInfo(): JSONObject {
        return JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER ?: "")
            put("model", Build.MODEL ?: "")
            put("brand", Build.BRAND ?: "")
            put("android_version", Build.VERSION.RELEASE ?: "")
            put("sdk_int", Build.VERSION.SDK_INT)
            put("abi", (Build.SUPPORTED_ABIS.firstOrNull() ?: ""))
        }
    }

    /**
     * Recursively convert a Map into a JSONObject.
     * Handles nested Maps and skips null values to keep JSON clean.
     */
    private fun toJsonObject(map: Map<String, Any?>): JSONObject {
        val obj = JSONObject()
        if (map.isEmpty()) return obj
        for ((k, v) in map) {
            when (v) {
                null              -> { /* skip nulls */ }
                is String         -> obj.put(k, v)
                is Number         -> obj.put(k, v)
                is Boolean        -> obj.put(k, v)
                is Map<*, *>      -> {
                    @Suppress("UNCHECKED_CAST")
                    obj.put(k, toJsonObject(v as Map<String, Any?>))
                }
                is JSONObject     -> obj.put(k, v)
                else              -> obj.put(k, v.toString())
            }
        }
        return obj
    }
}
