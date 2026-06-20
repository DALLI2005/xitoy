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

    @SerializedName("category")
    val category: String,

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
) {
    // Barcha rasmlar: images mavjud bo'lsa ularni, yo'qsa imageUrl ni qaytaradi
    val allImages: List<String>
        get() = if (images.isNotEmpty()) images else if (imageUrl.isNotEmpty()) listOf(imageUrl) else emptyList()
}
