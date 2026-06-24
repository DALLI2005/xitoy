package com.commander.xitoy.domain.repository

import com.commander.xitoy.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(): List<Product>

    // Oxirgi muvaffaqiyatli yuklangan mahsulotlarni xotiradan qaytaradi
    // (server kutmasdan). Hali hech narsa yuklanmagan bo'lsa null.
    fun getCachedProducts(): List<Product>?
}
