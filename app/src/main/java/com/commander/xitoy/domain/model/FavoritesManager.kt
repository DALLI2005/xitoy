package com.commander.xitoy.domain.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesManager {
    private val _favorites = MutableStateFlow<List<Product>>(emptyList())
    val favorites: StateFlow<List<Product>> = _favorites.asStateFlow()

    fun toggle(product: Product) {
        val current = _favorites.value
        _favorites.value = if (current.any { it.name == product.name }) {
            current.filter { it.name != product.name }
        } else {
            current + product
        }
    }
}
