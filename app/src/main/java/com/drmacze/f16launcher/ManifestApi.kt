package com.drmacze.f16launcher

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * v7.9.38: ManifestApi — fetch mod manifest dari GitHub raw URL.
 *
 * Manifest adalah single source of truth untuk mod versions. Launcher fetch
 * manifest ini tiap buka DLC tab untuk cek mod terbaru. User tidak perlu
 * query SQL manual atau tunggu developer insert ke Supabase — cukup update
 * manifest.json di repo, Launcher otomatis dapat versi baru.
 *
 * URL: https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json
 *
 * Schema (v2):
 * {
 *   "schema_version": 2,
 *   "updated_at": "2026-07-07T10:30:00Z",
 *   "mods": [
 *     {
 *       "id": "save-menu-enhancer",
 *       "name": "Save Menu Enhancer",
 *       "latest_version": "1.0.0",
 *       "published": true,
 *       "critical": false,
 *       "versions": [
 *         {
 *           "version": "1.0.0",
 *           "url": "https://github.com/...",
 *           "sha256": "...",
 *           "size": 20911
 *         }
 *       ]
 *     }
 *   ]
 * }
 *
 * Caching:
 * - Launcher cache manifest di SharedPreferences (15 menit TTL)
 * - Force refresh saat user pull-to-refresh di DLC tab
 * - Fallback ke Supabase update_posts kalau manifest fetch gagal
 */
object ManifestApi {

    private const val TAG = "ManifestApi"
    private const val MANIFEST_URL = "https://raw.githubusercontent.com/drmacze/DLavie-Launcher-Data/main/manifest.json"
    private const val PREFS_NAME = "dlavie_manifest_prefs"
    private const val KEY_CACHED_MANIFEST = "cached_manifest"
    private const val KEY_CACHED_AT = "cached_at"
    private const val CACHE_TTL_MS = 15 * 60 * 1000L  // 15 minutes

    /**
     * Fetch manifest dari GitHub raw URL. Pakai cache kalau masih fresh.
     *
     * @param context Application context untuk akses SharedPreferences
     * @param forceRefresh true untuk skip cache dan fetch fresh dari server
     * @return Manifest object atau null jika fetch gagal
     */
    suspend fun fetchManifest(context: Context, forceRefresh: Boolean = false): Manifest? =
        withContext(Dispatchers.IO) {
            // Cek cache dulu kalau tidak force refresh
            if (!forceRefresh) {
                val cached = getCachedManifest(context)
                if (cached != null) {
                    Log.d(TAG, "Using cached manifest (age=${System.currentTimeMillis() - getCachedAt(context)}ms)")
                    return@withContext cached
                }
            }

            // Fetch fresh dari server
            Log.i(TAG, "Fetching manifest from: $MANIFEST_URL")
            try {
                val conn = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("User-Agent", "DLavie-Launcher")
                }
                try {
                    val code = conn.responseCode
                    if (code !in 200..299) {
                        Log.w(TAG, "Manifest fetch failed: HTTP $code")
                        return@withContext getCachedManifest(context)  // fallback ke cache lama
                    }
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val manifest = parseManifest(json)
                    // Cache hasil
                    cacheManifest(context, text, System.currentTimeMillis())
                    Log.i(TAG, "Manifest fetched: ${manifest.mods.size} mods, updated_at=${manifest.updatedAt}")
                    manifest
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Manifest fetch error: ${e.message}")
                getCachedManifest(context)  // fallback ke cache
            }
        }

    /**
     * Get mod list dari manifest. Filter hanya yang published=true dan
     * punya versions[] tidak kosong.
     */
    suspend fun fetchMods(context: Context, forceRefresh: Boolean = false): List<ModEntry> =
        withContext(Dispatchers.IO) {
            val manifest = fetchManifest(context, forceRefresh) ?: return@withContext emptyList()
            manifest.mods.filter { it.published && it.versions.isNotEmpty() }
        }

    /**
     * Cek apakah ada update tersedia untuk mod yang sudah terinstall.
     *
     * @param installedVersions Map<modId, installedVersionString>
     * @return List of mods yang punya update
     */
    suspend fun checkUpdates(
        context: Context,
        installedVersions: Map<String, String>
    ): List<ModEntry> = withContext(Dispatchers.IO) {
        val mods = fetchMods(context)
        mods.filter { mod ->
            val installed = installedVersions[mod.id]
            installed != null && installed != mod.latestVersion
        }
    }

    // ─── Cache helpers ───────────────────────────────────────────────────────

