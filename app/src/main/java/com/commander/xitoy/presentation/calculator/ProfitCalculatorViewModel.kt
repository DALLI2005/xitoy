package com.commander.xitoy.presentation.calculator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.domain.use_case.GetProductsUseCase
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Saqlangan hisob-kitob (SharedPreferences'da JSON sifatida saqlanadi)
data class SavedCalc(
    val name: String,
    val costPrice: Long,      // tan narx (1 dona, so'm)
    val sellPrice: Long,      // sotuv narxi (1 dona, so'm)
    val qty: Long,
    val totalProfit: Long,
    val marginPercent: Int
)

@HiltViewModel
class ProfitCalculatorViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val prefs = context.getSharedPreferences("dalli_calc", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _saved = MutableStateFlow(loadSaved())
    val saved: StateFlow<List<SavedCalc>> = _saved

    fun loadProducts() {
        getProductsUseCase.getCached()?.let { if (it.isNotEmpty()) _products.value = it }
        viewModelScope.launch {
            runCatching { getProductsUseCase() }
                .onSuccess { if (it.isNotEmpty()) _products.value = it }
        }
    }

    fun save(calc: SavedCalc) {
        val list = (listOf(calc) + _saved.value).take(20)
        _saved.value = list
        persist(list)
    }

    fun delete(calc: SavedCalc) {
        val list = _saved.value.filterNot { it === calc }
        _saved.value = list
        persist(list)
    }

    private fun loadSaved(): List<SavedCalc> {
        val json = prefs.getString(KEY_SAVED, null) ?: return emptyList()
        return runCatching {
            gson.fromJson(json, Array<SavedCalc>::class.java)?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<SavedCalc>) {
        prefs.edit().putString(KEY_SAVED, gson.toJson(list)).apply()
    }

    companion object {
        private const val KEY_SAVED = "saved_calcs"
    }
}
