package com.commander.xitoy.presentation.cart

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.commander.xitoy.domain.model.RishtonLocationChecker
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.commander.xitoy.data.remote.OrderItemDetail
import com.commander.xitoy.domain.model.CartItem
import com.commander.xitoy.domain.model.CartManager
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.model.SessionManager
import com.commander.xitoy.presentation.common.rememberHaptic
import com.commander.xitoy.presentation.common.rememberStrongHaptic
import com.commander.xitoy.ui.theme.DalliAccentInk
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary

private fun groupSom(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()

@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onOrderPlaced: () -> Unit = {},
    onNavigateToPayment: (orderId: String, total: Long) -> Unit = { _, _ -> }
) {
    val viewModel: CartViewModel = hiltViewModel()
    val orderState by viewModel.orderState.collectAsState()

    val cartItems = CartManager.cartItems.collectAsState().value
    val groupedItems = remember(cartItems) {
        cartItems.groupBy { "${it.product.name}|${it.variantName}|${it.sizeName}" }
            .entries.map { (_, items) -> items.first() to items.size }
    }
    val totalPrice = cartItems.sumOf { it.effectivePrice * (100 - it.product.discountPercent) / 100.0 }

    var showDialog by remember { mutableStateOf(false) }

    val session = SessionManager.session.value
    var name by remember { mutableStateOf(session?.fullname?.ifBlank { session.ism } ?: "") }
    var phone by remember {
        mutableStateOf(if (session?.phone.isNullOrBlank()) "+998" else session?.phone ?: "+998")
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms -> locationPermissionGranted = perms.values.any { it } }

    var showOutsideRishtonWarning by remember { mutableStateOf(false) }
    var pendingOrderAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Error toast
    LaunchedEffect(orderState) {
        if (orderState is OrderState.Error) {
            Toast.makeText(context, (orderState as OrderState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DalliBackground)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Savatcha",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = DalliText
                )
                if (cartItems.isNotEmpty()) {
                    Text(
                        text = "${cartItems.size} dona · ${groupedItems.size} pozitsiya",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            if (groupedItems.isEmpty()) {
                CartEmptyState(
                    modifier = Modifier.weight(1f),
                    onStartShopping = onBackClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    contentPadding = PaddingValues(bottom = 200.dp)
                ) {
                    items(groupedItems, key = { "${it.first.product.name}|${it.first.variantName}|${it.first.sizeName}" }) { (item, quantity) ->
                        CartItemCard(
                            item = item,
                            quantity = quantity,
                            onIncrement = { CartManager.addToCart(item.product, item.variantName, item.effectivePrice, item.variantImageUrl, item.sizeName) },
                            onDecrement = { CartManager.removeFromCart(item) },
                            onRemoveAll = { CartManager.removeAllOf(item) }
                        )
                    }
                }
            }
        }

        // Sticky buyurtma paneli
        if (groupedItems.isNotEmpty()) {
            OrderSummary(
                totalPrice = totalPrice,
                itemCount = cartItems.size,
                enabled = cartItems.isNotEmpty() && orderState !is OrderState.Loading,
                onOrderClick = { showDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Loading overlay
        if (orderState is OrderState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DalliPrimary)
            }
        }
    }

    // Buyurtma ma'lumotlari dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Buyurtmani rasmiylashtirish",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PolishedDialogField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Ismingiz",
                        icon = Icons.Default.Person
                    )
                    PolishedDialogField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Telefon raqamingiz",
                        icon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.length >= 13) {
                            val itemsText = groupedItems.joinToString(", ") { (item, q) ->
                                val parts = listOfNotNull(item.variantName, item.sizeName)
                                val suffix = if (parts.isNotEmpty()) " (${parts.joinToString(", ")})" else ""
                                "${item.product.name}$suffix x$q"
                            }
                            val royxat = groupedItems.map { (item, q) ->
                                OrderItemDetail(
                                    nomi    = item.product.name,
                                    variant = item.variantName,
                                    razmer  = item.sizeName,
                                    soni    = q,
                                    narx    = item.effectivePrice.toLong(),
                                    rasm    = item.variantImageUrl ?: item.product.imageUrl
                                )
                            }
                            val firstItem = groupedItems.firstOrNull()?.first
                            val orderAction: () -> Unit = {
                                showDialog = false
                                viewModel.placeOrder(
                                    telegramId          = session?.telegramId ?: "",
                                    fullname            = name,
                                    phone               = phone,
                                    locationLink        = session?.address ?: "",
                                    mahsulotlar         = itemsText,
                                    jamiSumma           = totalPrice.toLong(),
                                    mahsulotRasm        = firstItem?.variantImageUrl ?: firstItem?.product?.imageUrl,
                                    mahsulotlarRoyxati  = royxat
                                )
                            }

                            // Ruxsat hali so'ralmagan bo'lsa, parallel ravishda so'rash
                            if (!locationPermissionGranted) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }

                            // Lokatsiya tekshiruvi — 8 soniya timeout, TO'SIQ EMAS
                            coroutineScope.launch {
                                val distance = withTimeoutOrNull(8000) {
                                    RishtonLocationChecker.getDistanceFromRishtonKm(context)
                                }
                                if (RishtonLocationChecker.isOutsideRishton(distance)) {
                                    pendingOrderAction = orderAction
                                    showOutsideRishtonWarning = true
                                } else {
                                    orderAction()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Iltimos, ma'lumotlarni to'liq kiriting!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Tasdiqlash", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Bekor qilish", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Muvaffaqiyat dialog
    if (orderState is OrderState.Success) {
        val orderId = (orderState as OrderState.Success).orderId
        AlertDialog(
            onDismissRequest = { },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Buyurtma qabul qilindi!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Buyurtmangiz muvaffaqiyatli rasmiylashtirildi. Tez orada siz bilan bog'lanamiz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DalliMuted
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DalliPrimarySoft)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Buyurtma ID",
                                fontSize = 12.sp,
                                color = DalliMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = orderId,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DalliPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val capturedTotal = totalPrice.toLong()
                        viewModel.resetState()
                        CartManager.clearCart()
                        onNavigateToPayment(orderId, capturedTotal)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
                ) {
                    Text("To'lovga o'tish", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Rishton hududidan tashqarida ogohlantirish dialogi
    if (showOutsideRishtonWarning) {
        AlertDialog(
            onDismissRequest = {
                showOutsideRishtonWarning = false
                pendingOrderAction = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = DalliAccentInk,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Diqqat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "Siz hozir Rishton hududidan tashqarida ko'rinasiz. Baribir buyurtma bermoqchimisiz?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DalliMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingOrderAction?.invoke()
                        showOutsideRishtonWarning = false
                        pendingOrderAction = null
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Davom etish", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOutsideRishtonWarning = false
                        pendingOrderAction = null
                    }
                ) {
                    Text("Bekor qilish", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// Savat bo'sh holati
// -------------------------------------------------------------------------
@Composable
private fun CartEmptyState(
    modifier: Modifier = Modifier,
    onStartShopping: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(DalliPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Savatcha bo'sh",
                    tint = DalliPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Savatcha bo'sh",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DalliText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Katalogdan kerakli mahsulotlarni tanlang, buyurtma tafsilotlari shu yerda ko'rinadi.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DalliMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onStartShopping,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Xaridni boshlash",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Savatdagi bitta mahsulot qatori
// -------------------------------------------------------------------------
@Composable
private fun CartItemCard(
    item: CartItem,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemoveAll: () -> Unit
) {
    val product = item.product
    val itemPrice = (item.effectivePrice * (100 - product.discountPercent) / 100).toLong()
    val lineTotal = itemPrice * quantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DalliLine)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            AsyncImage(
                model = item.variantImageUrl ?: product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(DalliPrimarySoft)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = product.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DalliText,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onRemoveAll() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "O'chirish",
                            tint = DalliMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (item.variantName != null) {
                    Text(
                        text = "Rangi: ${item.variantName}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliPrimary
                    )
                }
                if (item.sizeName != null) {
                    Text(
                        text = "O'lchami: ${item.sizeName}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliPrimary
                    )
                }
                Text(
                    text = "${groupSom(itemPrice)} so'm / dona",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DalliMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = groupSom(lineTotal),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DalliText,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "so'm",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DalliMuted
                        )
                    }
                    QuantityStepper(
                        quantity = quantity,
                        onDecrement = onDecrement,
                        onIncrement = onIncrement
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val haptic = rememberHaptic()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, DalliLine, RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(DalliBackground)
                .clickable { haptic(); onDecrement() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DalliTextSecondary)
        }
        Text(
            text = "$quantity",
            modifier = Modifier
                .widthIn(min = 34.dp)
                .padding(horizontal = 4.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DalliText,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(DalliBackground)
                .clickable { haptic(); onIncrement() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = DalliTextSecondary)
        }
    }
}

// -------------------------------------------------------------------------
// Pastki umumiy hisob kartochkasi
// -------------------------------------------------------------------------
@Composable
private fun OrderSummary(
    totalPrice: Double,
    itemCount: Int,
    enabled: Boolean,
    onOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orderHaptic = rememberStrongHaptic()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DalliLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow(
                label = "Mahsulotlar ($itemCount dona)",
                value = "${groupSom(totalPrice.toLong())} so'm",
                valueColor = DalliTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow(
                label = "Yetkazib berish",
                value = "Bepul",
                valueColor = DalliSuccess
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DalliLine)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jami",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DalliText
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = groupSom(totalPrice.toLong()),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DalliPrimary,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "so'm",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { orderHaptic(); onOrderClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DalliPrimary,
                    disabledContainerColor = DalliLine,
                    disabledContentColor = DalliMuted
                )
            ) {
                Text(
                    text = "Buyurtma berish",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DalliMuted
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun PolishedDialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = label, tint = DalliMuted)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = DalliBackground,
            unfocusedContainerColor = DalliBackground,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = DalliPrimary
        )
    )
}
