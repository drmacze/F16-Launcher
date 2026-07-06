package com.drmacze.f16launcher

import androidx.compose.ui.graphics.Color

/**
 * Game item data class — shared across GameHubScreen, GameDetailScreen, GameActionPanel, etc.
 *
 * v7.3.7: Extracted to separate file for clean compilation.
 * v7.8.0: Added serverStatus field for cloud gaming-style server status indicator.
 * v7.9.3: Enhanced with full metadata fields for console-style GameDetailScreen:
 *   - description, developer, version, size, category, ageRating
 *   - screenshots list (drawable resource IDs)
 *   - features list (for "Features" section)
 *   - apkUrl (for install/download)
 *   - lastUpdate (formatted date string)
 *   - language, engine
 * Now fully parameterized — GameDetailScreen bisa dipakai untuk FIFA 16 AND FIFA 15.
 */
data class GameItem(
    val title: String,
    val subtitle: String,
    val packageName: String,
    val mainActivity: String,
    val coverGradient: List<Color>,
    val coverText: String,
    val coverImageRes: Int? = null,
    val serverStatus: ServerStatus = ServerStatus.ONLINE,
    // v7.9.3: Console-style metadata
    val description: String = "",
    val developer: String = "DLavie Company",
    val version: String = "",
    val sizeMb: String = "",
    val category: String = "Olahraga",
    val ageRating: String = "9+",
    val language: String = "Indonesia, English",
    val engine: String = "EA Sports Engine",
    val lastUpdate: String = "",
    val features: List<String> = emptyList(),
    val screenshots: List<Int> = emptyList(),  // drawable resource IDs
    val apkUrl: String = "",
    val dataUrl: String? = null  // optional OBB/DATA download URL (FIFA 15)
)

/**
 * Server status untuk game card — cloud gaming style.
 * v7.9.13: BUSY dihapus dari manual config — sekarang auto-detect dari sinyal user.
 *
 * - ONLINE: Server normal, game bisa dimainkan (green dot)
 * - MAINTENANCE: Server maintenance, game tidak bisa dimainkan (red dot)
 * - OFFLINE: Server down (red dot)
 * - BUSY: (auto) Sinyal user lemah — bukan status server, tapi status koneksi user
 *         Auto-set saat user klik install/play jika sinyal lemah
 */
enum class ServerStatus(val label: String, val dotColor: Color, val textColor: Color, val bgColor: Color) {
    ONLINE("Server Online", Color(0xFF4CAF50), Color(0xFF4CAF50), Color(0x1A4CAF50)),
    MAINTENANCE("Maintenance", Color(0xFFFF5252), Color(0xFFFF5252), Color(0x1AFF5252)),
    OFFLINE("Server Offline", Color(0xFFFF5252), Color(0xFFFF5252), Color(0x1AFF5252)),
    BUSY("Sinyal Lemah", Color(0xFFFFC107), Color(0xFFFFC107), Color(0x1AFFC107))  // auto-detect, bukan dari Supabase
}

/**
 * v7.9.3: Ping signal quality — cloud gaming style latency indicator.
 * Real HTTP latency test ke game server (bukan dummy).
 *
 * - EXCELLENT: < 100ms (green) — ping bagus, lancar
 * - GOOD: 100-300ms (light green) — ping cukup, minor delay
 * - FAIR: 300-800ms (yellow) — ping sedang, mungkin lag
 * - POOR: > 800ms (orange) — ping buruk, lag
 * - UNKNOWN: gagal test (gray) — tidak bisa test ping
 */
enum class PingQuality(val label: String, val color: Color, val msThreshold: Long) {
    EXCELLENT("Sangat Baik", Color(0xFF4CAF50), 100),
    GOOD("Baik", Color(0xFF8BC34A), 300),
    FAIR("Sedang", Color(0xFFFFC107), 800),
    POOR("Buruk", Color(0xFFFF9800), Long.MAX_VALUE),
    UNKNOWN("Tidak Diketahui", Color(0xFF9E9E9E), 0);

    companion object {
        fun fromMs(ms: Long): PingQuality = when {
            ms < EXCELLENT.msThreshold -> EXCELLENT
            ms < GOOD.msThreshold -> GOOD
            ms < FAIR.msThreshold -> FAIR
            else -> POOR
        }

        /**
         * v7.9.13: Check apakah sinyal user lemah (untuk auto-set BUSY status).
         * Return true kalau sinyal lemah (POOR atau UNKNOWN) — user akan dapat
         * toast "Kekuatan sinyalmu lemah" saat klik install/play.
         */
        fun isWeakSignal(ms: Long?): Boolean {
            if (ms == null) return false  // gagal test tapi bukan berarti sinyal lemah
            return fromMs(ms) == POOR
        }
    }
}
