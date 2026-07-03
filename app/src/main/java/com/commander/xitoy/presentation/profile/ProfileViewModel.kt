package com.commander.xitoy.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commander.xitoy.data.remote.AuthApi
import com.commander.xitoy.data.remote.ChangePasswordRequest
import com.commander.xitoy.data.remote.OrderApi
import com.commander.xitoy.domain.model.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

sealed interface ChangePasswordState {
    data object Idle : ChangePasswordState
    data object Loading : ChangePasswordState
    data object Success : ChangePasswordState
    data class Error(val message: String) : ChangePasswordState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val orderApi: OrderApi,
    private val authApi: AuthApi
) : ViewModel() {

    data class ProfileStats(
        val orderCount: Int = 0,
        val totalImported: Long = 0,
        val totalSaved: Long = 0,
        val isLoading: Boolean = true
    )

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats

    private val _changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Idle)
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState

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

    fun changePassword(oldPassword: String, newPassword: String) {
        if (_changePasswordState.value is ChangePasswordState.Loading) return
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState.Loading
            try {
                val userId = SessionManager.session.value?.telegramId?.toLongOrNull()
                    ?: throw IllegalStateException("Session yo'q")
                val resp = authApi.changePassword(
                    ChangePasswordRequest(userId, oldPassword, newPassword)
                )
                SessionManager.updateToken(resp.token)
                _changePasswordState.value = ChangePasswordState.Success
            } catch (e: HttpException) {
                _changePasswordState.value = ChangePasswordState.Error(
                    when (e.code()) {
                        401 -> "Joriy parol noto'g'ri"
                        404 -> "Foydalanuvchi topilmadi, qayta kiring"
                        400 -> "Yangi parol kamida 6 belgidan iborat bo'lishi kerak"
                        else -> "Xatolik yuz berdi, qayta urinib ko'ring"
                    }
                )
            } catch (e: Exception) {
                _changePasswordState.value = ChangePasswordState.Error("Internet aloqasini tekshiring")
            }
        }
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordState.Idle
    }
}
