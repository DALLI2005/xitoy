package com.commander.xitoy.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.OrderApi
import com.commander.xitoy.domain.model.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val orderApi: OrderApi
) : ViewModel() {

    data class ProfileStats(
        val orderCount: Int = 0,
        val totalImported: Long = 0,
        val totalSaved: Long = 0,
        val isLoading: Boolean = true
    )

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats

    fun loadStats() {
        viewModelScope.launch {
            val telegramId = SessionManager.session.value?.telegramId ?: run {
                _stats.value = ProfileStats(isLoading = false)
                return@launch
            }
            try {
                val orders = orderApi.listOrders(telegramId).orders
                val totalImported = orders.sumOf { it.jami_summa }
                val totalSaved = (totalImported * 1.6).toLong()
                _stats.value = ProfileStats(
                    orderCount = orders.size,
                    totalImported = totalImported,
                    totalSaved = totalSaved,
                    isLoading = false
                )
            } catch (e: Exception) {
                _stats.value = ProfileStats(isLoading = false)
            }
        }
    }
}
