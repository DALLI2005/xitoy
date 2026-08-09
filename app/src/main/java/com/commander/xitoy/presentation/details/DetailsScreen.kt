package com.commander.xitoy.presentation.details

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.commander.xitoy.ui.theme.DalliLine2
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliSurfaceAlt
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.ShoppingCart
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Truck
import com.composables.icons.lucide.X
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
    var fullscreenOpen by remember(product.id) { mutableStateOf(false) }

    // Size support
    val availableSizes = product.razmerMatritsa[activeImg.toString()] ?: emptyList()
    var selectedSize by remember(product.id, activeImg) { mutableStateOf<com.commander.xitoy.domain.model.SizeOption?>(null) }

    // Rang xususiyati
    val rangAttrKey = product.attributes.keys.firstOrNull { it.equals("Rang", ignoreCase = true) }
    val rangValues = rangAttrKey?.let { product.attributes[it] } ?: emptyList()

    // Narx/variant sinxronizatsiyasi — "Variantlar (Rang va Narx)" bo'limi, alohida
    // tizim, rang nomi bilan variant nomi matn darajasida moslashtiriladi.
    val rangValueToImageIndex: Map<String, Int> = if (product.variantlarYoqilgan) {
        rangValues.mapNotNull { value ->
            val idx = product.variantNomlari.indexOfFirst { it.equals(value, ignoreCase = true) }
            if (idx >= 0) value to idx else null
        }.toMap()
    } else emptyMap()
    var selectedRangValue by remember(product.id) { mutableStateOf(rangValues.firstOrNull()) }

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
    val isFavorite = favorites.any { it.id == product.id }
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

    // O'xshash tovarlar: avval eng yaqin toifa (tovar turi → kichik toifa →
    // asosiy toifa), keyin qolganlari (max 6)
    val similar = remember(product.id, allProducts) {
        val others = allProducts.filter { it.id != product.id }
        val sameType = others.filter {
            product.productType.isNotBlank() && it.productType == product.productType
        }
        val sameSub = others.filter {
            product.subcategory.isNotBlank() && it.subcategory == product.subcategory
        }
        val sameRoot = others.filter { it.category == product.category }
        (sameType + sameSub + sameRoot + others).distinctBy { it.id }.take(6)
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

            // ─── Hero — bosilganda fullscreen rasm ko'rish ochiladi ──────────
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
                        .clickable(enabled = images.isNotEmpty()) { fullscreenOpen = true }
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

            // ─── Thumbnaillar — BIRINCHI qator (rasmlar tepada) ────────────────
            // Rang tanlovchi haqiqiy rasmlarga bog'langan bo'lsagina (valueToImageIndex
            // to'la) u orqali barcha rasmlarni ko'rish mumkin; aks holda (rang faqat
            // ma'lumot yoki rasmga bog'lanmagan) thumbnaillar hamon kerak.
            if (rangValueToImageIndex.isEmpty() && (images.size > 1 || product.variantlarYoqilgan)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chegara yo'q — admin nechta rasm qo'shgan bo'lsa, hammasi
                    // ko'rinadi (qatorga sig'masa gorizontal skroll bilan).
                    images.forEachIndexed { i, img ->
                        val selected = activeImg == i
                        val thumbScale by animateFloatAsState(
                            targetValue = if (selected) 1.1f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "thumb_scale"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .scale(thumbScale)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(DalliSurfaceAlt)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) DalliPrimary else DalliLine,
                                        shape = RoundedCornerShape(11.dp)
                                    )
                                    .clickable { activeImg = i }
                            ) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp))
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

            // ─── Rang tanlash — IKKINCHI qator, to'g'ridan-to'g'ri rangRasmlari
            // orqali (matn moslashtirish emas) ────────────────────────────────
            if (rangValues.isNotEmpty()) {
                RangSelector(
                    rangValues = rangValues,
                    selectedValue = selectedRangValue,
                    rangImages = product.rangRasmlari,
                    onSelect = { value ->
                        selectedRangValue = value
                        rangValueToImageIndex[value]?.let { activeImg = it }
                    }
                )
            }

            // ─── Boshqa xususiyatlar — Rang tanlashdan DARHOL KEYIN, narx
            // blokidan oldin, ketma-ket to'plangan holda. Rang va o'lcham
            // yuqorida alohida (haqiqiy rasm/narx bilan) ko'rsatilgani uchun
            // bu yerda takrorlanmaydi.
            val otherAttributes = product.attributes.filterKeys { key ->
                val isRang = key.equals("Rang", ignoreCase = true)
                val isSizeLike = key.normalizeUz().contains("olcham") && availableSizes.isNotEmpty()
                !isRang && !isSizeLike
            }
            if (otherAttributes.isNotEmpty()) {
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 14.dp)) {
                    AttributesSection(attributes = otherAttributes)
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
                    Row {
                        Text(
                            "O'lcham: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliMuted
                        )
                        Text(
                            selectedSize?.nomi ?: "tanlang",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DalliText
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSizes.forEach { size ->
                            val isSelected = selectedSize?.nomi == size.nomi
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) DalliText else DalliLine,
                                animationSpec = tween(180),
                                label = "chip_border"
                            )
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DalliSurface)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = borderColor,
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
                                    color = DalliText
                                )
                                val sizeDiscountedPrice = (size.narx * (100 - product.discountPercent) / 100).toLong()
                                Text(
                                    text = "${groupSom(sizeDiscountedPrice)} so'm",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = DalliMuted
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
                            // To'liq yo'l: "Elektronika › Aqlli uy va xavfsizlik › Aqlli uy"
                            text = product.fullCategoryPath,
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
                    Column(modifier = Modifier.weight(1f, fill = false)) {
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
                            // Narx ustuni weight(fill=false) bilan cheklangani uchun bu
                            // badge doim o'z tabiiy kengligini oladi; maxLines/softWrap —
                            // ikki xonali foizlarda ham qator buzilishidan qo'shimcha himoya.
                            Text(
                                "-${product.discountPercent}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Info chiplar — haqiqiy ma'lumot: yetkazish, kafolat, mamlakat
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    InfoChip(icon = Lucide.Truck, value = "8-12 kun", label = "Yetkazish", tone = DalliAccent)
                    InfoChip(
                        icon = Lucide.ShieldCheck,
                        value = product.guaranteeText,
                        label = "Kafolat",
                        tone = DalliSuccess
                    )
                    if (product.country.isNotBlank()) {
                        InfoChip(
                            icon = Lucide.Globe,
                            value = product.country,
                            label = "Ishlab chiqarilgan",
                            tone = DalliPrimary
                        )
                    } else {
                        InfoChip(
                            icon = Lucide.CircleCheck,
                            value = "Mavjud",
                            label = "Omborda",
                            tone = DalliSuccess
                        )
                    }
                }

                // Tavsifnoma — barcha texnik ma'lumotlar jadvali
                val specs = product.specifications
                if (specs.isNotEmpty()) {
                    SpecificationsSection(specs = specs)
                }

                // Tavsif — uzun bo'lsa yig'ib ko'rsatiladi (progressive disclosure)
                val descToShow = product.description.trim()
                if (descToShow.isNotBlank() && descToShow != "-" && descToShow != product.name.trim()) {
                    DescriptionSection(text = descToShow)
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
                                        isFavorite = favorites.any { it.id == sp.id },
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

        // ─── Full-screen rasm ko'rish ─────────────────────────────────────────
        if (fullscreenOpen && images.isNotEmpty()) {
            FullScreenImageViewer(
                images = images,
                initialIndex = activeImg.coerceIn(0, images.lastIndex),
                productName = product.name,
                onIndexChange = { activeImg = it },
                onClose = { fullscreenOpen = false }
            )
        }
    }
}

/** Hero ustidagi yumaloq suzuvchi tugma */
@Composable
private fun RoundBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = DalliText,
    badge: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(40.dp)) {
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
/** Bo'lim sarlavhasi — barcha bo'limlarda bir xil ierarxiya. */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        color = DalliText,
        letterSpacing = (-0.2).sp
    )
}

