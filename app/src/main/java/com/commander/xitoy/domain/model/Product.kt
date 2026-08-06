package com.commander.xitoy.domain.model

import com.google.gson.annotations.SerializedName

data class Product(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("price")
    val price: Double,

    @SerializedName("image")
    val imageUrl: String,

    // Kategoriya 3 darajali: category (asosiy toifa) → subcategory → productType.
    // Backend har doim uchalasini ham to'ldirilgan holda qaytaradi (categories.py).
    @SerializedName("category")
    val category: String,

    @SerializedName("subcategory")
    val subcategory: String = "",

    @SerializedName("productType")
    val productType: String = "",

    @SerializedName("categoryPath")
    val categoryPath: List<String> = emptyList(),

    val discountPercent: Int = 0,

    val images: List<String> = emptyList(),

    @SerializedName("sold_count")
    val soldCount: Int = 0,

    @SerializedName("rating")
    val rating: Float = 0f,

    @SerializedName("discountType")
    val discountType: String = "doimiy",

    @SerializedName("discountExpires")
    val discountExpires: String? = null,

    @SerializedName("autoDelete")
    val autoDelete: Boolean = false,

    @SerializedName("variantlarYoqilgan")
    val variantlarYoqilgan: Boolean = false,

    @SerializedName("variantNomlari")
    val variantNomlari: List<String> = emptyList(),

    @SerializedName("variantNarxlari")
    val variantNarxlari: List<Double> = emptyList(),

    @SerializedName("razmerMatritsa")
    val razmerMatritsa: Map<String, List<SizeOption>> = emptyMap(),

    // ── Admin saytda kiritadigan qo'shimcha ma'lumotlar ──────────────────────
    /** Ishlab chiqarilgan mamlakat, masalan "Xitoy". Bo'sh = ko'rsatilmagan. */
    @SerializedName("country")
    val country: String = "",

    /** Kafolat oylarda. 0 = admin ko'rsatmagan → qonun bo'yicha 6 oy. */
    @SerializedName("guaranteeMonths")
    val guaranteeMonths: Int = 0,

    /** Xususiyatlar: {"Rang": ["Qora","Oq"], "O'lcham": ["M","L"]} */
    @SerializedName("attributes")
    val attributes: Map<String, List<String>> = emptyMap(),
) {
    // Barcha rasmlar: images mavjud bo'lsa ularni, yo'qsa imageUrl ni qaytaradi
    val allImages: List<String>
        get() = if (images.isNotEmpty()) images else if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList()

    /** To'liq kategoriya yo'li: "Elektronika › Aqlli uy va xavfsizlik › Aqlli uy" */
    val fullCategoryPath: String
        get() = categoryPath.ifEmpty {
            listOf(category, subcategory, productType).filter { it.isNotBlank() }
        }.joinToString(" › ")

    /** Qidiruv/filtrda kategoriyaning istalgan darajasi bo'yicha moslik */
    fun matchesCategory(query: String): Boolean =
        category.equals(query, ignoreCase = true) ||
            subcategory.equals(query, ignoreCase = true) ||
            productType.equals(query, ignoreCase = true)

    /** Kafolat matni. Admin ko'rsatmagan bo'lsa — qonun bo'yicha 6 oy. */
    val guaranteeText: String
        get() = if (guaranteeMonths > 0) "$guaranteeMonths oy" else "6 oy"

    /**
     * Tovar tavsifnomasi — kartochkadagi "Xususiyatlar" jadvali uchun
     * tayyor (nom, qiymat) juftliklari. Faqat to'ldirilganlari qaytariladi.
     */
    val specifications: List<Pair<String, String>>
        get() = buildList {
            if (productType.isNotBlank()) add("Tovar turi" to productType)
            if (subcategory.isNotBlank()) add("Toifa" to subcategory)
            if (country.isNotBlank()) add("Ishlab chiqarilgan" to country)
            add("Kafolat" to guaranteeText)
            attributes.forEach { (nom, qiymatlar) ->
                if (qiymatlar.isNotEmpty()) add(nom to qiymatlar.joinToString(", "))
            }
        }
}
