package com.commander.xitoy.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (variantName: String?, variantImageUrl: String?, price: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val images = product.allImages

    var selectedVariant by remember { mutableIntStateOf(0) }

    val activePrice = if (
        product.variantlarYoqilgan &&
        product.variantNarxlari.isNotEmpty() &&
        selectedVariant < product.variantNarxlari.size
    ) {
        product.variantNarxlari[selectedVariant]
    } else {
        product.price
    }

    val selectedImageUrl = images.getOrNull(selectedVariant) ?: product.imageUrl
    val selectedVariantName = if (product.variantlarYoqilgan) {
        product.variantNomlari.getOrNull(selectedVariant)
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DalliSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product header: image + name + price
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
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
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = groupSomSheet(activePrice.toLong()),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DalliText,
                            letterSpacing = (-0.4).sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "so'm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliMuted
                        )
                    }
                }
            }

            // Variant chips (if variants enabled)
            if (product.variantlarYoqilgan && product.variantNomlari.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Rang tanlang:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliTextSecondary
                    )
                    if (images.size > 1) {
                        // Image thumbnail chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            images.forEachIndexed { i, img ->
                                val variantName = product.variantNomlari.getOrNull(i) ?: ""
                                val selected = i == selectedVariant
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            BorderStroke(if (selected) 2.dp else 1.dp, if (selected) DalliPrimary else DalliLine),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedVariant = i },
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    AsyncImage(
                                        model = img,
                                        contentDescription = variantName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (variantName.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = variantName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Text chips
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            product.variantNomlari.forEachIndexed { i, name ->
                                val selected = i == selectedVariant
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) DalliPrimary else DalliSurface)
                                        .border(
                                            BorderStroke(1.dp, if (selected) DalliPrimary else DalliLine),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedVariant = i }
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) Color.White else DalliText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Add to cart button
            Button(
                onClick = { onAddToCart(selectedVariantName, selectedImageUrl, activePrice) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
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
