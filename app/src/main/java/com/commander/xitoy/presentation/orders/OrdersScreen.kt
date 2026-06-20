package com.commander.xitoy.presentation.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commander.xitoy.data.remote.OrderItem
import com.commander.xitoy.domain.model.SessionManager
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliLine2
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSuccessSoft
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary

// Asosiy bosqichlar (progress bar uchun)
private data class Stage(val short: String, val full: String)

private val STAGES = listOf(
    Stage("Yangi",       "Buyurtma berildi"),
    Stage("Tasdiqlandi", "Tasdiqlandi"),
    Stage("Yo'lda",      "Yo'lda (transport)"),
    Stage("Yetkazildi",  "Yetkazib berildi")
)

private fun holatToStage(holat: String) = when (holat) {
    "Tolov_kutilmoqda" -> 0
    "Tasdiqlandi"      -> 1
    "Yo'lda"           -> 2
    "Yetkazildi"       -> 3
    "Rad_etildi"       -> 0
    else               -> 0
}

private fun holatDisplay(holat: String): String = when (holat) {
    "Tolov_kutilmoqda" -> "To'lov kutilmoqda"
    "Rad_etildi"       -> "Rad etildi"
    else               -> holat
}

private fun holatColors(holat: String): Pair<Color, Color> = when (holat) {
    "Yangi"            -> Color(0xFFEEF2FF) to Color(0xFF1B40D4)
    "Tolov_kutilmoqda" -> Color(0xFFFEF9C3) to Color(0xFFCA8A04)
    "Tasdiqlandi"      -> Color(0xFFEDE9FE) to Color(0xFF7C3AED)
    "Yo'lda"           -> Color(0xFFFEF3C7) to Color(0xFFD97706)
    "Yetkazildi"       -> DalliSuccessSoft to DalliSuccess
    "Rad_etildi"       -> Color(0xFFFFE4E4) to Color(0xFFDC2626)
    else               -> Color(0xFFEEF2FF) to Color(0xFF1B40D4)
}

private val MONTH_EN = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
private val MONTH_UZ = listOf("yanvar","fevral","mart","aprel","may","iyun","iyul","avgust","sentabr","oktabr","noyabr","dekabr")

private fun formatOrderDate(raw: String): String = try {
    when {
        // Sheets String(date): "Thu Jun 18 2026 19:20:00 GMT+0500 (...)"
        raw.contains("GMT") -> {
            val parts = raw.substringBefore(" GMT").trim().split(Regex("\\s+"))
            // ["Thu","Jun","18","2026","19:20:00"]
            val monthIdx = MONTH_EN.indexOf(parts[1])
            val day   = parts[2].trimStart('0').ifEmpty { "0" }
            val time  = parts[4].take(5)
            "$day-${MONTH_UZ[monthIdx]}, $time"
        }
        // ISO format: "2026-06-18 19:20" yoki "2026-06-18 19:20:00"
        raw.contains("-") && raw.contains(":") -> {
            val (datePart, timePart) = raw.trim().split(" ", limit = 2)
            val (_, month, day) = datePart.split("-")
            val time = timePart.take(5)
            "${day.trimStart('0')}-${MONTH_UZ[month.toInt() - 1]}, $time"
        }
        else -> raw
    }
} catch (_: Exception) { raw }

