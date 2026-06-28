package com.commander.xitoy.domain.model

import com.commander.xitoy.data.remote.OrderApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _favorites = MutableStateFlow<List<Product>>(emptyList())
    val favorites: StateFlow<List<Product>> = _favorites.asStateFlow()

    private var api: OrderApi? = null
    private var telegramId: String? = null

    suspend fun loadFavorites(orderApi: OrderApi, tgId: String) {
        api = orderApi
        telegramId = tgId
        try {
            _favorites.value = orderApi.getFavorites(tgId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggle(product: Product) {
        val current = _favorites.value
        val isFav = current.any { it.id == product.id }
        if (isFav) {
            _favorites.value = current.filter { it.id != product.id }
            scope.launch {
                try {
                    val tid = telegramId ?: return@launch
                    api?.removeFavorite(product.id.toString(), tid)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _favorites.value = _favorites.value + product
                }
            }
        } else {
            _favorites.value = current + product
            scope.launch {
                try {
                    val tid = telegramId ?: return@launch
                    api?.addFavorite(product.id.toString(), tid)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _favorites.value = _favorites.value.filter { it.id != product.id }
                }
            }
        }
    }
}
