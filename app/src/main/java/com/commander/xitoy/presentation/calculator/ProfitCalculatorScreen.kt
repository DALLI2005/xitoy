package com.commander.xitoy.presentation.calculator

import android.content.Intent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

// ---- Valyuta va rejim ----
private enum class Currency(val symbol: String, val defaultRate: Double) {
    SOM("so'm", 1.0),
    CNY("¥", 1750.0),
    USD("$", 12600.0)
}

private enum class CalcMode { KNOWN_PRICE, TARGET_MARGIN }

// ---- Formatlash / parslash ----
private fun Long.somStr(): String {
    val neg = this < 0
    val s = abs(this).toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (neg) "-$s" else s
}

private fun Double.somStr(): String = roundToLong().somStr()

private fun String.num(): Double =
    replace(" ", "").replace(',', '.').toDoubleOrNull() ?: 0.0

// Faqat raqamlar
private fun intIn(s: String): String = s.filter { it.isDigit() }.take(12)

// Raqamlar + bitta nuqta (yuan/foiz uchun)
private fun decIn(s: String): String {
    val f = s.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.').take(12)
    val i = f.indexOf('.')
    return if (i < 0) f else f.substring(0, i + 1) + f.substring(i + 1).replace(".", "")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitCalculatorScreen(onBackClick: () -> Unit = {}) {
    val viewModel: ProfitCalculatorViewModel = hiltViewModel()
    val products by viewModel.products.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadProducts() }

    // ---- Kiritish holati ----
    var currency by remember { mutableStateOf(Currency.SOM) }
    var rateText by remember { mutableStateOf("") }
    var basePriceText by remember { mutableStateOf("") }
    var shippingText by remember { mutableStateOf("") }
    var commissionText by remember { mutableStateOf("") }
    var customsText by remember { mutableStateOf("") }
    var fixedCostText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(CalcMode.KNOWN_PRICE) }
    var sellPriceText by remember { mutableStateOf("") }
    var targetMarginText by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var productName by remember { mutableStateOf<String?>(null) }

    var showPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // ---- Hisob-kitob ----
    val rate = if (currency == Currency.SOM) 1.0
    else rateText.num().let { if (it <= 0) currency.defaultRate else it }
    val baseSom = basePriceText.num() * rate
    val shipping = shippingText.num()
    val commission = commissionText.num()
    val customs = customsText.num()
    val fixed = fixedCostText.num()
    val qty = intIn(qtyText).toLongOrNull()?.coerceAtLeast(1L) ?: 1L

    val costPerUnit = baseSom + shipping + baseSom * commission / 100 + baseSom * customs / 100
    val sellPerUnit = when (mode) {
        CalcMode.KNOWN_PRICE -> sellPriceText.num()
        CalcMode.TARGET_MARGIN -> costPerUnit * (1 + targetMarginText.num() / 100)
    }
    val profitPerUnit = sellPerUnit - costPerUnit
    val marginPct = if (costPerUnit > 0) profitPerUnit / costPerUnit * 100 else 0.0
    val totalInvestment = costPerUnit * qty + fixed
    val totalRevenue = sellPerUnit * qty
    val totalProfit = totalRevenue - totalInvestment
    val roi = if (totalInvestment > 0) totalProfit / totalInvestment * 100 else 0.0
    val breakEven = if (sellPerUnit > 0) ceil(totalInvestment / sellPerUnit).toLong() else 0L
    val hasInput = baseSom > 0 && sellPerUnit > 0

    Scaffold(
        containerColor = DalliBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Foyda kalkulyatori", fontWeight = FontWeight.ExtraBold, color = DalliText, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga", tint = DalliText)
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
                "Xitoydan olib O'zbekistonda sotsangiz — real xarajatlar bilan qancha foyda qilishingizni hisoblang.",
                fontSize = 13.5.sp, color = DalliMuted, lineHeight = 19.sp
            )
            Spacer(Modifier.height(14.dp))

            // Katalogdan tanlash
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DalliLine),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DalliPrimary)
            ) {
                Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    productName?.let { "Mahsulot: $it" } ?: "Katalogdan mahsulot tanlash",
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1
                )
            }

            Spacer(Modifier.height(14.dp))

            // ---- 1. Tan narx (xarajatlar) ----
            SectionCard(title = "1. Tan narx (xarajatlar)") {
                // Valyuta tanlash
                Text("Valyuta", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliTextSecondary)
                Spacer(Modifier.height(8.dp))
                SegmentedToggle(
                    options = listOf(
                        Currency.SOM to "So'm",
                        Currency.CNY to "Yuan ¥",
                        Currency.USD to "Dollar $"
                    ),
                    selected = currency,
                    onSelect = { currency = it }
                )
                Spacer(Modifier.height(14.dp))

                if (currency != Currency.SOM) {
                    MoneyField(
                        value = rateText, onValueChange = { rateText = decIn(it) },
                        label = "Kurs (1 ${currency.symbol} = ? so'm)",
                        helper = "Bo'sh qoldirsangiz taxminiy: ${currency.defaultRate.somStr()}",
                        suffix = "so'm"
                    )
                    Spacer(Modifier.height(14.dp))
                }

                MoneyField(
                    value = basePriceText, onValueChange = { basePriceText = decIn(it) },
                    label = "Mahsulot narxi",
                    helper = "Xitoydagi narxi (${currency.symbol})",
                    suffix = currency.symbol
                )
                if (currency != Currency.SOM && baseSom > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("≈ ${baseSom.somStr()} so'm", fontSize = 12.sp, color = DalliPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(14.dp))
                MoneyField(
                    value = shippingText, onValueChange = { shippingText = intIn(it) },
                    label = "Yetkazib berish (1 dona)", helper = "Dostavka narxi, so'm"
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        MoneyField(
                            value = commissionText, onValueChange = { commissionText = decIn(it) },
                            label = "Komissiya", helper = null, suffix = "%"
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        MoneyField(
                            value = customsText, onValueChange = { customsText = decIn(it) },
                            label = "Bojxona", helper = null, suffix = "%"
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- 2. Sotuv va miqdor ----
            SectionCard(title = "2. Sotuv va miqdor") {
                SegmentedToggle(
                    options = listOf(
                        CalcMode.KNOWN_PRICE to "Sotuv narxi",
                        CalcMode.TARGET_MARGIN to "Foyda % belgilash"
                    ),
                    selected = mode,
                    onSelect = { mode = it }
                )
                Spacer(Modifier.height(14.dp))
                when (mode) {
                    CalcMode.KNOWN_PRICE -> MoneyField(
                        value = sellPriceText, onValueChange = { sellPriceText = intIn(it) },
                        label = "O'zbekistonda sotuv narxi (1 dona)", helper = "Bozorda sotilishi mumkin bo'lgan narx"
                    )
                    CalcMode.TARGET_MARGIN -> {
                        MoneyField(
                            value = targetMarginText, onValueChange = { targetMarginText = decIn(it) },
                            label = "Xohlagan foyda (ustama)", helper = "Tan narx ustiga qo'shiladigan foiz", suffix = "%"
                        )
                        if (hasInput) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tavsiya sotuv narxi: ${sellPerUnit.somStr()} so'm",
                                fontSize = 12.5.sp, color = DalliPrimary, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        MoneyField(
                            value = qtyText, onValueChange = { qtyText = intIn(it).take(5) },
                            label = "Miqdori", helper = null, suffix = "dona"
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        MoneyField(
                            value = fixedCostText, onValueChange = { fixedCostText = intIn(it) },
                            label = "Qo'shimcha xarajat", helper = null, suffix = "so'm"
                        )
                    }
                }
                Text(
                    "Qo'shimcha xarajat — butun partiya uchun reklama, ijara va h.k.",
                    fontSize = 11.sp, color = DalliMuted, modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // ---- Natija ----
            ResultCard(
                hasInput = hasInput,
                costPerUnit = costPerUnit,
                sellPerUnit = sellPerUnit,
                profitPerUnit = profitPerUnit,
                marginPct = marginPct,
                totalInvestment = totalInvestment,
                totalProfit = totalProfit,
                roi = roi,
                breakEven = breakEven,
                qty = qty
            )

            if (hasInput) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DalliPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DalliPrimary)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Saqlash", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            val text = buildShareText(
                                productName, costPerUnit, sellPerUnit, profitPerUnit,
                                marginPct, qty, totalProfit, roi
                            )
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            runCatching { context.startActivity(Intent.createChooser(send, "Ulashish")) }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ulashish", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            // ---- Saqlangan hisoblar ----
            if (saved.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Saqlangan hisoblar", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DalliText)
                Spacer(Modifier.height(10.dp))
                saved.forEach { calc ->
                    SavedCalcRow(calc = calc, onDelete = { viewModel.delete(calc) })
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    // ---- Mahsulot tanlash sheet ----
    if (showPicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
            containerColor = DalliSurface
        ) {
            Text(
                "Mahsulot tanlang",
                fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = DalliText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            if (products.isEmpty()) {
                Text(
                    "Mahsulotlar yuklanmoqda yoki topilmadi.",
                    color = DalliMuted, fontSize = 14.sp,
                    modifier = Modifier.padding(20.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                    items(products, key = { it.id }) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currency = Currency.SOM
                                    basePriceText = p.price.roundToLong().toString()
                                    productName = p.name
                                    showPicker = false
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = DalliText, modifier = Modifier.weight(1f), maxLines = 1)
                            Spacer(Modifier.width(10.dp))
                            Text("${p.price.somStr()} so'm", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = DalliPrimary)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(DalliLine))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    // ---- Saqlash dialogi ----
    if (showSaveDialog) {
        var nameText by remember { mutableStateOf(productName ?: "") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Hisobni saqlash", fontWeight = FontWeight.ExtraBold, color = DalliText) },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(40) },
                    singleLine = true,
                    placeholder = { Text("Nomi (masalan: Quloqchin)", color = DalliMuted) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DalliPrimary, unfocusedBorderColor = DalliLine, cursorColor = DalliPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.save(
                        SavedCalc(
                            name = nameText.ifBlank { "Hisob" },
                            costPrice = costPerUnit.roundToLong(),
                            sellPrice = sellPerUnit.roundToLong(),
                            qty = qty,
                            totalProfit = totalProfit.roundToLong(),
                            marginPercent = marginPct.roundToLong().toInt()
                        )
                    )
                    showSaveDialog = false
                }) { Text("Saqlash", color = DalliPrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Bekor", color = DalliMuted) }
            },
            containerColor = DalliSurface
        )
    }
}

// ---------------------------------------------------------------------------
private fun buildShareText(
    name: String?, cost: Double, sell: Double, profit: Double,
    margin: Double, qty: Long, totalProfit: Double, roi: Double
): String = buildString {
    appendLine("📊 Foyda hisobi${name?.let { " — $it" } ?: ""}")
    appendLine("Tan narx: ${cost.somStr()} so'm")
    appendLine("Sotuv narxi: ${sell.somStr()} so'm")
    appendLine("Bir donadan foyda: ${profit.somStr()} so'm (${margin.roundToLong()}%)")
    appendLine("Miqdor: $qty dona")
    appendLine("Jami foyda: ${totalProfit.somStr()} so'm")
    append("ROI: ${roi.roundToLong()}%")
}

// ---------------------------------------------------------------------------
@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = BorderStroke(1.dp, DalliLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = DalliText)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun <T> SegmentedToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DalliBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) DalliPrimary else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.5.sp,
                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (active) Color.White else DalliMuted,
                    maxLines = 1
                )
            }
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
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DalliTextSecondary)
        if (helper != null) {
            Spacer(Modifier.height(2.dp))
            Text(helper, fontSize = 11.5.sp, color = DalliMuted)
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
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DalliText),
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
    costPerUnit: Double,
    sellPerUnit: Double,
    profitPerUnit: Double,
    marginPct: Double,
    totalInvestment: Double,
    totalProfit: Double,
    roi: Double,
    breakEven: Long,
    qty: Long
) {
    val profitable = profitPerUnit >= 0
    val accent = if (profitable) DalliSuccess else DalliError

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
                        .background(if (hasInput) accent.copy(alpha = 0.12f) else DalliAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = if (hasInput) accent else DalliAccentInk, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Natija", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DalliText)
            }
            Spacer(Modifier.height(16.dp))

            if (!hasInput) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, null, tint = DalliMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Narx va sotuvni kiriting — natija shu yerda chiqadi", fontSize = 13.5.sp, color = DalliMuted)
                }
            } else {
                ResultRow("Tan narx (1 dona)", "${costPerUnit.somStr()} so'm", DalliText)
                Spacer(Modifier.height(9.dp))
                ResultRow("Sotuv narxi (1 dona)", "${sellPerUnit.somStr()} so'm", DalliText)
                Spacer(Modifier.height(9.dp))
                ResultRow("Bir donadan foyda", "${profitPerUnit.somStr()} so'm", accent)
                Spacer(Modifier.height(9.dp))
                ResultRow("Foyda foizi (ustama)", "${marginPct.roundToLong()}%", accent)
                Spacer(Modifier.height(9.dp))
                ResultRow("Jami investitsiya", "${totalInvestment.somStr()} so'm", DalliText)
                Spacer(Modifier.height(9.dp))
                ResultRow("ROI (pul qaytishi)", "${roi.roundToLong()}%", accent)
                Spacer(Modifier.height(9.dp))
                ResultRow("Zarar chegarasi", "$breakEven / $qty dona", DalliTextSecondary)

                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.10f))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (profitable) "Jami foyda" else "Jami zarar", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = DalliMuted)
                            Text("$qty dona uchun", fontSize = 11.sp, color = DalliMuted)
                        }
                        Text("${totalProfit.somStr()} so'm", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = accent)
                    }
                }
                if (!profitable) {
                    Spacer(Modifier.height(10.dp))
                    Text("Sotuv narxi tan narxidan past — bu narxda foyda yo'q.", fontSize = 12.sp, color = DalliError, fontWeight = FontWeight.SemiBold)
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
        Text(label, fontSize = 13.5.sp, color = DalliTextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
    }
}

@Composable
private fun SavedCalcRow(calc: SavedCalc, onDelete: () -> Unit) {
    val profitable = calc.totalProfit >= 0
    val accent = if (profitable) DalliSuccess else DalliError
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = DalliSurface),
        border = BorderStroke(1.dp, DalliLine)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(calc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DalliText, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${calc.qty} dona · ${calc.marginPercent}% ustama",
                    fontSize = 11.5.sp, color = DalliMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${calc.totalProfit.somStr()} so'm", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = accent)
                Text("jami foyda", fontSize = 10.5.sp, color = DalliMuted)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "O'chirish", tint = DalliMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
