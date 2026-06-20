package com.commander.xitoy.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.use_case.GetProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryItem(
    val title: String,
    val count: Int
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchCategories()
    }

    fun refresh() {
        fetchCategories()
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val products = getProductsUseCase()
                _products.value = products
                _categories.value = products
                    .groupBy { it.category }
                    .map { (category, items) -> CategoryItem(title = category, count = items.size) }
                    .sortedBy { it.title }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Noma'lum xatolik yuz berdi"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
