package com.commander.xitoy.presentation.superadmin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.commander.xitoy.data.remote.SuperadminOrderItem
import com.commander.xitoy.presentation.common.holatColors
import com.commander.xitoy.presentation.common.holatDisplay
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.User
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.composables.icons.lucide.Lucide

private fun groupSomDetail(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()

@Composable
fun SuperadminOrderDetailScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onLockedOut: () -> Unit
) {
    val viewModel: SuperadminOrderDetailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(orderId) { viewModel.load(orderId) }

    LaunchedEffect(state) {
        if (state is SuperadminOrderDetailState.LoggedOut) onLockedOut()
    }

    Column(modifier = Modifier.fillMaxSize().background(DalliBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(44.dp)) {
                Icon(Lucide.ArrowLeft, contentDescription = "Orqaga", tint = DalliText)
            }
            Text(
                "Buyurtma tafsiloti",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = DalliText
            )
        }

        when (val s = state) {
            is SuperadminOrderDetailState.Loading, is SuperadminOrderDetailState.LoggedOut -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DalliPrimary)
                }
            }
            is SuperadminOrderDetailState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("Topilmadi", fontWeight = FontWeight.ExtraBold, color = DalliText, fontSize = 16.sp)
                        Text(s.message, color = DalliMuted, fontSize = 13.sp)
                    }
                }
            }
            is SuperadminOrderDetailState.Success -> {
                OrderDetailContent(order = s.order)
            }
        }
    }
}

@Composable
private fun OrderDetailContent(order: com.commander.xitoy.data.remote.SuperadminOrderDetail) {
    val (badgeBg, badgeColor) = holatColors(order.status)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DalliSurface)
                .border(BorderStroke(1.dp, DalliLine), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.orderId, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DalliText)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(holatDisplay(order.status), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = badgeColor)
                }
            }
            Text(order.createdAt, fontSize = 12.sp, color = DalliMuted)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DalliSurface)
                .border(BorderStroke(1.dp, DalliLine), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mijoz", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliMuted)
            InfoRow(icon = Lucide.User, text = order.fullname.ifBlank { "Noma'lum" })
            if (order.phone.isNotBlank()) {
                InfoRow(icon = Lucide.Phone, text = order.phone)
            }
            if (order.locationLink.isNotBlank()) {
                InfoRow(icon = Lucide.MapPin, text = order.locationLink)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DalliSurface)
                .border(BorderStroke(1.dp, DalliLine), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mahsulotlar (${order.items.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliMuted)
            order.items.forEachIndexed { index, item ->
                OrderProductRow(item)
                if (index != order.items.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DalliLine))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DalliPrimarySoft)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jami summa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DalliText)
            Text("${groupSomDetail(order.jamiSumma)} so'm", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DalliPrimary)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = DalliPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DalliText)
    }
}

@Composable
private fun OrderProductRow(item: SuperadminOrderItem) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DalliBackground),
            contentAlignment = Alignment.Center
        ) {
            if (!item.rasm.isNullOrBlank()) {
                AsyncImage(
                    model = item.rasm,
                    contentDescription = item.nomi,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                )
            } else {
                Icon(Lucide.Package, contentDescription = null, tint = DalliMuted, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.nomi, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = DalliText)
            val attrs = listOfNotNull(
                item.variant?.takeIf { it.isNotBlank() },
                item.razmer?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (attrs.isNotBlank()) {
                Text(attrs, fontSize = 12.sp, color = DalliMuted)
            }
            Text("${item.soni} dona", fontSize = 12.sp, color = DalliMuted)
        }
        Text("${groupSomDetail(item.narx)} so'm", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = DalliText)
    }
}
