package com.commander.xitoy.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import com.commander.xitoy.domain.model.Product

interface XitoyApi {

    // Google Apps Script dan barcha tovarlarni olish
    // t parametri har so'rovda kesh bypass qilish uchun
    @GET("exec")
    suspend fun getProducts(@Query("t") timestamp: Long): List<Product>
}
