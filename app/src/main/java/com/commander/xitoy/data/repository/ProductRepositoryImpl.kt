package com.commander.xitoy.data.repository

import android.content.Context
import com.commander.xitoy.data.remote.XitoyApi
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.repository.ProductRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProductRepositoryImpl(
    private val api: XitoyApi,
    private val context: Context
) : ProductRepository {

    private val prefs = context.getSharedPreferences("products_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cacheKey = "products_json"

    @Volatile
    private var cachedProducts: List<Product>? = loadFromDisk()

    override suspend fun getProducts(): List<Product> {
        val fresh = api.getProducts(timestamp = System.currentTimeMillis())
        cachedProducts = fresh
        saveToDisk(fresh)
        return fresh
    }

    override fun getCachedProducts(): List<Product>? = cachedProducts

    private fun saveToDisk(products: List<Product>) {
        try {
            val json = gson.toJson(products)
            prefs.edit().putString(cacheKey, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromDisk(): List<Product>? {
        return try {
            val json = prefs.getString(cacheKey, null) ?: return null
            val type = object : TypeToken<List<Product>>() {}.type
            gson.fromJson<List<Product>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
