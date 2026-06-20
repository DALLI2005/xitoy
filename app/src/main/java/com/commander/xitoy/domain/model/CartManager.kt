package com.commander.xitoy.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CartManager {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun addToCart(product: Product, variantName: String? = null, variantPrice: Double? = null) {
        val item = CartItem(
            product = product,
            variantName = variantName,
            effectivePrice = variantPrice ?: product.price
        )
        _cartItems.value = _cartItems.value + item
    }

    fun removeFromCart(item: CartItem) {
        val list = _cartItems.value.toMutableList()
        val idx = list.indexOfFirst { it.product.name == item.product.name && it.variantName == item.variantName }
        if (idx >= 0) list.removeAt(idx)
        _cartItems.value = list
    }

    fun removeAllOf(item: CartItem) {
        _cartItems.value = _cartItems.value.filter {
            it.product.name != item.product.name || it.variantName != item.variantName
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }
}
