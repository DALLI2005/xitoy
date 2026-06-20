package com.commander.xitoy.domain.repository

import com.commander.xitoy.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(): List<Product>
}
