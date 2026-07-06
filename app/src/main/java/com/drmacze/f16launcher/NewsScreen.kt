package com.drmacze.f16launcher

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * v7.9.3: NewsScreen — composite news feed untuk Beranda.
 *
 * Sumber news (semua real, no dummy):
 * 1. update_posts — patch announcements (rich: release notes, known issues, critical)
 * 2. notification_campaigns — announcements, maintenance, community
 * 3. feed_posts (official only) — pinned/official posts dari admin
 *
 * Digabung, di-sort by tanggal, ditampilkan sebagai:
 * - News hero carousel (top 3 news, swipeable)
 * - News feed list (all news, scrollable)
 */
@Composable
fun NewsScreen(api: CommunityApi) {
    val scope = rememberCoroutineScope()
    var news by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var sliderPosts by remember { mutableStateOf<List<NewsItem>>(emptyList()) }  // v7.9.7: pure image slider
    var loading by remember { mutableStateOf(true) }
    var selectedNews by remember { mutableStateOf<NewsItem?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        news = fetchAllNews(api)
        sliderPosts = fetchSliderPosts(api)  // v7.9.7: fetch pure image slider posts
        loading = false
    }

    // v7.9.6 FIX: NewsDetailScreen sebagai full-screen overlay Dialog (bukan embedded).
    selectedNews?.let { item ->
        Dialog(onDismissRequest = { selectedNews = null }) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Carbon
            ) {
                NewsDetailContent(
                    news = item,
                    onBack = { selectedNews = null }
                )
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        // ── v7.9.7: Pure Image Slider (promo banner) ──
        // Hanya gambar, ratio auto (16:9 fillMaxWidth), no text.
        // Source: feed_posts WHERE is_slider=true AND official=true
        if (sliderPosts.isNotEmpty()) {
            PromoSliderCarousel(
                items = sliderPosts,
                onClick = { selectedNews = it }
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── v7.9.7: News Hero Carousel (top 3 news) — fallback if no slider ──
        // Kalau slider kosong, tetap tampilkan hero carousel dari news posts
        if (sliderPosts.isEmpty() && news.isNotEmpty()) {
            val heroNews = news.take(3)
            NewsHeroCarousel(
                news = heroNews,
                onClick = { selectedNews = it }
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Section header ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(width = 3.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
            Spacer(Modifier.width(8.dp))
            Text("Berita Terbaru", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            Spacer(Modifier.weight(1f))
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White.copy(0.5f))
            }
        }

        // ── News Feed List ──
        if (loading && news.isEmpty()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { NewsCardSkeleton() }
            }
        } else if (news.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Article, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada berita", color = Color.White.copy(0.5f), fontSize = 14.sp, fontFamily = InterFontFamily)
                    Spacer(Modifier.height(4.dp))
                    Text("Cek lagi nanti untuk update terbaru", color = Color.White.copy(0.3f), fontSize = 12.sp, fontFamily = InterFontFamily)
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                news.forEach { item ->
                    NewsCard(
                        news = item,
                        onClick = { selectedNews = it }
                    )
                }
                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

// ─── v7.9.7: Promo Slider Carousel (pure image, auto ratio 16:9) ─────────────

@Composable
private fun PromoSliderCarousel(items: List<NewsItem>, onClick: (NewsItem) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    // Auto-scroll every 4 seconds
    LaunchedEffect(items.size) {
        if (items.size > 1) {
            while (true) {
                delay(4000)
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(Modifier.fillMaxWidth().height(180.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            PromoSliderSlide(item = item, onClick = { onClick(item) })
        }

        // Dots indicator
        if (items.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(items.size) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        Modifier.size(if (isActive) 20.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) Color.White else Color.White.copy(0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun PromoSliderSlide(item: NewsItem, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxSize().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        // Pure image, auto ratio (fillMaxSize = match parent 180dp height)
        if (item.imageUrl.isNotBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (item.sliderImages.isNotEmpty()) {
            // Fallback: use first slider image
            AsyncImage(
                model = item.sliderImages.first(),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // No image — gradient placeholder
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1)))
            ))
        }

        // Subtle gradient at bottom for any text overlay (optional)
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.3f)))
        ))
    }
}

// ─── News Hero Carousel ──────────────────────────────────────────────────────

@Composable
private fun NewsHeroCarousel(news: List<NewsItem>, onClick: (NewsItem) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { news.size })

    Box(Modifier.fillMaxWidth().height(200.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = news[page]
            NewsHeroSlide(item = item, onClick = { onClick(item) })
        }

        // Dots indicator
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(news.size) { index ->
                val isActive = pagerState.currentPage == index
                Box(
                    Modifier.size(if (isActive) 20.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isActive) Color.White else Color.White.copy(0.3f))
                )
            }
        }
    }
}

