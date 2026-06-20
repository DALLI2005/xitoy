package com.commander.xitoy.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.OrderApi
import com.commander.xitoy.data.remote.OrderItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OrdersState {
    object Loading : OrdersState()
    data class Success(val orders: List<OrderItem>) : OrdersState()
    data class Error(val message: String) : OrdersState()
}

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderApi: OrderApi
) : ViewModel() {

    private val _state = MutableStateFlow<OrdersState>(OrdersState.Loading)
    val state: StateFlow<OrdersState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun loadOrders(telegramId: String) {
        viewModelScope.launch {
            _state.value = OrdersState.Loading
            try {
                val response = orderApi.listOrders(telegramId)
                _state.value = OrdersState.Success(response.orders)
            } catch (e: Exception) {
                _state.value = OrdersState.Error(e.message ?: "Buyurtmalar yuklanmadi")
            }
        }
    }

    fun startPolling(telegramId: String) {
        loadOrders(telegramId)
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(15_000)
                refreshSilently(telegramId)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun refreshSilently(telegramId: String) {
        try {
            val response = orderApi.listOrders(telegramId)
            _state.value = OrdersState.Success(response.orders)
        } catch (_: Exception) {
            // Polling xatosini ko'rsatmaymiz — mavjud holat saqlanadi
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
