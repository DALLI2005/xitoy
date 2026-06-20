package com.commander.xitoy.presentation.sales

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tag
import coil.compose.AsyncImage
import com.commander.xitoy.domain.model.FavoritesManager
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.ui.theme.DalliAccent
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val PageBg       = Color(0xFF000000)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val TextPrimary  = Color(0xFF09090B)
private val TextSecondary = Color(0xFF71717A)
private val GoldText     = Color(0xFFFFD700)

private val cardColors = listOf(
    Color(0xFF1A1A2E),
    Color(0xFF16213E),
    Color(0xFF0F3460),
)

private fun rankRating(rank: Int) = when (rank) { 1 -> 4.9f; 2 -> 4.7f; else -> 4.5f }

private fun originalPrice(price: Double, discountPercent: Int): Double =
    if (discountPercent > 0) price / (1.0 - discountPercent / 100.0) else price

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onProductClick: (Product) -> Unit,
    onBackClick: () -> Unit,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val products by viewModel.discountedProducts.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val sortMode   by viewModel.sortMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {
        SalesTopBar(count = products.size, onBackClick = onBackClick)
        SortTabs(sortMode = sortMode, onSortChange = viewModel::setSort)

        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isLoading && products.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DalliPrimary, strokeWidth = 2.5.dp)
                    }
                }
                products.isEmpty() -> EmptyDiscountState()
                else -> {
                    val top3 = products.take(3)
                    val rest = products.drop(3)
                    val pagerState = rememberPagerState { top3.size }

                    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {

                        // ── Top chegirmalar sarlavhasi ──
                        item {
                            Spacer(Modifier.height(28.dp))
                            Column(Modifier.padding(horizontal = 24.dp)) {
                                Text(
                                    "Top chegirmalar",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SurfaceWhite,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Eng katta chegirmali mahsulotlar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SurfaceWhite.copy(alpha = 0.45f)
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // ── HorizontalPager (Flutter PageView) ──
                        item {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                pageSpacing = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                            ) { page ->
                                val pageOffset = (
                                    (pagerState.currentPage - page) +
                                    pagerState.currentPageOffsetFraction
                                ).absoluteValue
                                val scale = lerp(0.93f, 1f, (1f - pageOffset).coerceIn(0f, 1f))
                                TopRankCard(
                                    product = top3[page],
                                    rank = page + 1,
                                    cardBg = cardColors[page % cardColors.size],
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = scale; scaleY = scale
                                    },
                                    onClick = { onProductClick(top3[page]) }
                                )
                            }
                        }

                        // ── Page indicator noqtalari ──
                        item {
                            Spacer(Modifier.height(20.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(top3.size) { i ->
                                    val active = i == pagerState.currentPage
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(
                                                width  = if (active) 24.dp else 8.dp,
                                                height = 8.dp
                                            )
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (active) GoldText
                                                else SurfaceWhite.copy(alpha = 0.25f)
                                            )
                                    )
                                }
                            }
                        }

                        // ── Qolgan chegirmalar ──
                        if (rest.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(32.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HorizontalDivider(
                                        Modifier.weight(1f),
                                        color = SurfaceWhite.copy(alpha = 0.12f)
                                    )
                                    Text(
                                        "Qolgan chegirmalar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    HorizontalDivider(
                                        Modifier.weight(1f),
                                        color = SurfaceWhite.copy(alpha = 0.12f)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            itemsIndexed(rest) { index, product ->
                                DiscountListItem(
                                    product = product,
                                    rank = index + 4,
                                    onClick = { onProductClick(product) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Flutter uslubidagi kartochka
// ─────────────────────────────────────────────
@Composable
private fun TopRankCard(
    product: Product,
    rank: Int,
    cardBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val favorites by FavoritesManager.favorites.collectAsState()
    val isFavorite = favorites.any { it.name == product.name }
    val rating = rankRating(rank)
    val origPrice = originalPrice(product.price, product.discountPercent)

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable { onClick() }
    ) {
        // Rasm
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Placeholder gradient (rasm yuklanishidan oldin va ustida)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            cardBg.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Pastki matn bloki
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Yulduz reyting
            StarRow(rating = rating)
            Spacer(Modifier.height(6.dp))

            // Mahsulot nomi
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(6.dp))

            // Narxlar
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatPrice(product.price),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldText
                    )
                )
                if (product.discountPercent > 0) {
                    Text(
                        text = formatPrice(origPrice),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SurfaceWhite.copy(alpha = 0.4f),
                            textDecoration = TextDecoration.LineThrough
                        ),
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }

        // Chegirma nishoni — yuqori o'ng
        if (product.discountPercent > 0) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "-${product.discountPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SurfaceWhite
                )
            }
        }

        // Sevimli tugma — chegirma nishoni ostida
        Box(
            modifier = Modifier
                .padding(top = 48.dp, end = 12.dp)
                .align(Alignment.TopEnd)
                .size(30.dp)
                .clip(CircleShape)
                .background(SurfaceWhite.copy(alpha = 0.88f))
                .clickable { FavoritesManager.toggle(product) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) DalliAccent else DalliMuted,
                modifier = Modifier.size(15.dp)
            )
        }

        // 3D RankBadge — kartochkadan chiqib turadi
        RankBadge(
            rank = rank,
            modifier = Modifier
                .size(90.dp)
                .offset(x = (-8).dp, y = (-4).dp)
        )
    }
}

