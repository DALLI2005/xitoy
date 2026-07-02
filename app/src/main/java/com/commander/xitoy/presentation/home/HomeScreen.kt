package com.commander.xitoy.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.commander.xitoy.R
import com.commander.xitoy.data.remote.OrderItem
import com.commander.xitoy.domain.model.CartManager
import com.commander.xitoy.domain.model.CurrencyRateManager
import com.commander.xitoy.domain.model.FavoritesManager
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.model.SeenOrdersManager
import com.commander.xitoy.domain.model.SessionManager
import com.commander.xitoy.presentation.common.ORDER_STAGES
import com.commander.xitoy.presentation.common.OrderProgressBar
import com.commander.xitoy.presentation.common.holatDisplay
import com.commander.xitoy.presentation.common.holatToStage
import com.commander.xitoy.presentation.common.rememberStrongHaptic
import com.commander.xitoy.presentation.orders.OrdersState
import com.commander.xitoy.presentation.orders.OrdersViewModel
import com.commander.xitoy.ui.theme.DalliAccent
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliError
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimaryDark
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliTeal
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Medal
import com.composables.icons.lucide.Sparkles

import com.composables.icons.lucide.Zap
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DeliveryGreen = Color(0xFF5EE29A)
private val HitOrange = Color(0xFFF97316)
private val StarGold = Color(0xFFF5A623)

private fun getDisplayRating(id: Int, rating: Float): Float =
    if (rating > 0f) rating else 3.5f + (id % 15) * 0.1f

