package com.drmacze.f16launcher

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import org.json.JSONArray

// ─── Data Models ─────────────────────────────────────────────────────────────

data class BannerSlide(
    val id: String,
    val sortOrder: Int,
    val title: String?,
    val subtitle: String?,
    val mediaType: String,   // "image" | "gif" | "video"
    val mediaUrl: String,
    val linkUrl: String?,
    val durationSeconds: Int
)

data class NewsPost(
    val id: String,
    val title: String,
    val body: String,
    val footerText: String?,
    val imageUrl: String?,
    val labelType: String,   // "maintenance" | "information" | "other"
    val official: Boolean,
    val publishedAt: String
)

// ─── Parsers ─────────────────────────────────────────────────────────────────

fun parseBannerSlides(arr: JSONArray): List<BannerSlide> = try {
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        BannerSlide(
            id              = o.optString("id"),
            sortOrder       = o.optInt("sort_order", 0),
            title           = o.optString("title", "").ifEmpty { null },
            subtitle        = o.optString("subtitle", "").ifEmpty { null },
            mediaType       = o.optString("media_type", "image"),
            mediaUrl        = o.optString("media_url", ""),
            linkUrl         = o.optString("link_url", "").ifEmpty { null },
            durationSeconds = o.optInt("duration_seconds", 5).coerceIn(1, 60)
        )
    }.filter { it.mediaUrl.isNotBlank() }
} catch (_: Exception) { emptyList() }

fun parseNewsPosts(arr: JSONArray): List<NewsPost> = try {
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        NewsPost(
            id          = o.optString("id"),
            title       = o.optString("title"),
            body        = o.optString("body"),
            footerText  = o.optString("footer_text", "").ifEmpty { null },
            imageUrl    = o.optString("image_url", "").ifEmpty { null },
            labelType   = o.optString("label_type", "information"),
            official    = o.optBoolean("official", true),
            publishedAt = o.optString("published_at")
        )
    }
} catch (_: Exception) { emptyList() }

// ─── Banner Slider Composable ────────────────────────────────────────────────

/**
 * Auto-sliding banner slider (Pinterest-style 16:9 rounded).
 *
 * Features:
 * - HorizontalPager with smooth swipe
 * - Auto-advance setiap `durationSeconds` detik (per-slide duration)
 * - Support image, GIF, dan video (MP4) via Coil + ExoPlayer
 * - Dot indicators di bawah (selected = wide cyan, others = small gray)
 * - Optional overlay title + subtitle (gradient from-black-80)
 * - Click → open linkUrl in browser
 *
 * @param slides List of BannerSlide
 * @param onClick Called when banner is clicked (slide passed)
 */
