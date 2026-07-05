package com.drmacze.f16launcher

import androidx.compose.ui.graphics.Color

/**
 * Game item data class — shared across GameHubScreen, GameActionPanel, etc.
 * v7.3.7: Extracted to separate file for clean compilation.
 */
data class GameItem(
    val title: String,
    val subtitle: String,
    val packageName: String,
    val mainActivity: String,
    val coverGradient: List<Color>,
    val coverText: String,
    val coverImageRes: Int? = null
)