private fun getMinOrderLabel(id: Int, soldCount: Int): String =
    if (soldCount > 0) "${(soldCount / 10) * 10}+ dona"
    else "${(id % 9 + 1) * 10}+ dona"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    ordersViewModel: OrdersViewModel = hiltViewModel(),
    onProductClick: (Product) -> Unit,
    onCartClick: () -> Unit,
    onFavoritesClick: () -> Unit = {},
    onSalesClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    pendingCategory: String? = null,
    onCategoryConsumed: () -> Unit = {}
) {
    val filteredProducts = viewModel.filteredProducts.collectAsState().value
    val totalCount = viewModel.allProductsCount.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val errorMessage = viewModel.errorMessage.collectAsState().value
    val favorites = FavoritesManager.favorites.collectAsState().value
    val searchQuery = viewModel.searchQuery.collectAsState().value
    val filterState by viewModel.filterState.collectAsState()
    val session by SessionManager.session.collectAsState()
    val ordersState by ordersViewModel.state.collectAsState()
    var quickAddProduct by remember { mutableStateOf<Product?>(null) }
    var addedProductName by remember { mutableStateOf<String?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    // Kirish animatsiyasi har bir karta uchun faqat bir marta o'ynaydi
    val shownProductIds = remember { mutableSetOf<Int>() }

    LaunchedEffect(addedProductName) {
        if (addedProductName != null) {
            delay(1500L)
            addedProductName = null
        }
    }

    LaunchedEffect(session?.telegramId) {
        val tid = session?.telegramId
        if (!tid.isNullOrBlank()) ordersViewModel.loadOrders(tid)
    }

    LaunchedEffect(pendingCategory) {
        if (!pendingCategory.isNullOrBlank()) {
            viewModel.onSearchQueryChange(pendingCategory)
            onCategoryConsumed()
        }
    }

    val latestOrder: OrderItem? = (ordersState as? OrdersState.Success)?.orders?.firstOrNull()
    val bannerStage = latestOrder?.let { holatToStage(it.holat) } ?: 0
    val isDelivered = latestOrder != null && holatToStage(latestOrder.holat) == 3
    val shouldShowBanner = latestOrder != null && !(
        isDelivered && SeenOrdersManager.isDeliveredOrderSeen(context, latestOrder.order_id)
    )

    // "Yetkazildi" banneri 3 soniya ko'rsatilgandan keyin avtomatik "ko'rildi" belgilanadi
    if (shouldShowBanner && isDelivered) {
        LaunchedEffect(latestOrder.order_id) {
            delay(3000L)
            SeenOrdersManager.markDeliveredOrderSeen(context, latestOrder.order_id)
        }
    }

    val headerScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        ShopHeader(
            scrolled = headerScrolled,
            favoritesCount = favorites.size,
            onTitleClick = { coroutineScope.launch { gridState.animateScrollToItem(0) } },
            onFavoritesClick = onFavoritesClick
        )
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().background(DalliBackground),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val ism = session?.fullname?.takeIf { it.isNotBlank() }
                    ?: session?.ism?.takeIf { it.isNotBlank() }
                    ?: ""
                val firstName = ism.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: ""
                val avatarLetter = firstName.firstOrNull()?.uppercase() ?: "D"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (firstName.isNotBlank()) "Salom, $firstName" else "Xush kelibsiz",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = DalliText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Lucide.MapPin,
                                contentDescription = null,
                                tint = DalliPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Rishton, Farg'ona",
                                fontSize = 13.sp,
                                color = DalliMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape, ambientColor = DalliPrimary.copy(alpha = 0.25f), spotColor = DalliPrimary.copy(alpha = 0.3f))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(DalliPrimary, Color(0xFF7C3AED))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarLetter,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            FlatSearchBar(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = CircleShape,
                                    ambientColor = DalliPrimary.copy(alpha = 0.10f),
                                    spotColor = DalliPrimary.copy(alpha = 0.12f)
                                )
                                .clip(CircleShape)
                                .background(
                                    if (filterState.isActive)
                                        Brush.linearGradient(listOf(DalliPrimary, Color(0xFF7C3AED)))
                                    else
                                        Brush.linearGradient(listOf(DalliSurface, DalliSurface))
                                )
                                .clickable { showFilterSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            BadgedBox(badge = {
                                if (filterState.activeCount > 0) {
                                    Badge(
                                        containerColor = Color.White,
                                        contentColor = DalliPrimary
                                    ) {
                                        Text(
                                            "${filterState.activeCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Filter",
                                    tint = if (filterState.isActive) Color.White else DalliText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    HeroBanner(
                        onClick = onSalesClick,
                        totalCount = totalCount,
                        // Parallax: scroll paytida banner kontentdan sal sekinroq siljiydi
                        modifier = Modifier.graphicsLayer {
                            val heroItem = gridState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == 1 }
                            val y = heroItem?.offset?.y ?: 0
                            translationY = if (y < 0) -y * 0.15f else 0f
                        }
                    )
                    if (shouldShowBanner) {
                        DeliveryBanner(
                            order = latestOrder!!,
                            stage = bannerStage,
                            onClick = {
                                if (isDelivered) {
                                    SeenOrdersManager.markDeliveredOrderSeen(context, latestOrder.order_id)
                                }
                                onOrdersClick()
                            }
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryChipRow(selected = searchQuery, onCategoryClick = { viewModel.onSearchQueryChange(it) })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(
                    title = "Tezkor takliflar",
                    action = "Hammasi",
                    onAction = onSalesClick,
                    leadingIcon = Lucide.Zap,
                    iconTint = HitOrange
                )
            }

            when {
                isLoading && filteredProducts.isEmpty() -> {
                    items(6) { SkeletonProductCard() }
                }
                errorMessage != null && filteredProducts.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(Icons.Default.Info, null, tint = DalliMuted, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Internet aloqasi yo'q",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(errorMessage, color = DalliMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                else -> {
                    itemsIndexed(filteredProducts, key = { _, p -> p.id }) { index, product ->
                        val isFav = favorites.any { it.id == product.id }
                        val playEntrance = remember(product.id) { shownProductIds.add(product.id) }
                        ProductCard(
                            product = product,
                            isFavorite = isFav,
                            entranceIndex = if (playEntrance) index else null,
                            onClick = { onProductClick(product) },
                            onQuickAdd = {
                                    if (product.allImages.size > 1 || product.variantlarYoqilgan) {
                                    quickAddProduct = product
                                } else {
                                    CartManager.addToCart(product, null, product.price, null)
                                    addedProductName = product.name
                                }
                            }
                        )
                    }
                }
            }

            // Pastki bo'lim — "Siz uchun lenta" (home.jsx dagidek to'liq enli qatorlar)
            val feed = filteredProducts.take(6)
            if (feed.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Lucide.Sparkles, null, tint = DalliPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                "Siz uchun lenta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DalliText
                            )
                        }
                    }
                }
                items(feed, span = { GridItemSpan(maxLineSpan) }, key = { "feed_${it.id}" }) { product ->
                    ProductRow(
                        product = product,
                        onClick = { onProductClick(product) },
                        onQuickAdd = {
                            if (product.allImages.size > 1 || product.variantlarYoqilgan) {
                                quickAddProduct = product
                            } else {
                                CartManager.addToCart(product, null, product.price, null)
                                addedProductName = product.name
                            }
                        }
                    )
                }
            }
        }   // LazyVerticalGrid
        }   // PullToRefreshBox
    }       // Column

    AnimatedVisibility(
        visible = addedProductName != null,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit  = slideOutVertically { it / 2 } + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 120.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(999.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(999.dp))
                .background(DalliText)
                .padding(horizontal = 18.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Lucide.CircleCheck, null, tint = DalliTeal, modifier = Modifier.size(16.dp))
            Text(
                "Savatga qo'shildi",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = filterState,
            availableCategories = listOf(
                "Aksessuar", "Kiyim", "Elektronika",
                "Boshqa", "Sport", "Poyabzal", "Uy uchun"
            ),
            minPrice = 0f,
            maxPrice = 1_000_000f,
            onApply = { newFilter ->
                viewModel.updateFilter(newFilter)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
    } // Box end

    quickAddProduct?.let { product ->
        QuickAddBottomSheet(
            product = product,
            onDismiss = { quickAddProduct = null },
            onAddToCart = { variantName, variantImg, price ->
                CartManager.addToCart(product, variantName, price, variantImg)
                quickAddProduct = null
            }
        )
    }
}

@Composable
private fun ShopHeader(
    scrolled: Boolean = false,
    favoritesCount: Int = 0,
    onTitleClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {}
) {
    // Scroll boshlanganda header ostida yumshoq soya paydo bo'ladi
    val headerElevation by animateDpAsState(
        targetValue = if (scrolled) 10.dp else 0.dp,
        animationSpec = tween(220),
        label = "header_elevation"
    )
    // Sevimlilarga yangi mahsulot qo'shilganda yurakcha "sakrab" qo'yadi
    val heartScale = remember { Animatable(1f) }
    var prevFavCount by remember { mutableStateOf(-1) }
    LaunchedEffect(favoritesCount) {
        if (prevFavCount in 0 until favoritesCount) {
            heartScale.snapTo(1f)
            heartScale.animateTo(1.4f, spring(stiffness = Spring.StiffnessHigh))
            heartScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
        prevFavCount = favoritesCount
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = headerElevation,
                ambientColor = DalliPrimary.copy(alpha = 0.12f),
                spotColor = DalliPrimary.copy(alpha = 0.15f)
            )
            .background(DalliSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTitleClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_dalli_new),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Dalli",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp,
                    color = DalliText
                )
                Text(
                    text = "Shop",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.4).sp,
                    color = DalliPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onFavoritesClick() },
                contentAlignment = Alignment.Center
            ) {
                BadgedBox(badge = {
                    if (favoritesCount > 0) {
                        Badge(containerColor = DalliError, contentColor = Color.White) {
                            Text(
                                text = if (favoritesCount > 99) "99+" else "$favoritesCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }) {
                    Icon(
                        imageVector = Lucide.Heart,
                        contentDescription = "Sevimlilar",
                        tint = DalliText,
                        modifier = Modifier
                            .size(20.dp)
                            .scale(heartScale.value)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CnyRatePill()
        }
        HorizontalDivider(color = DalliLine, thickness = 1.dp)
    }
}

@Composable
private fun CnyRatePill() {
    val rate = CurrencyRateManager.cnyRate.collectAsState().value
    LaunchedEffect(Unit) { CurrencyRateManager.load() }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(DalliPrimarySoft)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(Icons.Default.Public, null, tint = DalliPrimary, modifier = Modifier.size(14.dp))
        Text("¥1 =", color = DalliPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        // Kurs yangilanganda raqam g'ildirakdek pastdan yuqoriga aylanib almashadi
        AnimatedContent(
            targetState = rate?.somText ?: "…",
            transitionSpec = {
                (slideInVertically { it } + fadeIn(tween(220))) togetherWith
                    (slideOutVertically { -it } + fadeOut(tween(180)))
            },
            label = "rate_roll"
        ) { somText ->
            Text(somText, color = DalliPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
        }
        if (rate != null) {
            Text(
                "${if (rate.isUp) "▲" else "▼"}${rate.percentText}%",
                color = if (rate.isUp) DalliSuccess else DalliError,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlatSearchBar(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Mahsulot yoki brend qidirish…", color = DalliMuted, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = DalliPrimary, modifier = Modifier.size(19.dp)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(999.dp),
                ambientColor = DalliPrimary.copy(alpha = 0.10f),
                spotColor = DalliPrimary.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(999.dp),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DalliSurface,
            unfocusedContainerColor = DalliSurface,
            focusedIndicatorColor = DalliPrimary,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = DalliPrimary
        )
    )
}

@Composable
private fun HeroBanner(onClick: () -> Unit, totalCount: Int = 0, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_motion")

    val circle1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle1_scale"
    )
    val circle2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle2_offset"
    )
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = DalliPrimary.copy(alpha = 0.35f),
                spotColor = DalliPrimary.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF2D1B69), Color(0xFF4B1FDC), Color(0xFF7C3AED))
                )
            )
            .clickable { onClick() }
    ) {
        // Doira 1: sekin pulsatsiya (nafas olish effekti)
        Box(
            modifier = Modifier
                .size(130.dp)
                .offset(x = 230.dp, y = (-20).dp)
                .scale(circle1Scale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
        )
        // Doira 2: sekin vertikal suzish — logo strelkasidagi firuza tus
        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(x = 280.dp, y = (90f - circle2Offset).dp)
                .clip(CircleShape)
                .background(DalliTeal.copy(alpha = 0.20f))
        )
        // Shimmer qatlami: diagonal yorug'lik chizig'i
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 250f, 200f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Lucide.Flame, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(
                        "HAFTA HITI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.8.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Eng yaxshi\ntakliflar",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                lineHeight = 29.sp,
                letterSpacing = (-0.4).sp,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Ko'rish", color = DalliPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Text(
                    "${if (totalCount > 0) "${(totalCount / 10) * 10}+" else "120+"}  mahsulot · MOQ 10",
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
private fun DeliveryBanner(order: OrderItem, stage: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(DalliText)
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.LocalShipping, null, tint = DeliveryGreen, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${order.order_id} · ${holatDisplay(order.holat)}",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            OrderProgressBar(stage = stage, modifier = Modifier.fillMaxWidth())
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChipRow(selected: String, onCategoryClick: (String) -> Unit) {
    val categories = listOf("Hammasi", "Elektronika", "Kiyim", "Poyabzal", "Aksessuar", "Sport", "Uy uchun")
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isAll = cat == "Hammasi"
            val isSelected = if (isAll) selected.isBlank() else selected.equals(cat, ignoreCase = true)
            Box(
                modifier = Modifier
                    .then(
                        if (isSelected) Modifier.shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(999.dp),
                            ambientColor = DalliPrimary.copy(alpha = 0.3f),
                            spotColor = DalliPrimary.copy(alpha = 0.35f)
                        ) else Modifier
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected)
                            Brush.linearGradient(listOf(DalliPrimary, Color(0xFF7C3AED)))
                        else
                            Brush.linearGradient(listOf(DalliSurface, DalliSurface))
                    )
                    .then(
                        if (!isSelected) Modifier.border(1.dp, DalliLine, RoundedCornerShape(999.dp)) else Modifier
                    )
                    .clickable { onCategoryClick(if (isAll) "" else cat) }
            ) {
                Text(
                    text = cat,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else DalliTextSecondary
                )
            }
        }
    }
}

