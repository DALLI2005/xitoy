package com.commander.xitoy.presentation.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.commander.xitoy.domain.model.CartManager
import com.commander.xitoy.domain.model.FavoritesManager
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.presentation.common.rememberHaptic
import com.commander.xitoy.presentation.common.rememberStrongHaptic
import com.commander.xitoy.presentation.home.ProductCard
import com.commander.xitoy.ui.theme.DalliAccent
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliSurfaceAlt
import com.commander.xitoy.ui.theme.DalliText
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.ShoppingCart
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Truck
import kotlinx.coroutines.delay

private val StarGold = Color(0xFFF5A623)

private fun groupSom(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()

private fun formatSold(n: Int): String =
    if (n >= 1000) {
        val k = n / 1000.0
        if (k == k.toLong().toDouble()) "${k.toLong()}k" else String.format("%.1fk", k)
    } else n.toString()

@Composable
fun DetailsScreen(
    product: Product,
    allProducts: List<Product> = emptyList(),
    onBackClick: () -> Unit,
    onCartClick: () -> Unit = {},
    onProductClick: (Product) -> Unit = {}
) {
    val images = product.allImages
    var activeImg by remember(product.id) { mutableIntStateOf(0) }

    // Size support
    val availableSizes = product.razmerMatritsa[activeImg.toString()] ?: emptyList()
    var selectedSize by remember(product.id, activeImg) { mutableStateOf<com.commander.xitoy.domain.model.SizeOption?>(null) }

    // Variant support
    val activeBasePrice = when {
        selectedSize != null -> selectedSize!!.narx
        product.variantlarYoqilgan && activeImg < product.variantNarxlari.size -> product.variantNarxlari[activeImg]
        else -> product.price
    }
    val selectedVariantName: String? = if (product.variantlarYoqilgan) product.variantNomlari.getOrNull(activeImg) else null

    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var added by remember(product.id) { mutableStateOf(false) }

    val favorites = FavoritesManager.favorites.collectAsState().value
    val isFavorite = favorites.any { it.name == product.name }
    val cartCount = CartManager.cartItems.collectAsState().value.size

    val canAddToCart = availableSizes.isEmpty() || selectedSize != null
    val finalPrice = (activeBasePrice * (100 - product.discountPercent) / 100).toLong()
    val isHot = product.soldCount >= 100
    val totalPrice = finalPrice * quantity

    var pressed by remember { mutableStateOf(false) }
    val btnScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )
    val btnColor by animateColorAsState(
        targetValue = if (added) DalliSuccess else DalliPrimary,
        animationSpec = tween(250),
        label = "btn_color"
    )

    // "Qo'shildi" holatini 1.6 soniyadan keyin tiklash
    LaunchedEffect(added) {
        if (added) {
            delay(1600)
            added = false
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(150)
            pressed = false
        }
    }

    // O'xshash tovarlar: avval shu kategoriya, keyin boshqalar (max 6)
    val similar = remember(product.id, allProducts) {
        (allProducts.filter { it.category == product.category && it.id != product.id } +
            allProducts.filter { it.category != product.category }).take(6)
    }

    val displayRating = if (product.rating > 0f) product.rating else 3.5f + (product.id % 15) * 0.1f
    val ratingText = String.format("%.1f", displayRating)

    val haptic = rememberStrongHaptic()
    val favHaptic = rememberHaptic()

    Box(modifier = Modifier.fillMaxSize().background(DalliBackground)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ─── Hero ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DalliSurfaceAlt)
                ) {
                    Crossfade(
                        targetState = activeImg,
                        animationSpec = tween(280),
                        label = "img_crossfade"
                    ) { idx ->
                        val img = images.getOrNull(idx) ?: images.firstOrNull()
                        if (img != null) {
                            AsyncImage(
                                model = img,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Yuqori tugmalar qatori
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RoundBtn(icon = Lucide.ChevronLeft, onClick = onBackClick)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundBtn(
                            icon = Lucide.Heart,
                            onClick = { favHaptic(); FavoritesManager.toggle(product) },
                            tint = if (isFavorite) DalliAccent else DalliText
                        )
                        RoundBtn(
                            icon = Lucide.ShoppingCart,
                            onClick = onCartClick,
                            badge = cartCount
                        )
                    }
                }

                // HIT badge
                if (isHot) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 60.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(DalliAccent)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Lucide.Flame, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(
                            "HIT mahsulot",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // ─── Thumbnaillar ─────────────────────────────────────────────────
            if (images.size > 1 || product.variantlarYoqilgan) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    images.take(4).forEachIndexed { i, img ->
                        val selected = activeImg == i
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DalliSurfaceAlt)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) DalliPrimary else DalliLine,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { activeImg = i }
                            ) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                )
                            }
                            if (product.variantlarYoqilgan && i < product.variantNomlari.size && product.variantNomlari[i].isNotBlank()) {
                                Text(
                                    text = product.variantNomlari[i],
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) DalliPrimary else DalliMuted,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ─── Razmer tanlash ───────────────────────────────────────────────
            if (availableSizes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "O'lcham",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliText
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSizes.forEach { size ->
                            val isSelected = selectedSize?.nomi == size.nomi
                            val chipBg by animateColorAsState(
                                targetValue = if (isSelected) DalliPrimary else DalliSurface,
                                animationSpec = tween(180),
                                label = "chip_bg"
                            )
                            val chipText by animateColorAsState(
                                targetValue = if (isSelected) Color.White else DalliText,
                                animationSpec = tween(180),
                                label = "chip_text"
                            )
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(chipBg)
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = DalliLine,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedSize = if (isSelected) null else size }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = size.nomi,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipText
                                )
                                val sizeDiscountedPrice = (size.narx * (100 - product.discountPercent) / 100).toLong()
                                Text(
                                    text = "${groupSom(sizeDiscountedPrice)} so'm",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else DalliMuted
                                )
                            }
                        }
                    }
                }
            }

            // ─── Asosiy ma'lumot ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Kategoriya + nom + reyting
                Column {
                    if (product.category.isNotBlank()) {
                        Text(
                            text = product.category,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DalliPrimary
                        )
                        Spacer(Modifier.height(5.dp))
                    }
                    Text(
                        text = product.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DalliText,
                        lineHeight = 27.sp,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Lucide.Star, null, tint = StarGold, modifier = Modifier.size(15.dp))
                        Text(ratingText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliText)
                        if (product.soldCount > 0) {
                            Text(
                                "· ${formatSold(product.soldCount)} sotildi",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DalliMuted
                            )
                        }
                    }
                }

                // Narx bloki
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DalliSurface)
                        .border(1.dp, DalliLine, RoundedCornerShape(16.dp))
                        .padding(15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Narx · 1 dona",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliMuted
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${groupSom(finalPrice)} so'm",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DalliText,
                                letterSpacing = (-0.4).sp
                            )
                            if (product.discountPercent > 0) {
                                Text(
                                    "${groupSom(activeBasePrice.toLong())} so'm",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DalliMuted,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }
                    }
                    if (product.discountPercent > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(DalliPrimary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "-${product.discountPercent}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Info chiplar
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    InfoChip(icon = Lucide.Truck, value = "8-12 kun", label = "Yetkazish", tone = DalliAccent)
                    InfoChip(icon = Lucide.CircleCheck, value = "Mavjud", label = "Omborda", tone = DalliSuccess)
                    InfoChip(
                        icon = Lucide.Package,
                        value = product.category.ifBlank { "Original" },
                        label = "Kategoriya",
                        tone = DalliPrimary
                    )
                }

                // Tavsif
                val descToShow = product.description.trim()
                if (descToShow.isNotBlank() && descToShow != "-" && descToShow != product.name.trim()) {
                    Column {
                        Text("Tavsifi", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DalliText)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = descToShow,
                            fontSize = 13.5.sp,
                            color = DalliMuted,
                            lineHeight = 22.sp
                        )
                    }
                }

                // O'xshash tovarlar
                if (similar.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Text("O'xshash tovarlar", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DalliText)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            similar.forEach { sp ->
                                Box(modifier = Modifier.width(160.dp)) {
                                    ProductCard(
                                        product = sp,
                                        isFavorite = favorites.any { it.name == sp.name },
                                        onClick = { onProductClick(sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── Sticky pastki panel ──────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shadowElevation = 24.dp,
            color = DalliSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
            if (!canAddToCart) {
                Text(
                    text = "Iltimos, o'lchamni tanlang",
                    fontSize = 12.sp,
                    color = DalliMuted,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miqdor boshqaruvi
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DalliLine, RoundedCornerShape(12.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { if (quantity > 1) quantity-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Minus, null, tint = DalliMuted, modifier = Modifier.size(16.dp))
                    }
                    Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = quantity,
                            transitionSpec = {
                                (fadeIn()).togetherWith(fadeOut())
                            },
                            label = "qty"
                        ) { count ->
                            Text("$count", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DalliText)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { quantity++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.Plus, null, tint = DalliMuted, modifier = Modifier.size(16.dp))
                    }
                }

                // Savatga qo'shish tugmasi — bounce + AnimatedContent
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .scale(btnScale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canAddToCart) btnColor else DalliMuted.copy(alpha = 0.35f))
                        .clickable {
                            if (!added && canAddToCart) {
                                haptic()
                                val priceOverride = when {
                                    selectedSize != null -> selectedSize!!.narx
                                    product.variantlarYoqilgan -> activeBasePrice
                                    else -> null
                                }
                                repeat(quantity) {
                                    CartManager.addToCart(
                                        product,
                                        selectedVariantName,
                                        priceOverride,
                                        images.getOrNull(activeImg) ?: product.imageUrl,
                                        selectedSize?.nomi
                                    )
                                }
                                added = true
                                pressed = true
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = added,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.82f, animationSpec = tween(240)) +
                             fadeIn(animationSpec = tween(240)))
                                .togetherWith(
                                    scaleOut(targetScale = 0.82f, animationSpec = tween(180)) +
                                    fadeOut(animationSpec = tween(180))
                                )
                        },
                        label = "btn_content"
                    ) { isAdded ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAdded) {
                                Icon(Lucide.Check, null, tint = Color.White, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Qo'shildi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            } else {
                                Icon(Lucide.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Savatga · ${groupSom(totalPrice)} so'm",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            } // Column end
        }
    }
}

/** Hero ustidagi yumaloq suzuvchi tugma */
@Composable
private fun RoundBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = DalliText,
    badge: Int = 0
) {
    Box(modifier = Modifier.size(40.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(DalliAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (badge > 9) "9+" else "$badge",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/** Yetkazish / ombor / kategoriya kabi info chip */
@Composable
private fun RowScope.InfoChip(
    icon: ImageVector,
    value: String,
    label: String,
    tone: Color
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(13.dp))
            .background(DalliSurface)
            .border(1.dp, DalliLine, RoundedCornerShape(13.dp))
            .padding(vertical = 11.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = tone, modifier = Modifier.size(17.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DalliText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = DalliMuted)
    }
}
