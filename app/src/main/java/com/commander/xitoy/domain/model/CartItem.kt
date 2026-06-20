package com.commander.xitoy.domain.model

data class CartItem(
    val product: Product,
    val variantName: String? = null,
    val effectivePrice: Double = product.price
)