/**
 * Xususiyatlar bo'limi. "Rang" uchun rang doirasi ko'rsatiladi, qolganlari
 * oddiy chip. Ma'no faqat rang orqali berilmaydi — nomi ham doim yoziladi.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttributesSection(attributes: Map<String, List<String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Xususiyatlar")
        attributes.forEach { (nom, qiymatlar) ->
            if (qiymatlar.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = nom,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DalliMuted
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        qiymatlar.forEach { qiymat ->
                            AttributeChip(name = nom, value = qiymat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeChip(name: String, value: String) {
    val swatch = if (name.equals("Rang", ignoreCase = true)) colorSwatchFor(value) else null
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DalliSurface)
            .border(1.dp, DalliLine, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (swatch != null) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(1.dp, DalliLine2, CircleShape)
            )
        }
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DalliTextSecondary
        )
    }
}

private fun String.normalizeUz(): String =
    this.lowercase().replace("'", "").replace("’", "").replace("ʻ", "")

/**
 * Rang tanlash — "Rang: {tanlangan}" sarlavhasi va pastda kvadrat swatchlar.
 * Admin shu rangga to'g'ridan-to'g'ri rasm biriktirgan bo'lsa (product.rangRasmlari),
 * o'sha haqiqiy rasm ko'rsatiladi — matn moslashtirishga tayanilmaydi; aks holda
 * oddiy rang doirasi chiziladi.
 */
