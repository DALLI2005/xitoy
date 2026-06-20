package com.commander.xitoy.domain.use_case

import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.repository.ProductRepository

class GetProductsUseCase(
    private val repository: ProductRepository
) {
    // 'invoke' operatori orqali biz bu class'ni xuddi oddiy funksiyadek ishlata olamiz
    suspend operator fun invoke(): List<Product> {
        // Kelajakda tovarlarni narxi bo'yicha saralash (sort) yoki
        // faqat aktiv tovarlarni ko'rsatish mantiqini aynan shu yerga yozamiz.
        // Hozircha faqat borini olib kelamiz:
        return repository.getProducts()
    }
}