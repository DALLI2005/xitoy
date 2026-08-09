package com.commander.xitoy.presentation.home

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.model.SizeOption
import com.commander.xitoy.presentation.common.colorSwatchFor
import com.commander.xitoy.presentation.common.computeSelectionRules
import com.commander.xitoy.presentation.common.rememberStrongHaptic
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliSurfaceAlt
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary
import kotlinx.coroutines.delay

/**
 * Bosh ekrandagi "+" tugmasi orqali ochiladigan tezkor buyurtma varag'i
 * (Uzum Market uslubi). DetailsScreen bilan bir xil qoidalarga rioya qiladi
 * (computeSelectionRules) — rang/o'lcham/boshqa xususiyatlar hech biri
 * oldindan tanlanmagan holda ochiladi, barchasi tanlanmaguncha "Savatga"
 * tugmasi off-active turadi.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (variantName: String?, sizeName: String?, imageUrl: String?, price: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // O'lcham — DetailsScreen'dagi kabi razmerMatritsa'dan, "0"-variant guruhi
    // (sheet'da alohida rasm-indeks tanlagichi yo'q, shuning uchun standart
    // guruh ishlatiladi).
    val availableSizes = product.razmerMatritsa["0"] ?: emptyList()
    var selectedSize by remember(product.id) { mutableStateOf<SizeOption?>(null) }
    val selectedOtherAttributes = remember(product.id) { mutableStateMapOf<String, String>() }
    // Hech narsa oldindan tanlangan bo'lmasligi kerak.
    var selectedRangValue by remember(product.id) { mutableStateOf<String?>(null) }

    val selectionRules = computeSelectionRules(
        product = product,
        availableSizes = availableSizes,
        selectedRangValue = selectedRangValue,
        selectedSize = selectedSize,
        selectedOtherAttributes = selectedOtherAttributes
    )
    val rangValues = selectionRules.rangValues
    val otherAttributes = selectionRules.otherAttributes
    val missingSelections = selectionRules.missingSelections
    val canAddToCart = missingSelections.isEmpty()

    // Rang tanlansa va unga rasm biriktirilgan bo'lsa — o'sha haqiqiy rasm
    // ko'rsatiladi (to'g'ridan-to'g'ri rangRasmlari orqali, matn moslashtirmasdan).
    val selectedImageUrl = selectedRangValue?.let { product.rangRasmlari[it] } ?: product.imageUrl
    val activeBasePrice = selectedSize?.narx ?: product.price
    val finalPrice = (activeBasePrice * (100 - product.discountPercent) / 100).toLong()

    val haptic = rememberStrongHaptic()
    var pressed by remember { mutableStateOf(false) }
    val btnScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )
    LaunchedEffect(pressed) {
        if (pressed) { delay(150); pressed = false }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DalliSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── Rasm + nom + narx (chegirma bilan) ──────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DalliText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Narx ustuni weight(fill=false) — chegirma badge hech qachon
                        // qatorga sig'may sinmasligi uchun (DetailsScreen'da tuzatilgan
                        // xuddi shu bug'ning oldini olish naqshi).
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${groupSomSheet(finalPrice)} so'm",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DalliText,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (product.discountPercent > 0) {
                                    Text(
                                        text = "${groupSomSheet(activeBasePrice.toLong())} so'm",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DalliMuted,
                                        textDecoration = TextDecoration.LineThrough,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (product.discountPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DalliPrimary)
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "-${product.discountPercent}%",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // ── Rang tanlash — rasmli/rangli kompakt kartochkalar ───────────
            if (rangValues.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text("Rang: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DalliMuted)
                        Text(selectedRangValue ?: "", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliText)
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rangValues.forEach { value ->
                            val isSelected = value == selectedRangValue
                            val imgUrl = product.rangRasmlari[value]
                            val swatchColor = if (imgUrl == null) colorSwatchFor(value) else null
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(swatchColor ?: DalliSurfaceAlt)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) DalliText else DalliLine,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedRangValue = value },
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
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else if (swatchColor == null) {
                                    Text(
                                        text = value,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DalliText,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── O'lcham tanlash ──────────────────────────────────────────────
            if (availableSizes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text("O'lcham: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DalliMuted)
                        Text(selectedSize?.nomi ?: "", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliText)
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSizes.forEach { size ->
                            val isSelected = selectedSize?.nomi == size.nomi
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DalliSurface)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) DalliText else DalliLine,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedSize = if (isSelected) null else size }
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(size.nomi, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliText)
                            }
                        }
                    }
                }
            }

            // ── Boshqa xususiyatlar (masalan Xotira hajmi) ──────────────────
            if (otherAttributes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    otherAttributes.forEach { (nom, qiymatlar) ->
                        if (qiymatlar.isNotEmpty()) {
                            val selectedValue = selectedOtherAttributes[nom]
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(nom, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = DalliMuted)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    qiymatlar.forEach { qiymat ->
                                        val isSelected = selectedValue == qiymat
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(DalliSurface)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) DalliText else DalliLine,
                                                    shape = RoundedCornerShape(9.dp)
                                                )
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedOtherAttributes.remove(nom)
                                                    } else {
                                                        selectedOtherAttributes[nom] = qiymat
                                                    }
                                                }
                                                .padding(horizontal = 11.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = qiymat,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) DalliText else DalliTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!canAddToCart) {
                Text(
                    text = "Iltimos, ${missingSelections.joinToString(", ")} tanlang",
                    fontSize = 12.sp,
                    color = DalliMuted,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Savatga qo'shish tugmasi ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(btnScale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canAddToCart) DalliPrimary else DalliMuted.copy(alpha = 0.35f))
                    .clickable {
                        if (canAddToCart) {
                            haptic()
                            pressed = true
                            onAddToCart(selectedRangValue, selectedSize?.nomi, selectedImageUrl, activeBasePrice)
                        } else {
                            Toast.makeText(
                                context,
                                "${missingSelections.joinToString(", ")} tanlang",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Savatga qo'shish",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

private fun groupSomSheet(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()
