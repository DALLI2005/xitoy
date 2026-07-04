package com.commander.xitoy.presentation.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commander.xitoy.ui.theme.DalliAccentInk
import com.commander.xitoy.ui.theme.DalliAccentSoft
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliError
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliSuccess
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary

// Minglikni bo'sh joy bilan ajratish (100000 -> 100 000)
private fun groupSom(v: Long): String =
    v.toString().reversed().chunked(3).joinToString(" ").reversed()

// Faqat raqamlarni qoldiramiz va Long ga aylantiramiz
private fun String.toSom(): Long = filter { it.isDigit() }.toLongOrNull() ?: 0L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitCalculatorScreen(onBackClick: () -> Unit = {}) {
    var chinaPrice by remember { mutableStateOf("") }   // Xitoydan tan narxi
    var uzPrice by remember { mutableStateOf("") }       // O'zbekistonda sotuv narxi
    var qty by remember { mutableStateOf("1") }          // Miqdor (dona)

    val china = chinaPrice.toSom()
    val uz = uzPrice.toSom()
    val count = qty.filter { it.isDigit() }.toLongOrNull()?.coerceAtLeast(1L) ?: 1L

    val profitPerUnit = uz - china
    val totalProfit = profitPerUnit * count
    val marginPercent = if (china > 0) (profitPerUnit.toDouble() / china * 100) else 0.0
    val hasInput = china > 0 && uz > 0

    Scaffold(
        containerColor = DalliBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Foyda kalkulyatori",
                        fontWeight = FontWeight.ExtraBold,
                        color = DalliText,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Orqaga",
                            tint = DalliText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DalliBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Mahsulotni Xitoydan olib, O'zbekistonda sotsangiz qancha foyda qilishingizni hisoblang.",
                fontSize = 13.5.sp,
                color = DalliMuted,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(16.dp))

            // Kiritish kartochkasi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = DalliSurface),
                border = BorderStroke(1.dp, DalliLine)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MoneyField(
                        value = chinaPrice,
                        onValueChange = { chinaPrice = it },
                        label = "Xitoydan tan narxi",
                        helper = "Ushbu ilova orqali sotib olish narxi"
                    )
                    Spacer(Modifier.height(14.dp))
                    MoneyField(
                        value = uzPrice,
                        onValueChange = { uzPrice = it },
                        label = "O'zbekistonda sotuv narxi",
                        helper = "Bozorda sotilishi mumkin bo'lgan narx"
                    )
                    Spacer(Modifier.height(14.dp))
                    MoneyField(
                        value = qty,
                        onValueChange = { qty = it.filter { c -> c.isDigit() }.take(5) },
                        label = "Miqdori (dona)",
                        helper = null,
                        suffix = "dona"
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Natija kartochkasi
            ResultCard(
                hasInput = hasInput,
                profitPerUnit = profitPerUnit,
                totalProfit = totalProfit,
                marginPercent = marginPercent,
                count = count
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helper: String?,
    suffix: String = "so'm"
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DalliTextSecondary
        )
        if (helper != null) {
            Spacer(Modifier.height(2.dp))
            Text(text = helper, fontSize = 11.5.sp, color = DalliMuted)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("0", color = DalliMuted) },
            suffix = { Text(suffix, color = DalliMuted, fontWeight = FontWeight.SemiBold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = DalliText
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DalliPrimary,
                unfocusedBorderColor = DalliLine,
                cursorColor = DalliPrimary,
                focusedContainerColor = DalliSurface,
                unfocusedContainerColor = DalliSurface
            )
        )
    }
}

@Composable
private fun ResultCard(
    hasInput: Boolean,
    profitPerUnit: Long,
    totalProfit: Long,
    marginPercent: Double,
    count: Long
) {
    val profitable = profitPerUnit >= 0
    val accentColor = if (profitable) DalliSuccess else DalliError

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = BorderStroke(1.dp, DalliLine)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (hasInput) accentColor.copy(alpha = 0.12f) else DalliAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = if (hasInput) accentColor else DalliAccentInk,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Natija",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = DalliText
                )
            }

            Spacer(Modifier.height(16.dp))

            if (!hasInput) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Calculate,
                        contentDescription = null,
                        tint = DalliMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Narxlarni kiriting — natija shu yerda chiqadi",
                        fontSize = 13.5.sp,
                        color = DalliMuted
                    )
                }
            } else {
                // Bir donadan foyda
                ResultRow(
                    label = "Bir donadan foyda",
                    value = "${groupSom(profitPerUnit)} so'm",
                    valueColor = accentColor
                )
                Spacer(Modifier.height(10.dp))
                DividerLine()
                Spacer(Modifier.height(10.dp))
                // Foyda foizi
                ResultRow(
                    label = "Foyda foizi",
                    value = "%.0f%%".format(marginPercent),
                    valueColor = accentColor
                )

                Spacer(Modifier.height(16.dp))

                // Umumiy foyda — ajratilgan blok
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.10f))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (profitable) "Jami foyda" else "Jami zarar",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DalliMuted
                            )
                            Text(
                                "$count dona uchun",
                                fontSize = 11.sp,
                                color = DalliMuted
                            )
                        }
                        Text(
                            "${groupSom(totalProfit)} so'm",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = accentColor
                        )
                    }
                }

                if (!profitable) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Sotuv narxi tan narxidan past — bu narxda foyda yo'q.",
                        fontSize = 12.sp,
                        color = DalliError,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = DalliTextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DalliLine)
    )
}
