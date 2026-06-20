package com.commander.xitoy.presentation.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commander.xitoy.ui.theme.DalliAccentInk
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Sparkles
import com.commander.xitoy.ui.theme.DalliAccentSoft
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.domain.model.SessionManager
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary

@Composable
fun ProfileScreen(
    onLoginClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val stats by viewModel.stats.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    val session = SessionManager.session.collectAsState().value
    // To'liq ismni afzal ko'ramiz (bot orqali kiritilgan), bo'lmasa Telegram ismi
    val ism = session?.fullname?.takeIf { it.isNotBlank() }
        ?: session?.ism?.takeIf { it.isNotBlank() }
        ?: "Foydalanuvchi"
    val username = session?.username?.takeIf { it.isNotBlank() }
    val phone = session?.phone?.takeIf { it.isNotBlank() }
    val address = session?.address?.takeIf { it.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DalliBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = DalliText
        )
        Spacer(modifier = Modifier.height(14.dp))

        ProfileUserCard(ism = ism, username = username)
        Spacer(modifier = Modifier.height(14.dp))

        if (phone != null || address != null) {
            ProfileContactCard(phone = phone, address = address)
            Spacer(modifier = Modifier.height(14.dp))
        }

        ProfileStatsRow(stats = stats)
        Spacer(modifier = Modifier.height(14.dp))

        ProfileMenuCard()

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                SessionManager.logout()
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
            border = BorderStroke(1.dp, Color(0xFFDC2626)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Akkauntdan chiqish", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

// -------------------------------------------------------------------------
// Foydalanuvchi kartochkasi: avatar + ism + PRO badge
// -------------------------------------------------------------------------
private fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

@Composable
private fun ProfileUserCard(ism: String, username: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = BorderStroke(1.dp, DalliLine)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bosh harflar avatari — indigo kvadrat
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DalliPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialsOf(ism),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ism,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = DalliText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (username != null) "@$username" else "Telegram orqali kirgan",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = DalliMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // PRO badge — pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(DalliAccentSoft)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(Lucide.Sparkles, null, tint = DalliAccentInk, modifier = Modifier.size(11.dp))
                    Text(
                        text = "PRO",
                        color = DalliAccentInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Aloqa ma'lumotlari: telefon + manzil (bot orqali ro'yxatdan o'tishda yig'iladi)
// -------------------------------------------------------------------------
@Composable
private fun ProfileContactCard(phone: String?, address: String?) {
    val context = LocalContext.current
    // Manzil — bot orqali Google Maps havolasi sifatida saqlanadi
    val isLocationLink = address?.startsWith("http") == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = BorderStroke(1.dp, DalliLine)
    ) {
        Column {
            if (phone != null) {
                ProfileContactRow(
                    icon = Icons.Default.Phone,
                    label = "Telefon",
                    value = phone
                )
            }
            if (phone != null && address != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DalliLine)
                )
            }
            if (address != null) {
                ProfileContactRow(
                    icon = Lucide.MapPin,
                    label = "Manzil",
                    value = if (isLocationLink) "Xaritada ko'rish" else address,
                    onClick = if (isLocationLink) {
                        {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(address))
                                )
                            }
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ProfileContactRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = DalliTextSecondary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = DalliMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = DalliText
            )
        }
    }
}

// -------------------------------------------------------------------------
// Statistika qatori — 3 ta karta
// -------------------------------------------------------------------------
private fun formatCompact(amount: Long): String {
    return when {
        amount >= 1_000_000 -> "%.1fM".format(amount / 1_000_000.0)
        amount >= 1_000 -> "%.0fK".format(amount / 1_000.0)
        else -> amount.toString()
    }
}

@Composable
private fun ProfileStatsRow(stats: ProfileViewModel.ProfileStats) {
    val items = listOf(
        Triple(
            if (stats.isLoading) "..." else "${stats.orderCount}",
            "Buyurtmalar",
            DalliText
        ),
        Triple(
            if (stats.isLoading) "..." else formatCompact(stats.totalImported),
            "Import qilingan",
            DalliText
        ),
        Triple(
            if (stats.isLoading) "..." else formatCompact(stats.totalSaved),
            "Tejagan pulingiz",
            DalliSuccess
        )
    )
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items.forEach { (value, label, valueColor) ->
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = DalliSurface),
                border = BorderStroke(1.dp, DalliLine)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = value,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = valueColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = DalliMuted
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Menyu ro'yxati
// -------------------------------------------------------------------------
@Composable
private fun ProfileMenuCard() {
    val menu = listOf(
        Icons.Default.LocationOn to "Yetkazib berish manzillari",
        Icons.Default.Calculate to "Foyda kalkulyatori",
        Icons.Default.Notifications to "Kurs o'zgarishi xabarnomasi",
        Icons.Default.Language to "Til · O'zbekcha",
        Icons.Default.Inventory2 to "Bojxona hujjatlari",
        Icons.Default.SupportAgent to "Yordam markazi"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = BorderStroke(1.dp, DalliLine)
    ) {
        Column {
            menu.forEachIndexed { index, (icon, label) ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DalliLine)
                    )
                }
                ProfileMenuItem(icon = icon, title = label, onClick = {})
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = DalliTextSecondary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(13.dp))
        Text(
            text = title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DalliText,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "O'tish",
            tint = DalliMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