// ─────────────────────────────────────────────
// Yulduz reyting qatori
// ─────────────────────────────────────────────
@Composable
private fun StarRow(rating: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { i ->
            val filled = i < rating.toInt()
            val half   = !filled && i < rating
            Icon(
                imageVector = when {
                    filled -> Icons.Default.Star
                    half   -> Icons.Default.StarHalf
                    else   -> Icons.Outlined.StarOutline
                },
                contentDescription = null,
                tint = GoldText,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = "%.1f".format(rating),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = SurfaceWhite.copy(alpha = 0.7f)
        )
    }
}

// ─────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────
@Composable
private fun SalesTopBar(count: Int, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Orqaga",
                tint = SurfaceWhite,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Chegirmali mahsulotlar",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = SurfaceWhite
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DalliPrimary.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "$count ta",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DalliPrimary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Sort tablar
// ─────────────────────────────────────────────
@Composable
private fun SortTabs(sortMode: SalesSort, onSortChange: (SalesSort) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortChip(
            label = "Chegirma %",
            selected = sortMode == SalesSort.BY_DISCOUNT,
            onClick = { onSortChange(SalesSort.BY_DISCOUNT) }
        )
        SortChip(
            label = "Arzon narx",
            selected = sortMode == SalesSort.BY_PRICE,
            onClick = { onSortChange(SalesSort.BY_PRICE) }
        )
    }
    HorizontalDivider(color = SurfaceWhite.copy(alpha = 0.08f))
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        if (selected) DalliPrimary else Color(0xFF2A2A2A),
        animationSpec = tween(200), label = "chip"
    )
    val textColor by animateColorAsState(
        if (selected) SurfaceWhite else TextSecondary,
        animationSpec = tween(200), label = "chip_text"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

// ─────────────────────────────────────────────
// Qolgan mahsulotlar ro'yxati (4+)
// ─────────────────────────────────────────────
@Composable
private fun DiscountListItem(product: Product, rank: Int, onClick: () -> Unit) {
    val favorites by FavoritesManager.favorites.collectAsState()
    val isFavorite = favorites.any { it.name == product.name }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3A3A3A),
                modifier = Modifier.width(26.dp)
            )
            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.size(64.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SurfaceWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = formatPrice(product.price),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldText
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "-${product.discountPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SurfaceWhite
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A2A))
                        .clickable { FavoritesManager.toggle(product) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) DalliAccent else DalliMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 122.dp, end = 20.dp),
            color = Color(0xFF1F1F1F)
        )
    }
}

// ─────────────────────────────────────────────
// Bo'sh holat
// ─────────────────────────────────────────────
@Composable
private fun EmptyDiscountState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Lucide.Tag,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Chegirmali mahsulotlar yo'q",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Yangi chegirmalar tez orada qo'shiladi",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

// ─────────────────────────────────────────────
// Yordamchi funksiyalar
// ─────────────────────────────────────────────
private fun formatPrice(price: Double): String {
    val p = price.toLong()
    return buildString {
        val s = p.toString()
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) append(' ')
            append(c)
        }
        append(" so'm")
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + fraction * (stop - start)