@Composable
private fun RangSelector(
    rangValues: List<String>,
    selectedValue: String?,
    rangImages: Map<String, String>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row {
            Text("Rang: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DalliMuted)
            Text(selectedValue ?: "", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliText)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rangValues.forEach { value ->
                val isSelected = value == selectedValue
                val imgUrl = rangImages[value]
                val swatchColor = if (imgUrl == null) colorSwatchFor(value) else null
                val chipScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "rang_chip_scale"
                )
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(chipScale)
                        .clip(RoundedCornerShape(16.dp))
                        .background(swatchColor ?: DalliSurfaceAlt)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) DalliText else DalliLine,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center
                ) {
                    if (imgUrl != null) {
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = value,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .padding(if (isSelected) 3.dp else 0.dp)
                                .clip(RoundedCornerShape(13.dp))
                        )
                    } else if (swatchColor == null) {
                        // Nomi rang lug'atida yo'q — rasm ham, doira ham chizib bo'lmaydi,
                        // shuning uchun kamida matn bilan farqlash kerak.
                        Text(
                            text = value,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliText,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Saytdagi rang nomlarini ko'rsatish uchun rangga o'giradi. */
private fun colorSwatchFor(name: String): Color? = when (name.trim().lowercase()) {
    "alvon" -> Color(0xFFE03C31)
    "ametist" -> Color(0xFF9966CC)
    "to'q qizil" -> Color(0xFF8B0000)
    "sarg'ish" -> Color(0xFFF5DEB3)
    "sarg'ish melanj" -> Color(0xFFD2B48C)
    "oq" -> Color(0xFFFFFFFF)
    "moviy" -> Color(0xFF4169E1)
    "xantal" -> Color(0xFFFFDB58)
    "sariq" -> Color(0xFFFFFF00)
    "yashil" -> Color(0xFF008000)
    "yashil xaki" -> Color(0xFF556B2F)
    "tilla xaki" -> Color(0xFFBDB76B)
    "tilla" -> Color(0xFFFFD700)
    "indigo" -> Color(0xFF4B0082)
    "qora" -> Color(0xFF000000)
    "kulrang" -> Color(0xFF808080)
    "qizil" -> Color(0xFFFF0000)
    "pushti" -> Color(0xFFFFC0CB)
    "jigarrang" -> Color(0xFFA52A2A)
    "binafsha" -> Color(0xFF800080)
    "havorang" -> Color(0xFF87CEEB)
    else -> null
}

/** Tavsifnoma jadvali — nom / qiymat qatorlari, orasida ajratgich. */
@Composable
private fun SpecificationsSection(specs: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        SectionTitle("Tavsifnoma")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DalliSurface)
                .border(1.dp, DalliLine, RoundedCornerShape(16.dp))
        ) {
            specs.forEachIndexed { index, (nom, qiymat) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = nom,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = DalliMuted
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = qiymat,
                        modifier = Modifier.weight(1.3f),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliText,
                        textAlign = TextAlign.End,
                        lineHeight = 20.sp
                    )
                }
                if (index != specs.lastIndex) {
                    HorizontalDivider(color = DalliLine, thickness = 1.dp)
                }
            }
        }
    }
}

