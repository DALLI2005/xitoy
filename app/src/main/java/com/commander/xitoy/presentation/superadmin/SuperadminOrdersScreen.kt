package com.commander.xitoy.presentation.superadmin

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.commander.xitoy.data.remote.SuperadminOrderListItem
import com.commander.xitoy.presentation.common.holatColors
import com.commander.xitoy.presentation.common.holatDisplay
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PackageSearch
import com.composables.icons.lucide.User
import com.commander.xitoy.domain.model.SuperadminSessionManager
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliError
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText

private val MONTH_UZ_S = listOf("yanvar","fevral","mart","aprel","may","iyun","iyul","avgust","sentabr","oktabr","noyabr","dekabr")

private fun formatOrderDateShort(raw: String): String = try {
    when {
        raw.contains("GMT") -> {
            val parts = raw.substringBefore(" GMT").trim().split(Regex("\\s+"))
            val monthEn = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            val monthIdx = monthEn.indexOf(parts[1])
            val day  = parts[2].trimStart('0').ifEmpty { "0" }
            val time = parts[4].take(5)
            "$day-${MONTH_UZ_S[monthIdx]}, $time"
        }
        raw.contains("-") && raw.contains(":") -> {
            val (datePart, timePart) = raw.trim().split(" ", limit = 2)
            val (_, month, day) = datePart.split("-")
            val time = timePart.take(5)
            "${day.trimStart('0')}-${MONTH_UZ_S[month.toInt() - 1]}, $time"
        }
        else -> raw
    }
} catch (_: Exception) { raw }

private fun groupSomOrders(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()

@Composable
fun SuperadminOrdersScreen(
    onBackClick: () -> Unit,
    onOrderClick: (String) -> Unit,
    onLockedOut: () -> Unit
) {
    val viewModel: SuperadminOrdersViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.startPolling() }
    DisposableEffect(Unit) { onDispose { viewModel.stopPolling() } }

    LaunchedEffect(state) {
        if (state is SuperadminOrdersState.LoggedOut) onLockedOut()
    }

    Column(modifier = Modifier.fillMaxSize().background(DalliBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(44.dp)) {
                Icon(Lucide.ArrowLeft, contentDescription = "Orqaga", tint = DalliText)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Superadmin panel",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = DalliText
                )
                Text(
                    "Jonli buyurtmalar oqimi",
                    fontSize = 12.sp,
                    color = DalliMuted
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Sozlamalar", tint = DalliText)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Parolni o'zgartirish") },
                        onClick = {
                            showMenu = false
                            showPasswordDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Panelni yopish") },
                        onClick = {
                            showMenu = false
                            SuperadminSessionManager.lock()
                            onLockedOut()
                        }
                    )
                }
            }
        }

        when (val s = state) {
            is SuperadminOrdersState.Loading -> OrdersLoadingBox()
            is SuperadminOrdersState.Error -> OrdersErrorBox(message = s.message, onRetry = viewModel::loadOrders)
            is SuperadminOrdersState.LoggedOut -> OrdersLoadingBox()
            is SuperadminOrdersState.Success -> {
                if (s.orders.isEmpty()) {
                    OrdersEmptyBox()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(s.orders, key = { it.orderId }) { order ->
                            SuperadminOrderCard(order = order, onClick = { onOrderClick(order.orderId) })
                        }
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        SuperadminChangePasswordDialog(onDismiss = { showPasswordDialog = false })
    }
}

@Composable
private fun SuperadminOrderCard(order: SuperadminOrderListItem, onClick: () -> Unit) {
    val (badgeBg, badgeColor) = holatColors(order.holat)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DalliSurface)
            .border(BorderStroke(1.dp, DalliLine), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DalliPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.User, contentDescription = null, tint = DalliPrimary, modifier = Modifier.size(19.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.fullname.ifBlank { "Mijoz" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DalliText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${order.orderId} · ${formatOrderDateShort(order.sana)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DalliMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(holatDisplay(order.holat), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = badgeColor)
            }
        }
        Spacer(modifier = Modifier.height(11.dp))
        Text(
            text = order.mahsulotlar,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DalliMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${groupSomOrders(order.jamiSumma)} so'm",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DalliText
        )
    }
}

@Composable
private fun OrdersLoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DalliPrimary)
    }
}

@Composable
private fun OrdersErrorBox(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Yuklab bo'lmadi", fontWeight = FontWeight.ExtraBold, color = DalliText, fontSize = 16.sp)
            Text(message, color = DalliMuted, fontSize = 13.sp)
            Text(
                "Qayta urinish",
                color = DalliPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRetry() }.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun OrdersEmptyBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Icon(Lucide.PackageSearch, null, tint = DalliPrimary, modifier = Modifier.size(38.dp))
            }
            Text("Hali buyurtma yo'q", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = DalliText)
            Text(
                "Yangi buyurtma tushishi bilan shu yerda darhol ko'rinadi.",
                fontSize = 13.sp,
                color = DalliMuted
            )
        }
    }
}

@Composable
private fun SuperadminChangePasswordDialog(onDismiss: () -> Unit) {
    val viewModel: SuperadminChangePasswordViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.success) {
        if (state.success) onDismiss()
    }

    Dialog(onDismissRequest = { if (!state.isLoading) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DalliSurface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DalliPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Lock, null, tint = DalliPrimary, modifier = Modifier.size(19.dp))
                }
                Text("Parolni o'zgartirish", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = DalliText)
            }

            OutlinedTextField(
                value = state.oldPassword,
                onValueChange = viewModel::onOldPasswordChange,
                placeholder = { Text("Joriy parol") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DalliPrimary,
                    unfocusedBorderColor = DalliLine,
                    focusedTextColor = DalliText,
                    unfocusedTextColor = DalliText,
                    cursorColor = DalliPrimary
                )
            )
            OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                placeholder = { Text("Yangi parol") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                enabled = !state.isLoading,
                isError = state.error != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DalliPrimary,
                    unfocusedBorderColor = DalliLine,
                    focusedTextColor = DalliText,
                    unfocusedTextColor = DalliText,
                    cursorColor = DalliPrimary
                )
            )
            state.error?.let { errorText ->
                Text(errorText, fontSize = 12.5.sp, color = DalliError)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { if (!state.isLoading) onDismiss() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bekor qilish")
                }
                Button(
                    onClick = { viewModel.submit() },
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Saqlash", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
