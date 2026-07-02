package com.drmacze.f16launcher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Telemetry — fire-and-forget event tracking via Supabase `app_events` table.
 *
 * Contract (per project spec):
 *  - Never blocks UI; runs on Dispatchers.IO inside a SupervisorJob.
 *  - Silent failure: any error is swallowed.
 *  - Only logs when the user is logged in (CommunityApi.loggedIn()).
 *  - Captures user_id (server-side default), event_type, event_data jsonb,
 *    app_version, country, and a minimal device_info payload.
 *
 * Events fired across the launcher:
 *  - app_open         (MainShell)
 *  - login            (DLavieGuidedActivity)
 *  - register         (DLavieGuidedActivity)
 *  - patch_apply      (DevPatchEngine / UpdateScreen)
 *  - patch_rollback   (UpdateScreen rollback action)
 *  - game_launch      (launchGame in ModernLauncherActivity)
 *  - download_apk     (HomeScreen startDownload)
 *  - logout           (MainShell onLogout)
 */
object Telemetry {

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

    /**
     * Track an event. Fire-and-forget.
     * Safe to call from the main thread — it dispatches to Dispatchers.IO.
     */
    fun track(context: Context, eventType: String, eventData: Map<String, Any?> = emptyMap()) {
        scope.launch {
            runCatching {
                val api = CommunityApi(context.applicationContext)
                if (!api.loggedIn()) return@runCatching
                api.logEvent(
                    eventType,
                    toJsonObject(eventData),
                    appVersion(context),
                    api.country(),
                    deviceInfo()
                )
            }
        }
    }

    /**
     * Convenience variant for callers that already hold a CommunityApi instance
     * (avoids re-instantiating SharedPreferences).
     */
    fun track(api: CommunityApi, context: Context, eventType: String, eventData: Map<String, Any?> = emptyMap()) {
        scope.launch {
            runCatching {
                if (!api.loggedIn()) return@runCatching
                api.logEvent(
                    eventType,
                    toJsonObject(eventData),
                    appVersion(context),
                    api.country(),
                    deviceInfo()
                )
            }
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun appVersion(context: Context): String {
        return try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode.toLong()
            "${pkg.versionName} ($code)"
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
    }

    private fun deviceInfo(): JSONObject {
        return JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER ?: "")
            put("model", Build.MODEL ?: "")
            put("brand", Build.BRAND ?: "")
            put("sdk_int", Build.VERSION.SDK_INT)
            put("os_release", Build.VERSION.RELEASE ?: "")
            put("abi", (Build.SUPPORTED_ABIS.firstOrNull() ?: ""))
        }
    }

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
