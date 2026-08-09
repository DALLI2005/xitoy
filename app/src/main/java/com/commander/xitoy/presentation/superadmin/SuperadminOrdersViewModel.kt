package com.commander.xitoy.presentation.superadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.SuperadminApi
import com.commander.xitoy.data.remote.SuperadminOrderListItem
import com.commander.xitoy.domain.model.SuperadminSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SuperadminOrdersState {
    object Loading : SuperadminOrdersState()
    data class Success(val orders: List<SuperadminOrderListItem>) : SuperadminOrdersState()
    data class Error(val message: String) : SuperadminOrdersState()
    object LoggedOut : SuperadminOrdersState()
}

@HiltViewModel
class SuperadminOrdersViewModel @Inject constructor(
    private val api: SuperadminApi
) : ViewModel() {

    private val _state = MutableStateFlow<SuperadminOrdersState>(SuperadminOrdersState.Loading)
    val state: StateFlow<SuperadminOrdersState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun loadOrders() {
        val token = SuperadminSessionManager.tokenOrNull()
        if (token == null) {
            _state.value = SuperadminOrdersState.LoggedOut
            return
        }
        viewModelScope.launch {
            _state.value = SuperadminOrdersState.Loading
            try {
                val response = api.listOrders(token)
                _state.value = SuperadminOrdersState.Success(response.orders)
            } catch (e: Exception) {
                _state.value = SuperadminOrdersState.Error(e.message ?: "Buyurtmalar yuklanmadi")
            }
        }
    }

    fun startPolling() {
        loadOrders()
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(15_000)
                refreshSilently()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun refreshSilently() {
        val token = SuperadminSessionManager.tokenOrNull() ?: run {
            _state.value = SuperadminOrdersState.LoggedOut
            return
        }
        try {
            val response = api.listOrders(token)
            _state.value = SuperadminOrdersState.Success(response.orders)
        } catch (_: Exception) {
            // Polling xatosini ko'rsatmaymiz — mavjud holat saqlanadi
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
