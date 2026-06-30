package com.commander.xitoy.presentation.home

enum class SortOption(val label: String) {
    NEWEST("Eng yangi"),
    PRICE_ASC("Eng arzon"),
    PRICE_DESC("Eng qimmat"),
    BEST_SELLING("Eng ko'p sotilgan")
}

data class FilterState(
    val sortBy: SortOption = SortOption.NEWEST,
    val selectedCategories: Set<String> = emptySet(),
    val priceRange: ClosedFloatingPointRange<Float> = 0f..1_000_000f,
    val onlyDiscounted: Boolean = false
) {
    val isActive: Boolean
        get() = sortBy != SortOption.NEWEST ||
                selectedCategories.isNotEmpty() ||
                priceRange != 0f..1_000_000f ||
                onlyDiscounted

    val activeCount: Int
        get() = listOf(
            selectedCategories.isNotEmpty(),
            priceRange != 0f..1_000_000f,
            onlyDiscounted
        ).count { it }
}

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.OrderApi
import com.commander.xitoy.domain.model.FavoritesManager
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.model.SessionManager
import com.commander.xitoy.domain.use_case.GetProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val orderApi: OrderApi
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allProductsCount: StateFlow<Int> = _products
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val filteredProducts: StateFlow<List<Product>> = combine(
        _products,
        _searchQuery.debounce(300).onStart { emit("") }
    ) { products, query ->
        if (query.isBlank()) products
        else products.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Xatoni ushlab, dizaynga uzatish uchun maxsus o'zgaruvchi
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchProducts()
        startPolling()
        loadFavorites()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        fetchProducts()
    }

    private fun fetchProducts() {
        viewModelScope.launch {
            // 1. AVVAL keshlangan ma'lumot bo'lsa, DARHOL ko'rsat (server kutmasdan).
            //    Shu tarzda sahifa qayta ochilganda "qotish" yo'qoladi.
            val cached = getProductsUseCase.getCached()
            val hasCache = !cached.isNullOrEmpty()
            if (hasCache) {
                _products.value = cached!!
                _isLoading.value = false // indikator ko'rsatilmaydi
            } else {
                _isLoading.value = true  // faqat birinchi marta (kesh yo'q) indikator
            }

            // 2. Keyin fonda serverdan yangisini ol (jimgina).
            _errorMessage.value = null
            try {
                _products.value = getProductsUseCase()
            } catch (e: Exception) {
                // Kesh bor bo'lsa, xato ko'rsatmaymiz — eski ma'lumot turaveradi.
                if (!hasCache) {
                    _errorMessage.value = e.message ?: "Noma'lum xatolik yuz berdi"
                }
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            val tid = SessionManager.session.value?.telegramId ?: return@launch
            FavoritesManager.loadFavorites(orderApi, tid)
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L) // 30 soniyada bir yangilash
                try {
                    val fresh = getProductsUseCase()
                    if (fresh != _products.value) {
                        _products.value = fresh
                    }
                    _errorMessage.value = null
                } catch (_: Exception) {
                    // polling xatosi jimgina o'tib ketadi
                }
            }
        }
    }
}