private fun formatSold(n: Int): String =
    if (n >= 1000) {
        val k = n / 1000.0
        if (k == k.toLong().toDouble()) "${k.toLong()}k" else String.format("%.1fk", k)
    } else n.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    rank: Int? = null,
    isFavorite: Boolean = false,
    entranceIndex: Int? = null,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit = {}
) {
    val rankBorderColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFB0BEC5)
        3 -> Color(0xFFCD7F32)
        else -> DalliLine
    }
    val isHot = product.soldCount >= 100

    val isTemporary = product.discountType == "vaqtinchalik" && !product.discountExpires.isNullOrBlank()
    val haptic = rememberStrongHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "card_press"
    )

    // Bir martalik kirish animatsiyasi: pastdan yumshoq ko'tarilib paydo bo'ladi
    val entranceAlpha = remember { Animatable(if (entranceIndex != null) 0f else 1f) }
    val entranceShift = remember { Animatable(if (entranceIndex != null) 26f else 0f) }
    LaunchedEffect(Unit) {
        if (entranceIndex != null) {
            delay((entranceIndex % 6) * 45L)
            launch { entranceAlpha.animateTo(1f, tween(320)) }
            launch {
                entranceShift.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
        }
    }

    // Yurakcha bosilganda "pop" effekti (faqat qo'shilganda)
    val favScale = remember { Animatable(1f) }
    var favInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(isFavorite) {
        if (favInitialized && isFavorite) {
            favScale.snapTo(0.5f)
            favScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
        favInitialized = true
    }

    // "+" bosilganda bir lahzaga ✓ belgiga aylanadi
    var showAddedCheck by remember { mutableStateOf(false) }
    LaunchedEffect(showAddedCheck) {
        if (showAddedCheck) {
            delay(900L)
            showAddedCheck = false
        }
    }

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = if (rank != null) BorderStroke(1.dp, rankBorderColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = DalliPrimary.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
    ) {
        Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Rasm + ustki badge'lar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .size(400, 400)
                        .crossfade(150)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Ustki chap: HIT (to'q sariq) + chegirma (qizil) — chegirma faqat shu yerda
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isHot) {
                        BadgePill(text = "HIT", bg = HitOrange, textColor = Color.White, icon = Lucide.Flame)
                    }
                    if (product.discountPercent > 0) {
                        BadgePill(text = "-${product.discountPercent}%", bg = DalliError, textColor = Color.White)
                    }
                }
                // Pastki o'ng: favorit tugma
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f))
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
                if (rank != null) {
                    // 1-o'rin oltin, 2-o'rin kumush, 3-o'rin bronza
                    val medalColor = when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> null
                    }
                    if (medalColor != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(7.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.95f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.Medal,
                                contentDescription = "$rank-o'rin",
                                tint = medalColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Ma'lumot
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val displayRating = getDisplayRating(product.id, product.rating)
                val ratingText = String.format("%.1f", displayRating)
                val minOrderLabel = getMinOrderLabel(product.id, product.soldCount)
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = DalliText,
                    lineHeight = 18.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(13.dp))
                    Text(ratingText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DalliText)
                    if (product.soldCount > 0) {
                        Text(
                            "· ${formatSold(product.soldCount)} sotildi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliMuted
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tan narx · $minOrderLabel",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        PriceText(product.price.toLong(), numberSize = 16.sp, somSize = 10.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = DalliPrimary.copy(alpha = 0.35f),
                                spotColor = DalliPrimary.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(DalliPrimary, Color(0xFF7C3AED)))
                            )
                            .clickable { haptic(); onQuickAdd() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Savatga qo'shish",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                if (isTemporary) {
                    CountdownText(
                        discountExpires = product.discountExpires!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonProductCard() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DalliSurface)
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(DalliLine.copy(alpha = shimmerAlpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(13.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(DalliLine.copy(alpha = shimmerAlpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(13.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(DalliLine.copy(alpha = shimmerAlpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(17.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DalliLine.copy(alpha = shimmerAlpha))
        )
    }
}

@Composable
private fun BadgePill(text: String, bg: Color, textColor: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(11.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            fontSize = 10.5.sp
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit,
    leadingIcon: ImageVector? = null,
    iconTint: Color = DalliText
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DalliText
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onAction() }
        ) {
            Text(action, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliPrimary)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = DalliPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun groupSom(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()

@Composable
private fun PriceText(value: Long, numberSize: TextUnit, somSize: TextUnit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            groupSom(value),
            fontSize = numberSize,
            fontWeight = FontWeight.ExtraBold,
            color = DalliText,
            letterSpacing = (-0.4).sp,
            lineHeight = numberSize,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "so'm",
            fontSize = somSize,
            fontWeight = FontWeight.SemiBold,
            color = DalliMuted
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductRow(product: Product, onClick: () -> Unit, onQuickAdd: () -> Unit = {}) {
    val haptic = rememberStrongHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "row_press"
    )
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = DalliPrimary.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUrl)
                    .size(200, 200)
                    .crossfade(150)
                    .build(),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val rowRating = String.format("%.1f", getDisplayRating(product.id, product.rating))
                Text(
                    product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DalliText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(13.dp))
                    Text(rowRating, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DalliText)
                    if (product.soldCount > 0) {
                        Text(
                            "· ${formatSold(product.soldCount)} sotildi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliMuted
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    PriceText(product.price.toLong(), numberSize = 17.sp, somSize = 11.sp)
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = DalliPrimary.copy(alpha = 0.35f),
                                spotColor = DalliPrimary.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(DalliPrimary, Color(0xFF7C3AED)))
                            )
                            .clickable { haptic(); onQuickAdd() }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Text("Savatga", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
