package com.commander.xitoy.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.CartCancelRequest
import com.commander.xitoy.data.remote.CartSyncRequest
import com.commander.xitoy.data.remote.OrderApi
import com.commander.xitoy.data.remote.OrderRequest
import com.commander.xitoy.domain.model.CartManager
import com.commander.xitoy.domain.model.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OrderState {
    object Idle : OrderState()
    object Loading : OrderState()
    data class Success(val orderId: String) : OrderState()
    data class Error(val message: String) : OrderState()
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val orderApi: OrderApi
) : ViewModel() {

    private val _orderState = MutableStateFlow<OrderState>(OrderState.Idle)
    val orderState: StateFlow<OrderState> = _orderState.asStateFlow()

    init {
        observeCartForReminder()
    }

    private fun observeCartForReminder() {
        var previousCount = 0
        viewModelScope.launch {
            CartManager.cartItems.collect { items ->
                val currentCount = items.size
                val telegramId = SessionManager.session.value?.telegramId ?: ""
                if (telegramId.isNotEmpty()) {
                    when {
                        currentCount > 0 && previousCount == 0 -> {
                            val name = items.firstOrNull()?.product?.name ?: ""
                            if (name.isNotEmpty()) syncCartReminderSilent(telegramId, name)
                        }
                        currentCount == 0 && previousCount > 0 -> {
                            cancelCartReminderSilent(telegramId)
                        }
                    }
                }
                previousCount = currentCount
            }
        }
    }

    fun placeOrder(
        telegramId: String,
        fullname: String,
        phone: String,
        locationLink: String,
        mahsulotlar: String,
        jamiSumma: Long,
        mahsulotRasm: String? = null
    ) {
        viewModelScope.launch {
            _orderState.value = OrderState.Loading
            try {
                val response = orderApi.createOrder(
                    OrderRequest(
                        telegram_id = telegramId,
                        fullname = fullname,
                        phone = phone,
                        location_link = locationLink,
                        mahsulotlar = mahsulotlar,
                        jami_summa = jamiSumma,
                        mahsulot_rasm = mahsulotRasm
                    )
                )
                _orderState.value = OrderState.Success(response.order_id)
                cancelCartReminderSilent(telegramId)
            } catch (e: Exception) {
                _orderState.value = OrderState.Error(e.message ?: "Xatolik yuz berdi")
            }
        }
    }

    fun resetState() {
        _orderState.value = OrderState.Idle
    }

    private fun syncCartReminderSilent(telegramId: String, mahsulotNomi: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try { orderApi.syncCart(CartSyncRequest(telegramId, mahsulotNomi)) } catch (_: Exception) {}
        }
    }

    private fun cancelCartReminderSilent(telegramId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try { orderApi.cancelCartReminder(CartCancelRequest(telegramId)) } catch (_: Exception) {}
        }
    }
}
