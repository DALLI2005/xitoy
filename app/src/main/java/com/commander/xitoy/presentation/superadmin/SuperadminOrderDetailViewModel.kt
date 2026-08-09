package com.commander.xitoy.presentation.superadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.SuperadminApi
import com.commander.xitoy.data.remote.SuperadminOrderDetail
import com.commander.xitoy.domain.model.SuperadminSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SuperadminOrderDetailState {
    object Loading : SuperadminOrderDetailState()
    data class Success(val order: SuperadminOrderDetail) : SuperadminOrderDetailState()
    data class Error(val message: String) : SuperadminOrderDetailState()
    object LoggedOut : SuperadminOrderDetailState()
}

@HiltViewModel
class SuperadminOrderDetailViewModel @Inject constructor(
    private val api: SuperadminApi
) : ViewModel() {

    private val _state = MutableStateFlow<SuperadminOrderDetailState>(SuperadminOrderDetailState.Loading)
    val state: StateFlow<SuperadminOrderDetailState> = _state.asStateFlow()

    fun load(orderId: String) {
        val token = SuperadminSessionManager.tokenOrNull()
        if (token == null) {
            _state.value = SuperadminOrderDetailState.LoggedOut
            return
        }
        viewModelScope.launch {
            _state.value = SuperadminOrderDetailState.Loading
            try {
                val order = api.getOrderDetail(token, orderId)
                _state.value = SuperadminOrderDetailState.Success(order)
            } catch (e: Exception) {
                _state.value = SuperadminOrderDetailState.Error(e.message ?: "Buyurtma topilmadi")
            }
        }
    }
}