    private fun getCachedManifest(context: Context): Manifest? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedAt = prefs.getLong(KEY_CACHED_AT, 0)
        if (cachedAt == 0L) return null
        val age = System.currentTimeMillis() - cachedAt
        if (age > CACHE_TTL_MS) return null  // expired
        val text = prefs.getString(KEY_CACHED_MANIFEST, null) ?: return null
        return try {
            parseManifest(JSONObject(text))
        } catch (e: Exception) {
            Log.w(TAG, "Cached manifest parse error: ${e.message}")
            null
        }
    }

    private fun getCachedAt(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_CACHED_AT, 0)
    }

    private fun cacheManifest(context: Context, text: String, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CACHED_MANIFEST, text)
            .putLong(KEY_CACHED_AT, timestamp)
            .apply()
    }

    // ─── Parser ──────────────────────────────────────────────────────────────

    private fun parseManifest(json: JSONObject): Manifest {
        val modsArr = json.optJSONArray("mods") ?: JSONArray()
        val mods = mutableListOf<ModEntry>()
        for (i in 0 until modsArr.length()) {
            val modJson = modsArr.getJSONObject(i)
            val versionsArr = modJson.optJSONArray("versions") ?: JSONArray()
            val versions = mutableListOf<ModVersion>()
            for (j in 0 until versionsArr.length()) {
                val vJson = versionsArr.getJSONObject(j)
                versions.add(
                    ModVersion(
                        version = vJson.optString("version"),
                        versionCode = vJson.optInt("version_code", 0),
                        releasedAt = vJson.optString("released_at"),
                        url = vJson.optString("url"),
                        sha256 = vJson.optString("sha256"),
                        size = vJson.optLong("size", 0),
                        channel = vJson.optString("channel", "stable"),
                        critical = vJson.optBoolean("critical", false)
                    )
                )
            }
            // latest version = first in versions[] (paling baru di depan)
            val latestVersion = versions.firstOrNull()
            mods.add(
                ModEntry(
                    id = modJson.optString("id"),
                    slug = modJson.optString("slug", modJson.optString("id")),
                    name = modJson.optString("name"),
                    title = modJson.optString("title"),
                    description = modJson.optString("description"),
                    category = modJson.optString("category", "gameplay_enhancement"),
                    author = modJson.optString("author", "DLavie Team"),
                    icon = modJson.optString("icon", "extension"),
                    latestVersion = modJson.optString("latest_version"),
                    versionCode = modJson.optInt("version_code", 0),
                    channel = modJson.optString("channel", "stable"),
                    published = modJson.optBoolean("published", false),
                    critical = modJson.optBoolean("critical", false),
                    restartGameRequired = modJson.optBoolean("restart_game_required", false),
                    riskLevel = modJson.optString("risk_level", "low"),
                    minLauncherVersionCode = modJson.optInt("min_launcher_version_code", 0),
                    targetGamePackage = modJson.optString("target_game_package", "com.ea.gp.fifaworld"),
                    releasedAt = modJson.optString("released_at"),
                    body = modJson.optString("body"),
                    releaseNotes = jsonArrayToStringList(modJson.optJSONArray("release_notes")),
                    knownIssues = jsonArrayToStringList(modJson.optJSONArray("known_issues")),
                    versions = versions,
                    latestVersionEntry = latestVersion
                )
            )
        }
        return Manifest(
            schemaVersion = json.optInt("schema_version", 1),
            updatedAt = json.optString("updated_at"),
            mods = mods
        )
    }

    private fun jsonArrayToStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }

    // ─── Data classes ────────────────────────────────────────────────────────

    data class Manifest(
        val schemaVersion: Int,
        val updatedAt: String,
        val mods: List<ModEntry>
    )

    data class ModEntry(
        val id: String,
        val slug: String,
        val name: String,
        val title: String,
        val description: String,
        val category: String,
        val author: String,
        val icon: String,
        val latestVersion: String,
        val versionCode: Int,
        val channel: String,
        val published: Boolean,
        val critical: Boolean,
        val restartGameRequired: Boolean,
        val riskLevel: String,
        val minLauncherVersionCode: Int,
        val targetGamePackage: String,
        val releasedAt: String,
        val body: String,
        val releaseNotes: List<String>,
        val knownIssues: List<String>,
        val versions: List<ModVersion>,
        val latestVersionEntry: ModVersion?
    )

    data class ModVersion(
        val version: String,
        val versionCode: Int,
        val releasedAt: String,
        val url: String,
        val sha256: String,
        val size: Long,
        val channel: String,
        val critical: Boolean
    )
}