@Composable
fun BannerSlider(
    slides: List<BannerSlide>,
    onClick: (BannerSlide) -> Unit = {}
) {
    if (slides.isEmpty()) return

    // v7.9.78: Pakai Crossfade (bukan HorizontalPager) — hanya 1 banner visible.
    // Tidak ada "setengah setengah" saat transisi. Fade transition yang clean.
    var currentIndex by remember { mutableStateOf(0) }

    // Auto-advance — duration per-slide
    LaunchedEffect(currentIndex, slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        val currentSlide = slides.getOrNull(currentIndex) ?: return@LaunchedEffect
        delay(currentSlide.durationSeconds * 1000L)
        currentIndex = (currentIndex + 1) % slides.size
    }

    val currentSlide = slides.getOrNull(currentIndex) ?: return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)  // Pinterest-style 16:9
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .clickable { onClick(currentSlide) }
        ) {
            // Crossfade transition — only 1 banner visible at a time (no split screen)
            androidx.compose.animation.Crossfade(
                targetState = currentIndex,
                animationSpec = tween(800, easing = FastOutSlowInEasing),
                label = "banner_crossfade"
            ) { index ->
                val slide = slides.getOrNull(index) ?: return@Crossfade
                BannerMediaContent(slide = slide)
            }

            // Overlay: title + subtitle (current slide)
            if (currentSlide.title != null || currentSlide.subtitle != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        currentSlide.title?.let { title ->
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        currentSlide.subtitle?.let { sub ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = sub,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // No overlay — clickable handled by parent Box
            }

            // Play icon indicator for video banners (top-right)
            if (currentSlide.mediaType == "video") {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(20.dp)
                )
            }
        }

        // Dot indicators (pill style — Pinterest-like)
        if (slides.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                slides.forEachIndexed { index, _ ->
                    val isSelected = currentIndex == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .then(
                                if (isSelected) Modifier.width(24.dp)
                                else Modifier.width(6.dp)
                            )
                            .clip(CircleShape)
                            .background(
                                if (isSelected) CandyCyan
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerMediaContent(slide: BannerSlide) {
    val context = LocalContext.current
    when (slide.mediaType) {
        "video" -> {
            // v7.9.78: Video with loading indicator + error fallback
            var videoLoading by remember { mutableStateOf(true) }
            var videoError by remember { mutableStateOf(false) }

            // Loading indicator while video buffers
            if (videoLoading && !videoError) {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.6f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Error fallback — show black bg with play icon
            if (videoError) {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // ExoPlayer for MP4 video
            val exoPlayer = remember(slide.mediaUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(slide.mediaUrl))
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    volume = 0f  // muted
                    prepare()
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            when (state) {
                                androidx.media3.common.Player.STATE_READY -> {
                                    videoLoading = false
                                    videoError = false
                                }
                                androidx.media3.common.Player.STATE_BUFFERING -> {
                                    videoLoading = true
                                }
                                androidx.media3.common.Player.STATE_ENDED -> {
                                    videoLoading = false
                                }
                                androidx.media3.common.Player.STATE_IDLE -> {
                                    videoError = true
                                    videoLoading = false
                                }
                            }
                        }
                    })
                }
            }
            DisposableEffect(slide.mediaUrl) {
                onDispose { exoPlayer.release() }
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            // Image + GIF via Coil with loading + error fallback
            var imageLoading by remember(slide.mediaUrl) { mutableStateOf(true) }
            var imageError by remember(slide.mediaUrl) { mutableStateOf(false) }

            if (imageLoading) {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.6f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (imageError) {
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(slide.mediaUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = remember {
                    coil.ImageLoader.Builder(context)
                        .components { add(coil.decode.GifDecoder.Factory()) }
                        .build()
                },
                contentDescription = slide.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onLoading = { imageLoading = true },
                onSuccess = { imageLoading = false; imageError = false },
                onError = { imageLoading = false; imageError = true }
            )
        }
    }
}

// ─── News List Composable ────────────────────────────────────────────────────

@Composable
fun NewsList(posts: List<NewsPost>) {
    if (posts.isEmpty()) return

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).background(CandyCyan.copy(0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = CandyCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Berita Resmi",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "${posts.size} post dari developer",
                    color = SoftText,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        posts.forEach { post ->
            NewsPostCard(post = post)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NewsPostCard(post: NewsPost) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2.copy(0.5f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Label + Official badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Label type badge
            val (labelColor, labelText) = when (post.labelType) {
                "maintenance"  -> Pair(AmberWarn, "Maintenance")
                "information"  -> Pair(CandyCyan, "Information")
                "other"        -> Pair(Color(0xFFB783FF), "Other")
                else           -> Pair(SoftText, post.labelType)
            }
            Box(
                Modifier
                    .background(labelColor.copy(0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    labelText,
                    color = labelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (post.official) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .background(NeonGreen.copy(0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "Official",
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Title
        Text(
            post.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Body
        if (post.body.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                post.body,
                color = SoftText,
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
        }

        // Image
        post.imageUrl?.let { url ->
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = post.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        // Footer
        post.footerText?.let { footer ->
            Spacer(Modifier.height(8.dp))
            Text(
                footer,
                color = SubText,
                fontSize = 10.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}
