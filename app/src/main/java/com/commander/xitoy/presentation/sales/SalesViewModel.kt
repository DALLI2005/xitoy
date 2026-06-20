package com.commander.xitoy.presentation.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.use_case.GetProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SalesSort { BY_DISCOUNT, BY_PRICE }

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())

    val sortMode = MutableStateFlow(SalesSort.BY_DISCOUNT)

    val discountedProducts: StateFlow<List<Product>> = combine(_products, sortMode) { list, sort ->
        val filtered = list.filter { it.discountPercent > 0 }
        when (sort) {
            SalesSort.BY_DISCOUNT -> filtered.sortedByDescending { it.discountPercent }
            SalesSort.BY_PRICE -> filtered.sortedBy { it.price }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { fetchProducts() }

    fun setSort(sort: SalesSort) { sortMode.value = sort }
    fun refresh() { fetchProducts() }

    private fun fetchProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try { _products.value = getProductsUseCase() }
            catch (_: Exception) { }
            finally { _isLoading.value = false }
        }
    }
}