private fun groupSom(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()

@Composable
fun OrdersScreen() {
    val viewModel: OrdersViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val session by SessionManager.session.collectAsState()

    LaunchedEffect(session?.telegramId) {
        val tid = session?.telegramId
        if (!tid.isNullOrBlank()) {
            viewModel.startPolling(tid)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    when (val s = state) {
        is OrdersState.Loading -> OrdersLoadingState()
        is OrdersState.Error -> OrdersErrorState(
            message = s.message,
            onRetry = { viewModel.loadOrders(session?.telegramId ?: "") }
        )
        is OrdersState.Success -> {
            if (s.orders.isEmpty()) {
                OrdersEmptyState()
            } else {
                OrdersList(orders = s.orders)
            }
        }
    }
}

@Composable
private fun OrdersList(orders: List<OrderItem>) {
    var expandedId by remember { mutableStateOf<String?>(orders.firstOrNull()?.order_id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DalliBackground),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp)
    ) {
        item {
            Text(
                text = "Buyurtmalar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = DalliText
            )
        }
        items(orders, key = { it.order_id }) { order ->
            OrderCard(
                order = order,
                expanded = expandedId == order.order_id,
                onToggle = {
                    expandedId = if (expandedId == order.order_id) null else order.order_id
                }
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderItem,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val stage = holatToStage(order.holat)
    val done = order.holat == "Yetkazildi"
    val (badgeBg, badgeColor) = holatColors(order.holat)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DalliSurface)
            .clickable { onToggle() }
            .border(BorderStroke(1.dp, DalliLine), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            // Yuqori qator: ikonka + id + holat badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (done) DalliSuccessSoft else DalliPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (done) Icons.Default.Check else Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = if (done) DalliSuccess else DalliPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.order_id,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DalliText
                    )
                    Text(
                        text = formatOrderDate(order.sana),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliMuted
                    )
                }
                StatusBadge(text = holatDisplay(order.holat), bg = badgeBg, color = badgeColor)
            }

            Spacer(modifier = Modifier.height(12.dp))
            ProgressBar(stage = stage)
            Spacer(modifier = Modifier.height(11.dp))

            Text(
                text = order.mahsulotlar,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = DalliMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DalliLine)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DalliBackground)
                    .padding(horizontal = 15.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Yetkazish bosqichlari",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DalliText
                )
                OrderTimeline(stage = stage)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DalliLine)
                )

                Text(
                    text = "Mahsulotlar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DalliTextSecondary
                )
                Text(
                    text = order.mahsulotlar,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DalliText,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jami summa",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DalliTextSecondary
                    )
                    Text(
                        text = "${groupSom(order.jami_summa)} so'm",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DalliText
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, bg: Color, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
private fun ProgressBar(stage: Int) {
    val done = stage == STAGES.size - 1
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        STAGES.indices.forEach { i ->
            val active = i <= stage
            val fillColor = when {
                active && done -> DalliSuccess
                active         -> DalliPrimary
                else           -> DalliLine2
            }
            val fill by animateFloatAsState(
                targetValue = if (active) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 500,
                    delayMillis    = i * 130,
                    easing         = FastOutSlowInEasing
                ),
                label = "fill_$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DalliLine2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(fillColor)
                )
            }
        }
    }
}

@Composable
private fun OrderTimeline(stage: Int) {
    Column {
        STAGES.forEachIndexed { i, s ->
            val isDone = i < stage
            val isCurrent = i == stage
            val isLast = i == STAGES.size - 1

            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone -> DalliSuccess
                                    isCurrent -> DalliPrimary
                                    else -> DalliSurface
                                }
                            )
                            .then(
                                if (!isDone && !isCurrent)
                                    Modifier.border(BorderStroke(2.dp, DalliLine2), CircleShape)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isDone -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            isCurrent -> Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(30.dp)
                                .background(if (i < stage) DalliSuccess else DalliLine2)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(13.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (isLast) 0.dp else 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.full,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = when {
                                isCurrent -> DalliText
                                isDone -> DalliTextSecondary
                                else -> DalliMuted
                            },
                            lineHeight = 18.sp
                        )
                        if (isCurrent) {
                            Text(
                                text = "Hozirgi bosqich",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DalliPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DalliBackground),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = DalliPrimary)
    }
}

@Composable
private fun OrdersErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DalliBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = DalliMuted,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Yuklab bo'lmadi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DalliText
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = DalliMuted
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
            ) {
                Text("Qayta urinish", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OrdersEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DalliBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(DalliPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory2, null, tint = DalliPrimary, modifier = Modifier.size(40.dp))
            }
            Text(
                text = "Hali buyurtmangiz yo'q",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DalliText
            )
            Text(
                text = "Bu yerda buyurtmalaringiz va yetkazib berish holati ko'rinadi.",
                style = MaterialTheme.typography.bodyMedium,
                color = DalliMuted
            )
        }
    }
}
