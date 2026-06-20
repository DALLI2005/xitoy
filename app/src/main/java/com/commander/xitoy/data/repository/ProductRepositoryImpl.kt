package com.commander.xitoy.data.repository

import com.commander.xitoy.data.remote.XitoyApi
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.repository.ProductRepository

class ProductRepositoryImpl(
    private val api: XitoyApi
) : ProductRepository {

    override suspend fun getProducts(): List<Product> {
        return api.getProducts(timestamp = System.currentTimeMillis())
    }
}
