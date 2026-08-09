package com.commander.xitoy.presentation.common

import androidx.compose.ui.graphics.Color
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.model.SizeOption

/**
 * Mahsulotni savatga qo'shishdan oldin tanlov talab qiladigan xususiyatlari
 * bormi (rang, o'lcham yoki boshqa attribute, yoki eski Variantlar tizimi).
 * Bo'lsa — tanlov oynasi (QuickAddBottomSheet yoki DetailsScreen) orqali
 * tanlov so'raladi; aks holda to'g'ridan-to'g'ri savatga qo'shiladi.
 */
fun productNeedsSelection(product: Product): Boolean =
    product.attributes.values.any { it.isNotEmpty() } ||
        product.razmerMatritsa.values.any { it.isNotEmpty() } ||
        product.variantlarYoqilgan

/** O'zbekcha apostrof variantlarini bir xillashtiradi (masalan "o'lcham" solishtirish uchun). */
fun String.normalizeUzText(): String =
    this.lowercase().replace("'", "").replace("’", "").replace("ʻ", "")

data class ProductSelectionRules(
    val rangValues: List<String>,
    val otherAttributes: Map<String, List<String>>,
    val missingSelections: List<String>
)

/**
 * Mahsulotning tanlanishi kerak bo'lgan xususiyatlarini (rang, o'lcham, boshqalar)
 * va hozirgi tanlovlarga ko'ra hali yetishmayotganlarini hisoblaydi.
 *
 * DetailsScreen va QuickAddBottomSheet BIR XIL qoidaga rioya qilishi uchun bu
 * funksiya markazlashgan — ikkalasi ham shu yerdan chaqiradi, shunda kelajakda
 * ular orasida qoidalar farqlanib, "tanlovsiz savatga tushib qolish" muammosi
 * qaytadan paydo bo'lmaydi.
 */
fun computeSelectionRules(
    product: Product,
    availableSizes: List<SizeOption>,
    selectedRangValue: String?,
    selectedSize: SizeOption?,
    selectedOtherAttributes: Map<String, String>
): ProductSelectionRules {
    val rangAttrKey = product.attributes.keys.firstOrNull { it.equals("Rang", ignoreCase = true) }
    val rangValues = rangAttrKey?.let { product.attributes[it] } ?: emptyList()

    // Rang va o'lcham yuqorida alohida (haqiqiy rasm/narx bilan) ko'rsatilgani
    // uchun umumiy "Xususiyatlar" ro'yxatida takrorlanmaydi.
    val otherAttributes = product.attributes.filterKeys { key ->
        val isRang = key.equals("Rang", ignoreCase = true)
        val isSizeLike = key.normalizeUzText().contains("olcham") && availableSizes.isNotEmpty()
        !isRang && !isSizeLike
    }

    val missingSelections = buildList {
        if (rangValues.isNotEmpty() && selectedRangValue == null) add("Rang")
        if (availableSizes.isNotEmpty() && selectedSize == null) add("O'lcham")
        otherAttributes.keys.forEach { attrName ->
            if (selectedOtherAttributes[attrName] == null) add(attrName)
        }
    }

    return ProductSelectionRules(rangValues, otherAttributes, missingSelections)
}

/**
 * Saytdagi rang nomlarini ko'rsatish uchun rangga o'giradi. DetailsScreen va
 * QuickAddBottomSheet bir xil rang-nom lug'atidan foydalanishi uchun shu yerda
 * markazlashgan — aks holda ikkalasida ro'yxat farqlanib qolishi mumkin edi.
 */
fun colorSwatchFor(name: String): Color? = when (name.trim().lowercase()) {
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