@Composable
private fun NewsHeroSlide(item: NewsItem, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxSize().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        // v7.9.6: Show image if available, otherwise gradient background
        if (item.imageUrl.isNotBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Background gradient (based on category)
            val bgGradient = when (item.category) {
                NewsCategory.UPDATE -> Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1)))
                NewsCategory.ANNOUNCEMENT -> Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF311B92)))
                NewsCategory.MAINTENANCE -> Brush.verticalGradient(listOf(Color(0xFFB71C1C), Color(0xFF7F0000)))
                NewsCategory.COMMUNITY -> Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF0D3814)))
            }
            Box(Modifier.fillMaxSize().background(bgGradient))
        }

        // Dark overlay for readability
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)))

        // Content
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: category badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(item.category.color.copy(0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        item.category.label,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
                if (item.critical) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF5252).copy(0.3f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("CRITICAL", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
            }

            // Bottom: title + subtitle
            Column {
                Text(
                    item.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = InterFontFamily
                )
                if (item.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.subtitle,
                        color = Color.White.copy(0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = InterFontFamily
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    item.timeAgo,
                    color = Color.White.copy(0.5f),
                    fontSize = 10.sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

// ─── News Card (list item) ───────────────────────────────────────────────────

@Composable
private fun NewsCard(news: NewsItem, onClick: (NewsItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(news) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBase),
        border = BorderStroke(1.dp, GlassStroke)
    ) {
        Column {
            // v7.9.6: Cover image (if available)
            if (news.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = news.imageUrl,
                    contentDescription = news.title,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(Modifier.padding(14.dp)) {
                // Top: category badge + timestamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(4.dp))
                            .background(news.category.color.copy(0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            news.category.label,
                            color = news.category.color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                    if (news.critical) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF5252).copy(0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("CRITICAL", color = Color(0xFFFF5252), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        news.timeAgo,
                        color = Color.White.copy(0.4f),
                        fontSize = 10.sp,
                        fontFamily = InterFontFamily
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Title
                Text(
                    news.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = InterFontFamily
                )
                // Body preview
                if (news.body.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        news.body,
                        color = Color.White.copy(0.6f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = InterFontFamily
                    )
                }
                // Release notes (if update_posts)
                if (news.releaseNotes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    news.releaseNotes.take(3).forEach { note ->
                        Row(Modifier.padding(vertical = 1.dp)) {
                            Text("•", color = Color.White.copy(0.5f), fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                note,
                                color = Color.White.copy(0.7f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }
                // v7.9.6: Slider images indicator
                if (news.sliderImages.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PhotoLibrary, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${news.sliderImages.size} gambar", color = Color.White.copy(0.4f), fontSize = 10.sp, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassBase)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(Modifier.fillMaxWidth(0.3f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.1f)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.1f)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(0.05f)))
        }
    }
}

// ─── News Detail Content (rendered inside Dialog overlay) ────────────────────
// v7.9.6: Renamed from NewsDetailScreen to NewsDetailContent.
// Rendered inside Dialog (full-screen Surface) supaya tidak crash di verticalScroll.

@Composable
private fun NewsDetailContent(news: NewsItem, onBack: () -> Unit) {
    var selectedSliderImage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Carbon)) {
        // Header with back button
        Surface(color = GlassBase) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp).clickable { onBack() })
                Spacer(Modifier.width(12.dp))
                Text("Detail Berita", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }
        }

        // Content
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // v7.9.6: Cover image (if available)
            if (news.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = news.imageUrl,
                    contentDescription = news.title,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Category + critical badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(news.category.color.copy(0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(news.category.label, color = news.category.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
                if (news.critical) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF5252).copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("CRITICAL", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
            }

            // Title
            Text(news.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = InterFontFamily)

            // Timestamp
            Text(news.timeAgo, color = Color.White.copy(0.5f), fontSize = 12.sp, fontFamily = InterFontFamily)

            // Version (if update post)
            if (news.version.isNotBlank()) {
                Text("Versi: ${news.version}", color = CandyCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
            }

            // Body
            if (news.body.isNotBlank()) {
                Text("Deskripsi", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                Text(news.body, color = Color.White.copy(0.7f), fontSize = 13.sp, lineHeight = 20.sp, fontFamily = InterFontFamily)
            }

            // v7.9.6: Slider images gallery (promo images)
            if (news.sliderImages.isNotEmpty()) {
                Text("Galeri", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(news.sliderImages) { img ->
                        AsyncImage(
                            model = img,
                            contentDescription = "Slider image",
                            modifier = Modifier.size(280.dp, 160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedSliderImage = img },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Release notes
            if (news.releaseNotes.isNotEmpty()) {
                Text("Release Notes", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                news.releaseNotes.forEach { note ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("•", color = NeonGreen, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(note, color = Color.White.copy(0.8f), fontSize = 13.sp, fontFamily = InterFontFamily)
                    }
                }
            }

            // Known issues
            if (news.knownIssues.isNotEmpty()) {
                Text("Known Issues", color = Color(0xFFFFC107), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                news.knownIssues.forEach { issue ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("•", color = Color(0xFFFFC107), fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(issue, color = Color.White.copy(0.8f), fontSize = 13.sp, fontFamily = InterFontFamily)
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }

    // Full-screen image viewer for slider
    selectedSliderImage?.let { url ->
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(0.95f))
                .clickable { selectedSliderImage = null },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Full image",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            Box(
                Modifier.align(Alignment.TopEnd).padding(24.dp).size(40.dp)
                    .clip(CircleShape).background(Color.White.copy(0.2f))
                    .clickable { selectedSliderImage = null },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ─── Data Classes ────────────────────────────────────────────────────────────

enum class NewsCategory(val label: String, val color: Color) {
    UPDATE("UPDATE", Color(0xFF2196F3)),
    ANNOUNCEMENT("ANNOUNCEMENT", Color(0xFF9C27B0)),
    MAINTENANCE("MAINTENANCE", Color(0xFFFF5252)),
    COMMUNITY("COMMUNITY", Color(0xFF4CAF50))
}

data class NewsItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val body: String = "",
    val category: NewsCategory,
    val critical: Boolean = false,
    val version: String = "",
    val releaseNotes: List<String> = emptyList(),
    val knownIssues: List<String> = emptyList(),
    val createdAt: Long = 0,
    val timeAgo: String = "",
    // v7.9.6: Image support — cover image + slider images (multiple for promo)
    val imageUrl: String = "",
    val sliderImages: List<String> = emptyList()
)

// ─── API: Fetch all news (composite) ──────────────────────────────────────────

private suspend fun fetchAllNews(api: CommunityApi): List<NewsItem> = withContext(Dispatchers.IO) {
    val result = mutableListOf<NewsItem>()
    val now = System.currentTimeMillis()

    // 1. Fetch update_posts
    runCatching {
        val arr = api.fetchUpdatePosts()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val createdAt = parseIsoDate(o.optString("created_at")) ?: now
            val releaseNotes = arrayListOf<String>()
            val rn = o.optJSONArray("release_notes")
            if (rn != null) for (j in 0 until rn.length()) releaseNotes.add(rn.getString(j))
            val knownIssues = arrayListOf<String>()
            val ki = o.optJSONArray("known_issues")
            if (ki != null) for (j in 0 until ki.length()) knownIssues.add(ki.getString(j))
            result.add(NewsItem(
                id = "upd_${o.optString("id")}",
                title = o.optString("title", "Update"),
                subtitle = o.optString("version_name", ""),
                body = o.optString("body", ""),
                category = NewsCategory.UPDATE,
                critical = o.optBoolean("critical", false),
                version = o.optString("version_name", ""),
                releaseNotes = releaseNotes,
                knownIssues = knownIssues,
                createdAt = createdAt,
                timeAgo = timeAgo(createdAt, now)
            ))
        }
    }.onFailure { Log.w("NewsScreen", "fetchUpdatePosts failed", it) }

    // 2. Fetch notification_campaigns
    runCatching {
        val arr = api.getNotifications(10)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val createdAt = parseIsoDate(o.optString("sent_at")) ?: parseIsoDate(o.optString("created_at")) ?: now
            val catStr = o.optString("category", "announcement").lowercase()
            val cat = when (catStr) {
                "update" -> NewsCategory.UPDATE
                "maintenance" -> NewsCategory.MAINTENANCE
                "community" -> NewsCategory.COMMUNITY
                else -> NewsCategory.ANNOUNCEMENT
            }
            result.add(NewsItem(
                id = "not_${o.optString("id")}",
                title = o.optString("title", "Pengumuman"),
                body = o.optString("body", ""),
                category = cat,
                createdAt = createdAt,
                timeAgo = timeAgo(createdAt, now)
            ))
        }
    }.onFailure { Log.w("NewsScreen", "getNotifications failed", it) }

    // 3. Fetch official feed posts (pinned/official only) — v7.9.6: include image_url + slider_images
    //    v7.9.7: filter is_slider=eq.false (slider posts TIDAK masuk news cards, hanya di slider carousel)
    runCatching {
        val resp = api.requestPublic("GET", "/rest/v1/feed_posts?official=eq.true&is_slider=eq.false&order=created_at.desc&limit=10&select=id,title,body,image_url,slider_images,created_at")
        val arr = JSONArray(resp)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val createdAt = parseIsoDate(o.optString("created_at")) ?: now
            // Parse slider_images (text[])
            val sliderImgs = arrayListOf<String>()
            val si = o.optJSONArray("slider_images")
            if (si != null) for (j in 0 until si.length()) sliderImgs.add(si.getString(j))
            result.add(NewsItem(
                id = "feed_${o.optString("id")}",
                title = o.optString("title", "Post"),
                body = o.optString("body", ""),
                category = NewsCategory.COMMUNITY,
                createdAt = createdAt,
                timeAgo = timeAgo(createdAt, now),
                imageUrl = o.optString("image_url", "").ifBlank { "" },
                sliderImages = sliderImgs
            ))
        }
    }.onFailure { Log.w("NewsScreen", "official feed failed", it) }

    // Sort by createdAt desc
    result.sortedByDescending { it.createdAt }
}

/**
 * v7.9.7: Fetch pure image slider posts (promo banners).
 * Source: feed_posts WHERE is_slider=true AND official=true
 * Hanya gambar, no text body — tampil di slider carousel Beranda.
 */
private suspend fun fetchSliderPosts(api: CommunityApi): List<NewsItem> = withContext(Dispatchers.IO) {
    val result = mutableListOf<NewsItem>()
    val now = System.currentTimeMillis()
    runCatching {
        val resp = api.requestPublic("GET", "/rest/v1/feed_posts?is_slider=eq.true&official=eq.true&order=created_at.desc&limit=10&select=id,title,image_url,slider_images,created_at")
        val arr = JSONArray(resp)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val createdAt = parseIsoDate(o.optString("created_at")) ?: now
            val sliderImgs = arrayListOf<String>()
            val si = o.optJSONArray("slider_images")
            if (si != null) for (j in 0 until si.length()) sliderImgs.add(si.getString(j))
            result.add(NewsItem(
                id = "slider_${o.optString("id")}",
                title = o.optString("title", "Slider"),
                category = NewsCategory.COMMUNITY,
                createdAt = createdAt,
                timeAgo = timeAgo(createdAt, now),
                imageUrl = o.optString("image_url", "").ifBlank { "" },
                sliderImages = sliderImgs
            ))
        }
    }.onFailure { Log.w("NewsScreen", "fetchSliderPosts failed", it) }
    // Sort by createdAt desc
    result.sortedByDescending { it.createdAt }
}

private fun parseIsoDate(iso: String): Long? {
    if (iso.isBlank()) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)?.time
    } catch (_: Exception) { null }
}

private fun timeAgo(createdAt: Long, now: Long): String {
    val diff = now - createdAt
    val sec = diff / 1000
    val min = sec / 60
    val hr = min / 60
    val day = hr / 24
    return when {
        sec < 60 -> "Baru saja"
        min < 60 -> "${min} menit lalu"
        hr < 24 -> "${hr} jam lalu"
        day < 7 -> "${day} hari lalu"
        else -> SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date(createdAt))
    }
}
