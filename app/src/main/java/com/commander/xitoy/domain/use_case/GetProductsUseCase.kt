package com.commander.xitoy.domain.use_case

import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.repository.ProductRepository

class GetProductsUseCase(
    private val repository: ProductRepository
) {
    // 'invoke' operatori orqali biz bu class'ni xuddi oddiy funksiyadek ishlata olamiz
    suspend operator fun invoke(): List<Product> {
        // Bir xil 'id' ga ega takror mahsulotlarni olib tashlaymiz.
        // LazyColumn / LazyVerticalGrid har bir element uchun UNIKAL key talab
        // qiladi (ekranlarda key = { it.id } ishlatilgan). Agar serverdan takror
        // id kelsa, scroll paytida ikkala element bir vaqtda kompozitsiyaga
        // kirganda Compose "Key ... was already used" IllegalArgumentException
        // bilan ilovani yopib yuboradi. distinctBy birinchi nusxani saqlaydi va
        // ro'yxat tartibini buzmaydi.
        return repository.getProducts().distinctBy { it.id }
    }

    // Keshlangan mahsulotlarni darhol qaytaradi (server so'rovisiz).
    // invoke() bilan bir xil — takror id'larni olib tashlaymiz, chunki
    // ro'yxat to'g'ridan-to'g'ri LazyColumn/LazyVerticalGrid key sifatida ishlatiladi.
    fun getCached(): List<Product>? =
        repository.getCachedProducts()?.distinctBy { it.id }
}