/**
 * Tavsif. 4 qatordan uzun bo'lsa yig'iladi va "Batafsil" tugmasi chiqadi —
 * tugma balandligi 44dp dan kam emas.
 */
@Composable
private fun DescriptionSection(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        SectionTitle("Tavsifi")
        Text(
            text = text,
            fontSize = 14.5.sp,
            color = DalliTextSecondary,
            lineHeight = 23.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) overflows = result.hasVisualOverflow
            }
        )
        if (overflows || expanded) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .heightIn(min = 44.dp)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (expanded) "Yig'ish" else "Batafsil",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DalliPrimary
                )
            }
        }
    }
}

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

/**
 * Full-screen rasm ko'rish rejimi. Sahifalar orasida swipe (HorizontalPager),
 * har bir rasmda double-tap zoom (majburiy) va pinch-to-zoom (qo'shimcha).
 * `initialIndex` orqali ochiladi, swipe qilinganda `onIndexChange` orqali
 * tashqi `activeImg` bilan sinxron turadi. X tugmasi yoki orqaga qaytish
 * gesture'i bilan yopiladi.
 */
@Composable
private fun FullScreenImageViewer(
    images: List<String>,
    initialIndex: Int,
    productName: String,
    onIndexChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { images.size })
    var zoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        onIndexChange(pagerState.currentPage)
        zoomed = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomableImage(
                model = images.getOrNull(page),
                contentDescription = productName,
                onZoomChange = { zoomed = it }
            )
        }

        RoundBtn(
            icon = Lucide.X,
            onClick = onClose,
            tint = DalliText,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
        )

        if (images.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Faqat 2+ barmoq bosilgandagina pinch-to-zoom sifatida ishlaydi. Bitta
 * barmoqli teginish/harakatni HECH QACHON consume qilmaydi — shu sababli
 * uni o'rab turgan HorizontalPager bitta barmoqli swipe'ni erkin oladi.
 */
private suspend fun PointerInputScope.detectMultitouchZoom(
    onGesture: (zoom: Float, pan: Offset) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    onGesture(zoomChange, panChange)
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * Bitta rasm — double-tap bilan zoom in/out (1x <-> 2.5x) va pinch-to-zoom
 * (1x..4x) qo'llab-quvvatlaydi. Zoom qilinmagan holatda (scale=1f) bitta
 * barmoqli gorizontal surish HECH QANDAY gesture bilan tutib qolinmaydi —
 * shuning uchun tashqi HorizontalPager swipe qila oladi. Faqat zoom
 * qilingandan keyin bitta barmoqli surish rasmni pan qiladi (alohida,
 * shart bilan qo'shiladigan pointerInput orqali).
 */
@Composable
private fun ZoomableImage(
    model: String?,
    contentDescription: String?,
    onZoomChange: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val isZoomed = scale > 1.01f

    val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = tween(220), label = "zoom_scale")
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, animationSpec = tween(220), label = "zoom_offset_x")
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY, animationSpec = tween(220), label = "zoom_offset_y")

    LaunchedEffect(isZoomed) { onZoomChange(isZoomed) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectMultitouchZoom { zoomChange, panChange ->
                    val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                    scale = newScale
                    if (newScale > 1f) {
                        offsetX += panChange.x
                        offsetY += panChange.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .then(
                if (isZoomed) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = animatedOffsetX
                    translationY = animatedOffsetY
                }
        )
    }
}
