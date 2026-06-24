package com.commander.xitoy.data.repository

import com.commander.xitoy.data.remote.XitoyApi
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.repository.ProductRepository

class ProductRepositoryImpl(
    private val api: XitoyApi
) : ProductRepository {

    // Xotira keshi — ilova ishlab turganda saqlanadi (repository @Singleton).
    // Sahifa qayta ochilganda mahsulotlarni server kutmasdan darhol ko'rsatish uchun.
    @Volatile
    private var cachedProducts: List<Product>? = null

    override suspend fun getProducts(): List<Product> {
        val fresh = api.getProducts(timestamp = System.currentTimeMillis())
        cachedProducts = fresh
        return fresh
    }

    override fun getCachedProducts(): List<Product>? = cachedProducts
}
