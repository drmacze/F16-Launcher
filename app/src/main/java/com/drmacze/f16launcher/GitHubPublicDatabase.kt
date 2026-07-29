package com.drmacze.f16launcher

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Read-only public data client backed by DLavie-Launcher-Data.
 *
 * Security rules:
 * - This client only performs anonymous GET requests.
 * - No GitHub token, Supabase key, or user credential is sent.
 * - File paths are selected from a fixed allow-list to prevent arbitrary URL access.
 * - User-generated/private data must never be stored in this public repository.
 *
 * GitHub is suitable here for version manifests, news, banners and public app config.
 * It is not a safe replacement for authenticated, user-writable database operations.
 */
object GitHubPublicDatabase {
    private const val TAG = "GitHubPublicDb"
    private const val OWNER = "drmacze"
    private const val REPOSITORY = "DLavie-Launcher-Data"
    private const val BRANCH = "main"
    private const val DEFAULT_CACHE_TTL_MS = 5 * 60 * 1000L

    enum class PublicFile(val path: String) {
        MANIFEST("manifest.json"),
        BANNER_SLIDES("banner_slides.json"),
        NEWS_POSTS("news_posts.json"),
        PUBLIC_DATABASE("public_database.json"),
        APP_CONFIG("app_config.json"),
        NOTIFICATION_CAMPAIGNS("notification_campaigns.json"),
        UPDATE_POSTS("update_posts.json"),
        OFFICIAL_FEED("official_feed.json")
    }

    private data class CacheEntry(val body: String, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun rawUrl(file: PublicFile): String =
        "https://raw.githubusercontent.com/$OWNER/$REPOSITORY/$BRANCH/${file.path}"

    fun clearMemoryCache() {
        cache.clear()
    }

    suspend fun fetchObject(
        file: PublicFile,
        maxAgeMillis: Long = DEFAULT_CACHE_TTL_MS
    ): JSONObject = JSONObject(fetchText(file, maxAgeMillis))

    suspend fun fetchArray(
        file: PublicFile,
        maxAgeMillis: Long = DEFAULT_CACHE_TTL_MS
    ): JSONArray = JSONArray(fetchText(file, maxAgeMillis))

    private suspend fun fetchText(file: PublicFile, maxAgeMillis: Long): String =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            cache[file.path]?.takeIf { maxAgeMillis > 0L && it.expiresAt > now }?.let {
                return@withContext it.body
            }

            val failures = mutableListOf<String>()
            val body = runCatching { fetchFromJsDelivr(file) }
                .onFailure { failures += "jsDelivr: ${it.message}" }
                .getOrNull()
                ?: runCatching { fetchFromGitHubApi(file) }
                    .onFailure { failures += "GitHub API: ${it.message}" }
                    .getOrNull()
                ?: runCatching { fetchRaw(file) }
                    .onFailure { failures += "GitHub raw: ${it.message}" }
                    .getOrNull()
                ?: throw IllegalStateException(
                    "Semua sumber data GitHub gagal untuk ${file.path}: ${failures.joinToString(" | ")}"
                )

            validateJsonRoot(file, body)
            if (maxAgeMillis > 0L) {
                cache[file.path] = CacheEntry(body, now + maxAgeMillis)
            }
            body
        }

    private fun fetchFromJsDelivr(file: PublicFile): String {
        // Ten-minute bucket keeps CDN useful while still allowing reasonably fresh data.
        val revisionBucket = System.currentTimeMillis() / (10 * 60 * 1000L)
        val url = "https://cdn.jsdelivr.net/gh/$OWNER/$REPOSITORY@$BRANCH/${file.path}?v=$revisionBucket"
        return getText(url)
    }

    private fun fetchRaw(file: PublicFile): String {
        val separator = if (rawUrl(file).contains('?')) '&' else '?'
        return getText("${rawUrl(file)}${separator}t=${System.currentTimeMillis()}")
    }

    private fun fetchFromGitHubApi(file: PublicFile): String {
        val url = "https://api.github.com/repos/$OWNER/$REPOSITORY/contents/${file.path}?ref=$BRANCH"
        val response = getText(
            url = url,
            headers = mapOf("Accept" to "application/vnd.github+json")
        )
        val payload = JSONObject(response)
        val encoding = payload.optString("encoding")
        require(encoding.equals("base64", ignoreCase = true)) {
            "Encoding GitHub tidak didukung: $encoding"
        }
        val encoded = payload.getString("content").replace("\n", "")
        return String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
    }

    private fun getText(url: String, headers: Map<String, String> = emptyMap()): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "DLavie-Launcher/${BuildConfig.VERSION_CODE}")
            setRequestProperty("Cache-Control", "no-cache")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            connect()
        }

        return try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("HTTP $status dari $url")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun validateJsonRoot(file: PublicFile, body: String) {
        val trimmed = body.trim()
        require(trimmed.isNotEmpty()) { "${file.path} kosong" }

        val expectsArray = file in setOf(
            PublicFile.BANNER_SLIDES,
            PublicFile.NEWS_POSTS,
            PublicFile.NOTIFICATION_CAMPAIGNS,
            PublicFile.UPDATE_POSTS,
            PublicFile.OFFICIAL_FEED
        )

        if (expectsArray) {
            require(trimmed.startsWith("[")) { "${file.path} harus berupa JSON array" }
            JSONArray(trimmed)
        } else {
            require(trimmed.startsWith("{")) { "${file.path} harus berupa JSON object" }
            JSONObject(trimmed)
        }

        Log.d(TAG, "Validated ${file.path} (${body.length} chars)")
    }
}
