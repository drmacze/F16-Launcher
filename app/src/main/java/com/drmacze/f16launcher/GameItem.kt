package com.drmacze.f16launcher

import androidx.compose.ui.graphics.Color

/**
 * Game item data class — shared across GameHubScreen, GameActionPanel, etc.
 * v7.3.7: Extracted to separate file for clean compilation.
 * v7.8.0: Added serverStatus field for cloud gaming-style server status indicator.
 */
data class GameItem(
    val title: String,
    val subtitle: String,
    val packageName: String,
    val mainActivity: String,
    val coverGradient: List<Color>,
    val coverText: String,
    val coverImageRes: Int? = null,
    val serverStatus: ServerStatus = ServerStatus.ONLINE
)

/**
 * Server status untuk game card — cloud gaming style.
 * - ONLINE: Server normal, game bisa dimainkan (green dot)
 * - BUSY: Server sibuk, mungkin ada delay (yellow dot)
 * - MAINTENANCE: Server maintenance, game tidak bisa dimainkan (red dot)
 * - OFFLINE: Server down (red dot)
 */
enum class ServerStatus(val label: String, val dotColor: Color, val textColor: Color, val bgColor: Color) {
    ONLINE("Server Online", Color(0xFF4CAF50), Color(0xFF4CAF50), Color(0x1A4CAF50)),
    BUSY("Server Sibuk", Color(0xFFFFC107), Color(0xFFFFC107), Color(0x1AFFC107)),
    MAINTENANCE("Maintenance", Color(0xFFFF5252), Color(0xFFFF5252), Color(0x1AFF5252)),
    OFFLINE("Server Offline", Color(0xFFFF5252), Color(0xFFFF5252), Color(0x1AFF5252))
}
